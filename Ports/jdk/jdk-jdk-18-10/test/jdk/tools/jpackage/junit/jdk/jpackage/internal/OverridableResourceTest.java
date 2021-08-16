/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jpackage.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import jdk.jpackage.internal.resources.ResourceLocator;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class OverridableResourceTest {

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testDefault() throws IOException {
        byte[] actualBytes = saveToFile(new OverridableResource(DEFAULT_NAME));

        try (InputStream is = ResourceLocator.class.getResourceAsStream(
                DEFAULT_NAME)) {
            assertArrayEquals(is.readAllBytes(), actualBytes);
        }
    }

    @Test
    public void testDefaultWithSubstitution() throws IOException {
        OverridableResource resource = new OverridableResource(DEFAULT_NAME);

        List<String> linesBeforeSubstitution = convertToStringList(saveToFile(
                resource));

        if (SUBSTITUTION_DATA.size() != 1) {
            // Test setup issue
            throw new IllegalArgumentException(
                    "Substitution map should contain only a single entry");
        }

        resource.setSubstitutionData(SUBSTITUTION_DATA);
        List<String> linesAfterSubstitution = convertToStringList(saveToFile(
                resource));

        assertEquals(linesBeforeSubstitution.size(), linesAfterSubstitution.size());

        Iterator<String> beforeIt = linesBeforeSubstitution.iterator();
        Iterator<String> afterIt = linesAfterSubstitution.iterator();

        var substitutionEntry = SUBSTITUTION_DATA.entrySet().iterator().next();

        boolean linesMismatch = false;
        while (beforeIt.hasNext()) {
            String beforeStr = beforeIt.next();
            String afterStr = afterIt.next();

            if (beforeStr.equals(afterStr)) {
                assertFalse(beforeStr.contains(substitutionEntry.getKey()));
            } else {
                linesMismatch = true;
                assertTrue(beforeStr.contains(substitutionEntry.getKey()));
                assertTrue(afterStr.contains(substitutionEntry.getValue()));
                assertFalse(afterStr.contains(substitutionEntry.getKey()));
            }
        }

        assertTrue(linesMismatch);
    }

    @Test
    public void testCustom() throws IOException {
        testCustom(DEFAULT_NAME);
    }

    @Test
    public void testCustomNoDefault() throws IOException {
        testCustom(null);
    }

    private void testCustom(String defaultName) throws IOException {
        List<String> expectedResourceData = List.of("A", "B", "C");

        Path customFile = createCustomFile("foo", expectedResourceData);

        List<String> actualResourceData = convertToStringList(saveToFile(
                new OverridableResource(defaultName)
                        .setPublicName(customFile.getFileName())
                        .setResourceDir(customFile.getParent())));

        assertArrayEquals(expectedResourceData.toArray(String[]::new),
                actualResourceData.toArray(String[]::new));
    }

    @Test
    public void testCustomtWithSubstitution() throws IOException {
        testCustomtWithSubstitution(DEFAULT_NAME);
    }

    @Test
    public void testCustomtWithSubstitutionNoDefault() throws IOException {
        testCustomtWithSubstitution(null);
    }

    private void testCustomtWithSubstitution(String defaultName) throws IOException {
        final List<String> resourceData = List.of("A", "[BB]", "C", "Foo",
                "GoodbyeHello");
        final Path customFile = createCustomFile("foo", resourceData);

        final Map<String, String> substitutionData = new HashMap(Map.of("B",
                "Bar", "Foo", "B"));
        substitutionData.put("Hello", null);

        final List<String> expectedResourceData = List.of("A", "[BarBar]", "C",
                "B", "Goodbye");

        final List<String> actualResourceData = convertToStringList(saveToFile(
                new OverridableResource(defaultName)
                        .setPublicName(customFile.getFileName())
                        .setSubstitutionData(substitutionData)
                        .setResourceDir(customFile.getParent())));
        assertArrayEquals(expectedResourceData.toArray(String[]::new),
                actualResourceData.toArray(String[]::new));

        // Don't call setPublicName()
        final Path dstFile = tempFolder.newFolder().toPath().resolve(customFile.getFileName());
        new OverridableResource(defaultName)
                .setSubstitutionData(substitutionData)
                .setResourceDir(customFile.getParent())
                .saveToFile(dstFile);
        assertArrayEquals(expectedResourceData.toArray(String[]::new),
                convertToStringList(Files.readAllBytes(dstFile)).toArray(
                        String[]::new));

        // Verify setSubstitutionData() stores a copy of passed in data
        Map<String, String> substitutionData2 = new HashMap(substitutionData);
        var resource = new OverridableResource(defaultName)
                .setResourceDir(customFile.getParent());

        resource.setSubstitutionData(substitutionData2);
        substitutionData2.clear();
        Files.delete(dstFile);
        resource.saveToFile(dstFile);
        assertArrayEquals(expectedResourceData.toArray(String[]::new),
                convertToStringList(Files.readAllBytes(dstFile)).toArray(
                        String[]::new));
    }

    @Test
    public void testNoDefault() throws IOException {
        Path dstFolder = tempFolder.newFolder().toPath();
        Path dstFile = dstFolder.resolve(Path.of("foo", "bar"));

        new OverridableResource(null).saveToFile(dstFile);

        assertFalse(dstFile.toFile().exists());
    }

    private final static String DEFAULT_NAME;
    private final static Map<String, String> SUBSTITUTION_DATA;
    static {
        if (Platform.isWindows()) {
            DEFAULT_NAME = "WinLauncher.template";
            SUBSTITUTION_DATA = Map.of("COMPANY_NAME", "Foo9090345");
        } else if (Platform.isLinux()) {
            DEFAULT_NAME = "template.control";
            SUBSTITUTION_DATA = Map.of("APPLICATION_PACKAGE", "Package1967");
        } else if (Platform.isMac()) {
            DEFAULT_NAME = "Info-lite.plist.template";
            SUBSTITUTION_DATA = Map.of("DEPLOY_BUNDLE_IDENTIFIER", "12345");
        } else {
            throw Platform.throwUnknownPlatformError();
        }
    }

    private byte[] saveToFile(OverridableResource resource) throws IOException {
        Path dstFile = tempFolder.newFile().toPath();
        resource.saveToFile(dstFile);
        assertThat(0, is(not(dstFile.toFile().length())));

        return Files.readAllBytes(dstFile);
    }

    private Path createCustomFile(String publicName, List<String> data) throws
            IOException {
        Path resourceFolder = tempFolder.newFolder().toPath();
        Path customFile = resourceFolder.resolve(publicName);

        Files.write(customFile, data);

        return customFile;
    }

    private static List<String> convertToStringList(byte[] data) {
        return List.of(new String(data, StandardCharsets.UTF_8).split("\\R"));
    }
}
