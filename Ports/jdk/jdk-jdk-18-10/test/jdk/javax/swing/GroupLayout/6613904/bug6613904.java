/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6613904
 * @summary javax.swing.GroupLayout.createParallelGroup(..) doesn't throw IllegalArgumentException for null arg
 * @author Pavel Porvatov
 */

import javax.swing.GroupLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class bug6613904 {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                GroupLayout groupLayout = new GroupLayout(new JPanel());

                try {
                    groupLayout.createParallelGroup(null);

                    throw new RuntimeException("groupLayout.createParallelGroup(null) doesn't throw IAE");
                } catch (IllegalArgumentException e) {
                    // Ok
                }

                try {
                    groupLayout.createParallelGroup(null, true);

                    throw new RuntimeException("groupLayout.createParallelGroup(null, true) doesn't throw IAE");
                } catch (IllegalArgumentException e) {
                    // Ok
                }

                try {
                    groupLayout.createParallelGroup(null, false);

                    throw new RuntimeException("groupLayout.createParallelGroup(null, false) doesn't throw IAE");
                } catch (IllegalArgumentException e) {
                    // Ok
                }
            }
        });
    }
}
