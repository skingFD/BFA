#!/bin/bash

SCRIPTPATH=`readlink -f $0`
BASEDIR="`dirname $SCRIPTPATH`/.."
CONFIGDIR="$BASEDIR/../../configs/snapshots"
OUTPUTDIR=$BASEDIR/../../output
LOGDIR=$OUTPUTDIR/logs
MINHOSTS=2

while getopts t OPT; do
    case $OPT in
        t) # Use example configs
            CONFIGDIR="$BASEDIR/../../configs/examples"
            LOGDIR=$OUTPUTDIR/testing
            if [ $MINHOSTS -gt 0 ]; then
                MINHOSTS=65536
            fi
            ;;
        \?)
            echo "Invalid option: -$OPTARG"
            echo "Usage: `basename $0` [-t] NET_NAME [ARGS]"
            exit 1
            ;;
    esac
done
NETNAME=${@:$OPTIND:1}
ARGS=${@:$OPTIND:2}

CONFIGDIR=$CONFIGDIR/$NETNAME
if [ ! -d "$CONFIGDIR" ]; then
    echo "Configs directory \"$CONFIGDIR\" does not exist"
    exit 1
fi

LOGDIR=$LOGDIR/$NETNAME
mkdir -p $LOGDIR

java -cp $BASEDIR/target/*:$BASEDIR/target/lib/* edu.wisc.cs.arc.Driver \
        -configs $CONFIGDIR -descriptions -routersonly -internalonly \
        -minhosts $MINHOSTS \
        2>&1 | tee $LOGDIR/memusage.log
