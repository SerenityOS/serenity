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

import org.jtregext.GuiTestListener;
import com.sun.swingset3.DemoProperties;
import com.sun.swingset3.demos.togglebutton.DirectionPanel;
import com.sun.swingset3.demos.togglebutton.LayoutControlPanel;
import com.sun.swingset3.demos.togglebutton.ToggleButtonDemo;
import static com.sun.swingset3.demos.togglebutton.ToggleButtonDemo.*;
import java.util.function.BiFunction;
import javax.swing.UIManager;
import org.jemmy2ext.JemmyExt.ByClassChooser;
import static org.jemmy2ext.JemmyExt.EXACT_STRING_COMPARATOR;
import static org.jemmy2ext.JemmyExt.getBorderTitledJPanelOperator;
import static org.jemmy2ext.JemmyExt.getLabeledContainerOperator;
import static org.testng.AssertJUnit.*;
import org.testng.annotations.Test;
import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.operators.ContainerOperator;
import org.netbeans.jemmy.operators.JCheckBoxOperator;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JRadioButtonOperator;
import org.netbeans.jemmy.operators.JTabbedPaneOperator;
import org.testng.annotations.Listeners;

/*
 * @test
 * @key headful
 * @summary Verifies SwingSet3 ToggleButtonDemo by toggling each radio button,
 *          each checkbox and each location of the direction dial toggle.
 *          It verifies initial selected values for all the elements and checks
 *          that those change upon clicking. When toggling radio buttons it
 *          verifies that other radio buttons in the same group are not longer
 *          selected.
 *
 * @library /sanity/client/lib/jemmy/src
 * @library /sanity/client/lib/Extensions/src
 * @library /sanity/client/lib/SwingSet3/src
 * @modules java.desktop
 *          java.logging
 * @build org.jemmy2ext.JemmyExt
 * @build com.sun.swingset3.demos.togglebutton.ToggleButtonDemo
 * @run testng/timeout=600 ToggleButtonDemoTest
 */
@Listeners(GuiTestListener.class)
public class ToggleButtonDemoTest {

    @Test(dataProvider = "availableLookAndFeels", dataProviderClass = TestHelpers.class)
    public void test(String lookAndFeel) throws Exception {
        UIManager.setLookAndFeel(lookAndFeel);
        new ClassReference(ToggleButtonDemo.class.getCanonicalName()).startApplication();

        JFrameOperator mainFrame = new JFrameOperator(ToggleButtonDemo.class.getAnnotation(DemoProperties.class).value());
        JTabbedPaneOperator tabPane = new JTabbedPaneOperator(mainFrame);

        // Radio Button Toggles
        testRadioButtons(getBorderTitledJPanelOperator(mainFrame, TEXT_RADIO_BUTTONS), 3, null);
        testRadioButtons(getBorderTitledJPanelOperator(mainFrame, IMAGE_RADIO_BUTTONS), 3, null);
        testRadioButtons(getLabeledContainerOperator(mainFrame, PAD_AMOUNT), 3, (t, i) -> DEFAULT.equals(t));

        // switch to the Check Boxes Tab
        tabPane.selectPage(CHECK_BOXES);

        // Check Box Toggles
        ContainerOperator<?> textCheckBoxesJPanel = getBorderTitledJPanelOperator(mainFrame, TEXT_CHECKBOXES);
        testCheckBox(textCheckBoxesJPanel, CHECK1, false);
        testCheckBox(textCheckBoxesJPanel, CHECK2, false);
        testCheckBox(textCheckBoxesJPanel, CHECK3, false);

        ContainerOperator<?> imageCheckBoxesJPanel = getBorderTitledJPanelOperator(mainFrame, IMAGE_CHECKBOXES);
        testCheckBox(imageCheckBoxesJPanel, CHECK1, false);
        testCheckBox(imageCheckBoxesJPanel, CHECK2, false);
        testCheckBox(imageCheckBoxesJPanel, CHECK3, false);

        ContainerOperator<?> displayOptionsContainer = getLabeledContainerOperator(mainFrame, DISPLAY_OPTIONS);
        testCheckBox(displayOptionsContainer, PAINT_BORDER, false);
        testCheckBox(displayOptionsContainer, PAINT_FOCUS, true);
        testCheckBox(displayOptionsContainer, ENABLED, true);
        testCheckBox(displayOptionsContainer, CONTENT_FILLED, true);

        // Direction Button Toggles
        testToggleButtons(mainFrame);
    }

    /**
     * The interface is invoked for each radio button providing its name and
     * index and it should return whether the radio button has to be selected.
     */
    private static interface SelectedRadioButton extends BiFunction<String, Integer, Boolean> {
    }

    /**
     * Tests a group of radio buttons
     *
     * @param parent container containing the buttons
     * @param radioButtonCount number of radio buttons
     * @param selectedRadioButton initial selected radio button
     */
    private void testRadioButtons(ContainerOperator<?> parent, int radioButtonCount, SelectedRadioButton selectedRadioButton) {
        JRadioButtonOperator[] jrbo = new JRadioButtonOperator[radioButtonCount];
        for (int i = 0; i < radioButtonCount; i++) {
            jrbo[i] = new JRadioButtonOperator(parent, i);
            if (selectedRadioButton != null && selectedRadioButton.apply(jrbo[i].getText(), i)) {
                assertTrue("Radio Button " + i + " is initially selected", jrbo[i].isSelected());
            } else {
                assertFalse("Radio Button " + i + " is initially not selected", jrbo[i].isSelected());
            }
        }

        for (int i = 0; i < radioButtonCount; i++) {
            jrbo[i].push();
            jrbo[i].waitSelected(true);

            for (int j = 0; j < radioButtonCount; j++) {
                if (i != j) {
                    jrbo[j].waitSelected(false);
                }
            }
        }
    }

    /**
     * Will change the state of the CheckBox then change back to initial state.
     *
     * @param parent
     * @param text
     * @param expectedValue
     * @throws Exception
     */
    private void testCheckBox(ContainerOperator<?> parent, String text, boolean expectedValue) {

        System.out.println("Testing " + text);
        parent.setComparator(EXACT_STRING_COMPARATOR);
        JCheckBoxOperator jcbo = new JCheckBoxOperator(parent, text);
        jcbo.waitSelected(expectedValue);

        // click check box (toggle the state)
        jcbo.push();
        jcbo.waitSelected(!expectedValue);

        jcbo.push();
        jcbo.waitSelected(expectedValue);
    }


    /*
     * testDirectionRadioButtons(JFrameOperator) will toggle each position of
     * the direction radio button panels
     */
    private void testToggleButtons(JFrameOperator jfo) {
        ComponentChooser directionPanelChooser = new ByClassChooser(DirectionPanel.class);

        String text_Position = LayoutControlPanel.TEXT_POSITION;
        ContainerOperator<?> textPositionContainer = getLabeledContainerOperator(jfo, text_Position);
        ContainerOperator<?> directionPanelOperator = new ContainerOperator<>(textPositionContainer, directionPanelChooser);
        testRadioButtons(directionPanelOperator, 9, (t, i) -> i == 5);

        // Unfortunately, both directionPanels are in the same parent container
        // so we have to use indexes here.
        // There is no guarantee that if the UI changes, the code would be still
        // valid in terms of which directionPanel is checked first. However, it
        // does guarantee that two different directionPanels are checked.
        String content_Alignment = LayoutControlPanel.CONTENT_ALIGNMENT;
        ContainerOperator<?> contentAlignmentContainer = getLabeledContainerOperator(jfo, content_Alignment);
        ContainerOperator<?> directionPanelOperator2 = new ContainerOperator<>(contentAlignmentContainer, directionPanelChooser,
                contentAlignmentContainer.getSource() == textPositionContainer.getSource() ? 1 : 0);
        testRadioButtons(directionPanelOperator2, 9, (t, i) -> i == 4);

    }

}
