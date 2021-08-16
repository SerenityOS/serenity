/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jdk.javadoc.internal.doclets.toolkit.Content;

/**
 * A sequence of Content nodes.
 */
public class ContentBuilder extends Content {
    protected List<Content> contents = Collections.emptyList();

    public ContentBuilder() { }

    public ContentBuilder(Content... contents) {
        for (Content c : contents) {
            add(c);
        }
    }

    @Override
    public ContentBuilder add(Content content) {
        Objects.requireNonNull(content);
        ensureMutableContents();
        if (content instanceof ContentBuilder cb) {
            contents.addAll(cb.contents);
        } else
            contents.add(content);
        return this;
    }

    @Override
    public ContentBuilder add(CharSequence text) {
        if (text.length() > 0) {
            ensureMutableContents();
            Content c = contents.isEmpty() ? null : contents.get(contents.size() - 1);
            TextBuilder tb;
            if (c instanceof TextBuilder tbi) {
                tb = tbi;
            } else {
                contents.add(tb = new TextBuilder());
            }
            tb.add(text);
        }
        return this;
    }

    @Override
    public boolean write(Writer writer, boolean atNewline) throws IOException {
        for (Content content: contents) {
            atNewline = content.write(writer, atNewline);
        }
        return atNewline;
    }

    @Override
    public boolean isEmpty() {
        for (Content content: contents) {
            if (!content.isEmpty())
                return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * A content builder is valid if any of its content is; thus, it is
     * valid to be added to an HtmlTree, which checks the validity of
     * each content in this builder.
     */
    @Override
    public boolean isValid() {
        for (Content content: contents) {
            if (content.isValid())
                return true;
        }
        return false;
    }

    @Override
    public int charCount() {
        int n = 0;
        for (Content c : contents)
            n += c.charCount();
        return n;
    }

    private void ensureMutableContents() {
        if (contents.isEmpty())
            contents = new ArrayList<>();
    }
}
