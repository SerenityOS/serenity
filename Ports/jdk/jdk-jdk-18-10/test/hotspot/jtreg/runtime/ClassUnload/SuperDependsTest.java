/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test SuperDependsTest
 * @bug 8210094
 * @summary Create ClassLoader dependency from initiating loader to class loader through subclassing
 * @requires vm.opt.final.ClassUnloading
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @compile p2/c2.java MyDiffClassLoader.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -Xmn8m -XX:+UnlockDiagnosticVMOptions -Xlog:class+unload -XX:+WhiteBoxAPI SuperDependsTest
 */

import sun.hotspot.WhiteBox;
import p2.*;
import jdk.test.lib.classloader.ClassUnloadCommon;

public class SuperDependsTest {
    public static WhiteBox wb = WhiteBox.getWhiteBox();
    public static final String MY_TEST = "SuperDependsTest$c1s";


    // p2.c2 loads through super class and creates dependency
    public static class c1s extends p2.c2 {

        private void test() throws Exception {
            method2();
        }

        public c1s () throws Exception {
            test();
            ClassUnloadCommon.triggerUnloading();  // should not unload anything
            test();
        }
    }

    public void test() throws Throwable {

        // now use the same loader to load class MyTest
        Class MyTest_class = new MyDiffClassLoader(MY_TEST).loadClass(MY_TEST);

        // Call MyTest to load p2.c2 twice and call p2.c2.method2
        MyTest_class.newInstance();
        ClassUnloadCommon.triggerUnloading();  // should not unload anything
        ClassUnloadCommon.failIf(!wb.isClassAlive(MY_TEST), "should not be unloaded");
        ClassUnloadCommon.failIf(!wb.isClassAlive("p2.c2"), "should not be unloaded");
        // Unless MyTest_class is referenced here, the compiler can unload it.
        System.out.println("Should not unload anything before here because " + MyTest_class + " is still alive.");
    }

    public static void main(String args[]) throws Throwable {
        SuperDependsTest d = new SuperDependsTest();
        d.test();
        ClassUnloadCommon.triggerUnloading();  // should not unload anything
        System.out.println("Should unload MyTest and p2.c2 just now");
        ClassUnloadCommon.failIf(wb.isClassAlive(MY_TEST), "should be unloaded");
        ClassUnloadCommon.failIf(wb.isClassAlive("p2.c2"), "should be unloaded");
    }
}
