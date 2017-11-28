name := "Mittere"
version := "1.0"
scalaVersion := "2.12.3"
cancelable in Global := true
lazy val akkaVersion = "2.5.6"
connectInput in run := true
mainClass := Some("MyApp")

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

libraryDependencies ++= Seq(
  "org.scalafx" %% "scalafx" % "8.0.102-R11",
  "org.scalafx" %% "scalafxml-core-sfx8" % "0.4",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
)

fork := true

