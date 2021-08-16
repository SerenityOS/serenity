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

import java.awt.DisplayMode;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.util.ArrayList;
import java.util.Random;

import static java.awt.DisplayMode.REFRESH_RATE_UNKNOWN;

/**
 * @test
 * @key headful
 * @bug 6430607 8198613
 * @summary Test that we throw an exception for incorrect display modes
 * @author Dmitri.Trembovetski@Sun.COM area=FullScreen
 * @run main/othervm NonExistentDisplayModeTest
 * @run main/othervm -Dsun.java2d.noddraw=true NonExistentDisplayModeTest
 */
public class NonExistentDisplayModeTest {

    public static void main(String[] args) {
        new NonExistentDisplayModeTest().start();
    }

    private void start() {
        Frame f = new Frame("Testing, please wait..");
        f.pack();
        GraphicsDevice gd = f.getGraphicsConfiguration().getDevice();
        if (!gd.isFullScreenSupported()) {
            System.out.println("Exclusive FS mode not supported, test passed.");
            f.dispose();
            return;
        }

        gd.setFullScreenWindow(f);
        if (!gd.isDisplayChangeSupported()) {
            System.out.println("DisplayMode change not supported, test passed.");
            f.dispose();
            return;
        }

        DisplayMode dms[] = gd.getDisplayModes();
        ArrayList<DisplayMode> dmList = new ArrayList<DisplayMode>(dms.length);
        for (DisplayMode dm : dms) {
            dmList.add(dm);
        }

        ArrayList<DisplayMode> nonExistentDms = createNonExistentDMList(dmList);

        for (DisplayMode dm : nonExistentDms) {
            boolean exThrown = false;
            try {
                System.out.printf("Testing mode: (%4dx%4d) depth=%3d rate=%d\n",
                                  dm.getWidth(), dm.getHeight(),
                                  dm.getBitDepth(), dm.getRefreshRate());
                gd.setDisplayMode(dm);
            } catch (IllegalArgumentException e) {
                exThrown = true;
            }
            if (!exThrown) {
                gd.setFullScreenWindow(null);
                f.dispose();
                throw new
                    RuntimeException("Failed: No exception thrown for dm "+dm);
            }
        }
        gd.setFullScreenWindow(null);
        f.dispose();
        System.out.println("Test passed.");
    }

    private static final Random rnd = new Random();
    private ArrayList<DisplayMode>
        createNonExistentDMList(ArrayList<DisplayMode> dmList)
    {
        ArrayList<DisplayMode> newList =
            new ArrayList<DisplayMode>(dmList.size());
        // vary one parameter at a time
        int param = 0;
        for (DisplayMode dm : dmList) {
            param = ++param % 3;
            switch (param) {
                case 0: {
                    DisplayMode newDM = deriveSize(dm);
                    if (!dmList.contains(newDM)) {
                        newList.add(newDM);
                    }
                    break;
                }
                case 1: {
                    DisplayMode newDM = deriveDepth(dm);
                    if (!dmList.contains(newDM)) {
                        newList.add(newDM);
                    }
                    break;
                }
                case 2: {
                    if (dm.getRefreshRate() != REFRESH_RATE_UNKNOWN) {
                        DisplayMode newDM = deriveRR(dm);
                        if (!dmList.contains(newDM)) {
                            newList.add(newDM);
                        }
                    }
                    break;
                }
            }
        }
        return newList;
    }

    private static DisplayMode deriveSize(DisplayMode dm) {
        int w = dm.getWidth() / 7;
        int h = dm.getHeight() / 3;
        return new DisplayMode(w, h, dm.getBitDepth(), dm.getRefreshRate());
    }
    private static DisplayMode deriveRR(DisplayMode dm) {
        return new DisplayMode(dm.getWidth(), dm.getHeight(),
                               dm.getBitDepth(), 777);
    }
    private static DisplayMode deriveDepth(DisplayMode dm) {
        int depth;
        if (dm.getBitDepth() == DisplayMode.BIT_DEPTH_MULTI) {
            depth = 77;
        } else {
            depth = DisplayMode.BIT_DEPTH_MULTI;
        }
        return new DisplayMode(dm.getWidth(), dm.getHeight(),
                               depth, dm.getRefreshRate());
    }
}
