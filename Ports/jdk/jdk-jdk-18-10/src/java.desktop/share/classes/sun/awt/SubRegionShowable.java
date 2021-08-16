/*
 * Copyright (c) 2005, 2008, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt;

/**
 * Interface used by Swing to make copies from the Swing back buffer
 * more optimal when using BufferStrategy; no need to copy the entire
 * buffer when only a small sub-region has changed.
 * @see javax.swing.BufferStrategyPaintManager
 *
 */
public interface SubRegionShowable {
    /**
     * Shows the specific subregion.
     */
    public void show(int x1, int y1, int x2, int y2);

    /**
     * Shows the specified region if the buffer is not lost and the dimensions
     * of the back-buffer match those of the component.
     *
     * @return true if successful
     */
    // NOTE: this is invoked by swing on the toolkit thread!
    public boolean showIfNotLost(int x1, int y1, int x2, int y2);
}
