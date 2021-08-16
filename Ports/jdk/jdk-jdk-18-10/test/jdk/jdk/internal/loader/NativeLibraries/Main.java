/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8240975
 * @modules java.base/jdk.internal.loader
 * @build java.base/* p.Test Main
 * @run main/othervm/native -Xcheck:jni Main
 * @summary Test loading and unloading of native libraries
 */

import jdk.internal.loader.*;
import jdk.internal.loader.NativeLibrariesTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String... args) throws Exception {
        setup();

        NativeLibrariesTest test = new NativeLibrariesTest();
        test.runTest();

        try {
            System.loadLibrary(NativeLibrariesTest.LIB_NAME);
        } catch (UnsatisfiedLinkError e) { e.printStackTrace(); }

        // unload the native library and then System::loadLibrary should succeed
        test.unload();
        System.loadLibrary(NativeLibrariesTest.LIB_NAME);

        // expect NativeLibraries to fail since the library has been loaded by System::loadLibrary
        try {
            test.load(false);
        } catch (UnsatisfiedLinkError e) { e.printStackTrace(); }
    }
    /*
     * move p/Test.class out from classpath to the scratch directory
     */
    static void setup() throws IOException {
        String dir = System.getProperty("test.classes", ".");
        Path p = Files.createDirectories(Paths.get("classes").resolve("p"));
        Files.move(Paths.get(dir, "p", "Test.class"), p.resolve("Test.class"));
    }

}
