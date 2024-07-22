package myCPU.pipeline.decode

import spinal.core._
import myCPU.builder._
import myCPU.constants.LA32._
import myCPU.core.CoreConfig
import myCPU.constants._

class DecodeSignals(config: CoreConfig) {
    // object microOp extends Stageable(MicroOp
    object FUType extends Stageable(FuType())
    object IMM extends Stageable(Bits(DataWidth bits))
    object IMMExtType extends Stageable(ImmExtType())
    object JUMPType extends Stageable(JumpType())

    // ALU
    object SRC1Addr extends Stageable(Bits(RegAddrWidth bits))
    object SRC2Addr extends Stageable(Bits(RegAddrWidth bits))
    object SRC1 extends Stageable(Bits(DataWidth bits))
    object SRC2 extends Stageable(Bits(DataWidth bits))
    object ALUOp extends Stageable(ALUOpType())
    object SRC1_FROM extends Stageable(ALUOpSrc())
    object SRC2_FROM extends Stageable(ALUOpSrc())

    // BRU
    object BRUOp extends Stageable(BRUOpType())

    // LSU
    object MEM_READ extends Stageable(Bits(4 bits))
    object MEM_READ_UE extends Stageable(Bool)
    object MEM_WRITE extends Stageable(Bits(4 bits))
    // object MEM_RDATA extends Stageable(Bits(DataWidth bits))
    // object RA extends Stageable(Bits(32 bits))
    // object INST extends Stageable(Bits(InstWidth bits))
    object REG_WRITE_VALID extends Stageable(Bool)
    object REG_WRITE_ADDR extends Stageable(Bits(RegAddrWidth bits))
    // object REG_WRITE_DATA extends Stageable(Bits(DataWidth bits))
}

final case class MicroOp() extends Bundle {
    val FUType = FuType()
    val IMMExtType = ImmExtType()

    val SRC1Addr = Bits(RegAddrWidth bits)
    val SRC2Addr = Bits(RegAddrWidth bits)
    val SRC1 = Bits(DataWidth bits)
    val SRC2 = Bits(DataWidth bits)
    val ALUOp = ALUOpType()
    val SRC1_FROM_IMM = Bool
    val SRC2_FROM_IMM = Bool
    val RESULT = Bits(DataWidth bits)

    val BRUOp = BRUOpType()
    val IMM = Bits(DataWidth bits)
    val JUMPType = JumpType()

    val MEM_READ = Bits(4 bits)
    val MEM_READ_UE = Bool
    val MEM_WRITE = Bits(4 bits)
    val MEM_RDATA = Bits(DataWidth bits)
    val REG_WRITE_VALID = Bool
    val REG_WRITE_ADDR = Bits(RegAddrWidth bits)
    val REG_WRITE_DATA = Bits(DataWidth bits)
}
