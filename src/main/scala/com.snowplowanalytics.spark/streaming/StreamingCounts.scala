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

import com.amazonaws.services.dynamodbv2.document.DynamoDB
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

  /**
   * Private function to set up Spark Streaming
   *
   * @param config The configuration for our job using StreamingCountsConfig.scala
   */
  private def setupSparkContext(config: StreamingCountsConfig): StreamingContext = {
    val streamingSparkContext = {
      val sparkConf = new SparkConf().setAppName(config.appName).setMaster(config.master)
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
    val kinesisClient = KU.setupKinesisClientConnection(config.endpointUrl, config.awsProfile)
    require(kinesisClient != null,
      "No AWS credentials found. Please specify credentials using one of the methods specified " +
        "in http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/credentials.html")

    // setting up Spark Streaming connection to DynamoDB
    lazy val dynamoConnection = DynamoUtils.setupDynamoClientConnection(config.awsProfile)

    val streamingSparkContext = setupSparkContext(config)
    val numShards = KU.getShardCount(kinesisClient, config.streamName)
    val sparkDStreams = (0 until numShards).map { i =>
      KinesisUtils.createStream(
        ssc = streamingSparkContext,
        streamName = config.streamName,
        endpointUrl = config.endpointUrl,
        initialPositionInStream = config.initialPosition,
        checkpointInterval = config.batchInterval,
        storageLevel = config.storageLevel
        )
    }

    // Map phase: union DStreams, derive events, determine bucket
    val bucketedEvents = streamingSparkContext
      .union(sparkDStreams)
      .map { bytes =>
        val e = SimpleEvent.fromJson(bytes)
        (e.bucket, e.`type`)
      }

    // Reduce phase: group by key then by count
    val bucketedEventCounts = bucketedEvents
      .groupByKey
      .map { case (eventType, events) =>
        val count = events.groupBy(identity).mapValues(_.size)
        (eventType, count)
      }

    // Iterate over each aggregate record and save the record into DynamoDB
    bucketedEventCounts.foreachRDD { rdd =>
      rdd.foreach { case (bucket, aggregates) =>
        aggregates.foreach { case (eventType, count) =>
          DynamoUtils.setOrUpdateCount(
            dynamoConnection,
            config.tableName,
            bucket.toString,
            eventType,
            DynamoUtils.timeNow(),
            DynamoUtils.timeNow(),
            count.toInt
          )
        }
      }
    }

    // Start Spark Streaming process
    streamingSparkContext.start()
    streamingSparkContext.awaitTermination()
  }
}