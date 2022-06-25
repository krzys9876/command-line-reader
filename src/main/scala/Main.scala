package org.kr

import java.time.{LocalDate, LocalDateTime}

object Main extends App {

  // input: --a=123 --b "DEF GHI" -c=JKL -d T -e 2022-01-02 -f "2022-01-02 23:45:56" MNO PQR

  val argsProcessor=ArgsProcessor.parse(args)
  println(argsProcessor.named.mkString("|"))
  println(argsProcessor.positional.mkString("|"))

  println(argsProcessor.asMap(Left("a")).asInt)
  println(argsProcessor.asMap(Left("a")).asString)
  println(argsProcessor.asMap(Left("b")).asInt)
  println(argsProcessor.asMap(Left("b")).asString)
  println(argsProcessor.asMap(Left("c")).asBoolean)
  println(argsProcessor.asMap(Left("c")).asInt)
  println(argsProcessor.asMap(Left("d")).asBoolean)
  println(argsProcessor.asMap(Left("d")).asInt)
  println(argsProcessor.asMap(Left("e")).asLocalDate)
  println(argsProcessor.asMap(Left("f")).asLocalDateTime)

  println(argsProcessor.args.mkString("|"))

  println(argsProcessor("a"))
  println(argsProcessor("b"))
  println(argsProcessor("c"))
  println(argsProcessor("d"))
  println(argsProcessor("e"))
  println(argsProcessor("f"))
  println(argsProcessor(1))
  println(argsProcessor(2))



  val a:Option[Int]=argsProcessor("a")
  val b:Option[String]=argsProcessor("b")
  val c:Option[String]=argsProcessor("c")
  val d:Option[Boolean]=argsProcessor("d")
  val e:Option[LocalDate]=argsProcessor("e")
  val f:Option[LocalDateTime]=argsProcessor("f")
  println(a.get)
  println(b.get)
  println(c.get)
  println(d.get)
  println(e.get)
  println(f.get)

}

