/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.jcmd;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.Utils;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.FileHelper;
import jdk.test.lib.process.OutputAnalyzer;

/**
 * @test TestClassId
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @modules jdk.jfr/jdk.jfr.internal
 * @run main/othervm jdk.jfr.jcmd.TestJcmdLegacy
 */
public class TestJcmdLegacy {

    private static final String DIR = System.getProperty("test.src", ".");
    private static final File SETTINGS = new File(DIR, "legacy.jfc");

    private static final String LEGACY_EVENT = "com.oracle.jdk.JVMInformation";

    public static void main(String... args) throws Exception {
        testAPI();
        testJcmd();
    }

    private static void testJcmd() throws Exception {
        String name = "testLegacy";
        Path p = Paths.get(name + ".jfr").toAbsolutePath().normalize();
        OutputAnalyzer output = JcmdHelper.jcmd("JFR.start", "name=" + name, "settings=" + SETTINGS.getCanonicalPath());
        JcmdAsserts.assertRecordingHasStarted(output);
        JcmdHelper.waitUntilRunning(name);
        JcmdHelper.stopWriteToFileAndCheck(name, p.toFile());
        FileHelper.verifyRecording(p.toFile());
        verify(p);
    }

    private static void testAPI() throws IOException, Exception {
        Path p = Utils.createTempFile("enable-legacy-event", ".jfr");

        try (Recording r = new Recording()) {
            r.enable(LEGACY_EVENT);
            r.start();
            r.stop();
            r.dump(p);
            verify(p);
        }
    }

    private static void verify(Path p) throws IOException, Exception {
        for (RecordedEvent e : RecordingFile.readAllEvents(p)) {
            System.out.println(e.getEventType().getName());
            if (e.getEventType().getName().equals(EventNames.JVMInformation)) {
                return;
            }
        }
        throw new Exception("Could not find legacy event");
    }
}
