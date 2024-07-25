package myCPU.pipeline.fetch

import myCPU.builder.Plugin
import myCPU.core.Core
import spinal.core._
import myCPU.InstBundle
import spinal.lib.master
import myCPU.core.LA32R.PC_INIT

class FetcherPlugin extends Plugin[Core]{
    val instBundle = master(InstBundle())
    override def setup(pipeline: Core): Unit = {

    }

    def build(pipeline: Core): Unit = {
        import pipeline._
        import pipeline.config._

        IF1 plug new Area{
            import IF1._
            instBundle.en := arbitration.isValid
            // stuck 时候 npc 就应该是当前的 pc
            instBundle.addr := arbitration.isStuck ? output(fetchSignals.PC).asBits | output(fetchSignals.NPC).asBits
            insert(fetchSignals.INST) := (!arbitration.isFlushed) ? instBundle.rdata | B(0, 32 bits)
            arbitration.flushIt setWhen(instBundle.addr === PC_INIT)
        }

        // IF2 plug new Area{
        //     // import IF2._
        //     // val inst = RegNextWhen[Bits](instBundle.rdata, !arbitration.isStuck, init = 0)

        // }

    }
  
}
