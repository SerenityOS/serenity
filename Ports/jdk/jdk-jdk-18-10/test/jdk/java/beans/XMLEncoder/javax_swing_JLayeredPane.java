/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5023552 6972468
 * @summary Tests JLayeredPane encoding
 * @run main/othervm -Djava.security.manager=allow javax_swing_JLayeredPane
 * @author Sergey Malenkov
 */

import java.awt.Color;

import javax.swing.JLayeredPane;
import javax.swing.JPanel;

public final class javax_swing_JLayeredPane extends AbstractTest<JLayeredPane> {
    public static void main(String[] args) {
        new javax_swing_JLayeredPane().test(true);
    }

    private static void init(JLayeredPane pane, int layer, int x, int y, int w, int h, Color color) {
        JPanel panel = new JPanel();
        panel.setBackground(color);
        panel.setLocation(x, y);
        panel.setSize(w, h);
        pane.add(panel, new Integer(layer));
    }

    protected JLayeredPane getObject() {
        JLayeredPane pane = new JLayeredPane();
        init(pane, 0, 25, 25, 50, 50, Color.RED);
        init(pane, 1, 10, 10, 50, 50, Color.BLUE);
        init(pane, 2, 40, 40, 50, 50, Color.YELLOW);
        pane.setSize(200, 200);
        return pane;
    }

    protected JLayeredPane getAnotherObject() {
        return null; // TODO: could not update property
        // return new JLayeredPane();
    }
}
