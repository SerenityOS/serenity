/*
 * Copyright (c) 1995, 2013, Oracle and/or its affiliates. All rights reserved.
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
package java.awt.peer;

import java.awt.TextComponent;
import java.awt.im.InputMethodRequests;

/**
 * The peer interface for {@link TextComponent}.
 *
 * The peer interfaces are intended only for use in porting
 * the AWT. They are not intended for use by application
 * developers, and developers should not implement peers
 * nor invoke any of the peer methods directly on the peer
 * instances.
 */
public interface TextComponentPeer extends ComponentPeer {

    /**
     * Sets if the text component should be editable or not.
     *
     * @param editable {@code true} for editable text components,
     *        {@code false} for non-editable text components
     *
     * @see TextComponent#setEditable(boolean)
     */
    void setEditable(boolean editable);

    /**
     * Returns the current content of the text component.
     *
     * @return the current content of the text component
     *
     * @see TextComponent#getText()
     */
    String getText();

    /**
     * Sets the content for the text component.
     *
     * @param text the content to set
     *
     * @see TextComponent#setText(String)
     */
    void setText(String text);

    /**
     * Returns the start index of the current selection.
     *
     * @return the start index of the current selection
     *
     * @see TextComponent#getSelectionStart()
     */
    int getSelectionStart();

    /**
     * Returns the end index of the current selection.
     *
     * @return the end index of the current selection
     *
     * @see TextComponent#getSelectionEnd()
     */
    int getSelectionEnd();

    /**
     * Selects an area of the text component.
     *
     * @param selStart the start index of the new selection
     * @param selEnd the end index of the new selection
     *
     * @see TextComponent#select(int, int)
     */
    void select(int selStart, int selEnd);

    /**
     * Sets the caret position of the text component.
     *
     * @param pos the caret position to set
     *
     * @see TextComponent#setCaretPosition(int)
     */
    void setCaretPosition(int pos);

    /**
     * Returns the current caret position.
     *
     * @return the current caret position
     *
     * @see TextComponent#getCaretPosition()
     */
    int getCaretPosition();

    /**
     * Returns the input method requests.
     *
     * @return the input method requests
     */
    InputMethodRequests getInputMethodRequests();
}
