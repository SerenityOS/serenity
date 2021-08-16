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
 @bug 8055197 7186036
 @summary TextField should replace EOL character with space character
 @run main EOLTest
 */

import java.awt.Frame;
import java.awt.TextField;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

public class EOLTest {

    private Frame mainFrame;
    private TextField textField;
    private String testStrEOL;
    private boolean isTestFail;
    private int testFailCount;
    StringBuilder testFailMessage;
    private String expectedString = "Row1 Row2 Row3";

    public EOLTest() {
        mainFrame = new Frame();
        mainFrame.setSize(200, 200);
        mainFrame.setVisible(true);
        testFailMessage = new StringBuilder();
        testStrEOL = "Row1" + System.lineSeparator() + "Row2\nRow3";
    }

    private void testConstructor1() {
        textField = new TextField(testStrEOL);
        textField.setSize(200, 100);
        mainFrame.add(textField);
        checkTest();
        mainFrame.remove(textField);
    }

    private void testConstructor2() {
        textField = new TextField(30);
        textField.setSize(200, 100);
        mainFrame.add(textField);
        textField.setText(testStrEOL);
        checkTest();
        mainFrame.remove(textField);
    }

    private void testConstructor3() {
        textField = new TextField(testStrEOL, 30);
        textField.setSize(200, 100);
        mainFrame.add(textField);
        checkTest();
        mainFrame.remove(textField);
    }

    private void testSetText() {
        textField = new TextField();
        textField.setSize(200, 100);
        textField.setText(testStrEOL);
        mainFrame.add(textField);
        checkTest();
        mainFrame.remove(textField);
    }

    private void testDeserialization() {
        TextField textFieldToSerialize = new TextField(testStrEOL);
        textFieldToSerialize.setSize(200, 100);
        mainFrame.add(textFieldToSerialize);
        try {
            // Serialize TextField object "textFieldToSerialize".
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutput outStream = new ObjectOutputStream(baos);
            outStream.writeObject(textFieldToSerialize);

            // Search the text variable data through serialized object stream.
            byte[] streamedBytes = baos.toByteArray();
            int foundLoc = 0;
            for (int i = 0; i < streamedBytes.length; ++i) {
                if (streamedBytes[i] == expectedString.charAt(0)) {
                    foundLoc = i;
                    int j = 1;
                    for (; j < expectedString.length(); ++j) {
                        if (streamedBytes[i+j] != expectedString.charAt(j)) {
                            break;
                        }
                    }
                    if (j == expectedString.length()) {
                        break;
                    }
                }
                foundLoc = -1;
            }

            if (foundLoc == -1) {
                // Could not find text data in serialized object stream.
                throw new Exception("Could not find text data in serialized "
                    + "object stream.");
            }
            // Replace space character from serialized stream with
            // EOL character for testing de-serialization.
            String EOLChar = System.lineSeparator();
            String newExpectedString = "";
            for (int i = foundLoc, j = 0; j < expectedString.length(); ++i, ++j) {
                newExpectedString += (char)(streamedBytes[i]);
                if (streamedBytes[i] == ' ') {
                    int k = 0;
                    for (; k < EOLChar.length(); ++k) {
                        streamedBytes[i + k] = (byte) EOLChar.charAt(k);
                    }
                    i += k-1;
                    j += k-1;
                }
            }
            // New line character varies with platform,
            // ex. For windows '\r\n', for linux '\n'.
            // While replacing space from serialized object stream, the length
            // of EOL character will affect the expected string as well.
            expectedString = newExpectedString;

            // De-serialize TextField object stream.
            ByteArrayInputStream bais = new ByteArrayInputStream(streamedBytes);
            ObjectInput inStream = new ObjectInputStream(bais);
            textField = (TextField) inStream.readObject();
        } catch (Exception ex) {
            // Serialization or De-serialization failed.
            // Create textField with empty string to show failure.
            ex.printStackTrace();
            textField = new TextField();
        }

        checkTest();
        mainFrame.remove(textFieldToSerialize);
    }

    private void checkTest() {
        if (!textField.getText().equals(expectedString)) {
            testFailMessage.append("TestFail line : ");
            testFailMessage.append(Thread.currentThread().getStackTrace()[2].
                    getLineNumber());
            testFailMessage.append(" TextField.getText() : \"");
            testFailMessage.append(textField.getText());
            testFailMessage.append("\" does not match expected string : \"");
            testFailMessage.append(expectedString).append("\"");
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

    private void dispose() {
        if (mainFrame != null) {
            mainFrame.dispose();
        }
    }

    public static void main(String[] args) {
        EOLTest testEOL = new EOLTest();
        testEOL.testConstructor1();
        testEOL.testConstructor2();
        testEOL.testConstructor3();
        testEOL.testSetText();
        testEOL.testDeserialization();
        testEOL.checkFailures();
        testEOL.dispose();
    }
}
