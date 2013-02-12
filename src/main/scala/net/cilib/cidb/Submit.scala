package net.cilib.cidb

import com.mongodb.casbah.Imports._
import java.io._
import java.net.URLClassLoader
import java.util.ArrayList
import java.util.Properties
import net.liftweb.json.JsonParser._
import Util._
import Tags._

object Submit {

  def getSimulator(specFile: String, urlClassLoader: URLClassLoader) = {
    //TODO: get right sim
    val shellClass = Class.forName("net.sourceforge.cilib.simulator.SimulatorShell", true, urlClassLoader)

    val defaultConstructor = shellClass.getDeclaredConstructors()(0)
    defaultConstructor.setAccessible(true)

    val prepareMethod = shellClass.getDeclaredMethod("prepare", classOf[File])
    val sims = prepareMethod.invoke(defaultConstructor.newInstance(), new File(specFile)).asInstanceOf[ArrayList[_]]
    sims.get(0)
  }

  def submit(specFile: String, resultFile: String, jarFile: String, p: Properties) = {

    val urlClassLoader = new URLClassLoader(Array(new File(jarFile).toURI().toURL()))

    val simulatorClass = Class.forName("net.sourceforge.cilib.simulator.Simulator", true, urlClassLoader)
    val createSimulationMethod = simulatorClass.getDeclaredMethod("createSimulation")
    val getSamplesMethod = simulatorClass.getDeclaredMethod("getSamples")

    val simulationClass = Class.forName("net.sourceforge.cilib.simulator.Simulation", true, urlClassLoader)
    val getAlgorithmMethod = simulationClass.getDeclaredMethod("getAlgorithm")
    val getMeasurementSuiteMethod = simulationClass.getDeclaredMethod("getMeasurementSuite")
    val getProblemMethod = simulationClass.getDeclaredMethod("getProblem")

    val simulator = getSimulator(specFile, urlClassLoader)
    val simulation = createSimulationMethod.invoke(simulator)

    val data = MongoDBObject(
      "algorithm" -> Encoder.toJson(getAlgorithmMethod.invoke(simulation)),
      "measurements" -> Encoder.toJson(getMeasurementSuiteMethod.invoke(simulation)),
      "problem" -> Encoder.toJson(getProblemMethod.invoke(simulation)),
      "samples" -> getSamplesMethod.invoke(simulator)
    )

    val simObject = MongoDBObject(
      "specification" -> readFile(specFile),
      "results" -> readFile(resultFile),
      "data" -> data
    )

    val connection = connect(p)
    connection(p.getProperty("db_database"))(p.getProperty("db_simCol")) += simObject

    updateTagsCollection(data, connection, p)

    connection.close
  }
}
