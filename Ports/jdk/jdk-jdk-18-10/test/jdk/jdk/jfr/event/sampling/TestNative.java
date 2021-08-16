/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.sampling;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordingStream;
import jdk.jfr.internal.JVM;
import jdk.test.lib.jfr.EventNames;

/*
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @modules jdk.jfr/jdk.jfr.internal
 * @run main jdk.jfr.event.sampling.TestNative
 */
public class TestNative {

    final static String NATIVE_EVENT = EventNames.NativeMethodSample;

    static volatile boolean alive = true;

    public static void main(String[] args) throws Exception {
        try (RecordingStream rs = new RecordingStream()) {
            rs.enable(NATIVE_EVENT).withPeriod(Duration.ofMillis(1));
            rs.onEvent(NATIVE_EVENT, e -> {
                alive = false;
                rs.close();
            });
            Thread t = new Thread(TestNative::nativeMethod);
            t.setDaemon(true);
            t.start();
            rs.start();
        }

    }

    public static void nativeMethod() {
        while (alive) {
            JVM.getJVM().getPid();
        }
    }
}
