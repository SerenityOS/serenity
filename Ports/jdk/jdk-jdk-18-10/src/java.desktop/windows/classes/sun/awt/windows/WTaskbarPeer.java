/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package sun.awt.windows;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Taskbar.Feature;
import java.awt.Taskbar.State;
import java.awt.peer.TaskbarPeer;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import sun.awt.AWTAccessor;
import sun.awt.OSInfo;
import sun.awt.shell.ShellFolder;

final class WTaskbarPeer implements TaskbarPeer {

    private static boolean supported = false;
    private static boolean initExecuted = false;

    private static synchronized void init() {
        if (!initExecuted) {
            supported = OSInfo.getWindowsVersion()
                    .compareTo(OSInfo.WINDOWS_7) >= 0
                    && ShellFolder.invoke(() -> nativeInit());
        }
        initExecuted = true;
    }

    static boolean isTaskbarSupported() {
        init();
        return supported;
    }

    WTaskbarPeer() {
        init();
    }

    @Override
    public boolean isSupported(Feature feature) {
        switch(feature) {
            case ICON_BADGE_IMAGE_WINDOW:
            case PROGRESS_STATE_WINDOW:
            case PROGRESS_VALUE_WINDOW:
                return supported;
            case USER_ATTENTION_WINDOW:
                return true;
            default:
                return false;
        }
    }

    private static int[] imageToArray(Image image) {
        if (image == null) {
            return null;
        }

        int w = image.getWidth(null);
        int h = image.getHeight(null);

        if (w < 0 || h < 0) {
            return null;
        }

        BufferedImage bimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g2 = bimg.createGraphics();
        g2.setComposite(AlphaComposite.Src);
        g2.drawImage(image, 0, 0, null);
        g2.dispose();

        return ((DataBufferInt) bimg.getRaster().getDataBuffer()).getData();
    }

    @Override
    public void setWindowIconBadge(Window window, final Image image) {
        WWindowPeer wp = AWTAccessor.getComponentAccessor().getPeer(window);
        if (wp != null) {
            int[] buffer = imageToArray(image);
            ShellFolder.invoke(() -> {
               setOverlayIcon(wp.getHWnd(), buffer,
                                buffer != null ? image.getWidth(null) : 0,
                                buffer != null ? image.getHeight(null) : 0);
               return null;
            });
        }
    }

    @Override
    public void requestWindowUserAttention(Window window) {
        WWindowPeer wp = AWTAccessor.getComponentAccessor().getPeer(window);
        if (wp != null) {
            flashWindow(wp.getHWnd());
        }
    }

    @Override
    public void setWindowProgressValue(Window window, int value) {
        WWindowPeer wp = AWTAccessor.getComponentAccessor().getPeer(window);
        if (wp != null) {
            ShellFolder.invoke(() -> {
                setProgressValue(wp.getHWnd(), value);
                return null;
            });
        }
    }

    @Override
    public void setWindowProgressState(Window window, State state) {
        WWindowPeer wp = AWTAccessor.getComponentAccessor().getPeer(window);
        if (wp != null) {
            ShellFolder.invoke(() -> {
                setProgressState(wp.getHWnd(), state);
                return null;
            });
        }
    }

    private static native boolean nativeInit();

    private native void setProgressValue(long hwnd, int value);

    private native void setProgressState(long hwnd, State state);

    private native void setOverlayIcon(long hwnd, int[] badge, int width, int height);

    private native void flashWindow(long hWnd);

}
