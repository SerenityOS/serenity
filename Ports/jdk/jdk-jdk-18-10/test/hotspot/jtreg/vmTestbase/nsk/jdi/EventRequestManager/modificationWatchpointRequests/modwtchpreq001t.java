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

package nsk.jdi.EventRequestManager.modificationWatchpointRequests;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


/**
 * This is a debuggee class containing different kinds of fields
 */
public class modwtchpreq001t {
// instance fields
    byte    byteFld = 127;
    short   shortFld = -32768;
    int     intFld = 2147483647;
    long    longFld = 9223372036854775807L;
    float   floatFld = 5.1F;
    double  doubleFld = 6.2D;
    char    charFld = 'a';
    boolean booleanFld = false;
    String  strFld = "instance field";
// fields with different access rights
    static    short sFld = 32767;
    private   byte prFld = -128;
    public    float pubFld = 0.9F;
    protected double protFld = -435.789D;
    transient int  tFld = -2147483648;
    volatile  long vFld = -922337203685477580L;
    final     char fFld = 'z';

    public static void main(String args[]) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        IOPipe pipe = argHandler.createDebugeeIOPipe();

        pipe.println(modwtchpreq001.COMMAND_READY);
        String cmd = pipe.readln();
        if (!cmd.equals(modwtchpreq001.COMMAND_QUIT)) {
            System.err.println("TEST BUG: unknown debugger command: "
                + cmd);
            System.exit(modwtchpreq001.JCK_STATUS_BASE +
                modwtchpreq001.FAILED);
        }
        System.exit(modwtchpreq001.JCK_STATUS_BASE +
            modwtchpreq001.PASSED);
    }
}
