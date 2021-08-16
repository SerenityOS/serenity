/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8237894
 * @summary CTW: C1 compilation fails with assert(x->type()->tag() == f->type()->tag()) failed: should have same type
 *
 * @run main/othervm
 *      -Xcomp -Xbatch -XX:CompileCommand=compileonly,compiler.c1.T*::*
 *      -XX:CompileCommand=exclude,compiler.c1.TestValueNumberingNullObject::main
 *      -XX:CompileCommand=inline,*.*
 *      -XX:TieredStopAtLevel=3
 *      compiler.c1.TestValueNumberingNullObject
 */

package compiler.c1;

class T1 {

    public T2 f1;

    public int za() {
        return 0;
    }

    public int zb() {
        return 0;
    }

    public int zc() {
        return 0;
    }

    public int zd() {
        return 0;
    }

    public int ze() {
        return 0;
    }

    public int zf() {
        return 0;
    }

    public int zg() {
        return 0;
    }

    public int zh() {
        return 0;
    }
}

class T2 {

    public T1 f1;

    public int zh() {
        return 0;
    }
}

public class TestValueNumberingNullObject {

    public static void main(String args[]) {
        new T1();  // Load
        new T2();  // Load
        try {
            // case 1
            // Null based field access.
            // Value Numbering null based field access causes instructions to be eliminated across type/subtypes.
            // declared type of these instructions are field type, so it being receiver causes problems to Type System.
            // to mitigate this issue, we hash declared type in addition to existing hashing.
            testFieldAccess();
        } catch (Exception e) {
        }
        try {
            // case 2
            // Null based indexed access.
            // Value Numbering null based indexed access causes instructions to be eliminated across type/subtypes.
            // element basic type in encoded in the access instruction, this causes problems to Type system.
            // declared type of these instructions are null, so it being receiver doesn't cause any problem to Type System.
            // to mitigate this issue, we hash basic type in addition to existing hashing
            basicTypeAccess();
        } catch (Exception e) {
        }
    }

    static long testFieldAccess() {
        T1 t1 = null;
        T2 t2 = null;
        T1[] t3 = null;
        T2[] t4 = null;

        long value = t1.f1.zh() + t2.f1.zh();
        // null array object based field access.
        value += t3[2].f1.zh() + t4[2].f1.zh();
        return value;
    }

    static long basicTypeAccess() {
        long[] f1 = null;
        int[] f2 = null;
        T2[] t2 = null;
        T1[] t1 = null;
        return f1[5] + f2[5] + t2[5].zh() + t1[5].zh();
    }
}

