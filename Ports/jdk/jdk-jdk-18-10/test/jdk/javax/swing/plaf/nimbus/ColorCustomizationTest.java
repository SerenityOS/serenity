/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
   @bug 6860433
   @summary Tests variuos techniques of Nimbus color customization
   @author Peter Zhelezniakov
   @run main ColorCustomizationTest
*/

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.plaf.synth.Region;

public class ColorCustomizationTest
{
    final static int WIDTH = 200;
    final static int HEIGHT = 100;

    static NimbusLookAndFeel nimbus;

    final JLabel label;
    final Graphics g;

    ColorCustomizationTest() {
        label = new JLabel();
        label.setSize(200, 100);

        g = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB).getGraphics();
    }

    public static void main(String[] args) throws Exception {
        nimbus = new NimbusLookAndFeel();
        try {
            UIManager.setLookAndFeel(nimbus);
        } catch (UnsupportedLookAndFeelException e) {
            throw new Error("Unable to set Nimbus LAF");
        }
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() {
                new ColorCustomizationTest().test();
            }
        });
    }

    void check(Color c) {
        SwingUtilities.updateComponentTreeUI(label);
        label.paint(g);
        if (label.getBackground().getRGB() != c.getRGB()) {
            System.err.println("Color mismatch!");
            System.err.println("   found: " + label.getBackground());
            System.err.println("   expected: " + c);
            throw new RuntimeException("Test failed");
        }
    }

    void test() {
        testOverrides();
        testInheritance();
        testNames();
        testBaseColor();
    }

    void testOverrides() {
        Color defaultColor = label.getBackground();

        // override default background
        UIDefaults defs = new UIDefaults();
        defs.put("Label.background", new ColorUIResource(Color.RED));
        label.putClientProperty("Nimbus.Overrides", defs);
        check(Color.RED);

        // change overriding color
        defs = new UIDefaults();
        defs.put("Label.background", new ColorUIResource(Color.GREEN));
        label.putClientProperty("Nimbus.Overrides", defs);
        check(Color.GREEN);

        // remove override
        label.putClientProperty("Nimbus.Overrides", null);
        check(defaultColor);
    }

    void testInheritance() {
        Color defaultColor = label.getBackground();

        // more specific setting is in global defaults
        UIManager.put("Label[Enabled].background", new ColorUIResource(Color.RED));

        // less specific one is in overrides
        UIDefaults defs = new UIDefaults();
        defs.put("Label.background", new ColorUIResource(Color.GREEN));

        // global wins
        label.putClientProperty("Nimbus.Overrides", defs);
        check(Color.RED);

        // now override wins
        label.putClientProperty("Nimbus.Overrides.InheritDefaults", false);
        check(Color.GREEN);

        // global is back
        label.putClientProperty("Nimbus.Overrides.InheritDefaults", true);
        check(Color.RED);

        // back to default color
        UIManager.put("Label[Enabled].background", null);
        label.putClientProperty("Nimbus.Overrides.InheritDefaults", false);
        label.putClientProperty("Nimbus.Overrides", null);
        check(defaultColor);
    }

    void testNames() {
        Color defaultColor = label.getBackground();

        UIManager.put("\"BlueLabel\"[Enabled].background",
                new ColorUIResource(Color.BLUE));
        UIManager.put("\"RedLabel\"[Enabled].background",
                new ColorUIResource(Color.RED));
        nimbus.register(Region.LABEL, "\"BlueLabel\"");
        nimbus.register(Region.LABEL, "\"RedLabel\"");

        label.setName("BlueLabel");
        check(Color.BLUE);
        label.setName("RedLabel");
        check(Color.RED);

        // remove name, color goes back to default
        label.setName(null);
        check(defaultColor);
    }

    void testBaseColor() {
        UIManager.put("control", Color.GREEN);
        check(Color.GREEN);
    }
}
