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

package jdk.jfr.api.consumer.filestream;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;

import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.jfr.consumer.EventStream;

/**
 * @test
 * @summary Verifies that it is possible to stream contents from a multichunked file
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.consumer.filestream.TestMultipleChunk
 */
public class TestMultipleChunk {

    static class SnakeEvent extends Event {
        int id;
    }

    public static void main(String... args) throws Exception {
        Path path = Paths.get("./using-file.jfr");
        try (Recording r1 = new Recording()) {
            r1.start();
            emitSnakeEvent(1);
            emitSnakeEvent(2);
            emitSnakeEvent(3);
            // Force a chunk rotation
            try (Recording r2 = new Recording()) {
                r2.start();
                emitSnakeEvent(4);
                emitSnakeEvent(5);
                emitSnakeEvent(6);
                r2.stop();
            }
            r1.stop();
            r1.dump(path);
            AtomicLong counter = new AtomicLong();
            try (EventStream es = EventStream.openFile(path)) {
                es.onEvent(e -> {
                    counter.incrementAndGet();
                });
                es.start();
                if (counter.get() != 6) {
                    throw new Exception("Expected 6 event, but got " + counter.get());
                }
            }
        }
    }

    static void emitSnakeEvent(int id) {
        SnakeEvent e = new SnakeEvent();
        e.id = id;
        e.commit();
    }

}
