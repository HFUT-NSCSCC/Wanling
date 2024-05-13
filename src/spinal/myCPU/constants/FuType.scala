package myCPU.constants

import spinal.core.SpinalEnum
import spinal.core.binarySequential

object FuType extends SpinalEnum(binarySequential){
    val ALU, BRU, LSU = newElement()
  
}
