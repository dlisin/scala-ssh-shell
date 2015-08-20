package com.dlisin.scala.shell

import java.io.OutputStream

import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.NamedParam._
import scala.tools.nsc.interpreter._

private[shell] class SshILoop(inputStream: InputStream, outputStream: OutputStream,
                              replName: Option[String],
                              initialBindings: Seq[NamedParam],
                              initialCommands: Seq[String]) extends ILoop(None, new JPrintWriter(outputStream)) {

  private val stdoutBinding = new Typed("stdout", out)

  override def prompt: String = {
    replName match {
      case Some(value) => s"$value> "
      case None => super.prompt
    }
  }

  override def chooseReader(settings: Settings): InteractiveReader = {
    new SshJLineReader(inputStream, outputStream, () => new JLineCompletion(intp))
  }

  override def createInterpreter(): Unit = {
    super.createInterpreter()
    intp.initializeSynchronous()

    // Default bindings
    intp.quietBind(stdoutBinding)
    intp.quietRun( """def println(a: Any) = { stdout.write(a.toString); stdout.write('\n'); }""")
    intp.quietRun( """def exit = println("Use ctrl-D to exit shell.")""")

    // User bindings
    initialBindings.foreach(intp.quietBind)
    initialCommands.foreach(intp.quietRun)
  }

  override def printWelcome(): Unit = {
    super.printWelcome()

    // Print initial bindings
    printBinding(stdoutBinding)
    initialBindings.foreach(printBinding)
  }

  private def printBinding(b:NamedParam):Unit = {
    echo(s"${b.name}: ${b.tpe} = ${b.value}")
  }
}
