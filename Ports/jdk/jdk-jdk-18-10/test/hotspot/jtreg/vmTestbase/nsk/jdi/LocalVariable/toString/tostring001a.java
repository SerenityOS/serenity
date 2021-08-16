/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.LocalVariable.toString;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * The debugged application of the test.
 */
public class tostring001a {

    //------------------------------------------------------- immutable common fields

    private static int exitStatus;
    private static ArgumentHandler argHandler;
    private static Log log;
    private static IOPipe pipe;

    //------------------------------------------------------- immutable common methods

    static void display(String msg) {
        log.display("debuggee > " + msg);
    }

    static void complain(String msg) {
        log.complain("debuggee FAILURE > " + msg);
    }

    public static void receiveSignal(String signal) {
        String line = pipe.readln();

        if ( !line.equals(signal) )
            throw new Failure("UNEXPECTED debugger's signal " + line);

        display("debuger's <" + signal + "> signal received.");
    }

    //------------------------------------------------------ mutable common fields

    //------------------------------------------------------ test specific fields

    //------------------------------------------------------ mutable common method

    public static void main (String argv[]) {
        exitStatus = Consts.TEST_FAILED;
        argHandler = new ArgumentHandler(argv);
        log = new Log(System.err, argHandler);
        pipe = argHandler.createDebugeeIOPipe(log);

        // Tested local variables --------------------
        boolean z0 = true,       z1[] = {z0}, z2[][] = {z1};
        byte    b0 = (byte)-1,    b1[] = {b0}, b2[][] = {b1};
        char    c0 = '\n',       c1[] = {c0}, c2[][] = {c1};
        double  d0 = (double)-1, d1[] = {d0}, d2[][] = {d1};
        float   f0 = (float)-1,  f1[] = {f0}, f2[][] = {f1};
        int     i0 = -1,         i1[] = {i0}, i2[][] = {i1};
        long    l0 = (long)-1,   l1[] = {l0}, l2[][] = {l1};
        short   r0 = (short)-1,  r1[] = {r0}, r2[][] = {r1};

        Boolean   Z0 = Boolean.valueOf(false),  Z1[] = {Z0}, Z2[][] = {Z1};
        Byte B0 = Byte.valueOf((byte)1),        B1[] = {B0}, B2[][] = {B1};
        Character C0 = Character.valueOf('z'),  C1[] = {C0}, C2[][] = {C1};
        Double D0 = Double.valueOf((double)1),  D1[] = {D0}, D2[][] = {D1};
        Float F0 = Float.valueOf((float)1),     F1[] = {F0}, F2[][] = {F1};
        Integer I0 = Integer.valueOf(1),        I1[] = {I0}, I2[][] = {I1};
        Long L0 = Long.valueOf((long)1),        L1[] = {L0}, L2[][] = {L1};
        Short R0 = Short.valueOf((short)1),     R1[] = {R0}, R2[][] = {R1};

        String    s0 = "string", s1[] = {s0}, s2[][] = {s1};
        Object    o0 = new Object(), o1[] = {o0}, o2[][] = {o1};
        tostring001aP p0 = new tostring001aP(), p1[] = {p0}, p2[][] = {p1};
        tostring001aM m0 = new tostring001aP(), m1[] = {m0}, m2[][] = {m1};
        //--------------------------------------------

        try {
            pipe.println(tostring001.SIGNAL_READY);

            receiveSignal(tostring001.SIGNAL_QUIT);
            display("completed succesfully.");
            System.exit(Consts.TEST_PASSED + Consts.JCK_STATUS_BASE);
        } catch (Failure e) {
            log.complain(e.getMessage());
            System.exit(Consts.TEST_FAILED + Consts.JCK_STATUS_BASE);
        }
    }

    //--------------------------------------------------------- test specific methods
}

//--------------------------------------------------------- test specific classes
class tostring001aP implements tostring001aM {}
interface tostring001aM {}
