/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.accessibility.*;

import java.util.Map;
import java.util.concurrent.Callable;

import sun.awt.AWTAccessor;
import sun.awt.dnd.*;
import sun.lwawt.LWComponentPeer;
import sun.lwawt.LWWindowPeer;
import sun.lwawt.PlatformWindow;


public final class CDragSourceContextPeer extends SunDragSourceContextPeer {

    private static final CDragSourceContextPeer fInstance = new CDragSourceContextPeer(null);

    private Image  fDragImage;
    private CImage fDragCImage;
    private Point  fDragImageOffset;

    private static Component hoveringComponent = null;

    private static double fMaxImageSize = 128.0;

    static {
        @SuppressWarnings("removal")
        String propValue = java.security.AccessController.doPrivileged(new sun.security.action.GetPropertyAction("apple.awt.dnd.defaultDragImageSize"));
        if (propValue != null) {
            try {
                double value = Double.parseDouble(propValue);
                if (value > 0) {
                    fMaxImageSize = value;
                }
            } catch(NumberFormatException e) {}
        }
    }

    private CDragSourceContextPeer(DragGestureEvent dge) {
        super(dge);
    }

    public static CDragSourceContextPeer createDragSourceContextPeer(DragGestureEvent dge) throws InvalidDnDOperationException {
        fInstance.setTrigger(dge);

        return fInstance;
    }

    // We have to overload this method just to be able to grab the drag image and its offset as shared code doesn't store it:
    public void startDrag(DragSourceContext dsc, Cursor cursor, Image dragImage, Point dragImageOffset) throws InvalidDnDOperationException {
        fDragImage = dragImage;
        fDragImageOffset = dragImageOffset;

        super.startDrag(dsc, cursor, dragImage, dragImageOffset);
    }

    protected void startDrag(Transferable transferable, long[] formats, Map<Long, DataFlavor> formatMap) {
        DragGestureEvent trigger = getTrigger();
        InputEvent         triggerEvent = trigger.getTriggerEvent();

        Point dragOrigin = new Point(trigger.getDragOrigin());
        @SuppressWarnings("deprecation")
        int extModifiers = (triggerEvent.getModifiers() | triggerEvent.getModifiersEx());
        long timestamp   = triggerEvent.getWhen();
        int clickCount   = ((triggerEvent instanceof MouseEvent) ? (((MouseEvent) triggerEvent).getClickCount()) : 1);

        Component component = trigger.getComponent();
        // For a lightweight component traverse up the hierarchy to the root
        Point loc = component.getLocation();
        Component rootComponent = component;
        while (!(rootComponent instanceof Window)) {
            dragOrigin.translate(loc.x, loc.y);
            rootComponent = rootComponent.getParent();
            loc = rootComponent.getLocation();
        }

        // If there isn't any drag image make one of default appearance:
        if (fDragImage == null)
            this.setDefaultDragImage(component);

        // Get drag image (if any) as BufferedImage and convert that to CImage:
        Point dragImageOffset;

        if (fDragImage != null) {
            try {
                fDragCImage = CImage.getCreator().createFromImageImmediately(fDragImage);
            } catch(Exception e) {
                // image creation may fail for any reason
                throw new InvalidDnDOperationException("Drag image can not be created.");
            }
            if (fDragCImage == null) {
                throw new InvalidDnDOperationException("Drag image is not ready.");
            }

            dragImageOffset = fDragImageOffset;
        } else {

            fDragCImage = null;
            dragImageOffset = new Point(0, 0);
        }

        try {
            //It sure will be LWComponentPeer instance as rootComponent is a Window
            LWComponentPeer<?, ?> peer = AWTAccessor.getComponentAccessor()
                                                    .getPeer(rootComponent);
            PlatformWindow platformWindow = peer.getPlatformWindow();
            long nativeViewPtr = CPlatformWindow.getNativeViewPtr(platformWindow);
            if (nativeViewPtr == 0L) throw new InvalidDnDOperationException("Unsupported platform window implementation");

            // Create native dragging source:
            final long nativeDragSource = createNativeDragSource(component, nativeViewPtr, transferable, triggerEvent,
                (int) (dragOrigin.getX()), (int) (dragOrigin.getY()), extModifiers,
                clickCount, timestamp, fDragCImage != null ? fDragCImage.ptr : 0L, dragImageOffset.x, dragImageOffset.y,
                getDragSourceContext().getSourceActions(), formats, formatMap);

            if (nativeDragSource == 0)
                throw new InvalidDnDOperationException("");

            setNativeContext(nativeDragSource);
        }

        catch (Exception e) {
            throw new InvalidDnDOperationException("failed to create native peer: " + e);
        }

        SunDropTargetContextPeer.setCurrentJVMLocalSourceTransferable(transferable);

        CCursorManager.getInstance().setCursor(getCursor());

        // Create a new thread to run the dragging operation since it's synchronous, only coming back
        // after dragging is finished. This leaves the AWT event thread free to handle AWT events which
        // are posted during dragging by native event handlers.

        try {
            Runnable dragRunnable = () -> {
                final long nativeDragSource = getNativeContext();
                try {
                    doDragging(nativeDragSource);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    releaseNativeDragSource(nativeDragSource);
                    fDragImage = null;
                    if (fDragCImage != null) {
                        fDragCImage.dispose();
                        fDragCImage = null;
                    }
                }
            };
            new Thread(null, dragRunnable, "Drag", 0, false).start();
        } catch (Exception e) {
            final long nativeDragSource = getNativeContext();
            setNativeContext(0);
            releaseNativeDragSource(nativeDragSource);
            SunDropTargetContextPeer.setCurrentJVMLocalSourceTransferable(null);
            throw new InvalidDnDOperationException("failed to start dragging thread: " + e);
        }
    }

    private void setDefaultDragImage(Component component) {
        boolean handled = false;

        // Special-case default drag image, depending on the drag source type:
        if (component.isLightweight()) {
            if (component instanceof JTextComponent) {
                this.setDefaultDragImage((JTextComponent) component);
                handled = true;
            } else if (component instanceof JTree) {
                            this.setDefaultDragImage((JTree) component);
                            handled = true;
                        } else if (component instanceof JTable) {
                            this.setDefaultDragImage((JTable) component);
                            handled = true;
                        } else if (component instanceof JList) {
                            this.setDefaultDragImage((JList) component);
                            handled = true;
                        }
        }

        if (handled == false)
            this.setDefaultDragImage();
    }

    @SuppressWarnings("deprecation")
    private void setDefaultDragImage(JTextComponent component) {
        DragGestureEvent trigger = getTrigger();
        int selectionStart = component.getSelectionStart();
        int selectionEnd = component.getSelectionEnd();
        boolean handled = false;

        // Make sure we're dragging current selection:
        int index = component.viewToModel(trigger.getDragOrigin());
        if ((selectionStart < selectionEnd) && (index >= selectionStart) && (index <= selectionEnd)) {
            try {
                Rectangle selectionStartBounds = component.modelToView(selectionStart);
                Rectangle selectionEndBounds = component.modelToView(selectionEnd);

                Rectangle selectionBounds = null;

                // Single-line selection:
                if (selectionStartBounds.y == selectionEndBounds.y) {
                    selectionBounds = new Rectangle(selectionStartBounds.x, selectionStartBounds.y,
                        selectionEndBounds.x - selectionStartBounds.x + selectionEndBounds.width,
                        selectionEndBounds.y - selectionStartBounds.y + selectionEndBounds.height);
                }

                // Multi-line selection:
                else {
                    AccessibleContext ctx = component.getAccessibleContext();
                    AccessibleText at = (AccessibleText) ctx;

                    selectionBounds = component.modelToView(selectionStart);
                    for (int i = selectionStart + 1; i <= selectionEnd; i++) {
                                            Rectangle charBounds = at.getCharacterBounds(i);
                                            // Invalid index returns null Rectangle
                                            // Note that this goes against jdk doc - should be empty, but is null instead
                                            if (charBounds != null) {
                                                selectionBounds.add(charBounds);
                                            }
                    }
                }

                this.setOutlineDragImage(selectionBounds);
                handled = true;
            }

            catch (BadLocationException exc) {
                // Default the drag image to component bounds.
            }
        }

        if (handled == false)
            this.setDefaultDragImage();
    }


    private void setDefaultDragImage(JTree component) {
        Rectangle selectedOutline = null;

        int[] selectedRows = component.getSelectionRows();
        for (int i=0; i<selectedRows.length; i++) {
            Rectangle r = component.getRowBounds(selectedRows[i]);
            if (selectedOutline == null)
                selectedOutline = r;
            else
                selectedOutline.add(r);
        }

        if (selectedOutline != null) {
            this.setOutlineDragImage(selectedOutline);
        } else {
            this.setDefaultDragImage();
        }
    }

    private void setDefaultDragImage(JTable component) {
        Rectangle selectedOutline = null;

        // This code will likely break once multiple selections works (3645873)
        int[] selectedRows = component.getSelectedRows();
        int[] selectedColumns = component.getSelectedColumns();
        for (int row=0; row<selectedRows.length; row++) {
            for (int col=0; col<selectedColumns.length; col++) {
                Rectangle r = component.getCellRect(selectedRows[row], selectedColumns[col], true);
                if (selectedOutline == null)
                    selectedOutline = r;
                else
                    selectedOutline.add(r);
            }
        }

        if (selectedOutline != null) {
            this.setOutlineDragImage(selectedOutline);
        } else {
            this.setDefaultDragImage();
        }
    }

    private void setDefaultDragImage(JList<?> component) {
        Rectangle selectedOutline = null;

        // This code actually works, even under the (non-existant) multiple-selections, because we only draw a union outline
        int[] selectedIndices = component.getSelectedIndices();
        if (selectedIndices.length > 0)
            selectedOutline = component.getCellBounds(selectedIndices[0], selectedIndices[selectedIndices.length-1]);

        if (selectedOutline != null) {
            this.setOutlineDragImage(selectedOutline);
        } else {
            this.setDefaultDragImage();
        }
    }


    private void setDefaultDragImage() {
        DragGestureEvent trigger = this.getTrigger();
        Component comp = trigger.getComponent();

        setOutlineDragImage(new Rectangle(0, 0, comp.getWidth(), comp.getHeight()), true);
    }

    private void setOutlineDragImage(Rectangle outline) {
        setOutlineDragImage(outline, false);
    }

    private void setOutlineDragImage(Rectangle outline, Boolean shouldScale) {
        int width = (int)outline.getWidth();
        int height = (int)outline.getHeight();

        double scale = 1.0;
        if (shouldScale) {
            final int area = width * height;
            final int maxArea = (int)(fMaxImageSize * fMaxImageSize);

            if (area > maxArea) {
                scale = (double)area / (double)maxArea;
                width /= scale;
                height /= scale;
            }
        }

        if (width <=0) width = 1;
        if (height <=0) height = 1;

        DragGestureEvent trigger = this.getTrigger();
        Component comp = trigger.getComponent();
        Point compOffset = comp.getLocation();

        // For lightweight components add some special treatment:
        if (comp instanceof JComponent) {
            // Intersect requested bounds with visible bounds:
            Rectangle visibleBounds = ((JComponent) comp).getVisibleRect();
            Rectangle clipedOutline = outline.intersection(visibleBounds);
            if (clipedOutline.isEmpty() == false)
                outline = clipedOutline;

            // Compensate for the component offset (e.g. when contained in a JScrollPane):
            outline.translate(compOffset.x, compOffset.y);
        }

        GraphicsConfiguration config = comp.getGraphicsConfiguration();
        BufferedImage dragImage = config.createCompatibleImage(width, height, Transparency.TRANSLUCENT);

        Color paint = Color.gray;
        BasicStroke stroke = new BasicStroke(2.0f);
        int halfLineWidth = (int) (stroke.getLineWidth() + 1) / 2; // Rounded up.

        Graphics2D g2 = (Graphics2D) dragImage.getGraphics();
        g2.setPaint(paint);
        g2.setStroke(stroke);
        g2.drawRect(halfLineWidth, halfLineWidth, width - 2 * halfLineWidth - 1, height - 2 * halfLineWidth - 1);
        g2.dispose();

        fDragImage = dragImage;


        Point dragOrigin = trigger.getDragOrigin();
        Point dragImageOffset = new Point(outline.x - dragOrigin.x, outline.y - dragOrigin.y);
        if (comp instanceof JComponent) {
            dragImageOffset.translate(-compOffset.x, -compOffset.y);
        }

        if (shouldScale) {
            dragImageOffset.x /= scale;
            dragImageOffset.y /= scale;
        }

        fDragImageOffset = dragImageOffset;
    }

    /**
     * upcall from native code
     */
    private void dragMouseMoved(final int targetActions,
                                final int modifiers,
                                final int x, final int y) {

        try {
            Component componentAt = LWCToolkit.invokeAndWait(
                    new Callable<Component>() {
                        @Override
                        public Component call() {
                            LWWindowPeer mouseEventComponent = LWWindowPeer.getWindowUnderCursor();
                            if (mouseEventComponent == null) {
                                return null;
                            }
                            Component root = SwingUtilities.getRoot(mouseEventComponent.getTarget());
                            if (root == null) {
                                return null;
                            }
                            Point rootLocation = root.getLocationOnScreen();
                            return getDropTargetAt(root, x - rootLocation.x, y - rootLocation.y);
                        }
                    }, getComponent());

            if(componentAt != hoveringComponent) {
                if(hoveringComponent != null) {
                    dragExit(x, y);
                }
                if(componentAt != null) {
                    dragEnter(targetActions, modifiers, x, y);
                }
                hoveringComponent = componentAt;
            }

            postDragSourceDragEvent(targetActions, modifiers, x, y,
                    DISPATCH_MOUSE_MOVED);
        } catch (Exception e) {
            throw new InvalidDnDOperationException("Failed to handle DragMouseMoved event");
        }
    }

    //Returns the first lightweight or heavyweight Component which has a dropTarget ready to accept the drag
    //Should be called from the EventDispatchThread
    private static Component getDropTargetAt(Component root, int x, int y) {
        if (!root.contains(x, y) || !root.isEnabled() || !root.isVisible()) {
            return null;
        }

        if (root.getDropTarget() != null && root.getDropTarget().isActive()) {
            return root;
        }

        if (root instanceof Container) {
            for (Component comp : ((Container) root).getComponents()) {
                Point loc = comp.getLocation();
                Component dropTarget = getDropTargetAt(comp, x - loc.x, y - loc.y);
                if (dropTarget != null) {
                    return dropTarget;
                }
            }
        }

        return null;
    }

    /**
     * upcall from native code - reset hovering component
     */
    private void resetHovering() {
        hoveringComponent = null;
    }

    @Override
    protected void setNativeCursor(long nativeCtxt, Cursor c, int cType) {
        CCursorManager.getInstance().setCursor(c);
    }

    // Native support:
    private native long createNativeDragSource(Component component, long nativePeer, Transferable transferable,
        InputEvent triggerEvent, int dragPosX, int dragPosY, int extModifiers, int clickCount, long timestamp,
        long nsDragImagePtr, int dragImageOffsetX, int dragImageOffsetY,
        int sourceActions, long[] formats, Map<Long, DataFlavor> formatMap);

    private native void doDragging(long nativeDragSource);

    private native void releaseNativeDragSource(long nativeDragSource);
}
