/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 */
package test;

import org.testng.annotations.Test;
import util.ZipFsBaseTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.zip.ZipEntry;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.*;
import static org.testng.Assert.*;

/**
 * @test
 * @bug 8242006
 * @summary Improve FileChannel and SeekableByteChannel Zip FS test coverage
 * @modules jdk.zipfs
 * @run testng test.ChannelTests
 */
public class ChannelTests extends ZipFsBaseTest {

    // Size of the ByteBuffer to use for reading/writing
    public static final int BYTEBUFFER_SIZE = 8192;
    // Values used to create the entries to be copied into/from a Zip file
    private static final String GRAND_SLAMS_HEADER = "The Grand Slams Are:"
            + System.lineSeparator();
    private static final String AUSTRALIAN_OPEN = "Australian Open"
            + System.lineSeparator();
    private static final String FRENCH_OPEN = "French Open" + System.lineSeparator();
    private static final String WIMBLEDON = "Wimbledon" + System.lineSeparator();
    private static final String US_OPEN = "U.S. Open" + System.lineSeparator();
    private static final String GRAND_SLAMS = AUSTRALIAN_OPEN
            + FRENCH_OPEN
            + WIMBLEDON
            + US_OPEN;
    private static final String THE_SLAMS = GRAND_SLAMS_HEADER
            + GRAND_SLAMS;
    private static final String FIFTH_MAJOR = "Indian Wells is the 5th Major"
            + System.lineSeparator();
    private static final Random RANDOM = new Random();

    /**
     * Validate SeekableByteChannel can be used to copy an OS file to
     * a Zip file
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void sbcFromOSToZipTest(final Map<String, String> env,
                                   final int compression) throws Exception {
        Entry e00 = Entry.of("Entry-00", compression, FIFTH_MAJOR);
        Path osFile = generatePath(HERE, "test", ".txt");
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(osFile);
        Files.deleteIfExists(zipFile);
        Files.writeString(osFile, FIFTH_MAJOR);
        // Create a Zip entry from an OS file
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env)) {
            sbcCopy(osFile, zipfs.getPath(e00.name));
        }
        // Check to see if the entries match
        verify(zipFile, e00);
        Files.deleteIfExists(osFile);
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate SeekableByteChannel can be used to copy an entry from
     * a Zip file to an OS file
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void sbcFromZipToOSTest(final Map<String, String> env,
                                   final int compression) throws Exception {
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        Entry e1 = Entry.of("Entry-1", compression, FIFTH_MAJOR);
        Path zipFile = generatePath(HERE, "test", ".zip");
        Path osFile = generatePath(HERE, "test", ".txt");
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(osFile);
        zip(zipFile, env, e0, e1);
        verify(zipFile, e0, e1);
        // Create an OS file from a Zip entry
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env)) {
            sbcCopy(zipfs.getPath(e0.name), osFile);
        }
        // Check to see if the file exists and the bytes match
        assertTrue(Files.isRegularFile(osFile));
        assertEquals(Files.readAllBytes(osFile), e0.bytes);
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(osFile);
    }

    /**
     * Validate SeekableByteChannel can be used to copy an entry from
     * one Zip file to another Zip file
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void sbcFromZipToZipTest(final Map<String, String> env,
                                    final int compression) throws Exception {
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        Entry e1 = Entry.of("Entry-1", compression, FIFTH_MAJOR);
        Path zipFile = generatePath(HERE, "test", ".zip");
        Path zipFile2 = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(zipFile2);
        zip(zipFile, env, e0, e1);
        verify(zipFile, e0, e1);
        // Copy entries from one Zip file to another using SeekableByteChannel
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileSystem zipfs2 = FileSystems.newFileSystem(zipFile2, env)) {
            sbcCopy(zipfs.getPath(e0.name), zipfs2.getPath(e0.name));
            sbcCopy(zipfs.getPath(e1.name), zipfs2.getPath(e1.name));
        }
        // Check to see if the entries match
        verify(zipFile2, e0, e1);
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(zipFile2);
    }

    /**
     * Validate SeekableByteChannel can be used to copy an entry within
     * a Zip file with the correct compression
     *
     * @param env                 Zip FS properties to use when creating the Zip file
     * @param compression         The compression used when writing the initial entries
     * @param expectedCompression The compression to be used when copying the entry
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "copyMoveMap")
    public void sbcChangeCompressionTest(final Map<String, String> env,
                                         final int compression,
                                         final int expectedCompression) throws Exception {
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        Entry e1 = Entry.of("Entry-1", compression, FIFTH_MAJOR);
        Entry e00 = Entry.of("Entry-00", expectedCompression, THE_SLAMS);
        // Compression method to use when copying the entry
        String targetCompression = expectedCompression == ZipEntry.STORED ? "true" : "false";
        Path zipFile = generatePath(HERE, "test", ".zip");
        Path zipFile2 = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(zipFile2);
        // Create the initial Zip files
        zip(zipFile, env, e0, e1);
        zip(zipFile2, env, e0, e1);
        verify(zipFile, e0, e1);
        // Copy the entry from one Zip file to another using SeekableByteChannel
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileSystem zipfs2 = FileSystems.newFileSystem(zipFile2,
                     Map.of("noCompression", targetCompression))) {
            sbcCopy(zipfs.getPath(e0.name), zipfs2.getPath(e00.name));
        }
        // Check to see if the entries match
        verify(zipFile2, e0, e1, e00);
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(zipFile2);
    }

    /**
     * Validate SeekableByteChannel::read can be used to read a Zip entry
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void sbcReadTest(final Map<String, String> env,
                            final int compression) throws Exception {
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        zip(zipFile, env, e0);
        // Read an entry
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             SeekableByteChannel sbc =
                     Files.newByteChannel(zipfs.getPath(e0.name),
                     Set.of(READ))) {
            ByteBuffer buf = ByteBuffer.allocate((int) sbc.size());
            int bytesRead = sbc.read(buf);
            // Check to see if the expected bytes were read
            byte[] result = Arrays.copyOfRange(buf.array(), 0, bytesRead);
            assertEquals(THE_SLAMS.getBytes(UTF_8), result);
        }
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate SeekableByteChannel::write can be used to create a Zip entry
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void sbcWriteTest(final Map<String, String> env,
                             final int compression) throws Exception {
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        // Create the Zip entry
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             SeekableByteChannel sbc =
                     Files.newByteChannel(zipfs.getPath(e0.name),
                     Set.of(CREATE, WRITE))) {
            ByteBuffer bb = ByteBuffer.wrap(THE_SLAMS.getBytes(UTF_8));
            sbc.write(bb);
        }
        // Verify the entry
        verify(zipFile, e0);
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate SeekableByteChannel can be used to append to an entry
     * in a Zip file
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void sbcAppendTest(final Map<String, String> env,
                              final int compression) throws Exception {
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        zip(zipFile, env, e0);
        // Update a Zip entry by appending to it
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             SeekableByteChannel sbc =
                     Files.newByteChannel(zipfs.getPath(e0.name),
                             Set.of(WRITE, APPEND))) {
            ByteBuffer bb = ByteBuffer.wrap(FIFTH_MAJOR.getBytes());
            sbc.write(bb);
        }
        // Check to see if the entries match
        verify(zipFile, e0.content(THE_SLAMS + FIFTH_MAJOR));
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate UnsupportedOperationException is thrown when
     * SeekableByteChannel::truncate is invoked
     * Note: Feature Request: JDK-8241959 has been created to support this
     * functionality
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void sbcTruncateTest(final Map<String, String> env,
                                final int compression) throws Exception {
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        zip(zipFile, env, e0);
        // Verify that a UnsupportedOperationException is thrown
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             SeekableByteChannel sbc =
                     Files.newByteChannel(zipfs.getPath(e0.name), Set.of(WRITE))) {
            assertThrows(UnsupportedOperationException.class, () ->
                    sbc.truncate(GRAND_SLAMS_HEADER.length()));
        }
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate FileAlreadyExistsException is thrown when
     * Files::newByteChannel is invoked with the CREATE_NEW option along with
     * either the WRITE or APPEND option and the entry already exists
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void sbcFAETest(final Map<String, String> env,
                           final int compression) throws Exception {
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        zip(zipFile, env, e0);
        // Validate that a FileAlreadyExistsException is thrown
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env)) {
            assertThrows(FileAlreadyExistsException.class, () ->
                    Files.newByteChannel(zipfs.getPath(e0.name),
                            Set.of(CREATE_NEW, WRITE)));
            assertThrows(FileAlreadyExistsException.class, () ->
                    Files.newByteChannel(zipfs.getPath(e0.name),
                            Set.of(CREATE_NEW, APPEND)));
        }
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate when SeekableByteChannel::close is called more than once, that
     * no error occurs
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void sbcCloseTest(final Map<String, String> env,
                             final int compression) throws Exception {
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        zip(zipFile, env, e0);
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env)) {
            SeekableByteChannel sbc = Files.newByteChannel(zipfs.getPath(e0.name),
                    Set.of(READ, WRITE));
            sbc.close();
            sbc.close();
            assertFalse(sbc.isOpen());
        }
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate ClosedChannelException is thrown when a SeekableByteChannel
     * method is invoked after calling SeekableByteChannel::close
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void sbcCCETest(final Map<String, String> env,
                           final int compression) throws Exception {
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        zip(zipFile, env, e0);
        ByteBuffer bb = ByteBuffer.wrap("First Serve".getBytes(UTF_8));
        // Check that ClosedChannelException is thrown if the channel is closed
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env)) {
            SeekableByteChannel sbc = Files.newByteChannel(zipfs.getPath(e0.name),
                    Set.of(READ, WRITE));
            sbc.close();
            assertThrows(ClosedChannelException.class, sbc::position);
            assertThrows(ClosedChannelException.class, () -> sbc.position(1));
            assertThrows(ClosedChannelException.class, () -> sbc.read(bb));
            assertThrows(ClosedChannelException.class, sbc::size);
            assertThrows(ClosedChannelException.class, () -> sbc.truncate(2));
            assertThrows(ClosedChannelException.class, () -> sbc.write(bb));
        }
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate SeekableByteChannel::size can be used to obtain the size
     * of a Zip entry
     * Note:  If the file is opened for writing, the test will fail unless data
     * has been written.  See: JDK-8241949
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void sbcSizeTest(final Map<String, String> env,
                            final int compression) throws Exception {
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        zip(zipFile, env, e0);
        // Open the file and validate the size
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             SeekableByteChannel sbc = Files.newByteChannel(
                     zipfs.getPath(e0.name), Set.of(READ))) {
            assertEquals(sbc.size(), THE_SLAMS.length());
        }
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             SeekableByteChannel sbc = Files.newByteChannel(zipfs.getPath(e0.name))) {
            assertEquals(sbc.size(), THE_SLAMS.length());
        }
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             SeekableByteChannel sbc = Files.newByteChannel(zipfs.getPath("Entry-01")
                     , Set.of(CREATE, WRITE))) {
            sbc.write(ByteBuffer.wrap(FIFTH_MAJOR.getBytes(UTF_8)));
            assertEquals(sbc.size(), FIFTH_MAJOR.length());
        }
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate SeekableByteChannel::isOpen returns true when the file
     * is open and false after SeekableByteChannel::close is called
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void sbcOpenClosedTest(final Map<String, String> env,
                                  final int compression) throws Exception {
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        zip(zipFile, env, e0);
        // Validate SeekableByteChannel::isOpen
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             SeekableByteChannel sbc = Files.newByteChannel(zipfs.getPath(e0.name),
                     Set.of(READ))) {
            assertTrue(sbc.isOpen());
            sbc.close();
            assertFalse(sbc.isOpen());
        }
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate SeekableByteChannel::position returns the expected position
     * Note: due to bug JDK-8241882, the position will not exceed the file size
     * in the test
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void sbcPositionTest(final Map<String, String> env,
                                final int compression) throws Exception {
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        zip(zipFile, env, e0);
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             SeekableByteChannel sbc =
                     Files.newByteChannel(zipfs.getPath(e0.name), Set.of(READ))) {
            int fSize = (int) sbc.size();
            // Specify the seed to use
            int seed = fSize + 1;
            ByteBuffer bb = ByteBuffer.allocate(BYTEBUFFER_SIZE);
            sbc.read(bb);
            for (var i = 0; i < fSize; i++) {
                long pos = RANDOM.nextInt(seed);
                sbc.position(pos);
                assertEquals(sbc.position(), pos);
            }
        }
        Files.deleteIfExists(zipFile);
    }

    // ### FileChannel Tests ###

    /**
     * Validate a FileChannel can be used to copy an OS file to
     * a Zip file
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcFromOSToZipTest(final Map<String, String> env,
                                  final int compression) throws Exception {
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        Path osFile = generatePath(HERE, "test", ".txt");
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(osFile);
        Files.deleteIfExists(zipFile);
        Files.writeString(osFile, THE_SLAMS);
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env)) {
            fcCopy(osFile, zipfs.getPath(e0.name));
        }
        // Verify the entry was copied
        verify(zipFile, e0);
        Files.deleteIfExists(osFile);
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate a FileChannel can be used to copy an entry from
     * a Zip file to an OS file
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcFromZipToOSTest(final Map<String, String> env,
                                  final int compression) throws Exception {
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        Entry e1 = Entry.of("Entry-1", compression, FIFTH_MAJOR);
        Path zipFile = generatePath(HERE, "test", ".zip");
        Path osFile = generatePath(HERE, "test", ".txt");
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(osFile);
        zip(zipFile, env, e0, e1);
        verify(zipFile, e0, e1);
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env)) {
            fcCopy(zipfs.getPath(e0.name), osFile);
        }
        // Check to see if the file exists and the bytes match
        assertTrue(Files.isRegularFile(osFile));
        assertEquals(Files.readAllBytes(osFile), e0.bytes);
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(osFile);
    }

    /**
     * Validate a FileChannel can be used to copy an entry from
     * a Zip file to another Zip file
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcFromZipToZipTest(final Map<String, String> env,
                                   final int compression) throws Exception {
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        Entry e1 = Entry.of("Entry-1", compression, FIFTH_MAJOR);
        Path zipFile = generatePath(HERE, "test", ".zip");
        Path zipFile2 = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(zipFile2);
        zip(zipFile, env, e0, e1);
        verify(zipFile, e0, e1);
        // Copy entries from one Zip file to another using FileChannel
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileSystem zipfs2 = FileSystems.newFileSystem(zipFile2, env)) {
            fcCopy(zipfs.getPath(e0.name), zipfs2.getPath(e0.name));
            fcCopy(zipfs.getPath(e1.name), zipfs2.getPath(e1.name));
        }
        // Check to see if the entries match
        verify(zipFile2, e0, e1);
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(zipFile2);
    }

    /**
     * Validate a FileChannel can be used to copy an entry within
     * a Zip file with the correct compression
     *
     * @param env                 Zip FS properties to use when creating the Zip file
     * @param compression         The compression used when writing the initial entries
     * @param expectedCompression The compression to be used when copying the entry
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "copyMoveMap")
    public void fcChangeCompressionTest(final Map<String, String> env,
                                        final int compression,
                                        final int expectedCompression) throws Exception {
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        Entry e1 = Entry.of("Entry-1", compression, FIFTH_MAJOR);
        Entry e00 = Entry.of("Entry-00", expectedCompression, THE_SLAMS);
        // Compression method to use when copying the entry
        String targetCompression = expectedCompression == ZipEntry.STORED ? "true" : "false";
        Path zipFile = generatePath(HERE, "test", ".zip");
        Path zipFile2 = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(zipFile2);
        // Create the initial Zip files
        zip(zipFile, env, e0, e1);
        zip(zipFile2, env, e0, e1);
        verify(zipFile, e0, e1);
        // Copy the entry from one Zip file to another
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileSystem zipfs2 = FileSystems.newFileSystem(zipFile2,
                     Map.of("noCompression", targetCompression))) {
            fcCopy(zipfs.getPath(e0.name), zipfs2.getPath(e00.name));
        }
        // Check to see if the entries match
        verify(zipFile2, e0, e1, e00);
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(zipFile2);
    }

    /**
     * Validate a FileChannel can be used to append an entry
     * in a Zip file
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcAppendTest(final Map<String, String> env,
                             final int compression) throws Exception {
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        zip(zipFile, env, e0);
        // Update the Zip entry by appending to it
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileChannel fc = FileChannel.open(zipfs.getPath(e0.name),
                     Set.of(WRITE, APPEND))) {
            ByteBuffer bb = ByteBuffer.wrap(FIFTH_MAJOR.getBytes());
            fc.write(bb);
        }
        // Check to see if the entries match
        verify(zipFile, e0.content(THE_SLAMS + FIFTH_MAJOR));
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate FileChannel::truncate will truncate the file at the specified
     * position
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcTruncateTest(final Map<String, String> env,
                               final int compression) throws Exception {
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        zip(zipFile, env, e0);
        // Truncate the Zip entry
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileChannel fc = FileChannel.open(zipfs.getPath(e0.name),
                     Set.of(WRITE))) {
            fc.truncate(GRAND_SLAMS_HEADER.length());
        }
        // Check to see if the entries match
        verify(zipFile, e0.content(GRAND_SLAMS_HEADER));
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate FileChannel::map throws an UnsupportedOperationException
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcMapTest(final Map<String, String> env,
                          final int compression) throws Exception {
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        zip(zipFile, env, e0);
        // Check UnsupportedOperationException is thrown
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileChannel fc = FileChannel.open(zipfs.getPath(e0.name),
                     Set.of(READ))) {
            assertThrows(UnsupportedOperationException.class, () ->
                    fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size()));
            assertThrows(UnsupportedOperationException.class, () ->
                    fc.map(FileChannel.MapMode.READ_WRITE, 0, fc.size()));
            assertThrows(UnsupportedOperationException.class, () ->
                    fc.map(FileChannel.MapMode.PRIVATE, 0, fc.size()));
        }
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate FileChannel::isOpen returns true when the file is open
     * and false after FileChannel::close is called
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcOpenClosedTest(final Map<String, String> env,
                                 final int compression) throws Exception {
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        zip(zipFile, env, e0);
        // Validate FileChannel::isOpen
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileChannel fc = FileChannel.open(zipfs.getPath(e0.name),
                     Set.of(READ))) {
            assertTrue(fc.isOpen());
            fc.close();
            assertFalse(fc.isOpen());
        }
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate FileAlreadyExistsException is thrown when
     * FileChannel::open is invoked with the CREATE_NEW option and the Zip
     * entry already exists
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcFAETest(final Map<String, String> env,
                          final int compression) throws Exception {
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        zip(zipFile, env, e0);
        // Check FileAlreadyExistsException is thrown
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env)) {
            assertThrows(FileAlreadyExistsException.class, () ->
                    FileChannel.open(zipfs.getPath(e0.name), Set.of(CREATE_NEW, WRITE)));
        }
        Files.deleteIfExists(zipFile);
    }


    /**
     * Validate when FileChannel::close is called more than once, that
     * no error occurs
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcCloseTest(final Map<String, String> env,
                             final int compression) throws Exception {
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        zip(zipFile, env, e0);
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env)) {
            FileChannel fc = FileChannel.open(zipfs.getPath(e0.name),
                    Set.of(READ, WRITE));
            fc.close();
            fc.close();
            assertFalse(fc.isOpen());
        }
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate ClosedChannelException is thrown when
     * FileChannel::close is invoked and another FileChannel method is invoked
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcCCETest(final Map<String, String> env,
                          final int compression) throws Exception {
        Path zipFile = generatePath(HERE, "test", ".zip");
        Path osFile = generatePath(HERE, "test", ".txt");
        Files.deleteIfExists(osFile);
        Files.deleteIfExists(zipFile);
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        zip(zipFile, env, e0);
        // Create the ByteBuffer array to be used
        ByteBuffer[] bb = {
                ByteBuffer.wrap("First Serve".getBytes(UTF_8)),
                ByteBuffer.wrap("Fault".getBytes(UTF_8)),
                ByteBuffer.wrap("Double Fault".getBytes(UTF_8))
        };
        // Check ClosedChannelException is thrown if the channel is closed
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileChannel fc = FileChannel.open(zipfs.getPath(e0.name), Set.of(READ, WRITE))) {
            fc.close();
            assertThrows(ClosedChannelException.class, () -> fc.force(false));
            assertThrows(ClosedChannelException.class, fc::lock);
            assertThrows(ClosedChannelException.class, () -> fc.lock(0, 0, false));
            assertThrows(ClosedChannelException.class, fc::position);
            assertThrows(ClosedChannelException.class, () -> fc.position(1));
            assertThrows(ClosedChannelException.class, () -> fc.read(bb));
            assertThrows(ClosedChannelException.class, () -> fc.read(bb, 1, 2));
            assertThrows(ClosedChannelException.class, () -> fc.read(bb[0]));
            assertThrows(ClosedChannelException.class, () -> fc.read(bb[0], 1));
            assertThrows(ClosedChannelException.class, fc::size);
            assertThrows(ClosedChannelException.class, fc::tryLock);
            assertThrows(ClosedChannelException.class, () ->
                    fc.tryLock(0, 1, false));
            assertThrows(ClosedChannelException.class, () -> fc.truncate(2));
            assertThrows(ClosedChannelException.class, () -> fc.write(bb));
            assertThrows(ClosedChannelException.class, () -> fc.write(bb[0]));
            // Note does not check closed 1st when file not opened with "WRITE"
            assertThrows(ClosedChannelException.class, () -> fc.write(bb[0], 1));
            assertThrows(ClosedChannelException.class, () -> fc.write(bb, 1, 2));
            try (
                    FileChannel out = FileChannel.open(osFile, Set.of(CREATE_NEW, WRITE))) {
                assertThrows(ClosedChannelException.class, () ->
                        fc.transferTo(0, fc.size(), out));
                // Check when 'fc' is closed
                assertThrows(ClosedChannelException.class, () ->
                        out.transferFrom(fc, 0, fc.size()));
                fc.close();
                // Check when 'out' is closed
                assertThrows(ClosedChannelException.class, () ->
                        out.transferFrom(fc, 0, fc.size()));
            }
        }
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(osFile);
    }

    /**
     * Validate FileChannel::read can read an entry from a Zip file
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcReadTest(final Map<String, String> env,
                           final int compression) throws Exception {
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        zip(zipFile, env, e0);
        // Read an entry
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileChannel fc = FileChannel.open(zipfs.getPath(e0.name),
                     Set.of(READ))) {
            ByteBuffer buf = ByteBuffer.allocate((int) fc.size());
            int bytesRead = fc.read(buf);
            // Check to see if the expected bytes were read
            byte[] result = Arrays.copyOfRange(buf.array(), 0, bytesRead);
            assertEquals(THE_SLAMS.getBytes(UTF_8), result);
        }
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate FileChannel::read can read an entry from a Zip file
     *  when specifying a starting position
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcReadPosTest(final Map<String, String> env,
                              final int compression) throws Exception {
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        zip(zipFile, env, e0);
        // Read an entry specifying a starting position within the file
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileChannel fc = FileChannel.open(zipfs.getPath(e0.name),
                     Set.of(READ))) {
            ByteBuffer buf = ByteBuffer.allocate((int) fc.size());
            int bytesRead = fc.read(buf, GRAND_SLAMS_HEADER.length());
            // Check to see if the expected bytes were read
            byte[] result = Arrays.copyOfRange(buf.array(), 0, bytesRead);
            assertEquals(GRAND_SLAMS.getBytes(UTF_8), result);
        }
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate FileChannel::read  can be used with a ByteBuffer array to
     * read an entry from a Zip file
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcReadArrayTest(final Map<String, String> env,
                                final int compression) throws Exception {
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        zip(zipFile, env, e0);
        // Create the ByteBuffer array that will be updated
        ByteBuffer[] bb = {
                ByteBuffer.allocate(GRAND_SLAMS_HEADER.length()),
                ByteBuffer.allocate(AUSTRALIAN_OPEN.length()),
                ByteBuffer.allocate(FRENCH_OPEN.length()),
                ByteBuffer.allocate(WIMBLEDON.length()),
                ByteBuffer.allocate(US_OPEN.length()),
        };
        // Read an entry with a ByteBuffer array
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileChannel fc = FileChannel.open(zipfs.getPath(e0.name),
                     Set.of(READ))) {
            fc.read(bb);
            // Convert the ByteBuffer array into a single byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            for (ByteBuffer b : bb) {
                bos.write(b.array());
            }
            // Check to see if the returned byte array is what is expected
            assertEquals(e0.bytes, bos.toByteArray());
        }
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate FileChannel::read can be used to update specific offset(s)
     * of a ByteBuffer array when reading a Zip entry
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcReadArrayWithOffsetTest(final Map<String, String> env,
                                          final int compression) throws Exception {
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        zip(zipFile, env, e0);
        // Initial values that will be replaced by the AUSTRALIAN_OPEN and FRENCH_OPEN
        // values via FileChannel::read
        String newValue = "Homeward Bound!" + System.lineSeparator();
        String newValue2 = "Sybase Open" + System.lineSeparator();
        // Create the ByteBuffer array that will be updated
        ByteBuffer[] bb = {
                ByteBuffer.wrap((newValue)
                        .getBytes(UTF_8)),
                ByteBuffer.wrap((newValue2)
                        .getBytes(UTF_8)),
                ByteBuffer.wrap((WIMBLEDON)
                        .getBytes(UTF_8)),
                ByteBuffer.wrap((US_OPEN)
                        .getBytes(UTF_8))
        };
        // Read the Zip entry replacing the data in offset 0 and 1
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileChannel fc = FileChannel.open(zipfs.getPath(e0.name),
                     Set.of(READ))) {
            fc.position(GRAND_SLAMS_HEADER.length());
            fc.read(bb, 0, 2);
        }
        // Convert the ByteBuffer array into a single byte array
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (ByteBuffer b : bb) {
            bos.write(b.array());
        }
        // Check to see if the returned byte array is what is expected
        assertEquals(GRAND_SLAMS.getBytes(UTF_8), bos.toByteArray());
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate FileChannel::transferTo can be used to copy an OS file to
     * a Zip file
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcTransferToZipTest(final Map<String, String> env,
                                    final int compression) throws Exception {
        Entry e00 = Entry.of("Entry-00", compression, THE_SLAMS);
        Path osFile = generatePath(HERE, "test", ".txt");
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(osFile);
        Files.deleteIfExists(zipFile);
        Files.writeString(osFile, THE_SLAMS);
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env)) {
            fcTransferTo(osFile, zipfs.getPath(e00.name));
        }
        // Verify the entry was copied
        verify(zipFile, e00);
        assertEquals(Files.readAllBytes(osFile), e00.bytes);
        Files.deleteIfExists(osFile);
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate FileChannel::transferTo can be used to copy a Zip entry to
     * an OS File
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcTransferToOsTest(final Map<String, String> env,
                                   final int compression) throws Exception {
        Entry e00 = Entry.of("Entry-00", compression, THE_SLAMS);
        Path osFile = generatePath(HERE, "test", ".txt");
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(osFile);
        Files.deleteIfExists(zipFile);
        zip(zipFile, env, e00);
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env)) {
            fcTransferTo(zipfs.getPath(e00.name), osFile);
        }
        // Verify the entry was copied
        assertEquals(Files.readAllBytes(osFile), e00.bytes);
        Files.deleteIfExists(osFile);
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate FileChannel::transferTo can be used to copy a Zip entry to
     * another Zip file
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcTransferToZipToZipTest(final Map<String, String> env,
                                         final int compression) throws Exception {
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        Entry e1 = Entry.of("Entry-1", compression, FIFTH_MAJOR);
        Path zipFile = generatePath(HERE, "test", ".zip");
        Path zipFile2 = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(zipFile2);
        zip(zipFile, env, e0, e1);
        verify(zipFile, e0, e1);
        // Copy entries from one Zip file to another using FileChannel
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileSystem zipfs2 = FileSystems.newFileSystem(zipFile2, env)) {
            fcTransferTo(zipfs.getPath(e0.name), zipfs2.getPath(e0.name));
            fcTransferTo(zipfs.getPath(e1.name), zipfs2.getPath(e1.name));
        }
        // Check to see if the entries match
        verify(zipFile2, e0, e1);
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(zipFile2);
    }

    /**
     * Validate FileChannel::transferFrom can be used to copy an OS File to
     * a Zip entry
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcTransferFromOsTest(final Map<String, String> env,
                                     final int compression) throws Exception {
        Entry e00 = Entry.of("Entry-00", compression, THE_SLAMS);
        Path osFile = generatePath(HERE, "test", ".txt");
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(osFile);
        Files.deleteIfExists(zipFile);
        Files.writeString(osFile, THE_SLAMS);
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env)) {
            fcTransferFrom(osFile, zipfs.getPath(e00.name));
        }
        // Verify the entry was copied
        zip(zipFile, env, e00);
        assertEquals(Files.readAllBytes(osFile), e00.bytes);
        Files.deleteIfExists(osFile);
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate FileChannel::transferFrom can be used to copy a Zip entry to
     * an OS File
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcTransferFromZipTest(final Map<String, String> env,
                                      final int compression) throws Exception {
        Entry e00 = Entry.of("Entry-00", compression, THE_SLAMS);
        Path osFile = generatePath(HERE, "test", ".txt");
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(osFile);
        Files.deleteIfExists(zipFile);
        zip(zipFile, env, e00);
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env)) {
            fcTransferFrom(zipfs.getPath(e00.name), osFile);
        }
        // Verify the bytes match
        assertEquals(Files.readAllBytes(osFile), e00.bytes);
        Files.deleteIfExists(osFile);
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate FileChannel::transferFrom can be used to copy a Zip entry
     * to another Zip file
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcTransferFromZipToZipTest(final Map<String, String> env,
                                           final int compression) throws Exception {
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        Entry e1 = Entry.of("Entry-1", compression, FIFTH_MAJOR);
        Path zipFile = generatePath(HERE, "test", ".zip");
        Path zipFile2 = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(zipFile2);
        zip(zipFile, env, e0, e1);
        verify(zipFile, e0, e1);
        // Copy entries from one Zip file to another using FileChannel
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileSystem zipfs2 = FileSystems.newFileSystem(zipFile2, env)) {
            fcTransferFrom(zipfs.getPath(e0.name), zipfs2.getPath(e0.name));
            fcTransferFrom(zipfs.getPath(e1.name), zipfs2.getPath(e1.name));
        }
        // Check to see if the entries match
        verify(zipFile2, e0, e1);
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(zipFile2);
    }

    /**
     * Validate FileChannel::write can be used to create a Zip entry
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcWriteTest(final Map<String, String> env,
                            final int compression) throws Exception {
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        // Create the Zip entry
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileChannel fc = FileChannel.open(zipfs.getPath(e0.name),
                     Set.of(CREATE, WRITE))) {
            ByteBuffer bb = ByteBuffer.wrap(THE_SLAMS.getBytes(UTF_8));
            fc.write(bb);
        }
        // Verify the entry was updated
        verify(zipFile, e0);
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate FileChannel::write can be used to update a Zip entry
     * when specifying a starting position
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcWritePosTest(final Map<String, String> env,
                               final int compression) throws Exception {
        // Use this value to replace the value specified for AUSTRALIAN_OPEN
        String NewValue = "Homeward Bound!" + System.lineSeparator();
        // Expected results after updating the file
        String updatedFile = GRAND_SLAMS_HEADER
                + NewValue
                + FRENCH_OPEN
                + WIMBLEDON
                + US_OPEN;
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        zip(zipFile, env, e0);
        // Update the Zip entry at the specified position
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileChannel fc = FileChannel.open(zipfs.getPath(e0.name),
                     Set.of(READ, WRITE))) {
            ByteBuffer bb = ByteBuffer.wrap(NewValue.getBytes(UTF_8));
            fc.write(bb, GRAND_SLAMS_HEADER.length());
        }
        // Verify the entry was updated
        verify(zipFile, e0.content(updatedFile));
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate FileChannel::write using a ByteBuffer array
     * can be used to create a Zip entry
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcWriteArrayTest(final Map<String, String> env,
                                 final int compression) throws Exception {
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        // Entry added to the Zip file
        Entry e0 = Entry.of("Entry-0", compression, GRAND_SLAMS);
        // Create the ByteBuffer array that will be used to create the Zip entry
        ByteBuffer[] bb = {
                ByteBuffer.wrap(AUSTRALIAN_OPEN.getBytes(UTF_8)),
                ByteBuffer.wrap(FRENCH_OPEN.getBytes(UTF_8)),
                ByteBuffer.wrap(WIMBLEDON.getBytes(UTF_8)),
                ByteBuffer.wrap(US_OPEN.getBytes(UTF_8))
        };
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileChannel fc = FileChannel.open(zipfs.getPath(e0.name),
                     Set.of(CREATE, WRITE))) {
            fc.write(bb);
            assertEquals(fc.size(), GRAND_SLAMS.length());
        }
        // Verify the entry was created
        verify(zipFile, e0);
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate FileChannel::write specifying a ByteBuffer array
     *  with an offset can be used to create a Zip entry
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcWriteArrayWithOffsetTest(final Map<String, String> env,
                                           final int compression) throws Exception {

        // Use this value to replace the value specified for AUSTRALIAN_OPEN
        String newValue = "Homeward Bound!" + System.lineSeparator();
        // Use this value to replace the value specified for FRENCH_OPEN
        String newValue2 = "Sybase Open" + System.lineSeparator();
        // Expected results after updating the file
        String updatedFile = GRAND_SLAMS_HEADER
                + newValue
                + newValue2
                + WIMBLEDON
                + US_OPEN;
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        // Initial Zip entry
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        zip(zipFile, env, e0);
        // Create the ByteBuffer array that will be used to update the Zip entry
        ByteBuffer[] bb = {
                ByteBuffer.wrap(newValue.getBytes(UTF_8)),
                ByteBuffer.wrap(newValue2.getBytes(UTF_8)),
                ByteBuffer.wrap("!!!Should not Write!!!!".getBytes(UTF_8))
        };
        // Move to the file position and then write the updates to the file
        // specifying the ByteBuffer offset & length
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileChannel fc = FileChannel.open(zipfs.getPath(e0.name),
                     Set.of(CREATE, WRITE))) {
            // Skip past the header
            fc.position(GRAND_SLAMS_HEADER.length());
            // Replace the original values
            fc.write(bb, 0, 2);
            assertEquals(fc.size(), THE_SLAMS.length());
        }
        // Verify the entry was updated
        verify(zipFile, e0.content(updatedFile));
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate FileChannel::force can be used when writing a Zip entry
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcForceWriteTest(final Map<String, String> env,
                            final int compression) throws Exception {
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        // Check that no errors occur when using FileChannel::force
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileChannel fc = FileChannel.open(zipfs.getPath(e0.name),
                     Set.of(CREATE, WRITE))) {
            fc.force(false);
            fc.write(ByteBuffer.wrap(GRAND_SLAMS_HEADER.getBytes(UTF_8)));
            fc.force(true);
            fc.write(ByteBuffer.wrap(GRAND_SLAMS.getBytes(UTF_8)));
        }
        // Verify the entry was updated
        verify(zipFile, e0);
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate FileChannel::position returns the expected position
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcPositionTest(final Map<String, String> env,
                                final int compression) throws Exception {
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        zip(zipFile, env, e0);
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileChannel fc = FileChannel.open(zipfs.getPath(e0.name), Set.of(READ))) {
            int fSize = (int) fc.size();
            // Specify the seed to use
            int seed = fSize + 10;
            ByteBuffer bb = ByteBuffer.allocate(BYTEBUFFER_SIZE);
            fc.read(bb);
            for (var i = 0; i < fSize; i++) {
                long pos = RANDOM.nextInt(seed);
                fc.position(pos);
                assertEquals(fc.position(), pos);
            }
        }
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate FileChannel::size can be used to obtain the size of a Zip entry
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcSizeTest(final Map<String, String> env,
                           final int compression) throws Exception {
        Path osFile = Path.of("GrandSlams.txt");
        Files.deleteIfExists(osFile);
        Files.writeString(osFile, THE_SLAMS);
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        zip(zipFile, env, e0);
        // Validate the file sizes match
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileChannel fc = FileChannel.open(zipfs.getPath(e0.name))) {
            assertEquals(fc.size(), THE_SLAMS.length());
        }
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileChannel fc = FileChannel.open(zipfs.getPath(e0.name), Set.of(READ))) {
            assertEquals(fc.size(), THE_SLAMS.length());
        }
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileChannel fc = FileChannel.open(zipfs.getPath(e0.name), Set.of(READ, WRITE))) {
            assertEquals(fc.size(), THE_SLAMS.length());
        }
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileChannel fc = FileChannel.open(zipfs.getPath(e0.name), Set.of(WRITE))) {
            assertEquals(fc.size(), THE_SLAMS.length());
        }
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileChannel fc = FileChannel.open(zipfs.getPath(e0.name), Set.of(APPEND))) {
            assertEquals(fc.size(), THE_SLAMS.length());
        }
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileChannel fc = FileChannel.open(zipfs.getPath("Entry-01"),
                     Set.of(CREATE, WRITE))) {
            fc.write(ByteBuffer.wrap(FIFTH_MAJOR.getBytes(UTF_8)));
            assertEquals(fc.size(), FIFTH_MAJOR.length());
        }
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(osFile);
    }

    /**
     * Validate FileChannel::lock returns a valid lock
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcLockTest(final Map<String, String> env,
                           final int compression) throws Exception {
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        zip(zipFile, env, e0);
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileChannel in = FileChannel.open(zipfs.getPath(e0.name), Set.of(READ, WRITE))) {
            FileLock lock = in.lock();
            assertNotNull(lock);
            assertTrue(lock.isValid());
            lock.close();
            assertFalse(lock.isValid());
            // Acquire another lock specifying an offset and size
            lock = in.lock(0, 10, false);
            assertNotNull(lock);
            assertTrue(lock.isValid());
            lock.close();
            assertFalse(lock.isValid());
        }
        Files.deleteIfExists(zipFile);
    }

    /**
     * Validate FileChannel::tryLock returns a valid lock
     *
     * @param env         Zip FS properties to use when creating the Zip file
     * @param compression The compression used when writing the entries
     * @throws Exception If an error occurs
     */
    @Test(dataProvider = "zipfsMap")
    public void fcTryLockTest(final Map<String, String> env,
                              final int compression) throws Exception {
        Path zipFile = generatePath(HERE, "test", ".zip");
        Files.deleteIfExists(zipFile);
        Entry e0 = Entry.of("Entry-0", compression, THE_SLAMS);
        zip(zipFile, env, e0);
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFile, env);
             FileChannel in = FileChannel.open(zipfs.getPath(e0.name), Set.of(READ, WRITE))) {
            FileLock lock = in.tryLock();
            assertNotNull(lock);
            assertTrue(lock.isValid());
            lock.close();
            assertFalse(lock.isValid());
            // Acquire another lock specifying an offset and size
            lock = in.tryLock(0, 10, false);
            assertNotNull(lock);
            assertTrue(lock.isValid());
            lock.close();
            assertFalse(lock.isValid());
        }
        Files.deleteIfExists(zipFile);
    }

    /**
     * Use a SeekableByteChannel to copy an entry from one file to another
     *
     * @param src Path of file to read from
     * @param dst Path of file to write to
     * @throws IOException if an error occurs
     */
    private static void sbcCopy(Path src, Path dst) throws IOException {
        try (SeekableByteChannel in = Files.newByteChannel(src, Set.of(READ));
             SeekableByteChannel out = Files.newByteChannel(dst,
                     Set.of(CREATE_NEW, WRITE))) {
            ByteBuffer bb = ByteBuffer.allocate(BYTEBUFFER_SIZE);
            while (in.read(bb) >= 0) {
                bb.flip();
                out.write(bb);
                bb.clear();
            }
        }
    }

    /**
     * Use a FileChannel to copy an entry from one file to another
     *
     * @param src Path of file to read from
     * @param dst Path of file to write to
     * @throws IOException if an error occurs
     */
    private static void fcCopy(Path src, Path dst) throws IOException {
        try (FileChannel srcFc = FileChannel.open(src, Set.of(READ));
             FileChannel dstFc = FileChannel.open(dst, Set.of(CREATE_NEW, WRITE))) {
            ByteBuffer bb = ByteBuffer.allocate(BYTEBUFFER_SIZE);
            while (srcFc.read(bb) >= 0) {
                bb.flip();
                dstFc.write(bb);
                bb.clear();
            }
        }
    }

    /**
     * Use FileChannel::transferTo to copy an entry from one file to another
     *
     * @param src Path of file to read from
     * @param dst Path of file to write to
     * @throws IOException if an error occurs
     */
    private static void fcTransferTo(Path src, Path dst) throws IOException {
        try (FileChannel in = FileChannel.open(src, Set.of(READ));
             FileChannel out = FileChannel.open(dst, Set.of(CREATE_NEW, WRITE))) {
            in.transferTo(0, in.size(), out);
        }
    }

    /**
     * Use FileChannel::transferFrom to copy an entry from one file to another
     *
     * @param from Path of file to read from
     * @param to   Path of file to write to
     * @throws IOException if an error occurs
     */
    private static void fcTransferFrom(Path from, Path to) throws IOException {
        try (FileChannel in = FileChannel.open(from, Set.of(READ));
             FileChannel out = FileChannel.open(to, Set.of(CREATE_NEW, WRITE))) {
            out.transferFrom(in, 0, in.size());
        }
    }
}
