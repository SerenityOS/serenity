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

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import sun.util.logging.PlatformLogger;

/**
 * A FocusTraversalPolicy that determines traversal order based on the order
 * of child Components in a Container. From a particular focus cycle root, the
 * policy makes a pre-order traversal of the Component hierarchy, and traverses
 * a Container's children according to the ordering of the array returned by
 * {@code Container.getComponents()}. Portions of the hierarchy that are
 * not visible and displayable will not be searched.
 * <p>
 * By default, ContainerOrderFocusTraversalPolicy implicitly transfers focus
 * down-cycle. That is, during normal forward focus traversal, the Component
 * traversed after a focus cycle root will be the focus-cycle-root's default
 * Component to focus. This behavior can be disabled using the
 * {@code setImplicitDownCycleTraversal} method.
 * <p>
 * By default, methods of this class will return a Component only if it is
 * visible, displayable, enabled, and focusable. Subclasses can modify this
 * behavior by overriding the {@code accept} method.
 * <p>
 * This policy takes into account <a
 * href="doc-files/FocusSpec.html#FocusTraversalPolicyProviders">focus traversal
 * policy providers</a>.  When searching for first/last/next/previous Component,
 * if a focus traversal policy provider is encountered, its focus traversal
 * policy is used to perform the search operation.
 *
 * @author David Mendenhall
 *
 * @see Container#getComponents
 * @since 1.4
 */
public class ContainerOrderFocusTraversalPolicy extends FocusTraversalPolicy
    implements java.io.Serializable
{
    private static final PlatformLogger log = PlatformLogger.getLogger("java.awt.ContainerOrderFocusTraversalPolicy");

    /**
     * This constant is used when the forward focus traversal order is active.
     */
    private final int FORWARD_TRAVERSAL = 0;

    /**
     * This constant is used when the backward focus traversal order is active.
     */
    private final int BACKWARD_TRAVERSAL = 1;

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 486933713763926351L;

    /**
     * Whether this {@code ContainerOrderFocusTraversalPolicy} transfers focus
     * down-cycle implicitly.
     */
    private boolean implicitDownCycleTraversal = true;

    /**
     * Used by getComponentAfter and getComponentBefore for efficiency. In
     * order to maintain compliance with the specification of
     * FocusTraversalPolicy, if traversal wraps, we should invoke
     * getFirstComponent or getLastComponent. These methods may be overriden in
     * subclasses to behave in a non-generic way. However, in the generic case,
     * these methods will simply return the first or last Components of the
     * sorted list, respectively. Since getComponentAfter and
     * getComponentBefore have already built the list before determining
     * that they need to invoke getFirstComponent or getLastComponent, the
     * list should be reused if possible.
     */
    private transient Container cachedRoot;
    private transient List<Component> cachedCycle;

    /**
     * Constructs a {@code ContainerOrderFocusTraversalPolicy}.
     */
    public ContainerOrderFocusTraversalPolicy() {}

    /*
     * We suppose to use getFocusTraversalCycle & getComponentIndex methods in order
     * to divide the policy into two parts:
     * 1) Making the focus traversal cycle.
     * 2) Traversing the cycle.
     * The 1st point assumes producing a list of components representing the focus
     * traversal cycle. The two methods mentioned above should implement this logic.
     * The 2nd point assumes implementing the common concepts of operating on the
     * cycle: traversing back and forth, retrieving the initial/default/first/last
     * component. These concepts are described in the AWT Focus Spec and they are
     * applied to the FocusTraversalPolicy in general.
     * Thus, a descendant of this policy may wish to not reimplement the logic of
     * the 2nd point but just override the implementation of the 1st one.
     * A striking example of such a descendant is the javax.swing.SortingFocusTraversalPolicy.
     */
    /*protected*/ private List<Component> getFocusTraversalCycle(Container aContainer) {
        List<Component> cycle = new ArrayList<Component>();
        enumerateCycle(aContainer, cycle);
        return cycle;
    }
    /*protected*/ private int getComponentIndex(List<Component> cycle, Component aComponent) {
        return cycle.indexOf(aComponent);
    }

    private void enumerateCycle(Container container, List<Component> cycle) {
        if (!(container.isVisible() && container.isDisplayable())) {
            return;
        }

        cycle.add(container);

        Component[] components = container.getComponents();
        for (int i = 0; i < components.length; i++) {
            Component comp = components[i];
            if (comp instanceof Container) {
                Container cont = (Container)comp;

                if (!cont.isFocusCycleRoot() && !cont.isFocusTraversalPolicyProvider()) {
                    enumerateCycle(cont, cycle);
                    continue;
                }
            }
            cycle.add(comp);
        }
    }

    private Container getTopmostProvider(Container focusCycleRoot, Component aComponent) {
        Container aCont = aComponent.getParent();
        Container ftp = null;
        while (aCont  != focusCycleRoot && aCont != null) {
            if (aCont.isFocusTraversalPolicyProvider()) {
                ftp = aCont;
            }
            aCont = aCont.getParent();
        }
        if (aCont == null) {
            return null;
        }
        return ftp;
    }

    /*
     * Checks if a new focus cycle takes place and returns a Component to traverse focus to.
     * @param comp a possible focus cycle root or policy provider
     * @param traversalDirection the direction of the traversal
     * @return a Component to traverse focus to if {@code comp} is a root or provider
     *         and implicit down-cycle is set, otherwise {@code null}
     */
    private Component getComponentDownCycle(Component comp, int traversalDirection) {
        Component retComp = null;

        if (comp instanceof Container) {
            Container cont = (Container)comp;

            if (cont.isFocusCycleRoot()) {
                if (getImplicitDownCycleTraversal()) {
                    retComp = cont.getFocusTraversalPolicy().getDefaultComponent(cont);

                    if (retComp != null && log.isLoggable(PlatformLogger.Level.FINE)) {
                        log.fine("### Transferred focus down-cycle to " + retComp +
                                 " in the focus cycle root " + cont);
                    }
                } else {
                    return null;
                }
            } else if (cont.isFocusTraversalPolicyProvider()) {
                retComp = (traversalDirection == FORWARD_TRAVERSAL ?
                           cont.getFocusTraversalPolicy().getDefaultComponent(cont) :
                           cont.getFocusTraversalPolicy().getLastComponent(cont));

                if (retComp != null && log.isLoggable(PlatformLogger.Level.FINE)) {
                    log.fine("### Transferred focus to " + retComp + " in the FTP provider " + cont);
                }
            }
        }
        return retComp;
    }

    /**
     * Returns the Component that should receive the focus after aComponent.
     * aContainer must be a focus cycle root of aComponent or a focus traversal policy provider.
     * <p>
     * By default, ContainerOrderFocusTraversalPolicy implicitly transfers
     * focus down-cycle. That is, during normal forward focus traversal, the
     * Component traversed after a focus cycle root will be the focus-cycle-
     * root's default Component to focus. This behavior can be disabled using
     * the {@code setImplicitDownCycleTraversal} method.
     * <p>
     * If aContainer is <a href="doc-files/FocusSpec.html#FocusTraversalPolicyProviders">focus
     * traversal policy provider</a>, the focus is always transferred down-cycle.
     *
     * @param aContainer a focus cycle root of aComponent or a focus traversal policy provider
     * @param aComponent a (possibly indirect) child of aContainer, or
     *        aContainer itself
     * @return the Component that should receive the focus after aComponent, or
     *         null if no suitable Component can be found
     * @throws IllegalArgumentException if aContainer is not a focus cycle
     *         root of aComponent or focus traversal policy provider, or if either aContainer or
     *         aComponent is null
     */
    public Component getComponentAfter(Container aContainer, Component aComponent) {
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("### Searching in " + aContainer + " for component after " + aComponent);
        }

        if (aContainer == null || aComponent == null) {
            throw new IllegalArgumentException("aContainer and aComponent cannot be null");
        }
        if (!aContainer.isFocusTraversalPolicyProvider() && !aContainer.isFocusCycleRoot()) {
            throw new IllegalArgumentException("aContainer should be focus cycle root or focus traversal policy provider");

        } else if (aContainer.isFocusCycleRoot() && !aComponent.isFocusCycleRoot(aContainer)) {
            throw new IllegalArgumentException("aContainer is not a focus cycle root of aComponent");
        }

        synchronized(aContainer.getTreeLock()) {

            if (!(aContainer.isVisible() && aContainer.isDisplayable())) {
                return null;
            }

            // Before all the checks below we first see if it's an FTP provider or a focus cycle root.
            // If it's the case just go down cycle (if it's set to "implicit").
            Component comp = getComponentDownCycle(aComponent, FORWARD_TRAVERSAL);
            // Check if aComponent is focus-cycle-root's default Component, i.e.
            // focus cycle root & focus-cycle-root's default Component is same.
            if (comp != null && comp != aComponent) {
                return comp;
            }

            // See if the component is inside of policy provider.
            Container provider = getTopmostProvider(aContainer, aComponent);
            if (provider != null) {
                if (log.isLoggable(PlatformLogger.Level.FINE)) {
                    log.fine("### Asking FTP " + provider + " for component after " + aComponent);
                }

                // FTP knows how to find component after the given. We don't.
                FocusTraversalPolicy policy = provider.getFocusTraversalPolicy();
                Component afterComp = policy.getComponentAfter(provider, aComponent);

                // Null result means that we overstepped the limit of the FTP's cycle.
                // In that case we must quit the cycle, otherwise return the component found.
                if (afterComp != null) {
                    if (log.isLoggable(PlatformLogger.Level.FINE)) {
                        log.fine("### FTP returned " + afterComp);
                    }
                    return afterComp;
                }
                aComponent = provider;
            }

            List<Component> cycle = getFocusTraversalCycle(aContainer);

            if (log.isLoggable(PlatformLogger.Level.FINE)) {
                log.fine("### Cycle is " + cycle + ", component is " + aComponent);
            }

            int index = getComponentIndex(cycle, aComponent);

            if (index < 0) {
                if (log.isLoggable(PlatformLogger.Level.FINE)) {
                    log.fine("### Didn't find component " + aComponent + " in a cycle " + aContainer);
                }
                return getFirstComponent(aContainer);
            }

            for (index++; index < cycle.size(); index++) {
                comp = cycle.get(index);
                if (accept(comp)) {
                    return comp;
                } else if ((comp = getComponentDownCycle(comp, FORWARD_TRAVERSAL)) != null) {
                    return comp;
                }
            }

            if (aContainer.isFocusCycleRoot()) {
                this.cachedRoot = aContainer;
                this.cachedCycle = cycle;

                comp = getFirstComponent(aContainer);

                this.cachedRoot = null;
                this.cachedCycle = null;

                return comp;
            }
        }
        return null;
    }

    /**
     * Returns the Component that should receive the focus before aComponent.
     * aContainer must be a focus cycle root of aComponent or a <a
     * href="doc-files/FocusSpec.html#FocusTraversalPolicyProviders">focus traversal policy
     * provider</a>.
     *
     * @param aContainer a focus cycle root of aComponent or focus traversal policy provider
     * @param aComponent a (possibly indirect) child of aContainer, or
     *        aContainer itself
     * @return the Component that should receive the focus before aComponent,
     *         or null if no suitable Component can be found
     * @throws IllegalArgumentException if aContainer is not a focus cycle
     *         root of aComponent or focus traversal policy provider, or if either aContainer or
     *         aComponent is null
     */
    public Component getComponentBefore(Container aContainer, Component aComponent) {
        if (aContainer == null || aComponent == null) {
            throw new IllegalArgumentException("aContainer and aComponent cannot be null");
        }
        if (!aContainer.isFocusTraversalPolicyProvider() && !aContainer.isFocusCycleRoot()) {
            throw new IllegalArgumentException("aContainer should be focus cycle root or focus traversal policy provider");

        } else if (aContainer.isFocusCycleRoot() && !aComponent.isFocusCycleRoot(aContainer)) {
            throw new IllegalArgumentException("aContainer is not a focus cycle root of aComponent");
        }

        synchronized(aContainer.getTreeLock()) {

            if (!(aContainer.isVisible() && aContainer.isDisplayable())) {
                return null;
            }

            // See if the component is inside of policy provider.
            Container provider = getTopmostProvider(aContainer, aComponent);
            if (provider != null) {
                if (log.isLoggable(PlatformLogger.Level.FINE)) {
                    log.fine("### Asking FTP " + provider + " for component after " + aComponent);
                }

                // FTP knows how to find component after the given. We don't.
                FocusTraversalPolicy policy = provider.getFocusTraversalPolicy();
                Component beforeComp = policy.getComponentBefore(provider, aComponent);

                // Null result means that we overstepped the limit of the FTP's cycle.
                // In that case we must quit the cycle, otherwise return the component found.
                if (beforeComp != null) {
                    if (log.isLoggable(PlatformLogger.Level.FINE)) {
                        log.fine("### FTP returned " + beforeComp);
                    }
                    return beforeComp;
                }
                aComponent = provider;

                // If the provider is traversable it's returned.
                if (accept(aComponent)) {
                    return aComponent;
                }
            }

            List<Component> cycle = getFocusTraversalCycle(aContainer);

            if (log.isLoggable(PlatformLogger.Level.FINE)) {
                log.fine("### Cycle is " + cycle + ", component is " + aComponent);
            }

            int index = getComponentIndex(cycle, aComponent);

            if (index < 0) {
                if (log.isLoggable(PlatformLogger.Level.FINE)) {
                    log.fine("### Didn't find component " + aComponent + " in a cycle " + aContainer);
                }
                return getLastComponent(aContainer);
            }

            Component comp = null;
            Component tryComp = null;

            for (index--; index>=0; index--) {
                comp = cycle.get(index);
                if (comp != aContainer && (tryComp = getComponentDownCycle(comp, BACKWARD_TRAVERSAL)) != null) {
                    return tryComp;
                } else if (accept(comp)) {
                    return comp;
                }
            }

            if (aContainer.isFocusCycleRoot()) {
                this.cachedRoot = aContainer;
                this.cachedCycle = cycle;

                comp = getLastComponent(aContainer);

                this.cachedRoot = null;
                this.cachedCycle = null;

                return comp;
            }
        }
        return null;
    }

    /**
     * Returns the first Component in the traversal cycle. This method is used
     * to determine the next Component to focus when traversal wraps in the
     * forward direction.
     *
     * @param aContainer the focus cycle root or focus traversal policy provider whose first
     *        Component is to be returned
     * @return the first Component in the traversal cycle of aContainer,
     *         or null if no suitable Component can be found
     * @throws IllegalArgumentException if aContainer is null
     */
    public Component getFirstComponent(Container aContainer) {
        List<Component> cycle;

        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("### Getting first component in " + aContainer);
        }
        if (aContainer == null) {
            throw new IllegalArgumentException("aContainer cannot be null");

        }

        synchronized(aContainer.getTreeLock()) {

            if (!(aContainer.isVisible() && aContainer.isDisplayable())) {
                return null;
            }

            if (this.cachedRoot == aContainer) {
                cycle = this.cachedCycle;
            } else {
                cycle = getFocusTraversalCycle(aContainer);
            }

            if (cycle.size() == 0) {
                if (log.isLoggable(PlatformLogger.Level.FINE)) {
                    log.fine("### Cycle is empty");
                }
                return null;
            }
            if (log.isLoggable(PlatformLogger.Level.FINE)) {
                log.fine("### Cycle is " + cycle);
            }

            for (Component comp : cycle) {
                if (accept(comp)) {
                    return comp;
                } else if (comp != aContainer &&
                           (comp = getComponentDownCycle(comp, FORWARD_TRAVERSAL)) != null)
                {
                    return comp;
                }
            }
        }
        return null;
    }

    /**
     * Returns the last Component in the traversal cycle. This method is used
     * to determine the next Component to focus when traversal wraps in the
     * reverse direction.
     *
     * @param aContainer the focus cycle root or focus traversal policy provider whose last
     *        Component is to be returned
     * @return the last Component in the traversal cycle of aContainer,
     *         or null if no suitable Component can be found
     * @throws IllegalArgumentException if aContainer is null
     */
    public Component getLastComponent(Container aContainer) {
        List<Component> cycle;
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("### Getting last component in " + aContainer);
        }

        if (aContainer == null) {
            throw new IllegalArgumentException("aContainer cannot be null");
        }

        synchronized(aContainer.getTreeLock()) {

            if (!(aContainer.isVisible() && aContainer.isDisplayable())) {
                return null;
            }

            if (this.cachedRoot == aContainer) {
                cycle = this.cachedCycle;
            } else {
                cycle = getFocusTraversalCycle(aContainer);
            }

            if (cycle.size() == 0) {
                if (log.isLoggable(PlatformLogger.Level.FINE)) {
                    log.fine("### Cycle is empty");
                }
                return null;
            }
            if (log.isLoggable(PlatformLogger.Level.FINE)) {
                log.fine("### Cycle is " + cycle);
            }

            for (int i= cycle.size() - 1; i >= 0; i--) {
                Component comp = cycle.get(i);
                if (accept(comp)) {
                    return comp;
                } else if (comp instanceof Container && comp != aContainer) {
                    Container cont = (Container)comp;
                    if (cont.isFocusTraversalPolicyProvider()) {
                        Component retComp = cont.getFocusTraversalPolicy().getLastComponent(cont);
                        if (retComp != null) {
                            return retComp;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the default Component to focus. This Component will be the first
     * to receive focus when traversing down into a new focus traversal cycle
     * rooted at aContainer. The default implementation of this method
     * returns the same Component as {@code getFirstComponent}.
     *
     * @param aContainer the focus cycle root or focus traversal policy provider whose default
     *        Component is to be returned
     * @return the default Component in the traversal cycle of aContainer,
     *         or null if no suitable Component can be found
     * @see #getFirstComponent
     * @throws IllegalArgumentException if aContainer is null
     */
    public Component getDefaultComponent(Container aContainer) {
        return getFirstComponent(aContainer);
    }

    /**
     * Sets whether this ContainerOrderFocusTraversalPolicy transfers focus
     * down-cycle implicitly. If {@code true}, during normal forward focus
     * traversal, the Component traversed after a focus cycle root will be the
     * focus-cycle-root's default Component to focus. If {@code false},
     * the next Component in the focus traversal cycle rooted at the specified
     * focus cycle root will be traversed instead. The default value for this
     * property is {@code true}.
     *
     * @param implicitDownCycleTraversal whether this
     *        ContainerOrderFocusTraversalPolicy transfers focus down-cycle
     *        implicitly
     * @see #getImplicitDownCycleTraversal
     * @see #getFirstComponent
     */
    public void setImplicitDownCycleTraversal(boolean implicitDownCycleTraversal) {
        this.implicitDownCycleTraversal = implicitDownCycleTraversal;
    }

    /**
     * Returns whether this ContainerOrderFocusTraversalPolicy transfers focus
     * down-cycle implicitly. If {@code true}, during normal forward focus
     * traversal, the Component traversed after a focus cycle root will be the
     * focus-cycle-root's default Component to focus. If {@code false},
     * the next Component in the focus traversal cycle rooted at the specified
     * focus cycle root will be traversed instead.
     *
     * @return whether this ContainerOrderFocusTraversalPolicy transfers focus
     *         down-cycle implicitly
     * @see #setImplicitDownCycleTraversal
     * @see #getFirstComponent
     */
    public boolean getImplicitDownCycleTraversal() {
        return implicitDownCycleTraversal;
    }

    /**
     * Determines whether a Component is an acceptable choice as the new
     * focus owner. By default, this method will accept a Component if and
     * only if it is visible, displayable, enabled, and focusable.
     *
     * @param aComponent the Component whose fitness as a focus owner is to
     *        be tested
     * @return {@code true} if aComponent is visible, displayable,
     *         enabled, and focusable; {@code false} otherwise
     */
    protected boolean accept(Component aComponent) {
        if (!aComponent.canBeFocusOwner()) {
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

        return true;
    }
}
