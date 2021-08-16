/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4252173 7077259
 * @summary Inability to reuse the HorizontalSliderThumbIcon
 * @author Pavel Porvatov
 * @run main/othervm -Dswing.defaultlaf=javax.swing.plaf.metal.MetalLookAndFeel bug4252173
 */

import javax.swing.*;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;

public class bug4252173 {
    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme());

                JComponent component = new JLabel();

                Icon horizontalThumbIcon = UIManager.getIcon("Slider.horizontalThumbIcon");

                Icon verticalThumbIcon = UIManager.getIcon("Slider.verticalThumbIcon");

                Graphics g = new BufferedImage(100, 100, BufferedImage.TYPE_4BYTE_ABGR).getGraphics();

                horizontalThumbIcon.paintIcon(component, g, 0, 0);

                verticalThumbIcon.paintIcon(component, g, 0, 0);
            }
        });
    }
}
