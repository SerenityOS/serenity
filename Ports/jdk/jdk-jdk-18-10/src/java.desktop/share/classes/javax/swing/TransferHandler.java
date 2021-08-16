/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing;

import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.beans.*;
import java.lang.reflect.*;
import java.io.*;
import java.util.TooManyListenersException;
import javax.swing.plaf.UIResource;
import javax.swing.event.*;
import javax.swing.text.JTextComponent;

import sun.reflect.misc.MethodUtil;
import sun.swing.SwingUtilities2;
import sun.awt.AppContext;
import sun.swing.*;
import sun.awt.SunToolkit;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.security.AccessControlContext;

import jdk.internal.access.SharedSecrets;
import jdk.internal.access.JavaSecurityAccess;

import sun.awt.AWTAccessor;

/**
 * This class is used to handle the transfer of a <code>Transferable</code>
 * to and from Swing components.  The <code>Transferable</code> is used to
 * represent data that is exchanged via a cut, copy, or paste
 * to/from a clipboard.  It is also used in drag-and-drop operations
 * to represent a drag from a component, and a drop to a component.
 * Swing provides functionality that automatically supports cut, copy,
 * and paste keyboard bindings that use the functionality provided by
 * an implementation of this class.  Swing also provides functionality
 * that automatically supports drag and drop that uses the functionality
 * provided by an implementation of this class.  The Swing developer can
 * concentrate on specifying the semantics of a transfer primarily by setting
 * the <code>transferHandler</code> property on a Swing component.
 * <p>
 * This class is implemented to provide a default behavior of transferring
 * a component property simply by specifying the name of the property in
 * the constructor.  For example, to transfer the foreground color from
 * one component to another either via the clipboard or a drag and drop operation
 * a <code>TransferHandler</code> can be constructed with the string "foreground".  The
 * built in support will use the color returned by <code>getForeground</code> as the source
 * of the transfer, and <code>setForeground</code> for the target of a transfer.
 * <p>
 * Please see
 * <a href="https://docs.oracle.com/javase/tutorial/uiswing/dnd/index.html">
 * How to Use Drag and Drop and Data Transfer</a>,
 * a section in <em>The Java Tutorial</em>, for more information.
 *
 *
 * @author Timothy Prinzing
 * @author Shannon Hickey
 * @since 1.4
 */
@SuppressWarnings("serial")
public class TransferHandler implements Serializable {

    /**
     * An <code>int</code> representing no transfer action.
     */
    public static final int NONE = DnDConstants.ACTION_NONE;

    /**
     * An <code>int</code> representing a &quot;copy&quot; transfer action.
     * This value is used when data is copied to a clipboard
     * or copied elsewhere in a drag and drop operation.
     */
    public static final int COPY = DnDConstants.ACTION_COPY;

    /**
     * An <code>int</code> representing a &quot;move&quot; transfer action.
     * This value is used when data is moved to a clipboard (i.e. a cut)
     * or moved elsewhere in a drag and drop operation.
     */
    public static final int MOVE = DnDConstants.ACTION_MOVE;

    /**
     * An <code>int</code> representing a source action capability of either
     * &quot;copy&quot; or &quot;move&quot;.
     */
    public static final int COPY_OR_MOVE = DnDConstants.ACTION_COPY_OR_MOVE;

    /**
     * An <code>int</code> representing a &quot;link&quot; transfer action.
     * This value is used to specify that data should be linked in a drag
     * and drop operation.
     *
     * @see java.awt.dnd.DnDConstants#ACTION_LINK
     * @since 1.6
     */
    public static final int LINK = DnDConstants.ACTION_LINK;

    /**
     * An interface to tag things with a {@code getTransferHandler} method.
     */
    interface HasGetTransferHandler {

        /** Returns the {@code TransferHandler}.
         *
         * @return The {@code TransferHandler} or {@code null}
         */
        public TransferHandler getTransferHandler();
    }

    /**
     * Represents a location where dropped data should be inserted.
     * This is a base class that only encapsulates a point.
     * Components supporting drop may provide subclasses of this
     * containing more information.
     * <p>
     * Developers typically shouldn't create instances of, or extend, this
     * class. Instead, these are something provided by the DnD
     * implementation by <code>TransferSupport</code> instances and by
     * components with a <code>getDropLocation()</code> method.
     *
     * @see javax.swing.TransferHandler.TransferSupport#getDropLocation
     * @since 1.6
     */
    public static class DropLocation {
        private final Point dropPoint;

        /**
         * Constructs a drop location for the given point.
         *
         * @param dropPoint the drop point, representing the mouse's
         *        current location within the component.
         * @throws IllegalArgumentException if the point
         *         is <code>null</code>
         */
        protected DropLocation(Point dropPoint) {
            if (dropPoint == null) {
                throw new IllegalArgumentException("Point cannot be null");
            }

            this.dropPoint = new Point(dropPoint);
        }

        /**
         * Returns the drop point, representing the mouse's
         * current location within the component.
         *
         * @return the drop point.
         */
        public final Point getDropPoint() {
            return new Point(dropPoint);
        }

        /**
         * Returns a string representation of this drop location.
         * This method is intended to be used for debugging purposes,
         * and the content and format of the returned string may vary
         * between implementations.
         *
         * @return a string representation of this drop location
         */
        public String toString() {
            return getClass().getName() + "[dropPoint=" + dropPoint + "]";
        }
    };

    /**
     * This class encapsulates all relevant details of a clipboard
     * or drag and drop transfer, and also allows for customizing
     * aspects of the drag and drop experience.
     * <p>
     * The main purpose of this class is to provide the information
     * needed by a developer to determine the suitability of a
     * transfer or to import the data contained within. But it also
     * doubles as a controller for customizing properties during drag
     * and drop, such as whether or not to show the drop location,
     * and which drop action to use.
     * <p>
     * Developers typically need not create instances of this
     * class. Instead, they are something provided by the DnD
     * implementation to certain methods in <code>TransferHandler</code>.
     *
     * @see #canImport(TransferHandler.TransferSupport)
     * @see #importData(TransferHandler.TransferSupport)
     * @since 1.6
     */
    public static final class TransferSupport {
        private boolean isDrop;
        private Component component;

        private boolean showDropLocationIsSet;
        private boolean showDropLocation;

        private int dropAction = -1;

        /**
         * The source is a {@code DropTargetDragEvent} or
         * {@code DropTargetDropEvent} for drops,
         * and a {@code Transferable} otherwise
         */
        private Object source;

        private DropLocation dropLocation;

        /**
         * Create a <code>TransferSupport</code> with <code>isDrop()</code>
         * <code>true</code> for the given component, event, and index.
         *
         * @param component the target component
         * @param event a <code>DropTargetEvent</code>
         */
        private TransferSupport(Component component,
                             DropTargetEvent event) {

            isDrop = true;
            setDNDVariables(component, event);
        }

        /**
         * Create a <code>TransferSupport</code> with <code>isDrop()</code>
         * <code>false</code> for the given component and
         * <code>Transferable</code>.
         *
         * @param component the target component
         * @param transferable the transferable
         * @throws NullPointerException if either parameter
         *         is <code>null</code>
         */
        public TransferSupport(Component component, Transferable transferable) {
            if (component == null) {
                throw new NullPointerException("component is null");
            }

            if (transferable == null) {
                throw new NullPointerException("transferable is null");
            }

            isDrop = false;
            this.component = component;
            this.source = transferable;
        }

        /**
         * Allows for a single instance to be reused during DnD.
         *
         * @param component the target component
         * @param event a <code>DropTargetEvent</code>
         */
        private void setDNDVariables(Component component,
                                     DropTargetEvent event) {

            assert isDrop;

            this.component = component;
            this.source = event;
            dropLocation = null;
            dropAction = -1;
            showDropLocationIsSet = false;

            if (source == null) {
                return;
            }

            assert source instanceof DropTargetDragEvent ||
                   source instanceof DropTargetDropEvent;

            Point p = source instanceof DropTargetDragEvent
                          ? ((DropTargetDragEvent)source).getLocation()
                          : ((DropTargetDropEvent)source).getLocation();

            if (SunToolkit.isInstanceOf(component, "javax.swing.text.JTextComponent")) {
                dropLocation = SwingAccessor.getJTextComponentAccessor().
                                   dropLocationForPoint((JTextComponent)component, p);
            } else if (component instanceof JComponent) {
                dropLocation = ((JComponent)component).dropLocationForPoint(p);
            }

            /*
             * The drop location may be null at this point if the component
             * doesn't return custom drop locations. In this case, a point-only
             * drop location will be created lazily when requested.
             */
        }

        /**
         * Returns whether or not this <code>TransferSupport</code>
         * represents a drop operation.
         *
         * @return <code>true</code> if this is a drop operation,
         *         <code>false</code> otherwise.
         */
        public boolean isDrop() {
            return isDrop;
        }

        /**
         * Returns the target component of this transfer.
         *
         * @return the target component
         */
        public Component getComponent() {
            return component;
        }

        /**
         * Checks that this is a drop and throws an
         * {@code IllegalStateException} if it isn't.
         *
         * @throws IllegalStateException if {@code isDrop} is false.
         */
        private void assureIsDrop() {
            if (!isDrop) {
                throw new IllegalStateException("Not a drop");
            }
        }

        /**
         * Returns the current (non-{@code null}) drop location for the component,
         * when this {@code TransferSupport} represents a drop.
         * <p>
         * Note: For components with built-in drop support, this location
         * will be a subclass of {@code DropLocation} of the same type
         * returned by that component's {@code getDropLocation} method.
         * <p>
         * This method is only for use with drag and drop transfers.
         * Calling it when {@code isDrop()} is {@code false} results
         * in an {@code IllegalStateException}.
         *
         * @return the drop location
         * @throws IllegalStateException if this is not a drop
         * @see #isDrop()
         */
        public DropLocation getDropLocation() {
            assureIsDrop();

            if (dropLocation == null) {
                /*
                 * component didn't give us a custom drop location,
                 * so lazily create a point-only location
                 */
                Point p = source instanceof DropTargetDragEvent
                              ? ((DropTargetDragEvent)source).getLocation()
                              : ((DropTargetDropEvent)source).getLocation();

                dropLocation = new DropLocation(p);
            }

            return dropLocation;
        }

        /**
         * Sets whether or not the drop location should be visually indicated
         * for the transfer - which must represent a drop. This is applicable to
         * those components that automatically
         * show the drop location when appropriate during a drag and drop
         * operation. By default, the drop location is shown only when the
         * {@code TransferHandler} has said it can accept the import represented
         * by this {@code TransferSupport}. With this method you can force the
         * drop location to always be shown, or always not be shown.
         * <p>
         * This method is only for use with drag and drop transfers.
         * Calling it when {@code isDrop()} is {@code false} results
         * in an {@code IllegalStateException}.
         *
         * @param showDropLocation whether or not to indicate the drop location
         * @throws IllegalStateException if this is not a drop
         * @see #isDrop()
         */
        public void setShowDropLocation(boolean showDropLocation) {
            assureIsDrop();

            this.showDropLocation = showDropLocation;
            this.showDropLocationIsSet = true;
        }

        /**
         * Sets the drop action for the transfer - which must represent a drop
         * - to the given action,
         * instead of the default user drop action. The action must be
         * supported by the source's drop actions, and must be one
         * of {@code COPY}, {@code MOVE} or {@code LINK}.
         * <p>
         * This method is only for use with drag and drop transfers.
         * Calling it when {@code isDrop()} is {@code false} results
         * in an {@code IllegalStateException}.
         *
         * @param dropAction the drop action
         * @throws IllegalStateException if this is not a drop
         * @throws IllegalArgumentException if an invalid action is specified
         * @see #getDropAction
         * @see #getUserDropAction
         * @see #getSourceDropActions
         * @see #isDrop()
         */
        public void setDropAction(int dropAction) {
            assureIsDrop();

            int action = dropAction & getSourceDropActions();

            if (!(action == COPY || action == MOVE || action == LINK)) {
                throw new IllegalArgumentException("unsupported drop action: " + dropAction);
            }

            this.dropAction = dropAction;
        }

        /**
         * Returns the action chosen for the drop, when this
         * {@code TransferSupport} represents a drop.
         * <p>
         * Unless explicitly chosen by way of {@code setDropAction},
         * this returns the user drop action provided by
         * {@code getUserDropAction}.
         * <p>
         * You may wish to query this in {@code TransferHandler}'s
         * {@code importData} method to customize processing based
         * on the action.
         * <p>
         * This method is only for use with drag and drop transfers.
         * Calling it when {@code isDrop()} is {@code false} results
         * in an {@code IllegalStateException}.
         *
         * @return the action chosen for the drop
         * @throws IllegalStateException if this is not a drop
         * @see #setDropAction
         * @see #getUserDropAction
         * @see #isDrop()
         */
        public int getDropAction() {
            return dropAction == -1 ? getUserDropAction() : dropAction;
        }

        /**
         * Returns the user drop action for the drop, when this
         * {@code TransferSupport} represents a drop.
         * <p>
         * The user drop action is chosen for a drop as described in the
         * documentation for {@link java.awt.dnd.DropTargetDragEvent} and
         * {@link java.awt.dnd.DropTargetDropEvent}. A different action
         * may be chosen as the drop action by way of the {@code setDropAction}
         * method.
         * <p>
         * You may wish to query this in {@code TransferHandler}'s
         * {@code canImport} method when determining the suitability of a
         * drop or when deciding on a drop action to explicitly choose.
         * <p>
         * This method is only for use with drag and drop transfers.
         * Calling it when {@code isDrop()} is {@code false} results
         * in an {@code IllegalStateException}.
         *
         * @return the user drop action
         * @throws IllegalStateException if this is not a drop
         * @see #setDropAction
         * @see #getDropAction
         * @see #isDrop()
         */
        public int getUserDropAction() {
            assureIsDrop();

            return (source instanceof DropTargetDragEvent)
                ? ((DropTargetDragEvent)source).getDropAction()
                : ((DropTargetDropEvent)source).getDropAction();
        }

        /**
         * Returns the drag source's supported drop actions, when this
         * {@code TransferSupport} represents a drop.
         * <p>
         * The source actions represent the set of actions supported by the
         * source of this transfer, and are represented as some bitwise-OR
         * combination of {@code COPY}, {@code MOVE} and {@code LINK}.
         * You may wish to query this in {@code TransferHandler}'s
         * {@code canImport} method when determining the suitability of a drop
         * or when deciding on a drop action to explicitly choose. To determine
         * if a particular action is supported by the source, bitwise-AND
         * the action with the source drop actions, and then compare the result
         * against the original action. For example:
         * <pre>
         * boolean copySupported = (COPY &amp; getSourceDropActions()) == COPY;
         * </pre>
         * <p>
         * This method is only for use with drag and drop transfers.
         * Calling it when {@code isDrop()} is {@code false} results
         * in an {@code IllegalStateException}.
         *
         * @return the drag source's supported drop actions
         * @throws IllegalStateException if this is not a drop
         * @see #isDrop()
         */
        public int getSourceDropActions() {
            assureIsDrop();

            return (source instanceof DropTargetDragEvent)
                ? ((DropTargetDragEvent)source).getSourceActions()
                : ((DropTargetDropEvent)source).getSourceActions();
        }

        /**
         * Returns the data flavors for this transfer.
         *
         * @return the data flavors for this transfer
         */
        public DataFlavor[] getDataFlavors() {
            if (isDrop) {
                if (source instanceof DropTargetDragEvent) {
                    return ((DropTargetDragEvent)source).getCurrentDataFlavors();
                } else {
                    return ((DropTargetDropEvent)source).getCurrentDataFlavors();
                }
            }

            return ((Transferable)source).getTransferDataFlavors();
        }

        /**
         * Returns whether or not the given data flavor is supported.
         *
         * @param df the <code>DataFlavor</code> to test
         * @return whether or not the given flavor is supported.
         */
        public boolean isDataFlavorSupported(DataFlavor df) {
            if (isDrop) {
                if (source instanceof DropTargetDragEvent) {
                    return ((DropTargetDragEvent)source).isDataFlavorSupported(df);
                } else {
                    return ((DropTargetDropEvent)source).isDataFlavorSupported(df);
                }
            }

            return ((Transferable)source).isDataFlavorSupported(df);
        }

        /**
         * Returns the <code>Transferable</code> associated with this transfer.
         * <p>
         * Note: Unless it is necessary to fetch the <code>Transferable</code>
         * directly, use one of the other methods on this class to inquire about
         * the transfer. This may perform better than fetching the
         * <code>Transferable</code> and asking it directly.
         *
         * @return the <code>Transferable</code> associated with this transfer
         */
        public Transferable getTransferable() {
            if (isDrop) {
                if (source instanceof DropTargetDragEvent) {
                    return ((DropTargetDragEvent)source).getTransferable();
                } else {
                    return ((DropTargetDropEvent)source).getTransferable();
                }
            }

            return (Transferable)source;
        }
    }


    /**
     * Returns an {@code Action} that performs cut operations to the
     * clipboard. When performed, this action operates on the {@code JComponent}
     * source of the {@code ActionEvent} by invoking {@code exportToClipboard},
     * with a {@code MOVE} action, on the component's {@code TransferHandler}.
     *
     * @return an {@code Action} for performing cuts to the clipboard
     */
    public static Action getCutAction() {
        return cutAction;
    }

    /**
     * Returns an {@code Action} that performs copy operations to the
     * clipboard. When performed, this action operates on the {@code JComponent}
     * source of the {@code ActionEvent} by invoking {@code exportToClipboard},
     * with a {@code COPY} action, on the component's {@code TransferHandler}.
     *
     * @return an {@code Action} for performing copies to the clipboard
     */
    public static Action getCopyAction() {
        return copyAction;
    }

    /**
     * Returns an {@code Action} that performs paste operations from the
     * clipboard. When performed, this action operates on the {@code JComponent}
     * source of the {@code ActionEvent} by invoking {@code importData},
     * with the clipboard contents, on the component's {@code TransferHandler}.
     *
     * @return an {@code Action} for performing pastes from the clipboard
     */
    public static Action getPasteAction() {
        return pasteAction;
    }


    /**
     * Constructs a transfer handler that can transfer a Java Bean property
     * from one component to another via the clipboard or a drag and drop
     * operation.
     *
     * @param property  the name of the property to transfer; this can
     *  be <code>null</code> if there is no property associated with the transfer
     *  handler (a subclass that performs some other kind of transfer, for example)
     */
    public TransferHandler(String property) {
        propertyName = property;
    }

    /**
     * Convenience constructor for subclasses.
     */
    protected TransferHandler() {
        this(null);
    }


    /**
     * image for the {@code startDrag} method
     *
     * @see java.awt.dnd.DragGestureEvent#startDrag(Cursor dragCursor, Image dragImage, Point imageOffset, Transferable transferable, DragSourceListener dsl)
     */
    private  Image dragImage;

    /**
     * anchor offset for the {@code startDrag} method
     *
     * @see java.awt.dnd.DragGestureEvent#startDrag(Cursor dragCursor, Image dragImage, Point imageOffset, Transferable transferable, DragSourceListener dsl)
     */
    private  Point dragImageOffset;

    /**
     * Sets the drag image parameter. The image has to be prepared
     * for rendering by the moment of the call. The image is stored
     * by reference because of some performance reasons.
     *
     * @param img an image to drag
     */
    public void setDragImage(Image img) {
        dragImage = img;
    }

    /**
     * Returns the drag image. If there is no image to drag,
     * the returned value is {@code null}.
     *
     * @return the reference to the drag image
     */
    public Image getDragImage() {
        return dragImage;
    }

    /**
     * Sets an anchor offset for the image to drag.
     * It can not be {@code null}.
     *
     * @param p a {@code Point} object that corresponds
     * to coordinates of an anchor offset of the image
     * relative to the upper left corner of the image
     */
    public void setDragImageOffset(Point p) {
        dragImageOffset = new Point(p);
    }

    /**
     * Returns an anchor offset for the image to drag.
     *
     * @return a {@code Point} object that corresponds
     * to coordinates of an anchor offset of the image
     * relative to the upper left corner of the image.
     * The point {@code (0,0)} returns by default.
     */
    public Point getDragImageOffset() {
        if (dragImageOffset == null) {
            return new Point(0,0);
        }
        return new Point(dragImageOffset);
    }

    /**
     * Causes the Swing drag support to be initiated.  This is called by
     * the various UI implementations in the <code>javax.swing.plaf.basic</code>
     * package if the dragEnabled property is set on the component.
     * This can be called by custom UI
     * implementations to use the Swing drag support.  This method can also be called
     * by a Swing extension written as a subclass of <code>JComponent</code>
     * to take advantage of the Swing drag support.
     * <p>
     * The transfer <em>will not necessarily</em> have been completed at the
     * return of this call (i.e. the call does not block waiting for the drop).
     * The transfer will take place through the Swing implementation of the
     * <code>java.awt.dnd</code> mechanism, requiring no further effort
     * from the developer. The <code>exportDone</code> method will be called
     * when the transfer has completed.
     *
     * @param comp  the component holding the data to be transferred;
     *              provided to enable sharing of <code>TransferHandler</code>s
     * @param e     the event that triggered the transfer
     * @param action the transfer action initially requested;
     *               either {@code COPY}, {@code MOVE} or {@code LINK};
     *               the DnD system may change the action used during the
     *               course of the drag operation
     */
    public void exportAsDrag(JComponent comp, InputEvent e, int action) {
        int srcActions = getSourceActions(comp);

        // only mouse events supported for drag operations
        if (!(e instanceof MouseEvent)
                // only support known actions
                || !(action == COPY || action == MOVE || action == LINK)
                // only support valid source actions
                || (srcActions & action) == 0) {

            action = NONE;
        }

        if (action != NONE && !GraphicsEnvironment.isHeadless()) {
            if (recognizer == null) {
                recognizer = new SwingDragGestureRecognizer(new DragHandler());
            }
            recognizer.gestured(comp, (MouseEvent)e, srcActions, action);
        } else {
            exportDone(comp, null, NONE);
        }
    }

    /**
     * Causes a transfer from the given component to the
     * given clipboard.  This method is called by the default cut and
     * copy actions registered in a component's action map.
     * <p>
     * The transfer will take place using the <code>java.awt.datatransfer</code>
     * mechanism, requiring no further effort from the developer. Any data
     * transfer <em>will</em> be complete and the <code>exportDone</code>
     * method will be called with the action that occurred, before this method
     * returns. Should the clipboard be unavailable when attempting to place
     * data on it, the <code>IllegalStateException</code> thrown by
     * {@link Clipboard#setContents(Transferable, ClipboardOwner)} will
     * be propagated through this method. However,
     * <code>exportDone</code> will first be called with an action
     * of <code>NONE</code> for consistency.
     *
     * @param comp  the component holding the data to be transferred;
     *              provided to enable sharing of <code>TransferHandler</code>s
     * @param clip  the clipboard to transfer the data into
     * @param action the transfer action requested; this should
     *  be a value of either <code>COPY</code> or <code>MOVE</code>;
     *  the operation performed is the intersection  of the transfer
     *  capabilities given by getSourceActions and the requested action;
     *  the intersection may result in an action of <code>NONE</code>
     *  if the requested action isn't supported
     * @throws IllegalStateException if the clipboard is currently unavailable
     * @see Clipboard#setContents(Transferable, ClipboardOwner)
     */
    public void exportToClipboard(JComponent comp, Clipboard clip, int action)
                                                  throws IllegalStateException {

        if ((action == COPY || action == MOVE)
                && (getSourceActions(comp) & action) != 0) {

            Transferable t = createTransferable(comp);
            if (t != null) {
                try {
                    clip.setContents(t, null);
                    exportDone(comp, t, action);
                    return;
                } catch (IllegalStateException ise) {
                    exportDone(comp, t, NONE);
                    throw ise;
                }
            }
        }

        exportDone(comp, null, NONE);
    }

    /**
     * Causes a transfer to occur from a clipboard or a drag and
     * drop operation. The <code>Transferable</code> to be
     * imported and the component to transfer to are contained
     * within the <code>TransferSupport</code>.
     * <p>
     * While the drag and drop implementation calls {@code canImport}
     * to determine the suitability of a transfer before calling this
     * method, the implementation of paste does not. As such, it cannot
     * be assumed that the transfer is acceptable upon a call to
     * this method for paste. It is recommended that {@code canImport} be
     * explicitly called to cover this case.
     * <p>
     * Note: The <code>TransferSupport</code> object passed to this method
     * is only valid for the duration of the method call. It is undefined
     * what values it may contain after this method returns.
     *
     * @param support the object containing the details of
     *        the transfer, not <code>null</code>.
     * @return true if the data was inserted into the component,
     *         false otherwise
     * @throws NullPointerException if <code>support</code> is {@code null}
     * @see #canImport(TransferHandler.TransferSupport)
     * @since 1.6
     */
    public boolean importData(TransferSupport support) {
        return support.getComponent() instanceof JComponent
            ? importData((JComponent)support.getComponent(), support.getTransferable())
            : false;
    }

    /**
     * Causes a transfer to a component from a clipboard or a
     * DND drop operation.  The <code>Transferable</code> represents
     * the data to be imported into the component.
     * <p>
     * Note: Swing now calls the newer version of <code>importData</code>
     * that takes a <code>TransferSupport</code>, which in turn calls this
     * method (if the component in the {@code TransferSupport} is a
     * {@code JComponent}). Developers are encouraged to call and override the
     * newer version as it provides more information (and is the only
     * version that supports use with a {@code TransferHandler} set directly
     * on a {@code JFrame} or other non-{@code JComponent}).
     *
     * @param comp  the component to receive the transfer;
     *              provided to enable sharing of <code>TransferHandler</code>s
     * @param t     the data to import
     * @return  true if the data was inserted into the component, false otherwise
     * @see #importData(TransferHandler.TransferSupport)
     */
    public boolean importData(JComponent comp, Transferable t) {
        PropertyDescriptor prop = getPropertyDescriptor(comp);
        if (prop != null) {
            Method writer = prop.getWriteMethod();
            if (writer == null) {
                // read-only property. ignore
                return false;
            }
            Class<?>[] params = writer.getParameterTypes();
            if (params.length != 1) {
                // zero or more than one argument, ignore
                return false;
            }
            DataFlavor flavor = getPropertyDataFlavor(params[0], t.getTransferDataFlavors());
            if (flavor != null) {
                try {
                    Object value = t.getTransferData(flavor);
                    Object[] args = { value };
                    MethodUtil.invoke(writer, comp, args);
                    return true;
                } catch (Exception ex) {
                    System.err.println("Invocation failed");
                    // invocation code
                }
            }
        }
        return false;
    }

    /**
     * This method is called repeatedly during a drag and drop operation
     * to allow the developer to configure properties of, and to return
     * the acceptability of transfers; with a return value of {@code true}
     * indicating that the transfer represented by the given
     * {@code TransferSupport} (which contains all of the details of the
     * transfer) is acceptable at the current time, and a value of {@code false}
     * rejecting the transfer.
     * <p>
     * For those components that automatically display a drop location during
     * drag and drop, accepting the transfer, by default, tells them to show
     * the drop location. This can be changed by calling
     * {@code setShowDropLocation} on the {@code TransferSupport}.
     * <p>
     * By default, when the transfer is accepted, the chosen drop action is that
     * picked by the user via their drag gesture. The developer can override
     * this and choose a different action, from the supported source
     * actions, by calling {@code setDropAction} on the {@code TransferSupport}.
     * <p>
     * On every call to {@code canImport}, the {@code TransferSupport} contains
     * fresh state. As such, any properties set on it must be set on every
     * call. Upon a drop, {@code canImport} is called one final time before
     * calling into {@code importData}. Any state set on the
     * {@code TransferSupport} during that last call will be available in
     * {@code importData}.
     * <p>
     * This method is not called internally in response to paste operations.
     * As such, it is recommended that implementations of {@code importData}
     * explicitly call this method for such cases and that this method
     * be prepared to return the suitability of paste operations as well.
     * <p>
     * Note: The <code>TransferSupport</code> object passed to this method
     * is only valid for the duration of the method call. It is undefined
     * what values it may contain after this method returns.
     *
     * @param support the object containing the details of
     *        the transfer, not <code>null</code>.
     * @return <code>true</code> if the import can happen,
     *         <code>false</code> otherwise
     * @throws NullPointerException if <code>support</code> is {@code null}
     * @see #importData(TransferHandler.TransferSupport)
     * @see javax.swing.TransferHandler.TransferSupport#setShowDropLocation
     * @see javax.swing.TransferHandler.TransferSupport#setDropAction
     * @since 1.6
     */
    public boolean canImport(TransferSupport support) {
        return support.getComponent() instanceof JComponent
            ? canImport((JComponent)support.getComponent(), support.getDataFlavors())
            : false;
    }

    /**
     * Indicates whether a component will accept an import of the given
     * set of data flavors prior to actually attempting to import it.
     * <p>
     * Note: Swing now calls the newer version of <code>canImport</code>
     * that takes a <code>TransferSupport</code>, which in turn calls this
     * method (only if the component in the {@code TransferSupport} is a
     * {@code JComponent}). Developers are encouraged to call and override the
     * newer version as it provides more information (and is the only
     * version that supports use with a {@code TransferHandler} set directly
     * on a {@code JFrame} or other non-{@code JComponent}).
     *
     * @param comp  the component to receive the transfer;
     *              provided to enable sharing of <code>TransferHandler</code>s
     * @param transferFlavors  the data formats available
     * @return  true if the data can be inserted into the component, false otherwise
     * @see #canImport(TransferHandler.TransferSupport)
     */
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
        PropertyDescriptor prop = getPropertyDescriptor(comp);
        if (prop != null) {
            Method writer = prop.getWriteMethod();
            if (writer == null) {
                // read-only property. ignore
                return false;
            }
            Class<?>[] params = writer.getParameterTypes();
            if (params.length != 1) {
                // zero or more than one argument, ignore
                return false;
            }
            DataFlavor flavor = getPropertyDataFlavor(params[0], transferFlavors);
            if (flavor != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the type of transfer actions supported by the source;
     * any bitwise-OR combination of {@code COPY}, {@code MOVE}
     * and {@code LINK}.
     * <p>
     * Some models are not mutable, so a transfer operation of {@code MOVE}
     * should not be advertised in that case. Returning {@code NONE}
     * disables transfers from the component.
     *
     * @param c  the component holding the data to be transferred;
     *           provided to enable sharing of <code>TransferHandler</code>s
     * @return {@code COPY} if the transfer property can be found,
     *          otherwise returns <code>NONE</code>
     */
    public int getSourceActions(JComponent c) {
        PropertyDescriptor prop = getPropertyDescriptor(c);
        if (prop != null) {
            return COPY;
        }
        return NONE;
    }

    /**
     * Returns an object that establishes the look of a transfer.  This is
     * useful for both providing feedback while performing a drag operation and for
     * representing the transfer in a clipboard implementation that has a visual
     * appearance.  The implementation of the <code>Icon</code> interface should
     * not alter the graphics clip or alpha level.
     * The icon implementation need not be rectangular or paint all of the
     * bounding rectangle and logic that calls the icons paint method should
     * not assume the all bits are painted. <code>null</code> is a valid return value
     * for this method and indicates there is no visual representation provided.
     * In that case, the calling logic is free to represent the
     * transferable however it wants.
     * <p>
     * The default Swing logic will not do an alpha blended drag animation if
     * the return is <code>null</code>.
     *
     * @param t  the data to be transferred; this value is expected to have been
     *  created by the <code>createTransferable</code> method
     * @return  <code>null</code>, indicating
     *    there is no default visual representation
     */
    public Icon getVisualRepresentation(Transferable t) {
        return null;
    }

    /**
     * Creates a <code>Transferable</code> to use as the source for
     * a data transfer. Returns the representation of the data to
     * be transferred, or <code>null</code> if the component's
     * property is <code>null</code>
     *
     * @param c  the component holding the data to be transferred;
     *              provided to enable sharing of <code>TransferHandler</code>s
     * @return  the representation of the data to be transferred, or
     *  <code>null</code> if the property associated with <code>c</code>
     *  is <code>null</code>
     *
     */
    protected Transferable createTransferable(JComponent c) {
        PropertyDescriptor property = getPropertyDescriptor(c);
        if (property != null) {
            return new PropertyTransferable(property, c);
        }
        return null;
    }

    /**
     * Invoked after data has been exported.  This method should remove
     * the data that was transferred if the action was <code>MOVE</code>.
     * <p>
     * This method is implemented to do nothing since <code>MOVE</code>
     * is not a supported action of this implementation
     * (<code>getSourceActions</code> does not include <code>MOVE</code>).
     *
     * @param source the component that was the source of the data
     * @param data   The data that was transferred or possibly null
     *               if the action is <code>NONE</code>.
     * @param action the actual action that was performed
     */
    protected void exportDone(JComponent source, Transferable data, int action) {
    }

    /**
     * Fetches the property descriptor for the property assigned to this transfer
     * handler on the given component (transfer handler may be shared).  This
     * returns <code>null</code> if the property descriptor can't be found
     * or there is an error attempting to fetch the property descriptor.
     */
    private PropertyDescriptor getPropertyDescriptor(JComponent comp) {
        if (propertyName == null) {
            return null;
        }
        Class<?> k = comp.getClass();
        BeanInfo bi;
        try {
            bi = Introspector.getBeanInfo(k);
        } catch (IntrospectionException ex) {
            return null;
        }
        PropertyDescriptor[] props = bi.getPropertyDescriptors();
        for (int i=0; i < props.length; i++) {
            if (propertyName.equals(props[i].getName())) {
                Method reader = props[i].getReadMethod();

                if (reader != null) {
                    Class<?>[] params = reader.getParameterTypes();

                    if (params == null || params.length == 0) {
                        // found the desired descriptor
                        return props[i];
                    }
                }
            }
        }
        return null;
    }

    /**
     * Fetches the data flavor from the array of possible flavors that
     * has data of the type represented by property type.  Null is
     * returned if there is no match.
     */
    private DataFlavor getPropertyDataFlavor(Class<?> k, DataFlavor[] flavors) {
        for(int i = 0; i < flavors.length; i++) {
            DataFlavor flavor = flavors[i];
            if ("application".equals(flavor.getPrimaryType()) &&
                "x-java-jvm-local-objectref".equals(flavor.getSubType()) &&
                k.isAssignableFrom(flavor.getRepresentationClass())) {

                return flavor;
            }
        }
        return null;
    }


    private String propertyName;
    private static SwingDragGestureRecognizer recognizer = null;

    private static DropTargetListener getDropTargetListener() {
        synchronized(DropHandler.class) {
            DropHandler handler =
                (DropHandler)AppContext.getAppContext().get(DropHandler.class);

            if (handler == null) {
                handler = new DropHandler();
                AppContext.getAppContext().put(DropHandler.class, handler);
            }

            return handler;
        }
    }

    static class PropertyTransferable implements Transferable {

        PropertyTransferable(PropertyDescriptor p, JComponent c) {
            property = p;
            component = c;
        }

        // --- Transferable methods ----------------------------------------------

        /**
         * Returns an array of <code>DataFlavor</code> objects indicating the flavors the data
         * can be provided in.  The array should be ordered according to preference
         * for providing the data (from most richly descriptive to least descriptive).
         * @return an array of data flavors in which this data can be transferred
         */
        public DataFlavor[] getTransferDataFlavors() {
            DataFlavor[] flavors = new DataFlavor[1];
            Class<?> propertyType = property.getPropertyType();
            String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=" + propertyType.getName();
            try {
                flavors[0] = new DataFlavor(mimeType);
            } catch (ClassNotFoundException cnfe) {
                flavors = new DataFlavor[0];
            }
            return flavors;
        }

        /**
         * Returns whether the specified data flavor is supported for
         * this object.
         * @param flavor the requested flavor for the data
         * @return true if this <code>DataFlavor</code> is supported,
         *   otherwise false
         */
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            Class<?> propertyType = property.getPropertyType();
            if ("application".equals(flavor.getPrimaryType()) &&
                "x-java-jvm-local-objectref".equals(flavor.getSubType()) &&
                flavor.getRepresentationClass().isAssignableFrom(propertyType)) {

                return true;
            }
            return false;
        }

        /**
         * Returns an object which represents the data to be transferred.  The class
         * of the object returned is defined by the representation class of the flavor.
         *
         * @param flavor the requested flavor for the data
         * @see DataFlavor#getRepresentationClass
         * @exception IOException                if the data is no longer available
         *              in the requested flavor.
         * @exception UnsupportedFlavorException if the requested data flavor is
         *              not supported.
         */
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (! isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            Method reader = property.getReadMethod();
            Object value = null;
            try {
                value = MethodUtil.invoke(reader, component, (Object[])null);
            } catch (Exception ex) {
                throw new IOException("Property read failed: " + property.getName());
            }
            return value;
        }

        JComponent component;
        PropertyDescriptor property;
    }

    /**
     * This is the default drop target for drag and drop operations if
     * one isn't provided by the developer.  <code>DropTarget</code>
     * only supports one <code>DropTargetListener</code> and doesn't
     * function properly if it isn't set.
     * This class sets the one listener as the linkage of drop handling
     * to the <code>TransferHandler</code>, and adds support for
     * additional listeners which some of the <code>ComponentUI</code>
     * implementations install to manipulate a drop insertion location.
     */
    static class SwingDropTarget extends DropTarget implements UIResource {

        SwingDropTarget(Component c) {
            super(c, COPY_OR_MOVE | LINK, null);
            try {
                // addDropTargetListener is overridden
                // we specifically need to add to the superclass
                super.addDropTargetListener(getDropTargetListener());
            } catch (TooManyListenersException tmle) {}
        }

        public void addDropTargetListener(DropTargetListener dtl) throws TooManyListenersException {
            // Since the super class only supports one DropTargetListener,
            // and we add one from the constructor, we always add to the
            // extended list.
            if (listenerList == null) {
                listenerList = new EventListenerList();
            }
            listenerList.add(DropTargetListener.class, dtl);
        }

        public void removeDropTargetListener(DropTargetListener dtl) {
            if (listenerList != null) {
                listenerList.remove(DropTargetListener.class, dtl);
            }
        }

        // --- DropTargetListener methods (multicast) --------------------------

        public void dragEnter(DropTargetDragEvent e) {
            super.dragEnter(e);
            if (listenerList != null) {
                Object[] listeners = listenerList.getListenerList();
                for (int i = listeners.length-2; i>=0; i-=2) {
                    if (listeners[i]==DropTargetListener.class) {
                        ((DropTargetListener)listeners[i+1]).dragEnter(e);
                    }
                }
            }
        }

        public void dragOver(DropTargetDragEvent e) {
            super.dragOver(e);
            if (listenerList != null) {
                Object[] listeners = listenerList.getListenerList();
                for (int i = listeners.length-2; i>=0; i-=2) {
                    if (listeners[i]==DropTargetListener.class) {
                        ((DropTargetListener)listeners[i+1]).dragOver(e);
                    }
                }
            }
        }

        public void dragExit(DropTargetEvent e) {
            super.dragExit(e);
            if (listenerList != null) {
                Object[] listeners = listenerList.getListenerList();
                for (int i = listeners.length-2; i>=0; i-=2) {
                    if (listeners[i]==DropTargetListener.class) {
                        ((DropTargetListener)listeners[i+1]).dragExit(e);
                    }
                }
            }
            if (!isActive()) {
                // If the Drop target is inactive the dragExit will not be dispatched to the dtListener,
                // so make sure that we clean up the dtListener anyway.
                DropTargetListener dtListener = getDropTargetListener();
                    if (dtListener != null && dtListener instanceof DropHandler) {
                        ((DropHandler)dtListener).cleanup(false);
                    }
            }
        }

        public void drop(DropTargetDropEvent e) {
            super.drop(e);
            if (listenerList != null) {
                Object[] listeners = listenerList.getListenerList();
                for (int i = listeners.length-2; i>=0; i-=2) {
                    if (listeners[i]==DropTargetListener.class) {
                        ((DropTargetListener)listeners[i+1]).drop(e);
                    }
                }
            }
        }

        public void dropActionChanged(DropTargetDragEvent e) {
            super.dropActionChanged(e);
            if (listenerList != null) {
                Object[] listeners = listenerList.getListenerList();
                for (int i = listeners.length-2; i>=0; i-=2) {
                    if (listeners[i]==DropTargetListener.class) {
                        ((DropTargetListener)listeners[i+1]).dropActionChanged(e);
                    }
                }
            }
        }

        private EventListenerList listenerList;
    }

    private static class DropHandler implements DropTargetListener,
                                                Serializable,
                                                ActionListener {

        private Timer timer;
        private Point lastPosition;
        private Rectangle outer = new Rectangle();
        private Rectangle inner = new Rectangle();
        private int hysteresis = 10;

        private Component component;
        private Object state;
        private TransferSupport support =
            new TransferSupport(null, (DropTargetEvent)null);

        private static final int AUTOSCROLL_INSET = 10;

        /**
         * Update the geometry of the autoscroll region.  The geometry is
         * maintained as a pair of rectangles.  The region can cause
         * a scroll if the pointer sits inside it for the duration of the
         * timer.  The region that causes the timer countdown is the area
         * between the two rectangles.
         * <p>
         * This is implemented to use the visible area of the component
         * as the outer rectangle, and the insets are fixed at 10. Should
         * the component be smaller than a total of 20 in any direction,
         * autoscroll will not occur in that direction.
         */
        private void updateAutoscrollRegion(JComponent c) {
            // compute the outer
            Rectangle visible = c.getVisibleRect();
            outer.setBounds(visible.x, visible.y, visible.width, visible.height);

            // compute the insets
            Insets i = new Insets(0, 0, 0, 0);
            if (c instanceof Scrollable) {
                int minSize = 2 * AUTOSCROLL_INSET;

                if (visible.width >= minSize) {
                    i.left = i.right = AUTOSCROLL_INSET;
                }

                if (visible.height >= minSize) {
                    i.top = i.bottom = AUTOSCROLL_INSET;
                }
            }

            // set the inner from the insets
            inner.setBounds(visible.x + i.left,
                          visible.y + i.top,
                          visible.width - (i.left + i.right),
                          visible.height - (i.top  + i.bottom));
        }

        /**
         * Perform an autoscroll operation.  This is implemented to scroll by the
         * unit increment of the Scrollable using scrollRectToVisible.  If the
         * cursor is in a corner of the autoscroll region, more than one axis will
         * scroll.
         */
        private void autoscroll(JComponent c, Point pos) {
            if (c instanceof Scrollable) {
                Scrollable s = (Scrollable) c;
                if (pos.y < inner.y) {
                    // scroll upward
                    int dy = s.getScrollableUnitIncrement(outer, SwingConstants.VERTICAL, -1);
                    Rectangle r = new Rectangle(inner.x, outer.y - dy, inner.width, dy);
                    c.scrollRectToVisible(r);
                } else if (pos.y > (inner.y + inner.height)) {
                    // scroll downard
                    int dy = s.getScrollableUnitIncrement(outer, SwingConstants.VERTICAL, 1);
                    Rectangle r = new Rectangle(inner.x, outer.y + outer.height, inner.width, dy);
                    c.scrollRectToVisible(r);
                }

                if (pos.x < inner.x) {
                    // scroll left
                    int dx = s.getScrollableUnitIncrement(outer, SwingConstants.HORIZONTAL, -1);
                    Rectangle r = new Rectangle(outer.x - dx, inner.y, dx, inner.height);
                    c.scrollRectToVisible(r);
                } else if (pos.x > (inner.x + inner.width)) {
                    // scroll right
                    int dx = s.getScrollableUnitIncrement(outer, SwingConstants.HORIZONTAL, 1);
                    Rectangle r = new Rectangle(outer.x + outer.width, inner.y, dx, inner.height);
                    c.scrollRectToVisible(r);
                }
            }
        }

        /**
         * Initializes the internal properties if they haven't been already
         * inited. This is done lazily to avoid loading of desktop properties.
         */
        private void initPropertiesIfNecessary() {
            if (timer == null) {
                Toolkit t = Toolkit.getDefaultToolkit();
                Integer prop;

                prop = (Integer)
                    t.getDesktopProperty("DnD.Autoscroll.interval");

                timer = new Timer(prop == null ? 100 : prop.intValue(), this);

                prop = (Integer)
                    t.getDesktopProperty("DnD.Autoscroll.initialDelay");

                timer.setInitialDelay(prop == null ? 100 : prop.intValue());

                prop = (Integer)
                    t.getDesktopProperty("DnD.Autoscroll.cursorHysteresis");

                if (prop != null) {
                    hysteresis = prop.intValue();
                }
            }
        }

        /**
         * The timer fired, perform autoscroll if the pointer is within the
         * autoscroll region.
         * <P>
         * @param e the <code>ActionEvent</code>
         */
        public void actionPerformed(ActionEvent e) {
            updateAutoscrollRegion((JComponent)component);
            if (outer.contains(lastPosition) && !inner.contains(lastPosition)) {
                autoscroll((JComponent)component, lastPosition);
            }
        }

        // --- DropTargetListener methods -----------------------------------

        private void setComponentDropLocation(TransferSupport support,
                                              boolean forDrop) {

            DropLocation dropLocation = (support == null)
                                        ? null
                                        : support.getDropLocation();

            if (SunToolkit.isInstanceOf(component, "javax.swing.text.JTextComponent")) {
                state = SwingAccessor.getJTextComponentAccessor().
                            setDropLocation((JTextComponent)component, dropLocation, state, forDrop);
            } else if (component instanceof JComponent) {
                state = ((JComponent)component).setDropLocation(dropLocation, state, forDrop);
            }
        }

        private void handleDrag(DropTargetDragEvent e) {
            TransferHandler importer =
                ((HasGetTransferHandler)component).getTransferHandler();

            if (importer == null) {
                e.rejectDrag();
                setComponentDropLocation(null, false);
                return;
            }

            support.setDNDVariables(component, e);
            boolean canImport = importer.canImport(support);

            if (canImport) {
                e.acceptDrag(support.getDropAction());
            } else {
                e.rejectDrag();
            }

            boolean showLocation = support.showDropLocationIsSet ?
                                   support.showDropLocation :
                                   canImport;

            setComponentDropLocation(showLocation ? support : null, false);
        }

        public void dragEnter(DropTargetDragEvent e) {
            state = null;
            component = e.getDropTargetContext().getComponent();

            handleDrag(e);

            if (component instanceof JComponent) {
                lastPosition = e.getLocation();
                updateAutoscrollRegion((JComponent)component);
                initPropertiesIfNecessary();
            }
        }

        public void dragOver(DropTargetDragEvent e) {
            handleDrag(e);

            if (!(component instanceof JComponent)) {
                return;
            }

            Point p = e.getLocation();

            if (Math.abs(p.x - lastPosition.x) > hysteresis
                    || Math.abs(p.y - lastPosition.y) > hysteresis) {
                // no autoscroll
                if (timer.isRunning()) timer.stop();
            } else {
                if (!timer.isRunning()) timer.start();
            }

            lastPosition = p;
        }

        public void dragExit(DropTargetEvent e) {
            cleanup(false);
        }

        public void drop(DropTargetDropEvent e) {
            TransferHandler importer =
                ((HasGetTransferHandler)component).getTransferHandler();

            if (importer == null) {
                e.rejectDrop();
                cleanup(false);
                return;
            }

            support.setDNDVariables(component, e);
            boolean canImport = importer.canImport(support);

            if (canImport) {
                e.acceptDrop(support.getDropAction());

                boolean showLocation = support.showDropLocationIsSet ?
                                       support.showDropLocation :
                                       canImport;

                setComponentDropLocation(showLocation ? support : null, false);

                boolean success;

                try {
                    success = importer.importData(support);
                } catch (RuntimeException re) {
                    success = false;
                }

                e.dropComplete(success);
                cleanup(success);
            } else {
                e.rejectDrop();
                cleanup(false);
            }
        }

        public void dropActionChanged(DropTargetDragEvent e) {
            /*
             * Work-around for Linux bug where dropActionChanged
             * is called before dragEnter.
             */
            if (component == null) {
                return;
            }

            handleDrag(e);
        }

        private void cleanup(boolean forDrop) {
            setComponentDropLocation(null, forDrop);
            if (component instanceof JComponent) {
                ((JComponent)component).dndDone();
            }

            if (timer != null) {
                timer.stop();
            }

            state = null;
            component = null;
            lastPosition = null;
        }
    }

    /**
     * This is the default drag handler for drag and drop operations that
     * use the <code>TransferHandler</code>.
     */
    private static class DragHandler implements DragGestureListener, DragSourceListener {

        private boolean scrolls;

        // --- DragGestureListener methods -----------------------------------

        /**
         * a Drag gesture has been recognized
         */
        public void dragGestureRecognized(DragGestureEvent dge) {
            JComponent c = (JComponent) dge.getComponent();
            TransferHandler th = c.getTransferHandler();
            Transferable t = th.createTransferable(c);
            if (t != null) {
                scrolls = c.getAutoscrolls();
                c.setAutoscrolls(false);
                try {
                    Image im = th.getDragImage();
                    if (im == null) {
                        dge.startDrag(null, t, this);
                    } else {
                        dge.startDrag(null, im, th.getDragImageOffset(), t, this);
                    }
                    return;
                } catch (RuntimeException re) {
                    c.setAutoscrolls(scrolls);
                }
            }

            th.exportDone(c, t, NONE);
        }

        // --- DragSourceListener methods -----------------------------------

        /**
         * as the hotspot enters a platform dependent drop site
         */
        public void dragEnter(DragSourceDragEvent dsde) {
        }

        /**
         * as the hotspot moves over a platform dependent drop site
         */
        public void dragOver(DragSourceDragEvent dsde) {
        }

        /**
         * as the hotspot exits a platform dependent drop site
         */
        public void dragExit(DragSourceEvent dsde) {
        }

        /**
         * as the operation completes
         */
        public void dragDropEnd(DragSourceDropEvent dsde) {
            DragSourceContext dsc = dsde.getDragSourceContext();
            JComponent c = (JComponent)dsc.getComponent();
            if (dsde.getDropSuccess()) {
                c.getTransferHandler().exportDone(c, dsc.getTransferable(), dsde.getDropAction());
            } else {
                c.getTransferHandler().exportDone(c, dsc.getTransferable(), NONE);
            }
            c.setAutoscrolls(scrolls);
        }

        public void dropActionChanged(DragSourceDragEvent dsde) {
        }
    }

    private static class SwingDragGestureRecognizer extends DragGestureRecognizer {

        SwingDragGestureRecognizer(DragGestureListener dgl) {
            super(DragSource.getDefaultDragSource(), null, NONE, dgl);
        }

        void gestured(JComponent c, MouseEvent e, int srcActions, int action) {
            setComponent(c);
            setSourceActions(srcActions);
            appendEvent(e);
            fireDragGestureRecognized(action, e.getPoint());
        }

        /**
         * register this DragGestureRecognizer's Listeners with the Component
         */
        protected void registerListeners() {
        }

        /**
         * unregister this DragGestureRecognizer's Listeners with the Component
         *
         * subclasses must override this method
         */
        protected void unregisterListeners() {
        }

    }

    static final Action cutAction = new TransferAction("cut");
    static final Action copyAction = new TransferAction("copy");
    static final Action pasteAction = new TransferAction("paste");

    static class TransferAction extends UIAction implements UIResource {

        TransferAction(String name) {
            super(name);
        }

        @Override
        public boolean accept(Object sender) {
            return !(sender instanceof JComponent
                    && ((JComponent)sender).getTransferHandler() == null);
        }

        private static final JavaSecurityAccess javaSecurityAccess =
            SharedSecrets.getJavaSecurityAccess();

        public void actionPerformed(final ActionEvent e) {
            final Object src = e.getSource();

            final PrivilegedAction<Void> action = new PrivilegedAction<Void>() {
                public Void run() {
                    actionPerformedImpl(e);
                    return null;
                }
            };

            @SuppressWarnings("removal")
            final AccessControlContext stack = AccessController.getContext();
            @SuppressWarnings("removal")
            final AccessControlContext srcAcc = AWTAccessor.getComponentAccessor().getAccessControlContext((Component)src);
            @SuppressWarnings("removal")
            final AccessControlContext eventAcc = AWTAccessor.getAWTEventAccessor().getAccessControlContext(e);

                if (srcAcc == null) {
                    javaSecurityAccess.doIntersectionPrivilege(action, stack, eventAcc);
                } else {
                    javaSecurityAccess.doIntersectionPrivilege(
                        new PrivilegedAction<Void>() {
                            public Void run() {
                                javaSecurityAccess.doIntersectionPrivilege(action, eventAcc);
                                return null;
                             }
                    }, stack, srcAcc);
                }
        }

        private void actionPerformedImpl(ActionEvent e) {
            Object src = e.getSource();
            if (src instanceof JComponent) {
                JComponent c = (JComponent) src;
                TransferHandler th = c.getTransferHandler();
                Clipboard clipboard = getClipboard(c);
                String name = (String) getValue(Action.NAME);

                Transferable trans = null;

                // any of these calls may throw IllegalStateException
                try {
                    if ((clipboard != null) && (th != null) && (name != null)) {
                        if ("cut".equals(name)) {
                            th.exportToClipboard(c, clipboard, MOVE);
                        } else if ("copy".equals(name)) {
                            th.exportToClipboard(c, clipboard, COPY);
                        } else if ("paste".equals(name)) {
                            trans = clipboard.getContents(null);
                        }
                    }
                } catch (IllegalStateException ise) {
                    // clipboard was unavailable
                    UIManager.getLookAndFeel().provideErrorFeedback(c);
                    return;
                }

                // this is a paste action, import data into the component
                if (trans != null) {
                    th.importData(new TransferSupport(c, trans));
                }
            }
        }

        /**
         * Returns the clipboard to use for cut/copy/paste.
         */
        private Clipboard getClipboard(JComponent c) {
            if (SwingUtilities2.canAccessSystemClipboard()) {
                return c.getToolkit().getSystemClipboard();
            }
            Clipboard clipboard = (Clipboard)sun.awt.AppContext.getAppContext().
                get(SandboxClipboardKey);
            if (clipboard == null) {
                clipboard = new Clipboard("Sandboxed Component Clipboard");
                sun.awt.AppContext.getAppContext().put(SandboxClipboardKey,
                                                       clipboard);
            }
            return clipboard;
        }

        /**
         * Key used in app context to lookup Clipboard to use if access to
         * System clipboard is denied.
         */
        private static Object SandboxClipboardKey = new Object();

    }

}
