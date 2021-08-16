/*
 * Copyright (c) 2003, 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Arrays;
import java.util.Map;
import sun.reflect.misc.ReflectUtil;

/**
 * An input stream used for custom mapping user-defined types (UDTs).
 * An <code>SQLInputImpl</code> object is an input stream that contains a
 * stream of values that are the attributes of a UDT.
 * <p>
 * This class is used by the driver behind the scenes when the method
 * <code>getObject</code> is called on an SQL structured or distinct type
 * that has a custom mapping; a programmer never invokes
 * <code>SQLInputImpl</code> methods directly. They are provided here as a
 * convenience for those who write <code>RowSet</code> implementations.
 * <P>
 * The <code>SQLInputImpl</code> class provides a set of
 * reader methods analogous to the <code>ResultSet</code> getter
 * methods.  These methods make it possible to read the values in an
 * <code>SQLInputImpl</code> object.
 * <P>
 * The method <code>wasNull</code> is used to determine whether the
 * the last value read was SQL <code>NULL</code>.
 * <P>When the method <code>getObject</code> is called with an
 * object of a class implementing the interface <code>SQLData</code>,
 * the JDBC driver calls the method <code>SQLData.getSQLType</code>
 * to determine the SQL type of the UDT being custom mapped. The driver
 * creates an instance of <code>SQLInputImpl</code>, populating it with the
 * attributes of the UDT.  The driver then passes the input
 * stream to the method <code>SQLData.readSQL</code>, which in turn
 * calls the <code>SQLInputImpl</code> reader methods
 * to read the attributes from the input stream.
 * @since 1.5
 * @see java.sql.SQLData
 */
public class SQLInputImpl implements SQLInput {

    /**
     * <code>true</code> if the last value returned was <code>SQL NULL</code>;
     * <code>false</code> otherwise.
     */
    private boolean lastValueWasNull;

    /**
     * The current index into the array of SQL structured type attributes
     * that will be read from this <code>SQLInputImpl</code> object and
     * mapped to the fields of a class in the Java programming language.
     */
    private int idx;

    /**
     * The array of attributes to be read from this stream.  The order
     * of the attributes is the same as the order in which they were
     * listed in the SQL definition of the UDT.
     */
    private Object attrib[];

    /**
     * The type map to use when the method <code>readObject</code>
     * is invoked. This is a <code>java.util.Map</code> object in which
     * there may be zero or more entries.  Each entry consists of the
     * fully qualified name of a UDT (the value to be mapped) and the
     * <code>Class</code> object for a class that implements
     * <code>SQLData</code> (the Java class that defines how the UDT
     * will be mapped).
     */
    private Map<String,Class<?>> map;


    /**
     * Creates an <code>SQLInputImpl</code> object initialized with the
     * given array of attributes and the given type map. If any of the
     * attributes is a UDT whose name is in an entry in the type map,
     * the attribute will be mapped according to the corresponding
     * <code>SQLData</code> implementation.
     *
     * @param attributes an array of <code>Object</code> instances in which
     *        each element is an attribute of a UDT. The order of the
     *        attributes in the array is the same order in which
     *        the attributes were defined in the UDT definition.
     * @param map a <code>java.util.Map</code> object containing zero or more
     *        entries, with each entry consisting of 1) a <code>String</code>
     *        giving the fully
     *        qualified name of the UDT and 2) the <code>Class</code> object
     *        for the <code>SQLData</code> implementation that defines how
     *        the UDT is to be mapped
     * @throws SQLException if the <code>attributes</code> or the <code>map</code>
     *        is a <code>null</code> value
     */

    public SQLInputImpl(Object[] attributes, Map<String,Class<?>> map)
        throws SQLException
    {
        if ((attributes == null) || (map == null)) {
            throw new SQLException("Cannot instantiate a SQLInputImpl " +
            "object with null parameters");
        }
        // assign our local reference to the attribute stream
        attrib = Arrays.copyOf(attributes, attributes.length);
        // init the index point before the head of the stream
        idx = -1;
        // set the map
        this.map = map;
    }


    /**
     * Retrieves the next attribute in this <code>SQLInputImpl</code> object
     * as an <code>Object</code> in the Java programming language.
     *
     * @return the next value in the input stream
     *         as an <code>Object</code> in the Java programming language
     * @throws SQLException if the read position is located at an invalid
     *         position or if there are no further values in the stream
     */
    private Object getNextAttribute() throws SQLException {
        if (++idx >= attrib.length) {
            throw new SQLException("SQLInputImpl exception: Invalid read " +
                                   "position");
        } else {
            lastValueWasNull = attrib[idx] == null;
            return attrib[idx];
        }
    }


    //================================================================
    // Methods for reading attributes from the stream of SQL data.
    // These methods correspond to the column-accessor methods of
    // java.sql.ResultSet.
    //================================================================

    /**
     * Retrieves the next attribute in this <code>SQLInputImpl</code> object as
     * a <code>String</code> in the Java programming language.
     * <p>
     * This method does not perform type-safe checking to determine if the
     * returned type is the expected type; this responsibility is delegated
     * to the UDT mapping as defined by a <code>SQLData</code>
     * implementation.
     *
     * @return the next attribute in this <code>SQLInputImpl</code> object;
     *     if the value is <code>SQL NULL</code>, return <code>null</code>
     * @throws SQLException if the read position is located at an invalid
     *     position or if there are no further values in the stream.
     */
    public String readString() throws SQLException {
        return  (String)getNextAttribute();
    }

    /**
     * Retrieves the next attribute in this <code>SQLInputImpl</code> object as
     * a <code>boolean</code> in the Java programming language.
     * <p>
     * This method does not perform type-safe checking to determine if the
     * returned type is the expected type; this responsibility is delegated
     * to the UDT mapping as defined by a <code>SQLData</code>
     * implementation.
     *
     * @return the next attribute in this <code>SQLInputImpl</code> object;
     *     if the value is <code>SQL NULL</code>, return <code>null</code>
     * @throws SQLException if the read position is located at an invalid
     *     position or if there are no further values in the stream.
     */
    public boolean readBoolean() throws SQLException {
        Boolean attrib = (Boolean)getNextAttribute();
        return  (attrib == null) ? false : attrib.booleanValue();
    }

    /**
     * Retrieves the next attribute in this <code>SQLInputImpl</code> object as
     * a <code>byte</code> in the Java programming language.
     * <p>
     * This method does not perform type-safe checking to determine if the
     * returned type is the expected type; this responsibility is delegated
     * to the UDT mapping as defined by a <code>SQLData</code>
     * implementation.
     *
     * @return the next attribute in this <code>SQLInputImpl</code> object;
     *     if the value is <code>SQL NULL</code>, return <code>null</code>
     * @throws SQLException if the read position is located at an invalid
     *     position or if there are no further values in the stream
     */
    public byte readByte() throws SQLException {
        Byte attrib = (Byte)getNextAttribute();
        return  (attrib == null) ? 0 : attrib.byteValue();
    }

    /**
     * Retrieves the next attribute in this <code>SQLInputImpl</code> object
     * as a <code>short</code> in the Java programming language.
     * <P>
     * This method does not perform type-safe checking to determine if the
     * returned type is the expected type; this responsibility is delegated
     * to the UDT mapping as defined by a <code>SQLData</code> implementation.
     *
     * @return the next attribute in this <code>SQLInputImpl</code> object;
     *       if the value is <code>SQL NULL</code>, return <code>null</code>
     * @throws SQLException if the read position is located at an invalid
     *       position or if there are no more values in the stream
     */
    public short readShort() throws SQLException {
        Short attrib = (Short)getNextAttribute();
        return (attrib == null) ? 0 : attrib.shortValue();
    }

    /**
     * Retrieves the next attribute in this <code>SQLInputImpl</code> object
     * as an <code>int</code> in the Java programming language.
     * <P>
     * This method does not perform type-safe checking to determine if the
     * returned type is the expected type; this responsibility is delegated
     * to the UDT mapping as defined by a <code>SQLData</code> implementation.
     *
     * @return the next attribute in this <code>SQLInputImpl</code> object;
     *       if the value is <code>SQL NULL</code>, return <code>null</code>
     * @throws SQLException if the read position is located at an invalid
     *       position or if there are no more values in the stream
     */
    public int readInt() throws SQLException {
        Integer attrib = (Integer)getNextAttribute();
        return (attrib == null) ? 0 : attrib.intValue();
    }

    /**
     * Retrieves the next attribute in this <code>SQLInputImpl</code> object
     * as a <code>long</code> in the Java programming language.
     * <P>
     * This method does not perform type-safe checking to determine if the
     * returned type is the expected type; this responsibility is delegated
     * to the UDT mapping as defined by a <code>SQLData</code> implementation.
     *
     * @return the next attribute in this <code>SQLInputImpl</code> object;
     *       if the value is <code>SQL NULL</code>, return <code>null</code>
     * @throws SQLException if the read position is located at an invalid
     *       position or if there are no more values in the stream
     */
    public long readLong() throws SQLException {
        Long attrib = (Long)getNextAttribute();
        return (attrib == null) ? 0 : attrib.longValue();
    }

    /**
     * Retrieves the next attribute in this <code>SQLInputImpl</code> object
     * as a <code>float</code> in the Java programming language.
     * <P>
     * This method does not perform type-safe checking to determine if the
     * returned type is the expected type; this responsibility is delegated
     * to the UDT mapping as defined by a <code>SQLData</code> implementation.
     *
     * @return the next attribute in this <code>SQLInputImpl</code> object;
     *       if the value is <code>SQL NULL</code>, return <code>null</code>
     * @throws SQLException if the read position is located at an invalid
     *       position or if there are no more values in the stream
     */
    public float readFloat() throws SQLException {
        Float attrib = (Float)getNextAttribute();
        return (attrib == null) ? 0 : attrib.floatValue();
    }

    /**
     * Retrieves the next attribute in this <code>SQLInputImpl</code> object
     * as a <code>double</code> in the Java programming language.
     * <P>
     * This method does not perform type-safe checking to determine if the
     * returned type is the expected type; this responsibility is delegated
     * to the UDT mapping as defined by a <code>SQLData</code> implementation.
     *
     * @return the next attribute in this <code>SQLInputImpl</code> object;
     *       if the value is <code>SQL NULL</code>, return <code>null</code>
     * @throws SQLException if the read position is located at an invalid
     *       position or if there are no more values in the stream
     */
    public double readDouble() throws SQLException {
        Double attrib = (Double)getNextAttribute();
        return (attrib == null)  ? 0 :  attrib.doubleValue();
    }

    /**
     * Retrieves the next attribute in this <code>SQLInputImpl</code> object
     * as a <code>java.math.BigDecimal</code>.
     * <P>
     * This method does not perform type-safe checking to determine if the
     * returned type is the expected type; this responsibility is delegated
     * to the UDT mapping as defined by a <code>SQLData</code> implementation.
     *
     * @return the next attribute in this <code>SQLInputImpl</code> object;
     *       if the value is <code>SQL NULL</code>, return <code>null</code>
     * @throws SQLException if the read position is located at an invalid
     *       position or if there are no more values in the stream
     */
    public java.math.BigDecimal readBigDecimal() throws SQLException {
        return (java.math.BigDecimal)getNextAttribute();
    }

    /**
     * Retrieves the next attribute in this <code>SQLInputImpl</code> object
     * as an array of bytes.
     * <p>
     * This method does not perform type-safe checking to determine if the
     * returned type is the expected type; this responsibility is delegated
     * to the UDT mapping as defined by a <code>SQLData</code> implementation.
     *
     * @return the next attribute in this <code>SQLInputImpl</code> object;
     *       if the value is <code>SQL NULL</code>, return <code>null</code>
     * @throws SQLException if the read position is located at an invalid
     *       position or if there are no more values in the stream
     */
    public byte[] readBytes() throws SQLException {
        return (byte[])getNextAttribute();
    }

    /**
     * Retrieves the next attribute in this <code>SQLInputImpl</code> as
     * a <code>java.sql.Date</code> object.
     * <P>
     * This method does not perform type-safe checking to determine if the
     * returned type is the expected type; this responsibility is delegated
     * to the UDT mapping as defined by a <code>SQLData</code> implementation.
     *
     * @return the next attribute in this <code>SQLInputImpl</code> object;
     *       if the value is <code>SQL NULL</code>, return <code>null</code>
     * @throws SQLException if the read position is located at an invalid
     *       position or if there are no more values in the stream
     */
    public java.sql.Date readDate() throws SQLException {
        return (java.sql.Date)getNextAttribute();
    }

    /**
     * Retrieves the next attribute in this <code>SQLInputImpl</code> object as
     * a <code>java.sql.Time</code> object.
     * <P>
     * This method does not perform type-safe checking to determine if the
     * returned type is the expected type as this responsibility is delegated
     * to the UDT mapping as implemented by a <code>SQLData</code>
     * implementation.
     *
     * @return the attribute; if the value is <code>SQL NULL</code>, return
     * <code>null</code>
     * @throws SQLException if the read position is located at an invalid
     * position; or if there are no further values in the stream.
     */
    public java.sql.Time readTime() throws SQLException {
        return (java.sql.Time)getNextAttribute();
    }

    /**
     * Retrieves the next attribute in this <code>SQLInputImpl</code> object as
     * a <code>java.sql.Timestamp</code> object.
     *
     * @return the attribute; if the value is <code>SQL NULL</code>, return
     * <code>null</code>
     * @throws SQLException if the read position is located at an invalid
     * position; or if there are no further values in the stream.
     */
    public java.sql.Timestamp readTimestamp() throws SQLException {
        return (java.sql.Timestamp)getNextAttribute();
    }

    /**
     * Retrieves the next attribute in this <code>SQLInputImpl</code> object
     * as a stream of Unicode characters.
     * <P>
     * This method does not perform type-safe checking to determine if the
     * returned type is the expected type as this responsibility is delegated
     * to the UDT mapping as implemented by a <code>SQLData</code>
     * implementation.
     *
     * @return the attribute; if the value is <code>SQL NULL</code>, return <code>null</code>
     * @throws SQLException if the read position is located at an invalid
     * position; or if there are no further values in the stream.
     */
    public java.io.Reader readCharacterStream() throws SQLException {
        return (java.io.Reader)getNextAttribute();
    }

    /**
     * Returns the next attribute in this <code>SQLInputImpl</code> object
     * as a stream of ASCII characters.
     * <P>
     * This method does not perform type-safe checking to determine if the
     * returned type is the expected type as this responsibility is delegated
     * to the UDT mapping as implemented by a <code>SQLData</code>
     * implementation.
     *
     * @return the attribute; if the value is <code>SQL NULL</code>,
     * return <code>null</code>
     * @throws SQLException if the read position is located at an invalid
     * position; or if there are no further values in the stream.
     */
    public java.io.InputStream readAsciiStream() throws SQLException {
        return (java.io.InputStream)getNextAttribute();
    }

    /**
     * Returns the next attribute in this <code>SQLInputImpl</code> object
     * as a stream of uninterpreted bytes.
     * <P>
     * This method does not perform type-safe checking to determine if the
     * returned type is the expected type as this responsibility is delegated
     * to the UDT mapping as implemented by a <code>SQLData</code>
     * implementation.
     *
     * @return the attribute; if the value is <code>SQL NULL</code>, return
     * <code>null</code>
     * @throws SQLException if the read position is located at an invalid
     * position; or if there are no further values in the stream.
     */
    public java.io.InputStream readBinaryStream() throws SQLException {
        return (java.io.InputStream)getNextAttribute();
    }

    //================================================================
    // Methods for reading items of SQL user-defined types from the stream.
    //================================================================

    /**
     * Retrieves the value at the head of this <code>SQLInputImpl</code>
     * object as an <code>Object</code> in the Java programming language.  The
     * actual type of the object returned is determined by the default
     * mapping of SQL types to types in the Java programming language unless
     * there is a custom mapping, in which case the type of the object
     * returned is determined by this stream's type map.
     * <P>
     * The JDBC technology-enabled driver registers a type map with the stream
     * before passing the stream to the application.
     * <P>
     * When the datum at the head of the stream is an SQL <code>NULL</code>,
     * this method returns <code>null</code>.  If the datum is an SQL
     * structured or distinct type with a custom mapping, this method
     * determines the SQL type of the datum at the head of the stream,
     * constructs an object of the appropriate class, and calls the method
     * <code>SQLData.readSQL</code> on that object. The <code>readSQL</code>
     * method then calls the appropriate <code>SQLInputImpl.readXXX</code>
     * methods to retrieve the attribute values from the stream.
     *
     * @return the value at the head of the stream as an <code>Object</code>
     *         in the Java programming language; <code>null</code> if
     *         the value is SQL <code>NULL</code>
     * @throws SQLException if the read position is located at an invalid
     * position; or if there are no further values in the stream.
     */
    public Object readObject() throws SQLException {
        Object attrib = getNextAttribute();
        if (attrib instanceof Struct) {
            Struct s = (Struct)attrib;
            // look up the class in the map
            Class<?> c = map.get(s.getSQLTypeName());
            if (c != null) {
                // create new instance of the class
                SQLData obj = null;
                try {
                    ReflectUtil.checkPackageAccess(c);
                    @SuppressWarnings("deprecation")
                    Object tmp = c.newInstance();
                    obj = (SQLData)tmp;
                } catch (Exception ex) {
                    throw new SQLException("Unable to Instantiate: ", ex);
                }
                // get the attributes from the struct
                Object attribs[] = s.getAttributes(map);
                // create the SQLInput "stream"
                SQLInputImpl sqlInput = new SQLInputImpl(attribs, map);
                // read the values...
                obj.readSQL(sqlInput, s.getSQLTypeName());
                return obj;
            }
        }
        return attrib;
    }

    /**
     * Retrieves the value at the head of this <code>SQLInputImpl</code> object
     * as a <code>Ref</code> object in the Java programming language.
     *
     * @return a <code>Ref</code> object representing the SQL
     *         <code>REF</code> value at the head of the stream; if the value
     *         is <code>SQL NULL</code> return <code>null</code>
     * @throws SQLException if the read position is located at an invalid
     *         position; or if there are no further values in the stream.
     */
    public Ref readRef() throws SQLException {
        return (Ref)getNextAttribute();
    }

    /**
     * Retrieves the <code>BLOB</code> value at the head of this
     * <code>SQLInputImpl</code> object as a <code>Blob</code> object
     * in the Java programming language.
     * <P>
     * This method does not perform type-safe checking to determine if the
     * returned type is the expected type as this responsibility is delegated
     * to the UDT mapping as implemented by a <code>SQLData</code>
     * implementation.
     *
     * @return a <code>Blob</code> object representing the SQL
     *         <code>BLOB</code> value at the head of this stream;
     *         if the value is <code>SQL NULL</code>, return
     *         <code>null</code>
     * @throws SQLException if the read position is located at an invalid
     * position; or if there are no further values in the stream.
     */
    public Blob readBlob() throws SQLException {
        return (Blob)getNextAttribute();
    }

    /**
     * Retrieves the <code>CLOB</code> value at the head of this
     * <code>SQLInputImpl</code> object as a <code>Clob</code> object
     * in the Java programming language.
     * <P>
     * This method does not perform type-safe checking to determine if the
     * returned type is the expected type as this responsibility is delegated
     * to the UDT mapping as implemented by a <code>SQLData</code>
     * implementation.
     *
     * @return a <code>Clob</code> object representing the SQL
     *         <code>CLOB</code> value at the head of the stream;
     *         if the value is <code>SQL NULL</code>, return
     *         <code>null</code>
     * @throws SQLException if the read position is located at an invalid
     * position; or if there are no further values in the stream.
     */
    public Clob readClob() throws SQLException {
        return (Clob)getNextAttribute();
    }

    /**
     * Reads an SQL <code>ARRAY</code> value from the stream and
     * returns it as an <code>Array</code> object in the Java programming
     * language.
     * <P>
     * This method does not perform type-safe checking to determine if the
     * returned type is the expected type as this responsibility is delegated
     * to the UDT mapping as implemented by a <code>SQLData</code>
     * implementation.
     *
     * @return an <code>Array</code> object representing the SQL
     *         <code>ARRAY</code> value at the head of the stream; *
     *         if the value is <code>SQL NULL</code>, return
     *         <code>null</code>
     * @throws SQLException if the read position is located at an invalid
     * position; or if there are no further values in the stream.

     */
    public Array readArray() throws SQLException {
        return (Array)getNextAttribute();
    }

    /**
     * Ascertains whether the last value read from this
     * <code>SQLInputImpl</code> object was <code>null</code>.
     *
     * @return <code>true</code> if the SQL value read most recently was
     *         <code>null</code>; otherwise, <code>false</code>; by default it
     *         will return false
     * @throws SQLException if an error occurs determining the last value
     *         read was a <code>null</code> value or not;
     */
    public boolean wasNull() throws SQLException {
        return lastValueWasNull;
    }

    /**
     * Reads an SQL <code>DATALINK</code> value from the stream and
     * returns it as an <code>URL</code> object in the Java programming
     * language.
     * <P>
     * This method does not perform type-safe checking to determine if the
     * returned type is the expected type as this responsibility is delegated
     * to the UDT mapping as implemented by a <code>SQLData</code>
     * implementation.
     *
     * @return an <code>URL</code> object representing the SQL
     *         <code>DATALINK</code> value at the head of the stream; *
     *         if the value is <code>SQL NULL</code>, return
     *         <code>null</code>
     * @throws SQLException if the read position is located at an invalid
     * position; or if there are no further values in the stream.
     */
    public java.net.URL readURL() throws SQLException {
        return (java.net.URL)getNextAttribute();
    }

    //---------------------------- JDBC 4.0 -------------------------

    /**
     * Reads an SQL <code>NCLOB</code> value from the stream and returns it as a
     * <code>Clob</code> object in the Java programming language.
     *
     * @return a <code>NClob</code> object representing data of the SQL <code>NCLOB</code> value
     * at the head of the stream; <code>null</code> if the value read is
     * SQL <code>NULL</code>
     * @exception SQLException if a database access error occurs
     * @since 1.6
     */
     public NClob readNClob() throws SQLException {
        return (NClob)getNextAttribute();
     }

    /**
     * Reads the next attribute in the stream and returns it as a <code>String</code>
     * in the Java programming language. It is intended for use when
     * accessing  <code>NCHAR</code>,<code>NVARCHAR</code>
     * and <code>LONGNVARCHAR</code> columns.
     *
     * @return the attribute; if the value is SQL <code>NULL</code>, returns <code>null</code>
     * @exception SQLException if a database access error occurs
     * @since 1.6
     */
    public String readNString() throws SQLException {
        return (String)getNextAttribute();
    }

    /**
     * Reads an SQL <code>XML</code> value from the stream and returns it as a
     * <code>SQLXML</code> object in the Java programming language.
     *
     * @return a <code>SQLXML</code> object representing data of the SQL <code>XML</code> value
     * at the head of the stream; <code>null</code> if the value read is
     * SQL <code>NULL</code>
     * @exception SQLException if a database access error occurs
     * @since 1.6
     */
    public SQLXML readSQLXML() throws SQLException {
        return (SQLXML)getNextAttribute();
    }

    /**
     * Reads an SQL <code>ROWID</code> value from the stream and returns it as a
     * <code>RowId</code> object in the Java programming language.
     *
     * @return a <code>RowId</code> object representing data of the SQL <code>ROWID</code> value
     * at the head of the stream; <code>null</code> if the value read is
     * SQL <code>NULL</code>
     * @exception SQLException if a database access error occurs
     * @since 1.6
     */
    public RowId readRowId() throws SQLException {
        return  (RowId)getNextAttribute();
    }


}
