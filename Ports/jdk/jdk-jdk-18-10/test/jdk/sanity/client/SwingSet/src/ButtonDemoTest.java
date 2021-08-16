/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

import org.jtregext.GuiTestListener;
import com.sun.swingset3.demos.JHyperlink;
import com.sun.swingset3.demos.button.ButtonDemo;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import static org.testng.AssertJUnit.*;
import org.testng.annotations.Test;
import org.jemmy2ext.JemmyExt.ByToolTipChooser;
import static org.jemmy2ext.JemmyExt.EXACT_STRING_COMPARATOR;
import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JFrameOperator;
import static com.sun.swingset3.demos.button.ButtonDemo.*;
import org.jemmy2ext.JemmyExt;
import org.jemmy2ext.JemmyExt.MultiThreadedTryCatch;
import org.testng.annotations.Listeners;

/*
 * @test
 * @key headful
 * @summary Verifies buttons on SwingSet3 ButtonDemo page by clicking each button
 *          and checking model change events. It also verifies tooltips and text
 *          on buttons before and after click.
 *
 * @library /sanity/client/lib/jemmy/src
 * @library /sanity/client/lib/Extensions/src
 * @library /sanity/client/lib/SwingSet3/src
 * @modules java.desktop
 *          java.logging
 * @build org.jemmy2ext.JemmyExt
 * @build com.sun.swingset3.demos.button.ButtonDemo
 * @run testng/timeout=600 ButtonDemoTest
 */
@Listeners(GuiTestListener.class)
public class ButtonDemoTest {

    private static final String[] BUTTON_TEXT_AFTER = {
        DO_IT_AGAIN,};

    private static final String[] BUTTON_TEXT_BEFORE = {
        DO_IT,
        "",
        FIND,
        GO,
        CONNECT,
        "",
        GET_MORE_INFO,
        null
    };

    private static final String[] BUTTON_TOOLTIP = {
        SIMPLE_BUTTON,
        IMAGE_BUTTON,
        BUTTON_WITH_TEXT_AND_IMAGE,
        BUTTON_WITH_BACKGROUND_COLOR,
        BUTTON_WITH_NO_BORDER,
        BUTTON_WITH_ROLLOVER_IMAGE,
        JAVA_SE_URL,
        JAVA_BLOGS_URL
    };

    private static final String[] GOLDEN = {
        "isArmed = false, isEnabled = true, isPressed = false, isSelected = false",
        "isArmed = true, isEnabled = true, isPressed = false, isSelected = false",
        "isArmed = true, isEnabled = true, isPressed = true, isSelected = false",
        "isArmed = true, isEnabled = true, isPressed = false, isSelected = false",
        "isArmed = false, isEnabled = true, isPressed = false, isSelected = false"
    };

    @Test(dataProvider = "availableLookAndFeels", dataProviderClass = TestHelpers.class)
    public void test(String lookAndFeel) throws Exception {
        UIManager.setLookAndFeel(lookAndFeel);

        new ClassReference(ButtonDemo.class.getCanonicalName()).startApplication();

        JFrameOperator mainFrame = new JFrameOperator(DEMO_TITLE);
        mainFrame.setComparator(EXACT_STRING_COMPARATOR);

        // Check all the buttons
        for (int i = 0; i < BUTTON_TOOLTIP.length; i++) {
            String tooltip = BUTTON_TOOLTIP[i];

            JButtonOperator button = new JButtonOperator(mainFrame, new ByToolTipChooser(tooltip));

            assertEquals(BUTTON_TEXT_BEFORE[i], button.getText());

            // Two buttons are hyperlinks, we don't want to click them
            if (!button.getSource().getClass().equals(JHyperlink.class)) {
                checkButton(button);
            }

            if (BUTTON_TEXT_AFTER.length > i) {
                assertEquals(BUTTON_TEXT_AFTER[i], button.getText());
            } else {
                assertEquals(BUTTON_TEXT_BEFORE[i], button.getText());
            }
        }
    }

    private void checkButton(JButtonOperator button) throws Exception {
        MultiThreadedTryCatch tryCatch = new JemmyExt.MultiThreadedTryCatch();
        try {
            BlockingQueue<String> modelStateChanges = new ArrayBlockingQueue<>(GOLDEN.length);
            button.getQueueTool().invokeAndWait(() -> {
                try {
                    JButton jButton = (JButton) button.getSource();
                    ButtonModel model = jButton.getModel();
                    String line = toString(model);
                    System.out.println("Inital: " + line);
                    modelStateChanges.add(line);
                    model.addChangeListener((ChangeEvent e) -> {
                        try {
                            String line2 = toString(model);
                            System.out.println("ChangeEvent: " + line2);

                            // We are only interested in the first GOLDEN.length events
                            if (modelStateChanges.remainingCapacity() > 0) {
                                modelStateChanges.add(line2);
                            }
                        } catch (RuntimeException | Error t) {
                            tryCatch.register(t);
                        }
                    });
                } catch (Error error) {
                    // All exceptions are already handled by Jemmy but Errors are not
                    tryCatch.register(error);
                    throw error;
                }
            });

            assertEquals("Initial state check", GOLDEN[0], modelStateChanges.take());

            button.clickMouse();

            for (int state = 1; state < GOLDEN.length; state++) {
                assertEquals(GOLDEN[state], modelStateChanges.take());
            }
        } catch (RuntimeException | Error | InterruptedException t) {
            tryCatch.registerRoot(t);
        } finally {
            tryCatch.throwRegistered();
        }
    }

    private static String toString(ButtonModel model) {
        return "isArmed = " + model.isArmed()
                + ", isEnabled = " + model.isEnabled()
                + ", isPressed = " + model.isPressed()
                + ", isSelected = " + model.isSelected();
    }

}
