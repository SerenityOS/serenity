/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Setting a border on a JLayer causes an Exceptions
 * @author Alexander Potochkin
 * @run main bug8054543
 */

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class bug8054543 {

    public bug8054543() {
        JLayer<JComponent> layer = new JLayer<>();
        Border border = BorderFactory.createLineBorder(Color.GREEN);
        JButton view = new JButton("JButton");

        layer.setBorder(border);
        check(layer.getBorder(), null);

        layer.setBorder(null);
        check(layer.getBorder(), null);

        layer.setView(view);
        check(layer.getBorder(), view.getBorder());

        layer.setBorder(border);
        check(border, view.getBorder());

        layer.setBorder(null);
        check(layer.getBorder(), view.getBorder());
    }

    private void check(Object o1, Object o2) {
        if (o1 != o2) {
            throw new RuntimeException("Test failed");
        }
    }

    public static void main(String... args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                new bug8054543();
            }
        });
    }
}
