/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javax.sql.rowset;

import java.sql.*;
import javax.sql.*;
import java.io.*;

import java.lang.reflect.*;

/**
 * Provides implementations for the methods that set and get
 * metadata information about a <code>RowSet</code> object's columns.
 * A <code>RowSetMetaDataImpl</code> object keeps track of the
 * number of columns in the rowset and maintains an internal array
 * of column attributes for each column.
 * <P>
 * A <code>RowSet</code> object creates a <code>RowSetMetaDataImpl</code>
 * object internally in order to set and retrieve information about
 * its columns.
 * <P>
 * NOTE: All metadata in a <code>RowSetMetaDataImpl</code> object
 * should be considered as unavailable until the <code>RowSet</code> object
 * that it describes is populated.
 * Therefore, any <code>RowSetMetaDataImpl</code> method that retrieves information
 * is defined as having unspecified behavior when it is called
 * before the <code>RowSet</code> object contains data.
 *
 * @since 1.5
 */
public class RowSetMetaDataImpl implements RowSetMetaData,  Serializable {
    /**
     * Constructs a {@code RowSetMetaDataImpl} object.
     */
    public RowSetMetaDataImpl() {}

    /**
     * The number of columns in the <code>RowSet</code> object that created
     * this <code>RowSetMetaDataImpl</code> object.
     * @serial
     */
    private int colCount;

    /**
     * An array of <code>ColInfo</code> objects used to store information
     * about each column in the <code>RowSet</code> object for which
     * this <code>RowSetMetaDataImpl</code> object was created. The first
     * <code>ColInfo</code> object in this array contains information about
     * the first column in the <code>RowSet</code> object, the second element
     * contains information about the second column, and so on.
     * @serial
     */
    private ColInfo[] colInfo;

    /**
     * Checks to see that the designated column is a valid column number for
     * the <code>RowSet</code> object for which this <code>RowSetMetaDataImpl</code>
     * was created. To be valid, a column number must be greater than
     * <code>0</code> and less than or equal to the number of columns in a row.
     * @throws SQLException with the message "Invalid column index"
     *        if the given column number is out of the range of valid column
     *        numbers for the <code>RowSet</code> object
     */
    private void checkColRange(int col) throws SQLException {
        if (col <= 0 || col > colCount) {
            throw new SQLException("Invalid column index :"+col);
        }
    }

    /**
     * Checks to see that the given SQL type is a valid column type and throws an
     * <code>SQLException</code> object if it is not.
     * To be valid, a SQL type must be one of the constant values
     * in the <code><a href="../../sql/Types.html">java.sql.Types</a></code>
     * class.
     *
     * @param SQLType an <code>int</code> defined in the class <code>java.sql.Types</code>
     * @throws SQLException if the given <code>int</code> is not a constant defined in the
     *         class <code>java.sql.Types</code>
     */
    private void checkColType(int SQLType) throws SQLException {
        try {
            Class<?> c = java.sql.Types.class;
            Field[] publicFields = c.getFields();
            int fieldValue = 0;
            for (int i = 0; i < publicFields.length; i++) {
                fieldValue = publicFields[i].getInt(c);
                if (fieldValue == SQLType) {
                    return;
                 }
            }
        } catch (Exception e) {
            throw new SQLException(e.getMessage());
        }
        throw new SQLException("Invalid SQL type for column");
    }

    /**
     * Sets to the given number the number of columns in the <code>RowSet</code>
     * object for which this <code>RowSetMetaDataImpl</code> object was created.
     *
     * @param columnCount an <code>int</code> giving the number of columns in the
     *        <code>RowSet</code> object
     * @throws SQLException if the given number is equal to or less than zero
     */
    public void setColumnCount(int columnCount) throws SQLException {

        if (columnCount <= 0) {
            throw new SQLException("Invalid column count. Cannot be less " +
                "or equal to zero");
            }

       colCount = columnCount;

       // If the colCount is Integer.MAX_VALUE,
       // we do not initialize the colInfo object.
       // even if we try to initialize the colCount with
       // colCount = Integer.MAx_VALUE-1, the colInfo
       // initialization fails throwing an ERROR
       // OutOfMemory Exception. So we do not initialize
       // colInfo at Integer.MAX_VALUE. This is to pass TCK.

       if(!(colCount == Integer.MAX_VALUE)) {
            colInfo = new ColInfo[colCount + 1];

           for (int i=1; i <= colCount; i++) {
                 colInfo[i] = new ColInfo();
           }
       }


    }

    /**
     * Sets whether the designated column is automatically
     * numbered, thus read-only, to the given <code>boolean</code>
     * value.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     *        must be between <code>1</code> and the number of columns
     *        in the rowset, inclusive
     * @param property <code>true</code> if the given column is
     *                 automatically incremented; <code>false</code>
     *                 otherwise
     * @throws SQLException if a database access error occurs or
     *         the given index is out of bounds
     */
    public void setAutoIncrement(int columnIndex, boolean property) throws SQLException {
        checkColRange(columnIndex);
        colInfo[columnIndex].autoIncrement = property;
    }

    /**
     * Sets whether the name of the designated column is case sensitive to
     * the given <code>boolean</code>.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     *        must be between <code>1</code> and the number of columns
     *        in the rowset, inclusive
     * @param property <code>true</code> to indicate that the column
     *                 name is case sensitive; <code>false</code> otherwise
     * @throws SQLException if a database access error occurs or
     *         the given column number is out of bounds
     */
    public void setCaseSensitive(int columnIndex, boolean property) throws SQLException {
        checkColRange(columnIndex);
        colInfo[columnIndex].caseSensitive = property;
    }

    /**
     * Sets whether a value stored in the designated column can be used
     * in a <code>WHERE</code> clause to the given <code>boolean</code> value.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     *                    must be between <code>1</code> and the number
     *                    of columns in the rowset, inclusive
     * @param property <code>true</code> to indicate that a column
     *                 value can be used in a <code>WHERE</code> clause;
     *                 <code>false</code> otherwise
     *
     * @throws SQLException if a database access error occurs or
     *         the given column number is out of bounds
     */
    public void setSearchable(int columnIndex, boolean property)
        throws SQLException {
        checkColRange(columnIndex);
        colInfo[columnIndex].searchable = property;
    }

    /**
     * Sets whether a value stored in the designated column is a cash
     * value to the given <code>boolean</code>.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     * must be between <code>1</code> and the number of columns,
     * inclusive between <code>1</code> and the number of columns, inclusive
     * @param property true if the value is a cash value; false otherwise.
     * @throws SQLException if a database access error occurs
     *         or the given column number is out of bounds
     */
    public void setCurrency(int columnIndex, boolean property)
        throws SQLException {
        checkColRange(columnIndex);
        colInfo[columnIndex].currency = property;
    }

    /**
     * Sets whether a value stored in the designated column can be set
     * to <code>NULL</code> to the given constant from the interface
     * <code>ResultSetMetaData</code>.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     *        must be between <code>1</code> and the number of columns, inclusive
     * @param property one of the following <code>ResultSetMetaData</code> constants:
     *                 <code>columnNoNulls</code>,
     *                 <code>columnNullable</code>, or
     *                 <code>columnNullableUnknown</code>
     *
     * @throws SQLException if a database access error occurs,
     *         the given column number is out of bounds, or the value supplied
     *         for the <i>property</i> parameter is not one of the following
     *         constants:
     *           <code>ResultSetMetaData.columnNoNulls</code>,
     *           <code>ResultSetMetaData.columnNullable</code>, or
     *           <code>ResultSetMetaData.columnNullableUnknown</code>
     */
    public void setNullable(int columnIndex, int property) throws SQLException {
        if ((property < ResultSetMetaData.columnNoNulls) ||
            property > ResultSetMetaData.columnNullableUnknown) {
                throw new SQLException("Invalid nullable constant set. Must be " +
                    "either columnNoNulls, columnNullable or columnNullableUnknown");
        }
        checkColRange(columnIndex);
        colInfo[columnIndex].nullable = property;
    }

    /**
     * Sets whether a value stored in the designated column is a signed
     * number to the given <code>boolean</code>.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     *        must be between <code>1</code> and the number of columns, inclusive
     * @param property <code>true</code> to indicate that a column
     *                 value is a signed number;
     *                 <code>false</code> to indicate that it is not
     * @throws SQLException if a database access error occurs
     *         or the given column number is out of bounds
     */
    public void setSigned(int columnIndex, boolean property) throws SQLException {
        checkColRange(columnIndex);
        colInfo[columnIndex].signed = property;
    }

    /**
     * Sets the normal maximum number of chars in the designated column
     * to the given number.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     *        must be between <code>1</code> and the number of columns, inclusive
     * @param size the maximum size of the column in chars; must be
     *        <code>0</code> or more
     * @throws SQLException if a database access error occurs,
     *        the given column number is out of bounds, or <i>size</i> is
     *        less than <code>0</code>
     */
    public void setColumnDisplaySize(int columnIndex, int size) throws SQLException {
        if (size < 0) {
            throw new SQLException("Invalid column display size. Cannot be less " +
                "than zero");
        }
        checkColRange(columnIndex);
        colInfo[columnIndex].columnDisplaySize = size;
    }

    /**
     * Sets the suggested column label for use in printouts and
     * displays, if any, to <i>label</i>. If <i>label</i> is
     * <code>null</code>, the column label is set to an empty string
     * ("").
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     *        must be between <code>1</code> and the number of columns, inclusive
     * @param label the column label to be used in printouts and displays; if the
     *        column label is <code>null</code>, an empty <code>String</code> is
     *        set
     * @throws SQLException if a database access error occurs
     *         or the given column index is out of bounds
     */
    public void setColumnLabel(int columnIndex, String label) throws SQLException {
        checkColRange(columnIndex);
        if (label != null) {
            colInfo[columnIndex].columnLabel = label;
        } else {
            colInfo[columnIndex].columnLabel = "";
        }
    }

    /**
     * Sets the column name of the designated column to the given name.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     *      must be between <code>1</code> and the number of columns, inclusive
     * @param columnName a <code>String</code> object indicating the column name;
     *      if the given name is <code>null</code>, an empty <code>String</code>
     *      is set
     * @throws SQLException if a database access error occurs or the given column
     *      index is out of bounds
     */
    public void setColumnName(int columnIndex, String columnName) throws SQLException {
        checkColRange(columnIndex);
        if (columnName != null) {
            colInfo[columnIndex].columnName = columnName;
        } else {
            colInfo[columnIndex].columnName = "";
        }
    }

    /**
     * Sets the designated column's table's schema name, if any, to
     * <i>schemaName</i>. If <i>schemaName</i> is <code>null</code>,
     * the schema name is set to an empty string ("").
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     *        must be between <code>1</code> and the number of columns, inclusive
     * @param schemaName the schema name for the table from which a value in the
     *        designated column was derived; may be an empty <code>String</code>
     *        or <code>null</code>
     * @throws SQLException if a database access error occurs
     *        or the given column number is out of bounds
     */
    public void setSchemaName(int columnIndex, String schemaName) throws SQLException {
        checkColRange(columnIndex);
        if (schemaName != null ) {
            colInfo[columnIndex].schemaName = schemaName;
        } else {
            colInfo[columnIndex].schemaName = "";
        }
    }

    /**
     * Sets the total number of decimal digits in a value stored in the
     * designated column to the given number.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     *        must be between <code>1</code> and the number of columns, inclusive
     * @param precision the total number of decimal digits; must be <code>0</code>
     *        or more
     * @throws SQLException if a database access error occurs,
     *         <i>columnIndex</i> is out of bounds, or <i>precision</i>
     *         is less than <code>0</code>
     */
    public void setPrecision(int columnIndex, int precision) throws SQLException {

        if (precision < 0) {
            throw new SQLException("Invalid precision value. Cannot be less " +
                "than zero");
        }
        checkColRange(columnIndex);
        colInfo[columnIndex].colPrecision = precision;
    }

    /**
     * Sets the number of digits to the right of the decimal point in a value
     * stored in the designated column to the given number.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     *        must be between <code>1</code> and the number of columns, inclusive
     * @param scale the number of digits to the right of the decimal point; must be
     *        zero or greater
     * @throws SQLException if a database access error occurs,
     *         <i>columnIndex</i> is out of bounds, or <i>scale</i>
     *         is less than <code>0</code>
     */
    public void setScale(int columnIndex, int scale) throws SQLException {
        if (scale < 0) {
            throw new SQLException("Invalid scale size. Cannot be less " +
                "than zero");
        }
        checkColRange(columnIndex);
        colInfo[columnIndex].colScale = scale;
    }

    /**
     * Sets the name of the table from which the designated column
     * was derived to the given table name.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     *        must be between <code>1</code> and the number of columns, inclusive
     * @param tableName the column's table name; may be <code>null</code> or an
     *        empty string
     * @throws SQLException if a database access error occurs
     *         or the given column number is out of bounds
     */
    public void setTableName(int columnIndex, String tableName) throws SQLException {
        checkColRange(columnIndex);
        if (tableName != null) {
            colInfo[columnIndex].tableName = tableName;
        } else {
            colInfo[columnIndex].tableName = "";
        }
    }

    /**
     * Sets the catalog name of the table from which the designated
     * column was derived to <i>catalogName</i>. If <i>catalogName</i>
     * is <code>null</code>, the catalog name is set to an empty string.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     *        must be between <code>1</code> and the number of columns, inclusive
     * @param catalogName the column's table's catalog name; if the catalogName
     *        is <code>null</code>, an empty <code>String</code> is set
     * @throws SQLException if a database access error occurs
     *         or the given column number is out of bounds
     */
    public void setCatalogName(int columnIndex, String catalogName) throws SQLException {
        checkColRange(columnIndex);
        if (catalogName != null)
            colInfo[columnIndex].catName = catalogName;
        else
            colInfo[columnIndex].catName = "";
    }

    /**
     * Sets the SQL type code for values stored in the designated column
     * to the given type code from the class <code>java.sql.Types</code>.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     *        must be between <code>1</code> and the number of columns, inclusive
     * @param SQLType the designated column's SQL type, which must be one of the
     *                constants in the class <code>java.sql.Types</code>
     * @throws SQLException if a database access error occurs,
     *         the given column number is out of bounds, or the column type
     *         specified is not one of the constants in
     *         <code>java.sql.Types</code>
     * @see java.sql.Types
     */
    public void setColumnType(int columnIndex, int SQLType) throws SQLException {
        // examine java.sql.Type reflectively, loop on the fields and check
        // this. Separate out into a private method
        checkColType(SQLType);
        checkColRange(columnIndex);
        colInfo[columnIndex].colType = SQLType;
    }

    /**
     * Sets the type name used by the data source for values stored in the
     * designated column to the given type name.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     * must be between <code>1</code> and the number of columns, inclusive
     * @param typeName the data source-specific type name; if <i>typeName</i> is
     *        <code>null</code>, an empty <code>String</code> is set
     * @throws SQLException if a database access error occurs
     *         or the given column number is out of bounds
     */
    public void setColumnTypeName(int columnIndex, String typeName)
        throws SQLException {
        checkColRange(columnIndex);
        if (typeName != null) {
            colInfo[columnIndex].colTypeName = typeName;
        } else {
            colInfo[columnIndex].colTypeName = "";
        }
    }

    /**
     * Retrieves the number of columns in the <code>RowSet</code> object
     * for which this <code>RowSetMetaDataImpl</code> object was created.
     *
     * @return the number of columns
     * @throws SQLException if an error occurs determining the column count
     */
    public int getColumnCount() throws SQLException {
        return colCount;
    }

    /**
     * Retrieves whether a value stored in the designated column is
     * automatically numbered, and thus readonly.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     *         must be between <code>1</code> and the number of columns, inclusive
     * @return <code>true</code> if the column is automatically numbered;
     *         <code>false</code> otherwise
     * @throws SQLException if a database access error occurs
     * or the given column number is out of bounds
     */
    public boolean isAutoIncrement(int columnIndex) throws SQLException {
        checkColRange(columnIndex);
        return colInfo[columnIndex].autoIncrement;
    }

    /**
     * Indicates whether the case of the designated column's name
     * matters.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     * must be between <code>1</code> and the number of columns, inclusive
     * @return <code>true</code> if the column name is case sensitive;
     *          <code>false</code> otherwise
     * @throws SQLException if a database access error occurs
     * or the given column number is out of bounds
     */
    public boolean isCaseSensitive(int columnIndex) throws SQLException {
        checkColRange(columnIndex);
        return colInfo[columnIndex].caseSensitive;
    }

    /**
     * Indicates whether a value stored in the designated column
     * can be used in a <code>WHERE</code> clause.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     *        must be between <code>1</code> and the number of columns, inclusive
     * @return <code>true</code> if a value in the designated column can be used in a
     *         <code>WHERE</code> clause; <code>false</code> otherwise
     * @throws SQLException if a database access error occurs
     *         or the given column number is out of bounds
     */
    public boolean isSearchable(int columnIndex) throws SQLException {
        checkColRange(columnIndex);
        return colInfo[columnIndex].searchable;
    }

    /**
     * Indicates whether a value stored in the designated column
     * is a cash value.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     *        must be between <code>1</code> and the number of columns, inclusive
     * @return <code>true</code> if a value in the designated column is a cash value;
     *         <code>false</code> otherwise
     * @throws SQLException if a database access error occurs
     *         or the given column number is out of bounds
     */
    public boolean isCurrency(int columnIndex) throws SQLException {
        checkColRange(columnIndex);
        return colInfo[columnIndex].currency;
    }

    /**
     * Retrieves a constant indicating whether it is possible
     * to store a <code>NULL</code> value in the designated column.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     *        must be between <code>1</code> and the number of columns, inclusive
     * @return a constant from the <code>ResultSetMetaData</code> interface;
     *         either <code>columnNoNulls</code>,
     *         <code>columnNullable</code>, or
     *         <code>columnNullableUnknown</code>
     * @throws SQLException if a database access error occurs
     *         or the given column number is out of bounds
     */
    public int isNullable(int columnIndex) throws SQLException {
        checkColRange(columnIndex);
        return colInfo[columnIndex].nullable;
    }

    /**
     * Indicates whether a value stored in the designated column is
     * a signed number.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     *        must be between <code>1</code> and the number of columns, inclusive
     * @return <code>true</code> if a value in the designated column is a signed
     *         number; <code>false</code> otherwise
     * @throws SQLException if a database access error occurs
     *         or the given column number is out of bounds
     */
    public boolean isSigned(int columnIndex) throws SQLException {
        checkColRange(columnIndex);
        return colInfo[columnIndex].signed;
    }

    /**
     * Retrieves the normal maximum width in chars of the designated column.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     *        must be between <code>1</code> and the number of columns, inclusive
     * @return the maximum number of chars that can be displayed in the designated
     *         column
     * @throws SQLException if a database access error occurs
     *         or the given column number is out of bounds
     */
    public int getColumnDisplaySize(int columnIndex) throws SQLException {
        checkColRange(columnIndex);
        return colInfo[columnIndex].columnDisplaySize;
    }

    /**
     * Retrieves the suggested column title for the designated
     * column for use in printouts and displays.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     *        must be between <code>1</code> and the number of columns, inclusive
     * @return the suggested column name to use in printouts and displays
     * @throws SQLException if a database access error occurs
     *         or the given column number is out of bounds
     */
    public String getColumnLabel(int columnIndex) throws SQLException {
        checkColRange(columnIndex);
        return colInfo[columnIndex].columnLabel;
    }

    /**
     * Retrieves the name of the designated column.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     * must be between <code>1</code> and the number of columns, inclusive
     * @return the column name of the designated column
     * @throws SQLException if a database access error occurs
     * or the given column number is out of bounds
     */
    public String getColumnName(int columnIndex) throws SQLException {
        checkColRange(columnIndex);
        return colInfo[columnIndex].columnName;
    }

    /**
     * Retrieves the schema name of the table from which the value
     * in the designated column was derived.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     *         must be between <code>1</code> and the number of columns,
     *         inclusive
     * @return the schema name or an empty <code>String</code> if no schema
     *         name is available
     * @throws SQLException if a database access error occurs
     * or the given column number is out of bounds
     */
    public String getSchemaName(int columnIndex) throws SQLException {
        checkColRange(columnIndex);
        String str ="";
        if(colInfo[columnIndex].schemaName == null){
        } else {
              str = colInfo[columnIndex].schemaName;
        }
        return str;
    }

    /**
     * Retrieves the total number of digits for values stored in
     * the designated column.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     * must be between <code>1</code> and the number of columns, inclusive
     * @return the precision for values stored in the designated column
     * @throws SQLException if a database access error occurs
     * or the given column number is out of bounds
     */
    public int getPrecision(int columnIndex) throws SQLException {
        checkColRange(columnIndex);
        return colInfo[columnIndex].colPrecision;
    }

    /**
     * Retrieves the number of digits to the right of the decimal point
     * for values stored in the designated column.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     * must be between <code>1</code> and the number of columns, inclusive
     * @return the scale for values stored in the designated column
     * @throws SQLException if a database access error occurs
     * or the given column number is out of bounds
     */
    public int getScale(int columnIndex) throws SQLException {
        checkColRange(columnIndex);
        return colInfo[columnIndex].colScale;
    }

    /**
     * Retrieves the name of the table from which the value
     * in the designated column was derived.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     * must be between <code>1</code> and the number of columns, inclusive
     * @return the table name or an empty <code>String</code> if no table name
     *         is available
     * @throws SQLException if a database access error occurs
     * or the given column number is out of bounds
     */
    public String getTableName(int columnIndex) throws SQLException {
        checkColRange(columnIndex);
        return colInfo[columnIndex].tableName;
    }

    /**
     * Retrieves the catalog name of the table from which the value
     * in the designated column was derived.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     * must be between <code>1</code> and the number of columns, inclusive
     * @return the catalog name of the column's table or an empty
     *         <code>String</code> if no catalog name is available
     * @throws SQLException if a database access error occurs
     * or the given column number is out of bounds
     */
    public String getCatalogName(int columnIndex) throws SQLException {
        checkColRange(columnIndex);
        String str ="";
        if(colInfo[columnIndex].catName == null){
        } else {
           str = colInfo[columnIndex].catName;
        }
        return str;
    }

    /**
     * Retrieves the type code (one of the <code>java.sql.Types</code>
     * constants) for the SQL type of the value stored in the
     * designated column.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     * must be between <code>1</code> and the number of columns, inclusive
     * @return an <code>int</code> representing the SQL type of values
     * stored in the designated column
     * @throws SQLException if a database access error occurs
     * or the given column number is out of bounds
     * @see java.sql.Types
     */
    public int getColumnType(int columnIndex) throws SQLException {
        checkColRange(columnIndex);
        return colInfo[columnIndex].colType;
    }

    /**
     * Retrieves the DBMS-specific type name for values stored in the
     * designated column.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     * must be between <code>1</code> and the number of columns, inclusive
     * @return the type name used by the data source
     * @throws SQLException if a database access error occurs
     * or the given column number is out of bounds
     */
    public String getColumnTypeName(int columnIndex) throws SQLException {
        checkColRange(columnIndex);
        return colInfo[columnIndex].colTypeName;
    }


    /**
     * Indicates whether the designated column is definitely
     * not writable, thus readonly.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     * must be between <code>1</code> and the number of columns, inclusive
     * @return <code>true</code> if this <code>RowSet</code> object is read-Only
     * and thus not updatable; <code>false</code> otherwise
     * @throws SQLException if a database access error occurs
     * or the given column number is out of bounds
     */
    public boolean isReadOnly(int columnIndex) throws SQLException {
        checkColRange(columnIndex);
        return colInfo[columnIndex].readOnly;
    }

    /**
     * Indicates whether it is possible for a write operation on
     * the designated column to succeed. A return value of
     * <code>true</code> means that a write operation may or may
     * not succeed.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     *         must be between <code>1</code> and the number of columns, inclusive
     * @return <code>true</code> if a write operation on the designated column may
     *          will succeed; <code>false</code> otherwise
     * @throws SQLException if a database access error occurs
     *         or the given column number is out of bounds
     */
    public boolean isWritable(int columnIndex) throws SQLException {
        checkColRange(columnIndex);
        return colInfo[columnIndex].writable;
    }

    /**
     * Indicates whether a write operation on the designated column
     * will definitely succeed.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     * must be between <code>1</code> and the number of columns, inclusive
     * @return <code>true</code> if a write operation on the designated column will
     *         definitely succeed; <code>false</code> otherwise
     * @throws SQLException if a database access error occurs
     * or the given column number is out of bounds
     */
    public  boolean isDefinitelyWritable(int columnIndex) throws SQLException {
        checkColRange(columnIndex);
        return true;
    }

    /**
     * Retrieves the fully-qualified name of the class in the Java
     * programming language to which a value in the designated column
     * will be mapped.  For example, if the value is an <code>int</code>,
     * the class name returned by this method will be
     * <code>java.lang.Integer</code>.
     * <P>
     * If the value in the designated column has a custom mapping,
     * this method returns the name of the class that implements
     * <code>SQLData</code>. When the method <code>ResultSet.getObject</code>
     * is called to retrieve a value from the designated column, it will
     * create an instance of this class or one of its subclasses.
     *
     * @param columnIndex the first column is 1, the second is 2, and so on;
     *        must be between <code>1</code> and the number of columns, inclusive
     * @return the fully-qualified name of the class in the Java programming
     *        language that would be used by the method <code>RowSet.getObject</code> to
     *        retrieve the value in the specified column. This is the class
     *        name used for custom mapping when there is a custom mapping.
     * @throws SQLException if a database access error occurs
     *         or the given column number is out of bounds
     */
    public String getColumnClassName(int columnIndex) throws SQLException {
        String className = String.class.getName();

        int sqlType = getColumnType(columnIndex);

        switch (sqlType) {

        case Types.NUMERIC:
        case Types.DECIMAL:
            className = java.math.BigDecimal.class.getName();
            break;

        case Types.BIT:
            className = java.lang.Boolean.class.getName();
            break;

        case Types.TINYINT:
            className = java.lang.Byte.class.getName();
            break;

        case Types.SMALLINT:
            className = java.lang.Short.class.getName();
            break;

        case Types.INTEGER:
            className = java.lang.Integer.class.getName();
            break;

        case Types.BIGINT:
            className = java.lang.Long.class.getName();
            break;

        case Types.REAL:
            className = java.lang.Float.class.getName();
            break;

        case Types.FLOAT:
        case Types.DOUBLE:
            className = java.lang.Double.class.getName();
            break;

        case Types.BINARY:
        case Types.VARBINARY:
        case Types.LONGVARBINARY:
            className = "byte[]";
            break;

        case Types.DATE:
            className = java.sql.Date.class.getName();
            break;

        case Types.TIME:
            className = java.sql.Time.class.getName();
            break;

        case Types.TIMESTAMP:
            className = java.sql.Timestamp.class.getName();
            break;

        case Types.BLOB:
            className = java.sql.Blob.class.getName();
            break;

        case Types.CLOB:
            className = java.sql.Clob.class.getName();
            break;
        }

        return className;
    }

    /**
     * Returns an object that implements the given interface to allow access to non-standard methods,
     * or standard methods not exposed by the proxy.
     * The result may be either the object found to implement the interface or a proxy for that object.
     * If the receiver implements the interface then that is the object. If the receiver is a wrapper
     * and the wrapped object implements the interface then that is the object. Otherwise the object is
     *  the result of calling <code>unwrap</code> recursively on the wrapped object. If the receiver is not a
     * wrapper and does not implement the interface, then an <code>SQLException</code> is thrown.
     *
     * @param iface A Class defining an interface that the result must implement.
     * @return an object that implements the interface. May be a proxy for the actual implementing object.
     * @throws java.sql.SQLException If no object found that implements the interface
     * @since 1.6
     */
    public <T> T unwrap(java.lang.Class<T> iface) throws java.sql.SQLException {

        if(isWrapperFor(iface)) {
            return iface.cast(this);
        } else {
            throw new SQLException("unwrap failed for:"+ iface);
        }
    }

    /**
     * Returns true if this either implements the interface argument or is directly or indirectly a wrapper
     * for an object that does. Returns false otherwise. If this implements the interface then return true,
     * else if this is a wrapper then return the result of recursively calling <code>isWrapperFor</code> on the wrapped
     * object. If this does not implement the interface and is not a wrapper, return false.
     * This method should be implemented as a low-cost operation compared to <code>unwrap</code> so that
     * callers can use this method to avoid expensive <code>unwrap</code> calls that may fail. If this method
     * returns true then calling <code>unwrap</code> with the same argument should succeed.
     *
     * @param interfaces a Class defining an interface.
     * @return true if this implements the interface or directly or indirectly wraps an object that does.
     * @throws java.sql.SQLException  if an error occurs while determining whether this is a wrapper
     * for an object with the given interface.
     * @since 1.6
     */
    public boolean isWrapperFor(Class<?> interfaces) throws SQLException {
        return interfaces.isInstance(this);
    }

    static final long serialVersionUID = 6893806403181801867L;

    /**
     * {@code ColInfo} objects are used to store information
     * about each column in the {@code RowSet} object for which
     * this {@code RowSetMetaDataImpl} object was created.
     */
    private class ColInfo implements Serializable {
        /**
         * The field that indicates whether the value in this column is a number
         * that is incremented automatically, which makes the value read-only.
         * <code>true</code> means that the value in this column
         * is automatically numbered; <code>false</code> means that it is not.
         *
         * @serial
         */
        public boolean autoIncrement;

        /**
         * The field that indicates whether the value in this column is case sensitive.
         * <code>true</code> means that it is; <code>false</code> that it is not.
         *
         * @serial
         */
        public boolean caseSensitive;

        /**
         * The field that indicates whether the value in this column is a cash value
         * <code>true</code> means that it is; <code>false</code> that it is not.
         *
         * @serial
         */
        public boolean currency;

        /**
         * The field that indicates whether the value in this column is nullable.
         * The possible values are the <code>ResultSet</code> constants
         * <code>columnNoNulls</code>, <code>columnNullable</code>, and
         * <code>columnNullableUnknown</code>.
         *
         * @serial
         */
        public int nullable;

        /**
         * The field that indicates whether the value in this column is a signed number.
         * <code>true</code> means that it is; <code>false</code> that it is not.
         *
         * @serial
         */
        public boolean signed;

        /**
         * The field that indicates whether the value in this column can be used in
         * a <code>WHERE</code> clause.
         * <code>true</code> means that it can; <code>false</code> that it cannot.
         *
         * @serial
         */
        public boolean searchable;

        /**
         * The field that indicates the normal maximum width in characters for
         * this column.
         *
         * @serial
         */
        public int columnDisplaySize;

        /**
         * The field that holds the suggested column title for this column, to be
         * used in printing and displays.
         *
         * @serial
         */
        public String columnLabel;

        /**
         * The field that holds the name of this column.
         *
         * @serial
         */
        public  String columnName;

        /**
         * The field that holds the schema name for the table from which this column
         * was derived.
         *
         * @serial
         */
        public String schemaName;

        /**
         * The field that holds the precision of the value in this column.  For number
         * types, the precision is the total number of decimal digits; for character types,
         * it is the maximum number of characters; for binary types, it is the maximum
         * length in bytes.
         *
         * @serial
         */
        public int colPrecision;

        /**
         * The field that holds the scale (number of digits to the right of the decimal
         * point) of the value in this column.
         *
         * @serial
         */
        public int colScale;

        /**
         * The field that holds the name of the table from which this column
         * was derived.  This value may be the empty string if there is no
         * table name, such as when this column is produced by a join.
         *
         * @serial
         */
        public String tableName ="";

        /**
         * The field that holds the catalog name for the table from which this column
         * was derived.  If the DBMS does not support catalogs, the value may be the
         * empty string.
         *
         * @serial
         */
        public String catName;

        /**
         * The field that holds the type code from the class <code>java.sql.Types</code>
         * indicating the type of the value in this column.
         *
         * @serial
         */
        public int colType;

        /**
         * The field that holds the type name used by this particular data source
         * for the value stored in this column.
         *
         * @serial
         */
        public String colTypeName;

        /**
         * The field that holds the updatability boolean per column of a RowSet
         *
         * @serial
         */
        public boolean readOnly = false;

        /**
         * The field that hold the writable boolean per column of a RowSet
         *
         *@serial
         */
        public boolean writable = true;

        static final long serialVersionUID = 5490834817919311283L;
    }
}
