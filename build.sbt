name := "redis-client-examples"

scalaVersion := "2.11.2"

version := "1.0"

libraryDependencies ++= Seq(
  "net.debasishg" %% "redisclient" % "2.13",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.4.3",
  "org.msgpack" %% "msgpack-scala" % "0.6.11"
)
