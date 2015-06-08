# Spark Streaming Example Project

[ ![Build Status] [travis-image] ] [travis]  [ ![Release] [release-image] ] [releases] [ ![License] [license-image] ] [license]

## Introduction

This is a simple time series analysis job written in Scala for the [Spark] [spark] Streaming cluster computing platform.
__First__, this app generates/sends raw events to AWS Kinesis. __Second__, we process the raw events with Apache Spark Streaming. Our data processing
sorts each event into a "bucket". __Third__, Spark counts and aggregates the raw events into 1 minute buckets. __Last__, this Spark app takes the aggregate records and saves the aggregate events into AWS DynamoDB Database. Read More about 
[Kinesis and Spark Streaming](https://spark.apache.org/docs/latest/streaming-kinesis-integration.html).
Follow along in this [blog post] (http://snowplowanalytics.com/blog/2015/06/09/spark-streaming-example-project-0.1.0-released/) to get the project up and running yourself.

Input: Example of a raw events in JSON format

```bash
{ "dateString": "2015-06-22T00:23:24.306091", "eventType": "Red" }
{ "dateString": "2015-06-22T00:23:24.450129", "eventType": "Yellow" }
{ "dateString": "2015-06-22T00:23:24.826703", "eventType": "Blue" }
```

Output: Example of the AggregateRecords table in DynamoDB
![data table png][data-table]


This was built by the Data Science team at [Snowplow Analytics] [snowplow], who use Spark on their [Data pipelines and algorithms] [data-pipelines-algos] projects.

Please ensure your AWS credentials have access policies assigned to use Cloudwatch, Kinesis, and DynamoDB services.

We assume you have an Internet connection so we can access services and download code from github. Also, you will need git, Vagrant and VirtualBox installed locally. This project is specifically configured to run in AWS region "us-east-1" to ensure all AWS services are available. Building Spark on a vagrant box requires RAM. Ensure you have at least 8GB of RAM and 64 bit OS hosting vagrant.

** Running this requires an Amazon AWS account, and it will incur charges **
	
	
# Quickstart for those who have Apache Spark already compiled with Kinesis

#### Step 1 - Cloning Spark-Streaming-Example-Project

```bash
host> git clone repo
```

#### Step 2 - Building Spark-Streaming-Example-Project

```bash
host> sbt assembly
```

The 'fat jar' is now available as:

```bash
target/scala-2.10/simple-project_2.10-0.1.jar
```

## Developer Quickstart

Assuming git, **[Vagrant] [vagrant-install]** and **[VirtualBox] [virtualbox-install]** installed:

```bash
 host$ git clone https://github.com/snowplow/spark-streaming-example-project.git
 host$ cd spark-streaming-example-project
 host$ vagrant up && vagrant ssh
guest$ cd /vagrant
guest$ sbt compile
```

## Tutorial
Follow along in this [blog post] (http://snowplowanalytics.com/blog/2015/06/09/spark-streaming-example-project-0.1.0-released/) to get the project up and running yourself.


```bash
$ inv create_profile my-profile
AWS Access Key ID [None]: ...
AWS Secret Access Key [None]: ...
Default region name [None]: eu-west-1
Default output format [None]:
```

```bash
$ inv create_kinesis_stream my-profile my-stream
```

Wait a minute and then:

```bash
$ inv describe_kinesis_stream my-profile my-stream
{
    "StreamDescription": {
        "StreamStatus": "ACTIVE",
        "StreamName": "my-stream",
        "StreamARN": "arn:aws:kinesis:eu-west-1:719197435995:stream/my-stream",
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


```bash
$ inv create_dynamodb_table my-profile eu-west-1 my-table
```

Now start sending events to the stream:

```bash
$ inv generate_events my-profile eu-west-1 my-stream
Event sent to Kinesis: {"timestamp": "2015-06-05T12:54:43.064528", "type": "Green", "id": "4ec80fb1-0963-4e35-8f54-ce760499d974"}
Event sent to Kinesis: {"timestamp": "2015-06-05T12:54:43.757797", "type": "Red", "id": "eb84b0d1-f793-4213-8a65-2fb09eab8c5c"}
Event sent to Kinesis: {"timestamp": "2015-06-05T12:54:44.295972", "type": "Yellow", "id": "4654bdc8-86d4-44a3-9920-fee7939e2582"}
...
```

Building Spark Streaming with Kinesis support:

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

Submit your application to Spark:
Open a new terminal window. Start a second shell into the vagrant box with:
```bash
host> vagrant ssh
```
Start Apache Spark Streaming system with this command:
```bash
vagrant@spark-streaming-example-project:/vagrant$   inv spark_streaming
```

__NOTE__: For those interested in understanding what is behind the __inv spark_streaming__ command:
```bash
$SPARK_HOME/bin/spark-submit --class com.snowplowanalytics.spark.streaming.StreamingCountsApp \
                             --master local[2] \
                             spark-streaming-example-project/target/scala-2.10/spark-streaming-example-project-0.1.0.jar \
                             --config spark-streaming-example-project/src/main/resources/config.hocon.sample
```

## Next steps

Fork this project and adapt it into your own custom Spark job.

To invoke/schedule your Spark job on EMR, check out:

* [Spark Plug] [spark-plug] for Scala
* [Elasticity] [elasticity] for Ruby
* [Boto] [boto] for Python
* [Lemur] [lemur] for Clojure

## Roadmap

* Maybe a Spark Streaming Machine Learning example

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

[spark]: http://spark-project.org/
[wordcount]: https://github.com/twitter/scalding/blob/master/README.md
[snowplow]: http://snowplowanalytics.com
[data-pipelines-algos]: http://snowplowanalytics.com/services/pipelines.html

[vagrant-install]: http://docs.vagrantup.com/v2/installation/index.html
[virtualbox-install]: https://www.virtualbox.org/wiki/Downloads

[spark-streaming-example-project]: https://github.com/snowplow/spark-streaming-example-project
[scalding-example-project]: https://github.com/snowplow/scalding-example-project

[issue-1]: https://github.com/snowplow/spark-example-project/issues/1
[issue-2]: https://github.com/snowplow/spark-example-project/issues/2
[aws-spark-tutorial]: http://aws.amazon.com/articles/4926593393724923
[spark-emr-howto]: https://forums.aws.amazon.com/thread.jspa?messageID=458398

[emr]: http://aws.amazon.com/elasticmapreduce/
[hello-txt]: https://github.com/snowplow/spark-example-project/raw/master/data/hello.txt
[emr-client]: http://aws.amazon.com/developertools/2264

[elasticity]: https://github.com/rslifka/elasticity
[spark-plug]: https://github.com/ogrodnek/spark-plug
[lemur]: https://github.com/TheClimateCorporation/lemur
[boto]: http://boto.readthedocs.org/en/latest/ref/emr.html


[data-table]: https://raw.githubusercontent.com/bigsnarfdude/snowplow.github.com/spark-streaming-example-project/assets/img/blog/2015/06/aggregateRecords2.png
