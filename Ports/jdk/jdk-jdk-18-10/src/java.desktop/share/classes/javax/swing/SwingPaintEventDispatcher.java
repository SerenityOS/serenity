/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.PaintEvent;
import java.security.AccessController;
import sun.awt.AppContext;
import sun.awt.SunToolkit;
import sun.awt.event.IgnorePaintEvent;
import sun.security.action.GetBooleanAction;
import sun.security.action.GetPropertyAction;

/**
 * Swing's PaintEventDispatcher.  If the component specified by the PaintEvent
 * is a top level Swing component (JFrame, JWindow, JDialog, JApplet), this
 * will forward the request to the RepaintManager for eventual painting.
 *
 */
@SuppressWarnings("removal")
class SwingPaintEventDispatcher extends sun.awt.PaintEventDispatcher {
    private static final boolean SHOW_FROM_DOUBLE_BUFFER;
    private static final boolean ERASE_BACKGROUND;

    static {
        SHOW_FROM_DOUBLE_BUFFER = "true".equals(AccessController.doPrivileged(
              new GetPropertyAction("swing.showFromDoubleBuffer", "true")));
        ERASE_BACKGROUND = AccessController.doPrivileged(
                                 new GetBooleanAction("swing.nativeErase"));
    }

    public PaintEvent createPaintEvent(Component component, int x, int y,
                                         int w, int h) {
        if (component instanceof RootPaneContainer) {
            AppContext appContext = SunToolkit.targetToAppContext(component);
            RepaintManager rm = RepaintManager.currentManager(appContext);
            if (!SHOW_FROM_DOUBLE_BUFFER ||
                  !rm.show((Container)component, x, y, w, h)) {
                rm.nativeAddDirtyRegion(appContext, (Container)component,
                                        x, y, w, h);
            }
            // For backward compatibility generate an empty paint
            // event.  Not doing this broke parts of Netbeans.
            return new IgnorePaintEvent(component, PaintEvent.PAINT,
                                        new Rectangle(x, y, w, h));
        }
        else if (component instanceof SwingHeavyWeight) {
            AppContext appContext = SunToolkit.targetToAppContext(component);
            RepaintManager rm = RepaintManager.currentManager(appContext);
            rm.nativeAddDirtyRegion(appContext, (Container)component,
                                    x, y, w, h);
            return new IgnorePaintEvent(component, PaintEvent.PAINT,
                                        new Rectangle(x, y, w, h));
        }
        return super.createPaintEvent(component, x, y, w, h);
    }

    public boolean shouldDoNativeBackgroundErase(Component c) {
        return ERASE_BACKGROUND || !(c instanceof RootPaneContainer);
    }

    public boolean queueSurfaceDataReplacing(Component c, Runnable r) {
        if (c instanceof RootPaneContainer) {
            AppContext appContext = SunToolkit.targetToAppContext(c);
            RepaintManager.currentManager(appContext).
                    nativeQueueSurfaceDataRunnable(appContext, c, r);
            return true;
        }
        return super.queueSurfaceDataReplacing(c, r);
    }
}
