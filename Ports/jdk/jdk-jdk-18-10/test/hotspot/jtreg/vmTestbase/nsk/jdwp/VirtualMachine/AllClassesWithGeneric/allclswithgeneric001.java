/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdwp.VirtualMachine.AllClassesWithGeneric;

import java.io.*;
import java.util.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

/**
 * This test checks that the JDWP command <code>AllClassesWithGeneric</code>
 * from the <code>VirtualMachine</code> command set returns generic signature
 * information properly.<br>
 * Debuggee part of the test creates instances of several tested classes. Some
 * of the classes are generic. Debugger part obtains signature information of
 * all loaded classes by sending the tested JDWP command. Proper generic
 * signature should be returned for each tested class which is generic, or
 * an empty string for non-generic one. All tested classes should be found
 * in reply packet as well.
 */
public class allclswithgeneric001 {
    static final String DEBUGGEE_CLASS =
        "nsk.jdwp.VirtualMachine.AllClassesWithGeneric.allclswithgeneric001t";

    static final String JDWP_COMMAND_NAME = "VirtualMachine.AllClassesWithGeneric";
    static final int JDWP_COMMAND_ID =
        JDWP.Command.VirtualMachine.AllClassesWithGeneric;

    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT = "quit";

    static final String[][] classes = {
        {"Lnsk/jdwp/VirtualMachine/AllClassesWithGeneric/allclswithgeneric001t;",
            "NULL"},

        {"Lnsk/jdwp/VirtualMachine/AllClassesWithGeneric/allclswithgeneric001b;",
            "<L:Ljava/lang/String;>Ljava/lang/Object;"},

        {"Lnsk/jdwp/VirtualMachine/AllClassesWithGeneric/allclswithgeneric001c;",
            "<A:Ljava/lang/Object;B:Ljava/lang/Integer;>Ljava/lang/Object;"},

        {"Lnsk/jdwp/VirtualMachine/AllClassesWithGeneric/allclswithgeneric001d;",
            "<T:Ljava/lang/Object;>Ljava/lang/Object;Lnsk/jdwp/VirtualMachine/AllClassesWithGeneric/allclswithgeneric001if<TT;>;"},

        {"Lnsk/jdwp/VirtualMachine/AllClassesWithGeneric/allclswithgeneric001e;",
            "NULL"},

        {"Lnsk/jdwp/VirtualMachine/AllClassesWithGeneric/allclswithgeneric001if;",
            "<I:Ljava/lang/Object;>Ljava/lang/Object;"},

        {"Lnsk/jdwp/VirtualMachine/AllClassesWithGeneric/allclswithgeneric001f;",
            "NULL"},

        {"Lnsk/jdwp/VirtualMachine/AllClassesWithGeneric/allclswithgeneric001g;",
            "<E:Lnsk/jdwp/VirtualMachine/AllClassesWithGeneric/allclswithgeneric001e;:Lnsk/jdwp/VirtualMachine/AllClassesWithGeneric/allclswithgeneric001if;>Ljava/lang/Object;"},

        {"Lnsk/jdwp/VirtualMachine/AllClassesWithGeneric/allclswithgeneric001h;",
            "<A1:Ljava/lang/Object;B1:Ljava/lang/Object;C1:Ljava/lang/Object;>Lnsk/jdwp/VirtualMachine/AllClassesWithGeneric/allclswithgeneric001d<TA1;>;Lnsk/jdwp/VirtualMachine/AllClassesWithGeneric/allclswithgeneric001if2<TA1;TB1;TC1;>;"}
    };

    static final int CLS_NUM = classes.length;

    private int foundClasses[] = new int[CLS_NUM];

    private Log log;

    public static void main(String argv[]) {
        System.exit(run(argv,System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new allclswithgeneric001().runThis(argv, out);
    }

    public int runThis(String argv[], PrintStream out) {
        ArgumentHandler argumentHandler = new ArgumentHandler(argv);
        log = new Log(out, argumentHandler);
        boolean result = true;

        try {
            for(int i=0; i<CLS_NUM; i++)
                foundClasses[i] = 0;

            Binder binder = new Binder(argumentHandler, log);

            log.display("Starting debuggee VM ...");
            Debugee debuggee = binder.bindToDebugee(DEBUGGEE_CLASS);

            Transport transport = debuggee.getTransport();
            IOPipe pipe = debuggee.createIOPipe();

            log.display("Waiting for VM_INIT event ...");
            debuggee.waitForVMInit();

            log.display("Querying for IDSizes ...");
            debuggee.queryForIDSizes();

            log.display("Resuming debuggee VM ...");
            debuggee.resume();

            log.display("Waiting for command: " + COMMAND_READY
                + " ...");
            String cmd = pipe.readln();
            log.display(" ... Received command: " + cmd);

            try {
                /////// begin test of JDWP command
                log.display("\n>>>>>> Create command " + JDWP_COMMAND_NAME);
                CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);

                log.display("Sending command packet:\n" + command);
                transport.write(command);

                log.display("\nWaiting for reply packet ...");
                ReplyPacket reply = new ReplyPacket();
                transport.read(reply);
                log.display(" ... Reply packet received:\n" + reply);

                log.display("\nChecking reply packet header");
                reply.checkHeader(command.getPacketID());

             /* parsing of reply data:
                int classes - Number of reference types that follow

                ---- Repeated 'classes' times:
                byte refTypeTag - Kind of following reference type.
                referenceTypeID typeID - Loaded reference type.
                string signature - The JNI signature for the loaded reference type.
                string genericSignature - The generic signature for the loaded reference type
                                          or an empty string if there is none.
                int status - The current class status.
                ----
             */
                log.display("\nParsing reply packet:");
                reply.resetPosition();

                int cls_num = reply.getInt();
                log.display("\tclasses: " + cls_num);

                for (int i=0; i<cls_num; i++) {
                    byte refTypeTag = reply.getByte();
                    long typeID = reply.getReferenceTypeID();
                    String signature = reply.getString();
                    String genSignature = reply.getString();
                    int status = reply.getInt();

                    int idx = findClass(signature);
                    if (idx != -1) {
                        foundClasses[idx]++;

                        log.display("\n--> found tested class "
                            + classes[idx][0] + " :"
                            + "\n\t\trefTypeTag: " + refTypeTag
                            + "\n\t\ttypeID: " + typeID
                            + "\n\t\tsignature: " + signature);

                        if (genSignature.length() == 0) // a non-generic class
                            genSignature = "NULL";
                            if (!genSignature.equals(classes[idx][1])) {
                            log.complain("TEST FAILED: Unexpected generic signature of tested class #"
                                + (i+1) + " (" + classes[idx][0] + ")"
                                + " in the reply packet:"
                                + "\n\tGot: " + genSignature
                                + "\n\tExpected: " + classes[idx][1] + "\n");
                            result = false;
                        }
                        else
                            log.display("\t\tgeneric signature: " + genSignature);

                        log.display("\t\tstatus: " + status);
                    }
                }

                if (!reply.isParsed()) {
                    log.complain("TEST FAILED: Extra trailing bytes found in reply packet at: "
                        + reply.currentPosition());
                    result = false;
                } else
                    log.display("\n<<<<<< Reply packet parsed");
                /////// end test of JDWP command

                for (int i=0; i<CLS_NUM; i++)
                    if (foundClasses[i] != 1) {
                        result = false;
                        log.complain("TEST FAILED: Unexpected number of reference types for the tested class\n\t"
                            + classes[i][0] + "\n\tin reply packet on "
                            + JDWP_COMMAND_NAME
                            + ":\n\tGot: " + foundClasses[i]
                            + "\n\tExpected: 1");
                    }

            } catch (Exception e) {
                e.printStackTrace(out);
                log.complain("Caught exception while testing JDWP command: "
                    + e);
                result = false;
            } finally {
                log.display("Sending command: " + COMMAND_QUIT + " ...");
                pipe.println(COMMAND_QUIT);

                log.display("Waiting for debuggee exits ...");
                int code = debuggee.waitFor();
                if (code == Consts.JCK_STATUS_BASE + Consts.TEST_PASSED) {
                    log.display(" ... Debuggee PASSED with the exit code: "
                        + code);
                } else {
                    log.complain(" ... Debuggee FAILED with the exit code: "
                        + code);
                    result = false;
                }
            }

        } catch (Exception e) {
            e.printStackTrace(out);
            log.complain("Caught unexpected exception while communicating with debugee: "
                + e);
            result = false;
        }

        if (!result)
            return Consts.TEST_FAILED;

        return Consts.TEST_PASSED;
    }

    private int findClass(String cls) {
        for (int i=0; i<CLS_NUM; i++)
            if (cls.equals(classes[i][0]))
                return i; // the tested class found

        return -1; // the tested class not found
    }
}
