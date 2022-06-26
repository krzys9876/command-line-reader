package org.kr.args

import ArgsProcessor.{isNamed, preProcess, splitNamed}

import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter
import scala.annotation.tailrec
import scala.language.implicitConversions
import scala.util.Try

class ArgsProcessor(val args:Array[String]) {

  lazy val named:List[Argument]=doParse(args)._1
  lazy val positional: List[String]=doParse(args)._2
  lazy val arguments:List[Argument]=
    named ++
      positional.foldLeft((List[Argument](),0))(
        {case((list,counter),value)=>(list :+ Argument(counter+1,value),counter+1)})
        ._1

  lazy val asMap:Map[Either[String,Int],Argument]=arguments.map(arg=> arg.key->arg).toMap

  def apply(name:String):Option[Argument]=asMap.get(Left(name))
  def apply(position:Int):Option[Argument]=asMap.get(Right(position))

  private def doParse(args:Array[String]):(List[Argument],List[String])={
    val argsPreprocessed=preProcess(args.toList)
    val namedElements=argsPreprocessed.takeWhile(isNamed)
    val remainingElements=argsPreprocessed.takeRight(argsPreprocessed.length-namedElements.length)
    val named=namedElements.map(value=>splitNamed(value))
    (named,remainingElements)
  }
}

object ArgsProcessor {
  val keyPrefixes=List("--","-")
  val assignment="="

  def parse(args:Array[String]):ArgsProcessor = {
    new ArgsProcessor(args)
  }

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
    val keyReplaced=removePrefix(key)
    val value=namedText.substring(splitPos+1)
    Argument(keyReplaced,value)
  }

  private def removePrefix(key:String):String=
    keyPrefixes.foldLeft(key)((keyRepl,prefix)=>
      if(keyRepl.startsWith(prefix)) keyRepl.substring(prefix.length) else keyRepl)
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

  implicit def argToString(arg:Option[Argument]):Option[String]=arg.flatMap(_.asString)
  implicit def argToInt(arg:Option[Argument]):Option[Int]=arg.flatMap(_.asInt)
  implicit def argToBoolean(arg:Option[Argument]):Option[Boolean]=arg.flatMap(_.asBoolean)
  implicit def argToLocalDate(arg:Option[Argument]):Option[LocalDate]=arg.flatMap(_.asLocalDate)
  implicit def argToLocalDateTime(arg:Option[Argument]):Option[LocalDateTime]=arg.flatMap(_.asLocalDateTime)
}

class ArgumentT[T](override val key:Either[String,Int], override val value:String, val defaultValue:T)
  extends Argument(key,value) {
  override def toString: String = {
    val part1=key match {
      case Left(name) => f"key: $name"
      case Right(pos) => f"pos: $pos"
    }
    val part2=Argument(key,value).asInstanceOf[T]
    part1+" value: "+part2
  }
}

object ArgumentT {
  def apply[T](arg:Argument,defaultValue:T):ArgumentT[T]=new ArgumentT(arg.key,arg.value,defaultValue)

  implicit def argToString(arg:ArgumentT[String]):String=arg.asString.getOrElse(arg.defaultValue)
  implicit def argToInt(arg:ArgumentT[Int]):Int=arg.asInt.getOrElse(arg.defaultValue)
  implicit def argToBoolean(arg:ArgumentT[Boolean]):Boolean=arg.asBoolean.getOrElse(arg.defaultValue)
  implicit def argToLocalDate(arg:ArgumentT[LocalDate]):LocalDate=arg.asLocalDate.getOrElse(arg.defaultValue)
  implicit def argToLocalDateTime(arg:ArgumentT[LocalDateTime]):LocalDateTime=arg.asLocalDateTime.getOrElse(arg.defaultValue)
}