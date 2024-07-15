package myCPU.pipeline.fetch

import spinal.core._
import spinal.lib._
import myCPU.constants.ImmExtType

final case class ImmExtForBranch() extends Component{
    val io = new Bundle{
        val inst = in(Bits(32 bits))
        val immType = in(ImmExtType())
        val imm = out(Bits(32 bits))
    }

    val imm16S = (io.inst(25 downto 10) << 2).asSInt.resize(32 bits).asBits
    val imm26S = ((io.inst(9 downto 0) ## io.inst(25 downto 10)) << 2).asSInt.resize(32 bits).asBits

    switch(io.immType){
        import ImmExtType._
        is(SI16){
            io.imm := imm16S
        }
        is(SI26){
            io.imm := imm26S
        }
        default{
            io.imm := 0
        }
    }
}
