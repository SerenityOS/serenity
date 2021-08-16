/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
   @bug 7160604
   @summary Using non-opaque windows - popups are initially not painted correctly
   @author Oleg Pekhovskiy
   @run applet/manual=yesno bug7160604.html
*/

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JApplet;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import static java.awt.GraphicsDevice.WindowTranslucency.*;

public class bug7160604 extends JApplet {

    public void init() {
        SwingUtilities.invokeLater(() -> {
            if (!GraphicsEnvironment
                    .getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice()
                    .isWindowTranslucencySupported(PERPIXEL_TRANSLUCENT)) {
                // Tested translucency is not supported. Test passed
                return;
            }

            final JWindow window = new JWindow();
            window.setLocation(200, 200);
            window.setSize(300, 300);

            final JLabel label = new JLabel("...click to invoke JPopupMenu");
            label.setOpaque(true);
            final JPanel contentPane = new JPanel(new BorderLayout());
            contentPane.setBorder(BorderFactory.createLineBorder(Color.RED));
            window.setContentPane(contentPane);
            contentPane.add(label, BorderLayout.NORTH);

            final JComboBox comboBox = new JComboBox(new Object[]{"1", "2", "3", "4"});
            contentPane.add(comboBox, BorderLayout.SOUTH);

            final JPopupMenu jPopupMenu = new JPopupMenu();

            jPopupMenu.add("string");
            jPopupMenu.add(new AbstractAction("action") {
                @Override
                public void actionPerformed(final ActionEvent e) {
                }
            });
            jPopupMenu.add(new JLabel("label"));
            jPopupMenu.add(new JMenuItem("MenuItem"));
            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(final MouseEvent e) {
                    jPopupMenu.show(label, 0, 0);
                }
            });

            window.setBackground(new Color(0, 0, 0, 0));

            window.setVisible(true);
        });
    }
}
