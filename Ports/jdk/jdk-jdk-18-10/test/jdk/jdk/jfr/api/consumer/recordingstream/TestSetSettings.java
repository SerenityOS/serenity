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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import jdk.jfr.Enabled;
import jdk.jfr.Event;
import jdk.jfr.Name;
import jdk.jfr.consumer.RecordingStream;

/**
 * @test
 * @summary Tests RecordingStream::setSettings
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm -Xlog:jfr+system+parser jdk.jfr.api.consumer.recordingstream.TestSetSettings
 */
public final class TestSetSettings {

    @Name("LateBloomer")
    @Enabled(false)
    private final static class LateBloomer extends Event {
    }

    private static CountDownLatch lateBloomer = new CountDownLatch(1);

    public static void main(String... args) throws Exception {
        try (RecordingStream r = new RecordingStream()) {
            r.startAsync();
            Map<String, String> settings = new HashMap<String, String>();
            settings.put("LateBloomer#enabled", "true");
            r.setSettings(settings);
            r.onEvent("LateBloomer", e -> {
                lateBloomer.countDown();
            });
            LateBloomer event = new LateBloomer();
            event.commit();
            lateBloomer.await();
        }
    }
}
