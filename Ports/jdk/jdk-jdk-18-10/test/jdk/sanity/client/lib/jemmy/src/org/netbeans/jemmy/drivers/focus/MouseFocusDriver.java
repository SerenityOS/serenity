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
package org.netbeans.jemmy.drivers.focus;

import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.FocusDriver;
import org.netbeans.jemmy.drivers.LightSupportiveDriver;
import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.operators.Operator;

public class MouseFocusDriver extends LightSupportiveDriver implements FocusDriver {

    private QueueTool queueTool;

    public MouseFocusDriver() {
        super(new String[]{
            "org.netbeans.jemmy.operators.JListOperator",
            "org.netbeans.jemmy.operators.JScrollBarOperator",
            "org.netbeans.jemmy.operators.JSliderOperator",
            "org.netbeans.jemmy.operators.JTableOperator",
            "org.netbeans.jemmy.operators.JTextComponentOperator",
            "org.netbeans.jemmy.operators.JTreeOperator",
            "org.netbeans.jemmy.operators.ListOperator",
            "org.netbeans.jemmy.operators.ScrollbarOperator",
            "org.netbeans.jemmy.operators.TextAreaOperator",
            "org.netbeans.jemmy.operators.TextComponentOperator",
            "org.netbeans.jemmy.operators.TextFieldOperator"});
        queueTool = new QueueTool();
    }

    @Override
    public void giveFocus(final ComponentOperator oper) {
        if (!oper.hasFocus()) {
            queueTool.invokeSmoothly(new QueueTool.QueueAction<Void>("Mouse click to get focus") {
                @Override
                public Void launch() {
                    DriverManager.getMouseDriver(oper).
                            clickMouse(oper, oper.getCenterXForClick(), oper.getCenterYForClick(),
                                    1, Operator.getDefaultMouseButton(), 0,
                                    oper.getTimeouts().create("ComponentOperator.MouseClickTimeout"));
                    return null;
                }
            });
            oper.waitHasFocus();
        }
    }
}
