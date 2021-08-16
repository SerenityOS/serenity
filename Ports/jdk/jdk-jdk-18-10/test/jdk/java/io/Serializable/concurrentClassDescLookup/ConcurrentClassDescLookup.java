/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Verify that concurrent class descriptor lookups function properly,
 *          even when class descriptor initialization is slow or throws an
 *          exception.
 */

import java.io.*;

class Good implements Serializable {
    private static final long serialVersionUID = 6319710844400051132L;

    static {
        try { Thread.sleep(1000); } catch (InterruptedException ex) {}
    }
}

class Bad implements Serializable {
    // explicit suid triggers class initialization during classdesc lookup
    private static final long serialVersionUID = 0xBAD;
    static {
        try { Thread.sleep(1000); } catch (InterruptedException ex) {}
        if ("foo".equals("foo")) {
            throw new RuntimeException();
        }
    }
}

class SuccessfulLookup extends Thread {
    Class<?> cl;
    long suid;
    Object barrier;
    boolean ok;

    SuccessfulLookup(Class<?> cl, long suid, Object barrier) {
        this.cl = cl;
        this.suid = suid;
        this.barrier = barrier;
    }

    public void run() {
        synchronized (barrier) {
            try { barrier.wait(); } catch (InterruptedException ex) {}
        }
        for (int i = 0; i < 100; i++) {
            if (ObjectStreamClass.lookup(cl).getSerialVersionUID() != suid) {
                return;
            }
        }
        ok = true;
    }
}

class FailingLookup extends Thread {
    Class<?> cl;
    final Object barrier;
    boolean ok;

    FailingLookup(Class<?> cl, Object barrier) {
        this.cl = cl;
        this.barrier = barrier;
    }

    public void run() {
        synchronized (barrier) {
            try { barrier.wait(); } catch (InterruptedException ex) {}
        }
        for (int i = 0; i < 100; i++) {
            try {
                ObjectStreamClass.lookup(cl);
                return;
            } catch (Throwable th) {
            }
        }
        ok = true;
    }
}

public class ConcurrentClassDescLookup {
    public static void main(String[] args) throws Exception {
        ClassLoader loader = ConcurrentClassDescLookup.class.getClassLoader();
        Class<?> cl = Class.forName("Good", false, loader);
        Object barrier = new Object();
        SuccessfulLookup[] slookups = new SuccessfulLookup[50];
        for (int i = 0; i < slookups.length; i++) {
            slookups[i] =
                new SuccessfulLookup(cl, 6319710844400051132L, barrier);
            slookups[i].start();
        }
        Thread.sleep(1000);
        synchronized (barrier) {
            barrier.notifyAll();
        }
        for (int i = 0; i < slookups.length; i++) {
            slookups[i].join();
            if (!slookups[i].ok) {
                throw new Error();
            }
        }

        cl = Class.forName("Bad", false, loader);
        FailingLookup[] flookups = new FailingLookup[50];
        for (int i = 0; i < flookups.length; i++) {
            flookups[i] = new FailingLookup(cl, barrier);
            flookups[i].start();
        }
        Thread.sleep(1000);
        synchronized (barrier) {
            barrier.notifyAll();
        }
        for (int i = 0; i < slookups.length; i++) {
            flookups[i].join();
            if (!flookups[i].ok) {
                throw new Error();
            }
        }
    }
}
