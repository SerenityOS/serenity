/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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


package nsk.jdi.Field.type;

import com.sun.jdi.*;
import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


public class type002a {
    public static void main (String argv[]) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(System.err, argHandler);
        IOPipe pipe = argHandler.createDebugeeIOPipe(log);
        type002aClassToCheck check = new type002aClassToCheck();

        log.display("DEBUGEE> debugee started.");
        pipe.println("ready");
        String instruction = pipe.readln();
        if (instruction.equals("quit")) {
            log.display("DEBUGEE> \"quit\" signal recieved.");
            log.display("DEBUGEE> completed succesfully.");
            System.exit(95);
        }
        log.complain("DEBUGEE FAILURE> unexpected signal "
                   + "(no \"quit\") - " + instruction);
        log.complain("DEBUGEE FAILURE> TEST FAILED");
        System.exit(97);
    }
}

class type002aClassToCheck {
    // No array fields
    class Class {}
    Class     X0 = new Class();
    Boolean   Z0 = Boolean.valueOf(true);
    Byte      B0 = Byte.valueOf(Byte.MIN_VALUE);
    Character C0 = Character.valueOf('\u00ff');
    Double    D0 = Double.valueOf(1);
    Float     F0 = Float.valueOf(1f);
    Integer   I0 = Integer.valueOf(1);
    Long      L0 = Long.valueOf(1l);
    String    S0 = new String();
    Object    O0 = new Object();

    static    Long LS0 = Long.valueOf(1l);
    private   Long LP0 = Long.valueOf(1l);
    public    Long LU0 = Long.valueOf(1l);
    protected Long LR0 = Long.valueOf(1l);
    transient Long LT0 = Long.valueOf(1l);
    volatile  Long LV0 = Long.valueOf(1l);
    final     Long LF0 = Long.valueOf(1l);

    interface Inter {}
    static class InterClass implements Inter {}
    Inter E0 = new InterClass();
    static    Inter ES0 = new InterClass();
    private   Inter EP0 = new InterClass();
    public    Inter EU0 = new InterClass();
    protected Inter ER0 = new InterClass();
    transient Inter ET0 = new InterClass();
    volatile  Inter EV0 = new InterClass();
    final     Inter EF0 = new InterClass();
}
