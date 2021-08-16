/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8261395
 * @summary Test the code paths when a nest-host and nest-member class are in
 *          different protection domains and the compiler thread needs to
 *          perform a nestmate access check.
 * @comment We use WB to force-compile a constructor to recreate the original
 *          failure scenario, so only run when we have "normal" compiler flags.
 * @requires vm.compMode == "Xmixed" &
 *           vm.compiler2.enabled &
 *           (vm.opt.TieredStopAtLevel == null | vm.opt.TieredStopAtLevel == 4)
 * @library /test/lib /
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @compile Host.java
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -Xlog:class+nestmates=trace,protectiondomain=trace
 *                   -Djava.security.manager=allow
 *                   TestDifferentProtectionDomains
 */

import java.lang.reflect.Constructor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.security.ProtectionDomain;

import compiler.whitebox.CompilerWhiteBoxTest;
import sun.hotspot.WhiteBox;

public class TestDifferentProtectionDomains {

    static final String TARGET = "Host";

    // We need a custom classloader so that we can
    // use a different protection domain for our target classes.

    static class CustomLoader extends ClassLoader {

        CustomLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                // First, check if the class has already been loaded
                Class<?> clz = findLoadedClass(name);
                if (clz != null) {
                    return clz;
                }

                // Check for target class
                if (name.startsWith(TARGET)) {
                    try {
                        String clzFile = name.replaceAll("\\.", "/") + ".class";
                        byte[] buff = getResourceAsStream(clzFile).readAllBytes();
                        ProtectionDomain differentPD = new ProtectionDomain(null, null);
                        return defineClass(name, buff, 0, buff.length, differentPD);
                    } catch (Throwable t) {
                        throw new RuntimeException("Unexpected", t);
                    }
                }
            }
            return super.loadClass(name, resolve);
        }
    }

    public static void main(String[] args) throws Throwable {

        CustomLoader cl = new CustomLoader(TestDifferentProtectionDomains.class.getClassLoader());
        Class<?> host = cl.loadClass("Host");
        Class<?> member = cl.loadClass("Host$Member");

        if (host.getProtectionDomain() == member.getProtectionDomain()) {
            throw new Error("ProtectionDomain instances were not different!");
        }

        Constructor cons = member.getDeclaredConstructor(new Class<?>[] {});
        WhiteBox wb = WhiteBox.getWhiteBox();

        // The code path for the original failure is now only followed when
        // there is a security manager set, so we set one. We do this here
        // as any earlier causes security exceptions running the test and we
        // don't want to have to set up a policy file etc.
        System.setSecurityManager(new SecurityManager());

        // Force the constructor to compile, which then triggers the nestmate
        // access check in the compiler thread, which leads to the original bug.
        if (!wb.enqueueMethodForCompilation(cons, CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION)) {
            throw new RuntimeException("Failed to queue constructor for compilation");
        }
        while (!wb.isMethodCompiled(cons)) {
            Thread.sleep(100);
        }

        // Just for good measure call the compiled constructor.
        Object m = member.newInstance();
    }
}
