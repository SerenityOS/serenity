/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package sun.awt.X11;

import java.awt.*;
import java.awt.peer.*;

import sun.awt.SunToolkit;

import sun.awt.X11GraphicsConfig;
import sun.awt.X11GraphicsDevice;

class XCanvasPeer extends XComponentPeer implements CanvasPeer {

    private boolean eraseBackgroundDisabled;

    XCanvasPeer() {}

    XCanvasPeer(XCreateWindowParams params) {
        super(params);
    }

    XCanvasPeer(Component target) {
        super(target);
    }

    void preInit(XCreateWindowParams params) {
        super.preInit(params);
        if (SunToolkit.getSunAwtNoerasebackground()) {
            disableBackgroundErase();
        }
    }

    /* Get a GraphicsConfig with the same visual on the new
     * screen, which should be easy in Xinerama mode.
     */
    public GraphicsConfiguration getAppropriateGraphicsConfiguration(
                                    GraphicsConfiguration gc)
    {
        if (graphicsConfig == null || gc == null) {
            return gc;
        }
        // Opt: Only need to do if we're not using the default GC

        int screenNum = ((X11GraphicsDevice)gc.getDevice()).getScreen();

        X11GraphicsConfig parentgc;
        // save vis id of current gc
        int visual = graphicsConfig.getVisual();

        X11GraphicsDevice newDev = (X11GraphicsDevice) GraphicsEnvironment.
            getLocalGraphicsEnvironment().
            getScreenDevices()[screenNum];

        for (int i = 0; i < newDev.getNumConfigs(screenNum); i++) {
            if (visual == newDev.getConfigVisualId(i, screenNum)) {
                // use that
                graphicsConfig = (X11GraphicsConfig)newDev.getConfigurations()[i];
                break;
            }
        }
        // just in case...
        if (graphicsConfig == null) {
            graphicsConfig = (X11GraphicsConfig) GraphicsEnvironment.
                getLocalGraphicsEnvironment().
                getScreenDevices()[screenNum].
                getDefaultConfiguration();
        }

        return graphicsConfig;
    }

    protected boolean shouldFocusOnClick() {
        // Canvas should always be able to be focused by mouse clicks.
        return true;
    }

    public void disableBackgroundErase() {
        eraseBackgroundDisabled = true;
    }
    protected boolean doEraseBackground() {
        return !eraseBackgroundDisabled;
    }
}
