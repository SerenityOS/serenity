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

import java.util.ArrayList;
import java.util.List;

import jdk.jfr.AnnotationElement;
import jdk.jfr.Event;
import jdk.jfr.EventFactory;
import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.ValueDescriptor;
import jdk.test.lib.Asserts;


/**
 * @test
 * @summary EventFactory simple test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.event.TestEventFactory
 */
public class TestEventFactory {

    public static void main(String[] args) throws Exception {
        List<ValueDescriptor> vds = new ArrayList<>();
        vds.add(new ValueDescriptor(String.class, "Message"));
        vds.add(new ValueDescriptor(String.class, "message"));

        List<AnnotationElement> annos = new ArrayList<>();
        annos.add(new AnnotationElement(Label.class, "Hello World"));

        EventFactory f = EventFactory.create(annos, vds);
        EventType type = f.getEventType();
        Asserts.assertNotNull(type);

        Event e = f.newEvent();
        e.set(0, "test Message");
        e.set(1, "test message");

        try {
            e.set(100, "should fail");
            Asserts.fail("The expected exception IndexOutOfBoundsException have not been thrown");
        } catch(IndexOutOfBoundsException expected) {
            // OK, as expected
        }

        try {
            e.set(-200, "should fail again");
            Asserts.fail("The expected exception IndexOutOfBoundsException have not been thrown");
        } catch(IndexOutOfBoundsException expected) {
            // OK, as expected
        }
    }
}
