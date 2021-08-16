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
 * Benchmark for testing speed of reads/writes of repeated objects.
 */
public class RepeatObjs implements Benchmark {

    static class Node implements Serializable {
    }

    /**
     * Write and read repeated objects to/from a stream.  The benchmark is run
     * for a given number of batches.  Within each batch, a set of objects
     * is written to and read from the stream.  The set of objects remains the
     * same between batches (and the serialization streams are not reset) in
     * order to test the speed of object -> wire handle lookup, and vice versa.
     * Arguments: <# objects> <# cycles>
     */
    public long run(String[] args) throws Exception {
        int size = Integer.parseInt(args[0]);
        int nbatches = Integer.parseInt(args[1]);
        Node[] objs = genObjs(size);
        StreamBuffer sbuf = new StreamBuffer();
        ObjectOutputStream oout =
            new ObjectOutputStream(sbuf.getOutputStream());
        ObjectInputStream oin =
            new ObjectInputStream(sbuf.getInputStream());

        doReps(oout, oin, sbuf, objs, 1);       // warmup

        long start = System.currentTimeMillis();
        doReps(oout, oin, sbuf, objs, nbatches);
        return System.currentTimeMillis() - start;
    }

    /**
     * Generate objects.
     */
    Node[] genObjs(int nobjs) {
        Node[] objs = new Node[nobjs];
        for (int i = 0; i < nobjs; i++)
            objs[i] = new Node();
        return objs;
    }

    /**
     * Run benchmark for given number of batches.
     */
    void doReps(ObjectOutputStream oout, ObjectInputStream oin,
                StreamBuffer sbuf, Node[] objs, int nbatches)
        throws Exception
    {
        int nobjs = objs.length;
        for (int i = 0; i < nbatches; i++) {
            sbuf.reset();
            for (int j = 0; j < nobjs; j++) {
                oout.writeObject(objs[j]);
            }
            oout.flush();
            for (int j = 0; j < nobjs; j++) {
                oin.readObject();
            }
        }
    }
}
