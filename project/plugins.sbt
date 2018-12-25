scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

// MiMa ensures bin-compatibility among patch releases
// https://github.com/lightbend/migration-manager
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.3.0")

// Code formatter for Scala
// https://scalameta.org/scalafmt/
addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "1.5.1")

// Publishing this library to Sonatype repositories + Maven Central
// https://www.scala-sbt.org/1.0/docs/Using-Sonatype.html
addSbtPlugin("com.jsuereth"   % "sbt-pgp"      % "1.1.2")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.3")
