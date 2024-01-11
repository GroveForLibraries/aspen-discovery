#!/bin/sh
if [ -z "$1" ]
  then
    echo "Please provide the server name to update as the first argument."
    exit 1
fi

yum install -y java-11-openjdk
update-alternatives --set java /usr/lib/jvm/java-11-openjdk-11.0.21.0.9-1.el7_9.x86_64/bin/java

#Update the startup script to call solr 8 rather than solr 7
