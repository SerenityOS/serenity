/*
 * Copyright (c) 2020, Red Hat Inc.
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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jdk.internal.platform.CgroupSubsystemController;
import jdk.test.lib.Utils;
import jdk.test.lib.util.FileUtils;

/*
 * @test
 * @key cgroups
 * @requires os.family == "linux"
 * @modules java.base/jdk.internal.platform
 * @library /test/lib
 * @run junit/othervm TestCgroupSubsystemController
 */

/**
 *
 * Basic unit test for CgroupSubsystemController
 *
 */
public class TestCgroupSubsystemController {

    private static final double DELTA = 0.01;
    private Path existingDirectory;
    private Path existingFile;
    private String existingFileName = "test-controller-file";
    private String existingFileContents = "foobar";
    private String doubleValueContents = "1.5";
    private String longValueContents = "3000000000";
    private String longValueMatchingLineContents = "testme\n" +
                                                   "itemfoo 25";
    private String longEntryContents = "s 1\n" +
                                       "t 2";
    private String longEntryName = "longEntry";
    private String longEntryMatchingLineName = "longMatchingLine";
    private String doubleValueName = "doubleValue";
    private String longValueName = "longValue";
    private CgroupSubsystemController mockController;

    @Before
    public void setup() {
        try {
            existingDirectory = Utils.createTempDirectory(TestCgroupSubsystemController.class.getSimpleName());
            existingFile = Paths.get(existingDirectory.toString(), existingFileName);
            Files.writeString(existingFile, existingFileContents, StandardCharsets.UTF_8);
            Path longFile = Paths.get(existingDirectory.toString(), longValueName);
            Files.writeString(longFile, longValueContents);
            Path doubleFile = Paths.get(existingDirectory.toString(), doubleValueName);
            Files.writeString(doubleFile, doubleValueContents);
            Path longEntryFile = Paths.get(existingDirectory.toString(), longEntryName);
            Files.writeString(longEntryFile, longEntryContents);
            Path longMatchingLine = Paths.get(existingDirectory.toString(), longEntryMatchingLineName);
            Files.writeString(longMatchingLine, longValueMatchingLineContents);
            mockController = new MockCgroupSubsystemController(existingDirectory.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @After
    public void teardown() {
        try {
            FileUtils.deleteFileTreeWithRetry(existingDirectory);
        } catch (IOException e) {
            System.err.println("Teardown failed. " + e.getMessage());
        }
    }

    @Test
    public void getStringValueNullController() {
        String val = CgroupSubsystemController.getStringValue(null, "ignore");
        assertNull(val);
    }

    @Test
    public void getStringValueIOException() throws IOException {
        String val = CgroupSubsystemController.getStringValue(mockController, "don-t-exist.txt");
        assertNull(val);
    }

    @Test
    public void getStringValueSuccess() {
        String actual = CgroupSubsystemController.getStringValue(mockController, existingFileName);
        assertEquals(existingFileContents, actual);
    }

    @Test
    public void convertStringToLong() {
        String strVal = "1230";
        long longVal = Long.parseLong(strVal);
        long actual = CgroupSubsystemController.convertStringToLong(strVal, -1L, 0);
        assertEquals(longVal, actual);

        String overflowVal = "9223372036854775808"; // Long.MAX_VALUE + 1
        long overflowDefault = -1;
        actual = CgroupSubsystemController.convertStringToLong(overflowVal, overflowDefault, 0);
        assertEquals(overflowDefault, actual);
        overflowDefault = Long.MAX_VALUE;
        actual = CgroupSubsystemController.convertStringToLong(overflowVal, overflowDefault, 0);
        assertEquals(overflowDefault, actual);
    }

    @Test
    public void convertStringRangeToIntArray() {
        assertNull(CgroupSubsystemController.stringRangeToIntArray(null));
        assertNull(CgroupSubsystemController.stringRangeToIntArray(""));
        String strRange = "2,4,6";
        int[] actual = CgroupSubsystemController.stringRangeToIntArray(strRange);
        int[] expected = new int[] { 2, 4, 6 };
        assertTrue(Arrays.equals(expected, actual));
        strRange = "6,1-3";
        actual = CgroupSubsystemController.stringRangeToIntArray(strRange);
        expected = new int[] { 1, 2, 3, 6 };
        assertTrue(Arrays.equals(expected, actual));
    }

    @Test
    public void getDoubleValue() {
        double defaultValue = -3;
        double actual = CgroupSubsystemController.getDoubleValue(null, null, defaultValue);
        assertEquals(defaultValue, actual, DELTA);
        double expected = Double.parseDouble(doubleValueContents);
        actual = CgroupSubsystemController.getDoubleValue(mockController, doubleValueName, defaultValue);
        assertEquals(expected, actual, DELTA);
        actual = CgroupSubsystemController.getDoubleValue(mockController, "don't-exist", defaultValue);
        assertEquals(defaultValue, actual, DELTA);
    }

    @Test
    public void getLongValue() {
        long defaultValue = -4;
        long actual = CgroupSubsystemController.getLongValue(null, null, Long::parseLong, defaultValue);
        assertEquals(defaultValue, actual);
        actual = CgroupSubsystemController.getLongValue(mockController, "dont-exist", Long::parseLong, defaultValue);
        assertEquals(defaultValue, actual);
        long expected = Long.parseLong(longValueContents);
        actual = CgroupSubsystemController.getLongValue(mockController, longValueName, Long::parseLong, defaultValue);
        assertEquals(expected, actual);
    }

    @Test
    public void getLongEntry() {
        long defaultValue = -5;
        long actual = CgroupSubsystemController.getLongEntry(null, null, "no-matter", defaultValue);
        assertEquals(defaultValue, actual);
        actual = CgroupSubsystemController.getLongEntry(mockController, "dont-exist", "foo-bar", defaultValue);
        assertEquals(defaultValue, actual);
        actual = CgroupSubsystemController.getLongEntry(mockController, longEntryName, "t", defaultValue);
        assertEquals(2, actual);
    }

    @Test
    public void getLongMatchingLine() {
        long defaultValue = -6;
        long actual = CgroupSubsystemController.getLongValueMatchingLine(null, null, "no-matter", Long::parseLong, defaultValue);
        assertEquals(defaultValue, actual);
        actual = CgroupSubsystemController.getLongValueMatchingLine(mockController, "dont-exist", "no-matter", Long::parseLong, defaultValue);
        assertEquals(defaultValue, actual);
        actual = CgroupSubsystemController.getLongValueMatchingLine(mockController, longEntryMatchingLineName, "item", TestCgroupSubsystemController::convertLong, defaultValue);
        assertEquals(25, actual);
    }

    public static long convertLong(String line) {
        return Long.parseLong(line.split("\\s+")[1]);
    }

    static class MockCgroupSubsystemController implements CgroupSubsystemController {

        private final String path;

        public MockCgroupSubsystemController(String path) {
            this.path = path;
        }

        @Override
        public String path() {
            return path;
        }

    }

}
