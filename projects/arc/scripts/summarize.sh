#!/bin/bash

BASEDIR="`dirname $0`/.."
LOGSDIR=$BASEDIR/logs

OKLOG=$LOGSDIR/ok.log
grep OK $LOGSDIR/bulk.log | cut -f1 -d' ' > $OKLOG

SUMMARYCSV=$LOGSDIR/summary.csv
CBCSV=$LOGSDIR/currentlyBlocked.csv
ABCSV=$LOGSDIR/alwaysBlocked.csv
ARCSV=$LOGSDIR/alwaysReachable.csv

echo "network,parseTime,baseETGTime,policyGroupsTime,separatePolicyGroupsTime,flowETGsTime,currentlyBlockedTime,pruneETGsTime,alwaysBlockedTime,alwaysIsolatedTime,alwaysReachableTime,devicesCount,baseETGVertices,baseETGEdges,baseETGDiameter,policyGroupsCount,separatePolicyGroupsCount" > $SUMMARYCSV

echo "network,time,vertices,edges,result" > $CBCSV
echo "network,time,vertices,edges,result" > $ABCSV
echo "network,time,vertices,edges,result" > $ARCSV

cat $OKLOG | while read NETWORK; do
    echo $NETWORK
    LOGDIR=$LOGSDIR/$NETWORK
    TIMES=`cat $LOGDIR/time.log | cut -f3 -d' '`
    TIMES=`echo $TIMES | sed -e 's/ /,/g'`
    COUNTS=`cat $LOGDIR/count.log | cut -f3 -d' '`
    COUNTS=`echo $COUNTS | sed -e 's/ /,/g'`
    echo $NETWORK,$TIMES,$COUNTS >> $SUMMARYCSV
    cat $LOGDIR/timeCurrentlyBlocked.log | while read LINE; do
        echo $NETWORK,$LINE >> $CBCSV
    done
    cat $LOGDIR/timeAlwaysBlocked.log | while read LINE; do
        echo $NETWORK,$LINE >> $ABCSV
    done
    cat $LOGDIR/timeAlwaysReachable.log | while read LINE; do
        echo $NETWORK,$LINE >> $ARCSV
    done
done
