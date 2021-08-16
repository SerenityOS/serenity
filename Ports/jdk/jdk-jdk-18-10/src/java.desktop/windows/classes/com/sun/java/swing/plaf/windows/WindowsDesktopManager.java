/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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


package com.sun.java.swing.plaf.windows;

import javax.swing.DefaultDesktopManager;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.beans.PropertyVetoException;
import java.util.Vector;
import java.lang.ref.WeakReference;

/**
 * This class implements a DesktopManager which more closely follows
 * the MDI model than the DefaultDesktopManager.  Unlike the
 * DefaultDesktopManager policy, MDI requires that the selected
 * and activated child frames are the same, and that that frame
 * always be the top-most window.
 * <p>
 * The maximized state is managed by the DesktopManager with MDI,
 * instead of just being a property of the individual child frame.
 * This means that if the currently selected window is maximized
 * and another window is selected, that new window will be maximized.
 *
 * @see javax.swing.DefaultDesktopManager
 * @author Thomas Ball
 */
@SuppressWarnings("serial") // JDK-implementation class
public class WindowsDesktopManager extends DefaultDesktopManager
        implements java.io.Serializable, javax.swing.plaf.UIResource {

    /* The frame which is currently selected/activated.
     * We store this value to enforce MDI's single-selection model.
     */
    private WeakReference<JInternalFrame> currentFrameRef;

    public void activateFrame(JInternalFrame f) {
        JInternalFrame currentFrame = currentFrameRef != null ?
            currentFrameRef.get() : null;
        try {
            super.activateFrame(f);
            if (currentFrame != null && f != currentFrame) {
                // If the current frame is maximized, transfer that
                // attribute to the frame being activated.
                if (!currentFrame.isClosed() && currentFrame.isMaximum() &&
                    (f.getClientProperty("JInternalFrame.frameType") !=
                    "optionDialog") ) {
                    //Special case.  If key binding was used to select next
                    //frame instead of minimizing the icon via the minimize
                    //icon.
                    if (!currentFrame.isIcon()) {
                        currentFrame.setMaximum(false);
                        if (f.isMaximizable()) {
                            if (!f.isMaximum()) {
                                f.setMaximum(true);
                            } else if (f.isMaximum() && f.isIcon()) {
                                f.setIcon(false);
                            } else {
                                f.setMaximum(false);
                            }
                        }
                    }
                }
                if (currentFrame.isSelected()) {
                    currentFrame.setSelected(false);
                }
            }

            if (!f.isSelected()) {
                f.setSelected(true);
            }
        } catch (PropertyVetoException e) {}
        if (f != currentFrame) {
            currentFrameRef = new WeakReference<JInternalFrame>(f);
        }
    }

}
