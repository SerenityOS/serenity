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

class XObjectInputStream extends AbstractObjectInputStream {

    XObjectInputStream(InputStream in)
        throws IOException, StreamCorruptedException
        {
            super(in);
            dis = new DataInputStream(in);
        }

    public final void defaultReadObject()
        throws IOException, ClassNotFoundException, NotActiveException
    {
    }

    protected final Object readObjectOverride()
        throws OptionalDataException, ClassNotFoundException, IOException {

        Object readResult = null;
        Object prevObject = currentObject;
        Class<?>  prevDesc   = currentClassDescriptor;

        boolean NotImplemented = true;
        if (NotImplemented)
            throw new IOException("readObjectOverride not implemented");

        try {
            currentObject = null;

            //Read in class of object to currentDescriptor
            String className = dis.readUTF();
            currentClassDescriptor = Class.forName(className);

            try {
                //currentObject = Allocate a new instance of the class
                currentObject =
                    allocateNewObject(currentClassDescriptor,
                                   currentClassDescriptor);
            } catch (InstantiationException e) {
                throw new InvalidClassException(currentClassDescriptor.getName(),
                                                e.getMessage());
            } catch (IllegalAccessException e) {
                throw new InvalidClassException(currentClassDescriptor.getName(),
                                                e.getMessage());
            }

            //if currentDescriptor.isAssignable(Externalizable.class) {
            //    Object[] argList = {this};
            //    InvokeMethod(currentObject, readExternalMethod, argList);
            //} else {
            //    Does currentDescriptor have a readObject method
            //    if it does
            //        invokeMethod(this, readObjectMethod, {this});
            //    else
            //        defaultReadObject();
            //}
            // check for replacement on currentObject.
            // if toplevel readobject
            //    doObjectValidations.

        } finally {
            readResult = currentObject;
            currentObject = prevObject;
        }
        return readResult;
    }

    public ObjectInputStream.GetField readFields()
        throws IOException, ClassNotFoundException, NotActiveException {
            throw new Error("not implememted");
    }

    public synchronized void registerValidation(ObjectInputValidation obj,
                                                int prio)
        throws NotActiveException, InvalidObjectException {
    }

    public int read() throws IOException {
        return dis.read();
    }

    public int read(byte[] data, int offset, int length) throws IOException {
        return dis.read(data, offset, length);
    }

    public int available() throws IOException {
        return in.available();
    }

    public boolean readBoolean() throws IOException {
        throw new IOException("Not Implemented");
    }

    public byte readByte() throws IOException {
        throw new IOException("Not Implemented");
    }
    public int readUnsignedByte()  throws IOException {
        throw new IOException("Not Implemented");
    }
    public short readShort()  throws IOException {
        throw new IOException("Not Implemented");
    }
    public int readUnsignedShort() throws IOException {
        throw new IOException("Not Implemented");
    }
    public char readChar()  throws IOException {
        throw new IOException("Not Implemented");
    }
    public int readInt()  throws IOException {
        throw new IOException("Not Implemented");
    }
    public long readLong()  throws IOException {
        throw new IOException("Not Implemented");
    }
    public float readFloat() throws IOException {
        throw new IOException("Not Implemented");
    }
    public double readDouble() throws IOException {
        throw new IOException("Not Implemented");
    }
    public void readFully(byte[] data) throws IOException {
        throw new IOException("Not Implemented");
    }
    public void readFully(byte[] data, int offset, int size) throws IOException {
        throw new IOException("Not Implemented");
    }
    public int skipBytes(int len) throws IOException {
        throw new IOException("Not Implemented");
    }
    public String readLine() throws IOException {
        throw new IOException("Not Implemented");
    }
    public String readUTF() throws IOException {
        throw new IOException("Not Implemented");
    }

    public void close() throws IOException {
        in.close();
    }
    /**********************************************************/

    /**
     * Provide access to the persistent fields read from the input stream.
     */

     public static class InternalGetField extends ObjectInputStream.GetField {

        /**
         * Get the ObjectStreamClass that describes the fields in the stream.
         */
        public ObjectStreamClass getObjectStreamClass() {
            throw new Error("not implemented");
        }

        /**
         * Return true if the named field is defaulted and has no value
         * in this stream.
         */
        public boolean defaulted(String name)
            throws IOException, IllegalArgumentException
        {
            throw new Error("not implemented");
            //ObjectStreamField field = checkField(name, null);
        }

         public boolean get(String name, boolean defvalue)
             throws IOException, IllegalArgumentException {
             throw new Error("not implemented");
         }

         public char get(String name, char defvalue)
             throws IOException, IllegalArgumentException {
             throw new Error("not implemented");
         }

         public byte get(String name, byte defvalue)
             throws IOException, IllegalArgumentException {
             throw new Error("not implemented");
         }

         public short get(String name, short defvalue)
             throws IOException, IllegalArgumentException {
             throw new Error("not implemented");
         }

         public int get(String name, int defvalue)
             throws IOException, IllegalArgumentException {
             throw new Error("not implemented");
         }

         public long get(String name, long defvalue)
             throws IOException, IllegalArgumentException {
             throw new Error("not implemented");
         }

         public float get(String name, float defvalue)
             throws IOException, IllegalArgumentException {
             throw new Error("not implemented");
         }

         public double get(String name, double defvalue)
             throws IOException, IllegalArgumentException {
             throw new Error("not implemented");
         }

         public Object get(String name, Object defvalue)
             throws IOException, IllegalArgumentException {
             throw new Error("not implemented");
         }

         public void read(ObjectInputStream in)
             throws IOException, ClassNotFoundException {
         }
     }

    private Object currentObject;
    private Class<?> currentClassDescriptor;



    /****************************************************************/

    /* CODE LIFTED FROM ObjectStreamClass constuctor.
     * ObjectStreamClass.readObjectMethod is private.
     *
     * Look for the readObject method
     * Set the accessible flag on it here. ObjectOutputStream
     * will call it as necessary.
     */
    public static Method getReadObjectMethod(final Class<?> cl) {

        Method readObjectMethod =
            java.security.AccessController.doPrivileged
            (new java.security.PrivilegedAction<Method>() {
                public Method run() {
                    Method m = null;
                    try {
                        Class<?>[] args = {ObjectInputStream.class};
                        m = cl.getDeclaredMethod("readObject", args);
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
        return readObjectMethod;
    }

    /*************************************************************/

    /* taken verbatim from ObjectInputStream. */
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

    protected boolean enableResolveObject(boolean enable)
        throws SecurityException
    {
        throw new Error("To be implemented");
    }

    private DataInputStream dis;
};
