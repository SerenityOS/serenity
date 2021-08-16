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
import java.io.ObjectStreamClass;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * Benchmark for testing speed of proxy class descriptor reads/writes.
 */
public class ProxyClassDesc implements Benchmark {

    static interface A1 {};
    static interface A2 {};
    static interface A3 {};
    static interface A4 {};
    static interface A5 {};
    static interface B1 {};
    static interface B2 {};
    static interface B3 {};
    static interface B4 {};
    static interface B5 {};
    static interface C1 {};
    static interface C2 {};
    static interface C3 {};
    static interface C4 {};
    static interface C5 {};

    /**
     * Write and read proxy class descriptors to/from a stream.
     * Arguments: <# cycles>
     */
    public long run(String[] args) throws Exception {
        int ncycles = Integer.parseInt(args[0]);
        StreamBuffer sbuf = new StreamBuffer();
        ObjectOutputStream oout =
            new ObjectOutputStream(sbuf.getOutputStream());
        ObjectInputStream oin =
            new ObjectInputStream(sbuf.getInputStream());
        ObjectStreamClass[] descs = genDescs();

        doReps(oout, oin, sbuf, descs, 1);      // warmup

        long start = System.currentTimeMillis();
        doReps(oout, oin, sbuf, descs, ncycles);
        return System.currentTimeMillis() - start;
    }

    /**
     * Generate proxy class descriptors.
     */
    ObjectStreamClass[] genDescs() {
        ClassLoader ldr = ProxyClassDesc.class.getClassLoader();
        Class[] ifaces = new Class[3];
        Class[] a =
            new Class[] { A1.class, A2.class, A3.class, A4.class, A5.class };
        Class[] b =
            new Class[] { B1.class, B2.class, B3.class, B4.class, B5.class };
        Class[] c =
            new Class[] { C1.class, C2.class, C3.class, C4.class, C5.class };
        ObjectStreamClass[] descs =
            new ObjectStreamClass[a.length * b.length * c.length];
        int n = 0;
        for (int i = 0; i < a.length; i++) {
            ifaces[0] = a[i];
            for (int j = 0; j < b.length; j++) {
                ifaces[1] = b[j];
                for (int k = 0; k < c.length; k++) {
                    ifaces[2] = c[k];
                    Class proxyClass = Proxy.getProxyClass(ldr, ifaces);
                    descs[n++] = ObjectStreamClass.lookup(proxyClass);
                }
            }
        }
        return descs;
    }

    /**
     * Run benchmark for given number of cycles.
     */
    void doReps(ObjectOutputStream oout, ObjectInputStream oin,
                StreamBuffer sbuf, ObjectStreamClass[] descs, int ncycles)
        throws Exception
    {
        int ndescs = descs.length;
        for (int i = 0; i < ncycles; i++) {
            sbuf.reset();
            oout.reset();
            for (int j = 0; j < ndescs; j++) {
                oout.writeObject(descs[j]);
            }
            oout.flush();
            for (int j = 0; j < ndescs; j++) {
                oin.readObject();
            }
        }
    }
}
