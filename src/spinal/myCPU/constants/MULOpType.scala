package myCPU.constants

import spinal.core._
import spinal.lib._

object MULOpType extends SpinalEnum(binarySequential){
    val NONE = newElement()
    val MUL, MULH = newElement()
    val MULHU = newElement()
    val DIV, DIVU = newElement()
    val MOD, MODU = newElement()
}
