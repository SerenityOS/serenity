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

/**
 * Provides classes and interfaces for handling text, dates, numbers,
 * and messages in a manner independent of natural languages.  This
 * means your main application or applet can be written to be
 * language-independent, and it can rely upon separate,
 * dynamically-linked localized resources. This allows the flexibility
 * of adding localizations for new localizations at any time.
 *
 * <p>These classes are capable of formatting dates, numbers, and
 * messages, parsing; searching and sorting strings; and iterating
 * over characters, words, sentences, and line breaks.  This package
 * contains three main groups of classes and interfaces:
 *
 * <ul>
 * <li>Classes for iteration over text
 * <li>Classes for formatting and parsing
 * <li>Classes for string collation
 * </ul>
 *
 * @since 1.1
 */
package java.text;
