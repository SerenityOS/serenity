/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7190310
 * @summary Inlining WeakReference.get(), and hoisting $referent may lead to non-terminating loops
 *
 * @run main/othervm/timeout=600 -Xbatch compiler.c2.Test7190310
 */

/*
 * Note bug exhibits as infinite loop, timeout is helpful.
 * It should normally finish pretty quickly, but on some especially slow machines
 * it may not.  The companion _unsafe test lacks a timeout, but that is okay.
 */
package compiler.c2;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public class Test7190310 {
    private static Object str = new Object() {
        public String toString() {
            return "The Object";
        }

        protected void finalize() throws Throwable {
            System.out.println("The Object is being finalized");
            super.finalize();
        }
    };
    private final static ReferenceQueue<Object> rq =
            new ReferenceQueue<Object>();
    private final static WeakReference<Object> wr =
            new WeakReference<Object>(str, rq);

    public static void main(String[] args)
            throws InterruptedException {
        Thread reader = new Thread() {
            public void run() {
                while (wr.get() != null) {
                }
                System.out.println("wr.get() returned null");
            }
        };

        Thread queueReader = new Thread() {
            public void run() {
                try {
                    Reference<? extends Object> ref = rq.remove();
                    System.out.println(ref);
                    System.out.println("queueReader returned, ref==wr is "
                            + (ref == wr));
                } catch (InterruptedException e) {
                    System.err.println("Sleep interrupted - exiting");
                }
            }
        };

        reader.start();
        queueReader.start();

        Thread.sleep(1000);
        str = null;
        System.gc();
    }
}

