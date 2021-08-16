/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.swingset3.demos.slider.SliderDemo;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.util.function.Predicate;

import javax.swing.UIManager;

import static org.testng.AssertJUnit.*;

import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.scrolling.KeyboardJSliderScrollDriver;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JSliderOperator;
import org.netbeans.jemmy.accessibility.AccessibleNameChooser;
import org.netbeans.jemmy.util.LookAndFeel;

import static com.sun.swingset3.demos.slider.SliderDemo.*;
import org.testng.annotations.Listeners;

/*
 * @test
 * @key headful
 * @summary Verifies SwingSet3 SliderDemo by moving the sliders through
 *  different means, checking the slider value corresponding to it,
 *  checking maximum and minimum values, checking Snap to tick is working
 *  and checking the presence of ticks and labels.
 *
 * @library /sanity/client/lib/jemmy/src
 * @library /sanity/client/lib/Extensions/src
 * @library /sanity/client/lib/SwingSet3/src
 * @modules java.desktop
 *          java.logging
 * @build org.jemmy2ext.JemmyExt
 * @build com.sun.swingset3.demos.slider.SliderDemo
 * @run testng/timeout=600 SliderDemoTest
 */
@Listeners(GuiTestListener.class)
public class SliderDemoTest {

    private static final int PLAIN_SLIDER_MINIMUM = -10;
    private static final int PLAIN_SLIDER_MAXIMUM = 100;
    private static final int HORIZONTAL_MINOR_TICKS_SLIDER_MINIMUM = 0;
    private static final int HORIZONTAL_MINOR_TICKS_SLIDER_MAXIMUM = 11;
    private static final int VERTICAL_MINOR_TICKS_SLIDER_MINIMUM = 0;
    private static final int VERTICAL_MINOR_TICKS_SLIDER_MAXIMUM = 100;

    @BeforeClass
    public void useKeyboardSliderDriver() {
        DriverManager.setScrollDriver(new KeyboardJSliderScrollDriver());
    }

    @Test(dataProvider = "availableLookAndFeels", dataProviderClass = TestHelpers.class)
    public void test(String lookAndFeel) throws Exception {
        UIManager.setLookAndFeel(lookAndFeel);
        new ClassReference(SliderDemo.class.getCanonicalName()).startApplication();
        JFrameOperator frame = new JFrameOperator(DEMO_TITLE);
        plain(frame, HORIZONTAL_PLAIN_SLIDER);
        majorTicks(frame, HORIZONTAL_MAJOR_TICKS_SLIDER);
        minorTicks(frame, HORIZONTAL_MINOR_TICKS_SLIDER);
        disabled(frame, HORIZONTAL_DISABLED_SLIDER);
        plain(frame, VERTICAL_PLAIN_SLIDER);
        majorTicks(frame, VERTICAL_MAJOR_TICKS_SLIDER);
        minorTicks(frame, VERTICAL_MINOR_TICKS_SLIDER);
        disabled(frame, VERTICAL_DISABLED_SLIDER);
    }

    private void plain(JFrameOperator jfo, String accessibleName) {
        JSliderOperator jso = new JSliderOperator(jfo,
                new AccessibleNameChooser(accessibleName));
        if (accessibleName.equals(HORIZONTAL_PLAIN_SLIDER)) {
            jso.waitHasFocus();
            checkKeyboard(jso);
            checkMouse(jso);
        }
        checkMaximum(jso, PLAIN_SLIDER_MAXIMUM);
        checkMinimum(jso, PLAIN_SLIDER_MINIMUM);
        checkMoveForward(jso, 10);
    }

    private void majorTicks(JFrameOperator jfo, String accessibleName) {
        JSliderOperator jso = new JSliderOperator(jfo,
                new AccessibleNameChooser(accessibleName));
        checkMoveForward(jso, 40);
        assertTrue(jso.getPaintTicks());
        assertEquals(100, jso.getMajorTickSpacing());
    }

    private void minorTicks(JFrameOperator jfo, String accessibleName) {
        JSliderOperator jso = new JSliderOperator(jfo,
                new AccessibleNameChooser(accessibleName));
        if (accessibleName.equals(HORIZONTAL_MINOR_TICKS_SLIDER)) {
            checkMaximum(jso, HORIZONTAL_MINOR_TICKS_SLIDER_MAXIMUM);
            checkMinimum(jso, HORIZONTAL_MINOR_TICKS_SLIDER_MINIMUM);
            checkMoveForward(jso, 2);
            checkSnapToTick(jso, 5, 6);
            assertEquals(5, jso.getMajorTickSpacing());
            assertEquals(1, jso.getMinorTickSpacing());
        } else {
            checkMaximum(jso, VERTICAL_MINOR_TICKS_SLIDER_MAXIMUM);
            checkMinimum(jso, VERTICAL_MINOR_TICKS_SLIDER_MINIMUM);
            checkMoveForward(jso, 10);
            assertEquals(20, jso.getMajorTickSpacing());
            assertEquals(5, jso.getMinorTickSpacing());
        }
        assertTrue(jso.getPaintTicks());
        assertTrue(jso.getPaintLabels());
    }

    private void disabled(JFrameOperator jfo, String accessibleName)
            throws InterruptedException {
        JSliderOperator jso = new JSliderOperator(jfo,
                new AccessibleNameChooser(accessibleName));
        int initialvalue;
        initialvalue = jso.getValue();
        jso.clickMouse(jso.getCenterXForClick(), jso.getCenterYForClick(), 10);
        Thread.sleep(500);
        assertFalse(jso.hasFocus());
        assertEquals(initialvalue, jso.getValue());
    }

    private void checkMaximum(JSliderOperator jso, int maxValue) {
        jso.scrollToMaximum();
        waitSliderValue(jso, jSlider -> jSlider.getValue() == maxValue,
                "value == " + maxValue);
    }

    private void checkMinimum(JSliderOperator jso, int minValue) {
        jso.scrollToMinimum();
        waitSliderValue(jso, jSlider -> jSlider.getValue() == minValue,
                "value == " + minValue);
    }

    private void checkKeyboard(JSliderOperator jso) {
        boolean isMotif = LookAndFeel.isMotif();
        checkKeyPress(jso, KeyEvent.VK_HOME,
                jSlider -> jSlider.getValue() == jso.getMinimum(),
                "value == " + jso.getMinimum());

        {
            int expectedValue = jso.getValue() + 1;
            checkKeyPress(jso, KeyEvent.VK_UP,
                    jSlider -> jSlider.getValue() >= expectedValue,
                    "value >= " + expectedValue);
        }
        {
            int expectedValue = jso.getValue() + 1;
            checkKeyPress(jso, KeyEvent.VK_RIGHT,
                    jSlider -> jSlider.getValue() >= expectedValue,
                    "value >= " + expectedValue);
        }
        if (!isMotif) {
            int expectedValue = jso.getValue() + 11;
            checkKeyPress(jso, KeyEvent.VK_PAGE_UP,
                    jSlider -> jSlider.getValue() >= expectedValue,
                    "value >= " + expectedValue);
        }

        checkKeyPress(jso, KeyEvent.VK_END,
                jSlider -> jSlider.getValue() == jso.getMaximum(),
                "value == " + jso.getMaximum());

        {
            int expectedValue = jso.getValue() - 1;
            checkKeyPress(jso, KeyEvent.VK_DOWN,
                    jSlider -> jSlider.getValue() <= expectedValue,
                    "value <= " + expectedValue);
        }
        {
            int expectedValue = jso.getValue() - 1;
            checkKeyPress(jso, KeyEvent.VK_LEFT,
                    jSlider -> jSlider.getValue() <= expectedValue,
                    "value <= " + expectedValue);
        }
        if (!isMotif) {
            int expectedValue = jso.getValue() - 11;
            checkKeyPress(jso, KeyEvent.VK_PAGE_DOWN,
                    jSlider -> jSlider.getValue() <= expectedValue,
                    "value <= " + expectedValue);
        }
    }

    private void checkKeyPress(JSliderOperator jso, int keyCode,
            Predicate<JSliderOperator> predicate,
                               String description) {
        jso.pushKey(keyCode);
        waitSliderValue(jso, predicate, description);
    }

    private void waitSliderValue(JSliderOperator jso,
            Predicate<JSliderOperator> predicate, String description) {
        jso.waitState(new ComponentChooser() {
            public boolean checkComponent(Component comp) {
                return predicate.test(jso);
            }

            public String getDescription() {
                return description;
            }
        });
    }

    private void checkMoveForward(JSliderOperator jso, int value) {
        jso.setValue(jso.getMinimum());
        int finalValue = jso.getValue() + value;
        jso.scrollToValue(finalValue);
        waitSliderValue(jso, jSlider -> jSlider.getValue() == finalValue,
                "value == " + finalValue);
    }

    private void checkSnapToTick(JSliderOperator jso, int expectedLower,
            int expectedHigher) {
        jso.pressMouse(jso.getCenterXForClick(), jso.getCenterYForClick());
        waitSliderValue(jso, jSlider -> jSlider.getValue() == expectedLower
                || jSlider.getValue() == expectedHigher,
                "value is either" + expectedLower + " or " + expectedHigher);
        jso.releaseMouse();
    }

    private void checkMouse(JSliderOperator jso) {
        // Check mouse dragging by pressing on the center of Slider and then
        // dragging the mouse till the end of the track.
        // We set the initial value of the slider as 45,
        // which is the value of the slider at the middle.
        jso.setValue((jso.getMaximum() + jso.getMinimum()) / 2);
        jso.pressMouse(jso.getCenterXForClick(), jso.getCenterYForClick());
        jso.dragMouse(jso.getWidth() + 10, jso.getHeight());
        waitSliderValue(jso, jSlider -> jSlider.getValue() == jSlider.getMaximum(),
                "value == " + jso.getMaximum());
        jso.releaseMouse();

        // Check mouse click by clicking on the center of the track 2 times
        // and waiting till the slider value has changed from its previous
        // value as a result of the clicks.
        jso.clickMouse(jso.getCenterXForClick(), jso.getCenterYForClick(), 2);
        waitSliderValue(jso, jSlider -> jSlider.getValue() != jSlider.getMaximum(),
                "value != " + jso.getMaximum());
    }
}
