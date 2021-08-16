/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * A FocusTraversalPolicy defines the order in which Components with a
 * particular focus cycle root are traversed. Instances can apply the policy to
 * arbitrary focus cycle roots, allowing themselves to be shared across
 * Containers. They do not need to be reinitialized when the focus cycle roots
 * of a Component hierarchy change.
 * <p>
 * The core responsibility of a FocusTraversalPolicy is to provide algorithms
 * determining the next and previous Components to focus when traversing
 * forward or backward in a UI. Each FocusTraversalPolicy must also provide
 * algorithms for determining the first, last, and default Components in a
 * traversal cycle. First and last Components are used when normal forward and
 * backward traversal, respectively, wraps. The default Component is the first
 * to receive focus when traversing down into a new focus traversal cycle.
 * A FocusTraversalPolicy can optionally provide an algorithm for determining
 * a Window's initial Component. The initial Component is the first to receive
 * focus when a Window is first made visible.
 * <p>
 * FocusTraversalPolicy takes into account <a
 * href="doc-files/FocusSpec.html#FocusTraversalPolicyProviders">focus traversal
 * policy providers</a>.  When searching for first/last/next/previous Component,
 * if a focus traversal policy provider is encountered, its focus traversal
 * policy is used to perform the search operation.
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
 * @see Container#setFocusTraversalPolicy
 * @see Container#getFocusTraversalPolicy
 * @see Container#setFocusCycleRoot
 * @see Container#isFocusCycleRoot
 * @see Container#setFocusTraversalPolicyProvider
 * @see Container#isFocusTraversalPolicyProvider
 * @see KeyboardFocusManager#setDefaultFocusTraversalPolicy
 * @see KeyboardFocusManager#getDefaultFocusTraversalPolicy
 * @since 1.4
 */
public abstract class FocusTraversalPolicy {

    /**
     * Constructs a {@code FocusTraversalPolicy}.
     */
    protected FocusTraversalPolicy() {}

    /**
     * Returns the Component that should receive the focus after aComponent.
     * aContainer must be a focus cycle root of aComponent or a focus traversal
     * policy provider.
     *
     * @param aContainer a focus cycle root of aComponent or focus traversal
     *        policy provider
     * @param aComponent a (possibly indirect) child of aContainer, or
     *        aContainer itself
     * @return the Component that should receive the focus after aComponent, or
     *         null if no suitable Component can be found
     * @throws IllegalArgumentException if aContainer is not a focus cycle
     *         root of aComponent or a focus traversal policy provider, or if
     *         either aContainer or aComponent is null
     */
    public abstract Component getComponentAfter(Container aContainer,
                                                Component aComponent);

    /**
     * Returns the Component that should receive the focus before aComponent.
     * aContainer must be a focus cycle root of aComponent or a focus traversal
     * policy provider.
     *
     * @param aContainer a focus cycle root of aComponent or focus traversal
     *        policy provider
     * @param aComponent a (possibly indirect) child of aContainer, or
     *        aContainer itself
     * @return the Component that should receive the focus before aComponent,
     *         or null if no suitable Component can be found
     * @throws IllegalArgumentException if aContainer is not a focus cycle
     *         root of aComponent or a focus traversal policy provider, or if
     *         either aContainer or aComponent is null
     */
    public abstract Component getComponentBefore(Container aContainer,
                                                 Component aComponent);

    /**
     * Returns the first Component in the traversal cycle. This method is used
     * to determine the next Component to focus when traversal wraps in the
     * forward direction.
     *
     * @param aContainer the focus cycle root or focus traversal policy provider
     *        whose first Component is to be returned
     * @return the first Component in the traversal cycle of aContainer,
     *         or null if no suitable Component can be found
     * @throws IllegalArgumentException if aContainer is null
     */
    public abstract Component getFirstComponent(Container aContainer);

    /**
     * Returns the last Component in the traversal cycle. This method is used
     * to determine the next Component to focus when traversal wraps in the
     * reverse direction.
     *
     * @param aContainer the focus cycle root or focus traversal policy
     *        provider whose last Component is to be returned
     * @return the last Component in the traversal cycle of aContainer,
     *         or null if no suitable Component can be found
     * @throws IllegalArgumentException if aContainer is null
     */
    public abstract Component getLastComponent(Container aContainer);

    /**
     * Returns the default Component to focus. This Component will be the first
     * to receive focus when traversing down into a new focus traversal cycle
     * rooted at aContainer.
     *
     * @param aContainer the focus cycle root or focus traversal policy
     *        provider whose default Component is to be returned
     * @return the default Component in the traversal cycle of aContainer,
     *         or null if no suitable Component can be found
     * @throws IllegalArgumentException if aContainer is null
     */
    public abstract Component getDefaultComponent(Container aContainer);

    /**
     * Returns the Component that should receive the focus when a Window is
     * made visible for the first time. Once the Window has been made visible
     * by a call to {@code show()} or {@code setVisible(true)}, the
     * initial Component will not be used again. Instead, if the Window loses
     * and subsequently regains focus, or is made invisible or undisplayable
     * and subsequently made visible and displayable, the Window's most
     * recently focused Component will become the focus owner. The default
     * implementation of this method returns the default Component.
     *
     * @param window the Window whose initial Component is to be returned
     * @return the Component that should receive the focus when window is made
     *         visible for the first time, or null if no suitable Component can
     *         be found
     * @see #getDefaultComponent
     * @see Window#getMostRecentFocusOwner
     * @throws IllegalArgumentException if window is null
     */
    public Component getInitialComponent(Window window) {
        if ( window == null ){
            throw new IllegalArgumentException("window cannot be equal to null.");
        }
        Component def = getDefaultComponent(window);
        if (def == null && window.isFocusableWindow()) {
            def = window;
        }
        return def;
    }
}
