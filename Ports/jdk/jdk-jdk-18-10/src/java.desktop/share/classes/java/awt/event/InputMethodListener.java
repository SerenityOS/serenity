/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * The listener interface for receiving input method events. A text editing
 * component has to install an input method event listener in order to work
 * with input methods.
 *
 * <p>
 * The text editing component also has to provide an instance of InputMethodRequests.
 *
 * @author JavaSoft Asia/Pacific
 * @see InputMethodEvent
 * @see java.awt.im.InputMethodRequests
 * @since 1.2
 */
public interface InputMethodListener extends EventListener {

    /**
     * Invoked when the text entered through an input method has changed.
     * @param event the event to be processed
     */
    void inputMethodTextChanged(InputMethodEvent event);

    /**
     * Invoked when the caret within composed text has changed.
     * @param event the event to be processed
     */
    void caretPositionChanged(InputMethodEvent event);
}
