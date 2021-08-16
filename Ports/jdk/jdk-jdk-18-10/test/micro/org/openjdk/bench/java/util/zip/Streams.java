/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
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

package org.openjdk.bench.java.util.zip;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;
import java.util.zip.DeflaterOutputStream;

/**
 * Test the average execution time of "InflaterOutputStream.write()" depending
 * on the size of the internal "InflaterOutputStream" byte buffer and the size
 * of the compressed data input buffer passed to "write()".
 *
 * The size of the compressed data input buffer is controlled by the "size"
 * parameter which runs from "512" to "65536".
 *
 * The size of the internal byte buffer is a multiple of "size" controlled by
 * the "scale" paramter which runs from "1" to "8".
 *
 * For peak perfomance the internal buffer should be big enough to hold all
 * the data decompressed from the input buffer. This of course depends on
 * the compression rate of the input data. E.g. if the compression rate of
 * the compressed input data is 4 (i.e. the original input data was compressed
 * to 1/4 of its original size) the internal buffer should be four times bigger
 * than the size of the compressed data buffer passed to "write()" because in
 * that case one single call to the native zlib "inflate()" method is sufficent
 * to decompress all data and store it in the output buffer from where it can
 * be written to the output stream with one single call to the output streams
 * "write()" method.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class Streams {

    private FileInputStream in;
    private FileOutputStream out;
    @Param({"512", "1024", "2048", "4096", "8192", "16384", "32768", "65536"})
    private int size;
    @Param({"1", "2", "4", "8"})
    private int scale;
    private byte[] buf;

    private static byte[] data = new byte[1024 * 1024];

    @Setup(Level.Trial)
    public void beforeRun() throws IOException {
        // The reason for this whole dance is to programmatically create a one
        // megabyte file which can be compressed by factor ~6. This will give
        // us good results for the various scale factors (i.e. the relation
        // between the deflated input buffer and the inflated output buffer).
        // We achieve the desired compression factor by creating a 64 byte
        // array of random data and than fill the final 1mb file with random
        // 8-byte substrings of these 64 random bytes. This vaguely mimics
        // a language with 8 character words over a set of 64 different characters.
        final int characters = 64;
        final int wordLength = 8;
        buf = new byte[characters];
        Random r = new Random(123456789);
        r.nextBytes(buf);
        for (int i = 0; i < data.length / wordLength; i++) {
            System.arraycopy(buf, r.nextInt(characters - wordLength), data, i * wordLength, wordLength);
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(data);

        File deflated = File.createTempFile("inflaterOutputStreamWrite", ".deflated");
        deflated.deleteOnExit();
        FileOutputStream fout = new FileOutputStream(deflated);
        DeflaterOutputStream defout = new DeflaterOutputStream(fout);
        bais.transferTo(defout);
        // We need to close the DeflaterOutputStream in order to flush all the
        // compressed data in the Deflater and the underlying FileOutputStream.
        defout.close();
        in = new FileInputStream(deflated);
        File inflated = File.createTempFile("inflaterOutputStreamWrite", ".inflated");
        inflated.deleteOnExit();
        out = new FileOutputStream(inflated);
    }

    @Setup(Level.Iteration)
    public void beforeIteration() throws IOException {
        in.getChannel().position(0);
        out.getChannel().position(0);
        buf = new byte[size];
    }

    @Benchmark
    public void inflaterOutputStreamWrite() throws IOException {
        in.getChannel().position(0);
        out.getChannel().position(0);
        InflaterOutputStream inflate = new InflaterOutputStream(out, new Inflater(), scale * size);
        int len;
        // buf.length == size
        while ((len = in.read(buf)) != -1) {
            inflate.write(buf, 0, len);
        }
        inflate.finish();
    }
}
