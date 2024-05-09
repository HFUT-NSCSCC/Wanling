// package myCPU.pipeline.fetch

// import myCPU.builder.Plugin
// import myCPU.core.Core
// import spinal.core._
// import spinal.lib._

// class TestInst extends Plugin[Core]{
//     override def setup(pipeline: Core): Unit = {

//     }
  
//     def build(pipeline: Core): Unit = {
//         import pipeline._
//         import pipeline.config._

//         IF2 plug new Area{
//             import IF2._
//             insert(INST) := U"h12345678".asBits
//         }
//     }
// }
