/*
 * Copyright (c) 2004, 2014, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/*
 * @author Aruna Samji
 */

public class GUIUndFrame extends Frame {

    JFrame jframe1, jframe2, jframe3;
    Component comp;
    JButton jbutton1, jbutton2, jbutton3, jbutton4;
    JTextArea jtextarea;

    volatile boolean win_act, win_deact, win_ico, win_deico, win_close;

    public GUIUndFrame() {
        //GUI for UndJFrameProperties
        jframe1 = new JFrame();
        jframe1.getContentPane().setLayout(new FlowLayout());
        jframe1.setSize(500,255);
        jframe1.setUndecorated(true);
        jframe1.getContentPane().setBackground(Color.red);

        jframe1.addWindowListener( new WindowAdapter() {
            public void windowActivated(WindowEvent e){
                comp = null;
                comp = e.getComponent();
                if (e.getComponent() == jframe1)
                    win_act = true;
            }

            public void windowDeactivated(WindowEvent e){
                win_deact = true;
            }
        });


        jbutton1 = new JButton("Hide me");
        jbutton1.addActionListener(e -> jframe1.setVisible(false));


        //Create a normal decorated frame to test all the relevant assertions
        jframe2 = new JFrame();
        jframe2.getContentPane().setLayout(new FlowLayout());
        jframe2.setLocation(0,270);
        jframe2.setSize(500,255);
        jframe2.getContentPane().setBackground(Color.blue);
        jbutton2 = new JButton("Show hiddenJFrame");
        jbutton2.addActionListener(e -> jframe1.setVisible(true));


        //GUI for UndFrameIconifyRepaint
        jframe3 = new JFrame();
        jframe3.getContentPane().setLayout(new FlowLayout());
        jframe3.setSize(500,255);
        jframe3.getContentPane().setBackground(Color.green);
        jframe3.setUndecorated(true);
        jframe3.addWindowListener( new WindowAdapter() {
            public void windowActivated(WindowEvent e) {
                comp = null;
                comp = e.getComponent();
                if(e.getComponent() == jframe3){
                    win_act=true;

                }
            }
            public void windowIconified(WindowEvent e){ win_ico = true; }
            public void windowDeiconified(WindowEvent e){ win_deico = true; }
            public void windowDeactivated(WindowEvent e){ win_deact = true; }
            public void windowClosed(WindowEvent e){ win_close = true; }
        });

        jbutton3 = new JButton("Minimize me");
        jbutton3.addActionListener(e -> jframe3.setState(Frame.ICONIFIED));
        jbutton4 = new JButton("Maximize me");
        jbutton4.addActionListener(e -> {
            if (Toolkit.getDefaultToolkit().isFrameStateSupported
                    (Frame.MAXIMIZED_BOTH)) {
                jframe3.setExtendedState(Frame.MAXIMIZED_BOTH);
            }
        });

        jtextarea = new JTextArea("Textarea");
    }
}
