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
package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.util.*;
import java.awt.FocusTraversalPolicy;
import sun.util.logging.PlatformLogger;
import sun.security.action.GetPropertyAction;
import java.security.AccessController;

/**
 * A FocusTraversalPolicy that determines traversal order by sorting the
 * Components of a focus traversal cycle based on a given Comparator. Portions
 * of the Component hierarchy that are not visible and displayable will not be
 * included.
 * <p>
 * By default, SortingFocusTraversalPolicy implicitly transfers focus down-
 * cycle. That is, during normal focus traversal, the Component
 * traversed after a focus cycle root will be the focus-cycle-root's default
 * Component to focus. This behavior can be disabled using the
 * <code>setImplicitDownCycleTraversal</code> method.
 * <p>
 * By default, methods of this class with return a Component only if it is
 * visible, displayable, enabled, and focusable. Subclasses can modify this
 * behavior by overriding the <code>accept</code> method.
 * <p>
 * This policy takes into account <a
 * href="../../java/awt/doc-files/FocusSpec.html#FocusTraversalPolicyProviders">focus traversal
 * policy providers</a>.  When searching for first/last/next/previous Component,
 * if a focus traversal policy provider is encountered, its focus traversal
 * policy is used to perform the search operation.
 *
 * @author David Mendenhall
 *
 * @see java.util.Comparator
 * @since 1.4
 */
public class SortingFocusTraversalPolicy
    extends InternalFrameFocusTraversalPolicy
{
    private Comparator<? super Component> comparator;
    private boolean implicitDownCycleTraversal = true;

    private PlatformLogger log = PlatformLogger.getLogger("javax.swing.SortingFocusTraversalPolicy");

    /**
     * Used by getComponentAfter and getComponentBefore for efficiency. In
     * order to maintain compliance with the specification of
     * FocusTraversalPolicy, if traversal wraps, we should invoke
     * getFirstComponent or getLastComponent. These methods may be overriden in
     * subclasses to behave in a non-generic way. However, in the generic case,
     * these methods will simply return the first or last Components of the
     * sorted list, respectively. Since getComponentAfter and
     * getComponentBefore have already built the sorted list before determining
     * that they need to invoke getFirstComponent or getLastComponent, the
     * sorted list should be reused if possible.
     */
    private transient Container cachedRoot;
    private transient List<Component> cachedCycle;

    // Delegate our fitness test to ContainerOrder so that we only have to
    // code the algorithm once.
    private static final SwingContainerOrderFocusTraversalPolicy
        fitnessTestPolicy = new SwingContainerOrderFocusTraversalPolicy();

    private final int FORWARD_TRAVERSAL = 0;
    private final int BACKWARD_TRAVERSAL = 1;

    /*
     * When true (by default), the legacy merge-sort algo is used to sort an FTP cycle.
     * When false, the default (tim-sort) algo is used, which may lead to an exception.
     * See: JDK-8048887
     */
    @SuppressWarnings("removal")
    private static final boolean legacySortingFTPEnabled = "true".equals(
            AccessController.doPrivileged(
                    new GetPropertyAction("swing.legacySortingFTPEnabled", "true")));

    /**
     * Constructs a SortingFocusTraversalPolicy without a Comparator.
     * Subclasses must set the Comparator using <code>setComparator</code>
     * before installing this FocusTraversalPolicy on a focus cycle root or
     * KeyboardFocusManager.
     */
    protected SortingFocusTraversalPolicy() {
    }

    /**
     * Constructs a SortingFocusTraversalPolicy with the specified Comparator.
     *
     * @param comparator the {@code Comparator} to sort by
     */
    public SortingFocusTraversalPolicy(Comparator<? super Component> comparator) {
        this.comparator = comparator;
    }

    private List<Component> getFocusTraversalCycle(Container aContainer) {
        List<Component> cycle = new ArrayList<Component>();
        enumerateAndSortCycle(aContainer, cycle);
        return cycle;
    }
    private int getComponentIndex(List<Component> cycle, Component aComponent) {
        int index;
        try {
            index = Collections.binarySearch(cycle, aComponent, comparator);
        } catch (ClassCastException e) {
            if (log.isLoggable(PlatformLogger.Level.FINE)) {
                log.fine("### During the binary search for " + aComponent + " the exception occurred: ", e);
            }
            return -1;
        }
        if (index < 0) {
            // Fix for 5070991.
            // A workaround for a transitivity problem caused by ROW_TOLERANCE,
            // because of that the component may be missed in the binary search.
            // Try to search it again directly.
            index = cycle.indexOf(aComponent);
        }
        return index;
    }

    private void enumerateAndSortCycle(Container focusCycleRoot, List<Component> cycle) {
        if (focusCycleRoot.isShowing()) {
            enumerateCycle(focusCycleRoot, cycle);
            if (legacySortingFTPEnabled) {
                legacySort(cycle, comparator);
            } else {
                cycle.sort(comparator);
            }
        }
    }

    private void legacySort(List<Component> l,
                                              Comparator<? super Component> c) {
        if (c != null && l.size() > 1) {
            Component[] a = l.toArray(new Component[l.size()]);
            mergeSort(a.clone(), a, 0, a.length, 0, c);
            ListIterator<Component> i = l.listIterator();
            for (Component e : a) {
                i.next();
                i.set(e);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void enumerateCycle(Container container, List<Component> cycle) {
        if (!(container.isVisible() && container.isDisplayable())) {
            return;
        }

        cycle.add(container);

        Component[] components = container.getComponents();
        for (Component comp : components) {
            if (comp instanceof Container) {
                Container cont = (Container)comp;

                if (!cont.isFocusCycleRoot() &&
                    !cont.isFocusTraversalPolicyProvider() &&
                    !((cont instanceof JComponent) && ((JComponent)cont).isManagingFocus()))
                {
                    enumerateCycle(cont, cycle);
                    continue;
                }
            }
            cycle.add(comp);
        }
    }

    Container getTopmostProvider(Container focusCycleRoot, Component aComponent) {
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
                        log.fine("### Transfered focus down-cycle to " + retComp +
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
                    log.fine("### Transfered focus to " + retComp + " in the FTP provider " + cont);
                }
            }
        }
        return retComp;
    }

    /**
     * Returns the Component that should receive the focus after aComponent.
     * aContainer must be a focus cycle root of aComponent or a focus traversal policy provider.
     * <p>
     * By default, SortingFocusTraversalPolicy implicitly transfers focus down-
     * cycle. That is, during normal focus traversal, the Component
     * traversed after a focus cycle root will be the focus-cycle-root's
     * default Component to focus. This behavior can be disabled using the
     * <code>setImplicitDownCycleTraversal</code> method.
     * <p>
     * If aContainer is <a href="../../java/awt/doc-files/FocusSpec.html#FocusTraversalPolicyProviders">focus
     * traversal policy provider</a>, the focus is always transferred down-cycle.
     *
     * @param aContainer a focus cycle root of aComponent or a focus traversal policy provider
     * @param aComponent a (possibly indirect) child of aContainer, or
     *        aContainer itself
     * @return the Component that should receive the focus after aComponent, or
     *         null if no suitable Component can be found
     * @throws IllegalArgumentException if aContainer is not a focus cycle
     *         root of aComponent or a focus traversal policy provider, or if either aContainer or
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

        // Before all the ckecks below we first see if it's an FTP provider or a focus cycle root.
        // If it's the case just go down cycle (if it's set to "implicit").
        Component comp = getComponentDownCycle(aComponent, FORWARD_TRAVERSAL);
        if (comp != null) {
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
        return null;
    }

    /**
     * Returns the Component that should receive the focus before aComponent.
     * aContainer must be a focus cycle root of aComponent or a focus traversal policy provider.
     * <p>
     * By default, SortingFocusTraversalPolicy implicitly transfers focus down-
     * cycle. That is, during normal focus traversal, the Component
     * traversed after a focus cycle root will be the focus-cycle-root's
     * default Component to focus. This behavior can be disabled using the
     * <code>setImplicitDownCycleTraversal</code> method.
     * <p>
     * If aContainer is <a href="../../java/awt/doc-files/FocusSpec.html#FocusTraversalPolicyProviders">focus
     * traversal policy provider</a>, the focus is always transferred down-cycle.
     *
     * @param aContainer a focus cycle root of aComponent or a focus traversal policy provider
     * @param aComponent a (possibly indirect) child of aContainer, or
     *        aContainer itself
     * @return the Component that should receive the focus before aComponent,
     *         or null if no suitable Component can be found
     * @throws IllegalArgumentException if aContainer is not a focus cycle
     *         root of aComponent or a focus traversal policy provider, or if either aContainer or
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

        Component comp;
        Component tryComp;

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
        return null;
    }

    /**
     * Returns the first Component in the traversal cycle. This method is used
     * to determine the next Component to focus when traversal wraps in the
     * forward direction.
     *
     * @param aContainer a focus cycle root of aComponent or a focus traversal policy provider whose
     *        first Component is to be returned
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
        return null;
    }

    /**
     * Returns the last Component in the traversal cycle. This method is used
     * to determine the next Component to focus when traversal wraps in the
     * reverse direction.
     *
     * @param aContainer a focus cycle root of aComponent or a focus traversal policy provider whose
     *        last Component is to be returned
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
        return null;
    }

    /**
     * Returns the default Component to focus. This Component will be the first
     * to receive focus when traversing down into a new focus traversal cycle
     * rooted at aContainer. The default implementation of this method
     * returns the same Component as <code>getFirstComponent</code>.
     *
     * @param aContainer a focus cycle root of aComponent or a focus traversal policy provider whose
     *        default Component is to be returned
     * @return the default Component in the traversal cycle of aContainer,
     *         or null if no suitable Component can be found
     * @see #getFirstComponent
     * @throws IllegalArgumentException if aContainer is null
     */
    public Component getDefaultComponent(Container aContainer) {
        return getFirstComponent(aContainer);
    }

    /**
     * Sets whether this SortingFocusTraversalPolicy transfers focus down-cycle
     * implicitly. If <code>true</code>, during normal focus traversal,
     * the Component traversed after a focus cycle root will be the focus-
     * cycle-root's default Component to focus. If <code>false</code>, the
     * next Component in the focus traversal cycle rooted at the specified
     * focus cycle root will be traversed instead. The default value for this
     * property is <code>true</code>.
     *
     * @param implicitDownCycleTraversal whether this
     *        SortingFocusTraversalPolicy transfers focus down-cycle implicitly
     * @see #getImplicitDownCycleTraversal
     * @see #getFirstComponent
     */
    public void setImplicitDownCycleTraversal(boolean implicitDownCycleTraversal) {
        this.implicitDownCycleTraversal = implicitDownCycleTraversal;
    }

    /**
     * Returns whether this SortingFocusTraversalPolicy transfers focus down-
     * cycle implicitly. If <code>true</code>, during normal focus
     * traversal, the Component traversed after a focus cycle root will be the
     * focus-cycle-root's default Component to focus. If <code>false</code>,
     * the next Component in the focus traversal cycle rooted at the specified
     * focus cycle root will be traversed instead.
     *
     * @return whether this SortingFocusTraversalPolicy transfers focus down-
     *         cycle implicitly
     * @see #setImplicitDownCycleTraversal
     * @see #getFirstComponent
     */
    public boolean getImplicitDownCycleTraversal() {
        return implicitDownCycleTraversal;
    }

    /**
     * Sets the Comparator which will be used to sort the Components in a
     * focus traversal cycle.
     *
     * @param comparator the Comparator which will be used for sorting
     */
    protected void setComparator(Comparator<? super Component> comparator) {
        this.comparator = comparator;
    }

    /**
     * Returns the Comparator which will be used to sort the Components in a
     * focus traversal cycle.
     *
     * @return the Comparator which will be used for sorting
     */
    protected Comparator<? super Component> getComparator() {
        return comparator;
    }

    /**
     * Determines whether a Component is an acceptable choice as the new
     * focus owner. By default, this method will accept a Component if and
     * only if it is visible, displayable, enabled, and focusable.
     *
     * @param aComponent the Component whose fitness as a focus owner is to
     *        be tested
     * @return <code>true</code> if aComponent is visible, displayable,
     *         enabled, and focusable; <code>false</code> otherwise
     */
    protected boolean accept(Component aComponent) {
        return fitnessTestPolicy.accept(aComponent);
    }

    // merge sort implementation copied from java.utils.Arrays
    private <T> void mergeSort(T[] src, T[] dest,
                               int low, int high, int off,
                               Comparator<? super T> c) {
        int length = high - low;

        // Insertion sort on smallest arrays
        if (length < 7) {
            for (int i=low; i<high; i++)
                for (int j=i; j>low && c.compare(dest[j-1], dest[j])>0; j--) {
                    T t = dest[j];
                    dest[j] = dest[j-1];
                    dest[j-1] = t;
                }
            return;
        }

        // Recursively sort halves of dest into src
        int destLow  = low;
        int destHigh = high;
        low  += off;
        high += off;
        int mid = (low + high) >>> 1;
        mergeSort(dest, src, low, mid, -off, c);
        mergeSort(dest, src, mid, high, -off, c);

        // If list is already sorted, just copy from src to dest.  This is an
        // optimization that results in faster sorts for nearly ordered lists.
        if (c.compare(src[mid-1], src[mid]) <= 0) {
            System.arraycopy(src, low, dest, destLow, length);
            return;
        }

        // Merge sorted halves (now in src) into dest
        for(int i = destLow, p = low, q = mid; i < destHigh; i++) {
            if (q >= high || p < mid && c.compare(src[p], src[q]) <= 0)
                dest[i] = src[p++];
            else
                dest[i] = src[q++];
        }
    }
}

// Create our own subclass and change accept to public so that we can call
// accept.
@SuppressWarnings("serial") // JDK-implementation class
class SwingContainerOrderFocusTraversalPolicy
    extends java.awt.ContainerOrderFocusTraversalPolicy
{
    public boolean accept(Component aComponent) {
        return super.accept(aComponent);
    }
}
