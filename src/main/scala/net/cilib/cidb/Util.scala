package net.cilib.cidb

import com.mongodb.casbah.MongoConnection
import java.io._
import java.util.Properties
import org.apache.commons.cli.{Option => Opt, Options}
import scala.io.Source._
import scala.tools.jline.console.ConsoleReader

object Util {

  def connect(p: Properties) = {
    val connection = MongoConnection(p.getProperty("db_host"), p.getProperty("db_port").toInt)
    //TODO: authenticate
    connection
  }

  def exit(err: String, e: Int) = {
    println(err)
    System.exit(e)
  }

  def readFile(f: String) = {
    val source = fromFile(f)
    val contents = source.mkString
    source.close
    contents
  }

  def createOptions = {
    val o = new Options()

    val help = new Opt("h", "help", false, "Display CIdb usage information")
    val submit = new Opt("s", "submit", false, "Submit a simulation to add to the database")
    val search = new Opt("g", "search", true, "Search the database using the given search string")
    val results = new Opt("r", "results", true, "File containing the results of the simulation")
    val spec = new Opt("f", "spec", true, "File containing the specification of the simulation")
    val jar = new Opt("j", "jar", true, "Jar file used for the simulation")
    val config = new Opt("c", "config", true, "CIdb configuration file (default: \"cidb.conf\")")

    search.setArgName("search string")
    results.setArgName("results file")
    spec.setArgName("specification file")
    config.setArgName("config file")
    jar.setArgName("jar file")

    o.addOption(help);
    o.addOption(submit);
    o.addOption(search);
    o.addOption(results);
    o.addOption(spec);
    o.addOption(jar);
    o.addOption(config);

    o
  }

  def config(file: String): Properties = {
    try {
      val p = new Properties()
      p.load(new FileInputStream(file)); p
    } catch {
      case _ => {
        println("Warning: Could not load \""+ file + "\".")
        val input = new ConsoleReader().readLine("Create new configuration? (yes/no): ")

        if (!List("yes", "y").exists(_.equalsIgnoreCase(input)))
          exit("Error: No CIdb configuration found.", 1)
        createConfig(file).get
      }
    }
  }

  def createConfig(file: String) = {
    try {
      val reader = new ConsoleReader()
      val p = new Properties()

      p.setProperty("db_host", reader.readLine("Enter user database host: "))
      p.setProperty("db_port", reader.readLine("Enter user database port: "))
      p.setProperty("db_database", reader.readLine("Enter database name: "))
      p.setProperty("db_simCol", reader.readLine("Enter simulation collection name: "))
      p.setProperty("db_tagCol", reader.readLine("Enter tag collection name: "))
      p.setProperty("db_user", reader.readLine("Enter database username: "))
      p.setProperty("db_pass", reader.readLine("Enter database password: "))

      p.store(new FileOutputStream(file), null)

      println("CIdb configuration complete.")
      Some(p)
    } catch {
      case _ => exit("Error: Could not save configuration file.", 1); None
    }
  }
}
