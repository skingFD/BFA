#!/bin/bash

SCRIPTPATH=`readlink -f $0`
BASEDIR="`dirname $SCRIPTPATH`/.."
OUTPUTDIR=$BASEDIR/../../output
LOGDIR=$OUTPUTDIR/logs/$1
SUMMARYDIR=$OUTPUTDIR/summary/$1
NETWORKLIST=$BASEDIR/../../configs/networks.txt

TIMESCSV=$SUMMARYDIR/equivalence_times.csv
COUNTSCSV=$SUMMARYDIR/equivalence_counts.csv
mkdir -p $SUMMARYDIR

echo "network,parse,processEtg,policyGroups,separatePolicyGroups,flowEtgs,equivalence,pruneEtgs" > $TIMESCSV

echo "network,devices,deviceEtgVertices,deviceEtgEdges,processEtgVertices,processEtgEdges,ospfProcesses,bgpProcesses,staticProcesses,processEtgDiameter,instanceEtgVertices,instanceEtgEdges,ospfInstances,bgpInstances,staticInstances,policyGroups,separatePolicyGroups" > $COUNTSCSV

cat $NETWORKLIST | while read NETWORK; do
    echo $NETWORK
    NWLOGDIR=$LOGDIR/$NETWORK
    NWLOG=$NWLOGDIR/veq.log

    grep "TIME:" $NWLOG > $NWLOGDIR/equivalence_time.log
    TIMES=`cat $NWLOGDIR/equivalence_time.log | cut -f3 -d' '`
    TIMES=`echo $TIMES | sed -e 's/ /,/g'`
    echo $NETWORK,$TIMES >> $TIMESCSV

    grep "COUNT:" $NWLOG > $NWLOGDIR/equivalence_count.log
    COUNTS=`cat $NWLOGDIR/equivalence_count.log | cut -f3 -d' '`
    COUNTS=`echo $COUNTS | sed -e 's/ /,/g'`
    echo $NETWORK,$COUNTS >> $COUNTSCSV
done
