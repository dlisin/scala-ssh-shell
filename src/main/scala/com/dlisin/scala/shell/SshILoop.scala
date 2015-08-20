package com.dlisin.scala.shell

import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter._

private[shell] object SshILoop {
  def apply(inputStream: InputStream, outputStream: OutputStream,
            name: Option[String] = None,
            bindValues: Seq[NamedParam] = Seq.empty,
            bindCommands: Seq[String] = Seq.empty): SshILoop = {
    new SshILoop(inputStream, outputStream, name, bindValues, bindCommands)
  }
}

private[shell] class SshILoop(in: InputStream, out: OutputStream,
                              name: Option[String],
                              bindValues: Seq[NamedParam],
                              bindCommands: Seq[String])
  extends ILoop(None, new JPrintWriter(out)) {

  override def prompt: String = {
    name match {
      case Some(value) => s"$value> "
      case None => super.prompt
    }
  }

  override def chooseReader(settings: Settings): InteractiveReader = {
    SshJLineReader(in, out, () => new JLineCompletion(intp))
  }

  override def createInterpreter(): Unit = {
    super.createInterpreter()

    intp = new ILoopInterpreter {
      override final def initializeSynchronous(): Unit = {
        super.initializeSynchronous()

        // Default bindings
        intp.quietBind("stdout", out)
        intp.quietRun( """def println(a: Any) = { stdout.write(a.toString); stdout.write('\n'); }""")
        intp.quietRun( """def exit = println("Use ctrl-D to exit shell.")""")

        // User bindings
        bindValues.foreach(intp.quietBind)
        bindCommands.foreach(intp.quietRun)
      }
    }
  }
}
