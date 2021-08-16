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
  @test
  @key headful
  @bug 6424157
  @author Artem Ananiev: area=eventqueue
  @run main PreserveDispatchThread
*/

import java.awt.*;
import java.awt.event.*;

public class PreserveDispatchThread {

    private static volatile Frame f;
    private static volatile Dialog d;

    private static volatile boolean isEDT = true;

    public static void main(String[] args) throws Exception {
        f = new Frame("F");
        f.setSize(320, 340);
        f.setLocationRelativeTo(null);
        f.setVisible(true);

        try {
            test1();
            if (!isEDT) {
                throw new RuntimeException("Test FAILED (test1): event dispatch thread is changed");
            }

            test2();
            if (!isEDT) {
                throw new RuntimeException("Test FAILED (test2): event dispatch thread is changed");
            }

            test3();
            if (!isEDT) {
                throw new RuntimeException("Test FAILED (test3): event dispatch thread is changed");
            }
        } finally {
            if (d != null) {
                d.dispose();
            }
            f.dispose();
        }
    }

    /*
     * Tests that push/pop doesn't change the dispatch thread if
     * called on EDT.
     */
    private static void test1() throws Exception {
        EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                TestEventQueue teq = new TestEventQueue();
                EventQueue seq = Toolkit.getDefaultToolkit().getSystemEventQueue();
                try {
                    seq.push(teq);
                    d = new TestDialog();
                    d.setVisible(true);
                    checkEDT();
                } finally {
                    teq.pop();
                }
                checkEDT();
            }
        });
    }

    /*
     * Tests that push/pop doesn't change the dispatch thread if
     * called on the main thread.
     */
    private static void test2() throws Exception {
        TestEventQueue teq = new TestEventQueue();
        EventQueue seq = Toolkit.getDefaultToolkit().getSystemEventQueue();
        try {
            seq.push(teq);
            EventQueue.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    checkEDT();
                    d = new TestDialog();
                    d.setVisible(true);
                    checkEDT();
                }
            });
        } finally {
            teq.pop();
        }
    }

    private static final Object test3Lock = new Object();
    private static boolean test3Sync = false;

    /*
     * A complex test: several nested invokeLater() are called and
     * in every runnable a check for EDT is performed. At the ent
     * of the test we wait for all the runnables to be processed
     * and the dialog is disposed; otherwise the last EDT check can
     * be later than this method returns and the whole test is passed.
     */
    private static void test3() throws Exception {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                d = new Dialog(f, true);
                d.setSize(240, 180);
                d.setLocationRelativeTo(f);
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        d.setVisible(true);
                        checkEDT();
                    }
                });
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        TestEventQueue teq = new TestEventQueue();
                        EventQueue seq = Toolkit.getDefaultToolkit().getSystemEventQueue();
                        try {
                            seq.push(teq);
                            checkEDT();
                            EventQueue.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    d.dispose();
                                    checkEDT();
                                    synchronized (test3Lock) {
                                        test3Sync = true;
                                        test3Lock.notify();
                                    }
                                }
                            });
                        } finally {
                            teq.pop();
                        }
                        checkEDT();
                    }
                });
                checkEDT();
            }
        });
        synchronized (test3Lock) {
            while (!test3Sync) {
                try {
                    test3Lock.wait();
                } catch (InterruptedException ie) {
                    break;
                }
            }
        }
        // Make sure all the nested invokeLater/invokeAndWait are processed
        EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    private static void checkEDT() {
        isEDT = isEDT && EventQueue.isDispatchThread();
    }

    private static class TestEventQueue extends EventQueue {
        public TestEventQueue() {
            super();
        }
        public void pop() {
            super.pop();
        }
    }

    private static class TestDialog extends Dialog {
        private volatile boolean dialogShown = false;
        private volatile boolean paintCalled = false;
        public TestDialog() {
            super(f, true);
            setSize(240, 180);
            setLocationRelativeTo(f);
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentShown(ComponentEvent e) {
                    if (paintCalled) {
                        dispose();
                    }
                    dialogShown = true;
                }
            });
        }
        @Override
        public void paint(Graphics g) {
            if (dialogShown) {
                dispose();
            }
            paintCalled = true;
        }
    }

}
