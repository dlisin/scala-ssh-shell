package com.dlisin.scala.shell

import java.io.{FileInputStream, InputStreamReader}
import java.lang.{Iterable => JIterable}
import java.util.{Collection => JCollection}

import grizzled.slf4j.Logging
import org.apache.sshd.SshServer
import org.apache.sshd.common.KeyPairProvider
import org.apache.sshd.common.keyprovider.AbstractKeyPairProvider
import org.apache.sshd.server.PasswordAuthenticator
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.server.session.ServerSession

import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.NamedParam
import scala.tools.nsc.typechecker.TypeStrings

object ScalaShell {

  import scala.collection.JavaConverters._

  def apply(replName: String,
            port: Int,
            user: String,
            passwd: String,
            hostKeyPath: Option[List[String]] = None,
            host: Option[List[String]] = None,
            initialBindings: Option[List[NamedParam]] = None,
            initialCommands: Option[List[String]] = None): ScalaShell = {
    new ScalaShell(
      replName = replName,
      port = port,
      user = user,
      passwd = passwd,
      hostKeyPath = hostKeyPath,
      host = host,
      initialBindings = initialBindings,
      initialCommands = initialCommands
    )
  }

  /**
   * Java API
   */
  def create(replName: String,
             port: Int,
             user: String,
             passwd: String): ScalaShell = {
    apply(
      replName = replName,
      port = port,
      user = user,
      passwd = passwd
    )
  }

  /**
   * Java API
   */
  def create(replName: String,
             port: Int,
             user: String,
             passwd: String,
             initialBindings: JCollection[NamedParam],
             initialCommands: JCollection[String]): ScalaShell = {
    apply(
      replName = replName,
      port = port,
      user = user,
      passwd = passwd,
      initialBindings = Option(initialBindings.asScala.toList),
      initialCommands = Option(initialCommands.asScala.toList)
    )
  }

  /**
   * Java API
   */
  def create(replName: String,
             port: Int,
             user: String,
             passwd: String,
             hostKeyPath: JCollection[String],
             host: JCollection[String],
             initialBindings: JCollection[NamedParam],
             initialCommands: JCollection[String]): ScalaShell = {
    apply(
      replName = replName,
      port = port,
      user = user,
      passwd = passwd,
      hostKeyPath = Option(hostKeyPath.asScala.toList),
      host = Option(host.asScala.toList),
      initialBindings = Option(initialBindings.asScala.toList),
      initialCommands = Option(initialCommands.asScala.toList)
    )
  }

  /**
   * Java API
   */
  def create(replName: String,
             port: Int,
             user: String,
             passwd: String,
             hostKeyPath: JCollection[String],
             initialBindings: JCollection[NamedParam],
             initialCommands: JCollection[String]): ScalaShell = {
    apply(
      replName = replName,
      port = port,
      user = user,
      passwd = passwd,
      hostKeyPath = Option(hostKeyPath.asScala.toList) // ToDo
    )
  }
}

class ScalaShell(replName: String,
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
        val tmp = classOf[ScalaShell].getResourceAsStream(path)
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
      if (keys.isEmpty) {
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

/**
 * Java API
 */
class Binding(val name: String, val tpe: String, val value: Any) extends NamedParam {
  def this(name: String, value: Any) = this(name, TypeStrings.fromValue(value), value)
}
