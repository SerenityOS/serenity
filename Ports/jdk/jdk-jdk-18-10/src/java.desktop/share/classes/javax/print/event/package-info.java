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
 * Package {@code javax.print.event} contains event classes and listener
 * interfaces.
 * <p>
 * They may be used to monitor both print services (such as printers going
 * on-line &amp; off-line), and the progress of a specific print job.
 * <p>
 * Please note: In the {@code javax.print} APIs, a {@code null} reference
 * parameter to methods is incorrect unless explicitly documented on the method
 * as having a meaningful interpretation. Usage to the contrary is incorrect
 * coding and may result in a run time exception either immediately or at some
 * later time. {@code IllegalArgumentException} and {@code NullPointerException}
 * are examples of typical and acceptable run time exceptions for such cases.
 *
 * @since 1.4
 */
package javax.print.event;
