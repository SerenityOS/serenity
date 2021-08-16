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

package java.awt.event;

import java.awt.Component;
import java.io.Serial;
import java.lang.annotation.Native;

/**
 * An event which indicates that the mouse wheel was rotated in a component.
 * <P>
 * A wheel mouse is a mouse which has a wheel in place of the middle button.
 * This wheel can be rotated towards or away from the user.  Mouse wheels are
 * most often used for scrolling, though other uses are possible.
 * <P>
 * A MouseWheelEvent object is passed to every {@code MouseWheelListener}
 * object which registered to receive the "interesting" mouse events using the
 * component's {@code addMouseWheelListener} method.  Each such listener
 * object gets a {@code MouseEvent} containing the mouse event.
 * <P>
 * Due to the mouse wheel's special relationship to scrolling Components,
 * MouseWheelEvents are delivered somewhat differently than other MouseEvents.
 * This is because while other MouseEvents usually affect a change on
 * the Component directly under the mouse
 * cursor (for instance, when clicking a button), MouseWheelEvents often have
 * an effect away from the mouse cursor (moving the wheel while
 * over a Component inside a ScrollPane should scroll one of the
 * Scrollbars on the ScrollPane).
 * <P>
 * MouseWheelEvents start delivery from the Component underneath the
 * mouse cursor.  If MouseWheelEvents are not enabled on the
 * Component, the event is delivered to the first ancestor
 * Container with MouseWheelEvents enabled.  This will usually be
 * a ScrollPane with wheel scrolling enabled.  The source
 * Component and x,y coordinates will be relative to the event's
 * final destination (the ScrollPane).  This allows a complex
 * GUI to be installed without modification into a ScrollPane, and
 * for all MouseWheelEvents to be delivered to the ScrollPane for
 * scrolling.
 * <P>
 * Some AWT Components are implemented using native widgets which
 * display their own scrollbars and handle their own scrolling.
 * The particular Components for which this is true will vary from
 * platform to platform.  When the mouse wheel is
 * moved over one of these Components, the event is delivered straight to
 * the native widget, and not propagated to ancestors.
 * <P>
 * Platforms offer customization of the amount of scrolling that
 * should take place when the mouse wheel is moved.  The two most
 * common settings are to scroll a certain number of "units"
 * (commonly lines of text in a text-based component) or an entire "block"
 * (similar to page-up/page-down).  The MouseWheelEvent offers
 * methods for conforming to the underlying platform settings.  These
 * platform settings can be changed at any time by the user.  MouseWheelEvents
 * reflect the most recent settings.
 * <P>
 * The {@code MouseWheelEvent} class includes methods for
 * getting the number of "clicks" by which the mouse wheel is rotated.
 * The {@link #getWheelRotation} method returns the integer number
 * of "clicks" corresponding to the number of notches by which the wheel was
 * rotated. In addition to this method, the {@code MouseWheelEvent}
 * class provides the {@link #getPreciseWheelRotation} method which returns
 * a double number of "clicks" in case a partial rotation occurred.
 * The {@link #getPreciseWheelRotation} method is useful if a mouse supports
 * a high-resolution wheel, such as a freely rotating wheel with no
 * notches. Applications can benefit by using this method to process
 * mouse wheel events more precisely, and thus, making visual perception
 * smoother.
 *
 * @author Brent Christian
 * @see MouseWheelListener
 * @see java.awt.ScrollPane
 * @see java.awt.ScrollPane#setWheelScrollingEnabled(boolean)
 * @see javax.swing.JScrollPane
 * @see javax.swing.JScrollPane#setWheelScrollingEnabled(boolean)
 * @since 1.4
 */

public class MouseWheelEvent extends MouseEvent {

    /**
     * Constant representing scrolling by "units" (like scrolling with the
     * arrow keys)
     *
     * @see #getScrollType
     */
    @Native public static final int WHEEL_UNIT_SCROLL = 0;

    /**
     * Constant representing scrolling by a "block" (like scrolling
     * with page-up, page-down keys)
     *
     * @see #getScrollType
     */
    @Native public static final int WHEEL_BLOCK_SCROLL = 1;

    /**
     * Indicates what sort of scrolling should take place in response to this
     * event, based on platform settings.  Legal values are:
     * <ul>
     * <li> WHEEL_UNIT_SCROLL
     * <li> WHEEL_BLOCK_SCROLL
     * </ul>
     *
     * @see #getScrollType
     */
    int scrollType;

    /**
     * Only valid for scrollType WHEEL_UNIT_SCROLL.
     * Indicates number of units that should be scrolled per
     * click of mouse wheel rotation, based on platform settings.
     *
     * @see #getScrollAmount
     * @see #getScrollType
     */
    int scrollAmount;

    /**
     * Indicates how far the mouse wheel was rotated.
     *
     * @see #getWheelRotation
     */
    int wheelRotation;

    /**
     * Indicates how far the mouse wheel was rotated.
     *
     * @see #getPreciseWheelRotation
     */
    double preciseWheelRotation;

    /**
     * Use serialVersionUID from JDK 1.6 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 6459879390515399677L;

    /**
     * Constructs a {@code MouseWheelEvent} object with the
     * specified source component, type, modifiers, coordinates,
     * scroll type, scroll amount, and wheel rotation.
     * <p>Absolute coordinates xAbs and yAbs are set to source's location on screen plus
     * relative coordinates x and y. xAbs and yAbs are set to zero if the source is not showing.
     * <p>Note that passing in an invalid {@code id} results in
     * unspecified behavior. This method throws an
     * {@code IllegalArgumentException} if {@code source}
     * is {@code null}.
     *
     * @param source         the {@code Component} that originated
     *                       the event
     * @param id             the integer that identifies the event
     * @param when           a long that gives the time the event occurred
     * @param modifiers      the modifier keys down during event
     *                       (shift, ctrl, alt, meta)
     * @param x              the horizontal x coordinate for the mouse location
     * @param y              the vertical y coordinate for the mouse location
     * @param clickCount     the number of mouse clicks associated with event
     * @param popupTrigger   a boolean, true if this event is a trigger for a
     *                       popup-menu
     * @param scrollType     the type of scrolling which should take place in
     *                       response to this event;  valid values are
     *                       {@code WHEEL_UNIT_SCROLL} and
     *                       {@code WHEEL_BLOCK_SCROLL}
     * @param  scrollAmount  for scrollType {@code WHEEL_UNIT_SCROLL},
     *                       the number of units to be scrolled
     * @param wheelRotation  the integer number of "clicks" by which the mouse
     *                       wheel was rotated
     *
     * @throws IllegalArgumentException if {@code source} is null
     * @see MouseEvent#MouseEvent(java.awt.Component, int, long, int, int, int, int, boolean)
     * @see MouseEvent#MouseEvent(java.awt.Component, int, long, int, int, int, int, int, int, boolean, int)
     */
    public MouseWheelEvent (Component source, int id, long when, int modifiers,
                      int x, int y, int clickCount, boolean popupTrigger,
                      int scrollType, int scrollAmount, int wheelRotation) {

        this(source, id, when, modifiers, x, y, 0, 0, clickCount,
             popupTrigger, scrollType, scrollAmount, wheelRotation);
    }

    /**
     * Constructs a {@code MouseWheelEvent} object with the
     * specified source component, type, modifiers, coordinates,
     * absolute coordinates, scroll type, scroll amount, and wheel rotation.
     * <p>Note that passing in an invalid {@code id} results in
     * unspecified behavior. This method throws an
     * {@code IllegalArgumentException} if {@code source}
     * is {@code null}.<p>
     * Even if inconsistent values for relative and absolute coordinates are
     * passed to the constructor, the MouseWheelEvent instance is still
     * created and no exception is thrown.
     *
     * @param source         the {@code Component} that originated
     *                       the event
     * @param id             the integer that identifies the event
     * @param when           a long that gives the time the event occurred
     * @param modifiers      the modifier keys down during event
     *                       (shift, ctrl, alt, meta)
     * @param x              the horizontal x coordinate for the mouse location
     * @param y              the vertical y coordinate for the mouse location
     * @param xAbs           the absolute horizontal x coordinate for the mouse location
     * @param yAbs           the absolute vertical y coordinate for the mouse location
     * @param clickCount     the number of mouse clicks associated with event
     * @param popupTrigger   a boolean, true if this event is a trigger for a
     *                       popup-menu
     * @param scrollType     the type of scrolling which should take place in
     *                       response to this event;  valid values are
     *                       {@code WHEEL_UNIT_SCROLL} and
     *                       {@code WHEEL_BLOCK_SCROLL}
     * @param  scrollAmount  for scrollType {@code WHEEL_UNIT_SCROLL},
     *                       the number of units to be scrolled
     * @param wheelRotation  the integer number of "clicks" by which the mouse
     *                       wheel was rotated
     *
     * @throws IllegalArgumentException if {@code source} is null
     * @see MouseEvent#MouseEvent(java.awt.Component, int, long, int, int, int, int, boolean)
     * @see MouseEvent#MouseEvent(java.awt.Component, int, long, int, int, int, int, int, int, boolean, int)
     * @since 1.6
     */
    public MouseWheelEvent (Component source, int id, long when, int modifiers,
                            int x, int y, int xAbs, int yAbs, int clickCount, boolean popupTrigger,
                            int scrollType, int scrollAmount, int wheelRotation) {

        this(source, id, when, modifiers, x, y, xAbs, yAbs, clickCount, popupTrigger,
             scrollType, scrollAmount, wheelRotation, wheelRotation);

    }


    /**
     * Constructs a {@code MouseWheelEvent} object with the specified
     * source component, type, modifiers, coordinates, absolute coordinates,
     * scroll type, scroll amount, and wheel rotation.
     * <p>Note that passing in an invalid {@code id} parameter results
     * in unspecified behavior. This method throws an
     * {@code IllegalArgumentException} if {@code source} equals
     * {@code null}.
     * <p>Even if inconsistent values for relative and absolute coordinates
     * are passed to the constructor, a {@code MouseWheelEvent} instance
     * is still created and no exception is thrown.
     *
     * @param source         the {@code Component} that originated the event
     * @param id             the integer value that identifies the event
     * @param when           a long value that gives the time when the event occurred
     * @param modifiers      the modifier keys down during event
     *                       (shift, ctrl, alt, meta)
     * @param x              the horizontal {@code x} coordinate for the
     *                       mouse location
     * @param y              the vertical {@code y} coordinate for the
     *                       mouse location
     * @param xAbs           the absolute horizontal {@code x} coordinate for
     *                       the mouse location
     * @param yAbs           the absolute vertical {@code y} coordinate for
     *                       the mouse location
     * @param clickCount     the number of mouse clicks associated with the event
     * @param popupTrigger   a boolean value, {@code true} if this event is a trigger
     *                       for a popup-menu
     * @param scrollType     the type of scrolling which should take place in
     *                       response to this event;  valid values are
     *                       {@code WHEEL_UNIT_SCROLL} and
     *                       {@code WHEEL_BLOCK_SCROLL}
     * @param  scrollAmount  for scrollType {@code WHEEL_UNIT_SCROLL},
     *                       the number of units to be scrolled
     * @param wheelRotation  the integer number of "clicks" by which the mouse wheel
     *                       was rotated
     * @param preciseWheelRotation the double number of "clicks" by which the mouse wheel
     *                       was rotated
     *
     * @throws IllegalArgumentException if {@code source} is null
     * @see MouseEvent#MouseEvent(java.awt.Component, int, long, int, int, int, int, boolean)
     * @see MouseEvent#MouseEvent(java.awt.Component, int, long, int, int, int, int, int, int, boolean, int)
     * @since 1.7
     */
    public MouseWheelEvent (Component source, int id, long when, int modifiers,
                            int x, int y, int xAbs, int yAbs, int clickCount, boolean popupTrigger,
                            int scrollType, int scrollAmount, int wheelRotation, double preciseWheelRotation) {

        super(source, id, when, modifiers, x, y, xAbs, yAbs, clickCount,
              popupTrigger, MouseEvent.NOBUTTON);

        this.scrollType = scrollType;
        this.scrollAmount = scrollAmount;
        this.wheelRotation = wheelRotation;
        this.preciseWheelRotation = preciseWheelRotation;

    }

    /**
     * Returns the type of scrolling that should take place in response to this
     * event.  This is determined by the native platform.  Legal values are:
     * <ul>
     * <li> MouseWheelEvent.WHEEL_UNIT_SCROLL
     * <li> MouseWheelEvent.WHEEL_BLOCK_SCROLL
     * </ul>
     *
     * @return either MouseWheelEvent.WHEEL_UNIT_SCROLL or
     *  MouseWheelEvent.WHEEL_BLOCK_SCROLL, depending on the configuration of
     *  the native platform.
     * @see java.awt.Adjustable#getUnitIncrement
     * @see java.awt.Adjustable#getBlockIncrement
     * @see javax.swing.Scrollable#getScrollableUnitIncrement
     * @see javax.swing.Scrollable#getScrollableBlockIncrement
     */
    public int getScrollType() {
        return scrollType;
    }

    /**
     * Returns the number of units that should be scrolled per
     * click of mouse wheel rotation.
     * Only valid if {@code getScrollType} returns
     * {@code MouseWheelEvent.WHEEL_UNIT_SCROLL}
     *
     * @return number of units to scroll, or an undefined value if
     *  {@code getScrollType} returns
     *  {@code MouseWheelEvent.WHEEL_BLOCK_SCROLL}
     * @see #getScrollType
     */
    public int getScrollAmount() {
        return scrollAmount;
    }

    /**
     * Returns the number of "clicks" the mouse wheel was rotated, as an integer.
     * A partial rotation may occur if the mouse supports a high-resolution wheel.
     * In this case, the method returns zero until a full "click" has been accumulated.
     *
     * @return negative values if the mouse wheel was rotated up/away from
     * the user, and positive values if the mouse wheel was rotated down/
     * towards the user
     * @see #getPreciseWheelRotation
     */
    public int getWheelRotation() {
        return wheelRotation;
    }

    /**
     * Returns the number of "clicks" the mouse wheel was rotated, as a double.
     * A partial rotation may occur if the mouse supports a high-resolution wheel.
     * In this case, the return value will include a fractional "click".
     *
     * @return negative values if the mouse wheel was rotated up or away from
     * the user, and positive values if the mouse wheel was rotated down or
     * towards the user
     * @see #getWheelRotation
     * @since 1.7
     */
    public double getPreciseWheelRotation() {
        return preciseWheelRotation;
    }

    /**
     * This is a convenience method to aid in the implementation of
     * the common-case MouseWheelListener - to scroll a ScrollPane or
     * JScrollPane by an amount which conforms to the platform settings.
     * (Note, however, that {@code ScrollPane} and
     * {@code JScrollPane} already have this functionality built in.)
     * <P>
     * This method returns the number of units to scroll when scroll type is
     * MouseWheelEvent.WHEEL_UNIT_SCROLL, and should only be called if
     * {@code getScrollType} returns MouseWheelEvent.WHEEL_UNIT_SCROLL.
     * <P>
     * Direction of scroll, amount of wheel movement,
     * and platform settings for wheel scrolling are all accounted for.
     * This method does not and cannot take into account value of the
     * Adjustable/Scrollable unit increment, as this will vary among
     * scrolling components.
     * <P>
     * A simplified example of how this method might be used in a
     * listener:
     * <pre>
     *  mouseWheelMoved(MouseWheelEvent event) {
     *      ScrollPane sp = getScrollPaneFromSomewhere();
     *      Adjustable adj = sp.getVAdjustable()
     *      if (MouseWheelEvent.getScrollType() == WHEEL_UNIT_SCROLL) {
     *          int totalScrollAmount =
     *              event.getUnitsToScroll() *
     *              adj.getUnitIncrement();
     *          adj.setValue(adj.getValue() + totalScrollAmount);
     *      }
     *  }
     * </pre>
     *
     * @return the number of units to scroll based on the direction and amount
     *  of mouse wheel rotation, and on the wheel scrolling settings of the
     *  native platform
     * @see #getScrollType
     * @see #getScrollAmount
     * @see MouseWheelListener
     * @see java.awt.Adjustable
     * @see java.awt.Adjustable#getUnitIncrement
     * @see javax.swing.Scrollable
     * @see javax.swing.Scrollable#getScrollableUnitIncrement
     * @see java.awt.ScrollPane
     * @see java.awt.ScrollPane#setWheelScrollingEnabled
     * @see javax.swing.JScrollPane
     * @see javax.swing.JScrollPane#setWheelScrollingEnabled
     */
    public int getUnitsToScroll() {
        return scrollAmount * wheelRotation;
    }

    /**
     * Returns a parameter string identifying this event.
     * This method is useful for event-logging and for debugging.
     *
     * @return a string identifying the event and its attributes
     */
    public String paramString() {
        String scrollTypeStr = null;

        if (getScrollType() == WHEEL_UNIT_SCROLL) {
            scrollTypeStr = "WHEEL_UNIT_SCROLL";
        }
        else if (getScrollType() == WHEEL_BLOCK_SCROLL) {
            scrollTypeStr = "WHEEL_BLOCK_SCROLL";
        }
        else {
            scrollTypeStr = "unknown scroll type";
        }
        return super.paramString()+",scrollType="+scrollTypeStr+
         ",scrollAmount="+getScrollAmount()+",wheelRotation="+
         getWheelRotation()+",preciseWheelRotation="+getPreciseWheelRotation();
    }
}
