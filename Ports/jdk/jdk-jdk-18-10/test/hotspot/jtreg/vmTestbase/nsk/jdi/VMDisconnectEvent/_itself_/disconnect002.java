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

package nsk.jdi.VMDisconnectEvent._itself_;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


// This class is the debugger application in the test

public class disconnect002 {
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;
    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT  = "quit";
    static final String TEST_NAME = "nsk.jdi.VMDisconnectEvent._itself_.disconnect002";
    static final String DEBUGGEE_NAME = TEST_NAME + "a";

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

    public static int run(final String args[], final PrintStream out) {

         testFailed         = false;
         eventReceived      = false;

         argHandler = new ArgumentHandler(args);
         log = new Log(out, argHandler);

         Debugee debuggee;
         Binder binder         = new Binder(argHandler, log);
         log.display("Connecting to debuggee");
         debuggee = binder.bindToDebugee(DEBUGGEE_NAME);
         debuggee.redirectStderr(log, "debuggee >");

         pipe = debuggee.createIOPipe();
         vm   = debuggee.VM();

         debuggee.resume();

         log.display("Waiting for command: " + COMMAND_READY);
         String command = pipe.readln();
         if (!command.equals(COMMAND_READY)) {
             log.complain("TEST BUG: unknown debuggee's command: " + command);
             pipe.println(COMMAND_QUIT);
             debuggee.waitFor();
             return disconnect002.FAILED;
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

                                  if (event instanceof VMDisconnectEvent) {
                                       log.display("VMDisconnectEvent received");
                                       VMDisconnectEvent castedEvent = (VMDisconnectEvent)event;

                                       // try to check event references if not disconnected
                                       try {

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

                                       } catch (VMDisconnectedException e) {
                                           log.display("Unable to check event references because of VM disconnection");
                                       }

                                       eventReceived = true;
                                       break isConnected;
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

         log.display("Invoking VirtualMachine.dispose() for debuggee.");
         vm.dispose();

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

         log.display("Sending command: " + COMMAND_QUIT);
         pipe.println(COMMAND_QUIT);

         log.display("Waiting for debuggee terminating");
         debuggee.waitFor();

         if (!eventReceived) {
              log.complain("FAILURE 3: VMDisconnectEvent is not received after target VM terminating");
              return disconnect002.FAILED;
         }

         return disconnect002.PASSED;
    }
}
