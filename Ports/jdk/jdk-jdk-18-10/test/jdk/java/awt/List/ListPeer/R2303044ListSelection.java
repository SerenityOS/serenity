/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.HeadlessException;
import java.awt.List;
import java.awt.Robot;

/**
 * @test
 * @key headful
 * @summary rdar://problem/2303044 List selection not set when peer is created
 * @summary com.apple.junit.java.awt.List;
 * @run main R2303044ListSelection
 */
public final class R2303044ListSelection {

    public static final String ITEM_NAME = "myItem";

    public static void main(final String[] args) throws HeadlessException {
        final Frame frame = new Frame("Test Frame");
        final List list = new List();
        frame.setSize(300, 200);
        list.add(ITEM_NAME);
        list.select(0);
        frame.add(list);
        frame.validate();
        frame.setVisible(true);
        sleep();
        if (!ITEM_NAME.equals(list.getSelectedItem())) {
            throw new RuntimeException("List item not selected item.");
        }
        list.removeAll();
        frame.dispose();
    }

    private static void sleep() {
        try {
            Robot robot = new Robot();
            robot.waitForIdle();
            Thread.sleep(1000);
        } catch (final Exception ignored) {
            ignored.printStackTrace();
        }
    }
}
