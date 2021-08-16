/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

import static jdk.test.lib.Asserts.assertFalse;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Utils;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;
import sun.hotspot.WhiteBox;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @requires vm.compMode!="Xint"
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:.
 *     -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *     jdk.jfr.event.compiler.TestCompilerCompile
 */
public class TestCompilerCompile {
    private final static String EVENT_NAME = EventNames.Compilation;
    private final static String METHOD_NAME = "dummyMethod";
    private boolean foundKnownMethod = false;
    private boolean foundOsrMethod = false;

    public static void main(String[] args) throws Throwable {
        TestCompilerCompile test = new TestCompilerCompile();
        test.doTest();
    }

    static void dummyMethod() {
        System.out.println("hello!");
    }

    public void doTest() throws Throwable {
        Recording recording = new Recording();
        recording.enable(EVENT_NAME);

        recording.start();
        long start = System.currentTimeMillis();
        // provoke OSR compilation
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
        }
        // compile dummyMethod()
        Method mtd = TestCompilerCompile.class.getDeclaredMethod(METHOD_NAME, new Class[0]);
        WhiteBox WB = WhiteBox.getWhiteBox();
        String directive = "[{ match: \"" + TestCompilerCompile.class.getName().replace('.', '/')
                + "." + METHOD_NAME + "\", " + "BackgroundCompilation: false }]";
        WB.addCompilerDirective(directive);
        if (!WB.enqueueMethodForCompilation(mtd, 4 /* CompLevel_full_optimization */)) {
            WB.enqueueMethodForCompilation(mtd, 1 /* CompLevel_simple */);
        }
        Utils.waitForCondition(() -> WB.isMethodCompiled(mtd));
        dummyMethod();

        System.out.println("time:" + (System.currentTimeMillis() - start));
        recording.stop();

        Set<Integer> compileIds = new HashSet<Integer>();
        List<RecordedEvent> events = Events.fromRecording(recording);
        Events.hasEvents(events);
        for (RecordedEvent event : events) {
            System.out.println("Event:" + event);
            verifyEvent(event);
            Integer compileId = Events.assertField(event, "compileId").getValue();
            assertFalse(compileIds.contains(compileId), "compile id not unique: " + compileId);
            compileIds.add(compileId);
        }

        // Verify that we actually encountered our expected method
        if (!foundKnownMethod) {
            throw new Exception("Couldn't find method jdk/jfr/event/compiler/TestCompilerCompile.dummyMethod()V among compilation events");
        }

        // Verify that doTest() function has been replaced on stack.
        if (!foundOsrMethod) {
            throw new Exception("No On Stack Replacement of function doTest()");
        }
    }

    private void verifyEvent(RecordedEvent event) throws Throwable {
        Events.assertJavaMethod(event);
        Events.assertEventThread(event);

        String methodName = Events.assertField(event, "method.name").notEmpty().getValue();
        String methodDescriptor = Events.assertField(event, "method.descriptor").notEmpty().getValue();
        String methodType = Events.assertField(event, "method.type.name").notEmpty().getValue();

        // Compare with a known candidate
        if ("jdk/jfr/event/compiler/TestCompilerCompile".equals(methodType) && "dummyMethod".equals(methodName) && "()V".equals(methodDescriptor)) {
            foundKnownMethod = true;
        }

        // The doTest() function is live almost the entire time the test runs.
        // We should get at least 1 "on stack replacement" for that method.
        if (TestCompilerCompile.class.getName().replace('.', '/').equals(methodType) && "doTest".equals(methodName)) {
            boolean isOsr = Events.assertField(event, "isOsr").getValue();
            if (isOsr) {
                foundOsrMethod = true;
            }
        }

        Events.assertField(event, "compileId").atLeast(0);
        Events.assertField(event, "compileLevel").atLeast((short) 0).atMost((short) 4);
        Events.assertField(event, "inlinedBytes").atLeast(0L);
        Events.assertField(event, "codeSize").atLeast(0L);
        Events.assertField(event, "isOsr");
    }
}
