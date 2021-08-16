/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.runtime;

import static jdk.test.lib.Asserts.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Platform;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @bug 8216559
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.event.runtime.TestNativeLibrariesEvent
 */
public class TestNativeLibrariesEvent {

    private final static String EVENT_NAME = EventNames.NativeLibrary;

    public static void main(String[] args) throws Throwable {
        Recording recording = new Recording();
        recording.enable(EVENT_NAME);
        recording.start();
        recording.stop();

        List<String> expectedLibs = getExpectedLibs();
        for (RecordedEvent event : Events.fromRecording(recording)) {
            System.out.println("Event:" + event);
            long unsignedTopAddress = event.getValue("topAddress");
            long unsignedBaseAddress = event.getValue("baseAddress");
            assertValidAddresses(unsignedBaseAddress, unsignedTopAddress);
            String lib = Events.assertField(event, "name").notEmpty().getValue();
            for (String expectedLib : new ArrayList<>(expectedLibs)) {
                if (lib.contains(expectedLib)) {
                    expectedLibs.remove(expectedLib);
                }
            }
        }
        assertTrue(expectedLibs.isEmpty(), "Missing libraries:" + expectedLibs.stream().collect(Collectors.joining(", ")));
    }

    private static List<String> getExpectedLibs() throws Throwable {
        String libTemplate = null;
        if (Platform.isWindows()) {
            libTemplate = "%s.dll";
        } else if (Platform.isOSX()) {
            libTemplate = "lib%s.dylib";
        } else if (Platform.isLinux()) {
            libTemplate = "lib%s.so";
        }
        if (libTemplate == null) {
            throw new Exception("Unsupported OS");
        }

        List<String> libs = new ArrayList<String>();
        String[] names = { "jvm", "java", "zip" };
        for (String name : names) {
            libs.add(String.format(libTemplate, name));
        }
        return libs;
    }

    private static void assertValidAddresses(long unsignedBaseAddress, long unsignedTopAddress) throws Exception {
        if (unsignedTopAddress != 0) { // guard against missing value (0)
            if (Long.compareUnsigned(unsignedTopAddress, unsignedBaseAddress) < 0) {
                throw new Exception("Top address " + Long.toHexString(unsignedTopAddress) + " is below base addess " + Long.toHexString(unsignedBaseAddress));
            }
        }
    }
}
