/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @summary Basic functional test of ThreadLocal
 * @author Josh Bloch
 */

public class Basic {
    static ThreadLocal n = new ThreadLocal() {
        int i = 0;
        protected synchronized Object initialValue() {
            return new Integer(i++);
        }
    };

    public static void main(String args[]) throws Exception {
        int threadCount = 100;
        Thread th[] = new Thread[threadCount];
        final int x[] = new int[threadCount];

        // Start the threads
        for(int i=0; i<threadCount; i++) {
            th[i] = new Thread() {
                public void run() {
                    int threadId = ((Integer)(n.get())).intValue();
                    for (int j=0; j<threadId; j++) {
                        x[threadId]++;
                        Thread.currentThread().yield();
                    }
                }
            };
            th[i].start();
        }

        // Wait for the threads to finish
        for(int i=0; i<threadCount; i++)
            th[i].join();

        // Check results
        for(int i=0; i<threadCount; i++)
            if (x[i] != i)
                throw(new Exception("x[" + i + "] =" + x[i]));
    }
}
