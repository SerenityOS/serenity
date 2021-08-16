/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

import jdk.javadoc.internal.doclets.toolkit.Content;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Content for the {@code <body>} element.
 *
 * The content is a {@code <div>} element that contains a
 * header that is always visible, and main content that
 * can be scrolled if necessary.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class BodyContents extends Content {

    private List<Content> mainContents = new ArrayList<>();
    private HtmlTree header = null;
    private HtmlTree footer = null;

    public BodyContents addMainContent(Content content) {
        mainContents.add(content);
        return this;
    }

    public BodyContents setHeader(HtmlTree header) {
        this.header = Objects.requireNonNull(header);
        return this;
    }

    public BodyContents setFooter(HtmlTree footer) {
        this.footer = footer;
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation always returns {@code false}.
     *
     * @return {@code false}
     */
    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean write(Writer out, boolean atNewline) throws IOException {
        return toContent().write(out, atNewline);
    }

    /**
     * Returns the HTML for the contents of the BODY element.
     *
     * @return the HTML
     */
    private Content toContent() {
        if (header == null)
            throw new NullPointerException();

        HtmlTree flexHeader = header.addStyle(HtmlStyle.flexHeader);

        HtmlTree flexContent = HtmlTree.DIV(HtmlStyle.flexContent)
                .add(HtmlTree.MAIN().add(mainContents))
                .add(footer == null ? HtmlTree.EMPTY : footer);

        return HtmlTree.DIV(HtmlStyle.flexBox)
                .add(flexHeader)
                .add(flexContent);
    }
}
