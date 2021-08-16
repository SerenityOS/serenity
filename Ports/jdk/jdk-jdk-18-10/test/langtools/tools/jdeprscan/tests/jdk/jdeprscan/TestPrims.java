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
 * @build jdk.jdeprscan.TestPrims
 * @run testng jdk.jdeprscan.TestPrims
 */

package jdk.jdeprscan;

import com.sun.tools.jdeprscan.Main;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.regex.Pattern;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TestPrims {

    @Test
    public void test() throws IOException {
        final String TESTSRC = System.getProperty("test.src");
        final String TESTCLASSPATH = System.getProperty("test.class.path");
        String CSV_FILE = TESTSRC + File.separator + "TestPrims.csv";

        ByteArrayOutputStream outBaos = new ByteArrayOutputStream();
        ByteArrayOutputStream errBaos = new ByteArrayOutputStream();
        boolean mainResult;

        try (PrintStream out = new PrintStream(outBaos, false, "UTF-8");
             PrintStream err = new PrintStream(errBaos, false, "UTF-8")) {
            mainResult = Main.call(
                out, err,
                "--class-path", TESTCLASSPATH,
                "--Xload-csv", CSV_FILE,
                "jdk.jdeprscan.TestPrims$Usage");
            // assertion is checked below after output is dumped
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

        String outString = new String(outBytes, "UTF-8");
        String errString = new String(errBytes, "UTF-8");

        // matches message "class <classname> uses deprecated class [I"
        boolean outMatch = Pattern.compile("^class ").matcher(outString).find();

        // matches message "error: cannot find class [I"
        boolean errMatch = Pattern.compile("^error: ").matcher(errString).find();

        if (!mainResult) {
            System.out.println("FAIL: Main.call returned false");
        }

        if (outMatch) {
            System.out.println("FAIL: stdout contains unexpected error message");
        }

        if (errMatch) {
            System.out.println("FAIL: stderr contains unexpected error message");
        }

        assertTrue(mainResult && !outMatch && !errMatch);
    }

    static class Usage {
        void prims(boolean z, byte b, short s, char c,
                   int i, long j, float f, double d) { }

        void primsArrays(boolean[] z, byte[] b, short[] s, char[] c,
                         int[] i, long[] j, float[] f, double[] d) { }

        boolean zfield;
        byte    bfield;
        short   sfield;
        char    cfield;
        int     ifield;
        long    jfield;
        float   ffield;
        double  dfield;

        boolean[] azfield;
        byte[]    abfield;
        short[]   asfield;
        char[]    acfield;
        int[]     aifield;
        long[]    ajfield;
        float[]   affield;
        double[]  adfield;


        Object[] clones() {
            return new Object[] {
                azfield.clone(),
                abfield.clone(),
                asfield.clone(),
                acfield.clone(),
                aifield.clone(),
                ajfield.clone(),
                affield.clone(),
                adfield.clone()
            };
        }
    }
}
