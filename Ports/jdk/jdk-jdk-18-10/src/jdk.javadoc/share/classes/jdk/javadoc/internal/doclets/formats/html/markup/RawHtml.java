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

package jdk.javadoc.internal.doclets.formats.html.markup;

import java.io.IOException;
import java.io.Writer;

import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.util.DocletConstants;

/**
 * Class for generating raw HTML content to be added to HTML pages of javadoc output.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class RawHtml extends Content {

    private final String rawHtmlContent;

    /**
     * Constructor to construct a RawHtml object.
     *
     * @param rawHtml raw HTML text to be added
     */
    public RawHtml(CharSequence rawHtml) {
        rawHtmlContent = rawHtml.toString();
    }

    @Override
    public boolean isEmpty() {
        return rawHtmlContent.isEmpty();
    }

    @Override
    public String toString() {
        return rawHtmlContent;
    }

    private enum State { TEXT, ENTITY, TAG, STRING }

    @Override
    public int charCount() {
        return charCount(rawHtmlContent);
    }

    static int charCount(CharSequence htmlText) {
        State state = State.TEXT;
        int count = 0;
        for (int i = 0; i < htmlText.length(); i++) {
            char c = htmlText.charAt(i);
            switch (state) {
                case TEXT:
                    switch (c) {
                        case '<':
                            state = State.TAG;
                            break;
                        case '&':
                            state = State.ENTITY;
                            count++;
                            break;
                        case '\r':
                        case '\n':
                            // Windows uses "\r\n" as line separator while UNIX uses "\n".
                            // Ignore line separators to get consistent results across platforms.
                            break;
                        default:
                            count++;
                    }
                    break;

                case ENTITY:
                    if (!Character.isLetterOrDigit(c))
                        state = State.TEXT;
                    break;

                case TAG:
                    switch (c) {
                        case '"':
                            state = State.STRING;
                            break;
                        case '>':
                            state = State.TEXT;
                            break;
                    }
                    break;

                case STRING:
                    switch (c) {
                        case '"':
                            state = State.TAG;
                            break;
                    }
            }
        }
        return count;
    }

    @Override
    public boolean write(Writer out, boolean atNewline) throws IOException {
        out.write(rawHtmlContent);
        return rawHtmlContent.endsWith(DocletConstants.NL);
    }
}
