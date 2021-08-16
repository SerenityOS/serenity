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

/**
 * Provides utility classes to allow serializable mappings between SQL types
 * and data types in the Java programming language.
 * <p> Standard JDBC {@code RowSet} implementations may use these utility
 * classes to
 * assist in the serialization of disconnected {@code RowSet} objects.
 * This is useful
 * when  transmitting a disconnected {@code RowSet} object over the wire to
 * a different VM or across layers within an application.<br>
 * </p>
 *
 * <h2>1.0 SerialArray</h2>
 * A serializable mapping in the Java programming language of an SQL ARRAY
 * value. <br>
 * <br>
 * The {@code SerialArray} class provides a constructor for creating a {@code SerialArray}
 * instance from an Array object, methods for getting the base type and
 * the SQL name for the base type, and methods for copying all or part of a
 * {@code SerialArray} object. <br>
 *
 * <h2>2.0 SerialBlob</h2>
 * A serializable mapping in the Java programming language of an SQL BLOB
 * value.  <br>
 * <br>
 * The {@code SerialBlob} class provides a constructor for creating an instance
 * from a Blob object. Note that the Blob object should have brought the SQL
 * BLOB value's data over to the client before a {@code SerialBlob} object
 * is constructed from it. The data of an SQL BLOB value can be materialized
 * on the client as an array of bytes (using the method {@code Blob.getBytes})
 * or as a stream of uninterpreted bytes (using the method {@code Blob.getBinaryStream}).
 * <br>
 * <br>
 * {@code SerialBlob} methods make it possible to make a copy of a {@code SerialBlob}
 * object as an array of bytes or as a stream. They also make it possible
 * to locate a given pattern of bytes or a {@code Blob} object within a {@code SerialBlob}
 * object. <br>
 *
 * <h2>3.0 SerialClob</h2>
 * A serializable mapping in the Java programming language of an SQL CLOB
 * value.  <br>
 * <br>
 * The {@code SerialClob} class provides a constructor for creating an instance
 * from a {@code Clob} object. Note that the {@code Clob} object should have
 * brought the SQL CLOB value's data over to the client before a {@code SerialClob}
 * object is constructed from it. The data of an SQL CLOB value can be
 * materialized on the client as a stream of Unicode characters. <br>
 * <br>
 * {@code SerialClob} methods make it possible to get a substring from a
 * {@code SerialClob} object or to locate the start of a pattern of characters.
 * <br>
 *
 * <h2>5.0 SerialDatalink</h2>
 * A serializable mapping in the Java programming language of an SQL DATALINK
 * value. A DATALINK value references a file outside of the underlying data source
 * that the originating data source manages. <br>
 * <br>
 * {@code RowSet} implementations can use the method {@code RowSet.getURL()} to retrieve
 * a {@code java.net.URL} object, which can be used to manipulate the external data.
 * <br>
 * <PRE>
 *    java.net.URL url = rowset.getURL(1);
 * </PRE>
 *
 * <h2>6.0 SerialJavaObject</h2>
 * A serializable mapping in the Java programming language of an SQL JAVA_OBJECT
 * value. Assuming the Java object instance implements the Serializable interface,
 * this simply wraps the serialization process. <br>
 * <br>
 * If however, the serialization is not possible in the case where the Java
 * object is not immediately serializable, this class will attempt to serialize
 * all non static members to permit the object instance state to be serialized.
 * Static or transient fields cannot be serialized and attempting to do so
 * will result in a {@code SerialException} being thrown. <br>
 *
 * <h2>7.0 SerialRef</h2>
 * A serializable mapping between the SQL REF type and the Java programming
 * language. <br>
 * <br>
 * The {@code SerialRef} class provides a constructor for creating a {@code SerialRef}
 * instance from a {@code Ref} type and provides methods for getting
 * and setting the {@code Ref} object type. <br>
 *
 * <h2>8.0 SerialStruct</h2>
 * A serializable mapping in the Java programming language of an SQL structured
 * type. Each attribute that is not already serializable is mapped to a serializable
 * form, and if an attribute is itself a structured type, each of its attributes
 * that is not already serializable is mapped to a serializable form. <br>
 * <br>
 * In addition, if a {@code Map} object is passed to one of the constructors or
 * to the method {@code getAttributes}, the structured type is custom mapped
 * according to the mapping specified in the {@code Map} object.
 * <br>
 * The {@code SerialStruct} class provides a constructor for creating an
 * instance  from a {@code Struct} object, a method for retrieving the SQL
 * type name of the SQL structured type in the database, and methods for retrieving
 * its attribute values. <br>
 *
 * <h2>9.0 SQLInputImpl</h2>
 *   An input stream used for custom mapping user-defined types (UDTs). An
 *   {@code SQLInputImpl} object is an input stream that contains a stream of
 *   values that are
 * the attributes of a UDT. This class is used by the driver behind the scenes
 * when the method {@code getObject} is called on an SQL structured or distinct
 * type that has a custom mapping; a programmer never invokes {@code SQLInputImpl}
 * methods directly. <br>
 *   <br>
 * The {@code SQLInputImpl} class provides a set of reader methods
 * analogous to the {@code ResultSet} getter methods. These methods make it
 * possible to read the values in an {@code SQLInputImpl} object. The method
 * {@code wasNull} is used to determine whether the last value read was SQL NULL.
 * <br>
 *  <br>
 * When a constructor or getter method that takes a {@code Map} object is called,
 * the JDBC driver calls the method
 * {@code SQLData.getSQLType} to determine the SQL type of the UDT being custom
 * mapped. The driver  creates an instance of {@code SQLInputImpl}, populating it with
 * the attributes of  the UDT. The driver then passes the input stream to the
 * method {@code SQLData.readSQL},  which in turn calls the {@code SQLInputImpl}
 * methods to read the  attributes from the input stream. <br>
 *
 * <h2>10.0 SQLOutputImpl</h2>
 *   The output stream for writing the attributes of a custom mapped user-defined
 *  type (UDT) back to the database. The driver uses this interface internally,
 *  and its methods are never directly invoked by an application programmer.
 * <br>
 *   <br>
 * When an application calls the method {@code PreparedStatement.setObject}, the
 * driver checks to see whether the value to be written is a UDT with a custom
 * mapping. If it is, there will be an entry in a type map containing the Class
 * object for the class that implements {@code SQLData} for this UDT. If the
 * value to be written is an instance of {@code SQLData}, the driver will
 * create  an instance of {@code SQLOutputImpl} and pass it to the method
 * {@code SQLData.writeSQL}.
 * The method {@code writeSQL} in turn calls the appropriate {@code SQLOutputImpl}
 * writer methods to write data from the {@code SQLData} object to the
 * {@code SQLOutputImpl}
 * output  stream as the representation of an SQL user-defined type.
 *
 * <h2>Custom Mapping</h2>
 * The JDBC API provides mechanisms for mapping an SQL structured type or DISTINCT
 * type to the Java programming language.  Typically, a structured type is mapped
 * to a class, and its attributes are mapped to fields in the class.
 * (A DISTINCT type can thought of as having one attribute.)  However, there are
 * many other possibilities, and there may be any number of different mappings.
 * <P>
 * A programmer defines the mapping by implementing the interface {@code SQLData}.
 * For example, if an SQL structured type named AUTHORS has the attributes NAME,
 * TITLE, and PUBLISHER, it could be mapped to a Java class named Authors.  The
 * Authors class could have the fields name, title, and publisher, to which the
 * attributes of AUTHORS are mapped.  In such a case, the implementation of
 * {@code SQLData} could look like the following:
 * <PRE>
 *    public class Authors implements SQLData {
 *        public String name;
 *        public String title;
 *        public String publisher;
 *
 *        private String sql_type;
 *        public String getSQLTypeName() {
 *            return sql_type;
 *        }
 *
 *        public void readSQL(SQLInput stream, String type)
 *                                   throws SQLException  {
 *            sql_type = type;
 *            name = stream.readString();
 *            title = stream.readString();
 *            publisher = stream.readString();
 *        }
 *
 *        public void writeSQL(SQLOutput stream) throws SQLException {
 *            stream.writeString(name);
 *            stream.writeString(title);
 *            stream.writeString(publisher);
 *        }
 *    }
 * </PRE>
 *
 * A {@code java.util.Map} object is used to associate the SQL structured
 * type with its mapping to the class {@code Authors}. The following code fragment shows
 * how a {@code Map} object might be created and given an entry associating
 * {@code AUTHORS} and {@code Authors}.
 * <PRE>
 *     java.util.Map map = new java.util.HashMap();
 *     map.put("SCHEMA_NAME.AUTHORS", Class.forName("Authors");
 * </PRE>
 *
 * The {@code Map} object <i>map</i> now contains an entry with the
 * fully qualified name of the SQL structured type and the {@code Class}
 *  object for the class {@code Authors}.  It can be passed to a method
 * to tell the driver how to map {@code AUTHORS} to {@code Authors}.
 * <P>
 * For a disconnected {@code RowSet} object, custom mapping can be done
 * only when a {@code Map} object is passed to the method or constructor
 * that will be doing the custom mapping.  The situation is different for
 * connected {@code RowSet} objects because they maintain a connection
 * with the data source.  A method that does custom mapping and is called by
 * a disconnected {@code RowSet} object may use the {@code Map}
 * object that is associated with the {@code Connection} object being
 * used. So, in other words, if no map is specified, the connection's type
 * map can be used by default.
 */
package javax.sql.rowset.serial;
