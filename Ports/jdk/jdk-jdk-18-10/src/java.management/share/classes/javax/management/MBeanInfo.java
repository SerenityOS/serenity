/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.management;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Objects;

import static javax.management.ImmutableDescriptor.nonNullDescriptor;

/**
 * <p>Describes the management interface exposed by an MBean; that is,
 * the set of attributes and operations which are available for
 * management operations.  Instances of this class are immutable.
 * Subclasses may be mutable but this is not recommended.</p>
 *
 * <p id="info-changed">Usually the {@code MBeanInfo} for any given MBean does
 * not change over the lifetime of that MBean.  Dynamic MBeans can change their
 * {@code MBeanInfo} and in that case it is recommended that they emit a {@link
 * Notification} with a {@linkplain Notification#getType() type} of {@code
 * "jmx.mbean.info.changed"} and a {@linkplain Notification#getUserData()
 * userData} that is the new {@code MBeanInfo}.  This is not required, but
 * provides a conventional way for clients of the MBean to discover the change.
 * See also the <a href="Descriptor.html#immutableInfo">immutableInfo</a> and
 * <a href="Descriptor.html#infoTimeout">infoTimeout</a> fields in the {@code
 * MBeanInfo} {@link Descriptor}.</p>
 *
 * <p>The contents of the {@code MBeanInfo} for a Dynamic MBean
 * are determined by its {@link DynamicMBean#getMBeanInfo
 * getMBeanInfo()} method.  This includes Open MBeans and Model
 * MBeans, which are kinds of Dynamic MBeans.</p>
 *
 * <p>The contents of the {@code MBeanInfo} for a Standard MBean
 * are determined by the MBean server as follows:</p>
 *
 * <ul>
 *
 * <li>{@link #getClassName()} returns the Java class name of the MBean
 * object;
 *
 * <li>{@link #getConstructors()} returns the list of all public
 * constructors in that object;
 *
 * <li>{@link #getAttributes()} returns the list of all attributes
 * whose existence is deduced from the presence in the MBean interface
 * of a <code>get<i>Name</i></code>, <code>is<i>Name</i></code>, or
 * <code>set<i>Name</i></code> method that conforms to the conventions
 * for Standard MBeans;
 *
 * <li>{@link #getOperations()} returns the list of all methods in
 * the MBean interface that do not represent attributes;
 *
 * <li>{@link #getNotifications()} returns an empty array if the MBean
 * does not implement the {@link NotificationBroadcaster} interface,
 * otherwise the result of calling {@link
 * NotificationBroadcaster#getNotificationInfo()} on it;
 *
 * <li>{@link #getDescriptor()} returns a descriptor containing the contents
 * of any descriptor annotations in the MBean interface (see
 * {@link DescriptorKey &#64;DescriptorKey}).
 *
 * </ul>
 *
 * <p>The description returned by {@link #getDescription()} and the
 * descriptions of the contained attributes and operations are not specified.</p>
 *
 * <p>The remaining details of the {@code MBeanInfo} for a
 * Standard MBean are not specified.  This includes the description of
 * any contained constructors, and notifications; the names
 * of parameters to constructors and operations; and the descriptions of
 * constructor parameters.</p>
 *
 * @since 1.5
 */
public class MBeanInfo implements Cloneable, Serializable, DescriptorRead {

    /* Serial version */
    static final long serialVersionUID = -6451021435135161911L;

    /**
     * @serial The Descriptor for the MBean.  This field
     * can be null, which is equivalent to an empty Descriptor.
     */
    private transient Descriptor descriptor;

    /**
     * @serial The human readable description of the class.
     */
    private final String description;

    /**
     * @serial The MBean qualified name.
     */
    private final String className;

    /**
     * @serial The MBean attribute descriptors.
     */
    private final MBeanAttributeInfo[] attributes;

    /**
     * @serial The MBean operation descriptors.
     */
    private final MBeanOperationInfo[] operations;

     /**
     * @serial The MBean constructor descriptors.
     */
    private final MBeanConstructorInfo[] constructors;

    /**
     * @serial The MBean notification descriptors.
     */
    private final MBeanNotificationInfo[] notifications;

    private transient int hashCode;

    /**
     * <p>True if this class is known not to override the array-valued
     * getters of MBeanInfo.  Obviously true for MBeanInfo itself, and true
     * for a subclass where we succeed in reflecting on the methods
     * and discover they are not overridden.</p>
     *
     * <p>The purpose of this variable is to avoid cloning the arrays
     * when doing operations like {@link #equals} where we know they
     * will not be changed.  If a subclass overrides a getter, we
     * cannot access the corresponding array directly.</p>
     */
    private final transient boolean arrayGettersSafe;

    /**
     * Constructs an {@code MBeanInfo}.
     *
     * @param className The name of the Java class of the MBean described
     * by this {@code MBeanInfo}.  This value may be any
     * syntactically legal Java class name.  It does not have to be a
     * Java class known to the MBean server or to the MBean's
     * ClassLoader.  If it is a Java class known to the MBean's
     * ClassLoader, it is recommended but not required that the
     * class's public methods include those that would appear in a
     * Standard MBean implementing the attributes and operations in
     * this MBeanInfo.
     * @param description A human readable description of the MBean (optional).
     * @param attributes The list of exposed attributes of the MBean.
     * This may be null with the same effect as a zero-length array.
     * @param constructors The list of public constructors of the
     * MBean.  This may be null with the same effect as a zero-length
     * array.
     * @param operations The list of operations of the MBean.  This
     * may be null with the same effect as a zero-length array.
     * @param notifications The list of notifications emitted.  This
     * may be null with the same effect as a zero-length array.
     */
    public MBeanInfo(String className,
                     String description,
                     MBeanAttributeInfo[] attributes,
                     MBeanConstructorInfo[] constructors,
                     MBeanOperationInfo[] operations,
                     MBeanNotificationInfo[] notifications)
            throws IllegalArgumentException {
        this(className, description, attributes, constructors, operations,
             notifications, null);
    }

    /**
     * Constructs an {@code MBeanInfo}.
     *
     * @param className The name of the Java class of the MBean described
     * by this {@code MBeanInfo}.  This value may be any
     * syntactically legal Java class name.  It does not have to be a
     * Java class known to the MBean server or to the MBean's
     * ClassLoader.  If it is a Java class known to the MBean's
     * ClassLoader, it is recommended but not required that the
     * class's public methods include those that would appear in a
     * Standard MBean implementing the attributes and operations in
     * this MBeanInfo.
     * @param description A human readable description of the MBean (optional).
     * @param attributes The list of exposed attributes of the MBean.
     * This may be null with the same effect as a zero-length array.
     * @param constructors The list of public constructors of the
     * MBean.  This may be null with the same effect as a zero-length
     * array.
     * @param operations The list of operations of the MBean.  This
     * may be null with the same effect as a zero-length array.
     * @param notifications The list of notifications emitted.  This
     * may be null with the same effect as a zero-length array.
     * @param descriptor The descriptor for the MBean.  This may be null
     * which is equivalent to an empty descriptor.
     *
     * @since 1.6
     */
    public MBeanInfo(String className,
                     String description,
                     MBeanAttributeInfo[] attributes,
                     MBeanConstructorInfo[] constructors,
                     MBeanOperationInfo[] operations,
                     MBeanNotificationInfo[] notifications,
                     Descriptor descriptor)
            throws IllegalArgumentException {

        this.className = className;

        this.description = description;

        if (attributes == null)
            attributes = MBeanAttributeInfo.NO_ATTRIBUTES;
        this.attributes = attributes;

        if (operations == null)
            operations = MBeanOperationInfo.NO_OPERATIONS;
        this.operations = operations;

        if (constructors == null)
            constructors = MBeanConstructorInfo.NO_CONSTRUCTORS;
        this.constructors = constructors;

        if (notifications == null)
            notifications = MBeanNotificationInfo.NO_NOTIFICATIONS;
        this.notifications = notifications;

        if (descriptor == null)
            descriptor = ImmutableDescriptor.EMPTY_DESCRIPTOR;
        this.descriptor = descriptor;

        this.arrayGettersSafe =
                arrayGettersSafe(this.getClass(), MBeanInfo.class);
    }

    /**
     * <p>Returns a shallow clone of this instance.
     * The clone is obtained by simply calling {@code super.clone()},
     * thus calling the default native shallow cloning mechanism
     * implemented by {@code Object.clone()}.
     * No deeper cloning of any internal field is made.</p>
     *
     * <p>Since this class is immutable, the clone method is chiefly of
     * interest to subclasses.</p>
     */
     @Override
     public Object clone () {
         try {
             return super.clone() ;
         } catch (CloneNotSupportedException e) {
             // should not happen as this class is cloneable
             return null;
         }
     }


    /**
     * Returns the name of the Java class of the MBean described by
     * this {@code MBeanInfo}.
     *
     * @return the class name.
     */
    public String getClassName()  {
        return className;
    }

    /**
     * Returns a human readable description of the MBean.
     *
     * @return the description.
     */
    public String getDescription()  {
        return description;
    }

    /**
     * Returns the list of attributes exposed for management.
     * Each attribute is described by an {@code MBeanAttributeInfo} object.
     *
     * The returned array is a shallow copy of the internal array,
     * which means that it is a copy of the internal array of
     * references to the {@code MBeanAttributeInfo} objects
     * but that each referenced {@code MBeanAttributeInfo} object is not copied.
     *
     * @return  An array of {@code MBeanAttributeInfo} objects.
     */
    public MBeanAttributeInfo[] getAttributes()   {
        MBeanAttributeInfo[] as = nonNullAttributes();
        if (as.length == 0)
            return as;
        else
            return as.clone();
    }

    private MBeanAttributeInfo[] fastGetAttributes() {
        if (arrayGettersSafe)
            return nonNullAttributes();
        else
            return getAttributes();
    }

    /**
     * Return the value of the attributes field, or an empty array if
     * the field is null.  This can't happen with a
     * normally-constructed instance of this class, but can if the
     * instance was deserialized from another implementation that
     * allows the field to be null.  It would be simpler if we enforced
     * the class invariant that these fields cannot be null by writing
     * a readObject() method, but that would require us to define the
     * various array fields as non-final, which is annoying because
     * conceptually they are indeed final.
     */
    private MBeanAttributeInfo[] nonNullAttributes() {
        return (attributes == null) ?
            MBeanAttributeInfo.NO_ATTRIBUTES : attributes;
    }

    /**
     * Returns the list of operations  of the MBean.
     * Each operation is described by an {@code MBeanOperationInfo} object.
     *
     * The returned array is a shallow copy of the internal array,
     * which means that it is a copy of the internal array of
     * references to the {@code MBeanOperationInfo} objects
     * but that each referenced {@code MBeanOperationInfo} object is not copied.
     *
     * @return  An array of {@code MBeanOperationInfo} objects.
     */
    public MBeanOperationInfo[] getOperations()  {
        MBeanOperationInfo[] os = nonNullOperations();
        if (os.length == 0)
            return os;
        else
            return os.clone();
    }

    private MBeanOperationInfo[] fastGetOperations() {
        if (arrayGettersSafe)
            return nonNullOperations();
        else
            return getOperations();
    }

    private MBeanOperationInfo[] nonNullOperations() {
        return (operations == null) ?
            MBeanOperationInfo.NO_OPERATIONS : operations;
    }

    /**
     * <p>Returns the list of the public constructors of the MBean.
     * Each constructor is described by an
     * {@code MBeanConstructorInfo} object.</p>
     *
     * <p>The returned array is a shallow copy of the internal array,
     * which means that it is a copy of the internal array of
     * references to the {@code MBeanConstructorInfo} objects but
     * that each referenced {@code MBeanConstructorInfo} object
     * is not copied.</p>
     *
     * <p>The returned list is not necessarily exhaustive.  That is,
     * the MBean may have a public constructor that is not in the
     * list.  In this case, the MBean server can construct another
     * instance of this MBean's class using that constructor, even
     * though it is not listed here.</p>
     *
     * @return  An array of {@code MBeanConstructorInfo} objects.
     */
    public MBeanConstructorInfo[] getConstructors()  {
        MBeanConstructorInfo[] cs = nonNullConstructors();
        if (cs.length == 0)
            return cs;
        else
            return cs.clone();
    }

    private MBeanConstructorInfo[] fastGetConstructors() {
        if (arrayGettersSafe)
            return nonNullConstructors();
        else
            return getConstructors();
    }

    private MBeanConstructorInfo[] nonNullConstructors() {
        return (constructors == null) ?
            MBeanConstructorInfo.NO_CONSTRUCTORS : constructors;
    }

    /**
     * Returns the list of the notifications emitted by the MBean.
     * Each notification is described by an {@code MBeanNotificationInfo} object.
     *
     * The returned array is a shallow copy of the internal array,
     * which means that it is a copy of the internal array of
     * references to the {@code MBeanNotificationInfo} objects
     * but that each referenced {@code MBeanNotificationInfo} object is not copied.
     *
     * @return  An array of {@code MBeanNotificationInfo} objects.
     */
    public MBeanNotificationInfo[] getNotifications()  {
        MBeanNotificationInfo[] ns = nonNullNotifications();
        if (ns.length == 0)
            return ns;
        else
            return ns.clone();
    }

    private MBeanNotificationInfo[] fastGetNotifications() {
        if (arrayGettersSafe)
            return nonNullNotifications();
        else
            return getNotifications();
    }

    private MBeanNotificationInfo[] nonNullNotifications() {
        return (notifications == null) ?
            MBeanNotificationInfo.NO_NOTIFICATIONS : notifications;
    }

    /**
     * Get the descriptor of this MBeanInfo.  Changing the returned value
     * will have no affect on the original descriptor.
     *
     * @return a descriptor that is either immutable or a copy of the original.
     *
     * @since 1.6
     */
    public Descriptor getDescriptor() {
        return (Descriptor) nonNullDescriptor(descriptor).clone();
    }

    @Override
    public String toString() {
        return
            getClass().getName() + "[" +
            "description=" + getDescription() + ", " +
            "attributes=" + Arrays.asList(fastGetAttributes()) + ", " +
            "constructors=" + Arrays.asList(fastGetConstructors()) + ", " +
            "operations=" + Arrays.asList(fastGetOperations()) + ", " +
            "notifications=" + Arrays.asList(fastGetNotifications()) + ", " +
            "descriptor=" + getDescriptor() +
            "]";
    }

    /**
     * <p>Compare this MBeanInfo to another.  Two MBeanInfo objects
     * are equal if and only if they return equal values for {@link
     * #getClassName()}, for {@link #getDescription()}, and for
     * {@link #getDescriptor()}, and the
     * arrays returned by the two objects for {@link
     * #getAttributes()}, {@link #getOperations()}, {@link
     * #getConstructors()}, and {@link #getNotifications()} are
     * pairwise equal.  Here "equal" means {@link
     * Object#equals(Object)}, not identity.</p>
     *
     * <p>If two MBeanInfo objects return the same values in one of
     * their arrays but in a different order then they are not equal.</p>
     *
     * @param o the object to compare to.
     *
     * @return true if and only if {@code o} is an MBeanInfo that is equal
     * to this one according to the rules above.
     */
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MBeanInfo))
            return false;
        MBeanInfo p = (MBeanInfo) o;
        if (!isEqual(getClassName(),  p.getClassName()) ||
                !isEqual(getDescription(), p.getDescription()) ||
                !getDescriptor().equals(p.getDescriptor())) {
            return false;
        }

        return
            (Arrays.equals(p.fastGetAttributes(), fastGetAttributes()) &&
             Arrays.equals(p.fastGetOperations(), fastGetOperations()) &&
             Arrays.equals(p.fastGetConstructors(), fastGetConstructors()) &&
             Arrays.equals(p.fastGetNotifications(), fastGetNotifications()));
    }

    @Override
    public int hashCode() {
        /* Since computing the hashCode is quite expensive, we cache it.
           If by some terrible misfortune the computed value is 0, the
           caching won't work and we will recompute it every time.

           We don't bother synchronizing, because, at worst, n different
           threads will compute the same hashCode at the same time.  */
        if (hashCode != 0)
            return hashCode;

        hashCode = Objects.hash(getClassName(), getDescriptor())
                ^ Arrays.hashCode(fastGetAttributes())
                ^ Arrays.hashCode(fastGetOperations())
                ^ Arrays.hashCode(fastGetConstructors())
                ^ Arrays.hashCode(fastGetNotifications());

        return hashCode;
    }

    /**
     * Cached results of previous calls to arrayGettersSafe.  This is
     * a WeakHashMap so that we don't prevent a class from being
     * garbage collected just because we know whether it's immutable.
     */
    private static final Map<Class<?>, Boolean> arrayGettersSafeMap =
        new WeakHashMap<Class<?>, Boolean>();

    /**
     * Return true if {@code subclass} is known to preserve the
     * immutability of {@code immutableClass}.  The class
     * {@code immutableClass} is a reference class that is known
     * to be immutable.  The subclass {@code subclass} is
     * considered immutable if it does not override any public method
     * of {@code immutableClass} whose name begins with "get".
     * This is obviously not an infallible test for immutability,
     * but it works for the public interfaces of the MBean*Info classes.
    */
    @SuppressWarnings("removal")
    static boolean arrayGettersSafe(Class<?> subclass, Class<?> immutableClass) {
        if (subclass == immutableClass)
            return true;
        synchronized (arrayGettersSafeMap) {
            Boolean safe = arrayGettersSafeMap.get(subclass);
            if (safe == null) {
                try {
                    ArrayGettersSafeAction action =
                        new ArrayGettersSafeAction(subclass, immutableClass);
                    safe = AccessController.doPrivileged(action);
                } catch (Exception e) { // e.g. SecurityException
                    /* We don't know, so we assume it isn't.  */
                    safe = false;
                }
                arrayGettersSafeMap.put(subclass, safe);
            }
            return safe;
        }
    }

    /*
     * The PrivilegedAction stuff is probably overkill.  We can be
     * pretty sure the caller does have the required privileges -- a
     * JMX user that can't do reflection can't even use Standard
     * MBeans!  But there's probably a performance gain by not having
     * to check the whole call stack.
     */
    private static class ArrayGettersSafeAction
            implements PrivilegedAction<Boolean> {

        private final Class<?> subclass;
        private final Class<?> immutableClass;

        ArrayGettersSafeAction(Class<?> subclass, Class<?> immutableClass) {
            this.subclass = subclass;
            this.immutableClass = immutableClass;
        }

        public Boolean run() {
            Method[] methods = immutableClass.getMethods();
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];
                String methodName = method.getName();
                if (methodName.startsWith("get") &&
                        method.getParameterTypes().length == 0 &&
                        method.getReturnType().isArray()) {
                    try {
                        Method submethod =
                            subclass.getMethod(methodName);
                        if (!submethod.equals(method))
                            return false;
                    } catch (NoSuchMethodException e) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private static boolean isEqual(String s1, String s2) {
        boolean ret;

        if (s1 == null) {
            ret = (s2 == null);
        } else {
            ret = s1.equals(s2);
        }

        return ret;
    }

    /**
     * Serializes an {@link MBeanInfo} to an {@link ObjectOutputStream}.
     * @serialData
     * For compatibility reasons, an object of this class is serialized as follows.
     * <p>
     * The method {@link ObjectOutputStream#defaultWriteObject defaultWriteObject()}
     * is called first to serialize the object except the field {@code descriptor}
     * which is declared as transient. The field {@code descriptor} is serialized
     * as follows:
     *     <ul>
     *     <li> If {@code descriptor} is an instance of the class
     *        {@link ImmutableDescriptor}, the method {@link ObjectOutputStream#write
     *        write(int val)} is called to write a byte with the value {@code 1},
     *        then the method {@link ObjectOutputStream#writeObject writeObject(Object obj)}
     *        is called twice to serialize the field names and the field values of the
     *        {@code descriptor}, respectively as a {@code String[]} and an
     *        {@code Object[]};</li>
     *     <li> Otherwise, the method {@link ObjectOutputStream#write write(int val)}
     *        is called to write a byte with the value {@code 0}, then the method
     *        {@link ObjectOutputStream#writeObject writeObject(Object obj)} is called
     *        to serialize the field {@code descriptor} directly.
     *     </ul>
     *
     * @since 1.6
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();

        if (descriptor.getClass() == ImmutableDescriptor.class) {
            out.write(1);

            final String[] names = descriptor.getFieldNames();

            out.writeObject(names);
            out.writeObject(descriptor.getFieldValues(names));
        } else {
            out.write(0);

            out.writeObject(descriptor);
        }
    }

    /**
     * Deserializes an {@link MBeanInfo} from an {@link ObjectInputStream}.
     * @serialData
     * For compatibility reasons, an object of this class is deserialized as follows.
     * <p>
     * The method {@link ObjectInputStream#defaultReadObject defaultReadObject()}
     * is called first to deserialize the object except the field
     * {@code descriptor}, which is not serialized in the default way. Then the method
     * {@link ObjectInputStream#read read()} is called to read a byte, the field
     * {@code descriptor} is deserialized according to the value of the byte value:
     *    <ul>
     *    <li>1. The method {@link ObjectInputStream#readObject readObject()}
     *       is called twice to obtain the field names (a {@code String[]}) and
     *       the field values (an {@code Object[]}) of the {@code descriptor}.
     *       The two obtained values then are used to construct
     *       an {@link ImmutableDescriptor} instance for the field
     *       {@code descriptor};</li>
     *    <li>0. The value for the field {@code descriptor} is obtained directly
     *       by calling the method {@link ObjectInputStream#readObject readObject()}.
     *       If the obtained value is null, the field {@code descriptor} is set to
     *       {@link ImmutableDescriptor#EMPTY_DESCRIPTOR EMPTY_DESCRIPTOR};</li>
     *    <li>-1. This means that there is no byte to read and that the object is from
     *       an earlier version of the JMX API. The field {@code descriptor} is set to
     *       {@link ImmutableDescriptor#EMPTY_DESCRIPTOR EMPTY_DESCRIPTOR}.</li>
     *    <li>Any other value. A {@link StreamCorruptedException} is thrown.</li>
     *    </ul>
     *
     * @since 1.6
     */

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {

        in.defaultReadObject();

        switch (in.read()) {
        case 1:
            final String[] names = (String[])in.readObject();

            final Object[] values = (Object[]) in.readObject();
            descriptor = (names.length == 0) ?
                ImmutableDescriptor.EMPTY_DESCRIPTOR :
                new ImmutableDescriptor(names, values);

            break;
        case 0:
            descriptor = (Descriptor)in.readObject();

            if (descriptor == null) {
                descriptor = ImmutableDescriptor.EMPTY_DESCRIPTOR;
            }

            break;
        case -1: // from an earlier version of the JMX API
            descriptor = ImmutableDescriptor.EMPTY_DESCRIPTOR;

            break;
        default:
            throw new StreamCorruptedException("Got unexpected byte.");
        }
    }
}
