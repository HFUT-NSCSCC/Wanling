package myCPU.pipeline.issue

import myCPU.builder.Plugin
import myCPU.core.Core
import spinal.core._
import myCPU.pipeline.execute.IntALUSignals
import myCPU.constants.OpSrc

class IntIssuePlugin extends Plugin[Core]{
    override def setup(pipeline: Core): Unit = {

    }

    def build(pipeline: Core): Unit = {
        import pipeline._
        import pipeline.config._

        ISS plug new Area{
            import ISS._
            // 发射整数运算信号
            // 目前不做仲裁
            val intALUSignals = new IntALUSignals()
            val src1 = Bits(32 bits)
            val src2 = Bits(32 bits)
            switch(input(decodeSignals.SRC1_FROM)) {
                is(OpSrc.REG) {
                    src1 := (output(decodeSignals.SRC1))
                }
                is(OpSrc.IMM) {
                    src1 := (input(decodeSignals.IMM))
                }
                is(OpSrc.PC) {
                    src1 := (input(fetchSignals.PC))
                }
                default {
                    src1 := B(0, 32 bits)
                }
            }

            switch(input(decodeSignals.SRC2_FROM)) {
                is(OpSrc.REG) {
                    src2 := (output(decodeSignals.SRC2))
                }
                is(OpSrc.IMM) {
                    src2 := (input(decodeSignals.IMM))
                }
                is(OpSrc.PC) {
                    src2 := (input(fetchSignals.PC))
                }
                default {
                    src2 := B(0, 32 bits)
                }
            }
            intALUSignals.SRC1 := src1
            intALUSignals.SRC2 := src2
            intALUSignals.ALUOp := input(decodeSignals.ALUOp)
            insert(exeSignals.intALUSignals) := intALUSignals
            // insert(writeSignals.FUTypeWB) := input(decodeSignals.FUType)
            // insert(writeSignals.REG_WRITE_VALID) := input(decodeSignals.REG_WRITE_VALID)
        }
    }
  
}
