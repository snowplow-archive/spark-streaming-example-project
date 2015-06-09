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
import java.text.SimpleDateFormat
import java.util.Date

// json4s
import org.json4s._
import org.json4s.jackson.JsonMethods._

// This project
import storage.BucketingStrategy

/**
 * Companion object for creating a SimpleEvent
 * from incoming JSON
 */
object SimpleEvent {

  private val format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

  /**
   * Converts date string into Date object
   */
  def convertStringToDate(dateString: String): Date = format.parse(dateString)

  /**
   * Converts Kinesis ByteArray of JSON data into SimpleEvent objects
   */
  def fromJson(byteArray: Array[Byte]): SimpleEvent = {
    implicit val formats = DefaultFormats
    val newString = new String(byteArray, "UTF-8")
    val parsed = parse(newString)
    parsed.extract[SimpleEvent]
  }

}

/**
 * Simple Class demonstrating an EventType log consisting of:
 *   1. ISO 8601 DateTime Object that will be downsampled
 *      (see BucketingStrategy.scala file for more details)
 *   2. A simple model of colors for this EventType:
 *      'Red','Orange','Yellow','Green', or 'Blue'
 *   example log: {"timestamp": "2015-06-05T13:00:22.540374", "type": "Orange", "id": "018dd633-f4c3-4599-9b44-ebf71a1c519f"}
 */
case class SimpleEvent(id: String, timestamp: String, `type`: String) {

  // Convert timestamp into Time Bucket using Bucketing Strategy
  val bucket = BucketingStrategy.bucket(SimpleEvent.convertStringToDate(timestamp))

}
