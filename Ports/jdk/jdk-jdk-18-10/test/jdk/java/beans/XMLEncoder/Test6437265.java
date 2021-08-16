/*
 * Copyright (c) 2006, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6437265
 * @summary Tests encoding of container with BorderLayout
 * @run main/othervm -Djava.security.manager=allow Test6437265
 * @author Sergey Malenkov
 */

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JPanel;

public final class Test6437265 extends AbstractTest<JPanel> {
    private static final String[] NAMES = {
            BorderLayout.EAST,
            BorderLayout.WEST,
            BorderLayout.NORTH,
            BorderLayout.SOUTH,
            BorderLayout.CENTER,
            BorderLayout.LINE_END,
            BorderLayout.PAGE_END,
            BorderLayout.LINE_START,
            BorderLayout.PAGE_START};

    public static void main(String[] args) {
        new Test6437265().test(true);
    }

    protected JPanel getObject() {
        JPanel panel = new MyPanel();
        for (String name : NAMES) {
            panel.add(name, new JLabel(name));
        }
        return panel;
    }

    protected void validate(JPanel before, JPanel after) {
        validate(before);
        validate(after);
        super.validate(before, after);
    }

    private static void validate(JPanel panel) {
        BorderLayout layout = (BorderLayout) panel.getLayout();
        for (Component component : panel.getComponents()) {
            String name = (String) layout.getConstraints(component);
            if (name == null)
                throw new Error("The component is not layed out: " + component);

            JLabel label = (JLabel) component;
            if (!name.equals(label.getText()))
                throw new Error("The component is layed out on " + name + ": " + component);
        }
    }

    public static final class MyPanel extends JPanel {
        public MyPanel() {
            // the bug is reproducible for containers
            // that uses BorderLayout as default layout manager.
            super(new BorderLayout(3, 3));
        }
    }
}
