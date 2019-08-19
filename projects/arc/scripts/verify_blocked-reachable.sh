#!/bin/bash

SCRIPTPATH=`readlink -f $0`
BASEDIR="`dirname $SCRIPTPATH`/.."
CONFIGDIR="$BASEDIR/../../configs/examples/$1"
if [ ! -d "$CONFIGDIR" ]; then
    CONFIGDIR=$BASEDIR/../../configs/working/$1
fi
echo $CONFIGDIR
OUTPUTDIR=$BASEDIR/../../output
GRAPHDIR=$OUTPUTDIR/graphs/$1
LOGDIR=$OUTPUTDIR/logs/$1

if [[ $GRAPHDIR != "" ]]; then
    mkdir -p $GRAPHDIR
fi
mkdir -p $LOGDIR

EXTRAARGS=$2
EXTRAARGS=`echo $EXTRAARGS | sed -e "s@-graphs@-graphs $GRAPHDIR@"`

java -cp $BASEDIR/target/*:$BASEDIR/target/lib/* edu.wisc.cs.arc.Driver \
        -configs $CONFIGDIR -descriptions -routersonly \
        -vab -var 1 -t -internalonly -noprune \
        $EXTRAARGS 2>&1 | tee $LOGDIR/blocked-reachable.log

JAVAEXIT=${PIPESTATUS[0]}
if [[ $JAVAEXIT -ne 0 ]]; then
    exit $JAVAEXIT
fi

#Warn assumptions: false
#Routers Only: true
#Minimum policy group size: 0
#Include entire flowspace: false
#Generate per-flow ETGs: true
#Use interface descriptions to generate device-based ETG: true
#Convert process-based ETG to interface-based ETG: false
#Parallelize: true
#Prune: true
#Graphs directory: null
#Serialized ETGs file: null
#Verify currently blocked: false
#Verify always blocked: true
#Verify always reachable: true K=1
#Verify always isolated: false
#Verify paths: false VIRL log=null
