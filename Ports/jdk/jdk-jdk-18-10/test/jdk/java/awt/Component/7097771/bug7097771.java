/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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

import test.java.awt.regtesthelpers.Util;

import java.awt.AWTException;
import java.awt.Button;
import java.awt.Frame;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/*
  @test
  @key headful
  @bug 7097771
  @summary setEnabled does not work for components in disabled containers.
  @author sergey.bylokhov@oracle.com: area=awt.component
  @library ../../regtesthelpers
  @build Util
  @run main bug7097771
*/
public final class bug7097771 extends Frame implements ActionListener {

    private static volatile boolean action;

    public static void main(final String[] args) throws AWTException {
        final bug7097771 frame = new bug7097771();
        frame.setSize(300, 300);
        frame.setLocationRelativeTo(null);
        final Button button = new Button();
        button.addActionListener(frame);
        frame.add(button);
        frame.setVisible(true);
        Robot robot = new Robot();
        sleep(robot);
        frame.setEnabled(false);
        button.setEnabled(false);
        button.setEnabled(true);
        sleep(robot);
        Util.clickOnComp(button, robot);
        sleep(robot);
        frame.dispose();
        if (action) {
            throw new RuntimeException("Button is not disabled.");
        }
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        action = true;
    }

    private static void sleep(Robot robot) {
        robot.waitForIdle();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
    }
}
