package org.kr.args

import ArgsProcessor.{formatKey, isNamed, preProcess, splitNamed}

import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter
import scala.annotation.tailrec
import scala.language.implicitConversions
import scala.util.Try
import scala.reflect.runtime.universe._

class ArgsProcessor(val args:Array[String]) {

  lazy val named:List[Argument]=parse(args)._1
  lazy val positional: List[String]=parse(args)._2
  lazy val arguments:List[Argument]=
    named ++
      positional.foldLeft((List[Argument](),0))(
        {case((list,counter),value)=>(list :+ Argument(counter,value),counter+1)})
        ._1

  lazy val asMap:Map[Either[String,Int],Argument]=arguments.map(arg=> arg.key->arg).toMap

  def apply(name:String):Option[Argument]=asMap.get(Left(formatKey(name)))
  def apply(position:Int):Option[Argument]=asMap.get(Right(position))

  private def parse(args:Array[String]):(List[Argument],List[String])={
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

  private def splitNamed(namedText: String):Argument = {
    val splitPos=namedText.indexOf(assignment)
    val key=namedText.substring(0,splitPos)
    val keyReplaced=formatKey(key)
    val value=namedText.substring(splitPos+1)
    Argument(keyReplaced,value)
  }

  private def formatKey(key:String):String= {
    val removedPrefix=keyPrefixes.foldLeft(key)((keyRepl,prefix)=>
      if(keyRepl.startsWith(prefix)) keyRepl.substring(prefix.length) else keyRepl)
    val removedSeparators=separators.foldLeft(removedPrefix)((keyRepl,separator)=>
      keyRepl.replace(separator,""))
    removedSeparators.toUpperCase
  }
}

case class Argument(key:Either[String,Int],value:String) {
  def asString:Option[String]=Some(value)

  def asBoolean:Option[Boolean]=
    value.toUpperCase() match {
      case "T" | "TRUE" => Some(true)
      case "F" | "FALSE" => Some(false)
      case _ => None
    }

  def asInt:Option[Int]=
    Try(Some(value.toInt)).getOrElse(None)

  def asLocalDate:Option[LocalDate]=
    Try(Some(LocalDate.parse(value,DateTimeFormatter.ofPattern("yyyy-MM-dd")))).getOrElse(None)

  def asLocalDateTime:Option[LocalDateTime]=
    Try(Some(LocalDateTime.parse(value,DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))).getOrElse(None)
}

object Argument {
  def apply(key:String,value:String):Argument = new Argument(Left(key),value)
  def apply(key:Int,value:String):Argument = new Argument(Right(key),value)

  // implicit conversion between optional argument and its optional value of a given type
  implicit def argToType[U: TypeTag](arg:Option[Argument]):Option[U]= {
    // explicit choice of method that converts string value of an argument to a given type
    val result = typeOf[U] match {
      case t if t =:= typeOf[Int] => arg.flatMap(_.asInt)
      case t if t =:= typeOf[String] => arg.flatMap(_.asString)
      case t if t =:= typeOf[Boolean] => arg.flatMap(_.asBoolean)
      case t if t =:= typeOf[LocalDate] => arg.flatMap(_.asLocalDate)
      case t if t =:= typeOf[LocalDateTime] => arg.flatMap(_.asLocalDateTime)
    }
    result.asInstanceOf[Option[U]]
  }
}