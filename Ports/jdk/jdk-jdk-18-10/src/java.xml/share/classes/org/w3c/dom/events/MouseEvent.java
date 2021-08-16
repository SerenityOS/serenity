/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file and, per its terms, should not be removed:
 *
 * Copyright (c) 2000 World Wide Web Consortium,
 * (Massachusetts Institute of Technology, Institut National de
 * Recherche en Informatique et en Automatique, Keio University). All
 * Rights Reserved. This program is distributed under the W3C's Software
 * Intellectual Property License. This program is distributed in the
 * hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.
 * See W3C License http://www.w3.org/Consortium/Legal/ for more details.
 */

package org.w3c.dom.events;

import org.w3c.dom.views.AbstractView;

/**
 * The <code>MouseEvent</code> interface provides specific contextual
 * information associated with Mouse events.
 * <p>The <code>detail</code> attribute inherited from <code>UIEvent</code>
 * indicates the number of times a mouse button has been pressed and
 * released over the same screen location during a user action. The
 * attribute value is 1 when the user begins this action and increments by 1
 * for each full sequence of pressing and releasing. If the user moves the
 * mouse between the mousedown and mouseup the value will be set to 0,
 * indicating that no click is occurring.
 * <p>In the case of nested elements mouse events are always targeted at the
 * most deeply nested element. Ancestors of the targeted element may use
 * bubbling to obtain notification of mouse events which occur within its
 * descendent elements.
 * <p>See also the <a href='http://www.w3.org/TR/2000/REC-DOM-Level-2-Events-20001113'>Document Object Model (DOM) Level 2 Events Specification</a>.
 * @since 1.5, DOM Level 2
 */
public interface MouseEvent extends UIEvent {
    /**
     * The horizontal coordinate at which the event occurred relative to the
     * origin of the screen coordinate system.
     */
    public int getScreenX();

    /**
     * The vertical coordinate at which the event occurred relative to the
     * origin of the screen coordinate system.
     */
    public int getScreenY();

    /**
     * The horizontal coordinate at which the event occurred relative to the
     * DOM implementation's client area.
     */
    public int getClientX();

    /**
     * The vertical coordinate at which the event occurred relative to the DOM
     * implementation's client area.
     */
    public int getClientY();

    /**
     * Used to indicate whether the 'ctrl' key was depressed during the firing
     * of the event.
     */
    public boolean getCtrlKey();

    /**
     * Used to indicate whether the 'shift' key was depressed during the
     * firing of the event.
     */
    public boolean getShiftKey();

    /**
     * Used to indicate whether the 'alt' key was depressed during the firing
     * of the event. On some platforms this key may map to an alternative
     * key name.
     */
    public boolean getAltKey();

    /**
     * Used to indicate whether the 'meta' key was depressed during the firing
     * of the event. On some platforms this key may map to an alternative
     * key name.
     */
    public boolean getMetaKey();

    /**
     * During mouse events caused by the depression or release of a mouse
     * button, <code>button</code> is used to indicate which mouse button
     * changed state. The values for <code>button</code> range from zero to
     * indicate the left button of the mouse, one to indicate the middle
     * button if present, and two to indicate the right button. For mice
     * configured for left handed use in which the button actions are
     * reversed the values are instead read from right to left.
     */
    public short getButton();

    /**
     * Used to identify a secondary <code>EventTarget</code> related to a UI
     * event. Currently this attribute is used with the mouseover event to
     * indicate the <code>EventTarget</code> which the pointing device
     * exited and with the mouseout event to indicate the
     * <code>EventTarget</code> which the pointing device entered.
     */
    public EventTarget getRelatedTarget();

    /**
     * The <code>initMouseEvent</code> method is used to initialize the value
     * of a <code>MouseEvent</code> created through the
     * <code>DocumentEvent</code> interface. This method may only be called
     * before the <code>MouseEvent</code> has been dispatched via the
     * <code>dispatchEvent</code> method, though it may be called multiple
     * times during that phase if necessary. If called multiple times, the
     * final invocation takes precedence.
     * @param typeArg Specifies the event type.
     * @param canBubbleArg Specifies whether or not the event can bubble.
     * @param cancelableArg Specifies whether or not the event's default
     *   action can be prevented.
     * @param viewArg Specifies the <code>Event</code>'s
     *   <code>AbstractView</code>.
     * @param detailArg Specifies the <code>Event</code>'s mouse click count.
     * @param screenXArg Specifies the <code>Event</code>'s screen x
     *   coordinate
     * @param screenYArg Specifies the <code>Event</code>'s screen y
     *   coordinate
     * @param clientXArg Specifies the <code>Event</code>'s client x
     *   coordinate
     * @param clientYArg Specifies the <code>Event</code>'s client y
     *   coordinate
     * @param ctrlKeyArg Specifies whether or not control key was depressed
     *   during the <code>Event</code>.
     * @param altKeyArg Specifies whether or not alt key was depressed during
     *   the <code>Event</code>.
     * @param shiftKeyArg Specifies whether or not shift key was depressed
     *   during the <code>Event</code>.
     * @param metaKeyArg Specifies whether or not meta key was depressed
     *   during the <code>Event</code>.
     * @param buttonArg Specifies the <code>Event</code>'s mouse button.
     * @param relatedTargetArg Specifies the <code>Event</code>'s related
     *   <code>EventTarget</code>.
     */
    public void initMouseEvent(String typeArg,
                               boolean canBubbleArg,
                               boolean cancelableArg,
                               AbstractView viewArg,
                               int detailArg,
                               int screenXArg,
                               int screenYArg,
                               int clientXArg,
                               int clientYArg,
                               boolean ctrlKeyArg,
                               boolean altKeyArg,
                               boolean shiftKeyArg,
                               boolean metaKeyArg,
                               short buttonArg,
                               EventTarget relatedTargetArg);

}
