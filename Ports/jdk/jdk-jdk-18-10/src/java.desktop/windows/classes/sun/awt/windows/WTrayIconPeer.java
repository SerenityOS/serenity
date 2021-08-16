/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Graphics2D;
import java.awt.AWTEvent;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.PopupMenu;
import java.awt.Point;
import java.awt.TrayIcon;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.peer.TrayIconPeer;
import java.awt.image.*;

import sun.awt.AWTAccessor;
import sun.awt.SunToolkit;
import sun.awt.image.IntegerComponentRaster;
import sun.java2d.pipe.Region;

final class WTrayIconPeer extends WObjectPeer implements TrayIconPeer {
    static final int TRAY_ICON_WIDTH = 16;
    static final int TRAY_ICON_HEIGHT = 16;
    static final int TRAY_ICON_MASK_SIZE = (TRAY_ICON_WIDTH * TRAY_ICON_HEIGHT) / 8;

    IconObserver observer = new IconObserver();
    boolean firstUpdate = true;
    Frame popupParent = new Frame("PopupMessageWindow");
    PopupMenu popup;

    @Override
    protected void disposeImpl() {
        if (popupParent != null) {
            popupParent.dispose();
        }
        popupParent.dispose();
        _dispose();
        WToolkit.targetDisposedPeer(target, this);
    }

    WTrayIconPeer(TrayIcon target) {
        this.target = target;
        popupParent.addNotify();
        create();
        updateImage();
    }

    @Override
    public void updateImage() {
        Image image = ((TrayIcon)target).getImage();
        if (image != null) {
            updateNativeImage(image);
        }
    }

    @Override
    public native void setToolTip(String tooltip);

    @Override
    public synchronized void showPopupMenu(final int x, final int y) {
        if (isDisposed())
            return;

        SunToolkit.executeOnEventHandlerThread(target, () -> {
            PopupMenu newPopup = ((TrayIcon)target).getPopupMenu();
            if (popup != newPopup) {
                if (popup != null) {
                    popupParent.remove(popup);
                }
                if (newPopup != null) {
                    popupParent.add(newPopup);
                }
                popup = newPopup;
            }
            if (popup != null) {
                WPopupMenuPeer peer = AWTAccessor.getMenuComponentAccessor()
                                                 .getPeer(popup);
                peer.show(popupParent, new Point(x, y));
            }
        });
    }

    @Override
    public void displayMessage(String caption, String text, String messageType) {
        // The situation when both caption and text are null is processed in the shared code.
        if (caption == null) {
            caption = "";
        }
        if (text == null) {
            text = "";
        }
        _displayMessage(caption, text, messageType);
    }


    // ***********************************************
    // ***********************************************


    synchronized void updateNativeImage(Image image) {
        if (isDisposed())
            return;

        boolean autosize = ((TrayIcon)target).isImageAutoSize();
        AffineTransform tx = GraphicsEnvironment.getLocalGraphicsEnvironment().
                getDefaultScreenDevice().getDefaultConfiguration().
                getDefaultTransform();
        int w = Region.clipScale(TRAY_ICON_WIDTH, tx.getScaleX());
        int h = Region.clipScale(TRAY_ICON_HEIGHT, tx.getScaleY());
        int imgWidth = Region.clipScale(image.getWidth(observer), tx.getScaleX());
        int imgHeight = Region.clipScale(image.getHeight(observer), tx.getScaleY());
        BufferedImage bufImage = new BufferedImage(w,
                h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr = bufImage.createGraphics();
        if (gr != null) {
            try {
                gr.setPaintMode();

                gr.drawImage(image, 0, 0, (autosize ? w : imgWidth),
                             (autosize ? h : imgHeight), observer);

                createNativeImage(bufImage);

                updateNativeIcon(!firstUpdate);
                if (firstUpdate) firstUpdate = false;

            } finally {
                gr.dispose();
            }
        }
    }

    void createNativeImage(BufferedImage bimage) {
        Raster raster = bimage.getRaster();
        byte[] andMask = new byte[TRAY_ICON_MASK_SIZE];
        int[]  pixels = ((DataBufferInt)raster.getDataBuffer()).getData();
        int npixels = pixels.length;
        int ficW = raster.getWidth();

        for (int i = 0; i < npixels; i++) {
            int ibyte = i / 8;
            int omask = 1 << (7 - (i % 8));

            if ((pixels[i] & 0xff000000) == 0) {
                // Transparent bit
                if (ibyte < andMask.length) {
                    andMask[ibyte] |= omask;
                }
            }
        }

        if (raster instanceof IntegerComponentRaster) {
            ficW = ((IntegerComponentRaster)raster).getScanlineStride();
        }
        setNativeIcon(((DataBufferInt)bimage.getRaster().getDataBuffer()).getData(),
                      andMask, ficW, raster.getWidth(), raster.getHeight());
    }

    void postEvent(AWTEvent event) {
        WToolkit.postEvent(WToolkit.targetToAppContext(target), event);
    }

    native void create();
    synchronized native void _dispose();

    /*
     * Updates/adds the icon in/to the system tray.
     * @param doUpdate if {@code true}, updates the icon,
     * otherwise, adds the icon
     */
    native void updateNativeIcon(boolean doUpdate);

    native void setNativeIcon(int[] rData, byte[] andMask, int nScanStride,
                              int width, int height);

    native void _displayMessage(String caption, String text, String messageType);

    class IconObserver implements ImageObserver {
        @Override
        public boolean imageUpdate(Image image, int flags, int x, int y, int width, int height) {
            if (image != ((TrayIcon)target).getImage() || // if the image has been changed
                isDisposed())
            {
                return false;
            }
            if ((flags & (ImageObserver.FRAMEBITS | ImageObserver.ALLBITS |
                          ImageObserver.WIDTH | ImageObserver.HEIGHT)) != 0)
            {
                updateNativeImage(image);
            }
            return (flags & ImageObserver.ALLBITS) == 0;
        }
    }
}
