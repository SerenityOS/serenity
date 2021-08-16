/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6199676
 * @summary Tests preview panel after L&F changing
 */

import java.awt.Component;
import java.awt.Container;

import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class Test6199676 implements Runnable {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Test6199676());
    }

    private static Component getPreview(Container container) {
        String name = "ColorChooser.previewPanelHolder";
        for (Component component : container.getComponents()) {
            if (!name.equals(component.getName())) {
                component = (component instanceof Container)
                        ? getPreview((Container) component)
                        : null;
            }
            if (component instanceof Container) {
                container = (Container) component;
                return 1 == container.getComponentCount()
                        ? container.getComponent(0)
                        : null;
            }
        }
        return null;
    }

    private static boolean isShowing(Component component) {
        return (component != null) && component.isShowing();
    }

    public synchronized void run() {
        JColorChooser chooser = new JColorChooser();
        JFrame frame = new JFrame(getClass().getName());
        try {
            frame.add(chooser);
            frame.setVisible(true);
            // Check default preview panel
            setLookAndFeel(UIManager.getInstalledLookAndFeels()[0]);
            SwingUtilities.updateComponentTreeUI(chooser);
            Component component = chooser.getPreviewPanel();
            if (component == null) {
                component = getPreview(chooser);
            }
            if (!isShowing(component)) {
                throw new RuntimeException(
                        "default preview panel is not showing");
            }
            // Check custom preview panel
            chooser.setPreviewPanel(new JPanel());
            setLookAndFeel(UIManager.getInstalledLookAndFeels()[1]);
            SwingUtilities.updateComponentTreeUI(chooser);
            if (isShowing(chooser.getPreviewPanel())) {
                throw new RuntimeException("custom preview panel is showing");
            }
        } finally {
            frame.dispose();
        }
    }

    private static void setLookAndFeel(final UIManager.LookAndFeelInfo laf) {
        try {
            UIManager.setLookAndFeel(laf.getClassName());
        } catch (final UnsupportedLookAndFeelException ignored){
        } catch (ClassNotFoundException | InstantiationException |
                IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
