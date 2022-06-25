package org.kr

import scala.annotation.tailrec

case class ArgsProcessor(named:List[Argument],positional: List[String])

object ArgsProcessor {
  val keyPrefixes=List("--","-")
  val assignment="="

  def apply(args:Array[String]):ArgsProcessor = {
    val argsPreprocessed=preProcess(args.toList)
    val namedElements=argsPreprocessed.takeWhile(isNamed)
    val remainingElements=argsPreprocessed.takeRight(argsPreprocessed.length-namedElements.length)
    val named=namedElements.map(value=>splitNamed(value))
    new ArgsProcessor(named,remainingElements)
  }

  @tailrec
  private def preProcess(toProcess:List[String], processed:List[String]=List()) : List[String]={
    toProcess match {
      case head::tail if isNamed(head) => preProcess(tail,processed ++ List(head))
      case head1::head2::tail if isNamed(head1+assignment+head2) => preProcess(tail,processed ++ List(head1+assignment+head2))
      case _ => processed ++ toProcess
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

case class Argument(key:Either[String,Int],value:String)

object Argument {
  def apply(key:String,value:String):Argument = new Argument(Left(key),value)
  def apply(key:Int,value:String):Argument = new Argument(Right(key),value)
}