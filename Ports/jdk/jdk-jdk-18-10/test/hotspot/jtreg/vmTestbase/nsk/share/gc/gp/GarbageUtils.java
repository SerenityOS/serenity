/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.gc.gp;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.*;
import java.util.*;
import nsk.share.gc.gp.array.*;
import nsk.share.gc.gp.string.*;
import nsk.share.gc.gp.list.*;
import nsk.share.gc.gp.tree.*;
import nsk.share.gc.gp.misc.*;
import nsk.share.gc.gp.classload.*;
import nsk.share.gc.Memory;
import nsk.share.TestBug;
import nsk.share.test.*;

/**
 * Utility methods for garbage producers.
 */
public final class GarbageUtils {
        private static final int ALLOCATION_LIMIT = 50000000; //50 Mb
        private static GarbageProducers garbageProducers;
        private static List<GarbageProducer> primitiveArrayProducers;
        private static List<GarbageProducer> arrayProducers;
        private static final GarbageProducer  byteArrayProducer = new ByteArrayProducer();
        public static enum OOM_TYPE {
            ANY (),
            HEAP("Java heap space"),
            METASPACE("Metaspace", "Compressed class space");

            private final String[] expectedStrings;
            OOM_TYPE(String... expectedStrings) {
                this.expectedStrings = expectedStrings;
            }

            /**
             * Returns true if the given error message matches
             * one of expected strings.
             */
            public boolean accept(String errorMessage) {
                if (expectedStrings == null || expectedStrings.length == 0 || errorMessage == null) {
                    return true;
                }
                for (String s: expectedStrings) {
                    if (errorMessage.indexOf(s) != -1) {
                        return true;
                    }
                }
                return false;
            }
        };

        // Force loading of OOM_TYPE and calling of enum constructors when loading GarbageUtils class.
        public static final Object[] thisIsGarbageArray_theOnlyPurposeForCreatingItAndDeclaringItPublicIsToInitializeIntancesOfOOMEnumberation = new Object[] { OOM_TYPE.ANY, OOM_TYPE.HEAP, OOM_TYPE.METASPACE };

        // Force early loading of classes that might otherwise unexpectedly fail
        // class loading during testing due to high memory pressure.
        public static final StringWriter preloadStringWriter = new StringWriter(1);
        public static final PrintWriter preloadPrintWriter = new PrintWriter(preloadStringWriter);
        public static final Throwable preloadThrowable = new Throwable("preload");

        private GarbageUtils() {
        }

        /**
         * Eat memory using execution controller that waits for 2 minutes.
         * @return number of OOME occured
         */
        public static int eatMemory() {
                return eatMemory(2 * 60 * 1000);
        }

        /**
         * Eat memory using execution controller that waits for timeout.
         * @return number of OOME occured
         */
        public static int eatMemory(final long timeout) {
                return eatMemory(new ExecutionController() {
                        final long initialTime = System.currentTimeMillis();

                                @Override
                                public void start(long stdIterations) {}

                                @Override
                                public boolean iteration() {return false;}

                                @Override
                                public boolean continueExecution() {
                                        return System.currentTimeMillis() - initialTime < timeout;
                                }

                                @Override
                                public long getIteration() {return 0;}

                                @Override
                                public void finish() {}
                });
        }


        /**
         * Eat memory using given execution controller and garbage producer.
         *
         * @param stresser execution controller
         * @param gp garbage producer
         * @return number of OOME occured
         */
        public static int eatMemory(ExecutionController stresser) {
            return eatMemory(stresser, byteArrayProducer, 50, 100, 2, OOM_TYPE.ANY);
        }

        /**
         * Eat memory using given execution controller and garbage producer.
         *
         * @param stresser execution controller
         * @param gp garbage producer
         * @return number of OOME occured
         */
        public static int eatMemory(ExecutionController stresser, GarbageProducer gp) {
            return eatMemory(stresser, gp, 50, 100, 2, OOM_TYPE.ANY);
        }

        /**
         * Eat memory using given garbage producer and given factor.
         *
         * @param gp garbage producer
         * @param factor factor to divide the array size by
         * @return number of OOME occured
         */
        public static int eatMemory(ExecutionController stresser, GarbageProducer gp, long factor) {
            return eatMemory(stresser, gp, 50, 100, factor, OOM_TYPE.ANY);
        }

        /**
         * Eat memory using default(byte[]) garbage producer.
         *
         * Note that this method can throw Failure if any exception
         * is thrown while eating memory. To avoid OOM while allocating
         * exception we preallocate it before the lunch starts. It means
         * that exception stack trace does not correspond to the place
         * where exception is thrown, but points at start of the method.
         *
         * @param stresser stresser
         * @param initialFactor determines which portion of initial memory initial chunk will be
         * @param minMemoryChunk determines when to stop
         * @param factor factor to divide the array size by
         * @return number of OOME occured
         */
        public static int eatMemory(ExecutionController stresser,long initialFactor, long minMemoryChunk, long factor) {
            return eatMemory(stresser, byteArrayProducer, initialFactor, minMemoryChunk, factor, OOM_TYPE.ANY);
        }

        /**
         * Eat memory using given garbage producer.
         *
         * Note that this method can throw Failure if any exception
         * is thrown while eating memory. To avoid OOM while allocating
         * exception we preallocate it before the lunch starts. It means
         * that exception stack trace does not correspond to the place
         * where exception is thrown, but points at start of the method.
         *
         * @param stresser stresser to use
         * @param gp garbage producer
         * @param initialFactor determines which portion of initial memory initial chunk will be
         * @param minMemoryChunk determines when to stop
         * @param factor factor to divide the array size by. A value of 0 means that method returns after first  OOME
         * @param type of OutOfMemory Exception: Java heap space or Metadata space
         * @return number of OOME occured
         */
        public static int eatMemory(ExecutionController stresser, GarbageProducer gp, long initialFactor, long minMemoryChunk, long factor) {
            return eatMemory(stresser, gp, initialFactor, minMemoryChunk, factor, OOM_TYPE.ANY);
        }

         static int numberOfOOMEs = 0;

         /**
          * Minimal wrapper of the main implementation. Catches any OOM
          * that might be thrown when rematerializing Objects when deoptimizing.
          *
          * It is Important that the impl is not inlined.
          */

         public static int eatMemory(ExecutionController stresser, GarbageProducer gp, long initialFactor, long minMemoryChunk, long factor, OOM_TYPE type) {
            try {
               // Using a methodhandle invoke of eatMemoryImpl to prevent inlining of it
               MethodHandles.Lookup lookup = MethodHandles.lookup();
               MethodType mt = MethodType.methodType(
                     int.class,
                     ExecutionController.class,
                     GarbageProducer.class,
                     long.class,
                     long.class,
                     long.class,
                     OOM_TYPE.class);
               MethodHandle eat = lookup.findStatic(GarbageUtils.class, "eatMemoryImpl", mt);
               return (int) eat.invoke(stresser, gp, initialFactor, minMemoryChunk, factor, type);
            } catch (OutOfMemoryError e) {
               return numberOfOOMEs++;
            } catch (Throwable t) {
               throw new RuntimeException(t);
            }
         }

        /**
         * Eat memory using given garbage producer.
         *
         * Note that this method can throw Failure if any exception
         * is thrown while eating memory. To avoid OOM while allocating
         * exception we preallocate it before the lunch starts. It means
         * that exception stack trace does not correspond to the place
         * where exception is thrown, but points at start of the method.
         *
         * @param stresser stresser to use
         * @param gp garbage producer
         * @param initialFactor determines which portion of initial memory initial chunk will be
         * @param minMemoryChunk determines when to stop
         * @param factor factor to divide the array size by. A value of 0 means that method returns after first  OOME
         * @param type of OutOfMemory Exception: Java heap space or Metadata space
         * @return number of OOME occured
         */

         public static int eatMemoryImpl(ExecutionController stresser, GarbageProducer gp, long initialFactor, long minMemoryChunk, long factor, OOM_TYPE type) {
                numberOfOOMEs = 0;
                try {
                        byte[] someMemory = new byte[200000]; //200 Kb
                        try {
                                Runtime runtime = Runtime.getRuntime();
                                long maxMemory = runtime.maxMemory();
                                long maxMemoryChunk = maxMemory / initialFactor;
                                long chunk = maxMemoryChunk;
                                chunk = chunk > ALLOCATION_LIMIT ? ALLOCATION_LIMIT : chunk;
                                int allocations = 0;
                                List<Object> storage = new ArrayList<Object>();

                                while (chunk > minMemoryChunk && stresser.continueExecution()) {
                                        try {
                                                storage.add(gp.create(chunk));
                                                if (Thread.currentThread().isInterrupted()) {
                                                        return numberOfOOMEs;
                                                }
                                                // if we are able to eat chunk*factor let
                                                // try to increase size of chunk
                                                if (chunk * factor < maxMemoryChunk
                                                        && factor != 0 && allocations++ == factor + 1) {
                                                    chunk = chunk * factor;
                                                    allocations = 0;
                                                }
                                        } catch (OutOfMemoryError e) {
                                            someMemory = null;
                                            if (type != OOM_TYPE.ANY) {
                                                if (type.accept(e.toString())) {
                                                    numberOfOOMEs++;
                                                } else {
                                                    // Trying to catch situation when Java generates OOM different type that test trying to catch
                                                    throw new TestBug("Test throw OOM of unexpected type." + e.toString());
                                                }
                                            } else {
                                               numberOfOOMEs++;
                                            }
                                            allocations = 0;
                                            if (factor == 0) {
                                                return numberOfOOMEs;
                                            } else {
                                                chunk = chunk / factor;
                                            }
                                        }
                                }
                        } catch (OutOfMemoryError e) {
                            someMemory = null;
                            if (type != OOM_TYPE.ANY) {
                                if (type.accept(e.toString())) {
                                    numberOfOOMEs++;
                                } else {
                                    // Trying to catch situation when Java generates OOM different type that test trying to catch
                                    throw new TestBug("Test throw OOM of unexpected type." + e.toString());
                                }
                            } else {
                                numberOfOOMEs++;
                            }
                         // all memory is eaten now even before we start, just return
                        }
                } catch (OutOfMemoryError e) {
                        numberOfOOMEs++;
                }
                return numberOfOOMEs;
        }

        /**
         * Get all primitive array producers.
         */
        public static List<GarbageProducer> getPrimitiveArrayProducers() {
                return getGarbageProducers().getPrimitiveArrayProducers();
        }

        /**
         * Get all array producers.
         */
        public static List<GarbageProducer> getArrayProducers() {
                return getGarbageProducers().getArrayProducers();
        }

        /**
         * Determine size of each object in array which will occupy given
         * memory with distribution determined by given memory strategy.
         */
        public static long getArraySize(long memory, MemoryStrategy memoryStrategy) {
                return memoryStrategy.getSize(memory - Memory.getArrayExtraSize(), Memory.getReferenceSize());
        }

        /**
         * Determine object count in array which will occupy given
         * memory with distribution determined by given memory strategy.
         */
        public static int getArrayCount(long memory, MemoryStrategy memoryStrategy) {
                return memoryStrategy.getCount(memory - Memory.getArrayExtraSize(), Memory.getReferenceSize());
        }

        /**
         * Get garbage producer by identifier.
         *
         * @param id identifier
         * @return garbage producer for this identifier
         */
        public static GarbageProducer getGarbageProducer(String id) {
                if (id == null || id.equals("byteArr"))
                        return new ByteArrayProducer();
                else if (id.equals("booleanArr"))
                        return new BooleanArrayProducer();
                else if (id.equals("shortArr"))
                        return new ShortArrayProducer();
                else if (id.equals("charArr"))
                        return new CharArrayProducer();
                else if (id.equals("intArr"))
                        return new IntArrayProducer();
                else if (id.equals("longArr"))
                        return new LongArrayProducer();
                else if (id.equals("floatArr"))
                        return new FloatArrayProducer();
                else if (id.equals("doubleArr"))
                        return new DoubleArrayProducer();
                else if (id.equals("objectArr"))
                        return new ObjectArrayProducer();
                else if (id.equals("randomString"))
                        return new RandomStringProducer();
                else if (id.equals("simpleString"))
                        return new SimpleStringProducer();
                else if (id.startsWith("interned("))
                        return new InternedStringProducer(getGarbageProducer(getInBrackets(id)));
                else if (id.startsWith("linearList("))
                        return new LinearListProducer(MemoryStrategy.fromString(getInBrackets(id)));
                else if (id.startsWith("circularList("))
                        return new CircularListProducer(MemoryStrategy.fromString(getInBrackets(id)));
                else if (id.startsWith("nonbranchyTree("))
                        return new NonbranchyTreeProducer(MemoryStrategy.fromString(getInBrackets(id)));
                else if (id.equals("class"))
                        return new GeneratedClassProducer();
                else if (id.startsWith("hashed("))
                        return new HashedGarbageProducer(getGarbageProducer(getInBrackets(id)));
                else if (id.startsWith("random("))
                        return new RandomProducer(getGarbageProducerList(getInBrackets(id)));
                else if (id.startsWith("twofields("))
                        return new TwoFieldsObjectProducer(getGarbageProducer(getInBrackets(id)));
                else if (id.startsWith("arrayof("))
                        return new ArrayOfProducer(getGarbageProducer(getInBrackets(id)));
                else if (id.startsWith("trace("))
                        return new TraceProducer(getGarbageProducer(getInBrackets(id)));
                else
                        throw new TestBug("Invalid garbage producer identifier: " + id);
        }

        private static String getInBrackets(String s) {
                int n1 = s.indexOf('(');
                if (n1 == -1)
                        throw new TestBug("Opening bracket not found: " + s);
                int n2 = s.lastIndexOf(')');
                if (n2 == -1)
                        throw new TestBug("Closing bracket not found: " + s);
                return s.substring(n1 + 1, n2);
        }

        private static List<GarbageProducer> getGarbageProducerList(String s) {
                if (s.equals("primitiveArrays"))
                        return getPrimitiveArrayProducers();
                else if (s.equals("arrays"))
                        return getArrayProducers();
                else {
                        String[] ids = s.split(",");
                        List<GarbageProducer> garbageProducers = new ArrayList<GarbageProducer>(ids.length);
                        for (int i = 0; i < ids.length; ++i)
                                garbageProducers.add(getGarbageProducer(ids[i]));
                        return garbageProducers;
                        //throw new TestBug("Invalid id for list of garbage producers: " + id);
                }
        }

        public static GarbageProducers getGarbageProducers() {
                if (garbageProducers == null)
                        garbageProducers = new GarbageProducers();
                return garbageProducers;
        }
}
