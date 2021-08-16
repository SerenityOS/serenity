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

package nsk.jdb.methods.methods002;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdb.*;

import java.io.*;

/* This is debuggee aplication */
public class methods002a {
    static methods002a _methods002a = new methods002a();

    public static void main(String args[]) {
       System.exit(methods002.JCK_STATUS_BASE + _methods002a.runIt(args, System.out));
    }

    static void lastBreak () {}

    public int runIt(String args[], PrintStream out) {
        JdbArgumentHandler argumentHandler = new JdbArgumentHandler(args);
        Log log = new Log(out, argumentHandler);

        methods002b b = new methods002b(1);
        methods002c c = new methods002c();
        methods002e e = new methods002e();
        methods002f f = new methods002f();
        methods002g g = new methods002g();
        lastBreak();

        log.display("Debuggee PASSED");
        return methods002.PASSED;
    }

    static long lo;

    // various method modifiers
                 void  m01 (long l) { lo = l; };
    private      void  m02 (long l) { lo = l; };
    protected    void  m03 (long l) { lo = l; };
    public       void  m04 (long l) { lo = l; };
    static       void  m05 (long l) { lo = l; };
    synchronized void  m06 (long l) { lo = l; };
    strictfp     void  m07 (long l) { lo = l; };
    native       void  m08 (long l);
    public static synchronized strictfp void m09 (long l) { lo = l; };

                 long  m10 (long l) { return lo + l; };
    private      long  m11 (long l) { return lo + l; };
    protected    long  m12 (long l) { return lo + l; };
    public       long  m13 (long l) { return lo + l; };
    static       long  m14 (long l) { return lo + l; };
    synchronized long  m15 (long l) { return lo + l; };
    strictfp     long  m16 (long l) { return lo + l; };
    native       long  m17 (long l);
    public static synchronized strictfp long m18 (long l) { return lo + l; };

                 Object  m19 () { return new Object(); };
    private      Object  m20 () { return new Object(); };
    protected    Object  m21 () { return new Object(); };
    public       Object  m22 () { return new Object(); };
    static       Object  m23 () { return new Object(); };
    synchronized Object  m24 () { return new Object(); };
    strictfp     Object  m25 () { return new Object(); };
    native       Object  m26 ();
    public static synchronized strictfp Object m27 () { return new Object(); };


    // array methods
    double[]   m28 () { return new double[1]; };
    double     m29 (double[] arr) {return arr[0];};
    double[][] m30 (double[][] arr) {return arr;};

    String[]   m31 () { return new String[1];};
    String     m32 (String[] arr) { return arr[0];};
    String[][] m33 (String[][] arr) {return arr;};

    // final methods
    final        void    f01 (long l) { lo = l; };
    final        long    f02 (long l) { return lo + l; };
    final        Object  f03 () { return new Object(); };
}

// Class with many constructors
class methods002b {
    int ind;
    methods002b (int i) { ind = i; };

    private   methods002b (int i, int j) { ind = i+j; };
    protected methods002b (int i, int j, int k) { ind = i+j+k; };
    public    methods002b (int i, int j, int k, int l) { ind = i+j+k+l; };
}

// Class with overloaded methods
class methods002c {
    int m01 (int i) { return i; };
    int m01 (int i, int j) { return i+j; };
    int m01 (int i, short j) { return i+j; };
}

// Class with abstract methods
abstract class methods002d {
    abstract void m01 ();
}

interface methods002i {
    void i01 ();
}

class methods002e extends methods002d implements methods002i {
    void m01 () {};
    public void i01 () {};
}

// Class with inherited methods
class methods002f extends methods002a {}

// Class with inherited and overrided method
class methods002g extends methods002f {
    static long lo;

                 void  m01 (long l) { lo = l; };
    private      void  m02 (long l) { lo = l; };
    protected    void  m03 (long l) { lo = l; };
    public       void  m04 (long l) { lo = l; };
    static       void  m05 (long l) { lo = l; };
    synchronized void  m06 (long l) { lo = l; };
    strictfp     void  m07 (long l) { lo = l; };
    native       void  m08 (long l);
    public static synchronized strictfp void m09 (long l) { lo = l; };

                 long  m10 (long l) { return lo + l; };
    private      long  m11 (long l) { return lo + l; };
    protected    long  m12 (long l) { return lo + l; };
    public       long  m13 (long l) { return lo + l; };
    static       long  m14 (long l) { return lo + l; };
    synchronized long  m15 (long l) { return lo + l; };
    strictfp     long  m16 (long l) { return lo + l; };
    native       long  m17 (long l);
    public static synchronized strictfp long m18 (long l) { return lo + l; };

                 Object  m19 () { return new Object(); };
    private      Object  m20 () { return new Object(); };
    protected    Object  m21 () { return new Object(); };
    public       Object  m22 () { return new Object(); };
    static       Object  m23 () { return new Object(); };
    synchronized Object  m24 () { return new Object(); };
    strictfp     Object  m25 () { return new Object(); };
    native       Object  m26 ();
    public static synchronized strictfp Object m27 () { return new Object(); };

    double[]   m28 () { return new double[1]; };
    double     m29 (double[] arr) {return arr[0];};
    double[][] m30 (double[][] arr) {return arr;};

    String[]   m31 () { return new String[1];};
    String     m32 (String[] arr) { return arr[0];};
    String[][] m33 (String[][] arr) {return arr;};
}
