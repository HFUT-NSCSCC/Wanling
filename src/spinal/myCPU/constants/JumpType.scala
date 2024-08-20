package myCPU.constants

import spinal.core.SpinalEnum
import spinal.core.binarySequential


// 跳转类型
object JumpType extends SpinalEnum(binarySequential){
    val NONE = newElement()
    val Branch, JB, JBL, JIRL = newElement()
  
}
