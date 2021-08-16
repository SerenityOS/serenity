/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package nsk.jdi.VMStartEvent.thread;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.io.*;
import java.util.Iterator;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


// This class is the debugger application in the test

public class thread001 {
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;

    static final int TIMEOUT_DELTA = 1000; // milliseconds

    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT = "quit";

    static final String DEBUGEE_NAME = "nsk.jdi.VMStartEvent.thread.thread001a";

    static private Debugee debuggee;
    static private VirtualMachine vm;
    static private IOPipe pipe;
    static private Log log;
    static private ArgumentHandler argHandler;
    static private EventSet eventSet;

    static private boolean testFailed;
    static private boolean eventReceived;

    public static void main (String args[]) {
          System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run (final String args[], final PrintStream out) {

         testFailed         = false;
         eventReceived      = false;

         argHandler = new ArgumentHandler(args);
         log = new Log(out, argHandler);

         // launch debuggee
         Binder binder  = new Binder(argHandler, log);
         thread001.log.display("Connecting to debuggee");
         debuggee = binder.bindToDebugeeNoWait(DEBUGEE_NAME);
         debuggee.redirectStderr(thread001.log, "debuggee >");

         pipe = debuggee.createIOPipe();
         vm = debuggee.VM();

         // define separate thread for handling events
         class EventHandler extends Thread {
               public void run() {
                    eventSet = null;
                    try {
                         while (!eventReceived) {
                             eventSet = vm.eventQueue().remove();

                             EventIterator eventIterator = eventSet.eventIterator();
                             while (eventIterator.hasNext()) {
                                  Event event = eventIterator.nextEvent();
                                  log.display("Event received: " + event.toString());

                                  // handle VMStartEvent
                                  if (event instanceof VMStartEvent) {
                                       VMStartEvent castedEvent = (VMStartEvent)event;

                                       EventRequest eventRequest = castedEvent.request();
                                       if (eventRequest != null) {
                                           log.complain("FAILURE 1: eventRequest is not equal to null");
                                           testFailed = true;
                                       }
                                       VirtualMachine eventMachine = castedEvent.virtualMachine();
                                       if (!(vm.equals(eventMachine))) {
                                           log.complain("FAILURE 2: eventVirtualMachine is not equal to checked vm");
                                           testFailed = true;
                                       }

                                       ThreadReference eventThread = castedEvent.thread();
                                       if (eventThread == null) {
                                           log.complain("FAILURE 3: ThreadStartEvent.thread() returns null");
                                           testFailed = true;
                                       }

                                       String threadName = eventThread.name();
                                       if ((threadName == null) || (threadName.equals(""))) {
                                           log.complain("FAILURE 4: thread reference has invalid empty name");
                                           testFailed = true;
                                       } else {
                                           log.display ("VMStartEvent was received in thread " + threadName);
                                       }

                                       // Check that thread is in VirtualMachine.allThreads() list
                                       boolean found = false;
                                       Iterator threadsList = debuggee.VM().allThreads().iterator();
                                       while (!found && threadsList.hasNext()) {
                                            found = eventThread.equals((ThreadReference)threadsList.next());
                                       }
                                       if (!found) {
                                           log.complain("FAILURE 5: " + threadName + " is not in debuggee's allThreads() list");
                                           testFailed = true;
                                       }

                                       eventReceived = true;
                                  }
                             }
                             // resume each handled event set
                             eventSet.resume();
                         }
                    } catch (InterruptedException e) {
                          log.complain("TEST INCOMPLETE: caught InterruptedException while waiting for event");
                          testFailed = true;
                    } catch (VMDisconnectedException e) {
                          log.complain("TEST INCOMPLETE: caught VMDisconnectedException while waiting for event");
                          testFailed = true;
                    }
                    log.display("eventHandler completed");
               }
         }

         // start event handling thread
         EventHandler eventHandler = new EventHandler();
         log.display("Starting eventHandler");
         eventHandler.start();

         // wait for debuggee started
         String command = pipe.readln();
         if (!command.equals(COMMAND_READY)) {
              log.complain("Unknown command from debuggee :" + command);
              testFailed = true;
         }

         // wait for all expected events received
         try {
               eventHandler.join(argHandler.getWaitTime()*60000);
               if (eventHandler.isAlive()) {
                    log.complain("FAILURE 20: Timeout for waiting of event was exceeded");
                    eventHandler.interrupt();
                    testFailed = true;
               }
         } catch (InterruptedException e) {
               log.complain("TEST INCOMPLETE: InterruptedException caught while waiting for eventHandler's death");
               testFailed = true;
         }

         // force debuggee to exit
         thread001.log.display("Sending command: " + COMMAND_QUIT);
         pipe.println(COMMAND_QUIT);

         // wait for debuggee terminated
         thread001.log.display("Waiting for debuggee terminating");
         debuggee.endDebugee();

         // check test results
         if (!eventReceived) {
              thread001.log.complain("FAILURE 6: VMStartEvent is not received");
              testFailed = true;
         }

         if (testFailed) {
              return thread001.FAILED;
         }
         return thread001.PASSED;
    }
}
