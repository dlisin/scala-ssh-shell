package com.dlisin.scala.ssh

import org.apache.sshd.common.keyprovider.AbstractKeyPairProvider
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.server.session.ServerSession

import scala.reflect.Manifest
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.NamedParam
import scala.tools.nsc.interpreter.NamedParam.Typed

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

  var bindings: Seq[NamedParam] = IndexedSeq()

  def bind[T: Manifest](name: String, value: T): Unit = {
    bind(new Typed[T](name, value))
  }

  def bind(binding: NamedParam): Unit = {
    bindings :+= binding
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

        override def getKeyTypes = getKeyType(pair)

        override def loadKey(s: String) = pair

        def loadKeys() = Array[java.security.KeyPair]()
      }
    else
      new SimpleGeneratorHostKeyProvider())

  val settings = { () =>
    val settings = new Settings()
    settings.embeddedDefaults(getClass.getClassLoader)
    settings.usejavacp.value = true
    settings.Yreplsync.value = true
    settings
  }

  sshd.setShellFactory(new ReplShellFactory(name, settings, bindings: _*))

  def start() {
    sshd.start()
  }

  def stop() {
    sshd.stop()
  }
}
