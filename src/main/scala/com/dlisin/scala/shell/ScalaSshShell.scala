package com.dlisin.scala.shell

import grizzled.slf4j.Logging
import org.apache.sshd.SshServer
import org.apache.sshd.common.KeyPairProvider
import org.apache.sshd.common.keyprovider.AbstractKeyPairProvider
import org.apache.sshd.common.util.KeyUtils
import org.apache.sshd.server.PasswordAuthenticator
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.server.session.ServerSession

import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.NamedParam

class ScalaSshShell(replName: String,
                    port: Int,
                    user: String,
                    passwd: String,
                    keysResourcePath: Option[String] = None,
                    host: Option[List[String]] = None,
                    initialBindings: Option[List[NamedParam]] = None,
                    initialCommands: Option[List[String]] = None) extends Logging {

  private var _initialBindings = initialBindings.getOrElse(Seq.empty)
  private var _initialCommands = initialCommands.getOrElse(Seq.empty)

  private lazy val sshd = {
    val pwAuth: PasswordAuthenticator = {
      new org.apache.sshd.server.PasswordAuthenticator {
        def authenticate(u: String, p: String, s: ServerSession) =
          u == user && p == passwd
      }
    }

    val keyPairProvider: KeyPairProvider = {
      if (keysResourcePath.isDefined)
        new AbstractKeyPairProvider {
          val pair = new SimpleGeneratorHostKeyProvider() {
            val in = classOf[ScalaSshShell].getResourceAsStream(
              keysResourcePath.get)
            val get = doReadKeyPair(in)
          }.get

          override def getKeyTypes = KeyUtils.getKeyType(pair)

          override def loadKey(s: String) = pair

          def loadKeys() = new java.util.ArrayList[java.security.KeyPair]()
        }
      else
        new SimpleGeneratorHostKeyProvider()
    }

    val shellFactory = {
      val settings = { () =>
        val settings = new Settings()
        settings.embeddedDefaults(getClass.getClassLoader)
        settings.usejavacp.value = true
        settings.Yreplsync.value = true
        settings
      }

      new ReplShellFactory(settings, replName, _initialBindings, _initialCommands)
    }

    val sshd = SshServer.setUpDefaultServer()
    sshd.setPort(port)
    host.foreach(hostList => sshd.setHost(hostList.mkString(",")))
    sshd.setPasswordAuthenticator(pwAuth)
    sshd.setKeyPairProvider(keyPairProvider)
    sshd.setShellFactory(shellFactory)
    sshd
  }

  def addInitialBinding(binding: NamedParam): Unit = {
    _initialBindings :+= binding
  }

  def addInitialCommand(command: String): Unit = {
    _initialCommands :+= command
  }

  def start() {
    sshd.start()
  }

  def stop() {
    sshd.stop()
  }
}
