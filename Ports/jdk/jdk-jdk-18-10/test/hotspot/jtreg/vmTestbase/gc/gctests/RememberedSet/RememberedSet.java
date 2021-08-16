/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * This test stress RememberetSet procerssing in the G1 by creation of references
 * between different 1MB blocks.
 * Test is specific for G1, for other GCs it should just pass.
 */


/*
 * @test
 * @modules java.base/jdk.internal.misc:+open java.base/jdk.internal.vm.annotation:+open java.base/sun.reflect.annotation:+open
 * @key stress
 *
 * @summary converted from VM Testbase gc/gctests/RememberedSet.
 * VM Testbase keywords: [gc, stress, stressopt, feature_g1, nonconcurrent]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm -XX:-UseGCOverheadLimit gc.gctests.RememberedSet.RememberedSet
 */

package gc.gctests.RememberedSet;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import nsk.share.gc.GC;
import nsk.share.gc.MemoryObject;
import nsk.share.gc.ThreadedGCTest;
import nsk.share.test.ExecutionController;
import jdk.internal.misc.Unsafe;

public class RememberedSet extends ThreadedGCTest {

    static class PointerUtils {
        private static Unsafe unsafe;
        private static long fieldOffset;
        private static PointerUtils instance = new PointerUtils();
        private static boolean compressedRef = false;

        static {
            try {
                unsafe = Unsafe.getUnsafe();
                fieldOffset = unsafe.objectFieldOffset(PointerUtils.class.getDeclaredField("obj"));
                long fieldOffset0 = unsafe.objectFieldOffset(PointerUtils.class.getDeclaredField("obj0"));
                int oopSize = (int)Math.abs(fieldOffset - fieldOffset0);

                if (oopSize != unsafe.addressSize()) {
                    System.out.println("Compressed oops detected");
                    compressedRef = true;
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        private Object obj;
        private Object obj0;

        public synchronized static long toAddress(Object o) {
            long address;
            instance.obj = o;

            if (compressedRef || unsafe.addressSize() == 4) {
                address = unsafe.getInt(instance, fieldOffset);
            }
            else {
                address = unsafe.getLong(instance, fieldOffset);
            }

            return address;
        }

    }
    private ExecutionController stresser;

    @Override
    protected Runnable createRunnable(int i) {
        return new Worker();
    }

    class Worker implements Runnable {

        static final long BLOCK_SIZE = 1024 * 1024;


        // this method tries to allocate a new MemoryObject
        // which is in another 1MB block
        MemoryObject getOutOfTheBlockObject(int size, Object obj) {
            long address = PointerUtils.toAddress(obj);
            MemoryObject ref = new MemoryObject(size);
            int attempt = (int) (BLOCK_SIZE / size);
            while (attempt != 0 && Math.abs(address - PointerUtils.toAddress(ref)) < BLOCK_SIZE) {
                ref = new MemoryObject(size);
                attempt--;
            }
            return ref;
        }

        @Override
        public void run() {

            int size = (int) Math.sqrt(BLOCK_SIZE);
            int refsCount = (int) (runParams.getTestMemory() / BLOCK_SIZE);
            int count = (int) (runParams.getTestMemory() / runParams.getNumberOfThreads() / (refsCount * size));
            // Each cycle 10% of references and 10% of arrays are reallocated
            int step = 10;

            List<List<MemoryObject>> objs = new ArrayList<List<MemoryObject>>(count);
            for (int i = 0; i < count; i++) {
                List<MemoryObject> obj = new ArrayList<MemoryObject>();
                objs.add(obj);
                for (int j = 0; j < refsCount; j++) {
                    obj.add(getOutOfTheBlockObject(size, obj));
                }
            }
            if (stresser == null) {
                stresser = getExecutionController();
            }
            int shift = 0;
            while (stresser.continueExecution()) {
                for (int j = shift; j < refsCount; j += step) {
                    for (int i = 0; i < count; i ++) {
                        // update each 10th reference to allow GC previous one
                        List<MemoryObject> obj = objs.get(i);
                        obj.set(j, getOutOfTheBlockObject(size, obj));
                    }
                }
                for (int i = step - shift; i < count; i += step) {
                    // update each 10th array of references
                    // to allocate it in the another 1MB block (as new young object)
                    List<MemoryObject> obj = new ArrayList<MemoryObject>();
                    objs.set(i, obj);
                    for (int j = 0; j < refsCount; j++) {
                        obj.add(getOutOfTheBlockObject(size, obj));
                    }
                }
                // shift is changed from 0 to step - 1
                log.debug("shift = " + shift);
                shift = (shift + 1) % step;
            }
        }
    }

    public static void main(String[] args) {
        GC.runTest(new RememberedSet(), args);
    }
}
