package net.cilib.cidb

import com.mongodb.casbah.Imports._
import java.io._
import net.liftweb.json.JsonParser._
import scala.io.Source._
import org.apache.commons.cli.{ParseException => ParseError, _}
import Util._

/**
 * TODO: dont add duplicates, get right sim
 */
object Main {

  def main(args: Array[String]) = {
    try {
      handleCommandline(new GnuParser().parse(createOptions, args))
    } catch {
      case e: ParseError => {
        exit("Error: Failed to parse command line arguments. Use --help to see options.\n" + e.getMessage(), 1)
      }
    }
  }

  def handleCommandline(cli: CommandLine) = {

    def op(s: String) = cli.hasOption(s)
    def opVal(s: String) = cli.getOptionValue(s)
    def opHelp = { new HelpFormatter() printHelp("CIdb", createOptions); System.exit(0) }
    val opLen = cli.getOptions().length
    val addOps = if (op("config")) 1 else 0
    val props = if (op("config")) config(opVal("config")) else config("cidb.conf")

    if (op("help")) {
      opHelp
    } else if (op("submit")) {
      if (!op("results") || !op("spec") || !op("jar")) exit("Option --submit must be used with --results, --jar and --spec", 1)
      if (op("search")) exit("Option --submit cannot be used with option --search", 1)

      Submit.submit(opVal("spec"), opVal("results"), opVal("jar"), props)
    } else if (op("search")) {
      if (opLen != 1 + addOps) exit("Option --search must be used without any other options.", 1)

      Search.search(props, opVal("search") + " " + cli.getArgs().mkString(" "))
    } else {
      println("Error: You must submit a simulation with results or supply a search string!")
      opHelp
    }
  }
}
