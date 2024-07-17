package myCPU.pipeline.execute

import myCPU.builder.Plugin
import myCPU.core.Core
import spinal.core._
import spinal.lib._
import myCPU.pipeline.fetch.PCManagerPlugin
import _root_.myCPU.constants._

class BRUPlugin extends Plugin[Core]{
    override def setup(pipeline: Core): Unit = {

    }

    def build(pipeline: Core): Unit = { 
        import pipeline._
        import pipeline.config._

        ID plug new Area{
            import ID._
            // val bruSignals = input(exeSignals.bruSignals)

            // val src1 = bruSignals.SRC1.asUInt //rj
            // val src2 = bruSignals.SRC2.asUInt //rd
            // val bruOp = bruSignals.BRUOp

            val src1 = output(decodeSignals.SRC1).asUInt
            val src2_from = output(decodeSignals.SRC2_FROM)
            val src2 = Select(
                (src2_from === ALUOpSrc.REG) -> U(output(decodeSignals.SRC2)),
                (src2_from === ALUOpSrc.IMM) -> U(output(decodeSignals.IMM)),
                (src2_from === ALUOpSrc.PC)  -> U(input(fetchSignals.PC)),
                default -> U(0, 32 bits)
            )
            val bruOp = output(decodeSignals.BRUOp)


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
            val imm = output(decodeSignals.IMM)
            val jumpType = output(decodeSignals.JUMPType)
            insert(writeSignals.JUMPType) := jumpType
            val branchTarget = (jumpType =/= JumpType.JIRL) ? (input(fetchSignals.PC).asUInt + imm.asUInt) | (src1 + imm.asUInt)
            insert(decodeSignals.RESULT) := branchTarget.asBits

            val pcManager = service(classOf[PCManagerPlugin])
            jump := (
                        (jumpType === JumpType.Branch && branch) || (jumpType =/= JumpType.NONE && jumpType =/= JumpType.Branch)
                    ) && arbitration.isValidNotStuck
                            
            pcManager.jump := jump
            pcManager.jumpTarget := branchTarget
            arbitration.flushNext setWhen(jump)
        }
    }
  
}
