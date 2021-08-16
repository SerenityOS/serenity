/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import jdk.test.lib.Asserts;

/**
 * @test
 * @bug 8141690
 * @summary MakeJavaSecurity.java functions
 * @library /test/lib /test/jdk
 * @run main MakeJavaSecurityTest
 */
public class MakeJavaSecurityTest {

    private static final String TEST_SRC = System.getProperty("test.src", ".");

    public static void main(String[] args) throws Exception {
        Path toolPath = getMakeJavaSecPath();

        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                toolPath.toString(),
                TEST_SRC + "/raw_java_security",
                "outfile",
                "solaris",
                "sparc",
                "somepolicy",
                TEST_SRC + "/more_restricted");

        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);

        verifyOutputFile();
    }

    private static Path getMakeJavaSecPath() {
        String testRoot = System.getProperty("test.root", ".");
        Path toolPath = Paths.get(testRoot).getParent().getParent();
        toolPath = toolPath.resolve("make/jdk/src/classes/build/tools" +
                "/makejavasecurity/MakeJavaSecurity.java");

        Asserts.assertTrue(Files.exists(toolPath),
                String.format("Cannot find %s. Maybe not all code repos are available",
                        toolPath));
        return toolPath;
    }

    private static void verifyOutputFile() throws IOException {
        Path actualFile = Path.of("./outfile");
        Path expectedFile = Path.of(TEST_SRC + "/final_java_security");

        List<String> list1 = Files.readAllLines(actualFile);
        List<String> list2 = Files.readAllLines(expectedFile);
        list1 = removeEmptyLines(list1);
        list2 = removeEmptyLines(list2);

        String errorMessage = String.format("""
                Expected file content:
                 %s\s
                not equal to actual:
                 %s\s
                """, list2, list1);

        Asserts.assertTrue(list1.equals(list2), errorMessage);
    }

    private static List<String> removeEmptyLines(List<String> list) {
        return list.stream()
                .filter(item -> !item.isBlank())
                .collect(Collectors.toList());
    }
}
