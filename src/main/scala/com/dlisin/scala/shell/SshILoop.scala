package com.dlisin.scala.shell

import java.io.OutputStream

import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter._
import scala.util.Properties._

private[shell] class SshILoop(inputStream: InputStream, outputStream: OutputStream,
                              replName: Option[String],
                              initialBindings: Seq[NamedParam],
                              initialCommands: Seq[String]) extends ILoop(None, new JPrintWriter(outputStream)) {

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
    intp.quietBind("stdout", out)
    intp.quietRun( """def println(a: Any) = { stdout.write(a.toString); stdout.write('\n'); }""")

    // User bindings
    initialBindings.foreach(intp.quietBind)
    initialCommands.foreach(intp.quietRun)
  }

  override def printWelcome(): Unit = {
    echo(s"Welcome to Scala $versionString ($javaVmName, Java $javaVersion).")
    echo(s"Type in expressions to have them evaluated.")

    echo(s"The following predefined val's are available:")
    printBinding("stdout", out)
    initialBindings.foreach(printBinding)

    echo(s"Type :help for more information.")
    echo(s"Type :quit or press Ctrl-D to exit shell.")
  }

  private def printBinding(b: NamedParam): Unit = {
    echo(s"${b.name}: ${b.tpe} = ${b.value}")
  }
}
