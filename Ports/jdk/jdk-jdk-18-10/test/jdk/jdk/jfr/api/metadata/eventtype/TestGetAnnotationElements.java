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
package jdk.jfr.api.metadata.eventtype;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.jfr.AnnotationElement;
import jdk.jfr.BooleanFlag;
import jdk.jfr.Category;
import jdk.jfr.ContentType;
import jdk.jfr.Description;
import jdk.jfr.Enabled;
import jdk.jfr.Event;
import jdk.jfr.EventFactory;
import jdk.jfr.EventType;
import jdk.jfr.Experimental;
import jdk.jfr.Frequency;
import jdk.jfr.Label;
import jdk.jfr.MemoryAddress;
import jdk.jfr.DataAmount;
import jdk.jfr.MetadataDefinition;
import jdk.jfr.Name;
import jdk.jfr.Percentage;
import jdk.jfr.Period;
import jdk.jfr.Registered;
import jdk.jfr.Relational;
import jdk.jfr.StackTrace;
import jdk.jfr.Threshold;
import jdk.jfr.Timespan;
import jdk.jfr.Timestamp;
import jdk.jfr.TransitionFrom;
import jdk.jfr.TransitionTo;
import jdk.jfr.Unsigned;
import jdk.jfr.ValueDescriptor;
import jdk.test.lib.Asserts;

/**
 * @test
 * @summary Test for AnnotationElement.getAnnotationElements()
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.metadata.eventtype.TestGetAnnotationElements
 */
public class TestGetAnnotationElements {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Throwable {
        Class<?>[] jfrAnnotations = {
            Category.class, Description.class, Enabled.class,
            Experimental.class, BooleanFlag.class, Frequency.class, Label.class,
            MemoryAddress.class, DataAmount.class, Name.class,
            Registered.class, Percentage.class,
            Period.class, Relational.class, StackTrace.class,
            Threshold.class, Timespan.class, Timestamp.class,
            TransitionFrom.class, TransitionTo.class, Unsigned.class
        };

        for (Class<?> clz : jfrAnnotations) {
            Class<? extends Annotation> annptationClass = (Class<? extends Annotation>) clz;
            System.out.println("AnnotationElement: " + annptationClass);
            Map<String, Object> values = createValueMapForAnnotation(annptationClass);
            List<Annotation> persistableAnnotation = createPersistableAnnotationList(annptationClass);
            AnnotationElement ae = new AnnotationElement(annptationClass, values);
            List<AnnotationElement> aes = ae.getAnnotationElements();
            Asserts.assertEquals(persistableAnnotation.size(), aes.size());
        }

        List<ValueDescriptor> fields = new ArrayList<>();
        List<AnnotationElement> eventAnnotations = new ArrayList<>();
        eventAnnotations.add(new AnnotationElement(Label.class, "MyEvent"));

        EventFactory f = EventFactory.create(eventAnnotations, fields);
        EventType type = f.getEventType();
        List<AnnotationElement> aes = type.getAnnotationElements().get(0).getAnnotationElements();
        Asserts.assertEquals(0, aes.size());

        EventType et = EventType.getEventType(MyEvent.class);
        ValueDescriptor field = et.getField("transactionRate");
        aes = field.getAnnotationElements().get(0).getAnnotationElements();
        Asserts.assertEquals(3, aes.size());
        assertContainsAnnotation(aes, Description.class);
        assertContainsAnnotation(aes, Label.class);
        assertContainsAnnotation(aes, ContentType.class);

    }

    private static List<Annotation> createPersistableAnnotationList( Class<? extends Annotation> annptationClass) {
       List<Annotation> as = new ArrayList<>();
        for (Annotation a : annptationClass.getAnnotations()) {
           MetadataDefinition m = a.annotationType().getAnnotation(MetadataDefinition.class);
           if (m != null) {
               as.add(a);
           }
       }
        return as;
    }

    private static void assertContainsAnnotation(List<AnnotationElement> aez, Class<?> clz) {
        for (AnnotationElement ae : aez) {
            if (ae.getTypeName().equals(clz.getName())) {
                return;
            }
        }
        Asserts.fail("Class " + clz + " not found in the annotation elements");
    }

    private static Map<String, Object> createValueMapForAnnotation(Class<?> clz) {
        Map<String, Object> map = new HashMap<>();
        for (Method method : clz.getDeclaredMethods()) {
            int modifiers = method.getModifiers();
            if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
                map.put(method.getName(), "value");
            }
        }
        return map;
    }

    private static class MyEvent extends Event {

        @Frequency
        long transactionRate;
    }

}
