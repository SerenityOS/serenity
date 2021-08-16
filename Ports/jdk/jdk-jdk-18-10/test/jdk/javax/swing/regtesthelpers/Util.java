/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * <p>This class contains utilities useful for regression testing.
 * <p>When using jtreg you would include this class via something like:
 * <pre>
 *
 * @library ../../regtesthelpers
 * @build Util
 * </pre>
 */

public class Util {

    /**
     * Convert a rectangle from coordinate system of Component c to
     * screen coordinate system.
     *
     * @param r a non-null Rectangle
     * @param c a Component whose coordinate system is used for conversion
     */
    public static void convertRectToScreen(Rectangle r, Component c) {
        Point p = new Point(r.x, r.y);
        SwingUtilities.convertPointToScreen(p, c);
        r.x = p.x;
        r.y = p.y;
    }

    /**
     * Compares two bufferedImages pixel-by-pixel.
     * return true if all pixels in the two areas are identical
     */
    public static boolean compareBufferedImages(BufferedImage bufferedImage0, BufferedImage bufferedImage1) {
        int width = bufferedImage0.getWidth();
        int height = bufferedImage0.getHeight();

        if (width != bufferedImage1.getWidth() || height != bufferedImage1.getHeight()) {
            return false;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (bufferedImage0.getRGB(x, y) != bufferedImage1.getRGB(x, y)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Fills the heap until OutOfMemoryError occurs. This method is useful for
     * WeakReferences removing. To minimize the amount of filled memory the
     * test should provide reasonable heap size via -mx option.
     */
    public static void generateOOME() {
        List<Object> bigLeak = new LinkedList<Object>();

        boolean oome = false;

        System.out.print("Filling the heap");

        try {
            for(int i = 0; true ; i++) {
                // Now, use up all RAM
                bigLeak.add(new byte[1024 * 1024]);

                System.out.print(".");

                // Give the GC a change at that weakref
                if (i % 10 == 0) {
                    System.gc();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (OutOfMemoryError e) {
            bigLeak = null;
            oome = true;
        }

        System.out.println("");

        if (!oome) {
            throw new RuntimeException("Problem with test case - never got OOME");
        }

        System.out.println("Got OOME");
    }

    /**
     * Find a sub component by class name.
     * Always run this method on the EDT thread
     */
    public static Component findSubComponent(Component parent, String className) {
        String parentClassName = parent.getClass().getName();

        if (parentClassName.contains(className)) {
            return parent;
        }

        if (parent instanceof Container) {
            for (Component child : ((Container) parent).getComponents()) {
                Component subComponent = findSubComponent(child, className);

                if (subComponent != null) {
                    return subComponent;
                }
            }
        }

        return null;
    }

     /**
     * Hits mnemonics by robot.
     */
    public static void hitMnemonics(Robot robot, int... keys) {

        ArrayList<Integer> mnemonicKeyCodes = getSystemMnemonicKeyCodes();
        for (Integer mnemonic : mnemonicKeyCodes) {
            robot.keyPress(mnemonic);
        }

        hitKeys(robot, keys);

        for (Integer mnemonic : mnemonicKeyCodes) {
            robot.keyRelease(mnemonic);
        }
    }

     /**
     * Hits keys by robot.
     */
    public static void hitKeys(Robot robot, int... keys) {
        for (int i = 0; i < keys.length; i++) {
            robot.keyPress(keys[i]);
        }

        for (int i = keys.length - 1; i >= 0; i--) {
            robot.keyRelease(keys[i]);
        }
    }

    /**
     * Moves mouse smoothly from (x0, y0) to (x1, y1).
     */
    public static void glide(Robot robot, int x0, int y0, int x1, int y1) throws AWTException {
        float dmax = (float) Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        float dx = (x1 - x0) / dmax;
        float dy = (y1 - y0) / dmax;

        for (int i = 0; i <= dmax; i += 10) {
            robot.mouseMove((int) (x0 + dx * i), (int) (y0 + dy * i));
        }
    }

    /**
     * Gets component center point
     *
     * @return center point of the <code>component</code>
     */
    public static Point getCenterPoint(final Component component) throws Exception {
        return Util.invokeOnEDT(new Callable<Point>() {

            @Override
            public Point call() throws Exception {
                Point p = component.getLocationOnScreen();
                Dimension size = component.getSize();
                return new Point(p.x + size.width / 2, p.y + size.height / 2);
            }
        });
    }

    /**
     * Invokes the <code>task</code> on the EDT thread.
     *
     * @return result of the <code>task</code>
     */
    public static <T> T invokeOnEDT(final Callable<T> task) throws Exception {
        final List<T> result = new ArrayList<>(1);
        final Exception[] exception = new Exception[1];

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    result.add(task.call());
                } catch (Exception e) {
                    exception[0] = e;
                }
            }
        });

        if (exception[0] != null) {
            throw exception[0];
        }

        return result.get(0);
    }

    /**
     * Gets the key codes list from modifiers
     * @param modifiers an integer combination of the modifier constants
     * @return key codes list
     */
    public static ArrayList<Integer> getKeyCodesFromKeyMask(int modifiers) {
        ArrayList<Integer> result = new ArrayList<>();
        if ((modifiers & InputEvent.CTRL_MASK) != 0) {
            result.add(KeyEvent.VK_CONTROL);
        }
        if ((modifiers & InputEvent.ALT_MASK) != 0) {
            result.add(KeyEvent.VK_ALT);
        }
        if ((modifiers & InputEvent.SHIFT_MASK) != 0) {
            result.add(KeyEvent.VK_SHIFT);
        }
        if ((modifiers & InputEvent.META_MASK) != 0) {
            result.add(KeyEvent.VK_META);
        }
        return result;
    }

    /**
     * Gets key codes from system mnemonic key mask
     * @return key codes list
     */
    public static ArrayList<Integer> getSystemMnemonicKeyCodes() {
        String osName = System.getProperty("os.name");
        ArrayList<Integer> result = new ArrayList<>();
        if (osName.contains("OS X")) {
            result.add(KeyEvent.VK_CONTROL);
        }
        result.add(KeyEvent.VK_ALT);
        return result;
    }

   /**
    * Creates and returns a JDialog with two button, one that says pass,
    * another that says fail. The fail button is wired to call
    * <code>uiTestFailed</code> with <code>failString</code> and the pass
    * button is wired to invoked <code>uiTestPassed</code>.
    * <p>The content pane of the JDialog uses a BorderLayout with the
    * buttons inside a horizontal box with filler between them and the
    * pass button on the left.
    * <p>The returned Dialog has not been packed, or made visible, it is
    * up to the caller to do that (after putting in some useful components).
    */
    public static JDialog createModalDialogWithPassFailButtons(final String failString) {
        JDialog  retDialog = new JDialog();
        Box      buttonBox = Box.createHorizontalBox();
        JButton  passButton = new JButton("Pass");
        JButton  failButton = new JButton("Fail");

        passButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                retDialog.dispose();
            }
        });
        failButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                retDialog.dispose();
                throw new RuntimeException("Test failed. " + failString);
            }
        });
        retDialog.setTitle("Test");
        retDialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        buttonBox.add(passButton);
        buttonBox.add(Box.createGlue());
        buttonBox.add(failButton);
        retDialog.getContentPane().add(buttonBox, BorderLayout.SOUTH);
        retDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        return retDialog;
    }
}
