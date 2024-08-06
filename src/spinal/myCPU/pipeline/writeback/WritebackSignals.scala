package myCPU.pipeline.writeback

import myCPU.core.CoreConfig
import myCPU.core.LA32R._
import spinal.core._
import myCPU.builder._
import myCPU.constants.JumpType
import myCPU.constants.FuType

final case class WritebackSignals(config: CoreConfig){
    object FUType_WB extends Stageable(FuType())
    object ALU_RESULT_WB extends Stageable(Bits(DataWidth bits))
    object MUL_RESULT_WB extends Stageable(Bits(DataWidth bits))
    object MEM_RDATA_WB extends Stageable(Bits(DataWidth bits))
    object MEM_RDATA_WB_RAW extends Stageable(Bits(DataWidth bits))
    object REG_WRITE_VALID_WB extends Stageable(Bool)
    object REG_WRITE_ADDR_WB extends Stageable(UInt(RegAddrWidth bits))
    object REG_WRITE_DATA_WB extends Stageable(Bits(DataWidth bits))
    object PC_WB extends Stageable(UInt(PCWidth bits))

}
