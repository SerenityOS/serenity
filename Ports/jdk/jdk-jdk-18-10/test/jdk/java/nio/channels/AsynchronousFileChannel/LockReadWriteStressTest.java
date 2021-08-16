/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8184157
 * @summary Ensure that correct PendingFuture is used in Iocp completion status event handler
 * @requires (os.family == "windows")
 */

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardOpenOption.*;

public class LockReadWriteStressTest {
    public static void main(String[] args) throws Exception {
        Path path = Path.of("blah");
        ByteBuffer buf = ByteBuffer.allocate(16);
        for (int i=0; i < 1000; i++) {
            try (AsynchronousFileChannel ch = AsynchronousFileChannel.open(path,READ, WRITE, CREATE)) {
                FileLock lock = ch.lock().get();
                ch.read(buf, 0).get();
                buf.rewind();
                ch.write(buf, 0).get();
                lock.release();
                buf.clear();
            }
        }
    }
}
