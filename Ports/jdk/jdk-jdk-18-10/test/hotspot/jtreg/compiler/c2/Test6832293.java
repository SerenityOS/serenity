/*
 * Copyright (c) 2008, 2009, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 6832293
 * @summary JIT compiler got wrong result in type checking with -server
 *
 * @run main/othervm -Xcomp
 *      -XX:CompileCommand=compileonly,compiler.c2.Test6832293::run
 *      compiler.c2.Test6832293
 */

package compiler.c2;

import java.io.PrintStream;

public class Test6832293 {
    static interface SomeInterface {
        int SEVENS = 777;
    }

    static interface AnotherInterface {
        int THIRDS = 33;
    }

    static class SomeClass implements SomeInterface {
        int i;

        SomeClass(int i) {
            this.i = i;
        }
    }

    static class ImmediateSubclass extends SomeClass implements SomeInterface {
        float f;

        ImmediateSubclass(int i, float f) {
            super(i);
            this.f = f;
        }
    }

    static final class FinalSubclass extends ImmediateSubclass implements AnotherInterface {
        double d;

        FinalSubclass(int i, float f, double d) {
            super(i, f);
            this.d = d;
        }
    }

    public static void main(String args[]) throws Exception{
        /* try to pre initialize */
        SomeClass[] a=new SomeClass[10];
        String className = Test6832293.class.getName();
        Class.forName(className + "$ImmediateSubclass");
        Class.forName(className + "$FinalSubclass");
        System.exit(run(args, System.out) + 95/*STATUS_TEMP*/);
    }

    static int errorStatus = 0/*STATUS_PASSED*/;

    static void errorAlert(PrintStream out, int errorLevel) {
        out.println("Test: failure #" + errorLevel);
        errorStatus = 2/*STATUS_FAILED*/;
    }

    static SomeClass[] v2 = new FinalSubclass[4];

    public static int run(String args[],PrintStream out) {
        int i [], j [];
        SomeInterface u [], v[] [];
        AnotherInterface w [];
        SomeClass x [] [];

        i = new int [10];
        i[0] = 777;
        j = (int []) i;
        if (j != i)
            errorAlert(out, 2);
        else if (j.length != 10)
            errorAlert(out, 3);
        else if (j[0] != 777)
            errorAlert(out, 4);

        v = new SomeClass [3] [];
        x = (SomeClass [] []) v;
        if (! (x instanceof SomeInterface [] []))
            errorAlert(out, 5);
        else if (! (x instanceof SomeClass [] []))
            errorAlert(out, 6);
        else if (x != v)
            errorAlert(out, 7);

        x[0] = (SomeClass []) new ImmediateSubclass [4];
        if (! (x[0] instanceof ImmediateSubclass []))
            errorAlert(out, 8);
        else if (x[0].length != 4)
            errorAlert(out, 9);

        x[1] = (SomeClass []) v2;
        if (! (x[1] instanceof FinalSubclass []))
            errorAlert(out, 10);
        else if (x[1].length != 4)
            errorAlert(out, 11);

        w = (AnotherInterface []) x[1];
        if (! (w instanceof FinalSubclass []))
            errorAlert(out, 12);
        else if (w != x[1])
            errorAlert(out, 13);
        else if (w.length != 4)
            errorAlert(out, 14);

        return errorStatus;
    }
}

