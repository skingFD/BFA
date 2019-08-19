#!/bin/bash

SCRIPTPATH=`readlink -f $0`
BASEDIR="`dirname $SCRIPTPATH`/.."
LOGSDIR=$BASEDIR/logs

OKLOG="$LOGSDIR/ok.log"
grep OK $LOGSDIR/bulk-basegraphs.log | cut -f1 -d' ' > $OKLOG

RAWLOGFILE="basegraphs-gen.log"
COUNTLOGFILE="counts.log"
SUMMARYCSV="$LOGSDIR/counts.csv"

echo "network,devices,processEtgVertices,processEtgEdges,ospfProcesses,bgpProcesses,staticProcesses,processEtgDiameter,instanceEtgVertices,instanceEtgEdges,ospfInstances,bgpInstances,staticInstances,deviceEtgVertices,deviceEtgEdges,policyGroups,separatePolicyGroups" > $SUMMARYCSV

cat $OKLOG | while read NETWORK; do
    echo $NETWORK
    NWLOGDIR=$LOGSDIR/$NETWORK
    grep "COUNT:" $NWLOGDIR/$RAWLOGFILE > $NWLOGDIR/$COUNTLOGFILE
    COUNTS=`cat $NWLOGDIR/$COUNTLOGFILE | cut -f3 -d' '`
    COUNTS=`echo $COUNTS | sed -e 's/ /,/g'`
    echo $NETWORK,$COUNTS >> $SUMMARYCSV
done
