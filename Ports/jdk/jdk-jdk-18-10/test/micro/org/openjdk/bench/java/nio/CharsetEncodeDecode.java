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
package org.openjdk.bench.java.nio;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.concurrent.TimeUnit;

/**
 * This benchmark tests the encode/decode loops on different Charsets. It was created from an adhoc benchmark addressing
 * a performance issue which in the end boiled down to the encode/decode loops. This is the reason for the values on the
 * char and byte arrays.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class CharsetEncodeDecode {

    private byte[] BYTES;
    private char[] CHARS;

    private CharsetEncoder encoder;
    private CharsetDecoder decoder;

    @Param({"BIG5", "ISO-8859-15", "ASCII", "UTF-16"})
    private String type;

    @Param("16384")
    private int size;

    @Setup
    public void prepare() {
        BYTES = new byte[size];
        CHARS = new char[size];
        for (int i = 0; i < size; ++i) {
            int val = 48 + (i % 16);
            BYTES[i] = (byte) val;
            CHARS[i] = (char) val;
        }

        encoder = Charset.forName(type).newEncoder();
        decoder = Charset.forName(type).newDecoder();
    }

    @Benchmark
    public ByteBuffer encode() throws CharacterCodingException {
        CharBuffer charBuffer = CharBuffer.wrap(CHARS);
        return encoder.encode(charBuffer);
    }

    @Benchmark
    public CharBuffer decode() throws CharacterCodingException {
        ByteBuffer byteBuffer = ByteBuffer.wrap(BYTES);
        return decoder.decode(byteBuffer);
    }

}
