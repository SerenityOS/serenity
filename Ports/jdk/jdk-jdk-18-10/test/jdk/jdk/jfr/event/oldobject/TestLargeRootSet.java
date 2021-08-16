/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedObject;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.internal.test.WhiteBox;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @modules jdk.jfr/jdk.jfr.internal.test
 * @run main/othervm -XX:TLABSize=2k jdk.jfr.event.oldobject.TestLargeRootSet
 */
public class TestLargeRootSet {
    static class Node {
        Node left;
        Node right;
        Object value;
    }

    static class Leak {
        // Leaking object has to be of some size,
        // otherwise Node object wins most of the
        // slots in the object queue.
        // In a normal application, objects would
        // be of various size and allocated over a
        // longer period of time. This would create
        // randomness not present in the test.
        public long value1;
        public Object value2;
        float value3;
        int value4;
        double value5;
    }

    public static void main(String[] args) throws Exception {
        WhiteBox.setWriteAllObjectSamples(true);
        WhiteBox.setSkipBFS(true);
        HashMap<Object, Node> leaks = new HashMap<>();
        try (Recording r = new Recording()) {
            r.enable(EventNames.OldObjectSample).withStackTrace().with("cutoff", "infinity");
            r.start();
            for (int i = 0; i < 1_000_000; i++) {
                Node node = new Node();
                node.left = new Node();
                node.right = new Node();
                node.right.value = new Leak();
                leaks.put(i, node);
            }
            r.stop();
            List<RecordedEvent> events = Events.fromRecording(r);
            Events.hasEvents(events);
            for (RecordedEvent e : events) {
                RecordedClass type = e.getValue("object.type");
                if (type.getName().equals(Leak.class.getName())) {
                    return;
                }
            }
        }
    }
}
