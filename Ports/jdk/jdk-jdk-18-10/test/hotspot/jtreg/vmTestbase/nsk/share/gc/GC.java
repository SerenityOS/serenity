/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.gc;

import nsk.share.test.*;
import nsk.share.runner.*;
import nsk.share.gc.gp.GarbageProducer;
import nsk.share.gc.gp.GarbageProducerAware;
import nsk.share.gc.gp.GarbageProducer1Aware;
import nsk.share.gc.gp.MemoryStrategy;
import nsk.share.gc.gp.MemoryStrategyAware;
import nsk.share.gc.gp.GarbageUtils;
import nsk.share.gc.lock.Lockers;
import nsk.share.gc.lock.LockersAware;
import nsk.share.gc.lock.LockerUtils;

/**
 * Utility methods for GC tests.
 */
public class GC extends nsk.share.test.Tests {
        protected static class GCTestRunner extends TestRunner {
                private GCParams gcParams;
                private GarbageProducer garbageProducer;
                private GarbageProducer garbageProducer1;
                private MemoryStrategy memoryStrategy;
                private Lockers lockers;

                public GCTestRunner(Test test, String[] args) {
                        super(test, args);
                }

                private GCParams getGCParams(String[] args) {
                        if (gcParams == null) {
                                gcParams = GCParams.getInstance();
                                gcParams.parseCommandLine(args);
                        }
                        return gcParams;
                }

                private GarbageProducer getGarbageProducer(String[] args) {
                        if (garbageProducer == null) {
                                garbageProducer = GarbageUtils.getGarbageProducer(getGCParams(args).getGarbageProducerId());
                                configure(garbageProducer);
                        }
                        return garbageProducer;
                }

                private GarbageProducer getGarbageProducer1(String[] args) {
                        if (garbageProducer1 == null) {
                                garbageProducer1 = GarbageUtils.getGarbageProducer(getGCParams(args).getGarbageProducer1Id());
                                configure(garbageProducer1);
                        }
                        return garbageProducer1;
                }

                private MemoryStrategy getMemoryStrategy(String[] args) {
                        if (memoryStrategy == null) {
                                memoryStrategy = MemoryStrategy.fromString(getGCParams(args).getMemoryStrategyId());
                                configure(memoryStrategy);
                        }
                        return memoryStrategy;
                }

                private Lockers getLockers(String[] args) {
                        if (lockers == null) {
                                lockers = LockerUtils.getLockers(getGCParams(args).getLockersId());
                                configure(lockers);
                        }
                        return lockers;
                }

                public void configure(Object test) {
                        super.configure(test);
                        if (test instanceof GCParamsAware)
                                ((GCParamsAware) test).setGCParams(getGCParams(args));
                        if (test instanceof GarbageProducerAware)
                                ((GarbageProducerAware) test).setGarbageProducer(getGarbageProducer(args));
                        if (test instanceof GarbageProducer1Aware)
                                ((GarbageProducer1Aware) test).setGarbageProducer1(getGarbageProducer1(args));
                        if (test instanceof MemoryStrategyAware)
                                ((MemoryStrategyAware) test).setMemoryStrategy(getMemoryStrategy(args));
                        if (test instanceof LockersAware)
                                ((LockersAware) test).setLockers(getLockers(args));
                }


        }

        private GC() {
        }

        public static void runTest(Test test, String[] args) {
                new GCTestRunner(test, args).run();
        }
}
