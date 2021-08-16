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

import java.awt.event.KeyEvent;

import org.netbeans.jemmy.Timeout;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.LightSupportiveDriver;
import org.netbeans.jemmy.drivers.TextDriver;
import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.operators.JTextComponentOperator;
import org.netbeans.jemmy.operators.TextComponentOperator;

/**
 * Superclass for all TextDrivers using API calls.
 *
 * @author Alexandre Iline(alexandre.iline@oracle.com)
 */
public abstract class TextAPIDriver extends LightSupportiveDriver implements TextDriver {

    /**
     * Constructs a ChoiceDriver.
     *
     * @param supported an array of supported class names
     */
    public TextAPIDriver(String[] supported) {
        super(supported);
    }

    @Override
    public void changeCaretPosition(ComponentOperator oper, int position) {
        checkSupported(oper);
        if (oper instanceof TextComponentOperator) {
            ((TextComponentOperator) oper).setCaretPosition(position);
        } else {
            ((JTextComponentOperator) oper).setCaretPosition(position);
        }
    }

    @Override
    public void selectText(ComponentOperator oper, int startPosition, int finalPosition) {
        checkSupported(oper);
        int start = (startPosition < finalPosition) ? startPosition : finalPosition;
        int end = (startPosition > finalPosition) ? startPosition : finalPosition;
        if (oper instanceof TextComponentOperator) {
            TextComponentOperator toper = ((TextComponentOperator) oper);
            toper.setSelectionStart(start);
            toper.setSelectionEnd(end);
        } else {
            JTextComponentOperator toper = ((JTextComponentOperator) oper);
            toper.setSelectionStart(start);
            toper.setSelectionEnd(end);
        }
    }

    @Override
    public void clearText(ComponentOperator oper) {
        if (oper instanceof TextComponentOperator) {
            ((TextComponentOperator) oper).setText("");
        } else {
            ((JTextComponentOperator) oper).setText("");
        }
    }

    @Override
    public void typeText(ComponentOperator oper, String text, int caretPosition) {
        checkSupported(oper);
        String curtext = getText(oper);
        int realPos = caretPosition;
        if (getSelectionStart(oper) == realPos
                || getSelectionEnd(oper) == realPos) {
            if (getSelectionEnd(oper) == realPos) {
                realPos = realPos - (getSelectionEnd(oper) - getSelectionStart(oper));
            }
            curtext
                    = curtext.substring(0, getSelectionStart(oper))
                    + curtext.substring(getSelectionEnd(oper));
        }
        changeText(oper,
                curtext.substring(0, realPos) + text
                + curtext.substring(realPos));
    }

    @Override
    public void changeText(ComponentOperator oper, String text) {
        checkSupported(oper);
        if (oper instanceof TextComponentOperator) {
            ((TextComponentOperator) oper).setText(text);
        } else {
            ((JTextComponentOperator) oper).setText(text);
        }
    }

    @Override
    public void enterText(ComponentOperator oper, String text) {
        changeText(oper, text);
        DriverManager.getKeyDriver(oper).
                pushKey(oper, KeyEvent.VK_ENTER, 0,
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
}
