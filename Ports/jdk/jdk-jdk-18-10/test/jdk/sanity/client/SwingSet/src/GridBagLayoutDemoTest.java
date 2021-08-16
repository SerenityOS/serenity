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
import org.jtregext.GuiTestListener;
import com.sun.swingset3.demos.gridbaglayout.GridBagLayoutDemo;
import static com.sun.swingset3.demos.gridbaglayout.GridBagLayoutDemo.*;
import static com.sun.swingset3.demos.gridbaglayout.Calculator.*;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import javax.swing.JButton;
import javax.swing.UIManager;
import org.testng.annotations.Test;
import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JTextFieldOperator;
import org.testng.annotations.Listeners;
import static org.jemmy2ext.JemmyExt.EXACT_STRING_COMPARATOR;
import static org.jemmy2ext.JemmyExt.getUIValue;
/*
 * @test
 * @key headful
 * @summary Verifies SwingSet3 GridBagLayoutDemo by checking the relative
 *  location of all the components before and after resizing the frame,
 *  interacting with all the controls and checking this interaction on the
 *  text field display.
 *
 * @library /sanity/client/lib/jemmy/src
 * @library /sanity/client/lib/Extensions/src
 * @library /sanity/client/lib/SwingSet3/src
 * @modules java.desktop
 *          java.logging
 * @build org.jemmy2ext.JemmyExt
 * @build com.sun.swingset3.demos.gridbaglayout.GridBagLayoutDemo
 * @run testng/timeout=600 GridBagLayoutDemoTest
 */

@Listeners(GuiTestListener.class)
public class GridBagLayoutDemoTest {

    private JTextFieldOperator tfScreen;
    private JButtonOperator buttonZero;
    private JButtonOperator buttonOne;
    private JButtonOperator buttonTwo;
    private JButtonOperator buttonThree;
    private JButtonOperator buttonFour;
    private JButtonOperator buttonFive;
    private JButtonOperator buttonSix;
    private JButtonOperator buttonSeven;
    private JButtonOperator buttonEight;
    private JButtonOperator buttonNine;
    private JButtonOperator buttonPlus;
    private JButtonOperator buttonMinus;
    private JButtonOperator buttonMultiply;
    private JButtonOperator buttonDivide;
    private JButtonOperator buttonComma;
    private JButtonOperator buttonSqrt;
    private JButtonOperator buttonReciprocal;
    private JButtonOperator buttonToggleSign;
    private JButtonOperator buttonEquals;
    private JButtonOperator backspaceButton;
    private JButtonOperator resetButton;
    private JFrameOperator mainFrame;

    @Test(dataProvider = "availableLookAndFeels", dataProviderClass = TestHelpers.class)
    public void test(String lookAndFeel) throws Exception {
        initializeUIComponents(lookAndFeel);
        // Check that the relative location of buttons is as expected.
        checkRelativeLocations();
        // Interact with buttons and check the result on the display.
        checkInteractionOnDisplay();
        // Change the location and check that the relative location of buttons is same as before.
        checkChangeLocation();
        // Change the size and check that the relative location of buttons is same as before.
        checkChangeSize();
    }

    private double x(Component component) {
        return component.getLocation().getX();
    }

    private double y(Component component) {
        return component.getLocation().getY();
    }

    private void checkRight(JButtonOperator currentButton, JButtonOperator rightButton) {
        // Check that x coordinate of right button is greater than that of right
        // end of current button
        currentButton.waitStateOnQueue(button -> x(button) + button.getWidth() < x(rightButton.getSource()));
        // Check that the y coordinate of the button is same as the button to
        // the right
        currentButton.waitStateOnQueue(button -> y(button) == y(rightButton.getSource()));
        // Check that the height of the button is same as the button to the
        // right
        currentButton.waitStateOnQueue(button -> button.getHeight() == rightButton.getHeight());
    }

    private void checkBelow(JButtonOperator currentButton, JButtonOperator buttonBelow) {
        // Check that y coordinate of button below is greater than that of
        // bottom end of current button
        currentButton.waitStateOnQueue(button -> y(button) + button.getHeight() < y(buttonBelow.getSource()));
        // Check that the x coordinate of the button is same as the button below
        currentButton.waitStateOnQueue(button -> x(button) == x(buttonBelow.getSource()));
        // Check that the width of the button is same as the button below
        currentButton.waitStateOnQueue(button -> button.getWidth() == buttonBelow.getWidth());
    }

    private void checkRelativeLocations() {
        // Check relative location of button 7
        checkRight(buttonSeven, buttonEight);
        checkBelow(buttonSeven, buttonFour);

        // Check relative location of button 8
        checkRight(buttonEight, buttonNine);
        checkBelow(buttonEight, buttonFive);

        // Check relative location of button 9
        checkRight(buttonNine, buttonDivide);
        checkBelow(buttonNine, buttonSix);

        // Check relative location of Division button
        checkRight(buttonDivide, buttonReciprocal);
        checkBelow(buttonDivide, buttonMultiply);

        // Check relative location of Reciprocal and Sqrt buttons
        checkBelow(buttonReciprocal, buttonSqrt);

        // Check relative location of button 4
        checkRight(buttonFour, buttonFive);
        checkBelow(buttonFour, buttonOne);

        // Check relative location of button 5
        checkRight(buttonFive, buttonSix);
        checkBelow(buttonFive, buttonTwo);

        // Check relative location of button 6
        checkRight(buttonSix, buttonMultiply);
        checkBelow(buttonSix, buttonThree);

        // Check relative location of Multiply button
        checkRight(buttonMultiply, buttonSqrt);
        checkBelow(buttonMultiply, buttonMinus);

        // Check relative location of button 1
        checkRight(buttonOne, buttonTwo);
        checkBelow(buttonOne, buttonZero);

        // Check relative location of button 2
        checkRight(buttonTwo, buttonThree);
        checkBelow(buttonTwo, buttonToggleSign);

        // Check relative location of button 3
        checkRight(buttonThree, buttonMinus);
        checkBelow(buttonThree, buttonComma);

        // Check relative location of Minus button
        checkBelow(buttonMinus, buttonPlus);

        // Check relative location of button 0
        checkRight(buttonZero, buttonToggleSign);

        // Check relative location of Sign Toggle Button
        checkRight(buttonToggleSign, buttonComma);

        // Check relative location of Comma button
        checkRight(buttonComma, buttonPlus);

        // Check relative location of plus and Equals buttons
        checkRight(buttonPlus, buttonEquals);

        // Check relative location of JPanel containing Backspace and Reset
        // buttons
        Point parentLocation = getUIValue(backspaceButton, (JButton button) -> button.getParent().getLocation());
        // Check that the y coordinate of bottom of the screen is
        // less than that of parent of backspace button
        tfScreen.waitStateOnQueue(screen -> y(screen) + screen.getHeight() < parentLocation.getY());
        // Check that the y coordinate of the button 7 is greater
        // than that of parent of backspace
        buttonSeven.waitStateOnQueue(button -> parentLocation.getY() < y(button));
        // Check that the y coordinate of reciprocal button is greater
        // than that of parent of backspace
        buttonReciprocal.waitStateOnQueue(button -> parentLocation.getY() < y(button));
        // Check that x coordinate of screen is same as that of parent of
        // backspace
        tfScreen.waitStateOnQueue(screen -> x(screen) == parentLocation.getX());
        // Check that x coordinate of button 7 is same as that of parent of
        // backspace
        buttonSeven.waitStateOnQueue(button -> x(button) == parentLocation.getX());

        // Check relative location of Backspace button
        // Check that the x coordinate of right of backspace button
        // is less than that of reset button
        backspaceButton.waitStateOnQueue(button -> x(button) + button.getWidth() < x(resetButton.getSource()));
        // Check that the height of backspace button is same as that of reset
        // button
        backspaceButton.waitStateOnQueue(button -> button.getHeight() == resetButton.getHeight());
        // Check that the y coordinate bottom of backspace button is less that
        // that of button 7
        backspaceButton.waitStateOnQueue(
                button -> parentLocation.getY() + button.getParent().getHeight() < y(buttonSeven.getSource()));

        // Check that the x coordinate of reset button is greater than that
        // of right of backspace button
        resetButton.waitStateOnQueue(button -> x(backspaceButton.getSource()) + backspaceButton.getWidth() < x(button));

        // Check that the height of reset button is same as that of backspace
        // button
        resetButton.waitStateOnQueue(button -> backspaceButton.getHeight() == button.getHeight());

        // Check that the y coordinate of bottom of reset button is less
        // than that of divide button.
        resetButton.waitStateOnQueue(
                button -> parentLocation.getY() + button.getParent().getHeight() < y(buttonDivide.getSource()));

        // Check that the y coordinate of top of screen is lower
        // than that of parent of backspace button
        tfScreen.waitStateOnQueue(screen -> y(screen) + screen.getHeight() < parentLocation.getY());
    }

    private void checkInteractionOnDisplay() {
        // Check buttons: 1,2,+,=,C
        buttonOne.push();
        tfScreen.waitText("1");
        buttonPlus.push();
        tfScreen.waitText("1");
        buttonTwo.push();
        tfScreen.waitText("2");
        buttonEquals.push();
        tfScreen.waitText("3");
        resetButton.push();
        tfScreen.waitText("0");

        // Check buttons: 3,4,-
        buttonFour.push();
        tfScreen.waitText("4");
        buttonMinus.push();
        tfScreen.waitText("4");
        buttonThree.push();
        tfScreen.waitText("3");
        buttonEquals.push();
        tfScreen.waitText("1");
        reset();

        // Check buttons: 5,6,*
        buttonFive.push();
        tfScreen.waitText("5");
        buttonMultiply.push();
        tfScreen.waitText("5");
        buttonSix.push();
        tfScreen.waitText("6");
        buttonEquals.push();
        tfScreen.waitText("30");
        reset();

        // Check buttons: 8,9,/
        buttonNine.push();
        buttonNine.push();
        tfScreen.waitText("99");
        buttonDivide.push();
        tfScreen.waitText("99");
        buttonEight.push();
        tfScreen.waitText("8");
        buttonEquals.push();
        tfScreen.waitText("12.375");
        reset();

        // Check buttons: 7,0,[+/-],Backspace
        buttonSeven.push();
        tfScreen.waitText("7");
        buttonZero.push();
        tfScreen.waitText("70");
        buttonToggleSign.push();
        tfScreen.waitText("-70");
        buttonToggleSign.push();
        tfScreen.waitText("70");
        backspaceButton.push();
        tfScreen.waitText("7");
        reset();

        // Check Sqrt Button
        buttonFour.push();
        buttonNine.push();
        tfScreen.waitText("49");
        buttonSqrt.push();
        tfScreen.waitText("7");
        reset();

        // Check Reciprocal Button
        buttonFour.push();
        tfScreen.waitText("4");
        buttonReciprocal.push();
        tfScreen.waitText("0.25");
        reset();

        // Check Comma button
        buttonFour.push();
        buttonComma.push();
        tfScreen.waitText("4,");
    }

    private void reset() {
        resetButton.push();
        tfScreen.waitText("0");
    }

    private void initializeUIComponents(String lookAndFeel) throws Exception {
        UIManager.setLookAndFeel(lookAndFeel);
        new ClassReference(GridBagLayoutDemo.class.getCanonicalName()).startApplication();
        mainFrame = new JFrameOperator(GRID_BAG_LAYOUT_DEMO_TITLE);
        mainFrame.setComparator(EXACT_STRING_COMPARATOR);
        buttonZero = new JButtonOperator(mainFrame, ZERO_BUTTON_TITLE);
        buttonOne = new JButtonOperator(mainFrame, ONE_BUTTON_TITLE);
        buttonTwo = new JButtonOperator(mainFrame, TWO_BUTTON_TITLE);
        buttonThree = new JButtonOperator(mainFrame, THREE_BUTTON_TITLE);
        buttonFour = new JButtonOperator(mainFrame, FOUR_BUTTON_TITLE);
        buttonFive = new JButtonOperator(mainFrame, FIVE_BUTTON_TITLE);
        buttonSix = new JButtonOperator(mainFrame, SIX_BUTTON_TITLE);
        buttonSeven = new JButtonOperator(mainFrame, SEVEN_BUTTON_TITLE);
        buttonEight = new JButtonOperator(mainFrame, EIGHT_BUTTON_TITLE);
        buttonNine = new JButtonOperator(mainFrame, NINE_BUTTON_TITLE);
        buttonPlus = new JButtonOperator(mainFrame, PLUS_BUTTON_TITLE);
        buttonMinus = new JButtonOperator(mainFrame, MINUS_BUTTON_TITLE);
        buttonMultiply = new JButtonOperator(mainFrame, MULTIPLY_BUTTON_TITLE);
        buttonDivide = new JButtonOperator(mainFrame, DIVIDE_BUTTON_TITLE);
        buttonComma = new JButtonOperator(mainFrame, ",");
        buttonSqrt = new JButtonOperator(mainFrame, SQRT_BUTTON_TITLE);
        buttonReciprocal = new JButtonOperator(mainFrame, INVERSE_BUTTON_TITLE);
        buttonToggleSign = new JButtonOperator(mainFrame, SWAPSIGN_BUTTON_TITLE);
        buttonEquals = new JButtonOperator(mainFrame, EQUALS_BUTTON_TITLE);
        resetButton = new JButtonOperator(mainFrame, C_BUTTON_TITLE);
        backspaceButton = new JButtonOperator(mainFrame, BACKSPACE_BUTTON_TITLE);
        tfScreen = new JTextFieldOperator(mainFrame, 0);
    }

    private void checkChangeLocation() {
        Point startingPoint = new Point(100, 100);
        mainFrame.setLocation(startingPoint);
        mainFrame.waitComponentLocation(startingPoint);
        checkRelativeLocations();
    }

    private void checkChangeSize() {
        Dimension newSize = new Dimension((int) mainFrame.getToolkit().getScreenSize().getWidth() / 2,
                (int) mainFrame.getToolkit().getScreenSize().getHeight() / 2);
        mainFrame.setSize(newSize);
        mainFrame.waitComponentSize(newSize);
        checkRelativeLocations();
    }
}
