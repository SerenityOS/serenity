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

package nsk.jdwp.ReferenceType.SourceDebugExtension;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

/**
 * The test checks that the <code>SourceDebugExtension class file
 * attribute</code> can be obtained at the JDWP level of JDPA via
 * the <code>SourceDebugExtension</code> command in the
 * <code>ReferenceType</code> command set. The command is sent
 * by a debugger. Received reply data should contain the debug
 * extension string or the JDWP error <code>ABSENT_INFORMATION</code>.
 */
public class srcdebugext001 {
    public static final int JCK_STATUS_BASE = 95;
    public static final int PASSED = 0;
    public static final int FAILED = 2;
    static final String DEBUGGEE_CLASS =
        "nsk.jdwp.ReferenceType.SourceDebugExtension.srcdebugext001t";
    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT = "quit";

    static final String JDWP_COMMAND_NAME = "ReferenceType.SourceDebugExtension";
    static final int JDWP_COMMAND_ID =
        JDWP.Command.ReferenceType.SourceDebugExtension;

    private Log log;
    private IOPipe pipe;
    private Debugee debuggee;
    private Transport transport;

    public static void main (String argv[]) {
        System.exit(run(argv,System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new srcdebugext001().runIt(argv, out);
    }

    private int runIt(String args[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        CommandPacket cmdPack = new CommandPacket(JDWP_COMMAND_ID);
        ReplyPacket replyPack = new ReplyPacket();

        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);
        try {
            debuggee = binder.bindToDebugee(DEBUGGEE_CLASS);
            debuggee.waitForVMInit();
            pipe = debuggee.createIOPipe();
            debuggee.resume();
        } catch(Exception e) {
            log.complain("FAILURE: caught: " + e);
            e.printStackTrace();
            return FAILED;
        }
        String cmd = pipe.readln();
        if (!cmd.equals(COMMAND_READY)) {
            log.complain("TEST BUG: unknown debuggee's command: "
                + cmd);
            return quitDebuggee(FAILED);
        }

// send a command packet
        transport = debuggee.getTransport();
        long rTypeID = getRefTypeID(DEBUGGEE_CLASS);
        cmdPack.addReferenceTypeID(rTypeID);
        cmdPack.setLength();
        try {
            log.display("Sending a command: " + JDWP_COMMAND_NAME);
            transport.write(cmdPack);
            log.display("Waiting for reply");
            transport.read(replyPack);
                log.display("Reply received:\n" + replyPack);

// check received reply packet
            if (checkReplyPacket(replyPack,cmdPack) != PASSED)
                return quitDebuggee(FAILED);
            switch(replyPack.getErrorCode()) {
                case JDWP.Error.NONE:
                    replyPack.resetPosition();
                    log.display("TEST PASSED: received reply does not contain errors.\n\t"
                        + "The debug extension string is: "
                        + replyPack.getString());
                    break;
                case JDWP.Error.ABSENT_INFORMATION:
                    log.display("TEST PASSED: received reply contains a valid error: ABSENT_INFORMATION");
                    break;
                default:
                    log.complain("Unexpected error code "
                        + replyPack.getErrorCode() + " in reply:\n"
                        + replyPack);
                    return quitDebuggee(FAILED);
            }
        } catch(Exception e) {
            log.complain("FAILURE: caught: " + e);
            e.printStackTrace();
            return quitDebuggee(FAILED);
        }
        return quitDebuggee(PASSED);
    }

    private long getRefTypeID(String cls) {
        ReplyPacket reply = new ReplyPacket();
        long typeID = 0;
        String clsSig = "L" + cls.replace('.', '/') + ";";

        log.display("\ngetRefTypeID: getting a RefetenceType ID for the signature:\n\t"
            + clsSig);
        CommandPacket cmd =
            new CommandPacket(JDWP.Command.VirtualMachine.ClassesBySignature);
        cmd.addString(clsSig);
        cmd.setLength();
        try {
            log.display("\ngetRefTypeID: sending a command VirtualMachine.ClassesBySignature:\n"
                + cmd);
            transport.write(cmd);
            log.display("getRefTypeID: Waiting for reply");
            transport.read(reply);
                log.display("getRefTypeID: Reply received:\n" + reply);
            if (checkReplyPacket(reply,cmd) != PASSED)
                throw new Failure("TEST FAILED");
//            reply = debuggee.receiveReplyFor(cmd);
//            log.display("getRefTypeID: reply received:\n" + reply);
            log.display("getRefTypeID: extracting ReferenceTypeID from the reply packet");
            reply.resetPosition();

/* parsing of reply data:
       int classes - number of reference types that follow
       ---- Repeated 'classes' times:
       byte refTypeTag - kind of following reference type
       referenceTypeID - typeID matching loaded reference type
       int status - the current class status
       ---- */
            int cls_num = reply.getInt();
            if (cls_num != 1) {
                throw new Failure("TEST FAILED: Illegal number of returned classes: "
                    + cls_num + ", expected: 1");
            }
            else
                log.display("getRefTypeID: reply data:\n\tnumber of returned classes: "
                    + cls_num);
            byte refTypeTag = reply.getByte();
            typeID = reply.getReferenceTypeID();
            log.display("\treferenceTypeID: " + typeID);
            int status = reply.getInt();
            log.display("\tstatus: " + status + "\n");
        } catch(Exception e) {
            quitDebuggee(FAILED);
            if (e instanceof Failure)
                throw new Failure(e);
            else {
                e.printStackTrace();
                throw new Failure("TEST FAILED: " + e.toString());
            }
        }

        if (!reply.isParsed()) {
            quitDebuggee(FAILED);
            throw new Failure("TEST FAILED: Extra bytes in reply packet at: "
                + reply.currentPosition());
        }

        return typeID;
    }

    private int checkReplyPacket(ReplyPacket reply, CommandPacket cmd) {
        int ret = PASSED;

        if (reply.getFlags() != JDWP.Flag.REPLY_PACKET) {
            log.complain("TEST FAILED: Unexpected flags in reply packet:\n\tgot=0x"
                + Packet.toHexString(reply.getFlags(), 2)
                + " should be: 0x"
                + Integer.toHexString(JDWP.Flag.REPLY_PACKET));
            ret = FAILED;
        }
        if (reply.getPacketID() != cmd.getPacketID()) {
            log.complain("TEST FAILED: Unexpected id field in reply packet:\n\tgot=0x"
                + Packet.toHexString(reply.getPacketID(), 8)
                + " should be: 0x" + Packet.toHexString(cmd.getPacketID(), 8));
            ret = FAILED;
        }
        return ret;
    }

    private int quitDebuggee(int stat) {
        pipe.println(COMMAND_QUIT);
        debuggee.waitFor();
        return stat;
    }
}
