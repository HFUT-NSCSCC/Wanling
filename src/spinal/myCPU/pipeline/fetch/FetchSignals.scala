package myCPU.pipeline.fetch

import spinal.core._
import myCPU.builder._
import myCPU.core.LA32R._
import myCPU.core.CoreConfig

class FetchSignals(config: CoreConfig){
    object PC extends Stageable(UInt(PCWidth bits))
    object NPC extends Stageable(UInt(PCWidth bits))
    object INST extends Stageable(Bits(InstWidth bits))
    object PREJUMP extends Stageable(Bool())
}
