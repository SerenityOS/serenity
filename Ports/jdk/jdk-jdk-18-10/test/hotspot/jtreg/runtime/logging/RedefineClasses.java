/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8197901 8209758
 * @summary Redefine classes with enabling logging to verify Klass:external_name() during GC.
 * @comment This test is simplified version of serviceability/jvmti/RedefineClasses/RedefineRunningMethods.java.
 * @library /test/lib
 * @modules java.compiler
 *          java.instrument
 * @requires vm.jvmti
 * @run main RedefineClassHelper
 * @run main/othervm -Xmx256m -XX:MaxMetaspaceSize=64m -javaagent:redefineagent.jar -Xlog:all=trace:file=all.log RedefineClasses
 */

// package access top-level class to avoid problem with RedefineClassHelper
// and nested types.
class RedefineClasses_B {
    public static void test() {
    }
}

public class RedefineClasses {
    static Object[] obj = new Object[20];
    public static String newB =
            "class RedefineClasses_B {" +
            "    public static void test() { " +
            "    }" +
            "}";

    public static void main(String[] args) throws Exception {
        RedefineClassHelper.redefineClass(RedefineClasses_B.class, newB);
        RedefineClasses_B.test();
        for (int i = 0; i < 20 ; i++) {
            obj[i] = new byte[1024 * 1024];
            System.gc();
        }
    }
}
