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

import java.applet.Applet;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputEvent;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.PaintEvent;
import java.awt.event.TextEvent;
import java.awt.im.InputContext;
import java.awt.im.InputMethodRequests;
import java.awt.image.BufferStrategy;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.VolatileImage;
import java.awt.peer.ComponentPeer;
import java.awt.peer.ContainerPeer;
import java.awt.peer.LightweightPeer;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.Transient;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serial;
import java.io.Serializable;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleComponent;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleSelection;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.swing.JComponent;
import javax.swing.JRootPane;

import sun.awt.AWTAccessor;
import sun.awt.AppContext;
import sun.awt.ComponentFactory;
import sun.awt.ConstrainableGraphics;
import sun.awt.EmbeddedFrame;
import sun.awt.RequestFocusController;
import sun.awt.SubRegionShowable;
import sun.awt.SunToolkit;
import sun.awt.dnd.SunDropTargetEvent;
import sun.awt.im.CompositionArea;
import sun.awt.image.VSyncedBSManager;
import sun.font.FontManager;
import sun.font.FontManagerFactory;
import sun.font.SunFontManager;
import sun.java2d.SunGraphics2D;
import sun.java2d.SunGraphicsEnvironment;
import sun.java2d.pipe.Region;
import sun.java2d.pipe.hw.ExtendedBufferCapabilities;
import sun.security.action.GetPropertyAction;
import sun.swing.SwingAccessor;
import sun.util.logging.PlatformLogger;

import static sun.java2d.pipe.hw.ExtendedBufferCapabilities.VSyncType.VSYNC_DEFAULT;
import static sun.java2d.pipe.hw.ExtendedBufferCapabilities.VSyncType.VSYNC_ON;

/**
 * A <em>component</em> is an object having a graphical representation
 * that can be displayed on the screen and that can interact with the
 * user. Examples of components are the buttons, checkboxes, and scrollbars
 * of a typical graphical user interface. <p>
 * The {@code Component} class is the abstract superclass of
 * the nonmenu-related Abstract Window Toolkit components. Class
 * {@code Component} can also be extended directly to create a
 * lightweight component. A lightweight component is a component that is
 * not associated with a native window. On the contrary, a heavyweight
 * component is associated with a native window. The {@link #isLightweight()}
 * method may be used to distinguish between the two kinds of the components.
 * <p>
 * Lightweight and heavyweight components may be mixed in a single component
 * hierarchy. However, for correct operating of such a mixed hierarchy of
 * components, the whole hierarchy must be valid. When the hierarchy gets
 * invalidated, like after changing the bounds of components, or
 * adding/removing components to/from containers, the whole hierarchy must be
 * validated afterwards by means of the {@link Container#validate()} method
 * invoked on the top-most invalid container of the hierarchy.
 *
 * <h2>Serialization</h2>
 * It is important to note that only AWT listeners which conform
 * to the {@code Serializable} protocol will be saved when
 * the object is stored.  If an AWT object has listeners that
 * aren't marked serializable, they will be dropped at
 * {@code writeObject} time.  Developers will need, as always,
 * to consider the implications of making an object serializable.
 * One situation to watch out for is this:
 * <pre>
 *    import java.awt.*;
 *    import java.awt.event.*;
 *    import java.io.Serializable;
 *
 *    class MyApp implements ActionListener, Serializable
 *    {
 *        BigObjectThatShouldNotBeSerializedWithAButton bigOne;
 *        Button aButton = new Button();
 *
 *        MyApp()
 *        {
 *            // Oops, now aButton has a listener with a reference
 *            // to bigOne!
 *            aButton.addActionListener(this);
 *        }
 *
 *        public void actionPerformed(ActionEvent e)
 *        {
 *            System.out.println("Hello There");
 *        }
 *    }
 * </pre>
 * In this example, serializing {@code aButton} by itself
 * will cause {@code MyApp} and everything it refers to
 * to be serialized as well.  The problem is that the listener
 * is serializable by coincidence, not by design.  To separate
 * the decisions about {@code MyApp} and the
 * {@code ActionListener} being serializable one can use a
 * nested class, as in the following example:
 * <pre>
 *    import java.awt.*;
 *    import java.awt.event.*;
 *    import java.io.Serializable;
 *
 *    class MyApp implements java.io.Serializable
 *    {
 *         BigObjectThatShouldNotBeSerializedWithAButton bigOne;
 *         Button aButton = new Button();
 *
 *         static class MyActionListener implements ActionListener
 *         {
 *             public void actionPerformed(ActionEvent e)
 *             {
 *                 System.out.println("Hello There");
 *             }
 *         }
 *
 *         MyApp()
 *         {
 *             aButton.addActionListener(new MyActionListener());
 *         }
 *    }
 * </pre>
 * <p>
 * <b>Note</b>: For more information on the paint mechanisms utilized
 * by AWT and Swing, including information on how to write the most
 * efficient painting code, see
 * <a href="http://www.oracle.com/technetwork/java/painting-140037.html">Painting in AWT and Swing</a>.
 * <p>
 * For details on the focus subsystem, see
 * <a href="https://docs.oracle.com/javase/tutorial/uiswing/misc/focus.html">
 * How to Use the Focus Subsystem</a>,
 * a section in <em>The Java Tutorial</em>, and the
 * <a href="doc-files/FocusSpec.html">Focus Specification</a>
 * for more information.
 *
 * @author      Arthur van Hoff
 * @author      Sami Shaio
 */
public abstract class Component implements ImageObserver, MenuContainer,
                                           Serializable
{

    private static final PlatformLogger log = PlatformLogger.getLogger("java.awt.Component");
    private static final PlatformLogger eventLog = PlatformLogger.getLogger("java.awt.event.Component");
    private static final PlatformLogger focusLog = PlatformLogger.getLogger("java.awt.focus.Component");
    private static final PlatformLogger mixingLog = PlatformLogger.getLogger("java.awt.mixing.Component");

    /**
     * The peer of the component. The peer implements the component's
     * behavior. The peer is set when the {@code Component} is
     * added to a container that also is a peer.
     * @see #addNotify
     * @see #removeNotify
     */
    transient volatile ComponentPeer peer;

    /**
     * The parent of the object. It may be {@code null}
     * for top-level components.
     * @see #getParent
     */
    transient Container parent;

    /**
     * The {@code AppContext} of the component. Applets/Plugin may
     * change the AppContext.
     */
    transient AppContext appContext;

    /**
     * The x position of the component in the parent's coordinate system.
     *
     * @serial
     * @see #getLocation
     */
    int x;

    /**
     * The y position of the component in the parent's coordinate system.
     *
     * @serial
     * @see #getLocation
     */
    int y;

    /**
     * The width of the component.
     *
     * @serial
     * @see #getSize
     */
    int width;

    /**
     * The height of the component.
     *
     * @serial
     * @see #getSize
     */
    int height;

    /**
     * The foreground color for this component.
     * {@code foreground} can be {@code null}.
     *
     * @serial
     * @see #getForeground
     * @see #setForeground
     */
    Color       foreground;

    /**
     * The background color for this component.
     * {@code background} can be {@code null}.
     *
     * @serial
     * @see #getBackground
     * @see #setBackground
     */
    Color       background;

    /**
     * The font used by this component.
     * The {@code font} can be {@code null}.
     *
     * @serial
     * @see #getFont
     * @see #setFont
     */
    volatile Font font;

    /**
     * The font which the peer is currently using.
     * ({@code null} if no peer exists.)
     */
    Font        peerFont;

    /**
     * The cursor displayed when pointer is over this component.
     * This value can be {@code null}.
     *
     * @serial
     * @see #getCursor
     * @see #setCursor
     */
    Cursor      cursor;

    /**
     * The locale for the component.
     *
     * @serial
     * @see #getLocale
     * @see #setLocale
     */
    Locale      locale;

    /**
     * A reference to a {@code GraphicsConfiguration} object
     * used to describe the characteristics of a graphics
     * destination.
     * This value can be {@code null}.
     *
     * @since 1.3
     * @serial
     * @see GraphicsConfiguration
     * @see #getGraphicsConfiguration
     */
    private transient volatile GraphicsConfiguration graphicsConfig;

    /**
     * A reference to a {@code BufferStrategy} object
     * used to manipulate the buffers on this component.
     *
     * @since 1.4
     * @see java.awt.image.BufferStrategy
     * @see #getBufferStrategy()
     */
    private transient BufferStrategy bufferStrategy = null;

    /**
     * True when the object should ignore all repaint events.
     *
     * @since 1.4
     * @serial
     * @see #setIgnoreRepaint
     * @see #getIgnoreRepaint
     */
    boolean ignoreRepaint = false;

    /**
     * True when the object is visible. An object that is not
     * visible is not drawn on the screen.
     *
     * @serial
     * @see #isVisible
     * @see #setVisible
     */
    boolean visible = true;

    /**
     * True when the object is enabled. An object that is not
     * enabled does not interact with the user.
     *
     * @serial
     * @see #isEnabled
     * @see #setEnabled
     */
    boolean enabled = true;

    /**
     * True when the object is valid. An invalid object needs to
     * be laid out. This flag is set to false when the object
     * size is changed.
     *
     * @serial
     * @see #isValid
     * @see #validate
     * @see #invalidate
     */
    private volatile boolean valid = false;

    /**
     * The {@code DropTarget} associated with this component.
     *
     * @since 1.2
     * @serial
     * @see #setDropTarget
     * @see #getDropTarget
     */
    DropTarget dropTarget;

    /**
     * @serial
     * @see #add
     */
    Vector<PopupMenu> popups;

    /**
     * A component's name.
     * This field can be {@code null}.
     *
     * @serial
     * @see #getName
     * @see #setName(String)
     */
    private String name;

    /**
     * A bool to determine whether the name has
     * been set explicitly. {@code nameExplicitlySet} will
     * be false if the name has not been set and
     * true if it has.
     *
     * @serial
     * @see #getName
     * @see #setName(String)
     */
    private boolean nameExplicitlySet = false;

    /**
     * Indicates whether this Component can be focused.
     *
     * @serial
     * @see #setFocusable
     * @see #isFocusable
     * @since 1.4
     */
    private boolean focusable = true;

    private static final int FOCUS_TRAVERSABLE_UNKNOWN = 0;
    private static final int FOCUS_TRAVERSABLE_DEFAULT = 1;
    private static final int FOCUS_TRAVERSABLE_SET = 2;

    /**
     * Tracks whether this Component is relying on default focus traversability.
     *
     * @serial
     * @since 1.4
     */
    private int isFocusTraversableOverridden = FOCUS_TRAVERSABLE_UNKNOWN;

    /**
     * The focus traversal keys. These keys will generate focus traversal
     * behavior for Components for which focus traversal keys are enabled. If a
     * value of null is specified for a traversal key, this Component inherits
     * that traversal key from its parent. If all ancestors of this Component
     * have null specified for that traversal key, then the current
     * KeyboardFocusManager's default traversal key is used.
     *
     * @serial
     * @see #setFocusTraversalKeys
     * @see #getFocusTraversalKeys
     * @since 1.4
     */
    @SuppressWarnings("serial") // Not statically typed as Serializable
    Set<AWTKeyStroke>[] focusTraversalKeys;

    private static final String[] focusTraversalKeyPropertyNames = {
        "forwardFocusTraversalKeys",
        "backwardFocusTraversalKeys",
        "upCycleFocusTraversalKeys",
        "downCycleFocusTraversalKeys"
    };

    /**
     * Indicates whether focus traversal keys are enabled for this Component.
     * Components for which focus traversal keys are disabled receive key
     * events for focus traversal keys. Components for which focus traversal
     * keys are enabled do not see these events; instead, the events are
     * automatically converted to traversal operations.
     *
     * @serial
     * @see #setFocusTraversalKeysEnabled
     * @see #getFocusTraversalKeysEnabled
     * @since 1.4
     */
    private boolean focusTraversalKeysEnabled = true;

    /**
     * The locking object for AWT component-tree and layout operations.
     *
     * @see #getTreeLock
     */
    static final Object LOCK = new AWTTreeLock();
    static class AWTTreeLock {}

    /*
     * The component's AccessControlContext.
     */
    @SuppressWarnings("removal")
    private transient volatile AccessControlContext acc =
        AccessController.getContext();

    /**
     * Minimum size.
     * (This field perhaps should have been transient).
     *
     * @serial
     */
    Dimension minSize;

    /**
     * Whether or not setMinimumSize has been invoked with a non-null value.
     */
    boolean minSizeSet;

    /**
     * Preferred size.
     * (This field perhaps should have been transient).
     *
     * @serial
     */
    Dimension prefSize;

    /**
     * Whether or not setPreferredSize has been invoked with a non-null value.
     */
    boolean prefSizeSet;

    /**
     * Maximum size
     *
     * @serial
     */
    Dimension maxSize;

    /**
     * Whether or not setMaximumSize has been invoked with a non-null value.
     */
    boolean maxSizeSet;

    /**
     * The orientation for this component.
     * @see #getComponentOrientation
     * @see #setComponentOrientation
     */
    transient ComponentOrientation componentOrientation
    = ComponentOrientation.UNKNOWN;

    /**
     * {@code newEventsOnly} will be true if the event is
     * one of the event types enabled for the component.
     * It will then allow for normal processing to
     * continue.  If it is false the event is passed
     * to the component's parent and up the ancestor
     * tree until the event has been consumed.
     *
     * @serial
     * @see #dispatchEvent
     */
    boolean newEventsOnly = false;
    transient ComponentListener componentListener;
    transient FocusListener focusListener;
    transient HierarchyListener hierarchyListener;
    transient HierarchyBoundsListener hierarchyBoundsListener;
    transient KeyListener keyListener;
    transient MouseListener mouseListener;
    transient MouseMotionListener mouseMotionListener;
    transient MouseWheelListener mouseWheelListener;
    transient InputMethodListener inputMethodListener;

    /** Internal, constants for serialization */
    static final String actionListenerK = "actionL";
    static final String adjustmentListenerK = "adjustmentL";
    static final String componentListenerK = "componentL";
    static final String containerListenerK = "containerL";
    static final String focusListenerK = "focusL";
    static final String itemListenerK = "itemL";
    static final String keyListenerK = "keyL";
    static final String mouseListenerK = "mouseL";
    static final String mouseMotionListenerK = "mouseMotionL";
    static final String mouseWheelListenerK = "mouseWheelL";
    static final String textListenerK = "textL";
    static final String ownedWindowK = "ownedL";
    static final String windowListenerK = "windowL";
    static final String inputMethodListenerK = "inputMethodL";
    static final String hierarchyListenerK = "hierarchyL";
    static final String hierarchyBoundsListenerK = "hierarchyBoundsL";
    static final String windowStateListenerK = "windowStateL";
    static final String windowFocusListenerK = "windowFocusL";

    /**
     * The {@code eventMask} is ONLY set by subclasses via
     * {@code enableEvents}.
     * The mask should NOT be set when listeners are registered
     * so that we can distinguish the difference between when
     * listeners request events and subclasses request them.
     * One bit is used to indicate whether input methods are
     * enabled; this bit is set by {@code enableInputMethods} and is
     * on by default.
     *
     * @serial
     * @see #enableInputMethods
     * @see AWTEvent
     */
    long eventMask = AWTEvent.INPUT_METHODS_ENABLED_MASK;

    /**
     * Static properties for incremental drawing.
     * @see #imageUpdate
     */
    static boolean isInc;
    static int incRate;
    static {
        /* ensure that the necessary native libraries are loaded */
        Toolkit.loadLibraries();
        /* initialize JNI field and method ids */
        if (!GraphicsEnvironment.isHeadless()) {
            initIDs();
        }

        @SuppressWarnings("removal")
        String s = java.security.AccessController.doPrivileged(
                                                               new GetPropertyAction("awt.image.incrementaldraw"));
        isInc = (s == null || s.equals("true"));

        @SuppressWarnings("removal")
        String s2 = java.security.AccessController.doPrivileged(
                                                        new GetPropertyAction("awt.image.redrawrate"));
        incRate = (s2 != null) ? Integer.parseInt(s2) : 100;
    }

    /**
     * Ease-of-use constant for {@code getAlignmentY()}.
     * Specifies an alignment to the top of the component.
     * @see     #getAlignmentY
     */
    public static final float TOP_ALIGNMENT = 0.0f;

    /**
     * Ease-of-use constant for {@code getAlignmentY} and
     * {@code getAlignmentX}. Specifies an alignment to
     * the center of the component
     * @see     #getAlignmentX
     * @see     #getAlignmentY
     */
    public static final float CENTER_ALIGNMENT = 0.5f;

    /**
     * Ease-of-use constant for {@code getAlignmentY}.
     * Specifies an alignment to the bottom of the component.
     * @see     #getAlignmentY
     */
    public static final float BOTTOM_ALIGNMENT = 1.0f;

    /**
     * Ease-of-use constant for {@code getAlignmentX}.
     * Specifies an alignment to the left side of the component.
     * @see     #getAlignmentX
     */
    public static final float LEFT_ALIGNMENT = 0.0f;

    /**
     * Ease-of-use constant for {@code getAlignmentX}.
     * Specifies an alignment to the right side of the component.
     * @see     #getAlignmentX
     */
    public static final float RIGHT_ALIGNMENT = 1.0f;

    /**
     * Use serialVersionUID from JDK 1.1 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -7644114512714619750L;

    /**
     * If any {@code PropertyChangeListeners} have been registered,
     * the {@code changeSupport} field describes them.
     *
     * @serial
     * @since 1.2
     * @see #addPropertyChangeListener
     * @see #removePropertyChangeListener
     * @see #firePropertyChange
     */
    private PropertyChangeSupport changeSupport;

    /*
     * In some cases using "this" as an object to synchronize by
     * can lead to a deadlock if client code also uses synchronization
     * by a component object. For every such situation revealed we should
     * consider possibility of replacing "this" with the package private
     * objectLock object introduced below. So far there are 3 issues known:
     * - CR 6708322 (the getName/setName methods);
     * - CR 6608764 (the PropertyChangeListener machinery);
     * - CR 7108598 (the Container.paint/KeyboardFocusManager.clearMostRecentFocusOwner methods).
     *
     * Note: this field is considered final, though readObject() prohibits
     * initializing final fields.
     */
    private transient Object objectLock = new Object();
    Object getObjectLock() {
        return objectLock;
    }

    /*
     * Returns the acc this component was constructed with.
     */
    @SuppressWarnings("removal")
    final AccessControlContext getAccessControlContext() {
        if (acc == null) {
            throw new SecurityException("Component is missing AccessControlContext");
        }
        return acc;
    }

    /**
     * Whether the component is packed or not;
     */
    boolean isPacked = false;

    /**
     * Pseudoparameter for direct Geometry API (setLocation, setBounds setSize
     * to signal setBounds what's changing. Should be used under TreeLock.
     * This is only needed due to the inability to change the cross-calling
     * order of public and deprecated methods.
     */
    private int boundsOp = ComponentPeer.DEFAULT_OPERATION;

    /**
     * Enumeration of the common ways the baseline of a component can
     * change as the size changes.  The baseline resize behavior is
     * primarily for layout managers that need to know how the
     * position of the baseline changes as the component size changes.
     * In general the baseline resize behavior will be valid for sizes
     * greater than or equal to the minimum size (the actual minimum
     * size; not a developer specified minimum size).  For sizes
     * smaller than the minimum size the baseline may change in a way
     * other than the baseline resize behavior indicates.  Similarly,
     * as the size approaches {@code Integer.MAX_VALUE} and/or
     * {@code Short.MAX_VALUE} the baseline may change in a way
     * other than the baseline resize behavior indicates.
     *
     * @see #getBaselineResizeBehavior
     * @see #getBaseline(int,int)
     * @since 1.6
     */
    public enum BaselineResizeBehavior {
        /**
         * Indicates the baseline remains fixed relative to the
         * y-origin.  That is, {@code getBaseline} returns
         * the same value regardless of the height or width.  For example, a
         * {@code JLabel} containing non-empty text with a
         * vertical alignment of {@code TOP} should have a
         * baseline type of {@code CONSTANT_ASCENT}.
         */
        CONSTANT_ASCENT,

        /**
         * Indicates the baseline remains fixed relative to the height
         * and does not change as the width is varied.  That is, for
         * any height H the difference between H and
         * {@code getBaseline(w, H)} is the same.  For example, a
         * {@code JLabel} containing non-empty text with a
         * vertical alignment of {@code BOTTOM} should have a
         * baseline type of {@code CONSTANT_DESCENT}.
         */
        CONSTANT_DESCENT,

        /**
         * Indicates the baseline remains a fixed distance from
         * the center of the component.  That is, for any height H the
         * difference between {@code getBaseline(w, H)} and
         * {@code H / 2} is the same (plus or minus one depending upon
         * rounding error).
         * <p>
         * Because of possible rounding errors it is recommended
         * you ask for the baseline with two consecutive heights and use
         * the return value to determine if you need to pad calculations
         * by 1.  The following shows how to calculate the baseline for
         * any height:
         * <pre>
         *   Dimension preferredSize = component.getPreferredSize();
         *   int baseline = getBaseline(preferredSize.width,
         *                              preferredSize.height);
         *   int nextBaseline = getBaseline(preferredSize.width,
         *                                  preferredSize.height + 1);
         *   // Amount to add to height when calculating where baseline
         *   // lands for a particular height:
         *   int padding = 0;
         *   // Where the baseline is relative to the mid point
         *   int baselineOffset = baseline - height / 2;
         *   if (preferredSize.height % 2 == 0 &amp;&amp;
         *       baseline != nextBaseline) {
         *       padding = 1;
         *   }
         *   else if (preferredSize.height % 2 == 1 &amp;&amp;
         *            baseline == nextBaseline) {
         *       baselineOffset--;
         *       padding = 1;
         *   }
         *   // The following calculates where the baseline lands for
         *   // the height z:
         *   int calculatedBaseline = (z + padding) / 2 + baselineOffset;
         * </pre>
         */
        CENTER_OFFSET,

        /**
         * Indicates the baseline resize behavior can not be expressed using
         * any of the other constants.  This may also indicate the baseline
         * varies with the width of the component.  This is also returned
         * by components that do not have a baseline.
         */
        OTHER
    }

    /*
     * The shape set with the applyCompoundShape() method. It includes the result
     * of the HW/LW mixing related shape computation. It may also include
     * the user-specified shape of the component.
     * The 'null' value means the component has normal shape (or has no shape at all)
     * and applyCompoundShape() will skip the following shape identical to normal.
     */
    private transient Region compoundShape = null;

    /*
     * Represents the shape of this lightweight component to be cut out from
     * heavyweight components should they intersect. Possible values:
     *    1. null - consider the shape rectangular
     *    2. EMPTY_REGION - nothing gets cut out (children still get cut out)
     *    3. non-empty - this shape gets cut out.
     */
    private transient Region mixingCutoutRegion = null;

    /*
     * Indicates whether addNotify() is complete
     * (i.e. the peer is created).
     */
    private transient boolean isAddNotifyComplete = false;

    /**
     * Should only be used in subclass getBounds to check that part of bounds
     * is actually changing
     */
    int getBoundsOp() {
        assert Thread.holdsLock(getTreeLock());
        return boundsOp;
    }

    void setBoundsOp(int op) {
        assert Thread.holdsLock(getTreeLock());
        if (op == ComponentPeer.RESET_OPERATION) {
            boundsOp = ComponentPeer.DEFAULT_OPERATION;
        } else
            if (boundsOp == ComponentPeer.DEFAULT_OPERATION) {
                boundsOp = op;
            }
    }

    // Whether this Component has had the background erase flag
    // specified via SunToolkit.disableBackgroundErase(). This is
    // needed in order to make this function work on X11 platforms,
    // where currently there is no chance to interpose on the creation
    // of the peer and therefore the call to XSetBackground.
    transient boolean backgroundEraseDisabled;

    static {
        AWTAccessor.setComponentAccessor(new AWTAccessor.ComponentAccessor() {
            public void setBackgroundEraseDisabled(Component comp, boolean disabled) {
                comp.backgroundEraseDisabled = disabled;
            }
            public boolean getBackgroundEraseDisabled(Component comp) {
                return comp.backgroundEraseDisabled;
            }
            public Rectangle getBounds(Component comp) {
                return new Rectangle(comp.x, comp.y, comp.width, comp.height);
            }
            public void setGraphicsConfiguration(Component comp,
                    GraphicsConfiguration gc)
            {
                comp.setGraphicsConfiguration(gc);
            }
            public void requestFocus(Component comp, FocusEvent.Cause cause) {
                comp.requestFocus(cause);
            }
            public boolean canBeFocusOwner(Component comp) {
                return comp.canBeFocusOwner();
            }

            public boolean isVisible(Component comp) {
                return comp.isVisible_NoClientCode();
            }
            public void setRequestFocusController
                (RequestFocusController requestController)
            {
                 Component.setRequestFocusController(requestController);
            }
            public AppContext getAppContext(Component comp) {
                 return comp.appContext;
            }
            public void setAppContext(Component comp, AppContext appContext) {
                 comp.appContext = appContext;
            }
            public Container getParent(Component comp) {
                return comp.getParent_NoClientCode();
            }
            public void setParent(Component comp, Container parent) {
                comp.parent = parent;
            }
            public void setSize(Component comp, int width, int height) {
                comp.width = width;
                comp.height = height;
            }
            public Point getLocation(Component comp) {
                return comp.location_NoClientCode();
            }
            public void setLocation(Component comp, int x, int y) {
                comp.x = x;
                comp.y = y;
            }
            public boolean isEnabled(Component comp) {
                return comp.isEnabledImpl();
            }
            public boolean isDisplayable(Component comp) {
                return comp.peer != null;
            }
            public Cursor getCursor(Component comp) {
                return comp.getCursor_NoClientCode();
            }
            @SuppressWarnings("unchecked")
            public <T extends ComponentPeer> T getPeer(Component comp) {
                return (T) comp.peer;
            }
            public void setPeer(Component comp, ComponentPeer peer) {
                comp.peer = peer;
            }
            public boolean isLightweight(Component comp) {
                return (comp.peer instanceof LightweightPeer);
            }
            public boolean getIgnoreRepaint(Component comp) {
                return comp.ignoreRepaint;
            }
            public int getWidth(Component comp) {
                return comp.width;
            }
            public int getHeight(Component comp) {
                return comp.height;
            }
            public int getX(Component comp) {
                return comp.x;
            }
            public int getY(Component comp) {
                return comp.y;
            }
            public Color getForeground(Component comp) {
                return comp.foreground;
            }
            public Color getBackground(Component comp) {
                return comp.background;
            }
            public void setBackground(Component comp, Color background) {
                comp.background = background;
            }
            public Font getFont(Component comp) {
                return comp.getFont_NoClientCode();
            }
            public void processEvent(Component comp, AWTEvent e) {
                comp.processEvent(e);
            }

            @SuppressWarnings("removal")
            public AccessControlContext getAccessControlContext(Component comp) {
                return comp.getAccessControlContext();
            }

            public void revalidateSynchronously(Component comp) {
                comp.revalidateSynchronously();
            }

            @Override
            public void createBufferStrategy(Component comp, int numBuffers,
                    BufferCapabilities caps) throws AWTException {
                comp.createBufferStrategy(numBuffers, caps);
            }

            @Override
            public BufferStrategy getBufferStrategy(Component comp) {
                return comp.getBufferStrategy();
            }
        });
    }

    /**
     * Constructs a new component. Class {@code Component} can be
     * extended directly to create a lightweight component that does not
     * utilize an opaque native window. A lightweight component must be
     * hosted by a native container somewhere higher up in the component
     * tree (for example, by a {@code Frame} object).
     */
    protected Component() {
        appContext = AppContext.getAppContext();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    void initializeFocusTraversalKeys() {
        focusTraversalKeys = new Set[3];
    }

    /**
     * Constructs a name for this component.  Called by {@code getName}
     * when the name is {@code null}.
     */
    String constructComponentName() {
        return null; // For strict compliance with prior platform versions, a Component
                     // that doesn't set its name should return null from
                     // getName()
    }

    /**
     * Gets the name of the component.
     * @return this component's name
     * @see    #setName
     * @since 1.1
     */
    public String getName() {
        if (name == null && !nameExplicitlySet) {
            synchronized(getObjectLock()) {
                if (name == null && !nameExplicitlySet)
                    name = constructComponentName();
            }
        }
        return name;
    }

    /**
     * Sets the name of the component to the specified string.
     * @param name  the string that is to be this
     *           component's name
     * @see #getName
     * @since 1.1
     */
    public void setName(String name) {
        String oldName;
        synchronized(getObjectLock()) {
            oldName = this.name;
            this.name = name;
            nameExplicitlySet = true;
        }
        firePropertyChange("name", oldName, name);
    }

    /**
     * Gets the parent of this component.
     * @return the parent container of this component
     * @since 1.0
     */
    public Container getParent() {
        return getParent_NoClientCode();
    }

    // NOTE: This method may be called by privileged threads.
    //       This functionality is implemented in a package-private method
    //       to insure that it cannot be overridden by client subclasses.
    //       DO NOT INVOKE CLIENT CODE ON THIS THREAD!
    final Container getParent_NoClientCode() {
        return parent;
    }

    // This method is overridden in the Window class to return null,
    //    because the parent field of the Window object contains
    //    the owner of the window, not its parent.
    Container getContainer() {
        return getParent_NoClientCode();
    }

    /**
     * Associate a {@code DropTarget} with this component.
     * The {@code Component} will receive drops only if it
     * is enabled.
     *
     * @see #isEnabled
     * @param dt The DropTarget
     */

    public synchronized void setDropTarget(DropTarget dt) {
        if (dt == dropTarget || (dropTarget != null && dropTarget.equals(dt)))
            return;

        DropTarget old;

        if ((old = dropTarget) != null) {
            dropTarget.removeNotify();

            DropTarget t = dropTarget;

            dropTarget = null;

            try {
                t.setComponent(null);
            } catch (IllegalArgumentException iae) {
                // ignore it.
            }
        }

        // if we have a new one, and we have a peer, add it!

        if ((dropTarget = dt) != null) {
            try {
                dropTarget.setComponent(this);
                dropTarget.addNotify();
            } catch (IllegalArgumentException iae) {
                if (old != null) {
                    try {
                        old.setComponent(this);
                        dropTarget.addNotify();
                    } catch (IllegalArgumentException iae1) {
                        // ignore it!
                    }
                }
            }
        }
    }

    /**
     * Gets the {@code DropTarget} associated with this
     * {@code Component}.
     *
     * @return the drop target
     */

    public synchronized DropTarget getDropTarget() { return dropTarget; }

    /**
     * Gets the {@code GraphicsConfiguration} associated with this
     * {@code Component}.
     * If the {@code Component} has not been assigned a specific
     * {@code GraphicsConfiguration},
     * the {@code GraphicsConfiguration} of the
     * {@code Component} object's top-level container is
     * returned.
     * If the {@code Component} has been created, but not yet added
     * to a {@code Container}, this method returns {@code null}.
     *
     * @return the {@code GraphicsConfiguration} used by this
     *          {@code Component} or {@code null}
     * @since 1.3
     */
    public GraphicsConfiguration getGraphicsConfiguration() {
        return getGraphicsConfiguration_NoClientCode();
    }

    final GraphicsConfiguration getGraphicsConfiguration_NoClientCode() {
        return graphicsConfig;
    }

    void setGraphicsConfiguration(GraphicsConfiguration gc) {
        synchronized(getTreeLock()) {
            if (updateGraphicsData(gc)) {
                removeNotify();
                addNotify();
            }
        }
    }

    final boolean updateGraphicsData(GraphicsConfiguration gc) {
        GraphicsConfiguration oldConfig = graphicsConfig;
        // First, update own graphics configuration
        boolean ret = updateSelfGraphicsData(gc);
        // Second, update children graphics configurations
        ret |= updateChildGraphicsData(gc);
        // Third, fire PropertyChange if needed
        if (oldConfig != gc) {
            /*
             * If component is moved from one screen to another screen or shown
             * for the first time graphicsConfiguration property is fired to
             * enable the component to recalculate any rendering data, if needed
             */
            firePropertyChange("graphicsConfiguration", oldConfig, gc);
        }
        return ret;
    }

    private boolean updateSelfGraphicsData(GraphicsConfiguration gc) {
        checkTreeLock();
        if (graphicsConfig == gc) {
            return false;
        }
        graphicsConfig = gc;

        ComponentPeer peer = this.peer;
        if (peer != null) {
            return peer.updateGraphicsData(gc);
        }
        return false;
    }

    boolean updateChildGraphicsData(GraphicsConfiguration gc) {
        return false;
    }

    /**
     * Checks that this component's {@code GraphicsDevice}
     * {@code idString} matches the string argument.
     */
    void checkGD(String stringID) {
        if (graphicsConfig != null) {
            if (!graphicsConfig.getDevice().getIDstring().equals(stringID)) {
                throw new IllegalArgumentException(
                                                   "adding a container to a container on a different GraphicsDevice");
            }
        }
    }

    /**
     * Gets this component's locking object (the object that owns the thread
     * synchronization monitor) for AWT component-tree and layout
     * operations.
     * @return this component's locking object
     */
    public final Object getTreeLock() {
        return LOCK;
    }

    final void checkTreeLock() {
        if (!Thread.holdsLock(getTreeLock())) {
            throw new IllegalStateException("This function should be called while holding treeLock");
        }
    }

    /**
     * Gets the toolkit of this component. Note that
     * the frame that contains a component controls which
     * toolkit is used by that component. Therefore if the component
     * is moved from one frame to another, the toolkit it uses may change.
     * @return  the toolkit of this component
     * @since 1.0
     */
    public Toolkit getToolkit() {
        return getToolkitImpl();
    }

    /*
     * This is called by the native code, so client code can't
     * be called on the toolkit thread.
     */
    final Toolkit getToolkitImpl() {
        Container parent = this.parent;
        if (parent != null) {
            return parent.getToolkitImpl();
        }
        return Toolkit.getDefaultToolkit();
    }

    final ComponentFactory getComponentFactory() {
        final Toolkit toolkit = getToolkit();
        if (toolkit instanceof ComponentFactory) {
            return (ComponentFactory) toolkit;
        }
        throw new AWTError("UI components are unsupported by: " + toolkit);
    }

    /**
     * Determines whether this component is valid. A component is valid
     * when it is correctly sized and positioned within its parent
     * container and all its children are also valid.
     * In order to account for peers' size requirements, components are invalidated
     * before they are first shown on the screen. By the time the parent container
     * is fully realized, all its components will be valid.
     * @return {@code true} if the component is valid, {@code false}
     * otherwise
     * @see #validate
     * @see #invalidate
     * @since 1.0
     */
    public boolean isValid() {
        return (peer != null) && valid;
    }

    /**
     * Determines whether this component is displayable. A component is
     * displayable when it is connected to a native screen resource.
     * <p>
     * A component is made displayable either when it is added to
     * a displayable containment hierarchy or when its containment
     * hierarchy is made displayable.
     * A containment hierarchy is made displayable when its ancestor
     * window is either packed or made visible.
     * <p>
     * A component is made undisplayable either when it is removed from
     * a displayable containment hierarchy or when its containment hierarchy
     * is made undisplayable.  A containment hierarchy is made
     * undisplayable when its ancestor window is disposed.
     *
     * @return {@code true} if the component is displayable,
     * {@code false} otherwise
     * @see Container#add(Component)
     * @see Window#pack
     * @see Window#show
     * @see Container#remove(Component)
     * @see Window#dispose
     * @since 1.2
     */
    public boolean isDisplayable() {
        return peer != null;
    }

    /**
     * Determines whether this component should be visible when its
     * parent is visible. Components are
     * initially visible, with the exception of top level components such
     * as {@code Frame} objects.
     * @return {@code true} if the component is visible,
     * {@code false} otherwise
     * @see #setVisible
     * @since 1.0
     */
    @Transient
    public boolean isVisible() {
        return isVisible_NoClientCode();
    }
    final boolean isVisible_NoClientCode() {
        return visible;
    }

    /**
     * Determines whether this component will be displayed on the screen.
     * @return {@code true} if the component and all of its ancestors
     *          until a toplevel window or null parent are visible,
     *          {@code false} otherwise
     */
    boolean isRecursivelyVisible() {
        return visible && (parent == null || parent.isRecursivelyVisible());
    }

    /**
     * Determines the bounds of a visible part of the component relative to its
     * parent.
     *
     * @return the visible part of bounds
     */
    private Rectangle getRecursivelyVisibleBounds() {
        final Component container = getContainer();
        final Rectangle bounds = getBounds();
        if (container == null) {
            // we are top level window or haven't a container, return our bounds
            return bounds;
        }
        // translate the container's bounds to our coordinate space
        final Rectangle parentsBounds = container.getRecursivelyVisibleBounds();
        parentsBounds.setLocation(0, 0);
        return parentsBounds.intersection(bounds);
    }

    /**
     * Translates absolute coordinates into coordinates in the coordinate
     * space of this component.
     */
    Point pointRelativeToComponent(Point absolute) {
        Point compCoords = getLocationOnScreen();
        return new Point(absolute.x - compCoords.x,
                         absolute.y - compCoords.y);
    }

    /**
     * Assuming that mouse location is stored in PointerInfo passed
     * to this method, it finds a Component that is in the same
     * Window as this Component and is located under the mouse pointer.
     * If no such Component exists, null is returned.
     * NOTE: this method should be called under the protection of
     * tree lock, as it is done in Component.getMousePosition() and
     * Container.getMousePosition(boolean).
     */
    Component findUnderMouseInWindow(PointerInfo pi) {
        if (!isShowing()) {
            return null;
        }
        Window win = getContainingWindow();
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        if (!(toolkit instanceof ComponentFactory)) {
            return null;
        }
        if (!((ComponentFactory) toolkit).getMouseInfoPeer().isWindowUnderMouse(win)) {
            return null;
        }
        final boolean INCLUDE_DISABLED = true;
        Point relativeToWindow = win.pointRelativeToComponent(pi.getLocation());
        Component inTheSameWindow = win.findComponentAt(relativeToWindow.x,
                                                        relativeToWindow.y,
                                                        INCLUDE_DISABLED);
        return inTheSameWindow;
    }

    /**
     * Returns the position of the mouse pointer in this {@code Component}'s
     * coordinate space if the {@code Component} is directly under the mouse
     * pointer, otherwise returns {@code null}.
     * If the {@code Component} is not showing on the screen, this method
     * returns {@code null} even if the mouse pointer is above the area
     * where the {@code Component} would be displayed.
     * If the {@code Component} is partially or fully obscured by other
     * {@code Component}s or native windows, this method returns a non-null
     * value only if the mouse pointer is located above the unobscured part of the
     * {@code Component}.
     * <p>
     * For {@code Container}s it returns a non-null value if the mouse is
     * above the {@code Container} itself or above any of its descendants.
     * Use {@link Container#getMousePosition(boolean)} if you need to exclude children.
     * <p>
     * Sometimes the exact mouse coordinates are not important, and the only thing
     * that matters is whether a specific {@code Component} is under the mouse
     * pointer. If the return value of this method is {@code null}, mouse
     * pointer is not directly above the {@code Component}.
     *
     * @exception HeadlessException if GraphicsEnvironment.isHeadless() returns true
     * @see       #isShowing
     * @see       Container#getMousePosition
     * @return    mouse coordinates relative to this {@code Component}, or null
     * @since     1.5
     */
    public Point getMousePosition() throws HeadlessException {
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
            if (!isSameOrAncestorOf(inTheSameWindow, true)) {
                return null;
            }
            return pointRelativeToComponent(pi.getLocation());
        }
    }

    /**
     * Overridden in Container. Must be called under TreeLock.
     */
    boolean isSameOrAncestorOf(Component comp, boolean allowChildren) {
        return comp == this;
    }

    /**
     * Determines whether this component is showing on screen. This means
     * that the component must be visible, and it must be in a container
     * that is visible and showing.
     * <p>
     * <strong>Note:</strong> sometimes there is no way to detect whether the
     * {@code Component} is actually visible to the user.  This can happen when:
     * <ul>
     * <li>the component has been added to a visible {@code ScrollPane} but
     * the {@code Component} is not currently in the scroll pane's view port.
     * <li>the {@code Component} is obscured by another {@code Component} or
     * {@code Container}.
     * </ul>
     * @return {@code true} if the component is showing,
     *          {@code false} otherwise
     * @see #setVisible
     * @since 1.0
     */
    public boolean isShowing() {
        if (visible && (peer != null)) {
            Container parent = this.parent;
            return (parent == null) || parent.isShowing();
        }
        return false;
    }

    /**
     * Determines whether this component is enabled. An enabled component
     * can respond to user input and generate events. Components are
     * enabled initially by default. A component may be enabled or disabled by
     * calling its {@code setEnabled} method.
     * @return {@code true} if the component is enabled,
     *          {@code false} otherwise
     * @see #setEnabled
     * @since 1.0
     */
    public boolean isEnabled() {
        return isEnabledImpl();
    }

    /*
     * This is called by the native code, so client code can't
     * be called on the toolkit thread.
     */
    final boolean isEnabledImpl() {
        return enabled;
    }

    /**
     * Enables or disables this component, depending on the value of the
     * parameter {@code b}. An enabled component can respond to user
     * input and generate events. Components are enabled initially by default.
     *
     * <p>Note: Disabling a lightweight component does not prevent it from
     * receiving MouseEvents.
     * <p>Note: Disabling a heavyweight container prevents all components
     * in this container from receiving any input events.  But disabling a
     * lightweight container affects only this container.
     *
     * @param     b   If {@code true}, this component is
     *            enabled; otherwise this component is disabled
     * @see #isEnabled
     * @see #isLightweight
     * @since 1.1
     */
    public void setEnabled(boolean b) {
        enable(b);
    }

    /**
     * @deprecated As of JDK version 1.1,
     * replaced by {@code setEnabled(boolean)}.
     */
    @Deprecated
    public void enable() {
        if (!enabled) {
            synchronized (getTreeLock()) {
                enabled = true;
                ComponentPeer peer = this.peer;
                if (peer != null) {
                    peer.setEnabled(true);
                    if (visible && !getRecursivelyVisibleBounds().isEmpty()) {
                        updateCursorImmediately();
                    }
                }
            }
            if (accessibleContext != null) {
                accessibleContext.firePropertyChange(
                                                     AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                                                     null, AccessibleState.ENABLED);
            }
        }
    }

    /**
     * Enables or disables this component.
     *
     * @param  b {@code true} to enable this component;
     *         otherwise {@code false}
     *
     * @deprecated As of JDK version 1.1,
     * replaced by {@code setEnabled(boolean)}.
     */
    @Deprecated
    public void enable(boolean b) {
        if (b) {
            enable();
        } else {
            disable();
        }
    }

    /**
     * @deprecated As of JDK version 1.1,
     * replaced by {@code setEnabled(boolean)}.
     */
    @Deprecated
    public void disable() {
        if (enabled) {
            KeyboardFocusManager.clearMostRecentFocusOwner(this);
            synchronized (getTreeLock()) {
                enabled = false;
                // A disabled lw container is allowed to contain a focus owner.
                if ((isFocusOwner() || (containsFocus() && !isLightweight())) &&
                    KeyboardFocusManager.isAutoFocusTransferEnabled())
                {
                    // Don't clear the global focus owner. If transferFocus
                    // fails, we want the focus to stay on the disabled
                    // Component so that keyboard traversal, et. al. still
                    // makes sense to the user.
                    transferFocus(false);
                }
                ComponentPeer peer = this.peer;
                if (peer != null) {
                    peer.setEnabled(false);
                    if (visible && !getRecursivelyVisibleBounds().isEmpty()) {
                        updateCursorImmediately();
                    }
                }
            }
            if (accessibleContext != null) {
                accessibleContext.firePropertyChange(
                                                     AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                                                     null, AccessibleState.ENABLED);
            }
        }
    }

    /**
     * Returns true if this component is painted to an offscreen image
     * ("buffer") that's copied to the screen later.  Component
     * subclasses that support double buffering should override this
     * method to return true if double buffering is enabled.
     *
     * @return false by default
     */
    public boolean isDoubleBuffered() {
        return false;
    }

    /**
     * Enables or disables input method support for this component. If input
     * method support is enabled and the component also processes key events,
     * incoming events are offered to
     * the current input method and will only be processed by the component or
     * dispatched to its listeners if the input method does not consume them.
     * By default, input method support is enabled.
     *
     * @param enable true to enable, false to disable
     * @see #processKeyEvent
     * @since 1.2
     */
    public void enableInputMethods(boolean enable) {
        if (enable) {
            if ((eventMask & AWTEvent.INPUT_METHODS_ENABLED_MASK) != 0)
                return;

            // If this component already has focus, then activate the
            // input method by dispatching a synthesized focus gained
            // event.
            if (isFocusOwner()) {
                InputContext inputContext = getInputContext();
                if (inputContext != null) {
                    FocusEvent focusGainedEvent =
                        new FocusEvent(this, FocusEvent.FOCUS_GAINED);
                    inputContext.dispatchEvent(focusGainedEvent);
                }
            }

            eventMask |= AWTEvent.INPUT_METHODS_ENABLED_MASK;
        } else {
            if ((eventMask & AWTEvent.INPUT_METHODS_ENABLED_MASK) != 0) {
                InputContext inputContext = getInputContext();
                if (inputContext != null) {
                    inputContext.endComposition();
                    inputContext.removeNotify(this);
                }
            }
            eventMask &= ~AWTEvent.INPUT_METHODS_ENABLED_MASK;
        }
    }

    /**
     * Shows or hides this component depending on the value of parameter
     * {@code b}.
     * <p>
     * This method changes layout-related information, and therefore,
     * invalidates the component hierarchy.
     *
     * @param b  if {@code true}, shows this component;
     * otherwise, hides this component
     * @see #isVisible
     * @see #invalidate
     * @since 1.1
     */
    public void setVisible(boolean b) {
        show(b);
    }

    /**
     * @deprecated As of JDK version 1.1,
     * replaced by {@code setVisible(boolean)}.
     */
    @Deprecated
    public void show() {
        if (!visible) {
            synchronized (getTreeLock()) {
                visible = true;
                mixOnShowing();
                ComponentPeer peer = this.peer;
                if (peer != null) {
                    peer.setVisible(true);
                    createHierarchyEvents(HierarchyEvent.HIERARCHY_CHANGED,
                                          this, parent,
                                          HierarchyEvent.SHOWING_CHANGED,
                                          Toolkit.enabledOnToolkit(AWTEvent.HIERARCHY_EVENT_MASK));
                    if (peer instanceof LightweightPeer) {
                        repaint();
                    }
                    updateCursorImmediately();
                }

                if (componentListener != null ||
                    (eventMask & AWTEvent.COMPONENT_EVENT_MASK) != 0 ||
                    Toolkit.enabledOnToolkit(AWTEvent.COMPONENT_EVENT_MASK)) {
                    ComponentEvent e = new ComponentEvent(this,
                                                          ComponentEvent.COMPONENT_SHOWN);
                    Toolkit.getEventQueue().postEvent(e);
                }
            }
            Container parent = this.parent;
            if (parent != null) {
                parent.invalidate();
            }
        }
    }

    /**
     * Makes this component visible or invisible.
     *
     * @param  b {@code true} to make this component visible;
     *         otherwise {@code false}
     *
     * @deprecated As of JDK version 1.1,
     * replaced by {@code setVisible(boolean)}.
     */
    @Deprecated
    public void show(boolean b) {
        if (b) {
            show();
        } else {
            hide();
        }
    }

    boolean containsFocus() {
        return isFocusOwner();
    }

    void clearMostRecentFocusOwnerOnHide() {
        KeyboardFocusManager.clearMostRecentFocusOwner(this);
    }

    void clearCurrentFocusCycleRootOnHide() {
        /* do nothing */
    }

    /**
     * @deprecated As of JDK version 1.1,
     * replaced by {@code setVisible(boolean)}.
     */
    @Deprecated
    public void hide() {
        isPacked = false;

        if (visible) {
            clearCurrentFocusCycleRootOnHide();
            clearMostRecentFocusOwnerOnHide();
            synchronized (getTreeLock()) {
                visible = false;
                mixOnHiding(isLightweight());
                if (containsFocus() && KeyboardFocusManager.isAutoFocusTransferEnabled()) {
                    transferFocus(true);
                }
                ComponentPeer peer = this.peer;
                if (peer != null) {
                    peer.setVisible(false);
                    createHierarchyEvents(HierarchyEvent.HIERARCHY_CHANGED,
                                          this, parent,
                                          HierarchyEvent.SHOWING_CHANGED,
                                          Toolkit.enabledOnToolkit(AWTEvent.HIERARCHY_EVENT_MASK));
                    if (peer instanceof LightweightPeer) {
                        repaint();
                    }
                    updateCursorImmediately();
                }
                if (componentListener != null ||
                    (eventMask & AWTEvent.COMPONENT_EVENT_MASK) != 0 ||
                    Toolkit.enabledOnToolkit(AWTEvent.COMPONENT_EVENT_MASK)) {
                    ComponentEvent e = new ComponentEvent(this,
                                                          ComponentEvent.COMPONENT_HIDDEN);
                    Toolkit.getEventQueue().postEvent(e);
                }
            }
            Container parent = this.parent;
            if (parent != null) {
                parent.invalidate();
            }
        }
    }

    /**
     * Gets the foreground color of this component.
     * @return this component's foreground color; if this component does
     * not have a foreground color, the foreground color of its parent
     * is returned
     * @see #setForeground
     * @since 1.0
     */
    @Transient
    public Color getForeground() {
        Color foreground = this.foreground;
        if (foreground != null) {
            return foreground;
        }
        Container parent = this.parent;
        return (parent != null) ? parent.getForeground() : null;
    }

    /**
     * Sets the foreground color of this component.
     * @param c the color to become this component's
     *          foreground color; if this parameter is {@code null}
     *          then this component will inherit
     *          the foreground color of its parent
     * @see #getForeground
     * @since 1.0
     */
    public void setForeground(Color c) {
        Color oldColor = foreground;
        ComponentPeer peer = this.peer;
        foreground = c;
        if (peer != null) {
            c = getForeground();
            if (c != null) {
                peer.setForeground(c);
            }
        }
        // This is a bound property, so report the change to
        // any registered listeners.  (Cheap if there are none.)
        firePropertyChange("foreground", oldColor, c);
    }

    /**
     * Returns whether the foreground color has been explicitly set for this
     * Component. If this method returns {@code false}, this Component is
     * inheriting its foreground color from an ancestor.
     *
     * @return {@code true} if the foreground color has been explicitly
     *         set for this Component; {@code false} otherwise.
     * @since 1.4
     */
    public boolean isForegroundSet() {
        return (foreground != null);
    }

    /**
     * Gets the background color of this component.
     * @return this component's background color; if this component does
     *          not have a background color,
     *          the background color of its parent is returned
     * @see #setBackground
     * @since 1.0
     */
    @Transient
    public Color getBackground() {
        Color background = this.background;
        if (background != null) {
            return background;
        }
        Container parent = this.parent;
        return (parent != null) ? parent.getBackground() : null;
    }

    /**
     * Sets the background color of this component.
     * <p>
     * The background color affects each component differently and the
     * parts of the component that are affected by the background color
     * may differ between operating systems.
     *
     * @param c the color to become this component's color;
     *          if this parameter is {@code null}, then this
     *          component will inherit the background color of its parent
     * @see #getBackground
     * @since 1.0
     */
    public void setBackground(Color c) {
        Color oldColor = background;
        ComponentPeer peer = this.peer;
        background = c;
        if (peer != null) {
            c = getBackground();
            if (c != null) {
                peer.setBackground(c);
            }
        }
        // This is a bound property, so report the change to
        // any registered listeners.  (Cheap if there are none.)
        firePropertyChange("background", oldColor, c);
    }

    /**
     * Returns whether the background color has been explicitly set for this
     * Component. If this method returns {@code false}, this Component is
     * inheriting its background color from an ancestor.
     *
     * @return {@code true} if the background color has been explicitly
     *         set for this Component; {@code false} otherwise.
     * @since 1.4
     */
    public boolean isBackgroundSet() {
        return (background != null);
    }

    /**
     * Gets the font of this component.
     * @return this component's font; if a font has not been set
     * for this component, the font of its parent is returned
     * @see #setFont
     * @since 1.0
     */
    @Transient
    public Font getFont() {
        return getFont_NoClientCode();
    }

    // NOTE: This method may be called by privileged threads.
    //       This functionality is implemented in a package-private method
    //       to insure that it cannot be overridden by client subclasses.
    //       DO NOT INVOKE CLIENT CODE ON THIS THREAD!
    final Font getFont_NoClientCode() {
        Font font = this.font;
        if (font != null) {
            return font;
        }
        Container parent = this.parent;
        return (parent != null) ? parent.getFont_NoClientCode() : null;
    }

    /**
     * Sets the font of this component.
     * <p>
     * This method changes layout-related information, and therefore,
     * invalidates the component hierarchy.
     *
     * @param f the font to become this component's font;
     *          if this parameter is {@code null} then this
     *          component will inherit the font of its parent
     * @see #getFont
     * @see #invalidate
     * @since 1.0
     */
    public void setFont(Font f) {
        Font oldFont, newFont;
        synchronized(getTreeLock()) {
            oldFont = font;
            newFont = font = f;
            ComponentPeer peer = this.peer;
            if (peer != null) {
                f = getFont();
                if (f != null) {
                    peer.setFont(f);
                    peerFont = f;
                }
            }
        }
        // This is a bound property, so report the change to
        // any registered listeners.  (Cheap if there are none.)
        firePropertyChange("font", oldFont, newFont);

        // This could change the preferred size of the Component.
        // Fix for 6213660. Should compare old and new fonts and do not
        // call invalidate() if they are equal.
        if (f != oldFont && (oldFont == null ||
                                      !oldFont.equals(f))) {
            invalidateIfValid();
        }
    }

    /**
     * Returns whether the font has been explicitly set for this Component. If
     * this method returns {@code false}, this Component is inheriting its
     * font from an ancestor.
     *
     * @return {@code true} if the font has been explicitly set for this
     *         Component; {@code false} otherwise.
     * @since 1.4
     */
    public boolean isFontSet() {
        return (font != null);
    }

    /**
     * Gets the locale of this component.
     * @return this component's locale; if this component does not
     *          have a locale, the locale of its parent is returned
     * @see #setLocale
     * @exception IllegalComponentStateException if the {@code Component}
     *          does not have its own locale and has not yet been added to
     *          a containment hierarchy such that the locale can be determined
     *          from the containing parent
     * @since  1.1
     */
    public Locale getLocale() {
        Locale locale = this.locale;
        if (locale != null) {
            return locale;
        }
        Container parent = this.parent;

        if (parent == null) {
            throw new IllegalComponentStateException("This component must have a parent in order to determine its locale");
        } else {
            return parent.getLocale();
        }
    }

    /**
     * Sets the locale of this component.  This is a bound property.
     * <p>
     * This method changes layout-related information, and therefore,
     * invalidates the component hierarchy.
     *
     * @param l the locale to become this component's locale
     * @see #getLocale
     * @see #invalidate
     * @since 1.1
     */
    public void setLocale(Locale l) {
        Locale oldValue = locale;
        locale = l;

        // This is a bound property, so report the change to
        // any registered listeners.  (Cheap if there are none.)
        firePropertyChange("locale", oldValue, l);

        // This could change the preferred size of the Component.
        invalidateIfValid();
    }

    /**
     * Gets the instance of {@code ColorModel} used to display
     * the component on the output device.
     * @return the color model used by this component
     * @see java.awt.image.ColorModel
     * @see java.awt.peer.ComponentPeer#getColorModel()
     * @see Toolkit#getColorModel()
     * @since 1.0
     */
    public ColorModel getColorModel() {
        ComponentPeer peer = this.peer;
        if ((peer != null) && ! (peer instanceof LightweightPeer)) {
            return peer.getColorModel();
        } else if (GraphicsEnvironment.isHeadless()) {
            return ColorModel.getRGBdefault();
        } // else
        return getToolkit().getColorModel();
    }

    /**
     * Gets the location of this component in the form of a
     * point specifying the component's top-left corner.
     * The location will be relative to the parent's coordinate space.
     * <p>
     * Due to the asynchronous nature of native event handling, this
     * method can return outdated values (for instance, after several calls
     * of {@code setLocation()} in rapid succession).  For this
     * reason, the recommended method of obtaining a component's position is
     * within {@code java.awt.event.ComponentListener.componentMoved()},
     * which is called after the operating system has finished moving the
     * component.
     * </p>
     * @return an instance of {@code Point} representing
     *          the top-left corner of the component's bounds in
     *          the coordinate space of the component's parent
     * @see #setLocation
     * @see #getLocationOnScreen
     * @since 1.1
     */
    public Point getLocation() {
        return location();
    }

    /**
     * Gets the location of this component in the form of a point
     * specifying the component's top-left corner in the screen's
     * coordinate space.
     * @return an instance of {@code Point} representing
     *          the top-left corner of the component's bounds in the
     *          coordinate space of the screen
     * @throws IllegalComponentStateException if the
     *          component is not showing on the screen
     * @see #setLocation
     * @see #getLocation
     */
    public Point getLocationOnScreen() {
        synchronized (getTreeLock()) {
            return getLocationOnScreen_NoTreeLock();
        }
    }

    /*
     * a package private version of getLocationOnScreen
     * used by GlobalCursormanager to update cursor
     */
    final Point getLocationOnScreen_NoTreeLock() {
        ComponentPeer peer = this.peer;
        if (peer != null && isShowing()) {
            if (peer instanceof LightweightPeer) {
                // lightweight component location needs to be translated
                // relative to a native component.
                Container host = getNativeContainer();
                Point pt = host.peer.getLocationOnScreen();
                for(Component c = this; c != host; c = c.getContainer()) {
                    pt.x += c.x;
                    pt.y += c.y;
                }
                return pt;
            } else {
                Point pt = peer.getLocationOnScreen();
                return pt;
            }
        } else {
            throw new IllegalComponentStateException("component must be showing on the screen to determine its location");
        }
    }


    /**
     * Returns the location of this component's top left corner.
     *
     * @return the location of this component's top left corner
     * @deprecated As of JDK version 1.1,
     * replaced by {@code getLocation()}.
     */
    @Deprecated
    public Point location() {
        return location_NoClientCode();
    }

    private Point location_NoClientCode() {
        return new Point(x, y);
    }

    /**
     * Moves this component to a new location. The top-left corner of
     * the new location is specified by the {@code x} and {@code y}
     * parameters in the coordinate space of this component's parent.
     * <p>
     * This method changes layout-related information, and therefore,
     * invalidates the component hierarchy.
     *
     * @param x the <i>x</i>-coordinate of the new location's
     *          top-left corner in the parent's coordinate space
     * @param y the <i>y</i>-coordinate of the new location's
     *          top-left corner in the parent's coordinate space
     * @see #getLocation
     * @see #setBounds
     * @see #invalidate
     * @since 1.1
     */
    public void setLocation(int x, int y) {
        move(x, y);
    }

    /**
     * Moves this component to a new location.
     *
     * @param  x the <i>x</i>-coordinate of the new location's
     *           top-left corner in the parent's coordinate space
     * @param  y the <i>y</i>-coordinate of the new location's
     *           top-left corner in the parent's coordinate space
     *
     * @deprecated As of JDK version 1.1,
     * replaced by {@code setLocation(int, int)}.
     */
    @Deprecated
    public void move(int x, int y) {
        synchronized(getTreeLock()) {
            setBoundsOp(ComponentPeer.SET_LOCATION);
            setBounds(x, y, width, height);
        }
    }

    /**
     * Moves this component to a new location. The top-left corner of
     * the new location is specified by point {@code p}. Point
     * {@code p} is given in the parent's coordinate space.
     * <p>
     * This method changes layout-related information, and therefore,
     * invalidates the component hierarchy.
     *
     * @param p the point defining the top-left corner
     *          of the new location, given in the coordinate space of this
     *          component's parent
     * @see #getLocation
     * @see #setBounds
     * @see #invalidate
     * @since 1.1
     */
    public void setLocation(Point p) {
        setLocation(p.x, p.y);
    }

    /**
     * Returns the size of this component in the form of a
     * {@code Dimension} object. The {@code height}
     * field of the {@code Dimension} object contains
     * this component's height, and the {@code width}
     * field of the {@code Dimension} object contains
     * this component's width.
     * @return a {@code Dimension} object that indicates the
     *          size of this component
     * @see #setSize
     * @since 1.1
     */
    public Dimension getSize() {
        return size();
    }

    /**
     * Returns the size of this component in the form of a
     * {@code Dimension} object.
     *
     * @return the {@code Dimension} object that indicates the
     *         size of this component
     * @deprecated As of JDK version 1.1,
     * replaced by {@code getSize()}.
     */
    @Deprecated
    public Dimension size() {
        return new Dimension(width, height);
    }

    /**
     * Resizes this component so that it has width {@code width}
     * and height {@code height}.
     * <p>
     * This method changes layout-related information, and therefore,
     * invalidates the component hierarchy.
     *
     * @param width the new width of this component in pixels
     * @param height the new height of this component in pixels
     * @see #getSize
     * @see #setBounds
     * @see #invalidate
     * @since 1.1
     */
    public void setSize(int width, int height) {
        resize(width, height);
    }

    /**
     * Resizes this component.
     *
     * @param  width the new width of the component
     * @param  height the new height of the component
     * @deprecated As of JDK version 1.1,
     * replaced by {@code setSize(int, int)}.
     */
    @Deprecated
    public void resize(int width, int height) {
        synchronized(getTreeLock()) {
            setBoundsOp(ComponentPeer.SET_SIZE);
            setBounds(x, y, width, height);
        }
    }

    /**
     * Resizes this component so that it has width {@code d.width}
     * and height {@code d.height}.
     * <p>
     * This method changes layout-related information, and therefore,
     * invalidates the component hierarchy.
     *
     * @param d the dimension specifying the new size
     *          of this component
     * @throws NullPointerException if {@code d} is {@code null}
     * @see #setSize
     * @see #setBounds
     * @see #invalidate
     * @since 1.1
     */
    public void setSize(Dimension d) {
        resize(d);
    }

    /**
     * Resizes this component so that it has width {@code d.width}
     * and height {@code d.height}.
     *
     * @param  d the new size of this component
     * @deprecated As of JDK version 1.1,
     * replaced by {@code setSize(Dimension)}.
     */
    @Deprecated
    public void resize(Dimension d) {
        setSize(d.width, d.height);
    }

    /**
     * Gets the bounds of this component in the form of a
     * {@code Rectangle} object. The bounds specify this
     * component's width, height, and location relative to
     * its parent.
     * @return a rectangle indicating this component's bounds
     * @see #setBounds
     * @see #getLocation
     * @see #getSize
     */
    public Rectangle getBounds() {
        return bounds();
    }

    /**
     * Returns the bounding rectangle of this component.
     *
     * @return the bounding rectangle for this component
     * @deprecated As of JDK version 1.1,
     * replaced by {@code getBounds()}.
     */
    @Deprecated
    public Rectangle bounds() {
        return new Rectangle(x, y, width, height);
    }

    /**
     * Moves and resizes this component. The new location of the top-left
     * corner is specified by {@code x} and {@code y}, and the
     * new size is specified by {@code width} and {@code height}.
     * <p>
     * This method changes layout-related information, and therefore,
     * invalidates the component hierarchy.
     *
     * @param x the new <i>x</i>-coordinate of this component
     * @param y the new <i>y</i>-coordinate of this component
     * @param width the new {@code width} of this component
     * @param height the new {@code height} of this
     *          component
     * @see #getBounds
     * @see #setLocation(int, int)
     * @see #setLocation(Point)
     * @see #setSize(int, int)
     * @see #setSize(Dimension)
     * @see #invalidate
     * @since 1.1
     */
    public void setBounds(int x, int y, int width, int height) {
        reshape(x, y, width, height);
    }

    /**
     * Reshapes the bounding rectangle for this component.
     *
     * @param  x the <i>x</i> coordinate of the upper left corner of the rectangle
     * @param  y the <i>y</i> coordinate of the upper left corner of the rectangle
     * @param  width the width of the rectangle
     * @param  height the height of the rectangle
     *
     * @deprecated As of JDK version 1.1,
     * replaced by {@code setBounds(int, int, int, int)}.
     */
    @Deprecated
    public void reshape(int x, int y, int width, int height) {
        synchronized (getTreeLock()) {
            try {
                setBoundsOp(ComponentPeer.SET_BOUNDS);
                boolean resized = (this.width != width) || (this.height != height);
                boolean moved = (this.x != x) || (this.y != y);
                if (!resized && !moved) {
                    return;
                }
                int oldX = this.x;
                int oldY = this.y;
                int oldWidth = this.width;
                int oldHeight = this.height;
                this.x = x;
                this.y = y;
                this.width = width;
                this.height = height;

                if (resized) {
                    isPacked = false;
                }

                boolean needNotify = true;
                mixOnReshaping();
                if (peer != null) {
                    // LightweightPeer is an empty stub so can skip peer.reshape
                    if (!(peer instanceof LightweightPeer)) {
                        reshapeNativePeer(x, y, width, height, getBoundsOp());
                        // Check peer actually changed coordinates
                        resized = (oldWidth != this.width) || (oldHeight != this.height);
                        moved = (oldX != this.x) || (oldY != this.y);
                        // fix for 5025858: do not send ComponentEvents for toplevel
                        // windows here as it is done from peer or native code when
                        // the window is really resized or moved, otherwise some
                        // events may be sent twice
                        if (this instanceof Window) {
                            needNotify = false;
                        }
                    }
                    if (resized) {
                        invalidate();
                    }
                    if (parent != null) {
                        parent.invalidateIfValid();
                    }
                }
                if (needNotify) {
                    notifyNewBounds(resized, moved);
                }
                repaintParentIfNeeded(oldX, oldY, oldWidth, oldHeight);
            } finally {
                setBoundsOp(ComponentPeer.RESET_OPERATION);
            }
        }
    }

    private void repaintParentIfNeeded(int oldX, int oldY, int oldWidth,
                                       int oldHeight)
    {
        if (parent != null && peer instanceof LightweightPeer && isShowing()) {
            // Have the parent redraw the area this component occupied.
            parent.repaint(oldX, oldY, oldWidth, oldHeight);
            // Have the parent redraw the area this component *now* occupies.
            repaint();
        }
    }

    private void reshapeNativePeer(int x, int y, int width, int height, int op) {
        // native peer might be offset by more than direct
        // parent since parent might be lightweight.
        int nativeX = x;
        int nativeY = y;
        for (Component c = parent;
             (c != null) && (c.peer instanceof LightweightPeer);
             c = c.parent)
        {
            nativeX += c.x;
            nativeY += c.y;
        }
        peer.setBounds(nativeX, nativeY, width, height, op);
    }

    @SuppressWarnings("deprecation")
    private void notifyNewBounds(boolean resized, boolean moved) {
        if (componentListener != null
            || (eventMask & AWTEvent.COMPONENT_EVENT_MASK) != 0
            || Toolkit.enabledOnToolkit(AWTEvent.COMPONENT_EVENT_MASK))
            {
                if (resized) {
                    ComponentEvent e = new ComponentEvent(this,
                                                          ComponentEvent.COMPONENT_RESIZED);
                    Toolkit.getEventQueue().postEvent(e);
                }
                if (moved) {
                    ComponentEvent e = new ComponentEvent(this,
                                                          ComponentEvent.COMPONENT_MOVED);
                    Toolkit.getEventQueue().postEvent(e);
                }
            } else {
                if (this instanceof Container && ((Container)this).countComponents() > 0) {
                    boolean enabledOnToolkit =
                        Toolkit.enabledOnToolkit(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK);
                    if (resized) {

                        ((Container)this).createChildHierarchyEvents(
                                                                     HierarchyEvent.ANCESTOR_RESIZED, 0, enabledOnToolkit);
                    }
                    if (moved) {
                        ((Container)this).createChildHierarchyEvents(
                                                                     HierarchyEvent.ANCESTOR_MOVED, 0, enabledOnToolkit);
                    }
                }
                }
    }

    /**
     * Moves and resizes this component to conform to the new
     * bounding rectangle {@code r}. This component's new
     * position is specified by {@code r.x} and {@code r.y},
     * and its new size is specified by {@code r.width} and
     * {@code r.height}
     * <p>
     * This method changes layout-related information, and therefore,
     * invalidates the component hierarchy.
     *
     * @param r the new bounding rectangle for this component
     * @throws NullPointerException if {@code r} is {@code null}
     * @see       #getBounds
     * @see       #setLocation(int, int)
     * @see       #setLocation(Point)
     * @see       #setSize(int, int)
     * @see       #setSize(Dimension)
     * @see #invalidate
     * @since     1.1
     */
    public void setBounds(Rectangle r) {
        setBounds(r.x, r.y, r.width, r.height);
    }


    /**
     * Returns the current x coordinate of the components origin.
     * This method is preferable to writing
     * {@code component.getBounds().x},
     * or {@code component.getLocation().x} because it doesn't
     * cause any heap allocations.
     *
     * @return the current x coordinate of the components origin
     * @since 1.2
     */
    public int getX() {
        return x;
    }


    /**
     * Returns the current y coordinate of the components origin.
     * This method is preferable to writing
     * {@code component.getBounds().y},
     * or {@code component.getLocation().y} because it
     * doesn't cause any heap allocations.
     *
     * @return the current y coordinate of the components origin
     * @since 1.2
     */
    public int getY() {
        return y;
    }


    /**
     * Returns the current width of this component.
     * This method is preferable to writing
     * {@code component.getBounds().width},
     * or {@code component.getSize().width} because it
     * doesn't cause any heap allocations.
     *
     * @return the current width of this component
     * @since 1.2
     */
    public int getWidth() {
        return width;
    }


    /**
     * Returns the current height of this component.
     * This method is preferable to writing
     * {@code component.getBounds().height},
     * or {@code component.getSize().height} because it
     * doesn't cause any heap allocations.
     *
     * @return the current height of this component
     * @since 1.2
     */
    public int getHeight() {
        return height;
    }

    /**
     * Stores the bounds of this component into "return value" <b>rv</b> and
     * return <b>rv</b>.  If rv is {@code null} a new
     * {@code Rectangle} is allocated.
     * This version of {@code getBounds} is useful if the caller
     * wants to avoid allocating a new {@code Rectangle} object
     * on the heap.
     *
     * @param rv the return value, modified to the components bounds
     * @return rv
     */
    public Rectangle getBounds(Rectangle rv) {
        if (rv == null) {
            return new Rectangle(getX(), getY(), getWidth(), getHeight());
        }
        else {
            rv.setBounds(getX(), getY(), getWidth(), getHeight());
            return rv;
        }
    }

    /**
     * Stores the width/height of this component into "return value" <b>rv</b>
     * and return <b>rv</b>.   If rv is {@code null} a new
     * {@code Dimension} object is allocated.  This version of
     * {@code getSize} is useful if the caller wants to avoid
     * allocating a new {@code Dimension} object on the heap.
     *
     * @param rv the return value, modified to the components size
     * @return rv
     */
    public Dimension getSize(Dimension rv) {
        if (rv == null) {
            return new Dimension(getWidth(), getHeight());
        }
        else {
            rv.setSize(getWidth(), getHeight());
            return rv;
        }
    }

    /**
     * Stores the x,y origin of this component into "return value" <b>rv</b>
     * and return <b>rv</b>.   If rv is {@code null} a new
     * {@code Point} is allocated.
     * This version of {@code getLocation} is useful if the
     * caller wants to avoid allocating a new {@code Point}
     * object on the heap.
     *
     * @param rv the return value, modified to the components location
     * @return rv
     */
    public Point getLocation(Point rv) {
        if (rv == null) {
            return new Point(getX(), getY());
        }
        else {
            rv.setLocation(getX(), getY());
            return rv;
        }
    }

    /**
     * Returns true if this component is completely opaque, returns
     * false by default.
     * <p>
     * An opaque component paints every pixel within its
     * rectangular region. A non-opaque component paints only some of
     * its pixels, allowing the pixels underneath it to "show through".
     * A component that does not fully paint its pixels therefore
     * provides a degree of transparency.
     * <p>
     * Subclasses that guarantee to always completely paint their
     * contents should override this method and return true.
     *
     * @return true if this component is completely opaque
     * @see #isLightweight
     * @since 1.2
     */
    public boolean isOpaque() {
        if (peer == null) {
            return false;
        }
        else {
            return !isLightweight();
        }
    }


    /**
     * A lightweight component doesn't have a native toolkit peer.
     * Subclasses of {@code Component} and {@code Container},
     * other than the ones defined in this package like {@code Button}
     * or {@code Scrollbar}, are lightweight.
     * All of the Swing components are lightweights.
     * <p>
     * This method will always return {@code false} if this component
     * is not displayable because it is impossible to determine the
     * weight of an undisplayable component.
     *
     * @return true if this component has a lightweight peer; false if
     *         it has a native peer or no peer
     * @see #isDisplayable
     * @since 1.2
     */
    public boolean isLightweight() {
        return peer instanceof LightweightPeer;
    }


    /**
     * Sets the preferred size of this component to a constant
     * value.  Subsequent calls to {@code getPreferredSize} will always
     * return this value.  Setting the preferred size to {@code null}
     * restores the default behavior.
     *
     * @param preferredSize The new preferred size, or null
     * @see #getPreferredSize
     * @see #isPreferredSizeSet
     * @since 1.5
     */
    public void setPreferredSize(Dimension preferredSize) {
        Dimension old;
        // If the preferred size was set, use it as the old value, otherwise
        // use null to indicate we didn't previously have a set preferred
        // size.
        if (prefSizeSet) {
            old = this.prefSize;
        }
        else {
            old = null;
        }
        this.prefSize = preferredSize;
        prefSizeSet = (preferredSize != null);
        firePropertyChange("preferredSize", old, preferredSize);
    }


    /**
     * Returns true if the preferred size has been set to a
     * non-{@code null} value otherwise returns false.
     *
     * @return true if {@code setPreferredSize} has been invoked
     *         with a non-null value.
     * @since 1.5
     */
    public boolean isPreferredSizeSet() {
        return prefSizeSet;
    }


    /**
     * Gets the preferred size of this component.
     * @return a dimension object indicating this component's preferred size
     * @see #getMinimumSize
     * @see LayoutManager
     */
    public Dimension getPreferredSize() {
        return preferredSize();
    }


    /**
     * Returns the component's preferred size.
     *
     * @return the component's preferred size
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
                prefSize = (peer != null) ?
                    peer.getPreferredSize() :
                    getMinimumSize();
                dim = prefSize;
            }
        }
        return new Dimension(dim);
    }

    /**
     * Sets the minimum size of this component to a constant
     * value.  Subsequent calls to {@code getMinimumSize} will always
     * return this value.  Setting the minimum size to {@code null}
     * restores the default behavior.
     *
     * @param minimumSize the new minimum size of this component
     * @see #getMinimumSize
     * @see #isMinimumSizeSet
     * @since 1.5
     */
    public void setMinimumSize(Dimension minimumSize) {
        Dimension old;
        // If the minimum size was set, use it as the old value, otherwise
        // use null to indicate we didn't previously have a set minimum
        // size.
        if (minSizeSet) {
            old = this.minSize;
        }
        else {
            old = null;
        }
        this.minSize = minimumSize;
        minSizeSet = (minimumSize != null);
        firePropertyChange("minimumSize", old, minimumSize);
    }

    /**
     * Returns whether or not {@code setMinimumSize} has been
     * invoked with a non-null value.
     *
     * @return true if {@code setMinimumSize} has been invoked with a
     *              non-null value.
     * @since 1.5
     */
    public boolean isMinimumSizeSet() {
        return minSizeSet;
    }

    /**
     * Gets the minimum size of this component.
     * @return a dimension object indicating this component's minimum size
     * @see #getPreferredSize
     * @see LayoutManager
     */
    public Dimension getMinimumSize() {
        return minimumSize();
    }

    /**
     * Returns the minimum size of this component.
     *
     * @return the minimum size of this component
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
                minSize = (peer != null) ?
                    peer.getMinimumSize() :
                    size();
                dim = minSize;
            }
        }
        return new Dimension(dim);
    }

    /**
     * Sets the maximum size of this component to a constant
     * value.  Subsequent calls to {@code getMaximumSize} will always
     * return this value.  Setting the maximum size to {@code null}
     * restores the default behavior.
     *
     * @param maximumSize a {@code Dimension} containing the
     *          desired maximum allowable size
     * @see #getMaximumSize
     * @see #isMaximumSizeSet
     * @since 1.5
     */
    public void setMaximumSize(Dimension maximumSize) {
        // If the maximum size was set, use it as the old value, otherwise
        // use null to indicate we didn't previously have a set maximum
        // size.
        Dimension old;
        if (maxSizeSet) {
            old = this.maxSize;
        }
        else {
            old = null;
        }
        this.maxSize = maximumSize;
        maxSizeSet = (maximumSize != null);
        firePropertyChange("maximumSize", old, maximumSize);
    }

    /**
     * Returns true if the maximum size has been set to a non-{@code null}
     * value otherwise returns false.
     *
     * @return true if {@code maximumSize} is non-{@code null},
     *          false otherwise
     * @since 1.5
     */
    public boolean isMaximumSizeSet() {
        return maxSizeSet;
    }

    /**
     * Gets the maximum size of this component.
     * @return a dimension object indicating this component's maximum size
     * @see #getMinimumSize
     * @see #getPreferredSize
     * @see LayoutManager
     */
    public Dimension getMaximumSize() {
        if (isMaximumSizeSet()) {
            return new Dimension(maxSize);
        }
        return new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
    }

    /**
     * Returns the alignment along the x axis.  This specifies how
     * the component would like to be aligned relative to other
     * components.  The value should be a number between 0 and 1
     * where 0 represents alignment along the origin, 1 is aligned
     * the furthest away from the origin, 0.5 is centered, etc.
     *
     * @return the horizontal alignment of this component
     */
    public float getAlignmentX() {
        return CENTER_ALIGNMENT;
    }

    /**
     * Returns the alignment along the y axis.  This specifies how
     * the component would like to be aligned relative to other
     * components.  The value should be a number between 0 and 1
     * where 0 represents alignment along the origin, 1 is aligned
     * the furthest away from the origin, 0.5 is centered, etc.
     *
     * @return the vertical alignment of this component
     */
    public float getAlignmentY() {
        return CENTER_ALIGNMENT;
    }

    /**
     * Returns the baseline.  The baseline is measured from the top of
     * the component.  This method is primarily meant for
     * {@code LayoutManager}s to align components along their
     * baseline.  A return value less than 0 indicates this component
     * does not have a reasonable baseline and that
     * {@code LayoutManager}s should not align this component on
     * its baseline.
     * <p>
     * The default implementation returns -1.  Subclasses that support
     * baseline should override appropriately.  If a value &gt;= 0 is
     * returned, then the component has a valid baseline for any
     * size &gt;= the minimum size and {@code getBaselineResizeBehavior}
     * can be used to determine how the baseline changes with size.
     *
     * @param width the width to get the baseline for
     * @param height the height to get the baseline for
     * @return the baseline or &lt; 0 indicating there is no reasonable
     *         baseline
     * @throws IllegalArgumentException if width or height is &lt; 0
     * @see #getBaselineResizeBehavior
     * @see java.awt.FontMetrics
     * @since 1.6
     */
    public int getBaseline(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException(
                    "Width and height must be >= 0");
        }
        return -1;
    }

    /**
     * Returns an enum indicating how the baseline of the component
     * changes as the size changes.  This method is primarily meant for
     * layout managers and GUI builders.
     * <p>
     * The default implementation returns
     * {@code BaselineResizeBehavior.OTHER}.  Subclasses that have a
     * baseline should override appropriately.  Subclasses should
     * never return {@code null}; if the baseline can not be
     * calculated return {@code BaselineResizeBehavior.OTHER}.  Callers
     * should first ask for the baseline using
     * {@code getBaseline} and if a value &gt;= 0 is returned use
     * this method.  It is acceptable for this method to return a
     * value other than {@code BaselineResizeBehavior.OTHER} even if
     * {@code getBaseline} returns a value less than 0.
     *
     * @return an enum indicating how the baseline changes as the component
     *         size changes
     * @see #getBaseline(int, int)
     * @since 1.6
     */
    public BaselineResizeBehavior getBaselineResizeBehavior() {
        return BaselineResizeBehavior.OTHER;
    }

    /**
     * Prompts the layout manager to lay out this component. This is
     * usually called when the component (more specifically, container)
     * is validated.
     * @see #validate
     * @see LayoutManager
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
    }

    /**
     * Validates this component.
     * <p>
     * The meaning of the term <i>validating</i> is defined by the ancestors of
     * this class. See {@link Container#validate} for more details.
     *
     * @see       #invalidate
     * @see       #doLayout()
     * @see       LayoutManager
     * @see       Container#validate
     * @since     1.0
     */
    public void validate() {
        synchronized (getTreeLock()) {
            ComponentPeer peer = this.peer;
            boolean wasValid = isValid();
            if (!wasValid && peer != null) {
                Font newfont = getFont();
                Font oldfont = peerFont;
                if (newfont != null && !Objects.equals(oldfont, newfont)) {
                    peer.setFont(newfont);
                    peerFont = newfont;
                }
                peer.layout();
            }
            valid = true;
            if (!wasValid) {
                mixOnValidating();
            }
        }
    }

    /**
     * Invalidates this component and its ancestors.
     * <p>
     * By default, all the ancestors of the component up to the top-most
     * container of the hierarchy are marked invalid. If the {@code
     * java.awt.smartInvalidate} system property is set to {@code true},
     * invalidation stops on the nearest validate root of this component.
     * Marking a container <i>invalid</i> indicates that the container needs to
     * be laid out.
     * <p>
     * This method is called automatically when any layout-related information
     * changes (e.g. setting the bounds of the component, or adding the
     * component to a container).
     * <p>
     * This method might be called often, so it should work fast.
     *
     * @see       #validate
     * @see       #doLayout
     * @see       LayoutManager
     * @see       java.awt.Container#isValidateRoot
     * @since     1.0
     */
    public void invalidate() {
        synchronized (getTreeLock()) {
            /* Nullify cached layout and size information.
             * For efficiency, propagate invalidate() upwards only if
             * some other component hasn't already done so first.
             */
            valid = false;
            if (!isPreferredSizeSet()) {
                prefSize = null;
            }
            if (!isMinimumSizeSet()) {
                minSize = null;
            }
            if (!isMaximumSizeSet()) {
                maxSize = null;
            }
            invalidateParent();
        }
    }

    /**
     * Invalidates the parent of this component if any.
     *
     * This method MUST BE invoked under the TreeLock.
     */
    void invalidateParent() {
        if (parent != null) {
            parent.invalidateIfValid();
        }
    }

    /** Invalidates the component unless it is already invalid.
     */
    final void invalidateIfValid() {
        if (isValid()) {
            invalidate();
        }
    }

    /**
     * Revalidates the component hierarchy up to the nearest validate root.
     * <p>
     * This method first invalidates the component hierarchy starting from this
     * component up to the nearest validate root. Afterwards, the component
     * hierarchy is validated starting from the nearest validate root.
     * <p>
     * This is a convenience method supposed to help application developers
     * avoid looking for validate roots manually. Basically, it's equivalent to
     * first calling the {@link #invalidate()} method on this component, and
     * then calling the {@link #validate()} method on the nearest validate
     * root.
     *
     * @see Container#isValidateRoot
     * @since 1.7
     */
    public void revalidate() {
        revalidateSynchronously();
    }

    /**
     * Revalidates the component synchronously.
     */
    final void revalidateSynchronously() {
        synchronized (getTreeLock()) {
            invalidate();

            Container root = getContainer();
            if (root == null) {
                // There's no parents. Just validate itself.
                validate();
            } else {
                while (!root.isValidateRoot()) {
                    if (root.getContainer() == null) {
                        // If there's no validate roots, we'll validate the
                        // topmost container
                        break;
                    }

                    root = root.getContainer();
                }

                root.validate();
            }
        }
    }

    /**
     * Creates a graphics context for this component. This method will
     * return {@code null} if this component is currently not
     * displayable.
     * @return a graphics context for this component, or {@code null}
     *             if it has none
     * @see       #paint
     * @since     1.0
     */
    public Graphics getGraphics() {
        if (peer instanceof LightweightPeer) {
            // This is for a lightweight component, need to
            // translate coordinate spaces and clip relative
            // to the parent.
            if (parent == null) return null;
            Graphics g = parent.getGraphics();
            if (g == null) return null;
            if (g instanceof ConstrainableGraphics) {
                ((ConstrainableGraphics) g).constrain(x, y, width, height);
            } else {
                g.translate(x,y);
                g.setClip(0, 0, width, height);
            }
            g.setFont(getFont());
            return g;
        } else {
            ComponentPeer peer = this.peer;
            return (peer != null) ? peer.getGraphics() : null;
        }
    }

    final Graphics getGraphics_NoClientCode() {
        ComponentPeer peer = this.peer;
        if (peer instanceof LightweightPeer) {
            // This is for a lightweight component, need to
            // translate coordinate spaces and clip relative
            // to the parent.
            Container parent = this.parent;
            if (parent == null) return null;
            Graphics g = parent.getGraphics_NoClientCode();
            if (g == null) return null;
            if (g instanceof ConstrainableGraphics) {
                ((ConstrainableGraphics) g).constrain(x, y, width, height);
            } else {
                g.translate(x,y);
                g.setClip(0, 0, width, height);
            }
            g.setFont(getFont_NoClientCode());
            return g;
        } else {
            return (peer != null) ? peer.getGraphics() : null;
        }
    }

    /**
     * Gets the font metrics for the specified font.
     * Warning: Since Font metrics are affected by the
     * {@link java.awt.font.FontRenderContext FontRenderContext} and
     * this method does not provide one, it can return only metrics for
     * the default render context which may not match that used when
     * rendering on the Component if {@link Graphics2D} functionality is being
     * used. Instead metrics can be obtained at rendering time by calling
     * {@link Graphics#getFontMetrics()} or text measurement APIs on the
     * {@link Font Font} class.
     * @param font the font for which font metrics is to be
     *          obtained
     * @return the font metrics for {@code font}
     * @see       #getFont
     * @see       java.awt.peer.ComponentPeer#getFontMetrics(Font)
     * @see       Toolkit#getFontMetrics(Font)
     * @since     1.0
     */
    public FontMetrics getFontMetrics(Font font) {
        // This is an unsupported hack, but left in for a customer.
        // Do not remove.
        FontManager fm = FontManagerFactory.getInstance();
        if (fm instanceof SunFontManager
            && ((SunFontManager) fm).usePlatformFontMetrics()) {

            if (peer != null &&
                !(peer instanceof LightweightPeer)) {
                return peer.getFontMetrics(font);
            }
        }
        return sun.font.FontDesignMetrics.getMetrics(font);
    }

    /**
     * Sets the cursor image to the specified cursor.  This cursor
     * image is displayed when the {@code contains} method for
     * this component returns true for the current cursor location, and
     * this Component is visible, displayable, and enabled. Setting the
     * cursor of a {@code Container} causes that cursor to be displayed
     * within all of the container's subcomponents, except for those
     * that have a non-{@code null} cursor.
     * <p>
     * The method may have no visual effect if the Java platform
     * implementation and/or the native system do not support
     * changing the mouse cursor shape.
     * @param cursor One of the constants defined
     *          by the {@code Cursor} class;
     *          if this parameter is {@code null}
     *          then this component will inherit
     *          the cursor of its parent
     * @see       #isEnabled
     * @see       #isShowing
     * @see       #getCursor
     * @see       #contains
     * @see       Toolkit#createCustomCursor
     * @see       Cursor
     * @since     1.1
     */
    public void setCursor(Cursor cursor) {
        this.cursor = cursor;
        updateCursorImmediately();
    }

    /**
     * Updates the cursor.  May not be invoked from the native
     * message pump.
     */
    final void updateCursorImmediately() {
        if (peer instanceof LightweightPeer) {
            Container nativeContainer = getNativeContainer();

            if (nativeContainer == null) return;

            ComponentPeer cPeer = nativeContainer.peer;

            if (cPeer != null) {
                cPeer.updateCursorImmediately();
            }
        } else if (peer != null) {
            peer.updateCursorImmediately();
        }
    }

    /**
     * Gets the cursor set in the component. If the component does
     * not have a cursor set, the cursor of its parent is returned.
     * If no cursor is set in the entire hierarchy,
     * {@code Cursor.DEFAULT_CURSOR} is returned.
     *
     * @return the cursor for this component
     * @see #setCursor
     * @since 1.1
     */
    public Cursor getCursor() {
        return getCursor_NoClientCode();
    }

    final Cursor getCursor_NoClientCode() {
        Cursor cursor = this.cursor;
        if (cursor != null) {
            return cursor;
        }
        Container parent = this.parent;
        if (parent != null) {
            return parent.getCursor_NoClientCode();
        } else {
            return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        }
    }

    /**
     * Returns whether the cursor has been explicitly set for this Component.
     * If this method returns {@code false}, this Component is inheriting
     * its cursor from an ancestor.
     *
     * @return {@code true} if the cursor has been explicitly set for this
     *         Component; {@code false} otherwise.
     * @since 1.4
     */
    public boolean isCursorSet() {
        return (cursor != null);
    }

    /**
     * Paints this component.
     * <p>
     * This method is called when the contents of the component should
     * be painted; such as when the component is first being shown or
     * is damaged and in need of repair.  The clip rectangle in the
     * {@code Graphics} parameter is set to the area
     * which needs to be painted.
     * Subclasses of {@code Component} that override this
     * method need not call {@code super.paint(g)}.
     * <p>
     * For performance reasons, {@code Component}s with zero width
     * or height aren't considered to need painting when they are first shown,
     * and also aren't considered to need repair.
     * <p>
     * <b>Note</b>: For more information on the paint mechanisms utilitized
     * by AWT and Swing, including information on how to write the most
     * efficient painting code, see
     * <a href="http://www.oracle.com/technetwork/java/painting-140037.html">Painting in AWT and Swing</a>.
     *
     * @param g the graphics context to use for painting
     * @see       #update
     * @since     1.0
     */
    public void paint(Graphics g) {
    }

    /**
     * Updates this component.
     * <p>
     * If this component is not a lightweight component, the
     * AWT calls the {@code update} method in response to
     * a call to {@code repaint}.  You can assume that
     * the background is not cleared.
     * <p>
     * The {@code update} method of {@code Component}
     * calls this component's {@code paint} method to redraw
     * this component.  This method is commonly overridden by subclasses
     * which need to do additional work in response to a call to
     * {@code repaint}.
     * Subclasses of Component that override this method should either
     * call {@code super.update(g)}, or call {@code paint(g)}
     * directly from their {@code update} method.
     * <p>
     * The origin of the graphics context, its
     * ({@code 0},&nbsp;{@code 0}) coordinate point, is the
     * top-left corner of this component. The clipping region of the
     * graphics context is the bounding rectangle of this component.
     *
     * <p>
     * <b>Note</b>: For more information on the paint mechanisms utilitized
     * by AWT and Swing, including information on how to write the most
     * efficient painting code, see
     * <a href="http://www.oracle.com/technetwork/java/painting-140037.html">Painting in AWT and Swing</a>.
     *
     * @param g the specified context to use for updating
     * @see       #paint
     * @see       #repaint()
     * @since     1.0
     */
    public void update(Graphics g) {
        paint(g);
    }

    /**
     * Paints this component and all of its subcomponents.
     * <p>
     * The origin of the graphics context, its
     * ({@code 0},&nbsp;{@code 0}) coordinate point, is the
     * top-left corner of this component. The clipping region of the
     * graphics context is the bounding rectangle of this component.
     *
     * @param     g   the graphics context to use for painting
     * @see       #paint
     * @since     1.0
     */
    public void paintAll(Graphics g) {
        if (isShowing()) {
            GraphicsCallback.PeerPaintCallback.getInstance().
                runOneComponent(this, new Rectangle(0, 0, width, height),
                                g, g.getClip(),
                                GraphicsCallback.LIGHTWEIGHTS |
                                GraphicsCallback.HEAVYWEIGHTS);
        }
    }

    /**
     * Simulates the peer callbacks into java.awt for painting of
     * lightweight Components.
     * @param     g   the graphics context to use for painting
     * @see       #paintAll
     */
    void lightweightPaint(Graphics g) {
        paint(g);
    }

    /**
     * Paints all the heavyweight subcomponents.
     */
    void paintHeavyweightComponents(Graphics g) {
    }

    /**
     * Repaints this component.
     * <p>
     * If this component is a lightweight component, this method
     * causes a call to this component's {@code paint}
     * method as soon as possible.  Otherwise, this method causes
     * a call to this component's {@code update} method as soon
     * as possible.
     * <p>
     * <b>Note</b>: For more information on the paint mechanisms utilitized
     * by AWT and Swing, including information on how to write the most
     * efficient painting code, see
     * <a href="http://www.oracle.com/technetwork/java/painting-140037.html">Painting in AWT and Swing</a>.

     *
     * @see       #update(Graphics)
     * @since     1.0
     */
    public void repaint() {
        repaint(0, 0, 0, width, height);
    }

    /**
     * Repaints the component.  If this component is a lightweight
     * component, this results in a call to {@code paint}
     * within {@code tm} milliseconds.
     * <p>
     * <b>Note</b>: For more information on the paint mechanisms utilitized
     * by AWT and Swing, including information on how to write the most
     * efficient painting code, see
     * <a href="http://www.oracle.com/technetwork/java/painting-140037.html">Painting in AWT and Swing</a>.
     *
     * @param tm maximum time in milliseconds before update
     * @see #paint
     * @see #update(Graphics)
     * @since 1.0
     */
    public void repaint(long tm) {
        repaint(tm, 0, 0, width, height);
    }

    /**
     * Repaints the specified rectangle of this component.
     * <p>
     * If this component is a lightweight component, this method
     * causes a call to this component's {@code paint} method
     * as soon as possible.  Otherwise, this method causes a call to
     * this component's {@code update} method as soon as possible.
     * <p>
     * <b>Note</b>: For more information on the paint mechanisms utilitized
     * by AWT and Swing, including information on how to write the most
     * efficient painting code, see
     * <a href="http://www.oracle.com/technetwork/java/painting-140037.html">Painting in AWT and Swing</a>.
     *
     * @param     x   the <i>x</i> coordinate
     * @param     y   the <i>y</i> coordinate
     * @param     width   the width
     * @param     height  the height
     * @see       #update(Graphics)
     * @since     1.0
     */
    public void repaint(int x, int y, int width, int height) {
        repaint(0, x, y, width, height);
    }

    /**
     * Repaints the specified rectangle of this component within
     * {@code tm} milliseconds.
     * <p>
     * If this component is a lightweight component, this method causes
     * a call to this component's {@code paint} method.
     * Otherwise, this method causes a call to this component's
     * {@code update} method.
     * <p>
     * <b>Note</b>: For more information on the paint mechanisms utilitized
     * by AWT and Swing, including information on how to write the most
     * efficient painting code, see
     * <a href="http://www.oracle.com/technetwork/java/painting-140037.html">Painting in AWT and Swing</a>.
     *
     * @param     tm   maximum time in milliseconds before update
     * @param     x    the <i>x</i> coordinate
     * @param     y    the <i>y</i> coordinate
     * @param     width    the width
     * @param     height   the height
     * @see       #update(Graphics)
     * @since     1.0
     */
    public void repaint(long tm, int x, int y, int width, int height) {
        if (this.peer instanceof LightweightPeer) {
            // Needs to be translated to parent coordinates since
            // a parent native container provides the actual repaint
            // services.  Additionally, the request is restricted to
            // the bounds of the component.
            if (parent != null) {
                if (x < 0) {
                    width += x;
                    x = 0;
                }
                if (y < 0) {
                    height += y;
                    y = 0;
                }

                int pwidth = (width > this.width) ? this.width : width;
                int pheight = (height > this.height) ? this.height : height;

                if (pwidth <= 0 || pheight <= 0) {
                    return;
                }

                int px = this.x + x;
                int py = this.y + y;
                parent.repaint(tm, px, py, pwidth, pheight);
            }
        } else {
            if (isVisible() && (this.peer != null) &&
                (width > 0) && (height > 0)) {
                PaintEvent e = new PaintEvent(this, PaintEvent.UPDATE,
                                              new Rectangle(x, y, width, height));
                SunToolkit.postEvent(SunToolkit.targetToAppContext(this), e);
            }
        }
    }

    /**
     * Prints this component. Applications should override this method
     * for components that must do special processing before being
     * printed or should be printed differently than they are painted.
     * <p>
     * The default implementation of this method calls the
     * {@code paint} method.
     * <p>
     * The origin of the graphics context, its
     * ({@code 0},&nbsp;{@code 0}) coordinate point, is the
     * top-left corner of this component. The clipping region of the
     * graphics context is the bounding rectangle of this component.
     * @param     g   the graphics context to use for printing
     * @see       #paint(Graphics)
     * @since     1.0
     */
    public void print(Graphics g) {
        paint(g);
    }

    /**
     * Prints this component and all of its subcomponents.
     * <p>
     * The origin of the graphics context, its
     * ({@code 0},&nbsp;{@code 0}) coordinate point, is the
     * top-left corner of this component. The clipping region of the
     * graphics context is the bounding rectangle of this component.
     * @param     g   the graphics context to use for printing
     * @see       #print(Graphics)
     * @since     1.0
     */
    public void printAll(Graphics g) {
        if (isShowing()) {
            GraphicsCallback.PeerPrintCallback.getInstance().
                runOneComponent(this, new Rectangle(0, 0, width, height),
                                g, g.getClip(),
                                GraphicsCallback.LIGHTWEIGHTS |
                                GraphicsCallback.HEAVYWEIGHTS);
        }
    }

    /**
     * Simulates the peer callbacks into java.awt for printing of
     * lightweight Components.
     * @param     g   the graphics context to use for printing
     * @see       #printAll
     */
    void lightweightPrint(Graphics g) {
        print(g);
    }

    /**
     * Prints all the heavyweight subcomponents.
     */
    void printHeavyweightComponents(Graphics g) {
    }

    private Insets getInsets_NoClientCode() {
        ComponentPeer peer = this.peer;
        if (peer instanceof ContainerPeer) {
            return (Insets)((ContainerPeer)peer).getInsets().clone();
        }
        return new Insets(0, 0, 0, 0);
    }

    /**
     * Repaints the component when the image has changed.
     * This {@code imageUpdate} method of an {@code ImageObserver}
     * is called when more information about an
     * image which had been previously requested using an asynchronous
     * routine such as the {@code drawImage} method of
     * {@code Graphics} becomes available.
     * See the definition of {@code imageUpdate} for
     * more information on this method and its arguments.
     * <p>
     * The {@code imageUpdate} method of {@code Component}
     * incrementally draws an image on the component as more of the bits
     * of the image are available.
     * <p>
     * If the system property {@code awt.image.incrementaldraw}
     * is missing or has the value {@code true}, the image is
     * incrementally drawn. If the system property has any other value,
     * then the image is not drawn until it has been completely loaded.
     * <p>
     * Also, if incremental drawing is in effect, the value of the
     * system property {@code awt.image.redrawrate} is interpreted
     * as an integer to give the maximum redraw rate, in milliseconds. If
     * the system property is missing or cannot be interpreted as an
     * integer, the redraw rate is once every 100ms.
     * <p>
     * The interpretation of the {@code x}, {@code y},
     * {@code width}, and {@code height} arguments depends on
     * the value of the {@code infoflags} argument.
     *
     * @param     img   the image being observed
     * @param     infoflags   see {@code imageUpdate} for more information
     * @param     x   the <i>x</i> coordinate
     * @param     y   the <i>y</i> coordinate
     * @param     w   the width
     * @param     h   the height
     * @return    {@code false} if the infoflags indicate that the
     *            image is completely loaded; {@code true} otherwise.
     *
     * @see     java.awt.image.ImageObserver
     * @see     Graphics#drawImage(Image, int, int, Color, java.awt.image.ImageObserver)
     * @see     Graphics#drawImage(Image, int, int, java.awt.image.ImageObserver)
     * @see     Graphics#drawImage(Image, int, int, int, int, Color, java.awt.image.ImageObserver)
     * @see     Graphics#drawImage(Image, int, int, int, int, java.awt.image.ImageObserver)
     * @see     java.awt.image.ImageObserver#imageUpdate(java.awt.Image, int, int, int, int, int)
     * @since   1.0
     */
    public boolean imageUpdate(Image img, int infoflags,
                               int x, int y, int w, int h) {
        int rate = -1;
        if ((infoflags & (FRAMEBITS|ALLBITS)) != 0) {
            rate = 0;
        } else if ((infoflags & SOMEBITS) != 0) {
            if (isInc) {
                rate = incRate;
                if (rate < 0) {
                    rate = 0;
                }
            }
        }
        if (rate >= 0) {
            repaint(rate, 0, 0, width, height);
        }
        return (infoflags & (ALLBITS|ABORT)) == 0;
    }

    /**
     * Creates an image from the specified image producer.
     * @param     producer  the image producer
     * @return    the image produced
     * @since     1.0
     */
    public Image createImage(ImageProducer producer) {
        return getToolkit().createImage(producer);
    }

    /**
     * Creates an off-screen drawable image to be used for double buffering.
     *
     * @param  width the specified width
     * @param  height the specified height
     * @return an off-screen drawable image, which can be used for double
     *         buffering. The {@code null} value if the component is not
     *         displayable or {@code GraphicsEnvironment.isHeadless()} returns
     *         {@code true}.
     * @see #isDisplayable
     * @see GraphicsEnvironment#isHeadless
     * @since 1.0
     */
    public Image createImage(int width, int height) {
        ComponentPeer peer = this.peer;
        if (peer instanceof LightweightPeer) {
            if (parent != null) { return parent.createImage(width, height); }
            else { return null;}
        } else {
            return (peer != null) ? peer.createImage(width, height) : null;
        }
    }

    /**
     * Creates a volatile off-screen drawable image to be used for double
     * buffering.
     *
     * @param  width the specified width
     * @param  height the specified height
     * @return an off-screen drawable image, which can be used for double
     *         buffering. The {@code null} value if the component is not
     *         displayable or {@code GraphicsEnvironment.isHeadless()} returns
     *         {@code true}.
     * @see java.awt.image.VolatileImage
     * @see #isDisplayable
     * @see GraphicsEnvironment#isHeadless
     * @since 1.4
     */
    public VolatileImage createVolatileImage(int width, int height) {
        ComponentPeer peer = this.peer;
        if (peer instanceof LightweightPeer) {
            if (parent != null) {
                return parent.createVolatileImage(width, height);
            }
            else { return null;}
        } else {
            return (peer != null) ?
                peer.createVolatileImage(width, height) : null;
        }
    }

    /**
     * Creates a volatile off-screen drawable image, with the given
     * capabilities. The contents of this image may be lost at any time due to
     * operating system issues, so the image must be managed via the
     * {@code VolatileImage} interface.
     *
     * @param  width the specified width
     * @param  height the specified height
     * @param  caps the image capabilities
     * @return a VolatileImage object, which can be used to manage surface
     *         contents loss and capabilities. The {@code null} value if the
     *         component is not displayable or
     *         {@code GraphicsEnvironment.isHeadless()} returns {@code true}.
     * @throws AWTException if an image with the specified capabilities cannot
     *         be created
     * @see java.awt.image.VolatileImage
     * @since 1.4
     */
    public VolatileImage createVolatileImage(int width, int height,
                                             ImageCapabilities caps)
            throws AWTException {
        // REMIND : check caps
        return createVolatileImage(width, height);
    }

    /**
     * Prepares an image for rendering on this component.  The image
     * data is downloaded asynchronously in another thread and the
     * appropriate screen representation of the image is generated.
     * @param     image   the {@code Image} for which to
     *                    prepare a screen representation
     * @param     observer   the {@code ImageObserver} object
     *                       to be notified as the image is being prepared
     * @return    {@code true} if the image has already been fully
     *           prepared; {@code false} otherwise
     * @since     1.0
     */
    public boolean prepareImage(Image image, ImageObserver observer) {
        return prepareImage(image, -1, -1, observer);
    }

    /**
     * Prepares an image for rendering on this component at the
     * specified width and height.
     * <p>
     * The image data is downloaded asynchronously in another thread,
     * and an appropriately scaled screen representation of the image is
     * generated.
     * @param     image    the instance of {@code Image}
     *            for which to prepare a screen representation
     * @param     width    the width of the desired screen representation
     * @param     height   the height of the desired screen representation
     * @param     observer   the {@code ImageObserver} object
     *            to be notified as the image is being prepared
     * @return    {@code true} if the image has already been fully
     *          prepared; {@code false} otherwise
     * @see       java.awt.image.ImageObserver
     * @since     1.0
     */
    public boolean prepareImage(Image image, int width, int height,
                                ImageObserver observer) {
        return getToolkit().prepareImage(image, width, height, observer);
    }

    /**
     * Returns the status of the construction of a screen representation
     * of the specified image.
     * <p>
     * This method does not cause the image to begin loading. An
     * application must use the {@code prepareImage} method
     * to force the loading of an image.
     * <p>
     * Information on the flags returned by this method can be found
     * with the discussion of the {@code ImageObserver} interface.
     * @param     image   the {@code Image} object whose status
     *            is being checked
     * @param     observer   the {@code ImageObserver}
     *            object to be notified as the image is being prepared
     * @return  the bitwise inclusive <b>OR</b> of
     *            {@code ImageObserver} flags indicating what
     *            information about the image is currently available
     * @see      #prepareImage(Image, int, int, java.awt.image.ImageObserver)
     * @see      Toolkit#checkImage(Image, int, int, java.awt.image.ImageObserver)
     * @see      java.awt.image.ImageObserver
     * @since    1.0
     */
    public int checkImage(Image image, ImageObserver observer) {
        return checkImage(image, -1, -1, observer);
    }

    /**
     * Returns the status of the construction of a screen representation
     * of the specified image.
     * <p>
     * This method does not cause the image to begin loading. An
     * application must use the {@code prepareImage} method
     * to force the loading of an image.
     * <p>
     * The {@code checkImage} method of {@code Component}
     * calls its peer's {@code checkImage} method to calculate
     * the flags. If this component does not yet have a peer, the
     * component's toolkit's {@code checkImage} method is called
     * instead.
     * <p>
     * Information on the flags returned by this method can be found
     * with the discussion of the {@code ImageObserver} interface.
     * @param     image   the {@code Image} object whose status
     *                    is being checked
     * @param     width   the width of the scaled version
     *                    whose status is to be checked
     * @param     height  the height of the scaled version
     *                    whose status is to be checked
     * @param     observer   the {@code ImageObserver} object
     *                    to be notified as the image is being prepared
     * @return    the bitwise inclusive <b>OR</b> of
     *            {@code ImageObserver} flags indicating what
     *            information about the image is currently available
     * @see      #prepareImage(Image, int, int, java.awt.image.ImageObserver)
     * @see      Toolkit#checkImage(Image, int, int, java.awt.image.ImageObserver)
     * @see      java.awt.image.ImageObserver
     * @since    1.0
     */
    public int checkImage(Image image, int width, int height,
                          ImageObserver observer) {
        return getToolkit().checkImage(image, width, height, observer);
    }

    /**
     * Creates a new strategy for multi-buffering on this component.
     * Multi-buffering is useful for rendering performance.  This method
     * attempts to create the best strategy available with the number of
     * buffers supplied.  It will always create a {@code BufferStrategy}
     * with that number of buffers.
     * A page-flipping strategy is attempted first, then a blitting strategy
     * using accelerated buffers.  Finally, an unaccelerated blitting
     * strategy is used.
     * <p>
     * Each time this method is called,
     * the existing buffer strategy for this component is discarded.
     * @param numBuffers number of buffers to create, including the front buffer
     * @exception IllegalArgumentException if numBuffers is less than 1.
     * @exception IllegalStateException if the component is not displayable
     * @see #isDisplayable
     * @see Window#getBufferStrategy()
     * @see Canvas#getBufferStrategy()
     * @since 1.4
     */
    void createBufferStrategy(int numBuffers) {
        BufferCapabilities bufferCaps;
        if (numBuffers > 1) {
            // Try to create a page-flipping strategy
            bufferCaps = new BufferCapabilities(new ImageCapabilities(true),
                                                new ImageCapabilities(true),
                                                BufferCapabilities.FlipContents.UNDEFINED);
            try {
                createBufferStrategy(numBuffers, bufferCaps);
                return; // Success
            } catch (AWTException e) {
                // Failed
            }
        }
        // Try a blitting (but still accelerated) strategy
        bufferCaps = new BufferCapabilities(new ImageCapabilities(true),
                                            new ImageCapabilities(true),
                                            null);
        try {
            createBufferStrategy(numBuffers, bufferCaps);
            return; // Success
        } catch (AWTException e) {
            // Failed
        }
        // Try an unaccelerated blitting strategy
        bufferCaps = new BufferCapabilities(new ImageCapabilities(false),
                                            new ImageCapabilities(false),
                                            null);
        try {
            createBufferStrategy(numBuffers, bufferCaps);
            return; // Success
        } catch (AWTException e) {
            // Code should never reach here (an unaccelerated blitting
            // strategy should always work)
            throw new InternalError("Could not create a buffer strategy", e);
        }
    }

    /**
     * Creates a new strategy for multi-buffering on this component with the
     * required buffer capabilities.  This is useful, for example, if only
     * accelerated memory or page flipping is desired (as specified by the
     * buffer capabilities).
     * <p>
     * Each time this method
     * is called, {@code dispose} will be invoked on the existing
     * {@code BufferStrategy}.
     * @param numBuffers number of buffers to create
     * @param caps the required capabilities for creating the buffer strategy;
     * cannot be {@code null}
     * @exception AWTException if the capabilities supplied could not be
     * supported or met; this may happen, for example, if there is not enough
     * accelerated memory currently available, or if page flipping is specified
     * but not possible.
     * @exception IllegalArgumentException if numBuffers is less than 1, or if
     * caps is {@code null}
     * @see Window#getBufferStrategy()
     * @see Canvas#getBufferStrategy()
     * @since 1.4
     */
    void createBufferStrategy(int numBuffers,
                              BufferCapabilities caps) throws AWTException {
        // Check arguments
        if (numBuffers < 1) {
            throw new IllegalArgumentException(
                "Number of buffers must be at least 1");
        }
        if (caps == null) {
            throw new IllegalArgumentException("No capabilities specified");
        }
        // Destroy old buffers
        if (bufferStrategy != null) {
            bufferStrategy.dispose();
        }
        if (numBuffers == 1) {
            bufferStrategy = new SingleBufferStrategy(caps);
        } else {
            SunGraphicsEnvironment sge = (SunGraphicsEnvironment)
                GraphicsEnvironment.getLocalGraphicsEnvironment();
            if (!caps.isPageFlipping() && sge.isFlipStrategyPreferred(peer)) {
                caps = new ProxyCapabilities(caps);
            }
            // assert numBuffers > 1;
            if (caps.isPageFlipping()) {
                bufferStrategy = new FlipSubRegionBufferStrategy(numBuffers, caps);
            } else {
                bufferStrategy = new BltSubRegionBufferStrategy(numBuffers, caps);
            }
        }
    }

    /**
     * This is a proxy capabilities class used when a FlipBufferStrategy
     * is created instead of the requested Blit strategy.
     *
     * @see sun.java2d.SunGraphicsEnvironment#isFlipStrategyPreferred(ComponentPeer)
     */
    private class ProxyCapabilities extends ExtendedBufferCapabilities {
        private BufferCapabilities orig;
        private ProxyCapabilities(BufferCapabilities orig) {
            super(orig.getFrontBufferCapabilities(),
                  orig.getBackBufferCapabilities(),
                  orig.getFlipContents() ==
                      BufferCapabilities.FlipContents.BACKGROUND ?
                      BufferCapabilities.FlipContents.BACKGROUND :
                      BufferCapabilities.FlipContents.COPIED);
            this.orig = orig;
        }
    }

    /**
     * @return the buffer strategy used by this component
     * @see Window#createBufferStrategy
     * @see Canvas#createBufferStrategy
     * @since 1.4
     */
    BufferStrategy getBufferStrategy() {
        return bufferStrategy;
    }

    /**
     * @return the back buffer currently used by this component's
     * BufferStrategy.  If there is no BufferStrategy or no
     * back buffer, this method returns null.
     */
    Image getBackBuffer() {
        if (bufferStrategy != null) {
            if (bufferStrategy instanceof BltBufferStrategy) {
                BltBufferStrategy bltBS = (BltBufferStrategy)bufferStrategy;
                return bltBS.getBackBuffer();
            } else if (bufferStrategy instanceof FlipBufferStrategy) {
                FlipBufferStrategy flipBS = (FlipBufferStrategy)bufferStrategy;
                return flipBS.getBackBuffer();
            }
        }
        return null;
    }

    /**
     * Inner class for flipping buffers on a component.  That component must
     * be a {@code Canvas} or {@code Window} or {@code Applet}.
     * @see Canvas
     * @see Window
     * @see Applet
     * @see java.awt.image.BufferStrategy
     * @author Michael Martak
     * @since 1.4
     */
    protected class FlipBufferStrategy extends BufferStrategy {
        /**
         * The number of buffers
         */
        protected int numBuffers; // = 0
        /**
         * The buffering capabilities
         */
        protected BufferCapabilities caps; // = null
        /**
         * The drawing buffer
         */
        protected Image drawBuffer; // = null
        /**
         * The drawing buffer as a volatile image
         */
        protected VolatileImage drawVBuffer; // = null
        /**
         * Whether or not the drawing buffer has been recently restored from
         * a lost state.
         */
        protected boolean validatedContents; // = false

        /**
         * Size of the back buffers.  (Note: these fields were added in 6.0
         * but kept package-private to avoid exposing them in the spec.
         * None of these fields/methods really should have been marked
         * protected when they were introduced in 1.4, but now we just have
         * to live with that decision.)
         */

         /**
          * The width of the back buffers
          */
        private int width;

        /**
         * The height of the back buffers
         */
        private int height;

        /**
         * Creates a new flipping buffer strategy for this component.
         * The component must be a {@code Canvas} or {@code Window} or
         * {@code Applet}.
         * @see Canvas
         * @see Window
         * @see Applet
         * @param numBuffers the number of buffers
         * @param caps the capabilities of the buffers
         * @exception AWTException if the capabilities supplied could not be
         * supported or met
         * @exception ClassCastException if the component is not a canvas or
         * window.
         * @exception IllegalStateException if the component has no peer
         * @exception IllegalArgumentException if {@code numBuffers} is less than two,
         * or if {@code BufferCapabilities.isPageFlipping} is not
         * {@code true}.
         * @see #createBuffers(int, BufferCapabilities)
         */
        @SuppressWarnings("removal")
        protected FlipBufferStrategy(int numBuffers, BufferCapabilities caps)
            throws AWTException
        {
            if (!(Component.this instanceof Window) &&
                !(Component.this instanceof Canvas) &&
                !(Component.this instanceof Applet))
            {
                throw new ClassCastException(
                        "Component must be a Canvas or Window or Applet");
            }
            this.numBuffers = numBuffers;
            this.caps = caps;
            createBuffers(numBuffers, caps);
        }

        /**
         * Creates one or more complex, flipping buffers with the given
         * capabilities.
         * @param numBuffers number of buffers to create; must be greater than
         * one
         * @param caps the capabilities of the buffers.
         * {@code BufferCapabilities.isPageFlipping} must be
         * {@code true}.
         * @exception AWTException if the capabilities supplied could not be
         * supported or met
         * @exception IllegalStateException if the component has no peer
         * @exception IllegalArgumentException if numBuffers is less than two,
         * or if {@code BufferCapabilities.isPageFlipping} is not
         * {@code true}.
         * @see java.awt.BufferCapabilities#isPageFlipping()
         */
        protected void createBuffers(int numBuffers, BufferCapabilities caps)
            throws AWTException
        {
            if (numBuffers < 2) {
                throw new IllegalArgumentException(
                    "Number of buffers cannot be less than two");
            } else if (peer == null) {
                throw new IllegalStateException(
                    "Component must have a valid peer");
            } else if (caps == null || !caps.isPageFlipping()) {
                throw new IllegalArgumentException(
                    "Page flipping capabilities must be specified");
            }

            // save the current bounds
            width = getWidth();
            height = getHeight();

            if (drawBuffer != null) {
                // dispose the existing backbuffers
                invalidate();
                // ... then recreate the backbuffers
            }

            if (caps instanceof ExtendedBufferCapabilities) {
                ExtendedBufferCapabilities ebc =
                    (ExtendedBufferCapabilities)caps;
                if (ebc.getVSync() == VSYNC_ON) {
                    // if this buffer strategy is not allowed to be v-synced,
                    // change the caps that we pass to the peer but keep on
                    // trying to create v-synced buffers;
                    // do not throw IAE here in case it is disallowed, see
                    // ExtendedBufferCapabilities for more info
                    if (!VSyncedBSManager.vsyncAllowed(this)) {
                        caps = ebc.derive(VSYNC_DEFAULT);
                    }
                }
            }

            peer.createBuffers(numBuffers, caps);
            updateInternalBuffers();
        }

        /**
         * Updates internal buffers (both volatile and non-volatile)
         * by requesting the back-buffer from the peer.
         */
        private void updateInternalBuffers() {
            // get the images associated with the draw buffer
            drawBuffer = getBackBuffer();
            if (drawBuffer instanceof VolatileImage) {
                drawVBuffer = (VolatileImage)drawBuffer;
            } else {
                drawVBuffer = null;
            }
        }

        /**
         * @return direct access to the back buffer, as an image.
         * @exception IllegalStateException if the buffers have not yet
         * been created
         */
        protected Image getBackBuffer() {
            if (peer != null) {
                return peer.getBackBuffer();
            } else {
                throw new IllegalStateException(
                    "Component must have a valid peer");
            }
        }

        /**
         * Flipping moves the contents of the back buffer to the front buffer,
         * either by copying or by moving the video pointer.
         * @param flipAction an integer value describing the flipping action
         * for the contents of the back buffer.  This should be one of the
         * values of the {@code BufferCapabilities.FlipContents}
         * property.
         * @exception IllegalStateException if the buffers have not yet
         * been created
         * @see java.awt.BufferCapabilities#getFlipContents()
         */
        protected void flip(BufferCapabilities.FlipContents flipAction) {
            if (peer != null) {
                Image backBuffer = getBackBuffer();
                if (backBuffer != null) {
                    peer.flip(0, 0,
                              backBuffer.getWidth(null),
                              backBuffer.getHeight(null), flipAction);
                }
            } else {
                throw new IllegalStateException(
                    "Component must have a valid peer");
            }
        }

        void flipSubRegion(int x1, int y1, int x2, int y2,
                      BufferCapabilities.FlipContents flipAction)
        {
            if (peer != null) {
                peer.flip(x1, y1, x2, y2, flipAction);
            } else {
                throw new IllegalStateException(
                    "Component must have a valid peer");
            }
        }

        /**
         * Destroys the buffers and invalidates the state of FlipBufferStrategy.
         */
        private void invalidate() {
            drawBuffer = null;
            drawVBuffer = null;
            destroyBuffers();
        }

        /**
         * Destroys the buffers created through this object
         */
        protected void destroyBuffers() {
            VSyncedBSManager.releaseVsync(this);
            if (peer != null) {
                peer.destroyBuffers();
            } else {
                throw new IllegalStateException(
                    "Component must have a valid peer");
            }
        }

        /**
         * @return the buffering capabilities of this strategy
         */
        public BufferCapabilities getCapabilities() {
            if (caps instanceof ProxyCapabilities) {
                return ((ProxyCapabilities)caps).orig;
            } else {
                return caps;
            }
        }

        /**
         * @return the graphics on the drawing buffer.  This method may not
         * be synchronized for performance reasons; use of this method by multiple
         * threads should be handled at the application level.  Disposal of the
         * graphics object must be handled by the application.
         */
        public Graphics getDrawGraphics() {
            revalidate();
            return drawBuffer.getGraphics();
        }

        /**
         * Restore the drawing buffer if it has been lost
         */
        protected void revalidate() {
            validatedContents = false;
            if (getWidth() != width || getHeight() != height
                    || drawBuffer == null) {
                // component has been resized or the peer was recreated;
                // recreate the backbuffers
                try {
                    createBuffers(numBuffers, caps);
                } catch (AWTException e) {
                    // shouldn't be possible
                }
                validatedContents = true;
            }

            // get the buffers from the peer every time since they
            // might have been replaced in response to a display change event
            updateInternalBuffers();

            // now validate the backbuffer
            if (drawVBuffer != null) {
                GraphicsConfiguration gc =
                        getGraphicsConfiguration_NoClientCode();
                int returnCode = drawVBuffer.validate(gc);
                if (returnCode == VolatileImage.IMAGE_INCOMPATIBLE) {
                    try {
                        createBuffers(numBuffers, caps);
                    } catch (AWTException e) {
                        // shouldn't be possible
                    }
                    if (drawVBuffer != null) {
                        // backbuffers were recreated, so validate again
                        drawVBuffer.validate(gc);
                    }
                    validatedContents = true;
                } else if (returnCode == VolatileImage.IMAGE_RESTORED) {
                    validatedContents = true;
                }
            }
        }

        /**
         * @return whether the drawing buffer was lost since the last call to
         * {@code getDrawGraphics}
         */
        public boolean contentsLost() {
            if (drawVBuffer == null) {
                return false;
            }
            return drawVBuffer.contentsLost();
        }

        /**
         * @return whether the drawing buffer was recently restored from a lost
         * state and reinitialized to the default background color (white)
         */
        public boolean contentsRestored() {
            return validatedContents;
        }

        /**
         * Makes the next available buffer visible by either blitting or
         * flipping.
         */
        public void show() {
            flip(caps.getFlipContents());
        }

        /**
         * Makes specified region of the next available buffer visible
         * by either blitting or flipping.
         */
        void showSubRegion(int x1, int y1, int x2, int y2) {
            flipSubRegion(x1, y1, x2, y2, caps.getFlipContents());
        }

        /**
         * {@inheritDoc}
         * @since 1.6
         */
        public void dispose() {
            if (Component.this.bufferStrategy == this) {
                Component.this.bufferStrategy = null;
                if (peer != null) {
                    invalidate();
                }
            }
        }

    } // Inner class FlipBufferStrategy

    /**
     * Inner class for blitting offscreen surfaces to a component.
     *
     * @author Michael Martak
     * @since 1.4
     */
    protected class BltBufferStrategy extends BufferStrategy {

        /**
         * The buffering capabilities
         */
        protected BufferCapabilities caps; // = null
        /**
         * The back buffers
         */
        protected VolatileImage[] backBuffers; // = null
        /**
         * Whether or not the drawing buffer has been recently restored from
         * a lost state.
         */
        protected boolean validatedContents; // = false
        /**
         * Width of the back buffers
         */
        protected int width;
        /**
         * Height of the back buffers
         */
        protected int height;

        /**
         * Insets for the hosting Component.  The size of the back buffer
         * is constrained by these.
         */
        private Insets insets;

        /**
         * Creates a new blt buffer strategy around a component
         * @param numBuffers number of buffers to create, including the
         * front buffer
         * @param caps the capabilities of the buffers
         */
        protected BltBufferStrategy(int numBuffers, BufferCapabilities caps) {
            this.caps = caps;
            createBackBuffers(numBuffers - 1);
        }

        /**
         * {@inheritDoc}
         * @since 1.6
         */
        public void dispose() {
            if (backBuffers != null) {
                for (int counter = backBuffers.length - 1; counter >= 0;
                     counter--) {
                    if (backBuffers[counter] != null) {
                        backBuffers[counter].flush();
                        backBuffers[counter] = null;
                    }
                }
            }
            if (Component.this.bufferStrategy == this) {
                Component.this.bufferStrategy = null;
            }
        }

        /**
         * Creates the back buffers
         *
         * @param numBuffers the number of buffers to create
         */
        protected void createBackBuffers(int numBuffers) {
            if (numBuffers == 0) {
                backBuffers = null;
            } else {
                // save the current bounds
                width = getWidth();
                height = getHeight();
                insets = getInsets_NoClientCode();
                int iWidth = width - insets.left - insets.right;
                int iHeight = height - insets.top - insets.bottom;

                // It is possible for the component's width and/or height
                // to be 0 here.  Force the size of the backbuffers to
                // be > 0 so that creating the image won't fail.
                iWidth = Math.max(1, iWidth);
                iHeight = Math.max(1, iHeight);
                if (backBuffers == null) {
                    backBuffers = new VolatileImage[numBuffers];
                } else {
                    // flush any existing backbuffers
                    for (int i = 0; i < numBuffers; i++) {
                        if (backBuffers[i] != null) {
                            backBuffers[i].flush();
                            backBuffers[i] = null;
                        }
                    }
                }

                // create the backbuffers
                for (int i = 0; i < numBuffers; i++) {
                    backBuffers[i] = createVolatileImage(iWidth, iHeight);
                }
            }
        }

        /**
         * @return the buffering capabilities of this strategy
         */
        public BufferCapabilities getCapabilities() {
            return caps;
        }

        /**
         * @return the draw graphics
         */
        public Graphics getDrawGraphics() {
            revalidate();
            Image backBuffer = getBackBuffer();
            if (backBuffer == null) {
                return getGraphics();
            }
            SunGraphics2D g = (SunGraphics2D)backBuffer.getGraphics();
            g.constrain(-insets.left, -insets.top,
                        backBuffer.getWidth(null) + insets.left,
                        backBuffer.getHeight(null) + insets.top);
            return g;
        }

        /**
         * @return direct access to the back buffer, as an image.
         * If there is no back buffer, returns null.
         */
        Image getBackBuffer() {
            if (backBuffers != null) {
                return backBuffers[backBuffers.length - 1];
            } else {
                return null;
            }
        }

        /**
         * Makes the next available buffer visible.
         */
        public void show() {
            showSubRegion(insets.left, insets.top,
                          width - insets.right,
                          height - insets.bottom);
        }

        /**
         * Package-private method to present a specific rectangular area
         * of this buffer.  This class currently shows only the entire
         * buffer, by calling showSubRegion() with the full dimensions of
         * the buffer.  Subclasses (e.g., BltSubRegionBufferStrategy
         * and FlipSubRegionBufferStrategy) may have region-specific show
         * methods that call this method with actual sub regions of the
         * buffer.
         */
        void showSubRegion(int x1, int y1, int x2, int y2) {
            if (backBuffers == null) {
                return;
            }
            // Adjust location to be relative to client area.
            x1 -= insets.left;
            x2 -= insets.left;
            y1 -= insets.top;
            y2 -= insets.top;
            Graphics g = getGraphics_NoClientCode();
            if (g == null) {
                // Not showing, bail
                return;
            }
            try {
                // First image copy is in terms of Frame's coordinates, need
                // to translate to client area.
                g.translate(insets.left, insets.top);
                for (int i = 0; i < backBuffers.length; i++) {
                    g.drawImage(backBuffers[i],
                                x1, y1, x2, y2,
                                x1, y1, x2, y2,
                                null);
                    g.dispose();
                    g = null;
                    g = backBuffers[i].getGraphics();
                }
            } finally {
                if (g != null) {
                    g.dispose();
                }
            }
        }

        /**
         * Restore the drawing buffer if it has been lost
         */
        protected void revalidate() {
            revalidate(true);
        }

        void revalidate(boolean checkSize) {
            validatedContents = false;

            if (backBuffers == null) {
                return;
            }

            if (checkSize) {
                Insets insets = getInsets_NoClientCode();
                if (getWidth() != width || getHeight() != height ||
                    !insets.equals(this.insets)) {
                    // component has been resized; recreate the backbuffers
                    createBackBuffers(backBuffers.length);
                    validatedContents = true;
                }
            }

            // now validate the backbuffer
            GraphicsConfiguration gc = getGraphicsConfiguration_NoClientCode();
            int returnCode =
                backBuffers[backBuffers.length - 1].validate(gc);
            if (returnCode == VolatileImage.IMAGE_INCOMPATIBLE) {
                if (checkSize) {
                    createBackBuffers(backBuffers.length);
                    // backbuffers were recreated, so validate again
                    backBuffers[backBuffers.length - 1].validate(gc);
                }
                // else case means we're called from Swing on the toolkit
                // thread, don't recreate buffers as that'll deadlock
                // (creating VolatileImages invokes getting GraphicsConfig
                // which grabs treelock).
                validatedContents = true;
            } else if (returnCode == VolatileImage.IMAGE_RESTORED) {
                validatedContents = true;
            }
        }

        /**
         * @return whether the drawing buffer was lost since the last call to
         * {@code getDrawGraphics}
         */
        public boolean contentsLost() {
            if (backBuffers == null) {
                return false;
            } else {
                return backBuffers[backBuffers.length - 1].contentsLost();
            }
        }

        /**
         * @return whether the drawing buffer was recently restored from a lost
         * state and reinitialized to the default background color (white)
         */
        public boolean contentsRestored() {
            return validatedContents;
        }
    } // Inner class BltBufferStrategy

    /**
     * Private class to perform sub-region flipping.
     */
    private class FlipSubRegionBufferStrategy extends FlipBufferStrategy
        implements SubRegionShowable
    {

        protected FlipSubRegionBufferStrategy(int numBuffers,
                                              BufferCapabilities caps)
            throws AWTException
        {
            super(numBuffers, caps);
        }

        public void show(int x1, int y1, int x2, int y2) {
            showSubRegion(x1, y1, x2, y2);
        }

        // This is invoked by Swing on the toolkit thread.
        public boolean showIfNotLost(int x1, int y1, int x2, int y2) {
            if (!contentsLost()) {
                showSubRegion(x1, y1, x2, y2);
                return !contentsLost();
            }
            return false;
        }
    }

    /**
     * Private class to perform sub-region blitting.  Swing will use
     * this subclass via the SubRegionShowable interface in order to
     * copy only the area changed during a repaint.
     * See javax.swing.BufferStrategyPaintManager.
     */
    private class BltSubRegionBufferStrategy extends BltBufferStrategy
        implements SubRegionShowable
    {

        protected BltSubRegionBufferStrategy(int numBuffers,
                                             BufferCapabilities caps)
        {
            super(numBuffers, caps);
        }

        public void show(int x1, int y1, int x2, int y2) {
            showSubRegion(x1, y1, x2, y2);
        }

        // This method is called by Swing on the toolkit thread.
        public boolean showIfNotLost(int x1, int y1, int x2, int y2) {
            if (!contentsLost()) {
                showSubRegion(x1, y1, x2, y2);
                return !contentsLost();
            }
            return false;
        }
    }

    /**
     * Inner class for flipping buffers on a component.  That component must
     * be a {@code Canvas} or {@code Window}.
     * @see Canvas
     * @see Window
     * @see java.awt.image.BufferStrategy
     * @author Michael Martak
     * @since 1.4
     */
    private class SingleBufferStrategy extends BufferStrategy {

        private BufferCapabilities caps;

        public SingleBufferStrategy(BufferCapabilities caps) {
            this.caps = caps;
        }
        public BufferCapabilities getCapabilities() {
            return caps;
        }
        public Graphics getDrawGraphics() {
            return getGraphics();
        }
        public boolean contentsLost() {
            return false;
        }
        public boolean contentsRestored() {
            return false;
        }
        public void show() {
            // Do nothing
        }
    } // Inner class SingleBufferStrategy

    /**
     * Sets whether or not paint messages received from the operating system
     * should be ignored.  This does not affect paint events generated in
     * software by the AWT, unless they are an immediate response to an
     * OS-level paint message.
     * <p>
     * This is useful, for example, if running under full-screen mode and
     * better performance is desired, or if page-flipping is used as the
     * buffer strategy.
     *
     * @param ignoreRepaint {@code true} if the paint messages from the OS
     *                      should be ignored; otherwise {@code false}
     *
     * @since 1.4
     * @see #getIgnoreRepaint
     * @see Canvas#createBufferStrategy
     * @see Window#createBufferStrategy
     * @see java.awt.image.BufferStrategy
     * @see GraphicsDevice#setFullScreenWindow
     */
    public void setIgnoreRepaint(boolean ignoreRepaint) {
        this.ignoreRepaint = ignoreRepaint;
    }

    /**
     * @return whether or not paint messages received from the operating system
     * should be ignored.
     *
     * @since 1.4
     * @see #setIgnoreRepaint
     */
    public boolean getIgnoreRepaint() {
        return ignoreRepaint;
    }

    /**
     * Checks whether this component "contains" the specified point,
     * where {@code x} and {@code y} are defined to be
     * relative to the coordinate system of this component.
     *
     * @param     x   the <i>x</i> coordinate of the point
     * @param     y   the <i>y</i> coordinate of the point
     * @return {@code true} if the point is within the component;
     *         otherwise {@code false}
     * @see       #getComponentAt(int, int)
     * @since     1.1
     */
    public boolean contains(int x, int y) {
        return inside(x, y);
    }

    /**
     * Checks whether the point is inside of this component.
     *
     * @param  x the <i>x</i> coordinate of the point
     * @param  y the <i>y</i> coordinate of the point
     * @return {@code true} if the point is within the component;
     *         otherwise {@code false}
     * @deprecated As of JDK version 1.1,
     * replaced by contains(int, int).
     */
    @Deprecated
    public boolean inside(int x, int y) {
        return (x >= 0) && (x < width) && (y >= 0) && (y < height);
    }

    /**
     * Checks whether this component "contains" the specified point,
     * where the point's <i>x</i> and <i>y</i> coordinates are defined
     * to be relative to the coordinate system of this component.
     *
     * @param     p     the point
     * @return {@code true} if the point is within the component;
     *         otherwise {@code false}
     * @throws    NullPointerException if {@code p} is {@code null}
     * @see       #getComponentAt(Point)
     * @since     1.1
     */
    public boolean contains(Point p) {
        return contains(p.x, p.y);
    }

    /**
     * Determines if this component or one of its immediate
     * subcomponents contains the (<i>x</i>,&nbsp;<i>y</i>) location,
     * and if so, returns the containing component. This method only
     * looks one level deep. If the point (<i>x</i>,&nbsp;<i>y</i>) is
     * inside a subcomponent that itself has subcomponents, it does not
     * go looking down the subcomponent tree.
     * <p>
     * The {@code locate} method of {@code Component} simply
     * returns the component itself if the (<i>x</i>,&nbsp;<i>y</i>)
     * coordinate location is inside its bounding box, and {@code null}
     * otherwise.
     * @param     x   the <i>x</i> coordinate
     * @param     y   the <i>y</i> coordinate
     * @return    the component or subcomponent that contains the
     *                (<i>x</i>,&nbsp;<i>y</i>) location;
     *                {@code null} if the location
     *                is outside this component
     * @see       #contains(int, int)
     * @since     1.0
     */
    public Component getComponentAt(int x, int y) {
        return locate(x, y);
    }

    /**
     * Returns the component occupying the position specified (this component,
     * or immediate child component, or null if neither
     * of the first two occupies the location).
     *
     * @param  x the <i>x</i> coordinate to search for components at
     * @param  y the <i>y</i> coordinate to search for components at
     * @return the component at the specified location or {@code null}
     * @deprecated As of JDK version 1.1,
     * replaced by getComponentAt(int, int).
     */
    @Deprecated
    public Component locate(int x, int y) {
        return contains(x, y) ? this : null;
    }

    /**
     * Returns the component or subcomponent that contains the
     * specified point.
     * @param  p the point
     * @return the component at the specified location or {@code null}
     * @see java.awt.Component#contains
     * @since 1.1
     */
    public Component getComponentAt(Point p) {
        return getComponentAt(p.x, p.y);
    }

    /**
     * @param  e the event to deliver
     * @deprecated As of JDK version 1.1,
     * replaced by {@code dispatchEvent(AWTEvent e)}.
     */
    @Deprecated
    public void deliverEvent(Event e) {
        postEvent(e);
    }

    /**
     * Dispatches an event to this component or one of its sub components.
     * Calls {@code processEvent} before returning for 1.1-style
     * events which have been enabled for the {@code Component}.
     * @param e the event
     */
    public final void dispatchEvent(AWTEvent e) {
        dispatchEventImpl(e);
    }

    @SuppressWarnings("deprecation")
    void dispatchEventImpl(AWTEvent e) {
        int id = e.getID();

        // Check that this component belongs to this app-context
        AppContext compContext = appContext;
        if (compContext != null && !compContext.equals(AppContext.getAppContext())) {
            if (eventLog.isLoggable(PlatformLogger.Level.FINE)) {
                eventLog.fine("Event " + e + " is being dispatched on the wrong AppContext");
            }
        }

        if (eventLog.isLoggable(PlatformLogger.Level.FINEST)) {
            eventLog.finest("{0}", e);
        }

        /*
         * 0. Set timestamp and modifiers of current event.
         */
        if (!(e instanceof KeyEvent)) {
            // Timestamp of a key event is set later in DKFM.preDispatchKeyEvent(KeyEvent).
            EventQueue.setCurrentEventAndMostRecentTime(e);
        }

        /*
         * 1. Pre-dispatchers. Do any necessary retargeting/reordering here
         *    before we notify AWTEventListeners.
         */

        if (e instanceof SunDropTargetEvent) {
            ((SunDropTargetEvent)e).dispatch();
            return;
        }

        if (!e.focusManagerIsDispatching) {
            // Invoke the private focus retargeting method which provides
            // lightweight Component support
            if (e.isPosted) {
                e = KeyboardFocusManager.retargetFocusEvent(e);
                e.isPosted = true;
            }

            // Now, with the event properly targeted to a lightweight
            // descendant if necessary, invoke the public focus retargeting
            // and dispatching function
            if (KeyboardFocusManager.getCurrentKeyboardFocusManager().
                dispatchEvent(e))
            {
                return;
            }
        }
        if ((e instanceof FocusEvent) && focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
            focusLog.finest("" + e);
        }
        // MouseWheel may need to be retargeted here so that
        // AWTEventListener sees the event go to the correct
        // Component.  If the MouseWheelEvent needs to go to an ancestor,
        // the event is dispatched to the ancestor, and dispatching here
        // stops.
        if (id == MouseEvent.MOUSE_WHEEL &&
            (!eventTypeEnabled(id)) &&
            (peer != null && !peer.handlesWheelScrolling()) &&
            (dispatchMouseWheelToAncestor((MouseWheelEvent)e)))
        {
            return;
        }

        /*
         * 2. Allow the Toolkit to pass this to AWTEventListeners.
         */
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        toolkit.notifyAWTEventListeners(e);


        /*
         * 3. If no one has consumed a key event, allow the
         *    KeyboardFocusManager to process it.
         */
        if (!e.isConsumed()) {
            if (e instanceof java.awt.event.KeyEvent) {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().
                    processKeyEvent(this, (KeyEvent)e);
                if (e.isConsumed()) {
                    return;
                }
            }
        }

        /*
         * 4. Allow input methods to process the event
         */
        if (areInputMethodsEnabled()) {
            // We need to pass on InputMethodEvents since some host
            // input method adapters send them through the Java
            // event queue instead of directly to the component,
            // and the input context also handles the Java composition window
            if(((e instanceof InputMethodEvent) && !(this instanceof CompositionArea))
               ||
               // Otherwise, we only pass on input and focus events, because
               // a) input methods shouldn't know about semantic or component-level events
               // b) passing on the events takes time
               // c) isConsumed() is always true for semantic events.
               (e instanceof InputEvent) || (e instanceof FocusEvent)) {
                InputContext inputContext = getInputContext();


                if (inputContext != null) {
                    inputContext.dispatchEvent(e);
                    if (e.isConsumed()) {
                        if ((e instanceof FocusEvent) && focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                            focusLog.finest("3579: Skipping " + e);
                        }
                        return;
                    }
                }
            }
        } else {
            // When non-clients get focus, we need to explicitly disable the native
            // input method. The native input method is actually not disabled when
            // the active/passive/peered clients loose focus.
            if (id == FocusEvent.FOCUS_GAINED) {
                InputContext inputContext = getInputContext();
                if (inputContext != null && inputContext instanceof sun.awt.im.InputContext) {
                    ((sun.awt.im.InputContext)inputContext).disableNativeIM();
                }
            }
        }


        /*
         * 5. Pre-process any special events before delivery
         */
        switch(id) {
            // Handling of the PAINT and UPDATE events is now done in the
            // peer's handleEvent() method so the background can be cleared
            // selectively for non-native components on Windows only.
            // - Fred.Ecks@Eng.sun.com, 5-8-98

          case KeyEvent.KEY_PRESSED:
          case KeyEvent.KEY_RELEASED:
              Container p = (Container)((this instanceof Container) ? this : parent);
              if (p != null) {
                  p.preProcessKeyEvent((KeyEvent)e);
                  if (e.isConsumed()) {
                        if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                            focusLog.finest("Pre-process consumed event");
                        }
                      return;
                  }
              }
              break;

          default:
              break;
        }

        /*
         * 6. Deliver event for normal processing
         */
        if (newEventsOnly) {
            // Filtering needs to really be moved to happen at a lower
            // level in order to get maximum performance gain;  it is
            // here temporarily to ensure the API spec is honored.
            //
            if (eventEnabled(e)) {
                processEvent(e);
            }
        } else if (id == MouseEvent.MOUSE_WHEEL) {
            // newEventsOnly will be false for a listenerless ScrollPane, but
            // MouseWheelEvents still need to be dispatched to it so scrolling
            // can be done.
            autoProcessMouseWheel((MouseWheelEvent)e);
        } else if (!(e instanceof MouseEvent && !postsOldMouseEvents())) {
            //
            // backward compatibility
            //
            Event olde = e.convertToOld();
            if (olde != null) {
                int key = olde.key;
                int modifiers = olde.modifiers;

                postEvent(olde);
                if (olde.isConsumed()) {
                    e.consume();
                }
                // if target changed key or modifier values, copy them
                // back to original event
                //
                switch(olde.id) {
                  case Event.KEY_PRESS:
                  case Event.KEY_RELEASE:
                  case Event.KEY_ACTION:
                  case Event.KEY_ACTION_RELEASE:
                      if (olde.key != key) {
                          ((KeyEvent)e).setKeyChar(olde.getKeyEventChar());
                      }
                      if (olde.modifiers != modifiers) {
                          ((KeyEvent)e).setModifiers(olde.modifiers);
                      }
                      break;
                  default:
                      break;
                }
            }
        }

        /*
         * 9. Allow the peer to process the event.
         * Except KeyEvents, they will be processed by peer after
         * all KeyEventPostProcessors
         * (see DefaultKeyboardFocusManager.dispatchKeyEvent())
         */
        if (!(e instanceof KeyEvent)) {
            ComponentPeer tpeer = peer;
            if (e instanceof FocusEvent && (tpeer == null || tpeer instanceof LightweightPeer)) {
                // if focus owner is lightweight then its native container
                // processes event
                Component source = (Component)e.getSource();
                if (source != null) {
                    Container target = source.getNativeContainer();
                    if (target != null) {
                        tpeer = target.peer;
                    }
                }
            }
            if (tpeer != null) {
                tpeer.handleEvent(e);
            }
        }

        if (SunToolkit.isTouchKeyboardAutoShowEnabled() &&
            (toolkit instanceof SunToolkit) &&
            ((e instanceof MouseEvent) || (e instanceof FocusEvent))) {
            ((SunToolkit)toolkit).showOrHideTouchKeyboard(this, e);
        }
    } // dispatchEventImpl()

    /*
     * If newEventsOnly is false, method is called so that ScrollPane can
     * override it and handle common-case mouse wheel scrolling.  NOP
     * for Component.
     */
    void autoProcessMouseWheel(MouseWheelEvent e) {}

    /*
     * Dispatch given MouseWheelEvent to the first ancestor for which
     * MouseWheelEvents are enabled.
     *
     * Returns whether or not event was dispatched to an ancestor
     */
    @SuppressWarnings("deprecation")
    boolean dispatchMouseWheelToAncestor(MouseWheelEvent e) {
        int newX, newY;
        newX = e.getX() + getX(); // Coordinates take into account at least
        newY = e.getY() + getY(); // the cursor's position relative to this
                                  // Component (e.getX()), and this Component's
                                  // position relative to its parent.
        MouseWheelEvent newMWE;

        if (eventLog.isLoggable(PlatformLogger.Level.FINEST)) {
            eventLog.finest("dispatchMouseWheelToAncestor");
            eventLog.finest("orig event src is of " + e.getSource().getClass());
        }

        /* parent field for Window refers to the owning Window.
         * MouseWheelEvents should NOT be propagated into owning Windows
         */
        synchronized (getTreeLock()) {
            Container anc = getParent();
            while (anc != null && !anc.eventEnabled(e)) {
                // fix coordinates to be relative to new event source
                newX += anc.getX();
                newY += anc.getY();

                if (!(anc instanceof Window)) {
                    anc = anc.getParent();
                }
                else {
                    break;
                }
            }

            if (eventLog.isLoggable(PlatformLogger.Level.FINEST)) {
                eventLog.finest("new event src is " + anc.getClass());
            }

            if (anc != null && anc.eventEnabled(e)) {
                // Change event to be from new source, with new x,y
                // For now, just create a new event - yucky

                newMWE = new MouseWheelEvent(anc, // new source
                                             e.getID(),
                                             e.getWhen(),
                                             e.getModifiers(),
                                             newX, // x relative to new source
                                             newY, // y relative to new source
                                             e.getXOnScreen(),
                                             e.getYOnScreen(),
                                             e.getClickCount(),
                                             e.isPopupTrigger(),
                                             e.getScrollType(),
                                             e.getScrollAmount(),
                                             e.getWheelRotation(),
                                             e.getPreciseWheelRotation());
                ((AWTEvent)e).copyPrivateDataInto(newMWE);
                // When dispatching a wheel event to
                // ancestor, there is no need trying to find descendant
                // lightweights to dispatch event to.
                // If we dispatch the event to toplevel ancestor,
                // this could enclose the loop: 6480024.
                anc.dispatchEventToSelf(newMWE);
                if (newMWE.isConsumed()) {
                    e.consume();
                }
                return true;
            }
        }
        return false;
    }

    boolean areInputMethodsEnabled() {
        // in 1.2, we assume input method support is required for all
        // components that handle key events, but components can turn off
        // input methods by calling enableInputMethods(false).
        return ((eventMask & AWTEvent.INPUT_METHODS_ENABLED_MASK) != 0) &&
            ((eventMask & AWTEvent.KEY_EVENT_MASK) != 0 || keyListener != null);
    }

    // REMIND: remove when filtering is handled at lower level
    boolean eventEnabled(AWTEvent e) {
        return eventTypeEnabled(e.id);
    }

    boolean eventTypeEnabled(int type) {
        switch(type) {
          case ComponentEvent.COMPONENT_MOVED:
          case ComponentEvent.COMPONENT_RESIZED:
          case ComponentEvent.COMPONENT_SHOWN:
          case ComponentEvent.COMPONENT_HIDDEN:
              if ((eventMask & AWTEvent.COMPONENT_EVENT_MASK) != 0 ||
                  componentListener != null) {
                  return true;
              }
              break;
          case FocusEvent.FOCUS_GAINED:
          case FocusEvent.FOCUS_LOST:
              if ((eventMask & AWTEvent.FOCUS_EVENT_MASK) != 0 ||
                  focusListener != null) {
                  return true;
              }
              break;
          case KeyEvent.KEY_PRESSED:
          case KeyEvent.KEY_RELEASED:
          case KeyEvent.KEY_TYPED:
              if ((eventMask & AWTEvent.KEY_EVENT_MASK) != 0 ||
                  keyListener != null) {
                  return true;
              }
              break;
          case MouseEvent.MOUSE_PRESSED:
          case MouseEvent.MOUSE_RELEASED:
          case MouseEvent.MOUSE_ENTERED:
          case MouseEvent.MOUSE_EXITED:
          case MouseEvent.MOUSE_CLICKED:
              if ((eventMask & AWTEvent.MOUSE_EVENT_MASK) != 0 ||
                  mouseListener != null) {
                  return true;
              }
              break;
          case MouseEvent.MOUSE_MOVED:
          case MouseEvent.MOUSE_DRAGGED:
              if ((eventMask & AWTEvent.MOUSE_MOTION_EVENT_MASK) != 0 ||
                  mouseMotionListener != null) {
                  return true;
              }
              break;
          case MouseEvent.MOUSE_WHEEL:
              if ((eventMask & AWTEvent.MOUSE_WHEEL_EVENT_MASK) != 0 ||
                  mouseWheelListener != null) {
                  return true;
              }
              break;
          case InputMethodEvent.INPUT_METHOD_TEXT_CHANGED:
          case InputMethodEvent.CARET_POSITION_CHANGED:
              if ((eventMask & AWTEvent.INPUT_METHOD_EVENT_MASK) != 0 ||
                  inputMethodListener != null) {
                  return true;
              }
              break;
          case HierarchyEvent.HIERARCHY_CHANGED:
              if ((eventMask & AWTEvent.HIERARCHY_EVENT_MASK) != 0 ||
                  hierarchyListener != null) {
                  return true;
              }
              break;
          case HierarchyEvent.ANCESTOR_MOVED:
          case HierarchyEvent.ANCESTOR_RESIZED:
              if ((eventMask & AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK) != 0 ||
                  hierarchyBoundsListener != null) {
                  return true;
              }
              break;
          case ActionEvent.ACTION_PERFORMED:
              if ((eventMask & AWTEvent.ACTION_EVENT_MASK) != 0) {
                  return true;
              }
              break;
          case TextEvent.TEXT_VALUE_CHANGED:
              if ((eventMask & AWTEvent.TEXT_EVENT_MASK) != 0) {
                  return true;
              }
              break;
          case ItemEvent.ITEM_STATE_CHANGED:
              if ((eventMask & AWTEvent.ITEM_EVENT_MASK) != 0) {
                  return true;
              }
              break;
          case AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED:
              if ((eventMask & AWTEvent.ADJUSTMENT_EVENT_MASK) != 0) {
                  return true;
              }
              break;
          default:
              break;
        }
        //
        // Always pass on events defined by external programs.
        //
        if (type > AWTEvent.RESERVED_ID_MAX) {
            return true;
        }
        return false;
    }

    /**
     * @deprecated As of JDK version 1.1,
     * replaced by dispatchEvent(AWTEvent).
     */
    @Deprecated
    public boolean postEvent(Event e) {
        ComponentPeer peer = this.peer;

        if (handleEvent(e)) {
            e.consume();
            return true;
        }

        Component parent = this.parent;
        int eventx = e.x;
        int eventy = e.y;
        if (parent != null) {
            e.translate(x, y);
            if (parent.postEvent(e)) {
                e.consume();
                return true;
            }
            // restore coords
            e.x = eventx;
            e.y = eventy;
        }
        return false;
    }

    // Event source interfaces

    /**
     * Adds the specified component listener to receive component events from
     * this component.
     * If listener {@code l} is {@code null},
     * no exception is thrown and no action is performed.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param    l   the component listener
     * @see      java.awt.event.ComponentEvent
     * @see      java.awt.event.ComponentListener
     * @see      #removeComponentListener
     * @see      #getComponentListeners
     * @since    1.1
     */
    public synchronized void addComponentListener(ComponentListener l) {
        if (l == null) {
            return;
        }
        componentListener = AWTEventMulticaster.add(componentListener, l);
        newEventsOnly = true;
    }

    /**
     * Removes the specified component listener so that it no longer
     * receives component events from this component. This method performs
     * no function, nor does it throw an exception, if the listener
     * specified by the argument was not previously added to this component.
     * If listener {@code l} is {@code null},
     * no exception is thrown and no action is performed.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     * @param    l   the component listener
     * @see      java.awt.event.ComponentEvent
     * @see      java.awt.event.ComponentListener
     * @see      #addComponentListener
     * @see      #getComponentListeners
     * @since    1.1
     */
    public synchronized void removeComponentListener(ComponentListener l) {
        if (l == null) {
            return;
        }
        componentListener = AWTEventMulticaster.remove(componentListener, l);
    }

    /**
     * Returns an array of all the component listeners
     * registered on this component.
     *
     * @return all {@code ComponentListener}s of this component
     *         or an empty array if no component
     *         listeners are currently registered
     *
     * @see #addComponentListener
     * @see #removeComponentListener
     * @since 1.4
     */
    public synchronized ComponentListener[] getComponentListeners() {
        return getListeners(ComponentListener.class);
    }

    /**
     * Adds the specified focus listener to receive focus events from
     * this component when this component gains input focus.
     * If listener {@code l} is {@code null},
     * no exception is thrown and no action is performed.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param    l   the focus listener
     * @see      java.awt.event.FocusEvent
     * @see      java.awt.event.FocusListener
     * @see      #removeFocusListener
     * @see      #getFocusListeners
     * @since    1.1
     */
    public synchronized void addFocusListener(FocusListener l) {
        if (l == null) {
            return;
        }
        focusListener = AWTEventMulticaster.add(focusListener, l);
        newEventsOnly = true;

        // if this is a lightweight component, enable focus events
        // in the native container.
        if (peer instanceof LightweightPeer) {
            parent.proxyEnableEvents(AWTEvent.FOCUS_EVENT_MASK);
        }
    }

    /**
     * Removes the specified focus listener so that it no longer
     * receives focus events from this component. This method performs
     * no function, nor does it throw an exception, if the listener
     * specified by the argument was not previously added to this component.
     * If listener {@code l} is {@code null},
     * no exception is thrown and no action is performed.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param    l   the focus listener
     * @see      java.awt.event.FocusEvent
     * @see      java.awt.event.FocusListener
     * @see      #addFocusListener
     * @see      #getFocusListeners
     * @since    1.1
     */
    public synchronized void removeFocusListener(FocusListener l) {
        if (l == null) {
            return;
        }
        focusListener = AWTEventMulticaster.remove(focusListener, l);
    }

    /**
     * Returns an array of all the focus listeners
     * registered on this component.
     *
     * @return all of this component's {@code FocusListener}s
     *         or an empty array if no component
     *         listeners are currently registered
     *
     * @see #addFocusListener
     * @see #removeFocusListener
     * @since 1.4
     */
    public synchronized FocusListener[] getFocusListeners() {
        return getListeners(FocusListener.class);
    }

    /**
     * Adds the specified hierarchy listener to receive hierarchy changed
     * events from this component when the hierarchy to which this container
     * belongs changes.
     * If listener {@code l} is {@code null},
     * no exception is thrown and no action is performed.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param    l   the hierarchy listener
     * @see      java.awt.event.HierarchyEvent
     * @see      java.awt.event.HierarchyListener
     * @see      #removeHierarchyListener
     * @see      #getHierarchyListeners
     * @since    1.3
     */
    public void addHierarchyListener(HierarchyListener l) {
        if (l == null) {
            return;
        }
        boolean notifyAncestors;
        synchronized (this) {
            notifyAncestors =
                (hierarchyListener == null &&
                 (eventMask & AWTEvent.HIERARCHY_EVENT_MASK) == 0);
            hierarchyListener = AWTEventMulticaster.add(hierarchyListener, l);
            notifyAncestors = (notifyAncestors && hierarchyListener != null);
            newEventsOnly = true;
        }
        if (notifyAncestors) {
            synchronized (getTreeLock()) {
                adjustListeningChildrenOnParent(AWTEvent.HIERARCHY_EVENT_MASK,
                                                1);
            }
        }
    }

    /**
     * Removes the specified hierarchy listener so that it no longer
     * receives hierarchy changed events from this component. This method
     * performs no function, nor does it throw an exception, if the listener
     * specified by the argument was not previously added to this component.
     * If listener {@code l} is {@code null},
     * no exception is thrown and no action is performed.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param    l   the hierarchy listener
     * @see      java.awt.event.HierarchyEvent
     * @see      java.awt.event.HierarchyListener
     * @see      #addHierarchyListener
     * @see      #getHierarchyListeners
     * @since    1.3
     */
    public void removeHierarchyListener(HierarchyListener l) {
        if (l == null) {
            return;
        }
        boolean notifyAncestors;
        synchronized (this) {
            notifyAncestors =
                (hierarchyListener != null &&
                 (eventMask & AWTEvent.HIERARCHY_EVENT_MASK) == 0);
            hierarchyListener =
                AWTEventMulticaster.remove(hierarchyListener, l);
            notifyAncestors = (notifyAncestors && hierarchyListener == null);
        }
        if (notifyAncestors) {
            synchronized (getTreeLock()) {
                adjustListeningChildrenOnParent(AWTEvent.HIERARCHY_EVENT_MASK,
                                                -1);
            }
        }
    }

    /**
     * Returns an array of all the hierarchy listeners
     * registered on this component.
     *
     * @return all of this component's {@code HierarchyListener}s
     *         or an empty array if no hierarchy
     *         listeners are currently registered
     *
     * @see      #addHierarchyListener
     * @see      #removeHierarchyListener
     * @since    1.4
     */
    public synchronized HierarchyListener[] getHierarchyListeners() {
        return getListeners(HierarchyListener.class);
    }

    /**
     * Adds the specified hierarchy bounds listener to receive hierarchy
     * bounds events from this component when the hierarchy to which this
     * container belongs changes.
     * If listener {@code l} is {@code null},
     * no exception is thrown and no action is performed.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param    l   the hierarchy bounds listener
     * @see      java.awt.event.HierarchyEvent
     * @see      java.awt.event.HierarchyBoundsListener
     * @see      #removeHierarchyBoundsListener
     * @see      #getHierarchyBoundsListeners
     * @since    1.3
     */
    public void addHierarchyBoundsListener(HierarchyBoundsListener l) {
        if (l == null) {
            return;
        }
        boolean notifyAncestors;
        synchronized (this) {
            notifyAncestors =
                (hierarchyBoundsListener == null &&
                 (eventMask & AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK) == 0);
            hierarchyBoundsListener =
                AWTEventMulticaster.add(hierarchyBoundsListener, l);
            notifyAncestors = (notifyAncestors &&
                               hierarchyBoundsListener != null);
            newEventsOnly = true;
        }
        if (notifyAncestors) {
            synchronized (getTreeLock()) {
                adjustListeningChildrenOnParent(
                                                AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK, 1);
            }
        }
    }

    /**
     * Removes the specified hierarchy bounds listener so that it no longer
     * receives hierarchy bounds events from this component. This method
     * performs no function, nor does it throw an exception, if the listener
     * specified by the argument was not previously added to this component.
     * If listener {@code l} is {@code null},
     * no exception is thrown and no action is performed.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param    l   the hierarchy bounds listener
     * @see      java.awt.event.HierarchyEvent
     * @see      java.awt.event.HierarchyBoundsListener
     * @see      #addHierarchyBoundsListener
     * @see      #getHierarchyBoundsListeners
     * @since    1.3
     */
    public void removeHierarchyBoundsListener(HierarchyBoundsListener l) {
        if (l == null) {
            return;
        }
        boolean notifyAncestors;
        synchronized (this) {
            notifyAncestors =
                (hierarchyBoundsListener != null &&
                 (eventMask & AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK) == 0);
            hierarchyBoundsListener =
                AWTEventMulticaster.remove(hierarchyBoundsListener, l);
            notifyAncestors = (notifyAncestors &&
                               hierarchyBoundsListener == null);
        }
        if (notifyAncestors) {
            synchronized (getTreeLock()) {
                adjustListeningChildrenOnParent(
                                                AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK, -1);
            }
        }
    }

    // Should only be called while holding the tree lock
    int numListening(long mask) {
        // One mask or the other, but not neither or both.
        if (eventLog.isLoggable(PlatformLogger.Level.FINE)) {
            if ((mask != AWTEvent.HIERARCHY_EVENT_MASK) &&
                (mask != AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK))
            {
                eventLog.fine("Assertion failed");
            }
        }
        if ((mask == AWTEvent.HIERARCHY_EVENT_MASK &&
             (hierarchyListener != null ||
              (eventMask & AWTEvent.HIERARCHY_EVENT_MASK) != 0)) ||
            (mask == AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK &&
             (hierarchyBoundsListener != null ||
              (eventMask & AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK) != 0))) {
            return 1;
        } else {
            return 0;
        }
    }

    // Should only be called while holding tree lock
    int countHierarchyMembers() {
        return 1;
    }
    // Should only be called while holding the tree lock
    int createHierarchyEvents(int id, Component changed,
                              Container changedParent, long changeFlags,
                              boolean enabledOnToolkit) {
        switch (id) {
          case HierarchyEvent.HIERARCHY_CHANGED:
              if (hierarchyListener != null ||
                  (eventMask & AWTEvent.HIERARCHY_EVENT_MASK) != 0 ||
                  enabledOnToolkit) {
                  HierarchyEvent e = new HierarchyEvent(this, id, changed,
                                                        changedParent,
                                                        changeFlags);
                  dispatchEvent(e);
                  return 1;
              }
              break;
          case HierarchyEvent.ANCESTOR_MOVED:
          case HierarchyEvent.ANCESTOR_RESIZED:
              if (eventLog.isLoggable(PlatformLogger.Level.FINE)) {
                  if (changeFlags != 0) {
                      eventLog.fine("Assertion (changeFlags == 0) failed");
                  }
              }
              if (hierarchyBoundsListener != null ||
                  (eventMask & AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK) != 0 ||
                  enabledOnToolkit) {
                  HierarchyEvent e = new HierarchyEvent(this, id, changed,
                                                        changedParent);
                  dispatchEvent(e);
                  return 1;
              }
              break;
          default:
              // assert false
              if (eventLog.isLoggable(PlatformLogger.Level.FINE)) {
                  eventLog.fine("This code must never be reached");
              }
              break;
        }
        return 0;
    }

    /**
     * Returns an array of all the hierarchy bounds listeners
     * registered on this component.
     *
     * @return all of this component's {@code HierarchyBoundsListener}s
     *         or an empty array if no hierarchy bounds
     *         listeners are currently registered
     *
     * @see      #addHierarchyBoundsListener
     * @see      #removeHierarchyBoundsListener
     * @since    1.4
     */
    public synchronized HierarchyBoundsListener[] getHierarchyBoundsListeners() {
        return getListeners(HierarchyBoundsListener.class);
    }

    /*
     * Should only be called while holding the tree lock.
     * It's added only for overriding in java.awt.Window
     * because parent in Window is owner.
     */
    void adjustListeningChildrenOnParent(long mask, int num) {
        if (parent != null) {
            parent.adjustListeningChildren(mask, num);
        }
    }

    /**
     * Adds the specified key listener to receive key events from
     * this component.
     * If l is null, no exception is thrown and no action is performed.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param    l   the key listener.
     * @see      java.awt.event.KeyEvent
     * @see      java.awt.event.KeyListener
     * @see      #removeKeyListener
     * @see      #getKeyListeners
     * @since    1.1
     */
    public synchronized void addKeyListener(KeyListener l) {
        if (l == null) {
            return;
        }
        keyListener = AWTEventMulticaster.add(keyListener, l);
        newEventsOnly = true;

        // if this is a lightweight component, enable key events
        // in the native container.
        if (peer instanceof LightweightPeer) {
            parent.proxyEnableEvents(AWTEvent.KEY_EVENT_MASK);
        }
    }

    /**
     * Removes the specified key listener so that it no longer
     * receives key events from this component. This method performs
     * no function, nor does it throw an exception, if the listener
     * specified by the argument was not previously added to this component.
     * If listener {@code l} is {@code null},
     * no exception is thrown and no action is performed.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param    l   the key listener
     * @see      java.awt.event.KeyEvent
     * @see      java.awt.event.KeyListener
     * @see      #addKeyListener
     * @see      #getKeyListeners
     * @since    1.1
     */
    public synchronized void removeKeyListener(KeyListener l) {
        if (l == null) {
            return;
        }
        keyListener = AWTEventMulticaster.remove(keyListener, l);
    }

    /**
     * Returns an array of all the key listeners
     * registered on this component.
     *
     * @return all of this component's {@code KeyListener}s
     *         or an empty array if no key
     *         listeners are currently registered
     *
     * @see      #addKeyListener
     * @see      #removeKeyListener
     * @since    1.4
     */
    public synchronized KeyListener[] getKeyListeners() {
        return getListeners(KeyListener.class);
    }

    /**
     * Adds the specified mouse listener to receive mouse events from
     * this component.
     * If listener {@code l} is {@code null},
     * no exception is thrown and no action is performed.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param    l   the mouse listener
     * @see      java.awt.event.MouseEvent
     * @see      java.awt.event.MouseListener
     * @see      #removeMouseListener
     * @see      #getMouseListeners
     * @since    1.1
     */
    public synchronized void addMouseListener(MouseListener l) {
        if (l == null) {
            return;
        }
        mouseListener = AWTEventMulticaster.add(mouseListener,l);
        newEventsOnly = true;

        // if this is a lightweight component, enable mouse events
        // in the native container.
        if (peer instanceof LightweightPeer) {
            parent.proxyEnableEvents(AWTEvent.MOUSE_EVENT_MASK);
        }
    }

    /**
     * Removes the specified mouse listener so that it no longer
     * receives mouse events from this component. This method performs
     * no function, nor does it throw an exception, if the listener
     * specified by the argument was not previously added to this component.
     * If listener {@code l} is {@code null},
     * no exception is thrown and no action is performed.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param    l   the mouse listener
     * @see      java.awt.event.MouseEvent
     * @see      java.awt.event.MouseListener
     * @see      #addMouseListener
     * @see      #getMouseListeners
     * @since    1.1
     */
    public synchronized void removeMouseListener(MouseListener l) {
        if (l == null) {
            return;
        }
        mouseListener = AWTEventMulticaster.remove(mouseListener, l);
    }

    /**
     * Returns an array of all the mouse listeners
     * registered on this component.
     *
     * @return all of this component's {@code MouseListener}s
     *         or an empty array if no mouse
     *         listeners are currently registered
     *
     * @see      #addMouseListener
     * @see      #removeMouseListener
     * @since    1.4
     */
    public synchronized MouseListener[] getMouseListeners() {
        return getListeners(MouseListener.class);
    }

    /**
     * Adds the specified mouse motion listener to receive mouse motion
     * events from this component.
     * If listener {@code l} is {@code null},
     * no exception is thrown and no action is performed.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param    l   the mouse motion listener
     * @see      java.awt.event.MouseEvent
     * @see      java.awt.event.MouseMotionListener
     * @see      #removeMouseMotionListener
     * @see      #getMouseMotionListeners
     * @since    1.1
     */
    public synchronized void addMouseMotionListener(MouseMotionListener l) {
        if (l == null) {
            return;
        }
        mouseMotionListener = AWTEventMulticaster.add(mouseMotionListener,l);
        newEventsOnly = true;

        // if this is a lightweight component, enable mouse events
        // in the native container.
        if (peer instanceof LightweightPeer) {
            parent.proxyEnableEvents(AWTEvent.MOUSE_MOTION_EVENT_MASK);
        }
    }

    /**
     * Removes the specified mouse motion listener so that it no longer
     * receives mouse motion events from this component. This method performs
     * no function, nor does it throw an exception, if the listener
     * specified by the argument was not previously added to this component.
     * If listener {@code l} is {@code null},
     * no exception is thrown and no action is performed.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param    l   the mouse motion listener
     * @see      java.awt.event.MouseEvent
     * @see      java.awt.event.MouseMotionListener
     * @see      #addMouseMotionListener
     * @see      #getMouseMotionListeners
     * @since    1.1
     */
    public synchronized void removeMouseMotionListener(MouseMotionListener l) {
        if (l == null) {
            return;
        }
        mouseMotionListener = AWTEventMulticaster.remove(mouseMotionListener, l);
    }

    /**
     * Returns an array of all the mouse motion listeners
     * registered on this component.
     *
     * @return all of this component's {@code MouseMotionListener}s
     *         or an empty array if no mouse motion
     *         listeners are currently registered
     *
     * @see      #addMouseMotionListener
     * @see      #removeMouseMotionListener
     * @since    1.4
     */
    public synchronized MouseMotionListener[] getMouseMotionListeners() {
        return getListeners(MouseMotionListener.class);
    }

    /**
     * Adds the specified mouse wheel listener to receive mouse wheel events
     * from this component.  Containers also receive mouse wheel events from
     * sub-components.
     * <p>
     * For information on how mouse wheel events are dispatched, see
     * the class description for {@link MouseWheelEvent}.
     * <p>
     * If l is {@code null}, no exception is thrown and no
     * action is performed.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param    l   the mouse wheel listener
     * @see      java.awt.event.MouseWheelEvent
     * @see      java.awt.event.MouseWheelListener
     * @see      #removeMouseWheelListener
     * @see      #getMouseWheelListeners
     * @since    1.4
     */
    public synchronized void addMouseWheelListener(MouseWheelListener l) {
        if (l == null) {
            return;
        }
        mouseWheelListener = AWTEventMulticaster.add(mouseWheelListener,l);
        newEventsOnly = true;

        // if this is a lightweight component, enable mouse events
        // in the native container.
        if (peer instanceof LightweightPeer) {
            parent.proxyEnableEvents(AWTEvent.MOUSE_WHEEL_EVENT_MASK);
        }
    }

    /**
     * Removes the specified mouse wheel listener so that it no longer
     * receives mouse wheel events from this component. This method performs
     * no function, nor does it throw an exception, if the listener
     * specified by the argument was not previously added to this component.
     * If l is null, no exception is thrown and no action is performed.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param    l   the mouse wheel listener.
     * @see      java.awt.event.MouseWheelEvent
     * @see      java.awt.event.MouseWheelListener
     * @see      #addMouseWheelListener
     * @see      #getMouseWheelListeners
     * @since    1.4
     */
    public synchronized void removeMouseWheelListener(MouseWheelListener l) {
        if (l == null) {
            return;
        }
        mouseWheelListener = AWTEventMulticaster.remove(mouseWheelListener, l);
    }

    /**
     * Returns an array of all the mouse wheel listeners
     * registered on this component.
     *
     * @return all of this component's {@code MouseWheelListener}s
     *         or an empty array if no mouse wheel
     *         listeners are currently registered
     *
     * @see      #addMouseWheelListener
     * @see      #removeMouseWheelListener
     * @since    1.4
     */
    public synchronized MouseWheelListener[] getMouseWheelListeners() {
        return getListeners(MouseWheelListener.class);
    }

    /**
     * Adds the specified input method listener to receive
     * input method events from this component. A component will
     * only receive input method events from input methods
     * if it also overrides {@code getInputMethodRequests} to return an
     * {@code InputMethodRequests} instance.
     * If listener {@code l} is {@code null},
     * no exception is thrown and no action is performed.
     * <p>Refer to
     * <a href="{@docRoot}/java.desktop/java/awt/doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param    l   the input method listener
     * @see      java.awt.event.InputMethodEvent
     * @see      java.awt.event.InputMethodListener
     * @see      #removeInputMethodListener
     * @see      #getInputMethodListeners
     * @see      #getInputMethodRequests
     * @since    1.2
     */
    public synchronized void addInputMethodListener(InputMethodListener l) {
        if (l == null) {
            return;
        }
        inputMethodListener = AWTEventMulticaster.add(inputMethodListener, l);
        newEventsOnly = true;
    }

    /**
     * Removes the specified input method listener so that it no longer
     * receives input method events from this component. This method performs
     * no function, nor does it throw an exception, if the listener
     * specified by the argument was not previously added to this component.
     * If listener {@code l} is {@code null},
     * no exception is thrown and no action is performed.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param    l   the input method listener
     * @see      java.awt.event.InputMethodEvent
     * @see      java.awt.event.InputMethodListener
     * @see      #addInputMethodListener
     * @see      #getInputMethodListeners
     * @since    1.2
     */
    public synchronized void removeInputMethodListener(InputMethodListener l) {
        if (l == null) {
            return;
        }
        inputMethodListener = AWTEventMulticaster.remove(inputMethodListener, l);
    }

    /**
     * Returns an array of all the input method listeners
     * registered on this component.
     *
     * @return all of this component's {@code InputMethodListener}s
     *         or an empty array if no input method
     *         listeners are currently registered
     *
     * @see      #addInputMethodListener
     * @see      #removeInputMethodListener
     * @since    1.4
     */
    public synchronized InputMethodListener[] getInputMethodListeners() {
        return getListeners(InputMethodListener.class);
    }

    /**
     * Returns an array of all the objects currently registered
     * as <code><em>Foo</em>Listener</code>s
     * upon this {@code Component}.
     * <code><em>Foo</em>Listener</code>s are registered using the
     * <code>add<em>Foo</em>Listener</code> method.
     *
     * <p>
     * You can specify the {@code listenerType} argument
     * with a class literal, such as
     * <code><em>Foo</em>Listener.class</code>.
     * For example, you can query a
     * {@code Component c}
     * for its mouse listeners with the following code:
     *
     * <pre>MouseListener[] mls = (MouseListener[])(c.getListeners(MouseListener.class));</pre>
     *
     * If no such listeners exist, this method returns an empty array.
     *
     * @param <T> the type of the listeners
     * @param listenerType the type of listeners requested; this parameter
     *          should specify an interface that descends from
     *          {@code java.util.EventListener}
     * @return an array of all objects registered as
     *          <code><em>Foo</em>Listener</code>s on this component,
     *          or an empty array if no such listeners have been added
     * @exception ClassCastException if {@code listenerType}
     *          doesn't specify a class or interface that implements
     *          {@code java.util.EventListener}
     * @throws NullPointerException if {@code listenerType} is {@code null}
     * @see #getComponentListeners
     * @see #getFocusListeners
     * @see #getHierarchyListeners
     * @see #getHierarchyBoundsListeners
     * @see #getKeyListeners
     * @see #getMouseListeners
     * @see #getMouseMotionListeners
     * @see #getMouseWheelListeners
     * @see #getInputMethodListeners
     * @see #getPropertyChangeListeners
     *
     * @since 1.3
     */
    @SuppressWarnings("unchecked")
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        EventListener l = null;
        if  (listenerType == ComponentListener.class) {
            l = componentListener;
        } else if (listenerType == FocusListener.class) {
            l = focusListener;
        } else if (listenerType == HierarchyListener.class) {
            l = hierarchyListener;
        } else if (listenerType == HierarchyBoundsListener.class) {
            l = hierarchyBoundsListener;
        } else if (listenerType == KeyListener.class) {
            l = keyListener;
        } else if (listenerType == MouseListener.class) {
            l = mouseListener;
        } else if (listenerType == MouseMotionListener.class) {
            l = mouseMotionListener;
        } else if (listenerType == MouseWheelListener.class) {
            l = mouseWheelListener;
        } else if (listenerType == InputMethodListener.class) {
            l = inputMethodListener;
        } else if (listenerType == PropertyChangeListener.class) {
            return (T[])getPropertyChangeListeners();
        }
        return AWTEventMulticaster.getListeners(l, listenerType);
    }

    /**
     * Gets the input method request handler which supports
     * requests from input methods for this component. A component
     * that supports on-the-spot text input must override this
     * method to return an {@code InputMethodRequests} instance.
     * At the same time, it also has to handle input method events.
     *
     * @return the input method request handler for this component,
     *          {@code null} by default
     * @see #addInputMethodListener
     * @since 1.2
     */
    public InputMethodRequests getInputMethodRequests() {
        return null;
    }

    /**
     * Gets the input context used by this component for handling
     * the communication with input methods when text is entered
     * in this component. By default, the input context used for
     * the parent component is returned. Components may
     * override this to return a private input context.
     *
     * @return the input context used by this component;
     *          {@code null} if no context can be determined
     * @since 1.2
     */
    public InputContext getInputContext() {
        Container parent = this.parent;
        if (parent == null) {
            return null;
        } else {
            return parent.getInputContext();
        }
    }

    /**
     * Enables the events defined by the specified event mask parameter
     * to be delivered to this component.
     * <p>
     * Event types are automatically enabled when a listener for
     * that event type is added to the component.
     * <p>
     * This method only needs to be invoked by subclasses of
     * {@code Component} which desire to have the specified event
     * types delivered to {@code processEvent} regardless of whether
     * or not a listener is registered.
     * @param      eventsToEnable   the event mask defining the event types
     * @see        #processEvent
     * @see        #disableEvents
     * @see        AWTEvent
     * @since      1.1
     */
    protected final void enableEvents(long eventsToEnable) {
        long notifyAncestors = 0;
        synchronized (this) {
            if ((eventsToEnable & AWTEvent.HIERARCHY_EVENT_MASK) != 0 &&
                hierarchyListener == null &&
                (eventMask & AWTEvent.HIERARCHY_EVENT_MASK) == 0) {
                notifyAncestors |= AWTEvent.HIERARCHY_EVENT_MASK;
            }
            if ((eventsToEnable & AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK) != 0 &&
                hierarchyBoundsListener == null &&
                (eventMask & AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK) == 0) {
                notifyAncestors |= AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK;
            }
            eventMask |= eventsToEnable;
            newEventsOnly = true;
        }

        // if this is a lightweight component, enable mouse events
        // in the native container.
        if (peer instanceof LightweightPeer) {
            parent.proxyEnableEvents(eventMask);
        }
        if (notifyAncestors != 0) {
            synchronized (getTreeLock()) {
                adjustListeningChildrenOnParent(notifyAncestors, 1);
            }
        }
    }

    /**
     * Disables the events defined by the specified event mask parameter
     * from being delivered to this component.
     * @param      eventsToDisable   the event mask defining the event types
     * @see        #enableEvents
     * @since      1.1
     */
    protected final void disableEvents(long eventsToDisable) {
        long notifyAncestors = 0;
        synchronized (this) {
            if ((eventsToDisable & AWTEvent.HIERARCHY_EVENT_MASK) != 0 &&
                hierarchyListener == null &&
                (eventMask & AWTEvent.HIERARCHY_EVENT_MASK) != 0) {
                notifyAncestors |= AWTEvent.HIERARCHY_EVENT_MASK;
            }
            if ((eventsToDisable & AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK)!=0 &&
                hierarchyBoundsListener == null &&
                (eventMask & AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK) != 0) {
                notifyAncestors |= AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK;
            }
            eventMask &= ~eventsToDisable;
        }
        if (notifyAncestors != 0) {
            synchronized (getTreeLock()) {
                adjustListeningChildrenOnParent(notifyAncestors, -1);
            }
        }
    }

    transient sun.awt.EventQueueItem[] eventCache;

    /**
     * @see #isCoalescingEnabled
     * @see #checkCoalescing
     */
    private transient boolean coalescingEnabled = checkCoalescing();

    /**
     * Weak map of known coalesceEvent overriders.
     * Value indicates whether overriden.
     * Bootstrap classes are not included.
     */
    private static final Map<Class<?>, Boolean> coalesceMap =
        new java.util.WeakHashMap<Class<?>, Boolean>();

    /**
     * Indicates whether this class overrides coalesceEvents.
     * It is assumed that all classes that are loaded from the bootstrap
     *   do not.
     * The bootstrap class loader is assumed to be represented by null.
     * We do not check that the method really overrides
     *   (it might be static, private or package private).
     */
     private boolean checkCoalescing() {
         if (getClass().getClassLoader()==null) {
             return false;
         }
         final Class<? extends Component> clazz = getClass();
         synchronized (coalesceMap) {
             // Check cache.
             Boolean value = coalesceMap.get(clazz);
             if (value != null) {
                 return value;
             }

             // Need to check non-bootstraps.
             @SuppressWarnings("removal")
             Boolean enabled = java.security.AccessController.doPrivileged(
                 new java.security.PrivilegedAction<Boolean>() {
                     public Boolean run() {
                         return isCoalesceEventsOverriden(clazz);
                     }
                 }
                 );
             coalesceMap.put(clazz, enabled);
             return enabled;
         }
     }

    /**
     * Parameter types of coalesceEvents(AWTEvent,AWTEVent).
     */
    private static final Class<?>[] coalesceEventsParams = {
        AWTEvent.class, AWTEvent.class
    };

    /**
     * Indicates whether a class or its superclasses override coalesceEvents.
     * Must be called with lock on coalesceMap and privileged.
     * @see #checkCoalescing
     */
    private static boolean isCoalesceEventsOverriden(Class<?> clazz) {
        assert Thread.holdsLock(coalesceMap);

        // First check superclass - we may not need to bother ourselves.
        Class<?> superclass = clazz.getSuperclass();
        if (superclass == null) {
            // Only occurs on implementations that
            //   do not use null to represent the bootstrap class loader.
            return false;
        }
        if (superclass.getClassLoader() != null) {
            Boolean value = coalesceMap.get(superclass);
            if (value == null) {
                // Not done already - recurse.
                if (isCoalesceEventsOverriden(superclass)) {
                    coalesceMap.put(superclass, true);
                    return true;
                }
            } else if (value) {
                return true;
            }
        }

        try {
            // Throws if not overriden.
            clazz.getDeclaredMethod(
                "coalesceEvents", coalesceEventsParams
                );
            return true;
        } catch (NoSuchMethodException e) {
            // Not present in this class.
            return false;
        }
    }

    /**
     * Indicates whether coalesceEvents may do something.
     */
    final boolean isCoalescingEnabled() {
        return coalescingEnabled;
     }


    /**
     * Potentially coalesce an event being posted with an existing
     * event.  This method is called by {@code EventQueue.postEvent}
     * if an event with the same ID as the event to be posted is found in
     * the queue (both events must have this component as their source).
     * This method either returns a coalesced event which replaces
     * the existing event (and the new event is then discarded), or
     * {@code null} to indicate that no combining should be done
     * (add the second event to the end of the queue).  Either event
     * parameter may be modified and returned, as the other one is discarded
     * unless {@code null} is returned.
     * <p>
     * This implementation of {@code coalesceEvents} coalesces
     * two event types: mouse move (and drag) events,
     * and paint (and update) events.
     * For mouse move events the last event is always returned, causing
     * intermediate moves to be discarded.  For paint events, the new
     * event is coalesced into a complex {@code RepaintArea} in the peer.
     * The new {@code AWTEvent} is always returned.
     *
     * @param  existingEvent  the event already on the {@code EventQueue}
     * @param  newEvent       the event being posted to the
     *          {@code EventQueue}
     * @return a coalesced event, or {@code null} indicating that no
     *          coalescing was done
     */
    protected AWTEvent coalesceEvents(AWTEvent existingEvent,
                                      AWTEvent newEvent) {
        return null;
    }

    /**
     * Processes events occurring on this component. By default this
     * method calls the appropriate
     * <code>process&lt;event&nbsp;type&gt;Event</code>
     * method for the given class of event.
     * <p>Note that if the event parameter is {@code null}
     * the behavior is unspecified and may result in an
     * exception.
     *
     * @param     e the event
     * @see       #processComponentEvent
     * @see       #processFocusEvent
     * @see       #processKeyEvent
     * @see       #processMouseEvent
     * @see       #processMouseMotionEvent
     * @see       #processInputMethodEvent
     * @see       #processHierarchyEvent
     * @see       #processMouseWheelEvent
     * @since     1.1
     */
    protected void processEvent(AWTEvent e) {
        if (e instanceof FocusEvent) {
            processFocusEvent((FocusEvent)e);

        } else if (e instanceof MouseEvent) {
            switch(e.getID()) {
              case MouseEvent.MOUSE_PRESSED:
              case MouseEvent.MOUSE_RELEASED:
              case MouseEvent.MOUSE_CLICKED:
              case MouseEvent.MOUSE_ENTERED:
              case MouseEvent.MOUSE_EXITED:
                  processMouseEvent((MouseEvent)e);
                  break;
              case MouseEvent.MOUSE_MOVED:
              case MouseEvent.MOUSE_DRAGGED:
                  processMouseMotionEvent((MouseEvent)e);
                  break;
              case MouseEvent.MOUSE_WHEEL:
                  processMouseWheelEvent((MouseWheelEvent)e);
                  break;
            }

        } else if (e instanceof KeyEvent) {
            processKeyEvent((KeyEvent)e);

        } else if (e instanceof ComponentEvent) {
            processComponentEvent((ComponentEvent)e);
        } else if (e instanceof InputMethodEvent) {
            processInputMethodEvent((InputMethodEvent)e);
        } else if (e instanceof HierarchyEvent) {
            switch (e.getID()) {
              case HierarchyEvent.HIERARCHY_CHANGED:
                  processHierarchyEvent((HierarchyEvent)e);
                  break;
              case HierarchyEvent.ANCESTOR_MOVED:
              case HierarchyEvent.ANCESTOR_RESIZED:
                  processHierarchyBoundsEvent((HierarchyEvent)e);
                  break;
            }
        }
    }

    /**
     * Processes component events occurring on this component by
     * dispatching them to any registered
     * {@code ComponentListener} objects.
     * <p>
     * This method is not called unless component events are
     * enabled for this component. Component events are enabled
     * when one of the following occurs:
     * <ul>
     * <li>A {@code ComponentListener} object is registered
     * via {@code addComponentListener}.
     * <li>Component events are enabled via {@code enableEvents}.
     * </ul>
     * <p>Note that if the event parameter is {@code null}
     * the behavior is unspecified and may result in an
     * exception.
     *
     * @param       e the component event
     * @see         java.awt.event.ComponentEvent
     * @see         java.awt.event.ComponentListener
     * @see         #addComponentListener
     * @see         #enableEvents
     * @since       1.1
     */
    protected void processComponentEvent(ComponentEvent e) {
        ComponentListener listener = componentListener;
        if (listener != null) {
            int id = e.getID();
            switch(id) {
              case ComponentEvent.COMPONENT_RESIZED:
                  listener.componentResized(e);
                  break;
              case ComponentEvent.COMPONENT_MOVED:
                  listener.componentMoved(e);
                  break;
              case ComponentEvent.COMPONENT_SHOWN:
                  listener.componentShown(e);
                  break;
              case ComponentEvent.COMPONENT_HIDDEN:
                  listener.componentHidden(e);
                  break;
            }
        }
    }

    /**
     * Processes focus events occurring on this component by
     * dispatching them to any registered
     * {@code FocusListener} objects.
     * <p>
     * This method is not called unless focus events are
     * enabled for this component. Focus events are enabled
     * when one of the following occurs:
     * <ul>
     * <li>A {@code FocusListener} object is registered
     * via {@code addFocusListener}.
     * <li>Focus events are enabled via {@code enableEvents}.
     * </ul>
     * <p>
     * If focus events are enabled for a {@code Component},
     * the current {@code KeyboardFocusManager} determines
     * whether or not a focus event should be dispatched to
     * registered {@code FocusListener} objects.  If the
     * events are to be dispatched, the {@code KeyboardFocusManager}
     * calls the {@code Component}'s {@code dispatchEvent}
     * method, which results in a call to the {@code Component}'s
     * {@code processFocusEvent} method.
     * <p>
     * If focus events are enabled for a {@code Component}, calling
     * the {@code Component}'s {@code dispatchEvent} method
     * with a {@code FocusEvent} as the argument will result in a
     * call to the {@code Component}'s {@code processFocusEvent}
     * method regardless of the current {@code KeyboardFocusManager}.
     *
     * <p>Note that if the event parameter is {@code null}
     * the behavior is unspecified and may result in an
     * exception.
     *
     * @param       e the focus event
     * @see         java.awt.event.FocusEvent
     * @see         java.awt.event.FocusListener
     * @see         java.awt.KeyboardFocusManager
     * @see         #addFocusListener
     * @see         #enableEvents
     * @see         #dispatchEvent
     * @since       1.1
     */
    protected void processFocusEvent(FocusEvent e) {
        FocusListener listener = focusListener;
        if (listener != null) {
            int id = e.getID();
            switch(id) {
              case FocusEvent.FOCUS_GAINED:
                  listener.focusGained(e);
                  break;
              case FocusEvent.FOCUS_LOST:
                  listener.focusLost(e);
                  break;
            }
        }
    }

    /**
     * Processes key events occurring on this component by
     * dispatching them to any registered
     * {@code KeyListener} objects.
     * <p>
     * This method is not called unless key events are
     * enabled for this component. Key events are enabled
     * when one of the following occurs:
     * <ul>
     * <li>A {@code KeyListener} object is registered
     * via {@code addKeyListener}.
     * <li>Key events are enabled via {@code enableEvents}.
     * </ul>
     *
     * <p>
     * If key events are enabled for a {@code Component},
     * the current {@code KeyboardFocusManager} determines
     * whether or not a key event should be dispatched to
     * registered {@code KeyListener} objects.  The
     * {@code DefaultKeyboardFocusManager} will not dispatch
     * key events to a {@code Component} that is not the focus
     * owner or is not showing.
     * <p>
     * As of J2SE 1.4, {@code KeyEvent}s are redirected to
     * the focus owner. Please see the
     * <a href="doc-files/FocusSpec.html">Focus Specification</a>
     * for further information.
     * <p>
     * Calling a {@code Component}'s {@code dispatchEvent}
     * method with a {@code KeyEvent} as the argument will
     * result in a call to the {@code Component}'s
     * {@code processKeyEvent} method regardless of the
     * current {@code KeyboardFocusManager} as long as the
     * component is showing, focused, and enabled, and key events
     * are enabled on it.
     * <p>If the event parameter is {@code null}
     * the behavior is unspecified and may result in an
     * exception.
     *
     * @param       e the key event
     * @see         java.awt.event.KeyEvent
     * @see         java.awt.event.KeyListener
     * @see         java.awt.KeyboardFocusManager
     * @see         java.awt.DefaultKeyboardFocusManager
     * @see         #processEvent
     * @see         #dispatchEvent
     * @see         #addKeyListener
     * @see         #enableEvents
     * @see         #isShowing
     * @since       1.1
     */
    protected void processKeyEvent(KeyEvent e) {
        KeyListener listener = keyListener;
        if (listener != null) {
            int id = e.getID();
            switch(id) {
              case KeyEvent.KEY_TYPED:
                  listener.keyTyped(e);
                  break;
              case KeyEvent.KEY_PRESSED:
                  listener.keyPressed(e);
                  break;
              case KeyEvent.KEY_RELEASED:
                  listener.keyReleased(e);
                  break;
            }
        }
    }

    /**
     * Processes mouse events occurring on this component by
     * dispatching them to any registered
     * {@code MouseListener} objects.
     * <p>
     * This method is not called unless mouse events are
     * enabled for this component. Mouse events are enabled
     * when one of the following occurs:
     * <ul>
     * <li>A {@code MouseListener} object is registered
     * via {@code addMouseListener}.
     * <li>Mouse events are enabled via {@code enableEvents}.
     * </ul>
     * <p>Note that if the event parameter is {@code null}
     * the behavior is unspecified and may result in an
     * exception.
     *
     * @param       e the mouse event
     * @see         java.awt.event.MouseEvent
     * @see         java.awt.event.MouseListener
     * @see         #addMouseListener
     * @see         #enableEvents
     * @since       1.1
     */
    protected void processMouseEvent(MouseEvent e) {
        MouseListener listener = mouseListener;
        if (listener != null) {
            int id = e.getID();
            switch(id) {
              case MouseEvent.MOUSE_PRESSED:
                  listener.mousePressed(e);
                  break;
              case MouseEvent.MOUSE_RELEASED:
                  listener.mouseReleased(e);
                  break;
              case MouseEvent.MOUSE_CLICKED:
                  listener.mouseClicked(e);
                  break;
              case MouseEvent.MOUSE_EXITED:
                  listener.mouseExited(e);
                  break;
              case MouseEvent.MOUSE_ENTERED:
                  listener.mouseEntered(e);
                  break;
            }
        }
    }

    /**
     * Processes mouse motion events occurring on this component by
     * dispatching them to any registered
     * {@code MouseMotionListener} objects.
     * <p>
     * This method is not called unless mouse motion events are
     * enabled for this component. Mouse motion events are enabled
     * when one of the following occurs:
     * <ul>
     * <li>A {@code MouseMotionListener} object is registered
     * via {@code addMouseMotionListener}.
     * <li>Mouse motion events are enabled via {@code enableEvents}.
     * </ul>
     * <p>Note that if the event parameter is {@code null}
     * the behavior is unspecified and may result in an
     * exception.
     *
     * @param       e the mouse motion event
     * @see         java.awt.event.MouseEvent
     * @see         java.awt.event.MouseMotionListener
     * @see         #addMouseMotionListener
     * @see         #enableEvents
     * @since       1.1
     */
    protected void processMouseMotionEvent(MouseEvent e) {
        MouseMotionListener listener = mouseMotionListener;
        if (listener != null) {
            int id = e.getID();
            switch(id) {
              case MouseEvent.MOUSE_MOVED:
                  listener.mouseMoved(e);
                  break;
              case MouseEvent.MOUSE_DRAGGED:
                  listener.mouseDragged(e);
                  break;
            }
        }
    }

    /**
     * Processes mouse wheel events occurring on this component by
     * dispatching them to any registered
     * {@code MouseWheelListener} objects.
     * <p>
     * This method is not called unless mouse wheel events are
     * enabled for this component. Mouse wheel events are enabled
     * when one of the following occurs:
     * <ul>
     * <li>A {@code MouseWheelListener} object is registered
     * via {@code addMouseWheelListener}.
     * <li>Mouse wheel events are enabled via {@code enableEvents}.
     * </ul>
     * <p>
     * For information on how mouse wheel events are dispatched, see
     * the class description for {@link MouseWheelEvent}.
     * <p>
     * Note that if the event parameter is {@code null}
     * the behavior is unspecified and may result in an
     * exception.
     *
     * @param       e the mouse wheel event
     * @see         java.awt.event.MouseWheelEvent
     * @see         java.awt.event.MouseWheelListener
     * @see         #addMouseWheelListener
     * @see         #enableEvents
     * @since       1.4
     */
    protected void processMouseWheelEvent(MouseWheelEvent e) {
        MouseWheelListener listener = mouseWheelListener;
        if (listener != null) {
            int id = e.getID();
            switch(id) {
              case MouseEvent.MOUSE_WHEEL:
                  listener.mouseWheelMoved(e);
                  break;
            }
        }
    }

    boolean postsOldMouseEvents() {
        return false;
    }

    /**
     * Processes input method events occurring on this component by
     * dispatching them to any registered
     * {@code InputMethodListener} objects.
     * <p>
     * This method is not called unless input method events
     * are enabled for this component. Input method events are enabled
     * when one of the following occurs:
     * <ul>
     * <li>An {@code InputMethodListener} object is registered
     * via {@code addInputMethodListener}.
     * <li>Input method events are enabled via {@code enableEvents}.
     * </ul>
     * <p>Note that if the event parameter is {@code null}
     * the behavior is unspecified and may result in an
     * exception.
     *
     * @param       e the input method event
     * @see         java.awt.event.InputMethodEvent
     * @see         java.awt.event.InputMethodListener
     * @see         #addInputMethodListener
     * @see         #enableEvents
     * @since       1.2
     */
    protected void processInputMethodEvent(InputMethodEvent e) {
        InputMethodListener listener = inputMethodListener;
        if (listener != null) {
            int id = e.getID();
            switch (id) {
              case InputMethodEvent.INPUT_METHOD_TEXT_CHANGED:
                  listener.inputMethodTextChanged(e);
                  break;
              case InputMethodEvent.CARET_POSITION_CHANGED:
                  listener.caretPositionChanged(e);
                  break;
            }
        }
    }

    /**
     * Processes hierarchy events occurring on this component by
     * dispatching them to any registered
     * {@code HierarchyListener} objects.
     * <p>
     * This method is not called unless hierarchy events
     * are enabled for this component. Hierarchy events are enabled
     * when one of the following occurs:
     * <ul>
     * <li>An {@code HierarchyListener} object is registered
     * via {@code addHierarchyListener}.
     * <li>Hierarchy events are enabled via {@code enableEvents}.
     * </ul>
     * <p>Note that if the event parameter is {@code null}
     * the behavior is unspecified and may result in an
     * exception.
     *
     * @param       e the hierarchy event
     * @see         java.awt.event.HierarchyEvent
     * @see         java.awt.event.HierarchyListener
     * @see         #addHierarchyListener
     * @see         #enableEvents
     * @since       1.3
     */
    protected void processHierarchyEvent(HierarchyEvent e) {
        HierarchyListener listener = hierarchyListener;
        if (listener != null) {
            int id = e.getID();
            switch (id) {
              case HierarchyEvent.HIERARCHY_CHANGED:
                  listener.hierarchyChanged(e);
                  break;
            }
        }
    }

    /**
     * Processes hierarchy bounds events occurring on this component by
     * dispatching them to any registered
     * {@code HierarchyBoundsListener} objects.
     * <p>
     * This method is not called unless hierarchy bounds events
     * are enabled for this component. Hierarchy bounds events are enabled
     * when one of the following occurs:
     * <ul>
     * <li>An {@code HierarchyBoundsListener} object is registered
     * via {@code addHierarchyBoundsListener}.
     * <li>Hierarchy bounds events are enabled via {@code enableEvents}.
     * </ul>
     * <p>Note that if the event parameter is {@code null}
     * the behavior is unspecified and may result in an
     * exception.
     *
     * @param       e the hierarchy event
     * @see         java.awt.event.HierarchyEvent
     * @see         java.awt.event.HierarchyBoundsListener
     * @see         #addHierarchyBoundsListener
     * @see         #enableEvents
     * @since       1.3
     */
    protected void processHierarchyBoundsEvent(HierarchyEvent e) {
        HierarchyBoundsListener listener = hierarchyBoundsListener;
        if (listener != null) {
            int id = e.getID();
            switch (id) {
              case HierarchyEvent.ANCESTOR_MOVED:
                  listener.ancestorMoved(e);
                  break;
              case HierarchyEvent.ANCESTOR_RESIZED:
                  listener.ancestorResized(e);
                  break;
            }
        }
    }

    /**
     * @param  evt the event to handle
     * @return {@code true} if the event was handled, {@code false} otherwise
     * @deprecated As of JDK version 1.1
     * replaced by processEvent(AWTEvent).
     */
    @Deprecated
    public boolean handleEvent(Event evt) {
        switch (evt.id) {
          case Event.MOUSE_ENTER:
              return mouseEnter(evt, evt.x, evt.y);

          case Event.MOUSE_EXIT:
              return mouseExit(evt, evt.x, evt.y);

          case Event.MOUSE_MOVE:
              return mouseMove(evt, evt.x, evt.y);

          case Event.MOUSE_DOWN:
              return mouseDown(evt, evt.x, evt.y);

          case Event.MOUSE_DRAG:
              return mouseDrag(evt, evt.x, evt.y);

          case Event.MOUSE_UP:
              return mouseUp(evt, evt.x, evt.y);

          case Event.KEY_PRESS:
          case Event.KEY_ACTION:
              return keyDown(evt, evt.key);

          case Event.KEY_RELEASE:
          case Event.KEY_ACTION_RELEASE:
              return keyUp(evt, evt.key);

          case Event.ACTION_EVENT:
              return action(evt, evt.arg);
          case Event.GOT_FOCUS:
              return gotFocus(evt, evt.arg);
          case Event.LOST_FOCUS:
              return lostFocus(evt, evt.arg);
        }
        return false;
    }

    /**
     * @param  evt the event to handle
     * @param  x the x coordinate
     * @param  y the y coordinate
     * @return {@code false}
     * @deprecated As of JDK version 1.1,
     * replaced by processMouseEvent(MouseEvent).
     */
    @Deprecated
    public boolean mouseDown(Event evt, int x, int y) {
        return false;
    }

    /**
     * @param  evt the event to handle
     * @param  x the x coordinate
     * @param  y the y coordinate
     * @return {@code false}
     * @deprecated As of JDK version 1.1,
     * replaced by processMouseMotionEvent(MouseEvent).
     */
    @Deprecated
    public boolean mouseDrag(Event evt, int x, int y) {
        return false;
    }

    /**
     * @param  evt the event to handle
     * @param  x the x coordinate
     * @param  y the y coordinate
     * @return {@code false}
     * @deprecated As of JDK version 1.1,
     * replaced by processMouseEvent(MouseEvent).
     */
    @Deprecated
    public boolean mouseUp(Event evt, int x, int y) {
        return false;
    }

    /**
     * @param  evt the event to handle
     * @param  x the x coordinate
     * @param  y the y coordinate
     * @return {@code false}
     * @deprecated As of JDK version 1.1,
     * replaced by processMouseMotionEvent(MouseEvent).
     */
    @Deprecated
    public boolean mouseMove(Event evt, int x, int y) {
        return false;
    }

    /**
     * @param  evt the event to handle
     * @param  x the x coordinate
     * @param  y the y coordinate
     * @return {@code false}
     * @deprecated As of JDK version 1.1,
     * replaced by processMouseEvent(MouseEvent).
     */
    @Deprecated
    public boolean mouseEnter(Event evt, int x, int y) {
        return false;
    }

    /**
     * @param  evt the event to handle
     * @param  x the x coordinate
     * @param  y the y coordinate
     * @return {@code false}
     * @deprecated As of JDK version 1.1,
     * replaced by processMouseEvent(MouseEvent).
     */
    @Deprecated
    public boolean mouseExit(Event evt, int x, int y) {
        return false;
    }

    /**
     * @param  evt the event to handle
     * @param  key the key pressed
     * @return {@code false}
     * @deprecated As of JDK version 1.1,
     * replaced by processKeyEvent(KeyEvent).
     */
    @Deprecated
    public boolean keyDown(Event evt, int key) {
        return false;
    }

    /**
     * @param  evt the event to handle
     * @param  key the key pressed
     * @return {@code false}
     * @deprecated As of JDK version 1.1,
     * replaced by processKeyEvent(KeyEvent).
     */
    @Deprecated
    public boolean keyUp(Event evt, int key) {
        return false;
    }

    /**
     * @param  evt the event to handle
     * @param  what the object acted on
     * @return {@code false}
     * @deprecated As of JDK version 1.1,
     * should register this component as ActionListener on component
     * which fires action events.
     */
    @Deprecated
    public boolean action(Event evt, Object what) {
        return false;
    }

    /**
     * Makes this {@code Component} displayable by connecting it to a
     * native screen resource.
     * This method is called internally by the toolkit and should
     * not be called directly by programs.
     * <p>
     * This method changes layout-related information, and therefore,
     * invalidates the component hierarchy.
     *
     * @see       #isDisplayable
     * @see       #removeNotify
     * @see #invalidate
     * @since 1.0
     */
    public void addNotify() {
        synchronized (getTreeLock()) {
            ComponentPeer peer = this.peer;
            if (peer == null || peer instanceof LightweightPeer){
                if (peer == null) {
                    // Update both the Component's peer variable and the local
                    // variable we use for thread safety.
                    this.peer = peer = getComponentFactory().createComponent(this);
                }

                // This is a lightweight component which means it won't be
                // able to get window-related events by itself.  If any
                // have been enabled, then the nearest native container must
                // be enabled.
                if (parent != null) {
                    long mask = 0;
                    if ((mouseListener != null) || ((eventMask & AWTEvent.MOUSE_EVENT_MASK) != 0)) {
                        mask |= AWTEvent.MOUSE_EVENT_MASK;
                    }
                    if ((mouseMotionListener != null) ||
                        ((eventMask & AWTEvent.MOUSE_MOTION_EVENT_MASK) != 0)) {
                        mask |= AWTEvent.MOUSE_MOTION_EVENT_MASK;
                    }
                    if ((mouseWheelListener != null ) ||
                        ((eventMask & AWTEvent.MOUSE_WHEEL_EVENT_MASK) != 0)) {
                        mask |= AWTEvent.MOUSE_WHEEL_EVENT_MASK;
                    }
                    if (focusListener != null || (eventMask & AWTEvent.FOCUS_EVENT_MASK) != 0) {
                        mask |= AWTEvent.FOCUS_EVENT_MASK;
                    }
                    if (keyListener != null || (eventMask & AWTEvent.KEY_EVENT_MASK) != 0) {
                        mask |= AWTEvent.KEY_EVENT_MASK;
                    }
                    if (mask != 0) {
                        parent.proxyEnableEvents(mask);
                    }
                }
            } else {
                // It's native. If the parent is lightweight it will need some
                // help.
                Container parent = getContainer();
                if (parent != null && parent.isLightweight()) {
                    relocateComponent();
                    if (!parent.isRecursivelyVisibleUpToHeavyweightContainer())
                    {
                        peer.setVisible(false);
                    }
                }
            }
            invalidate();

            int npopups = (popups != null? popups.size() : 0);
            for (int i = 0 ; i < npopups ; i++) {
                PopupMenu popup = popups.elementAt(i);
                popup.addNotify();
            }

            if (dropTarget != null) dropTarget.addNotify();

            peerFont = getFont();

            if (getContainer() != null && !isAddNotifyComplete) {
                getContainer().increaseComponentCount(this);
            }


            // Update stacking order
            updateZOrder();

            if (!isAddNotifyComplete) {
                mixOnShowing();
            }

            isAddNotifyComplete = true;

            if (hierarchyListener != null ||
                (eventMask & AWTEvent.HIERARCHY_EVENT_MASK) != 0 ||
                Toolkit.enabledOnToolkit(AWTEvent.HIERARCHY_EVENT_MASK)) {
                HierarchyEvent e =
                    new HierarchyEvent(this, HierarchyEvent.HIERARCHY_CHANGED,
                                       this, parent,
                                       HierarchyEvent.DISPLAYABILITY_CHANGED |
                                       ((isRecursivelyVisible())
                                        ? HierarchyEvent.SHOWING_CHANGED
                                        : 0));
                dispatchEvent(e);
            }
        }
    }

    /**
     * Makes this {@code Component} undisplayable by destroying it native
     * screen resource.
     * <p>
     * This method is called by the toolkit internally and should
     * not be called directly by programs. Code overriding
     * this method should call {@code super.removeNotify} as
     * the first line of the overriding method.
     *
     * @see       #isDisplayable
     * @see       #addNotify
     * @since 1.0
     */
    public void removeNotify() {
        KeyboardFocusManager.clearMostRecentFocusOwner(this);
        if (KeyboardFocusManager.getCurrentKeyboardFocusManager().
            getPermanentFocusOwner() == this)
        {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().
                setGlobalPermanentFocusOwner(null);
        }

        synchronized (getTreeLock()) {
            if (isFocusOwner() && KeyboardFocusManager.isAutoFocusTransferEnabledFor(this)) {
                transferFocus(true);
            }

            if (getContainer() != null && isAddNotifyComplete) {
                getContainer().decreaseComponentCount(this);
            }

            int npopups = (popups != null? popups.size() : 0);
            for (int i = 0 ; i < npopups ; i++) {
                PopupMenu popup = popups.elementAt(i);
                popup.removeNotify();
            }
            // If there is any input context for this component, notify
            // that this component is being removed. (This has to be done
            // before hiding peer.)
            if ((eventMask & AWTEvent.INPUT_METHODS_ENABLED_MASK) != 0) {
                InputContext inputContext = getInputContext();
                if (inputContext != null) {
                    inputContext.removeNotify(this);
                }
            }

            ComponentPeer p = peer;
            if (p != null) {
                boolean isLightweight = isLightweight();

                if (bufferStrategy instanceof FlipBufferStrategy) {
                    ((FlipBufferStrategy)bufferStrategy).invalidate();
                }

                if (dropTarget != null) dropTarget.removeNotify();

                // Hide peer first to stop system events such as cursor moves.
                if (visible) {
                    p.setVisible(false);
                }

                peer = null; // Stop peer updates.
                peerFont = null;

                Toolkit.getEventQueue().removeSourceEvents(this, false);
                KeyboardFocusManager.getCurrentKeyboardFocusManager().
                    discardKeyEvents(this);

                p.dispose();

                mixOnHiding(isLightweight);

                isAddNotifyComplete = false;
                // Nullifying compoundShape means that the component has normal shape
                // (or has no shape at all).
                this.compoundShape = null;
            }

            if (hierarchyListener != null ||
                (eventMask & AWTEvent.HIERARCHY_EVENT_MASK) != 0 ||
                Toolkit.enabledOnToolkit(AWTEvent.HIERARCHY_EVENT_MASK)) {
                HierarchyEvent e =
                    new HierarchyEvent(this, HierarchyEvent.HIERARCHY_CHANGED,
                                       this, parent,
                                       HierarchyEvent.DISPLAYABILITY_CHANGED |
                                       ((isRecursivelyVisible())
                                        ? HierarchyEvent.SHOWING_CHANGED
                                        : 0));
                dispatchEvent(e);
            }
        }
    }

    /**
     * @param  evt the event to handle
     * @param  what the object focused
     * @return  {@code false}
     * @deprecated As of JDK version 1.1,
     * replaced by processFocusEvent(FocusEvent).
     */
    @Deprecated
    public boolean gotFocus(Event evt, Object what) {
        return false;
    }

    /**
     * @param evt  the event to handle
     * @param what the object focused
     * @return  {@code false}
     * @deprecated As of JDK version 1.1,
     * replaced by processFocusEvent(FocusEvent).
     */
    @Deprecated
    public boolean lostFocus(Event evt, Object what) {
        return false;
    }

    /**
     * Returns whether this {@code Component} can become the focus
     * owner.
     *
     * @return {@code true} if this {@code Component} is
     * focusable; {@code false} otherwise
     * @see #setFocusable
     * @since 1.1
     * @deprecated As of 1.4, replaced by {@code isFocusable()}.
     */
    @Deprecated
    public boolean isFocusTraversable() {
        if (isFocusTraversableOverridden == FOCUS_TRAVERSABLE_UNKNOWN) {
            isFocusTraversableOverridden = FOCUS_TRAVERSABLE_DEFAULT;
        }
        return focusable;
    }

    /**
     * Returns whether this Component can be focused.
     *
     * @return {@code true} if this Component is focusable;
     *         {@code false} otherwise.
     * @see #setFocusable
     * @since 1.4
     */
    public boolean isFocusable() {
        return isFocusTraversable();
    }

    /**
     * Sets the focusable state of this Component to the specified value. This
     * value overrides the Component's default focusability.
     *
     * @param focusable indicates whether this Component is focusable
     * @see #isFocusable
     * @since 1.4
     */
    public void setFocusable(boolean focusable) {
        boolean oldFocusable;
        synchronized (this) {
            oldFocusable = this.focusable;
            this.focusable = focusable;
        }
        isFocusTraversableOverridden = FOCUS_TRAVERSABLE_SET;

        firePropertyChange("focusable", oldFocusable, focusable);
        if (oldFocusable && !focusable) {
            if (isFocusOwner() && KeyboardFocusManager.isAutoFocusTransferEnabled()) {
                transferFocus(true);
            }
            KeyboardFocusManager.clearMostRecentFocusOwner(this);
        }
    }

    final boolean isFocusTraversableOverridden() {
        return (isFocusTraversableOverridden != FOCUS_TRAVERSABLE_DEFAULT);
    }

    /**
     * Sets the focus traversal keys for a given traversal operation for this
     * Component.
     * <p>
     * The default values for a Component's focus traversal keys are
     * implementation-dependent. Sun recommends that all implementations for a
     * particular native platform use the same default values. The
     * recommendations for Windows and Unix are listed below. These
     * recommendations are used in the Sun AWT implementations.
     *
     * <table class="striped">
     * <caption>Recommended default values for a Component's focus traversal
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
     * to any Component. It is a runtime error to specify a KEY_TYPED event as
     * mapping to a focus traversal operation, or to map the same event to
     * multiple default focus traversal operations.
     * <p>
     * If a value of null is specified for the Set, this Component inherits the
     * Set from its parent. If all ancestors of this Component have null
     * specified for the Set, then the current KeyboardFocusManager's default
     * Set is used.
     * <p>
     * This method may throw a {@code ClassCastException} if any {@code Object}
     * in {@code keystrokes} is not an {@code AWTKeyStroke}.
     *
     * @param id one of KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
     *        KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, or
     *        KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS
     * @param keystrokes the Set of AWTKeyStroke for the specified operation
     * @see #getFocusTraversalKeys
     * @see KeyboardFocusManager#FORWARD_TRAVERSAL_KEYS
     * @see KeyboardFocusManager#BACKWARD_TRAVERSAL_KEYS
     * @see KeyboardFocusManager#UP_CYCLE_TRAVERSAL_KEYS
     * @throws IllegalArgumentException if id is not one of
     *         KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
     *         KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, or
     *         KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS, or if keystrokes
     *         contains null, or if any keystroke represents a KEY_TYPED event,
     *         or if any keystroke already maps to another focus traversal
     *         operation for this Component
     * @since 1.4
     */
    public void setFocusTraversalKeys(int id,
                                      Set<? extends AWTKeyStroke> keystrokes)
    {
        if (id < 0 || id >= KeyboardFocusManager.TRAVERSAL_KEY_LENGTH - 1) {
            throw new IllegalArgumentException("invalid focus traversal key identifier");
        }

        setFocusTraversalKeys_NoIDCheck(id, keystrokes);
    }

    /**
     * Returns the Set of focus traversal keys for a given traversal operation
     * for this Component. (See
     * {@code setFocusTraversalKeys} for a full description of each key.)
     * <p>
     * If a Set of traversal keys has not been explicitly defined for this
     * Component, then this Component's parent's Set is returned. If no Set
     * has been explicitly defined for any of this Component's ancestors, then
     * the current KeyboardFocusManager's default Set is returned.
     *
     * @param id one of KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
     *        KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, or
     *        KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS
     * @return the Set of AWTKeyStrokes for the specified operation. The Set
     *         will be unmodifiable, and may be empty. null will never be
     *         returned.
     * @see #setFocusTraversalKeys
     * @see KeyboardFocusManager#FORWARD_TRAVERSAL_KEYS
     * @see KeyboardFocusManager#BACKWARD_TRAVERSAL_KEYS
     * @see KeyboardFocusManager#UP_CYCLE_TRAVERSAL_KEYS
     * @throws IllegalArgumentException if id is not one of
     *         KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
     *         KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, or
     *         KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS
     * @since 1.4
     */
    public Set<AWTKeyStroke> getFocusTraversalKeys(int id) {
        if (id < 0 || id >= KeyboardFocusManager.TRAVERSAL_KEY_LENGTH - 1) {
            throw new IllegalArgumentException("invalid focus traversal key identifier");
        }

        return getFocusTraversalKeys_NoIDCheck(id);
    }

    // We define these methods so that Container does not need to repeat this
    // code. Container cannot call super.<method> because Container allows
    // DOWN_CYCLE_TRAVERSAL_KEY while Component does not. The Component method
    // would erroneously generate an IllegalArgumentException for
    // DOWN_CYCLE_TRAVERSAL_KEY.
    final void setFocusTraversalKeys_NoIDCheck(int id, Set<? extends AWTKeyStroke> keystrokes) {
        Set<AWTKeyStroke> oldKeys;

        synchronized (this) {
            if (focusTraversalKeys == null) {
                initializeFocusTraversalKeys();
            }

            if (keystrokes != null) {
                for (AWTKeyStroke keystroke : keystrokes ) {

                    if (keystroke == null) {
                        throw new IllegalArgumentException("cannot set null focus traversal key");
                    }

                    if (keystroke.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
                        throw new IllegalArgumentException("focus traversal keys cannot map to KEY_TYPED events");
                    }

                    for (int i = 0; i < focusTraversalKeys.length; i++) {
                        if (i == id) {
                            continue;
                        }

                        if (getFocusTraversalKeys_NoIDCheck(i).contains(keystroke))
                        {
                            throw new IllegalArgumentException("focus traversal keys must be unique for a Component");
                        }
                    }
                }
            }

            oldKeys = focusTraversalKeys[id];
            focusTraversalKeys[id] = (keystrokes != null)
                ? Collections.unmodifiableSet(new HashSet<AWTKeyStroke>(keystrokes))
                : null;
        }

        firePropertyChange(focusTraversalKeyPropertyNames[id], oldKeys,
                           keystrokes);
    }
    final Set<AWTKeyStroke> getFocusTraversalKeys_NoIDCheck(int id) {
        // Okay to return Set directly because it is an unmodifiable view
        @SuppressWarnings("unchecked")
        Set<AWTKeyStroke> keystrokes = (focusTraversalKeys != null)
            ? focusTraversalKeys[id]
            : null;

        if (keystrokes != null) {
            return keystrokes;
        } else {
            Container parent = this.parent;
            if (parent != null) {
                return parent.getFocusTraversalKeys(id);
            } else {
                return KeyboardFocusManager.getCurrentKeyboardFocusManager().
                    getDefaultFocusTraversalKeys(id);
            }
        }
    }

    /**
     * Returns whether the Set of focus traversal keys for the given focus
     * traversal operation has been explicitly defined for this Component. If
     * this method returns {@code false}, this Component is inheriting the
     * Set from an ancestor, or from the current KeyboardFocusManager.
     *
     * @param id one of KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
     *        KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, or
     *        KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS
     * @return {@code true} if the Set of focus traversal keys for the
     *         given focus traversal operation has been explicitly defined for
     *         this Component; {@code false} otherwise.
     * @throws IllegalArgumentException if id is not one of
     *         KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
     *         KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, or
     *         KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS
     * @since 1.4
     */
    public boolean areFocusTraversalKeysSet(int id) {
        if (id < 0 || id >= KeyboardFocusManager.TRAVERSAL_KEY_LENGTH - 1) {
            throw new IllegalArgumentException("invalid focus traversal key identifier");
        }

        return (focusTraversalKeys != null && focusTraversalKeys[id] != null);
    }

    /**
     * Sets whether focus traversal keys are enabled for this Component.
     * Components for which focus traversal keys are disabled receive key
     * events for focus traversal keys. Components for which focus traversal
     * keys are enabled do not see these events; instead, the events are
     * automatically converted to traversal operations.
     *
     * @param focusTraversalKeysEnabled whether focus traversal keys are
     *        enabled for this Component
     * @see #getFocusTraversalKeysEnabled
     * @see #setFocusTraversalKeys
     * @see #getFocusTraversalKeys
     * @since 1.4
     */
    public void setFocusTraversalKeysEnabled(boolean
                                             focusTraversalKeysEnabled) {
        boolean oldFocusTraversalKeysEnabled;
        synchronized (this) {
            oldFocusTraversalKeysEnabled = this.focusTraversalKeysEnabled;
            this.focusTraversalKeysEnabled = focusTraversalKeysEnabled;
        }
        firePropertyChange("focusTraversalKeysEnabled",
                           oldFocusTraversalKeysEnabled,
                           focusTraversalKeysEnabled);
    }

    /**
     * Returns whether focus traversal keys are enabled for this Component.
     * Components for which focus traversal keys are disabled receive key
     * events for focus traversal keys. Components for which focus traversal
     * keys are enabled do not see these events; instead, the events are
     * automatically converted to traversal operations.
     *
     * @return whether focus traversal keys are enabled for this Component
     * @see #setFocusTraversalKeysEnabled
     * @see #setFocusTraversalKeys
     * @see #getFocusTraversalKeys
     * @since 1.4
     */
    public boolean getFocusTraversalKeysEnabled() {
        return focusTraversalKeysEnabled;
    }

    /**
     * Requests that this Component get the input focus, and that this
     * Component's top-level ancestor become the focused Window. This
     * component must be displayable, focusable, visible and all of
     * its ancestors (with the exception of the top-level Window) must
     * be visible for the request to be granted. Every effort will be
     * made to honor the request; however, in some cases it may be
     * impossible to do so. Developers must never assume that this
     * Component is the focus owner until this Component receives a
     * FOCUS_GAINED event. If this request is denied because this
     * Component's top-level Window cannot become the focused Window,
     * the request will be remembered and will be granted when the
     * Window is later focused by the user.
     * <p>
     * This method cannot be used to set the focus owner to no Component at
     * all. Use {@code KeyboardFocusManager.clearGlobalFocusOwner()}
     * instead.
     * <p>
     * Because the focus behavior of this method is platform-dependent,
     * developers are strongly encouraged to use
     * {@code requestFocusInWindow} when possible.
     *
     * <p>Note: Not all focus transfers result from invoking this method. As
     * such, a component may receive focus without this or any of the other
     * {@code requestFocus} methods of {@code Component} being invoked.
     *
     * @see #requestFocusInWindow
     * @see java.awt.event.FocusEvent
     * @see #addFocusListener
     * @see #isFocusable
     * @see #isDisplayable
     * @see KeyboardFocusManager#clearGlobalFocusOwner
     * @since 1.0
     */
    public void requestFocus() {
        requestFocusHelper(false, true);
    }


    /**
     * Requests by the reason of {@code cause} that this Component get the input
     * focus, and that this Component's top-level ancestor become the
     * focused Window. This component must be displayable, focusable, visible
     * and all of its ancestors (with the exception of the top-level Window)
     * must be visible for the request to be granted. Every effort will be
     * made to honor the request; however, in some cases it may be
     * impossible to do so. Developers must never assume that this
     * Component is the focus owner until this Component receives a
     * FOCUS_GAINED event.
     * <p>
     * The focus request effect may also depend on the provided
     * cause value. If this request is succeed the {@code FocusEvent}
     * generated in the result will receive the cause value specified as the
     * argument of method. If this request is denied because this Component's
     * top-level Window cannot become the focused Window, the request will be
     * remembered and will be granted when the Window is later focused by the
     * user.
     * <p>
     * This method cannot be used to set the focus owner to no Component at
     * all. Use {@code KeyboardFocusManager.clearGlobalFocusOwner()}
     * instead.
     * <p>
     * Because the focus behavior of this method is platform-dependent,
     * developers are strongly encouraged to use
     * {@code requestFocusInWindow(FocusEvent.Cause)} when possible.
     *
     * <p>Note: Not all focus transfers result from invoking this method. As
     * such, a component may receive focus without this or any of the other
     * {@code requestFocus} methods of {@code Component} being invoked.
     *
     * @param  cause the cause why the focus is requested
     * @see FocusEvent
     * @see FocusEvent.Cause
     * @see #requestFocusInWindow(FocusEvent.Cause)
     * @see java.awt.event.FocusEvent
     * @see #addFocusListener
     * @see #isFocusable
     * @see #isDisplayable
     * @see KeyboardFocusManager#clearGlobalFocusOwner
     * @since 9
     */
    public void requestFocus(FocusEvent.Cause cause) {
        requestFocusHelper(false, true, cause);
    }

    /**
     * Requests that this {@code Component} get the input focus,
     * and that this {@code Component}'s top-level ancestor
     * become the focused {@code Window}. This component must be
     * displayable, focusable, visible and all of its ancestors (with
     * the exception of the top-level Window) must be visible for the
     * request to be granted. Every effort will be made to honor the
     * request; however, in some cases it may be impossible to do
     * so. Developers must never assume that this component is the
     * focus owner until this component receives a FOCUS_GAINED
     * event. If this request is denied because this component's
     * top-level window cannot become the focused window, the request
     * will be remembered and will be granted when the window is later
     * focused by the user.
     * <p>
     * This method returns a boolean value. If {@code false} is returned,
     * the request is <b>guaranteed to fail</b>. If {@code true} is
     * returned, the request will succeed <b>unless</b> it is vetoed, or an
     * extraordinary event, such as disposal of the component's peer, occurs
     * before the request can be granted by the native windowing system. Again,
     * while a return value of {@code true} indicates that the request is
     * likely to succeed, developers must never assume that this component is
     * the focus owner until this component receives a FOCUS_GAINED event.
     * <p>
     * This method cannot be used to set the focus owner to no component at
     * all. Use {@code KeyboardFocusManager.clearGlobalFocusOwner}
     * instead.
     * <p>
     * Because the focus behavior of this method is platform-dependent,
     * developers are strongly encouraged to use
     * {@code requestFocusInWindow} when possible.
     * <p>
     * Every effort will be made to ensure that {@code FocusEvent}s
     * generated as a
     * result of this request will have the specified temporary value. However,
     * because specifying an arbitrary temporary state may not be implementable
     * on all native windowing systems, correct behavior for this method can be
     * guaranteed only for lightweight {@code Component}s.
     * This method is not intended
     * for general use, but exists instead as a hook for lightweight component
     * libraries, such as Swing.
     *
     * <p>Note: Not all focus transfers result from invoking this method. As
     * such, a component may receive focus without this or any of the other
     * {@code requestFocus} methods of {@code Component} being invoked.
     *
     * @param temporary true if the focus change is temporary,
     *        such as when the window loses the focus; for
     *        more information on temporary focus changes see the
     *<a href="doc-files/FocusSpec.html">Focus Specification</a>
     * @return {@code false} if the focus change request is guaranteed to
     *         fail; {@code true} if it is likely to succeed
     * @see java.awt.event.FocusEvent
     * @see #addFocusListener
     * @see #isFocusable
     * @see #isDisplayable
     * @see KeyboardFocusManager#clearGlobalFocusOwner
     * @since 1.4
     */
    protected boolean requestFocus(boolean temporary) {
        return requestFocusHelper(temporary, true);
    }

    /**
     * Requests by the reason of {@code cause} that this {@code Component} get
     * the input focus, and that this {@code Component}'s top-level ancestor
     * become the focused {@code Window}. This component must be
     * displayable, focusable, visible and all of its ancestors (with
     * the exception of the top-level Window) must be visible for the
     * request to be granted. Every effort will be made to honor the
     * request; however, in some cases it may be impossible to do
     * so. Developers must never assume that this component is the
     * focus owner until this component receives a FOCUS_GAINED
     * event. If this request is denied because this component's
     * top-level window cannot become the focused window, the request
     * will be remembered and will be granted when the window is later
     * focused by the user.
     * <p>
     * This method returns a boolean value. If {@code false} is returned,
     * the request is <b>guaranteed to fail</b>. If {@code true} is
     * returned, the request will succeed <b>unless</b> it is vetoed, or an
     * extraordinary event, such as disposal of the component's peer, occurs
     * before the request can be granted by the native windowing system. Again,
     * while a return value of {@code true} indicates that the request is
     * likely to succeed, developers must never assume that this component is
     * the focus owner until this component receives a FOCUS_GAINED event.
     * <p>
     * The focus request effect may also depend on the provided
     * cause value. If this request is succeed the {FocusEvent}
     * generated in the result will receive the cause value specified as the
     * argument of the method.
     * <p>
     * This method cannot be used to set the focus owner to no component at
     * all. Use {@code KeyboardFocusManager.clearGlobalFocusOwner}
     * instead.
     * <p>
     * Because the focus behavior of this method is platform-dependent,
     * developers are strongly encouraged to use
     * {@code requestFocusInWindow} when possible.
     * <p>
     * Every effort will be made to ensure that {@code FocusEvent}s
     * generated as a
     * result of this request will have the specified temporary value. However,
     * because specifying an arbitrary temporary state may not be implementable
     * on all native windowing systems, correct behavior for this method can be
     * guaranteed only for lightweight {@code Component}s.
     * This method is not intended
     * for general use, but exists instead as a hook for lightweight component
     * libraries, such as Swing.
     * <p>
     * Note: Not all focus transfers result from invoking this method. As
     * such, a component may receive focus without this or any of the other
     * {@code requestFocus} methods of {@code Component} being invoked.
     *
     * @param temporary true if the focus change is temporary,
     *        such as when the window loses the focus; for
     *        more information on temporary focus changes see the
     *<a href="doc-files/FocusSpec.html">Focus Specification</a>
     *
     * @param  cause the cause why the focus is requested
     * @return {@code false} if the focus change request is guaranteed to
     *         fail; {@code true} if it is likely to succeed
     * @see FocusEvent
     * @see FocusEvent.Cause
     * @see #addFocusListener
     * @see #isFocusable
     * @see #isDisplayable
     * @see KeyboardFocusManager#clearGlobalFocusOwner
     * @since 9
     */
    protected boolean requestFocus(boolean temporary, FocusEvent.Cause cause) {
        return requestFocusHelper(temporary, true, cause);
    }

    /**
     * Requests that this Component get the input focus, if this
     * Component's top-level ancestor is already the focused
     * Window. This component must be displayable, focusable, visible
     * and all of its ancestors (with the exception of the top-level
     * Window) must be visible for the request to be granted. Every
     * effort will be made to honor the request; however, in some
     * cases it may be impossible to do so. Developers must never
     * assume that this Component is the focus owner until this
     * Component receives a FOCUS_GAINED event.
     * <p>
     * This method returns a boolean value. If {@code false} is returned,
     * the request is <b>guaranteed to fail</b>. If {@code true} is
     * returned, the request will succeed <b>unless</b> it is vetoed, or an
     * extraordinary event, such as disposal of the Component's peer, occurs
     * before the request can be granted by the native windowing system. Again,
     * while a return value of {@code true} indicates that the request is
     * likely to succeed, developers must never assume that this Component is
     * the focus owner until this Component receives a FOCUS_GAINED event.
     * <p>
     * This method cannot be used to set the focus owner to no Component at
     * all. Use {@code KeyboardFocusManager.clearGlobalFocusOwner()}
     * instead.
     * <p>
     * The focus behavior of this method can be implemented uniformly across
     * platforms, and thus developers are strongly encouraged to use this
     * method over {@code requestFocus} when possible. Code which relies
     * on {@code requestFocus} may exhibit different focus behavior on
     * different platforms.
     *
     * <p>Note: Not all focus transfers result from invoking this method. As
     * such, a component may receive focus without this or any of the other
     * {@code requestFocus} methods of {@code Component} being invoked.
     *
     * @return {@code false} if the focus change request is guaranteed to
     *         fail; {@code true} if it is likely to succeed
     * @see #requestFocus
     * @see java.awt.event.FocusEvent
     * @see #addFocusListener
     * @see #isFocusable
     * @see #isDisplayable
     * @see KeyboardFocusManager#clearGlobalFocusOwner
     * @since 1.4
     */
    public boolean requestFocusInWindow() {
        return requestFocusHelper(false, false);
    }

    /**
     * Requests by the reason of {@code cause} that this Component get the input
     * focus, if this Component's top-level ancestor is already the focused
     * Window. This component must be displayable, focusable, visible
     * and all of its ancestors (with the exception of the top-level
     * Window) must be visible for the request to be granted. Every
     * effort will be made to honor the request; however, in some
     * cases it may be impossible to do so. Developers must never
     * assume that this Component is the focus owner until this
     * Component receives a FOCUS_GAINED event.
     * <p>
     * This method returns a boolean value. If {@code false} is returned,
     * the request is <b>guaranteed to fail</b>. If {@code true} is
     * returned, the request will succeed <b>unless</b> it is vetoed, or an
     * extraordinary event, such as disposal of the Component's peer, occurs
     * before the request can be granted by the native windowing system. Again,
     * while a return value of {@code true} indicates that the request is
     * likely to succeed, developers must never assume that this Component is
     * the focus owner until this Component receives a FOCUS_GAINED event.
     * <p>
     * The focus request effect may also depend on the provided
     * cause value. If this request is succeed the {@code FocusEvent}
     * generated in the result will receive the cause value specified as the
     * argument of the method.
     * <p>
     * This method cannot be used to set the focus owner to no Component at
     * all. Use {@code KeyboardFocusManager.clearGlobalFocusOwner()}
     * instead.
     * <p>
     * The focus behavior of this method can be implemented uniformly across
     * platforms, and thus developers are strongly encouraged to use this
     * method over {@code requestFocus(FocusEvent.Cause)} when possible.
     * Code which relies on {@code requestFocus(FocusEvent.Cause)} may exhibit
     * different focus behavior on different platforms.
     *
     * <p>Note: Not all focus transfers result from invoking this method. As
     * such, a component may receive focus without this or any of the other
     * {@code requestFocus} methods of {@code Component} being invoked.
     *
     * @param  cause the cause why the focus is requested
     * @return {@code false} if the focus change request is guaranteed to
     *         fail; {@code true} if it is likely to succeed
     * @see #requestFocus(FocusEvent.Cause)
     * @see FocusEvent
     * @see FocusEvent.Cause
     * @see java.awt.event.FocusEvent
     * @see #addFocusListener
     * @see #isFocusable
     * @see #isDisplayable
     * @see KeyboardFocusManager#clearGlobalFocusOwner
     * @since 9
     */
    public boolean requestFocusInWindow(FocusEvent.Cause cause) {
        return requestFocusHelper(false, false, cause);
    }

    /**
     * Requests that this {@code Component} get the input focus,
     * if this {@code Component}'s top-level ancestor is already
     * the focused {@code Window}.  This component must be
     * displayable, focusable, visible and all of its ancestors (with
     * the exception of the top-level Window) must be visible for the
     * request to be granted. Every effort will be made to honor the
     * request; however, in some cases it may be impossible to do
     * so. Developers must never assume that this component is the
     * focus owner until this component receives a FOCUS_GAINED event.
     * <p>
     * This method returns a boolean value. If {@code false} is returned,
     * the request is <b>guaranteed to fail</b>. If {@code true} is
     * returned, the request will succeed <b>unless</b> it is vetoed, or an
     * extraordinary event, such as disposal of the component's peer, occurs
     * before the request can be granted by the native windowing system. Again,
     * while a return value of {@code true} indicates that the request is
     * likely to succeed, developers must never assume that this component is
     * the focus owner until this component receives a FOCUS_GAINED event.
     * <p>
     * This method cannot be used to set the focus owner to no component at
     * all. Use {@code KeyboardFocusManager.clearGlobalFocusOwner}
     * instead.
     * <p>
     * The focus behavior of this method can be implemented uniformly across
     * platforms, and thus developers are strongly encouraged to use this
     * method over {@code requestFocus} when possible. Code which relies
     * on {@code requestFocus} may exhibit different focus behavior on
     * different platforms.
     * <p>
     * Every effort will be made to ensure that {@code FocusEvent}s
     * generated as a
     * result of this request will have the specified temporary value. However,
     * because specifying an arbitrary temporary state may not be implementable
     * on all native windowing systems, correct behavior for this method can be
     * guaranteed only for lightweight components. This method is not intended
     * for general use, but exists instead as a hook for lightweight component
     * libraries, such as Swing.
     *
     * <p>Note: Not all focus transfers result from invoking this method. As
     * such, a component may receive focus without this or any of the other
     * {@code requestFocus} methods of {@code Component} being invoked.
     *
     * @param temporary true if the focus change is temporary,
     *        such as when the window loses the focus; for
     *        more information on temporary focus changes see the
     *<a href="doc-files/FocusSpec.html">Focus Specification</a>
     * @return {@code false} if the focus change request is guaranteed to
     *         fail; {@code true} if it is likely to succeed
     * @see #requestFocus
     * @see java.awt.event.FocusEvent
     * @see #addFocusListener
     * @see #isFocusable
     * @see #isDisplayable
     * @see KeyboardFocusManager#clearGlobalFocusOwner
     * @since 1.4
     */
    protected boolean requestFocusInWindow(boolean temporary) {
        return requestFocusHelper(temporary, false);
    }

    boolean requestFocusInWindow(boolean temporary, FocusEvent.Cause cause) {
        return requestFocusHelper(temporary, false, cause);
    }

    final boolean requestFocusHelper(boolean temporary,
                                     boolean focusedWindowChangeAllowed) {
        return requestFocusHelper(temporary, focusedWindowChangeAllowed, FocusEvent.Cause.UNKNOWN);
    }

    final boolean requestFocusHelper(boolean temporary,
                                     boolean focusedWindowChangeAllowed,
                                     FocusEvent.Cause cause)
    {
        // 1) Check if the event being dispatched is a system-generated mouse event.
        AWTEvent currentEvent = EventQueue.getCurrentEvent();
        if (currentEvent instanceof MouseEvent &&
            SunToolkit.isSystemGenerated(currentEvent))
        {
            // 2) Sanity check: if the mouse event component source belongs to the same containing window.
            Component source = ((MouseEvent)currentEvent).getComponent();
            if (source == null || source.getContainingWindow() == getContainingWindow()) {
                focusLog.finest("requesting focus by mouse event \"in window\"");

                // If both the conditions are fulfilled the focus request should be strictly
                // bounded by the toplevel window. It's assumed that the mouse event activates
                // the window (if it wasn't active) and this makes it possible for a focus
                // request with a strong in-window requirement to change focus in the bounds
                // of the toplevel. If, by any means, due to asynchronous nature of the event
                // dispatching mechanism, the window happens to be natively inactive by the time
                // this focus request is eventually handled, it should not re-activate the
                // toplevel. Otherwise the result may not meet user expectations. See 6981400.
                focusedWindowChangeAllowed = false;
            }
        }
        if (!isRequestFocusAccepted(temporary, focusedWindowChangeAllowed, cause)) {
            if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                focusLog.finest("requestFocus is not accepted");
            }
            return false;
        }
        // Update most-recent map
        KeyboardFocusManager.setMostRecentFocusOwner(this);

        Component window = this;
        while ( (window != null) && !(window instanceof Window)) {
            if (!window.isVisible()) {
                if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                    focusLog.finest("component is recursively invisible");
                }
                return false;
            }
            window = window.parent;
        }

        ComponentPeer peer = this.peer;
        Component heavyweight = (peer instanceof LightweightPeer)
            ? getNativeContainer() : this;
        if (heavyweight == null || !heavyweight.isVisible()) {
            if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                focusLog.finest("Component is not a part of visible hierarchy");
            }
            return false;
        }
        peer = heavyweight.peer;
        if (peer == null) {
            if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                focusLog.finest("Peer is null");
            }
            return false;
        }

        // Focus this Component
        long time = 0;
        if (EventQueue.isDispatchThread()) {
            time = Toolkit.getEventQueue().getMostRecentKeyEventTime();
        } else {
            // A focus request made from outside EDT should not be associated with any event
            // and so its time stamp is simply set to the current time.
            time = System.currentTimeMillis();
        }

        boolean success = peer.requestFocus
            (this, temporary, focusedWindowChangeAllowed, time, cause);
        if (!success) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager
                (appContext).dequeueKeyEvents(time, this);
            if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                focusLog.finest("Peer request failed");
            }
        } else {
            if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                focusLog.finest("Pass for " + this);
            }
        }
        return success;
    }

    private boolean isRequestFocusAccepted(boolean temporary,
                                           boolean focusedWindowChangeAllowed,
                                           FocusEvent.Cause cause)
    {
        if (!isFocusable() || !isVisible()) {
            if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                focusLog.finest("Not focusable or not visible");
            }
            return false;
        }

        ComponentPeer peer = this.peer;
        if (peer == null) {
            if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                focusLog.finest("peer is null");
            }
            return false;
        }

        Window window = getContainingWindow();
        if (window == null || !window.isFocusableWindow()) {
            if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                focusLog.finest("Component doesn't have toplevel");
            }
            return false;
        }

        // We have passed all regular checks for focus request,
        // now let's call RequestFocusController and see what it says.
        Component focusOwner = KeyboardFocusManager.getMostRecentFocusOwner(window);
        if (focusOwner == null) {
            // sometimes most recent focus owner may be null, but focus owner is not
            // e.g. we reset most recent focus owner if user removes focus owner
            focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (focusOwner != null && focusOwner.getContainingWindow() != window) {
                focusOwner = null;
            }
        }

        if (focusOwner == this || focusOwner == null) {
            // Controller is supposed to verify focus transfers and for this it
            // should know both from and to components.  And it shouldn't verify
            // transfers from when these components are equal.
            if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                focusLog.finest("focus owner is null or this");
            }
            return true;
        }

        if (FocusEvent.Cause.ACTIVATION == cause) {
            // we shouldn't call RequestFocusController in case we are
            // in activation.  We do request focus on component which
            // has got temporary focus lost and then on component which is
            // most recent focus owner.  But most recent focus owner can be
            // changed by requestFocusXXX() call only, so this transfer has
            // been already approved.
            if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                focusLog.finest("cause is activation");
            }
            return true;
        }

        boolean ret = Component.requestFocusController.acceptRequestFocus(focusOwner,
                                                                          this,
                                                                          temporary,
                                                                          focusedWindowChangeAllowed,
                                                                          cause);
        if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
            focusLog.finest("RequestFocusController returns {0}", ret);
        }

        return ret;
    }

    private static RequestFocusController requestFocusController = new DummyRequestFocusController();

    // Swing access this method through reflection to implement InputVerifier's functionality.
    // Perhaps, we should make this method public (later ;)
    private static class DummyRequestFocusController implements RequestFocusController {
        public boolean acceptRequestFocus(Component from, Component to,
                                          boolean temporary, boolean focusedWindowChangeAllowed,
                                          FocusEvent.Cause cause)
        {
            return true;
        }
    };

    static synchronized void setRequestFocusController(RequestFocusController requestController)
    {
        if (requestController == null) {
            requestFocusController = new DummyRequestFocusController();
        } else {
            requestFocusController = requestController;
        }
    }

    /**
     * Returns the Container which is the focus cycle root of this Component's
     * focus traversal cycle. Each focus traversal cycle has only a single
     * focus cycle root and each Component which is not a Container belongs to
     * only a single focus traversal cycle. Containers which are focus cycle
     * roots belong to two cycles: one rooted at the Container itself, and one
     * rooted at the Container's nearest focus-cycle-root ancestor. For such
     * Containers, this method will return the Container's nearest focus-cycle-
     * root ancestor.
     *
     * @return this Component's nearest focus-cycle-root ancestor
     * @see Container#isFocusCycleRoot()
     * @since 1.4
     */
    public Container getFocusCycleRootAncestor() {
        Container rootAncestor = this.parent;
        while (rootAncestor != null && !rootAncestor.isFocusCycleRoot()) {
            rootAncestor = rootAncestor.parent;
        }
        return rootAncestor;
    }

    /**
     * Returns whether the specified Container is the focus cycle root of this
     * Component's focus traversal cycle. Each focus traversal cycle has only
     * a single focus cycle root and each Component which is not a Container
     * belongs to only a single focus traversal cycle.
     *
     * @param container the Container to be tested
     * @return {@code true} if the specified Container is a focus-cycle-
     *         root of this Component; {@code false} otherwise
     * @see Container#isFocusCycleRoot()
     * @since 1.4
     */
    public boolean isFocusCycleRoot(Container container) {
        Container rootAncestor = getFocusCycleRootAncestor();
        return (rootAncestor == container);
    }

    Container getTraversalRoot() {
        return getFocusCycleRootAncestor();
    }

    /**
     * Transfers the focus to the next component, as though this Component were
     * the focus owner.
     * @see       #requestFocus()
     * @since     1.1
     */
    public void transferFocus() {
        nextFocus();
    }

    /**
     * @deprecated As of JDK version 1.1,
     * replaced by transferFocus().
     */
    @Deprecated
    public void nextFocus() {
        transferFocus(false);
    }

    boolean transferFocus(boolean clearOnFailure) {
        if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
            focusLog.finer("clearOnFailure = " + clearOnFailure);
        }
        Component toFocus = getNextFocusCandidate();
        boolean res = false;
        if (toFocus != null && !toFocus.isFocusOwner() && toFocus != this) {
            res = toFocus.requestFocusInWindow(FocusEvent.Cause.TRAVERSAL_FORWARD);
        }
        if (clearOnFailure && !res) {
            if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
                focusLog.finer("clear global focus owner");
            }
            KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwnerPriv();
        }
        if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
            focusLog.finer("returning result: " + res);
        }
        return res;
    }

    @SuppressWarnings("removal")
    final Component getNextFocusCandidate() {
        Container rootAncestor = getTraversalRoot();
        Component comp = this;
        while (rootAncestor != null &&
               !(rootAncestor.isShowing() && rootAncestor.canBeFocusOwner()))
        {
            comp = rootAncestor;
            rootAncestor = comp.getFocusCycleRootAncestor();
        }
        if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
            focusLog.finer("comp = " + comp + ", root = " + rootAncestor);
        }
        Component candidate = null;
        if (rootAncestor != null) {
            FocusTraversalPolicy policy = rootAncestor.getFocusTraversalPolicy();
            Component toFocus = policy.getComponentAfter(rootAncestor, comp);
            if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
                focusLog.finer("component after is " + toFocus);
            }
            if (toFocus == null) {
                toFocus = policy.getDefaultComponent(rootAncestor);
                if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
                    focusLog.finer("default component is " + toFocus);
                }
            }
            if (toFocus == null) {
                Applet applet = EmbeddedFrame.getAppletIfAncestorOf(this);
                if (applet != null) {
                    toFocus = applet;
                }
            }
            candidate = toFocus;
        }
        if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
            focusLog.finer("Focus transfer candidate: " + candidate);
        }
        return candidate;
    }

    /**
     * Transfers the focus to the previous component, as though this Component
     * were the focus owner.
     * @see       #requestFocus()
     * @since     1.4
     */
    public void transferFocusBackward() {
        transferFocusBackward(false);
    }

    boolean transferFocusBackward(boolean clearOnFailure) {
        Container rootAncestor = getTraversalRoot();
        Component comp = this;
        while (rootAncestor != null &&
               !(rootAncestor.isShowing() && rootAncestor.canBeFocusOwner()))
        {
            comp = rootAncestor;
            rootAncestor = comp.getFocusCycleRootAncestor();
        }
        boolean res = false;
        if (rootAncestor != null) {
            FocusTraversalPolicy policy = rootAncestor.getFocusTraversalPolicy();
            Component toFocus = policy.getComponentBefore(rootAncestor, comp);
            if (toFocus == null) {
                toFocus = policy.getDefaultComponent(rootAncestor);
            }
            if (toFocus != null) {
                res = toFocus.requestFocusInWindow(FocusEvent.Cause.TRAVERSAL_BACKWARD);
            }
        }
        if (clearOnFailure && !res) {
            if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
                focusLog.finer("clear global focus owner");
            }
            KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwnerPriv();
        }
        if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
            focusLog.finer("returning result: " + res);
        }
        return res;
    }

    /**
     * Transfers the focus up one focus traversal cycle. Typically, the focus
     * owner is set to this Component's focus cycle root, and the current focus
     * cycle root is set to the new focus owner's focus cycle root. If,
     * however, this Component's focus cycle root is a Window, then the focus
     * owner is set to the focus cycle root's default Component to focus, and
     * the current focus cycle root is unchanged.
     *
     * @see       #requestFocus()
     * @see       Container#isFocusCycleRoot()
     * @see       Container#setFocusCycleRoot(boolean)
     * @since     1.4
     */
    public void transferFocusUpCycle() {
        Container rootAncestor;
        for (rootAncestor = getFocusCycleRootAncestor();
             rootAncestor != null && !(rootAncestor.isShowing() &&
                                       rootAncestor.isFocusable() &&
                                       rootAncestor.isEnabled());
             rootAncestor = rootAncestor.getFocusCycleRootAncestor()) {
        }

        if (rootAncestor != null) {
            Container rootAncestorRootAncestor =
                rootAncestor.getFocusCycleRootAncestor();
            Container fcr = (rootAncestorRootAncestor != null) ?
                rootAncestorRootAncestor : rootAncestor;

            KeyboardFocusManager.getCurrentKeyboardFocusManager().
                setGlobalCurrentFocusCycleRootPriv(fcr);
            rootAncestor.requestFocus(FocusEvent.Cause.TRAVERSAL_UP);
        } else {
            Window window = getContainingWindow();

            if (window != null) {
                Component toFocus = window.getFocusTraversalPolicy().
                    getDefaultComponent(window);
                if (toFocus != null) {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().
                        setGlobalCurrentFocusCycleRootPriv(window);
                    toFocus.requestFocus(FocusEvent.Cause.TRAVERSAL_UP);
                }
            }
        }
    }

    /**
     * Returns {@code true} if this {@code Component} is the
     * focus owner.  This method is obsolete, and has been replaced by
     * {@code isFocusOwner()}.
     *
     * @return {@code true} if this {@code Component} is the
     *         focus owner; {@code false} otherwise
     * @since 1.2
     */
    public boolean hasFocus() {
        return (KeyboardFocusManager.getCurrentKeyboardFocusManager().
                getFocusOwner() == this);
    }

    /**
     * Returns {@code true} if this {@code Component} is the
     *    focus owner.
     *
     * @return {@code true} if this {@code Component} is the
     *     focus owner; {@code false} otherwise
     * @since 1.4
     */
    public boolean isFocusOwner() {
        return hasFocus();
    }

    /**
     * Used to disallow auto-focus-transfer on disposal of the focus owner
     * in the process of disposing its parent container.
     */
    private boolean autoFocusTransferOnDisposal = true;

    void setAutoFocusTransferOnDisposal(boolean value) {
        autoFocusTransferOnDisposal = value;
    }

    boolean isAutoFocusTransferOnDisposal() {
        return autoFocusTransferOnDisposal;
    }

    /**
     * Adds the specified popup menu to the component.
     * @param     popup the popup menu to be added to the component.
     * @see       #remove(MenuComponent)
     * @exception NullPointerException if {@code popup} is {@code null}
     * @since     1.1
     */
    public void add(PopupMenu popup) {
        synchronized (getTreeLock()) {
            if (popup.parent != null) {
                popup.parent.remove(popup);
            }
            if (popups == null) {
                popups = new Vector<PopupMenu>();
            }
            popups.addElement(popup);
            popup.parent = this;

            if (peer != null) {
                if (popup.peer == null) {
                    popup.addNotify();
                }
            }
        }
    }

    /**
     * Removes the specified popup menu from the component.
     * @param     popup the popup menu to be removed
     * @see       #add(PopupMenu)
     * @since     1.1
     */
    @SuppressWarnings("unchecked")
    public void remove(MenuComponent popup) {
        synchronized (getTreeLock()) {
            if (popups == null) {
                return;
            }
            int index = popups.indexOf(popup);
            if (index >= 0) {
                PopupMenu pmenu = (PopupMenu)popup;
                if (pmenu.peer != null) {
                    pmenu.removeNotify();
                }
                pmenu.parent = null;
                popups.removeElementAt(index);
                if (popups.size() == 0) {
                    popups = null;
                }
            }
        }
    }

    /**
     * Returns a string representing the state of this component. This
     * method is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not be
     * {@code null}.
     *
     * @return  a string representation of this component's state
     * @since     1.0
     */
    protected String paramString() {
        final String thisName = Objects.toString(getName(), "");
        final String invalid = isValid() ? "" : ",invalid";
        final String hidden = visible ? "" : ",hidden";
        final String disabled = enabled ? "" : ",disabled";
        return thisName + ',' + x + ',' + y + ',' + width + 'x' + height
                + invalid + hidden + disabled;
    }

    /**
     * Returns a string representation of this component and its values.
     * @return    a string representation of this component
     * @since     1.0
     */
    public String toString() {
        return getClass().getName() + '[' + paramString() + ']';
    }

    /**
     * Prints a listing of this component to the standard system output
     * stream {@code System.out}.
     * @see       java.lang.System#out
     * @since     1.0
     */
    public void list() {
        list(System.out, 0);
    }

    /**
     * Prints a listing of this component to the specified output
     * stream.
     * @param    out   a print stream
     * @throws   NullPointerException if {@code out} is {@code null}
     * @since    1.0
     */
    public void list(PrintStream out) {
        list(out, 0);
    }

    /**
     * Prints out a list, starting at the specified indentation, to the
     * specified print stream.
     * @param     out      a print stream
     * @param     indent   number of spaces to indent
     * @see       java.io.PrintStream#println(java.lang.Object)
     * @throws    NullPointerException if {@code out} is {@code null}
     * @since     1.0
     */
    public void list(PrintStream out, int indent) {
        for (int i = 0 ; i < indent ; i++) {
            out.print(" ");
        }
        out.println(this);
    }

    /**
     * Prints a listing to the specified print writer.
     * @param  out  the print writer to print to
     * @throws NullPointerException if {@code out} is {@code null}
     * @since 1.1
     */
    public void list(PrintWriter out) {
        list(out, 0);
    }

    /**
     * Prints out a list, starting at the specified indentation, to
     * the specified print writer.
     * @param out the print writer to print to
     * @param indent the number of spaces to indent
     * @throws NullPointerException if {@code out} is {@code null}
     * @see       java.io.PrintStream#println(java.lang.Object)
     * @since 1.1
     */
    public void list(PrintWriter out, int indent) {
        for (int i = 0 ; i < indent ; i++) {
            out.print(" ");
        }
        out.println(this);
    }

    /*
     * Fetches the native container somewhere higher up in the component
     * tree that contains this component.
     */
    final Container getNativeContainer() {
        Container p = getContainer();
        while (p != null && p.peer instanceof LightweightPeer) {
            p = p.getContainer();
        }
        return p;
    }

    /**
     * Adds a PropertyChangeListener to the listener list. The listener is
     * registered for all bound properties of this class, including the
     * following:
     * <ul>
     *    <li>this Component's font ("font")</li>
     *    <li>this Component's background color ("background")</li>
     *    <li>this Component's foreground color ("foreground")</li>
     *    <li>this Component's focusability ("focusable")</li>
     *    <li>this Component's focus traversal keys enabled state
     *        ("focusTraversalKeysEnabled")</li>
     *    <li>this Component's Set of FORWARD_TRAVERSAL_KEYS
     *        ("forwardFocusTraversalKeys")</li>
     *    <li>this Component's Set of BACKWARD_TRAVERSAL_KEYS
     *        ("backwardFocusTraversalKeys")</li>
     *    <li>this Component's Set of UP_CYCLE_TRAVERSAL_KEYS
     *        ("upCycleFocusTraversalKeys")</li>
     *    <li>this Component's preferred size ("preferredSize")</li>
     *    <li>this Component's minimum size ("minimumSize")</li>
     *    <li>this Component's maximum size ("maximumSize")</li>
     *    <li>this Component's name ("name")</li>
     * </ul>
     * Note that if this {@code Component} is inheriting a bound property, then no
     * event will be fired in response to a change in the inherited property.
     * <p>
     * If {@code listener} is {@code null},
     * no exception is thrown and no action is performed.
     *
     * @param    listener  the property change listener to be added
     *
     * @see #removePropertyChangeListener
     * @see #getPropertyChangeListeners
     * @see #addPropertyChangeListener(java.lang.String, java.beans.PropertyChangeListener)
     */
    public void addPropertyChangeListener(
                                                       PropertyChangeListener listener) {
        synchronized (getObjectLock()) {
            if (listener == null) {
                return;
            }
            if (changeSupport == null) {
                changeSupport = new PropertyChangeSupport(this);
            }
            changeSupport.addPropertyChangeListener(listener);
        }
    }

    /**
     * Removes a PropertyChangeListener from the listener list. This method
     * should be used to remove PropertyChangeListeners that were registered
     * for all bound properties of this class.
     * <p>
     * If listener is null, no exception is thrown and no action is performed.
     *
     * @param listener the PropertyChangeListener to be removed
     *
     * @see #addPropertyChangeListener
     * @see #getPropertyChangeListeners
     * @see #removePropertyChangeListener(java.lang.String,java.beans.PropertyChangeListener)
     */
    public void removePropertyChangeListener(
                                                          PropertyChangeListener listener) {
        synchronized (getObjectLock()) {
            if (listener == null || changeSupport == null) {
                return;
            }
            changeSupport.removePropertyChangeListener(listener);
        }
    }

    /**
     * Returns an array of all the property change listeners
     * registered on this component.
     *
     * @return all of this component's {@code PropertyChangeListener}s
     *         or an empty array if no property change
     *         listeners are currently registered
     *
     * @see      #addPropertyChangeListener
     * @see      #removePropertyChangeListener
     * @see      #getPropertyChangeListeners(java.lang.String)
     * @see      java.beans.PropertyChangeSupport#getPropertyChangeListeners
     * @since    1.4
     */
    public PropertyChangeListener[] getPropertyChangeListeners() {
        synchronized (getObjectLock()) {
            if (changeSupport == null) {
                return new PropertyChangeListener[0];
            }
            return changeSupport.getPropertyChangeListeners();
        }
    }

    /**
     * Adds a PropertyChangeListener to the listener list for a specific
     * property. The specified property may be user-defined, or one of the
     * following:
     * <ul>
     *    <li>this Component's font ("font")</li>
     *    <li>this Component's background color ("background")</li>
     *    <li>this Component's foreground color ("foreground")</li>
     *    <li>this Component's focusability ("focusable")</li>
     *    <li>this Component's focus traversal keys enabled state
     *        ("focusTraversalKeysEnabled")</li>
     *    <li>this Component's Set of FORWARD_TRAVERSAL_KEYS
     *        ("forwardFocusTraversalKeys")</li>
     *    <li>this Component's Set of BACKWARD_TRAVERSAL_KEYS
     *        ("backwardFocusTraversalKeys")</li>
     *    <li>this Component's Set of UP_CYCLE_TRAVERSAL_KEYS
     *        ("upCycleFocusTraversalKeys")</li>
     * </ul>
     * Note that if this {@code Component} is inheriting a bound property, then no
     * event will be fired in response to a change in the inherited property.
     * <p>
     * If {@code propertyName} or {@code listener} is {@code null},
     * no exception is thrown and no action is taken.
     *
     * @param propertyName one of the property names listed above
     * @param listener the property change listener to be added
     *
     * @see #removePropertyChangeListener(java.lang.String, java.beans.PropertyChangeListener)
     * @see #getPropertyChangeListeners(java.lang.String)
     * @see #addPropertyChangeListener(java.lang.String, java.beans.PropertyChangeListener)
     */
    public void addPropertyChangeListener(
                                                       String propertyName,
                                                       PropertyChangeListener listener) {
        synchronized (getObjectLock()) {
            if (listener == null) {
                return;
            }
            if (changeSupport == null) {
                changeSupport = new PropertyChangeSupport(this);
            }
            changeSupport.addPropertyChangeListener(propertyName, listener);
        }
    }

    /**
     * Removes a {@code PropertyChangeListener} from the listener
     * list for a specific property. This method should be used to remove
     * {@code PropertyChangeListener}s
     * that were registered for a specific bound property.
     * <p>
     * If {@code propertyName} or {@code listener} is {@code null},
     * no exception is thrown and no action is taken.
     *
     * @param propertyName a valid property name
     * @param listener the PropertyChangeListener to be removed
     *
     * @see #addPropertyChangeListener(java.lang.String, java.beans.PropertyChangeListener)
     * @see #getPropertyChangeListeners(java.lang.String)
     * @see #removePropertyChangeListener(java.beans.PropertyChangeListener)
     */
    public void removePropertyChangeListener(
                                                          String propertyName,
                                                          PropertyChangeListener listener) {
        synchronized (getObjectLock()) {
            if (listener == null || changeSupport == null) {
                return;
            }
            changeSupport.removePropertyChangeListener(propertyName, listener);
        }
    }

    /**
     * Returns an array of all the listeners which have been associated
     * with the named property.
     *
     * @param  propertyName the property name
     * @return all of the {@code PropertyChangeListener}s associated with
     *         the named property; if no such listeners have been added or
     *         if {@code propertyName} is {@code null}, an empty
     *         array is returned
     *
     * @see #addPropertyChangeListener(java.lang.String, java.beans.PropertyChangeListener)
     * @see #removePropertyChangeListener(java.lang.String, java.beans.PropertyChangeListener)
     * @see #getPropertyChangeListeners
     * @since 1.4
     */
    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        synchronized (getObjectLock()) {
            if (changeSupport == null) {
                return new PropertyChangeListener[0];
            }
            return changeSupport.getPropertyChangeListeners(propertyName);
        }
    }

    /**
     * Support for reporting bound property changes for Object properties.
     * This method can be called when a bound property has changed and it will
     * send the appropriate PropertyChangeEvent to any registered
     * PropertyChangeListeners.
     *
     * @param propertyName the property whose value has changed
     * @param oldValue the property's previous value
     * @param newValue the property's new value
     */
    protected void firePropertyChange(String propertyName,
                                      Object oldValue, Object newValue) {
        PropertyChangeSupport changeSupport;
        synchronized (getObjectLock()) {
            changeSupport = this.changeSupport;
        }
        if (changeSupport == null ||
            (oldValue != null && newValue != null && oldValue.equals(newValue))) {
            return;
        }
        changeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Support for reporting bound property changes for boolean properties.
     * This method can be called when a bound property has changed and it will
     * send the appropriate PropertyChangeEvent to any registered
     * PropertyChangeListeners.
     *
     * @param propertyName the property whose value has changed
     * @param oldValue the property's previous value
     * @param newValue the property's new value
     * @since 1.4
     */
    protected void firePropertyChange(String propertyName,
                                      boolean oldValue, boolean newValue) {
        PropertyChangeSupport changeSupport = this.changeSupport;
        if (changeSupport == null || oldValue == newValue) {
            return;
        }
        changeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Support for reporting bound property changes for integer properties.
     * This method can be called when a bound property has changed and it will
     * send the appropriate PropertyChangeEvent to any registered
     * PropertyChangeListeners.
     *
     * @param propertyName the property whose value has changed
     * @param oldValue the property's previous value
     * @param newValue the property's new value
     * @since 1.4
     */
    protected void firePropertyChange(String propertyName,
                                      int oldValue, int newValue) {
        PropertyChangeSupport changeSupport = this.changeSupport;
        if (changeSupport == null || oldValue == newValue) {
            return;
        }
        changeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Reports a bound property change.
     *
     * @param propertyName the programmatic name of the property
     *          that was changed
     * @param oldValue the old value of the property (as a byte)
     * @param newValue the new value of the property (as a byte)
     * @see #firePropertyChange(java.lang.String, java.lang.Object,
     *          java.lang.Object)
     * @since 1.5
     */
    public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {
        if (changeSupport == null || oldValue == newValue) {
            return;
        }
        firePropertyChange(propertyName, Byte.valueOf(oldValue), Byte.valueOf(newValue));
    }

    /**
     * Reports a bound property change.
     *
     * @param propertyName the programmatic name of the property
     *          that was changed
     * @param oldValue the old value of the property (as a char)
     * @param newValue the new value of the property (as a char)
     * @see #firePropertyChange(java.lang.String, java.lang.Object,
     *          java.lang.Object)
     * @since 1.5
     */
    public void firePropertyChange(String propertyName, char oldValue, char newValue) {
        if (changeSupport == null || oldValue == newValue) {
            return;
        }
        firePropertyChange(propertyName, Character.valueOf(oldValue), Character.valueOf(newValue));
    }

    /**
     * Reports a bound property change.
     *
     * @param propertyName the programmatic name of the property
     *          that was changed
     * @param oldValue the old value of the property (as a short)
     * @param newValue the new value of the property (as a short)
     * @see #firePropertyChange(java.lang.String, java.lang.Object,
     *          java.lang.Object)
     * @since 1.5
     */
    public void firePropertyChange(String propertyName, short oldValue, short newValue) {
        if (changeSupport == null || oldValue == newValue) {
            return;
        }
        firePropertyChange(propertyName, Short.valueOf(oldValue), Short.valueOf(newValue));
    }


    /**
     * Reports a bound property change.
     *
     * @param propertyName the programmatic name of the property
     *          that was changed
     * @param oldValue the old value of the property (as a long)
     * @param newValue the new value of the property (as a long)
     * @see #firePropertyChange(java.lang.String, java.lang.Object,
     *          java.lang.Object)
     * @since 1.5
     */
    public void firePropertyChange(String propertyName, long oldValue, long newValue) {
        if (changeSupport == null || oldValue == newValue) {
            return;
        }
        firePropertyChange(propertyName, Long.valueOf(oldValue), Long.valueOf(newValue));
    }

    /**
     * Reports a bound property change.
     *
     * @param propertyName the programmatic name of the property
     *          that was changed
     * @param oldValue the old value of the property (as a float)
     * @param newValue the new value of the property (as a float)
     * @see #firePropertyChange(java.lang.String, java.lang.Object,
     *          java.lang.Object)
     * @since 1.5
     */
    public void firePropertyChange(String propertyName, float oldValue, float newValue) {
        if (changeSupport == null || oldValue == newValue) {
            return;
        }
        firePropertyChange(propertyName, Float.valueOf(oldValue), Float.valueOf(newValue));
    }

    /**
     * Reports a bound property change.
     *
     * @param propertyName the programmatic name of the property
     *          that was changed
     * @param oldValue the old value of the property (as a double)
     * @param newValue the new value of the property (as a double)
     * @see #firePropertyChange(java.lang.String, java.lang.Object,
     *          java.lang.Object)
     * @since 1.5
     */
    public void firePropertyChange(String propertyName, double oldValue, double newValue) {
        if (changeSupport == null || oldValue == newValue) {
            return;
        }
        firePropertyChange(propertyName, Double.valueOf(oldValue), Double.valueOf(newValue));
    }


    // Serialization support.

    /**
     * Component Serialized Data Version.
     *
     * @serial
     */
    private int componentSerializedDataVersion = 4;

    /**
     * This hack is for Swing serialization. It will invoke
     * the Swing package private method {@code compWriteObjectNotify}.
     */
    private void doSwingSerialization() {
        if (!(this instanceof JComponent)) {
            return;
        }
        @SuppressWarnings("deprecation")
        Package swingPackage = Package.getPackage("javax.swing");
        // For Swing serialization to correctly work Swing needs to
        // be notified before Component does it's serialization.  This
        // hack accommodates this.
        //
        // Swing classes MUST be loaded by the bootstrap class loader,
        // otherwise we don't consider them.
        for (Class<?> klass = Component.this.getClass(); klass != null;
                   klass = klass.getSuperclass()) {
            if (klass.getPackage() == swingPackage &&
                      klass.getClassLoader() == null) {

                SwingAccessor.getJComponentAccessor()
                        .compWriteObjectNotify((JComponent) this);
                return;
            }
        }
    }

    /**
     * Writes default serializable fields to stream.  Writes
     * a variety of serializable listeners as optional data.
     * The non-serializable listeners are detected and
     * no attempt is made to serialize them.
     *
     * @param  s the {@code ObjectOutputStream} to write
     * @throws IOException if an I/O error occurs
     * @serialData {@code null} terminated sequence of
     *   0 or more pairs; the pair consists of a {@code String}
     *   and an {@code Object}; the {@code String} indicates
     *   the type of object and is one of the following (as of 1.4):
     *   {@code componentListenerK} indicating an
     *     {@code ComponentListener} object;
     *   {@code focusListenerK} indicating an
     *     {@code FocusListener} object;
     *   {@code keyListenerK} indicating an
     *     {@code KeyListener} object;
     *   {@code mouseListenerK} indicating an
     *     {@code MouseListener} object;
     *   {@code mouseMotionListenerK} indicating an
     *     {@code MouseMotionListener} object;
     *   {@code inputMethodListenerK} indicating an
     *     {@code InputMethodListener} object;
     *   {@code hierarchyListenerK} indicating an
     *     {@code HierarchyListener} object;
     *   {@code hierarchyBoundsListenerK} indicating an
     *     {@code HierarchyBoundsListener} object;
     *   {@code mouseWheelListenerK} indicating an
     *     {@code MouseWheelListener} object
     * @serialData an optional {@code ComponentOrientation}
     *    (after {@code inputMethodListener}, as of 1.2)
     *
     * @see AWTEventMulticaster#save(java.io.ObjectOutputStream, java.lang.String, java.util.EventListener)
     * @see #componentListenerK
     * @see #focusListenerK
     * @see #keyListenerK
     * @see #mouseListenerK
     * @see #mouseMotionListenerK
     * @see #inputMethodListenerK
     * @see #hierarchyListenerK
     * @see #hierarchyBoundsListenerK
     * @see #mouseWheelListenerK
     * @see #readObject(ObjectInputStream)
     */
    @Serial
    private void writeObject(ObjectOutputStream s)
      throws IOException
    {
        doSwingSerialization();

        s.defaultWriteObject();

        AWTEventMulticaster.save(s, componentListenerK, componentListener);
        AWTEventMulticaster.save(s, focusListenerK, focusListener);
        AWTEventMulticaster.save(s, keyListenerK, keyListener);
        AWTEventMulticaster.save(s, mouseListenerK, mouseListener);
        AWTEventMulticaster.save(s, mouseMotionListenerK, mouseMotionListener);
        AWTEventMulticaster.save(s, inputMethodListenerK, inputMethodListener);

        s.writeObject(null);
        s.writeObject(componentOrientation);

        AWTEventMulticaster.save(s, hierarchyListenerK, hierarchyListener);
        AWTEventMulticaster.save(s, hierarchyBoundsListenerK,
                                 hierarchyBoundsListener);
        s.writeObject(null);

        AWTEventMulticaster.save(s, mouseWheelListenerK, mouseWheelListener);
        s.writeObject(null);

    }

    /**
     * Reads the {@code ObjectInputStream} and if it isn't
     * {@code null} adds a listener to receive a variety
     * of events fired by the component.
     * Unrecognized keys or values will be ignored.
     *
     * @param  s the {@code ObjectInputStream} to read
     * @throws ClassNotFoundException if the class of a serialized object could
     *         not be found
     * @throws IOException if an I/O error occurs
     * @see #writeObject(ObjectOutputStream)
     */
    @SuppressWarnings("removal")
    @Serial
    private void readObject(ObjectInputStream s)
      throws ClassNotFoundException, IOException
    {
        objectLock = new Object();

        acc = AccessController.getContext();

        s.defaultReadObject();

        appContext = AppContext.getAppContext();
        coalescingEnabled = checkCoalescing();
        if (componentSerializedDataVersion < 4) {
            // These fields are non-transient and rely on default
            // serialization. However, the default values are insufficient,
            // so we need to set them explicitly for object data streams prior
            // to 1.4.
            focusable = true;
            isFocusTraversableOverridden = FOCUS_TRAVERSABLE_UNKNOWN;
            initializeFocusTraversalKeys();
            focusTraversalKeysEnabled = true;
        }

        Object keyOrNull;
        while(null != (keyOrNull = s.readObject())) {
            String key = ((String)keyOrNull).intern();

            if (componentListenerK == key)
                addComponentListener((ComponentListener)(s.readObject()));

            else if (focusListenerK == key)
                addFocusListener((FocusListener)(s.readObject()));

            else if (keyListenerK == key)
                addKeyListener((KeyListener)(s.readObject()));

            else if (mouseListenerK == key)
                addMouseListener((MouseListener)(s.readObject()));

            else if (mouseMotionListenerK == key)
                addMouseMotionListener((MouseMotionListener)(s.readObject()));

            else if (inputMethodListenerK == key)
                addInputMethodListener((InputMethodListener)(s.readObject()));

            else // skip value for unrecognized key
                s.readObject();

        }

        // Read the component's orientation if it's present
        Object orient = null;

        try {
            orient = s.readObject();
        } catch (java.io.OptionalDataException e) {
            // JDK 1.1 instances will not have this optional data.
            // e.eof will be true to indicate that there is no more
            // data available for this object.
            // If e.eof is not true, throw the exception as it
            // might have been caused by reasons unrelated to
            // componentOrientation.

            if (!e.eof)  {
                throw (e);
            }
        }

        if (orient != null) {
            componentOrientation = (ComponentOrientation)orient;
        } else {
            componentOrientation = ComponentOrientation.UNKNOWN;
        }

        try {
            while(null != (keyOrNull = s.readObject())) {
                String key = ((String)keyOrNull).intern();

                if (hierarchyListenerK == key) {
                    addHierarchyListener((HierarchyListener)(s.readObject()));
                }
                else if (hierarchyBoundsListenerK == key) {
                    addHierarchyBoundsListener((HierarchyBoundsListener)
                                               (s.readObject()));
                }
                else {
                    // skip value for unrecognized key
                    s.readObject();
                }
            }
        } catch (java.io.OptionalDataException e) {
            // JDK 1.1/1.2 instances will not have this optional data.
            // e.eof will be true to indicate that there is no more
            // data available for this object.
            // If e.eof is not true, throw the exception as it
            // might have been caused by reasons unrelated to
            // hierarchy and hierarchyBounds listeners.

            if (!e.eof)  {
                throw (e);
            }
        }

        try {
            while (null != (keyOrNull = s.readObject())) {
                String key = ((String)keyOrNull).intern();

                if (mouseWheelListenerK == key) {
                    addMouseWheelListener((MouseWheelListener)(s.readObject()));
                }
                else {
                    // skip value for unrecognized key
                    s.readObject();
                }
            }
        } catch (java.io.OptionalDataException e) {
            // pre-1.3 instances will not have this optional data.
            // e.eof will be true to indicate that there is no more
            // data available for this object.
            // If e.eof is not true, throw the exception as it
            // might have been caused by reasons unrelated to
            // mouse wheel listeners

            if (!e.eof)  {
                throw (e);
            }
        }

        if (popups != null) {
            int npopups = popups.size();
            for (int i = 0 ; i < npopups ; i++) {
                PopupMenu popup = popups.elementAt(i);
                popup.parent = this;
            }
        }
    }

    /**
     * Sets the language-sensitive orientation that is to be used to order
     * the elements or text within this component.  Language-sensitive
     * {@code LayoutManager} and {@code Component}
     * subclasses will use this property to
     * determine how to lay out and draw components.
     * <p>
     * At construction time, a component's orientation is set to
     * {@code ComponentOrientation.UNKNOWN},
     * indicating that it has not been specified
     * explicitly.  The UNKNOWN orientation behaves the same as
     * {@code ComponentOrientation.LEFT_TO_RIGHT}.
     * <p>
     * To set the orientation of a single component, use this method.
     * To set the orientation of an entire component
     * hierarchy, use
     * {@link #applyComponentOrientation applyComponentOrientation}.
     * <p>
     * This method changes layout-related information, and therefore,
     * invalidates the component hierarchy.
     *
     * @param  o the orientation to be set
     *
     * @see ComponentOrientation
     * @see #invalidate
     *
     * @author Laura Werner, IBM
     */
    public void setComponentOrientation(ComponentOrientation o) {
        ComponentOrientation oldValue = componentOrientation;
        componentOrientation = o;

        // This is a bound property, so report the change to
        // any registered listeners.  (Cheap if there are none.)
        firePropertyChange("componentOrientation", oldValue, o);

        // This could change the preferred size of the Component.
        invalidateIfValid();
    }

    /**
     * Retrieves the language-sensitive orientation that is to be used to order
     * the elements or text within this component.  {@code LayoutManager}
     * and {@code Component}
     * subclasses that wish to respect orientation should call this method to
     * get the component's orientation before performing layout or drawing.
     *
     * @return the orientation to order the elements or text
     * @see ComponentOrientation
     *
     * @author Laura Werner, IBM
     */
    public ComponentOrientation getComponentOrientation() {
        return componentOrientation;
    }

    /**
     * Sets the {@code ComponentOrientation} property of this component
     * and all components contained within it.
     * <p>
     * This method changes layout-related information, and therefore,
     * invalidates the component hierarchy.
     *
     *
     * @param orientation the new component orientation of this component and
     *        the components contained within it.
     * @exception NullPointerException if {@code orientation} is null.
     * @see #setComponentOrientation
     * @see #getComponentOrientation
     * @see #invalidate
     * @since 1.4
     */
    public void applyComponentOrientation(ComponentOrientation orientation) {
        if (orientation == null) {
            throw new NullPointerException();
        }
        setComponentOrientation(orientation);
    }

    final boolean canBeFocusOwner() {
        // It is enabled, visible, focusable.
        if (isEnabled() && isDisplayable() && isVisible() && isFocusable()) {
            return true;
        }
        return false;
    }

    /**
     * Checks that this component meets the prerequisites to be focus owner:
     * - it is enabled, visible, focusable
     * - it's parents are all enabled and showing
     * - top-level window is focusable
     * - if focus cycle root has DefaultFocusTraversalPolicy then it also checks that this policy accepts
     * this component as focus owner
     * @since 1.5
     */
    final boolean canBeFocusOwnerRecursively() {
        // - it is enabled, visible, focusable
        if (!canBeFocusOwner()) {
            return false;
        }

        // - it's parents are all enabled and showing
        synchronized(getTreeLock()) {
            if (parent != null) {
                return parent.canContainFocusOwner(this);
            }
        }
        return true;
    }

    /**
     * Fix the location of the HW component in a LW container hierarchy.
     */
    final void relocateComponent() {
        synchronized (getTreeLock()) {
            if (peer == null) {
                return;
            }
            int nativeX = x;
            int nativeY = y;
            for (Component cont = getContainer();
                    cont != null && cont.isLightweight();
                    cont = cont.getContainer())
            {
                nativeX += cont.x;
                nativeY += cont.y;
            }
            peer.setBounds(nativeX, nativeY, width, height,
                    ComponentPeer.SET_LOCATION);
        }
    }

    /**
     * Returns the {@code Window} ancestor of the component.
     * @return Window ancestor of the component or component by itself if it is Window;
     *         null, if component is not a part of window hierarchy
     */
    Window getContainingWindow() {
        return SunToolkit.getContainingWindow(this);
    }

    /**
     * Initialize JNI field and method IDs
     */
    private static native void initIDs();

    /*
     * --- Accessibility Support ---
     *
     *  Component will contain all of the methods in interface Accessible,
     *  though it won't actually implement the interface - that will be up
     *  to the individual objects which extend Component.
     */

    /**
     * The {@code AccessibleContext} associated with this {@code Component}.
     */
    @SuppressWarnings("serial") // Not statically typed as Serializable
    protected AccessibleContext accessibleContext = null;

    /**
     * Gets the {@code AccessibleContext} associated
     * with this {@code Component}.
     * The method implemented by this base
     * class returns null.  Classes that extend {@code Component}
     * should implement this method to return the
     * {@code AccessibleContext} associated with the subclass.
     *
     *
     * @return the {@code AccessibleContext} of this
     *    {@code Component}
     * @since 1.3
     */
    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }

    /**
     * Inner class of Component used to provide default support for
     * accessibility.  This class is not meant to be used directly by
     * application developers, but is instead meant only to be
     * subclassed by component developers.
     * <p>
     * The class used to obtain the accessible role for this object.
     * @since 1.3
     */
    protected abstract class AccessibleAWTComponent extends AccessibleContext
        implements Serializable, AccessibleComponent {

        /**
         * Use serialVersionUID from JDK 1.3 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = 642321655757800191L;

        /**
         * Though the class is abstract, this should be called by
         * all sub-classes.
         */
        protected AccessibleAWTComponent() {
        }

        /**
         * Number of PropertyChangeListener objects registered. It's used
         * to add/remove ComponentListener and FocusListener to track
         * target Component's state.
         */
        private transient volatile int propertyListenersCount = 0;

        /**
         * A component listener to track show/hide/resize events
         * and convert them to PropertyChange events.
         */
        @SuppressWarnings("serial") // Not statically typed as Serializable
        protected ComponentListener accessibleAWTComponentHandler = null;

        /**
         * A listener to track focus events
         * and convert them to PropertyChange events.
         */
        @SuppressWarnings("serial") // Not statically typed as Serializable
        protected FocusListener accessibleAWTFocusHandler = null;

        /**
         * Fire PropertyChange listener, if one is registered,
         * when shown/hidden..
         * @since 1.3
         */
        protected class AccessibleAWTComponentHandler implements ComponentListener, Serializable {

            /**
             * Use serialVersionUID from JDK 1.3 for interoperability.
             */
            @Serial
            private static final long serialVersionUID = -1009684107426231869L;

            /**
             * Constructs an {@code AccessibleAWTComponentHandler}.
             */
            protected AccessibleAWTComponentHandler() {}

            public void componentHidden(ComponentEvent e)  {
                if (accessibleContext != null) {
                    accessibleContext.firePropertyChange(
                                                         AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                                                         AccessibleState.VISIBLE, null);
                }
            }

            public void componentShown(ComponentEvent e)  {
                if (accessibleContext != null) {
                    accessibleContext.firePropertyChange(
                                                         AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                                                         null, AccessibleState.VISIBLE);
                }
            }

            public void componentMoved(ComponentEvent e)  {
            }

            public void componentResized(ComponentEvent e)  {
            }
        } // inner class AccessibleAWTComponentHandler


        /**
         * Fire PropertyChange listener, if one is registered,
         * when focus events happen
         * @since 1.3
         */
        protected class AccessibleAWTFocusHandler implements FocusListener, Serializable {

            /**
             * Use serialVersionUID from JDK 1.3 for interoperability.
             */
            @Serial
            private static final long serialVersionUID = 3150908257351582233L;

            /**
             * Constructs an {@code AccessibleAWTFocusHandler}.
             */
            protected AccessibleAWTFocusHandler() {}

            public void focusGained(FocusEvent event) {
                if (accessibleContext != null) {
                    accessibleContext.firePropertyChange(
                                                         AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                                                         null, AccessibleState.FOCUSED);
                }
            }
            public void focusLost(FocusEvent event) {
                if (accessibleContext != null) {
                    accessibleContext.firePropertyChange(
                                                         AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                                                         AccessibleState.FOCUSED, null);
                }
            }
        }  // inner class AccessibleAWTFocusHandler


        /**
         * Adds a {@code PropertyChangeListener} to the listener list.
         *
         * @param listener  the property change listener to be added
         */
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            if (accessibleAWTComponentHandler == null) {
                accessibleAWTComponentHandler = new AccessibleAWTComponentHandler();
            }
            if (accessibleAWTFocusHandler == null) {
                accessibleAWTFocusHandler = new AccessibleAWTFocusHandler();
            }
            if (propertyListenersCount++ == 0) {
                Component.this.addComponentListener(accessibleAWTComponentHandler);
                Component.this.addFocusListener(accessibleAWTFocusHandler);
            }
            super.addPropertyChangeListener(listener);
        }

        /**
         * Remove a PropertyChangeListener from the listener list.
         * This removes a PropertyChangeListener that was registered
         * for all properties.
         *
         * @param listener  The PropertyChangeListener to be removed
         */
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            if (--propertyListenersCount == 0) {
                Component.this.removeComponentListener(accessibleAWTComponentHandler);
                Component.this.removeFocusListener(accessibleAWTFocusHandler);
            }
            super.removePropertyChangeListener(listener);
        }

        // AccessibleContext methods
        //
        /**
         * Gets the accessible name of this object.  This should almost never
         * return {@code java.awt.Component.getName()},
         * as that generally isn't a localized name,
         * and doesn't have meaning for the user.  If the
         * object is fundamentally a text object (e.g. a menu item), the
         * accessible name should be the text of the object (e.g. "save").
         * If the object has a tooltip, the tooltip text may also be an
         * appropriate String to return.
         *
         * @return the localized name of the object -- can be
         *         {@code null} if this
         *         object does not have a name
         * @see javax.accessibility.AccessibleContext#setAccessibleName
         */
        public String getAccessibleName() {
            return accessibleName;
        }

        /**
         * Gets the accessible description of this object.  This should be
         * a concise, localized description of what this object is - what
         * is its meaning to the user.  If the object has a tooltip, the
         * tooltip text may be an appropriate string to return, assuming
         * it contains a concise description of the object (instead of just
         * the name of the object - e.g. a "Save" icon on a toolbar that
         * had "save" as the tooltip text shouldn't return the tooltip
         * text as the description, but something like "Saves the current
         * text document" instead).
         *
         * @return the localized description of the object -- can be
         *        {@code null} if this object does not have a description
         * @see javax.accessibility.AccessibleContext#setAccessibleDescription
         */
        public String getAccessibleDescription() {
            return accessibleDescription;
        }

        /**
         * Gets the role of this object.
         *
         * @return an instance of {@code AccessibleRole}
         *      describing the role of the object
         * @see javax.accessibility.AccessibleRole
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.AWT_COMPONENT;
        }

        /**
         * Gets the state of this object.
         *
         * @return an instance of {@code AccessibleStateSet}
         *       containing the current state set of the object
         * @see javax.accessibility.AccessibleState
         */
        public AccessibleStateSet getAccessibleStateSet() {
            return Component.this.getAccessibleStateSet();
        }

        /**
         * Gets the {@code Accessible} parent of this object.
         * If the parent of this object implements {@code Accessible},
         * this method should simply return {@code getParent}.
         *
         * @return the {@code Accessible} parent of this
         *      object -- can be {@code null} if this
         *      object does not have an {@code Accessible} parent
         */
        public Accessible getAccessibleParent() {
            if (accessibleParent != null) {
                return accessibleParent;
            } else {
                Container parent = getParent();
                if (parent instanceof Accessible) {
                    return (Accessible) parent;
                }
            }
            return null;
        }

        /**
         * Gets the index of this object in its accessible parent.
         *
         * @return the index of this object in its parent; or -1 if this
         *    object does not have an accessible parent
         * @see #getAccessibleParent
         */
        public int getAccessibleIndexInParent() {
            return Component.this.getAccessibleIndexInParent();
        }

        /**
         * Returns the number of accessible children in the object.  If all
         * of the children of this object implement {@code Accessible},
         * then this method should return the number of children of this object.
         *
         * @return the number of accessible children in the object
         */
        public int getAccessibleChildrenCount() {
            return 0; // Components don't have children
        }

        /**
         * Returns the nth {@code Accessible} child of the object.
         *
         * @param i zero-based index of child
         * @return the nth {@code Accessible} child of the object
         */
        public Accessible getAccessibleChild(int i) {
            return null; // Components don't have children
        }

        /**
         * Returns the locale of this object.
         *
         * @return the locale of this object
         */
        public Locale getLocale() {
            return Component.this.getLocale();
        }

        /**
         * Gets the {@code AccessibleComponent} associated
         * with this object if one exists.
         * Otherwise return {@code null}.
         *
         * @return the component
         */
        public AccessibleComponent getAccessibleComponent() {
            return this;
        }


        // AccessibleComponent methods
        //
        /**
         * Gets the background color of this object.
         *
         * @return the background color, if supported, of the object;
         *      otherwise, {@code null}
         */
        public Color getBackground() {
            return Component.this.getBackground();
        }

        /**
         * Sets the background color of this object.
         * (For transparency, see {@code isOpaque}.)
         *
         * @param c the new {@code Color} for the background
         * @see Component#isOpaque
         */
        public void setBackground(Color c) {
            Component.this.setBackground(c);
        }

        /**
         * Gets the foreground color of this object.
         *
         * @return the foreground color, if supported, of the object;
         *     otherwise, {@code null}
         */
        public Color getForeground() {
            return Component.this.getForeground();
        }

        /**
         * Sets the foreground color of this object.
         *
         * @param c the new {@code Color} for the foreground
         */
        public void setForeground(Color c) {
            Component.this.setForeground(c);
        }

        /**
         * Gets the {@code Cursor} of this object.
         *
         * @return the {@code Cursor}, if supported,
         *     of the object; otherwise, {@code null}
         */
        public Cursor getCursor() {
            return Component.this.getCursor();
        }

        /**
         * Sets the {@code Cursor} of this object.
         * <p>
         * The method may have no visual effect if the Java platform
         * implementation and/or the native system do not support
         * changing the mouse cursor shape.
         * @param cursor the new {@code Cursor} for the object
         */
        public void setCursor(Cursor cursor) {
            Component.this.setCursor(cursor);
        }

        /**
         * Gets the {@code Font} of this object.
         *
         * @return the {@code Font}, if supported,
         *    for the object; otherwise, {@code null}
         */
        public Font getFont() {
            return Component.this.getFont();
        }

        /**
         * Sets the {@code Font} of this object.
         *
         * @param f the new {@code Font} for the object
         */
        public void setFont(Font f) {
            Component.this.setFont(f);
        }

        /**
         * Gets the {@code FontMetrics} of this object.
         *
         * @param f the {@code Font}
         * @return the {@code FontMetrics}, if supported,
         *     the object; otherwise, {@code null}
         * @see #getFont
         */
        public FontMetrics getFontMetrics(Font f) {
            if (f == null) {
                return null;
            } else {
                return Component.this.getFontMetrics(f);
            }
        }

        /**
         * Determines if the object is enabled.
         *
         * @return true if object is enabled; otherwise, false
         */
        public boolean isEnabled() {
            return Component.this.isEnabled();
        }

        /**
         * Sets the enabled state of the object.
         *
         * @param b if true, enables this object; otherwise, disables it
         */
        public void setEnabled(boolean b) {
            boolean old = Component.this.isEnabled();
            Component.this.setEnabled(b);
            if (b != old) {
                if (accessibleContext != null) {
                    if (b) {
                        accessibleContext.firePropertyChange(
                                                             AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                                                             null, AccessibleState.ENABLED);
                    } else {
                        accessibleContext.firePropertyChange(
                                                             AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                                                             AccessibleState.ENABLED, null);
                    }
                }
            }
        }

        /**
         * Determines if the object is visible.  Note: this means that the
         * object intends to be visible; however, it may not in fact be
         * showing on the screen because one of the objects that this object
         * is contained by is not visible.  To determine if an object is
         * showing on the screen, use {@code isShowing}.
         *
         * @return true if object is visible; otherwise, false
         */
        public boolean isVisible() {
            return Component.this.isVisible();
        }

        /**
         * Sets the visible state of the object.
         *
         * @param b if true, shows this object; otherwise, hides it
         */
        public void setVisible(boolean b) {
            boolean old = Component.this.isVisible();
            Component.this.setVisible(b);
            if (b != old) {
                if (accessibleContext != null) {
                    if (b) {
                        accessibleContext.firePropertyChange(
                                                             AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                                                             null, AccessibleState.VISIBLE);
                    } else {
                        accessibleContext.firePropertyChange(
                                                             AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                                                             AccessibleState.VISIBLE, null);
                    }
                }
            }
        }

        /**
         * Determines if the object is showing.  This is determined by checking
         * the visibility of the object and ancestors of the object.  Note:
         * this will return true even if the object is obscured by another
         * (for example, it happens to be underneath a menu that was pulled
         * down).
         *
         * @return true if object is showing; otherwise, false
         */
        public boolean isShowing() {
            return Component.this.isShowing();
        }

        /**
         * Checks whether the specified point is within this object's bounds,
         * where the point's x and y coordinates are defined to be relative to
         * the coordinate system of the object.
         *
         * @param p the {@code Point} relative to the
         *     coordinate system of the object
         * @return true if object contains {@code Point}; otherwise false
         */
        public boolean contains(Point p) {
            return Component.this.contains(p);
        }

        /**
         * Returns the location of the object on the screen.
         *
         * @return location of object on screen -- can be
         *    {@code null} if this object is not on the screen
         */
        public Point getLocationOnScreen() {
            synchronized (Component.this.getTreeLock()) {
                if (Component.this.isShowing()) {
                    return Component.this.getLocationOnScreen();
                } else {
                    return null;
                }
            }
        }

        /**
         * Gets the location of the object relative to the parent in the form
         * of a point specifying the object's top-left corner in the screen's
         * coordinate space.
         *
         * @return an instance of Point representing the top-left corner of
         * the object's bounds in the coordinate space of the screen;
         * {@code null} if this object or its parent are not on the screen
         */
        public Point getLocation() {
            return Component.this.getLocation();
        }

        /**
         * Sets the location of the object relative to the parent.
         * @param p  the coordinates of the object
         */
        public void setLocation(Point p) {
            Component.this.setLocation(p);
        }

        /**
         * Gets the bounds of this object in the form of a Rectangle object.
         * The bounds specify this object's width, height, and location
         * relative to its parent.
         *
         * @return a rectangle indicating this component's bounds;
         *   {@code null} if this object is not on the screen
         */
        public Rectangle getBounds() {
            return Component.this.getBounds();
        }

        /**
         * Sets the bounds of this object in the form of a
         * {@code Rectangle} object.
         * The bounds specify this object's width, height, and location
         * relative to its parent.
         *
         * @param r a rectangle indicating this component's bounds
         */
        public void setBounds(Rectangle r) {
            Component.this.setBounds(r);
        }

        /**
         * Returns the size of this object in the form of a
         * {@code Dimension} object. The height field of the
         * {@code Dimension} object contains this object's
         * height, and the width field of the {@code Dimension}
         * object contains this object's width.
         *
         * @return a {@code Dimension} object that indicates
         *     the size of this component; {@code null} if
         *     this object is not on the screen
         */
        public Dimension getSize() {
            return Component.this.getSize();
        }

        /**
         * Resizes this object so that it has width and height.
         *
         * @param d the dimension specifying the new size of the object
         */
        public void setSize(Dimension d) {
            Component.this.setSize(d);
        }

        /**
         * Returns the {@code Accessible} child,
         * if one exists, contained at the local
         * coordinate {@code Point}.  Otherwise returns
         * {@code null}.
         *
         * @param p the point defining the top-left corner of
         *      the {@code Accessible}, given in the
         *      coordinate space of the object's parent
         * @return the {@code Accessible}, if it exists,
         *      at the specified location; else {@code null}
         */
        public Accessible getAccessibleAt(Point p) {
            return null; // Components don't have children
        }

        /**
         * Returns whether this object can accept focus or not.
         *
         * @return true if object can accept focus; otherwise false
         */
        public boolean isFocusTraversable() {
            return Component.this.isFocusTraversable();
        }

        /**
         * Requests focus for this object.
         */
        public void requestFocus() {
            Component.this.requestFocus();
        }

        /**
         * Adds the specified focus listener to receive focus events from this
         * component.
         *
         * @param l the focus listener
         */
        public void addFocusListener(FocusListener l) {
            Component.this.addFocusListener(l);
        }

        /**
         * Removes the specified focus listener so it no longer receives focus
         * events from this component.
         *
         * @param l the focus listener
         */
        public void removeFocusListener(FocusListener l) {
            Component.this.removeFocusListener(l);
        }

    } // inner class AccessibleAWTComponent


    /**
     * Gets the index of this object in its accessible parent.
     * If this object does not have an accessible parent, returns
     * -1.
     *
     * @return the index of this object in its accessible parent
     */
    int getAccessibleIndexInParent() {
        synchronized (getTreeLock()) {

            AccessibleContext accContext = getAccessibleContext();
            if (accContext == null) {
                return -1;
            }

            Accessible parent = accContext.getAccessibleParent();
            if (parent == null) {
                return -1;
            }

            accContext = parent.getAccessibleContext();
            for (int i = 0; i < accContext.getAccessibleChildrenCount(); i++) {
                if (this.equals(accContext.getAccessibleChild(i))) {
                    return i;
                }
            }

            return -1;
        }
    }

    /**
     * Gets the current state set of this object.
     *
     * @return an instance of {@code AccessibleStateSet}
     *    containing the current state set of the object
     * @see AccessibleState
     */
    AccessibleStateSet getAccessibleStateSet() {
        synchronized (getTreeLock()) {
            AccessibleStateSet states = new AccessibleStateSet();
            if (this.isEnabled()) {
                states.add(AccessibleState.ENABLED);
            }
            if (this.isFocusTraversable()) {
                states.add(AccessibleState.FOCUSABLE);
            }
            if (this.isVisible()) {
                states.add(AccessibleState.VISIBLE);
            }
            if (this.isShowing()) {
                states.add(AccessibleState.SHOWING);
            }
            if (this.isFocusOwner()) {
                states.add(AccessibleState.FOCUSED);
            }
            if (this instanceof Accessible) {
                AccessibleContext ac = ((Accessible) this).getAccessibleContext();
                if (ac != null) {
                    Accessible ap = ac.getAccessibleParent();
                    if (ap != null) {
                        AccessibleContext pac = ap.getAccessibleContext();
                        if (pac != null) {
                            AccessibleSelection as = pac.getAccessibleSelection();
                            if (as != null) {
                                states.add(AccessibleState.SELECTABLE);
                                int i = ac.getAccessibleIndexInParent();
                                if (i >= 0) {
                                    if (as.isAccessibleChildSelected(i)) {
                                        states.add(AccessibleState.SELECTED);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (Component.isInstanceOf(this, "javax.swing.JComponent")) {
                if (((javax.swing.JComponent) this).isOpaque()) {
                    states.add(AccessibleState.OPAQUE);
                }
            }
            return states;
        }
    }

    /**
     * Checks that the given object is instance of the given class.
     * @param obj Object to be checked
     * @param className The name of the class. Must be fully-qualified class name.
     * @return true, if this object is instanceof given class,
     *         false, otherwise, or if obj or className is null
     */
    static boolean isInstanceOf(Object obj, String className) {
        if (obj == null) return false;
        if (className == null) return false;

        Class<?> cls = obj.getClass();
        while (cls != null) {
            if (cls.getName().equals(className)) {
                return true;
            }
            cls = cls.getSuperclass();
        }
        return false;
    }


    // ************************** MIXING CODE *******************************

    /**
     * Check whether we can trust the current bounds of the component.
     * The return value of false indicates that the container of the
     * component is invalid, and therefore needs to be laid out, which would
     * probably mean changing the bounds of its children.
     * Null-layout of the container or absence of the container mean
     * the bounds of the component are final and can be trusted.
     */
    final boolean areBoundsValid() {
        Container cont = getContainer();
        return cont == null || cont.isValid() || cont.getLayout() == null;
    }

    /**
     * Applies the shape to the component
     * @param shape Shape to be applied to the component
     */
    void applyCompoundShape(Region shape) {
        checkTreeLock();

        if (!areBoundsValid()) {
            if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
                mixingLog.fine("this = " + this + "; areBoundsValid = " + areBoundsValid());
            }
            return;
        }

        if (!isLightweight()) {
            ComponentPeer peer = this.peer;
            if (peer != null) {
                // The Region class has some optimizations. That's why
                // we should manually check whether it's empty and
                // substitute the object ourselves. Otherwise we end up
                // with some incorrect Region object with loX being
                // greater than the hiX for instance.
                if (shape.isEmpty()) {
                    shape = Region.EMPTY_REGION;
                }


                // Note: the shape is not really copied/cloned. We create
                // the Region object ourselves, so there's no any possibility
                // to modify the object outside of the mixing code.
                // Nullifying compoundShape means that the component has normal shape
                // (or has no shape at all).
                if (shape.equals(getNormalShape())) {
                    if (this.compoundShape == null) {
                        return;
                    }
                    this.compoundShape = null;
                    peer.applyShape(null);
                } else {
                    if (shape.equals(getAppliedShape())) {
                        return;
                    }
                    this.compoundShape = shape;
                    Point compAbsolute = getLocationOnWindow();
                    if (mixingLog.isLoggable(PlatformLogger.Level.FINER)) {
                        mixingLog.fine("this = " + this +
                                "; compAbsolute=" + compAbsolute + "; shape=" + shape);
                    }
                    peer.applyShape(shape.getTranslatedRegion(-compAbsolute.x, -compAbsolute.y));
                }
            }
        }
    }

    /**
     * Returns the shape previously set with applyCompoundShape().
     * If the component is LW or no shape was applied yet,
     * the method returns the normal shape.
     */
    private Region getAppliedShape() {
        checkTreeLock();
        //XXX: if we allow LW components to have a shape, this must be changed
        return (this.compoundShape == null || isLightweight()) ? getNormalShape() : this.compoundShape;
    }

    Point getLocationOnWindow() {
        checkTreeLock();
        Point curLocation = getLocation();

        for (Container parent = getContainer();
                parent != null && !(parent instanceof Window);
                parent = parent.getContainer())
        {
            curLocation.x += parent.getX();
            curLocation.y += parent.getY();
        }

        return curLocation;
    }

    /**
     * Returns the full shape of the component located in window coordinates
     */
    final Region getNormalShape() {
        checkTreeLock();
        //XXX: we may take into account a user-specified shape for this component
        Point compAbsolute = getLocationOnWindow();
        return
            Region.getInstanceXYWH(
                    compAbsolute.x,
                    compAbsolute.y,
                    getWidth(),
                    getHeight()
            );
    }

    /**
     * Returns the "opaque shape" of the component.
     *
     * The opaque shape of a lightweight components is the actual shape that
     * needs to be cut off of the heavyweight components in order to mix this
     * lightweight component correctly with them.
     *
     * The method is overriden in the java.awt.Container to handle non-opaque
     * containers containing opaque children.
     *
     * See 6637655 for details.
     */
    Region getOpaqueShape() {
        checkTreeLock();
        if (mixingCutoutRegion != null) {
            return mixingCutoutRegion;
        } else {
            return getNormalShape();
        }
    }

    final int getSiblingIndexAbove() {
        checkTreeLock();
        Container parent = getContainer();
        if (parent == null) {
            return -1;
        }

        int nextAbove = parent.getComponentZOrder(this) - 1;

        return nextAbove < 0 ? -1 : nextAbove;
    }

    final ComponentPeer getHWPeerAboveMe() {
        checkTreeLock();

        Container cont = getContainer();
        int indexAbove = getSiblingIndexAbove();

        while (cont != null) {
            for (int i = indexAbove; i > -1; i--) {
                Component comp = cont.getComponent(i);
                if (comp != null && comp.isDisplayable() && !comp.isLightweight()) {
                    return comp.peer;
                }
            }
            // traversing the hierarchy up to the closest HW container;
            // further traversing may return a component that is not actually
            // a native sibling of this component and this kind of z-order
            // request may not be allowed by the underlying system (6852051).
            if (!cont.isLightweight()) {
                break;
            }

            indexAbove = cont.getSiblingIndexAbove();
            cont = cont.getContainer();
        }

        return null;
    }

    final int getSiblingIndexBelow() {
        checkTreeLock();
        Container parent = getContainer();
        if (parent == null) {
            return -1;
        }

        int nextBelow = parent.getComponentZOrder(this) + 1;

        return nextBelow >= parent.getComponentCount() ? -1 : nextBelow;
    }

    final boolean isNonOpaqueForMixing() {
        return mixingCutoutRegion != null &&
            mixingCutoutRegion.isEmpty();
    }

    private Region calculateCurrentShape() {
        checkTreeLock();
        Region s = getNormalShape();

        if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
            mixingLog.fine("this = " + this + "; normalShape=" + s);
        }

        if (getContainer() != null) {
            Component comp = this;
            Container cont = comp.getContainer();

            while (cont != null) {
                for (int index = comp.getSiblingIndexAbove(); index != -1; --index) {
                    /* It is assumed that:
                     *
                     *    getComponent(getContainer().getComponentZOrder(comp)) == comp
                     *
                     * The assumption has been made according to the current
                     * implementation of the Container class.
                     */
                    Component c = cont.getComponent(index);
                    if (c.isLightweight() && c.isShowing()) {
                        s = s.getDifference(c.getOpaqueShape());
                    }
                }

                if (cont.isLightweight()) {
                    s = s.getIntersection(cont.getNormalShape());
                } else {
                    break;
                }

                comp = cont;
                cont = cont.getContainer();
            }
        }

        if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
            mixingLog.fine("currentShape=" + s);
        }

        return s;
    }

    void applyCurrentShape() {
        checkTreeLock();
        if (!areBoundsValid()) {
            if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
                mixingLog.fine("this = " + this + "; areBoundsValid = " + areBoundsValid());
            }
            return; // Because applyCompoundShape() ignores such components anyway
        }
        if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
            mixingLog.fine("this = " + this);
        }
        applyCompoundShape(calculateCurrentShape());
    }

    final void subtractAndApplyShape(Region s) {
        checkTreeLock();

        if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
            mixingLog.fine("this = " + this + "; s=" + s);
        }

        applyCompoundShape(getAppliedShape().getDifference(s));
    }

    private void applyCurrentShapeBelowMe() {
        checkTreeLock();
        Container parent = getContainer();
        if (parent != null && parent.isShowing()) {
            // First, reapply shapes of my siblings
            parent.recursiveApplyCurrentShape(getSiblingIndexBelow());

            // Second, if my container is non-opaque, reapply shapes of siblings of my container
            Container parent2 = parent.getContainer();
            while (!parent.isOpaque() && parent2 != null) {
                parent2.recursiveApplyCurrentShape(parent.getSiblingIndexBelow());

                parent = parent2;
                parent2 = parent.getContainer();
            }
        }
    }

    final void subtractAndApplyShapeBelowMe() {
        checkTreeLock();
        Container parent = getContainer();
        if (parent != null && isShowing()) {
            Region opaqueShape = getOpaqueShape();

            // First, cut my siblings
            parent.recursiveSubtractAndApplyShape(opaqueShape, getSiblingIndexBelow());

            // Second, if my container is non-opaque, cut siblings of my container
            Container parent2 = parent.getContainer();
            while (!parent.isOpaque() && parent2 != null) {
                parent2.recursiveSubtractAndApplyShape(opaqueShape, parent.getSiblingIndexBelow());

                parent = parent2;
                parent2 = parent.getContainer();
            }
        }
    }

    void mixOnShowing() {
        synchronized (getTreeLock()) {
            if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
                mixingLog.fine("this = " + this);
            }
            if (!isMixingNeeded()) {
                return;
            }
            if (isLightweight()) {
                subtractAndApplyShapeBelowMe();
            } else {
                applyCurrentShape();
            }
        }
    }

    void mixOnHiding(boolean isLightweight) {
        // We cannot be sure that the peer exists at this point, so we need the argument
        //    to find out whether the hiding component is (well, actually was) a LW or a HW.
        synchronized (getTreeLock()) {
            if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
                mixingLog.fine("this = " + this + "; isLightweight = " + isLightweight);
            }
            if (!isMixingNeeded()) {
                return;
            }
            if (isLightweight) {
                applyCurrentShapeBelowMe();
            }
        }
    }

    void mixOnReshaping() {
        synchronized (getTreeLock()) {
            if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
                mixingLog.fine("this = " + this);
            }
            if (!isMixingNeeded()) {
                return;
            }
            if (isLightweight()) {
                applyCurrentShapeBelowMe();
            } else {
                applyCurrentShape();
            }
        }
    }

    void mixOnZOrderChanging(int oldZorder, int newZorder) {
        synchronized (getTreeLock()) {
            boolean becameHigher = newZorder < oldZorder;
            Container parent = getContainer();

            if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
                mixingLog.fine("this = " + this +
                    "; oldZorder=" + oldZorder + "; newZorder=" + newZorder + "; parent=" + parent);
            }
            if (!isMixingNeeded()) {
                return;
            }
            if (isLightweight()) {
                if (becameHigher) {
                    if (parent != null && isShowing()) {
                        parent.recursiveSubtractAndApplyShape(getOpaqueShape(), getSiblingIndexBelow(), oldZorder);
                    }
                } else {
                    if (parent != null) {
                        parent.recursiveApplyCurrentShape(oldZorder, newZorder);
                    }
                }
            } else {
                if (becameHigher) {
                    applyCurrentShape();
                } else {
                    if (parent != null) {
                        Region shape = getAppliedShape();

                        for (int index = oldZorder; index < newZorder; index++) {
                            Component c = parent.getComponent(index);
                            if (c.isLightweight() && c.isShowing()) {
                                shape = shape.getDifference(c.getOpaqueShape());
                            }
                        }
                        applyCompoundShape(shape);
                    }
                }
            }
        }
    }

    void mixOnValidating() {
        // This method gets overriden in the Container. Obviously, a plain
        // non-container components don't need to handle validation.
    }

    final boolean isMixingNeeded() {
        if (SunToolkit.getSunAwtDisableMixing()) {
            if (mixingLog.isLoggable(PlatformLogger.Level.FINEST)) {
                mixingLog.finest("this = " + this + "; Mixing disabled via sun.awt.disableMixing");
            }
            return false;
        }
        if (!areBoundsValid()) {
            if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
                mixingLog.fine("this = " + this + "; areBoundsValid = " + areBoundsValid());
            }
            return false;
        }
        Window window = getContainingWindow();
        if (window != null) {
            if (!window.hasHeavyweightDescendants() || !window.hasLightweightDescendants() || window.isDisposing()) {
                if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
                    mixingLog.fine("containing window = " + window +
                            "; has h/w descendants = " + window.hasHeavyweightDescendants() +
                            "; has l/w descendants = " + window.hasLightweightDescendants() +
                            "; disposing = " + window.isDisposing());
                }
                return false;
            }
        } else {
            if (mixingLog.isLoggable(PlatformLogger.Level.FINE)) {
                mixingLog.fine("this = " + this + "; containing window is null");
            }
            return false;
        }
        return true;
    }

    /**
     * Sets a 'mixing-cutout' shape for this lightweight component.
     *
     * This method is used exclusively for the purposes of the
     * Heavyweight/Lightweight Components Mixing feature and will
     * have no effect if applied to a heavyweight component.
     *
     * By default a lightweight component is treated as an opaque rectangle for
     * the purposes of the Heavyweight/Lightweight Components Mixing feature.
     * This method enables developers to set an arbitrary shape to be cut out
     * from heavyweight components positioned underneath the lightweight
     * component in the z-order.
     * <p>
     * The {@code shape} argument may have the following values:
     * <ul>
     * <li>{@code null} - reverts the default cutout shape (the rectangle equal
     * to the component's {@code getBounds()})
     * <li><i>empty-shape</i> - does not cut out anything from heavyweight
     * components. This makes this lightweight component effectively
     * transparent. Note that descendants of the lightweight component still
     * affect the shapes of heavyweight components.  An example of an
     * <i>empty-shape</i> is {@code new Rectangle()}.
     * <li><i>non-empty-shape</i> - the given shape will be cut out from
     * heavyweight components.
     * </ul>
     * <p>
     * The most common example when the 'mixing-cutout' shape is needed is a
     * glass pane component. The {@link JRootPane#setGlassPane} method
     * automatically sets the <i>empty-shape</i> as the 'mixing-cutout' shape
     * for the given glass pane component.  If a developer needs some other
     * 'mixing-cutout' shape for the glass pane (which is rare), this must be
     * changed manually after installing the glass pane to the root pane.
     *
     * @param shape the new 'mixing-cutout' shape
     * @since 9
     */
    public void setMixingCutoutShape(Shape shape) {
        Region region = shape == null ? null : Region.getInstance(shape, null);

        synchronized (getTreeLock()) {
            boolean needShowing = false;
            boolean needHiding = false;

            if (!isNonOpaqueForMixing()) {
                needHiding = true;
            }

            mixingCutoutRegion = region;

            if (!isNonOpaqueForMixing()) {
                needShowing = true;
            }

            if (isMixingNeeded()) {
                if (needHiding) {
                    mixOnHiding(isLightweight());
                }
                if (needShowing) {
                    mixOnShowing();
                }
            }
        }
    }

    // ****************** END OF MIXING CODE ********************************

    // Note that the method is overriden in the Window class,
    // a window doesn't need to be updated in the Z-order.
    void updateZOrder() {
        peer.setZOrder(getHWPeerAboveMe());
    }

}
