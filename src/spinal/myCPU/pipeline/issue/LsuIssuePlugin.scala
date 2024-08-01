package myCPU.pipeline.issue

import myCPU.builder.Plugin
import myCPU.core.Core
import myCPU.pipeline.execute.LSUSignals
import spinal.core._

class LsuIssuePlugin extends Plugin[Core]{
    override def setup(pipeline: Core): Unit = {

    }
  
    def build(pipeline: Core): Unit = {
        import pipeline._
        import pipeline.config._

        ISS plug new Area{
            import ISS._

            val lsuSignals = new LSUSignals()
            // lsuSignals.SRC1 := output(decodeSignals.SRC1)
            lsuSignals.SRC2 := output(decodeSignals.SRC2)
            // lsuSignals.IMM := input(decodeSignals.IMM)
            lsuSignals.VADDR := input(decodeSignals.SRC1).asUInt + input(decodeSignals.IMM).asUInt
            lsuSignals.MEM_READ := input(decodeSignals.MEM_READ) & (arbitration.isValid #* 4)
            lsuSignals.MEM_READ_UE := input(decodeSignals.MEM_READ_UE)
            lsuSignals.MEM_WRITE := input(decodeSignals.MEM_WRITE) & (arbitration.isValid #* 4)
            insert(exeSignals.lsuSignals) := lsuSignals
        }
    }
}
