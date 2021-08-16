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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.jfr.AnnotationElement;
import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.EventFactory;
import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.MetadataDefinition;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import jdk.jfr.Relational;
import jdk.jfr.ValueDescriptor;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.event.dynamic.TestDynamicAnnotations
 */
public class TestDynamicAnnotations {

    @Label("Execution Context Id")
    @Description("A unique identifier to correlate events or requests associated with the same task across several components")
    @Relational
    @MetadataDefinition
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface ECID {
    }

    @MetadataDefinition
    @Target({ ElementType.FIELD, ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    private @interface Array {
        String[] stringArray();

        int[] intArray();

        long[] longArray();

        float[] floatArray();

        double[] doubleArray();

        boolean[] booleanArray();

        short[] shortArray();

        byte[] byteArray();

        char[] charArray();
    }

    public static void main(String[] args) throws Throwable {
        testEventFactoryExample();
        testECID();
        testArray();
    }

    // Copy of sample code in Javadoc for jdk.jfr.EVentFactory
    public static void testEventFactoryExample() throws IOException {
         List<ValueDescriptor> fields = new ArrayList<>();
         List<AnnotationElement> messageAnnotations = Collections.singletonList(new AnnotationElement(Label.class, "Message"));
         fields.add(new ValueDescriptor(String.class, "message", messageAnnotations));
         List<AnnotationElement> numberAnnotations = Collections.singletonList(new AnnotationElement(Label.class, "Number"));
         fields.add(new ValueDescriptor(int.class, "number", numberAnnotations));

         String[] category = { "Example", "Getting Started" };
         List<AnnotationElement> eventAnnotations = new ArrayList<>();
         eventAnnotations.add(new AnnotationElement(Name.class, "com.example.HelloWorld"));
         eventAnnotations.add(new AnnotationElement(Label.class, "Hello World"));
         eventAnnotations.add(new AnnotationElement(Description.class, "Helps programmer getting started"));
         eventAnnotations.add(new AnnotationElement(Category.class, category));

         EventFactory f = EventFactory.create(eventAnnotations, fields);

         Event event = f.newEvent();
         event.set(0, "hello, world!");
         event.set(1, 4711);
         event.commit();
    }

    public static void testECID() throws Exception {
        List<ValueDescriptor> fields = new ArrayList<>();

        List<AnnotationElement> fieldAnnotations = new ArrayList<>();
        fieldAnnotations.add(new AnnotationElement(ECID.class));
        ValueDescriptor ecidField = new ValueDescriptor(String.class, "ecid", fieldAnnotations);
        fields.add(ecidField);

        EventFactory f = EventFactory.create(fieldAnnotations, fields);

        String ecidValue = "131739871298371279812";
        try (Recording r = new Recording()) {
            r.start();
            Event event = f.newEvent();
            event.set(0, ecidValue);
            event.commit();
            r.stop();
            List<RecordedEvent> events = Events.fromRecording(r);
            Events.hasEvents(events);
            Events.assertField(events.get(0), "ecid").equal(ecidValue);
        }
        EventType type = f.getEventType();
        ECID e = type.getAnnotation(ECID.class);
        if (e == null) {
            throw new Exception("Missing ECID annotation");
        }
    }

    public static void testArray() throws Exception {
        List<AnnotationElement> annotations = new ArrayList<>();
        Map<String, Object> values = new HashMap<>();
        values.put("stringArray", new String[] {"zero", "one"});
        values.put("intArray", new int[] {0, 1});
        values.put("longArray", new long[] {0L, 1L});
        values.put("floatArray", new float[] {0.0f, 1.0f});
        values.put("doubleArray", new double[] {0.0, 1.0});
        values.put("booleanArray", new boolean[] {false, true});
        values.put("shortArray", new short[] {(short)0, (short)1});
        values.put("byteArray", new byte[] {(byte)0, (byte)1});
        values.put("charArray", new char[] {'0','1'});

        annotations.add(new AnnotationElement(Array.class, values));
        EventFactory f = EventFactory.create(annotations, Collections.emptyList());
        Array a = f.getEventType().getAnnotation(Array.class);
        if (a == null) {
            throw new Exception("Missing array annotation");
        }
        verifyArrayAnnotation(a);
        System.out.println("Event metadata is correct");
        try (Recording r = new Recording()) {
            r.start();
            Event e = f.newEvent();
            e.commit();
            r.stop();
            List<RecordedEvent> events = Events.fromRecording(r);
            Events.hasEvents(events);
            RecordedEvent re = events.get(0);
            Array arrayAnnotation = re.getEventType().getAnnotation(Array.class);
            if (arrayAnnotation== null) {
                throw new Exception("Missing array annotation");
            }
            verifyArrayAnnotation(arrayAnnotation);
            System.out.println("Persisted event metadata is correct");
        }
    }

    private static void verifyArrayAnnotation(Array a) throws Exception {
        if (!a.stringArray()[0].equals("zero") || !a.stringArray()[1].equals("one")) {
            throw new Exception("string[] doesn't match");
        }
        if (a.intArray()[0] != 0 || a.intArray()[1] != 1) {
            throw new Exception("int[] doesn't match");
        }
        if (a.longArray()[0] != 0 || a.longArray()[1] != 1) {
            throw new Exception("long[] doesn't match");
        }
        if (a.floatArray()[0] != 0.0f || a.floatArray()[1] != 1.0f) {
            throw new Exception("float[] doesn't match");
        }
        if (a.doubleArray()[0] != 0.0 || a.doubleArray()[1] != 1.0) {
            throw new Exception("double[] doesn't match");
        }
        if (a.booleanArray()[0] != false || a.booleanArray()[1] != true) {
            throw new Exception("boolean[] doesn't match");
        }
        if (a.shortArray()[0] != (short)0 || a.shortArray()[1] != (short)1) {
            throw new Exception("short[] doesn't match");
        }
        if (a.byteArray()[0] != (byte)0 || a.byteArray()[1] != (byte)1) {
            throw new Exception("byte[] doesn't match");
        }
        if (a.charArray()[0] != '0' || a.charArray()[1] != '1') {
            throw new Exception("char[] doesn't match");
        }
    }
}
