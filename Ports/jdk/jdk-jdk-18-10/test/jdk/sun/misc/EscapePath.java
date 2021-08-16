/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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
/* @test
 * @bug 4359123
 * @summary  Test loading of classes with # in the path
 */
import java.io.*;

public class EscapePath {

    private static String testPath;

    static {
        testPath = System.getProperty("test.src");
        if (testPath == null)
            testPath = "";
        else
            testPath = testPath + File.separator;
    }

    public static void main(String[] args) throws Exception {
        createTestDir();
        copyClassFile();
        invokeJava();
        eraseTestDir();
    }

    private static void createTestDir() throws Exception {
        File testDir = new File("a#b");
        boolean result = testDir.mkdir();
    }

    private static void eraseTestDir() throws Exception {
        File classFile = new File("a#b/Hello.class");
        classFile.delete();
        File testDir = new File("a#b");
        testDir.delete();
    }

    private static void copyClassFile() throws Exception {
        FileInputStream fis = new FileInputStream(testPath + "Hello.class");
        FileOutputStream fos = new FileOutputStream("a#b/Hello.class");

        int bytesRead;
        byte buf[] = new byte[410];
        do {
            bytesRead = fis.read(buf);
            if (bytesRead > 0)
                fos.write(buf, 0, bytesRead);
        } while (bytesRead != -1);
        fis.close();
        fos.flush();
        fos.close();
    }

    private static void invokeJava() throws Exception {
        String command = System.getProperty("java.home") +
                         File.separator + "bin" + File.separator +
                         "java -classpath " + "a#b/ Hello";
        Process p = Runtime.getRuntime().exec(command);
        p.waitFor();
        int result = p.exitValue();
        if (result != 0)
            throw new RuntimeException("Path encoding failure.");
    }
}
