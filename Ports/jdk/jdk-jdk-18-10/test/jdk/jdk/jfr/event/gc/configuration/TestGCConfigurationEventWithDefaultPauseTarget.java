/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.gc.configuration;

import static jdk.test.lib.Asserts.assertGreaterThanOrEqual;

import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.EventVerifier;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:-UseFastUnorderedTimeStamps jdk.jfr.event.gc.configuration.TestGCConfigurationEventWithDefaultPauseTarget
 */
public class TestGCConfigurationEventWithDefaultPauseTarget {
    public static void main(String[] args) throws Exception {
        Recording recording = new Recording();
        recording.enable(EventNames.GCConfiguration);
        recording.start();
        recording.stop();
        List<RecordedEvent> events = Events.fromRecording(recording);
        assertGreaterThanOrEqual(events.size(), 1, "Expected at least 1 event");
        DefaultGCConfigurationVerifier verifier = new DefaultGCConfigurationVerifier(events.get(0));
        verifier.verify();
    }
}

class DefaultGCConfigurationVerifier extends EventVerifier {
    public DefaultGCConfigurationVerifier(RecordedEvent event) {
        super(event);
    }

    @Override
    public void verify() throws Exception {
        verifyEquals("pauseTarget", Long.MIN_VALUE);
    }
}
