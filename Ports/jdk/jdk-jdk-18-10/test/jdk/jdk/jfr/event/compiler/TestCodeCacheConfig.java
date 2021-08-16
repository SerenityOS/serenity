/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.compiler;

import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;
import sun.hotspot.WhiteBox;

/**
 * @test TestCodeCacheConfig
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:.
 *     -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *     -XX:+SegmentedCodeCache jdk.jfr.event.compiler.TestCodeCacheConfig
 * @run main/othervm -Xbootclasspath/a:.
 *     -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *     -XX:-SegmentedCodeCache jdk.jfr.event.compiler.TestCodeCacheConfig
 * @summary check "Code Cache Configuration" jfr event
 */
public class TestCodeCacheConfig {
    private final static String EVENT_NAME = EventNames.CodeCacheConfiguration;

    private static final long CodeCacheExpectedSize = WhiteBox.getWhiteBox().getUintxVMFlag("ReservedCodeCacheSize");

    public static void main(String[] args) throws Exception {
        Recording recording = new Recording();
        recording.enable(EVENT_NAME);
        recording.start();
        recording.stop();

        List<RecordedEvent> events = Events.fromRecording(recording);
        Events.hasEvents(events);
        RecordedEvent event = events.get(0);
        long initialSize = (long) event.getValue("initialSize");
        long reservedSize = (long) event.getValue("reservedSize");
        long nonNMethodSize = (long) event.getValue("nonNMethodSize");
        long profiledSize = (long) event.getValue("profiledSize");
        long nonProfiledSize = (long) event.getValue("nonProfiledSize");
        long expansionSize = (long) event.getValue("expansionSize");
        long minBlockLength = (long) event.getValue("minBlockLength");
        long startAddress = (long) event.getValue("startAddress");
        long reservedTopAddress = (long) event.getValue("reservedTopAddress");

        Asserts.assertGT(initialSize, 1024L,
            "initialSize less than 1024 byte, got " + initialSize);

        Asserts.assertEQ(reservedSize, CodeCacheExpectedSize,
            String.format("Unexpected reservedSize value. Expected %d but " + "got %d", CodeCacheExpectedSize, reservedSize));

        Asserts.assertLTE(nonNMethodSize, CodeCacheExpectedSize,
            String.format("Unexpected nonNMethodSize value. Expected <= %d but " + "got %d", CodeCacheExpectedSize, nonNMethodSize));

        Asserts.assertLTE(profiledSize, CodeCacheExpectedSize,
            String.format("Unexpected profiledSize value. Expected <= %d but " + "got %d", CodeCacheExpectedSize, profiledSize));

        Asserts.assertLTE(nonProfiledSize, CodeCacheExpectedSize,
            String.format("Unexpected nonProfiledSize value. Expected <= %d but " + "got %d", CodeCacheExpectedSize, nonProfiledSize));

        Asserts.assertGTE(expansionSize, 1024L,
            "expansionSize less than 1024 " + "bytes, got " + expansionSize);

        Asserts.assertGTE(minBlockLength, 1L,
            "minBlockLength less than 1 byte, got " + minBlockLength);

        Asserts.assertNE(startAddress, 0L,
            "startAddress null");

        Asserts.assertNE(reservedTopAddress, 0L,
            "codeCacheReservedTopAddr null");
    }
}
