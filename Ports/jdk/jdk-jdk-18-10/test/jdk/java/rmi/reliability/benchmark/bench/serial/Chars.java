/*
 * Copyright (c) 1999, 2008, Oracle and/or its affiliates. All rights reserved.
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

/*
 *
 */

package bench.serial;

import bench.Benchmark;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Benchmark for testing speed of char reads/writes.
 */
public class Chars implements Benchmark {

    /**
     * Write and read char values to/from a stream.  The benchmark is run in
     * batches: each "batch" consists of a fixed number of read/write cycles,
     * and the stream is flushed (and underlying stream buffer cleared) in
     * between each batch.
     * Arguments: <# batches> <# cycles per batch>
     */
    public long run(String[] args) throws Exception {
        int nbatches = Integer.parseInt(args[0]);
        int ncycles = Integer.parseInt(args[1]);
        StreamBuffer sbuf = new StreamBuffer();
        ObjectOutputStream oout =
            new ObjectOutputStream(sbuf.getOutputStream());
        ObjectInputStream oin =
            new ObjectInputStream(sbuf.getInputStream());

        doReps(oout, oin, sbuf, 1, ncycles);    // warmup

        long start = System.currentTimeMillis();
        doReps(oout, oin, sbuf, nbatches, ncycles);
        return System.currentTimeMillis() - start;
    }

    /**
     * Run benchmark for given number of batches, with given number of cycles
     * for each batch.
     */
    void doReps(ObjectOutputStream oout, ObjectInputStream oin,
                StreamBuffer sbuf, int nbatches, int ncycles)
        throws Exception
    {
        for (int i = 0; i < nbatches; i++) {
            sbuf.reset();
            for (int j = 0; j < ncycles; j++) {
                oout.writeChar('0');
            }
            oout.flush();
            for (int j = 0; j < ncycles; j++) {
                oin.readChar();
            }
        }
    }
}
