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
            val pcBeforeStuck = RegNextWhen[UInt](output(fetchSignals.PC), !arbitration.isStuck, init = PC_INIT)

            instBundle.addr := arbitration.isStuck ? pcBeforeStuck.asBits | output(fetchSignals.PC).asBits
            // instBundle.addr := output(fetchSignals.PC).asBits
            // arbitration.flushIt setWhen(instBundle.addr === PC_INIT)
            arbitration.haltItself setWhen(!instBundle.rvalid && arbitration.isValid)
        }
        
        IF2 plug new Area{
            import IF2._
            arbitration.haltItself setWhen(!instBundle.rresp && arbitration.isValid)
            val inst = RegNextWhen[Bits](instBundle.rdata, instBundle.rresp, init = 0)
            insert(fetchSignals.INST) := (instBundle.rresp) ? instBundle.rdata | inst

        }

    }
  
}
