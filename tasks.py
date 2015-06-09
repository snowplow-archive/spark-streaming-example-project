# Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
#
# This program is licensed to you under the Apache License Version 2.0,
# and you may not use this file except in compliance with the Apache License Version 2.0.
# You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the Apache License Version 2.0 is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.

import datetime, json, uuid, time
from functools import partial
from random import choice

from invoke import run, task

import boto
from boto import kinesis
import boto.dynamodb2
from boto.dynamodb2.fields import HashKey, RangeKey, KeysOnlyIndex, GlobalAllIndex
from boto.dynamodb2.table import Table
from boto.dynamodb2.types import NUMBER


JAR_FILE = "spark-streaming-example-project-0.1-0.jar"

# Selection of EventType values
COLORS = ['Red','Orange','Yellow','Green','Blue']
        

# DynamoDB settings
THROUGHPUT_READ = 20
THROUGHPUT_WRITE = 20


# AWS Kinesis Data Generator
def picker(seq):
  """
  Returns a new function that can be called without arguments
  to select and return a random color
  """
  return partial(choice, seq)

def create_event():
  """
  Returns a choice of color and builds and event
  """
  event_id = str(uuid.uuid4())
  color_choice = picker(COLORS)

  return (event_id, {
    "id": event_id,
    "timestamp": datetime.datetime.now().isoformat(),
    "type": color_choice()
  })

def write_event(conn, stream_name):
  """
  Returns the event and event event_payload
  """
  event_id, event_payload = create_event()
  event_json = json.dumps(event_payload)
  conn.put_record(stream_name, event_json, event_id)
  return event_json


@task
def generate_events(profile, region, stream):
    """
    load demo data with python generator script for SimpleEvents
    """
    conn = kinesis.connect_to_region(region, profile_name=profile)
    while True:
        event_json = write_event(conn, stream)
        print "Event sent to Kinesis: {}".format(event_json)
        #time.sleep(5)

@task
def build_spark():
    """
    compile, build and assembly Apache Spark with Kinesis support
    """
    run("rm -rf master.zip spark-master", pty=True)
    run("wget https://github.com/apache/spark/archive/master.zip")
    run("unzip master.zip", pty=True)
    run('export MAVEN_OPTS="-Xmx1g -XX:MaxPermSize=256M -XX:ReservedCodeCacheSize=256m" && cd spark-master && mvn -Pkinesis-asl -DskipTests clean package', pty=True)


@task
def assemble_project():
    """
    build spark-streaming-example-project
    and package into "fat jar" ready for spark-submit
    """
    run("sbt assembly", pty=True)


@task
def create_profile(profile):
    """
    Create a profile
    """
    run("aws configure --profile {}".format(profile), pty=True)


@task
def create_dynamodb_table(profile, region, table):
    """
    DynamoDB table creation with AWS Boto library in Python
    """

    connection = boto.dynamodb2.connect_to_region(region, profile_name=profile)
    aggregate = Table.create(table,
                             schema=[
                                 HashKey("BucketStart"),
                                 RangeKey("EventType"),
                             ],
                             throughput={
                                 'read': THROUGHPUT_READ,
                                 'write': THROUGHPUT_WRITE
                             },
                             connection=connection
                             )


@task
def create_kinesis_stream(profile, stream):
    """
    create our Kinesis stream
    """

    # TODO: switch to use boto
    run("aws kinesis create-stream --stream-name {} --shard-count 1 --profile {}".format(stream, profile), pty=True)


@task
def describe_kinesis_stream(profile, stream):
    """
    show status Kinesis stream named eventStream
    """

    # TODO: switch to use boto
    run("aws kinesis describe-stream --stream-name {} --profile {}".format(stream, profile), pty=True)


@task
def run_project(config_path):
    """
    Submits the compiled "fat jar" to Apache Spark and
    starts Spark Streaming based on project settings
    """
    run("./spark-master/bin/spark-submit \
        --class com.snowplowanalytics.spark.streaming.StreamingCountsApp \
        --master local[4] \
        ./target/scala-2.10/{} \
        --config {}".format(JAR_FILE, config_path),
        pty=True)
