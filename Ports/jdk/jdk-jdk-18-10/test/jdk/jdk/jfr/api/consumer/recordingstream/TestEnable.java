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

package jdk.jfr.api.consumer.recordingstream;

import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import jdk.jfr.Enabled;
import jdk.jfr.Event;
import jdk.jfr.consumer.RecordingStream;

/**
 * @test
 * @summary Tests RecordingStream::enable(...)
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.consumer.recordingstream.TestEnable
 */
public class TestEnable {

    @Enabled(false)
    private static class EnabledEvent extends Event {
    }

    public static void main(String... args) throws Exception {
        testEnableWithClass();
        testEnableWithEventName();
    }

    private static void testEnableWithEventName() {
        test(r -> r.enable(EnabledEvent.class.getName()));
    }

    private static void testEnableWithClass() {
        test(r -> r.enable(EnabledEvent.class));
    }

    private static void test(Consumer<RecordingStream> enablement) {
        CountDownLatch event = new CountDownLatch(1);
        try (RecordingStream r = new RecordingStream()) {
            r.onEvent(e -> {
                event.countDown();
            });
            enablement.accept(r);
            r.startAsync();
            EnabledEvent e = new EnabledEvent();
            e.commit();
            try {
                event.await();
            } catch (InterruptedException ie) {
                throw new RuntimeException("Unexpected interruption of latch", ie);
            }
        }
    }
}
