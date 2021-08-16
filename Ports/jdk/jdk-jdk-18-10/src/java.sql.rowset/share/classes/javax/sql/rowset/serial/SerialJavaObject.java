/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Vector;
import javax.sql.rowset.RowSetWarning;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;
import sun.reflect.misc.ReflectUtil;

/**
 * A serializable mapping in the Java programming language of an SQL
 * <code>JAVA_OBJECT</code> value. Assuming the Java object
 * implements the <code>Serializable</code> interface, this class simply wraps the
 * serialization process.
 * <P>
 * If however, the serialization is not possible because
 * the Java object is not immediately serializable, this class will
 * attempt to serialize all non-static members to permit the object
 * state to be serialized.
 * Static or transient fields cannot be serialized; an attempt to serialize
 * them will result in a <code>SerialException</code> object being thrown.
 *
 * <h2> Thread safety </h2>
 *
 * A SerialJavaObject is not safe for use by multiple concurrent threads.  If a
 * SerialJavaObject is to be used by more than one thread then access to the
 * SerialJavaObject should be controlled by appropriate synchronization.
 *
 * @author Jonathan Bruce
 * @since 1.5
 */
public class SerialJavaObject implements Serializable, Cloneable {

    /**
     * Placeholder for object to be serialized.
     */
    @SuppressWarnings("serial") // Not statically typed as Serializable
    private Object obj;


   /**
    * Placeholder for all fields in the <code>JavaObject</code> being serialized.
    */
    private transient Field[] fields;

    /**
     * Constructor for <code>SerialJavaObject</code> helper class.
     *
     * @param obj the Java <code>Object</code> to be serialized
     * @throws SerialException if the object is found not to be serializable
     */
    public SerialJavaObject(Object obj) throws SerialException {

        // if any static fields are found, an exception
        // should be thrown


        // get Class. Object instance should always be available
        Class<?> c = obj.getClass();

        // determine if object implements Serializable i/f
        if (!(obj instanceof java.io.Serializable)) {
            setWarning(new RowSetWarning("Warning, the object passed to the constructor does not implement Serializable"));
        }

        // can only determine public fields (obviously). If
        // any of these are static, this should invalidate
        // the action of attempting to persist these fields
        // in a serialized form
        fields = c.getFields();

        if (hasStaticFields(fields)) {
            throw new SerialException("Located static fields in " +
                "object instance. Cannot serialize");
        }

        this.obj = obj;
    }

    /**
     * Returns an <code>Object</code> that is a copy of this <code>SerialJavaObject</code>
     * object.
     *
     * @return a copy of this <code>SerialJavaObject</code> object as an
     *         <code>Object</code> in the Java programming language
     * @throws SerialException if the instance is corrupt
     */
    public Object getObject() throws SerialException {
        return this.obj;
    }

    /**
     * Returns an array of <code>Field</code> objects that contains each
     * field of the object that this helper class is serializing.
     *
     * @return an array of <code>Field</code> objects
     * @throws SerialException if an error is encountered accessing
     * the serialized object
     * @throws  SecurityException  If a security manager, <i>s</i>, is present
     * and the caller's class loader is not the same as or an
     * ancestor of the class loader for the class of the
     * {@linkplain #getObject object} being serialized
     * and invocation of {@link SecurityManager#checkPackageAccess
     * s.checkPackageAccess()} denies access to the package
     * of that class.
     * @see Class#getFields
     */
    @CallerSensitive
    public Field[] getFields() throws SerialException {
        if (fields != null) {
            Class<?> c = this.obj.getClass();
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                /*
                 * Check if the caller is allowed to access the specified class's package.
                 * If access is denied, throw a SecurityException.
                 */
                Class<?> caller = Reflection.getCallerClass();
                if (ReflectUtil.needsPackageAccessCheck(caller.getClassLoader(),
                                                        c.getClassLoader())) {
                    ReflectUtil.checkPackageAccess(c);
                }
            }
            return c.getFields();
        } else {
            throw new SerialException("SerialJavaObject does not contain" +
                " a serialized object instance");
        }
    }

    /**
     * The identifier that assists in the serialization of this
     * <code>SerialJavaObject</code> object.
     */
    static final long serialVersionUID = -1465795139032831023L;

    /**
     * A container for the warnings issued on this <code>SerialJavaObject</code>
     * object. When there are multiple warnings, each warning is chained to the
     * previous warning.
     */
    Vector<RowSetWarning> chain;

    /**
     * Compares this SerialJavaObject to the specified object.
     * The result is {@code true} if and only if the argument
     * is not {@code null} and is a {@code SerialJavaObject}
     * object that is identical to this object
     *
     * @param  o The object to compare this {@code SerialJavaObject} against
     *
     * @return  {@code true} if the given object represents a {@code SerialJavaObject}
     *          equivalent to this SerialJavaObject, {@code false} otherwise
     *
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof SerialJavaObject) {
            SerialJavaObject sjo = (SerialJavaObject) o;
            return obj.equals(sjo.obj);
        }
        return false;
    }

    /**
     * Returns a hash code for this SerialJavaObject. The hash code for a
     * {@code SerialJavaObject} object is taken as the hash code of
     * the {@code Object} it stores
     *
     * @return  a hash code value for this object.
     */
    public int hashCode() {
        return 31 + obj.hashCode();
    }

    /**
     * Returns a clone of this {@code SerialJavaObject}.
     *
     * @return  a clone of this SerialJavaObject
     */

    public Object clone() {
        try {
            SerialJavaObject sjo = (SerialJavaObject) super.clone();
            sjo.fields = Arrays.copyOf(fields, fields.length);
            if (chain != null)
                sjo.chain = new Vector<>(chain);
            return sjo;
        } catch (CloneNotSupportedException ex) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
    }

    /**
     * Registers the given warning.
     */
    private void setWarning(RowSetWarning e) {
        if (chain == null) {
            chain = new Vector<>();
        }
        chain.add(e);
    }

    /**
     * readObject is called to restore the state of the {@code SerialJavaObject}
     * from a stream.
     * @param s the {@code ObjectInputStream} to read from.
     *
     * @throws  ClassNotFoundException if the class of a serialized object
     *          could not be found.
     * @throws  IOException if an I/O error occurs.
     */
    private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {

        ObjectInputStream.GetField fields1 = s.readFields();
        @SuppressWarnings("unchecked")
        Vector<RowSetWarning> tmp = (Vector<RowSetWarning>)fields1.get("chain", null);
        if (tmp != null)
            chain = new Vector<>(tmp);

        obj = fields1.get("obj", null);
        if (obj != null) {
            fields = obj.getClass().getFields();
            if(hasStaticFields(fields))
                throw new IOException("Located static fields in " +
                "object instance. Cannot serialize");
        } else {
            throw new IOException("Object cannot be null!");
        }

    }

    /**
     * writeObject is called to save the state of the {@code SerialJavaObject}
     * to a stream.
     * @param s the {@code ObjectOutputStream} to write to.
     * @throws  IOException if an I/O error occurs.
     */
    private void writeObject(ObjectOutputStream s)
            throws IOException {
        ObjectOutputStream.PutField fields = s.putFields();
        fields.put("obj", obj);
        fields.put("chain", chain);
        s.writeFields();
    }

    /*
     * Check to see if there are any Static Fields in this object
     */
    private static boolean hasStaticFields(Field[] fields) {
        for (Field field : fields) {
            if ( field.getModifiers() == Modifier.STATIC) {
                return true;
            }
        }
        return false;
    }
}
