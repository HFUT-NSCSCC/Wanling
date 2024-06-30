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

            insert(writeSignals.REG_WRITE_VALID) := input(decodeSignals.REG_WRITE_VALID) && !arbitration.isFlushed
            insert(writeSignals.FUType) := input(decodeSignals.FUType)
            insert(writeSignals.REG_WRITE_ADDR) := input(decodeSignals.REG_WRITE_ADDR)
            insert(writeSignals.PC) := input(fetchSignals.PC)
        }

        // ID plug new Area{
        //     import ID._
        //     output(decodeSignals.REG_WRITE_VALID) := insert(decodeSignals.REG_WRITE_VALID) && !arbitration.isFlushed
        // }

    }
}
