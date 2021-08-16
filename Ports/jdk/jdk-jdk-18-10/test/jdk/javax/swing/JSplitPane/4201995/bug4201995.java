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

/*
 * @test
 * @bug 4201995
 * @summary Tests that JSplitPane is opaque
 * @author Scott Violet
 */

import javax.swing.*;

public class bug4201995 {
    public static void main(String[] args) throws Exception {
        for (UIManager.LookAndFeelInfo LF :
                UIManager.getInstalledLookAndFeels()) {
            try {
                UIManager.setLookAndFeel(LF.getClassName());
            } catch (UnsupportedLookAndFeelException ignored) {
                System.out.println("Unsupported L&F: " + LF.getClassName());
                continue;
            } catch (ClassNotFoundException | InstantiationException
                     | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Testing L&F: " + LF.getClassName());
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    boolean expectedOpaqueValue =
                        !("Nimbus".equals(UIManager.getLookAndFeel().getName()) ||
                          UIManager.getLookAndFeel().getName().contains("GTK"));
                    JSplitPane sp = new JSplitPane();
                    System.out.println("sp.isOpaque " + sp.isOpaque());

                    if (sp.isOpaque() != expectedOpaqueValue) {
                        throw new RuntimeException("JSplitPane has incorrect default opaque value");
                    }
                }
            });
        }
    }
}
