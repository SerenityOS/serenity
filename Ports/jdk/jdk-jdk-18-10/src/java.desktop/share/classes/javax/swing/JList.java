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
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.IllegalComponentStateException;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.beans.BeanProperty;
import java.beans.JavaBean;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.Transient;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleComponent;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleIcon;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleSelection;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleText;
import javax.accessibility.AccessibleValue;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.ListUI;
import javax.swing.text.Position;

import sun.awt.AWTAccessor;
import sun.awt.AWTAccessor.MouseEventAccessor;
import sun.swing.SwingUtilities2;
import sun.swing.SwingUtilities2.Section;

import static sun.swing.SwingUtilities2.Section.LEADING;
import static sun.swing.SwingUtilities2.Section.TRAILING;

/**
 * A component that displays a list of objects and allows the user to select
 * one or more items. A separate model, {@code ListModel}, maintains the
 * contents of the list.
 * <p>
 * It's easy to display an array or Vector of objects, using the {@code JList}
 * constructor that automatically builds a read-only {@code ListModel} instance
 * for you:
 * <pre>
 * {@code
 * // Create a JList that displays strings from an array
 *
 * String[] data = {"one", "two", "three", "four"};
 * JList<String> myList = new JList<String>(data);
 *
 * // Create a JList that displays the superclasses of JList.class, by
 * // creating it with a Vector populated with this data
 *
 * Vector<Class<?>> superClasses = new Vector<Class<?>>();
 * Class<JList> rootClass = javax.swing.JList.class;
 * for(Class<?> cls = rootClass; cls != null; cls = cls.getSuperclass()) {
 *     superClasses.addElement(cls);
 * }
 * JList<Class<?>> myList = new JList<Class<?>>(superClasses);
 *
 * // The automatically created model is stored in JList's "model"
 * // property, which you can retrieve
 *
 * ListModel<Class<?>> model = myList.getModel();
 * for(int i = 0; i < model.getSize(); i++) {
 *     System.out.println(model.getElementAt(i));
 * }
 * }
 * </pre>
 * <p>
 * A {@code ListModel} can be supplied directly to a {@code JList} by way of a
 * constructor or the {@code setModel} method. The contents need not be static -
 * the number of items, and the values of items can change over time. A correct
 * {@code ListModel} implementation notifies the set of
 * {@code javax.swing.event.ListDataListener}s that have been added to it, each
 * time a change occurs. These changes are characterized by a
 * {@code javax.swing.event.ListDataEvent}, which identifies the range of list
 * indices that have been modified, added, or removed. {@code JList}'s
 * {@code ListUI} is responsible for keeping the visual representation up to
 * date with changes, by listening to the model.
 * <p>
 * Simple, dynamic-content, {@code JList} applications can use the
 * {@code DefaultListModel} class to maintain list elements. This class
 * implements the {@code ListModel} interface and also provides a
 * <code>java.util.Vector</code>-like API. Applications that need a more
 * custom <code>ListModel</code> implementation may instead wish to subclass
 * {@code AbstractListModel}, which provides basic support for managing and
 * notifying listeners. For example, a read-only implementation of
 * {@code AbstractListModel}:
 * <pre>
 * {@code
 * // This list model has about 2^16 elements.  Enjoy scrolling.
 *
 * ListModel<String> bigData = new AbstractListModel<String>() {
 *     public int getSize() { return Short.MAX_VALUE; }
 *     public String getElementAt(int index) { return "Index " + index; }
 * };
 * }
 * </pre>
 * <p>
 * The selection state of a {@code JList} is managed by another separate
 * model, an instance of {@code ListSelectionModel}. {@code JList} is
 * initialized with a selection model on construction, and also contains
 * methods to query or set this selection model. Additionally, {@code JList}
 * provides convenient methods for easily managing the selection. These methods,
 * such as {@code setSelectedIndex} and {@code getSelectedValue}, are cover
 * methods that take care of the details of interacting with the selection
 * model. By default, {@code JList}'s selection model is configured to allow any
 * combination of items to be selected at a time; selection mode
 * {@code MULTIPLE_INTERVAL_SELECTION}. The selection mode can be changed
 * on the selection model directly, or via {@code JList}'s cover method.
 * Responsibility for updating the selection model in response to user gestures
 * lies with the list's {@code ListUI}.
 * <p>
 * A correct {@code ListSelectionModel} implementation notifies the set of
 * {@code javax.swing.event.ListSelectionListener}s that have been added to it
 * each time a change to the selection occurs. These changes are characterized
 * by a {@code javax.swing.event.ListSelectionEvent}, which identifies the range
 * of the selection change.
 * <p>
 * The preferred way to listen for changes in list selection is to add
 * {@code ListSelectionListener}s directly to the {@code JList}. {@code JList}
 * then takes care of listening to the selection model and notifying your
 * listeners of change.
 * <p>
 * Responsibility for listening to selection changes in order to keep the list's
 * visual representation up to date lies with the list's {@code ListUI}.
 * <p>
 * <a id="renderer"></a>
 * Painting of cells in a {@code JList} is handled by a delegate called a
 * cell renderer, installed on the list as the {@code cellRenderer} property.
 * The renderer provides a {@code java.awt.Component} that is used
 * like a "rubber stamp" to paint the cells. Each time a cell needs to be
 * painted, the list's {@code ListUI} asks the cell renderer for the component,
 * moves it into place, and has it paint the contents of the cell by way of its
 * {@code paint} method. A default cell renderer, which uses a {@code JLabel}
 * component to render, is installed by the lists's {@code ListUI}. You can
 * substitute your own renderer using code like this:
 * <pre>
 * {@code
 *  // Display an icon and a string for each object in the list.
 *
 * class MyCellRenderer extends JLabel implements ListCellRenderer<Object> {
 *     static final ImageIcon longIcon = new ImageIcon("long.gif");
 *     static final ImageIcon shortIcon = new ImageIcon("short.gif");
 *
 *     // This is the only method defined by ListCellRenderer.
 *     // We just reconfigure the JLabel each time we're called.
 *
 *     public Component getListCellRendererComponent(
 *       JList<?> list,           // the list
 *       Object value,            // value to display
 *       int index,               // cell index
 *       boolean isSelected,      // is the cell selected
 *       boolean cellHasFocus)    // does the cell have focus
 *     {
 *         String s = value.toString();
 *         setText(s);
 *         setIcon((s.length() > 10) ? longIcon : shortIcon);
 *         if (isSelected) {
 *             setBackground(list.getSelectionBackground());
 *             setForeground(list.getSelectionForeground());
 *         } else {
 *             setBackground(list.getBackground());
 *             setForeground(list.getForeground());
 *         }
 *         setEnabled(list.isEnabled());
 *         setFont(list.getFont());
 *         setOpaque(true);
 *         return this;
 *     }
 * }
 *
 * myList.setCellRenderer(new MyCellRenderer());
 * }
 * </pre>
 * <p>
 * Another job for the cell renderer is in helping to determine sizing
 * information for the list. By default, the list's {@code ListUI} determines
 * the size of cells by asking the cell renderer for its preferred
 * size for each list item. This can be expensive for large lists of items.
 * To avoid these calculations, you can set a {@code fixedCellWidth} and
 * {@code fixedCellHeight} on the list, or have these values calculated
 * automatically based on a single prototype value:
 * <a id="prototype_example"></a>
 * <pre>
 * {@code
 * JList<String> bigDataList = new JList<String>(bigData);
 *
 * // We don't want the JList implementation to compute the width
 * // or height of all of the list cells, so we give it a string
 * // that's as big as we'll need for any cell.  It uses this to
 * // compute values for the fixedCellWidth and fixedCellHeight
 * // properties.
 *
 * bigDataList.setPrototypeCellValue("Index 1234567890");
 * }
 * </pre>
 * <p>
 * {@code JList} doesn't implement scrolling directly. To create a list that
 * scrolls, make it the viewport view of a {@code JScrollPane}. For example:
 * <pre>
 * JScrollPane scrollPane = new JScrollPane(myList);
 *
 * // Or in two steps:
 * JScrollPane scrollPane = new JScrollPane();
 * scrollPane.getViewport().setView(myList);
 * </pre>
 * <p>
 * {@code JList} doesn't provide any special handling of double or triple
 * (or N) mouse clicks, but it's easy to add a {@code MouseListener} if you
 * wish to take action on these events. Use the {@code locationToIndex}
 * method to determine what cell was clicked. For example:
 * <pre>
 * MouseListener mouseListener = new MouseAdapter() {
 *     public void mouseClicked(MouseEvent e) {
 *         if (e.getClickCount() == 2) {
 *             int index = list.locationToIndex(e.getPoint());
 *             System.out.println("Double clicked on Item " + index);
 *          }
 *     }
 * };
 * list.addMouseListener(mouseListener);
 * </pre>
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
 * <p>
 * See <a href="https://docs.oracle.com/javase/tutorial/uiswing/components/list.html">How to Use Lists</a>
 * in <a href="https://docs.oracle.com/javase/tutorial/"><em>The Java Tutorial</em></a>
 * for further documentation.
 *
 * @see ListModel
 * @see AbstractListModel
 * @see DefaultListModel
 * @see ListSelectionModel
 * @see DefaultListSelectionModel
 * @see ListCellRenderer
 * @see DefaultListCellRenderer
 *
 * @param <E> the type of the elements of this list
 *
 * @author Hans Muller
 * @since 1.2
 */
@JavaBean(defaultProperty = "UI", description = "A component which allows for the selection of one or more objects from a list.")
@SwingContainer(false)
@SuppressWarnings("serial") // Same-version serialization only
public class JList<E> extends JComponent implements Scrollable, Accessible
{
    /**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "ListUI";

    /**
     * Indicates a vertical layout of cells, in a single column;
     * the default layout.
     * @see #setLayoutOrientation
     * @since 1.4
     */
    public static final int VERTICAL = 0;

    /**
     * Indicates a "newspaper style" layout with cells flowing vertically
     * then horizontally.
     * @see #setLayoutOrientation
     * @since 1.4
     */
    public static final int VERTICAL_WRAP = 1;

    /**
     * Indicates a "newspaper style" layout with cells flowing horizontally
     * then vertically.
     * @see #setLayoutOrientation
     * @since 1.4
     */
    public static final int HORIZONTAL_WRAP = 2;

    private int fixedCellWidth = -1;
    private int fixedCellHeight = -1;
    private int horizontalScrollIncrement = -1;
    private E prototypeCellValue;
    private int visibleRowCount = 8;
    private Color selectionForeground;
    private Color selectionBackground;
    private boolean dragEnabled;

    private ListSelectionModel selectionModel;
    private ListModel<E> dataModel;
    private ListCellRenderer<? super E> cellRenderer;
    private ListSelectionListener selectionListener;

    /**
     * How to lay out the cells; defaults to <code>VERTICAL</code>.
     */
    private int layoutOrientation;

    /**
     * The drop mode for this component.
     */
    private DropMode dropMode = DropMode.USE_SELECTION;

    /**
     * The drop location.
     */
    private transient DropLocation dropLocation;

    /**
     * Flag to indicate UI update is in progress
     */
    private transient boolean updateInProgress;

    /**
     * A subclass of <code>TransferHandler.DropLocation</code> representing
     * a drop location for a <code>JList</code>.
     *
     * @see #getDropLocation
     * @since 1.6
     */
    public static final class DropLocation extends TransferHandler.DropLocation {
        private final int index;
        private final boolean isInsert;

        private DropLocation(Point p, int index, boolean isInsert) {
            super(p);
            this.index = index;
            this.isInsert = isInsert;
        }

        /**
         * Returns the index where dropped data should be placed in the
         * list. Interpretation of the value depends on the drop mode set on
         * the associated component. If the drop mode is either
         * <code>DropMode.USE_SELECTION</code> or <code>DropMode.ON</code>,
         * the return value is an index of a row in the list. If the drop mode is
         * <code>DropMode.INSERT</code>, the return value refers to the index
         * where the data should be inserted. If the drop mode is
         * <code>DropMode.ON_OR_INSERT</code>, the value of
         * <code>isInsert()</code> indicates whether the index is an index
         * of a row, or an insert index.
         * <p>
         * <code>-1</code> indicates that the drop occurred over empty space,
         * and no index could be calculated.
         *
         * @return the drop index
         */
        public int getIndex() {
            return index;
        }

        /**
         * Returns whether or not this location represents an insert
         * location.
         *
         * @return whether or not this is an insert location
         */
        public boolean isInsert() {
            return isInsert;
        }

        /**
         * Returns a string representation of this drop location.
         * This method is intended to be used for debugging purposes,
         * and the content and format of the returned string may vary
         * between implementations.
         *
         * @return a string representation of this drop location
         */
        public String toString() {
            return getClass().getName()
                   + "[dropPoint=" + getDropPoint() + ","
                   + "index=" + index + ","
                   + "insert=" + isInsert + "]";
        }
    }

    /**
     * Constructs a {@code JList} that displays elements from the specified,
     * {@code non-null}, model. All {@code JList} constructors delegate to
     * this one.
     * <p>
     * This constructor registers the list with the {@code ToolTipManager},
     * allowing for tooltips to be provided by the cell renderers.
     *
     * @param dataModel the model for the list
     * @exception IllegalArgumentException if the model is {@code null}
     */
    public JList(ListModel<E> dataModel)
    {
        if (dataModel == null) {
            throw new IllegalArgumentException("dataModel must be non null");
        }

        // Register with the ToolTipManager so that tooltips from the
        // renderer show through.
        ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
        toolTipManager.registerComponent(this);

        layoutOrientation = VERTICAL;

        this.dataModel = dataModel;
        selectionModel = createSelectionModel();
        setAutoscrolls(true);
        updateUI();
    }


    /**
     * Constructs a <code>JList</code> that displays the elements in
     * the specified array. This constructor creates a read-only model
     * for the given array, and then delegates to the constructor that
     * takes a {@code ListModel}.
     * <p>
     * Attempts to pass a {@code null} value to this method results in
     * undefined behavior and, most likely, exceptions. The created model
     * references the given array directly. Attempts to modify the array
     * after constructing the list results in undefined behavior.
     *
     * @param  listData  the array of Objects to be loaded into the data model,
     *                   {@code non-null}
     */
    public JList(final E[] listData)
    {
        this (
            new AbstractListModel<E>() {
                public int getSize() { return listData.length; }
                public E getElementAt(int i) { return listData[i]; }
            }
        );
    }


    /**
     * Constructs a <code>JList</code> that displays the elements in
     * the specified <code>Vector</code>. This constructor creates a read-only
     * model for the given {@code Vector}, and then delegates to the constructor
     * that takes a {@code ListModel}.
     * <p>
     * Attempts to pass a {@code null} value to this method results in
     * undefined behavior and, most likely, exceptions. The created model
     * references the given {@code Vector} directly. Attempts to modify the
     * {@code Vector} after constructing the list results in undefined behavior.
     *
     * @param  listData  the <code>Vector</code> to be loaded into the
     *                   data model, {@code non-null}
     */
    public JList(final Vector<? extends E> listData) {
        this (
            new AbstractListModel<E>() {
                public int getSize() { return listData.size(); }
                public E getElementAt(int i) { return listData.elementAt(i); }
            }
        );
    }


    /**
     * Constructs a <code>JList</code> with an empty, read-only, model.
     */
    public JList() {
        this (
            new AbstractListModel<E>() {
              public int getSize() { return 0; }
              public E getElementAt(int i) { throw new IndexOutOfBoundsException("No Data Model"); }
            }
        );
    }


    /**
     * Returns the {@code ListUI}, the look and feel object that
     * renders this component.
     *
     * @return the <code>ListUI</code> object that renders this component
     */
    public ListUI getUI() {
        return (ListUI)ui;
    }


    /**
     * Sets the {@code ListUI}, the look and feel object that
     * renders this component.
     *
     * @param ui  the <code>ListUI</code> object
     * @see UIDefaults#getUI
     */
    @BeanProperty(hidden = true, visualUpdate = true, description
            = "The UI object that implements the Component's LookAndFeel.")
    public void setUI(ListUI ui) {
        super.setUI(ui);
    }


    /**
     * Resets the {@code ListUI} property by setting it to the value provided
     * by the current look and feel. If the current cell renderer was installed
     * by the developer (rather than the look and feel itself), this also causes
     * the cell renderer and its children to be updated, by calling
     * {@code SwingUtilities.updateComponentTreeUI} on it.
     *
     * @see UIManager#getUI
     * @see SwingUtilities#updateComponentTreeUI
     */
    public void updateUI() {
        if (!updateInProgress) {
            updateInProgress = true;
            try {
                setUI((ListUI)UIManager.getUI(this));

                ListCellRenderer<? super E> renderer = getCellRenderer();
                if (renderer instanceof Component) {
                    SwingUtilities.updateComponentTreeUI((Component)renderer);
                }
            } finally {
                updateInProgress = false;
            }
        }
    }


    /**
     * Returns {@code "ListUI"}, the <code>UIDefaults</code> key used to look
     * up the name of the {@code javax.swing.plaf.ListUI} class that defines
     * the look and feel for this component.
     *
     * @return the string "ListUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    @BeanProperty(bound = false)
    public String getUIClassID() {
        return uiClassID;
    }


    /* -----private-----
     * This method is called by setPrototypeCellValue and setCellRenderer
     * to update the fixedCellWidth and fixedCellHeight properties from the
     * current value of prototypeCellValue (if it's non null).
     * <p>
     * This method sets fixedCellWidth and fixedCellHeight but does <b>not</b>
     * generate PropertyChangeEvents for them.
     *
     * @see #setPrototypeCellValue
     * @see #setCellRenderer
     */
    private void updateFixedCellSize()
    {
        ListCellRenderer<? super E> cr = getCellRenderer();
        E value = getPrototypeCellValue();

        if ((cr != null) && (value != null)) {
            Component c = cr.getListCellRendererComponent(this, value, 0, false, false);

            /* The ListUI implementation will add Component c to its private
             * CellRendererPane however we can't assume that's already
             * been done here.  So we temporarily set the one "inherited"
             * property that may affect the renderer components preferred size:
             * its font.
             */
            Font f = c.getFont();
            c.setFont(getFont());

            Dimension d = c.getPreferredSize();
            fixedCellWidth = d.width;
            fixedCellHeight = d.height;

            c.setFont(f);
        }
    }


    /**
     * Returns the "prototypical" cell value -- a value used to calculate a
     * fixed width and height for cells. This can be {@code null} if there
     * is no such value.
     *
     * @return the value of the {@code prototypeCellValue} property
     * @see #setPrototypeCellValue
     */
    public E getPrototypeCellValue() {
        return prototypeCellValue;
    }

    /**
     * Sets the {@code prototypeCellValue} property, and then (if the new value
     * is {@code non-null}), computes the {@code fixedCellWidth} and
     * {@code fixedCellHeight} properties by requesting the cell renderer
     * component for the given value (and index 0) from the cell renderer, and
     * using that component's preferred size.
     * <p>
     * This method is useful when the list is too long to allow the
     * {@code ListUI} to compute the width/height of each cell, and there is a
     * single cell value that is known to occupy as much space as any of the
     * others, a so-called prototype.
     * <p>
     * While all three of the {@code prototypeCellValue},
     * {@code fixedCellHeight}, and {@code fixedCellWidth} properties may be
     * modified by this method, {@code PropertyChangeEvent} notifications are
     * only sent when the {@code prototypeCellValue} property changes.
     * <p>
     * To see an example which sets this property, see the
     * <a href="#prototype_example">class description</a> above.
     * <p>
     * The default value of this property is <code>null</code>.
     * <p>
     * This is a JavaBeans bound property.
     *
     * @param prototypeCellValue  the value on which to base
     *                          <code>fixedCellWidth</code> and
     *                          <code>fixedCellHeight</code>
     * @see #getPrototypeCellValue
     * @see #setFixedCellWidth
     * @see #setFixedCellHeight
     * @see JComponent#addPropertyChangeListener
     */
    @BeanProperty(visualUpdate = true, description
            = "The cell prototype value, used to compute cell width and height.")
    public void setPrototypeCellValue(E prototypeCellValue) {
        E oldValue = this.prototypeCellValue;
        this.prototypeCellValue = prototypeCellValue;

        /* If the prototypeCellValue has changed and is non-null,
         * then recompute fixedCellWidth and fixedCellHeight.
         */

        if ((prototypeCellValue != null) && !prototypeCellValue.equals(oldValue)) {
            updateFixedCellSize();
        }

        firePropertyChange("prototypeCellValue", oldValue, prototypeCellValue);
    }


    /**
     * Returns the value of the {@code fixedCellWidth} property.
     *
     * @return the fixed cell width
     * @see #setFixedCellWidth
     */
    public int getFixedCellWidth() {
        return fixedCellWidth;
    }

    /**
     * Sets a fixed value to be used for the width of every cell in the list.
     * If {@code width} is -1, cell widths are computed in the {@code ListUI}
     * by applying <code>getPreferredSize</code> to the cell renderer component
     * for each list element.
     * <p>
     * The default value of this property is {@code -1}.
     * <p>
     * This is a JavaBeans bound property.
     *
     * @param width the width to be used for all cells in the list
     * @see #setPrototypeCellValue
     * @see #setFixedCellWidth
     * @see JComponent#addPropertyChangeListener
     */
    @BeanProperty(visualUpdate = true, description
            = "Defines a fixed cell width when greater than zero.")
    public void setFixedCellWidth(int width) {
        int oldValue = fixedCellWidth;
        fixedCellWidth = width;
        firePropertyChange("fixedCellWidth", oldValue, fixedCellWidth);
    }


    /**
     * Returns the value of the {@code fixedCellHeight} property.
     *
     * @return the fixed cell height
     * @see #setFixedCellHeight
     */
    public int getFixedCellHeight() {
        return fixedCellHeight;
    }

    /**
     * Sets a fixed value to be used for the height of every cell in the list.
     * If {@code height} is -1, cell heights are computed in the {@code ListUI}
     * by applying <code>getPreferredSize</code> to the cell renderer component
     * for each list element.
     * <p>
     * The default value of this property is {@code -1}.
     * <p>
     * This is a JavaBeans bound property.
     *
     * @param height the height to be used for all cells in the list
     * @see #setPrototypeCellValue
     * @see #setFixedCellWidth
     * @see JComponent#addPropertyChangeListener
     */
    @BeanProperty(visualUpdate = true, description
            = "Defines a fixed cell height when greater than zero.")
    public void setFixedCellHeight(int height) {
        int oldValue = fixedCellHeight;
        fixedCellHeight = height;
        firePropertyChange("fixedCellHeight", oldValue, fixedCellHeight);
    }


    /**
     * Returns the object responsible for painting list items.
     *
     * @return the value of the {@code cellRenderer} property
     * @see #setCellRenderer
     */
    @Transient
    public ListCellRenderer<? super E> getCellRenderer() {
        return cellRenderer;
    }

    /**
     * Sets the delegate that is used to paint each cell in the list.
     * The job of a cell renderer is discussed in detail in the
     * <a href="#renderer">class level documentation</a>.
     * <p>
     * If the {@code prototypeCellValue} property is {@code non-null},
     * setting the cell renderer also causes the {@code fixedCellWidth} and
     * {@code fixedCellHeight} properties to be re-calculated. Only one
     * <code>PropertyChangeEvent</code> is generated however -
     * for the <code>cellRenderer</code> property.
     * <p>
     * The default value of this property is provided by the {@code ListUI}
     * delegate, i.e. by the look and feel implementation.
     * <p>
     * This is a JavaBeans bound property.
     *
     * @param cellRenderer the <code>ListCellRenderer</code>
     *                          that paints list cells
     * @see #getCellRenderer
     */
    @BeanProperty(visualUpdate = true, description
            = "The component used to draw the cells.")
    public void setCellRenderer(ListCellRenderer<? super E> cellRenderer) {
        ListCellRenderer<? super E> oldValue = this.cellRenderer;
        this.cellRenderer = cellRenderer;

        /* If the cellRenderer has changed and prototypeCellValue
         * was set, then recompute fixedCellWidth and fixedCellHeight.
         */
        if ((cellRenderer != null) && !cellRenderer.equals(oldValue)) {
            updateFixedCellSize();
        }

        firePropertyChange("cellRenderer", oldValue, cellRenderer);
    }


    /**
     * Returns the color used to draw the foreground of selected items.
     * {@code DefaultListCellRenderer} uses this color to draw the foreground
     * of items in the selected state, as do the renderers installed by most
     * {@code ListUI} implementations.
     *
     * @return the color to draw the foreground of selected items
     * @see #setSelectionForeground
     * @see DefaultListCellRenderer
     */
    public Color getSelectionForeground() {
        return selectionForeground;
    }


    /**
     * Sets the color used to draw the foreground of selected items, which
     * cell renderers can use to render text and graphics.
     * {@code DefaultListCellRenderer} uses this color to draw the foreground
     * of items in the selected state, as do the renderers installed by most
     * {@code ListUI} implementations.
     * <p>
     * The default value of this property is defined by the look and feel
     * implementation.
     * <p>
     * This is a JavaBeans bound property.
     *
     * @param selectionForeground  the {@code Color} to use in the foreground
     *                             for selected list items
     * @see #getSelectionForeground
     * @see #setSelectionBackground
     * @see #setForeground
     * @see #setBackground
     * @see #setFont
     * @see DefaultListCellRenderer
     */
    @BeanProperty(visualUpdate = true, description
            = "The foreground color of selected cells.")
    public void setSelectionForeground(Color selectionForeground) {
        Color oldValue = this.selectionForeground;
        this.selectionForeground = selectionForeground;
        firePropertyChange("selectionForeground", oldValue, selectionForeground);
    }


    /**
     * Returns the color used to draw the background of selected items.
     * {@code DefaultListCellRenderer} uses this color to draw the background
     * of items in the selected state, as do the renderers installed by most
     * {@code ListUI} implementations.
     *
     * @return the color to draw the background of selected items
     * @see #setSelectionBackground
     * @see DefaultListCellRenderer
     */
    public Color getSelectionBackground() {
        return selectionBackground;
    }


    /**
     * Sets the color used to draw the background of selected items, which
     * cell renderers can use fill selected cells.
     * {@code DefaultListCellRenderer} uses this color to fill the background
     * of items in the selected state, as do the renderers installed by most
     * {@code ListUI} implementations.
     * <p>
     * The default value of this property is defined by the look
     * and feel implementation.
     * <p>
     * This is a JavaBeans bound property.
     *
     * @param selectionBackground  the {@code Color} to use for the
     *                             background of selected cells
     * @see #getSelectionBackground
     * @see #setSelectionForeground
     * @see #setForeground
     * @see #setBackground
     * @see #setFont
     * @see DefaultListCellRenderer
     */
    @BeanProperty(visualUpdate = true, description
            = "The background color of selected cells.")
    public void setSelectionBackground(Color selectionBackground) {
        Color oldValue = this.selectionBackground;
        this.selectionBackground = selectionBackground;
        firePropertyChange("selectionBackground", oldValue, selectionBackground);
    }


    /**
     * Returns the value of the {@code visibleRowCount} property. See the
     * documentation for {@link #setVisibleRowCount} for details on how to
     * interpret this value.
     *
     * @return the value of the {@code visibleRowCount} property.
     * @see #setVisibleRowCount
     */
    public int getVisibleRowCount() {
        return visibleRowCount;
    }

    /**
     * Sets the {@code visibleRowCount} property, which has different meanings
     * depending on the layout orientation: For a {@code VERTICAL} layout
     * orientation, this sets the preferred number of rows to display without
     * requiring scrolling; for other orientations, it affects the wrapping of
     * cells.
     * <p>
     * In {@code VERTICAL} orientation:<br>
     * Setting this property affects the return value of the
     * {@link #getPreferredScrollableViewportSize} method, which is used to
     * calculate the preferred size of an enclosing viewport. See that method's
     * documentation for more details.
     * <p>
     * In {@code HORIZONTAL_WRAP} and {@code VERTICAL_WRAP} orientations:<br>
     * This affects how cells are wrapped. See the documentation of
     * {@link #setLayoutOrientation} for more details.
     * <p>
     * The default value of this property is {@code 8}.
     * <p>
     * Calling this method with a negative value results in the property
     * being set to {@code 0}.
     * <p>
     * This is a JavaBeans bound property.
     *
     * @param visibleRowCount  an integer specifying the preferred number of
     *                         rows to display without requiring scrolling
     * @see #getVisibleRowCount
     * @see #getPreferredScrollableViewportSize
     * @see #setLayoutOrientation
     * @see JComponent#getVisibleRect
     * @see JViewport
     */
    @BeanProperty(visualUpdate = true, description
            = "The preferred number of rows to display without requiring scrolling")
    public void setVisibleRowCount(int visibleRowCount) {
        int oldValue = this.visibleRowCount;
        this.visibleRowCount = Math.max(0, visibleRowCount);
        firePropertyChange("visibleRowCount", oldValue, visibleRowCount);
    }


    /**
     * Returns the layout orientation property for the list: {@code VERTICAL}
     * if the layout is a single column of cells, {@code VERTICAL_WRAP} if the
     * layout is "newspaper style" with the content flowing vertically then
     * horizontally, or {@code HORIZONTAL_WRAP} if the layout is "newspaper
     * style" with the content flowing horizontally then vertically.
     *
     * @return the value of the {@code layoutOrientation} property
     * @see #setLayoutOrientation
     * @since 1.4
     */
    public int getLayoutOrientation() {
        return layoutOrientation;
    }


    /**
     * Defines the way list cells are layed out. Consider a {@code JList}
     * with five cells. Cells can be layed out in one of the following ways:
     *
     * <pre>
     * VERTICAL:          0
     *                    1
     *                    2
     *                    3
     *                    4
     *
     * HORIZONTAL_WRAP:   0  1  2
     *                    3  4
     *
     * VERTICAL_WRAP:     0  3
     *                    1  4
     *                    2
     * </pre>
     * <p>
     * A description of these layouts follows:
     *
     * <table class="striped">
     * <caption>Describes layouts VERTICAL,HORIZONTAL_WRAP, and VERTICAL_WRAP
     * </caption>
     * <thead>
     *   <tr>
     *     <th scope="col">Value
     *     <th scope="col">Description
     * </thead>
     * <tbody>
     *   <tr>
     *     <th scope="row">{@code VERTICAL}
     *     <td>Cells are layed out vertically in a single column.
     *   <tr>
     *     <th scope="row">{@code HORIZONTAL_WRAP}
     *     <td>Cells are layed out horizontally, wrapping to a new row as
     *     necessary. If the {@code visibleRowCount} property is less than or
     *     equal to zero, wrapping is determined by the width of the list;
     *     otherwise wrapping is done in such a way as to ensure
     *     {@code visibleRowCount} rows in the list.
     *   <tr>
     *     <th scope="row">{@code VERTICAL_WRAP}
     *     <td>Cells are layed out vertically, wrapping to a new column as
     *     necessary. If the {@code visibleRowCount} property is less than or
     *     equal to zero, wrapping is determined by the height of the list;
     *     otherwise wrapping is done at {@code visibleRowCount} rows.
     * </tbody>
     * </table>
     *
     * The default value of this property is <code>VERTICAL</code>.
     *
     * @param layoutOrientation the new layout orientation, one of:
     *        {@code VERTICAL}, {@code HORIZONTAL_WRAP} or {@code VERTICAL_WRAP}
     * @see #getLayoutOrientation
     * @see #setVisibleRowCount
     * @see #getScrollableTracksViewportHeight
     * @see #getScrollableTracksViewportWidth
     * @throws IllegalArgumentException if {@code layoutOrientation} isn't one of the
     *         allowable values
     * @since 1.4
     */
    @BeanProperty(visualUpdate = true, enumerationValues = {
            "JList.VERTICAL",
            "JList.HORIZONTAL_WRAP",
            "JList.VERTICAL_WRAP"}, description
            = "Defines the way list cells are layed out.")
    public void setLayoutOrientation(int layoutOrientation) {
        int oldValue = this.layoutOrientation;
        switch (layoutOrientation) {
        case VERTICAL:
        case VERTICAL_WRAP:
        case HORIZONTAL_WRAP:
            this.layoutOrientation = layoutOrientation;
            firePropertyChange("layoutOrientation", oldValue, layoutOrientation);
            break;
        default:
            throw new IllegalArgumentException("layoutOrientation must be one of: VERTICAL, HORIZONTAL_WRAP or VERTICAL_WRAP");
        }
    }


    /**
     * Returns the smallest list index that is currently visible.
     * In a left-to-right {@code componentOrientation}, the first visible
     * cell is found closest to the list's upper-left corner. In right-to-left
     * orientation, it is found closest to the upper-right corner.
     * If nothing is visible or the list is empty, {@code -1} is returned.
     * Note that the returned cell may only be partially visible.
     *
     * @return the index of the first visible cell
     * @see #getLastVisibleIndex
     * @see JComponent#getVisibleRect
     */
    @BeanProperty(bound = false)
    public int getFirstVisibleIndex() {
        Rectangle r = getVisibleRect();
        int first;
        if (this.getComponentOrientation().isLeftToRight()) {
            first = locationToIndex(r.getLocation());
        } else {
            first = locationToIndex(new Point((r.x + r.width) - 1, r.y));
        }
        if (first != -1) {
            Rectangle bounds = getCellBounds(first, first);
            if (bounds != null) {
                SwingUtilities.computeIntersection(r.x, r.y, r.width, r.height, bounds);
                if (bounds.width == 0 || bounds.height == 0) {
                    first = -1;
                }
            }
        }
        return first;
    }


    /**
     * Returns the largest list index that is currently visible.
     * If nothing is visible or the list is empty, {@code -1} is returned.
     * Note that the returned cell may only be partially visible.
     *
     * @return the index of the last visible cell
     * @see #getFirstVisibleIndex
     * @see JComponent#getVisibleRect
     */
    @BeanProperty(bound = false)
    public int getLastVisibleIndex() {
        boolean leftToRight = this.getComponentOrientation().isLeftToRight();
        Rectangle r = getVisibleRect();
        Point lastPoint;
        if (leftToRight) {
            lastPoint = new Point((r.x + r.width) - 1, (r.y + r.height) - 1);
        } else {
            lastPoint = new Point(r.x, (r.y + r.height) - 1);
        }
        int location = locationToIndex(lastPoint);

        if (location != -1) {
            Rectangle bounds = getCellBounds(location, location);

            if (bounds != null) {
                SwingUtilities.computeIntersection(r.x, r.y, r.width, r.height, bounds);
                if (bounds.width == 0 || bounds.height == 0) {
                    // Try the top left(LTR) or top right(RTL) corner, and
                    // then go across checking each cell for HORIZONTAL_WRAP.
                    // Try the lower left corner, and then go across checking
                    // each cell for other list layout orientation.
                    boolean isHorizontalWrap =
                        (getLayoutOrientation() == HORIZONTAL_WRAP);
                    Point visibleLocation = isHorizontalWrap ?
                        new Point(lastPoint.x, r.y) :
                        new Point(r.x, lastPoint.y);
                    int last;
                    int visIndex = -1;
                    int lIndex = location;
                    location = -1;

                    do {
                        last = visIndex;
                        visIndex = locationToIndex(visibleLocation);

                        if (visIndex != -1) {
                            bounds = getCellBounds(visIndex, visIndex);
                            if (visIndex != lIndex && bounds != null &&
                                bounds.contains(visibleLocation)) {
                                location = visIndex;
                                if (isHorizontalWrap) {
                                    visibleLocation.y = bounds.y + bounds.height;
                                    if (visibleLocation.y >= lastPoint.y) {
                                        // Past visible region, bail.
                                        last = visIndex;
                                    }
                                }
                                else {
                                    visibleLocation.x = bounds.x + bounds.width;
                                    if (visibleLocation.x >= lastPoint.x) {
                                        // Past visible region, bail.
                                        last = visIndex;
                                    }
                                }

                            }
                            else {
                                last = visIndex;
                            }
                        }
                    } while (visIndex != -1 && last != visIndex);
                }
            }
        }
        return location;
    }


    /**
     * Scrolls the list within an enclosing viewport to make the specified
     * cell completely visible. This calls {@code scrollRectToVisible} with
     * the bounds of the specified cell. For this method to work, the
     * {@code JList} must be within a <code>JViewport</code>.
     * <p>
     * If the given index is outside the list's range of cells, this method
     * results in nothing.
     *
     * @param index  the index of the cell to make visible
     * @see JComponent#scrollRectToVisible
     * @see #getVisibleRect
     */
    public void ensureIndexIsVisible(int index) {
        Rectangle cellBounds = getCellBounds(index, index);
        if (cellBounds != null) {
            scrollRectToVisible(cellBounds);
        }
    }

    /**
     * Turns on or off automatic drag handling. In order to enable automatic
     * drag handling, this property should be set to {@code true}, and the
     * list's {@code TransferHandler} needs to be {@code non-null}.
     * The default value of the {@code dragEnabled} property is {@code false}.
     * <p>
     * The job of honoring this property, and recognizing a user drag gesture,
     * lies with the look and feel implementation, and in particular, the list's
     * {@code ListUI}. When automatic drag handling is enabled, most look and
     * feels (including those that subclass {@code BasicLookAndFeel}) begin a
     * drag and drop operation whenever the user presses the mouse button over
     * an item and then moves the mouse a few pixels. Setting this property to
     * {@code true} can therefore have a subtle effect on how selections behave.
     * <p>
     * If a look and feel is used that ignores this property, you can still
     * begin a drag and drop operation by calling {@code exportAsDrag} on the
     * list's {@code TransferHandler}.
     *
     * @param b whether or not to enable automatic drag handling
     * @exception HeadlessException if
     *            <code>b</code> is <code>true</code> and
     *            <code>GraphicsEnvironment.isHeadless()</code>
     *            returns <code>true</code>
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see #getDragEnabled
     * @see #setTransferHandler
     * @see TransferHandler
     * @since 1.4
     */
    @BeanProperty(bound = false, description
            = "determines whether automatic drag handling is enabled")
    public void setDragEnabled(boolean b) {
        if (b && GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }
        dragEnabled = b;
    }

    /**
     * Returns whether or not automatic drag handling is enabled.
     *
     * @return the value of the {@code dragEnabled} property
     * @see #setDragEnabled
     * @since 1.4
     */
    public boolean getDragEnabled() {
        return dragEnabled;
    }

    /**
     * Sets the drop mode for this component. For backward compatibility,
     * the default for this property is <code>DropMode.USE_SELECTION</code>.
     * Usage of one of the other modes is recommended, however, for an
     * improved user experience. <code>DropMode.ON</code>, for instance,
     * offers similar behavior of showing items as selected, but does so without
     * affecting the actual selection in the list.
     * <p>
     * <code>JList</code> supports the following drop modes:
     * <ul>
     *    <li><code>DropMode.USE_SELECTION</code></li>
     *    <li><code>DropMode.ON</code></li>
     *    <li><code>DropMode.INSERT</code></li>
     *    <li><code>DropMode.ON_OR_INSERT</code></li>
     * </ul>
     * The drop mode is only meaningful if this component has a
     * <code>TransferHandler</code> that accepts drops.
     *
     * @param dropMode the drop mode to use
     * @throws IllegalArgumentException if the drop mode is unsupported
     *         or <code>null</code>
     * @see #getDropMode
     * @see #getDropLocation
     * @see #setTransferHandler
     * @see TransferHandler
     * @since 1.6
     */
    public final void setDropMode(DropMode dropMode) {
        if (dropMode != null) {
            switch (dropMode) {
                case USE_SELECTION:
                case ON:
                case INSERT:
                case ON_OR_INSERT:
                    this.dropMode = dropMode;
                    return;
            }
        }

        throw new IllegalArgumentException(dropMode + ": Unsupported drop mode for list");
    }

    /**
     * Returns the drop mode for this component.
     *
     * @return the drop mode for this component
     * @see #setDropMode
     * @since 1.6
     */
    public final DropMode getDropMode() {
        return dropMode;
    }

    /**
     * Calculates a drop location in this component, representing where a
     * drop at the given point should insert data.
     *
     * @param p the point to calculate a drop location for
     * @return the drop location, or <code>null</code>
     */
    DropLocation dropLocationForPoint(Point p) {
        DropLocation location = null;
        Rectangle rect = null;

        int index = locationToIndex(p);
        if (index != -1) {
            rect = getCellBounds(index, index);
        }

        switch(dropMode) {
            case USE_SELECTION:
            case ON:
                location = new DropLocation(p,
                    (rect != null && rect.contains(p)) ? index : -1,
                    false);

                break;
            case INSERT:
                if (index == -1) {
                    location = new DropLocation(p, getModel().getSize(), true);
                    break;
                }

                if (layoutOrientation == HORIZONTAL_WRAP) {
                    boolean ltr = getComponentOrientation().isLeftToRight();

                    if (SwingUtilities2.liesInHorizontal(rect, p, ltr, false) == TRAILING) {
                        index++;
                    // special case for below all cells
                    } else if (index == getModel().getSize() - 1 && p.y >= rect.y + rect.height) {
                        index++;
                    }
                } else {
                    if (SwingUtilities2.liesInVertical(rect, p, false) == TRAILING) {
                        index++;
                    }
                }

                location = new DropLocation(p, index, true);

                break;
            case ON_OR_INSERT:
                if (index == -1) {
                    location = new DropLocation(p, getModel().getSize(), true);
                    break;
                }

                boolean between = false;

                if (layoutOrientation == HORIZONTAL_WRAP) {
                    boolean ltr = getComponentOrientation().isLeftToRight();

                    Section section = SwingUtilities2.liesInHorizontal(rect, p, ltr, true);
                    if (section == TRAILING) {
                        index++;
                        between = true;
                    // special case for below all cells
                    } else if (index == getModel().getSize() - 1 && p.y >= rect.y + rect.height) {
                        index++;
                        between = true;
                    } else if (section == LEADING) {
                        between = true;
                    }
                } else {
                    Section section = SwingUtilities2.liesInVertical(rect, p, true);
                    if (section == LEADING) {
                        between = true;
                    } else if (section == TRAILING) {
                        index++;
                        between = true;
                    }
                }

                location = new DropLocation(p, index, between);

                break;
            default:
                assert false : "Unexpected drop mode";
        }

        return location;
    }

    /**
     * Called to set or clear the drop location during a DnD operation.
     * In some cases, the component may need to use it's internal selection
     * temporarily to indicate the drop location. To help facilitate this,
     * this method returns and accepts as a parameter a state object.
     * This state object can be used to store, and later restore, the selection
     * state. Whatever this method returns will be passed back to it in
     * future calls, as the state parameter. If it wants the DnD system to
     * continue storing the same state, it must pass it back every time.
     * Here's how this is used:
     * <p>
     * Let's say that on the first call to this method the component decides
     * to save some state (because it is about to use the selection to show
     * a drop index). It can return a state object to the caller encapsulating
     * any saved selection state. On a second call, let's say the drop location
     * is being changed to something else. The component doesn't need to
     * restore anything yet, so it simply passes back the same state object
     * to have the DnD system continue storing it. Finally, let's say this
     * method is messaged with <code>null</code>. This means DnD
     * is finished with this component for now, meaning it should restore
     * state. At this point, it can use the state parameter to restore
     * said state, and of course return <code>null</code> since there's
     * no longer anything to store.
     *
     * @param location the drop location (as calculated by
     *        <code>dropLocationForPoint</code>) or <code>null</code>
     *        if there's no longer a valid drop location
     * @param state the state object saved earlier for this component,
     *        or <code>null</code>
     * @param forDrop whether or not the method is being called because an
     *        actual drop occurred
     * @return any saved state for this component, or <code>null</code> if none
     */
    Object setDropLocation(TransferHandler.DropLocation location,
                           Object state,
                           boolean forDrop) {

        Object retVal = null;
        DropLocation listLocation = (DropLocation)location;

        if (dropMode == DropMode.USE_SELECTION) {
            if (listLocation == null) {
                if (!forDrop && state != null) {
                    setSelectedIndices(((int[][])state)[0]);

                    int anchor = ((int[][])state)[1][0];
                    int lead = ((int[][])state)[1][1];

                    SwingUtilities2.setLeadAnchorWithoutSelection(
                            getSelectionModel(), lead, anchor);
                }
            } else {
                if (dropLocation == null) {
                    int[] inds = getSelectedIndices();
                    retVal = new int[][] {inds, {getAnchorSelectionIndex(),
                                                 getLeadSelectionIndex()}};
                } else {
                    retVal = state;
                }

                int index = listLocation.getIndex();
                if (index == -1) {
                    clearSelection();
                    getSelectionModel().setAnchorSelectionIndex(-1);
                    getSelectionModel().setLeadSelectionIndex(-1);
                } else {
                    setSelectionInterval(index, index);
                }
            }
        }

        DropLocation old = dropLocation;
        dropLocation = listLocation;
        firePropertyChange("dropLocation", old, dropLocation);

        return retVal;
    }

    /**
     * Returns the location that this component should visually indicate
     * as the drop location during a DnD operation over the component,
     * or {@code null} if no location is to currently be shown.
     * <p>
     * This method is not meant for querying the drop location
     * from a {@code TransferHandler}, as the drop location is only
     * set after the {@code TransferHandler}'s <code>canImport</code>
     * has returned and has allowed for the location to be shown.
     * <p>
     * When this property changes, a property change event with
     * name "dropLocation" is fired by the component.
     * <p>
     * By default, responsibility for listening for changes to this property
     * and indicating the drop location visually lies with the list's
     * {@code ListUI}, which may paint it directly and/or install a cell
     * renderer to do so. Developers wishing to implement custom drop location
     * painting and/or replace the default cell renderer, may need to honor
     * this property.
     *
     * @return the drop location
     * @see #setDropMode
     * @see TransferHandler#canImport(TransferHandler.TransferSupport)
     * @since 1.6
     */
    @BeanProperty(bound = false)
    public final DropLocation getDropLocation() {
        return dropLocation;
    }

    /**
     * Returns the next list element whose {@code toString} value
     * starts with the given prefix.
     *
     * @param prefix the string to test for a match
     * @param startIndex the index for starting the search
     * @param bias the search direction, either
     * Position.Bias.Forward or Position.Bias.Backward.
     * @return the index of the next list element that
     * starts with the prefix; otherwise {@code -1}
     * @exception IllegalArgumentException if prefix is {@code null}
     * or startIndex is out of bounds
     * @since 1.4
     */
    public int getNextMatch(String prefix, int startIndex, Position.Bias bias) {
        ListModel<E> model = getModel();
        int max = model.getSize();
        if (prefix == null) {
            throw new IllegalArgumentException();
        }
        if (startIndex < 0 || startIndex >= max) {
            throw new IllegalArgumentException();
        }
        prefix = prefix.toUpperCase();

        // start search from the next element after the selected element
        int increment = (bias == Position.Bias.Forward) ? 1 : -1;
        int index = startIndex;
        do {
            E element = model.getElementAt(index);

            if (element != null) {
                String string;

                if (element instanceof String) {
                    string = ((String)element).toUpperCase();
                }
                else {
                    string = element.toString();
                    if (string != null) {
                        string = string.toUpperCase();
                    }
                }

                if (string != null && string.startsWith(prefix)) {
                    return index;
                }
            }
            index = (index + increment + max) % max;
        } while (index != startIndex);
        return -1;
    }

    /**
     * Returns the tooltip text to be used for the given event. This overrides
     * {@code JComponent}'s {@code getToolTipText} to first check the cell
     * renderer component for the cell over which the event occurred, returning
     * its tooltip text, if any. This implementation allows you to specify
     * tooltip text on the cell level, by using {@code setToolTipText} on your
     * cell renderer component.
     * <p>
     * <strong>Note:</strong> For <code>JList</code> to properly display the
     * tooltips of its renderers in this manner, <code>JList</code> must be a
     * registered component with the <code>ToolTipManager</code>. This registration
     * is done automatically in the constructor. However, if at a later point
     * <code>JList</code> is unregistered, by way of a call to
     * {@code setToolTipText(null)}, tips from the renderers will no longer display.
     *
     * @param event the {@code MouseEvent} to fetch the tooltip text for
     * @see JComponent#setToolTipText
     * @see JComponent#getToolTipText
     */
    @SuppressWarnings("deprecation")
    public String getToolTipText(MouseEvent event) {
        if(event != null) {
            Point p = event.getPoint();
            int index = locationToIndex(p);
            ListCellRenderer<? super E> r = getCellRenderer();
            Rectangle cellBounds;

            if (index != -1 && r != null && (cellBounds =
                               getCellBounds(index, index)) != null &&
                               cellBounds.contains(p.x, p.y)) {
                ListSelectionModel lsm = getSelectionModel();
                Component rComponent = r.getListCellRendererComponent(
                           this, getModel().getElementAt(index), index,
                           lsm.isSelectedIndex(index),
                           (hasFocus() && (lsm.getLeadSelectionIndex() ==
                                           index)));

                if(rComponent instanceof JComponent) {
                    MouseEvent      newEvent;

                    p.translate(-cellBounds.x, -cellBounds.y);
                    newEvent = new MouseEvent(rComponent, event.getID(),
                                              event.getWhen(),
                                              event.getModifiers(),
                                              p.x, p.y,
                                              event.getXOnScreen(),
                                              event.getYOnScreen(),
                                              event.getClickCount(),
                                              event.isPopupTrigger(),
                                              MouseEvent.NOBUTTON);
                    MouseEventAccessor meAccessor =
                        AWTAccessor.getMouseEventAccessor();
                    meAccessor.setCausedByTouchEvent(newEvent,
                        meAccessor.isCausedByTouchEvent(event));

                    String tip = ((JComponent)rComponent).getToolTipText(
                                              newEvent);

                    if (tip != null) {
                        return tip;
                    }
                }
            }
        }
        return super.getToolTipText();
    }

    /**
     * --- ListUI Delegations ---
     */


    /**
     * Returns the cell index closest to the given location in the list's
     * coordinate system. To determine if the cell actually contains the
     * specified location, compare the point against the cell's bounds,
     * as provided by {@code getCellBounds}. This method returns {@code -1}
     * if the model is empty
     * <p>
     * This is a cover method that delegates to the method of the same name
     * in the list's {@code ListUI}. It returns {@code -1} if the list has
     * no {@code ListUI}.
     *
     * @param location the coordinates of the point
     * @return the cell index closest to the given location, or {@code -1}
     */
    public int locationToIndex(Point location) {
        ListUI ui = getUI();
        return (ui != null) ? ui.locationToIndex(this, location) : -1;
    }


    /**
     * Returns the origin of the specified item in the list's coordinate
     * system. This method returns {@code null} if the index isn't valid.
     * <p>
     * This is a cover method that delegates to the method of the same name
     * in the list's {@code ListUI}. It returns {@code null} if the list has
     * no {@code ListUI}.
     *
     * @param index the cell index
     * @return the origin of the cell, or {@code null}
     */
    public Point indexToLocation(int index) {
        ListUI ui = getUI();
        return (ui != null) ? ui.indexToLocation(this, index) : null;
    }


    /**
     * Returns the bounding rectangle, in the list's coordinate system,
     * for the range of cells specified by the two indices.
     * These indices can be supplied in any order.
     * <p>
     * If the smaller index is outside the list's range of cells, this method
     * returns {@code null}. If the smaller index is valid, but the larger
     * index is outside the list's range, the bounds of just the first index
     * is returned. Otherwise, the bounds of the valid range is returned.
     * <p>
     * This is a cover method that delegates to the method of the same name
     * in the list's {@code ListUI}. It returns {@code null} if the list has
     * no {@code ListUI}.
     *
     * @param index0 the first index in the range
     * @param index1 the second index in the range
     * @return the bounding rectangle for the range of cells, or {@code null}
     */
    public Rectangle getCellBounds(int index0, int index1) {
        ListUI ui = getUI();
        return (ui != null) ? ui.getCellBounds(this, index0, index1) : null;
    }


    /**
     * --- ListModel Support ---
     */


    /**
     * Returns the data model that holds the list of items displayed
     * by the <code>JList</code> component.
     *
     * @return the <code>ListModel</code> that provides the displayed
     *                          list of items
     * @see #setModel
     */
    public ListModel<E> getModel() {
        return dataModel;
    }

    /**
     * Sets the model that represents the contents or "value" of the
     * list, notifies property change listeners, and then clears the
     * list's selection.
     * <p>
     * This is a JavaBeans bound property.
     *
     * @param model  the <code>ListModel</code> that provides the
     *                                          list of items for display
     * @exception IllegalArgumentException  if <code>model</code> is
     *                                          <code>null</code>
     * @see #getModel
     * @see #clearSelection
     */
    @BeanProperty(visualUpdate = true, description
            = "The object that contains the data to be drawn by this JList.")
    public void setModel(ListModel<E> model) {
        if (model == null) {
            throw new IllegalArgumentException("model must be non null");
        }
        ListModel<E> oldValue = dataModel;
        dataModel = model;
        firePropertyChange("model", oldValue, dataModel);
        clearSelection();
    }


    /**
     * Constructs a read-only <code>ListModel</code> from an array of items,
     * and calls {@code setModel} with this model.
     * <p>
     * Attempts to pass a {@code null} value to this method results in
     * undefined behavior and, most likely, exceptions. The created model
     * references the given array directly. Attempts to modify the array
     * after invoking this method results in undefined behavior.
     *
     * @param listData an array of {@code E} containing the items to
     *        display in the list
     * @see #setModel
     */
    public void setListData(final E[] listData) {
        setModel (
            new AbstractListModel<E>() {
                public int getSize() { return listData.length; }
                public E getElementAt(int i) { return listData[i]; }
            }
        );
    }


    /**
     * Constructs a read-only <code>ListModel</code> from a <code>Vector</code>
     * and calls {@code setModel} with this model.
     * <p>
     * Attempts to pass a {@code null} value to this method results in
     * undefined behavior and, most likely, exceptions. The created model
     * references the given {@code Vector} directly. Attempts to modify the
     * {@code Vector} after invoking this method results in undefined behavior.
     *
     * @param listData a <code>Vector</code> containing the items to
     *                                          display in the list
     * @see #setModel
     */
    public void setListData(final Vector<? extends E> listData) {
        setModel (
            new AbstractListModel<E>() {
                public int getSize() { return listData.size(); }
                public E getElementAt(int i) { return listData.elementAt(i); }
            }
        );
    }


    /**
     * --- ListSelectionModel delegations and extensions ---
     */


    /**
     * Returns an instance of {@code DefaultListSelectionModel}; called
     * during construction to initialize the list's selection model
     * property.
     *
     * @return a {@code DefaultListSelecitonModel}, used to initialize
     *         the list's selection model property during construction
     * @see #setSelectionModel
     * @see DefaultListSelectionModel
     */
    protected ListSelectionModel createSelectionModel() {
        return new DefaultListSelectionModel();
    }


    /**
     * Returns the current selection model. The selection model maintains the
     * selection state of the list. See the class level documentation for more
     * details.
     *
     * @return the <code>ListSelectionModel</code> that maintains the
     *         list's selections
     *
     * @see #setSelectionModel
     * @see ListSelectionModel
     */
    public ListSelectionModel getSelectionModel() {
        return selectionModel;
    }


    /**
     * Notifies {@code ListSelectionListener}s added directly to the list
     * of selection changes made to the selection model. {@code JList}
     * listens for changes made to the selection in the selection model,
     * and forwards notification to listeners added to the list directly,
     * by calling this method.
     * <p>
     * This method constructs a {@code ListSelectionEvent} with this list
     * as the source, and the specified arguments, and sends it to the
     * registered {@code ListSelectionListeners}.
     *
     * @param firstIndex the first index in the range, {@code <= lastIndex}
     * @param lastIndex the last index in the range, {@code >= firstIndex}
     * @param isAdjusting whether or not this is one in a series of
     *        multiple events, where changes are still being made
     *
     * @see #addListSelectionListener
     * @see #removeListSelectionListener
     * @see javax.swing.event.ListSelectionEvent
     * @see EventListenerList
     */
    protected void fireSelectionValueChanged(int firstIndex, int lastIndex,
                                             boolean isAdjusting)
    {
        Object[] listeners = listenerList.getListenerList();
        ListSelectionEvent e = null;

        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ListSelectionListener.class) {
                if (e == null) {
                    e = new ListSelectionEvent(this, firstIndex, lastIndex,
                                               isAdjusting);
                }
                ((ListSelectionListener)listeners[i+1]).valueChanged(e);
            }
        }
    }


    /* A ListSelectionListener that forwards ListSelectionEvents from
     * the selectionModel to the JList ListSelectionListeners.  The
     * forwarded events only differ from the originals in that their
     * source is the JList instead of the selectionModel itself.
     */
    private class ListSelectionHandler implements ListSelectionListener, Serializable
    {
        public void valueChanged(ListSelectionEvent e) {
            fireSelectionValueChanged(e.getFirstIndex(),
                                      e.getLastIndex(),
                                      e.getValueIsAdjusting());
        }
    }


    /**
     * Adds a listener to the list, to be notified each time a change to the
     * selection occurs; the preferred way of listening for selection state
     * changes. {@code JList} takes care of listening for selection state
     * changes in the selection model, and notifies the given listener of
     * each change. {@code ListSelectionEvent}s sent to the listener have a
     * {@code source} property set to this list.
     *
     * @param listener the {@code ListSelectionListener} to add
     * @see #getSelectionModel
     * @see #getListSelectionListeners
     */
    public void addListSelectionListener(ListSelectionListener listener)
    {
        if (selectionListener == null) {
            selectionListener = new ListSelectionHandler();
            getSelectionModel().addListSelectionListener(selectionListener);
        }

        listenerList.add(ListSelectionListener.class, listener);
    }


    /**
     * Removes a selection listener from the list.
     *
     * @param listener the {@code ListSelectionListener} to remove
     * @see #addListSelectionListener
     * @see #getSelectionModel
     */
    public void removeListSelectionListener(ListSelectionListener listener) {
        listenerList.remove(ListSelectionListener.class, listener);
    }


    /**
     * Returns an array of all the {@code ListSelectionListener}s added
     * to this {@code JList} by way of {@code addListSelectionListener}.
     *
     * @return all of the {@code ListSelectionListener}s on this list, or
     *         an empty array if no listeners have been added
     * @see #addListSelectionListener
     * @since 1.4
     */
    @BeanProperty(bound = false)
    public ListSelectionListener[] getListSelectionListeners() {
        return listenerList.getListeners(ListSelectionListener.class);
    }


    /**
     * Sets the <code>selectionModel</code> for the list to a
     * non-<code>null</code> <code>ListSelectionModel</code>
     * implementation. The selection model handles the task of making single
     * selections, selections of contiguous ranges, and non-contiguous
     * selections.
     * <p>
     * This is a JavaBeans bound property.
     *
     * @param selectionModel  the <code>ListSelectionModel</code> that
     *                          implements the selections
     * @exception IllegalArgumentException   if <code>selectionModel</code>
     *                                          is <code>null</code>
     * @see #getSelectionModel
     */
    @BeanProperty(description
            = "The selection model, recording which cells are selected.")
    public void setSelectionModel(ListSelectionModel selectionModel) {
        if (selectionModel == null) {
            throw new IllegalArgumentException("selectionModel must be non null");
        }

        /* Remove the forwarding ListSelectionListener from the old
         * selectionModel, and add it to the new one, if necessary.
         */
        if (selectionListener != null) {
            this.selectionModel.removeListSelectionListener(selectionListener);
            selectionModel.addListSelectionListener(selectionListener);
        }

        ListSelectionModel oldValue = this.selectionModel;
        this.selectionModel = selectionModel;
        firePropertyChange("selectionModel", oldValue, selectionModel);
    }


    /**
     * Sets the selection mode for the list. This is a cover method that sets
     * the selection mode directly on the selection model.
     * <p>
     * The following list describes the accepted selection modes:
     * <ul>
     * <li>{@code ListSelectionModel.SINGLE_SELECTION} -
     *   Only one list index can be selected at a time. In this mode,
     *   {@code setSelectionInterval} and {@code addSelectionInterval} are
     *   equivalent, both replacing the current selection with the index
     *   represented by the second argument (the "lead").
     * <li>{@code ListSelectionModel.SINGLE_INTERVAL_SELECTION} -
     *   Only one contiguous interval can be selected at a time.
     *   In this mode, {@code addSelectionInterval} behaves like
     *   {@code setSelectionInterval} (replacing the current selection},
     *   unless the given interval is immediately adjacent to or overlaps
     *   the existing selection, and can be used to grow the selection.
     * <li>{@code ListSelectionModel.MULTIPLE_INTERVAL_SELECTION} -
     *   In this mode, there's no restriction on what can be selected.
     *   This mode is the default.
     * </ul>
     *
     * @param selectionMode the selection mode
     * @see #getSelectionMode
     * @throws IllegalArgumentException if the selection mode isn't
     *         one of those allowed
     */
    @BeanProperty(bound = false, enumerationValues = {
            "ListSelectionModel.SINGLE_SELECTION",
            "ListSelectionModel.SINGLE_INTERVAL_SELECTION",
            "ListSelectionModel.MULTIPLE_INTERVAL_SELECTION"}, description
            = "The selection mode.")
    public void setSelectionMode(int selectionMode) {
        getSelectionModel().setSelectionMode(selectionMode);
    }

    /**
     * Returns the current selection mode for the list. This is a cover
     * method that delegates to the method of the same name on the
     * list's selection model.
     *
     * @return the current selection mode
     * @see #setSelectionMode
     */
    public int getSelectionMode() {
        return getSelectionModel().getSelectionMode();
    }


    /**
     * Returns the anchor selection index. This is a cover method that
     * delegates to the method of the same name on the list's selection model.
     *
     * @return the anchor selection index
     * @see ListSelectionModel#getAnchorSelectionIndex
     */
    @BeanProperty(bound = false)
    public int getAnchorSelectionIndex() {
        return getSelectionModel().getAnchorSelectionIndex();
    }


    /**
     * Returns the lead selection index. This is a cover method that
     * delegates to the method of the same name on the list's selection model.
     *
     * @return the lead selection index
     * @see ListSelectionModel#getLeadSelectionIndex
     */
    @BeanProperty(bound = false, description
            = "The lead selection index.")
    public int getLeadSelectionIndex() {
        return getSelectionModel().getLeadSelectionIndex();
    }


    /**
     * Returns the smallest selected cell index, or {@code -1} if the selection
     * is empty. This is a cover method that delegates to the method of the same
     * name on the list's selection model.
     *
     * @return the smallest selected cell index, or {@code -1}
     * @see ListSelectionModel#getMinSelectionIndex
     */
    @BeanProperty(bound = false)
    public int getMinSelectionIndex() {
        return getSelectionModel().getMinSelectionIndex();
    }


    /**
     * Returns the largest selected cell index, or {@code -1} if the selection
     * is empty. This is a cover method that delegates to the method of the same
     * name on the list's selection model.
     *
     * @return the largest selected cell index
     * @see ListSelectionModel#getMaxSelectionIndex
     */
    @BeanProperty(bound = false)
    public int getMaxSelectionIndex() {
        return getSelectionModel().getMaxSelectionIndex();
    }


    /**
     * Returns {@code true} if the specified index is selected,
     * else {@code false}. This is a cover method that delegates to the method
     * of the same name on the list's selection model.
     *
     * @param index index to be queried for selection state
     * @return {@code true} if the specified index is selected,
     *         else {@code false}
     * @see ListSelectionModel#isSelectedIndex
     * @see #setSelectedIndex
     */
    public boolean isSelectedIndex(int index) {
        return getSelectionModel().isSelectedIndex(index);
    }


    /**
     * Returns {@code true} if nothing is selected, else {@code false}.
     * This is a cover method that delegates to the method of the same
     * name on the list's selection model.
     *
     * @return {@code true} if nothing is selected, else {@code false}
     * @see ListSelectionModel#isSelectionEmpty
     * @see #clearSelection
     */
    @BeanProperty(bound = false)
    public boolean isSelectionEmpty() {
        return getSelectionModel().isSelectionEmpty();
    }


    /**
     * Clears the selection; after calling this method, {@code isSelectionEmpty}
     * will return {@code true}. This is a cover method that delegates to the
     * method of the same name on the list's selection model.
     *
     * @see ListSelectionModel#clearSelection
     * @see #isSelectionEmpty
     */
    public void clearSelection() {
        getSelectionModel().clearSelection();
    }


    /**
     * Selects the specified interval. Both {@code anchor} and {@code lead}
     * indices are included. {@code anchor} doesn't have to be less than or
     * equal to {@code lead}. This is a cover method that delegates to the
     * method of the same name on the list's selection model.
     * <p>
     * Refer to the documentation of the selection model class being used
     * for details on how values less than {@code 0} are handled.
     *
     * @param anchor the first index to select
     * @param lead the last index to select
     * @see ListSelectionModel#setSelectionInterval
     * @see DefaultListSelectionModel#setSelectionInterval
     * @see #createSelectionModel
     * @see #addSelectionInterval
     * @see #removeSelectionInterval
     */
    public void setSelectionInterval(int anchor, int lead) {
        getSelectionModel().setSelectionInterval(anchor, lead);
    }


    /**
     * Sets the selection to be the union of the specified interval with current
     * selection. Both the {@code anchor} and {@code lead} indices are
     * included. {@code anchor} doesn't have to be less than or
     * equal to {@code lead}. This is a cover method that delegates to the
     * method of the same name on the list's selection model.
     * <p>
     * Refer to the documentation of the selection model class being used
     * for details on how values less than {@code 0} are handled.
     *
     * @param anchor the first index to add to the selection
     * @param lead the last index to add to the selection
     * @see ListSelectionModel#addSelectionInterval
     * @see DefaultListSelectionModel#addSelectionInterval
     * @see #createSelectionModel
     * @see #setSelectionInterval
     * @see #removeSelectionInterval
     */
    public void addSelectionInterval(int anchor, int lead) {
        getSelectionModel().addSelectionInterval(anchor, lead);
    }


    /**
     * Sets the selection to be the set difference of the specified interval
     * and the current selection. Both the {@code index0} and {@code index1}
     * indices are removed. {@code index0} doesn't have to be less than or
     * equal to {@code index1}. This is a cover method that delegates to the
     * method of the same name on the list's selection model.
     * <p>
     * Refer to the documentation of the selection model class being used
     * for details on how values less than {@code 0} are handled.
     *
     * @param index0 the first index to remove from the selection
     * @param index1 the last index to remove from the selection
     * @see ListSelectionModel#removeSelectionInterval
     * @see DefaultListSelectionModel#removeSelectionInterval
     * @see #createSelectionModel
     * @see #setSelectionInterval
     * @see #addSelectionInterval
     */
    public void removeSelectionInterval(int index0, int index1) {
        getSelectionModel().removeSelectionInterval(index0, index1);
    }


    /**
     * Sets the selection model's {@code valueIsAdjusting} property. When
     * {@code true}, upcoming changes to selection should be considered part
     * of a single change. This property is used internally and developers
     * typically need not call this method. For example, when the model is being
     * updated in response to a user drag, the value of the property is set
     * to {@code true} when the drag is initiated and set to {@code false}
     * when the drag is finished. This allows listeners to update only
     * when a change has been finalized, rather than handling all of the
     * intermediate values.
     * <p>
     * You may want to use this directly if making a series of changes
     * that should be considered part of a single change.
     * <p>
     * This is a cover method that delegates to the method of the same name on
     * the list's selection model. See the documentation for
     * {@link javax.swing.ListSelectionModel#setValueIsAdjusting} for
     * more details.
     *
     * @param b the new value for the property
     * @see ListSelectionModel#setValueIsAdjusting
     * @see javax.swing.event.ListSelectionEvent#getValueIsAdjusting
     * @see #getValueIsAdjusting
     */
    public void setValueIsAdjusting(boolean b) {
        getSelectionModel().setValueIsAdjusting(b);
    }


    /**
     * Returns the value of the selection model's {@code isAdjusting} property.
     * <p>
     * This is a cover method that delegates to the method of the same name on
     * the list's selection model.
     *
     * @return the value of the selection model's {@code isAdjusting} property.
     *
     * @see #setValueIsAdjusting
     * @see ListSelectionModel#getValueIsAdjusting
     */
    public boolean getValueIsAdjusting() {
        return getSelectionModel().getValueIsAdjusting();
    }


    /**
     * Returns an array of all of the selected indices, in increasing
     * order.
     *
     * @return all of the selected indices, in increasing order,
     *         or an empty array if nothing is selected
     * @see #removeSelectionInterval
     * @see #addListSelectionListener
     */
    @Transient
    public int[] getSelectedIndices() {
        return getSelectionModel().getSelectedIndices();
    }


    /**
     * Selects a single cell. Does nothing if the given index is greater
     * than or equal to the model size. This is a convenience method that uses
     * {@code setSelectionInterval} on the selection model. Refer to the
     * documentation for the selection model class being used for details on
     * how values less than {@code 0} are handled.
     *
     * @param index the index of the cell to select
     * @see ListSelectionModel#setSelectionInterval
     * @see #isSelectedIndex
     * @see #addListSelectionListener
     */
    @BeanProperty(bound = false, description
            = "The index of the selected cell.")
    public void setSelectedIndex(int index) {
        if (index >= getModel().getSize()) {
            return;
        }
        getSelectionModel().setSelectionInterval(index, index);
    }


    /**
     * Changes the selection to be the set of indices specified by the given
     * array. Indices greater than or equal to the model size are ignored.
     * This is a convenience method that clears the selection and then uses
     * {@code addSelectionInterval} on the selection model to add the indices.
     * Refer to the documentation of the selection model class being used for
     * details on how values less than {@code 0} are handled.
     *
     * @param indices an array of the indices of the cells to select,
     *                {@code non-null}
     * @see ListSelectionModel#addSelectionInterval
     * @see #isSelectedIndex
     * @see #addListSelectionListener
     * @throws NullPointerException if the given array is {@code null}
     */
    public void setSelectedIndices(int[] indices) {
        ListSelectionModel sm = getSelectionModel();
        sm.clearSelection();
        int size = getModel().getSize();
        for (int i : indices) {
            if (i < size) {
                sm.addSelectionInterval(i, i);
            }
        }
    }


    /**
     * Returns an array of all the selected values, in increasing order based
     * on their indices in the list.
     *
     * @return the selected values, or an empty array if nothing is selected
     * @see #isSelectedIndex
     * @see #getModel
     * @see #addListSelectionListener
     *
     * @deprecated As of JDK 1.7, replaced by {@link #getSelectedValuesList()}
     */
    @Deprecated
    @BeanProperty(bound = false)
    public Object[] getSelectedValues() {
        ListSelectionModel sm = getSelectionModel();
        ListModel<E> dm = getModel();

        int iMin = sm.getMinSelectionIndex();
        int iMax = sm.getMaxSelectionIndex();
        int size = dm.getSize();

        if ((iMin < 0) || (iMax < 0) || (iMin >= size)) {
            return new Object[0];
        }
        iMax = iMax < size ? iMax : size - 1;

        Object[] rvTmp = new Object[1+ (iMax - iMin)];
        int n = 0;
        for(int i = iMin; i <= iMax; i++) {
            if (sm.isSelectedIndex(i)) {
                rvTmp[n++] = dm.getElementAt(i);
            }
        }
        Object[] rv = new Object[n];
        System.arraycopy(rvTmp, 0, rv, 0, n);
        return rv;
    }

    /**
     * Returns a list of all the selected items, in increasing order based
     * on their indices in the list.
     *
     * @return the selected items, or an empty list if nothing is selected
     * @see #isSelectedIndex
     * @see #getModel
     * @see #addListSelectionListener
     *
     * @since 1.7
     */
    @BeanProperty(bound = false)
    public List<E> getSelectedValuesList() {
        ListModel<E> dm = getModel();
        int[] selectedIndices = getSelectedIndices();

        if (selectedIndices.length > 0) {
            int size = dm.getSize();
            if (selectedIndices[0] >= size) {
                return Collections.emptyList();
            }
            List<E> selectedItems = new ArrayList<E>();
            for (int i : selectedIndices) {
                if (i >= size)
                    break;
                selectedItems.add(dm.getElementAt(i));
            }
            return selectedItems;
        }
        return Collections.emptyList();
    }


    /**
     * Returns the smallest selected cell index; <i>the selection</i> when only
     * a single item is selected in the list. When multiple items are selected,
     * it is simply the smallest selected index. Returns {@code -1} if there is
     * no selection.
     * <p>
     * This method is a cover that delegates to {@code getMinSelectionIndex}.
     *
     * @return the smallest selected cell index
     * @see #getMinSelectionIndex
     * @see #addListSelectionListener
     */
    public int getSelectedIndex() {
        return getMinSelectionIndex();
    }


    /**
     * Returns the value for the smallest selected cell index;
     * <i>the selected value</i> when only a single item is selected in the
     * list. When multiple items are selected, it is simply the value for the
     * smallest selected index. Returns {@code null} if there is no selection.
     * <p>
     * This is a convenience method that simply returns the model value for
     * {@code getMinSelectionIndex}.
     *
     * @return the first selected value
     * @see #getMinSelectionIndex
     * @see #getModel
     * @see #addListSelectionListener
     */
    @BeanProperty(bound = false)
    public E getSelectedValue() {
        int i = getMinSelectionIndex();
        return ((i == -1) || (i >= getModel().getSize())) ? null :
                getModel().getElementAt(i);
    }


    /**
     * Selects the specified object from the list.
     * If the object passed is {@code null}, the selection is cleared.
     *
     * @param anObject      the object to select
     * @param shouldScroll  {@code true} if the list should scroll to display
     *                      the selected object, if one exists; otherwise {@code false}
     */
    public void setSelectedValue(Object anObject,boolean shouldScroll) {
        if(anObject == null)
            clearSelection();
        else if(!anObject.equals(getSelectedValue())) {
            int i,c;
            ListModel<E> dm = getModel();
            for(i=0,c=dm.getSize();i<c;i++)
                if(anObject.equals(dm.getElementAt(i))){
                    setSelectedIndex(i);
                    if(shouldScroll)
                        ensureIndexIsVisible(i);
                    repaint();  /** FIX-ME setSelectedIndex does not redraw all the time with the basic l&f**/
                    return;
                }
            setSelectedIndex(-1);
        }
        repaint(); /** FIX-ME setSelectedIndex does not redraw all the time with the basic l&f**/
    }



    /**
     * --- The Scrollable Implementation ---
     */

    private void checkScrollableParameters(Rectangle visibleRect, int orientation) {
        if (visibleRect == null) {
            throw new IllegalArgumentException("visibleRect must be non-null");
        }
        switch (orientation) {
        case SwingConstants.VERTICAL:
        case SwingConstants.HORIZONTAL:
            break;
        default:
            throw new IllegalArgumentException("orientation must be one of: VERTICAL, HORIZONTAL");
        }
    }


    /**
     * Computes the size of viewport needed to display {@code visibleRowCount}
     * rows. The value returned by this method depends on the layout
     * orientation:
     * <p>
     * <b>{@code VERTICAL}:</b>
     * <br>
     * This is trivial if both {@code fixedCellWidth} and {@code fixedCellHeight}
     * have been set (either explicitly or by specifying a prototype cell value).
     * The width is simply the {@code fixedCellWidth} plus the list's horizontal
     * insets. The height is the {@code fixedCellHeight} multiplied by the
     * {@code visibleRowCount}, plus the list's vertical insets.
     * <p>
     * If either {@code fixedCellWidth} or {@code fixedCellHeight} haven't been
     * specified, heuristics are used. If the model is empty, the width is
     * the {@code fixedCellWidth}, if greater than {@code 0}, or a hard-coded
     * value of {@code 256}. The height is the {@code fixedCellHeight} multiplied
     * by {@code visibleRowCount}, if {@code fixedCellHeight} is greater than
     * {@code 0}, otherwise it is a hard-coded value of {@code 16} multiplied by
     * {@code visibleRowCount}.
     * <p>
     * If the model isn't empty, the width is the preferred size's width,
     * typically the width of the widest list element. The height is the
     * height of the cell with index 0 multiplied by the {@code visibleRowCount},
     * plus the list's vertical insets.
     * <p>
     * <b>{@code VERTICAL_WRAP} or {@code HORIZONTAL_WRAP}:</b>
     * <br>
     * This method simply returns the value from {@code getPreferredSize}.
     * The list's {@code ListUI} is expected to override {@code getPreferredSize}
     * to return an appropriate value.
     *
     * @return a dimension containing the size of the viewport needed
     *          to display {@code visibleRowCount} rows
     * @see #getPreferredScrollableViewportSize
     * @see #setPrototypeCellValue
     */
    @BeanProperty(bound = false)
    public Dimension getPreferredScrollableViewportSize()
    {
        if (getLayoutOrientation() != VERTICAL) {
            return getPreferredSize();
        }
        Insets insets = getInsets();
        int dx = insets.left + insets.right;
        int dy = insets.top + insets.bottom;

        int visibleRowCount = getVisibleRowCount();
        int fixedCellWidth = getFixedCellWidth();
        int fixedCellHeight = getFixedCellHeight();

        if ((fixedCellWidth > 0) && (fixedCellHeight > 0)) {
            int width = fixedCellWidth + dx;
            int height = (visibleRowCount * fixedCellHeight) + dy;
            return new Dimension(width, height);
        }
        else if (getModel().getSize() > 0) {
            int width = getPreferredSize().width;
            int height;
            Rectangle r = getCellBounds(0, 0);
            if (r != null) {
                height = (visibleRowCount * r.height) + dy;
            }
            else {
                // Will only happen if UI null, shouldn't matter what we return
                height = 1;
            }
            return new Dimension(width, height);
        }
        else {
            fixedCellWidth = (fixedCellWidth > 0) ? fixedCellWidth : 256;
            fixedCellHeight = (fixedCellHeight > 0) ? fixedCellHeight : 16;
            return new Dimension(fixedCellWidth, fixedCellHeight * visibleRowCount);
        }
    }


    /**
     * Returns the distance to scroll to expose the next or previous
     * row (for vertical scrolling) or column (for horizontal scrolling).
     * <p>
     * For horizontal scrolling, if the layout orientation is {@code VERTICAL},
     * then the list's font size is returned (or {@code 1} if the font is
     * {@code null}).
     *
     * @param visibleRect the view area visible within the viewport
     * @param orientation {@code SwingConstants.HORIZONTAL} or
     *                    {@code SwingConstants.VERTICAL}
     * @param direction less or equal to zero to scroll up/back,
     *                  greater than zero for down/forward
     * @return the "unit" increment for scrolling in the specified direction;
     *         always positive
     * @see #getScrollableBlockIncrement
     * @see Scrollable#getScrollableUnitIncrement
     * @throws IllegalArgumentException if {@code visibleRect} is {@code null}, or
     *         {@code orientation} isn't one of {@code SwingConstants.VERTICAL} or
     *         {@code SwingConstants.HORIZONTAL}
     */
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
    {
        checkScrollableParameters(visibleRect, orientation);

        if (orientation == SwingConstants.VERTICAL) {
            int row = locationToIndex(visibleRect.getLocation());

            if (row == -1) {
                return 0;
            }
            else {
                /* Scroll Down */
                if (direction > 0) {
                    Rectangle r = getCellBounds(row, row);
                    return (r == null) ? 0 : r.height - (visibleRect.y - r.y);
                }
                /* Scroll Up */
                else {
                    Rectangle r = getCellBounds(row, row);

                    /* The first row is completely visible and it's row 0.
                     * We're done.
                     */
                    if ((r.y == visibleRect.y) && (row == 0))  {
                        return 0;
                    }
                    /* The first row is completely visible, return the
                     * height of the previous row or 0 if the first row
                     * is the top row of the list.
                     */
                    else if (r.y == visibleRect.y) {
                        Point loc = r.getLocation();
                        loc.y--;
                        int prevIndex = locationToIndex(loc);
                        Rectangle prevR = getCellBounds(prevIndex, prevIndex);

                        if (prevR == null || prevR.y >= r.y) {
                            return 0;
                        }
                        return prevR.height;
                    }
                    /* The first row is partially visible, return the
                     * height of hidden part.
                     */
                    else {
                        return visibleRect.y - r.y;
                    }
                }
            }
        } else if (orientation == SwingConstants.HORIZONTAL &&
                           getLayoutOrientation() != JList.VERTICAL) {
            boolean leftToRight = getComponentOrientation().isLeftToRight();
            int index;
            Point leadingPoint;

            if (leftToRight) {
                leadingPoint = visibleRect.getLocation();
            }
            else {
                leadingPoint = new Point(visibleRect.x + visibleRect.width -1,
                                         visibleRect.y);
            }
            index = locationToIndex(leadingPoint);

            if (index != -1) {
                Rectangle cellBounds = getCellBounds(index, index);
                if (cellBounds != null && cellBounds.contains(leadingPoint)) {
                    int leadingVisibleEdge;
                    int leadingCellEdge;

                    if (leftToRight) {
                        leadingVisibleEdge = visibleRect.x;
                        leadingCellEdge = cellBounds.x;
                    }
                    else {
                        leadingVisibleEdge = visibleRect.x + visibleRect.width;
                        leadingCellEdge = cellBounds.x + cellBounds.width;
                    }

                    if (leadingCellEdge != leadingVisibleEdge) {
                        if (direction < 0) {
                            // Show remainder of leading cell
                            return Math.abs(leadingVisibleEdge - leadingCellEdge);

                        }
                        else if (leftToRight) {
                            // Hide rest of leading cell
                            return leadingCellEdge + cellBounds.width - leadingVisibleEdge;
                        }
                        else {
                            // Hide rest of leading cell
                            return leadingVisibleEdge - cellBounds.x;
                        }
                    }
                    // ASSUME: All cells are the same width
                    return cellBounds.width;
                }
            }
        }
        Font f = getFont();
        return (f != null) ? f.getSize() : 1;
    }


    /**
     * Returns the distance to scroll to expose the next or previous block.
     * <p>
     * For vertical scrolling, the following rules are used:
     * <ul>
     * <li>if scrolling down, returns the distance to scroll so that the last
     * visible element becomes the first completely visible element
     * <li>if scrolling up, returns the distance to scroll so that the first
     * visible element becomes the last completely visible element
     * <li>returns {@code visibleRect.height} if the list is empty
     * </ul>
     * <p>
     * For horizontal scrolling, when the layout orientation is either
     * {@code VERTICAL_WRAP} or {@code HORIZONTAL_WRAP}:
     * <ul>
     * <li>if scrolling right, returns the distance to scroll so that the
     * last visible element becomes
     * the first completely visible element
     * <li>if scrolling left, returns the distance to scroll so that the first
     * visible element becomes the last completely visible element
     * <li>returns {@code visibleRect.width} if the list is empty
     * </ul>
     * <p>
     * For horizontal scrolling and {@code VERTICAL} orientation,
     * returns {@code visibleRect.width}.
     * <p>
     * Note that the value of {@code visibleRect} must be the equal to
     * {@code this.getVisibleRect()}.
     *
     * @param visibleRect the view area visible within the viewport
     * @param orientation {@code SwingConstants.HORIZONTAL} or
     *                    {@code SwingConstants.VERTICAL}
     * @param direction less or equal to zero to scroll up/back,
     *                  greater than zero for down/forward
     * @return the "block" increment for scrolling in the specified direction;
     *         always positive
     * @see #getScrollableUnitIncrement
     * @see Scrollable#getScrollableBlockIncrement
     * @throws IllegalArgumentException if {@code visibleRect} is {@code null}, or
     *         {@code orientation} isn't one of {@code SwingConstants.VERTICAL} or
     *         {@code SwingConstants.HORIZONTAL}
     */
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        checkScrollableParameters(visibleRect, orientation);
        if (orientation == SwingConstants.VERTICAL) {
            int inc = visibleRect.height;
            /* Scroll Down */
            if (direction > 0) {
                // last cell is the lowest left cell
                int last = locationToIndex(new Point(visibleRect.x, visibleRect.y+visibleRect.height-1));
                if (last != -1) {
                    Rectangle lastRect = getCellBounds(last,last);
                    if (lastRect != null) {
                        inc = lastRect.y - visibleRect.y;
                        if ( (inc == 0) && (last < getModel().getSize()-1) ) {
                            inc = lastRect.height;
                        }
                    }
                }
            }
            /* Scroll Up */
            else {
                int newFirst = locationToIndex(new Point(visibleRect.x, visibleRect.y-visibleRect.height));
                int first = getFirstVisibleIndex();
                if (newFirst != -1) {
                    if (first == -1) {
                        first = locationToIndex(visibleRect.getLocation());
                    }
                    Rectangle newFirstRect = getCellBounds(newFirst,newFirst);
                    Rectangle firstRect = getCellBounds(first,first);
                    if ((newFirstRect != null) && (firstRect!=null)) {
                        while ( (newFirstRect.y + visibleRect.height <
                                 firstRect.y + firstRect.height) &&
                                (newFirstRect.y < firstRect.y) ) {
                            newFirst++;
                            newFirstRect = getCellBounds(newFirst,newFirst);
                        }
                        inc = visibleRect.y - newFirstRect.y;
                        if ( (inc <= 0) && (newFirstRect.y > 0)) {
                            newFirst--;
                            newFirstRect = getCellBounds(newFirst,newFirst);
                            if (newFirstRect != null) {
                                inc = visibleRect.y - newFirstRect.y;
                            }
                        }
                    }
                }
            }
            return inc;
        }
        else if (orientation == SwingConstants.HORIZONTAL &&
                 getLayoutOrientation() != JList.VERTICAL) {
            boolean leftToRight = getComponentOrientation().isLeftToRight();
            int inc = visibleRect.width;
            /* Scroll Right (in ltr mode) or Scroll Left (in rtl mode) */
            if (direction > 0) {
                // position is upper right if ltr, or upper left otherwise
                int x = visibleRect.x + (leftToRight ? (visibleRect.width - 1) : 0);
                int last = locationToIndex(new Point(x, visibleRect.y));

                if (last != -1) {
                    Rectangle lastRect = getCellBounds(last,last);
                    if (lastRect != null) {
                        if (leftToRight) {
                            inc = lastRect.x - visibleRect.x;
                        } else {
                            inc = visibleRect.x + visibleRect.width
                                      - (lastRect.x + lastRect.width);
                        }
                        if (inc < 0) {
                            inc += lastRect.width;
                        } else if ( (inc == 0) && (last < getModel().getSize()-1) ) {
                            inc = lastRect.width;
                        }
                    }
                }
            }
            /* Scroll Left (in ltr mode) or Scroll Right (in rtl mode) */
            else {
                // position is upper left corner of the visibleRect shifted
                // left by the visibleRect.width if ltr, or upper right shifted
                // right by the visibleRect.width otherwise
                int x = visibleRect.x + (leftToRight
                                         ? -visibleRect.width
                                         : visibleRect.width - 1 + visibleRect.width);
                int first = locationToIndex(new Point(x, visibleRect.y));

                if (first != -1) {
                    Rectangle firstRect = getCellBounds(first,first);
                    if (firstRect != null) {
                        // the right of the first cell
                        int firstRight = firstRect.x + firstRect.width;

                        if (leftToRight) {
                            if ((firstRect.x < visibleRect.x - visibleRect.width)
                                    && (firstRight < visibleRect.x)) {
                                inc = visibleRect.x - firstRight;
                            } else {
                                inc = visibleRect.x - firstRect.x;
                            }
                        } else {
                            int visibleRight = visibleRect.x + visibleRect.width;

                            if ((firstRight > visibleRight + visibleRect.width)
                                    && (firstRect.x > visibleRight)) {
                                inc = firstRect.x - visibleRight;
                            } else {
                                inc = firstRight - visibleRight;
                            }
                        }
                    }
                }
            }
            return inc;
        }
        return visibleRect.width;
    }


    /**
     * Returns {@code true} if this {@code JList} is displayed in a
     * {@code JViewport} and the viewport is wider than the list's
     * preferred width, or if the layout orientation is {@code HORIZONTAL_WRAP}
     * and {@code visibleRowCount <= 0}; otherwise returns {@code false}.
     * <p>
     * If {@code false}, then don't track the viewport's width. This allows
     * horizontal scrolling if the {@code JViewport} is itself embedded in a
     * {@code JScrollPane}.
     *
     * @return whether or not an enclosing viewport should force the list's
     *         width to match its own
     * @see Scrollable#getScrollableTracksViewportWidth
     */
    @BeanProperty(bound = false)
    public boolean getScrollableTracksViewportWidth() {
        if (getLayoutOrientation() == HORIZONTAL_WRAP &&
                                      getVisibleRowCount() <= 0) {
            return true;
        }
        Container parent = SwingUtilities.getUnwrappedParent(this);
        if (parent instanceof JViewport) {
            return parent.getWidth() > getPreferredSize().width;
        }
        return false;
    }

    /**
     * Returns {@code true} if this {@code JList} is displayed in a
     * {@code JViewport} and the viewport is taller than the list's
     * preferred height, or if the layout orientation is {@code VERTICAL_WRAP}
     * and {@code visibleRowCount <= 0}; otherwise returns {@code false}.
     * <p>
     * If {@code false}, then don't track the viewport's height. This allows
     * vertical scrolling if the {@code JViewport} is itself embedded in a
     * {@code JScrollPane}.
     *
     * @return whether or not an enclosing viewport should force the list's
     *         height to match its own
     * @see Scrollable#getScrollableTracksViewportHeight
     */
    @BeanProperty(bound = false)
    public boolean getScrollableTracksViewportHeight() {
        if (getLayoutOrientation() == VERTICAL_WRAP &&
                     getVisibleRowCount() <= 0) {
            return true;
        }
        Container parent = SwingUtilities.getUnwrappedParent(this);
        if (parent instanceof JViewport) {
            return parent.getHeight() > getPreferredSize().height;
        }
        return false;
    }


    /*
     * See {@code readObject} and {@code writeObject} in {@code JComponent}
     * for more information about serialization in Swing.
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


    /**
     * Returns a {@code String} representation of this {@code JList}.
     * This method is intended to be used only for debugging purposes,
     * and the content and format of the returned {@code String} may vary
     * between implementations. The returned {@code String} may be empty,
     * but may not be {@code null}.
     *
     * @return  a {@code String} representation of this {@code JList}.
     */
    protected String paramString() {
        String selectionForegroundString = (selectionForeground != null ?
                                            selectionForeground.toString() :
                                            "");
        String selectionBackgroundString = (selectionBackground != null ?
                                            selectionBackground.toString() :
                                            "");

        return super.paramString() +
        ",fixedCellHeight=" + fixedCellHeight +
        ",fixedCellWidth=" + fixedCellWidth +
        ",horizontalScrollIncrement=" + horizontalScrollIncrement +
        ",selectionBackground=" + selectionBackgroundString +
        ",selectionForeground=" + selectionForegroundString +
        ",visibleRowCount=" + visibleRowCount +
        ",layoutOrientation=" + layoutOrientation;
    }


    /**
     * --- Accessibility Support ---
     */

    /**
     * Gets the {@code AccessibleContext} associated with this {@code JList}.
     * For {@code JList}, the {@code AccessibleContext} takes the form of an
     * {@code AccessibleJList}.
     * <p>
     * A new {@code AccessibleJList} instance is created if necessary.
     *
     * @return an {@code AccessibleJList} that serves as the
     *         {@code AccessibleContext} of this {@code JList}
     */
    @BeanProperty(bound = false)
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJList();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * {@code JList} class. It provides an implementation of the
     * Java Accessibility API appropriate to list user-interface
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
    protected class AccessibleJList extends AccessibleJComponent
        implements AccessibleSelection, PropertyChangeListener,
        ListSelectionListener, ListDataListener {

        int leadSelectionIndex;

        /**
         * Constructs an {@code AccessibleJList}.
         */
        public AccessibleJList() {
            super();
            JList.this.addPropertyChangeListener(this);
            JList.this.getSelectionModel().addListSelectionListener(this);
            JList.this.getModel().addListDataListener(this);
            leadSelectionIndex = JList.this.getLeadSelectionIndex();
        }

        /**
         * Property Change Listener change method. Used to track changes
         * to the DataModel and ListSelectionModel, in order to re-set
         * listeners to those for reporting changes there via the Accessibility
         * PropertyChange mechanism.
         *
         * @param e PropertyChangeEvent
         */
        public void propertyChange(PropertyChangeEvent e) {
            String name = e.getPropertyName();
            Object oldValue = e.getOldValue();
            Object newValue = e.getNewValue();

                // re-set listData listeners
            if (name.compareTo("model") == 0) {

                if (oldValue != null && oldValue instanceof ListModel) {
                    ((ListModel) oldValue).removeListDataListener(this);
                }
                if (newValue != null && newValue instanceof ListModel) {
                    ((ListModel) newValue).addListDataListener(this);
                }

                // re-set listSelectionModel listeners
            } else if (name.compareTo("selectionModel") == 0) {

                if (oldValue != null && oldValue instanceof ListSelectionModel) {
                    ((ListSelectionModel) oldValue).removeListSelectionListener(this);
                }
                if (newValue != null && newValue instanceof ListSelectionModel) {
                    ((ListSelectionModel) newValue).addListSelectionListener(this);
                }

                firePropertyChange(
                    AccessibleContext.ACCESSIBLE_SELECTION_PROPERTY,
                    Boolean.valueOf(false), Boolean.valueOf(true));
            }
        }

        /**
         * List Selection Listener value change method. Used to fire
         * the property change
         *
         * @param e ListSelectionEvent
         *
         */
        public void valueChanged(ListSelectionEvent e) {
            int oldLeadSelectionIndex = leadSelectionIndex;
            leadSelectionIndex = JList.this.getLeadSelectionIndex();
            if (oldLeadSelectionIndex != leadSelectionIndex) {
                Accessible oldLS, newLS;
                oldLS = (oldLeadSelectionIndex >= 0)
                        ? getAccessibleChild(oldLeadSelectionIndex)
                        : null;
                newLS = (leadSelectionIndex >= 0)
                        ? getAccessibleChild(leadSelectionIndex)
                        : null;
                firePropertyChange(AccessibleContext.ACCESSIBLE_ACTIVE_DESCENDANT_PROPERTY,
                                   oldLS, newLS);
            }

            firePropertyChange(AccessibleContext.ACCESSIBLE_VISIBLE_DATA_PROPERTY,
                               Boolean.valueOf(false), Boolean.valueOf(true));
            firePropertyChange(AccessibleContext.ACCESSIBLE_SELECTION_PROPERTY,
                               Boolean.valueOf(false), Boolean.valueOf(true));

            // Process the State changes for Multiselectable
            AccessibleStateSet s = getAccessibleStateSet();
            ListSelectionModel lsm = JList.this.getSelectionModel();
            if (lsm.getSelectionMode() != ListSelectionModel.SINGLE_SELECTION) {
                if (!s.contains(AccessibleState.MULTISELECTABLE)) {
                    s.add(AccessibleState.MULTISELECTABLE);
                    firePropertyChange(AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                                       null, AccessibleState.MULTISELECTABLE);
                }
            } else {
                if (s.contains(AccessibleState.MULTISELECTABLE)) {
                    s.remove(AccessibleState.MULTISELECTABLE);
                    firePropertyChange(AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                                       AccessibleState.MULTISELECTABLE, null);
                }
            }
        }

        /**
         * List Data Listener interval added method. Used to fire the visible data property change
         *
         * @param e ListDataEvent
         *
         */
        public void intervalAdded(ListDataEvent e) {
            firePropertyChange(AccessibleContext.ACCESSIBLE_VISIBLE_DATA_PROPERTY,
                               Boolean.valueOf(false), Boolean.valueOf(true));
        }

        /**
         * List Data Listener interval removed method. Used to fire the visible data property change
         *
         * @param e ListDataEvent
         *
         */
        public void intervalRemoved(ListDataEvent e) {
            firePropertyChange(AccessibleContext.ACCESSIBLE_VISIBLE_DATA_PROPERTY,
                               Boolean.valueOf(false), Boolean.valueOf(true));
        }

        /**
         * List Data Listener contents changed method. Used to fire the visible data property change
         *
         * @param e ListDataEvent
         *
         */
         public void contentsChanged(ListDataEvent e) {
             firePropertyChange(AccessibleContext.ACCESSIBLE_VISIBLE_DATA_PROPERTY,
                                Boolean.valueOf(false), Boolean.valueOf(true));
         }

    // AccessibleContext methods

        /**
         * Get the state set of this object.
         *
         * @return an instance of AccessibleState containing the current state
         * of the object
         * @see AccessibleState
         */
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet states = super.getAccessibleStateSet();
            if (selectionModel.getSelectionMode() !=
                ListSelectionModel.SINGLE_SELECTION) {
                states.add(AccessibleState.MULTISELECTABLE);
            }
            return states;
        }

        /**
         * Get the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the
         * object
         * @see AccessibleRole
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.LIST;
        }

        /**
         * Returns the <code>Accessible</code> child contained at
         * the local coordinate <code>Point</code>, if one exists.
         * Otherwise returns <code>null</code>.
         *
         * @return the <code>Accessible</code> at the specified
         *    location, if it exists
         */
        public Accessible getAccessibleAt(Point p) {
            int i = locationToIndex(p);
            if (i >= 0) {
                return new AccessibleJListChild(JList.this, i);
            } else {
                return null;
            }
        }

        /**
         * Returns the number of accessible children in the object.  If all
         * of the children of this object implement Accessible, than this
         * method should return the number of children of this object.
         *
         * @return the number of accessible children in the object.
         */
        public int getAccessibleChildrenCount() {
            return getModel().getSize();
        }

        /**
         * Return the nth Accessible child of the object.
         *
         * @param i zero-based index of child
         * @return the nth Accessible child of the object
         */
        public Accessible getAccessibleChild(int i) {
            if (i >= getModel().getSize()) {
                return null;
            } else {
                return new AccessibleJListChild(JList.this, i);
            }
        }

        /**
         * Get the AccessibleSelection associated with this object.  In the
         * implementation of the Java Accessibility API for this class,
         * return this object, which is responsible for implementing the
         * AccessibleSelection interface on behalf of itself.
         *
         * @return this object
         */
        public AccessibleSelection getAccessibleSelection() {
            return this;
        }


    // AccessibleSelection methods

        /**
         * Returns the number of items currently selected.
         * If no items are selected, the return value will be 0.
         *
         * @return the number of items currently selected.
         */
         public int getAccessibleSelectionCount() {
             return JList.this.getSelectedIndices().length;
         }

        /**
         * Returns an Accessible representing the specified selected item
         * in the object.  If there isn't a selection, or there are
         * fewer items selected than the integer passed in, the return
         * value will be <code>null</code>.
         *
         * @param i the zero-based index of selected items
         * @return an Accessible containing the selected item
         */
         public Accessible getAccessibleSelection(int i) {
             int len = getAccessibleSelectionCount();
             if (i < 0 || i >= len) {
                 return null;
             } else {
                 return getAccessibleChild(JList.this.getSelectedIndices()[i]);
             }
         }

        /**
         * Returns true if the current child of this object is selected.
         *
         * @param i the zero-based index of the child in this Accessible
         * object.
         * @see AccessibleContext#getAccessibleChild
         */
        public boolean isAccessibleChildSelected(int i) {
            return isSelectedIndex(i);
        }

        /**
         * Adds the specified selected item in the object to the object's
         * selection.  If the object supports multiple selections,
         * the specified item is added to any existing selection, otherwise
         * it replaces any existing selection in the object.  If the
         * specified item is already selected, this method has no effect.
         *
         * @param i the zero-based index of selectable items
         */
         public void addAccessibleSelection(int i) {
             JList.this.addSelectionInterval(i, i);
         }

        /**
         * Removes the specified selected item in the object from the object's
         * selection.  If the specified item isn't currently selected, this
         * method has no effect.
         *
         * @param i the zero-based index of selectable items
         */
         public void removeAccessibleSelection(int i) {
             JList.this.removeSelectionInterval(i, i);
         }

        /**
         * Clears the selection in the object, so that nothing in the
         * object is selected.
         */
         public void clearAccessibleSelection() {
             JList.this.clearSelection();
         }

        /**
         * Causes every selected item in the object to be selected
         * if the object supports multiple selections.
         */
         public void selectAllAccessibleSelection() {
             JList.this.addSelectionInterval(0, getAccessibleChildrenCount() -1);
         }

          /**
           * This class implements accessibility support appropriate
           * for list children.
           */
        protected class AccessibleJListChild extends AccessibleContext
                implements Accessible, AccessibleComponent, AccessibleAction {
            private JList<E>     parent = null;
            int indexInParent;
            private Component component = null;
            private AccessibleContext accessibleContext = null;
            private ListModel<E> listModel;
            private ListCellRenderer<? super E> cellRenderer = null;

            /**
             * Constructs an {@code AccessibleJListChild}.
             * @param parent the parent
             * @param indexInParent the index in the parent
             */
            public AccessibleJListChild(JList<E> parent, int indexInParent) {
                this.parent = parent;
                this.setAccessibleParent(parent);
                this.indexInParent = indexInParent;
                if (parent != null) {
                    listModel = parent.getModel();
                    cellRenderer = parent.getCellRenderer();
                }
            }

            private Component getCurrentComponent() {
                return getComponentAtIndex(indexInParent);
            }

            AccessibleContext getCurrentAccessibleContext() {
                Component c = getComponentAtIndex(indexInParent);
                if (c instanceof Accessible) {
                    return c.getAccessibleContext();
                } else {
                    return null;
                }
            }

            private Component getComponentAtIndex(int index) {
                if (index < 0 || index >= listModel.getSize()) {
                    return null;
                }
                if ((parent != null)
                        && (listModel != null)
                        && cellRenderer != null) {
                    E value = listModel.getElementAt(index);
                    boolean isSelected = parent.isSelectedIndex(index);
                    boolean isFocussed = parent.isFocusOwner()
                            && (index == parent.getLeadSelectionIndex());
                    return cellRenderer.getListCellRendererComponent(
                            parent,
                            value,
                            index,
                            isSelected,
                            isFocussed);
                } else {
                    return null;
                }
            }


            // Accessible Methods
           /**
            * Get the AccessibleContext for this object. In the
            * implementation of the Java Accessibility API for this class,
            * returns this object, which is its own AccessibleContext.
            *
            * @return this object
            */
            public AccessibleContext getAccessibleContext() {
                return this;
            }


            // AccessibleContext methods

            public String getAccessibleName() {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac != null) {
                    return ac.getAccessibleName();
                } else {
                    return null;
                }
            }

            public void setAccessibleName(String s) {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac != null) {
                    ac.setAccessibleName(s);
                }
            }

            public String getAccessibleDescription() {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac != null) {
                    return ac.getAccessibleDescription();
                } else {
                    return null;
                }
            }

            public void setAccessibleDescription(String s) {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac != null) {
                    ac.setAccessibleDescription(s);
                }
            }

            public AccessibleRole getAccessibleRole() {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac != null) {
                    return ac.getAccessibleRole();
                } else {
                    return null;
                }
            }

            public AccessibleStateSet getAccessibleStateSet() {
                AccessibleContext ac = getCurrentAccessibleContext();
                AccessibleStateSet s;
                if (ac != null) {
                    s = ac.getAccessibleStateSet();
                } else {
                    s = new AccessibleStateSet();
                }

                s.add(AccessibleState.SELECTABLE);
                if (parent.isFocusOwner()
                    && (indexInParent == parent.getLeadSelectionIndex())) {
                    s.add(AccessibleState.ACTIVE);
                }
                if (parent.isSelectedIndex(indexInParent)) {
                    s.add(AccessibleState.SELECTED);
                }
                if (this.isShowing()) {
                    s.add(AccessibleState.SHOWING);
                } else if (s.contains(AccessibleState.SHOWING)) {
                    s.remove(AccessibleState.SHOWING);
                }
                if (this.isVisible()) {
                    s.add(AccessibleState.VISIBLE);
                } else if (s.contains(AccessibleState.VISIBLE)) {
                    s.remove(AccessibleState.VISIBLE);
                }
                s.add(AccessibleState.TRANSIENT); // cell-rendered
                return s;
            }

            public int getAccessibleIndexInParent() {
                return indexInParent;
            }

            public int getAccessibleChildrenCount() {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac != null) {
                    return ac.getAccessibleChildrenCount();
                } else {
                    return 0;
                }
            }

            public Accessible getAccessibleChild(int i) {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac != null) {
                    Accessible accessibleChild = ac.getAccessibleChild(i);
                    ac.setAccessibleParent(this);
                    return accessibleChild;
                } else {
                    return null;
                }
            }

            public Locale getLocale() {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac != null) {
                    return ac.getLocale();
                } else {
                    return null;
                }
            }

            public void addPropertyChangeListener(PropertyChangeListener l) {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac != null) {
                    ac.addPropertyChangeListener(l);
                }
            }

            public void removePropertyChangeListener(PropertyChangeListener l) {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac != null) {
                    ac.removePropertyChangeListener(l);
                }
            }

           /**
            * Get the AccessibleComponent associated with this object.  In the
            * implementation of the Java Accessibility API for this class,
            * return this object, which is responsible for implementing the
            * AccessibleComponent interface on behalf of itself.
            *
            * @return this object
            */
            public AccessibleComponent getAccessibleComponent() {
                return this; // to override getBounds()
            }

            public AccessibleSelection getAccessibleSelection() {
                AccessibleContext ac = getCurrentAccessibleContext();
                return ac != null ? ac.getAccessibleSelection() : null;
            }

            public AccessibleText getAccessibleText() {
                AccessibleContext ac = getCurrentAccessibleContext();
                return ac != null ? ac.getAccessibleText() : null;
            }

            public AccessibleValue getAccessibleValue() {
                AccessibleContext ac = getCurrentAccessibleContext();
                return ac != null ? ac.getAccessibleValue() : null;
            }


            // AccessibleComponent methods

            public Color getBackground() {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac instanceof AccessibleComponent) {
                    return ((AccessibleComponent) ac).getBackground();
                } else {
                    Component c = getCurrentComponent();
                    if (c != null) {
                        return c.getBackground();
                    } else {
                        return null;
                    }
                }
            }

            public void setBackground(Color c) {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac instanceof AccessibleComponent) {
                    ((AccessibleComponent) ac).setBackground(c);
                } else {
                    Component cp = getCurrentComponent();
                    if (cp != null) {
                        cp.setBackground(c);
                    }
                }
            }

            public Color getForeground() {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac instanceof AccessibleComponent) {
                    return ((AccessibleComponent) ac).getForeground();
                } else {
                    Component c = getCurrentComponent();
                    if (c != null) {
                        return c.getForeground();
                    } else {
                        return null;
                    }
                }
            }

            public void setForeground(Color c) {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac instanceof AccessibleComponent) {
                    ((AccessibleComponent) ac).setForeground(c);
                } else {
                    Component cp = getCurrentComponent();
                    if (cp != null) {
                        cp.setForeground(c);
                    }
                }
            }

            public Cursor getCursor() {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac instanceof AccessibleComponent) {
                    return ((AccessibleComponent) ac).getCursor();
                } else {
                    Component c = getCurrentComponent();
                    if (c != null) {
                        return c.getCursor();
                    } else {
                        Accessible ap = getAccessibleParent();
                        if (ap instanceof AccessibleComponent) {
                            return ((AccessibleComponent) ap).getCursor();
                        } else {
                            return null;
                        }
                    }
                }
            }

            public void setCursor(Cursor c) {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac instanceof AccessibleComponent) {
                    ((AccessibleComponent) ac).setCursor(c);
                } else {
                    Component cp = getCurrentComponent();
                    if (cp != null) {
                        cp.setCursor(c);
                    }
                }
            }

            public Font getFont() {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac instanceof AccessibleComponent) {
                    return ((AccessibleComponent) ac).getFont();
                } else {
                    Component c = getCurrentComponent();
                    if (c != null) {
                        return c.getFont();
                    } else {
                        return null;
                    }
                }
            }

            public void setFont(Font f) {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac instanceof AccessibleComponent) {
                    ((AccessibleComponent) ac).setFont(f);
                } else {
                    Component c = getCurrentComponent();
                    if (c != null) {
                        c.setFont(f);
                    }
                }
            }

            public FontMetrics getFontMetrics(Font f) {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac instanceof AccessibleComponent) {
                    return ((AccessibleComponent) ac).getFontMetrics(f);
                } else {
                    Component c = getCurrentComponent();
                    if (c != null) {
                        return c.getFontMetrics(f);
                    } else {
                        return null;
                    }
                }
            }

            public boolean isEnabled() {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac instanceof AccessibleComponent) {
                    return ((AccessibleComponent) ac).isEnabled();
                } else {
                    Component c = getCurrentComponent();
                    if (c != null) {
                        return c.isEnabled();
                    } else {
                        return false;
                    }
                }
            }

            public void setEnabled(boolean b) {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac instanceof AccessibleComponent) {
                    ((AccessibleComponent) ac).setEnabled(b);
                } else {
                    Component c = getCurrentComponent();
                    if (c != null) {
                        c.setEnabled(b);
                    }
                }
            }

            public boolean isVisible() {
                int fi = parent.getFirstVisibleIndex();
                int li = parent.getLastVisibleIndex();
                // The UI incorrectly returns a -1 for the last
                // visible index if the list is smaller than the
                // viewport size.
                if (li == -1) {
                    li = parent.getModel().getSize() - 1;
                }
                return ((indexInParent >= fi)
                        && (indexInParent <= li));
            }

            public void setVisible(boolean b) {
            }

            public boolean isShowing() {
                return (parent.isShowing() && isVisible());
            }

            public boolean contains(Point p) {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac instanceof AccessibleComponent) {
                    Rectangle r = ((AccessibleComponent) ac).getBounds();
                    return r.contains(p);
                } else {
                    Component c = getCurrentComponent();
                    if (c != null) {
                        Rectangle r = c.getBounds();
                        return r.contains(p);
                    } else {
                        return getBounds().contains(p);
                    }
                }
            }

            public Point getLocationOnScreen() {
                if (parent != null) {
                    Point listLocation;
                    try {
                        listLocation = parent.getLocationOnScreen();
                    } catch (IllegalComponentStateException e) {
                        // This can happen if the component isn't visisble
                        return null;
                    }
                    Point componentLocation = parent.indexToLocation(indexInParent);
                    if (componentLocation != null) {
                        componentLocation.translate(listLocation.x, listLocation.y);
                        return componentLocation;
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            }

            public Point getLocation() {
                if (parent != null) {
                    return parent.indexToLocation(indexInParent);
                } else {
                    return null;
                }
            }

            public void setLocation(Point p) {
                if ((parent != null)  && (parent.contains(p))) {
                    ensureIndexIsVisible(indexInParent);
                }
            }

            public Rectangle getBounds() {
                if (parent != null) {
                    return parent.getCellBounds(indexInParent,indexInParent);
                } else {
                    return null;
                }
            }

            public void setBounds(Rectangle r) {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac instanceof AccessibleComponent) {
                    ((AccessibleComponent) ac).setBounds(r);
                }
            }

            public Dimension getSize() {
                Rectangle cellBounds = this.getBounds();
                if (cellBounds != null) {
                    return cellBounds.getSize();
                } else {
                    return null;
                }
            }

            public void setSize (Dimension d) {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac instanceof AccessibleComponent) {
                    ((AccessibleComponent) ac).setSize(d);
                } else {
                    Component c = getCurrentComponent();
                    if (c != null) {
                        c.setSize(d);
                    }
                }
            }

            public Accessible getAccessibleAt(Point p) {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac instanceof AccessibleComponent) {
                    return ((AccessibleComponent) ac).getAccessibleAt(p);
                } else {
                    return null;
                }
            }

            @SuppressWarnings("deprecation")
            public boolean isFocusTraversable() {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac instanceof AccessibleComponent) {
                    return ((AccessibleComponent) ac).isFocusTraversable();
                } else {
                    Component c = getCurrentComponent();
                    if (c != null) {
                        return c.isFocusTraversable();
                    } else {
                        return false;
                    }
                }
            }

            public void requestFocus() {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac instanceof AccessibleComponent) {
                    ((AccessibleComponent) ac).requestFocus();
                } else {
                    Component c = getCurrentComponent();
                    if (c != null) {
                        c.requestFocus();
                    }
                }
            }

            public void addFocusListener(FocusListener l) {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac instanceof AccessibleComponent) {
                    ((AccessibleComponent) ac).addFocusListener(l);
                } else {
                    Component c = getCurrentComponent();
                    if (c != null) {
                        c.addFocusListener(l);
                    }
                }
            }

            public void removeFocusListener(FocusListener l) {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac instanceof AccessibleComponent) {
                    ((AccessibleComponent) ac).removeFocusListener(l);
                } else {
                    Component c = getCurrentComponent();
                    if (c != null) {
                        c.removeFocusListener(l);
                    }
                }
            }

            // TIGER - 4733624
            /**
             * Returns the icon for the element renderer, as the only item
             * of an array of <code>AccessibleIcon</code>s or a <code>null</code> array
             * if the renderer component contains no icons.
             *
             * @return an array containing the accessible icon
             *         or a <code>null</code> array if none
             * @since 1.3
             */
            public AccessibleIcon [] getAccessibleIcon() {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac != null) {
                    return ac.getAccessibleIcon();
                } else {
                    return null;
                }
            }

            /**
             * {@inheritDoc}
             * @implSpec Returns the AccessibleAction for this AccessibleJListChild
             * as follows:  First getListCellRendererComponent of the ListCellRenderer
             * for the component at the "index in parent" of this child is called.
             * Then its AccessibleContext is fetched and that AccessibleContext's
             * AccessibleAction is returned.  Note that if an AccessibleAction
             * is not found using this process then this object with its implementation
             * of the AccessibleAction interface is returned.
             * @since 9
             */
            @Override
            public AccessibleAction getAccessibleAction() {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac == null) {
                    return null;
                } else {
                    AccessibleAction aa = ac.getAccessibleAction();
                    if (aa != null) {
                        return aa;
                    } else {
                        return this;
                    }
                }
            }

            /**
             * {@inheritDoc}
             * @implSpec If i == 0 selects this AccessibleJListChild by calling
             * JList.this.setSelectedIndex(indexInParent) and then returns true;
             * otherwise returns false.
             * @since 9
             */
            @Override
            public boolean doAccessibleAction(int i) {
                if (i == 0) {
                    JList.this.setSelectedIndex(indexInParent);
                    return true;
                } else {
                    return false;
                }
            }

            /**
             * {@inheritDoc}
             * @implSpec If i == 0 returns the action description fetched from
             * UIManager.getString("AbstractButton.clickText");
             * otherwise returns null.
             * @since 9
             */
            @Override
            public String getAccessibleActionDescription(int i) {
                if (i == 0) {
                    return UIManager.getString("AbstractButton.clickText");
                } else {
                    return null;
                }
            }

            /**
             * {@inheritDoc}
             * @implSpec Returns 1, i.e. there is only one action.
             * @since 9
             */
            @Override
            public int getAccessibleActionCount() {
                return 1;
            }

        } // inner class AccessibleJListChild

    } // inner class AccessibleJList
}
