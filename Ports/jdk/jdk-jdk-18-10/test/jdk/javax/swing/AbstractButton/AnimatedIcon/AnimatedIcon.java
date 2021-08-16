/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;

/**
 * @test
 * @bug 6573305
 * @summary Animated icon should animate when the JButton is pressed.
 * @author Sergey Bylokhov
 */
public final class AnimatedIcon {

    public static void main(final String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final BufferedImage bi = new BufferedImage(1, 1, TYPE_INT_RGB);
            final ImageIcon icon = new ImageIcon(bi);
            final JButton button = new JButton(icon);
            // Default icon is set => imageUpdate should return true for it
            isAnimated(bi, button);
            button.getModel().setPressed(true);
            button.getModel().setArmed(true);
            isAnimated(bi, button);
            button.getModel().setPressed(false);
            button.getModel().setArmed(false);
            button.getModel().setSelected(true);
            isAnimated(bi, button);
            button.getModel().setSelected(false);
            button.getModel().setRollover(true);
            button.setRolloverEnabled(true);
            isAnimated(bi, button);
            button.getModel().setSelected(true);
            isAnimated(bi, button);
            // Default icon is not set => imageUpdate should return true for
            // other icons if any
            button.setIcon(null);
            button.setPressedIcon(icon);
            button.getModel().setPressed(true);
            button.getModel().setArmed(true);
            isAnimated(bi, button);
        });
    }

    private static void isAnimated(BufferedImage bi, JButton button) {
        if (!button.imageUpdate(bi, ImageObserver.SOMEBITS, 0, 0, 1, 1)) {
            throw new RuntimeException();
        }
    }
}
