/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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


package sun.lwawt.macosx;

import java.awt.AWTKeyStroke;
import java.awt.Toolkit;
import java.lang.reflect.InvocationTargetException;

import sun.awt.AWTAccessor;
import sun.awt.EmbeddedFrame;
import sun.lwawt.LWWindowPeer;

/*
 * The CViewEmbeddedFrame class is used in the SWT_AWT bridge.
 * This is a part of public API and should not be renamed or moved
 */
@SuppressWarnings("serial") // JDK implementation class
public class CViewEmbeddedFrame extends EmbeddedFrame {

    private final long nsViewPtr;

    private boolean isActive = false;

    public CViewEmbeddedFrame(long nsViewPtr) {
        this.nsViewPtr = nsViewPtr;
    }

    @Override
    public void addNotify() {
        if (!isDisplayable()) {
            LWCToolkit toolkit = (LWCToolkit) Toolkit.getDefaultToolkit();
            setPeer(toolkit.createEmbeddedFrame(this));
        }
        super.addNotify();
    }

    public long getEmbedderHandle() {
        return nsViewPtr;
    }

    @Override
    public void registerAccelerator(AWTKeyStroke awtks) {
    }

    @Override
    public void unregisterAccelerator(AWTKeyStroke awtks) {
    }

    public boolean isParentWindowActive() {
        return isActive;
    }

    /*
     * Synthetic event delivery for focus management
     */
    @Override
    public void synthesizeWindowActivation(boolean activated) {
        if (isActive != activated) {
            isActive = activated;
            final LWWindowPeer peer = AWTAccessor.getComponentAccessor()
                                                 .getPeer(this);
            peer.notifyActivation(activated, null);
        }
    }

    /**
     * Initializes the embedded frame bounds and validates a component.
     * Designed to be called from the main thread. This method should be called
     * once from the initialization of the SWT_AWT Bridge.
     */
    public void validateWithBounds(final int x, final int y, final int width,
                                   final int height) {
        try {
            LWCToolkit.invokeAndWait(() -> {
                final LWWindowPeer peer = AWTAccessor.getComponentAccessor()
                                                     .getPeer(this);
                peer.setBoundsPrivate(0, 0, width, height);
                validate();
                setVisible(true);
            }, this);
        } catch (InvocationTargetException ex) {
        }
    }
}
