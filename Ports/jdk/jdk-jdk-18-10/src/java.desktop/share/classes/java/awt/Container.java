/*
 * Copyright (c) 1995, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.dnd.DropTarget;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.FocusEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.peer.ComponentPeer;
import java.awt.peer.ContainerPeer;
import java.awt.peer.LightweightPeer;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serial;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Set;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleComponent;
import javax.accessibility.AccessibleContext;

import sun.awt.AWTAccessor;
import sun.awt.AWTAccessor.MouseEventAccessor;
import sun.awt.AppContext;
import sun.awt.PeerEvent;
import sun.awt.SunToolkit;
import sun.awt.dnd.SunDropTargetEvent;
import sun.java2d.pipe.Region;
import sun.security.action.GetBooleanAction;
import sun.util.logging.PlatformLogger;

/**
 * A generic Abstract Window Toolkit(AWT) container object is a component
 * that can contain other AWT components.
 * <p>
 * Components added to a container are tracked in a list.  The order
 * of the list will define the components' front-to-back stacking order
 * within the container.  If no index is specified when adding a
 * component to a container, it will be added to the end of the list
 * (and hence to the bottom of the stacking order).
 * <p>
 * <b>Note</b>: For details on the focus subsystem, see
 * <a href="https://docs.oracle.com/javase/tutorial/uiswing/misc/focus.html">
 * How to Use the Focus Subsystem</a>,
 * a section in <em>The Java Tutorial</em>, and the
 * <a href="doc-files/FocusSpec.html">Focus Specification</a>
 * for more information.
 *
 * @author      Arthur van Hoff
 * @author      Sami Shaio
 * @see       #add(java.awt.Component, int)
 * @see       #getComponent(int)
 * @see       LayoutManager
 * @since     1.0
 */
public class Container extends Component {

    private static final PlatformLogger log = PlatformLogger.getLogger("java.awt.Container");
    private static final PlatformLogger eventLog = PlatformLogger.getLogger("java.awt.event.Container");

    private static final Component[] EMPTY_ARRAY = new Component[0];

    /**
     * The components in this container.
     * @see #add
     * @see #getComponents
     */
    private java.util.List<Component> component = new ArrayList<>();

    /**
     * Layout manager for this container.
     * @see #doLayout
     * @see #setLayout
     * @see #getLayout
     */
    LayoutManager layoutMgr;

    /**
     * Event router for lightweight components.  If this container
     * is native, this dispatcher takes care of forwarding and
     * retargeting the events to lightweight components contained
     * (if any).
     */
    private LightweightDispatcher dispatcher;

    /**
     * The focus traversal policy that will manage keyboard traversal of this
     * Container's children, if this Container is a focus cycle root. If the
     * value is null, this Container inherits its policy from its focus-cycle-
     * root ancestor. If all such ancestors of this Container have null
     * policies, then the current KeyboardFocusManager's default policy is
     * used. If the value is non-null, this policy will be inherited by all
     * focus-cycle-root children that have no keyboard-traversal policy of
     * their own (as will, recursively, their focus-cycle-root children).
     * <p>
     * If this Container is not a focus cycle root, the value will be
     * remembered, but will not be used or inherited by this or any other
     * Containers until this Container is made a focus cycle root.
     *
     * @see #setFocusTraversalPolicy
     * @see #getFocusTraversalPolicy
     * @since 1.4
     */
    private transient FocusTraversalPolicy focusTraversalPolicy;

    /**
     * Indicates whether this Component is the root of a focus traversal cycle.
     * Once focus enters a traversal cycle, typically it cannot leave it via
     * focus traversal unless one of the up- or down-cycle keys is pressed.
     * Normal traversal is limited to this Container, and all of this
     * Container's descendants that are not descendants of inferior focus cycle
     * roots.
     *
     * @see #setFocusCycleRoot
     * @see #isFocusCycleRoot
     * @since 1.4
     */
    private boolean focusCycleRoot = false;


    /**
     * Stores the value of focusTraversalPolicyProvider property.
     * @since 1.5
     * @see #setFocusTraversalPolicyProvider
     */
    private boolean focusTraversalPolicyProvider;

    // keeps track of the threads that are printing this component
    private transient Set<Thread> printingThreads;
    // True if there is at least one thread that's printing this component
    private transient boolean printing = false;

    transient ContainerListener containerListener;

    /* HierarchyListener and HierarchyBoundsListener support */
    transient int listeningChildren;
    transient int listeningBoundsChildren;
    transient int descendantsCount;

    /* Non-opaque window support -- see Window.setLayersOpaque */
    transient Color preserveBackgroundColor = null;

    /**
     * Use serialVersionUID from JDK 1.1 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 4613797578919906343L;

    /**
     * A constant which toggles one of the controllable behaviors
     * of {@code getMouseEventTarget}. It is used to specify whether
     * the method can return the Container on which it is originally called
     * in case if none of its children are the current mouse event targets.
     *
     * @see #getMouseEventTarget(int, int, boolean)
     */
    static final boolean INCLUDE_SELF = true;

    /**
     * A constant which toggles one of the controllable behaviors
     * of {@code getMouseEventTarget}. It is used to specify whether
     * the method should search only lightweight components.
     *
     * @see #getMouseEventTarget(int, int, boolean)
     */
    static final boolean SEARCH_HEAVYWEIGHTS = true;

    /*
     * Number of HW or LW components in this container (including
     * all descendant containers).
     */
    private transient int numOfHWComponents = 0;
    private transient int numOfLWComponents = 0;

    private static final PlatformLogger mixingLog = PlatformLogger.getLogger("java.awt.mixing.Container");

    /**
     * @serialField ncomponents                     int
     *       The number of components in this container.
     *       This value can be null.
     * @serialField component                       Component[]
     *       The components in this container.
     * @serialField layoutMgr                       LayoutManager
     *       Layout manager for this container.
     * @serialField dispatcher                      LightweightDispatcher
     *       Event router for lightweight components.  If this container
     *       is native, this dispatcher takes care of forwarding and
     *       retargeting the events to lightweight components contained
     *       (if any).
     * @serialField maxSize                         Dimension
     *       Maximum size of this Container.
     * @serialField focusCycleRoot                  boolean
     *       Indicates whether this Component is the root of a focus traversal cycle.
     *       Once focus enters a traversal cycle, typically it cannot leave it via
     *       focus traversal unless one of the up- or down-cycle keys is pressed.
     *       Normal traversal is limited to this Container, and all of this
     *       Container's descendants that are not descendants of inferior focus cycle
     *       roots.
     * @serialField containerSerializedDataVersion  int
     *       Container Serial Data Version.
     * @serialField focusTraversalPolicyProvider    boolean
     *       Stores the value of focusTraversalPolicyProvider property.
     */
    @Serial
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("ncomponents", Integer.TYPE),
        new ObjectStreamField("component", Component[].class),
        new ObjectStreamField("layoutMgr", LayoutManager.class),
        new ObjectStreamField("dispatcher", LightweightDispatcher.class),
        new ObjectStreamField("maxSize", Dimension.class),
        new ObjectStreamField("focusCycleRoot", Boolean.TYPE),
        new ObjectStreamField("containerSerializedDataVersion", Integer.TYPE),
        new ObjectStreamField("focusTraversalPolicyProvider", Boolean.TYPE),
    };

    static {
        /* ensure that the necessary native libraries are loaded */
        Toolkit.loadLibraries();
        if (!GraphicsEnvironment.isHeadless()) {
            initIDs();
        }

        AWTAccessor.setContainerAccessor(new AWTAccessor.ContainerAccessor() {
            @Override
            public void validateUnconditionally(Container cont) {
                cont.validateUnconditionally();
            }

            @Override
            public Component findComponentAt(Container cont, int x, int y,
                    boolean ignoreEnabled) {
                return cont.findComponentAt(x, y, ignoreEnabled);
            }

            @Override
            public void startLWModal(Container cont) {
                cont.startLWModal();
            }

            @Override
            public void stopLWModal(Container cont) {
                cont.stopLWModal();
            }
        });
    }

    /**
     * Initialize JNI field and method IDs for fields that may be
       called from C.
     */
    private static native void initIDs();

    /**
     * Constructs a new Container. Containers can be extended directly,
     * but are lightweight in this case and must be contained by a parent
     * somewhere higher up in the component tree that is native.
     * (such as Frame for example).
     */
    public Container() {
    }
    @SuppressWarnings({"unchecked","rawtypes"})
    void initializeFocusTraversalKeys() {
        focusTraversalKeys = new Set[4];
    }

    /**
     * Gets the number of components in this panel.
     * <p>
     * Note: This method should be called under AWT tree lock.
     *
     * @return    the number of components in this panel.
     * @see       #getComponent
     * @since     1.1
     * @see Component#getTreeLock()
     */
    public int getComponentCount() {
        return countComponents();
    }

    /**
     * Returns the number of components in this container.
     *
     * @return the number of components in this container
     * @deprecated As of JDK version 1.1,
     * replaced by getComponentCount().
     */
    @Deprecated
    public int countComponents() {
        // This method is not synchronized under AWT tree lock.
        // Instead, the calling code is responsible for the
        // synchronization. See 6784816 for details.
        return component.size();
    }

    /**
     * Gets the nth component in this container.
     * <p>
     * Note: This method should be called under AWT tree lock.
     *
     * @param      n   the index of the component to get.
     * @return     the n<sup>th</sup> component in this container.
     * @exception  ArrayIndexOutOfBoundsException
     *                 if the n<sup>th</sup> value does not exist.
     * @see Component#getTreeLock()
     */
    public Component getComponent(int n) {
        // This method is not synchronized under AWT tree lock.
        // Instead, the calling code is responsible for the
        // synchronization. See 6784816 for details.
        try {
            return component.get(n);
        } catch (IndexOutOfBoundsException z) {
            throw new ArrayIndexOutOfBoundsException("No such child: " + n);
        }
    }

    /**
     * Gets all the components in this container.
     * <p>
     * Note: This method should be called under AWT tree lock.
     *
     * @return    an array of all the components in this container.
     * @see Component#getTreeLock()
     */
    public Component[] getComponents() {
        // This method is not synchronized under AWT tree lock.
        // Instead, the calling code is responsible for the
        // synchronization. See 6784816 for details.
        return getComponents_NoClientCode();
    }

    // NOTE: This method may be called by privileged threads.
    //       This functionality is implemented in a package-private method
    //       to insure that it cannot be overridden by client subclasses.
    //       DO NOT INVOKE CLIENT CODE ON THIS THREAD!
    final Component[] getComponents_NoClientCode() {
        return component.toArray(EMPTY_ARRAY);
    }

    /*
     * Wrapper for getComponents() method with a proper synchronization.
     */
    Component[] getComponentsSync() {
        synchronized (getTreeLock()) {
            return getComponents();
        }
    }

    /**
     * Determines the insets of this container, which indicate the size
     * of the container's border.
     * <p>
     * A {@code Frame} object, for example, has a top inset that
     * corresponds to the height of the frame's title bar.
     * @return    the insets of this container.
     * @see       Insets
     * @see       LayoutManager
     * @since     1.1
     */
    public Insets getInsets() {
        return insets();
    }

    /**
     * Returns the insets for this container.
     *
     * @deprecated As of JDK version 1.1,
     * replaced by {@code getInsets()}.
     * @return the insets for this container
     */
    @Deprecated
    public Insets insets() {
        ComponentPeer peer = this.peer;
        if (peer instanceof ContainerPeer) {
            ContainerPeer cpeer = (ContainerPeer)peer;
            return (Insets)cpeer.getInsets().clone();
        }
        return new Insets(0, 0, 0, 0);
    }

    /**
     * Appends the specified component to the end of this container.
     * This is a convenience method for {@link #addImpl}.
     * <p>
     * This method changes layout-related information, and therefore,
     * invalidates the component hierarchy. If the container has already been
     * displayed, the hierarchy must be validated thereafter in order to
     * display the added component.
     *
     * @param     comp   the component to be added
     * @exception NullPointerException if {@code comp} is {@code null}
     * @see #addImpl
     * @see #invalidate
     * @see #validate
     * @see javax.swing.JComponent#revalidate()
     * @return    the component argument
     */
    public Component add(Component comp) {
        addImpl(comp, null, -1);
        return comp;
    }

    /**
     * Adds the specified component to this container.
     * This is a convenience method for {@link #addImpl}.
     * <p>
     * This method is obsolete as of 1.1.  Please use the
     * method {@code add(Component, Object)} instead.
     * <p>
     * This method changes layout-related information, and therefore,
     * invalidates the component hierarchy. If the container has already been
     * displayed, the hierarchy must be validated thereafter in order to
     * display the added component.
     *
     * @param  name the name of the component to be added
     * @param  comp the component to be added
     * @return the component added
     * @exception NullPointerException if {@code comp} is {@code null}
     * @see #add(Component, Object)
     * @see #invalidate
     */
    public Component add(String name, Component comp) {
        addImpl(comp, name, -1);
        return comp;
    }

    /**
     * Adds the specified component to this container at the given
     * position.
     * This is a convenience method for {@link #addImpl}.
     * <p>
     * This method changes layout-related information, and therefore,
     * invalidates the component hierarchy. If the container has already been
     * displayed, the hierarchy must be validated thereafter in order to
     * display the added component.
     *
     *
     * @param     comp   the component to be added
     * @param     index    the position at which to insert the component,
     *                   or {@code -1} to append the component to the end
     * @exception NullPointerException if {@code comp} is {@code null}
     * @exception IllegalArgumentException if {@code index} is invalid (see
     *            {@link #addImpl} for details)
     * @return    the component {@code comp}
     * @see #addImpl
     * @see #remove
     * @see #invalidate
     * @see #validate
     * @see javax.swing.JComponent#revalidate()
     */
    public Component add(Component comp, int index) {
        addImpl(comp, null, index);
        return comp;
    }

    /**
     * Checks that the component
     * isn't supposed to be added into itself.
     */
    private void checkAddToSelf(Component comp){
        if (comp instanceof Container) {
            for (Container cn = this; cn != null; cn=cn.parent) {
                if (cn == comp) {
                    throw new IllegalArgumentException("adding container's parent to itself");
                }
            }
        }
    }

    /**
     * Checks that the component is not a Window instance.
     */
    private void checkNotAWindow(Component comp){
        if (comp instanceof Window) {
            throw new IllegalArgumentException("adding a window to a container");
        }
    }

    /**
     * Checks that the component comp can be added to this container
     * Checks :  index in bounds of container's size,
     * comp is not one of this container's parents,
     * and comp is not a window.
     * Comp and container must be on the same GraphicsDevice.
     * if comp is container, all sub-components must be on
     * same GraphicsDevice.
     *
     * @since 1.5
     */
    private void checkAdding(Component comp, int index) {
        checkTreeLock();

        GraphicsConfiguration thisGC = getGraphicsConfiguration();

        if (index > component.size() || index < 0) {
            throw new IllegalArgumentException("illegal component position");
        }
        if (comp.parent == this) {
            if (index == component.size()) {
                throw new IllegalArgumentException("illegal component position " +
                                                   index + " should be less than " + component.size());
            }
        }
        checkAddToSelf(comp);
        checkNotAWindow(comp);

        Window thisTopLevel = getContainingWindow();
        Window compTopLevel = comp.getContainingWindow();
        if (thisTopLevel != compTopLevel) {
            throw new IllegalArgumentException("component and container should be in the same top-level window");
        }
        if (thisGC != null) {
            comp.checkGD(thisGC.getDevice().getIDstring());
        }
    }

    /**
     * Removes component comp from this container without making unnecessary changes
     * and generating unnecessary events. This function intended to perform optimized
     * remove, for example, if newParent and current parent are the same it just changes
     * index without calling removeNotify.
     * Note: Should be called while holding treeLock
     * Returns whether removeNotify was invoked
     * @since: 1.5
     */
    private boolean removeDelicately(Component comp, Container newParent, int newIndex) {
        checkTreeLock();

        int index = getComponentZOrder(comp);
        boolean needRemoveNotify = isRemoveNotifyNeeded(comp, this, newParent);
        if (needRemoveNotify) {
            comp.removeNotify();
        }
        if (newParent != this) {
            if (layoutMgr != null) {
                layoutMgr.removeLayoutComponent(comp);
            }
            adjustListeningChildren(AWTEvent.HIERARCHY_EVENT_MASK,
                                    -comp.numListening(AWTEvent.HIERARCHY_EVENT_MASK));
            adjustListeningChildren(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK,
                                    -comp.numListening(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK));
            adjustDescendants(-(comp.countHierarchyMembers()));

            comp.parent = null;
            if (needRemoveNotify) {
                comp.setGraphicsConfiguration(null);
            }
            component.remove(index);

            invalidateIfValid();
        } else {
            // We should remove component and then
            // add it by the newIndex without newIndex decrement if even we shift components to the left
            // after remove. Consult the rules below:
            // 2->4: 012345 -> 013425, 2->5: 012345 -> 013452
            // 4->2: 012345 -> 014235
            component.remove(index);
            component.add(newIndex, comp);
        }
        if (comp.parent == null) { // was actually removed
            if (containerListener != null ||
                (eventMask & AWTEvent.CONTAINER_EVENT_MASK) != 0 ||
                Toolkit.enabledOnToolkit(AWTEvent.CONTAINER_EVENT_MASK)) {
                ContainerEvent e = new ContainerEvent(this,
                                                      ContainerEvent.COMPONENT_REMOVED,
                                                      comp);
                dispatchEvent(e);

            }
            comp.createHierarchyEvents(HierarchyEvent.HIERARCHY_CHANGED, comp,
                                       this, HierarchyEvent.PARENT_CHANGED,
                                       Toolkit.enabledOnToolkit(AWTEvent.HIERARCHY_EVENT_MASK));
            if (peer != null && layoutMgr == null && isVisible()) {
                updateCursorImmediately();
            }
        }
        return needRemoveNotify;
    }

    /**
     * Checks whether this container can contain component which is focus owner.
     * Verifies that container is enable and showing, and if it is focus cycle root
     * its FTP allows component to be focus owner
     * @since 1.5
     */
    boolean canContainFocusOwner(Component focusOwnerCandidate) {
        if (!(isEnabled() && isDisplayable()
              && isVisible() && isFocusable()))
        {
            return false;
        }
        if (isFocusCycleRoot()) {
            FocusTraversalPolicy policy = getFocusTraversalPolicy();
            if (policy instanceof DefaultFocusTraversalPolicy) {
                if (!((DefaultFocusTraversalPolicy)policy).accept(focusOwnerCandidate)) {
                    return false;
                }
            }
        }
        synchronized(getTreeLock()) {
            if (parent != null) {
                return parent.canContainFocusOwner(focusOwnerCandidate);
            }
        }
        return true;
    }

    /**
     * Checks whether or not this container has heavyweight children.
     * Note: Should be called while holding tree lock
     * @return true if there is at least one heavyweight children in a container, false otherwise
     * @since 1.5
     */
    final boolean hasHeavyweightDescendants() {
        checkTreeLock();
        return numOfHWComponents > 0;
    }

    /**
     * Checks whether or not this container has lightweight children.
     * Note: Should be called while holding tree lock
     * @return true if there is at least one lightweight children in a container, false otherwise
     * @since 1.7
     */
    final boolean hasLightweightDescendants() {
        checkTreeLock();
        return numOfLWComponents > 0;
    }

    /**
     * Returns closest heavyweight component to this container. If this container is heavyweight
     * returns this.
     * @since 1.5
     */
    Container getHeavyweightContainer() {
        checkTreeLock();
        if (peer != null && !(peer instanceof LightweightPeer)) {
            return this;
        } else {
            return getNativeContainer();
        }
    }

    /**
     * Detects whether or not remove from current parent and adding to new parent requires call of
     * removeNotify on the component. Since removeNotify destroys native window this might (not)
     * be required. For example, if new container and old containers are the same we don't need to
     * destroy native window.
     * @since: 1.5
     */
    private static boolean isRemoveNotifyNeeded(Component comp, Container oldContainer, Container newContainer) {
        if (oldContainer == null) { // Component didn't have parent - no removeNotify
            return false;
        }
        if (comp.peer == null) { // Component didn't have peer - no removeNotify
            return false;
        }
        if (newContainer.peer == null) {
            // Component has peer but new Container doesn't - call removeNotify
            return true;
        }

        // If component is lightweight non-Container or lightweight Container with all but heavyweight
        // children there is no need to call remove notify
        if (comp.isLightweight()) {
            boolean isContainer = comp instanceof Container;

            if (!isContainer || (isContainer && !((Container)comp).hasHeavyweightDescendants())) {
                return false;
            }
        }

        // If this point is reached, then the comp is either a HW or a LW container with HW descendants.

        // All three components have peers, check for peer change
        Container newNativeContainer = oldContainer.getHeavyweightContainer();
        Container oldNativeContainer = newContainer.getHeavyweightContainer();
        if (newNativeContainer != oldNativeContainer) {
            // Native containers change - check whether or not current platform supports
            // changing of widget hierarchy on native level without recreation.
            // The current implementation forbids reparenting of LW containers with HW descendants
            // into another native container w/o destroying the peers. Actually such an operation
            // is quite rare. If we ever need to save the peers, we'll have to slightly change the
            // addDelicately() method in order to handle such LW containers recursively, reparenting
            // each HW descendant independently.
            return !comp.peer.isReparentSupported();
        } else {
            return false;
        }
    }

    /**
     * Moves the specified component to the specified z-order index in
     * the container. The z-order determines the order that components
     * are painted; the component with the highest z-order paints first
     * and the component with the lowest z-order paints last.
     * Where components overlap, the component with the lower
     * z-order paints over the component with the higher z-order.
     * <p>
     * If the component is a child of some other container, it is
     * removed from that container before being added to this container.
     * The important difference between this method and
     * {@code java.awt.Container.add(Component, int)} is that this method
     * doesn't call {@code removeNotify} on the component while
     * removing it from its previous container unless necessary and when
     * allowed by the underlying native windowing system. This way, if the
     * component has the keyboard focus, it maintains the focus when
     * moved to the new position.
     * <p>
     * This property is guaranteed to apply only to lightweight
     * non-{@code Container} components.
     * <p>
     * This method changes layout-related information, and therefore,
     * invalidates the component hierarchy.
     * <p>
     * <b>Note</b>: Not all platforms support changing the z-order of
     * heavyweight components from one container into another without
     * the call to {@code removeNotify}. There is no way to detect
     * whether a platform supports this, so developers shouldn't make
     * any assumptions.
     *
     * @param     comp the component to be moved
     * @param     index the position in the container's list to
     *            insert the component, where {@code getComponentCount()}
     *            appends to the end
     * @exception NullPointerException if {@code comp} is
     *            {@code null}
     * @exception IllegalArgumentException if {@code comp} is one of the
     *            container's parents
     * @exception IllegalArgumentException if {@code index} is not in
     *            the range {@code [0, getComponentCount()]} for moving
     *            between containers, or not in the range
     *            {@code [0, getComponentCount()-1]} for moving inside
     *            a container
     * @exception IllegalArgumentException if adding a container to itself
     * @exception IllegalArgumentException if adding a {@code Window}
     *            to a container
     * @see #getComponentZOrder(java.awt.Component)
     * @see #invalidate
     * @since 1.5
     */
    public void setComponentZOrder(Component comp, int index) {
         synchronized (getTreeLock()) {
             // Store parent because remove will clear it
             Container curParent = comp.parent;
             int oldZindex = getComponentZOrder(comp);

             if (curParent == this && index == oldZindex) {
                 return;
             }
             checkAdding(comp, index);

             boolean peerRecreated = (curParent != null) ?
                 curParent.removeDelicately(comp, this, index) : false;

             addDelicately(comp, curParent, index);

             // If the oldZindex == -1, the component gets inserted,
             // rather than it changes its z-order.
             if (!peerRecreated && oldZindex != -1) {
                 // The new 'index' cannot be == -1.
                 // It gets checked at the checkAdding() method.
                 // Therefore both oldZIndex and index denote
                 // some existing positions at this point and
                 // this is actually a Z-order changing.
                 comp.mixOnZOrderChanging(oldZindex, index);
             }
         }
    }

    /**
     * Traverses the tree of components and reparents children heavyweight component
     * to new heavyweight parent.
     * @since 1.5
     */
    @SuppressWarnings("deprecation")
    private void reparentTraverse(ContainerPeer parentPeer, Container child) {
        checkTreeLock();

        for (int i = 0; i < child.getComponentCount(); i++) {
            Component comp = child.getComponent(i);
            if (comp.isLightweight()) {
                // If components is lightweight check if it is container
                // If it is container it might contain heavyweight children we need to reparent
                if (comp instanceof Container) {
                    reparentTraverse(parentPeer, (Container)comp);
                }
            } else {
                // Q: Need to update NativeInLightFixer?
                comp.peer.reparent(parentPeer);
            }
        }
    }

    /**
     * Reparents child component peer to this container peer.
     * Container must be heavyweight.
     * @since 1.5
     */
    @SuppressWarnings("deprecation")
    private void reparentChild(Component comp) {
        checkTreeLock();
        if (comp == null) {
            return;
        }
        if (comp.isLightweight()) {
            // If component is lightweight container we need to reparent all its explicit  heavyweight children
            if (comp instanceof Container) {
                // Traverse component's tree till depth-first until encountering heavyweight component
                reparentTraverse((ContainerPeer)peer, (Container)comp);
            }
        } else {
            comp.peer.reparent((ContainerPeer) peer);
        }
    }

    /**
     * Adds component to this container. Tries to minimize side effects of this adding -
     * doesn't call remove notify if it is not required.
     * @since 1.5
     */
    private void addDelicately(Component comp, Container curParent, int index) {
        checkTreeLock();

        // Check if moving between containers
        if (curParent != this) {
            //index == -1 means add to the end.
            if (index == -1) {
                component.add(comp);
            } else {
                component.add(index, comp);
            }
            comp.parent = this;
            comp.setGraphicsConfiguration(getGraphicsConfiguration());

            adjustListeningChildren(AWTEvent.HIERARCHY_EVENT_MASK,
                                    comp.numListening(AWTEvent.HIERARCHY_EVENT_MASK));
            adjustListeningChildren(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK,
                                    comp.numListening(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK));
            adjustDescendants(comp.countHierarchyMembers());
        } else {
            if (index < component.size()) {
                component.set(index, comp);
            }
        }

        invalidateIfValid();
        if (peer != null) {
            if (comp.peer == null) { // Remove notify was called or it didn't have peer - create new one
                comp.addNotify();
            } else { // Both container and child have peers, it means child peer should be reparented.
                // In both cases we need to reparent native widgets.
                Container newNativeContainer = getHeavyweightContainer();
                Container oldNativeContainer = curParent.getHeavyweightContainer();
                if (oldNativeContainer != newNativeContainer) {
                    // Native container changed - need to reparent native widgets
                    newNativeContainer.reparentChild(comp);
                }
                comp.updateZOrder();

                if (!comp.isLightweight() && isLightweight()) {
                    // If component is heavyweight and one of the containers is lightweight
                    // the location of the component should be fixed.
                    comp.relocateComponent();
                }
            }
        }
        if (curParent != this) {
            /* Notify the layout manager of the added component. */
            if (layoutMgr != null) {
                if (layoutMgr instanceof LayoutManager2) {
                    ((LayoutManager2)layoutMgr).addLayoutComponent(comp, null);
                } else {
                    layoutMgr.addLayoutComponent(null, comp);
                }
            }
            if (containerListener != null ||
                (eventMask & AWTEvent.CONTAINER_EVENT_MASK) != 0 ||
                Toolkit.enabledOnToolkit(AWTEvent.CONTAINER_EVENT_MASK)) {
                ContainerEvent e = new ContainerEvent(this,
                                                      ContainerEvent.COMPONENT_ADDED,
                                                      comp);
                dispatchEvent(e);
            }
            comp.createHierarchyEvents(HierarchyEvent.HIERARCHY_CHANGED, comp,
                                       this, HierarchyEvent.PARENT_CHANGED,
                                       Toolkit.enabledOnToolkit(AWTEvent.HIERARCHY_EVENT_MASK));

            // If component is focus owner or parent container of focus owner check that after reparenting
            // focus owner moved out if new container prohibit this kind of focus owner.
            if (comp.isFocusOwner() && !comp.canBeFocusOwnerRecursively()) {
                comp.transferFocus();
            } else if (comp instanceof Container) {
                Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focusOwner != null && isParentOf(focusOwner) && !focusOwner.canBeFocusOwnerRecursively()) {
                    focusOwner.transferFocus();
                }
            }
        } else {
            comp.createHierarchyEvents(HierarchyEvent.HIERARCHY_CHANGED, comp,
                                       this, HierarchyEvent.HIERARCHY_CHANGED,
                                       Toolkit.enabledOnToolkit(AWTEvent.HIERARCHY_EVENT_MASK));
        }

        if (peer != null && layoutMgr == null && isVisible()) {
            updateCursorImmediately();
        }
    }

    /**
     * Returns the z-order index of the component inside the container.
     * The higher a component is in the z-order hierarchy, the lower
     * its index.  The component with the lowest z-order index is
     * painted last, above all other child components.
     *
     * @param comp the component being queried
     * @return  the z-order index of the component; otherwise
     *          returns -1 if the component is {@code null}
     *          or doesn't belong to the container
     * @see #setComponentZOrder(java.awt.Component, int)
     * @since 1.5
     */
    public int getComponentZOrder(Component comp) {
        if (comp == null) {
            return -1;
        }
        synchronized(getTreeLock()) {
            // Quick check - container should be immediate parent of the component
            if (comp.parent != this) {
                return -1;
            }
            return component.indexOf(comp);
        }
    }

    /**
     * Adds the specified component to the end of this container.
     * Also notifies the layout manager to add the component to
     * this container's layout using the specified constraints object.
     * This is a convenience method for {@link #addImpl}.
     * <p>
     * This method changes layout-related information, and therefore,
     * invalidates the component hierarchy. If the container has already been
     * displayed, the hierarchy must be validated thereafter in order to
     * display the added component.
     *
     *
     * @param     comp the component to be added
     * @param     constraints an object expressing
     *                  layout constraints for this component
     * @exception NullPointerException if {@code comp} is {@code null}
     * @see #addImpl
     * @see #invalidate
     * @see #validate
     * @see javax.swing.JComponent#revalidate()
     * @see       LayoutManager
     * @since     1.1
     */
    public void add(Component comp, Object constraints) {
        addImpl(comp, constraints, -1);
    }

    /**
     * Adds the specified component to this container with the specified
     * constraints at the specified index.  Also notifies the layout
     * manager to add the component to the this container's layout using
     * the specified constraints object.
     * This is a convenience method for {@link #addImpl}.
     * <p>
     * This method changes layout-related information, and therefore,
     * invalidates the component hierarchy. If the container has already been
     * displayed, the hierarchy must be validated thereafter in order to
     * display the added component.
     *
     *
     * @param comp the component to be added
     * @param constraints an object expressing layout constraints for this
     * @param index the position in the container's list at which to insert
     * the component; {@code -1} means insert at the end
     * component
     * @exception NullPointerException if {@code comp} is {@code null}
     * @exception IllegalArgumentException if {@code index} is invalid (see
     *            {@link #addImpl} for details)
     * @see #addImpl
     * @see #invalidate
     * @see #validate
     * @see javax.swing.JComponent#revalidate()
     * @see #remove
     * @see LayoutManager
     */
    public void add(Component comp, Object constraints, int index) {
       addImpl(comp, constraints, index);
    }

    /**
     * Adds the specified component to this container at the specified
     * index. This method also notifies the layout manager to add
     * the component to this container's layout using the specified
     * constraints object via the {@code addLayoutComponent}
     * method.
     * <p>
     * The constraints are
     * defined by the particular layout manager being used.  For
     * example, the {@code BorderLayout} class defines five
     * constraints: {@code BorderLayout.NORTH},
     * {@code BorderLayout.SOUTH}, {@code BorderLayout.EAST},
     * {@code BorderLayout.WEST}, and {@code BorderLayout.CENTER}.
     * <p>
     * The {@code GridBagLayout} class requires a
     * {@code GridBagConstraints} object.  Failure to pass
     * the correct type of constraints object results in an
     * {@code IllegalArgumentException}.
     * <p>
     * If the current layout manager implements {@code LayoutManager2}, then
     * {@link LayoutManager2#addLayoutComponent(Component,Object)} is invoked on
     * it. If the current layout manager does not implement
     * {@code LayoutManager2}, and constraints is a {@code String}, then
     * {@link LayoutManager#addLayoutComponent(String,Component)} is invoked on it.
     * <p>
     * If the component is not an ancestor of this container and has a non-null
     * parent, it is removed from its current parent before it is added to this
     * container.
     * <p>
     * This is the method to override if a program needs to track
     * every add request to a container as all other add methods defer
     * to this one. An overriding method should
     * usually include a call to the superclass's version of the method:
     *
     * <blockquote>
     * {@code super.addImpl(comp, constraints, index)}
     * </blockquote>
     * <p>
     * This method changes layout-related information, and therefore,
     * invalidates the component hierarchy. If the container has already been
     * displayed, the hierarchy must be validated thereafter in order to
     * display the added component.
     *
     * @param     comp       the component to be added
     * @param     constraints an object expressing layout constraints
     *                 for this component
     * @param     index the position in the container's list at which to
     *                 insert the component, where {@code -1}
     *                 means append to the end
     * @exception IllegalArgumentException if {@code index} is invalid;
     *            if {@code comp} is a child of this container, the valid
     *            range is {@code [-1, getComponentCount()-1]}; if component is
     *            not a child of this container, the valid range is
     *            {@code [-1, getComponentCount()]}
     *
     * @exception IllegalArgumentException if {@code comp} is an ancestor of
     *                                     this container
     * @exception IllegalArgumentException if adding a window to a container
     * @exception NullPointerException if {@code comp} is {@code null}
     * @see       #add(Component)
     * @see       #add(Component, int)
     * @see       #add(Component, java.lang.Object)
     * @see #invalidate
     * @see       LayoutManager
     * @see       LayoutManager2
     * @since     1.1
     */
    protected void addImpl(Component comp, Object constraints, int index) {
        synchronized (getTreeLock()) {
            /* Check for correct arguments:  index in bounds,
             * comp cannot be one of this container's parents,
             * and comp cannot be a window.
             * comp and container must be on the same GraphicsDevice.
             * if comp is container, all sub-components must be on
             * same GraphicsDevice.
             */
            GraphicsConfiguration thisGC = this.getGraphicsConfiguration();

            if (index > component.size() || (index < 0 && index != -1)) {
                throw new IllegalArgumentException(
                          "illegal component position");
            }
            checkAddToSelf(comp);
            checkNotAWindow(comp);
            /* Reparent the component and tidy up the tree's state. */
            if (comp.parent != null) {
                comp.parent.remove(comp);
                if (index > component.size()) {
                    throw new IllegalArgumentException("illegal component position");
                }
            }
            if (thisGC != null) {
                comp.checkGD(thisGC.getDevice().getIDstring());
            }



            //index == -1 means add to the end.
            if (index == -1) {
                component.add(comp);
            } else {
                component.add(index, comp);
            }
            comp.parent = this;
            comp.setGraphicsConfiguration(thisGC);

            adjustListeningChildren(AWTEvent.HIERARCHY_EVENT_MASK,
                comp.numListening(AWTEvent.HIERARCHY_EVENT_MASK));
            adjustListeningChildren(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK,
                comp.numListening(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK));
            adjustDescendants(comp.countHierarchyMembers());

            invalidateIfValid();
            if (peer != null) {
                comp.addNotify();
            }

            /* Notify the layout manager of the added component. */
            if (layoutMgr != null) {
                if (layoutMgr instanceof LayoutManager2) {
                    ((LayoutManager2)layoutMgr).addLayoutComponent(comp, constraints);
                } else if (constraints instanceof String) {
                    layoutMgr.addLayoutComponent((String)constraints, comp);
                }
            }
            if (containerListener != null ||
                (eventMask & AWTEvent.CONTAINER_EVENT_MASK) != 0 ||
                Toolkit.enabledOnToolkit(AWTEvent.CONTAINER_EVENT_MASK)) {
                ContainerEvent e = new ContainerEvent(this,
                                     ContainerEvent.COMPONENT_ADDED,
                                     comp);
                dispatchEvent(e);
            }

            comp.createHierarchyEvents(HierarchyEvent.HIERARCHY_CHANGED, comp,
                                       this, HierarchyEvent.PARENT_CHANGED,
                                       Toolkit.enabledOnToolkit(AWTEvent.HIERARCHY_EVENT_MASK));
            if (peer != null && layoutMgr == null && isVisible()) {
                updateCursorImmediately();
            }
        }
    }

    @Override
    final boolean updateChildGraphicsData(GraphicsConfiguration gc) {
        checkTreeLock();

        boolean ret = false;

        for (Component comp : component) {
            if (comp != null) {
                ret |= comp.updateGraphicsData(gc);
            }
        }
        return ret;
    }

    /**
     * Checks that all Components that this Container contains are on
     * the same GraphicsDevice as this Container.  If not, throws an
     * IllegalArgumentException.
     */
    void checkGD(String stringID) {
        for (Component comp : component) {
            if (comp != null) {
                comp.checkGD(stringID);
            }
        }
    }

    /**
     * Removes the component, specified by {@code index},
     * from this container.
     * This method also notifies the layout manager to remove the
     * component from this container's layout via the
     * {@code removeLayoutComponent} method.
     * <p>
     * This method changes layout-related information, and therefore,
     * invalidates the component hierarchy. If the container has already been
     * displayed, the hierarchy must be validated thereafter in order to
     * reflect the changes.
     *
     *
     * @param     index   the index of the component to be removed
     * @throws ArrayIndexOutOfBoundsException if {@code index} is not in
     *         range {@code [0, getComponentCount()-1]}
     * @see #add
     * @see #invalidate
     * @see #validate
     * @see #getComponentCount
     * @since 1.1
     */
    public void remove(int index) {
        synchronized (getTreeLock()) {
            if (index < 0  || index >= component.size()) {
                throw new ArrayIndexOutOfBoundsException(index);
            }
            Component comp = component.get(index);
            if (peer != null) {
                comp.removeNotify();
            }
            if (layoutMgr != null) {
                layoutMgr.removeLayoutComponent(comp);
            }

            adjustListeningChildren(AWTEvent.HIERARCHY_EVENT_MASK,
                -comp.numListening(AWTEvent.HIERARCHY_EVENT_MASK));
            adjustListeningChildren(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK,
                -comp.numListening(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK));
            adjustDescendants(-(comp.countHierarchyMembers()));

            comp.parent = null;
            component.remove(index);
            comp.setGraphicsConfiguration(null);

            invalidateIfValid();
            if (containerListener != null ||
                (eventMask & AWTEvent.CONTAINER_EVENT_MASK) != 0 ||
                Toolkit.enabledOnToolkit(AWTEvent.CONTAINER_EVENT_MASK)) {
                ContainerEvent e = new ContainerEvent(this,
                                     ContainerEvent.COMPONENT_REMOVED,
                                     comp);
                dispatchEvent(e);
            }

            comp.createHierarchyEvents(HierarchyEvent.HIERARCHY_CHANGED, comp,
                                       this, HierarchyEvent.PARENT_CHANGED,
                                       Toolkit.enabledOnToolkit(AWTEvent.HIERARCHY_EVENT_MASK));
            if (peer != null && layoutMgr == null && isVisible()) {
                updateCursorImmediately();
            }
        }
    }

    /**
     * Removes the specified component from this container.
     * This method also notifies the layout manager to remove the
     * component from this container's layout via the
     * {@code removeLayoutComponent} method.
     * <p>
     * This method changes layout-related information, and therefore,
     * invalidates the component hierarchy. If the container has already been
     * displayed, the hierarchy must be validated thereafter in order to
     * reflect the changes.
     *
     * @param comp the component to be removed
     * @throws NullPointerException if {@code comp} is {@code null}
     * @see #add
     * @see #invalidate
     * @see #validate
     * @see #remove(int)
     */
    public void remove(Component comp) {
        synchronized (getTreeLock()) {
            if (comp.parent == this)  {
                int index = component.indexOf(comp);
                if (index >= 0) {
                    remove(index);
                }
            }
        }
    }

    /**
     * Removes all the components from this container.
     * This method also notifies the layout manager to remove the
     * components from this container's layout via the
     * {@code removeLayoutComponent} method.
     * <p>
     * This method changes layout-related information, and therefore,
     * invalidates the component hierarchy. If the container has already been
     * displayed, the hierarchy must be validated thereafter in order to
     * reflect the changes.
     *
     * @see #add
     * @see #remove
     * @see #invalidate
     */
    public void removeAll() {
        synchronized (getTreeLock()) {
            adjustListeningChildren(AWTEvent.HIERARCHY_EVENT_MASK,
                                    -listeningChildren);
            adjustListeningChildren(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK,
                                    -listeningBoundsChildren);
            adjustDescendants(-descendantsCount);

            while (!component.isEmpty()) {
                Component comp = component.remove(component.size()-1);

                if (peer != null) {
                    comp.removeNotify();
                }
                if (layoutMgr != null) {
                    layoutMgr.removeLayoutComponent(comp);
                }
                comp.parent = null;
                comp.setGraphicsConfiguration(null);
                if (containerListener != null ||
                   (eventMask & AWTEvent.CONTAINER_EVENT_MASK) != 0 ||
                    Toolkit.enabledOnToolkit(AWTEvent.CONTAINER_EVENT_MASK)) {
                    ContainerEvent e = new ContainerEvent(this,
                                     ContainerEvent.COMPONENT_REMOVED,
                                     comp);
                    dispatchEvent(e);
                }

                comp.createHierarchyEvents(HierarchyEvent.HIERARCHY_CHANGED,
                                           comp, this,
                                           HierarchyEvent.PARENT_CHANGED,
                                           Toolkit.enabledOnToolkit(AWTEvent.HIERARCHY_EVENT_MASK));
            }
            if (peer != null && layoutMgr == null && isVisible()) {
                updateCursorImmediately();
            }
            invalidateIfValid();
        }
    }

    // Should only be called while holding tree lock
    int numListening(long mask) {
        int superListening = super.numListening(mask);

        if (mask == AWTEvent.HIERARCHY_EVENT_MASK) {
            if (eventLog.isLoggable(PlatformLogger.Level.FINE)) {
                // Verify listeningChildren is correct
                int sum = 0;
                for (Component comp : component) {
                    sum += comp.numListening(mask);
                }
                if (listeningChildren != sum) {
                    eventLog.fine("Assertion (listeningChildren == sum) failed");
                }
            }
            return listeningChildren + superListening;
        } else if (mask == AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK) {
            if (eventLog.isLoggable(PlatformLogger.Level.FINE)) {
                // Verify listeningBoundsChildren is correct
                int sum = 0;
                for (Component comp : component) {
                    sum += comp.numListening(mask);
                }
                if (listeningBoundsChildren != sum) {
                    eventLog.fine("Assertion (listeningBoundsChildren == sum) failed");
                }
            }
            return listeningBoundsChildren + superListening;
        } else {
            // assert false;
            if (eventLog.isLoggable(PlatformLogger.Level.FINE)) {
                eventLog.fine("This code must never be reached");
            }
            return superListening;
        }
    }

    // Should only be called while holding tree lock
    void adjustListeningChildren(long mask, int num) {
        if (eventLog.isLoggable(PlatformLogger.Level.FINE)) {
            boolean toAssert = (mask == AWTEvent.HIERARCHY_EVENT_MASK ||
                                mask == AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK ||
                                mask == (AWTEvent.HIERARCHY_EVENT_MASK |
                                         AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK));
            if (!toAssert) {
                eventLog.fine("Assertion failed");
            }
        }

        if (num == 0)
            return;

        if ((mask & AWTEvent.HIERARCHY_EVENT_MASK) != 0) {
            listeningChildren += num;
        }
        if ((mask & AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK) != 0) {
            listeningBoundsChildren += num;
        }

        adjustListeningChildrenOnParent(mask, num);
    }

    // Should only be called while holding tree lock
    void adjustDescendants(int num) {
        if (num == 0)
            return;

        descendantsCount += num;
        adjustDescendantsOnParent(num);
    }

    // Should only be called while holding tree lock
    void adjustDescendantsOnParent(int num) {
        if (parent != null) {
            parent.adjustDescendants(num);
        }
    }

    // Should only be called while holding tree lock
    int countHierarchyMembers() {
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            // Verify descendantsCount is correct
            int sum = 0;
            for (Component comp : component) {
                sum += comp.countHierarchyMembers();
            }
            if (descendantsCount != sum) {
                log.fine("Assertion (descendantsCount == sum) failed");
            }
        }
        return descendantsCount + 1;
    }

    private int getListenersCount(int id, boolean enabledOnToolkit) {
        checkTreeLock();
        if (enabledOnToolkit) {
            return descendantsCount;
        }
        switch (id) {
          case HierarchyEvent.HIERARCHY_CHANGED:
            return listeningChildren;
          case HierarchyEvent.ANCESTOR_MOVED:
          case HierarchyEvent.ANCESTOR_RESIZED:
            return listeningBoundsChildren;
          default:
            return 0;
        }
    }

    final int createHierarchyEvents(int id, Component changed,
        Container changedParent, long changeFlags, boolean enabledOnToolkit)
    {
        checkTreeLock();
        int listeners = getListenersCount(id, enabledOnToolkit);

        for (int count = listeners, i = 0; count > 0; i++) {
            count -= component.get(i).createHierarchyEvents(id, changed,
                changedParent, changeFlags, enabledOnToolkit);
        }
        return listeners +
            super.createHierarchyEvents(id, changed, changedParent,
                                        changeFlags, enabledOnToolkit);
    }

    final void createChildHierarchyEvents(int id, long changeFlags,
        boolean enabledOnToolkit)
    {
        checkTreeLock();
        if (component.isEmpty()) {
            return;
        }
        int listeners = getListenersCount(id, enabledOnToolkit);

        for (int count = listeners, i = 0; count > 0; i++) {
            count -= component.get(i).createHierarchyEvents(id, this, parent,
                changeFlags, enabledOnToolkit);
        }
    }

    /**
     * Gets the layout manager for this container.
     *
     * @see #doLayout
     * @see #setLayout
     * @return the current layout manager for this container
     */
    public LayoutManager getLayout() {
        return layoutMgr;
    }

    /**
     * Sets the layout manager for this container.
     * <p>
     * This method changes layout-related information, and therefore,
     * invalidates the component hierarchy.
     *
     * @param mgr the specified layout manager
     * @see #doLayout
     * @see #getLayout
     * @see #invalidate
     */
    public void setLayout(LayoutManager mgr) {
        layoutMgr = mgr;
        invalidateIfValid();
    }

    /**
     * Causes this container to lay out its components.  Most programs
     * should not call this method directly, but should invoke
     * the {@code validate} method instead.
     * @see LayoutManager#layoutContainer
     * @see #setLayout
     * @see #validate
     * @since 1.1
     */
    public void doLayout() {
        layout();
    }

    /**
     * @deprecated As of JDK version 1.1,
     * replaced by {@code doLayout()}.
     */
    @Deprecated
    public void layout() {
        LayoutManager layoutMgr = this.layoutMgr;
        if (layoutMgr != null) {
            layoutMgr.layoutContainer(this);
        }
    }

    /**
     * Indicates if this container is a <i>validate root</i>.
     * <p>
     * Layout-related changes, such as bounds of the validate root descendants,
     * do not affect the layout of the validate root parent. This peculiarity
     * enables the {@code invalidate()} method to stop invalidating the
     * component hierarchy when the method encounters a validate root. However,
     * to preserve backward compatibility this new optimized behavior is
     * enabled only when the {@code java.awt.smartInvalidate} system property
     * value is set to {@code true}.
     * <p>
     * If a component hierarchy contains validate roots and the new optimized
     * {@code invalidate()} behavior is enabled, the {@code validate()} method
     * must be invoked on the validate root of a previously invalidated
     * component to restore the validity of the hierarchy later. Otherwise,
     * calling the {@code validate()} method on the top-level container (such
     * as a {@code Frame} object) should be used to restore the validity of the
     * component hierarchy.
     * <p>
     * The {@code Window} class and the {@code Applet} class are the validate
     * roots in AWT.  Swing introduces more validate roots.
     *
     * @return whether this container is a validate root
     * @see #invalidate
     * @see java.awt.Component#invalidate
     * @see javax.swing.JComponent#isValidateRoot
     * @see javax.swing.JComponent#revalidate
     * @since 1.7
     */
    public boolean isValidateRoot() {
        return false;
    }

    // Don't lazy-read because every app uses invalidate()
    @SuppressWarnings("removal")
    private static final boolean isJavaAwtSmartInvalidate
            = AccessController.doPrivileged(
                new GetBooleanAction("java.awt.smartInvalidate"));

    /**
     * Invalidates the parent of the container unless the container
     * is a validate root.
     */
    @Override
    void invalidateParent() {
        if (!isJavaAwtSmartInvalidate || !isValidateRoot()) {
            super.invalidateParent();
        }
    }

    /**
     * Invalidates the container.
     * <p>
     * If the {@code LayoutManager} installed on this container is an instance
     * of the {@code LayoutManager2} interface, then
     * the {@link LayoutManager2#invalidateLayout(Container)} method is invoked
     * on it supplying this {@code Container} as the argument.
     * <p>
     * Afterwards this method marks this container invalid, and invalidates its
     * ancestors. See the {@link Component#invalidate} method for more details.
     *
     * @see #validate
     * @see #layout
     * @see LayoutManager2
     */
    @Override
    public void invalidate() {
        LayoutManager layoutMgr = this.layoutMgr;
        if (layoutMgr instanceof LayoutManager2) {
            LayoutManager2 lm = (LayoutManager2) layoutMgr;
            lm.invalidateLayout(this);
        }
        super.invalidate();
    }

    /**
     * Validates this container and all of its subcomponents.
     * <p>
     * Validating a container means laying out its subcomponents.
     * Layout-related changes, such as setting the bounds of a component, or
     * adding a component to the container, invalidate the container
     * automatically.  Note that the ancestors of the container may be
     * invalidated also (see {@link Component#invalidate} for details.)
     * Therefore, to restore the validity of the hierarchy, the {@code
     * validate()} method should be invoked on the top-most invalid
     * container of the hierarchy.
     * <p>
     * Validating the container may be a quite time-consuming operation. For
     * performance reasons a developer may postpone the validation of the
     * hierarchy till a set of layout-related operations completes, e.g. after
     * adding all the children to the container.
     * <p>
     * If this {@code Container} is not valid, this method invokes
     * the {@code validateTree} method and marks this {@code Container}
     * as valid. Otherwise, no action is performed.
     *
     * @see #add(java.awt.Component)
     * @see #invalidate
     * @see Container#isValidateRoot
     * @see javax.swing.JComponent#revalidate()
     * @see #validateTree
     */
    public void validate() {
        boolean updateCur = false;
        synchronized (getTreeLock()) {
            if ((!isValid() || descendUnconditionallyWhenValidating)
                    && peer != null)
            {
                ContainerPeer p = null;
                if (peer instanceof ContainerPeer) {
                    p = (ContainerPeer) peer;
                }
                if (p != null) {
                    p.beginValidate();
                }
                validateTree();
                if (p != null) {
                    p.endValidate();
                    // Avoid updating cursor if this is an internal call.
                    // See validateUnconditionally() for details.
                    if (!descendUnconditionallyWhenValidating) {
                        updateCur = isVisible();
                    }
                }
            }
        }
        if (updateCur) {
            updateCursorImmediately();
        }
    }

    /**
     * Indicates whether valid containers should also traverse their
     * children and call the validateTree() method on them.
     *
     * Synchronization: TreeLock.
     *
     * The field is allowed to be static as long as the TreeLock itself is
     * static.
     *
     * @see #validateUnconditionally()
     */
    private static boolean descendUnconditionallyWhenValidating = false;

    /**
     * Unconditionally validate the component hierarchy.
     */
    final void validateUnconditionally() {
        boolean updateCur = false;
        synchronized (getTreeLock()) {
            descendUnconditionallyWhenValidating = true;

            validate();
            if (peer instanceof ContainerPeer) {
                updateCur = isVisible();
            }

            descendUnconditionallyWhenValidating = false;
        }
        if (updateCur) {
            updateCursorImmediately();
        }
    }

    /**
     * Recursively descends the container tree and recomputes the
     * layout for any subtrees marked as needing it (those marked as
     * invalid).  Synchronization should be provided by the method
     * that calls this one:  {@code validate}.
     *
     * @see #doLayout
     * @see #validate
     */
    protected void validateTree() {
        checkTreeLock();
        if (!isValid() || descendUnconditionallyWhenValidating) {
            if (peer instanceof ContainerPeer) {
                ((ContainerPeer)peer).beginLayout();
            }
            if (!isValid()) {
                doLayout();
            }
            for (int i = 0; i < component.size(); i++) {
                Component comp = component.get(i);
                if (   (comp instanceof Container)
                       && !(comp instanceof Window)
                       && (!comp.isValid() ||
                           descendUnconditionallyWhenValidating))
                {
                    ((Container)comp).validateTree();
                } else {
                    comp.validate();
                }
            }
            if (peer instanceof ContainerPeer) {
                ((ContainerPeer)peer).endLayout();
            }
        }
        super.validate();
    }

    /**
     * Recursively descends the container tree and invalidates all
     * contained components.
     */
    void invalidateTree() {
        synchronized (getTreeLock()) {
            for (int i = 0; i < component.size(); i++) {
                Component comp = component.get(i);
                if (comp instanceof Container) {
                    ((Container)comp).invalidateTree();
                }
                else {
                    comp.invalidateIfValid();
                }
            }
            invalidateIfValid();
        }
    }

    /**
     * Sets the font of this container.
     * <p>
     * This method changes layout-related information, and therefore,
     * invalidates the component hierarchy.
     *
     * @param f The font to become this container's font.
     * @see Component#getFont
     * @see #invalidate
     * @since 1.0
     */
    public void setFont(Font f) {
        boolean shouldinvalidate = false;

        Font oldfont = getFont();
        super.setFont(f);
        Font newfont = getFont();
        if (newfont != oldfont && (oldfont == null ||
                                   !oldfont.equals(newfont))) {
            invalidateTree();
        }
    }

    /**
     * Returns the preferred size of this container.  If the preferred size has
     * not been set explicitly by {@link Component#setPreferredSize(Dimension)}
     * and this {@code Container} has a {@code non-null} {@link LayoutManager},
     * then {@link LayoutManager#preferredLayoutSize(Container)}
     * is used to calculate the preferred size.
     *
     * <p>Note: some implementations may cache the value returned from the
     * {@code LayoutManager}.  Implementations that cache need not invoke
     * {@code preferredLayoutSize} on the {@code LayoutManager} every time
     * this method is invoked, rather the {@code LayoutManager} will only
     * be queried after the {@code Container} becomes invalid.
     *
     * @return    an instance of {@code Dimension} that represents
     *                the preferred size of this container.
     * @see       #getMinimumSize
     * @see       #getMaximumSize
     * @see       #getLayout
     * @see       LayoutManager#preferredLayoutSize(Container)
     * @see       Component#getPreferredSize
     */
    public Dimension getPreferredSize() {
        return preferredSize();
    }

    /**
     * @deprecated As of JDK version 1.1,
     * replaced by {@code getPreferredSize()}.
     */
    @Deprecated
    public Dimension preferredSize() {
        /* Avoid grabbing the lock if a reasonable cached size value
         * is available.
         */
        Dimension dim = prefSize;
        if (dim == null || !(isPreferredSizeSet() || isValid())) {
            synchronized (getTreeLock()) {
                prefSize = (layoutMgr != null) ?
                    layoutMgr.preferredLayoutSize(this) :
                    super.preferredSize();
                dim = prefSize;
            }
        }
        if (dim != null){
            return new Dimension(dim);
        }
        else{
            return dim;
        }
    }

    /**
     * Returns the minimum size of this container.  If the minimum size has
     * not been set explicitly by {@link Component#setMinimumSize(Dimension)}
     * and this {@code Container} has a {@code non-null} {@link LayoutManager},
     * then {@link LayoutManager#minimumLayoutSize(Container)}
     * is used to calculate the minimum size.
     *
     * <p>Note: some implementations may cache the value returned from the
     * {@code LayoutManager}.  Implementations that cache need not invoke
     * {@code minimumLayoutSize} on the {@code LayoutManager} every time
     * this method is invoked, rather the {@code LayoutManager} will only
     * be queried after the {@code Container} becomes invalid.
     *
     * @return    an instance of {@code Dimension} that represents
     *                the minimum size of this container.
     * @see       #getPreferredSize
     * @see       #getMaximumSize
     * @see       #getLayout
     * @see       LayoutManager#minimumLayoutSize(Container)
     * @see       Component#getMinimumSize
     * @since     1.1
     */
    public Dimension getMinimumSize() {
        return minimumSize();
    }

    /**
     * @deprecated As of JDK version 1.1,
     * replaced by {@code getMinimumSize()}.
     */
    @Deprecated
    public Dimension minimumSize() {
        /* Avoid grabbing the lock if a reasonable cached size value
         * is available.
         */
        Dimension dim = minSize;
        if (dim == null || !(isMinimumSizeSet() || isValid())) {
            synchronized (getTreeLock()) {
                minSize = (layoutMgr != null) ?
                    layoutMgr.minimumLayoutSize(this) :
                    super.minimumSize();
                dim = minSize;
            }
        }
        if (dim != null){
            return new Dimension(dim);
        }
        else{
            return dim;
        }
    }

    /**
     * Returns the maximum size of this container.  If the maximum size has
     * not been set explicitly by {@link Component#setMaximumSize(Dimension)}
     * and the {@link LayoutManager} installed on this {@code Container}
     * is an instance of {@link LayoutManager2}, then
     * {@link LayoutManager2#maximumLayoutSize(Container)}
     * is used to calculate the maximum size.
     *
     * <p>Note: some implementations may cache the value returned from the
     * {@code LayoutManager2}.  Implementations that cache need not invoke
     * {@code maximumLayoutSize} on the {@code LayoutManager2} every time
     * this method is invoked, rather the {@code LayoutManager2} will only
     * be queried after the {@code Container} becomes invalid.
     *
     * @return    an instance of {@code Dimension} that represents
     *                the maximum size of this container.
     * @see       #getPreferredSize
     * @see       #getMinimumSize
     * @see       #getLayout
     * @see       LayoutManager2#maximumLayoutSize(Container)
     * @see       Component#getMaximumSize
     */
    public Dimension getMaximumSize() {
        /* Avoid grabbing the lock if a reasonable cached size value
         * is available.
         */
        Dimension dim = maxSize;
        if (dim == null || !(isMaximumSizeSet() || isValid())) {
            synchronized (getTreeLock()) {
               if (layoutMgr instanceof LayoutManager2) {
                    LayoutManager2 lm = (LayoutManager2) layoutMgr;
                    maxSize = lm.maximumLayoutSize(this);
               } else {
                    maxSize = super.getMaximumSize();
               }
               dim = maxSize;
            }
        }
        if (dim != null){
            return new Dimension(dim);
        }
        else{
            return dim;
        }
    }

    /**
     * Returns the alignment along the x axis.  This specifies how
     * the component would like to be aligned relative to other
     * components.  The value should be a number between 0 and 1
     * where 0 represents alignment along the origin, 1 is aligned
     * the furthest away from the origin, 0.5 is centered, etc.
     */
    public float getAlignmentX() {
        float xAlign;
        if (layoutMgr instanceof LayoutManager2) {
            synchronized (getTreeLock()) {
                LayoutManager2 lm = (LayoutManager2) layoutMgr;
                xAlign = lm.getLayoutAlignmentX(this);
            }
        } else {
            xAlign = super.getAlignmentX();
        }
        return xAlign;
    }

    /**
     * Returns the alignment along the y axis.  This specifies how
     * the component would like to be aligned relative to other
     * components.  The value should be a number between 0 and 1
     * where 0 represents alignment along the origin, 1 is aligned
     * the furthest away from the origin, 0.5 is centered, etc.
     */
    public float getAlignmentY() {
        float yAlign;
        if (layoutMgr instanceof LayoutManager2) {
            synchronized (getTreeLock()) {
                LayoutManager2 lm = (LayoutManager2) layoutMgr;
                yAlign = lm.getLayoutAlignmentY(this);
            }
        } else {
            yAlign = super.getAlignmentY();
        }
        return yAlign;
    }

    /**
     * Paints the container. This forwards the paint to any lightweight
     * components that are children of this container. If this method is
     * reimplemented, super.paint(g) should be called so that lightweight
     * components are properly rendered. If a child component is entirely
     * clipped by the current clipping setting in g, paint() will not be
     * forwarded to that child.
     *
     * @param g the specified Graphics window
     * @see   Component#update(Graphics)
     */
    public void paint(Graphics g) {
        if (isShowing()) {
            synchronized (getObjectLock()) {
                if (printing) {
                    if (printingThreads.contains(Thread.currentThread())) {
                        return;
                    }
                }
            }

            // The container is showing on screen and
            // this paint() is not called from print().
            // Paint self and forward the paint to lightweight subcomponents.

            // super.paint(); -- Don't bother, since it's a NOP.

            GraphicsCallback.PaintCallback.getInstance().
                runComponents(getComponentsSync(), g, GraphicsCallback.LIGHTWEIGHTS);
        }
    }

    /**
     * Updates the container.  This forwards the update to any lightweight
     * components that are children of this container.  If this method is
     * reimplemented, super.update(g) should be called so that lightweight
     * components are properly rendered.  If a child component is entirely
     * clipped by the current clipping setting in g, update() will not be
     * forwarded to that child.
     *
     * @param g the specified Graphics window
     * @see   Component#update(Graphics)
     */
    public void update(Graphics g) {
        if (isShowing()) {
            if (! (peer instanceof LightweightPeer)) {
                g.clearRect(0, 0, width, height);
            }
            paint(g);
        }
    }

    /**
     * Prints the container. This forwards the print to any lightweight
     * components that are children of this container. If this method is
     * reimplemented, super.print(g) should be called so that lightweight
     * components are properly rendered. If a child component is entirely
     * clipped by the current clipping setting in g, print() will not be
     * forwarded to that child.
     *
     * @param g the specified Graphics window
     * @see   Component#update(Graphics)
     */
    public void print(Graphics g) {
        if (isShowing()) {
            Thread t = Thread.currentThread();
            try {
                synchronized (getObjectLock()) {
                    if (printingThreads == null) {
                        printingThreads = new HashSet<>();
                    }
                    printingThreads.add(t);
                    printing = true;
                }
                super.print(g);  // By default, Component.print() calls paint()
            } finally {
                synchronized (getObjectLock()) {
                    printingThreads.remove(t);
                    printing = !printingThreads.isEmpty();
                }
            }

            GraphicsCallback.PrintCallback.getInstance().
                runComponents(getComponentsSync(), g, GraphicsCallback.LIGHTWEIGHTS);
        }
    }

    /**
     * Paints each of the components in this container.
     * @param     g   the graphics context.
     * @see       Component#paint
     * @see       Component#paintAll
     */
    public void paintComponents(Graphics g) {
        if (isShowing()) {
            GraphicsCallback.PaintAllCallback.getInstance().
                runComponents(getComponentsSync(), g, GraphicsCallback.TWO_PASSES);
        }
    }

    /**
     * Simulates the peer callbacks into java.awt for printing of
     * lightweight Containers.
     * @param     g   the graphics context to use for printing.
     * @see       Component#printAll
     * @see       #printComponents
     */
    void lightweightPaint(Graphics g) {
        super.lightweightPaint(g);
        paintHeavyweightComponents(g);
    }

    /**
     * Prints all the heavyweight subcomponents.
     */
    void paintHeavyweightComponents(Graphics g) {
        if (isShowing()) {
            GraphicsCallback.PaintHeavyweightComponentsCallback.getInstance().
                runComponents(getComponentsSync(), g,
                              GraphicsCallback.LIGHTWEIGHTS | GraphicsCallback.HEAVYWEIGHTS);
        }
    }

    /**
     * Prints each of the components in this container.
     * @param     g   the graphics context.
     * @see       Component#print
     * @see       Component#printAll
     */
    public void printComponents(Graphics g) {
        if (isShowing()) {
            GraphicsCallback.PrintAllCallback.getInstance().
                runComponents(getComponentsSync(), g, GraphicsCallback.TWO_PASSES);
        }
    }

    /**
     * Simulates the peer callbacks into java.awt for printing of
     * lightweight Containers.
     * @param     g   the graphics context to use for printing.
     * @see       Component#printAll
     * @see       #printComponents
     */
    void lightweightPrint(Graphics g) {
        super.lightweightPrint(g);
        printHeavyweightComponents(g);
    }

    /**
     * Prints all the heavyweight subcomponents.
     */
    void printHeavyweightComponents(Graphics g) {
        if (isShowing()) {
            GraphicsCallback.PrintHeavyweightComponentsCallback.getInstance().
                runComponents(getComponentsSync(), g,
                              GraphicsCallback.LIGHTWEIGHTS | GraphicsCallback.HEAVYWEIGHTS);
        }
    }

    /**
     * Adds the specified container listener to receive container events
     * from this container.
     * If l is null, no exception is thrown and no action is performed.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param    l the container listener
     *
     * @see #removeContainerListener
     * @see #getContainerListeners
     */
    public synchronized void addContainerListener(ContainerListener l) {
        if (l == null) {
            return;
        }
        containerListener = AWTEventMulticaster.add(containerListener, l);
        newEventsOnly = true;
    }

    /**
     * Removes the specified container listener so it no longer receives
     * container events from this container.
     * If l is null, no exception is thrown and no action is performed.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param   l the container listener
     *
     * @see #addContainerListener
     * @see #getContainerListeners
     */
    public synchronized void removeContainerListener(ContainerListener l) {
        if (l == null) {
            return;
        }
        containerListener = AWTEventMulticaster.remove(containerListener, l);
    }

    /**
     * Returns an array of all the container listeners
     * registered on this container.
     *
     * @return all of this container's {@code ContainerListener}s
     *         or an empty array if no container
     *         listeners are currently registered
     *
     * @see #addContainerListener
     * @see #removeContainerListener
     * @since 1.4
     */
    public synchronized ContainerListener[] getContainerListeners() {
        return getListeners(ContainerListener.class);
    }

    /**
     * Returns an array of all the objects currently registered
     * as <code><em>Foo</em>Listener</code>s
     * upon this {@code Container}.
     * <code><em>Foo</em>Listener</code>s are registered using the
     * <code>add<em>Foo</em>Listener</code> method.
     *
     * <p>
     * You can specify the {@code listenerType} argument
     * with a class literal, such as
     * <code><em>Foo</em>Listener.class</code>.
     * For example, you can query a
     * {@code Container c}
     * for its container listeners with the following code:
     *
     * <pre>ContainerListener[] cls = (ContainerListener[])(c.getListeners(ContainerListener.class));</pre>
     *
     * If no such listeners exist, this method returns an empty array.
     *
     * @param listenerType the type of listeners requested; this parameter
     *          should specify an interface that descends from
     *          {@code java.util.EventListener}
     * @return an array of all objects registered as
     *          <code><em>Foo</em>Listener</code>s on this container,
     *          or an empty array if no such listeners have been added
     * @exception ClassCastException if {@code listenerType}
     *          doesn't specify a class or interface that implements
     *          {@code java.util.EventListener}
     * @exception NullPointerException if {@code listenerType} is {@code null}
     *
     * @see #getContainerListeners
     *
     * @since 1.3
     */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        EventListener l = null;
        if  (listenerType == ContainerListener.class) {
            l = containerListener;
        } else {
            return super.getListeners(listenerType);
        }
        return AWTEventMulticaster.getListeners(l, listenerType);
    }

    // REMIND: remove when filtering is done at lower level
    boolean eventEnabled(AWTEvent e) {
        int id = e.getID();

        if (id == ContainerEvent.COMPONENT_ADDED ||
            id == ContainerEvent.COMPONENT_REMOVED) {
            if ((eventMask & AWTEvent.CONTAINER_EVENT_MASK) != 0 ||
                containerListener != null) {
                return true;
            }
            return false;
        }
        return super.eventEnabled(e);
    }

    /**
     * Processes events on this container. If the event is a
     * {@code ContainerEvent}, it invokes the
     * {@code processContainerEvent} method, else it invokes
     * its superclass's {@code processEvent}.
     * <p>Note that if the event parameter is {@code null}
     * the behavior is unspecified and may result in an
     * exception.
     *
     * @param e the event
     */
    protected void processEvent(AWTEvent e) {
        if (e instanceof ContainerEvent) {
            processContainerEvent((ContainerEvent)e);
            return;
        }
        super.processEvent(e);
    }

    /**
     * Processes container events occurring on this container by
     * dispatching them to any registered ContainerListener objects.
     * NOTE: This method will not be called unless container events
     * are enabled for this component; this happens when one of the
     * following occurs:
     * <ul>
     * <li>A ContainerListener object is registered via
     *     {@code addContainerListener}
     * <li>Container events are enabled via {@code enableEvents}
     * </ul>
     * <p>Note that if the event parameter is {@code null}
     * the behavior is unspecified and may result in an
     * exception.
     *
     * @param e the container event
     * @see Component#enableEvents
     */
    protected void processContainerEvent(ContainerEvent e) {
        ContainerListener listener = containerListener;
        if (listener != null) {
            switch(e.getID()) {
              case ContainerEvent.COMPONENT_ADDED:
                listener.componentAdded(e);
                break;
              case ContainerEvent.COMPONENT_REMOVED:
                listener.componentRemoved(e);
                break;
            }
        }
    }

    /*
     * Dispatches an event to this component or one of its sub components.
     * Create ANCESTOR_RESIZED and ANCESTOR_MOVED events in response to
     * COMPONENT_RESIZED and COMPONENT_MOVED events. We have to do this
     * here instead of in processComponentEvent because ComponentEvents
     * may not be enabled for this Container.
     * @param e the event
     */
    void dispatchEventImpl(AWTEvent e) {
        if ((dispatcher != null) && dispatcher.dispatchEvent(e)) {
            // event was sent to a lightweight component.  The
            // native-produced event sent to the native container
            // must be properly disposed of by the peer, so it
            // gets forwarded.  If the native host has been removed
            // as a result of the sending the lightweight event,
            // the peer reference will be null.
            e.consume();
            if (peer != null) {
                peer.handleEvent(e);
            }
            return;
        }

        super.dispatchEventImpl(e);

        synchronized (getTreeLock()) {
            switch (e.getID()) {
              case ComponentEvent.COMPONENT_RESIZED:
                createChildHierarchyEvents(HierarchyEvent.ANCESTOR_RESIZED, 0,
                                           Toolkit.enabledOnToolkit(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK));
                break;
              case ComponentEvent.COMPONENT_MOVED:
                createChildHierarchyEvents(HierarchyEvent.ANCESTOR_MOVED, 0,
                                       Toolkit.enabledOnToolkit(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK));
                break;
              default:
                break;
            }
        }
    }

    /*
     * Dispatches an event to this component, without trying to forward
     * it to any subcomponents
     * @param e the event
     */
    void dispatchEventToSelf(AWTEvent e) {
        super.dispatchEventImpl(e);
    }

    /**
     * Fetches the top-most (deepest) lightweight component that is interested
     * in receiving mouse events.
     */
    Component getMouseEventTarget(int x, int y, boolean includeSelf) {
        return getMouseEventTarget(x, y, includeSelf,
                                   MouseEventTargetFilter.FILTER,
                                   !SEARCH_HEAVYWEIGHTS);
    }

    /**
     * Fetches the top-most (deepest) component to receive SunDropTargetEvents.
     */
    Component getDropTargetEventTarget(int x, int y, boolean includeSelf) {
        return getMouseEventTarget(x, y, includeSelf,
                                   DropTargetEventTargetFilter.FILTER,
                                   SEARCH_HEAVYWEIGHTS);
    }

    /**
     * A private version of getMouseEventTarget which has two additional
     * controllable behaviors. This method searches for the top-most
     * descendant of this container that contains the given coordinates
     * and is accepted by the given filter. The search will be constrained to
     * lightweight descendants if the last argument is {@code false}.
     *
     * @param filter EventTargetFilter instance to determine whether the
     *        given component is a valid target for this event.
     * @param searchHeavyweights if {@code false}, the method
     *        will bypass heavyweight components during the search.
     */
    private Component getMouseEventTarget(int x, int y, boolean includeSelf,
                                          EventTargetFilter filter,
                                          boolean searchHeavyweights) {
        Component comp = null;
        if (searchHeavyweights) {
            comp = getMouseEventTargetImpl(x, y, includeSelf, filter,
                                           SEARCH_HEAVYWEIGHTS,
                                           searchHeavyweights);
        }

        if (comp == null || comp == this) {
            comp = getMouseEventTargetImpl(x, y, includeSelf, filter,
                                           !SEARCH_HEAVYWEIGHTS,
                                           searchHeavyweights);
        }

        return comp;
    }

    /**
     * A private version of getMouseEventTarget which has three additional
     * controllable behaviors. This method searches for the top-most
     * descendant of this container that contains the given coordinates
     * and is accepted by the given filter. The search will be constrained to
     * descendants of only lightweight children or only heavyweight children
     * of this container depending on searchHeavyweightChildren. The search will
     * be constrained to only lightweight descendants of the searched children
     * of this container if searchHeavyweightDescendants is {@code false}.
     *
     * @param filter EventTargetFilter instance to determine whether the
     *        selected component is a valid target for this event.
     * @param searchHeavyweightChildren if {@code true}, the method
     *        will bypass immediate lightweight children during the search.
     *        If {@code false}, the methods will bypass immediate
     *        heavyweight children during the search.
     * @param searchHeavyweightDescendants if {@code false}, the method
     *        will bypass heavyweight descendants which are not immediate
     *        children during the search. If {@code true}, the method
     *        will traverse both lightweight and heavyweight descendants during
     *        the search.
     */
    private Component getMouseEventTargetImpl(int x, int y, boolean includeSelf,
                                         EventTargetFilter filter,
                                         boolean searchHeavyweightChildren,
                                         boolean searchHeavyweightDescendants) {
        synchronized (getTreeLock()) {

            for (int i = 0; i < component.size(); i++) {
                Component comp = component.get(i);
                if (comp != null && comp.visible &&
                    ((!searchHeavyweightChildren &&
                      comp.peer instanceof LightweightPeer) ||
                     (searchHeavyweightChildren &&
                      !(comp.peer instanceof LightweightPeer))) &&
                    comp.contains(x - comp.x, y - comp.y)) {

                    // found a component that intersects the point, see if there
                    // is a deeper possibility.
                    if (comp instanceof Container) {
                        Container child = (Container) comp;
                        Component deeper = child.getMouseEventTarget(
                                x - child.x,
                                y - child.y,
                                includeSelf,
                                filter,
                                searchHeavyweightDescendants);
                        if (deeper != null) {
                            return deeper;
                        }
                    } else {
                        if (filter.accept(comp)) {
                            // there isn't a deeper target, but this component
                            // is a target
                            return comp;
                        }
                    }
                }
            }

            boolean isPeerOK;
            boolean isMouseOverMe;

            isPeerOK = (peer instanceof LightweightPeer) || includeSelf;
            isMouseOverMe = contains(x,y);

            // didn't find a child target, return this component if it's
            // a possible target
            if (isMouseOverMe && isPeerOK && filter.accept(this)) {
                return this;
            }
            // no possible target
            return null;
        }
    }

    static interface EventTargetFilter {
        boolean accept(final Component comp);
    }

    static class MouseEventTargetFilter implements EventTargetFilter {
        static final EventTargetFilter FILTER = new MouseEventTargetFilter();

        private MouseEventTargetFilter() {}

        public boolean accept(final Component comp) {
            return (comp.eventMask & AWTEvent.MOUSE_MOTION_EVENT_MASK) != 0
                || (comp.eventMask & AWTEvent.MOUSE_EVENT_MASK) != 0
                || (comp.eventMask & AWTEvent.MOUSE_WHEEL_EVENT_MASK) != 0
                || comp.mouseListener != null
                || comp.mouseMotionListener != null
                || comp.mouseWheelListener != null;
        }
    }

    static class DropTargetEventTargetFilter implements EventTargetFilter {
        static final EventTargetFilter FILTER = new DropTargetEventTargetFilter();

        private DropTargetEventTargetFilter() {}

        public boolean accept(final Component comp) {
            DropTarget dt = comp.getDropTarget();
            return dt != null && dt.isActive();
        }
    }

    /**
     * This is called by lightweight components that want the containing
     * windowed parent to enable some kind of events on their behalf.
     * This is needed for events that are normally only dispatched to
     * windows to be accepted so that they can be forwarded downward to
     * the lightweight component that has enabled them.
     */
    void proxyEnableEvents(long events) {
        if (peer instanceof LightweightPeer) {
            // this container is lightweight.... continue sending it
            // upward.
            if (parent != null) {
                parent.proxyEnableEvents(events);
            }
        } else {
            // This is a native container, so it needs to host
            // one of it's children.  If this function is called before
            // a peer has been created we don't yet have a dispatcher
            // because it has not yet been determined if this instance
            // is lightweight.
            if (dispatcher != null) {
                dispatcher.enableEvents(events);
            }
        }
    }

    /**
     * @deprecated As of JDK version 1.1,
     * replaced by {@code dispatchEvent(AWTEvent e)}
     */
    @Deprecated
    public void deliverEvent(Event e) {
        Component comp = getComponentAt(e.x, e.y);
        if ((comp != null) && (comp != this)) {
            e.translate(-comp.x, -comp.y);
            comp.deliverEvent(e);
        } else {
            postEvent(e);
        }
    }

    /**
     * Locates the component that contains the x,y position.  The
     * top-most child component is returned in the case where there
     * is overlap in the components.  This is determined by finding
     * the component closest to the index 0 that claims to contain
     * the given point via Component.contains(), except that Components
     * which have native peers take precedence over those which do not
     * (i.e., lightweight Components).
     *
     * @param x the <i>x</i> coordinate
     * @param y the <i>y</i> coordinate
     * @return null if the component does not contain the position.
     * If there is no child component at the requested point and the
     * point is within the bounds of the container the container itself
     * is returned; otherwise the top-most child is returned.
     * @see Component#contains
     * @since 1.1
     */
    public Component getComponentAt(int x, int y) {
        return locate(x, y);
    }

    /**
     * @deprecated As of JDK version 1.1,
     * replaced by {@code getComponentAt(int, int)}.
     */
    @Deprecated
    public Component locate(int x, int y) {
        if (!contains(x, y)) {
            return null;
        }
        Component lightweight = null;
        synchronized (getTreeLock()) {
            // Optimized version of two passes:
            // see comment in sun.awt.SunGraphicsCallback
            for (final Component comp : component) {
                if (comp.contains(x - comp.x, y - comp.y)) {
                    if (!comp.isLightweight()) {
                        // return heavyweight component as soon as possible
                        return comp;
                    }
                    if (lightweight == null) {
                        // save and return later the first lightweight component
                        lightweight = comp;
                    }
                }
            }
        }
        return lightweight != null ? lightweight : this;
    }

    /**
     * Gets the component that contains the specified point.
     * @param      p   the point.
     * @return     returns the component that contains the point,
     *                 or {@code null} if the component does
     *                 not contain the point.
     * @see        Component#contains
     * @since      1.1
     */
    public Component getComponentAt(Point p) {
        return getComponentAt(p.x, p.y);
    }

    /**
     * Returns the position of the mouse pointer in this {@code Container}'s
     * coordinate space if the {@code Container} is under the mouse pointer,
     * otherwise returns {@code null}.
     * This method is similar to {@link Component#getMousePosition()} with the exception
     * that it can take the {@code Container}'s children into account.
     * If {@code allowChildren} is {@code false}, this method will return
     * a non-null value only if the mouse pointer is above the {@code Container}
     * directly, not above the part obscured by children.
     * If {@code allowChildren} is {@code true}, this method returns
     * a non-null value if the mouse pointer is above {@code Container} or any
     * of its descendants.
     *
     * @exception HeadlessException if GraphicsEnvironment.isHeadless() returns true
     * @param     allowChildren true if children should be taken into account
     * @see       Component#getMousePosition
     * @return    mouse coordinates relative to this {@code Component}, or null
     * @since     1.5
     */
    public Point getMousePosition(boolean allowChildren) throws HeadlessException {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }
        @SuppressWarnings("removal")
        PointerInfo pi = java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction<PointerInfo>() {
                public PointerInfo run() {
                    return MouseInfo.getPointerInfo();
                }
            }
        );
        synchronized (getTreeLock()) {
            Component inTheSameWindow = findUnderMouseInWindow(pi);
            if (isSameOrAncestorOf(inTheSameWindow, allowChildren)) {
                return  pointRelativeToComponent(pi.getLocation());
            }
            return null;
        }
    }

    boolean isSameOrAncestorOf(Component comp, boolean allowChildren) {
        return this == comp || (allowChildren && isParentOf(comp));
    }

    /**
     * Locates the visible child component that contains the specified
     * position.  The top-most child component is returned in the case
     * where there is overlap in the components.  If the containing child
     * component is a Container, this method will continue searching for
     * the deepest nested child component.  Components which are not
     * visible are ignored during the search.<p>
     *
     * The findComponentAt method is different from getComponentAt in
     * that getComponentAt only searches the Container's immediate
     * children; if the containing component is a Container,
     * findComponentAt will search that child to find a nested component.
     *
     * @param x the <i>x</i> coordinate
     * @param y the <i>y</i> coordinate
     * @return null if the component does not contain the position.
     * If there is no child component at the requested point and the
     * point is within the bounds of the container the container itself
     * is returned.
     * @see Component#contains
     * @see #getComponentAt
     * @since 1.2
     */
    public Component findComponentAt(int x, int y) {
        return findComponentAt(x, y, true);
    }

    /**
     * Private version of findComponentAt which has a controllable
     * behavior. Setting 'ignoreEnabled' to 'false' bypasses disabled
     * Components during the search. This behavior is used by the
     * lightweight cursor support in sun.awt.GlobalCursorManager.
     *
     * The addition of this feature is temporary, pending the
     * adoption of new, public API which exports this feature.
     */
    final Component findComponentAt(int x, int y, boolean ignoreEnabled) {
        synchronized (getTreeLock()) {
            if (isRecursivelyVisible()){
                return findComponentAtImpl(x, y, ignoreEnabled);
            }
        }
        return null;
    }

    final Component findComponentAtImpl(int x, int y, boolean ignoreEnabled) {
        // checkTreeLock(); commented for a performance reason

        if (!(contains(x, y) && visible && (ignoreEnabled || enabled))) {
            return null;
        }
        Component lightweight = null;
        // Optimized version of two passes:
        // see comment in sun.awt.SunGraphicsCallback
        for (final Component comp : component) {
            final int x1 = x - comp.x;
            final int y1 = y - comp.y;
            if (!comp.contains(x1, y1)) {
                continue; // fast path
            }
            if (!comp.isLightweight()) {
                final Component child = getChildAt(comp, x1, y1, ignoreEnabled);
                if (child != null) {
                    // return heavyweight component as soon as possible
                    return child;
                }
            } else {
                if (lightweight == null) {
                    // save and return later the first lightweight component
                    lightweight = getChildAt(comp, x1, y1, ignoreEnabled);
                }
            }
        }
        return lightweight != null ? lightweight : this;
    }

    /**
     * Helper method for findComponentAtImpl. Finds a child component using
     * findComponentAtImpl for Container and getComponentAt for Component.
     */
    private static Component getChildAt(Component comp, int x, int y,
                                        boolean ignoreEnabled) {
        if (comp instanceof Container) {
            comp = ((Container) comp).findComponentAtImpl(x, y,
                                                          ignoreEnabled);
        } else {
            comp = comp.getComponentAt(x, y);
        }
        if (comp != null && comp.visible &&
                (ignoreEnabled || comp.enabled)) {
            return comp;
        }
        return null;
    }

    /**
     * Locates the visible child component that contains the specified
     * point.  The top-most child component is returned in the case
     * where there is overlap in the components.  If the containing child
     * component is a Container, this method will continue searching for
     * the deepest nested child component.  Components which are not
     * visible are ignored during the search.<p>
     *
     * The findComponentAt method is different from getComponentAt in
     * that getComponentAt only searches the Container's immediate
     * children; if the containing component is a Container,
     * findComponentAt will search that child to find a nested component.
     *
     * @param      p   the point.
     * @return null if the component does not contain the position.
     * If there is no child component at the requested point and the
     * point is within the bounds of the container the container itself
     * is returned.
     * @throws NullPointerException if {@code p} is {@code null}
     * @see Component#contains
     * @see #getComponentAt
     * @since 1.2
     */
    public Component findComponentAt(Point p) {
        return findComponentAt(p.x, p.y);
    }

    /**
     * Makes this Container displayable by connecting it to
     * a native screen resource.  Making a container displayable will
     * cause all of its children to be made displayable.
     * This method is called internally by the toolkit and should
     * not be called directly by programs.
     * @see Component#isDisplayable
     * @see #removeNotify
     */
    public void addNotify() {
        synchronized (getTreeLock()) {
            // addNotify() on the children may cause proxy event enabling
            // on this instance, so we first call super.addNotify() and
            // possibly create an lightweight event dispatcher before calling
            // addNotify() on the children which may be lightweight.
            super.addNotify();
            if (! (peer instanceof LightweightPeer)) {
                dispatcher = new LightweightDispatcher(this);
            }

            // We shouldn't use iterator because of the Swing menu
            // implementation specifics:
            // the menu is being assigned as a child to JLayeredPane
            // instead of particular component so always affect
            // collection of component if menu is becoming shown or hidden.
            for (int i = 0; i < component.size(); i++) {
                component.get(i).addNotify();
            }
        }
    }

    /**
     * Makes this Container undisplayable by removing its connection
     * to its native screen resource.  Making a container undisplayable
     * will cause all of its children to be made undisplayable.
     * This method is called by the toolkit internally and should
     * not be called directly by programs.
     * @see Component#isDisplayable
     * @see #addNotify
     */
    public void removeNotify() {
        synchronized (getTreeLock()) {
            // We shouldn't use iterator because of the Swing menu
            // implementation specifics:
            // the menu is being assigned as a child to JLayeredPane
            // instead of particular component so always affect
            // collection of component if menu is becoming shown or hidden.
            for (int i = component.size()-1 ; i >= 0 ; i--) {
                Component comp = component.get(i);
                if (comp != null) {
                    // Fix for 6607170.
                    // We want to suppress focus change on disposal
                    // of the focused component. But because of focus
                    // is asynchronous, we should suppress focus change
                    // on every component in case it receives native focus
                    // in the process of disposal.
                    comp.setAutoFocusTransferOnDisposal(false);
                    comp.removeNotify();
                    comp.setAutoFocusTransferOnDisposal(true);
                 }
             }
            // If some of the children had focus before disposal then it still has.
            // Auto-transfer focus to the next (or previous) component if auto-transfer
            // is enabled.
            if (containsFocus() && KeyboardFocusManager.isAutoFocusTransferEnabledFor(this)) {
                if (!transferFocus(false)) {
                    transferFocusBackward(true);
                }
            }
            if ( dispatcher != null ) {
                dispatcher.dispose();
                dispatcher = null;
            }
            super.removeNotify();
        }
    }

    /**
     * Checks if the component is contained in the component hierarchy of
     * this container.
     * @param c the component
     * @return     {@code true} if it is an ancestor;
     *             {@code false} otherwise.
     * @since      1.1
     */
    public boolean isAncestorOf(Component c) {
        Container p;
        if (c == null || ((p = c.getParent()) == null)) {
            return false;
        }
        while (p != null) {
            if (p == this) {
                return true;
            }
            p = p.getParent();
        }
        return false;
    }

    /*
     * The following code was added to support modal JInternalFrames
     * Unfortunately this code has to be added here so that we can get access to
     * some private AWT classes like SequencedEvent.
     *
     * The native container of the LW component has this field set
     * to tell it that it should block Mouse events for all LW
     * children except for the modal component.
     *
     * In the case of nested Modal components, we store the previous
     * modal component in the new modal components value of modalComp;
     */

    transient Component modalComp;
    transient AppContext modalAppContext;

    private void startLWModal() {
        // Store the app context on which this component is being shown.
        // Event dispatch thread of this app context will be sleeping until
        // we wake it by any event from hideAndDisposeHandler().
        modalAppContext = AppContext.getAppContext();

        // keep the KeyEvents from being dispatched
        // until the focus has been transferred
        long time = Toolkit.getEventQueue().getMostRecentKeyEventTime();
        Component predictedFocusOwner = (Component.isInstanceOf(this, "javax.swing.JInternalFrame")) ? ((javax.swing.JInternalFrame)(this)).getMostRecentFocusOwner() : null;
        if (predictedFocusOwner != null) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().
                enqueueKeyEvents(time, predictedFocusOwner);
        }
        // We have two mechanisms for blocking: 1. If we're on the
        // EventDispatchThread, start a new event pump. 2. If we're
        // on any other thread, call wait() on the treelock.
        final Container nativeContainer;
        synchronized (getTreeLock()) {
            nativeContainer = getHeavyweightContainer();
            if (nativeContainer.modalComp != null) {
                this.modalComp =  nativeContainer.modalComp;
                nativeContainer.modalComp = this;
                return;
            }
            else {
                nativeContainer.modalComp = this;
            }
        }

        Runnable pumpEventsForHierarchy = () -> {
            EventDispatchThread dispatchThread = (EventDispatchThread)Thread.currentThread();
            dispatchThread.pumpEventsForHierarchy(() -> nativeContainer.modalComp != null,
                    Container.this);
        };

        if (EventQueue.isDispatchThread()) {
            SequencedEvent currentSequencedEvent =
                KeyboardFocusManager.getCurrentKeyboardFocusManager().
                getCurrentSequencedEvent();
            if (currentSequencedEvent != null) {
                currentSequencedEvent.dispose();
            }

            pumpEventsForHierarchy.run();
        } else {
            synchronized (getTreeLock()) {
                Toolkit.getEventQueue().
                    postEvent(new PeerEvent(this,
                                pumpEventsForHierarchy,
                                PeerEvent.PRIORITY_EVENT));
                while (nativeContainer.modalComp != null)
                {
                    try {
                        getTreeLock().wait();
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }
        if (predictedFocusOwner != null) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().
                dequeueKeyEvents(time, predictedFocusOwner);
        }
    }

    private void stopLWModal() {
        synchronized (getTreeLock()) {
            if (modalAppContext != null) {
                Container nativeContainer = getHeavyweightContainer();
                if(nativeContainer != null) {
                    if (this.modalComp !=  null) {
                        nativeContainer.modalComp = this.modalComp;
                        this.modalComp = null;
                        return;
                    }
                    else {
                        nativeContainer.modalComp = null;
                    }
                }
                // Wake up event dispatch thread on which the dialog was
                // initially shown
                SunToolkit.postEvent(modalAppContext,
                        new PeerEvent(this,
                                new WakingRunnable(),
                                PeerEvent.PRIORITY_EVENT));
            }
            EventQueue.invokeLater(new WakingRunnable());
            getTreeLock().notifyAll();
        }
    }

    static final class WakingRunnable implements Runnable {
        public void run() {
        }
    }

    /* End of JOptionPane support code */

    /**
     * Returns a string representing the state of this {@code Container}.
     * This method is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not be
     * {@code null}.
     *
     * @return    the parameter string of this container
     */
    protected String paramString() {
        String str = super.paramString();
        LayoutManager layoutMgr = this.layoutMgr;
        if (layoutMgr != null) {
            str += ",layout=" + layoutMgr.getClass().getName();
        }
        return str;
    }

    /**
     * Prints a listing of this container to the specified output
     * stream. The listing starts at the specified indentation.
     * <p>
     * The immediate children of the container are printed with
     * an indentation of {@code indent+1}.  The children
     * of those children are printed at {@code indent+2}
     * and so on.
     *
     * @param    out      a print stream
     * @param    indent   the number of spaces to indent
     * @throws   NullPointerException if {@code out} is {@code null}
     * @see      Component#list(java.io.PrintStream, int)
     * @since    1.0
     */
    public void list(PrintStream out, int indent) {
        super.list(out, indent);
        synchronized(getTreeLock()) {
            for (int i = 0; i < component.size(); i++) {
                Component comp = component.get(i);
                if (comp != null) {
                    comp.list(out, indent+1);
                }
            }
        }
    }

    /**
     * Prints out a list, starting at the specified indentation,
     * to the specified print writer.
     * <p>
     * The immediate children of the container are printed with
     * an indentation of {@code indent+1}.  The children
     * of those children are printed at {@code indent+2}
     * and so on.
     *
     * @param    out      a print writer
     * @param    indent   the number of spaces to indent
     * @throws   NullPointerException if {@code out} is {@code null}
     * @see      Component#list(java.io.PrintWriter, int)
     * @since    1.1
     */
    public void list(PrintWriter out, int indent) {
        super.list(out, indent);
        synchronized(getTreeLock()) {
            for (int i = 0; i < component.size(); i++) {
                Component comp = component.get(i);
                if (comp != null) {
                    comp.list(out, indent+1);
                }
            }
        }
    }

    /**
     * Sets the focus traversal keys for a given traversal operation for this
     * Container.
     * <p>
     * The default values for a Container's focus traversal keys are
     * implementation-dependent. Sun recommends that all implementations for a
     * particular native platform use the same default values. The
     * recommendations for Windows and Unix are listed below. These
     * recommendations are used in the Sun AWT implementations.
     *
     * <table class="striped">
     * <caption>Recommended default values for a Container's focus traversal
     * keys</caption>
     * <thead>
     *   <tr>
     *     <th scope="col">Identifier
     *     <th scope="col">Meaning
     *     <th scope="col">Default
     * </thead>
     * <tbody>
     *   <tr>
     *     <th scope="row">KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS
     *     <td>Normal forward keyboard traversal
     *     <td>TAB on KEY_PRESSED, CTRL-TAB on KEY_PRESSED
     *   <tr>
     *     <th scope="row">KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS
     *     <td>Normal reverse keyboard traversal
     *     <td>SHIFT-TAB on KEY_PRESSED, CTRL-SHIFT-TAB on KEY_PRESSED
     *   <tr>
     *     <th scope="row">KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS
     *     <td>Go up one focus traversal cycle
     *     <td>none
     *   <tr>
     *     <th scope="row">KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS
     *     <td>Go down one focus traversal cycle
     *     <td>none
     * </tbody>
     * </table>
     *
     * To disable a traversal key, use an empty Set; Collections.EMPTY_SET is
     * recommended.
     * <p>
     * Using the AWTKeyStroke API, client code can specify on which of two
     * specific KeyEvents, KEY_PRESSED or KEY_RELEASED, the focus traversal
     * operation will occur. Regardless of which KeyEvent is specified,
     * however, all KeyEvents related to the focus traversal key, including the
     * associated KEY_TYPED event, will be consumed, and will not be dispatched
     * to any Container. It is a runtime error to specify a KEY_TYPED event as
     * mapping to a focus traversal operation, or to map the same event to
     * multiple default focus traversal operations.
     * <p>
     * If a value of null is specified for the Set, this Container inherits the
     * Set from its parent. If all ancestors of this Container have null
     * specified for the Set, then the current KeyboardFocusManager's default
     * Set is used.
     * <p>
     * This method may throw a {@code ClassCastException} if any {@code Object}
     * in {@code keystrokes} is not an {@code AWTKeyStroke}.
     *
     * @param id one of KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
     *        KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
     *        KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS, or
     *        KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS
     * @param keystrokes the Set of AWTKeyStroke for the specified operation
     * @see #getFocusTraversalKeys
     * @see KeyboardFocusManager#FORWARD_TRAVERSAL_KEYS
     * @see KeyboardFocusManager#BACKWARD_TRAVERSAL_KEYS
     * @see KeyboardFocusManager#UP_CYCLE_TRAVERSAL_KEYS
     * @see KeyboardFocusManager#DOWN_CYCLE_TRAVERSAL_KEYS
     * @throws IllegalArgumentException if id is not one of
     *         KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
     *         KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
     *         KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS, or
     *         KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS, or if keystrokes
     *         contains null, or if any keystroke represents a KEY_TYPED event,
     *         or if any keystroke already maps to another focus traversal
     *         operation for this Container
     * @since 1.4
     */
    public void setFocusTraversalKeys(int id,
                                      Set<? extends AWTKeyStroke> keystrokes)
    {
        if (id < 0 || id >= KeyboardFocusManager.TRAVERSAL_KEY_LENGTH) {
            throw new IllegalArgumentException("invalid focus traversal key identifier");
        }

        // Don't call super.setFocusTraversalKey. The Component parameter check
        // does not allow DOWN_CYCLE_TRAVERSAL_KEYS, but we do.
        setFocusTraversalKeys_NoIDCheck(id, keystrokes);
    }

    /**
     * Returns the Set of focus traversal keys for a given traversal operation
     * for this Container. (See
     * {@code setFocusTraversalKeys} for a full description of each key.)
     * <p>
     * If a Set of traversal keys has not been explicitly defined for this
     * Container, then this Container's parent's Set is returned. If no Set
     * has been explicitly defined for any of this Container's ancestors, then
     * the current KeyboardFocusManager's default Set is returned.
     *
     * @param id one of KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
     *        KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
     *        KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS, or
     *        KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS
     * @return the Set of AWTKeyStrokes for the specified operation. The Set
     *         will be unmodifiable, and may be empty. null will never be
     *         returned.
     * @see #setFocusTraversalKeys
     * @see KeyboardFocusManager#FORWARD_TRAVERSAL_KEYS
     * @see KeyboardFocusManager#BACKWARD_TRAVERSAL_KEYS
     * @see KeyboardFocusManager#UP_CYCLE_TRAVERSAL_KEYS
     * @see KeyboardFocusManager#DOWN_CYCLE_TRAVERSAL_KEYS
     * @throws IllegalArgumentException if id is not one of
     *         KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
     *         KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
     *         KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS, or
     *         KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS
     * @since 1.4
     */
    public Set<AWTKeyStroke> getFocusTraversalKeys(int id) {
        if (id < 0 || id >= KeyboardFocusManager.TRAVERSAL_KEY_LENGTH) {
            throw new IllegalArgumentException("invalid focus traversal key identifier");
        }

        // Don't call super.getFocusTraversalKey. The Component parameter check
        // does not allow DOWN_CYCLE_TRAVERSAL_KEY, but we do.
        return getFocusTraversalKeys_NoIDCheck(id);
    }

    /**
     * Returns whether the Set of focus traversal keys for the given focus
     * traversal operation has been explicitly defined for this Container. If
     * this method returns {@code false}, this Container is inheriting the
     * Set from an ancestor, or from the current KeyboardFocusManager.
     *
     * @param id one of KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
     *        KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
     *        KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS, or
     *        KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS
     * @return {@code true} if the Set of focus traversal keys for the
     *         given focus traversal operation has been explicitly defined for
     *         this Component; {@code false} otherwise.
     * @throws IllegalArgumentException if id is not one of
     *         KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
     *        KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
     *        KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS, or
     *        KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS
     * @since 1.4
     */
    public boolean areFocusTraversalKeysSet(int id) {
        if (id < 0 || id >= KeyboardFocusManager.TRAVERSAL_KEY_LENGTH) {
            throw new IllegalArgumentException("invalid focus traversal key identifier");
        }

        return (focusTraversalKeys != null && focusTraversalKeys[id] != null);
    }

    /**
     * Returns whether the specified Container is the focus cycle root of this
     * Container's focus traversal cycle. Each focus traversal cycle has only
     * a single focus cycle root and each Container which is not a focus cycle
     * root belongs to only a single focus traversal cycle. Containers which
     * are focus cycle roots belong to two cycles: one rooted at the Container
     * itself, and one rooted at the Container's nearest focus-cycle-root
     * ancestor. This method will return {@code true} for both such
     * Containers in this case.
     *
     * @param container the Container to be tested
     * @return {@code true} if the specified Container is a focus-cycle-
     *         root of this Container; {@code false} otherwise
     * @see #isFocusCycleRoot()
     * @since 1.4
     */
    public boolean isFocusCycleRoot(Container container) {
        if (isFocusCycleRoot() && container == this) {
            return true;
        } else {
            return super.isFocusCycleRoot(container);
        }
    }

    private Container findTraversalRoot() {
        // I potentially have two roots, myself and my root parent
        // If I am the current root, then use me
        // If none of my parents are roots, then use me
        // If my root parent is the current root, then use my root parent
        // If neither I nor my root parent is the current root, then
        // use my root parent (a guess)

        Container currentFocusCycleRoot = KeyboardFocusManager.
            getCurrentKeyboardFocusManager().getCurrentFocusCycleRoot();
        Container root;

        if (currentFocusCycleRoot == this) {
            root = this;
        } else {
            root = getFocusCycleRootAncestor();
            if (root == null) {
                root = this;
            }
        }

        if (root != currentFocusCycleRoot) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().
                setGlobalCurrentFocusCycleRootPriv(root);
        }
        return root;
    }

    final boolean containsFocus() {
        final Component focusOwner = KeyboardFocusManager.
            getCurrentKeyboardFocusManager().getFocusOwner();
        return isParentOf(focusOwner);
    }

    /**
     * Check if this component is the child of this container or its children.
     * Note: this function acquires treeLock
     * Note: this function traverses children tree only in one Window.
     * @param comp a component in test, must not be null
     */
    private boolean isParentOf(Component comp) {
        synchronized(getTreeLock()) {
            while (comp != null && comp != this && !(comp instanceof Window)) {
                comp = comp.getParent();
            }
            return (comp == this);
        }
    }

    void clearMostRecentFocusOwnerOnHide() {
        boolean reset = false;
        Window window = null;

        synchronized (getTreeLock()) {
            window = getContainingWindow();
            if (window != null) {
                Component comp = KeyboardFocusManager.getMostRecentFocusOwner(window);
                reset = ((comp == this) || isParentOf(comp));
                // This synchronized should always be the second in a pair
                // (tree lock, KeyboardFocusManager.class)
                synchronized(KeyboardFocusManager.class) {
                    Component storedComp = window.getTemporaryLostComponent();
                    if (isParentOf(storedComp) || storedComp == this) {
                        window.setTemporaryLostComponent(null);
                    }
                }
            }
        }

        if (reset) {
            KeyboardFocusManager.setMostRecentFocusOwner(window, null);
        }
    }

    void clearCurrentFocusCycleRootOnHide() {
        KeyboardFocusManager kfm =
            KeyboardFocusManager.getCurrentKeyboardFocusManager();
        Container cont = kfm.getCurrentFocusCycleRoot();

        if (cont == this || isParentOf(cont)) {
            kfm.setGlobalCurrentFocusCycleRootPriv(null);
        }
    }

    final Container getTraversalRoot() {
        if (isFocusCycleRoot()) {
            return findTraversalRoot();
        }

        return super.getTraversalRoot();
    }

    /**
     * Sets the focus traversal policy that will manage keyboard traversal of
     * this Container's children, if this Container is a focus cycle root. If
     * the argument is null, this Container inherits its policy from its focus-
     * cycle-root ancestor. If the argument is non-null, this policy will be
     * inherited by all focus-cycle-root children that have no keyboard-
     * traversal policy of their own (as will, recursively, their focus-cycle-
     * root children).
     * <p>
     * If this Container is not a focus cycle root, the policy will be
     * remembered, but will not be used or inherited by this or any other
     * Containers until this Container is made a focus cycle root.
     *
     * @param policy the new focus traversal policy for this Container
     * @see #getFocusTraversalPolicy
     * @see #setFocusCycleRoot
     * @see #isFocusCycleRoot
     * @since 1.4
     */
    public void setFocusTraversalPolicy(FocusTraversalPolicy policy) {
        FocusTraversalPolicy oldPolicy;
        synchronized (this) {
            oldPolicy = this.focusTraversalPolicy;
            this.focusTraversalPolicy = policy;
        }
        firePropertyChange("focusTraversalPolicy", oldPolicy, policy);
    }

    /**
     * Returns the focus traversal policy that will manage keyboard traversal
     * of this Container's children, or null if this Container is not a focus
     * cycle root. If no traversal policy has been explicitly set for this
     * Container, then this Container's focus-cycle-root ancestor's policy is
     * returned.
     *
     * @return this Container's focus traversal policy, or null if this
     *         Container is not a focus cycle root.
     * @see #setFocusTraversalPolicy
     * @see #setFocusCycleRoot
     * @see #isFocusCycleRoot
     * @since 1.4
     */
    public FocusTraversalPolicy getFocusTraversalPolicy() {
        if (!isFocusTraversalPolicyProvider() && !isFocusCycleRoot()) {
            return null;
        }

        FocusTraversalPolicy policy = this.focusTraversalPolicy;
        if (policy != null) {
            return policy;
        }

        Container rootAncestor = getFocusCycleRootAncestor();
        if (rootAncestor != null) {
            return rootAncestor.getFocusTraversalPolicy();
        } else {
            return KeyboardFocusManager.getCurrentKeyboardFocusManager().
                getDefaultFocusTraversalPolicy();
        }
    }

    /**
     * Returns whether the focus traversal policy has been explicitly set for
     * this Container. If this method returns {@code false}, this
     * Container will inherit its focus traversal policy from an ancestor.
     *
     * @return {@code true} if the focus traversal policy has been
     *         explicitly set for this Container; {@code false} otherwise.
     * @since 1.4
     */
    public boolean isFocusTraversalPolicySet() {
        return (focusTraversalPolicy != null);
    }

    /**
     * Sets whether this Container is the root of a focus traversal cycle. Once
     * focus enters a traversal cycle, typically it cannot leave it via focus
     * traversal unless one of the up- or down-cycle keys is pressed. Normal
     * traversal is limited to this Container, and all of this Container's
     * descendants that are not descendants of inferior focus cycle roots. Note
     * that a FocusTraversalPolicy may bend these restrictions, however. For
     * example, ContainerOrderFocusTraversalPolicy supports implicit down-cycle
     * traversal.
     * <p>
     * The alternative way to specify the traversal order of this Container's
     * children is to make this Container a
     * <a href="doc-files/FocusSpec.html#FocusTraversalPolicyProviders">focus traversal policy provider</a>.
     *
     * @param focusCycleRoot indicates whether this Container is the root of a
     *        focus traversal cycle
     * @see #isFocusCycleRoot()
     * @see #setFocusTraversalPolicy
     * @see #getFocusTraversalPolicy
     * @see ContainerOrderFocusTraversalPolicy
     * @see #setFocusTraversalPolicyProvider
     * @since 1.4
     */
    public void setFocusCycleRoot(boolean focusCycleRoot) {
        boolean oldFocusCycleRoot;
        synchronized (this) {
            oldFocusCycleRoot = this.focusCycleRoot;
            this.focusCycleRoot = focusCycleRoot;
        }
        firePropertyChange("focusCycleRoot", oldFocusCycleRoot,
                           focusCycleRoot);
    }

    /**
     * Returns whether this Container is the root of a focus traversal cycle.
     * Once focus enters a traversal cycle, typically it cannot leave it via
     * focus traversal unless one of the up- or down-cycle keys is pressed.
     * Normal traversal is limited to this Container, and all of this
     * Container's descendants that are not descendants of inferior focus
     * cycle roots. Note that a FocusTraversalPolicy may bend these
     * restrictions, however. For example, ContainerOrderFocusTraversalPolicy
     * supports implicit down-cycle traversal.
     *
     * @return whether this Container is the root of a focus traversal cycle
     * @see #setFocusCycleRoot
     * @see #setFocusTraversalPolicy
     * @see #getFocusTraversalPolicy
     * @see ContainerOrderFocusTraversalPolicy
     * @since 1.4
     */
    public boolean isFocusCycleRoot() {
        return focusCycleRoot;
    }

    /**
     * Sets whether this container will be used to provide focus
     * traversal policy. Container with this property as
     * {@code true} will be used to acquire focus traversal policy
     * instead of closest focus cycle root ancestor.
     * @param provider indicates whether this container will be used to
     *                provide focus traversal policy
     * @see #setFocusTraversalPolicy
     * @see #getFocusTraversalPolicy
     * @see #isFocusTraversalPolicyProvider
     * @since 1.5
     */
    public final void setFocusTraversalPolicyProvider(boolean provider) {
        boolean oldProvider;
        synchronized(this) {
            oldProvider = focusTraversalPolicyProvider;
            focusTraversalPolicyProvider = provider;
        }
        firePropertyChange("focusTraversalPolicyProvider", oldProvider, provider);
    }

    /**
     * Returns whether this container provides focus traversal
     * policy. If this property is set to {@code true} then when
     * keyboard focus manager searches container hierarchy for focus
     * traversal policy and encounters this container before any other
     * container with this property as true or focus cycle roots then
     * its focus traversal policy will be used instead of focus cycle
     * root's policy.
     * @see #setFocusTraversalPolicy
     * @see #getFocusTraversalPolicy
     * @see #setFocusCycleRoot
     * @see #setFocusTraversalPolicyProvider
     * @return {@code true} if this container provides focus traversal
     *         policy, {@code false} otherwise
     * @since 1.5
     */
    public final boolean isFocusTraversalPolicyProvider() {
        return focusTraversalPolicyProvider;
    }

    /**
     * Transfers the focus down one focus traversal cycle. If this Container is
     * a focus cycle root, then the focus owner is set to this Container's
     * default Component to focus, and the current focus cycle root is set to
     * this Container. If this Container is not a focus cycle root, then no
     * focus traversal operation occurs.
     *
     * @see       Component#requestFocus()
     * @see       #isFocusCycleRoot
     * @see       #setFocusCycleRoot
     * @since     1.4
     */
    public void transferFocusDownCycle() {
        if (isFocusCycleRoot()) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().
                setGlobalCurrentFocusCycleRootPriv(this);
            Component toFocus = getFocusTraversalPolicy().
                getDefaultComponent(this);
            if (toFocus != null) {
                toFocus.requestFocus(FocusEvent.Cause.TRAVERSAL_DOWN);
            }
        }
    }

    void preProcessKeyEvent(KeyEvent e) {
        Container parent = this.parent;
        if (parent != null) {
            parent.preProcessKeyEvent(e);
        }
    }

    void postProcessKeyEvent(KeyEvent e) {
        Container parent = this.parent;
        if (parent != null) {
            parent.postProcessKeyEvent(e);
        }
    }

    boolean postsOldMouseEvents() {
        return true;
    }

    /**
     * Sets the {@code ComponentOrientation} property of this container
     * and all components contained within it.
     * <p>
     * This method changes layout-related information, and therefore,
     * invalidates the component hierarchy.
     *
     * @param o the new component orientation of this container and
     *        the components contained within it.
     * @exception NullPointerException if {@code orientation} is null.
     * @see Component#setComponentOrientation
     * @see Component#getComponentOrientation
     * @see #invalidate
     * @since 1.4
     */
    public void applyComponentOrientation(ComponentOrientation o) {
        super.applyComponentOrientation(o);
        synchronized (getTreeLock()) {
            for (int i = 0; i < component.size(); i++) {
                Component comp = component.get(i);
                comp.applyComponentOrientation(o);
            }
        }
    }

    /**
     * Adds a PropertyChangeListener to the listener list. The listener is
     * registered for all bound properties of this class, including the
     * following:
     * <ul>
     *    <li>this Container's font ("font")</li>
     *    <li>this Container's background color ("background")</li>
     *    <li>this Container's foreground color ("foreground")</li>
     *    <li>this Container's focusability ("focusable")</li>
     *    <li>this Container's focus traversal keys enabled state
     *        ("focusTraversalKeysEnabled")</li>
     *    <li>this Container's Set of FORWARD_TRAVERSAL_KEYS
     *        ("forwardFocusTraversalKeys")</li>
     *    <li>this Container's Set of BACKWARD_TRAVERSAL_KEYS
     *        ("backwardFocusTraversalKeys")</li>
     *    <li>this Container's Set of UP_CYCLE_TRAVERSAL_KEYS
     *        ("upCycleFocusTraversalKeys")</li>
     *    <li>this Container's Set of DOWN_CYCLE_TRAVERSAL_KEYS
     *        ("downCycleFocusTraversalKeys")</li>
     *    <li>this Container's focus traversal policy ("focusTraversalPolicy")
     *        </li>
     *    <li>this Container's focus-cycle-root state ("focusCycleRoot")</li>
     * </ul>
     * Note that if this Container is inheriting a bound property, then no
     * event will be fired in response to a change in the inherited property.
     * <p>
     * If listener is null, no exception is thrown and no action is performed.
     *
     * @param    listener  the PropertyChangeListener to be added
     *
     * @see Component#removePropertyChangeListener
     * @see #addPropertyChangeListener(java.lang.String,java.beans.PropertyChangeListener)
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        super.addPropertyChangeListener(listener);
    }

    /**
     * Adds a PropertyChangeListener to the listener list for a specific
     * property. The specified property may be user-defined, or one of the
     * following defaults:
     * <ul>
     *    <li>this Container's font ("font")</li>
     *    <li>this Container's background color ("background")</li>
     *    <li>this Container's foreground color ("foreground")</li>
     *    <li>this Container's focusability ("focusable")</li>
     *    <li>this Container's focus traversal keys enabled state
     *        ("focusTraversalKeysEnabled")</li>
     *    <li>this Container's Set of FORWARD_TRAVERSAL_KEYS
     *        ("forwardFocusTraversalKeys")</li>
     *    <li>this Container's Set of BACKWARD_TRAVERSAL_KEYS
     *        ("backwardFocusTraversalKeys")</li>
     *    <li>this Container's Set of UP_CYCLE_TRAVERSAL_KEYS
     *        ("upCycleFocusTraversalKeys")</li>
     *    <li>this Container's Set of DOWN_CYCLE_TRAVERSAL_KEYS
     *        ("downCycleFocusTraversalKeys")</li>
     *    <li>this Container's focus traversal policy ("focusTraversalPolicy")
     *        </li>
     *    <li>this Container's focus-cycle-root state ("focusCycleRoot")</li>
     *    <li>this Container's focus-traversal-policy-provider state("focusTraversalPolicyProvider")</li>
     *    <li>this Container's focus-traversal-policy-provider state("focusTraversalPolicyProvider")</li>
     * </ul>
     * Note that if this Container is inheriting a bound property, then no
     * event will be fired in response to a change in the inherited property.
     * <p>
     * If listener is null, no exception is thrown and no action is performed.
     *
     * @param propertyName one of the property names listed above
     * @param listener the PropertyChangeListener to be added
     *
     * @see #addPropertyChangeListener(java.beans.PropertyChangeListener)
     * @see Component#removePropertyChangeListener
     */
    public void addPropertyChangeListener(String propertyName,
                                          PropertyChangeListener listener) {
        super.addPropertyChangeListener(propertyName, listener);
    }

    // Serialization support. A Container is responsible for restoring the
    // parent fields of its component children.

    /**
     * Container Serial Data Version.
     */
    private int containerSerializedDataVersion = 1;

    /**
     * Serializes this {@code Container} to the specified
     * {@code ObjectOutputStream}.
     * <ul>
     *    <li>Writes default serializable fields to the stream.</li>
     *    <li>Writes a list of serializable ContainerListener(s) as optional
     *        data. The non-serializable ContainerListener(s) are detected and
     *        no attempt is made to serialize them.</li>
     *    <li>Write this Container's FocusTraversalPolicy if and only if it
     *        is Serializable; otherwise, {@code null} is written.</li>
     * </ul>
     *
     * @param  s the {@code ObjectOutputStream} to write
     * @throws IOException if an I/O error occurs
     * @serialData {@code null} terminated sequence of 0 or more pairs;
     *   the pair consists of a {@code String} and {@code Object};
     *   the {@code String} indicates the type of object and
     *   is one of the following:
     *   {@code containerListenerK} indicating an
     *     {@code ContainerListener} object;
     *   the {@code Container}'s {@code FocusTraversalPolicy},
     *     or {@code null}
     *
     * @see AWTEventMulticaster#save(java.io.ObjectOutputStream, java.lang.String, java.util.EventListener)
     * @see Container#containerListenerK
     * @see #readObject(ObjectInputStream)
     */
    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        ObjectOutputStream.PutField f = s.putFields();
        f.put("ncomponents", component.size());
        f.put("component", component.toArray(EMPTY_ARRAY));
        f.put("layoutMgr", layoutMgr);
        f.put("dispatcher", dispatcher);
        f.put("maxSize", maxSize);
        f.put("focusCycleRoot", focusCycleRoot);
        f.put("containerSerializedDataVersion", containerSerializedDataVersion);
        f.put("focusTraversalPolicyProvider", focusTraversalPolicyProvider);
        s.writeFields();

        AWTEventMulticaster.save(s, containerListenerK, containerListener);
        s.writeObject(null);

        if (focusTraversalPolicy instanceof java.io.Serializable) {
            s.writeObject(focusTraversalPolicy);
        } else {
            s.writeObject(null);
        }
    }

    /**
     * Deserializes this {@code Container} from the specified
     * {@code ObjectInputStream}.
     * <ul>
     *    <li>Reads default serializable fields from the stream.</li>
     *    <li>Reads a list of serializable ContainerListener(s) as optional
     *        data. If the list is null, no Listeners are installed.</li>
     *    <li>Reads this Container's FocusTraversalPolicy, which may be null,
     *        as optional data.</li>
     * </ul>
     *
     * @param  s the {@code ObjectInputStream} to read
     * @throws ClassNotFoundException if the class of a serialized object could
     *         not be found
     * @throws IOException if an I/O error occurs
     * @serial
     * @see #addContainerListener
     * @see #writeObject(ObjectOutputStream)
     */
    @Serial
    private void readObject(ObjectInputStream s)
        throws ClassNotFoundException, IOException
    {
        ObjectInputStream.GetField f = s.readFields();
        // array of components may not be present in the stream or may be null
        Component [] tmpComponent = (Component[])f.get("component", null);
        if (tmpComponent == null) {
            tmpComponent = EMPTY_ARRAY;
        }
        int ncomponents = (Integer) f.get("ncomponents", 0);
        if (ncomponents < 0 || ncomponents > tmpComponent.length) {
            throw new InvalidObjectException("Incorrect number of components");
        }
        component = new java.util.ArrayList<Component>(ncomponents);
        for (int i = 0; i < ncomponents; ++i) {
            component.add(tmpComponent[i]);
        }
        layoutMgr = (LayoutManager)f.get("layoutMgr", null);
        dispatcher = (LightweightDispatcher)f.get("dispatcher", null);
        // Old stream. Doesn't contain maxSize among Component's fields.
        if (maxSize == null) {
            maxSize = (Dimension)f.get("maxSize", null);
        }
        focusCycleRoot = f.get("focusCycleRoot", false);
        containerSerializedDataVersion = f.get("containerSerializedDataVersion", 1);
        focusTraversalPolicyProvider = f.get("focusTraversalPolicyProvider", false);
        java.util.List<Component> component = this.component;
        for(Component comp : component) {
            comp.parent = this;
            adjustListeningChildren(AWTEvent.HIERARCHY_EVENT_MASK,
                                    comp.numListening(AWTEvent.HIERARCHY_EVENT_MASK));
            adjustListeningChildren(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK,
                                    comp.numListening(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK));
            adjustDescendants(comp.countHierarchyMembers());
        }

        Object keyOrNull;
        while(null != (keyOrNull = s.readObject())) {
            String key = ((String)keyOrNull).intern();

            if (containerListenerK == key) {
                addContainerListener((ContainerListener)(s.readObject()));
            } else {
                // skip value for unrecognized key
                s.readObject();
            }
        }

        try {
            Object policy = s.readObject();
            if (policy instanceof FocusTraversalPolicy) {
                focusTraversalPolicy = (FocusTraversalPolicy)policy;
            }
        } catch (java.io.OptionalDataException e) {
            // JDK 1.1/1.2/1.3 instances will not have this optional data.
            // e.eof will be true to indicate that there is no more data
            // available for this object. If e.eof is not true, throw the
            // exception as it might have been caused by reasons unrelated to
            // focusTraversalPolicy.

            if (!e.eof) {
                throw e;
            }
        }
    }

    /*
     * --- Accessibility Support ---
     */

    /**
     * Inner class of Container used to provide default support for
     * accessibility.  This class is not meant to be used directly by
     * application developers, but is instead meant only to be
     * subclassed by container developers.
     * <p>
     * The class used to obtain the accessible role for this object,
     * as well as implementing many of the methods in the
     * AccessibleContainer interface.
     * @since 1.3
     */
    protected class AccessibleAWTContainer extends AccessibleAWTComponent {

        /**
         * Use serialVersionUID from JDK 1.3 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = 5081320404842566097L;

        /**
         * Constructs an {@code AccessibleAWTContainer}.
         */
        protected AccessibleAWTContainer() {}

        /**
         * Returns the number of accessible children in the object.  If all
         * of the children of this object implement {@code Accessible},
         * then this method should return the number of children of this object.
         *
         * @return the number of accessible children in the object
         */
        public int getAccessibleChildrenCount() {
            return Container.this.getAccessibleChildrenCount();
        }

        /**
         * Returns the nth {@code Accessible} child of the object.
         *
         * @param i zero-based index of child
         * @return the nth {@code Accessible} child of the object
         */
        public Accessible getAccessibleChild(int i) {
            return Container.this.getAccessibleChild(i);
        }

        /**
         * Returns the {@code Accessible} child, if one exists,
         * contained at the local coordinate {@code Point}.
         *
         * @param p the point defining the top-left corner of the
         *    {@code Accessible}, given in the coordinate space
         *    of the object's parent
         * @return the {@code Accessible}, if it exists,
         *    at the specified location; else {@code null}
         */
        public Accessible getAccessibleAt(Point p) {
            return Container.this.getAccessibleAt(p);
        }

        /**
         * Number of PropertyChangeListener objects registered. It's used
         * to add/remove ContainerListener to track target Container's state.
         */
        private transient volatile int propertyListenersCount = 0;

        /**
         * The handler to fire {@code PropertyChange}
         * when children are added or removed
         */
        @SuppressWarnings("serial") // Not statically typed as Serializable
        protected ContainerListener accessibleContainerHandler = null;

        /**
         * Fire {@code PropertyChange} listener, if one is registered,
         * when children are added or removed.
         * @since 1.3
         */
        protected class AccessibleContainerHandler
            implements ContainerListener, Serializable {

            /**
             * Use serialVersionUID from JDK 1.3 for interoperability.
             */
            @Serial
            private static final long serialVersionUID = -480855353991814677L;

            /**
             * Constructs an {@code AccessibleContainerHandler}.
             */
            protected AccessibleContainerHandler() {}

            public void componentAdded(ContainerEvent e) {
                Component c = e.getChild();
                if (c != null && c instanceof Accessible) {
                    AccessibleAWTContainer.this.firePropertyChange(
                        AccessibleContext.ACCESSIBLE_CHILD_PROPERTY,
                        null, ((Accessible) c).getAccessibleContext());
                }
            }
            public void componentRemoved(ContainerEvent e) {
                Component c = e.getChild();
                if (c != null && c instanceof Accessible) {
                    AccessibleAWTContainer.this.firePropertyChange(
                        AccessibleContext.ACCESSIBLE_CHILD_PROPERTY,
                        ((Accessible) c).getAccessibleContext(), null);
                }
            }
        }

        /**
         * Adds a PropertyChangeListener to the listener list.
         *
         * @param listener  the PropertyChangeListener to be added
         */
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            if (accessibleContainerHandler == null) {
                accessibleContainerHandler = new AccessibleContainerHandler();
            }
            if (propertyListenersCount++ == 0) {
                Container.this.addContainerListener(accessibleContainerHandler);
            }
            super.addPropertyChangeListener(listener);
        }

        /**
         * Remove a PropertyChangeListener from the listener list.
         * This removes a PropertyChangeListener that was registered
         * for all properties.
         *
         * @param listener the PropertyChangeListener to be removed
         */
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            if (--propertyListenersCount == 0) {
                Container.this.removeContainerListener(accessibleContainerHandler);
            }
            super.removePropertyChangeListener(listener);
        }

    } // inner class AccessibleAWTContainer

    /**
     * Returns the {@code Accessible} child contained at the local
     * coordinate {@code Point}, if one exists.  Otherwise
     * returns {@code null}.
     *
     * @param p the point defining the top-left corner of the
     *    {@code Accessible}, given in the coordinate space
     *    of the object's parent
     * @return the {@code Accessible} at the specified location,
     *    if it exists; otherwise {@code null}
     */
    Accessible getAccessibleAt(Point p) {
        synchronized (getTreeLock()) {
            if (this instanceof Accessible) {
                Accessible a = (Accessible)this;
                AccessibleContext ac = a.getAccessibleContext();
                if (ac != null) {
                    AccessibleComponent acmp;
                    Point location;
                    int nchildren = ac.getAccessibleChildrenCount();
                    for (int i=0; i < nchildren; i++) {
                        a = ac.getAccessibleChild(i);
                        if ((a != null)) {
                            ac = a.getAccessibleContext();
                            if (ac != null) {
                                acmp = ac.getAccessibleComponent();
                                if ((acmp != null) && (acmp.isShowing())) {
                                    location = acmp.getLocation();
                                    Point np = new Point(p.x-location.x,
                                                         p.y-location.y);
                                    if (acmp.contains(np)){
                                        return a;
                                    }
                                }
                            }
                        }
                    }
                }
                return (Accessible)this;
            } else {
                Component ret = this;
                if (!this.contains(p.x,p.y)) {
                    ret = null;
                } else {
                    int ncomponents = this.getComponentCount();
                    for (int i=0; i < ncomponents; i++) {
                        Component comp = this.getComponent(i);
                        if ((comp != null) && comp.isShowing()) {
                            Point location = comp.getLocation();
                            if (comp.contains(p.x-location.x,p.y-location.y)) {
                                ret = comp;
                            }
                        }
                    }
                }
                if (ret instanceof Accessible) {
                    return (Accessible) ret;
                }
            }
            return null;
        }
    }

    /**
     * Returns the number of accessible children in the object.  If all
     * of the children of this object implement {@code Accessible},
     * then this method should return the number of children of this object.
     *
     * @return the number of accessible children in the object
     */
    int getAccessibleChildrenCount() {
        synchronized (getTreeLock()) {
            int count = 0;
            Component[] children = this.getComponents();
            for (int i = 0; i < children.length; i++) {
                if (children[i] instanceof Accessible) {
                    count++;
                }
            }
            return count;
        }
    }

    /**
     * Returns the nth {@code Accessible} child of the object.
     *
     * @param i zero-based index of child
     * @return the nth {@code Accessible} child of the object
     */
    Accessible getAccessibleChild(int i) {
        synchronized (getTreeLock()) {
            Component[] children = this.getComponents();
            int count = 0;
            for (int j = 0; j < children.length; j++) {
                if (children[j] instanceof Accessible) {
                    if (count == i) {
                        return (Accessible) children[j];
                    } else {
                        count++;
                    }
                }
            }
            return null;
        }
    }

    // ************************** MIXING CODE *******************************

    final void increaseComponentCount(Component c) {
        synchronized (getTreeLock()) {
            if (!c.isDisplayable()) {
                throw new IllegalStateException(
                    "Peer does not exist while invoking the increaseComponentCount() method"
                );
            }

            int addHW = 0;
            int addLW = 0;

            if (c instanceof Container) {
                addLW = ((Container)c).numOfLWComponents;
                addHW = ((Container)c).numOfHWComponents;
            }
            if (c.isLightweight()) {
                addLW++;
            } else {
                addHW++;
            }

            for (Container cont = this; cont != null; cont = cont.getContainer()) {
                cont.numOfLWComponents += addLW;
                cont.numOfHWComponents += addHW;
            }
        }
    }

    final void decreaseComponentCount(Component c) {
        synchronized (getTreeLock()) {
            if (!c.isDisplayable()) {
                throw new IllegalStateException(
                    "Peer does not exist while invoking the decreaseComponentCount() method"
                );
            }

            int subHW = 0;
            int subLW = 0;

            if (c instanceof Container) {
                subLW = ((Container)c).numOfLWComponents;
                subHW = ((Container)c).numOfHWComponents;
            }
            if (c.isLightweight()) {
                subLW++;
            } else {
                subHW++;
            }

            for (Container cont = this; cont != null; cont = cont.getContainer()) {
                cont.numOfLWComponents -= subLW;
                cont.numOfHWComponents -= subHW;
            }
        }
    }

    private int getTopmostComponentIndex() {
        checkTreeLock();
        if (getComponentCount() > 0) {
            return 0;
        }
        return -1;
    }

    private int getBottommostComponentIndex() {
        checkTreeLock();
        if (getComponentCount() > 0) {
            return getComponentCount() - 1;
        }
        return -1;
    }

    /*
     * This method is overriden to handle opaque children in non-opaque
     * containers.
     */
    @Override
    final Region getOpaqueShape() {
        checkTreeLock();
        if (isLightweight() && isNonOpaqueForMixing()
                && hasLightweightDescendants())
        {
            Region s = Region.EMPTY_REGION;
            for (int index = 0; index < getComponentCount(); index++) {
                Component c = getComponent(index);
                if (c.isLightweight() && c.isShowing()) {
                    s = s.getUnion(c.getOpaqueShape());
                }
            }
            return s.getIntersection(getNormalShape());
        }
        return super.getOpaqueShape();
    }

    final void recursiveSubtractAndApplyShape(Region shape) {
        recursiveSubtractAndApplyShape(shape, getTopmostComponentIndex(), getBottommostComponentIndex());
    }

    final void recursiveSubtractAndApplyShape(Region shape, int fromZorder) {
        recursiveSubtractAndApplyShape(shape, fromZorder, getBottommostComponentIndex());
    }

    final void recursiveSubtractAndApplyShape(Region shape, int fromZorder, int toZorder) {
        checkTreeLock();
        if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
            mixingLog.fine("this = " + this +
                "; shape=" + shape + "; fromZ=" + fromZorder + "; toZ=" + toZorder);
        }
        if (fromZorder == -1) {
            return;
        }
        if (shape.isEmpty()) {
            return;
        }
        // An invalid container with not-null layout should be ignored
        // by the mixing code, the container will be validated later
        // and the mixing code will be executed later.
        if (getLayout() != null && !isValid()) {
            return;
        }
        for (int index = fromZorder; index <= toZorder; index++) {
            Component comp = getComponent(index);
            if (!comp.isLightweight()) {
                comp.subtractAndApplyShape(shape);
            } else if (comp instanceof Container &&
                    ((Container)comp).hasHeavyweightDescendants() && comp.isShowing()) {
                ((Container)comp).recursiveSubtractAndApplyShape(shape);
            }
        }
    }

    final void recursiveApplyCurrentShape() {
        recursiveApplyCurrentShape(getTopmostComponentIndex(), getBottommostComponentIndex());
    }

    final void recursiveApplyCurrentShape(int fromZorder) {
        recursiveApplyCurrentShape(fromZorder, getBottommostComponentIndex());
    }

    final void recursiveApplyCurrentShape(int fromZorder, int toZorder) {
        checkTreeLock();
        if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
            mixingLog.fine("this = " + this +
                "; fromZ=" + fromZorder + "; toZ=" + toZorder);
        }
        if (fromZorder == -1) {
            return;
        }
        // An invalid container with not-null layout should be ignored
        // by the mixing code, the container will be validated later
        // and the mixing code will be executed later.
        if (getLayout() != null && !isValid()) {
            return;
        }
        for (int index = fromZorder; index <= toZorder; index++) {
            Component comp = getComponent(index);
            if (!comp.isLightweight()) {
                comp.applyCurrentShape();
            }
            if (comp instanceof Container &&
                    ((Container)comp).hasHeavyweightDescendants()) {
                ((Container)comp).recursiveApplyCurrentShape();
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void recursiveShowHeavyweightChildren() {
        if (!hasHeavyweightDescendants() || !isVisible()) {
            return;
        }
        for (int index = 0; index < getComponentCount(); index++) {
            Component comp = getComponent(index);
            if (comp.isLightweight()) {
                if  (comp instanceof Container) {
                    ((Container)comp).recursiveShowHeavyweightChildren();
                }
            } else {
                if (comp.isVisible()) {
                    ComponentPeer peer = comp.peer;
                    if (peer != null) {
                        peer.setVisible(true);
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void recursiveHideHeavyweightChildren() {
        if (!hasHeavyweightDescendants()) {
            return;
        }
        for (int index = 0; index < getComponentCount(); index++) {
            Component comp = getComponent(index);
            if (comp.isLightweight()) {
                if  (comp instanceof Container) {
                    ((Container)comp).recursiveHideHeavyweightChildren();
                }
            } else {
                if (comp.isVisible()) {
                    ComponentPeer peer = comp.peer;
                    if (peer != null) {
                        peer.setVisible(false);
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void recursiveRelocateHeavyweightChildren(Point origin) {
        for (int index = 0; index < getComponentCount(); index++) {
            Component comp = getComponent(index);
            if (comp.isLightweight()) {
                if  (comp instanceof Container &&
                        ((Container)comp).hasHeavyweightDescendants())
                {
                    final Point newOrigin = new Point(origin);
                    newOrigin.translate(comp.getX(), comp.getY());
                    ((Container)comp).recursiveRelocateHeavyweightChildren(newOrigin);
                }
            } else {
                ComponentPeer peer = comp.peer;
                if (peer != null) {
                    peer.setBounds(origin.x + comp.getX(), origin.y + comp.getY(),
                            comp.getWidth(), comp.getHeight(),
                            ComponentPeer.SET_LOCATION);
                }
            }
        }
    }

    /**
     * Checks if the container and its direct lightweight containers are
     * visible.
     *
     * Consider the heavyweight container hides or shows the HW descendants
     * automatically. Therefore we care of LW containers' visibility only.
     *
     * This method MUST be invoked under the TreeLock.
     */
    final boolean isRecursivelyVisibleUpToHeavyweightContainer() {
        if (!isLightweight()) {
            return true;
        }

        for (Container cont = this;
                cont != null && cont.isLightweight();
                cont = cont.getContainer())
        {
            if (!cont.isVisible()) {
                return false;
            }
        }
        return true;
    }

    @Override
    void mixOnShowing() {
        synchronized (getTreeLock()) {
            if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
                mixingLog.fine("this = " + this);
            }

            boolean isLightweight = isLightweight();

            if (isLightweight && isRecursivelyVisibleUpToHeavyweightContainer()) {
                recursiveShowHeavyweightChildren();
            }

            if (!isMixingNeeded()) {
                return;
            }

            if (!isLightweight || (isLightweight && hasHeavyweightDescendants())) {
                recursiveApplyCurrentShape();
            }

            super.mixOnShowing();
        }
    }

    @Override
    void mixOnHiding(boolean isLightweight) {
        synchronized (getTreeLock()) {
            if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
                mixingLog.fine("this = " + this +
                        "; isLightweight=" + isLightweight);
            }
            if (isLightweight) {
                recursiveHideHeavyweightChildren();
            }
            super.mixOnHiding(isLightweight);
        }
    }

    @Override
    void mixOnReshaping() {
        synchronized (getTreeLock()) {
            if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
                mixingLog.fine("this = " + this);
            }

            boolean isMixingNeeded = isMixingNeeded();

            if (isLightweight() && hasHeavyweightDescendants()) {
                final Point origin = new Point(getX(), getY());
                for (Container cont = getContainer();
                        cont != null && cont.isLightweight();
                        cont = cont.getContainer())
                {
                    origin.translate(cont.getX(), cont.getY());
                }

                recursiveRelocateHeavyweightChildren(origin);

                if (!isMixingNeeded) {
                    return;
                }

                recursiveApplyCurrentShape();
            }

            if (!isMixingNeeded) {
                return;
            }

            super.mixOnReshaping();
        }
    }

    @Override
    void mixOnZOrderChanging(int oldZorder, int newZorder) {
        synchronized (getTreeLock()) {
            if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
                mixingLog.fine("this = " + this +
                    "; oldZ=" + oldZorder + "; newZ=" + newZorder);
            }

            if (!isMixingNeeded()) {
                return;
            }

            boolean becameHigher = newZorder < oldZorder;

            if (becameHigher && isLightweight() && hasHeavyweightDescendants()) {
                recursiveApplyCurrentShape();
            }
            super.mixOnZOrderChanging(oldZorder, newZorder);
        }
    }

    @Override
    void mixOnValidating() {
        synchronized (getTreeLock()) {
            if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
                mixingLog.fine("this = " + this);
            }

            if (!isMixingNeeded()) {
                return;
            }

            if (hasHeavyweightDescendants()) {
                recursiveApplyCurrentShape();
            }

            if (isLightweight() && isNonOpaqueForMixing()) {
                subtractAndApplyShapeBelowMe();
            }

            super.mixOnValidating();
        }
    }

    // ****************** END OF MIXING CODE ********************************
}


/**
 * Class to manage the dispatching of MouseEvents to the lightweight descendants
 * and SunDropTargetEvents to both lightweight and heavyweight descendants
 * contained by a native container.
 *
 * NOTE: the class name is not appropriate anymore, but we cannot change it
 * because we must keep serialization compatibility.
 *
 * @author Timothy Prinzing
 */
class LightweightDispatcher implements java.io.Serializable, AWTEventListener {

    /**
     * Use serialVersionUID from JDK 1.1 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 5184291520170872969L;
    /*
     * Our own mouse event for when we're dragged over from another hw
     * container
     */
    private static final int  LWD_MOUSE_DRAGGED_OVER = 1500;

    private static final PlatformLogger eventLog = PlatformLogger.getLogger("java.awt.event.LightweightDispatcher");

    private static final int BUTTONS_DOWN_MASK;

    static {
        int[] buttonsDownMask = AWTAccessor.getInputEventAccessor().
                getButtonDownMasks();
        int mask = 0;
        for (int buttonDownMask : buttonsDownMask) {
            mask |= buttonDownMask;
        }
        BUTTONS_DOWN_MASK = mask;
    }

    LightweightDispatcher(Container nativeContainer) {
        this.nativeContainer = nativeContainer;
        mouseEventTarget = new WeakReference<>(null);
        targetLastEntered = new WeakReference<>(null);
        targetLastEnteredDT = new WeakReference<>(null);
        eventMask = 0;
    }

    /*
     * Clean up any resources allocated when dispatcher was created;
     * should be called from Container.removeNotify
     */
    void dispose() {
        //System.out.println("Disposing lw dispatcher");
        stopListeningForOtherDrags();
        mouseEventTarget.clear();
        targetLastEntered.clear();
        targetLastEnteredDT.clear();
    }

    /**
     * Enables events to subcomponents.
     */
    void enableEvents(long events) {
        eventMask |= events;
    }

    /**
     * Dispatches an event to a sub-component if necessary, and
     * returns whether or not the event was forwarded to a
     * sub-component.
     *
     * @param e the event
     */
    boolean dispatchEvent(AWTEvent e) {
        boolean ret = false;

        /*
         * Fix for BugTraq Id 4389284.
         * Dispatch SunDropTargetEvents regardless of eventMask value.
         * Do not update cursor on dispatching SunDropTargetEvents.
         */
        if (e instanceof SunDropTargetEvent) {

            SunDropTargetEvent sdde = (SunDropTargetEvent) e;
            ret = processDropTargetEvent(sdde);

        } else {
            if (e instanceof MouseEvent && (eventMask & MOUSE_MASK) != 0) {
                MouseEvent me = (MouseEvent) e;
                ret = processMouseEvent(me);
            }

            if (e.getID() == MouseEvent.MOUSE_MOVED) {
                nativeContainer.updateCursorImmediately();
            }
        }

        return ret;
    }

    /* This method effectively returns whether or not a mouse button was down
     * just BEFORE the event happened.  A better method name might be
     * wasAMouseButtonDownBeforeThisEvent().
     */
    private boolean isMouseGrab(MouseEvent e) {
        int modifiers = e.getModifiersEx();

        if (e.getID() == MouseEvent.MOUSE_PRESSED
                || e.getID() == MouseEvent.MOUSE_RELEASED) {
            modifiers ^= InputEvent.getMaskForButton(e.getButton());
        }
        /* modifiers now as just before event */
        return ((modifiers & BUTTONS_DOWN_MASK) != 0);
    }

    /**
     * This method attempts to distribute a mouse event to a lightweight
     * component.  It tries to avoid doing any unnecessary probes down
     * into the component tree to minimize the overhead of determining
     * where to route the event, since mouse movement events tend to
     * come in large and frequent amounts.
     */
    private boolean processMouseEvent(MouseEvent e) {
        int id = e.getID();
        Component mouseOver =   // sensitive to mouse events
            nativeContainer.getMouseEventTarget(e.getX(), e.getY(),
                                                Container.INCLUDE_SELF);

        trackMouseEnterExit(mouseOver, e);

        Component met = mouseEventTarget.get();
        // 4508327 : MOUSE_CLICKED should only go to the recipient of
        // the accompanying MOUSE_PRESSED, so don't reset mouseEventTarget on a
        // MOUSE_CLICKED.
        if (!isMouseGrab(e) && id != MouseEvent.MOUSE_CLICKED) {
            met = (mouseOver != nativeContainer) ? mouseOver : null;
            mouseEventTarget = new WeakReference<>(met);
        }

        if (met != null) {
            switch (id) {
                case MouseEvent.MOUSE_ENTERED:
                case MouseEvent.MOUSE_EXITED:
                    break;
                case MouseEvent.MOUSE_PRESSED:
                    retargetMouseEvent(met, id, e);
                    break;
                case MouseEvent.MOUSE_RELEASED:
                    retargetMouseEvent(met, id, e);
                    break;
                case MouseEvent.MOUSE_CLICKED:
                    // 4508327: MOUSE_CLICKED should never be dispatched to a Component
                    // other than that which received the MOUSE_PRESSED event.  If the
                    // mouse is now over a different Component, don't dispatch the event.
                    // The previous fix for a similar problem was associated with bug
                    // 4155217.
                    if (mouseOver == met) {
                        retargetMouseEvent(mouseOver, id, e);
                    }
                    break;
                case MouseEvent.MOUSE_MOVED:
                    retargetMouseEvent(met, id, e);
                    break;
                case MouseEvent.MOUSE_DRAGGED:
                    if (isMouseGrab(e)) {
                        retargetMouseEvent(met, id, e);
                    }
                    break;
                case MouseEvent.MOUSE_WHEEL:
                    // This may send it somewhere that doesn't have MouseWheelEvents
                    // enabled.  In this case, Component.dispatchEventImpl() will
                    // retarget the event to a parent that DOES have the events enabled.
                    if (eventLog.isLoggable(PlatformLogger.Level.FINEST) && (mouseOver != null)) {
                        eventLog.finest("retargeting mouse wheel to " +
                                mouseOver.getName() + ", " +
                                mouseOver.getClass());
                    }
                    retargetMouseEvent(mouseOver, id, e);
                    break;
            }
            //Consuming of wheel events is implemented in "retargetMouseEvent".
            if (id != MouseEvent.MOUSE_WHEEL) {
                e.consume();
            }
        }
        return e.isConsumed();
    }

    private boolean processDropTargetEvent(SunDropTargetEvent e) {
        int id = e.getID();
        int x = e.getX();
        int y = e.getY();

        /*
         * Fix for BugTraq ID 4395290.
         * It is possible that SunDropTargetEvent's Point is outside of the
         * native container bounds. In this case we truncate coordinates.
         */
        if (!nativeContainer.contains(x, y)) {
            final Dimension d = nativeContainer.getSize();
            if (d.width <= x) {
                x = d.width - 1;
            } else if (x < 0) {
                x = 0;
            }
            if (d.height <= y) {
                y = d.height - 1;
            } else if (y < 0) {
                y = 0;
            }
        }
        Component mouseOver =   // not necessarily sensitive to mouse events
            nativeContainer.getDropTargetEventTarget(x, y,
                                                     Container.INCLUDE_SELF);
        trackMouseEnterExit(mouseOver, e);

        if (mouseOver != nativeContainer && mouseOver != null) {
            switch (id) {
            case SunDropTargetEvent.MOUSE_ENTERED:
            case SunDropTargetEvent.MOUSE_EXITED:
                break;
            default:
                retargetMouseEvent(mouseOver, id, e);
                e.consume();
                break;
            }
        }
        return e.isConsumed();
    }

    /*
     * Generates dnd enter/exit events as mouse moves over lw components
     * @param targetOver       Target mouse is over (including native container)
     * @param e                SunDropTarget mouse event in native container
     */
    private void trackDropTargetEnterExit(Component targetOver, MouseEvent e) {
        int id = e.getID();
        if (id == MouseEvent.MOUSE_ENTERED && isMouseDTInNativeContainer) {
            // This can happen if a lightweight component which initiated the
            // drag has an associated drop target. MOUSE_ENTERED comes when the
            // mouse is in the native container already. To propagate this event
            // properly we should null out targetLastEntered.
            targetLastEnteredDT.clear();
        } else if (id == MouseEvent.MOUSE_ENTERED) {
            isMouseDTInNativeContainer = true;
        } else if (id == MouseEvent.MOUSE_EXITED) {
            isMouseDTInNativeContainer = false;
        }
        Component tle = retargetMouseEnterExit(targetOver, e,
                                                     targetLastEnteredDT.get(),
                                                     isMouseDTInNativeContainer);
        targetLastEnteredDT = new WeakReference<>(tle);
    }

    /*
     * Generates enter/exit events as mouse moves over lw components
     * @param targetOver        Target mouse is over (including native container)
     * @param e                 Mouse event in native container
     */
    private void trackMouseEnterExit(Component targetOver, MouseEvent e) {
        if (e instanceof SunDropTargetEvent) {
            trackDropTargetEnterExit(targetOver, e);
            return;
        }
        int id = e.getID();

        if ( id != MouseEvent.MOUSE_EXITED &&
             id != MouseEvent.MOUSE_DRAGGED &&
             id != LWD_MOUSE_DRAGGED_OVER &&
                !isMouseInNativeContainer) {
            // any event but an exit or drag means we're in the native container
            isMouseInNativeContainer = true;
            startListeningForOtherDrags();
        } else if (id == MouseEvent.MOUSE_EXITED) {
            isMouseInNativeContainer = false;
            stopListeningForOtherDrags();
        }
        Component tle = retargetMouseEnterExit(targetOver, e,
                                                   targetLastEntered.get(),
                                                   isMouseInNativeContainer);
        targetLastEntered = new WeakReference<>(tle);
    }

    private Component retargetMouseEnterExit(Component targetOver, MouseEvent e,
                                             Component lastEntered,
                                             boolean inNativeContainer) {
        int id = e.getID();
        Component targetEnter = inNativeContainer ? targetOver : null;

        if (lastEntered != targetEnter) {
            if (lastEntered != null) {
                retargetMouseEvent(lastEntered, MouseEvent.MOUSE_EXITED, e);
            }
            if (id == MouseEvent.MOUSE_EXITED) {
                // consume native exit event if we generate one
                e.consume();
            }

            if (targetEnter != null) {
                retargetMouseEvent(targetEnter, MouseEvent.MOUSE_ENTERED, e);
            }
            if (id == MouseEvent.MOUSE_ENTERED) {
                // consume native enter event if we generate one
                e.consume();
            }
        }
        return targetEnter;
    }

    /*
     * Listens to global mouse drag events so even drags originating
     * from other heavyweight containers will generate enter/exit
     * events in this container
     */
    @SuppressWarnings("removal")
    private void startListeningForOtherDrags() {
        //System.out.println("Adding AWTEventListener");
        java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction<Object>() {
                public Object run() {
                    nativeContainer.getToolkit().addAWTEventListener(
                        LightweightDispatcher.this,
                        AWTEvent.MOUSE_EVENT_MASK |
                        AWTEvent.MOUSE_MOTION_EVENT_MASK);
                    return null;
                }
            }
        );
    }

    @SuppressWarnings("removal")
    private void stopListeningForOtherDrags() {
        //System.out.println("Removing AWTEventListener");
        java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction<Object>() {
                public Object run() {
                    nativeContainer.getToolkit().removeAWTEventListener(LightweightDispatcher.this);
                    return null;
                }
            }
        );
    }

    /*
     * (Implementation of AWTEventListener)
     * Listen for drag events posted in other hw components so we can
     * track enter/exit regardless of where a drag originated
     */
    @SuppressWarnings("deprecation")
    public void eventDispatched(AWTEvent e) {
        boolean isForeignDrag = (e instanceof MouseEvent) &&
                                !(e instanceof SunDropTargetEvent) &&
                                (e.id == MouseEvent.MOUSE_DRAGGED) &&
                                (e.getSource() != nativeContainer);

        if (!isForeignDrag) {
            // only interested in drags from other hw components
            return;
        }

        MouseEvent      srcEvent = (MouseEvent)e;
        MouseEvent      me;

        synchronized (nativeContainer.getTreeLock()) {
            Component srcComponent = srcEvent.getComponent();

            // component may have disappeared since drag event posted
            // (i.e. Swing hierarchical menus)
            if ( !srcComponent.isShowing() ) {
                return;
            }

            // see 5083555
            // check if srcComponent is in any modal blocked window
            Component c = nativeContainer;
            while ((c != null) && !(c instanceof Window)) {
                c = c.getParent_NoClientCode();
            }
            if ((c == null) || ((Window)c).isModalBlocked()) {
                return;
            }

            //
            // create an internal 'dragged-over' event indicating
            // we are being dragged over from another hw component
            //
            me = new MouseEvent(nativeContainer,
                               LWD_MOUSE_DRAGGED_OVER,
                               srcEvent.getWhen(),
                               srcEvent.getModifiersEx() | srcEvent.getModifiers(),
                               srcEvent.getX(),
                               srcEvent.getY(),
                               srcEvent.getXOnScreen(),
                               srcEvent.getYOnScreen(),
                               srcEvent.getClickCount(),
                               srcEvent.isPopupTrigger(),
                               srcEvent.getButton());
            MouseEventAccessor meAccessor = AWTAccessor.getMouseEventAccessor();
            meAccessor.setCausedByTouchEvent(me,
                meAccessor.isCausedByTouchEvent(srcEvent));
            ((AWTEvent)srcEvent).copyPrivateDataInto(me);
            // translate coordinates to this native container
            final Point ptSrcOrigin = srcComponent.getLocationOnScreen();

            if (AppContext.getAppContext() != nativeContainer.appContext) {
                final MouseEvent mouseEvent = me;
                Runnable r = new Runnable() {
                        public void run() {
                            if (!nativeContainer.isShowing() ) {
                                return;
                            }

                            Point       ptDstOrigin = nativeContainer.getLocationOnScreen();
                            mouseEvent.translatePoint(ptSrcOrigin.x - ptDstOrigin.x,
                                              ptSrcOrigin.y - ptDstOrigin.y );
                            Component targetOver =
                                nativeContainer.getMouseEventTarget(mouseEvent.getX(),
                                                                    mouseEvent.getY(),
                                                                    Container.INCLUDE_SELF);
                            trackMouseEnterExit(targetOver, mouseEvent);
                        }
                    };
                SunToolkit.executeOnEventHandlerThread(nativeContainer, r);
                return;
            } else {
                if (!nativeContainer.isShowing() ) {
                    return;
                }

                Point   ptDstOrigin = nativeContainer.getLocationOnScreen();
                me.translatePoint( ptSrcOrigin.x - ptDstOrigin.x, ptSrcOrigin.y - ptDstOrigin.y );
            }
        }
        //System.out.println("Track event: " + me);
        // feed the 'dragged-over' event directly to the enter/exit
        // code (not a real event so don't pass it to dispatchEvent)
        Component targetOver =
            nativeContainer.getMouseEventTarget(me.getX(), me.getY(),
                                                Container.INCLUDE_SELF);
        trackMouseEnterExit(targetOver, me);
    }

    /**
     * Sends a mouse event to the current mouse event recipient using
     * the given event (sent to the windowed host) as a srcEvent.  If
     * the mouse event target is still in the component tree, the
     * coordinates of the event are translated to those of the target.
     * If the target has been removed, we don't bother to send the
     * message.
     */
    @SuppressWarnings("deprecation")
    void retargetMouseEvent(Component target, int id, MouseEvent e) {
        if (target == null) {
            return; // mouse is over another hw component or target is disabled
        }

        int x = e.getX(), y = e.getY();
        Component component;

        for(component = target;
            component != null && component != nativeContainer;
            component = component.getParent()) {
            x -= component.x;
            y -= component.y;
        }
        MouseEvent retargeted;
        if (component != null) {
            if (e instanceof SunDropTargetEvent) {
                retargeted = new SunDropTargetEvent(target,
                                                    id,
                                                    x,
                                                    y,
                                                    ((SunDropTargetEvent)e).getDispatcher());
            } else if (id == MouseEvent.MOUSE_WHEEL) {
                retargeted = new MouseWheelEvent(target,
                                      id,
                                       e.getWhen(),
                                       e.getModifiersEx() | e.getModifiers(),
                                       x,
                                       y,
                                       e.getXOnScreen(),
                                       e.getYOnScreen(),
                                       e.getClickCount(),
                                       e.isPopupTrigger(),
                                       ((MouseWheelEvent)e).getScrollType(),
                                       ((MouseWheelEvent)e).getScrollAmount(),
                                       ((MouseWheelEvent)e).getWheelRotation(),
                                       ((MouseWheelEvent)e).getPreciseWheelRotation());
            }
            else {
                retargeted = new MouseEvent(target,
                                            id,
                                            e.getWhen(),
                                            e.getModifiersEx() | e.getModifiers(),
                                            x,
                                            y,
                                            e.getXOnScreen(),
                                            e.getYOnScreen(),
                                            e.getClickCount(),
                                            e.isPopupTrigger(),
                                            e.getButton());
                MouseEventAccessor meAccessor = AWTAccessor.getMouseEventAccessor();
                meAccessor.setCausedByTouchEvent(retargeted,
                    meAccessor.isCausedByTouchEvent(e));
            }

            ((AWTEvent)e).copyPrivateDataInto(retargeted);

            if (target == nativeContainer) {
                // avoid recursively calling LightweightDispatcher...
                ((Container)target).dispatchEventToSelf(retargeted);
            } else {
                assert AppContext.getAppContext() == target.appContext;

                if (nativeContainer.modalComp != null) {
                    if (((Container)nativeContainer.modalComp).isAncestorOf(target)) {
                        target.dispatchEvent(retargeted);
                    } else {
                        e.consume();
                    }
                } else {
                    target.dispatchEvent(retargeted);
                }
            }
            if (id == MouseEvent.MOUSE_WHEEL && retargeted.isConsumed()) {
                //An exception for wheel bubbling to the native system.
                //In "processMouseEvent" total event consuming for wheel events is skipped.
                //Protection from bubbling of Java-accepted wheel events.
                e.consume();
            }
        }
    }

    // --- member variables -------------------------------

    /**
     * The windowed container that might be hosting events for
     * subcomponents.
     */
    private Container nativeContainer;

    /**
     * This variable is not used, but kept for serialization compatibility
     */
    private Component focus;

    /**
     * The current subcomponent being hosted by this windowed
     * component that has events being forwarded to it.  If this
     * is null, there are currently no events being forwarded to
     * a subcomponent.
     */
    private transient WeakReference<Component> mouseEventTarget;

    /**
     * The last component entered by the {@code MouseEvent}.
     */
    private transient  WeakReference<Component> targetLastEntered;

    /**
     * The last component entered by the {@code SunDropTargetEvent}.
     */
    private transient  WeakReference<Component> targetLastEnteredDT;

    /**
     * Is the mouse over the native container.
     */
    private transient boolean isMouseInNativeContainer = false;

    /**
     * Is DnD over the native container.
     */
    private transient boolean isMouseDTInNativeContainer = false;

    /**
     * This variable is not used, but kept for serialization compatibility
     */
    private Cursor nativeCursor;

    /**
     * The event mask for contained lightweight components.  Lightweight
     * components need a windowed container to host window-related
     * events.  This separate mask indicates events that have been
     * requested by contained lightweight components without effecting
     * the mask of the windowed component itself.
     */
    private long eventMask;

    /**
     * The kind of events routed to lightweight components from windowed
     * hosts.
     */
    private static final long PROXY_EVENT_MASK =
        AWTEvent.FOCUS_EVENT_MASK |
        AWTEvent.KEY_EVENT_MASK |
        AWTEvent.MOUSE_EVENT_MASK |
        AWTEvent.MOUSE_MOTION_EVENT_MASK |
        AWTEvent.MOUSE_WHEEL_EVENT_MASK;

    private static final long MOUSE_MASK =
        AWTEvent.MOUSE_EVENT_MASK |
        AWTEvent.MOUSE_MOTION_EVENT_MASK |
        AWTEvent.MOUSE_WHEEL_EVENT_MASK;
}
