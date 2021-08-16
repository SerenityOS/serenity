/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6520101
 * @summary JFileChooser throws OOM in 1.4.2, 5.0u4 and 1.6.0
 * @author Praveen Gupta
 * @run main/othervm/timeout=600 -Xmx8m -verify bug6520101
*/

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class bug6520101 implements Runnable {

    private static final int ATTEMPTS = 500;
    private static final int INTERVAL = 100;

    private static final boolean ALWAYS_NEW_INSTANCE = false;
    private static final boolean DO_GC_EACH_INTERVAL = false;
    private static final boolean UPDATE_UI_EACH_INTERVAL = true;
    private static final boolean AUTO_CLOSE_DIALOG = true;

    private static JFileChooser CHOOSER;

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");

        for (int i = 0; i < ATTEMPTS; i++) {
            doAttempt();
        }

        System.out.println("Test passed successfully");
    }

    private static void doAttempt() throws InterruptedException {
        if (ALWAYS_NEW_INSTANCE || (CHOOSER == null))
            CHOOSER = new JFileChooser(".");

        if (UPDATE_UI_EACH_INTERVAL) {
            CHOOSER.updateUI();
        }

        if (AUTO_CLOSE_DIALOG) {
            Thread t = new Thread(new bug6520101(CHOOSER));
            t.start();
            CHOOSER.showOpenDialog(null);
            t.join();
        } else {
            CHOOSER.showOpenDialog(null);
        }

        if (DO_GC_EACH_INTERVAL) {
            System.gc();
        }
    }

    private final JFileChooser chooser;

    bug6520101(JFileChooser chooser) {
        this.chooser = chooser;
    }

    public void run() {
        while (!this.chooser.isShowing()) {
            try {
                Thread.sleep(30);
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }

        Timer timer = new Timer(INTERVAL, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                chooser.cancelSelection();
            }
        });

        timer.setRepeats(false);
        timer.start();
    }
}
