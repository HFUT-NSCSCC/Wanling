package myCPU.peripheral

import spinal.core._
import spinal.lib._
import spinal.lib.fsm._
import myCPU.core.LA32R.CYCLES_TO_READ
import myCPU.core.LA32R.CYCLES_TO_WRITE
import myCPU.InstBundle
case class Sram(ramname: String) extends Bundle with IMasterSlave{
    setName(ramname)
    val data = Analog(Bits(32 bits))
    val addr = Bits(20 bits)
    val be_n = Bits(4 bits)
    val ce_n = Bool
    val oe_n = Bool
    val we_n = Bool

    def asMaster(): Unit = {
        inout(data)
        out(addr, be_n, ce_n, oe_n, we_n)
    }

}

class ExtSramCtrl extends Component{
    val io = new Bundle{
        val dataSram = slave(BusBundle("data_sram"))
        val extSram = master(Sram("ext_sram"))
    }
    val extSram_r = Reg(Sram("ext_sram_r"))
    extSram_r.ce_n init(True)
    val counter = Reg(UInt(4 bits))
    val do_store = RegInit(False)
    val wdata = RegInit(B(0, 32 bits))
    io.dataSram.do_store := do_store

    when(do_store){
        io.extSram.data := wdata
    }

    // read the data
    io.dataSram.rdata := io.extSram.data
    io.extSram.addr := extSram_r.addr
    io.extSram.be_n := extSram_r.be_n
    io.extSram.ce_n := extSram_r.ce_n
    io.extSram.we_n := extSram_r.we_n
    io.extSram.oe_n := extSram_r.oe_n

    val fsm = new StateMachine {
        val IDLE = new State with EntryPoint
        val READ = new State
        val WRITE = new State

        IDLE
            .onEntry{
                extSram_r.ce_n := True
                extSram_r.oe_n := True
                extSram_r.we_n := True
            }
            .whenIsActive{
                when(io.dataSram.en){
                    when(io.dataSram.we.orR){
                        goto(WRITE)
                    } otherwise {
                        goto(READ)
                    }
                } otherwise {
                    goto(IDLE)
                }
            }

        READ
            .onEntry{
                counter := 0
                extSram_r.addr := io.dataSram.addr(21 downto 2)
                extSram_r.be_n := B"0000"
                extSram_r.ce_n := False
                extSram_r.oe_n := False
                extSram_r.we_n := True
            }
            .whenIsActive{
                counter := counter + 1
                when(counter === U(CYCLES_TO_READ - 1)){
                    when(io.dataSram.en){
                        when(io.dataSram.we.orR){
                            goto(WRITE)
                        } otherwise {
                            counter := 0
                            extSram_r.addr := io.dataSram.addr(21 downto 2)
                            extSram_r.be_n := B"0000"
                            extSram_r.ce_n := False
                            extSram_r.oe_n := False
                            extSram_r.we_n := True
                            goto(READ)
                        }
                    } otherwise {
                        goto(IDLE)
                    }
                }
            }

        WRITE
            .onEntry{
                counter := 0
                wdata := io.dataSram.wdata
                extSram_r.addr := io.dataSram.addr(21 downto 2)
                extSram_r.be_n := ~io.dataSram.we
                extSram_r.ce_n := False
                extSram_r.oe_n := True
                extSram_r.we_n := False
                do_store := True
            }
            .whenIsActive{
                counter := counter + 1
                when(counter === U(CYCLES_TO_WRITE - 1)) {
                    when(io.dataSram.en){
                        when(io.dataSram.we.orR){
                            goto(WRITE)
                        } otherwise {
                            goto(READ)
                        }
                    } otherwise {
                        goto(IDLE)
                    }
                }
            }
            .onExit{
                do_store := False
            }
    }
}

class BaseSramCtrl extends Component{
    val io = new Bundle{
        val instBundle = slave(InstBundle())
        val instSram = slave(BusBundle("inst_sram"))
        val baseSram = master(Sram("base_sram"))
    }
    val baseSram_r = Reg(Sram("base_sram_r"))
    baseSram_r.ce_n init(True)
    val counter = Reg(UInt(4 bits))
    val do_store = RegInit(False)
    val wdata = RegInit(B(0, 32 bits))
    io.instSram.do_store := do_store
    val inst = RegInit(B(0, 32 bits))

    when(do_store){
        io.baseSram.data := wdata
    }

    // read the data
    io.instSram.rdata := io.baseSram.data
    
    io.baseSram.addr := baseSram_r.addr
    io.baseSram.be_n := baseSram_r.be_n
    io.baseSram.ce_n := baseSram_r.ce_n
    io.baseSram.we_n := baseSram_r.we_n
    io.baseSram.oe_n := baseSram_r.oe_n
    
    val fsm = new StateMachine {
        val FETCH = new State with EntryPoint
        val READ = new State
        val WRITE = new State
        
        // io.instBundle.rdata := (io.baseSram.oe_n || stateReg =/= FETCH.refOwner) ? inst | io.baseSram.data
        val doInstFetch = RegInit(False)
        io.instBundle.rdata := io.baseSram.data
        FETCH
            .onEntry{
                counter := 0
                baseSram_r.be_n := B"0000"
                baseSram_r.addr := io.instBundle.addr(21 downto 2)
                baseSram_r.ce_n := ~io.instBundle.en
                baseSram_r.oe_n := ~io.instBundle.en
                baseSram_r.we_n := True
            }
            .whenIsActive{
                counter := counter + 1
                io.instBundle.rdata := io.baseSram.oe_n ? inst | io.baseSram.data
                when(!io.baseSram.oe_n){
                    inst := io.baseSram.data
                }
                when(counter === U(CYCLES_TO_READ - 1)) {
                    when(io.instSram.en){
                        when(io.instSram.we.orR){
                            goto(WRITE)
                        } otherwise {
                            goto(READ)
                        }
                    } otherwise {
                        counter := 0
                        baseSram_r.be_n := B"0000"
                        baseSram_r.addr := io.instBundle.addr(21 downto 2)
                        baseSram_r.ce_n := ~io.instBundle.en
                        baseSram_r.oe_n := ~io.instBundle.en
                        baseSram_r.we_n := True
                        goto(FETCH)
                    }
                }
            }

        READ
            .onEntry{
                counter := 0
                baseSram_r.addr := io.instSram.addr(21 downto 2)
                baseSram_r.be_n := B"0000"
                baseSram_r.ce_n := ~io.instSram.en & ~io.instBundle.en
                baseSram_r.oe_n := (~(io.instSram.en & ~(io.instSram.we.orR))) & (~(io.instBundle.en & True))
                baseSram_r.we_n := True
            }
            .whenIsActive{
                io.instBundle.rdata := inst
                counter := counter + 1
                when(counter === U(CYCLES_TO_READ - 1)) {
                    when(io.instSram.en){
                        when(io.instSram.we.orR){
                            goto(WRITE)
                        } otherwise {
                            counter := 0
                            baseSram_r.addr := io.instSram.addr(21 downto 2)
                            baseSram_r.be_n := B"0000"
                            baseSram_r.ce_n := ~io.instSram.en & ~io.instBundle.en
                            baseSram_r.oe_n := (~(io.instSram.en & ~(io.instSram.we.orR))) & (~(io.instBundle.en & True))
                            baseSram_r.we_n := True
                            goto(READ)
                        }
                    } otherwise {
                        goto(FETCH)
                    }
                }
            }

        WRITE
            .onEntry{
                counter := 0
                wdata := io.instSram.wdata
                baseSram_r.addr := io.instSram.addr(21 downto 2)
                baseSram_r.be_n := ~io.instSram.we
                baseSram_r.ce_n := False
                baseSram_r.oe_n := True
                baseSram_r.we_n := False
                do_store := True
            }
            .whenIsActive{
                counter := counter + 1
                io.instBundle.rdata := inst
                when(counter === U(CYCLES_TO_WRITE - 1)) {
                    when(io.instSram.en || io.instBundle.en){
                        when(io.instSram.we.orR){
                            goto(WRITE)
                        } otherwise {
                            goto(READ)
                        }
                    } otherwise {
                        goto(FETCH)
                    }
                }
            }
            .onExit{
                do_store := False
            }
    }
}
