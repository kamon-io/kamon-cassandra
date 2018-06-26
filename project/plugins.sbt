lazy val root: Project = project.in(file(".")).dependsOn(latestSbtUmbrella)
lazy val latestSbtUmbrella = uri("git://github.com/kamon-io/kamon-sbt-umbrella.git")

addSbtPlugin("com.lightbend.sbt" % "sbt-javaagent" % "0.1.3")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.7")


