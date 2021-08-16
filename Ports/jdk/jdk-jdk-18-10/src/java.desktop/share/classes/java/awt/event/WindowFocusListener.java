/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.EventListener;

/**
 * The listener interface for receiving {@code WindowEvents}, including
 * {@code WINDOW_GAINED_FOCUS} and {@code WINDOW_LOST_FOCUS} events.
 * The class that is interested in processing a {@code WindowEvent}
 * either implements this interface (and
 * all the methods it contains) or extends the abstract
 * {@code WindowAdapter} class (overriding only the methods of interest).
 * The listener object created from that class is then registered with a
 * {@code Window}
 * using the {@code Window}'s {@code addWindowFocusListener} method.
 * When the {@code Window}'s
 * status changes by virtue of it being opened, closed, activated, deactivated,
 * iconified, or deiconified, or by focus being transferred into or out of the
 * {@code Window}, the relevant method in the listener object is invoked,
 * and the {@code WindowEvent} is passed to it.
 *
 * @author David Mendenhall
 *
 * @see WindowAdapter
 * @see WindowEvent
 * @see <a href="https://docs.oracle.com/javase/tutorial/uiswing/events/windowlistener.html">Tutorial: Writing a Window Listener</a>
 *
 * @since 1.4
 */
public interface WindowFocusListener extends EventListener {

    /**
     * Invoked when the Window is set to be the focused Window, which means
     * that the Window, or one of its subcomponents, will receive keyboard
     * events.
     * @param e the event to be processed
     */
    public void windowGainedFocus(WindowEvent e);

    /**
     * Invoked when the Window is no longer the focused Window, which means
     * that keyboard events will no longer be delivered to the Window or any of
     * its subcomponents.
     * @param e the event to be processed
     */
    public void windowLostFocus(WindowEvent e);
}
