/*
 * Copyright (c) 2009, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6794831
 * @summary Infinite loop while painting ticks on Slider with maximum=MAX_INT
 * @author Pavel Porvatov
   @run main bug6794831
 */

import javax.swing.*;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class bug6794831 {
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    public static void main(String args[])
            throws InterruptedException, InvocationTargetException {
        new bug6794831().run();
    }

    private void run() throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                for (UIManager.LookAndFeelInfo lookAndFeelInfo : UIManager.getInstalledLookAndFeels()) {
                    try {
                        UIManager.setLookAndFeel(lookAndFeelInfo.getClassName());
                    } catch (Exception e) {
                        fail(e.getMessage());
                    }

                    BufferedImage image = new BufferedImage(300, 200, BufferedImage.TYPE_INT_ARGB);

                    // Test 1
                    JSlider slider = new JSlider(0, Integer.MAX_VALUE - 1, 0);

                    slider.setMajorTickSpacing((Integer.MAX_VALUE - 1) / 4);
                    slider.setPaintTicks(true);

                    ((BasicSliderUI) slider.getUI()).paintTicks(image.getGraphics());

                    // Test 2
                    slider = new JSlider(0, Integer.MAX_VALUE - 1, 0);

                    slider.setMinorTickSpacing((Integer.MAX_VALUE - 1) / 4);
                    slider.setPaintTicks(true);

                    ((BasicSliderUI) slider.getUI()).paintTicks(image.getGraphics());

                    // Test 3
                    slider = new JSlider(0, Integer.MAX_VALUE - 1, 0);

                    slider.setOrientation(JSlider.VERTICAL);
                    slider.setMajorTickSpacing((Integer.MAX_VALUE - 1) / 4);
                    slider.setPaintTicks(true);

                    ((BasicSliderUI) slider.getUI()).paintTicks(image.getGraphics());

                    // Test 4
                    slider = new JSlider(0, Integer.MAX_VALUE - 1, 0);

                    slider.setOrientation(JSlider.VERTICAL);
                    slider.setMinorTickSpacing((Integer.MAX_VALUE - 1) / 4);
                    slider.setPaintTicks(true);

                    ((BasicSliderUI) slider.getUI()).paintTicks(image.getGraphics());

                    countDownLatch.countDown();
                }
            }
        });

        if (countDownLatch.await(3000, TimeUnit.MILLISECONDS)) {
            System.out.println("bug6794831 passed");
        } else {
            fail("bug6794831 failed");
        }
    }

    private static void fail(String msg) {
        throw new RuntimeException(msg);
    }
}
