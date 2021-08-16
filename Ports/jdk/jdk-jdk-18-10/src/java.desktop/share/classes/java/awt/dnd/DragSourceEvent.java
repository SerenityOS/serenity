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

import java.awt.Point;
import java.io.Serial;
import java.util.EventObject;

/**
 * This class is the base class for
 * {@code DragSourceDragEvent} and
 * {@code DragSourceDropEvent}.
 * <p>
 * {@code DragSourceEvent}s are generated whenever the drag enters, moves
 * over, or exits a drop site, when the drop action changes, and when the drag
 * ends. The location for the generated {@code DragSourceEvent} specifies
 * the mouse cursor location in screen coordinates at the moment this event
 * occurred.
 * <p>
 * In a multi-screen environment without a virtual device, the cursor location is
 * specified in the coordinate system of the <i>initiator</i>
 * {@code GraphicsConfiguration}. The <i>initiator</i>
 * {@code GraphicsConfiguration} is the {@code GraphicsConfiguration}
 * of the {@code Component} on which the drag gesture for the current drag
 * operation was recognized. If the cursor location is outside the bounds of
 * the initiator {@code GraphicsConfiguration}, the reported coordinates are
 * clipped to fit within the bounds of that {@code GraphicsConfiguration}.
 * <p>
 * In a multi-screen environment with a virtual device, the location is specified
 * in the corresponding virtual coordinate system. If the cursor location is
 * outside the bounds of the virtual device the reported coordinates are
 * clipped to fit within the bounds of the virtual device.
 *
 * @since 1.2
 */

public class DragSourceEvent extends EventObject {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -763287114604032641L;

    /**
     * The {@code boolean} indicating whether the cursor location
     * is specified for this event.
     *
     * @serial
     */
    private final boolean locationSpecified;

    /**
     * The horizontal coordinate for the cursor location at the moment this
     * event occurred if the cursor location is specified for this event;
     * otherwise zero.
     *
     * @serial
     */
    private final int x;

    /**
     * The vertical coordinate for the cursor location at the moment this event
     * occurred if the cursor location is specified for this event;
     * otherwise zero.
     *
     * @serial
     */
    private final int y;

    /**
     * Construct a {@code DragSourceEvent}
     * given a specified {@code DragSourceContext}.
     * The coordinates for this {@code DragSourceEvent}
     * are not specified, so {@code getLocation} will return
     * {@code null} for this event.
     *
     * @param dsc the {@code DragSourceContext}
     *
     * @throws IllegalArgumentException if {@code dsc} is {@code null}.
     *
     * @see #getLocation
     */

    public DragSourceEvent(DragSourceContext dsc) {
        super(dsc);
        locationSpecified = false;
        this.x = 0;
        this.y = 0;
    }

    /**
     * Construct a {@code DragSourceEvent} given a specified
     * {@code DragSourceContext}, and coordinates of the cursor
     * location.
     *
     * @param dsc the {@code DragSourceContext}
     * @param x   the horizontal coordinate for the cursor location
     * @param y   the vertical coordinate for the cursor location
     *
     * @throws IllegalArgumentException if {@code dsc} is {@code null}.
     *
     * @since 1.4
     */
    public DragSourceEvent(DragSourceContext dsc, int x, int y) {
        super(dsc);
        locationSpecified = true;
        this.x = x;
        this.y = y;
    }

    /**
     * This method returns the {@code DragSourceContext} that
     * originated the event.
     *
     * @return the {@code DragSourceContext} that originated the event
     */

    public DragSourceContext getDragSourceContext() {
        return (DragSourceContext)getSource();
    }

    /**
     * This method returns a {@code Point} indicating the cursor
     * location in screen coordinates at the moment this event occurred, or
     * {@code null} if the cursor location is not specified for this
     * event.
     *
     * @return the {@code Point} indicating the cursor location
     *         or {@code null} if the cursor location is not specified
     * @since 1.4
     */
    public Point getLocation() {
        if (locationSpecified) {
            return new Point(x, y);
        } else {
            return null;
        }
    }

    /**
     * This method returns the horizontal coordinate of the cursor location in
     * screen coordinates at the moment this event occurred, or zero if the
     * cursor location is not specified for this event.
     *
     * @return an integer indicating the horizontal coordinate of the cursor
     *         location or zero if the cursor location is not specified
     * @since 1.4
     */
    public int getX() {
        return x;
    }

    /**
     * This method returns the vertical coordinate of the cursor location in
     * screen coordinates at the moment this event occurred, or zero if the
     * cursor location is not specified for this event.
     *
     * @return an integer indicating the vertical coordinate of the cursor
     *         location or zero if the cursor location is not specified
     * @since 1.4
     */
    public int getY() {
        return y;
    }
}
