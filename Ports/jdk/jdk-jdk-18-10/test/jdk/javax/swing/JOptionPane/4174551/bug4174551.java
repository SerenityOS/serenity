/*
 * Copyright (c) 2001, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4174551
 * @summary JOptionPane should allow custom buttons
 * @author Xhipra Tyagi(xhipra.tyagi@india.sun.com) area=Swing
 * @run applet/manual=yesno bug4174551.html
 */
import java.awt.Font;
import javax.swing.JApplet;
import javax.swing.UIManager;
import javax.swing.JOptionPane;

public class bug4174551 extends JApplet {

    public void init() {
        try {
            java.awt.EventQueue.invokeLater( () -> {
                UIManager.getDefaults().put("OptionPane.buttonFont", new Font("Dialog", Font.PLAIN, 10));
                UIManager.getDefaults().put("OptionPane.messageFont", new Font("Dialog", Font.PLAIN, 24));
                JOptionPane.showMessageDialog(null, "HI 24!");

                System.out.println(UIManager.getDefaults().get("OptionPane.buttonFont"));
                System.out.println(UIManager.getDefaults().get("OptionPane.messageFont"));
            });
        }catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}
