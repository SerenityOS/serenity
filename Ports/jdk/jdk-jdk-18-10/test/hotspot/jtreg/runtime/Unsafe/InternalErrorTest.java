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

/*
 * @test
 * @bug 8191278
 * @requires os.family != "windows"
 * @summary Check that SIGBUS errors caused by memory accesses in Unsafe_CopyMemory()
 * and UnsafeCopySwapMemory() get converted to java.lang.InternalError exceptions.
 * @modules java.base/jdk.internal.misc
 *          java.base/java.nio:+open
 *
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm -XX:CompileCommand=exclude,*InternalErrorTest.main -XX:CompileCommand=inline,*.get -XX:CompileCommand=inline,*Unsafe.* -Xbootclasspath/a:.  -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI InternalErrorTest
 */

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import jdk.internal.misc.Unsafe;
import sun.hotspot.WhiteBox;

// Test that illegal memory access errors in Unsafe_CopyMemory0() and
// UnsafeCopySwapMemory() that cause SIGBUS errors result in
// java.lang.InternalError exceptions, not JVM crashes.
public class InternalErrorTest {

    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final int pageSize = WhiteBox.getWhiteBox().getVMPageSize();
    private static final String expectedErrorMsg = "fault occurred in a recent unsafe memory access";
    private static final String failureMsg1 = "InternalError not thrown";
    private static final String failureMsg2 = "Wrong InternalError: ";

    public static void main(String[] args) throws Throwable {
        Unsafe unsafe = Unsafe.getUnsafe();

        String currentDir = System.getProperty("test.classes");
        File file = new File(currentDir, "tmpFile.txt");

        StringBuilder s = new StringBuilder();
        for (int i = 1; i < pageSize + 1000; i++) {
            s.append("1");
        }
        Files.write(file.toPath(), s.toString().getBytes());
        FileChannel fileChannel = new RandomAccessFile(file, "r").getChannel();
        MappedByteBuffer buffer =
            fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());

        // Get address of mapped memory.
        long mapAddr = 0;
        try {
            Field af = java.nio.Buffer.class.getDeclaredField("address");
            af.setAccessible(true);
            mapAddr = af.getLong(buffer);
        } catch (Exception f) {
            throw f;
        }
        long allocMem = unsafe.allocateMemory(4000);

        for (int i = 0; i < 3; i++) {
            test(buffer, unsafe, mapAddr, allocMem, i);
        }

        Files.write(file.toPath(), "2".getBytes());
        buffer.position(buffer.position() + pageSize);
        for (int i = 0; i < 3; i++) {
            try {
                test(buffer, unsafe, mapAddr, allocMem, i);
                WhiteBox.getWhiteBox().forceSafepoint();
                throw new RuntimeException(failureMsg1);
            } catch (InternalError e) {
                if (!e.getMessage().contains(expectedErrorMsg)) {
                    throw new RuntimeException(failureMsg2 + e.getMessage());
                }
            }
        }

        Method m = InternalErrorTest.class.getMethod("test", MappedByteBuffer.class, Unsafe.class, long.class, long.class, int.class);
        WhiteBox.getWhiteBox().enqueueMethodForCompilation(m, 3);

        for (int i = 0; i < 3; i++) {
            try {
                test(buffer, unsafe, mapAddr, allocMem, i);
                WhiteBox.getWhiteBox().forceSafepoint();
                throw new RuntimeException(failureMsg1);
            } catch (InternalError e) {
                if (!e.getMessage().contains(expectedErrorMsg)) {
                    throw new RuntimeException(failureMsg2 + e.getMessage());
                }
            }
        }

        WhiteBox.getWhiteBox().enqueueMethodForCompilation(m, 4);

        for (int i = 0; i < 3; i++) {
            try {
                test(buffer, unsafe, mapAddr, allocMem, i);
                WhiteBox.getWhiteBox().forceSafepoint();
                throw new RuntimeException(failureMsg1);
            } catch (InternalError e) {
                if (!e.getMessage().contains(expectedErrorMsg)) {
                    throw new RuntimeException(failureMsg2 + e.getMessage());
                }
            }
        }

        System.out.println("Success");
    }

    public static void test(MappedByteBuffer buffer, Unsafe unsafe, long mapAddr, long allocMem, int type) {
        switch (type) {
            case 0:
                // testing Unsafe.copyMemory, trying to access a word from next page after truncation.
                buffer.get(new byte[8]);
                break;
            case 1:
                // testing Unsafe.copySwapMemory, trying to access next  page after truncation.
                unsafe.copySwapMemory(null, mapAddr + pageSize, new byte[4000], 16, 2000, 2);
                break;
            case 2:
                // testing Unsafe.copySwapMemory, trying to access next  page after truncation.
                unsafe.copySwapMemory(null, mapAddr + pageSize, null, allocMem, 2000, 2);
                break;
        }
    }

}
