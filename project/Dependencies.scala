/*
 * Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

import sbt._

object Dependencies {
  val resolutionRepos = Seq(
    "Akka Repository" at "http://repo.akka.io/releases/",
    "Spray Repository" at "http://repo.spray.cc/"
  )

  object V {
    // Java
    val awsSdk               = "1.9.34"
    val awsKinesisConnectors = "1.1.1"
    // Scala
    val spark                = "1.3.0"
    val argot                = "1.0.3"
    // Add versions for your additional libraries here...
    // Scala (test)
    val specs2               = "1.13"
    val guava                = "11.0.1"
    val json4s               = "3.2.10"

  }

  object Libraries {
    // Java
    val awsSdk                = "com.amazonaws"    % "aws-java-sdk"                 % V.awsSdk
    val awsSdkCore            = "com.amazonaws"    % "aws-java-sdk-core"            % V.awsSdk
    val awsKinesisConnectors  = "com.amazonaws"    % "amazon-kinesis-connectors"    % V.awsKinesisConnectors

    // Scala
    val argot                 = "org.clapper"      %% "argot"                       % V.argot
    val sparkCore             = "org.apache.spark" %% "spark-core"                  % V.spark
    val sparkStreaming        = "org.apache.spark" %% "spark-streaming"             % V.spark
    val sparkStreamingKinesis = "org.apache.spark" %% "spark-streaming-kinesis-asl" % V.spark
    val json4s                = "org.json4s"       %% "json4s-jackson"              % V.json4s

    // Scala (test only)
    val specs2                = "org.specs2"       % "specs2_2.10"                  % V.specs2       % "test"
    val guava                 = "com.google.guava" % "guava"                        % V.guava        % "test"

    // Add additional libraries from mvnrepository.com (SBT syntax) here...
  }
}
