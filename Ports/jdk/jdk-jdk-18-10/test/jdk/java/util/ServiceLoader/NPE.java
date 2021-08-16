/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7197642
 * @summary test ServiceLoader.load methods for NullPointerException.
 */
import java.util.ServiceLoader;
import java.util.Arrays;

public final class NPE {
    abstract static class Test {
        String name;

        Test(String name) { this.name = name; }

        abstract void run();
    }

    static Test load = new Test("ServiceLoader.load(null)") {
        void run() { ServiceLoader.load(null); }
    };

    static Test loadWithClassLoader = new Test("ServiceLoader.load(null, loader)") {
        void run() { ServiceLoader.load(null, NPE.class.getClassLoader()); }
    };

    static Test loadInstalled = new Test("ServiceLoader.loadInstalled(null)") {
        void run() { ServiceLoader.loadInstalled(null); }
    };

    public static void main(String[] args) throws Exception {
        for (Test t : Arrays.asList(load, loadWithClassLoader, loadInstalled)) {
            NullPointerException caught = null;
            try {
                t.run();
            } catch (NullPointerException e) {
                caught = e;
            }
            if (caught == null) {
                throw new RuntimeException("NullPointerException expected for method invocation of " + t.name);
            }
        }
    }
}

