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
import java.io.Serializable;

/**
 * Benchmark for testing speed of object array reads/writes.
 */
public class ObjArrays implements Benchmark {

    static class Node implements Serializable {
        boolean z;
        byte b;
        char c;
        short s;
        int i;
        float f;
        long j;
        double d;
        String str = "bodega";
        Object parent, left, right;

        Node(Object parent, int depth) {
            this.parent = parent;
            if (depth > 0) {
                left = new Node(this, depth - 1);
                right = new Node(this, depth - 1);
            }
        }
    }

    /**
     * Write and read object arrays to/from a stream.  The benchmark is run in
     * batches, with each batch consisting of a fixed number of read/write
     * cycles.  The ObjectOutputStream is reset after each batch of cycles has
     * completed.
     * Arguments: <array size> <# batches> <# cycles per batch>
     */
    public long run(String[] args) throws Exception {
        int size = Integer.parseInt(args[0]);
        int nbatches = Integer.parseInt(args[1]);
        int ncycles = Integer.parseInt(args[2]);
        Node[][] arrays = genArrays(size, ncycles);
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
     * Generate object arrays.
     */
    Node[][] genArrays(int size, int narrays) {
        Node[][] arrays = new Node[narrays][size];
        for (int i = 0; i < narrays; i++) {
            for (int j = 0; j < size; j++) {
                arrays[i][j] = new Node(null, 0);
            }
        }
        return arrays;
    }

    /**
     * Run benchmark for given number of batches, with given number of cycles
     * for each batch.
     */
    void doReps(ObjectOutputStream oout, ObjectInputStream oin,
                StreamBuffer sbuf, Node[][] arrays, int nbatches)
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
