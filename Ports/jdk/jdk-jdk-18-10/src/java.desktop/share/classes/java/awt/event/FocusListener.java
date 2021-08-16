/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * The listener interface for receiving keyboard focus events on
 * a component.
 * The class that is interested in processing a focus event
 * either implements this interface (and all the methods it
 * contains) or extends the abstract {@code FocusAdapter} class
 * (overriding only the methods of interest).
 * The listener object created from that class is then registered with a
 * component using the component's {@code addFocusListener}
 * method. When the component gains or loses the keyboard focus,
 * the relevant method in the listener object
 * is invoked, and the {@code FocusEvent} is passed to it.
 *
 * @see FocusAdapter
 * @see FocusEvent
 * @see <a href="https://docs.oracle.com/javase/tutorial/uiswing/events/focuslistener.html">Tutorial: Writing a Focus Listener</a>
 *
 * @author Carl Quinn
 * @since 1.1
 */
public interface FocusListener extends EventListener {

    /**
     * Invoked when a component gains the keyboard focus.
     * @param e the event to be processed
     */
    public void focusGained(FocusEvent e);

    /**
     * Invoked when a component loses the keyboard focus.
     * @param e the event to be processed
     */
    public void focusLost(FocusEvent e);
}
