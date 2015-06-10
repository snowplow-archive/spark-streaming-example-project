# Spark Streaming Example Project

[ ![Build Status] [travis-image] ] [travis]  [ ![Release] [release-image] ] [releases] [ ![License] [license-image] ] [license]

## Introduction

This is a simple time series analysis stream processing job ([introductory blog post] [blog-post]) written in Scala for the [Spark Streaming] [spark-streaming] cluster computing platform, processing JSON events from [Amazon Kinesis] [kinesis] and writing aggregates to [Amazon DynamoDB] [dynamodb].

This was built by the Data Science team at [Snowplow Analytics] [snowplow], who use Spark Streaming in their client projects.

**Running this requires an Amazon AWS account, and it will incur charges.**

_See also:_ [Spark Example Project] [spark-example-project] | [AWS Lambda Example Project] [aws-lambda-example-project]

## Overview

We have implemented a super-simple analytics-on-write stream processing job using Spark Streaming. Our Spark Streaming job reads a Kinesis stream containing events in a JSON format:

```json
{
  "timestamp": "2015-06-05T12:54:43.064528",
  "type": "Green",
  "id": "4ec80fb1-0963-4e35-8f54-ce760499d974"
}
```

Our job counts the events by `type` and aggregates these counts into 1 minute buckets. The job then takes these aggregates and saves them into a table in DynamoDB:

![dynamodb-table-image][dynamodb-table-image]

## Developer Quickstart

Assuming git, [Vagrant] [vagrant-install] and [VirtualBox] [virtualbox-install] installed:

```bash
 host$ git clone https://github.com/snowplow/spark-streaming-example-project.git
 host$ cd spark-streaming-example-project
 host$ vagrant up && vagrant ssh
guest$ cd /vagrant
guest$ sbt compile
```

## Tutorial

You can follow along in [the release blog post] [blog-post] to get the project up and running yourself.

The below steps assume that you are running inside Vagrant, as per the Developer Quickstart above.

### 1. Setting up AWS

First we need to configure a default AWS profile:

```bash
$ aws configure
AWS Access Key ID [None]: ...
AWS Secret Access Key [None]: ...
Default region name [None]: us-east-1
Default output format [None]: json
```

Now we create our Kinesis event stream:

```bash
$ inv create_kinesis_stream default my-stream
```

Wait a minute and then:

```bash
$ inv describe_kinesis_stream default my-stream
{
    "StreamDescription": {
        "StreamStatus": "ACTIVE",
        "StreamName": "my-stream",
        "StreamARN": "arn:aws:kinesis:us-east-1:719197435995:stream/my-stream",
        "Shards": [
            {
                "ShardId": "shardId-000000000000",
                "HashKeyRange": {
                    "EndingHashKey": "340282366920938463463374607431768211455",
                    "StartingHashKey": "0"
                },
                "SequenceNumberRange": {
                    "StartingSequenceNumber": "49551350243544458458477304430170758137221526998466166786"
                }
            }
        ]
    }
}
```

If the Kinesis response says that the stream is still being created, wait a minute and then try again.

Now create our DynamoDB table:

```bash
$ inv create_dynamodb_table default us-east-1 my-table
```

### 2. Sending events to Kinesis

We need to start sending events to our new Kinesis stream. We have created a helper method to do this - run the below and leave it running: 

```bash
$ inv generate_events default us-east-1 my-stream
Event sent to Kinesis: {"timestamp": "2015-06-05T12:54:43.064528", "type": "Green", "id": "4ec80fb1-0963-4e35-8f54-ce760499d974"}
Event sent to Kinesis: {"timestamp": "2015-06-05T12:54:43.757797", "type": "Red", "id": "eb84b0d1-f793-4213-8a65-2fb09eab8c5c"}
Event sent to Kinesis: {"timestamp": "2015-06-05T12:54:44.295972", "type": "Yellow", "id": "4654bdc8-86d4-44a3-9920-fee7939e2582"}
...
```

Now open up a separate terminal for the rest of the setup.

### 3. Running our job on Spark Streaming

First we need to build Spark Streaming with Kinesis support. This can take up to 90 minutes:

```bash
$ inv build_spark
...
[INFO] Spark Kinesis Integration ......................... SUCCESS [1:11.115s]
...
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 1:29:00.686s
[INFO] Finished at: Sun Jun 07 00:32:09 UTC 2015
[INFO] Final Memory: 94M/665M
[INFO] ------------------------------------------------------------------------
```

Now we build our application. This should take closer to 10 minutes:

```bash
$ inv build_project
...
```

Finally we can submit our job to Spark with this command:

```bash
$ inv run_project config/config.hocon.sample
...
```

If you have updated any of the configuration options above (e.g. stream name or region), then you will have to update the `config.hocon.sample` file accordingly.

### 4. Monitoring your job

First review the spooling output of the `run_project` command above - it's very verbose, but if you don't see any Java stack traces in there, then Spark Streaming should be running okay.

Now head over to your host machine's [localhost:4040] [localhost-4040] and you should see something like this:

![spark-ui-image][spark-ui-image]

You can see how our Spark Streaming job _discretizes_ the Kinesis event stream into 2-second-duration "micro-batches", which are each then processed as a discrete Spark job.

Finally, let's check the data in our DynamoDB table. Make sure you are in the correct AWS region, then click on `my-table` and hit the `Explore Table` button:

![dynamodb-table-image][dynamodb-table-image]

For each **BucketStart** and **EventType** pair, we see a **Count**, plus some **CreatedAt** and **UpdatedAt** metadata for debugging purposes. Our bucket size is 1 minute, and we have 5 discrete event types, hence the matrix of rows that we see.

## Roadmap

* Porting this job to [AWS Lambda] [aws-lambda-example-project]
* Various improvements for the [0.2.0 release] [020-milestone]
* Expanding our analytics-on-write thinking into our new [Icebucket] [icebucket] project

## Copyright and license

Copyright 2015 Snowplow Analytics Ltd.

Licensed under the [Apache License, Version 2.0] [license] (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[travis]: https://travis-ci.org/snowplow/spark-streaming-example-project
[travis-image]: https://travis-ci.org/snowplow/spark-streaming-example-project.png?branch=master
[license-image]: http://img.shields.io/badge/license-Apache--2-blue.svg?style=flat
[license]: http://www.apache.org/licenses/LICENSE-2.0
[release-image]: http://img.shields.io/badge/release-0.1.0-blue.svg?style=flat
[releases]: https://github.com/snowplow/spark-streaming-example-project/releases

[blog-post]: http://snowplowanalytics.com/blog/2015/06/10/spark-streaming-example-project-0.1.0-released/

[dynamodb-table-image]: /docs/dynamodb-table-image.png?raw=true
[spark-ui-image]: /docs/spark-ui-image.png?raw=true

[spark-streaming]: https://spark.apache.org/streaming/
[kinesis]: http://aws.amazon.com/kinesis
[dynamodb]: http://aws.amazon.com/dynamodb
[snowplow]: http://snowplowanalytics.com
[icebucket]: https://github.com/snowplow/icebucket

[vagrant-install]: http://docs.vagrantup.com/v2/installation/index.html
[virtualbox-install]: https://www.virtualbox.org/wiki/Downloads

[spark-example-project]: https://github.com/snowplow/spark-example-project
[aws-lambda-example-project]: https://github.com/snowplow/aws-lambda-example-project

[localhost-4040]: http://localhost:4040/

[020-milestone]: https://github.com/snowplow/spark-streaming-example-project/milestones/Version%200.2.0
