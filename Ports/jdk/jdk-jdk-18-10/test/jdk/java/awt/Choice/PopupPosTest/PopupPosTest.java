/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @bug 5044150
  @summary Tests that pupup doesn't popdown if no space to display under
  @requires (os.family == "linux")
  @run main PopupPosTest
*/

import java.awt.*;
import java.awt.event.*;

public class PopupPosTest {

    public static void main(final String[] args) {
        Frame frame = new TestFrame();
    }
}

class TestFrame extends Frame implements ItemListener {
    Robot robot;
    Toolkit tk = Toolkit.getDefaultToolkit();
    Choice choice = new Choice();
    boolean indexChanged = false;
    final static int INITIAL_ITEM = 99;
    volatile boolean stateChanged;

    public TestFrame() {
        for (int i = 0; i < 100; i++) {
             choice.addItem("Item Item Item " + i);
        }
        choice.addItemListener(this);

        choice.select(INITIAL_ITEM);
        choice.setFont(new Font("Courier", Font.BOLD + Font.ITALIC, 100));

        add(choice, BorderLayout.CENTER);
        Dimension screen = tk.getScreenSize();
        setSize(screen.width - 10, screen.height - 70);
        setVisible(true);
        toFront();
        try {
            robot = new Robot();
            robot.setAutoDelay(50);
            robot.waitForIdle();
            // fix for 6175418. When we take "choice.getHeight()/2"
            // divider 2 is not sufficiently big to hit into the
            // small box Choice. We should use bigger divider to get
            // smaller value choice.getHeight()/i. 4 is sufficient.
            Point pt = choice.getLocationOnScreen();
            // click on 1/4 of Choice's height
            mouseMoveAndPressOnChoice(pt.x + choice.getWidth()/2,
                              pt.y + choice.getHeight()/4);

            // click on center of Choice's height
            mouseMoveAndPressOnChoice(pt.x + choice.getWidth()/2,
                              pt.y + choice.getHeight()/2);

            // click on 3/4 of Choice's height
            mouseMoveAndPressOnChoice(pt.x + choice.getWidth()/2,
                              pt.y + choice.getHeight()*3/4);
            // testing that ItemEvent doesn't generated on a simple
            // mouse click when the dropdown appears under mouse : 6425067
            stateChanged = false;
            openChoice();
            closeChoice();
        } catch (Throwable e) {
            throw new RuntimeException("The test was not completed.\n\n" + e);
        }

        if (!indexChanged){
            throw new RuntimeException("Test failed. Another item wasn't selected.");
        }

        if(stateChanged){
            throw new RuntimeException("Test failed. ItemEvent was generated on a simple mouse click when the dropdown appears under mouse");
        }
    }// start()

    public void itemStateChanged(ItemEvent ie) {
        System.out.println("choice.stateChanged = "+ ie);
        stateChanged = true;
    }

    public void mouseMoveAndPressOnChoice(int x, int y){
        openChoice();
        robot.mouseMove(x, y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.delay(30);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.waitForIdle();
        //should close choice after each test stage
        closeChoice();
        checkSelectedIndex();
    }

    public void openChoice(){
        Point pt = choice.getLocationOnScreen();
        robot.mouseMove(pt.x + choice.getWidth() - choice.getHeight()/4,
                        pt.y + choice.getHeight()/2);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.delay(30);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.waitForIdle();
    }
    public void closeChoice(){
        robot.keyPress(KeyEvent.VK_ESCAPE);
        robot.keyRelease(KeyEvent.VK_ESCAPE);
        robot.waitForIdle();
    }

    public void checkSelectedIndex(){
        if (choice.getSelectedIndex() != INITIAL_ITEM) {
            System.out.println("choice.getSelectedIndex = "+ choice.getSelectedIndex());
            indexChanged = true;
        }
    }
}// class TestFrame
