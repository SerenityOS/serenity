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
package java.awt;

import java.awt.event.KeyEvent;


/**
 * A KeyEventDispatcher cooperates with the current KeyboardFocusManager in the
 * targeting and dispatching of all KeyEvents. KeyEventDispatchers registered
 * with the current KeyboardFocusManager will receive KeyEvents before they are
 * dispatched to their targets, allowing each KeyEventDispatcher to retarget
 * the event, consume it, dispatch the event itself, or make other changes.
 * <p>
 * Note that KeyboardFocusManager itself implements KeyEventDispatcher. By
 * default, the current KeyboardFocusManager will be the sink for all KeyEvents
 * not dispatched by the registered KeyEventDispatchers. The current
 * KeyboardFocusManager cannot be completely deregistered as a
 * KeyEventDispatcher. However, if a KeyEventDispatcher reports that it
 * dispatched the KeyEvent, regardless of whether it actually did so, the
 * KeyboardFocusManager will take no further action with regard to the
 * KeyEvent. (While it is possible for client code to register the current
 * KeyboardFocusManager as a KeyEventDispatcher one or more times, this is
 * usually unnecessary and not recommended.)
 *
 * @author David Mendenhall
 *
 * @see KeyboardFocusManager#addKeyEventDispatcher
 * @see KeyboardFocusManager#removeKeyEventDispatcher
 * @since 1.4
 */
@FunctionalInterface
public interface KeyEventDispatcher {

    /**
     * This method is called by the current KeyboardFocusManager requesting
     * that this KeyEventDispatcher dispatch the specified event on its behalf.
     * This KeyEventDispatcher is free to retarget the event, consume it,
     * dispatch it itself, or make other changes. This capability is typically
     * used to deliver KeyEvents to Components other than the focus owner. This
     * can be useful when navigating children of non-focusable Windows in an
     * accessible environment, for example. Note that if a KeyEventDispatcher
     * dispatches the KeyEvent itself, it must use {@code redispatchEvent}
     * to prevent the current KeyboardFocusManager from recursively requesting
     * that this KeyEventDispatcher dispatch the event again.
     * <p>
     * If an implementation of this method returns {@code false}, then
     * the KeyEvent is passed to the next KeyEventDispatcher in the chain,
     * ending with the current KeyboardFocusManager. If an implementation
     * returns {@code true}, the KeyEvent is assumed to have been
     * dispatched (although this need not be the case), and the current
     * KeyboardFocusManager will take no further action with regard to the
     * KeyEvent. In such a case,
     * {@code KeyboardFocusManager.dispatchEvent} should return
     * {@code true} as well. If an implementation consumes the KeyEvent,
     * but returns {@code false}, the consumed event will still be passed
     * to the next KeyEventDispatcher in the chain. It is important for
     * developers to check whether the KeyEvent has been consumed before
     * dispatching it to a target. By default, the current KeyboardFocusManager
     * will not dispatch a consumed KeyEvent.
     *
     * @param e the KeyEvent to dispatch
     * @return {@code true} if the KeyboardFocusManager should take no
     *         further action with regard to the KeyEvent; {@code false}
     *         otherwise
     * @see KeyboardFocusManager#redispatchEvent
     */
    boolean dispatchKeyEvent(KeyEvent e);
}
