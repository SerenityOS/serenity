/*
 * Copyright (c) 2009, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6799345
 * @key headful
 * @summary Tests that no exceptions are thrown from TimerQueue and
 * SwingWorker on AppContext shutdown
 * @author art
 * @modules java.desktop/sun.awt
 * @run main TestShutdown
 */

import java.awt.*;
import java.awt.event.*;

import java.util.*;

import javax.swing.*;

import sun.awt.*;

public class TestShutdown
{
    private static AppContext targetAppContext;

    private static JFrame f;
    private static JTextField tf;

    private static volatile boolean exceptionsOccurred = false;
    private static volatile boolean appcontextInitDone = false;

    private static int timerValue = 0;

    public static void main(String[] args)
        throws Exception
    {
        ThreadGroup tg = new TestThreadGroup("TTG");
        Thread t = new Thread(tg, new TestRunnable(), "InitThread");
        t.start();

        while (!appcontextInitDone)
        {
            Thread.sleep(1000);
        }

        targetAppContext.dispose();

        if (exceptionsOccurred)
        {
            throw new RuntimeException("Test FAILED: some exceptions occurred");
        }
    }

    static void initGUI()
    {
        f = new JFrame("F");
        f.setBounds(100, 100, 200, 100);
        tf = new JTextField("Test");
        f.add(tf);
        f.setVisible(true);
    }

    static void startGUI()
    {
        // caret blink Timer
        tf.requestFocusInWindow();

        // misc Timer
        ActionListener al = new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent ae)
            {
                System.out.println("Timer tick: " + timerValue++);
            }
        };
        new javax.swing.Timer(30, al).start();
    }

    static class TestThreadGroup extends ThreadGroup
    {
        public TestThreadGroup(String name)
        {
            super(name);
        }

        @Override
        public synchronized void uncaughtException(Thread thread, Throwable t)
        {
            if (t instanceof ThreadDeath)
            {
                // this one is expected, rethrow
                throw (ThreadDeath)t;
            }
            System.err.println("Test FAILED: an exception is caught in the " +
                               "target thread group on thread " + thread.getName());
            t.printStackTrace(System.err);
            exceptionsOccurred = true;
        }
    }

    static class TestRunnable implements Runnable
    {
        @Override
        public void run()
        {
            SunToolkit stk = (SunToolkit)Toolkit.getDefaultToolkit();
            targetAppContext = stk.createNewAppContext();

            // create and show frame and text field
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    initGUI();
                }
            });
            stk.realSync();

            // start some Timers
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    startGUI();
                }
            });

            // start multiple SwingWorkers
            while (!Thread.interrupted())
            {
                try
                {
                    new TestSwingWorker().execute();
                    Thread.sleep(40);
                }
                catch (Exception e)
                {
                    // exception here is expected, skip
                    break;
                }
            }
        }
    }

    static class TestSwingWorker extends SwingWorker<String, Integer>
    {
        @Override
        public String doInBackground()
        {
            Random r = new Random();
            for (int i = 0; i < 10; i++)
            {
                try
                {
                    int delay = r.nextInt() % 50;
                    Thread.sleep(delay);
                    publish(delay);
                }
                catch (Exception z)
                {
                    break;
                }
            }
            if (!appcontextInitDone)
            {
                appcontextInitDone = true;
            }
            return "Done";
        }

        @Override
        public void process(java.util.List<Integer> chunks)
        {
            for (Integer i : chunks)
            {
                System.err.println("Processed: " + i);
            }
        }
    }
}
