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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.testng.Assert.assertTrue;

/**
 * @test
 * @bug 8226530
 * @summary ZIP File System tests that leverage DirectoryStream
 * @compile Zip64SizeTest.java
 * @run testng Zip64SizeTest
 */
public class Zip64SizeTest {

    private static final int BUFFER_SIZE = 2048;
    // ZIP file to create
    private static final String ZIP_FILE_NAME = "Zip64SizeTest.zip";
    // File that will be created with a size greater than 0xFFFFFFFF
    private static final String LARGE_FILE_NAME = "LargeZipEntry.txt";
    // File that will be created with a size less than 0xFFFFFFFF
    private static final String SMALL_FILE_NAME = "SmallZipEntry.txt";
    // List of files to be added to the ZIP file
    private static final List<String> ZIP_ENTRIES = List.of(LARGE_FILE_NAME,
            SMALL_FILE_NAME);
    private static final long LARGE_FILE_SIZE = 5L * 1024L * 1024L * 1024L; // 5GB
    private static final long SMALL_FILE_SIZE = 0x100000L; // 1024L x 1024L;

    /**
     * Validate that if the size of a ZIP entry exceeds 0xFFFFFFFF, that the
     * correct size is returned from the ZIP64 Extended information.
     * @throws IOException
     */
    @Test
    private static void validateZipEntrySizes() throws IOException {
        createFiles();
        createZipFile();
        System.out.println("Validating Zip Entry Sizes");
        try (ZipFile zip = new ZipFile(ZIP_FILE_NAME)) {
            ZipEntry ze = zip.getEntry(LARGE_FILE_NAME);
            System.out.printf("Entry: %s, size= %s%n", ze.getName(), ze.getSize());
            assertTrue(ze.getSize() == LARGE_FILE_SIZE);
            ze = zip.getEntry(SMALL_FILE_NAME);
            System.out.printf("Entry: %s, size= %s%n", ze.getName(), ze.getSize());
            assertTrue(ze.getSize() == SMALL_FILE_SIZE);

        }
    }

    /**
     * Delete the files created for use by the test
     * @throws IOException if an error occurs deleting the files
     */
    private static void deleteFiles() throws IOException {
        Files.deleteIfExists(Path.of(ZIP_FILE_NAME));
        Files.deleteIfExists(Path.of(LARGE_FILE_NAME));
        Files.deleteIfExists(Path.of(SMALL_FILE_NAME));
    }

    /**
     * Create the ZIP file adding an entry whose size exceeds 0xFFFFFFFF
     * @throws IOException if an error occurs creating the ZIP File
     */
    private static void createZipFile() throws IOException {
        try (FileOutputStream fos = new FileOutputStream(ZIP_FILE_NAME);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            System.out.printf("Creating Zip file: %s%n", ZIP_FILE_NAME);
            for (String srcFile : ZIP_ENTRIES) {
                System.out.printf("...Adding Entry: %s%n", srcFile);
                File fileToZip = new File(srcFile);
                try (FileInputStream fis = new FileInputStream(fileToZip)) {
                    ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
                    zipEntry.setSize(fileToZip.length());
                    zos.putNextEntry(zipEntry);
                    byte[] bytes = new byte[BUFFER_SIZE];
                    int length;
                    while ((length = fis.read(bytes)) >= 0) {
                        zos.write(bytes, 0, length);
                    }
                }
            }
        }
    }

    /**
     * Create the files that will be added to the ZIP file
     * @throws IOException if there is a problem  creating the files
     */
    private static void createFiles() throws IOException {
        try (RandomAccessFile largeFile = new RandomAccessFile(LARGE_FILE_NAME, "rw");
             RandomAccessFile smallFile = new RandomAccessFile(SMALL_FILE_NAME, "rw")) {
            System.out.printf("Creating %s%n", LARGE_FILE_NAME);
            largeFile.setLength(LARGE_FILE_SIZE);
            System.out.printf("Creating %s%n", SMALL_FILE_NAME);
            smallFile.setLength(SMALL_FILE_SIZE);
        }
    }

    /**
     * Make sure the needed test files do not exist prior to executing the test
     * @throws IOException
     */
    @BeforeMethod
    public void setUp() throws IOException {
        deleteFiles();
    }

    /**
     * Remove the files created for the test
     * @throws IOException
     */
    @AfterMethod
    public void tearDown() throws IOException {
        deleteFiles();
    }
}
