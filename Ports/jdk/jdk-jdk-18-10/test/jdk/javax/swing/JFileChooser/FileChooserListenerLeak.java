/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * @test
 * @bug 8240633
 * @key headful
 * @summary FileChooserUI delegates should clean listeners in JFileChooser
 */
public final class FileChooserListenerLeak {

    public static void main(final String[] args) throws Exception {
        EventQueue.invokeAndWait(()->{
            JFileChooser chooser = new JFileChooser();
            checkListenersCount(chooser);
            LookAndFeelInfo[] infos = UIManager.getInstalledLookAndFeels();
            for (int i = 0; i < 100; ++i) {
                for (LookAndFeelInfo installedLookAndFeel : infos) {
                    setLookAndFeel(installedLookAndFeel);
                    SwingUtilities.updateComponentTreeUI(chooser);
                }
            }
            checkListenersCount(chooser);
        });
    }

    private static void checkListenersCount(Component comp) {
        test(comp.getComponentListeners());
        test(comp.getFocusListeners());
        test(comp.getHierarchyListeners());
        test(comp.getHierarchyBoundsListeners());
        test(comp.getKeyListeners());
        test(comp.getMouseListeners());
        test(comp.getMouseMotionListeners());
        test(comp.getMouseWheelListeners());
        test(comp.getInputMethodListeners());
        test(comp.getPropertyChangeListeners());
        if (comp instanceof JComponent) {
            test(((JComponent) comp).getAncestorListeners());
            test(((JComponent) comp).getVetoableChangeListeners());
        }
        if (comp instanceof JMenuItem) {
            test(((JMenuItem) comp).getMenuKeyListeners());
            test(((JMenuItem) comp).getMenuDragMouseListeners());
        }
        if (comp instanceof JMenu) {
            test(((JMenu) comp).getMenuListeners());
        }
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                checkListenersCount(child);
            }
        }
    }

    /**
     * Checks the count of specific listeners, assumes that the proper
     * implementation does not use more than 10 listeners.
     */
    private static void test(Object[] listeners) {
        int length = listeners.length;
        if (length > 20) {
            throw new RuntimeException("The count of listeners is: " + length);
        }
    }

    private static void setLookAndFeel(UIManager.LookAndFeelInfo laf) {
        try {
            UIManager.setLookAndFeel(laf.getClassName());
        } catch (UnsupportedLookAndFeelException ignored){
            System.out.println("Unsupported LookAndFeel: " + laf.getClassName());
        } catch (ClassNotFoundException | InstantiationException |
                IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
