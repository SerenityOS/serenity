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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

/*
 * @test
 * @bug 8202788
 * @summary Test reclamation of thread-local temporary direct byte buffers at thread exit
 * @modules java.management
 * @run main/othervm TempDirectBuffersReclamation
 */
public class TempDirectBuffersReclamation {

    public static void main(String[] args) throws IOException {

        BufferPoolMXBean dbPool = ManagementFactory
            .getPlatformMXBeans(BufferPoolMXBean.class)
            .stream()
            .filter(bp -> bp.getName().equals("direct"))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Can't obtain direct BufferPoolMXBean"));

        long count0 = dbPool.getCount();
        long memoryUsed0 = dbPool.getMemoryUsed();

        Thread thread = new Thread(TempDirectBuffersReclamation::doFileChannelWrite);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        long count1 = dbPool.getCount();
        long memoryUsed1 = dbPool.getMemoryUsed();

        if (count0 != count1 || memoryUsed0 != memoryUsed1) {
            throw new AssertionError(
                "Direct BufferPool not same before thread activity and after thread exit.\n" +
                "Before: # of buffers: " + count0 + ", memory used: " + memoryUsed0 + "\n" +
                " After: # of buffers: " + count1 + ", memory used: " + memoryUsed1 + "\n"
            );
        }
    }

    static void doFileChannelWrite() {
        try {
            Path file = Files.createTempFile("test", ".tmp");
            try (FileChannel fc = FileChannel.open(file, CREATE, WRITE, TRUNCATE_EXISTING)) {
                fc.write(ByteBuffer.wrap("HELLO".getBytes(StandardCharsets.UTF_8)));
            } finally {
                Files.delete(file);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
