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
  @test
  @key headful
  @bug 8015500
  @summary DisposeAction multiplies the WINDOW_CLOSED event.
  @author jlm@joseluismartin.info
  @run main WindowClosedEventOnDispose
 */


import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * WindowClosedEventOnDispose.java
 * Summary: tests that Window don't multiplies the WINDOW_CLOSED event
 * on dispose.
 * Test fails if fire more events that expected;
 */
public class WindowClosedEventOnDispose {

    private static int N_LOOPS = 5;
    private static int N_DIALOGS = 2;

    public static void main(String args[]) throws Exception {
        tesWithFrame();
        testWithoutFrame();
        testHidenChildDispose();
        testHidenWindowDispose();
    }

    /**
     * Test WINDOW_CLOSED event received by a dialog
     * that have a owner window.
     * @throws Exception
     */
    public static void tesWithFrame() throws Exception {
        doTest(true);
    }

    /**
     * Test WINDOW_CLOSED event received by a dialog
     * that don't have a owner window.
     * @throws Exception
     */
    public static void testWithoutFrame() throws Exception  {
        System.out.println("Run without owner Frame");
        doTest(false);
    }

    /**
     * Test if a dialog that has never been shown fire
     * the WINDOW_CLOSED event on parent dispose().
     * @throws Exception
     */
    public static void testHidenChildDispose() throws Exception {
        JFrame f = new JFrame();
        JDialog dlg = new JDialog(f);
        Listener l = new Listener();
        dlg.addWindowListener(l);
        f.dispose();
        waitEvents();

        assertEquals(0, l.getCount());
    }

    /**
     * Test if a dialog fire the WINDOW_CLOSED event
     * on parent dispose().
     * @throws Exception
     */
    public static void testVisibleChildParentDispose() throws Exception {
        JFrame f = new JFrame();
        JDialog dlg = new JDialog(f);
        Listener l = new Listener();
        dlg.addWindowListener(l);
        dlg.setVisible(true);
        f.dispose();
        waitEvents();

        assertEquals(1, l.getCount());
    }

    /**
     * Test if a Window that has never been shown fire the
     * WINDOW_CLOSED event on dispose()
     */
    public static void testHidenWindowDispose() throws Exception {
        JFrame f = new JFrame();
        Listener l = new Listener();
        f.addWindowListener(l);
        f.dispose();
        waitEvents();

        assertEquals(0, l.getCount());
    }

    /**
     * Test if a JDialog receive the correct number
     * of WINDOW_CLOSED_EVENT
     * @param useFrame true if use a owner frame
     * @throws Exception
     */
    private static void doTest(final boolean useFrame) throws Exception {
        final Listener l  = new Listener();
        final JFrame f = new JFrame();

        for (int i = 0; i < N_LOOPS; i++) {

            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    JDialog[] dialogs = new JDialog[N_DIALOGS];
                    for (int i = 0; i < N_DIALOGS; i++) {
                        if (useFrame) {
                            dialogs[i]= new JDialog(f);
                        }
                        else {
                            dialogs[i] = new JDialog();
                        }

                        dialogs[i].addWindowListener(l);
                        dialogs[i].setVisible(true);
                    }

                    // Dispose all
                    for (JDialog d : dialogs)
                        d.dispose();

                    f.dispose();
                }
            });
        }

        waitEvents();

        assertEquals(N_DIALOGS * N_LOOPS, l.getCount());
    }

    private static void waitEvents() throws InterruptedException {
        // Wait until events are dispatched
        while (Toolkit.getDefaultToolkit().getSystemEventQueue().peekEvent() != null)
            Thread.sleep(100);
    }

    /**
     * @param expected the expected value
     * @param real the real value
     */
    private static void assertEquals(int expected, int real) throws Exception {
        if (expected != real) {
            throw new Exception("Expected events: " + expected + " Received Events: " + real);
        }
    }

}

/**
 * Listener to count events
 */
class Listener extends WindowAdapter {

    private volatile int count = 0;

    public void windowClosed(WindowEvent e) {
        count++;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
