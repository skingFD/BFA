#!/bin/bash

SCRIPTPATH=`readlink -f $0`
BASEDIR="`dirname $SCRIPTPATH`/.."

BASEONLY=0
while getopts :b OPT; do
    case $OPT in
        b)
            echo "Only rendering base graphs"
            BASEONLY=1
            ;;
        \?)
            echo "Invalid option: -$OPTARG"
            echo "Usage: `basename $0` [-b] graphdir"
            exit 1
            ;;
    esac
done
GRAPHDIR=${@:$OPTIND:1}
if [ ! -d "$GRAPHDIR" ]; then
    echo "Graph directory \"$GRAPHDIR\" does not exist"
    exit 1
fi

echo "Rendering graphs in ${GRAPHDIR}..."

GVFILE=$GRAPHDIR/base.gv
if [ -f $GVFILE ]; then
    PNGFILE=`echo $GVFILE | sed -e 's/\.gv$/\.png/'`
    dot -Tpng -o $PNGFILE $GVFILE
fi

GVFILE=$GRAPHDIR/topo.gv
if [ -f $GVFILE ]; then
    PNGFILE=`echo $GVFILE | sed -e 's/\.gv$/\.png/'`
    dot -Tpng -o $PNGFILE $GVFILE
fi

GVFILE=$GRAPHDIR/instance.gv
if [ -f $GVFILE ]; then
    PNGFILE=`echo $GVFILE | sed -e 's/\.gv$/\.png/'`
    dot -Tpng -o $PNGFILE $GVFILE
fi

if [[ $BASEONLY -eq 1 ]]; then
    exit 0
fi

ls $GRAPHDIR/*.gv | while read GVFILE; do
    PNGFILE=`echo $GVFILE | sed -e 's/\.gv$/\.png/'`
    dot -Tpng -o $PNGFILE $GVFILE
    echo "Rendered $GVFILE to $PNGFILE"
done
