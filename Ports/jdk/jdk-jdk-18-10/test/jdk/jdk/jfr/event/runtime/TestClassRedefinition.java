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
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @summary Tests ClassRedefinition event by redefining classes in a Java agent
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @modules java.instrument
 *
 * @build jdk.jfr.event.runtime.RedefinableClass
 * @build jdk.jfr.event.runtime.Bytes
 * @build jdk.jfr.event.runtime.TestClassRedefinition
 *
 * @run driver jdk.test.lib.util.JavaAgentBuilder
 *      jdk.jfr.event.runtime.TestClassRedefinition TestClassRedefinition.jar
 *
 * @run main/othervm -javaagent:TestClassRedefinition.jar
 *      jdk.jfr.event.runtime.TestClassRedefinition
 */
public class TestClassRedefinition {
    private final static Path DUMP_PATH = Paths.get("dump.jfr");

    // Called when agent is loaded from command line
    public static void premain(String agentArgs, Instrumentation instrumentation) throws Exception {
        try (Recording r = new Recording()) {
            r.enable(EventNames.ClassRedefinition);
            r.start();
            byte[] worldBytes = Bytes.classBytes(RedefinableClass.class);
            byte[] earthBytes = Bytes.replaceAll(worldBytes, Bytes.WORLD, Bytes.EARTH);
            RedefinableClass.sayHello();
            ClassDefinition cd1 = new ClassDefinition(RedefinableClass.class, earthBytes);
            instrumentation.redefineClasses(cd1);
            RedefinableClass.sayHello();
            ClassDefinition cd2 = new ClassDefinition(RedefinableClass.class, worldBytes);
            instrumentation.redefineClasses(cd2);
            RedefinableClass.sayHello();
            r.stop();
            r.dump(DUMP_PATH);
        }
    }

    public static void main(String[] args) throws Throwable {
        List<RecordedEvent> events = RecordingFile.readAllEvents(DUMP_PATH);

        Asserts.assertEquals(events.size(), 2, "Expected exactly two ClassRedefinition event");
        RecordedEvent e1 = events.get(0);
        System.out.println(e1);
        RecordedEvent e2 = events.get(1);
        System.out.println(e2);

        Events.assertField(e1, "classModificationCount").equal(1);
        Events.assertField(e2, "classModificationCount").equal(2);

        Events.assertField(e1, "redefinitionId").atLeast(1L);
        Events.assertField(e2, "redefinitionId").notEqual(e1.getValue("redefinitionId"));

        RecordedClass clazz1 = e1.getClass("redefinedClass");
        Asserts.assertEquals(clazz1.getName(), RedefinableClass.class.getName());
        RecordedClass clazz2 = e1.getClass("redefinedClass");
        Asserts.assertEquals(clazz2.getName(), RedefinableClass.class.getName());
    }
}
