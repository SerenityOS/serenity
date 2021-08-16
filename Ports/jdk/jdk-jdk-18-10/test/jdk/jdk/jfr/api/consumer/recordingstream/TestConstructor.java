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

import jdk.jfr.Configuration;
import jdk.jfr.Enabled;
import jdk.jfr.consumer.RecordingStream;
import jdk.test.lib.jfr.EventNames;

/**
 * @test
 * @summary Tests RecordingStream::RecordingStream()
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.consumer.recordingstream.TestConstructor
 */
public class TestConstructor {

    public static void main(String... args) throws Exception {
        testEmpty();
        testConfiguration();
    }

    private static void testConfiguration() throws Exception {
        CountDownLatch jvmInformation = new CountDownLatch(1);
        Configuration c = Configuration.getConfiguration("default");
        if (!c.getSettings().containsKey(EventNames.JVMInformation + "#" + Enabled.NAME)) {
            throw new Exception("Expected default configuration to contain enabled " + EventNames.JVMInformation);
        }
        RecordingStream r = new RecordingStream(c);
        r.onEvent("jdk.JVMInformation", e -> {
            jvmInformation.countDown();
        });
        r.startAsync();
        jvmInformation.await();
        r.close();
    }

    private static void testEmpty() {
        RecordingStream r = new RecordingStream();
        r.close();
    }
}
