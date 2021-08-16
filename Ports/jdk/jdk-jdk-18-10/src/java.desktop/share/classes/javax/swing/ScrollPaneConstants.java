/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing;


/**
 * Constants used with the JScrollPane component.
 *
 * @author Hans Muller
 * @since 1.2
 */
public interface ScrollPaneConstants
{
    /**
     * Identifies a "viewport" or display area, within which
     * scrolled contents are visible.
     */
    String VIEWPORT = "VIEWPORT";
    /** Identifies a vertical scrollbar. */
    String VERTICAL_SCROLLBAR = "VERTICAL_SCROLLBAR";
    /** Identifies a horizontal scrollbar. */
    String HORIZONTAL_SCROLLBAR = "HORIZONTAL_SCROLLBAR";
    /**
     * Identifies the area along the left side of the viewport between the
     * upper left corner and the lower left corner.
     */
    String ROW_HEADER = "ROW_HEADER";
    /**
     * Identifies the area at the top the viewport between the
     * upper left corner and the upper right corner.
     */
    String COLUMN_HEADER = "COLUMN_HEADER";
    /** Identifies the lower left corner of the viewport. */
    String LOWER_LEFT_CORNER = "LOWER_LEFT_CORNER";
    /** Identifies the lower right corner of the viewport. */
    String LOWER_RIGHT_CORNER = "LOWER_RIGHT_CORNER";
    /** Identifies the upper left corner of the viewport. */
    String UPPER_LEFT_CORNER = "UPPER_LEFT_CORNER";
    /** Identifies the upper right corner of the viewport. */
    String UPPER_RIGHT_CORNER = "UPPER_RIGHT_CORNER";

    /** Identifies the lower leading edge corner of the viewport. The leading edge
     * is determined relative to the Scroll Pane's ComponentOrientation property.
     */
    String LOWER_LEADING_CORNER = "LOWER_LEADING_CORNER";
    /** Identifies the lower trailing edge corner of the viewport. The trailing edge
     * is determined relative to the Scroll Pane's ComponentOrientation property.
     */
    String LOWER_TRAILING_CORNER = "LOWER_TRAILING_CORNER";
    /** Identifies the upper leading edge corner of the viewport.  The leading edge
     * is determined relative to the Scroll Pane's ComponentOrientation property.
     */
    String UPPER_LEADING_CORNER = "UPPER_LEADING_CORNER";
    /** Identifies the upper trailing edge corner of the viewport. The trailing edge
     * is determined relative to the Scroll Pane's ComponentOrientation property.
     */
    String UPPER_TRAILING_CORNER = "UPPER_TRAILING_CORNER";

    /** Identifies the vertical scroll bar policy property. */
    String VERTICAL_SCROLLBAR_POLICY = "VERTICAL_SCROLLBAR_POLICY";
    /** Identifies the horizontal scroll bar policy property. */
    String HORIZONTAL_SCROLLBAR_POLICY = "HORIZONTAL_SCROLLBAR_POLICY";

    /**
     * Used to set the vertical scroll bar policy so that
     * vertical scrollbars are displayed only when needed.
     */
    int VERTICAL_SCROLLBAR_AS_NEEDED = 20;
    /**
     * Used to set the vertical scroll bar policy so that
     * vertical scrollbars are never displayed.
     */
    int VERTICAL_SCROLLBAR_NEVER = 21;
    /**
     * Used to set the vertical scroll bar policy so that
     * vertical scrollbars are always displayed.
     */
    int VERTICAL_SCROLLBAR_ALWAYS = 22;

    /**
     * Used to set the horizontal scroll bar policy so that
     * horizontal scrollbars are displayed only when needed.
     */
    int HORIZONTAL_SCROLLBAR_AS_NEEDED = 30;
    /**
     * Used to set the horizontal scroll bar policy so that
     * horizontal scrollbars are never displayed.
     */
    int HORIZONTAL_SCROLLBAR_NEVER = 31;
    /**
     * Used to set the horizontal scroll bar policy so that
     * horizontal scrollbars are always displayed.
     */
    int HORIZONTAL_SCROLLBAR_ALWAYS = 32;
}
