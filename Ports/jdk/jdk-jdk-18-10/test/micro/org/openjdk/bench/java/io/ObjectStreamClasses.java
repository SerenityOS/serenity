/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.io;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

/**
 * A micro benchmark used to measure the performance impact from multi threaded access to ObjectStreamClass.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class ObjectStreamClasses {

    public Class<?>[] classes;

    @Setup
    public void setup() {
        LinkedList<Class> queue = new LinkedList<Class>();
        int i = 0;
        while (true) {
            // Loop until we get a ClassNotFoundException
            // Maybe rewrite this considering the fact that there are 29
            // inner classes available?
            try {
                Class clazz = Class.forName(ObjectStreamClasses.class.getName() + "$SerializableClass" + i++);
                queue.add(clazz);
            } catch (ClassNotFoundException e) {
                break;
            }
        }
        classes = new Class[queue.size()];

        // Make ObjectStreamClass load all classes into the static map
        i = 0;
        while (!queue.isEmpty()) {
            classes[i] = (Class) queue.remove();
            i++;
        }
    }

    /**
     * Tests the static lookup function. Depending on JRE version the internal behavior is different but the general
     * behavior is a synchronized call to some sort of static container.
     */
    @Benchmark
    public void testLookup(Blackhole bh) {
        for (Class<?> klass : classes) {
            bh.consume(ObjectStreamClass.lookup(klass));
        }
    }

    static class SerializableClass0 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass1 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass2 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass3 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass4 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass5 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass6 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass7 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass8 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass9 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass10 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass11 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass12 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass13 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass14 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass15 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass16 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass17 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass18 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass19 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass20 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass21 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass22 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass23 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass24 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass25 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass26 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass27 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass28 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    static class SerializableClass29 extends SerializableClass {
        private static final long serialVersionUID = 1L;
    }

    @SuppressWarnings("unused")
    private static class SerializableClass implements Serializable {

        private static final long serialVersionUID = 6107539118220989250L;
        public Object objectField00 = new Object();
        public Object objectField01 = new Object();
        public Object objectField02 = new Object();
        public Object objectField03 = new Object();
        public Object objectField04 = new Object();
        public Object objectField05 = new Object();
        public Object objectField06 = new Object();
        public Object objectField07 = new Object();
        public Object objectField08 = new Object();
        public Object objectField09 = new Object();
        public Object objectField10 = new Object();
        public Object objectField11 = new Object();
        public Object objectField12 = new Object();
        public Object objectField13 = new Object();
        public Object objectField14 = new Object();
        public Object objectField15 = new Object();
        public Object objectField16 = new Object();
        public Object objectField17 = new Object();
        public Object objectField18 = new Object();
        public Object objectField19 = new Object();
        public Object objectField20 = new Object();
        public Object objectField21 = new Object();
        public Object objectField22 = new Object();
        public Object objectField23 = new Object();
        public Object objectField24 = new Object();
        public Object objectField25 = new Object();
        public Object objectField26 = new Object();
        public Object objectField27 = new Object();
        public Object objectField28 = new Object();
        public Object objectField29 = new Object();

        SerializableClass() {
            super();
        }
    }
}
