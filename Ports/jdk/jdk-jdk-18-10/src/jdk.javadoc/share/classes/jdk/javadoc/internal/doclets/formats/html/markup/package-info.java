/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 *  This package contains classes that create and write HTML markup tags.
 *
 *  <p>The primary low level classes are
 *  {@link jdk.javadoc.internal.doclets.formats.html.markup.HtmlTree HtmlTree}
 *  and other subtypes of {@code Content}. In addition, there are mid-level builders
 *  like {@link jdk.javadoc.internal.doclets.formats.html.TableHeader TableHeader}
 *  and {@link jdk.javadoc.internal.doclets.formats.html.Table Table}
 *  to help build more complex HTML trees.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 *
 * @see <a href="https://html.spec.whatwg.org/multipage/">HTML: Living Standard</a>
 * @see <a href="https://html.spec.whatwg.org/multipage/syntax,html">HTML: Living Standard: The HTML Syntax</a>
 * @see <a href="https://www.w3.org/TR/html51/">HTML 5.1</a>
 */
package jdk.javadoc.internal.doclets.formats.html.markup;
