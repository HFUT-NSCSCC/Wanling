// package myCPU.builder

// import scala.collection.mutable
// import spinal.core._
// import spinal.lib._

// trait MultiPipeline extends Pipeline{
//     val pipelines = mutable.ArrayBuffer[Pipeline]()

//     override def setUpPlugins(): Unit = {
//         super.setUpPlugins()
//         pipelines.foreach(_.setUpPlugins())
//     }

//     override def buildPlugins(): Unit = {
//         super.buildPlugins()
//         pipelines.foreach(_.buildPlugins())
//     }

//     override def connectStages(): Unit = {
//         pipelines.foreach(_.connectStages())
//     }

//     override def build(): Unit = {
//         setUpPlugins()
//         buildPlugins()
//         connectStages()
//     }

//     def addPipeline(pipeline: Pipeline): Unit = {
//         pipeline.setGlobalCtx(this)
//         pipelines += pipeline
//     }

//     Component.current.addPrePopTask(() => build())
  
// }
