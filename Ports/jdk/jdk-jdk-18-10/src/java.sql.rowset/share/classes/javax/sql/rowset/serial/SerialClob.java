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

package javax.sql.rowset.serial;

import java.sql.*;
import java.io.*;
import java.util.Arrays;

/**
 * A serialized mapping in the Java programming language of an SQL
 * <code>CLOB</code> value.
 * <P>
 * The <code>SerialClob</code> class provides a constructor for creating
 * an instance from a <code>Clob</code> object.  Note that the <code>Clob</code>
 * object should have brought the SQL <code>CLOB</code> value's data over
 * to the client before a <code>SerialClob</code> object
 * is constructed from it.  The data of an SQL <code>CLOB</code> value can
 * be materialized on the client as a stream of Unicode characters.
 * <P>
 * <code>SerialClob</code> methods make it possible to get a substring
 * from a <code>SerialClob</code> object or to locate the start of
 * a pattern of characters.
 *
 * <h2> Thread safety </h2>
 *
 * <p> A SerialClob is not safe for use by multiple concurrent threads.  If a
 * SerialClob is to be used by more than one thread then access to the SerialClob
 * should be controlled by appropriate synchronization.
 *
 * @author Jonathan Bruce
 * @since 1.5
 */
public class SerialClob implements Clob, Serializable, Cloneable {

    /**
     * A serialized array of characters containing the data of the SQL
     * <code>CLOB</code> value that this <code>SerialClob</code> object
     * represents.
     *
     * @serial
     */
    private char buf[];

    /**
     * Internal Clob representation if SerialClob is initialized with a
     * Clob. Null if SerialClob is initialized with a char[].
     */
    @SuppressWarnings("serial")  // Not statically typed as Serializable; checked in writeObject
    private Clob clob;

    /**
     * The length in characters of this <code>SerialClob</code> object's
     * internal array of characters.
     *
     * @serial
     */
    private long len;

    /**
     * The original length in characters of this <code>SerialClob</code>
     * object's internal array of characters.
     *
     * @serial
     */
    private long origLen;

    /**
     * Constructs a <code>SerialClob</code> object that is a serialized version of
     * the given <code>char</code> array.
     * <p>
     * The new <code>SerialClob</code> object is initialized with the data from the
     * <code>char</code> array, thus allowing disconnected <code>RowSet</code>
     * objects to establish a serialized <code>Clob</code> object without touching
     * the data source.
     *
     * @param ch the char array representing the <code>Clob</code> object to be
     *         serialized
     * @throws SerialException if an error occurs during serialization
     * @throws SQLException if a SQL error occurs
     */
    public SerialClob(char ch[]) throws SerialException, SQLException {

        // %%% JMB. Agreed. Add code here to throw a SQLException if no
        // support is available for locatorsUpdateCopy=false
        // Serializing locators is not supported.

        len = ch.length;
        buf = new char[(int)len];
        for (int i = 0; i < len ; i++){
           buf[i] = ch[i];
        }
        origLen = len;
        clob = null;
    }

    /**
     * Constructs a <code>SerialClob</code> object that is a serialized
     * version of the given <code>Clob</code> object.
     * <P>
     * The new <code>SerialClob</code> object is initialized with the
     * data from the <code>Clob</code> object; therefore, the
     * <code>Clob</code> object should have previously brought the
     * SQL <code>CLOB</code> value's data over to the client from
     * the database. Otherwise, the new <code>SerialClob</code> object
     * object will contain no data.
     * <p>
     * Note: The <code>Clob</code> object supplied to this constructor must
     * return non-null for both the <code>Clob.getCharacterStream()</code>
     * and <code>Clob.getAsciiStream</code> methods. This <code>SerialClob</code>
     * constructor cannot serialize a <code>Clob</code> object in this instance
     * and will throw an <code>SQLException</code> object.
     *
     * @param  clob the <code>Clob</code> object from which this
     *     <code>SerialClob</code> object is to be constructed; cannot be null
     * @throws SerialException if an error occurs during serialization
     * @throws SQLException if a SQL error occurs in capturing the CLOB;
     *     if the <code>Clob</code> object is a null; or if either of the
     *     <code>Clob.getCharacterStream()</code> and <code>Clob.getAsciiStream()</code>
     *     methods on the <code>Clob</code> returns a null
     * @see java.sql.Clob
     */
    public SerialClob(Clob clob) throws SerialException, SQLException {

        if (clob == null) {
            throw new SQLException("Cannot instantiate a SerialClob " +
                "object with a null Clob object");
        }
        len = clob.length();
        this.clob = clob;
        buf = new char[(int)len];
        int read = 0;
        int offset = 0;

        try (Reader charStream = clob.getCharacterStream()) {
            if (charStream == null) {
                throw new SQLException("Invalid Clob object. The call to getCharacterStream " +
                    "returned null which cannot be serialized.");
            }

            // Note: get an ASCII stream in order to null-check it,
            // even though we don't do anything with it.
            try (InputStream asciiStream = clob.getAsciiStream()) {
                if (asciiStream == null) {
                    throw new SQLException("Invalid Clob object. The call to getAsciiStream " +
                        "returned null which cannot be serialized.");
                }
            }

            try (Reader reader = new BufferedReader(charStream)) {
                do {
                    read = reader.read(buf, offset, (int)(len - offset));
                    offset += read;
                } while (read > 0);
            }
        } catch (java.io.IOException ex) {
            throw new SerialException("SerialClob: " + ex.getMessage());
        }

        origLen = len;
    }

    /**
     * Retrieves the number of characters in this <code>SerialClob</code>
     * object's array of characters.
     *
     * @return a <code>long</code> indicating the length in characters of this
     *         <code>SerialClob</code> object's array of character
     * @throws SerialException if an error occurs;
     * if {@code free} had previously been called on this object
     */
    public long length() throws SerialException {
        isValid();
        return len;
    }

    /**
     * Returns this <code>SerialClob</code> object's data as a stream
     * of Unicode characters. Unlike the related method, <code>getAsciiStream</code>,
     * a stream is produced regardless of whether the <code>SerialClob</code> object
     * was created with a <code>Clob</code> object or a <code>char</code> array.
     *
     * @return a <code>java.io.Reader</code> object containing this
     *         <code>SerialClob</code> object's data
     * @throws SerialException if an error occurs;
     * if {@code free} had previously been called on this object
     */
    public java.io.Reader getCharacterStream() throws SerialException {
        isValid();
        return (java.io.Reader) new CharArrayReader(buf);
    }

    /**
     * Retrieves the <code>CLOB</code> value designated by this <code>SerialClob</code>
     * object as an ascii stream. This method forwards the <code>getAsciiStream</code>
     * call to the underlying <code>Clob</code> object in the event that this
     * <code>SerialClob</code> object is instantiated with a <code>Clob</code>
     * object. If this <code>SerialClob</code> object is instantiated with
     * a <code>char</code> array, a <code>SerialException</code> object is thrown.
     *
     * @return a <code>java.io.InputStream</code> object containing
     *     this <code>SerialClob</code> object's data
     * @throws SerialException if this {@code SerialClob} object was not
     * instantiated with a <code>Clob</code> object;
     * if {@code free} had previously been called on this object
     * @throws SQLException if there is an error accessing the
     *     <code>CLOB</code> value represented by the <code>Clob</code> object
     * that was used to create this <code>SerialClob</code> object
     */
    public java.io.InputStream getAsciiStream() throws SerialException, SQLException {
        isValid();
        if (this.clob != null) {
            return this.clob.getAsciiStream();
        } else {
            throw new SerialException("Unsupported operation. SerialClob cannot " +
                "return a the CLOB value as an ascii stream, unless instantiated " +
                "with a fully implemented Clob object.");
        }
    }

    /**
     * Returns a copy of the substring contained in this
     * <code>SerialClob</code> object, starting at the given position
     * and continuing for the specified number or characters.
     *
     * @param pos the position of the first character in the substring
     *            to be copied; the first character of the
     *            <code>SerialClob</code> object is at position
     *            <code>1</code>; must not be less than <code>1</code>,
     *            and the sum of the starting position and the length
     *            of the substring must be less than the length of this
     *            <code>SerialClob</code> object
     * @param length the number of characters in the substring to be
     *               returned; must not be greater than the length of
     *               this <code>SerialClob</code> object, and the
     *               sum of the starting position and the length
     *               of the substring must be less than the length of this
     *               <code>SerialClob</code> object
     * @return a <code>String</code> object containing a substring of
     *         this <code>SerialClob</code> object beginning at the
     *         given position and containing the specified number of
     *         consecutive characters
     * @throws SerialException if either of the arguments is out of bounds;
     * if {@code free} had previously been called on this object
     */
    public String getSubString(long pos, int length) throws SerialException {

        isValid();
        if (pos < 1 || pos > this.length()) {
            throw new SerialException("Invalid position in SerialClob object set");
        }

        if ((pos-1) + length > this.length()) {
            throw new SerialException("Invalid position and substring length");
        }

        try {
            return new String(buf, (int)pos - 1, length);

        } catch (StringIndexOutOfBoundsException e) {
            throw new SerialException("StringIndexOutOfBoundsException: " +
                e.getMessage());
        }

    }

    /**
     * Returns the position in this <code>SerialClob</code> object
     * where the given <code>String</code> object begins, starting
     * the search at the specified position. This method returns
     * <code>-1</code> if the pattern is not found.
     *
     * @param searchStr the <code>String</code> object for which to
     *                  search
     * @param start the position in this <code>SerialClob</code> object
     *         at which to start the search; the first position is
     *         <code>1</code>; must not be less than <code>1</code> nor
     *         greater than the length of this <code>SerialClob</code> object
     * @return the position at which the given <code>String</code> object
     *         begins, starting the search at the specified position;
     *         <code>-1</code> if the given <code>String</code> object is
     *         not found or the starting position is out of bounds; position
     *         numbering for the return value starts at <code>1</code>
     * @throws SerialException  if the {@code free} method had been
     * previously called on this object
     * @throws SQLException if there is an error accessing the Clob value
     *         from the database.
     */
    public long position(String searchStr, long start)
        throws SerialException, SQLException {
        isValid();
        if (start < 1 || start > len) {
            return -1;
        }

        char pattern[] = searchStr.toCharArray();

        int pos = (int)start-1;
        int i = 0;
        long patlen = pattern.length;

        while (pos < len) {
            if (pattern[i] == buf[pos]) {
                if (i + 1 == patlen) {
                    return (pos + 1) - (patlen - 1);
                }
                i++; pos++; // increment pos, and i

            } else if (pattern[i] != buf[pos]) {
                pos++; // increment pos only
            }
        }
        return -1; // not found
    }

    /**
     * Returns the position in this <code>SerialClob</code> object
     * where the given <code>Clob</code> signature begins, starting
     * the search at the specified position. This method returns
     * <code>-1</code> if the pattern is not found.
     *
     * @param searchStr the <code>Clob</code> object for which to search
     * @param start the position in this <code>SerialClob</code> object
     *        at which to begin the search; the first position is
     *         <code>1</code>; must not be less than <code>1</code> nor
     *         greater than the length of this <code>SerialClob</code> object
     * @return the position at which the given <code>Clob</code>
     *         object begins in this <code>SerialClob</code> object,
     *         at or after the specified starting position
     * @throws SerialException if an error occurs locating the Clob signature;
     * if the {@code free} method had been previously called on this object
     * @throws SQLException if there is an error accessing the Clob value
     *         from the database
     */
    public long position(Clob searchStr, long start)
        throws SerialException, SQLException {
        isValid();
        return position(searchStr.getSubString(1,(int)searchStr.length()), start);
    }

    /**
     * Writes the given Java <code>String</code> to the <code>CLOB</code>
     * value that this <code>SerialClob</code> object represents, at the position
     * <code>pos</code>.
     *
     * @param pos the position at which to start writing to the <code>CLOB</code>
     *         value that this <code>SerialClob</code> object represents; the first
     *         position is <code>1</code>; must not be less than <code>1</code> nor
     *         greater than the length of this <code>SerialClob</code> object
     * @param str the string to be written to the <code>CLOB</code>
     *        value that this <code>SerialClob</code> object represents
     * @return the number of characters written
     * @throws SerialException if there is an error accessing the
     *     <code>CLOB</code> value; if an invalid position is set; if an
     *     invalid offset value is set; if number of bytes to be written
     *     is greater than the <code>SerialClob</code> length; or the combined
     *     values of the length and offset is greater than the Clob buffer;
     * if the {@code free} method had been previously called on this object
     */
    public int setString(long pos, String str) throws SerialException {
        return (setString(pos, str, 0, str.length()));
    }

    /**
     * Writes <code>len</code> characters of <code>str</code>, starting
     * at character <code>offset</code>, to the <code>CLOB</code> value
     * that this <code>Clob</code> represents.
     *
     * @param pos the position at which to start writing to the <code>CLOB</code>
     *         value that this <code>SerialClob</code> object represents; the first
     *         position is <code>1</code>; must not be less than <code>1</code> nor
     *         greater than the length of this <code>SerialClob</code> object
     * @param str the string to be written to the <code>CLOB</code>
     *        value that this <code>Clob</code> object represents
     * @param offset the offset into <code>str</code> to start reading
     *        the characters to be written
     * @param length the number of characters to be written
     * @return the number of characters written
     * @throws SerialException if there is an error accessing the
     *     <code>CLOB</code> value; if an invalid position is set; if an
     *     invalid offset value is set; if number of bytes to be written
     *     is greater than the <code>SerialClob</code> length; or the combined
     *     values of the length and offset is greater than the Clob buffer;
     * if the {@code free} method had been previously called on this object
     */
    public int setString(long pos, String str, int offset, int length)
        throws SerialException {
        isValid();
        String temp = str.substring(offset);
        char cPattern[] = temp.toCharArray();

        if (offset < 0 || offset > str.length()) {
            throw new SerialException("Invalid offset in byte array set");
        }

        if (pos < 1 || pos > this.length()) {
            throw new SerialException("Invalid position in Clob object set");
        }

        if ((long)(length) > origLen) {
            throw new SerialException("Buffer is not sufficient to hold the value");
        }

        if ((length + offset) > str.length()) {
            // need check to ensure length + offset !> bytes.length
            throw new SerialException("Invalid OffSet. Cannot have combined offset " +
                " and length that is greater that the Blob buffer");
        }

        int i = 0;
        pos--;  //values in the array are at position one less
        while ( i < length || (offset + i +1) < (str.length() - offset ) ) {
            this.buf[(int)pos + i ] = cPattern[offset + i ];
            i++;
        }
        return i;
    }

    /**
     * Retrieves a stream to be used to write Ascii characters to the
     * <code>CLOB</code> value that this <code>SerialClob</code> object represents,
     * starting at position <code>pos</code>. This method forwards the
     * <code>setAsciiStream()</code> call to the underlying <code>Clob</code> object in
     * the event that this <code>SerialClob</code> object is instantiated with a
     * <code>Clob</code> object. If this <code>SerialClob</code> object is instantiated
     *  with a <code>char</code> array, a <code>SerialException</code> object is thrown.
     *
     * @param pos the position at which to start writing to the
     *        <code>CLOB</code> object
     * @return the stream to which ASCII encoded characters can be written
     * @throws SerialException if SerialClob is not instantiated with a
     *     Clob object;
     * if the {@code free} method had been previously called on this object
     * @throws SQLException if there is an error accessing the
     *     <code>CLOB</code> value
     * @see #getAsciiStream
     */
    public java.io.OutputStream setAsciiStream(long pos)
        throws SerialException, SQLException {
        isValid();
         if (this.clob != null) {
             return this.clob.setAsciiStream(pos);
         } else {
             throw new SerialException("Unsupported operation. SerialClob cannot " +
                "return a writable ascii stream\n unless instantiated with a Clob object " +
                "that has a setAsciiStream() implementation");
         }
    }

    /**
     * Retrieves a stream to be used to write a stream of Unicode characters
     * to the <code>CLOB</code> value that this <code>SerialClob</code> object
     * represents, at position <code>pos</code>. This method forwards the
     * <code>setCharacterStream()</code> call to the underlying <code>Clob</code>
     * object in the event that this <code>SerialClob</code> object is instantiated with a
     * <code>Clob</code> object. If this <code>SerialClob</code> object is instantiated with
     * a <code>char</code> array, a <code>SerialException</code> is thrown.
     *
     * @param  pos the position at which to start writing to the
     *        <code>CLOB</code> value
     *
     * @return a stream to which Unicode encoded characters can be written
     * @throws SerialException if the SerialClob is not instantiated with
     *     a Clob object;
     * if the {@code free} method had been previously called on this object
     * @throws SQLException if there is an error accessing the
     *            <code>CLOB</code> value
     * @see #getCharacterStream
     */
    public java.io.Writer setCharacterStream(long pos)
        throws SerialException, SQLException {
        isValid();
        if (this.clob != null) {
            return this.clob.setCharacterStream(pos);
        } else {
            throw new SerialException("Unsupported operation. SerialClob cannot " +
                "return a writable character stream\n unless instantiated with a Clob object " +
                "that has a setCharacterStream implementation");
        }
    }

    /**
     * Truncates the <code>CLOB</code> value that this <code>SerialClob</code>
     * object represents so that it has a length of <code>len</code>
     * characters.
     * <p>
     * Truncating a <code>SerialClob</code> object to length 0 has the effect of
     * clearing its contents.
     *
     * @param length the length, in bytes, to which the <code>CLOB</code>
     *        value should be truncated
     * @throws SerialException if there is an error accessing the
     *        <code>CLOB</code> value;
     * if the {@code free} method had been previously called on this object
     */
    public void truncate(long length) throws SerialException {
        isValid();
        if (length > len) {
           throw new SerialException
              ("Length more than what can be truncated");
        } else {
             len = length;
             // re-size the buffer

             if (len == 0) {
                buf = new char[] {};
             } else {
                buf = (this.getSubString(1, (int)len)).toCharArray();
             }
        }
    }


    /**
     * Returns a {@code Reader} object that contains a partial
     * {@code SerialClob} value, starting
     * with the character specified by pos, which is length characters in length.
     *
     * @param pos the offset to the first character of the partial value to
     * be retrieved.  The first character in the {@code SerialClob} is at position 1.
     * @param length the length in characters of the partial value to be retrieved.
     * @return {@code Reader} through which the partial {@code SerialClob}
     * value can be read.
     * @throws SQLException if pos is less than 1 or if pos is greater than the
     * number of characters in the {@code SerialClob} or if pos + length
     * is greater than the number of characters in the {@code SerialClob};
     * @throws SerialException if the {@code free} method had been previously
     * called on this object
     * @since 1.6
     */
    public Reader getCharacterStream(long pos, long length) throws SQLException {
        isValid();
        if (pos < 1 || pos > len) {
            throw new SerialException("Invalid position in Clob object set");
        }

        if ((pos-1) + length > len) {
            throw new SerialException("Invalid position and substring length");
        }
        if (length <= 0) {
            throw new SerialException("Invalid length specified");
        }
        return new CharArrayReader(buf, (int)pos, (int)length);
    }

    /**
     * This method frees the {@code SerialClob} object and releases the
     * resources that it holds.
     * The object is invalid once the {@code free} method is called.
     * <p>
     * If {@code free} is called multiple times, the subsequent
     * calls to {@code free} are treated as a no-op.
     * </P>
     * @throws SQLException if an error occurs releasing
     * the Clob's resources
     * @since 1.6
     */
    public void free() throws SQLException {
        if (buf != null) {
            buf = null;
            if (clob != null) {
                clob.free();
            }
            clob = null;
        }
    }

    /**
     * Compares this SerialClob to the specified object.  The result is {@code
     * true} if and only if the argument is not {@code null} and is a {@code
     * SerialClob} object that represents the same sequence of characters as this
     * object.
     *
     * @param  obj The object to compare this {@code SerialClob} against
     *
     * @return  {@code true} if the given object represents a {@code SerialClob}
     *          equivalent to this SerialClob, {@code false} otherwise
     *
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SerialClob) {
            SerialClob sc = (SerialClob)obj;
            if (this.len == sc.len) {
                return Arrays.equals(buf, sc.buf);
            }
        }
        return false;
    }

    /**
     * Returns a hash code for this {@code SerialClob}.
     * @return  a hash code value for this object.
     */
    public int hashCode() {
       return ((31 + Arrays.hashCode(buf)) * 31 + (int)len) * 31 + (int)origLen;
    }

    /**
     * Returns a clone of this {@code SerialClob}. The copy will contain a
     * reference to a clone of the internal character array, not a reference
     * to the original internal character array of this {@code SerialClob} object.
     * The underlying {@code Clob} object will be set to null.
     *
     * @return  a clone of this SerialClob
     */
    public Object clone() {
        try {
            SerialClob sc = (SerialClob) super.clone();
            sc.buf = (buf != null) ? Arrays.copyOf(buf, (int)len) : null;
            sc.clob = null;
            return sc;
        } catch (CloneNotSupportedException ex) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
    }

    /**
     * readObject is called to restore the state of the SerialClob from
     * a stream.
     * @param s the {@code ObjectInputStream} to read from.
     *
     * @throws  ClassNotFoundException if the class of a serialized object
     *          could not be found.
     * @throws  IOException if an I/O error occurs.
     */
    private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {

        ObjectInputStream.GetField fields = s.readFields();
       char[] tmp = (char[])fields.get("buf", null);
       if (tmp == null)
           throw new InvalidObjectException("buf is null and should not be!");
       buf = tmp.clone();
       len = fields.get("len", 0L);
       if (buf.length != len)
           throw new InvalidObjectException("buf is not the expected size");
       origLen = fields.get("origLen", 0L);
       clob = (Clob) fields.get("clob", null);
    }

    /**
     * writeObject is called to save the state of the SerialClob
     * to a stream.
     * @param s the {@code ObjectOutputStream} to write to.
     * @throws  IOException if an I/O error occurs.
     */
    private void writeObject(ObjectOutputStream s)
            throws IOException {

        ObjectOutputStream.PutField fields = s.putFields();
        fields.put("buf", buf);
        fields.put("len", len);
        fields.put("origLen", origLen);
        // Note: this check to see if it is an instance of Serializable
        // is for backwards compatibility
        fields.put("clob", clob instanceof Serializable ? clob : null);
        s.writeFields();
    }

    /**
     * Check to see if this object had previously had its {@code free} method
     * called
     *
     * @throws SerialException
     */
    private void isValid() throws SerialException {
        if (buf == null) {
            throw new SerialException("Error: You cannot call a method on a "
                    + "SerialClob instance once free() has been called.");
        }
    }

    /**
     * The identifier that assists in the serialization of this {@code SerialClob}
     * object.
     */
    static final long serialVersionUID = -1662519690087375313L;
}
