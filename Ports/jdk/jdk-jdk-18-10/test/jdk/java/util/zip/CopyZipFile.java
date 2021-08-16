/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
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

/**
 * @test
 * @summary Test behaviour when copying ZipEntries between zip files.
 * @run main/othervm CopyZipFile
 */

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class CopyZipFile {
    private static final String ZIP_FILE = "first.zip";
    private static final String TEST_STRING = "TestTestTest";

    private static void createZip(String zipFile) throws Exception {
        File f = new File(zipFile);
        f.deleteOnExit();
        try (OutputStream os = new FileOutputStream(f);
             ZipOutputStream zos = new ZipOutputStream(os)) {
            // First file will be compressed with DEFAULT_COMPRESSION (i.e. -1 or 6)
            zos.putNextEntry(new ZipEntry("test1.txt"));
            zos.write(TEST_STRING.getBytes());
            zos.closeEntry();
            // Second file won't be compressed at all (i.e. STORED)
            zos.setMethod(ZipOutputStream.STORED);
            ZipEntry ze = new ZipEntry("test2.txt");
            int length = TEST_STRING.length();
            ze.setSize(length);
            ze.setCompressedSize(length);
            CRC32 crc = new CRC32();
            crc.update(TEST_STRING.getBytes("utf8"), 0, length);
            ze.setCrc(crc.getValue());
            zos.putNextEntry(ze);
            zos.write(TEST_STRING.getBytes());
            // Third file will be compressed with NO_COMPRESSION (i.e. 0)
            zos.setMethod(ZipOutputStream.DEFLATED);
            zos.setLevel(Deflater.NO_COMPRESSION);
            zos.putNextEntry(new ZipEntry("test3.txt"));
            zos.write(TEST_STRING.getBytes());
            // Fourth file will be compressed with BEST_SPEED (i.e. 1)
            zos.setLevel(Deflater.BEST_SPEED);
            zos.putNextEntry(new ZipEntry("test4.txt"));
            zos.write(TEST_STRING.getBytes());
            // Fifth file will be compressed with BEST_COMPRESSION (i.e. 9)
            zos.setLevel(Deflater.BEST_COMPRESSION);
            zos.putNextEntry(new ZipEntry("test5.txt"));
            zos.write(TEST_STRING.getBytes());
        }
    }

    public static void main(String args[]) throws Exception {
        // By default, ZipOutputStream creates zip files with Local File Headers
        // without size, compressedSize and crc values and an extra Data
        // Descriptor (see https://en.wikipedia.org/wiki/Zip_(file_format)
        // after the data belonging to that entry with these values if in the
        // corresponding ZipEntry one of the size, compressedSize or crc fields is
        // equal to '-1' (which is the default for newly created ZipEntries).
        createZip(ZIP_FILE);

        // Now read all the entries of the newly generated zip file with a ZipInputStream
        // and copy them to a new zip file with the help of a ZipOutputStream.
        // This only works reliably because the generated zip file has no values for the
        // size, compressedSize and crc values of a zip entry in the local file header and
        // therefore the ZipEntry objects created by ZipOutputStream.getNextEntry() will have
        // all these fields set to '-1'.
        ZipEntry entry;
        byte[] buf = new byte[512];
        try (InputStream is = new FileInputStream(ZIP_FILE);
             ZipInputStream zis = new ZipInputStream(is);
             OutputStream os = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(os)) {
            while((entry = zis.getNextEntry())!=null) {
                // ZipInputStream.getNextEntry() only reads the Local File Header of a zip entry,
                // so for the zip file we've just generated the ZipEntry fields 'size', 'compressedSize`
                // and 'crc' for deflated entries should be uninitialized (i.e. '-1').
                System.out.println(
                    String.format("name=%s, clen=%d, len=%d, crc=%d",
                                  entry.getName(), entry.getCompressedSize(), entry.getSize(), entry.getCrc()));
                if (entry.getMethod() == ZipEntry.DEFLATED &&
                    (entry.getCompressedSize() != -1 || entry.getSize() != -1 || entry.getCrc() != -1)) {
                    throw new Exception("'size', 'compressedSize' and 'crc' shouldn't be initialized at this point.");
                }
                zos.putNextEntry(entry);
                zis.transferTo(zos);
                // After all the data belonging to a zip entry has been inflated (i.e. after ZipInputStream.read()
                // returned '-1'), it is guaranteed that the ZipInputStream will also have consumed the Data
                // Descriptor (if any) after the data and will have updated the 'size', 'compressedSize' and 'crc'
                // fields of the ZipEntry object.
                System.out.println(
                    String.format("name=%s, clen=%d, len=%d, crc=%d\n",
                                  entry.getName(), entry.getCompressedSize(), entry.getSize(), entry.getCrc()));
                if (entry.getCompressedSize() == -1 || entry.getSize() == -1) {
                    throw new Exception("'size' and 'compressedSize' must be initialized at this point.");
                }
            }
        }

        // Now we read all the entries of the initially generated zip file with the help
        // of the ZipFile class. The ZipFile class reads all the zip entries from the Central
        // Directory which must have accurate information for size, compressedSize and crc.
        // This means that all ZipEntry objects returned from ZipFile will have correct
        // settings for these fields.
        // If the compression level was different in the initial zip file (which we can't find
        // out any more now because the zip file format doesn't record this information) the
        // size of the re-compressed entry we are writing to the ZipOutputStream might differ
        // from the original compressed size recorded in the ZipEntry. This would result in an
        // "invalid entry compressed size" ZipException if ZipOutputStream wouldn't ignore
        // the implicitely set compressed size attribute of ZipEntries read from a ZipFile
        // or ZipInputStream.
        try (OutputStream os = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(os);
             ZipFile zf = new ZipFile(ZIP_FILE)) {
            Enumeration<? extends ZipEntry> entries = zf.entries();
            while (entries.hasMoreElements()) {
                entry = entries.nextElement();
                System.out.println(
                    String.format("name=%s, clen=%d, len=%d, crc=%d\n",
                                  entry.getName(), entry.getCompressedSize(),
                                  entry.getSize(), entry.getCrc()));
                if (entry.getCompressedSize() == -1 || entry.getSize() == -1) {
                    throw new Exception("'size' and 'compressedSize' must be initialized at this point.");
                }
                InputStream is = zf.getInputStream(entry);
                zos.putNextEntry(entry);
                is.transferTo(zos);
                zos.closeEntry();
            }
        }

        // The compressed size attribute of a ZipEntry shouldn't be ignored if it was set
        // explicitely by calling ZipEntry.setCpompressedSize()
        try (OutputStream os = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(os);
             ZipFile zf = new ZipFile(ZIP_FILE)) {
            Enumeration<? extends ZipEntry> entries = zf.entries();
            while (entries.hasMoreElements()) {
                try {
                    entry = entries.nextElement();
                    entry.setCompressedSize(entry.getCompressedSize());
                    InputStream is = zf.getInputStream(entry);
                    zos.putNextEntry(entry);
                    is.transferTo(zos);
                    zos.closeEntry();
                    if ("test3.txt".equals(entry.getName())) {
                        throw new Exception(
                            "Should throw a ZipException if ZipEntry.setCpompressedSize() was called.");
                    }
                } catch (ZipException ze) {
                    if ("test1.txt".equals(entry.getName()) || "test2.txt".equals(entry.getName())) {
                        throw new Exception(
                            "Shouldn't throw a ZipExcpetion for STORED files or files compressed with DEFAULT_COMPRESSION");
                    }
                    // Hack to fix and close the offending zip entry with the correct compressed size.
                    // The exception message is something like:
                    //   "invalid entry compressed size (expected 12 but got 7 bytes)"
                    // and we need to extract the second integer.
                    Pattern cSize = Pattern.compile("\\d+");
                    Matcher m = cSize.matcher(ze.getMessage());
                    m.find();
                    m.find();
                    entry.setCompressedSize(Integer.parseInt(m.group()));
                }
            }
        }
    }
}
