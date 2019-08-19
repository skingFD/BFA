#!/usr/bin/python

import os
import subprocess
import sys
import time

class MyPopen(subprocess.Popen):
    def setName(self, name):
        self.name = name
        self.runtime = 0
    
    def getName(self):
        return self.name

    def addRunTime(self, time):
        self.runtime += time

    def getRunTime(self):
        return self.runtime

class Pool:
    def __init__(self, capacity, poll=1, timeout=-1):
        self.running = []
        self.capacity = capacity
        self.poll = poll
        self.timeout = timeout
        self.devnull = open(os.devnull, 'w')

    def submit(self, cmd, name=None):
        self.wait()
        proc = MyPopen(cmd, stdout=self.devnull, stderr=self.devnull)
        self.running.append(proc)
        if (name is None):
            name = cmd[0]
        proc.setName(name)
        print "Start: %s" % (name)
        sys.stdout.flush()

    def wait(self):
        self.waitUntil(self.capacity-1)

    def join(self):
        self.waitUntil(0)

    def waitUntil(self, remain):
        while (len(self.running) > remain):
            sys.stdout.flush()
            time.sleep(self.poll)
            toremove = []
            for proc in self.running:
                retcode = proc.poll()
                if (retcode is not None):
                    print "Finish: %s %d" % (proc.getName(), retcode)
                    toremove.append(proc)
                else:
                    proc.addRunTime(self.poll)
                    if (self.timeout > 0 and proc.getRunTime() >= self.timeout):
                        proc.kill()
                        print "Kill: %s" % (proc.getName())
                        toremove.append(proc)

            for proc in toremove:
                self.running.remove(proc)
            print "%d of %d active" % (len(self.running), self.capacity)

#pool = Pool(5)    
#remain = 10
#while remain > 0:
#    remain -= 1;
#    pool.submit(["sleep","5"])
#pool.join()
