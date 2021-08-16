/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package org.netbeans.jemmy.drivers;

import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.JemmyException;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.drivers.buttons.ButtonMouseDriver;
import org.netbeans.jemmy.drivers.focus.APIFocusDriver;
import org.netbeans.jemmy.drivers.focus.MouseFocusDriver;
import org.netbeans.jemmy.drivers.lists.ChoiceDriver;
import org.netbeans.jemmy.drivers.lists.JComboMouseDriver;
import org.netbeans.jemmy.drivers.lists.JListMouseDriver;
import org.netbeans.jemmy.drivers.lists.JTabAPIDriver;
import org.netbeans.jemmy.drivers.lists.JTableHeaderDriver;
import org.netbeans.jemmy.drivers.lists.ListKeyboardDriver;
import org.netbeans.jemmy.drivers.menus.AppleMenuDriver;
import org.netbeans.jemmy.drivers.menus.DefaultJMenuDriver;
import org.netbeans.jemmy.drivers.menus.QueueJMenuDriver;
import org.netbeans.jemmy.drivers.scrolling.JScrollBarAPIDriver;
import org.netbeans.jemmy.drivers.scrolling.JSliderAPIDriver;
import org.netbeans.jemmy.drivers.scrolling.JSplitPaneDriver;
import org.netbeans.jemmy.drivers.scrolling.ScrollPaneDriver;
import org.netbeans.jemmy.drivers.scrolling.ScrollbarDriver;
import org.netbeans.jemmy.drivers.tables.JTableMouseDriver;
import org.netbeans.jemmy.drivers.text.AWTTextKeyboardDriver;
import org.netbeans.jemmy.drivers.text.SwingTextKeyboardDriver;
import org.netbeans.jemmy.drivers.trees.JTreeAPIDriver;
import org.netbeans.jemmy.drivers.windows.DefaultFrameDriver;
import org.netbeans.jemmy.drivers.windows.DefaultWindowDriver;
import org.netbeans.jemmy.drivers.windows.InternalFrameAPIDriver;

/**
 * Installs all necessary drivers for Jemmy operators except low-level drivers
 * which are installed by
 * <a href="InputDriverInstaller.java">InputDriverInstaller</a>.
 *
 * @author Alexandre Iline(alexandre.iline@oracle.com)
 */
public class APIDriverInstaller extends ArrayDriverInstaller {

    /**
     * Constructs a DefaultDriverInstaller object.
     *
     * @param shortcutEvents Signals whether shortcut mode is used.
     */
    public APIDriverInstaller(boolean shortcutEvents) {
        super(new String[]{
            DriverManager.LIST_DRIVER_ID,
            DriverManager.MULTISELLIST_DRIVER_ID,
            DriverManager.TREE_DRIVER_ID,
            DriverManager.TEXT_DRIVER_ID,
            DriverManager.TEXT_DRIVER_ID,
            DriverManager.SCROLL_DRIVER_ID,
            DriverManager.SCROLL_DRIVER_ID,
            DriverManager.SCROLL_DRIVER_ID,
            DriverManager.SCROLL_DRIVER_ID,
            DriverManager.SCROLL_DRIVER_ID,
            DriverManager.SCROLL_DRIVER_ID,
            DriverManager.BUTTON_DRIVER_ID,
            DriverManager.LIST_DRIVER_ID,
            DriverManager.LIST_DRIVER_ID,
            DriverManager.MULTISELLIST_DRIVER_ID,
            DriverManager.LIST_DRIVER_ID,
            DriverManager.LIST_DRIVER_ID,
            DriverManager.MULTISELLIST_DRIVER_ID,
            DriverManager.TABLE_DRIVER_ID,
            DriverManager.LIST_DRIVER_ID,
            DriverManager.FRAME_DRIVER_ID,
            DriverManager.WINDOW_DRIVER_ID,
            DriverManager.FRAME_DRIVER_ID,
            DriverManager.INTERNAL_FRAME_DRIVER_ID,
            DriverManager.WINDOW_DRIVER_ID,
            DriverManager.FOCUS_DRIVER_ID,
            DriverManager.FOCUS_DRIVER_ID,
            DriverManager.MENU_DRIVER_ID,
            DriverManager.MENU_DRIVER_ID,
            DriverManager.ORDEREDLIST_DRIVER_ID},
                new Object[]{
                    new JTreeAPIDriver(),
                    new JTreeAPIDriver(),
                    new JTreeAPIDriver(),
                    new AWTTextKeyboardDriver(),
                    new SwingTextKeyboardDriver(),
                    new ScrollbarDriver(),
                    new ScrollPaneDriver(),
                    new JScrollBarAPIDriver(),
                    new JSplitPaneDriver(),
                    new JSliderAPIDriver(),
                    createSpinnerDriver(),
                    new ButtonMouseDriver(),
                    new JTabAPIDriver(),
                    new ListKeyboardDriver(),
                    new ListKeyboardDriver(),
                    new JComboMouseDriver(),
                    new JListMouseDriver(),
                    new JListMouseDriver(),
                    new JTableMouseDriver(),
                    new ChoiceDriver(),
                    new DefaultFrameDriver(),
                    new DefaultWindowDriver(),
                    new InternalFrameAPIDriver(),
                    new InternalFrameAPIDriver(),
                    new InternalFrameAPIDriver(),
                    new APIFocusDriver(),
                    new MouseFocusDriver(),
                    (shortcutEvents ? new QueueJMenuDriver() : new DefaultJMenuDriver()),
                    ((System.getProperty("apple.laf.useScreenMenuBar") != null
                    && System.getProperty("apple.laf.useScreenMenuBar").equals("true")) ? new AppleMenuDriver()
                    : (shortcutEvents ? new QueueJMenuDriver() : new DefaultJMenuDriver())),
                    new JTableHeaderDriver()});
    }

    /**
     * Constructs a DefaultDriverInstaller object with shortcut mode flag taken
     * from {@code JemmyProperties}.
     */
    public APIDriverInstaller() {
        this((JemmyProperties.getCurrentDispatchingModel()
                & JemmyProperties.SHORTCUT_MODEL_MASK) != 0);
    }

    private static LightDriver createSpinnerDriver() {
        if (System.getProperty("java.specification.version").compareTo("1.3") > 0) {
            try {
                return ((LightDriver) new ClassReference("org.netbeans.jemmy.drivers.scrolling.JSpinnerDriver").
                        newInstance(null, null));
            } catch (ClassNotFoundException e) {
                JemmyProperties.getCurrentOutput().
                        printErrLine("ATTENTION! you are using Jemmy built by Java earlier then 1.4, under "
                                + "Java 1.4. \nImpossible to create JSpinnerDriver");
                return createEmptyDriver();
            } catch (Exception e) {
                throw (new JemmyException("Impossible to create JSpinnerDriver although java version is "
                        + System.getProperty("java.version"),
                        e));
            }
        } else {
            return createEmptyDriver();
        }
    }

    private static LightDriver createEmptyDriver() {
        return (new LightDriver() {
            @Override
            public String[] getSupported() {
                return new String[]{Object.class.getName()};
            }
        });
    }
}
