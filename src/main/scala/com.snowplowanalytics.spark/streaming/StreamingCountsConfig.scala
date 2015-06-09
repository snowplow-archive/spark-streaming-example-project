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

// AWS SDK
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream

// Spark
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.Duration

/**
 * Configuration object for our StreamingCounts job
 */
case class StreamingCountsConfig(
  region:             String,
  streamName:         String,
  checkpointInterval: Duration,
  initialPosition:    InitialPositionInStream,
  storageLevel:       StorageLevel,
  appName:            String,
  master:             String,
  batchInterval:      Duration,
  tableName:          String,
  awsProfile:         String
) {

  /**
   * The Kinesis endpoint from the region.
   */
  val endpointUrl = s"https://kinesis.${region}.amazonaws.com"
}
