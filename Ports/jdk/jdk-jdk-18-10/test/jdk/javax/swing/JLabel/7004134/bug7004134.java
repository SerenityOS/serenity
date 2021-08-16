/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7004134
 * @summary JLabel containing a ToolTipText does no longer show ToolTip after browser refresh
 * @author Pavel Porvatov
 * @modules java.desktop/sun.awt
 * @modules java.desktop/javax.swing:open
 */

import sun.awt.SunToolkit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;

public class bug7004134 {
    private static volatile JFrame frame;

    private static volatile JLabel label;

    private static volatile int toolTipWidth;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                label = new JLabel("A JLabel used as object for an HTML-formatted tooltip");
                label.setToolTipText("<html><body bgcolor=FFFFE1>An HTML-formatted ToolTip</body></html>");

                frame = new JFrame();

                frame.add(label);
                frame.pack();
                frame.setVisible(true);
            }
        });

        ((SunToolkit) SunToolkit.getDefaultToolkit()).realSync();

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                ToolTipManager toolTipManager = ToolTipManager.sharedInstance();

                toolTipManager.setInitialDelay(0);
                toolTipManager.mouseMoved(new MouseEvent(label, 0, 0, 0, 0, 0, 0, false));
            }
        });

        Thread.sleep(500);

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                toolTipWidth = getTipWindow().getWidth();

                frame.dispose();
            }
        });

        Thread thread = new Thread(new ThreadGroup("Some ThreadGroup"), new Runnable() {
            public void run() {
                SunToolkit.createNewAppContext();

                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            frame = new JFrame();

                            frame.add(label);
                            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                            frame.pack();
                            frame.setVisible(true);
                        }
                    });

                    ((SunToolkit) SunToolkit.getDefaultToolkit()).realSync();

                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            ToolTipManager toolTipManager = ToolTipManager.sharedInstance();

                            toolTipManager.setInitialDelay(0);
                            toolTipManager.mouseMoved(new MouseEvent(label, 0, 0, 0, 0, 0, 0, false));
                        }
                    });

                    Thread.sleep(500);

                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            int newToolTipWidth = getTipWindow().getWidth();

                            frame.dispose();

                            if (toolTipWidth != newToolTipWidth) {
                                throw new RuntimeException("Tooltip width is different. Initial: " + toolTipWidth +
                                        ", new: " + newToolTipWidth);
                            }
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }
        });

        thread.start();
        thread.join();
    }

    private static Component getTipWindow() {
        try {
            Field tipWindowField = ToolTipManager.class.getDeclaredField("tipWindow");

            tipWindowField.setAccessible(true);

            Popup value = (Popup) tipWindowField.get(ToolTipManager.sharedInstance());

            Field componentField = Popup.class.getDeclaredField("component");

            componentField.setAccessible(true);

            return (Component) componentField.get(value);
        } catch (Exception e) {
            throw new RuntimeException("getToolTipComponent failed", e);
        }
    }
}
