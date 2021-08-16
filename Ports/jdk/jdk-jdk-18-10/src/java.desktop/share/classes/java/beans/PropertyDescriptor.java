/*
 * Copyright (c) 1996, 2015, Oracle and/or its affiliates. All rights reserved.
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
package java.beans;

import java.lang.ref.Reference;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.Map.Entry;

import com.sun.beans.introspect.PropertyInfo;
import sun.reflect.misc.ReflectUtil;

/**
 * A PropertyDescriptor describes one property that a Java Bean
 * exports via a pair of accessor methods.
 * @since 1.1
 */
public class PropertyDescriptor extends FeatureDescriptor {

    private Reference<? extends Class<?>> propertyTypeRef;
    private final MethodRef readMethodRef = new MethodRef();
    private final MethodRef writeMethodRef = new MethodRef();
    private Reference<? extends Class<?>> propertyEditorClassRef;

    private boolean bound;
    private boolean constrained;

    // The base name of the method name which will be prefixed with the
    // read and write method. If name == "foo" then the baseName is "Foo"
    private String baseName;

    private String writeMethodName;
    private String readMethodName;

    /**
     * Constructs a PropertyDescriptor for a property that follows
     * the standard Java convention by having getFoo and setFoo
     * accessor methods.  Thus if the argument name is "fred", it will
     * assume that the writer method is "setFred" and the reader method
     * is "getFred" (or "isFred" for a boolean property).  Note that the
     * property name should start with a lower case character, which will
     * be capitalized in the method names.
     *
     * @param propertyName The programmatic name of the property.
     * @param beanClass The Class object for the target bean.  For
     *          example sun.beans.OurButton.class.
     * @exception IntrospectionException if an exception occurs during
     *              introspection.
     */
    public PropertyDescriptor(String propertyName, Class<?> beanClass)
                throws IntrospectionException {
        this(propertyName, beanClass,
                Introspector.IS_PREFIX + NameGenerator.capitalize(propertyName),
                Introspector.SET_PREFIX + NameGenerator.capitalize(propertyName));
    }

    /**
     * This constructor takes the name of a simple property, and method
     * names for reading and writing the property.
     *
     * @param propertyName The programmatic name of the property.
     * @param beanClass The Class object for the target bean.  For
     *          example sun.beans.OurButton.class.
     * @param readMethodName The name of the method used for reading the property
     *           value.  May be null if the property is write-only.
     * @param writeMethodName The name of the method used for writing the property
     *           value.  May be null if the property is read-only.
     * @exception IntrospectionException if an exception occurs during
     *              introspection.
     */
    public PropertyDescriptor(String propertyName, Class<?> beanClass,
                String readMethodName, String writeMethodName)
                throws IntrospectionException {
        if (beanClass == null) {
            throw new IntrospectionException("Target Bean class is null");
        }
        if (propertyName == null || propertyName.length() == 0) {
            throw new IntrospectionException("bad property name");
        }
        if ("".equals(readMethodName) || "".equals(writeMethodName)) {
            throw new IntrospectionException("read or write method name should not be the empty string");
        }
        setName(propertyName);
        setClass0(beanClass);

        this.readMethodName = readMethodName;
        if (readMethodName != null && getReadMethod() == null) {
            throw new IntrospectionException("Method not found: " + readMethodName);
        }
        this.writeMethodName = writeMethodName;
        if (writeMethodName != null && getWriteMethod() == null) {
            throw new IntrospectionException("Method not found: " + writeMethodName);
        }
        // If this class or one of its base classes allow PropertyChangeListener,
        // then we assume that any properties we discover are "bound".
        // See Introspector.getTargetPropertyInfo() method.
        Class<?>[] args = { PropertyChangeListener.class };
        this.bound = null != Introspector.findMethod(beanClass, "addPropertyChangeListener", args.length, args);
    }

    /**
     * This constructor takes the name of a simple property, and Method
     * objects for reading and writing the property.
     *
     * @param propertyName The programmatic name of the property.
     * @param readMethod The method used for reading the property value.
     *          May be null if the property is write-only.
     * @param writeMethod The method used for writing the property value.
     *          May be null if the property is read-only.
     * @exception IntrospectionException if an exception occurs during
     *              introspection.
     */
    public PropertyDescriptor(String propertyName, Method readMethod, Method writeMethod)
                throws IntrospectionException {
        if (propertyName == null || propertyName.length() == 0) {
            throw new IntrospectionException("bad property name");
        }
        setName(propertyName);
        setReadMethod(readMethod);
        setWriteMethod(writeMethod);
    }

    /**
     * Creates {@code PropertyDescriptor} from the specified property info.
     *
     * @param entry  the pair of values,
     *               where the {@code key} is the base name of the property (the rest of the method name)
     *               and the {@code value} is the automatically generated property info
     * @param bound  the flag indicating whether it is possible to treat this property as a bound property
     *
     * @since 9
     */
    PropertyDescriptor(Entry<String,PropertyInfo> entry, boolean bound) {
        String base = entry.getKey();
        PropertyInfo info = entry.getValue();
        setName(Introspector.decapitalize(base));
        setReadMethod0(info.getReadMethod());
        setWriteMethod0(info.getWriteMethod());
        setPropertyType(info.getPropertyType());
        setConstrained(info.isConstrained());
        setBound(bound && info.is(PropertyInfo.Name.bound));

        boolean isExpert = info.is(PropertyInfo.Name.expert);
        setValue(PropertyInfo.Name.expert.name(), isExpert); // compatibility
        setExpert(isExpert);

        boolean isHidden = info.is(PropertyInfo.Name.hidden);
        setValue(PropertyInfo.Name.hidden.name(), isHidden); // compatibility
        setHidden(isHidden);

        setPreferred(info.is(PropertyInfo.Name.preferred));

        boolean isRequired = info.is(PropertyInfo.Name.required);
        setValue(PropertyInfo.Name.required.name(), isRequired);

        boolean visual = info.is(PropertyInfo.Name.visualUpdate);
        setValue(PropertyInfo.Name.visualUpdate.name(), visual);

        Object description = info.get(PropertyInfo.Name.description);
        if (description != null) {
            setShortDescription(description.toString());
        }
        Object values = info.get(PropertyInfo.Name.enumerationValues);
        if (values == null) {
            values = new Object[0];
        }
        setValue(PropertyInfo.Name.enumerationValues.name(), values);
        this.baseName = base;
    }

    /**
     * Returns the Java type info for the property.
     * Note that the {@code Class} object may describe
     * primitive Java types such as {@code int}.
     * This type is returned by the read method
     * or is used as the parameter type of the write method.
     * Returns {@code null} if the type is an indexed property
     * that does not support non-indexed access.
     *
     * @return the {@code Class} object that represents the Java type info,
     *         or {@code null} if the type cannot be determined
     */
    public synchronized Class<?> getPropertyType() {
        Class<?> type = getPropertyType0();
        if (type  == null) {
            try {
                type = findPropertyType(getReadMethod(), getWriteMethod());
                setPropertyType(type);
            } catch (IntrospectionException ex) {
                // Fall
            }
        }
        return type;
    }

    private void setPropertyType(Class<?> type) {
        this.propertyTypeRef = getWeakReference(type);
    }

    private Class<?> getPropertyType0() {
        return (this.propertyTypeRef != null)
                ? this.propertyTypeRef.get()
                : null;
    }

    /**
     * Gets the method that should be used to read the property value.
     *
     * @return The method that should be used to read the property value.
     * May return null if the property can't be read.
     */
    public synchronized Method getReadMethod() {
        Method readMethod = this.readMethodRef.get();
        if (readMethod == null) {
            Class<?> cls = getClass0();
            if (cls == null || (readMethodName == null && !this.readMethodRef.isSet())) {
                // The read method was explicitly set to null.
                return null;
            }
            String nextMethodName = Introspector.GET_PREFIX + getBaseName();
            if (readMethodName == null) {
                Class<?> type = getPropertyType0();
                if (type == boolean.class || type == null) {
                    readMethodName = Introspector.IS_PREFIX + getBaseName();
                } else {
                    readMethodName = nextMethodName;
                }
            }

            // Since there can be multiple write methods but only one getter
            // method, find the getter method first so that you know what the
            // property type is.  For booleans, there can be "is" and "get"
            // methods.  If an "is" method exists, this is the official
            // reader method so look for this one first.
            readMethod = Introspector.findMethod(cls, readMethodName, 0);
            if ((readMethod == null) && !readMethodName.equals(nextMethodName)) {
                readMethodName = nextMethodName;
                readMethod = Introspector.findMethod(cls, readMethodName, 0);
            }
            try {
                setReadMethod(readMethod);
            } catch (IntrospectionException ex) {
                // fall
            }
        }
        return readMethod;
    }

    /**
     * Sets the method that should be used to read the property value.
     *
     * @param readMethod The new read method.
     * @throws IntrospectionException if the read method is invalid
     * @since 1.2
     */
    public synchronized void setReadMethod(Method readMethod)
                                throws IntrospectionException {
        // The property type is determined by the read method.
        setPropertyType(findPropertyType(readMethod, this.writeMethodRef.get()));
        setReadMethod0(readMethod);
    }

    private void setReadMethod0(Method readMethod) {
        this.readMethodRef.set(readMethod);
        if (readMethod == null) {
            readMethodName = null;
            return;
        }
        setClass0(readMethod.getDeclaringClass());

        readMethodName = readMethod.getName();
        setTransient(readMethod.getAnnotation(Transient.class));
    }

    /**
     * Gets the method that should be used to write the property value.
     *
     * @return The method that should be used to write the property value.
     * May return null if the property can't be written.
     */
    public synchronized Method getWriteMethod() {
        Method writeMethod = this.writeMethodRef.get();
        if (writeMethod == null) {
            Class<?> cls = getClass0();
            if (cls == null || (writeMethodName == null && !this.writeMethodRef.isSet())) {
                // The write method was explicitly set to null.
                return null;
            }

            // We need the type to fetch the correct method.
            Class<?> type = getPropertyType0();
            if (type == null) {
                try {
                    // Can't use getPropertyType since it will lead to recursive loop.
                    type = findPropertyType(getReadMethod(), null);
                    setPropertyType(type);
                } catch (IntrospectionException ex) {
                    // Without the correct property type we can't be guaranteed
                    // to find the correct method.
                    return null;
                }
            }

            if (writeMethodName == null) {
                writeMethodName = Introspector.SET_PREFIX + getBaseName();
            }

            Class<?>[] args = (type == null) ? null : new Class<?>[] { type };
            writeMethod = Introspector.findMethod(cls, writeMethodName, 1, args);
            if (writeMethod != null) {
                if (!writeMethod.getReturnType().equals(void.class)) {
                    writeMethod = null;
                }
            }
            try {
                setWriteMethod(writeMethod);
            } catch (IntrospectionException ex) {
                // fall through
            }
        }
        return writeMethod;
    }

    /**
     * Sets the method that should be used to write the property value.
     *
     * @param writeMethod The new write method.
     * @throws IntrospectionException if the write method is invalid
     * @since 1.2
     */
    public synchronized void setWriteMethod(Method writeMethod)
                                throws IntrospectionException {
        // Set the property type - which validates the method
        setPropertyType(findPropertyType(getReadMethod(), writeMethod));
        setWriteMethod0(writeMethod);
    }

    private void setWriteMethod0(Method writeMethod) {
        this.writeMethodRef.set(writeMethod);
        if (writeMethod == null) {
            writeMethodName = null;
            return;
        }
        setClass0(writeMethod.getDeclaringClass());

        writeMethodName = writeMethod.getName();
        setTransient(writeMethod.getAnnotation(Transient.class));
    }

    /**
     * Overridden to ensure that a super class doesn't take precedent
     */
    void setClass0(Class<?> clz) {
        if (getClass0() != null && clz.isAssignableFrom(getClass0())) {
            // don't replace a subclass with a superclass
            return;
        }
        super.setClass0(clz);
    }

    /**
     * Updates to "bound" properties will cause a "PropertyChange" event to
     * get fired when the property is changed.
     *
     * @return True if this is a bound property.
     */
    public boolean isBound() {
        return bound;
    }

    /**
     * Updates to "bound" properties will cause a "PropertyChange" event to
     * get fired when the property is changed.
     *
     * @param bound True if this is a bound property.
     */
    public void setBound(boolean bound) {
        this.bound = bound;
    }

    /**
     * Attempted updates to "Constrained" properties will cause a "VetoableChange"
     * event to get fired when the property is changed.
     *
     * @return True if this is a constrained property.
     */
    public boolean isConstrained() {
        return constrained;
    }

    /**
     * Attempted updates to "Constrained" properties will cause a "VetoableChange"
     * event to get fired when the property is changed.
     *
     * @param constrained True if this is a constrained property.
     */
    public void setConstrained(boolean constrained) {
        this.constrained = constrained;
    }


    /**
     * Normally PropertyEditors will be found using the PropertyEditorManager.
     * However if for some reason you want to associate a particular
     * PropertyEditor with a given property, then you can do it with
     * this method.
     *
     * @param propertyEditorClass  The Class for the desired PropertyEditor.
     */
    public void setPropertyEditorClass(Class<?> propertyEditorClass) {
        this.propertyEditorClassRef = getWeakReference(propertyEditorClass);
    }

    /**
     * Gets any explicit PropertyEditor Class that has been registered
     * for this property.
     *
     * @return Any explicit PropertyEditor Class that has been registered
     *          for this property.  Normally this will return "null",
     *          indicating that no special editor has been registered,
     *          so the PropertyEditorManager should be used to locate
     *          a suitable PropertyEditor.
     */
    public Class<?> getPropertyEditorClass() {
        return (this.propertyEditorClassRef != null)
                ? this.propertyEditorClassRef.get()
                : null;
    }

    /**
     * Constructs an instance of a property editor using the current
     * property editor class.
     * <p>
     * If the property editor class has a public constructor that takes an
     * Object argument then it will be invoked using the bean parameter
     * as the argument. Otherwise, the default constructor will be invoked.
     *
     * @param bean the source object
     * @return a property editor instance or null if a property editor has
     *         not been defined or cannot be created
     * @since 1.5
     */
    @SuppressWarnings("deprecation")
    public PropertyEditor createPropertyEditor(Object bean) {
        Object editor = null;

        final Class<?> cls = getPropertyEditorClass();
        if (cls != null && PropertyEditor.class.isAssignableFrom(cls)
                && ReflectUtil.isPackageAccessible(cls)) {
            Constructor<?> ctor = null;
            if (bean != null) {
                try {
                    ctor = cls.getConstructor(new Class<?>[] { Object.class });
                } catch (Exception ex) {
                    // Fall through
                }
            }
            try {
                if (ctor == null) {
                    editor = cls.newInstance();
                } else {
                    editor = ctor.newInstance(new Object[] { bean });
                }
            } catch (Exception ex) {
                // Fall through
            }
        }
        return (PropertyEditor)editor;
    }


    /**
     * Compares this {@code PropertyDescriptor} against the specified object.
     * Returns true if the objects are the same. Two {@code PropertyDescriptor}s
     * are the same if the read, write, property types, property editor and
     * flags  are equivalent.
     *
     * @since 1.4
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && obj instanceof PropertyDescriptor) {
            PropertyDescriptor other = (PropertyDescriptor)obj;
            Method otherReadMethod = other.getReadMethod();
            Method otherWriteMethod = other.getWriteMethod();

            if (!compareMethods(getReadMethod(), otherReadMethod)) {
                return false;
            }

            if (!compareMethods(getWriteMethod(), otherWriteMethod)) {
                return false;
            }

            if (getPropertyType() == other.getPropertyType() &&
                getPropertyEditorClass() == other.getPropertyEditorClass() &&
                bound == other.isBound() && constrained == other.isConstrained() &&
                writeMethodName == other.writeMethodName &&
                readMethodName == other.readMethodName) {
                return true;
            }
        }
        return false;
    }

    /**
     * Package private helper method for Descriptor .equals methods.
     *
     * @param a first method to compare
     * @param b second method to compare
     * @return boolean to indicate that the methods are equivalent
     */
    boolean compareMethods(Method a, Method b) {
        // Note: perhaps this should be a protected method in FeatureDescriptor
        if ((a == null) != (b == null)) {
            return false;
        }

        if (a != null && b != null) {
            if (!a.equals(b)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Package-private constructor.
     * Merge two property descriptors.  Where they conflict, give the
     * second argument (y) priority over the first argument (x).
     *
     * @param x  The first (lower priority) PropertyDescriptor
     * @param y  The second (higher priority) PropertyDescriptor
     */
    PropertyDescriptor(PropertyDescriptor x, PropertyDescriptor y) {
        super(x,y);

        if (y.baseName != null) {
            baseName = y.baseName;
        } else {
            baseName = x.baseName;
        }

        if (y.readMethodName != null) {
            readMethodName = y.readMethodName;
        } else {
            readMethodName = x.readMethodName;
        }

        if (y.writeMethodName != null) {
            writeMethodName = y.writeMethodName;
        } else {
            writeMethodName = x.writeMethodName;
        }

        if (y.propertyTypeRef != null) {
            propertyTypeRef = y.propertyTypeRef;
        } else {
            propertyTypeRef = x.propertyTypeRef;
        }

        // Figure out the merged read method.
        Method xr = x.getReadMethod();
        Method yr = y.getReadMethod();

        // Normally give priority to y's readMethod.
        try {
            if (isAssignable(xr, yr)) {
                setReadMethod(yr);
            } else {
                setReadMethod(xr);
            }
        } catch (IntrospectionException ex) {
            // fall through
        }

        // However, if both x and y reference read methods in the same class,
        // give priority to a boolean "is" method over a boolean "get" method.
        if (xr != null && yr != null &&
                   xr.getDeclaringClass() == yr.getDeclaringClass() &&
                   getReturnType(getClass0(), xr) == boolean.class &&
                   getReturnType(getClass0(), yr) == boolean.class &&
                   xr.getName().indexOf(Introspector.IS_PREFIX) == 0 &&
                   yr.getName().indexOf(Introspector.GET_PREFIX) == 0) {
            try {
                setReadMethod(xr);
            } catch (IntrospectionException ex) {
                // fall through
            }
        }

        Method xw = x.getWriteMethod();
        Method yw = y.getWriteMethod();

        try {
            if (yw != null) {
                setWriteMethod(yw);
            } else {
                setWriteMethod(xw);
            }
        } catch (IntrospectionException ex) {
            // Fall through
        }

        if (y.getPropertyEditorClass() != null) {
            setPropertyEditorClass(y.getPropertyEditorClass());
        } else {
            setPropertyEditorClass(x.getPropertyEditorClass());
        }


        bound = x.bound | y.bound;
        constrained = x.constrained | y.constrained;
    }

    /*
     * Package-private dup constructor.
     * This must isolate the new object from any changes to the old object.
     */
    PropertyDescriptor(PropertyDescriptor old) {
        super(old);
        propertyTypeRef = old.propertyTypeRef;
        this.readMethodRef.set(old.readMethodRef.get());
        this.writeMethodRef.set(old.writeMethodRef.get());
        propertyEditorClassRef = old.propertyEditorClassRef;

        writeMethodName = old.writeMethodName;
        readMethodName = old.readMethodName;
        baseName = old.baseName;

        bound = old.bound;
        constrained = old.constrained;
    }

    void updateGenericsFor(Class<?> type) {
        setClass0(type);
        try {
            setPropertyType(findPropertyType(this.readMethodRef.get(), this.writeMethodRef.get()));
        }
        catch (IntrospectionException exception) {
            setPropertyType(null);
        }
    }

    /**
     * Returns the property type that corresponds to the read and write method.
     * The type precedence is given to the readMethod.
     *
     * @return the type of the property descriptor or null if both
     *         read and write methods are null.
     * @throws IntrospectionException if the read or write method is invalid
     */
    private Class<?> findPropertyType(Method readMethod, Method writeMethod)
        throws IntrospectionException {
        Class<?> propertyType = null;
        try {
            if (readMethod != null) {
                Class<?>[] params = getParameterTypes(getClass0(), readMethod);
                if (params.length != 0) {
                    throw new IntrospectionException("bad read method arg count: "
                                                     + readMethod);
                }
                propertyType = getReturnType(getClass0(), readMethod);
                if (propertyType == Void.TYPE) {
                    throw new IntrospectionException("read method " +
                                        readMethod.getName() + " returns void");
                }
            }
            if (writeMethod != null) {
                Class<?>[] params = getParameterTypes(getClass0(), writeMethod);
                if (params.length != 1) {
                    throw new IntrospectionException("bad write method arg count: "
                                                     + writeMethod);
                }
                if (propertyType != null && !params[0].isAssignableFrom(propertyType)) {
                    throw new IntrospectionException("type mismatch between read and write methods");
                }
                propertyType = params[0];
            }
        } catch (IntrospectionException ex) {
            throw ex;
        }
        return propertyType;
    }


    /**
     * Returns a hash code value for the object.
     * See {@link java.lang.Object#hashCode} for a complete description.
     *
     * @return a hash code value for this object.
     * @since 1.5
     */
    public int hashCode() {
        int result = 7;

        result = 37 * result + ((getPropertyType() == null) ? 0 :
                                getPropertyType().hashCode());
        result = 37 * result + ((getReadMethod() == null) ? 0 :
                                getReadMethod().hashCode());
        result = 37 * result + ((getWriteMethod() == null) ? 0 :
                                getWriteMethod().hashCode());
        result = 37 * result + ((getPropertyEditorClass() == null) ? 0 :
                                getPropertyEditorClass().hashCode());
        result = 37 * result + ((writeMethodName == null) ? 0 :
                                writeMethodName.hashCode());
        result = 37 * result + ((readMethodName == null) ? 0 :
                                readMethodName.hashCode());
        result = 37 * result + getName().hashCode();
        result = 37 * result + ((bound == false) ? 0 : 1);
        result = 37 * result + ((constrained == false) ? 0 : 1);

        return result;
    }

    // Calculate once since capitalize() is expensive.
    String getBaseName() {
        if (baseName == null) {
            baseName = NameGenerator.capitalize(getName());
        }
        return baseName;
    }

    void appendTo(StringBuilder sb) {
        appendTo(sb, "bound", this.bound);
        appendTo(sb, "constrained", this.constrained);
        appendTo(sb, "propertyEditorClass", this.propertyEditorClassRef);
        appendTo(sb, "propertyType", this.propertyTypeRef);
        appendTo(sb, "readMethod", this.readMethodRef.get());
        appendTo(sb, "writeMethod", this.writeMethodRef.get());
    }

    boolean isAssignable(Method m1, Method m2) {
        if (m1 == null) {
            return true; // choose second method
        }
        if (m2 == null) {
            return false; // choose first method
        }
        if (!m1.getName().equals(m2.getName())) {
            return true; // choose second method by default
        }
        Class<?> type1 = m1.getDeclaringClass();
        Class<?> type2 = m2.getDeclaringClass();
        if (!type1.isAssignableFrom(type2)) {
            return false; // choose first method: it declared later
        }
        type1 = getReturnType(getClass0(), m1);
        type2 = getReturnType(getClass0(), m2);
        if (!type1.isAssignableFrom(type2)) {
            return false; // choose first method: it overrides return type
        }
        Class<?>[] args1 = getParameterTypes(getClass0(), m1);
        Class<?>[] args2 = getParameterTypes(getClass0(), m2);
        if (args1.length != args2.length) {
            return true; // choose second method by default
        }
        for (int i = 0; i < args1.length; i++) {
            if (!args1[i].isAssignableFrom(args2[i])) {
                return false; // choose first method: it overrides parameter
            }
        }
        return true; // choose second method
    }
}
