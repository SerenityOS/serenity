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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import jdk.jfr.Event;
import jdk.jfr.consumer.RecordingStream;

/**
 * @test
 * @summary Tests RecordingStream::disable(...)
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.consumer.recordingstream.TestDisable
 */
public class TestDisable {

    private static class DisabledEvent extends Event {
    }

    private static class EnabledEvent extends Event {
    }

    public static void main(String... args) throws Exception {
        testDisableWithClass();
        testDisableWithEventName();
    }

    private static void testDisableWithEventName() {
        test(r -> r.disable(DisabledEvent.class.getName()));
    }

    private static void testDisableWithClass() {
        test(r -> r.disable(DisabledEvent.class));
    }

    private static void test(Consumer<RecordingStream> disablement) {
        CountDownLatch twoEvent = new CountDownLatch(2);
        AtomicBoolean fail = new AtomicBoolean(false);
        try(RecordingStream r = new RecordingStream()) {
            r.onEvent(e -> {
                if (e.getEventType().getName().equals(DisabledEvent.class.getName())) {
                    fail.set(true);
                }
                twoEvent.countDown();
            });
            disablement.accept(r);
            r.startAsync();
            EnabledEvent e1 = new EnabledEvent();
            e1.commit();
            DisabledEvent d1 = new DisabledEvent();
            d1.commit();
            EnabledEvent e2 = new EnabledEvent();
            e2.commit();
            try {
                twoEvent.await();
            } catch (InterruptedException ie) {
                throw new RuntimeException("Unexpexpected interruption of thread", ie);
            }
            if (fail.get()) {
                throw new RuntimeException("Should not receive a disabled event");
            }
        }
    }
}
