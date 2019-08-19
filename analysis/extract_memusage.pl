#!/usr/bin/perl

# PURPOSE: Extract memory usage from a log file.
# AUTHOR: Aaron Gember-Jacobson (agemberjacobson@cs.colgate.edu)

use strict;
use Cwd 'abs_path';
use File::Basename;

# Get directories and files
my $scriptpath = abs_path($0);
my $basedir = dirname($scriptpath);

my $outputdir = "$basedir/../output";
my $logsdir = "$outputdir/logs";

# Get arguments
my $logname="memusage.log";

# Get list of networks
opendir(D, $logsdir) or die("Could not open $logsdir");
my @networks = sort(readdir(D));
shift @networks;
shift @networks;

print "Snapshot,Etgs,Mem\n";

# Process all networks
foreach my $network (@networks) {

    # Get list of snapshots
    my $networkdir = "$logsdir/$network";
    opendir(D, $networkdir) or die("Could not open $networkdir");
    my @snapshots = sort(readdir(D));
    shift @snapshots;
    shift @snapshots;

    # Process all snapshots
	foreach my $currsnap (@snapshots) {
        # Make sure log file exists
        my $logfile = "$networkdir/$currsnap/$logname";
        if (not -e $logfile) {
            next;
        }

        # Get number of ETGs
        my $etgs = `grep -E "Need to generate" $logfile`;
        $etgs =~ s/[^0-9]//g;

        # Get number of policy groups
        my $mem = `grep -E "MEM: flowETGs" $logfile`;
        $mem =~ s/[^0-9]//g;

        print "$network/$currsnap,$etgs,$mem\n";
    }

}
