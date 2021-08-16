/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

import sun.awt.*;
import java.awt.*;
import java.awt.event.InvocationEvent;
import java.awt.peer.ComponentPeer;
import java.awt.image.*;

import sun.awt.image.ByteInterleavedRaster;
import sun.security.action.GetPropertyAction;

import java.awt.peer.FramePeer;
import java.security.PrivilegedAction;
import  java.security.AccessController;

@SuppressWarnings("serial") // JDK-implementation class
public class WEmbeddedFrame extends EmbeddedFrame {

    static {
        initIDs();
    }

    private long handle;

    private int bandWidth = 0;
    private int bandHeight = 0;
    private int imgWid = 0;
    private int imgHgt = 0;

    private static int pScale = 0;
    private static final int MAX_BAND_SIZE = (1024*30);

    /**
     * This flag is set to {@code true} if this embedded frame is hosted by Internet Explorer.
     */
    private boolean isEmbeddedInIE = false;

    @SuppressWarnings("removal")
    private static String printScale = AccessController.doPrivileged(
        new GetPropertyAction("sun.java2d.print.pluginscalefactor"));

    public WEmbeddedFrame() {
        this((long)0);
    }

    /**
     * @deprecated This constructor will be removed in 1.5
     */
    @Deprecated
    public WEmbeddedFrame(int handle) {
        this((long)handle);
    }

    public WEmbeddedFrame(long handle) {
        this.handle = handle;
        if (handle != 0) {
            addNotify();
            show();
        }
    }

    public void addNotify() {
        if (!isDisplayable()) {
            WToolkit toolkit = (WToolkit)Toolkit.getDefaultToolkit();
            setPeer(toolkit.createEmbeddedFrame(this));
        }
        super.addNotify();
    }

    /*
     * Get the native handle
    */
    public long getEmbedderHandle() {
        return handle;
    }

    /*
     * Print the embedded frame and its children using the specified HDC.
     */

    void print(long hdc) {
        BufferedImage bandImage = null;

        int xscale = 1;
        int yscale = 1;

        /* Is this is either a printer DC or an enhanced meta file DC ?
         * Mozilla passes in a printer DC, IE passes plug-in a DC for an
         * enhanced meta file. Its possible we may be passed to a memory
         * DC. If we here create a larger image, draw in to it and have
         * that memory DC then lose the image resolution only to scale it
         * back up again when sending to a printer it will look really bad.
         * So, is this is either a printer DC or an enhanced meta file DC ?
         * Scale only if it is. Use a 4x scale factor, partly since for
         * an enhanced meta file we don't know anything about the
         * real resolution of the destination.
         *
         * For a printer DC we could probably derive the scale factor to use
         * by querying LOGPIXELSX/Y, and dividing that by the screen
         * resolution (typically 96 dpi or 120 dpi) but that would typically
         * make for even bigger output for marginal extra quality.
         * But for enhanced meta file we don't know anything about the
         * real resolution of the destination so
         */
        if (isPrinterDC(hdc)) {
            xscale = yscale = getPrintScaleFactor();
        }

        int frameHeight = getHeight();
        if (bandImage == null) {
            bandWidth = getWidth();
            if (bandWidth % 4 != 0) {
                bandWidth += (4 - (bandWidth % 4));
            }
            if (bandWidth <= 0) {
                return;
            }

            bandHeight = Math.min(MAX_BAND_SIZE/bandWidth, frameHeight);

            imgWid = bandWidth * xscale;
            imgHgt = bandHeight * yscale;
            bandImage = new BufferedImage(imgWid, imgHgt,
                                          BufferedImage.TYPE_3BYTE_BGR);
        }

        Graphics clearGraphics = bandImage.getGraphics();
        clearGraphics.setColor(Color.white);
        Graphics2D g2d = (Graphics2D)bandImage.getGraphics();
        g2d.translate(0, imgHgt);
        g2d.scale(xscale, -yscale);

        ByteInterleavedRaster ras = (ByteInterleavedRaster)bandImage.getRaster();
        byte[] data = ras.getDataStorage();

        for (int bandTop = 0; bandTop < frameHeight; bandTop += bandHeight) {
            clearGraphics.fillRect(0, 0, bandWidth, bandHeight);

            printComponents(g2d);
            int imageOffset =0;
            int currBandHeight = bandHeight;
            int currImgHeight = imgHgt;
            if ((bandTop+bandHeight) > frameHeight) {
                // last band
                currBandHeight = frameHeight - bandTop;
                currImgHeight = currBandHeight*yscale;

                // multiply by 3 because the image is a 3 byte BGR
                imageOffset = imgWid*(imgHgt-currImgHeight)*3;
            }

            printBand(hdc, data, imageOffset,
                      0, 0, imgWid, currImgHeight,
                      0, bandTop, bandWidth, currBandHeight);
            g2d.translate(0, -bandHeight);
        }
    }

    @SuppressWarnings("removal")
    protected static int getPrintScaleFactor() {
        // check if value is already cached
        if (pScale != 0)
            return pScale;
        if (printScale == null) {
            // if no system property is specified,
            // check for environment setting
            printScale = AccessController.doPrivileged(
                new PrivilegedAction<String>() {
                    public String run() {
                        return System.getenv("JAVA2D_PLUGIN_PRINT_SCALE");
                    }
                }
            );
        }
        int default_printDC_scale = 4;
        int scale = default_printDC_scale;
        if (printScale != null) {
            try {
                scale = Integer.parseInt(printScale);
                if (scale > 8 || scale < 1) {
                    scale = default_printDC_scale;
                }
            } catch (NumberFormatException nfe) {
            }
        }
        pScale = scale;
        return pScale;
    }


    private native boolean isPrinterDC(long hdc);

    private native void printBand(long hdc, byte[] data, int offset, int sx,
                                  int sy, int swidth, int sheight, int dx,
                                  int dy, int dwidth, int dheight);

    /**
     * Initialize JNI field IDs
     */
    private static native void initIDs();

    /**
     * This method is called from the native code when this embedded
     * frame should be activated. It is expected to be overridden in
     * subclasses, for example, in plugin to activate the browser
     * window that contains this embedded frame.
     *
     * NOTE: This method may be called by privileged threads.
     *     DO NOT INVOKE CLIENT CODE ON THIS THREAD!
     */
    public void activateEmbeddingTopLevel() {
    }

    public void synthesizeWindowActivation(final boolean activate) {
        final FramePeer peer = AWTAccessor.getComponentAccessor().getPeer(this);
        if (!activate || EventQueue.isDispatchThread()) {
            peer.emulateActivation(activate);
        } else {
            // To avoid focus concurrence b/w IE and EmbeddedFrame
            // activation is postponed by means of posting it to EDT.
            Runnable r = new Runnable() {
                public void run() {
                    peer.emulateActivation(true);
                }
            };
            WToolkit.postEvent(WToolkit.targetToAppContext(this),
                               new InvocationEvent(this, r));
        }
    }

    public void registerAccelerator(AWTKeyStroke stroke) {}
    public void unregisterAccelerator(AWTKeyStroke stroke) {}

    /**
     * Should be overridden in subclasses. Call to
     *     super.notifyModalBlocked(blocker, blocked) must be present
     *     when overriding.
     * It may occur that embedded frame is not put into its
     *     container at the moment when it is blocked, for example,
     *     when running an applet in IE. Then the call to this method
     *     should be delayed until embedded frame is reparented.
     *
     * NOTE: This method may be called by privileged threads.
     *     DO NOT INVOKE CLIENT CODE ON THIS THREAD!
     */
    public void notifyModalBlocked(Dialog blocker, boolean blocked) {
        try {
            ComponentPeer thisPeer = (ComponentPeer)WToolkit.targetToPeer(this);
            ComponentPeer blockerPeer = (ComponentPeer)WToolkit.targetToPeer(blocker);
            notifyModalBlockedImpl((WEmbeddedFramePeer)thisPeer,
                                   (WWindowPeer)blockerPeer, blocked);
        } catch (Exception z) {
            z.printStackTrace(System.err);
        }
    }
    native void notifyModalBlockedImpl(WEmbeddedFramePeer peer, WWindowPeer blockerPeer, boolean blocked);
}
