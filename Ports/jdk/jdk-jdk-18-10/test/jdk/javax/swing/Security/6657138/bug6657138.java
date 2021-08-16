/*
 * Copyright (c) 2009, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6657138
 * @summary Verifies that buttons and labels don't share their ui's across appContexts
 * @author Alexander Potochkin
 * @modules java.desktop/sun.awt
 */

import sun.awt.SunToolkit;

import javax.swing.*;
import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.ComponentUI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class bug6657138 implements Runnable {

    private static Map<JComponent, Map<String, ComponentUI>> componentMap =
            Collections.synchronizedMap(
            new HashMap<JComponent, Map<String, ComponentUI>>());

    public void run() {
        SunToolkit.createNewAppContext();
        try {
            testUIMap();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void testUIMap() throws Exception {
        UIManager.LookAndFeelInfo[] lafs = UIManager.getInstalledLookAndFeels();
        Set<JComponent> components = componentMap.keySet();
        for (JComponent c : components) {
            Map<String, ComponentUI> uiMap = componentMap.get(c);

            for (UIManager.LookAndFeelInfo laf : lafs) {
                if ("Nimbus".equals(laf.getName())) {
                    // for some unclear reasons
                    // Nimbus ui delegate for a button is null
                    // when this method is called from the new AppContext
                    continue;
                }
                String className = laf.getClassName();
                try {
                    UIManager.setLookAndFeel(className);
                } catch (final UnsupportedLookAndFeelException ignored) {
                    continue;
                }
                ComponentUI ui = UIManager.getUI(c);
                if (ui == null) {
                    throw new RuntimeException("UI is null for " + c);
                }
                if (ui == uiMap.get(laf.getName())) {
                    throw new RuntimeException(
                            "Two AppContexts share the same UI delegate! \n" +
                                    c + "\n" + ui);
                }
                uiMap.put(laf.getName(), ui);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        componentMap.put(new JButton("JButton"),
                new HashMap<String, ComponentUI>());
        componentMap.put(new JToggleButton("JToggleButton"),
                new HashMap<String, ComponentUI>());
        componentMap.put(new JRadioButton("JRadioButton"),
                new HashMap<String, ComponentUI>());
        componentMap.put(new JCheckBox("JCheckBox"),
                new HashMap<String, ComponentUI>());
        componentMap.put(new JCheckBox("JLabel"),
                new HashMap<String, ComponentUI>());
        testUIMap();
        ThreadGroup group = new ThreadGroup("6657138");
        Thread thread = new Thread(group, new bug6657138());
        thread.start();
        thread.join();
    }
}

