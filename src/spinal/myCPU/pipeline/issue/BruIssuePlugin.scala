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
            val bruSignals = new BRUSignals()
            bruSignals.SRC1 := output(decodeSignals.SRC1)
            bruSignals.SRC2 := output(decodeSignals.SRC2)
            bruSignals.SRC2_FROM := input(decodeSignals.SRC2_FROM)
            bruSignals.BRUOp := input(decodeSignals.BRUOp)
            bruSignals.IMM := input(decodeSignals.IMM)
            bruSignals.JUMPType := input(decodeSignals.JUMPType)
            insert(exeSignals.bruSignals) := bruSignals
        }
    }
}
