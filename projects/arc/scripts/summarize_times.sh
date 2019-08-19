#!/bin/bash

SCRIPTPATH=`readlink -f $0`
BASEDIR="`dirname $SCRIPTPATH`/.."
LOGSDIR=$BASEDIR/logs

OKLOG="$LOGSDIR/ok.log"
grep OK $LOGSDIR/bulk-full.log | cut -f1 -d' ' > $OKLOG

RAWLOGFILE="full.log"
TIMELOGFILE="times.log"
SUMMARYCSV="$LOGSDIR/times.csv"

echo "network,parse,processEtg,policyGroups,separatePolicyGroups,flowEtgs,pruneEtgs,alwaysBlocked,alwaysReachable" > $SUMMARYCSV

cat $OKLOG | while read NETWORK; do
    echo $NETWORK
    NWLOGDIR=$LOGSDIR/$NETWORK
    grep "TIME:" $NWLOGDIR/$RAWLOGFILE > $NWLOGDIR/$TIMELOGFILE
    COUNTS=`cat $NWLOGDIR/$TIMELOGFILE | cut -f3 -d' '`
    COUNTS=`echo $COUNTS | sed -e 's/ /,/g'`
    echo $NETWORK,$COUNTS >> $SUMMARYCSV
done
