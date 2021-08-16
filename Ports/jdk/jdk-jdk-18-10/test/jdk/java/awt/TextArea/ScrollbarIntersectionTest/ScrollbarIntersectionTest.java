/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6429174
  @summary Tests that mouse click at the are of intersection of two
   scrollbars for text area doesn't trigger any scrolling
  @author artem.ananiev@sun.com: area=awt.text
  @library /test/lib
  @build jdk.test.lib.Platform
  @run main ScrollbarIntersectionTest
*/

import java.awt.*;
import java.awt.event.*;

import jdk.test.lib.Platform;

public class ScrollbarIntersectionTest
{
    private static void init()
    {

        Frame f = new Frame("F");
        f.setBounds(100, 100, 480, 360);
        f.setLayout(new BorderLayout());

        TextArea ta = new TextArea(null, 8, 24, TextArea.SCROLLBARS_BOTH);
        // append several lines to show vertical scrollbar
        for (int i = 0; i < 128; i++)
        {
            ta.append("" + i + "\n");
        }
        // and some characters into the last line for horizontal scrollbar
        for (int i = 0; i < 128; i++)
        {
            ta.append("" + i);
        }
        ta.append("\n");
        f.add(ta);

        f.setVisible(true);

        Robot r = null;
        try
        {
            r = new Robot();
            r.setAutoDelay(20);
        }
        catch (Exception z)
        {
            z.printStackTrace(System.err);
            fail(z.getMessage());
            return;
        }
        r.waitForIdle();

        ta.setCaretPosition(0);
        r.waitForIdle();

        Point p = ta.getLocationOnScreen();
        Dimension d = ta.getSize();

        int fh = 8;
        Graphics g = ta.getGraphics();
        try
        {
            FontMetrics fm = g.getFontMetrics();
            fh = fm.getHeight();
        }
        finally
        {
            if (g != null)
            {
                g.dispose();
            }
        };

        r.mouseMove(p.x + d.width - 2, p.y + d.height - 2);
        r.mousePress(InputEvent.BUTTON1_MASK);
        r.mouseRelease(InputEvent.BUTTON1_MASK);
        r.waitForIdle();

        // select 1st line in the text area
        r.mouseMove(p.x + 2, p.y + 2 + fh / 2);
        r.mousePress(InputEvent.BUTTON1_MASK);
        for (int i = 0; i < d.width - 4; i += 4)
        {
            r.mouseMove(p.x + 2 + i, p.y + 2 + fh / 2);
        }
        r.mouseRelease(InputEvent.BUTTON1_MASK);
        r.waitForIdle();

        String sel = ta.getSelectedText();
        System.err.println("Selected text: " + sel);
        if ((sel == null) || !sel.startsWith("0"))
        {
            fail("Test FAILED: TextArea is scrolled");
            return;
        }

        pass();
    }

    private static boolean theTestPassed = false;
    private static boolean testGeneratedInterrupt = false;
    private static String failureMessage = "";

    private static Thread mainThread = null;

    private static int sleepTime = 300000;

    public static void main( String args[] ) throws InterruptedException
    {
        if (Platform.isOSX()) {
            // On OS X, this area is commandeered by the system,
            // and frame would be wildly resized
            System.out.println("Not for OS X");
            return;
        }
        mainThread = Thread.currentThread();
        try
        {
            init();
        }
        catch( TestPassedException e )
        {
            return;
        }

        try
        {
            Thread.sleep( sleepTime );
            throw new RuntimeException( "Timed out after " + sleepTime/1000 + " seconds" );
        }
        catch (InterruptedException e)
        {
            if( ! testGeneratedInterrupt ) throw e;

            testGeneratedInterrupt = false;

            if ( theTestPassed == false )
            {
                throw new RuntimeException( failureMessage );
            }
        }
    }

    public static synchronized void setTimeoutTo( int seconds )
    {
        sleepTime = seconds * 1000;
    }

    public static synchronized void pass()
    {
        if ( mainThread == Thread.currentThread() )
        {
            theTestPassed = true;
            throw new TestPassedException();
        }
        theTestPassed = true;
        testGeneratedInterrupt = true;
        mainThread.interrupt();
    }

    public static synchronized void fail()
    {
        fail( "it just plain failed! :-)" );
    }

    public static synchronized void fail( String whyFailed )
    {
        if ( mainThread == Thread.currentThread() )
        {
            throw new RuntimeException( whyFailed );
        }
        theTestPassed = false;
        testGeneratedInterrupt = true;
        failureMessage = whyFailed;
        mainThread.interrupt();
    }
}

class TestPassedException extends RuntimeException
{
}
