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
 * <p>The standard mapping in the Java programming language for an SQL
 * structured type. A {@code Struct} object contains a
 * value for each attribute of the SQL structured type that
 * it represents.
 * By default, an instance of{@code Struct} is valid as long as the
 * application has a reference to it.
 * <p>
 * All methods on the {@code Struct} interface must be fully implemented if the
 * JDBC driver supports the data type.
 * @since 1.2
 */

public interface Struct {

  /**
   * Retrieves the SQL type name of the SQL structured type
   * that this {@code Struct} object represents.
   *
   * @return the fully-qualified type name of the SQL structured
   *          type for which this {@code Struct} object
   *          is the generic representation
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  String getSQLTypeName() throws SQLException;

  /**
   * Produces the ordered values of the attributes of the SQL
   * structured type that this {@code Struct} object represents.
   * As individual attributes are processed, this method uses the type map
   * associated with the
   * connection for customizations of the type mappings.
   * If there is no
   * entry in the connection's type map that matches the structured
   * type that an attribute represents,
   * the driver uses the standard mapping.
   * <p>
   * Conceptually, this method calls the method
   * {@code getObject} on each attribute
   * of the structured type and returns a Java array containing
   * the result.
   *
   * @return an array containing the ordered attribute values
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  Object[] getAttributes() throws SQLException;

  /**
   * Produces the ordered values of the attributes of the SQL
   * structured type that this {@code Struct} object represents.
   *  As individual attributes are processed, this method uses the given type map
   * for customizations of the type mappings.
   * If there is no
   * entry in the given type map that matches the structured
   * type that an attribute represents,
   * the driver uses the standard mapping. This method never
   * uses the type map associated with the connection.
   * <p>
   * Conceptually, this method calls the method
   * {@code getObject} on each attribute
   * of the structured type and returns a Java array containing
   * the result.
   *
   * @param map a mapping of SQL type names to Java classes
   * @return an array containing the ordered attribute values
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  Object[] getAttributes(java.util.Map<String,Class<?>> map)
      throws SQLException;
}
