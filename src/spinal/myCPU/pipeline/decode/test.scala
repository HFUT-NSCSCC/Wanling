// package myCPU.pipeline.decode

// import spinal.core._
// import myCPU.builder.Plugin
// import myCPU.core.Core

// class test extends Plugin[Core]{
//     override def setup(pipeline: Core): Unit = {

//     }

//     def build(pipeline: Core): Unit = {
//         import pipeline._
//         import pipeline.config._

//         ID plug new Area{
//             import ID._ 
//             insert(RD) := U("11111").asBits
//             insert(RJ) := U("00000").asBits
//         }

//         EXE1 plug new Area{
//             import EXE1._
//             // output(RD)
//             // output(RJ)
//             val result = (U(input(RD)) + U(input(RJ))).asBits.addAttribute("keep")
//             insert(RK) := result
//         }
//     }
  
// }
