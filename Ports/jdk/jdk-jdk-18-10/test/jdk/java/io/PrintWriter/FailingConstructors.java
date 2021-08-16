/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7000511
 * @summary PrintStream, PrintWriter, Formatter, Scanner leave files open when
 *          exception thrown
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

public class FailingConstructors {
    static final String fileName = "FailingConstructorsTest";
    static final String UNSUPPORTED_CHARSET = "unknownCharset";
    static final String FILE_CONTENTS = "This is a small file!";

    private static void realMain(String[] args) throws Throwable {
        test(false, new File(fileName));

        /* create the file and write its contents */
        File file = File.createTempFile(fileName, null);
        file.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(FILE_CONTENTS.getBytes());
        fos.close();

        test(true, file);
        file.delete();
    }

    private static void test(boolean exists, File file) throws Throwable {
        /* PrintWriter(File file, String csn) */
        try {
            new PrintWriter(file, UNSUPPORTED_CHARSET);
            fail();
        } catch(FileNotFoundException|UnsupportedEncodingException e) {
            pass();
        }

        check(exists, file);

        try {
            new PrintWriter(file, (String)null);
            fail();
        } catch(FileNotFoundException|NullPointerException e) {
            pass();
        }

        check(exists, file);

        /* PrintWriter(String fileName, String csn)  */
        try {
            new PrintWriter(file.getName(), UNSUPPORTED_CHARSET);
            fail();
        } catch(FileNotFoundException|UnsupportedEncodingException e) {
            pass();
        }

        check(exists, file);

        try {
            new PrintWriter(file.getName(), (String)null);
            fail();
        } catch(FileNotFoundException|NullPointerException e) {
            pass();
        }

        check(exists, file);
    }

    private static void check(boolean exists, File file) {
        if (exists) {
            /* the file should be unchanged */
            verifyContents(file);
        } else {
            /* the file should not have been created */
            if (file.exists()) { fail(file + " should not have been created"); }
        }
    }

    private static void verifyContents(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] contents = FILE_CONTENTS.getBytes();
            int read, count = 0;
            while ((read = fis.read()) != -1) {
                if (read != contents[count++])  {
                    fail("file contents have been altered");
                    return;
                }
            }
        } catch (IOException ioe) {
            unexpected(ioe);
        }
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void fail() {failed++; Thread.dumpStack();}
    static void fail(String message) {System.out.println(message); fail(); }
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
