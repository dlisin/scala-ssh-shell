package com.dlisin.scala.shell

import grizzled.slf4j.Logging
import org.apache.sshd.SshServer
import org.apache.sshd.common.KeyPairProvider
import org.apache.sshd.common.keyprovider.AbstractKeyPairProvider
import java.io.{InputStreamReader, FileInputStream}
import org.apache.sshd.server.PasswordAuthenticator
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.server.session.ServerSession

import java.lang.{Iterable => JIterable}

import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.NamedParam

class ScalaSshShell(replName: String,
                    port: Int,
                    user: String,
                    passwd: String,
                    hostKeyPath: Option[List[String]] = None,
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
      import java.security.KeyPair
      import org.bouncycastle.openssl._
      import org.bouncycastle.openssl.jcajce._

      val converter = new JcaPEMKeyConverter()
      def readKeyPair(is: java.io.InputStream): Option[KeyPair] = {
        try {
          val pemParser = new PEMParser(new InputStreamReader(is))
          val pemKeyPair = pemParser.readObject().asInstanceOf[PEMKeyPair]
          Some(converter.getKeyPair(pemKeyPair))
        } catch {
          case e: Throwable =>
            logger.warn(s"HostKeyPairProvider: Skipping Host KeyPair because there was an error while trying to read it: ${e.getMessage}")
            None
        }
      }

      // All host keys loaded from classpath resources
      val hostKeyResourcePath = List("/ssh_host_dsa_key", "/ssh_host_rsa_key")
      val resourceKeys: List[KeyPair] = hostKeyResourcePath.flatMap { path =>
        val tmp = classOf[ScalaSshShell].getResourceAsStream(path)
        if (tmp == null) None
        else {
          readKeyPair(tmp)
        }
      }

      // All host keys loaded from provided resources
      val providedKeys: List[KeyPair] = hostKeyPath.map(_.map { path =>
        try {
          readKeyPair(new FileInputStream(path))
        } catch {
          case e: Throwable =>
            logger.warn(s"HostKeyPairProvider: Skipping Host KeyPair from $path because there was an error reading it", e)
            None
        }
      }).getOrElse(Nil).flatten

      // Merge the list
      val defaultKeyPairProvider = new SimpleGeneratorHostKeyProvider()
      val keys: List[KeyPair] = resourceKeys ::: providedKeys
      if(keys.isEmpty) {
        logger.info("HostKeyPairProvider: No Host KeyPairs were loaded. Falling back to SimpleGeneratorHostKeyProvider to generate keys for you.")
        defaultKeyPairProvider
      }
      else {
        logger.info(s"HostKeyPairProvider: has a total of ${keys.length} host KeyPairs loaded (${providedKeys.length} from provided paths / ${resourceKeys.length} from classpath resources)")
        new AbstractKeyPairProvider {
          import scala.collection.JavaConverters._
          override def loadKeys(): JIterable[KeyPair] = keys.asJava
        }
      }
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
