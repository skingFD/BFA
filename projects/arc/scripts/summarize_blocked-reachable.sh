#!/bin/bash

SCRIPTPATH=`readlink -f $0`
BASEDIR="`dirname $SCRIPTPATH`/.."
OUTPUTDIR=$BASEDIR/../../output
LOGDIR=$OUTPUTDIR/logs/$1
SUMMARYDIR=$OUTPUTDIR/summary/$1
NETWORKLIST=$BASEDIR/../../configs/networks.txt

TIMESCSV=$SUMMARYDIR/blocked-reachable_times.csv
BLOCKEDCSV=$SUMMARYDIR/blocked_times.csv
REACHABLECSV=$SUMMARYDIR/reachable_times.csv
COUNTSCSV=$SUMMARYDIR/blocked-reachable_counts.csv
mkdir -p $SUMMARYDIR

echo "network,parse,processEtg,policyGroups,separatePolicyGroups,flowEtgs,pruneEtgs,alwaysBlocked,alwaysReachable" > $TIMESCSV

echo "network,devices,deviceEtgVertices,deviceEtgEdges,processEtgVertices,processEtgEdges,ospfProcesses,bgpProcesses,staticProcesses,processEtgDiameter,instanceEtgVertices,instanceEtgEdges,ospfInstances,bgpInstances,staticInstances,policyGroups,separatePolicyGroups" > $COUNTSCSV

echo "network,time" > $BLOCKEDCSV
echo "network,time" > $REACHABLECSV

cat $NETWORKLIST | while read NETWORK; do
    echo $NETWORK
    NWLOGDIR=$LOGDIR/$NETWORK
    NWLOG=$NWLOGDIR/blocked-reachable.log

    grep "TIME:" $NWLOG > $NWLOGDIR/blocked-reachable_time.log
    TIMES=`cat $NWLOGDIR/blocked-reachable_time.log | cut -f3 -d' '`
    TIMES=`echo $TIMES | sed -e 's/ /,/g'`
    echo $NETWORK,$TIMES >> $TIMESCSV

    grep "COUNT:" $NWLOG > $NWLOGDIR/blocked-reachable_count.log
    COUNTS=`cat $NWLOGDIR/blocked-reachable_count.log | cut -f3 -d' '`
    COUNTS=`echo $COUNTS | sed -e 's/ /,/g'`
    echo $NETWORK,$COUNTS >> $COUNTSCSV

    grep "TIMEONE: AlwaysBlocked" $NWLOG | cut -f3 -d' ' | sed -e 's/ /,/g' > $NWLOGDIR/blocked_time.log
    cat $NWLOGDIR/blocked_time.log | while read LINE; do
        echo $NETWORK,$LINE >> $BLOCKEDCSV
    done

    grep "TIMEONE: AlwaysReachable" $NWLOG | cut -f3 -d' ' | sed -e 's/ /,/g' > $NWLOGDIR/reachable_time.log
    cat $NWLOGDIR/reachable_time.log | while read LINE; do
        echo $NETWORK,$LINE >> $REACHABLECSV
    done
done
