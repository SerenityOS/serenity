/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
   @test
   @key headful
   @bug 6919629
   @summary Tests that components with Nimbus.Overrides are GC'ed properly
   @author Peter Zhelezniakov
   @run main Test6919629
*/

import java.awt.Color;
import java.lang.ref.WeakReference;
import javax.swing.*;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

public class Test6919629
{
    JFrame f;
    WeakReference<JLabel> ref;

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(new NimbusLookAndFeel());
        Test6919629 t = new Test6919629();
        t.test();
        System.gc();
        t.check();
    }

    void test() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                UIDefaults d = new UIDefaults();
                d.put("Label.textForeground", Color.MAGENTA);

                JLabel l = new JLabel();
                ref = new WeakReference<JLabel>(l);
                l.putClientProperty("Nimbus.Overrides", d);

                f = new JFrame();
                f.getContentPane().add(l);
                f.pack();
                f.setVisible(true);
            }
        });
        Thread.sleep(2000);

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                f.getContentPane().removeAll();
                f.setVisible(false);
                f.dispose();
            }
        });
        Thread.sleep(2000);
    }

    void check() {
        if (ref.get() != null) {
            throw new RuntimeException("Failed: an unused component wasn't collected");
        }
    }
}
