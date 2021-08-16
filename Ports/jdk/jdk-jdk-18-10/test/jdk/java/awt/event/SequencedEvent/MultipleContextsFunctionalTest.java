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

/**
 * @test
 * @bug 8204142
 * @key headful
 * @summary Deadlock when queueing SequencedEvent of different AppContexts
 * @author Laurent Bourges
 * @modules java.desktop/sun.awt
 * @run main/othervm/timeout=30 MultipleContextsFunctionalTest
 */

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public final class MultipleContextsFunctionalTest {

    private static final long serialVersionUID = 1L;

    private static final int NUM_WINDOW = 2;
    private static final int INTERVAL = 50;
    private static final int MAX_TIME = 10000; // 10s
    private static final int TOLERANCE = 10000;// 10s
    private static final int CHECK_LAPSE = 100;
    private static final int MAX_COUNT = MAX_TIME / INTERVAL;
    private static final int EXPECTED = MAX_COUNT * NUM_WINDOW;
    private static final List<TestWindow> WINDOWS = new ArrayList<TestWindow>();

    public static void main(String[] args) {
        for (int i = 0; i < NUM_WINDOW; i++) {
            createWin(i);
        }

        int total = 0;
        int waitingTime = MAX_TIME + TOLERANCE;
        while (waitingTime > 0 && total != EXPECTED) {
            try {
                Thread.sleep(CHECK_LAPSE);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            waitingTime -= CHECK_LAPSE;

            total = 0;
            for (TestWindow window : WINDOWS) {
                total += window.getCounter();
            }
        }

        // Failure if AWT hanging: assert
        System.out.println("Total [" + total + "] - Expected [" + EXPECTED + "]");
        if (total == EXPECTED) {
            System.out.println("Test PASSED");
            return;
        }
        System.out.println("Test FAILED");
        Runtime.getRuntime().halt(-1);
    }

    private static void createWin(int tgNum) {
        new Thread(new ThreadGroup("TG " + tgNum),
                new Runnable() {
            @Override
            public void run() {
                sun.awt.SunToolkit.createNewAppContext();

                final AtomicReference<TestWindow> ref =
                        new AtomicReference<TestWindow>();

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        final TestWindow window = new TestWindow(tgNum);
                        window.setVisible(true);
                        ref.set(window);
                        WINDOWS.add(window);
                    }
                });

                // Wait for window to show
                TestWindow window = ref.get();
                while (window == null) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                    window = ref.get();
                }
                window.enableTimer(true);
            }
        }).start();
    }

    private static final class TestWindow extends JFrame implements ActionListener {

        private final JButton btn;
        private int counter = 0;
        private final Timer t;

        TestWindow(final int num) {
            super("Test Window [" + num + "]");
            setMinimumSize(new Dimension(300, 200));
            setLocation(100 + 400 * (num - 1), 100);

            setLayout(new BorderLayout());
            JLabel textBlock = new JLabel("Lorem ipsum dolor sit amet...");
            add(textBlock);

            btn = new JButton("TEST");
            add(btn, BorderLayout.SOUTH);

            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            pack();

            t = new Timer(INTERVAL, this);
            t.setRepeats(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            this.toFront();
            btn.setText("TEST " + (++counter));
            this.toBack();
            if (counter < MAX_COUNT) {
                enableTimer(true);
            } else {
                dispose();
            }
        }

        void enableTimer(boolean enable) {
            if (enable) {
                t.start();
            } else {
                t.stop();
            }
        }

        int getCounter() {
            return counter;
        }
    }
}
