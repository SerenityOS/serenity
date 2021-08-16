/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @key headful
 * @bug 7159566
 * @summary The choice positioned in the top of applet when clicking the choice.
 * @author Petr Pchelko
 * @library ../../regtesthelpers
 * @build Util
 * @run main ChoiceLocationTest
 */

import java.awt.*;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.util.stream.Stream;

import test.java.awt.regtesthelpers.Util;

public class ChoiceLocationTest {

    private static final int FRAME_LOCATION = 100;
    private static final int FRAME_SIZE = 400;
    private static final int CLICK_STEP = 5;

    private static String[] choiceItems = new String[] {
            "test item 1",
            "test item 2",
            "test item 3"
    };

    private static volatile Frame frame;
    private static volatile Choice choice;

    public static void main(String[] args) throws Exception {
        try {
            SwingUtilities.invokeAndWait(ChoiceLocationTest::initAndShowUI);
            Robot r = new Robot();
            r.waitForIdle();
            r.delay(100);

            Util.clickOnComp(choice, r);

            choice.addItemListener(event -> {throw new RuntimeException("Failed: the choice popup is in the wrong place");});

            // Test: click in several places on the top of the frame an ensure there' no choice there
            Point locOnScreen = frame.getLocationOnScreen();
            int x = locOnScreen.x + FRAME_SIZE / 2;
            for (int y = locOnScreen.y + frame.getInsets().top + 10; y < locOnScreen.y + FRAME_SIZE / 3 ; y += CLICK_STEP) {
                r.mouseMove(x, y);
                r.waitForIdle();
                r.mousePress(InputEvent.BUTTON1_MASK);
                r.mouseRelease(InputEvent.BUTTON1_MASK);
                r.waitForIdle();
                r.delay(100);
            }
        } finally {
            if (frame != null) {
                frame.dispose();
            }
        }

    }

    private static void initAndShowUI() {
        frame = new Frame("Test frame");
        choice = new Choice();
        Stream.of(choiceItems).forEach(choice::add);
        frame.add(choice);
        frame.setBounds(FRAME_LOCATION, FRAME_LOCATION, FRAME_SIZE, FRAME_SIZE);
        frame.setVisible(true);
    }


}
