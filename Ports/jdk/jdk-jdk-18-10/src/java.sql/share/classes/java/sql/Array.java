/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.sql;

/**
 * The mapping in the Java programming language for the SQL type
 * {@code ARRAY}.
 * By default, an {@code Array} value is a transaction-duration
 * reference to an SQL {@code ARRAY} value.  By default, an {@code Array}
 * object is implemented using an SQL LOCATOR(array) internally, which
 * means that an {@code Array} object contains a logical pointer
 * to the data in the SQL {@code ARRAY} value rather
 * than containing the {@code ARRAY} value's data.
 * <p>
 * The {@code Array} interface provides methods for bringing an SQL
 * {@code ARRAY} value's data to the client as either an array or a
 * {@code ResultSet} object.
 * If the elements of the SQL {@code ARRAY}
 * are a UDT, they may be custom mapped.  To create a custom mapping,
 * a programmer must do two things:
 * <ul>
 * <li>create a class that implements the {@link SQLData}
 * interface for the UDT to be custom mapped.
 * <li>make an entry in a type map that contains
 *   <ul>
 *   <li>the fully-qualified SQL type name of the UDT
 *   <li>the {@code Class} object for the class implementing
 *       {@code SQLData}
 *   </ul>
 * </ul>
 * <p>
 * When a type map with an entry for
 * the base type is supplied to the methods {@code getArray}
 * and {@code getResultSet}, the mapping
 * it contains will be used to map the elements of the {@code ARRAY} value.
 * If no type map is supplied, which would typically be the case,
 * the connection's type map is used by default.
 * If the connection's type map or a type map supplied to a method has no entry
 * for the base type, the elements are mapped according to the standard mapping.
 * <p>
 * All methods on the {@code Array} interface must be fully implemented if the
 * JDBC driver supports the data type.
 *
 * @since 1.2
 */

public interface Array {

  /**
   * Retrieves the SQL type name of the elements in
   * the array designated by this {@code Array} object.
   * If the elements are a built-in type, it returns
   * the database-specific type name of the elements.
   * If the elements are a user-defined type (UDT),
   * this method returns the fully-qualified SQL type name.
   *
   * @return a {@code String} that is the database-specific
   * name for a built-in base type; or the fully-qualified SQL type
   * name for a base type that is a UDT
   * @throws SQLException if an error occurs while attempting
   * to access the type name
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  String getBaseTypeName() throws SQLException;

  /**
   * Retrieves the JDBC type of the elements in the array designated
   * by this {@code Array} object.
   *
   * @return a constant from the class {@link java.sql.Types} that is
   * the type code for the elements in the array designated by this
   * {@code Array} object
   * @throws SQLException if an error occurs while attempting
   * to access the base type
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  int getBaseType() throws SQLException;

  /**
   * Retrieves the contents of the SQL {@code ARRAY} value designated
   * by this
   * {@code Array} object in the form of an array in the Java
   * programming language. This version of the method {@code getArray}
   * uses the type map associated with the connection for customizations of
   * the type mappings.
   * <p>
   * <strong>Note:</strong> When {@code getArray} is used to materialize
   * a base type that maps to a primitive data type, then it is
   * implementation-defined whether the array returned is an array of
   * that primitive data type or an array of {@code Object}.
   *
   * @return an array in the Java programming language that contains
   * the ordered elements of the SQL {@code ARRAY} value
   * designated by this {@code Array} object
   * @throws SQLException if an error occurs while attempting to
   * access the array
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  Object getArray() throws SQLException;

  /**
   * Retrieves the contents of the SQL {@code ARRAY} value designated by this
   * {@code Array} object.
   * This method uses
   * the specified {@code map} for type map customizations
   * unless the base type of the array does not match a user-defined
   * type in {@code map}, in which case it
   * uses the standard mapping. This version of the method
   * {@code getArray} uses either the given type map or the standard mapping;
   * it never uses the type map associated with the connection.
   * <p>
   * <strong>Note:</strong> When {@code getArray} is used to materialize
   * a base type that maps to a primitive data type, then it is
   * implementation-defined whether the array returned is an array of
   * that primitive data type or an array of {@code Object}.
   *
   * @param map a {@code java.util.Map} object that contains mappings
   *            of SQL type names to classes in the Java programming language
   * @return an array in the Java programming language that contains the ordered
   *         elements of the SQL array designated by this object
   * @throws SQLException if an error occurs while attempting to
   *                      access the array
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  Object getArray(java.util.Map<String,Class<?>> map) throws SQLException;

  /**
   * Retrieves a slice of the SQL {@code ARRAY}
   * value designated by this {@code Array} object, beginning with the
   * specified {@code index} and containing up to {@code count}
   * successive elements of the SQL array.  This method uses the type map
   * associated with the connection for customizations of the type mappings.
   * <p>
   * <strong>Note:</strong> When {@code getArray} is used to materialize
   * a base type that maps to a primitive data type, then it is
   * implementation-defined whether the array returned is an array of
   * that primitive data type or an array of {@code Object}.
   *
   * @param index the array index of the first element to retrieve;
   *              the first element is at index 1
   * @param count the number of successive SQL array elements to retrieve
   * @return an array containing up to {@code count} consecutive elements
   * of the SQL array, beginning with element {@code index}
   * @throws SQLException if an error occurs while attempting to
   * access the array
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  Object getArray(long index, int count) throws SQLException;

  /**
   * Retrieves a slice of the SQL {@code ARRAY} value
   * designated by this {@code Array} object, beginning with the specified
   * {@code index} and containing up to {@code count}
   * successive elements of the SQL array.
   * <P>
   * This method uses
   * the specified {@code map} for type map customizations
   * unless the base type of the array does not match a user-defined
   * type in {@code map}, in which case it
   * uses the standard mapping. This version of the method
   * {@code getArray} uses either the given type map or the standard mapping;
   * it never uses the type map associated with the connection.
   * <p>
   * <strong>Note:</strong> When {@code getArray} is used to materialize
   * a base type that maps to a primitive data type, then it is
   * implementation-defined whether the array returned is an array of
   * that primitive data type or an array of {@code Object}.
   *
   * @param index the array index of the first element to retrieve;
   *              the first element is at index 1
   * @param count the number of successive SQL array elements to
   * retrieve
   * @param map a {@code java.util.Map} object
   * that contains SQL type names and the classes in
   * the Java programming language to which they are mapped
   * @return an array containing up to {@code count}
   * consecutive elements of the SQL {@code ARRAY} value designated by this
   * {@code Array} object, beginning with element
   * {@code index}
   * @throws SQLException if an error occurs while attempting to
   * access the array
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  Object getArray(long index, int count, java.util.Map<String,Class<?>> map)
    throws SQLException;

  /**
   * Retrieves a result set that contains the elements of the SQL
   * {@code ARRAY} value
   * designated by this {@code Array} object.  If appropriate,
   * the elements of the array are mapped using the connection's type
   * map; otherwise, the standard mapping is used.
   * <p>
   * The result set contains one row for each array element, with
   * two columns in each row.  The second column stores the element
   * value; the first column stores the index into the array for
   * that element (with the first array element being at index 1).
   * The rows are in ascending order corresponding to
   * the order of the indices.
   *
   * @return a {@link ResultSet} object containing one row for each
   * of the elements in the array designated by this {@code Array}
   * object, with the rows in ascending order based on the indices.
   * @throws SQLException if an error occurs while attempting to
   * access the array
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  ResultSet getResultSet () throws SQLException;

  /**
   * Retrieves a result set that contains the elements of the SQL
   * {@code ARRAY} value designated by this {@code Array} object.
   * This method uses
   * the specified {@code map} for type map customizations
   * unless the base type of the array does not match a user-defined
   * type in {@code map}, in which case it
   * uses the standard mapping. This version of the method
   * {@code getResultSet} uses either the given type map or the standard mapping;
   * it never uses the type map associated with the connection.
   * <p>
   * The result set contains one row for each array element, with
   * two columns in each row.  The second column stores the element
   * value; the first column stores the index into the array for
   * that element (with the first array element being at index 1).
   * The rows are in ascending order corresponding to
   * the order of the indices.
   *
   * @param map contains the mapping of SQL user-defined types to
   * classes in the Java programming language
   * @return a {@code ResultSet} object containing one row for each
   * of the elements in the array designated by this {@code Array}
   * object, with the rows in ascending order based on the indices.
   * @throws SQLException if an error occurs while attempting to
   * access the array
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  ResultSet getResultSet (java.util.Map<String,Class<?>> map) throws SQLException;

  /**
   * Retrieves a result set holding the elements of the subarray that
   * starts at index {@code index} and contains up to
   * {@code count} successive elements.  This method uses
   * the connection's type map to map the elements of the array if
   * the map contains an entry for the base type. Otherwise, the
   * standard mapping is used.
   * <P>
   * The result set has one row for each element of the SQL array
   * designated by this object, with the first row containing the
   * element at index {@code index}.  The result set has
   * up to {@code count} rows in ascending order based on the
   * indices.  Each row has two columns:  The second column stores
   * the element value; the first column stores the index into the
   * array for that element.
   *
   * @param index the array index of the first element to retrieve;
   *              the first element is at index 1
   * @param count the number of successive SQL array elements to retrieve
   * @return a {@code ResultSet} object containing up to
   * {@code count} consecutive elements of the SQL array
   * designated by this {@code Array} object, starting at
   * index {@code index}.
   * @throws SQLException if an error occurs while attempting to
   * access the array
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  ResultSet getResultSet(long index, int count) throws SQLException;

  /**
   * Retrieves a result set holding the elements of the subarray that
   * starts at index {@code index} and contains up to
   * {@code count} successive elements.
   * This method uses
   * the specified {@code map} for type map customizations
   * unless the base type of the array does not match a user-defined
   * type in {@code map}, in which case it
   * uses the standard mapping. This version of the method
   * {@code getResultSet} uses either the given type map or the standard mapping;
   * it never uses the type map associated with the connection.
   * <P>
   * The result set has one row for each element of the SQL array
   * designated by this object, with the first row containing the
   * element at index {@code index}.  The result set has
   * up to {@code count} rows in ascending order based on the
   * indices.  Each row has two columns:  The second column stores
   * the element value; the first column stores the index into the
   * array for that element.
   *
   * @param index the array index of the first element to retrieve;
   *              the first element is at index 1
   * @param count the number of successive SQL array elements to retrieve
   * @param map the {@code Map} object that contains the mapping
   * of SQL type names to classes in the Java(tm) programming language
   * @return a {@code ResultSet} object containing up to
   * {@code count} consecutive elements of the SQL array
   * designated by this {@code Array} object, starting at
   * index {@code index}.
   * @throws SQLException if an error occurs while attempting to
   * access the array
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  ResultSet getResultSet (long index, int count,
                          java.util.Map<String,Class<?>> map)
    throws SQLException;
    /**
     * This method frees the {@code Array} object and releases the resources that
     * it holds. The object is invalid once the {@code free}
     * method is called.
     * <p>
     * After {@code free} has been called, any attempt to invoke a
     * method other than {@code free} will result in a {@code SQLException}
     * being thrown.  If {@code free} is called multiple times, the subsequent
     * calls to {@code free} are treated as a no-op.
     *
     * @throws SQLException if an error occurs releasing
     * the Array's resources
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void free() throws SQLException;

}
