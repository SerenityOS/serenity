/*
 * Copyright (c) 1996, 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.beans.TypeResolver;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.ref.SoftReference;

import java.lang.reflect.Method;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map.Entry;

/**
 * The FeatureDescriptor class is the common baseclass for PropertyDescriptor,
 * EventSetDescriptor, and MethodDescriptor, etc.
 * <p>
 * It supports some common information that can be set and retrieved for
 * any of the introspection descriptors.
 * <p>
 * In addition it provides an extension mechanism so that arbitrary
 * attribute/value pairs can be associated with a design feature.
 *
 * @since 1.1
 */

public class FeatureDescriptor {
    private static final String TRANSIENT = "transient";

    private Reference<? extends Class<?>> classRef;

    /**
     * Constructs a {@code FeatureDescriptor}.
     */
    public FeatureDescriptor() {
    }

    /**
     * Gets the programmatic name of this feature.
     *
     * @return The programmatic name of the property/method/event
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the programmatic name of this feature.
     *
     * @param name  The programmatic name of the property/method/event
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the localized display name of this feature.
     *
     * @return The localized display name for the property/method/event.
     *  This defaults to the same as its programmatic name from getName.
     */
    public String getDisplayName() {
        if (displayName == null) {
            return getName();
        }
        return displayName;
    }

    /**
     * Sets the localized display name of this feature.
     *
     * @param displayName  The localized display name for the
     *          property/method/event.
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * The "expert" flag is used to distinguish between those features that are
     * intended for expert users from those that are intended for normal users.
     *
     * @return True if this feature is intended for use by experts only.
     */
    public boolean isExpert() {
        return expert;
    }

    /**
     * The "expert" flag is used to distinguish between features that are
     * intended for expert users from those that are intended for normal users.
     *
     * @param expert True if this feature is intended for use by experts only.
     */
    public void setExpert(boolean expert) {
        this.expert = expert;
    }

    /**
     * The "hidden" flag is used to identify features that are intended only
     * for tool use, and which should not be exposed to humans.
     *
     * @return True if this feature should be hidden from human users.
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * The "hidden" flag is used to identify features that are intended only
     * for tool use, and which should not be exposed to humans.
     *
     * @param hidden  True if this feature should be hidden from human users.
     */
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    /**
     * The "preferred" flag is used to identify features that are particularly
     * important for presenting to humans.
     *
     * @return True if this feature should be preferentially shown to human users.
     * @since 1.2
     */
    public boolean isPreferred() {
        return preferred;
    }

    /**
     * The "preferred" flag is used to identify features that are particularly
     * important for presenting to humans.
     *
     * @param preferred  True if this feature should be preferentially shown
     *                   to human users.
     * @since 1.2
     */
    public void setPreferred(boolean preferred) {
        this.preferred = preferred;
    }

    /**
     * Gets the short description of this feature.
     *
     * @return  A localized short description associated with this
     *   property/method/event.  This defaults to be the display name.
     */
    public String getShortDescription() {
        if (shortDescription == null) {
            return getDisplayName();
        }
        return shortDescription;
    }

    /**
     * You can associate a short descriptive string with a feature.  Normally
     * these descriptive strings should be less than about 40 characters.
     * @param text  A (localized) short description to be associated with
     * this property/method/event.
     */
    public void setShortDescription(String text) {
        shortDescription = text;
    }

    /**
     * Associate a named attribute with this feature.
     *
     * @param attributeName  The locale-independent name of the attribute
     * @param value  The value.
     */
    public void setValue(String attributeName, Object value) {
        getTable().put(attributeName, value);
    }

    /**
     * Retrieve a named attribute with this feature.
     *
     * @param attributeName  The locale-independent name of the attribute
     * @return  The value of the attribute.  May be null if
     *     the attribute is unknown.
     */
    public Object getValue(String attributeName) {
        return (this.table != null)
                ? this.table.get(attributeName)
                : null;
    }

    /**
     * Gets an enumeration of the locale-independent names of this
     * feature.
     *
     * @return  An enumeration of the locale-independent names of any
     *    attributes that have been registered with setValue.
     */
    public Enumeration<String> attributeNames() {
        return getTable().keys();
    }

    /**
     * Package-private constructor,
     * Merge information from two FeatureDescriptors.
     * The merged hidden and expert flags are formed by or-ing the values.
     * In the event of other conflicts, the second argument (y) is
     * given priority over the first argument (x).
     *
     * @param x  The first (lower priority) MethodDescriptor
     * @param y  The second (higher priority) MethodDescriptor
     */
    FeatureDescriptor(FeatureDescriptor x, FeatureDescriptor y) {
        expert = x.expert | y.expert;
        hidden = x.hidden | y.hidden;
        preferred = x.preferred | y.preferred;
        name = y.name;
        shortDescription = x.shortDescription;
        if (y.shortDescription != null) {
            shortDescription = y.shortDescription;
        }
        displayName = x.displayName;
        if (y.displayName != null) {
            displayName = y.displayName;
        }
        classRef = x.classRef;
        if (y.classRef != null) {
            classRef = y.classRef;
        }
        addTable(x.table);
        addTable(y.table);
    }

    /*
     * Package-private dup constructor
     * This must isolate the new object from any changes to the old object.
     */
    FeatureDescriptor(FeatureDescriptor old) {
        expert = old.expert;
        hidden = old.hidden;
        preferred = old.preferred;
        name = old.name;
        shortDescription = old.shortDescription;
        displayName = old.displayName;
        classRef = old.classRef;

        addTable(old.table);
    }

    /**
     * Copies all values from the specified attribute table.
     * If some attribute is exist its value should be overridden.
     *
     * @param table  the attribute table with new values
     */
    private void addTable(Hashtable<String, Object> table) {
        if ((table != null) && !table.isEmpty()) {
            getTable().putAll(table);
        }
    }

    /**
     * Returns the initialized attribute table.
     *
     * @return the initialized attribute table
     */
    private Hashtable<String, Object> getTable() {
        if (this.table == null) {
            this.table = new Hashtable<>();
        }
        return this.table;
    }

    /**
     * Sets the "transient" attribute according to the annotation.
     * If the "transient" attribute is already set
     * it should not be changed.
     *
     * @param annotation  the annotation of the element of the feature
     */
    void setTransient(Transient annotation) {
        if ((annotation != null) && (null == getValue(TRANSIENT))) {
            setValue(TRANSIENT, annotation.value());
        }
    }

    /**
     * Indicates whether the feature is transient.
     *
     * @return {@code true} if the feature is transient,
     *         {@code false} otherwise
     */
    boolean isTransient() {
        Object value = getValue(TRANSIENT);
        return (value instanceof Boolean)
                ? (Boolean) value
                : false;
    }

    // Package private methods for recreating the weak/soft referent

    void setClass0(Class<?> cls) {
        this.classRef = getWeakReference(cls);
    }

    Class<?> getClass0() {
        return (this.classRef != null)
                ? this.classRef.get()
                : null;
    }

    /**
     * Creates a new soft reference that refers to the given object.
     *
     * @return a new soft reference or {@code null} if object is {@code null}
     *
     * @see SoftReference
     */
    static <T> Reference<T> getSoftReference(T object) {
        return (object != null)
                ? new SoftReference<>(object)
                : null;
    }

    /**
     * Creates a new weak reference that refers to the given object.
     *
     * @return a new weak reference or {@code null} if object is {@code null}
     *
     * @see WeakReference
     */
    static <T> Reference<T> getWeakReference(T object) {
        return (object != null)
                ? new WeakReference<>(object)
                : null;
    }

    /**
     * Resolves the return type of the method.
     *
     * @param base    the class that contains the method in the hierarchy
     * @param method  the object that represents the method
     * @return a class identifying the return type of the method
     *
     * @see Method#getGenericReturnType
     * @see Method#getReturnType
     */
    static Class<?> getReturnType(Class<?> base, Method method) {
        if (base == null) {
            base = method.getDeclaringClass();
        }
        return TypeResolver.erase(TypeResolver.resolveInClass(base, method.getGenericReturnType()));
    }

    /**
     * Resolves the parameter types of the method.
     *
     * @param base    the class that contains the method in the hierarchy
     * @param method  the object that represents the method
     * @return an array of classes identifying the parameter types of the method
     *
     * @see Method#getGenericParameterTypes
     * @see Method#getParameterTypes
     */
    static Class<?>[] getParameterTypes(Class<?> base, Method method) {
        if (base == null) {
            base = method.getDeclaringClass();
        }
        return TypeResolver.erase(TypeResolver.resolveInClass(base, method.getGenericParameterTypes()));
    }

    private boolean expert;
    private boolean hidden;
    private boolean preferred;
    private String shortDescription;
    private String name;
    private String displayName;
    private Hashtable<String, Object> table;

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object
     *
     * @since 1.7
     */
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getName());
        sb.append("[name=").append(this.name);
        appendTo(sb, "displayName", this.displayName);
        appendTo(sb, "shortDescription", this.shortDescription);
        appendTo(sb, "preferred", this.preferred);
        appendTo(sb, "hidden", this.hidden);
        appendTo(sb, "expert", this.expert);
        if ((this.table != null) && !this.table.isEmpty()) {
            sb.append("; values={");
            for (Entry<String, Object> entry : this.table.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("; ");
            }
            sb.setLength(sb.length() - 2);
            sb.append("}");
        }
        appendTo(sb);
        return sb.append("]").toString();
    }

    void appendTo(StringBuilder sb) {
    }

    static void appendTo(StringBuilder sb, String name, Reference<?> reference) {
        if (reference != null) {
            appendTo(sb, name, reference.get());
        }
    }

    static void appendTo(StringBuilder sb, String name, Object value) {
        if (value != null) {
            sb.append("; ").append(name).append("=").append(value);
        }
    }

    static void appendTo(StringBuilder sb, String name, boolean value) {
        if (value) {
            sb.append("; ").append(name);
        }
    }
}
