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
  val keyPrefixes: List[String] =List("--","-")
  val separators: List[String] =List(".","-","_")
  val preferredSeparator:String="-"
  val assignment="="

  @tailrec
  private def preProcess(toProcess:List[String], processed:List[String]=List()) : List[String]={
    toProcess match {
      // any argument with assignment (=)
      case head::tail if isNamed(head) =>
        preProcess(tail,processed ++ List(head))
      // any argument with value but without assignment (=)
      case head1::head2::tail if isArgNameOnly(head1) && !isArgName(head2) && !isNamed(head2) && isNamed(head1+assignment+head2) =>
        preProcess(tail,processed ++ List(head1+assignment+head2))
      // any argument without assignment (=) - e.g. boolean w/o value (--help)
      case head :: tail if isArgNameOnly(head) && isNamed(head + assignment + "") =>
        preProcess(tail, processed ++ List(head + assignment + ""))
      case _ =>
        processed ++ toProcess
    }
  }

  private def isNamed(value:String):Boolean =
    !value.isBlank &&
      isArgName(value) &&
      !value.startsWith(assignment) &&
      value.contains(assignment)

  private def isArgName(value: String): Boolean = keyPrefixes.exists(value.startsWith)
  private def isArgNameOnly(value: String): Boolean = isArgName(value) && !value.contains(assignment)

  private def splitNamed(namedText: String):RawArgument = {
    val splitPos=namedText.indexOf(assignment)
    val key=namedText.substring(0,splitPos)
    val keyReplaced=formatKey(key)
    val value=namedText.substring(splitPos+1)
    RawArgument(keyReplaced,value)
  }

  private def formatKey(key:String):String = {
    val removedPrefix=keyPrefixes.foldLeft(key)((keyRepl,prefix)=>
      if(keyRepl.startsWith(prefix)) keyRepl.substring(prefix.length) else keyRepl)
    val removedSeparators=removeSeparators(removedPrefix)
    removedSeparators.toUpperCase
  }

  def unformatName(fieldName:String):String = {
    fieldName match {
      case startsWithCapital if startsWithCapital.nonEmpty && startsWithCapital.charAt(0).isUpper =>
        replaceSeparators(fieldName,preferredSeparator).toLowerCase()
      case _ => addSeparators(fieldName)
    }

  }

  private def removeSeparators(text: String): String = replaceSeparators(text,"")

  private def replaceSeparators(text: String, to:String): String =
    separators.foldLeft(text)((keyRepl, separator) => keyRepl.replace(separator, to))

  private def addSeparators(text: String): String =
    text.toCharArray.toList.foldLeft("")((textWithSeparators, char) =>
      textWithSeparators + (char match {
        case separator if separators.contains(separator.toString) => preferredSeparator
        case upper if upper.isUpper && !textWithSeparators.endsWith(preferredSeparator)  =>
          preferredSeparator + char.toLower
        case other => other.toLower
      }))
}

case class RawArgument(key:Either[String,Int], value:String) {
  def asString:Option[String]=Some(value)
  def asBoolean:Option[Boolean]=
    value.toUpperCase() match {
      //NOTE: empty value is treated as true. The fact that we try to evaluate the value means that the argument
      // has been provided and for boolean the default value for a provided argument must be true
      case "T" | "TRUE" | "" => Some(true)
      case "F" | "FALSE" => Some(false)
      case _ => None
    }
  def asInt:Option[Int]=value.toIntOption
  def asLong:Option[Long]=value.toLongOption
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
  val name:String
}

object RawArgumentConverter {
  implicit object RawArgumentToInt extends RawArgumentConverter[Int] {
    override def toValue(rawArgument: Option[RawArgument]): Option[Int] = rawArgument.flatMap(_.asInt)
    override val name:String = "number (int)"
  }
  implicit object RawArgumentToLong extends RawArgumentConverter[Long] {
    override def toValue(rawArgument: Option[RawArgument]): Option[Long] = rawArgument.flatMap(_.asLong)
    override val name:String = "number (long)"
  }
  implicit object RawArgumentToString extends RawArgumentConverter[String] {
    override def toValue(rawArgument: Option[RawArgument]): Option[String] = rawArgument.flatMap(_.asString)
    override val name:String = "text"
  }
  implicit object RawArgumentToBoolean extends RawArgumentConverter[Boolean] {
    override def toValue(rawArgument: Option[RawArgument]): Option[Boolean] = rawArgument.flatMap(_.asBoolean)
    override val name:String = "[true]/false"
  }
  implicit object RawArgumentToLocalDate extends RawArgumentConverter[LocalDate] {
    override def toValue(rawArgument: Option[RawArgument]): Option[LocalDate] = rawArgument.flatMap(_.asLocalDate)
    override val name:String = "date (yyyy-mm-dd)"
  }
  implicit object RawArgumentToLocalDateTime extends RawArgumentConverter[LocalDateTime] {
    override def toValue(rawArgument: Option[RawArgument]): Option[LocalDateTime] = rawArgument.flatMap(_.asLocalDateTime)
    override val name:String = "date (yyyy-mm-dd hh:mi:ss)"
  }
  implicit object RawArgumentToDouble extends RawArgumentConverter[Double] {
    override def toValue(rawArgument: Option[RawArgument]): Option[Double] = rawArgument.flatMap(_.asDouble)
    override val name:String = "real number"
  }
}