package myCPU.pipeline.execute

import myCPU.builder.Plugin
import myCPU.core.Core
import spinal.core._
import spinal.lib._
import myCPU.pipeline.fetch.PCManagerPlugin
import _root_.myCPU.constants.JumpType.JIRL

class BRUPlugin extends Plugin[Core]{
    override def setup(pipeline: Core): Unit = {

    }

    def build(pipeline: Core): Unit = { 
        import pipeline._
        import pipeline.config._

        EXE1 plug new Area{
            import EXE1._

            val src1 = input(SRC1).asUInt //rj
            val src2 = input(SRC2).asUInt //rd
            val bruOp = input(BRUOp)

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

            val imm = output(IMM)
            val jumpType = output(JUMPType)
            val branchTarget = (jumpType =/= JIRL) ? (input(PC).asUInt + imm.asUInt) | (src1 + imm.asUInt)

            val pcManager = service(classOf[PCManagerPlugin])
            jump := branch
            pcManager.jump := jump
            pcManager.jumpTarget := branchTarget

        }
    }
  
}
