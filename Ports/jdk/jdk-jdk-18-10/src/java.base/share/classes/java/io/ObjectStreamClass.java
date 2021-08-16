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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.UndeclaredThrowableException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import jdk.internal.misc.Unsafe;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;
import jdk.internal.reflect.ReflectionFactory;
import jdk.internal.access.SharedSecrets;
import jdk.internal.access.JavaSecurityAccess;
import sun.reflect.misc.ReflectUtil;
import static java.io.ObjectStreamField.*;

/**
 * Serialization's descriptor for classes.  It contains the name and
 * serialVersionUID of the class.  The ObjectStreamClass for a specific class
 * loaded in this Java VM can be found/created using the lookup method.
 *
 * <p>The algorithm to compute the SerialVersionUID is described in
 * <a href="{@docRoot}/../specs/serialization/class.html#stream-unique-identifiers">
 *    <cite>Java Object Serialization Specification,</cite> Section 4.6, "Stream Unique Identifiers"</a>.
 *
 * @author      Mike Warres
 * @author      Roger Riggs
 * @see ObjectStreamField
 * @see <a href="{@docRoot}/../specs/serialization/class.html">
 *      <cite>Java Object Serialization Specification,</cite> Section 4, "Class Descriptors"</a>
 * @since   1.1
 */
public class ObjectStreamClass implements Serializable {

    /** serialPersistentFields value indicating no serializable fields */
    public static final ObjectStreamField[] NO_FIELDS =
        new ObjectStreamField[0];

    @java.io.Serial
    private static final long serialVersionUID = -6120832682080437368L;
    /**
     * {@code ObjectStreamClass} has no fields for default serialization.
     */
    @java.io.Serial
    private static final ObjectStreamField[] serialPersistentFields =
        NO_FIELDS;

    /** reflection factory for obtaining serialization constructors */
    @SuppressWarnings("removal")
    private static final ReflectionFactory reflFactory =
        AccessController.doPrivileged(
            new ReflectionFactory.GetReflectionFactoryAction());

    private static class Caches {
        /** cache mapping local classes -> descriptors */
        static final ConcurrentMap<WeakClassKey,Reference<?>> localDescs =
            new ConcurrentHashMap<>();

        /** cache mapping field group/local desc pairs -> field reflectors */
        static final ConcurrentMap<FieldReflectorKey,Reference<?>> reflectors =
            new ConcurrentHashMap<>();

        /** queue for WeakReferences to local classes */
        private static final ReferenceQueue<Class<?>> localDescsQueue =
            new ReferenceQueue<>();
        /** queue for WeakReferences to field reflectors keys */
        private static final ReferenceQueue<Class<?>> reflectorsQueue =
            new ReferenceQueue<>();
    }

    /** class associated with this descriptor (if any) */
    private Class<?> cl;
    /** name of class represented by this descriptor */
    private String name;
    /** serialVersionUID of represented class (null if not computed yet) */
    private volatile Long suid;

    /** true if represents dynamic proxy class */
    private boolean isProxy;
    /** true if represents enum type */
    private boolean isEnum;
    /** true if represents record type */
    private boolean isRecord;
    /** true if represented class implements Serializable */
    private boolean serializable;
    /** true if represented class implements Externalizable */
    private boolean externalizable;
    /** true if desc has data written by class-defined writeObject method */
    private boolean hasWriteObjectData;
    /**
     * true if desc has externalizable data written in block data format; this
     * must be true by default to accommodate ObjectInputStream subclasses which
     * override readClassDescriptor() to return class descriptors obtained from
     * ObjectStreamClass.lookup() (see 4461737)
     */
    private boolean hasBlockExternalData = true;

    /**
     * Contains information about InvalidClassException instances to be thrown
     * when attempting operations on an invalid class. Note that instances of
     * this class are immutable and are potentially shared among
     * ObjectStreamClass instances.
     */
    private static class ExceptionInfo {
        private final String className;
        private final String message;

        ExceptionInfo(String cn, String msg) {
            className = cn;
            message = msg;
        }

        /**
         * Returns (does not throw) an InvalidClassException instance created
         * from the information in this object, suitable for being thrown by
         * the caller.
         */
        InvalidClassException newInvalidClassException() {
            return new InvalidClassException(className, message);
        }
    }

    /** exception (if any) thrown while attempting to resolve class */
    private ClassNotFoundException resolveEx;
    /** exception (if any) to throw if non-enum deserialization attempted */
    private ExceptionInfo deserializeEx;
    /** exception (if any) to throw if non-enum serialization attempted */
    private ExceptionInfo serializeEx;
    /** exception (if any) to throw if default serialization attempted */
    private ExceptionInfo defaultSerializeEx;

    /** serializable fields */
    private ObjectStreamField[] fields;
    /** aggregate marshalled size of primitive fields */
    private int primDataSize;
    /** number of non-primitive fields */
    private int numObjFields;
    /** reflector for setting/getting serializable field values */
    private FieldReflector fieldRefl;
    /** data layout of serialized objects described by this class desc */
    private volatile ClassDataSlot[] dataLayout;

    /** serialization-appropriate constructor, or null if none */
    private Constructor<?> cons;
    /** record canonical constructor (shared among OSCs for same class), or null */
    private MethodHandle canonicalCtr;
    /** cache of record deserialization constructors per unique set of stream fields
     * (shared among OSCs for same class), or null */
    private DeserializationConstructorsCache deserializationCtrs;
    /** session-cache of record deserialization constructor
     * (in de-serialized OSC only), or null */
    private MethodHandle deserializationCtr;
    /** protection domains that need to be checked when calling the constructor */
    private ProtectionDomain[] domains;

    /** class-defined writeObject method, or null if none */
    private Method writeObjectMethod;
    /** class-defined readObject method, or null if none */
    private Method readObjectMethod;
    /** class-defined readObjectNoData method, or null if none */
    private Method readObjectNoDataMethod;
    /** class-defined writeReplace method, or null if none */
    private Method writeReplaceMethod;
    /** class-defined readResolve method, or null if none */
    private Method readResolveMethod;

    /** local class descriptor for represented class (may point to self) */
    private ObjectStreamClass localDesc;
    /** superclass descriptor appearing in stream */
    private ObjectStreamClass superDesc;

    /** true if, and only if, the object has been correctly initialized */
    private boolean initialized;

    /**
     * Initializes native code.
     */
    private static native void initNative();
    static {
        initNative();
    }

    /**
     * Find the descriptor for a class that can be serialized.  Creates an
     * ObjectStreamClass instance if one does not exist yet for class. Null is
     * returned if the specified class does not implement java.io.Serializable
     * or java.io.Externalizable.
     *
     * @param   cl class for which to get the descriptor
     * @return  the class descriptor for the specified class
     */
    public static ObjectStreamClass lookup(Class<?> cl) {
        return lookup(cl, false);
    }

    /**
     * Returns the descriptor for any class, regardless of whether it
     * implements {@link Serializable}.
     *
     * @param        cl class for which to get the descriptor
     * @return       the class descriptor for the specified class
     * @since 1.6
     */
    public static ObjectStreamClass lookupAny(Class<?> cl) {
        return lookup(cl, true);
    }

    /**
     * Returns the name of the class described by this descriptor.
     * This method returns the name of the class in the format that
     * is used by the {@link Class#getName} method.
     *
     * @return a string representing the name of the class
     */
    public String getName() {
        return name;
    }

    /**
     * Return the serialVersionUID for this class.  The serialVersionUID
     * defines a set of classes all with the same name that have evolved from a
     * common root class and agree to be serialized and deserialized using a
     * common format.  NonSerializable classes have a serialVersionUID of 0L.
     *
     * @return  the SUID of the class described by this descriptor
     */
    @SuppressWarnings("removal")
    public long getSerialVersionUID() {
        // REMIND: synchronize instead of relying on volatile?
        if (suid == null) {
            if (isRecord)
                return 0L;

            suid = AccessController.doPrivileged(
                new PrivilegedAction<Long>() {
                    public Long run() {
                        return computeDefaultSUID(cl);
                    }
                }
            );
        }
        return suid.longValue();
    }

    /**
     * Return the class in the local VM that this version is mapped to.  Null
     * is returned if there is no corresponding local class.
     *
     * @return  the {@code Class} instance that this descriptor represents
     */
    @SuppressWarnings("removal")
    @CallerSensitive
    public Class<?> forClass() {
        if (cl == null) {
            return null;
        }
        requireInitialized();
        if (System.getSecurityManager() != null) {
            Class<?> caller = Reflection.getCallerClass();
            if (ReflectUtil.needsPackageAccessCheck(caller.getClassLoader(), cl.getClassLoader())) {
                ReflectUtil.checkPackageAccess(cl);
            }
        }
        return cl;
    }

    /**
     * Return an array of the fields of this serializable class.
     *
     * @return  an array containing an element for each persistent field of
     *          this class. Returns an array of length zero if there are no
     *          fields.
     * @since 1.2
     */
    public ObjectStreamField[] getFields() {
        return getFields(true);
    }

    /**
     * Get the field of this class by name.
     *
     * @param   name the name of the data field to look for
     * @return  The ObjectStreamField object of the named field or null if
     *          there is no such named field.
     */
    public ObjectStreamField getField(String name) {
        return getField(name, null);
    }

    /**
     * Return a string describing this ObjectStreamClass.
     */
    public String toString() {
        return name + ": static final long serialVersionUID = " +
            getSerialVersionUID() + "L;";
    }

    /**
     * Looks up and returns class descriptor for given class, or null if class
     * is non-serializable and "all" is set to false.
     *
     * @param   cl class to look up
     * @param   all if true, return descriptors for all classes; if false, only
     *          return descriptors for serializable classes
     */
    static ObjectStreamClass lookup(Class<?> cl, boolean all) {
        if (!(all || Serializable.class.isAssignableFrom(cl))) {
            return null;
        }
        processQueue(Caches.localDescsQueue, Caches.localDescs);
        WeakClassKey key = new WeakClassKey(cl, Caches.localDescsQueue);
        Reference<?> ref = Caches.localDescs.get(key);
        Object entry = null;
        if (ref != null) {
            entry = ref.get();
        }
        EntryFuture future = null;
        if (entry == null) {
            EntryFuture newEntry = new EntryFuture();
            Reference<?> newRef = new SoftReference<>(newEntry);
            do {
                if (ref != null) {
                    Caches.localDescs.remove(key, ref);
                }
                ref = Caches.localDescs.putIfAbsent(key, newRef);
                if (ref != null) {
                    entry = ref.get();
                }
            } while (ref != null && entry == null);
            if (entry == null) {
                future = newEntry;
            }
        }

        if (entry instanceof ObjectStreamClass) {  // check common case first
            return (ObjectStreamClass) entry;
        }
        if (entry instanceof EntryFuture) {
            future = (EntryFuture) entry;
            if (future.getOwner() == Thread.currentThread()) {
                /*
                 * Handle nested call situation described by 4803747: waiting
                 * for future value to be set by a lookup() call further up the
                 * stack will result in deadlock, so calculate and set the
                 * future value here instead.
                 */
                entry = null;
            } else {
                entry = future.get();
            }
        }
        if (entry == null) {
            try {
                entry = new ObjectStreamClass(cl);
            } catch (Throwable th) {
                entry = th;
            }
            if (future.set(entry)) {
                Caches.localDescs.put(key, new SoftReference<>(entry));
            } else {
                // nested lookup call already set future
                entry = future.get();
            }
        }

        if (entry instanceof ObjectStreamClass) {
            return (ObjectStreamClass) entry;
        } else if (entry instanceof RuntimeException) {
            throw (RuntimeException) entry;
        } else if (entry instanceof Error) {
            throw (Error) entry;
        } else {
            throw new InternalError("unexpected entry: " + entry);
        }
    }

    /**
     * Placeholder used in class descriptor and field reflector lookup tables
     * for an entry in the process of being initialized.  (Internal) callers
     * which receive an EntryFuture belonging to another thread as the result
     * of a lookup should call the get() method of the EntryFuture; this will
     * return the actual entry once it is ready for use and has been set().  To
     * conserve objects, EntryFutures synchronize on themselves.
     */
    private static class EntryFuture {

        private static final Object unset = new Object();
        private final Thread owner = Thread.currentThread();
        private Object entry = unset;

        /**
         * Attempts to set the value contained by this EntryFuture.  If the
         * EntryFuture's value has not been set already, then the value is
         * saved, any callers blocked in the get() method are notified, and
         * true is returned.  If the value has already been set, then no saving
         * or notification occurs, and false is returned.
         */
        synchronized boolean set(Object entry) {
            if (this.entry != unset) {
                return false;
            }
            this.entry = entry;
            notifyAll();
            return true;
        }

        /**
         * Returns the value contained by this EntryFuture, blocking if
         * necessary until a value is set.
         */
        @SuppressWarnings("removal")
        synchronized Object get() {
            boolean interrupted = false;
            while (entry == unset) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    interrupted = true;
                }
            }
            if (interrupted) {
                AccessController.doPrivileged(
                    new PrivilegedAction<>() {
                        public Void run() {
                            Thread.currentThread().interrupt();
                            return null;
                        }
                    }
                );
            }
            return entry;
        }

        /**
         * Returns the thread that created this EntryFuture.
         */
        Thread getOwner() {
            return owner;
        }
    }

    /**
     * Creates local class descriptor representing given class.
     */
    @SuppressWarnings("removal")
    private ObjectStreamClass(final Class<?> cl) {
        this.cl = cl;
        name = cl.getName();
        isProxy = Proxy.isProxyClass(cl);
        isEnum = Enum.class.isAssignableFrom(cl);
        isRecord = cl.isRecord();
        serializable = Serializable.class.isAssignableFrom(cl);
        externalizable = Externalizable.class.isAssignableFrom(cl);

        Class<?> superCl = cl.getSuperclass();
        superDesc = (superCl != null) ? lookup(superCl, false) : null;
        localDesc = this;

        if (serializable) {
            AccessController.doPrivileged(new PrivilegedAction<>() {
                public Void run() {
                    if (isEnum) {
                        suid = 0L;
                        fields = NO_FIELDS;
                        return null;
                    }
                    if (cl.isArray()) {
                        fields = NO_FIELDS;
                        return null;
                    }

                    suid = getDeclaredSUID(cl);
                    try {
                        fields = getSerialFields(cl);
                        computeFieldOffsets();
                    } catch (InvalidClassException e) {
                        serializeEx = deserializeEx =
                            new ExceptionInfo(e.classname, e.getMessage());
                        fields = NO_FIELDS;
                    }

                    if (isRecord) {
                        canonicalCtr = canonicalRecordCtr(cl);
                        deserializationCtrs = new DeserializationConstructorsCache();
                    } else if (externalizable) {
                        cons = getExternalizableConstructor(cl);
                    } else {
                        cons = getSerializableConstructor(cl);
                        writeObjectMethod = getPrivateMethod(cl, "writeObject",
                            new Class<?>[] { ObjectOutputStream.class },
                            Void.TYPE);
                        readObjectMethod = getPrivateMethod(cl, "readObject",
                            new Class<?>[] { ObjectInputStream.class },
                            Void.TYPE);
                        readObjectNoDataMethod = getPrivateMethod(
                            cl, "readObjectNoData", null, Void.TYPE);
                        hasWriteObjectData = (writeObjectMethod != null);
                    }
                    domains = getProtectionDomains(cons, cl);
                    writeReplaceMethod = getInheritableMethod(
                        cl, "writeReplace", null, Object.class);
                    readResolveMethod = getInheritableMethod(
                        cl, "readResolve", null, Object.class);
                    return null;
                }
            });
        } else {
            suid = 0L;
            fields = NO_FIELDS;
        }

        try {
            fieldRefl = getReflector(fields, this);
        } catch (InvalidClassException ex) {
            // field mismatches impossible when matching local fields vs. self
            throw new InternalError(ex);
        }

        if (deserializeEx == null) {
            if (isEnum) {
                deserializeEx = new ExceptionInfo(name, "enum type");
            } else if (cons == null && !isRecord) {
                deserializeEx = new ExceptionInfo(name, "no valid constructor");
            }
        }
        if (isRecord && canonicalCtr == null) {
            deserializeEx = new ExceptionInfo(name, "record canonical constructor not found");
        } else {
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].getField() == null) {
                    defaultSerializeEx = new ExceptionInfo(
                        name, "unmatched serializable field(s) declared");
                }
            }
        }
        initialized = true;
    }

    /**
     * Creates blank class descriptor which should be initialized via a
     * subsequent call to initProxy(), initNonProxy() or readNonProxy().
     */
    ObjectStreamClass() {
    }

    /**
     * Creates a PermissionDomain that grants no permission.
     */
    private ProtectionDomain noPermissionsDomain() {
        PermissionCollection perms = new Permissions();
        perms.setReadOnly();
        return new ProtectionDomain(null, perms);
    }

    /**
     * Aggregate the ProtectionDomains of all the classes that separate
     * a concrete class {@code cl} from its ancestor's class declaring
     * a constructor {@code cons}.
     *
     * If {@code cl} is defined by the boot loader, or the constructor
     * {@code cons} is declared by {@code cl}, or if there is no security
     * manager, then this method does nothing and {@code null} is returned.
     *
     * @param cons A constructor declared by {@code cl} or one of its
     *             ancestors.
     * @param cl A concrete class, which is either the class declaring
     *           the constructor {@code cons}, or a serializable subclass
     *           of that class.
     * @return An array of ProtectionDomain representing the set of
     *         ProtectionDomain that separate the concrete class {@code cl}
     *         from its ancestor's declaring {@code cons}, or {@code null}.
     */
    @SuppressWarnings("removal")
    private ProtectionDomain[] getProtectionDomains(Constructor<?> cons,
                                                    Class<?> cl) {
        ProtectionDomain[] domains = null;
        if (cons != null && cl.getClassLoader() != null
                && System.getSecurityManager() != null) {
            Class<?> cls = cl;
            Class<?> fnscl = cons.getDeclaringClass();
            Set<ProtectionDomain> pds = null;
            while (cls != fnscl) {
                ProtectionDomain pd = cls.getProtectionDomain();
                if (pd != null) {
                    if (pds == null) pds = new HashSet<>();
                    pds.add(pd);
                }
                cls = cls.getSuperclass();
                if (cls == null) {
                    // that's not supposed to happen
                    // make a ProtectionDomain with no permission.
                    // should we throw instead?
                    if (pds == null) pds = new HashSet<>();
                    else pds.clear();
                    pds.add(noPermissionsDomain());
                    break;
                }
            }
            if (pds != null) {
                domains = pds.toArray(new ProtectionDomain[0]);
            }
        }
        return domains;
    }

    /**
     * Initializes class descriptor representing a proxy class.
     */
    void initProxy(Class<?> cl,
                   ClassNotFoundException resolveEx,
                   ObjectStreamClass superDesc)
        throws InvalidClassException
    {
        ObjectStreamClass osc = null;
        if (cl != null) {
            osc = lookup(cl, true);
            if (!osc.isProxy) {
                throw new InvalidClassException(
                    "cannot bind proxy descriptor to a non-proxy class");
            }
        }
        this.cl = cl;
        this.resolveEx = resolveEx;
        this.superDesc = superDesc;
        isProxy = true;
        serializable = true;
        suid = 0L;
        fields = NO_FIELDS;
        if (osc != null) {
            localDesc = osc;
            name = localDesc.name;
            externalizable = localDesc.externalizable;
            writeReplaceMethod = localDesc.writeReplaceMethod;
            readResolveMethod = localDesc.readResolveMethod;
            deserializeEx = localDesc.deserializeEx;
            domains = localDesc.domains;
            cons = localDesc.cons;
        }
        fieldRefl = getReflector(fields, localDesc);
        initialized = true;
    }

    /**
     * Initializes class descriptor representing a non-proxy class.
     */
    void initNonProxy(ObjectStreamClass model,
                      Class<?> cl,
                      ClassNotFoundException resolveEx,
                      ObjectStreamClass superDesc)
        throws InvalidClassException
    {
        long suid = model.getSerialVersionUID();
        ObjectStreamClass osc = null;
        if (cl != null) {
            osc = lookup(cl, true);
            if (osc.isProxy) {
                throw new InvalidClassException(
                        "cannot bind non-proxy descriptor to a proxy class");
            }
            if (model.isEnum != osc.isEnum) {
                throw new InvalidClassException(model.isEnum ?
                        "cannot bind enum descriptor to a non-enum class" :
                        "cannot bind non-enum descriptor to an enum class");
            }

            if (model.serializable == osc.serializable &&
                    !cl.isArray() && !cl.isRecord() &&
                    suid != osc.getSerialVersionUID()) {
                throw new InvalidClassException(osc.name,
                        "local class incompatible: " +
                                "stream classdesc serialVersionUID = " + suid +
                                ", local class serialVersionUID = " +
                                osc.getSerialVersionUID());
            }

            if (!classNamesEqual(model.name, osc.name)) {
                throw new InvalidClassException(osc.name,
                        "local class name incompatible with stream class " +
                                "name \"" + model.name + "\"");
            }

            if (!model.isEnum) {
                if ((model.serializable == osc.serializable) &&
                        (model.externalizable != osc.externalizable)) {
                    throw new InvalidClassException(osc.name,
                            "Serializable incompatible with Externalizable");
                }

                if ((model.serializable != osc.serializable) ||
                        (model.externalizable != osc.externalizable) ||
                        !(model.serializable || model.externalizable)) {
                    deserializeEx = new ExceptionInfo(
                            osc.name, "class invalid for deserialization");
                }
            }
        }

        this.cl = cl;
        this.resolveEx = resolveEx;
        this.superDesc = superDesc;
        name = model.name;
        this.suid = suid;
        isProxy = false;
        isEnum = model.isEnum;
        serializable = model.serializable;
        externalizable = model.externalizable;
        hasBlockExternalData = model.hasBlockExternalData;
        hasWriteObjectData = model.hasWriteObjectData;
        fields = model.fields;
        primDataSize = model.primDataSize;
        numObjFields = model.numObjFields;

        if (osc != null) {
            localDesc = osc;
            isRecord = localDesc.isRecord;
            // canonical record constructor is shared
            canonicalCtr = localDesc.canonicalCtr;
            // cache of deserialization constructors is shared
            deserializationCtrs = localDesc.deserializationCtrs;
            writeObjectMethod = localDesc.writeObjectMethod;
            readObjectMethod = localDesc.readObjectMethod;
            readObjectNoDataMethod = localDesc.readObjectNoDataMethod;
            writeReplaceMethod = localDesc.writeReplaceMethod;
            readResolveMethod = localDesc.readResolveMethod;
            if (deserializeEx == null) {
                deserializeEx = localDesc.deserializeEx;
            }
            domains = localDesc.domains;
            assert cl.isRecord() ? localDesc.cons == null : true;
            cons = localDesc.cons;
        }

        fieldRefl = getReflector(fields, localDesc);
        // reassign to matched fields so as to reflect local unshared settings
        fields = fieldRefl.getFields();

        initialized = true;
    }

    /**
     * Reads non-proxy class descriptor information from given input stream.
     * The resulting class descriptor is not fully functional; it can only be
     * used as input to the ObjectInputStream.resolveClass() and
     * ObjectStreamClass.initNonProxy() methods.
     */
    void readNonProxy(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        name = in.readUTF();
        suid = in.readLong();
        isProxy = false;

        byte flags = in.readByte();
        hasWriteObjectData =
            ((flags & ObjectStreamConstants.SC_WRITE_METHOD) != 0);
        hasBlockExternalData =
            ((flags & ObjectStreamConstants.SC_BLOCK_DATA) != 0);
        externalizable =
            ((flags & ObjectStreamConstants.SC_EXTERNALIZABLE) != 0);
        boolean sflag =
            ((flags & ObjectStreamConstants.SC_SERIALIZABLE) != 0);
        if (externalizable && sflag) {
            throw new InvalidClassException(
                name, "serializable and externalizable flags conflict");
        }
        serializable = externalizable || sflag;
        isEnum = ((flags & ObjectStreamConstants.SC_ENUM) != 0);
        if (isEnum && suid.longValue() != 0L) {
            throw new InvalidClassException(name,
                "enum descriptor has non-zero serialVersionUID: " + suid);
        }

        int numFields = in.readShort();
        if (isEnum && numFields != 0) {
            throw new InvalidClassException(name,
                "enum descriptor has non-zero field count: " + numFields);
        }
        fields = (numFields > 0) ?
            new ObjectStreamField[numFields] : NO_FIELDS;
        for (int i = 0; i < numFields; i++) {
            char tcode = (char) in.readByte();
            String fname = in.readUTF();
            String signature = ((tcode == 'L') || (tcode == '[')) ?
                in.readTypeString() : String.valueOf(tcode);
            try {
                fields[i] = new ObjectStreamField(fname, signature, false);
            } catch (RuntimeException e) {
                throw (IOException) new InvalidClassException(name,
                    "invalid descriptor for field " + fname).initCause(e);
            }
        }
        computeFieldOffsets();
    }

    /**
     * Writes non-proxy class descriptor information to given output stream.
     */
    void writeNonProxy(ObjectOutputStream out) throws IOException {
        out.writeUTF(name);
        out.writeLong(getSerialVersionUID());

        byte flags = 0;
        if (externalizable) {
            flags |= ObjectStreamConstants.SC_EXTERNALIZABLE;
            int protocol = out.getProtocolVersion();
            if (protocol != ObjectStreamConstants.PROTOCOL_VERSION_1) {
                flags |= ObjectStreamConstants.SC_BLOCK_DATA;
            }
        } else if (serializable) {
            flags |= ObjectStreamConstants.SC_SERIALIZABLE;
        }
        if (hasWriteObjectData) {
            flags |= ObjectStreamConstants.SC_WRITE_METHOD;
        }
        if (isEnum) {
            flags |= ObjectStreamConstants.SC_ENUM;
        }
        out.writeByte(flags);

        out.writeShort(fields.length);
        for (int i = 0; i < fields.length; i++) {
            ObjectStreamField f = fields[i];
            out.writeByte(f.getTypeCode());
            out.writeUTF(f.getName());
            if (!f.isPrimitive()) {
                out.writeTypeString(f.getTypeString());
            }
        }
    }

    /**
     * Returns ClassNotFoundException (if any) thrown while attempting to
     * resolve local class corresponding to this class descriptor.
     */
    ClassNotFoundException getResolveException() {
        return resolveEx;
    }

    /**
     * Throws InternalError if not initialized.
     */
    private final void requireInitialized() {
        if (!initialized)
            throw new InternalError("Unexpected call when not initialized");
    }

    /**
     * Throws InvalidClassException if not initialized.
     * To be called in cases where an uninitialized class descriptor indicates
     * a problem in the serialization stream.
     */
    final void checkInitialized() throws InvalidClassException {
        if (!initialized) {
            throw new InvalidClassException("Class descriptor should be initialized");
        }
    }

    /**
     * Throws an InvalidClassException if object instances referencing this
     * class descriptor should not be allowed to deserialize.  This method does
     * not apply to deserialization of enum constants.
     */
    void checkDeserialize() throws InvalidClassException {
        requireInitialized();
        if (deserializeEx != null) {
            throw deserializeEx.newInvalidClassException();
        }
    }

    /**
     * Throws an InvalidClassException if objects whose class is represented by
     * this descriptor should not be allowed to serialize.  This method does
     * not apply to serialization of enum constants.
     */
    void checkSerialize() throws InvalidClassException {
        requireInitialized();
        if (serializeEx != null) {
            throw serializeEx.newInvalidClassException();
        }
    }

    /**
     * Throws an InvalidClassException if objects whose class is represented by
     * this descriptor should not be permitted to use default serialization
     * (e.g., if the class declares serializable fields that do not correspond
     * to actual fields, and hence must use the GetField API).  This method
     * does not apply to deserialization of enum constants.
     */
    void checkDefaultSerialize() throws InvalidClassException {
        requireInitialized();
        if (defaultSerializeEx != null) {
            throw defaultSerializeEx.newInvalidClassException();
        }
    }

    /**
     * Returns superclass descriptor.  Note that on the receiving side, the
     * superclass descriptor may be bound to a class that is not a superclass
     * of the subclass descriptor's bound class.
     */
    ObjectStreamClass getSuperDesc() {
        requireInitialized();
        return superDesc;
    }

    /**
     * Returns the "local" class descriptor for the class associated with this
     * class descriptor (i.e., the result of
     * ObjectStreamClass.lookup(this.forClass())) or null if there is no class
     * associated with this descriptor.
     */
    ObjectStreamClass getLocalDesc() {
        requireInitialized();
        return localDesc;
    }

    /**
     * Returns arrays of ObjectStreamFields representing the serializable
     * fields of the represented class.  If copy is true, a clone of this class
     * descriptor's field array is returned, otherwise the array itself is
     * returned.
     */
    ObjectStreamField[] getFields(boolean copy) {
        return copy ? fields.clone() : fields;
    }

    /**
     * Looks up a serializable field of the represented class by name and type.
     * A specified type of null matches all types, Object.class matches all
     * non-primitive types, and any other non-null type matches assignable
     * types only.  Returns matching field, or null if no match found.
     */
    ObjectStreamField getField(String name, Class<?> type) {
        for (int i = 0; i < fields.length; i++) {
            ObjectStreamField f = fields[i];
            if (f.getName().equals(name)) {
                if (type == null ||
                    (type == Object.class && !f.isPrimitive()))
                {
                    return f;
                }
                Class<?> ftype = f.getType();
                if (ftype != null && type.isAssignableFrom(ftype)) {
                    return f;
                }
            }
        }
        return null;
    }

    /**
     * Returns true if class descriptor represents a dynamic proxy class, false
     * otherwise.
     */
    boolean isProxy() {
        requireInitialized();
        return isProxy;
    }

    /**
     * Returns true if class descriptor represents an enum type, false
     * otherwise.
     */
    boolean isEnum() {
        requireInitialized();
        return isEnum;
    }

    /**
     * Returns true if class descriptor represents a record type, false
     * otherwise.
     */
    boolean isRecord() {
        requireInitialized();
        return isRecord;
    }

    /**
     * Returns true if represented class implements Externalizable, false
     * otherwise.
     */
    boolean isExternalizable() {
        requireInitialized();
        return externalizable;
    }

    /**
     * Returns true if represented class implements Serializable, false
     * otherwise.
     */
    boolean isSerializable() {
        requireInitialized();
        return serializable;
    }

    /**
     * Returns true if class descriptor represents externalizable class that
     * has written its data in 1.2 (block data) format, false otherwise.
     */
    boolean hasBlockExternalData() {
        requireInitialized();
        return hasBlockExternalData;
    }

    /**
     * Returns true if class descriptor represents serializable (but not
     * externalizable) class which has written its data via a custom
     * writeObject() method, false otherwise.
     */
    boolean hasWriteObjectData() {
        requireInitialized();
        return hasWriteObjectData;
    }

    /**
     * Returns true if represented class is serializable/externalizable and can
     * be instantiated by the serialization runtime--i.e., if it is
     * externalizable and defines a public no-arg constructor, or if it is
     * non-externalizable and its first non-serializable superclass defines an
     * accessible no-arg constructor.  Otherwise, returns false.
     */
    boolean isInstantiable() {
        requireInitialized();
        return (cons != null);
    }

    /**
     * Returns true if represented class is serializable (but not
     * externalizable) and defines a conformant writeObject method.  Otherwise,
     * returns false.
     */
    boolean hasWriteObjectMethod() {
        requireInitialized();
        return (writeObjectMethod != null);
    }

    /**
     * Returns true if represented class is serializable (but not
     * externalizable) and defines a conformant readObject method.  Otherwise,
     * returns false.
     */
    boolean hasReadObjectMethod() {
        requireInitialized();
        return (readObjectMethod != null);
    }

    /**
     * Returns true if represented class is serializable (but not
     * externalizable) and defines a conformant readObjectNoData method.
     * Otherwise, returns false.
     */
    boolean hasReadObjectNoDataMethod() {
        requireInitialized();
        return (readObjectNoDataMethod != null);
    }

    /**
     * Returns true if represented class is serializable or externalizable and
     * defines a conformant writeReplace method.  Otherwise, returns false.
     */
    boolean hasWriteReplaceMethod() {
        requireInitialized();
        return (writeReplaceMethod != null);
    }

    /**
     * Returns true if represented class is serializable or externalizable and
     * defines a conformant readResolve method.  Otherwise, returns false.
     */
    boolean hasReadResolveMethod() {
        requireInitialized();
        return (readResolveMethod != null);
    }

    /**
     * Creates a new instance of the represented class.  If the class is
     * externalizable, invokes its public no-arg constructor; otherwise, if the
     * class is serializable, invokes the no-arg constructor of the first
     * non-serializable superclass.  Throws UnsupportedOperationException if
     * this class descriptor is not associated with a class, if the associated
     * class is non-serializable or if the appropriate no-arg constructor is
     * inaccessible/unavailable.
     */
    @SuppressWarnings("removal")
    Object newInstance()
        throws InstantiationException, InvocationTargetException,
               UnsupportedOperationException
    {
        requireInitialized();
        if (cons != null) {
            try {
                if (domains == null || domains.length == 0) {
                    return cons.newInstance();
                } else {
                    JavaSecurityAccess jsa = SharedSecrets.getJavaSecurityAccess();
                    PrivilegedAction<?> pea = () -> {
                        try {
                            return cons.newInstance();
                        } catch (InstantiationException
                                 | InvocationTargetException
                                 | IllegalAccessException x) {
                            throw new UndeclaredThrowableException(x);
                        }
                    }; // Can't use PrivilegedExceptionAction with jsa
                    try {
                        return jsa.doIntersectionPrivilege(pea,
                                   AccessController.getContext(),
                                   new AccessControlContext(domains));
                    } catch (UndeclaredThrowableException x) {
                        Throwable cause = x.getCause();
                        if (cause instanceof InstantiationException)
                            throw (InstantiationException) cause;
                        if (cause instanceof InvocationTargetException)
                            throw (InvocationTargetException) cause;
                        if (cause instanceof IllegalAccessException)
                            throw (IllegalAccessException) cause;
                        // not supposed to happen
                        throw x;
                    }
                }
            } catch (IllegalAccessException ex) {
                // should not occur, as access checks have been suppressed
                throw new InternalError(ex);
            } catch (InstantiationError err) {
                var ex = new InstantiationException();
                ex.initCause(err);
                throw ex;
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Invokes the writeObject method of the represented serializable class.
     * Throws UnsupportedOperationException if this class descriptor is not
     * associated with a class, or if the class is externalizable,
     * non-serializable or does not define writeObject.
     */
    void invokeWriteObject(Object obj, ObjectOutputStream out)
        throws IOException, UnsupportedOperationException
    {
        requireInitialized();
        if (writeObjectMethod != null) {
            try {
                writeObjectMethod.invoke(obj, new Object[]{ out });
            } catch (InvocationTargetException ex) {
                Throwable th = ex.getCause();
                if (th instanceof IOException) {
                    throw (IOException) th;
                } else {
                    throwMiscException(th);
                }
            } catch (IllegalAccessException ex) {
                // should not occur, as access checks have been suppressed
                throw new InternalError(ex);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Invokes the readObject method of the represented serializable class.
     * Throws UnsupportedOperationException if this class descriptor is not
     * associated with a class, or if the class is externalizable,
     * non-serializable or does not define readObject.
     */
    void invokeReadObject(Object obj, ObjectInputStream in)
        throws ClassNotFoundException, IOException,
               UnsupportedOperationException
    {
        requireInitialized();
        if (readObjectMethod != null) {
            try {
                readObjectMethod.invoke(obj, new Object[]{ in });
            } catch (InvocationTargetException ex) {
                Throwable th = ex.getCause();
                if (th instanceof ClassNotFoundException) {
                    throw (ClassNotFoundException) th;
                } else if (th instanceof IOException) {
                    throw (IOException) th;
                } else {
                    throwMiscException(th);
                }
            } catch (IllegalAccessException ex) {
                // should not occur, as access checks have been suppressed
                throw new InternalError(ex);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Invokes the readObjectNoData method of the represented serializable
     * class.  Throws UnsupportedOperationException if this class descriptor is
     * not associated with a class, or if the class is externalizable,
     * non-serializable or does not define readObjectNoData.
     */
    void invokeReadObjectNoData(Object obj)
        throws IOException, UnsupportedOperationException
    {
        requireInitialized();
        if (readObjectNoDataMethod != null) {
            try {
                readObjectNoDataMethod.invoke(obj, (Object[]) null);
            } catch (InvocationTargetException ex) {
                Throwable th = ex.getCause();
                if (th instanceof ObjectStreamException) {
                    throw (ObjectStreamException) th;
                } else {
                    throwMiscException(th);
                }
            } catch (IllegalAccessException ex) {
                // should not occur, as access checks have been suppressed
                throw new InternalError(ex);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Invokes the writeReplace method of the represented serializable class and
     * returns the result.  Throws UnsupportedOperationException if this class
     * descriptor is not associated with a class, or if the class is
     * non-serializable or does not define writeReplace.
     */
    Object invokeWriteReplace(Object obj)
        throws IOException, UnsupportedOperationException
    {
        requireInitialized();
        if (writeReplaceMethod != null) {
            try {
                return writeReplaceMethod.invoke(obj, (Object[]) null);
            } catch (InvocationTargetException ex) {
                Throwable th = ex.getCause();
                if (th instanceof ObjectStreamException) {
                    throw (ObjectStreamException) th;
                } else {
                    throwMiscException(th);
                    throw new InternalError(th);  // never reached
                }
            } catch (IllegalAccessException ex) {
                // should not occur, as access checks have been suppressed
                throw new InternalError(ex);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Invokes the readResolve method of the represented serializable class and
     * returns the result.  Throws UnsupportedOperationException if this class
     * descriptor is not associated with a class, or if the class is
     * non-serializable or does not define readResolve.
     */
    Object invokeReadResolve(Object obj)
        throws IOException, UnsupportedOperationException
    {
        requireInitialized();
        if (readResolveMethod != null) {
            try {
                return readResolveMethod.invoke(obj, (Object[]) null);
            } catch (InvocationTargetException ex) {
                Throwable th = ex.getCause();
                if (th instanceof ObjectStreamException) {
                    throw (ObjectStreamException) th;
                } else {
                    throwMiscException(th);
                    throw new InternalError(th);  // never reached
                }
            } catch (IllegalAccessException ex) {
                // should not occur, as access checks have been suppressed
                throw new InternalError(ex);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Class representing the portion of an object's serialized form allotted
     * to data described by a given class descriptor.  If "hasData" is false,
     * the object's serialized form does not contain data associated with the
     * class descriptor.
     */
    static class ClassDataSlot {

        /** class descriptor "occupying" this slot */
        final ObjectStreamClass desc;
        /** true if serialized form includes data for this slot's descriptor */
        final boolean hasData;

        ClassDataSlot(ObjectStreamClass desc, boolean hasData) {
            this.desc = desc;
            this.hasData = hasData;
        }
    }

    /**
     * Returns array of ClassDataSlot instances representing the data layout
     * (including superclass data) for serialized objects described by this
     * class descriptor.  ClassDataSlots are ordered by inheritance with those
     * containing "higher" superclasses appearing first.  The final
     * ClassDataSlot contains a reference to this descriptor.
     */
    ClassDataSlot[] getClassDataLayout() throws InvalidClassException {
        // REMIND: synchronize instead of relying on volatile?
        if (dataLayout == null) {
            dataLayout = getClassDataLayout0();
        }
        return dataLayout;
    }

    private ClassDataSlot[] getClassDataLayout0()
        throws InvalidClassException
    {
        ArrayList<ClassDataSlot> slots = new ArrayList<>();
        Class<?> start = cl, end = cl;

        // locate closest non-serializable superclass
        while (end != null && Serializable.class.isAssignableFrom(end)) {
            end = end.getSuperclass();
        }

        HashSet<String> oscNames = new HashSet<>(3);

        for (ObjectStreamClass d = this; d != null; d = d.superDesc) {
            if (oscNames.contains(d.name)) {
                throw new InvalidClassException("Circular reference.");
            } else {
                oscNames.add(d.name);
            }

            // search up inheritance hierarchy for class with matching name
            String searchName = (d.cl != null) ? d.cl.getName() : d.name;
            Class<?> match = null;
            for (Class<?> c = start; c != end; c = c.getSuperclass()) {
                if (searchName.equals(c.getName())) {
                    match = c;
                    break;
                }
            }

            // add "no data" slot for each unmatched class below match
            if (match != null) {
                for (Class<?> c = start; c != match; c = c.getSuperclass()) {
                    slots.add(new ClassDataSlot(
                        ObjectStreamClass.lookup(c, true), false));
                }
                start = match.getSuperclass();
            }

            // record descriptor/class pairing
            slots.add(new ClassDataSlot(d.getVariantFor(match), true));
        }

        // add "no data" slot for any leftover unmatched classes
        for (Class<?> c = start; c != end; c = c.getSuperclass()) {
            slots.add(new ClassDataSlot(
                ObjectStreamClass.lookup(c, true), false));
        }

        // order slots from superclass -> subclass
        Collections.reverse(slots);
        return slots.toArray(new ClassDataSlot[slots.size()]);
    }

    /**
     * Returns aggregate size (in bytes) of marshalled primitive field values
     * for represented class.
     */
    int getPrimDataSize() {
        return primDataSize;
    }

    /**
     * Returns number of non-primitive serializable fields of represented
     * class.
     */
    int getNumObjFields() {
        return numObjFields;
    }

    /**
     * Fetches the serializable primitive field values of object obj and
     * marshals them into byte array buf starting at offset 0.  It is the
     * responsibility of the caller to ensure that obj is of the proper type if
     * non-null.
     */
    void getPrimFieldValues(Object obj, byte[] buf) {
        fieldRefl.getPrimFieldValues(obj, buf);
    }

    /**
     * Sets the serializable primitive fields of object obj using values
     * unmarshalled from byte array buf starting at offset 0.  It is the
     * responsibility of the caller to ensure that obj is of the proper type if
     * non-null.
     */
    void setPrimFieldValues(Object obj, byte[] buf) {
        fieldRefl.setPrimFieldValues(obj, buf);
    }

    /**
     * Fetches the serializable object field values of object obj and stores
     * them in array vals starting at offset 0.  It is the responsibility of
     * the caller to ensure that obj is of the proper type if non-null.
     */
    void getObjFieldValues(Object obj, Object[] vals) {
        fieldRefl.getObjFieldValues(obj, vals);
    }

    /**
     * Checks that the given values, from array vals starting at offset 0,
     * are assignable to the given serializable object fields.
     * @throws ClassCastException if any value is not assignable
     */
    void checkObjFieldValueTypes(Object obj, Object[] vals) {
        fieldRefl.checkObjectFieldValueTypes(obj, vals);
    }

    /**
     * Sets the serializable object fields of object obj using values from
     * array vals starting at offset 0.  It is the responsibility of the caller
     * to ensure that obj is of the proper type if non-null.
     */
    void setObjFieldValues(Object obj, Object[] vals) {
        fieldRefl.setObjFieldValues(obj, vals);
    }

    /**
     * Calculates and sets serializable field offsets, as well as primitive
     * data size and object field count totals.  Throws InvalidClassException
     * if fields are illegally ordered.
     */
    private void computeFieldOffsets() throws InvalidClassException {
        primDataSize = 0;
        numObjFields = 0;
        int firstObjIndex = -1;

        for (int i = 0; i < fields.length; i++) {
            ObjectStreamField f = fields[i];
            switch (f.getTypeCode()) {
                case 'Z', 'B' -> f.setOffset(primDataSize++);
                case 'C', 'S' -> {
                    f.setOffset(primDataSize);
                    primDataSize += 2;
                }
                case 'I', 'F' -> {
                    f.setOffset(primDataSize);
                    primDataSize += 4;
                }
                case 'J', 'D' -> {
                    f.setOffset(primDataSize);
                    primDataSize += 8;
                }
                case '[', 'L' -> {
                    f.setOffset(numObjFields++);
                    if (firstObjIndex == -1) {
                        firstObjIndex = i;
                    }
                }
                default -> throw new InternalError();
            }
        }
        if (firstObjIndex != -1 &&
            firstObjIndex + numObjFields != fields.length)
        {
            throw new InvalidClassException(name, "illegal field order");
        }
    }

    /**
     * If given class is the same as the class associated with this class
     * descriptor, returns reference to this class descriptor.  Otherwise,
     * returns variant of this class descriptor bound to given class.
     */
    private ObjectStreamClass getVariantFor(Class<?> cl)
        throws InvalidClassException
    {
        if (this.cl == cl) {
            return this;
        }
        ObjectStreamClass desc = new ObjectStreamClass();
        if (isProxy) {
            desc.initProxy(cl, null, superDesc);
        } else {
            desc.initNonProxy(this, cl, null, superDesc);
        }
        return desc;
    }

    /**
     * Returns public no-arg constructor of given class, or null if none found.
     * Access checks are disabled on the returned constructor (if any), since
     * the defining class may still be non-public.
     */
    private static Constructor<?> getExternalizableConstructor(Class<?> cl) {
        try {
            Constructor<?> cons = cl.getDeclaredConstructor((Class<?>[]) null);
            cons.setAccessible(true);
            return ((cons.getModifiers() & Modifier.PUBLIC) != 0) ?
                cons : null;
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    /**
     * Returns subclass-accessible no-arg constructor of first non-serializable
     * superclass, or null if none found.  Access checks are disabled on the
     * returned constructor (if any).
     */
    private static Constructor<?> getSerializableConstructor(Class<?> cl) {
        return reflFactory.newConstructorForSerialization(cl);
    }

    /**
     * Returns the canonical constructor for the given record class, or null if
     * the not found ( which should never happen for correctly generated record
     * classes ).
     */
    @SuppressWarnings("removal")
    private static MethodHandle canonicalRecordCtr(Class<?> cls) {
        assert cls.isRecord() : "Expected record, got: " + cls;
        PrivilegedAction<MethodHandle> pa = () -> {
            Class<?>[] paramTypes = Arrays.stream(cls.getRecordComponents())
                                          .map(RecordComponent::getType)
                                          .toArray(Class<?>[]::new);
            try {
                Constructor<?> ctr = cls.getDeclaredConstructor(paramTypes);
                ctr.setAccessible(true);
                return MethodHandles.lookup().unreflectConstructor(ctr);
            } catch (IllegalAccessException | NoSuchMethodException e) {
                return null;
            }
        };
        return AccessController.doPrivileged(pa);
    }

    /**
     * Returns the canonical constructor, if the local class equivalent of this
     * stream class descriptor is a record class, otherwise null.
     */
    MethodHandle getRecordConstructor() {
        return canonicalCtr;
    }

    /**
     * Returns non-static, non-abstract method with given signature provided it
     * is defined by or accessible (via inheritance) by the given class, or
     * null if no match found.  Access checks are disabled on the returned
     * method (if any).
     */
    private static Method getInheritableMethod(Class<?> cl, String name,
                                               Class<?>[] argTypes,
                                               Class<?> returnType)
    {
        Method meth = null;
        Class<?> defCl = cl;
        while (defCl != null) {
            try {
                meth = defCl.getDeclaredMethod(name, argTypes);
                break;
            } catch (NoSuchMethodException ex) {
                defCl = defCl.getSuperclass();
            }
        }

        if ((meth == null) || (meth.getReturnType() != returnType)) {
            return null;
        }
        meth.setAccessible(true);
        int mods = meth.getModifiers();
        if ((mods & (Modifier.STATIC | Modifier.ABSTRACT)) != 0) {
            return null;
        } else if ((mods & (Modifier.PUBLIC | Modifier.PROTECTED)) != 0) {
            return meth;
        } else if ((mods & Modifier.PRIVATE) != 0) {
            return (cl == defCl) ? meth : null;
        } else {
            return packageEquals(cl, defCl) ? meth : null;
        }
    }

    /**
     * Returns non-static private method with given signature defined by given
     * class, or null if none found.  Access checks are disabled on the
     * returned method (if any).
     */
    private static Method getPrivateMethod(Class<?> cl, String name,
                                           Class<?>[] argTypes,
                                           Class<?> returnType)
    {
        try {
            Method meth = cl.getDeclaredMethod(name, argTypes);
            meth.setAccessible(true);
            int mods = meth.getModifiers();
            return ((meth.getReturnType() == returnType) &&
                    ((mods & Modifier.STATIC) == 0) &&
                    ((mods & Modifier.PRIVATE) != 0)) ? meth : null;
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    /**
     * Returns true if classes are defined in the same runtime package, false
     * otherwise.
     */
    private static boolean packageEquals(Class<?> cl1, Class<?> cl2) {
        return cl1.getClassLoader() == cl2.getClassLoader() &&
                cl1.getPackageName() == cl2.getPackageName();
    }

    /**
     * Compares class names for equality, ignoring package names.  Returns true
     * if class names equal, false otherwise.
     */
    private static boolean classNamesEqual(String name1, String name2) {
        int idx1 = name1.lastIndexOf('.') + 1;
        int idx2 = name2.lastIndexOf('.') + 1;
        int len1 = name1.length() - idx1;
        int len2 = name2.length() - idx2;
        return len1 == len2 &&
                name1.regionMatches(idx1, name2, idx2, len1);
    }

    /**
     * Returns JVM type signature for given list of parameters and return type.
     */
    private static String getMethodSignature(Class<?>[] paramTypes,
                                             Class<?> retType)
    {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (int i = 0; i < paramTypes.length; i++) {
            appendClassSignature(sb, paramTypes[i]);
        }
        sb.append(')');
        appendClassSignature(sb, retType);
        return sb.toString();
    }

    /**
     * Convenience method for throwing an exception that is either a
     * RuntimeException, Error, or of some unexpected type (in which case it is
     * wrapped inside an IOException).
     */
    private static void throwMiscException(Throwable th) throws IOException {
        if (th instanceof RuntimeException) {
            throw (RuntimeException) th;
        } else if (th instanceof Error) {
            throw (Error) th;
        } else {
            IOException ex = new IOException("unexpected exception type");
            ex.initCause(th);
            throw ex;
        }
    }

    /**
     * Returns ObjectStreamField array describing the serializable fields of
     * the given class.  Serializable fields backed by an actual field of the
     * class are represented by ObjectStreamFields with corresponding non-null
     * Field objects.  Throws InvalidClassException if the (explicitly
     * declared) serializable fields are invalid.
     */
    private static ObjectStreamField[] getSerialFields(Class<?> cl)
        throws InvalidClassException
    {
        if (!Serializable.class.isAssignableFrom(cl))
            return NO_FIELDS;

        ObjectStreamField[] fields;
        if (cl.isRecord()) {
            fields = getDefaultSerialFields(cl);
            Arrays.sort(fields);
        } else if (!Externalizable.class.isAssignableFrom(cl) &&
            !Proxy.isProxyClass(cl) &&
                   !cl.isInterface()) {
            if ((fields = getDeclaredSerialFields(cl)) == null) {
                fields = getDefaultSerialFields(cl);
            }
            Arrays.sort(fields);
        } else {
            fields = NO_FIELDS;
        }
        return fields;
    }

    /**
     * Returns serializable fields of given class as defined explicitly by a
     * "serialPersistentFields" field, or null if no appropriate
     * "serialPersistentFields" field is defined.  Serializable fields backed
     * by an actual field of the class are represented by ObjectStreamFields
     * with corresponding non-null Field objects.  For compatibility with past
     * releases, a "serialPersistentFields" field with a null value is
     * considered equivalent to not declaring "serialPersistentFields".  Throws
     * InvalidClassException if the declared serializable fields are
     * invalid--e.g., if multiple fields share the same name.
     */
    private static ObjectStreamField[] getDeclaredSerialFields(Class<?> cl)
        throws InvalidClassException
    {
        ObjectStreamField[] serialPersistentFields = null;
        try {
            Field f = cl.getDeclaredField("serialPersistentFields");
            int mask = Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL;
            if ((f.getModifiers() & mask) == mask) {
                f.setAccessible(true);
                serialPersistentFields = (ObjectStreamField[]) f.get(null);
            }
        } catch (Exception ex) {
        }
        if (serialPersistentFields == null) {
            return null;
        } else if (serialPersistentFields.length == 0) {
            return NO_FIELDS;
        }

        ObjectStreamField[] boundFields =
            new ObjectStreamField[serialPersistentFields.length];
        Set<String> fieldNames = new HashSet<>(serialPersistentFields.length);

        for (int i = 0; i < serialPersistentFields.length; i++) {
            ObjectStreamField spf = serialPersistentFields[i];

            String fname = spf.getName();
            if (fieldNames.contains(fname)) {
                throw new InvalidClassException(
                    "multiple serializable fields named " + fname);
            }
            fieldNames.add(fname);

            try {
                Field f = cl.getDeclaredField(fname);
                if ((f.getType() == spf.getType()) &&
                    ((f.getModifiers() & Modifier.STATIC) == 0))
                {
                    boundFields[i] =
                        new ObjectStreamField(f, spf.isUnshared(), true);
                }
            } catch (NoSuchFieldException ex) {
            }
            if (boundFields[i] == null) {
                boundFields[i] = new ObjectStreamField(
                    fname, spf.getType(), spf.isUnshared());
            }
        }
        return boundFields;
    }

    /**
     * Returns array of ObjectStreamFields corresponding to all non-static
     * non-transient fields declared by given class.  Each ObjectStreamField
     * contains a Field object for the field it represents.  If no default
     * serializable fields exist, NO_FIELDS is returned.
     */
    private static ObjectStreamField[] getDefaultSerialFields(Class<?> cl) {
        Field[] clFields = cl.getDeclaredFields();
        ArrayList<ObjectStreamField> list = new ArrayList<>();
        int mask = Modifier.STATIC | Modifier.TRANSIENT;

        for (int i = 0; i < clFields.length; i++) {
            if ((clFields[i].getModifiers() & mask) == 0) {
                list.add(new ObjectStreamField(clFields[i], false, true));
            }
        }
        int size = list.size();
        return (size == 0) ? NO_FIELDS :
            list.toArray(new ObjectStreamField[size]);
    }

    /**
     * Returns explicit serial version UID value declared by given class, or
     * null if none.
     */
    private static Long getDeclaredSUID(Class<?> cl) {
        try {
            Field f = cl.getDeclaredField("serialVersionUID");
            int mask = Modifier.STATIC | Modifier.FINAL;
            if ((f.getModifiers() & mask) == mask) {
                f.setAccessible(true);
                return f.getLong(null);
            }
        } catch (Exception ex) {
        }
        return null;
    }

    /**
     * Computes the default serial version UID value for the given class.
     */
    private static long computeDefaultSUID(Class<?> cl) {
        if (!Serializable.class.isAssignableFrom(cl) || Proxy.isProxyClass(cl))
        {
            return 0L;
        }

        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream dout = new DataOutputStream(bout);

            dout.writeUTF(cl.getName());

            int classMods = cl.getModifiers() &
                (Modifier.PUBLIC | Modifier.FINAL |
                 Modifier.INTERFACE | Modifier.ABSTRACT);

            /*
             * compensate for javac bug in which ABSTRACT bit was set for an
             * interface only if the interface declared methods
             */
            Method[] methods = cl.getDeclaredMethods();
            if ((classMods & Modifier.INTERFACE) != 0) {
                classMods = (methods.length > 0) ?
                    (classMods | Modifier.ABSTRACT) :
                    (classMods & ~Modifier.ABSTRACT);
            }
            dout.writeInt(classMods);

            if (!cl.isArray()) {
                /*
                 * compensate for change in 1.2FCS in which
                 * Class.getInterfaces() was modified to return Cloneable and
                 * Serializable for array classes.
                 */
                Class<?>[] interfaces = cl.getInterfaces();
                String[] ifaceNames = new String[interfaces.length];
                for (int i = 0; i < interfaces.length; i++) {
                    ifaceNames[i] = interfaces[i].getName();
                }
                Arrays.sort(ifaceNames);
                for (int i = 0; i < ifaceNames.length; i++) {
                    dout.writeUTF(ifaceNames[i]);
                }
            }

            Field[] fields = cl.getDeclaredFields();
            MemberSignature[] fieldSigs = new MemberSignature[fields.length];
            for (int i = 0; i < fields.length; i++) {
                fieldSigs[i] = new MemberSignature(fields[i]);
            }
            Arrays.sort(fieldSigs, new Comparator<>() {
                public int compare(MemberSignature ms1, MemberSignature ms2) {
                    return ms1.name.compareTo(ms2.name);
                }
            });
            for (int i = 0; i < fieldSigs.length; i++) {
                MemberSignature sig = fieldSigs[i];
                int mods = sig.member.getModifiers() &
                    (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED |
                     Modifier.STATIC | Modifier.FINAL | Modifier.VOLATILE |
                     Modifier.TRANSIENT);
                if (((mods & Modifier.PRIVATE) == 0) ||
                    ((mods & (Modifier.STATIC | Modifier.TRANSIENT)) == 0))
                {
                    dout.writeUTF(sig.name);
                    dout.writeInt(mods);
                    dout.writeUTF(sig.signature);
                }
            }

            if (hasStaticInitializer(cl)) {
                dout.writeUTF("<clinit>");
                dout.writeInt(Modifier.STATIC);
                dout.writeUTF("()V");
            }

            Constructor<?>[] cons = cl.getDeclaredConstructors();
            MemberSignature[] consSigs = new MemberSignature[cons.length];
            for (int i = 0; i < cons.length; i++) {
                consSigs[i] = new MemberSignature(cons[i]);
            }
            Arrays.sort(consSigs, new Comparator<>() {
                public int compare(MemberSignature ms1, MemberSignature ms2) {
                    return ms1.signature.compareTo(ms2.signature);
                }
            });
            for (int i = 0; i < consSigs.length; i++) {
                MemberSignature sig = consSigs[i];
                int mods = sig.member.getModifiers() &
                    (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED |
                     Modifier.STATIC | Modifier.FINAL |
                     Modifier.SYNCHRONIZED | Modifier.NATIVE |
                     Modifier.ABSTRACT | Modifier.STRICT);
                if ((mods & Modifier.PRIVATE) == 0) {
                    dout.writeUTF("<init>");
                    dout.writeInt(mods);
                    dout.writeUTF(sig.signature.replace('/', '.'));
                }
            }

            MemberSignature[] methSigs = new MemberSignature[methods.length];
            for (int i = 0; i < methods.length; i++) {
                methSigs[i] = new MemberSignature(methods[i]);
            }
            Arrays.sort(methSigs, new Comparator<>() {
                public int compare(MemberSignature ms1, MemberSignature ms2) {
                    int comp = ms1.name.compareTo(ms2.name);
                    if (comp == 0) {
                        comp = ms1.signature.compareTo(ms2.signature);
                    }
                    return comp;
                }
            });
            for (int i = 0; i < methSigs.length; i++) {
                MemberSignature sig = methSigs[i];
                int mods = sig.member.getModifiers() &
                    (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED |
                     Modifier.STATIC | Modifier.FINAL |
                     Modifier.SYNCHRONIZED | Modifier.NATIVE |
                     Modifier.ABSTRACT | Modifier.STRICT);
                if ((mods & Modifier.PRIVATE) == 0) {
                    dout.writeUTF(sig.name);
                    dout.writeInt(mods);
                    dout.writeUTF(sig.signature.replace('/', '.'));
                }
            }

            dout.flush();

            MessageDigest md = MessageDigest.getInstance("SHA");
            byte[] hashBytes = md.digest(bout.toByteArray());
            long hash = 0;
            for (int i = Math.min(hashBytes.length, 8) - 1; i >= 0; i--) {
                hash = (hash << 8) | (hashBytes[i] & 0xFF);
            }
            return hash;
        } catch (IOException ex) {
            throw new InternalError(ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new SecurityException(ex.getMessage());
        }
    }

    /**
     * Returns true if the given class defines a static initializer method,
     * false otherwise.
     */
    private static native boolean hasStaticInitializer(Class<?> cl);

    /**
     * Class for computing and caching field/constructor/method signatures
     * during serialVersionUID calculation.
     */
    private static class MemberSignature {

        public final Member member;
        public final String name;
        public final String signature;

        public MemberSignature(Field field) {
            member = field;
            name = field.getName();
            signature = getClassSignature(field.getType());
        }

        public MemberSignature(Constructor<?> cons) {
            member = cons;
            name = cons.getName();
            signature = getMethodSignature(
                cons.getParameterTypes(), Void.TYPE);
        }

        public MemberSignature(Method meth) {
            member = meth;
            name = meth.getName();
            signature = getMethodSignature(
                meth.getParameterTypes(), meth.getReturnType());
        }
    }

    /**
     * Class for setting and retrieving serializable field values in batch.
     */
    // REMIND: dynamically generate these?
    private static class FieldReflector {

        /** handle for performing unsafe operations */
        private static final Unsafe unsafe = Unsafe.getUnsafe();

        /** fields to operate on */
        private final ObjectStreamField[] fields;
        /** number of primitive fields */
        private final int numPrimFields;
        /** unsafe field keys for reading fields - may contain dupes */
        private final long[] readKeys;
        /** unsafe fields keys for writing fields - no dupes */
        private final long[] writeKeys;
        /** field data offsets */
        private final int[] offsets;
        /** field type codes */
        private final char[] typeCodes;
        /** field types */
        private final Class<?>[] types;

        /**
         * Constructs FieldReflector capable of setting/getting values from the
         * subset of fields whose ObjectStreamFields contain non-null
         * reflective Field objects.  ObjectStreamFields with null Fields are
         * treated as filler, for which get operations return default values
         * and set operations discard given values.
         */
        FieldReflector(ObjectStreamField[] fields) {
            this.fields = fields;
            int nfields = fields.length;
            readKeys = new long[nfields];
            writeKeys = new long[nfields];
            offsets = new int[nfields];
            typeCodes = new char[nfields];
            ArrayList<Class<?>> typeList = new ArrayList<>();
            Set<Long> usedKeys = new HashSet<>();


            for (int i = 0; i < nfields; i++) {
                ObjectStreamField f = fields[i];
                Field rf = f.getField();
                long key = (rf != null) ?
                    unsafe.objectFieldOffset(rf) : Unsafe.INVALID_FIELD_OFFSET;
                readKeys[i] = key;
                writeKeys[i] = usedKeys.add(key) ?
                    key : Unsafe.INVALID_FIELD_OFFSET;
                offsets[i] = f.getOffset();
                typeCodes[i] = f.getTypeCode();
                if (!f.isPrimitive()) {
                    typeList.add((rf != null) ? rf.getType() : null);
                }
            }

            types = typeList.toArray(new Class<?>[typeList.size()]);
            numPrimFields = nfields - types.length;
        }

        /**
         * Returns list of ObjectStreamFields representing fields operated on
         * by this reflector.  The shared/unshared values and Field objects
         * contained by ObjectStreamFields in the list reflect their bindings
         * to locally defined serializable fields.
         */
        ObjectStreamField[] getFields() {
            return fields;
        }

        /**
         * Fetches the serializable primitive field values of object obj and
         * marshals them into byte array buf starting at offset 0.  The caller
         * is responsible for ensuring that obj is of the proper type.
         */
        void getPrimFieldValues(Object obj, byte[] buf) {
            if (obj == null) {
                throw new NullPointerException();
            }
            /* assuming checkDefaultSerialize() has been called on the class
             * descriptor this FieldReflector was obtained from, no field keys
             * in array should be equal to Unsafe.INVALID_FIELD_OFFSET.
             */
            for (int i = 0; i < numPrimFields; i++) {
                long key = readKeys[i];
                int off = offsets[i];
                switch (typeCodes[i]) {
                    case 'Z' -> Bits.putBoolean(buf, off, unsafe.getBoolean(obj, key));
                    case 'B' -> buf[off] = unsafe.getByte(obj, key);
                    case 'C' -> Bits.putChar(buf, off, unsafe.getChar(obj, key));
                    case 'S' -> Bits.putShort(buf, off, unsafe.getShort(obj, key));
                    case 'I' -> Bits.putInt(buf, off, unsafe.getInt(obj, key));
                    case 'F' -> Bits.putFloat(buf, off, unsafe.getFloat(obj, key));
                    case 'J' -> Bits.putLong(buf, off, unsafe.getLong(obj, key));
                    case 'D' -> Bits.putDouble(buf, off, unsafe.getDouble(obj, key));
                    default  -> throw new InternalError();
                }
            }
        }

        /**
         * Sets the serializable primitive fields of object obj using values
         * unmarshalled from byte array buf starting at offset 0.  The caller
         * is responsible for ensuring that obj is of the proper type.
         */
        void setPrimFieldValues(Object obj, byte[] buf) {
            if (obj == null) {
                throw new NullPointerException();
            }
            for (int i = 0; i < numPrimFields; i++) {
                long key = writeKeys[i];
                if (key == Unsafe.INVALID_FIELD_OFFSET) {
                    continue;           // discard value
                }
                int off = offsets[i];
                switch (typeCodes[i]) {
                    case 'Z' -> unsafe.putBoolean(obj, key, Bits.getBoolean(buf, off));
                    case 'B' -> unsafe.putByte(obj, key, buf[off]);
                    case 'C' -> unsafe.putChar(obj, key, Bits.getChar(buf, off));
                    case 'S' -> unsafe.putShort(obj, key, Bits.getShort(buf, off));
                    case 'I' -> unsafe.putInt(obj, key, Bits.getInt(buf, off));
                    case 'F' -> unsafe.putFloat(obj, key, Bits.getFloat(buf, off));
                    case 'J' -> unsafe.putLong(obj, key, Bits.getLong(buf, off));
                    case 'D' -> unsafe.putDouble(obj, key, Bits.getDouble(buf, off));
                    default  -> throw new InternalError();
                }
            }
        }

        /**
         * Fetches the serializable object field values of object obj and
         * stores them in array vals starting at offset 0.  The caller is
         * responsible for ensuring that obj is of the proper type.
         */
        void getObjFieldValues(Object obj, Object[] vals) {
            if (obj == null) {
                throw new NullPointerException();
            }
            /* assuming checkDefaultSerialize() has been called on the class
             * descriptor this FieldReflector was obtained from, no field keys
             * in array should be equal to Unsafe.INVALID_FIELD_OFFSET.
             */
            for (int i = numPrimFields; i < fields.length; i++) {
                vals[offsets[i]] = switch (typeCodes[i]) {
                    case 'L', '[' -> unsafe.getReference(obj, readKeys[i]);
                    default       -> throw new InternalError();
                };
            }
        }

        /**
         * Checks that the given values, from array vals starting at offset 0,
         * are assignable to the given serializable object fields.
         * @throws ClassCastException if any value is not assignable
         */
        void checkObjectFieldValueTypes(Object obj, Object[] vals) {
            setObjFieldValues(obj, vals, true);
        }

        /**
         * Sets the serializable object fields of object obj using values from
         * array vals starting at offset 0.  The caller is responsible for
         * ensuring that obj is of the proper type; however, attempts to set a
         * field with a value of the wrong type will trigger an appropriate
         * ClassCastException.
         */
        void setObjFieldValues(Object obj, Object[] vals) {
            setObjFieldValues(obj, vals, false);
        }

        private void setObjFieldValues(Object obj, Object[] vals, boolean dryRun) {
            if (obj == null) {
                throw new NullPointerException();
            }
            for (int i = numPrimFields; i < fields.length; i++) {
                long key = writeKeys[i];
                if (key == Unsafe.INVALID_FIELD_OFFSET) {
                    continue;           // discard value
                }
                switch (typeCodes[i]) {
                    case 'L', '[' -> {
                        Object val = vals[offsets[i]];
                        if (val != null &&
                            !types[i - numPrimFields].isInstance(val))
                        {
                            Field f = fields[i].getField();
                            throw new ClassCastException(
                                "cannot assign instance of " +
                                val.getClass().getName() + " to field " +
                                f.getDeclaringClass().getName() + "." +
                                f.getName() + " of type " +
                                f.getType().getName() + " in instance of " +
                                obj.getClass().getName());
                        }
                        if (!dryRun)
                            unsafe.putReference(obj, key, val);
                    }
                    default -> throw new InternalError();
                }
            }
        }
    }

    /**
     * Matches given set of serializable fields with serializable fields
     * described by the given local class descriptor, and returns a
     * FieldReflector instance capable of setting/getting values from the
     * subset of fields that match (non-matching fields are treated as filler,
     * for which get operations return default values and set operations
     * discard given values).  Throws InvalidClassException if unresolvable
     * type conflicts exist between the two sets of fields.
     */
    private static FieldReflector getReflector(ObjectStreamField[] fields,
                                               ObjectStreamClass localDesc)
        throws InvalidClassException
    {
        // class irrelevant if no fields
        Class<?> cl = (localDesc != null && fields.length > 0) ?
            localDesc.cl : null;
        processQueue(Caches.reflectorsQueue, Caches.reflectors);
        FieldReflectorKey key = new FieldReflectorKey(cl, fields,
                                                      Caches.reflectorsQueue);
        Reference<?> ref = Caches.reflectors.get(key);
        Object entry = null;
        if (ref != null) {
            entry = ref.get();
        }
        EntryFuture future = null;
        if (entry == null) {
            EntryFuture newEntry = new EntryFuture();
            Reference<?> newRef = new SoftReference<>(newEntry);
            do {
                if (ref != null) {
                    Caches.reflectors.remove(key, ref);
                }
                ref = Caches.reflectors.putIfAbsent(key, newRef);
                if (ref != null) {
                    entry = ref.get();
                }
            } while (ref != null && entry == null);
            if (entry == null) {
                future = newEntry;
            }
        }

        if (entry instanceof FieldReflector) {  // check common case first
            return (FieldReflector) entry;
        } else if (entry instanceof EntryFuture) {
            entry = ((EntryFuture) entry).get();
        } else if (entry == null) {
            try {
                entry = new FieldReflector(matchFields(fields, localDesc));
            } catch (Throwable th) {
                entry = th;
            }
            future.set(entry);
            Caches.reflectors.put(key, new SoftReference<>(entry));
        }

        if (entry instanceof FieldReflector) {
            return (FieldReflector) entry;
        } else if (entry instanceof InvalidClassException) {
            throw (InvalidClassException) entry;
        } else if (entry instanceof RuntimeException) {
            throw (RuntimeException) entry;
        } else if (entry instanceof Error) {
            throw (Error) entry;
        } else {
            throw new InternalError("unexpected entry: " + entry);
        }
    }

    /**
     * FieldReflector cache lookup key.  Keys are considered equal if they
     * refer to the same class and equivalent field formats.
     */
    private static class FieldReflectorKey extends WeakReference<Class<?>> {

        private final String[] sigs;
        private final int hash;
        private final boolean nullClass;

        FieldReflectorKey(Class<?> cl, ObjectStreamField[] fields,
                          ReferenceQueue<Class<?>> queue)
        {
            super(cl, queue);
            nullClass = (cl == null);
            sigs = new String[2 * fields.length];
            for (int i = 0, j = 0; i < fields.length; i++) {
                ObjectStreamField f = fields[i];
                sigs[j++] = f.getName();
                sigs[j++] = f.getSignature();
            }
            hash = System.identityHashCode(cl) + Arrays.hashCode(sigs);
        }

        public int hashCode() {
            return hash;
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (obj instanceof FieldReflectorKey other) {
                Class<?> referent;
                return (nullClass ? other.nullClass
                                  : ((referent = get()) != null) &&
                                    (other.refersTo(referent))) &&
                        Arrays.equals(sigs, other.sigs);
            } else {
                return false;
            }
        }
    }

    /**
     * Matches given set of serializable fields with serializable fields
     * obtained from the given local class descriptor (which contain bindings
     * to reflective Field objects).  Returns list of ObjectStreamFields in
     * which each ObjectStreamField whose signature matches that of a local
     * field contains a Field object for that field; unmatched
     * ObjectStreamFields contain null Field objects.  Shared/unshared settings
     * of the returned ObjectStreamFields also reflect those of matched local
     * ObjectStreamFields.  Throws InvalidClassException if unresolvable type
     * conflicts exist between the two sets of fields.
     */
    private static ObjectStreamField[] matchFields(ObjectStreamField[] fields,
                                                   ObjectStreamClass localDesc)
        throws InvalidClassException
    {
        ObjectStreamField[] localFields = (localDesc != null) ?
            localDesc.fields : NO_FIELDS;

        /*
         * Even if fields == localFields, we cannot simply return localFields
         * here.  In previous implementations of serialization,
         * ObjectStreamField.getType() returned Object.class if the
         * ObjectStreamField represented a non-primitive field and belonged to
         * a non-local class descriptor.  To preserve this (questionable)
         * behavior, the ObjectStreamField instances returned by matchFields
         * cannot report non-primitive types other than Object.class; hence
         * localFields cannot be returned directly.
         */

        ObjectStreamField[] matches = new ObjectStreamField[fields.length];
        for (int i = 0; i < fields.length; i++) {
            ObjectStreamField f = fields[i], m = null;
            for (int j = 0; j < localFields.length; j++) {
                ObjectStreamField lf = localFields[j];
                if (f.getName().equals(lf.getName())) {
                    if ((f.isPrimitive() || lf.isPrimitive()) &&
                        f.getTypeCode() != lf.getTypeCode())
                    {
                        throw new InvalidClassException(localDesc.name,
                            "incompatible types for field " + f.getName());
                    }
                    if (lf.getField() != null) {
                        m = new ObjectStreamField(
                            lf.getField(), lf.isUnshared(), false);
                    } else {
                        m = new ObjectStreamField(
                            lf.getName(), lf.getSignature(), lf.isUnshared());
                    }
                }
            }
            if (m == null) {
                m = new ObjectStreamField(
                    f.getName(), f.getSignature(), false);
            }
            m.setOffset(f.getOffset());
            matches[i] = m;
        }
        return matches;
    }

    /**
     * Removes from the specified map any keys that have been enqueued
     * on the specified reference queue.
     */
    static void processQueue(ReferenceQueue<Class<?>> queue,
                             ConcurrentMap<? extends
                             WeakReference<Class<?>>, ?> map)
    {
        Reference<? extends Class<?>> ref;
        while((ref = queue.poll()) != null) {
            map.remove(ref);
        }
    }

    /**
     *  Weak key for Class objects.
     *
     **/
    static class WeakClassKey extends WeakReference<Class<?>> {
        /**
         * saved value of the referent's identity hash code, to maintain
         * a consistent hash code after the referent has been cleared
         */
        private final int hash;

        /**
         * Create a new WeakClassKey to the given object, registered
         * with a queue.
         */
        WeakClassKey(Class<?> cl, ReferenceQueue<Class<?>> refQueue) {
            super(cl, refQueue);
            hash = System.identityHashCode(cl);
        }

        /**
         * Returns the identity hash code of the original referent.
         */
        public int hashCode() {
            return hash;
        }

        /**
         * Returns true if the given object is this identical
         * WeakClassKey instance, or, if this object's referent has not
         * been cleared, if the given object is another WeakClassKey
         * instance with the identical non-null referent as this one.
         */
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (obj instanceof WeakClassKey) {
                Class<?> referent = get();
                return (referent != null) &&
                        (((WeakClassKey) obj).refersTo(referent));
            } else {
                return false;
            }
        }
    }

    /**
     * A LRA cache of record deserialization constructors.
     */
    @SuppressWarnings("serial")
    private static final class DeserializationConstructorsCache
        extends ConcurrentHashMap<DeserializationConstructorsCache.Key, MethodHandle>  {

        // keep max. 10 cached entries - when the 11th element is inserted the oldest
        // is removed and 10 remains - 11 is the biggest map size where internal
        // table of 16 elements is sufficient (inserting 12th element would resize it to 32)
        private static final int MAX_SIZE = 10;
        private Key.Impl first, last; // first and last in FIFO queue

        DeserializationConstructorsCache() {
            // start small - if there is more than one shape of ObjectStreamClass
            // deserialized, there will typically be two (current version and previous version)
            super(2);
        }

        MethodHandle get(ObjectStreamField[] fields) {
            return get(new Key.Lookup(fields));
        }

        synchronized MethodHandle putIfAbsentAndGet(ObjectStreamField[] fields, MethodHandle mh) {
            Key.Impl key = new Key.Impl(fields);
            var oldMh = putIfAbsent(key, mh);
            if (oldMh != null) return oldMh;
            // else we did insert new entry -> link the new key as last
            if (last == null) {
                last = first = key;
            } else {
                last = (last.next = key);
            }
            // may need to remove first
            if (size() > MAX_SIZE) {
                assert first != null;
                remove(first);
                first = first.next;
                if (first == null) {
                    last = null;
                }
            }
            return mh;
        }

        // a key composed of ObjectStreamField[] names and types
        static abstract class Key {
            abstract int length();
            abstract String fieldName(int i);
            abstract Class<?> fieldType(int i);

            @Override
            public final int hashCode() {
                int n = length();
                int h = 0;
                for (int i = 0; i < n; i++) h = h * 31 + fieldType(i).hashCode();
                for (int i = 0; i < n; i++) h = h * 31 + fieldName(i).hashCode();
                return h;
            }

            @Override
            public final boolean equals(Object obj) {
                if (!(obj instanceof Key other)) return false;
                int n = length();
                if (n != other.length()) return false;
                for (int i = 0; i < n; i++) if (fieldType(i) != other.fieldType(i)) return false;
                for (int i = 0; i < n; i++) if (!fieldName(i).equals(other.fieldName(i))) return false;
                return true;
            }

            // lookup key - just wraps ObjectStreamField[]
            static final class Lookup extends Key {
                final ObjectStreamField[] fields;

                Lookup(ObjectStreamField[] fields) { this.fields = fields; }

                @Override
                int length() { return fields.length; }

                @Override
                String fieldName(int i) { return fields[i].getName(); }

                @Override
                Class<?> fieldType(int i) { return fields[i].getType(); }
            }

            // real key - copies field names and types and forms FIFO queue in cache
            static final class Impl extends Key {
                Impl next;
                final String[] fieldNames;
                final Class<?>[] fieldTypes;

                Impl(ObjectStreamField[] fields) {
                    this.fieldNames = new String[fields.length];
                    this.fieldTypes = new Class<?>[fields.length];
                    for (int i = 0; i < fields.length; i++) {
                        fieldNames[i] = fields[i].getName();
                        fieldTypes[i] = fields[i].getType();
                    }
                }

                @Override
                int length() { return fieldNames.length; }

                @Override
                String fieldName(int i) { return fieldNames[i]; }

                @Override
                Class<?> fieldType(int i) { return fieldTypes[i]; }
            }
        }
    }

    /** Record specific support for retrieving and binding stream field values. */
    static final class RecordSupport {
        /**
         * Returns canonical record constructor adapted to take two arguments:
         * {@code (byte[] primValues, Object[] objValues)}
         * and return
         * {@code Object}
         */
        @SuppressWarnings("removal")
        static MethodHandle deserializationCtr(ObjectStreamClass desc) {
            // check the cached value 1st
            MethodHandle mh = desc.deserializationCtr;
            if (mh != null) return mh;
            mh = desc.deserializationCtrs.get(desc.getFields(false));
            if (mh != null) return desc.deserializationCtr = mh;

            // retrieve record components
            RecordComponent[] recordComponents;
            try {
                Class<?> cls = desc.forClass();
                PrivilegedExceptionAction<RecordComponent[]> pa = cls::getRecordComponents;
                recordComponents = AccessController.doPrivileged(pa);
            } catch (PrivilegedActionException e) {
                throw new InternalError(e.getCause());
            }

            // retrieve the canonical constructor
            // (T1, T2, ..., Tn):TR
            mh = desc.getRecordConstructor();

            // change return type to Object
            // (T1, T2, ..., Tn):TR -> (T1, T2, ..., Tn):Object
            mh = mh.asType(mh.type().changeReturnType(Object.class));

            // drop last 2 arguments representing primValues and objValues arrays
            // (T1, T2, ..., Tn):Object -> (T1, T2, ..., Tn, byte[], Object[]):Object
            mh = MethodHandles.dropArguments(mh, mh.type().parameterCount(), byte[].class, Object[].class);

            for (int i = recordComponents.length-1; i >= 0; i--) {
                String name = recordComponents[i].getName();
                Class<?> type = recordComponents[i].getType();
                // obtain stream field extractor that extracts argument at
                // position i (Ti+1) from primValues and objValues arrays
                // (byte[], Object[]):Ti+1
                MethodHandle combiner = streamFieldExtractor(name, type, desc);
                // fold byte[] privValues and Object[] objValues into argument at position i (Ti+1)
                // (..., Ti, Ti+1, byte[], Object[]):Object -> (..., Ti, byte[], Object[]):Object
                mh = MethodHandles.foldArguments(mh, i, combiner);
            }
            // what we are left with is a MethodHandle taking just the primValues
            // and objValues arrays and returning the constructed record instance
            // (byte[], Object[]):Object

            // store it into cache and return the 1st value stored
            return desc.deserializationCtr =
                desc.deserializationCtrs.putIfAbsentAndGet(desc.getFields(false), mh);
        }

        /** Returns the number of primitive fields for the given descriptor. */
        private static int numberPrimValues(ObjectStreamClass desc) {
            ObjectStreamField[] fields = desc.getFields();
            int primValueCount = 0;
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].isPrimitive())
                    primValueCount++;
                else
                    break;  // can be no more
            }
            return primValueCount;
        }

        /**
         * Returns extractor MethodHandle taking the primValues and objValues arrays
         * and extracting the argument of canonical constructor with given name and type
         * or producing  default value for the given type if the field is absent.
         */
        private static MethodHandle streamFieldExtractor(String pName,
                                                         Class<?> pType,
                                                         ObjectStreamClass desc) {
            ObjectStreamField[] fields = desc.getFields(false);

            for (int i = 0; i < fields.length; i++) {
                ObjectStreamField f = fields[i];
                String fName = f.getName();
                if (!fName.equals(pName))
                    continue;

                Class<?> fType = f.getField().getType();
                if (!pType.isAssignableFrom(fType))
                    throw new InternalError(fName + " unassignable, pType:" + pType + ", fType:" + fType);

                if (f.isPrimitive()) {
                    // (byte[], int):fType
                    MethodHandle mh = PRIM_VALUE_EXTRACTORS.get(fType);
                    if (mh == null) {
                        throw new InternalError("Unexpected type: " + fType);
                    }
                    // bind offset
                    // (byte[], int):fType -> (byte[]):fType
                    mh = MethodHandles.insertArguments(mh, 1, f.getOffset());
                    // drop objValues argument
                    // (byte[]):fType -> (byte[], Object[]):fType
                    mh = MethodHandles.dropArguments(mh, 1, Object[].class);
                    // adapt return type to pType
                    // (byte[], Object[]):fType -> (byte[], Object[]):pType
                    if (pType != fType) {
                        mh = mh.asType(mh.type().changeReturnType(pType));
                    }
                    return mh;
                } else { // reference
                    // (Object[], int):Object
                    MethodHandle mh = MethodHandles.arrayElementGetter(Object[].class);
                    // bind index
                    // (Object[], int):Object -> (Object[]):Object
                    mh = MethodHandles.insertArguments(mh, 1, i - numberPrimValues(desc));
                    // drop primValues argument
                    // (Object[]):Object -> (byte[], Object[]):Object
                    mh = MethodHandles.dropArguments(mh, 0, byte[].class);
                    // adapt return type to pType
                    // (byte[], Object[]):Object -> (byte[], Object[]):pType
                    if (pType != Object.class) {
                        mh = mh.asType(mh.type().changeReturnType(pType));
                    }
                    return mh;
                }
            }

            // return default value extractor if no field matches pName
            return MethodHandles.empty(MethodType.methodType(pType, byte[].class, Object[].class));
        }

        private static final Map<Class<?>, MethodHandle> PRIM_VALUE_EXTRACTORS;
        static {
            var lkp = MethodHandles.lookup();
            try {
                PRIM_VALUE_EXTRACTORS = Map.of(
                    byte.class, MethodHandles.arrayElementGetter(byte[].class),
                    short.class, lkp.findStatic(Bits.class, "getShort", MethodType.methodType(short.class, byte[].class, int.class)),
                    int.class, lkp.findStatic(Bits.class, "getInt", MethodType.methodType(int.class, byte[].class, int.class)),
                    long.class, lkp.findStatic(Bits.class, "getLong", MethodType.methodType(long.class, byte[].class, int.class)),
                    float.class, lkp.findStatic(Bits.class, "getFloat", MethodType.methodType(float.class, byte[].class, int.class)),
                    double.class, lkp.findStatic(Bits.class, "getDouble", MethodType.methodType(double.class, byte[].class, int.class)),
                    char.class, lkp.findStatic(Bits.class, "getChar", MethodType.methodType(char.class, byte[].class, int.class)),
                    boolean.class, lkp.findStatic(Bits.class, "getBoolean", MethodType.methodType(boolean.class, byte[].class, int.class))
                );
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new InternalError("Can't lookup Bits.getXXX", e);
            }
        }
    }
}
