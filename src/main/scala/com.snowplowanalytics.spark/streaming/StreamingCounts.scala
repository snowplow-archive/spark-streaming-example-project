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


// Spark
import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.kinesis.KinesisUtils

// This project
import storage.DynamoUtils
import kinesis.{KinesisUtils => KU}


/**
 * Core of the Spark Streaming Application
 * 1. Configuration information is brought in from StreamingCountsApp.scala
 * 2. Object sets up Kinesis, DynamoDB, CloudTrail connections
 * 3. Once connections are up, Spark StreamingCounts stream processing starts
 * AWS Kinesis -> Apache Spark Streaming -> AWS DynamoDB
 * Raw Data    -> Stream Processing Data -> Stored in Database
 *
 * (More on Spark Streaming: https://spark.apache.org/docs/1.3.0/streaming-kinesis-integration.html)
 */
object StreamingCounts {

  private val AppName = "StreamingCounts"

  /**
   * Private function to set up Spark Streaming
   *
   * @param config The configuration for our job using StreamingCountsConfig.scala
   */
  private def setUpSparkContext(config: StreamingCountsConfig): StreamingContext = {
    val streamingSparkContext = {
      val sparkConf = new SparkConf().setAppName(AppName).setMaster(config.master)
      new StreamingContext(sparkConf, config.batchInterval)
    }
    streamingSparkContext
  }

  /**
   * Starts our processing of a single Kinesis stream.
   * Never ends.
   *
   * @param config The configuration for our job using StreamingCountsConfig.scala
   */
  def execute(config: StreamingCountsConfig) {

    // setting up Spark Streaming connection to Kinesis
    val kinesisClient = KU.setUpKinesisClientConnection(config)
    require(kinesisClient != null,
      "No AWS credentials found. Please specify credentials using one of the methods specified " +
        "in http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/credentials.html")
    val streamingSparkContext = setUpSparkContext(config)
    val numShards = KU.getShardCount(kinesisClient, config.streamName)
    val sparkDStreams = (0 until numShards).map { i =>
      KinesisUtils.createStream(ssc=streamingSparkContext,
        streamName=config.streamName,
        endpointUrl=config.endpointURL,
        initialPositionInStream=config.initialPosition,
        checkpointInterval=config.batchInterval,
        storageLevel=config.storageLevel)
    }

    // Spark Stream processing of raw events into aggregate events
    val unionStreams = streamingSparkContext.union(sparkDStreams)
    val event = unionStreams.map(byteArray => (SimpleEvent.fromJson(byteArray)))
    val bucketed = event.map(e => (e.bucket, e.`type`))
    val computedStream = bucketed.groupByKey()
    val countedEventTypesPerMinute = computedStream.map(row => (row._1, row._2.groupBy(identity).mapValues(_.size)))

    // iterating each aggregate record and saving the record into DynamoDB for persistent storage
    countedEventTypesPerMinute.foreachRDD { rdd =>
      rdd.foreach { recordsList =>
        recordsList._2.foreach { record =>
          // tableName:String, bucketStart:String, createdAt:String, eventType:String, updatedAt:String, count:String

          DynamoUtils.putItem(
            config.recordsTableName,
            recordsList._1.toString,
            DynamoUtils.timeNow(),
            record._1,
            DynamoUtils.timeNow(),
            record._2.toString
          )
        }
      }
    }

    // start Apache Spark Stream process
    streamingSparkContext.start()
    streamingSparkContext.awaitTermination()
  }
}