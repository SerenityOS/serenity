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
package org.netbeans.jemmy.drivers.windows;

import java.awt.Frame;
import java.awt.Window;
import java.awt.event.WindowEvent;

import org.netbeans.jemmy.drivers.FrameDriver;
import org.netbeans.jemmy.drivers.LightSupportiveDriver;
import org.netbeans.jemmy.drivers.input.EventDriver;
import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.operators.FrameOperator;

public class DefaultFrameDriver extends LightSupportiveDriver implements FrameDriver {

    EventDriver eDriver;

    public DefaultFrameDriver() {
        super(new String[]{"org.netbeans.jemmy.operators.FrameOperator"});
        eDriver = new EventDriver();
    }

    @Override
    public void iconify(ComponentOperator oper) {
        checkSupported(oper);
        eDriver.dispatchEvent(oper.getSource(),
                new WindowEvent((Window) oper.getSource(),
                        WindowEvent.WINDOW_ICONIFIED));
        ((FrameOperator) oper).setState(Frame.ICONIFIED);
    }

    @Override
    public void deiconify(ComponentOperator oper) {
        checkSupported(oper);
        eDriver.dispatchEvent(oper.getSource(),
                new WindowEvent((Window) oper.getSource(),
                        WindowEvent.WINDOW_DEICONIFIED));
        ((FrameOperator) oper).setState(Frame.NORMAL);
    }

    /** Maximizes the frame.
     *
     * @param oper Frame operator.
     * @throws UnsupportedOperatorException if operator class name is not in
     *         the list of supported classes names
     */
    @Override
    public void maximize(ComponentOperator oper) {
        checkSupported(oper);
        ((FrameOperator) oper).setExtendedState(Frame.MAXIMIZED_BOTH);
    }

    /** Demaximizes the frame.
     *
     * @param oper Frame operator.
     * @throws UnsupportedOperatorException if operator class name is not in
     *         the list of supported classes names
     */
    @Override
    public void demaximize(ComponentOperator oper) {
        checkSupported(oper);
        ((FrameOperator) oper).setExtendedState(Frame.NORMAL);
    }

}
