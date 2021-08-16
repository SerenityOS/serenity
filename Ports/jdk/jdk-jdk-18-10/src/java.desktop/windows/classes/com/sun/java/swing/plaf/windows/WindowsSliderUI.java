/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSliderUI;

import static com.sun.java.swing.plaf.windows.TMSchema.Part;
import static com.sun.java.swing.plaf.windows.TMSchema.Prop;
import static com.sun.java.swing.plaf.windows.TMSchema.State;
import static com.sun.java.swing.plaf.windows.XPStyle.Skin;

/**
 * Windows rendition of the component.
 */
public class WindowsSliderUI extends BasicSliderUI
{
    private boolean rollover = false;
    private boolean pressed = false;

    public WindowsSliderUI(JSlider b){
        super(b);
    }

    public static ComponentUI createUI(JComponent b) {
        return new WindowsSliderUI((JSlider)b);
    }


    /**
     * Overrides to return a private track listener subclass which handles
     * the HOT, PRESSED, and FOCUSED states.
     * @since 1.6
     */
    protected TrackListener createTrackListener(JSlider slider) {
        return new WindowsTrackListener();
    }

    private class WindowsTrackListener extends TrackListener {

        public void mouseMoved(MouseEvent e) {
            updateRollover(thumbRect.contains(e.getX(), e.getY()));
            super.mouseMoved(e);
        }

        public void mouseEntered(MouseEvent e) {
            updateRollover(thumbRect.contains(e.getX(), e.getY()));
            super.mouseEntered(e);
        }

        public void mouseExited(MouseEvent e) {
            updateRollover(false);
            super.mouseExited(e);
        }

        public void mousePressed(MouseEvent e) {
            updatePressed(thumbRect.contains(e.getX(), e.getY()));
            super.mousePressed(e);
        }

        public void mouseReleased(MouseEvent e) {
            updatePressed(false);
            super.mouseReleased(e);
        }

        public void updatePressed(boolean newPressed) {
            // You can't press a disabled slider
            if (!slider.isEnabled()) {
                return;
            }
            if (pressed != newPressed) {
                pressed = newPressed;
                slider.repaint(thumbRect);
            }
        }

        public void updateRollover(boolean newRollover) {
            // You can't have a rollover on a disabled slider
            if (!slider.isEnabled()) {
                return;
            }
            if (rollover != newRollover) {
                rollover = newRollover;
                slider.repaint(thumbRect);
            }
        }

    }


    public void paintTrack(Graphics g)  {
        XPStyle xp = XPStyle.getXP();
        if (xp != null) {
            boolean vertical = (slider.getOrientation() == JSlider.VERTICAL);
            Part part = vertical ? Part.TKP_TRACKVERT : Part.TKP_TRACK;
            Skin skin = xp.getSkin(slider, part);

            if (vertical) {
                int x = (trackRect.width - skin.getWidth()) / 2;
                skin.paintSkin(g, trackRect.x + x, trackRect.y,
                               skin.getWidth(), trackRect.height, null);
            } else {
                int y = (trackRect.height - skin.getHeight()) / 2;
                skin.paintSkin(g, trackRect.x, trackRect.y + y,
                               trackRect.width, skin.getHeight(), null);
            }
        } else {
            super.paintTrack(g);
        }
    }


    protected void paintMinorTickForHorizSlider( Graphics g, Rectangle tickBounds, int x ) {
        XPStyle xp = XPStyle.getXP();
        if (xp != null) {
            g.setColor(xp.getColor(slider, Part.TKP_TICS, null, Prop.COLOR, Color.black));
        }
        super.paintMinorTickForHorizSlider(g, tickBounds, x);
    }

    protected void paintMajorTickForHorizSlider( Graphics g, Rectangle tickBounds, int x ) {
        XPStyle xp = XPStyle.getXP();
        if (xp != null) {
            g.setColor(xp.getColor(slider, Part.TKP_TICS, null, Prop.COLOR, Color.black));
        }
        super.paintMajorTickForHorizSlider(g, tickBounds, x);
    }

    protected void paintMinorTickForVertSlider( Graphics g, Rectangle tickBounds, int y ) {
        XPStyle xp = XPStyle.getXP();
        if (xp != null) {
            g.setColor(xp.getColor(slider, Part.TKP_TICSVERT, null, Prop.COLOR, Color.black));
        }
        super.paintMinorTickForVertSlider(g, tickBounds, y);
    }

    protected void paintMajorTickForVertSlider( Graphics g, Rectangle tickBounds, int y ) {
        XPStyle xp = XPStyle.getXP();
        if (xp != null) {
            g.setColor(xp.getColor(slider, Part.TKP_TICSVERT, null, Prop.COLOR, Color.black));
        }
        super.paintMajorTickForVertSlider(g, tickBounds, y);
    }


    public void paintThumb(Graphics g)  {
        XPStyle xp = XPStyle.getXP();
        if (xp != null) {
            Part part = getXPThumbPart();
            State state = State.NORMAL;

            if (slider.hasFocus()) {
                state = State.FOCUSED;
            }
            if (rollover) {
                state = State.HOT;
            }
            if (pressed) {
                state = State.PRESSED;
            }
            if(!slider.isEnabled()) {
                state = State.DISABLED;
            }

            xp.getSkin(slider, part).paintSkin(g, thumbRect.x, thumbRect.y, state);
        } else {
            super.paintThumb(g);
        }
    }

    protected Dimension getThumbSize() {
        XPStyle xp = XPStyle.getXP();
        if (xp != null) {
            Dimension size = new Dimension();
            Skin s = xp.getSkin(slider, getXPThumbPart());
            size.width = s.getWidth();
            size.height = s.getHeight();
            return size;
        } else {
            return super.getThumbSize();
        }
    }

    private Part getXPThumbPart() {
        Part part;
        boolean vertical = (slider.getOrientation() == JSlider.VERTICAL);
        boolean leftToRight = slider.getComponentOrientation().isLeftToRight();
        Boolean paintThumbArrowShape =
                (Boolean)slider.getClientProperty("Slider.paintThumbArrowShape");
        if ((!slider.getPaintTicks() && paintThumbArrowShape == null) ||
            paintThumbArrowShape == Boolean.FALSE) {
                part = vertical ? Part.TKP_THUMBVERT
                                : Part.TKP_THUMB;
        } else {
                part = vertical ? (leftToRight ? Part.TKP_THUMBRIGHT : Part.TKP_THUMBLEFT)
                                : Part.TKP_THUMBBOTTOM;
        }
        return part;
    }
}
