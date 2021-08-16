/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8168444
 * @summary Test of jdeprscan handling of primitives and primitive arrays.
 * @modules jdk.jdeps/com.sun.tools.jdeprscan
 * @run main jdk.jdeprscan.TestNotFound
 */

package jdk.jdeprscan;

import com.sun.tools.jdeprscan.Main;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;

public class TestNotFound {

    public static void main(String[] args) throws IOException {
        final String SEP = File.separator;
        final String TESTCLASSES = System.getProperty("test.classes");
        final String THISCLASS =
            TESTCLASSES + SEP + "jdk" + SEP + "jdeprscan" + SEP + "TestNotFound.class";

        ByteArrayOutputStream outBaos = new ByteArrayOutputStream();
        ByteArrayOutputStream errBaos = new ByteArrayOutputStream();
        boolean mainResult;

        // Causes 5 Methodrefs to be generated, thus 5 occurrences
        // of the Target class not being found. But only one message
        // should be emitted.

        Target.method1();
        Target.method2();
        Target.method3();
        Target.method4();
        Target.method5();

        try (PrintStream out = new PrintStream(outBaos, false, "UTF-8");
             PrintStream err = new PrintStream(errBaos, false, "UTF-8")) {
            // Call jdeprscan without the proper classpath, so Target isn't found.
            // Result is checked below after output is dumped.
            mainResult = Main.call(out, err, THISCLASS);
        }

        byte[] outBytes = outBaos.toByteArray();
        byte[] errBytes = errBaos.toByteArray();
        ByteArrayInputStream outbais = new ByteArrayInputStream(outBytes);
        ByteArrayInputStream errbais = new ByteArrayInputStream(errBytes);

        System.out.println("--- stdout ---");
        outbais.transferTo(System.out);
        System.out.println("--- end stdout ---");

        System.out.println("--- stderr ---");
        errbais.transferTo(System.out);
        System.out.println("--- end stderr ---");

        long errCount = new Scanner(new String(errBytes, "UTF-8")).findAll("error:").count();

        System.out.println("mainResult = " + mainResult);
        System.out.println("errCount = " + errCount);

        if (!mainResult) {
            System.out.println("FAIL: mainResult should be true");
        }

        if (errCount != 1L) {
            System.out.println("FAIL: errCount should equal 1");
        }

        if (!mainResult || errCount != 1L) {
            throw new AssertionError("Test failed.");
        } else {
            System.out.println("Test passed.");
        }
    }

    static class Target {
        static void method1() { }
        static void method2() { }
        static void method3() { }
        static void method4() { }
        static void method5() { }
    }
}
