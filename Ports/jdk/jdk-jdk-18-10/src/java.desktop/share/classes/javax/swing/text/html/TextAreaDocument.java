/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.text.html;

import javax.swing.text.*;


/**
 * TextAreaDocument extends the capabilities of the PlainDocument
 * to store the data that is initially set in the Document.
 * This is stored in order to enable an accurate reset of the
 * state when a reset is requested.
 *
 * @author Sunita Mani
 */
@SuppressWarnings("serial") // Superclass is not serializable across versions
class TextAreaDocument extends PlainDocument {

    String initialText;


    /**
     * Resets the model by removing all the data,
     * and restoring it to its initial state.
     */
    void reset() {
        try {
            remove(0, getLength());
            if (initialText != null) {
                insertString(0, initialText, null);
            }
        } catch (BadLocationException e) {
        }
    }

    /**
     * Stores the data that the model is initially
     * loaded with.
     */
    void storeInitialText() {
        try {
            initialText = getText(0, getLength());
        } catch (BadLocationException e) {
        }
    }
}
