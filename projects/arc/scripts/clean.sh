#!/bin/bash

NETNUM=0
ls graphs | while read NETWORK; do
	NETNUM=$((NETNUM+1))
	echo $NETWORK $NETNUM
	NETDIR=clean/network$NETNUM
	mkdir $NETDIR
	cp graphs/$NETWORK/topo.gv $NETDIR
	FULLLOG=logs/$NETWORK/full.log
	START=`grep -n "COUNT: separatePolicyGroups" $FULLLOG | cut -f1 -d':'`
	END=`wc -l $FULLLOG | cut -f1 -d' '`
	echo $START $END
	NUMLINE=$((START-END))
	tail -n $NUMLINE $FULLLOG > $NETDIR/isolation.log
done
