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
package storage

// Java
import java.util.Date
import java.util.TimeZone
import java.text.SimpleDateFormat


// AWS Authentication
// http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html
import com.amazonaws.auth.profile.ProfileCredentialsProvider

// AWS DynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.{AttributeUpdate, DynamoDB, Item}

/**
 * Object sets up singleton that finds AWS credentials for DynamoDB to access the
 * aggregation records table. The utility function below puts items into the
 * "AggregateRecords" table.
 */
object DynamoUtils {

  val dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  val timezone = TimeZone.getTimeZone("UTC")

  /**
   * Function timezone helper
   */
  def timeNow(): String = {
    dateFormatter.setTimeZone(timezone)
    dateFormatter.format(new Date())
  }


  /**
   * Function wraps DynamoDB cred setup
   */
  def setupDynamoClientConnection(awsProfile: String): DynamoDB = {
    val credentials = new ProfileCredentialsProvider(awsProfile)
    val dynamoDB = new DynamoDB(new AmazonDynamoDBClient(credentials))
    dynamoDB
  }


  /**
   * Function wraps get or create item in DynamoDB table
   */
  def setOrUpdateCount(dynamoDB: DynamoDB, tableName: String, bucketStart: String, eventType: String, createdAt: String,  updatedAt: String, count: Int){

    val recordInTable = getItem(dynamoDB: DynamoDB, tableName, bucketStart, eventType)
    println(recordInTable)
    if (recordInTable == null) {
      DynamoUtils.putItem(dynamoDB: DynamoDB, tableName, bucketStart, eventType, createdAt, updatedAt, count)
    } else {
      val oldCreatedAt = recordInTable.getJSON("CreatedAt").replace("\"", "").replace("\\", "")
      val oldCount = recordInTable.getJSON("Count").toInt
      val newCount = oldCount + count.toInt
      DynamoUtils.putItem(dynamoDB: DynamoDB, tableName, bucketStart, eventType, oldCreatedAt, updatedAt, newCount)
    }
  }


  /**
   * Function wraps AWS Java getItemOutcome operation to DynamoDB table
   */
  def getItem(dynamoDB: DynamoDB, tableName: String, bucketStart: String, eventType: String): Item = {

    val table = dynamoDB.getTable(tableName)
    val items = table.getItemOutcome("BucketStart", bucketStart, "EventType", eventType)
    items.getItem
  }


  /**
   * Function wraps AWS Java putItem operation to DynamoDB table
   */
  def putItem(dynamoDB: DynamoDB, tableName: String, bucketStart: String, eventType: String, createdAt: String,  updatedAt: String, count: Int) {

    // AggregateRecords column names
    val tablePrimaryKeyName = "BucketStart"
    val tableEventTypeSecondaryKeyName = "EventType"
    val tableCreatedAtColumnName = "CreatedAt"
    val tableUpdatedAtColumnName = "UpdatedAt"
    val tableCountColumnName = "Count"

    try {
      val time = new Date().getTime - (1 * 24 * 60 * 60 * 1000)
      val date = new Date()
      date.setTime(time)
      dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"))
      val table = dynamoDB.getTable(tableName)
      println("Adding data to " + tableName)

      val item = new Item().withPrimaryKey(tablePrimaryKeyName, bucketStart)
        .withString(tableEventTypeSecondaryKeyName, eventType)
        .withString(tableCreatedAtColumnName, createdAt)
        .withString(tableUpdatedAtColumnName, updatedAt)
        .withInt(tableCountColumnName, count)

      // saving the data to DynamoDB AggregrateRecords table
      // println(item)
      table.putItem(item)
    } catch {
      case e: Exception => {
        System.err.println("Failed to create item in " + tableName)
        System.err.println(e.getMessage)
      }
    }
  }
}
