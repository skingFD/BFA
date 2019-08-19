#!/bin/bash

SCRIPTPATH=`readlink -f $0`
BASEDIR="`dirname $SCRIPTPATH`/.."
NETWORKLIST=$BASEDIR/../../configs/networks.txt

RED=$(tput setaf 1)
GREEN=$(tput setaf 2)
NORMAL=$(tput sgr0)
COL=50

SCRIPT=$1

EXTRAARGS=$2
echo $EXTRAARGS

#ls $BASEDIR/configs | while read NETWORK; do
cat $NETWORKLIST | while read NETWORK; do
    printf "%-40s" $NETWORK
    $BASEDIR/scripts/$SCRIPT $NETWORK "$EXTRAARGS" > /dev/null
    if [ $? -eq 0 ]; then
        echo -e "$GREEN[OK]$NORMAL"
    else
        echo -e "$RED[FAIL]$NORMAL"
    fi
done
