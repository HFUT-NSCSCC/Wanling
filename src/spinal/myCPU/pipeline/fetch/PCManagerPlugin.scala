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
    // val jumpTarget = UInt(32 bits)
    // val jump = Bool

    val preJump = Bool
    val predictTarget = UInt(32 bits)
    val redirect = Bool
    val redirectTarget = UInt(32 bits)

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
            val PCval = RegNextWhen[UInt](nextPC, !arbitration.isStuck ,init = PC_INIT) 
            arbitration.haltByOther setWhen(ClockDomain.current.isResetActive)
            // instBundle.en := !arbitration.isStuck
            // instBundle.addr := PCval.asBits
            
            // nextPC := jump ? jumpTarget | PCval + 4
            // nextPC := Mux(jump, jumpTarget,
            //         Mux(arbitration.isStuck, PCval,
            //          PCval + 4))
            // insert(fetchSignals.PC) := PCval.asBits

            val inst = output(fetchSignals.INST)
            val isBranch = inst(31 downto 30) === B"01" && inst(29 downto 26) =/= B"0011"
            val immType = Mux(inst(29 downto 27) === B"010", ImmExtType.SI26, ImmExtType.SI16)
            val immExtForBranch = ImmExtForBranch()
            immExtForBranch.io.inst := inst
            immExtForBranch.io.immType := immType
            val imm = immExtForBranch.io.imm.asUInt

            nextPC := Mux(redirect, redirectTarget,
                      Mux(preJump, predictTarget,
                      PCval + 4))
            
            insert(fetchSignals.PREJUMP) := preJump
            insert(fetchSignals.PC) := PCval.asBits

        }
        
        ID plug new Area{
            import ID._
            val inst = input(fetchSignals.INST)
            val isBranch = inst(31 downto 30) === B"01" && inst(29 downto 26) =/= B"0011"
            val immType = Mux(inst(29 downto 27) === B"010", ImmExtType.SI26, ImmExtType.SI16)
            val immExtForBranch = ImmExtForBranch()
            immExtForBranch.io.inst := inst
            immExtForBranch.io.immType := immType
            val imm = immExtForBranch.io.imm.asUInt

            preJump := isBranch && imm.msb && arbitration.isValidNotStuck
            predictTarget := input(fetchSignals.PC).asUInt + imm

            arbitration.flushNext setWhen(preJump)
            
            insert(fetchSignals.PREJUMP) := preJump

        }

    }
  
}
