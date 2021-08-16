/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.io;

import java.io.ObjectStreamClass.WeakClassKey;
import java.lang.ref.ReferenceQueue;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static java.io.ObjectStreamClass.processQueue;
import sun.reflect.misc.ReflectUtil;

/**
 * An ObjectOutputStream writes primitive data types and graphs of Java objects
 * to an OutputStream.  The objects can be read (reconstituted) using an
 * ObjectInputStream.  Persistent storage of objects can be accomplished by
 * using a file for the stream.  If the stream is a network socket stream, the
 * objects can be reconstituted on another host or in another process.
 *
 * <p>Only objects that support the java.io.Serializable interface can be
 * written to streams.  The class of each serializable object is encoded
 * including the class name and signature of the class, the values of the
 * object's fields and arrays, and the closure of any other objects referenced
 * from the initial objects.
 *
 * <p>The method writeObject is used to write an object to the stream.  Any
 * object, including Strings and arrays, is written with writeObject. Multiple
 * objects or primitives can be written to the stream.  The objects must be
 * read back from the corresponding ObjectInputstream with the same types and
 * in the same order as they were written.
 *
 * <p>Primitive data types can also be written to the stream using the
 * appropriate methods from DataOutput. Strings can also be written using the
 * writeUTF method.
 *
 * <p>The default serialization mechanism for an object writes the class of the
 * object, the class signature, and the values of all non-transient and
 * non-static fields.  References to other objects (except in transient or
 * static fields) cause those objects to be written also. Multiple references
 * to a single object are encoded using a reference sharing mechanism so that
 * graphs of objects can be restored to the same shape as when the original was
 * written.
 *
 * <p>For example to write an object that can be read by the example in
 * ObjectInputStream:
 * <br>
 * <pre>
 *      FileOutputStream fos = new FileOutputStream("t.tmp");
 *      ObjectOutputStream oos = new ObjectOutputStream(fos);
 *
 *      oos.writeInt(12345);
 *      oos.writeObject("Today");
 *      oos.writeObject(new Date());
 *
 *      oos.close();
 * </pre>
 *
 * <p>Classes that require special handling during the serialization and
 * deserialization process must implement special methods with these exact
 * signatures:
 * <br>
 * <pre>
 * private void readObject(java.io.ObjectInputStream stream)
 *     throws IOException, ClassNotFoundException;
 * private void writeObject(java.io.ObjectOutputStream stream)
 *     throws IOException
 * private void readObjectNoData()
 *     throws ObjectStreamException;
 * </pre>
 *
 * <p>The writeObject method is responsible for writing the state of the object
 * for its particular class so that the corresponding readObject method can
 * restore it.  The method does not need to concern itself with the state
 * belonging to the object's superclasses or subclasses.  State is saved by
 * writing the individual fields to the ObjectOutputStream using the
 * writeObject method or by using the methods for primitive data types
 * supported by DataOutput.
 *
 * <p>Serialization does not write out the fields of any object that does not
 * implement the java.io.Serializable interface.  Subclasses of Objects that
 * are not serializable can be serializable. In this case the non-serializable
 * class must have a no-arg constructor to allow its fields to be initialized.
 * In this case it is the responsibility of the subclass to save and restore
 * the state of the non-serializable class. It is frequently the case that the
 * fields of that class are accessible (public, package, or protected) or that
 * there are get and set methods that can be used to restore the state.
 *
 * <p>Serialization of an object can be prevented by implementing writeObject
 * and readObject methods that throw the NotSerializableException.  The
 * exception will be caught by the ObjectOutputStream and abort the
 * serialization process.
 *
 * <p>Implementing the Externalizable interface allows the object to assume
 * complete control over the contents and format of the object's serialized
 * form.  The methods of the Externalizable interface, writeExternal and
 * readExternal, are called to save and restore the objects state.  When
 * implemented by a class they can write and read their own state using all of
 * the methods of ObjectOutput and ObjectInput.  It is the responsibility of
 * the objects to handle any versioning that occurs.
 *
 * <p>Enum constants are serialized differently than ordinary serializable or
 * externalizable objects.  The serialized form of an enum constant consists
 * solely of its name; field values of the constant are not transmitted.  To
 * serialize an enum constant, ObjectOutputStream writes the string returned by
 * the constant's name method.  Like other serializable or externalizable
 * objects, enum constants can function as the targets of back references
 * appearing subsequently in the serialization stream.  The process by which
 * enum constants are serialized cannot be customized; any class-specific
 * writeObject and writeReplace methods defined by enum types are ignored
 * during serialization.  Similarly, any serialPersistentFields or
 * serialVersionUID field declarations are also ignored--all enum types have a
 * fixed serialVersionUID of 0L.
 *
 * <p>Primitive data, excluding serializable fields and externalizable data, is
 * written to the ObjectOutputStream in block-data records. A block data record
 * is composed of a header and data. The block data header consists of a marker
 * and the number of bytes to follow the header.  Consecutive primitive data
 * writes are merged into one block-data record.  The blocking factor used for
 * a block-data record will be 1024 bytes.  Each block-data record will be
 * filled up to 1024 bytes, or be written whenever there is a termination of
 * block-data mode.  Calls to the ObjectOutputStream methods writeObject,
 * defaultWriteObject and writeFields initially terminate any existing
 * block-data record.
 *
 * <p>Records are serialized differently than ordinary serializable or externalizable
 * objects, see <a href="ObjectInputStream.html#record-serialization">record serialization</a>.
 *
 * @author      Mike Warres
 * @author      Roger Riggs
 * @see java.io.DataOutput
 * @see java.io.ObjectInputStream
 * @see java.io.Serializable
 * @see java.io.Externalizable
 * @see <a href="{@docRoot}/../specs/serialization/output.html">
 *      <cite>Java Object Serialization Specification,</cite> Section 2, "Object Output Classes"</a>
 * @since       1.1
 */
public class ObjectOutputStream
    extends OutputStream implements ObjectOutput, ObjectStreamConstants
{

    private static class Caches {
        /** cache of subclass security audit results */
        static final ConcurrentMap<WeakClassKey,Boolean> subclassAudits =
            new ConcurrentHashMap<>();

        /** queue for WeakReferences to audited subclasses */
        static final ReferenceQueue<Class<?>> subclassAuditsQueue =
            new ReferenceQueue<>();
    }

    /** filter stream for handling block data conversion */
    private final BlockDataOutputStream bout;
    /** obj -> wire handle map */
    private final HandleTable handles;
    /** obj -> replacement obj map */
    private final ReplaceTable subs;
    /** stream protocol version */
    private int protocol = PROTOCOL_VERSION_2;
    /** recursion depth */
    private int depth;

    /** buffer for writing primitive field values */
    private byte[] primVals;

    /** if true, invoke writeObjectOverride() instead of writeObject() */
    private final boolean enableOverride;
    /** if true, invoke replaceObject() */
    private boolean enableReplace;

    // values below valid only during upcalls to writeObject()/writeExternal()
    /**
     * Context during upcalls to class-defined writeObject methods; holds
     * object currently being serialized and descriptor for current class.
     * Null when not during writeObject upcall.
     */
    private SerialCallbackContext curContext;
    /** current PutField object */
    private PutFieldImpl curPut;

    /** custom storage for debug trace info */
    private final DebugTraceInfoStack debugInfoStack;

    /**
     * value of "sun.io.serialization.extendedDebugInfo" property,
     * as true or false for extended information about exception's place
     */
    @SuppressWarnings("removal")
    private static final boolean extendedDebugInfo =
        java.security.AccessController.doPrivileged(
            new sun.security.action.GetBooleanAction(
                "sun.io.serialization.extendedDebugInfo")).booleanValue();

    /**
     * Creates an ObjectOutputStream that writes to the specified OutputStream.
     * This constructor writes the serialization stream header to the
     * underlying stream; callers may wish to flush the stream immediately to
     * ensure that constructors for receiving ObjectInputStreams will not block
     * when reading the header.
     *
     * <p>If a security manager is installed, this constructor will check for
     * the "enableSubclassImplementation" SerializablePermission when invoked
     * directly or indirectly by the constructor of a subclass which overrides
     * the ObjectOutputStream.putFields or ObjectOutputStream.writeUnshared
     * methods.
     *
     * @param   out output stream to write to
     * @throws  IOException if an I/O error occurs while writing stream header
     * @throws  SecurityException if untrusted subclass illegally overrides
     *          security-sensitive methods
     * @throws  NullPointerException if {@code out} is {@code null}
     * @since   1.4
     * @see     ObjectOutputStream#ObjectOutputStream()
     * @see     ObjectOutputStream#putFields()
     * @see     ObjectInputStream#ObjectInputStream(InputStream)
     */
    public ObjectOutputStream(OutputStream out) throws IOException {
        verifySubclass();
        bout = new BlockDataOutputStream(out);
        handles = new HandleTable(10, (float) 3.00);
        subs = new ReplaceTable(10, (float) 3.00);
        enableOverride = false;
        writeStreamHeader();
        bout.setBlockDataMode(true);
        if (extendedDebugInfo) {
            debugInfoStack = new DebugTraceInfoStack();
        } else {
            debugInfoStack = null;
        }
    }

    /**
     * Provide a way for subclasses that are completely reimplementing
     * ObjectOutputStream to not have to allocate private data just used by
     * this implementation of ObjectOutputStream.
     *
     * <p>If there is a security manager installed, this method first calls the
     * security manager's {@code checkPermission} method with a
     * {@code SerializablePermission("enableSubclassImplementation")}
     * permission to ensure it's ok to enable subclassing.
     *
     * @throws  SecurityException if a security manager exists and its
     *          {@code checkPermission} method denies enabling
     *          subclassing.
     * @throws  IOException if an I/O error occurs while creating this stream
     * @see SecurityManager#checkPermission
     * @see java.io.SerializablePermission
     */
    protected ObjectOutputStream() throws IOException, SecurityException {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(SUBCLASS_IMPLEMENTATION_PERMISSION);
        }
        bout = null;
        handles = null;
        subs = null;
        enableOverride = true;
        debugInfoStack = null;
    }

    /**
     * Specify stream protocol version to use when writing the stream.
     *
     * <p>This routine provides a hook to enable the current version of
     * Serialization to write in a format that is backwards compatible to a
     * previous version of the stream format.
     *
     * <p>Every effort will be made to avoid introducing additional
     * backwards incompatibilities; however, sometimes there is no
     * other alternative.
     *
     * @param   version use ProtocolVersion from java.io.ObjectStreamConstants.
     * @throws  IllegalStateException if called after any objects
     *          have been serialized.
     * @throws  IllegalArgumentException if invalid version is passed in.
     * @throws  IOException if I/O errors occur
     * @see java.io.ObjectStreamConstants#PROTOCOL_VERSION_1
     * @see java.io.ObjectStreamConstants#PROTOCOL_VERSION_2
     * @since   1.2
     */
    public void useProtocolVersion(int version) throws IOException {
        if (handles.size() != 0) {
            // REMIND: implement better check for pristine stream?
            throw new IllegalStateException("stream non-empty");
        }
        switch (version) {
            case PROTOCOL_VERSION_1:
            case PROTOCOL_VERSION_2:
                protocol = version;
                break;

            default:
                throw new IllegalArgumentException(
                    "unknown version: " + version);
        }
    }

    /**
     * Write the specified object to the ObjectOutputStream.  The class of the
     * object, the signature of the class, and the values of the non-transient
     * and non-static fields of the class and all of its supertypes are
     * written.  Default serialization for a class can be overridden using the
     * writeObject and the readObject methods.  Objects referenced by this
     * object are written transitively so that a complete equivalent graph of
     * objects can be reconstructed by an ObjectInputStream.
     *
     * <p>Exceptions are thrown for problems with the OutputStream and for
     * classes that should not be serialized.  All exceptions are fatal to the
     * OutputStream, which is left in an indeterminate state, and it is up to
     * the caller to ignore or recover the stream state.
     *
     * @throws  InvalidClassException Something is wrong with a class used by
     *          serialization.
     * @throws  NotSerializableException Some object to be serialized does not
     *          implement the java.io.Serializable interface.
     * @throws  IOException Any exception thrown by the underlying
     *          OutputStream.
     */
    public final void writeObject(Object obj) throws IOException {
        if (enableOverride) {
            writeObjectOverride(obj);
            return;
        }
        try {
            writeObject0(obj, false);
        } catch (IOException ex) {
            if (depth == 0) {
                writeFatalException(ex);
            }
            throw ex;
        }
    }

    /**
     * Method used by subclasses to override the default writeObject method.
     * This method is called by trusted subclasses of ObjectOutputStream that
     * constructed ObjectOutputStream using the protected no-arg constructor.
     * The subclass is expected to provide an override method with the modifier
     * "final".
     *
     * @param   obj object to be written to the underlying stream
     * @throws  IOException if there are I/O errors while writing to the
     *          underlying stream
     * @see #ObjectOutputStream()
     * @see #writeObject(Object)
     * @since 1.2
     */
    protected void writeObjectOverride(Object obj) throws IOException {
    }

    /**
     * Writes an "unshared" object to the ObjectOutputStream.  This method is
     * identical to writeObject, except that it always writes the given object
     * as a new, unique object in the stream (as opposed to a back-reference
     * pointing to a previously serialized instance).  Specifically:
     * <ul>
     *   <li>An object written via writeUnshared is always serialized in the
     *       same manner as a newly appearing object (an object that has not
     *       been written to the stream yet), regardless of whether or not the
     *       object has been written previously.
     *
     *   <li>If writeObject is used to write an object that has been previously
     *       written with writeUnshared, the previous writeUnshared operation
     *       is treated as if it were a write of a separate object.  In other
     *       words, ObjectOutputStream will never generate back-references to
     *       object data written by calls to writeUnshared.
     * </ul>
     * While writing an object via writeUnshared does not in itself guarantee a
     * unique reference to the object when it is deserialized, it allows a
     * single object to be defined multiple times in a stream, so that multiple
     * calls to readUnshared by the receiver will not conflict.  Note that the
     * rules described above only apply to the base-level object written with
     * writeUnshared, and not to any transitively referenced sub-objects in the
     * object graph to be serialized.
     *
     * <p>ObjectOutputStream subclasses which override this method can only be
     * constructed in security contexts possessing the
     * "enableSubclassImplementation" SerializablePermission; any attempt to
     * instantiate such a subclass without this permission will cause a
     * SecurityException to be thrown.
     *
     * @param   obj object to write to stream
     * @throws  NotSerializableException if an object in the graph to be
     *          serialized does not implement the Serializable interface
     * @throws  InvalidClassException if a problem exists with the class of an
     *          object to be serialized
     * @throws  IOException if an I/O error occurs during serialization
     * @since 1.4
     */
    public void writeUnshared(Object obj) throws IOException {
        try {
            writeObject0(obj, true);
        } catch (IOException ex) {
            if (depth == 0) {
                writeFatalException(ex);
            }
            throw ex;
        }
    }

    /**
     * Write the non-static and non-transient fields of the current class to
     * this stream.  This may only be called from the writeObject method of the
     * class being serialized. It will throw the NotActiveException if it is
     * called otherwise.
     *
     * @throws  IOException if I/O errors occur while writing to the underlying
     *          {@code OutputStream}
     */
    public void defaultWriteObject() throws IOException {
        SerialCallbackContext ctx = curContext;
        if (ctx == null) {
            throw new NotActiveException("not in call to writeObject");
        }
        Object curObj = ctx.getObj();
        ObjectStreamClass curDesc = ctx.getDesc();
        bout.setBlockDataMode(false);
        defaultWriteFields(curObj, curDesc);
        bout.setBlockDataMode(true);
    }

    /**
     * Retrieve the object used to buffer persistent fields to be written to
     * the stream.  The fields will be written to the stream when writeFields
     * method is called.
     *
     * @return  an instance of the class Putfield that holds the serializable
     *          fields
     * @throws  IOException if I/O errors occur
     * @since 1.2
     */
    public ObjectOutputStream.PutField putFields() throws IOException {
        if (curPut == null) {
            SerialCallbackContext ctx = curContext;
            if (ctx == null) {
                throw new NotActiveException("not in call to writeObject");
            }
            ctx.checkAndSetUsed();
            ObjectStreamClass curDesc = ctx.getDesc();
            curPut = new PutFieldImpl(curDesc);
        }
        return curPut;
    }

    /**
     * Write the buffered fields to the stream.
     *
     * @throws  IOException if I/O errors occur while writing to the underlying
     *          stream
     * @throws  NotActiveException Called when a classes writeObject method was
     *          not called to write the state of the object.
     * @since 1.2
     */
    public void writeFields() throws IOException {
        if (curPut == null) {
            throw new NotActiveException("no current PutField object");
        }
        bout.setBlockDataMode(false);
        curPut.writeFields();
        bout.setBlockDataMode(true);
    }

    /**
     * Reset will disregard the state of any objects already written to the
     * stream.  The state is reset to be the same as a new ObjectOutputStream.
     * The current point in the stream is marked as reset so the corresponding
     * ObjectInputStream will be reset at the same point.  Objects previously
     * written to the stream will not be referred to as already being in the
     * stream.  They will be written to the stream again.
     *
     * @throws  IOException if reset() is invoked while serializing an object.
     */
    public void reset() throws IOException {
        if (depth != 0) {
            throw new IOException("stream active");
        }
        bout.setBlockDataMode(false);
        bout.writeByte(TC_RESET);
        clear();
        bout.setBlockDataMode(true);
    }

    /**
     * Subclasses may implement this method to allow class data to be stored in
     * the stream. By default this method does nothing.  The corresponding
     * method in ObjectInputStream is resolveClass.  This method is called
     * exactly once for each unique class in the stream.  The class name and
     * signature will have already been written to the stream.  This method may
     * make free use of the ObjectOutputStream to save any representation of
     * the class it deems suitable (for example, the bytes of the class file).
     * The resolveClass method in the corresponding subclass of
     * ObjectInputStream must read and use any data or objects written by
     * annotateClass.
     *
     * @param   cl the class to annotate custom data for
     * @throws  IOException Any exception thrown by the underlying
     *          OutputStream.
     */
    protected void annotateClass(Class<?> cl) throws IOException {
    }

    /**
     * Subclasses may implement this method to store custom data in the stream
     * along with descriptors for dynamic proxy classes.
     *
     * <p>This method is called exactly once for each unique proxy class
     * descriptor in the stream.  The default implementation of this method in
     * {@code ObjectOutputStream} does nothing.
     *
     * <p>The corresponding method in {@code ObjectInputStream} is
     * {@code resolveProxyClass}.  For a given subclass of
     * {@code ObjectOutputStream} that overrides this method, the
     * {@code resolveProxyClass} method in the corresponding subclass of
     * {@code ObjectInputStream} must read any data or objects written by
     * {@code annotateProxyClass}.
     *
     * @param   cl the proxy class to annotate custom data for
     * @throws  IOException any exception thrown by the underlying
     *          {@code OutputStream}
     * @see ObjectInputStream#resolveProxyClass(String[])
     * @since   1.3
     */
    protected void annotateProxyClass(Class<?> cl) throws IOException {
    }

    /**
     * This method will allow trusted subclasses of ObjectOutputStream to
     * substitute one object for another during serialization. Replacing
     * objects is disabled until enableReplaceObject is called. The
     * enableReplaceObject method checks that the stream requesting to do
     * replacement can be trusted.  The first occurrence of each object written
     * into the serialization stream is passed to replaceObject.  Subsequent
     * references to the object are replaced by the object returned by the
     * original call to replaceObject.  To ensure that the private state of
     * objects is not unintentionally exposed, only trusted streams may use
     * replaceObject.
     *
     * <p>The ObjectOutputStream.writeObject method takes a parameter of type
     * Object (as opposed to type Serializable) to allow for cases where
     * non-serializable objects are replaced by serializable ones.
     *
     * <p>When a subclass is replacing objects it must insure that either a
     * complementary substitution must be made during deserialization or that
     * the substituted object is compatible with every field where the
     * reference will be stored.  Objects whose type is not a subclass of the
     * type of the field or array element abort the serialization by raising an
     * exception and the object is not be stored.
     *
     * <p>This method is called only once when each object is first
     * encountered.  All subsequent references to the object will be redirected
     * to the new object. This method should return the object to be
     * substituted or the original object.
     *
     * <p>Null can be returned as the object to be substituted, but may cause
     * NullReferenceException in classes that contain references to the
     * original object since they may be expecting an object instead of
     * null.
     *
     * @param   obj the object to be replaced
     * @return  the alternate object that replaced the specified one
     * @throws  IOException Any exception thrown by the underlying
     *          OutputStream.
     */
    protected Object replaceObject(Object obj) throws IOException {
        return obj;
    }

    /**
     * Enables the stream to do replacement of objects written to the stream.  When
     * enabled, the {@link #replaceObject} method is called for every object being
     * serialized.
     *
     * <p>If object replacement is currently not enabled, and
     * {@code enable} is true, and there is a security manager installed,
     * this method first calls the security manager's
     * {@code checkPermission} method with the
     * {@code SerializablePermission("enableSubstitution")} permission to
     * ensure that the caller is permitted to enable the stream to do replacement
     * of objects written to the stream.
     *
     * @param   enable true for enabling use of {@code replaceObject} for
     *          every object being serialized
     * @return  the previous setting before this method was invoked
     * @throws  SecurityException if a security manager exists and its
     *          {@code checkPermission} method denies enabling the stream
     *          to do replacement of objects written to the stream.
     * @see SecurityManager#checkPermission
     * @see java.io.SerializablePermission
     */
    protected boolean enableReplaceObject(boolean enable)
        throws SecurityException
    {
        if (enable == enableReplace) {
            return enable;
        }
        if (enable) {
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(SUBSTITUTION_PERMISSION);
            }
        }
        enableReplace = enable;
        return !enableReplace;
    }

    /**
     * The writeStreamHeader method is provided so subclasses can append or
     * prepend their own header to the stream.  It writes the magic number and
     * version to the stream.
     *
     * @throws  IOException if I/O errors occur while writing to the underlying
     *          stream
     */
    protected void writeStreamHeader() throws IOException {
        bout.writeShort(STREAM_MAGIC);
        bout.writeShort(STREAM_VERSION);
    }

    /**
     * Write the specified class descriptor to the ObjectOutputStream.  Class
     * descriptors are used to identify the classes of objects written to the
     * stream.  Subclasses of ObjectOutputStream may override this method to
     * customize the way in which class descriptors are written to the
     * serialization stream.  The corresponding method in ObjectInputStream,
     * {@code readClassDescriptor}, should then be overridden to
     * reconstitute the class descriptor from its custom stream representation.
     * By default, this method writes class descriptors according to the format
     * defined in the Object Serialization specification.
     *
     * <p>Note that this method will only be called if the ObjectOutputStream
     * is not using the old serialization stream format (set by calling
     * ObjectOutputStream's {@code useProtocolVersion} method).  If this
     * serialization stream is using the old format
     * ({@code PROTOCOL_VERSION_1}), the class descriptor will be written
     * internally in a manner that cannot be overridden or customized.
     *
     * @param   desc class descriptor to write to the stream
     * @throws  IOException If an I/O error has occurred.
     * @see java.io.ObjectInputStream#readClassDescriptor()
     * @see #useProtocolVersion(int)
     * @see java.io.ObjectStreamConstants#PROTOCOL_VERSION_1
     * @since 1.3
     */
    protected void writeClassDescriptor(ObjectStreamClass desc)
        throws IOException
    {
        desc.writeNonProxy(this);
    }

    /**
     * Writes a byte. This method will block until the byte is actually
     * written.
     *
     * @param   val the byte to be written to the stream
     * @throws  IOException If an I/O error has occurred.
     */
    public void write(int val) throws IOException {
        bout.write(val);
    }

    /**
     * Writes an array of bytes. This method will block until the bytes are
     * actually written.
     *
     * @param   buf the data to be written
     * @throws  IOException If an I/O error has occurred.
     */
    public void write(byte[] buf) throws IOException {
        bout.write(buf, 0, buf.length, false);
    }

    /**
     * Writes a sub array of bytes.
     *
     * @param   buf the data to be written
     * @param   off the start offset in the data
     * @param   len the number of bytes that are written
     * @throws  IOException If an I/O error has occurred.
     */
    public void write(byte[] buf, int off, int len) throws IOException {
        if (buf == null) {
            throw new NullPointerException();
        }
        int endoff = off + len;
        if (off < 0 || len < 0 || endoff > buf.length || endoff < 0) {
            throw new IndexOutOfBoundsException();
        }
        bout.write(buf, off, len, false);
    }

    /**
     * Flushes the stream. This will write any buffered output bytes and flush
     * through to the underlying stream.
     *
     * @throws  IOException If an I/O error has occurred.
     */
    public void flush() throws IOException {
        bout.flush();
    }

    /**
     * Drain any buffered data in ObjectOutputStream.  Similar to flush but
     * does not propagate the flush to the underlying stream.
     *
     * @throws  IOException if I/O errors occur while writing to the underlying
     *          stream
     */
    protected void drain() throws IOException {
        bout.drain();
    }

    /**
     * Closes the stream. This method must be called to release any resources
     * associated with the stream.
     *
     * @throws  IOException If an I/O error has occurred.
     */
    public void close() throws IOException {
        flush();
        clear();
        bout.close();
    }

    /**
     * Writes a boolean.
     *
     * @param   val the boolean to be written
     * @throws  IOException if I/O errors occur while writing to the underlying
     *          stream
     */
    public void writeBoolean(boolean val) throws IOException {
        bout.writeBoolean(val);
    }

    /**
     * Writes an 8 bit byte.
     *
     * @param   val the byte value to be written
     * @throws  IOException if I/O errors occur while writing to the underlying
     *          stream
     */
    public void writeByte(int val) throws IOException  {
        bout.writeByte(val);
    }

    /**
     * Writes a 16 bit short.
     *
     * @param   val the short value to be written
     * @throws  IOException if I/O errors occur while writing to the underlying
     *          stream
     */
    public void writeShort(int val)  throws IOException {
        bout.writeShort(val);
    }

    /**
     * Writes a 16 bit char.
     *
     * @param   val the char value to be written
     * @throws  IOException if I/O errors occur while writing to the underlying
     *          stream
     */
    public void writeChar(int val)  throws IOException {
        bout.writeChar(val);
    }

    /**
     * Writes a 32 bit int.
     *
     * @param   val the integer value to be written
     * @throws  IOException if I/O errors occur while writing to the underlying
     *          stream
     */
    public void writeInt(int val)  throws IOException {
        bout.writeInt(val);
    }

    /**
     * Writes a 64 bit long.
     *
     * @param   val the long value to be written
     * @throws  IOException if I/O errors occur while writing to the underlying
     *          stream
     */
    public void writeLong(long val)  throws IOException {
        bout.writeLong(val);
    }

    /**
     * Writes a 32 bit float.
     *
     * @param   val the float value to be written
     * @throws  IOException if I/O errors occur while writing to the underlying
     *          stream
     */
    public void writeFloat(float val) throws IOException {
        bout.writeFloat(val);
    }

    /**
     * Writes a 64 bit double.
     *
     * @param   val the double value to be written
     * @throws  IOException if I/O errors occur while writing to the underlying
     *          stream
     */
    public void writeDouble(double val) throws IOException {
        bout.writeDouble(val);
    }

    /**
     * Writes a String as a sequence of bytes.
     *
     * @param   str the String of bytes to be written
     * @throws  IOException if I/O errors occur while writing to the underlying
     *          stream
     */
    public void writeBytes(String str) throws IOException {
        bout.writeBytes(str);
    }

    /**
     * Writes a String as a sequence of chars.
     *
     * @param   str the String of chars to be written
     * @throws  IOException if I/O errors occur while writing to the underlying
     *          stream
     */
    public void writeChars(String str) throws IOException {
        bout.writeChars(str);
    }

    /**
     * Primitive data write of this String in
     * <a href="DataInput.html#modified-utf-8">modified UTF-8</a>
     * format.  Note that there is a
     * significant difference between writing a String into the stream as
     * primitive data or as an Object. A String instance written by writeObject
     * is written into the stream as a String initially. Future writeObject()
     * calls write references to the string into the stream.
     *
     * @param   str the String to be written
     * @throws  IOException if I/O errors occur while writing to the underlying
     *          stream
     */
    public void writeUTF(String str) throws IOException {
        bout.writeUTF(str);
    }

    /**
     * Provide programmatic access to the persistent fields to be written
     * to ObjectOutput.
     *
     * @since 1.2
     */
    public abstract static class PutField {
        /**
         * Constructor for subclasses to call.
         */
        public PutField() {}

        /**
         * Put the value of the named boolean field into the persistent field.
         *
         * @param  name the name of the serializable field
         * @param  val the value to assign to the field
         * @throws IllegalArgumentException if {@code name} does not
         * match the name of a serializable field for the class whose fields
         * are being written, or if the type of the named field is not
         * {@code boolean}
         */
        public abstract void put(String name, boolean val);

        /**
         * Put the value of the named byte field into the persistent field.
         *
         * @param  name the name of the serializable field
         * @param  val the value to assign to the field
         * @throws IllegalArgumentException if {@code name} does not
         * match the name of a serializable field for the class whose fields
         * are being written, or if the type of the named field is not
         * {@code byte}
         */
        public abstract void put(String name, byte val);

        /**
         * Put the value of the named char field into the persistent field.
         *
         * @param  name the name of the serializable field
         * @param  val the value to assign to the field
         * @throws IllegalArgumentException if {@code name} does not
         * match the name of a serializable field for the class whose fields
         * are being written, or if the type of the named field is not
         * {@code char}
         */
        public abstract void put(String name, char val);

        /**
         * Put the value of the named short field into the persistent field.
         *
         * @param  name the name of the serializable field
         * @param  val the value to assign to the field
         * @throws IllegalArgumentException if {@code name} does not
         * match the name of a serializable field for the class whose fields
         * are being written, or if the type of the named field is not
         * {@code short}
         */
        public abstract void put(String name, short val);

        /**
         * Put the value of the named int field into the persistent field.
         *
         * @param  name the name of the serializable field
         * @param  val the value to assign to the field
         * @throws IllegalArgumentException if {@code name} does not
         * match the name of a serializable field for the class whose fields
         * are being written, or if the type of the named field is not
         * {@code int}
         */
        public abstract void put(String name, int val);

        /**
         * Put the value of the named long field into the persistent field.
         *
         * @param  name the name of the serializable field
         * @param  val the value to assign to the field
         * @throws IllegalArgumentException if {@code name} does not
         * match the name of a serializable field for the class whose fields
         * are being written, or if the type of the named field is not
         * {@code long}
         */
        public abstract void put(String name, long val);

        /**
         * Put the value of the named float field into the persistent field.
         *
         * @param  name the name of the serializable field
         * @param  val the value to assign to the field
         * @throws IllegalArgumentException if {@code name} does not
         * match the name of a serializable field for the class whose fields
         * are being written, or if the type of the named field is not
         * {@code float}
         */
        public abstract void put(String name, float val);

        /**
         * Put the value of the named double field into the persistent field.
         *
         * @param  name the name of the serializable field
         * @param  val the value to assign to the field
         * @throws IllegalArgumentException if {@code name} does not
         * match the name of a serializable field for the class whose fields
         * are being written, or if the type of the named field is not
         * {@code double}
         */
        public abstract void put(String name, double val);

        /**
         * Put the value of the named Object field into the persistent field.
         *
         * @param  name the name of the serializable field
         * @param  val the value to assign to the field
         *         (which may be {@code null})
         * @throws IllegalArgumentException if {@code name} does not
         * match the name of a serializable field for the class whose fields
         * are being written, or if the type of the named field is not a
         * reference type
         */
        public abstract void put(String name, Object val);

        /**
         * Write the data and fields to the specified ObjectOutput stream,
         * which must be the same stream that produced this
         * {@code PutField} object.
         *
         * @param  out the stream to write the data and fields to
         * @throws IOException if I/O errors occur while writing to the
         *         underlying stream
         * @throws IllegalArgumentException if the specified stream is not
         *         the same stream that produced this {@code PutField}
         *         object
         * @deprecated This method does not write the values contained by this
         *         {@code PutField} object in a proper format, and may
         *         result in corruption of the serialization stream.  The
         *         correct way to write {@code PutField} data is by
         *         calling the {@link java.io.ObjectOutputStream#writeFields()}
         *         method.
         */
        @Deprecated
        public abstract void write(ObjectOutput out) throws IOException;
    }


    /**
     * Returns protocol version in use.
     */
    int getProtocolVersion() {
        return protocol;
    }

    /**
     * Writes string without allowing it to be replaced in stream.  Used by
     * ObjectStreamClass to write class descriptor type strings.
     */
    void writeTypeString(String str) throws IOException {
        int handle;
        if (str == null) {
            writeNull();
        } else if ((handle = handles.lookup(str)) != -1) {
            writeHandle(handle);
        } else {
            writeString(str, false);
        }
    }

    /**
     * Verifies that this (possibly subclass) instance can be constructed
     * without violating security constraints: the subclass must not override
     * security-sensitive non-final methods, or else the
     * "enableSubclassImplementation" SerializablePermission is checked.
     */
    private void verifySubclass() {
        Class<?> cl = getClass();
        if (cl == ObjectOutputStream.class) {
            return;
        }
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            return;
        }
        processQueue(Caches.subclassAuditsQueue, Caches.subclassAudits);
        WeakClassKey key = new WeakClassKey(cl, Caches.subclassAuditsQueue);
        Boolean result = Caches.subclassAudits.get(key);
        if (result == null) {
            result = auditSubclass(cl);
            Caches.subclassAudits.putIfAbsent(key, result);
        }
        if (!result) {
            sm.checkPermission(SUBCLASS_IMPLEMENTATION_PERMISSION);
        }
    }

    /**
     * Performs reflective checks on given subclass to verify that it doesn't
     * override security-sensitive non-final methods.  Returns TRUE if subclass
     * is "safe", FALSE otherwise.
     */
    @SuppressWarnings("removal")
    private static Boolean auditSubclass(Class<?> subcl) {
        return AccessController.doPrivileged(
            new PrivilegedAction<>() {
                public Boolean run() {
                    for (Class<?> cl = subcl;
                         cl != ObjectOutputStream.class;
                         cl = cl.getSuperclass())
                    {
                        try {
                            cl.getDeclaredMethod(
                                "writeUnshared", new Class<?>[] { Object.class });
                            return Boolean.FALSE;
                        } catch (NoSuchMethodException ex) {
                        }
                        try {
                            cl.getDeclaredMethod("putFields", (Class<?>[]) null);
                            return Boolean.FALSE;
                        } catch (NoSuchMethodException ex) {
                        }
                    }
                    return Boolean.TRUE;
                }
            }
        );
    }

    /**
     * Clears internal data structures.
     */
    private void clear() {
        subs.clear();
        handles.clear();
    }

    /**
     * Underlying writeObject/writeUnshared implementation.
     */
    private void writeObject0(Object obj, boolean unshared)
        throws IOException
    {
        boolean oldMode = bout.setBlockDataMode(false);
        depth++;
        try {
            // handle previously written and non-replaceable objects
            int h;
            if ((obj = subs.lookup(obj)) == null) {
                writeNull();
                return;
            } else if (!unshared && (h = handles.lookup(obj)) != -1) {
                writeHandle(h);
                return;
            } else if (obj instanceof Class) {
                writeClass((Class) obj, unshared);
                return;
            } else if (obj instanceof ObjectStreamClass) {
                writeClassDesc((ObjectStreamClass) obj, unshared);
                return;
            }

            // check for replacement object
            Object orig = obj;
            Class<?> cl = obj.getClass();
            ObjectStreamClass desc;
            for (;;) {
                // REMIND: skip this check for strings/arrays?
                Class<?> repCl;
                desc = ObjectStreamClass.lookup(cl, true);
                if (!desc.hasWriteReplaceMethod() ||
                    (obj = desc.invokeWriteReplace(obj)) == null ||
                    (repCl = obj.getClass()) == cl)
                {
                    break;
                }
                cl = repCl;
            }
            if (enableReplace) {
                Object rep = replaceObject(obj);
                if (rep != obj && rep != null) {
                    cl = rep.getClass();
                    desc = ObjectStreamClass.lookup(cl, true);
                }
                obj = rep;
            }

            // if object replaced, run through original checks a second time
            if (obj != orig) {
                subs.assign(orig, obj);
                if (obj == null) {
                    writeNull();
                    return;
                } else if (!unshared && (h = handles.lookup(obj)) != -1) {
                    writeHandle(h);
                    return;
                } else if (obj instanceof Class) {
                    writeClass((Class) obj, unshared);
                    return;
                } else if (obj instanceof ObjectStreamClass) {
                    writeClassDesc((ObjectStreamClass) obj, unshared);
                    return;
                }
            }

            // remaining cases
            if (obj instanceof String) {
                writeString((String) obj, unshared);
            } else if (cl.isArray()) {
                writeArray(obj, desc, unshared);
            } else if (obj instanceof Enum) {
                writeEnum((Enum<?>) obj, desc, unshared);
            } else if (obj instanceof Serializable) {
                writeOrdinaryObject(obj, desc, unshared);
            } else {
                if (extendedDebugInfo) {
                    throw new NotSerializableException(
                        cl.getName() + "\n" + debugInfoStack.toString());
                } else {
                    throw new NotSerializableException(cl.getName());
                }
            }
        } finally {
            depth--;
            bout.setBlockDataMode(oldMode);
        }
    }

    /**
     * Writes null code to stream.
     */
    private void writeNull() throws IOException {
        bout.writeByte(TC_NULL);
    }

    /**
     * Writes given object handle to stream.
     */
    private void writeHandle(int handle) throws IOException {
        bout.writeByte(TC_REFERENCE);
        bout.writeInt(baseWireHandle + handle);
    }

    /**
     * Writes representation of given class to stream.
     */
    private void writeClass(Class<?> cl, boolean unshared) throws IOException {
        bout.writeByte(TC_CLASS);
        writeClassDesc(ObjectStreamClass.lookup(cl, true), false);
        handles.assign(unshared ? null : cl);
    }

    /**
     * Writes representation of given class descriptor to stream.
     */
    private void writeClassDesc(ObjectStreamClass desc, boolean unshared)
        throws IOException
    {
        int handle;
        if (desc == null) {
            writeNull();
        } else if (!unshared && (handle = handles.lookup(desc)) != -1) {
            writeHandle(handle);
        } else if (desc.isProxy()) {
            writeProxyDesc(desc, unshared);
        } else {
            writeNonProxyDesc(desc, unshared);
        }
    }

    private boolean isCustomSubclass() {
        // Return true if this class is a custom subclass of ObjectOutputStream
        return getClass().getClassLoader()
                   != ObjectOutputStream.class.getClassLoader();
    }

    /**
     * Writes class descriptor representing a dynamic proxy class to stream.
     */
    private void writeProxyDesc(ObjectStreamClass desc, boolean unshared)
        throws IOException
    {
        bout.writeByte(TC_PROXYCLASSDESC);
        handles.assign(unshared ? null : desc);

        Class<?> cl = desc.forClass();
        Class<?>[] ifaces = cl.getInterfaces();
        bout.writeInt(ifaces.length);
        for (int i = 0; i < ifaces.length; i++) {
            bout.writeUTF(ifaces[i].getName());
        }

        bout.setBlockDataMode(true);
        if (cl != null && isCustomSubclass()) {
            ReflectUtil.checkPackageAccess(cl);
        }
        annotateProxyClass(cl);
        bout.setBlockDataMode(false);
        bout.writeByte(TC_ENDBLOCKDATA);

        writeClassDesc(desc.getSuperDesc(), false);
    }

    /**
     * Writes class descriptor representing a standard (i.e., not a dynamic
     * proxy) class to stream.
     */
    private void writeNonProxyDesc(ObjectStreamClass desc, boolean unshared)
        throws IOException
    {
        bout.writeByte(TC_CLASSDESC);
        handles.assign(unshared ? null : desc);

        if (protocol == PROTOCOL_VERSION_1) {
            // do not invoke class descriptor write hook with old protocol
            desc.writeNonProxy(this);
        } else {
            writeClassDescriptor(desc);
        }

        Class<?> cl = desc.forClass();
        bout.setBlockDataMode(true);
        if (cl != null && isCustomSubclass()) {
            ReflectUtil.checkPackageAccess(cl);
        }
        annotateClass(cl);
        bout.setBlockDataMode(false);
        bout.writeByte(TC_ENDBLOCKDATA);

        writeClassDesc(desc.getSuperDesc(), false);
    }

    /**
     * Writes given string to stream, using standard or long UTF format
     * depending on string length.
     */
    private void writeString(String str, boolean unshared) throws IOException {
        handles.assign(unshared ? null : str);
        long utflen = bout.getUTFLength(str);
        if (utflen <= 0xFFFF) {
            bout.writeByte(TC_STRING);
            bout.writeUTF(str, utflen);
        } else {
            bout.writeByte(TC_LONGSTRING);
            bout.writeLongUTF(str, utflen);
        }
    }

    /**
     * Writes given array object to stream.
     */
    private void writeArray(Object array,
                            ObjectStreamClass desc,
                            boolean unshared)
        throws IOException
    {
        bout.writeByte(TC_ARRAY);
        writeClassDesc(desc, false);
        handles.assign(unshared ? null : array);

        Class<?> ccl = desc.forClass().getComponentType();
        if (ccl.isPrimitive()) {
            if (ccl == Integer.TYPE) {
                int[] ia = (int[]) array;
                bout.writeInt(ia.length);
                bout.writeInts(ia, 0, ia.length);
            } else if (ccl == Byte.TYPE) {
                byte[] ba = (byte[]) array;
                bout.writeInt(ba.length);
                bout.write(ba, 0, ba.length, true);
            } else if (ccl == Long.TYPE) {
                long[] ja = (long[]) array;
                bout.writeInt(ja.length);
                bout.writeLongs(ja, 0, ja.length);
            } else if (ccl == Float.TYPE) {
                float[] fa = (float[]) array;
                bout.writeInt(fa.length);
                bout.writeFloats(fa, 0, fa.length);
            } else if (ccl == Double.TYPE) {
                double[] da = (double[]) array;
                bout.writeInt(da.length);
                bout.writeDoubles(da, 0, da.length);
            } else if (ccl == Short.TYPE) {
                short[] sa = (short[]) array;
                bout.writeInt(sa.length);
                bout.writeShorts(sa, 0, sa.length);
            } else if (ccl == Character.TYPE) {
                char[] ca = (char[]) array;
                bout.writeInt(ca.length);
                bout.writeChars(ca, 0, ca.length);
            } else if (ccl == Boolean.TYPE) {
                boolean[] za = (boolean[]) array;
                bout.writeInt(za.length);
                bout.writeBooleans(za, 0, za.length);
            } else {
                throw new InternalError();
            }
        } else {
            Object[] objs = (Object[]) array;
            int len = objs.length;
            bout.writeInt(len);
            if (extendedDebugInfo) {
                debugInfoStack.push(
                    "array (class \"" + array.getClass().getName() +
                    "\", size: " + len  + ")");
            }
            try {
                for (int i = 0; i < len; i++) {
                    if (extendedDebugInfo) {
                        debugInfoStack.push(
                            "element of array (index: " + i + ")");
                    }
                    try {
                        writeObject0(objs[i], false);
                    } finally {
                        if (extendedDebugInfo) {
                            debugInfoStack.pop();
                        }
                    }
                }
            } finally {
                if (extendedDebugInfo) {
                    debugInfoStack.pop();
                }
            }
        }
    }

    /**
     * Writes given enum constant to stream.
     */
    private void writeEnum(Enum<?> en,
                           ObjectStreamClass desc,
                           boolean unshared)
        throws IOException
    {
        bout.writeByte(TC_ENUM);
        ObjectStreamClass sdesc = desc.getSuperDesc();
        writeClassDesc((sdesc.forClass() == Enum.class) ? desc : sdesc, false);
        handles.assign(unshared ? null : en);
        writeString(en.name(), false);
    }

    /**
     * Writes representation of a "ordinary" (i.e., not a String, Class,
     * ObjectStreamClass, array, or enum constant) serializable object to the
     * stream.
     */
    private void writeOrdinaryObject(Object obj,
                                     ObjectStreamClass desc,
                                     boolean unshared)
        throws IOException
    {
        if (extendedDebugInfo) {
            debugInfoStack.push(
                (depth == 1 ? "root " : "") + "object (class \"" +
                obj.getClass().getName() + "\", " + obj.toString() + ")");
        }
        try {
            desc.checkSerialize();

            bout.writeByte(TC_OBJECT);
            writeClassDesc(desc, false);
            handles.assign(unshared ? null : obj);

            if (desc.isRecord()) {
                writeRecordData(obj, desc);
            } else if (desc.isExternalizable() && !desc.isProxy()) {
                writeExternalData((Externalizable) obj);
            } else {
                writeSerialData(obj, desc);
            }
        } finally {
            if (extendedDebugInfo) {
                debugInfoStack.pop();
            }
        }
    }

    /**
     * Writes externalizable data of given object by invoking its
     * writeExternal() method.
     */
    private void writeExternalData(Externalizable obj) throws IOException {
        PutFieldImpl oldPut = curPut;
        curPut = null;

        if (extendedDebugInfo) {
            debugInfoStack.push("writeExternal data");
        }
        SerialCallbackContext oldContext = curContext;
        try {
            curContext = null;
            if (protocol == PROTOCOL_VERSION_1) {
                obj.writeExternal(this);
            } else {
                bout.setBlockDataMode(true);
                obj.writeExternal(this);
                bout.setBlockDataMode(false);
                bout.writeByte(TC_ENDBLOCKDATA);
            }
        } finally {
            curContext = oldContext;
            if (extendedDebugInfo) {
                debugInfoStack.pop();
            }
        }

        curPut = oldPut;
    }

    /** Writes the record component values for the given record object. */
    private void writeRecordData(Object obj, ObjectStreamClass desc)
        throws IOException
    {
        assert obj.getClass().isRecord();
        ObjectStreamClass.ClassDataSlot[] slots = desc.getClassDataLayout();
        if (slots.length != 1) {
            throw new InvalidClassException(
                    "expected a single record slot length, but found: " + slots.length);
        }

        defaultWriteFields(obj, desc);  // #### seems unnecessary to use the accessors
    }

    /**
     * Writes instance data for each serializable class of given object, from
     * superclass to subclass.
     */
    private void writeSerialData(Object obj, ObjectStreamClass desc)
        throws IOException
    {
        ObjectStreamClass.ClassDataSlot[] slots = desc.getClassDataLayout();
        for (int i = 0; i < slots.length; i++) {
            ObjectStreamClass slotDesc = slots[i].desc;
            if (slotDesc.hasWriteObjectMethod()) {
                PutFieldImpl oldPut = curPut;
                curPut = null;
                SerialCallbackContext oldContext = curContext;

                if (extendedDebugInfo) {
                    debugInfoStack.push(
                        "custom writeObject data (class \"" +
                        slotDesc.getName() + "\")");
                }
                try {
                    curContext = new SerialCallbackContext(obj, slotDesc);
                    bout.setBlockDataMode(true);
                    slotDesc.invokeWriteObject(obj, this);
                    bout.setBlockDataMode(false);
                    bout.writeByte(TC_ENDBLOCKDATA);
                } finally {
                    curContext.setUsed();
                    curContext = oldContext;
                    if (extendedDebugInfo) {
                        debugInfoStack.pop();
                    }
                }

                curPut = oldPut;
            } else {
                defaultWriteFields(obj, slotDesc);
            }
        }
    }

    /**
     * Fetches and writes values of serializable fields of given object to
     * stream.  The given class descriptor specifies which field values to
     * write, and in which order they should be written.
     */
    private void defaultWriteFields(Object obj, ObjectStreamClass desc)
        throws IOException
    {
        Class<?> cl = desc.forClass();
        if (cl != null && obj != null && !cl.isInstance(obj)) {
            throw new ClassCastException();
        }

        desc.checkDefaultSerialize();

        int primDataSize = desc.getPrimDataSize();
        if (primDataSize > 0) {
            if (primVals == null || primVals.length < primDataSize) {
                primVals = new byte[primDataSize];
            }
            desc.getPrimFieldValues(obj, primVals);
            bout.write(primVals, 0, primDataSize, false);
        }

        int numObjFields = desc.getNumObjFields();
        if (numObjFields > 0) {
            ObjectStreamField[] fields = desc.getFields(false);
            Object[] objVals = new Object[numObjFields];
            int numPrimFields = fields.length - objVals.length;
            desc.getObjFieldValues(obj, objVals);
            for (int i = 0; i < objVals.length; i++) {
                if (extendedDebugInfo) {
                    debugInfoStack.push(
                        "field (class \"" + desc.getName() + "\", name: \"" +
                        fields[numPrimFields + i].getName() + "\", type: \"" +
                        fields[numPrimFields + i].getType() + "\")");
                }
                try {
                    writeObject0(objVals[i],
                                 fields[numPrimFields + i].isUnshared());
                } finally {
                    if (extendedDebugInfo) {
                        debugInfoStack.pop();
                    }
                }
            }
        }
    }

    /**
     * Attempts to write to stream fatal IOException that has caused
     * serialization to abort.
     */
    private void writeFatalException(IOException ex) throws IOException {
        /*
         * Note: the serialization specification states that if a second
         * IOException occurs while attempting to serialize the original fatal
         * exception to the stream, then a StreamCorruptedException should be
         * thrown (section 2.1).  However, due to a bug in previous
         * implementations of serialization, StreamCorruptedExceptions were
         * rarely (if ever) actually thrown--the "root" exceptions from
         * underlying streams were thrown instead.  This historical behavior is
         * followed here for consistency.
         */
        clear();
        boolean oldMode = bout.setBlockDataMode(false);
        try {
            bout.writeByte(TC_EXCEPTION);
            writeObject0(ex, false);
            clear();
        } finally {
            bout.setBlockDataMode(oldMode);
        }
    }

    /**
     * Default PutField implementation.
     */
    private class PutFieldImpl extends PutField {

        /** class descriptor describing serializable fields */
        private final ObjectStreamClass desc;
        /** primitive field values */
        private final byte[] primVals;
        /** object field values */
        private final Object[] objVals;

        /**
         * Creates PutFieldImpl object for writing fields defined in given
         * class descriptor.
         */
        PutFieldImpl(ObjectStreamClass desc) {
            this.desc = desc;
            primVals = new byte[desc.getPrimDataSize()];
            objVals = new Object[desc.getNumObjFields()];
        }

        public void put(String name, boolean val) {
            Bits.putBoolean(primVals, getFieldOffset(name, Boolean.TYPE), val);
        }

        public void put(String name, byte val) {
            primVals[getFieldOffset(name, Byte.TYPE)] = val;
        }

        public void put(String name, char val) {
            Bits.putChar(primVals, getFieldOffset(name, Character.TYPE), val);
        }

        public void put(String name, short val) {
            Bits.putShort(primVals, getFieldOffset(name, Short.TYPE), val);
        }

        public void put(String name, int val) {
            Bits.putInt(primVals, getFieldOffset(name, Integer.TYPE), val);
        }

        public void put(String name, float val) {
            Bits.putFloat(primVals, getFieldOffset(name, Float.TYPE), val);
        }

        public void put(String name, long val) {
            Bits.putLong(primVals, getFieldOffset(name, Long.TYPE), val);
        }

        public void put(String name, double val) {
            Bits.putDouble(primVals, getFieldOffset(name, Double.TYPE), val);
        }

        public void put(String name, Object val) {
            objVals[getFieldOffset(name, Object.class)] = val;
        }

        // deprecated in ObjectOutputStream.PutField
        public void write(ObjectOutput out) throws IOException {
            /*
             * Applications should *not* use this method to write PutField
             * data, as it will lead to stream corruption if the PutField
             * object writes any primitive data (since block data mode is not
             * unset/set properly, as is done in OOS.writeFields()).  This
             * broken implementation is being retained solely for behavioral
             * compatibility, in order to support applications which use
             * OOS.PutField.write() for writing only non-primitive data.
             *
             * Serialization of unshared objects is not implemented here since
             * it is not necessary for backwards compatibility; also, unshared
             * semantics may not be supported by the given ObjectOutput
             * instance.  Applications which write unshared objects using the
             * PutField API must use OOS.writeFields().
             */
            if (ObjectOutputStream.this != out) {
                throw new IllegalArgumentException("wrong stream");
            }
            out.write(primVals, 0, primVals.length);

            ObjectStreamField[] fields = desc.getFields(false);
            int numPrimFields = fields.length - objVals.length;
            // REMIND: warn if numPrimFields > 0?
            for (int i = 0; i < objVals.length; i++) {
                if (fields[numPrimFields + i].isUnshared()) {
                    throw new IOException("cannot write unshared object");
                }
                out.writeObject(objVals[i]);
            }
        }

        /**
         * Writes buffered primitive data and object fields to stream.
         */
        void writeFields() throws IOException {
            bout.write(primVals, 0, primVals.length, false);

            ObjectStreamField[] fields = desc.getFields(false);
            int numPrimFields = fields.length - objVals.length;
            for (int i = 0; i < objVals.length; i++) {
                if (extendedDebugInfo) {
                    debugInfoStack.push(
                        "field (class \"" + desc.getName() + "\", name: \"" +
                        fields[numPrimFields + i].getName() + "\", type: \"" +
                        fields[numPrimFields + i].getType() + "\")");
                }
                try {
                    writeObject0(objVals[i],
                                 fields[numPrimFields + i].isUnshared());
                } finally {
                    if (extendedDebugInfo) {
                        debugInfoStack.pop();
                    }
                }
            }
        }

        /**
         * Returns offset of field with given name and type.  A specified type
         * of null matches all types, Object.class matches all non-primitive
         * types, and any other non-null type matches assignable types only.
         * Throws IllegalArgumentException if no matching field found.
         */
        private int getFieldOffset(String name, Class<?> type) {
            ObjectStreamField field = desc.getField(name, type);
            if (field == null) {
                throw new IllegalArgumentException("no such field " + name +
                                                   " with type " + type);
            }
            return field.getOffset();
        }
    }

    /**
     * Buffered output stream with two modes: in default mode, outputs data in
     * same format as DataOutputStream; in "block data" mode, outputs data
     * bracketed by block data markers (see object serialization specification
     * for details).
     */
    private static class BlockDataOutputStream
        extends OutputStream implements DataOutput
    {
        /** maximum data block length */
        private static final int MAX_BLOCK_SIZE = 1024;
        /** maximum data block header length */
        private static final int MAX_HEADER_SIZE = 5;
        /** (tunable) length of char buffer (for writing strings) */
        private static final int CHAR_BUF_SIZE = 256;

        /** buffer for writing general/block data */
        private final byte[] buf = new byte[MAX_BLOCK_SIZE];
        /** buffer for writing block data headers */
        private final byte[] hbuf = new byte[MAX_HEADER_SIZE];
        /** char buffer for fast string writes */
        private final char[] cbuf = new char[CHAR_BUF_SIZE];

        /** block data mode */
        private boolean blkmode = false;
        /** current offset into buf */
        private int pos = 0;

        /** underlying output stream */
        private final OutputStream out;
        /** loopback stream (for data writes that span data blocks) */
        private final DataOutputStream dout;

        /**
         * Creates new BlockDataOutputStream on top of given underlying stream.
         * Block data mode is turned off by default.
         */
        BlockDataOutputStream(OutputStream out) {
            this.out = out;
            dout = new DataOutputStream(this);
        }

        /**
         * Sets block data mode to the given mode (true == on, false == off)
         * and returns the previous mode value.  If the new mode is the same as
         * the old mode, no action is taken.  If the new mode differs from the
         * old mode, any buffered data is flushed before switching to the new
         * mode.
         */
        boolean setBlockDataMode(boolean mode) throws IOException {
            if (blkmode == mode) {
                return blkmode;
            }
            drain();
            blkmode = mode;
            return !blkmode;
        }

        /**
         * Returns true if the stream is currently in block data mode, false
         * otherwise.
         */
        boolean getBlockDataMode() {
            return blkmode;
        }

        /* ----------------- generic output stream methods ----------------- */
        /*
         * The following methods are equivalent to their counterparts in
         * OutputStream, except that they partition written data into data
         * blocks when in block data mode.
         */

        public void write(int b) throws IOException {
            if (pos >= MAX_BLOCK_SIZE) {
                drain();
            }
            buf[pos++] = (byte) b;
        }

        public void write(byte[] b) throws IOException {
            write(b, 0, b.length, false);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            write(b, off, len, false);
        }

        public void flush() throws IOException {
            drain();
            out.flush();
        }

        public void close() throws IOException {
            flush();
            out.close();
        }

        /**
         * Writes specified span of byte values from given array.  If copy is
         * true, copies the values to an intermediate buffer before writing
         * them to underlying stream (to avoid exposing a reference to the
         * original byte array).
         */
        void write(byte[] b, int off, int len, boolean copy)
            throws IOException
        {
            if (!(copy || blkmode)) {           // write directly
                drain();
                out.write(b, off, len);
                return;
            }

            while (len > 0) {
                if (pos >= MAX_BLOCK_SIZE) {
                    drain();
                }
                if (len >= MAX_BLOCK_SIZE && !copy && pos == 0) {
                    // avoid unnecessary copy
                    writeBlockHeader(MAX_BLOCK_SIZE);
                    out.write(b, off, MAX_BLOCK_SIZE);
                    off += MAX_BLOCK_SIZE;
                    len -= MAX_BLOCK_SIZE;
                } else {
                    int wlen = Math.min(len, MAX_BLOCK_SIZE - pos);
                    System.arraycopy(b, off, buf, pos, wlen);
                    pos += wlen;
                    off += wlen;
                    len -= wlen;
                }
            }
        }

        /**
         * Writes all buffered data from this stream to the underlying stream,
         * but does not flush underlying stream.
         */
        void drain() throws IOException {
            if (pos == 0) {
                return;
            }
            if (blkmode) {
                writeBlockHeader(pos);
            }
            out.write(buf, 0, pos);
            pos = 0;
        }

        /**
         * Writes block data header.  Data blocks shorter than 256 bytes are
         * prefixed with a 2-byte header; all others start with a 5-byte
         * header.
         */
        private void writeBlockHeader(int len) throws IOException {
            if (len <= 0xFF) {
                hbuf[0] = TC_BLOCKDATA;
                hbuf[1] = (byte) len;
                out.write(hbuf, 0, 2);
            } else {
                hbuf[0] = TC_BLOCKDATALONG;
                Bits.putInt(hbuf, 1, len);
                out.write(hbuf, 0, 5);
            }
        }


        /* ----------------- primitive data output methods ----------------- */
        /*
         * The following methods are equivalent to their counterparts in
         * DataOutputStream, except that they partition written data into data
         * blocks when in block data mode.
         */

        public void writeBoolean(boolean v) throws IOException {
            if (pos >= MAX_BLOCK_SIZE) {
                drain();
            }
            Bits.putBoolean(buf, pos++, v);
        }

        public void writeByte(int v) throws IOException {
            if (pos >= MAX_BLOCK_SIZE) {
                drain();
            }
            buf[pos++] = (byte) v;
        }

        public void writeChar(int v) throws IOException {
            if (pos + 2 <= MAX_BLOCK_SIZE) {
                Bits.putChar(buf, pos, (char) v);
                pos += 2;
            } else {
                dout.writeChar(v);
            }
        }

        public void writeShort(int v) throws IOException {
            if (pos + 2 <= MAX_BLOCK_SIZE) {
                Bits.putShort(buf, pos, (short) v);
                pos += 2;
            } else {
                dout.writeShort(v);
            }
        }

        public void writeInt(int v) throws IOException {
            if (pos + 4 <= MAX_BLOCK_SIZE) {
                Bits.putInt(buf, pos, v);
                pos += 4;
            } else {
                dout.writeInt(v);
            }
        }

        public void writeFloat(float v) throws IOException {
            if (pos + 4 <= MAX_BLOCK_SIZE) {
                Bits.putFloat(buf, pos, v);
                pos += 4;
            } else {
                dout.writeFloat(v);
            }
        }

        public void writeLong(long v) throws IOException {
            if (pos + 8 <= MAX_BLOCK_SIZE) {
                Bits.putLong(buf, pos, v);
                pos += 8;
            } else {
                dout.writeLong(v);
            }
        }

        public void writeDouble(double v) throws IOException {
            if (pos + 8 <= MAX_BLOCK_SIZE) {
                Bits.putDouble(buf, pos, v);
                pos += 8;
            } else {
                dout.writeDouble(v);
            }
        }

        public void writeBytes(String s) throws IOException {
            int endoff = s.length();
            int cpos = 0;
            int csize = 0;
            for (int off = 0; off < endoff; ) {
                if (cpos >= csize) {
                    cpos = 0;
                    csize = Math.min(endoff - off, CHAR_BUF_SIZE);
                    s.getChars(off, off + csize, cbuf, 0);
                }
                if (pos >= MAX_BLOCK_SIZE) {
                    drain();
                }
                int n = Math.min(csize - cpos, MAX_BLOCK_SIZE - pos);
                int stop = pos + n;
                while (pos < stop) {
                    buf[pos++] = (byte) cbuf[cpos++];
                }
                off += n;
            }
        }

        public void writeChars(String s) throws IOException {
            int endoff = s.length();
            for (int off = 0; off < endoff; ) {
                int csize = Math.min(endoff - off, CHAR_BUF_SIZE);
                s.getChars(off, off + csize, cbuf, 0);
                writeChars(cbuf, 0, csize);
                off += csize;
            }
        }

        public void writeUTF(String s) throws IOException {
            writeUTF(s, getUTFLength(s));
        }


        /* -------------- primitive data array output methods -------------- */
        /*
         * The following methods write out spans of primitive data values.
         * Though equivalent to calling the corresponding primitive write
         * methods repeatedly, these methods are optimized for writing groups
         * of primitive data values more efficiently.
         */

        void writeBooleans(boolean[] v, int off, int len) throws IOException {
            int endoff = off + len;
            while (off < endoff) {
                if (pos >= MAX_BLOCK_SIZE) {
                    drain();
                }
                int stop = Math.min(endoff, off + (MAX_BLOCK_SIZE - pos));
                while (off < stop) {
                    Bits.putBoolean(buf, pos++, v[off++]);
                }
            }
        }

        void writeChars(char[] v, int off, int len) throws IOException {
            int limit = MAX_BLOCK_SIZE - 2;
            int endoff = off + len;
            while (off < endoff) {
                if (pos <= limit) {
                    int avail = (MAX_BLOCK_SIZE - pos) >> 1;
                    int stop = Math.min(endoff, off + avail);
                    while (off < stop) {
                        Bits.putChar(buf, pos, v[off++]);
                        pos += 2;
                    }
                } else {
                    dout.writeChar(v[off++]);
                }
            }
        }

        void writeShorts(short[] v, int off, int len) throws IOException {
            int limit = MAX_BLOCK_SIZE - 2;
            int endoff = off + len;
            while (off < endoff) {
                if (pos <= limit) {
                    int avail = (MAX_BLOCK_SIZE - pos) >> 1;
                    int stop = Math.min(endoff, off + avail);
                    while (off < stop) {
                        Bits.putShort(buf, pos, v[off++]);
                        pos += 2;
                    }
                } else {
                    dout.writeShort(v[off++]);
                }
            }
        }

        void writeInts(int[] v, int off, int len) throws IOException {
            int limit = MAX_BLOCK_SIZE - 4;
            int endoff = off + len;
            while (off < endoff) {
                if (pos <= limit) {
                    int avail = (MAX_BLOCK_SIZE - pos) >> 2;
                    int stop = Math.min(endoff, off + avail);
                    while (off < stop) {
                        Bits.putInt(buf, pos, v[off++]);
                        pos += 4;
                    }
                } else {
                    dout.writeInt(v[off++]);
                }
            }
        }

        void writeFloats(float[] v, int off, int len) throws IOException {
            int limit = MAX_BLOCK_SIZE - 4;
            int endoff = off + len;
            while (off < endoff) {
                if (pos <= limit) {
                    int avail = (MAX_BLOCK_SIZE - pos) >> 2;
                    int stop = Math.min(endoff, off + avail);
                    while (off < stop) {
                        Bits.putFloat(buf, pos, v[off++]);
                        pos += 4;
                    }
                } else {
                    dout.writeFloat(v[off++]);
                }
            }
        }

        void writeLongs(long[] v, int off, int len) throws IOException {
            int limit = MAX_BLOCK_SIZE - 8;
            int endoff = off + len;
            while (off < endoff) {
                if (pos <= limit) {
                    int avail = (MAX_BLOCK_SIZE - pos) >> 3;
                    int stop = Math.min(endoff, off + avail);
                    while (off < stop) {
                        Bits.putLong(buf, pos, v[off++]);
                        pos += 8;
                    }
                } else {
                    dout.writeLong(v[off++]);
                }
            }
        }

        void writeDoubles(double[] v, int off, int len) throws IOException {
            int limit = MAX_BLOCK_SIZE - 8;
            int endoff = off + len;
            while (off < endoff) {
                if (pos <= limit) {
                    int avail = (MAX_BLOCK_SIZE - pos) >> 3;
                    int stop = Math.min(endoff, off + avail);
                    while (off < stop) {
                        Bits.putDouble(buf, pos, v[off++]);
                        pos += 8;
                    }
                } else {
                    dout.writeDouble(v[off++]);
                }
            }
        }

        /**
         * Returns the length in bytes of the UTF encoding of the given string.
         */
        long getUTFLength(String s) {
            int len = s.length();
            long utflen = 0;
            for (int off = 0; off < len; ) {
                int csize = Math.min(len - off, CHAR_BUF_SIZE);
                s.getChars(off, off + csize, cbuf, 0);
                for (int cpos = 0; cpos < csize; cpos++) {
                    char c = cbuf[cpos];
                    if (c >= 0x0001 && c <= 0x007F) {
                        utflen++;
                    } else if (c > 0x07FF) {
                        utflen += 3;
                    } else {
                        utflen += 2;
                    }
                }
                off += csize;
            }
            return utflen;
        }

        /**
         * Writes the given string in UTF format.  This method is used in
         * situations where the UTF encoding length of the string is already
         * known; specifying it explicitly avoids a prescan of the string to
         * determine its UTF length.
         */
        void writeUTF(String s, long utflen) throws IOException {
            if (utflen > 0xFFFFL) {
                throw new UTFDataFormatException();
            }
            writeShort((int) utflen);
            if (utflen == (long) s.length()) {
                writeBytes(s);
            } else {
                writeUTFBody(s);
            }
        }

        /**
         * Writes given string in "long" UTF format.  "Long" UTF format is
         * identical to standard UTF, except that it uses an 8 byte header
         * (instead of the standard 2 bytes) to convey the UTF encoding length.
         */
        void writeLongUTF(String s) throws IOException {
            writeLongUTF(s, getUTFLength(s));
        }

        /**
         * Writes given string in "long" UTF format, where the UTF encoding
         * length of the string is already known.
         */
        void writeLongUTF(String s, long utflen) throws IOException {
            writeLong(utflen);
            if (utflen == (long) s.length()) {
                writeBytes(s);
            } else {
                writeUTFBody(s);
            }
        }

        /**
         * Writes the "body" (i.e., the UTF representation minus the 2-byte or
         * 8-byte length header) of the UTF encoding for the given string.
         */
        private void writeUTFBody(String s) throws IOException {
            int limit = MAX_BLOCK_SIZE - 3;
            int len = s.length();
            for (int off = 0; off < len; ) {
                int csize = Math.min(len - off, CHAR_BUF_SIZE);
                s.getChars(off, off + csize, cbuf, 0);
                for (int cpos = 0; cpos < csize; cpos++) {
                    char c = cbuf[cpos];
                    if (pos <= limit) {
                        if (c <= 0x007F && c != 0) {
                            buf[pos++] = (byte) c;
                        } else if (c > 0x07FF) {
                            buf[pos + 2] = (byte) (0x80 | ((c >> 0) & 0x3F));
                            buf[pos + 1] = (byte) (0x80 | ((c >> 6) & 0x3F));
                            buf[pos + 0] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                            pos += 3;
                        } else {
                            buf[pos + 1] = (byte) (0x80 | ((c >> 0) & 0x3F));
                            buf[pos + 0] = (byte) (0xC0 | ((c >> 6) & 0x1F));
                            pos += 2;
                        }
                    } else {    // write one byte at a time to normalize block
                        if (c <= 0x007F && c != 0) {
                            write(c);
                        } else if (c > 0x07FF) {
                            write(0xE0 | ((c >> 12) & 0x0F));
                            write(0x80 | ((c >> 6) & 0x3F));
                            write(0x80 | ((c >> 0) & 0x3F));
                        } else {
                            write(0xC0 | ((c >> 6) & 0x1F));
                            write(0x80 | ((c >> 0) & 0x3F));
                        }
                    }
                }
                off += csize;
            }
        }
    }

    /**
     * Lightweight identity hash table which maps objects to integer handles,
     * assigned in ascending order.
     */
    private static class HandleTable {

        /* number of mappings in table/next available handle */
        private int size;
        /* size threshold determining when to expand hash spine */
        private int threshold;
        /* factor for computing size threshold */
        private final float loadFactor;
        /* maps hash value -> candidate handle value */
        private int[] spine;
        /* maps handle value -> next candidate handle value */
        private int[] next;
        /* maps handle value -> associated object */
        private Object[] objs;

        /**
         * Creates new HandleTable with given capacity and load factor.
         */
        HandleTable(int initialCapacity, float loadFactor) {
            this.loadFactor = loadFactor;
            spine = new int[initialCapacity];
            next = new int[initialCapacity];
            objs = new Object[initialCapacity];
            threshold = (int) (initialCapacity * loadFactor);
            clear();
        }

        /**
         * Assigns next available handle to given object, and returns handle
         * value.  Handles are assigned in ascending order starting at 0.
         */
        int assign(Object obj) {
            if (size >= next.length) {
                growEntries();
            }
            if (size >= threshold) {
                growSpine();
            }
            insert(obj, size);
            return size++;
        }

        /**
         * Looks up and returns handle associated with given object, or -1 if
         * no mapping found.
         */
        int lookup(Object obj) {
            if (size == 0) {
                return -1;
            }
            int index = hash(obj) % spine.length;
            for (int i = spine[index]; i >= 0; i = next[i]) {
                if (objs[i] == obj) {
                    return i;
                }
            }
            return -1;
        }

        /**
         * Resets table to its initial (empty) state.
         */
        void clear() {
            Arrays.fill(spine, -1);
            Arrays.fill(objs, 0, size, null);
            size = 0;
        }

        /**
         * Returns the number of mappings currently in table.
         */
        int size() {
            return size;
        }

        /**
         * Inserts mapping object -> handle mapping into table.  Assumes table
         * is large enough to accommodate new mapping.
         */
        private void insert(Object obj, int handle) {
            int index = hash(obj) % spine.length;
            objs[handle] = obj;
            next[handle] = spine[index];
            spine[index] = handle;
        }

        /**
         * Expands the hash "spine" -- equivalent to increasing the number of
         * buckets in a conventional hash table.
         */
        private void growSpine() {
            spine = new int[(spine.length << 1) + 1];
            threshold = (int) (spine.length * loadFactor);
            Arrays.fill(spine, -1);
            for (int i = 0; i < size; i++) {
                insert(objs[i], i);
            }
        }

        /**
         * Increases hash table capacity by lengthening entry arrays.
         */
        private void growEntries() {
            int newLength = (next.length << 1) + 1;
            int[] newNext = new int[newLength];
            System.arraycopy(next, 0, newNext, 0, size);
            next = newNext;

            Object[] newObjs = new Object[newLength];
            System.arraycopy(objs, 0, newObjs, 0, size);
            objs = newObjs;
        }

        /**
         * Returns hash value for given object.
         */
        private int hash(Object obj) {
            return System.identityHashCode(obj) & 0x7FFFFFFF;
        }
    }

    /**
     * Lightweight identity hash table which maps objects to replacement
     * objects.
     */
    private static class ReplaceTable {

        /* maps object -> index */
        private final HandleTable htab;
        /* maps index -> replacement object */
        private Object[] reps;

        /**
         * Creates new ReplaceTable with given capacity and load factor.
         */
        ReplaceTable(int initialCapacity, float loadFactor) {
            htab = new HandleTable(initialCapacity, loadFactor);
            reps = new Object[initialCapacity];
        }

        /**
         * Enters mapping from object to replacement object.
         */
        void assign(Object obj, Object rep) {
            int index = htab.assign(obj);
            while (index >= reps.length) {
                grow();
            }
            reps[index] = rep;
        }

        /**
         * Looks up and returns replacement for given object.  If no
         * replacement is found, returns the lookup object itself.
         */
        Object lookup(Object obj) {
            int index = htab.lookup(obj);
            return (index >= 0) ? reps[index] : obj;
        }

        /**
         * Resets table to its initial (empty) state.
         */
        void clear() {
            Arrays.fill(reps, 0, htab.size(), null);
            htab.clear();
        }

        /**
         * Returns the number of mappings currently in table.
         */
        int size() {
            return htab.size();
        }

        /**
         * Increases table capacity.
         */
        private void grow() {
            Object[] newReps = new Object[(reps.length << 1) + 1];
            System.arraycopy(reps, 0, newReps, 0, reps.length);
            reps = newReps;
        }
    }

    /**
     * Stack to keep debug information about the state of the
     * serialization process, for embedding in exception messages.
     */
    private static class DebugTraceInfoStack {
        private final List<String> stack;

        DebugTraceInfoStack() {
            stack = new ArrayList<>();
        }

        /**
         * Removes all of the elements from enclosed list.
         */
        void clear() {
            stack.clear();
        }

        /**
         * Removes the object at the top of enclosed list.
         */
        void pop() {
            stack.remove(stack.size()-1);
        }

        /**
         * Pushes a String onto the top of enclosed list.
         */
        void push(String entry) {
            stack.add("\t- " + entry);
        }

        /**
         * Returns a string representation of this object
         */
        public String toString() {
            StringJoiner sj = new StringJoiner("\n");
            for (int i = stack.size() - 1; i >= 0; i--) {
                sj.add(stack.get(i));
            }
            return sj.toString();
        }
    }

}
