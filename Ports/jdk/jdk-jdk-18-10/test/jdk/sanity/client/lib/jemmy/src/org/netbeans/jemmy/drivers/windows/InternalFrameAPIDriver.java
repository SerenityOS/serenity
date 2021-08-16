/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.jemmy.drivers.windows;

import java.awt.Component;

import org.netbeans.jemmy.drivers.FrameDriver;
import org.netbeans.jemmy.drivers.InternalFrameDriver;
import org.netbeans.jemmy.drivers.LightSupportiveDriver;
import org.netbeans.jemmy.drivers.WindowDriver;
import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.operators.JInternalFrameOperator;

/**
 * InternalFrameDriver to do all actions using internal frame APIs.
 *
 * Note: There is no API to get title component, so this driver throws
 *       UnsupportedOperationException for all title component related APIs.
 */
public class InternalFrameAPIDriver extends LightSupportiveDriver
        implements WindowDriver, FrameDriver, InternalFrameDriver {

    public InternalFrameAPIDriver() {
        super(new String[]{"org.netbeans.jemmy.operators.JInternalFrameOperator"});
    }

    @Override
    public void activate(ComponentOperator oper) {
        checkSupported(oper);
        ((JInternalFrameOperator) oper).moveToFront();
        ((JInternalFrameOperator) oper).setSelected(true);
    }

    @Override
    public void maximize(ComponentOperator oper) {
        checkSupported(oper);
        if (!((JInternalFrameOperator) oper).isSelected()) {
            activate(oper);
        }
        ((JInternalFrameOperator) oper).setMaximum(true);
    }

    @Override
    public void demaximize(ComponentOperator oper) {
        checkSupported(oper);
        if (!((JInternalFrameOperator) oper).isSelected()) {
            activate(oper);
        }
        ((JInternalFrameOperator) oper).setMaximum(false);
    }

    @Override
    public void iconify(ComponentOperator oper) {
        checkSupported(oper);
        ((JInternalFrameOperator) oper).setIcon(true);
    }

    @Override
    public void deiconify(ComponentOperator oper) {
        checkSupported(oper);
        ((JInternalFrameOperator) oper).setIcon(false);
    }

    @Override
    public void requestClose(ComponentOperator oper) {
        checkSupported(oper);
        ((JInternalFrameOperator) oper).setClosed(true);
    }

    @Override
    public void move(ComponentOperator oper, int x, int y) {
        checkSupported(oper);
        oper.setLocation(x, y);
    }

    @Override
    public void resize(ComponentOperator oper, int width, int height) {
        checkSupported(oper);
        oper.setSize(width, height);
    }

    @Override
    public Component getTitlePane(ComponentOperator oper) {
        throw new UnsupportedOperationException(
                "There is no way to get title pane of an internal frame.");
    }

    @Override
    public void requestCloseAndThenHide(ComponentOperator oper) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void close(ComponentOperator oper) {
        requestClose(oper);
    }
}
