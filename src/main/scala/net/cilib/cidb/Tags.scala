package net.cilib.cidb

import com.mongodb.casbah.Imports._
import java.io._
import java.util.Properties
import scala.collection.mutable.HashMap
import scala.io.Source._

object Tags {

  val REJECT = List("@type")

  def updateTagsCollection(data: DBObject, connection: MongoConnection, p: Properties) = {
    var tmpTags = new HashMap[String,Set[String]]

    def recurse(b: List[String], a: DBObject): Any = {
      for (i <- a) {
        i._2 match {
          case it: DBObject => recurse(i._1 :: b, it)
          case o => {
              val keys = (i._1 :: b).reverse
              val key = "data." + keys.mkString(".")

              for (
                k <- keys
                if !REJECT.exists(_ == k)
              ) tmpTags += k -> (tmpTags.getOrElse(k, Set.empty) + key)
          }
        }
      }
    }

    recurse(List.empty, data)
    val tags = tmpTags map { a => {
        MongoDBObject("term" -> a._1, "keys" -> a._2)
      }
    }

    val tagsCol = connection(p.getProperty("db_database"))(p.getProperty("db_tagCol"))
    for (a <- tags) {
      val b = tagsCol.find(MongoDBObject("term" -> a("term")))

      if (b.count == 0) {
        tagsCol += a
      } else {
        // update with items not already there
        tagsCol.update(MongoDBObject("term" -> a("term")), $addToSet("keys") $each(a("keys")))
      }
    }
  }

}
