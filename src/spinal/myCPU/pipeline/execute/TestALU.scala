package myCPU.pipeline.execute

import myCPU.builder.Plugin
import myCPU.core.Core
import spinal.core._
import spinal.lib._

class TestALU extends Plugin[Core]{
    override def setup(pipeline: Core): Unit = {

    }

    def build(pipeline: Core): Unit = {
        import pipeline._
        import pipeline.config._

        EXE3 plug new Area{
            import EXE3._
            val result = (U(input(RJ)) + U(input(RK)))
            val addr = U(input(RA))
            insert(REG_WRITE_DATA) := result.asBits
            insert(REG_WRITE_ADDR) := addr.asBits
            insert(REG_WRITE_VALID) := True
        }
    }
  
}
