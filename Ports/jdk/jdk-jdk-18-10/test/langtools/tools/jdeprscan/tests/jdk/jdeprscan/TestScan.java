/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8145464 8164837 8165646
 * @summary Basic test of jdeprscan's scanning phase.
 * @modules jdk.jdeps/com.sun.tools.jdeprscan
 * @library ../../../cases
 * @library ../../../usage
 * @build jdk.deprcases.members.* jdk.deprcases.types.*
 * @build jdk.deprusage.*
 * @build jdk.jdeprscan.TestScan
 * @run testng jdk.jdeprscan.TestScan
 */

package jdk.jdeprscan;

import com.sun.tools.jdeprscan.Main;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.testng.Assert;

import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;


public class TestScan {
    Set<String> loadExpected() throws IOException {
        Path expFile = Paths.get(System.getProperty("test.src"), "TestScanExpected.txt");
        return new HashSet<>(Files.readAllLines(expFile, StandardCharsets.UTF_8));
    }

    @Test
    public void testScanAgainstReferenceFile() throws IOException {
        String[] testClassPath = System.getProperty("test.class.path", "")
                .split(File.pathSeparator);
        String deprcases = Stream.of(testClassPath)
                .filter(e -> e.endsWith("cases"))
                .findAny()
                .orElseThrow(() -> new InternalError("cases not found"));
        String deprusage = Stream.of(testClassPath)
                .filter(e -> e.endsWith("usage"))
                .findAny()
                .orElseThrow(() -> new InternalError("usage not found"));

        Set<String> expected = loadExpected();
        System.out.println("expected = " + expected);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(baos, false, "UTF-8")) {
            boolean r = Main.call(out, System.err,
                "--class-path", deprcases, "--Xload-dir", deprcases, deprusage);
            assertTrue(r);
        }
        byte[] bytes = baos.toByteArray();

        System.out.println("--- output ---");
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        bais.transferTo(System.out);
        System.out.println("--- end output ---");

        Set<String> actual =
            new BufferedReader(
                new InputStreamReader(
                    new ByteArrayInputStream(bytes), StandardCharsets.UTF_8))
                        .lines()
                        .filter(line -> !line.endsWith(":"))
                        .map(line -> line.split(" +"))
                        .map(array -> array[1])
                        .collect(Collectors.toSet());
        System.out.println("actual = " + actual);

        Set<String> diff1 = new HashSet<>(expected);
        diff1.removeAll(actual);
        Set<String> diff2 = new HashSet<>(actual);
        diff2.removeAll(expected);

        if (diff1.size() > 0 || diff2.size() > 0) {
            System.out.println("missing items: " + diff1);
            System.out.println("extra items: " + diff2);
        }

        Assert.assertEquals(diff1.size(), 0);
        Assert.assertEquals(diff2.size(), 0);
    }
}
