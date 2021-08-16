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
package org.netbeans.jemmy.drivers.text;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import org.netbeans.jemmy.CharBindingMap;
import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.Timeout;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.KeyDriver;
import org.netbeans.jemmy.drivers.LightSupportiveDriver;
import org.netbeans.jemmy.drivers.TextDriver;
import org.netbeans.jemmy.operators.ComponentOperator;

/**
 * Superclass for all TextDrivers using keyboard.
 *
 * @author Alexandre Iline(alexandre.iline@oracle.com)
 */
public abstract class TextKeyboardDriver extends LightSupportiveDriver implements TextDriver {

    /**
     * Constructs a TextKeyboardDriver.
     *
     * @param supported an array of supported class names
     */
    public TextKeyboardDriver(String[] supported) {
        super(supported);
    }

    @Override
    public void changeCaretPosition(ComponentOperator oper, int position) {
        DriverManager.getFocusDriver(oper).giveFocus(oper);
        checkSupported(oper);
        changeCaretPosition(oper, position, 0);
    }

    @Override
    public void selectText(ComponentOperator oper, int startPosition, int finalPosition) {
        changeCaretPosition(oper, startPosition);
        DriverManager.getKeyDriver(oper).pressKey(oper, KeyEvent.VK_SHIFT, 0);
        changeCaretPosition(oper, finalPosition, InputEvent.SHIFT_MASK);
        DriverManager.getKeyDriver(oper).releaseKey(oper, KeyEvent.VK_SHIFT, 0);
    }

    @Override
    public void clearText(ComponentOperator oper) {
        DriverManager.getFocusDriver(oper).giveFocus(oper);
        checkSupported(oper);
        KeyDriver kdriver = DriverManager.getKeyDriver(oper);
        Timeout pushTime = oper.getTimeouts().create("ComponentOperator.PushKeyTimeout");
        Timeout betweenTime = getBetweenTimeout(oper);
        while (getCaretPosition(oper) > 0) {
            kdriver.typeKey(oper, KeyEvent.VK_BACK_SPACE, (char) KeyEvent.VK_BACK_SPACE, 0, pushTime);
            betweenTime.sleep();
        }
        while (getText(oper).length() > 0) {
            kdriver.pushKey(oper, KeyEvent.VK_DELETE, 0, pushTime);
            betweenTime.sleep();
        }
    }

    @Override
    public void typeText(ComponentOperator oper, String text, int caretPosition) {
        changeCaretPosition(oper, caretPosition);
        KeyDriver kDriver = DriverManager.getKeyDriver(oper);
        CharBindingMap map = oper.getCharBindingMap();
        Timeout pushTime = oper.getTimeouts().create("ComponentOperator.PushKeyTimeout");
        Timeout betweenTime = getBetweenTimeout(oper);
        char[] crs = text.toCharArray();
        for (char cr : crs) {
            kDriver.typeKey(oper, map.getCharKey(cr), cr, map.getCharModifiers(cr), pushTime);
            betweenTime.sleep();
        }
    }

    @Override
    public void changeText(ComponentOperator oper, String text) {
        clearText(oper);
        typeText(oper, text, 0);
    }

    @Override
    public void enterText(ComponentOperator oper, String text) {
        changeText(oper, text);
        DriverManager.getKeyDriver(oper).pushKey(oper, KeyEvent.VK_ENTER, 0,
                new Timeout("", 0));
    }

    /**
     * Returns operator's text.
     *
     * @param oper an operator.
     * @return string representing component text.
     */
    public abstract String getText(ComponentOperator oper);

    /**
     * Returns current caret position.
     *
     * @param oper an operator.
     * @return int represnting current operator's caret position.
     */
    public abstract int getCaretPosition(ComponentOperator oper);

    /**
     * Returns a caret position of selection start.
     *
     * @param oper an operator.
     * @return int represnting index of operator's selection start.
     */
    public abstract int getSelectionStart(ComponentOperator oper);

    /**
     * Returns a caret position of selection end.
     *
     * @param oper an operator.
     * @return int represnting index of operator's selection end.
     */
    public abstract int getSelectionEnd(ComponentOperator oper);

    /**
     * Returns an array of navigation keys.
     *
     * @param oper an operator.
     * @return an array on NavigationKey instances.
     */
    public abstract NavigationKey[] getKeys(ComponentOperator oper);

    /**
     * Returns a timeout to sleep between text typing and caret operations.
     *
     * @param oper an operator.
     * @return a Timeout instance.
     */
    public abstract Timeout getBetweenTimeout(ComponentOperator oper);

    /**
     * Changes current caret position to specifyed.
     *
     * @param oper an operator.
     * @param position new caret position
     * @param preModifiers a modifiers (combination of
     * {@code InputEvent.*_MASK} fields) pushed before caret moving (like
     * shift during text selection).
     */
    protected void changeCaretPosition(ComponentOperator oper, final int position, final int preModifiers) {
        NavigationKey[] keys = getKeys(oper);
        for (int i = keys.length - 1; i >= 0; i--) {
            if (keys[i] instanceof OffsetKey) {
                moveCaret(oper, (OffsetKey) keys[i], position, preModifiers);
            } else {
                moveCaret(oper, (GoAndBackKey) keys[i], position, preModifiers);
            }
        }
    }

    private int difference(int one, int two) {
        if (one >= two) {
            return one - two;
        } else {
            return two - one;
        }
    }

    private void push(ComponentOperator oper, NavigationKey key, int preModifiers) {
        DriverManager.getKeyDriver(oper).
                pushKey(oper, key.getKeyCode(), key.getModifiers() | preModifiers,
                        oper.getTimeouts().create("ComponentOperator.PushKeyTimeout"));
        getBetweenTimeout(oper).sleep();
    }

    private final void moveCaret(ComponentOperator oper, GoAndBackKey key, int position, int preModifiers) {
        int newDiff = difference(position, getCaretPosition(oper));
        int oldDiff = newDiff;
        QueueTool qTool = new QueueTool();
        qTool.setOutput(oper.getOutput().createErrorOutput());
        while (key.getDirection() * (position - getCaretPosition(oper)) > 0) {
            oldDiff = newDiff;
            push(oper, key, preModifiers);
            qTool.waitEmpty();
            newDiff = difference(position, getCaretPosition(oper));
            if (newDiff == oldDiff) {
                return;
            }
        }
        if (newDiff > oldDiff) {
            push(oper, key.getBackKey(), preModifiers);
        }
    }

    private final void moveCaret(ComponentOperator oper, OffsetKey key, int position, int preModifiers) {
        if (gotToGo(oper, position, key.getExpectedPosition())) {
            push(oper, key, preModifiers);
        }
    }

    private boolean gotToGo(ComponentOperator oper, int point, int offset) {
        return difference(point, offset) < difference(point, getCaretPosition(oper));
    }
}
