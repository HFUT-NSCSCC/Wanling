package myCPU.pipeline.issue

import myCPU.builder.Plugin
import myCPU.core.Core
import spinal.core._

class IssuePlugin extends Plugin[Core]{
    override def setup(pipeline: Core): Unit = {

    }
  
    def build(pipeline: Core): Unit = {
        import pipeline._
        import pipeline.config._

        ISS plug new Area{
            import ISS._

            output(decodeSignals.REG_WRITE_VALID) := input(decodeSignals.REG_WRITE_VALID) && !arbitration.isFlushed
        }

        ID plug new Area{
            import ID._
            output(decodeSignals.REG_WRITE_VALID) := insert(decodeSignals.REG_WRITE_VALID) && !arbitration.isFlushed
        }

    }
}
