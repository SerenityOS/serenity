/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.event.runtime;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @summary Tests RedefinitionClasses event by redefining a class in a Java
 *          agent
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @modules java.instrument
 *
 * @build jdk.jfr.event.runtime.RedefinableClass
 * @build jdk.jfr.event.runtime.Bytes
 * @build jdk.jfr.event.runtime.TestRedefineClasses
 *
 * @run driver jdk.test.lib.util.JavaAgentBuilder
 *      jdk.jfr.event.runtime.TestRedefineClasses
 *      TestRedefineClasses.jar
 *
 * @run main/othervm -javaagent:TestRedefineClasses.jar
 *      jdk.jfr.event.runtime.TestRedefineClasses
 */
public class TestRedefineClasses {
    private final static Path DUMP_PATH = Paths.get("dump.jfr");
    private final static String TEST_AGENT = "Test Agent";

    // Called when agent is loaded at startup
    public static void premain(String agentArgs, Instrumentation instrumentation) throws Exception {
        Thread.currentThread().setName(TEST_AGENT);
        try (Recording r = new Recording()) {
            r.enable(EventNames.RedefineClasses);
            r.start();
            RedefinableClass.sayHello();
            byte[] bytes = Bytes.classBytes(RedefinableClass.class);
            bytes = Bytes.replaceAll(bytes, Bytes.WORLD, Bytes.EARTH);
            ClassDefinition c1 = new ClassDefinition(RedefinableClass.class, bytes);
            instrumentation.redefineClasses(c1);
            RedefinableClass.sayHello();
            r.stop();
            r.dump(DUMP_PATH);
        }
    }

    public static void main(String[] args) throws Throwable {
        List<RecordedEvent> events = RecordingFile.readAllEvents(DUMP_PATH);
        Asserts.assertEquals(events.size(), 1, "Expected one RedefineClasses event");
        RecordedEvent event = events.get(0);

        System.out.println(event);

        Events.assertField(event, "eventThread.javaName").equal(TEST_AGENT);
        Events.assertField(event, "classCount").equal(1);
        Events.assertField(event, "redefinitionId").atLeast(1L);
        Events.assertField(event, "duration").atLeast(1L);
        Events.assertFrame(event, TestRedefineClasses.class, "premain");
    }
}
