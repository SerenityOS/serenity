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

package nsk.jdwp.VirtualMachine.Version;

import java.io.*;
import java.util.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

public class version002 {
    static final int JCK_STATUS_BASE = 95;
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final String PACKAGE_NAME = "nsk.jdwp.VirtualMachine.Version";
    static final String TEST_CLASS_NAME = PACKAGE_NAME + "." + "version002";
    static final String DEBUGEE_CLASS_NAME = TEST_CLASS_NAME + "a";

    static final String JDWP_COMMAND_NAME = "VirtualMachine.Version";
    static final int JDWP_COMMAND_ID = JDWP.Command.VirtualMachine.Version;

    public static void main (String argv[]) {
        System.exit(run(argv,System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new version002().runIt(argv, out);
    }

    public int runIt(String argv[], PrintStream out) {

        boolean success = true;

        try {
            ArgumentHandler argumentHandler = new ArgumentHandler(argv);
            Log log = new Log(out, argumentHandler);

            try {

                Binder binder = new Binder(argumentHandler, log);
                log.display("Start debugee VM");
                Debugee debugee = binder.bindToDebugee(DEBUGEE_CLASS_NAME);
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

                // begin test of JDWP command

                try {
                    CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);
                    // A properly formed CommandPacket has JDWP.Flag.NONE
                    // for the 'flags' field. Set the 'flags' field to '!'
                    // simulate a bad value in the printable ASCII range.
                    // '!' == 0x21 which means the bad value also does not
                    // have the JDWP.Flag.REPLY_PACKET (0x80) set.
                    command.setFlags((byte) '!');

                    log.display("Sending command packet:\n" + command);
                    transport.write(command);

                    log.display("Waiting for reply packet");
                    ReplyPacket reply = new ReplyPacket();
                    transport.read(reply);

                    if (true) {
                        // In this test (compared to version001), we
                        // should never reach here because the debuggee
                        // should have thrown an IOException and printed
                        // this error message:
                        // ERROR: Received jdwpPacket with flags != 0x0 (actual=0x21) when a jdwpCmdPacket was expected.

                        throw new Failure("Debuggee did not detect bad flags field.");
                    }
                    // The code below this point of the try-catch block is
                    // not reachable and could be deleted. It is left in
                    // place to ease porting of this test to earlier releases.

                    log.display("Reply packet received:\n" + reply);

                    log.display("Checking reply packet header");
                    reply.checkHeader(command.getPacketID());

                    log.display("Parsing reply packet:");
                    reply.resetPosition();

                    String description = reply.getString();
                    log.display("  description: " + description);

                    int jdwpMajor = reply.getInt();
                    log.display("  jdwpMajor:   " + jdwpMajor);

                    int jdwpMinor = reply.getInt();
                    log.display("  jdwpMinor:   " + jdwpMinor);

                    String vmVersion = reply.getString();
                    log.display("  vmVersion:   " + vmVersion);

                    String vmName = reply.getString();
                    log.display("  vmName:      " + vmName);

                    if (! reply.isParsed()) {
                        log.complain("Extra bytes in reply packet at: " + reply.currentPosition());
                        success = false;
                    }

                } catch (IOException ie) {
                    // version001 expects to pass.
                    // This test expects to fail due to an IOException.
                    log.display("Expected IOException caught: " + ie);
                    success = true;
                } catch (Exception e) {
                    log.complain("Exception catched: " + e);
                    success = false;
                }

                // end test of JDWP command

                log.display("Sending command: " + "quit");
                pipe.println("quit");

                log.display("Waiting for debugee exits");
                int code = debugee.waitFor();
                if (code == JCK_STATUS_BASE + PASSED) {
                    log.display("Debugee PASSED: " + code);
                } else {
                    log.complain("Debugee FAILED: " + code);
                    success = false;
                }

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
