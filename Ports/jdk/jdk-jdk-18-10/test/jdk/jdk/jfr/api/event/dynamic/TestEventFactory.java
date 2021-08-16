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

package jdk.jfr.api.event.dynamic;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.jfr.AnnotationElement;
import jdk.jfr.Event;
import jdk.jfr.EventFactory;
import jdk.jfr.EventType;
import jdk.jfr.MetadataDefinition;
import jdk.jfr.Recording;
import jdk.jfr.ValueDescriptor;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventTypePrototype;
import jdk.test.lib.jfr.Events;


/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.event.dynamic.TestEventFactory
 */
public class TestEventFactory {

    @MetadataDefinition
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.TYPE })
    public @interface TestAnnotation {
        String value();
    }

    public final static Map<String, Object> EVENT_VALUES = new HashMap<>();
    public final static EventTypePrototype EVENT_TYPE_SHOULD_NOT_COMMIT;
    public final static EventTypePrototype EVENT_TYPE_SHOULD_COMMIT;

    // keep alive to prevent event metadata getting GC.
    public static EventFactory ef1;
    public static EventFactory ef2;

    static {
        EVENT_VALUES.put("intField", Integer.MAX_VALUE);
        EVENT_VALUES.put("longField", Long.MAX_VALUE);
        EVENT_VALUES.put("byteField", (byte) 5);
        EVENT_VALUES.put("charField", (char) 'H');
        EVENT_VALUES.put("shortField", (short) 56);
        EVENT_VALUES.put("booleanField", true);
        EVENT_VALUES.put("floatField", 4711.0f);
        EVENT_VALUES.put("doubleField", 3.141);
        EVENT_VALUES.put("classField", String.class);
        EVENT_VALUES.put("stringField", "Yeah!");
        EVENT_VALUES.put("threadField", Thread.currentThread());

        EVENT_TYPE_SHOULD_NOT_COMMIT = makeEventType("com.test.ShouldNotCommit");
        EVENT_TYPE_SHOULD_COMMIT = makeEventType("com.test.ShouldCommit");
    }

    public static void main(String[] args) throws Throwable {
        Recording r = new Recording();
        r.enable(EVENT_TYPE_SHOULD_COMMIT.getName()).withoutStackTrace();
        r.enable(EVENT_TYPE_SHOULD_NOT_COMMIT.getName()).withoutStackTrace();

        // Commit before start, should not be included
        ef1 = EventFactory.create(EVENT_TYPE_SHOULD_NOT_COMMIT.getAnnotations(), EVENT_TYPE_SHOULD_NOT_COMMIT.getFields());

        Event event1 = ef1.newEvent();

        setEventValues(event1, ef1, EVENT_TYPE_SHOULD_NOT_COMMIT);
        event1.commit();

        r.start();
        // Commit after start, should be included
        ef2 = EventFactory.create(EVENT_TYPE_SHOULD_COMMIT.getAnnotations(),  EVENT_TYPE_SHOULD_COMMIT.getFields());

        Event event2 = ef2.newEvent();
        setEventValues(event2, ef2, EVENT_TYPE_SHOULD_COMMIT);
        event2.commit();

        r.stop();

        RecordingFile es = Events.copyTo(r);
        EventType e1 = findEventType(es.readEventTypes(), EVENT_TYPE_SHOULD_NOT_COMMIT.getName());
        assertEquals(e1, ef1.getEventType());

        EventType e2 = findEventType(es.readEventTypes(), EVENT_TYPE_SHOULD_COMMIT.getName());
        assertEquals(e2, ef2.getEventType());

        verifyEvent(es);
    }

    private static EventType findEventType(List<EventType> es, String name) {
        for (EventType t : es) {
            if (t.getName().equals(name)) {
                return t;
            }
        }
        throw new AssertionError("Could not find expected event type " + name);
    }

    private static void assertEquals(EventType e1, EventType expected) {
        Asserts.assertEquals(e1.getName(), expected.getName());
        Asserts.assertEquals(e1.getDescription(), expected.getDescription());
        Asserts.assertEquals(e1.getLabel(), expected.getLabel());
        assertValueDescriptorEquals(e1.getFields(), expected.getFields());
        assertAnnotationEquals(e1.getAnnotationElements(), expected.getAnnotationElements());
    }

    private static void assertValueDescriptorEquals(List<ValueDescriptor> values, List<ValueDescriptor> expected) {
        if (values.isEmpty() && expected.isEmpty()) {
            return;
        }

        Map<String, ValueDescriptor> valueMap = new HashMap<>();
        for (ValueDescriptor v : values) {
            valueMap.put(v.getName(), v);
        }
        for (ValueDescriptor f : expected) {
            ValueDescriptor v = valueMap.remove(f.getName());
            if (v == null) {
                throw new AssertionError("Expected value descriptor " + f.getName() + " not found");
            }
            assertEquals(v, f);
        }
        if (!valueMap.isEmpty()) {
            throw new AssertionError("More fields than expected");
        }
    }

    private static void assertEquals(ValueDescriptor v1, ValueDescriptor expected) {
        Asserts.assertEquals(v1.getName(), expected.getName());
        Asserts.assertEquals(v1.getTypeName(), expected.getTypeName());
        assertAnnotationEquals(v1.getAnnotationElements(), expected.getAnnotationElements());
    }

    private static void assertAnnotationEquals(List<AnnotationElement> annotations, List<AnnotationElement> expected) {
        annotations = new ArrayList<>(annotations); // make mutable
        expected = new ArrayList<>(expected); // make mutable
        class AnnotationTypeComparator implements Comparator<AnnotationElement> {
            @Override
            public int compare(AnnotationElement a, AnnotationElement b) {
                return a.getTypeName().compareTo(b.getTypeName());
            }
        }

        if (annotations.isEmpty() && expected.isEmpty()) {
            return;
        }

        if (annotations.size() != expected.size()) {
            System.out.println("Was:");
            for(AnnotationElement ae: annotations) {
                System.out.println(ae.getTypeName());
            }
            System.out.println("Expected:");
            for(AnnotationElement ae: expected) {
                System.out.println(ae.getTypeName());
            }
            throw new AssertionError("Wrong number of annotations");
        }
        Collections.sort(expected, new AnnotationTypeComparator());
        Collections.sort(annotations, new AnnotationTypeComparator());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(annotations.get(i), expected.get(i));
        }
    }

    private static void assertEquals(AnnotationElement a1, AnnotationElement expected) {
        Asserts.assertEquals(a1.getTypeName(), expected.getTypeName());
        // Don't recurse into annotation
        assertValueDescriptorEquals(a1.getValueDescriptors(), expected.getValueDescriptors());
    }

    private static void verifyEvent(RecordingFile rf) throws IOException {
        if (!rf.hasMoreEvents()) {
            throw new AssertionError("Expected one dynamic event");
        }
        verifyValues(rf.readEvent());
        if (rf.hasMoreEvents()) {
            throw new AssertionError("Expected one dynamic event");
        }
    }

    private static void setEventValues(Event event, EventFactory f, EventTypePrototype eventTypeProto) {
        for (Map.Entry<String, Object> entry : EVENT_VALUES.entrySet()) {
            int index = eventTypeProto.getFieldIndex(entry.getKey());
            event.set(index, entry.getValue());
        }
    }

    private static void verifyValues(RecordedEvent event) {
        for (Map.Entry<String, Object> entry : EVENT_VALUES.entrySet()) {
            String fieldName = entry.getKey();
            Object value = event.getValue(fieldName);
            Object expected = EVENT_VALUES.get(fieldName);
            if (expected instanceof Class) {
                value = ((RecordedClass) value).getName();
                expected = ((Class<?>) expected).getName();
            }
            if (expected instanceof Thread) {
                value = ((RecordedThread) value).getJavaName();
                expected = ((Thread) expected).getName();
            }
            Asserts.assertEQ(value, expected);
        }
    }

    private static EventTypePrototype makeEventType(String eventName) {
        EventTypePrototype prototype = new EventTypePrototype(eventName);
        prototype.addAnnotation(new AnnotationElement(TestAnnotation.class, "type"));
        for (Map.Entry<String, Object> entry : EVENT_VALUES.entrySet()) {
            Class<?> type = makePrimitive(entry.getValue().getClass());
            String fieldName = entry.getKey();
            prototype.addField(new ValueDescriptor(type, fieldName));
        }
        // add an annotated field
        List<AnnotationElement> annos = new ArrayList<>();
        annos.add(new AnnotationElement(TestAnnotation.class, "field"));
        prototype.addField( new ValueDescriptor(int.class, "annotatedField", annos));

        return prototype;
    }

    private static Class<?> makePrimitive(Class<? extends Object> clazz) {
        if (clazz == Integer.class) {
            return int.class;
        }
        if (clazz == Long.class) {
            return long.class;
        }
        if (clazz == Double.class) {
            return double.class;
        }
        if (clazz == Float.class) {
            return float.class;
        }
        if (clazz == Short.class) {
            return short.class;
        }
        if (clazz == Character.class) {
            return char.class;
        }
        if (clazz == Byte.class) {
            return byte.class;
        }
        if (clazz == Boolean.class) {
            return boolean.class;
        }
        return clazz;
    }
}
