/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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


package nsk.jdi.TypeComponent.name;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


public class name003a {
    public static void main (String argv[]) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(System.err, argHandler);
        IOPipe pipe = argHandler.createDebugeeIOPipe(log);
        name003aClassToCheck classToCheck = new name003aClassToCheck();

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

class name003aClassToCheck extends name003aSuperClass {
              name003aClassToCheck()           {};
    public    name003aClassToCheck(long l)     {};
    private   name003aClassToCheck(Object obj) {};
    protected name003aClassToCheck(long[] l)   {};

    static int ci;
    static Long cL;
    static long[] cl = new long[10];

    static { ci = 1; }
    static { cL = Long.valueOf(1l); }
    static {
        for (int i = 0; i < 10; i++) {
            cl[i] = (long)i;
        }
    }
}

class name003aSuperClass {
              name003aSuperClass()           {};
    public    name003aSuperClass(long l)     {};
    private   name003aSuperClass(Object obj) {};
    protected name003aSuperClass(long[] l)   {};

    static boolean sb;
    static Object sO;
    static float[] sf = new float[10];

    static { sb = true; }
    static { sO = new Object(); }
    static {
        for (int i = 0; i < 10; i++) {
            sf[i] = (float)i;
        }
    }
}
