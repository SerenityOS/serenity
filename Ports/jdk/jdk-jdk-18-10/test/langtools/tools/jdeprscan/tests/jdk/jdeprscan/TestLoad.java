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
 * @bug 8145464 8164837
 * @summary Test of jdeprscan tool loading and printing to aCSV file.
 * @modules jdk.jdeps/com.sun.tools.jdeprscan
 * @library ../../../cases
 * @build jdk.deprcases.members.* jdk.deprcases.types.*
 * @build jdk.jdeprscan.TestLoad
 * @run testng jdk.jdeprscan.TestLoad
 */

package jdk.jdeprscan;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sun.tools.jdeprscan.Main;

import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;


public class TestLoad {
    static final String UTF8 = "UTF-8";
    static final String EXPECTED = "TestLoadExpected.csv";

    @Test
    public void test1() throws IOException, UnsupportedEncodingException {
        String testClassPath = System.getProperty("test.class.path", "");
        String deprcases = Stream.of(testClassPath.split(File.pathSeparator))
                .filter(e -> e.endsWith("cases"))
                .findAny()
                .orElseThrow(() -> new InternalError("cases not found"));
        boolean rval;

        System.out.println("test.src = " + System.getProperty("test.src"));
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ByteArrayOutputStream berr = new ByteArrayOutputStream();

        try (PrintStream prout = new PrintStream(bout, true, UTF8);
             PrintStream prerr = new PrintStream(berr, true, UTF8)) {
            System.out.println("Calling JDeprScan --Xprint-csv --Xload-dir " + deprcases);
            rval = Main.call(prout, prerr, "--Xprint-csv", "--Xload-dir", deprcases);
            System.out.println("JDeprScan returns " + rval);
        }

        System.out.println("----- stdout");
        new ByteArrayInputStream(bout.toByteArray()).transferTo(System.out);
        System.out.println("----- end stdout");
        System.out.println("----- stderr");
        new ByteArrayInputStream(berr.toByteArray()).transferTo(System.out);
        System.out.println("----- end stderr");

        List<String> actualList;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bout.toByteArray());
             InputStreamReader isr = new InputStreamReader(bais);
             BufferedReader br = new BufferedReader(isr)) {
            actualList = br.lines().collect(Collectors.toList());
        }

        Path expfile = Paths.get(System.getProperty("test.src"), EXPECTED);
        List<String> expectedList = Files.readAllLines(expfile);

        Set<String> actual = new HashSet<>(actualList.subList(1, actualList.size()));
        Set<String> expected = new HashSet<>(expectedList.subList(1, expectedList.size()));

        Set<String> diff1 = new HashSet<>(actual);
        diff1.removeAll(expected);
        Set<String> diff2 = new HashSet<>(expected);
        diff2.removeAll(actual);
        if (diff1.size() > 0) {
            System.out.println("Extra lines in output:");
            diff1.forEach(System.out::println);
        }

        if (diff2.size() > 0) {
            System.out.println("Lines missing from output:");
            diff2.forEach(System.out::println);
        }

        assertTrue(rval);
        assertEquals(berr.toByteArray().length, 0);
        assertEquals(actual.size(), actualList.size() - 1);
        assertEquals(diff1.size(), 0);
        assertEquals(diff2.size(), 0);
    }
}
