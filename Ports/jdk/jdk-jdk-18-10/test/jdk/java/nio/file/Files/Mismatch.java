/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/* @test
 * @bug 8202285
 * @build Mismatch
 * @run testng Mismatch
 * @summary Unit test for the Files.mismatch method.
 */
public class Mismatch {
    // the standard buffer size
    final static int BUFFER_SIZE = 8192;

    private static final int MISMATCH_NO = -1;

    // Map to be used for creating a ZIP archive
    private static final Map<String, String> ZIPFS_MAP = Map.of("create", "true");

    // temporary test directory where all test files will be created
    Path testDir;

    @BeforeClass
    void setup() throws IOException {
        testDir = Files.createTempDirectory("testMismatch");
    }

    @AfterClass
    void cleanup() throws IOException {
        // clean up files created under the test directory
        Files.walk(testDir).map(Path::toFile).forEach(File::delete);
        Files.deleteIfExists(testDir);
    }

    /*
     * DataProvider for mismatch test. Provides the following fields:
     * path1 -- the path to a file
     * path2 -- the path to another file
     * expected -- expected result of the mismatch method
     * note -- a note about the test
     */
    @DataProvider(name = "testMismatch")
    public Object[][] getDataForMismatch() throws IOException {
        // an non-existent file
        Path foo = Paths.get("nonexistentfile");

        /**
         * File path naming convention:
         * "test" + file size + [abm] [+ position of a modified char] + [ab]
         * where:
         * a or b -- is used to differentiate two files of the same size.
         * m -- indicates the file is modified at the position specified after it
         */

        // create empty files
        int size = 0;
        Path test0a = createASCIIFile(testDir, "test0a", 0, -1, ' ');
        Path test0b = createASCIIFile(testDir, "test0b", 0, -1, ' ');

        /**
         * Since the Impl uses a standard buffer of 8192, the test files are created
         * with sizes <= and > 8192, either multiples of the buffer size, or random.
         * The files are then altered at the begining (0), end (size), and a random
         * position.
         */
        size = 147;
        Path test147a = createASCIIFile(testDir, "test147a", size, -1, ' ');
        Path test147b = createASCIIFile(testDir, "test147b", size, -1, ' ');
        Path test147m0 = createASCIIFile(testDir, "test147m0", size, 0, '!');
        Path test147m70 = createASCIIFile(testDir, "test147m70", size, 70, '@');
        Path test147m146 = createASCIIFile(testDir, "test147m146", size, size - 1, '$');

        size = 1024;
        Path test1024a = createASCIIFile(testDir, "test1024a", size, -1, ' ');
        Path test1024b = createASCIIFile(testDir, "test1024b", size, -1, ' ');
        Path test1024m512 = createASCIIFile(testDir, "test1024m512", size, size >> 1, '@');
        Path test1024m1023 = createASCIIFile(testDir, "test1024m1023", size, size - 1, '$');

        size = BUFFER_SIZE;
        Path test8192a = createASCIIFile(testDir, "test8192a", size, -1, ' ');
        Path test8192b = createASCIIFile(testDir, "test8192b", size, -1, ' ');
        Path test8192m4096 = createASCIIFile(testDir, "test8192m4096", size, size >> 1, '%');
        Path test8192m8191 = createASCIIFile(testDir, "test8192m8191", size, size - 1, '$');


        // create files with size several times > BUFFER_SIZE to be used for tests that verify
        // the situations where they are read into full buffers a few times
        size = BUFFER_SIZE << 3;
        Path test65536a = createASCIIFile(testDir, "test65536a", size, -1, ' ');
        Path test65536b = createASCIIFile(testDir, "test65536b", size, -1, ' ');
        Path test65536m0 = createASCIIFile(testDir, "test65536m0", size, 0, '!');
        Path test65536m32768 = createASCIIFile(testDir, "test65536m32768", size, size >> 1, '%');
        Path test65536m65535 = createASCIIFile(testDir, "test65536m65535", size, size - 1, '$');

        // create files with sizes that will be iterated several times with full buffers, and
        // then a partial one at the last
        size = 70025;
        Path test70025a = createASCIIFile(testDir, "test70025a", size, -1, ' ');
        Path test70025b = createASCIIFile(testDir, "test70025b", size, -1, ' ');
        Path test70025m8400 = createASCIIFile(testDir, "test70025m8400", size, 8400, '@');
        Path test70025m35000 = createASCIIFile(testDir, "test70025m35000", size, 35000, '%');
        Path test70025m70024 = createASCIIFile(testDir, "test70025m70024", size, 70024, '$');

        // create larger files with >= 1048576. The mismatching will be similar. These are just
        // tests to exercise the process with larger files
        size = 1048576;
        Path test1048576a = createASCIIFile(testDir, "test1048576a", size, -1, ' ');

        size = 1065000;
        Path test1065000m532500 = createASCIIFile(testDir, "test1065000m532500", size, size >> 1, '%');
        Path test1065000m1064999 = createASCIIFile(testDir, "test1065000m1064999", size, 1064999, '$');

        return new Object[][]{
            // Spec Case 1: the two paths locate the same file , even if one does not exist
            {foo, foo, MISMATCH_NO, "Same file, no mismatch"},
            {test1024a, test1024a, MISMATCH_NO, "Same file, no mismatch"},

            // Spec Case 2:  The two files are the same size, and every byte in the first file
            // is identical to the corresponding byte in the second file.
            {test0a, test0b, MISMATCH_NO, "Sizes == 0, no mismatch"},
            {test147a, test147b, MISMATCH_NO, "size = 147 < buffer = 8192, no mismatch"},
            {test1024a, test1024b, MISMATCH_NO, "size = 1024 < buffer = 8192, no mismatch"},
            {test8192a, test8192b, MISMATCH_NO, "size = 8192 = buffer = 8192, no mismatch"},
            {test65536a, test65536b, MISMATCH_NO, "read 8 * full buffer, no mismatch"},
            {test70025a, test70025b, MISMATCH_NO, "read 8 * full buffer plus a partial buffer, no mismatch"},


            /**
             * Spec Case 3: the value returned is the position of the first mismatched byte
             * Impl: the impl uses a buffer 8192. The testcases below covers a range of files
             * with sizes <= and > the buffer size. The last buffer is either full or partially full.
            */

            // edge case, one of the file sizes is zero
            // also covers Spec Case 4 and 6
            {test147a, test147m0, 0, "mismatch = 0 (at the beginning)"},
            {test65536m0, test65536a, 0, "mismatch = 0 (at the beginning)"},

            /**
             * Compares files of equal sizes
            */
            // small files
            {test147a, test147m70, 70, "read one partial buffer, mismatch = 70"},
            {test147a, test147m146, 146, "read one partial buffer, mismatch = 146 (end)"},
            {test1024a, test1024m512, 512, "read one partial buffer, mismatch = 512"},
            {test1024a, test1024m1023, 1023, "read one partial buffer, mismatch = 1023 (end)"},

            // file size >= Impl's Buffer Size
            {test8192a, test8192m4096, 4096, "read one buffer, mismatch = 4096 "},
            {test8192a, test8192m8191, 8191, "read one buffer, mismatch = 8191 (at the end)"},

            // file size = n * Impl's Buffer Size
            {test65536a, test65536m32768, 32768, "read through half of the file, mismatch = 32768"},
            {test65536a, test65536m65535, 65535, "read through the whole file, mismatch = 65535 (at the end)"},

            // file size = n * Impl's Buffer Size + x
            {test70025a, test70025m8400, 8400, "mismatch in the 2nd buffer, mismatch = 8400"},
            {test70025a, test70025m35000, 35000, "read about half of the file, mismatch = 35000"},
            {test70025a, test70025m70024, 70024, "read through the whole file, mismatch = 70024 (at the end)"},

            /**
             * Compares files of unequal sizes
            */
            {test8192m8191, test70025m35000, 8191, "mismatch at the end of the 1st file/buffer, mismatch = 8191"},
            {test65536m32768, test70025m8400, 8400, "mismatch in the 2nd buffer, mismatch = 8400"},
            {test70025m70024, test1065000m532500, 70024, "mismatch at the end of the 1st file, mismatch = 70024"},

            /**
             * Spec Case 4:  returns the size of the smaller file (in bytes) when the files are
             * different sizes and every byte of the smaller file is identical to the corresponding
             * byte of the larger file.
             * Impl: similar to case 3, covers a range of file sizes
            */
            {test147a, test1024a, 147, "mismatch is the length of the smaller file: 147"},
            {test1024a, test8192a, 1024, "mismatch is the length of the smaller file: 1024"},
            {test1024a, test65536a, 1024, "mismatch is the length of the smaller file: 1024"},
            {test8192a, test65536a, 8192, "mismatch is the length of the smaller file: 8192"},
            {test70025a, test65536a, 65536, "mismatch is the length of the smaller file: 65536"},
            {test1048576a, test1065000m1064999, 1048576, "mismatch is the length of the smaller file: 1048576"},

            // Spec Case 5: This method is always reflexive (for Path f , mismatch(f,f) returns -1L)
            // See tests for Spec Case 1.

            // Spec Case 6: If the file system and files remain static, then this method is symmetric
            // (for two Paths f and g, mismatch(f,g) will return the same value as mismatch(g,f)).
            // The following tests are selected from tests for Spec Case 3 with the order of
            // file paths switched, the returned values are the same as those for Case 3:
            {test147m70, test147a, 70, "read one partial buffer, mismatch = 70"},
            {test147m146, test147a, 146, "read one partial buffer, mismatch = 146 (end)"},
            {test1024m512, test1024a, 512, "read one partial buffer, mismatch = 512"},
            {test1024m1023, test1024a, 1023, "read one partial buffer, mismatch = 1023 (end)"},

            {test70025m35000, test8192m8191, 8191, "mismatch at the end of the 1st file/buffer, mismatch = 8191"},
            {test70025m8400, test65536m32768, 8400, "mismatch in the 2nd buffer, mismatch = 8400"},
            {test1065000m532500, test70025m70024, 70024, "mismatch at the end of the 1st file, mismatch = 70024"},
        };
    }

    /*
     * DataProvider for mismatch tests involving ZipFS using a few test cases selected
     * from those of the original mismatch tests.
     */
    @DataProvider(name = "testMismatchZipfs")
    public Object[][] getDataForMismatchZipfs() throws IOException {
        Path test1200 = createASCIIFile(testDir, "test1200", 1200, -1, ' ');
        Path test9500 = createASCIIFile(testDir, "test9500", 9500, -1, ' ');
        Path test9500m4200 = createASCIIFile(testDir, "test9500m4200", 9500, 4200, '!');
        Path test80025 = createASCIIFile(testDir, "test80025", 80025, -1, ' ');
        Path test1028500 = createASCIIFile(testDir, "test1028500", 1028500, -1, ' ');
        return new Object[][]{
            {test1200, test1200, MISMATCH_NO, "Compares the file and its copy in zip, no mismatch"},
            {test9500, test9500m4200, 4200,
                "Compares a copy of test9500m4200 in zip with test9500, shall return 4200"},
            {test80025, test1028500, 80025, "mismatch is the length of the smaller file: 80025"},
        };
    }

    /*
     * DataProvider for verifying null handling.
     */
    @DataProvider(name = "testFileNull")
    public Object[][] getDataForNull() throws IOException {
        Path test = createASCIIFile(testDir, "testNonNull", 2200, -1, ' ');
        return new Object[][]{
            {(Path)null, (Path)null},
            {(Path)null, test},
            {test, (Path)null},
        };
    }

    /*
     * DataProvider for verifying how the mismatch method handles the situation
     * when one or both files do not exist.
     */
    @DataProvider(name = "testFileNotExist")
    public Object[][] getDataForFileNotExist() throws IOException {
        Path test = createASCIIFile(testDir, "testFileNotExist", 3200, -1, ' ');
        return new Object[][]{
            {Paths.get("foo"), Paths.get("bar")},
            {Paths.get("foo"), test},
            {test, Paths.get("bar")},
        };
    }

    /**
     * Tests the mismatch method. Refer to the dataProvider testMismatch for more
     * details about the cases.
     * @param path the path to a file
     * @param path2 the path to another file
     * @param expected the expected result
     * @param msg the message about the test
     * @throws IOException if the test fails
     */
    @Test(dataProvider = "testMismatch", priority = 0)
    public void testMismatch(Path path, Path path2, long expected, String msg)
        throws IOException {
        Assert.assertEquals(Files.mismatch(path, path2), expected, msg);
    }

    /**
     * Tests the mismatch method by comparing files with those in a ZIP file.
     * @param path the path to a file
     * @param path2 the path to another file to be added into a ZIP file
     * @param expected the expected result
     * @param msg the message about the test
     * @throws IOException if the test fails
     */
    @Test(dataProvider = "testMismatchZipfs", priority = 1)
    public void testMismatchZipfs(Path path, Path path2, long expected, String msg)
        throws IOException {
        Path zipPath = Paths.get(testDir.toString(), "TestWithFSZip.zip");
        try (FileSystem fs = getZipFSProvider().newFileSystem(zipPath, ZIPFS_MAP)) {
            Path copy = fs.getPath(path.getFileName().toString());
            Files.copy(path, copy, REPLACE_EXISTING);

            if (path2 == null) {
                Assert.assertEquals(Files.mismatch(copy, path), expected, msg);
            } else {
                Assert.assertEquals(Files.mismatch(copy, path2), expected, msg);
            }
        }
    }

    /**
     * Verifies that NullPointerException is thrown when one or both files are null.
     * @param path the path to a file
     * @param path2 the path to another file
     * @throws NullPointerException as expected
     */
    @Test(dataProvider = "testFileNull", priority = 2, expectedExceptions = NullPointerException.class)
    public void testMismatchNull(Path path, Path path2) throws Exception {
        long result = Files.mismatch(path, path2);
    }

    /**
     * Verifies that IOException is thrown when one or both files do not exist.
     * @param path the path to a file
     * @param path2 the path to another file
     * @throws IOException as expected
     */
    @Test(dataProvider = "testFileNotExist", priority = 2, expectedExceptions = IOException.class)
    public void testMismatchNotExist(Path path, Path path2) throws IOException {
        long result = Files.mismatch(path, path2);
    }

    /**
     * Creates a file with ASCII content with one character altered
     * at the specified position.
     *
     * Note: Files.mismatch method does a byte-by-byte comparison. ASCII files
     * are sufficient for verifying the feature.
     *
     * @param dir the directory in which the file is to be created
     * @param purpose the purpose of the file
     * @param size the size of the file
     * @param pos the position where the alternative char is to be added. If it
     *            is smaller than zero, no alternation shall be made.
     * @param c the character
     * @return path of the created file
     * @throws IOException
     */
    private static Path createASCIIFile(Path dir, String purpose, int size, int pos,
                                        char c) throws IOException {
        Path path = Files.createFile(Paths.get(dir.toString(), purpose + ".txt"));
        if (size > 0) {
            writeASCIIFile(path, size, pos, c);
        }
        return path;
    }

    private static void writeASCIIFile(Path path, int size, int pos, char c)
        throws IOException {
        byte[] a = createASCIIArray(size);
        if (pos >= 0) a[pos] = (byte)(c & 0xFF); // US_ASCII char only, may cast directly
        Files.write(path, a);
    }

    private static byte[] createASCIIArray(int length) {
        byte[] bytes = "ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz 0123456789 \n"
            .getBytes(StandardCharsets.US_ASCII);
        byte[] a = new byte[length];
        fillArray(bytes, a);
        return a;
    }

    private static FileSystemProvider getZipFSProvider() {
        for (FileSystemProvider provider : FileSystemProvider.installedProviders()) {
            if ("jar".equals(provider.getScheme())) {
                return provider;
            }
        }
        return null;
    }

    /**
     * Fills the destination array by copying the source array repeatedly until
     * it is completely filled.
     *
     * @param src the source array
     * @param dest the destination array
     */
    public static void fillArray(byte[] src, byte[] dest) {
        int bLen = src.length;
        int space = dest.length;
        int iteration = 0;

        while (space > 0) {
            if (space >= bLen) {
                System.arraycopy(src, 0, dest, iteration++ * bLen, bLen);
                space -= bLen;
            } else {
                System.arraycopy(src, 0, dest, iteration++ * bLen, space);
                break;
            }
        }
    }
}
