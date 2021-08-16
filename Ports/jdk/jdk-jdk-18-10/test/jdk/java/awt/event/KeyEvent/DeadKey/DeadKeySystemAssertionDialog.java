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

import java.awt.Frame;
import java.awt.Robot;
import java.awt.TextField;
import java.awt.event.KeyEvent;
/*
 * @test
 * @key headful
 * @bug 8013849
 * @summary Awt assert on Hashtable.cpp:124
 * @author alexandr.scherbatiy area=awt.event
 * @run main/timeout=5 DeadKeySystemAssertionDialog
 */

public class DeadKeySystemAssertionDialog {

    public static void main(String[] args) throws Exception {

        Frame frame = new Frame();
        frame.setSize(300, 200);

        TextField textField = new TextField();
        frame.add(textField);

        Robot robot = new Robot();
        robot.setAutoDelay(50);

        frame.setVisible(true);
        robot.waitForIdle();

        textField.requestFocus();
        robot.waitForIdle();

        // Check that the system assertion dialog does not block Java
        robot.keyPress(KeyEvent.VK_A);
        robot.keyRelease(KeyEvent.VK_A);
        robot.waitForIdle();

        frame.setVisible(false);
        frame.dispose();
    }
}
