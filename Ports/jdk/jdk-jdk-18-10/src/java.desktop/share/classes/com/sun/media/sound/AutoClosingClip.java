/*
 * Copyright (c) 2002, 2007, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.media.sound;

import javax.sound.sampled.Clip;

/**
 * Interface for Clip objects that close themselves automatically.
 *
 * @author Florian Bomers
 */
interface AutoClosingClip extends Clip {

    /**
     * Indicates whether this clip instance is auto closing.
     * When the clip is auto closing, it calls the close()
     * method automatically after a short period of inactivity.<br>
     * <br>
     *
     * @return true if this clip is auto closing
     */
    boolean isAutoClosing();

    /**
     * Sets whether this Clip instance is auto closing or not.
     * If true, the close() method will be called automatically
     * after a short period of inactivity.
     *
     * @param value - true to set this clip to auto closing
     */
    void setAutoClosing(boolean value);
}
