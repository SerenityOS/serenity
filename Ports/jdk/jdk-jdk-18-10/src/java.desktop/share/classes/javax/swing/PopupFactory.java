/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sun.awt.EmbeddedFrame;
import sun.awt.OSInfo;
import sun.swing.SwingAccessor;

import static javax.swing.ClientPropertyKey.PopupFactory_FORCE_HEAVYWEIGHT_POPUP;

/**
 * <code>PopupFactory</code>, as the name implies, is used to obtain
 * instances of <code>Popup</code>s. <code>Popup</code>s are used to
 * display a <code>Component</code> above all other <code>Component</code>s
 * in a particular containment hierarchy. The general contract is that
 * once you have obtained a <code>Popup</code> from a
 * <code>PopupFactory</code>, you must invoke <code>hide</code> on the
 * <code>Popup</code>. The typical usage is:
 * <pre>
 *   PopupFactory factory = PopupFactory.getSharedInstance();
 *   Popup popup = factory.getPopup(owner, contents, x, y);
 *   popup.show();
 *   ...
 *   popup.hide();
 * </pre>
 *
 * @see Popup
 *
 * @since 1.4
 */
public class PopupFactory {

    static {
        SwingAccessor.setPopupFactoryAccessor(new SwingAccessor.PopupFactoryAccessor() {
            @Override
            public Popup getHeavyWeightPopup(PopupFactory factory, Component owner,
                                             Component contents, int ownerX, int ownerY) {
                return factory.getPopup(owner, contents, ownerX, ownerY, HEAVY_WEIGHT_POPUP);
            }
        });
    }
    /**
     * The shared instanceof <code>PopupFactory</code> is per
     * <code>AppContext</code>. This is the key used in the
     * <code>AppContext</code> to locate the <code>PopupFactory</code>.
     */
    private static final Object SharedInstanceKey =
        new StringBuffer("PopupFactory.SharedInstanceKey");

    /**
     * Max number of items to store in any one particular cache.
     */
    private static final int MAX_CACHE_SIZE = 5;

    /**
     * Key used to indicate a light weight popup should be used.
     */
    static final int LIGHT_WEIGHT_POPUP   = 0;

    /**
     * Key used to indicate a medium weight Popup should be used.
     */
    static final int MEDIUM_WEIGHT_POPUP  = 1;

    /*
     * Key used to indicate a heavy weight Popup should be used.
     */
    static final int HEAVY_WEIGHT_POPUP   = 2;

    /**
     * Default type of Popup to create.
     */
    private int popupType = LIGHT_WEIGHT_POPUP;

    /**
     * Constructs a {@code PopupFactory}.
     */
    public PopupFactory() {}

    /**
     * Sets the <code>PopupFactory</code> that will be used to obtain
     * <code>Popup</code>s.
     * This will throw an <code>IllegalArgumentException</code> if
     * <code>factory</code> is null.
     *
     * @param factory Shared PopupFactory
     * @exception IllegalArgumentException if <code>factory</code> is null
     * @see #getPopup
     */
    public static void setSharedInstance(PopupFactory factory) {
        if (factory == null) {
            throw new IllegalArgumentException("PopupFactory can not be null");
        }
        SwingUtilities.appContextPut(SharedInstanceKey, factory);
    }

    /**
     * Returns the shared <code>PopupFactory</code> which can be used
     * to obtain <code>Popup</code>s.
     *
     * @return Shared PopupFactory
     */
    public static PopupFactory getSharedInstance() {
        PopupFactory factory = (PopupFactory)SwingUtilities.appContextGet(
                         SharedInstanceKey);

        if (factory == null) {
            factory = new PopupFactory();
            setSharedInstance(factory);
        }
        return factory;
    }


    /**
     * Provides a hint as to the type of <code>Popup</code> that should
     * be created.
     */
    void setPopupType(int type) {
        popupType = type;
    }

    /**
     * Returns the preferred type of Popup to create.
     */
    int getPopupType() {
        return popupType;
    }

    /**
     * Creates a <code>Popup</code> for the Component <code>owner</code>
     * containing the Component <code>contents</code>. <code>owner</code>
     * is used to determine which <code>Window</code> the new
     * <code>Popup</code> will parent the <code>Component</code> the
     * <code>Popup</code> creates to. A null <code>owner</code> implies there
     * is no valid parent. <code>x</code> and
     * <code>y</code> specify the preferred initial location to place
     * the <code>Popup</code> at. Based on screen size, or other paramaters,
     * the <code>Popup</code> may not display at <code>x</code> and
     * <code>y</code>.
     *
     * @param owner    Component mouse coordinates are relative to, may be null
     * @param contents Contents of the Popup
     * @param x        Initial x screen coordinate
     * @param y        Initial y screen coordinate
     * @exception IllegalArgumentException if contents is null
     * @return Popup containing Contents
     */
    public Popup getPopup(Component owner, Component contents,
            int x, int y) throws IllegalArgumentException {
        return getPopup(owner, contents, x, y, false);
    }

    /**
     * Creates a {@code Popup} for the Component {@code owner}
     * containing the Component {@code contents}.
     * The window containing the component {@code owner}
     * will be used as the parent window. A null {@code owner} implies there
     * is no valid parent. {@code x} and {@code y} specify the preferred
     * initial location to place the {@code Popup} at. Based on screen size,
     * or other parameters, the {@code Popup} may not display at {@code x} and
     * {@code y}. {@code isHeavyWeightPopup} specifies if the {@code Popup}
     * will be heavyweight. Passing {@code true} will force the {@code Popup}
     * type to be heavyweight, otherwise {@code Popup} type will be selected by
     * {@code Popup} factory. Lightweight {@code Popup} windows are more
     * efficient than heavyweight (native peer) windows, but lightweight
     * and heavyweight components do not mix well in a GUI.
     * This method is intended to be used only by PopupFactory sub-classes.
     * @param owner Component mouse coordinates are relative to, may be null
     * @param contents Contents of the Popup
     * @param x Initial x screen coordinate
     * @param y Initial y screen coordinate
     * @param isHeavyWeightPopup true if Popup should be heavy weight,
     * otherwise popup type will be selected by popup factory.
     * @throws IllegalArgumentException if contents is null
     * @return Popup containing Contents
     */
    protected Popup getPopup(Component owner, Component contents, int x, int y,
            boolean isHeavyWeightPopup) throws IllegalArgumentException {
        if (contents == null) {
            throw new IllegalArgumentException(
                    "Popup.getPopup must be passed non-null contents");
        }
        if (isHeavyWeightPopup) {
            return getPopup(owner, contents, x, y, HEAVY_WEIGHT_POPUP);
        }
        int popupType = getPopupType(owner, contents, x, y);
        Popup popup = getPopup(owner, contents, x, y, popupType);

        if (popup == null) {
            // Didn't fit, force to heavy.
            popup = getPopup(owner, contents, x, y, HEAVY_WEIGHT_POPUP);
        }
        return popup;
    }

    /**
     * Returns the popup type to use for the specified parameters.
     */
    private int getPopupType(Component owner, Component contents,
                             int ownerX, int ownerY) {
        int popupType = getPopupType();

        if (owner == null || invokerInHeavyWeightPopup(owner)) {
            popupType = HEAVY_WEIGHT_POPUP;
        }
        else if (popupType == LIGHT_WEIGHT_POPUP &&
                 !(contents instanceof JToolTip) &&
                 !(contents instanceof JPopupMenu)) {
            popupType = MEDIUM_WEIGHT_POPUP;
        }

        // Check if the parent component is an option pane.  If so we need to
        // force a heavy weight popup in order to have event dispatching work
        // correctly.
        Component c = owner;
        while (c != null) {
            if (c instanceof JComponent) {
                if (((JComponent)c).getClientProperty(
                            PopupFactory_FORCE_HEAVYWEIGHT_POPUP) == Boolean.TRUE) {
                    popupType = HEAVY_WEIGHT_POPUP;
                    break;
                }
            }
            c = c.getParent();
        }

        return popupType;
    }

    /**
     * Obtains the appropriate <code>Popup</code> based on
     * <code>popupType</code>.
     */
    @SuppressWarnings("removal")
    private Popup getPopup(Component owner, Component contents,
                           int ownerX, int ownerY, int popupType) {
        if (GraphicsEnvironment.isHeadless()) {
            return getHeadlessPopup(owner, contents, ownerX, ownerY);
        }

        switch(popupType) {
        case LIGHT_WEIGHT_POPUP:
            return getLightWeightPopup(owner, contents, ownerX, ownerY);
        case MEDIUM_WEIGHT_POPUP:
            return getMediumWeightPopup(owner, contents, ownerX, ownerY);
        case HEAVY_WEIGHT_POPUP:
            Popup popup = getHeavyWeightPopup(owner, contents, ownerX, ownerY);
            if ((AccessController.doPrivileged(OSInfo.getOSTypeAction()) ==
                OSInfo.OSType.MACOSX) && (owner != null) &&
                (EmbeddedFrame.getAppletIfAncestorOf(owner) != null)) {
                ((HeavyWeightPopup)popup).setCacheEnabled(false);
            }
            return popup;
        }
        return null;
    }

    /**
     * Creates a headless popup
     */
    private Popup getHeadlessPopup(Component owner, Component contents,
                                   int ownerX, int ownerY) {
        return HeadlessPopup.getHeadlessPopup(owner, contents, ownerX, ownerY);
    }

    /**
     * Creates a light weight popup.
     */
    private Popup getLightWeightPopup(Component owner, Component contents,
                                         int ownerX, int ownerY) {
        return LightWeightPopup.getLightWeightPopup(owner, contents, ownerX,
                                                    ownerY);
    }

    /**
     * Creates a medium weight popup.
     */
    private Popup getMediumWeightPopup(Component owner, Component contents,
                                          int ownerX, int ownerY) {
        return MediumWeightPopup.getMediumWeightPopup(owner, contents,
                                                      ownerX, ownerY);
    }

    /**
     * Creates a heavy weight popup.
     */
    private Popup getHeavyWeightPopup(Component owner, Component contents,
                                         int ownerX, int ownerY) {
        if (GraphicsEnvironment.isHeadless()) {
            return getMediumWeightPopup(owner, contents, ownerX, ownerY);
        }
        return HeavyWeightPopup.getHeavyWeightPopup(owner, contents, ownerX,
                                                    ownerY);
    }

    /**
     * Returns true if the Component <code>i</code> inside a heavy weight
     * <code>Popup</code>.
     */
    private boolean invokerInHeavyWeightPopup(Component i) {
        if (i != null) {
            Container parent;
            for(parent = i.getParent() ; parent != null ; parent =
                    parent.getParent()) {
                if (parent instanceof Popup.HeavyWeightWindow) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Popup implementation that uses a Window as the popup.
     */
    private static class HeavyWeightPopup extends Popup {
        private static final Object heavyWeightPopupCacheKey =
                 new StringBuffer("PopupFactory.heavyWeightPopupCache");

        private volatile boolean isCacheEnabled = true;

        /**
         * Returns either a new or recycled <code>Popup</code> containing
         * the specified children.
         */
        static Popup getHeavyWeightPopup(Component owner, Component contents,
                                         int ownerX, int ownerY) {
            Window window = (owner != null) ? SwingUtilities.
                              getWindowAncestor(owner) : null;
            HeavyWeightPopup popup = null;

            if (window != null) {
                popup = getRecycledHeavyWeightPopup(window);
            }

            boolean focusPopup = false;
            if(contents != null && contents.isFocusable()) {
                if(contents instanceof JPopupMenu) {
                    JPopupMenu jpm = (JPopupMenu) contents;
                    Component[] popComps = jpm.getComponents();
                    for (Component popComp : popComps) {
                        if (!(popComp instanceof MenuElement) &&
                                !(popComp instanceof JSeparator)) {
                            focusPopup = true;
                            break;
                        }
                    }
                }
            }

            if (popup == null ||
                ((JWindow) popup.getComponent())
                 .getFocusableWindowState() != focusPopup) {

                if(popup != null) {
                    // The recycled popup can't serve us well
                    // dispose it and create new one
                    popup._dispose();
                }

                popup = new HeavyWeightPopup();
            }

            popup.reset(owner, contents, ownerX, ownerY);

            if(focusPopup) {
                JWindow wnd = (JWindow) popup.getComponent();
                wnd.setFocusableWindowState(true);
                // Set window name. We need this in BasicPopupMenuUI
                // to identify focusable popup window.
                wnd.setName("###focusableSwingPopup###");
            }

            return popup;
        }

        /**
         * Returns a previously disposed heavy weight <code>Popup</code>
         * associated with <code>window</code>. This will return null if
         * there is no <code>HeavyWeightPopup</code> associated with
         * <code>window</code>.
         */
        private static HeavyWeightPopup getRecycledHeavyWeightPopup(Window w) {
            synchronized (HeavyWeightPopup.class) {
                List<HeavyWeightPopup> cache;
                Map<Window, List<HeavyWeightPopup>> heavyPopupCache = getHeavyWeightPopupCache();

                if (heavyPopupCache.containsKey(w)) {
                    cache = heavyPopupCache.get(w);
                } else {
                    return null;
                }
                if (cache.size() > 0) {
                    HeavyWeightPopup r = cache.get(0);
                    cache.remove(0);
                    return r;
                }
                return null;
            }
        }

        /**
         * Returns the cache to use for heavy weight popups. Maps from
         * <code>Window</code> to a <code>List</code> of
         * <code>HeavyWeightPopup</code>s.
         */
        @SuppressWarnings("unchecked")
        private static Map<Window, List<HeavyWeightPopup>> getHeavyWeightPopupCache() {
            synchronized (HeavyWeightPopup.class) {
                Map<Window, List<HeavyWeightPopup>> cache = (Map<Window, List<HeavyWeightPopup>>)SwingUtilities.appContextGet(
                                  heavyWeightPopupCacheKey);

                if (cache == null) {
                    cache = new HashMap<>(2);
                    SwingUtilities.appContextPut(heavyWeightPopupCacheKey,
                                                 cache);
                }
                return cache;
            }
        }

        /**
         * Recycles the passed in <code>HeavyWeightPopup</code>.
         */
        private static void recycleHeavyWeightPopup(HeavyWeightPopup popup) {
            synchronized (HeavyWeightPopup.class) {
                List<HeavyWeightPopup> cache;
                Window window = SwingUtilities.getWindowAncestor(
                                     popup.getComponent());
                Map<Window, List<HeavyWeightPopup>> heavyPopupCache = getHeavyWeightPopupCache();

                if (window instanceof Popup.DefaultFrame ||
                                      !window.isVisible()) {
                    // If the Window isn't visible, we don't cache it as we
                    // likely won't ever get a windowClosed event to clean up.
                    // We also don't cache DefaultFrames as this indicates
                    // there wasn't a valid Window parent, and thus we don't
                    // know when to clean up.
                    popup._dispose();
                    return;
                } else if (heavyPopupCache.containsKey(window)) {
                    cache = heavyPopupCache.get(window);
                } else {
                    cache = new ArrayList<HeavyWeightPopup>();
                    heavyPopupCache.put(window, cache);
                    // Clean up if the Window is closed
                    final Window w = window;

                    w.addWindowListener(new WindowAdapter() {
                        public void windowClosed(WindowEvent e) {
                            List<HeavyWeightPopup> popups;

                            synchronized(HeavyWeightPopup.class) {
                                Map<Window, List<HeavyWeightPopup>> heavyPopupCache2 =
                                              getHeavyWeightPopupCache();

                                popups = heavyPopupCache2.remove(w);
                            }
                            if (popups != null) {
                                for (int counter = popups.size() - 1;
                                                   counter >= 0; counter--) {
                                    popups.get(counter)._dispose();
                                }
                            }
                        }
                    });
                }

                if(cache.size() < MAX_CACHE_SIZE) {
                    cache.add(popup);
                } else {
                    popup._dispose();
                }
            }
        }

        /**
         * Enables or disables cache for current object.
         */
        void setCacheEnabled(boolean enable) {
            isCacheEnabled = enable;
        }

        //
        // Popup methods
        //
        public void hide() {
            super.hide();
            if (isCacheEnabled) {
                recycleHeavyWeightPopup(this);
            } else {
                this._dispose();
            }
        }

        /**
         * As we recycle the <code>Window</code>, we don't want to dispose it,
         * thus this method does nothing, instead use <code>_dipose</code>
         * which will handle the disposing.
         */
        void dispose() {
        }

        void _dispose() {
            super.dispose();
        }
    }



    /**
     * ContainerPopup consolidates the common code used in the light/medium
     * weight implementations of <code>Popup</code>.
     */
    private static class ContainerPopup extends Popup {
        /** Component we are to be added to. */
        Component owner;
        /** Desired x location. */
        int x;
        /** Desired y location. */
        int y;

        public void hide() {
            Component component = getComponent();

            if (component != null) {
                Container parent = component.getParent();

                if (parent != null) {
                    Rectangle bounds = component.getBounds();

                    parent.remove(component);
                    parent.repaint(bounds.x, bounds.y, bounds.width,
                                   bounds.height);
                }
            }
            owner = null;
        }
        public void pack() {
            Component component = getComponent();

            if (component != null) {
                component.setSize(component.getPreferredSize());
            }
        }

        void reset(Component owner, Component contents, int ownerX,
                   int ownerY) {
            if ((owner instanceof JFrame) || (owner instanceof JDialog) ||
                                                 (owner instanceof JWindow)) {
                // Force the content to be added to the layered pane, otherwise
                // we'll get an exception when adding to the RootPaneContainer.
                owner = ((RootPaneContainer)owner).getLayeredPane();
            }
            super.reset(owner, contents, ownerX, ownerY);

            x = ownerX;
            y = ownerY;
            this.owner = owner;
        }

        boolean overlappedByOwnedWindow() {
            Component component = getComponent();
            if(owner != null && component != null) {
                Window w = SwingUtilities.getWindowAncestor(owner);
                if (w == null) {
                    return false;
                }
                Window[] ownedWindows = w.getOwnedWindows();
                if(ownedWindows != null) {
                    Rectangle bnd = component.getBounds();
                    for (Window window : ownedWindows) {
                        if (window.isVisible() &&
                                bnd.intersects(window.getBounds())) {

                            return true;
                        }
                    }
                }
            }
            return false;
        }

        /**
         * Returns true if popup can fit the screen and the owner's top parent.
         * It determines can popup be lightweight or mediumweight.
         */
        @SuppressWarnings("removal")
        boolean fitsOnScreen() {
            boolean result = false;
            Component component = getComponent();
            if (owner != null && component != null) {
                int popupWidth = component.getWidth();
                int popupHeight = component.getHeight();

                Container parent = (Container) SwingUtilities.getRoot(owner);
                if (parent instanceof JFrame ||
                    parent instanceof JDialog ||
                    parent instanceof JWindow) {

                    Rectangle parentBounds = parent.getBounds();
                    Insets i = parent.getInsets();
                    parentBounds.x += i.left;
                    parentBounds.y += i.top;
                    parentBounds.width -= i.left + i.right;
                    parentBounds.height -= i.top + i.bottom;

                    if (JPopupMenu.canPopupOverlapTaskBar()) {
                        GraphicsConfiguration gc =
                                parent.getGraphicsConfiguration();
                        Rectangle popupArea = getContainerPopupArea(gc);
                        result = parentBounds.intersection(popupArea)
                                .contains(x, y, popupWidth, popupHeight);
                    } else {
                        result = parentBounds
                                .contains(x, y, popupWidth, popupHeight);
                    }
                } else if (parent instanceof JApplet) {
                    Rectangle parentBounds = parent.getBounds();
                    Point p = parent.getLocationOnScreen();
                    parentBounds.x = p.x;
                    parentBounds.y = p.y;
                    result = parentBounds.contains(x, y, popupWidth, popupHeight);
                }
            }
            return result;
        }

        Rectangle getContainerPopupArea(GraphicsConfiguration gc) {
            Rectangle screenBounds;
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Insets insets;
            if(gc != null) {
                // If we have GraphicsConfiguration use it
                // to get screen bounds
                screenBounds = gc.getBounds();
                insets = toolkit.getScreenInsets(gc);
            } else {
                // If we don't have GraphicsConfiguration use primary screen
                screenBounds = new Rectangle(toolkit.getScreenSize());
                insets = new Insets(0, 0, 0, 0);
            }
            // Take insets into account
            screenBounds.x += insets.left;
            screenBounds.y += insets.top;
            screenBounds.width -= (insets.left + insets.right);
            screenBounds.height -= (insets.top + insets.bottom);
            return screenBounds;
        }
    }


    /**
     * Popup implementation that is used in headless environment.
     */
    private static class HeadlessPopup extends ContainerPopup {
        static Popup getHeadlessPopup(Component owner, Component contents,
                                      int ownerX, int ownerY) {
            HeadlessPopup popup = new HeadlessPopup();
            popup.reset(owner, contents, ownerX, ownerY);
            return popup;
        }

        Component createComponent(Component owner) {
            return new Panel(new BorderLayout());
        }

        public void show() {
        }
        public void hide() {
        }
    }


    /**
     * Popup implementation that uses a JPanel as the popup.
     */
    private static class LightWeightPopup extends ContainerPopup {
        private static final Object lightWeightPopupCacheKey =
                         new StringBuffer("PopupFactory.lightPopupCache");

        /**
         * Returns a light weight <code>Popup</code> implementation. If
         * the <code>Popup</code> needs more space that in available in
         * <code>owner</code>, this will return null.
         */
        static Popup getLightWeightPopup(Component owner, Component contents,
                                         int ownerX, int ownerY) {
            LightWeightPopup popup = getRecycledLightWeightPopup();

            if (popup == null) {
                popup = new LightWeightPopup();
            }
            popup.reset(owner, contents, ownerX, ownerY);
            if (!popup.fitsOnScreen() ||
                 popup.overlappedByOwnedWindow()) {
                popup.hide();
                return null;
            }
            return popup;
        }

        /**
         * Returns the cache to use for heavy weight popups.
         */
        @SuppressWarnings("unchecked")
        private static List<LightWeightPopup> getLightWeightPopupCache() {
            List<LightWeightPopup> cache = (List<LightWeightPopup>)SwingUtilities.appContextGet(
                                   lightWeightPopupCacheKey);
            if (cache == null) {
                cache = new ArrayList<>();
                SwingUtilities.appContextPut(lightWeightPopupCacheKey, cache);
            }
            return cache;
        }

        /**
         * Recycles the LightWeightPopup <code>popup</code>.
         */
        private static void recycleLightWeightPopup(LightWeightPopup popup) {
            synchronized (LightWeightPopup.class) {
                List<LightWeightPopup> lightPopupCache = getLightWeightPopupCache();
                if (lightPopupCache.size() < MAX_CACHE_SIZE) {
                    lightPopupCache.add(popup);
                }
            }
        }

        /**
         * Returns a previously used <code>LightWeightPopup</code>, or null
         * if none of the popups have been recycled.
         */
        private static LightWeightPopup getRecycledLightWeightPopup() {
            synchronized (LightWeightPopup.class) {
                List<LightWeightPopup> lightPopupCache = getLightWeightPopupCache();
                if (lightPopupCache.size() > 0) {
                    LightWeightPopup r = lightPopupCache.get(0);
                    lightPopupCache.remove(0);
                    return r;
                }
                return null;
            }
        }



        //
        // Popup methods
        //
        public void hide() {
            super.hide();

            Container component = (Container)getComponent();

            component.removeAll();
            recycleLightWeightPopup(this);
        }

        @SuppressWarnings("removal")
        public void show() {
            Container parent = null;

            if (owner != null) {
                parent = (owner instanceof Container? (Container)owner : owner.getParent());
            }

            // Try to find a JLayeredPane and Window to add
            for (Container p = parent; p != null; p = p.getParent()) {
                if (p instanceof JRootPane) {
                    if (p.getParent() instanceof JInternalFrame) {
                        continue;
                    }
                    parent = ((JRootPane)p).getLayeredPane();
                    // Continue, so that if there is a higher JRootPane, we'll
                    // pick it up.
                } else if(p instanceof Window) {
                    if (parent == null) {
                        parent = p;
                    }
                    break;
                } else if (p instanceof JApplet) {
                    // Painting code stops at Applets, we don't want
                    // to add to a Component above an Applet otherwise
                    // you'll never see it painted.
                    break;
                }
            }

            Point p = SwingUtilities.convertScreenLocationToParent(parent, x,
                                                                   y);
            Component component = getComponent();

            component.setLocation(p.x, p.y);
            if (parent instanceof JLayeredPane) {
                parent.add(component, JLayeredPane.POPUP_LAYER, 0);
            } else {
                parent.add(component);
            }
            pack();
            component.setVisible(true);
        }

        Component createComponent(Component owner) {
            return new JPanel(new BorderLayout(), true);
        }

        //
        // Local methods
        //

        /**
         * Resets the <code>Popup</code> to an initial state.
         */
        void reset(Component owner, Component contents, int ownerX,
                   int ownerY) {
            super.reset(owner, contents, ownerX, ownerY);

            JComponent component = (JComponent)getComponent();
            component.setVisible(false);
            component.setLocation(ownerX, ownerY);
            component.setOpaque(contents.isOpaque());
            component.add(contents, BorderLayout.CENTER);
            pack();
        }
    }


    /**
     * Popup implementation that uses a Panel as the popup.
     */
    private static class MediumWeightPopup extends ContainerPopup {
        private static final Object mediumWeightPopupCacheKey =
                             new StringBuffer("PopupFactory.mediumPopupCache");

        /** Child of the panel. The contents are added to this. */
        private JRootPane rootPane;


        /**
         * Returns a medium weight <code>Popup</code> implementation. If
         * the <code>Popup</code> needs more space that in available in
         * <code>owner</code>, this will return null.
         */
        static Popup getMediumWeightPopup(Component owner, Component contents,
                                          int ownerX, int ownerY) {
            MediumWeightPopup popup = getRecycledMediumWeightPopup();

            if (popup == null) {
                popup = new MediumWeightPopup();
            }
            popup.reset(owner, contents, ownerX, ownerY);
            if (!popup.fitsOnScreen() ||
                 popup.overlappedByOwnedWindow()) {
                popup.hide();
                return null;
            }
            return popup;
        }

        /**
         * Returns the cache to use for medium weight popups.
         */
        @SuppressWarnings("unchecked")
        private static List<MediumWeightPopup> getMediumWeightPopupCache() {
            List<MediumWeightPopup> cache = (List<MediumWeightPopup>)SwingUtilities.appContextGet(
                                    mediumWeightPopupCacheKey);

            if (cache == null) {
                cache = new ArrayList<>();
                SwingUtilities.appContextPut(mediumWeightPopupCacheKey, cache);
            }
            return cache;
        }

        /**
         * Recycles the MediumWeightPopup <code>popup</code>.
         */
        private static void recycleMediumWeightPopup(MediumWeightPopup popup) {
            synchronized (MediumWeightPopup.class) {
                List<MediumWeightPopup> mediumPopupCache = getMediumWeightPopupCache();
                if (mediumPopupCache.size() < MAX_CACHE_SIZE) {
                    mediumPopupCache.add(popup);
                }
            }
        }

        /**
         * Returns a previously used <code>MediumWeightPopup</code>, or null
         * if none of the popups have been recycled.
         */
        private static MediumWeightPopup getRecycledMediumWeightPopup() {
            synchronized (MediumWeightPopup.class) {
                List<MediumWeightPopup> mediumPopupCache = getMediumWeightPopupCache();
                if (mediumPopupCache.size() > 0) {
                    MediumWeightPopup r = mediumPopupCache.get(0);
                    mediumPopupCache.remove(0);
                    return r;
                }
                return null;
            }
        }


        //
        // Popup
        //

        public void hide() {
            super.hide();
            rootPane.getContentPane().removeAll();
            recycleMediumWeightPopup(this);
        }

        @SuppressWarnings("removal")
        public void show() {
            Component component = getComponent();
            Container parent = null;

            if (owner != null) {
                parent = owner.getParent();
            }
            /*
              Find the top level window,
              if it has a layered pane,
              add to that, otherwise
              add to the window. */
            while (!(parent instanceof Window || parent instanceof Applet) &&
                   (parent!=null)) {
                parent = parent.getParent();
            }

            if (parent instanceof RootPaneContainer) {
                parent = ((RootPaneContainer) parent).getLayeredPane();
            }

            Point p = SwingUtilities.convertScreenLocationToParent(parent,
                                                                   x, y);
            component.setLocation(p.x, p.y);
            if (parent instanceof JLayeredPane) {
                parent.add(component, JLayeredPane.POPUP_LAYER, 0);
            } else {
                parent.add(component);
            }
            pack();
            component.setVisible(true);
            component.revalidate();
        }

        Component createComponent(Component owner) {
            Panel component = new MediumWeightComponent();

            rootPane = new JRootPane();
            // NOTE: this uses setOpaque vs LookAndFeel.installProperty as
            // there is NO reason for the RootPane not to be opaque. For
            // painting to work the contentPane must be opaque, therefor the
            // RootPane can also be opaque.
            rootPane.setOpaque(true);
            component.add(rootPane, BorderLayout.CENTER);
            return component;
        }

        /**
         * Resets the <code>Popup</code> to an initial state.
         */
        void reset(Component owner, Component contents, int ownerX,
                   int ownerY) {
            super.reset(owner, contents, ownerX, ownerY);

            Component component = getComponent();
            component.setVisible(false);
            component.setLocation(ownerX, ownerY);
            rootPane.getContentPane().add(contents, BorderLayout.CENTER);
            pack();
        }


        // This implements SwingHeavyWeight so that repaints on it
        // are processed by the RepaintManager and SwingPaintEventDispatcher.
        @SuppressWarnings("serial") // JDK-implementation class
        private static class MediumWeightComponent extends Panel implements
                                                           SwingHeavyWeight {
            MediumWeightComponent() {
                super(new BorderLayout());
            }
        }
    }
}

