package myCPU.pipeline.fetch

import myCPU.builder.Plugin
import myCPU.core.Core
import spinal.core._
import myCPU.InstBundle

class FetcherPlugin extends Plugin[Core]{
    val instBundle = new InstBundle()
    override def setup(pipeline: Core): Unit = {

    }

    def build(pipeline: Core): Unit = {
        import pipeline._
        import pipeline.config._

        IF1 plug new Area{
            import IF1._
            instBundle.en := arbitration.isValidNotStuck
            instBundle.addr := output(fetchSignals.PC)
            insert(fetchSignals.INST) := instBundle.en ? instBundle.rdata | B(0, 32 bits)
        }

        // IF2 plug new Area{
        //     // import IF2._
        //     // val inst = RegNextWhen[Bits](instBundle.rdata, !arbitration.isStuck, init = 0)

        // }

    }
  
}
