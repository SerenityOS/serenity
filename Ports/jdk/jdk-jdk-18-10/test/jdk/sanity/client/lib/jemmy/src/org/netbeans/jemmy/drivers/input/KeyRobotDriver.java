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
package org.netbeans.jemmy.drivers.input;

import org.netbeans.jemmy.Timeout;
import org.netbeans.jemmy.drivers.KeyDriver;
import org.netbeans.jemmy.operators.ComponentOperator;

/**
 * KeyDriver using robot operations.
 *
 * @author Alexandre Iline(alexandre.iline@oracle.com)
 */
public class KeyRobotDriver extends RobotDriver implements KeyDriver {

    /**
     * Constructs a KeyRobotDriver object.
     *
     * @param autoDelay Time for {@code Robot.setAutoDelay(long)} method.
     */
    public KeyRobotDriver(Timeout autoDelay) {
        super(autoDelay);
    }

    /**
     * Constructs a KeyRobotDriver object.
     *
     * @param autoDelay Time for {@code Robot.setAutoDelay(long)} method.
     * @param supported an array of supported class names
     */
    public KeyRobotDriver(Timeout autoDelay, String[] supported) {
        super(autoDelay, supported);
    }

    @Override
    public void pushKey(ComponentOperator oper, int keyCode, int modifiers, Timeout pushTime) {
        pressKey(oper, keyCode, modifiers);
        pushTime.sleep();
        releaseKey(oper, keyCode, modifiers);
    }

    @Override
    public void typeKey(ComponentOperator oper, int keyCode, char keyChar, int modifiers, Timeout pushTime) {
        pushKey(oper, keyCode, modifiers, pushTime);
    }

    /**
     * Presses a key.
     *
     * @param oper Operator to press a key on.
     * @param keyCode Key code ({@code KeyEventVK_*} field.
     * @param modifiers a combination of {@code InputEvent.*_MASK} fields.
     */
    @Override
    public void pressKey(ComponentOperator oper, int keyCode, int modifiers) {
        pressKey(keyCode, modifiers);
    }

    @Override
    public void typedKey(ComponentOperator oper, int keyCode, char keyChar, int modifiers) {
        releaseKey(oper, keyCode, modifiers);
    }

    /**
     * Releases a key.
     *
     * @param oper Operator to release a key on.
     * @param keyCode Key code ({@code KeyEventVK_*} field.
     * @param modifiers a combination of {@code InputEvent.*_MASK} fields.
     */
    @Override
    public void releaseKey(ComponentOperator oper, int keyCode, int modifiers) {
        releaseKey(keyCode, modifiers);
    }
}
