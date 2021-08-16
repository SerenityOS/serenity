/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.stress.jni;

class GarbageGenerator extends Thread {
    class Garbage {
        Garbage() {
            this(1024);
        }

        Garbage(int m) {
            memory = new byte[m];
        }

        void setNext(Garbage n) {
            next = n;
        }

        Garbage getNext() {
            return next;
        }

        protected void finalize() {
        }

        private Garbage next;
        private byte[] memory;
    }

    class GarbageRing {
        GarbageRing() {
            attachment = new Garbage(0);
        }

        void add(int size) {
            Garbage head = attachment.getNext();
            Garbage g = new Garbage(size);
            if (head != null) {
                Garbage oldNext = head.getNext();
                if (oldNext != null) {
                    g.setNext(oldNext);
                    head.setNext(g);
                    attachment.setNext(g);
                } else {
                    g.setNext(head);
                    head.setNext(g);
                }
            } else
                attachment.setNext(g);
        }

        void discard() {
            attachment.setNext(null);
        }

        private byte[] memory;
        private Garbage attachment;
    }

    public void run() {
        GarbageRing gr = new GarbageRing();
        int g = 0;
        while (!done) {
            for (g = 0; g < ringSize; g++) {
                gr.add(allocSize);
                Thread.yield();
            }
            gr.discard();
            try {
                sleep(interval);
            } catch (InterruptedException e) {
            }
        }
        if (DEBUG) System.out.println("GarbageRing::run(): done");
    }

    public void setAllocSize(int i) {
        allocSize = i;
    }

    public int getAllocSize() {
        return allocSize;
    }

    public void setInterval(int i) {
        interval = i;
    }

    public int getInterval() {
        return interval;
    }

    public void halt() {
        done = true;
    }

    private int allocSize = 10000;
    private int ringSize = 50;
    private int interval = 1000;
    private boolean done = false;
    final private static boolean DEBUG = false;
}
