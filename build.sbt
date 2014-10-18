name := "redis-client-examples"

scalaVersion := "2.11.2"

version := "1.0"

libraryDependencies ++= Seq(
  "net.debasishg" %% "redisclient" % "2.13",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.4.3",
  "org.msgpack" %% "msgpack-scala" % "0.6.11",
  "com.google.api-ads" % "ads-lib" % "1.33.0",  //DFPリクエスト用
  "com.google.api-ads" % "dfp-axis" % "1.33.0", //DFPリクエスト用
  "org.specs2" %% "specs2" % "2.3.12" % "test" // Specs2テスト用
)
