package myCPU.pipeline.issue

import myCPU.builder.Plugin
import myCPU.core.Core
import myCPU.pipeline.execute.BRUSignals
import spinal.core._

class BruIssuePlugin extends Plugin[Core]{
    override def setup(pipeline: Core): Unit = {

    }
  
    def build(pipeline: Core): Unit = {
        import pipeline._
        import pipeline.config._

        ISS plug new Area{
            import ISS._
            val bruALUSignals = new BRUSignals()
            bruALUSignals.SRC1 := input(decodeSignals.SRC1)
            bruALUSignals.SRC2 := input(decodeSignals.SRC2)
            bruALUSignals.BRUOp := input(decodeSignals.BRUOp)
            bruALUSignals.IMM := input(decodeSignals.IMM)
            bruALUSignals.JUMPType := input(decodeSignals.JUMPType)
            insert(exeSignals.bruSignals) := bruALUSignals
        }
    }
}
