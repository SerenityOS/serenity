/*
 * Copyright (c) 2003, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4903007 6972468
 * @summary Tests encoding of container with boxes and BoxLayout
 * @run main/othervm -Djava.security.manager=allow Test4903007
 * @author Sergey Malenkov, Mark Davidson
 */

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class Test4903007 extends AbstractTest<JPanel> {
    public static void main(String[] args) throws Exception {
        new Test4903007().test(true);
    }

    protected JPanel getObject() {
        Box vBox = Box.createVerticalBox();
        vBox.add(new JButton("button"));
        vBox.add(Box.createVerticalStrut(10));
        vBox.add(new JLabel("label"));
        vBox.add(Box.createVerticalGlue());
        vBox.add(new JButton("button"));
        vBox.add(Box.createVerticalStrut(10));
        vBox.add(new JLabel("label"));

        Box hBox = Box.createHorizontalBox();
        hBox.add(new JButton("button"));
        hBox.add(Box.createHorizontalStrut(10));
        hBox.add(new JLabel("label"));
        hBox.add(Box.createHorizontalGlue());
        hBox.add(new JButton("button"));
        hBox.add(Box.createHorizontalStrut(10));
        hBox.add(new JLabel("label"));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(vBox);
        panel.add(Box.createGlue());
        panel.add(hBox);
        return panel;
    }

    protected JPanel getAnotherObject() {
        return null; // TODO: could not update property
        // return new JPanel();
    }
}
