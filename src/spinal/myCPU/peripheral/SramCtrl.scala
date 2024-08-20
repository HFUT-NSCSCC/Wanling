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


// Ext-SRAM控制器
// 状态机设计: https://jiunian-pic-1310185536.cos.ap-nanjing.myqcloud.com/image-20240820211735193.png
class ExtSramCtrl extends Component{
    val io = new Bundle{
        val fromBridgeExt = slave(BusBundle("data_sram"))
        val extSram = master(Sram("ext_sram"))
    }
    val extSram_r = Reg(Sram("ext_sram_r"))
    extSram_r.addr init(B(0, 20 bits))
    extSram_r.be_n init(B"1111")
    extSram_r.oe_n init(True)
    extSram_r.ce_n init(True)
    extSram_r.we_n init(True)
    val counter = Reg(UInt(4 bits))

    when(!extSram_r.we_n){
        io.extSram.data := extSram_r.data
    }

    // read the data
    io.fromBridgeExt.rdata := io.extSram.data
    io.extSram.addr := extSram_r.addr
    io.extSram.be_n := extSram_r.be_n
    io.extSram.ce_n := extSram_r.ce_n
    io.extSram.we_n := extSram_r.we_n
    io.extSram.oe_n := extSram_r.oe_n

    val fsm = new StateMachine {
        val IDLE = new State with EntryPoint
        val READ = new State
        val WRITE = new State
        // io.fromBridgeExt.wready := True
        // io.fromBridgeExt.rvalid := True
        // io.fromBridgeExt.rresp := False

        stateBoot.whenIsActive{
            io.fromBridgeExt.wready := True
            io.fromBridgeExt.rvalid := True
            io.fromBridgeExt.rresp := False
        }

        IDLE
            .onEntry{
                extSram_r.ce_n := True
                extSram_r.oe_n := True
                extSram_r.we_n := True
            }
            .whenIsActive{
                io.fromBridgeExt.wready := True
                io.fromBridgeExt.rvalid := True
                io.fromBridgeExt.rresp := False
                when(io.fromBridgeExt.en){
                    when(io.fromBridgeExt.we.orR){
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
                extSram_r.addr := io.fromBridgeExt.addr(21 downto 2)
                extSram_r.be_n := B"0000"
                extSram_r.ce_n := False
                extSram_r.oe_n := False
                extSram_r.we_n := True
            }
            .whenIsActive{
                counter := counter + 1
                when(counter === U(CYCLES_TO_READ - 1)){
                    io.fromBridgeExt.wready := True
                    io.fromBridgeExt.rvalid := True
                    io.fromBridgeExt.rresp := True
                    when(io.fromBridgeExt.en){
                        when(io.fromBridgeExt.we.orR){
                            goto(WRITE)
                        } otherwise {
                            // 再一次发起读请求
                            counter := 0
                            extSram_r.addr := io.fromBridgeExt.addr(21 downto 2)
                            extSram_r.be_n := B"0000"
                            extSram_r.ce_n := False
                            extSram_r.oe_n := False
                            extSram_r.we_n := True
                            goto(READ)
                        }
                    } otherwise {
                        goto(IDLE)
                    }
                } otherwise {
                    io.fromBridgeExt.wready := False
                    io.fromBridgeExt.rvalid := False
                    io.fromBridgeExt.rresp := False
                }
            }

        WRITE
            .onEntry{
                counter := 0
                extSram_r.data := io.fromBridgeExt.wdata
                extSram_r.addr := io.fromBridgeExt.addr(21 downto 2)
                extSram_r.be_n := ~io.fromBridgeExt.we
                extSram_r.ce_n := False
                extSram_r.oe_n := True
                extSram_r.we_n := False
            }
            .whenIsActive{
                counter := counter + 1
                when(counter === U(CYCLES_TO_WRITE - 1)) {
                    io.fromBridgeExt.wready := True
                    io.fromBridgeExt.rvalid := True
                    io.fromBridgeExt.rresp := False
                    when(io.fromBridgeExt.en){
                        when(io.fromBridgeExt.we.orR){
                            extSram_r.data := io.fromBridgeExt.wdata
                            extSram_r.addr := io.fromBridgeExt.addr(21 downto 2)
                            extSram_r.be_n := ~io.fromBridgeExt.we
                            extSram_r.ce_n := False
                            extSram_r.oe_n := True
                            extSram_r.we_n := False
                            counter := 0
                            goto(WRITE)
                        } otherwise {
                            goto(READ)
                        }
                    } otherwise {
                        goto(IDLE)
                    }
                } otherwise {
                    io.fromBridgeExt.wready := False
                    io.fromBridgeExt.rvalid := False
                    io.fromBridgeExt.rresp := False
                }
            }
    }
}

// base-sram控制器
// 状态机设计: https://jiunian-pic-1310185536.cos.ap-nanjing.myqcloud.com/image-20240820211839159.png
class BaseSramCtrl extends Component{
    val io = new Bundle{
        val instBundle = slave(InstBundle())
        val fromBridgeBase = slave(BusBundle("inst_sram"))
        val baseSram = master(Sram("base_sram"))
    }
    val baseSram_r = Reg(Sram("base_sram_r"))
    baseSram_r.addr init(B(0, 20 bits))
    baseSram_r.be_n init(B"1111")
    baseSram_r.oe_n init(True)
    baseSram_r.ce_n init(True)
    baseSram_r.we_n init(True)
    val counter = Reg(UInt(4 bits))
    val inst = RegInit(B(0, 32 bits))

    when(!baseSram_r.we_n){
        io.baseSram.data := baseSram_r.data
    }

    // read the data
    io.fromBridgeBase.rdata := io.baseSram.data
    
    io.baseSram.addr := baseSram_r.addr
    io.baseSram.be_n := baseSram_r.be_n
    io.baseSram.ce_n := baseSram_r.ce_n
    io.baseSram.we_n := baseSram_r.we_n
    io.baseSram.oe_n := baseSram_r.oe_n
    
    val fsm = new StateMachine {
        val FETCH = new State with EntryPoint
        val READ = new State
        val WRITE = new State
        // io.fromBridgeBase.wready := True
        // io.fromBridgeBase.rvalid := True
        // io.fromBridgeBase.rresp := False
        // io.instBundle.rvalid := False
        // io.instBundle.rresp := False

        stateBoot.whenIsActive{
            io.fromBridgeBase.wready := True
            io.fromBridgeBase.rvalid := True
            io.fromBridgeBase.rresp := False
            io.instBundle.rvalid := True
            io.instBundle.rresp := False
        }
        
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
                    io.instBundle.rresp := True
                    io.fromBridgeBase.rvalid := True
                    io.fromBridgeBase.wready := True
                    io.fromBridgeBase.rresp := False
                    // 如果lsu发起访存请求, 则优先访存
                    when(io.fromBridgeBase.en){
                        io.instBundle.rvalid := False
                        when(io.fromBridgeBase.we.orR){
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
                        io.instBundle.rvalid := True
                        goto(FETCH)
                    }
                } otherwise {
                    io.fromBridgeBase.wready := False
                    io.fromBridgeBase.rvalid := False
                    io.fromBridgeBase.rresp := False
                    io.instBundle.rvalid := False
                    io.instBundle.rresp := False
                }
            }

        READ
            .onEntry{
                counter := 0
                baseSram_r.addr := io.fromBridgeBase.addr(21 downto 2)
                baseSram_r.be_n := B"0000"
                baseSram_r.ce_n := ~io.fromBridgeBase.en
                baseSram_r.oe_n := (~(io.fromBridgeBase.en & ~(io.fromBridgeBase.we.orR)))
                baseSram_r.we_n := True
            }
            .whenIsActive{
                io.instBundle.rdata := inst
                counter := counter + 1
                when(counter === U(CYCLES_TO_READ - 1)) {
                    io.fromBridgeBase.rvalid := True
                    io.fromBridgeBase.wready := True
                    io.fromBridgeBase.rresp := True
                    io.instBundle.rresp := False
                    when(io.fromBridgeBase.en){
                        io.instBundle.rvalid := False
                        when(io.fromBridgeBase.we.orR){
                            goto(WRITE)
                        } otherwise {
                            counter := 0
                            baseSram_r.addr := io.fromBridgeBase.addr(21 downto 2)
                            baseSram_r.be_n := B"0000"
                            baseSram_r.ce_n := ~io.fromBridgeBase.en
                            baseSram_r.oe_n := (~(io.fromBridgeBase.en & ~(io.fromBridgeBase.we.orR)))
                            baseSram_r.we_n := True
                            goto(READ)
                        }
                    } otherwise {
                        io.instBundle.rvalid := True
                        goto(FETCH)
                    }
                } otherwise {
                    io.fromBridgeBase.wready := False
                    io.fromBridgeBase.rvalid := False
                    io.fromBridgeBase.rresp := False
                    io.instBundle.rvalid := False
                    io.instBundle.rresp := False
                }
            }

        WRITE
            .onEntry{
                counter := 0
                baseSram_r.data := io.fromBridgeBase.wdata
                baseSram_r.addr := io.fromBridgeBase.addr(21 downto 2)
                baseSram_r.be_n := ~io.fromBridgeBase.we
                baseSram_r.ce_n := False
                baseSram_r.oe_n := True
                baseSram_r.we_n := False
            }
            .whenIsActive{
                counter := counter + 1
                io.instBundle.rdata := inst
                when(counter === U(CYCLES_TO_WRITE - 1)) {
                    io.fromBridgeBase.rvalid := True
                    io.fromBridgeBase.wready := True
                    io.fromBridgeBase.rresp := False
                    io.instBundle.rresp := False
                    when(io.fromBridgeBase.en){
                        io.instBundle.rvalid := False
                        when(io.fromBridgeBase.we.orR){
                            counter := 0
                            baseSram_r.data := io.fromBridgeBase.wdata
                            baseSram_r.addr := io.fromBridgeBase.addr(21 downto 2)
                            baseSram_r.be_n := ~io.fromBridgeBase.we
                            baseSram_r.ce_n := False
                            baseSram_r.oe_n := True
                            baseSram_r.we_n := False
                            goto(WRITE)
                        } otherwise {
                            goto(READ)
                        }
                    } otherwise {
                        io.instBundle.rvalid := True
                        goto(FETCH)
                    }
                } otherwise {
                    io.fromBridgeBase.wready := False
                    io.fromBridgeBase.rvalid := False
                    io.fromBridgeBase.rresp := False
                    io.instBundle.rvalid := False
                    io.instBundle.rresp := False
                }
            }
            .onExit{
            }
    }
}
