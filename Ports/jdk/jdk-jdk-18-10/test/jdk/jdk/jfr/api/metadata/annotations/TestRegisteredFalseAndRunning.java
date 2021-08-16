/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
import jdk.jfr.Recording;
import jdk.jfr.Registered;

/**
 * @test Tests that commit doesn't throw exception when an event has not been registered.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.metadata.annotations.TestRegisteredFalseAndRunning
 * @run main/othervm -XX:FlightRecorderOptions:retransform=false jdk.jfr.api.metadata.annotations.TestRegisteredFalseAndRunning
 */
public class TestRegisteredFalseAndRunning {
    @Registered(false)
    static class NoAutoEvent extends Event {
        String hello;
    }

    public static void main(String... args) {
        try (Recording r = new Recording()) {
            r.start();
            NoAutoEvent event = new NoAutoEvent();
            event.commit();
        }
    }
}
