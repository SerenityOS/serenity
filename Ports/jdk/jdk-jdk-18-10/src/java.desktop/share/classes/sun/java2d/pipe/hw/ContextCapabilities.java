/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.pipe.hw;


/**
 * Represents a set of capabilities of a BufferedContext and associated
 * AccelGraphicsConfig.
 *
 * @see AccelGraphicsConfig
 */
public class ContextCapabilities {
    /** Indicates that the context has no capabilities. */
    public static final int CAPS_EMPTY             = (0 << 0);
    /** Indicates that the context supports RT surfaces with alpha channel. */
    public static final int CAPS_RT_PLAIN_ALPHA    = (1 << 1);
    /** Indicates that the context supports RT textures with alpha channel. */
    public static final int CAPS_RT_TEXTURE_ALPHA  = (1 << 2);
    /** Indicates that the context supports opaque RT textures. */
    public static final int CAPS_RT_TEXTURE_OPAQUE = (1 << 3);
    /** Indicates that the context supports multitexturing. */
    public static final int CAPS_MULTITEXTURE      = (1 << 4);
    /** Indicates that the context supports non-pow2 texture dimensions. */
    public static final int CAPS_TEXNONPOW2        = (1 << 5);
    /** Indicates that the context supports non-square textures. */
    public static final int CAPS_TEXNONSQUARE      = (1 << 6);
    /** Indicates that the context supports pixel shader 2.0 or better. */
    public static final int CAPS_PS20              = (1 << 7);
    /** Indicates that the context supports pixel shader 3.0 or better. */
    public static final int CAPS_PS30              = (1 << 8);
    /*
     *  Pipeline contexts should use this for defining pipeline-specific
     *  capabilities, for example:
     *    int CAPS_D3D_1 = (FIRST_PRIVATE_CAP << 0);
     *    int CAPS_D3D_2 = (FIRST_PRIVATE_CAP << 1);
     */
    protected static final int FIRST_PRIVATE_CAP   = (1 << 16);

    protected final int caps;
    protected final String adapterId;

    /**
     * Constructs a {@code ContextCapabilities} object.
     * @param caps an {@code int} representing the capabilities
     * @param adapterId {@code String} representing the name of the adapter, or null,
     * in which case the adapterId will be set to "unknown adapter".
     */
    protected ContextCapabilities(int caps, String adapterId) {
        this.caps = caps;
        this.adapterId = adapterId != null ? adapterId : "unknown adapter";
    }

    /**
     * Returns a string representing the name of the graphics adapter if such
     * could be determined. It is guaranteed to never return {@code null}.
     * @return string representing adapter id
     */
    public String getAdapterId() {
        return adapterId;
    }

    /**
     * Returns an {@code int} with capabilities (OR-ed constants defined in
     * this class and its pipeline-specific subclasses).
     * @return capabilities as {@code int}
     */
    public int getCaps() {
        return caps;
    }

    @Override
    public String toString() {
        StringBuilder sb =
            new StringBuilder("ContextCapabilities: adapter=" +
                             adapterId+", caps=");
        if (caps == CAPS_EMPTY) {
            sb.append("CAPS_EMPTY");
        } else {
            if ((caps & CAPS_RT_PLAIN_ALPHA) != 0) {
                sb.append("CAPS_RT_PLAIN_ALPHA|");
            }
            if ((caps & CAPS_RT_TEXTURE_ALPHA) != 0) {
                sb.append("CAPS_RT_TEXTURE_ALPHA|");
            }
            if ((caps & CAPS_RT_TEXTURE_OPAQUE) != 0) {
                sb.append("CAPS_RT_TEXTURE_OPAQUE|");
            }
            if ((caps & CAPS_MULTITEXTURE) != 0) {
                sb.append("CAPS_MULTITEXTURE|");
            }
            if ((caps & CAPS_TEXNONPOW2) != 0) {
                sb.append("CAPS_TEXNONPOW2|");
            }
            if ((caps & CAPS_TEXNONSQUARE) != 0) {
                sb.append("CAPS_TEXNONSQUARE|");
            }
            if ((caps & CAPS_PS20) != 0) {
                sb.append("CAPS_PS20|");
            }
            if ((caps & CAPS_PS30) != 0) {
                sb.append("CAPS_PS30|");
            }
        }
        return sb.toString();
    }
}
