/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @key headful
  @bug 4452373 6567564
  @summary Tests that all the child and toplevel components have
their GraphicsConfiguration updated when the window moves from
one screen to another, on Windows or Linux/Solaris + Xinerama
  @author artem.ananiev: area=awt.multiscreen
  @library ../../regtesthelpers
  @build Util
  @run main UpdateGCTest
*/

import java.awt.*;
import java.awt.event.*;

import test.java.awt.regtesthelpers.Util;

public class UpdateGCTest
{
    public static void main(String[] args)
    {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gds = ge.getScreenDevices();
        if (gds.length < 2)
        {
            System.out.println("The test should be run in multi-screen configuration. Test PASSED/skipped");
            return;
        }
        boolean virtualConfig = false;
        for (GraphicsDevice gd : gds)
        {
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            if ((gc.getBounds().x != 0) || (gc.getBounds().y != 0))
            {
                virtualConfig = true;
                break;
            }
        }
        if (!virtualConfig)
        {
            System.out.println("The test should be run in virtual multi-screen mode. Test PASSED/skipped");
            return;
        }

        try
        {
            Robot robot = new Robot();
            Util.waitForIdle(robot);

            for (GraphicsDevice gdOrig : gds)
            {
                GraphicsConfiguration gcOrig = gdOrig.getDefaultConfiguration();

                // test Frame
                Frame f = new Frame("F", gcOrig);
                f.setSize(200, 200);
                f.setLayout(new BorderLayout());
                // test Canvas
                f.add(new Canvas(gcOrig), BorderLayout.NORTH);
                // test lightweight
                Container c = new Container() {};
                c.setLayout(new BorderLayout());
                // test hw inside lw
                c.add(new Panel());
                c.add(new Canvas(gcOrig));
                f.add(c, BorderLayout.SOUTH);
                // test Panel
                Panel p = new Panel();
                p.setLayout(new BorderLayout());
                // test nested Canvas
                p.add(new Canvas(gcOrig), BorderLayout.NORTH);
                // test nested lightweight
                p.add(new Component() {}, BorderLayout.SOUTH);
                // test nested panel
                p.add(new Panel(), BorderLayout.CENTER);
                f.add(p, BorderLayout.CENTER);

                f.setVisible(true);
                Util.waitForIdle(robot);

                for (GraphicsDevice gd : gds)
                {
                    GraphicsConfiguration gc = gd.getDefaultConfiguration();

                    f.setLocation(gc.getBounds().x + 100, gc.getBounds().y + 100);
                    Util.waitForIdle(robot);

                    checkGC(f, gc);
                }
            }
        }
        catch (Exception z)
        {
            System.err.println("Unknown exception caught");
            z.printStackTrace(System.err);
            throw new RuntimeException("Test FAILED: " + z.getMessage());
        }

        System.out.println("Test PASSED");
    }

    private static void checkGC(Component c, GraphicsConfiguration gc)
    {
        if (c.getGraphicsConfiguration() != gc)
        {
            System.err.println("GC for component (" + c + ") is not updated");
            System.err.println("Right GC: " + gc);
            System.err.println("Component GC: " + c.getGraphicsConfiguration());
            throw new RuntimeException("Test FAILED: component GC is not updated");
        }
        System.out.println("Checked GC for component (" + c + "): OK");

        if (c instanceof Container)
        {
            Container cc = (Container)c;
            Component[] children = cc.getComponents();
            for (Component child : children)
            {
                checkGC(child, gc);
            }
        }
    }
}
