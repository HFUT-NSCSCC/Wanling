package myCPU.pipeline.fetch

import spinal.core._
import myCPU.builder.Plugin
import myCPU.core.Core
import myCPU.CacheBasicConfig
import _root_.myCPU.blackbox.SDPRAMAsync
import spinal.lib.fsm._
import _root_.spinal.lib.MuxOH
import myCPU.builder.Stageable
import myCPU.constants.ImmExtType
import myCPU.constants.JumpType

final case class BTBConfig(
    sets: Int = 64,
    lineSize: Int = 1,
    ways: Int = 2
) extends CacheBasicConfig{
    val enable = true
}

final case class BTBLineInfo(config: BTBConfig) extends Bundle{
    val lru = Bits(log2Up(config.ways) bits)
    // Branch Instruction Address, xor every 4 bit to contruct bia
    val bia = Vec(Bits(32/4 + 8 bits), config.ways)
    // val backwards = Vec(Bool, config.ways)
    val tracker = Vec(UInt(2 bits), config.ways)
    // Branch Target Address
    val bta = Vec(UInt(32 bits), config.ways)
}

class BPUPlugin extends Plugin[Core]{
    private val btbConfig = BTBConfig()
    val valids = Vec(Vec(RegInit(False), btbConfig.ways), btbConfig.sets)
    val infoRAM = new SDPRAMAsync(BTBLineInfo(btbConfig), btbConfig.sets)

    object BTB_TAG extends Stageable(Bits(32/4 + 8 bits))
    object BTB_INFO extends Stageable(BTBLineInfo(btbConfig))
    object BTB_HIT extends Stageable(Bool)
    object BRANCH extends Stageable(Bool)
    object BRANCH_TARGET extends Stageable(UInt(32 bits))
    object BRANCH_IMM extends Stageable(UInt(32 bits))

    def getBTBTag(pc: UInt): Bits = {
        val width = pc.getWidth / 4 + 8
        val ret = Bits(width bits)
        for (i <- 0 until width - 8){
            ret(i) := pc(i*4) ^ pc(i*4+1) ^ pc(i*4+2) ^ pc(i*4+3)
        }
        ret(width-1 downto width - 8) := pc(11 downto 4).asBits
        ret
    }

    def build(pipeline: Core): Unit = {
        import pipeline._

        val rPort = infoRAM.io.read

        val branchJumpInst = Bool
        val actuallyTaken = Bool
        val actualBranchTarget = UInt(32 bits)
        
        val pcManager = service(classOf[PCManagerPlugin])
        
        IF1 plug new Area {
            import IF1._
            val prejump = Bool
            val pc = output(fetchSignals.PC)
            val idx = pc(btbConfig.indexRange)
            val tag = getBTBTag(pc)
            insert(BTB_TAG) := tag
            rPort.address := pc(btbConfig.indexRange)
            val btbValids = valids(idx)
            val btbInfo = rPort.data
            insert(BTB_INFO) := btbInfo

            val hits = btbValids.zip(btbInfo.bia).map {case(valid, t) => valid && t === tag}
            val hit = hits.reduce(_ || _)
            insert(BTB_HIT) := hit

            val predictTarget = MuxOH(hits, btbInfo.bta)
            // val backwards = MuxOH(hits, btbInfo.backwards)
            val tracker = MuxOH(hits, btbInfo.tracker)

            // val fromPredictor = False
            // val DirectionPredictor = new StateMachine{
            //     val StronglyNotTaken = new State
            //     val WeaklyNotTaken = new State with EntryPoint
            //     val WeaklyTaken = new State
            //     val StronglyTaken = new State
            //     val wPort = infoRAM.io.write

            //     StronglyNotTaken
            //         .whenIsActive{
            //             fromPredictor := False
            //             when(branchJumpInst){
            //                 when(actuallyTaken){
            //                     goto(WeaklyNotTaken)
            //                 } otherwise {
            //                     goto(StronglyNotTaken)
            //                 }
            //             }
            //         }

            //     WeaklyNotTaken
            //         .whenIsActive{
            //             fromPredictor := False
            //             when(branchJumpInst){
            //                 when(actuallyTaken){
            //                     goto(WeaklyTaken)
            //                 } otherwise {
            //                     goto(StronglyNotTaken)
            //                 }
            //             }
            //         }

            //     WeaklyTaken
            //         .whenIsActive{
            //             fromPredictor := True
            //             when(branchJumpInst){
            //                 when(actuallyTaken){
            //                     goto(StronglyTaken)
            //                 } otherwise {
            //                     goto(WeaklyNotTaken)
            //                 }
            //             }
            //         }

            //     StronglyTaken
            //         .whenIsActive{
            //             fromPredictor := True
            //             when(branchJumpInst){
            //                 when(actuallyTaken){
            //                     goto(StronglyTaken)
            //                 } otherwise {
            //                     goto(WeaklyTaken)
            //                 }
            //             }
            //         }
            // }
            // prejump := backwards && hit && arbitration.isValidNotStuck
            prejump := tracker.msb && hit && arbitration.isValidNotStuck
            pcManager.preJump := prejump
            insert(fetchSignals.PREJUMP) := prejump
            pcManager.predictTarget := predictTarget
        }

        IF2 plug new Area{
            import IF2._
            // get the immediate number of branch instruction
            val inst = output(fetchSignals.INST)
            val immType = Mux(inst(29 downto 27) === B"010", ImmExtType.SI26, ImmExtType.SI16)
            val immExtForBranch = ImmExtForBranch()
            immExtForBranch.io.inst := inst
            immExtForBranch.io.immType := immType
            val imm = immExtForBranch.io.imm.asUInt
            insert(BRANCH_IMM) := imm
        }

        ISS plug new Area{
            import ISS._
            val src1 = input(decodeSignals.SRC1).asUInt
            val src2 = input(decodeSignals.SRC2).asUInt
            val bruOp = input(decodeSignals.BRUOp)

            val branch = Bool

            switch(bruOp){
                import myCPU.constants.BRUOpType._
                is(EQ){
                    branch := src1 === src2
                }
                is(NEQ){
                    branch := src1 =/= src2
                }
                is(LT){
                    branch := src1.asSInt < src2.asSInt
                }
                is(LTU){
                    branch := src1 < src2
                }
                is(GE){
                    branch := src1.asSInt >= src2.asSInt
                }
                is(GEU){
                    branch := src1 >= src2
                }
                default{
                    branch := False
                }
            }
            insert(BRANCH) := branch
            val imm = input(BRANCH_IMM)
            val jumpType = input(decodeSignals.JUMPType)
            val relativeTarget = input(fetchSignals.PC) + imm
            val absoluteTarget = src1 + imm
            // val opA = Mux(jumpType =/= JumpType.JIRL, input(fetchSignals.PC), src1)
            val branchTarget = Mux(jumpType =/= JumpType.JIRL, relativeTarget, absoluteTarget)
            insert(BRANCH_TARGET) := branchTarget
        }

        EXE1 plug new Area{
            import EXE1._

            val jumpType = input(decodeSignals.JUMPType)
            val branch = input(BRANCH)
            val branchTarget = input(BRANCH_TARGET)
            val jump = Bool

            jump := (jumpType === JumpType.Branch && branch) || (jumpType =/= JumpType.NONE && jumpType =/= JumpType.Branch)
        

            val preJump = input(fetchSignals.PREJUMP)
            val redirect = (jump ^ preJump) && arbitration.isValid
            branchJumpInst := jumpType =/= JumpType.NONE && jumpType =/= JumpType.JIRL && arbitration.isValid
            actuallyTaken := jump && arbitration.isValid
            actualBranchTarget := branchTarget

            // profiling
            // val count = RegInit(U(0, 32 bits))
            // val miss = RegInit(U(0, 32 bits))
            // when(branchJumpInst){
            //     count := count + 1
            //     when(redirect){
            //         miss := miss + 1
            //     }
            // }
            
            pcManager.redirect := redirect
            pcManager.redirectTarget := Mux(jump, branchTarget, input(fetchSignals.PC) + U(4))
            
            // val fetcher = service(classOf[FetcherPlugin])
            val fetcher = service(classOf[ICachePlugin])
            arbitration.flushNext setWhen(redirect)
            arbitration.haltItself setWhen(!fetcher.branchable && redirect)


            // update BTB
            val wPort = infoRAM.io.write
            val tag = input(BTB_TAG)
            val idx = input(fetchSignals.PC)(btbConfig.indexRange)
            wPort.valid := branchJumpInst && ~input(BTB_HIT)
            wPort.payload.address := idx

            val replaceWay = input(BTB_INFO).lru.asUInt
            val newInfo = input(BTB_INFO).copy()
            // 若 miss, 则初始化为01(weakly not taken)
            val oldTracker = input(BTB_HIT) ? input(BTB_INFO).tracker(replaceWay) | U(B"01")
            // val newForBranchInst = Mux(oldTracker === B"11" && actuallyTaken, B"11",
            //                         Mux(oldTracker === B"00" && !actuallyTaken, B"00",
            //                         Mux(actuallyTaken)))
            val newForBranchInst = Mux(actuallyTaken, 
                                        Mux(oldTracker === U(B"11"), U(B"11"), oldTracker + 1),
                                        Mux(oldTracker === U(B"00"), U(B"00"), oldTracker - 1))
            val newTracker = (jumpType === JumpType.Branch) ? newForBranchInst | U(B"10")
            newInfo.bia := input(BTB_INFO).bia
            // newInfo.backwards := input(BTB_INFO).backwards
            newInfo.tracker := input(BTB_INFO).tracker
            newInfo.bta := input(BTB_INFO).bta
            newInfo.bia(replaceWay) := tag
            // newInfo.backwards(replaceWay) := input(BRANCH_IMM).msb
            newInfo.tracker(replaceWay) := newTracker
            newInfo.bta(replaceWay) := branchTarget
            newInfo.lru := ~input(BTB_INFO).lru
            valids(idx)(replaceWay).set()
            wPort.payload.data := newInfo
        }
    }
}
