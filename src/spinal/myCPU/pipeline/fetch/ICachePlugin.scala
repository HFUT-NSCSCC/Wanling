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
    val infoRAM = new SDPRAM(CacheLineInfo(icache), icache.sets, false)

    object ICACHE_VALIDS extends Stageable(valids.dataType())
    object ICACHE_RSPS extends Stageable(Vec(Bits(32 bits), icache.ways))
    object ICACHE_INFO extends Stageable(CacheLineInfo(icache))
    def build(pipeline: Core): Unit = {
        import pipeline._

        instBundle.en := False
        instBundle.addr := 0

        val rPort = infoRAM.io.read
        
        IF1 plug new Area {
            import IF1._
            val rValid = !pipeline.IF1.arbitration.isStuck
            // val pcBeforeStuck = RegNextWhen[UInt](IF1.output(fetchSignals.PC), !IF1.arbitration.isStuck, init = PC_INIT)
            
            val rAddr = pipeline.IF1.output(fetchSignals.PC)
            val idx = rAddr(icache.indexRange)
            // val rAddr = Mux(refetchValid, pipeline.IF1.output(fetchSignals.PC), pipeline.IF1.output(fetchSignals.NPC))
            rPort.cmd.valid := rValid
            rPort.cmd.payload := idx
    
            val dataRs = Vec(dataRAMs.map(_.io.read))
            dataRs.foreach{ p => 
                p.cmd.valid := rValid
                p.cmd.payload := idx
            }
        }
        
        IF2 plug new Area {
            import IF2._
            
            val pc = input(fetchSignals.PC)
            insert(ICACHE_VALIDS) := valids(pc(icache.indexRange))
            for (i <- 0 until icache.ways) {
                insert(ICACHE_RSPS)(i) := dataRAMs(i).io.read.rsp(pc(icache.wordOffsetRange))
            }
            insert(ICACHE_INFO) := rPort.rsp
            val idx = pc(icache.indexRange)
            val tag = pc(icache.tagRange)
            val offset = pc(icache.wordOffsetRange)
            val setValids = input(ICACHE_VALIDS)

            val wPort = infoRAM.io.write.setIdle()
            val dataWs = Vec(dataRAMs.map(_.io.write.setIdle()))
            val dataMasks = Vec(dataRAMs.map(_.io.writeMask.clearAll().subdivideIn(4 bits, true)))

            val hits = setValids.zip(input(ICACHE_INFO).tags).map {case(valid, t) => 
                valid && t === tag    
            }
            // val hit = hits.
            val hit = hits.reduce(_ || _)
            val hitData = MuxOH(hits, input(ICACHE_RSPS))
            insert(fetchSignals.INST) := hitData

            when(hit) {
                val newInfo = input(ICACHE_INFO).copy()
                newInfo.tags := input(ICACHE_INFO).tags
                newInfo.lru(0) := hits(0)
                wPort.valid.set()
                wPort.payload.address := idx
                wPort.payload.data := newInfo
            }

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
                    when(arbitration.isValid && !hit){
                        arbitration.haltItself.set()
                        branchable := False
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
                    // branchable := True
                    when(instBundle.rresp) {
                        dataWs(replaceWay).valid := True
                        dataWs(replaceWay).payload.address := idx
                        dataWs(replaceWay).payload.data.foreach(_ := instBundle.rdata)
                        dataMasks(replaceWay)(count).setAll()

                        val newInfo = input(ICACHE_INFO).copy()
                        newInfo.tags := input(ICACHE_INFO).tags
                        newInfo.tags(replaceWay) := tag
                        newInfo.lru := ~input(ICACHE_INFO).lru
                        valids(idx)(replaceWay).set()
                        wPort.valid.set()
                        wPort.payload.address := idx
                        wPort.payload.data := newInfo
                        goto(FINISH)
                    }
                }

                FINISH.whenIsActive{
                    branchable := True
                    when(!arbitration.isStuck){
                        insert(fetchSignals.INST) := inst2Fire
                        goto(stateBoot)
                    }
                }
            }
        }

    }
}
