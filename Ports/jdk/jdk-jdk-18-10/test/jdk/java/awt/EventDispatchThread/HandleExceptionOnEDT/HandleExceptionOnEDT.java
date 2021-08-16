/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6304473 6727884
  @summary Tests that an exception on EDT is handled with ThreadGroup.uncaughtException()
  @author artem.ananiev: area=awt.eventdispatching
  @library ../../regtesthelpers
  @build Util
  @run main HandleExceptionOnEDT
*/

import java.awt.*;
import java.awt.event.*;

import test.java.awt.regtesthelpers.Util;

public class HandleExceptionOnEDT
{
    private final static String EXCEPTION_MESSAGE = "A1234567890";

    private static volatile boolean exceptionHandled = false;
    private static volatile boolean mousePressed = false;

    public static void main(String[] args)
    {
        final Thread.UncaughtExceptionHandler eh = new Thread.UncaughtExceptionHandler()
        {
            @Override
            public void uncaughtException(Thread t, Throwable e)
            {
                if (e.getMessage().equals(EXCEPTION_MESSAGE))
                {
                    exceptionHandled = true;
                }
            }
        };

        Frame f = new Frame("F");
        f.setBounds(100, 100, 400, 300);
        // set exception handler for EDT
        f.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowOpened(WindowEvent we)
            {
                Thread edt = Thread.currentThread();
                edt.setUncaughtExceptionHandler(eh);
            }
        });
        f.setVisible(true);

        Robot r = Util.createRobot();
        Util.waitForIdle(r);

        // check exception without modal dialog
        MouseListener exceptionListener = new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent me)
            {
                throw new RuntimeException(EXCEPTION_MESSAGE);
            }
        };
        f.addMouseListener(exceptionListener);

        exceptionHandled = false;
        Point fp = f.getLocationOnScreen();
        r.mouseMove(fp.x + f.getWidth() / 2, fp.y + f.getHeight() / 2);
        Util.waitForIdle(r);
        r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        Util.waitForIdle(r);
        r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        f.removeMouseListener(exceptionListener);

        if (!exceptionHandled)
        {
            throw new RuntimeException("Test FAILED: exception is not handled for frame");
        }

        // check exception with modal dialog
        final Dialog d = new Dialog(f, "D", true);
        d.setBounds(fp.x + 100, fp.y + 100, 400, 300);
        d.addMouseListener(exceptionListener);
        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                d.setVisible(true);
            }
        });
        Util.waitForIdle(r);

        exceptionHandled = false;
        Point dp = d.getLocationOnScreen();
        r.mouseMove(dp.x + d.getWidth() / 2, dp.y + d.getHeight() / 2);
        Util.waitForIdle(r);
        r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        Util.waitForIdle(r);
        r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        d.removeMouseListener(exceptionListener);

        if (!exceptionHandled)
        {
            throw new RuntimeException("Test FAILED: exception is not handled for modal dialog");
        }

        // check the dialog is still modal
        MouseListener pressedListener = new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent me)
            {
                mousePressed = true;
            }
        };
        f.addMouseListener(pressedListener);

        mousePressed = false;
        r.mouseMove(fp.x + 50, fp.y + 50);
        Util.waitForIdle(r);
        r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        Util.waitForIdle(r);
        r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        Util.waitForIdle(r);
        f.removeMouseListener(pressedListener);

        if (mousePressed)
        {
            throw new RuntimeException("Test FAILED: modal dialog is not modal or visible after exception");
        }

        // test is passed
        d.dispose();
        f.dispose();
    }
}
