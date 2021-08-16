/*
* Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.swingset3.demos.colorchooser.ColorChooserDemo.BACKGROUND;
import static com.sun.swingset3.demos.colorchooser.ColorChooserDemo.CHOOSER_TITLE;
import static com.sun.swingset3.demos.colorchooser.ColorChooserDemo.DEMO_TITLE;
import static com.sun.swingset3.demos.colorchooser.ColorChooserDemo.GRADIENT_1;
import static com.sun.swingset3.demos.colorchooser.ColorChooserDemo.GRADIENT_2;
import static com.sun.swingset3.demos.colorchooser.ColorChooserDemo.PERIMETER;

import java.awt.Color;
import java.awt.event.KeyEvent;

import org.jemmy2ext.JemmyExt.ByClassChooser;
import org.jtregext.GuiTestListener;
import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.DialogWaiter;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JColorChooserOperator;
import org.netbeans.jemmy.operators.JComponentOperator;
import org.netbeans.jemmy.operators.JDialogOperator;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JSliderOperator;
import org.netbeans.jemmy.operators.JSpinnerOperator;
import org.netbeans.jemmy.operators.JTabbedPaneOperator;
import org.netbeans.jemmy.operators.JTextFieldOperator;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.sun.swingset3.demos.colorchooser.BezierAnimationPanel;
import com.sun.swingset3.demos.colorchooser.BezierAnimationPanel.BezierColor;
import com.sun.swingset3.demos.colorchooser.ColorChooserDemo;

/*
* @test
* @key headful
* @summary Verifies SwingSet3 ColorChooserDemo by performing simple interaction
*  with all the controls that are shown in the ColorChooserDialog.
*
* @library /sanity/client/lib/jemmy/src
* @library /sanity/client/lib/Extensions/src
* @library /sanity/client/lib/SwingSet3/src
* @modules java.desktop
*          java.logging
* @build org.jemmy2ext.JemmyExt
* @build com.sun.swingset3.demos.colorchooser.ColorChooserDemo
* @run testng ColorChooserDemoTest
*/
@Listeners(GuiTestListener.class)
public class ColorChooserDemoTest {

    private static final String OK_BUTTON_TITLE = "OK";
    private static final String CANCEL_BUTTON_TITLE = "Cancel";
    private static final String RESET_BUTTON_TITLE = "Reset";
    private static final String HSV = "HSV";
    private static final String RGB = "RGB";
    private static final String HSL = "HSL";
    private static final String CMYK = "CMYK";
    private static final int HSV_NUMBER_OF_SLIDERS_AND_SPINNERS = 4;
    private static final int RGB_NUMBER_OF_SLIDERS_AND_SPINNERS = 4;
    private static final int HSL_NUMBER_OF_SLIDERS_AND_SPINNERS = 4;
    private static final int CMYK_NUMBER_OF_SLIDERS_AND_SPINNERS = 5;
    private static final int HSV_HUE_INDEX = 0;
    private static final int HSV_SATURATION_INDEX = 1;
    private static final int HSV_VALUE_INDEX = 2;
    private static final int HSV_TRANSPARENCY_INDEX = 3;
    private static final int HSL_HUE_INDEX = 0;
    private static final int HSL_SATURATION_INDEX = 1;
    private static final int HSL_LIGHTNESS_INDEX = 2;
    private static final int HSL_TRANSPARENCY_INDEX = 3;
    private static final int RGB_RED_INDEX = 0;
    private static final int RGB_GREEN_INDEX = 1;
    private static final int RGB_BLUE_INDEX = 2;
    private static final int RGB_ALPHA_INDEX = 3;
    private static final int RGB_COLORCODE_TEXT_FIELD_INDEX = 4;
    private static final int CMYK_CYAN_INDEX = 0;
    private static final int CMYK_MAGENTA_INDEX = 1;
    private static final int CMYK_YELLOW_INDEX = 2;
    private static final int CMYK_BLACK_INDEX = 3;
    private static final int CMYK_ALPHA_INDEX = 4;

    private final Color resetColor = new Color(125, 125, 125);

    private JDialogOperator colorChooserDialog;
    private JButtonOperator okButton;
    private JButtonOperator cancelButton;
    private JButtonOperator resetButton;
    private JColorChooserOperator colorChooser;
    private JButtonOperator backgroundButton;
    private JButtonOperator gradient1Button;
    private JButtonOperator gradient2Button;
    private JButtonOperator perimeterButton;
    private JTabbedPaneOperator tabOperator;
    private JComponentOperator bezierAnimationPanel;
    private JSliderOperator[] sliders = new JSliderOperator[5];
    private JSpinnerOperator[] spinners = new JSpinnerOperator[5];
    private JButtonOperator lastFocusedButton;

    @Test
    public void test() throws Exception {
        new ClassReference(ColorChooserDemo.class.getCanonicalName()).startApplication();
        JFrameOperator frame = new JFrameOperator(DEMO_TITLE);
        bezierAnimationPanel = new JComponentOperator(frame, new ByClassChooser(BezierAnimationPanel.class));
        initializePanelButtons(frame);
        checkBackgroundColorChooser();
        checkGradient1ColorChooser();
        checkGradient2ColorChooser();
        checkPerimeterColorChooser();
    }

    private void checkBackgroundColorChooser() throws Exception {
        basicCheck(backgroundButton, BezierColor.BACKGROUND);
        checkAllColorChoosers(backgroundButton);
    }

    private void checkGradient1ColorChooser() throws Exception {
        basicCheck(gradient1Button, BezierColor.GRADIENT_A);
    }

    private void checkGradient2ColorChooser() throws Exception {
        basicCheck(gradient2Button, BezierColor.GRADIENT_B);
    }

    private void checkPerimeterColorChooser() throws Exception {
        basicCheck(perimeterButton, BezierColor.OUTER);
    }

    private void pushButtonAndInitialize(JButtonOperator jbo) throws InterruptedException {
        // Wait for focus to return to last focused button
        lastFocusedButton.waitHasFocus();
        jbo.pushNoBlock();
        lastFocusedButton = jbo;
        // Wait till the ColorChooserDemo Dialog Opens
        new DialogWaiter().waitDialog(CHOOSER_TITLE, false, false);
        initializeDialog();
    }

    private void initializePanelButtons(JFrameOperator frame) {
        backgroundButton = new JButtonOperator(frame, BACKGROUND);
        gradient1Button = new JButtonOperator(frame, GRADIENT_1);
        gradient2Button = new JButtonOperator(frame, GRADIENT_2);
        perimeterButton = new JButtonOperator(frame, PERIMETER);
        lastFocusedButton = backgroundButton;
    }

    private void initializeDialog() {
        colorChooserDialog = new JDialogOperator(CHOOSER_TITLE);
        tabOperator = new JTabbedPaneOperator(colorChooserDialog);
        colorChooser = new JColorChooserOperator(colorChooserDialog);
        okButton = new JButtonOperator(colorChooserDialog, OK_BUTTON_TITLE);
        cancelButton = new JButtonOperator(colorChooserDialog, CANCEL_BUTTON_TITLE);
        resetButton = new JButtonOperator(colorChooserDialog, RESET_BUTTON_TITLE);
    }

    private void basicCheck(JButtonOperator jbo, BezierColor bezierColor) throws Exception {
        Color testColor = new Color(100, 26, 155);
        Color testColor2 = new Color(10, 40, 50);
        checkDefaultColorChooser(jbo, testColor, bezierColor);
        checkCancelButton(jbo, testColor2);
        checkResetButton(jbo, testColor2);
    }

    private void checkDefaultColorChooser(JButtonOperator jbo, Color testColor, BezierColor bezierColor)
            throws Exception {
        BezierAnimationPanel bezierPanel;
        pushButtonAndInitialize(jbo);
        // Check ColorChooser color is being set and used accordingly
        // in the animation panel
        setAndWaitColor(testColor);
        pushButtonAndWaitDialogClosed(okButton);
        bezierPanel = (BezierAnimationPanel) bezierAnimationPanel.getSource();
        colorChooser.waitStateOnQueue(jColorChooser -> (bezierPanel.getBezierColor(bezierColor).equals(testColor)));
    }

    private void checkCancelButton(JButtonOperator jbo, Color testColor) throws Exception {
        pushButtonAndInitialize(jbo);
        setAndWaitColor(testColor);
        pushButtonAndWaitDialogClosed(cancelButton);
    }

    private void checkResetButton(JButtonOperator jbo, Color testColor) throws Exception {
        pushButtonAndInitialize(jbo);
        Color initialColor = colorChooser.getColor();
        setAndWaitColor(testColor);
        resetButton.push();
        waitJColorChooserColor(initialColor);
        pushButtonAndWaitDialogClosed(okButton);
    }

    private void checkAllColorChoosers(JButtonOperator jbo) throws Exception {
        pushButtonAndInitialize(jbo);
        checkHSV();
        checkHSL();
        checkRGB();
        checkCMYK();
        pushButtonAndWaitDialogClosed(okButton);
    }

    private void waitJColorChooserColor(Color expectedColor) {
        colorChooser.waitStateOnQueue(jColorChooser -> colorChooser.getColor().equals(expectedColor));
    }

    private void setAndWaitColor(Color color) {
        colorChooser.setColor(color);
        // Wait for the Color to be set
        waitJColorChooserColor(color);
    }

    private void resetColor() {
        colorChooser.setColor(resetColor);
        // Wait for the Color to be reset
        waitJColorChooserColor(resetColor);
    }

    private void checkHSV() {
        tabOperator.selectPage(HSV);
        initializeSliderAndSpinner(HSV_NUMBER_OF_SLIDERS_AND_SPINNERS);
        resetColor();
        setAndCheckSlider(sliders[HSV_SATURATION_INDEX], 50, new Color(125, 62, 62));
        setAndCheckSlider(sliders[HSV_VALUE_INDEX], 80, new Color(204, 102, 102));
        setAndCheckSlider(sliders[HSV_HUE_INDEX], 50, new Color(204, 187, 102));
        setAndCheckSlider(sliders[HSV_TRANSPARENCY_INDEX], 50, new Color(204, 187, 102, 127));
        setAndCheckSpinner(spinners[HSV_SATURATION_INDEX], 25, new Color(204, 195, 153, 127));
        setAndCheckSpinner(spinners[HSV_VALUE_INDEX], 40, new Color(102, 97, 76, 127));
        setAndCheckSpinner(spinners[HSV_HUE_INDEX], 25, new Color(102, 87, 76, 127));
        setAndCheckSpinner(spinners[HSV_TRANSPARENCY_INDEX], 100, new Color(102, 87, 76, 0));
    }

    private void checkHSL() {
        tabOperator.selectPage(HSL);
        initializeSliderAndSpinner(HSL_NUMBER_OF_SLIDERS_AND_SPINNERS);
        resetColor();
        setAndCheckSlider(sliders[HSL_SATURATION_INDEX], 50, new Color(187, 62, 62));
        setAndCheckSlider(sliders[HSL_LIGHTNESS_INDEX], 80, new Color(229, 178, 178));
        setAndCheckSlider(sliders[HSL_HUE_INDEX], 180, new Color(178, 229, 229));
        setAndCheckSlider(sliders[HSL_TRANSPARENCY_INDEX], 50, new Color(178, 229, 229, 127));
        setAndCheckSpinner(spinners[HSL_SATURATION_INDEX], 25, new Color(191, 216, 216, 127));
        setAndCheckSpinner(spinners[HSL_LIGHTNESS_INDEX], 40, new Color(76, 127, 127, 127));
        setAndCheckSpinner(spinners[HSL_HUE_INDEX], 25, new Color(127, 97, 76, 127));
        setAndCheckSpinner(spinners[HSL_TRANSPARENCY_INDEX], 50, new Color(127, 97, 76, 127));
    }

    private void checkRGB() {
        String sampleColor = "111111";
        tabOperator.selectPage(RGB);
        initializeSliderAndSpinner(RGB_NUMBER_OF_SLIDERS_AND_SPINNERS);
        JTextFieldOperator colorCode = new JTextFieldOperator(colorChooserDialog, RGB_COLORCODE_TEXT_FIELD_INDEX);
        resetColor();
        setAndCheckSlider(sliders[RGB_GREEN_INDEX], 50, new Color(125, 50, 125, 255));
        setAndCheckSlider(sliders[RGB_BLUE_INDEX], 80, new Color(125, 50, 80, 255));
        setAndCheckSlider(sliders[RGB_RED_INDEX], 50, new Color(50, 50, 80, 255));
        setAndCheckSlider(sliders[RGB_ALPHA_INDEX], 125, new Color(50, 50, 80, 125));
        setAndCheckSpinner(spinners[RGB_GREEN_INDEX], 25, new Color(50, 25, 80, 125));
        setAndCheckSpinner(spinners[RGB_BLUE_INDEX], 40, new Color(50, 25, 40, 125));
        setAndCheckSpinner(spinners[RGB_RED_INDEX], 25, new Color(25, 25, 40, 125));
        setAndCheckSpinner(spinners[RGB_ALPHA_INDEX], 255, new Color(25, 25, 40, 255));

        colorCode.setText(sampleColor);
        // Wait for the sampleColor to be set in the color code text field.
        colorCode.waitText(sampleColor);
        colorCode.getFocus();
        colorCode.pressKey(KeyEvent.VK_TAB);
        // Wait for the color to be set
        waitJColorChooserColor(new Color(17, 17, 17, 255));
    }

    private void checkCMYK() {
        tabOperator.selectPage(CMYK);
        initializeSliderAndSpinner(CMYK_NUMBER_OF_SLIDERS_AND_SPINNERS);
        resetColor();
        setAndCheckSlider(sliders[CMYK_MAGENTA_INDEX], 50, new Color(125, 100, 125, 255));
        setAndCheckSlider(sliders[CMYK_YELLOW_INDEX], 80, new Color(125, 100, 85, 255));
        setAndCheckSlider(sliders[CMYK_CYAN_INDEX], 50, new Color(100, 100, 85, 255));
        setAndCheckSlider(sliders[CMYK_BLACK_INDEX], 50, new Color(164, 164, 140, 255));
        setAndCheckSlider(sliders[CMYK_ALPHA_INDEX], 125, new Color(164, 164, 140, 125));
        setAndCheckSpinner(spinners[CMYK_MAGENTA_INDEX], 25, new Color(164, 184, 140, 125));
        setAndCheckSpinner(spinners[CMYK_YELLOW_INDEX], 40, new Color(164, 184, 172, 125));
        setAndCheckSpinner(spinners[CMYK_CYAN_INDEX], 25, new Color(184, 184, 172, 125));
        setAndCheckSpinner(spinners[CMYK_BLACK_INDEX], 100, new Color(139, 139, 130, 125));
        setAndCheckSpinner(spinners[CMYK_ALPHA_INDEX], 255, new Color(139, 139, 130, 255));
    }

    private void setAndCheckSlider(JSliderOperator slider, int sliderValue, Color expectedColor) {
        slider.setValue(sliderValue);
        // Wait for slider to attain the specified value
        slider.waitStateOnQueue(jSlider -> slider.getValue() == sliderValue);
        colorChooser.waitStateOnQueue(jColorChooser -> (colorChooser.getColor().equals(expectedColor)));
    }

    private void setAndCheckSpinner(JSpinnerOperator spinner, int spinnerValue, Color expectedColor) {
        spinner.setValue(spinnerValue);
        // Wait for spinner to attain the specified value
        spinner.waitStateOnQueue(jSpinner -> (int) spinner.getValue() == spinnerValue);
        colorChooser.waitStateOnQueue(jColorChooser -> (colorChooser.getColor().equals(expectedColor)));
    }

    private void initializeSliderAndSpinner(int numberOfSlidersAndSpinners) {
        for (int i = 0; i < numberOfSlidersAndSpinners; i++) {
            sliders[i] = new JSliderOperator(colorChooserDialog, i);
            spinners[i] = new JSpinnerOperator(colorChooserDialog, i);
        }
    }

    private void pushButtonAndWaitDialogClosed(JButtonOperator button) {
        button.push();
        // Wait for the color chooser dialog to close.
        colorChooserDialog.waitClosed();
    }
}

