/*
 * Copyright (c) 2021, Datadog, Inc. All rights reserved.
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

package jdk.jfr.event.oldobject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;
import jdk.jfr.internal.test.WhiteBox;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @modules jdk.jfr/jdk.jfr.internal.test
 * @run main/othervm  -XX:TLABSize=2k jdk.jfr.event.oldobject.TestObjectSize
 */
public class TestObjectSize {

    private interface Leak {
    }
    private static class Leak1 implements Leak {
        private long field1;
    }
    private static class Leak2 implements Leak {
        private long field1;
        private long field2;
    }
    private static class Leak3 implements Leak {
        private long field1;
        private long field2;
        private long field3;
    }

    public static List<Object> leak = new ArrayList<>(OldObjects.MIN_SIZE);

    public static void main(String[] args) throws Exception {
        WhiteBox.setWriteAllObjectSamples(true);

        final Random rand = new Random(1L);

        long sizeLeak1, sizeLeak2, sizeLeak3;

        do {
            sizeLeak1 = -1;
            sizeLeak2 = -1;
            sizeLeak3 = -1;

            try (Recording recording = new Recording()) {
                leak.clear();
                recording.enable(EventNames.OldObjectSample).withStackTrace().with("cutoff", "infinity");
                recording.start();

                for (int i = 0; i < 1000; i++) {
                    switch (rand.nextInt(3)) {
                    case 0: leak.add(new Leak1()); break;
                    case 1: leak.add(new Leak2()); break;
                    case 2: leak.add(new Leak3()); break;
                    }
                }

                recording.stop();

                List<RecordedEvent> events = Events.fromRecording(recording);
                Events.hasEvents(events);
                for (RecordedEvent e : events) {
                    RecordedObject object = e.getValue("object");
                    RecordedClass type = object.getValue("type");
                    long objectSize = e.getLong("objectSize");
                    System.err.println("type = " + type.getName() + ", objectSize = " + e.getLong("objectSize"));
                    if (objectSize <= 0) {
                        throw new Exception("Object size for " + type.getName() + " is lower or equal to 0");
                    }
                    if (type.getName().equals(Leak1.class.getName())) {
                        sizeLeak1 = objectSize;
                    }
                    if (type.getName().equals(Leak2.class.getName())) {
                        sizeLeak2 = objectSize;
                    }
                    if (type.getName().equals(Leak3.class.getName())) {
                        sizeLeak3 = objectSize;
                    }
                }
            }
        } while (sizeLeak1 == -1 || sizeLeak2 == -1 || sizeLeak3 == -1);

        if (sizeLeak3 <= sizeLeak2) {
            throw new Exception("Object size for " + Leak3.class.getName() + " is lower or equal to size for" + Leak2.class.getName());
        }
        if (sizeLeak2 <= sizeLeak1) {
            throw new Exception("Object size for " + Leak2.class.getName() + " is lower or equal to size for" + Leak1.class.getName());
        }
    }
}
