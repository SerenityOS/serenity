/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug     8073001 8081764
 * @summary Test verifies that combo box with custom editor renders
 *          focus ring around arrow button correctly.
 * @library /test/lib
 * @build jdk.test.lib.Platform
 * @run     main CustomComboBoxFocusTest
 */

import java.awt.AWTException;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import javax.imageio.ImageIO;
import javax.swing.ComboBoxEditor;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import jdk.test.lib.Platform;

public class CustomComboBoxFocusTest {

    private static CustomComboBoxFocusTest test = null;

    public static void main(String[] args) {
        if (!Platform.isOSX()) {
            System.out.println("Only Mac platform test. Test is skipped for other OS.");
            return;
        }

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    test = new CustomComboBoxFocusTest();
                }
            });
        } catch (InterruptedException | InvocationTargetException e ) {
            throw new RuntimeException("Test failed.", e);
        }

        SwingUtilities.invokeLater(test.init);

        try {
            System.out.println("Wait for screenshots...");
            test.testDone.await();
        } catch (InterruptedException e) {
            throw new RuntimeException("Test failed.", e);
        }
        System.out.println("Compare screenshots...");
        if (!test.match()) {
            throw new RuntimeException("Test failed.");
        }
        System.out.println("Test passed.");
    }

    private final JComboBox<String> ref = new JComboBox<String>() {
        public String toString() {
            return "reference";
        }
    };

    private final JComboBox<String> custom = new JComboBox<String>() {
        public String toString() {
            return "custom";
        }
    };

    private final JFrame frame;

    private CountDownLatch testDone = new CountDownLatch(1);

    private Robot robot;

    public CustomComboBoxFocusTest() {
        frame = new JFrame(System.getProperty("java.version"));

        try {
            robot = new Robot(frame.getGraphicsConfiguration().getDevice());

        } catch (AWTException e) {
            throw new RuntimeException("Test failed.", e);
        }
    }

    private boolean match() {
        final BufferedImage a = captureRef.img;
        final BufferedImage b = captureCustom.img;

        final int w = Math.min(a.getWidth(), b.getWidth());
        final int h = Math.min(a.getHeight(), b.getHeight());

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    private JComboBox<String> getReference() {
        return ref;
    }

    private JComboBox<String> getCustom() {
        return custom;
    }

    private JFrame getFrame() {
        return frame;
    }

    private static abstract class Step implements Runnable {
        final public void run() {
            doStep();

            final Step next = nextStep();

            if (next != null) {
                SwingUtilities.invokeLater(next);
            }
        }

        public abstract void doStep();

        public abstract Step nextStep();
    }

    private final Step init = new Step() {
        public void doStep() {
            final JFrame f = getFrame();

            final JPanel p = new JPanel(new GridLayout(4, 1, 20, 20));

            JComboBox<String> r = getReference();
            r.setEditable(true);
            r.addItem("One");

            JComboBox<String> c = getCustom();
            c.setEditable(true);
            c.addItem("One");
            final ComboBoxEditor e = new ComboBoxEditor() {
                private JTextField text = new JTextField();

                @Override
                public Component getEditorComponent() {
                    return this.text;
                }

                @Override
                public void setItem(Object o) {
                    text.setText(o == null ? "" : o.toString());
                }

                @Override
                public Object getItem() {
                    return text.getText();
                }

                @Override
                public void selectAll() {
                    text.selectAll();
                }

                @Override
                public void addActionListener(ActionListener actionListener) {
                    text.addActionListener(actionListener);
                }

                @Override
                public void removeActionListener(ActionListener actionListener) {
                    text.removeActionListener(actionListener);
                }
            };
            c.setEditor(e);

            p.add(new JLabel("Reference"));
            p.add(r);
            p.add(c);
            p.add(new JLabel("Custom"));

            f.add(p);

            f.pack();
            f.setVisible(true);
        }

        public Step nextStep() {
            return focusRef;
        }
    };

    private class FocusStep extends Step {
        private final JComboBox<String> target;
        private final Step focusHandler;
        private final Step next;

        public FocusStep(JComboBox<String> t, Step h, Step n) {
            target = t;
            focusHandler = h;
            next = n;
        }

        public void doStep() {
            System.out.println("Request focus on " + target);
            final Component c = target.getEditor().getEditorComponent();

            c.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    SwingUtilities.invokeLater(focusHandler);
                }

                @Override
                public void focusLost(FocusEvent e) {

                }
            });

            c.requestFocus();

        }

        public Step nextStep() {
            return next;
        }
    }


    private class CaptureStep extends Step {
        private final JComboBox<String> target;
        private BufferedImage img;
        private String fname;
        private final Step next;

        private final int timeout = 2000;

        public CaptureStep(JComboBox<String> t, String name, Step n) {
            target = t;
            next = n;
            fname = name;
        }

        public void doStep() {
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
            }
            System.out.println("Capture sceeenshot of " + target);

            Rectangle bounds = target.getBounds();
            Point p = target.getLocationOnScreen();
            System.out.println("Target bounds: " + bounds);
            System.out.println("Target location: " + p);

            bounds.x = p.x;
            bounds.y = p.y;

            img = robot.createScreenCapture(bounds);

            try {
                ImageIO.write(img, "PNG", new File(fname + ".png"));
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

        }

        public Step nextStep() {
            return next;
        }
    }

    private final Step done = new Step() {
        public void doStep() {
            JFrame f = getFrame();
            if (f != null) {
                f.dispose();
            }
            System.out.println("Done");

            testDone.countDown();
        }

        public Step nextStep() {
            return null;
        }
    };

    private final CaptureStep captureCustom = new CaptureStep(getCustom(), "cb_custom", done);

    private final FocusStep focusCustom = new FocusStep(getCustom(), captureCustom, null);

    private final CaptureStep captureRef = new CaptureStep(getReference(), "cb_ref", focusCustom);

    private final FocusStep focusRef = new FocusStep(getReference(), captureRef, null);
}
