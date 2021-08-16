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

import java.util.List;

import jdk.jfr.Event;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.jfr.Registered;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @summary Tests that a cloned event can be successfully committed.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.event.TestClonedEvent
 */

public class TestClonedEvent  {

    public static void main(String[] args) throws Throwable {
        Recording r = new Recording();
        r.enable(MyEvent.class);

        r.start();

        MyEvent event = new MyEvent();

        MyEvent event2 = (MyEvent)event.clone();

        FlightRecorder.register(MyEvent.class);
        event.commit();
        event2.commit();

        r.stop();

        List<RecordedEvent> events = Events.fromRecording(r);
        Asserts.assertEquals(2, events.size());

        r.close();

        FlightRecorder.unregister(MyEvent.class);

        Recording r2 = new Recording();
        r2.enable(MyEvent.class);

        r2.start();
        event.commit();
        event2.commit();

        r2.stop();

        events = Events.fromRecording(r2);
        Asserts.assertEquals(0, events.size());

        r2.close();
    }

    @Registered(false)
    private static class MyEvent extends Event implements Cloneable {

        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

    }

}
