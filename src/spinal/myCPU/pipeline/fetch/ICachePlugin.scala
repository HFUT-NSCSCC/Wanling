package myCPU.pipeline.fetch
import spinal.core._
import myCPU.builder.Plugin
import myCPU.core.Core
import myCPU.ICacheConfig
import myCPU.blackbox.SDPRAM
import myCPU.CacheBasicConfig
import myCPU.builder.Stageable
import myCPU.core.LA32R.PC_INIT
import spinal.lib.MuxOH
import spinal.lib.fsm.StateMachine
import spinal.lib.fsm.State
import myCPU.InstBundle
import _root_.spinal.lib.Counter
import spinal.lib.master
import myCPU.blackbox.SDPRAMAsync

final case class CacheLineInfo(config: CacheBasicConfig, useDirty: Boolean = false) extends Bundle{
    val lru = Bits(log2Up(config.ways) bits)
    val tags = Vec(UInt(config.tagRange.size bits), config.ways)
    val dirtyBits = useDirty generate(Bits(config.ways bits))

}

class ICachePlugin extends Plugin[Core]{
    val instBundle = master(InstBundle())
    val branchable = RegInit(True)
    private val icache = ICacheConfig()

    val valids = Vec(Vec(RegInit(False), icache.ways), icache.sets)
    val dataRAMs = 
        Seq.fill(icache.ways)(
            new SDPRAM(Vec(Bits(32 bits), icache.lineWords), icache.sets, false, useByteEnable = true)
            )
    // 为了能够在当前周期获得缓存命中信息, 对info采用了异步存储的方法(对时序并不友好, 可以考虑使用NPC来发起请求, 但不方便调试, 且理解起来较难)
    val infoRAMASync = new SDPRAMAsync(CacheLineInfo(icache), icache.sets)

    object ICACHE_VALIDS extends Stageable(valids.dataType())
    object ICACHE_RSPS extends Stageable(Vec(Bits(32 bits), icache.ways))
    object ICACHE_INFO extends Stageable(CacheLineInfo(icache))
    object ICACHE_HIT extends Stageable(Bool)
    object ICACHE_HITS extends Stageable(Vec(Bool, icache.ways))
    def build(pipeline: Core): Unit = {
        import pipeline._

        instBundle.en := False
        instBundle.addr := 0

        IF1 plug new Area {
            import IF1._
            val rValid = !pipeline.IF1.arbitration.isStuck
            
            val rAddr = pipeline.IF1.output(fetchSignals.PC)
            val idx = rAddr(icache.indexRange)
            val tag = rAddr(icache.tagRange)
    
            val dataRs = Vec(dataRAMs.map(_.io.read))
            dataRs.foreach{ p => 
                p.cmd.valid := rValid
                p.cmd.payload := idx
            }
            val setValids = valids(idx)
            infoRAMASync.io.read.address := idx
            val infos = infoRAMASync.io.read.data
            val hits = setValids.zip(infos.tags).map {case(valid, t) => 
                valid && t === tag    
            }
            val hit = hits.reduce(_ || _)
            insert(ICACHE_HIT) := hit
            insert(ICACHE_HITS) := Vec(hits(0), hits(1))
            insert(ICACHE_INFO) := infos
            when(!arbitration.isFlushed && arbitration.isValidNotStuck && !hit){
                branchable := False
            }
        }
        
        IF2 plug new Area {
            import IF2._
            
            val pc = input(fetchSignals.PC)
            insert(ICACHE_VALIDS) := valids(pc(icache.indexRange))
            for (i <- 0 until icache.ways) {
                insert(ICACHE_RSPS)(i) := dataRAMs(i).io.read.rsp(pc(icache.wordOffsetRange))
            }
            val idx = pc(icache.indexRange)
            val tag = pc(icache.tagRange)
            val offset = pc(icache.wordOffsetRange)
            val setValids = input(ICACHE_VALIDS)

            val wPortAsync = infoRAMASync.io.write.setIdle()
            val dataWs = Vec(dataRAMs.map(_.io.write.setIdle()))
            val dataMasks = Vec(dataRAMs.map(_.io.writeMask.clearAll().subdivideIn(4 bits, true)))

            // val hits = setValids.zip(input(ICACHE_INFO).tags).map {case(valid, t) => 
            //     valid && t === tag    
            // }
            // // val hit = hits.
            // val hit = hits.reduce(_ || _)
            val hits = input(ICACHE_HITS)
            val hit = input(ICACHE_HIT)
            val hitData = MuxOH(hits, input(ICACHE_RSPS))
            insert(fetchSignals.INST) := hitData

            // 命中的时候对info进行更新
            when(hit) {
                val newInfo = input(ICACHE_INFO).copy()
                newInfo.tags := input(ICACHE_INFO).tags
                newInfo.lru(0) := hits(0)
                wPortAsync.valid.set()
                wPortAsync.payload.address := idx
                wPortAsync.payload.data := newInfo
            }

            // 缓存缺失处理的状态机设计
            // 状态机设计: https://jiunian-pic-1310185536.cos.ap-nanjing.myqcloud.com/image-20240820213152504.png
            val cacheRefillFSM = new StateMachine{
                val READ = new State
                val COMMIT = new State
                val FINISH = new State

                disableAutoStart()
                setEntry(stateBoot)
                val inst2Fire = Reg(Bits(32 bits)) init(0)
                // val count = Counter(icache.lineWords)
                val count = Reg(UInt(log2Up(icache.lineWords) bits)) init(0)
                val replaceWay = input(ICACHE_INFO).lru.asUInt

                when(count === offset) {
                    inst2Fire := instBundle.rdata
                }
                
                stateBoot.whenIsActive {
                    when(arbitration.isValidOnEntry && !hit){
                        arbitration.haltItself.set()
                        // branchable := False
                        when(instBundle.rvalid){
                            // count.clear()
                            count := 0
                            instBundle.en := True
                            instBundle.addr := (pc(31 downto icache.offsetWidth) @@ U(0, icache.offsetWidth bits)).asBits
                            goto(READ)
                        }
                    }
                }

                READ.whenIsActive{
                    arbitration.haltItself.set()
                    branchable := False
                    when(instBundle.rresp){
                        dataWs(replaceWay).valid := True
                        dataWs(replaceWay).payload.address := idx
                        dataWs(replaceWay).payload.data.foreach(_ := instBundle.rdata)
                        dataMasks(replaceWay)(count).setAll()
                    }
                    when(instBundle.rvalid) {
                        instBundle.en := True
                        count := count + 1
                        instBundle.addr := (pc(31 downto icache.offsetWidth) @@ U(0, icache.offsetWidth bits) + ((count + 1) << 2)).asBits
                        when(count === (icache.lineWords - 2)){
                            goto(COMMIT)
                        } otherwise {
                            goto(READ)
                        }
                    }
                }

                COMMIT.whenIsActive{
                    arbitration.haltByOther.set()
                    // branchable := False
                    when(instBundle.rresp) {
                        // branchable := True
                        branchable := True
                        dataWs(replaceWay).valid := True
                        dataWs(replaceWay).payload.address := idx
                        dataWs(replaceWay).payload.data.foreach(_ := instBundle.rdata)
                        dataMasks(replaceWay)(count).setAll()

                        val newInfo = input(ICACHE_INFO).copy()
                        newInfo.tags := input(ICACHE_INFO).tags
                        newInfo.tags(replaceWay) := tag
                        newInfo.lru := ~input(ICACHE_INFO).lru
                        valids(idx)(replaceWay).set()
                        // wPort.valid.set()
                        // wPort.payload.address := idx
                        // wPort.payload.data := newInfo
                        wPortAsync.valid.set()
                        wPortAsync.payload.address := idx
                        wPortAsync.payload.data := newInfo
                        goto(FINISH)
                    }
                }

                FINISH.whenIsActive{
                    when(!arbitration.isStuck){
                        insert(fetchSignals.INST) := inst2Fire
                        goto(stateBoot)
                    }
                }
            }
        }

    }
}
