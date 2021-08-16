/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8143366
 * @summary Check that control chars are displayed correctly
 * @modules jdk.jdeps/com.sun.tools.javap
 * @run testng ControlCharTest
 */

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.regex.Pattern;

public class ControlCharTest {
    private String classpath;
    private Path output;
    private Pattern ptrn;

    @BeforeClass
    public void initialize() throws Exception {
        String testClasses = System.getProperty("test.classes", ".");
        classpath = "-classpath " + testClasses + " ControlCharTest$Strings";
        String userdir = System.getProperty("user.dir", ".");
        output = Paths.get(userdir, "output.txt");
        String regex = Pattern.quote("\\u0001\\u0002\\u0003")  // \u0001\u0002\u0003
                + ".+123.+"                   // 123
                + Pattern.quote("\\\\u0000")  // \\u0000
                + ".+"
                + Pattern.quote("\\u0000")    // \u0000
                ;
        ptrn = Pattern.compile(regex, Pattern.DOTALL);
    }

    @AfterClass
    public void close() throws IOException {
        Files.deleteIfExists(output);
    }

    @DataProvider(name = "options")
    public Object[][] createData() {
        return new Object[][] {
                { "-v", ""},
                { "-constants", ""}
        };
    }
    @Test(dataProvider = "options")
    public void test(String option, String ignore) throws Exception {
        String cmdline = option + " " + classpath;
        javap(cmdline.split(" +"));
        try (Scanner scnr = new Scanner(output)) {
            Assert.assertNotNull(scnr.findWithinHorizon(ptrn, 0));
        }
    }

    private void javap(String... args) throws Exception {
        try (PrintWriter out = new PrintWriter(output.toFile())) {
            int rc = com.sun.tools.javap.Main.run(args, out);
            if (rc < 0)
                throw new Exception("javap exited, rc=" + rc);
        }
    }

    // small class to test
    static class Strings {
        static final String s = "\1\2\3";
        static final String s1 = "123";
        static final String s2 = "\\u0000";
        static final String s3 = "\0";
        static String f() { return s + s1 + s2 + s3; }
    }
}


