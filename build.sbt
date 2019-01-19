name := "example-proj"

version := "0.1"

scalaVersion := "2.12.8"
scalacOptions += "-Ypartial-unification"

val akkaVersion = "2.5.8"
val akkaHttpVersion = "10.0.11"
val catsVersion = "1.5.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka"             %% "akka-actor"                    % akkaVersion,
  "com.typesafe.akka"             %% "akka-http-core"                % akkaHttpVersion,
  "com.typesafe.akka"             %% "akka-stream"                   % akkaVersion,
  "com.typesafe.akka"             %% "akka-stream-kafka"             % "0.22",
  "org.typelevel"                 %% "cats-core"                     % catsVersion
)
