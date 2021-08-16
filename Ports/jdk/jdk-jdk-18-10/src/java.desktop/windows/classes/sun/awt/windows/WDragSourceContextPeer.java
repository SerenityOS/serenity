/*
 * Copyright (c) 1997, 2009, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import java.awt.datatransfer.Transferable;

import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.InvalidDnDOperationException;

import java.awt.event.InputEvent;

import java.util.Map;

import sun.awt.dnd.SunDragSourceContextPeer;

/**
 * <p>
 * TBC
 * </p>
 *
 * @since 1.2
 *
 */

final class WDragSourceContextPeer extends SunDragSourceContextPeer {
    public void startSecondaryEventLoop(){
        WToolkit.startSecondaryEventLoop();
    }
    public void quitSecondaryEventLoop(){
        WToolkit.quitSecondaryEventLoop();
    }

    private static final WDragSourceContextPeer theInstance =
        new WDragSourceContextPeer(null);

    /**
     * construct a new WDragSourceContextPeer. package private
     */

    private WDragSourceContextPeer(DragGestureEvent dge) {
        super(dge);
    }

    static WDragSourceContextPeer createDragSourceContextPeer(DragGestureEvent dge) throws InvalidDnDOperationException {
        theInstance.setTrigger(dge);
        return theInstance;
    }

    @Override
    protected void startDrag(Transferable trans,
                             long[] formats, Map<Long, DataFlavor> formatMap) {

        long nativeCtxtLocal = 0;

        nativeCtxtLocal = createDragSource(getTrigger().getComponent(),
                                           trans,
                                           getTrigger().getTriggerEvent(),
                                           getTrigger().getSourceAsDragGestureRecognizer().getSourceActions(),
                                           formats,
                                           formatMap);

        if (nativeCtxtLocal == 0) {
            throw new InvalidDnDOperationException("failed to create native peer");
        }

        int[] imageData = null;
        Point op = null;

        Image im = getDragImage();
        int imageWidth = -1;
        int imageHeight = -1;
        if (im != null) {
            //image is ready (partial images are ok)
            try{
                imageWidth = im.getWidth(null);
                imageHeight = im.getHeight(null);
                if (imageWidth < 0 || imageHeight < 0) {
                    throw new InvalidDnDOperationException("drag image is not ready");
                }
                //We could get an exception from user code here.
                //"im" and "dragImageOffset" are user-defined objects
                op = getDragImageOffset(); //op could not be null here
                BufferedImage bi = new BufferedImage(
                        imageWidth,
                        imageHeight,
                        BufferedImage.TYPE_INT_ARGB);
                bi.getGraphics().drawImage(im, 0, 0, null);

                //we can get out-of-memory here
                imageData = ((DataBufferInt)bi.getData().getDataBuffer()).getData();
            } catch (Throwable ex) {
                throw new InvalidDnDOperationException("drag image creation problem: " + ex.getMessage());
            }
        }

        //We shouldn't have user-level exceptions since now.
        //Any exception leads to corrupted D'n'D state.
        setNativeContext(nativeCtxtLocal);
        WDropTargetContextPeer.setCurrentJVMLocalSourceTransferable(trans);

        if (imageData != null) {
            doDragDrop(
                    getNativeContext(),
                    getCursor(),
                    imageData,
                    imageWidth, imageHeight,
                    op.x, op.y);
        } else {
            doDragDrop(
                    getNativeContext(),
                    getCursor(),
                    null,
                    -1, -1,
                    0, 0);
        }
    }

    /**
     * downcall into native code
     */

    native long createDragSource(Component component,
                                 Transferable transferable,
                                 InputEvent nativeTrigger,
                                 int actions,
                                 long[] formats,
                                 Map<Long, DataFlavor> formatMap);

    /**
     * downcall into native code
     */

    native void doDragDrop(
            long nativeCtxt,
            Cursor cursor,
            int[] imageData,
            int imgWidth, int imgHight,
            int offsetX, int offsetY);

    protected native void setNativeCursor(long nativeCtxt, Cursor c, int cType);

}
