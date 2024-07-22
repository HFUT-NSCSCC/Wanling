package myCPU.core

import spinal.core._
import spinal.core.default
import myCPU.constants.LA32.RegAddrWidth
import myCPU.constants.ForwardType
import myCPU.constants.FuType

class BypassNetwork extends Component{
    val io = new Bundle{
        val rsISS = in(Bits(RegAddrWidth bits))
        val rdEXE1 = in(Bits(RegAddrWidth bits))
        val rdEXE2 = in(Bits(RegAddrWidth bits))
        val rdWB   = in(Bits(RegAddrWidth bits))
        val fuTypeEXE1 = in(FuType)
        val regWriteValidEXE1 = in(Bool)
        val regWriteValidEXE2 = in(Bool)
        val regWriteValidWB = in(Bool)
        val rsForwardType = out(Bits(ForwardType().getBitsWidth bits))
        val forwardable = out(Bool)
    }
    io.rsForwardType := Select(
        (io.rsISS =/= 0 && io.rsISS === io.rdEXE1 && io.regWriteValidEXE1) -> ForwardType.FROMEXE1.asBits,
        (io.rsISS =/= 0 && io.rsISS === io.rdEXE2 && io.regWriteValidEXE2) -> ForwardType.FROMEXE2.asBits,
        (io.rsISS =/= 0 && io.rsISS === io.rdWB   && io.regWriteValidWB  ) -> ForwardType.FROMWB.asBits,
        default -> ForwardType.FROMREG.asBits
    )

    io.forwardable := !((io.rsForwardType === ForwardType.FROMEXE1.asBits) && io.fuTypeEXE1 === FuType.LSU)
}