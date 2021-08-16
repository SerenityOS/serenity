/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import javax.swing.border.TitledBorder;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 * @test
 * @key headful
 * @bug 8204963
 * @summary Verifies TitledBorder's memory leak
 * @run main TestTitledBorderLeak
 */

public class TestTitledBorderLeak {

    final static int TOTAL_TITLEDBORDER = 10;
    final static int GC_ATTEMPTS = 10;
    static ArrayList<WeakReference<TitledBorder>> weakRefArrTB =
                                         new ArrayList(TOTAL_TITLEDBORDER);

    public static void main(String[] args) throws Exception {

        JFrame frame[] = new JFrame[TOTAL_TITLEDBORDER];

        SwingUtilities.invokeAndWait(() -> {
            for (int i = 0; i < TOTAL_TITLEDBORDER; i++) {
                TitledBorder tb = new TitledBorder("");
                weakRefArrTB.add(i, new WeakReference<TitledBorder>(tb));
                JLabel label = new JLabel("TitledBorder");
                label.setBorder(tb);
                frame[i] = new JFrame("Borders");
                JPanel panel = new JPanel();
                panel.add(label);
                frame[i].setContentPane(panel);
                frame[i].setVisible(true);

            }
        });
        if (TOTAL_TITLEDBORDER != weakRefArrTB.size()) {
            System.err.println("TOTAL_TITLEDBORDER != weakRefArrTB.size()");
        }
        Thread.sleep(3000);
        SwingUtilities.invokeAndWait(() -> {
            for (int i = 0; i < TOTAL_TITLEDBORDER; i++) {
                frame[i].dispose();
                frame[i] = null;
            }
        });
        Thread.sleep(3000);
        attemptGCTitledBorder();
        if (TOTAL_TITLEDBORDER != getCleanedUpTitledBorderCount()) {
            throw new RuntimeException("Expected Total TitledBorder to be freed : " + TOTAL_TITLEDBORDER +
                           " Freed " + getCleanedUpTitledBorderCount());
        }
        System.out.println("OK");
    }

    private static void attemptGCTitledBorder() {
        // Attempt gc GC_ATTEMPTS times
        for (int i = 0; i < GC_ATTEMPTS; i++) {
            System.gc();
            System.runFinalization();
            if (getCleanedUpTitledBorderCount() == TOTAL_TITLEDBORDER) {
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                System.err.println("InterruptedException occurred during Thread.sleep()");
            }
        }
    }

    private static int getCleanedUpTitledBorderCount() {
        int count = 0;
        for (WeakReference<TitledBorder> ref : weakRefArrTB) {
            if (ref.get() == null) {
                count++;
            }
        }
        return count;
    }
}
