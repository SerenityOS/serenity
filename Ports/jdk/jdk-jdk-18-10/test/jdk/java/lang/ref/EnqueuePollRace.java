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

/* @test
 * @bug 8014890
 * @summary Verify that a race between ReferenceQueue.enqueue() and poll() does not occur.
 * @author thomas.schatzl@oracle.com
 * @run main/othervm -Xmx10M EnqueuePollRace
 */

import java.lang.ref.*;

public class EnqueuePollRace {

    public static void main(String args[]) throws Exception {
        new WeakRef().run();
        System.out.println("Test passed.");
    }

    static class WeakRef {
        ReferenceQueue<Object> queue = new ReferenceQueue<Object>();
        final int numReferences = 100;
        final Reference refs[] = new Reference[numReferences];
        final int iterations = 1000;

        void run() throws InterruptedException {
            for (int i = 0; i < iterations; i++) {
                queue = new ReferenceQueue<Object>();

                for (int j = 0; j < refs.length; j++) {
                    refs[j] = new WeakReference(new Object(), queue);
                }

                System.gc(); // enqueues references into the list of discovered references

                // now manually enqueue all of them
                for (int j = 0; j < refs.length; j++) {
                    refs[j].enqueue();
                }
                // and get them back. There should be exactly numReferences
                // entries in the queue now.
                int foundReferences = 0;
                while (queue.poll() != null) {
                    foundReferences++;
                }

                if (foundReferences != refs.length) {
                    throw new RuntimeException("Got " + foundReferences + " references in the queue, but expected " + refs.length);
                }
            }
        }
    }
}
