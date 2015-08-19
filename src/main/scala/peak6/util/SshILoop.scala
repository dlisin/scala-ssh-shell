/* NSC -- new Scala compiler
 * Copyright 2005-2011 LAMP/EPFL
 * @author Alexander Spoon
 */

package peak6.util

import java.io.{InputStream, OutputStream, OutputStreamWriter, PrintWriter}

import org.apache.commons.codec.Charsets

import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.{InteractiveReader, _}

final class SshILoop(inputStream: InputStream, outputStream: OutputStream, bindValues: NamedParam*)
  extends scala.tools.nsc.interpreter.ILoop(
    None,
    new PrintWriter(new OutputStreamWriter(outputStream, Charsets.UTF_8))) {

  override def chooseReader(settings: Settings): InteractiveReader = {
    new SshJLineReader(inputStream, outputStream, () => new JLineCompletion(intp))
  }

  override def createInterpreter(): Unit = {
    if (addedClasspath != "")
      settings.classpath append addedClasspath
    intp = new ILoopInterpreter {
      override final def initializeSynchronous(): Unit = {
        super.initializeSynchronous()
        bindValues.foreach(intp.quietBind)
        in.postInit()
      }
    }
  }

}

