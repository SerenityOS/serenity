/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * The {@code AccessibleAction} interface should be supported by any object that
 * can perform one or more actions. This interface provides the standard
 * mechanism for an assistive technology to determine what those actions are as
 * well as tell the object to perform them. Any object that can be manipulated
 * should support this interface. Applications can determine if an object
 * supports the {@code AccessibleAction} interface by first obtaining its
 * {@code AccessibleContext} (see {@link Accessible}) and then calling the
 * {@link AccessibleContext#getAccessibleAction} method. If the return value is
 * not {@code null}, the object supports this interface.
 *
 * @author Peter Korn
 * @author Hans Muller
 * @author Willie Walker
 * @author Lynn Monsanto
 * @see Accessible
 * @see Accessible#getAccessibleContext
 * @see AccessibleContext
 * @see AccessibleContext#getAccessibleAction
 */
public interface AccessibleAction {

    /**
     * An action which causes a tree node to collapse if expanded and expand if
     * collapsed.
     *
     * @since 1.5
     */
    public static final String TOGGLE_EXPAND =
        new String ("toggleexpand");

    /**
     * An action which increments a value.
     *
     * @since 1.5
     */
    public static final String INCREMENT =
        new String ("increment");


    /**
     * An action which decrements a value.
     *
     * @since 1.5
     */
    public static final String DECREMENT =
        new String ("decrement");

    /**
     * An action which causes a component to execute its default action.
     *
     * @since 1.6
     */
    public static final String CLICK = new String("click");

    /**
     * An action which causes a popup to become visible if it is hidden and
     * hidden if it is visible.
     *
     * @since 1.6
     */
    public static final String TOGGLE_POPUP = new String("toggle popup");

    /**
     * Returns the number of accessible actions available in this object If
     * there are more than one, the first one is considered the "default" action
     * of the object.
     *
     * @return the zero-based number of actions in this object
     */
    public int getAccessibleActionCount();

    /**
     * Returns a description of the specified action of the object.
     *
     * @param  i zero-based index of the actions
     * @return a {@code String} description of the action
     * @see #getAccessibleActionCount
     */
    public String getAccessibleActionDescription(int i);

    /**
     * Performs the specified action on the object.
     *
     * @param  i zero-based index of actions
     * @return {@code true} if the action was performed; otherwise {@code false}
     * @see #getAccessibleActionCount
     */
    public boolean doAccessibleAction(int i);
}
