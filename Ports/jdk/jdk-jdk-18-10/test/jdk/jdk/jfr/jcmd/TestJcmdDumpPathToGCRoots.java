/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.jfr.Enabled;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;
import jdk.jfr.consumer.RecordingFile;
import jdk.jfr.internal.test.WhiteBox;
import jdk.test.lib.jfr.EventNames;

/**
 * @test
 * @summary Start a recording with or without path-to-gc-roots
 * @requires vm.hasJFR
 * @modules jdk.jfr/jdk.jfr.internal.test
 * @library /test/lib /test/jdk
 * @key jfr
 *
 * @run main/othervm -XX:TLABSize=2k jdk.jfr.jcmd.TestJcmdDumpPathToGCRoots
 */
public class TestJcmdDumpPathToGCRoots {

    private static final int OBJECT_COUNT = 100_000;
    public static List<Object[]> leak = new ArrayList<>(OBJECT_COUNT);

    public static void main(String[] args) throws Exception {
        WhiteBox.setWriteAllObjectSamples(true);

        String settingName = EventNames.OldObjectSample + "#" + "cutoff";

        // dump parameter trumps previous setting
        testDump("path-to-gc-roots=true", Collections.singletonMap(settingName, "infinity"), true);
        testDump("path-to-gc-roots=true", Collections.singletonMap(settingName, "0 ns"), true);
        testDump("path-to-gc-roots=true", Collections.emptyMap(), true);

        testDump("path-to-gc-roots=false", Collections.singletonMap(settingName, "infinity"), false);
        testDump("path-to-gc-roots=false", Collections.singletonMap(settingName, "0 ns"), false);
        testDump("path-to-gc-roots=false", Collections.emptyMap(), false);

        testDump("", Collections.singletonMap(settingName, "infinity"), true);
        testDump("", Collections.singletonMap(settingName, "0 ns"), false);
        testDump("", Collections.emptyMap(), false);
    }

    private static void testDump(String pathToGcRoots, Map<String, String> settings, boolean expectedChains) throws Exception {
        try (Recording r = new Recording()) {
            Map<String, String> p = new HashMap<>(settings);
            p.put(EventNames.OldObjectSample + "#" + Enabled.NAME, "true");
            r.setName("dodo");
            r.setSettings(p);
            r.setToDisk(true);
            r.start();
            clearLeak();
            System.out.println("Recording id: " + r.getId());
            System.out.println("Settings: " + settings.toString());
            System.out.println("Command: JFR.dump " + pathToGcRoots);
            System.out.println("Chains expected: " + expectedChains);
            buildLeak();
            System.gc();
            System.gc();
            File recording = new File("TestJcmdDumpPathToGCRoots" + r.getId() + ".jfr");
            recording.delete();
            JcmdHelper.jcmd("JFR.dump", "name=dodo", pathToGcRoots, "filename=" + recording.getAbsolutePath());
            r.setSettings(Collections.emptyMap());
            List<RecordedEvent> events = RecordingFile.readAllEvents(recording.toPath());
            if (events.isEmpty()) {
                throw new Exception("No events found in recoding");
            }
            boolean chains = hasChains(events);
            if (expectedChains && !chains) {
                System.out.println(events);
                throw new Exception("Expected chains but found none");
            }
            if (!expectedChains && chains) {
                System.out.println(events);
                throw new Exception("Didn't expect chains but found some");
            }
        }
    }

    private static void clearLeak() {
      leak.clear();
    }

    private static boolean hasChains(List<RecordedEvent> events) throws IOException {
        for (RecordedEvent e : events) {
            RecordedObject ro = e.getValue("object");
            if (ro.getValue("referrer") != null) {
                return true;
            }
        }
        return false;
    }

    private static void buildLeak() {
        for (int i = 0; i < OBJECT_COUNT;i ++) {
            leak.add(new Object[0]);
        }
    }
}
