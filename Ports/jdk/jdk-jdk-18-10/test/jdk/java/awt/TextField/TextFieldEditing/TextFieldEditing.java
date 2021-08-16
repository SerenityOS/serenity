/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 @bug 8060137
 @library ../../regtesthelpers
 @build Util
 @summary Test TextField setText API
 @run main TextFieldEditing
 */

import java.awt.Frame;
import java.awt.Robot;
import java.awt.TextField;
import java.awt.AWTException;
import java.awt.event.KeyEvent;
import test.java.awt.regtesthelpers.Util;

public class TextFieldEditing {

    final static Robot robot = Util.createRobot();
    private int testFailCount;
    private boolean isTestFail;
    private StringBuilder testFailMessage;

    private Frame mainFrame;
    private TextField textField;

    private TextFieldEditing() {
        testFailMessage = new StringBuilder();
        mainFrame = new Frame();
        mainFrame.setSize(200, 200);

        textField = new TextField();
        mainFrame.add(textField);
        mainFrame.setVisible(true);
    }

    private void dispose() {
        if (mainFrame != null) {
            mainFrame.dispose();
        }
    }

    public static void main(String[] s) {
        TextFieldEditing textField = new TextFieldEditing();
        textField.testSetText();
        textField.checkFailures();
        textField.dispose();
    }

    private void testSetText() {
        textField.setText(null);
        textField.requestFocus();
        Util.clickOnComp(textField, robot);
        Util.waitForIdle(robot);
        robot.keyPress(KeyEvent.VK_A);
        robot.delay(5);
        robot.keyRelease(KeyEvent.VK_A);
        Util.waitForIdle(robot);
        textField.setText(null);
        checkTest("");
        textField.setText("CaseSensitive");
        checkTest("CaseSensitive");
        textField.setText("caseSensitive");
        checkTest("caseSensitive");
    }

    private void checkTest(String str) {
        if (str != null && !str.equals(textField.getText())) {
            testFailMessage.append("TestFail line : ");
            testFailMessage.append(Thread.currentThread().getStackTrace()[2].
                    getLineNumber());
            testFailMessage.append(" TextField string : \"");
            testFailMessage.append(textField.getText());
            testFailMessage.append("\" does not match expected string : \"");
            testFailMessage.append(str).append("\"");
            testFailMessage.append(System.getProperty("line.separator"));
            testFailCount++;
            isTestFail = true;
        }
    }

    private void checkFailures() {
        if (isTestFail) {
            testFailMessage.insert(0, "Test Fail count : " + testFailCount
                    + System.getProperty("line.separator"));
            dispose();
            throw new RuntimeException(testFailMessage.toString());
        }
    }
}
