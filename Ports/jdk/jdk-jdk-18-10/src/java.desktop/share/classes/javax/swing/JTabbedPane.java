/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.beans.BeanProperty;
import java.beans.JavaBean;
import java.beans.Transient;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleComponent;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleIcon;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleSelection;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.TabbedPaneUI;
import javax.swing.plaf.UIResource;

import sun.swing.SwingUtilities2;

/**
 * A component that lets the user switch between a group of components by
 * clicking on a tab with a given title and/or icon.
 * For examples and information on using tabbed panes see
 * <a href="https://docs.oracle.com/javase/tutorial/uiswing/components/tabbedpane.html">How to Use Tabbed Panes</a>,
 * a section in <em>The Java Tutorial</em>.
 * <p>
 * Tabs/components are added to a <code>TabbedPane</code> object by using the
 * <code>addTab</code> and <code>insertTab</code> methods.
 * A tab is represented by an index corresponding
 * to the position it was added in, where the first tab has an index equal to 0
 * and the last tab has an index equal to the tab count minus 1.
 * <p>
 * The <code>TabbedPane</code> uses a <code>SingleSelectionModel</code>
 * to represent the set
 * of tab indices and the currently selected index.  If the tab count
 * is greater than 0, then there will always be a selected index, which
 * by default will be initialized to the first tab.  If the tab count is
 * 0, then the selected index will be -1.
 * <p>
 * The tab title can be rendered by a <code>Component</code>.
 * For example, the following produce similar results:
 * <pre>
 * // In this case the look and feel renders the title for the tab.
 * tabbedPane.addTab("Tab", myComponent);
 * // In this case the custom component is responsible for rendering the
 * // title of the tab.
 * tabbedPane.addTab(null, myComponent);
 * tabbedPane.setTabComponentAt(0, new JLabel("Tab"));
 * </pre>
 * The latter is typically used when you want a more complex user interaction
 * that requires custom components on the tab.  For example, you could
 * provide a custom component that animates or one that has widgets for
 * closing the tab.
 * <p>
 * If you specify a component for a tab, the <code>JTabbedPane</code>
 * will not render any text or icon you have specified for the tab.
 * <p>
 * <strong>Note:</strong>
 * Do not use <code>setVisible</code> directly on a tab component to make it visible,
 * use <code>setSelectedComponent</code> or <code>setSelectedIndex</code> methods instead.
 * <p>
 * <strong>Warning:</strong> Swing is not thread safe. For more
 * information see <a
 * href="package-summary.html#threading">Swing's Threading
 * Policy</a>.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @author Dave Moore
 * @author Philip Milne
 * @author Amy Fowler
 *
 * @see SingleSelectionModel
 * @since 1.2
 */
@JavaBean(defaultProperty = "UI", description = "A component which provides a tab folder metaphor for displaying one component from a set of components.")
@SwingContainer
@SuppressWarnings("serial") // Same-version serialization only
public class JTabbedPane extends JComponent
       implements Serializable, Accessible, SwingConstants {

   /**
    * The tab layout policy for wrapping tabs in multiple runs when all
    * tabs will not fit within a single run.
    */
    public static final int WRAP_TAB_LAYOUT = 0;

   /**
    * Tab layout policy for providing a subset of available tabs when all
    * the tabs will not fit within a single run.  If all the tabs do
    * not fit within a single run the look and feel will provide a way
    * to navigate to hidden tabs.
    */
    public static final int SCROLL_TAB_LAYOUT = 1;


    /**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "TabbedPaneUI";

    /**
     * Where the tabs are placed.
     * @see #setTabPlacement
     */
    protected int tabPlacement = TOP;

    private int tabLayoutPolicy;

    /** The default selection model */
    protected SingleSelectionModel model;

    private boolean haveRegistered;

    /**
     * The <code>changeListener</code> is the listener we add to the
     * model.
     */
    protected ChangeListener changeListener = null;

    private java.util.List<Page> pages;

    /* The component that is currently visible */
    private Component visComp = null;

    /**
     * Only one <code>ChangeEvent</code> is needed per <code>TabPane</code>
     * instance since the
     * event's only (read-only) state is the source property.  The source
     * of events generated here is always "this".
     */
    protected transient ChangeEvent changeEvent = null;

    /**
     * Creates an empty <code>TabbedPane</code> with a default
     * tab placement of <code>JTabbedPane.TOP</code>.
     * @see #addTab
     */
    public JTabbedPane() {
        this(TOP, WRAP_TAB_LAYOUT);
    }

    /**
     * Creates an empty <code>TabbedPane</code> with the specified tab placement
     * of either: <code>JTabbedPane.TOP</code>, <code>JTabbedPane.BOTTOM</code>,
     * <code>JTabbedPane.LEFT</code>, or <code>JTabbedPane.RIGHT</code>.
     *
     * @param tabPlacement the placement for the tabs relative to the content
     * @see #addTab
     */
    public JTabbedPane(int tabPlacement) {
        this(tabPlacement, WRAP_TAB_LAYOUT);
    }

    /**
     * Creates an empty <code>TabbedPane</code> with the specified tab placement
     * and tab layout policy.  Tab placement may be either:
     * <code>JTabbedPane.TOP</code>, <code>JTabbedPane.BOTTOM</code>,
     * <code>JTabbedPane.LEFT</code>, or <code>JTabbedPane.RIGHT</code>.
     * Tab layout policy may be either: <code>JTabbedPane.WRAP_TAB_LAYOUT</code>
     * or <code>JTabbedPane.SCROLL_TAB_LAYOUT</code>.
     *
     * @param tabPlacement the placement for the tabs relative to the content
     * @param tabLayoutPolicy the policy for laying out tabs when all tabs will not fit on one run
     * @exception IllegalArgumentException if tab placement or tab layout policy are not
     *            one of the above supported values
     * @see #addTab
     * @since 1.4
     */
    public JTabbedPane(int tabPlacement, int tabLayoutPolicy) {
        setTabPlacement(tabPlacement);
        setTabLayoutPolicy(tabLayoutPolicy);
        pages = new ArrayList<Page>(1);
        setModel(new DefaultSingleSelectionModel());
        updateUI();
    }

    /**
     * Returns the UI object which implements the L&amp;F for this component.
     *
     * @return a <code>TabbedPaneUI</code> object
     * @see #setUI
     */
    public TabbedPaneUI getUI() {
        return (TabbedPaneUI)ui;
    }

    /**
     * Sets the UI object which implements the L&amp;F for this component.
     *
     * @param ui the new UI object
     * @see UIDefaults#getUI
     */
    @BeanProperty(hidden = true, visualUpdate = true, description
            = "The UI object that implements the tabbedpane's LookAndFeel")
    public void setUI(TabbedPaneUI ui) {
        super.setUI(ui);
        // disabled icons are generated by LF so they should be unset here
        for (int i = 0; i < getTabCount(); i++) {
            Icon icon = pages.get(i).disabledIcon;
            if (icon instanceof UIResource) {
                setDisabledIconAt(i, null);
            }
        }
    }

    /**
     * Resets the UI property to a value from the current look and feel.
     *
     * @see JComponent#updateUI
     */
    public void updateUI() {
        setUI((TabbedPaneUI)UIManager.getUI(this));
    }


    /**
     * Returns the name of the UI class that implements the
     * L&amp;F for this component.
     *
     * @return the string "TabbedPaneUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    @BeanProperty(bound = false)
    public String getUIClassID() {
        return uiClassID;
    }


    /**
     * We pass <code>ModelChanged</code> events along to the listeners with
     * the tabbedpane (instead of the model itself) as the event source.
     */
    protected class ModelListener implements ChangeListener, Serializable {

        /**
         * Constructs a {@code ModelListener}.
         */
        protected ModelListener() {}

        public void stateChanged(ChangeEvent e) {
            fireStateChanged();
        }
    }

    /**
     * Subclasses that want to handle <code>ChangeEvents</code> differently
     * can override this to return a subclass of <code>ModelListener</code> or
     * another <code>ChangeListener</code> implementation.
     *
     * @return a {@code ChangeListener}
     * @see #fireStateChanged
     */
    protected ChangeListener createChangeListener() {
        return new ModelListener();
    }

    /**
     * Adds a <code>ChangeListener</code> to this tabbedpane.
     *
     * @param l the <code>ChangeListener</code> to add
     * @see #fireStateChanged
     * @see #removeChangeListener
     */
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    /**
     * Removes a <code>ChangeListener</code> from this tabbedpane.
     *
     * @param l the <code>ChangeListener</code> to remove
     * @see #fireStateChanged
     * @see #addChangeListener
     */
    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

   /**
     * Returns an array of all the <code>ChangeListener</code>s added
     * to this <code>JTabbedPane</code> with <code>addChangeListener</code>.
     *
     * @return all of the <code>ChangeListener</code>s added or an empty
     *         array if no listeners have been added
     * @since 1.4
     */
   @BeanProperty(bound = false)
   public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    /**
     * Sends a {@code ChangeEvent}, with this {@code JTabbedPane} as the source,
     * to each registered listener. This method is called each time there is
     * a change to either the selected index or the selected tab in the
     * {@code JTabbedPane}. Usually, the selected index and selected tab change
     * together. However, there are some cases, such as tab addition, where the
     * selected index changes and the same tab remains selected. There are other
     * cases, such as deleting the selected tab, where the index remains the
     * same, but a new tab moves to that index. Events are fired for all of
     * these cases.
     *
     * @see #addChangeListener
     * @see javax.swing.event.EventListenerList
     */
    @SuppressWarnings("deprecation")
    protected void fireStateChanged() {
        /* --- Begin code to deal with visibility --- */

        /* This code deals with changing the visibility of components to
         * hide and show the contents for the selected tab. It duplicates
         * logic already present in BasicTabbedPaneUI, logic that is
         * processed during the layout pass. This code exists to allow
         * developers to do things that are quite difficult to accomplish
         * with the previous model of waiting for the layout pass to process
         * visibility changes; such as requesting focus on the new visible
         * component.
         *
         * For the average code, using the typical JTabbedPane methods,
         * all visibility changes will now be processed here. However,
         * the code in BasicTabbedPaneUI still exists, for the purposes
         * of backward compatibility. Therefore, when making changes to
         * this code, ensure that the BasicTabbedPaneUI code is kept in
         * synch.
         */

        int selIndex = getSelectedIndex();

        /* if the selection is now nothing */
        if (selIndex < 0) {
            /* if there was a previous visible component */
            if (visComp != null && visComp.isVisible()) {
                /* make it invisible */
                visComp.setVisible(false);
            }

            /* now there's no visible component */
            visComp = null;

        /* else - the selection is now something */
        } else {
            /* Fetch the component for the new selection */
            Component newComp = getComponentAt(selIndex);

            /* if the new component is non-null and different */
            if (newComp != null && newComp != visComp) {
                boolean shouldChangeFocus = false;

                /* Note: the following (clearing of the old visible component)
                 * is inside this if-statement for good reason: Tabbed pane
                 * should continue to show the previously visible component
                 * if there is no component for the chosen tab.
                 */

                /* if there was a previous visible component */
                if (visComp != null) {
                    shouldChangeFocus =
                        (SwingUtilities.findFocusOwner(visComp) != null);

                    /* if it's still visible */
                    if (visComp.isVisible()) {
                        /* make it invisible */
                        visComp.setVisible(false);
                    }
                }

                if (!newComp.isVisible()) {
                    newComp.setVisible(true);
                }

                if (shouldChangeFocus) {
                    SwingUtilities2.tabbedPaneChangeFocusTo(newComp);
                }

                visComp = newComp;
            } /* else - the visible component shouldn't changed */
        }

        /* --- End code to deal with visibility --- */

        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ChangeListener.class) {
                // Lazily create the event:
                if (changeEvent == null)
                    changeEvent = new ChangeEvent(this);
                ((ChangeListener)listeners[i+1]).stateChanged(changeEvent);
            }
        }
    }

    /**
     * Returns the model associated with this tabbedpane.
     *
     * @return the {@code SingleSelectionModel} associated with this tabbedpane
     * @see #setModel
     */
    public SingleSelectionModel getModel() {
        return model;
    }

    /**
     * Sets the model to be used with this tabbedpane.
     *
     * @param model the model to be used
     * @see #getModel
     */
    @BeanProperty(description
            = "The tabbedpane's SingleSelectionModel.")
    public void setModel(SingleSelectionModel model) {
        SingleSelectionModel oldModel = getModel();

        if (oldModel != null) {
            oldModel.removeChangeListener(changeListener);
            changeListener = null;
        }

        this.model = model;

        if (model != null) {
            changeListener = createChangeListener();
            model.addChangeListener(changeListener);
        }

        firePropertyChange("model", oldModel, model);
        repaint();
    }

    /**
     * Returns the placement of the tabs for this tabbedpane.
     *
     * @return an {@code int} specifying the placement for the tabs
     * @see #setTabPlacement
     */
    public int getTabPlacement() {
        return tabPlacement;
    }

    /**
     * Sets the tab placement for this tabbedpane.
     * Possible values are:<ul>
     * <li><code>JTabbedPane.TOP</code>
     * <li><code>JTabbedPane.BOTTOM</code>
     * <li><code>JTabbedPane.LEFT</code>
     * <li><code>JTabbedPane.RIGHT</code>
     * </ul>
     * The default value, if not set, is <code>SwingConstants.TOP</code>.
     *
     * @param tabPlacement the placement for the tabs relative to the content
     * @exception IllegalArgumentException if tab placement value isn't one
     *                          of the above valid values
     */
    @BeanProperty(preferred = true, visualUpdate = true, enumerationValues = {
            "JTabbedPane.TOP",
            "JTabbedPane.LEFT",
            "JTabbedPane.BOTTOM",
            "JTabbedPane.RIGHT"}, description
            = "The tabbedpane's tab placement.")
    public void setTabPlacement(int tabPlacement) {
        checkTabPlacement(tabPlacement);
        if (this.tabPlacement != tabPlacement) {
            int oldValue = this.tabPlacement;
            this.tabPlacement = tabPlacement;
            firePropertyChange("tabPlacement", oldValue, tabPlacement);
            revalidate();
            repaint();
        }
    }

    private static void checkTabPlacement(int tabPlacement) {
        if (tabPlacement != TOP && tabPlacement != LEFT &&
            tabPlacement != BOTTOM && tabPlacement != RIGHT) {
            throw new IllegalArgumentException("illegal tab placement:"
                    + " must be TOP, BOTTOM, LEFT, or RIGHT");
        }
    }

    /**
     * Returns the policy used by the tabbedpane to layout the tabs when all the
     * tabs will not fit within a single run.
     *
     * @return an {@code int} specifying the policy used to layout the tabs
     * @see #setTabLayoutPolicy
     * @since 1.4
     */
    public int getTabLayoutPolicy() {
        return tabLayoutPolicy;
    }

   /**
     * Sets the policy which the tabbedpane will use in laying out the tabs
     * when all the tabs will not fit within a single run.
     * Possible values are:
     * <ul>
     * <li><code>JTabbedPane.WRAP_TAB_LAYOUT</code>
     * <li><code>JTabbedPane.SCROLL_TAB_LAYOUT</code>
     * </ul>
     *
     * The default value, if not set by the UI, is <code>JTabbedPane.WRAP_TAB_LAYOUT</code>.
     * <p>
     * Some look and feels might only support a subset of the possible
     * layout policies, in which case the value of this property may be
     * ignored.
     *
     * @param tabLayoutPolicy the policy used to layout the tabs
     * @exception IllegalArgumentException if layoutPolicy value isn't one
     *                          of the above valid values
     * @see #getTabLayoutPolicy
     * @since 1.4
     */
    @BeanProperty(preferred = true, visualUpdate = true, enumerationValues = {
            "JTabbedPane.WRAP_TAB_LAYOUT",
            "JTabbedPane.SCROLL_TAB_LAYOUT"}, description
            = "The tabbedpane's policy for laying out the tabs")
    public void setTabLayoutPolicy(int tabLayoutPolicy) {
        checkTabLayoutPolicy(tabLayoutPolicy);
        if (this.tabLayoutPolicy != tabLayoutPolicy) {
            int oldValue = this.tabLayoutPolicy;
            this.tabLayoutPolicy = tabLayoutPolicy;
            firePropertyChange("tabLayoutPolicy", oldValue, tabLayoutPolicy);
            revalidate();
            repaint();
        }
    }

    private static void checkTabLayoutPolicy(int tabLayoutPolicy) {
        if (tabLayoutPolicy != WRAP_TAB_LAYOUT
                && tabLayoutPolicy != SCROLL_TAB_LAYOUT) {
            throw new IllegalArgumentException("illegal tab layout policy:"
                    + " must be WRAP_TAB_LAYOUT or SCROLL_TAB_LAYOUT");
        }
    }

    /**
     * Returns the currently selected index for this tabbedpane.
     * Returns -1 if there is no currently selected tab.
     *
     * @return the index of the selected tab
     * @see #setSelectedIndex
     */
    @Transient
    public int getSelectedIndex() {
        return model.getSelectedIndex();
    }

    /**
     * Sets the selected index for this tabbedpane. The index must be
     * a valid tab index or -1, which indicates that no tab should be selected
     * (can also be used when there are no tabs in the tabbedpane).  If a -1
     * value is specified when the tabbedpane contains one or more tabs, then
     * the results will be implementation defined.
     *
     * @param index  the index to be selected
     * @exception IndexOutOfBoundsException if index is out of range
     *            {@code (index < -1 || index >= tab count)}
     *
     * @see #getSelectedIndex
     * @see SingleSelectionModel#setSelectedIndex
     */
    @BeanProperty(bound = false, preferred = true, description
            = "The tabbedpane's selected tab index.")
    public void setSelectedIndex(int index) {
        if (index != -1) {
            checkIndex(index);
        }
        setSelectedIndexImpl(index, true);
    }


    private void setSelectedIndexImpl(int index, boolean doAccessibleChanges) {
        int oldIndex = model.getSelectedIndex();
        Page oldPage = null, newPage = null;
        String oldName = null;

        doAccessibleChanges = doAccessibleChanges && (oldIndex != index);

        if (doAccessibleChanges) {
            if (accessibleContext != null) {
                oldName = accessibleContext.getAccessibleName();
            }

            if (oldIndex >= 0) {
                oldPage = pages.get(oldIndex);
            }

            if (index >= 0) {
                newPage = pages.get(index);
            }
        }

        model.setSelectedIndex(index);

        if (doAccessibleChanges) {
            changeAccessibleSelection(oldPage, oldName, newPage);
        }
    }

    private void changeAccessibleSelection(Page oldPage, String oldName, Page newPage) {
        if (accessibleContext == null) {
            return;
        }

        if (oldPage != null) {
            oldPage.firePropertyChange(AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                                       AccessibleState.SELECTED, null);
        }

        if (newPage != null) {
            newPage.firePropertyChange(AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                                       null, AccessibleState.SELECTED);
        }

        accessibleContext.firePropertyChange(
            AccessibleContext.ACCESSIBLE_NAME_PROPERTY,
            oldName,
            accessibleContext.getAccessibleName());
    }

    /**
     * Returns the currently selected component for this tabbedpane.
     * Returns <code>null</code> if there is no currently selected tab.
     *
     * @return the component corresponding to the selected tab
     * @see #setSelectedComponent
     */
    @Transient
    public Component getSelectedComponent() {
        int index = getSelectedIndex();
        if (index == -1) {
            return null;
        }
        return getComponentAt(index);
    }

    /**
     * Sets the selected component for this tabbedpane.  This
     * will automatically set the <code>selectedIndex</code> to the index
     * corresponding to the specified component.
     *
     * @param c the selected {@code Component} for this {@code TabbedPane}
     * @exception IllegalArgumentException if component not found in tabbed
     *          pane
     * @see #getSelectedComponent
     */
    @BeanProperty(bound = false, preferred = true, description
            = "The tabbedpane's selected component.")
    public void setSelectedComponent(Component c) {
        int index = indexOfComponent(c);
        if (index != -1) {
            setSelectedIndex(index);
        } else {
            throw new IllegalArgumentException("component not found in tabbed pane");
        }
    }

    /**
     * Inserts a new tab for the given component, at the given index,
     * represented by the given title and/or icon, either of which may
     * be {@code null}.
     *
     * @param title the title to be displayed on the tab
     * @param icon the icon to be displayed on the tab
     * @param component the component to be displayed when this tab is clicked.
     * @param tip the tooltip to be displayed for this tab
     * @param index the position to insert this new tab
     *       ({@code > 0 and <= getTabCount()})
     *
     * @throws IndexOutOfBoundsException if the index is out of range
     *         ({@code < 0 or > getTabCount()})
     *
     * @see #addTab
     * @see #removeTabAt
     */
    public void insertTab(String title, Icon icon, Component component, String tip, int index) {
        int newIndex = index;

        // If component already exists, remove corresponding
        // tab so that new tab gets added correctly
        // Note: we are allowing component=null because of compatibility,
        // but we really should throw an exception because much of the
        // rest of the JTabbedPane implementation isn't designed to deal
        // with null components for tabs.
        int removeIndex = indexOfComponent(component);
        if (component != null && removeIndex != -1) {
            removeTabAt(removeIndex);
            if (newIndex > removeIndex) {
                newIndex--;
            }
        }

        int selectedIndex = getSelectedIndex();

        pages.add(
            newIndex,
            new Page(this, title != null? title : "", icon, null, component, tip));


        if (component != null) {
            addImpl(component, null, -1);
            component.setVisible(false);
        } else {
            firePropertyChange("indexForNullComponent", -1, index);
        }

        if (pages.size() == 1) {
            setSelectedIndex(0);
        }

        if (selectedIndex >= newIndex) {
            setSelectedIndexImpl(selectedIndex + 1, false);
        }

        if (!haveRegistered && tip != null) {
            ToolTipManager.sharedInstance().registerComponent(this);
            haveRegistered = true;
        }

        if (accessibleContext != null) {
            accessibleContext.firePropertyChange(
                    AccessibleContext.ACCESSIBLE_VISIBLE_DATA_PROPERTY,
                    null, component);
        }
        revalidate();
        repaint();
    }

    /**
     * Adds a <code>component</code> and <code>tip</code>
     * represented by a <code>title</code> and/or <code>icon</code>,
     * either of which can be <code>null</code>.
     * Cover method for <code>insertTab</code>.
     *
     * @param title the title to be displayed in this tab
     * @param icon the icon to be displayed in this tab
     * @param component the component to be displayed when this tab is clicked
     * @param tip the tooltip to be displayed for this tab
     *
     * @see #insertTab
     * @see #removeTabAt
     */
    public void addTab(String title, Icon icon, Component component, String tip) {
        insertTab(title, icon, component, tip, pages.size());
    }

    /**
     * Adds a <code>component</code> represented by a <code>title</code>
     * and/or <code>icon</code>, either of which can be <code>null</code>.
     * Cover method for <code>insertTab</code>.
     *
     * @param title the title to be displayed in this tab
     * @param icon the icon to be displayed in this tab
     * @param component the component to be displayed when this tab is clicked
     *
     * @see #insertTab
     * @see #removeTabAt
     */
    public void addTab(String title, Icon icon, Component component) {
        insertTab(title, icon, component, null, pages.size());
    }

    /**
     * Adds a <code>component</code> represented by a <code>title</code>
     * and no icon.
     * Cover method for <code>insertTab</code>.
     *
     * @param title the title to be displayed in this tab
     * @param component the component to be displayed when this tab is clicked
     *
     * @see #insertTab
     * @see #removeTabAt
     */
    public void addTab(String title, Component component) {
        insertTab(title, null, component, null, pages.size());
    }

    /**
     * Adds a <code>component</code> with a tab title defaulting to
     * the name of the component which is the result of calling
     * <code>component.getName</code>.
     * Cover method for <code>insertTab</code>.
     *
     * @param component the component to be displayed when this tab is clicked
     * @return the component
     *
     * @see #insertTab
     * @see #removeTabAt
     */
    public Component add(Component component) {
        if (!(component instanceof UIResource)) {
            addTab(component.getName(), component);
        } else {
            super.add(component);
        }
        return component;
    }

    /**
     * Adds a <code>component</code> with the specified tab title.
     * Cover method for <code>insertTab</code>.
     *
     * @param title the title to be displayed in this tab
     * @param component the component to be displayed when this tab is clicked
     * @return the component
     *
     * @see #insertTab
     * @see #removeTabAt
     */
    public Component add(String title, Component component) {
        if (!(component instanceof UIResource)) {
            addTab(title, component);
        } else {
            super.add(title, component);
        }
        return component;
    }

    /**
     * Adds a <code>component</code> at the specified tab index with a tab
     * title defaulting to the name of the component.
     * Cover method for <code>insertTab</code>.
     *
     * @param component the component to be displayed when this tab is clicked
     * @param index the position to insert this new tab
     * @return the component
     *
     * @see #insertTab
     * @see #removeTabAt
     */
    public Component add(Component component, int index) {
        if (!(component instanceof UIResource)) {
            // Container.add() interprets -1 as "append", so convert
            // the index appropriately to be handled by the vector
            insertTab(component.getName(), null, component, null,
                      index == -1? getTabCount() : index);
        } else {
            super.add(component, index);
        }
        return component;
    }

    /**
     * Adds a <code>component</code> to the tabbed pane.
     * If <code>constraints</code> is a <code>String</code> or an
     * <code>Icon</code>, it will be used for the tab title,
     * otherwise the component's name will be used as the tab title.
     * Cover method for <code>insertTab</code>.
     *
     * @param component the component to be displayed when this tab is clicked
     * @param constraints the object to be displayed in the tab
     *
     * @see #insertTab
     * @see #removeTabAt
     */
    public void add(Component component, Object constraints) {
        if (!(component instanceof UIResource)) {
            if (constraints instanceof String) {
                addTab((String)constraints, component);
            } else if (constraints instanceof Icon) {
                addTab(null, (Icon)constraints, component);
            } else {
                add(component);
            }
        } else {
            super.add(component, constraints);
        }
    }

    /**
     * Adds a <code>component</code> at the specified tab index.
     * If <code>constraints</code> is a <code>String</code> or an
     * <code>Icon</code>, it will be used for the tab title,
     * otherwise the component's name will be used as the tab title.
     * Cover method for <code>insertTab</code>.
     *
     * @param component the component to be displayed when this tab is clicked
     * @param constraints the object to be displayed in the tab
     * @param index the position to insert this new tab
     *
     * @see #insertTab
     * @see #removeTabAt
     */
    public void add(Component component, Object constraints, int index) {
        if (!(component instanceof UIResource)) {

            Icon icon = constraints instanceof Icon? (Icon)constraints : null;
            String title = constraints instanceof String? (String)constraints : null;
            // Container.add() interprets -1 as "append", so convert
            // the index appropriately to be handled by the vector
            insertTab(title, icon, component, null, index == -1? getTabCount() : index);
        } else {
            super.add(component, constraints, index);
        }
    }

    private void clearAccessibleParent(Component c) {
        AccessibleContext ac = c.getAccessibleContext();
        if (ac != null) {
            ac.setAccessibleParent(null);
        }
    }

    /**
     * Removes the tab at <code>index</code>.
     * After the component associated with <code>index</code> is removed,
     * its visibility is reset to true to ensure it will be visible
     * if added to other containers.
     * @param index the index of the tab to be removed
     * @exception IndexOutOfBoundsException if index is out of range
     *            {@code (index < 0 || index >= tab count)}
     *
     * @see #addTab
     * @see #insertTab
     */
    @SuppressWarnings("deprecation")
    public void removeTabAt(int index) {
        checkIndex(index);

        Component component = getComponentAt(index);
        boolean shouldChangeFocus = false;
        int selected = getSelectedIndex();
        String oldName = null;

        /* if we're about to remove the visible component */
        if (component == visComp) {
            shouldChangeFocus = (SwingUtilities.findFocusOwner(visComp) != null);
            visComp = null;
        }

        if (accessibleContext != null) {
            /* if we're removing the selected page */
            if (index == selected) {
                /* fire an accessible notification that it's unselected */
                pages.get(index).firePropertyChange(
                    AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                    AccessibleState.SELECTED, null);

                oldName = accessibleContext.getAccessibleName();
            }

            accessibleContext.firePropertyChange(
                    AccessibleContext.ACCESSIBLE_VISIBLE_DATA_PROPERTY,
                    component, null);
        }

        // Force the tabComponent to be cleaned up.
        setTabComponentAt(index, null);
        pages.remove(index);

        // NOTE 4/15/2002 (joutwate):
        // This fix is implemented using client properties since there is
        // currently no IndexPropertyChangeEvent.  Once
        // IndexPropertyChangeEvents have been added this code should be
        // modified to use it.
        putClientProperty("__index_to_remove__", Integer.valueOf(index));

        /* if the selected tab is after the removal */
        if (selected > index) {
            setSelectedIndexImpl(selected - 1, false);

        /* if the selected tab is the last tab */
        } else if (selected >= getTabCount()) {
            setSelectedIndexImpl(selected - 1, false);
            Page newSelected = (selected != 0)
                ? pages.get(selected - 1)
                : null;

            changeAccessibleSelection(null, oldName, newSelected);

        /* selected index hasn't changed, but the associated tab has */
        } else if (index == selected) {
            fireStateChanged();
            changeAccessibleSelection(null, oldName, pages.get(index));
        }

        // We can't assume the tab indices correspond to the
        // container's children array indices, so make sure we
        // remove the correct child!
        if (component != null) {
            Component[] components = getComponents();
            for (int i = components.length; --i >= 0; ) {
                if (components[i] == component) {
                    super.remove(i);
                    component.setVisible(true);
                    clearAccessibleParent(component);
                    break;
                }
            }
        }

        if (shouldChangeFocus) {
            SwingUtilities2.tabbedPaneChangeFocusTo(getSelectedComponent());
        }

        revalidate();
        repaint();
    }

    /**
     * Removes the specified <code>Component</code> from the
     * <code>JTabbedPane</code>. The method does nothing
     * if the <code>component</code> is null.
     *
     * @param component the component to remove from the tabbedpane
     * @see #addTab
     * @see #removeTabAt
     */
    public void remove(Component component) {
        int index = indexOfComponent(component);
        if (index != -1) {
            removeTabAt(index);
        } else {
            // Container#remove(comp) invokes Container#remove(int)
            // so make sure JTabbedPane#remove(int) isn't called here
            Component[] children = getComponents();
            for (int i=0; i < children.length; i++) {
                if (component == children[i]) {
                    super.remove(i);
                    break;
                }
            }
        }
    }

    /**
     * Removes the tab and component which corresponds to the specified index.
     *
     * @param index the index of the component to remove from the
     *          <code>tabbedpane</code>
     * @exception IndexOutOfBoundsException if index is out of range
     *            {@code (index < 0 || index >= tab count)}
     * @see #addTab
     * @see #removeTabAt
     */
    public void remove(int index) {
        removeTabAt(index);
    }

    /**
     * Removes all the tabs and their corresponding components
     * from the <code>tabbedpane</code>.
     *
     * @see #addTab
     * @see #removeTabAt
     */
    public void removeAll() {
        setSelectedIndexImpl(-1, true);

        int tabCount = getTabCount();
        // We invoke removeTabAt for each tab, otherwise we may end up
        // removing Components added by the UI.
        while (tabCount-- > 0) {
            removeTabAt(tabCount);
        }
    }

    /**
     * Returns the number of tabs in this <code>tabbedpane</code>.
     *
     * @return an integer specifying the number of tabbed pages
     */
    @BeanProperty(bound = false)
    public int getTabCount() {
        return pages.size();
    }

    /**
     * Returns the number of tab runs currently used to display
     * the tabs.
     * @return an integer giving the number of rows if the
     *          <code>tabPlacement</code>
     *          is <code>TOP</code> or <code>BOTTOM</code>
     *          and the number of columns if
     *          <code>tabPlacement</code>
     *          is <code>LEFT</code> or <code>RIGHT</code>,
     *          or 0 if there is no UI set on this <code>tabbedpane</code>
     */
    @BeanProperty(bound = false)
    public int getTabRunCount() {
        if (ui != null) {
            return ((TabbedPaneUI)ui).getTabRunCount(this);
        }
        return 0;
    }


// Getters for the Pages

    /**
     * Returns the tab title at <code>index</code>.
     *
     * @param index  the index of the item being queried
     * @return the title at <code>index</code>
     * @exception IndexOutOfBoundsException if index is out of range
     *            {@code (index < 0 || index >= tab count)}
     * @see #setTitleAt
     */
    public String getTitleAt(int index) {
        return pages.get(index).title;
    }

    /**
     * Returns the tab icon at <code>index</code>.
     *
     * @param index  the index of the item being queried
     * @return the icon at <code>index</code>
     * @exception IndexOutOfBoundsException if index is out of range
     *            {@code (index < 0 || index >= tab count)}
     *
     * @see #setIconAt
     */
    public Icon getIconAt(int index) {
        return pages.get(index).icon;
    }

    /**
     * Returns the tab disabled icon at <code>index</code>.
     * If the tab disabled icon doesn't exist at <code>index</code>
     * this will forward the call to the look and feel to construct
     * an appropriate disabled Icon from the corresponding enabled
     * Icon. Some look and feels might not render the disabled Icon,
     * in which case it won't be created.
     *
     * @param index  the index of the item being queried
     * @return the icon at <code>index</code>
     * @exception IndexOutOfBoundsException if index is out of range
     *            {@code (index < 0 || index >= tab count)}
     *
     * @see #setDisabledIconAt
     */
    public Icon getDisabledIconAt(int index) {
        Page page = pages.get(index);
        if (page.disabledIcon == null) {
            page.disabledIcon = UIManager.getLookAndFeel().getDisabledIcon(this, page.icon);
        }
        return page.disabledIcon;
    }

    /**
     * Returns the tab tooltip text at <code>index</code>.
     *
     * @param index  the index of the item being queried
     * @return a string containing the tool tip text at <code>index</code>
     * @exception IndexOutOfBoundsException if index is out of range
     *            {@code (index < 0 || index >= tab count)}
     *
     * @see #setToolTipTextAt
     * @since 1.3
     */
    public String getToolTipTextAt(int index) {
        return pages.get(index).tip;
    }

    /**
     * Returns the tab background color at <code>index</code>.
     *
     * @param index  the index of the item being queried
     * @return the <code>Color</code> of the tab background at
     *          <code>index</code>
     * @exception IndexOutOfBoundsException if index is out of range
     *            {@code (index < 0 || index >= tab count)}
     *
     * @see #setBackgroundAt
     */
    public Color getBackgroundAt(int index) {
        return pages.get(index).getBackground();
    }

    /**
     * Returns the tab foreground color at <code>index</code>.
     *
     * @param index  the index of the item being queried
     * @return the <code>Color</code> of the tab foreground at
     *          <code>index</code>
     * @exception IndexOutOfBoundsException if index is out of range
     *            {@code (index < 0 || index >= tab count)}
     *
     * @see #setForegroundAt
     */
    public Color getForegroundAt(int index) {
        return pages.get(index).getForeground();
    }

    /**
     * Returns whether or not the tab at <code>index</code> is
     * currently enabled.
     *
     * @param index  the index of the item being queried
     * @return true if the tab at <code>index</code> is enabled;
     *          false otherwise
     * @exception IndexOutOfBoundsException if index is out of range
     *            {@code (index < 0 || index >= tab count)}
     *
     * @see #setEnabledAt
     */
    public boolean isEnabledAt(int index) {
        return pages.get(index).isEnabled();
    }

    /**
     * Returns the component at <code>index</code>.
     *
     * @param index  the index of the item being queried
     * @return the <code>Component</code> at <code>index</code>
     * @exception IndexOutOfBoundsException if index is out of range
     *            {@code (index < 0 || index >= tab count)}
     *
     * @see #setComponentAt
     */
    public Component getComponentAt(int index) {
        return pages.get(index).component;
    }

    /**
     * Returns the keyboard mnemonic for accessing the specified tab.
     * The mnemonic is the key which when combined with the look and feel's
     * mouseless modifier (usually Alt) will activate the specified
     * tab.
     *
     * @since 1.4
     * @param tabIndex the index of the tab that the mnemonic refers to
     * @return the key code which represents the mnemonic;
     *         -1 if a mnemonic is not specified for the tab
     * @exception IndexOutOfBoundsException if index is out of range
     *            (<code>tabIndex</code> &lt; 0 ||
     *              <code>tabIndex</code> &gt;= tab count)
     * @see #setDisplayedMnemonicIndexAt(int,int)
     * @see #setMnemonicAt(int,int)
     */
    public int getMnemonicAt(int tabIndex) {
        checkIndex(tabIndex);

        Page page = pages.get(tabIndex);
        return page.getMnemonic();
    }

    /**
     * Returns the character, as an index, that the look and feel should
     * provide decoration for as representing the mnemonic character.
     *
     * @since 1.4
     * @param tabIndex the index of the tab that the mnemonic refers to
     * @return index representing mnemonic character if one exists;
     *    otherwise returns -1
     * @exception IndexOutOfBoundsException if index is out of range
     *            (<code>tabIndex</code> &lt; 0 ||
     *              <code>tabIndex</code> &gt;= tab count)
     * @see #setDisplayedMnemonicIndexAt(int,int)
     * @see #setMnemonicAt(int,int)
     */
    public int getDisplayedMnemonicIndexAt(int tabIndex) {
        checkIndex(tabIndex);

        Page page = pages.get(tabIndex);
        return page.getDisplayedMnemonicIndex();
    }

    /**
     * Returns the tab bounds at <code>index</code>.  If the tab at
     * this index is not currently visible in the UI, then returns
     * <code>null</code>.
     * If there is no UI set on this <code>tabbedpane</code>,
     * then returns <code>null</code>.
     *
     * @param index the index to be queried
     * @return a <code>Rectangle</code> containing the tab bounds at
     *          <code>index</code>, or <code>null</code> if tab at
     *          <code>index</code> is not currently visible in the UI,
     *          or if there is no UI set on this <code>tabbedpane</code>
     * @exception IndexOutOfBoundsException if index is out of range
     *            {@code (index < 0 || index >= tab count)}
     */
    public Rectangle getBoundsAt(int index) {
        checkIndex(index);
        if (ui != null) {
            return ((TabbedPaneUI)ui).getTabBounds(this, index);
        }
        return null;
    }


// Setters for the Pages

    /**
     * Sets the title at <code>index</code> to <code>title</code> which
     * can be <code>null</code>.
     * The title is not shown if a tab component for this tab was specified.
     * An internal exception is raised if there is no tab at that index.
     *
     * @param index the tab index where the title should be set
     * @param title the title to be displayed in the tab
     * @exception IndexOutOfBoundsException if index is out of range
     *            {@code (index < 0 || index >= tab count)}
     *
     * @see #getTitleAt
     * @see #setTabComponentAt
     */
    @BeanProperty(preferred = true, visualUpdate = true, description
            = "The title at the specified tab index.")
    public void setTitleAt(int index, String title) {
        Page page = pages.get(index);
        String oldTitle =page.title;
        page.title = title;

        if (oldTitle != title) {
            firePropertyChange("indexForTitle", -1, index);
        }
        page.updateDisplayedMnemonicIndex();
        if ((oldTitle != title) && (accessibleContext != null)) {
            accessibleContext.firePropertyChange(
                    AccessibleContext.ACCESSIBLE_VISIBLE_DATA_PROPERTY,
                    oldTitle, title);
        }
        if (title == null || oldTitle == null ||
            !title.equals(oldTitle)) {
            revalidate();
            repaint();
        }
    }

    /**
     * Sets the icon at <code>index</code> to <code>icon</code> which can be
     * <code>null</code>. This does not set disabled icon at <code>icon</code>.
     * If the new Icon is different than the current Icon and disabled icon
     * is not explicitly set, the LookAndFeel will be asked to generate a disabled
     * Icon. To explicitly set disabled icon, use <code>setDisableIconAt()</code>.
     * The icon is not shown if a tab component for this tab was specified.
     * An internal exception is raised if there is no tab at that index.
     *
     * @param index the tab index where the icon should be set
     * @param icon the icon to be displayed in the tab
     * @exception IndexOutOfBoundsException if index is out of range
     *            {@code (index < 0 || index >= tab count)}
     *
     * @see #setDisabledIconAt
     * @see #getIconAt
     * @see #getDisabledIconAt
     * @see #setTabComponentAt
     */
    @BeanProperty(preferred = true, visualUpdate = true, description
            = "The icon at the specified tab index.")
    public void setIconAt(int index, Icon icon) {
        Page page = pages.get(index);
        Icon oldIcon = page.icon;
        if (icon != oldIcon) {
            page.icon = icon;

            /* If the default icon has really changed and we had
             * generated the disabled icon for this page, then
             * clear the disabledIcon field of the page.
             */
            if (page.disabledIcon instanceof UIResource) {
                page.disabledIcon = null;
            }

            // Fire the accessibility Visible data change
            if (accessibleContext != null) {
                accessibleContext.firePropertyChange(
                        AccessibleContext.ACCESSIBLE_VISIBLE_DATA_PROPERTY,
                        oldIcon, icon);
            }
            revalidate();
            repaint();
        }
    }

    /**
     * Sets the disabled icon at <code>index</code> to <code>icon</code>
     * which can be <code>null</code>.
     * An internal exception is raised if there is no tab at that index.
     *
     * @param index the tab index where the disabled icon should be set
     * @param disabledIcon the icon to be displayed in the tab when disabled
     * @exception IndexOutOfBoundsException if index is out of range
     *            {@code (index < 0 || index >= tab count)}
     *
     * @see #getDisabledIconAt
     */
    @BeanProperty(preferred = true, visualUpdate = true, description
            = "The disabled icon at the specified tab index.")
    public void setDisabledIconAt(int index, Icon disabledIcon) {
        Icon oldIcon = pages.get(index).disabledIcon;
        pages.get(index).disabledIcon = disabledIcon;
        if (disabledIcon != oldIcon && !isEnabledAt(index)) {
            revalidate();
            repaint();
        }
    }

    /**
     * Sets the tooltip text at <code>index</code> to <code>toolTipText</code>
     * which can be <code>null</code>.
     * An internal exception is raised if there is no tab at that index.
     *
     * @param index the tab index where the tooltip text should be set
     * @param toolTipText the tooltip text to be displayed for the tab
     * @exception IndexOutOfBoundsException if index is out of range
     *            {@code (index < 0 || index >= tab count)}
     *
     * @see #getToolTipTextAt
     * @since 1.3
     */
    @BeanProperty(preferred = true, description
            = "The tooltip text at the specified tab index.")
    public void setToolTipTextAt(int index, String toolTipText) {
        String oldToolTipText = pages.get(index).tip;
        pages.get(index).tip = toolTipText;

        if ((oldToolTipText != toolTipText) && (accessibleContext != null)) {
            accessibleContext.firePropertyChange(
                    AccessibleContext.ACCESSIBLE_VISIBLE_DATA_PROPERTY,
                    oldToolTipText, toolTipText);
        }
        if (!haveRegistered && toolTipText != null) {
            ToolTipManager.sharedInstance().registerComponent(this);
            haveRegistered = true;
        }
    }

    /**
     * Sets the background color at <code>index</code> to
     * <code>background</code>
     * which can be <code>null</code>, in which case the tab's background color
     * will default to the background color of the <code>tabbedpane</code>.
     * An internal exception is raised if there is no tab at that index.
     * <p>
     * It is up to the look and feel to honor this property, some may
     * choose to ignore it.
     *
     * @param index the tab index where the background should be set
     * @param background the color to be displayed in the tab's background
     * @exception IndexOutOfBoundsException if index is out of range
     *            {@code (index < 0 || index >= tab count)}
     *
     * @see #getBackgroundAt
     */
    @BeanProperty(preferred = true, visualUpdate = true, description
            = "The background color at the specified tab index.")
    public void setBackgroundAt(int index, Color background) {
        Color oldBg = pages.get(index).background;
        pages.get(index).setBackground(background);
        if (background == null || oldBg == null ||
            !background.equals(oldBg)) {
            Rectangle tabBounds = getBoundsAt(index);
            if (tabBounds != null) {
                repaint(tabBounds);
            }
        }
    }

    /**
     * Sets the foreground color at <code>index</code> to
     * <code>foreground</code> which can be
     * <code>null</code>, in which case the tab's foreground color
     * will default to the foreground color of this <code>tabbedpane</code>.
     * An internal exception is raised if there is no tab at that index.
     * <p>
     * It is up to the look and feel to honor this property, some may
     * choose to ignore it.
     *
     * @param index the tab index where the foreground should be set
     * @param foreground the color to be displayed as the tab's foreground
     * @exception IndexOutOfBoundsException if index is out of range
     *            {@code (index < 0 || index >= tab count)}
     *
     * @see #getForegroundAt
     */
    @BeanProperty(preferred = true, visualUpdate = true, description
            = "The foreground color at the specified tab index.")
    public void setForegroundAt(int index, Color foreground) {
        Color oldFg = pages.get(index).foreground;
        pages.get(index).setForeground(foreground);
        if (foreground == null || oldFg == null ||
            !foreground.equals(oldFg)) {
            Rectangle tabBounds = getBoundsAt(index);
            if (tabBounds != null) {
                repaint(tabBounds);
            }
        }
    }

    /**
     * Sets whether or not the tab at <code>index</code> is enabled.
     * An internal exception is raised if there is no tab at that index.
     *
     * @param index the tab index which should be enabled/disabled
     * @param enabled whether or not the tab should be enabled
     * @exception IndexOutOfBoundsException if index is out of range
     *            {@code (index < 0 || index >= tab count)}
     *
     * @see #isEnabledAt
     */
    public void setEnabledAt(int index, boolean enabled) {
        boolean oldEnabled = pages.get(index).isEnabled();
        pages.get(index).setEnabled(enabled);
        if (enabled != oldEnabled) {
            revalidate();
            repaint();
        }
    }

    /**
     * Sets the component at <code>index</code> to <code>component</code>.
     * An internal exception is raised if there is no tab at that index.
     *
     * @param index the tab index where this component is being placed
     * @param component the component for the tab
     * @exception IndexOutOfBoundsException if index is out of range
     *            {@code (index < 0 || index >= tab count)}
     *
     * @see #getComponentAt
     */
    @BeanProperty(visualUpdate = true, description
            = "The component at the specified tab index.")
    @SuppressWarnings("deprecation")
    public void setComponentAt(int index, Component component) {
        Page page = pages.get(index);
        if (component != page.component) {
            boolean shouldChangeFocus = false;

            if (page.component != null) {
                shouldChangeFocus =
                    (SwingUtilities.findFocusOwner(page.component) != null);

                // REMIND(aim): this is really silly;
                // why not if (page.component.getParent() == this) remove(component)
                synchronized(getTreeLock()) {
                    int count = getComponentCount();
                    Component[] children = getComponents();
                    for (int i = 0; i < count; i++) {
                        if (children[i] == page.component) {
                            super.remove(i);
                            clearAccessibleParent(children[i]);
                        }
                    }
                }
            }

            page.component = component;
            boolean selectedPage = (getSelectedIndex() == index);

            if (selectedPage) {
                this.visComp = component;
            }

            if (component != null) {
                component.setVisible(selectedPage);
                addImpl(component, null, -1);

                if (shouldChangeFocus) {
                    SwingUtilities2.tabbedPaneChangeFocusTo(component);
                }
            } else {
                repaint();
            }

            revalidate();
        }
    }

    /**
     * Provides a hint to the look and feel as to which character in the
     * text should be decorated to represent the mnemonic. Not all look and
     * feels may support this. A value of -1 indicates either there is
     * no mnemonic for this tab, or you do not wish the mnemonic to be
     * displayed for this tab.
     * <p>
     * The value of this is updated as the properties relating to the
     * mnemonic change (such as the mnemonic itself, the text...).
     * You should only ever have to call this if
     * you do not wish the default character to be underlined. For example, if
     * the text at tab index 3 was 'Apple Price', with a mnemonic of 'p',
     * and you wanted the 'P'
     * to be decorated, as 'Apple <u>P</u>rice', you would have to invoke
     * <code>setDisplayedMnemonicIndex(3, 6)</code> after invoking
     * <code>setMnemonicAt(3, KeyEvent.VK_P)</code>.
     * <p>Note that it is the programmer's responsibility to ensure
     * that each tab has a unique mnemonic or unpredictable results may
     * occur.
     *
     * @since 1.4
     * @param tabIndex the index of the tab that the mnemonic refers to
     * @param mnemonicIndex index into the <code>String</code> to underline
     * @exception IndexOutOfBoundsException if <code>tabIndex</code> is
     *            out of range ({@code tabIndex < 0 || tabIndex >= tab
     *            count})
     * @exception IllegalArgumentException will be thrown if
     *            <code>mnemonicIndex</code> is &gt;= length of the tab
     *            title , or &lt; -1
     * @see #setMnemonicAt(int,int)
     * @see #getDisplayedMnemonicIndexAt(int)
     */
    @BeanProperty(visualUpdate = true, description
            = "the index into the String to draw the keyboard character mnemonic at")
    public void setDisplayedMnemonicIndexAt(int tabIndex, int mnemonicIndex) {
        checkIndex(tabIndex);

        Page page = pages.get(tabIndex);

        page.setDisplayedMnemonicIndex(mnemonicIndex);
    }

    /**
     * Sets the keyboard mnemonic for accessing the specified tab.
     * The mnemonic is the key which when combined with the look and feel's
     * mouseless modifier (usually Alt) will activate the specified
     * tab.
     * <p>
     * A mnemonic must correspond to a single key on the keyboard
     * and should be specified using one of the <code>VK_XXX</code>
     * keycodes defined in <code>java.awt.event.KeyEvent</code>
     * or one of the extended keycodes obtained through
     * <code>java.awt.event.KeyEvent.getExtendedKeyCodeForChar</code>.
     * Mnemonics are case-insensitive, therefore a key event
     * with the corresponding keycode would cause the button to be
     * activated whether or not the Shift modifier was pressed.
     * <p>
     * This will update the displayed mnemonic property for the specified
     * tab.
     *
     * @since 1.4
     * @param tabIndex the index of the tab that the mnemonic refers to
     * @param mnemonic the key code which represents the mnemonic
     * @exception IndexOutOfBoundsException if <code>tabIndex</code> is out
     *            of range ({@code tabIndex < 0 || tabIndex >= tab count})
     * @see #getMnemonicAt(int)
     * @see #setDisplayedMnemonicIndexAt(int,int)
     */
    @BeanProperty(visualUpdate = true, description
            = "The keyboard mnenmonic, as a KeyEvent VK constant, for the specified tab")
    public void setMnemonicAt(int tabIndex, int mnemonic) {
        checkIndex(tabIndex);

        Page page = pages.get(tabIndex);
        page.setMnemonic(mnemonic);

        firePropertyChange("mnemonicAt", null, null);
    }

// end of Page setters

    /**
     * Returns the first tab index with a given <code>title</code>,  or
     * -1 if no tab has this title.
     *
     * @param title the title for the tab
     * @return the first tab index which matches <code>title</code>, or
     *          -1 if no tab has this title
     */
    public int indexOfTab(String title) {
        for(int i = 0; i < getTabCount(); i++) {
            if (getTitleAt(i).equals(title == null? "" : title)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the first tab index with a given <code>icon</code>,
     * or -1 if no tab has this icon.
     *
     * @param icon the icon for the tab
     * @return the first tab index which matches <code>icon</code>,
     *          or -1 if no tab has this icon
     */
    public int indexOfTab(Icon icon) {
        for(int i = 0; i < getTabCount(); i++) {
            Icon tabIcon = getIconAt(i);
            if ((tabIcon != null && tabIcon.equals(icon)) ||
                (tabIcon == null && tabIcon == icon)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the index of the tab for the specified component.
     * Returns -1 if there is no tab for this component.
     *
     * @param component the component for the tab
     * @return the first tab which matches this component, or -1
     *          if there is no tab for this component
     */
    public int indexOfComponent(Component component) {
        for(int i = 0; i < getTabCount(); i++) {
            Component c = getComponentAt(i);
            if ((c != null && c.equals(component)) ||
                (c == null && c == component)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the tab index corresponding to the tab whose bounds
     * intersect the specified location.  Returns -1 if no tab
     * intersects the location.
     *
     * @param x the x location relative to this tabbedpane
     * @param y the y location relative to this tabbedpane
     * @return the tab index which intersects the location, or
     *         -1 if no tab intersects the location
     * @since 1.4
     */
    public int indexAtLocation(int x, int y) {
        if (ui != null) {
            return ((TabbedPaneUI)ui).tabForCoordinate(this, x, y);
        }
        return -1;
    }


    /**
     * Returns the tooltip text for the component determined by the
     * mouse event location.
     *
     * @param event  the <code>MouseEvent</code> that tells where the
     *          cursor is lingering
     * @return the <code>String</code> containing the tooltip text
     */
    public String getToolTipText(MouseEvent event) {
        if (ui != null) {
            int index = ((TabbedPaneUI)ui).tabForCoordinate(this, event.getX(), event.getY());

            if (index != -1) {
                return pages.get(index).tip;
            }
        }
        return super.getToolTipText(event);
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= pages.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Tab count: " + pages.size());
        }
    }


    /**
     * See <code>readObject</code> and <code>writeObject</code> in
     * <code>JComponent</code> for more
     * information about serialization in Swing.
     */
    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        if (getUIClassID().equals(uiClassID)) {
            byte count = JComponent.getWriteObjCounter(this);
            JComponent.setWriteObjCounter(this, --count);
            if (count == 0 && ui != null) {
                ui.installUI(this);
            }
        }
    }

    /* Called from the <code>JComponent</code>'s
     * <code>EnableSerializationFocusListener</code> to
     * do any Swing-specific pre-serialization configuration.
     */
    void compWriteObjectNotify() {
        super.compWriteObjectNotify();
        // If ToolTipText != null, then the tooltip has already been
        // unregistered by JComponent.compWriteObjectNotify()
        if (getToolTipText() == null && haveRegistered) {
            ToolTipManager.sharedInstance().unregisterComponent(this);
        }
    }

    /**
     * See <code>readObject</code> and <code>writeObject</code> in
     * <code>JComponent</code> for more
     * information about serialization in Swing.
     */
    @Serial
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream s)
        throws IOException, ClassNotFoundException
    {
        ObjectInputStream.GetField f = s.readFields();

        int newTabPlacement = f.get("tabPlacement", TOP);
        checkTabPlacement(newTabPlacement);
        tabPlacement = newTabPlacement;
        int newTabLayoutPolicy = f.get("tabLayoutPolicy", 0);
        checkTabLayoutPolicy(newTabLayoutPolicy);
        tabLayoutPolicy = newTabLayoutPolicy;
        model = (SingleSelectionModel) f.get("model", null);
        haveRegistered = f.get("haveRegistered", false);
        changeListener = (ChangeListener) f.get("changeListener", null);
        pages = (java.util.List<JTabbedPane.Page>) f.get("pages", null);
        visComp = (Component) f.get("visComp", null);

        if ((ui != null) && (getUIClassID().equals(uiClassID))) {
            ui.installUI(this);
        }
        // If ToolTipText != null, then the tooltip has already been
        // registered by JComponent.readObject()
        if (getToolTipText() == null && haveRegistered) {
            ToolTipManager.sharedInstance().registerComponent(this);
        }
    }


    /**
     * Returns a string representation of this <code>JTabbedPane</code>.
     * This method
     * is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     *
     * @return  a string representation of this JTabbedPane.
     */
    protected String paramString() {
        String tabPlacementString;
        if (tabPlacement == TOP) {
            tabPlacementString = "TOP";
        } else if (tabPlacement == BOTTOM) {
            tabPlacementString = "BOTTOM";
        } else if (tabPlacement == LEFT) {
            tabPlacementString = "LEFT";
        } else if (tabPlacement == RIGHT) {
            tabPlacementString = "RIGHT";
        } else tabPlacementString = "";
        String haveRegisteredString = (haveRegistered ?
                                       "true" : "false");

        return super.paramString() +
        ",haveRegistered=" + haveRegisteredString +
        ",tabPlacement=" + tabPlacementString;
    }

/////////////////
// Accessibility support
////////////////

    /**
     * Gets the AccessibleContext associated with this JTabbedPane.
     * For tabbed panes, the AccessibleContext takes the form of an
     * AccessibleJTabbedPane.
     * A new AccessibleJTabbedPane instance is created if necessary.
     *
     * @return an AccessibleJTabbedPane that serves as the
     *         AccessibleContext of this JTabbedPane
     */
    @BeanProperty(bound = false)
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJTabbedPane();

            // initialize AccessibleContext for the existing pages
            int count = getTabCount();
            for (int i = 0; i < count; i++) {
                pages.get(i).initAccessibleContext();
            }
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * <code>JTabbedPane</code> class.  It provides an implementation of the
     * Java Accessibility API appropriate to tabbed pane user-interface
     * elements.
     * <p>
     * <strong>Warning:</strong>
     * Serialized objects of this class will not be compatible with
     * future Swing releases. The current serialization support is
     * appropriate for short term storage or RMI between applications running
     * the same version of Swing.  As of 1.4, support for long term storage
     * of all JavaBeans
     * has been added to the <code>java.beans</code> package.
     * Please see {@link java.beans.XMLEncoder}.
     */
    @SuppressWarnings("serial") // Same-version serialization only
    protected class AccessibleJTabbedPane extends AccessibleJComponent
        implements AccessibleSelection, ChangeListener {

        /**
         * Returns the accessible name of this object, or {@code null} if
         * there is no accessible name.
         *
         * @return the accessible name of this object, or {@code null}.
         * @since 1.6
         */
        public String getAccessibleName() {
            if (accessibleName != null) {
                return accessibleName;
            }

            String cp = (String)getClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY);

            if (cp != null) {
                return cp;
            }

            int index = getSelectedIndex();

            if (index >= 0) {
                return pages.get(index).getAccessibleName();
            }

            return super.getAccessibleName();
        }

        /**
         *  Constructs an AccessibleJTabbedPane
         */
        public AccessibleJTabbedPane() {
            super();
            JTabbedPane.this.model.addChangeListener(this);
        }

        public void stateChanged(ChangeEvent e) {
            Object o = e.getSource();
            firePropertyChange(AccessibleContext.ACCESSIBLE_SELECTION_PROPERTY,
                               null, o);
        }

        /**
         * Get the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of
         *          the object
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.PAGE_TAB_LIST;
        }

        /**
         * Returns the number of accessible children in the object.
         *
         * @return the number of accessible children in the object.
         */
        public int getAccessibleChildrenCount() {
            return getTabCount();
        }

        /**
         * Return the specified Accessible child of the object.
         *
         * @param i zero-based index of child
         * @return the Accessible child of the object
         * @exception IllegalArgumentException if index is out of bounds
         */
        public Accessible getAccessibleChild(int i) {
            if (i < 0 || i >= getTabCount()) {
                return null;
            }
            return pages.get(i);
        }

        /**
         * Gets the <code>AccessibleSelection</code> associated with
         * this object.  In the implementation of the Java
         * Accessibility API for this class,
         * returns this object, which is responsible for implementing the
         * <code>AccessibleSelection</code> interface on behalf of itself.
         *
         * @return this object
         */
        public AccessibleSelection getAccessibleSelection() {
           return this;
        }

        /**
         * Returns the <code>Accessible</code> child contained at
         * the local coordinate <code>Point</code>, if one exists.
         * Otherwise returns the currently selected tab.
         *
         * @return the <code>Accessible</code> at the specified
         *    location, if it exists
         */
        public Accessible getAccessibleAt(Point p) {
            int tab = ((TabbedPaneUI) ui).tabForCoordinate(JTabbedPane.this,
                                                           p.x, p.y);
            if (tab == -1) {
                tab = getSelectedIndex();
            }
            return getAccessibleChild(tab);
        }

        public int getAccessibleSelectionCount() {
            return 1;
        }

        public Accessible getAccessibleSelection(int i) {
            int index = getSelectedIndex();
            if (index == -1) {
                return null;
            }
            return pages.get(index);
        }

        public boolean isAccessibleChildSelected(int i) {
            return (i == getSelectedIndex());
        }

        public void addAccessibleSelection(int i) {
           setSelectedIndex(i);
        }

        public void removeAccessibleSelection(int i) {
           // can't do
        }

        public void clearAccessibleSelection() {
           // can't do
        }

        public void selectAllAccessibleSelection() {
           // can't do
        }
    }

    private class Page extends AccessibleContext
        implements Serializable, Accessible, AccessibleComponent {
        String title;
        Color background;
        Color foreground;
        Icon icon;
        Icon disabledIcon;
        JTabbedPane parent;
        Component component;
        String tip;
        boolean enabled = true;
        boolean needsUIUpdate;
        int mnemonic = -1;
        int mnemonicIndex = -1;
        Component tabComponent;

        Page(JTabbedPane parent,
             String title, Icon icon, Icon disabledIcon, Component component, String tip) {
            this.title = title;
            this.icon = icon;
            this.disabledIcon = disabledIcon;
            this.parent = parent;
            this.setAccessibleParent(parent);
            this.component = component;
            this.tip = tip;

            initAccessibleContext();
        }

        /*
         * initializes the AccessibleContext for the page
         */
        void initAccessibleContext() {
            if (JTabbedPane.this.accessibleContext != null &&
                component instanceof Accessible) {
                /*
                 * Do initialization if the AccessibleJTabbedPane
                 * has been instantiated. We do not want to load
                 * Accessibility classes unnecessarily.
                 */
                AccessibleContext ac;
                ac = component.getAccessibleContext();
                if (ac != null) {
                    ac.setAccessibleParent(this);
                }
            }
        }

        void setMnemonic(int mnemonic) {
            this.mnemonic = mnemonic;
            updateDisplayedMnemonicIndex();
        }

        int getMnemonic() {
            return mnemonic;
        }

        /*
         * Sets the page displayed mnemonic index
         */
        void setDisplayedMnemonicIndex(int mnemonicIndex) {
            if (this.mnemonicIndex != mnemonicIndex) {
                String t = getTitle();
                if (mnemonicIndex != -1 && (t == null ||
                        mnemonicIndex < 0 ||
                        mnemonicIndex >= t.length())) {
                    throw new IllegalArgumentException(
                                "Invalid mnemonic index: " + mnemonicIndex);
                }
                this.mnemonicIndex = mnemonicIndex;
                JTabbedPane.this.firePropertyChange("displayedMnemonicIndexAt",
                                                    null, null);
            }
        }

        /*
         * Returns the page displayed mnemonic index
         */
        int getDisplayedMnemonicIndex() {
            return this.mnemonicIndex;
        }

        void updateDisplayedMnemonicIndex() {
            setDisplayedMnemonicIndex(
                SwingUtilities.findDisplayedMnemonicIndex(getTitle(), mnemonic));
        }

        /////////////////
        // Accessibility support
        ////////////////

        public AccessibleContext getAccessibleContext() {
            return this;
        }


        // AccessibleContext methods

        public String getAccessibleName() {
            if (accessibleName != null) {
                return accessibleName;
            } else {
                return getTitle();
            }
        }

        public String getAccessibleDescription() {
            if (accessibleDescription != null) {
                return accessibleDescription;
            } else if (tip != null) {
                return tip;
            }
            return null;
        }

        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.PAGE_TAB;
        }

        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet states;
            states = parent.getAccessibleContext().getAccessibleStateSet();
            states.add(AccessibleState.SELECTABLE);
            if (getPageIndex() == parent.getSelectedIndex()) {
                states.add(AccessibleState.SELECTED);
            }
            return states;
        }

        public int getAccessibleIndexInParent() {
            return getPageIndex();
        }

        public int getAccessibleChildrenCount() {
            if (component instanceof Accessible) {
                return 1;
            } else {
                return 0;
            }
        }

        public Accessible getAccessibleChild(int i) {
            if (component instanceof Accessible) {
                return (Accessible) component;
            } else {
                return null;
            }
        }

        public Locale getLocale() {
            return parent.getLocale();
        }

        public AccessibleComponent getAccessibleComponent() {
            return this;
        }


        // AccessibleComponent methods

        public Color getBackground() {
            return background != null? background : parent.getBackground();
        }

        public void setBackground(Color c) {
            background = c;
        }

        public Color getForeground() {
            return foreground != null? foreground : parent.getForeground();
        }

        public void setForeground(Color c) {
            foreground = c;
        }

        public Cursor getCursor() {
            return parent.getCursor();
        }

        public void setCursor(Cursor c) {
            parent.setCursor(c);
        }

        public Font getFont() {
            return parent.getFont();
        }

        public void setFont(Font f) {
            parent.setFont(f);
        }

        public FontMetrics getFontMetrics(Font f) {
            return parent.getFontMetrics(f);
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean b) {
            enabled = b;
        }

        public boolean isVisible() {
            return parent.isVisible();
        }

        public void setVisible(boolean b) {
            parent.setVisible(b);
        }

        public boolean isShowing() {
            return parent.isShowing();
        }

        public boolean contains(Point p) {
            Rectangle r = getBounds();
            return r.contains(p);
        }

        public Point getLocationOnScreen() {
             Point parentLocation = parent.getLocationOnScreen();
             Point componentLocation = getLocation();
             componentLocation.translate(parentLocation.x, parentLocation.y);
             return componentLocation;
        }

        public Point getLocation() {
             Rectangle r = getBounds();
             return new Point(r.x, r.y);
        }

        public void setLocation(Point p) {
            // do nothing
        }

        public Rectangle getBounds() {
            return parent.getUI().getTabBounds(parent, getPageIndex());
        }

        public void setBounds(Rectangle r) {
            // do nothing
        }

        public Dimension getSize() {
            Rectangle r = getBounds();
            return new Dimension(r.width, r.height);
        }

        public void setSize(Dimension d) {
            // do nothing
        }

        public Accessible getAccessibleAt(Point p) {
            if (component instanceof Accessible) {
                return (Accessible) component;
            } else {
                return null;
            }
        }

        public boolean isFocusTraversable() {
            return false;
        }

        public void requestFocus() {
            // do nothing
        }

        public void addFocusListener(FocusListener l) {
            // do nothing
        }

        public void removeFocusListener(FocusListener l) {
            // do nothing
        }

        // TIGER - 4732339
        /**
         * Returns an AccessibleIcon
         *
         * @return the enabled icon if one exists and the page
         * is enabled. Otherwise, returns the disabled icon if
         * one exists and the page is disabled.  Otherwise, null
         * is returned.
         */
        public AccessibleIcon [] getAccessibleIcon() {
            AccessibleIcon accessibleIcon = null;
            if (enabled && icon instanceof ImageIcon) {
                AccessibleContext ac =
                    ((ImageIcon)icon).getAccessibleContext();
                accessibleIcon = (AccessibleIcon)ac;
            } else if (!enabled && disabledIcon instanceof ImageIcon) {
                AccessibleContext ac =
                    ((ImageIcon)disabledIcon).getAccessibleContext();
                accessibleIcon = (AccessibleIcon)ac;
            }
            if (accessibleIcon != null) {
                AccessibleIcon [] returnIcons = new AccessibleIcon[1];
                returnIcons[0] = accessibleIcon;
                return returnIcons;
            } else {
                return null;
            }
        }

        private String getTitle() {
            return getTitleAt(getPageIndex());
        }

        /*
         * getPageIndex() has three valid scenarios:
         * - null component and null tabComponent: use indexOfcomponent
         * - non-null component: use indexOfComponent
         * - null component and non-null tabComponent: use indexOfTabComponent
         *
         * Note: It's valid to have have a titled tab with a null component, e.g.
         *   myPane.add("my title", null);
         * but it's only useful to have one of those because indexOfComponent(null)
         * will find the first one.
         *
         * Note: indexofTab(title) is not useful because there are cases, due to
         * subclassing, where Page.title is not set and title is managed in a subclass
         * and fetched with an overridden JTabbedPane.getTitleAt(index).
         */
        private int getPageIndex() {
            int index;
            if (component != null || (component == null && tabComponent == null)) {
                index = parent.indexOfComponent(component);
            } else {
                // component is null, tabComponent is non-null
                index = parent.indexOfTabComponent(tabComponent);
            }
            return index;
        }

    }

    /**
    * Sets the component that is responsible for rendering the
    * title for the specified tab.  A null value means
    * <code>JTabbedPane</code> will render the title and/or icon for
    * the specified tab.  A non-null value means the component will
    * render the title and <code>JTabbedPane</code> will not render
    * the title and/or icon.
    * <p>
    * Note: The component must not be one that the developer has
    *       already added to the tabbed pane.
    *
    * @param index the tab index where the component should be set
    * @param component the component to render the title for the
    *                  specified tab
    * @exception IndexOutOfBoundsException if index is out of range
    *            {@code (index < 0 || index >= tab count)}
    * @exception IllegalArgumentException if component has already been
    *            added to this <code>JTabbedPane</code>
    *
    * @see #getTabComponentAt
    * @since 1.6
    */
    @BeanProperty(preferred = true, visualUpdate = true, description
            = "The tab component at the specified tab index.")
    public void setTabComponentAt(int index, Component component) {
        if (component != null && indexOfComponent(component) != -1) {
            throw new IllegalArgumentException("Component is already added to this JTabbedPane");
        }
        Component oldValue = getTabComponentAt(index);
        if (component != oldValue) {
            int tabComponentIndex = indexOfTabComponent(component);
            if (tabComponentIndex != -1) {
                setTabComponentAt(tabComponentIndex, null);
            }
            pages.get(index).tabComponent = component;
            firePropertyChange("indexForTabComponent", -1, index);
        }
    }

    /**
     * Returns the tab component at <code>index</code>.
     *
     * @param index  the index of the item being queried
     * @return the tab component at <code>index</code>
     * @exception IndexOutOfBoundsException if index is out of range
     *            {@code (index < 0 || index >= tab count)}
     *
     * @see #setTabComponentAt
     * @since 1.6
     */
    public Component getTabComponentAt(int index) {
        return pages.get(index).tabComponent;
    }

    /**
     * Returns the index of the tab for the specified tab component.
     * Returns -1 if there is no tab for this tab component.
     *
     * @param tabComponent the tab component for the tab
     * @return the first tab which matches this tab component, or -1
     *          if there is no tab for this tab component
     * @see #setTabComponentAt
     * @see #getTabComponentAt
     * @since 1.6
     */
     public int indexOfTabComponent(Component tabComponent) {
        for(int i = 0; i < getTabCount(); i++) {
            Component c = getTabComponentAt(i);
            if (c == tabComponent) {
                return i;
            }
        }
        return -1;
    }
}
