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


package nsk.jdi.TypeComponent.declaringType;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


public class decltype008a {
    public static void main (String argv[]) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(System.err, argHandler);
        IOPipe pipe = argHandler.createDebugeeIOPipe(log);
        decltype008aOtherClass otherClass = new decltype008aOtherClass(new String("decltype008a"), 1f);

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

class decltype008aOtherClass extends decltype008aMainClass {
    // All contructors from decltype008aOtherClass starts with reference to String object
    public decltype008aOtherClass(String S, float f) {
        super(Long.valueOf(1), f);
    };
    private decltype008aOtherClass(String S, Object obj){
        super(Long.valueOf(1), obj);
    };
    protected decltype008aOtherClass(String S, long[] l) {
        super(Long.valueOf(1), l);
    };

    static double cd;
    static String cS;
    static float[] cf = new float[10];

    static { cd = 1; }
    static { cS = new String(); }
    static {
        for (int i = 0; i < 10; i++) {
            cf[i] = (float)i;
        }
    }
}

class decltype008aMainClass {
    // All contructors from decltype008aMainClass starts with reference to Long object
    decltype008aMainClass(Long L, float f)    {};
    decltype008aMainClass(Long L, Object obj) {};
    decltype008aMainClass(Long L, long[] l)   {};

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
