/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @key headful
 * @bug 6741526 8004693
 * @summary KeyboardFocusManager.setDefaultFocusTraversalPolicy(FocusTraversalPolicy) affects created components
 * @author Andrei Dmitriev : area=awt-focus
 * @run main DefaultPolicyChange_Swing
 */

import java.awt.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class DefaultPolicyChange_Swing {

    public static void main(final String[] s) throws Exception {
        EventQueue.invokeAndWait(DefaultPolicyChange_Swing::runTestSwing);
    }

    private static void runTestSwing(){
        KeyboardFocusManager currentKFM = KeyboardFocusManager.getCurrentKeyboardFocusManager();

        JFrame jf = new JFrame("Test1");
        JWindow jw = new JWindow(jf);
        JDialog jd = new JDialog(jf);
        JPanel jp1 = new JPanel();
        JButton jb1 = new JButton("jb1");
        JTable jt1 = new JTable(new DefaultTableModel());

        jf.add(jb1);
        jf.add(jt1);
        jf.add(jp1);
        System.out.println("FTP current on jf= " + jf.getFocusTraversalPolicy());
        System.out.println("FTP current on jw= " + jw.getFocusTraversalPolicy());
        System.out.println("FTP current on jd= " + jd.getFocusTraversalPolicy());

        if (!(jf.getFocusTraversalPolicy() instanceof LayoutFocusTraversalPolicy) ||
            !(jw.getFocusTraversalPolicy() instanceof LayoutFocusTraversalPolicy) ||
            !(jd.getFocusTraversalPolicy() instanceof LayoutFocusTraversalPolicy))
        {
            throw new RuntimeException("Failure! Swing toplevel must have LayoutFocusTraversalPolicy installed");
        }

        FocusTraversalPolicy[] defaultFTP = {
                jf.getFocusTraversalPolicy(), jw.getFocusTraversalPolicy(),
                jd.getFocusTraversalPolicy()
        };

        jf.setVisible(true);

        System.out.println("Now will set another policy.");
        ContainerOrderFocusTraversalPolicy newFTP = new ContainerOrderFocusTraversalPolicy();
        currentKFM.setDefaultFocusTraversalPolicy(newFTP);

        FocusTraversalPolicy[] resultFTP = {
                jf.getFocusTraversalPolicy(), jw.getFocusTraversalPolicy(),
                jd.getFocusTraversalPolicy()
        };

        System.out.println("FTP current on jf= " + jf.getFocusTraversalPolicy());
        System.out.println("FTP current on jw= " + jw.getFocusTraversalPolicy());
        System.out.println("FTP current on jd= " + jd.getFocusTraversalPolicy());

        jf.dispose();

        for (int i=0; i < 3; i++) {
            if (!resultFTP[i].equals(defaultFTP[i])) {
                System.out.println("Failure! FocusTraversalPolicy should not change");
                System.out.println("Was: " + defaultFTP[i]);
                System.out.println("Become: " + resultFTP[i]);
                throw new RuntimeException("Failure! FocusTraversalPolicy should not change");
            }
        }
    }
}
