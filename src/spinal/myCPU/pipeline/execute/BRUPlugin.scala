package myCPU.pipeline.execute

import myCPU.builder.Plugin
import myCPU.core.Core
import spinal.core._
import spinal.lib._
import myCPU.pipeline.fetch.PCManagerPlugin
import myCPU.pipeline.decode.ScoreBoardPlugin
import _root_.myCPU.constants._

class BRUPlugin extends Plugin[Core]{
    override def setup(pipeline: Core): Unit = {

    }

    def build(pipeline: Core): Unit = { 
        import pipeline._
        import pipeline.config._

        ISS plug new Area{
            import ISS._
            val src1 = input(decodeSignals.SRC1).asUInt
            val src2_from = input(decodeSignals.SRC2_FROM)
            // -----有优先级-----
            // val src2 = Select(
            //     (src2_from === ALUOpSrc.REG) -> U(output(decodeSignals.SRC2)),
            //     (src2_from === ALUOpSrc.IMM) -> U(input(decodeSignals.IMM)),
            //     (src2_from === ALUOpSrc.PC)  -> U(input(fetchSignals.PC)),
            //     default -> U(0, 32 bits)
            // )


            // -----无优先级-----
            val src2 = UInt(32 bits)
            switch(src2_from){
                is(ALUOpSrc.REG){
                    src2 := U(output(decodeSignals.SRC2))
                }
                is(ALUOpSrc.IMM){
                    src2 := U(input(decodeSignals.IMM))
                }
                is(ALUOpSrc.PC){
                    src2 := U(input(fetchSignals.PC))
                }
                default{
                    src2 := U(0, 32 bits)
                }
            }
            val bruOp = input(decodeSignals.BRUOp)


            val branch = Bool
            val jump = Bool

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
                    branch := src1.asSInt > src2.asSInt
                }
                is(GEU){
                    branch := src1 > src2
                }
                default{
                    branch := False
                }
            }

            // val imm = bruSignals.IMM
            // val jumpType = bruSignals.JUMPType
            val imm = input(decodeSignals.IMM)
            val jumpType = input(decodeSignals.JUMPType)
            insert(writeSignals.JUMPType_WB) := jumpType
            val branchTarget = (jumpType =/= JumpType.JIRL) ? (input(fetchSignals.PC).asUInt + imm.asUInt) | (src1 + imm.asUInt)
            // insert(decodeSignals.RESULT) := branchTarget.asBits

            val pcManager = service(classOf[PCManagerPlugin])
            val scoreBoard = service(classOf[ScoreBoardPlugin])
            jump := (
                        (jumpType === JumpType.Branch && branch) || (jumpType =/= JumpType.NONE && jumpType =/= JumpType.Branch)
                    ) && arbitration.isValidNotStuck
                            
            pcManager.jump := jump
            pcManager.jumpTarget := branchTarget
            arbitration.flushNext setWhen(jump)
        }
    }
  
}
