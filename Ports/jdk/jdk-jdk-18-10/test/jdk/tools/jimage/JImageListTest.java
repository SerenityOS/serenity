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
 * @summary Tests to verify jimage 'list' action
 * @library /test/lib
 * @modules jdk.jlink/jdk.tools.jimage
 * @build jdk.test.lib.Asserts
 * @run main JImageListTest
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jdk.test.lib.Asserts.assertEquals;
import static jdk.test.lib.Asserts.assertFalse;
import static jdk.test.lib.Asserts.assertTrue;

public class JImageListTest extends JImageCliTest {
    public void testList() {
        jimage("list", getImagePath())
                .assertSuccess()
                .resultChecker(r -> {
                    String[] lines = r.output.split(System.lineSeparator());
                    assertTrue(lines.length > 0, "Option --list has output.");
                    assertTrue(lines[0].startsWith("jimage: " + getImagePath()),
                            "Output should start with jimage path.");

                    List<String> modules = Stream.of(lines)
                            .filter(s -> s.startsWith("Module: "))
                            .map(s -> s.substring(s.indexOf(':') + 1).trim())
                            .collect(Collectors.toList());
                    assertTrue(modules.size() > 0, "Image contains at least one module.");
                    assertTrue(modules.contains("java.base"), "Module java.base found.");
                });
    }

    public void testListHelp() {
        for (String opt : Arrays.asList("-h", "--help")) {
            jimage("list", opt)
                    .assertSuccess()
                    .resultChecker(r -> {
                        // list  -  descriptive text
                        assertMatches("\\s+list\\s+-\\s+.*", r.output);
                    });
        }
    }

    public void testListVerbose() {
        jimage("list", "--verbose", getImagePath())
                .assertSuccess()
                .resultChecker(r -> {
                    assertMatches("Offset\\s+Size\\s+Compressed\\s+Entry", r.output);

                    String[] lines = r.output.split("[" + System.lineSeparator() + "]+");
                    assertTrue(lines.length > 0, "Option --list has output.");
                    assertTrue(lines[0].startsWith("jimage: " + getImagePath()),
                            "Output should start with jimage path.");

                    List<String> modules = Stream.of(lines)
                            .filter(s -> s.startsWith("Module: "))
                            .map(s -> s.substring(s.indexOf(':') + 1).trim())
                            .collect(Collectors.toList());
                    assertTrue(modules.size() > 0, "Image contains at least one module.");
                    assertTrue(modules.contains("java.base"), "Module java.base found.");

                    Set<String> entries = Stream.of(lines)
                            .filter(s -> { return !s.startsWith("Module: ") && !s.startsWith("Offset"); })
                            // Offset \d+  Size \d+  Compressed \d+ Entry \.*
                            .filter(s -> !s.matches("\\s+\\d+\\s+\\d+\\s+\\d+\\s+.*"))
                            .collect(Collectors.toSet());
                    assertEquals(entries, new HashSet<>() {{ add("jimage: " + getImagePath()); }},
                            "All entries should be in format: Offset Size Compressed Entry");
                });
    }

    public void testListIncludeAllWithGlob() {
        JImageResult listAll = jimage("list", getImagePath()).assertSuccess();
        JImageResult listAllGlob = jimage("list", "--include", "**", getImagePath()).assertSuccess();
        assertEquals(listAllGlob.output, listAll.output, "--include ** should produce the same output");
    }

    public void testListIncludeWithGlob() {
        JImageResult listAll = jimage("list", getImagePath()).assertSuccess();
        Set<String> expected = Stream.of(listAll.output.split("[" + System.lineSeparator() + "]+"))
                .map(String::trim)
                .filter(s -> s.startsWith("java/util/zip"))
                .collect(Collectors.toSet());

        JImageResult listJavaUtil = jimage("list", "--include", "/java.base/java/util/zip/**", getImagePath()).assertSuccess();
        Set<String> actual = Stream.of(listJavaUtil.output.split("[" + System.lineSeparator() + "]+"))
                .map(String::trim)
                .filter(s -> !s.startsWith("jimage:") && !s.startsWith("Module:"))
                .collect(Collectors.toSet());
        assertEquals(actual, expected, "All java.util.zip classes are listed");
    }

    public void testListIncludeNoMatchWithGlob() {
        JImageResult listNotMatching = jimage("list", "--include", "not_matching", getImagePath()).assertSuccess();
        Set<String> entries = Stream.of(listNotMatching.output.split("["+ System.lineSeparator() + "]+"))
                .map(String::trim)
                .filter(s -> !s.startsWith("jimage:") && !s.startsWith("Module:"))
                .collect(Collectors.toSet());
        assertEquals(entries, Collections.emptySet(), "No java.util classes are listed");
    }

    public void testListIncludeAllWithExplicitGlob() {
        JImageResult listAll = jimage("list", getImagePath()).assertSuccess();
        JImageResult listAllGlob = jimage("list", "--include", "glob:**", getImagePath()).assertSuccess();
        assertEquals(listAllGlob.output, listAll.output, "--include glob:** should produce the same output");
    }

    public void testListIncludeAllWithRegex() {
        JImageResult listAll = jimage("list", getImagePath()).assertSuccess();
        JImageResult listAllRegex = jimage("list", "--include", "regex:.*", getImagePath()).assertSuccess();
        assertEquals(listAllRegex.output, listAll.output, "--include regex:.* should produce the same output");
    }

    public void testListIncludeWithRegex() {
        JImageResult listAll = jimage("list", getImagePath()).assertSuccess();
        Set<String> expected = Stream.of(listAll.output.split("[" + System.lineSeparator() + "]+"))
                .map(String::trim)
                .filter(s -> s.startsWith("java/text/"))
                .collect(Collectors.toSet());
        assertFalse(expected.isEmpty(), "There should be classes from java.text package");

        JImageResult listJavaText = jimage("list", "--include", "regex:/java.base/java/text/.*", getImagePath()).assertSuccess();
        Set<String> actual = Stream.of(listJavaText.output.split("[" + System.lineSeparator() + "]+"))
                .map(String::trim)
                .filter(s -> !s.startsWith("jimage:") && !s.startsWith("Module:"))
                .collect(Collectors.toSet());

        assertEquals(actual, expected, "All java.text classes are listed");
    }

    public void testListIncludeNoMatchWithRegex() {
        JImageResult listNotMatching = jimage("list", "--include", "regex:not_matching",
                getImagePath()).assertSuccess();
        Set<String> entries = Stream.of(listNotMatching.output.split("[" + System.lineSeparator() + "]+"))
                .map(String::trim)
                .filter(s -> !s.startsWith("jimage:") && !s.startsWith("Module:"))
                .collect(Collectors.toSet());
        assertEquals(entries, Collections.emptySet(), "No classes are listed");
    }

    public void testListIncludeMultiplePatterns() throws IOException {
        JImageResult listAll = jimage("list", getImagePath()).assertSuccess();
        Set<String> expected = Stream.of(listAll.output.split("[" + System.lineSeparator() + "]+"))
                .map(String::trim)
                .filter(s -> s.startsWith("java/time/") || s.startsWith("java/util/zip"))
                .collect(Collectors.toSet());
        assertFalse(expected.isEmpty(), "There should be classes from java.time and java.io packages");

        JImageResult listMatched = jimage("list", "--include", "glob:/java.base/java/time/**,regex:/java.base/java/util/zip/.*",
                getImagePath()).assertSuccess();
        Set<String> actual = Stream.of(listMatched.output.split("[" + System.lineSeparator() + "]+"))
                .map(String::trim)
                .filter(s -> !s.startsWith("jimage:") && !s.startsWith("Module:"))
                .collect(Collectors.toSet());

        assertEquals(actual, expected, "All java.time and java.util.zip classes are listed");
    }

    public void testListNoImageSpecified() {
        jimage("list", "")
                .assertFailure()
                .assertShowsError();
    }

    public void testListEmptyFile() throws IOException {
        Path tmp = Files.createTempFile(Paths.get("."), getClass().getName(), "empty_file");
        jimage("list", tmp.toString())
                .assertFailure()
                .assertShowsError();
    }

    public void testListNotAnImage() throws IOException {
        Path tmp = Files.createTempFile(Paths.get("."), getClass().getName(), "not_an_image");
        Files.write(tmp, "This is not an image".getBytes());
        jimage("list", tmp.toString())
                .assertFailure()
                .assertShowsError();
    }

    public void testListNotExistingImage() throws IOException {
        Path tmp = Paths.get(".", "not_existing_image");
        Files.deleteIfExists(tmp);
        jimage("list", tmp.toString())
                .assertFailure()
                .assertShowsError();
    }

    public void testListWithUnknownOption() {
        jimage("list", "--unknown")
                .assertFailure()
                .assertShowsError();
    }

    public static void main(String[] args) throws Throwable {
        new JImageListTest().runTests();
        // Just to ensure that jimage files will be unmapped
        System.gc();
    }
}

