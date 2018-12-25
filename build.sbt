lazy val root = (project in file("."))
  .settings(
    scalaVersion := "2.12.8",
    resolvers += "spring-milestone" at "https://repo.spring.io/milestone",
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.0"     % Compile,
      "io.monix"               %% "monix-reactive"     % "3.0.0-RC2" % Compile,
      "io.r2dbc"               % "r2dbc-client"        % "1.0.0.M6"  % Compile,
      "io.r2dbc"               % "r2dbc-h2"            % "1.0.0.M6"  % Test,
      "io.r2dbc"               % "r2dbc-postgresql"    % "1.0.0.M6"  % Test,
      "org.scalatest"          %% "scalatest"          % "3.0.5"     % Test
    ),
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Xfuture"),
    scalafmtOnCompile := true
  )
