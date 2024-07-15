package myCPU.pipeline.fetch

import myCPU.builder.Plugin
import myCPU.core.Core
import spinal.core._
import spinal.lib._
import myCPU.constants.LA32._
import myCPU.InstBundle
import myCPU.constants.ImmExtType

class PCManagerPlugin extends Plugin[Core]{
    val nextPC = UInt(32 bits)
    val correctTarget = UInt(32 bits)
    val correct = Bool

    // val instBundle = new InstBundle()
    
    // def jump(target: UInt): Unit = {
    //     jump := True
    //     nextPC := target
    // }
    override def setup(pipeline: Core): Unit = {
        
    }
    
    def build(pipeline: Core): Unit = {
        import pipeline._
        import pipeline.config._
        
        IF1 plug new Area{
            import IF1._
            val PCval = RegNextWhen[UInt](nextPC, !arbitration.isStuck, init = PC_INIT) 
            arbitration.haltByOther setWhen(ClockDomain.current.isResetActive)
            // instBundle.en := !arbitration.isStuck
            // instBundle.addr := PCval.asBits
            val inst = output(fetchSignals.INST)
            val isBranch = inst(31 downto 30) === B"01" && inst(29 downto 26) =/= B"0011"
            val IMMType = (inst(29 downto 26) === B"0100" || inst(29 downto 26) === B"0101") ? ImmExtType.SI26 | ImmExtType.SI16
            val immExtForBranch = ImmExtForBranch()
            immExtForBranch.io.inst := inst
            immExtForBranch.io.immType := IMMType
            val imm = immExtForBranch.io.imm.asUInt
            // 若为负数, 则预测为跳转
            val preJump = isBranch && imm(31)
            // TODO
            // nextPC := jump ? jumpTarget | 
            //             (imm(31)) ? (PCval + imm) | PCval + 4
            // nextPC := Select(
            //     (jump) -> jumpTarget,
            //     (preJump) -> (PCval + imm),
            //     default -> (PCval + 4)
            // )
            // nextPC := jump ? jumpTarget | 
            //         preJump ? (PCval + imm) | PCval + 4
            nextPC := Mux(correct, correctTarget,
                            Mux(preJump, PCval + imm, PCval + 4))
            insert(fetchSignals.PREJUMP) := preJump
            insert(fetchSignals.PC) := PCval.asBits
        }

    }
  
}
