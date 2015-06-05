#!/usr/bin/env python

import datetime, json, uuid, time
from boto import kinesis
from functools import partial
from random import choice
import boto.dynamodb2
from boto.dynamodb2.fields import HashKey, RangeKey, KeysOnlyIndex, GlobalAllIndex
from boto.dynamodb2.table import Table
from boto.dynamodb2.types import NUMBER

"""
This program loads synthetic EventType log data simulating logs with datetime stamp and EventType
"""

# Selection of EventType values
colors = ['Red','Orange','Yellow','Green','Blue']

# AWS Kinesis settings
kinesis_stream_name = "eventStream"
aws_region_name = "us-east-1"

# AWS Kinesis
def picker(seq):
  """
  Returns a new function that can be called without arguments 
  to select and return a random color
  """
  return partial(choice,seq)

def get_event_id():
  """
  Returns UUID as string
  """
  return str(uuid.uuid4())

def get_event_time():
  """
  Returns datetime stamp in ISO format
  """
  return datetime.datetime.now().isoformat()

def create_event():
  """
  Returns a choice of color and builds and event
  """
  event_id = get_event_id()
  color_choice = picker(colors)
  return (event_id, {
    "dateString": get_event_time(),
    "eventType": color_choice()
  })

def write_event(conn, stream_name):
  """
  Returns the event and event event_payload
  """
  event_id, event_payload = create_event()
  event_json = json.dumps(event_payload)
  conn.put_record(stream_name, event_json, event_id)
  print(event_json, event_id)
  return event_id

if __name__ == '__main__':
  conn = kinesis.connect_to_region(region_name=aws_region_name)
  while True:
    event_id = write_event(conn, kinesis_stream_name)
    print "Event sent to Kinesis: {}".format(event_id)
    time.sleep(.5)