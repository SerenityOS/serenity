/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6304979
  @key headful
  @summary REG: File Dialog throws ArrayIndexOutOfBounds Exception on XToolkit with b45
  @author Dmitry Cherepanov: area=awt.filedialog
  @run main/othervm -Dsun.awt.disableGtkFileDialogs=true ISCthrownByFileListTest
*/

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;

/*
  Since the "sun.awt.exception.handler" property will be removed in a future release
  this test will be rewritten using new future API. (<<< Done).
  It's important that the bug 6304979 is reproducible if the bug 6299853 is reproducible.
*/

public class ISCthrownByFileListTest
{
    private static Frame frame = null;
    private static FileDialog fd = null;

    // The handler load the class and instantiate this class
    // so the 'passed' variable is static
    static boolean passed = true;

    public static final void main(String args[]) {
        // It's not true that the native file dialog will be focused on Motif & Windows
        boolean isXToolkit = Toolkit.getDefaultToolkit().getClass().getName().equals("sun.awt.X11.XToolkit");
        if (!isXToolkit){
            return;
        }

        frame = new Frame("frame");
        frame.setLayout (new FlowLayout ());
        frame.setBounds(100, 100, 100, 100);
        frame.setVisible(true);

        fd = new FileDialog(frame, "file dialog", FileDialog.LOAD);

        // In order to handle all uncaught exceptions in the EDT
        final Thread.UncaughtExceptionHandler eh = new Thread.UncaughtExceptionHandler()
        {
            @Override
            public void uncaughtException(Thread t, Throwable e)
            {
                e.printStackTrace();
                ISCthrownByFileListTest.passed = false;
            }
        };

        test();
    }// start()

    private static void test (){
        Robot r;

        try {
            r = new Robot();
        } catch(AWTException e) {
            throw new RuntimeException(e.getMessage());
        }

        r.delay(500);
        new Thread(new Runnable() {
                public void run() {
                    // The bug 6299853 is reproducible only if the file list is not empty
                    // since else the focus will be set to the directory list.
                    // But the focus index of the directory list equals 0.
                    // So goto the source directory (the file list is non empty)
                    fd.setDirectory(System.getProperty("test.src", "."));
                    fd.setVisible(true);
                }
            }).start();
        r.delay(2000);
        r.waitForIdle();

        Component focusedWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
        if (focusedWindow != fd) {
            throw new RuntimeException("Test failed - the file dialog isn't focused window, owner: " + focusedWindow);
        }
        r.waitForIdle();

        r.keyPress(KeyEvent.VK_SPACE);
        r.delay(50);
        r.keyRelease(KeyEvent.VK_SPACE);
        r.delay(1000);
        fd.setVisible(false);
        r.delay(1000);
        r.waitForIdle();

        if (!ISCthrownByFileListTest.passed){
            throw new RuntimeException("Test failed.");
        }

    }// test()
}// class ISCthrownByFileListTest
