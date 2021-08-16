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

import java.io.IOException;
import java.util.List;

import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.jfr.Registered;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @summary Test enable/disable event and verify recording has expected events.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.event.TestClinitRegistration
 */

public class TestClinitRegistration {

    public static void main(String[] args) throws Exception {
        // Test basic registration with or without auto registration
        assertClinitRegister(AutoRegisteredEvent.class, true, false);
        assertClinitRegister(NotAutoRegisterededEvent.class, false, false);
        assertClinitRegister(AutoRegisteredUserClinit.class, true, true);
        assertClinitRegister(NotAutoRegisteredUserClinit.class, false, true);

        // Test complex <clinit>
        assertClinitRegister(ComplexClInit.class, true, true);

        // Test hierarchy
        assertClinitRegister(DerivedClinit.class, true, true);
        if (!isClinitExecuted(Base.class)) {
            Asserts.fail("Expected <clinit> of base class to be executed");
        }

        // Test committed event in <clinit>
        Recording r = new Recording();
        r.start();
        r.enable(EventInClinit.class);
        triggerClinit(EventInClinit.class);
        r.stop();
        hasEvent(r, EventInClinit.class.getName());
    }

    private static void assertClinitRegister(Class<? extends Event> eventClass, boolean shouldExist, boolean setsProperty) throws ClassNotFoundException {
        String className = eventClass.getName();
        triggerClinit(eventClass);
        boolean hasEventType = hasEventType(className);
        boolean hasProperty = Boolean.getBoolean(className);
        if (hasEventType && !shouldExist) {
            Asserts.fail("Event class " + className + " should not be registered");
        }
        if (!hasEventType && shouldExist) {
            Asserts.fail("Event class " + className + " is not registered");
        }
        if (setsProperty && !hasProperty) {
            Asserts.fail("Expected clinit to be executed");
        }
        if (!setsProperty && hasProperty) {
            Asserts.fail("Property in clinit should not been set. Test bug?");
        }
    }

    private static boolean hasEventType(String name) {
        for (EventType type : FlightRecorder.getFlightRecorder().getEventTypes()) {
            if (type.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static void triggerClinit(Class<?> clazz) throws ClassNotFoundException {
        Class.forName(clazz.getName(), true, clazz.getClassLoader());
    }

    private static void setClinitExecuted(Class<? extends Event> eventClass) {
        System.setProperty(eventClass.getName(), "true");
    }

    private static boolean isClinitExecuted(Class<? extends Event> eventClass) {
        return "true".equals(System.getProperty(eventClass.getName(), "true"));
    }

    static class AutoRegisteredEvent extends Event {
    }

    @Registered(false)
    static class NotAutoRegisterededEvent extends Event {
    }

    static class AutoRegisteredUserClinit extends Event {
        static {
            setClinitExecuted(AutoRegisteredUserClinit.class);
        }
    }

    @Registered(false)
    static class NotAutoRegisteredUserClinit extends Event {
        static {
            setClinitExecuted(NotAutoRegisteredUserClinit.class);
        }
    }

    static class Base extends Event {
        static {
            setClinitExecuted(Base.class);
        }
    }

    static class DerivedClinit extends Base {
        static {
            setClinitExecuted(DerivedClinit.class);
        }

        @Deprecated
        void myVoidMethod() {
        }
    }

    static class ComplexClInit extends Event {
        static {
            setClinitExecuted(ComplexClInit.class);
        }
        public static final long l = Long.parseLong("7");
        public static final int i = Integer.parseInt("7");
        public static final short s = Short.parseShort("7");
        public static final double d = Double.parseDouble("7");
        public static final float f = Float.parseFloat("7");
        public static final boolean b = Boolean.parseBoolean("true");
        public static final char c = (char) Integer.parseInt("48");
        public static final String text = "ioio".substring(2);
        public static final int[] primitivArray = new int[] { 7, 4 };
        public static final Class<?> Object = ComplexClInit.class;

        static {
            String text = "";
            long l = 56;
            long i = 56;
            if (i != l) {
                throw new RuntimeException("unexpected result from comparison");
            }
            if (!isClinitExecuted(ComplexClInit.class)) {
                throw new RuntimeException("Expected clinit flag to be set" + text);
            }
        }

        static {
            try {
                throw new IllegalStateException("Exception");
            } catch (IllegalStateException ise) {
                // as expected
            }
        }
    }

    static class EventInClinit extends Event {
        static {
            EventInClinit eventInClinit = new EventInClinit();
            eventInClinit.commit();
        }
    }

    public static void hasEvent(Recording r, String name) throws IOException {
        List<RecordedEvent> events = Events.fromRecording(r);
        Events.hasEvents(events);

        for (RecordedEvent event : events) {
            if (event.getEventType().getName().equals(name)) {
                return;
            }
        }
        Asserts.fail("Missing event " + name + " in recording " + events.toString());
    }
}
