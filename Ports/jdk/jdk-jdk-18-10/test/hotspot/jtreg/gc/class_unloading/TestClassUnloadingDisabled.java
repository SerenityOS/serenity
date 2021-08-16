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

package gc.class_unloading;

/*
 * @test TestClassUnloadingDisabledSerial
 * @bug 8114823
 * @requires vm.opt.ExplicitGCInvokesConcurrent != true
 * @requires vm.opt.ClassUnloading != true
 * @requires vm.gc.Serial
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:-ClassUnloading -XX:+UseSerialGC gc.class_unloading.TestClassUnloadingDisabled
 *
 */

/*
 * @test TestClassUnloadingDisabledParallel
 * @bug 8114823
 * @requires vm.opt.ExplicitGCInvokesConcurrent != true
 * @requires vm.opt.ClassUnloading != true
 * @requires vm.gc.Parallel
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:-ClassUnloading -XX:+UseParallelGC gc.class_unloading.TestClassUnloadingDisabled
 *
 */

/*
 * @test TestClassUnloadingDisabledG1
 * @bug 8114823
 * @requires vm.opt.ExplicitGCInvokesConcurrent != true
 * @requires vm.opt.ClassUnloading != true
 * @requires vm.gc.G1
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:-ClassUnloading -XX:+UseG1GC gc.class_unloading.TestClassUnloadingDisabled
 *
 */

/*
 * @test TestClassUnloadingDisabledShenandoah
 * @bug 8114823
 * @requires vm.gc.Shenandoah
 * @requires vm.opt.ExplicitGCInvokesConcurrent != true
 * @requires vm.opt.ClassUnloading != true
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:-ClassUnloading -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC gc.class_unloading.TestClassUnloadingDisabled
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import sun.hotspot.WhiteBox;

import static jdk.test.lib.Asserts.*;

public class TestClassUnloadingDisabled {
    public static void main(String args[]) throws Exception {
        final WhiteBox wb = WhiteBox.getWhiteBox();
        // Fetch the dir where the test class and the class
        // to be loaded resides.
        String classDir = TestClassUnloadingDisabled.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String className = "gc.class_unloading.ClassToLoadUnload"; // can not use ClassToLoadUnload.class.getName() as that would load the class

        assertFalse(wb.isClassAlive(className), "Should not be loaded yet");

        // The NoPDClassLoader handles loading classes in the test directory
        // and loads them without a protection domain, which in some cases
        // keeps the class live regardless of marking state.
        NoPDClassLoader nopd = new NoPDClassLoader(classDir);
        nopd.loadClass(className);

        assertTrue(wb.isClassAlive(className), "Class should be loaded");

        // Clear the class-loader, class and object references to make
        // class unloading possible.
        nopd = null;

        System.gc();
        assertTrue(wb.isClassAlive(className), "Class should not have ben unloaded");
    }
}

class NoPDClassLoader extends ClassLoader {
    String path;

    NoPDClassLoader(String path) {
        this.path = path;
    }

    public Class<?> loadClass(String name) throws ClassNotFoundException {
        byte[] cls = null;
        File f = new File(path,name + ".class");

        // Delegate class loading if class not present in the given
        // directory.
        if (!f.exists()) {
            return super.loadClass(name);
        }

        try {
            Path path = Paths.get(f.getAbsolutePath());
            cls = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new ClassNotFoundException(name);
        }

        // Define class with no protection domain and resolve it.
        return defineClass(name, cls, 0, cls.length, null);
    }
}

class ClassToLoadUnload {
}
