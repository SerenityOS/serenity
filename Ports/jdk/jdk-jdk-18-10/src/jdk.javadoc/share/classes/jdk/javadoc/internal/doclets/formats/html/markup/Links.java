/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
import jdk.javadoc.internal.doclets.toolkit.util.DocLink;
import jdk.javadoc.internal.doclets.toolkit.util.DocPath;

/**
 * Factory for HTML A elements: links (with a {@code href} attribute).
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Links {

    private final DocPath file;

    /**
     * Creates a {@code Links} object for a specific file.
     * Links to other files will be made relative to this file where possible.
     *
     * @param file the file
     */
    public Links(DocPath file) {
        this.file = file;
    }

    /**
     * Creates a link of the form {@code <a href="#id">label</a>}.
     *
     * @param id    the position of the link in the file
     * @param label the content for the link
     * @return a content tree for the link
     */
    public Content createLink(HtmlId id, Content label) {
        DocLink l = DocLink.fragment(id.name());
        return createLink(l, label, "");
    }

    /**
     * Creates a link of the form {@code <a href="#id">label</a>} if {@code link}
     * is {@code true}, or else just returns {@code label}.
     *
     * @param id    the position of the link in the file
     * @param label the content for the link
     * @param link  whether to create a link or just return the label
     * @return a content tree for the link or just the label
     */
    public Content createLink(HtmlId id, Content label, boolean link) {
        return link ? createLink(id, label) : label;
    }

    /**
     * Creates a link of the form {@code <a href="#id" title="title">label</a>}.
     *
     * @param id     the id to which the link will be created
     * @param label  the content for the link
     * @param title  the title for the link
     *
     * @return a content tree for the link
     */
    public Content createLink(HtmlId id, Content label, String title) {
        DocLink l = DocLink.fragment(id.name());
        return createLink(l, label, title);
    }

    /**
     * Creates a link of the form {@code <a href="path">label</a>}.
     *
     * @param path   the path for the link
     * @param label  the content for the link
     * @return a content tree for the link
     */
    public Content createLink(DocPath path, String label) {
        return createLink(path, Text.of(label), null, "");
    }

    /**
     * Creates a link of the form {@code <a href="path">label</a>}.
     *
     * @param path   the path for the link
     * @param label  the content for the link
     * @return a content tree for the link
     */
    public Content createLink(DocPath path, Content label) {
        return createLink(path, label, "");
    }

    /**
     * Creates a link of the form {@code <a href="path" title="title">label</a>}.
     * If {@code style} is not null, it will be added as {@code class="style"} to the link.
     *
     * @param path      the path for the link
     * @param label     the content for the link
     * @param style     the style for the link, or null
     * @param title     the title for the link
     * @return a content tree for the link
     */
    public Content createLink(DocPath path, Content label, HtmlStyle style, String title) {
        return createLink(new DocLink(path), label, style, title);
    }

    /**
     * Creates a link of the form {@code <a href="path" title="title">label</a>}.
     *
     * @param path      the path for the link
     * @param label     the content for the link
     * @param title     the title for the link
     * @return a content tree for the link
     */
    public Content createLink(DocPath path, Content label, String title) {
        return createLink(new DocLink(path), label, title);
    }

    /**
     * Creates a link of the form {@code <a href="link">label</a>}.
     *
     * @param link      the details for the link
     * @param label     the content for the link
     * @return a content tree for the link
     */
    public Content createLink(DocLink link, Content label) {
        return createLink(link, label, "");
    }

    /**
     * Creates a link of the form {@code <a href="path" title="title">label</a>}.
     *
     * @param link      the details for the link
     * @param label     the content for the link
     * @param title     the title for the link
     * @return a content tree for the link
     */
    public Content createLink(DocLink link, Content label, String title) {
        HtmlTree anchor = HtmlTree.A(link.relativizeAgainst(file).toString(), label);
        if (title != null && title.length() != 0) {
            anchor.put(HtmlAttr.TITLE, title);
        }
        return anchor;
    }

    /**
     * Creates a link of the form {@code <a href="link" title="title" >label</a>}.
     * If {@code style} is not null, it will be added as {@code class="style"} to the link.
     *
     * @param link      the details for the link
     * @param label     the content for the link
     * @param style     the style for the link, or null
     * @param title     the title for the link
     * @return a content tree for the link
     */
    public Content createLink(DocLink link, Content label, HtmlStyle style,
                              String title) {
        return createLink(link, label, style, title, false);
    }

    /**
     * Creates a link of the form {@code <a href="link" title="title">label</a>}.
     * If {@code style} is not null, it will be added as {@code class="style"} to the link.
     *
     * @param link       the details for the link
     * @param label      the content for the link
     * @param style      the style for the link, or null
     * @param title      the title for the link
     * @param isExternal is the link external to the generated documentation
     * @return a content tree for the link
     */
    public Content createLink(DocLink link, Content label, HtmlStyle style,
                              String title, boolean isExternal) {
        HtmlTree l = HtmlTree.A(link.relativizeAgainst(file).toString(), label);
        if (style != null) {
            l.setStyle(style);
        }
        if (title != null && title.length() != 0) {
            l.put(HtmlAttr.TITLE, title);
        }
        if (isExternal) {
            // Use addStyle as external links might have an explicit style set above as well.
            l.addStyle(HtmlStyle.externalLink);
        }
        return l;
    }

    /**
     * Creates an external link.
     *
     * @param link       the details for the link
     * @param label      the content for the link
     * @return a content tree for the link
     */
    public Content createExternalLink(DocLink link, Content label) {
        return HtmlTree.A(link.relativizeAgainst(file).toString(), label)
            .setStyle(HtmlStyle.externalLink);
    }
}
