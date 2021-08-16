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
package org.netbeans.jemmy.drivers.lists;

import javax.swing.UIManager;

import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.LightSupportiveDriver;
import org.netbeans.jemmy.drivers.ListDriver;
import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.operators.JComboBoxOperator;
import org.netbeans.jemmy.operators.JListOperator;
import org.netbeans.jemmy.util.EmptyVisualizer;

/**
 * List driver for javax.swing.JCompoBox component type.
 *
 * @author Alexandre Iline(alexandre.iline@oracle.com)
 */
public class JComboMouseDriver extends LightSupportiveDriver implements ListDriver {

    /**
     * Constructs a JComboMouseDriver.
     */
    QueueTool queueTool;

    public JComboMouseDriver() {
        super(new String[]{"org.netbeans.jemmy.operators.JComboBoxOperator"});
        queueTool = new QueueTool();
    }

    @Override
    public void selectItem(ComponentOperator oper, int index) {
        JComboBoxOperator coper = (JComboBoxOperator) oper;
        //1.5 workaround
        if (System.getProperty("java.specification.version").compareTo("1.4") > 0) {
            queueTool.setOutput(oper.getOutput().createErrorOutput());
            queueTool.waitEmpty(10);
            queueTool.waitEmpty(10);
            queueTool.waitEmpty(10);
        }
        //end of 1.5 workaround
        if (!coper.isPopupVisible()) {
            if (UIManager.getLookAndFeel().getClass().getName().equals("com.sun.java.swing.plaf.motif.MotifLookAndFeel")) {
                oper.clickMouse(oper.getWidth() - 2, oper.getHeight() / 2, 1);
            } else {
                DriverManager.getButtonDriver(coper.getButton()).
                        push(coper.getButton());
            }
        }
        JListOperator list = new JListOperator(coper.waitList());
        list.copyEnvironment(coper);
        list.setVisualizer(new EmptyVisualizer());
        coper.getTimeouts().sleep("JComboBoxOperator.BeforeSelectingTimeout");
        DriverManager.getListDriver(list).
                selectItem(list, index);
    }
}
