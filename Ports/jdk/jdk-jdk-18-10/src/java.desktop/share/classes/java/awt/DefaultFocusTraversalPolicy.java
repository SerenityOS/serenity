/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.awt;

import java.awt.peer.ComponentPeer;
import java.io.Serial;

/**
 * A FocusTraversalPolicy that determines traversal order based on the order
 * of child Components in a Container. From a particular focus cycle root, the
 * policy makes a pre-order traversal of the Component hierarchy, and traverses
 * a Container's children according to the ordering of the array returned by
 * {@code Container.getComponents()}. Portions of the hierarchy that are
 * not visible and displayable will not be searched.
 * <p>
 * If client code has explicitly set the focusability of a Component by either
 * overriding {@code Component.isFocusTraversable()} or
 * {@code Component.isFocusable()}, or by calling
 * {@code Component.setFocusable()}, then a DefaultFocusTraversalPolicy
 * behaves exactly like a ContainerOrderFocusTraversalPolicy. If, however, the
 * Component is relying on default focusability, then a
 * DefaultFocusTraversalPolicy will reject all Components with non-focusable
 * peers. This is the default FocusTraversalPolicy for all AWT Containers.
 * <p>
 * The focusability of a peer is implementation-dependent. Sun recommends that
 * all implementations for a particular native platform construct peers with
 * the same focusability. The recommendations for Windows and Unix are that
 * Canvases, Labels, Panels, Scrollbars, ScrollPanes, Windows, and lightweight
 * Components have non-focusable peers, and all other Components have focusable
 * peers. These recommendations are used in the Sun AWT implementations. Note
 * that the focusability of a Component's peer is different from, and does not
 * impact, the focusability of the Component itself.
 * <p>
 * Please see
 * <a href="https://docs.oracle.com/javase/tutorial/uiswing/misc/focus.html">
 * How to Use the Focus Subsystem</a>,
 * a section in <em>The Java Tutorial</em>, and the
 * <a href="doc-files/FocusSpec.html">Focus Specification</a>
 * for more information.
 *
 * @author David Mendenhall
 *
 * @see Container#getComponents
 * @see Component#isFocusable
 * @see Component#setFocusable
 * @since 1.4
 */
public class DefaultFocusTraversalPolicy
    extends ContainerOrderFocusTraversalPolicy
{
    /**
     * Use serialVersionUID from JDK 1.6 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 8876966522510157497L;

    /**
     * Constructs a {@code DefaultFocusTraversalPolicy}.
     */
    public DefaultFocusTraversalPolicy() {}

    /**
     * Determines whether a Component is an acceptable choice as the new
     * focus owner. The Component must be visible, displayable, and enabled
     * to be accepted. If client code has explicitly set the focusability
     * of the Component by either overriding
     * {@code Component.isFocusTraversable()} or
     * {@code Component.isFocusable()}, or by calling
     * {@code Component.setFocusable()}, then the Component will be
     * accepted if and only if it is focusable. If, however, the Component is
     * relying on default focusability, then all Canvases, Labels, Panels,
     * Scrollbars, ScrollPanes, Windows, and lightweight Components will be
     * rejected.
     *
     * @param aComponent the Component whose fitness as a focus owner is to
     *        be tested
     * @return {@code true} if aComponent meets the above requirements;
     *         {@code false} otherwise
     */
    protected boolean accept(Component aComponent) {
        if (!(aComponent.isVisible() && aComponent.isDisplayable() &&
              aComponent.isEnabled()))
        {
            return false;
        }

        // Verify that the Component is recursively enabled. Disabling a
        // heavyweight Container disables its children, whereas disabling
        // a lightweight Container does not.
        if (!(aComponent instanceof Window)) {
            for (Container enableTest = aComponent.getParent();
                 enableTest != null;
                 enableTest = enableTest.getParent())
            {
                if (!(enableTest.isEnabled() || enableTest.isLightweight())) {
                    return false;
                }
                if (enableTest instanceof Window) {
                    break;
                }
            }
        }

        boolean focusable = aComponent.isFocusable();
        if (aComponent.isFocusTraversableOverridden()) {
            return focusable;
        }

        ComponentPeer peer = aComponent.peer;
        return (peer != null && peer.isFocusable());
    }
}
