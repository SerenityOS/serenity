/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
   @bug 6456844
   @summary Tests that JTextComponent doesn't create drop locations with null bias.
   @author Shannon Hickey
   @modules java.desktop/sun.swing
*/

import sun.swing.SwingAccessor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;

public class bug6456844 {

    public static void main(String[] args) {
        JEditorPane ep = new JEditorPane();
        ep.setContentType("text/html");
        ep.setText("<html><body>abc</body></html>");
        ep.setBorder(new EmptyBorder(20, 20, 20, 20));
        ep.setBounds(0, 0, 100, 100);

        JTextComponent.DropLocation location =
                (JTextComponent.DropLocation) SwingAccessor.getJTextComponentAccessor().dropLocationForPoint(ep,
                        new Point(0, 0));

        if (location.getBias() == null) {
            throw new RuntimeException("null bias");
        }
    }
}
