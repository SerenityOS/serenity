/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
package gc.g1.unloading.keepref;

import gc.g1.unloading.check.cleanup.UnusedThreadKiller;

/**
 * This holder prevents class from being unloaded by keeping reference in stack local.
 *
 */
public class InStackLocalHolder implements RefHolder {

    public static class AuxiliaryThread extends Thread {
        private static final int STACK_DEPTH = 300;

        private ReferenceHolder referenceHolder;

        public AuxiliaryThread(ReferenceHolder referenceHolder) {
            this.referenceHolder = referenceHolder;
        }

        synchronized public void finishThread() {
            notifyAll();
        }

        @Override
        synchronized public void run() {
            new A().call(referenceHolder, STACK_DEPTH);
        }
    }

    private static class A {
        public void call(ReferenceHolder holder, int stackDepth) {
            if (stackDepth > 0) {
                new A().call(holder, stackDepth - 1);
            } else {
                Object ref = holder.obtainAndClear();
                synchronized (Thread.currentThread()) {
                    try {
                        Thread.currentThread().wait();
                        if (ref.hashCode() == 42) {
                            System.out.println("This clause is made to prevent compiler and javac optimizations from eliminating local reference \"ref\".");
                        }
                    } catch (InterruptedException e) {
                        new RuntimeException("Unexpected InterruptedException");
                    }
                }
            }
        }
    }

    private static class ReferenceHolder {
        private Object reference;

        public ReferenceHolder(Object reference) {
            this.reference = reference;
        }

        public Object obtainAndClear() {
            Object returnValue = reference;
            reference = new Object();
            return returnValue;
        }

    }

    @Override
    public Object hold(Object object) {
        Thread thread = new AuxiliaryThread(new ReferenceHolder(object));
        thread.setDaemon(true);
        thread.start();
        return new UnusedThreadKiller(thread.getId());
    }

}
