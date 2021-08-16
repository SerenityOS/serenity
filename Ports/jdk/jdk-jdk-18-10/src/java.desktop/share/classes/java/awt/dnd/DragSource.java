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

import java.awt.Component;
import java.awt.Cursor;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.FlavorMap;
import java.awt.datatransfer.SystemFlavorMap;
import java.awt.datatransfer.Transferable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.security.AccessController;
import java.util.EventListener;

import sun.awt.AWTAccessor;
import sun.awt.AWTAccessor.DragSourceContextAccessor;
import sun.awt.dnd.SunDragSourceContextPeer;
import sun.security.action.GetIntegerAction;

/**
 * The {@code DragSource} is the entity responsible
 * for the initiation of the Drag
 * and Drop operation, and may be used in a number of scenarios:
 * <UL>
 * <LI>1 default instance per JVM for the lifetime of that JVM.
 * <LI>1 instance per class of potential Drag Initiator object (e.g
 * TextField). [implementation dependent]
 * <LI>1 per instance of a particular
 * {@code Component}, or application specific
 * object associated with a {@code Component}
 * instance in the GUI. [implementation dependent]
 * <LI>Some other arbitrary association. [implementation dependent]
 *</UL>
 *
 * Once the {@code DragSource} is
 * obtained, a {@code DragGestureRecognizer} should
 * also be obtained to associate the {@code DragSource}
 * with a particular
 * {@code Component}.
 * <P>
 * The initial interpretation of the user's gesture,
 * and the subsequent starting of the drag operation
 * are the responsibility of the implementing
 * {@code Component}, which is usually
 * implemented by a {@code DragGestureRecognizer}.
 *<P>
 * When a drag gesture occurs, the
 * {@code DragSource}'s
 * startDrag() method shall be
 * invoked in order to cause processing
 * of the user's navigational
 * gestures and delivery of Drag and Drop
 * protocol notifications. A
 * {@code DragSource} shall only
 * permit a single Drag and Drop operation to be
 * current at any one time, and shall
 * reject any further startDrag() requests
 * by throwing an {@code IllegalDnDOperationException}
 * until such time as the extant operation is complete.
 * <P>
 * The startDrag() method invokes the
 * createDragSourceContext() method to
 * instantiate an appropriate
 * {@code DragSourceContext}
 * and associate the {@code DragSourceContextPeer}
 * with that.
 * <P>
 * If the Drag and Drop System is
 * unable to initiate a drag operation for
 * some reason, the startDrag() method throws
 * a {@code java.awt.dnd.InvalidDnDOperationException}
 * to signal such a condition. Typically this
 * exception is thrown when the underlying platform
 * system is either not in a state to
 * initiate a drag, or the parameters specified are invalid.
 * <P>
 * Note that during the drag, the
 * set of operations exposed by the source
 * at the start of the drag operation may not change
 * until the operation is complete.
 * The operation(s) are constant for the
 * duration of the operation with respect to the
 * {@code DragSource}.
 *
 * @since 1.2
 */

public class DragSource implements Serializable {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 6236096958971414066L;

    /*
     * load a system default cursor
     */

    private static Cursor load(String name) {
        if (GraphicsEnvironment.isHeadless()) {
            return null;
        }

        try {
            return (Cursor)Toolkit.getDefaultToolkit().getDesktopProperty(name);
        } catch (Exception e) {
            e.printStackTrace();

            throw new RuntimeException("failed to load system cursor: " + name + " : " + e.getMessage());
        }
    }


    /**
     * The default {@code Cursor} to use with a copy operation indicating
     * that a drop is currently allowed. {@code null} if
     * {@code GraphicsEnvironment.isHeadless()} returns {@code true}.
     *
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public static final Cursor DefaultCopyDrop =
        load("DnD.Cursor.CopyDrop");

    /**
     * The default {@code Cursor} to use with a move operation indicating
     * that a drop is currently allowed. {@code null} if
     * {@code GraphicsEnvironment.isHeadless()} returns {@code true}.
     *
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public static final Cursor DefaultMoveDrop =
        load("DnD.Cursor.MoveDrop");

    /**
     * The default {@code Cursor} to use with a link operation indicating
     * that a drop is currently allowed. {@code null} if
     * {@code GraphicsEnvironment.isHeadless()} returns {@code true}.
     *
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public static final Cursor DefaultLinkDrop =
        load("DnD.Cursor.LinkDrop");

    /**
     * The default {@code Cursor} to use with a copy operation indicating
     * that a drop is currently not allowed. {@code null} if
     * {@code GraphicsEnvironment.isHeadless()} returns {@code true}.
     *
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public static final Cursor DefaultCopyNoDrop =
        load("DnD.Cursor.CopyNoDrop");

    /**
     * The default {@code Cursor} to use with a move operation indicating
     * that a drop is currently not allowed. {@code null} if
     * {@code GraphicsEnvironment.isHeadless()} returns {@code true}.
     *
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public static final Cursor DefaultMoveNoDrop =
        load("DnD.Cursor.MoveNoDrop");

    /**
     * The default {@code Cursor} to use with a link operation indicating
     * that a drop is currently not allowed. {@code null} if
     * {@code GraphicsEnvironment.isHeadless()} returns {@code true}.
     *
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public static final Cursor DefaultLinkNoDrop =
        load("DnD.Cursor.LinkNoDrop");

    private static final DragSource dflt =
        (GraphicsEnvironment.isHeadless()) ? null : new DragSource();

    /**
     * Internal constants for serialization.
     */
    static final String dragSourceListenerK = "dragSourceL";
    static final String dragSourceMotionListenerK = "dragSourceMotionL";

    /**
     * Gets the {@code DragSource} object associated with
     * the underlying platform.
     *
     * @return the platform DragSource
     * @exception HeadlessException if GraphicsEnvironment.isHeadless()
     *            returns true
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public static DragSource getDefaultDragSource() {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        } else {
            return dflt;
        }
    }

    /**
     * Reports
     * whether or not drag
     * {@code Image} support
     * is available on the underlying platform.
     *
     * @return if the Drag Image support is available on this platform
     */

    public static boolean isDragImageSupported() {
        Toolkit t = Toolkit.getDefaultToolkit();

        Boolean supported;

        try {
            supported = (Boolean)Toolkit.getDefaultToolkit().getDesktopProperty("DnD.isDragImageSupported");

            return supported.booleanValue();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Creates a new {@code DragSource}.
     *
     * @exception HeadlessException if GraphicsEnvironment.isHeadless()
     *            returns true
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public DragSource() throws HeadlessException {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }
    }

    /**
     * Start a drag, given the {@code DragGestureEvent}
     * that initiated the drag, the initial
     * {@code Cursor} to use,
     * the {@code Image} to drag,
     * the offset of the {@code Image} origin
     * from the hotspot of the {@code Cursor} at
     * the instant of the trigger,
     * the {@code Transferable} subject data
     * of the drag, the {@code DragSourceListener},
     * and the {@code FlavorMap}.
     *
     * @param trigger        the {@code DragGestureEvent} that initiated the drag
     * @param dragCursor     the initial {@code Cursor} for this drag operation
     *                       or {@code null} for the default cursor handling;
     *                       see <a href="DragSourceContext.html#defaultCursor">DragSourceContext</a>
     *                       for more details on the cursor handling mechanism during drag and drop
     * @param dragImage      the image to drag or {@code null}
     * @param imageOffset    the offset of the {@code Image} origin from the hotspot
     *                       of the {@code Cursor} at the instant of the trigger
     * @param transferable   the subject data of the drag
     * @param dsl            the {@code DragSourceListener}
     * @param flavorMap      the {@code FlavorMap} to use, or {@code null}
     *
     * @throws java.awt.dnd.InvalidDnDOperationException
     *    if the Drag and Drop
     *    system is unable to initiate a drag operation, or if the user
     *    attempts to start a drag while an existing drag operation
     *    is still executing
     */

    public void startDrag(DragGestureEvent   trigger,
                          Cursor             dragCursor,
                          Image              dragImage,
                          Point              imageOffset,
                          Transferable       transferable,
                          DragSourceListener dsl,
                          FlavorMap          flavorMap) throws InvalidDnDOperationException {

        SunDragSourceContextPeer.setDragDropInProgress(true);

        try {
            if (flavorMap != null) this.flavorMap = flavorMap;

            DragSourceContext dsc = createDragSourceContext(trigger, dragCursor,
                                                            dragImage,
                                                            imageOffset,
                                                            transferable, dsl);

            if (dsc == null) {
                throw new InvalidDnDOperationException();
            }
            DragSourceContextAccessor acc = AWTAccessor.getDragSourceContextAccessor();
            acc.getPeer(dsc).startDrag(dsc, dsc.getCursor(), dragImage, imageOffset); // may throw
        } catch (RuntimeException e) {
            SunDragSourceContextPeer.setDragDropInProgress(false);
            throw e;
        }
    }

    /**
     * Start a drag, given the {@code DragGestureEvent}
     * that initiated the drag, the initial
     * {@code Cursor} to use,
     * the {@code Transferable} subject data
     * of the drag, the {@code DragSourceListener},
     * and the {@code FlavorMap}.
     *
     * @param trigger        the {@code DragGestureEvent} that
     * initiated the drag
     * @param dragCursor     the initial {@code Cursor} for this drag operation
     *                       or {@code null} for the default cursor handling;
     *                       see <a href="DragSourceContext.html#defaultCursor">DragSourceContext</a>
     *                       for more details on the cursor handling mechanism during drag and drop
     * @param transferable   the subject data of the drag
     * @param dsl            the {@code DragSourceListener}
     * @param flavorMap      the {@code FlavorMap} to use or {@code null}
     *
     * @throws java.awt.dnd.InvalidDnDOperationException
     *    if the Drag and Drop
     *    system is unable to initiate a drag operation, or if the user
     *    attempts to start a drag while an existing drag operation
     *    is still executing
     */

    public void startDrag(DragGestureEvent   trigger,
                          Cursor             dragCursor,
                          Transferable       transferable,
                          DragSourceListener dsl,
                          FlavorMap          flavorMap) throws InvalidDnDOperationException {
        startDrag(trigger, dragCursor, null, null, transferable, dsl, flavorMap);
    }

    /**
     * Start a drag, given the {@code DragGestureEvent}
     * that initiated the drag, the initial {@code Cursor}
     * to use,
     * the {@code Image} to drag,
     * the offset of the {@code Image} origin
     * from the hotspot of the {@code Cursor}
     * at the instant of the trigger,
     * the subject data of the drag, and
     * the {@code DragSourceListener}.
     *
     * @param trigger           the {@code DragGestureEvent} that initiated the drag
     * @param dragCursor     the initial {@code Cursor} for this drag operation
     *                       or {@code null} for the default cursor handling;
     *                       see <a href="DragSourceContext.html#defaultCursor">DragSourceContext</a>
     *                       for more details on the cursor handling mechanism during drag and drop
     * @param dragImage         the {@code Image} to drag or {@code null}
     * @param dragOffset        the offset of the {@code Image} origin from the hotspot
     *                          of the {@code Cursor} at the instant of the trigger
     * @param transferable      the subject data of the drag
     * @param dsl               the {@code DragSourceListener}
     *
     * @throws java.awt.dnd.InvalidDnDOperationException
     *    if the Drag and Drop
     *    system is unable to initiate a drag operation, or if the user
     *    attempts to start a drag while an existing drag operation
     *    is still executing
     */

    public void startDrag(DragGestureEvent   trigger,
                          Cursor             dragCursor,
                          Image              dragImage,
                          Point              dragOffset,
                          Transferable       transferable,
                          DragSourceListener dsl) throws InvalidDnDOperationException {
        startDrag(trigger, dragCursor, dragImage, dragOffset, transferable, dsl, null);
    }

    /**
     * Start a drag, given the {@code DragGestureEvent}
     * that initiated the drag, the initial
     * {@code Cursor} to
     * use,
     * the {@code Transferable} subject data
     * of the drag, and the {@code DragSourceListener}.
     *
     * @param trigger        the {@code DragGestureEvent} that initiated the drag
     * @param dragCursor     the initial {@code Cursor} for this drag operation
     *                       or {@code null} for the default cursor handling;
     *                       see <a href="DragSourceContext.html#defaultCursor">DragSourceContext</a> class
     *                       for more details on the cursor handling mechanism during drag and drop
     * @param transferable      the subject data of the drag
     * @param dsl               the {@code DragSourceListener}
     *
     * @throws java.awt.dnd.InvalidDnDOperationException
     *    if the Drag and Drop
     *    system is unable to initiate a drag operation, or if the user
     *    attempts to start a drag while an existing drag operation
     *    is still executing
     */

    public void startDrag(DragGestureEvent   trigger,
                          Cursor             dragCursor,
                          Transferable       transferable,
                          DragSourceListener dsl) throws InvalidDnDOperationException {
        startDrag(trigger, dragCursor, null, null, transferable, dsl, null);
    }

    /**
     * Creates the {@code DragSourceContext} to handle the current drag
     * operation.
     * <p>
     * To incorporate a new {@code DragSourceContext}
     * subclass, subclass {@code DragSource} and
     * override this method.
     * <p>
     * If {@code dragImage} is {@code null}, no image is used
     * to represent the drag over feedback for this drag operation, but
     * {@code NullPointerException} is not thrown.
     * <p>
     * If {@code dsl} is {@code null}, no drag source listener
     * is registered with the created {@code DragSourceContext},
     * but {@code NullPointerException} is not thrown.
     *
     * @param dgl           The {@code DragGestureEvent} that triggered the
     *                      drag
     * @param dragCursor     The initial {@code Cursor} for this drag operation
     *                       or {@code null} for the default cursor handling;
     *                       see <a href="DragSourceContext.html#defaultCursor">DragSourceContext</a> class
     *                       for more details on the cursor handling mechanism during drag and drop
     * @param dragImage     The {@code Image} to drag or {@code null}
     * @param imageOffset   The offset of the {@code Image} origin from the
     *                      hotspot of the cursor at the instant of the trigger
     * @param t             The subject data of the drag
     * @param dsl           The {@code DragSourceListener}
     *
     * @return the {@code DragSourceContext}
     *
     * @throws NullPointerException if {@code dscp} is {@code null}
     * @throws NullPointerException if {@code dgl} is {@code null}
     * @throws NullPointerException if {@code dragImage} is not
     *    {@code null} and {@code imageOffset} is {@code null}
     * @throws NullPointerException if {@code t} is {@code null}
     * @throws IllegalArgumentException if the {@code Component}
     *         associated with the trigger event is {@code null}.
     * @throws IllegalArgumentException if the {@code DragSource} for the
     *         trigger event is {@code null}.
     * @throws IllegalArgumentException if the drag action for the
     *         trigger event is {@code DnDConstants.ACTION_NONE}.
     * @throws IllegalArgumentException if the source actions for the
     *         {@code DragGestureRecognizer} associated with the trigger
     *         event are equal to {@code DnDConstants.ACTION_NONE}.
     */

    protected DragSourceContext createDragSourceContext(DragGestureEvent dgl,
                                                        Cursor dragCursor,
                                                        Image dragImage,
                                                        Point imageOffset,
                                                        Transferable t,
                                                        DragSourceListener dsl) {
        return new DragSourceContext(dgl, dragCursor, dragImage, imageOffset, t, dsl);
    }

    /**
     * This method returns the
     * {@code FlavorMap} for this {@code DragSource}.
     *
     * @return the {@code FlavorMap} for this {@code DragSource}
     */

    public FlavorMap getFlavorMap() { return flavorMap; }

    /**
     * Creates a new {@code DragGestureRecognizer}
     * that implements the specified
     * abstract subclass of
     * {@code DragGestureRecognizer}, and
     * sets the specified {@code Component}
     * and {@code DragGestureListener} on
     * the newly created object.
     *
     * @param <T> the type of {@code DragGestureRecognizer} to create
     * @param recognizerAbstractClass the requested abstract type
     * @param actions                 the permitted source drag actions
     * @param c                       the {@code Component} target
     * @param dgl        the {@code DragGestureListener} to notify
     *
     * @return the new {@code DragGestureRecognizer} or {@code null}
     *    if the {@code Toolkit.createDragGestureRecognizer} method
     *    has no implementation available for
     *    the requested {@code DragGestureRecognizer}
     *    subclass and returns {@code null}
     */

    public <T extends DragGestureRecognizer> T
        createDragGestureRecognizer(Class<T> recognizerAbstractClass,
                                    Component c, int actions,
                                    DragGestureListener dgl)
    {
        return Toolkit.getDefaultToolkit().createDragGestureRecognizer(recognizerAbstractClass, this, c, actions, dgl);
    }


    /**
     * Creates a new {@code DragGestureRecognizer}
     * that implements the default
     * abstract subclass of {@code DragGestureRecognizer}
     * for this {@code DragSource},
     * and sets the specified {@code Component}
     * and {@code DragGestureListener} on the
     * newly created object.
     *
     * For this {@code DragSource}
     * the default is {@code MouseDragGestureRecognizer}.
     *
     * @param c       the {@code Component} target for the recognizer
     * @param actions the permitted source actions
     * @param dgl     the {@code DragGestureListener} to notify
     *
     * @return the new {@code DragGestureRecognizer} or {@code null}
     *    if the {@code Toolkit.createDragGestureRecognizer} method
     *    has no implementation available for
     *    the requested {@code DragGestureRecognizer}
     *    subclass and returns {@code null}
     */

    public DragGestureRecognizer createDefaultDragGestureRecognizer(Component c, int actions, DragGestureListener dgl) {
        return Toolkit.getDefaultToolkit().createDragGestureRecognizer(MouseDragGestureRecognizer.class, this, c, actions, dgl);
    }

    /**
     * Adds the specified {@code DragSourceListener} to this
     * {@code DragSource} to receive drag source events during drag
     * operations initiated with this {@code DragSource}.
     * If a {@code null} listener is specified, no action is taken and no
     * exception is thrown.
     *
     * @param dsl the {@code DragSourceListener} to add
     *
     * @see      #removeDragSourceListener
     * @see      #getDragSourceListeners
     * @since 1.4
     */
    public void addDragSourceListener(DragSourceListener dsl) {
        if (dsl != null) {
            synchronized (this) {
                listener = DnDEventMulticaster.add(listener, dsl);
            }
        }
    }

    /**
     * Removes the specified {@code DragSourceListener} from this
     * {@code DragSource}.
     * If a {@code null} listener is specified, no action is taken and no
     * exception is thrown.
     * If the listener specified by the argument was not previously added to
     * this {@code DragSource}, no action is taken and no exception
     * is thrown.
     *
     * @param dsl the {@code DragSourceListener} to remove
     *
     * @see      #addDragSourceListener
     * @see      #getDragSourceListeners
     * @since 1.4
     */
    public void removeDragSourceListener(DragSourceListener dsl) {
        if (dsl != null) {
            synchronized (this) {
                listener = DnDEventMulticaster.remove(listener, dsl);
            }
        }
    }

    /**
     * Gets all the {@code DragSourceListener}s
     * registered with this {@code DragSource}.
     *
     * @return all of this {@code DragSource}'s
     *         {@code DragSourceListener}s or an empty array if no
     *         such listeners are currently registered
     *
     * @see      #addDragSourceListener
     * @see      #removeDragSourceListener
     * @since    1.4
     */
    public DragSourceListener[] getDragSourceListeners() {
        return getListeners(DragSourceListener.class);
    }

    /**
     * Adds the specified {@code DragSourceMotionListener} to this
     * {@code DragSource} to receive drag motion events during drag
     * operations initiated with this {@code DragSource}.
     * If a {@code null} listener is specified, no action is taken and no
     * exception is thrown.
     *
     * @param dsml the {@code DragSourceMotionListener} to add
     *
     * @see      #removeDragSourceMotionListener
     * @see      #getDragSourceMotionListeners
     * @since 1.4
     */
    public void addDragSourceMotionListener(DragSourceMotionListener dsml) {
        if (dsml != null) {
            synchronized (this) {
                motionListener = DnDEventMulticaster.add(motionListener, dsml);
            }
        }
    }

    /**
     * Removes the specified {@code DragSourceMotionListener} from this
     * {@code DragSource}.
     * If a {@code null} listener is specified, no action is taken and no
     * exception is thrown.
     * If the listener specified by the argument was not previously added to
     * this {@code DragSource}, no action is taken and no exception
     * is thrown.
     *
     * @param dsml the {@code DragSourceMotionListener} to remove
     *
     * @see      #addDragSourceMotionListener
     * @see      #getDragSourceMotionListeners
     * @since 1.4
     */
    public void removeDragSourceMotionListener(DragSourceMotionListener dsml) {
        if (dsml != null) {
            synchronized (this) {
                motionListener = DnDEventMulticaster.remove(motionListener, dsml);
            }
        }
    }

    /**
     * Gets all of the  {@code DragSourceMotionListener}s
     * registered with this {@code DragSource}.
     *
     * @return all of this {@code DragSource}'s
     *         {@code DragSourceMotionListener}s or an empty array if no
     *         such listeners are currently registered
     *
     * @see      #addDragSourceMotionListener
     * @see      #removeDragSourceMotionListener
     * @since    1.4
     */
    public DragSourceMotionListener[] getDragSourceMotionListeners() {
        return getListeners(DragSourceMotionListener.class);
    }

    /**
     * Gets all the objects currently registered as
     * <code><em>Foo</em>Listener</code>s upon this {@code DragSource}.
     * <code><em>Foo</em>Listener</code>s are registered using the
     * <code>add<em>Foo</em>Listener</code> method.
     *
     * @param <T> the type of listener objects
     * @param listenerType the type of listeners requested; this parameter
     *          should specify an interface that descends from
     *          {@code java.util.EventListener}
     * @return an array of all objects registered as
     *          <code><em>Foo</em>Listener</code>s on this
     *          {@code DragSource}, or an empty array if no such listeners
     *          have been added
     * @exception ClassCastException if {@code listenerType}
     *          doesn't specify a class or interface that implements
     *          {@code java.util.EventListener}
     *
     * @see #getDragSourceListeners
     * @see #getDragSourceMotionListeners
     * @since 1.4
     */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        EventListener l = null;
        if (listenerType == DragSourceListener.class) {
            l = listener;
        } else if (listenerType == DragSourceMotionListener.class) {
            l = motionListener;
        }
        return DnDEventMulticaster.getListeners(l, listenerType);
    }

    /**
     * This method calls {@code dragEnter} on the
     * {@code DragSourceListener}s registered with this
     * {@code DragSource}, and passes them the specified
     * {@code DragSourceDragEvent}.
     *
     * @param dsde the {@code DragSourceDragEvent}
     */
    void processDragEnter(DragSourceDragEvent dsde) {
        DragSourceListener dsl = listener;
        if (dsl != null) {
            dsl.dragEnter(dsde);
        }
    }

    /**
     * This method calls {@code dragOver} on the
     * {@code DragSourceListener}s registered with this
     * {@code DragSource}, and passes them the specified
     * {@code DragSourceDragEvent}.
     *
     * @param dsde the {@code DragSourceDragEvent}
     */
    void processDragOver(DragSourceDragEvent dsde) {
        DragSourceListener dsl = listener;
        if (dsl != null) {
            dsl.dragOver(dsde);
        }
    }

    /**
     * This method calls {@code dropActionChanged} on the
     * {@code DragSourceListener}s registered with this
     * {@code DragSource}, and passes them the specified
     * {@code DragSourceDragEvent}.
     *
     * @param dsde the {@code DragSourceDragEvent}
     */
    void processDropActionChanged(DragSourceDragEvent dsde) {
        DragSourceListener dsl = listener;
        if (dsl != null) {
            dsl.dropActionChanged(dsde);
        }
    }

    /**
     * This method calls {@code dragExit} on the
     * {@code DragSourceListener}s registered with this
     * {@code DragSource}, and passes them the specified
     * {@code DragSourceEvent}.
     *
     * @param dse the {@code DragSourceEvent}
     */
    void processDragExit(DragSourceEvent dse) {
        DragSourceListener dsl = listener;
        if (dsl != null) {
            dsl.dragExit(dse);
        }
    }

    /**
     * This method calls {@code dragDropEnd} on the
     * {@code DragSourceListener}s registered with this
     * {@code DragSource}, and passes them the specified
     * {@code DragSourceDropEvent}.
     *
     * @param dsde the {@code DragSourceEvent}
     */
    void processDragDropEnd(DragSourceDropEvent dsde) {
        DragSourceListener dsl = listener;
        if (dsl != null) {
            dsl.dragDropEnd(dsde);
        }
    }

    /**
     * This method calls {@code dragMouseMoved} on the
     * {@code DragSourceMotionListener}s registered with this
     * {@code DragSource}, and passes them the specified
     * {@code DragSourceDragEvent}.
     *
     * @param dsde the {@code DragSourceEvent}
     */
    void processDragMouseMoved(DragSourceDragEvent dsde) {
        DragSourceMotionListener dsml = motionListener;
        if (dsml != null) {
            dsml.dragMouseMoved(dsde);
        }
    }

    /**
     * Serializes this {@code DragSource}. This method first performs
     * default serialization. Next, it writes out this object's
     * {@code FlavorMap} if and only if it can be serialized. If not,
     * {@code null} is written instead. Next, it writes out
     * {@code Serializable} listeners registered with this
     * object. Listeners are written in a {@code null}-terminated sequence
     * of 0 or more pairs. The pair consists of a {@code String} and an
     * {@code Object}; the {@code String} indicates the type of the
     * {@code Object} and is one of the following:
     * <ul>
     * <li>{@code dragSourceListenerK} indicating a
     *     {@code DragSourceListener} object;
     * <li>{@code dragSourceMotionListenerK} indicating a
     *     {@code DragSourceMotionListener} object.
     * </ul>
     *
     * @param  s the {@code ObjectOutputStream} to write
     * @throws IOException if an I/O error occurs
     * @serialData Either a {@code FlavorMap} instance, or
     *      {@code null}, followed by a {@code null}-terminated
     *      sequence of 0 or more pairs; the pair consists of a
     *      {@code String} and an {@code Object}; the
     *      {@code String} indicates the type of the {@code Object}
     *      and is one of the following:
     *      <ul>
     *      <li>{@code dragSourceListenerK} indicating a
     *          {@code DragSourceListener} object;
     *      <li>{@code dragSourceMotionListenerK} indicating a
     *          {@code DragSourceMotionListener} object.
     *      </ul>.
     * @since 1.4
     */
    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();

        s.writeObject(SerializationTester.test(flavorMap) ? flavorMap : null);

        DnDEventMulticaster.save(s, dragSourceListenerK, listener);
        DnDEventMulticaster.save(s, dragSourceMotionListenerK, motionListener);
        s.writeObject(null);
    }

    /**
     * Deserializes this {@code DragSource}. This method first performs
     * default deserialization. Next, this object's {@code FlavorMap} is
     * deserialized by using the next object in the stream.
     * If the resulting {@code FlavorMap} is {@code null}, this
     * object's {@code FlavorMap} is set to the default FlavorMap for
     * this thread's {@code ClassLoader}.
     * Next, this object's listeners are deserialized by reading a
     * {@code null}-terminated sequence of 0 or more key/value pairs
     * from the stream:
     * <ul>
     * <li>If a key object is a {@code String} equal to
     * {@code dragSourceListenerK}, a {@code DragSourceListener} is
     * deserialized using the corresponding value object and added to this
     * {@code DragSource}.
     * <li>If a key object is a {@code String} equal to
     * {@code dragSourceMotionListenerK}, a
     * {@code DragSourceMotionListener} is deserialized using the
     * corresponding value object and added to this {@code DragSource}.
     * <li>Otherwise, the key/value pair is skipped.
     * </ul>
     *
     * @param  s the {@code ObjectInputStream} to read
     * @throws ClassNotFoundException if the class of a serialized object could
     *         not be found
     * @throws IOException if an I/O error occurs
     * @see java.awt.datatransfer.SystemFlavorMap#getDefaultFlavorMap
     * @since 1.4
     */
    @Serial
    private void readObject(ObjectInputStream s)
      throws ClassNotFoundException, IOException {
        s.defaultReadObject();

        // 'flavorMap' was written explicitly
        flavorMap = (FlavorMap)s.readObject();

        // Implementation assumes 'flavorMap' is never null.
        if (flavorMap == null) {
            flavorMap = SystemFlavorMap.getDefaultFlavorMap();
        }

        Object keyOrNull;
        while (null != (keyOrNull = s.readObject())) {
            String key = ((String)keyOrNull).intern();

            if (dragSourceListenerK == key) {
                addDragSourceListener((DragSourceListener)(s.readObject()));
            } else if (dragSourceMotionListenerK == key) {
                addDragSourceMotionListener(
                    (DragSourceMotionListener)(s.readObject()));
            } else {
                // skip value for unrecognized key
                s.readObject();
            }
        }
    }

    /**
     * Returns the drag gesture motion threshold. The drag gesture motion threshold
     * defines the recommended behavior for {@link MouseDragGestureRecognizer}s.
     * <p>
     * If the system property {@code awt.dnd.drag.threshold} is set to
     * a positive integer, this method returns the value of the system property;
     * otherwise if a pertinent desktop property is available and supported by
     * the implementation of the Java platform, this method returns the value of
     * that property; otherwise this method returns some default value.
     * The pertinent desktop property can be queried using
     * {@code java.awt.Toolkit.getDesktopProperty("DnD.gestureMotionThreshold")}.
     *
     * @return the drag gesture motion threshold
     * @see MouseDragGestureRecognizer
     * @since 1.5
     */
    public static int getDragThreshold() {
        @SuppressWarnings("removal")
        int ts = AccessController.doPrivileged(
                new GetIntegerAction("awt.dnd.drag.threshold", 0)).intValue();
        if (ts > 0) {
            return ts;
        } else {
            Integer td = (Integer)Toolkit.getDefaultToolkit().
                    getDesktopProperty("DnD.gestureMotionThreshold");
            if (td != null) {
                return td.intValue();
            }
        }
        return 5;
    }

    /*
     * fields
     */

    private transient FlavorMap flavorMap = SystemFlavorMap.getDefaultFlavorMap();

    private transient DragSourceListener listener;

    private transient DragSourceMotionListener motionListener;
}
