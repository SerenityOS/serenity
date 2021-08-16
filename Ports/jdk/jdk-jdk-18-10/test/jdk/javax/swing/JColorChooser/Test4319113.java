/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4319113
 * @summary Tests the open JColorChooser behavior on LaF change.
 * @author yan
 * @run applet/manual=yesno Test4319113.html
 */


import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.PrintStream;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Test4319113
extends JApplet
implements ActionListener {
    private final JFrame frame = new JFrame("frame");
    private JComboBox cbPlaf;

    @Override
    public void init() {
        try {
            java.awt.EventQueue.invokeLater( () -> {
                Test4319113.this.frame.setLayout(new GridLayout(2, 1));
                Test4319113.this.show(Test4319113.this.frame);
            });
        }catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Object object = actionEvent.getSource();
        Component component = object instanceof Component ? (Component)object : null;
        JDialog jDialog = JColorChooser.createDialog(component, "ColorChooser", false, new JColorChooser(Color.BLUE), null, null);
        jDialog.setVisible(true);
    }

    private void show(Window window) {
        JButton jButton = new JButton("Show ColorChooser");
        jButton.setActionCommand("Show ColorChooser");
        jButton.addActionListener(this);
        this.cbPlaf = new JComboBox<UIManager.LookAndFeelInfo>(UIManager.getInstalledLookAndFeels());
        this.cbPlaf.addItemListener(new ItemListener(){

            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getStateChange() == 1) {
                    SwingUtilities.invokeLater(new Runnable(){

                        @Override
                        public void run() {
                            UIManager.LookAndFeelInfo lookAndFeelInfo = (UIManager.LookAndFeelInfo)Test4319113.this.cbPlaf.getSelectedItem();
                            try {
                                UIManager.setLookAndFeel(lookAndFeelInfo.getClassName());
                                Frame[] arrframe = Frame.getFrames();
                                int n = arrframe.length;
                                while (--n >= 0) {
                                    Test4319113.updateWindowTreeUI(arrframe[n]);
                                }
                            }
                            catch (Exception var2_3) {
                                System.err.println("Exception while changing L&F!");
                            }
                        }
                    });
                }
            }

        });
        window.add(this.cbPlaf);
        window.add(jButton);
        window.pack();
        window.setVisible(true);
    }

    private static void updateWindowTreeUI(Window window) {
        SwingUtilities.updateComponentTreeUI(window);
        Window[] arrwindow = window.getOwnedWindows();
        int n = arrwindow.length;
        while (--n >= 0) {
            Window window2 = arrwindow[n];
            if (!window2.isDisplayable()) continue;
            Test4319113.updateWindowTreeUI(window2);
        }
    }

}
