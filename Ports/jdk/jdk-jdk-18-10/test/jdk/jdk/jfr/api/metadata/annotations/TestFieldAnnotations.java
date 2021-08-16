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

package jdk.jfr.api.metadata.annotations;

import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.Frequency;
import jdk.jfr.MemoryAddress;
import jdk.jfr.DataAmount;
import jdk.jfr.Percentage;
import jdk.jfr.Timespan;
import jdk.jfr.Timestamp;
import jdk.jfr.TransitionFrom;
import jdk.jfr.TransitionTo;
import jdk.jfr.Unsigned;
import jdk.jfr.ValueDescriptor;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.metadata.annotations.TestFieldAnnotations
 */
public class TestFieldAnnotations {

    static class FieldAnnotationEvent extends Event {
        @DataAmount
        int memoryAmount;

        @Frequency
        float frequency;

        @MemoryAddress
        long memoryAddress;

        @Percentage
        float percentage;

        @TransitionFrom
        Thread fromThread;

        @TransitionTo
        Thread toThread;

        @Unsigned
        long unsigned;

        @Timespan(Timespan.MILLISECONDS)
        long timespan;

        @Timestamp(Timestamp.MILLISECONDS_SINCE_EPOCH)
        long timestamp;
    }

    public static void main(String[] args) throws Exception {
        EventType t = EventType.getEventType(FieldAnnotationEvent.class);

        ValueDescriptor field = t.getField("memoryAmount");
        Events.hasAnnotation(field, DataAmount.class);

        field = t.getField("frequency");
        Events.hasAnnotation(field, Frequency.class);

        field = t.getField("memoryAddress");
        Events.hasAnnotation(field, MemoryAddress.class);

        field = t.getField("percentage");
        Events.hasAnnotation(field, Percentage.class);

        field = t.getField("fromThread");
        Events.hasAnnotation(field, TransitionFrom.class);

        field = t.getField("toThread");
        Events.hasAnnotation(field, TransitionTo.class);

        field = t.getField("unsigned");
        Events.hasAnnotation(field, Unsigned.class);

        field = t.getField("timespan");
        Events.assertAnnotation(field, Timespan.class, Timespan.MILLISECONDS);

        field = t.getField("timestamp");
        Events.assertAnnotation(field, Timestamp.class, Timestamp.MILLISECONDS_SINCE_EPOCH);
    }
}
