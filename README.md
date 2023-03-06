# Scala command-line reader
## Convert arguments to fields within a simple class

This short project is an exercise of scala implicit type conversions with a bit of java reflection.

If you're using your code in a way that the key configurations are passed as program arguments 
(this is what I do with my spark applications) you've probably already came across libraries that 
hide all the complexity of this (not that simple) task.

I usually try to understand the mechanics of the task before I decide if I should use an external library 
or write some code on my own. This is a result of a study of scala implicits (which I find extremely useful) 
and java reflection, which I already knew from other projects.

The idea is to allow a developer to create the simplest possible class which contains fields corresponding to every argument. 
These fields should be easily definable (e.g. required/optional) and accessible (without too much boilerplate). 
Both scala files are close to 150 lines, which I guess qualifies them as _simple_. There would be even
less without parsing additional types or printing instructions 

### Example ###

Consider an example argument list:

    --input-file="/tmp/some_folder/input.bin" --algorithm=gz --iterations=10 --output-file="/tmp/some_other_folder/output.bin"
  
You may define a class like this:

    class SampleArgs(args:Array[String]) extends ArgsAsClass(args) {
      val inputFile:Argument[String]=Argument.required
      val algorithm:Argument[String]=Argument.required
      val iterations:Argument[Int]=Argument.optional(5)
      val outputFile:Argument[String]=Argument.required
      val verbose:Argument[Boolean]=Argument.optional(false)

      parse()
    }

Invocation of the method <code>parse()</code> at the end of the class body is actually the only boilerplate 
(apart from type declaration). It fills all the required values just after instantiation of the class. See side note below.

Instantiate the class at the very top of your main class:

    object Main extends App {
      val arguments=new SampleArgs(args)
      ...
    }

_NOTE:_ this will throw <code>MissingArgumentException</code> if a required argument is missing!

_NOTE2:_ if a user provides <code>--help</code> as arguments (this is a reserved name!), this will throw <code>PrintHelpAndExit</code> exception 
with message containing usage instructions. You might leave it as it is, but if you prefer not to print 
a standard stack trace you should catch it and print instructions in a more friendly manner. Yes, this could
be done by packing <code>parse</code> method result in e.g. <code>Either</code>, so feel free to modify it 
if you find exceptions less elegant.

Now you can access values by:

    // assignment to a typed val - scala will do implicit type conversion for you:
    val inFile:String=arguments.inputFile

    // assignment to a val using explicit type conversion:
    val numOfIterations=arguments.iterations.value
    // or using apply():
    val numOfIterations=arguments.iterations()


    // assignent to a optional val with implicit type conversion
    val outFile:Option[String]=arguments.outputFile
    // or explicitly
    val outFile:String=arguments.outputFile.optValue.getOrElse("/tmp/any_folder/out.bin")
    
This is safe (i.e. no error will be thrown) but you have to deal with options yourself.

It was a bit complicated to accept Boolean parameters without values. This will not work if you mix
boolean parameters with positional ones, particularly when a boolean parameter is provided without value 
and right before named ones. This will confuse parser and cause incorrect behaviour.

I tend not to mix parameter types (named/positional) if possible. 

### Side note 1 on immutability ###

You may have noticed that I use some private _vars_ to keep a name, position and actual argument.
The problem is this: I need to set contents of a field on the basis of its actual name. In order
to use reflection the field must be instantiated, i.e. I cannot set _vals_ afterwards. 

Note that all this happens during, or rather just after, instantiation of the class. It means that
there is no access to _vars_ at runtime since they are set only once. This makes them a bit like _lazy vals_.

### Side note 2 on testability ###

As you use runtime arguments, you may also want to be using similar construct for testing.
You may create separate test class with arguments defined not as parser for array of strings
but even more verbose.

Say you define a trait with application-wide arguments:

    trait SampleArgsBase {
      val inputFile:Argument[String]
      val algorithm:Argument[String]
      val iterations:Argument[Int]
      val outputFile:Argument[String]
      val verbose:Argument[Boolean]
    }

Note: there's no <code>parse</code> method.

In your code you pass around a trait not the concrete type. You may make it implicit 
as well as other dependencies that you have to inject. 

At runtime, you may use the above class:

    class SampleArgsRuntime(args:Array[String]) extends ArgsAsClass(args) with SampleArgsBase {
        //same body as above

        parse()
    }

Note: You have to use <code>parse()</code> here.

For testing, you may define a separate class in a different way:

    class SampleArgsTest extends SampleArgsBase {
      val inputFile:Argument[String]=Argument.static("/test/files/in.bin")
      val algorithm:Argument[String]=Argument.static("fast")
      val iterations:Argument[Int]=Argument.static(2)
      val outputFile:Argument[String]=Argument.static("/test/files/in.bin")
      val verbose:Argument[Boolean]=Argument.ignored
    }

This is just a different convention. By using <code>static</code> you explicitly set configuration values
and more importantly by using <code>ignored</code> you show that this parameter is not 
used during testing. Since there is nothing to be parsed, you don't need the <code>parse</code> method.

I find this more verbose.

### Side note 3 on adding new types ###

Depending on personal preferences you may find useful arguments of very specific types, e.g. Double from scientific notation. 
To add new type all you have to do is:
1. Define a new parsing method in <code>RawArgument</code> class, e.g. <code>asYourType</code>. It should convert the textual <code>value</code> into option of the type you're adding. 
2. Add another implicit object to <code>RawArgumentConverter</code>, which overrides <code>toValue</code> method. It should invoke _asYourType_ method (you need _flatMap_ here since you must map option to option). 

This would look like:

    case class RawArgument(key:Either[String,Int], value:String) {
        ...
      def asDouble:Option[Double]=
        Double.toDoubleOption
    }

    object RawArgumentConverter {
        ...
      implicit object RawArgumentToDouble extends RawArgumentConverter[Double] {
        override def toValue(rawArgument: Option[RawArgument]): Option[Double] = rawArgument.flatMap(_.asDouble)
      }
    }

You could argue that value conversion and exposing implicit object could 
be combined <code>RawArgumentConverter</code>. Still I prefer to separate these two responsibilities, even 
if it generates some more boilerplate.

Now you can define an argument as double:

    class SampleArgs2(args:Array[String]) extends ArgsAsClass(args) {
      val doubleValue:Argument[Double]=Argument.required
      parse()
    }

