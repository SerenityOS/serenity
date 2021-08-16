/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JDialog;
import javax.swing.JFrame;

/*
  @test
  @key headful
  @bug 8160570 8166015
  @summary Tests that a modal dialog receives WINDOW_ACTIVATED
            & WINDOW_GAINED_FOCUS on first show.
*/

public class ModalDialogActivationTest {
    static final Object lock = new Object();
    static volatile boolean activated;
    static volatile boolean focused;

    public static void main(String[] args) throws InterruptedException {
        EventQueue.invokeLater(() -> runGUI());

        long time = System.currentTimeMillis();
        synchronized (lock) {
            while (!activated || !focused) {
                lock.wait(5000);
                if (System.currentTimeMillis() - time >= 5000) break;
            }
        }
        if (!activated || !focused) {
            throw new RuntimeException("Test FAILED: activated: " + activated
                                        + ", focused: " + focused);
        }
        System.out.println("Test PASSED");
    }

    static void runGUI() {
        JFrame f = new JFrame("frame");
        final JDialog d = new MyModalDialog(f, "dialog");
        d.addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                synchronized (lock) {
                    activated = true;
                    lock.notifyAll();
                }
            }
        });
        d.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                synchronized (lock) {
                    focused = true;
                    lock.notifyAll();
                }
            }
        });
        f.setVisible(true);
        d.setVisible(true);
    }

    static class MyModalDialog extends JDialog {
        public MyModalDialog(Frame owner, String title) {
            super(owner, title, true);
        }

        @Override
        public boolean getFocusableWindowState() {
            try {
                // let Toolkit thread go ahead
                Thread.sleep(100);
            } catch (InterruptedException ignore) {
            }
            return super.getFocusableWindowState();
        }
    }
}
