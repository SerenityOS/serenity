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

import java.io.InputStream;

/**
 * The representation (mapping) in
 * the Java programming language of an SQL
 * {@code BLOB} value.  An SQL {@code BLOB} is a built-in type
 * that stores a Binary Large Object as a column value in a row of
 * a database table. By default drivers implement {@code Blob} using
 * an SQL {@code locator(BLOB)}, which means that a
 * {@code Blob} object contains a logical pointer to the
 * SQL {@code BLOB} data rather than the data itself.
 * A {@code Blob} object is valid for the duration of the
 * transaction in which is was created.
 *
 * <P>Methods in the interfaces {@link ResultSet},
 * {@link CallableStatement}, and {@link PreparedStatement}, such as
 * {@code getBlob} and {@code setBlob} allow a programmer to
 * access an SQL {@code BLOB} value.
 * The {@code Blob} interface provides methods for getting the
 * length of an SQL {@code BLOB} (Binary Large Object) value,
 * for materializing a {@code BLOB} value on the client, and for
 * determining the position of a pattern of bytes within a
 * {@code BLOB} value. In addition, this interface has methods for updating
 * a {@code BLOB} value.
 * <p>
 * All methods on the {@code Blob} interface must be fully implemented if the
 * JDBC driver supports the data type.
 *
 * @since 1.2
 */

public interface Blob {

  /**
   * Returns the number of bytes in the {@code BLOB} value
   * designated by this {@code Blob} object.
   *
   * @return length of the {@code BLOB} in bytes
   * @throws SQLException if there is an error accessing the
   *         length of the {@code BLOB}
   * @throws SQLFeatureNotSupportedException if the JDBC driver
   *         does not support this method
   * @since 1.2
   */
  long length() throws SQLException;

  /**
   * Retrieves all or part of the {@code BLOB}
   * value that this {@code Blob} object represents, as an array of
   * bytes.  This {@code byte} array contains up to {@code length}
   * consecutive bytes starting at position {@code pos}.
   *
   * @param pos the ordinal position of the first byte in the
   *        {@code BLOB} value to be extracted; the first byte is at
   *        position 1
   * @param length the number of consecutive bytes to be copied; the value
   *        for length must be 0 or greater
   * @return a byte array containing up to {@code length}
   *         consecutive bytes from the {@code BLOB} value designated
   *         by this {@code Blob} object, starting with the
   *         byte at position {@code pos}
   * @throws SQLException if there is an error accessing the
   *         {@code BLOB} value; if pos is less than 1 or length is
   *         less than 0
   * @throws SQLFeatureNotSupportedException if the JDBC driver
   *         does not support this method
   * @see #setBytes
   * @since 1.2
   */
  byte[] getBytes(long pos, int length) throws SQLException;

  /**
   * Retrieves the {@code BLOB} value designated by this
   * {@code Blob} instance as a stream.
   *
   * @return a stream containing the {@code BLOB} data
   * @throws SQLException if there is an error accessing the
   *         {@code BLOB} value
   * @throws SQLFeatureNotSupportedException if the JDBC driver
   *         does not support this method
   * @see #setBinaryStream
   * @since 1.2
   */
  java.io.InputStream getBinaryStream () throws SQLException;

  /**
   * Retrieves the byte position at which the specified byte array
   * {@code pattern} begins within the {@code BLOB}
   * value that this {@code Blob} object represents.
   * The search for {@code pattern} begins at position
   * {@code start}.
   *
   * @param pattern the byte array for which to search
   * @param start the position at which to begin searching; the
   *        first position is 1
   * @return the position at which the pattern appears, else -1
   * @throws SQLException if there is an error accessing the
   *         {@code BLOB} or if start is less than 1
   * @throws SQLFeatureNotSupportedException if the JDBC driver
   *         does not support this method
   * @since 1.2
   */
  long position(byte pattern[], long start) throws SQLException;

  /**
   * Retrieves the byte position in the {@code BLOB} value
   * designated by this {@code Blob} object at which
   * {@code pattern} begins.  The search begins at position
   * {@code start}.
   *
   * @param pattern the {@code Blob} object designating
   *        the {@code BLOB} value for which to search
   * @param start the position in the {@code BLOB} value
   *        at which to begin searching; the first position is 1
   * @return the position at which the pattern begins, else -1
   * @throws SQLException if there is an error accessing the
   *         {@code BLOB} value or if start is less than 1
   * @throws SQLFeatureNotSupportedException if the JDBC driver
   *         does not support this method
   * @since 1.2
   */
  long position(Blob pattern, long start) throws SQLException;

    // -------------------------- JDBC 3.0 -----------------------------------

    /**
     * Writes the given array of bytes to the {@code BLOB} value that
     * this {@code Blob} object represents, starting at position
     * {@code pos}, and returns the number of bytes written.
     * The array of bytes will overwrite the existing bytes
     * in the {@code Blob} object starting at the position
     * {@code pos}.  If the end of the {@code Blob} value is reached
     * while writing the array of bytes, then the length of the {@code Blob}
     * value will be increased to accommodate the extra bytes.
     * <p>
     * <b>Note:</b> If the value specified for {@code pos}
     * is greater than the length+1 of the {@code BLOB} value then the
     * behavior is undefined. Some JDBC drivers may throw an
     * {@code SQLException} while other drivers may support this
     * operation.
     *
     * @param pos the position in the {@code BLOB} object at which
     *        to start writing; the first position is 1
     * @param bytes the array of bytes to be written to the {@code BLOB}
     *        value that this {@code Blob} object represents
     * @return the number of bytes written
     * @throws SQLException if there is an error accessing the
     *         {@code BLOB} value or if pos is less than 1
     * @throws SQLFeatureNotSupportedException if the JDBC driver
     *         does not support this method
     * @see #getBytes
     * @since 1.4
     */
    int setBytes(long pos, byte[] bytes) throws SQLException;

    /**
     * Writes all or part of the given {@code byte} array to the
     * {@code BLOB} value that this {@code Blob} object represents
     * and returns the number of bytes written.
     * Writing starts at position {@code pos} in the {@code BLOB}
     * value; {@code len} bytes from the given byte array are written.
     * The array of bytes will overwrite the existing bytes
     * in the {@code Blob} object starting at the position
     * {@code pos}.  If the end of the {@code Blob} value is reached
     * while writing the array of bytes, then the length of the {@code Blob}
     * value will be increased to accommodate the extra bytes.
     * <p>
     * <b>Note:</b> If the value specified for {@code pos}
     * is greater than the length+1 of the {@code BLOB} value then the
     * behavior is undefined. Some JDBC drivers may throw an
     * {@code SQLException} while other drivers may support this
     * operation.
     *
     * @param pos the position in the {@code BLOB} object at which
     *        to start writing; the first position is 1
     * @param bytes the array of bytes to be written to this {@code BLOB}
     *        object
     * @param offset the offset into the array {@code bytes} at which
     *        to start reading the bytes to be set
     * @param len the number of bytes to be written to the {@code BLOB}
     *        value from the array of bytes {@code bytes}
     * @return the number of bytes written
     * @throws SQLException if there is an error accessing the
     *         {@code BLOB} value or if pos is less than 1
     * @throws SQLFeatureNotSupportedException if the JDBC driver
     *         does not support this method
     * @see #getBytes
     * @since 1.4
     */
    int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException;

    /**
     * Retrieves a stream that can be used to write to the {@code BLOB}
     * value that this {@code Blob} object represents.  The stream begins
     * at position {@code pos}.
     * The  bytes written to the stream will overwrite the existing bytes
     * in the {@code Blob} object starting at the position
     * {@code pos}.  If the end of the {@code Blob} value is reached
     * while writing to the stream, then the length of the {@code Blob}
     * value will be increased to accommodate the extra bytes.
     * <p>
     * <b>Note:</b> If the value specified for {@code pos}
     * is greater than the length+1 of the {@code BLOB} value then the
     * behavior is undefined. Some JDBC drivers may throw an
     * {@code SQLException} while other drivers may support this
     * operation.
     *
     * @param pos the position in the {@code BLOB} value at which
     *        to start writing; the first position is 1
     * @return a {@code java.io.OutputStream} object to which data can
     *         be written
     * @throws SQLException if there is an error accessing the
     *         {@code BLOB} value or if pos is less than 1
     * @throws SQLFeatureNotSupportedException if the JDBC driver
     *         does not support this method
     * @see #getBinaryStream
     * @since 1.4
     */
    java.io.OutputStream setBinaryStream(long pos) throws SQLException;

    /**
     * Truncates the {@code BLOB} value that this {@code Blob}
     * object represents to be {@code len} bytes in length.
     * <p>
     * <b>Note:</b> If the value specified for {@code pos}
     * is greater than the length+1 of the {@code BLOB} value then the
     * behavior is undefined. Some JDBC drivers may throw an
     * {@code SQLException} while other drivers may support this
     * operation.
     *
     * @param len the length, in bytes, to which the {@code BLOB} value
     *        that this {@code Blob} object represents should be truncated
     * @throws SQLException if there is an error accessing the
     *         {@code BLOB} value or if len is less than 0
     * @throws SQLFeatureNotSupportedException if the JDBC driver
     *         does not support this method
     * @since 1.4
     */
    void truncate(long len) throws SQLException;

    /**
     * This method frees the {@code Blob} object and releases the resources that
     * it holds. The object is invalid once the {@code free}
     * method is called.
     * <p>
     * After {@code free} has been called, any attempt to invoke a
     * method other than {@code free} will result in an {@code SQLException}
     * being thrown.  If {@code free} is called multiple times, the subsequent
     * calls to {@code free} are treated as a no-op.
     *
     * @throws SQLException if an error occurs releasing
     *         the Blob's resources
     * @throws SQLFeatureNotSupportedException if the JDBC driver
     *         does not support this method
     * @since 1.6
     */
    void free() throws SQLException;

    /**
     * Returns an {@code InputStream} object that contains
     * a partial {@code Blob} value, starting with the byte
     * specified by pos, which is length bytes in length.
     *
     * @param pos the offset to the first byte of the partial value to be
     *        retrieved. The first byte in the {@code Blob} is at position 1.
     * @param length the length in bytes of the partial value to be retrieved
     * @return {@code InputStream} through which
     *         the partial {@code Blob} value can be read.
     * @throws SQLException if pos is less than 1 or if pos is greater
     *         than the number of bytes in the {@code Blob} or if
     *         pos + length is greater than the number of bytes
     *         in the {@code Blob}
     *
     * @throws SQLFeatureNotSupportedException if the JDBC driver
     *         does not support this method
     * @since 1.6
     */
    InputStream getBinaryStream(long pos, long length) throws SQLException;
}
