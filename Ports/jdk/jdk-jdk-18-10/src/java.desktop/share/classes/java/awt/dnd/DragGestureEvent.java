/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Collections;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;

/**
 * A {@code DragGestureEvent} is passed
 * to {@code DragGestureListener}'s
 * dragGestureRecognized() method
 * when a particular {@code DragGestureRecognizer} detects that a
 * platform dependent drag initiating gesture has occurred
 * on the {@code Component} that it is tracking.
 *
 * The {@code action} field of any {@code DragGestureEvent} instance should take one of the following
 * values:
 * <ul>
 * <li> {@code DnDConstants.ACTION_COPY}
 * <li> {@code DnDConstants.ACTION_MOVE}
 * <li> {@code DnDConstants.ACTION_LINK}
 * </ul>
 * Assigning the value different from listed above will cause an unspecified behavior.
 *
 * @see java.awt.dnd.DragGestureRecognizer
 * @see java.awt.dnd.DragGestureListener
 * @see java.awt.dnd.DragSource
 * @see java.awt.dnd.DnDConstants
 */

public class DragGestureEvent extends EventObject {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 9080172649166731306L;

    /**
     * Constructs a {@code DragGestureEvent} object given by the
     * {@code DragGestureRecognizer} instance firing this event,
     * an {@code act} parameter representing
     * the user's preferred action, an {@code ori} parameter
     * indicating the origin of the drag, and a {@code List} of
     * events that comprise the gesture({@code evs} parameter).
     *
     * @param dgr The {@code DragGestureRecognizer} firing this event
     * @param act The user's preferred action.
     *            For information on allowable values, see
     *            the class description for {@link DragGestureEvent}
     * @param ori The origin of the drag
     * @param evs The {@code List} of events that comprise the gesture
     *
     * @throws IllegalArgumentException if any parameter equals {@code null}
     * @throws IllegalArgumentException if the act parameter does not comply with
     *                                  the values given in the class
     *                                  description for {@link DragGestureEvent}
     * @see java.awt.dnd.DnDConstants
     */

    public DragGestureEvent(DragGestureRecognizer dgr, int act, Point ori,
                            List<? extends InputEvent> evs)
    {
        super(dgr);

        if ((component = dgr.getComponent()) == null)
            throw new IllegalArgumentException("null component");
        if ((dragSource = dgr.getDragSource()) == null)
            throw new IllegalArgumentException("null DragSource");

        if (evs == null || evs.isEmpty())
            throw new IllegalArgumentException("null or empty list of events");

        if (act != DnDConstants.ACTION_COPY &&
            act != DnDConstants.ACTION_MOVE &&
            act != DnDConstants.ACTION_LINK)
            throw new IllegalArgumentException("bad action");

        if (ori == null) throw new IllegalArgumentException("null origin");

        events     = evs;
        action     = act;
        origin     = ori;
    }

    /**
     * Returns the source as a {@code DragGestureRecognizer}.
     *
     * @return the source as a {@code DragGestureRecognizer}
     */

    public DragGestureRecognizer getSourceAsDragGestureRecognizer() {
        return (DragGestureRecognizer)getSource();
    }

    /**
     * Returns the {@code Component} associated
     * with this {@code DragGestureEvent}.
     *
     * @return the Component
     */

    public Component getComponent() { return component; }

    /**
     * Returns the {@code DragSource}.
     *
     * @return the {@code DragSource}
     */

    public DragSource getDragSource() { return dragSource; }

    /**
     * Returns a {@code Point} in the coordinates
     * of the {@code Component} over which the drag originated.
     *
     * @return the Point where the drag originated in Component coords.
     */

    public Point getDragOrigin() {
        return origin;
    }

    /**
     * Returns an {@code Iterator} for the events
     * comprising the gesture.
     *
     * @return an Iterator for the events comprising the gesture
     */
    @SuppressWarnings("unchecked")
    public Iterator<InputEvent> iterator() { return events.iterator(); }

    /**
     * Returns an {@code Object} array of the
     * events comprising the drag gesture.
     *
     * @return an array of the events comprising the gesture
     */

    public Object[] toArray() { return events.toArray(); }

    /**
     * Returns an array of the events comprising the drag gesture.
     *
     * @param array the array of {@code EventObject} sub(types)
     *
     * @return an array of the events comprising the gesture
     */
    @SuppressWarnings("unchecked")
    public Object[] toArray(Object[] array) { return events.toArray(array); }

    /**
     * Returns an {@code int} representing the
     * action selected by the user.
     *
     * @return the action selected by the user
     */

    public int getDragAction() { return action; }

    /**
     * Returns the initial event that triggered the gesture.
     *
     * @return the first "triggering" event in the sequence of the gesture
     */

    public InputEvent getTriggerEvent() {
        return getSourceAsDragGestureRecognizer().getTriggerEvent();
    }

    /**
     * Starts the drag operation given the {@code Cursor} for this drag
     * operation and the {@code Transferable} representing the source data
     * for this drag operation.
     * <br>
     * If a {@code null Cursor} is specified no exception will
     * be thrown and default drag cursors will be used instead.
     * <br>
     * If a {@code null Transferable} is specified
     * {@code NullPointerException} will be thrown.
     * @param dragCursor     The initial {@code Cursor} for this drag operation
     *                       or {@code null} for the default cursor handling;
     *                       see
     *                       <a href="DragSourceContext.html#defaultCursor">DragSourceContext</a>
     *                       for more details on the cursor handling mechanism
     *                       during drag and drop
     * @param transferable The {@code Transferable} representing the source
     *                     data for this drag operation.
     *
     * @throws InvalidDnDOperationException if the Drag and Drop
     *         system is unable to initiate a drag operation, or if the user
     *         attempts to start a drag while an existing drag operation is
     *         still executing.
     * @throws NullPointerException if the {@code Transferable} is {@code null}
     * @since 1.4
     */
    public void startDrag(Cursor dragCursor, Transferable transferable)
      throws InvalidDnDOperationException {
        dragSource.startDrag(this, dragCursor, transferable, null);
    }

    /**
     * Starts the drag given the initial {@code Cursor} to display,
     * the {@code Transferable} object,
     * and the {@code DragSourceListener} to use.
     *
     * @param dragCursor     The initial {@code Cursor} for this drag operation
     *                       or {@code null} for the default cursor handling;
     *                       see
     *                       <a href="DragSourceContext.html#defaultCursor">DragSourceContext</a>
     *                       for more details on the cursor handling mechanism
     *                       during drag and drop
     * @param transferable The source's Transferable
     * @param dsl          The source's DragSourceListener
     *
     * @throws InvalidDnDOperationException if
     * the Drag and Drop system is unable to
     * initiate a drag operation, or if the user
     * attempts to start a drag while an existing
     * drag operation is still executing.
     */

    public void startDrag(Cursor dragCursor, Transferable transferable, DragSourceListener dsl) throws InvalidDnDOperationException {
        dragSource.startDrag(this, dragCursor, transferable, dsl);
    }

    /**
     * Start the drag given the initial {@code Cursor} to display,
     * a drag {@code Image}, the offset of
     * the {@code Image},
     * the {@code Transferable} object, and
     * the {@code DragSourceListener} to use.
     *
     * @param dragCursor     The initial {@code Cursor} for this drag operation
     *                       or {@code null} for the default cursor handling;
     *                       see
     *                       <a href="DragSourceContext.html#defaultCursor">DragSourceContext</a>
     *                       for more details on the cursor handling mechanism
     *                       during drag and drop
     * @param dragImage    The source's dragImage
     * @param imageOffset  The dragImage's offset
     * @param transferable The source's Transferable
     * @param dsl          The source's DragSourceListener
     *
     * @throws InvalidDnDOperationException if
     * the Drag and Drop system is unable to
     * initiate a drag operation, or if the user
     * attempts to start a drag while an existing
     * drag operation is still executing.
     */

    public void startDrag(Cursor dragCursor, Image dragImage, Point imageOffset, Transferable transferable, DragSourceListener dsl) throws InvalidDnDOperationException {
        dragSource.startDrag(this,  dragCursor, dragImage, imageOffset, transferable, dsl);
    }

    /**
     * Serializes this {@code DragGestureEvent}. Performs default
     * serialization and then writes out this object's {@code List} of
     * gesture events if and only if the {@code List} can be serialized.
     * If not, {@code null} is written instead. In this case, a
     * {@code DragGestureEvent} created from the resulting deserialized
     * stream will contain an empty {@code List} of gesture events.
     *
     * @param  s the {@code ObjectOutputStream} to write
     * @throws IOException if an I/O error occurs
     * @serialData The default serializable fields, in alphabetical order,
     *             followed by either a {@code List} instance, or
     *             {@code null}.
     * @since 1.4
     */
    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();

        s.writeObject(SerializationTester.test(events) ? events : null);
    }

    /**
     * Deserializes this {@code DragGestureEvent}. This method first
     * performs default deserialization for all non-{@code transient}
     * fields. An attempt is then made to deserialize this object's
     * {@code List} of gesture events as well. This is first attempted
     * by deserializing the field {@code events}, because, in releases
     * prior to 1.4, a non-{@code transient} field of this name stored the
     * {@code List} of gesture events. If this fails, the next object in
     * the stream is used instead. If the resulting {@code List} is
     * {@code null}, this object's {@code List} of gesture events
     * is set to an empty {@code List}.
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

        DragSource newDragSource = (DragSource)f.get("dragSource", null);
        if (newDragSource == null) {
            throw new InvalidObjectException("null DragSource");
        }
        dragSource = newDragSource;

        Component newComponent = (Component)f.get("component", null);
        if (newComponent == null) {
            throw new InvalidObjectException("null component");
        }
        component = newComponent;

        Point newOrigin = (Point)f.get("origin", null);
        if (newOrigin == null) {
            throw new InvalidObjectException("null origin");
        }
        origin = newOrigin;

        int newAction = f.get("action", 0);
        if (newAction != DnDConstants.ACTION_COPY &&
                newAction != DnDConstants.ACTION_MOVE &&
                newAction != DnDConstants.ACTION_LINK) {
            throw new InvalidObjectException("bad action");
        }
        action = newAction;

        // Pre-1.4 support. 'events' was previously non-transient
        @SuppressWarnings("rawtypes")
        List newEvents;
        try {
            newEvents = (List)f.get("events", null);
        } catch (IllegalArgumentException e) {
            // 1.4-compatible byte stream. 'events' was written explicitly
            newEvents = (List)s.readObject();
        }

        // Implementation assumes 'events' is never null.
        if (newEvents != null && newEvents.isEmpty()) {
            // Constructor treats empty events list as invalid value
            // Throw exception if serialized list is empty
            throw new InvalidObjectException("empty list of events");
        } else if (newEvents == null) {
            newEvents = Collections.emptyList();
        }
        events = newEvents;
    }

    /*
     * fields
     */
    @SuppressWarnings("rawtypes")
    private transient List events;

    /**
     * The DragSource associated with this DragGestureEvent.
     *
     * @serial
     */
    private DragSource dragSource;

    /**
     * The Component associated with this DragGestureEvent.
     *
     * @serial
     */
    private Component  component;

    /**
     * The origin of the drag.
     *
     * @serial
     */
    private Point      origin;

    /**
     * The user's preferred action.
     *
     * @serial
     */
    private int        action;
}
