name := "scala-ssh-shell"

organization := "com.peak6"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.10.4"

scalacOptions ++= Vector("-unchecked", "-deprecation", "-Ywarn-all")

javacOptions ++= Vector("-encoding", "UTF-8")

retrieveManaged := true

libraryDependencies <++= (scalaVersion) {
	(scala) => Seq(
	"org.scala-lang" % "scala-compiler" % scala,
	"org.scala-lang" % "jline" % scala,
	"org.clapper" %% "grizzled-slf4j" % "1.0.2",
	"org.slf4j" % "slf4j-simple" % "1.6.4",
	"org.bouncycastle" % "bcprov-jdk16" % "1.46",
	"org.apache.sshd" % "sshd-core" % "0.6.0"
	)}

