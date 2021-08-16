/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.swingset3.demos.optionpane.OptionPaneDemo;
import static com.sun.swingset3.demos.optionpane.OptionPaneDemo.*;

import javax.swing.UIManager;

import org.jtregext.GuiTestListener;

import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.operators.Operator.DefaultStringComparator;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JComboBoxOperator;
import org.netbeans.jemmy.operators.JDialogOperator;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JLabelOperator;
import org.netbeans.jemmy.operators.JTextFieldOperator;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.*;


/*
 * @test
 * @key headful
 * @summary Verifies SwingSet3 OptionPaneDemo page by opening all the dialogs
 *          and choosing different options in them.
 *
 * @library /sanity/client/lib/jemmy/src
 * @library /sanity/client/lib/Extensions/src
 * @library /sanity/client/lib/SwingSet3/src
 * @modules java.desktop
 *          java.logging
 * @build org.jemmy2ext.JemmyExt
 * @build com.sun.swingset3.demos.optionpane.OptionPaneDemo
 * @run testng/timeout=600 OptionPaneDemoTest
 */
@Listeners(GuiTestListener.class)
public class OptionPaneDemoTest {

    public static final String SOME_TEXT_TO_TYPE = "I am some text";
    public static final String MESSAGE = UIManager.getString("OptionPane.messageDialogTitle");
    public static final String OK = "OK";
    public static final String CANCEL = "Cancel";
    public static final String INPUT = UIManager.getString("OptionPane.inputDialogTitle");
    public static final String TEXT_TO_TYPE = "Hooray! I'm a textField";
    public static final String NO = "No";
    public static final String YES = "Yes";
    public static final String SELECT_AN_OPTION = UIManager.getString("OptionPane.titleText");

    @Test(dataProvider = "availableLookAndFeels", dataProviderClass = TestHelpers.class)
    public void test(String lookAndFeel) throws Exception {
        UIManager.setLookAndFeel(lookAndFeel);

        new ClassReference(OptionPaneDemo.class.getCanonicalName()).startApplication();

        JFrameOperator frame = new JFrameOperator(DEMO_TITLE);

        showInputDialog(frame);
        showWarningDialog(frame);
        showMessageDialog(frame);
        showComponentDialog(frame);
        showConfirmationDialog(frame);
    }

    private void checkMessage(String message) {
        JDialogOperator jdo = new JDialogOperator(MESSAGE);
        new JLabelOperator(jdo, message);
        new JButtonOperator(jdo, OK).push();
        jdo.waitClosed();
    }

    private void useInputDialog(JFrameOperator jfo, String textToType, String buttonToPush) {
        new JButtonOperator(jfo, INPUT_BUTTON).pushNoBlock();
        JDialogOperator jdo = new JDialogOperator(INPUT);
        if (textToType != null) {
            JTextFieldOperator jto = new JTextFieldOperator(jdo);
            jto.typeText(textToType);
            jto.waitText(textToType);
        }
        new JButtonOperator(jdo, buttonToPush).push();
        jdo.waitClosed();
    }

    public void showInputDialog(JFrameOperator jfo) throws Exception {
        // Cancel with text case
        useInputDialog(jfo, SOME_TEXT_TO_TYPE, CANCEL);
        //TODO: wait for no dialog displayed

        // Cancel with *NO* text case
        useInputDialog(jfo, null, CANCEL);
        //TODO: wait for no dialog displayed

        // Text field has *NO* input
        useInputDialog(jfo, null, OK);
        //TODO: wait for no dialog displayed

        // Text field has input
        {
            final String enteredText = "Rambo";

            useInputDialog(jfo, enteredText, OK);
            checkMessage(enteredText + INPUT_RESPONSE);
        }
    }

    public void showWarningDialog(JFrameOperator jfo) throws Exception {
        new JButtonOperator(jfo, WARNING_BUTTON).pushNoBlock();

        JDialogOperator jdo = new JDialogOperator(WARNING_TITLE);

        new JButtonOperator(jdo, OK).push();

        jdo.waitClosed();
    }

    public void showMessageDialog(JFrameOperator jfo) throws Exception {
        new JButtonOperator(jfo, MESSAGE_BUTTON).pushNoBlock();

        JDialogOperator jdo = new JDialogOperator(MESSAGE);

        new JButtonOperator(jdo, OK).push();

        jdo.waitClosed();
    }

    private void callADialogAndClose(JFrameOperator jfo, String buttonToOpenDialog,
            String dialogTitle, String buttonToPush) {
        new JButtonOperator(jfo, buttonToOpenDialog).pushNoBlock();
        JDialogOperator jdo = new JDialogOperator(dialogTitle);
        new JButtonOperator(jdo, buttonToPush).push();
        jdo.waitClosed();
    }

    public void showComponentDialog(JFrameOperator jfo) throws Exception {
        // Case: Cancel
        callADialogAndClose(jfo, COMPONENT_BUTTON, COMPONENT_TITLE, COMPONENT_OP5);
        //TODO: wait for no dialog displayed

        // Case: Yes option selected
        {
            callADialogAndClose(jfo, COMPONENT_BUTTON, COMPONENT_TITLE, COMPONENT_OP1);
            checkMessage(COMPONENT_R1);
        }

        // Case: No option selected
        {
            callADialogAndClose(jfo, COMPONENT_BUTTON, COMPONENT_TITLE, COMPONENT_OP2);
            checkMessage(COMPONENT_R2);
        }

        // Case: Maybe option selected
        {
            callADialogAndClose(jfo, COMPONENT_BUTTON, COMPONENT_TITLE, COMPONENT_OP3);
            checkMessage(COMPONENT_R3);
        }

        // Case: Probably option selected
        {
            callADialogAndClose(jfo, COMPONENT_BUTTON, COMPONENT_TITLE, COMPONENT_OP4);
            checkMessage(COMPONENT_R4);
        }

        // Case TextField and ComboBox functional
        {
            new JButtonOperator(jfo, COMPONENT_BUTTON).push();

            JDialogOperator jdo = new JDialogOperator(COMPONENT_TITLE);

            JTextFieldOperator jto = new JTextFieldOperator(jdo);
            jto.clearText();
            jto.typeText(TEXT_TO_TYPE);
            jto.waitText(TEXT_TO_TYPE);

            JComboBoxOperator jcbo = new JComboBoxOperator(jdo);
            jcbo.selectItem(2);
            jcbo.waitItemSelected(2);

            new JButtonOperator(jdo, COMPONENT_OP5).push();
            jdo.waitClosed();
            //TODO: wait for no dialog displayed
        }
    }

    public void showConfirmationDialog(JFrameOperator jfo) throws Exception {
        // Case: Yes option selected
        {
            callADialogAndClose(jfo, CONFIRM_BUTTON, SELECT_AN_OPTION, YES);
            checkMessage(CONFIRM_YES);
        }

        // Case: No option selected
        {
            callADialogAndClose(jfo, CONFIRM_BUTTON, SELECT_AN_OPTION, NO);
            checkMessage(CONFIRM_NO);
        }

        // Case: Cancel option selected
        {
            callADialogAndClose(jfo, CONFIRM_BUTTON, SELECT_AN_OPTION, CANCEL);
            //TODO: wait for no dialog displayed
        }
    }

}
