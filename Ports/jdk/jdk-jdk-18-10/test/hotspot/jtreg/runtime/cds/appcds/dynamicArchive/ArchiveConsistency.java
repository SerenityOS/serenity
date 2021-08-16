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
 *
 */

/*
 * @test
 * @summary Corrupt the header CRC fields of the top archive. VM should exit with an error.
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds /test/hotspot/jtreg/runtime/cds/appcds/test-classes
 * @build Hello sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar hello.jar Hello
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI ArchiveConsistency
 */

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import sun.hotspot.WhiteBox;
import jdk.test.lib.helpers.ClassFileInstaller;

public class ArchiveConsistency extends DynamicArchiveTestBase {
    public static WhiteBox wb;
    public static int int_size;        // size of int
    public static String[] shared_region_name = {"ReadWrite", "ReadOnly", "BitMap"};
    public static int num_regions = shared_region_name.length;

    public static void main(String[] args) throws Exception {
        runTest(ArchiveConsistency::testCustomBase);
    }

    // Test with custom base archive + top archive
    static void testCustomBase() throws Exception {
        String topArchiveName = getNewArchiveName("top2");
        String baseArchiveName = getNewArchiveName("base");
        TestCommon.dumpBaseArchive(baseArchiveName);
        doTest(baseArchiveName, topArchiveName);
    }

    public static void setReadWritePermission(File file) throws Exception {
        if (!file.canRead()) {
            if (!file.setReadable(true)) {
                throw new IOException("Cannot modify file " + file + " as readable");
            }
        }
        if (!file.canWrite()) {
            if (!file.setWritable(true)) {
                throw new IOException("Cannot modify file " + file + " as writable");
            }
        }
    }

    public static long readInt(FileChannel fc, long offset, int nbytes) throws Exception {
        ByteBuffer bb = ByteBuffer.allocate(nbytes);
        bb.order(ByteOrder.nativeOrder());
        fc.position(offset);
        fc.read(bb);
        return  (nbytes > 4 ? bb.getLong(0) : bb.getInt(0));
    }

    public static long align_up_page(long l) throws Exception {
        // wb is obtained in getFileOffsetInfo() which is called first in main() else we should call
        // WhiteBox.getWhiteBox() here first.
        int pageSize = wb.getVMPageSize();
        return (l + pageSize -1) & (~ (pageSize - 1));
    }

    public static void writeData(FileChannel fc, long offset, ByteBuffer bb) throws Exception {
        fc.position(offset);
        fc.write(bb);
        fc.force(true);
    }

    public static FileChannel getFileChannel(File jsa) throws Exception {
        List<StandardOpenOption> arry = new ArrayList<StandardOpenOption>();
        arry.add(READ);
        arry.add(WRITE);
        return FileChannel.open(jsa.toPath(), new HashSet<StandardOpenOption>(arry));
    }

   public static void modifyJsaHeaderCRC(File jsa) throws Exception {
        FileChannel fc = getFileChannel(jsa);
        int_size = wb.getOffsetForName("int_size");
        System.out.println("    int_size " + int_size);
        ByteBuffer bbuf = ByteBuffer.allocateDirect(int_size);
        for (int i = 0; i < int_size; i++) {
            bbuf.put((byte)0);
        }

        int baseArchiveCRCOffset = wb.getOffsetForName("DynamicArchiveHeader::_base_region_crc");
        int crc = 0;
        System.out.printf("%-12s%-12s\n", "Space name", "CRC");
        for (int i = 0; i < num_regions; i++) {
            baseArchiveCRCOffset += int_size * i;
            System.out.println("    baseArchiveCRCOffset " + baseArchiveCRCOffset);
            crc = (int)readInt(fc, baseArchiveCRCOffset, int_size );
            System.out.printf("%-11s%-12d\n", shared_region_name[i], crc);
            bbuf.rewind();
            writeData(fc, baseArchiveCRCOffset, bbuf);
        }
        fc.force(true);
        if (fc.isOpen()) {
            fc.close();
        }
    }

    private static void doTest(String baseArchiveName, String topArchiveName) throws Exception {
        String appJar = ClassFileInstaller.getJarPath("hello.jar");
        String mainClass = "Hello";
        dump2(baseArchiveName, topArchiveName,
             "-Xlog:cds",
             "-Xlog:cds+dynamic=debug",
             "-cp", appJar, mainClass)
            .assertNormalExit(output -> {
                    output.shouldContain("Written dynamic archive 0x");
                });

        File jsa = new File(topArchiveName);
        if (!jsa.exists()) {
            throw new IOException(jsa + " does not exist!");
        }

        // Modify the CRC values in the header of the top archive.
        wb = WhiteBox.getWhiteBox();
        setReadWritePermission(jsa);
        modifyJsaHeaderCRC(jsa);

        run2(baseArchiveName, topArchiveName,
            "-Xlog:class+load",
            "-Xlog:cds+dynamic=debug,cds=debug",
            "-XX:+VerifySharedSpaces",
            "-cp", appJar, mainClass)
            .assertAbnormalExit(output -> {
                    output.shouldContain("Header checksum verification failed")
                          .shouldContain("Unable to use shared archive")
                          .shouldHaveExitValue(1);
                });
    }
}
