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

import java.text.SimpleDateFormat
import java.util.Date
import com.snowplowanalytics.spark.streaming.storage.BucketingStrategy
import org.json4s._
import org.json4s.jackson.JsonMethods._

/**
 * Companion object for creating a SimpleEvent
 * from incoming JSON
 */
object SimpleEvent {

  private val format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

  // Function converts date string into Date object
  def convertStringToDate(dateString: String): Date = format.parse(dateString)

  // Function converts Kinesis ByteArray of JSON data into SimpleEvent objects
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
 *   example log: { "dateString": "2015-05-27T00:23:24.306091", "eventType": "Red" }
 */
case class SimpleEvent(dateString: String, eventType: String) {

  // Function converts strings into Time Bucket using Bucketing Strategy
  def getBucket() = BucketingStrategy.bucket(SimpleEvent.convertStringToDate(dateString))

}
