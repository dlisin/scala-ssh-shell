package com.dlisin.scala.ssh

import java.io.{InputStream, OutputStream}
import java.util.{Collection => JCollection, List => JList}

import _root_.jline.console.history.{History => JHistory}
import _root_.jline.{console => jconsole}
import com.dlisin.scala.ssh.jline.JLineConsoleReader

import scala.tools.nsc.interpreter
import scala.tools.nsc.interpreter.Completion
import scala.tools.nsc.interpreter.jline.JLineHistory
import scala.tools.nsc.interpreter.session.History

private[ssh] class SshJLineReader(in: InputStream, out: OutputStream,
                                  completer: => Completion) extends interpreter.InteractiveReader {
  val interactive = true

  val history: History = new JLineHistory.JLineFileHistory()

  private val consoleReader = {
    val reader = new JLineConsoleReader(in, out)
    reader.setPaginationEnabled(interpreter.isPaged)
    reader.setExpandEvents(false)
    reader.setHistory(history.asInstanceOf[JHistory])

    reader
  }

  private[this] var _completion: Completion = interpreter.NoCompletion

  def completion: Completion = _completion

  override def postInit() = {
    _completion = completer

    consoleReader.initCompletion(completion)
  }

  def reset() = consoleReader.getTerminal.reset()

  def redrawLine() = consoleReader.redrawLineAndFlush()

  def readOneLine(prompt: String) = consoleReader.readLine(prompt)

  def readOneKey(prompt: String) = consoleReader.readOneKey(prompt)
}


