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

package jdk.jfr.api.settings;

import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Recording;
import jdk.jfr.SettingDefinition;
import jdk.test.lib.jfr.Events;

import static jdk.test.lib.Asserts.assertEquals;

/**
 * @test
 * @summary The test uses SettingControl
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.api.settings.TestFilterEvents
 */
public class TestFilterEvents {

    private static class AbstractHTTPEvent extends Event {
        @Label("HTTP URI")
        protected String uri;

        @Label("URI Filter")
        @SettingDefinition
        protected boolean uriFilter(RegExpControl control) {
            return control.matches(uri);
        }
    }

    private static final class HTTPGetEvent extends AbstractHTTPEvent {
        @Label("Thread Names")
        @Description("List of thread names to accept, such as \"main\" or \"workerThread1\", \"taskThread\"")
        @SettingDefinition
        private boolean threadNames(StringListSetting setting) {
            return setting.accept(Thread.currentThread().getName());
        }

    }
    private static final class HTTPPostEvent extends AbstractHTTPEvent {
    }

    public static void main(String[] args) throws Exception {
        Recording continuous = new Recording();
        continuous.enable(HTTPGetEvent.class).with("threadNames", "\"unused-threadname-1\"");
        assertEquals(0, makeProfilingRecording("\"unused-threadname-2\""));
        assertEquals(1, makeProfilingRecording("\"" + Thread.currentThread().getName() + "\""));
        continuous.close();
    }

    private static int makeProfilingRecording(String threadNames) throws Exception {
        try (Recording recording = new Recording()) {
            recording.enable(HTTPGetEvent.class).with("threadNames", threadNames);
            recording.enable(HTTPGetEvent.class).with("uriFilter", "https://www.example.com/list/.*");
            recording.enable(HTTPPostEvent.class).with("uriFilter", "https://www.example.com/list/.*");
            recording.start();

            HTTPGetEvent getEvent = new HTTPGetEvent();
            getEvent.uri = "https://www.example.com/list/item?id=4";
            getEvent.commit();

            HTTPPostEvent postEvent = new HTTPPostEvent();
            postEvent.uri = "https://www.example.com/admin/login?name=john";
            postEvent.commit();

            recording.stop();

            return Events.fromRecording(recording).size();
        }
    }

}
