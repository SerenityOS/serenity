/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
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

import org.netbeans.jemmy.EventDispatcher;
import org.netbeans.jemmy.JemmyException;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.Timeout;
import org.netbeans.jemmy.drivers.input.KeyEventDriver;
import org.netbeans.jemmy.drivers.input.KeyRobotDriver;
import org.netbeans.jemmy.drivers.input.MouseEventDriver;
import org.netbeans.jemmy.drivers.input.MouseRobotDriver;

/**
 * Installs drivers for low-level drivers.
 *
 * @author Alexandre Iline(alexandre.iline@oracle.com)
 */
public class InputDriverInstaller {

    Timeout robotAutoDelay;
    boolean useEventDrivers;
    boolean smooth = false;

    /**
     * Constructs an InputDriverInstaller object.
     *
     * @param useEventDrivers Tells whether to use event drivers, otherwise
     * robot drivers.
     * @param robotAutoDelay Time for {@code Robot.setAutoDelay(long)}
     * method.
     */
    public InputDriverInstaller(boolean useEventDrivers, Timeout robotAutoDelay) {
        this.robotAutoDelay = robotAutoDelay;
        this.useEventDrivers = useEventDrivers;
    }

    /**
     * Constructs an InputDriverInstaller object. Takes autodelay time from
     * JemmyProperties' timeouts.
     *
     * @param useEventDrivers Tells whether to use event drivers, otherwise
     * robot drivers.
     */
    public InputDriverInstaller(boolean useEventDrivers) {
        this(useEventDrivers,
                JemmyProperties.getCurrentTimeouts().
                create("EventDispatcher.RobotAutoDelay"));
    }

    /**
     * Constructs an InputDriverInstaller object. Takes autodelay time from
     * JemmyProperties' timeouts.
     *
     * @param useEventDrivers Tells whether to use event drivers, otherwise
     * robot drivers.
     * @param smooth whether to move mouse smoothly.
     */
    public InputDriverInstaller(boolean useEventDrivers, boolean smooth) {
        this(useEventDrivers);
        this.smooth = smooth;
    }

    /**
     * Constructs an InputDriverInstaller object. Uses event drivers.
     *
     * @param robotAutoDelay Time for {@code Robot.setAutoDelay(long)}
     * method.
     */
    public InputDriverInstaller(Timeout robotAutoDelay) {
        this(true,
                robotAutoDelay);
    }

    /**
     * Constructs an InputDriverInstaller object. Takes autodelay time from
     * JemmyProperties' timeouts. Uses event drivers.
     */
    public InputDriverInstaller() {
        this(true);
    }

    static {
        EventDispatcher.performInit();
    }

    /**
     * Installs input drivers.
     */
    public void install() {
        if (useEventDrivers) {
            LightDriver keyE = new KeyEventDriver();
            LightDriver mouseE = new MouseEventDriver();
            DriverManager.removeDriver(DriverManager.KEY_DRIVER_ID,
                    keyE.getSupported());
            DriverManager.removeDriver(DriverManager.MOUSE_DRIVER_ID,
                    mouseE.getSupported());
            DriverManager.setDriver(DriverManager.KEY_DRIVER_ID, keyE);
            DriverManager.setDriver(DriverManager.MOUSE_DRIVER_ID, mouseE);
            try {
                String[] awtOperators
                        = {
                            "org.netbeans.jemmy.operators.ButtonOperator",
                            "org.netbeans.jemmy.operators.CheckboxOperator",
                            "org.netbeans.jemmy.operators.ChoiceOperator",
                            "org.netbeans.jemmy.operators.LabelOperator",
                            "org.netbeans.jemmy.operators.ListOperator",
                            "org.netbeans.jemmy.operators.ScrollPaneOperator",
                            "org.netbeans.jemmy.operators.ScrollbarOperator",
                            "org.netbeans.jemmy.operators.TextAreaOperator",
                            "org.netbeans.jemmy.operators.TextComponentOperator",
                            "org.netbeans.jemmy.operators.TextFieldOperator"
                        };
                LightDriver keyR = new KeyRobotDriver(robotAutoDelay, awtOperators);
                LightDriver mouseR = new MouseRobotDriver(robotAutoDelay, awtOperators);
                DriverManager.removeDriver(DriverManager.KEY_DRIVER_ID,
                        keyR.getSupported());
                DriverManager.removeDriver(DriverManager.MOUSE_DRIVER_ID,
                        mouseR.getSupported());
                DriverManager.setDriver(DriverManager.KEY_DRIVER_ID, keyR);
                DriverManager.setDriver(DriverManager.MOUSE_DRIVER_ID, mouseR);
            } catch (JemmyException e) {
                if (!(e.getInnerThrowable() instanceof ClassNotFoundException)) {
                    throw (e);
                }
            }
        } else {
            LightDriver keyR = new KeyRobotDriver(robotAutoDelay);
            LightDriver mouseR = new MouseRobotDriver(robotAutoDelay, smooth);
            DriverManager.removeDriver(DriverManager.KEY_DRIVER_ID,
                    keyR.getSupported());
            DriverManager.removeDriver(DriverManager.MOUSE_DRIVER_ID,
                    mouseR.getSupported());
            DriverManager.setDriver(DriverManager.KEY_DRIVER_ID, keyR);
            DriverManager.setDriver(DriverManager.MOUSE_DRIVER_ID, mouseR);
        }
    }
}
