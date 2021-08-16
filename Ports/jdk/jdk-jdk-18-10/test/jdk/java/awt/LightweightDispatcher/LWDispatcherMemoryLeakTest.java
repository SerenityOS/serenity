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

import java.awt.AWTException;
import java.awt.FlowLayout;
import java.awt.Robot;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import test.java.awt.regtesthelpers.Util;

/*
 @test
 @key headful
 @bug 7079254 8163261
 @summary Toolkit eventListener leaks memory
 @library ../regtesthelpers
 @build Util
 @compile LWDispatcherMemoryLeakTest.java
 @run main/othervm -Xmx10M LWDispatcherMemoryLeakTest
 */
public class LWDispatcherMemoryLeakTest {

    private static JFrame frame;
    private static WeakReference<JButton> button;
    private static WeakReference<JPanel> p;

    public static void init() throws Throwable {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame = new JFrame();
                frame.setLayout(new FlowLayout());
                button = new WeakReference<JButton>(new JButton("Text"));
                p = new WeakReference<JPanel>(new JPanel(new FlowLayout()));
                p.get().add(button.get());
                frame.add(p.get());

                frame.setBounds(500, 400, 200, 200);
                frame.setVisible(true);
            }
        });

        Util.waitTillShown(button.get());
        Util.clickOnComp(button.get(), new Robot());

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame.remove(p.get());
            }
        });

        Util.waitForIdle(null);
        assertGC();
    }

    public static void assertGC() throws Throwable {
        List<byte[]> alloc = new ArrayList<byte[]>();
        int size = 10 * 1024;
        while (true) {
            try {
                alloc.add(new byte[size]);
            } catch (OutOfMemoryError err) {
                break;
            }
        }
        alloc = null;
        String leakObjs = "";
        if (button.get() != null) {
            leakObjs = "JButton";
        }
        if (p.get() != null) {
            leakObjs += " JPanel";
        }
        if (leakObjs != "") {
            throw new Exception("Test failed: " + leakObjs + " not collected");
        }
    }

    public static void main(String args[]) throws Throwable {
        init();
    }
}
