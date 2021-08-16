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

package nsk.jdwp.VirtualMachine.Exit;

import java.io.*;
import java.util.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

public class exit001 {
    static final int JCK_STATUS_BASE = 95;
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final String PACKAGE_NAME = "nsk.jdwp.VirtualMachine.Exit";
    static final String TEST_CLASS_NAME = PACKAGE_NAME + "." + "exit001";
    static final String DEBUGEE_CLASS_NAME = TEST_CLASS_NAME + "a";

    static final String JDWP_COMMAND_NAME = "VirtualMachine.Exit";
    static final int JDWP_COMMAND_ID = JDWP.Command.VirtualMachine.Exit;

    public static void main (String argv[]) {
        System.exit(run(argv,System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
    return new exit001().runIt(argv, out);
    }

    public int runIt(String argv[], PrintStream out) {

        boolean success = true;

        try {
            final ArgumentHandler argumentHandler = new ArgumentHandler(argv);
            final Log log = new Log(out, argumentHandler);

            try {

                Binder binder = new Binder(argumentHandler, log);
                log.display("Start debugee VM");
                final Debugee debugee = binder.bindToDebugee(DEBUGEE_CLASS_NAME);
                Transport transport = debugee.getTransport();
                IOPipe pipe = debugee.createIOPipe();

                log.display("Waiting for VM_INIT event");
                debugee.waitForVMInit();

                log.display("Querying for IDSizes");
                debugee.queryForIDSizes();

                log.display("Resume debugee VM");
                debugee.resume();

                log.display("Waiting for command: " + "ready");
                String cmd = pipe.readln();
                log.display("Received command: " + cmd);


                // Linux returns only 8 least significant bits of exit status - see #4459019 bug.
                int expectedExitCode = 0x0F;

                // begin test of JDWP command

                try {
                    log.display("Creating command packet for " + JDWP_COMMAND_NAME
                                + " with exit code: " + expectedExitCode);
                    CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);
                    command.addInt(expectedExitCode);
                    command.setLength();

                    log.display("Sending command packet:\n" + command);
                    transport.write(command);

                    log.display("Waiting for reply packet");
                    ReplyPacket reply = new ReplyPacket();
                    transport.read(reply);
                    log.display("Reply packet received:\n" + reply);

                    log.display("Checking reply packet header");
                    reply.checkHeader(command.getPacketID());

                    log.display("Parsing reply packet:");
                    reply.resetPosition();

                    if (! reply.isParsed()) {
                        log.complain("Extra bytes in reply packet at: " + reply.currentPosition());
                        success = false;
                    }
                } catch (Exception e) {
                    log.complain("Exception catched: " + e);
                    success = false;
                }

                // check if debugee has been actually terminated

                if (success) {
                    int waittime = argumentHandler.getWaitTime() * 60;  // seconds
                    int pause = 1;                                      // seconds
                    int tries = waittime / pause;
                    success = false;

                    for (int i = 0; i < tries; i++) {
                        if (debugee.terminated()) {
                            log.display("Debugee has been terminated successfully");
                            success = true;
                            break;
                        }
                        try {
                            Thread.currentThread().sleep(pause * 1000);
                        } catch (InterruptedException e) {
                            log.complain("Interrupted exception catched: " + e);
                        }
                    }
                    if (! success) {
                        log.complain("Debugee has not been terminated by request");
                    }
                }

                // check if debugee exit code is as expected

                if (success) {
                    int exitCode = debugee.getStatus();
                    log.display("Debugee exit code is: " + exitCode);
                    if (exitCode != expectedExitCode) {
                        log.complain("Debugee exit code is not equal to expected: " + expectedExitCode);
                        success = false;
                    }
                }

                // end test of JDWP command

            } catch (Exception e) {
                log.complain("Unexpected exception: " + e);
                e.printStackTrace(out);
                success = false;
            }

            if (!success) {
                log.complain("TEST FAILED");
                return FAILED;
            }

        } catch (Exception e) {
            out.println("Unexpected exception: " + e);
            e.printStackTrace(out);
            out.println("TEST FAILED");
            return FAILED;
        }

        out.println("TEST PASSED");
        return PASSED;

    }

}
