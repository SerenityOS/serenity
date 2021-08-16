/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 7038914 8016341
 * @summary Verify that the reference handler does not die after an OOME allocating the InterruptedException object
 * @run main/othervm -XX:-UseGCOverheadLimit -Xmx24M -XX:-UseTLAB OOMEInReferenceHandler
 * @author peter.levart@gmail.com
 * @key intermittent
 */

import java.lang.ref.*;

public class OOMEInReferenceHandler {
     static Object[] fillHeap() {
         Object[] first = null, last = null;
         int size = 1 << 20;
         while (size > 0) {
             try {
                 Object[] array = new Object[size];
                 if (first == null) {
                     first = array;
                 } else {
                     last[0] = array;
                 }
                 last = array;
             } catch (OutOfMemoryError oome) {
                 size = size >>> 1;
             }
         }
         return first;
     }

     public static void main(String[] args) throws Exception {
         // preinitialize the InterruptedException class so that the reference handler
         // does not die due to OOME when loading the class if it is the first use
         InterruptedException ie = new InterruptedException("dummy");

         ThreadGroup tg = Thread.currentThread().getThreadGroup();
         for (
             ThreadGroup tgn = tg;
             tgn != null;
             tg = tgn, tgn = tg.getParent()
             )
             ;

         Thread[] threads = new Thread[tg.activeCount()];
         Thread referenceHandlerThread = null;
         int n = tg.enumerate(threads);
         for (int i = 0; i < n; i++) {
             if ("Reference Handler".equals(threads[i].getName())) {
                 referenceHandlerThread = threads[i];
             }
         }

         if (referenceHandlerThread == null) {
             throw new IllegalStateException("Couldn't find Reference Handler thread.");
         }

         ReferenceQueue<Object> refQueue = new ReferenceQueue<>();
         Object referent = new Object();
         WeakReference<Object> weakRef = new WeakReference<>(referent, refQueue);

         Object waste = fillHeap();

         referenceHandlerThread.interrupt();

         // allow referenceHandlerThread some time to throw OOME
         Thread.sleep(500L);

         // release waste & referent
         waste = null;
         referent = null;

         // wait at most 10 seconds for success or failure
         for (int i = 0; i < 20; i++) {
             if (refQueue.poll() != null) {
                 // Reference Handler thread still working -> success
                 return;
             }
             System.gc();
             Thread.sleep(500L); // wait a little to allow GC to do it's work before allocating objects
             if (!referenceHandlerThread.isAlive()) {
                 // Reference Handler thread died -> failure
                 throw new Exception("Reference Handler thread died.");
             }
         }

         // no sure answer after 10 seconds
         throw new IllegalStateException("Reference Handler thread stuck. weakRef.get(): " + weakRef.get());
     }
}
