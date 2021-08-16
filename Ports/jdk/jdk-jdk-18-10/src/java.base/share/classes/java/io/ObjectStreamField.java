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

import java.lang.reflect.Field;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;
import sun.reflect.misc.ReflectUtil;

/**
 * A description of a Serializable field from a Serializable class.  An array
 * of ObjectStreamFields is used to declare the Serializable fields of a class.
 *
 * @author      Mike Warres
 * @author      Roger Riggs
 * @see ObjectStreamClass
 * @since 1.2
 */
public class ObjectStreamField
    implements Comparable<Object>
{

    /** field name */
    private final String name;
    /** canonical JVM signature of field type, if given */
    private final String signature;
    /** field type (Object.class if unknown non-primitive type) */
    private final Class<?> type;
    /** lazily constructed signature for the type, if no explicit signature */
    private String typeSignature;
    /** whether or not to (de)serialize field values as unshared */
    private final boolean unshared;
    /** corresponding reflective field object, if any */
    private final Field field;
    /** offset of field value in enclosing field group */
    private int offset;

    /**
     * Create a Serializable field with the specified type.  This field should
     * be documented with a {@code serialField} tag.
     *
     * @param   name the name of the serializable field
     * @param   type the {@code Class} object of the serializable field
     */
    public ObjectStreamField(String name, Class<?> type) {
        this(name, type, false);
    }

    /**
     * Creates an ObjectStreamField representing a serializable field with the
     * given name and type.  If unshared is false, values of the represented
     * field are serialized and deserialized in the default manner--if the
     * field is non-primitive, object values are serialized and deserialized as
     * if they had been written and read by calls to writeObject and
     * readObject.  If unshared is true, values of the represented field are
     * serialized and deserialized as if they had been written and read by
     * calls to writeUnshared and readUnshared.
     *
     * @param   name field name
     * @param   type field type
     * @param   unshared if false, write/read field values in the same manner
     *          as writeObject/readObject; if true, write/read in the same
     *          manner as writeUnshared/readUnshared
     * @since   1.4
     */
    public ObjectStreamField(String name, Class<?> type, boolean unshared) {
        if (name == null) {
            throw new NullPointerException();
        }
        this.name = name;
        this.type = type;
        this.unshared = unshared;
        this.field = null;
        this.signature = null;
    }

    /**
     * Creates an ObjectStreamField representing a field with the given name,
     * signature and unshared setting.
     */
    ObjectStreamField(String name, String signature, boolean unshared) {
        if (name == null) {
            throw new NullPointerException();
        }
        this.name = name;
        this.signature = signature.intern();
        this.unshared = unshared;
        this.field = null;

        type = switch (signature.charAt(0)) {
            case 'Z'      -> Boolean.TYPE;
            case 'B'      -> Byte.TYPE;
            case 'C'      -> Character.TYPE;
            case 'S'      -> Short.TYPE;
            case 'I'      -> Integer.TYPE;
            case 'J'      -> Long.TYPE;
            case 'F'      -> Float.TYPE;
            case 'D'      -> Double.TYPE;
            case 'L', '[' -> Object.class;
            default       -> throw new IllegalArgumentException("illegal signature");
        };
    }

    /**
     * Returns JVM type signature for given primitive.
     */
    private static String getPrimitiveSignature(Class<?> cl) {
        if (cl == Integer.TYPE)
            return "I";
        else if (cl == Byte.TYPE)
            return "B";
        else if (cl == Long.TYPE)
            return "J";
        else if (cl == Float.TYPE)
            return "F";
        else if (cl == Double.TYPE)
            return "D";
        else if (cl == Short.TYPE)
            return "S";
        else if (cl == Character.TYPE)
            return "C";
        else if (cl == Boolean.TYPE)
            return "Z";
        else if (cl == Void.TYPE)
            return "V";
        else
            throw new InternalError();
    }

    /**
     * Returns JVM type signature for given class.
     */
    static String getClassSignature(Class<?> cl) {
        if (cl.isPrimitive()) {
            return getPrimitiveSignature(cl);
        } else {
            return appendClassSignature(new StringBuilder(), cl).toString();
        }
    }

    static StringBuilder appendClassSignature(StringBuilder sbuf, Class<?> cl) {
        while (cl.isArray()) {
            sbuf.append('[');
            cl = cl.getComponentType();
        }

        if (cl.isPrimitive()) {
            sbuf.append(getPrimitiveSignature(cl));
        } else {
            sbuf.append('L').append(cl.getName().replace('.', '/')).append(';');
        }

        return sbuf;
    }

    /**
     * Creates an ObjectStreamField representing the given field with the
     * specified unshared setting.  For compatibility with the behavior of
     * earlier serialization implementations, a "showType" parameter is
     * necessary to govern whether or not a getType() call on this
     * ObjectStreamField (if non-primitive) will return Object.class (as
     * opposed to a more specific reference type).
     */
    ObjectStreamField(Field field, boolean unshared, boolean showType) {
        this.field = field;
        this.unshared = unshared;
        name = field.getName();
        Class<?> ftype = field.getType();
        type = (showType || ftype.isPrimitive()) ? ftype : Object.class;
        signature = getClassSignature(ftype).intern();
    }

    /**
     * Get the name of this field.
     *
     * @return  a {@code String} representing the name of the serializable
     *          field
     */
    public String getName() {
        return name;
    }

    /**
     * Get the type of the field.  If the type is non-primitive and this
     * {@code ObjectStreamField} was obtained from a deserialized {@link
     * ObjectStreamClass} instance, then {@code Object.class} is returned.
     * Otherwise, the {@code Class} object for the type of the field is
     * returned.
     *
     * @return  a {@code Class} object representing the type of the
     *          serializable field
     */
    @SuppressWarnings("removal")
    @CallerSensitive
    public Class<?> getType() {
        if (System.getSecurityManager() != null) {
            Class<?> caller = Reflection.getCallerClass();
            if (ReflectUtil.needsPackageAccessCheck(caller.getClassLoader(), type.getClassLoader())) {
                ReflectUtil.checkPackageAccess(type);
            }
        }
        return type;
    }

    /**
     * Returns character encoding of field type.  The encoding is as follows:
     * <blockquote><pre>
     * B            byte
     * C            char
     * D            double
     * F            float
     * I            int
     * J            long
     * L            class or interface
     * S            short
     * Z            boolean
     * [            array
     * </pre></blockquote>
     *
     * @return  the typecode of the serializable field
     */
    // REMIND: deprecate?
    public char getTypeCode() {
        return getSignature().charAt(0);
    }

    /**
     * Return the JVM type signature.
     *
     * @return  null if this field has a primitive type.
     */
    // REMIND: deprecate?
    public String getTypeString() {
        return isPrimitive() ? null : getSignature();
    }

    /**
     * Offset of field within instance data.
     *
     * @return  the offset of this field
     * @see #setOffset
     */
    // REMIND: deprecate?
    public int getOffset() {
        return offset;
    }

    /**
     * Offset within instance data.
     *
     * @param   offset the offset of the field
     * @see #getOffset
     */
    // REMIND: deprecate?
    protected void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     * Return true if this field has a primitive type.
     *
     * @return  true if and only if this field corresponds to a primitive type
     */
    // REMIND: deprecate?
    public boolean isPrimitive() {
        char tcode = getTypeCode();
        return ((tcode != 'L') && (tcode != '['));
    }

    /**
     * Returns boolean value indicating whether or not the serializable field
     * represented by this ObjectStreamField instance is unshared.
     *
     * @return {@code true} if this field is unshared
     *
     * @since 1.4
     */
    public boolean isUnshared() {
        return unshared;
    }

    /**
     * Compare this field with another {@code ObjectStreamField}.  Return
     * -1 if this is smaller, 0 if equal, 1 if greater.  Types that are
     * primitives are "smaller" than object types.  If equal, the field names
     * are compared.
     */
    // REMIND: deprecate?
    public int compareTo(Object obj) {
        ObjectStreamField other = (ObjectStreamField) obj;
        boolean isPrim = isPrimitive();
        if (isPrim != other.isPrimitive()) {
            return isPrim ? -1 : 1;
        }
        return name.compareTo(other.name);
    }

    /**
     * Return a string that describes this field.
     */
    public String toString() {
        return getSignature() + ' ' + name;
    }

    /**
     * Returns field represented by this ObjectStreamField, or null if
     * ObjectStreamField is not associated with an actual field.
     */
    Field getField() {
        return field;
    }

    /**
     * Returns JVM type signature of field (similar to getTypeString, except
     * that signature strings are returned for primitive fields as well).
     */
    String getSignature() {
        if (signature != null) {
            return signature;
        }

        String sig = typeSignature;
        // This lazy calculation is safe since signature can be null iff one
        // of the public constructors are used, in which case type is always
        // initialized to the exact type we want the signature to represent.
        if (sig == null) {
            typeSignature = sig = getClassSignature(type).intern();
        }
        return sig;
    }
}
