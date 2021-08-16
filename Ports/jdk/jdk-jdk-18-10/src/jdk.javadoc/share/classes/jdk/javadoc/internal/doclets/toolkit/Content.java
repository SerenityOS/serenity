/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.toolkit;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * A class to create content for javadoc output pages.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public abstract class Content {

    /**
     * Returns a string representation of the content.
     *
     * @return string representation of the content
     */
    @Override
    public String toString() {
        StringWriter out = new StringWriter();
        try {
            write(out, true);
        } catch (IOException e) {
            // cannot happen from StringWriter
            throw new AssertionError(e);
        }
        return out.toString();
    }

    /**
     * Adds content to the existing content.
     * This is an optional operation.
     *
     * @implSpec This implementation throws {@linkplain UnsupportedOperationException}.
     *
     * @param content content to be added
     * @return this object
     * @throws UnsupportedOperationException if this operation is not supported by
     *                                       a particular implementation
     * @throws IllegalArgumentException      if the content is not suitable to be added
     */
    public Content add(Content content) {
        throw new UnsupportedOperationException();
    }

    /**
     * Adds a string content to the existing content.
     * This is an optional operation.
     *
     * @implSpec
     * This implementation throws {@linkplain UnsupportedOperationException}.
     *
     * @param stringContent the string content to be added
     * @return this object
     * @throws UnsupportedOperationException if this operation is not supported by
     *                                       a particular implementation
     * @throws IllegalArgumentException      if the content is not suitable to be added
     */
    public Content add(CharSequence stringContent) {
        throw new UnsupportedOperationException();
    }

    /**
     * Writes content to a writer.
     *
     * @param writer the writer
     * @param atNewline whether the writer has just written a newline
     * @return  whether the writer has just written a newline
     * @throws IOException if an error occurs while writing the output
     */
    public abstract boolean write(Writer writer, boolean atNewline) throws IOException ;

    /**
     * Returns true if the content is empty.
     *
     * @return true if no content to be displayed else return false
     */
    public abstract boolean isEmpty();

    /**
     * Returns true if the content is valid. This allows filtering during
     * {@link #add(Content) addition}.
     *
     * @return true if the content is valid else return false
     */
    public boolean isValid() {
        return !isEmpty();
    }

    /**
     * Return the number of characters of plain text content in this object
     * (optional operation.)
     * @return the number of characters of plain text content in this
     */
    public int charCount() {
        return 0;
    }
}
