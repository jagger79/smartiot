#!/bin/bash

cd /srv/build
OPTS="$OPTS -Dserver.port=8081"
OPTS="$OPTS -Dzigbee.publisher.enabled=false"
OPTS="$OPTS -Dzigbee.subscriber.enabled=false"
export OPTS

java -version
java $OPTS -jar *.jar
