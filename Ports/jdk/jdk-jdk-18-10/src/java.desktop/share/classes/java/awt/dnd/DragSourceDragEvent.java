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

import java.awt.event.InputEvent;
import java.io.Serial;

/**
 * The {@code DragSourceDragEvent} is
 * delivered from the {@code DragSourceContextPeer},
 * via the {@code DragSourceContext}, to the {@code DragSourceListener}
 * registered with that {@code DragSourceContext} and with its associated
 * {@code DragSource}.
 * <p>
 * The {@code DragSourceDragEvent} reports the <i>target drop action</i>
 * and the <i>user drop action</i> that reflect the current state of
 * the drag operation.
 * <p>
 * <i>Target drop action</i> is one of {@code DnDConstants} that represents
 * the drop action selected by the current drop target if this drop action is
 * supported by the drag source or {@code DnDConstants.ACTION_NONE} if this
 * drop action is not supported by the drag source.
 * <p>
 * <i>User drop action</i> depends on the drop actions supported by the drag
 * source and the drop action selected by the user. The user can select a drop
 * action by pressing modifier keys during the drag operation:
 * <pre>
 *   Ctrl + Shift -&gt; ACTION_LINK
 *   Ctrl         -&gt; ACTION_COPY
 *   Shift        -&gt; ACTION_MOVE
 * </pre>
 * If the user selects a drop action, the <i>user drop action</i> is one of
 * {@code DnDConstants} that represents the selected drop action if this
 * drop action is supported by the drag source or
 * {@code DnDConstants.ACTION_NONE} if this drop action is not supported
 * by the drag source.
 * <p>
 * If the user doesn't select a drop action, the set of
 * {@code DnDConstants} that represents the set of drop actions supported
 * by the drag source is searched for {@code DnDConstants.ACTION_MOVE},
 * then for {@code DnDConstants.ACTION_COPY}, then for
 * {@code DnDConstants.ACTION_LINK} and the <i>user drop action</i> is the
 * first constant found. If no constant is found the <i>user drop action</i>
 * is {@code DnDConstants.ACTION_NONE}.
 *
 * @since 1.2
 *
 */

public class DragSourceDragEvent extends DragSourceEvent {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 481346297933902471L;

    /**
     * Constructs a {@code DragSourceDragEvent}.
     * This class is typically
     * instantiated by the {@code DragSourceContextPeer}
     * rather than directly
     * by client code.
     * The coordinates for this {@code DragSourceDragEvent}
     * are not specified, so {@code getLocation} will return
     * {@code null} for this event.
     * <p>
     * The arguments {@code dropAction} and {@code action} should
     * be one of {@code DnDConstants} that represents a single action.
     * The argument {@code modifiers} should be either a bitwise mask
     * of old {@code java.awt.event.InputEvent.*_MASK} constants or a
     * bitwise mask of extended {@code java.awt.event.InputEvent.*_DOWN_MASK}
     * constants.
     * This constructor does not throw any exception for invalid {@code dropAction},
     * {@code action} and {@code modifiers}.
     *
     * @param dsc the {@code DragSourceContext} that is to manage
     *            notifications for this event.
     * @param dropAction the user drop action.
     * @param action the target drop action.
     * @param modifiers the modifier keys down during event (shift, ctrl,
     *        alt, meta)
     *        Either extended _DOWN_MASK or old _MASK modifiers
     *        should be used, but both models should not be mixed
     *        in one event. Use of the extended modifiers is
     *        preferred.
     *
     * @throws IllegalArgumentException if {@code dsc} is {@code null}.
     *
     * @see java.awt.event.InputEvent
     * @see DragSourceEvent#getLocation
     */

    public DragSourceDragEvent(DragSourceContext dsc, int dropAction,
                               int action, int modifiers) {
        super(dsc);

        targetActions    = action;
        gestureModifiers = modifiers;
        this.dropAction  = dropAction;
        if ((modifiers & ~(JDK_1_3_MODIFIERS | JDK_1_4_MODIFIERS)) != 0) {
            invalidModifiers = true;
        } else if ((getGestureModifiers() != 0) && (getGestureModifiersEx() == 0)) {
            setNewModifiers();
        } else if ((getGestureModifiers() == 0) && (getGestureModifiersEx() != 0)) {
            setOldModifiers();
        } else {
            invalidModifiers = true;
        }
    }

    /**
     * Constructs a {@code DragSourceDragEvent} given the specified
     * {@code DragSourceContext}, user drop action, target drop action,
     * modifiers and coordinates.
     * <p>
     * The arguments {@code dropAction} and {@code action} should
     * be one of {@code DnDConstants} that represents a single action.
     * The argument {@code modifiers} should be either a bitwise mask
     * of old {@code java.awt.event.InputEvent.*_MASK} constants or a
     * bitwise mask of extended {@code java.awt.event.InputEvent.*_DOWN_MASK}
     * constants.
     * This constructor does not throw any exception for invalid {@code dropAction},
     * {@code action} and {@code modifiers}.
     *
     * @param dsc the {@code DragSourceContext} associated with this
     *        event.
     * @param dropAction the user drop action.
     * @param action the target drop action.
     * @param modifiers the modifier keys down during event (shift, ctrl,
     *        alt, meta)
     *        Either extended _DOWN_MASK or old _MASK modifiers
     *        should be used, but both models should not be mixed
     *        in one event. Use of the extended modifiers is
     *        preferred.
     * @param x   the horizontal coordinate for the cursor location
     * @param y   the vertical coordinate for the cursor location
     *
     * @throws IllegalArgumentException if {@code dsc} is {@code null}.
     *
     * @see java.awt.event.InputEvent
     * @since 1.4
     */
    public DragSourceDragEvent(DragSourceContext dsc, int dropAction,
                               int action, int modifiers, int x, int y) {
        super(dsc, x, y);

        targetActions    = action;
        gestureModifiers = modifiers;
        this.dropAction  = dropAction;
        if ((modifiers & ~(JDK_1_3_MODIFIERS | JDK_1_4_MODIFIERS)) != 0) {
            invalidModifiers = true;
        } else if ((getGestureModifiers() != 0) && (getGestureModifiersEx() == 0)) {
            setNewModifiers();
        } else if ((getGestureModifiers() == 0) && (getGestureModifiersEx() != 0)) {
            setOldModifiers();
        } else {
            invalidModifiers = true;
        }
    }

    /**
     * This method returns the target drop action.
     *
     * @return the target drop action.
     */
    public int getTargetActions() {
        return targetActions;
    }


    private static final int JDK_1_3_MODIFIERS = InputEvent.SHIFT_DOWN_MASK - 1;
    private static final int JDK_1_4_MODIFIERS =
            ((InputEvent.ALT_GRAPH_DOWN_MASK << 1) - 1) & ~JDK_1_3_MODIFIERS;

    /**
     * This method returns an {@code int} representing
     * the current state of the input device modifiers
     * associated with the user's gesture. Typically these
     * would be mouse buttons or keyboard modifiers.
     * <P>
     * If the {@code modifiers} passed to the constructor
     * are invalid, this method returns them unchanged.
     *
     * @return the current state of the input device modifiers
     */

    public int getGestureModifiers() {
        return invalidModifiers ? gestureModifiers : gestureModifiers & JDK_1_3_MODIFIERS;
    }

    /**
     * This method returns an {@code int} representing
     * the current state of the input device extended modifiers
     * associated with the user's gesture.
     * See {@link InputEvent#getModifiersEx}
     * <P>
     * If the {@code modifiers} passed to the constructor
     * are invalid, this method returns them unchanged.
     *
     * @return the current state of the input device extended modifiers
     * @since 1.4
     */

    public int getGestureModifiersEx() {
        return invalidModifiers ? gestureModifiers : gestureModifiers & JDK_1_4_MODIFIERS;
    }

    /**
     * This method returns the user drop action.
     *
     * @return the user drop action.
     */
    public int getUserAction() { return dropAction; }

    /**
     * This method returns the logical intersection of
     * the target drop action and the set of drop actions supported by
     * the drag source.
     *
     * @return the logical intersection of the target drop action and
     *         the set of drop actions supported by the drag source.
     */
    public int getDropAction() {
        return targetActions & getDragSourceContext().getSourceActions();
    }

    /*
     * fields
     */

    /**
     * The target drop action.
     *
     * @serial
     */
    private int     targetActions    = DnDConstants.ACTION_NONE;

    /**
     * The user drop action.
     *
     * @serial
     */
    private int     dropAction       = DnDConstants.ACTION_NONE;

    /**
     * The state of the input device modifiers associated with the user
     * gesture.
     *
     * @serial
     */
    private int     gestureModifiers = 0;

    /**
     * Indicates whether the {@code gestureModifiers} are invalid.
     *
     * @serial
     */
    private boolean invalidModifiers;

    /**
     * Sets new modifiers by the old ones.
     * The mouse modifiers have higher priority than overlaying key
     * modifiers.
     */
    @SuppressWarnings("deprecation")
    private void setNewModifiers() {
        if ((gestureModifiers & InputEvent.BUTTON1_MASK) != 0) {
            gestureModifiers |= InputEvent.BUTTON1_DOWN_MASK;
        }
        if ((gestureModifiers & InputEvent.BUTTON2_MASK) != 0) {
            gestureModifiers |= InputEvent.BUTTON2_DOWN_MASK;
        }
        if ((gestureModifiers & InputEvent.BUTTON3_MASK) != 0) {
            gestureModifiers |= InputEvent.BUTTON3_DOWN_MASK;
        }
        if ((gestureModifiers & InputEvent.SHIFT_MASK) != 0) {
            gestureModifiers |= InputEvent.SHIFT_DOWN_MASK;
        }
        if ((gestureModifiers & InputEvent.CTRL_MASK) != 0) {
            gestureModifiers |= InputEvent.CTRL_DOWN_MASK;
        }
        if ((gestureModifiers & InputEvent.ALT_GRAPH_MASK) != 0) {
            gestureModifiers |= InputEvent.ALT_GRAPH_DOWN_MASK;
        }
    }

    /**
     * Sets old modifiers by the new ones.
     */
    @SuppressWarnings("deprecation")
    private void setOldModifiers() {
        if ((gestureModifiers & InputEvent.BUTTON1_DOWN_MASK) != 0) {
            gestureModifiers |= InputEvent.BUTTON1_MASK;
        }
        if ((gestureModifiers & InputEvent.BUTTON2_DOWN_MASK) != 0) {
            gestureModifiers |= InputEvent.BUTTON2_MASK;
        }
        if ((gestureModifiers & InputEvent.BUTTON3_DOWN_MASK) != 0) {
            gestureModifiers |= InputEvent.BUTTON3_MASK;
        }
        if ((gestureModifiers & InputEvent.SHIFT_DOWN_MASK) != 0) {
            gestureModifiers |= InputEvent.SHIFT_MASK;
        }
        if ((gestureModifiers & InputEvent.CTRL_DOWN_MASK) != 0) {
            gestureModifiers |= InputEvent.CTRL_MASK;
        }
        if ((gestureModifiers & InputEvent.ALT_GRAPH_DOWN_MASK) != 0) {
            gestureModifiers |= InputEvent.ALT_GRAPH_MASK;
        }
    }
}
