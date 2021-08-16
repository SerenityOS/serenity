/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * Provides interfaces and classes for capture, processing, and playback of
 * sampled audio data.
 *
 * <h2>Related Documentation</h2>
 * For more information on using Java Sound see:
 * <ul>
 *   <li><a href="https://docs.oracle.com/javase/tutorial/sound/">
 *   The Java Sound Tutorial</a>
 * </ul>
 * Please note: In the {@code javax.sound.sampled} APIs, a {@code null}
 * reference parameter to methods is incorrect unless explicitly documented on
 * the method as having a meaningful interpretation. Usage to the contrary is
 * incorrect coding and may result in a run time exception either immediately or
 * at some later time. {@code NullPointerException} is an example of typical and
 * acceptable run time exception for such cases.
 *
 * @since 1.3
 */
package javax.sound.sampled;
