name := "play-rabbitmq-eventstream"

version := "1.0.0-SNAPSHOT"

organization := "de.thovid"

startYear := Some(2014)

description := "Play framework 2.x module to use rabbitMQ for publish subscribe between different play applications"

licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

scmInfo := Some(ScmInfo(url("https://github.com/thovid/play-rabbitmq-eventstream"), "https://github.com/thovid/play-rabbitmq-eventstream.git"))

libraryDependencies ++= Seq(
  "com.github.sstone" % "amqp-client_2.10" % "1.3",
  "com.typesafe.akka" %% "akka-actor" % "2.2.3", 
  "com.typesafe.akka" %% "akka-slf4j" % "2.2.3",
  "org.mockito" % "mockito-all" % "1.9.0" % "test"
)     

play.Project.playScalaSettings
