/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 @bug 8056151 8131751
 @summary Switching to GTK L&F on-the-fly leads to X Window System error RenderBadPicture
 @run main/othervm -Dswing.defaultlaf=javax.swing.plaf.metal.MetalLookAndFeel -Dsun.java2d.xrender=T RenderBadPictureCrash
 */
import java.awt.Color;
import java.awt.GraphicsDevice;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class RenderBadPictureCrash {

    public static void main(String[] args) throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(() -> {
            JFrame f = new JFrame();
            f.setUndecorated(true);
            GraphicsDevice gd = f.getGraphicsConfiguration().getDevice();
            if (gd.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)) {
                f.setBackground(new Color(0, 0, 0, 0));
            }
            f.setSize(200, 300);
            f.setVisible(true);

            try {
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
            } catch (Exception e) {
                System.err.println(e);
                System.err.println("Could not set GTKLookAndFeel, skipping this test");
            } finally {
                f.dispose();
            }
        });
    }

}
