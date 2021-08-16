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

import com.sun.swingset3.demos.textfield.JHistoryTextField;
import com.sun.swingset3.demos.textfield.TextFieldDemo;
import static com.sun.swingset3.demos.textfield.TextFieldDemo.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import javax.swing.JFormattedTextField;
import javax.swing.UIManager;

import static org.jemmy2ext.JemmyExt.*;

import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.operators.ContainerOperator;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JLabelOperator;
import org.netbeans.jemmy.operators.JPasswordFieldOperator;
import org.netbeans.jemmy.operators.JTextFieldOperator;

import org.jtregext.GuiTestListener;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.*;

/*
 * @test
 * @key headful
 * @summary Verifies SwingSet3 TextFieldDemo by entering text in each field and
 *          checking that app reacts accordingly.
 *
 * @library /sanity/client/lib/jemmy/src
 * @library /sanity/client/lib/Extensions/src
 * @library /sanity/client/lib/SwingSet3/src
 * @modules java.desktop
 *          java.logging
 * @build org.jemmy2ext.JemmyExt
 * @build com.sun.swingset3.demos.textfield.TextFieldDemo
 * @run testng/timeout=600 TextFieldDemoTest
 */
@Listeners(GuiTestListener.class)
public class TextFieldDemoTest {

    @Test(dataProvider = "availableLookAndFeels", dataProviderClass = TestHelpers.class)
    public void test(String lookAndFeel) throws Exception {
        UIManager.setLookAndFeel(lookAndFeel);
        new ClassReference(TextFieldDemo.class.getCanonicalName()).startApplication();

        JFrameOperator frame = new JFrameOperator(DEMO_TITLE);

        historyTextField(frame);
        dateTextField(frame);
        passwordField(frame);
    }

    private void historyTextField(JFrameOperator jfo) throws Exception {
        JTextFieldOperator jtfo = new JTextFieldOperator(jfo, new ByClassChooser(JHistoryTextField.class));
        jtfo.typeText("cat");

        jtfo.pressKey(KeyEvent.VK_DOWN);
        jtfo.pressKey(KeyEvent.VK_DOWN);
        jtfo.pressKey(KeyEvent.VK_ENTER);

        final String expectedValue = "category";
        jtfo.waitText(expectedValue);
        assertEquals("Select History Item", expectedValue, jtfo.getText());
    }

    public void dateTextField(JFrameOperator jfo) throws Exception {
        JTextFieldOperator jtfo = new JTextFieldOperator(jfo,
                new ByClassChooser(JFormattedTextField.class));
        ContainerOperator<?> containerOperator = new ContainerOperator<>(jtfo.getParent());
        JButtonOperator jbo = new JButtonOperator(containerOperator, GO);
        JLabelOperator dowLabel = new JLabelOperator(containerOperator);
        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
        calendar.setTime((Date) getUIValue(jtfo, jtf -> ((JFormattedTextField)jtf).getValue()));

        // Check default date Day of the Week
        jbo.push();
        dowLabel.waitText(calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH));

        // Check Custom Day of the Week
        calendar.set(2012, 9, 11); // Represents "Oct 11, 2012"
        Date date = calendar.getTime();
        String dateString = jtfo.getQueueTool().invokeAndWait(
                new QueueTool.QueueAction<String>("Formatting the value using JFormattedTextField formatter") {

            @Override
            public String launch() throws Exception {
                return ((JFormattedTextField) jtfo.getSource()).getFormatter().valueToString(date);
            }
        });
        System.out.println("dateString = " + dateString);
        jtfo.enterText(dateString);

        jbo.push();
        dowLabel.waitText("Thursday");
    }

    public void passwordField(JFrameOperator jfo) throws Exception {
        JPasswordFieldOperator password1 = new JPasswordFieldOperator(jfo, 0);
        JPasswordFieldOperator password2 = new JPasswordFieldOperator(jfo, 1);

        password1.typeText("password");
        password2.typeText("password");

        // Check Matching Passwords
        password1.waitState(new ComponentChooser() {
            public boolean checkComponent(Component comp) {
                return password1.getBackground().equals(Color.green) &&
                       password2.getBackground().equals(Color.green);
            }
            public String getDescription() {
                return "Passwords to match";
            }
        });

        // Check non-matching passwords
        final Color backgroundColor = UIManager.getColor("TextField.background");
        password2.typeText("passwereertegrs");
        password1.waitState(new ComponentChooser() {
            public boolean checkComponent(Component comp) {
                return password1.getBackground().equals(backgroundColor) &&
                       password2.getBackground().equals(backgroundColor);
            }
            public String getDescription() {
                return "Passwords not to match";
            }
        });
    }

}
