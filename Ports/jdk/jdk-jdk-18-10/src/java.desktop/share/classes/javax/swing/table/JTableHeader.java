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

package javax.swing.table;

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
import java.beans.PropertyChangeListener;
import java.beans.Transient;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Locale;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleComponent;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleSelection;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleText;
import javax.accessibility.AccessibleValue;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.plaf.TableHeaderUI;

import sun.awt.AWTAccessor;
import sun.awt.AWTAccessor.MouseEventAccessor;
import sun.swing.table.DefaultTableCellHeaderRenderer;

/**
 * This is the object which manages the header of the <code>JTable</code>.
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
 * @author Alan Chung
 * @author Philip Milne
 * @see javax.swing.JTable
 */
@SuppressWarnings("serial") // Same-version serialization only
public class JTableHeader extends JComponent implements TableColumnModelListener, Accessible
{
    /**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "TableHeaderUI";

//
// Instance Variables
//
    /**
     * The table for which this object is the header;
     * the default is <code>null</code>.
     */
    protected JTable table;

    /**
     * The <code>TableColumnModel</code> of the table header.
     */
    protected TableColumnModel  columnModel;

    /**
     * If true, reordering of columns are allowed by the user;
     * the default is true.
     */
    protected boolean   reorderingAllowed;

    /**
     * If true, resizing of columns are allowed by the user;
     * the default is true.
     */
    protected boolean   resizingAllowed;

    /**
     * Obsolete as of Java 2 platform v1.3.  Real time repaints, in response
     * to column dragging or resizing, are now unconditional.
     */
    /*
     * If this flag is true, then the header will repaint the table as
     * a column is dragged or resized; the default is true.
     */
    protected boolean   updateTableInRealTime;

    /** The index of the column being resized. <code>null</code> if not resizing. */
    protected transient TableColumn     resizingColumn;

    /** The index of the column being dragged. <code>null</code> if not dragging. */
    protected transient TableColumn     draggedColumn;

    /** The distance from its original position the column has been dragged. */
    protected transient int     draggedDistance;

    /**
      *  The default renderer to be used when a <code>TableColumn</code>
      *  does not define a <code>headerRenderer</code>.
      */
    private TableCellRenderer defaultRenderer;

    /**
     * Flag to indicate UI update is in progress
     */
    private transient boolean updateInProgress;

//
// Constructors
//

    /**
     *  Constructs a <code>JTableHeader</code> with a default
     *  <code>TableColumnModel</code>.
     *
     * @see #createDefaultColumnModel
     */
    public JTableHeader() {
        this(null);
    }

    /**
     *  Constructs a <code>JTableHeader</code> which is initialized with
     *  <code>cm</code> as the column model.  If <code>cm</code> is
     *  <code>null</code> this method will initialize the table header
     *  with a default <code>TableColumnModel</code>.
     *
     * @param cm        the column model for the table
     * @see #createDefaultColumnModel
     */
    public JTableHeader(TableColumnModel cm) {
        super();

        //setFocusable(false); // for strict win/mac compatibility mode,
                               // this method should be invoked

        if (cm == null)
            cm = createDefaultColumnModel();
        setColumnModel(cm);

        // Initialize local ivars
        initializeLocalVars();

        // Get UI going
        updateUI();
    }

//
// Local behavior attributes
//

    /**
     *  Sets the table associated with this header.
     *  @param  table   the new table
     */
    @BeanProperty(description
            = "The table associated with this header.")
    public void setTable(JTable table) {
        JTable old = this.table;
        this.table = table;
        firePropertyChange("table", old, table);
    }

    /**
      *  Returns the table associated with this header.
      *  @return  the <code>table</code> property
      */
    public JTable getTable() {
        return table;
    }

    /**
     *  Sets whether the user can drag column headers to reorder columns.
     *
     * @param   reorderingAllowed       true if the table view should allow
     *                                  reordering; otherwise false
     * @see     #getReorderingAllowed
     */
    @BeanProperty(description
            = "Whether the user can drag column headers to reorder columns.")
    public void setReorderingAllowed(boolean reorderingAllowed) {
        boolean old = this.reorderingAllowed;
        this.reorderingAllowed = reorderingAllowed;
        firePropertyChange("reorderingAllowed", old, reorderingAllowed);
    }

    /**
     * Returns true if the user is allowed to rearrange columns by
     * dragging their headers, false otherwise. The default is true. You can
     * rearrange columns programmatically regardless of this setting.
     *
     * @return  the <code>reorderingAllowed</code> property
     * @see     #setReorderingAllowed
     */
    public boolean getReorderingAllowed() {
        return reorderingAllowed;
    }

    /**
     *  Sets whether the user can resize columns by dragging between headers.
     *
     * @param   resizingAllowed         true if table view should allow
     *                                  resizing
     * @see     #getResizingAllowed
     */
    @BeanProperty(description
            = "Whether the user can resize columns by dragging between headers.")
    public void setResizingAllowed(boolean resizingAllowed) {
        boolean old = this.resizingAllowed;
        this.resizingAllowed = resizingAllowed;
        firePropertyChange("resizingAllowed", old, resizingAllowed);
    }

    /**
     * Returns true if the user is allowed to resize columns by dragging
     * between their headers, false otherwise. The default is true. You can
     * resize columns programmatically regardless of this setting.
     *
     * @return  the <code>resizingAllowed</code> property
     * @see     #setResizingAllowed
     */
    public boolean getResizingAllowed() {
        return resizingAllowed;
    }

    /**
     * Returns the dragged column, if and only if, a drag is in
     * process, otherwise returns <code>null</code>.
     *
     * @return  the dragged column, if a drag is in
     *          process, otherwise returns <code>null</code>
     * @see     #getDraggedDistance
     */
    public TableColumn getDraggedColumn() {
        return draggedColumn;
    }

    /**
     * Returns the column's horizontal distance from its original
     * position, if and only if, a drag is in process. Otherwise, the
     * the return value is meaningless.
     *
     * @return  the column's horizontal distance from its original
     *          position, if a drag is in process, otherwise the return
     *          value is meaningless
     * @see     #getDraggedColumn
     */
    public int getDraggedDistance() {
        return draggedDistance;
    }

    /**
     * Returns the resizing column.  If no column is being
     * resized this method returns <code>null</code>.
     *
     * @return  the resizing column, if a resize is in process, otherwise
     *          returns <code>null</code>
     */
    public TableColumn getResizingColumn() {
        return resizingColumn;
    }

    /**
     * Obsolete as of Java 2 platform v1.3.  Real time repaints, in response to
     * column dragging or resizing, are now unconditional.
     * @param flag true if tableView should update the body of the
     * table in real time
     */
    public void setUpdateTableInRealTime(boolean flag) {
        updateTableInRealTime = flag;
    }

    /**
     * Obsolete as of Java 2 platform v1.3.  Real time repaints, in response to
     * column dragging or resizing, are now unconditional.
     * @return  true if the table updates in real time
     */
    public boolean getUpdateTableInRealTime() {
        return updateTableInRealTime;
    }

    /**
     * Sets the default renderer to be used when no <code>headerRenderer</code>
     * is defined by a <code>TableColumn</code>.
     * @param  defaultRenderer  the default renderer
     * @since 1.3
     */
    public void setDefaultRenderer(TableCellRenderer defaultRenderer) {
        this.defaultRenderer = defaultRenderer;
    }

    /**
     * Returns the default renderer used when no <code>headerRenderer</code>
     * is defined by a <code>TableColumn</code>.
     * @return the default renderer
     * @since 1.3
     */
    @Transient
    public TableCellRenderer getDefaultRenderer() {
        return defaultRenderer;
    }

    /**
     * Returns the index of the column that <code>point</code> lies in, or -1 if it
     * lies out of bounds.
     *
     * @param point  if this <code>point</code> lies within a column, the index of
     *               that column will be returned; otherwise it is out of bounds
     *               and -1 is returned
     *
     * @return  the index of the column that <code>point</code> lies in, or -1 if it
     *          lies out of bounds
     */
    public int columnAtPoint(Point point) {
        int x = point.x;
        if (!getComponentOrientation().isLeftToRight()) {
            x = getWidthInRightToLeft() - x - 1;
        }
        return getColumnModel().getColumnIndexAtX(x);
    }

    /**
     * Returns the rectangle containing the header tile at <code>column</code>.
     * When the <code>column</code> parameter is out of bounds this method uses the
     * same conventions as the <code>JTable</code> method <code>getCellRect</code>.
     *
     * @param column  index of the column
     *
     * @return  the rectangle containing the header tile at <code>column</code>
     * @see JTable#getCellRect
     */
    public Rectangle getHeaderRect(int column) {
        Rectangle r = new Rectangle();
        TableColumnModel cm = getColumnModel();

        r.height = getHeight();

        if (column < 0) {
            // x = width = 0;
            if( !getComponentOrientation().isLeftToRight() ) {
                r.x = getWidthInRightToLeft();
            }
        }
        else if (column >= cm.getColumnCount()) {
            if( getComponentOrientation().isLeftToRight() ) {
                r.x = getWidth();
            }
        }
        else {
            for(int i = 0; i < column; i++) {
                r.x += cm.getColumn(i).getWidth();
            }
            if( !getComponentOrientation().isLeftToRight() ) {
                r.x = getWidthInRightToLeft() - r.x - cm.getColumn(column).getWidth();
            }

            r.width = cm.getColumn(column).getWidth();
        }
        return r;
    }


    /**
     * Allows the renderer's tips to be used if there is text set.
     * @param  event  the location of the event identifies the proper
     *                          renderer and, therefore, the proper tip
     * @return the tool tip for this component
     */
    @SuppressWarnings("deprecation")
    public String getToolTipText(MouseEvent event) {
        String tip = null;
        Point p = event.getPoint();
        int column;

        // Locate the renderer under the event location
        if ((column = columnAtPoint(p)) != -1) {
            TableColumn aColumn = columnModel.getColumn(column);
            TableCellRenderer renderer = aColumn.getHeaderRenderer();
            if (renderer == null) {
                renderer = defaultRenderer;
            }
            Component component = renderer.getTableCellRendererComponent(
                              getTable(), aColumn.getHeaderValue(), false, false,
                              -1, column);

            // Now have to see if the component is a JComponent before
            // getting the tip
            if (component instanceof JComponent) {
                // Convert the event to the renderer's coordinate system
                MouseEvent newEvent;
                Rectangle cellRect = getHeaderRect(column);

                p.translate(-cellRect.x, -cellRect.y);
                newEvent = new MouseEvent(component, event.getID(),
                                          event.getWhen(), event.getModifiers(),
                                          p.x, p.y, event.getXOnScreen(), event.getYOnScreen(),
                                          event.getClickCount(),
                                          event.isPopupTrigger(), MouseEvent.NOBUTTON);
                MouseEventAccessor meAccessor = AWTAccessor.getMouseEventAccessor();
                meAccessor.setCausedByTouchEvent(newEvent,
                    meAccessor.isCausedByTouchEvent(event));

                tip = ((JComponent)component).getToolTipText(newEvent);
            }
        }

        // No tip from the renderer get our own tip
        if (tip == null)
            tip = getToolTipText();

        return tip;
    }

    /**
     * Returns the preferred size of the table header.
     * This is the size required to display the header and requested for
     * the viewport.
     * The returned {@code Dimension} {@code width} will always be calculated by
     * the underlying TableHeaderUI, regardless of any width specified by
     * {@link JComponent#setPreferredSize(java.awt.Dimension)}
     *
     * @return the size
     */
    @Override
    public Dimension getPreferredSize() {
        Dimension preferredSize = super.getPreferredSize();
        if (isPreferredSizeSet() && ui != null) {
            Dimension size = ui.getPreferredSize(this);
            if (size != null) preferredSize.width = size.width;
        }
        return preferredSize;
    }

//
// Managing TableHeaderUI
//

    /**
     * Returns the look and feel (L&amp;F) object that renders this component.
     *
     * @return the <code>TableHeaderUI</code> object that renders this component
     */
    public TableHeaderUI getUI() {
        return (TableHeaderUI)ui;
    }

    /**
     * Sets the look and feel (L&amp;F) object that renders this component.
     *
     * @param ui  the <code>TableHeaderUI</code> L&amp;F object
     * @see UIDefaults#getUI
     */
    public void setUI(TableHeaderUI ui){
        if (this.ui != ui) {
            super.setUI(ui);
            repaint();
        }
    }

    /**
     * Notification from the <code>UIManager</code> that the look and feel
     * (L&amp;F) has changed.
     * Replaces the current UI object with the latest version from the
     * <code>UIManager</code>.
     *
     * @see JComponent#updateUI
     */
    public void updateUI(){
        if (!updateInProgress) {
            updateInProgress = true;
            try {
                setUI((TableHeaderUI)UIManager.getUI(this));

                TableCellRenderer renderer = getDefaultRenderer();
                if (renderer instanceof Component) {
                    SwingUtilities.updateComponentTreeUI((Component)renderer);
                }
            } finally {
                updateInProgress = false;
            }
        }
    }


    /**
     * Returns the suffix used to construct the name of the look and feel
     * (L&amp;F) class used to render this component.
     *
     * @return the string "TableHeaderUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    public String getUIClassID() {
        return uiClassID;
    }


//
// Managing models
//


    /**
     *  Sets the column model for this table to <code>newModel</code> and registers
     *  for listener notifications from the new column model.
     *
     * @param   columnModel     the new data source for this table
     * @exception IllegalArgumentException
     *                          if <code>newModel</code> is <code>null</code>
     * @see     #getColumnModel
     */
    @BeanProperty(description
            = "The object governing the way columns appear in the view.")
    public void setColumnModel(TableColumnModel columnModel) {
        if (columnModel == null) {
            throw new IllegalArgumentException("Cannot set a null ColumnModel");
        }
        TableColumnModel old = this.columnModel;
        if (columnModel != old) {
            if (old != null) {
                old.removeColumnModelListener(this);
            }
            this.columnModel = columnModel;
            columnModel.addColumnModelListener(this);

            firePropertyChange("columnModel", old, columnModel);
            resizeAndRepaint();
        }
    }

    /**
     * Returns the <code>TableColumnModel</code> that contains all column information
     * of this table header.
     *
     * @return  the <code>columnModel</code> property
     * @see     #setColumnModel
     */
    public TableColumnModel getColumnModel() {
        return columnModel;
    }

//
// Implementing TableColumnModelListener interface
//

    /**
     * Invoked when a column is added to the table column model.
     * <p>
     * Application code will not use these methods explicitly, they
     * are used internally by <code>JTable</code>.
     *
     * @param e  the event received
     * @see TableColumnModelListener
     */
    public void columnAdded(TableColumnModelEvent e) { resizeAndRepaint(); }


    /**
     * Invoked when a column is removed from the table column model.
     * <p>
     * Application code will not use these methods explicitly, they
     * are used internally by <code>JTable</code>.
     *
     * @param e  the event received
     * @see TableColumnModelListener
     */
    public void columnRemoved(TableColumnModelEvent e) { resizeAndRepaint(); }


    /**
     * Invoked when a column is repositioned.
     * <p>
     * Application code will not use these methods explicitly, they
     * are used internally by <code>JTable</code>.
     *
     * @param e the event received
     * @see TableColumnModelListener
     */
    public void columnMoved(TableColumnModelEvent e) { repaint(); }


    /**
     * Invoked when a column is moved due to a margin change.
     * <p>
     * Application code will not use these methods explicitly, they
     * are used internally by <code>JTable</code>.
     *
     * @param e the event received
     * @see TableColumnModelListener
     */
    public void columnMarginChanged(ChangeEvent e) { resizeAndRepaint(); }


    // --Redrawing the header is slow in cell selection mode.
    // --Since header selection is ugly and it is always clear from the
    // --view which columns are selected, don't redraw the header.
    /**
     * Invoked when the selection model of the <code>TableColumnModel</code>
     * is changed.  This method currently has no effect (the header is not
     * redrawn).
     * <p>
     * Application code will not use these methods explicitly, they
     * are used internally by <code>JTable</code>.
     *
     * @param e the event received
     * @see TableColumnModelListener
     */
    public void columnSelectionChanged(ListSelectionEvent e) { } // repaint(); }

//
//  Package Methods
//

    /**
     *  Returns the default column model object which is
     *  a <code>DefaultTableColumnModel</code>.  A subclass can override this
     *  method to return a different column model object
     *
     * @return the default column model object
     */
    protected TableColumnModel createDefaultColumnModel() {
        return new DefaultTableColumnModel();
    }

    /**
     *  Returns a default renderer to be used when no header renderer
     *  is defined by a <code>TableColumn</code>.
     *
     *  @return the default table column renderer
     * @since 1.3
     */
    protected TableCellRenderer createDefaultRenderer() {
        return new DefaultTableCellHeaderRenderer();
    }


    /**
     * Initializes the local variables and properties with default values.
     * Used by the constructor methods.
     */
    protected void initializeLocalVars() {
        setOpaque(true);
        table = null;
        reorderingAllowed = true;
        resizingAllowed = true;
        draggedColumn = null;
        draggedDistance = 0;
        resizingColumn = null;
        updateTableInRealTime = true;

        // I'm registered to do tool tips so we can draw tips for the
        // renderers
        ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
        toolTipManager.registerComponent(this);
        setDefaultRenderer(createDefaultRenderer());
    }

    /**
     * Sizes the header and marks it as needing display.  Equivalent
     * to <code>revalidate</code> followed by <code>repaint</code>.
     */
    public void resizeAndRepaint() {
        revalidate();
        repaint();
    }

    /**
      *  Sets the header's <code>draggedColumn</code> to <code>aColumn</code>.
      *  <p>
      *  Application code will not use this method explicitly, it is used
      *  internally by the column dragging mechanism.
      *
      *  @param  aColumn  the column being dragged, or <code>null</code> if
      *                 no column is being dragged
      */
    public void setDraggedColumn(TableColumn aColumn) {
        draggedColumn = aColumn;
    }

    /**
      *  Sets the header's <code>draggedDistance</code> to <code>distance</code>.
      *  @param distance  the distance dragged
      */
    public void setDraggedDistance(int distance) {
        draggedDistance = distance;
    }

    /**
      *  Sets the header's <code>resizingColumn</code> to <code>aColumn</code>.
      *  <p>
      *  Application code will not use this method explicitly, it
      *  is used internally by the column sizing mechanism.
      *
      *  @param  aColumn  the column being resized, or <code>null</code> if
      *                 no column is being resized
      */
    public void setResizingColumn(TableColumn aColumn) {
        resizingColumn = aColumn;
    }

    /**
     * See <code>readObject</code> and <code>writeObject</code> in
     * <code>JComponent</code> for more
     * information about serialization in Swing.
     */
    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        if ((ui != null) && (getUIClassID().equals(uiClassID))) {
            ui.installUI(this);
        }
    }

    private int getWidthInRightToLeft() {
        if ((table != null) &&
            (table.getAutoResizeMode() != JTable.AUTO_RESIZE_OFF)) {
            return table.getWidth();
        }
        return super.getWidth();
    }

    /**
     * Returns a string representation of this <code>JTableHeader</code>. This method
     * is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     * <P>
     * Overriding <code>paramString</code> to provide information about the
     * specific new aspects of the JFC components.
     *
     * @return  a string representation of this <code>JTableHeader</code>
     */
    protected String paramString() {
        String reorderingAllowedString = (reorderingAllowed ?
                                          "true" : "false");
        String resizingAllowedString = (resizingAllowed ?
                                        "true" : "false");
        String updateTableInRealTimeString = (updateTableInRealTime ?
                                              "true" : "false");

        return super.paramString() +
        ",draggedDistance=" + draggedDistance +
        ",reorderingAllowed=" + reorderingAllowedString +
        ",resizingAllowed=" + resizingAllowedString +
        ",updateTableInRealTime=" + updateTableInRealTimeString;
    }

/////////////////
// Accessibility support
////////////////

    /**
     * Gets the AccessibleContext associated with this JTableHeader.
     * For JTableHeaders, the AccessibleContext takes the form of an
     * AccessibleJTableHeader.
     * A new AccessibleJTableHeader instance is created if necessary.
     *
     * @return an AccessibleJTableHeader that serves as the
     *         AccessibleContext of this JTableHeader
     */
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJTableHeader();
        }
        return accessibleContext;
    }

    //
    // *** should also implement AccessibleSelection?
    // *** and what's up with keyboard navigation/manipulation?
    //
    /**
     * This class implements accessibility support for the
     * <code>JTableHeader</code> class.  It provides an implementation of the
     * Java Accessibility API appropriate to table header user-interface
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
    protected class AccessibleJTableHeader extends AccessibleJComponent {

        /**
         * Constructs an {@code AccessibleJTableHeader}.
         */
        protected AccessibleJTableHeader() {}

        /**
         * Get the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the
         * object
         * @see AccessibleRole
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.PANEL;
        }

        /**
         * Returns the Accessible child, if one exists, contained at the local
         * coordinate Point.
         *
         * @param p The point defining the top-left corner of the Accessible,
         * given in the coordinate space of the object's parent.
         * @return the Accessible, if it exists, at the specified location;
         * else null
         */
        public Accessible getAccessibleAt(Point p) {
            int column;

            // Locate the renderer under the Point
            if ((column = JTableHeader.this.columnAtPoint(p)) != -1) {
                TableColumn aColumn = JTableHeader.this.columnModel.getColumn(column);
                TableCellRenderer renderer = aColumn.getHeaderRenderer();
                if (renderer == null) {
                    if (defaultRenderer != null) {
                        renderer = defaultRenderer;
                    } else {
                        return null;
                    }
                }
                Component component = renderer.getTableCellRendererComponent(
                                  JTableHeader.this.getTable(),
                                  aColumn.getHeaderValue(), false, false,
                                  -1, column);

                return new AccessibleJTableHeaderEntry(column, JTableHeader.this, JTableHeader.this.table);
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
            return JTableHeader.this.columnModel.getColumnCount();
        }

        /**
         * Return the nth Accessible child of the object.
         *
         * @param i zero-based index of child
         * @return the nth Accessible child of the object
         */
        public Accessible getAccessibleChild(int i) {
            if (i < 0 || i >= getAccessibleChildrenCount()) {
                return null;
            } else {
                TableColumn aColumn = JTableHeader.this.columnModel.getColumn(i)
;
                TableCellRenderer renderer = aColumn.getHeaderRenderer();
                if (renderer == null) {
                    if (defaultRenderer != null) {
                        renderer = defaultRenderer;
                    } else {
                        return null;
                    }
                }
                Component component = renderer.getTableCellRendererComponent(
                                  JTableHeader.this.getTable(),
                                  aColumn.getHeaderValue(), false, false,
                                  -1, i);

                return new AccessibleJTableHeaderEntry(i, JTableHeader.this, JTableHeader.this.table);
            }
        }

      /**
       * This class provides an implementation of the Java Accessibility
       * API appropriate for JTableHeader entries.
       */
        protected class AccessibleJTableHeaderEntry extends AccessibleContext
            implements Accessible, AccessibleComponent  {

            private JTableHeader parent;
            private int column;
            private JTable table;

            /**
             *  Constructs an AccessiblJTableHeaaderEntry
             * @since 1.4
             *
             * @param c  the column index
             * @param p  the parent <code>JTableHeader</code>
             * @param t  the table <code>JTable</code>
             */
            public AccessibleJTableHeaderEntry(int c, JTableHeader p, JTable t) {
                parent = p;
                column = c;
                table = t;
                this.setAccessibleParent(parent);
            }

            /**
             * Get the AccessibleContext associated with this object.
             * In the implementation of the Java Accessibility API
             * for this class, returns this object, which serves as
             * its own AccessibleContext.
             *
             * @return this object
             */
            public AccessibleContext getAccessibleContext() {
                return this;
            }

            private AccessibleContext getCurrentAccessibleContext() {
                TableColumnModel tcm = table.getColumnModel();
                if (tcm != null) {
                    // Fixes 4772355 - ArrayOutOfBoundsException in
                    // JTableHeader
                    if (column < 0 || column >= tcm.getColumnCount()) {
                        return null;
                    }
                    TableColumn aColumn = tcm.getColumn(column);
                    TableCellRenderer renderer = aColumn.getHeaderRenderer();
                    if (renderer == null) {
                        if (defaultRenderer != null) {
                            renderer = defaultRenderer;
                        } else {
                            return null;
                        }
                    }
                    Component c = renderer.getTableCellRendererComponent(
                                      JTableHeader.this.getTable(),
                                      aColumn.getHeaderValue(), false, false,
                                      -1, column);
                    if (c instanceof Accessible) {
                        return ((Accessible) c).getAccessibleContext();
                    }
                }
                return null;
            }

            private Component getCurrentComponent() {
                TableColumnModel tcm = table.getColumnModel();
                if (tcm != null) {
                    // Fixes 4772355 - ArrayOutOfBoundsException in
                    // JTableHeader
                    if (column < 0 || column >= tcm.getColumnCount()) {
                        return null;
                    }
                    TableColumn aColumn = tcm.getColumn(column);
                    TableCellRenderer renderer = aColumn.getHeaderRenderer();
                    if (renderer == null) {
                        if (defaultRenderer != null) {
                            renderer = defaultRenderer;
                        } else {
                            return null;
                        }
                    }
                    return renderer.getTableCellRendererComponent(
                                      JTableHeader.this.getTable(),
                                      aColumn.getHeaderValue(), false, false,
                                      -1, column);
                } else {
                    return null;
                }
            }

        // AccessibleContext methods

            public String getAccessibleName() {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac != null) {
                    String name = ac.getAccessibleName();
                    if ((name != null) && (name != "")) {
                        // return the cell renderer's AccessibleName
                        return name;
                    }
                }
                if ((accessibleName != null) && (accessibleName != "")) {
                    return accessibleName;
                } else {
                    // fall back to the client property
                    String name = (String)getClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY);
                    if (name != null) {
                        return name;
                    } else {
                        return table.getColumnName(column);
                    }
                }
            }

            public void setAccessibleName(String s) {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac != null) {
                    ac.setAccessibleName(s);
                } else {
                    super.setAccessibleName(s);
                }
            }

            //
            // *** should check toolTip text for desc. (needs MouseEvent)
            //
            public String getAccessibleDescription() {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac != null) {
                    return ac.getAccessibleDescription();
                } else {
                    return super.getAccessibleDescription();
                }
            }

            public void setAccessibleDescription(String s) {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac != null) {
                    ac.setAccessibleDescription(s);
                } else {
                    super.setAccessibleDescription(s);
                }
            }

            public AccessibleRole getAccessibleRole() {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac != null) {
                    return ac.getAccessibleRole();
                } else {
                    return AccessibleRole.COLUMN_HEADER;
                }
            }

            public AccessibleStateSet getAccessibleStateSet() {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac != null) {
                    AccessibleStateSet states = ac.getAccessibleStateSet();
                    if (isShowing()) {
                        states.add(AccessibleState.SHOWING);
                    }
                    return states;
                } else {
                    return new AccessibleStateSet();  // must be non null?
                }
            }

            public int getAccessibleIndexInParent() {
                return column;
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
                } else {
                    super.addPropertyChangeListener(l);
                }
            }

            public void removePropertyChangeListener(PropertyChangeListener l) {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac != null) {
                    ac.removePropertyChangeListener(l);
                } else {
                    super.removePropertyChangeListener(l);
                }
            }

            public AccessibleAction getAccessibleAction() {
                return getCurrentAccessibleContext().getAccessibleAction();
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
                return getCurrentAccessibleContext().getAccessibleSelection();
            }

            public AccessibleText getAccessibleText() {
                return getCurrentAccessibleContext().getAccessibleText();
            }

            public AccessibleValue getAccessibleValue() {
                return getCurrentAccessibleContext().getAccessibleValue();
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
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac instanceof AccessibleComponent) {
                    return ((AccessibleComponent) ac).isVisible();
                } else {
                    Component c = getCurrentComponent();
                    if (c != null) {
                        return c.isVisible();
                    } else {
                        return false;
                    }
                }
            }

            public void setVisible(boolean b) {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac instanceof AccessibleComponent) {
                    ((AccessibleComponent) ac).setVisible(b);
                } else {
                    Component c = getCurrentComponent();
                    if (c != null) {
                        c.setVisible(b);
                    }
                }
            }

            public boolean isShowing() {
                if (isVisible() && JTableHeader.this.isShowing()) {
                    return true;
                } else {
                    return false;
                }
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
                    Point parentLocation = parent.getLocationOnScreen();
                    Point componentLocation = getLocation();
                    componentLocation.translate(parentLocation.x, parentLocation.y);
                    return componentLocation;
                } else {
                    return null;
                }
            }

            public Point getLocation() {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac instanceof AccessibleComponent) {
                    Rectangle r = ((AccessibleComponent) ac).getBounds();
                    return r.getLocation();
                } else {
                    Component c = getCurrentComponent();
                    if (c != null) {
                        Rectangle r = c.getBounds();
                        return r.getLocation();
                    } else {
                        return getBounds().getLocation();
                    }
                }
            }

            public void setLocation(Point p) {
//                if ((parent != null)  && (parent.contains(p))) {
//                    ensureIndexIsVisible(indexInParent);
//                }
            }

            public Rectangle getBounds() {
                  Rectangle r = table.getCellRect(-1, column, false);
                  r.y = 0;
                  return r;

//                AccessibleContext ac = getCurrentAccessibleContext();
//                if (ac instanceof AccessibleComponent) {
//                    return ((AccessibleComponent) ac).getBounds();
//                } else {
//                  Component c = getCurrentComponent();
//                  if (c != null) {
//                      return c.getBounds();
//                  } else {
//                      Rectangle r = table.getCellRect(-1, column, false);
//                      r.y = 0;
//                      return r;
//                  }
//              }
            }

            public void setBounds(Rectangle r) {
                AccessibleContext ac = getCurrentAccessibleContext();
                if (ac instanceof AccessibleComponent) {
                    ((AccessibleComponent) ac).setBounds(r);
                } else {
                    Component c = getCurrentComponent();
                    if (c != null) {
                        c.setBounds(r);
                    }
                }
            }

            public Dimension getSize() {
                return getBounds().getSize();
//                AccessibleContext ac = getCurrentAccessibleContext();
//                if (ac instanceof AccessibleComponent) {
//                    Rectangle r = ((AccessibleComponent) ac).getBounds();
//                    return r.getSize();
//                } else {
//                    Component c = getCurrentComponent();
//                    if (c != null) {
//                        Rectangle r = c.getBounds();
//                        return r.getSize();
//                    } else {
//                        return getBounds().getSize();
//                    }
//                }
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

        } // inner class AccessibleJTableHeaderElement

    }  // inner class AccessibleJTableHeader

}  // End of Class JTableHeader
