/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6275329
 * @summary lazySet methods
 */

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class Lazy {
    volatile int ii;
    volatile long ll;
    volatile Boolean bb;
    static final Lazy z = new Lazy();

    public static void main(String[] args) throws Exception {
        final AtomicBoolean b = new AtomicBoolean();
        final AtomicInteger i = new AtomicInteger();
        final AtomicLong    l = new AtomicLong();
        final AtomicReference<Long> r = new AtomicReference<>();

        final AtomicIntegerArray ia = new AtomicIntegerArray(1);
        final AtomicLongArray    la = new AtomicLongArray(1);
        final AtomicReferenceArray<Long> ra = new AtomicReferenceArray<>(1);

        final AtomicIntegerFieldUpdater<Lazy> iu =
            AtomicIntegerFieldUpdater.newUpdater(Lazy.class, "ii");
        final AtomicLongFieldUpdater<Lazy> lu =
            AtomicLongFieldUpdater.newUpdater(Lazy.class, "ll");
        final AtomicReferenceFieldUpdater<Lazy,Boolean> ru =
            AtomicReferenceFieldUpdater.newUpdater(Lazy.class,
                                                   Boolean.class, "bb");

        Thread[] threads = {
            new Thread() { public void run() { b.lazySet(true);    }},
            new Thread() { public void run() { i.lazySet(2);       }},
            new Thread() { public void run() { l.lazySet(3L);      }},
            new Thread() { public void run() { r.lazySet(9L);      }},
            new Thread() { public void run() { ia.lazySet(0,4);    }},
            new Thread() { public void run() { la.lazySet(0,5L);   }},
            new Thread() { public void run() { ra.lazySet(0,6L);   }},
            new Thread() { public void run() { iu.lazySet(z,7);    }},
            new Thread() { public void run() { lu.lazySet(z,8L);   }},
            new Thread() { public void run() { ru.lazySet(z,true); }}};

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        if (! (b.get()   == true &&
               i.get()   == 2    &&
               l.get()   == 3L   &&
               r.get()   == 9L   &&
               ia.get(0) == 4    &&
               la.get(0) == 5L   &&
               ra.get(0) == 6L   &&
               z.ii      == 7    &&
               z.ll      == 8L   &&
               z.bb      == true))
            throw new Exception("lazySet failed");
    }
}
