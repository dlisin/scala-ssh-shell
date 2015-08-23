package com.dlisin.scala.shell;

import java.util.Arrays;
import java.util.Collections;

public class JavaSample {
  public static void main(String[] args) {
    ScalaSshShell sshd = ScalaSshShell.create(
      "test",  // replName
      4444,    // port
      "user",  // user
      "fluke", // passwd

      // All the following parameters are OPTIONAL and have default values!

      // A list of all interfaces that SSHD should bind to
      Collections.singleton(
        "127.0.0.1"
      ),


      // A list of OpenSSH-style host key file paths
      Arrays.asList(
        "/tmp/id_dsa",
        "/tmp/id_rsa"
      ),

      // Path to an OpenSSH-style "authorized_keys" file
      "/tmp/authorized_keys",

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
