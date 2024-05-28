package myCPU.pipeline.fetch

import spinal.core._
import myCPU.builder._
import myCPU.constants.LA32._
import myCPU.core.CoreConfig

class FetchSignals(config: CoreConfig){
    object PC extends Stageable(Bits(PCWidth bits))
    object INST extends Stageable(Bits(InstWidth bits))
}
