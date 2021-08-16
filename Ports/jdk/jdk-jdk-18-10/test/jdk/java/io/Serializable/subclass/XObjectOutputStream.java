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

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;
import java.security.*;

public class XObjectOutputStream extends AbstractObjectOutputStream {
    XObjectOutputStream(OutputStream out) throws IOException {
        super(out);
    }

    protected boolean enableReplaceObject(boolean enable)
    {
        throw new Error("not implemented");
    }

    protected void annotateClass(Class<?> cl) throws IOException {
    }

    public void close() throws IOException{
        out.close();
    }

    protected Object replaceObject(Object obj) throws IOException {
        return obj;
    }

    protected void writeStreamHeader() throws IOException {
        super.writeStreamHeader();
    }

    protected final void writeObjectOverride(Object obj) throws IOException {
        Object prevCurrentObject = currentObject;
        currentObject = obj;
        System.out.println("writeObjectOverride(" + obj.toString() + ")");
        try {
        //     ** Preserving reference semantics.
        //     if (obj already serialized) {
        //       look up streamId for obj and write it into 'this' stream.
        //       return;
        //     }
        //
        //     if (obj instanceof Class) {
        //       //Special processing for classes.
        //       //Might need to call this.annotateClass(obj.getClass())
        //       //someday.
        //       return;
        //     }
        //
        //     **Replacement semantics
        //     Object replacement = obj;
        //     if (enableReplace)
        //       replacement = this.writeReplace(obj);
        //     if (replacement instanceof Replaceable)
        //       replacement = ((Replaceable)replacement).replaceObject(this);
        //     if (obj != replacement) {
        //       //record that all future occurances of obj should be replaced
        //       //with replacement
        //     }
        //
        //     if obj is Externalizeable {
        //      Object[] argList = {this};
        //      invokeMethod(obj, writeExternalMethod, argList);
        //     else

        Method writeObjectMethod = getWriteObjectMethod(obj.getClass());

        if (writeObjectMethod != null) {
            Object[] arglist = {this};
            invokeMethod(obj, writeObjectMethod, arglist);
        } else
            defaultWriteObject();
        } finally {
            currentObject = prevCurrentObject;
        }
    }

    /* Since defaultWriteObject() does not take the object to write as a parameter,
     * implementation is required to store currentObject when writeObject is called.
     */
    public void defaultWriteObject() throws IOException {
        Object obj = currentObject;
        System.out.println("XObjectOutputStream.defaultWriteObject(" +
                            obj.toString() + ")");

        //In order to access package, private and protected fields,
        //one needs to use Priviledged Access and be trusted code.
        //This test will avoid that problem by only serializing public fields.
        Field[] fields = obj.getClass().getFields();
        for (int i= 0; i < fields.length; i++) {
            //Skip non-Serializable fields.
            int mods = fields[i].getModifiers();
            if (Modifier.isStatic(mods) || Modifier.isTransient(mods))
                continue;
            Class<?> FieldType = fields[i].getType();
            if (FieldType.isPrimitive()) {
                System.out.println("Field " + fields[i].getName() +
                                    " has primitive type " + FieldType.toString());
            } else {
                System.out.println("**Field " + fields[i].getName() +
                                   " is an Object of type " + FieldType.toString());
                try {
                    writeObject(fields[i].get(obj));
                    if (FieldType.isArray()) {
                        Object[] array = ((Object[]) fields[i].get(obj));
                        Class<?> componentType = FieldType.getComponentType();
                        if (componentType.isPrimitive())
                            System.out.println("Output " + array.length + " primitive elements of" +
                                               componentType.toString());
                        else {
                            System.out.println("Output " + array.length + " of Object elements of" +
                                               componentType.toString());
                            for (int k = 0; k < array.length; k++) {
                                writeObject(array[k]);
                            }
                        }
                    }
                } catch (IllegalAccessException e) {
                    throw new IOException(e.getMessage());
                }
            }
        }
    }

    public PutField putFields() throws IOException {
        currentPutField = new InternalPutField();
        return currentPutField;
    }

    public void writeFields() throws IOException {
        currentPutField.write(this);
    }

    static final class InternalPutField extends ObjectOutputStream.PutField {
        String fieldName[];
        int    intValue[];
        int next;

        InternalPutField() {
            fieldName = new String[10];
            intValue = new int[10];
            next = 0;
        }
        /**
         * Put the value of the named boolean field into the persistent field.
         */
        public void put(String name, boolean value) {
        }

        /**
         * Put the value of the named char field into the persistent fields.
         */
        public void put(String name, char value) {
        }

        /**
         * Put the value of the named byte field into the persistent fields.
         */
        public void put(String name, byte value) {
        }

        /**
         * Put the value of the named short field into the persistent fields.
         */
        public void put(String name, short value) {
        }

        /**
         * Put the value of the named int field into the persistent fields.
         */
        public void put(String name, int value) {
            if (next < fieldName.length) {
                fieldName[next] = name;
                intValue[next] = value;
                next++;
            }
        }

        /**
         * Put the value of the named long field into the persistent fields.
         */
        public void put(String name, long value) {
        }

        /**
         * Put the value of the named float field into the persistent fields.
         */
        public void put(String name, float value) {
        }

        /**
         * Put the value of the named double field into the persistent field.
         */
        public void put(String name, double value) {
        }

        /**
         * Put the value of the named Object field into the persistent field.
         */
        public void put(String name, Object value) {
        }

        /**
         * Write the data and fields to the specified ObjectOutput stream.
         */
        @SuppressWarnings("deprecation")
        public void write(ObjectOutput out) throws IOException {
            for (int i = 0; i < next; i++)
                System.out.println(fieldName[i] + "=" + intValue[i]);
        }
    };


    /**
     * Writes a byte. This method will block until the byte is actually
     * written.
     * @param b the byte
     * @exception IOException If an I/O error has occurred.
     * @since     JDK1.1
     */
    public void write(int data) throws IOException {
    }

    /**
     * Writes an array of bytes. This method will block until the bytes
     * are actually written.
     * @param b the data to be written
     * @exception IOException If an I/O error has occurred.
     * @since     JDK1.1
     */
    public void write(byte b[]) throws IOException {
    }

    /**
     * Writes a sub array of bytes.
     * @param b the data to be written
     * @param off       the start offset in the data
     * @param len       the number of bytes that are written
     * @exception IOException If an I/O error has occurred.
     * @since     JDK1.1
     */
    public void write(byte b[], int off, int len) throws IOException {
    }

    public void writeBoolean(boolean data) throws IOException {
    }

    public void writeByte(int data) throws IOException {
    }

    public void writeShort(int data)  throws IOException {
    }

    public void writeChar(int data)  throws IOException {
    }
    public void writeInt(int data)  throws IOException{}
    public void writeLong(long data)  throws IOException{}
    public void writeFloat(float data) throws IOException{}
    public void writeDouble(double data) throws IOException{}
    public void writeBytes(String data) throws IOException{}
    public void writeChars(String data) throws IOException{}
    public void writeUTF(String data) throws IOException{}
    public void reset() throws IOException {}
    public void available() throws IOException {}
    public void drain() throws IOException {}

    private Object currentObject = null;
    private InternalPutField currentPutField;


    /********************************************************************/

    /* CODE LIFTED FROM ObjectStreamClass constuctor.
     * ObjectStreamClass.writeObjectMethod is private.
     *
     * Look for the writeObject method
     * Set the accessible flag on it here.
     * Subclass of AbstractObjectOutputStream will call it as necessary.
     */
    public static Method getWriteObjectMethod(final Class<?> cl) {

        Method writeObjectMethod =
            java.security.AccessController.doPrivileged
            (new java.security.PrivilegedAction<Method>() {
                public Method run() {
                    Method m = null;
                    try {
                        Class<?>[] args = {ObjectOutputStream.class};
                        m = cl.getDeclaredMethod("writeObject", args);
                        int mods = m.getModifiers();
                        // Method must be private and non-static
                        if (!Modifier.isPrivate(mods) ||
                            Modifier.isStatic(mods)) {
                            m = null;
                        } else {
                            m.setAccessible(true);
                        }
                    } catch (NoSuchMethodException e) {
                        m = null;
                    }
                    return m;
                }
            });
        return writeObjectMethod;
    }

    /*************************************************************/

    /* CODE LIFTED FROM ObjectOutputStream. */
    private static void invokeMethod(final Object obj, final Method m,
                                        final Object[] argList)
        throws IOException
    {
        try {
            java.security.AccessController.doPrivileged
                (new java.security.PrivilegedExceptionAction<Void>() {
                    public Void run() throws InvocationTargetException,
                                        java.lang.IllegalAccessException {
                        m.invoke(obj, argList);
                        return null;
                    }
                });
        } catch (java.security.PrivilegedActionException e) {
            Exception ex = e.getException();
            if (ex instanceof InvocationTargetException) {
                Throwable t =
                        ((InvocationTargetException)ex).getTargetException();
                if (t instanceof IOException)
                    throw (IOException)t;
                else if (t instanceof RuntimeException)
                    throw (RuntimeException) t;
                else if (t instanceof Error)
                    throw (Error) t;
                else
                    throw new Error("interal error");
            } else {
                // IllegalAccessException cannot happen
            }
        }
    }
};
