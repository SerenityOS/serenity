/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.dnd;

import java.awt.AWTError;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.peer.DragSourceContextPeer;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.TooManyListenersException;

import sun.awt.AWTAccessor;
import sun.awt.ComponentFactory;

/**
 * The {@code DragSourceContext} class is responsible for managing the
 * initiator side of the Drag and Drop protocol. In particular, it is responsible
 * for managing drag event notifications to the
 * {@linkplain DragSourceListener DragSourceListeners}
 * and {@linkplain DragSourceMotionListener DragSourceMotionListeners}, and providing the
 * {@link Transferable} representing the source data for the drag operation.
 * <p>
 * Note that the {@code DragSourceContext} itself
 * implements the {@code DragSourceListener} and
 * {@code DragSourceMotionListener} interfaces.
 * This is to allow the platform peer
 * (the {@link DragSourceContextPeer} instance)
 * created by the {@link DragSource} to notify
 * the {@code DragSourceContext} of
 * state changes in the ongoing operation. This allows the
 * {@code DragSourceContext} object to interpose
 * itself between the platform and the
 * listeners provided by the initiator of the drag operation.
 * <p>
 * <a id="defaultCursor"></a>
 * By default, {@code DragSourceContext} sets the cursor as appropriate
 * for the current state of the drag and drop operation. For example, if
 * the user has chosen {@linkplain DnDConstants#ACTION_MOVE the move action},
 * and the pointer is over a target that accepts
 * the move action, the default move cursor is shown. When
 * the pointer is over an area that does not accept the transfer,
 * the default "no drop" cursor is shown.
 * <p>
 * This default handling mechanism is disabled when a custom cursor is set
 * by the {@link #setCursor} method. When the default handling is disabled,
 * it becomes the responsibility
 * of the developer to keep the cursor up to date, by listening
 * to the {@code DragSource} events and calling the {@code setCursor()} method.
 * Alternatively, you can provide custom cursor behavior by providing
 * custom implementations of the {@code DragSource}
 * and the {@code DragSourceContext} classes.
 *
 * @see DragSourceListener
 * @see DragSourceMotionListener
 * @see DnDConstants
 * @since 1.2
 */

public class DragSourceContext
    implements DragSourceListener, DragSourceMotionListener, Serializable {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -115407898692194719L;

    // used by updateCurrentCursor

    /**
     * An {@code int} used by updateCurrentCursor()
     * indicating that the {@code Cursor} should change
     * to the default (no drop) {@code Cursor}.
     */
    protected static final int DEFAULT = 0;

    /**
     * An {@code int} used by updateCurrentCursor()
     * indicating that the {@code Cursor}
     * has entered a {@code DropTarget}.
     */
    protected static final int ENTER   = 1;

    /**
     * An {@code int} used by updateCurrentCursor()
     * indicating that the {@code Cursor} is
     * over a {@code DropTarget}.
     */
    protected static final int OVER    = 2;

    /**
     * An {@code int} used by updateCurrentCursor()
     * indicating that the user operation has changed.
     */

    protected static final int CHANGED = 3;

    static {
        AWTAccessor.setDragSourceContextAccessor(dsc -> dsc.peer);
    }

    /**
     * Called from {@code DragSource}, this constructor creates a new
     * {@code DragSourceContext} given the
     * {@code DragSourceContextPeer} for this Drag, the
     * {@code DragGestureEvent} that triggered the Drag, the initial
     * {@code Cursor} to use for the Drag, an (optional)
     * {@code Image} to display while the Drag is taking place, the offset
     * of the {@code Image} origin from the hotspot at the instant of the
     * triggering event, the {@code Transferable} subject data, and the
     * {@code DragSourceListener} to use during the Drag and Drop
     * operation.
     * <br>
     * If {@code DragSourceContextPeer} is {@code null}
     * {@code NullPointerException} is thrown.
     * <br>
     * If {@code DragGestureEvent} is {@code null}
     * {@code NullPointerException} is thrown.
     * <br>
     * If {@code Cursor} is {@code null} no exception is thrown and
     * the default drag cursor behavior is activated for this drag operation.
     * <br>
     * If {@code Image} is {@code null} no exception is thrown.
     * <br>
     * If {@code Image} is not {@code null} and the offset is
     * {@code null NullPointerException} is thrown.
     * <br>
     * If {@code Transferable} is {@code null}
     * {@code NullPointerException} is thrown.
     * <br>
     * If {@code DragSourceListener} is {@code null} no exception
     * is thrown.
     *
     * @param trigger    the triggering event
     * @param dragCursor     the initial {@code Cursor} for this drag operation
     *                       or {@code null} for the default cursor handling;
     *                       see <a href="DragSourceContext.html#defaultCursor">class level documentation</a>
     *                       for more details on the cursor handling mechanism during drag and drop
     * @param dragImage  the {@code Image} to drag (or {@code null})
     * @param offset     the offset of the image origin from the hotspot at the
     *                   instant of the triggering event
     * @param t          the {@code Transferable}
     * @param dsl        the {@code DragSourceListener}
     *
     * @throws IllegalArgumentException if the {@code Component} associated
     *         with the trigger event is {@code null}.
     * @throws IllegalArgumentException if the {@code DragSource} for the
     *         trigger event is {@code null}.
     * @throws IllegalArgumentException if the drag action for the
     *         trigger event is {@code DnDConstants.ACTION_NONE}.
     * @throws IllegalArgumentException if the source actions for the
     *         {@code DragGestureRecognizer} associated with the trigger
     *         event are equal to {@code DnDConstants.ACTION_NONE}.
     * @throws NullPointerException if dscp, trigger, or t are null, or
     *         if dragImage is non-null and offset is null
     */
    public DragSourceContext(DragGestureEvent trigger, Cursor dragCursor,
                             Image dragImage, Point offset, Transferable t,
                             DragSourceListener dsl) {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        if (!(toolkit instanceof ComponentFactory)) {
            throw new AWTError("Unsupported toolkit: " + toolkit);
        }
        DragSourceContextPeer dscp = ((ComponentFactory) toolkit).
                createDragSourceContextPeer(trigger);

        if (dscp == null) {
            throw new NullPointerException("DragSourceContextPeer");
        }

        if (trigger == null) {
            throw new NullPointerException("Trigger");
        }

        if (trigger.getDragSource() == null) {
            throw new IllegalArgumentException("DragSource");
        }

        if (trigger.getComponent() == null) {
            throw new IllegalArgumentException("Component");
        }

        if (trigger.getSourceAsDragGestureRecognizer().getSourceActions() ==
                 DnDConstants.ACTION_NONE) {
            throw new IllegalArgumentException("source actions");
        }

        if (trigger.getDragAction() == DnDConstants.ACTION_NONE) {
            throw new IllegalArgumentException("no drag action");
        }

        if (t == null) {
            throw new NullPointerException("Transferable");
        }

        if (dragImage != null && offset == null) {
            throw new NullPointerException("offset");
        }

        peer         = dscp;
        this.trigger = trigger;
        cursor       = dragCursor;
        transferable = t;
        listener     = dsl;
        sourceActions =
            trigger.getSourceAsDragGestureRecognizer().getSourceActions();

        useCustomCursor = (dragCursor != null);

        updateCurrentCursor(trigger.getDragAction(), getSourceActions(), DEFAULT);
    }

    /**
     * Returns the {@code DragSource}
     * that instantiated this {@code DragSourceContext}.
     *
     * @return the {@code DragSource} that
     *   instantiated this {@code DragSourceContext}
     */

    public DragSource   getDragSource() { return trigger.getDragSource(); }

    /**
     * Returns the {@code Component} associated with this
     * {@code DragSourceContext}.
     *
     * @return the {@code Component} that started the drag
     */

    public Component    getComponent() { return trigger.getComponent(); }

    /**
     * Returns the {@code DragGestureEvent}
     * that initially triggered the drag.
     *
     * @return the Event that triggered the drag
     */

    public DragGestureEvent getTrigger() { return trigger; }

    /**
     * Returns a bitwise mask of {@code DnDConstants} that
     * represent the set of drop actions supported by the drag source for the
     * drag operation associated with this {@code DragSourceContext}.
     *
     * @return the drop actions supported by the drag source
     */
    public int  getSourceActions() {
        return sourceActions;
    }

    /**
     * Sets the custom cursor for this drag operation to the specified
     * {@code Cursor}.  If the specified {@code Cursor}
     * is {@code null}, the default drag cursor behavior is
     * activated for this drag operation, otherwise it is deactivated.
     *
     * @param c     the initial {@code Cursor} for this drag operation,
     *                       or {@code null} for the default cursor handling;
     *                       see {@linkplain Cursor class
     *                       level documentation} for more details
     *                       on the cursor handling during drag and drop
     *
     */

    public synchronized void setCursor(Cursor c) {
        useCustomCursor = (c != null);
        setCursorImpl(c);
    }

    /**
     * Returns the current custom drag {@code Cursor}.
     *
     * @return the current custom drag {@code Cursor}, if it was set
     *         otherwise returns {@code null}.
     * @see #setCursor
     */

    public Cursor getCursor() { return cursor; }

    /**
     * Add a {@code DragSourceListener} to this
     * {@code DragSourceContext} if one has not already been added.
     * If a {@code DragSourceListener} already exists,
     * this method throws a {@code TooManyListenersException}.
     *
     * @param dsl the {@code DragSourceListener} to add.
     * Note that while {@code null} is not prohibited,
     * it is not acceptable as a parameter.
     *
     * @throws TooManyListenersException if
     * a {@code DragSourceListener} has already been added
     */

    public synchronized void addDragSourceListener(DragSourceListener dsl) throws TooManyListenersException {
        if (dsl == null) return;

        if (equals(dsl)) throw new IllegalArgumentException("DragSourceContext may not be its own listener");

        if (listener != null)
            throw new TooManyListenersException();
        else
            listener = dsl;
    }

    /**
     * Removes the specified {@code DragSourceListener}
     * from  this {@code DragSourceContext}.
     *
     * @param dsl the {@code DragSourceListener} to remove;
     *     note that while {@code null} is not prohibited,
     *     it is not acceptable as a parameter
     */

    public synchronized void removeDragSourceListener(DragSourceListener dsl) {
        if (listener != null && listener.equals(dsl)) {
            listener = null;
        } else
            throw new IllegalArgumentException();
    }

    /**
     * Notifies the peer that the {@code Transferable}'s
     * {@code DataFlavor}s have changed.
     */

    public void transferablesFlavorsChanged() {
        if (peer != null) peer.transferablesFlavorsChanged();
    }

    /**
     * Calls {@code dragEnter} on the
     * {@code DragSourceListener}s registered with this
     * {@code DragSourceContext} and with the associated
     * {@code DragSource}, and passes them the specified
     * {@code DragSourceDragEvent}.
     *
     * @param dsde the {@code DragSourceDragEvent}
     */
    public void dragEnter(DragSourceDragEvent dsde) {
        DragSourceListener dsl = listener;
        if (dsl != null) {
            dsl.dragEnter(dsde);
        }
        getDragSource().processDragEnter(dsde);

        updateCurrentCursor(getSourceActions(), dsde.getTargetActions(), ENTER);
    }

    /**
     * Calls {@code dragOver} on the
     * {@code DragSourceListener}s registered with this
     * {@code DragSourceContext} and with the associated
     * {@code DragSource}, and passes them the specified
     * {@code DragSourceDragEvent}.
     *
     * @param dsde the {@code DragSourceDragEvent}
     */
    public void dragOver(DragSourceDragEvent dsde) {
        DragSourceListener dsl = listener;
        if (dsl != null) {
            dsl.dragOver(dsde);
        }
        getDragSource().processDragOver(dsde);

        updateCurrentCursor(getSourceActions(), dsde.getTargetActions(), OVER);
    }

    /**
     * Calls {@code dragExit} on the
     * {@code DragSourceListener}s registered with this
     * {@code DragSourceContext} and with the associated
     * {@code DragSource}, and passes them the specified
     * {@code DragSourceEvent}.
     *
     * @param dse the {@code DragSourceEvent}
     */
    public void dragExit(DragSourceEvent dse) {
        DragSourceListener dsl = listener;
        if (dsl != null) {
            dsl.dragExit(dse);
        }
        getDragSource().processDragExit(dse);

        updateCurrentCursor(DnDConstants.ACTION_NONE, DnDConstants.ACTION_NONE, DEFAULT);
    }

    /**
     * Calls {@code dropActionChanged} on the
     * {@code DragSourceListener}s registered with this
     * {@code DragSourceContext} and with the associated
     * {@code DragSource}, and passes them the specified
     * {@code DragSourceDragEvent}.
     *
     * @param dsde the {@code DragSourceDragEvent}
     */
    public void dropActionChanged(DragSourceDragEvent dsde) {
        DragSourceListener dsl = listener;
        if (dsl != null) {
            dsl.dropActionChanged(dsde);
        }
        getDragSource().processDropActionChanged(dsde);

        updateCurrentCursor(getSourceActions(), dsde.getTargetActions(), CHANGED);
    }

    /**
     * Calls {@code dragDropEnd} on the
     * {@code DragSourceListener}s registered with this
     * {@code DragSourceContext} and with the associated
     * {@code DragSource}, and passes them the specified
     * {@code DragSourceDropEvent}.
     *
     * @param dsde the {@code DragSourceDropEvent}
     */
    public void dragDropEnd(DragSourceDropEvent dsde) {
        DragSourceListener dsl = listener;
        if (dsl != null) {
            dsl.dragDropEnd(dsde);
        }
        getDragSource().processDragDropEnd(dsde);
    }

    /**
     * Calls {@code dragMouseMoved} on the
     * {@code DragSourceMotionListener}s registered with the
     * {@code DragSource} associated with this
     * {@code DragSourceContext}, and them passes the specified
     * {@code DragSourceDragEvent}.
     *
     * @param dsde the {@code DragSourceDragEvent}
     * @since 1.4
     */
    public void dragMouseMoved(DragSourceDragEvent dsde) {
        getDragSource().processDragMouseMoved(dsde);
    }

    /**
     * Returns the {@code Transferable} associated with
     * this {@code DragSourceContext}.
     *
     * @return the {@code Transferable}
     */
    public Transferable getTransferable() { return transferable; }

    /**
     * If the default drag cursor behavior is active, this method
     * sets the default drag cursor for the specified actions
     * supported by the drag source, the drop target action,
     * and status, otherwise this method does nothing.
     *
     * @param sourceAct the actions supported by the drag source
     * @param targetAct the drop target action
     * @param status one of the fields {@code DEFAULT},
     *               {@code ENTER}, {@code OVER},
     *               {@code CHANGED}
     */
    @SuppressWarnings("fallthrough")
    protected synchronized void updateCurrentCursor(int sourceAct, int targetAct, int status) {

        // if the cursor has been previously set then don't do any defaults
        // processing.

        if (useCustomCursor) {
            return;
        }

        // do defaults processing

        Cursor c = null;

        switch (status) {
            default:
                targetAct = DnDConstants.ACTION_NONE;
            case ENTER:
            case OVER:
            case CHANGED:
                int    ra = sourceAct & targetAct;

                if (ra == DnDConstants.ACTION_NONE) { // no drop possible
                    if ((sourceAct & DnDConstants.ACTION_LINK) == DnDConstants.ACTION_LINK)
                        c = DragSource.DefaultLinkNoDrop;
                    else if ((sourceAct & DnDConstants.ACTION_MOVE) == DnDConstants.ACTION_MOVE)
                        c = DragSource.DefaultMoveNoDrop;
                    else
                        c = DragSource.DefaultCopyNoDrop;
                } else { // drop possible
                    if ((ra & DnDConstants.ACTION_LINK) == DnDConstants.ACTION_LINK)
                        c = DragSource.DefaultLinkDrop;
                    else if ((ra & DnDConstants.ACTION_MOVE) == DnDConstants.ACTION_MOVE)
                        c = DragSource.DefaultMoveDrop;
                    else
                        c = DragSource.DefaultCopyDrop;
                }
        }

        setCursorImpl(c);
    }

    private void setCursorImpl(Cursor c) {
        if (cursor == null || !cursor.equals(c)) {
            cursor = c;
            if (peer != null) peer.setCursor(cursor);
        }
    }

    /**
     * Serializes this {@code DragSourceContext}. This method first
     * performs default serialization. Next, this object's
     * {@code Transferable} is written out if and only if it can be
     * serialized. If not, {@code null} is written instead. In this case,
     * a {@code DragSourceContext} created from the resulting deserialized
     * stream will contain a dummy {@code Transferable} which supports no
     * {@code DataFlavor}s. Finally, this object's
     * {@code DragSourceListener} is written out if and only if it can be
     * serialized. If not, {@code null} is written instead.
     *
     * @param  s the {@code ObjectOutputStream} to write
     * @throws IOException if an I/O error occurs
     * @serialData The default serializable fields, in alphabetical order,
     *             followed by either a {@code Transferable} instance, or
     *             {@code null}, followed by either a
     *             {@code DragSourceListener} instance, or
     *             {@code null}.
     * @since 1.4
     */
    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();

        s.writeObject(SerializationTester.test(transferable)
                      ? transferable : null);
        s.writeObject(SerializationTester.test(listener)
                      ? listener : null);
    }

    /**
     * Deserializes this {@code DragSourceContext}. This method first
     * performs default deserialization for all non-{@code transient}
     * fields. This object's {@code Transferable} and
     * {@code DragSourceListener} are then deserialized as well by using
     * the next two objects in the stream. If the resulting
     * {@code Transferable} is {@code null}, this object's
     * {@code Transferable} is set to a dummy {@code Transferable}
     * which supports no {@code DataFlavor}s.
     *
     * @param  s the {@code ObjectInputStream} to read
     * @throws ClassNotFoundException if the class of a serialized object could
     *         not be found
     * @throws IOException if an I/O error occurs
     * @since 1.4
     */
    @Serial
    private void readObject(ObjectInputStream s)
        throws ClassNotFoundException, IOException
    {
        ObjectInputStream.GetField f = s.readFields();

        DragGestureEvent newTrigger = (DragGestureEvent)f.get("trigger", null);
        if (newTrigger == null) {
            throw new InvalidObjectException("Null trigger");
        }
        if (newTrigger.getDragSource() == null) {
            throw new InvalidObjectException("Null DragSource");
        }
        if (newTrigger.getComponent() == null) {
            throw new InvalidObjectException("Null trigger component");
        }

        int newSourceActions = f.get("sourceActions", 0)
                & (DnDConstants.ACTION_COPY_OR_MOVE | DnDConstants.ACTION_LINK);
        if (newSourceActions == DnDConstants.ACTION_NONE) {
            throw new InvalidObjectException("Invalid source actions");
        }
        int triggerActions = newTrigger.getDragAction();
        if (triggerActions != DnDConstants.ACTION_COPY &&
                triggerActions != DnDConstants.ACTION_MOVE &&
                triggerActions != DnDConstants.ACTION_LINK) {
            throw new InvalidObjectException("No drag action");
        }
        trigger = newTrigger;

        cursor = (Cursor)f.get("cursor", null);
        useCustomCursor = f.get("useCustomCursor", false);
        sourceActions = newSourceActions;

        transferable = (Transferable)s.readObject();
        listener = (DragSourceListener)s.readObject();

        // Implementation assumes 'transferable' is never null.
        if (transferable == null) {
            if (emptyTransferable == null) {
                emptyTransferable = new Transferable() {
                        public DataFlavor[] getTransferDataFlavors() {
                            return new DataFlavor[0];
                        }
                        public boolean isDataFlavorSupported(DataFlavor flavor)
                        {
                            return false;
                        }
                        public Object getTransferData(DataFlavor flavor)
                            throws UnsupportedFlavorException
                        {
                            throw new UnsupportedFlavorException(flavor);
                        }
                    };
            }
            transferable = emptyTransferable;
        }
    }

    private static Transferable emptyTransferable;

    /*
     * fields
     */
    private final transient DragSourceContextPeer peer;

    /**
     * The event which triggered the start of the drag.
     *
     * @serial
     */
    private DragGestureEvent    trigger;

    /**
     * The current drag cursor.
     *
     * @serial
     */
    private Cursor              cursor;

    private transient Transferable      transferable;

    private transient DragSourceListener    listener;

    /**
     * {@code true} if the custom drag cursor is used instead of the
     * default one.
     *
     * @serial
     */
    private boolean useCustomCursor;

    /**
     * A bitwise mask of {@code DnDConstants} that represents the set of
     * drop actions supported by the drag source for the drag operation associated
     * with this {@code DragSourceContext.}
     *
     * @serial
     */
    private int sourceActions;
}
