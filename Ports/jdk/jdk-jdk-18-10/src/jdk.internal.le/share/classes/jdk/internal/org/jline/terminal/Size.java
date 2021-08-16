/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.terminal;

public class Size {

    private int rows;
    private int cols;

    public Size() {
    }

    public Size(int columns, int rows) {
        this();
        setColumns(columns);
        setRows(rows);
    }

    public int getColumns() {
        return cols;
    }

    public void setColumns(int columns) {
        cols = (short) columns;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = (short) rows;
    }

    /**
     * A cursor position combines a row number with a column position.
     * <p>
     * Note each row has {@code col+1} different column positions,
     * including the right margin.
     * </p>
     *
     * @param col the new column
     * @param row the new row
     * @return the cursor position
     */
    public int cursorPos(int row, int col) {
        return row * (cols+1) + col;
    }

    public void copy(Size size) {
        setColumns(size.getColumns());
        setRows(size.getRows());
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Size) {
            Size size = (Size) o;
            return rows == size.rows && cols == size.cols;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return rows * 31 + cols;
    }

    @Override
    public String toString() {
        return "Size[" + "cols=" + cols + ", rows=" + rows + ']';
    }
}
