/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * Provides the interfaces for the Document Object Model (DOM). Supports the
 * <a href="http://www.w3.org/TR/DOM-Level-2-Core/">
 *     Document Object Model (DOM) Level 2 Core Specification</a>,
 * <a href="http://www.w3.org/TR/DOM-Level-3-Core">
 *     Document Object Model (DOM) Level 3 Core Specification</a>,
 * and <a href="http://www.w3.org/TR/DOM-Level-3-LS">
 *     Document Object Model (DOM) Level 3 Load and Save Specification</a>.
 *
 * @apiNote
 * The documentation comments for the get and set methods within this API are
 * written as property definitions and are shared between both methods. These
 * methods do not follow the standard Java SE specification format.
 *
 * <p>
 * Take the {@link org.w3c.dom.Node Node} TextContent property as an example, both
 * {@link org.w3c.dom.Node#getTextContent() getTextContent} and
 * {@link org.w3c.dom.Node#setTextContent(String) setTextContent} shared the same
 * content that defined the TextContent property itself.
 *
 *
 * @since 1.4
 */

package org.w3c.dom;
