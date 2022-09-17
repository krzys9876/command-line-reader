package org.kr.args

import ArgsProcessor.{formatKey, isNamed, preProcess, splitNamed}

import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter
import scala.annotation.tailrec
import scala.language.implicitConversions
import scala.util.Try

class ArgsProcessor(val args:Array[String]) {
  lazy val named:List[RawArgument]=parse(args)._1
  lazy val positional: List[String]=parse(args)._2
  lazy val arguments:List[RawArgument]=
    named ++
      positional.foldLeft((List[RawArgument](),0))(
        {case((list,counter),value)=>(list :+ RawArgument(counter,value),counter+1)})
        ._1

  lazy val asMap:Map[Either[String,Int],RawArgument]=arguments.map(arg=> arg.key->arg).toMap

  def apply(name:String):Option[RawArgument]=asMap.get(Left(formatKey(name)))
  def apply(position:Int):Option[RawArgument]=asMap.get(Right(position))

  private def parse(args:Array[String]):(List[RawArgument],List[String])={
    val argsPreprocessed=preProcess(args.toList)
    val namedElements=argsPreprocessed.takeWhile(isNamed)
    val remainingElements=argsPreprocessed.takeRight(argsPreprocessed.length-namedElements.length)
    val named=namedElements.map(value=>splitNamed(value))
    (named,remainingElements)
  }
}

object ArgsProcessor {
  val keyPrefixes=List("--","-")
  val separators=List(".","-","_")
  val assignment="="

  @tailrec
  private def preProcess(toProcess:List[String], processed:List[String]=List()) : List[String]={
    toProcess match {
      case head::tail if isNamed(head) =>
        preProcess(tail,processed ++ List(head))
      case head1::head2::tail if isNamed(head1+assignment+head2) =>
        preProcess(tail,processed ++ List(head1+assignment+head2))
      case _ =>
        processed ++ toProcess
    }
  }

  private def isNamed(value:String):Boolean =
    !value.isBlank &&
      keyPrefixes.exists(value.startsWith) &&
      !value.startsWith(assignment) &&
      value.contains(assignment)

  private def splitNamed(namedText: String):RawArgument = {
    val splitPos=namedText.indexOf(assignment)
    val key=namedText.substring(0,splitPos)
    val keyReplaced=formatKey(key)
    val value=namedText.substring(splitPos+1)
    RawArgument(keyReplaced,value)
  }

  private def formatKey(key:String):String= {
    val removedPrefix=keyPrefixes.foldLeft(key)((keyRepl,prefix)=>
      if(keyRepl.startsWith(prefix)) keyRepl.substring(prefix.length) else keyRepl)
    val removedSeparators=separators.foldLeft(removedPrefix)((keyRepl,separator)=>
      keyRepl.replace(separator,""))
    removedSeparators.toUpperCase
  }
}

case class RawArgument(key:Either[String,Int], value:String) {
  def asString:Option[String]=Some(value)
  def asBoolean:Option[Boolean]=
    value.toUpperCase() match {
      case "T" | "TRUE" => Some(true)
      case "F" | "FALSE" => Some(false)
      case _ => None
    }
  def asInt:Option[Int]=value.toIntOption
  def asLocalDate:Option[LocalDate]=
    Try(Some(LocalDate.parse(value,DateTimeFormatter.ofPattern("yyyy-MM-dd")))).getOrElse(None)
  def asLocalDateTime:Option[LocalDateTime]=
    Try(Some(LocalDateTime.parse(value,DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))).getOrElse(None)
  def asDouble:Option[Double]=value.toDoubleOption
}

object RawArgument {
  def apply(key:String,value:String):RawArgument = new RawArgument(Left(key),value)
  def apply(key:Int,value:String):RawArgument = new RawArgument(Right(key),value)
}

trait RawArgumentConverter[T] {
def toValue(rawArgument:Option[RawArgument]):Option[T]
}

object RawArgumentConverter {
  implicit object RawArgumentToInt extends RawArgumentConverter[Int] {
    override def toValue(rawArgument: Option[RawArgument]): Option[Int] = rawArgument.flatMap(_.asInt)
  }
  implicit object RawArgumentToString extends RawArgumentConverter[String] {
    override def toValue(rawArgument: Option[RawArgument]): Option[String] = rawArgument.flatMap(_.asString)
  }
  implicit object RawArgumentToBoolean extends RawArgumentConverter[Boolean] {
    override def toValue(rawArgument: Option[RawArgument]): Option[Boolean] = rawArgument.flatMap(_.asBoolean)
  }
  implicit object RawArgumentToLocalDate extends RawArgumentConverter[LocalDate] {
    override def toValue(rawArgument: Option[RawArgument]): Option[LocalDate] = rawArgument.flatMap(_.asLocalDate)
  }
  implicit object RawArgumentToLocalDateTime extends RawArgumentConverter[LocalDateTime] {
    override def toValue(rawArgument: Option[RawArgument]): Option[LocalDateTime] = rawArgument.flatMap(_.asLocalDateTime)
  }
  implicit object RawArgumentToDouble extends RawArgumentConverter[Double] {
    override def toValue(rawArgument: Option[RawArgument]): Option[Double] = rawArgument.flatMap(_.asDouble)
  }
}