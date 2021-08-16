/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * <b>[JDK INTERNAL]</b>
 * The {@code sun.util.logging.internal} package defines an internal
 * implementation of the {@link jdk.internal.logger.DefaultLoggerFinder} which
 * provides an extension of the {@link java.lang.System.Logger System.Logger}
 * interface making it easy to bridge from {@link java.util.logging};
 * the JDK default implementation of the LoggerFinder will return loggers
 * implementing this extension when {@code java.util.logging} is present.
 * </p>
 * <p>
 * When {@code java.util.logging} is present, Logger instances returned by
 * the JDK default implementation of the LoggerFinder
 * wrap an instance of {@link java.util.logging.Logger java.util.logging.Logger}
 * and implement the {@link
 * sun.util.logging.PlatformLogger.Bridge PlatformLogger.Bridge}
 * extension, overriding all the methods defined in
 * that extension in order to call the corresponding methods on their wrapped
 * {@linkplain java.util.logging.Logger backend Logger} instance.
 * <p>
 * <br>
 * @see java.lang.System.LoggerFinder
 * @see java.lang.System.Logger
 * @see sun.util.logging.PlatformLogger
 * @see sun.util.logging.PlatformLogger.Bridge
 * @see jdk.internal.logger
 *
 * @since 9
 */
package sun.util.logging.internal;
