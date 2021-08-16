/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.text;

import java.awt.*;
import java.util.BitSet;
import java.util.Vector;
import javax.swing.SizeRequirements;
import javax.swing.event.DocumentEvent;

import javax.swing.text.html.HTML;

/**
 * <p>
 * Implements View interface for a table, that is composed of an
 * element structure where the child elements of the element
 * this view is responsible for represent rows and the child
 * elements of the row elements are cells.  The cell elements can
 * have an arbitrary element structure under them, which will
 * be built with the ViewFactory returned by the getViewFactory
 * method.
 * <pre>
 *
 * &nbsp;  TABLE
 * &nbsp;    ROW
 * &nbsp;      CELL
 * &nbsp;      CELL
 * &nbsp;    ROW
 * &nbsp;      CELL
 * &nbsp;      CELL
 *
 * </pre>
 * <p>
 * This is implemented as a hierarchy of boxes, the table itself
 * is a vertical box, the rows are horizontal boxes, and the cells
 * are vertical boxes.  The cells are allowed to span multiple
 * columns and rows.  By default, the table can be thought of as
 * being formed over a grid (i.e. somewhat like one would find in
 * gridbag layout), where table cells can request to span more
 * than one grid cell.  The default horizontal span of table cells
 * will be based upon this grid, but can be changed by reimplementing
 * the requested span of the cell (i.e. table cells can have independent
 * spans if desired).
 *
 * @author  Timothy Prinzing
 * @see     View
 */
public abstract class TableView extends BoxView {

    /**
     * Constructs a TableView for the given element.
     *
     * @param elem the element that this view is responsible for
     */
    public TableView(Element elem) {
        super(elem, View.Y_AXIS);
        rows = new Vector<TableRow>();
        gridValid = false;
        totalColumnRequirements = new SizeRequirements();
    }

    /**
     * Creates a new table row.
     *
     * @param elem an element
     * @return the row
     */
    protected TableRow createTableRow(Element elem) {
        return new TableRow(elem);
    }

    /**
     * @deprecated Table cells can now be any arbitrary
     * View implementation and should be produced by the
     * ViewFactory rather than the table.
     *
     * @param elem an element
     * @return the cell
     */
    @Deprecated
    protected TableCell createTableCell(Element elem) {
        return new TableCell(elem);
    }

    /**
     * The number of columns in the table.
     */
    int getColumnCount() {
        return columnSpans.length;
    }

    /**
     * Fetches the span (width) of the given column.
     * This is used by the nested cells to query the
     * sizes of grid locations outside of themselves.
     */
    int getColumnSpan(int col) {
        return columnSpans[col];
    }

    /**
     * The number of rows in the table.
     */
    int getRowCount() {
        return rows.size();
    }

    /**
     * Fetches the span (height) of the given row.
     */
    int getRowSpan(int row) {
        View rv = getRow(row);
        if (rv != null) {
            return (int) rv.getPreferredSpan(Y_AXIS);
        }
        return 0;
    }

    TableRow getRow(int row) {
        if (row < rows.size()) {
            return rows.elementAt(row);
        }
        return null;
    }

    /**
     * Determines the number of columns occupied by
     * the table cell represented by given element.
     */
    /*protected*/ int getColumnsOccupied(View v) {
        // PENDING(prinz) this code should be in the html
        // paragraph, but we can't add api to enable it.
        AttributeSet a = v.getElement().getAttributes();
        String s = (String) a.getAttribute(HTML.Attribute.COLSPAN);
        if (s != null) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException nfe) {
                // fall through to one column
            }
        }

        return 1;
    }

    /**
     * Determines the number of rows occupied by
     * the table cell represented by given element.
     */
    /*protected*/ int getRowsOccupied(View v) {
        // PENDING(prinz) this code should be in the html
        // paragraph, but we can't add api to enable it.
        AttributeSet a = v.getElement().getAttributes();
        String s = (String) a.getAttribute(HTML.Attribute.ROWSPAN);
        if (s != null) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException nfe) {
                // fall through to one row
            }
        }

        return 1;
    }

    /*protected*/ void invalidateGrid() {
        gridValid = false;
    }

    protected void forwardUpdate(DocumentEvent.ElementChange ec,
                                     DocumentEvent e, Shape a, ViewFactory f) {
        super.forwardUpdate(ec, e, a, f);
        // A change in any of the table cells usually effects the whole table,
        // so redraw it all!
        if (a != null) {
            Component c = getContainer();
            if (c != null) {
                Rectangle alloc = (a instanceof Rectangle) ? (Rectangle)a :
                                   a.getBounds();
                c.repaint(alloc.x, alloc.y, alloc.width, alloc.height);
            }
        }
    }

    /**
     * Change the child views.  This is implemented to
     * provide the superclass behavior and invalidate the
     * grid so that rows and columns will be recalculated.
     */
    public void replace(int offset, int length, View[] views) {
        super.replace(offset, length, views);
        invalidateGrid();
    }

    /**
     * Fill in the grid locations that are placeholders
     * for multi-column, multi-row, and missing grid
     * locations.
     */
    void updateGrid() {
        if (! gridValid) {
            // determine which views are table rows and clear out
            // grid points marked filled.
            rows.removeAllElements();
            int n = getViewCount();
            for (int i = 0; i < n; i++) {
                View v = getView(i);
                if (v instanceof TableRow) {
                    rows.addElement((TableRow) v);
                    TableRow rv = (TableRow) v;
                    rv.clearFilledColumns();
                    rv.setRow(i);
                }
            }

            int maxColumns = 0;
            int nrows = rows.size();
            for (int row = 0; row < nrows; row++) {
                TableRow rv = getRow(row);
                int col = 0;
                for (int cell = 0; cell < rv.getViewCount(); cell++, col++) {
                    View cv = rv.getView(cell);
                    // advance to a free column
                    for (; rv.isFilled(col); col++);
                    int rowSpan = getRowsOccupied(cv);
                    int colSpan = getColumnsOccupied(cv);
                    if ((colSpan > 1) || (rowSpan > 1)) {
                        // fill in the overflow entries for this cell
                        int rowLimit = row + rowSpan;
                        int colLimit = col + colSpan;
                        for (int i = row; i < rowLimit; i++) {
                            for (int j = col; j < colLimit; j++) {
                                if (i != row || j != col) {
                                    addFill(i, j);
                                }
                            }
                        }
                        if (colSpan > 1) {
                            col += colSpan - 1;
                        }
                    }
                }
                maxColumns = Math.max(maxColumns, col);
            }

            // setup the column layout/requirements
            columnSpans = new int[maxColumns];
            columnOffsets = new int[maxColumns];
            columnRequirements = new SizeRequirements[maxColumns];
            for (int i = 0; i < maxColumns; i++) {
                columnRequirements[i] = new SizeRequirements();
            }
            gridValid = true;
        }
    }

    /**
     * Mark a grid location as filled in for a cells overflow.
     */
    void addFill(int row, int col) {
        TableRow rv = getRow(row);
        if (rv != null) {
            rv.fillColumn(col);
        }
    }

    /**
     * Lays out the columns to fit within the given target span.
     * Returns the results through {@code offsets} and {@code spans}.
     *
     * @param targetSpan the given span for total of all the table
     *  columns
     * @param reqs the requirements desired for each column.  This
     *  is the column maximum of the cells minimum, preferred, and
     *  maximum requested span
     * @param spans the return value of how much to allocated to
     *  each column
     * @param offsets the return value of the offset from the
     *  origin for each column
     */
    protected void layoutColumns(int targetSpan, int[] offsets, int[] spans,
                                 SizeRequirements[] reqs) {
        // allocate using the convenience method on SizeRequirements
        SizeRequirements.calculateTiledPositions(targetSpan, null, reqs,
                                                 offsets, spans);
    }

    /**
     * Perform layout for the minor axis of the box (i.e. the
     * axis orthogonal to the axis that it represents).  The results
     * of the layout should be placed in the given arrays which represent
     * the allocations to the children along the minor axis.  This
     * is called by the superclass whenever the layout needs to be
     * updated along the minor axis.
     * <p>
     * This is implemented to call the
     * {@link #layoutColumns layoutColumns} method, and then
     * forward to the superclass to actually carry out the layout
     * of the tables rows.
     *
     * @param targetSpan the total span given to the view, which
     *  would be used to layout the children.
     * @param axis the axis being layed out.
     * @param offsets the offsets from the origin of the view for
     *  each of the child views.  This is a return value and is
     *  filled in by the implementation of this method.
     * @param spans the span of each child view.  This is a return
     *  value and is filled in by the implementation of this method.
     */
    protected void layoutMinorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
        // make grid is properly represented
        updateGrid();

        // all of the row layouts are invalid, so mark them that way
        int n = getRowCount();
        for (int i = 0; i < n; i++) {
            TableRow row = getRow(i);
            row.layoutChanged(axis);
        }

        // calculate column spans
        layoutColumns(targetSpan, columnOffsets, columnSpans, columnRequirements);

        // continue normal layout
        super.layoutMinorAxis(targetSpan, axis, offsets, spans);
    }

    /**
     * Calculate the requirements for the minor axis.  This is called by
     * the superclass whenever the requirements need to be updated (i.e.
     * a preferenceChanged was messaged through this view).
     * <p>
     * This is implemented to calculate the requirements as the sum of the
     * requirements of the columns.
     */
    protected SizeRequirements calculateMinorAxisRequirements(int axis, SizeRequirements r) {
        updateGrid();

        // calculate column requirements for each column
        calculateColumnRequirements(axis);


        // the requirements are the sum of the columns.
        if (r == null) {
            r = new SizeRequirements();
        }
        long min = 0;
        long pref = 0;
        long max = 0;
        for (SizeRequirements req : columnRequirements) {
            min += req.minimum;
            pref += req.preferred;
            max += req.maximum;
        }
        r.minimum = (int) min;
        r.preferred = (int) pref;
        r.maximum = (int) max;
        r.alignment = 0;

        totalColumnRequirements.minimum = r.minimum;
        totalColumnRequirements.preferred = r.preferred;
        totalColumnRequirements.maximum = r.maximum;

        return r;
    }

    /*
    boolean shouldTrace() {
        AttributeSet a = getElement().getAttributes();
        Object o = a.getAttribute(HTML.Attribute.ID);
        if ((o != null) && o.equals("debug")) {
            return true;
        }
        return false;
    }
    */

    /**
     * Calculate the requirements for each column.  The calculation
     * is done as two passes over the table.  The table cells that
     * occupy a single column are scanned first to determine the
     * maximum of minimum, preferred, and maximum spans along the
     * give axis.  Table cells that span multiple columns are excluded
     * from the first pass.  A second pass is made to determine if
     * the cells that span multiple columns are satisfied.  If the
     * column requirements are not satisified, the needs of the
     * multi-column cell is mixed into the existing column requirements.
     * The calculation of the multi-column distribution is based upon
     * the proportions of the existing column requirements and taking
     * into consideration any constraining maximums.
     */
    void calculateColumnRequirements(int axis) {

        for (SizeRequirements req : columnRequirements) {
            req.minimum = 0;
            req.preferred = 0;
            req.maximum = Integer.MAX_VALUE;
        }

        // pass 1 - single column cells
        boolean hasMultiColumn = false;
        int nrows = getRowCount();
        for (int i = 0; i < nrows; i++) {
            TableRow row = getRow(i);
            int col = 0;
            int ncells = row.getViewCount();
            for (int cell = 0; cell < ncells; cell++, col++) {
                View cv = row.getView(cell);
                for (; row.isFilled(col); col++); // advance to a free column
                int rowSpan = getRowsOccupied(cv);
                int colSpan = getColumnsOccupied(cv);
                if (colSpan == 1) {
                    checkSingleColumnCell(axis, col, cv);
                } else {
                    hasMultiColumn = true;
                    col += colSpan - 1;
                }
            }
        }

        // pass 2 - multi-column cells
        if (hasMultiColumn) {
            for (int i = 0; i < nrows; i++) {
                TableRow row = getRow(i);
                int col = 0;
                int ncells = row.getViewCount();
                for (int cell = 0; cell < ncells; cell++, col++) {
                    View cv = row.getView(cell);
                    for (; row.isFilled(col); col++); // advance to a free column
                    int colSpan = getColumnsOccupied(cv);
                    if (colSpan > 1) {
                        checkMultiColumnCell(axis, col, colSpan, cv);
                        col += colSpan - 1;
                    }
                }
            }
        }

        /*
        if (shouldTrace()) {
            System.err.println("calc:");
            for (int i = 0; i < columnRequirements.length; i++) {
                System.err.println(" " + i + ": " + columnRequirements[i]);
            }
        }
        */
    }

    /**
     * check the requirements of a table cell that spans a single column.
     */
    void checkSingleColumnCell(int axis, int col, View v) {
        SizeRequirements req = columnRequirements[col];
        req.minimum = Math.max((int) v.getMinimumSpan(axis), req.minimum);
        req.preferred = Math.max((int) v.getPreferredSpan(axis), req.preferred);
        req.maximum = Math.max((int) v.getMaximumSpan(axis), req.maximum);
    }

    /**
     * check the requirements of a table cell that spans multiple
     * columns.
     */
    void checkMultiColumnCell(int axis, int col, int ncols, View v) {
        // calculate the totals
        long min = 0;
        long pref = 0;
        long max = 0;
        for (int i = 0; i < ncols; i++) {
            SizeRequirements req = columnRequirements[col + i];
            min += req.minimum;
            pref += req.preferred;
            max += req.maximum;
        }

        // check if the minimum size needs adjustment.
        int cmin = (int) v.getMinimumSpan(axis);
        if (cmin > min) {
            /*
             * the columns that this cell spans need adjustment to fit
             * this table cell.... calculate the adjustments.  The
             * maximum for each cell is the maximum of the existing
             * maximum or the amount needed by the cell.
             */
            SizeRequirements[] reqs = new SizeRequirements[ncols];
            for (int i = 0; i < ncols; i++) {
                SizeRequirements r = reqs[i] = columnRequirements[col + i];
                r.maximum = Math.max(r.maximum, (int) v.getMaximumSpan(axis));
            }
            int[] spans = new int[ncols];
            int[] offsets = new int[ncols];
            SizeRequirements.calculateTiledPositions(cmin, null, reqs,
                                                     offsets, spans);
            // apply the adjustments
            for (int i = 0; i < ncols; i++) {
                SizeRequirements req = reqs[i];
                req.minimum = Math.max(spans[i], req.minimum);
                req.preferred = Math.max(req.minimum, req.preferred);
                req.maximum = Math.max(req.preferred, req.maximum);
            }
        }

        // check if the preferred size needs adjustment.
        int cpref = (int) v.getPreferredSpan(axis);
        if (cpref > pref) {
            /*
             * the columns that this cell spans need adjustment to fit
             * this table cell.... calculate the adjustments.  The
             * maximum for each cell is the maximum of the existing
             * maximum or the amount needed by the cell.
             */
            SizeRequirements[] reqs = new SizeRequirements[ncols];
            for (int i = 0; i < ncols; i++) {
                SizeRequirements r = reqs[i] = columnRequirements[col + i];
            }
            int[] spans = new int[ncols];
            int[] offsets = new int[ncols];
            SizeRequirements.calculateTiledPositions(cpref, null, reqs,
                                                     offsets, spans);
            // apply the adjustments
            for (int i = 0; i < ncols; i++) {
                SizeRequirements req = reqs[i];
                req.preferred = Math.max(spans[i], req.preferred);
                req.maximum = Math.max(req.preferred, req.maximum);
            }
        }

    }

    /**
     * Fetches the child view that represents the given position in
     * the model.  This is implemented to walk through the children
     * looking for a range that contains the given position.  In this
     * view the children do not necessarily have a one to one mapping
     * with the child elements.
     *
     * @param pos  the search position &gt;= 0
     * @param a  the allocation to the table on entry, and the
     *   allocation of the view containing the position on exit
     * @return  the view representing the given position, or
     *   <code>null</code> if there isn't one
     */
    protected View getViewAtPosition(int pos, Rectangle a) {
        int n = getViewCount();
        for (int i = 0; i < n; i++) {
            View v = getView(i);
            int p0 = v.getStartOffset();
            int p1 = v.getEndOffset();
            if ((pos >= p0) && (pos < p1)) {
                // it's in this view.
                if (a != null) {
                    childAllocation(i, a);
                }
                return v;
            }
        }
        if (pos == getEndOffset()) {
            View v = getView(n - 1);
            if (a != null) {
                this.childAllocation(n - 1, a);
            }
            return v;
        }
        return null;
    }

    // ---- variables ----------------------------------------------------

    int[] columnSpans;
    int[] columnOffsets;

    SizeRequirements totalColumnRequirements;

    SizeRequirements[] columnRequirements;
    Vector<TableRow> rows;
    boolean gridValid;
    private static final BitSet EMPTY = new BitSet();

    /**
     * View of a row in a row-centric table.
     */
    public class TableRow extends BoxView {

        /**
         * Constructs a TableView for the given element.
         *
         * @param elem the element that this view is responsible for
         * @since 1.4
         */
        public TableRow(Element elem) {
            super(elem, View.X_AXIS);
            fillColumns = new BitSet();
        }

        void clearFilledColumns() {
            fillColumns.and(EMPTY);
        }

        void fillColumn(int col) {
            fillColumns.set(col);
        }

        boolean isFilled(int col) {
            return fillColumns.get(col);
        }

        /** get location in the overall set of rows */
        int getRow() {
            return row;
        }

        /**
         * set location in the overall set of rows, this is
         * set by the TableView.updateGrid() method.
         */
        void setRow(int row) {
            this.row = row;
        }

        /**
         * The number of columns present in this row.
         */
        int getColumnCount() {
            int nfill = 0;
            int n = fillColumns.size();
            for (int i = 0; i < n; i++) {
                if (fillColumns.get(i)) {
                    nfill ++;
                }
            }
            return getViewCount() + nfill;
        }

        /**
         * Change the child views.  This is implemented to
         * provide the superclass behavior and invalidate the
         * grid so that rows and columns will be recalculated.
         */
        public void replace(int offset, int length, View[] views) {
            super.replace(offset, length, views);
            invalidateGrid();
        }

        @Override
        protected SizeRequirements calculateMajorAxisRequirements(int axis, SizeRequirements r) {
            SizeRequirements req = new SizeRequirements();
            req.minimum = totalColumnRequirements.minimum;
            req.maximum = totalColumnRequirements.maximum;
            req.preferred = totalColumnRequirements.preferred;
            req.alignment = 0f;
            return req;
        }

        @Override
        public float getMinimumSpan(int axis) {
            float value;

            if (axis == View.X_AXIS) {
                value = totalColumnRequirements.minimum + getLeftInset() + getRightInset();
            } else {
                value = super.getMinimumSpan(axis);
            }
            return value;
        }

        @Override
        public float getMaximumSpan(int axis) {
            float value;

            if (axis == View.X_AXIS) {
                // We're flexible.
                value = (float) Integer.MAX_VALUE;
            } else {
                value = super.getMaximumSpan(axis);
            }
            return value;
        }

        @Override
        public float getPreferredSpan(int axis) {
            float value;

            if (axis == View.X_AXIS) {
                value = totalColumnRequirements.preferred + getLeftInset() + getRightInset();
            } else {
                value = super.getPreferredSpan(axis);
            }
            return value;
        }

        /**
         * Perform layout for the major axis of the box (i.e. the
         * axis that it represents).  The results of the layout should
         * be placed in the given arrays which represent the allocations
         * to the children along the major axis.
         * <p>
         * This is re-implemented to give each child the span of the column
         * width for the table, and to give cells that span multiple columns
         * the multi-column span.
         *
         * @param targetSpan the total span given to the view, which
         *  would be used to layout the children.
         * @param axis the axis being layed out.
         * @param offsets the offsets from the origin of the view for
         *  each of the child views.  This is a return value and is
         *  filled in by the implementation of this method.
         * @param spans the span of each child view.  This is a return
         *  value and is filled in by the implementation of this method.
         */
        protected void layoutMajorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
            int col = 0;
            int ncells = getViewCount();
            for (int cell = 0; cell < ncells; cell++, col++) {
                View cv = getView(cell);
                for (; isFilled(col); col++); // advance to a free column
                int colSpan = getColumnsOccupied(cv);
                spans[cell] = columnSpans[col];
                offsets[cell] = columnOffsets[col];
                if (colSpan > 1) {
                    int n = columnSpans.length;
                    for (int j = 1; j < colSpan; j++) {
                        // Because the table may be only partially formed, some
                        // of the columns may not yet exist.  Therefore we check
                        // the bounds.
                        if ((col+j) < n) {
                            spans[cell] += columnSpans[col+j];
                        }
                    }
                    col += colSpan - 1;
                }
            }
        }

        /**
         * Perform layout for the minor axis of the box (i.e. the
         * axis orthogonal to the axis that it represents).  The results
         * of the layout should be placed in the given arrays which represent
         * the allocations to the children along the minor axis.  This
         * is called by the superclass whenever the layout needs to be
         * updated along the minor axis.
         * <p>
         * This is implemented to delegate to the superclass, then adjust
         * the span for any cell that spans multiple rows.
         *
         * @param targetSpan the total span given to the view, which
         *  would be used to layout the children.
         * @param axis the axis being layed out.
         * @param offsets the offsets from the origin of the view for
         *  each of the child views.  This is a return value and is
         *  filled in by the implementation of this method.
         * @param spans the span of each child view.  This is a return
         *  value and is filled in by the implementation of this method.
         */
        protected void layoutMinorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
            super.layoutMinorAxis(targetSpan, axis, offsets, spans);
            int col = 0;
            int ncells = getViewCount();
            for (int cell = 0; cell < ncells; cell++, col++) {
                View cv = getView(cell);
                for (; isFilled(col); col++); // advance to a free column
                int colSpan = getColumnsOccupied(cv);
                int rowSpan = getRowsOccupied(cv);
                if (rowSpan > 1) {
                    for (int j = 1; j < rowSpan; j++) {
                        // test bounds of each row because it may not exist
                        // either because of error or because the table isn't
                        // fully loaded yet.
                        int row = getRow() + j;
                        if (row < TableView.this.getViewCount()) {
                            int span = TableView.this.getSpan(Y_AXIS, getRow()+j);
                            spans[cell] += span;
                        }
                    }
                }
                if (colSpan > 1) {
                    col += colSpan - 1;
                }
            }
        }

        /**
         * Determines the resizability of the view along the
         * given axis.  A value of 0 or less is not resizable.
         *
         * @param axis may be either View.X_AXIS or View.Y_AXIS
         * @return the resize weight
         * @exception IllegalArgumentException for an invalid axis
         */
        public int getResizeWeight(int axis) {
            return 1;
        }

        /**
         * Fetches the child view that represents the given position in
         * the model.  This is implemented to walk through the children
         * looking for a range that contains the given position.  In this
         * view the children do not necessarily have a one to one mapping
         * with the child elements.
         *
         * @param pos  the search position &gt;= 0
         * @param a  the allocation to the table on entry, and the
         *   allocation of the view containing the position on exit
         * @return  the view representing the given position, or
         *   <code>null</code> if there isn't one
         */
        protected View getViewAtPosition(int pos, Rectangle a) {
            int n = getViewCount();
            for (int i = 0; i < n; i++) {
                View v = getView(i);
                int p0 = v.getStartOffset();
                int p1 = v.getEndOffset();
                if ((pos >= p0) && (pos < p1)) {
                    // it's in this view.
                    if (a != null) {
                        childAllocation(i, a);
                    }
                    return v;
                }
            }
            if (pos == getEndOffset()) {
                View v = getView(n - 1);
                if (a != null) {
                    this.childAllocation(n - 1, a);
                }
                return v;
            }
            return null;
        }

        /** columns filled by multi-column or multi-row cells */
        BitSet fillColumns;
        /** the row within the overall grid */
        int row;
    }

    /**
     * @deprecated  A table cell can now be any View implementation.
     */
    @Deprecated
    public class TableCell extends BoxView implements GridCell {

        /**
         * Constructs a TableCell for the given element.
         *
         * @param elem the element that this view is responsible for
         * @since 1.4
         */
        public TableCell(Element elem) {
            super(elem, View.Y_AXIS);
        }

        // --- GridCell methods -------------------------------------

        /**
         * Gets the number of columns this cell spans (e.g. the
         * grid width).
         *
         * @return the number of columns
         */
        public int getColumnCount() {
            return 1;
        }

        /**
         * Gets the number of rows this cell spans (that is, the
         * grid height).
         *
         * @return the number of rows
         */
        public int getRowCount() {
            return 1;
        }


        /**
         * Sets the grid location.
         *
         * @param row the row &gt;= 0
         * @param col the column &gt;= 0
         */
        public void setGridLocation(int row, int col) {
            this.row = row;
            this.col = col;
        }

        /**
         * Gets the row of the grid location
         */
        public int getGridRow() {
            return row;
        }

        /**
         * Gets the column of the grid location
         */
        public int getGridColumn() {
            return col;
        }

        int row;
        int col;
    }

    /**
     * <em>
     * THIS IS NO LONGER USED, AND WILL BE REMOVED IN THE
     * NEXT RELEASE.  THE JCK SIGNATURE TEST THINKS THIS INTERFACE
     * SHOULD EXIST
     * </em>
     */
    interface GridCell {

        /**
         * Sets the grid location.
         *
         * @param row the row &gt;= 0
         * @param col the column &gt;= 0
         */
        public void setGridLocation(int row, int col);

        /**
         * Gets the row of the grid location
         */
        public int getGridRow();

        /**
         * Gets the column of the grid location
         */
        public int getGridColumn();

        /**
         * Gets the number of columns this cell spans (e.g. the
         * grid width).
         *
         * @return the number of columns
         */
        public int getColumnCount();

        /**
         * Gets the number of rows this cell spans (that is, the
         * grid height).
         *
         * @return the number of rows
         */
        public int getRowCount();

    }

}
