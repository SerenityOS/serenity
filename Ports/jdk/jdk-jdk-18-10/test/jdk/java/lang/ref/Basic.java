/*
 * Copyright (c) 1997, 2012, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @summary Basic functional test of reference objects
 * @author Mark Reinhold
 */


import java.lang.ref.*;
import java.util.Vector;


public class Basic {

    static ReferenceQueue q = new ReferenceQueue();
    static ReferenceQueue q2 = new ReferenceQueue();
    static Reference rw, rw2, rp, rp2;
    static Vector keep = new Vector();
    static boolean finalized = false;

    public static class ClearFinalizerThread {
        protected void finalize() {
            System.err.println("Cleared finalizer thread");
        }
    };

    protected void finalize() {
        Basic.finalized = true;
        System.err.println("Finalized " + this);
    }

    public static class Sub { };

    Object sub = new Sub();

    static void fork(Runnable proc) throws InterruptedException {
        Thread t = new Thread(proc);
        t.start();
        t.join();
    }

    static void showReferences() throws InterruptedException {
        fork(new Runnable() {
            public void run() {
                System.err.println("References: W " + rw.get()
                                   + ", W2 " + rw2.get()
                                   + ", P " + rp.get()
                                   + ", P2 " + rp2.get());
            }
        });
    }

    static void createNoise() throws InterruptedException {
        fork(new Runnable() {
            public void run() {
                keep.addElement(new PhantomReference(new Object(), q2));
            }
        });
    }


    public static void main(String[] args) throws Exception {

        fork(new Runnable() {
            public void run() {
                Basic s = new Basic();
                rw = new WeakReference(s, q);
                rw2 = new WeakReference(s);
                rp = new PhantomReference(s, q);
                rp2 = new PhantomReference(s.sub, q);
                s = null;
            }
        });

        showReferences();

        int ndq = 0;
        boolean prevFinalized = false;
    outer:
        for (int i = 1;; i++) {
            Reference r;

            createNoise();
            System.err.println("GC " + i);
            Thread.sleep(10);
            System.gc();
            System.runFinalization();

            showReferences();
            while ((r = q2.poll()) != null) {
                System.err.println("Noise " + r);
            }

            /* Cause a dummy object to be finalized, since the finalizer thread
               might retain a reference to the Basic instance after it's been
               finalized (this happens with java_g) */
            if (Basic.finalized && !prevFinalized) {
                fork(new Runnable() {
                    public void run() {
                        new ClearFinalizerThread();
                    }});
                prevFinalized = true;
            }

            while ((r = q.poll()) != null) {
                ndq++;
                if (r != null) {
                    System.err.println("Dequeued " + r);
                    if (ndq == 3) break outer;
                }
            }

            if (i >= 10) break;

        }

        if (ndq != 3) {
            throw new Exception("Expected to dequeue 3 reference objects,"
                                + " but only got " + ndq);
        }

        if (! Basic.finalized) {
            throw new Exception("Test object not finalized");
        }

    }

}
