/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * A package of the Java Image I/O API dealing with synchronous notification of
 * events during the reading and writing of images.
 * <p>
 * The {@code IIOReadProgressListener} interface allows for notification of the
 * percentage of an image that has been read successfully.
 * <p>
 * The {@code IIOReadUpdateListener} interface allows for notification of the
 * portions of an image that have been read. This is useful, for example, for
 * implementing dynamic display of an image as it is loaded.
 * <p>
 * The {@code IIOReadWarningListener} interface allows for notification of
 * non-fatal errors during reading.
 * <p>
 * The {@code IIOWriteWarningListener} and {@code IIOWriteProgressListener}
 * interfaces perform analogous functions for writers.
 *
 * @since 1.4
 */
package javax.imageio.event;
