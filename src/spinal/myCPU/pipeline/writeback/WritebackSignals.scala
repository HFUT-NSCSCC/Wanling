package myCPU.pipeline.writeback

import myCPU.core.CoreConfig
import myCPU.constants.LA32._
import spinal.core._
import myCPU.builder._
import myCPU.constants.JumpType
import myCPU.constants.FuType

final case class WritebackSignals(config: CoreConfig){
    object FUType extends Stageable(FuType())
    object ALU_RESLUT extends Stageable(Bits(DataWidth bits))
    object JUMPType extends Stageable(JumpType())
    object MEM_RDATA extends Stageable(Bits(DataWidth bits))
    // object REG_WRITE_VALID extends Stageable(Bool)
    object REG_WRITE_ADDR extends Stageable(Bits(RegAddrWidth bits))
    object REG_WRITE_DATA extends Stageable(Bits(DataWidth bits))

}
