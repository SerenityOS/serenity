/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @key stress randomness
 *
 * @summary converted from VM testbase nsk/stress/jni/gclocker/gcl001.
 * VM testbase keywords: [stress, quick, feature_283, nonconcurrent]
 * VM testbase readme:
 * DESCRIPTION
 *     Check compatibility of GC Locker improvements with JNI CS
 * Two types of concurrent threads are present:
 * A) Create N 'JNI CS' threads. Each of them will:
 *    1. Create primitive array and string with random data
 *    2. Pass it to native method
 *    3. Sort/Hash data in JNI CS mixing string and array critical sections
 *    4. Return from native
 *    5. Check data to be processed correctly
 * B) Create M ' Garbage producer/memory allocation' threads. Each of them will:
 *    1. Allocate memory blocks and make them garbage.
 *    2. Check for OOM errors.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native/timeout=480
 *      -XX:-UseGCOverheadLimit
 *      nsk.stress.jni.gclocker.gcl001
 *      -stressThreadsFactor 8
 */

package nsk.stress.jni.gclocker;

import nsk.share.gc.GC;
import nsk.share.gc.ThreadedGCTest;
import nsk.share.gc.gp.array.BooleanArrayProducer;
import nsk.share.gc.gp.array.ByteArrayProducer;
import nsk.share.gc.gp.array.CharArrayProducer;
import nsk.share.gc.gp.array.DoubleArrayProducer;
import nsk.share.gc.gp.array.FloatArrayProducer;
import nsk.share.gc.gp.array.IntArrayProducer;
import nsk.share.gc.gp.array.LongArrayProducer;
import nsk.share.gc.gp.array.ShortArrayProducer;
import nsk.share.test.ExecutionController;
import nsk.share.test.LocalRandom;

public class gcl001 extends ThreadedGCTest {

    static {
        System.loadLibrary("gcl001");
    }

    int maxBlockSize;

    public static void main(String[] args) {
        GC.runTest(new gcl001(), args);
    }

    @Override
    public void run() {
        // each thread have only one block at the time
        // so we should occupy less then 60% of heap with live objects
        long maxSize = runParams.getTestMemory() / runParams.getNumberOfThreads();
        if (maxSize > Integer.MAX_VALUE - 1) {
            maxSize = Integer.MAX_VALUE - 1;
        }
        maxBlockSize = (int) maxSize;
        log.info("Maximium block size = " + maxBlockSize);
        super.run();
    }

    @Override
    protected Runnable createRunnable(int i) {
        if (i < runParams.getNumberOfThreads() / 2) {
            return CreateJNIWorker(i, maxBlockSize);
        } else {
            return new GarbageProducer(maxBlockSize);
        }
    }

    public Runnable CreateJNIWorker(int number, int size) {
        JNIAbstractWorker worker = null;
        switch (number % 8) {
            case 0:
                worker = new JNIBooleanWorker(size);
                break;
            case 1:
                worker = new JNIByteWorker(size);
                break;
            case 2:
                worker = new JNICharWorker(size);
                break;
            case 3:
                worker = new JNIShortWorker(size);
                break;
            case 4:
                worker = new JNIIntWorker(size);
                break;
            case 5:
                worker = new JNILongWorker(size);
                break;
            case 6:
                worker = new JNIFloatWorker(size);
                break;
            case 7:
                worker = new JNIDoubleWorker(size);
                break;
        }
        return worker;
    }

    int random(int maxSize) {
        int res = LocalRandom.nextInt(maxSize);
        return res > 128 ? res : 128;
    }

    public static Object tmp;

    class GarbageProducer implements Runnable {

        private int maxSize;
        ExecutionController stresser;
        ByteArrayProducer bp;

        GarbageProducer(int size) {
            this.maxSize = size;
            bp = new ByteArrayProducer();
        }

        public void run() {
            if (stresser == null) {
                stresser = getExecutionController();
            }

            while (stresser.continueExecution()) {
                try {
                    byte[] arr = bp.create(random(maxSize));
                    tmp = arr;
                } catch (OutOfMemoryError oome) {
                    // Do nothing.
                }
            }
        }
    }

    abstract class JNIAbstractWorker extends JNIWorker implements Runnable {

        ExecutionController stresser;
        private int maxSize;

        public JNIAbstractWorker(int maxSize) {
            this.maxSize = maxSize;
        }

        public void check(boolean condition, String message) {
            if (!condition) {
                throw new Error(message);
            }
        }

        abstract void doit(int size);

        public void run() {
            // create array with random elements
            // store min and max element for further check
            // create new string
            // call JNI methods
            // check min/max as sort result
            // check string hash

            if (stresser == null) {
                stresser = getExecutionController();
            }
            while (stresser.continueExecution()) {
                // let make at least several values for long/float
                try {
                    doit(random(maxSize));
                } catch (OutOfMemoryError oome) {
                    // Do nothing.
                }
            }
        }
    }

    // BYTE
    class JNIBooleanWorker extends JNIAbstractWorker {

        BooleanArrayProducer gp = new BooleanArrayProducer();

        public JNIBooleanWorker(int size) {
            super(size);
        }

        void doit(int size) {

            boolean[] array = gp.create(size);
            // just to be sure that we have true & false
            array[0] = true;
            array[array.length - 1] = false;
            String str = "unsupported";
            int nativeHash = NativeCall(array, str);
            int javaHash = 0;
            for (int i = 0; i < str.length(); ++i) {
                javaHash += (int) str.charAt(i);
            }
            javaHash += javaHash;
            check(array[0] == false && array[array.length - 1] == true
                    && javaHash == nativeHash, "Data validation failure");

        }
    }

    class JNIByteWorker extends JNIAbstractWorker {

        ByteArrayProducer gp = new ByteArrayProducer();

        public JNIByteWorker(int size) {
            super(size);
        }

        void doit(int size) {

            byte[] array = gp.create(size);
            byte min = Byte.MAX_VALUE, max = Byte.MIN_VALUE;
            for (int i = 0; i < array.length; ++i) {
                if (array[i] > max) {
                    max = array[i];
                }

                if (array[i] < min) {
                    min = array[i];
                }
            }
            String str = "Min: " + min + " Max: " + max;
            int nativeHash = NativeCall(array, str);
            int javaHash = 0;
            for (int i = 0; i < str.length(); ++i) {
                javaHash += (int) str.charAt(i);
            }
            javaHash += javaHash;
            check(array[0] == min && array[array.length - 1] == max
                    && javaHash == nativeHash, "Data validation failure");

        }
    }

    // CHAR
    class JNICharWorker extends JNIAbstractWorker {

        CharArrayProducer gp = new CharArrayProducer();

        public JNICharWorker(int size) {
            super(size);
        }

        void doit(int size) {
            char[] array = gp.create(size);
            char min = 0xffff, max = 0;
            for (int i = 0; i < array.length; ++i) {
                array[i] = (char) LocalRandom.nextInt();
                if (array[i] > max) {
                    max = array[i];
                }

                if (array[i] < min) {
                    min = array[i];
                }
            }
            String str = "Min: " + min + " Max: " + max;
            int nativeHash = NativeCall(array, str);
            int javaHash = 0;
            for (int i = 0; i < str.length(); ++i) {
                javaHash += (int) str.charAt(i);
            }
            javaHash += javaHash;
            check(array[0] == min && array[array.length - 1] == max
                    && javaHash == nativeHash, "Data validation failure");

        }
    }

    // SHORT
    class JNIShortWorker extends JNIAbstractWorker {

        ShortArrayProducer gp = new ShortArrayProducer();

        public JNIShortWorker(int size) {
            super(size);
        }

        void doit(int size) {

            short[] array = gp.create(size);
            short min = Short.MAX_VALUE, max = Short.MIN_VALUE;
            for (int i = 0; i < array.length; ++i) {
                if (array[i] > max) {
                    max = array[i];
                }

                if (array[i] < min) {
                    min = array[i];
                }
            }
            String str = "Min: " + min + " Max: " + max;
            int nativeHash = NativeCall(array, str);
            int javaHash = 0;
            for (int i = 0; i < str.length(); ++i) {
                javaHash += (int) str.charAt(i);
            }
            javaHash += javaHash;
            check(array[0] == min && array[array.length - 1] == max
                    && javaHash == nativeHash, "Data validation failure");
        }
    }

    // INT
    class JNIIntWorker extends JNIAbstractWorker {

        IntArrayProducer gp = new IntArrayProducer();

        public JNIIntWorker(int size) {
            super(size);
        }

        void doit(int size) {
            int[] array = gp.create(size);
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
            for (int i = 0; i < array.length; ++i) {
                if (array[i] > max) {
                    max = array[i];
                }

                if (array[i] < min) {
                    min = array[i];
                }
            }
            String str = "Min: " + min + " Max: " + max;
            int nativeHash = NativeCall(array, str);
            int javaHash = 0;
            for (int i = 0; i < str.length(); ++i) {
                javaHash += (int) str.charAt(i);
            }
            javaHash += javaHash;
            check(array[0] == min && array[array.length - 1] == max
                    && javaHash == nativeHash, "Data validation failure");

        }
    }

    // LONG
    class JNILongWorker extends JNIAbstractWorker {

        LongArrayProducer gp = new LongArrayProducer();

        public JNILongWorker(int size) {
            super(size);
        }

        void doit(int size) {

            long[] array = gp.create(size);
            long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
            for (int i = 0; i < array.length; ++i) {
                if (array[i] > max) {
                    max = array[i];
                }

                if (array[i] < min) {
                    min = array[i];
                }
            }
            String str = "Min: " + min + " Max: " + max;
            int nativeHash = NativeCall(array, str);
            int javaHash = 0;
            for (int i = 0; i < str.length(); ++i) {
                javaHash += (int) str.charAt(i);
            }
            javaHash += javaHash;
            check(array[0] == min && array[array.length - 1] == max
                    && javaHash == nativeHash, "Data validation failure");

        }
    }

    // FLOAT
    class JNIFloatWorker extends JNIAbstractWorker {

        FloatArrayProducer gp = new FloatArrayProducer();

        public JNIFloatWorker(int size) {
            super(size);
        }

        void doit(int size) {

            float[] array = gp.create(size);
            float min = Float.MAX_VALUE, max = Float.MIN_VALUE;
            for (int i = 0; i < array.length; ++i) {
                if (array[i] > max) {
                    max = array[i];
                }

                if (array[i] < min) {
                    min = array[i];
                }
            }
            String str = "Min: " + min + " Max: " + max;
            int nativeHash = NativeCall(array, str);
            int javaHash = 0;
            for (int i = 0; i < str.length(); ++i) {
                javaHash += (int) str.charAt(i);
            }
            javaHash += javaHash;
            check(array[0] == min && array[array.length - 1] == max
                    && javaHash == nativeHash, "Data validation failure");
        }
    }

    // DOUBLE
    class JNIDoubleWorker extends JNIAbstractWorker {

        DoubleArrayProducer gp = new DoubleArrayProducer();

        public JNIDoubleWorker(int size) {
            super(size);
        }

        void doit(int size) {

            double[] array = gp.create(size);
            double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
            for (int i = 0; i < array.length; ++i) {
                if (array[i] > max) {
                    max = array[i];
                }

                if (array[i] < min) {
                    min = array[i];
                }
            }
            String str = "Min: " + min + " Max: " + max;
            int nativeHash = NativeCall(array, str);
            int javaHash = 0;
            for (int i = 0; i < str.length(); ++i) {
                javaHash += (int) str.charAt(i);
            }
            javaHash += javaHash;
            check(array[0] == min && array[array.length - 1] == max
                    && javaHash == nativeHash, "Data validation failure");
        }
    }
}

class JNIWorker {

    protected native int NativeCall(boolean[] array, String str);

    protected native int NativeCall(byte[] array, String str);

    protected native int NativeCall(char[] array, String str);

    protected native int NativeCall(short[] array, String str);

    protected native int NativeCall(int[] array, String str);

    protected native int NativeCall(long[] array, String str);

    protected native int NativeCall(float[] array, String str);

    protected native int NativeCall(double[] array, String str);
}
