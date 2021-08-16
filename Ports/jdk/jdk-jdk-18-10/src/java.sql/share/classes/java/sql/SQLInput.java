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
 * An input stream that contains a stream of values representing an
 * instance of an SQL structured type or an SQL distinct type.
 * This interface, used only for custom mapping, is used by the driver
 * behind the scenes, and a programmer never directly invokes
 * {@code SQLInput} methods. The <i>reader</i> methods
 * ({@code readLong}, {@code readBytes}, and so on)
 * provide a way  for an implementation of the {@code SQLData}
 *  interface to read the values in an {@code SQLInput} object.
 *  And as described in {@code SQLData}, calls to reader methods must
 * be made in the order that their corresponding attributes appear in the
 * SQL definition of the type.
 * The method {@code wasNull} is used to determine whether
 * the last value read was SQL {@code NULL}.
 * <P>When the method {@code getObject} is called with an
 * object of a class implementing the interface {@code SQLData},
 * the JDBC driver calls the method {@code SQLData.getSQLType}
 * to determine the SQL type of the user-defined type (UDT)
 * being custom mapped. The driver
 * creates an instance of {@code SQLInput}, populating it with the
 * attributes of the UDT.  The driver then passes the input
 * stream to the method {@code SQLData.readSQL}, which in turn
 * calls the {@code SQLInput} reader methods
 * in its implementation for reading the
 * attributes from the input stream.
 * @since 1.2
 */

public interface SQLInput {


    //================================================================
    // Methods for reading attributes from the stream of SQL data.
    // These methods correspond to the column-accessor methods of
    // java.sql.ResultSet.
    //================================================================

    /**
     * Reads the next attribute in the stream and returns it as a {@code String}
     * in the Java programming language.
     *
     * @return the attribute; if the value is SQL {@code NULL}, returns {@code null}
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    String readString() throws SQLException;

    /**
     * Reads the next attribute in the stream and returns it as a {@code boolean}
     * in the Java programming language.
     *
     * @return the attribute; if the value is SQL {@code NULL}, returns {@code false}
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    boolean readBoolean() throws SQLException;

    /**
     * Reads the next attribute in the stream and returns it as a {@code byte}
     * in the Java programming language.
     *
     * @return the attribute; if the value is SQL {@code NULL}, returns {@code 0}
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    byte readByte() throws SQLException;

    /**
     * Reads the next attribute in the stream and returns it as a {@code short}
     * in the Java programming language.
     *
     * @return the attribute; if the value is SQL {@code NULL}, returns {@code 0}
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    short readShort() throws SQLException;

    /**
     * Reads the next attribute in the stream and returns it as an {@code int}
     * in the Java programming language.
     *
     * @return the attribute; if the value is SQL {@code NULL}, returns {@code 0}
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    int readInt() throws SQLException;

    /**
     * Reads the next attribute in the stream and returns it as a {@code long}
     * in the Java programming language.
     *
     * @return the attribute; if the value is SQL {@code NULL}, returns {@code 0}
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    long readLong() throws SQLException;

    /**
     * Reads the next attribute in the stream and returns it as a {@code float}
     * in the Java programming language.
     *
     * @return the attribute; if the value is SQL {@code NULL}, returns {@code 0}
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    float readFloat() throws SQLException;

    /**
     * Reads the next attribute in the stream and returns it as a {@code double}
     * in the Java programming language.
     *
     * @return the attribute; if the value is SQL {@code NULL}, returns {@code 0}
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    double readDouble() throws SQLException;

    /**
     * Reads the next attribute in the stream and returns it as a {@code java.math.BigDecimal}
     * object in the Java programming language.
     *
     * @return the attribute; if the value is SQL {@code NULL}, returns {@code null}
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    java.math.BigDecimal readBigDecimal() throws SQLException;

    /**
     * Reads the next attribute in the stream and returns it as an array of bytes
     * in the Java programming language.
     *
     * @return the attribute; if the value is SQL {@code NULL}, returns {@code null}
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    byte[] readBytes() throws SQLException;

    /**
     * Reads the next attribute in the stream and returns it as a {@code java.sql.Date} object.
     *
     * @return the attribute; if the value is SQL {@code NULL}, returns {@code null}
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    java.sql.Date readDate() throws SQLException;

    /**
     * Reads the next attribute in the stream and returns it as a {@code java.sql.Time} object.
     *
     * @return the attribute; if the value is SQL {@code NULL}, returns {@code null}
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    java.sql.Time readTime() throws SQLException;

    /**
     * Reads the next attribute in the stream and returns it as a {@code java.sql.Timestamp} object.
     *
     * @return the attribute; if the value is SQL {@code NULL}, returns {@code null}
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    java.sql.Timestamp readTimestamp() throws SQLException;

    /**
     * Reads the next attribute in the stream and returns it as a stream of Unicode characters.
     *
     * @return the attribute; if the value is SQL {@code NULL}, returns {@code null}
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    java.io.Reader readCharacterStream() throws SQLException;

    /**
     * Reads the next attribute in the stream and returns it as a stream of ASCII characters.
     *
     * @return the attribute; if the value is SQL {@code NULL}, returns {@code null}
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    java.io.InputStream readAsciiStream() throws SQLException;

    /**
     * Reads the next attribute in the stream and returns it as a stream of uninterpreted
     * bytes.
     *
     * @return the attribute; if the value is SQL {@code NULL}, returns {@code null}
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    java.io.InputStream readBinaryStream() throws SQLException;

    //================================================================
    // Methods for reading items of SQL user-defined types from the stream.
    //================================================================

    /**
     * Reads the datum at the head of the stream and returns it as an
     * {@code Object} in the Java programming language.  The
     * actual type of the object returned is determined by the default type
     * mapping, and any customizations present in this stream's type map.
     *
     * <P>A type map is registered with the stream by the JDBC driver before the
     * stream is passed to the application.
     *
     * <P>When the datum at the head of the stream is an SQL {@code NULL},
     * the method returns {@code null}.  If the datum is an SQL structured or distinct
     * type, it determines the SQL type of the datum at the head of the stream.
     * If the stream's type map has an entry for that SQL type, the driver
     * constructs an object of the appropriate class and calls the method
     * {@code SQLData.readSQL} on that object, which reads additional data from the
     * stream, using the protocol described for that method.
     *
     * @return the datum at the head of the stream as an {@code Object} in the
     * Java programming language;{@code null} if the datum is SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    Object readObject() throws SQLException;

    /**
     * Reads an SQL {@code REF} value from the stream and returns it as a
     * {@code Ref} object in the Java programming language.
     *
     * @return a {@code Ref} object representing the SQL {@code REF} value
     * at the head of the stream; {@code null} if the value read is
     * SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    Ref readRef() throws SQLException;

    /**
     * Reads an SQL {@code BLOB} value from the stream and returns it as a
     * {@code Blob} object in the Java programming language.
     *
     * @return a {@code Blob} object representing data of the SQL {@code BLOB} value
     * at the head of the stream; {@code null} if the value read is
     * SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    Blob readBlob() throws SQLException;

    /**
     * Reads an SQL {@code CLOB} value from the stream and returns it as a
     * {@code Clob} object in the Java programming language.
     *
     * @return a {@code Clob} object representing data of the SQL {@code CLOB} value
     * at the head of the stream; {@code null} if the value read is
     * SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    Clob readClob() throws SQLException;

    /**
     * Reads an SQL {@code ARRAY} value from the stream and returns it as an
     * {@code Array} object in the Java programming language.
     *
     * @return an {@code Array} object representing data of the SQL
     * {@code ARRAY} value at the head of the stream; {@code null}
     * if the value read is SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    Array readArray() throws SQLException;

    /**
     * Retrieves whether the last value read was SQL {@code NULL}.
     *
     * @return {@code true} if the most recently read SQL value was SQL
     * {@code NULL}; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     *
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    boolean wasNull() throws SQLException;

    //---------------------------- JDBC 3.0 -------------------------

    /**
     * Reads an SQL {@code DATALINK} value from the stream and returns it as a
     * {@code java.net.URL} object in the Java programming language.
     *
     * @return a {@code java.net.URL} object.
     * @throws SQLException if a database access error occurs,
     *         or if a URL is malformed
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.4
     */
    java.net.URL readURL() throws SQLException;

     //---------------------------- JDBC 4.0 -------------------------

    /**
     * Reads an SQL {@code NCLOB} value from the stream and returns it as a
     * {@code NClob} object in the Java programming language.
     *
     * @return a {@code NClob} object representing data of the SQL {@code NCLOB} value
     * at the head of the stream; {@code null} if the value read is
     * SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    NClob readNClob() throws SQLException;

    /**
     * Reads the next attribute in the stream and returns it as a {@code String}
     * in the Java programming language. It is intended for use when
     * accessing  {@code NCHAR},{@code NVARCHAR}
     * and {@code LONGNVARCHAR} columns.
     *
     * @return the attribute; if the value is SQL {@code NULL}, returns {@code null}
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    String readNString() throws SQLException;

    /**
     * Reads an SQL {@code XML} value from the stream and returns it as a
     * {@code SQLXML} object in the Java programming language.
     *
     * @return a {@code SQLXML} object representing data of the SQL {@code XML} value
     * at the head of the stream; {@code null} if the value read is
     * SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    SQLXML readSQLXML() throws SQLException;

    /**
     * Reads an SQL {@code ROWID} value from the stream and returns it as a
     * {@code RowId} object in the Java programming language.
     *
     * @return a {@code RowId} object representing data of the SQL {@code ROWID} value
     * at the head of the stream; {@code null} if the value read is
     * SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    RowId readRowId() throws SQLException;

    //--------------------------JDBC 4.2 -----------------------------

    /**
     * Reads the next attribute in the stream and returns it as an
     * {@code Object} in the Java programming language. The
     * actual type of the object returned is determined by the specified
     * Java data type, and any customizations present in this
     * stream's type map.
     *
     * <P>A type map is registered with the stream by the JDBC driver before the
     * stream is passed to the application.
     *
     * <P>When the attribute at the head of the stream is an SQL {@code NULL}
     * the method returns {@code null}. If the attribute is an SQL
     * structured or distinct
     * type, it determines the SQL type of the attribute at the head of the stream.
     * If the stream's type map has an entry for that SQL type, the driver
     * constructs an object of the appropriate class and calls the method
     * {@code SQLData.readSQL} on that object, which reads additional data from the
     * stream, using the protocol described for that method.
     *<p>
     * The default implementation will throw {@code SQLFeatureNotSupportedException}
     *
     * @param <T> the type of the class modeled by this Class object
     * @param type Class representing the Java data type to convert the attribute to.
     * @return the attribute at the head of the stream as an {@code Object} in the
     * Java programming language;{@code null} if the attribute is SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.8
     */
    default <T> T readObject(Class<T> type) throws SQLException {
       throw new SQLFeatureNotSupportedException();
    }
}
