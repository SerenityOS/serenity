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

import static com.sun.swingset3.demos.frame.FrameDemo.BUSY_CHECKBOX;
import static com.sun.swingset3.demos.frame.FrameDemo.CONTENT_LABEL;
import static com.sun.swingset3.demos.frame.FrameDemo.CONTENT_LABEL_COLOR;
import static com.sun.swingset3.demos.frame.FrameDemo.CONTENT_LABEL_SIZE;
import static com.sun.swingset3.demos.frame.FrameDemo.DEMO_TITLE;
import static com.sun.swingset3.demos.frame.FrameDemo.INTERNAL_FRAME;
import static com.sun.swingset3.demos.frame.FrameDemo.MENU;
import static com.sun.swingset3.demos.frame.FrameDemo.MENU_ITEM1;
import static com.sun.swingset3.demos.frame.FrameDemo.MENU_ITEM2;
import static com.sun.swingset3.demos.frame.FrameDemo.SHOW_BUTTON;
import static com.sun.swingset3.demos.frame.FrameDemo.STATUS_LABEL;
import static com.sun.swingset3.demos.frame.FrameDemo.STATUS_LABEL_BORDER;
import static com.sun.swingset3.demos.frame.FrameDemo.STATUS_LABEL_HOR_ALIGNMENT;
import static com.sun.swingset3.demos.frame.FrameDemo.TOOLBAR_BUTTON;
import static org.jemmy2ext.JemmyExt.EXACT_STRING_COMPARATOR;
import static org.testng.AssertJUnit.assertEquals;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.UIManager;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.jtregext.GuiTestListener;
import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.WindowWaiter;
import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.operators.FrameOperator;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JCheckBoxOperator;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JLabelOperator;
import org.netbeans.jemmy.operators.JMenuBarOperator;
import org.netbeans.jemmy.operators.JMenuItemOperator;
import org.netbeans.jemmy.operators.JMenuOperator;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.sun.swingset3.demos.frame.FrameDemo;

/*
 * @test
 * @key headful
 * @summary Verifies SwingSet3 FrameDemo page by checking the different actions
 *  on the frame, properties and different actions on subcomponents of frame
 *  and control panel actions by checking and unchecking the busy check box and
 *  pressing the show button.
 *
 * @library /sanity/client/lib/jemmy/src
 * @library /sanity/client/lib/Extensions/src
 * @library /sanity/client/lib/SwingSet3/src
 * @modules java.desktop
 *          java.logging
 * @build org.jemmy2ext.JemmyExt
 * @build com.sun.swingset3.demos.frame.FrameDemo
 * @run testng/timeout=600 FrameDemoTest
 */
@Listeners(GuiTestListener.class)
public class FrameDemoTest {

    private final static Dimension NEW_SIZE = new Dimension(500, 500);
    private final static Point NEW_LOCATION = new Point(200, 200);
    private final static int NUMBER_OF_MENUS = 1;
    private final static int NUMBER_OF_MENU_ITEMS = 2;
    private final static int MAX_NUMBER_OF_FRAMES = 1;
    private final static int DELAY_AFTER_SHOW_BUTTON_PRESS = 500;

    /**
     * Testing the different actions on the frame, properties and different
     * actions on subcomponents of the frame and control panel action by
     * checking and unchecking the busy check box and pressing the show button.
     *
     * @throws Exception
     */
    @Test(dataProvider = "availableLookAndFeels", dataProviderClass = TestHelpers.class)
    public void test(String lookAndFeel) throws Exception {
        UIManager.setLookAndFeel(lookAndFeel);
        new ClassReference(FrameDemo.class.getCanonicalName()).startApplication();

        JFrameOperator masterFrameOperator = new JFrameOperator(DEMO_TITLE);
        masterFrameOperator.setComparator(EXACT_STRING_COMPARATOR);

        JFrameOperator internalFrameOperator = new JFrameOperator(INTERNAL_FRAME);
        internalFrameOperator.setComparator(EXACT_STRING_COMPARATOR);
        internalFrameOperator.setVerification(true);
        internalFrameOperator.waitComponentVisible(true);

        checkSubComponents(internalFrameOperator);
        checkFrameActions(internalFrameOperator);
        checkControlPanelActions(masterFrameOperator, internalFrameOperator);
    }

    /**
     * Verifying the status of added components to the frame
     * @param internalFrameOperator
     */
    private void checkSubComponents(JFrameOperator internalFrameOperator) {
        // Verifying the properties of added button to the frame
        JButtonOperator buttonOperator =
                new JButtonOperator(internalFrameOperator, TOOLBAR_BUTTON);
        AtomicBoolean buttonActionStatus = new AtomicBoolean(false);
        buttonOperator.addActionListener(event -> buttonActionStatus.set(true));
        buttonOperator.push();
        buttonOperator.waitStateOnQueue(comp -> buttonActionStatus.get());

        // Verifying the properties of added labels to the frame
        JLabelOperator contentLabelOperator =
                new JLabelOperator(internalFrameOperator, CONTENT_LABEL);
        contentLabelOperator.waitStateOnQueue(comp
                -> CONTENT_LABEL_SIZE.equals(comp.getSize()));
        contentLabelOperator.waitStateOnQueue(comp
                -> CONTENT_LABEL_COLOR.equals(comp.getBackground()));
        JLabelOperator statusLabelOperator =
                new JLabelOperator(internalFrameOperator, STATUS_LABEL);
        statusLabelOperator.waitStateOnQueue(comp
                -> STATUS_LABEL_BORDER.equals(((JLabel)comp).getBorder()));
        statusLabelOperator.waitStateOnQueue((component) -> STATUS_LABEL_HOR_ALIGNMENT
                == ((JLabel)component).getHorizontalAlignment());

        // Verifying the properties of added menu to the frame
        JMenuBarOperator menuBarOperator = new JMenuBarOperator(internalFrameOperator);
        menuBarOperator.waitStateOnQueue(comp -> NUMBER_OF_MENUS
                == ((JMenuBar)comp).getMenuCount());
        JMenuOperator menuOperator = new JMenuOperator(internalFrameOperator, MENU);
        menuOperator.waitStateOnQueue(comp -> NUMBER_OF_MENU_ITEMS
                == ((JMenu)comp).getMenuComponentCount());
        AtomicBoolean menuActionStatus = new AtomicBoolean(false);
        addMenuListener(menuOperator, menuActionStatus);
        menuOperator.push();
        menuOperator.waitStateOnQueue(comp -> menuActionStatus.get());

        // Verifying the properties of the menu items
        checkMenuItem((JMenuItem) menuOperator.getMenuComponent(0), MENU_ITEM1);
        checkMenuItem((JMenuItem) menuOperator.getMenuComponent(1), MENU_ITEM2);
    }

    /**
     * Verifying different actions on the frame
     * @param internalFrameOperator
     */
    private void checkFrameActions(JFrameOperator internalFrameOperator)
            throws InterruptedException {
        // Verifying the maximized status
        internalFrameOperator.maximize();
        // TODO This is a workaround for JDK-8210638, this delay has to remove
        // after fixing this bug, this is an unstable code.
        TestHelpers.delayBetweenFrameStateChange();
        internalFrameOperator.demaximize();
        // TODO This is a workaround for JDK-8210638, this delay has to remove
        // after fixing this bug, this is an unstable code.
        TestHelpers.delayBetweenFrameStateChange();

        // Verifying the iconified status
        internalFrameOperator.iconify();
        // TODO This is a workaround for JDK-8210638, this delay has to remove
        // after fixing this bug, this is an unstable code.
        TestHelpers.delayBetweenFrameStateChange();
        internalFrameOperator.deiconify();
        // TODO This is a workaround for JDK-8210638, this delay has to remove
        // after fixing this bug, this is an unstable code.
        TestHelpers.delayBetweenFrameStateChange();

        // Verifying the resize of the frame
        TestHelpers.checkChangeSize(internalFrameOperator, NEW_SIZE);

        // Verifying the change of location of the frame
        TestHelpers.checkChangeLocation(internalFrameOperator, NEW_LOCATION);
    }

    /**
     * Verifying control panel actions on the frame
     * @param masterFrameOprator
     * @param internalFrameOperator
     * @throws InterruptedException
     */
    private void checkControlPanelActions(JFrameOperator masterFrameOprator,
            JFrameOperator internalFrameOperator) throws InterruptedException {
        // Verifying the visibility and number of frames after pressing Show Button
        internalFrameOperator.requestClose();
        internalFrameOperator.waitClosed();
        JButtonOperator showButtonOperator = new JButtonOperator(masterFrameOprator, SHOW_BUTTON);
        showButtonOperator.push();
        internalFrameOperator.waitComponentVisible(true);
        showButtonOperator.push();
        showButtonOperator.push();
        Thread.sleep(DELAY_AFTER_SHOW_BUTTON_PRESS);
        int count = WindowWaiter.countWindows(masterFrameOprator.getOwner(),
                new FrameOperator.FrameFinder(new ComponentChooser() {

            @Override
            public String getDescription() {
                return "frames with name != " + DEMO_TITLE;
            }

            @Override
            public boolean checkComponent(Component comp) {
                return comp.isShowing()
                        && ((Frame) comp).getTitle() != DEMO_TITLE;
            }
        }));
        assertEquals("Number of frames after clicking Show Button two times"
                + " validation failed,", MAX_NUMBER_OF_FRAMES, count);

        // Verifying the visibility and cursor type after selecting busy check box
        JCheckBoxOperator busyCheckBoxOperator =
                new JCheckBoxOperator(masterFrameOprator, BUSY_CHECKBOX);
        busyCheckBoxOperator.setVerification(true);
        checkBusyCheckBox(internalFrameOperator, busyCheckBoxOperator, true);
        internalFrameOperator.waitStateOnQueue(comp -> Cursor.WAIT_CURSOR
                == internalFrameOperator.getGlassPane().getCursor().getType());

        checkBusyCheckBox(internalFrameOperator, busyCheckBoxOperator, false);
        internalFrameOperator.waitStateOnQueue(comp -> Cursor.DEFAULT_CURSOR
                == internalFrameOperator.getCursor().getType());
    }

    private void checkBusyCheckBox(JFrameOperator internalFrameOperator,
            JCheckBoxOperator busyCheckBoxOperator, boolean isSelect) {
        busyCheckBoxOperator.changeSelection(isSelect);
        new ComponentOperator(internalFrameOperator.
                getGlassPane()).waitComponentVisible(isSelect);
    }

    /**
     * Verifying the properties of the menu item
     * @param menuItem : menu item component
     * @param menuExpectedName : expected menu item name/text
     */
    private void checkMenuItem(JMenuItem menuItem, String menuExpectedName) {
        JMenuItemOperator menuItemOperator = new JMenuItemOperator(menuItem);
        AtomicBoolean menuItemActionStatus = new AtomicBoolean(false);
        menuItemOperator.addActionListener(event -> menuItemActionStatus.set(true));
        menuItemOperator.waitStateOnQueue((component)
                -> menuExpectedName.equals(((JMenuItem)component).getText()));
        menuItemOperator.push();
        menuItemOperator.waitStateOnQueue(comp -> menuItemActionStatus.get());
    }

    /**
     * Add menu listener to the operator
     * @param menuOperator : JMenuOperator on which menu listener has to be added
     * @param menuActionStatus : menu action status variable
     */
    private void addMenuListener(JMenuOperator menuOperator,
            AtomicBoolean menuActionStatus) {
        menuOperator.addMenuListener(new MenuListener() {

            @Override
            public void menuSelected(MenuEvent e) {
                menuActionStatus.set(true);
            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

            @Override
            public void menuCanceled(MenuEvent e) {
            }
        });
    }

}
