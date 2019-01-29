name := "example-proj"

version := "0.1"

scalaVersion := "2.12.8"
scalacOptions += "-Ypartial-unification"

val akkaVersion = "2.5.8"
val akkaHttpVersion = "10.0.11"
val catsVersion = "1.5.0"
val circeVersion = "0.11.1"

resolvers += Resolver.sonatypeRepo("releases")
addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8")

libraryDependencies ++= Seq(
  "com.typesafe.akka"             %% "akka-actor"                    % akkaVersion,
  "com.typesafe.akka"             %% "akka-http-core"                % akkaHttpVersion,
  "com.typesafe.akka"             %% "akka-stream"                   % akkaVersion,
  "com.typesafe.akka"             %% "akka-stream-kafka"             % "0.22",
  "de.heikoseeberger"             %% "akka-http-circe"               % "1.24.3",
  "io.circe"                      %% "circe-core"                    % circeVersion,
  "io.circe"                      %% "circe-generic"                 % circeVersion,
  "io.circe"                      %% "circe-parser"                  % circeVersion,
  "org.typelevel"                 %% "cats-core"                     % catsVersion
)
