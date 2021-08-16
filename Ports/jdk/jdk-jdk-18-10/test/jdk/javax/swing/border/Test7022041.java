/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Color;
import java.awt.Font;

import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/* @test
 * @bug 7022041
 * @summary This test check the behaviour of getTitleFont() and getTitleColor()
 *          methods of the TitledBorder class.
 * @author Pavel Tisnovsky
 */
public class Test7022041 {

    public static void main(String[] args) throws Exception {
        UIManager.LookAndFeelInfo[] installedLookAndFeels = UIManager.getInstalledLookAndFeels();
        // try to test all installed Look and Feels
        for (UIManager.LookAndFeelInfo lookAndFeel : installedLookAndFeels) {
            String name = lookAndFeel.getName();
            System.out.println("Testing " + name);
            // Some Look and Feels work only when test is run in a GUI environment
            // (GTK+ LAF is an example)
            try {
                UIManager.setLookAndFeel(lookAndFeel.getClassName());
                checkTitleColor();
                System.out.println("    titleColor test ok");
                checkTitleFont();
                System.out.println("    titleFont test ok");
            }
            catch (UnsupportedLookAndFeelException e) {
                System.out.println("    Note: LookAndFeel " + name
                                 + " is not supported on this configuration");
            }
        }
    }

    /**
      * Check behaviour of method TitledBorder.getTitleColor()
      */
    private static void checkTitleColor() {
        TitledBorder titledBorder = new TitledBorder(new EmptyBorder(1, 1, 1, 1));
        Color defaultColor = UIManager.getLookAndFeelDefaults().getColor("TitledBorder.titleColor");
        Color titledBorderColor = titledBorder.getTitleColor();

        // check default configuration
        if (defaultColor == null) {
            if (titledBorderColor == null) {
                return;
            }
            else {
                throw new RuntimeException("TitledBorder default color should be null");
            }
        }
        if (!defaultColor.equals(titledBorderColor)) {
            throw new RuntimeException("L&F default color " + defaultColor.toString()
                                     + " differs from TitledBorder color " + titledBorderColor.toString());
        }

        // title color is explicitly specified
        Color color = Color.green;
        titledBorder.setTitleColor(color);
        if (!color.equals(titledBorder.getTitleColor())) {
            throw new RuntimeException("TitledBorder color should be " + color.toString());
        }

        // title color is unspecified
        titledBorder.setTitleColor(null);
        if (!defaultColor.equals(titledBorder.getTitleColor())) {
            throw new RuntimeException("L&F default color " + defaultColor.toString()
                                     + " differs from TitledBorder color " + titledBorderColor.toString());
        }
    }

    /**
      * Check behaviour of method TitledBorder.getTitleFont()
      */
    private static void checkTitleFont() {
        TitledBorder titledBorder = new TitledBorder(new EmptyBorder(1, 1, 1, 1));
        Font defaultFont = UIManager.getLookAndFeelDefaults().getFont("TitledBorder.font");
        Font titledBorderFont = titledBorder.getTitleFont();

        // check default configuration
        if (defaultFont == null) {
            if (titledBorderFont == null) {
                return;
            }
            else {
                throw new RuntimeException("TitledBorder default font should be null");
            }
        }
        if (!defaultFont.equals(titledBorderFont)) {
            throw new RuntimeException("L&F default font " + defaultFont.toString()
                                     + " differs from TitledBorder font " + titledBorderFont.toString());
        }

        // title font is explicitly specified
        Font font = new Font("Dialog", Font.PLAIN, 10);
        titledBorder.setTitleFont(font);
        if (!font.equals(titledBorder.getTitleFont())) {
            throw new RuntimeException("TitledBorder font should be " + font.toString());
        }

        // title Font is unspecified
        titledBorder.setTitleFont(null);
        if (!defaultFont.equals(titledBorder.getTitleFont())) {
            throw new RuntimeException("L&F default font " + defaultFont.toString()
                                     + " differs from TitledBorder font " + titledBorderFont.toString());
        }
    }
}

