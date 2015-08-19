/*
 * Copyright 2011 PEAK6 Investments, L.P.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package peak6.util

import java.io.PrintWriter

import grizzled.slf4j.Logging
import org.apache.sshd.common.keyprovider.AbstractKeyPairProvider
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.server.session.ServerSession

import scala.reflect.Manifest
import scala.tools.nsc.Settings
import scala.tools.nsc.Settings

object ScalaSshShell {
  def main(args: Array[String]) {
    val sshd = new ScalaSshShell(port = 4444, name = "test", user = "user",
      passwd = "fluke",
      keysResourcePath = Some("/test.ssh.keys"))
    sshd.bind("pi", 3.1415926)
    sshd.bind("nums", Vector(1, 2, 3, 4, 5))
    new Thread {
      override final def run() {
        sshd.start()
      }
    }.start()
    new java.util.Scanner(System.in) nextLine()
    sshd.stop()
  }

  def generateKeys(path: String) {
    val key = new SimpleGeneratorHostKeyProvider(path)
    key.loadKeys()
  }
}

class ScalaSshShell(port: Int, name: String, user: String, passwd: String,
                    keysResourcePath: Option[String]) {

  var bindings: Seq[(String, String, Any)] = IndexedSeq()

  def bind[T: Manifest](name: String, value: T) {
    bindings :+=(name, manifest[T].toString, value)
  }

  val sshd = org.apache.sshd.SshServer.setUpDefaultServer()
  sshd.setPort(port)
  sshd.setReuseAddress(true)
  sshd.setPasswordAuthenticator(
    new org.apache.sshd.server.PasswordAuthenticator {
      def authenticate(u: String, p: String, s: ServerSession) =
        u == user && p == passwd
    })

  sshd.setKeyPairProvider(
    if (keysResourcePath.isDefined)
    // 'private' is one of the most annoying things ever invented.
    // Apache's sshd will only generate a key, or read it from an
    // absolute path (via a string, eg can't work directly on
    // resources), but they do privide protected methods for reading
    // from a stream, but not into the internal copy that gets
    // returned when you call loadKey(), which is of course privite
    // so there is no way to copy it. So we construct one provider
    // so we can parse the resource, and then impliment our own
    // instance of another so we can return it from loadKey(). What
    // a complete waste of time.
      new AbstractKeyPairProvider {
        val pair = new SimpleGeneratorHostKeyProvider() {
          val in = classOf[ScalaSshShell].getResourceAsStream(
            keysResourcePath.get)
          val get = doReadKeyPair(in)
        }.get

        override def getKeyTypes() = getKeyType(pair)

        override def loadKey(s: String) = pair

        def loadKeys() = Array[java.security.KeyPair]()
      }
    else
      new SimpleGeneratorHostKeyProvider())

  sshd.setShellFactory(
    new org.apache.sshd.common.Factory[org.apache.sshd.server.Command] {
      def create() =
        new org.apache.sshd.server.Command with Logging {
          logger.info("Instantiated")
          var in: java.io.InputStream = null
          var out: java.io.OutputStream = null
          var err: java.io.OutputStream = null
          var exit: org.apache.sshd.server.ExitCallback = null
          var thread: Thread = null
          @volatile var inShutdown = false

          def setInputStream(in: java.io.InputStream) {
            this.in = in
          }

          def setOutputStream(out: java.io.OutputStream) {
            this.out = new java.io.OutputStream {
              override def close() {
                out.close()
              }

              override def flush() {
                out.flush()
              }

              override def write(b: Int) {
                if (b.toChar == '\n')
                  out.write('\r')
                out.write(b)
              }

              override def write(b: Array[Byte]) {
                var i = 0
                while (i < b.size) {
                  write(b(i))
                  i += 1
                }
              }

              override def write(b: Array[Byte], off: Int, len: Int) {
                write(b.slice(off, off + len))
              }
            }
          }

          def setErrorStream(err: java.io.OutputStream) {
            this.err = err
          }

          def setExitCallback(exit: org.apache.sshd.server.ExitCallback) {
            this.exit = exit
          }

          def start(env: org.apache.sshd.server.Environment) {
            thread = CrashingThread.start(Some("ScalaSshShell-" + name)) {

              val repl = new SshILoop(in, out)
              val settings = new Settings()
              settings.embeddedDefaults(getClass.getClassLoader)
              settings.usejavacp.value = true
              settings.Yreplsync.value = true

//              repl.createInterpreter()
//              repl.in = new SshJLineReader(in, out, () => new JLineCompletion(repl.intp))

//              if (repl.intp.reporter.hasErrors) {
//                logger.error("Got errors, abandoning connection")
//                return
//              }

//              repl.printWelcome()
              try {
//                repl.intp.initialize()
//                repl.intp.beQuietDuring {
//                  repl.intp.bind("stdout", null) // ToDo : out
//                  for ((bname, btype, bval) <- bindings)
//                    repl.bind(bname, btype, bval)
//                }
//                repl.intp.quietRun(
//                  """def println(a: Any) = {
//                       stdout.write(a.toString)
//                       stdout.write('\n')
//                     }""")
//                repl.intp.quietRun(
//                  """def exit = println("Use ctrl-D to exit shell.")""")

                repl.process(settings)
              } finally {
                repl.closeInterpreter()
              }

              logger.info("Exited repl, closing ssh.")
              exit.onExit(0)
            }
          }

          def destroy() {
            inShutdown = true
          }
        }
    })

  def start() {
    sshd.start()
  }

  def stop() {
    sshd.stop()
  }
}
