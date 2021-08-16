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

import java.io.ObjectInputFilter.Config;
import java.io.ObjectStreamClass.WeakClassKey;
import java.io.ObjectStreamClass.RecordSupport;
import java.lang.System.Logger;
import java.lang.invoke.MethodHandle;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.io.ObjectStreamClass.processQueue;

import jdk.internal.access.SharedSecrets;
import jdk.internal.event.DeserializationEvent;
import jdk.internal.misc.Unsafe;
import sun.reflect.misc.ReflectUtil;
import sun.security.action.GetBooleanAction;
import sun.security.action.GetIntegerAction;

/**
 * An ObjectInputStream deserializes primitive data and objects previously
 * written using an ObjectOutputStream.
 *
 * <p><strong>Warning: Deserialization of untrusted data is inherently dangerous
 * and should be avoided. Untrusted data should be carefully validated according to the
 * "Serialization and Deserialization" section of the
 * {@extLink secure_coding_guidelines_javase Secure Coding Guidelines for Java SE}.
 * {@extLink serialization_filter_guide Serialization Filtering} describes best
 * practices for defensive use of serial filters.
 * </strong></p>
 *
 * <p>The key to disabling deserialization attacks is to prevent instances of
 * arbitrary classes from being deserialized, thereby preventing the direct or
 * indirect execution of their methods.
 * {@link ObjectInputFilter} describes how to use filters and
 * {@link ObjectInputFilter.Config} describes how to configure the filter and filter factory.
 * Each stream has an optional deserialization filter
 * to check the classes and resource limits during deserialization.
 * The JVM-wide filter factory ensures that a filter can be set on every {@link ObjectInputStream}
 * and every object read from the stream can be checked.
 * The {@linkplain #ObjectInputStream() ObjectInputStream constructors} invoke the filter factory
 * to select the initial filter which may be updated or replaced by {@link #setObjectInputFilter}.
 * <p>
 * If an ObjectInputStream has a filter, the {@link ObjectInputFilter} can check that
 * the classes, array lengths, number of references in the stream, depth, and
 * number of bytes consumed from the input stream are allowed and
 * if not, can terminate deserialization.
 *
 * <p>ObjectOutputStream and ObjectInputStream can provide an application with
 * persistent storage for graphs of objects when used with a FileOutputStream
 * and FileInputStream respectively.  ObjectInputStream is used to recover
 * those objects previously serialized. Other uses include passing objects
 * between hosts using a socket stream or for marshaling and unmarshaling
 * arguments and parameters in a remote communication system.
 *
 * <p>ObjectInputStream ensures that the types of all objects in the graph
 * created from the stream match the classes present in the Java Virtual
 * Machine.  Classes are loaded as required using the standard mechanisms.
 *
 * <p>Only objects that support the java.io.Serializable or
 * java.io.Externalizable interface can be read from streams.
 *
 * <p>The method {@code readObject} is used to read an object from the
 * stream.  Java's safe casting should be used to get the desired type.  In
 * Java, strings and arrays are objects and are treated as objects during
 * serialization. When read they need to be cast to the expected type.
 *
 * <p>Primitive data types can be read from the stream using the appropriate
 * method on DataInput.
 *
 * <p>The default deserialization mechanism for objects restores the contents
 * of each field to the value and type it had when it was written.  Fields
 * declared as transient or static are ignored by the deserialization process.
 * References to other objects cause those objects to be read from the stream
 * as necessary.  Graphs of objects are restored correctly using a reference
 * sharing mechanism.  New objects are always allocated when deserializing,
 * which prevents existing objects from being overwritten.
 *
 * <p>Reading an object is analogous to running the constructors of a new
 * object.  Memory is allocated for the object and initialized to zero (NULL).
 * No-arg constructors are invoked for the non-serializable classes and then
 * the fields of the serializable classes are restored from the stream starting
 * with the serializable class closest to java.lang.object and finishing with
 * the object's most specific class.
 *
 * <p>For example to read from a stream as written by the example in
 * ObjectOutputStream:
 * <br>
 * <pre>
 *      FileInputStream fis = new FileInputStream("t.tmp");
 *      ObjectInputStream ois = new ObjectInputStream(fis);
 *
 *      int i = ois.readInt();
 *      String today = (String) ois.readObject();
 *      Date date = (Date) ois.readObject();
 *
 *      ois.close();
 * </pre>
 *
 * <p>Classes control how they are serialized by implementing either the
 * java.io.Serializable or java.io.Externalizable interfaces.
 *
 * <p>Implementing the Serializable interface allows object serialization to
 * save and restore the entire state of the object and it allows classes to
 * evolve between the time the stream is written and the time it is read.  It
 * automatically traverses references between objects, saving and restoring
 * entire graphs.
 *
 * <p>Serializable classes that require special handling during the
 * serialization and deserialization process should implement the following
 * methods:
 *
 * <pre>
 * private void writeObject(java.io.ObjectOutputStream stream)
 *     throws IOException;
 * private void readObject(java.io.ObjectInputStream stream)
 *     throws IOException, ClassNotFoundException;
 * private void readObjectNoData()
 *     throws ObjectStreamException;
 * </pre>
 *
 * <p>The readObject method is responsible for reading and restoring the state
 * of the object for its particular class using data written to the stream by
 * the corresponding writeObject method.  The method does not need to concern
 * itself with the state belonging to its superclasses or subclasses.  State is
 * restored by reading data from the ObjectInputStream for the individual
 * fields and making assignments to the appropriate fields of the object.
 * Reading primitive data types is supported by DataInput.
 *
 * <p>Any attempt to read object data which exceeds the boundaries of the
 * custom data written by the corresponding writeObject method will cause an
 * OptionalDataException to be thrown with an eof field value of true.
 * Non-object reads which exceed the end of the allotted data will reflect the
 * end of data in the same way that they would indicate the end of the stream:
 * bytewise reads will return -1 as the byte read or number of bytes read, and
 * primitive reads will throw EOFExceptions.  If there is no corresponding
 * writeObject method, then the end of default serialized data marks the end of
 * the allotted data.
 *
 * <p>Primitive and object read calls issued from within a readExternal method
 * behave in the same manner--if the stream is already positioned at the end of
 * data written by the corresponding writeExternal method, object reads will
 * throw OptionalDataExceptions with eof set to true, bytewise reads will
 * return -1, and primitive reads will throw EOFExceptions.  Note that this
 * behavior does not hold for streams written with the old
 * {@code ObjectStreamConstants.PROTOCOL_VERSION_1} protocol, in which the
 * end of data written by writeExternal methods is not demarcated, and hence
 * cannot be detected.
 *
 * <p>The readObjectNoData method is responsible for initializing the state of
 * the object for its particular class in the event that the serialization
 * stream does not list the given class as a superclass of the object being
 * deserialized.  This may occur in cases where the receiving party uses a
 * different version of the deserialized instance's class than the sending
 * party, and the receiver's version extends classes that are not extended by
 * the sender's version.  This may also occur if the serialization stream has
 * been tampered; hence, readObjectNoData is useful for initializing
 * deserialized objects properly despite a "hostile" or incomplete source
 * stream.
 *
 * <p>Serialization does not read or assign values to the fields of any object
 * that does not implement the java.io.Serializable interface.  Subclasses of
 * Objects that are not serializable can be serializable. In this case the
 * non-serializable class must have a no-arg constructor to allow its fields to
 * be initialized.  In this case it is the responsibility of the subclass to
 * save and restore the state of the non-serializable class. It is frequently
 * the case that the fields of that class are accessible (public, package, or
 * protected) or that there are get and set methods that can be used to restore
 * the state.
 *
 * <p>Any exception that occurs while deserializing an object will be caught by
 * the ObjectInputStream and abort the reading process.
 *
 * <p>Implementing the Externalizable interface allows the object to assume
 * complete control over the contents and format of the object's serialized
 * form.  The methods of the Externalizable interface, writeExternal and
 * readExternal, are called to save and restore the objects state.  When
 * implemented by a class they can write and read their own state using all of
 * the methods of ObjectOutput and ObjectInput.  It is the responsibility of
 * the objects to handle any versioning that occurs.
 *
 * <p>Enum constants are deserialized differently than ordinary serializable or
 * externalizable objects.  The serialized form of an enum constant consists
 * solely of its name; field values of the constant are not transmitted.  To
 * deserialize an enum constant, ObjectInputStream reads the constant name from
 * the stream; the deserialized constant is then obtained by calling the static
 * method {@code Enum.valueOf(Class, String)} with the enum constant's
 * base type and the received constant name as arguments.  Like other
 * serializable or externalizable objects, enum constants can function as the
 * targets of back references appearing subsequently in the serialization
 * stream.  The process by which enum constants are deserialized cannot be
 * customized: any class-specific readObject, readObjectNoData, and readResolve
 * methods defined by enum types are ignored during deserialization.
 * Similarly, any serialPersistentFields or serialVersionUID field declarations
 * are also ignored--all enum types have a fixed serialVersionUID of 0L.
 *
 * <a id="record-serialization"></a>
 * <p>Records are serialized differently than ordinary serializable or externalizable
 * objects. During deserialization the record's canonical constructor is invoked
 * to construct the record object. Certain serialization-related methods, such
 * as readObject and writeObject, are ignored for serializable records. See
 * <a href="{@docRoot}/../specs/serialization/serial-arch.html#serialization-of-records">
 * <cite>Java Object Serialization Specification,</cite> Section 1.13,
 * "Serialization of Records"</a> for additional information.
 *
 * @author      Mike Warres
 * @author      Roger Riggs
 * @see java.io.DataInput
 * @see java.io.ObjectOutputStream
 * @see java.io.Serializable
 * @see <a href="{@docRoot}/../specs/serialization/input.html">
 *      <cite>Java Object Serialization Specification,</cite> Section 3, "Object Input Classes"</a>
 * @since   1.1
 */
public class ObjectInputStream
    extends InputStream implements ObjectInput, ObjectStreamConstants
{
    /** handle value representing null */
    private static final int NULL_HANDLE = -1;

    /** marker for unshared objects in internal handle table */
    private static final Object unsharedMarker = new Object();

    /**
     * immutable table mapping primitive type names to corresponding
     * class objects
     */
    private static final Map<String, Class<?>> primClasses =
        Map.of("boolean", boolean.class,
               "byte", byte.class,
               "char", char.class,
               "short", short.class,
               "int", int.class,
               "long", long.class,
               "float", float.class,
               "double", double.class,
               "void", void.class);

    private static class Caches {
        /** cache of subclass security audit results */
        static final ConcurrentMap<WeakClassKey,Boolean> subclassAudits =
            new ConcurrentHashMap<>();

        /** queue for WeakReferences to audited subclasses */
        static final ReferenceQueue<Class<?>> subclassAuditsQueue =
            new ReferenceQueue<>();

        /**
         * Property to permit setting a filter after objects
         * have been read.
         * See {@link #setObjectInputFilter(ObjectInputFilter)}
         */
        static final boolean SET_FILTER_AFTER_READ = GetBooleanAction
                .privilegedGetProperty("jdk.serialSetFilterAfterRead");

        /**
         * Property to override the implementation limit on the number
         * of interfaces allowed for Proxies. The property value is clamped to 0..65535.
         * The maximum number of interfaces allowed for a proxy is limited to 65535 by
         * {@link java.lang.reflect.Proxy#newProxyInstance(ClassLoader, Class[], InvocationHandler)}.
         */
        static final int PROXY_INTERFACE_LIMIT = Math.max(0, Math.min(65535, GetIntegerAction
                .privilegedGetProperty("jdk.serialProxyInterfaceLimit", 65535)));
    }

    /*
     * Separate class to defer initialization of logging until needed.
     */
    private static class Logging {
        /*
         * Logger for ObjectInputFilter results.
         * Setup the filter logger if it is set to DEBUG or TRACE.
         * (Assuming it will not change).
         */
        static final System.Logger filterLogger;

        static {
            Logger filterLog = System.getLogger("java.io.serialization");
            filterLogger = (filterLog.isLoggable(Logger.Level.DEBUG)
                    || filterLog.isLoggable(Logger.Level.TRACE)) ? filterLog : null;
        }
    }

    /** filter stream for handling block data conversion */
    private final BlockDataInputStream bin;
    /** validation callback list */
    private final ValidationList vlist;
    /** recursion depth */
    private long depth;
    /** Total number of references to any type of object, class, enum, proxy, etc. */
    private long totalObjectRefs;
    /** whether stream is closed */
    private boolean closed;

    /** wire handle -> obj/exception map */
    private final HandleTable handles;
    /** scratch field for passing handle values up/down call stack */
    private int passHandle = NULL_HANDLE;
    /** flag set when at end of field value block with no TC_ENDBLOCKDATA */
    private boolean defaultDataEnd = false;

    /** if true, invoke readObjectOverride() instead of readObject() */
    private final boolean enableOverride;
    /** if true, invoke resolveObject() */
    private boolean enableResolve;

    /**
     * Context during upcalls to class-defined readObject methods; holds
     * object currently being deserialized and descriptor for current class.
     * Null when not during readObject upcall.
     */
    private SerialCallbackContext curContext;

    /**
     * Filter of class descriptors and classes read from the stream;
     * may be null.
     */
    private ObjectInputFilter serialFilter;

    /**
     * True if the stream-specific filter has been set; initially false.
     */
    private boolean streamFilterSet;

    /**
     * Creates an ObjectInputStream that reads from the specified InputStream.
     * A serialization stream header is read from the stream and verified.
     * This constructor will block until the corresponding ObjectOutputStream
     * has written and flushed the header.
     *
     * <p>The constructor initializes the deserialization filter to the filter returned
     * by invoking the {@link Config#getSerialFilterFactory()} with {@code null} for the current filter
     * and the {@linkplain Config#getSerialFilter() static JVM-wide filter} for the requested filter.
     *
     * <p>If a security manager is installed, this constructor will check for
     * the "enableSubclassImplementation" SerializablePermission when invoked
     * directly or indirectly by the constructor of a subclass which overrides
     * the ObjectInputStream.readFields or ObjectInputStream.readUnshared
     * methods.
     *
     * @param   in input stream to read from
     * @throws  StreamCorruptedException if the stream header is incorrect
     * @throws  IOException if an I/O error occurs while reading stream header
     * @throws  SecurityException if untrusted subclass illegally overrides
     *          security-sensitive methods
     * @throws  NullPointerException if {@code in} is {@code null}
     * @see     ObjectInputStream#ObjectInputStream()
     * @see     ObjectInputStream#readFields()
     * @see     ObjectOutputStream#ObjectOutputStream(OutputStream)
     */
    public ObjectInputStream(InputStream in) throws IOException {
        verifySubclass();
        bin = new BlockDataInputStream(in);
        handles = new HandleTable(10);
        vlist = new ValidationList();
        streamFilterSet = false;
        serialFilter = Config.getSerialFilterFactorySingleton().apply(null, Config.getSerialFilter());
        enableOverride = false;
        readStreamHeader();
        bin.setBlockDataMode(true);
    }

    /**
     * Provide a way for subclasses that are completely reimplementing
     * ObjectInputStream to not have to allocate private data just used by this
     * implementation of ObjectInputStream.
     *
     * <p>The constructor initializes the deserialization filter to the filter returned
     * by invoking the {@link Config#getSerialFilterFactory()} with {@code null} for the current filter
     * and the {@linkplain Config#getSerialFilter() static JVM-wide filter} for the requested filter.
     *
     * <p>If there is a security manager installed, this method first calls the
     * security manager's {@code checkPermission} method with the
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
    protected ObjectInputStream() throws IOException, SecurityException {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(SUBCLASS_IMPLEMENTATION_PERMISSION);
        }
        bin = null;
        handles = null;
        vlist = null;
        streamFilterSet = false;
        serialFilter = Config.getSerialFilterFactorySingleton().apply(null, Config.getSerialFilter());
        enableOverride = true;
    }

    /**
     * Read an object from the ObjectInputStream.  The class of the object, the
     * signature of the class, and the values of the non-transient and
     * non-static fields of the class and all of its supertypes are read.
     * Default deserializing for a class can be overridden using the writeObject
     * and readObject methods.  Objects referenced by this object are read
     * transitively so that a complete equivalent graph of objects is
     * reconstructed by readObject.
     *
     * <p>The root object is completely restored when all of its fields and the
     * objects it references are completely restored.  At this point the object
     * validation callbacks are executed in order based on their registered
     * priorities. The callbacks are registered by objects (in the readObject
     * special methods) as they are individually restored.
     *
     * <p>The deserialization filter, when not {@code null}, is invoked for
     * each object (regular or class) read to reconstruct the root object.
     * See {@link #setObjectInputFilter(ObjectInputFilter) setObjectInputFilter} for details.
     *
     * <p>Exceptions are thrown for problems with the InputStream and for
     * classes that should not be deserialized.  All exceptions are fatal to
     * the InputStream and leave it in an indeterminate state; it is up to the
     * caller to ignore or recover the stream state.
     *
     * @throws  ClassNotFoundException Class of a serialized object cannot be
     *          found.
     * @throws  InvalidClassException Something is wrong with a class used by
     *          deserialization.
     * @throws  StreamCorruptedException Control information in the
     *          stream is inconsistent.
     * @throws  OptionalDataException Primitive data was found in the
     *          stream instead of objects.
     * @throws  IOException Any of the usual Input/Output related exceptions.
     */
    public final Object readObject()
        throws IOException, ClassNotFoundException {
        return readObject(Object.class);
    }

    /**
     * Reads a String and only a string.
     *
     * @return  the String read
     * @throws  EOFException If end of file is reached.
     * @throws  IOException If other I/O error has occurred.
     */
    private String readString() throws IOException {
        try {
            return (String) readObject(String.class);
        } catch (ClassNotFoundException cnf) {
            throw new IllegalStateException(cnf);
        }
    }

    /**
     * Internal method to read an object from the ObjectInputStream of the expected type.
     * Called only from {@code readObject()} and {@code readString()}.
     * Only {@code Object.class} and {@code String.class} are supported.
     *
     * @param type the type expected; either Object.class or String.class
     * @return an object of the type
     * @throws  IOException Any of the usual Input/Output related exceptions.
     * @throws  ClassNotFoundException Class of a serialized object cannot be
     *          found.
     */
    private final Object readObject(Class<?> type)
        throws IOException, ClassNotFoundException
    {
        if (enableOverride) {
            return readObjectOverride();
        }

        if (! (type == Object.class || type == String.class))
            throw new AssertionError("internal error");

        // if nested read, passHandle contains handle of enclosing object
        int outerHandle = passHandle;
        try {
            Object obj = readObject0(type, false);
            handles.markDependency(outerHandle, passHandle);
            ClassNotFoundException ex = handles.lookupException(passHandle);
            if (ex != null) {
                throw ex;
            }
            if (depth == 0) {
                vlist.doCallbacks();
                freeze();
            }
            return obj;
        } finally {
            passHandle = outerHandle;
            if (closed && depth == 0) {
                clear();
            }
        }
    }

    /**
     * This method is called by trusted subclasses of ObjectInputStream that
     * constructed ObjectInputStream using the protected no-arg constructor.
     * The subclass is expected to provide an override method with the modifier
     * "final".
     *
     * @return  the Object read from the stream.
     * @throws  ClassNotFoundException Class definition of a serialized object
     *          cannot be found.
     * @throws  OptionalDataException Primitive data was found in the stream
     *          instead of objects.
     * @throws  IOException if I/O errors occurred while reading from the
     *          underlying stream
     * @see #ObjectInputStream()
     * @see #readObject()
     * @since 1.2
     */
    protected Object readObjectOverride()
        throws IOException, ClassNotFoundException
    {
        return null;
    }

    /**
     * Reads an "unshared" object from the ObjectInputStream.  This method is
     * identical to readObject, except that it prevents subsequent calls to
     * readObject and readUnshared from returning additional references to the
     * deserialized instance obtained via this call.  Specifically:
     * <ul>
     *   <li>If readUnshared is called to deserialize a back-reference (the
     *       stream representation of an object which has been written
     *       previously to the stream), an ObjectStreamException will be
     *       thrown.
     *
     *   <li>If readUnshared returns successfully, then any subsequent attempts
     *       to deserialize back-references to the stream handle deserialized
     *       by readUnshared will cause an ObjectStreamException to be thrown.
     * </ul>
     * Deserializing an object via readUnshared invalidates the stream handle
     * associated with the returned object.  Note that this in itself does not
     * always guarantee that the reference returned by readUnshared is unique;
     * the deserialized object may define a readResolve method which returns an
     * object visible to other parties, or readUnshared may return a Class
     * object or enum constant obtainable elsewhere in the stream or through
     * external means. If the deserialized object defines a readResolve method
     * and the invocation of that method returns an array, then readUnshared
     * returns a shallow clone of that array; this guarantees that the returned
     * array object is unique and cannot be obtained a second time from an
     * invocation of readObject or readUnshared on the ObjectInputStream,
     * even if the underlying data stream has been manipulated.
     *
     * <p>The deserialization filter, when not {@code null}, is invoked for
     * each object (regular or class) read to reconstruct the root object.
     * See {@link #setObjectInputFilter(ObjectInputFilter) setObjectInputFilter} for details.
     *
     * <p>ObjectInputStream subclasses which override this method can only be
     * constructed in security contexts possessing the
     * "enableSubclassImplementation" SerializablePermission; any attempt to
     * instantiate such a subclass without this permission will cause a
     * SecurityException to be thrown.
     *
     * @return  reference to deserialized object
     * @throws  ClassNotFoundException if class of an object to deserialize
     *          cannot be found
     * @throws  StreamCorruptedException if control information in the stream
     *          is inconsistent
     * @throws  ObjectStreamException if object to deserialize has already
     *          appeared in stream
     * @throws  OptionalDataException if primitive data is next in stream
     * @throws  IOException if an I/O error occurs during deserialization
     * @since   1.4
     */
    public Object readUnshared() throws IOException, ClassNotFoundException {
        // if nested read, passHandle contains handle of enclosing object
        int outerHandle = passHandle;
        try {
            Object obj = readObject0(Object.class, true);
            handles.markDependency(outerHandle, passHandle);
            ClassNotFoundException ex = handles.lookupException(passHandle);
            if (ex != null) {
                throw ex;
            }
            if (depth == 0) {
                vlist.doCallbacks();
                freeze();
            }
            return obj;
        } finally {
            passHandle = outerHandle;
            if (closed && depth == 0) {
                clear();
            }
        }
    }

    /**
     * Read the non-static and non-transient fields of the current class from
     * this stream.  This may only be called from the readObject method of the
     * class being deserialized. It will throw the NotActiveException if it is
     * called otherwise.
     *
     * @throws  ClassNotFoundException if the class of a serialized object
     *          could not be found.
     * @throws  IOException if an I/O error occurs.
     * @throws  NotActiveException if the stream is not currently reading
     *          objects.
     */
    public void defaultReadObject()
        throws IOException, ClassNotFoundException
    {
        SerialCallbackContext ctx = curContext;
        if (ctx == null) {
            throw new NotActiveException("not in call to readObject");
        }
        Object curObj = ctx.getObj();
        ObjectStreamClass curDesc = ctx.getDesc();
        bin.setBlockDataMode(false);

        // Read fields of the current descriptor into a new FieldValues
        FieldValues values = new FieldValues(curDesc, true);
        if (curObj != null) {
            values.defaultCheckFieldValues(curObj);
            values.defaultSetFieldValues(curObj);
        }
        bin.setBlockDataMode(true);
        if (!curDesc.hasWriteObjectData()) {
            /*
             * Fix for 4360508: since stream does not contain terminating
             * TC_ENDBLOCKDATA tag, set flag so that reading code elsewhere
             * knows to simulate end-of-custom-data behavior.
             */
            defaultDataEnd = true;
        }
        ClassNotFoundException ex = handles.lookupException(passHandle);
        if (ex != null) {
            throw ex;
        }
    }

    /**
     * Reads the persistent fields from the stream and makes them available by
     * name.
     *
     * @return  the {@code GetField} object representing the persistent
     *          fields of the object being deserialized
     * @throws  ClassNotFoundException if the class of a serialized object
     *          could not be found.
     * @throws  IOException if an I/O error occurs.
     * @throws  NotActiveException if the stream is not currently reading
     *          objects.
     * @since 1.2
     */
    public ObjectInputStream.GetField readFields()
        throws IOException, ClassNotFoundException
    {
        SerialCallbackContext ctx = curContext;
        if (ctx == null) {
            throw new NotActiveException("not in call to readObject");
        }
        ctx.checkAndSetUsed();
        ObjectStreamClass curDesc = ctx.getDesc();
        bin.setBlockDataMode(false);
        // Read fields of the current descriptor into a new FieldValues
        FieldValues values = new FieldValues(curDesc, false);
        bin.setBlockDataMode(true);
        if (!curDesc.hasWriteObjectData()) {
            /*
             * Fix for 4360508: since stream does not contain terminating
             * TC_ENDBLOCKDATA tag, set flag so that reading code elsewhere
             * knows to simulate end-of-custom-data behavior.
             */
            defaultDataEnd = true;
        }
        return values;
    }

    /**
     * Register an object to be validated before the graph is returned.  While
     * similar to resolveObject these validations are called after the entire
     * graph has been reconstituted.  Typically, a readObject method will
     * register the object with the stream so that when all of the objects are
     * restored a final set of validations can be performed.
     *
     * @param   obj the object to receive the validation callback.
     * @param   prio controls the order of callbacks;zero is a good default.
     *          Use higher numbers to be called back earlier, lower numbers for
     *          later callbacks. Within a priority, callbacks are processed in
     *          no particular order.
     * @throws  NotActiveException The stream is not currently reading objects
     *          so it is invalid to register a callback.
     * @throws  InvalidObjectException The validation object is null.
     */
    public void registerValidation(ObjectInputValidation obj, int prio)
        throws NotActiveException, InvalidObjectException
    {
        if (depth == 0) {
            throw new NotActiveException("stream inactive");
        }
        vlist.register(obj, prio);
    }

    /**
     * Load the local class equivalent of the specified stream class
     * description.  Subclasses may implement this method to allow classes to
     * be fetched from an alternate source.
     *
     * <p>The corresponding method in {@code ObjectOutputStream} is
     * {@code annotateClass}.  This method will be invoked only once for
     * each unique class in the stream.  This method can be implemented by
     * subclasses to use an alternate loading mechanism but must return a
     * {@code Class} object. Once returned, if the class is not an array
     * class, its serialVersionUID is compared to the serialVersionUID of the
     * serialized class, and if there is a mismatch, the deserialization fails
     * and an {@link InvalidClassException} is thrown.
     *
     * <p>The default implementation of this method in
     * {@code ObjectInputStream} returns the result of calling
     * <pre>
     *     Class.forName(desc.getName(), false, loader)
     * </pre>
     * where {@code loader} is the first class loader on the current
     * thread's stack (starting from the currently executing method) that is
     * neither the {@linkplain ClassLoader#getPlatformClassLoader() platform
     * class loader} nor its ancestor; otherwise, {@code loader} is the
     * <em>platform class loader</em>. If this call results in a
     * {@code ClassNotFoundException} and the name of the passed
     * {@code ObjectStreamClass} instance is the Java language keyword
     * for a primitive type or void, then the {@code Class} object
     * representing that primitive type or void will be returned
     * (e.g., an {@code ObjectStreamClass} with the name
     * {@code "int"} will be resolved to {@code Integer.TYPE}).
     * Otherwise, the {@code ClassNotFoundException} will be thrown to
     * the caller of this method.
     *
     * @param   desc an instance of class {@code ObjectStreamClass}
     * @return  a {@code Class} object corresponding to {@code desc}
     * @throws  IOException any of the usual Input/Output exceptions.
     * @throws  ClassNotFoundException if class of a serialized object cannot
     *          be found.
     */
    protected Class<?> resolveClass(ObjectStreamClass desc)
        throws IOException, ClassNotFoundException
    {
        String name = desc.getName();
        try {
            return Class.forName(name, false, latestUserDefinedLoader());
        } catch (ClassNotFoundException ex) {
            Class<?> cl = primClasses.get(name);
            if (cl != null) {
                return cl;
            } else {
                throw ex;
            }
        }
    }

    /**
     * Returns a proxy class that implements the interfaces named in a proxy
     * class descriptor; subclasses may implement this method to read custom
     * data from the stream along with the descriptors for dynamic proxy
     * classes, allowing them to use an alternate loading mechanism for the
     * interfaces and the proxy class.
     *
     * <p>This method is called exactly once for each unique proxy class
     * descriptor in the stream.
     *
     * <p>The corresponding method in {@code ObjectOutputStream} is
     * {@code annotateProxyClass}.  For a given subclass of
     * {@code ObjectInputStream} that overrides this method, the
     * {@code annotateProxyClass} method in the corresponding subclass of
     * {@code ObjectOutputStream} must write any data or objects read by
     * this method.
     *
     * <p>The default implementation of this method in
     * {@code ObjectInputStream} returns the result of calling
     * {@code Proxy.getProxyClass} with the list of {@code Class}
     * objects for the interfaces that are named in the {@code interfaces}
     * parameter.  The {@code Class} object for each interface name
     * {@code i} is the value returned by calling
     * <pre>
     *     Class.forName(i, false, loader)
     * </pre>
     * where {@code loader} is the first class loader on the current
     * thread's stack (starting from the currently executing method) that is
     * neither the {@linkplain ClassLoader#getPlatformClassLoader() platform
     * class loader} nor its ancestor; otherwise, {@code loader} is the
     * <em>platform class loader</em>.
     * Unless any of the resolved interfaces are non-public, this same value
     * of {@code loader} is also the class loader passed to
     * {@code Proxy.getProxyClass}; if non-public interfaces are present,
     * their class loader is passed instead (if more than one non-public
     * interface class loader is encountered, an
     * {@code IllegalAccessError} is thrown).
     * If {@code Proxy.getProxyClass} throws an
     * {@code IllegalArgumentException}, {@code resolveProxyClass}
     * will throw a {@code ClassNotFoundException} containing the
     * {@code IllegalArgumentException}.
     *
     * @param interfaces the list of interface names that were
     *                deserialized in the proxy class descriptor
     * @return  a proxy class for the specified interfaces
     * @throws        IOException any exception thrown by the underlying
     *                {@code InputStream}
     * @throws        ClassNotFoundException if the proxy class or any of the
     *                named interfaces could not be found
     * @see ObjectOutputStream#annotateProxyClass(Class)
     * @since 1.3
     */
    protected Class<?> resolveProxyClass(String[] interfaces)
        throws IOException, ClassNotFoundException
    {
        ClassLoader latestLoader = latestUserDefinedLoader();
        ClassLoader nonPublicLoader = null;
        boolean hasNonPublicInterface = false;

        // define proxy in class loader of non-public interface(s), if any
        Class<?>[] classObjs = new Class<?>[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            Class<?> cl = Class.forName(interfaces[i], false, latestLoader);
            if ((cl.getModifiers() & Modifier.PUBLIC) == 0) {
                if (hasNonPublicInterface) {
                    if (nonPublicLoader != cl.getClassLoader()) {
                        throw new IllegalAccessError(
                            "conflicting non-public interface class loaders");
                    }
                } else {
                    nonPublicLoader = cl.getClassLoader();
                    hasNonPublicInterface = true;
                }
            }
            classObjs[i] = cl;
        }
        try {
            @SuppressWarnings("deprecation")
            Class<?> proxyClass = Proxy.getProxyClass(
                hasNonPublicInterface ? nonPublicLoader : latestLoader,
                classObjs);
            return proxyClass;
        } catch (IllegalArgumentException e) {
            throw new ClassNotFoundException(null, e);
        }
    }

    /**
     * This method will allow trusted subclasses of ObjectInputStream to
     * substitute one object for another during deserialization. Replacing
     * objects is disabled until enableResolveObject is called. The
     * enableResolveObject method checks that the stream requesting to resolve
     * object can be trusted. Every reference to serializable objects is passed
     * to resolveObject.  To insure that the private state of objects is not
     * unintentionally exposed only trusted streams may use resolveObject.
     *
     * <p>This method is called after an object has been read but before it is
     * returned from readObject.  The default resolveObject method just returns
     * the same object.
     *
     * <p>When a subclass is replacing objects it must insure that the
     * substituted object is compatible with every field where the reference
     * will be stored.  Objects whose type is not a subclass of the type of the
     * field or array element abort the deserialization by raising an exception
     * and the object is not be stored.
     *
     * <p>This method is called only once when each object is first
     * encountered.  All subsequent references to the object will be redirected
     * to the new object.
     *
     * @param   obj object to be substituted
     * @return  the substituted object
     * @throws  IOException Any of the usual Input/Output exceptions.
     */
    protected Object resolveObject(Object obj) throws IOException {
        return obj;
    }

    /**
     * Enables the stream to do replacement of objects read from the stream. When
     * enabled, the {@link #resolveObject} method is called for every object being
     * deserialized.
     *
     * <p>If object replacement is currently not enabled, and
     * {@code enable} is true, and there is a security manager installed,
     * this method first calls the security manager's
     * {@code checkPermission} method with the
     * {@code SerializablePermission("enableSubstitution")} permission to
     * ensure that the caller is permitted to enable the stream to do replacement
     * of objects read from the stream.
     *
     * @param   enable true for enabling use of {@code resolveObject} for
     *          every object being deserialized
     * @return  the previous setting before this method was invoked
     * @throws  SecurityException if a security manager exists and its
     *          {@code checkPermission} method denies enabling the stream
     *          to do replacement of objects read from the stream.
     * @see SecurityManager#checkPermission
     * @see java.io.SerializablePermission
     */
    protected boolean enableResolveObject(boolean enable)
        throws SecurityException
    {
        if (enable == enableResolve) {
            return enable;
        }
        if (enable) {
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(SUBSTITUTION_PERMISSION);
            }
        }
        enableResolve = enable;
        return !enableResolve;
    }

    /**
     * The readStreamHeader method is provided to allow subclasses to read and
     * verify their own stream headers. It reads and verifies the magic number
     * and version number.
     *
     * @throws  IOException if there are I/O errors while reading from the
     *          underlying {@code InputStream}
     * @throws  StreamCorruptedException if control information in the stream
     *          is inconsistent
     */
    protected void readStreamHeader()
        throws IOException, StreamCorruptedException
    {
        short s0 = bin.readShort();
        short s1 = bin.readShort();
        if (s0 != STREAM_MAGIC || s1 != STREAM_VERSION) {
            throw new StreamCorruptedException(
                String.format("invalid stream header: %04X%04X", s0, s1));
        }
    }

    /**
     * Read a class descriptor from the serialization stream.  This method is
     * called when the ObjectInputStream expects a class descriptor as the next
     * item in the serialization stream.  Subclasses of ObjectInputStream may
     * override this method to read in class descriptors that have been written
     * in non-standard formats (by subclasses of ObjectOutputStream which have
     * overridden the {@code writeClassDescriptor} method).  By default,
     * this method reads class descriptors according to the format defined in
     * the Object Serialization specification.
     *
     * @return  the class descriptor read
     * @throws  IOException If an I/O error has occurred.
     * @throws  ClassNotFoundException If the Class of a serialized object used
     *          in the class descriptor representation cannot be found
     * @see java.io.ObjectOutputStream#writeClassDescriptor(java.io.ObjectStreamClass)
     * @since 1.3
     */
    protected ObjectStreamClass readClassDescriptor()
        throws IOException, ClassNotFoundException
    {
        ObjectStreamClass desc = new ObjectStreamClass();
        desc.readNonProxy(this);
        return desc;
    }

    /**
     * Reads a byte of data. This method will block if no input is available.
     *
     * @return  the byte read, or -1 if the end of the stream is reached.
     * @throws  IOException If an I/O error has occurred.
     */
    public int read() throws IOException {
        return bin.read();
    }

    /**
     * Reads into an array of bytes.  This method will block until some input
     * is available. Consider using java.io.DataInputStream.readFully to read
     * exactly 'length' bytes.
     *
     * @param   buf the buffer into which the data is read
     * @param   off the start offset in the destination array {@code buf}
     * @param   len the maximum number of bytes read
     * @return  the actual number of bytes read, -1 is returned when the end of
     *          the stream is reached.
     * @throws  NullPointerException if {@code buf} is {@code null}.
     * @throws  IndexOutOfBoundsException if {@code off} is negative,
     *          {@code len} is negative, or {@code len} is greater than
     *          {@code buf.length - off}.
     * @throws  IOException If an I/O error has occurred.
     * @see java.io.DataInputStream#readFully(byte[],int,int)
     */
    public int read(byte[] buf, int off, int len) throws IOException {
        if (buf == null) {
            throw new NullPointerException();
        }
        int endoff = off + len;
        if (off < 0 || len < 0 || endoff > buf.length || endoff < 0) {
            throw new IndexOutOfBoundsException();
        }
        return bin.read(buf, off, len, false);
    }

    /**
     * Returns the number of bytes that can be read without blocking.
     *
     * @return  the number of available bytes.
     * @throws  IOException if there are I/O errors while reading from the
     *          underlying {@code InputStream}
     */
    public int available() throws IOException {
        return bin.available();
    }

    /**
     * Closes the input stream. Must be called to release any resources
     * associated with the stream.
     *
     * @throws  IOException If an I/O error has occurred.
     */
    public void close() throws IOException {
        /*
         * Even if stream already closed, propagate redundant close to
         * underlying stream to stay consistent with previous implementations.
         */
        closed = true;
        if (depth == 0) {
            clear();
        }
        bin.close();
    }

    /**
     * Reads in a boolean.
     *
     * @return  the boolean read.
     * @throws  EOFException If end of file is reached.
     * @throws  IOException If other I/O error has occurred.
     */
    public boolean readBoolean() throws IOException {
        return bin.readBoolean();
    }

    /**
     * Reads an 8 bit byte.
     *
     * @return  the 8 bit byte read.
     * @throws  EOFException If end of file is reached.
     * @throws  IOException If other I/O error has occurred.
     */
    public byte readByte() throws IOException  {
        return bin.readByte();
    }

    /**
     * Reads an unsigned 8 bit byte.
     *
     * @return  the 8 bit byte read.
     * @throws  EOFException If end of file is reached.
     * @throws  IOException If other I/O error has occurred.
     */
    public int readUnsignedByte()  throws IOException {
        return bin.readUnsignedByte();
    }

    /**
     * Reads a 16 bit char.
     *
     * @return  the 16 bit char read.
     * @throws  EOFException If end of file is reached.
     * @throws  IOException If other I/O error has occurred.
     */
    public char readChar()  throws IOException {
        return bin.readChar();
    }

    /**
     * Reads a 16 bit short.
     *
     * @return  the 16 bit short read.
     * @throws  EOFException If end of file is reached.
     * @throws  IOException If other I/O error has occurred.
     */
    public short readShort()  throws IOException {
        return bin.readShort();
    }

    /**
     * Reads an unsigned 16 bit short.
     *
     * @return  the 16 bit short read.
     * @throws  EOFException If end of file is reached.
     * @throws  IOException If other I/O error has occurred.
     */
    public int readUnsignedShort() throws IOException {
        return bin.readUnsignedShort();
    }

    /**
     * Reads a 32 bit int.
     *
     * @return  the 32 bit integer read.
     * @throws  EOFException If end of file is reached.
     * @throws  IOException If other I/O error has occurred.
     */
    public int readInt()  throws IOException {
        return bin.readInt();
    }

    /**
     * Reads a 64 bit long.
     *
     * @return  the read 64 bit long.
     * @throws  EOFException If end of file is reached.
     * @throws  IOException If other I/O error has occurred.
     */
    public long readLong()  throws IOException {
        return bin.readLong();
    }

    /**
     * Reads a 32 bit float.
     *
     * @return  the 32 bit float read.
     * @throws  EOFException If end of file is reached.
     * @throws  IOException If other I/O error has occurred.
     */
    public float readFloat() throws IOException {
        return bin.readFloat();
    }

    /**
     * Reads a 64 bit double.
     *
     * @return  the 64 bit double read.
     * @throws  EOFException If end of file is reached.
     * @throws  IOException If other I/O error has occurred.
     */
    public double readDouble() throws IOException {
        return bin.readDouble();
    }

    /**
     * Reads bytes, blocking until all bytes are read.
     *
     * @param   buf the buffer into which the data is read
     * @throws  NullPointerException If {@code buf} is {@code null}.
     * @throws  EOFException If end of file is reached.
     * @throws  IOException If other I/O error has occurred.
     */
    public void readFully(byte[] buf) throws IOException {
        bin.readFully(buf, 0, buf.length, false);
    }

    /**
     * Reads bytes, blocking until all bytes are read.
     *
     * @param   buf the buffer into which the data is read
     * @param   off the start offset into the data array {@code buf}
     * @param   len the maximum number of bytes to read
     * @throws  NullPointerException If {@code buf} is {@code null}.
     * @throws  IndexOutOfBoundsException If {@code off} is negative,
     *          {@code len} is negative, or {@code len} is greater than
     *          {@code buf.length - off}.
     * @throws  EOFException If end of file is reached.
     * @throws  IOException If other I/O error has occurred.
     */
    public void readFully(byte[] buf, int off, int len) throws IOException {
        int endoff = off + len;
        if (off < 0 || len < 0 || endoff > buf.length || endoff < 0) {
            throw new IndexOutOfBoundsException();
        }
        bin.readFully(buf, off, len, false);
    }

    /**
     * Skips bytes.
     *
     * @param   len the number of bytes to be skipped
     * @return  the actual number of bytes skipped.
     * @throws  IOException If an I/O error has occurred.
     */
    public int skipBytes(int len) throws IOException {
        return bin.skipBytes(len);
    }

    /**
     * Reads in a line that has been terminated by a \n, \r, \r\n or EOF.
     *
     * @return  a String copy of the line.
     * @throws  IOException if there are I/O errors while reading from the
     *          underlying {@code InputStream}
     * @deprecated This method does not properly convert bytes to characters.
     *          see DataInputStream for the details and alternatives.
     */
    @Deprecated
    public String readLine() throws IOException {
        return bin.readLine();
    }

    /**
     * Reads a String in
     * <a href="DataInput.html#modified-utf-8">modified UTF-8</a>
     * format.
     *
     * @return  the String.
     * @throws  IOException if there are I/O errors while reading from the
     *          underlying {@code InputStream}
     * @throws  UTFDataFormatException if read bytes do not represent a valid
     *          modified UTF-8 encoding of a string
     */
    public String readUTF() throws IOException {
        return bin.readUTF();
    }

    /**
     * Returns the deserialization filter for this stream.
     * The filter is the result of invoking the
     * {@link Config#getSerialFilterFactory() JVM-wide filter factory}
     * either by the {@linkplain #ObjectInputStream() constructor} or the most recent invocation of
     * {@link #setObjectInputFilter setObjectInputFilter}.
     *
     * @return the deserialization filter for the stream; may be null
     * @since 9
     */
    public final ObjectInputFilter getObjectInputFilter() {
        return serialFilter;
    }

    /**
     * Set the deserialization filter for the stream.
     *
     * The deserialization filter is set to the filter returned by invoking the
     * {@linkplain Config#getSerialFilterFactory() JVM-wide filter factory}
     * with the {@linkplain #getObjectInputFilter() current filter} and the {@code filter} parameter.
     * The current filter was set in the
     * {@linkplain #ObjectInputStream() ObjectInputStream constructors} by invoking the
     * {@linkplain Config#getSerialFilterFactory() JVM-wide filter factory} and may be {@code null}.
     * {@linkplain #setObjectInputFilter(ObjectInputFilter)} This method} can be called
     * once and only once before reading any objects from the stream;
     * for example, by calling {@link #readObject} or {@link #readUnshared}.
     *
     * <p>It is not permitted to replace a {@code non-null} filter with a {@code null} filter.
     * If the {@linkplain #getObjectInputFilter() current filter} is {@code non-null},
     * the value returned from the filter factory must be {@code non-null}.
     *
     * <p>The filter's {@link ObjectInputFilter#checkInput checkInput} method is called
     * for each class and reference in the stream.
     * The filter can check any or all of the class, the array length, the number
     * of references, the depth of the graph, and the size of the input stream.
     * The depth is the number of nested {@linkplain #readObject readObject}
     * calls starting with the reading of the root of the graph being deserialized
     * and the current object being deserialized.
     * The number of references is the cumulative number of objects and references
     * to objects already read from the stream including the current object being read.
     * The filter is invoked only when reading objects from the stream and not for
     * primitives.
     * <p>
     * If the filter returns {@link ObjectInputFilter.Status#REJECTED Status.REJECTED},
     * {@code null} or throws a {@link RuntimeException},
     * the active {@code readObject} or {@code readUnshared}
     * throws {@link InvalidClassException}, otherwise deserialization
     * continues uninterrupted.
     *
     * @implSpec
     * The filter, when not {@code null}, is invoked during {@link #readObject readObject}
     * and {@link #readUnshared readUnshared} for each object (regular or class) in the stream.
     * Strings are treated as primitives and do not invoke the filter.
     * The filter is called for:
     * <ul>
     *     <li>each object reference previously deserialized from the stream
     *     (class is {@code null}, arrayLength is -1),
     *     <li>each regular class (class is not {@code null}, arrayLength is -1),
     *     <li>each interface of a dynamic proxy and the dynamic proxy class itself
     *     (class is not {@code null}, arrayLength is -1),
     *     <li>each array is filtered using the array type and length of the array
     *     (class is the array type, arrayLength is the requested length),
     *     <li>each object replaced by its class' {@code readResolve} method
     *         is filtered using the replacement object's class, if not {@code null},
     *         and if it is an array, the arrayLength, otherwise -1,
     *     <li>and each object replaced by {@link #resolveObject resolveObject}
     *         is filtered using the replacement object's class, if not {@code null},
     *         and if it is an array, the arrayLength, otherwise -1.
     * </ul>
     *
     * When the {@link ObjectInputFilter#checkInput checkInput} method is invoked
     * it is given access to the current class, the array length,
     * the current number of references already read from the stream,
     * the depth of nested calls to {@link #readObject readObject} or
     * {@link #readUnshared readUnshared},
     * and the implementation dependent number of bytes consumed from the input stream.
     * <p>
     * Each call to {@link #readObject readObject} or
     * {@link #readUnshared readUnshared} increases the depth by 1
     * before reading an object and decreases by 1 before returning
     * normally or exceptionally.
     * The depth starts at {@code 1} and increases for each nested object and
     * decrements when each nested call returns.
     * The count of references in the stream starts at {@code 1} and
     * is increased before reading an object.
     *
     * @param filter the filter, may be null
     * @throws SecurityException if there is security manager and the
     *       {@code SerializablePermission("serialFilter")} is not granted
     * @throws IllegalStateException if an object has been read,
     *       if the filter factory returns {@code null} when the
     *       {@linkplain #getObjectInputFilter() current filter} is non-null, or
     *       if the filter has already been set.
     * @since 9
     */
    public final void setObjectInputFilter(ObjectInputFilter filter) {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(ObjectStreamConstants.SERIAL_FILTER_PERMISSION);
        }
        if (totalObjectRefs > 0 && !Caches.SET_FILTER_AFTER_READ) {
            throw new IllegalStateException(
                    "filter can not be set after an object has been read");
        }
        if (streamFilterSet) {
            throw new IllegalStateException("filter can not be set more than once");
        }
        streamFilterSet = true;
        // Delegate to serialFilterFactory to compute stream filter
        ObjectInputFilter next = Config.getSerialFilterFactory()
                .apply(serialFilter, filter);
        if (serialFilter != null && next == null) {
            throw new IllegalStateException("filter can not be replaced with null filter");
        }
        serialFilter = next;
    }

    /**
     * Invokes the deserialization filter if non-null.
     *
     * If the filter rejects or an exception is thrown, throws InvalidClassException.
     *
     * Logs and/or commits a {@code DeserializationEvent}, if configured.
     *
     * @param clazz the class; may be null
     * @param arrayLength the array length requested; use {@code -1} if not creating an array
     * @throws InvalidClassException if it rejected by the filter or
     *        a {@link RuntimeException} is thrown
     */
    private void filterCheck(Class<?> clazz, int arrayLength)
            throws InvalidClassException {
        // Info about the stream is not available if overridden by subclass, return 0
        long bytesRead = (bin == null) ? 0 : bin.getBytesRead();
        RuntimeException ex = null;
        ObjectInputFilter.Status status = null;

        if (serialFilter != null) {
            try {
                status = serialFilter.checkInput(new FilterValues(clazz, arrayLength,
                        totalObjectRefs, depth, bytesRead));
            } catch (RuntimeException e) {
                // Preventive interception of an exception to log
                status = ObjectInputFilter.Status.REJECTED;
                ex = e;
            }
            if (Logging.filterLogger != null) {
                // Debug logging of filter checks that fail; Tracing for those that succeed
                Logging.filterLogger.log(status == null || status == ObjectInputFilter.Status.REJECTED
                                ? Logger.Level.DEBUG
                                : Logger.Level.TRACE,
                        "ObjectInputFilter {0}: {1}, array length: {2}, nRefs: {3}, depth: {4}, bytes: {5}, ex: {6}",
                        status, clazz, arrayLength, totalObjectRefs, depth, bytesRead,
                        Objects.toString(ex, "n/a"));
            }
        }
        DeserializationEvent event = new DeserializationEvent();
        if (event.shouldCommit()) {
            event.filterConfigured = serialFilter != null;
            event.filterStatus = status != null ? status.name() : null;
            event.type = clazz;
            event.arrayLength = arrayLength;
            event.objectReferences = totalObjectRefs;
            event.depth = depth;
            event.bytesRead = bytesRead;
            event.exceptionType = ex != null ? ex.getClass() : null;
            event.exceptionMessage = ex != null ? ex.getMessage() : null;
            event.commit();
        }
        if (serialFilter != null && (status == null || status == ObjectInputFilter.Status.REJECTED)) {
            InvalidClassException ice = new InvalidClassException("filter status: " + status);
            ice.initCause(ex);
            throw ice;
        }
    }

    /**
     * Checks the given array type and length to ensure that creation of such
     * an array is permitted by this ObjectInputStream. The arrayType argument
     * must represent an actual array type.
     *
     * This private method is called via SharedSecrets.
     *
     * @param arrayType the array type
     * @param arrayLength the array length
     * @throws NullPointerException if arrayType is null
     * @throws IllegalArgumentException if arrayType isn't actually an array type
     * @throws NegativeArraySizeException if arrayLength is negative
     * @throws InvalidClassException if the filter rejects creation
     */
    private void checkArray(Class<?> arrayType, int arrayLength) throws InvalidClassException {
        if (! arrayType.isArray()) {
            throw new IllegalArgumentException("not an array type");
        }

        if (arrayLength < 0) {
            throw new NegativeArraySizeException();
        }

        filterCheck(arrayType, arrayLength);
    }

    /**
     * Provide access to the persistent fields read from the input stream.
     */
    public abstract static class GetField {
        /**
         * Constructor for subclasses to call.
         */
        public GetField() {}

        /**
         * Get the ObjectStreamClass that describes the fields in the stream.
         *
         * @return  the descriptor class that describes the serializable fields
         */
        public abstract ObjectStreamClass getObjectStreamClass();

        /**
         * Return true if the named field is defaulted and has no value in this
         * stream.
         *
         * @param  name the name of the field
         * @return true, if and only if the named field is defaulted
         * @throws IOException if there are I/O errors while reading from
         *         the underlying {@code InputStream}
         * @throws IllegalArgumentException if {@code name} does not
         *         correspond to a serializable field
         */
        public abstract boolean defaulted(String name) throws IOException;

        /**
         * Get the value of the named boolean field from the persistent field.
         *
         * @param  name the name of the field
         * @param  val the default value to use if {@code name} does not
         *         have a value
         * @return the value of the named {@code boolean} field
         * @throws IOException if there are I/O errors while reading from the
         *         underlying {@code InputStream}
         * @throws IllegalArgumentException if type of {@code name} is
         *         not serializable or if the field type is incorrect
         */
        public abstract boolean get(String name, boolean val)
            throws IOException;

        /**
         * Get the value of the named byte field from the persistent field.
         *
         * @param  name the name of the field
         * @param  val the default value to use if {@code name} does not
         *         have a value
         * @return the value of the named {@code byte} field
         * @throws IOException if there are I/O errors while reading from the
         *         underlying {@code InputStream}
         * @throws IllegalArgumentException if type of {@code name} is
         *         not serializable or if the field type is incorrect
         */
        public abstract byte get(String name, byte val) throws IOException;

        /**
         * Get the value of the named char field from the persistent field.
         *
         * @param  name the name of the field
         * @param  val the default value to use if {@code name} does not
         *         have a value
         * @return the value of the named {@code char} field
         * @throws IOException if there are I/O errors while reading from the
         *         underlying {@code InputStream}
         * @throws IllegalArgumentException if type of {@code name} is
         *         not serializable or if the field type is incorrect
         */
        public abstract char get(String name, char val) throws IOException;

        /**
         * Get the value of the named short field from the persistent field.
         *
         * @param  name the name of the field
         * @param  val the default value to use if {@code name} does not
         *         have a value
         * @return the value of the named {@code short} field
         * @throws IOException if there are I/O errors while reading from the
         *         underlying {@code InputStream}
         * @throws IllegalArgumentException if type of {@code name} is
         *         not serializable or if the field type is incorrect
         */
        public abstract short get(String name, short val) throws IOException;

        /**
         * Get the value of the named int field from the persistent field.
         *
         * @param  name the name of the field
         * @param  val the default value to use if {@code name} does not
         *         have a value
         * @return the value of the named {@code int} field
         * @throws IOException if there are I/O errors while reading from the
         *         underlying {@code InputStream}
         * @throws IllegalArgumentException if type of {@code name} is
         *         not serializable or if the field type is incorrect
         */
        public abstract int get(String name, int val) throws IOException;

        /**
         * Get the value of the named long field from the persistent field.
         *
         * @param  name the name of the field
         * @param  val the default value to use if {@code name} does not
         *         have a value
         * @return the value of the named {@code long} field
         * @throws IOException if there are I/O errors while reading from the
         *         underlying {@code InputStream}
         * @throws IllegalArgumentException if type of {@code name} is
         *         not serializable or if the field type is incorrect
         */
        public abstract long get(String name, long val) throws IOException;

        /**
         * Get the value of the named float field from the persistent field.
         *
         * @param  name the name of the field
         * @param  val the default value to use if {@code name} does not
         *         have a value
         * @return the value of the named {@code float} field
         * @throws IOException if there are I/O errors while reading from the
         *         underlying {@code InputStream}
         * @throws IllegalArgumentException if type of {@code name} is
         *         not serializable or if the field type is incorrect
         */
        public abstract float get(String name, float val) throws IOException;

        /**
         * Get the value of the named double field from the persistent field.
         *
         * @param  name the name of the field
         * @param  val the default value to use if {@code name} does not
         *         have a value
         * @return the value of the named {@code double} field
         * @throws IOException if there are I/O errors while reading from the
         *         underlying {@code InputStream}
         * @throws IllegalArgumentException if type of {@code name} is
         *         not serializable or if the field type is incorrect
         */
        public abstract double get(String name, double val) throws IOException;

        /**
         * Get the value of the named Object field from the persistent field.
         *
         * @param  name the name of the field
         * @param  val the default value to use if {@code name} does not
         *         have a value
         * @return the value of the named {@code Object} field
         * @throws IOException if there are I/O errors while reading from the
         *         underlying {@code InputStream}
         * @throws IllegalArgumentException if type of {@code name} is
         *         not serializable or if the field type is incorrect
         */
        public abstract Object get(String name, Object val) throws IOException;
    }

    /**
     * Verifies that this (possibly subclass) instance can be constructed
     * without violating security constraints: the subclass must not override
     * security-sensitive non-final methods, or else the
     * "enableSubclassImplementation" SerializablePermission is checked.
     */
    private void verifySubclass() {
        Class<?> cl = getClass();
        if (cl == ObjectInputStream.class) {
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
            new PrivilegedAction<Boolean>() {
                public Boolean run() {
                    for (Class<?> cl = subcl;
                         cl != ObjectInputStream.class;
                         cl = cl.getSuperclass())
                    {
                        try {
                            cl.getDeclaredMethod(
                                "readUnshared", (Class[]) null);
                            return Boolean.FALSE;
                        } catch (NoSuchMethodException ex) {
                        }
                        try {
                            cl.getDeclaredMethod("readFields", (Class[]) null);
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
        handles.clear();
        vlist.clear();
    }

    /**
     * Underlying readObject implementation.
     * @param type a type expected to be deserialized; non-null
     * @param unshared true if the object can not be a reference to a shared object, otherwise false
     */
    private Object readObject0(Class<?> type, boolean unshared) throws IOException {
        boolean oldMode = bin.getBlockDataMode();
        if (oldMode) {
            int remain = bin.currentBlockRemaining();
            if (remain > 0) {
                throw new OptionalDataException(remain);
            } else if (defaultDataEnd) {
                /*
                 * Fix for 4360508: stream is currently at the end of a field
                 * value block written via default serialization; since there
                 * is no terminating TC_ENDBLOCKDATA tag, simulate
                 * end-of-custom-data behavior explicitly.
                 */
                throw new OptionalDataException(true);
            }
            bin.setBlockDataMode(false);
        }

        byte tc;
        while ((tc = bin.peekByte()) == TC_RESET) {
            bin.readByte();
            handleReset();
        }

        depth++;
        totalObjectRefs++;
        try {
            switch (tc) {
                case TC_NULL:
                    return readNull();

                case TC_REFERENCE:
                    // check the type of the existing object
                    return type.cast(readHandle(unshared));

                case TC_CLASS:
                    if (type == String.class) {
                        throw new ClassCastException("Cannot cast a class to java.lang.String");
                    }
                    return readClass(unshared);

                case TC_CLASSDESC:
                case TC_PROXYCLASSDESC:
                    if (type == String.class) {
                        throw new ClassCastException("Cannot cast a class to java.lang.String");
                    }
                    return readClassDesc(unshared);

                case TC_STRING:
                case TC_LONGSTRING:
                    return checkResolve(readString(unshared));

                case TC_ARRAY:
                    if (type == String.class) {
                        throw new ClassCastException("Cannot cast an array to java.lang.String");
                    }
                    return checkResolve(readArray(unshared));

                case TC_ENUM:
                    if (type == String.class) {
                        throw new ClassCastException("Cannot cast an enum to java.lang.String");
                    }
                    return checkResolve(readEnum(unshared));

                case TC_OBJECT:
                    if (type == String.class) {
                        throw new ClassCastException("Cannot cast an object to java.lang.String");
                    }
                    return checkResolve(readOrdinaryObject(unshared));

                case TC_EXCEPTION:
                    if (type == String.class) {
                        throw new ClassCastException("Cannot cast an exception to java.lang.String");
                    }
                    IOException ex = readFatalException();
                    throw new WriteAbortedException("writing aborted", ex);

                case TC_BLOCKDATA:
                case TC_BLOCKDATALONG:
                    if (oldMode) {
                        bin.setBlockDataMode(true);
                        bin.peek();             // force header read
                        throw new OptionalDataException(
                            bin.currentBlockRemaining());
                    } else {
                        throw new StreamCorruptedException(
                            "unexpected block data");
                    }

                case TC_ENDBLOCKDATA:
                    if (oldMode) {
                        throw new OptionalDataException(true);
                    } else {
                        throw new StreamCorruptedException(
                            "unexpected end of block data");
                    }

                default:
                    throw new StreamCorruptedException(
                        String.format("invalid type code: %02X", tc));
            }
        } finally {
            depth--;
            bin.setBlockDataMode(oldMode);
        }
    }

    /**
     * If resolveObject has been enabled and given object does not have an
     * exception associated with it, calls resolveObject to determine
     * replacement for object, and updates handle table accordingly.  Returns
     * replacement object, or echoes provided object if no replacement
     * occurred.  Expects that passHandle is set to given object's handle prior
     * to calling this method.
     */
    private Object checkResolve(Object obj) throws IOException {
        if (!enableResolve || handles.lookupException(passHandle) != null) {
            return obj;
        }
        Object rep = resolveObject(obj);
        if (rep != obj) {
            // The type of the original object has been filtered but resolveObject
            // may have replaced it;  filter the replacement's type
            if (rep != null) {
                if (rep.getClass().isArray()) {
                    filterCheck(rep.getClass(), Array.getLength(rep));
                } else {
                    filterCheck(rep.getClass(), -1);
                }
            }
            handles.setObject(passHandle, rep);
        }
        return rep;
    }

    /**
     * Reads string without allowing it to be replaced in stream.  Called from
     * within ObjectStreamClass.read().
     */
    String readTypeString() throws IOException {
        int oldHandle = passHandle;
        try {
            byte tc = bin.peekByte();
            return switch (tc) {
                case TC_NULL                  -> (String) readNull();
                case TC_REFERENCE             -> (String) readHandle(false);
                case TC_STRING, TC_LONGSTRING -> readString(false);
                default                       -> throw new StreamCorruptedException(
                        String.format("invalid type code: %02X", tc));
            };
        } finally {
            passHandle = oldHandle;
        }
    }

    /**
     * Reads in null code, sets passHandle to NULL_HANDLE and returns null.
     */
    private Object readNull() throws IOException {
        if (bin.readByte() != TC_NULL) {
            throw new InternalError();
        }
        passHandle = NULL_HANDLE;
        return null;
    }

    /**
     * Reads in object handle, sets passHandle to the read handle, and returns
     * object associated with the handle.
     */
    private Object readHandle(boolean unshared) throws IOException {
        if (bin.readByte() != TC_REFERENCE) {
            throw new InternalError();
        }
        passHandle = bin.readInt() - baseWireHandle;
        if (passHandle < 0 || passHandle >= handles.size()) {
            throw new StreamCorruptedException(
                String.format("invalid handle value: %08X", passHandle +
                baseWireHandle));
        }
        if (unshared) {
            // REMIND: what type of exception to throw here?
            throw new InvalidObjectException(
                "cannot read back reference as unshared");
        }

        Object obj = handles.lookupObject(passHandle);
        if (obj == unsharedMarker) {
            // REMIND: what type of exception to throw here?
            throw new InvalidObjectException(
                "cannot read back reference to unshared object");
        }
        filterCheck(null, -1);       // just a check for number of references, depth, no class
        return obj;
    }

    /**
     * Reads in and returns class object.  Sets passHandle to class object's
     * assigned handle.  Returns null if class is unresolvable (in which case a
     * ClassNotFoundException will be associated with the class' handle in the
     * handle table).
     */
    private Class<?> readClass(boolean unshared) throws IOException {
        if (bin.readByte() != TC_CLASS) {
            throw new InternalError();
        }
        ObjectStreamClass desc = readClassDesc(false);
        Class<?> cl = desc.forClass();
        passHandle = handles.assign(unshared ? unsharedMarker : cl);

        ClassNotFoundException resolveEx = desc.getResolveException();
        if (resolveEx != null) {
            handles.markException(passHandle, resolveEx);
        }

        handles.finish(passHandle);
        return cl;
    }

    /**
     * Reads in and returns (possibly null) class descriptor.  Sets passHandle
     * to class descriptor's assigned handle.  If class descriptor cannot be
     * resolved to a class in the local VM, a ClassNotFoundException is
     * associated with the class descriptor's handle.
     */
    private ObjectStreamClass readClassDesc(boolean unshared)
        throws IOException
    {
        byte tc = bin.peekByte();

        return switch (tc) {
            case TC_NULL            -> (ObjectStreamClass) readNull();
            case TC_PROXYCLASSDESC  -> readProxyDesc(unshared);
            case TC_CLASSDESC       -> readNonProxyDesc(unshared);
            case TC_REFERENCE       -> {
                var d = (ObjectStreamClass) readHandle(unshared);
                // Should only reference initialized class descriptors
                d.checkInitialized();
                yield d;
            }
            default                 -> throw new StreamCorruptedException(
                    String.format("invalid type code: %02X", tc));
        };
    }

    private boolean isCustomSubclass() {
        // Return true if this class is a custom subclass of ObjectInputStream
        return getClass().getClassLoader()
                    != ObjectInputStream.class.getClassLoader();
    }

    /**
     * Reads in and returns class descriptor for a dynamic proxy class.  Sets
     * passHandle to proxy class descriptor's assigned handle.  If proxy class
     * descriptor cannot be resolved to a class in the local VM, a
     * ClassNotFoundException is associated with the descriptor's handle.
     */
    private ObjectStreamClass readProxyDesc(boolean unshared)
        throws IOException
    {
        if (bin.readByte() != TC_PROXYCLASSDESC) {
            throw new InternalError();
        }

        ObjectStreamClass desc = new ObjectStreamClass();
        int descHandle = handles.assign(unshared ? unsharedMarker : desc);
        passHandle = NULL_HANDLE;

        int numIfaces = bin.readInt();
        if (numIfaces > 65535) {
            // Report specification limit exceeded
            throw new InvalidObjectException("interface limit exceeded: " +
                    numIfaces +
                    ", limit: " + Caches.PROXY_INTERFACE_LIMIT);
        }
        String[] ifaces = new String[numIfaces];
        for (int i = 0; i < numIfaces; i++) {
            ifaces[i] = bin.readUTF();
        }

        // Recheck against implementation limit and throw with interface names
        if (numIfaces > Caches.PROXY_INTERFACE_LIMIT) {
            throw new InvalidObjectException("interface limit exceeded: " +
                    numIfaces +
                    ", limit: " + Caches.PROXY_INTERFACE_LIMIT +
                    "; " + Arrays.toString(ifaces));
        }
        Class<?> cl = null;
        ClassNotFoundException resolveEx = null;
        bin.setBlockDataMode(true);
        try {
            if ((cl = resolveProxyClass(ifaces)) == null) {
                resolveEx = new ClassNotFoundException("null class");
            } else if (!Proxy.isProxyClass(cl)) {
                throw new InvalidClassException("Not a proxy");
            } else {
                // ReflectUtil.checkProxyPackageAccess makes a test
                // equivalent to isCustomSubclass so there's no need
                // to condition this call to isCustomSubclass == true here.
                ReflectUtil.checkProxyPackageAccess(
                        getClass().getClassLoader(),
                        cl.getInterfaces());
                // Filter the interfaces
                for (Class<?> clazz : cl.getInterfaces()) {
                    filterCheck(clazz, -1);
                }
            }
        } catch (ClassNotFoundException ex) {
            resolveEx = ex;
        } catch (OutOfMemoryError memerr) {
            IOException ex = new InvalidObjectException("Proxy interface limit exceeded: " +
                    Arrays.toString(ifaces));
            ex.initCause(memerr);
            throw ex;
        }

        // Call filterCheck on the class before reading anything else
        filterCheck(cl, -1);

        skipCustomData();

        try {
            totalObjectRefs++;
            depth++;
            desc.initProxy(cl, resolveEx, readClassDesc(false));
        } catch (OutOfMemoryError memerr) {
            IOException ex = new InvalidObjectException("Proxy interface limit exceeded: " +
                    Arrays.toString(ifaces));
            ex.initCause(memerr);
            throw ex;
        } finally {
            depth--;
        }

        handles.finish(descHandle);
        passHandle = descHandle;
        return desc;
    }

    /**
     * Reads in and returns class descriptor for a class that is not a dynamic
     * proxy class.  Sets passHandle to class descriptor's assigned handle.  If
     * class descriptor cannot be resolved to a class in the local VM, a
     * ClassNotFoundException is associated with the descriptor's handle.
     */
    private ObjectStreamClass readNonProxyDesc(boolean unshared)
        throws IOException
    {
        if (bin.readByte() != TC_CLASSDESC) {
            throw new InternalError();
        }

        ObjectStreamClass desc = new ObjectStreamClass();
        int descHandle = handles.assign(unshared ? unsharedMarker : desc);
        passHandle = NULL_HANDLE;

        ObjectStreamClass readDesc;
        try {
            readDesc = readClassDescriptor();
        } catch (ClassNotFoundException ex) {
            throw (IOException) new InvalidClassException(
                "failed to read class descriptor").initCause(ex);
        }

        Class<?> cl = null;
        ClassNotFoundException resolveEx = null;
        bin.setBlockDataMode(true);
        final boolean checksRequired = isCustomSubclass();
        try {
            if ((cl = resolveClass(readDesc)) == null) {
                resolveEx = new ClassNotFoundException("null class");
            } else if (checksRequired) {
                ReflectUtil.checkPackageAccess(cl);
            }
        } catch (ClassNotFoundException ex) {
            resolveEx = ex;
        }

        // Call filterCheck on the class before reading anything else
        filterCheck(cl, -1);

        skipCustomData();

        try {
            totalObjectRefs++;
            depth++;
            desc.initNonProxy(readDesc, cl, resolveEx, readClassDesc(false));
        } finally {
            depth--;
        }

        handles.finish(descHandle);
        passHandle = descHandle;

        return desc;
    }

    /**
     * Reads in and returns new string.  Sets passHandle to new string's
     * assigned handle.
     */
    private String readString(boolean unshared) throws IOException {
        byte tc = bin.readByte();
        String str = switch (tc) {
            case TC_STRING      -> bin.readUTF();
            case TC_LONGSTRING  -> bin.readLongUTF();
            default             -> throw new StreamCorruptedException(
                    String.format("invalid type code: %02X", tc));
        };
        passHandle = handles.assign(unshared ? unsharedMarker : str);
        handles.finish(passHandle);
        return str;
    }

    /**
     * Reads in and returns array object, or null if array class is
     * unresolvable.  Sets passHandle to array's assigned handle.
     */
    private Object readArray(boolean unshared) throws IOException {
        if (bin.readByte() != TC_ARRAY) {
            throw new InternalError();
        }

        ObjectStreamClass desc = readClassDesc(false);
        int len = bin.readInt();

        filterCheck(desc.forClass(), len);

        Object array = null;
        Class<?> cl, ccl = null;
        if ((cl = desc.forClass()) != null) {
            ccl = cl.getComponentType();
            array = Array.newInstance(ccl, len);
        }

        int arrayHandle = handles.assign(unshared ? unsharedMarker : array);
        ClassNotFoundException resolveEx = desc.getResolveException();
        if (resolveEx != null) {
            handles.markException(arrayHandle, resolveEx);
        }

        if (ccl == null) {
            for (int i = 0; i < len; i++) {
                readObject0(Object.class, false);
            }
        } else if (ccl.isPrimitive()) {
            if (ccl == Integer.TYPE) {
                bin.readInts((int[]) array, 0, len);
            } else if (ccl == Byte.TYPE) {
                bin.readFully((byte[]) array, 0, len, true);
            } else if (ccl == Long.TYPE) {
                bin.readLongs((long[]) array, 0, len);
            } else if (ccl == Float.TYPE) {
                bin.readFloats((float[]) array, 0, len);
            } else if (ccl == Double.TYPE) {
                bin.readDoubles((double[]) array, 0, len);
            } else if (ccl == Short.TYPE) {
                bin.readShorts((short[]) array, 0, len);
            } else if (ccl == Character.TYPE) {
                bin.readChars((char[]) array, 0, len);
            } else if (ccl == Boolean.TYPE) {
                bin.readBooleans((boolean[]) array, 0, len);
            } else {
                throw new InternalError();
            }
        } else {
            Object[] oa = (Object[]) array;
            for (int i = 0; i < len; i++) {
                oa[i] = readObject0(Object.class, false);
                handles.markDependency(arrayHandle, passHandle);
            }
        }

        handles.finish(arrayHandle);
        passHandle = arrayHandle;
        return array;
    }

    /**
     * Reads in and returns enum constant, or null if enum type is
     * unresolvable.  Sets passHandle to enum constant's assigned handle.
     */
    private Enum<?> readEnum(boolean unshared) throws IOException {
        if (bin.readByte() != TC_ENUM) {
            throw new InternalError();
        }

        ObjectStreamClass desc = readClassDesc(false);
        if (!desc.isEnum()) {
            throw new InvalidClassException("non-enum class: " + desc);
        }

        int enumHandle = handles.assign(unshared ? unsharedMarker : null);
        ClassNotFoundException resolveEx = desc.getResolveException();
        if (resolveEx != null) {
            handles.markException(enumHandle, resolveEx);
        }

        String name = readString(false);
        Enum<?> result = null;
        Class<?> cl = desc.forClass();
        if (cl != null) {
            try {
                @SuppressWarnings("unchecked")
                Enum<?> en = Enum.valueOf((Class)cl, name);
                result = en;
            } catch (IllegalArgumentException ex) {
                throw (IOException) new InvalidObjectException(
                    "enum constant " + name + " does not exist in " +
                    cl).initCause(ex);
            }
            if (!unshared) {
                handles.setObject(enumHandle, result);
            }
        }

        handles.finish(enumHandle);
        passHandle = enumHandle;
        return result;
    }

    /**
     * Reads and returns "ordinary" (i.e., not a String, Class,
     * ObjectStreamClass, array, or enum constant) object, or null if object's
     * class is unresolvable (in which case a ClassNotFoundException will be
     * associated with object's handle).  Sets passHandle to object's assigned
     * handle.
     */
    private Object readOrdinaryObject(boolean unshared)
        throws IOException
    {
        if (bin.readByte() != TC_OBJECT) {
            throw new InternalError();
        }

        ObjectStreamClass desc = readClassDesc(false);
        desc.checkDeserialize();

        Class<?> cl = desc.forClass();
        if (cl == String.class || cl == Class.class
                || cl == ObjectStreamClass.class) {
            throw new InvalidClassException("invalid class descriptor");
        }

        Object obj;
        try {
            obj = desc.isInstantiable() ? desc.newInstance() : null;
        } catch (Exception ex) {
            throw (IOException) new InvalidClassException(
                desc.forClass().getName(),
                "unable to create instance").initCause(ex);
        }

        passHandle = handles.assign(unshared ? unsharedMarker : obj);
        ClassNotFoundException resolveEx = desc.getResolveException();
        if (resolveEx != null) {
            handles.markException(passHandle, resolveEx);
        }

        final boolean isRecord = desc.isRecord();
        if (isRecord) {
            assert obj == null;
            obj = readRecord(desc);
            if (!unshared)
                handles.setObject(passHandle, obj);
        } else if (desc.isExternalizable()) {
            readExternalData((Externalizable) obj, desc);
        } else {
            readSerialData(obj, desc);
        }

        handles.finish(passHandle);

        if (obj != null &&
            handles.lookupException(passHandle) == null &&
            desc.hasReadResolveMethod())
        {
            Object rep = desc.invokeReadResolve(obj);
            if (unshared && rep.getClass().isArray()) {
                rep = cloneArray(rep);
            }
            if (rep != obj) {
                // Filter the replacement object
                if (rep != null) {
                    if (rep.getClass().isArray()) {
                        filterCheck(rep.getClass(), Array.getLength(rep));
                    } else {
                        filterCheck(rep.getClass(), -1);
                    }
                }
                handles.setObject(passHandle, obj = rep);
            }
        }

        return obj;
    }

    /**
     * If obj is non-null, reads externalizable data by invoking readExternal()
     * method of obj; otherwise, attempts to skip over externalizable data.
     * Expects that passHandle is set to obj's handle before this method is
     * called.
     */
    private void readExternalData(Externalizable obj, ObjectStreamClass desc)
        throws IOException
    {
        SerialCallbackContext oldContext = curContext;
        if (oldContext != null)
            oldContext.check();
        curContext = null;
        try {
            boolean blocked = desc.hasBlockExternalData();
            if (blocked) {
                bin.setBlockDataMode(true);
            }
            if (obj != null) {
                try {
                    obj.readExternal(this);
                } catch (ClassNotFoundException ex) {
                    /*
                     * In most cases, the handle table has already propagated
                     * a CNFException to passHandle at this point; this mark
                     * call is included to address cases where the readExternal
                     * method has cons'ed and thrown a new CNFException of its
                     * own.
                     */
                     handles.markException(passHandle, ex);
                }
            }
            if (blocked) {
                skipCustomData();
            }
        } finally {
            if (oldContext != null)
                oldContext.check();
            curContext = oldContext;
        }
        /*
         * At this point, if the externalizable data was not written in
         * block-data form and either the externalizable class doesn't exist
         * locally (i.e., obj == null) or readExternal() just threw a
         * CNFException, then the stream is probably in an inconsistent state,
         * since some (or all) of the externalizable data may not have been
         * consumed.  Since there's no "correct" action to take in this case,
         * we mimic the behavior of past serialization implementations and
         * blindly hope that the stream is in sync; if it isn't and additional
         * externalizable data remains in the stream, a subsequent read will
         * most likely throw a StreamCorruptedException.
         */
    }

    /** Reads a record. */
    private Object readRecord(ObjectStreamClass desc) throws IOException {
        ObjectStreamClass.ClassDataSlot[] slots = desc.getClassDataLayout();
        if (slots.length != 1) {
            // skip any superclass stream field values
            for (int i = 0; i < slots.length-1; i++) {
                if (slots[i].hasData) {
                    new FieldValues(slots[i].desc, true);
                }
            }
        }

        FieldValues fieldValues = new FieldValues(desc, true);

        // get canonical record constructor adapted to take two arguments:
        // - byte[] primValues
        // - Object[] objValues
        // and return Object
        MethodHandle ctrMH = RecordSupport.deserializationCtr(desc);

        try {
            return (Object) ctrMH.invokeExact(fieldValues.primValues, fieldValues.objValues);
        } catch (Exception e) {
            InvalidObjectException ioe = new InvalidObjectException(e.getMessage());
            ioe.initCause(e);
            throw ioe;
        } catch (Error e) {
            throw e;
        } catch (Throwable t) {
            ObjectStreamException ose = new InvalidObjectException(
                    "ReflectiveOperationException during deserialization");
            ose.initCause(t);
            throw ose;
        }
    }

    /**
     * Reads (or attempts to skip, if obj is null or is tagged with a
     * ClassNotFoundException) instance data for each serializable class of
     * object in stream, from superclass to subclass.  Expects that passHandle
     * is set to obj's handle before this method is called.
     */
    private void readSerialData(Object obj, ObjectStreamClass desc)
        throws IOException
    {
        ObjectStreamClass.ClassDataSlot[] slots = desc.getClassDataLayout();
        // Best effort Failure Atomicity; slotValues will be non-null if field
        // values can be set after reading all field data in the hierarchy.
        // Field values can only be set after reading all data if there are no
        // user observable methods in the hierarchy, readObject(NoData). The
        // top most Serializable class in the hierarchy can be skipped.
        FieldValues[] slotValues = null;

        boolean hasSpecialReadMethod = false;
        for (int i = 1; i < slots.length; i++) {
            ObjectStreamClass slotDesc = slots[i].desc;
            if (slotDesc.hasReadObjectMethod()
                  || slotDesc.hasReadObjectNoDataMethod()) {
                hasSpecialReadMethod = true;
                break;
            }
        }
        // No special read methods, can store values and defer setting.
        if (!hasSpecialReadMethod)
            slotValues = new FieldValues[slots.length];

        for (int i = 0; i < slots.length; i++) {
            ObjectStreamClass slotDesc = slots[i].desc;

            if (slots[i].hasData) {
                if (obj == null || handles.lookupException(passHandle) != null) {
                    // Read fields of the current descriptor into a new FieldValues and discard
                    new FieldValues(slotDesc, true);
                } else if (slotDesc.hasReadObjectMethod()) {
                    ThreadDeath t = null;
                    boolean reset = false;
                    SerialCallbackContext oldContext = curContext;
                    if (oldContext != null)
                        oldContext.check();
                    try {
                        curContext = new SerialCallbackContext(obj, slotDesc);

                        bin.setBlockDataMode(true);
                        slotDesc.invokeReadObject(obj, this);
                    } catch (ClassNotFoundException ex) {
                        /*
                         * In most cases, the handle table has already
                         * propagated a CNFException to passHandle at this
                         * point; this mark call is included to address cases
                         * where the custom readObject method has cons'ed and
                         * thrown a new CNFException of its own.
                         */
                        handles.markException(passHandle, ex);
                    } finally {
                        do {
                            try {
                                curContext.setUsed();
                                if (oldContext!= null)
                                    oldContext.check();
                                curContext = oldContext;
                                reset = true;
                            } catch (ThreadDeath x) {
                                t = x;  // defer until reset is true
                            }
                        } while (!reset);
                        if (t != null)
                            throw t;
                    }

                    /*
                     * defaultDataEnd may have been set indirectly by custom
                     * readObject() method when calling defaultReadObject() or
                     * readFields(); clear it to restore normal read behavior.
                     */
                    defaultDataEnd = false;
                } else {
                    // Read fields of the current descriptor into a new FieldValues
                    FieldValues values = new FieldValues(slotDesc, true);
                    if (slotValues != null) {
                        slotValues[i] = values;
                    } else if (obj != null) {
                        values.defaultCheckFieldValues(obj);
                        values.defaultSetFieldValues(obj);
                    }
                }

                if (slotDesc.hasWriteObjectData()) {
                    skipCustomData();
                } else {
                    bin.setBlockDataMode(false);
                }
            } else {
                if (obj != null &&
                    slotDesc.hasReadObjectNoDataMethod() &&
                    handles.lookupException(passHandle) == null)
                {
                    slotDesc.invokeReadObjectNoData(obj);
                }
            }
        }

        if (obj != null && slotValues != null) {
            // Check that the non-primitive types are assignable for all slots
            // before assigning.
            for (int i = 0; i < slots.length; i++) {
                if (slotValues[i] != null)
                    slotValues[i].defaultCheckFieldValues(obj);
            }
            for (int i = 0; i < slots.length; i++) {
                if (slotValues[i] != null)
                    slotValues[i].defaultSetFieldValues(obj);
            }
        }
    }

    /**
     * Skips over all block data and objects until TC_ENDBLOCKDATA is
     * encountered.
     */
    private void skipCustomData() throws IOException {
        int oldHandle = passHandle;
        for (;;) {
            if (bin.getBlockDataMode()) {
                bin.skipBlockData();
                bin.setBlockDataMode(false);
            }
            switch (bin.peekByte()) {
                case TC_BLOCKDATA:
                case TC_BLOCKDATALONG:
                    bin.setBlockDataMode(true);
                    break;

                case TC_ENDBLOCKDATA:
                    bin.readByte();
                    passHandle = oldHandle;
                    return;

                default:
                    readObject0(Object.class, false);
                    break;
            }
        }
    }

    /**
     * Reads in and returns IOException that caused serialization to abort.
     * All stream state is discarded prior to reading in fatal exception.  Sets
     * passHandle to fatal exception's handle.
     */
    private IOException readFatalException() throws IOException {
        if (bin.readByte() != TC_EXCEPTION) {
            throw new InternalError();
        }
        clear();
        return (IOException) readObject0(Object.class, false);
    }

    /**
     * If recursion depth is 0, clears internal data structures; otherwise,
     * throws a StreamCorruptedException.  This method is called when a
     * TC_RESET typecode is encountered.
     */
    private void handleReset() throws StreamCorruptedException {
        if (depth > 0) {
            throw new StreamCorruptedException(
                "unexpected reset; recursion depth: " + depth);
        }
        clear();
    }

    /**
     * Returns the first non-null and non-platform class loader (not counting
     * class loaders of generated reflection implementation classes) up the
     * execution stack, or the platform class loader if only code from the
     * bootstrap and platform class loader is on the stack.
     */
    private static ClassLoader latestUserDefinedLoader() {
        return jdk.internal.misc.VM.latestUserDefinedLoader();
    }

    /**
     * Default GetField implementation.
     */
    private final class FieldValues extends GetField {

        /** class descriptor describing serializable fields */
        private final ObjectStreamClass desc;
        /** primitive field values */
        final byte[] primValues;
        /** object field values */
        final Object[] objValues;
        /** object field value handles */
        private final int[] objHandles;

        /**
         * Creates FieldValues object for reading fields defined in given
         * class descriptor.
         * @param desc the ObjectStreamClass to read
         * @param recordDependencies if true, record the dependencies
         *                           from current PassHandle and the object's read.
         */
        FieldValues(ObjectStreamClass desc, boolean recordDependencies) throws IOException {
            this.desc = desc;

            int primDataSize = desc.getPrimDataSize();
            primValues = (primDataSize > 0) ? new byte[primDataSize] : null;
            if (primDataSize > 0) {
                bin.readFully(primValues, 0, primDataSize, false);
            }

            int numObjFields = desc.getNumObjFields();
            objValues = (numObjFields > 0) ? new Object[numObjFields] : null;
            objHandles = (numObjFields > 0) ? new int[numObjFields] : null;
            if (numObjFields > 0) {
                int objHandle = passHandle;
                ObjectStreamField[] fields = desc.getFields(false);
                int numPrimFields = fields.length - objValues.length;
                for (int i = 0; i < objValues.length; i++) {
                    ObjectStreamField f = fields[numPrimFields + i];
                    objValues[i] = readObject0(Object.class, f.isUnshared());
                    objHandles[i] = passHandle;
                    if (recordDependencies && f.getField() != null) {
                        handles.markDependency(objHandle, passHandle);
                    }
                }
                passHandle = objHandle;
            }
        }

        public ObjectStreamClass getObjectStreamClass() {
            return desc;
        }

        public boolean defaulted(String name) {
            return (getFieldOffset(name, null) < 0);
        }

        public boolean get(String name, boolean val) {
            int off = getFieldOffset(name, Boolean.TYPE);
            return (off >= 0) ? Bits.getBoolean(primValues, off) : val;
        }

        public byte get(String name, byte val) {
            int off = getFieldOffset(name, Byte.TYPE);
            return (off >= 0) ? primValues[off] : val;
        }

        public char get(String name, char val) {
            int off = getFieldOffset(name, Character.TYPE);
            return (off >= 0) ? Bits.getChar(primValues, off) : val;
        }

        public short get(String name, short val) {
            int off = getFieldOffset(name, Short.TYPE);
            return (off >= 0) ? Bits.getShort(primValues, off) : val;
        }

        public int get(String name, int val) {
            int off = getFieldOffset(name, Integer.TYPE);
            return (off >= 0) ? Bits.getInt(primValues, off) : val;
        }

        public float get(String name, float val) {
            int off = getFieldOffset(name, Float.TYPE);
            return (off >= 0) ? Bits.getFloat(primValues, off) : val;
        }

        public long get(String name, long val) {
            int off = getFieldOffset(name, Long.TYPE);
            return (off >= 0) ? Bits.getLong(primValues, off) : val;
        }

        public double get(String name, double val) {
            int off = getFieldOffset(name, Double.TYPE);
            return (off >= 0) ? Bits.getDouble(primValues, off) : val;
        }

        public Object get(String name, Object val) {
            int off = getFieldOffset(name, Object.class);
            if (off >= 0) {
                int objHandle = objHandles[off];
                handles.markDependency(passHandle, objHandle);
                return (handles.lookupException(objHandle) == null) ?
                    objValues[off] : null;
            } else {
                return val;
            }
        }

        /** Throws ClassCastException if any value is not assignable. */
        void defaultCheckFieldValues(Object obj) {
            if (objValues != null)
                desc.checkObjFieldValueTypes(obj, objValues);
        }

        private void defaultSetFieldValues(Object obj) {
            if (primValues != null)
                desc.setPrimFieldValues(obj, primValues);
            if (objValues != null)
                desc.setObjFieldValues(obj, objValues);
        }

        /**
         * Returns offset of field with given name and type.  A specified type
         * of null matches all types, Object.class matches all non-primitive
         * types, and any other non-null type matches assignable types only.
         * If no matching field is found in the (incoming) class
         * descriptor but a matching field is present in the associated local
         * class descriptor, returns -1.  Throws IllegalArgumentException if
         * neither incoming nor local class descriptor contains a match.
         */
        private int getFieldOffset(String name, Class<?> type) {
            ObjectStreamField field = desc.getField(name, type);
            if (field != null) {
                return field.getOffset();
            } else if (desc.getLocalDesc().getField(name, type) != null) {
                return -1;
            } else {
                throw new IllegalArgumentException("no such field " + name +
                                                   " with type " + type);
            }
        }
    }

    /**
     * Prioritized list of callbacks to be performed once object graph has been
     * completely deserialized.
     */
    private static class ValidationList {

        private static class Callback {
            final ObjectInputValidation obj;
            final int priority;
            Callback next;
            @SuppressWarnings("removal")
            final AccessControlContext acc;

            Callback(ObjectInputValidation obj, int priority, Callback next,
                @SuppressWarnings("removal") AccessControlContext acc)
            {
                this.obj = obj;
                this.priority = priority;
                this.next = next;
                this.acc = acc;
            }
        }

        /** linked list of callbacks */
        private Callback list;

        /**
         * Creates new (empty) ValidationList.
         */
        ValidationList() {
        }

        /**
         * Registers callback.  Throws InvalidObjectException if callback
         * object is null.
         */
        void register(ObjectInputValidation obj, int priority)
            throws InvalidObjectException
        {
            if (obj == null) {
                throw new InvalidObjectException("null callback");
            }

            Callback prev = null, cur = list;
            while (cur != null && priority < cur.priority) {
                prev = cur;
                cur = cur.next;
            }
            @SuppressWarnings("removal")
            AccessControlContext acc = AccessController.getContext();
            if (prev != null) {
                prev.next = new Callback(obj, priority, cur, acc);
            } else {
                list = new Callback(obj, priority, list, acc);
            }
        }

        /**
         * Invokes all registered callbacks and clears the callback list.
         * Callbacks with higher priorities are called first; those with equal
         * priorities may be called in any order.  If any of the callbacks
         * throws an InvalidObjectException, the callback process is terminated
         * and the exception propagated upwards.
         */
        @SuppressWarnings("removal")
        void doCallbacks() throws InvalidObjectException {
            try {
                while (list != null) {
                    AccessController.doPrivileged(
                        new PrivilegedExceptionAction<Void>()
                    {
                        public Void run() throws InvalidObjectException {
                            list.obj.validateObject();
                            return null;
                        }
                    }, list.acc);
                    list = list.next;
                }
            } catch (PrivilegedActionException ex) {
                list = null;
                throw (InvalidObjectException) ex.getException();
            }
        }

        /**
         * Resets the callback list to its initial (empty) state.
         */
        public void clear() {
            list = null;
        }
    }

    /**
     * Hold a snapshot of values to be passed to an ObjectInputFilter.
     */
    static class FilterValues implements ObjectInputFilter.FilterInfo {
        final Class<?> clazz;
        final long arrayLength;
        final long totalObjectRefs;
        final long depth;
        final long streamBytes;

        public FilterValues(Class<?> clazz, long arrayLength, long totalObjectRefs,
                            long depth, long streamBytes) {
            this.clazz = clazz;
            this.arrayLength = arrayLength;
            this.totalObjectRefs = totalObjectRefs;
            this.depth = depth;
            this.streamBytes = streamBytes;
        }

        @Override
        public Class<?> serialClass() {
            return clazz;
        }

        @Override
        public long arrayLength() {
            return arrayLength;
        }

        @Override
        public long references() {
            return totalObjectRefs;
        }

        @Override
        public long depth() {
            return depth;
        }

        @Override
        public long streamBytes() {
            return streamBytes;
        }
    }

    /**
     * Input stream supporting single-byte peek operations.
     */
    private static class PeekInputStream extends InputStream {

        /** underlying stream */
        private final InputStream in;
        /** peeked byte */
        private int peekb = -1;
        /** total bytes read from the stream */
        private long totalBytesRead = 0;

        /**
         * Creates new PeekInputStream on top of given underlying stream.
         */
        PeekInputStream(InputStream in) {
            this.in = in;
        }

        /**
         * Peeks at next byte value in stream.  Similar to read(), except
         * that it does not consume the read value.
         */
        int peek() throws IOException {
            if (peekb >= 0) {
                return peekb;
            }
            peekb = in.read();
            totalBytesRead += peekb >= 0 ? 1 : 0;
            return peekb;
        }

        public int read() throws IOException {
            if (peekb >= 0) {
                int v = peekb;
                peekb = -1;
                return v;
            } else {
                int nbytes = in.read();
                totalBytesRead += nbytes >= 0 ? 1 : 0;
                return nbytes;
            }
        }

        public int read(byte[] b, int off, int len) throws IOException {
            int nbytes;
            if (len == 0) {
                return 0;
            } else if (peekb < 0) {
                nbytes = in.read(b, off, len);
                totalBytesRead += nbytes >= 0 ? nbytes : 0;
                return nbytes;
            } else {
                b[off++] = (byte) peekb;
                len--;
                peekb = -1;
                nbytes = in.read(b, off, len);
                totalBytesRead += nbytes >= 0 ? nbytes : 0;
                return (nbytes >= 0) ? (nbytes + 1) : 1;
            }
        }

        void readFully(byte[] b, int off, int len) throws IOException {
            int n = 0;
            while (n < len) {
                int count = read(b, off + n, len - n);
                if (count < 0) {
                    throw new EOFException();
                }
                n += count;
            }
        }

        public long skip(long n) throws IOException {
            if (n <= 0) {
                return 0;
            }
            int skipped = 0;
            if (peekb >= 0) {
                peekb = -1;
                skipped++;
                n--;
            }
            n = skipped + in.skip(n);
            totalBytesRead += n;
            return n;
        }

        public int available() throws IOException {
            return in.available() + ((peekb >= 0) ? 1 : 0);
        }

        public void close() throws IOException {
            in.close();
        }

        public long getBytesRead() {
            return totalBytesRead;
        }
    }

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();

    /**
     * Performs a "freeze" action, required to adhere to final field semantics.
     *
     * <p> This method can be called unconditionally before returning the graph,
     * from the topmost readObject call, since it is expected that the
     * additional cost of the freeze action is negligible compared to
     * reconstituting even the most simple graph.
     *
     * <p> Nested calls to readObject do not issue freeze actions because the
     * sub-graph returned from a nested call is not guaranteed to be fully
     * initialized yet (possible cycles).
     */
    private void freeze() {
        // Issue a StoreStore|StoreLoad fence, which is at least sufficient
        // to provide final-freeze semantics.
        UNSAFE.storeFence();
    }

    /**
     * Input stream with two modes: in default mode, inputs data written in the
     * same format as DataOutputStream; in "block data" mode, inputs data
     * bracketed by block data markers (see object serialization specification
     * for details).  Buffering depends on block data mode: when in default
     * mode, no data is buffered in advance; when in block data mode, all data
     * for the current data block is read in at once (and buffered).
     */
    private class BlockDataInputStream
        extends InputStream implements DataInput
    {
        /** maximum data block length */
        private static final int MAX_BLOCK_SIZE = 1024;
        /** maximum data block header length */
        private static final int MAX_HEADER_SIZE = 5;
        /** (tunable) length of char buffer (for reading strings) */
        private static final int CHAR_BUF_SIZE = 256;
        /** readBlockHeader() return value indicating header read may block */
        private static final int HEADER_BLOCKED = -2;

        /** buffer for reading general/block data */
        private final byte[] buf = new byte[MAX_BLOCK_SIZE];
        /** buffer for reading block data headers */
        private final byte[] hbuf = new byte[MAX_HEADER_SIZE];
        /** char buffer for fast string reads */
        private final char[] cbuf = new char[CHAR_BUF_SIZE];

        /** block data mode */
        private boolean blkmode = false;

        // block data state fields; values meaningful only when blkmode true
        /** current offset into buf */
        private int pos = 0;
        /** end offset of valid data in buf, or -1 if no more block data */
        private int end = -1;
        /** number of bytes in current block yet to be read from stream */
        private int unread = 0;

        /** underlying stream (wrapped in peekable filter stream) */
        private final PeekInputStream in;
        /** loopback stream (for data reads that span data blocks) */
        private final DataInputStream din;

        /**
         * Creates new BlockDataInputStream on top of given underlying stream.
         * Block data mode is turned off by default.
         */
        BlockDataInputStream(InputStream in) {
            this.in = new PeekInputStream(in);
            din = new DataInputStream(this);
        }

        /**
         * Sets block data mode to the given mode (true == on, false == off)
         * and returns the previous mode value.  If the new mode is the same as
         * the old mode, no action is taken.  Throws IllegalStateException if
         * block data mode is being switched from on to off while unconsumed
         * block data is still present in the stream.
         */
        boolean setBlockDataMode(boolean newmode) throws IOException {
            if (blkmode == newmode) {
                return blkmode;
            }
            if (newmode) {
                pos = 0;
                end = 0;
                unread = 0;
            } else if (pos < end) {
                throw new IllegalStateException("unread block data");
            }
            blkmode = newmode;
            return !blkmode;
        }

        /**
         * Returns true if the stream is currently in block data mode, false
         * otherwise.
         */
        boolean getBlockDataMode() {
            return blkmode;
        }

        /**
         * If in block data mode, skips to the end of the current group of data
         * blocks (but does not unset block data mode).  If not in block data
         * mode, throws an IllegalStateException.
         */
        void skipBlockData() throws IOException {
            if (!blkmode) {
                throw new IllegalStateException("not in block data mode");
            }
            while (end >= 0) {
                refill();
            }
        }

        /**
         * Attempts to read in the next block data header (if any).  If
         * canBlock is false and a full header cannot be read without possibly
         * blocking, returns HEADER_BLOCKED, else if the next element in the
         * stream is a block data header, returns the block data length
         * specified by the header, else returns -1.
         */
        private int readBlockHeader(boolean canBlock) throws IOException {
            if (defaultDataEnd) {
                /*
                 * Fix for 4360508: stream is currently at the end of a field
                 * value block written via default serialization; since there
                 * is no terminating TC_ENDBLOCKDATA tag, simulate
                 * end-of-custom-data behavior explicitly.
                 */
                return -1;
            }
            try {
                for (;;) {
                    int avail = canBlock ? Integer.MAX_VALUE : in.available();
                    if (avail == 0) {
                        return HEADER_BLOCKED;
                    }

                    int tc = in.peek();
                    switch (tc) {
                        case TC_BLOCKDATA:
                            if (avail < 2) {
                                return HEADER_BLOCKED;
                            }
                            in.readFully(hbuf, 0, 2);
                            return hbuf[1] & 0xFF;

                        case TC_BLOCKDATALONG:
                            if (avail < 5) {
                                return HEADER_BLOCKED;
                            }
                            in.readFully(hbuf, 0, 5);
                            int len = Bits.getInt(hbuf, 1);
                            if (len < 0) {
                                throw new StreamCorruptedException(
                                    "illegal block data header length: " +
                                    len);
                            }
                            return len;

                        /*
                         * TC_RESETs may occur in between data blocks.
                         * Unfortunately, this case must be parsed at a lower
                         * level than other typecodes, since primitive data
                         * reads may span data blocks separated by a TC_RESET.
                         */
                        case TC_RESET:
                            in.read();
                            handleReset();
                            break;

                        default:
                            if (tc >= 0 && (tc < TC_BASE || tc > TC_MAX)) {
                                throw new StreamCorruptedException(
                                    String.format("invalid type code: %02X",
                                    tc));
                            }
                            return -1;
                    }
                }
            } catch (EOFException ex) {
                throw new StreamCorruptedException(
                    "unexpected EOF while reading block data header");
            }
        }

        /**
         * Refills internal buffer buf with block data.  Any data in buf at the
         * time of the call is considered consumed.  Sets the pos, end, and
         * unread fields to reflect the new amount of available block data; if
         * the next element in the stream is not a data block, sets pos and
         * unread to 0 and end to -1.
         */
        private void refill() throws IOException {
            try {
                do {
                    pos = 0;
                    if (unread > 0) {
                        int n =
                            in.read(buf, 0, Math.min(unread, MAX_BLOCK_SIZE));
                        if (n >= 0) {
                            end = n;
                            unread -= n;
                        } else {
                            throw new StreamCorruptedException(
                                "unexpected EOF in middle of data block");
                        }
                    } else {
                        int n = readBlockHeader(true);
                        if (n >= 0) {
                            end = 0;
                            unread = n;
                        } else {
                            end = -1;
                            unread = 0;
                        }
                    }
                } while (pos == end);
            } catch (IOException ex) {
                pos = 0;
                end = -1;
                unread = 0;
                throw ex;
            }
        }

        /**
         * If in block data mode, returns the number of unconsumed bytes
         * remaining in the current data block.  If not in block data mode,
         * throws an IllegalStateException.
         */
        int currentBlockRemaining() {
            if (blkmode) {
                return (end >= 0) ? (end - pos) + unread : 0;
            } else {
                throw new IllegalStateException();
            }
        }

        /**
         * Peeks at (but does not consume) and returns the next byte value in
         * the stream, or -1 if the end of the stream/block data (if in block
         * data mode) has been reached.
         */
        int peek() throws IOException {
            if (blkmode) {
                if (pos == end) {
                    refill();
                }
                return (end >= 0) ? (buf[pos] & 0xFF) : -1;
            } else {
                return in.peek();
            }
        }

        /**
         * Peeks at (but does not consume) and returns the next byte value in
         * the stream, or throws EOFException if end of stream/block data has
         * been reached.
         */
        byte peekByte() throws IOException {
            int val = peek();
            if (val < 0) {
                throw new EOFException();
            }
            return (byte) val;
        }


        /* ----------------- generic input stream methods ------------------ */
        /*
         * The following methods are equivalent to their counterparts in
         * InputStream, except that they interpret data block boundaries and
         * read the requested data from within data blocks when in block data
         * mode.
         */

        public int read() throws IOException {
            if (blkmode) {
                if (pos == end) {
                    refill();
                }
                return (end >= 0) ? (buf[pos++] & 0xFF) : -1;
            } else {
                return in.read();
            }
        }

        public int read(byte[] b, int off, int len) throws IOException {
            return read(b, off, len, false);
        }

        public long skip(long len) throws IOException {
            long remain = len;
            while (remain > 0) {
                if (blkmode) {
                    if (pos == end) {
                        refill();
                    }
                    if (end < 0) {
                        break;
                    }
                    int nread = (int) Math.min(remain, end - pos);
                    remain -= nread;
                    pos += nread;
                } else {
                    int nread = (int) Math.min(remain, MAX_BLOCK_SIZE);
                    if ((nread = in.read(buf, 0, nread)) < 0) {
                        break;
                    }
                    remain -= nread;
                }
            }
            return len - remain;
        }

        public int available() throws IOException {
            if (blkmode) {
                if ((pos == end) && (unread == 0)) {
                    int n;
                    while ((n = readBlockHeader(false)) == 0) ;
                    switch (n) {
                        case HEADER_BLOCKED:
                            break;

                        case -1:
                            pos = 0;
                            end = -1;
                            break;

                        default:
                            pos = 0;
                            end = 0;
                            unread = n;
                            break;
                    }
                }
                // avoid unnecessary call to in.available() if possible
                int unreadAvail = (unread > 0) ?
                    Math.min(in.available(), unread) : 0;
                return (end >= 0) ? (end - pos) + unreadAvail : 0;
            } else {
                return in.available();
            }
        }

        public void close() throws IOException {
            if (blkmode) {
                pos = 0;
                end = -1;
                unread = 0;
            }
            in.close();
        }

        /**
         * Attempts to read len bytes into byte array b at offset off.  Returns
         * the number of bytes read, or -1 if the end of stream/block data has
         * been reached.  If copy is true, reads values into an intermediate
         * buffer before copying them to b (to avoid exposing a reference to
         * b).
         */
        int read(byte[] b, int off, int len, boolean copy) throws IOException {
            if (len == 0) {
                return 0;
            } else if (blkmode) {
                if (pos == end) {
                    refill();
                }
                if (end < 0) {
                    return -1;
                }
                int nread = Math.min(len, end - pos);
                System.arraycopy(buf, pos, b, off, nread);
                pos += nread;
                return nread;
            } else if (copy) {
                int nread = in.read(buf, 0, Math.min(len, MAX_BLOCK_SIZE));
                if (nread > 0) {
                    System.arraycopy(buf, 0, b, off, nread);
                }
                return nread;
            } else {
                return in.read(b, off, len);
            }
        }

        /* ----------------- primitive data input methods ------------------ */
        /*
         * The following methods are equivalent to their counterparts in
         * DataInputStream, except that they interpret data block boundaries
         * and read the requested data from within data blocks when in block
         * data mode.
         */

        public void readFully(byte[] b) throws IOException {
            readFully(b, 0, b.length, false);
        }

        public void readFully(byte[] b, int off, int len) throws IOException {
            readFully(b, off, len, false);
        }

        public void readFully(byte[] b, int off, int len, boolean copy)
            throws IOException
        {
            while (len > 0) {
                int n = read(b, off, len, copy);
                if (n < 0) {
                    throw new EOFException();
                }
                off += n;
                len -= n;
            }
        }

        public int skipBytes(int n) throws IOException {
            return din.skipBytes(n);
        }

        public boolean readBoolean() throws IOException {
            int v = read();
            if (v < 0) {
                throw new EOFException();
            }
            return (v != 0);
        }

        public byte readByte() throws IOException {
            int v = read();
            if (v < 0) {
                throw new EOFException();
            }
            return (byte) v;
        }

        public int readUnsignedByte() throws IOException {
            int v = read();
            if (v < 0) {
                throw new EOFException();
            }
            return v;
        }

        public char readChar() throws IOException {
            if (!blkmode) {
                pos = 0;
                in.readFully(buf, 0, 2);
            } else if (end - pos < 2) {
                return din.readChar();
            }
            char v = Bits.getChar(buf, pos);
            pos += 2;
            return v;
        }

        public short readShort() throws IOException {
            if (!blkmode) {
                pos = 0;
                in.readFully(buf, 0, 2);
            } else if (end - pos < 2) {
                return din.readShort();
            }
            short v = Bits.getShort(buf, pos);
            pos += 2;
            return v;
        }

        public int readUnsignedShort() throws IOException {
            if (!blkmode) {
                pos = 0;
                in.readFully(buf, 0, 2);
            } else if (end - pos < 2) {
                return din.readUnsignedShort();
            }
            int v = Bits.getShort(buf, pos) & 0xFFFF;
            pos += 2;
            return v;
        }

        public int readInt() throws IOException {
            if (!blkmode) {
                pos = 0;
                in.readFully(buf, 0, 4);
            } else if (end - pos < 4) {
                return din.readInt();
            }
            int v = Bits.getInt(buf, pos);
            pos += 4;
            return v;
        }

        public float readFloat() throws IOException {
            if (!blkmode) {
                pos = 0;
                in.readFully(buf, 0, 4);
            } else if (end - pos < 4) {
                return din.readFloat();
            }
            float v = Bits.getFloat(buf, pos);
            pos += 4;
            return v;
        }

        public long readLong() throws IOException {
            if (!blkmode) {
                pos = 0;
                in.readFully(buf, 0, 8);
            } else if (end - pos < 8) {
                return din.readLong();
            }
            long v = Bits.getLong(buf, pos);
            pos += 8;
            return v;
        }

        public double readDouble() throws IOException {
            if (!blkmode) {
                pos = 0;
                in.readFully(buf, 0, 8);
            } else if (end - pos < 8) {
                return din.readDouble();
            }
            double v = Bits.getDouble(buf, pos);
            pos += 8;
            return v;
        }

        public String readUTF() throws IOException {
            return readUTFBody(readUnsignedShort());
        }

        @SuppressWarnings("deprecation")
        public String readLine() throws IOException {
            return din.readLine();      // deprecated, not worth optimizing
        }

        /* -------------- primitive data array input methods --------------- */
        /*
         * The following methods read in spans of primitive data values.
         * Though equivalent to calling the corresponding primitive read
         * methods repeatedly, these methods are optimized for reading groups
         * of primitive data values more efficiently.
         */

        void readBooleans(boolean[] v, int off, int len) throws IOException {
            int stop, endoff = off + len;
            while (off < endoff) {
                if (!blkmode) {
                    int span = Math.min(endoff - off, MAX_BLOCK_SIZE);
                    in.readFully(buf, 0, span);
                    stop = off + span;
                    pos = 0;
                } else if (end - pos < 1) {
                    v[off++] = din.readBoolean();
                    continue;
                } else {
                    stop = Math.min(endoff, off + end - pos);
                }

                while (off < stop) {
                    v[off++] = Bits.getBoolean(buf, pos++);
                }
            }
        }

        void readChars(char[] v, int off, int len) throws IOException {
            int stop, endoff = off + len;
            while (off < endoff) {
                if (!blkmode) {
                    int span = Math.min(endoff - off, MAX_BLOCK_SIZE >> 1);
                    in.readFully(buf, 0, span << 1);
                    stop = off + span;
                    pos = 0;
                } else if (end - pos < 2) {
                    v[off++] = din.readChar();
                    continue;
                } else {
                    stop = Math.min(endoff, off + ((end - pos) >> 1));
                }

                while (off < stop) {
                    v[off++] = Bits.getChar(buf, pos);
                    pos += 2;
                }
            }
        }

        void readShorts(short[] v, int off, int len) throws IOException {
            int stop, endoff = off + len;
            while (off < endoff) {
                if (!blkmode) {
                    int span = Math.min(endoff - off, MAX_BLOCK_SIZE >> 1);
                    in.readFully(buf, 0, span << 1);
                    stop = off + span;
                    pos = 0;
                } else if (end - pos < 2) {
                    v[off++] = din.readShort();
                    continue;
                } else {
                    stop = Math.min(endoff, off + ((end - pos) >> 1));
                }

                while (off < stop) {
                    v[off++] = Bits.getShort(buf, pos);
                    pos += 2;
                }
            }
        }

        void readInts(int[] v, int off, int len) throws IOException {
            int stop, endoff = off + len;
            while (off < endoff) {
                if (!blkmode) {
                    int span = Math.min(endoff - off, MAX_BLOCK_SIZE >> 2);
                    in.readFully(buf, 0, span << 2);
                    stop = off + span;
                    pos = 0;
                } else if (end - pos < 4) {
                    v[off++] = din.readInt();
                    continue;
                } else {
                    stop = Math.min(endoff, off + ((end - pos) >> 2));
                }

                while (off < stop) {
                    v[off++] = Bits.getInt(buf, pos);
                    pos += 4;
                }
            }
        }

        void readFloats(float[] v, int off, int len) throws IOException {
            int stop, endoff = off + len;
            while (off < endoff) {
                if (!blkmode) {
                    int span = Math.min(endoff - off, MAX_BLOCK_SIZE >> 2);
                    in.readFully(buf, 0, span << 2);
                    stop = off + span;
                    pos = 0;
                } else if (end - pos < 4) {
                    v[off++] = din.readFloat();
                    continue;
                } else {
                    stop = Math.min(endoff, ((end - pos) >> 2));
                }

                while (off < stop) {
                    v[off++] = Bits.getFloat(buf, pos);
                    pos += 4;
                }
            }
        }

        void readLongs(long[] v, int off, int len) throws IOException {
            int stop, endoff = off + len;
            while (off < endoff) {
                if (!blkmode) {
                    int span = Math.min(endoff - off, MAX_BLOCK_SIZE >> 3);
                    in.readFully(buf, 0, span << 3);
                    stop = off + span;
                    pos = 0;
                } else if (end - pos < 8) {
                    v[off++] = din.readLong();
                    continue;
                } else {
                    stop = Math.min(endoff, off + ((end - pos) >> 3));
                }

                while (off < stop) {
                    v[off++] = Bits.getLong(buf, pos);
                    pos += 8;
                }
            }
        }

        void readDoubles(double[] v, int off, int len) throws IOException {
            int stop, endoff = off + len;
            while (off < endoff) {
                if (!blkmode) {
                    int span = Math.min(endoff - off, MAX_BLOCK_SIZE >> 3);
                    in.readFully(buf, 0, span << 3);
                    stop = off + span;
                    pos = 0;
                } else if (end - pos < 8) {
                    v[off++] = din.readDouble();
                    continue;
                } else {
                    stop = Math.min(endoff - off, ((end - pos) >> 3));
                }

                while (off < stop) {
                    v[off++] = Bits.getDouble(buf, pos);
                    pos += 8;
                }
            }
        }

        /**
         * Reads in string written in "long" UTF format.  "Long" UTF format is
         * identical to standard UTF, except that it uses an 8 byte header
         * (instead of the standard 2 bytes) to convey the UTF encoding length.
         */
        String readLongUTF() throws IOException {
            return readUTFBody(readLong());
        }

        /**
         * Reads in the "body" (i.e., the UTF representation minus the 2-byte
         * or 8-byte length header) of a UTF encoding, which occupies the next
         * utflen bytes.
         */
        private String readUTFBody(long utflen) throws IOException {
            StringBuilder sbuf;
            if (utflen > 0 && utflen < Integer.MAX_VALUE) {
                // a reasonable initial capacity based on the UTF length
                int initialCapacity = Math.min((int)utflen, 0xFFFF);
                sbuf = new StringBuilder(initialCapacity);
            } else {
                sbuf = new StringBuilder();
            }

            if (!blkmode) {
                end = pos = 0;
            }

            while (utflen > 0) {
                int avail = end - pos;
                if (avail >= 3 || (long) avail == utflen) {
                    utflen -= readUTFSpan(sbuf, utflen);
                } else {
                    if (blkmode) {
                        // near block boundary, read one byte at a time
                        utflen -= readUTFChar(sbuf, utflen);
                    } else {
                        // shift and refill buffer manually
                        if (avail > 0) {
                            System.arraycopy(buf, pos, buf, 0, avail);
                        }
                        pos = 0;
                        end = (int) Math.min(MAX_BLOCK_SIZE, utflen);
                        in.readFully(buf, avail, end - avail);
                    }
                }
            }

            return sbuf.toString();
        }

        /**
         * Reads span of UTF-encoded characters out of internal buffer
         * (starting at offset pos and ending at or before offset end),
         * consuming no more than utflen bytes.  Appends read characters to
         * sbuf.  Returns the number of bytes consumed.
         */
        private long readUTFSpan(StringBuilder sbuf, long utflen)
            throws IOException
        {
            int cpos = 0;
            int start = pos;
            int avail = Math.min(end - pos, CHAR_BUF_SIZE);
            // stop short of last char unless all of utf bytes in buffer
            int stop = pos + ((utflen > avail) ? avail - 2 : (int) utflen);
            boolean outOfBounds = false;

            try {
                while (pos < stop) {
                    int b1, b2, b3;
                    b1 = buf[pos++] & 0xFF;
                    switch (b1 >> 4) {
                        case 0, 1, 2, 3, 4, 5, 6, 7 -> // 1 byte format: 0xxxxxxx
                            cbuf[cpos++] = (char) b1;
                        case 12, 13 -> {  // 2 byte format: 110xxxxx 10xxxxxx
                            b2 = buf[pos++];
                            if ((b2 & 0xC0) != 0x80) {
                                throw new UTFDataFormatException();
                            }
                            cbuf[cpos++] = (char) (((b1 & 0x1F) << 6) |
                                                   ((b2 & 0x3F) << 0));
                        }
                        case 14 -> {  // 3 byte format: 1110xxxx 10xxxxxx 10xxxxxx
                            b3 = buf[pos + 1];
                            b2 = buf[pos + 0];
                            pos += 2;
                            if ((b2 & 0xC0) != 0x80 || (b3 & 0xC0) != 0x80) {
                                throw new UTFDataFormatException();
                            }
                            cbuf[cpos++] = (char) (((b1 & 0x0F) << 12) |
                                                   ((b2 & 0x3F) << 6) |
                                                   ((b3 & 0x3F) << 0));
                        }
                        default ->  throw new UTFDataFormatException(); // 10xx xxxx, 1111 xxxx
                    }
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                outOfBounds = true;
            } finally {
                if (outOfBounds || (pos - start) > utflen) {
                    /*
                     * Fix for 4450867: if a malformed utf char causes the
                     * conversion loop to scan past the expected end of the utf
                     * string, only consume the expected number of utf bytes.
                     */
                    pos = start + (int) utflen;
                    throw new UTFDataFormatException();
                }
            }

            sbuf.append(cbuf, 0, cpos);
            return pos - start;
        }

        /**
         * Reads in single UTF-encoded character one byte at a time, appends
         * the character to sbuf, and returns the number of bytes consumed.
         * This method is used when reading in UTF strings written in block
         * data mode to handle UTF-encoded characters which (potentially)
         * straddle block-data boundaries.
         */
        private int readUTFChar(StringBuilder sbuf, long utflen)
            throws IOException
        {
            int b1, b2, b3;
            b1 = readByte() & 0xFF;
            switch (b1 >> 4) {
                case 0, 1, 2, 3, 4, 5, 6, 7 -> {     // 1 byte format: 0xxxxxxx
                    sbuf.append((char) b1);
                    return 1;
                }
                case 12, 13 -> {    // 2 byte format: 110xxxxx 10xxxxxx
                    if (utflen < 2) {
                        throw new UTFDataFormatException();
                    }
                    b2 = readByte();
                    if ((b2 & 0xC0) != 0x80) {
                        throw new UTFDataFormatException();
                    }
                    sbuf.append((char) (((b1 & 0x1F) << 6) |
                                        ((b2 & 0x3F) << 0)));
                    return 2;
                }
                case 14 -> {    // 3 byte format: 1110xxxx 10xxxxxx 10xxxxxx
                    if (utflen < 3) {
                        if (utflen == 2) {
                            readByte();         // consume remaining byte
                        }
                        throw new UTFDataFormatException();
                    }
                    b2 = readByte();
                    b3 = readByte();
                    if ((b2 & 0xC0) != 0x80 || (b3 & 0xC0) != 0x80) {
                        throw new UTFDataFormatException();
                    }
                    sbuf.append((char) (((b1 & 0x0F) << 12) |
                                        ((b2 & 0x3F) << 6)  |
                                        ((b3 & 0x3F) << 0)));
                    return 3;
                }
                default -> throw new UTFDataFormatException(); // 10xx xxxx, 1111 xxxx
            }
        }

        /**
         * Returns the number of bytes read from the input stream.
         * @return the number of bytes read from the input stream
         */
        long getBytesRead() {
            return in.getBytesRead();
        }
    }

    /**
     * Unsynchronized table which tracks wire handle to object mappings, as
     * well as ClassNotFoundExceptions associated with deserialized objects.
     * This class implements an exception-propagation algorithm for
     * determining which objects should have ClassNotFoundExceptions associated
     * with them, taking into account cycles and discontinuities (e.g., skipped
     * fields) in the object graph.
     *
     * <p>General use of the table is as follows: during deserialization, a
     * given object is first assigned a handle by calling the assign method.
     * This method leaves the assigned handle in an "open" state, wherein
     * dependencies on the exception status of other handles can be registered
     * by calling the markDependency method, or an exception can be directly
     * associated with the handle by calling markException.  When a handle is
     * tagged with an exception, the HandleTable assumes responsibility for
     * propagating the exception to any other objects which depend
     * (transitively) on the exception-tagged object.
     *
     * <p>Once all exception information/dependencies for the handle have been
     * registered, the handle should be "closed" by calling the finish method
     * on it.  The act of finishing a handle allows the exception propagation
     * algorithm to aggressively prune dependency links, lessening the
     * performance/memory impact of exception tracking.
     *
     * <p>Note that the exception propagation algorithm used depends on handles
     * being assigned/finished in LIFO order; however, for simplicity as well
     * as memory conservation, it does not enforce this constraint.
     */
    // REMIND: add full description of exception propagation algorithm?
    private static class HandleTable {

        /* status codes indicating whether object has associated exception */
        private static final byte STATUS_OK = 1;
        private static final byte STATUS_UNKNOWN = 2;
        private static final byte STATUS_EXCEPTION = 3;

        /** array mapping handle -> object status */
        byte[] status;
        /** array mapping handle -> object/exception (depending on status) */
        Object[] entries;
        /** array mapping handle -> list of dependent handles (if any) */
        HandleList[] deps;
        /** lowest unresolved dependency */
        int lowDep = -1;
        /** number of handles in table */
        int size = 0;

        /**
         * Creates handle table with the given initial capacity.
         */
        HandleTable(int initialCapacity) {
            status = new byte[initialCapacity];
            entries = new Object[initialCapacity];
            deps = new HandleList[initialCapacity];
        }

        /**
         * Assigns next available handle to given object, and returns assigned
         * handle.  Once object has been completely deserialized (and all
         * dependencies on other objects identified), the handle should be
         * "closed" by passing it to finish().
         */
        int assign(Object obj) {
            if (size >= entries.length) {
                grow();
            }
            status[size] = STATUS_UNKNOWN;
            entries[size] = obj;
            return size++;
        }

        /**
         * Registers a dependency (in exception status) of one handle on
         * another.  The dependent handle must be "open" (i.e., assigned, but
         * not finished yet).  No action is taken if either dependent or target
         * handle is NULL_HANDLE. Additionally, no action is taken if the
         * dependent and target are the same.
         */
        void markDependency(int dependent, int target) {
            if (dependent == target || dependent == NULL_HANDLE || target == NULL_HANDLE) {
                return;
            }
            switch (status[dependent]) {

                case STATUS_UNKNOWN:
                    switch (status[target]) {
                        case STATUS_OK:
                            // ignore dependencies on objs with no exception
                            break;

                        case STATUS_EXCEPTION:
                            // eagerly propagate exception
                            markException(dependent,
                                (ClassNotFoundException) entries[target]);
                            break;

                        case STATUS_UNKNOWN:
                            // add to dependency list of target
                            if (deps[target] == null) {
                                deps[target] = new HandleList();
                            }
                            deps[target].add(dependent);

                            // remember lowest unresolved target seen
                            if (lowDep < 0 || lowDep > target) {
                                lowDep = target;
                            }
                            break;

                        default:
                            throw new InternalError();
                    }
                    break;

                case STATUS_EXCEPTION:
                    break;

                default:
                    throw new InternalError();
            }
        }

        /**
         * Associates a ClassNotFoundException (if one not already associated)
         * with the currently active handle and propagates it to other
         * referencing objects as appropriate.  The specified handle must be
         * "open" (i.e., assigned, but not finished yet).
         */
        void markException(int handle, ClassNotFoundException ex) {
            switch (status[handle]) {
                case STATUS_UNKNOWN:
                    status[handle] = STATUS_EXCEPTION;
                    entries[handle] = ex;

                    // propagate exception to dependents
                    HandleList dlist = deps[handle];
                    if (dlist != null) {
                        int ndeps = dlist.size();
                        for (int i = 0; i < ndeps; i++) {
                            markException(dlist.get(i), ex);
                        }
                        deps[handle] = null;
                    }
                    break;

                case STATUS_EXCEPTION:
                    break;

                default:
                    throw new InternalError();
            }
        }

        /**
         * Marks given handle as finished, meaning that no new dependencies
         * will be marked for handle.  Calls to the assign and finish methods
         * must occur in LIFO order.
         */
        void finish(int handle) {
            int end;
            if (lowDep < 0) {
                // no pending unknowns, only resolve current handle
                end = handle + 1;
            } else if (lowDep >= handle) {
                // pending unknowns now clearable, resolve all upward handles
                end = size;
                lowDep = -1;
            } else {
                // unresolved backrefs present, can't resolve anything yet
                return;
            }

            // change STATUS_UNKNOWN -> STATUS_OK in selected span of handles
            for (int i = handle; i < end; i++) {
                switch (status[i]) {
                    case STATUS_UNKNOWN:
                        status[i] = STATUS_OK;
                        deps[i] = null;
                        break;

                    case STATUS_OK:
                    case STATUS_EXCEPTION:
                        break;

                    default:
                        throw new InternalError();
                }
            }
        }

        /**
         * Assigns a new object to the given handle.  The object previously
         * associated with the handle is forgotten.  This method has no effect
         * if the given handle already has an exception associated with it.
         * This method may be called at any time after the handle is assigned.
         */
        void setObject(int handle, Object obj) {
            switch (status[handle]) {
                case STATUS_UNKNOWN:
                case STATUS_OK:
                    entries[handle] = obj;
                    break;

                case STATUS_EXCEPTION:
                    break;

                default:
                    throw new InternalError();
            }
        }

        /**
         * Looks up and returns object associated with the given handle.
         * Returns null if the given handle is NULL_HANDLE, or if it has an
         * associated ClassNotFoundException.
         */
        Object lookupObject(int handle) {
            return (handle != NULL_HANDLE &&
                    status[handle] != STATUS_EXCEPTION) ?
                entries[handle] : null;
        }

        /**
         * Looks up and returns ClassNotFoundException associated with the
         * given handle.  Returns null if the given handle is NULL_HANDLE, or
         * if there is no ClassNotFoundException associated with the handle.
         */
        ClassNotFoundException lookupException(int handle) {
            return (handle != NULL_HANDLE &&
                    status[handle] == STATUS_EXCEPTION) ?
                (ClassNotFoundException) entries[handle] : null;
        }

        /**
         * Resets table to its initial state.
         */
        void clear() {
            Arrays.fill(status, 0, size, (byte) 0);
            Arrays.fill(entries, 0, size, null);
            Arrays.fill(deps, 0, size, null);
            lowDep = -1;
            size = 0;
        }

        /**
         * Returns number of handles registered in table.
         */
        int size() {
            return size;
        }

        /**
         * Expands capacity of internal arrays.
         */
        private void grow() {
            int newCapacity = (entries.length << 1) + 1;

            byte[] newStatus = new byte[newCapacity];
            Object[] newEntries = new Object[newCapacity];
            HandleList[] newDeps = new HandleList[newCapacity];

            System.arraycopy(status, 0, newStatus, 0, size);
            System.arraycopy(entries, 0, newEntries, 0, size);
            System.arraycopy(deps, 0, newDeps, 0, size);

            status = newStatus;
            entries = newEntries;
            deps = newDeps;
        }

        /**
         * Simple growable list of (integer) handles.
         */
        private static class HandleList {
            private int[] list = new int[4];
            private int size = 0;

            public HandleList() {
            }

            public void add(int handle) {
                if (size >= list.length) {
                    int[] newList = new int[list.length << 1];
                    System.arraycopy(list, 0, newList, 0, list.length);
                    list = newList;
                }
                list[size++] = handle;
            }

            public int get(int index) {
                if (index >= size) {
                    throw new ArrayIndexOutOfBoundsException();
                }
                return list[index];
            }

            public int size() {
                return size;
            }
        }
    }

    /**
     * Method for cloning arrays in case of using unsharing reading
     */
    private static Object cloneArray(Object array) {
        if (array instanceof Object[]) {
            return ((Object[]) array).clone();
        } else if (array instanceof boolean[]) {
            return ((boolean[]) array).clone();
        } else if (array instanceof byte[]) {
            return ((byte[]) array).clone();
        } else if (array instanceof char[]) {
            return ((char[]) array).clone();
        } else if (array instanceof double[]) {
            return ((double[]) array).clone();
        } else if (array instanceof float[]) {
            return ((float[]) array).clone();
        } else if (array instanceof int[]) {
            return ((int[]) array).clone();
        } else if (array instanceof long[]) {
            return ((long[]) array).clone();
        } else if (array instanceof short[]) {
            return ((short[]) array).clone();
        } else {
            throw new AssertionError();
        }
    }

    static {
        SharedSecrets.setJavaObjectInputStreamAccess(ObjectInputStream::checkArray);
        SharedSecrets.setJavaObjectInputStreamReadString(ObjectInputStream::readString);
    }

}
