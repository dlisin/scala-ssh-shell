package com.dlisin.scala.shell

object ScalaSample {
  def main(args: Array[String]) {
    val sshd = new ScalaSshShell(replName = "test",
      port = 4444, user = "user", passwd = "fluke",
      keysResourcePath = Some("/test.ssh.keys"))
    sshd.addInitialBinding("pi", 3.1415926)
    sshd.addInitialBinding("nums", Vector(1, 2, 3, 4, 5))
    new Thread {
      override final def run() {
        sshd.start()
      }
    }.start()
    new java.util.Scanner(System.in) nextLine()
    sshd.stop()
  }
}
