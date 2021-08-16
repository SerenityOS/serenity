/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
   @key headful
   @bug 6709913
   @summary Verifies BasicComboBoxUI.isPopupVisible does not return NPE
   @run main BasicComboNPE
 */
import javax.swing.SwingUtilities;
import javax.swing.JComboBox;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.ComboBoxModel;
import java.awt.IllegalComponentStateException;

public class BasicComboNPE extends JComboBox
{
    public static void main(String[] args) {
        for (UIManager.LookAndFeelInfo laf : UIManager.getInstalledLookAndFeels()) {
            try {
                System.out.println("Test for LookAndFeel " + laf.getClassName());
                UIManager.setLookAndFeel(laf.getClassName());
                new BasicComboNPE().getModel();
            } catch (IllegalComponentStateException | ClassNotFoundException | InstantiationException |
                     IllegalAccessException | UnsupportedLookAndFeelException e ) {
               //System.out.println(e);
            }
        }

    }

    @Override
    public ComboBoxModel getModel()
    {
        setPopupVisible(false);
        isPopupVisible();
        return super.getModel();
    }
}
