/*
 * Copyright (c) 2002, 2015, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.plaf.synth;

import java.awt.*;
import javax.swing.*;

/**
 * An icon that is passed a {@code SynthContext}. Subclasses need only implement
 * the variants that take a {@code SynthContext}, but must be prepared for the
 * {@code SynthContext} to be null.
 *
 * @author Scott Violet
 */
public interface SynthIcon extends Icon {

    /**
     * Paints the icon at the specified location for the given synth context.
     *
     * @param context identifies hosting region, may be null.
     * @param g the graphics context
     * @param x the x location to paint to
     * @param y the y location to paint to
     * @param width the width of the region to paint to, may be 0
     * @param height the height of the region to paint to, may be 0
     */
    void paintIcon(SynthContext context, Graphics g, int x, int y,
            int width, int height);

    /**
     * Returns the icon's width for the given synth context.
     *
     * @param context {@code SynthContext} requesting the Icon, may be null.
     * @return an int specifying the width of the icon.
     */
    int getIconWidth(SynthContext context);

    /**
     * Returns the icon's height for the given synth context.
     *
     * @param context {@code SynthContext} requesting the Icon, may be null.
     * @return an int specifying the height of the icon.
     */
    int getIconHeight(SynthContext context);

    @Override
    default void paintIcon(Component c, Graphics g, int x, int y) {
        paintIcon(null, g, x, y, getIconWidth(), getIconHeight());
    }

    @Override
    default int getIconWidth() {
        return getIconWidth(null);
    }

    @Override
    default int getIconHeight() {
        return getIconHeight(null);
    }
}
