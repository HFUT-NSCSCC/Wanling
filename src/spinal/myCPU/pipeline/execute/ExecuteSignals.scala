package myCPU.pipeline.execute

import spinal.core._
import myCPU.builder._
import myCPU.constants.LA32._
import myCPU.core.CoreConfig
import myCPU.constants.ALUOpType
import myCPU.constants.BRUOpType
import myCPU.constants.JumpType

class ExecuteSignals(config: CoreConfig) {
    object intALUSignals extends Stageable(IntALUSignals())
    object bruSignals extends Stageable(BRUSignals())
    object lsuSignals extends Stageable(LSUSignals())
    object memSignals extends Stageable(MemSignals())
}

final case class IntALUSignals() extends Bundle{
    // val SRC1Addr = Bits(RegAddrWidth bits)
    // val SRC2Addr = Bits(RegAddrWidth bits)
    val SRC1 = Bits(DataWidth bits)
    val SRC2 = Bits(DataWidth bits)
    val ALUOp = ALUOpType()
    val IMM = Bits(DataWidth bits)
    val SRC1_FROM_IMM = Bool
    val SRC2_FROM_IMM = Bool
    // val RESULT = Bits(DataWidth bits)

    // object SRC1Addr extends Stageable(Bits(RegAddrWidth bits))
    // object SRC2Addr extends Stageable(Bits(RegAddrWidth bits))
    // object SRC1 extends Stageable(Bits(DataWidth bits))
    // object SRC2 extends Stageable(Bits(DataWidth bits))
    // object ALUOp extends Stageable(ALUOpType())
    // object SRC1_FROM_IMM extends Stageable(Bool)
    // object SRC2_FROM_IMM extends Stageable(Bool)
    // object RESULT extends Stageable(Bits(DataWidth bits))
}

final case class BRUSignals() extends Bundle {
    val SRC1 = Bits(DataWidth bits)
    val SRC2 = Bits(DataWidth bits)
    val BRUOp = BRUOpType()
    val IMM = Bits(DataWidth bits)
    val JUMPType = JumpType()



    // object SRC1 extends Stageable(Bits(DataWidth bits))
    // object SRC2 extends Stageable(Bits(DataWidth bits))
    // object BRUOp extends Stageable(BRUOpType())
    // object IMM extends Stageable(Bits(DataWidth bits))
    // object JUMPType extends Stageable(JumpType())
}

final case class LSUSignals() extends Bundle {
    val SRC1 = Bits(DataWidth bits)
    val SRC2 = Bits(DataWidth bits)
    val IMM = Bits(DataWidth bits)
    val MEM_READ = Bits(4 bits)
    val MEM_READ_UE = Bool
    val MEM_WRITE = Bits(4 bits)
    // val MEM_WRITE_DATA = Bits(DataWidth bits)
    val MEM_ADDR = Bits(DataWidth bits)
    // val MEM_RDATA = Bits(DataWidth bits)
    // val REG_WRITE_VALID = Bool
    // val REG_WRITE_ADDR = Bits(RegAddrWidth bits)
    // val REG_WRITE_DATA = Bits(DataWidth bits)
}

final case class MemSignals() extends Bundle{
    val MEM_EN = Bool()
    val MEM_WE = Bits(4 bits)
    val MEM_ADDR = Bits(DataWidth bits)
    val MEM_WDATA = Bits(DataWidth bits)
}