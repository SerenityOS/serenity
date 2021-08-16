/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.apple.laf;

import java.awt.*;
import java.beans.PropertyVetoException;
import java.util.Vector;

import javax.swing.*;

/**
 * Based on AquaInternalFrameManager
 *
 * DesktopManager implementation for Aqua
 *
 * Mac is more like Windows than it's like Motif/Basic
 *
 *    From WindowsDesktopManager:
 *
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
 * @see com.sun.java.swing.plaf.windows.WindowsDesktopManager
 */
@SuppressWarnings("serial") // JDK implementation class
public class AquaInternalFrameManager extends DefaultDesktopManager {
    // Variables

    /* The frame which is currently selected/activated.
     * We store this value to enforce Mac's single-selection model.
     */
    JInternalFrame fCurrentFrame;
    JInternalFrame fInitialFrame;
    AquaInternalFramePaneUI fCurrentPaneUI;

    /* The list of frames, sorted by order of creation.
     * This list is necessary because by default the order of
     * child frames in the JDesktopPane changes during frame
     * activation (the activated frame is moved to index 0).
     * We preserve the creation order so that "next" and "previous"
     * frame actions make sense.
     */
    Vector<JInternalFrame> fChildFrames = new Vector<JInternalFrame>(1);

    public void closeFrame(final JInternalFrame f) {
        if (f == fCurrentFrame) {
            activateNextFrame();
        }
        fChildFrames.removeElement(f);
        super.closeFrame(f);
    }

    public void deiconifyFrame(final JInternalFrame f) {
        JInternalFrame.JDesktopIcon desktopIcon;

        desktopIcon = f.getDesktopIcon();
        // If the icon moved, move the frame to that spot before expanding it
        // reshape does delta checks for us
        f.reshape(desktopIcon.getX(), desktopIcon.getY(), f.getWidth(), f.getHeight());
        super.deiconifyFrame(f);
    }

    void addIcon(final Container c, final JInternalFrame.JDesktopIcon desktopIcon) {
        c.add(desktopIcon);
    }

    /** Removes the frame from its parent and adds its desktopIcon to the parent. */
    public void iconifyFrame(final JInternalFrame f) {
        // Same as super except doesn't deactivate it
        JInternalFrame.JDesktopIcon desktopIcon;
        Container c;

        desktopIcon = f.getDesktopIcon();
        // Position depends on *current* position of frame, unlike super which reuses the first position
        final Rectangle r = getBoundsForIconOf(f);
        desktopIcon.setBounds(r.x, r.y, r.width, r.height);
        if (!wasIcon(f)) {
            setWasIcon(f, Boolean.TRUE);
        }
        c = f.getParent();
        if (c == null) return;

        c.remove(f);
        addIcon(c, desktopIcon);
        c.repaint(f.getX(), f.getY(), f.getWidth(), f.getHeight());
    }

    // WindowsDesktopManager code
    public void activateFrame(final JInternalFrame f) {
        try {
            if (f != null) super.activateFrame(f);

            // If this is the first activation, add to child list.
            if (fChildFrames.indexOf(f) == -1) {
                fChildFrames.addElement(f);
            }

            if (fCurrentFrame != null && f != fCurrentFrame) {
                if (fCurrentFrame.isSelected()) {
                    fCurrentFrame.setSelected(false);
                }
            }

            if (f != null && !f.isSelected()) {
                f.setSelected(true);
            }

            fCurrentFrame = f;
        } catch(final PropertyVetoException e) {}
    }

    private void switchFrame(final boolean next) {
        if (fCurrentFrame == null) {
            // initialize first frame we find
            if (fInitialFrame != null) activateFrame(fInitialFrame);
            return;
        }

        final int count = fChildFrames.size();
        if (count <= 1) {
            // No other child frames.
            return;
        }

        final int currentIndex = fChildFrames.indexOf(fCurrentFrame);
        if (currentIndex == -1) {
            // the "current frame" is no longer in the list
            fCurrentFrame = null;
            return;
        }

        int nextIndex;
        if (next) {
            nextIndex = currentIndex + 1;
            if (nextIndex == count) {
                nextIndex = 0;
            }
        } else {
            nextIndex = currentIndex - 1;
            if (nextIndex == -1) {
                nextIndex = count - 1;
            }
        }
        final JInternalFrame f = fChildFrames.elementAt(nextIndex);
        activateFrame(f);
        fCurrentFrame = f;
    }

    /**
     * Activate the next child JInternalFrame, as determined by
     * the frames' Z-order.  If there is only one child frame, it
     * remains activated.  If there are no child frames, nothing
     * happens.
     */
    public void activateNextFrame() {
        switchFrame(true);
    }

    /** same as above but will activate a frame if none
     *  have been selected
     */
    public void activateNextFrame(final JInternalFrame f) {
        fInitialFrame = f;
        switchFrame(true);
    }

    /**
     * Activate the previous child JInternalFrame, as determined by
     * the frames' Z-order.  If there is only one child frame, it
     * remains activated.  If there are no child frames, nothing
     * happens.
     */
    public void activatePreviousFrame() {
        switchFrame(false);
    }
}
