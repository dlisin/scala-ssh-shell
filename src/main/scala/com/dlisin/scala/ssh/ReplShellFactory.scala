package com.dlisin.scala.ssh

import grizzled.slf4j.Logging
import org.apache.sshd.common.Factory
import org.apache.sshd.server.{Command, Environment, ExitCallback}

import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter._

private[ssh] class ReplShellFactory(name: String,
                             settings: () => Settings,
                             boundValues: NamedParam*) extends Factory[Command] {
  override def create = new ReplCommand {
    override final def start(env: Environment) {
      new Thread {
        override final def run() {
          logger.info("Starting REPL session")
          val repl: ILoop = new SshILoop(name, inputStream, outputStream, boundValues: _*)

          try {
            repl.process(settings())
            exitCallback.onExit(0)
          } catch {
            case e: Throwable =>
              logger.error("An error occurred", e)
              exitCallback.onExit(1, e.getMessage)
          } finally {
            logger.info("Closing REPL session")
            repl.closeInterpreter()
          }
        }
      }.start()
    }
  }
}


private[ssh] trait ReplCommand extends Command with Logging {
  var _inputStream: InputStream = null
  var _outputStream: OutputStream = null
  var _errorStream: OutputStream = null
  var _exitCallback: ExitCallback = null

  protected final def inputStream = _inputStream

  protected final def outputStream = _outputStream

  protected final def errorStream = _errorStream

  protected final def exitCallback = _exitCallback

  override final def destroy() {}

  override final def setInputStream(inputStream: InputStream) {
    this._inputStream = inputStream
  }

  override final def setOutputStream(outputStream: OutputStream) {
    this._outputStream = new java.io.OutputStream {
      override def close() {
        outputStream.close()
      }

      override def flush() {
        outputStream.flush()
      }

      override def write(b: Int) {
        if (b.toChar == '\n')
          outputStream.write('\r')
        outputStream.write(b)
      }

      override def write(b: Array[Byte]) {
        var i = 0
        while (i < b.length) {
          write(b(i))
          i += 1
        }
      }

      override def write(b: Array[Byte], off: Int, len: Int) {
        write(b.slice(off, off + len))
      }
    }
  }

  override final def setErrorStream(errorStream: OutputStream) {
    this._errorStream = errorStream
  }

  override final def setExitCallback(exitCallback: ExitCallback) {
    this._exitCallback = exitCallback
  }
}
