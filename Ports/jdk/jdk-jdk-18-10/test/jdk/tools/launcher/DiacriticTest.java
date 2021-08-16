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
 * @bug 8017248
 * @summary Compiler Diacritics Issue
 * @run main DiacriticTest
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.charset.UnmappableCharacterException;
import java.util.ArrayList;
import java.util.HashMap;

public class DiacriticTest extends TestHelper {

    // NFD-normalized form of the class name
    static String NAME_NFD="ClassA\u0301";
    // NFC-normalized form of the same class name
    static String NAME_NFC="Class\u00C1";

    public static void main(String[] args) throws IOException {
        if (!isMacOSX) {
            System.out.println("This test is for Mac OS X only. Passing.");
            return;
        }

        String lang = System.getenv("LANG");
        if (lang != null && !lang.contains("UTF-8")) {
            System.out.println("LANG variable set to the language that " +
                               "does not support unicode, test passes vacuously");
            return;
        }

        File sourceFile = new File(NAME_NFC + ".java");
        String source = "public class " + NAME_NFC + " { " +
                "    public static void main(String args[]) {\n" +
                "        System.out.println(\"Success!\");\n" +
                "    }\n" +
                "}\n";
        ArrayList<String> content = new ArrayList<>();
        content.add(source);
        try {
            createFile(sourceFile, content);
        } catch (UnmappableCharacterException | InvalidPathException ipe) {
            System.out.println("The locale or file system is configured in a way " +
                               "that prevents file creation. Real testing impossible.");
            return;
        }

        HashMap<String, String> env = new HashMap<>();
        env.put("LC_CTYPE", "UTF-8");

        TestResult tr;
        tr = doExec(env, javacCmd, NAME_NFD + ".java");
        System.out.println(tr.testOutput);
        if (!tr.isOK()) {
            System.out.println(tr);
            throw new RuntimeException("Compilation failed");
        }
        tr = doExec(env, javaCmd, "-cp", ".", NAME_NFD);
        System.out.println(tr.testOutput);
        if (!tr.isOK()) {
            System.out.println(tr);
            throw new RuntimeException("Test execution failed");
        }
    }
}
