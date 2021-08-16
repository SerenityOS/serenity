/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Container;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.InputEvent;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JFrame;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import test.java.awt.regtesthelpers.Util;

/**
 * Base class for testing overlapping of Swing and AWT component put into GlassPane.
 * Validates drawing and event delivery at the components intersection.
 * <p> See {@link OverlappingTestBase} for usage
 *
 * @author Sergey Grinev
 */
public abstract class GlassPaneOverlappingTestBase extends SimpleOverlappingTestBase {

    /**
     * If true components is additionally tested to be correctly drawn after resize.
     */
    protected boolean testResize = true;
    private JFrame f = null;
    private volatile Point ancestorLoc;

    /**
     * Setups GlassPane with lightweight component returned by {@link SimpleOverlappingTestBase#getSwingComponent() }
     * Called by base class.
     */
    @Override
    protected void prepareControls() {
        wasLWClicked = false;

        if(f != null) {
            f.setVisible(false);
        }
        f = new JFrame("Mixing : GlassPane Overlapping test");
        f.setLayout(new SpringLayout());
        f.setSize(200, 200);

        propagateAWTControls(f);

        f.getGlassPane().setVisible(true);
        Container glassPane = (Container) f.getGlassPane();
        glassPane.setLayout(null);

        testedComponent = getSwingComponent();
        if (useDefaultClickValidation) {
            testedComponent.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    //System.err.println("lw mouse clicked");
                    wasLWClicked = true;
                }
            });
        }
        testedComponent.setBounds(0, 0, testedComponent.getPreferredSize().width, testedComponent.getPreferredSize().height);
        glassPane.add(testedComponent);

        f.setVisible(true);
    }

    public GlassPaneOverlappingTestBase() {
        super();
    }

    public GlassPaneOverlappingTestBase(boolean defaultClickValidation) {
        super(defaultClickValidation);
    }

    /**
     * Run test by {@link OverlappingTestBase#clickAndBlink(java.awt.Robot, java.awt.Point) } validation for current lightweight component.
     * <p>Also resize component and repeat validation in the resized area.
     * <p>Called by base class.
     * @return true if test passed
     * @see GlassPaneOverlappingTestBase#testResize
     */
    @Override
    protected boolean performTest() {
        if (!super.performTest()) {
            return false;
        }
        if (testResize) {
            wasLWClicked = false;
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        testedComponent.setBounds(0, 0, testedComponent.getPreferredSize().width, testedComponent.getPreferredSize().height + 20);
                        ancestorLoc = f.getLocationOnScreen();
                    }
                });
            } catch (InterruptedException ex) {
                fail(ex.getMessage());
            } catch (InvocationTargetException ex) {
                fail(ex.getMessage());
            }
            Point lLoc = testedComponent.getLocationOnScreen();
            lLoc.translate(1, testedComponent.getPreferredSize().height + 1);

            /* this is a workaround for certain jtreg(?) focus issue:
               tests fail starting after failing mixing tests but always pass alone.
             */
            Util.waitForIdle(robot);
            ancestorLoc.translate(isOel7() ? 5 : f.getWidth() / 2 - 15, 2);
            robot.mouseMove(ancestorLoc.x, ancestorLoc.y);
            Util.waitForIdle(robot);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.delay(50);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            Util.waitForIdle(robot);

            clickAndBlink(robot, lLoc);
            return wasLWClicked;
        } else {
            return true;
        }
    }
}
