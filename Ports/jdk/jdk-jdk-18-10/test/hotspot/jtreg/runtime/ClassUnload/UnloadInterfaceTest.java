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
 * @test UnloadInterfaceTest
 * @requires vm.opt.final.ClassUnloading
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @compile test/Interface.java
 * @compile test/ImplementorClass.java
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -Xmn8m -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xlog:class+unload=trace UnloadInterfaceTest
 */
import sun.hotspot.WhiteBox;
import test.Interface;
import java.lang.ClassLoader;
import jdk.test.lib.classloader.ClassUnloadCommon;

/**
 * Test that verifies that class unloaded removes the implementor from its the interface that it implements
 * via logging.
 * [1.364s][info][class,unload] unloading class test.ImplementorClass 0x00000008000a2840
 * [1.366s][trace][class,unload] unlinking class (subclass): test.ImplementorClass
 * [1.366s][trace][class,unload] unlinking class (implementor): test.ImplementorClass
 */
public class UnloadInterfaceTest {
    private static String className = "test.ImplementorClass";
    private static String interfaceName = "test.Interface";

    static class LoaderToUnload extends ClassLoader {
       ClassLoader myParent;
        public Class loadClass(String name) throws ClassNotFoundException {
            if (name.contains(className)) {
              System.out.println("className found " + className);
              byte[] data = ClassUnloadCommon.getClassData(name);
              return defineClass(name, data, 0, data.length);
            } else {
              return myParent.loadClass(name);
            }
        }
        public LoaderToUnload(ClassLoader parent) {
            super();
            myParent = parent;
        }
    }

    public static void main(String... args) throws Exception {
       run();
    }

    private static void run() throws Exception {
        final WhiteBox wb = WhiteBox.getWhiteBox();

        ClassUnloadCommon.failIf(wb.isClassAlive(className), "is not expected to be alive yet");

        // Load interface Class with one class loader.
        ClassLoader icl = ClassUnloadCommon.newClassLoader();
        Class<?> ic = icl.loadClass(interfaceName);

        ClassLoader cl = new LoaderToUnload(icl);
        Class<?> c = cl.loadClass(className);
        Object o = c.newInstance();

        ClassUnloadCommon.failIf(!wb.isClassAlive(className), "should be live here");
        ClassUnloadCommon.failIf(!wb.isClassAlive(interfaceName), "should be live here");

        cl = null; c = null; o = null;
        ClassUnloadCommon.triggerUnloading();
        ClassUnloadCommon.failIf(wb.isClassAlive(className), "should have been unloaded");
        ClassUnloadCommon.failIf(!wb.isClassAlive(interfaceName), "should be live here");
        System.out.println("We still have Interface referenced" + ic);
    }
}

