/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6904403 8010319
 * @summary Don't assert if we redefine finalize method
 * @requires vm.jvmti
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @modules java.compiler
 *          java.instrument
 *          jdk.jartool/sun.tools.jar
 * @run main RedefineClassHelper
 * @run main/othervm -javaagent:redefineagent.jar RedefineFinalizer
 */

/*
 * Regression test for hitting:
 *
 * assert(f == k->has_finalizer()) failed: inconsistent has_finalizer
 *
 * when redefining finalizer method
 */


// package access top-level class to avoid problem with RedefineClassHelper
// and nested types.
class RedefineFinalizer_B {
    protected void finalize() {
        // should be empty
    }
}

public class RedefineFinalizer {

    public static String newB =
                "class RedefineFinalizer_B {" +
                "   protected void finalize() { " +
                "       System.out.println(\"Finalizer called\");" +
                "   }" +
                "}";

    public static void main(String[] args) throws Exception {
        RedefineClassHelper.redefineClass(RedefineFinalizer_B.class, newB);

        A a = new A();
    }

    static class A extends RedefineFinalizer_B {
    }
}
