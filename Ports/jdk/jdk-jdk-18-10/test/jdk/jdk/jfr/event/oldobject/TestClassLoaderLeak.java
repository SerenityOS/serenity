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

package jdk.jfr.event.oldobject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;
import jdk.jfr.internal.test.WhiteBox;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @requires vm.gc != "Serial"
 * @library /test/lib /test/jdk
 * @modules jdk.jfr/jdk.jfr.internal.test
 * @run main/othervm -XX:TLABSize=2k jdk.jfr.event.oldobject.TestClassLoaderLeak
 */
public class TestClassLoaderLeak {

    public static List<Object> classObjects = new ArrayList<>(OldObjects.MIN_SIZE);

    public static void main(String[] args) throws Exception {
        WhiteBox.setWriteAllObjectSamples(true);

        try (Recording r = new Recording()) {
            r.enable(EventNames.OldObjectSample).withStackTrace().with("cutoff", "infinity");
            r.start();
            TestClassLoader testClassLoader = new TestClassLoader();
            for (Class<?> clazz : testClassLoader.loadClasses(OldObjects.MIN_SIZE / 20)) {
                // Allocate array to trigger sampling code path for interpreter / c1
                for (int i = 0; i < 20; i++) {
                    Object classArray = Array.newInstance(clazz, 20);
                    Array.set(classArray, i, clazz.newInstance());
                    classObjects.add(classArray);
                }
            }
            r.stop();
            List<RecordedEvent> events = Events.fromRecording(r);
            Events.hasEvents(events);
            for (RecordedEvent e : events) {
                RecordedObject object = e.getValue("object");
                RecordedClass rc = object.getValue("type");
                if (rc.getName().contains("TestClass")) {
                    return;
                }
            }
            Asserts.fail("Could not find class leak");
        }
    }
}
