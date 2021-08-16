/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.Type.hashCode;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * The debugged application of the test.
 */
public class hashcode001a {

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

    static boolean z0 = true, z1[] = {z0}, z2[][] = {z1};
    static byte    b0 = 0,    b1[] = {b0}, b2[][] = {b1};
    static char    c0 = '0',  c1[] = {c0}, c2[][] = {c1};
    static double  d0 = 0.0d, d1[] = {d0}, d2[][] = {d1};
    static float   f0 = 0.0f, f1[] = {f0}, f2[][] = {f1};
    static int     i0 = 0,    i1[] = {i0}, i2[][] = {i1};
    static long    l0 = 0,    l1[] = {l0}, l2[][] = {l1};
    static short   r0 = 0,    r1[] = {r0}, r2[][] = {r1};

    static String  s0 = "0",  s1[] = {s0}, s2[][] = {s1};
    static Object  o0 = new Object(), o1[] = {o0}, o2[][] = {o1};

    //------------------------------------------------------ mutable common method

    public static void main (String argv[]) {
        exitStatus = Consts.TEST_FAILED;
        argHandler = new ArgumentHandler(argv);
        log = new Log(System.err, argHandler);
        pipe = argHandler.createDebugeeIOPipe(log);
        try {
            pipe.println(hashcode001.SIGNAL_READY);
            receiveSignal(hashcode001.SIGNAL_QUIT);
            display("completed succesfully.");
            System.exit(Consts.TEST_PASSED + Consts.JCK_STATUS_BASE);
        } catch (Failure e) {
            log.complain(e.getMessage());
            System.exit(Consts.TEST_FAILED + Consts.JCK_STATUS_BASE);
        }
    }

    //--------------------------------------------------------- test specific methods

    void Mv() {};

    static              void MvS() {};
    strictfp            void MvI() {};
    synchronized        void MvY() {};
    public              void MvU() {};
    protected           void MvR() {};
    private             void MvP() {};
    native              void MvN();

    boolean Mz () { return true; };
    byte    Mb () { return (byte)0; };
    char    Mc () { return '0'; };
    double  Md () { return 0.0d; };
    float   Mf () { return 0.0f; };
    int     Mi () { return 0; };
    long    Ml () { return (long)0; };
    short   Mr () { return (short)0; };

    String  Ms () { return "0"; };
    Object  Mo () { return new Object(); };

}

//--------------------------------------------------------- test specific classes
