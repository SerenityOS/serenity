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
 * @requires (vm.gc == "Parallel" | vm.gc == null)
 *           & vm.opt.ExplicitGCInvokesConcurrent != true
 * @library /test/lib /test/jdk
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:-UseFastUnorderedTimeStamps -XX:+UseParallelGC -XX:ParallelGCThreads=3 -XX:ConcGCThreads=2 -XX:+UseDynamicNumberOfGCThreads -XX:-ExplicitGCInvokesConcurrent -XX:-DisableExplicitGC -XX:MaxGCPauseMillis=800 -XX:GCTimeRatio=19 jdk.jfr.event.gc.configuration.TestGCConfigurationEvent
 */
public class TestGCConfigurationEvent {
    public static void main(String[] args) throws Exception {
        Recording recording = new Recording();
        recording.enable(EventNames.GCConfiguration);
        recording.start();
        recording.stop();
        List<RecordedEvent> events = Events.fromRecording(recording);
        assertGreaterThanOrEqual(events.size(), 1, "Expected at least 1 event");
        GCConfigurationEventVerifier verifier = new GCConfigurationEventVerifier(events.get(0));
        verifier.verify();
    }
}

class GCConfigurationEventVerifier extends EventVerifier {
    public GCConfigurationEventVerifier(RecordedEvent event) {
        super(event);
    }

    @Override
    public void verify() throws Exception {
        verifyYoungGCIs("ParallelScavenge");
        verifyOldGCIs("ParallelOld");
        verifyParallelGCThreadsIs(3);
        verifyConcurrentGCThreadsIs(2);
        verifyUsesDynamicGCThreadsIs(true);
        verifyIsExplicitGCConcurrentIs(false);
        verifyIsExplicitGCDisabledIs(false);
        verifyPauseTargetIs(800);
        verifyGCTimeRatioIs(19);
    }

    private void verifyYoungGCIs(String expected) throws Exception {
        verifyEquals("youngCollector", expected);
    }

    private void verifyOldGCIs(String expected) throws Exception {
        verifyEquals("oldCollector", expected);
    }

    private void verifyParallelGCThreadsIs(int expected) throws Exception {
        verifyEquals("parallelGCThreads", expected);
    }

    private void verifyConcurrentGCThreadsIs(int expected) throws Exception {
        verifyEquals("concurrentGCThreads", expected);
    }

    private void verifyUsesDynamicGCThreadsIs(boolean expected) throws Exception {
        verifyEquals("usesDynamicGCThreads", expected);
    }

    private void verifyIsExplicitGCConcurrentIs(boolean expected) throws Exception {
        verifyEquals("isExplicitGCConcurrent", expected);
    }

    private void verifyIsExplicitGCDisabledIs(boolean expected) throws Exception {
        verifyEquals("isExplicitGCDisabled", expected);
    }

    private void verifyPauseTargetIs(long expected) throws Exception {
        verifyEquals("pauseTarget", expected);
    }

    private void verifyGCTimeRatioIs(int expected) throws Exception {
        verifyEquals("gcTimeRatio", expected);
    }
}
