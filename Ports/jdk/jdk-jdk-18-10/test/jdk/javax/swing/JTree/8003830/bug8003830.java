/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import javax.swing.JTree;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.TreePath;


/* Originally reported as NetBeans bug 222081.
 *
 * @test
 * @bug 8003830
 * @summary NullPointerException in BasicTreeUI.Actions when getPathBounds returns null
 * @author Jaroslav Tulach
 * @run main bug8003830
 */

public class bug8003830 implements Runnable {
    public static void main(String[] args) throws Exception {
        EventQueue.invokeAndWait(new bug8003830());
    }
    @Override
    public void run() {
        testNPEAtActionsPage();
    }

    public void testNPEAtActionsPage() {
        JTree tree = new JTree();
        BasicTreeUI ui = new NullReturningTreeUI();
        tree.setUI(ui);
        BasicTreeUI.TreePageAction tpa = ui.new TreePageAction(0, "down");
        tpa.actionPerformed(new ActionEvent(tree, 0, ""));
    }

    private static final class NullReturningTreeUI extends BasicTreeUI {
        @Override
        public Rectangle getPathBounds(JTree tree, TreePath path) {
            // the method can return null and callers have to be ready for
            // that. Simulate the case by returning null for unknown reason.
            return null;
        }
    }
}
