package myCPU.pipeline.execute

import spinal.core._
import myCPU.builder.Plugin
import myCPU.core.Core
import myCPU.builder.Stageable
import NOP.blackbox.execute.Multiplier
import myCPU.constants.MULOpType
import spinal.lib.math.MixedDivider
import spinal.lib.math.UnsignedDivider
import myCPU.blackbox.Divider


// 乘除余插件, 利用了multiplier和divider ip核实现
class MulPlugin extends Plugin[Core]{
    object OPA extends Stageable(SInt(32 bits))
    object OPB extends Stageable(SInt(32 bits))
    object SIGNED extends Stageable(Bool)
    object DIVISION extends Stageable(Bool)
    override def setup(pipeline: Core): Unit = {

    }

    def build(pipeline: Core): Unit = {
        import pipeline._
        import pipeline.config._
        val mult = new Multiplier()
        // val divider = new MixedDivider(32, 32, false)
        val divider = new Divider()

        val multInStage = EXE1 plug new Area{
            import EXE1._
            val aluSignals = input(exeSignals.intALUSignals)
            val a = aluSignals.SRC1.asSInt
            val b = aluSignals.SRC2.asSInt
            insert(OPA) := a
            insert(OPB) := b
            val signed = !(input(decodeSignals.MULOp) === MULOpType.MULHU || 
                         input(decodeSignals.MULOp) === MULOpType.DIVU || 
                         input(decodeSignals.MULOp) === MULOpType.MODU)
            insert(SIGNED) := signed
            val absA = a.abs(signed)
            val absB = b.abs(signed)

            mult.io.A := absA
            mult.io.B := absB


            val isDivision = arbitration.isValid && (input(decodeSignals.MULOp) === MULOpType.DIV || input(decodeSignals.MULOp) === MULOpType.DIVU || 
                             input(decodeSignals.MULOp) === MULOpType.MOD || input(decodeSignals.MULOp) === MULOpType.MODU)
            insert(DIVISION) := isDivision
            divider.io.s_axis_dividend_tvalid := isDivision
            divider.io.s_axis_dividend_tdata := absA.asBits
            divider.io.s_axis_divisor_tvalid := isDivision
            divider.io.s_axis_divisor_tdata := absB.asBits

            // divider.io.flush := False
            // divider.io.cmd.valid := isDivision
            // divider.io.cmd.signed := signed
            // divider.io.cmd.numerator := a.asBits
            // divider.io.cmd.denominator := b.asBits

            // val divider = new MixedDivider()

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

        EXE3 plug new Area{
            import EXE3._
            val a = input(OPA)
            val b = input(OPB)

            val signed = input(SIGNED)
            val mulResult = mult.io.P.twoComplement(signed && (a.sign ^ b.sign)).asBits(0, 64 bits)

            // divider.io.rsp.ready := !arbitration.isStuckByOthers
            // 若除法器未完成计算, 则对流水线进行阻塞
            arbitration.haltItself setWhen(arbitration.isValidOnEntry && input(DIVISION) && !divider.io.m_axis_dout_tvalid)
            val absQuotient = divider.io.m_axis_dout_tdata(63 downto 32).asUInt
            val absRemainder = divider.io.m_axis_dout_tdata(31 downto 0).asUInt
            val quotient = absQuotient.twoComplement(signed && (a.sign ^ b.sign)).asBits(0, 32 bits)
            val remainder = absRemainder.twoComplement(signed && a.sign).asBits(0, 32 bits)

            // insert(writeSignals.MUL_RESULT_WB) := (input(decodeSignals.MULOp) === MULOpType.MUL) ? 
            //                                             mulResult(31 downto 0) | 
            //                                             mulResult(63 downto 32)

            switch(input(decodeSignals.MULOp)){
                is(MULOpType.MUL){
                    insert(writeSignals.MUL_RESULT_WB) := mulResult(31 downto 0)
                }
                is(MULOpType.MULH, MULOpType.MULHU){
                    insert(writeSignals.MUL_RESULT_WB) := mulResult(63 downto 32)
                }
                is(MULOpType.DIV, MULOpType.DIVU){
                    insert(writeSignals.MUL_RESULT_WB) := quotient
                }
                is(MULOpType.MOD, MULOpType.MODU){
                    insert(writeSignals.MUL_RESULT_WB) := remainder
                }
                default{
                    insert(writeSignals.MUL_RESULT_WB) := 0
                }
            }

            // insert(writeSignals.MUL_RESULT_WB) := (S(0, MUL_HL.dataType.getWidth + 16 + 2 bit) + 
            //                         (False ## input(MUL_LL)).asSInt + 
            //                         (input(MUL_LH) << 16) + 
            //                         (input(MUL_HL) << 16))(31 downto 0).asBits
        }

    }
  
}
