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
 * Benchmark for testing speed of boolean array reads/writes.
 */
public class BooleanArrays implements Benchmark {

    /**
     * Write and read boolean arrays to/from a stream.  The benchmark is run in
     * batches, with each batch consisting of a fixed number of read/write
     * cycles.  The ObjectOutputStream is reset after each batch of cycles has
     * completed.
     * Arguments: <array size> <# batches> <# cycles per batch>
     */
    public long run(String[] args) throws Exception {
        int size = Integer.parseInt(args[0]);
        int nbatches = Integer.parseInt(args[1]);
        int ncycles = Integer.parseInt(args[2]);
        boolean[][] arrays = new boolean[ncycles][size];
        StreamBuffer sbuf = new StreamBuffer();
        ObjectOutputStream oout =
            new ObjectOutputStream(sbuf.getOutputStream());
        ObjectInputStream oin =
            new ObjectInputStream(sbuf.getInputStream());

        doReps(oout, oin, sbuf, arrays, 1);     // warmup

        long start = System.currentTimeMillis();
        doReps(oout, oin, sbuf, arrays, nbatches);
        return System.currentTimeMillis() - start;
    }

    /**
     * Run benchmark for given number of batches, with given number of cycles
     * for each batch.
     */
    void doReps(ObjectOutputStream oout, ObjectInputStream oin,
                StreamBuffer sbuf, boolean[][] arrays, int nbatches)
        throws Exception
    {
        int ncycles = arrays.length;
        for (int i = 0; i < nbatches; i++) {
            sbuf.reset();
            oout.reset();
            for (int j = 0; j < ncycles; j++) {
                oout.writeObject(arrays[j]);
            }
            oout.flush();
            for (int j = 0; j < ncycles; j++) {
                oin.readObject();
            }
        }
    }
}
