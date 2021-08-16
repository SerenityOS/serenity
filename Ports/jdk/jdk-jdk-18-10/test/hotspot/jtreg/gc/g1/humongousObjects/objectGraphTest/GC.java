/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package gc.g1.humongousObjects.objectGraphTest;

import gc.testlibrary.Helpers;
import jdk.test.lib.Asserts;
import sun.hotspot.WhiteBox;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Provides methods to initiate GC of requested type and
 * checks for states of humongous and non-humongous soft/weak externally
 * referenced objects after GCs
 */
public enum GC {
    CMC {
        @Override
        public Runnable get() {
            return () -> {
                Helpers.waitTillCMCFinished(WHITE_BOX, 0);
                WHITE_BOX.g1StartConcMarkCycle();
                Helpers.waitTillCMCFinished(WHITE_BOX, 0);
            };
        }

        public Consumer<ReferenceInfo<Object[]>> getChecker() {
            return getCheckerImpl(false, false, true, false);
        }

        @Override
        public List<String> shouldContain() {
            return Arrays.asList(GCTokens.WB_INITIATED_CMC);
        }

        @Override
        public List<String> shouldNotContain() {
            return Arrays.asList(GCTokens.WB_INITIATED_YOUNG_GC, GCTokens.WB_INITIATED_MIXED_GC,
                    GCTokens.FULL_GC, GCTokens.YOUNG_GC);
        }
    },

    CMC_NO_SURV_ROOTS {
        @Override
        public Runnable get() {
            return () -> {
                WHITE_BOX.youngGC();
                Helpers.waitTillCMCFinished(WHITE_BOX, 0);
                WHITE_BOX.youngGC();
                Helpers.waitTillCMCFinished(WHITE_BOX, 0);

                WHITE_BOX.g1StartConcMarkCycle();
                Helpers.waitTillCMCFinished(WHITE_BOX, 0);
            };
        }

        public Consumer<ReferenceInfo<Object[]>> getChecker() {
            return getCheckerImpl(true, false, true, false);
        }

        @Override
        public List<String> shouldContain() {
            return Arrays.asList(GCTokens.WB_INITIATED_CMC);
        }

        @Override
        public List<String> shouldNotContain() {
            return Arrays.asList(GCTokens.WB_INITIATED_MIXED_GC,
                    GCTokens.FULL_GC, GCTokens.YOUNG_GC);
        }
    },

    YOUNG_GC {
        @Override
        public Runnable get() {
            return WHITE_BOX::youngGC;
        }

        public Consumer<ReferenceInfo<Object[]>> getChecker() {
            return getCheckerImpl(false, false, true, false);
        }

        @Override
        public List<String> shouldContain() {
            return Arrays.asList(GCTokens.WB_INITIATED_YOUNG_GC);
        }

        @Override
        public List<String> shouldNotContain() {
            return Arrays.asList(GCTokens.WB_INITIATED_MIXED_GC, GCTokens.FULL_GC, GCTokens.WB_INITIATED_CMC,
                    GCTokens.CMC, GCTokens.YOUNG_GC);
        }
    },

    FULL_GC {
        @Override
        public Runnable get() {
            return System::gc;
        }

        public Consumer<ReferenceInfo<Object[]>> getChecker() {
            return getCheckerImpl(true, false, true, false);
        }

        @Override
        public List<String> shouldContain() {
            return Arrays.asList(GCTokens.FULL_GC);
        }

        @Override
        public List<String> shouldNotContain() {
            return Arrays.asList(GCTokens.WB_INITIATED_YOUNG_GC, GCTokens.WB_INITIATED_MIXED_GC,
                    GCTokens.WB_INITIATED_CMC, GCTokens.CMC, GCTokens.YOUNG_GC);
        }
    },

    MIXED_GC {
        @Override
        public Runnable get() {
            return () -> {
                WHITE_BOX.youngGC();
                Helpers.waitTillCMCFinished(WHITE_BOX, 0);
                WHITE_BOX.youngGC();
                Helpers.waitTillCMCFinished(WHITE_BOX, 0);

                WHITE_BOX.g1StartConcMarkCycle();
                Helpers.waitTillCMCFinished(WHITE_BOX, 0);

                WHITE_BOX.youngGC();
                Helpers.waitTillCMCFinished(WHITE_BOX, 0);
                // Provoking Mixed GC
                WHITE_BOX.youngGC();// second evacuation pause will be mixed
                Helpers.waitTillCMCFinished(WHITE_BOX, 0);
            };
        }

        public Consumer<ReferenceInfo<Object[]>> getChecker() {
            return getCheckerImpl(true, false, true, false);
        }

        @Override
        public List<String> shouldContain() {
            return Arrays.asList(GCTokens.WB_INITIATED_CMC);
        }

        @Override
        public List<String> shouldNotContain() {
            return Arrays.asList(GCTokens.YOUNG_GC);
        }
    },

    FULL_GC_MEMORY_PRESSURE {
        @Override
        public Runnable get() {
            return WHITE_BOX::fullGC;
        }

        public Consumer<ReferenceInfo<Object[]>> getChecker() {
            return getCheckerImpl(true, true, true, true);
        }

        @Override
        public List<String> shouldContain() {
            return Arrays.asList(GCTokens.FULL_GC_MEMORY_PRESSURE);
        }

        @Override
        public List<String> shouldNotContain() {
            return Arrays.asList(GCTokens.WB_INITIATED_YOUNG_GC, GCTokens.WB_INITIATED_MIXED_GC,
                    GCTokens.WB_INITIATED_CMC, GCTokens.CMC, GCTokens.YOUNG_GC, GCTokens.FULL_GC);
        }
    };

    protected String getErrorMessage(ReferenceInfo<Object[]> ref, boolean expectedNull, String gcType) {
        return String.format("Externally effectively %s referenced %shumongous object was%s deleted after %s",
                (ref.softlyReachable ? "soft" : "weak"), (ref.effectiveHumongous ? "" : "non-"),
                (expectedNull ? " not" : ""), gcType);
    }

    protected Consumer<ReferenceInfo<Object[]>> getCaseCheck(boolean expectedNull) {
        return expectedNull
                ? r -> Asserts.assertNull(r.reference.get(), getErrorMessage(r, true, name()))
                : r -> Asserts.assertNotNull(r.reference.get(), getErrorMessage(r, false, name()));
    }

    protected Consumer<ReferenceInfo<Object[]>> getCheckerImpl(boolean weakH, boolean softH,
                                                               boolean weakS, boolean softS) {
        return new Checker(getCaseCheck(weakH), getCaseCheck(softH), getCaseCheck(weakS), getCaseCheck(softS));
    }

    protected String getGcLogName(String prefix) {
        return prefix + "_" + name() + ".gc.log";
    }

    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    /**
     * @return method to initiate GC
     */
    public abstract Runnable get();

    /**
     * @return checker for objects' states after GC
     */
    public abstract Consumer<ReferenceInfo<Object[]>> getChecker();

    /**
     * @return list of tokens that should be contained in gc log after gc of specified type
     */
    public abstract List<String> shouldContain();

    /**
     * @return list of tokens that should not be contained in gc log after gc of specified type
     */
    public abstract List<String> shouldNotContain();


    /**
     * Checks object' state after gc
     * Contains 4 Consumers which are called depending on humongous/non-humongous and
     * external weak/soft referenced objects
     */
    private static class Checker implements Consumer<ReferenceInfo<Object[]>> {
        // 4 consumers with checks for (humongous /simple objects)*(weak/soft referenced)
        final Consumer<ReferenceInfo<Object[]>> weakHumongousCheck;
        final Consumer<ReferenceInfo<Object[]>> softHumongousCheck;
        final Consumer<ReferenceInfo<Object[]>> weakSimpleCheck;
        final Consumer<ReferenceInfo<Object[]>> softSimpleCheck;

        public Checker(Consumer<ReferenceInfo<Object[]>> weakHumongousCheck,
                       Consumer<ReferenceInfo<Object[]>> softHumongousCheck,
                       Consumer<ReferenceInfo<Object[]>> weakSimpleCheck,
                       Consumer<ReferenceInfo<Object[]>> softSimpleCheck) {
            this.weakHumongousCheck = weakHumongousCheck;
            this.softHumongousCheck = softHumongousCheck;
            this.weakSimpleCheck = weakSimpleCheck;
            this.softSimpleCheck = softSimpleCheck;
        }

        public void accept(ReferenceInfo<Object[]> ref) {

            System.out.println("reference.get() returned " + ref.reference.get());
            if (ref.effectiveHumongous && ref.softlyReachable) {
                System.out.println("soft and humongous");
                softHumongousCheck.accept(ref);
            }

            if (ref.effectiveHumongous && !ref.softlyReachable) {
                System.out.println("weak and humongous");
                weakHumongousCheck.accept(ref);

            }

            if (!ref.effectiveHumongous && ref.softlyReachable) {
                System.out.println("soft and non-humongous");
                softSimpleCheck.accept(ref);
            }

            if (!ref.effectiveHumongous && !ref.softlyReachable) {
                System.out.println("weak and non-humongous");
                weakSimpleCheck.accept(ref);
            }
        }
    }

}
