/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7192246
 * @summary  check that javac does not generate bridge methods for defaults
 * @modules jdk.jdeps/com.sun.tools.classfile
 */

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPool.*;
import com.sun.tools.classfile.Method;

import java.io.*;

public class TestNoBridgeOnDefaults {

    interface A<X> {
        default <Y> A<X> m(X x, Y y) { return Impl.<X,Y>m1(this, x, y); }
    }

    static abstract class B<X> implements A<X> { }

    interface C<X> extends A<X> {
        default <Y> C<X> m(X x, Y y) { return Impl.<X,Y>m2(this, x, y); }
    }

    static abstract class D<X> extends B<X> implements C<X> { }

    static class Impl {
       static <X, Y> A<X> m1(A<X> rec, X x, Y y) { return null; }
       static <X, Y> C<X> m2(C<X> rec, X x, Y y) { return null; }
    }

    static final String[] SUBTEST_NAMES = { B.class.getName() + ".class", D.class.getName() + ".class" };
    static final String TEST_METHOD_NAME = "m";

    public static void main(String... args) throws Exception {
        new TestNoBridgeOnDefaults().run();
    }

    public void run() throws Exception {
        String workDir = System.getProperty("test.classes");
        for (int i = 0 ; i < SUBTEST_NAMES.length ; i ++) {
            File compiledTest = new File(workDir, SUBTEST_NAMES[i]);
            checkNoBridgeOnDefaults(compiledTest);
        }
    }

    void checkNoBridgeOnDefaults(File f) {
        System.err.println("check: " + f);
        try {
            ClassFile cf = ClassFile.read(f);
            for (Method m : cf.methods) {
                String mname = m.getName(cf.constant_pool);
                if (mname.equals(TEST_METHOD_NAME)) {
                    throw new Error("unexpected bridge method found " + m);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("error reading " + f +": " + e);
        }
    }
}
