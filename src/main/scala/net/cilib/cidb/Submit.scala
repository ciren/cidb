package net.cilib.cidb

import com.cedarsoftware.util.io._
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON
import java.io._
import java.net.URLClassLoader
import java.util._
import java.util.jar.JarFile
import scala.collection.JavaConversions._
import Tags._
import Util._

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

  def objectToJson(obj: Any) = {
    def mustEdit(s: String) = s.contains(".") && s.substring(s.indexOf(".")+ 1).contains(".")
    JSON.parse(JsonWriter.objectToJson(obj).split("\"") map {x => if (mustEdit(x)) x.substring(x.lastIndexOf(".") + 1) else x} mkString("\""))
  }

  def submit(specFile: String, resultFile: String, jarFile: String, user: String, p: Properties) = {

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

    val manifest = new JarFile(jarFile).getManifest
    val version = "" // TODO: get cilib revision from manifest

    val data = MongoDBObject(
      "algorithm" -> objectToJson(getAlgorithmMethod.invoke(simulation)),
      "measurements" -> objectToJson(getMeasurementSuiteMethod.invoke(simulation)),
      "problem" -> objectToJson(getProblemMethod.invoke(simulation)),
      "samples" -> getSamplesMethod.invoke(simulator)
    )

    val simObject = MongoDBObject(
      "specification" -> readFile(specFile),
      "results" -> readFile(resultFile),
      "data" -> data,
      "version" -> version,
      "date" -> new Date(),
      "user" -> user
    )

    val connection = connect(p)
    connection(p.getProperty("db_database"))(p.getProperty("db_simCol")) += simObject

    updateTagsCollection(data, connection, p)

    connection.close
  }
}
