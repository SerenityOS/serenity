/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.swingset3.demos.internalframe.InternalFrameDemo.CLOSABLE_LABEL;
import static com.sun.swingset3.demos.internalframe.InternalFrameDemo.DEMO_TITLE;
import static com.sun.swingset3.demos.internalframe.InternalFrameDemo.FRAME0_X;
import static com.sun.swingset3.demos.internalframe.InternalFrameDemo.FRAME0_Y;
import static com.sun.swingset3.demos.internalframe.InternalFrameDemo.FRAME_GAP;
import static com.sun.swingset3.demos.internalframe.InternalFrameDemo.FRAME_HEIGHT;
import static com.sun.swingset3.demos.internalframe.InternalFrameDemo.FRAME_WIDTH;
import static com.sun.swingset3.demos.internalframe.InternalFrameDemo.ICONIFIABLE_LABEL;
import static com.sun.swingset3.demos.internalframe.InternalFrameDemo.INTERNAL_FRAME_LABEL;
import static com.sun.swingset3.demos.internalframe.InternalFrameDemo.MAXIMIZABLE_LABEL;
import static com.sun.swingset3.demos.internalframe.InternalFrameDemo.PALETTE_HEIGHT;
import static com.sun.swingset3.demos.internalframe.InternalFrameDemo.PALETTE_LABEL;
import static com.sun.swingset3.demos.internalframe.InternalFrameDemo.PALETTE_WIDTH;
import static com.sun.swingset3.demos.internalframe.InternalFrameDemo.PALETTE_X;
import static com.sun.swingset3.demos.internalframe.InternalFrameDemo.PALETTE_Y;
import static com.sun.swingset3.demos.internalframe.InternalFrameDemo.RESIZABLE_LABEL;
import static org.jemmy2ext.JemmyExt.EXACT_STRING_COMPARATOR;
import static org.testng.AssertJUnit.assertFalse;

import java.awt.Dimension;
import java.awt.Point;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.UIManager;

import org.jemmy2ext.JemmyExt;
import org.jtregext.GuiTestListener;
import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JCheckBoxOperator;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JInternalFrameOperator;
import org.netbeans.jemmy.operators.JTextFieldOperator;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.sun.swingset3.demos.internalframe.InternalFrameDemo;

/*
 * @test
 * @bug 8211703
 * @key headful
 * @summary Verifies SwingSet3 InternalFrameDemo page by checking the different
 *  actions on the parent frame, internal frame and creating internal frame
 *  with different properties
 *
 * @library /sanity/client/lib/jemmy/src
 * @library /sanity/client/lib/Extensions/src
 * @library /sanity/client/lib/SwingSet3/src
 * @modules java.desktop
 *          java.logging
 * @build org.jemmy2ext.JemmyExt
 * @build com.sun.swingset3.demos.internalframe.InternalFrameDemo
 * @run testng/timeout=600 InternalFrameDemoTest
 */
@Listeners(GuiTestListener.class)
public class InternalFrameDemoTest {

    private final static int PARENT_FRAME_NEW_SIZE_DELTA = 300;
    private final static int PARENT_FRAME_NEW_LOCATION_DELTA = 200;
    private final static Dimension INTERNAL_FRAME_NEW_SIZE = new Dimension(400, 400);
    private final static Point INTERNAL_FRAME_NEW_LOCATION = new Point(390, 120);
    private final static int DELAY = 500;
    private final static String INTERNAL_FRAME_NEW_NAME = "New Internal Frame";

    /**
     * Testing the different actions on the parent frame, internal frame and
     * creating internal frame with different properties
     *
     * @throws Exception
     */
    @Test(dataProvider = "availableLookAndFeels", dataProviderClass = TestHelpers.class)
    public void test(String lookAndFeel) throws Exception {
        UIManager.setLookAndFeel(lookAndFeel);
        // initializing internal frame driver for each L&F
        JemmyProperties.setCurrentDispatchingModel(
                JemmyProperties.getCurrentDispatchingModel());

        new ClassReference(InternalFrameDemo.class.getCanonicalName()).startApplication();

        JFrameOperator frameOperator = new JFrameOperator(DEMO_TITLE);
        frameOperator.setComparator(EXACT_STRING_COMPARATOR);
        frameOperator.setVerification(true);

        // verifying internal frame primary properties
        JInternalFrameOperator internalFrameOperator = new JInternalFrameOperator(
                frameOperator, getInternalFrameName(INTERNAL_FRAME_LABEL, 0));
        internalFrameOperator.setVerification(true);
        checkInternalFramePrimaryProps(internalFrameOperator,
                new Point(FRAME0_X, FRAME0_Y), new Dimension(FRAME_WIDTH, FRAME_HEIGHT));

        // Verifying the internal frame properties after doing different
        // actions on parent frame
        checkParentFrameAction(frameOperator, internalFrameOperator);

        // Verifying different actions on the internal frame
        checkInternalFrameAction(internalFrameOperator);

        // Creating internal frames with different action properties
        // and verifying their properties
        checkPaletteFrameAction(frameOperator);
    }

    /**
     * Verifying the internal frame properties after doing different actions on
     * parent frame, it should not affect internal frame.
     *
     * @param parentFrameOperator : parent fame operator
     * @param internalFrameOperator : internal fame operator
     * @throws InterruptedException
     */
    private void checkParentFrameAction(JFrameOperator parentFrameOperator,
            JInternalFrameOperator internalFrameOperator) throws InterruptedException {

        // Resizing the parent frame and verifying the size of the internal
        // frame(it should not be resized)
        Dimension orignalSize = parentFrameOperator.getSize();
        Dimension newSize = new Dimension(orignalSize.width - PARENT_FRAME_NEW_SIZE_DELTA,
                orignalSize.height - PARENT_FRAME_NEW_SIZE_DELTA);
        parentFrameOperator.resize(newSize.width, newSize.height);
        parentFrameOperator.waitComponentSize(newSize);
        // TODO This is a workaround for JDK-8210638, this delay has to remove
        // after fixing this bug, this is an unstable code.
        TestHelpers.delayBetweenFrameStateChange();
        // keeping some delay before checking the internal frame property
        // as it is a negative scenario
        Thread.sleep(DELAY);
        internalFrameOperator.waitComponentSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        // Resizing parent frame back to original size
        parentFrameOperator.resize(orignalSize.width, orignalSize.height);
        parentFrameOperator.waitComponentSize(orignalSize);
        // TODO This is a workaround for JDK-8210638, this delay has to remove
        // after fixing this bug, this is an unstable code.
        TestHelpers.delayBetweenFrameStateChange();

        // Iconifying the parent frame and verifying the iconified status of the internal
        // frame(it should not be iconified)
        parentFrameOperator.iconify();
        // TODO This is a workaround for JDK-8210638, this delay has to remove
        // after fixing this bug, this is an unstable code.
        TestHelpers.delayBetweenFrameStateChange();
        // keeping some delay before checking the internal frame property
        // as it is a negative scenario
        Thread.sleep(DELAY);
        assertFalse("Internal Frame should not be iconified when parent frame"
                + " alone is iconified.", internalFrameOperator.isIcon());
        parentFrameOperator.deiconify();
        // TODO This is a workaround for JDK-8210638, this delay has to remove
        // after fixing this bug, this is an unstable code.
        TestHelpers.delayBetweenFrameStateChange();

        // Maximizing the parent frame and verifying the maximized status of the internal
        // frame(it should not be maximized)
        parentFrameOperator.maximize();
        // TODO This is a workaround for JDK-8210638, this delay has to remove
        // after fixing this bug, this is an unstable code.
        TestHelpers.delayBetweenFrameStateChange();
        // keeping some delay before checking the internal frame property
        // as it is a negative scenario
        Thread.sleep(DELAY);
        assertFalse("Internal Frame should not be maximized when parent frame"
                + " alone is maximized.", internalFrameOperator.isMaximum());
        parentFrameOperator.demaximize();
        // TODO This is a workaround for JDK-8210638, this delay has to remove
        // after fixing this bug, this is an unstable code.
        TestHelpers.delayBetweenFrameStateChange();

        // Relocating the parent frame and verifying the location of the internal
        // frame(it should not be changed the location)
        Point orignalLocation = parentFrameOperator.getLocation();
        Point newLocation = new Point((orignalLocation.x - PARENT_FRAME_NEW_LOCATION_DELTA),
                (orignalLocation.y + PARENT_FRAME_NEW_LOCATION_DELTA));
        parentFrameOperator.move(newLocation.x, newLocation.y);
        parentFrameOperator.waitComponentLocation(newLocation);
        // TODO This is a workaround for JDK-8210638, this delay has to remove
        // after fixing this bug, this is an unstable code.
        TestHelpers.delayBetweenFrameStateChange();
        // keeping some delay before checking the internal frame property
        // as it is a negative scenario
        Thread.sleep(DELAY);
        internalFrameOperator.waitComponentLocation(new Point(FRAME0_X, FRAME0_Y));
        // Moving back parent frame to original location
        parentFrameOperator.move(orignalLocation.x, orignalLocation.y);
        parentFrameOperator.waitComponentLocation(orignalLocation);
        // TODO This is a workaround for JDK-8210638, this delay has to remove
        // after fixing this bug, this is an unstable code.
        TestHelpers.delayBetweenFrameStateChange();
    }

    /**
     * Verifying different actions on the internal frame.
     *
     * @param internalFrameOperator : internal fame operator
     * @throws InterruptedException
     */
    private void checkInternalFrameAction(JInternalFrameOperator
            internalFrameOperator) throws InterruptedException {
        // Verifying maximize and demaximize actions
        internalFrameOperator.waitStateOnQueue(comp
                -> ((JInternalFrame)comp).isMaximizable());
        internalFrameOperator.maximize();
        internalFrameOperator.demaximize();

        // Verifying iconify and deiconify actions
        internalFrameOperator.waitStateOnQueue(comp
                -> ((JInternalFrame)comp).isIconifiable());
        internalFrameOperator.iconify();
        internalFrameOperator.deiconify();

        // Verifying resize action
        internalFrameOperator.waitStateOnQueue(comp
                -> ((JInternalFrame)comp).isResizable());
        TestHelpers.checkChangeSize(internalFrameOperator,
                INTERNAL_FRAME_NEW_SIZE);

        // Verifying move action
        TestHelpers.checkChangeLocation(internalFrameOperator,
                INTERNAL_FRAME_NEW_LOCATION);

        // Verifying close action
        internalFrameOperator.waitStateOnQueue(comp
                -> ((JInternalFrame)comp).isClosable());
        internalFrameOperator.close();
    }

    /**
     * Creating internal frames with different action properties and verifying
     * their properties
     *
     * @param frameOperator : parent frame operator
     */
    private void checkPaletteFrameAction(JFrameOperator frameOperator) {
        // Getting palette frame and checking the properties of it
        JInternalFrameOperator paletteFrameOperator =
                new JInternalFrameOperator(frameOperator, PALETTE_LABEL);
        paletteFrameOperator.setComparator(EXACT_STRING_COMPARATOR);
        checkInternalFramePrimaryProps(paletteFrameOperator,
                new Point(PALETTE_X, PALETTE_Y),
                new Dimension(PALETTE_WIDTH, PALETTE_HEIGHT));

        // Getting all check boxes to set/reset frame properties
        JCheckBoxOperator closableOperator =
                new JCheckBoxOperator(paletteFrameOperator, CLOSABLE_LABEL);
        JCheckBoxOperator iconifiableOperator =
                new JCheckBoxOperator(paletteFrameOperator, ICONIFIABLE_LABEL);
        JCheckBoxOperator maximizableOperator =
                new JCheckBoxOperator(paletteFrameOperator, MAXIMIZABLE_LABEL);
        JCheckBoxOperator resizableOperator =
                new JCheckBoxOperator(paletteFrameOperator, RESIZABLE_LABEL);
        JCheckBoxOperator[] checkBoxes = {closableOperator, iconifiableOperator,
                maximizableOperator, resizableOperator};

        // Verifying internal frame with all action properties set to false
        checkFrameProps(frameOperator, paletteFrameOperator,
                getInternalFrameName(INTERNAL_FRAME_LABEL, 1), 1, checkBoxes, false);

        // Verifying internal frame with all action properties set to true and setting title
        JTextFieldOperator frameTitle = new JTextFieldOperator(paletteFrameOperator);
        frameTitle.setText(INTERNAL_FRAME_NEW_NAME);
        checkFrameProps(frameOperator, paletteFrameOperator,
                getInternalFrameName(INTERNAL_FRAME_NEW_NAME), 2, checkBoxes, true);
    }

    /**
     * Verifying internal frame properties
     *
     * @param frameOperator : parent frame operator
     * @param paletteFrameOperator : palette frame operator
     * @param title : title of the internal frame
     * @param index : index of the internal frame
     * @param checkBoxes : array of check boxes
     * @param checkBoxStatus : status of check box
     */
    private void checkFrameProps(JFrameOperator frameOperator,
            JInternalFrameOperator paletteFrameOperator,
            String title, int index, JCheckBoxOperator[] checkBoxes,
            boolean checkBoxStatus) {

        pushCheckBoxes(checkBoxes, checkBoxStatus);
        JButtonOperator button = new JButtonOperator(paletteFrameOperator, (index -1));
        button.push();
        JInternalFrameOperator internalFrameOperator =
                new JInternalFrameOperator(frameOperator, title);
        int gap = FRAME_GAP * index;
        checkInternalFramePrimaryProps(internalFrameOperator,
                new Point(FRAME0_X + gap, FRAME0_Y + gap),
                new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        checkFrameActionProps(internalFrameOperator, checkBoxStatus);
        ComponentOperator desktopOperator = new ComponentOperator(
                frameOperator, new JemmyExt.ByClassChooser(JDesktopPane.class));
        frameOperator.waitStateOnQueue(comp -> ((JDesktopPane)desktopOperator.
                getSource()).getAllFrames().length == index + 1);
    }

    /**
     * Verifying internal frame primary properties like showing status, location and size
     * @param internalFrameOperator
     * @param location
     * @param size
     */
    private void checkInternalFramePrimaryProps(JInternalFrameOperator internalFrameOperator,
            Point location, Dimension size) {
        internalFrameOperator.waitComponentShowing(true);
        internalFrameOperator.waitComponentLocation(location);
        internalFrameOperator.waitComponentSize(size);
    }

    /**
     * Verifying internal frame action status
     *
     * @param internalFrameOperator : internal frame operator
     * @param propertyStatus : status to check
     */
    private void checkFrameActionProps(JInternalFrameOperator internalFrameOperator,
            boolean propertyStatus) {
        internalFrameOperator.waitStateOnQueue(comp
                -> ((JInternalFrame)comp).isClosable() == propertyStatus);
        internalFrameOperator.waitStateOnQueue(comp
                -> ((JInternalFrame)comp).isIconifiable() == propertyStatus);
        internalFrameOperator.waitStateOnQueue(comp
                -> ((JInternalFrame)comp).isMaximizable() == propertyStatus);
        internalFrameOperator.waitStateOnQueue(comp
                -> ((JInternalFrame)comp).isResizable() == propertyStatus);
    }

    /**
     * To set/reset check boxes
     *
     * @param checkBoxes : array of check boxes
     * @param select : set/reset
     */
    private void pushCheckBoxes(JCheckBoxOperator []checkBoxes, boolean select){
        for (JCheckBoxOperator checkBox : checkBoxes) {
            checkBox.push();
            checkBox.waitSelected(select);
        }
    }

    /**
     * Gets the internal frame name
     * @param frameLabel
     * @return
     */
    private String getInternalFrameName(String frameLabel) {
        return (frameLabel+ "  ");
    }

    /**
     * Gets the internal frame name
     * @param frameLabel
     * @param index
     * @return
     */
    private String getInternalFrameName(String frameLabel, int index) {
        return getInternalFrameName(frameLabel+ " " + index);
    }

}
