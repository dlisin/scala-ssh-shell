/* NSC -- new Scala compiler
 * Copyright 2005-2011 LAMP/EPFL
 * @author Alexander Spoon
 */

package peak6.util

import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter._

class SshILoop(in: InputStream, out: OutputStream, bindValues: NamedParam*)
  extends ILoop(None, new JPrintWriter(out)) {

  override final def chooseReader(settings: Settings): InteractiveReader = {
    new SshJLineReader(in, out, new JLineCompletion(intp))
  }

  override def processLine(line: String): Boolean = {
    command(line) match {
      case Result(false, _) => false
      case Result(_, Some(line)) => addReplay(line); true
      case _ => true
    }
  }

}
