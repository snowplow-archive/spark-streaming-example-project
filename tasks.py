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

from invoke import run, task
import boto
import datetime, json, uuid, time
from boto import kinesis
from functools import partial
from random import choice
import boto.dynamodb2
from boto.dynamodb2.fields import HashKey, RangeKey, KeysOnlyIndex, GlobalAllIndex
from boto.dynamodb2.table import Table
from boto.dynamodb2.types import NUMBER

JAR_FILE  = "spark-streaming-example-project-0.1.jar"

@task
def load_kinesis():
    """
    load demo data with python generator script for SimpleEvents

    Usage:
       inv load_kinesis
    """
    run("python scripts/data_load_kinesis.py", pty=True)


@task
def build_project():
    """
    build spark-streaming-example-project
    and package into "fat jar" for spark-submit

    Usage:
       inv build_project
    """
    run("sbt assembly", pty=True)


@task
def get_spark():
    """
    wget Apache Spark zip file from github

    Usage:
       inv get_spark
    """
    run("wget https://github.com/apache/spark/archive/master.zip")

@task
def unzip_spark():
    """
    unzip Apache Spark zip file from github

    Usage:
       inv unzip_spark
    """
    run("unzip master.zip", pty=True)



@task
def mem_spark():
    """
    set memory maven for Apache Spark with Kinesis support

    Usage:
       inv mem_spark
    """
    run('export MAVEN_OPTS="-Xmx1g -XX:MaxPermSize=256M -XX:ReservedCodeCacheSize=256m"', pty=True)

@task
def build_spark():
    """
    compile, build and assembly Apache Spark with Kinesis support

    Usage:
       inv build_spark
    """
    run("mvn -Pkinesis-asl -DskipTests clean package", pty=True)


@task
def test_project():
    """
    Compile project and test

    Usage:
       inv test_project
    """
    run("sbt test", pty=True)


@task
def create_dynamodb_table():
    """
    DynamoDB table creation with AWS Boto library in Python

    Usage:
    inv create_dynamodb_table
    """
    # AWS DynamoDB settings
    dynamodb_log_postion_table_name = "AggregateRecords"
    throughput_read_value = 20
    throughput_write_value = 20
    primary_key_field_name = "BucketStart"
    secondary_key_field_name = "UpdatedAt"
    # DynamoDB table creation
    aggregate = Table.create(dynamodb_log_postion_table_name,
                             schema=[HashKey(primary_key_field_name),
                                     RangeKey(secondary_key_field_name),],
                             throughput={'read': throughput_read_value, 'write': throughput_write_value})
    print("Trying to create table in DynamoDB...it may take a couple of seconds")
    time.sleep(10)
    print("Almost there...still working")
    time.sleep(10)
    print("Going to try and insert some test data into table")

    # test item to put into table
    aggregate.put_item(data={
        'BucketStart': '2015-06-22T08:35:00.000',
        'UpdatedAt': '2015-06-22T08:35:12.345',
        'EventType': 'Click-Event-Lisa-Smith',
        'Count': '47'
    })
    print("Test item put in table AggregateRecords in DynamoDB")

@task
def add_credentials():
    """
    Submit AWS creds to the credentials file

    Usage:
        inv add_credentials
    """
    run("aws configure", pty=True)

@task
def create_kinesis():
    """
    create 1 Kinesis stream named eventStream

    Usage:
        inv create_kinesis
    """
    run("aws kinesis create-stream --stream-name eventStream --shard-count 1", pty=True)


@task
def show_kinesis():
    """
    show status Kinesis stream named eventStream

    Usage:
        inv show_kinesis
    """
    run("aws kinesis describe-stream --stream-name eventStream", pty=True)


@task
def kinesis_list_all():
    """
    shows all streams in Kinesis

    Usage:
        inv kinesis_list_all
    """
    run("aws kinesis list-streams", pty=True)


@task
def run_spark_streaming():
    """
    Submits the compiled "fat jar" to Apache Spark and
    starts spark streaming based on project settings

    Usage:
        inv run_spark_streaming
    """
    run("/home/ubuntu/spark/bin/spark-submit \
        --class \
        com.snowplowanalytics.spark.streaming.StreamingCountsApp \
        --master \
        local[4] \
        /home/ubuntu/spark-streaming-example-project/target/scala-2.10/spark-streaming-example-project-0.1.0.jar \
        --config \
        /home/ubuntu/spark-streaming-example-project/src/main/resources/config.hocon.sample",
        pty=True)

