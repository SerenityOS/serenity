/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.swing.interop;

import java.awt.AWTEvent;
import java.awt.Container;
import java.awt.Component;
import java.awt.event.WindowFocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyEvent;
import sun.awt.LightweightFrame;
import sun.awt.UngrabEvent;
import sun.awt.AWTAccessor;
import sun.swing.JLightweightFrame;

/**
 * This class wraps sun.swing.JLightweightFrame and implements
 * APIs to be used by FX swing interop to access and use JLightweightFrame APIs.
 *
 * @since 11
 */
public class LightweightFrameWrapper {

    JLightweightFrame lwFrame;

    public LightweightFrameWrapper() {
        lwFrame = new JLightweightFrame();
    }

    private JLightweightFrame getLightweightFrame() {
        return lwFrame;
    }

    public void notifyDisplayChanged(final int scaleFactor) {
        if (lwFrame != null) {
            lwFrame.notifyDisplayChanged(scaleFactor, scaleFactor);
        }
    }

    /**
     * {@code overrideNativeWindowHandle()} is package private but
     * part of the interface of this class. It supports providing a
     * foreign native window handle (i.e. an FX window handle) to AWT,
     * and as such is intended to be called via JNI code,
     * not by Java code, so it is not public.
     */
    void overrideNativeWindowHandle(long handle, Runnable closeWindow) {
        if (lwFrame != null) {
            lwFrame.overrideNativeWindowHandle(handle, closeWindow);
        }
    }

    public void setHostBounds(int x, int y, int w, int h) {
        if (lwFrame != null) {
            lwFrame.setHostBounds(x, y, w, h);
        }
    }

    public void dispose() {
        if (lwFrame != null) {
            lwFrame.dispose();
        }
    }

    public void addWindowFocusListener(WindowFocusListener listener) {
        if (lwFrame != null) {
            lwFrame.addWindowFocusListener(listener);
        }
    }

    public void setVisible(boolean visible) {
        if (lwFrame != null) {
            lwFrame.setVisible(visible);
        }
    }

    public void setBounds(int x, int y, int w, int h) {
        if (lwFrame != null) {
            lwFrame.setBounds(x, y, w, h);
        }
    }

    public void setContent(final LightweightContentWrapper lwCntWrapper) {
        if (lwFrame != null) {
            lwFrame.setContent(lwCntWrapper.getContent());
        }
    }

    public void emulateActivation(boolean activate) {
        if (lwFrame != null) {
            lwFrame.emulateActivation(activate);
        }
    }

    public MouseEvent createMouseEvent(LightweightFrameWrapper lwFrame,
                            int swingID, long swingWhen, int swingModifiers,
                            int relX, int relY, int absX, int absY,
                            int clickCount, boolean swingPopupTrigger,
                            int swingButton) {
        return new java.awt.event.MouseEvent(lwFrame.getLightweightFrame(),
                                             swingID, swingWhen,
                                             swingModifiers,
                                             relX, relY, absX, absY, clickCount,
                                             swingPopupTrigger, swingButton);
    }

    public MouseWheelEvent createMouseWheelEvent(LightweightFrameWrapper lwFrame,
                            int swingModifiers, int x, int y, int wheelRotation) {
        return  new MouseWheelEvent(lwFrame.getLightweightFrame(),
                                    java.awt.event.MouseEvent.MOUSE_WHEEL,
                                    System.currentTimeMillis(),
                                    swingModifiers, x, y, 0, 0, 0, false,
                                    MouseWheelEvent.WHEEL_UNIT_SCROLL, 1,
                                    wheelRotation);
    }

    public KeyEvent createKeyEvent(LightweightFrameWrapper lwFrame,
                                   int swingID, long swingWhen,
                                   int swingModifiers,
                                   int swingKeyCode, char swingChar) {
        return new java.awt.event.KeyEvent(lwFrame.getLightweightFrame(),
                       swingID, swingWhen, swingModifiers, swingKeyCode,
                       swingChar);
    }

    public AWTEvent createUngrabEvent(LightweightFrameWrapper lwFrame) {
        return new UngrabEvent(lwFrame.getLightweightFrame());
    }

    public Component findComponentAt(LightweightFrameWrapper cont, int x, int y, boolean ignoreEnabled) {
        Container lwframe = cont.getLightweightFrame();
        return AWTAccessor.getContainerAccessor().findComponentAt(lwframe, x, y, ignoreEnabled);
    }

    public boolean isCompEqual(Component c, LightweightFrameWrapper lwFrame) {
        return c != lwFrame.getLightweightFrame();
    }
}
