/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 6953396
 * @summary javax.swing.plaf.basic.BasicViewportUI.uninstallDefaults() is not called when UI is uninstalled
 * @author Alexander Potochkin
 */

import javax.swing.*;
import javax.swing.plaf.basic.BasicViewportUI;

public class bug6953396 {
    static volatile boolean flag;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                BasicViewportUI ui = new BasicViewportUI() {

                    @Override
                    protected void installDefaults(JComponent c) {
                        super.installDefaults(c);
                        flag = true;
                    }

                    @Override
                    protected void uninstallDefaults(JComponent c) {
                        super.uninstallDefaults(c);
                        flag = false;
                    }
                };

                JViewport viewport = new JViewport();
                viewport.setUI(ui);
                viewport.setUI(null);
            }
        });
        if (flag) {
            throw new RuntimeException("uninstallDefaults() hasn't been called");
        }
    }
}
