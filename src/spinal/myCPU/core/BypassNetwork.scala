package myCPU.core

import spinal.core._
import spinal.core.default
import myCPU.core.LA32R.RegAddrWidth
import myCPU.constants.ForwardType
import myCPU.constants.FuType


// 旁路网络
class BypassNetwork extends Component{
    val io = new Bundle{
        val rsISS = in(UInt(RegAddrWidth bits))
        val rdEXE1 = in(UInt(RegAddrWidth bits))
        val rdEXE2 = in(UInt(RegAddrWidth bits))
        val rdEXE3 = in(UInt(RegAddrWidth bits))
        val rdWB   = in(UInt(RegAddrWidth bits))
        val fuTypeEXE1 = in(FuType)
        val fuTypeEXE2 = in(FuType)
        val fuTypeEXE3 = in(FuType)
        val fuTypeWB = in(FuType)
        val regWriteValidEXE1 = in(Bool)
        val regWriteValidEXE2 = in(Bool)
        val regWriteValidEXE3 = in(Bool)
        val regWriteValidWB = in(Bool)
        val rsForwardType = out(Bits(ForwardType().getBitsWidth bits))
        val forwardable = out(Bool) // 若当前可以通过旁路获得最新的寄存器数据，则为true
    }
    io.rsForwardType := Select(
        (io.rsISS =/= 0 && io.rsISS === io.rdEXE1 && io.regWriteValidEXE1) -> ForwardType.FROMEXE1.asBits,
        (io.rsISS =/= 0 && io.rsISS === io.rdEXE2 && io.regWriteValidEXE2) -> ForwardType.FROMEXE2.asBits,
        (io.rsISS =/= 0 && io.rsISS === io.rdEXE3 && io.regWriteValidEXE3) -> ForwardType.FROMEXE3.asBits,
        (io.rsISS =/= 0 && io.rsISS === io.rdWB   && io.regWriteValidWB  ) -> ForwardType.FROMWB.asBits,
        default -> ForwardType.FROMREG.asBits
    )

    io.forwardable := !(((io.rsForwardType === ForwardType.FROMEXE1.asBits) && (io.fuTypeEXE1 === FuType.LSU || io.fuTypeEXE1 === FuType.MUL)) || 
                        ((io.rsForwardType === ForwardType.FROMEXE2.asBits) && (io.fuTypeEXE2 === FuType.LSU || io.fuTypeEXE2 === FuType.MUL)))
}
