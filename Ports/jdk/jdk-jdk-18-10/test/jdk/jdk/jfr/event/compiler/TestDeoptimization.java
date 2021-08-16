/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.compiler;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

import sun.hotspot.WhiteBox;

//    THIS TEST IS LINE NUMBER SENSITIVE

// Careful if moving this class or method somewhere since verifyDeoptimizationEventFields asserts the linenumber
class Dummy {
    public static void dummyMethod(boolean b) {
        if (b) {
            System.out.println("Deoptimized");
        }
    }
}

/**
 * @test
 * @key jfr
 * @summary sanity test for Deoptimization event, depends on Compilation event
 * @requires vm.hasJFR
 * @requires vm.compMode != "Xint"
 * @requires vm.flavor == "server" & (vm.opt.TieredStopAtLevel == 4 | vm.opt.TieredStopAtLevel == null)
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:-BackgroundCompilation -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI jdk.jfr.event.compiler.TestDeoptimization
 */
public class TestDeoptimization {
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    private final static String TYPE_NAME = Dummy.class.getName().replace(".", "/");
    private final static String METHOD_NAME = "dummyMethod";
    private static final String METHOD_DESCRIPTOR = "(Z)V";
    private static final String[] COMPILER =  { "c2",  "jvmci" };

    public static void main(String[] args) throws Throwable {
        new TestDeoptimization().doTest();
    }

    public void doTest() throws Throwable {
        Method dummyMethodDesc = Dummy.class.getDeclaredMethod("dummyMethod", boolean.class);

        System.out.println("Deoptimization Test");

        Recording recording = new Recording();
        recording.enable(EventNames.Deoptimization);
        recording.enable(EventNames.Compilation);
        recording.start();

        long start = System.currentTimeMillis();

        // load
        Dummy.dummyMethod(false);

        // compiling at level 3, for profiling support
        if (!WHITE_BOX.enqueueMethodForCompilation(dummyMethodDesc, 3)) {
            return;
        }

        // profile dummyMethod
        for (int i = 0; i < 20000; i++) {
            Dummy.dummyMethod(false);
        }

        // compiling at level 4
        if (!WHITE_BOX.enqueueMethodForCompilation(dummyMethodDesc, 4)) {
            return;
        }

        // provoke deoptimization by executing the uncommon trap in dummyMethod
        Dummy.dummyMethod(true);
        System.out.println("Time to load, compile and deoptimize dummyMethod: " + (System.currentTimeMillis() - start));
        recording.stop();

        List<RecordedEvent> events = Events.fromRecording(recording);
        Events.hasEvents(events);

        // get compile ids for all compilations of dummyMethod
        List<Integer> compileIds = events.stream()
                .filter(e -> e.getEventType().getName().equals(EventNames.Compilation))
                .filter(TestDeoptimization::isForDummyMethod)
                .map(e -> Events.assertField(e, "compileId").<Integer>getValue())
                .collect(Collectors.toList());
        Asserts.assertFalse(compileIds.isEmpty(),
                "couldn't find any " + EventNames.Compilation + " for " + METHOD_NAME);

        // get all deoptimization events associated with the compile ids
        List<RecordedEvent> deoptEventsForCompileIds = events.stream()
              .filter(e -> e.getEventType().getName().equals(EventNames.Deoptimization))
              .filter(e -> compileIds.contains(Events.assertField(e, "compileId").<Integer>getValue()))
              .collect(Collectors.toList());
        Asserts.assertFalse(deoptEventsForCompileIds.isEmpty(),
                "couldn't find any " + EventNames.Deoptimization + " for ids : " + compileIds);

        // verify deoptimization event fields
        deoptEventsForCompileIds.forEach(this::verifyDeoptimizationEventFields);
    }

    static boolean isForDummyMethod(RecordedEvent e) {
        return TYPE_NAME.equals(Events.assertField(e, "method.type.name").getValue())
                && METHOD_NAME.equals(Events.assertField(e, "method.name").getValue())
                && METHOD_DESCRIPTOR.equals(Events.assertField(e, "method.descriptor").getValue());
    }

    private void verifyDeoptimizationEventFields(RecordedEvent event) {
        Events.assertEventThread(event);
        Events.assertField(event, "compileId").atLeast(0);
        Events.assertField(event, "compiler").containsAny(COMPILER);
        Events.assertField(event, "lineNumber").equal(43);
        Events.assertField(event, "bci").atMost(1);
        // Both graal and c2 traps at ifeq. c2 deopt reinterpret from unstable ifeq, while Graal deopt reinterpret from next instruction after last state change.
        Events.assertField(event, "instruction").containsAny("ifeq", "iload_0");
        Events.assertField(event, "action").notEmpty().equal("reinterpret");
        Events.assertField(event, "reason").notEmpty().containsAny("unstable_if", "null_assert_or_unreached0");
    }
}

