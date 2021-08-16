/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

package gc.g1.humongousObjects;

import gc.testlibrary.Helpers;
import jdk.test.lib.Asserts;
import sun.hotspot.WhiteBox;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

/**
 * @test TestObjectCollected
 * @summary checks that after different type of GCs weak/soft references to humongous object behave correspondingly to
 * actual object behavior
 * @requires vm.gc.G1
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 * @modules java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *                   -XX:+WhiteBoxAPI -Xbootclasspath/a:. -Xms200m -Xmx200m -Xlog:gc
 *                   -XX:InitiatingHeapOccupancyPercent=100 -XX:G1HeapRegionSize=1M -Xlog:gc=info:file=TestObjectCollected.gc.log
 *                    gc.g1.humongousObjects.TestObjectCollected
 */


/**
 * Test checks that after different type of GCs weak/soft references to humongous object behave correspondingly to
 * actual object behavior.
 * So if object was collected, reference.get() should return null and vice versa
 * Since we check humongous objects after such an object is collected the region where it was allocated becomes free
 * or/and change type to non-humongous. Two WhiteBox method were used - first returns if a region containing certain
 * address is free and second - if a region containing certain address is humongous
 */

public class TestObjectCollected {
    /**
     * Provides methods to initiate GC of requested type
     */
    private enum GC {
        YOUNG_CG {
            @Override
            public void provoke() {
                WHITE_BOX.youngGC();
            }
        },
        FULL_GC {
            @Override
            public void provoke() {
                System.gc();
            }
        },
        CMC {
            @Override
            public void provoke() {
                Helpers.waitTillCMCFinished(WHITE_BOX, 0);
                WHITE_BOX.g1StartConcMarkCycle();
                Helpers.waitTillCMCFinished(WHITE_BOX, 0);
            }
        },
        FULL_GC_MEMORY_PRESSURE {
            @Override
            public void provoke() {
                WHITE_BOX.fullGC();
            }
        };
        private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

        public abstract void provoke();
    }

    /**
     * Factory for weak and soft references.
     * Allocates byte array of ALLOCATION_SIZE and returns weak/soft reference on it.
     */
    private enum REF_FACTORY {
        WEAK {
            @Override
            public Reference<byte[]> create() {
                return new WeakReference<>(new byte[ALLOCATION_SIZE], referenceQueqe);
            }
        },
        SOFT {
            @Override
            public Reference<byte[]> create() {
                return new SoftReference<>(new byte[ALLOCATION_SIZE], referenceQueqe);
            }
        };

        private static final ReferenceQueue<byte[]> referenceQueqe = new ReferenceQueue<>();
        private static final int ALLOCATION_SIZE = WhiteBox.getWhiteBox().g1RegionSize() * 2 / 3;

        /**
         * Factory method
         *
         * @return weak/soft reference on byte array of ALLOCATION_SIZE
         */
        public abstract Reference<byte[]> create();
    }


    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    /**
     * Does actual testing:
     * Gets a reference
     * Gets address of referenced object using WB method
     * Calls gc of provided type
     * Checks that object was/was not deleted using WB methods.
     */
    public static void doTesting(GC gc, REF_FACTORY ref) {

        System.out.println(String.format("Testing %s reference behavior after %s", ref.name(), gc.name()));

        Reference<byte[]> reference = ref.create();
        Asserts.assertNotNull(reference, "Test Bug: failed to allocate reference");
        long adr = WHITE_BOX.getObjectAddress(reference.get());

        //Sanity checks
        boolean isRefNulled = (reference.get() == null);
        boolean isRegionHumongous = WHITE_BOX.g1BelongsToHumongousRegion(adr);
        boolean isRegionFree = WHITE_BOX.g1BelongsToFreeRegion(adr);


        Asserts.assertEquals(isRefNulled, false,
                "We just allocated an object but reference.get() already returned null");

        Asserts.assertEquals(isRegionFree, false,
                "We just allocated an object but WB returns that allocation region is still considered free");

        Asserts.assertEquals(isRegionHumongous, true,
                "We just allocated a humongous object but WB returns that allocation region is not humongous");

        gc.provoke();

        isRefNulled = (reference.get() == null);
        isRegionHumongous = WHITE_BOX.g1BelongsToHumongousRegion(adr);
        isRegionFree = WHITE_BOX.g1BelongsToFreeRegion(adr);

        boolean isObjCollected = isRegionFree || !isRegionHumongous;

        Asserts.assertEquals(isRefNulled, isObjCollected,
                String.format("There is an inconsistensy between reference and white box "
                                + "method behavior - one considers object referenced with "
                                + "%s type collected and another doesn't!\n"
                                + "\treference.get() returned %snull\n"
                                + "\tWhiteBox methods returned that object was%s collected",
                        reference.getClass().getSimpleName(),
                        (isRefNulled ? "" : "not "),
                        (isObjCollected ? "" : " not")));

        System.out.println("Passed");
    }

    /**
     * Entry point
     *
     * @param args not used
     */
    public static void main(String[] args) {
        // Full gc - System.gc()
        TestObjectCollected.doTesting(GC.FULL_GC, REF_FACTORY.WEAK);
        TestObjectCollected.doTesting(GC.FULL_GC, REF_FACTORY.SOFT);

        // Full gc with memory pressure - WB.fullGC() emulates that no memory left
        TestObjectCollected.doTesting(GC.FULL_GC_MEMORY_PRESSURE, REF_FACTORY.WEAK);
        TestObjectCollected.doTesting(GC.FULL_GC_MEMORY_PRESSURE, REF_FACTORY.SOFT);

        // Young gc
        TestObjectCollected.doTesting(GC.YOUNG_CG, REF_FACTORY.WEAK);
        TestObjectCollected.doTesting(GC.YOUNG_CG, REF_FACTORY.SOFT);

        // Concurrent mark cycle
        TestObjectCollected.doTesting(GC.CMC, REF_FACTORY.WEAK);
        TestObjectCollected.doTesting(GC.CMC, REF_FACTORY.SOFT);
    }
}
