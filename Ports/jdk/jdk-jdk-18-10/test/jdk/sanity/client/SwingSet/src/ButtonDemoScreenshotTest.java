/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.swingset3.demos.button.ButtonDemo;
import org.jtregext.GuiTestListener;
import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

import javax.swing.UIManager;
import static com.sun.swingset3.demos.button.ButtonDemo.DEMO_TITLE;
import static org.jemmy2ext.JemmyExt.*;

/*
 * @test
 * @key headful screenshots
 * @summary Verifies buttons on SwingSet3 ButtonDemo page by clicking each
 *          button, taking its screenshots and checking that pressed button
 *          image is different from initial button image.
 *
 * @library /sanity/client/lib/jemmy/src
 * @library /sanity/client/lib/Extensions/src
 * @library /sanity/client/lib/SwingSet3/src
 * @modules java.desktop
 *          java.logging
 * @build org.jemmy2ext.JemmyExt
 * @build com.sun.swingset3.demos.button.ButtonDemo
 * @run testng/othervm/timeout=600 -Dswing.disablevistaanimation=true ButtonDemoScreenshotTest
 */
@Listeners(GuiTestListener.class)
public class ButtonDemoScreenshotTest {

    private static final int[] BUTTONS = {0, 1, 2, 3, 4, 5}; // "open browser" buttons (6, 7) open a browser, so ignore

    @Test(dataProvider = "availableLookAndFeels", dataProviderClass = TestHelpers.class)
    public void test(String lookAndFeel) throws Exception {
        UIManager.setLookAndFeel(lookAndFeel);

        //capture some of the background
        Dimension screeSize = Toolkit.getDefaultToolkit().getScreenSize();
        Point screenCenter = new Point(screeSize.width / 2, screeSize.height / 2);
        Rectangle center = new Rectangle(
                screenCenter.x - 50, screenCenter.y - 50,
                100, 100);
        BufferedImage background = getRobot().createScreenCapture(center);

        new ClassReference(ButtonDemo.class.getCanonicalName()).startApplication();

        JFrameOperator mainFrame = new JFrameOperator(DEMO_TITLE);
        mainFrame.waitComponentShowing(true);

        //make sure the frame is already painted
        waitChangedImage(() -> getRobot().createScreenCapture(center),
                background, mainFrame.getTimeouts(), "background");
        //make sure the frame is painted completely
        waitStillImage(mainFrame, "frame");

        // Check all the buttons
        for (int i : BUTTONS) {
            checkButton(mainFrame, i);
        }
    }

    private void checkButton(JFrameOperator jfo, int i) throws InterruptedException {
        JButtonOperator button = new JButtonOperator(jfo, i);

        //additional instrumentation for JDK-8198920. To be removed after the bug is fixed
        java.util.concurrent.atomic.AtomicBoolean actionListenerCalled = new java.util.concurrent.atomic.AtomicBoolean(false);
        button.addActionListener(e -> actionListenerCalled.set(true));
        //end of instrumentation for JDK-8198920

        button.moveMouse(button.getCenterX(), button.getCenterY());

        BufferedImage notPressed, pressed = null;
        notPressed = waitStillImage(button, "not-pressed-" + i);

        button.pressMouse();
        //additional instrumentation for JDK-8198920. To be removed after the bug is fixed
        button.getOutput().printTrace("JDK-8198920: Button pressed at " + System.currentTimeMillis());
        //end of instrumentation for JDK-8198920
        try {
            waitPressed(button);
            //additional instrumentation for JDK-8198920. To be removed after the bug is fixed
            button.getOutput().printTrace("JDK-8198920: Button press confirmed by " + System.currentTimeMillis());
            //end of instrumentation for JDK-8198920
            waitChangedImage(() -> capture(button), notPressed,
                    button.getTimeouts(), "after-press-" + i);
            pressed = waitStillImage(button, "pressed-" + i);
        } finally {
            button.releaseMouse();
            if(pressed != null) {
                waitChangedImage(() -> capture(button), pressed,
                        button.getTimeouts(), "released-" + i);
            }
            //additional instrumentation for JDK-8198920. To be removed after the bug is fixed
            button.getOutput().printTrace("JDK-8198920: Button released at " + System.currentTimeMillis());
            try {
                button.waitState(comp -> actionListenerCalled.get());
                button.getOutput().printTrace("JDK-8198920: Action listener was called by " + System.currentTimeMillis());
            } catch (org.netbeans.jemmy.TimeoutExpiredException e) {
                button.getOutput().printTrace("JDK-8198920: Action listener was not called by " + System.currentTimeMillis());
            }
            //end of instrumentation for JDK-8198920
        }
    }
}
