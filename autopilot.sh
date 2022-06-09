#!/bin/bash
# Check is Lock File exists, if not create it and set trap on exit
 if { set -C; 2>/dev/null >auto_pilot.lock; }; then
         trap "rm -f auto_pilot.lock" EXIT
 else
         echo "Lock file exists… exiting"
         exit
 fi

 mvn clean package
 java -jar target/auto-pilot-1.0-SNAPSHOT-spring-boot.jar $1 $2