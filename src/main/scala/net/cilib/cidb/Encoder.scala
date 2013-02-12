package net.cilib.cidb

import com.mongodb.util.JSON
import com.thoughtworks.xstream.XStream
import java.io._
import javax.xml.parsers._
import javax.xml.transform._
import javax.xml.transform.sax._
import javax.xml.transform.stream._
import net.liftweb.json.JsonAST._
import net.liftweb.json.Printer
import net.liftweb.json.Xml
import org.xml.sax._
import org.xml.sax.helpers._
import scala.math._
import scala.xml._
import scala.xml.Utility._

class Encoder() extends DefaultHandler {

  var count = 0
  var string = ""
  var name = ""

  @throws(classOf[org.xml.sax.SAXException])
  override def startDocument() = {
    string += "<" + name + ">"
  }

  @throws(classOf[org.xml.sax.SAXException])
  override def endDocument() = {
    string += "</" + name + ">"
  }

  @throws(classOf[org.xml.sax.SAXException])
  override def startElement(uri: String, localName: String, qName: String, attributes: Attributes) = {
    count += 1
    val tagName = if (qName contains("."))
      qName substring(qName.lastIndexOf(".") + 1) else qName

    if (count == 1) {
      string += "<class>" + tagName + "</class>"
    } else if (qName contains(".")) {
      string += "<item><class>" + tagName + "</class>"
    } else {
      string += "<" + tagName + ">"

      for (i <- 0 to attributes.getLength - 1) {
        val aName = attributes getQName(i)
        val aValue = attributes getValue(i)
        string += "<" + aName + ">" + aValue.substring(aValue.lastIndexOf(".") + 1) + "</" + aName + ">"
      }
    }
  }

  @throws(classOf[org.xml.sax.SAXException])
  override def endElement(uri: String, localName: String, qName: String) = {
    count -= 1
    val tagName = if (qName contains("."))
      qName substring(qName.lastIndexOf(".") + 1) else qName

    if (count != 0) {
      if (qName contains(".")) {
        string += "</item>"
      } else {
        string += "</" + tagName + ">"
      }
    }
  }

  @throws(classOf[org.xml.sax.SAXException])
  override def characters(ch: Array[Char], start: Int, length: Int) = {
    string += new String(ch, start, length).replaceAll("\\s","")
  }

  @throws(classOf[org.xml.sax.SAXException])
  override def ignorableWhitespace(ch: Array[Char], start: Int, length: Int) = {
  }

  def toJson(obj: Any) = {
    try {
      this.name = "tmp"
      this.count = 0
      this.string = ""

      val x = new XStream()
      x.setMode(XStream.NO_REFERENCES)

      val data = XML.loadString(x.toXML(obj))

      val factory = SAXParserFactory.newInstance()
      val saxParser = factory.newSAXParser()

      saxParser.parse(new ByteArrayInputStream(data.toString.getBytes()), this)
      
      val j = Xml.toJson(XML.loadString(string)).children(0).children(0)
      def r[T](j: JValue): T = {
        (j match {
            case JObject(o) => JObject(o map r[JField])
            case JString(s) => {
                try {
                  JDouble(s.toDouble)
                } catch {
                  case _ => JString(s.toLowerCase)
                }
            }
          case JField(f,s) => JField(f, s map r[JValue])
          case z => z
        }).asInstanceOf[T]
      }

      JSON.parse(Printer.compact(
         render(Xml.toJson(XML.loadString(string)).children(0).children(0).map(r[JValue]))
        ))
    } catch {
      case err: Throwable => err.printStackTrace() 
    }
  }
}

object Encoder {
  val enc = new Encoder()
  
  def toJson(obj: Any) = enc toJson(obj)
}
