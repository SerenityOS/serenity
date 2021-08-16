/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 8040076
 * @summary AwtList not garbage collected
 * @run main/othervm -Xmx100m AwtListGarbageCollectionTest
 */

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.ref.WeakReference;

public class AwtListGarbageCollectionTest {
    public static void main(String[] args) {
        Frame frame = new Frame("List leak test");
        try {
            test(frame);
        } finally {
            frame.dispose();
        }
    }

    private static void test(Frame frame) {
        WeakReference<List> weakListRef = null;
        try {
            frame.setSize(300, 200);
            frame.setVisible(true);

            List strongListRef = new List();
            frame.add(strongListRef);
            strongListRef.setMultipleMode(true);
            frame.remove(strongListRef);
            weakListRef = new WeakReference<List>(strongListRef);
            strongListRef = null;

            //make out of memory to force gc
            String veryLongString = new String(new char[100]);
            while (true) {
                veryLongString += veryLongString;
            }
        } catch (OutOfMemoryError e) {
            if (weakListRef == null) {
                throw new RuntimeException("Weak list ref wasn't created");
            } else if (weakListRef.get() != null) {
                throw new RuntimeException("List wasn't garbage collected");
            }
        }
    }
}
