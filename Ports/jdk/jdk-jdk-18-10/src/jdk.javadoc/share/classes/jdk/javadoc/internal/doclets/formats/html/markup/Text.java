/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.formats.html.markup;

import java.io.IOException;
import java.io.Writer;

import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.util.DocletConstants;

/**
 * Class for containing immutable string content for HTML tags of javadoc output.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Text extends Content {
    private final String string;

    public static final Text EMPTY = Text.of("");

    /**
     * Creates a new object containing immutable text.
     *
     * @param content the text content
     * @return the object
     */
    public static Text of(CharSequence content) {
        return new Text(content);
    }

    /**
     * Constructs an immutable text object.
     *
     * @param content content for the object
     */
    private Text(CharSequence content) {
        string = Entity.escapeHtmlChars(content);
    }

    @Override
    public boolean isEmpty() {
        return string.isEmpty();
    }

    @Override
    public int charCount() {
        return RawHtml.charCount(string);
    }

    @Override
    public String toString() {
        return string;
    }

    @Override
    public boolean write(Writer out, boolean atNewline) throws IOException {
        out.write(string);
        return string.endsWith(DocletConstants.NL);
    }

}
