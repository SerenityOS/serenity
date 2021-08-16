/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 8013563 8028486
 * @summary Tests that windows are removed from windows list
 * @library /javax/swing/regtesthelpers
 * @modules java.desktop/sun.awt
 *          java.desktop/sun.java2d
 * @build Util
 * @run main/othervm -Xms32M -Xmx32M WindowsLeak
*/

import java.awt.Frame;
import java.awt.Robot;
import java.awt.Window;
import java.lang.ref.WeakReference;
import java.util.Vector;

import sun.awt.AppContext;
import sun.java2d.Disposer;

public class WindowsLeak {

    private static volatile boolean disposerPhantomComplete;

    public static void main(String[] args) throws Exception {
        Robot r = new Robot();
        for (int i = 0; i < 100; i++) {
            Frame f = new Frame();
            f.pack();
            f.dispose();
        }
        r.waitForIdle();

        Disposer.addRecord(new Object(), () -> disposerPhantomComplete = true);

        while (!disposerPhantomComplete) {
            Util.generateOOME();
        }

        Vector<WeakReference<Window>> windowList =
                        (Vector<WeakReference<Window>>) AppContext.getAppContext().get(Window.class);

        if (windowList != null && !windowList.isEmpty()) {
            throw new RuntimeException("Test FAILED: Window list is not empty: " + windowList.size());
        }

        System.out.println("Test PASSED");
    }
}
