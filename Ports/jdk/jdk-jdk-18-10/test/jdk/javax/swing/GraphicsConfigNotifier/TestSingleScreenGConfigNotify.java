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

import java.awt.EventQueue;
import java.awt.GraphicsConfiguration;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JFrame;

/**
 * @test
 * @key headful
 * @bug 8201552
 * @summary Verifies if graphicsConfiguration property notification is sent
 *          when frame is shown on the screen.
 * @run main TestSingleScreenGConfigNotify
 * @run main/othervm -Dsun.java2d.uiScale=2.25 TestSingleScreenGConfigNotify
 */
public final class TestSingleScreenGConfigNotify {

    private static String name = "graphicsConfiguration";
    private static CountDownLatch go = new CountDownLatch(1);
    private static JFrame frame;
    private static GraphicsConfiguration after;
    private static GraphicsConfiguration before;
    private static JButton button;

    public static void main(final String[] args) throws Exception {
        EventQueue.invokeAndWait(() -> {
            frame = new JFrame();

            frame.setSize(300,300);
            frame.setLocationRelativeTo(null);
            button = new JButton();
            button.addPropertyChangeListener(evt -> {
                if (evt.getPropertyName().equals(name)) {
                    go.countDown();
                }
            });

            before = button.getGraphicsConfiguration();

            frame.add(button);
            frame.setVisible(true);
        });

        boolean called = go.await(10, TimeUnit.SECONDS);

        EventQueue.invokeAndWait(() -> {
            after = button.getGraphicsConfiguration();
            frame.dispose();
        });

        if (Objects.equals(before, after) && called) {
            throw new RuntimeException("propertyChange() should not be called");
        }
        if (!Objects.equals(before, after) && !called) {
            throw new RuntimeException("propertyChange() should be called");
        }
    }
}
