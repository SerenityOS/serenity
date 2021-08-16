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

package nsk.jdwp.ReferenceType.FieldsWithGeneric;

import java.io.*;
import java.util.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

/**
 * This test checks that the JDWP command <code>FieldsWithGeneric</code>
 * from the <code>ReferenceType</code> command set returns generic signature
 * information properly.<br>
 * Debuggee part of the test contains several tested fields. Some of them
 * are generic including the inherited fields. Debugger part obtains
 * information for each field in a reference type of a tested class.
 * Proper generic signature should be returned for the generic fields, or
 * an empty string for non-generic ones. Information for the inherited
 * fields should not be returned.
 */
public class fldwithgeneric001 {
    static final String DEBUGGEE_CLASS =
        "nsk.jdwp.ReferenceType.FieldsWithGeneric.fldwithgeneric001t";
    static final String TESTED_CLASS_SIGNATURE =
        "Lnsk/jdwp/ReferenceType/FieldsWithGeneric/fldwithgeneric001a;";

    static final String JDWP_COMMAND_NAME = "ReferenceType.FieldsWithGeneric";
    static final int JDWP_COMMAND_ID =
        JDWP.Command.ReferenceType.FieldsWithGeneric;

    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT = "quit";

    static final String[][] fields = {
        {"_fldwithgeneric001St",
            "Lnsk/jdwp/ReferenceType/FieldsWithGeneric/fldwithgeneric001;",
            "NULL"},

        {"_fldwithgeneric001b",
            "Lnsk/jdwp/ReferenceType/FieldsWithGeneric/fldwithgeneric001b;",
            "Lnsk/jdwp/ReferenceType/FieldsWithGeneric/fldwithgeneric001b<Ljava/lang/String;>;"},
        {"_fldwithgeneric001bSt",
            "Lnsk/jdwp/ReferenceType/FieldsWithGeneric/fldwithgeneric001b;",
            "Lnsk/jdwp/ReferenceType/FieldsWithGeneric/fldwithgeneric001b<Ljava/lang/String;>;"},

        {"_fldwithgeneric001c",
            "Lnsk/jdwp/ReferenceType/FieldsWithGeneric/fldwithgeneric001c;",
            "Lnsk/jdwp/ReferenceType/FieldsWithGeneric/fldwithgeneric001c<Ljava/lang/Boolean;Ljava/lang/Integer;>;"},
        {"_fldwithgeneric001cSt",
            "Lnsk/jdwp/ReferenceType/FieldsWithGeneric/fldwithgeneric001c;",
            "Lnsk/jdwp/ReferenceType/FieldsWithGeneric/fldwithgeneric001c<Ljava/lang/Boolean;Ljava/lang/Integer;>;"},

        {"_fldwithgeneric001e",
            "Lnsk/jdwp/ReferenceType/FieldsWithGeneric/fldwithgeneric001e;",
            "NULL"},
        {"_fldwithgeneric001eSt",
            "Lnsk/jdwp/ReferenceType/FieldsWithGeneric/fldwithgeneric001e;",
            "NULL"},

        {"_fldwithgeneric001if",
            "Lnsk/jdwp/ReferenceType/FieldsWithGeneric/fldwithgeneric001if;",
            "Lnsk/jdwp/ReferenceType/FieldsWithGeneric/fldwithgeneric001if<Ljava/lang/Object;>;"},
        {"_fldwithgeneric001ifSt",
            "Lnsk/jdwp/ReferenceType/FieldsWithGeneric/fldwithgeneric001if;",
            "Lnsk/jdwp/ReferenceType/FieldsWithGeneric/fldwithgeneric001if<Ljava/lang/Object;>;"},

        {"_fldwithgeneric001g",
            "Lnsk/jdwp/ReferenceType/FieldsWithGeneric/fldwithgeneric001g;",
            "Lnsk/jdwp/ReferenceType/FieldsWithGeneric/fldwithgeneric001g<Lnsk/jdwp/ReferenceType/FieldsWithGeneric/fldwithgeneric001f;>;"},
        {"_fldwithgeneric001gSt",
            "Lnsk/jdwp/ReferenceType/FieldsWithGeneric/fldwithgeneric001g;",
            "Lnsk/jdwp/ReferenceType/FieldsWithGeneric/fldwithgeneric001g<Lnsk/jdwp/ReferenceType/FieldsWithGeneric/fldwithgeneric001f;>;"},

        {"_fldwithgeneric001gArr",
            "[Lnsk/jdwp/ReferenceType/FieldsWithGeneric/fldwithgeneric001g;",
            "NULL"}
    };

    static final int FLDS_NUM = fields.length;

    public static void main(String argv[]) {
        System.exit(run(argv,System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new fldwithgeneric001().runThis(argv, out);
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
                long typeID = debuggee.getReferenceTypeID(TESTED_CLASS_SIGNATURE);

                /////// begin test of JDWP command
                log.display("\nCreate command " + JDWP_COMMAND_NAME
                            + " with ReferenceTypeID: " + typeID);
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
    int declared - Number of declared fields

    ---- Repeated 'declared' times:
    fieldID fieldID - Field ID
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

                if (declared != FLDS_NUM) {
                    log.complain("TEST FAILED: Unexpected number of declared fields in the reply packet:"
                        + "\n\tGot: " + declared
                        + "\n\tExpected: " + FLDS_NUM + "\n");
                    result = false;
                }

                for (int i=0; i<declared; i++) {
                    log.display("\t>>> field #" + i);

                    long fieldID = reply.getFieldID();
                    log.display("\t\tfieldID: " + fieldID);

                    String name = reply.getString();
                    log.display("\t\tname: " + name);
                    if (!name.equals(fields[i][0])) {
                        log.complain("TEST FAILED: Unexpected name of field #" + i
                            + " in the reply packet:"
                            + "\n\tGot: " + name
                            + "\n\tExpected: " + fields[i][0] + "\n");
                        result = false;
                    }

                    String signature = reply.getString();
                    log.display("\t\tsignature: " + signature);
                    if (!signature.equals(fields[i][1])) {
                        log.complain("TEST FAILED: Unexpected type signature of field #" + i
                            + " in the reply packet:"
                            + "\n\tGot: " + signature
                            + "\n\tExpected: " + fields[i][1] + "\n");
                        result = false;
                    }

                    String genSignature = reply.getString();
                    log.display("\t\tgeneric signature: " + genSignature);
                    if (genSignature.length() == 0) // a non-generic field
                        genSignature = "NULL";
                    if (!genSignature.equals(fields[i][2])) {
                        log.complain("TEST FAILED: Unexpected generic signature of field #" + i
                            + " in the reply packet:"
                            + "\n\tGot: " + genSignature
                            + "\n\tExpected: " + fields[i][2] + "\n");
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
                    log.display("\nReply packet parsed");
                /////// end test of JDWP command

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
