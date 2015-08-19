// vim: expandtab shiftwidth=4 softtabstop=4
/*
 * Copyright (c) 2012, Yang Bo. All rights reserved.
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */

package scala.tools.jline;

import jline.TerminalSupport;

/**
 * SshTerminal is like UnixTerminal, but it does not execute stty.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:dwkemp@gmail.com">Dale Kemp</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @author <a href="mailto:jbonofre@apache.org">Jean-Baptiste Onofrè</a>
 * @author <a href="mailto:pop.atry@gmail.com">杨博</a>
 * @since 2.0
 */
public class SshTerminal extends TerminalSupport {
  public SshTerminal() throws Exception {
    super(true);
  }

  /**
   * Remove line-buffered input by invoking "stty -icanon min 1"
   * against the current terminal.
   */
  @Override
  public void init() throws Exception {
    super.init();

    setAnsiSupported(true);
    setEchoEnabled(false);
  }
}
