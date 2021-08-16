/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.lwawt.macosx;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;

public final class CPrinterDevice extends GraphicsDevice {

    private final GraphicsConfiguration config;

    public CPrinterDevice(final CPrinterGraphicsConfig config) {
        this.config = config;
    }

    /**
     * Returns the type of this {@code GraphicsDevice}.
     * @return the type of this {@code GraphicsDevice}, which can
     * either be TYPE_RASTER_SCREEN, TYPE_PRINTER or TYPE_IMAGE_BUFFER.
     * @see #TYPE_RASTER_SCREEN
     * @see #TYPE_PRINTER
     * @see #TYPE_IMAGE_BUFFER
     */
    @Override
    public int getType() {
        return GraphicsDevice.TYPE_PRINTER;
    }

    /**
     * Returns the identification string associated with this
     * {@code GraphicsDevice}.
     * @return a {@code String} that is the identification
     * of this {@code GraphicsDevice}.
     */
    @Override
    public String getIDstring() {
        return ("Printer");
    }

    /**
     * Returns all of the {@code GraphicsConfiguration}
     * objects associated with this {@code GraphicsDevice}.
     * @return an array of {@code GraphicsConfiguration}
     * objects that are associated with this
     * {@code GraphicsDevice}.
     */
    @Override
    public GraphicsConfiguration[] getConfigurations() {
        return new GraphicsConfiguration[]{config};
    }

    /**
     * Returns the default {@code GraphicsConfiguration}
     * associated with this {@code GraphicsDevice}.
     * @return the default {@code GraphicsConfiguration}
     * of this {@code GraphicsDevice}.
     */
    @Override
    public GraphicsConfiguration getDefaultConfiguration() {
        return config;
    }
}
