/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8027572
 * @summary class unloading resets profile, method compiled after the profile is
 * first set and before class loading sets unknown bit with not recorded class
 * @library /
 * @build compiler.profiling.unloadingconflict.B
 * @run main/othervm -XX:TypeProfileLevel=222 -XX:-BackgroundCompilation
 *                   compiler.profiling.unloadingconflict.TestProfileConflictClassUnloading
 *
 */

package compiler.profiling.unloadingconflict;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;

public class TestProfileConflictClassUnloading {
    static class A {
    }


    static void m1(Object o) {
    }

    static void m2(Object o) {
        m1(o);
    }

    static void m3(A a, boolean do_call) {
        if (!do_call) {
            return;
        }
        m2(a);
    }

    public static ClassLoader newClassLoader() {
        try {
            return new URLClassLoader(new URL[] {
                    Paths.get(System.getProperty("test.classes",".")).toUri().toURL(),
            }, null);
        } catch (MalformedURLException e){
            throw new RuntimeException("Unexpected URL conversion failure", e);
        }
    }

    public static void main(String[] args) throws Exception {
        ClassLoader loader = newClassLoader();
        Object o = loader.loadClass("compiler.profiling.unloadingconflict.B").newInstance();
        // collect conflicting profiles
        for (int i = 0; i < 5000; i++) {
            m2(o);
        }
        // prepare for conflict
        A a = new A();
        for (int i = 0; i < 5000; i++) {
            m3(a, false);
        }
        // unload class in profile
        o = null;
        loader = null;
        System.gc();
        // record the conflict
        m3(a, true);
        // trigger another GC
        System.gc();
    }
}
