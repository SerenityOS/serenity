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

import org.netbeans.jemmy.Timeout;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.KeyDriver;
import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.operators.JEditorPaneOperator;
import org.netbeans.jemmy.operators.JTextAreaOperator;
import org.netbeans.jemmy.operators.JTextComponentOperator;

/**
 * TextDriver for swing text component types. Uses keyboard operations.
 *
 * @author Alexandre Iline(alexandre.iline@oracle.com)
 */
public class SwingTextKeyboardDriver extends TextKeyboardDriver {

    /**
     * Constructs a SwingTextKeyboardDriver.
     */
    public SwingTextKeyboardDriver() {
        super(new String[]{"org.netbeans.jemmy.operators.JTextComponentOperator"});
    }

    @Override
    public void clearText(ComponentOperator oper) {
        if (oper instanceof JTextAreaOperator
                || oper instanceof JEditorPaneOperator) {
            DriverManager.getFocusDriver(oper).giveFocus(oper);
            KeyDriver kdriver = DriverManager.getKeyDriver(oper);
            selectText(oper, 0, getText(oper).length());
            kdriver.pushKey(oper, KeyEvent.VK_DELETE, 0,
                    oper.getTimeouts().create("ComponentOperator.PushKeyTimeout"));
        } else {
            super.clearText(oper);
        }
    }

    @Override
    public String getText(ComponentOperator oper) {
        return ((JTextComponentOperator) oper).getDisplayedText();
    }

    @Override
    public int getCaretPosition(ComponentOperator oper) {
        return ((JTextComponentOperator) oper).getCaretPosition();
    }

    @Override
    public int getSelectionStart(ComponentOperator oper) {
        return ((JTextComponentOperator) oper).getSelectionStart();
    }

    @Override
    public int getSelectionEnd(ComponentOperator oper) {
        return ((JTextComponentOperator) oper).getSelectionEnd();
    }

    @Override
    public NavigationKey[] getKeys(ComponentOperator oper) {
        boolean multiString
                = oper instanceof JTextAreaOperator
                || oper instanceof JEditorPaneOperator;
        NavigationKey[] result = new NavigationKey[multiString ? 8 : 4];
        result[0] = new UpKey(KeyEvent.VK_LEFT, 0);
        result[1] = new DownKey(KeyEvent.VK_RIGHT, 0);
        ((UpKey) result[0]).setDownKey((DownKey) result[1]);
        ((DownKey) result[1]).setUpKey((UpKey) result[0]);
        if (multiString) {
            result[2] = new UpKey(KeyEvent.VK_UP, 0);
            result[3] = new DownKey(KeyEvent.VK_DOWN, 0);
            ((UpKey) result[2]).setDownKey((DownKey) result[3]);
            ((DownKey) result[3]).setUpKey((UpKey) result[2]);
            result[4] = new UpKey(KeyEvent.VK_PAGE_UP, 0);
            result[5] = new DownKey(KeyEvent.VK_PAGE_DOWN, 0);
            ((UpKey) result[4]).setDownKey((DownKey) result[5]);
            ((DownKey) result[5]).setUpKey((UpKey) result[4]);
            result[6] = new HomeKey(KeyEvent.VK_HOME, InputEvent.CTRL_MASK);
            result[7] = new EndKey(KeyEvent.VK_END, InputEvent.CTRL_MASK, this, oper);
        } else {
            result[2] = new HomeKey(KeyEvent.VK_HOME, 0);
            result[3] = new EndKey(KeyEvent.VK_END, 0, this, oper);
        }
        return result;
    }

    @Override
    public Timeout getBetweenTimeout(ComponentOperator oper) {
        return oper.getTimeouts().create("TextComponentOperator.BetweenKeysTimeout");
    }
}
