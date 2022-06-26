package org.kr.args

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

  println(argsProcessor.arguments.mkString("|"))

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

  val aT:ArgumentT[Int]=ArgumentT(argsProcessor("a").get,0)
  val bT:ArgumentT[String]=ArgumentT(argsProcessor("b").get,"")
  val cT:ArgumentT[String]=ArgumentT(argsProcessor("c").get,"")
  val dT:ArgumentT[Boolean]=ArgumentT(argsProcessor("d").get,false)
  val eT:ArgumentT[LocalDate]=ArgumentT(argsProcessor("e").get,LocalDate.of(2022,1,1))
  val fT:ArgumentT[LocalDateTime]=ArgumentT(argsProcessor("f").get,LocalDateTime.of(2022,1,1,1,2,3))

  println(aT.toString)
  println(bT.toString)
  println(cT.toString)
  println(dT.toString)
  println(eT.toString)
  println(fT.toString)

  val aTv:Int=aT
  val bTv:String=bT
  val cTv:String=cT
  val dTv:Boolean=dT
  val eTv:LocalDate=eT
  val fTv:LocalDateTime=fT

  println(aTv)
  println(bTv)
  println(cTv)
  println(dTv)
  println(eTv)
  println(fTv)

}

