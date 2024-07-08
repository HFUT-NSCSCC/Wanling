package myCPU.pipeline.execute

import myCPU.builder.Plugin
import myCPU.core.Core
import spinal.core._
import spinal.lib._
import myCPU.DataBundle

class LSUPlugin extends Plugin[Core]{
    val data = new DataBundle()
    // 设置数据端口的使能为寄存器，使其在默认状态下为False，避免译码信号未到达时候信号为未知，导致无法正常取指
    val data_en = RegInit(False)
    override def setup(pipeline: Core): Unit = {
        data.en := data_en

    }

    def build(pipeline: Core): Unit = {
        import pipeline._
        import pipeline.config._

        EXE1 plug new Area{
            import EXE1._

            val lsuSignals = input(exeSignals.lsuSignals)

            val src1 = lsuSignals.SRC1.asUInt //rj
            val imm = lsuSignals.IMM.asUInt // imm
            val vaddr = src1 + imm

            val memSignals = new MemSignals()
            memSignals.MEM_ADDR := vaddr.asBits & 0xFFFFFFFCL
            memSignals.MEM_EN := (lsuSignals.MEM_READ =/= B"0000" || lsuSignals.MEM_WRITE =/= B"0000")
            memSignals.MEM_WE := lsuSignals.MEM_WRITE & (arbitration.isValidNotStuck.asSInt.resize(4 bits).asBits)
            memSignals.MEM_WDATA := lsuSignals.SRC2
            memSignals.MEM_MASK := Select(
                (lsuSignals.MEM_READ === B"0001" || lsuSignals.MEM_WRITE === B"0001") -> (B"0001" |<< vaddr(1 downto 0)),
                (lsuSignals.MEM_READ === B"0011" || lsuSignals.MEM_WRITE === B"0011") -> (B"0011" |<< vaddr(1 downto 0)),
                (lsuSignals.MEM_READ === B"1111" || lsuSignals.MEM_WRITE === B"1111") -> B"1111",
                default -> B"0000"
            )
            when(arbitration.isValidNotStuck){
                data_en := memSignals.MEM_EN
            }
            insert(exeSignals.memSignals) := memSignals
        }
        
        EXE2 plug new Area{
            import EXE2._
            val memSignals = input(exeSignals.memSignals)
            val lsuSignals = input(exeSignals.lsuSignals)
            data.addr := memSignals.MEM_ADDR
            IF1.arbitration.haltItself setWhen(data_en && !memSignals.MEM_ADDR(22) && arbitration.isValidNotStuck)
            // memory read
            val rawData = Select(
                (lsuSignals.MEM_READ =/= B"0000") -> data.rdata,
                (lsuSignals.MEM_WRITE =/= B"0000") -> memSignals.MEM_WDATA,
                default -> B(0, 32 bits)
            )
            val rdata_ext = Bits(32 bits)
            switch(memSignals.MEM_MASK){
                is(B"0001"){
                    rdata_ext := Mux(lsuSignals.MEM_READ_UE, 
                                    rawData(7 downto 0).asUInt.resize(32 bits).asBits, 
                                    rawData(7 downto 0).asSInt.resize(32 bits).asBits)
                }
                is(B"0010"){
                    rdata_ext := Mux(lsuSignals.MEM_READ_UE, 
                                    rawData(15 downto 8).asUInt.resize(32 bits).asBits, 
                                    rawData(15 downto 8).asSInt.resize(32 bits).asBits)
                }
                is(B"0100"){
                    rdata_ext := Mux(lsuSignals.MEM_READ_UE, 
                                    rawData(23 downto 16).asUInt.resize(32 bits).asBits, 
                                    rawData(23 downto 16).asSInt.resize(32 bits).asBits)
                }
                is(B"1000"){
                    rdata_ext := Mux(lsuSignals.MEM_READ_UE, 
                                    rawData(31 downto 24).asUInt.resize(32 bits).asBits, 
                                    rawData(31 downto 24).asSInt.resize(32 bits).asBits)
                }
                is(B"0011"){
                    rdata_ext := Mux(lsuSignals.MEM_READ_UE, 
                                    rawData(15 downto 0).asUInt.resize(32 bits).asBits, 
                                    rawData(15 downto 0).asSInt.resize(32 bits).asBits)
                }
                is(B"1100"){
                    rdata_ext := Mux(lsuSignals.MEM_READ_UE, 
                                    rawData(31 downto 16).asUInt.resize(32 bits).asBits, 
                                    rawData(31 downto 16).asSInt.resize(32 bits).asBits)
                }
                default{
                    rdata_ext := rawData
                }
            }
            insert(writeSignals.MEM_RDATA) := rdata_ext
            
            // memory write
            data.we := memSignals.MEM_WE
            data.wdata := memSignals.MEM_WDATA
            
            
        }
    }
}
