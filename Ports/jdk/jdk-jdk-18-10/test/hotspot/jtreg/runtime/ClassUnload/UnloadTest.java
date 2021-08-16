/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test UnloadTest
 * @bug 8210559
 * @requires vm.opt.final.ClassUnloading
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @library classes
 * @build sun.hotspot.WhiteBox test.Empty
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -Xmn8m -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xlog:class+unload=debug UnloadTest
 */
import sun.hotspot.WhiteBox;
import jdk.test.lib.classloader.ClassUnloadCommon;

/**
 * Test that verifies that classes are unloaded when they are no longer reachable.
 *
 * The test creates a class loader, uses the loader to load a class and creates an instance
 * of that class. The it nulls out all the references to the instance, class and class loader
 * and tries to trigger class unloading. Then it verifies that the class is no longer
 * loaded by the VM.
 */
public class UnloadTest {
    private static String className = "test.Empty";

    public static void main(String... args) throws Exception {
       run();
    }

    private static void run() throws Exception {
        final WhiteBox wb = WhiteBox.getWhiteBox();

        ClassUnloadCommon.failIf(wb.isClassAlive(className), "is not expected to be alive yet");

        ClassLoader cl = ClassUnloadCommon.newClassLoader();
        Class<?> c = cl.loadClass(className);
        Object o = c.newInstance();

        ClassUnloadCommon.failIf(!wb.isClassAlive(className), "should be live here");

        String loaderName = cl.getName();
        int loadedRefcount = wb.getSymbolRefcount(loaderName);
        System.out.println("Refcount of symbol " + loaderName + " is " + loadedRefcount);

        cl = null; c = null; o = null;
        ClassUnloadCommon.triggerUnloading();
        ClassUnloadCommon.failIf(wb.isClassAlive(className), "should have been unloaded");

        int unloadedRefcount = wb.getSymbolRefcount(loaderName);
        System.out.println("Refcount of symbol " + loaderName + " is " + unloadedRefcount);
        ClassUnloadCommon.failIf(unloadedRefcount != (loadedRefcount - 1), "Refcount must be decremented");
    }
}
