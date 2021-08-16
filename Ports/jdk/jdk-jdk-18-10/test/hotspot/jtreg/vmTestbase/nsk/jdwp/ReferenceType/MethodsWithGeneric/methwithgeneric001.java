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

package nsk.jdwp.ReferenceType.MethodsWithGeneric;

import java.io.*;
import java.util.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

/**
 * This test checks that the JDWP command <code>MethodsWithGeneric</code>
 * from the <code>ReferenceType</code> command set returns generic signature
 * information properly.<br>
 * Debuggee part of the test creates instances of several tested classes with
 * methods to be checked. Some of the classes are generic and accordingly
 * contain generic methods. Debugger part obtains information for each method
 * in a reference type of a tested class. Proper generic signature should be
 * returned for the generic methods, or an empty string for non-generic ones.
 */
public class methwithgeneric001 {
    static final String DEBUGGEE_CLASS =
        "nsk.jdwp.ReferenceType.MethodsWithGeneric.methwithgeneric001t";

    static final String JDWP_COMMAND_NAME = "ReferenceType.MethodsWithGeneric";
    static final int JDWP_COMMAND_ID =
        JDWP.Command.ReferenceType.MethodsWithGeneric;

    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT = "quit";

    static final String[][][] methods = {
       {{"<init>",
            "()V",
            "NULL"},
        {"methwithgeneric001bMeth",
            "(Lnsk/jdwp/ReferenceType/MethodsWithGeneric/methwithgeneric001b;)Lnsk/jdwp/ReferenceType/MethodsWithGeneric/methwithgeneric001b;",
            "<L:Ljava/lang/String;>(Lnsk/jdwp/ReferenceType/MethodsWithGeneric/methwithgeneric001b<TL;>;)Lnsk/jdwp/ReferenceType/MethodsWithGeneric/methwithgeneric001b<Ljava/lang/String;>;"},
        {"methwithgeneric001bMethSt",
            "(Lnsk/jdwp/ReferenceType/MethodsWithGeneric/methwithgeneric001b;)Lnsk/jdwp/ReferenceType/MethodsWithGeneric/methwithgeneric001b;",
            "<T:Ljava/lang/String;>(Lnsk/jdwp/ReferenceType/MethodsWithGeneric/methwithgeneric001b<TT;>;)Lnsk/jdwp/ReferenceType/MethodsWithGeneric/methwithgeneric001b<Ljava/lang/String;>;"}},

       {{"<init>",
            "()V",
            "NULL"},
        {"methwithgeneric001cMeth",
            "(Ljava/lang/Class;)Ljava/lang/Object;",
            "<U:Ljava/lang/Object;>(Ljava/lang/Class<TU;>;)TU;"},
         {"methwithgeneric001cMethSt",
            "(Ljava/lang/Class;)Ljava/lang/Object;",
            "<U:Ljava/lang/Object;>(Ljava/lang/Class<TU;>;)TU;"}},

       {{"<init>",
            "()V",
            "NULL"},
        {"methwithgeneric001eMeth",
           "(Lnsk/jdwp/ReferenceType/MethodsWithGeneric/methwithgeneric001e;)V",
           "NULL"},
        {"methwithgeneric001eMethSt",
           "(Lnsk/jdwp/ReferenceType/MethodsWithGeneric/methwithgeneric001e;)V",
           "NULL"}},

       {{"methwithgeneric001ifMeth",
            "()I",
            "NULL"},
        {"methwithgeneric001ifMeth2",
            "(Ljava/lang/Object;)I",
            "<I:Ljava/lang/Object;>(TI;)I"}},

       {{"<init>",
            "()V",
            "NULL"},
        {"methwithgeneric001gMeth",
            "(Ljava/lang/Byte;Ljava/lang/Double;[Ljava/lang/Class;)V",
            "<A:Ljava/lang/Byte;B:Ljava/lang/Double;>(TA;TB;[Ljava/lang/Class<*>;)V"},
        {"methwithgeneric001gMethSt",
            "(Ljava/lang/Byte;Ljava/lang/Double;)V",
            "<A:Ljava/lang/Byte;B:Ljava/lang/Double;>(TA;TB;)V"}}
    };

    static final String[][] classes = {
        {"Lnsk/jdwp/ReferenceType/MethodsWithGeneric/methwithgeneric001b;", "3"},
        {"Lnsk/jdwp/ReferenceType/MethodsWithGeneric/methwithgeneric001c;", "3"},
        {"Lnsk/jdwp/ReferenceType/MethodsWithGeneric/methwithgeneric001e;", "3"},
        {"Lnsk/jdwp/ReferenceType/MethodsWithGeneric/methwithgeneric001if;", "2"},
        {"Lnsk/jdwp/ReferenceType/MethodsWithGeneric/methwithgeneric001g;", "3"}
    };

    static final int CLS_NUM = classes.length;

    public static void main(String argv[]) {
        System.exit(run(argv,System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new methwithgeneric001().runThis(argv, out);
    }

    public int runThis(String argv[], PrintStream out) {
        ArgumentHandler argumentHandler = new ArgumentHandler(argv);
        Log log = new Log(out, argumentHandler);
        boolean result = true;

        try {
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
                for (int i=0; i<CLS_NUM; i++) {
                    long typeID = debuggee.getReferenceTypeID(classes[i][0]);

                    /////// begin test of JDWP command
                    log.display("\n>>>>>> Create command " + JDWP_COMMAND_NAME
                        + "\n\twith ReferenceTypeID: " + typeID
                        + "\n\tof the class: " + classes[i][0]);
                    CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);
                    command.addReferenceTypeID(typeID);
                    command.setLength();

                    log.display("Sending command packet:\n" + command);
                    transport.write(command);

                    log.display("\nWaiting for reply packet ...");
                    ReplyPacket reply = new ReplyPacket();
                    transport.read(reply);
                    log.display(" ... Reply packet received:\n" + reply);

                    log.display("\nChecking reply packet header");
                    reply.checkHeader(command.getPacketID());

                 /* parsing of reply data:
                     int declared - Number of declared methods

                     ---- Repeated 'declared' times:
                     methodID methodID - Method ID
                     string nam - The name of the field
                     string signature - The JNI signature of the field.
                     string genericSignature - The generic signature of the field,
                                                 or an empty string if there is none.
                     int modBits - The modifier bit flags (also known as access flags)
                     ----
                 */
                    log.display("\nParsing reply packet:");
                    reply.resetPosition();

                    long declared = reply.getInt();
                    log.display("\tdeclared: " + declared);
                    int meth_num = Integer.parseInt(classes[i][1]);
                    if (declared != meth_num) {
                        log.complain("TEST FAILED: Unexpected number of declared methods in the reply packet:"
                            + "\n\tGot: " + declared
                            + "\n\tExpected: " + meth_num + "\n");
                        result = false;
                    }

                    for (int j=0; j<declared; j++) {
                        log.display("\t--> method #" + j);

                        long methodID = reply.getMethodID();
                        log.display("\t\tmethodID: " + methodID);

                        String name = reply.getString();
                        log.display("\t\tname: " + name);
                        if (!name.equals(methods[i][j][0])) {
                            log.complain("TEST FAILED: Unexpected name of method #" + i
                                + " in the reply packet:"
                                + "\n\tGot: " + name
                                + "\n\tExpected: " + methods[i][j][0] + "\n");
                            result = false;
                        }

                        String signature = reply.getString();
                        log.display("\t\tsignature: " + signature);
                        if (!signature.equals(methods[i][j][1])) {
                            log.complain("TEST FAILED: Unexpected type signature of field #" + i
                                + " in the reply packet:"
                                + "\n\tGot: " + signature
                                + "\n\tExpected: " + methods[i][j][1] + "\n");
                            result = false;
                        }

                        String genSignature = reply.getString();
                        log.display("\t\tgeneric signature: " + genSignature);
                        if (genSignature.length() == 0) // a non-generic field
                            genSignature = "NULL";
                        if (!genSignature.equals(methods[i][j][2])) {
                            log.complain("TEST FAILED: Unexpected generic signature of field #" + i
                                + " in the reply packet:"
                                + "\n\tGot: " + genSignature
                                + "\n\tExpected: " + methods[i][j][2] + "\n");
                            result = false;
                        }

                        int modBits = reply.getInt();
                        String modBitsString = "0x" + Packet.toHexString(modBits, 8);
                        log.display("\t\tmodBits: " + modBitsString);
                    }

                    if (!reply.isParsed()) {
                        log.complain("TEST FAILED: Extra trailing bytes found in reply packet at: "
                            + reply.currentPosition());
                        result = false;
                    } else
                        log.display("\n<<<<<< Reply packet parsed");
                    /////// end test of JDWP command
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

}
