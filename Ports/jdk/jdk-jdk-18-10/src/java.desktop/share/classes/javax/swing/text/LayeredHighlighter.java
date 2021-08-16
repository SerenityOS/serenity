/*
 * Copyright (c) 1998, 2015, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.text;

import java.awt.Graphics;
import java.awt.Shape;

/**
 *
 * @author  Scott Violet
 * @author  Timothy Prinzing
 * @see     Highlighter
 */
public abstract class LayeredHighlighter implements Highlighter {
    /**
     * Constructor for subclasses to call.
     */
    protected LayeredHighlighter() {}

    /**
     * When leaf Views (such as LabelView) are rendering they should
     * call into this method. If a highlight is in the given region it will
     * be drawn immediately.
     *
     * @param g Graphics used to draw
     * @param p0 starting offset of view
     * @param p1 ending offset of view
     * @param viewBounds Bounds of View
     * @param editor JTextComponent
     * @param view View instance being rendered
     */
    public abstract void paintLayeredHighlights(Graphics g, int p0, int p1,
                                                Shape viewBounds,
                                                JTextComponent editor,
                                                View view);


    /**
     * Layered highlight renderer.
     */
    public abstract static class LayerPainter implements Highlighter.HighlightPainter {
        /**
         * Constructor for subclasses to call.
         */
        protected LayerPainter() {}

        /**
         * @return a shape
         * @param g Graphics used to draw
         * @param p0 starting offset of view
         * @param p1 ending offset of view
         * @param viewBounds Bounds of View
         * @param editor JTextComponent
         * @param view View instance being rendered
         */
        public abstract Shape paintLayer(Graphics g, int p0, int p1,
                                        Shape viewBounds,JTextComponent editor,
                                        View view);
    }
}
