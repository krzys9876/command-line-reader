# Scala command-line reader
## Convert arguments to fields within a simple class

This short project is an excercise of scala impicit type conversions as well as a little bit of reflection.

If you're using your code in a way that the key configurations are passed as program arguments (this is what I do with my spark applications) 
you've probably already came across libraries that hide all the complexity of this (not that simple) task.

I usually try to understand the mechanics of the task before I decide if I should use an external library (who would write his/her own spark???) or write some code on my own.
This is a result of a study of scala impicits (which I really like) and java reflection, which I already knew from other projects.

The idea is to allow a developer to create a simplest possible class which contains fields corresponding to every argument. These fields should be easily 
definable (e.g. required/optional) and accessible (without too much boilerplate).

### Example ###

Consider an example argument list:

    --input-file="/tmp/some_folder/input.bin" --algorithm=gz --iterations=10 --output-file="/tmp/some_other_folder/output.bin"
  
You may define a class like this:

    class SampleArgs(args:Array[String]) extends ArgsAsClass(args) {
      val inputFile:ArgumentT[String]=ArgumentT.required
      val algorithm:ArgumentT[String]=ArgumentT.required
      val iterations:ArgumentT[Int]=ArgumentT.optional(5)
      val outputFile:ArgumentT[String]=ArgumentT.required
      val verbose:ArgumentT[Boolean]=ArgumentT.optional(false)

      parse()
    }

Invocation of the method _parse()_ is actually the only boilerplate (apart from type declaration).
It fills all the required values just after instantiation of the class. See side note below.

Instantiate the class at the very top of your main class:

    object Main extends App {
      val arguments=new SampleArgs(args)
      ...
    }

Now you can access values by:

    // assignment to a typed val - scala will do implicit type conversion for you:
    val inFile:String=arguments.inputFile

    // assignment to a val using explicit type conversion:
    val numOfIterations=arguments.iterations.value
    // or using apply():
    val numOfIterations=arguments.iterations()

NOTE: the above may throw error if a required argument is missing!

    // assignent to a optional val with implicit type conversion
    val outFile:Option[String]=arguments.outputFile
    // or explicitly
    val outFile:String=arguments.outputFile.optValue.getOrElse("/tmp/any_folder/out.bin")
    
This is safe (i.e. no error will be thrown) but you have to deal with options yourself.

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
      val inputFile:ArgumentT[String]
      val algorithm:ArgumentT[String]
      val iterations:ArgumentT[Int]
      val outputFile:ArgumentT[String]
      val verbose:ArgumentT[Boolean]
    }

Note: there's no _parse_ method.

In your code you pass around a trait not the concrete type. You may make it implicit 
as well as other dependencies that you have to inject. 

At runtime, you may use the above class:

    class SampleArgsRuntime(args:Array[String]) extends ArgsAsClass(args) with SampleArgsBase {
        //same body as above

        parse()
    }

Note: You have to use _parse()_ here.

For testing, you may define a separate class in a different way:

    class SampleArgsTest extends SampleArgsBase {
      val inputFile:ArgumentT[String]=ArgumentT.static("/test/files/in.bin")
      val algorithm:ArgumentT[String]=ArgumentT.static("fast")
      val iterations:ArgumentT[Int]=ArgumentT.static(2)
      val outputFile:ArgumentT[String]=ArgumentT.static("/test/files/in.bin")
      val verbose:ArgumentT[Boolean]=ArgumentT.ignored
    }

This is just a different convention. You explicitly set configuration values
and more importantly by using _ignored_ you show that this parameter is not 
used during testing. Since there is nothing to be parsed, you don't need the _parse_ method.

I find this more verbose.