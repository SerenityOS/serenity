/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.startupargs;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;

/**
 * @test
 * @summary Start a recording with custom settings
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @modules jdk.jfr/jdk.jfr.internal
 *
 * @run main/othervm -XX:StartFlightRecording:jdk.JVMInformation#enabled=false
 *      jdk.jfr.startupargs.TestEventSettings knownSetting
 *
 * @run main/othervm -XX:StartFlightRecording:com.example.Hello#stackTrace=true
 *      jdk.jfr.startupargs.TestEventSettings unknownSetting
 *
 * @run main/othervm -XX:StartFlightRecording:+HelloWorld#enabled=true
 *      jdk.jfr.startupargs.TestEventSettings addedUnknownSetting
 *
 * @run main/othervm
 *      -XX:StartFlightRecording:+A.B#enabled=true,+C.D#enabled=false
 *      jdk.jfr.startupargs.TestEventSettings multipleSettings
 *
 * @run main/othervm
 *      -XX:StartFlightRecording:class-loading=true,socket-threshold=100ms
 *      jdk.jfr.startupargs.TestEventSettings jfcOptions
 */
public class TestEventSettings {

    public static void main(String... args) throws Exception {
        String subTest = args[0];
        System.out.println(subTest);

        switch (subTest) {
        case "knownSetting" -> assertSetting("jdk.JVMInformation#enabled","false");
        case "unknownSetting" -> assertSetting("com.example.Hello#stackTrace", null);
        case "addedUnknownSetting" -> assertSetting("HelloWorld#enabled", "true");
        case "multipleSettings" -> {
            assertSetting("A.B#enabled", "true");
            assertSetting("C.D#enabled", "false");
        }
        case "jfcOptions" -> {
            assertSetting("jdk.ClassDefine#enabled","true");
            assertSetting("jdk.SocketRead#threshold", "100 ms");
        }
        default -> throw new Exception("Uknown tes " + subTest);
        }
    }

    private static void assertSetting(String key, String value) throws Exception {
        List<Recording> rs = FlightRecorder.getFlightRecorder().getRecordings();
        if (rs.isEmpty()) {
            throw new Exception("No recording started");
        }
        if (rs.size() != 1) {
            throw new Exception("Expected only one recording");
        }
        Map<String, String> currentSettings = rs.get(0).getSettings();
        String s = currentSettings.get(key);
        if (!Objects.equals(s, value)) {
            System.out.println("Key:" + key);
            System.out.println("Value:" + value);
            System.out.println("Result: " + s);
            System.out.println("All Setting:");
            for (var entry : currentSettings.entrySet()) {
                System.out.println(entry.getKey() + "=" + entry.getValue());
            }
            throw new Exception("Expected: " + value + " for " + key + " , got: " + s);
        }
    }
}
