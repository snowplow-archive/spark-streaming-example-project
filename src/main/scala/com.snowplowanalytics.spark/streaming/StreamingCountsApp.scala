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
package com.snowplowanalytics.spark.streaming

// Java
import java.io.File
import java.io.FileReader
import java.util.Properties

// AWS libs
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream
import com.amazonaws.services.kinesis.connectors.KinesisConnectorConfiguration

// Config
import com.typesafe.config.{Config, ConfigFactory}

// Argot
import org.clapper.argot._

// Spark
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{Minutes, Seconds, Milliseconds}

/**
 * The entry point class for the Spark Streaming Application.
 *
 * Usage:
 *
 * spark/bin/spark-submit --class com.snowplowanalytics.spark.streaming.StreamingCountsApp \
 *                        --master local[2] \
 *                        spark-streaming-example-project/target/scala-2.10/spark-streaming-example-project-0.1.0.jar \
 *                        --config spark-streaming-example-project/src/main/resources/config.hocon.sample
*/
object StreamingCountsApp {

  def main(args: Array[String]) {

    // General bumf for our app
    val parser = new ArgotParser(
      programName = "generated",
      compactUsage = true,
      preUsage = Some("%s: Version %s. Copyright (c) 2015, %s.".format(
        generated.Settings.name,
        generated.Settings.version,
        generated.Settings.organization)
      )
    )

    // Optional config argument
    val config = parser.option[Config](List("config"),
      "filename",
      "Configuration file.") {
      (c, opt) =>

        val file = new File(c)
        if (file.exists) {
          ConfigFactory.parseFile(file)
        } else {
          parser.usage("Configuration file \"%s\" does not exist".format(c))
          ConfigFactory.empty()
        }
    }
    parser.parse(args)

    // read the config file if --config parameter is provided else fail
    val conf = config.value.getOrElse(throw new RuntimeException("--config argument must be provided"))

    // create Spark Streaming Config from hocon file in resource directory
    val scc = StreamingCountsConfig(
      region = conf.getConfig("kinesis").getString("region"),
      streamName = conf.getConfig("kinesis").getString("streamName"),
      checkpointInterval = Minutes(conf.getConfig("spark").getInt("checkpointInterval")),
      initialPosition = InitialPositionInStream.LATEST,
      storageLevel = StorageLevel.MEMORY_AND_DISK_2,
      appName = conf.getConfig("spark").getString("appName"),
      master = conf.getConfig("spark").getString("master"),
      batchInterval =  Milliseconds(conf.getConfig("spark").getInt("batchInterval")),
      tableName = conf.getConfig("dynamodb").getString("tableName"),
      awsProfile = conf.getConfig("aws").getString("awsProfile")
    )

    // start StreamingCounts application with config object
    StreamingCounts.execute(scc)
  }
}
