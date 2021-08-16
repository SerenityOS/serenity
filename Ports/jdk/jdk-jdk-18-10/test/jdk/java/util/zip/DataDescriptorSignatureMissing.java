/*
 * Copyright 2012 Google, Inc.  All Rights Reserved.
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
 * @bug 8056934
 * @summary Check ability to read zip files created by python zipfile
 * implementation, which fails to write optional (but recommended) data
 * descriptor signatures.  Repro scenario is a Java -> Python -> Java round trip:
 * - ZipOutputStream creates zip file with DEFLATED entries and data
 *   descriptors with optional signature "PK0x0708".
 * - Python reads those entries, preserving the 0x08 flag byte
 * - Python outputs those entries with data descriptors lacking the
 *   optional signature.
 * - ZipInputStream cannot handle the missing signature
 *
 * No way to adapt the technique in this test to get a ZIP64 zip file
 * without data descriptors was found.
 *
 * @ignore This test has brittle dependencies on an external working python.
 */

import java.io.*;
import java.util.zip.*;

public class DataDescriptorSignatureMissing  {
    void printStream(InputStream is) throws IOException {
        Reader r = new InputStreamReader(is);
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[1024];
        int n;
        while ((n = r.read(buf)) > 0) {
            sb.append(buf, 0, n);
        }
        System.out.print(sb);
    }

    int entryCount(File zipFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(zipFile);
             ZipInputStream zis = new ZipInputStream(fis)) {
            for (int count = 0;; count++)
                if (zis.getNextEntry() == null)
                    return count;
        }
    }

    void test(String[] args) throws Throwable {
        if (! new File("/usr/bin/python").canExecute())
            return;

        // Create a java zip file with DEFLATED entries and data
        // descriptors with signatures.
        final File in = new File("in.zip");
        final File out = new File("out.zip");
        final int count = 3;
        try (FileOutputStream fos = new FileOutputStream(in);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (int i = 0; i < count; i++) {
                ZipEntry ze = new ZipEntry("hello.python" + i);
                ze.setMethod(ZipEntry.DEFLATED);
                zos.putNextEntry(ze);
                zos.write(new byte[10]);
                zos.closeEntry();
            }
        }

        // Copy the zip file using python's zipfile module
        String[] python_program_lines = {
            "import os",
            "import zipfile",
            "input_zip = zipfile.ZipFile('in.zip', mode='r')",
            "output_zip = zipfile.ZipFile('out.zip', mode='w')",
            "count08 = 0",
            "for input_info in input_zip.infolist():",
            "  output_info = input_info",
            "  if output_info.flag_bits & 0x08 == 0x08:",
            "    count08 += 1",
            "  output_zip.writestr(output_info, input_zip.read(input_info))",
            "output_zip.close()",
            "if count08 == 0:",
            "  raise ValueError('Expected to see entries with 0x08 flag_bits set')",
        };
        StringBuilder python_program_builder = new StringBuilder();
        for (String line : python_program_lines)
            python_program_builder.append(line).append('\n');
        String python_program = python_program_builder.toString();
        String[] cmdline = { "/usr/bin/python", "-c", python_program };
        ProcessBuilder pb = new ProcessBuilder(cmdline);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        printStream(p.getInputStream());
        p.waitFor();
        equal(p.exitValue(), 0);

        File pythonZipFile = new File("out.zip");
        check(pythonZipFile.exists());

        equal(entryCount(in),
              entryCount(out));

        // We expect out to be identical to in, except for the removal of
        // the optional data descriptor signatures.
        final int SIG_LENGTH = 4;       // length of a zip signature - PKxx
        equal(in.length(),
              out.length() + SIG_LENGTH * count);

        in.delete();
        out.delete();
    }

    //--------------------- Infrastructure ---------------------------
    volatile int passed = 0, failed = 0;
    void pass() {passed++;}
    void fail() {failed++; Thread.dumpStack();}
    void fail(String msg) {System.err.println(msg); fail();}
    void unexpected(Throwable t) {failed++; t.printStackTrace();}
    void check(boolean cond) {if (cond) pass(); else fail();}
    void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        new DataDescriptorSignatureMissing().instanceMain(args);}
    public void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
