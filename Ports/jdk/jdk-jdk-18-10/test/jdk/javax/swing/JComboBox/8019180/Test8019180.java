/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.CountDownLatch;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * @test
 * @key headful
 * @bug 8019180
 * @summary Tests that combobox works if it is used as action listener
 * @author Sergey Malenkov
 */

public class Test8019180 implements Runnable {
    private static final CountDownLatch LATCH = new CountDownLatch(1);
    private static final String[] ITEMS = {"First", "Second", "Third", "Fourth"};

    public static void main(String[] args) throws InterruptedException {
        SwingUtilities.invokeLater(new Test8019180());
        LATCH.await();
    }

    private JComboBox<String> test;

    @Override
    public void run() {
        if (this.test == null) {
            this.test = new JComboBox<>(ITEMS);
            this.test.addActionListener(this.test);
            JFrame frame = new JFrame();
            frame.add(test);
            frame.pack();
            frame.setVisible(true);
            SwingUtilities.invokeLater(this);
        } else {
            int index = this.test.getSelectedIndex();
            this.test.setSelectedIndex(1 + index);
            if (0 > this.test.getSelectedIndex()) {
                System.err.println("ERROR: no selection");
                System.exit(8019180);
            }
            SwingUtilities.getWindowAncestor(this.test).dispose();
            LATCH.countDown();
        }
    }
}
