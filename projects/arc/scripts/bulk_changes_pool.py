#!/usr/bin/python

import os
from pool import Pool
import subprocess
import sys

scriptpath = subprocess.check_output(["readlink", "-f", sys.argv[0]])
basedir = os.path.dirname(scriptpath)+"/.."
snapshotsdir = basedir+"/../../configs/snapshots"
logsdir = basedir+"/../../output/logs"

if (len(sys.argv) < 2):
    print "Usage: %s <script> [<args>]" % (sys.argv[0])
    sys.exit(1)

script = basedir+"/scripts/"+sys.argv[1]
args = sys.argv[2:]

#pool = Pool(15, 30, 60*60*3)
pool = Pool(2, 15, 60*10)

# Process each network
for network in sorted(os.listdir(snapshotsdir)):
#
    # Process each snapshot
    snapshots = sorted(os.listdir(snapshotsdir+"/"+network))
    for timestamp in snapshots:
        snapshot = network+"/"+timestamp
#        if (os.path.isfile(logsdir+"/"+snapshot+"/memusage.log")):
#            break # Only do first snapshot per network
        run = [script, snapshot]
        run.extend(args)
        pool.submit(run, name=snapshot)
        break # Only do first snapshot per network

# Wait for everything to finish
pool.join()
