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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.jfr.consumer.EventStream;
import jdk.jfr.consumer.RecordedEvent;

/**
 * @test
 * @summary Test EventStream::setReuse(...)
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.consumer.filestream.TestReuse
 */
public class TestReuse {

    static class ReuseEvent extends Event {
    }

    private static final boolean[] BOOLEAN_STATES = { false, true };

    public static void main(String... args) throws Exception {
        Path p = makeRecording();

        testSetReuseTrue(p);
        testSetReuseFalse(p);
    }

    private static void testSetReuseFalse(Path p) throws Exception {
        for (boolean ordered : BOOLEAN_STATES) {
            AtomicBoolean fail = new AtomicBoolean(false);
            Map<RecordedEvent, RecordedEvent> identity = new IdentityHashMap<>();
            try (EventStream es = EventStream.openFile(p)) {
                es.setOrdered(ordered);
                es.setReuse(false);
                es.onEvent(e -> {
                    if (identity.containsKey(e)) {
                        fail.set(true);
                        es.close();
                    }
                    identity.put(e, e);
                });
                es.start();
            }
            if (fail.get()) {
                throw new Exception("Unexpected reuse! Ordered = " + ordered);
            }

        }
    }

    private static void testSetReuseTrue(Path p) throws Exception {
        for (boolean ordered : BOOLEAN_STATES) {
            AtomicBoolean success = new AtomicBoolean(false);
            Map<RecordedEvent, RecordedEvent> events = new IdentityHashMap<>();
            try (EventStream es = EventStream.openFile(p)) {
                es.setOrdered(ordered);
                es.setReuse(true);
                es.onEvent(e -> {
                    if(events.containsKey(e)) {
                        success.set(true);;
                        es.close();
                    }
                    events.put(e,e);
                });
                es.start();
            }
            if (!success.get()) {
                throw new Exception("No reuse! Ordered = " + ordered);
            }
        }

    }

    private static Path makeRecording() throws IOException {
        try (Recording r = new Recording()) {
            r.start();
            for (int i = 0; i < 5; i++) {
                ReuseEvent e = new ReuseEvent();
                e.commit();
            }
            Recording rotation = new Recording();
            rotation.start();
            for (int i = 0; i < 5; i++) {
                ReuseEvent e = new ReuseEvent();
                e.commit();
            }
            r.stop();
            rotation.close();
            Path p = Files.createTempFile("recording", ".jfr");
            r.dump(p);
            return p;
        }
    }
}
