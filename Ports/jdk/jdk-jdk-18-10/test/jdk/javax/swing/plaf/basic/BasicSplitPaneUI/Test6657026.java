/*
 * Copyright (c) 2009, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/*
 * @test
 * @bug 6657026 7190595
 * @summary Tests shared BasicSplitPaneUI in different application contexts
 * @author Sergey Malenkov
 * @modules java.desktop/sun.awt
 */

import sun.awt.SunToolkit;

import java.awt.event.ActionEvent;
import java.util.Set;
import javax.swing.JSplitPane;
import javax.swing.plaf.basic.BasicSplitPaneUI;

public class Test6657026 extends BasicSplitPaneUI implements Runnable {

    public static void main(String[] args) throws InterruptedException {
        if (new JSplitPane().getFocusTraversalKeys(0).isEmpty()){
            throw new Error("unexpected traversal keys");
        }
        new JSplitPane() {
            public void setFocusTraversalKeys(int id, Set keystrokes) {
                keystrokes.clear();
                super.setFocusTraversalKeys(id, keystrokes);
            }
        };
        if (new JSplitPane().getFocusTraversalKeys(0).isEmpty()) {
            throw new Error("shared traversal keys");
        }
        KEYBOARD_DIVIDER_MOVE_OFFSET = -KEYBOARD_DIVIDER_MOVE_OFFSET;

        ThreadGroup group = new ThreadGroup("$$$");
        Thread thread = new Thread(group, new Test6657026());
        thread.start();
        thread.join();
    }

    public void run() {
        SunToolkit.createNewAppContext();
        if (new JSplitPane().getFocusTraversalKeys(0).isEmpty()) {
            throw new Error("shared traversal keys");
        }
        JSplitPane pane = new JSplitPane();
        pane.setUI(this);

        createFocusListener().focusGained(null); // allows actions
        test(pane, "positiveIncrement", 3);
        test(pane, "negativeIncrement", 0);
    }

    private static void test(JSplitPane pane, String action, int expected) {
        ActionEvent event = new ActionEvent(pane, expected, action);
        pane.getActionMap().get(action).actionPerformed(event);
        int actual = pane.getDividerLocation();
        if (actual != expected) {
            throw new Error(actual + ", but expected " + expected);
        }
    }
}
