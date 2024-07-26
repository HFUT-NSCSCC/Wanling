package myCPU.peripheral
import spinal.core._
import spinal.lib.IMasterSlave
import myCPU.DataBundle
import spinal.lib._

case class BusBundle(targetName: String) extends Bundle with IMasterSlave{
    setName(targetName)
    val en = Bool
    val we = Bits(4 bits)
    val addr = Bits(32 bits)
    val wdata = Bits(32 bits)
    val rdata = Bits(32 bits)
    val rready = Bool
    val wready = Bool

    def asMaster(): Unit = {
        out(en, we, addr, wdata)
        in(rdata, wready, rready)
    }
}

object BridgeConf {
    def ADDR_BASE = B"32'x0000_0000"
    def ADDR_EXT = B"32'x0040_0000"
    def ADDR_MASK = B"32'x7fc0_0000"
}

class Bridge extends Component{
    val io = new Bundle{
        val dBus = slave(DataBundle())
        val toBaseCtrl = master(BusBundle("inst_sram"))
        val toExtCtrl = master(BusBundle("data_sram"))
        val conf = master(BusBundle("conf"))
    }

    val sel_base = (io.dBus.addr & BridgeConf.ADDR_MASK) === BridgeConf.ADDR_BASE
    val sel_ext = (io.dBus.addr & BridgeConf.ADDR_MASK) === BridgeConf.ADDR_EXT
    val sel_conf = !sel_base && !sel_ext

    val sel_base_r = RegNext(sel_base, init = False)
    val sel_ext_r  = RegNext(sel_ext, init = False)
    val sel_conf_r = RegNext(sel_conf, init = False)

    io.toBaseCtrl.en := io.dBus.en & sel_base
    io.toBaseCtrl.we := (io.toBaseCtrl.en #* 4) & io.dBus.we
    io.toBaseCtrl.addr := io.dBus.addr
    io.toBaseCtrl.wdata := io.dBus.wdata

    io.toExtCtrl.en := io.dBus.en & sel_ext
    io.toExtCtrl.we := (io.toExtCtrl.en #* 4) & io.dBus.we
    io.toExtCtrl.addr := io.dBus.addr
    io.toExtCtrl.wdata := io.dBus.wdata

    io.conf.en := io.dBus.en & sel_conf
    io.conf.we := (io.conf.en #* 4) & io.dBus.we
    io.conf.addr := io.dBus.addr
    io.conf.wdata := io.dBus.wdata

    io.dBus.rdata := ((sel_base_r #* 32) & io.toBaseCtrl.rdata) |
                     ((sel_ext_r  #* 32) & io.toExtCtrl.rdata) |
                     ((sel_conf_r #* 32) & io.conf.rdata)

    io.dBus.wready := (sel_base && io.toBaseCtrl.wready) || (sel_ext && io.toExtCtrl.wready) || (sel_conf && io.conf.wready)
    io.dBus.rvalid := (sel_base && io.toBaseCtrl.rready) || (sel_ext && io.toExtCtrl.rready) || (sel_conf && io.conf.rready)
}
