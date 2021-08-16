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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Benchmark for testing speed of proxy array reads/writes.
 */
public class ProxyArrays implements Benchmark {

    static class DummyHandler implements InvocationHandler, Serializable {
        public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable
        {
            return null;
        }
    }

    static interface DummyInterface {
        public void foo();
    }

    /**
     * Write and read proxy arrays to/from a stream.  The benchmark is run in
     * batches, with each batch consisting of a fixed number of read/write
     * cycles.  The ObjectOutputStream is reset after each batch of cycles has
     * completed.
     * Arguments: <array size> <# batches> <# cycles per batch>
     */
    public long run(String[] args) throws Exception {
        int size = Integer.parseInt(args[0]);
        int nbatches = Integer.parseInt(args[1]);
        int ncycles = Integer.parseInt(args[2]);
        Proxy[][] arrays = genArrays(size, ncycles);
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
     * Generate proxy arrays.
     */
    Proxy[][] genArrays(int size, int narrays) throws Exception {
        Class proxyClass =
            Proxy.getProxyClass(DummyInterface.class.getClassLoader(),
                    new Class[] { DummyInterface.class });
        Constructor proxyCons =
            proxyClass.getConstructor(new Class[] { InvocationHandler.class });
        Object[] consArgs = new Object[] { new DummyHandler() };
        Proxy[][] arrays = new Proxy[narrays][size];
        for (int i = 0; i < narrays; i++) {
            for (int j = 0; j < size; j++) {
                arrays[i][j] = (Proxy) proxyCons.newInstance(consArgs);
            }
        }
        return arrays;
    }

    /**
     * Run benchmark for given number of batches, with given number of cycles
     * for each batch.
     */
    void doReps(ObjectOutputStream oout, ObjectInputStream oin,
                StreamBuffer sbuf, Proxy[][] arrays, int nbatches)
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
