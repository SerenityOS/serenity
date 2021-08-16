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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import test.java.awt.regtesthelpers.Util;

/**
 * Base class for testing overlapping of Swing and AWT component put into one frame.
 * Validates drawing and event delivery at the components intersection.
 * <p> See base class for usage
 *
 * @author Sergey Grinev
*/
public abstract class SimpleOverlappingTestBase extends OverlappingTestBase {

    {
        testEmbeddedFrame = true;
    }

    /**
     * Event delivery validation. If set to true (default) tested lightweight component will be provided
     * with mouse listener which should be called in order for test to pass.
     */
    protected final boolean useDefaultClickValidation;

    /**
     * Constructor which sets {@link SimpleOverlappingTestBase#useDefaultClickValidation }
     * @param defaultClickValidation
     */
    protected SimpleOverlappingTestBase(boolean defaultClickValidation) {
        super();
        this.useDefaultClickValidation = defaultClickValidation;
    }

    public SimpleOverlappingTestBase() {
        this(true);
    }

    //overridables
    /**
     * Successors override this method providing swing component for testing
     * @return swing component to test
     */
    protected abstract JComponent getSwingComponent();

    /**
     * For tests debugging. Please, ignore.
     */
    protected static final boolean debug = false;

    /**
     * Should be set to true if test isn't using {@link SimpleOverlappingTestBase#useDefaultClickValidation }
     */
    protected volatile boolean wasLWClicked = false;

    /**
     * Current tested lightweight component
     * @see SimpleOverlappingTestBase#getSwingComponent()
     */
    protected JComponent testedComponent;

    /**
     * Setups simple frame with lightweight component returned by {@link SimpleOverlappingTestBase#getSwingComponent() }
     * Called by base class.
     */
    protected void prepareControls() {
        wasLWClicked = false;

        final JFrame f = new JFrame("Mixing : Simple Overlapping test");
        f.setLayout(new SpringLayout());
        f.setSize(200, 200);

        testedComponent = getSwingComponent();

        if (useDefaultClickValidation) {
            testedComponent.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    wasLWClicked = true;
                    f.setVisible(false);
                }
            });
        }

        if (!debug) {
            f.add(testedComponent);
        } else {
            System.err.println("Warning: DEBUG MODE");
        }

        propagateAWTControls(f);

        f.setVisible(true);
    }

    /**
     * AWT Robot instance. Shouldn't be used in most cases.
     */
    protected Robot robot;

    /**
     * Run test by {@link OverlappingTestBase#clickAndBlink(java.awt.Robot, java.awt.Point) } validation for current lightweight component.
     * <p>Called by base class.
     * @return true if test passed
     */
    protected boolean performTest() {
        testedComponent.requestFocus();

        // run robot
        robot = Util.createRobot();
        robot.setAutoDelay(20);

        // get coord
        Point lLoc = !debug ? testedComponent.getLocationOnScreen() : new Point(70, 30);
        Util.waitForIdle(robot);
        /* this is a workaround for certain jtreg(?) focus issue:
           tests fail starting after failing mixing tests but always pass alone.
         */
        JFrame ancestor = (JFrame)(testedComponent.getTopLevelAncestor());
        if( ancestor != null ) {
            Point ancestorLoc = ancestor.getLocationOnScreen();
            ancestorLoc.translate(isOel7() ? 5 :
                                             ancestor.getWidth() / 2 - 15, 2);
            robot.mouseMove(ancestorLoc.x, ancestorLoc.y);
            Util.waitForIdle(robot);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.delay(50);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            Util.waitForIdle(robot);
        }

        clickAndBlink(robot, lLoc);
        Util.waitForIdle(robot);

        return wasLWClicked;
    }

    public boolean isOel7() {
        return System.getProperty("os.name").toLowerCase()
                .contains("linux") && System.getProperty("os.version")
                .toLowerCase().contains("el7");
    }

}
