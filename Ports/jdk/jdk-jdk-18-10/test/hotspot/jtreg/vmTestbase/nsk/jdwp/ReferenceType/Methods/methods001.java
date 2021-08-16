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

package nsk.jdwp.ReferenceType.Methods;

import java.io.*;
import java.util.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

public class methods001 {
    static final int JCK_STATUS_BASE = 95;
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final String PACKAGE_NAME = "nsk.jdwp.ReferenceType.Methods";
    static final String TEST_CLASS_NAME = PACKAGE_NAME + "." + "methods001";
    static final String DEBUGEE_CLASS_NAME = TEST_CLASS_NAME + "a";

    static final String JDWP_COMMAND_NAME = "ReferenceType.Methods";
    static final int JDWP_COMMAND_ID = JDWP.Command.ReferenceType.Methods;

    static final String TESTED_CLASS_NAME = DEBUGEE_CLASS_NAME + "$" + "TestedClass";
    static final String TESTED_CLASS_SIGNATURE = "L" + TESTED_CLASS_NAME.replace('.', '/') + ";";

    static final String[][] methods = {
                    {"<init>", "()V"},
                    {"byteMethod", "(B)B"},
                    {"booleanMethod", "()Z"},
                    {"charMethod", "(B)C"},
                    {"shortMethod", "(SS)S"},
                    {"intMethod", "(IS)I"},
                    {"longMethod", "(I)J"},
                    {"floatMethod", "(D)F"},
                    {"doubleMethod", "()D"},
                    {"stringMethod", "(Ljava/lang/String;C)Ljava/lang/String;"},
                    {"objectMethod",  "()" + TESTED_CLASS_SIGNATURE},
                    {"intArrayMethod", "(I)[I"},
                    {"<init>", "(Z)V"}
                 };
    static final int DECLARED_METHODS = methods.length;
    static final int METHOD_MODIFIER_FLAGS = JDWP.ModifierFlag.PUBLIC;

    public static void main (String argv[]) {
        System.exit(run(argv,System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
    return new methods001().runIt(argv, out);
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

                try {

                    long typeID = debugee.getReferenceTypeID(TESTED_CLASS_SIGNATURE);

                    // begin test of JDWP command

                    log.display("Create command " + JDWP_COMMAND_NAME
                                + " with ReferenceTypeID: " + typeID);
                    CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);
                    command.addReferenceTypeID(typeID);
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

                    long declared = reply.getInt();
                    log.display("  declared: " + declared);

                    if (declared != DECLARED_METHODS) {
                        log.complain("Unexpected number of declared methods in the reply packet" + declared
                                    + " (expected: " + DECLARED_METHODS + ")");
                        success = false;
                    }

                    for (int i = 0; i < declared; i++ ) {

                        log.display("  method #" + i);

                        long methodID = reply.getMethodID();
                        log.display("    methodID: " + methodID);

                        String name = reply.getString();
                        log.display("    name: " + name);
                        if (! name.equals(methods[i][0])) {
                            log.complain("Unexpected name of method #" + i + " in the reply packet: " + name
                                        + " (expected: " + methods[i][0] + ")");
                            success = false;
                        }

                        String signature = reply.getString();
                        log.display("    signature: " + signature);
                        if (! signature.equals(methods[i][1])) {
                            log.complain("Unexpected signature of method #" + i + " in the reply packet: " + signature
                                        + " (expected: " + methods[i][1] + ")");
                            success = false;
                        }

                        int modBits = reply.getInt();
                        String modBitsString = "0x" + Packet.toHexString(modBits, 8);
                        log.display("    modBits: " + modBitsString);
                        modBits &= JDWP.ModifierFlag.METHOD_MASK;
                        if (modBits != METHOD_MODIFIER_FLAGS) {
                            String expectedModBitsString = "0x" + Packet.toHexString(METHOD_MODIFIER_FLAGS, 8);
                            log.complain("Unexpected modifier flags of method #" + i + " in the reply packet: " + modBitsString
                                        + " (expected: " + expectedModBitsString + ")");
                            success = false;
                        }

                    }

                    if (! reply.isParsed()) {
                        log.complain("Extra trailing bytes found in reply packet at: " + reply.currentPosition());
                        success = false;
                    } else {
                        log.display("Reply packet parsed successfully");
                    }

                    // end test of JDWP command

                } catch (Exception e) {
                    log.complain("Caught exception while testing JDWP command: " + e);
                    success = false;
                } finally {
                    log.display("Sending command: " + "quit");
                    pipe.println("quit");

                    log.display("Waiting for debugee exits");
                    int code = debugee.waitFor();
                    if (code == JCK_STATUS_BASE + PASSED) {
                        log.display("Debugee PASSED with exit code: " + code);
                    } else {
                        log.complain("Debugee FAILED with exit code: " + code);
                        success = false;
                    }
                }

            } catch (Exception e) {
                log.complain("Caught unexpected exception while communicating with debugee: " + e);
                e.printStackTrace(out);
                success = false;
            }

            if (!success) {
                log.complain("TEST FAILED");
                return FAILED;
            }

        } catch (Exception e) {
            out.println("Caught unexpected exception while starting the test: " + e);
            e.printStackTrace(out);
            out.println("TEST FAILED");
            return FAILED;
        }

        out.println("TEST PASSED");
        return PASSED;

    }

}
