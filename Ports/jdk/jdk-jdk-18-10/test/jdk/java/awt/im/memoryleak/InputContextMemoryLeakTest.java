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

import java.awt.FlowLayout;
import java.awt.Robot;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import test.java.awt.regtesthelpers.Util;

/*
 @test
 @key headful
 @bug 7079260
 @summary XInputContext leaks memory by needRecetXXIClient field
 @author Petr Pchelko
 @library ../../regtesthelpers
 @build Util
 @compile InputContextMemoryLeakTest.java
 @run main/othervm -Xmx20M InputContextMemoryLeakTest
 */
public class InputContextMemoryLeakTest {

    private static JFrame frame;
    private static WeakReference<JTextField> text;
    private static WeakReference<JPanel> p;
    private static JButton button;

    public static void init() throws Throwable {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame = new JFrame();
                frame.setLayout(new FlowLayout());
                JPanel p1 = new JPanel();
                button = new JButton("Test");
                p1.add(button);
                frame.add(p1);
                text = new WeakReference<JTextField>(new JTextField("Text"));
                p = new WeakReference<JPanel>(new JPanel(new FlowLayout()));
                p.get().add(text.get());
                frame.add(p.get());
                frame.setBounds(500, 400, 200, 200);
                frame.setVisible(true);
            }
        });

        Util.focusComponent(text.get(), 500);
        Util.clickOnComp(button, new Robot());
        //References to objects testes for memory leak are stored in Util.
        //Need to clean them
        Util.cleanUp();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame.remove(p.get());
            }
        });

        Util.waitForIdle(null);
        //After the next caret blink it automatically TextField references
        Thread.sleep(text.get().getCaret().getBlinkRate() * 2);
        Util.waitForIdle(null);
        assertGC();
    }

      public static void assertGC() throws Throwable {
        List<byte[]> alloc = new ArrayList<byte[]>();
        int size = 1024 * 10;
        while (true) {
            try {
                alloc.add(new byte[size]);
            } catch (OutOfMemoryError err) {
                break;
            }
        }
        alloc = null;
        if (text.get() != null) {
            throw new Exception("Test failed: JTextField was not collected");
        }
    }

    public static void main(String args[]) throws Throwable {
        init();
    }
}
