/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * (C) Copyright IBM Corp. 1998 - All Rights Reserved
 *
 * The original version of this source code and documentation is copyrighted
 * and owned by IBM, Inc. These materials are provided under terms of a
 * License Agreement between IBM and Sun. This technology is protected by
 * multiple US and International patents. This notice and attribution to IBM
 * may not be removed.
 *
 */

package java.awt;

import java.io.Serial;
import java.util.Locale;
import java.util.ResourceBundle;

/**
  * The ComponentOrientation class encapsulates the language-sensitive
  * orientation that is to be used to order the elements of a component
  * or of text. It is used to reflect the differences in this ordering
  * between Western alphabets, Middle Eastern (such as Hebrew), and Far
  * Eastern (such as Japanese).
  * <p>
  * Fundamentally, this governs items (such as characters) which are laid out
  * in lines, with the lines then laid out in a block. This also applies
  * to items in a widget: for example, in a check box where the box is
  * positioned relative to the text.
  * <p>
  * There are four different orientations used in modern languages
  * as in the following table.<br>
  * <pre>
  * LT          RT          TL          TR
  * A B C       C B A       A D G       G D A
  * D E F       F E D       B E H       H E B
  * G H I       I H G       C F I       I F C
  * </pre><br>
  * (In the header, the two-letter abbreviation represents the item direction
  * in the first letter, and the line direction in the second. For example,
  * LT means "items left-to-right, lines top-to-bottom",
  * TL means "items top-to-bottom, lines left-to-right", and so on.)
  * <p>
  * The orientations are:
  * <ul>
  * <li>LT - Western Europe (optional for Japanese, Chinese, Korean)
  * <li>RT - Middle East (Arabic, Hebrew)
  * <li>TR - Japanese, Chinese, Korean
  * <li>TL - Mongolian
  * </ul>
  * Components whose view and controller code depends on orientation
  * should use the {@code isLeftToRight()} and
  * {@code isHorizontal()} methods to
  * determine their behavior. They should not include switch-like
  * code that keys off of the constants, such as:
  * <pre>
  * if (orientation == LEFT_TO_RIGHT) {
  *   ...
  * } else if (orientation == RIGHT_TO_LEFT) {
  *   ...
  * } else {
  *   // Oops
  * }
  * </pre>
  * This is unsafe, since more constants may be added in the future and
  * since it is not guaranteed that orientation objects will be unique.
  */
public final class ComponentOrientation implements java.io.Serializable
{
    /**
     * Use serialVersionUID from JDK 1.6 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -4113291392143563828L;

    // Internal constants used in the implementation
    private static final int UNK_BIT      = 1;
    private static final int HORIZ_BIT    = 2;
    private static final int LTR_BIT      = 4;

    /**
     * Items run left to right and lines flow top to bottom
     * Examples: English, French.
     */
    public static final ComponentOrientation LEFT_TO_RIGHT =
                    new ComponentOrientation(HORIZ_BIT|LTR_BIT);

    /**
     * Items run right to left and lines flow top to bottom
     * Examples: Arabic, Hebrew.
     */
    public static final ComponentOrientation RIGHT_TO_LEFT =
                    new ComponentOrientation(HORIZ_BIT);

    /**
     * Indicates that a component's orientation has not been set.
     * To preserve the behavior of existing applications,
     * isLeftToRight will return true for this value.
     */
    public static final ComponentOrientation UNKNOWN =
                    new ComponentOrientation(HORIZ_BIT|LTR_BIT|UNK_BIT);

    /**
     * Are lines horizontal?
     * This will return true for horizontal, left-to-right writing
     * systems such as Roman.
     *
     * @return {@code true} if this orientation has horizontal lines
     */
    public boolean isHorizontal() {
        return (orientation & HORIZ_BIT) != 0;
    }

    /**
     * HorizontalLines: Do items run left-to-right?<br>
     * Vertical Lines:  Do lines run left-to-right?<br>
     * This will return true for horizontal, left-to-right writing
     * systems such as Roman.
     *
     * @return {@code true} if this orientation is left-to-right
     */
    public boolean isLeftToRight() {
        return (orientation & LTR_BIT) != 0;
    }

    /**
     * Returns the orientation that is appropriate for the given locale.
     *
     * @param locale the specified locale
     * @return the orientation for the locale
     */
    public static ComponentOrientation getOrientation(Locale locale) {
        // A more flexible implementation would consult a ResourceBundle
        // to find the appropriate orientation.  Until pluggable locales
        // are introduced however, the flexibility isn't really needed.
        // So we choose efficiency instead.
        return switch (locale.getLanguage()) {
            case "ar", "fa", "he", "iw", "ji", "ur", "yi" -> RIGHT_TO_LEFT;
            default -> LEFT_TO_RIGHT;
        };
    }

    /**
     * Returns the orientation appropriate for the given ResourceBundle's
     * localization.  Three approaches are tried, in the following order:
     * <ol>
     * <li>Retrieve a ComponentOrientation object from the ResourceBundle
     *      using the string "Orientation" as the key.
     * <li>Use the ResourceBundle.getLocale to determine the bundle's
     *      locale, then return the orientation for that locale.
     * <li>Return the default locale's orientation.
     * </ol>
     *
     * @param  bdl the bundle to use
     * @return the orientation
     * @deprecated As of J2SE 1.4, use {@link #getOrientation(java.util.Locale)}.
     */
    @Deprecated
    public static ComponentOrientation getOrientation(ResourceBundle bdl)
    {
        ComponentOrientation result = null;

        try {
            result = (ComponentOrientation)bdl.getObject("Orientation");
        }
        catch (Exception e) {
        }

        if (result == null) {
            result = getOrientation(bdl.getLocale());
        }
        if (result == null) {
            result = getOrientation(Locale.getDefault());
        }
        return result;
    }

    /**
     * The bitwise-ored combination of flags.
     */
    private int orientation;

    private ComponentOrientation(int value)
    {
        orientation = value;
    }
 }
