#!/bin/bash

SCRIPTPATH=`readlink -f $0`
BASEDIR="`dirname $SCRIPTPATH`/.."
CONFIGDIR="$BASEDIR/../../configs/examples/$1"
DESCRIPS=""
if [ ! -d "$CONFIGDIR" ]; then
    CONFIGDIR=$BASEDIR/../../configs/working/$1
    DESCRIPS="-descriptions"
fi
echo $CONFIGDIR
OUTPUTDIR=$BASEDIR/../../output
GRAPHDIR=$OUTPUTDIR/graphs/$1
mkdir -p $GRAPHDIR
LOGDIR=$OUTPUTDIR/logs/$1
mkdir -p $LOGDIR

java -cp $BASEDIR/target/*:$BASEDIR/target/lib/* edu.wisc.cs.arc.Driver \
        -configs $CONFIGDIR -graphs $GRAPHDIR \
        -baseonly -routersonly $DESCRIPS 2>&1 | tee $LOGDIR/basegraphs-gen.log

JAVAEXIT=${PIPESTATUS[0]}
if [[ $JAVAEXIT -ne 0 ]]; then
    exit $JAVAEXIT
fi
