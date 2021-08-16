/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4128979
   @requires (os.family == "windows")
   @summary Tests that background changes correctly in WinLF for JToggleButton when pressed
   @key headful
   @run applet/manual=yesno bug4128979.html
*/

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Graphics;

public class bug4128979 extends JApplet {

    public static void main(String[] args) {
        JApplet applet = new bug4128979();
        applet.init();
        applet.start();

        JFrame frame = new JFrame("Test window");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(applet, BorderLayout.CENTER);
        frame.setSize(600, 240);
        frame.setVisible(true);
    }

    public void init() {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel");
        } catch (UnsupportedLookAndFeelException e) {
            JOptionPane.showMessageDialog(this,
                    "This test requires Windows look and feel, so just press Pass\n as "+
                    " this look and feel is unsupported on this platform.",
                    "Unsupported LF", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (Exception e) {
            throw new RuntimeException("Couldn't set look and feel");
        }

        setLayout(new FlowLayout());
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JPanel p1 = new JPanel();
        addButtons(p1);
        p.add(p1);

        JToolBar tb1 = new JToolBar();
        addButtons(tb1);
        p.add(tb1);

        JToolBar tb2 = new JToolBar();
        tb2.setRollover(true);
        addButtons(tb2);
        p.add(tb2);

        JLabel label = new JLabel("ToggleButton.highlight color: ");
        label.setComponentOrientation( ComponentOrientation.RIGHT_TO_LEFT);
        label.setIcon(new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setColor(UIManager.getColor("ToggleButton.highlight"));
                g.fillRect(x, y, 49, 49);
                g.setColor(new Color(~UIManager.getColor("ToggleButton.highlight").getRGB()));
                g.drawRect(x, y, 49, 49);
            }

            public int getIconWidth() {
                return 50;
            }

            public int getIconHeight() {
                return 50;
            }
        });
        add(p);
        add(label);
    }

    static void addButtons(Container c) {
        c.setLayout(new FlowLayout());

        c.add(new JToggleButton("DefaultBorder"));

        JToggleButton cbut = new JToggleButton("DefaultBorder");
        cbut.setBackground(Color.red);
        c.add(cbut);
        cbut = new JToggleButton("DefaultBorder");
        cbut.setBackground(Color.green);
        c.add(cbut);
        cbut = new JToggleButton("DefaultBorder");
        cbut.setBackground(Color.blue);
        c.add(cbut);

        JToggleButton but3 = new JToggleButton("LineBorder");
        but3.setBorder(BorderFactory.createLineBorder(Color.red));
        c.add(but3);

        JToggleButton but4 = new JToggleButton("null border");
        but4.setBorder(null);
        c.add(but4);
    }
}
