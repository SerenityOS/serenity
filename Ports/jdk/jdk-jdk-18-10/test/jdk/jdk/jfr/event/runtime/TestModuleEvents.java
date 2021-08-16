/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

import static jdk.test.lib.Asserts.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @summary Tests the JFR events related to modules
 * @key jfr
 * @requires vm.hasJFR
 * @requires !vm.graal.enabled
 * @library /test/lib
 * @run main/othervm --limit-modules java.base,jdk.jfr jdk.jfr.event.runtime.TestModuleEvents
 */
public final class TestModuleEvents {

    private static final String MODULE_EXPORT_EVENT_NAME = EventNames.ModuleExport;
    private static final String MODULE_REQUIRE_EVENT_NAME = EventNames.ModuleRequire;
    private static final String UNNAMED = "<unnamed>";

    public static void main(String[] args) throws Throwable {
        verifyRequiredModules();
        verifyExportedModules();
    }

    private static void verifyRequiredModules() throws Throwable {
        Recording recording = new Recording();
        recording.enable(MODULE_REQUIRE_EVENT_NAME);

        recording.start();
        recording.stop();

        List<RecordedEvent> events = Events.fromRecording(recording);
        assertDependency(events, "jdk.jfr", "java.base"); // jdk.jfr requires java.base (by default)
        assertDependency(events, "java.base", "jdk.jfr"); // java.base requires jdk.jfr for JDK events, i.e. FileRead

        recording.close();
    }

    private static void assertDependency(List<RecordedEvent> events, String source, String required) throws Exception {
        for (RecordedEvent e : events) {
            String sourceModule = e.getValue("source.name");
            if (source.equals(sourceModule)) {
                RecordedObject module = e.getValue("requiredModule");
                if (module != null) {
                    if (required.equals(module.getValue("name"))) {
                        return;
                    }
                }
            }
        }
        throw new Exception("Could not find module dependency between " + source + " and requires modeule "+ required);
    }

    private static void verifyExportedModules() throws Throwable {
        Recording recording = new Recording();
        recording.enable(MODULE_EXPORT_EVENT_NAME);
        recording.start();
        recording.stop();

        Map<String, String> edges = new HashMap<>();

        List<RecordedEvent> events = Events.fromRecording(recording);
        events.stream().forEach((ev) -> {
            String exportedPackage = getValue(ev.getValue("exportedPackage"), "name", UNNAMED);
            String toModule = getValue(ev.getValue("targetModule"), "name", UNNAMED);
            if (!toModule.equals("jdk.proxy1")) { // ignore jdk.proxy1 module
                edges.put(exportedPackage, toModule);
            }
        });

        // We expect
        // 1) jdk.jfr -> <unnamed> (because we use the package)
        // 2) java.util -> <unnamed> (because we use the package)
        // 3) jdk.jfr.events -> java.base (from the jfr design)
        // 4) jdk.internal -> jdk.jfr (from the jfr design)
        // Where 'a -> b' means "package 'a' exported to module 'b'"
        assertEquals(edges.get("jdk/jfr"), UNNAMED);
        assertEquals(edges.get("java/util"), UNNAMED);
        assertEquals(edges.get("jdk/jfr/events"), "java.base");
        assertEquals(edges.get("jdk/internal/vm/annotation"), "jdk.jfr");

        recording.close();
    }

    // Helper function to get field from a RecordedObject
    private static String getValue(RecordedObject ro, String field, String defVal) {
        if (ro != null && ro.getValue(field) != null) {
            return ro.getValue(field);
        } else {
            return defVal;
        }
    }
}
