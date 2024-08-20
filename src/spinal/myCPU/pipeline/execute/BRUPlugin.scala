package myCPU.pipeline.execute

import myCPU.builder.Plugin
import myCPU.core.Core
import spinal.core._
import spinal.lib._
import myCPU.pipeline.fetch.PCManagerPlugin
import _root_.myCPU.constants._
import myCPU.pipeline.fetch.FetcherPlugin
import myCPU.pipeline.fetch.ICachePlugin
import myCPU.builder.Stageable
// import myCPU.pipeline.fetch.ImmExtForBranch
import myCPU.pipeline.decode.ImmExt

// 分支执行单元, 已被集成在BPU(分支预测模块)中
class BRUPlugin extends Plugin[Core]{
    object BRANCH extends Stageable(Bool)
    object BRANCH_TARGET extends Stageable(UInt(32 bits))
    object BRANCH_IMM extends Stageable(UInt(32 bits))
    override def setup(pipeline: Core): Unit = {
        import pipeline._
    }
    
    def build(pipeline: Core): Unit = { 
        import pipeline._
        import pipeline.config._
        val pcManager = service(classOf[PCManagerPlugin])

        // 简单的分支预测, 根据跳转的方向进行静态预测
        IF2 plug new Area{
            import IF2._
            val inst = output(fetchSignals.INST)
            val isBranch = inst(31 downto 30) === B"01" && inst(29 downto 26) =/= B"0011"
            val immType = Mux(inst(29 downto 27) === B"010", ImmExtType.SI26, ImmExtType.SI16)
            val immExtForBranch = ImmExt()
            immExtForBranch.io.inst := inst
            immExtForBranch.io.immType := immType
            val imm = immExtForBranch.io.imm.asUInt
            insert(BRANCH_IMM) := imm

            val preJump = isBranch && imm.msb && arbitration.isValidNotStuck
            pcManager.preJump := preJump
            pcManager.predictTarget := input(fetchSignals.PC) + imm

            arbitration.flushNext setWhen(preJump)
            
            insert(fetchSignals.PREJUMP) := preJump
        }
        
        // 根据从寄存器中取出的数据, 判断是否进行跳转和跳转目标的计算(设计的似乎不太合理)
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
            pcManager.redirect := redirect
            pcManager.redirectTarget := Mux(jump, branchTarget, input(fetchSignals.PC) + U(4))
            
            // val fetcher = service(classOf[FetcherPlugin])
            val fetcher = service(classOf[ICachePlugin])
            arbitration.flushNext setWhen(redirect)
            arbitration.haltItself setWhen(!fetcher.branchable && redirect)
        }
    }
  
}
