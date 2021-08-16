/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.formats.html;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jdk.javadoc.internal.doclets.formats.html.markup.ContentBuilder;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlTree;
import jdk.javadoc.internal.doclets.toolkit.Content;

/**
 * A row of header cells for an HTML table.
 *
 * The header contains a list of {@code <th>} cells, providing the column headers.
 * The attribute {@code scope="col"} is automatically added to each header cell.
 * In addition, a series of style class names can be specified, to be applied one per cell.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class TableHeader extends Content {

    /**
     * The content to be put in each of the {@code <th>} cells in the header row.
     */
    private final List<Content> cellContents;
    /**
     * The style class names for each of the {@code <th>} cells in the header row.
     * If not set, default style names will be used.
     */
    private List<HtmlStyle> styles;

    /**
     * Creates a header row, with localized content for each cell.
     * Resources keys will be converted to content using {@link Contents#getContent(String)}.
     * @param contents a factory to get the content for each header cell.
     * @param colHeaderKeys the resource keys for the content in each cell.
     */
    public TableHeader(Contents contents, String... colHeaderKeys) {
        this.cellContents = Arrays.stream(colHeaderKeys)
                .map(contents::getContent)
                .toList();
    }

    /**
     * Creates a header row, with specified content for each cell.
     * @param headerCellContents a content object for each header cell
     */
    public TableHeader(Content... headerCellContents) {
        this.cellContents = Arrays.asList(headerCellContents);
    }

    /**
     * Creates a header row, with specified content for each cell.
     * @param headerCellContents a content object for each header cell
     */
    public TableHeader(List<Content> headerCellContents) {
        this.cellContents = headerCellContents;
    }

    /**
     * Set the style class names for each header cell.
     * The number of names must match the number of cells given to the constructor.
     * @param styles the style class names
     * @return this object
     */
    public TableHeader styles(HtmlStyle... styles) {
        if (styles.length != cellContents.size()) {
            throw new IllegalStateException();
        }
        this.styles = Arrays.asList(styles);
        return this;
    }

    /**
     * Set the style class names for each header cell.
     * The number of names must match the number of cells given to the constructor.
     * @param styles the style class names
     * @return this object
     */
    public TableHeader styles(List<HtmlStyle> styles) {
        if (styles.size() != cellContents.size()) {
            throw new IllegalStateException();
        }
        this.styles = styles;
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
     * Converts this header to a {@link Content} object, for use in an {@link HtmlTree}.
     * @return a Content object
     */
    private Content toContent() {
        Content header = new ContentBuilder();
        int i = 0;
        for (Content cellContent : cellContents) {
            HtmlStyle style = (styles != null) ? styles.get(i)
                    : (i == 0) ? HtmlStyle.colFirst
                    : (i == (cellContents.size() - 1)) ? HtmlStyle.colLast
                    : (i == 1) ? HtmlStyle.colSecond : null;
            HtmlTree cell = HtmlTree.DIV(HtmlStyle.tableHeader, cellContent);
            if (style != null) {
                cell.addStyle(style);
            }
            header.add(cell);
            i++;
        }
        return header;
    }

}
