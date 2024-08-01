package myCPU.pipeline.execute

import spinal.core._
import myCPU.builder.Plugin
import myCPU.core.Core
import myCPU.builder.Stageable
import NOP.blackbox.execute.Multiplier

class MulPlugin extends Plugin[Core]{
    object MUL_LL extends Stageable(UInt(32 bits))
    object MUL_LH extends Stageable(SInt(34 bits))
    object MUL_HL extends Stageable(SInt(34 bits))
    override def setup(pipeline: Core): Unit = {

    }

    def build(pipeline: Core): Unit = {
        import pipeline._
        import pipeline.config._

        val multInStage = EXE1 plug new Area{
            import EXE1._
            val aluSignals = input(exeSignals.intALUSignals)
            val a = aluSignals.SRC1
            val b = aluSignals.SRC2

            val mult = new Multiplier()
            mult.io.A := a.asUInt
            mult.io.B := b.asUInt

            // val aULow = a(15 downto 0).asUInt
            // val bULow = b(15 downto 0).asUInt
            // val aSLow = (False ## a(15 downto 0)).asSInt
            // val bSLow = (False ## b(15 downto 0)).asSInt
            // val aHigh = (((True && a.msb) ## a(31 downto 16))).asSInt
            // val bHigh = (((True && b.msb) ## b(31 downto 16))).asSInt
            // insert(MUL_LL) := aULow * bULow
            // insert(MUL_LH) := aSLow * bHigh
            // insert(MUL_HL) := aHigh * bSLow
        }

        EXE2 plug new Area{
            import EXE2._

            insert(writeSignals.MUL_RESULT_WB) := multInStage.mult.io.P.asBits(31 downto 0)

            // insert(writeSignals.MUL_RESULT_WB) := (S(0, MUL_HL.dataType.getWidth + 16 + 2 bit) + 
            //                         (False ## input(MUL_LL)).asSInt + 
            //                         (input(MUL_LH) << 16) + 
            //                         (input(MUL_HL) << 16))(31 downto 0).asBits
        }

    }
  
}
