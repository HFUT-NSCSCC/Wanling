package myCPU.pipeline.decode

import spinal.core._
import spinal.lib._
import myCPU.constants.ImmExtType

final case class ImmExt() extends Component{
    val io = new Bundle{
        val inst = in(Bits(32 bits))
        val immType = in(ImmExtType())
        val imm = out(Bits(32 bits))
    }

    val imm12 = io.inst(21 downto 10).asUInt
    val imm12U = imm12.resize(32 bits).asBits
    val imm12S = imm12.asSInt.resize(32 bits).asBits
    val imm8U = io.inst(17 downto 10).asUInt.resize(32 bits).asBits
    val imm16S = (io.inst(25 downto 10) << 2).asSInt.resize(32 bits).asBits
    val imm20S = io.inst(24 downto 5).asSInt.resize(32 bits).asBits
    val imm21S = (io.inst(4 downto 0) ## io.inst(25 downto 10)).asSInt.resize(32 bits).asBits
    val imm26S = ((io.inst(9 downto 0) ## io.inst(25 downto 10)) << 2).asSInt.resize(32 bits).asBits

    switch(io.immType){
        import ImmExtType._
        is(SI12){
            io.imm := imm12S
        }
        is(UI12){
            io.imm := imm12U
        }
        is(UI8){
            io.imm := imm8U
        }
        is(SI16){
            io.imm := imm16S
        }
        is(SI20){
            io.imm := imm20S
        }
        is(SI21){
            io.imm := imm21S
        }
        is(SI26){
            io.imm := imm26S
        }
        default{
            io.imm := 0
        }
    }
}
