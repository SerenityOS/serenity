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
 * The mapping in the Java programming language of an SQL {@code REF}
 * value, which is a reference to an SQL structured type value in the database.
 * <P>
 * SQL {@code REF} values are stored in a table that contains
 * instances of a referenceable SQL structured type, and each {@code REF}
 * value is a unique identifier for one instance in that table.
 * An SQL {@code REF} value may be used in place of the
 * SQL structured type it references, either as a column value in a
 * table or an attribute value in a structured type.
 * <P>
 * Because an SQL {@code REF} value is a logical pointer to an
 * SQL structured type, a {@code Ref} object is by default also a logical
 * pointer. Thus, retrieving an SQL {@code REF} value as
 * a {@code Ref} object does not materialize
 * the attributes of the structured type on the client.
 * <P>
 * A {@code Ref} object can be stored in the database using the
 * {@code PreparedStatement.setRef} method.
  * <p>
 * All methods on the {@code Ref} interface must be fully implemented if the
 * JDBC driver supports the data type.
 *
 * @see Struct
 * @since 1.2
 */
public interface Ref {

    /**
     * Retrieves the fully-qualified SQL name of the SQL structured type that
     * this {@code Ref} object references.
     *
     * @return the fully-qualified SQL name of the referenced SQL structured type
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    String getBaseTypeName() throws SQLException;

    /**
     * Retrieves the referenced object and maps it to a Java type
     * using the given type map.
     *
     * @param map a {@code java.util.Map} object that contains
     *        the mapping to use (the fully-qualified name of the SQL
     *        structured type being referenced and the class object for
     *        {@code SQLData} implementation to which the SQL
     *        structured type will be mapped)
     * @return  a Java {@code Object} that is the custom mapping for
     *          the SQL structured type to which this {@code Ref}
     *          object refers
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.4
     * @see #setObject
     */
    Object getObject(java.util.Map<String,Class<?>> map) throws SQLException;


    /**
     * Retrieves the SQL structured type instance referenced by
     * this {@code Ref} object.  If the connection's type map has an entry
     * for the structured type, the instance will be custom mapped to
     * the Java class indicated in the type map.  Otherwise, the
     * structured type instance will be mapped to a {@code Struct} object.
     *
     * @return  a Java {@code Object} that is the mapping for
     *          the SQL structured type to which this {@code Ref}
     *          object refers
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.4
     * @see #setObject
     */
    Object getObject() throws SQLException;

    /**
     * Sets the structured type value that this {@code Ref}
     * object references to the given instance of {@code Object}.
     * The driver converts this to an SQL structured type when it
     * sends it to the database.
     *
     * @param value an {@code Object} representing the SQL
     *        structured type instance that this
     *        {@code Ref} object will reference
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.4
     * @see #getObject()
     * @see #getObject(Map)
     * @see PreparedStatement#setObject(int, Object)
     * @see CallableStatement#setObject(String, Object)
     */
    void setObject(Object value) throws SQLException;

}
