/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @bug       7154072 7161320
  @summary   Tests that key events with modifiers are not swallowed.
  @author    anton.tarasov: area=awt.focus
  @library   ../../../regtesthelpers
  @library /test/lib
  @modules java.desktop/sun.awt
  @build jdk.test.lib.Platform
  @build     Util
  @run       main SwallowKeyEvents
*/

import jdk.test.lib.Platform;
import java.awt.AWTException;
import java.awt.Frame;
import java.awt.Robot;
import java.awt.TextField;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import test.java.awt.regtesthelpers.Util;

public class SwallowKeyEvents {
    static final int PRESS_COUNT = 10;

    static int keyPressedCount = 0;

    static Frame f = new Frame("Frame");
    static TextField t = new TextField("text");
    static Robot r;

    public static void main(String[] args) {
        if (Platform.isWindows()) {
            System.out.println("Skipped. Test not for MS Windows.");
            return;
        }

        f.add(t);
        f.pack();
        f.setVisible(true);

        t.requestFocus();

        try {
            r = new Robot();
        } catch (AWTException ex) {
            throw new RuntimeException(ex);
        }

        Util.waitForIdle(r);

        t.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent ke) {
                System.out.println(ke);
                if (ke.getKeyCode() == KeyEvent.VK_M) {
                    keyPressedCount++;
                }
            }
        });

        test();

        System.out.println("key_pressed count: " + keyPressedCount);

        if (keyPressedCount != PRESS_COUNT) {
            throw new RuntimeException("Test failed!");
        } else {
            System.out.println("Test passed.");
        }
    }

    public static void test() {
        r.keyPress(KeyEvent.VK_SHIFT);
        r.keyPress(KeyEvent.VK_META);

        for (int i=0; i<PRESS_COUNT; i++) {
            r.delay(100);
            r.keyPress(KeyEvent.VK_M);
            r.delay(100);
            r.keyRelease(KeyEvent.VK_M);
        }

        r.keyRelease(KeyEvent.VK_META);
        r.keyRelease(KeyEvent.VK_SHIFT);
    }
}
