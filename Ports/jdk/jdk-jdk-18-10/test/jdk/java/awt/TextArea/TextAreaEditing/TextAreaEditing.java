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
 @bug 8040322 8060137
 @library ../../regtesthelpers
 @build Util
 @summary Test TextArea APIs replaceRange, insert, append & setText
 @run main TextAreaEditing
 */

import java.awt.Frame;
import java.awt.Robot;
import java.awt.TextArea;
import java.awt.AWTException;
import java.awt.event.KeyEvent;
import test.java.awt.regtesthelpers.Util;

public class TextAreaEditing {

    final static Robot robot = Util.createRobot();
    private int testFailCount;
    private boolean isTestFail;
    private StringBuilder testFailMessage;

    private Frame mainFrame;
    private TextArea textArea;

    private TextAreaEditing() {
        testFailMessage = new StringBuilder();
        mainFrame = new Frame();
        mainFrame.setSize(200, 200);

        textArea = new TextArea();
        mainFrame.add(textArea);
        mainFrame.setVisible(true);
    }

    private void dispose() {
        if (mainFrame != null) {
            mainFrame.dispose();
        }
    }

    public static void main(String[] s) {
        TextAreaEditing textArea = new TextAreaEditing();
        textArea.testReplaceRange();
        textArea.testInsert();
        textArea.testAppend();
        textArea.testSetText();
        textArea.checkFailures();
        textArea.dispose();
    }

    private void testReplaceRange() {
        textArea.setText(null);
        textArea.replaceRange("Replace", 0, 0);
        textArea.setText(null);
        checkTest("");

        textArea.setText("SetText");
        textArea.replaceRange("Replace", 0, 3);
        checkTest("ReplaceText");

        textArea.replaceRange("String", textArea.getText().length(),
                textArea.getText().length());
        checkTest("ReplaceTextString");

        textArea.replaceRange("String", 0, 0);
        checkTest("StringReplaceTextString");

        textArea.replaceRange("replaceRange", 0, textArea.getText().length());
        checkTest("replaceRange");
    }

    private void testInsert() {
        textArea.setText(null);
        textArea.insert("Insert", 0);
        textArea.setText("");
        checkTest("");

        textArea.setText("SetText");
        textArea.insert("Insert", 3);
        checkTest("SetInsertText");

        textArea.insert("Insert", 0);
        checkTest("InsertSetInsertText");

        textArea.insert("Insert", textArea.getText().length());
        checkTest("InsertSetInsertTextInsert");
    }

    private void testAppend() {
        textArea.setText(null);
        textArea.append("Append");
        textArea.setText(null);
        checkTest("");

        textArea.setText("SetText");
        textArea.append("Append");
        checkTest("SetTextAppend");

        textArea.append("");
        checkTest("SetTextAppend");
        textArea.setText("");
        checkTest("");
    }

    private void testSetText() {
        textArea.setText(null);
        textArea.requestFocus();
        Util.clickOnComp(textArea, robot);
        Util.waitForIdle(robot);
        robot.keyPress(KeyEvent.VK_A);
        robot.delay(5);
        robot.keyRelease(KeyEvent.VK_A);
        Util.waitForIdle(robot);
        textArea.setText(null);
        checkTest("");
        textArea.setText("CaseSensitive");
        checkTest("CaseSensitive");
        textArea.setText("caseSensitive");
        checkTest("caseSensitive");

    }

    private void checkTest(String str) {
        if (str != null && !str.equals(textArea.getText())) {
            testFailMessage.append("TestFail line : ");
            testFailMessage.append(Thread.currentThread().getStackTrace()[2].
                    getLineNumber());
            testFailMessage.append(" TextArea string : \"");
            testFailMessage.append(textArea.getText());
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
