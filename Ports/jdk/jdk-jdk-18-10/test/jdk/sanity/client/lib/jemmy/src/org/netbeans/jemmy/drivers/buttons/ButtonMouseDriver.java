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
package org.netbeans.jemmy.drivers.buttons;

import org.netbeans.jemmy.drivers.ButtonDriver;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.LightSupportiveDriver;
import org.netbeans.jemmy.drivers.MouseDriver;
import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.operators.Operator;

/**
 * Driver to push a button by mouse click.
 *
 * @author Alexandre Iline(alexandre.iline@oracle.com)
 */
public class ButtonMouseDriver extends LightSupportiveDriver implements ButtonDriver {

    public ButtonMouseDriver() {
        super(new String[]{"org.netbeans.jemmy.operators.ComponentOperator"});
    }

    @Override
    public void press(ComponentOperator oper) {
        MouseDriver mdriver = DriverManager.getMouseDriver(oper);
        mdriver.moveMouse(oper,
                oper.getCenterXForClick(),
                oper.getCenterYForClick());
        mdriver.pressMouse(oper,
                oper.getCenterXForClick(),
                oper.getCenterYForClick(),
                Operator.getDefaultMouseButton(),
                0);
    }

    @Override
    public void release(ComponentOperator oper) {
        DriverManager.
                getMouseDriver(oper).
                releaseMouse(oper,
                        oper.getCenterXForClick(),
                        oper.getCenterYForClick(),
                        Operator.getDefaultMouseButton(),
                        0);
    }

    @Override
    public void push(ComponentOperator oper) {
        DriverManager.
                getMouseDriver(oper).
                clickMouse(oper,
                        oper.getCenterXForClick(),
                        oper.getCenterYForClick(),
                        1,
                        Operator.getDefaultMouseButton(),
                        0,
                        oper.getTimeouts().
                        create("ComponentOperator.MouseClickTimeout"));
    }
}
