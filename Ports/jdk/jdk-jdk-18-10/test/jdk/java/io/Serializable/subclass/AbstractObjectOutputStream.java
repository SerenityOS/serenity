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
 *
 */

import java.io.*;
import java.security.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.ObjectOutputStream;

/**
 * This class provides a means for a subclass to re-implement Serialization
 * while preserving the existing public API to Serialization. A complimentary
 * subclass of AbstractObjectInputStream must also be implemented to
 * deserializa the new implementation.<p>
 *
 * Since serialization must override java access rules in order to
 * access private, protected and package accessible Serializable fields,
 * only trusted classes are allowed to subclass AbstractObjectOutputStream.
 * Subclasses of AbstractObjectOututStream must have SerializablePermission
 * "enableAbstractSubclass" or this constructor will throw a
 * SecurityException.Implementations of this class should protect themselves
 * from being subclassed in a way that will provide access to object
 * references and other sensitive info. Specifically, writeObjectOverride()
 * should be made final.
 *
 * A subclass of AbstractObjectOutputStream writes primitive data types
 * and graphs of Java objects to an ObjectOutputStream.  The objects can be read
 * (reconstituted) using he complimentary subclass of AbstractObjectInputStream.<p>
 * Persistent storage of objects can be accomplished by using a file for
 * the stream. If the stream is a network socket stream, the objects can
 * be reconstituted on another host or in another process. <p>
 *
 * Only objects that support the java.io.Serializable interface can be
 * written to streams.<p>
 *
 * The method <STRONG>writeObjectOverride</STRONG> is used to write an object
 * to the stream.  Any object, including Strings and arrays, is
 * written with writeObject. Multiple objects or primitives can be
 * written to the stream.  The objects must be read back from the
 * corresponding subclass of AbstractObjectInputstream with the same types
 * and in the same order as they were written.<p>
 *
 * Primitive data types can also be written to the stream using the
 * appropriate methods from DataOutput. Strings can also be written
 * using the writeUTF method.<p>
 *
 * The default serialization mechanism for an object is defined by
 * defaultWriteObject(). References to other objects
 * (except in transient or static fields) cause those objects to be
 * written also. Multiple references to a single object are encoded
 * using a reference sharing mechanism so that graphs of objects can
 * be restored to the same shape as when the original was written. <p>
 *
 * Classes that require special handling during the serialization and deserialization
 * process must implement special methods with these exact signatures: <p>
 *
 * <PRE>
 * private void readObject(java.io.ObjectInputStream stream)
 *     throws IOException, ClassNotFoundException;
 * private void writeObject(java.io.ObjectOutputStream stream)
 *     throws IOException
 * </PRE><p>
 * The writeObject method is responsible for writing the state of
 * the object for its particular class so that the corresponding
 * readObject method can restore it.
 * The method does not need to concern itself with the
 * state belonging to the object's superclasses or subclasses.
 * State is saved by writing the individual fields to the ObjectOutputStream
 * using the writeObject method or by using the methods for
 * primitive data types supported by DataOutput. <p>
 *
 * Serialization does not write out the fields of any object that does
 * not implement the java.io.Serializable interface.  Subclasses of
 * Objects that are not serializable can be serialized. In this case
 * the non-serializable class must have a no-arg constructor to allow
 * its fields to be initialized.  In this case it is the
 * responsibility of the subclass to save and restore the state of the
 * non-serializable class. It is frequently the case that the fields
 * of that class are accessible (public, package, or protected) or
 * that there are get and set methods that can be used to restore the
 * state. <p>
 *
 * Serialization of an object can be prevented by implementing writeObject
 * and readObject methods that throw the NotSerializableException.
 * The exception will be caught by the ObjectOutputStream and abort the
 * serialization process.
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
 * @author      Joe Fialli
 *
 * @see java.io.ObjectOutputStream
 * @see java.io.DataOutput
 * @see java.io.Serializable
 * @see java.io.Externalizable
 * @see java.io.Replaceable
 * @see java.io.ext.AbstractObjectInputStream
 *
 * @since       JDK1.2
 */
public abstract class AbstractObjectOutputStream extends ObjectOutputStream
{
    protected OutputStream out;
    /* Stream Management Methods. */

    /**
     * Creates an ObjectOutputStream that writes to the specified OutputStream.
     *
     * Add the following line to the security policy file to enable
     * subclassing.
     *
     * <PRE>
     *     permission SerializablePermission "enableAbstractSubclass" ;
     * </PRE><p>
     *
     * @exception IOException Any exception thrown by the underlying OutputStream.
     * @see java.io.ObjectOutputStream#writeStreamHeader()
     */
    public AbstractObjectOutputStream(OutputStream out) throws IOException {
        this.out = out;
    }

    public abstract void reset() throws IOException;
    protected abstract void drain() throws IOException;
    public abstract void close() throws IOException;

    /*******************************************************************/

    /* Write Objects to Stream */

    /**
     * Write the specified object to a subclass of AbstractObjectOutputStream.<p>
     *
     * NOTE: The override method of this class should have the modifier final.<p>
     *
     * Default serialization for a class can be
     * overridden by defining writeObject and the readObject methods
     * for the Serializable class. Objects referenced by this object are
     * written transitively so that a complete equivalent graph of objects
     * can be reconstructed by an ObjectInputStream.  <p>
     *
     * This method must implement the substitution semantics on the
     * object to be written, write Externalizable objects with its classes
     * override of writeExternal, and it must call annotateClass when
     * writing an ObjectStreamClass to the stream.
     *
     * Exceptions can be thrown for problems with the OutputStream and
     * for classes that should not be serialized.
     *
     * For security's sake, any overrides of this method should be final.
     * Serialization typically needs to disable java access rules
     * to serialize private, protected and package accessible Serializable
     * fields. This method gets called for ALL Serializable objects.
     *
     * @exception InvalidClassException Something is wrong with a class used by
     *     serialization.
     * @exception NotSerializableException Some object to be serialized does not
     *    implement the java.io.Serializable interface.
     * @exception IOException Any exception thrown by the underlying OutputStream.
     * @see java.io.Externalizable
     * @see java.io.ObjectOutputStream#replaceObject(Object)
     * @see java.io.Replaceable
     * @see java.io.ObjectOutputStream#annotateClass(Class)
     */
    protected void writeObjectOverride(Object obj)
        throws IOException
    {
    }

    /**
     * Write the Serializable fields of the current object to this stream.<p>
     *
     * Note: The object being serialized is not passed to this method.
     *       For security purposes, the initial implementation maintained
     *       the state of the last object to be passed to writeObject and
     *       only allowed this method to be invoked for this object.<p>
     *
     * @exception NotActiveException  Thrown if a writeObject method is not
     *                                active.
     */
    public abstract void defaultWriteObject() throws IOException;

    /*************************************************************/
    /* Use the methods of PutField to map between Serializable fields
     * and actual fields of a Serializable class.
     */

    public abstract ObjectOutputStream.PutField putFields() throws IOException;

    /**
     * Note: The PutField being serialized is not passed to this method.
     *       For security purposes, the initial implementation maintained
     *       the state of the last putFields call and
     *       only allowed this method to be invoked for that PutFields object.
     */
    public abstract void writeFields() throws IOException;

    protected abstract boolean enableReplaceObject(boolean enable) throws SecurityException;

    /*******************************************************************/
    /* Write Primitive Data to stream.  DataOutput methods. */

    public abstract void write(int data) throws IOException;
    public abstract void write(byte b[]) throws IOException;
    public abstract void write(byte b[], int off, int len) throws IOException;
    public abstract void writeBoolean(boolean data) throws IOException;
    public abstract void writeByte(int data) throws IOException;
    public abstract void writeShort(int data)  throws IOException;
    public abstract void writeChar(int data)  throws IOException;
    public abstract void writeInt(int data)  throws IOException;
    public abstract void writeLong(long data)  throws IOException;
    public abstract void writeFloat(float data) throws IOException;
    public abstract void writeDouble(double data) throws IOException;
    public abstract void writeBytes(String data) throws IOException;
    public abstract void writeChars(String data) throws IOException;
    public abstract void writeUTF(String data) throws IOException;
};
