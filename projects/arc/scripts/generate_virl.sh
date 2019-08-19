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
VIRLFILE=$OUTPUTDIR/virl/configs/$1.virl

java -cp $BASEDIR/target/*:$BASEDIR/target/lib/* edu.wisc.cs.arc.Driver \
        -configs $CONFIGDIR -virl $VIRLFILE \
        -graphs $GRAPHDIR -baseonly \
        -routersonly -internalonly $DESCRIPS  2>&1 | tee $LOGDIR/virl-gen.log

JAVAEXIT=${PIPESTATUS[0]}
if [[ $JAVAEXIT -ne 0 ]]; then
    exit $JAVAEXIT
fi
