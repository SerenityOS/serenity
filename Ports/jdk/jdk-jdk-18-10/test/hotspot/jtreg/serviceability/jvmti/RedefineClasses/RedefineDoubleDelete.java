/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8178870 8010319
 * @summary Redefine class with CFLH twice to test deleting the cached_class_file
 * @requires vm.jvmti
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @modules java.compiler
 *          java.instrument
 *          jdk.jartool/sun.tools.jar
 * @run main RedefineClassHelper
 * @run main/othervm/native -Xlog:redefine+class+load+exceptions -agentlib:RedefineDoubleDelete -javaagent:redefineagent.jar RedefineDoubleDelete
 */

// package access top-level class to avoid problem with RedefineClassHelper
// and nested types.

// The ClassFileLoadHook for this class turns foo into faa and prints out faa.
class RedefineDoubleDelete_B {
    int faa() { System.out.println("foo"); return 1; }
}

public class RedefineDoubleDelete {

    // Class gets a redefinition error because it adds a data member
    public static String newB =
                "class RedefineDoubleDelete_B {" +
                "   int count1 = 0;" +
                "}";

    public static String newerB =
                "class RedefineDoubleDelete_B { " +
                "   int faa() { System.out.println(\"baa\"); return 2; }" +
                "}";

    public static void main(String args[]) throws Exception {

        RedefineDoubleDelete_B b = new RedefineDoubleDelete_B();
        int val = b.faa();
        if (val != 1) {
            throw new RuntimeException("return value wrong " + val);
        }

        // Redefine B twice to get cached_class_file in both B scratch classes
        try {
            RedefineClassHelper.redefineClass(RedefineDoubleDelete_B.class, newB);
        } catch (java.lang.UnsupportedOperationException e) {
            // this is expected
        }
        try {
            RedefineClassHelper.redefineClass(RedefineDoubleDelete_B.class, newB);
        } catch (java.lang.UnsupportedOperationException e) {
            // this is expected
        }

        // Do a full GC.
        System.gc();

        // Redefine with a compatible class
        RedefineClassHelper.redefineClass(RedefineDoubleDelete_B.class, newerB);
        val = b.faa();
        if (val != 2) {
            throw new RuntimeException("return value wrong " + val);
        }

        // Do another full GC to clean things up.
        System.gc();
    }
}
