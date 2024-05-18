package myCPU.pipeline.fetch

import myCPU.builder.Plugin
import myCPU.core.Core
import spinal.core._
import spinal.lib._
import myCPU.constants.LA32._
import myCPU.InstBundle

class PCManagerPlugin extends Plugin[Core]{
    val nextPC = UInt(32 bits)
    val jumpTarget = UInt(32 bits)
    val jump = Bool

    val instBundle = new InstBundle()
    
    // def jump(target: UInt): Unit = {
    //     jump := True
    //     nextPC := target
    // }
    override def setup(pipeline: Core): Unit = {
        
    }
    
    def build(pipeline: Core): Unit = {
        import pipeline._
        import pipeline.config._
        
        IF1 plug new Area{
            import IF1._
            val PCval = RegNextWhen[UInt](nextPC, !arbitration.isStuck, init = PC_INIT) 
            arbitration.haltByOther setWhen(ClockDomain.current.isResetActive)
            instBundle.en := !arbitration.isStuck
            instBundle.addr := PCval.asBits
            nextPC := jump ? jumpTarget | PCval + 4
            insert(PC) := PCval.asBits
        }

        IF2 plug new Area{
            import IF2._

            insert(INST) := instBundle.rdata
        }
    }
  
}
