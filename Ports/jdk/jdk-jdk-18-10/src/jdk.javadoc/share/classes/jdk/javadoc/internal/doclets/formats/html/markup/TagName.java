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

import jdk.javadoc.internal.doclets.toolkit.util.Utils;

/**
 * Enum representing the names for HTML elements.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 *
 * @see <a href="https://html.spec.whatwg.org/multipage/syntax.html#syntax-tag-name">WhatWG: Tag Name</a>
 * @see <a href="https://www.w3.org/TR/html51/syntax.html#tag-name">HTML 5.1: Tag Name</a>
 */
public enum TagName {
    A,
    BUTTON,
    BLOCKQUOTE,
    BODY,
    BR,
    CAPTION,
    CODE,
    DD,
    DIV,
    DL,
    DT,
    EM,
    FOOTER,
    H1,
    H2,
    H3,
    H4,
    H5,
    H6,
    HEAD,
    HEADER,
    HR,
    HTML,
    I,
    IMG,
    INPUT,
    LABEL,
    LI,
    LISTING,
    LINK,
    MAIN,
    MENU,
    META,
    NAV,
    NOSCRIPT,
    OL,
    P,
    PRE,
    SCRIPT,
    SECTION,
    SMALL,
    SPAN,
    STRONG,
    SUB,
    SUP,
    TABLE,
    TBODY,
    THEAD,
    TD,
    TH,
    TITLE,
    TR,
    UL,
    WBR;

    public final String value;

    TagName() {
        this.value = Utils.toLowerCase(name());
    }

    public String toString() {
        return value;
    }
}
