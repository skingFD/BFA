#!/bin/bash

if [[ $(uname) == "Darwin" ]]; then
    SCRIPTPATH=`greadlink -f $0`
else
    SCRIPTPATH=`readlink -f $0`
fi
BASEDIR="`dirname $SCRIPTPATH`/.."
CONFIGDIR="$BASEDIR/../../configs/examples/$1"
if [ ! -d "$CONFIGDIR" ]; then
    CONFIGDIR=$BASEDIR/../../configs/working/$1
fi
echo $CONFIGDIR
OUTPUTDIR=$BASEDIR/../../output
GRAPHDIR=$OUTPUTDIR/graphs/$1
LOGDIR=$OUTPUTDIR/logs/$1
OBJSDIR=$OUTPUTDIR/objs/

# Gurobi settings
export CODE_DIR="$BASEDIR/../../"
source $CODE_DIR/set_gurobi_envs.sh

if [[ $GRAPHDIR != "" ]]; then
    mkdir -p $GRAPHDIR
fi
mkdir -p $LOGDIR

EXTRAARGS=$2
EXTRAARGS=`echo $EXTRAARGS | sed -e "s@-graphs@-graphs $GRAPHDIR@"`

java -cp $BASEDIR/target/*:$BASEDIR/target/lib/* edu.wisc.cs.arc.Driver \
        -configs $CONFIGDIR -veq $OBJSDIR/${1}.obj \
        $EXTRAARGS 2>&1 | tee $LOGDIR/veq.log

JAVAEXIT=${PIPESTATUS[0]}
if [[ $JAVAEXIT -ne 0 ]]; then
    exit $JAVAEXIT
fi
