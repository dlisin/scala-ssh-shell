package com.dlisin.scala.ssh

import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter._

private[ssh] class SshILoop(name:String, in: InputStream, out: OutputStream, bindValues: NamedParam*)
  extends ILoop(None, new JPrintWriter(out)) {

  override def prompt: String = {
    s"$name> "
  }

  override def chooseReader(settings: Settings): InteractiveReader = {
    new SshJLineReader(in, out, new JLineCompletion(intp))
  }

  override def createInterpreter(): Unit = {
    super.createInterpreter()

    intp = new ILoopInterpreter {
      override final def initializeSynchronous(): Unit = {
        super.initializeSynchronous()
        // Bind values
        intp.quietBind("stdout", out)
        bindValues.foreach(intp.quietBind)

        // Bind methods
        intp.quietRun( """def println(a: Any) = { stdout.write(a.toString); stdout.write('\n'); }""")
        intp.quietRun( """def exit = println("Use ctrl-D to exit shell.")""")
      }
    }
  }
}
