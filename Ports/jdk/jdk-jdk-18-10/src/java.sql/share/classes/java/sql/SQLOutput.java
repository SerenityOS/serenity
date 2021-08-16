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
 * The output stream for writing the attributes of a user-defined
 * type back to the database.  This interface, used
 * only for custom mapping, is used by the driver, and its
 * methods are never directly invoked by a programmer.
 * <p>When an object of a class implementing the interface
 * {@code SQLData} is passed as an argument to an SQL statement, the
 * JDBC driver calls the method {@code SQLData.getSQLType} to
 * determine the  kind of SQL
 * datum being passed to the database.
 * The driver then creates an instance of {@code SQLOutput} and
 * passes it to the method {@code SQLData.writeSQL}.
 * The method {@code writeSQL} in turn calls the
 * appropriate {@code SQLOutput} <i>writer</i> methods
 * {@code writeBoolean}, {@code writeCharacterStream}, and so on)
 * to write data from the {@code SQLData} object to
 * the {@code SQLOutput} output stream as the
 * representation of an SQL user-defined type.
 * @since 1.2
 */

 public interface SQLOutput {

  //================================================================
  // Methods for writing attributes to the stream of SQL data.
  // These methods correspond to the column-accessor methods of
  // java.sql.ResultSet.
  //================================================================

  /**
   * Writes the next attribute to the stream as a {@code String}
   * in the Java programming language.
   *
   * @param x the value to pass to the database
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  void writeString(String x) throws SQLException;

  /**
   * Writes the next attribute to the stream as a Java boolean.
   * Writes the next attribute to the stream as a {@code String}
   * in the Java programming language.
   *
   * @param x the value to pass to the database
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  void writeBoolean(boolean x) throws SQLException;

  /**
   * Writes the next attribute to the stream as a Java byte.
   * Writes the next attribute to the stream as a {@code String}
   * in the Java programming language.
   *
   * @param x the value to pass to the database
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  void writeByte(byte x) throws SQLException;

  /**
   * Writes the next attribute to the stream as a Java short.
   * Writes the next attribute to the stream as a {@code String}
   * in the Java programming language.
   *
   * @param x the value to pass to the database
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  void writeShort(short x) throws SQLException;

  /**
   * Writes the next attribute to the stream as a Java int.
   * Writes the next attribute to the stream as a {@code String}
   * in the Java programming language.
   *
   * @param x the value to pass to the database
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  void writeInt(int x) throws SQLException;

  /**
   * Writes the next attribute to the stream as a Java long.
   * Writes the next attribute to the stream as a {@code String}
   * in the Java programming language.
   *
   * @param x the value to pass to the database
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  void writeLong(long x) throws SQLException;

  /**
   * Writes the next attribute to the stream as a Java float.
   * Writes the next attribute to the stream as a {@code String}
   * in the Java programming language.
   *
   * @param x the value to pass to the database
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  void writeFloat(float x) throws SQLException;

  /**
   * Writes the next attribute to the stream as a Java double.
   * Writes the next attribute to the stream as a {@code String}
   * in the Java programming language.
   *
   * @param x the value to pass to the database
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  void writeDouble(double x) throws SQLException;

  /**
   * Writes the next attribute to the stream as a java.math.BigDecimal object.
   * Writes the next attribute to the stream as a {@code String}
   * in the Java programming language.
   *
   * @param x the value to pass to the database
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  void writeBigDecimal(java.math.BigDecimal x) throws SQLException;

  /**
   * Writes the next attribute to the stream as an array of bytes.
   * Writes the next attribute to the stream as a {@code String}
   * in the Java programming language.
   *
   * @param x the value to pass to the database
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  void writeBytes(byte[] x) throws SQLException;

  /**
   * Writes the next attribute to the stream as a java.sql.Date object.
   * Writes the next attribute to the stream as a {@code java.sql.Date} object
   * in the Java programming language.
   *
   * @param x the value to pass to the database
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  void writeDate(java.sql.Date x) throws SQLException;

  /**
   * Writes the next attribute to the stream as a java.sql.Time object.
   * Writes the next attribute to the stream as a {@code java.sql.Date} object
   * in the Java programming language.
   *
   * @param x the value to pass to the database
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  void writeTime(java.sql.Time x) throws SQLException;

  /**
   * Writes the next attribute to the stream as a java.sql.Timestamp object.
   * Writes the next attribute to the stream as a {@code java.sql.Date} object
   * in the Java programming language.
   *
   * @param x the value to pass to the database
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  void writeTimestamp(java.sql.Timestamp x) throws SQLException;

  /**
   * Writes the next attribute to the stream as a stream of Unicode characters.
   *
   * @param x the value to pass to the database
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  void writeCharacterStream(java.io.Reader x) throws SQLException;

  /**
   * Writes the next attribute to the stream as a stream of ASCII characters.
   *
   * @param x the value to pass to the database
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  void writeAsciiStream(java.io.InputStream x) throws SQLException;

  /**
   * Writes the next attribute to the stream as a stream of uninterpreted
   * bytes.
   *
   * @param x the value to pass to the database
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  void writeBinaryStream(java.io.InputStream x) throws SQLException;

  //================================================================
  // Methods for writing items of SQL user-defined types to the stream.
  // These methods pass objects to the database as values of SQL
  // Structured Types, Distinct Types, Constructed Types, and Locator
  // Types.  They decompose the Java object(s) and write leaf data
  // items using the methods above.
  //================================================================

  /**
   * Writes to the stream the data contained in the given
   * {@code SQLData} object.
   * When the {@code SQLData} object is {@code null}, this
   * method writes an SQL {@code NULL} to the stream.
   * Otherwise, it calls the {@code SQLData.writeSQL}
   * method of the given object, which
   * writes the object's attributes to the stream.
   * The implementation of the method {@code SQLData.writeSQL}
   * calls the appropriate {@code SQLOutput} writer method(s)
   * for writing each of the object's attributes in order.
   * The attributes must be read from an {@code SQLInput}
   * input stream and written to an {@code SQLOutput}
   * output stream in the same order in which they were
   * listed in the SQL definition of the user-defined type.
   *
   * @param x the object representing data of an SQL structured or
   * distinct type
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  void writeObject(SQLData x) throws SQLException;

  /**
   * Writes an SQL {@code REF} value to the stream.
   *
   * @param x a {@code Ref} object representing data of an SQL
   * {@code REF} value
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  void writeRef(Ref x) throws SQLException;

  /**
   * Writes an SQL {@code BLOB} value to the stream.
   *
   * @param x a {@code Blob} object representing data of an SQL
   * {@code BLOB} value
   *
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  void writeBlob(Blob x) throws SQLException;

  /**
   * Writes an SQL {@code CLOB} value to the stream.
   *
   * @param x a {@code Clob} object representing data of an SQL
   * {@code CLOB} value
   *
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  void writeClob(Clob x) throws SQLException;

  /**
   * Writes an SQL structured type value to the stream.
   *
   * @param x a {@code Struct} object representing data of an SQL
   * structured type
   *
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  void writeStruct(Struct x) throws SQLException;

  /**
   * Writes an SQL {@code ARRAY} value to the stream.
   *
   * @param x an {@code Array} object representing data of an SQL
   * {@code ARRAY} type
   *
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.2
   */
  void writeArray(Array x) throws SQLException;

     //--------------------------- JDBC 3.0 ------------------------

     /**
      * Writes a SQL {@code DATALINK} value to the stream.
      *
      * @param x a {@code java.net.URL} object representing the data
      * of SQL DATALINK type
      *
      * @throws SQLException if a database access error occurs
      * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
      * this method
      * @since 1.4
      */
     void writeURL(java.net.URL x) throws SQLException;

     //--------------------------- JDBC 4.0 ------------------------

  /**
   * Writes the next attribute to the stream as a {@code String}
   * in the Java programming language. The driver converts this to a
   * SQL {@code NCHAR} or
   * {@code NVARCHAR} or {@code LONGNVARCHAR} value
   * (depending on the argument's
   * size relative to the driver's limits on {@code NVARCHAR} values)
   * when it sends it to the stream.
   *
   * @param x the value to pass to the database
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  void writeNString(String x) throws SQLException;

  /**
   * Writes an SQL {@code NCLOB} value to the stream.
   *
   * @param x a {@code NClob} object representing data of an SQL
   * {@code NCLOB} value
   *
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  void writeNClob(NClob x) throws SQLException;


  /**
   * Writes an SQL {@code ROWID} value to the stream.
   *
   * @param x a {@code RowId} object representing data of an SQL
   * {@code ROWID} value
   *
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  void writeRowId(RowId x) throws SQLException;


  /**
   * Writes an SQL {@code XML} value to the stream.
   *
   * @param x a {@code SQLXML} object representing data of an SQL
   * {@code XML} value
   *
   * @throws SQLException if a database access error occurs,
   * the {@code java.xml.transform.Result},
   *  {@code Writer} or {@code OutputStream} has not been closed for the {@code SQLXML} object or
   *  if there is an error processing the XML value.  The {@code getCause} method
   *  of the exception may provide a more detailed exception, for example, if the
   *  stream does not contain valid XML.
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @since 1.6
   */
  void writeSQLXML(SQLXML x) throws SQLException;

  //--------------------------JDBC 4.2 -----------------------------

  /**
   * Writes to the stream the data contained in the given object. The
   * object will be converted to the specified targetSqlType
   * before being sent to the stream.
   *<p>
   * When the {@code object} is {@code null}, this
   * method writes an SQL {@code NULL} to the stream.
   * <p>
   * If the object has a custom mapping (is of a class implementing the
   * interface {@code SQLData}),
   * the JDBC driver should call the method {@code SQLData.writeSQL} to
   * write it to the SQL data stream.
   * If, on the other hand, the object is of a class implementing
   * {@code Ref}, {@code Blob}, {@code Clob},  {@code NClob},
   *  {@code Struct}, {@code java.net.URL},
   * or {@code Array}, the driver should pass it to the database as a
   * value of the corresponding SQL type.
   *<P>
   * The default implementation will throw {@code SQLFeatureNotSupportedException}
   *
   * @param x the object containing the input parameter value
   * @param targetSqlType the SQL type to be sent to the database.
   * @throws SQLException if a database access error occurs  or
   *         if the Java Object specified by x is an InputStream
   *         or Reader object and the value of the scale parameter is less
   *         than zero
   * @throws SQLFeatureNotSupportedException if
   * the JDBC driver does not support this data type
   * @see JDBCType
   * @see SQLType
   * @since 1.8
   */
  default void writeObject(Object x, SQLType targetSqlType) throws SQLException {
        throw new SQLFeatureNotSupportedException();
  }

}

