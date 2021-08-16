/*
 * Copyright (c) 1997, 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4076287
 * @summary Invoking get on a SoftReference shouldn't pin the referent
 * @run main/othervm -ms16m -mx16m Pin
 * @author Peter Jones
 * @author Mark Reinhold
 */


import java.lang.ref.SoftReference;

public class Pin {

    static final int NUM_BLOCKS = 128;
    static final int BLOCK_SIZE = 32768;

    public static void main(String[] args) throws Exception {

        SoftReference[] blocks = new SoftReference[NUM_BLOCKS];

        byte[] block;

        System.err.println("Filling array with " + NUM_BLOCKS +
                           " SoftReferences to blocks of " +
                           BLOCK_SIZE + " bytes.");

        for (int i = 0; i < NUM_BLOCKS; ++ i) {
            block = new byte[BLOCK_SIZE];
            SoftReference ref = new SoftReference(block);
            blocks[i] = ref;
        }
        block = null;

        System.err.println("Allowing SoftReferences to be enqueued.");
        System.gc();
        Thread.sleep(1000);

        /* -- Commenting out the following section will hide the bug -- */
        System.err.println("Invoking get() on SoftReferences.");
        for (int i = 0; i < NUM_BLOCKS; ++ i) {
            block = (byte[]) blocks[i].get();
        }
        block = null;
        /* -- end -- */

        System.err.println("Forcing desperate garbage collection...");
        java.util.Vector chain = new java.util.Vector();
        try {
            while (true) {
                System.gc();
                int[] hungry = new int[65536];
                chain.addElement(hungry);
                Thread.sleep(100);              // yield, for what it's worth
            }
        } catch (OutOfMemoryError e) {
            chain = null; // Free memory for further work.
            System.err.println("Got OutOfMemoryError, as expected.");
        }

        int emptyCount = 0, fullCount = 0;
        System.err.print("Examining contents of array:");
        for (int i = 0; i < NUM_BLOCKS; ++ i) {
            block = (byte[]) blocks[i].get();
            if (block == null) {
                emptyCount++;
            } else {
                fullCount++;
            }
        }
        System.err.println(" " + emptyCount + " empty, " +
                           fullCount + " full.");
        if (emptyCount == 0)
            throw new Exception("No SoftReference instances were cleared");
    }

}
