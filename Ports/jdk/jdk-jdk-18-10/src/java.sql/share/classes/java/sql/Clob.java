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

import java.io.Reader;

/**
 * The mapping in the Java programming language
 * for the SQL {@code CLOB} type.
 * An SQL {@code CLOB} is a built-in type
 * that stores a Character Large Object as a column value in a row of
 * a database table.
 * By default drivers implement a {@code Clob} object using an SQL
 * {@code locator(CLOB)}, which means that a {@code Clob} object
 * contains a logical pointer to the SQL {@code CLOB} data rather than
 * the data itself. A {@code Clob} object is valid for the duration
 * of the transaction in which it was created.
 * <P>The {@code Clob} interface provides methods for getting the
 * length of an SQL {@code CLOB} (Character Large Object) value,
 * for materializing a {@code CLOB} value on the client, and for
 * searching for a substring or {@code CLOB} object within a
 * {@code CLOB} value.
 * Methods in the interfaces {@link ResultSet},
 * {@link CallableStatement}, and {@link PreparedStatement}, such as
 * {@code getClob} and {@code setClob} allow a programmer to
 * access an SQL {@code CLOB} value.  In addition, this interface
 * has methods for updating a {@code CLOB} value.
 * <p>
 * All methods on the {@code Clob} interface must be
 * fully implemented if the JDBC driver supports the data type.
 *
 * @since 1.2
 */

public interface Clob {

  /**
   * Retrieves the number of characters
   * in the {@code CLOB} value
   * designated by this {@code Clob} object.
   *
   * @return length of the {@code CLOB} in characters
   * @throws SQLException if there is an error accessing the
   *         length of the {@code CLOB} value
   * @throws SQLFeatureNotSupportedException if the JDBC driver
   *         does not support this method
   * @since 1.2
   */
  long length() throws SQLException;

  /**
   * Retrieves a copy of the specified substring
   * in the {@code CLOB} value
   * designated by this {@code Clob} object.
   * The substring begins at position
   * {@code pos} and has up to {@code length} consecutive
   * characters.
   *
   * @param pos the first character of the substring to be extracted.
   *        The first character is at position 1.
   * @param length the number of consecutive characters to be copied;
   *        the value for length must be 0 or greater
   * @return a {@code String} that is the specified substring in
   *         the {@code CLOB} value designated by this {@code Clob} object
   * @throws SQLException if there is an error accessing the
   *         {@code CLOB} value; if pos is less than 1 or length is
   *         less than 0
   * @throws SQLFeatureNotSupportedException if the JDBC driver
   *         does not support this method
   * @since 1.2
   */
  String getSubString(long pos, int length) throws SQLException;

  /**
   * Retrieves the {@code CLOB} value designated by this {@code Clob}
   * object as a {@code java.io.Reader} object (or as a stream of
   * characters).
   *
   * @return a {@code java.io.Reader} object containing the
   *         {@code CLOB} data
   * @throws SQLException if there is an error accessing the
   *         {@code CLOB} value
   * @throws SQLFeatureNotSupportedException if the JDBC driver
   *         does not support this method
   * @see #setCharacterStream
   * @since 1.2
   */
  java.io.Reader getCharacterStream() throws SQLException;

  /**
   * Retrieves the {@code CLOB} value designated by this {@code Clob}
   * object as an ascii stream.
   *
   * @return a {@code java.io.InputStream} object containing the
   *         {@code CLOB} data
   * @throws SQLException if there is an error accessing the
   *         {@code CLOB} value
   * @throws SQLFeatureNotSupportedException if the JDBC driver
   *         does not support this method
   * @see #setAsciiStream
   * @since 1.2
   */
  java.io.InputStream getAsciiStream() throws SQLException;

  /**
   * Retrieves the character position at which the specified substring
   * {@code searchstr} appears in the SQL {@code CLOB} value
   * represented by this {@code Clob} object.  The search
   * begins at position {@code start}.
   *
   * @param searchstr the substring for which to search
   * @param start the position at which to begin searching;
   *        the first position is 1
   * @return the position at which the substring appears or -1 if it is not
   *         present; the first position is 1
   * @throws SQLException if there is an error accessing the
   *         {@code CLOB} value or if pos is less than 1
   * @throws SQLFeatureNotSupportedException if the JDBC driver
   *         does not support this method
   * @since 1.2
   */
  long position(String searchstr, long start) throws SQLException;

  /**
   * Retrieves the character position at which the specified
   * {@code Clob} object {@code searchstr} appears in this
   * {@code Clob} object.  The search begins at position
   * {@code start}.
   *
   * @param searchstr the {@code Clob} object for which to search
   * @param start the position at which to begin searching; the first
   *              position is 1
   * @return the position at which the {@code Clob} object appears
   *         or -1 if it is not present; the first position is 1
   * @throws SQLException if there is an error accessing the
   *         {@code CLOB} value or if start is less than 1
   * @throws SQLFeatureNotSupportedException if the JDBC driver
   *         does not support this method
   * @since 1.2
   */
  long position(Clob searchstr, long start) throws SQLException;

    //---------------------------- jdbc 3.0 -----------------------------------

    /**
     * Writes the given Java {@code String} to the {@code CLOB}
     * value that this {@code Clob} object designates at the position
     * {@code pos}. The string will overwrite the existing characters
     * in the {@code Clob} object starting at the position
     * {@code pos}.  If the end of the {@code Clob} value is reached
     * while writing the given string, then the length of the {@code Clob}
     * value will be increased to accommodate the extra characters.
     * <p>
     * <b>Note:</b> If the value specified for {@code pos}
     * is greater than the length+1 of the {@code CLOB} value then the
     * behavior is undefined. Some JDBC drivers may throw an
     * {@code SQLException} while other drivers may support this
     * operation.
     *
     * @param pos the position at which to start writing to the {@code CLOB}
     *        value that this {@code Clob} object represents;
     *        the first position is 1.
     * @param str the string to be written to the {@code CLOB}
     *        value that this {@code Clob} designates
     * @return the number of characters written
     * @throws SQLException if there is an error accessing the
     *         {@code CLOB} value or if pos is less than 1
     *
     * @throws SQLFeatureNotSupportedException if the JDBC driver
     *         does not support this method
     * @since 1.4
     */
    int setString(long pos, String str) throws SQLException;

    /**
     * Writes {@code len} characters of {@code str}, starting
     * at character {@code offset}, to the {@code CLOB} value
     * that this {@code Clob} represents.
     * The string will overwrite the existing characters
     * in the {@code Clob} object starting at the position
     * {@code pos}.  If the end of the {@code Clob} value is reached
     * while writing the given string, then the length of the {@code Clob}
     * value will be increased to accommodate the extra characters.
     * <p>
     * <b>Note:</b> If the value specified for {@code pos}
     * is greater than the length+1 of the {@code CLOB} value then the
     * behavior is undefined. Some JDBC drivers may throw an
     * {@code SQLException} while other drivers may support this
     * operation.
     *
     * @param pos the position at which to start writing to this
     *        {@code CLOB} object; The first position  is 1
     * @param str the string to be written to the {@code CLOB}
     *        value that this {@code Clob} object represents
     * @param offset the offset into {@code str} to start reading
     *        the characters to be written
     * @param len the number of characters to be written
     * @return the number of characters written
     * @throws SQLException if there is an error accessing the
     *         {@code CLOB} value or if pos is less than 1
     *
     * @throws SQLFeatureNotSupportedException if the JDBC driver
     *         does not support this method
     * @since 1.4
     */
    int setString(long pos, String str, int offset, int len) throws SQLException;

    /**
     * Retrieves a stream to be used to write Ascii characters to the
     * {@code CLOB} value that this {@code Clob} object represents,
     * starting at position {@code pos}.  Characters written to the stream
     * will overwrite the existing characters
     * in the {@code Clob} object starting at the position
     * {@code pos}.  If the end of the {@code Clob} value is reached
     * while writing characters to the stream, then the length of the {@code Clob}
     * value will be increased to accommodate the extra characters.
     * <p>
     * <b>Note:</b> If the value specified for {@code pos}
     * is greater than the length+1 of the {@code CLOB} value then the
     * behavior is undefined. Some JDBC drivers may throw an
     * {@code SQLException} while other drivers may support this
     * operation.
     *
     * @param pos the position at which to start writing to this
     *        {@code CLOB} object; The first position is 1
     * @return the stream to which ASCII encoded characters can be written
     * @throws SQLException if there is an error accessing the
     *         {@code CLOB} value or if pos is less than 1
     * @throws SQLFeatureNotSupportedException if the JDBC driver
     *         does not support this method
     * @see #getAsciiStream
     *
     * @since 1.4
     */
    java.io.OutputStream setAsciiStream(long pos) throws SQLException;

    /**
     * Retrieves a stream to be used to write a stream of Unicode characters
     * to the {@code CLOB} value that this {@code Clob} object
     * represents, at position {@code pos}. Characters written to the stream
     * will overwrite the existing characters
     * in the {@code Clob} object starting at the position
     * {@code pos}.  If the end of the {@code Clob} value is reached
     * while writing characters to the stream, then the length of the {@code Clob}
     * value will be increased to accommodate the extra characters.
     * <p>
     * <b>Note:</b> If the value specified for {@code pos}
     * is greater than the length+1 of the {@code CLOB} value then the
     * behavior is undefined. Some JDBC drivers may throw an
     * {@code SQLException} while other drivers may support this
     * operation.
     *
     * @param  pos the position at which to start writing to the
     *        {@code CLOB} value; The first position is 1
     *
     * @return a stream to which Unicode encoded characters can be written
     * @throws SQLException if there is an error accessing the
     *         {@code CLOB} value or if pos is less than 1
     * @throws SQLFeatureNotSupportedException if the JDBC driver
     *         does not support this method
     * @see #getCharacterStream
     *
     * @since 1.4
     */
    java.io.Writer setCharacterStream(long pos) throws SQLException;

    /**
     * Truncates the {@code CLOB} value that this {@code Clob}
     * designates to have a length of {@code len}
     * characters.
     * <p>
     * <b>Note:</b> If the value specified for {@code pos}
     * is greater than the length+1 of the {@code CLOB} value then the
     * behavior is undefined. Some JDBC drivers may throw an
     * {@code SQLException} while other drivers may support this
     * operation.
     *
     * @param len the length, in characters, to which the {@code CLOB} value
     *        should be truncated
     * @throws SQLException if there is an error accessing the
     *         {@code CLOB} value or if len is less than 0
     *
     * @throws SQLFeatureNotSupportedException if the JDBC driver
     *         does not support this method
     * @since 1.4
     */
    void truncate(long len) throws SQLException;

    /**
     * This method releases the resources that the {@code Clob} object
     * holds.  The object is invalid once the {@code free} method
     * is called.
     * <p>
     * After {@code free} has been called, any attempt to invoke a
     * method other than {@code free} will result in a {@code SQLException}
     * being thrown.  If {@code free} is called multiple times, the subsequent
     * calls to {@code free} are treated as a no-op.
     *
     * @throws SQLException if an error occurs releasing
     *         the Clob's resources
     *
     * @throws SQLFeatureNotSupportedException if the JDBC driver
     *         does not support this method
     * @since 1.6
     */
    void free() throws SQLException;

    /**
     * Returns a {@code Reader} object that contains
     * a partial {@code Clob} value, starting with the character
     * specified by pos, which is length characters in length.
     *
     * @param pos the offset to the first character of the partial value to
     * be retrieved.  The first character in the Clob is at position 1.
     * @param length the length in characters of the partial value to be retrieved.
     * @return {@code Reader} through which
     *         the partial {@code Clob} value can be read.
     * @throws SQLException if pos is less than 1;
     *         or if pos is greater than the number of characters
     *         in the {@code Clob};
     *         or if pos + length is greater than the number of
     *         characters in the {@code Clob}
     *
     * @throws SQLFeatureNotSupportedException if the JDBC driver
     *         does not support this method
     * @since 1.6
     */
    Reader getCharacterStream(long pos, long length) throws SQLException;

}
