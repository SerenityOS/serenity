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

import org.openjdk.bench.java.io.BlackholedOutputStream;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class SerializationWriteReplace {

    private BlackholedOutputStream bos;
    private ObjectOutputStream os;

    @Setup
    public void setupStreams(Blackhole bh) throws IOException {
        bos = new BlackholedOutputStream(bh);
        os = new ObjectOutputStream(bos);
    }

    @TearDown
    public void downStreams() throws IOException {
        os.close();
        bos.close();
        os = null;
        bos = null;
    }

    @Benchmark
    public void writeReplace() throws IOException, ClassNotFoundException {
        os.writeObject(new Class2());
    }

    public abstract static class Base implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    public static class Class1 extends Base {
        private static final long serialVersionUID = 2L;
    }

    public static class Class2 extends Class1 {
        private static final long serialVersionUID = 3L;
        Object writeReplace() throws ObjectStreamException {
            return new Class3();
        }
    }

    public static class Class3 extends Base {
        private static final long serialVersionUID = 4L;
        private String tuto = "tuto";
        private byte b = (byte) 0xff;
    }

}
