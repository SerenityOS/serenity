/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.plaf.basic;

import sun.swing.SwingUtilities2;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.text.View;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.Vector;
import java.util.Hashtable;

import sun.swing.DefaultLookup;
import sun.swing.UIAction;

/**
 * A Basic L&amp;F implementation of TabbedPaneUI.
 *
 * @author Amy Fowler
 * @author Philip Milne
 * @author Steve Wilson
 * @author Tom Santos
 * @author Dave Moore
 */
public class BasicTabbedPaneUI extends TabbedPaneUI implements SwingConstants {


// Instance variables initialized at installation

    /** The tab pane */
    protected JTabbedPane tabPane;

    /** Highlight color */
    protected Color highlight;
    /** Light highlight color */
    protected Color lightHighlight;
    /** Shadow color */
    protected Color shadow;
    /** Dark shadow color */
    protected Color darkShadow;
    /** Focus color */
    protected Color focus;
    private   Color selectedColor;

    /** Text icon gap */
    protected int textIconGap;
    /** Tab run overlay */
    protected int tabRunOverlay;

    /** Tab insets */
    protected Insets tabInsets;
    /** Selected tab insets */
    protected Insets selectedTabPadInsets;
    /** Tab area insets */
    protected Insets tabAreaInsets;
    /** Content border insets */
    protected Insets contentBorderInsets;
    private boolean tabsOverlapBorder;
    private boolean tabsOpaque = true;
    private boolean contentOpaque = true;

    /**
     * As of Java 2 platform v1.3 this previously undocumented field is no
     * longer used.
     * Key bindings are now defined by the LookAndFeel, please refer to
     * the key bindings specification for further details.
     *
     * @deprecated As of Java 2 platform v1.3.
     */
    @Deprecated
    protected KeyStroke upKey;
    /**
     * As of Java 2 platform v1.3 this previously undocumented field is no
     * longer used.
     * Key bindings are now defined by the LookAndFeel, please refer to
     * the key bindings specification for further details.
     *
     * @deprecated As of Java 2 platform v1.3.
     */
    @Deprecated
    protected KeyStroke downKey;
    /**
     * As of Java 2 platform v1.3 this previously undocumented field is no
     * longer used.
     * Key bindings are now defined by the LookAndFeel, please refer to
     * the key bindings specification for further details.
     *
     * @deprecated As of Java 2 platform v1.3.
     */
    @Deprecated
    protected KeyStroke leftKey;
    /**
     * As of Java 2 platform v1.3 this previously undocumented field is no
     * longer used.
     * Key bindings are now defined by the LookAndFeel, please refer to
     * the key bindings specification for further details.
     *
     * @deprecated As of Java 2 platform v1.3.
     */
    @Deprecated
    protected KeyStroke rightKey;


// Transient variables (recalculated each time TabbedPane is layed out)
    /** Tab runs */
    protected int[] tabRuns = new int[10];
    /** Run count */
    protected int runCount = 0;
    /** Selected run */
    protected int selectedRun = -1;
    /** Tab rects */
    protected Rectangle[] rects = new Rectangle[0];
    /** Maximum tab height */
    protected int maxTabHeight;
    /** Maximum tab width */
    protected int maxTabWidth;

// Listeners

    /** Tab change listener */
    protected ChangeListener tabChangeListener;
    /** Property change listener */
    protected PropertyChangeListener propertyChangeListener;
    /** Mouse change listener */
    protected MouseListener mouseListener;
    /** Focus change listener */
    protected FocusListener focusListener;

// Private instance data

    private Insets currentPadInsets = new Insets(0,0,0,0);
    private Insets currentTabAreaInsets = new Insets(0,0,0,0);

    private Component visibleComponent;
    // PENDING(api): See comment for ContainerHandler
    private Vector<View> htmlViews;

    private Hashtable<Integer, Integer> mnemonicToIndexMap;

    /**
     * InputMap used for mnemonics. Only non-null if the JTabbedPane has
     * mnemonics associated with it. Lazily created in initMnemonics.
     */
    private InputMap mnemonicInputMap;

    // For use when tabLayoutPolicy = SCROLL_TAB_LAYOUT
    private ScrollableTabSupport tabScroller;

    private TabContainer tabContainer;

    /**
     * A rectangle used for general layout calculations in order
     * to avoid constructing many new Rectangles on the fly.
     */
    protected transient Rectangle calcRect = new Rectangle(0,0,0,0);

    /**
     * Tab that has focus.
     */
    private int focusIndex;

    /**
     * Combined listeners.
     */
    private Handler handler;

    /**
     * Index of the tab the mouse is over.
     */
    private int rolloverTabIndex;

    /**
     * This is set to true when a component is added/removed from the tab
     * pane and set to false when layout happens.  If true it indicates that
     * tabRuns is not valid and shouldn't be used.
     */
    private boolean isRunsDirty;

    private boolean calculatedBaseline;
    private int baseline;

// UI creation

    /**
     * Constructs a {@code BasicTabbedPaneUI}.
     */
    public BasicTabbedPaneUI() {}

    /**
     * Create a UI.
     * @param c a component
     * @return a UI
     */
    public static ComponentUI createUI(JComponent c) {
        return new BasicTabbedPaneUI();
    }

    static void loadActionMap(LazyActionMap map) {
        map.put(new Actions(Actions.NEXT));
        map.put(new Actions(Actions.PREVIOUS));
        map.put(new Actions(Actions.RIGHT));
        map.put(new Actions(Actions.LEFT));
        map.put(new Actions(Actions.UP));
        map.put(new Actions(Actions.DOWN));
        map.put(new Actions(Actions.PAGE_UP));
        map.put(new Actions(Actions.PAGE_DOWN));
        map.put(new Actions(Actions.REQUEST_FOCUS));
        map.put(new Actions(Actions.REQUEST_FOCUS_FOR_VISIBLE));
        map.put(new Actions(Actions.SET_SELECTED));
        map.put(new Actions(Actions.SELECT_FOCUSED));
        map.put(new Actions(Actions.SCROLL_FORWARD));
        map.put(new Actions(Actions.SCROLL_BACKWARD));
    }

// UI Installation/De-installation

    public void installUI(JComponent c) {
        this.tabPane = (JTabbedPane)c;

        calculatedBaseline = false;
        rolloverTabIndex = -1;
        focusIndex = -1;
        c.setLayout(createLayoutManager());
        installComponents();
        installDefaults();
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(JComponent c) {
        uninstallKeyboardActions();
        uninstallListeners();
        uninstallDefaults();
        uninstallComponents();
        c.setLayout(null);

        this.tabPane = null;
    }

    /**
     * Invoked by <code>installUI</code> to create
     * a layout manager object to manage
     * the <code>JTabbedPane</code>.
     *
     * @return a layout manager object
     *
     * @see TabbedPaneLayout
     * @see javax.swing.JTabbedPane#getTabLayoutPolicy
     */
    protected LayoutManager createLayoutManager() {
        if (tabPane.getTabLayoutPolicy() == JTabbedPane.SCROLL_TAB_LAYOUT) {
            return new TabbedPaneScrollLayout();
        } else { /* WRAP_TAB_LAYOUT */
            return new TabbedPaneLayout();
        }
    }

    /* In an attempt to preserve backward compatibility for programs
     * which have extended BasicTabbedPaneUI to do their own layout, the
     * UI uses the installed layoutManager (and not tabLayoutPolicy) to
     * determine if scrollTabLayout is enabled.
     */
    private boolean scrollableTabLayoutEnabled() {
        return (tabPane.getLayout() instanceof TabbedPaneScrollLayout);
    }

    /**
     * Creates and installs any required subcomponents for the JTabbedPane.
     * Invoked by installUI.
     *
     * @since 1.4
     */
    protected void installComponents() {
        if (scrollableTabLayoutEnabled()) {
            if (tabScroller == null) {
                tabScroller = new ScrollableTabSupport(tabPane.getTabPlacement());
                tabPane.add(tabScroller.viewport);
            }
        }
        installTabContainer();
    }

    private void installTabContainer() {
         for (int i = 0; i < tabPane.getTabCount(); i++) {
             Component tabComponent = tabPane.getTabComponentAt(i);
             if (tabComponent != null) {
                 if(tabContainer == null) {
                     tabContainer = new TabContainer();
                 }
                 tabContainer.add(tabComponent);
             }
         }
         if(tabContainer == null) {
             return;
         }
         if (scrollableTabLayoutEnabled()) {
             tabScroller.tabPanel.add(tabContainer);
         } else {
             tabPane.add(tabContainer);
         }
    }

    /**
     * Creates and returns a JButton that will provide the user
     * with a way to scroll the tabs in a particular direction. The
     * returned JButton must be instance of UIResource.
     *
     * @param direction One of the SwingConstants constants:
     * SOUTH, NORTH, EAST or WEST
     * @return Widget for user to
     * @see javax.swing.JTabbedPane#setTabPlacement
     * @see javax.swing.SwingConstants
     * @throws IllegalArgumentException if direction is not one of
     *         NORTH, SOUTH, EAST or WEST
     * @since 1.5
     */
    protected JButton createScrollButton(int direction) {
        if (direction != SOUTH && direction != NORTH && direction != EAST &&
                                  direction != WEST) {
            throw new IllegalArgumentException("Direction must be one of: " +
                                               "SOUTH, NORTH, EAST or WEST");
        }
        return new ScrollableTabButton(direction);
    }

    /**
     * Removes any installed subcomponents from the JTabbedPane.
     * Invoked by uninstallUI.
     *
     * @since 1.4
     */
    protected void uninstallComponents() {
        uninstallTabContainer();
        if (scrollableTabLayoutEnabled()) {
            tabPane.remove(tabScroller.viewport);
            tabPane.remove(tabScroller.scrollForwardButton);
            tabPane.remove(tabScroller.scrollBackwardButton);
            tabScroller = null;
        }
    }

    private void uninstallTabContainer() {
         if(tabContainer == null) {
             return;
         }
         // Remove all the tabComponents, making sure not to notify
         // the tabbedpane.
         tabContainer.notifyTabbedPane = false;
         tabContainer.removeAll();
         if(scrollableTabLayoutEnabled()) {
             tabContainer.remove(tabScroller.croppedEdge);
             tabScroller.tabPanel.remove(tabContainer);
         } else {
           tabPane.remove(tabContainer);
         }
         tabContainer = null;
    }

    /**
     * Install the defaults.
     */
    protected void installDefaults() {
        LookAndFeel.installColorsAndFont(tabPane, "TabbedPane.background",
                                    "TabbedPane.foreground", "TabbedPane.font");
        highlight = UIManager.getColor("TabbedPane.light");
        lightHighlight = UIManager.getColor("TabbedPane.highlight");
        shadow = UIManager.getColor("TabbedPane.shadow");
        darkShadow = UIManager.getColor("TabbedPane.darkShadow");
        focus = UIManager.getColor("TabbedPane.focus");
        selectedColor = UIManager.getColor("TabbedPane.selected");

        textIconGap = UIManager.getInt("TabbedPane.textIconGap");
        tabInsets = UIManager.getInsets("TabbedPane.tabInsets");
        selectedTabPadInsets = UIManager.getInsets("TabbedPane.selectedTabPadInsets");
        tabAreaInsets = UIManager.getInsets("TabbedPane.tabAreaInsets");
        tabsOverlapBorder = UIManager.getBoolean("TabbedPane.tabsOverlapBorder");
        contentBorderInsets = UIManager.getInsets("TabbedPane.contentBorderInsets");
        tabRunOverlay = UIManager.getInt("TabbedPane.tabRunOverlay");
        tabsOpaque = UIManager.getBoolean("TabbedPane.tabsOpaque");
        contentOpaque = UIManager.getBoolean("TabbedPane.contentOpaque");
        Object opaque = UIManager.get("TabbedPane.opaque");
        if (opaque == null) {
            opaque = Boolean.FALSE;
        }
        LookAndFeel.installProperty(tabPane, "opaque", opaque);

        // Fix for 6711145 BasicTabbedPanuUI should not throw a NPE if these
        // keys are missing. So we are setting them to there default values here
        // if the keys are missing.
        if (tabInsets == null) tabInsets = new Insets(0,4,1,4);
        if (selectedTabPadInsets == null) selectedTabPadInsets = new Insets(2,2,2,1);
        if (tabAreaInsets == null) tabAreaInsets = new Insets(3,2,0,2);
        if (contentBorderInsets == null) contentBorderInsets = new Insets(2,2,3,3);
    }

    /**
     * Uninstall the defaults.
     */
    protected void uninstallDefaults() {
        highlight = null;
        lightHighlight = null;
        shadow = null;
        darkShadow = null;
        focus = null;
        tabInsets = null;
        selectedTabPadInsets = null;
        tabAreaInsets = null;
        contentBorderInsets = null;
    }

    /**
     * Install the listeners.
     */
    protected void installListeners() {
        if ((propertyChangeListener = createPropertyChangeListener()) != null) {
            tabPane.addPropertyChangeListener(propertyChangeListener);
        }
        if ((tabChangeListener = createChangeListener()) != null) {
            tabPane.addChangeListener(tabChangeListener);
        }
        if ((mouseListener = createMouseListener()) != null) {
            tabPane.addMouseListener(mouseListener);
        }
        tabPane.addMouseMotionListener(getHandler());
        if ((focusListener = createFocusListener()) != null) {
            tabPane.addFocusListener(focusListener);
        }
        tabPane.addContainerListener(getHandler());
        if (tabPane.getTabCount()>0) {
            htmlViews = createHTMLVector();
        }
    }

    /**
     * Uninstall the listeners.
     */
    protected void uninstallListeners() {
        if (mouseListener != null) {
            tabPane.removeMouseListener(mouseListener);
            mouseListener = null;
        }
        tabPane.removeMouseMotionListener(getHandler());
        if (focusListener != null) {
            tabPane.removeFocusListener(focusListener);
            focusListener = null;
        }

        tabPane.removeContainerListener(getHandler());
        if (htmlViews!=null) {
            htmlViews.removeAllElements();
            htmlViews = null;
        }
        if (tabChangeListener != null) {
            tabPane.removeChangeListener(tabChangeListener);
            tabChangeListener = null;
        }
        if (propertyChangeListener != null) {
            tabPane.removePropertyChangeListener(propertyChangeListener);
            propertyChangeListener = null;
        }
        handler = null;
    }

    /**
     * Creates a mouse listener.
     * @return a mouse listener
     */
    protected MouseListener createMouseListener() {
        return getHandler();
    }

    /**
     * Creates a focus listener.
     * @return a focus listener
     */
    protected FocusListener createFocusListener() {
        return getHandler();
    }

    /**
     * Creates a change listener.
     * @return a change listener
     */
    protected ChangeListener createChangeListener() {
        return getHandler();
    }

    /**
     * Creates a property change listener.
     * @return a property change listener
     */
    protected PropertyChangeListener createPropertyChangeListener() {
        return getHandler();
    }

    private Handler getHandler() {
        if (handler == null) {
            handler = new Handler();
        }
        return handler;
    }

    /**
     * Installs the keyboard actions.
     */
    protected void installKeyboardActions() {
        InputMap km = getInputMap(JComponent.
                                  WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        SwingUtilities.replaceUIInputMap(tabPane, JComponent.
                                         WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                                         km);
        km = getInputMap(JComponent.WHEN_FOCUSED);
        SwingUtilities.replaceUIInputMap(tabPane, JComponent.WHEN_FOCUSED, km);

        LazyActionMap.installLazyActionMap(tabPane, BasicTabbedPaneUI.class,
                                           "TabbedPane.actionMap");
        updateMnemonics();
    }

    InputMap getInputMap(int condition) {
        if (condition == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) {
            return (InputMap)DefaultLookup.get(tabPane, this,
                                               "TabbedPane.ancestorInputMap");
        }
        else if (condition == JComponent.WHEN_FOCUSED) {
            return (InputMap)DefaultLookup.get(tabPane, this,
                                               "TabbedPane.focusInputMap");
        }
        return null;
    }

    /**
     * Uninstalls the keyboard actions.
     */
    protected void uninstallKeyboardActions() {
        SwingUtilities.replaceUIActionMap(tabPane, null);
        SwingUtilities.replaceUIInputMap(tabPane, JComponent.
                                         WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                                         null);
        SwingUtilities.replaceUIInputMap(tabPane, JComponent.WHEN_FOCUSED,
                                         null);
        SwingUtilities.replaceUIInputMap(tabPane,
                                         JComponent.WHEN_IN_FOCUSED_WINDOW,
                                         null);
        mnemonicToIndexMap = null;
        mnemonicInputMap = null;
    }

    /**
     * Reloads the mnemonics. This should be invoked when a memonic changes,
     * when the title of a mnemonic changes, or when tabs are added/removed.
     */
    private void updateMnemonics() {
        resetMnemonics();
        for (int counter = tabPane.getTabCount() - 1; counter >= 0;
             counter--) {
            int mnemonic = tabPane.getMnemonicAt(counter);

            if (mnemonic > 0) {
                addMnemonic(counter, mnemonic);
            }
        }
    }

    /**
     * Resets the mnemonics bindings to an empty state.
     */
    private void resetMnemonics() {
        if (mnemonicToIndexMap != null) {
            mnemonicToIndexMap.clear();
            mnemonicInputMap.clear();
        }
    }

    /**
     * Adds the specified mnemonic at the specified index.
     */
    private void addMnemonic(int index, int mnemonic) {
        if (mnemonicToIndexMap == null) {
            initMnemonics();
        }
        mnemonicInputMap.put(KeyStroke.getKeyStroke(mnemonic, BasicLookAndFeel.getFocusAcceleratorKeyMask()),
                             "setSelectedIndex");
        mnemonicInputMap.put(KeyStroke.getKeyStroke(mnemonic,
                SwingUtilities2.setAltGraphMask(
                        BasicLookAndFeel.getFocusAcceleratorKeyMask())),
                "setSelectedIndex");
        mnemonicToIndexMap.put(Integer.valueOf(mnemonic), Integer.valueOf(index));
    }

    /**
     * Installs the state needed for mnemonics.
     */
    private void initMnemonics() {
        mnemonicToIndexMap = new Hashtable<Integer, Integer>();
        mnemonicInputMap = new ComponentInputMapUIResource(tabPane);
        mnemonicInputMap.setParent(SwingUtilities.getUIInputMap(tabPane,
                              JComponent.WHEN_IN_FOCUSED_WINDOW));
        SwingUtilities.replaceUIInputMap(tabPane,
                              JComponent.WHEN_IN_FOCUSED_WINDOW,
                                         mnemonicInputMap);
    }

    /**
     * Sets the tab the mouse is over by location. This is a cover method
     * for <code>setRolloverTab(tabForCoordinate(x, y, false))</code>.
     */
    private void setRolloverTab(int x, int y) {
        // NOTE:
        // This calls in with false otherwise it could trigger a validate,
        // which should NOT happen if the user is only dragging the
        // mouse around.
        setRolloverTab(tabForCoordinate(tabPane, x, y, false));
    }

    /**
     * Sets the tab the mouse is currently over to <code>index</code>.
     * <code>index</code> will be -1 if the mouse is no longer over any
     * tab. No checking is done to ensure the passed in index identifies a
     * valid tab.
     *
     * @param index Index of the tab the mouse is over.
     * @since 1.5
     */
    protected void setRolloverTab(int index) {
        rolloverTabIndex = index;
    }

    /**
     * Returns the tab the mouse is currently over, or {@code -1} if the mouse is no
     * longer over any tab.
     *
     * @return the tab the mouse is currently over, or {@code -1} if the mouse is no
     * longer over any tab
     * @since 1.5
     */
    protected int getRolloverTab() {
        return rolloverTabIndex;
    }

    public Dimension getMinimumSize(JComponent c) {
        // Default to LayoutManager's minimumLayoutSize
        return null;
    }

    public Dimension getMaximumSize(JComponent c) {
        // Default to LayoutManager's maximumLayoutSize
        return null;
    }

    /**
     * Returns the baseline.
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @see javax.swing.JComponent#getBaseline(int, int)
     * @since 1.6
     */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        int baseline = calculateBaselineIfNecessary();
        if (baseline != -1) {
            int placement = tabPane.getTabPlacement();
            Insets insets = tabPane.getInsets();
            Insets tabAreaInsets = getTabAreaInsets(placement);
            switch(placement) {
            case JTabbedPane.TOP:
                baseline += insets.top + tabAreaInsets.top;
                return baseline;
            case JTabbedPane.BOTTOM:
                baseline = height - insets.bottom -
                    tabAreaInsets.bottom - maxTabHeight + baseline;
                return baseline;
            case JTabbedPane.LEFT:
            case JTabbedPane.RIGHT:
                baseline += insets.top + tabAreaInsets.top;
                return baseline;
            }
        }
        return -1;
    }

    /**
     * Returns an enum indicating how the baseline of the component
     * changes as the size changes.
     *
     * @throws NullPointerException {@inheritDoc}
     * @see javax.swing.JComponent#getBaseline(int, int)
     * @since 1.6
     */
    public Component.BaselineResizeBehavior getBaselineResizeBehavior(
            JComponent c) {
        super.getBaselineResizeBehavior(c);
        switch(tabPane.getTabPlacement()) {
        case JTabbedPane.LEFT:
        case JTabbedPane.RIGHT:
        case JTabbedPane.TOP:
            return Component.BaselineResizeBehavior.CONSTANT_ASCENT;
        case JTabbedPane.BOTTOM:
            return Component.BaselineResizeBehavior.CONSTANT_DESCENT;
        }
        return Component.BaselineResizeBehavior.OTHER;
    }

    /**
     * Returns the baseline for the specified tab.
     *
     * @param tab index of tab to get baseline for
     * @exception IndexOutOfBoundsException if index is out of range
     *            (index &lt; 0 || index &gt;= tab count)
     * @return baseline or a value &lt; 0 indicating there is no reasonable
     *                  baseline
     * @since 1.6
     */
    protected int getBaseline(int tab) {
        if (tabPane.getTabComponentAt(tab) != null) {
            int offset = getBaselineOffset();
            if (offset != 0) {
                // The offset is not applied to the tab component, and so
                // in general we can't get good alignment like with components
                // in the tab.
                return -1;
            }
            Component c = tabPane.getTabComponentAt(tab);
            Dimension pref = c.getPreferredSize();
            Insets tabInsets = getTabInsets(tabPane.getTabPlacement(), tab);
            int cellHeight = maxTabHeight - tabInsets.top - tabInsets.bottom;
            return c.getBaseline(pref.width, pref.height) +
                    (cellHeight - pref.height) / 2 + tabInsets.top;
        }
        else {
            View view = getTextViewForTab(tab);
            if (view != null) {
                int viewHeight = (int)view.getPreferredSpan(View.Y_AXIS);
                int baseline = BasicHTML.getHTMLBaseline(
                    view, (int)view.getPreferredSpan(View.X_AXIS), viewHeight);
                if (baseline >= 0) {
                    return maxTabHeight / 2 - viewHeight / 2 + baseline +
                        getBaselineOffset();
                }
                return -1;
            }
        }
        FontMetrics metrics = getFontMetrics();
        int fontHeight = metrics.getHeight();
        int fontBaseline = metrics.getAscent();
        return maxTabHeight / 2 - fontHeight / 2 + fontBaseline +
                getBaselineOffset();
    }

    /**
     * Returns the amount the baseline is offset by.  This is typically
     * the same as <code>getTabLabelShiftY</code>.
     *
     * @return amount to offset the baseline by
     * @since 1.6
     */
    protected int getBaselineOffset() {
        switch(tabPane.getTabPlacement()) {
        case JTabbedPane.TOP:
            if (tabPane.getTabCount() > 1) {
                return 1;
            }
            else {
                return -1;
            }
        case JTabbedPane.BOTTOM:
            if (tabPane.getTabCount() > 1) {
                return -1;
            }
            else {
                return 1;
            }
        default: // RIGHT|LEFT
            return (maxTabHeight % 2);
        }
    }

    private int calculateBaselineIfNecessary() {
        if (!calculatedBaseline) {
            calculatedBaseline = true;
            baseline = -1;
            if (tabPane.getTabCount() > 0) {
                calculateBaseline();
            }
        }
        return baseline;
    }

    private void calculateBaseline() {
        int tabCount = tabPane.getTabCount();
        int tabPlacement = tabPane.getTabPlacement();
        maxTabHeight = calculateMaxTabHeight(tabPlacement);
        baseline = getBaseline(0);
        if (isHorizontalTabPlacement()) {
            for(int i = 1; i < tabCount; i++) {
                if (getBaseline(i) != baseline) {
                    baseline = -1;
                    break;
                }
            }
        }
        else {
            // left/right, tabs may be different sizes.
            FontMetrics fontMetrics = getFontMetrics();
            int fontHeight = fontMetrics.getHeight();
            int height = calculateTabHeight(tabPlacement, 0, fontHeight);
            for(int i = 1; i < tabCount; i++) {
                int newHeight = calculateTabHeight(tabPlacement, i,fontHeight);
                if (height != newHeight) {
                    // assume different baseline
                    baseline = -1;
                    break;
                }
            }
        }
    }

// UI Rendering

    public void paint(Graphics g, JComponent c) {
        int selectedIndex = tabPane.getSelectedIndex();
        int tabPlacement = tabPane.getTabPlacement();

        ensureCurrentLayout();

        // Paint content border and tab area
        if (tabsOverlapBorder) {
            paintContentBorder(g, tabPlacement, selectedIndex);
        }
        // If scrollable tabs are enabled, the tab area will be
        // painted by the scrollable tab panel instead.
        //
        if (!scrollableTabLayoutEnabled()) { // WRAP_TAB_LAYOUT
            paintTabArea(g, tabPlacement, selectedIndex);
        }
        if (!tabsOverlapBorder) {
            paintContentBorder(g, tabPlacement, selectedIndex);
        }
    }

    /**
     * Paints the tabs in the tab area.
     * Invoked by paint().
     * The graphics parameter must be a valid <code>Graphics</code>
     * object.  Tab placement may be either:
     * <code>JTabbedPane.TOP</code>, <code>JTabbedPane.BOTTOM</code>,
     * <code>JTabbedPane.LEFT</code>, or <code>JTabbedPane.RIGHT</code>.
     * The selected index must be a valid tabbed pane tab index (0 to
     * tab count - 1, inclusive) or -1 if no tab is currently selected.
     * The handling of invalid parameters is unspecified.
     *
     * @param g the graphics object to use for rendering
     * @param tabPlacement the placement for the tabs within the JTabbedPane
     * @param selectedIndex the tab index of the selected component
     *
     * @since 1.4
     */
    protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
        int tabCount = tabPane.getTabCount();

        Rectangle iconRect = new Rectangle(),
                  textRect = new Rectangle();
        Rectangle clipRect = g.getClipBounds();

        // Paint tabRuns of tabs from back to front
        for (int i = runCount - 1; i >= 0; i--) {
            int start = tabRuns[i];
            int next = tabRuns[(i == runCount - 1)? 0 : i + 1];
            int end = (next != 0? next - 1: tabCount - 1);
            for (int j = start; j <= end; j++) {
                if (j != selectedIndex && rects[j].intersects(clipRect)) {
                    paintTab(g, tabPlacement, rects, j, iconRect, textRect);
                }
            }
        }

        // Paint selected tab if its in the front run
        // since it may overlap other tabs
        if (selectedIndex >= 0 && rects[selectedIndex].intersects(clipRect)) {
            paintTab(g, tabPlacement, rects, selectedIndex, iconRect, textRect);
        }
    }

    /**
     * Paints a tab.
     * @param g the graphics
     * @param tabPlacement the tab placement
     * @param rects rectangles
     * @param tabIndex the tab index
     * @param iconRect the icon rectangle
     * @param textRect the text rectangle
     */
    protected void paintTab(Graphics g, int tabPlacement,
                            Rectangle[] rects, int tabIndex,
                            Rectangle iconRect, Rectangle textRect) {
        Rectangle tabRect = rects[tabIndex];
        int selectedIndex = tabPane.getSelectedIndex();
        boolean isSelected = selectedIndex == tabIndex;

        if (tabsOpaque || tabPane.isOpaque()) {
            paintTabBackground(g, tabPlacement, tabIndex, tabRect.x, tabRect.y,
                    tabRect.width, tabRect.height, isSelected);
        }

        paintTabBorder(g, tabPlacement, tabIndex, tabRect.x, tabRect.y,
                       tabRect.width, tabRect.height, isSelected);

        String title = tabPane.getTitleAt(tabIndex);
        Font font = tabPane.getFont();
        FontMetrics metrics = SwingUtilities2.getFontMetrics(tabPane, g, font);
        Icon icon = getIconForTab(tabIndex);

        layoutLabel(tabPlacement, metrics, tabIndex, title, icon,
                    tabRect, iconRect, textRect, isSelected);

        if (tabPane.getTabComponentAt(tabIndex) == null) {
            String clippedTitle = title;

            if (scrollableTabLayoutEnabled() && tabScroller.croppedEdge.isParamsSet() &&
                    tabScroller.croppedEdge.getTabIndex() == tabIndex && isHorizontalTabPlacement()) {
                int availTextWidth = tabScroller.croppedEdge.getCropline() -
                        (textRect.x - tabRect.x) - tabScroller.croppedEdge.getCroppedSideWidth();
                clippedTitle = SwingUtilities2.clipStringIfNecessary(null, metrics, title, availTextWidth);
            } else if (!scrollableTabLayoutEnabled() && isHorizontalTabPlacement()) {
                clippedTitle = SwingUtilities2.clipStringIfNecessary(null, metrics, title, textRect.width);
            }

            paintText(g, tabPlacement, font, metrics,
                    tabIndex, clippedTitle, textRect, isSelected);

            paintIcon(g, tabPlacement, tabIndex, icon, iconRect, isSelected);
        }
        paintFocusIndicator(g, tabPlacement, rects, tabIndex,
                  iconRect, textRect, isSelected);
    }

    private boolean isHorizontalTabPlacement() {
        return tabPane.getTabPlacement() == TOP || tabPane.getTabPlacement() == BOTTOM;
    }

    /* This method will create and return a polygon shape for the given tab rectangle
     * which has been cropped at the specified cropline with a torn edge visual.
     * e.g. A "File" tab which has cropped been cropped just after the "i":
     *             -------------
     *             |  .....     |
     *             |  .          |
     *             |  ...  .    |
     *             |  .    .   |
     *             |  .    .    |
     *             |  .    .     |
     *             --------------
     *
     * The x, y arrays below define the pattern used to create a "torn" edge
     * segment which is repeated to fill the edge of the tab.
     * For tabs placed on TOP and BOTTOM, this righthand torn edge is created by
     * line segments which are defined by coordinates obtained by
     * subtracting xCropLen[i] from (tab.x + tab.width) and adding yCroplen[i]
     * to (tab.y).
     * For tabs placed on LEFT or RIGHT, the bottom torn edge is created by
     * subtracting xCropLen[i] from (tab.y + tab.height) and adding yCropLen[i]
     * to (tab.x).
     */
    private static int[] xCropLen = {1,1,0,0,1,1,2,2};
    private static int[] yCropLen = {0,3,3,6,6,9,9,12};
    private static final int CROP_SEGMENT = 12;

    private static Polygon createCroppedTabShape(int tabPlacement, Rectangle tabRect, int cropline) {
        int rlen;
        int start;
        int end;
        int ostart;

        switch(tabPlacement) {
          case LEFT:
          case RIGHT:
              rlen = tabRect.width;
              start = tabRect.x;
              end = tabRect.x + tabRect.width;
              ostart = tabRect.y + tabRect.height;
              break;
          case TOP:
          case BOTTOM:
          default:
             rlen = tabRect.height;
             start = tabRect.y;
             end = tabRect.y + tabRect.height;
             ostart = tabRect.x + tabRect.width;
        }
        int rcnt = rlen/CROP_SEGMENT;
        if (rlen%CROP_SEGMENT > 0) {
            rcnt++;
        }
        int npts = 2 + (rcnt*8);
        int[] xp = new int[npts];
        int[] yp = new int[npts];
        int pcnt = 0;

        xp[pcnt] = ostart;
        yp[pcnt++] = end;
        xp[pcnt] = ostart;
        yp[pcnt++] = start;
        for(int i = 0; i < rcnt; i++) {
            for(int j = 0; j < xCropLen.length; j++) {
                xp[pcnt] = cropline - xCropLen[j];
                yp[pcnt] = start + (i*CROP_SEGMENT) + yCropLen[j];
                if (yp[pcnt] >= end) {
                    yp[pcnt] = end;
                    pcnt++;
                    break;
                }
                pcnt++;
            }
        }
        if (tabPlacement == JTabbedPane.TOP || tabPlacement == JTabbedPane.BOTTOM) {
           return new Polygon(xp, yp, pcnt);

        } else { // LEFT or RIGHT
           return new Polygon(yp, xp, pcnt);
        }
    }

    /* If tabLayoutPolicy == SCROLL_TAB_LAYOUT, this method will paint an edge
     * indicating the tab is cropped in the viewport display
     */
    private void paintCroppedTabEdge(Graphics g) {
        int tabIndex = tabScroller.croppedEdge.getTabIndex();
        int cropline = tabScroller.croppedEdge.getCropline();
        int x,y;
        switch(tabPane.getTabPlacement()) {
          case LEFT:
          case RIGHT:
            x = rects[tabIndex].x;
            y = cropline;
            int xx = x;
            g.setColor(shadow);
            while(xx <= x+rects[tabIndex].width) {
                for (int i=0; i < xCropLen.length; i+=2) {
                    g.drawLine(xx+yCropLen[i],y-xCropLen[i],
                               xx+yCropLen[i+1]-1,y-xCropLen[i+1]);
                }
                xx+=CROP_SEGMENT;
            }
            break;
          case TOP:
          case BOTTOM:
          default:
            x = cropline;
            y = rects[tabIndex].y;
            int yy = y;
            g.setColor(shadow);
            while(yy <= y+rects[tabIndex].height) {
                for (int i=0; i < xCropLen.length; i+=2) {
                    g.drawLine(x-xCropLen[i],yy+yCropLen[i],
                               x-xCropLen[i+1],yy+yCropLen[i+1]-1);
                }
                yy+=CROP_SEGMENT;
            }
        }
    }

    /**
     * Laysout a label.
     * @param tabPlacement the tab placement
     * @param metrics the font metric
     * @param tabIndex the tab index
     * @param title the title
     * @param icon the icon
     * @param tabRect the tab rectangle
     * @param iconRect the icon rectangle
     * @param textRect the text rectangle
     * @param isSelected selection status
     */
    protected void layoutLabel(int tabPlacement,
                               FontMetrics metrics, int tabIndex,
                               String title, Icon icon,
                               Rectangle tabRect, Rectangle iconRect,
                               Rectangle textRect, boolean isSelected ) {
        textRect.x = textRect.y = iconRect.x = iconRect.y = 0;

        View v = getTextViewForTab(tabIndex);
        if (v != null) {
            tabPane.putClientProperty("html", v);
        }

        SwingUtilities.layoutCompoundLabel(tabPane,
                                           metrics, title, icon,
                                           SwingUtilities.CENTER,
                                           SwingUtilities.CENTER,
                                           SwingUtilities.CENTER,
                                           SwingUtilities.TRAILING,
                                           tabRect,
                                           iconRect,
                                           textRect,
                                           textIconGap);

        tabPane.putClientProperty("html", null);

        int xNudge = getTabLabelShiftX(tabPlacement, tabIndex, isSelected);
        int yNudge = getTabLabelShiftY(tabPlacement, tabIndex, isSelected);
        iconRect.x += xNudge;
        iconRect.y += yNudge;
        textRect.x += xNudge;
        textRect.y += yNudge;
    }

    /**
     * Paints an icon.
     * @param g the graphics
     * @param tabPlacement the tab placement
     * @param tabIndex the tab index
     * @param icon the icon
     * @param iconRect the icon rectangle
     * @param isSelected selection status
     */
    protected void paintIcon(Graphics g, int tabPlacement,
                             int tabIndex, Icon icon, Rectangle iconRect,
                             boolean isSelected ) {
        if (icon != null) {
            // Clip the icon within iconRect bounds
            Shape oldClip = g.getClip();
            ((Graphics2D)g).clip(iconRect);
            icon.paintIcon(tabPane, g, iconRect.x, iconRect.y);
            g.setClip(oldClip);
        }
    }

    /**
     * Paints text.
     * @param g the graphics
     * @param tabPlacement the tab placement
     * @param font the font
     * @param metrics the font metrics
     * @param tabIndex the tab index
     * @param title the title
     * @param textRect the text rectangle
     * @param isSelected selection status
     */
    protected void paintText(Graphics g, int tabPlacement,
                             Font font, FontMetrics metrics, int tabIndex,
                             String title, Rectangle textRect,
                             boolean isSelected) {

        g.setFont(font);

        View v = getTextViewForTab(tabIndex);
        if (v != null) {
            // html
            v.paint(g, textRect);
        } else {
            // plain text
            int mnemIndex = tabPane.getDisplayedMnemonicIndexAt(tabIndex);

            if (tabPane.isEnabled() && tabPane.isEnabledAt(tabIndex)) {
                Color fg = tabPane.getForegroundAt(tabIndex);
                if (isSelected && (fg instanceof UIResource)) {
                    Color selectedFG = UIManager.getColor(
                                  "TabbedPane.selectedForeground");
                    if (selectedFG != null) {
                        fg = selectedFG;
                    }
                }
                g.setColor(fg);
                SwingUtilities2.drawStringUnderlineCharAt(tabPane, g,
                             title, mnemIndex,
                             textRect.x, textRect.y + metrics.getAscent());

            } else { // tab disabled
                g.setColor(tabPane.getBackgroundAt(tabIndex).brighter());
                SwingUtilities2.drawStringUnderlineCharAt(tabPane, g,
                             title, mnemIndex,
                             textRect.x, textRect.y + metrics.getAscent());
                g.setColor(tabPane.getBackgroundAt(tabIndex).darker());
                SwingUtilities2.drawStringUnderlineCharAt(tabPane, g,
                             title, mnemIndex,
                             textRect.x - 1, textRect.y + metrics.getAscent() - 1);

            }
        }
    }

    /**
     * Returns the tab label shift x.
     * @param tabPlacement the tab placement
     * @param tabIndex the tab index
     * @param isSelected selection status
     * @return the tab label shift x
     */
    protected int getTabLabelShiftX(int tabPlacement, int tabIndex, boolean isSelected) {
        Rectangle tabRect = rects[tabIndex];
        String propKey = (isSelected ? "selectedLabelShift" : "labelShift");
        int nudge = DefaultLookup.getInt(
                tabPane, this, "TabbedPane." + propKey, 1);

        switch (tabPlacement) {
            case LEFT:
                return nudge;
            case RIGHT:
                return -nudge;
            case BOTTOM:
            case TOP:
            default:
                return tabRect.width % 2;
        }
    }

    /**
     * Returns the tab label shift y.
     * @param tabPlacement the tab placement
     * @param tabIndex the tab index
     * @param isSelected selection status
     * @return the tab label shift y
     */
    protected int getTabLabelShiftY(int tabPlacement, int tabIndex, boolean isSelected) {
        Rectangle tabRect = rects[tabIndex];
        int nudge = (isSelected ? DefaultLookup.getInt(tabPane, this, "TabbedPane.selectedLabelShift", -1) :
                DefaultLookup.getInt(tabPane, this, "TabbedPane.labelShift", 1));

        switch (tabPlacement) {
            case BOTTOM:
                return -nudge;
            case LEFT:
            case RIGHT:
                return tabRect.height % 2;
            case TOP:
            default:
                return nudge;
        }
    }

    /**
     * Paints the focus indicator.
     * @param g the graphics
     * @param tabPlacement the tab placement
     * @param rects rectangles
     * @param tabIndex the tab index
     * @param iconRect the icon rectangle
     * @param textRect the text rectangle
     * @param isSelected selection status
     */
    protected void paintFocusIndicator(Graphics g, int tabPlacement,
                                       Rectangle[] rects, int tabIndex,
                                       Rectangle iconRect, Rectangle textRect,
                                       boolean isSelected) {
        Rectangle tabRect = rects[tabIndex];
        if (tabPane.hasFocus() && isSelected) {
            int x, y, w, h;
            g.setColor(focus);
            switch(tabPlacement) {
              case LEFT:
                  x = tabRect.x + 3;
                  y = tabRect.y + 3;
                  w = tabRect.width - 5;
                  h = tabRect.height - 6;
                  break;
              case RIGHT:
                  x = tabRect.x + 2;
                  y = tabRect.y + 3;
                  w = tabRect.width - 5;
                  h = tabRect.height - 6;
                  break;
              case BOTTOM:
                  x = tabRect.x + 3;
                  y = tabRect.y + 2;
                  w = tabRect.width - 6;
                  h = tabRect.height - 5;
                  break;
              case TOP:
              default:
                  x = tabRect.x + 3;
                  y = tabRect.y + 3;
                  w = tabRect.width - 6;
                  h = tabRect.height - 5;
            }
            BasicGraphicsUtils.drawDashedRect(g, x, y, w, h);
        }
    }

    /**
      * this function draws the border around each tab
      * note that this function does now draw the background of the tab.
      * that is done elsewhere
      *
      * @param g             the graphics context in which to paint
      * @param tabPlacement  the placement (left, right, bottom, top) of the tab
      * @param tabIndex      the index of the tab with respect to other tabs
      * @param x             the x coordinate of tab
      * @param y             the y coordinate of tab
      * @param w             the width of the tab
      * @param h             the height of the tab
      * @param isSelected    a {@code boolean} which determines whether or not
      * the tab is selected
      */
    protected void paintTabBorder(Graphics g, int tabPlacement,
                                  int tabIndex,
                                  int x, int y, int w, int h,
                                  boolean isSelected ) {
        g.setColor(lightHighlight);

        switch (tabPlacement) {
          case LEFT:
              g.drawLine(x+1, y+h-2, x+1, y+h-2); // bottom-left highlight
              g.drawLine(x, y+2, x, y+h-3); // left highlight
              g.drawLine(x+1, y+1, x+1, y+1); // top-left highlight
              g.drawLine(x+2, y, x+w-1, y); // top highlight

              g.setColor(shadow);
              g.drawLine(x+2, y+h-2, x+w-1, y+h-2); // bottom shadow

              g.setColor(darkShadow);
              g.drawLine(x+2, y+h-1, x+w-1, y+h-1); // bottom dark shadow
              break;
          case RIGHT:
              g.drawLine(x, y, x+w-3, y); // top highlight

              g.setColor(shadow);
              g.drawLine(x, y+h-2, x+w-3, y+h-2); // bottom shadow
              g.drawLine(x+w-2, y+2, x+w-2, y+h-3); // right shadow

              g.setColor(darkShadow);
              g.drawLine(x+w-2, y+1, x+w-2, y+1); // top-right dark shadow
              g.drawLine(x+w-2, y+h-2, x+w-2, y+h-2); // bottom-right dark shadow
              g.drawLine(x+w-1, y+2, x+w-1, y+h-3); // right dark shadow
              g.drawLine(x, y+h-1, x+w-3, y+h-1); // bottom dark shadow
              break;
          case BOTTOM:
              g.drawLine(x, y, x, y+h-3); // left highlight
              g.drawLine(x+1, y+h-2, x+1, y+h-2); // bottom-left highlight

              g.setColor(shadow);
              g.drawLine(x+2, y+h-2, x+w-3, y+h-2); // bottom shadow
              g.drawLine(x+w-2, y, x+w-2, y+h-3); // right shadow

              g.setColor(darkShadow);
              g.drawLine(x+2, y+h-1, x+w-3, y+h-1); // bottom dark shadow
              g.drawLine(x+w-2, y+h-2, x+w-2, y+h-2); // bottom-right dark shadow
              g.drawLine(x+w-1, y, x+w-1, y+h-3); // right dark shadow
              break;
          case TOP:
          default:
              g.drawLine(x, y+2, x, y+h-1); // left highlight
              g.drawLine(x+1, y+1, x+1, y+1); // top-left highlight
              g.drawLine(x+2, y, x+w-3, y); // top highlight

              g.setColor(shadow);
              g.drawLine(x+w-2, y+2, x+w-2, y+h-1); // right shadow

              g.setColor(darkShadow);
              g.drawLine(x+w-1, y+2, x+w-1, y+h-1); // right dark-shadow
              g.drawLine(x+w-2, y+1, x+w-2, y+1); // top-right shadow
        }
    }

    /**
     * Paints the tab background.
     * @param g             the graphics context in which to paint
     * @param tabPlacement  the placement (left, right, bottom, top) of the tab
     * @param tabIndex      the index of the tab with respect to other tabs
     * @param x             the x coordinate of tab
     * @param y             the y coordinate of tab
     * @param w             the width of the tab
     * @param h             the height of the tab
     * @param isSelected    a {@code boolean} which determines whether or not
     * the tab is selected
     */
    protected void paintTabBackground(Graphics g, int tabPlacement,
                                      int tabIndex,
                                      int x, int y, int w, int h,
                                      boolean isSelected ) {
        g.setColor(!isSelected || selectedColor == null?
                   tabPane.getBackgroundAt(tabIndex) : selectedColor);
        switch(tabPlacement) {
          case LEFT:
              g.fillRect(x+1, y+1, w-1, h-3);
              break;
          case RIGHT:
              g.fillRect(x, y+1, w-2, h-3);
              break;
          case BOTTOM:
              g.fillRect(x+1, y, w-3, h-1);
              break;
          case TOP:
          default:
              g.fillRect(x+1, y+1, w-3, h-1);
        }
    }

    /**
     * Paints the content border.
     * @param g             the graphics context in which to paint
     * @param tabPlacement  the placement (left, right, bottom, top) of the tab
     * @param selectedIndex the tab index of the selected component
     */
    protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
        int width = tabPane.getWidth();
        int height = tabPane.getHeight();
        Insets insets = tabPane.getInsets();
        Insets tabAreaInsets = getTabAreaInsets(tabPlacement);

        int x = insets.left;
        int y = insets.top;
        int w = width - insets.right - insets.left;
        int h = height - insets.top - insets.bottom;

        switch(tabPlacement) {
          case LEFT:
              x += calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
              if (tabsOverlapBorder) {
                  x -= tabAreaInsets.right;
              }
              w -= (x - insets.left);
              break;
          case RIGHT:
              w -= calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
              if (tabsOverlapBorder) {
                  w += tabAreaInsets.left;
              }
              break;
          case BOTTOM:
              h -= calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
              if (tabsOverlapBorder) {
                  h += tabAreaInsets.top;
              }
              break;
          case TOP:
          default:
              y += calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
              if (tabsOverlapBorder) {
                  y -= tabAreaInsets.bottom;
              }
              h -= (y - insets.top);
        }

            if ( tabPane.getTabCount() > 0 && (contentOpaque || tabPane.isOpaque()) ) {
            // Fill region behind content area
            Color color = UIManager.getColor("TabbedPane.contentAreaColor");
            if (color != null) {
                g.setColor(color);
            }
            else if ( selectedColor == null || selectedIndex == -1 ) {
                g.setColor(tabPane.getBackground());
            }
            else {
                g.setColor(selectedColor);
            }
            g.fillRect(x,y,w,h);
        }

        paintContentBorderTopEdge(g, tabPlacement, selectedIndex, x, y, w, h);
        paintContentBorderLeftEdge(g, tabPlacement, selectedIndex, x, y, w, h);
        paintContentBorderBottomEdge(g, tabPlacement, selectedIndex, x, y, w, h);
        paintContentBorderRightEdge(g, tabPlacement, selectedIndex, x, y, w, h);

    }

    /**
     * Paints the content border top edge.
     * @param g             the graphics context in which to paint
     * @param tabPlacement  the placement (left, right, bottom, top) of the tab
     * @param selectedIndex the tab index of the selected component
     * @param x             the x coordinate of tab
     * @param y             the y coordinate of tab
     * @param w             the width of the tab
     * @param h             the height of the tab
     */
    protected void paintContentBorderTopEdge(Graphics g, int tabPlacement,
                                         int selectedIndex,
                                         int x, int y, int w, int h) {
        Rectangle selRect = selectedIndex < 0? null :
                               getTabBounds(selectedIndex, calcRect);

        g.setColor(lightHighlight);

        // Draw unbroken line if tabs are not on TOP, OR
        // selected tab is not in run adjacent to content, OR
        // selected tab is not visible (SCROLL_TAB_LAYOUT)
        //
        if (tabPlacement != TOP || selectedIndex < 0 ||
            (selRect.y + selRect.height + 1 < y) ||
            (selRect.x < x || selRect.x > x + w)) {
            g.drawLine(x, y, x+w-2, y);
        } else {
            // Break line to show visual connection to selected tab
            g.drawLine(x, y, selRect.x - 1, y);
            if (selRect.x + selRect.width < x + w - 2) {
                g.drawLine(selRect.x + selRect.width, y,
                           x+w-2, y);
            } else {
                g.setColor(shadow);
                g.drawLine(x+w-2, y, x+w-2, y);
            }
        }
    }

    /**
     * Paints the content border left edge.
     * @param g             the graphics context in which to paint
     * @param tabPlacement  the placement (left, right, bottom, top) of the tab
     * @param selectedIndex the tab index of the selected component
     * @param x             the x coordinate of tab
     * @param y             the y coordinate of tab
     * @param w             the width of the tab
     * @param h             the height of the tab
     */
    protected void paintContentBorderLeftEdge(Graphics g, int tabPlacement,
                                               int selectedIndex,
                                               int x, int y, int w, int h) {
        Rectangle selRect = selectedIndex < 0? null :
                               getTabBounds(selectedIndex, calcRect);

        g.setColor(lightHighlight);

        // Draw unbroken line if tabs are not on LEFT, OR
        // selected tab is not in run adjacent to content, OR
        // selected tab is not visible (SCROLL_TAB_LAYOUT)
        //
        if (tabPlacement != LEFT || selectedIndex < 0 ||
            (selRect.x + selRect.width + 1 < x) ||
            (selRect.y < y || selRect.y > y + h)) {
            g.drawLine(x, y, x, y+h-2);
        } else {
            // Break line to show visual connection to selected tab
            g.drawLine(x, y, x, selRect.y - 1);
            if (selRect.y + selRect.height < y + h - 2) {
                g.drawLine(x, selRect.y + selRect.height,
                           x, y+h-2);
            }
        }
    }

    /**
     * Paints the content border bottom edge.
     * @param g             the graphics context in which to paint
     * @param tabPlacement  the placement (left, right, bottom, top) of the tab
     * @param selectedIndex the tab index of the selected component
     * @param x             the x coordinate of tab
     * @param y             the y coordinate of tab
     * @param w             the width of the tab
     * @param h             the height of the tab
     */
    protected void paintContentBorderBottomEdge(Graphics g, int tabPlacement,
                                               int selectedIndex,
                                               int x, int y, int w, int h) {
        Rectangle selRect = selectedIndex < 0? null :
                               getTabBounds(selectedIndex, calcRect);

        g.setColor(shadow);

        // Draw unbroken line if tabs are not on BOTTOM, OR
        // selected tab is not in run adjacent to content, OR
        // selected tab is not visible (SCROLL_TAB_LAYOUT)
        //
        if (tabPlacement != BOTTOM || selectedIndex < 0 ||
             (selRect.y - 1 > h) ||
             (selRect.x < x || selRect.x > x + w)) {
            g.drawLine(x+1, y+h-2, x+w-2, y+h-2);
            g.setColor(darkShadow);
            g.drawLine(x, y+h-1, x+w-1, y+h-1);
        } else {
            // Break line to show visual connection to selected tab
            g.drawLine(x+1, y+h-2, selRect.x - 1, y+h-2);
            g.setColor(darkShadow);
            g.drawLine(x, y+h-1, selRect.x - 1, y+h-1);
            if (selRect.x + selRect.width < x + w - 2) {
                g.setColor(shadow);
                g.drawLine(selRect.x + selRect.width, y+h-2, x+w-2, y+h-2);
                g.setColor(darkShadow);
                g.drawLine(selRect.x + selRect.width, y+h-1, x+w-1, y+h-1);
            }
        }

    }

    /**
     * Paints the content border right edge.
     * @param g             the graphics context in which to paint
     * @param tabPlacement  the placement (left, right, bottom, top) of the tab
     * @param selectedIndex the tab index of the selected component
     * @param x             the x coordinate of tab
     * @param y             the y coordinate of tab
     * @param w             the width of the tab
     * @param h             the height of the tab
     */
    protected void paintContentBorderRightEdge(Graphics g, int tabPlacement,
                                               int selectedIndex,
                                               int x, int y, int w, int h) {
        Rectangle selRect = selectedIndex < 0? null :
                               getTabBounds(selectedIndex, calcRect);

        g.setColor(shadow);

        // Draw unbroken line if tabs are not on RIGHT, OR
        // selected tab is not in run adjacent to content, OR
        // selected tab is not visible (SCROLL_TAB_LAYOUT)
        //
        if (tabPlacement != RIGHT || selectedIndex < 0 ||
             (selRect.x - 1 > w) ||
             (selRect.y < y || selRect.y > y + h)) {
            g.drawLine(x+w-2, y+1, x+w-2, y+h-3);
            g.setColor(darkShadow);
            g.drawLine(x+w-1, y, x+w-1, y+h-1);
        } else {
            // Break line to show visual connection to selected tab
            g.drawLine(x+w-2, y+1, x+w-2, selRect.y - 1);
            g.setColor(darkShadow);
            g.drawLine(x+w-1, y, x+w-1, selRect.y - 1);

            if (selRect.y + selRect.height < y + h - 2) {
                g.setColor(shadow);
                g.drawLine(x+w-2, selRect.y + selRect.height,
                           x+w-2, y+h-2);
                g.setColor(darkShadow);
                g.drawLine(x+w-1, selRect.y + selRect.height,
                           x+w-1, y+h-2);
            }
        }
    }

    private void ensureCurrentLayout() {
        if (!tabPane.isValid()) {
            tabPane.validate();
        }
        /* If tabPane doesn't have a peer yet, the validate() call will
         * silently fail.  We handle that by forcing a layout if tabPane
         * is still invalid.  See bug 4237677.
         */
        if (!tabPane.isValid()) {
            TabbedPaneLayout layout = (TabbedPaneLayout)tabPane.getLayout();
            layout.calculateLayoutInfo();
        }
    }


// TabbedPaneUI methods

    /**
     * Returns the bounds of the specified tab index.  The bounds are
     * with respect to the JTabbedPane's coordinate space.
     */
    public Rectangle getTabBounds(JTabbedPane pane, int i) {
        ensureCurrentLayout();
        Rectangle tabRect = new Rectangle();
        return getTabBounds(i, tabRect);
    }

    public int getTabRunCount(JTabbedPane pane) {
        ensureCurrentLayout();
        return runCount;
    }

    /**
     * Returns the tab index which intersects the specified point
     * in the JTabbedPane's coordinate space.
     */
    public int tabForCoordinate(JTabbedPane pane, int x, int y) {
        return tabForCoordinate(pane, x, y, true);
    }

    private int tabForCoordinate(JTabbedPane pane, int x, int y,
                                 boolean validateIfNecessary) {
        if (validateIfNecessary) {
            ensureCurrentLayout();
        }
        if (isRunsDirty) {
            // We didn't recalculate the layout, runs and tabCount may not
            // line up, bail.
            return -1;
        }
        Point p = new Point(x, y);

        if (scrollableTabLayoutEnabled()) {
            translatePointToTabPanel(x, y, p);
            Rectangle viewRect = tabScroller.viewport.getViewRect();
            if (!viewRect.contains(p)) {
                return -1;
            }
        }
        int tabCount = tabPane.getTabCount();
        for (int i = 0; i < tabCount; i++) {
            if (rects[i].contains(p.x, p.y)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the bounds of the specified tab in the coordinate space
     * of the JTabbedPane component.  This is required because the tab rects
     * are by default defined in the coordinate space of the component where
     * they are rendered, which could be the JTabbedPane
     * (for WRAP_TAB_LAYOUT) or a ScrollableTabPanel (SCROLL_TAB_LAYOUT).
     * This method should be used whenever the tab rectangle must be relative
     * to the JTabbedPane itself and the result should be placed in a
     * designated Rectangle object (rather than instantiating and returning
     * a new Rectangle each time). The tab index parameter must be a valid
     * tabbed pane tab index (0 to tab count - 1, inclusive).  The destination
     * rectangle parameter must be a valid <code>Rectangle</code> instance.
     * The handling of invalid parameters is unspecified.
     *
     * @param tabIndex the index of the tab
     * @param dest the rectangle where the result should be placed
     * @return the resulting rectangle
     *
     * @since 1.4
     */
    protected Rectangle getTabBounds(int tabIndex, Rectangle dest) {
        dest.width = rects[tabIndex].width;
        dest.height = rects[tabIndex].height;

        if (scrollableTabLayoutEnabled()) { // SCROLL_TAB_LAYOUT
            // Need to translate coordinates based on viewport location &
            // view position
            Point vpp = tabScroller.viewport.getLocation();
            Point viewp = tabScroller.viewport.getViewPosition();
            dest.x = rects[tabIndex].x + vpp.x - viewp.x;
            dest.y = rects[tabIndex].y + vpp.y - viewp.y;

        } else { // WRAP_TAB_LAYOUT
            dest.x = rects[tabIndex].x;
            dest.y = rects[tabIndex].y;
        }
        return dest;
    }

    /**
     * Returns the index of the tab closest to the passed in location, note
     * that the returned tab may not contain the location x,y.
     */
    private int getClosestTab(int x, int y) {
        int min = 0;
        int tabCount = Math.min(rects.length, tabPane.getTabCount());
        int max = tabCount;
        int tabPlacement = tabPane.getTabPlacement();
        boolean useX = (tabPlacement == TOP || tabPlacement == BOTTOM);
        int want = (useX) ? x : y;

        while (min != max) {
            int current = (max + min) / 2;
            int minLoc;
            int maxLoc;

            if (useX) {
                minLoc = rects[current].x;
                maxLoc = minLoc + rects[current].width;
            }
            else {
                minLoc = rects[current].y;
                maxLoc = minLoc + rects[current].height;
            }
            if (want < minLoc) {
                max = current;
                if (min == max) {
                    return Math.max(0, current - 1);
                }
            }
            else if (want >= maxLoc) {
                min = current;
                if (max - min <= 1) {
                    return Math.max(current + 1, tabCount - 1);
                }
            }
            else {
                return current;
            }
        }
        return min;
    }

    /**
     * Returns a point which is translated from the specified point in the
     * JTabbedPane's coordinate space to the coordinate space of the
     * ScrollableTabPanel.  This is used for SCROLL_TAB_LAYOUT ONLY.
     */
    private Point translatePointToTabPanel(int srcx, int srcy, Point dest) {
        Point vpp = tabScroller.viewport.getLocation();
        Point viewp = tabScroller.viewport.getViewPosition();
        dest.x = srcx - vpp.x + viewp.x;
        dest.y = srcy - vpp.y + viewp.y;
        return dest;
    }

// BasicTabbedPaneUI methods

    /**
     * Returns the visible component.
     * @return the visible component
     */
    protected Component getVisibleComponent() {
        return visibleComponent;
    }

    /**
     * Sets the visible component.
     * @param component the component
     */
    protected void setVisibleComponent(Component component) {
        if (visibleComponent != null
                && visibleComponent != component
                && visibleComponent.getParent() == tabPane
                && visibleComponent.isVisible()) {

            visibleComponent.setVisible(false);
        }
        if (component != null && !component.isVisible()) {
            component.setVisible(true);
        }
        visibleComponent = component;
    }

    /**
     * Assure the rectangles are created.
     * @param tabCount the tab count
     */
    protected void assureRectsCreated(int tabCount) {
        int rectArrayLen = rects.length;
        if (tabCount != rectArrayLen ) {
            Rectangle[] tempRectArray = new Rectangle[tabCount];
            System.arraycopy(rects, 0, tempRectArray, 0,
                             Math.min(rectArrayLen, tabCount));
            rects = tempRectArray;
            for (int rectIndex = rectArrayLen; rectIndex < tabCount; rectIndex++) {
                rects[rectIndex] = new Rectangle();
            }
        }

    }

    /**
     * Expands the tab runs array.
     */
    protected void expandTabRunsArray() {
        int rectLen = tabRuns.length;
        int[] newArray = new int[rectLen+10];
        System.arraycopy(tabRuns, 0, newArray, 0, runCount);
        tabRuns = newArray;
    }

    /**
     * Returns the run for a tab.
     * @param tabCount the tab count
     * @param tabIndex the tab index.
     * @return the run for a tab
     */
    protected int getRunForTab(int tabCount, int tabIndex) {
        for (int i = 0; i < runCount; i++) {
            int first = tabRuns[i];
            int last = lastTabInRun(tabCount, i);
            if (tabIndex >= first && tabIndex <= last) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Returns the last tab in a run.
     * @param tabCount the tab count
     * @param run the run
     * @return the last tab in a run
     */
    protected int lastTabInRun(int tabCount, int run) {
        if (runCount == 1) {
            return tabCount - 1;
        }
        int nextRun = (run == runCount - 1? 0 : run + 1);
        if (tabRuns[nextRun] == 0) {
            return tabCount - 1;
        }
        return tabRuns[nextRun]-1;
    }

    /**
     * Returns the tab run overlay.
     * @param tabPlacement the placement (left, right, bottom, top) of the tab
     * @return the tab run overlay
     */
    protected int getTabRunOverlay(int tabPlacement) {
        return tabRunOverlay;
    }

    /**
     * Returns the tab run indent.
     * @param tabPlacement the placement (left, right, bottom, top) of the tab
     * @param run the tab run
     * @return the tab run indent
     */
    protected int getTabRunIndent(int tabPlacement, int run) {
        return 0;
    }

    /**
     * Returns whether or not the tab run should be padded.
     * @param tabPlacement the placement (left, right, bottom, top) of the tab
     * @param run the tab run
     * @return whether or not the tab run should be padded
     */
    protected boolean shouldPadTabRun(int tabPlacement, int run) {
        return runCount > 1;
    }

    /**
     * Returns whether or not the tab run should be rotated.
     * @param tabPlacement the placement (left, right, bottom, top) of the tab
     * @return whether or not the tab run should be rotated
     */
    protected boolean shouldRotateTabRuns(int tabPlacement) {
        return true;
    }

    /**
     * Returns the icon for a tab.
     * @param tabIndex the index of the tab
     * @return the icon for a tab
     */
    protected Icon getIconForTab(int tabIndex) {
        return (!tabPane.isEnabled() || !tabPane.isEnabledAt(tabIndex))?
                          tabPane.getDisabledIconAt(tabIndex) : tabPane.getIconAt(tabIndex);
    }

    /**
     * Returns the text View object required to render stylized text (HTML) for
     * the specified tab or null if no specialized text rendering is needed
     * for this tab. This is provided to support html rendering inside tabs.
     *
     * @param tabIndex the index of the tab
     * @return the text view to render the tab's text or null if no
     *         specialized rendering is required
     *
     * @since 1.4
     */
    protected View getTextViewForTab(int tabIndex) {
        if (htmlViews != null) {
            return htmlViews.elementAt(tabIndex);
        }
        return null;
    }

    /**
     * Calculates the tab height.
     * @param tabPlacement  the placement (left, right, bottom, top) of the tab
     * @param tabIndex      the index of the tab with respect to other tabs
     * @param fontHeight    the font height
     * @return the tab height
     */
    protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
        int height = 0;
        Component c = tabPane.getTabComponentAt(tabIndex);
        if (c != null) {
            height = c.getPreferredSize().height;
        } else {
            View v = getTextViewForTab(tabIndex);
            if (v != null) {
                // html
                height += (int) v.getPreferredSpan(View.Y_AXIS);
            } else {
                // plain text
                height += fontHeight;
            }
            Icon icon = getIconForTab(tabIndex);

            if (icon != null) {
                height = Math.max(height, icon.getIconHeight());
            }
        }
        Insets tabInsets = getTabInsets(tabPlacement, tabIndex);
        height += tabInsets.top + tabInsets.bottom + 2;
        return height;
    }

    /**
     * Calculates the maximum tab height.
     * @param tabPlacement  the placement (left, right, bottom, top) of the tab
     * @return the maximum tab height
     */
    protected int calculateMaxTabHeight(int tabPlacement) {
        FontMetrics metrics = getFontMetrics();
        int tabCount = tabPane.getTabCount();
        int result = 0;
        int fontHeight = metrics.getHeight();
        for(int i = 0; i < tabCount; i++) {
            result = Math.max(calculateTabHeight(tabPlacement, i, fontHeight), result);
        }
        return result;
    }

    /**
     * Calculates the tab width.
     * @param tabPlacement  the placement (left, right, bottom, top) of the tab
     * @param tabIndex      the index of the tab with respect to other tabs
     * @param metrics       the font metrics
     * @return the tab width
     */
    protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
        Insets tabInsets = getTabInsets(tabPlacement, tabIndex);
        int width = tabInsets.left + tabInsets.right + 3;
        Component tabComponent = tabPane.getTabComponentAt(tabIndex);
        if (tabComponent != null) {
            width += tabComponent.getPreferredSize().width;
        } else {
            Icon icon = getIconForTab(tabIndex);
            if (icon != null) {
                width += icon.getIconWidth() + textIconGap;
            }
            View v = getTextViewForTab(tabIndex);
            if (v != null) {
                // html
                width += (int) v.getPreferredSpan(View.X_AXIS);
            } else {
                // plain text
                String title = tabPane.getTitleAt(tabIndex);
                width += SwingUtilities2.stringWidth(tabPane, metrics, title);
            }
        }
        return width;
    }

    /**
     * Calculates the maximum tab width.
     * @param tabPlacement  the placement (left, right, bottom, top) of the tab
     * @return the maximum tab width
     */
    protected int calculateMaxTabWidth(int tabPlacement) {
        FontMetrics metrics = getFontMetrics();
        int tabCount = tabPane.getTabCount();
        int result = 0;
        for(int i = 0; i < tabCount; i++) {
            result = Math.max(calculateTabWidth(tabPlacement, i, metrics), result);
        }
        return result;
    }

    /**
     * Calculates the tab area height.
     * @param tabPlacement  the placement (left, right, bottom, top) of the tab
     * @param horizRunCount horizontal run count
     * @param maxTabHeight maximum tab height
     * @return the tab area height
     */
    protected int calculateTabAreaHeight(int tabPlacement, int horizRunCount, int maxTabHeight) {
        Insets tabAreaInsets = getTabAreaInsets(tabPlacement);
        int tabRunOverlay = getTabRunOverlay(tabPlacement);
        return (horizRunCount > 0?
                horizRunCount * (maxTabHeight-tabRunOverlay) + tabRunOverlay +
                tabAreaInsets.top + tabAreaInsets.bottom :
                0);
    }

    /**
     * Calculates the tab area width.
     * @param tabPlacement  the placement (left, right, bottom, top) of the tab
     * @param vertRunCount vertical run count
     * @param maxTabWidth maximum tab width
     * @return the tab area width
     */
    protected int calculateTabAreaWidth(int tabPlacement, int vertRunCount, int maxTabWidth) {
        Insets tabAreaInsets = getTabAreaInsets(tabPlacement);
        int tabRunOverlay = getTabRunOverlay(tabPlacement);
        return (vertRunCount > 0?
                vertRunCount * (maxTabWidth-tabRunOverlay) + tabRunOverlay +
                tabAreaInsets.left + tabAreaInsets.right :
                0);
    }

    /**
     * Returns the tab insets.
     * @param tabPlacement  the placement (left, right, bottom, top) of the tab
     * @param tabIndex the tab index
     * @return the tab insets
     */
    protected Insets getTabInsets(int tabPlacement, int tabIndex) {
        return tabInsets;
    }

    /**
     * Returns the selected tab pad insets.
     * @param tabPlacement  the placement (left, right, bottom, top) of the tab
     * @return the selected tab pad insets
     */
    protected Insets getSelectedTabPadInsets(int tabPlacement) {
        rotateInsets(selectedTabPadInsets, currentPadInsets, tabPlacement);
        return currentPadInsets;
    }

    /**
     * Returns the tab area insets.
     * @param tabPlacement  the placement (left, right, bottom, top) of the tab
     * @return the pad area insets
     */
    protected Insets getTabAreaInsets(int tabPlacement) {
        rotateInsets(tabAreaInsets, currentTabAreaInsets, tabPlacement);
        return currentTabAreaInsets;
    }

    /**
     * Returns the content border insets.
     * @param tabPlacement  the placement (left, right, bottom, top) of the tab
     * @return the content border insets
     */
    protected Insets getContentBorderInsets(int tabPlacement) {
        return contentBorderInsets;
    }

    /**
     * Returns the font metrics.
     * @return the font metrics
     */
    protected FontMetrics getFontMetrics() {
        Font font = tabPane.getFont();
        return tabPane.getFontMetrics(font);
    }


// Tab Navigation methods

    /**
     * Navigate the selected tab.
     * @param direction the direction
     */
    protected void navigateSelectedTab(int direction) {
        int tabPlacement = tabPane.getTabPlacement();
        int current = DefaultLookup.getBoolean(tabPane, this,
                             "TabbedPane.selectionFollowsFocus", true) ?
                             tabPane.getSelectedIndex() : getFocusIndex();
        int tabCount = tabPane.getTabCount();
        boolean leftToRight = BasicGraphicsUtils.isLeftToRight(tabPane);

        // If we have no tabs then don't navigate.
        if (tabCount <= 0) {
            return;
        }

        int offset;
        switch(tabPlacement) {
          case LEFT:
          case RIGHT:
              switch(direction) {
                 case NEXT:
                     selectNextTab(current);
                     break;
                 case PREVIOUS:
                     selectPreviousTab(current);
                     break;
                case NORTH:
                    selectPreviousTabInRun(current);
                    break;
                case SOUTH:
                    selectNextTabInRun(current);
                    break;
                case WEST:
                    offset = getTabRunOffset(tabPlacement, tabCount, current, false);
                    selectAdjacentRunTab(tabPlacement, current, offset);
                    break;
                case EAST:
                    offset = getTabRunOffset(tabPlacement, tabCount, current, true);
                    selectAdjacentRunTab(tabPlacement, current, offset);
                    break;
                default:
              }
              break;
          case BOTTOM:
          case TOP:
          default:
              switch(direction) {
                case NEXT:
                    selectNextTab(current);
                    break;
                case PREVIOUS:
                    selectPreviousTab(current);
                    break;
                case NORTH:
                    offset = getTabRunOffset(tabPlacement, tabCount, current, false);
                    selectAdjacentRunTab(tabPlacement, current, offset);
                    break;
                case SOUTH:
                    offset = getTabRunOffset(tabPlacement, tabCount, current, true);
                    selectAdjacentRunTab(tabPlacement, current, offset);
                    break;
                case EAST:
                    if (leftToRight) {
                        selectNextTabInRun(current);
                    } else {
                        selectPreviousTabInRun(current);
                    }
                    break;
                case WEST:
                    if (leftToRight) {
                        selectPreviousTabInRun(current);
                    } else {
                        selectNextTabInRun(current);
                    }
                    break;
                default:
              }
        }
    }

    /**
     * Select the next tab in the run.
     * @param current the current tab
     */
    protected void selectNextTabInRun(int current) {
        int tabCount = tabPane.getTabCount();
        int tabIndex = getNextTabIndexInRun(tabCount, current);

        while(tabIndex != current && !tabPane.isEnabledAt(tabIndex)) {
            tabIndex = getNextTabIndexInRun(tabCount, tabIndex);
        }
        navigateTo(tabIndex);
    }

    /**
     * Select the previous tab in the run.
     * @param current the current tab
     */
    protected void selectPreviousTabInRun(int current) {
        int tabCount = tabPane.getTabCount();
        int tabIndex = getPreviousTabIndexInRun(tabCount, current);

        while(tabIndex != current && !tabPane.isEnabledAt(tabIndex)) {
            tabIndex = getPreviousTabIndexInRun(tabCount, tabIndex);
        }
        navigateTo(tabIndex);
    }

    /**
     * Select the next tab.
     * @param current the current tab
     */
    protected void selectNextTab(int current) {
        int tabIndex = getNextTabIndex(current);

        while (tabIndex != current && !tabPane.isEnabledAt(tabIndex)) {
            tabIndex = getNextTabIndex(tabIndex);
        }
        navigateTo(tabIndex);
    }

    /**
     * Select the previous tab.
     * @param current the current tab
     */
    protected void selectPreviousTab(int current) {
        int tabIndex = getPreviousTabIndex(current);

        while (tabIndex != current && !tabPane.isEnabledAt(tabIndex)) {
            tabIndex = getPreviousTabIndex(tabIndex);
        }
        navigateTo(tabIndex);
    }

    /**
     * Selects an adjacent run of tabs.
     * @param tabPlacement  the placement (left, right, bottom, top) of the tab
     * @param tabIndex      the index of the tab with respect to other tabs
     * @param offset        selection offset
     */
    protected void selectAdjacentRunTab(int tabPlacement,
                                        int tabIndex, int offset) {
        if ( runCount < 2 ) {
            return;
        }
        int newIndex;
        Rectangle r = rects[tabIndex];
        switch(tabPlacement) {
          case LEFT:
          case RIGHT:
              newIndex = tabForCoordinate(tabPane, r.x + r.width/2 + offset,
                                       r.y + r.height/2);
              break;
          case BOTTOM:
          case TOP:
          default:
              newIndex = tabForCoordinate(tabPane, r.x + r.width/2,
                                       r.y + r.height/2 + offset);
        }
        if (newIndex != -1) {
            while (!tabPane.isEnabledAt(newIndex) && newIndex != tabIndex) {
                newIndex = getNextTabIndex(newIndex);
            }
            navigateTo(newIndex);
        }
    }

    private void navigateTo(int index) {
        if (DefaultLookup.getBoolean(tabPane, this,
                             "TabbedPane.selectionFollowsFocus", true)) {
            tabPane.setSelectedIndex(index);
        } else {
            // Just move focus (not selection)
            setFocusIndex(index, true);
        }
    }

    void setFocusIndex(int index, boolean repaint) {
        if (repaint && !isRunsDirty) {
            repaintTab(focusIndex);
            focusIndex = index;
            repaintTab(focusIndex);
        }
        else {
            focusIndex = index;
        }
    }

    /**
     * Repaints the specified tab.
     */
    private void repaintTab(int index) {
        // If we're not valid that means we will shortly be validated and
        // painted, which means we don't have to do anything here.
        if (!isRunsDirty && index >= 0 && index < tabPane.getTabCount()) {
            tabPane.repaint(getTabBounds(tabPane, index));
        }
    }

    /**
     * Makes sure the focusIndex is valid.
     */
    private void validateFocusIndex() {
        if (focusIndex >= tabPane.getTabCount()) {
            setFocusIndex(tabPane.getSelectedIndex(), false);
        }
    }

    /**
     * Returns the index of the tab that has focus.
     *
     * @return index of tab that has focus
     * @since 1.5
     */
    protected int getFocusIndex() {
        return focusIndex;
    }

    /**
     * Returns the tab run offset.
     * @param tabPlacement  the placement (left, right, bottom, top) of the tab
     * @param tabCount the tab count
     * @param tabIndex      the index of the tab with respect to other tabs
     * @param forward forward or not
     * @return the tab run offset
     */
    protected int getTabRunOffset(int tabPlacement, int tabCount,
                                  int tabIndex, boolean forward) {
        int run = getRunForTab(tabCount, tabIndex);
        int offset;
        switch(tabPlacement) {
          case LEFT: {
              if (run == 0) {
                  offset = (forward?
                            -(calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth)-maxTabWidth) :
                            -maxTabWidth);

              } else if (run == runCount - 1) {
                  offset = (forward?
                            maxTabWidth :
                            calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth)-maxTabWidth);
              } else {
                  offset = (forward? maxTabWidth : -maxTabWidth);
              }
              break;
          }
          case RIGHT: {
              if (run == 0) {
                  offset = (forward?
                            maxTabWidth :
                            calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth)-maxTabWidth);
              } else if (run == runCount - 1) {
                  offset = (forward?
                            -(calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth)-maxTabWidth) :
                            -maxTabWidth);
              } else {
                  offset = (forward? maxTabWidth : -maxTabWidth);
              }
              break;
          }
          case BOTTOM: {
              if (run == 0) {
                  offset = (forward?
                            maxTabHeight :
                            calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight)-maxTabHeight);
              } else if (run == runCount - 1) {
                  offset = (forward?
                            -(calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight)-maxTabHeight) :
                            -maxTabHeight);
              } else {
                  offset = (forward? maxTabHeight : -maxTabHeight);
              }
              break;
          }
          case TOP:
          default: {
              if (run == 0) {
                  offset = (forward?
                            -(calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight)-maxTabHeight) :
                            -maxTabHeight);
              } else if (run == runCount - 1) {
                  offset = (forward?
                            maxTabHeight :
                            calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight)-maxTabHeight);
              } else {
                  offset = (forward? maxTabHeight : -maxTabHeight);
              }
          }
        }
        return offset;
    }

    /**
     * Returns the previous tab index.
     * @param base the base
     * @return the previous tab index
     */
    protected int getPreviousTabIndex(int base) {
        int tabIndex = (base - 1 >= 0? base - 1 : tabPane.getTabCount() - 1);
        return (tabIndex >= 0? tabIndex : 0);
    }

    /**
     * Returns the next tab index.
     * @param base the base
     * @return the next tab index
     */
    protected int getNextTabIndex(int base) {
        return (base+1)%tabPane.getTabCount();
    }

    /**
     * Returns the next tab index in the run.
     * @param tabCount the tab count
     * @param base the base
     * @return the next tab index in the run
     */
    protected int getNextTabIndexInRun(int tabCount, int base) {
        if (runCount < 2) {
            return getNextTabIndex(base);
        }
        int currentRun = getRunForTab(tabCount, base);
        int next = getNextTabIndex(base);
        if (next == tabRuns[getNextTabRun(currentRun)]) {
            return tabRuns[currentRun];
        }
        return next;
    }

    /**
     * Returns the previous tab index in the run.
     * @param tabCount the tab count
     * @param base the base
     * @return the previous tab index in the run
     */
    protected int getPreviousTabIndexInRun(int tabCount, int base) {
        if (runCount < 2) {
            return getPreviousTabIndex(base);
        }
        int currentRun = getRunForTab(tabCount, base);
        if (base == tabRuns[currentRun]) {
            int previous = tabRuns[getNextTabRun(currentRun)]-1;
            return (previous != -1? previous : tabCount-1);
        }
        return getPreviousTabIndex(base);
    }

    /**
     * Returns the previous tab run.
     * @param baseRun the base run
     * @return the previous tab run
     */
    protected int getPreviousTabRun(int baseRun) {
        int runIndex = (baseRun - 1 >= 0? baseRun - 1 : runCount - 1);
        return (runIndex >= 0? runIndex : 0);
    }

    /**
     * Returns the next tab run.
     * @param baseRun the base run
     * @return the next tab run
     */
    protected int getNextTabRun(int baseRun) {
        return (baseRun+1)%runCount;
    }

    /**
     * Rotates the insets.
     * @param topInsets the top insets
     * @param targetInsets the target insets
     * @param targetPlacement the target placement
     */
    protected static void rotateInsets(Insets topInsets, Insets targetInsets, int targetPlacement) {

        switch(targetPlacement) {
          case LEFT:
              targetInsets.top = topInsets.left;
              targetInsets.left = topInsets.top;
              targetInsets.bottom = topInsets.right;
              targetInsets.right = topInsets.bottom;
              break;
          case BOTTOM:
              targetInsets.top = topInsets.bottom;
              targetInsets.left = topInsets.left;
              targetInsets.bottom = topInsets.top;
              targetInsets.right = topInsets.right;
              break;
          case RIGHT:
              targetInsets.top = topInsets.left;
              targetInsets.left = topInsets.bottom;
              targetInsets.bottom = topInsets.right;
              targetInsets.right = topInsets.top;
              break;
          case TOP:
          default:
              targetInsets.top = topInsets.top;
              targetInsets.left = topInsets.left;
              targetInsets.bottom = topInsets.bottom;
              targetInsets.right = topInsets.right;
        }
    }

    // REMIND(aim,7/29/98): This method should be made
    // protected in the next release where
    // API changes are allowed
    boolean requestFocusForVisibleComponent() {
        return SwingUtilities2.tabbedPaneChangeFocusTo(getVisibleComponent());
    }

    private static class Actions extends UIAction {
        static final String NEXT = "navigateNext";
        static final String PREVIOUS = "navigatePrevious";
        static final String RIGHT = "navigateRight";
        static final String LEFT = "navigateLeft";
        static final String UP = "navigateUp";
        static final String DOWN = "navigateDown";
        static final String PAGE_UP = "navigatePageUp";
        static final String PAGE_DOWN = "navigatePageDown";
        static final String REQUEST_FOCUS = "requestFocus";
        static final String REQUEST_FOCUS_FOR_VISIBLE =
                                    "requestFocusForVisibleComponent";
        static final String SET_SELECTED = "setSelectedIndex";
        static final String SELECT_FOCUSED = "selectTabWithFocus";
        static final String SCROLL_FORWARD = "scrollTabsForwardAction";
        static final String SCROLL_BACKWARD = "scrollTabsBackwardAction";

        Actions(String key) {
            super(key);
        }

        public void actionPerformed(ActionEvent e) {
            String key = getName();
            JTabbedPane pane = (JTabbedPane)e.getSource();
            BasicTabbedPaneUI ui = (BasicTabbedPaneUI)BasicLookAndFeel.
                       getUIOfType(pane.getUI(), BasicTabbedPaneUI.class);

            if (ui == null) {
                return;
            }
            if (key == NEXT) {
                ui.navigateSelectedTab(SwingConstants.NEXT);
            }
            else if (key == PREVIOUS) {
                ui.navigateSelectedTab(SwingConstants.PREVIOUS);
            }
            else if (key == RIGHT) {
                ui.navigateSelectedTab(SwingConstants.EAST);
            }
            else if (key == LEFT) {
                ui.navigateSelectedTab(SwingConstants.WEST);
            }
            else if (key == UP) {
                ui.navigateSelectedTab(SwingConstants.NORTH);
            }
            else if (key == DOWN) {
                ui.navigateSelectedTab(SwingConstants.SOUTH);
            }
            else if (key == PAGE_UP) {
                int tabPlacement = pane.getTabPlacement();
                if (tabPlacement == TOP|| tabPlacement == BOTTOM) {
                    ui.navigateSelectedTab(SwingConstants.WEST);
                } else {
                    ui.navigateSelectedTab(SwingConstants.NORTH);
                }
            }
            else if (key == PAGE_DOWN) {
                int tabPlacement = pane.getTabPlacement();
                if (tabPlacement == TOP || tabPlacement == BOTTOM) {
                    ui.navigateSelectedTab(SwingConstants.EAST);
                } else {
                    ui.navigateSelectedTab(SwingConstants.SOUTH);
                }
            }
            else if (key == REQUEST_FOCUS) {
                pane.requestFocus();
            }
            else if (key == REQUEST_FOCUS_FOR_VISIBLE) {
                ui.requestFocusForVisibleComponent();
            }
            else if (key == SET_SELECTED) {
                String command = e.getActionCommand();

                if (command != null && command.length() > 0) {
                    int mnemonic = (int)e.getActionCommand().charAt(0);
                    if (mnemonic >= 'a' && mnemonic <='z') {
                        mnemonic  -= ('a' - 'A');
                    }
                    Integer index = ui.mnemonicToIndexMap.get(Integer.valueOf(mnemonic));
                    if (index != null && pane.isEnabledAt(index.intValue())) {
                        pane.setSelectedIndex(index.intValue());
                    }
                }
            }
            else if (key == SELECT_FOCUSED) {
                int focusIndex = ui.getFocusIndex();
                if (focusIndex != -1) {
                    pane.setSelectedIndex(focusIndex);
                }
            }
            else if (key == SCROLL_FORWARD) {
                if (ui.scrollableTabLayoutEnabled()) {
                    ui.tabScroller.scrollForward(pane.getTabPlacement());
                }
            }
            else if (key == SCROLL_BACKWARD) {
                if (ui.scrollableTabLayoutEnabled()) {
                    ui.tabScroller.scrollBackward(pane.getTabPlacement());
                }
            }
        }
    }

    /**
     * This class should be treated as a &quot;protected&quot; inner class.
     * Instantiate it only within subclasses of BasicTabbedPaneUI.
     */
    public class TabbedPaneLayout implements LayoutManager {
        /**
         * Constructs a {@code TabbedPaneLayout}.
         */
        public TabbedPaneLayout() {}

        public void addLayoutComponent(String name, Component comp) {}

        public void removeLayoutComponent(Component comp) {}

        public Dimension preferredLayoutSize(Container parent) {
            return calculateSize(false);
        }

        public Dimension minimumLayoutSize(Container parent) {
            return calculateSize(true);
        }

        /**
         * Returns the calculated size.
         * @param minimum use the minimum size or preferred size
         * @return the calculated size
         */
        protected Dimension calculateSize(boolean minimum) {
            int tabPlacement = tabPane.getTabPlacement();
            Insets insets = tabPane.getInsets();
            Insets contentInsets = getContentBorderInsets(tabPlacement);
            Insets tabAreaInsets = getTabAreaInsets(tabPlacement);

            Dimension zeroSize = new Dimension(0,0);
            int height = 0;
            int width = 0;
            int cWidth = 0;
            int cHeight = 0;

            // Determine minimum size required to display largest
            // child in each dimension
            //
            for (int i = 0; i < tabPane.getTabCount(); i++) {
                Component component = tabPane.getComponentAt(i);
                if (component != null) {
                    Dimension size = minimum ? component.getMinimumSize() :
                                component.getPreferredSize();

                    if (size != null) {
                        cHeight = Math.max(size.height, cHeight);
                        cWidth = Math.max(size.width, cWidth);
                    }
                }
            }
            // Add content border insets to minimum size
            width += cWidth;
            height += cHeight;
            int tabExtent;

            // Calculate how much space the tabs will need, based on the
            // minimum size required to display largest child + content border
            //
            switch(tabPlacement) {
              case LEFT:
              case RIGHT:
                  height = Math.max(height, calculateMaxTabHeight(tabPlacement));
                  tabExtent = preferredTabAreaWidth(tabPlacement, height - tabAreaInsets.top - tabAreaInsets.bottom);
                  width += tabExtent;
                  break;
              case TOP:
              case BOTTOM:
              default:
                  width = Math.max(width, calculateMaxTabWidth(tabPlacement));
                  tabExtent = preferredTabAreaHeight(tabPlacement, width - tabAreaInsets.left - tabAreaInsets.right);
                  height += tabExtent;
            }
            return new Dimension(width + insets.left + insets.right + contentInsets.left + contentInsets.right,
                             height + insets.bottom + insets.top + contentInsets.top + contentInsets.bottom);

        }

        /**
         * Returns the preferred tab area height.
         * @param tabPlacement the tab placement
         * @param width the width
         * @return the preferred tab area height
         */
        protected int preferredTabAreaHeight(int tabPlacement, int width) {
            FontMetrics metrics = getFontMetrics();
            int tabCount = tabPane.getTabCount();
            int total = 0;
            if (tabCount > 0) {
                int rows = 1;
                int x = 0;

                int maxTabHeight = calculateMaxTabHeight(tabPlacement);

                for (int i = 0; i < tabCount; i++) {
                    int tabWidth = calculateTabWidth(tabPlacement, i, metrics);

                    if (x != 0 && x + tabWidth > width) {
                        rows++;
                        x = 0;
                    }
                    x += tabWidth;
                }
                total = calculateTabAreaHeight(tabPlacement, rows, maxTabHeight);
            }
            return total;
        }

        /**
         * Returns the preferred tab area width.
         * @param tabPlacement the tab placement
         * @param height the height
         * @return the preferred tab area widty
         */
        protected int preferredTabAreaWidth(int tabPlacement, int height) {
            FontMetrics metrics = getFontMetrics();
            int tabCount = tabPane.getTabCount();
            int total = 0;
            if (tabCount > 0) {
                int columns = 1;
                int y = 0;
                int fontHeight = metrics.getHeight();

                maxTabWidth = calculateMaxTabWidth(tabPlacement);

                for (int i = 0; i < tabCount; i++) {
                    int tabHeight = calculateTabHeight(tabPlacement, i, fontHeight);

                    if (y != 0 && y + tabHeight > height) {
                        columns++;
                        y = 0;
                    }
                    y += tabHeight;
                }
                total = calculateTabAreaWidth(tabPlacement, columns, maxTabWidth);
            }
            return total;
        }

        /** {@inheritDoc} */
        @SuppressWarnings("deprecation")
        public void layoutContainer(Container parent) {
            /* Some of the code in this method deals with changing the
            * visibility of components to hide and show the contents for the
            * selected tab. This is older code that has since been duplicated
            * in JTabbedPane.fireStateChanged(), so as to allow visibility
            * changes to happen sooner (see the note there). This code remains
            * for backward compatibility as there are some cases, such as
            * subclasses that don't fireStateChanged() where it may be used.
            * Any changes here need to be kept in synch with
            * JTabbedPane.fireStateChanged().
            */

            setRolloverTab(-1);

            int tabPlacement = tabPane.getTabPlacement();
            Insets insets = tabPane.getInsets();
            int selectedIndex = tabPane.getSelectedIndex();
            Component visibleComponent = getVisibleComponent();

            calculateLayoutInfo();

            Component selectedComponent = null;
            if (selectedIndex < 0) {
                if (visibleComponent != null) {
                    // The last tab was removed, so remove the component
                    setVisibleComponent(null);
                }
            } else {
                selectedComponent = tabPane.getComponentAt(selectedIndex);
            }
            int cx, cy, cw, ch;
            int totalTabWidth = 0;
            int totalTabHeight = 0;
            Insets contentInsets = getContentBorderInsets(tabPlacement);

            boolean shouldChangeFocus = false;

            // In order to allow programs to use a single component
            // as the display for multiple tabs, we will not change
            // the visible compnent if the currently selected tab
            // has a null component.  This is a bit dicey, as we don't
            // explicitly state we support this in the spec, but since
            // programs are now depending on this, we're making it work.
            //
            if(selectedComponent != null) {
                if(selectedComponent != visibleComponent &&
                        visibleComponent != null) {
                    if(SwingUtilities.findFocusOwner(visibleComponent) != null) {
                        shouldChangeFocus = true;
                    }
                }
                setVisibleComponent(selectedComponent);
            }

            Rectangle bounds = tabPane.getBounds();
            int numChildren = tabPane.getComponentCount();

            if(numChildren > 0) {

                switch(tabPlacement) {
                    case LEFT:
                        totalTabWidth = calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
                        cx = insets.left + totalTabWidth + contentInsets.left;
                        cy = insets.top + contentInsets.top;
                        break;
                    case RIGHT:
                        totalTabWidth = calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
                        cx = insets.left + contentInsets.left;
                        cy = insets.top + contentInsets.top;
                        break;
                    case BOTTOM:
                        totalTabHeight = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
                        cx = insets.left + contentInsets.left;
                        cy = insets.top + contentInsets.top;
                        break;
                    case TOP:
                    default:
                        totalTabHeight = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
                        cx = insets.left + contentInsets.left;
                        cy = insets.top + totalTabHeight + contentInsets.top;
                }

                cw = bounds.width - totalTabWidth -
                        insets.left - insets.right -
                        contentInsets.left - contentInsets.right;
                ch = bounds.height - totalTabHeight -
                        insets.top - insets.bottom -
                        contentInsets.top - contentInsets.bottom;

                for(int i = 0; i < numChildren; i++) {
                    Component child = tabPane.getComponent(i);
                    if(child == tabContainer) {

                        int tabContainerWidth = totalTabWidth == 0 ? bounds.width :
                                totalTabWidth + insets.left + insets.right +
                                        contentInsets.left + contentInsets.right;
                        int tabContainerHeight = totalTabHeight == 0 ? bounds.height :
                                totalTabHeight + insets.top + insets.bottom +
                                        contentInsets.top + contentInsets.bottom;

                        int tabContainerX = 0;
                        int tabContainerY = 0;
                        if(tabPlacement == BOTTOM) {
                            tabContainerY = bounds.height - tabContainerHeight;
                        } else if(tabPlacement == RIGHT) {
                            tabContainerX = bounds.width - tabContainerWidth;
                        }
                        child.setBounds(tabContainerX, tabContainerY, tabContainerWidth, tabContainerHeight);
                    } else {
                        child.setBounds(cx, cy, cw, ch);
                    }
                }
            }
            layoutTabComponents();
            if(shouldChangeFocus) {
                if(!requestFocusForVisibleComponent()) {
                    tabPane.requestFocus();
                }
            }
        }

        /**
         * Calculates the layout info.
         */
        public void calculateLayoutInfo() {
            int tabCount = tabPane.getTabCount();
            assureRectsCreated(tabCount);
            calculateTabRects(tabPane.getTabPlacement(), tabCount);
            isRunsDirty = false;
        }

        private void layoutTabComponents() {
            if (tabContainer == null) {
                return;
            }
            Rectangle rect = new Rectangle();
            Point delta = new Point(-tabContainer.getX(), -tabContainer.getY());
            if (scrollableTabLayoutEnabled()) {
                translatePointToTabPanel(0, 0, delta);
            }
            for (int i = 0; i < tabPane.getTabCount(); i++) {
                Component c = tabPane.getTabComponentAt(i);
                if (c == null) {
                    continue;
                }
                getTabBounds(i, rect);
                Dimension preferredSize = c.getPreferredSize();
                Insets insets = getTabInsets(tabPane.getTabPlacement(), i);
                int outerX = rect.x + insets.left + delta.x;
                int outerY = rect.y + insets.top + delta.y;
                int outerWidth = rect.width - insets.left - insets.right;
                int outerHeight = rect.height - insets.top - insets.bottom;
                //centralize component
                int x = outerX + (outerWidth - preferredSize.width) / 2;
                int y = outerY + (outerHeight - preferredSize.height) / 2;
                int tabPlacement = tabPane.getTabPlacement();
                boolean isSeleceted = i == tabPane.getSelectedIndex();
                c.setBounds(x + getTabLabelShiftX(tabPlacement, i, isSeleceted),
                            y + getTabLabelShiftY(tabPlacement, i, isSeleceted),
                        preferredSize.width, preferredSize.height);
            }
        }

        /**
         * Calculate the tab rectangles.
         * @param tabPlacement the tab placement
         * @param tabCount the tab count
         */
        protected void calculateTabRects(int tabPlacement, int tabCount) {
            FontMetrics metrics = getFontMetrics();
            Dimension size = tabPane.getSize();
            Insets insets = tabPane.getInsets();
            Insets tabAreaInsets = getTabAreaInsets(tabPlacement);
            int fontHeight = metrics.getHeight();
            int selectedIndex = tabPane.getSelectedIndex();
            int tabRunOverlay;
            int i, j;
            int x, y;
            int returnAt;
            boolean verticalTabRuns = (tabPlacement == LEFT || tabPlacement == RIGHT);
            boolean leftToRight = BasicGraphicsUtils.isLeftToRight(tabPane);

            //
            // Calculate bounds within which a tab run must fit
            //
            switch(tabPlacement) {
              case LEFT:
                  maxTabWidth = calculateMaxTabWidth(tabPlacement);
                  x = insets.left + tabAreaInsets.left;
                  y = insets.top + tabAreaInsets.top;
                  returnAt = size.height - (insets.bottom + tabAreaInsets.bottom);
                  break;
              case RIGHT:
                  maxTabWidth = calculateMaxTabWidth(tabPlacement);
                  x = size.width - insets.right - tabAreaInsets.right - maxTabWidth;
                  y = insets.top + tabAreaInsets.top;
                  returnAt = size.height - (insets.bottom + tabAreaInsets.bottom);
                  break;
              case BOTTOM:
                  maxTabHeight = calculateMaxTabHeight(tabPlacement);
                  x = insets.left + tabAreaInsets.left;
                  y = size.height - insets.bottom - tabAreaInsets.bottom - maxTabHeight;
                  returnAt = size.width - (insets.right + tabAreaInsets.right);
                  break;
              case TOP:
              default:
                  maxTabHeight = calculateMaxTabHeight(tabPlacement);
                  x = insets.left + tabAreaInsets.left;
                  y = insets.top + tabAreaInsets.top;
                  returnAt = size.width - (insets.right + tabAreaInsets.right);
                  break;
            }

            tabRunOverlay = getTabRunOverlay(tabPlacement);

            runCount = 0;
            selectedRun = -1;

            if (tabCount == 0) {
                return;
            }

            // Run through tabs and partition them into runs
            Rectangle rect;
            for (i = 0; i < tabCount; i++) {
                rect = rects[i];

                if (!verticalTabRuns) {
                    // Tabs on TOP or BOTTOM....
                    if (i > 0) {
                        rect.x = rects[i-1].x + rects[i-1].width;
                    } else {
                        tabRuns[0] = 0;
                        runCount = 1;
                        maxTabWidth = 0;
                        rect.x = x;
                    }
                    rect.width = calculateTabWidth(tabPlacement, i, metrics);
                    maxTabWidth = Math.max(maxTabWidth, rect.width);

                    // Never move a TAB down a run if it is in the first column.
                    // Even if there isn't enough room, moving it to a fresh
                    // line won't help.
                    if (rect.x != x && rect.x + rect.width > returnAt) {
                        if (runCount > tabRuns.length - 1) {
                            expandTabRunsArray();
                        }
                        tabRuns[runCount] = i;
                        runCount++;
                        rect.x = x;
                    }
                    // Initialize y position in case there's just one run
                    rect.y = y;
                    rect.height = maxTabHeight/* - 2*/;

                } else {
                    // Tabs on LEFT or RIGHT...
                    if (i > 0) {
                        rect.y = rects[i-1].y + rects[i-1].height;
                    } else {
                        tabRuns[0] = 0;
                        runCount = 1;
                        maxTabHeight = 0;
                        rect.y = y;
                    }
                    rect.height = calculateTabHeight(tabPlacement, i, fontHeight);
                    maxTabHeight = Math.max(maxTabHeight, rect.height);

                    // Never move a TAB over a run if it is in the first run.
                    // Even if there isn't enough room, moving it to a fresh
                    // column won't help.
                    if (rect.y != y && rect.y + rect.height > returnAt) {
                        if (runCount > tabRuns.length - 1) {
                            expandTabRunsArray();
                        }
                        tabRuns[runCount] = i;
                        runCount++;
                        rect.y = y;
                    }
                    // Initialize x position in case there's just one column
                    rect.x = x;
                    rect.width = maxTabWidth/* - 2*/;

                }
                if (i == selectedIndex) {
                    selectedRun = runCount - 1;
                }
            }

            if (runCount > 1) {
                // Re-distribute tabs in case last run has leftover space
                normalizeTabRuns(tabPlacement, tabCount, verticalTabRuns? y : x, returnAt);

                selectedRun = getRunForTab(tabCount, selectedIndex);

                // Rotate run array so that selected run is first
                if (shouldRotateTabRuns(tabPlacement)) {
                    rotateTabRuns(tabPlacement, selectedRun);
                }
            }

            // Step through runs from back to front to calculate
            // tab y locations and to pad runs appropriately
            for (i = runCount - 1; i >= 0; i--) {
                int start = tabRuns[i];
                int next = tabRuns[i == (runCount - 1)? 0 : i + 1];
                int end = (next != 0? next - 1 : tabCount - 1);
                if (!verticalTabRuns) {
                    for (j = start; j <= end; j++) {
                        rect = rects[j];
                        rect.y = y;
                        rect.x += getTabRunIndent(tabPlacement, i);
                    }
                    if (shouldPadTabRun(tabPlacement, i)) {
                        padTabRun(tabPlacement, start, end, returnAt);
                    }
                    if (tabPlacement == BOTTOM) {
                        y -= (maxTabHeight - tabRunOverlay);
                    } else {
                        y += (maxTabHeight - tabRunOverlay);
                    }
                } else {
                    for (j = start; j <= end; j++) {
                        rect = rects[j];
                        rect.x = x;
                        rect.y += getTabRunIndent(tabPlacement, i);
                    }
                    if (shouldPadTabRun(tabPlacement, i)) {
                        padTabRun(tabPlacement, start, end, returnAt);
                    }
                    if (tabPlacement == RIGHT) {
                        x -= (maxTabWidth - tabRunOverlay);
                    } else {
                        x += (maxTabWidth - tabRunOverlay);
                    }
                }
            }

            // Pad the selected tab so that it appears raised in front
            padSelectedTab(tabPlacement, selectedIndex);

            // if right to left and tab placement on the top or
            // the bottom, flip x positions and adjust by widths
            if (!leftToRight && !verticalTabRuns) {
                int rightMargin = size.width
                                  - (insets.right + tabAreaInsets.right);
                for (i = 0; i < tabCount; i++) {
                    rects[i].x = rightMargin - rects[i].x - rects[i].width;
                }
            }
        }


        /**
         * Rotates the run-index array so that the selected run is run[0].
         * @param tabPlacement the tab placement
         * @param selectedRun the selected run
         */
        protected void rotateTabRuns(int tabPlacement, int selectedRun) {
            for (int i = 0; i < selectedRun; i++) {
                int save = tabRuns[0];
                for (int j = 1; j < runCount; j++) {
                    tabRuns[j - 1] = tabRuns[j];
                }
                tabRuns[runCount-1] = save;
            }
        }

        /**
         * Normalizes the tab runs.
         * @param tabPlacement the tab placement
         * @param tabCount the tab count
         * @param start the start
         * @param max the max
         */
        protected void normalizeTabRuns(int tabPlacement, int tabCount,
                                     int start, int max) {
            boolean verticalTabRuns = (tabPlacement == LEFT || tabPlacement == RIGHT);
            int run = runCount - 1;
            boolean keepAdjusting = true;
            double weight = 1.25;

            // At this point the tab runs are packed to fit as many
            // tabs as possible, which can leave the last run with a lot
            // of extra space (resulting in very fat tabs on the last run).
            // So we'll attempt to distribute this extra space more evenly
            // across the runs in order to make the runs look more consistent.
            //
            // Starting with the last run, determine whether the last tab in
            // the previous run would fit (generously) in this run; if so,
            // move tab to current run and shift tabs accordingly.  Cycle
            // through remaining runs using the same algorithm.
            //
            while (keepAdjusting) {
                int last = lastTabInRun(tabCount, run);
                int prevLast = lastTabInRun(tabCount, run-1);
                int end;
                int prevLastLen;

                if (!verticalTabRuns) {
                    end = rects[last].x + rects[last].width;
                    prevLastLen = (int)(maxTabWidth*weight);
                } else {
                    end = rects[last].y + rects[last].height;
                    prevLastLen = (int)(maxTabHeight*weight*2);
                }

                // Check if the run has enough extra space to fit the last tab
                // from the previous row...
                if (max - end > prevLastLen) {

                    // Insert tab from previous row and shift rest over
                    tabRuns[run] = prevLast;
                    if (!verticalTabRuns) {
                        rects[prevLast].x = start;
                    } else {
                        rects[prevLast].y = start;
                    }
                    for (int i = prevLast+1; i <= last; i++) {
                        if (!verticalTabRuns) {
                            rects[i].x = rects[i-1].x + rects[i-1].width;
                        } else {
                            rects[i].y = rects[i-1].y + rects[i-1].height;
                        }
                    }

                } else if (run == runCount - 1) {
                    // no more room left in last run, so we're done!
                    keepAdjusting = false;
                }
                if (run - 1 > 0) {
                    // check previous run next...
                    run -= 1;
                } else {
                    // check last run again...but require a higher ratio
                    // of extraspace-to-tabsize because we don't want to
                    // end up with too many tabs on the last run!
                    run = runCount - 1;
                    weight += .25;
                }
            }
        }

        /**
         * Pads the tab run.
         * @param tabPlacement the tab placement
         * @param start the start
         * @param end the end
         * @param max the max
         */
        protected void padTabRun(int tabPlacement, int start, int end, int max) {
            Rectangle lastRect = rects[end];
            if (tabPlacement == TOP || tabPlacement == BOTTOM) {
                int runWidth = (lastRect.x + lastRect.width) - rects[start].x;
                int deltaWidth = max - (lastRect.x + lastRect.width);
                float factor = (float)deltaWidth / (float)runWidth;

                for (int j = start; j <= end; j++) {
                    Rectangle pastRect = rects[j];
                    if (j > start) {
                        pastRect.x = rects[j-1].x + rects[j-1].width;
                    }
                    pastRect.width += Math.round((float)pastRect.width * factor);
                }
                lastRect.width = max - lastRect.x;
            } else {
                int runHeight = (lastRect.y + lastRect.height) - rects[start].y;
                int deltaHeight = max - (lastRect.y + lastRect.height);
                float factor = (float)deltaHeight / (float)runHeight;

                for (int j = start; j <= end; j++) {
                    Rectangle pastRect = rects[j];
                    if (j > start) {
                        pastRect.y = rects[j-1].y + rects[j-1].height;
                    }
                    pastRect.height += Math.round((float)pastRect.height * factor);
                }
                lastRect.height = max - lastRect.y;
            }
        }

        /**
         * Pads selected tab.
         * @param tabPlacement the tab placement
         * @param selectedIndex the selected index
         */
        protected void padSelectedTab(int tabPlacement, int selectedIndex) {

            if (selectedIndex >= 0) {
                Rectangle selRect = rects[selectedIndex];
                Insets padInsets = getSelectedTabPadInsets(tabPlacement);
                selRect.x -= padInsets.left;
                selRect.width += (padInsets.left + padInsets.right);
                selRect.y -= padInsets.top;
                selRect.height += (padInsets.top + padInsets.bottom);

                if (!scrollableTabLayoutEnabled()) { // WRAP_TAB_LAYOUT
                    // do not expand selected tab more then necessary
                    Dimension size = tabPane.getSize();
                    Insets insets = tabPane.getInsets();

                    if ((tabPlacement == LEFT) || (tabPlacement == RIGHT)) {
                        int top = insets.top - selRect.y;
                        if (top > 0) {
                            selRect.y += top;
                            selRect.height -= top;
                        }
                        int bottom = (selRect.y + selRect.height) + insets.bottom - size.height;
                        if (bottom > 0) {
                            selRect.height -= bottom;
                        }
                    } else {
                        int left = insets.left - selRect.x;
                        if (left > 0) {
                            selRect.x += left;
                            selRect.width -= left;
                        }
                        int right = (selRect.x + selRect.width) + insets.right - size.width;
                        if (right > 0) {
                            selRect.width -= right;
                        }
                    }
                }
            }
        }
    }

    private class TabbedPaneScrollLayout extends TabbedPaneLayout {

        protected int preferredTabAreaHeight(int tabPlacement, int width) {
            return calculateTabAreaHeight(tabPlacement, 1, calculateMaxTabHeight(tabPlacement));
        }

        protected int preferredTabAreaWidth(int tabPlacement, int height) {
            return calculateTabAreaWidth(tabPlacement, 1, calculateMaxTabWidth(tabPlacement));
        }

        @SuppressWarnings("deprecation")
        public void layoutContainer(Container parent) {
            /* Some of the code in this method deals with changing the
             * visibility of components to hide and show the contents for the
             * selected tab. This is older code that has since been duplicated
             * in JTabbedPane.fireStateChanged(), so as to allow visibility
             * changes to happen sooner (see the note there). This code remains
             * for backward compatibility as there are some cases, such as
             * subclasses that don't fireStateChanged() where it may be used.
             * Any changes here need to be kept in synch with
             * JTabbedPane.fireStateChanged().
             */

            setRolloverTab(-1);

            int tabPlacement = tabPane.getTabPlacement();
            int tabCount = tabPane.getTabCount();
            Insets insets = tabPane.getInsets();
            int selectedIndex = tabPane.getSelectedIndex();
            Component visibleComponent = getVisibleComponent();

            calculateLayoutInfo();

            Component selectedComponent = null;
            if (selectedIndex < 0) {
                if (visibleComponent != null) {
                    // The last tab was removed, so remove the component
                    setVisibleComponent(null);
                }
            } else {
                selectedComponent = tabPane.getComponentAt(selectedIndex);
            }

            if (tabPane.getTabCount() == 0) {
                tabScroller.croppedEdge.resetParams();
                tabScroller.scrollForwardButton.setVisible(false);
                tabScroller.scrollBackwardButton.setVisible(false);
                return;
            }

            boolean shouldChangeFocus = false;

            // In order to allow programs to use a single component
            // as the display for multiple tabs, we will not change
            // the visible compnent if the currently selected tab
            // has a null component.  This is a bit dicey, as we don't
            // explicitly state we support this in the spec, but since
            // programs are now depending on this, we're making it work.
            //
            if(selectedComponent != null) {
                if(selectedComponent != visibleComponent &&
                        visibleComponent != null) {
                    if(SwingUtilities.findFocusOwner(visibleComponent) != null) {
                        shouldChangeFocus = true;
                    }
                }
                setVisibleComponent(selectedComponent);
            }
            int tx, ty, tw, th; // tab area bounds
            int cx, cy, cw, ch; // content area bounds
            Insets contentInsets = getContentBorderInsets(tabPlacement);
            Rectangle bounds = tabPane.getBounds();
            int numChildren = tabPane.getComponentCount();

            if(numChildren > 0) {
                switch(tabPlacement) {
                    case LEFT:
                        // calculate tab area bounds
                        tw = calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
                        th = bounds.height - insets.top - insets.bottom;
                        tx = insets.left;
                        ty = insets.top;

                        // calculate content area bounds
                        cx = tx + tw + contentInsets.left;
                        cy = ty + contentInsets.top;
                        cw = bounds.width - insets.left - insets.right - tw -
                                contentInsets.left - contentInsets.right;
                        ch = bounds.height - insets.top - insets.bottom -
                                contentInsets.top - contentInsets.bottom;
                        break;
                    case RIGHT:
                        // calculate tab area bounds
                        tw = calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
                        th = bounds.height - insets.top - insets.bottom;
                        tx = bounds.width - insets.right - tw;
                        ty = insets.top;

                        // calculate content area bounds
                        cx = insets.left + contentInsets.left;
                        cy = insets.top + contentInsets.top;
                        cw = bounds.width - insets.left - insets.right - tw -
                                contentInsets.left - contentInsets.right;
                        ch = bounds.height - insets.top - insets.bottom -
                                contentInsets.top - contentInsets.bottom;
                        break;
                    case BOTTOM:
                        // calculate tab area bounds
                        tw = bounds.width - insets.left - insets.right;
                        th = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
                        tx = insets.left;
                        ty = bounds.height - insets.bottom - th;

                        // calculate content area bounds
                        cx = insets.left + contentInsets.left;
                        cy = insets.top + contentInsets.top;
                        cw = bounds.width - insets.left - insets.right -
                                contentInsets.left - contentInsets.right;
                        ch = bounds.height - insets.top - insets.bottom - th -
                                contentInsets.top - contentInsets.bottom;
                        break;
                    case TOP:
                    default:
                        // calculate tab area bounds
                        tw = bounds.width - insets.left - insets.right;
                        th = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
                        tx = insets.left;
                        ty = insets.top;

                        // calculate content area bounds
                        cx = tx + contentInsets.left;
                        cy = ty + th + contentInsets.top;
                        cw = bounds.width - insets.left - insets.right -
                                contentInsets.left - contentInsets.right;
                        ch = bounds.height - insets.top - insets.bottom - th -
                                contentInsets.top - contentInsets.bottom;
                }

                for(int i = 0; i < numChildren; i++) {
                    Component child = tabPane.getComponent(i);

                    if(tabScroller != null && child == tabScroller.viewport) {
                        JViewport viewport = (JViewport) child;
                        Rectangle viewRect = viewport.getViewRect();
                        int vw = tw;
                        int vh = th;
                        Dimension butSize = tabScroller.scrollForwardButton.getPreferredSize();
                        switch(tabPlacement) {
                            case LEFT:
                            case RIGHT:
                                int totalTabHeight = rects[tabCount - 1].y + rects[tabCount - 1].height;
                                if(totalTabHeight > th) {
                                    // Allow space for scrollbuttons
                                    vh = (th > 2 * butSize.height) ? th - 2 * butSize.height : 0;
                                    if(totalTabHeight - viewRect.y <= vh) {
                                        // Scrolled to the end, so ensure the viewport size is
                                        // such that the scroll offset aligns with a tab
                                        vh = totalTabHeight - viewRect.y;
                                    }
                                }
                                break;
                            case BOTTOM:
                            case TOP:
                            default:
                                int totalTabWidth = rects[tabCount - 1].x + rects[tabCount - 1].width;
                                if(totalTabWidth > tw) {
                                    // Need to allow space for scrollbuttons
                                    vw = (tw > 2 * butSize.width) ? tw - 2 * butSize.width : 0;
                                    if(totalTabWidth - viewRect.x <= vw) {
                                        // Scrolled to the end, so ensure the viewport size is
                                        // such that the scroll offset aligns with a tab
                                        vw = totalTabWidth - viewRect.x;
                                    }
                                }
                        }
                        child.setBounds(tx, ty, vw, vh);

                    } else if(tabScroller != null &&
                            (child == tabScroller.scrollForwardButton ||
                            child == tabScroller.scrollBackwardButton)) {
                        Component scrollbutton = child;
                        Dimension bsize = scrollbutton.getPreferredSize();
                        int bx = 0;
                        int by = 0;
                        int bw = bsize.width;
                        int bh = bsize.height;
                        boolean visible = false;

                        switch(tabPlacement) {
                            case LEFT:
                            case RIGHT:
                                int totalTabHeight = rects[tabCount - 1].y + rects[tabCount - 1].height;
                                if(totalTabHeight > th) {
                                    visible = true;
                                    bx = (tabPlacement == LEFT ? tx + tw - bsize.width : tx);
                                    by = (child == tabScroller.scrollForwardButton) ?
                                            bounds.height - insets.bottom - bsize.height :
                                            bounds.height - insets.bottom - 2 * bsize.height;
                                }
                                break;

                            case BOTTOM:
                            case TOP:
                            default:
                                int totalTabWidth = rects[tabCount - 1].x + rects[tabCount - 1].width;

                                if(totalTabWidth > tw) {
                                    visible = true;
                                    bx = (child == tabScroller.scrollForwardButton) ?
                                            bounds.width - insets.left - bsize.width :
                                            bounds.width - insets.left - 2 * bsize.width;
                                    by = (tabPlacement == TOP ? ty + th - bsize.height : ty);
                                }
                        }
                        child.setVisible(visible);
                        if(visible) {
                            child.setBounds(bx, by, bw, bh);
                        }

                    } else {
                        // All content children...
                        child.setBounds(cx, cy, cw, ch);
                    }
                }
                super.layoutTabComponents();
                layoutCroppedEdge();
                if(shouldChangeFocus) {
                    if(!requestFocusForVisibleComponent()) {
                        tabPane.requestFocus();
                    }
                }
            }
        }

        private void layoutCroppedEdge() {
            tabScroller.croppedEdge.resetParams();
            Rectangle viewRect = tabScroller.viewport.getViewRect();
            int cropline;
            for (int i = 0; i < rects.length; i++) {
                Rectangle tabRect = rects[i];
                switch (tabPane.getTabPlacement()) {
                    case LEFT:
                    case RIGHT:
                        cropline = viewRect.y + viewRect.height;
                        if ((tabRect.y < cropline) && (tabRect.y + tabRect.height > cropline)) {
                            tabScroller.croppedEdge.setParams(i, cropline - tabRect.y - 1,
                                    -currentTabAreaInsets.left,  0);
                        }
                        break;
                    case TOP:
                    case BOTTOM:
                    default:
                        cropline = viewRect.x + viewRect.width;
                        if ((tabRect.x < cropline - 1) && (tabRect.x + tabRect.width > cropline)) {
                            tabScroller.croppedEdge.setParams(i, cropline - tabRect.x - 1,
                                    0, -currentTabAreaInsets.top);
                        }
                }
            }
        }

        protected void calculateTabRects(int tabPlacement, int tabCount) {
            FontMetrics metrics = getFontMetrics();
            Dimension size = tabPane.getSize();
            Insets insets = tabPane.getInsets();
            Insets tabAreaInsets = getTabAreaInsets(tabPlacement);
            int fontHeight = metrics.getHeight();
            int selectedIndex = tabPane.getSelectedIndex();
            int i;
            boolean verticalTabRuns = (tabPlacement == LEFT || tabPlacement == RIGHT);
            boolean leftToRight = BasicGraphicsUtils.isLeftToRight(tabPane);
            int x = tabAreaInsets.left;
            int y = tabAreaInsets.top;
            int totalWidth = 0;
            int totalHeight = 0;

            //
            // Calculate bounds within which a tab run must fit
            //
            switch(tabPlacement) {
              case LEFT:
              case RIGHT:
                  maxTabWidth = calculateMaxTabWidth(tabPlacement);
                  break;
              case BOTTOM:
              case TOP:
              default:
                  maxTabHeight = calculateMaxTabHeight(tabPlacement);
            }

            runCount = 0;
            selectedRun = -1;

            if (tabCount == 0) {
                return;
            }

            selectedRun = 0;
            runCount = 1;

            // Run through tabs and lay them out in a single run
            Rectangle rect;
            for (i = 0; i < tabCount; i++) {
                rect = rects[i];

                if (!verticalTabRuns) {
                    // Tabs on TOP or BOTTOM....
                    if (i > 0) {
                        rect.x = rects[i-1].x + rects[i-1].width;
                    } else {
                        tabRuns[0] = 0;
                        maxTabWidth = 0;
                        totalHeight += maxTabHeight;
                        rect.x = x;
                    }
                    rect.width = calculateTabWidth(tabPlacement, i, metrics);
                    totalWidth = rect.x + rect.width;
                    maxTabWidth = Math.max(maxTabWidth, rect.width);

                    rect.y = y;
                    rect.height = maxTabHeight/* - 2*/;

                } else {
                    // Tabs on LEFT or RIGHT...
                    if (i > 0) {
                        rect.y = rects[i-1].y + rects[i-1].height;
                    } else {
                        tabRuns[0] = 0;
                        maxTabHeight = 0;
                        totalWidth = maxTabWidth;
                        rect.y = y;
                    }
                    rect.height = calculateTabHeight(tabPlacement, i, fontHeight);
                    totalHeight = rect.y + rect.height;
                    maxTabHeight = Math.max(maxTabHeight, rect.height);

                    rect.x = x;
                    rect.width = maxTabWidth/* - 2*/;

                }
            }

            if (tabsOverlapBorder) {
                // Pad the selected tab so that it appears raised in front
                padSelectedTab(tabPlacement, selectedIndex);
            }

            // if right to left and tab placement on the top or
            // the bottom, flip x positions and adjust by widths
            if (!leftToRight && !verticalTabRuns) {
                int rightMargin = size.width
                                  - (insets.right + tabAreaInsets.right);
                for (i = 0; i < tabCount; i++) {
                    rects[i].x = rightMargin - rects[i].x - rects[i].width;
                }
            }
            tabScroller.tabPanel.setPreferredSize(new Dimension(totalWidth, totalHeight));
            tabScroller.tabPanel.invalidate();
        }
    }

    private class ScrollableTabSupport implements ActionListener,
                            ChangeListener {
        public ScrollableTabViewport viewport;
        public ScrollableTabPanel tabPanel;
        public JButton scrollForwardButton;
        public JButton scrollBackwardButton;
        public CroppedEdge croppedEdge;
        public int leadingTabIndex;

        private Point tabViewPosition = new Point(0,0);

        ScrollableTabSupport(int tabPlacement) {
            viewport = new ScrollableTabViewport();
            tabPanel = new ScrollableTabPanel();
            viewport.setView(tabPanel);
            viewport.addChangeListener(this);
            croppedEdge = new CroppedEdge();
            createButtons();
        }

        /**
         * Recreates the scroll buttons and adds them to the TabbedPane.
         */
        void createButtons() {
            if (scrollForwardButton != null) {
                tabPane.remove(scrollForwardButton);
                scrollForwardButton.removeActionListener(this);
                tabPane.remove(scrollBackwardButton);
                scrollBackwardButton.removeActionListener(this);
            }
            int tabPlacement = tabPane.getTabPlacement();
            if (tabPlacement == TOP || tabPlacement == BOTTOM) {
                scrollForwardButton = createScrollButton(EAST);
                scrollBackwardButton = createScrollButton(WEST);

            } else { // tabPlacement = LEFT || RIGHT
                scrollForwardButton = createScrollButton(SOUTH);
                scrollBackwardButton = createScrollButton(NORTH);
            }
            scrollForwardButton.addActionListener(this);
            scrollBackwardButton.addActionListener(this);
            tabPane.add(scrollForwardButton);
            tabPane.add(scrollBackwardButton);
        }

        public void scrollForward(int tabPlacement) {
            Dimension viewSize = viewport.getViewSize();
            Rectangle viewRect = viewport.getViewRect();

            if (tabPlacement == TOP || tabPlacement == BOTTOM) {
                if (viewRect.width >= viewSize.width - viewRect.x) {
                    return; // no room left to scroll
                }
            } else { // tabPlacement == LEFT || tabPlacement == RIGHT
                if (viewRect.height >= viewSize.height - viewRect.y) {
                    return;
                }
            }
            setLeadingTabIndex(tabPlacement, leadingTabIndex+1);
        }

        public void scrollBackward(int tabPlacement) {
            if (leadingTabIndex == 0) {
                return; // no room left to scroll
            }
            setLeadingTabIndex(tabPlacement, leadingTabIndex-1);
        }

        public void setLeadingTabIndex(int tabPlacement, int index) {
            leadingTabIndex = index;
            Dimension viewSize = viewport.getViewSize();
            Rectangle viewRect = viewport.getViewRect();

            switch(tabPlacement) {
              case TOP:
              case BOTTOM:
                tabViewPosition.x = leadingTabIndex == 0? 0 : rects[leadingTabIndex].x;

                if ((viewSize.width - tabViewPosition.x) < viewRect.width) {
                    // We've scrolled to the end, so adjust the viewport size
                    // to ensure the view position remains aligned on a tab boundary
                    Dimension extentSize = new Dimension(viewSize.width - tabViewPosition.x,
                                                         viewRect.height);
                    viewport.setExtentSize(extentSize);
                }
                break;
              case LEFT:
              case RIGHT:
                tabViewPosition.y = leadingTabIndex == 0? 0 : rects[leadingTabIndex].y;

                if ((viewSize.height - tabViewPosition.y) < viewRect.height) {
                // We've scrolled to the end, so adjust the viewport size
                // to ensure the view position remains aligned on a tab boundary
                     Dimension extentSize = new Dimension(viewRect.width,
                                                          viewSize.height - tabViewPosition.y);
                     viewport.setExtentSize(extentSize);
                }
            }
            viewport.setViewPosition(tabViewPosition);
        }

        public void stateChanged(ChangeEvent e) {
            updateView();
        }

        private void updateView() {
            int tabPlacement = tabPane.getTabPlacement();
            int tabCount = tabPane.getTabCount();
            assureRectsCreated(tabCount);
            Rectangle vpRect = viewport.getBounds();
            Dimension viewSize = viewport.getViewSize();
            Rectangle viewRect = viewport.getViewRect();

            leadingTabIndex = getClosestTab(viewRect.x, viewRect.y);

            // If the tab isn't right aligned, adjust it.
            if (leadingTabIndex + 1 < tabCount) {
                switch (tabPlacement) {
                case TOP:
                case BOTTOM:
                    if (rects[leadingTabIndex].x < viewRect.x) {
                        leadingTabIndex++;
                    }
                    break;
                case LEFT:
                case RIGHT:
                    if (rects[leadingTabIndex].y < viewRect.y) {
                        leadingTabIndex++;
                    }
                    break;
                }
            }
            Insets contentInsets = getContentBorderInsets(tabPlacement);
            switch(tabPlacement) {
              case LEFT:
                  tabPane.repaint(vpRect.x+vpRect.width, vpRect.y,
                                  contentInsets.left, vpRect.height);
                  scrollBackwardButton.setEnabled(
                          viewRect.y > 0 && leadingTabIndex > 0);
                  scrollForwardButton.setEnabled(
                          leadingTabIndex < tabCount-1 &&
                          viewSize.height-viewRect.y > viewRect.height);
                  break;
              case RIGHT:
                  tabPane.repaint(vpRect.x-contentInsets.right, vpRect.y,
                                  contentInsets.right, vpRect.height);
                  scrollBackwardButton.setEnabled(
                          viewRect.y > 0 && leadingTabIndex > 0);
                  scrollForwardButton.setEnabled(
                          leadingTabIndex < tabCount-1 &&
                          viewSize.height-viewRect.y > viewRect.height);
                  break;
              case BOTTOM:
                  tabPane.repaint(vpRect.x, vpRect.y-contentInsets.bottom,
                                  vpRect.width, contentInsets.bottom);
                  scrollBackwardButton.setEnabled(
                          viewRect.x > 0 && leadingTabIndex > 0);
                  scrollForwardButton.setEnabled(
                          leadingTabIndex < tabCount-1 &&
                          viewSize.width-viewRect.x > viewRect.width);
                  break;
              case TOP:
              default:
                  tabPane.repaint(vpRect.x, vpRect.y+vpRect.height,
                                  vpRect.width, contentInsets.top);
                  scrollBackwardButton.setEnabled(
                          viewRect.x > 0 && leadingTabIndex > 0);
                  scrollForwardButton.setEnabled(
                          leadingTabIndex < tabCount-1 &&
                          viewSize.width-viewRect.x > viewRect.width);
            }
        }

        /**
         * ActionListener for the scroll buttons.
         */
        public void actionPerformed(ActionEvent e) {
            ActionMap map = tabPane.getActionMap();

            if (map != null) {
                String actionKey;

                if (e.getSource() == scrollForwardButton) {
                    actionKey = "scrollTabsForwardAction";
                }
                else {
                    actionKey = "scrollTabsBackwardAction";
                }
                Action action = map.get(actionKey);

                if (action != null && action.isEnabled()) {
                    action.actionPerformed(new ActionEvent(tabPane,
                        ActionEvent.ACTION_PERFORMED, null, e.getWhen(),
                        e.getModifiers()));
                }
            }
        }

        public String toString() {
            return "viewport.viewSize=" + viewport.getViewSize() + "\n" +
                              "viewport.viewRectangle="+viewport.getViewRect()+"\n"+
                              "leadingTabIndex="+leadingTabIndex+"\n"+
                              "tabViewPosition=" + tabViewPosition;
        }

    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private class ScrollableTabViewport extends JViewport implements UIResource {
        public ScrollableTabViewport() {
            super();
            setName("TabbedPane.scrollableViewport");
            setScrollMode(SIMPLE_SCROLL_MODE);
            setOpaque(tabPane.isOpaque());
            Color bgColor = UIManager.getColor("TabbedPane.tabAreaBackground");
            if (bgColor == null) {
                bgColor = tabPane.getBackground();
            }
            setBackground(bgColor);
        }
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private class ScrollableTabPanel extends JPanel implements UIResource {
        public ScrollableTabPanel() {
            super(null);
            setOpaque(tabPane.isOpaque());
            Color background = tabPane.getBackground();
            Color tabAreaBackground = UIManager.getColor("TabbedPane.tabAreaBackground");
            if (background instanceof UIResource && tabAreaBackground != null) {
                setBackground(tabAreaBackground);
            } else {
                setBackground(background);
            }
        }
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            BasicTabbedPaneUI.this.paintTabArea(g, tabPane.getTabPlacement(),
                                                tabPane.getSelectedIndex());
            if (tabScroller.croppedEdge.isParamsSet() && tabContainer == null) {
                Rectangle croppedRect = rects[tabScroller.croppedEdge.getTabIndex()];
                g.translate(croppedRect.x, croppedRect.y);
                tabScroller.croppedEdge.paintComponent(g);
                g.translate(-croppedRect.x, -croppedRect.y);
            }
        }

        public void doLayout() {
            if (getComponentCount() > 0) {
                Component child = getComponent(0);
                child.setBounds(0, 0, getWidth(), getHeight());
            }
        }
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private class ScrollableTabButton extends BasicArrowButton implements UIResource,
                                                                            SwingConstants {
        public ScrollableTabButton(int direction) {
            super(direction,
                  UIManager.getColor("TabbedPane.selected"),
                  UIManager.getColor("TabbedPane.shadow"),
                  UIManager.getColor("TabbedPane.darkShadow"),
                  UIManager.getColor("TabbedPane.highlight"));
        }
    }


// Controller: event listeners

    private class Handler implements ChangeListener, ContainerListener,
                  FocusListener, MouseListener, MouseMotionListener,
                  PropertyChangeListener {
        //
        // PropertyChangeListener
        //
        public void propertyChange(PropertyChangeEvent e) {
            JTabbedPane pane = (JTabbedPane)e.getSource();
            String name = e.getPropertyName();
            boolean isScrollLayout = scrollableTabLayoutEnabled();
            if (name == "mnemonicAt") {
                updateMnemonics();
                pane.repaint();
            }
            else if (name == "displayedMnemonicIndexAt") {
                pane.repaint();
            }
            else if (name =="indexForTitle") {
                calculatedBaseline = false;
                Integer index = (Integer) e.getNewValue();
                updateHtmlViews(index, false);
            } else if (name == "tabLayoutPolicy") {
                BasicTabbedPaneUI.this.uninstallUI(pane);
                BasicTabbedPaneUI.this.installUI(pane);
                calculatedBaseline = false;
            } else if (name == "tabPlacement") {
                if (scrollableTabLayoutEnabled()) {
                    tabScroller.createButtons();
                }
                calculatedBaseline = false;
            } else if (name == "opaque" && isScrollLayout) {
                boolean newVal = ((Boolean)e.getNewValue()).booleanValue();
                tabScroller.tabPanel.setOpaque(newVal);
                tabScroller.viewport.setOpaque(newVal);
            } else if (name == "background" && isScrollLayout) {
                Color newVal = (Color)e.getNewValue();
                tabScroller.tabPanel.setBackground(newVal);
                tabScroller.viewport.setBackground(newVal);
                Color newColor = selectedColor == null ? newVal : selectedColor;
                tabScroller.scrollForwardButton.setBackground(newColor);
                tabScroller.scrollBackwardButton.setBackground(newColor);
            } else if (name == "indexForTabComponent") {
                if (tabContainer != null) {
                    tabContainer.removeUnusedTabComponents();
                }
                Component c = tabPane.getTabComponentAt(
                        (Integer)e.getNewValue());
                if (c != null) {
                    if (tabContainer == null) {
                        installTabContainer();
                    } else {
                        tabContainer.add(c);
                    }
                }
                tabPane.revalidate();
                tabPane.repaint();
                calculatedBaseline = false;
            } else if (name == "indexForNullComponent") {
                isRunsDirty = true;
                updateHtmlViews((Integer)e.getNewValue(), true);
            } else if (name == "font" || SwingUtilities2.isScaleChanged(e)) {
                calculatedBaseline = false;
            }
        }

        private void updateHtmlViews(int index, boolean inserted) {
            String title = tabPane.getTitleAt(index);
            boolean isHTML = BasicHTML.isHTMLString(title);
            if (isHTML) {
                if (htmlViews==null) {    // Initialize vector
                    htmlViews = createHTMLVector();
                } else {                  // Vector already exists
                    View v = BasicHTML.createHTMLView(tabPane, title);
                    setHtmlView(v, inserted, index);
                }
            } else {                             // Not HTML
                if (htmlViews!=null) {           // Add placeholder
                    setHtmlView(null, inserted, index);
                }                                // else nada!
            }
            updateMnemonics();
        }

        private void setHtmlView(View v, boolean inserted, int index) {
            if (inserted || index >= htmlViews.size()) {
                htmlViews.insertElementAt(v, index);
            } else {
                htmlViews.setElementAt(v, index);
            }
        }

        //
        // ChangeListener
        //
        public void stateChanged(ChangeEvent e) {
            JTabbedPane tabPane = (JTabbedPane)e.getSource();
            tabPane.revalidate();
            tabPane.repaint();

            setFocusIndex(tabPane.getSelectedIndex(), false);

            if (scrollableTabLayoutEnabled()) {
                ensureCurrentLayout();
                int index = tabPane.getSelectedIndex();
                if (index < rects.length && index != -1) {
                    tabScroller.tabPanel.scrollRectToVisible(
                            (Rectangle)rects[index].clone());
                }
            }
        }

        //
        // MouseListener
        //
        public void mouseClicked(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
            setRolloverTab(e.getX(), e.getY());
        }

        public void mouseExited(MouseEvent e) {
            setRolloverTab(-1);
        }

        public void mousePressed(MouseEvent e) {
            if (!tabPane.isEnabled()) {
                return;
            }
            int tabIndex = tabForCoordinate(tabPane, e.getX(), e.getY());
            if (tabIndex >= 0 && tabPane.isEnabledAt(tabIndex)) {
                if (tabIndex != tabPane.getSelectedIndex()) {
                    // Clicking on unselected tab, change selection, do NOT
                    // request focus.
                    // This will trigger the focusIndex to change by way
                    // of stateChanged.
                    tabPane.setSelectedIndex(tabIndex);
                }
                else if (tabPane.isRequestFocusEnabled()) {
                    // Clicking on selected tab, try and give the tabbedpane
                    // focus.  Repaint will occur in focusGained.
                    tabPane.requestFocus();
                }
            }
        }

        //
        // MouseMotionListener
        //
        public void mouseDragged(MouseEvent e) {
        }

        public void mouseMoved(MouseEvent e) {
            setRolloverTab(e.getX(), e.getY());
        }

        //
        // FocusListener
        //
        public void focusGained(FocusEvent e) {
           setFocusIndex(tabPane.getSelectedIndex(), true);
        }
        public void focusLost(FocusEvent e) {
           repaintTab(focusIndex);
        }


        //
        // ContainerListener
        //
    /* GES 2/3/99:
       The container listener code was added to support HTML
       rendering of tab titles.

       Ideally, we would be able to listen for property changes
       when a tab is added or its text modified.  At the moment
       there are no such events because the Beans spec doesn't
       allow 'indexed' property changes (i.e. tab 2's text changed
       from A to B).

       In order to get around this, we listen for tabs to be added
       or removed by listening for the container events.  we then
       queue up a runnable (so the component has a chance to complete
       the add) which checks the tab title of the new component to see
       if it requires HTML rendering.

       The Views (one per tab title requiring HTML rendering) are
       stored in the htmlViews Vector, which is only allocated after
       the first time we run into an HTML tab.  Note that this vector
       is kept in step with the number of pages, and nulls are added
       for those pages whose tab title do not require HTML rendering.

       This makes it easy for the paint and layout code to tell
       whether to invoke the HTML engine without having to check
       the string during time-sensitive operations.

       When we have added a way to listen for tab additions and
       changes to tab text, this code should be removed and
       replaced by something which uses that.  */

        public void componentAdded(ContainerEvent e) {
            JTabbedPane tp = (JTabbedPane)e.getContainer();
            Component child = e.getChild();
            if (child instanceof UIResource) {
                return;
            }
            isRunsDirty = true;
            updateHtmlViews(tp.indexOfComponent(child), true);
        }
        public void componentRemoved(ContainerEvent e) {
            JTabbedPane tp = (JTabbedPane)e.getContainer();
            Component child = e.getChild();
            if (child instanceof UIResource) {
                return;
            }

            // NOTE 4/15/2002 (joutwate):
            // This fix is implemented using client properties since there is
            // currently no IndexPropertyChangeEvent.  Once
            // IndexPropertyChangeEvents have been added this code should be
            // modified to use it.
            Integer indexObj =
                (Integer)tp.getClientProperty("__index_to_remove__");
            if (indexObj != null) {
                int index = indexObj.intValue();
                if (htmlViews != null && htmlViews.size() > index) {
                    htmlViews.removeElementAt(index);
                }
                tp.putClientProperty("__index_to_remove__", null);
            }
            isRunsDirty = true;
            updateMnemonics();

            validateFocusIndex();
        }
    }

    /**
     * This class should be treated as a &quot;protected&quot; inner class.
     * Instantiate it only within subclasses of BasicTabbedPaneUI.
     */
    public class PropertyChangeHandler implements PropertyChangeListener {
        /**
         * Constructs a {@code PropertyChangeHandler}.
         */
        public PropertyChangeHandler() {}

        // NOTE: This class exists only for backward compatibility. All
        // its functionality has been moved into Handler. If you need to add
        // new functionality add it to the Handler, but make sure this
        // class calls into the Handler.
        public void propertyChange(PropertyChangeEvent e) {
            getHandler().propertyChange(e);
        }
    }

    /**
     * This class should be treated as a &quot;protected&quot; inner class.
     * Instantiate it only within subclasses of BasicTabbedPaneUI.
     */
    public class TabSelectionHandler implements ChangeListener {
        /**
         * Constructs a {@code TabSelectionHandler}.
         */
        public TabSelectionHandler() {}

        // NOTE: This class exists only for backward compatibility. All
        // its functionality has been moved into Handler. If you need to add
        // new functionality add it to the Handler, but make sure this
        // class calls into the Handler.
        public void stateChanged(ChangeEvent e) {
            getHandler().stateChanged(e);
        }
    }

    /**
     * This class should be treated as a &quot;protected&quot; inner class.
     * Instantiate it only within subclasses of BasicTabbedPaneUI.
     */
    public class MouseHandler extends MouseAdapter {
        /**
         * Constructs a {@code MouseHandler}.
         */
        public MouseHandler() {}

        // NOTE: This class exists only for backward compatibility. All
        // its functionality has been moved into Handler. If you need to add
        // new functionality add it to the Handler, but make sure this
        // class calls into the Handler.
        public void mousePressed(MouseEvent e) {
            getHandler().mousePressed(e);
        }
    }

    /**
     * This class should be treated as a &quot;protected&quot; inner class.
     * Instantiate it only within subclasses of BasicTabbedPaneUI.
     */
    public class FocusHandler extends FocusAdapter {
        // NOTE: This class exists only for backward compatibility. All
        // its functionality has been moved into Handler. If you need to add
        // new functionality add it to the Handler, but make sure this
        // class calls into the Handler.
        /**
         * Constructs a {@code FocusHandler}.
         */
        public FocusHandler() {}

        public void focusGained(FocusEvent e) {
            getHandler().focusGained(e);
        }
        public void focusLost(FocusEvent e) {
            getHandler().focusLost(e);
        }
    }

    private Vector<View> createHTMLVector() {
        Vector<View> htmlViews = new Vector<View>();
        int count = tabPane.getTabCount();
        if (count>0) {
            for (int i=0 ; i<count; i++) {
                String title = tabPane.getTitleAt(i);
                if (BasicHTML.isHTMLString(title)) {
                    htmlViews.addElement(BasicHTML.createHTMLView(tabPane, title));
                } else {
                    htmlViews.addElement(null);
                }
            }
        }
        return htmlViews;
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private class TabContainer extends JPanel implements UIResource {
        private boolean notifyTabbedPane = true;

        public TabContainer() {
            super(null);
            setOpaque(false);
        }

        public void remove(Component comp) {
            int index = tabPane.indexOfTabComponent(comp);
            super.remove(comp);
            if (notifyTabbedPane && index != -1) {
                tabPane.setTabComponentAt(index, null);
            }
        }

        private void removeUnusedTabComponents() {
            for (Component c : getComponents()) {
                if (!(c instanceof UIResource)) {
                    int index = tabPane.indexOfTabComponent(c);
                    if (index == -1) {
                        super.remove(c);
                    }
                }
            }
        }

        public boolean isOptimizedDrawingEnabled() {
            return tabScroller != null && !tabScroller.croppedEdge.isParamsSet();
        }

        public void doLayout() {
            // We layout tabComponents in JTabbedPane's layout manager
            // and use this method as a hook for repainting tabs
            // to update tabs area e.g. when the size of tabComponent was changed
            if (scrollableTabLayoutEnabled()) {
                tabScroller.tabPanel.repaint();
                tabScroller.updateView();
            } else {
                tabPane.repaint(getBounds());
            }
        }
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private class CroppedEdge extends JPanel implements UIResource {
        private Shape shape;
        private int tabIndex;
        private int cropline;
        private int cropx, cropy;

        public CroppedEdge() {
            setOpaque(false);
        }

        public void setParams(int tabIndex, int cropline, int cropx, int cropy) {
            this.tabIndex = tabIndex;
            this.cropline = cropline;
            this.cropx = cropx;
            this.cropy = cropy;
            Rectangle tabRect = rects[tabIndex];
            setBounds(tabRect);
            shape = createCroppedTabShape(tabPane.getTabPlacement(), tabRect, cropline);
            if (getParent() == null && tabContainer != null) {
                tabContainer.add(this, 0);
            }
        }

        public void resetParams() {
            shape = null;
            if (getParent() == tabContainer && tabContainer != null) {
                tabContainer.remove(this);
            }
        }

        public boolean isParamsSet() {
            return shape != null;
        }

        public int getTabIndex() {
            return tabIndex;
        }

        public int getCropline() {
            return cropline;
        }

        public int getCroppedSideWidth() {
            return 3;
        }

        private Color getBgColor() {
            Component parent = tabPane.getParent();
            if (parent != null) {
                Color bg = parent.getBackground();
                if (bg != null) {
                    return bg;
                }
            }
            return UIManager.getColor("control");
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (isParamsSet() && g instanceof Graphics2D) {
                Graphics2D g2 = (Graphics2D) g;
                g2.clipRect(0, 0, getWidth(), getHeight());
                g2.setColor(getBgColor());
                g2.translate(cropx, cropy);
                g2.fill(shape);
                paintCroppedTabEdge(g);
                g2.translate(-cropx, -cropy);
            }
        }
    }
}
