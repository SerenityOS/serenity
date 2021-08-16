/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8178025
 * @summary  Verifies if graphicsConfiguration property notification is sent
 *           when frame is moved from one screen to another in multiscreen
 *           environment.
 * @key headful
 * @run main TestMultiScreenGConfigNotify
 */

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class TestMultiScreenGConfigNotify {

    static JFrame f;
    static String propName[];
    static int propCount = 0;
    static boolean result = false;
    public static void main(String[] args) throws Exception {

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gds = ge.getScreenDevices();
        if (gds.length < 2) {
            return;
        }
        GraphicsConfiguration gc = gds[0].getDefaultConfiguration();
        GraphicsConfiguration gc2 = gds[1].getDefaultConfiguration();
        propName = new String[10];
        SwingUtilities.invokeAndWait(() -> {
            f = new JFrame();
            f.setSize(300, 300);
            f.setBounds(gc.getBounds().x, gc.getBounds().y, f.getWidth(), f.getHeight());
            f.setVisible(true);

            f.addPropertyChangeListener((PropertyChangeEvent evt) -> {
                String name = evt.getPropertyName();
                System.out.println("propertyChange " + name);
                propName[propCount] = name;
                propCount++;
            });
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
            }
            f.setBounds(gc2.getBounds().x, gc2.getBounds().y,
                        f.getWidth(), f.getHeight());
        });

        Thread.sleep(1000);
        for(int i = 0; i < propCount; i++) {
            if (propName[i].equals("graphicsConfiguration")) {
                result = true;
            }
        }
        SwingUtilities.invokeAndWait(() ->  f.dispose());
        if(!result) {
            throw new RuntimeException("graphicsConfiguration notification not sent");
        }
    }
}
