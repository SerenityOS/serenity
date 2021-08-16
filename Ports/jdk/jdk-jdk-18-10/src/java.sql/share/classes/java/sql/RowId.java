/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * The representation (mapping) in the Java programming language of an SQL ROWID
 * value. An SQL ROWID is a built-in type, a value of which can be thought of as
 * an address  for its identified row in a database table. Whether that address
 * is logical or, in any  respects, physical is determined by its originating data
 * source.
 * <p>
 * Methods in the interfaces {@code ResultSet}, {@code CallableStatement},
 * and {@code PreparedStatement}, such as {@code getRowId} and {@code setRowId}
 * allow a programmer to access a SQL {@code ROWID}  value. The {@code RowId}
 * interface provides a method
 * for representing the value of the {@code ROWID} as a byte array or as a
 * {@code String}.
 * <p>
 * The method {@code getRowIdLifetime} in the interface {@code DatabaseMetaData},
 * can be used
 * to determine if a {@code RowId} object remains valid for the duration of the transaction in
 * which  the {@code RowId} was created, the duration of the session in which
 * the {@code RowId} was created,
 * or, effectively, for as long as its identified row is not deleted. In addition
 * to specifying the duration of its valid lifetime outside its originating data
 * source, {@code getRowIdLifetime} specifies the duration of a {@code ROWID}
 * value's valid lifetime
 * within its originating data source. In this, it differs from a large object,
 * because there is no limit on the valid lifetime of a large  object within its
 * originating data source.
 * <p>
 * All methods on the {@code RowId} interface must be fully implemented if the
 * JDBC driver supports the data type.
 *
 * @see java.sql.DatabaseMetaData
 * @since 1.6
 */

public interface RowId {
    /**
     * Compares this {@code RowId} to the specified object. The result is
     * {@code true} if and only if the argument is not null and is a RowId
     * object that represents the same ROWID as  this object.
     * <p>
     * It is important
     * to consider both the origin and the valid lifetime of a {@code RowId}
     * when comparing it to another {@code RowId}. If both are valid, and
     * both are from the same table on the same data source, then if they are equal
     * they identify
     * the same row; if one or more is no longer guaranteed to be valid, or if
     * they originate from different data sources, or different tables on the
     * same data source, they  may be equal but still
     * not identify the same row.
     *
     * @param obj the {@code Object} to compare this {@code RowId} object
     *     against.
     * @return true if the {@code RowId}s are equal; false otherwise
     * @since 1.6
     */
    boolean equals(Object obj);

    /**
     * Returns an array of bytes representing the value of the SQL {@code ROWID}
     * designated by this {@code java.sql.RowId} object.
     *
     * @return an array of bytes, whose length is determined by the driver supplying
     *     the connection, representing the value of the ROWID designated by this
     *     java.sql.RowId object.
     */
     byte[] getBytes();

     /**
      * Returns a String representing the value of the SQL ROWID designated by this
      * {@code java.sql.RowId} object.
      * <p>
      *Like {@code java.sql.Date.toString()}
      * returns the contents of its DATE as the {@code String} "2004-03-17"
      * rather than as  DATE literal in SQL (which would have been the {@code String}
      * DATE "2004-03-17"), toString()
      * returns the contents of its ROWID in a form specific to the driver supplying
      * the connection, and possibly not as a {@code ROWID} literal.
      *
      * @return a String whose format is determined by the driver supplying the
      *     connection, representing the value of the {@code ROWID} designated
      *     by this {@code java.sql.RowId}  object.
      */
     String toString();

     /**
      * Returns a hash code value of this {@code RowId} object.
      *
      * @return a hash code for the {@code RowId}
      */
     int hashCode();

}
