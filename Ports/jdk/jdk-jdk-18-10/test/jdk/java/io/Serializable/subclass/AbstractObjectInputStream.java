/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/*
 *
 */

import java.io.*;
import java.util.Vector;
import java.util.Stack;
import java.util.Hashtable;
import java.lang.Math;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * This abstract class enables one to subclass ObjectInputStream
 * for the purpose of re-implementing serialization while preserving the
 * existing public serialization API. A complimentary subclass of
 * AbstractObjectInputStream must also be implemented.<p>
 *
 * Since serialization must override java access rules in order to
 * access private, protected and package accessible Serializable fields,
 * only trusted classes are allowed to subclass AbstractObjectInputStream.
 * Subclasses of AbstractObjectInputStream must define SerializablePermission
 * "enableAbstractSubclass" within a security policy file or this
 * constructor will throw a SecurityException. Implementations of this
 * class should protect themselves from being subclassed in a way that will
 * provide access to object references and other sensitive info.
 * Specifically, readObjectOverride() should be made final.
 * <p>
 *
 * A subclass of AbstractObjectInputStream deserializes primitive data and
 * objects previously written by a subclass of AbstractObjectOutputStream.
 * The subclass ensures that the types of all objects in the graph created
 * from the stream match the classes present in the Java Virtual Machine.
 * Classes are loaded as required using the standard mechanisms. <p>
 *
 * Only objects that support the java.io.Serializable or
 * java.io.Externalizable interface can be read from streams.
 *
 * The method <STRONG>readObjectOverride</STRONG> is used to read an object
 * from the stream.  Java's safe casting should be used to get the
 * desired type.  In Java, strings and arrays are objects and are
 * treated as objects during serialization. When read with readObject()
 * they need to be cast to the expected type.<p>
 *
 * Primitive data types can be read from the stream using the appropriate
 * method on DataInput. <p>
 *
 * The default deserialization mechanism for objects restores the
 * contents of each field to the value and type it had when it was written.
 * References to other objects cause those
 * objects to be read from the stream as necessary.  Graphs of objects
 * are restored correctly using a reference sharing mechanism.  New
 * objects are always allocated when deserializing, which prevents
 * existing objects from being overwritten. <p>
 *
 * Reading an object is analogous to running the constructors of a new
 * object.  Memory is allocated for the object and initialized to zero
 * (NULL).  No-arg constructors are invoked for the first non-serializable
 * super class and then the fields of the serializable classes are
 * restored from the stream starting with the serializable class closest to
 * java.lang.object and finishing with the object's most specifiec
 * class. <p>
 *
 * Classes control how they are serialized by implementing either the
 * java.io.Serializable or java.io.Externalizable interfaces.<P>
 *
 * Implementing the Serializable interface allows object serialization
 * to save and restore the entire state of the object and it allows
 * classes to evolve between the time the stream is written and the time it is
 * read.  It automatically traverses references between objects,
 * saving and restoring entire graphs.
 *
 * Serializable classes that require special handling during the
 * serialization and deserialization process should implement both
 * of these methods:<p>
 *
 * <PRE>
 * private void writeObject(java.io.ObjectOutputStream stream)
 *     throws IOException;
 * private void readObject(java.io.ObjectInputStream stream)
 *     throws IOException, ClassNotFoundException;
 * </PRE><p>
 *
 * The readObject method is responsible for reading and restoring the
 * state of the object for its particular class using data written to
 * the stream by the corresponding writeObject method.  The method
 * does not need to concern itself with the state belonging to its
 * superclasses or subclasses.  State is restored by reading data from
 * the ObjectInputStream for the individual fields and making
 * assignments to the appropriate fields of the object.  Reading
 * primitive data types is supported by DataInput. <p>
 *
 * Serialization does not read or assign values to the fields of any
 * object that does not implement the java.io.Serializable interface.
 * Subclasses of Objects that are not serializable can be
 * serializable. In this case the non-serializable class must have an
 * accessible no-arg constructor to allow its fields to be initialized.
 * In this case it is the responsibility of the subclass to save and restore
 * the state of the non-serializable class. It is frequently the case that
 * the fields of that class are accessible (public, package, or
 * protected) or that there are get and set methods that can be used
 * to restore the state. <p>
 *
 * Implementing the Externalizable interface allows the object to
 * assume complete control over the contents and format of the object's
 * serialized form.  The methods of the Externalizable interface,
 * writeExternal and readExternal, are called to save and restore the
 * objects state.  When implemented by a class they can write and read
 * their own state using all of the methods of ObjectOutput and
 * ObjectInput.  It is the responsibility of the objects to handle any
 * versioning that occurs.
 *
 * @author  Joe Fialli
 *
 * @see java.io.ObjectInputStream
 * @see java.io.DataInput
 * @see java.io.Serializable
 * @see java.io.Externalizable
 * @see java.io.ext.AbstractObjectOutputStream
 * @since   JDK1.2
 */
public abstract class AbstractObjectInputStream extends ObjectInputStream
{
    protected InputStream in;
    /**
     * Create an ObjectInputStream that reads from the specified InputStream.<p>
     *
     * Add the following line to the security policy file to enable
     * subclassing.
     *
     * <PRE>
     *     permission SerializablePermission "enableAbstractSubclass" ;
     * </PRE><p>
     *
     * @exception StreamCorruptedException The version or magic number are incorrect.
     * @exception IOException An exception occurred in the underlying stream.
     * @exception SecurityException if subclass does not have SerializablePermiision
     *            "enableAbstractSubclass".
     */
    public AbstractObjectInputStream(InputStream in)
        throws IOException, StreamCorruptedException
        {
            this.in = in;
        }

    public abstract void close() throws IOException;

    /***************************************************************/
    /* Read an object from the stream. */

    /**
     * Read an object from the ObjectInputStream.<p>
     *
     * NOTE: The override method of this class should have the modifier final.<p>
     *
     * Default deserializing for a class can be
     * overriden by defining a readObject method for the Serializable class.
     * Objects referenced by this object are read transitively so
     * that a complete equivalent graph of objects is reconstructed by
     * readObject. <p>
     *
     * The root object is completely restored when all of its fields
     * and the objects it references are completely restored. At this
     * point the object validation callbacks are executed in order
     * based on their registered priorities. The callbacks are
     * registered by objects (in the readObject special methods)
     * as they are individually restored. <p>
     *
     * For security's sake, any overrides of this method should be final.
     * Serialization typically needs to disable java access rules
     * to serialize private, protected and package accessible Serializable
     * fields. This method gets called for ALL Serializable objects.
     *
     * @exception java.lang.ClassNotFoundException Class of a serialized object
     *      cannot be found.
     * @exception InvalidClassException Something is wrong with a class used by
     *     serialization.
     * @exception StreamCorruptedException Control information in the
     *     stream is inconsistent.
     * @exception OptionalDataException Primitive data was found in the
     * stream instead of objects.
     * @exception IOException Any of the usual Input/Output related exceptions.
     *
     * @see java.io.ObjectInputStream#resolveObject(Object)
     * @see java.io.Resolvable
     * @see java.io.Externalizable
     * @see java.io.ObjectInputValidation
     * @see #registerValidation(ObjectInputValidation, int)
     * @see java.io.ObjectInputStream#resolveClass(ObjectStreamClass)
     */
    protected Object readObjectOverride()
        throws OptionalDataException, ClassNotFoundException, IOException {
            return null;
    }

    /**
     * Read the Serializable fields of the current object from this stream.<p>
     *
     * Note: The object being deserialized is not passed to this method.
     *       For security purposes, the initial implementation maintained
     *       the state of the last object to be read by readObject
     *       only allowed this method to be invoked for this object.<p>
     *
     * @exception NotActiveException  Thrown if a readObject method is not
     *                                active.
     * @exception ClassNotFoundException if no corresponding local class can be
     *                                   found in the local JVM.
     */
    public abstract void defaultReadObject()
        throws IOException, ClassNotFoundException, NotActiveException;

    /**
     * Enable allocation for subclass reimplementing serialization.<p>
     *
     * Note: Default allocation does not have the java access priviledges
     * to invoke package and protected constructors.<p>
     *
     * Security alert: this JVM native method is private within ObjectInputStream; however,
     *                 it was anticipated that re-implementors of serialization would need
     *                 access to this method. Is this allocator considered a security problem? <p>
     *
     * @param ctorClass  is the same class or a superclass of <STRONG>ofClass</STRONG>
     * @param ofClass    the type of the object to allocate.
     * @return   an object of <STRONG>ofClass</STRONG> type.
     *
     * @exception IllegalAccessException if no-arg constructor of
     *            <STRONG>ctorClass</STRONG> is not accessible from
     *            <STRONG>ofClass</STRONG>.
     * @exception InstantiationException  TBD.
     */
    protected final native Object
        allocateNewObject(Class<?> ofClass, Class<?> ctorClass)
        throws InstantiationException, IllegalAccessException;

    /**
     * Enable allocation for subclass reimplementing serialization.<p>
     *
     * Note: Default allocation does not have the java access priviledges
     * to invoke package and protected constructors.<p>
     *
     * Security alert: this JVM native method is private within ObjectInputStream; however,
     *                 it was anticipated that re-implementors of serialization would need
     *                 access to this method. Is this allocator considered a security problem?<p>
     *
     *
     * @exception IllegalAccessException  TBD.
     * @exception InstantiationException  TBD.
     */
    protected final native Object
        allocateNewArray(Class<?> componentClass, int length)
        throws InstantiationException, IllegalAccessException;

    /**
     * Reads the Serializable fields from the stream into a buffer
     * and makes the fields available by name.
     *
     * @exception java.lang.ClassNotFoundException if the class of a serialized
     *              object could not be found.
     * @exception IOException        if an I/O error occurs.
     * @exception NotActiveException if readObject() is not currently active.
     */
    public abstract ObjectInputStream.GetField readFields()
        throws IOException, ClassNotFoundException, NotActiveException;

    protected abstract boolean enableResolveObject(boolean enable) throws SecurityException;

    public abstract void registerValidation(ObjectInputValidation obj,
                                            int prio)
        throws NotActiveException, InvalidObjectException;


    /****************************************************************/

    /* Use DataInput methods to read primitive data from the stream. */

    public abstract int read() throws IOException;
    public abstract int read(byte[] data, int offset, int length)
        throws IOException;
    public abstract boolean readBoolean() throws IOException;
    public abstract byte readByte() throws IOException;
    public abstract int readUnsignedByte()  throws IOException;
    public abstract short readShort()  throws IOException;
    public abstract int readUnsignedShort() throws IOException;
    public abstract char readChar()  throws IOException;
    public abstract int readInt()  throws IOException;
    public abstract long readLong()  throws IOException;
    public abstract float readFloat() throws IOException;
    public abstract double readDouble() throws IOException;
    public abstract void readFully(byte[] data) throws IOException;
    public abstract void readFully(byte[] data, int offset, int size) throws IOException;
    public abstract String readUTF() throws IOException;
    public abstract int available() throws IOException;
    public abstract int skipBytes(int len) throws IOException;

    /* @deprecated */
    @SuppressWarnings("deprecation")
    public abstract String readLine() throws IOException;
};
