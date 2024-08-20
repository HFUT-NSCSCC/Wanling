package myCPU.constants

import spinal.core.SpinalEnum
import spinal.core.binarySequential


// 功能部件
object FuType extends SpinalEnum(binarySequential){
    val ALU, BRU, LSU = newElement()
    val MUL = newElement()
  
}
