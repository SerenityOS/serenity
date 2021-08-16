/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
package gc.gctests.StringInternSyncWithGC;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import nsk.share.TestBug;
import nsk.share.TestFailure;
import nsk.share.gc.gp.string.RandomStringProducer;
import nsk.share.test.ExecutionController;
import nsk.share.test.LocalRandom;

class StringGenerator implements Runnable {

    private final RandomStringProducer gp;
    private final List<String> stringsToIntern;
    private final int threadNumber;
    private final int numberOfActions;
    private final int maxStringSize;
    private final StringInternSyncWithGC base;
    private static final ReadWriteLock RWLOCK = new ReentrantReadWriteLock();

    public StringGenerator(int threadId, StringInternSyncWithGC base) {
        this.base = base;
        threadNumber = threadId;
        stringsToIntern = base.getStringsToIntern();
        numberOfActions = base.getNumberOfThreads() > 4 ? base.getNumberOfThreads() : 4;
        gp = base.getGarbageProducer();
        maxStringSize = base.getMaxStringSize();
    }

    /* This field is public just to be not optimized */
    public String copy;

    @Override
    public void run() {
        ExecutionController stresser = base.getExecController();
        try {
            RWLOCK.readLock().lock();
            Object[] refToInterned = new Object[stringsToIntern.size()];
            for (int i = 0; i < stringsToIntern.size(); i++) {
                if (!stresser.continueExecution()) {
                    return;
                }
                int index = LocalRandom.nextInt(stringsToIntern.size());
                String str = stringsToIntern.get(index);
                int action = LocalRandom.nextInt(numberOfActions);

                /* We want to provoke a lot of collections for each interned copy
                 * so map should be "sparse".
                 */
                copy = new String(str);
                switch (action) {
                    case 0:
                        refToInterned[index] = copy.intern();
                        break;
                    case 1:
                        refToInterned[index] = new WeakReference(copy.intern());
                        break;
                    case 2:
                        refToInterned[index] = new SoftReference(copy.intern());
                        break;
                    default:
                        refToInterned[index] = null;
                        break;
                }
            }
            for (int index = 0; index < stringsToIntern.size(); index++) {
                verify(refToInterned[index], stringsToIntern.get(index));
            }
        } finally {
            RWLOCK.readLock().unlock();
        }

        if (threadNumber == 0) {
            try {
                RWLOCK.writeLock().lock();
                for (int index = 0; index < stringsToIntern.size(); index++) {
                    stringsToIntern.set(index, gp.create(maxStringSize).intern());
                }
            } finally {
                RWLOCK.writeLock().unlock();
            }
        }
    }

    /*
     * Verify that all exist interned strings in a map
     * a same objects.
     */
    private void verify(Object obj, String str) {
        if (obj == null) {
            return;
        }
        if (obj instanceof Reference) {
            obj = ((Reference) obj).get();
            if (obj == null) {
                return;
            }
        }
        if (!(obj instanceof String)) {
            throw new TestBug("Expected String. Find :" + obj.getClass());
        }
        String interned = (String) obj;
        if (!interned.equals(str)) {
            throw new TestFailure("Interned not equals to original string.");
        }
        if (obj != str.intern()) {
            throw new TestFailure("Interned not same as original string.");
        }
    }
}
