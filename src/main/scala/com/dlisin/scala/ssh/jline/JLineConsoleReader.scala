package com.dlisin.scala.ssh.jline

import java.io.{InputStream, OutputStream}
import java.util.{Collection => JCollection, List => JList}

import _root_.jline.console.completer.{ArgumentCompleter, Completer}
import _root_.jline.{console => jconsole}

import scala.tools.nsc.interpreter
import scala.tools.nsc.interpreter.Completion
import scala.tools.nsc.interpreter.Completion.Candidates
import scala.tools.nsc.interpreter.jline.JLineDelimiter

private[ssh] class JLineConsoleReader(in: InputStream, out: OutputStream)
  extends jconsole.ConsoleReader(in, out) with interpreter.VariColumnTabulator {
  val isAcross = interpreter.isAcross
  val marginSize = 3

  def width = getTerminal.getWidth

  def height = getTerminal.getHeight

  private def morePrompt = "--More--"

  private def emulateMore(): Int = {
    val key = readOneKey(morePrompt)
    try key match {
      case '\r' | '\n' => 1
      case 'q' => -1
      case _ => height - 1
    }
    finally {
      eraseLine()
      // TODO: still not quite managing to erase --More-- and get
      // back to a scala prompt without another keypress.
      if (key == 'q') {
        putString(getPrompt)
        redrawLine()
        flush()
      }
    }
  }

  override def printColumns(items: JCollection[_ <: CharSequence]): Unit = {
    printColumns_(javaCharSeqCollectionToScala(items))
  }

  private def printColumns_(items: List[String]): Unit = if (items exists (_ != "")) {
    val grouped = tabulate(items)
    var linesLeft = if (isPaginationEnabled) height - 1 else Int.MaxValue
    grouped foreach { xs =>
      println(xs.mkString)
      linesLeft -= 1
      if (linesLeft <= 0) {
        linesLeft = emulateMore()
        if (linesLeft < 0)
          return
      }
    }
  }

  private def javaCharSeqCollectionToScala(xs: JCollection[_ <: CharSequence]): List[String] = {
    import scala.collection.JavaConverters._
    xs.asScala.toList map ("" + _)
  }

  def readOneKey(prompt: String) = {
    this.print(prompt)
    this.flush()
    this.readCharacter()
  }

  def eraseLine() = resetPromptLine("", "", 0)

  def redrawLineAndFlush(): Unit = {
    flush()
    drawLine()
    flush()
  }

  // A hook for running code after the repl is done initializing.
  def initCompletion(completion: Completion): Unit = {
    this setBellEnabled false

    if (completion ne interpreter.NoCompletion) {
      val jlineCompleter = new ArgumentCompleter(new JLineDelimiter,
        new Completer {
          val tc = completion.completer()

          def complete(_buf: String, cursor: Int, candidates: JList[CharSequence]): Int = {
            val buf = if (_buf == null) "" else _buf
            val Candidates(newCursor, newCandidates) = tc.complete(buf, cursor)
            newCandidates foreach (candidates add _)
            newCursor
          }
        }
      )

      jlineCompleter setStrict false

      this addCompleter jlineCompleter
      this setAutoprintThreshold 400 // max completion candidates without warning
    }
  }
}
