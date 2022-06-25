package org.kr

object Main extends App {
  val argsProcessor=ArgsProcessor(args)
  println(argsProcessor.named.mkString("|"))
  println(argsProcessor.positional.mkString("|"))
}

