package myCPU.pipeline.issue

import myCPU.builder.Plugin
import myCPU.core.Core
import spinal.core._
import myCPU.pipeline.execute.IntALUSignals

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
            intALUSignals.SRC1 := output(decodeSignals.SRC1)
            intALUSignals.SRC2 := output(decodeSignals.SRC2)
            intALUSignals.ALUOp := input(decodeSignals.ALUOp)
            intALUSignals.IMM := input(decodeSignals.IMM)
            intALUSignals.SRC1_FROM := input(decodeSignals.SRC1_FROM)
            intALUSignals.SRC2_FROM := input(decodeSignals.SRC2_FROM)
            insert(exeSignals.intALUSignals) := intALUSignals
            insert(writeSignals.FUTypeWB) := input(decodeSignals.FUType)
        }
    }
  
}
