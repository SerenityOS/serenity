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

package nsk.jvmti.GetMethodName;

import java.io.*;
import java.util.*;

import nsk.share.*;

/**
 * This test checks that the JVMTI function <code>GetMethodName()</code>
 * returns generic signature information for methods properly.<br>
 * The test creates instances of several tested classes with methods to
 * be checked. Some of the classes are generic and accordingly contain
 * generic methods. An agent part obtains the method signatures.
 * Proper generic signatures should be returned for the generic methods,
 * or NULL for non-generic ones.
 */
public class methname003 {
    static {
        try {
            System.loadLibrary("methname003");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load \"methname003\" library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native int check(Object cls, int clsIdx);

    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // produce JCK-like exit status
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new methname003().runThis(argv, out);
    }

    private int runThis(String argv[], PrintStream out) {
        int res = Consts.TEST_PASSED;

        // create instances of the tested classes
        methname003b<String> _methname003b =
            new methname003b<String>();
        methname003c<Boolean, Integer> _methname003c =
            new methname003c<Boolean, Integer>();
        methname003e _methname003e =
            new methname003e();
        methname003if<Object> _methname003if =
            new methname003d<Object>();
        methname003g<methname003f> _methname003g =
            new methname003g<methname003f>();

        if (check(_methname003b, 0) == Consts.TEST_FAILED)
            res = Consts.TEST_FAILED;
        if (check(_methname003c, 1) == Consts.TEST_FAILED)
            res = Consts.TEST_FAILED;
        if (check(_methname003e, 2) == Consts.TEST_FAILED)
            res = Consts.TEST_FAILED;
        if (check(_methname003if, 3) == Consts.TEST_FAILED)
            res = Consts.TEST_FAILED;
        if (check(_methname003g, 4) == Consts.TEST_FAILED)
            res = Consts.TEST_FAILED;

        return res;
    }
}

/*
 * Dummy classes used only for verifying generic signature information
 * in an agent.
 */

class methname003b<L extends String> {
    <L extends String> methname003b<String> methname003bMeth(methname003b<L> m) {
        return new methname003b<String>();
    }

    static <T extends String> methname003b<String> methname003bMethSt(methname003b<T> m) {
        return new methname003b<String>();
    }
}

class methname003c<A, B extends Integer> {
    public <U> U methname003cMeth(Class<U> klass) throws Exception {
        return klass.newInstance();
    }

    static public <U> U methname003cMethSt(Class<U> klass) throws Exception {
        return klass.newInstance();
    }
}

interface methname003if<I> {
    int methname003ifMeth();

    <I> int methname003ifMeth2(I v);
}

class methname003d<T> implements methname003if<T> {
    public int methname003ifMeth() {
        return 1;
    }

    public <T> int methname003ifMeth2(T v) {
        return 2;
    }
}

class methname003e {
    void methname003eMeth(methname003e e) {}
    static void methname003eMethSt(methname003e e) {}
}

class methname003f extends methname003e implements methname003if {
    public int methname003ifMeth() {
        return 3;
    }

    public int methname003ifMeth2(Object v) {
        return 4;
    }
}

class methname003g<E extends methname003e & methname003if> {
    <A extends Byte, B extends Double> void methname003gMeth(A a, B b, Class<?>[] c) {}

    static <A extends Byte, B extends Double> void methname003gMethSt(A a, B b) {}
}
