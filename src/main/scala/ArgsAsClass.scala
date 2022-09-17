package org.kr.args

import java.lang.reflect.Field
import scala.language.implicitConversions

class ArgsAsClass(args:Array[String]) extends ArgsProcessor(args) {
  private def getListOfFields: List[Field] = {
    val valList = this.getClass.getDeclaredFields
      .filterNot(this.getClass.getMethods.toSet)
      .filter(f => f.getType.equals(classOf[Argument[_]]))
      .toList
    valList.foreach(_.setAccessible(true))
    valList
  }

  private def fillArgs(): Unit =
    getListOfFields.foreach(field=>{
      val argField=field.get(this).asInstanceOf[Argument[_]]
      argField.argumentType match {
        case NAMED=>
          argField.setName(field.getName)
          argField.setArg(this(field.getName))
        case POSITIONAL=>
          argField.setArg(this(argField.pos))
      }
    })

  def parse(): ArgsAsClass = {
    fillArgs()
    this
  }
}

sealed trait ArgumentType {
  val name:String
}

object NAMED extends ArgumentType {override val name:String="NAMED"}
object POSITIONAL extends ArgumentType {override val name:String="NAMED"}

class Argument[T](val defaultValue:Option[T], val isRequired:Boolean, val argumentType:ArgumentType)
                 (implicit converter:RawArgumentConverter[T]) {
  private[this] var privKey: Option[Either[String, Int]] = None
  private[this] var privArg: Option[RawArgument] = None

  // set name only once
  def setName(name: String): Argument[T] = {
    privKey = privKey.orElse(Some(Left(name)))
    this
  }
  // set pos only once
  def setPos(pos: Int): Argument[T] = {
    privKey = privKey.orElse(Some(Right(pos)))
    this
  }
  //set arg only once
  def setArg(newArg: Option[RawArgument]): Argument[T] = {
    privArg = privArg.orElse(newArg)
    this
  }

  lazy val name: String = privKey.flatMap(_.left.toOption).getOrElse("")
  lazy val pos: Int = privKey.flatMap(_.swap.left.toOption).getOrElse(0)
  protected lazy val arg: Option[RawArgument] = privArg

  lazy val optValue:Option[T]=converter.toValue(arg)
  lazy val value:T= {
    val v:Option[T]=converter.toValue(arg)
    (arg.isEmpty,isRequired,v.isEmpty,defaultValue.isEmpty) match {
      case (true,true,_,_) => throw new ValueConversionForArgumentException(f"Missing argument: '$name'")
      case (false,true,true,_) => throw new MissingArgumentException(f"Incorrect value conversion for: '$name'")
      case (_,_,true,true) => throw new MissingArgumentException(f"Missing argument: '$name' without default value")
      case (_,_,false,_) => v.get
      case (_,_,true,false) => v.getOrElse(defaultValue.get)
    }
  }
  def apply()(implicit conv:RawArgumentConverter[T]):T=value

  override def toString: String = f"${if(argumentType==NAMED) f"name:$name" else f"pos:$pos"}  = $value"
 }

object Argument {
  def optional[U](defaultValue: U)(implicit converter:RawArgumentConverter[U]):Argument[U]=
    new Argument(Some(defaultValue),false,NAMED)
  def required[U](implicit converter:RawArgumentConverter[U]):Argument[U]=
    new Argument(None,true,NAMED)
  def static[U](value: U)(implicit converter:RawArgumentConverter[U]):Argument[U]=
    new Argument(Some(value),false,NAMED)
  def ignored[U](implicit converter:RawArgumentConverter[U]):Argument[U]=
    new Argument(None,false,NAMED)

  def optionalPos[U](pos:Int,defaultValue: U)(implicit converter:RawArgumentConverter[U]):Argument[U]=
    new Argument(Some(defaultValue),false,POSITIONAL).setPos(pos)
  def requiredPos[U](pos:Int)(implicit converter:RawArgumentConverter[U]):Argument[U]=
    new Argument(None,true,POSITIONAL).setPos(pos)
  def staticPos[U](pos:Int,value: U)(implicit converter:RawArgumentConverter[U]):Argument[U]=
    new Argument(Some(value),false,POSITIONAL).setPos(pos)
  def ignoredPos[U](pos:Int)(implicit converter:RawArgumentConverter[U]):Argument[U]=
    new Argument(None,false,POSITIONAL).setPos(pos)

  //implicit conversion between typed argument and option of its value
  implicit def asOption[U](arg:Argument[U])(implicit converter:RawArgumentConverter[U]) :Option[U]= arg.optValue
  //implicit (unsafe) conversion between typed argument and its value
  implicit def asValueUnsafe[U](arg:Argument[U])(implicit converter:RawArgumentConverter[U]) :U= arg.value
}

class MissingArgumentException(message : String) extends Exception(message) {}
class ValueConversionForArgumentException(message : String) extends Exception(message) {}