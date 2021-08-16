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

package nsk.jdi.VMDeathEvent._itself_;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


// This class is the debugger in the test

public class vmdeath001 {
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;
    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT = "quit";
    static final String TEST_NAME = "nsk.jdi.VMDeathEvent._itself_.vmdeath001";
    static final String DEBUGEE_NAME = TEST_NAME + "a";

    static private Debugee debuggee;
    static private VirtualMachine vm;
    static private IOPipe pipe;
    static private Log log;
    static private ArgumentHandler argHandler;
    static private EventSet eventSet;

    static private boolean testFailed;
    static private boolean eventReceived;

    static private boolean disconnectReceived;
    static private boolean vmdeathIsPrior;

    public static void main (String args[]) {
          System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(final String args[], final PrintStream out) {

         testFailed         = false;
         eventReceived      = false;

         disconnectReceived = false;
         vmdeathIsPrior     = true;

         argHandler = new ArgumentHandler(args);
         log = new Log(out, argHandler);

         Binder binder = new Binder(argHandler, log);
         log.display("Connecting to debuggee");
         debuggee = binder.bindToDebugee(DEBUGEE_NAME);
         debuggee.redirectStderr(log, "debuggee >");

         pipe = debuggee.createIOPipe();
         vm = debuggee.VM();

         log.display("Resuming debuggee");
         debuggee.resume();

         log.display("Waiting for command: " + COMMAND_READY);
         String command = pipe.readln();
         if (!command.equals(COMMAND_READY)) {
             log.complain("TEST BUG: unknown debuggee's command: " + command);
             pipe.println(COMMAND_QUIT);
             debuggee.waitFor();
             return vmdeath001.FAILED;
         }

         class EventHandler extends Thread {
               public void run() {
                    eventSet = null;
                    try {
                         isConnected:
                         while (true) {
                             eventSet = vm.eventQueue().remove();

                             EventIterator eventIterator = eventSet.eventIterator();
                             while (eventIterator.hasNext()) {

                                  Event event = eventIterator.nextEvent();
//                                  log.display("Event received: " + event.toString());

                                  if (event instanceof VMDeathEvent) {
                                       log.display("VMDeathEvent received");
                                       VMDeathEvent castedEvent = (VMDeathEvent)event;
                                       eventReceived = true;

                                       // try to check event references if not disconnected
                                       try {
                                           VirtualMachine eventMachine = castedEvent.virtualMachine();
                                           if (!(vm.equals(eventMachine))) {
                                               log.complain("FAILURE 3: eventVirtualMachine is not equal to checked vm");
                                               testFailed = true;
                                           }

                                           EventRequest request = castedEvent.request();
                                           if( request != null) {
                                               log.complain("FAILURE 4: Non-null reference returned by Event.request(): " + request);
                                               testFailed = false;
                                           }
                                       } catch (VMDisconnectedException e) {
                                           log.display("Unable to check event references because of VM disconnection");
                                       }

                                  } else if (event instanceof VMDisconnectEvent) {
                                       log.display("VMDisconnectEvent received");
                                       VMDisconnectEvent castedEvent = (VMDisconnectEvent)event;

                                       if (!eventReceived) {
                                           vmdeathIsPrior = false;
                                       }
                                       disconnectReceived = true;
                                       break isConnected;
                                  } else if (event instanceof VMDeathEvent) {
                                       eventReceived = true;
                                 }
                             }

                             // try to resume event set if not disconnected
                             try {
                                 eventSet.resume();
                             } catch (VMDisconnectedException e) {
                                 log.display("Unable to resume event set because of VM disconnection");
                             }

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

         EventHandler eventHandler = new EventHandler();
         log.display("Starting eventHandler");
         eventHandler.start();

         log.display("Sending command: " + COMMAND_QUIT);
         pipe.println(COMMAND_QUIT);
         log.display("Waiting for debuggee terminating");
         debuggee.waitFor();

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

         if (!eventReceived) {
              log.complain("FAILURE 1: VMDeathEvent is not received after target VM terminating");
              return vmdeath001.FAILED;
         } else if (!vmdeathIsPrior) {
              log.complain("FAILURE 2: VMDeathEvent is not received before VMDisconnectEvent");
              return vmdeath001.FAILED;
         }
         return vmdeath001.PASSED;
    }
}
