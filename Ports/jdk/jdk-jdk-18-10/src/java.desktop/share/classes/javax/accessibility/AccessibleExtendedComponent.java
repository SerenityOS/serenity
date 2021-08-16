/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * The {@code AccessibleExtendedComponent} interface should be supported by any
 * object that is rendered on the screen. This interface provides the standard
 * mechanism for an assistive technology to determine the extended graphical
 * representation of an object. Applications can determine if an object supports
 * the {@code AccessibleExtendedComponent} interface by first obtaining its
 * {@code AccessibleContext} and then calling the
 * {@link AccessibleContext#getAccessibleComponent} method. If the return value
 * is not {@code null} and the type of the return value is
 * {@code AccessibleExtendedComponent}, the object supports this interface.
 *
 * @author Lynn Monsanto
 * @see Accessible
 * @see Accessible#getAccessibleContext
 * @see AccessibleContext
 * @see AccessibleContext#getAccessibleComponent
 * @since 1.4
 */
public interface AccessibleExtendedComponent extends AccessibleComponent {

    /**
     * Returns the tool tip text.
     *
     * @return the tool tip text, if supported, of the object; otherwise,
     *         {@code null}
     */
    public String getToolTipText();

    /**
     * Returns the titled border text.
     *
     * @return the titled border text, if supported, of the object; otherwise,
     *         {@code null}
     */
    public String getTitledBorderText();

    /**
     * Returns key bindings associated with this object.
     *
     * @return the key bindings, if supported, of the object; otherwise,
     *         {@code null}
     * @see AccessibleKeyBinding
     */
    public AccessibleKeyBinding getAccessibleKeyBinding();
}
