/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

import static jdk.test.lib.Asserts.*;



public class TestDialog extends Dialog implements ActionListener,
    FocusListener, WindowFocusListener, WindowListener {

    public Button closeButton, openButton, dummyButton;

    public Flag closeClicked, openClicked, dummyClicked;
    public Flag closeGained,  openGained,  dummyGained;
    public Flag closeLost,    openLost,    dummyLost;
    public Flag focusGained, focusLost;
    public Flag activated;

    public Panel topPanel;

    public static int delay = 500;
    public static int keyDelay = 100;

    public TestDialog(Frame frame) {
        super(frame);
        initializeGUI();
    }

    public TestDialog(Dialog dialog) {
        super(dialog);
        initializeGUI();
    }

    public void resetStatus() {
        activated.reset();
        focusGained.reset();
        closeGained.reset();
        openGained.reset();
        closeClicked.reset();
        openClicked.reset();
    }

    private void initFlags() {
        closeClicked = new Flag();
        openClicked  = new Flag();
        dummyClicked = new Flag();
        closeGained  = new Flag();
        openGained   = new Flag();
        dummyGained  = new Flag();
        closeLost    = new Flag();
        openLost     = new Flag();
        dummyLost    = new Flag();
        focusGained  = new Flag();
        focusLost    = new Flag();
        activated    = new Flag();
    }

    private void initializeGUI() {

        initFlags();
        this.setTitle("Dialog");

        this.addWindowFocusListener(this);
        this.addWindowListener(this);

        this.setLayout(new GridLayout(3, 1));

        topPanel = new Panel();
        topPanel.setFocusable(false);
        this.add(topPanel);

        Panel p = new Panel();
        p.setLayout(new GridLayout(1, 3));

        closeButton = new Button("Close");
        closeButton.addActionListener(this);
        closeButton.addFocusListener(this);

        openButton = new Button("Open");
        openButton.addActionListener(this);
        openButton.addFocusListener(this);

        dummyButton = new Button("Dummy");
        dummyButton.addActionListener(this);
        dummyButton.addFocusListener(this);

        p.add(closeButton);
        p.add(openButton);
        p.add(dummyButton);

        this.add(p);

        Panel bottomPanel = new Panel();
        bottomPanel.setFocusable(false);
        this.add(bottomPanel);

        setSize(150, 150);
    }

    public void doOpenAction()  {}
    public void doCloseAction() {}
    public void doDummyAction() {}

    @Override
    public void actionPerformed(ActionEvent event) {
        if (closeButton.equals(event.getSource())) {
            closeClicked.flagTriggered();
            doCloseAction();
        } else if (openButton.equals(event.getSource())) {
            openClicked.flagTriggered();
            doOpenAction();
        } else if (dummyButton.equals(event.getSource())) {
            dummyClicked.flagTriggered();
            doDummyAction();
        }
    }

    @Override
    public void focusGained(FocusEvent event) {
        if (closeButton.equals(event.getSource())) {
            closeGained.flagTriggered();
        } else if (openButton.equals(event.getSource())) {
            openGained.flagTriggered();
        } else if (dummyButton.equals(event.getSource())) {
            dummyGained.flagTriggered();
        }
    }

    @Override
    public void focusLost(FocusEvent event) {
        if (closeButton.equals(event.getSource())) {
            closeLost.flagTriggered();
        } else if (openButton.equals(event.getSource())) {
            openLost.flagTriggered();
        } else if (dummyButton.equals(event.getSource())) {
            dummyLost.flagTriggered();
        }
    }

    @Override
    public void windowGainedFocus(WindowEvent event) {
        focusGained.flagTriggered();
    }

    @Override
    public void windowLostFocus(WindowEvent event) {
        focusLost.flagTriggered();
    }

    @Override
    public void windowActivated(WindowEvent e) {
        activated.flagTriggered();
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
        assertTrue(false, "user closed window");
    }

    @Override
    public void windowDeactivated(WindowEvent e) {}
    @Override
    public void windowDeiconified(WindowEvent e) {}
    @Override
    public void windowIconified(WindowEvent e) {}
    @Override
    public void windowOpened(WindowEvent e) {}

    public void clickButton(Button b, ExtendedRobot robot) {

        try {
            Flag.waitTillShown(b);
        } catch (InterruptedException e) {}

        if ((closeButton.equals(b) || openButton.equals(b) ||
            dummyButton.equals(b)) && robot != null) {
            robot.mouseMove((int) b.getLocationOnScreen().x + b.getSize().width / 2,
                            (int) b.getLocationOnScreen().y + b.getSize().height / 2);
            robot.delay(delay);
            robot.click();
            robot.delay(delay);
        }
    }

    public void clickOpenButton(ExtendedRobot robot) throws Exception {
        clickOpenButton(robot, true, "");
    }

    public void clickOpenButton(ExtendedRobot robot,
                                boolean       refState,
                                String        message) throws Exception {
        openClicked.reset();
        clickButton(openButton, robot);
        openClicked.waitForFlagTriggered();

        String msg = "Clicking the dialog Open button " + (refState ?
            "did not trigger an action." :
            "triggered an action when it should not.");
        assertEQ(openClicked.flag(), refState, msg + " " + message);
    }

    public void clickCloseButton(ExtendedRobot robot) throws Exception {
        clickCloseButton(robot, true, "");
    }

    public void clickCloseButton(ExtendedRobot robot,
                                 boolean       refState,
                                 String        message) throws Exception {
        closeClicked.reset();
        clickButton(closeButton, robot);
        closeClicked.waitForFlagTriggered();

        String msg = "Clicking the dialog Close button " + (refState ?
            "did not trigger an action." :
            "triggered an action when it should not.");
        assertEQ(closeClicked.flag(), refState, msg + " " + message);
    }

    public void clickDummyButton(ExtendedRobot robot) throws Exception {
        clickDummyButton(robot, Flag.ATTEMPTS);
    }

    public void clickDummyButton(ExtendedRobot robot,
                                 int           attempts) throws Exception {
        clickDummyButton(robot, attempts, true, "");
    }

    public void clickDummyButton(ExtendedRobot robot,
                                 int           attempts,
                                 boolean       refState,
                                 String        message) throws Exception {
        dummyClicked.reset();
        clickButton(dummyButton, robot);
        dummyClicked.waitForFlagTriggered(attempts);

        String msg = "Clicking the dialog Dummy button " + (refState ?
            "did not trigger an action." :
            "triggered an action when it should not.");
        assertEQ(dummyClicked.flag(), refState, msg + " " + message);
    }


    private void clickInside(ExtendedRobot robot) throws Exception {

        try {
            Flag.waitTillShown(topPanel);
        } catch (InterruptedException e) {}

        if (robot != null) {
            robot.mouseMove((int) topPanel.getLocationOnScreen().x + topPanel.getSize().width / 2,
                            (int) topPanel.getLocationOnScreen().y + topPanel.getSize().height / 2);
            robot.delay(delay);
            robot.click();
            robot.delay(delay);
        }
    }

    public void transferFocusToDialog(ExtendedRobot robot,
                                      String message,
                                      Button b) throws Exception {
        focusGained.reset();
        clickInside(robot);
        focusGained.waitForFlagTriggered();
        assertTrue(focusGained.flag(),
            "clicking inside the Dialog did not make it focused. " + message);

        robot.waitForIdle(delay);
        if (b != null) {
            assertTrue(b.hasFocus(), "button " + b.getLabel() +
                " did not gain focus when Dialog brought to top");
        }
    }

    public void transferFocusToBlockedDialog(ExtendedRobot robot,
                                             String message,
                                             Button b) throws Exception {
        focusGained.reset();
        clickInside(robot);
        robot.waitForIdle(delay);

        assertFalse(focusGained.flag(),
            "clicking inside a blocked Dialog made it focused. " + message);

        robot.waitForIdle(delay);
        if (b != null) {
            assertFalse(b.hasFocus(),
                "button " + b.getLabel() + " present in a blocked dialog gained focus");
        }
    }

    public void checkBlockedDialog(ExtendedRobot robot,
                                   String message) throws Exception {
        dummyGained.reset();
        dummyClicked.reset();
        focusGained.reset();

        clickButton(dummyButton, robot);

        robot.waitForIdle(delay);

        assertFalse(dummyClicked.flag(),
            "DummyButton on blocked Dialog triggered action when clicked. " + message);

        assertFalse(dummyGained.flag(),
            "DummyButton on blocked Dialog gained focus when clicked. " + message);

        assertFalse(focusGained.flag(),
            "A blocked Dialog gained focus when component clicked. " + message);
    }

    public void checkUnblockedDialog(ExtendedRobot robot,
                                     String message) throws Exception {
        dummyGained.reset();
        dummyClicked.reset();
        clickButton(dummyButton, robot);

        dummyGained.waitForFlagTriggered();
        assertTrue(dummyGained.flag(),
            "DummyButton on Dialog did not gain focus when clicked. " + message);

        dummyClicked.waitForFlagTriggered();
        assertTrue(dummyClicked.flag(),
            "DummyButton on Dialog did not trigger action when clicked. " + message);

        closeGained.reset();
        robot.type(KeyEvent.VK_TAB);

        closeGained.waitForFlagTriggered();
        assertTrue(closeGained.flag(),
            "Tab navigation on Dialog did not happen properly. First " +
            "button did not gain focus. " + message);
    }

    public void checkCloseButtonFocusGained(boolean refState) {
        checkCloseButtonFocusGained(refState, Flag.ATTEMPTS);
    }

    public void checkCloseButtonFocusGained(boolean refState, int attempts) {
        try {
            closeGained.waitForFlagTriggered(attempts);
        } catch (InterruptedException e) {}

        String msg = "dialog Close button ";
        msg += (refState ? "did not gain focus" :
                "gained focus when it should not");

        assertTrue(closeGained.flag() == refState, msg);
    }

    public void checkOpenButtonFocusGained(boolean refState) {
        try {
            openGained.waitForFlagTriggered();
        } catch (InterruptedException e) {}

        String msg = "dialog Open button ";
        msg += (refState ? "did not gain focus" :
                "gained focus when it should not");

        assertTrue(openGained.flag() == refState, msg);
    }

    public void checkOpenButtonFocusLost(boolean refState) {
        checkOpenButtonFocusLost(refState, Flag.ATTEMPTS);
    }

    public void checkOpenButtonFocusLost(boolean refState, int attempts) {
        try {
            openLost.waitForFlagTriggered(attempts);
        } catch (InterruptedException e) {}

        String msg = "dialog Open button ";
        msg += (refState ? "did not lose focus" :
                "lost focus when it should not");

        assertTrue(openLost.flag() == refState, msg);
    }
}
