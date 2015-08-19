package peak6.util

import jline.TerminalSupport

/**
 * SshTerminal is like UnixTerminal, but it does not execute stty.
 */
final class SshTerminal extends TerminalSupport(true) {
  override protected def init() = {
    super.init()
    setAnsiSupported(true)
    setEchoEnabled(false)
  }
}
