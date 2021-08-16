/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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

import sun.awt.AWTAccessor;

import javax.swing.plaf.LayerUI;
import javax.swing.border.Border;
import javax.accessibility.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.util.ArrayList;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * {@code JLayer} is a universal decorator for Swing components
 * which enables you to implement various advanced painting effects as well as
 * receive notifications of all {@code AWTEvent}s generated within its borders.
 * <p>
 * {@code JLayer} delegates the handling of painting and input events to a
 * {@link javax.swing.plaf.LayerUI} object, which performs the actual decoration.
 * <p>
 * The custom painting implemented in the {@code LayerUI} and events notification
 * work for the JLayer itself and all its subcomponents.
 * This combination enables you to enrich existing components
 * by adding new advanced functionality such as temporary locking of a hierarchy,
 * data tips for compound components, enhanced mouse scrolling etc and so on.
 * <p>
 * {@code JLayer} is a good solution if you only need to do custom painting
 * over compound component or catch input events from its subcomponents.
 * <pre>
 * import javax.swing.*;
 * import javax.swing.plaf.LayerUI;
 * import java.awt.*;
 *
 * public class JLayerSample {
 *
 *     private static JLayer&lt;JComponent&gt; createLayer() {
 *         // This custom layerUI will fill the layer with translucent green
 *         // and print out all mouseMotion events generated within its borders
 *         LayerUI&lt;JComponent&gt; layerUI = new LayerUI&lt;JComponent&gt;() {
 *
 *             public void paint(Graphics g, JComponent c) {
 *                 // paint the layer as is
 *                 super.paint(g, c);
 *                 // fill it with the translucent green
 *                 g.setColor(new Color(0, 128, 0, 128));
 *                 g.fillRect(0, 0, c.getWidth(), c.getHeight());
 *             }
 *
 *             public void installUI(JComponent c) {
 *                 super.installUI(c);
 *                 // enable mouse motion events for the layer's subcomponents
 *                 ((JLayer) c).setLayerEventMask(AWTEvent.MOUSE_MOTION_EVENT_MASK);
 *             }
 *
 *             public void uninstallUI(JComponent c) {
 *                 super.uninstallUI(c);
 *                 // reset the layer event mask
 *                 ((JLayer) c).setLayerEventMask(0);
 *             }
 *
 *             // overridden method which catches MouseMotion events
 *             public void eventDispatched(AWTEvent e, JLayer&lt;? extends JComponent&gt; l) {
 *                 System.out.println("AWTEvent detected: " + e);
 *             }
 *         };
 *         // create a component to be decorated with the layer
 *         JPanel panel = new JPanel();
 *         panel.add(new JButton("JButton"));
 *
 *         // create the layer for the panel using our custom layerUI
 *         return new JLayer&lt;JComponent&gt;(panel, layerUI);
 *     }
 *
 *     private static void createAndShowGUI() {
 *         final JFrame frame = new JFrame();
 *         frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
 *
 *         // work with the layer as with any other Swing component
 *         frame.add(createLayer());
 *
 *         frame.setSize(200, 200);
 *         frame.setLocationRelativeTo(null);
 *         frame.setVisible(true);
 *     }
 *
 *     public static void main(String[] args) throws Exception {
 *         SwingUtilities.invokeAndWait(new Runnable() {
 *             public void run() {
 *                 createAndShowGUI();
 *             }
 *         });
 *     }
 * }
 * </pre>
 *
 * <b>Note:</b> {@code JLayer} doesn't support the following methods:
 * <ul>
 * <li>{@link Container#add(java.awt.Component)}</li>
 * <li>{@link Container#add(String, java.awt.Component)}</li>
 * <li>{@link Container#add(java.awt.Component, int)}</li>
 * <li>{@link Container#add(java.awt.Component, Object)}</li>
 * <li>{@link Container#add(java.awt.Component, Object, int)}</li>
 * </ul>
 * using any of them will cause {@code UnsupportedOperationException} to be thrown,
 * to add a component to {@code JLayer}
 * use {@link #setView(Component)} or {@link #setGlassPane(JPanel)}.
 *
 * @param <V> the type of {@code JLayer}'s view component
 *
 * @see #JLayer(Component)
 * @see #setView(Component)
 * @see #getView()
 * @see javax.swing.plaf.LayerUI
 * @see #JLayer(Component, LayerUI)
 * @see #setUI(javax.swing.plaf.LayerUI)
 * @see #getUI()
 * @since 1.7
 *
 * @author Alexander Potochkin
 */
@SuppressWarnings("serial") // Superclass is not serializable across versions
public final class JLayer<V extends Component>
        extends JComponent
        implements Scrollable, PropertyChangeListener, Accessible {
    private V view;
    // this field is necessary because JComponent.ui is transient
    // when layerUI is serializable
    private LayerUI<? super V> layerUI;
    private JPanel glassPane;
    private long eventMask;
    private transient boolean isPaintCalling;
    private transient boolean isPaintImmediatelyCalling;
    private transient boolean isImageUpdateCalling;

    private static final LayerEventController eventController =
            new LayerEventController();

    /**
     * Creates a new {@code JLayer} object with a {@code null} view component
     * and default {@link javax.swing.plaf.LayerUI}.
     *
     * @see #setView
     * @see #setUI
     */
    public JLayer() {
        this(null);
    }

    /**
     * Creates a new {@code JLayer} object
     * with default {@link javax.swing.plaf.LayerUI}.
     *
     * @param view the component to be decorated by this {@code JLayer}
     *
     * @see #setUI
     */
    public JLayer(V view) {
        this(view, new LayerUI<V>());
    }

    /**
     * Creates a new {@code JLayer} object with the specified view component
     * and {@link javax.swing.plaf.LayerUI} object.
     *
     * @param view the component to be decorated
     * @param ui the {@link javax.swing.plaf.LayerUI} delegate
     * to be used by this {@code JLayer}
     */
    public JLayer(V view, LayerUI<V> ui) {
        setGlassPane(createGlassPane());
        setView(view);
        setUI(ui);
    }

    /**
     * Returns the {@code JLayer}'s view component or {@code null}.
     * <br>This is a bound property.
     *
     * @return the {@code JLayer}'s view component
     *         or {@code null} if none exists
     *
     * @see #setView(Component)
     */
    public V getView() {
        return view;
    }

    /**
     * Sets the {@code JLayer}'s view component, which can be {@code null}.
     * <br>This is a bound property.
     *
     * @param view the view component for this {@code JLayer}
     *
     * @see #getView()
     */
    public void setView(V view) {
        Component oldView = getView();
        if (oldView != null) {
            super.remove(oldView);
        }
        if (view != null) {
            super.addImpl(view, null, getComponentCount());
        }
        this.view = view;
        firePropertyChange("view", oldView, view);
        revalidate();
        repaint();
    }

    /**
     * Sets the {@link javax.swing.plaf.LayerUI} which will perform painting
     * and receive input events for this {@code JLayer}.
     *
     * @param ui the {@link javax.swing.plaf.LayerUI} for this {@code JLayer}
     */
    public void setUI(LayerUI<? super V> ui) {
        this.layerUI = ui;
        super.setUI(ui);
    }

    /**
     * Returns the {@link javax.swing.plaf.LayerUI} for this {@code JLayer}.
     *
     * @return the {@code LayerUI} for this {@code JLayer}
     */
    public LayerUI<? super V> getUI() {
        return layerUI;
    }

    /**
     * Returns the {@code JLayer}'s glassPane component or {@code null}.
     * <br>This is a bound property.
     *
     * @return the {@code JLayer}'s glassPane component
     *         or {@code null} if none exists
     *
     * @see #setGlassPane(JPanel)
     */
    public JPanel getGlassPane() {
        return glassPane;
    }

    /**
     * Sets the {@code JLayer}'s glassPane component, which can be {@code null}.
     * <br>This is a bound property.
     *
     * @param glassPane the glassPane component of this {@code JLayer}
     *
     * @see #getGlassPane()
     */
    public void setGlassPane(JPanel glassPane) {
        Component oldGlassPane = getGlassPane();
        boolean isGlassPaneVisible = false;
        if (oldGlassPane != null) {
            isGlassPaneVisible = oldGlassPane.isVisible();
            super.remove(oldGlassPane);
        }
        if (glassPane != null) {
            glassPane.setMixingCutoutShape(new Rectangle());
            glassPane.setVisible(isGlassPaneVisible);
            super.addImpl(glassPane, null, 0);
        }
        this.glassPane = glassPane;
        firePropertyChange("glassPane", oldGlassPane, glassPane);
        revalidate();
        repaint();
    }

    /**
     * Called by the constructor methods to create a default {@code glassPane}.
     * By default this method creates a new JPanel with visibility set to true
     * and opacity set to false.
     *
     * @return the default {@code glassPane}
     */
    public JPanel createGlassPane() {
        return new DefaultLayerGlassPane();
    }

    /**
     * Sets the layout manager for this container.  This method is
     * overridden to prevent the layout manager from being set.
     * <p>Note:  If {@code mgr} is non-{@code null}, this
     * method will throw an exception as layout managers are not supported on
     * a {@code JLayer}.
     *
     * @param mgr the specified layout manager
     * @exception IllegalArgumentException this method is not supported
     */
    public void setLayout(LayoutManager mgr) {
        if (mgr != null) {
            throw new IllegalArgumentException("JLayer.setLayout() not supported");
        }
    }

    /**
     * Delegates its functionality to the {@code getView().setBorder(Border)} method,
     * if the view component is an instance of {@code javax.swing.JComponent},
     * otherwise this method is a no-op.
     *
     * @param border the border to be rendered for the {@code view} component
     * @see #getView()
     * @see javax.swing.JComponent#setBorder(Border)
     */
    public void setBorder(Border border) {
        if (view instanceof JComponent) {
            ((JComponent)view).setBorder(border);
        }
    }

    /**
     * Delegates its functionality to the {@code getView().getBorder()} method,
     * if the view component is an instance of {@code javax.swing.JComponent},
     * otherwise returns {@code null}.
     *
     * @return the border object for the {@code view} component
     * @see #getView()
     * @see #setBorder
     * @see javax.swing.JComponent#getBorder()
     */
    public Border getBorder() {
        if (view instanceof JComponent) {
            return ((JComponent) view).getBorder();
        }
        return null;
    }

    /**
     * This method is not supported by {@code JLayer}
     * and always throws {@code UnsupportedOperationException}
     *
     * @throws UnsupportedOperationException this method is not supported
     *
     * @see #setView(Component)
     * @see #setGlassPane(JPanel)
     */
    protected void addImpl(Component comp, Object constraints, int index) {
        throw new UnsupportedOperationException(
                "Adding components to JLayer is not supported, " +
                        "use setView() or setGlassPane() instead");
    }

    /**
     * {@inheritDoc}
     */
    public void remove(Component comp) {
        if (comp == null) {
            super.remove(comp);
        } else if (comp == getView()) {
            setView(null);
        } else if (comp == getGlassPane()) {
            setGlassPane(null);
        } else {
            super.remove(comp);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() {
        if (view != null) {
            setView(null);
        }
        if (glassPane != null) {
            setGlassPane(null);
        }
    }

    /**
     * Always returns {@code true} to cause painting to originate from {@code JLayer},
     * or one of its ancestors.
     *
     * @return true
     * @see JComponent#isPaintingOrigin()
     */
    protected boolean isPaintingOrigin() {
        return true;
    }

    /**
     * Delegates its functionality to the
     * {@link javax.swing.plaf.LayerUI#paintImmediately(int, int, int, int, JLayer)} method,
     * if {@code LayerUI} is set.
     *
     * @param x  the x value of the region to be painted
     * @param y  the y value of the region to be painted
     * @param w  the width of the region to be painted
     * @param h  the height of the region to be painted
     */
    public void paintImmediately(int x, int y, int w, int h) {
        if (!isPaintImmediatelyCalling && getUI() != null) {
            isPaintImmediatelyCalling = true;
            try {
                getUI().paintImmediately(x, y, w, h, this);
            } finally {
                isPaintImmediatelyCalling = false;
            }
        } else {
            super.paintImmediately(x, y, w, h);
        }
    }

    /**
     * Delegates its functionality to the
     * {@link javax.swing.plaf.LayerUI#imageUpdate(java.awt.Image, int, int, int, int, int, JLayer)} method,
     * if the {@code LayerUI} is set.
     *
     * @param     img   the image being observed
     * @param     infoflags   see {@code imageUpdate} for more information
     * @param     x   the <i>x</i> coordinate
     * @param     y   the <i>y</i> coordinate
     * @param     w   the width
     * @param     h   the height
     * @return    {@code false} if the infoflags indicate that the
     *            image is completely loaded; {@code true} otherwise.
     */
    public boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h) {
        if (!isImageUpdateCalling && getUI() != null) {
            isImageUpdateCalling = true;
            try {
                return getUI().imageUpdate(img, infoflags, x, y, w, h, this);
            } finally {
                isImageUpdateCalling = false;
            }
        } else {
            return super.imageUpdate(img, infoflags, x, y, w, h);
        }
    }

    /**
     * Delegates all painting to the {@link javax.swing.plaf.LayerUI} object.
     *
     * @param g the {@code Graphics} to render to
     */
    public void paint(Graphics g) {
        if (!isPaintCalling) {
            isPaintCalling = true;
            try {
                super.paintComponent(g);
            } finally {
                isPaintCalling = false;
            }
        } else {
            super.paint(g);
        }
    }

    /**
     * This method is empty, because all painting is done by
     * {@link #paint(Graphics)} and
     * {@link javax.swing.plaf.LayerUI#update(Graphics, JComponent)} methods
     */
    protected void paintComponent(Graphics g) {
    }

    /**
     * The {@code JLayer} overrides the default implementation of
     * this method (in {@code JComponent}) to return {@code false}.
     * This ensures
     * that the drawing machinery will call the {@code JLayer}'s
     * {@code paint}
     * implementation rather than messaging the {@code JLayer}'s
     * children directly.
     *
     * @return false
     */
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if (getUI() != null) {
            getUI().applyPropertyChange(evt, this);
        }
    }

    /**
     * Enables the events from JLayer and <b>all its descendants</b>
     * defined by the specified event mask parameter
     * to be delivered to the
     * {@link LayerUI#eventDispatched(AWTEvent, JLayer)} method.
     * <p>
     * Events are delivered provided that {@code LayerUI} is set
     * for this {@code JLayer} and the {@code JLayer}
     * is displayable.
     * <p>
     * The following example shows how to correctly use this method
     * in the {@code LayerUI} implementations:
     * <pre>
     *    public void installUI(JComponent c) {
     *       super.installUI(c);
     *       JLayer l = (JLayer) c;
     *       // this LayerUI will receive only key and focus events
     *       l.setLayerEventMask(AWTEvent.KEY_EVENT_MASK | AWTEvent.FOCUS_EVENT_MASK);
     *    }
     *
     *    public void uninstallUI(JComponent c) {
     *       super.uninstallUI(c);
     *       JLayer l = (JLayer) c;
     *       // JLayer must be returned to its initial state
     *       l.setLayerEventMask(0);
     *    }
     * </pre>
     *
     * By default {@code JLayer} receives no events and its event mask is {@code 0}.
     *
     * @param layerEventMask the bitmask of event types to receive
     *
     * @see #getLayerEventMask()
     * @see LayerUI#eventDispatched(AWTEvent, JLayer)
     * @see Component#isDisplayable()
     */
    public void setLayerEventMask(long layerEventMask) {
        long oldEventMask = getLayerEventMask();
        this.eventMask = layerEventMask;
        firePropertyChange("layerEventMask", oldEventMask, layerEventMask);
        if (layerEventMask != oldEventMask) {
            disableEvents(oldEventMask);
            enableEvents(eventMask);
            if (isDisplayable()) {
                eventController.updateAWTEventListener(
                        oldEventMask, layerEventMask);
            }
        }
    }

    /**
     * Returns the bitmap of event mask to receive by this {@code JLayer}
     * and its {@code LayerUI}.
     * <p>
     * It means that {@link javax.swing.plaf.LayerUI#eventDispatched(AWTEvent, JLayer)} method
     * will only receive events that match the event mask.
     * <p>
     * By default {@code JLayer} receives no events.
     *
     * @return the bitmask of event types to receive for this {@code JLayer}
     */
    public long getLayerEventMask() {
        return eventMask;
    }

    /**
     * Delegates its functionality to the {@link javax.swing.plaf.LayerUI#updateUI(JLayer)} method,
     * if {@code LayerUI} is set.
     */
    public void updateUI() {
        if (getUI() != null) {
            getUI().updateUI(this);
        }
    }

    /**
     * Returns the preferred size of the viewport for a view component.
     * <p>
     * If the view component of this layer implements {@link Scrollable}, this method delegates its
     * implementation to the view component.
     *
     * @return the preferred size of the viewport for a view component
     *
     * @see Scrollable
     */
    public Dimension getPreferredScrollableViewportSize() {
        if (getView() instanceof Scrollable) {
            return ((Scrollable)getView()).getPreferredScrollableViewportSize();
        }
        return getPreferredSize();
    }

    /**
     * Returns a scroll increment, which is required for components
     * that display logical rows or columns in order to completely expose
     * one block of rows or columns, depending on the value of orientation.
     * <p>
     * If the view component of this layer implements {@link Scrollable}, this method delegates its
     * implementation to the view component.
     *
     * @return the "block" increment for scrolling in the specified direction
     *
     * @see Scrollable
     */
    public int getScrollableBlockIncrement(Rectangle visibleRect,
                                           int orientation, int direction) {
        if (getView() instanceof Scrollable) {
            return ((Scrollable)getView()).getScrollableBlockIncrement(visibleRect,
                    orientation, direction);
        }
        return (orientation == SwingConstants.VERTICAL) ? visibleRect.height :
                visibleRect.width;
    }

    /**
     * Returns {@code false} to indicate that the height of the viewport does not
     * determine the height of the layer, unless the preferred height
     * of the layer is smaller than the height of the viewport.
     * <p>
     * If the view component of this layer implements {@link Scrollable}, this method delegates its
     * implementation to the view component.
     *
     * @return whether the layer should track the height of the viewport
     *
     * @see Scrollable
     */
    public boolean getScrollableTracksViewportHeight() {
        if (getView() instanceof Scrollable) {
            return ((Scrollable)getView()).getScrollableTracksViewportHeight();
        }
        return false;
    }

    /**
     * Returns {@code false} to indicate that the width of the viewport does not
     * determine the width of the layer, unless the preferred width
     * of the layer is smaller than the width of the viewport.
     * <p>
     * If the view component of this layer implements {@link Scrollable}, this method delegates its
     * implementation to the view component.
     *
     * @return whether the layer should track the width of the viewport
     *
     * @see Scrollable
     */
    public boolean getScrollableTracksViewportWidth() {
        if (getView() instanceof Scrollable) {
            return ((Scrollable)getView()).getScrollableTracksViewportWidth();
        }
        return false;
    }

    /**
     * Returns a scroll increment, which is required for components
     * that display logical rows or columns in order to completely expose
     * one new row or column, depending on the value of orientation.
     * Ideally, components should handle a partially exposed row or column
     * by returning the distance required to completely expose the item.
     * <p>
     * Scrolling containers, like {@code JScrollPane}, will use this method
     * each time the user requests a unit scroll.
     * <p>
     * If the view component of this layer implements {@link Scrollable}, this method delegates its
     * implementation to the view component.
     *
     * @return The "unit" increment for scrolling in the specified direction.
     *         This value should always be positive.
     *
     * @see Scrollable
     */
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation,
                                          int direction) {
        if (getView() instanceof Scrollable) {
            return ((Scrollable) getView()).getScrollableUnitIncrement(
                    visibleRect, orientation, direction);
        }
        return 1;
    }

    @Serial
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField f = s.readFields();

        view = (V) f.get("view", null);
        glassPane = (JPanel) f.get("glassPane", null);
        eventMask = f.get("eventMask", 0l);
        if (eventMask != 0) {
            eventController.updateAWTEventListener(0, eventMask);
        }
        LayerUI<V> newLayerUI = (LayerUI<V>) f.get("layerUI", null);
        if (newLayerUI != null) {
            setUI(newLayerUI);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addNotify() {
        super.addNotify();
        eventController.updateAWTEventListener(0, eventMask);
    }

    /**
     * {@inheritDoc}
     */
    public void removeNotify() {
        super.removeNotify();
        eventController.updateAWTEventListener(eventMask, 0);
    }

    /**
     * Delegates its functionality to the {@link javax.swing.plaf.LayerUI#doLayout(JLayer)} method,
     * if {@code LayerUI} is set.
     */
    public void doLayout() {
        if (getUI() != null) {
            getUI().doLayout(this);
        }
    }

    /**
     * Gets the AccessibleContext associated with this {@code JLayer}.
     *
     * @return the AccessibleContext associated with this {@code JLayer}.
     */
    @SuppressWarnings("serial") // anonymous class
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
                public AccessibleRole getAccessibleRole() {
                    return AccessibleRole.PANEL;
                }
            };
        }
        return accessibleContext;
    }

    /**
     * static AWTEventListener to be shared with all AbstractLayerUIs
     */
    private static class LayerEventController implements AWTEventListener {
        private ArrayList<Long> layerMaskList =
                new ArrayList<Long>();

        private long currentEventMask;

        private static final long ACCEPTED_EVENTS =
                AWTEvent.COMPONENT_EVENT_MASK |
                        AWTEvent.CONTAINER_EVENT_MASK |
                        AWTEvent.FOCUS_EVENT_MASK |
                        AWTEvent.KEY_EVENT_MASK |
                        AWTEvent.MOUSE_WHEEL_EVENT_MASK |
                        AWTEvent.MOUSE_MOTION_EVENT_MASK |
                        AWTEvent.MOUSE_EVENT_MASK |
                        AWTEvent.INPUT_METHOD_EVENT_MASK |
                        AWTEvent.HIERARCHY_EVENT_MASK |
                        AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK;

        @SuppressWarnings({"unchecked", "rawtypes"})
        public void eventDispatched(AWTEvent event) {
            Object source = event.getSource();
            if (source instanceof Component) {
                Component component = (Component) source;
                while (component != null) {
                    if (component instanceof JLayer) {
                        JLayer l = (JLayer) component;
                        LayerUI<?> ui = l.getUI();
                        if (ui != null &&
                                isEventEnabled(l.getLayerEventMask(), event.getID()) &&
                                (!(event instanceof InputEvent) || !((InputEvent)event).isConsumed())) {
                            ui.eventDispatched(event, l);
                        }
                    }
                    component = component.getParent();
                }
            }
        }

        private void updateAWTEventListener(long oldEventMask, long newEventMask) {
            if (oldEventMask != 0) {
                layerMaskList.remove(oldEventMask);
            }
            if (newEventMask != 0) {
                layerMaskList.add(newEventMask);
            }
            long combinedMask = 0;
            for (Long mask : layerMaskList) {
                combinedMask |= mask;
            }
            // filter out all unaccepted events
            combinedMask &= ACCEPTED_EVENTS;
            if (combinedMask == 0) {
                removeAWTEventListener();
            } else if (getCurrentEventMask() != combinedMask) {
                removeAWTEventListener();
                addAWTEventListener(combinedMask);
            }
            currentEventMask = combinedMask;
        }

        private long getCurrentEventMask() {
            return currentEventMask;
        }

        @SuppressWarnings("removal")
        private void addAWTEventListener(final long eventMask) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    Toolkit.getDefaultToolkit().
                            addAWTEventListener(LayerEventController.this, eventMask);
                    return null;
                }
            });

        }

        @SuppressWarnings("removal")
        private void removeAWTEventListener() {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    Toolkit.getDefaultToolkit().
                            removeAWTEventListener(LayerEventController.this);
                    return null;
                }
            });
        }

        private boolean isEventEnabled(long eventMask, int id) {
            return (((eventMask & AWTEvent.COMPONENT_EVENT_MASK) != 0 &&
                    id >= ComponentEvent.COMPONENT_FIRST &&
                    id <= ComponentEvent.COMPONENT_LAST)
                    || ((eventMask & AWTEvent.CONTAINER_EVENT_MASK) != 0 &&
                    id >= ContainerEvent.CONTAINER_FIRST &&
                    id <= ContainerEvent.CONTAINER_LAST)
                    || ((eventMask & AWTEvent.FOCUS_EVENT_MASK) != 0 &&
                    id >= FocusEvent.FOCUS_FIRST &&
                    id <= FocusEvent.FOCUS_LAST)
                    || ((eventMask & AWTEvent.KEY_EVENT_MASK) != 0 &&
                    id >= KeyEvent.KEY_FIRST &&
                    id <= KeyEvent.KEY_LAST)
                    || ((eventMask & AWTEvent.MOUSE_WHEEL_EVENT_MASK) != 0 &&
                    id == MouseEvent.MOUSE_WHEEL)
                    || ((eventMask & AWTEvent.MOUSE_MOTION_EVENT_MASK) != 0 &&
                    (id == MouseEvent.MOUSE_MOVED ||
                            id == MouseEvent.MOUSE_DRAGGED))
                    || ((eventMask & AWTEvent.MOUSE_EVENT_MASK) != 0 &&
                    id != MouseEvent.MOUSE_MOVED &&
                    id != MouseEvent.MOUSE_DRAGGED &&
                    id != MouseEvent.MOUSE_WHEEL &&
                    id >= MouseEvent.MOUSE_FIRST &&
                    id <= MouseEvent.MOUSE_LAST)
                    || ((eventMask & AWTEvent.INPUT_METHOD_EVENT_MASK) != 0 &&
                    id >= InputMethodEvent.INPUT_METHOD_FIRST &&
                    id <= InputMethodEvent.INPUT_METHOD_LAST)
                    || ((eventMask & AWTEvent.HIERARCHY_EVENT_MASK) != 0 &&
                    id == HierarchyEvent.HIERARCHY_CHANGED)
                    || ((eventMask & AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK) != 0 &&
                    (id == HierarchyEvent.ANCESTOR_MOVED ||
                            id == HierarchyEvent.ANCESTOR_RESIZED)));
        }
    }

    /**
     * The default glassPane for the {@link javax.swing.JLayer}.
     * It is a subclass of {@code JPanel} which is non opaque by default.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private static class DefaultLayerGlassPane extends JPanel {
        /**
         * Creates a new {@link DefaultLayerGlassPane}
         */
        public DefaultLayerGlassPane() {
            setOpaque(false);
        }

        /**
         * First, implementation of this method iterates through
         * glassPane's child components and returns {@code true}
         * if any of them is visible and contains passed x,y point.
         * After that it checks if no mouseListeners is attached to this component
         * and no mouse cursor is set, then it returns {@code false},
         * otherwise calls the super implementation of this method.
         *
         * @param x the <i>x</i> coordinate of the point
         * @param y the <i>y</i> coordinate of the point
         * @return true if this component logically contains x,y
         */
        public boolean contains(int x, int y) {
            for (int i = 0; i < getComponentCount(); i++) {
                Component c = getComponent(i);
                Point point = SwingUtilities.convertPoint(this, new Point(x, y), c);
                if(c.isVisible() && c.contains(point)){
                    return true;
                }
            }
            if (getMouseListeners().length == 0
                    && getMouseMotionListeners().length == 0
                    && getMouseWheelListeners().length == 0
                    && !isCursorSet()) {
                return false;
            }
            return super.contains(x, y);
        }
    }
}
