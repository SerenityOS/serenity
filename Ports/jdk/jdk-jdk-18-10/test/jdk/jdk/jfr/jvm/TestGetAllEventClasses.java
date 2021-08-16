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

package jdk.jfr.jvm;

import jdk.jfr.Event;
import jdk.jfr.internal.JVM;

import java.util.List;

/**
 * @test TestGetAllEventClasses
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @modules jdk.jfr/jdk.jfr.internal
 *
 * @build jdk.jfr.jvm.HelloWorldEvent1
 * @build jdk.jfr.jvm.HelloWorldEvent2
 * @run main/othervm jdk.jfr.jvm.TestGetAllEventClasses
 */
public class TestGetAllEventClasses {

    public static void main(String... args) throws ClassNotFoundException {
        JVM jvm = JVM.getJVM();
        // before creating  native
        assertEmptyEventList(jvm);
        jvm.createNativeJFR();
        // after creating native
        assertEmptyEventList(jvm);

        // Test event class load is triggered and only once
        Class<? extends Event> clazz = initialize("jdk.jfr.jvm.HelloWorldEvent1");
        // check that the event class is registered
        assertEventsIncluded(jvm, clazz);
        // second active use of the same event class should not add another class
        // to the list - it would already be loaded
        clazz = initialize(clazz);
        assertEventsIncluded(jvm, clazz);

        // second event class
        Class<? extends Event> clazz2 = initialize("jdk.jfr.jvm.HelloWorldEvent2");
        // the list of event classes should now have two classes registered
        assertEventsIncluded(jvm, clazz, clazz2);

        // verify that an abstract event class is not included
        Class<? extends Event> abstractClass = initialize(MyAbstractEvent.class); // to run <clinit>
        assertEventsExcluded(jvm, abstractClass);

        // verify that a class that is yet to run its <clinit> is not included in the list of event classes
        assertEventsExcluded(jvm, MyUnInitializedEvent.class);

        // ensure old classes are not lost
        assertEventsIncluded(jvm, clazz, clazz2);

        jvm.destroyNativeJFR();
    }

    private static Class<? extends Event> initialize(String name) throws ClassNotFoundException {
        // Class.forName() will force the class to run its <clinit> method
        return Class.forName(name).asSubclass(Event.class);
    }

    private static Class<? extends Event> initialize(Class<? extends Event> event) throws ClassNotFoundException {
        return initialize(event.getName());
    }

    private static void assertEmptyEventList(JVM jvm) {
        if (!jvm.getAllEventClasses().isEmpty()) {
            throw new AssertionError("should not have any event classes registered!");
        }
    }

    @SafeVarargs
    private static void assertEventsExcluded(JVM jvm, Class<? extends Event>... targetEvents) {
        assertEvents(jvm, false, targetEvents);
    }

    @SafeVarargs
    private static void assertEventsIncluded(JVM jvm, Class<? extends Event>... targetEvents) {
        assertEvents(jvm, true, targetEvents);
    }

    @SafeVarargs
    @SuppressWarnings("rawtypes")
    private static void assertEvents(JVM jvm, boolean inclusion, Class<? extends Event>... targetEvents) {
        final List list = jvm.getAllEventClasses();
        for (Class<? extends Event> ev : targetEvents) {
           if (list.contains(ev)) {
               if (inclusion) {
                   continue;
               }
               throw new AssertionError(ev.getName() + " in list but should not be!");
           }
           if (!inclusion) {
               continue;
           }
           throw new AssertionError(ev.getName() + " in not in list but should be!");
       }
    }

    private static abstract class MyAbstractEvent extends Event {}
    private static class MyUnInitializedEvent extends Event {}
}
