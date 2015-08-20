package com.dlisin.scala.shell

import jline.TerminalSupport

private[shell] class SshTerminal extends TerminalSupport(true) {
  override def init(): Unit = {
    super.init()
    setAnsiSupported(true)
    setEchoEnabled(false)
  }
}
