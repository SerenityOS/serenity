/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javax.accessibility;

/**
 * The {@code AccessibleIcon} interface should be supported by any object that
 * has an associated icon (e.g., buttons). This interface provides the standard
 * mechanism for an assistive technology to get descriptive information about
 * icons. Applications can determine if an object supports the
 * {@code AccessibleIcon} interface by first obtaining its
 * {@code AccessibleContext} (see {@link Accessible}) and then calling the
 * {@link AccessibleContext#getAccessibleIcon} method. If the return value is
 * not {@code null}, the object supports this interface.
 *
 * @author Lynn Monsanto
 * @see Accessible
 * @see AccessibleContext
 * @since 1.3
 */
public interface AccessibleIcon {

    /**
     * Gets the description of the icon. This is meant to be a brief textual
     * description of the object. For example, it might be presented to a blind
     * user to give an indication of the purpose of the icon.
     *
     * @return the description of the icon
     */
    public String getAccessibleIconDescription();

    /**
     * Sets the description of the icon. This is meant to be a brief textual
     * description of the object. For example, it might be presented to a blind
     * user to give an indication of the purpose of the icon.
     *
     * @param  description the description of the icon
     */
    public void setAccessibleIconDescription(String description);

    /**
     * Gets the width of the icon.
     *
     * @return the width of the icon
     */
    public int getAccessibleIconWidth();

    /**
     * Gets the height of the icon.
     *
     * @return the height of the icon
     */
    public int getAccessibleIconHeight();
}
