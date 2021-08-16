/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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

public class ManifestTestApp {
    public static void main(String args[]) {
        System.out.println("Hello from ManifestTestApp!");

        new ManifestTestApp().doTest();
        System.exit(0);
    }

    private void doTest() {
        try {
            // load the class only found via the Boot-Class-Path attribute
            Object instance = loadExampleClass();
            if (instance.getClass().getClassLoader() == null) {
                System.out.println("PASS: ExampleForBootClassPath was loaded" +
                    " by the boot class path loader.");
            } else {
                System.out.println("FAIL: ExampleForBootClassPath was loaded" +
                    " by a non-boot class path loader.");
                System.exit(1);
            }
        } catch (NoClassDefFoundError ncdfe) {
            // This message just lets ManifestTest.sh know whether or
            // not ExampleForBootClassPath was loaded. Depending on
            // the current test case, that will be either a PASSing
            // condition or a FAILing condition as determined by
            // ManifestTest.sh.
            System.out.println("ExampleForBootClassPath was not loaded.");
        }
    }

    Object loadExampleClass() {
        ExampleForBootClassPath instance = new ExampleForBootClassPath();
        System.out.println("ExampleForBootClassPath was loaded.");
        if (instance.fifteen() == 15) {
            System.out.println("PASS: the correct" +
                " ExampleForBootClassPath was loaded.");
        } else {
            System.out.println("FAIL: the wrong ExampleForBootClassPath" +
                " was loaded.");
            System.out.println("FAIL: instance.fifteen()=" +
                instance.fifteen());
            System.exit(1);
        }
        return instance;
    }
}
