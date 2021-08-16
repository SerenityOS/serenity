/*
* Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @test
 * @key headful
 * @bug 8013468
 * @summary Cursor does not update properly when in fullscreen mode on Mac
 *    The core reason of the issue was the lack of a mouse entered event in fullscreen
 * @requires (os.family == "mac")
 * @modules java.desktop/com.apple.eawt
 * @library ../../regtesthelpers
 * @build Util
 * @modules java.desktop/com.apple.eawt
 * @author Petr Pchelko area=awt.event
 * @run main FullscreenEnterEventTest
 */

public class FullscreenEnterEventTest {

    private static String OS = System.getProperty("os.name").toLowerCase();

    private static JFrame frame;

    private static volatile int mouseEnterCount = 0;

    private static volatile boolean windowEnteringFullScreen = false;
    private static volatile boolean windowEnteredFullScreen = false;

    public static void main(String[] args) throws Exception {

        if (!OS.contains("mac")) {
            System.out.println("The test is applicable only to Mac OS X. Passed");
            return;
        }

        //Move the mouse out, because it could interfere with the test.
        Robot r = Util.createRobot();
        r.setAutoDelay(50);
        Util.waitForIdle(r);
        r.mouseMove(0, 0);
        Util.waitForIdle(r);

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                createAndShowGUI();
            }
        });

        //Move the mouse away from the frame and check the View-base full screen mode
        Util.waitForIdle(r);
        r.mouseMove(500, 500);
        Util.waitForIdle(r);
        mouseEnterCount = 0;
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(frame);
            }
        });
        Util.waitForIdle(r);
        r.delay(150);
        if (mouseEnterCount != 1) {
            throw new RuntimeException("No MouseEntered event for view-base full screen. Failed.");
        }
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(null);
            }
        });

        //Test native full screen support
        Util.waitForIdle(r);
        Point fullScreenButtonPos = frame.getLocation();
        fullScreenButtonPos.translate(frame.getWidth() - 10, 10);
        r.mouseMove(fullScreenButtonPos.x, fullScreenButtonPos.y);
        r.delay(150);
        mouseEnterCount = 0;

        //Cant use waitForIdle for full screen transition.
        int waitCount = 0;
        while (!windowEnteringFullScreen) {
            r.mousePress(InputEvent.BUTTON1_MASK);
            r.mouseRelease(InputEvent.BUTTON1_MASK);
            Thread.sleep(100);
            if (waitCount++ > 10) throw new RuntimeException("Can't enter full screen mode. Failed");
        }

        waitCount = 0;
        while (!windowEnteredFullScreen) {
            Thread.sleep(200);
            if (waitCount++ > 10) throw new RuntimeException("Can't enter full screen mode. Failed");
        }

        if (mouseEnterCount != 1) {
            throw new RuntimeException("No MouseEntered event for native full screen. Failed. mouseEnterCount:"+mouseEnterCount);
        }
    }

    private static void createAndShowGUI() {
        frame = new JFrame(" Fullscreen OSX Bug ");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        enableFullScreen(frame);
        frame.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                mouseEnterCount++;
            }
        });
        frame.setBounds(100, 100, 100, 100);
        frame.pack();
        frame.setVisible(true);

    }

    /*
     *  Use reflection to make a test compilable on not Mac OS X
     */
    private static void enableFullScreen(Window window) {
        try {
            Class fullScreenUtilities = Class.forName("com.apple.eawt.FullScreenUtilities");
            Method setWindowCanFullScreen = fullScreenUtilities.getMethod("setWindowCanFullScreen", Window.class, boolean.class);
            setWindowCanFullScreen.invoke(fullScreenUtilities, window, true);
            Class fullScreenListener = Class.forName("com.apple.eawt.FullScreenListener");
            Object listenerObject = Proxy.newProxyInstance(fullScreenListener.getClassLoader(), new Class[]{fullScreenListener}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    switch (method.getName()) {
                        case "windowEnteringFullScreen":
                            windowEnteringFullScreen = true;
                            break;
                        case "windowEnteredFullScreen":
                            windowEnteredFullScreen = true;
                            break;
                    }
                    return null;
                }
            });
            Method addFullScreenListener = fullScreenUtilities.getMethod("addFullScreenListenerTo", Window.class, fullScreenListener);
            addFullScreenListener.invoke(fullScreenUtilities, window, listenerObject);
        } catch (Exception e) {
            throw new RuntimeException("FullScreen utilities not available", e);
        }
    }

}
