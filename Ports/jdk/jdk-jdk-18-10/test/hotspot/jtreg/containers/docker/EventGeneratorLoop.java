/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
import jdk.jfr.Event;
import jdk.jfr.Description;
import jdk.jfr.Label;


// This class generates simple event in a loop for a specified time.
public class EventGeneratorLoop {
    public static final String MAIN_METHOD_STARTED = "MAIN_METHOD_STARTED";

    @Label("SimpleEvent")
    @Description("Simple custom event")
    static class SimpleEvent extends Event {
        @Label("Message")
        String msg;

        @Label("Count")
        int count;
    }

    public static void main(String[] args) throws Exception {
        if ((args.length < 1) || (args[0] == null)) {
            throw new IllegalArgumentException("Expecting one argument: time to run (seconds)");
        }
        int howLong = Integer.parseInt(args[0]);

        System.out.println(MAIN_METHOD_STARTED + ", argument is " + howLong);

        for (int i=0; i < howLong; i++) {
            SimpleEvent ev = new SimpleEvent();
            ev.msg = "Hello";
            ev.count = i;
            ev.commit();

            try { Thread.sleep(1000); } catch (InterruptedException e) {}
            System.out.print(".");
        }

        System.out.println("EventGeneratorLoop is coming to an end");
    }

}
