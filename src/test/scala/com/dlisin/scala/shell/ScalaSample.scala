package com.dlisin.scala.shell

object ScalaSample {
  def main(args: Array[String]) {
    val sshd = new ScalaSshShell(
      replName = "test",
      port = 4444,
      user = "user",
      passwd = "fluke",

      // All the following parameters are OPTIONAL and have default values!

      // A list of OpenSSH-style host key file paths
      hostKeyPath = Some(List(
        "/tmp/ssh_host_rsa_key",
        "/tmp/ssh_host_dsa_key"
      )),

      // A list of all interfaces that SSHD should bind to
      host = Some(List(
        "127.0.0.1"
      )),

      // Initial bindings (same as if you add it later with .addInitialBinding method)
      initialBindings = Some(List(
        ("pi", 3.1415926)
      )),

      // Initial commands (same as if you add it later with .addInitialCommand method)
      initialCommands = Some(List(
        "import java.util.{Date => JDate}"
      ))
    )

    sshd.addInitialBinding("nums", Vector(1, 2, 3, 4, 5))

    sshd.addInitialCommand("import java.util.{ArrayList => JArrayList}")

    new Thread {
      override final def run() {
        sshd.start()
      }
    }.start()
    new java.util.Scanner(System.in) nextLine()
    sshd.stop()
  }
}
