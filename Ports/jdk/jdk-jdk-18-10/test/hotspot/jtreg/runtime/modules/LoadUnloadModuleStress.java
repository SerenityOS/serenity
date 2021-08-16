/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Ensure module information is cleaned when owning class loader unloads
 * @requires vm.opt.final.ClassUnloading
 * @modules java.base/jdk.internal.misc
 * @library /test/lib ..
 * @build sun.hotspot.WhiteBox
 * @compile/module=java.base java/lang/ModuleHelper.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xmx64m -Xmx64m LoadUnloadModuleStress 15000
 */

import java.lang.ref.WeakReference;

import static jdk.test.lib.Asserts.*;

public class LoadUnloadModuleStress {
    private static long timeout;
    private static long timeStamp;

    public static byte[] garbage;
    public static volatile WeakReference<MyClassLoader> clweak;

    public static Object createModule() throws Throwable {
        MyClassLoader cl = new MyClassLoader();
        Object module = ModuleHelper.ModuleObject("mymodule", cl, new String [] {"PackageA"});
        assertNotNull(module);
        ModuleHelper.DefineModule(module, false, "9.0", "mymodule", new String[] { "PackageA" });
        clweak = new WeakReference<>(cl);
        return module;
    }

    public static void main(String args[]) throws Throwable {
        timeout = Long.valueOf(args[0]);
        timeStamp = System.currentTimeMillis();

        while(System.currentTimeMillis() - timeStamp < timeout) {
            WeakReference<Object> modweak = new WeakReference<>(createModule());

            while(clweak.get() != null) {
                garbage = new byte[8192];
                System.gc();
            }
            assertNull(modweak.get());
        }
    }
    static class MyClassLoader extends ClassLoader { }
}
