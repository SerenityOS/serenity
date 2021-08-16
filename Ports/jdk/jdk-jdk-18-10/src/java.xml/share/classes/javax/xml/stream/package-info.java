/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * <p>
 * Defines interfaces and classes for the Streaming API for XML (StAX).
 *
 * <p>
 * StAX provides two basic functions: the cursor API allowing users to
 * read and write XML efficiently, and the event iterator API promoting
 * ease of use that is event based, easy to extend and pipeline.
 * The event iterator API is intended to layer on top of the cursor API.
 *
 * <p>
 * The cursor API defines two interfaces: {@link XMLStreamReader}
 * and {@link XMLStreamWriter}, while the event iterator API defines:
 * {@link XMLEventReader} and {@link XMLEventWriter}.
 *
 * <p>
 * StAX supports plugability with {@link XMLInputFactory} and
 * {@link XMLOutputFactory} that define how an implementation is
 * located through a process as described in the {@code newFactory}
 * methods.
 *
 *
 * @since 1.6
 */

package javax.xml.stream;
