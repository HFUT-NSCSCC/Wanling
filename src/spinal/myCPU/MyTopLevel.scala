package myCPU

import spinal.core._
import myCPU.core.Core
import myCPU.core.CoreConfig

object MyTopLevelVerilog extends App {
  Config.spinal.generateVerilog(new MyCPU(CoreConfig()))
}

object MyTopLevelVhdl extends App {
  Config.spinal.generateVhdl(new Core(CoreConfig()))
}
