#!/bin/bash

if [[ $(uname) == "Darwin" ]]; then
    SCRIPTPATH=`greadlink -f $0`
else
    SCRIPTPATH=`readlink -f $0`
fi
BASEDIR="`dirname $SCRIPTPATH`/.."
CONFIGDIR="$BASEDIR/../../configs/snapshots"
OUTPUTDIR=$BASEDIR/../../output
LOGDIR=$OUTPUTDIR/logs
GRAPHS=""

while getopts tg OPT; do
    case $OPT in
        t) # Use example configs
            CONFIGDIR="$BASEDIR/../../configs/examples"
            LOGDIR=$OUTPUTDIR/testing
            ;;
        g) # Generate graphs
            GRAPHS="-graphs"
            ;;
        \?)
            echo "Invalid option: -$OPTARG"
            echo "Usage: `basename $0` [-t] [-g] NET_NAME [ARGS]"
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

if [ ! -z "$GRAPHS" ]; then
    mkdir -p $LOGDIR/graphs
    GRAPHS="$GRAPHS $LOGDIR/graphs"
fi

java -cp $BASEDIR/target/*:$BASEDIR/target/lib/* edu.wisc.cs.arc.Driver \
        -configs $CONFIGDIR $GRAPHS $ARGS 2>&1 | tee $LOGDIR/run.log

JAVAEXIT=${PIPESTATUS[0]}
if [[ $JAVAEXIT -ne 0 ]]; then
    exit $JAVAEXIT
fi

if [ ! -z "$GRAPHS" ]; then
    $BASEDIR/scripts/render_graphs.sh $LOGDIR/graphs
fi
