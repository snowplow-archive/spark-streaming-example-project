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
package kinesis

// AWS KCL
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.kinesis.AmazonKinesisClient


object KinesisUtils {

  /**
   * Singleton Object that takes 1.KCL/AWS credentials object. 2.Kinesis Stream
   * name. The utility function queries the stream name and determines how many
   * shards are in the stream so that the sharded data can be unioned/joined
   * for processing by Apache Spark Streaming application
   *
   * @param AmazonKinesisClient AWS Kinesis Client
   * @param String Kinesis Stream Name
   * @param StreamingCountsConfig Application Configuration information object
   */
  def getShardCount(kinesisClient: AmazonKinesisClient, stream: String): Int =
    kinesisClient
      .describeStream(stream)
      .getStreamDescription
      .getShards
      .size

  /**
   * Finds AWS Credential by provided awsProfile and creates Kinesis Client
   */
  def setupKinesisClientConnection(endpointUrl: String, awsProfile: String): AmazonKinesisClient = {
    val credentials = new ProfileCredentialsProvider(awsProfile)
    val akc = new AmazonKinesisClient(credentials)
    akc.setEndpoint(endpointUrl)
    akc
  }
}
