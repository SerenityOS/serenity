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

/**
 *  The <code>EventTarget</code> interface is implemented by all
 * <code>Nodes</code> in an implementation which supports the DOM Event
 * Model. Therefore, this interface can be obtained by using
 * binding-specific casting methods on an instance of the <code>Node</code>
 * interface. The interface allows registration and removal of
 * <code>EventListeners</code> on an <code>EventTarget</code> and dispatch
 * of events to that <code>EventTarget</code>.
 * <p>See also the <a href='http://www.w3.org/TR/2000/REC-DOM-Level-2-Events-20001113'>Document Object Model (DOM) Level 2 Events Specification</a>.
 * @since 1.5, DOM Level 2
 */
public interface EventTarget {
    /**
     * This method allows the registration of event listeners on the event
     * target. If an <code>EventListener</code> is added to an
     * <code>EventTarget</code> while it is processing an event, it will not
     * be triggered by the current actions but may be triggered during a
     * later stage of event flow, such as the bubbling phase.
     * <br> If multiple identical <code>EventListener</code>s are registered
     * on the same <code>EventTarget</code> with the same parameters the
     * duplicate instances are discarded. They do not cause the
     * <code>EventListener</code> to be called twice and since they are
     * discarded they do not need to be removed with the
     * <code>removeEventListener</code> method.
     * @param type The event type for which the user is registering
     * @param listener The <code>listener</code> parameter takes an interface
     *   implemented by the user which contains the methods to be called
     *   when the event occurs.
     * @param useCapture If true, <code>useCapture</code> indicates that the
     *   user wishes to initiate capture. After initiating capture, all
     *   events of the specified type will be dispatched to the registered
     *   <code>EventListener</code> before being dispatched to any
     *   <code>EventTargets</code> beneath them in the tree. Events which
     *   are bubbling upward through the tree will not trigger an
     *   <code>EventListener</code> designated to use capture.
     */
    public void addEventListener(String type,
                                 EventListener listener,
                                 boolean useCapture);

    /**
     * This method allows the removal of event listeners from the event
     * target. If an <code>EventListener</code> is removed from an
     * <code>EventTarget</code> while it is processing an event, it will not
     * be triggered by the current actions. <code>EventListener</code>s can
     * never be invoked after being removed.
     * <br>Calling <code>removeEventListener</code> with arguments which do
     * not identify any currently registered <code>EventListener</code> on
     * the <code>EventTarget</code> has no effect.
     * @param type Specifies the event type of the <code>EventListener</code>
     *   being removed.
     * @param listener The <code>EventListener</code> parameter indicates the
     *   <code>EventListener </code> to be removed.
     * @param useCapture Specifies whether the <code>EventListener</code>
     *   being removed was registered as a capturing listener or not. If a
     *   listener was registered twice, one with capture and one without,
     *   each must be removed separately. Removal of a capturing listener
     *   does not affect a non-capturing version of the same listener, and
     *   vice versa.
     */
    public void removeEventListener(String type,
                                    EventListener listener,
                                    boolean useCapture);

    /**
     * This method allows the dispatch of events into the implementations
     * event model. Events dispatched in this manner will have the same
     * capturing and bubbling behavior as events dispatched directly by the
     * implementation. The target of the event is the
     * <code> EventTarget</code> on which <code>dispatchEvent</code> is
     * called.
     * @param evt Specifies the event type, behavior, and contextual
     *   information to be used in processing the event.
     * @return The return value of <code>dispatchEvent</code> indicates
     *   whether any of the listeners which handled the event called
     *   <code>preventDefault</code>. If <code>preventDefault</code> was
     *   called the value is false, else the value is true.
     * @exception EventException
     *   UNSPECIFIED_EVENT_TYPE_ERR: Raised if the <code>Event</code>'s type
     *   was not specified by initializing the event before
     *   <code>dispatchEvent</code> was called. Specification of the
     *   <code>Event</code>'s type as <code>null</code> or an empty string
     *   will also trigger this exception.
     */
    public boolean dispatchEvent(Event evt)
                                 throws EventException;

}
