/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.text.html.parser;

import javax.swing.text.html.HTML;
/**
 * A generic HTML TagElement class. The methods define how white
 * space is interpreted around the tag.
 *
 * @author      Sunita Mani
 */

public class TagElement {

    Element elem;
    HTML.Tag htmlTag;
    boolean insertedByErrorRecovery;

    /**
     * Creates a generic HTML TagElement class with {@code fictional} equals to {@code false}.
     *
     * @param elem an element
     */
    public TagElement(Element elem) {
        this(elem, false);
    }

    /**
     * Creates a generic HTML TagElement class.
     *
     * @param elem an element
     * @param fictional if {@code true} the tag is inserted by error recovery.
     */
    public TagElement (Element elem, boolean fictional) {
        this.elem = elem;
        htmlTag = HTML.getTag(elem.getName());
        if (htmlTag == null) {
            htmlTag = new HTML.UnknownTag(elem.getName());
        }
        insertedByErrorRecovery = fictional;
    }

    /**
     * Returns {@code true} if this tag causes a
     * line break to the flow of data, otherwise returns
     * {@code false}.
     *
     * @return {@code true} if this tag causes a
     *   line break to the flow of data, otherwise returns
     *   {@code false}
     */
    public boolean breaksFlow() {
        return htmlTag.breaksFlow();
    }

    /**
     * Returns {@code true} if this tag is pre-formatted.
     *
     * @return {@code true} if this tag is pre-formatted,
     *   otherwise returns {@code false}
     */
    public boolean isPreformatted() {
        return htmlTag.isPreformatted();
    }

    /**
     * Returns the element.
     *
     * @return the element
     */
    public Element getElement() {
        return elem;
    }

    /**
     * Returns the tag constant corresponding to the name of the {@code element}
     *
     * @return the tag constant corresponding to the name of the {@code element}
     */
    public HTML.Tag getHTMLTag() {
        return htmlTag;
    }

    /**
     * Returns {@code true} if the tag is fictional.
     *
     * @return {@code true} if the tag is fictional.
     */
    public boolean fictional() {
        return insertedByErrorRecovery;
    }
}
