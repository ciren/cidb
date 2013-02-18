import AssemblyKeys._

assemblySettings

name := "CIdb"

version := "0.1"

scalaVersion := "2.9.2"

mainClass in (Compile, run) := Some("net.cilib.cidb.Main")

mainClass in assembly := Some("net.cilib.cidb.Main")

jarName in assembly := "CIdb-0.1.jar"

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) => {
    case PathList("org", "fusesource", xs @ _*) => MergeStrategy.first
    case PathList("org", "xmlpull", xs @ _*) => MergeStrategy.first
    case x => old(x)
  }
}

libraryDependencies ++= Seq(
    "org.mongodb" % "casbah_2.9.2" % "2.4.1",
    "commons-cli" % "commons-cli" % "1.2",
    "org.scala-lang" % "jline" % "2.9.2"
)

