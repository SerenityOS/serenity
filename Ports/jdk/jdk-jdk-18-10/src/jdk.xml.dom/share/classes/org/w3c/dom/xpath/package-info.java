/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * Provides interfaces for DOM Level 3 XPath Specification. The XPath module
 * provides simple functionalities to access a DOM tree using
 * <a href='https://www.w3.org/TR/1999/REC-xpath-19991116/'>XPath 1.0</a>.
 * <p>
 * The interfaces and classes in this package came from
 * Document Object Model (DOM) Level 3 XPath Specification,
 * Working Draft 20 August 2002. Refer to
 * <a href='https://www.w3.org/TR/DOM-Level-3-XPath/'>
 * Document Object Model (DOM) Level 3 XPath Specification, Version 1.0,
 * W3C Working Group Note 26 February 2004</a> except that the values of
 * {@link XPathException#INVALID_EXPRESSION_ERR} and {@link XPathException#TYPE_ERR}
 * are 1 and 2 respectively (instead of 51 and 52).
 *
 * @since 1.4
 */

package org.w3c.dom.xpath;
