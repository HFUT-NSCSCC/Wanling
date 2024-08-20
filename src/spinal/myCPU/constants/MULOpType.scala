package myCPU.constants

import spinal.core._
import spinal.lib._


// 乘除余操作类型
object MULOpType extends SpinalEnum(binarySequential){
    val NONE = newElement()
    val MUL, MULH = newElement()
    val MULHU = newElement()
    val DIV, DIVU = newElement()
    val MOD, MODU = newElement()
}
