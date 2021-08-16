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

package jdk.jfr.api.event;

import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.Recording;
import jdk.jfr.ValueDescriptor;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @summary Test with event class inheritance
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.event.TestExtends
 */

public class TestExtends {

    private static final int DEFAULT_FIELD_COUNT = 4;

    @SuppressWarnings("unused")
    private static class GrandpaEvent extends Event {
        public int gPublicField = 4;
        protected int gProtectedField = 3;
        private int gPrivateField = 2;
        int gDefaultField = 1;
        private int hiddenField = 4711;
    }

    @SuppressWarnings("unused")
    private static class ParentEvent extends GrandpaEvent {
        public int pPublicField = 40;
        protected int pProtectedField = 30;
        private int pPrivateField = 20;
        int pDefaultField = 10;
        private boolean hiddenField = true;
    }

    @SuppressWarnings("unused")
    private static class MeEvent extends ParentEvent {
        public int mPublicField = 400;
        protected int mProtectedField = 300;
        private int mPrivateField = 200;
        int mDefaultField = 100;
        private String hiddenField = "Hidden";
    }

    public static void main(String[] args) throws Exception {
        Recording r = new Recording();
        r.enable(GrandpaEvent.class).withoutStackTrace();
        r.enable(ParentEvent.class).withStackTrace();
        r.enable(MeEvent.class).withoutStackTrace();
        r.start();

        GrandpaEvent g = new GrandpaEvent();
        g.commit();

        ParentEvent p = new ParentEvent();
        p.commit();

        MeEvent m = new MeEvent();
        m.commit();

        r.stop();
        for (RecordedEvent re : Events.fromRecording(r)) {
            System.out.println(re);
        }
        // Grandpa
        EventType grandpaType = EventType.getEventType(GrandpaEvent.class);
        verifyField(grandpaType, "gPublicField");
        verifyField(grandpaType, "gProtectedField");
        verifyField(grandpaType, "gPrivateField");
        verifyField(grandpaType, "gDefaultField");
        verifyField(grandpaType, "hiddenField");
        verifyFieldCount(grandpaType, 5);

        // Parent
        EventType parentType = EventType.getEventType(ParentEvent.class);
        verifyField(parentType, "gPublicField");
        verifyField(parentType, "gProtectedField");
        verifyField(parentType, "gDefaultField");
        verifyField(parentType, "pPublicField");
        verifyField(parentType, "pProtectedField");
        verifyField(parentType, "pPrivateField");
        verifyField(parentType, "pDefaultField");
        verifyField(parentType, "hiddenField");
        verifyFieldCount(parentType, 8);

        // Me
        EventType meType = EventType.getEventType(MeEvent.class);
        verifyField(meType, "gPublicField");
        verifyField(meType, "gProtectedField");
        verifyField(meType, "gDefaultField");
        verifyField(meType, "pPublicField");
        verifyField(meType, "pProtectedField");
        verifyField(meType, "pDefaultField");
        verifyField(meType, "mPublicField");
        verifyField(meType, "mProtectedField");
        verifyField(meType, "mPrivateField");
        verifyField(meType, "mDefaultField");
        verifyField(meType, "hiddenField");
        verifyFieldCount(meType, 11);

        for (RecordedEvent re : Events.fromRecording(r)) {
            System.out.println(re);
        }

        RecordedEvent grandpa = findEvent(r, GrandpaEvent.class.getName());
        Asserts.assertEquals(grandpa.getValue("gPublicField"), 4);
        Asserts.assertEquals(grandpa.getValue("gProtectedField"), 3);
        Asserts.assertEquals(grandpa.getValue("gPrivateField"), 2);
        Asserts.assertEquals(grandpa.getValue("gDefaultField"), 1);
        Asserts.assertEquals(grandpa.getValue("hiddenField"), 4711);

        RecordedEvent parent = findEvent(r, ParentEvent.class.getName());
        Asserts.assertEquals(parent.getValue("gPublicField"), 4);
        Asserts.assertEquals(parent.getValue("gProtectedField"), 3);
        Asserts.assertEquals(parent.getValue("gDefaultField"), 1);
        Asserts.assertEquals(parent.getValue("pPublicField"), 40);
        Asserts.assertEquals(parent.getValue("pProtectedField"), 30);
        Asserts.assertEquals(parent.getValue("pPrivateField"), 20);
        Asserts.assertEquals(parent.getValue("pDefaultField"), 10);
        Asserts.assertEquals(parent.getValue("hiddenField"), true);

        RecordedEvent me = findEvent(r, MeEvent.class.getName());
        Asserts.assertEquals(me.getValue("gPublicField"), 4);
        Asserts.assertEquals(me.getValue("gProtectedField"), 3);
        Asserts.assertEquals(me.getValue("gDefaultField"), 1);
        Asserts.assertEquals(me.getValue("pPublicField"), 40);
        Asserts.assertEquals(me.getValue("pProtectedField"), 30);
        Asserts.assertEquals(me.getValue("pDefaultField"), 10);
        Asserts.assertEquals(me.getValue("mPublicField"), 400);
        Asserts.assertEquals(me.getValue("mProtectedField"), 300);
        Asserts.assertEquals(me.getValue("mPrivateField"), 200);
        Asserts.assertEquals(me.getValue("mDefaultField"), 100);
        Asserts.assertEquals(me.getValue("hiddenField"), "Hidden");
    }

    private static RecordedEvent findEvent(Recording r, String name) throws Exception {
        for (RecordedEvent re : Events.fromRecording(r)) {
            if (re.getEventType().getName().equals(name)) {
                return re;
            }
        }
        throw new Exception("Event type hierarchy exist, but missing event " + name + " from recording");
    }

    private static void verifyFieldCount(EventType t, int count) throws Exception {
        if (t.getFields().size() != count + DEFAULT_FIELD_COUNT) {
            throw new Exception("Incorrect number of fields " + count);
        }
    }

    private static void verifyField(EventType t, String name) throws Exception {
        ValueDescriptor d = t.getField(name);
        if (d == null) {
            throw new Exception("Missing field " + name + " in event " + t.getName());
        }
    }
}
