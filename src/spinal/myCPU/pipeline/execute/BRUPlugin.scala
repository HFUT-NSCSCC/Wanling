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

        EXE1 plug new Area{
            import EXE1._
            val src1 = input(decodeSignals.SRC1).asUInt
            val src2 = input(decodeSignals.SRC2).asUInt
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
            val opA = Mux(jumpType =/= JumpType.JIRL, input(fetchSignals.PC), src1)
            val branchTarget = opA + imm.asUInt

            val pcManager = service(classOf[PCManagerPlugin])
            jump := (
                        (jumpType === JumpType.Branch && branch) || (jumpType =/= JumpType.NONE && jumpType =/= JumpType.Branch)
                    ) && arbitration.isValidNotStuck

            val preJump = input(fetchSignals.PREJUMP)
            val redirect = (jump ^ preJump) && arbitration.isValidNotStuck
            pcManager.redirect := redirect
            pcManager.redirectTarget := Mux(jump, branchTarget, input(fetchSignals.PC) + U(4))
            arbitration.flushNext setWhen(redirect)
                            
            // pcManager.jump := jump
            // pcManager.jumpTarget := branchTarget
            // arbitration.flushNext setWhen(jump)
        }
    }
  
}
