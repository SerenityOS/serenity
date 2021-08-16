/*
 * Copyright (c) 2020, 2021, Red Hat Inc. All rights reserved.
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

import org.openjdk.jmh.annotations.*;

import java.io.*;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 1, warmups = 0)
@Measurement(iterations = 6, time = 1)
@Warmup(iterations=2, time = 2)
@State(Scope.Benchmark)
public class DataOutputStreamTest {

    public enum BasicType {CHAR, SHORT, INT, STRING}
    @Param({"CHAR", "SHORT", "INT", "STRING"}) BasicType basicType;

    @Param({"4096"}) int size;
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(size);
    File f;
    String outputString;
    FileOutputStream fileOutputStream;
    DataOutput bufferedFileStream, rawFileStream, byteArrayStream;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        f = File.createTempFile("DataOutputStreamTest","out");
        fileOutputStream = new FileOutputStream(f);
        byteArrayStream = new DataOutputStream(byteArrayOutputStream);
        rawFileStream = new DataOutputStream(fileOutputStream);
        bufferedFileStream = new DataOutputStream(new BufferedOutputStream(fileOutputStream));
        outputString = new String(new byte[size]);
    }

    public void writeChars(DataOutput dataOutput)
            throws Exception {
        for (int i = 0; i < size; i += 2) {
            dataOutput.writeChar(i);
        }
    }

    public void writeShorts(DataOutput dataOutput)
            throws Exception {
        for (int i = 0; i < size; i += 2) {
            dataOutput.writeShort(i);
        }
    }

    public void writeInts(DataOutput dataOutput)
            throws Exception {
        for (int i = 0; i < size; i += 4) {
            dataOutput.writeInt(i);
        }
    }

    public void writeString(DataOutput dataOutput)
            throws Exception {
        dataOutput.writeChars(outputString);
    }

    public void write(DataOutput dataOutput)
            throws Exception {
        switch (basicType) {
            case CHAR:
                writeChars(dataOutput);
                break;
            case SHORT:
                writeShorts(dataOutput);
                break;
            case INT:
                writeInts(dataOutput);
                break;
            case STRING:
                writeString(dataOutput);
                break;
        }
    }

    @Benchmark
    public void dataOutputStreamOverByteArray() throws Exception {
        byteArrayOutputStream.reset();
        write(byteArrayStream);
        byteArrayOutputStream.flush();
    }

    @Benchmark
    public void dataOutputStreamOverRawFileStream() throws Exception {
        fileOutputStream.getChannel().position(0);
        write(rawFileStream);
        fileOutputStream.flush();
    }

    @Benchmark
    public void dataOutputStreamOverBufferedFileStream() throws Exception{
        fileOutputStream.getChannel().position(0);
        write(bufferedFileStream);
        fileOutputStream.flush();
    }
}
