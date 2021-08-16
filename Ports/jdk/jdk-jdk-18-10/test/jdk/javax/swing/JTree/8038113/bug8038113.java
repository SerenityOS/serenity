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
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.Icon;
import javax.swing.JApplet;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicTreeUI;

/* @test
 * @bug 8038113
 * @summary [macosx] JTree icon is not rendered in high resolution on Retina
 * @run applet/manual=yesno bug8038113.html
 */
public class bug8038113 extends JApplet {

    @Override
    public void init() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {

                final JTree tree = new JTree();
                final BasicTreeUI treeUI = (BasicTreeUI) tree.getUI();

                final JPanel panel = new JPanel() {

                    @Override
                    public void paint(Graphics g) {
                        super.paint(g);
                        Graphics2D g2 = (Graphics2D) g;
                        g2.setStroke(new BasicStroke(0.5f));
                        g2.scale(2, 2);

                        int x = 10;
                        int y = 10;
                        Icon collapsedIcon = treeUI.getCollapsedIcon();
                        Icon expandeIcon = treeUI.getExpandedIcon();
                        int w = collapsedIcon.getIconWidth();
                        int h = collapsedIcon.getIconHeight();
                        collapsedIcon.paintIcon(this, g, x, y);
                        g.drawRect(x, y, w, h);

                        y += 10 + h;
                        w = expandeIcon.getIconWidth();
                        h = expandeIcon.getIconHeight();
                        expandeIcon.paintIcon(this, g, x, y);
                        g.drawRect(x, y, w, h);

                    }
                };
                getContentPane().setLayout(new BorderLayout());
                getContentPane().add(panel, BorderLayout.CENTER);
            }
        });
    }
}
