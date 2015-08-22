package com.dlisin.scala.shell;

import java.util.Arrays;
import java.util.Collections;

public class JavaSample {
  public static void main(String[] args) {
    ScalaShell sshd = ScalaShell.create(
      "test",  // Repl name
      4444,    // Port
      "user",  // User
      "fluke", // Password

      // All the following parameters are OPTIONAL and have default values!

      // A list of OpenSSH-style host key file paths
      Arrays.asList(
        "~/.ssh/id_dsa",
        "~/.ssh/id_rsa"
      ),

      // A list of all interfaces that SSHD should bind to
      Collections.singleton(
        "127.0.0.1"
      ),

      // Initial bindings (same as if you add it later with .addInitialBinding method)
      Collections.singleton(
        new Binding("pi", 3.1415926)
      ),

      // Initial commands (same as if you add it later with .addInitialCommand method)
      Collections.singleton(
        "import java.util.{Date => JDate}"
      )
    );

    sshd.addInitialBinding(new Binding("nums", Arrays.asList(1, 2, 3, 4, 5)));

    sshd.addInitialCommand("import java.util.{ArrayList => JArrayList}");

    new Thread(sshd::start).start();
    new java.util.Scanner(System.in).nextLine();

    sshd.stop();
  }
}
