/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug     8024511
 * @summary Verifies that instances of color profiles are destroyed correctly.
 *          A crash during profile destruction indicates failure.
 *
 * @run     main DisposalCrashTest
 */

import static java.awt.color.ColorSpace.*;
import java.awt.color.ICC_Profile;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Vector;

public class DisposalCrashTest {

    static final ReferenceQueue<ICC_Profile> queue = new ReferenceQueue<>();
    static final Vector<Reference<? extends ICC_Profile>> v = new Vector<>();

    public static void main(String[] args) {
        int[] ids = new int[]{
            CS_sRGB, CS_CIEXYZ, CS_GRAY, CS_LINEAR_RGB, CS_PYCC
        };

        for (int id : ids) {
            ICC_Profile p = getCopyOf(id);
        }

        while (!v.isEmpty()) {
            System.gc();
            System.out.println(".");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {};

            final Reference<? extends ICC_Profile> ref = queue.poll();
            System.out.println("Got reference: " + ref);

            v.remove(ref);
        }

        System.out.println("Test PASSED.");
    }

    private static ICC_Profile getCopyOf(int id) {
        ICC_Profile std = ICC_Profile.getInstance(id);

        byte[] data = std.getData();

        ICC_Profile p = ICC_Profile.getInstance(data);

        WeakReference<ICC_Profile> ref = new WeakReference<>(p, queue);

        v.add(ref);

        return p;
    }
}
