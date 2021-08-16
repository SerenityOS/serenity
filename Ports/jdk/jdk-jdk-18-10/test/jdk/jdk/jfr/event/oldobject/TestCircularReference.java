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
package jdk.jfr.event.oldobject;

import java.util.ArrayList;
import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.internal.test.WhiteBox;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @modules jdk.jfr/jdk.jfr.internal.test
 * @run main/othervm -XX:TLABSize=2k jdk.jfr.event.oldobject.TestCircularReference
 */
public class TestCircularReference {

    static class TestCircularReferrer {
        // Allocate array to trigger sampling code path for interpreter / c1
        final byte[] leak = new byte[1_0000_000];
        TestCircularReferrer reference;

        public void setReference(TestCircularReferrer reference) {
            this.reference = reference;
        }
    }

    public final static List<Object> referenceHolder = new ArrayList<>(OldObjects.MIN_SIZE);

    public static void main(String[] args) throws Exception {
        WhiteBox.setWriteAllObjectSamples(true);

        try (Recording r = new Recording()) {
            r.enable(EventNames.OldObjectSample).with("cutoff", "infinity").withStackTrace();
            r.start();

            TestCircularReferrer a = new TestCircularReferrer();
            TestCircularReferrer b = new TestCircularReferrer();
            TestCircularReferrer c = new TestCircularReferrer();
            a.setReference(b);
            b.setReference(c);
            c.setReference(a);
            referenceHolder.add(a);

            TestCircularReferrer selfReflector = new TestCircularReferrer();
            selfReflector.setReference(selfReflector);
            referenceHolder.add(selfReflector);

            r.stop();

            List<RecordedEvent> events = Events.fromRecording(r);
            if (events.isEmpty()) {
                throw new AssertionError("Expected Old Object Sample events!");
            }
        }
    }
}
