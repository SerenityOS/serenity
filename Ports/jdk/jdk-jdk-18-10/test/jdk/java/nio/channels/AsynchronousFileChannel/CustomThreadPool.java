/*
 * Copyright (c) 2008, 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4607272 6842687
 * @summary Unit test for java.nio.channels.AsynchronousFileChannel
 * @build CustomThreadPool MyThreadFactory
 * @run main/othervm -Djava.nio.channels.DefaultThreadPool.threadFactory=MyThreadFactory CustomThreadPool
 */

import java.io.File;
import static java.nio.file.StandardOpenOption.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.concurrent.atomic.AtomicReference;

public class CustomThreadPool {

    public static void main(String[] args) throws Exception {
        File blah = File.createTempFile("blah", null);
        blah.deleteOnExit();
        AsynchronousFileChannel ch =
            AsynchronousFileChannel.open(blah.toPath(), READ, WRITE);
        ByteBuffer src = ByteBuffer.wrap("Scooby Snacks".getBytes());

        final AtomicReference<Thread> invoker = new AtomicReference<Thread>();
        ch.write(src, 0, invoker,
            new CompletionHandler<Integer,AtomicReference<Thread>>() {
                public void completed(Integer result, AtomicReference<Thread> invoker) {
                    invoker.set(Thread.currentThread());
                }
                public void failed(Throwable exc, AtomicReference<Thread> invoker) {
                }
            });
        Thread t;
        while ((t = invoker.get()) == null) {
            Thread.sleep(100);
        }
        ch.close();

        // check handler was run by known thread
        if (!MyThreadFactory.created(t))
            throw new RuntimeException("Handler invoked by unknown thread");
    }
}
