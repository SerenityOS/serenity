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

import java.awt.Point;
import java.awt.Window;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.lang.InterruptedException;
import java.lang.System;
import java.lang.Thread;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * @test
 * @key headful
 * @bug 8024185
 * @summary Native Mac OS X full screen does not work after showing the splash
 * @requires (os.family == "mac")
 * @library ../
 * @library ../../../../lib/testlibrary
 * @modules java.desktop/sun.awt
 *          java.desktop/com.apple.eawt
 * @build GenerateTestImage
 * @run main GenerateTestImage
 * @author Petr Pchelko area=awt.event
 * @run main/othervm -splash:test.png FullScreenAfterSplash
 */

public class FullScreenAfterSplash {

    private static JFrame frame;

    private static volatile boolean windowEnteringFullScreen = false;
    private static volatile boolean windowEnteredFullScreen = false;

    public static void main(String[] args) throws Exception {

        try {
            //Move the mouse out, because it could interfere with the test.
            Robot r = new Robot();
            r.setAutoDelay(50);
            r.mouseMove(0, 0);
            sleep();

            SwingUtilities.invokeAndWait(FullScreenAfterSplash::createAndShowGUI);
            sleep();

            Point fullScreenButtonPos = frame.getLocation();
            if(System.getProperty("os.version").equals("10.9"))
                fullScreenButtonPos.translate(frame.getWidth() - 10, frame.getHeight()/2);
            else
                fullScreenButtonPos.translate(55,frame.getHeight()/2);
            r.mouseMove(fullScreenButtonPos.x, fullScreenButtonPos.y);

            //Cant use waitForIdle for full screen transition.
            int waitCount = 0;
            while (!windowEnteringFullScreen) {
                r.mousePress(InputEvent.BUTTON1_MASK);
                r.mouseRelease(InputEvent.BUTTON1_MASK);
                Thread.sleep(100);
                if (waitCount++ > 10) {
                    System.err.println("Can't enter full screen mode. Failed.");
                    System.exit(1);
                }
            }

            waitCount = 0;
            while (!windowEnteredFullScreen) {
                Thread.sleep(100);
                if (waitCount++ > 10) {
                    System.err.println("Can't enter full screen mode. Failed.");
                    System.exit(1);
                }
            }
        } finally {
            if (frame != null) {
                frame.dispose();
            }
        }
    }

    private static void createAndShowGUI() {
        frame = new JFrame(" Fullscreen OSX Bug ");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        enableFullScreen(frame);
        frame.setBounds(100, 100, 100, 100);
        frame.pack();
        frame.setVisible(true);
    }

    /*
     *  Use reflection to make a test compilable on not Mac OS X
     */
    private static void enableFullScreen(Window window) {
        try {
            Class<?> fullScreenUtilities = Class.forName("com.apple.eawt.FullScreenUtilities");
            Method setWindowCanFullScreen = fullScreenUtilities.getMethod("setWindowCanFullScreen", Window.class, boolean.class);
            setWindowCanFullScreen.invoke(fullScreenUtilities, window, true);
            Class fullScreenListener = Class.forName("com.apple.eawt.FullScreenListener");
            Object listenerObject = Proxy.newProxyInstance(fullScreenListener.getClassLoader(), new Class[]{fullScreenListener}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "windowEnteringFullScreen":
                        windowEnteringFullScreen = true;
                        break;
                    case "windowEnteredFullScreen":
                        windowEnteredFullScreen = true;
                        break;
                }
                return null;
            });
            Method addFullScreenListener = fullScreenUtilities.getMethod("addFullScreenListenerTo", Window.class, fullScreenListener);
            addFullScreenListener.invoke(fullScreenUtilities, window, listenerObject);
        } catch (Exception e) {
            throw new RuntimeException("FullScreen utilities not available", e);
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) { }
    }
}
