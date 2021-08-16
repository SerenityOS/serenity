/*
 * Copyright (c) 2004, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.management;
import java.lang.management.MemoryUsage;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.LockInfo;
import java.lang.management.ThreadInfo;
import java.lang.reflect.*;
import java.util.List;
import java.util.Map;
import java.util.*;
import java.io.InvalidObjectException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import javax.management.openmbean.*;
import static javax.management.openmbean.SimpleType.*;

/**
 * A mapped mxbean type maps a Java type to an open type.
 * Only the following Java types are mappable
 * (currently required by the platform MXBeans):
 * <ol>
 *   <li>Primitive types</li>
 *   <li>Wrapper classes such java.lang.Integer, etc</li>
 *   <li>Classes with only getter methods and with a static "from" method
 *      that takes a CompositeData argument.</li>
 *   <li>{@code E[]} where {@code E} is a type of 1-4 (can be multi-dimensional array)</li>
 *   <li>{@code List<E>} where E is a type of 1-3</li>
 *   <li>{@code Map<K, V>} where {@code K} and {@code V} are a type of 1-4</li>
 * </ol>
 *
 * OpenDataException will be thrown if a Java type is not supported.
 */
// Suppress unchecked cast warnings at line 442, 523 and 546
// Suppress unchecked calls at line 235, 284, 380 and 430.
@SuppressWarnings("unchecked")
public abstract class MappedMXBeanType {
    private static final WeakHashMap<Type,MappedMXBeanType> convertedTypes =
        new WeakHashMap<>();

    boolean  isBasicType = false;
    OpenType<?> openType = inProgress;
    Class<?>    mappedTypeClass;

    static synchronized MappedMXBeanType newMappedType(Type javaType)
            throws OpenDataException {

        MappedMXBeanType mt = null;
        if (javaType instanceof Class) {
            final Class<?> c = (Class<?>) javaType;
            if (c.isEnum()) {
                mt = new EnumMXBeanType(c);
            } else if (c.isArray()) {
                mt = new ArrayMXBeanType(c);
            } else {
                mt = new CompositeDataMXBeanType(c);
            }
        } else if (javaType instanceof ParameterizedType) {
            final ParameterizedType pt = (ParameterizedType) javaType;
            final Type rawType = pt.getRawType();
            if (rawType instanceof Class) {
                final Class<?> rc = (Class<?>) rawType;
                if (rc == List.class) {
                    mt = new ListMXBeanType(pt);
                } else if (rc == Map.class) {
                    mt = new MapMXBeanType(pt);
                }
            }
        } else if (javaType instanceof GenericArrayType) {
           final GenericArrayType t = (GenericArrayType) javaType;
           mt = new GenericArrayMXBeanType(t);
        }
        // No open type mapped for the javaType
        if (mt == null) {
            throw new OpenDataException(javaType +
                " is not a supported MXBean type.");
        }
        convertedTypes.put(javaType, mt);
        return mt;
    }

    // basic types do not require data mapping
    static synchronized MappedMXBeanType newBasicType(Class<?> c, OpenType<?> ot)
            throws OpenDataException {
        MappedMXBeanType mt = new BasicMXBeanType(c, ot);
        convertedTypes.put(c, mt);
        return mt;
    }

    public static synchronized MappedMXBeanType getMappedType(Type t)
            throws OpenDataException {
        MappedMXBeanType mt = convertedTypes.get(t);
        if (mt == null) {
            mt = newMappedType(t);
        }

        if (mt.getOpenType() instanceof InProgress) {
            throw new OpenDataException("Recursive data structure");
        }
        return mt;
    }

    // Convert a class to an OpenType
    public static synchronized OpenType<?> toOpenType(Type t)
            throws OpenDataException {
        MappedMXBeanType mt = getMappedType(t);
        return mt.getOpenType();
    }

    public static Object toJavaTypeData(Object openData, Type t)
            throws OpenDataException, InvalidObjectException {
        if (openData == null) {
            return null;
        }
        MappedMXBeanType mt = getMappedType(t);
        return mt.toJavaTypeData(openData);
    }

    public static Object toOpenTypeData(Object data, Type t)
            throws OpenDataException {
        if (data == null) {
            return null;
        }
        MappedMXBeanType mt = getMappedType(t);
        return mt.toOpenTypeData(data);
    }

    // Return the mapped open type
    public OpenType<?> getOpenType() {
        return openType;
    }

    boolean isBasicType() {
        return isBasicType;
    }

    // Return the type name of the mapped open type
    // For primitive types, the type name is the same as the javaType
    // but the mapped open type is the wrapper class
    String getTypeName() {
        return getMappedTypeClass().getName();
    }

    // Return the mapped open type
    Class<?> getMappedTypeClass() {
        return mappedTypeClass;
    }

    abstract Type getJavaType();

    // return name of the class or the generic type
    abstract String getName();

    public abstract Object toOpenTypeData(Object javaTypeData)
        throws OpenDataException;

    public abstract Object toJavaTypeData(Object openTypeData)
        throws OpenDataException, InvalidObjectException;

    // Basic Types - Classes that do not require data conversion
    //               including primitive types and all SimpleType
    //
    //   Mapped open type: SimpleType for corresponding basic type
    //
    // Data Mapping:
    //   T <-> T (no conversion)
    //
    static class BasicMXBeanType extends MappedMXBeanType {
        final Class<?> basicType;
        BasicMXBeanType(Class<?> c, OpenType<?> openType) {
            this.basicType = c;
            this.openType = openType;
            this.mappedTypeClass = c;
            this.isBasicType = true;
        }

        Type getJavaType() {
            return basicType;
        }

        String getName() {
            return basicType.getName();
        }

        public Object toOpenTypeData(Object data) throws OpenDataException {
            return data;
        }

        public Object toJavaTypeData(Object data)
            throws OpenDataException, InvalidObjectException {

            return data;
        }
    }


    // Enum subclasses
    //   Mapped open type - String
    //
    // Data Mapping:
    //   Enum <-> enum's name
    //
    static class EnumMXBeanType extends MappedMXBeanType {
        @SuppressWarnings("rawtypes")
        final Class enumClass;
        EnumMXBeanType(Class<?> c) {
            this.enumClass = c;
            this.openType = STRING;
            this.mappedTypeClass = String.class;
        }

        Type getJavaType() {
            return enumClass;
        }

        String getName() {
            return enumClass.getName();
        }

        public Object toOpenTypeData(Object data) throws OpenDataException {
            return ((Enum) data).name();
        }

        public Object toJavaTypeData(Object data)
            throws OpenDataException, InvalidObjectException {

            try {
                return Enum.valueOf(enumClass, (String) data);
            } catch (IllegalArgumentException e) {
                // missing enum constants
                final InvalidObjectException ioe =
                    new InvalidObjectException("Enum constant named " +
                    (String) data + " is missing");
                ioe.initCause(e);
                throw ioe;
            }
        }
    }

    // Array E[]
    //   Mapped open type - Array with element of OpenType for E
    //
    // Data Mapping:
    //   E[] <-> openTypeData(E)[]
    //
    static class ArrayMXBeanType extends MappedMXBeanType {
        final Class<?> arrayClass;
        protected MappedMXBeanType componentType;
        protected MappedMXBeanType baseElementType;

        ArrayMXBeanType(Class<?> c) throws OpenDataException {
            this.arrayClass = c;
            this.componentType = getMappedType(c.getComponentType());

            StringBuilder className = new StringBuilder();
            Class<?> et = c;
            int dim;
            for (dim = 0; et.isArray(); dim++) {
                className.append('[');
                et = et.getComponentType();
            }
            baseElementType = getMappedType(et);
            if (et.isPrimitive()) {
                className = new StringBuilder(c.getName());
            } else {
                className.append('L').append(baseElementType.getTypeName()).append(';');
            }
            try {
                mappedTypeClass = Class.forName(className.toString());
            } catch (ClassNotFoundException e) {
                final OpenDataException ode =
                    new OpenDataException("Cannot obtain array class");
                ode.initCause(e);
                throw ode;
            }

            openType = new ArrayType<>(dim, baseElementType.getOpenType());
        }

        protected ArrayMXBeanType() {
            arrayClass = null;
        };

        Type getJavaType() {
            return arrayClass;
        }

        String getName() {
            return arrayClass.getName();
        }

        public Object toOpenTypeData(Object data) throws OpenDataException {
            // If the base element type is a basic type
            // return the data as no conversion is needed.
            // Primitive types are not converted to wrappers.
            if (baseElementType.isBasicType()) {
                return data;
            }

            final Object[] array = (Object[]) data;
            final Object[] openArray = (Object[])
                Array.newInstance(componentType.getMappedTypeClass(),
                                  array.length);
            int i = 0;
            for (Object o : array) {
                if (o == null) {
                    openArray[i] = null;
                } else {
                    openArray[i] = componentType.toOpenTypeData(o);
                }
                i++;
            }
            return openArray;
        }


        public Object toJavaTypeData(Object data)
            throws OpenDataException, InvalidObjectException {

            // If the base element type is a basic type
            // return the data as no conversion is needed.
            if (baseElementType.isBasicType()) {
                return data;
            }

            final Object[] openArray = (Object[]) data;
            final Object[] array = (Object[])
                Array.newInstance((Class) componentType.getJavaType(),
                                  openArray.length);
            int i = 0;
            for (Object o : openArray) {
                if (o == null) {
                    array[i] = null;
                } else {
                    array[i] = componentType.toJavaTypeData(o);
                }
                i++;
            }
            return array;
        }

    }

    static class GenericArrayMXBeanType extends ArrayMXBeanType {
        final GenericArrayType gtype;
        GenericArrayMXBeanType(GenericArrayType gat) throws OpenDataException {
            this.gtype = gat;
            this.componentType = getMappedType(gat.getGenericComponentType());

            StringBuilder className = new StringBuilder();
            Type elementType = gat;
            int dim;
            for (dim = 0; elementType instanceof GenericArrayType; dim++) {
                className.append('[');
                GenericArrayType et = (GenericArrayType) elementType;
                elementType = et.getGenericComponentType();
            }
            baseElementType = getMappedType(elementType);
            if (elementType instanceof Class && ((Class) elementType).isPrimitive()) {
                className = new StringBuilder(gat.toString());
            } else {
                className.append('L').append(baseElementType.getTypeName()).append(';');
            }
            try {
                mappedTypeClass = Class.forName(className.toString());
            } catch (ClassNotFoundException e) {
                final OpenDataException ode =
                    new OpenDataException("Cannot obtain array class");
                ode.initCause(e);
                throw ode;
            }

            openType = new ArrayType<>(dim, baseElementType.getOpenType());
        }

        Type getJavaType() {
            return gtype;
        }

        String getName() {
            return gtype.toString();
        }
    }

    // List<E>
    //   Mapped open type - Array with element of OpenType for E
    //
    // Data Mapping:
    //   List<E> <-> openTypeData(E)[]
    //
    static class ListMXBeanType extends MappedMXBeanType {
        final ParameterizedType javaType;
        final MappedMXBeanType paramType;
        final String typeName;

        ListMXBeanType(ParameterizedType pt) throws OpenDataException {
            this.javaType = pt;

            final Type[] argTypes = pt.getActualTypeArguments();
            assert(argTypes.length == 1);

            if (!(argTypes[0] instanceof Class)) {
                throw new OpenDataException("Element Type for " + pt +
                   " not supported");
            }
            final Class<?> et = (Class<?>) argTypes[0];
            if (et.isArray()) {
                throw new OpenDataException("Element Type for " + pt +
                   " not supported");
            }
            paramType = getMappedType(et);
            typeName = "List<" + paramType.getName() + ">";

            try {
                mappedTypeClass = Class.forName(
                    "[L" + paramType.getTypeName() + ";");
            } catch (ClassNotFoundException e) {
                final OpenDataException ode =
                    new OpenDataException("Array class not found");
                ode.initCause(e);
                throw ode;
            }
            openType = new ArrayType<>(1, paramType.getOpenType());
        }

        Type getJavaType() {
            return javaType;
        }

        String getName() {
            return typeName;
        }

        public Object toOpenTypeData(Object data) throws OpenDataException {
            final List<Object> list = (List<Object>) data;

            final Object[] openArray = (Object[])
                Array.newInstance(paramType.getMappedTypeClass(),
                                  list.size());
            int i = 0;
            for (Object o : list) {
                openArray[i++] = paramType.toOpenTypeData(o);
            }
            return openArray;
        }

        public Object toJavaTypeData(Object data)
            throws OpenDataException, InvalidObjectException {

            final Object[] openArray = (Object[]) data;
            List<Object> result = new ArrayList<>(openArray.length);
            for (Object o : openArray) {
                result.add(paramType.toJavaTypeData(o));
            }
            return result;
        }
    }

    private static final String KEY   = "key";
    private static final String VALUE = "value";
    private static final String[] mapIndexNames = {KEY};
    private static final String[] mapItemNames = {KEY, VALUE};

    // Map<K,V>
    //   Mapped open type - TabularType with row type:
    //                        CompositeType:
    //                          "key"   of openDataType(K)
    //                          "value" of openDataType(V)
    //                        "key" is the index name
    //
    // Data Mapping:
    //   Map<K,V> <-> TabularData
    //
    static class MapMXBeanType extends MappedMXBeanType {
        final ParameterizedType javaType;
        final MappedMXBeanType keyType;
        final MappedMXBeanType valueType;
        final String typeName;

        MapMXBeanType(ParameterizedType pt) throws OpenDataException {
            this.javaType = pt;

            final Type[] argTypes = pt.getActualTypeArguments();
            assert(argTypes.length == 2);
            this.keyType = getMappedType(argTypes[0]);
            this.valueType = getMappedType(argTypes[1]);


            // FIXME: generate typeName for generic
            typeName = "Map<" + keyType.getName() + "," +
                                valueType.getName() + ">";
            final OpenType<?>[] mapItemTypes = new OpenType<?>[] {
                                                keyType.getOpenType(),
                                                valueType.getOpenType(),
                                            };
            final CompositeType rowType =
                new CompositeType(typeName,
                                  typeName,
                                  mapItemNames,
                                  mapItemNames,
                                  mapItemTypes);

            openType = new TabularType(typeName, typeName, rowType, mapIndexNames);
            mappedTypeClass = javax.management.openmbean.TabularData.class;
        }

        Type getJavaType() {
            return javaType;
        }

        String getName() {
            return typeName;
        }

        public Object toOpenTypeData(Object data) throws OpenDataException {
            final Map<Object,Object> map = (Map<Object,Object>) data;
            final TabularType tabularType = (TabularType) openType;
            final TabularData table = new TabularDataSupport(tabularType);
            final CompositeType rowType = tabularType.getRowType();

            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                final Object key = keyType.toOpenTypeData(entry.getKey());
                final Object value = valueType.toOpenTypeData(entry.getValue());
                final CompositeData row =
                    new CompositeDataSupport(rowType,
                                             mapItemNames,
                                             new Object[] {key, value});
                table.put(row);
            }
            return table;
        }

        public Object toJavaTypeData(Object data)
            throws OpenDataException, InvalidObjectException {

            final TabularData td = (TabularData) data;

            Map<Object, Object> result = new HashMap<>();
            for (CompositeData row : (Collection<CompositeData>) td.values()) {
                Object key = keyType.toJavaTypeData(row.get(KEY));
                Object value = valueType.toJavaTypeData(row.get(VALUE));
                result.put(key, value);
            }
            return result;
        }
    }

    private static final Class<?> COMPOSITE_DATA_CLASS =
        javax.management.openmbean.CompositeData.class;

    // Classes that have a static from method
    //   Mapped open type - CompositeData
    //
    // Data Mapping:
    //   Classes <-> CompositeData
    //
    // The name and type of items for a class are identified from
    // the getter methods. For example, a class defines a method:
    //
    //    public FooType getFoo();
    //
    // The composite data view for this class will contain one
    // item entry for a "foo" attribute and the item type is
    // one of the open types defined in the OpenType class that
    // can be determined in the following manner:
    // o If FooType is a primitive type, the item type a wrapper
    //   class for the corresponding primitive type (such as
    //   Integer, Long, Boolean, etc).
    // o If FooType is of type CompositeData or TabularData,
    //   the item type is FooType.
    // o If FooType is an Enum, the item type is a String and
    //   the value is the name of the enum constant.
    // o If FooType is a class or an interface other than the above,
    //   the item type is CompositeData. The same convention
    //   can be recursively applied to the FooType class when
    //   constructing the composite data for the "foo" attribute.
    // o If FooType is an array, the item type is an array and
    //   its element type is determined as described above.
    //
    static class CompositeDataMXBeanType extends MappedMXBeanType {
        final Class<?> javaClass;
        boolean isCompositeData = false;
        Method fromMethod = null;
        Method toMethod = null;

        @SuppressWarnings("removal")
        CompositeDataMXBeanType(Class<?> c) throws OpenDataException {
            this.javaClass = c;
            this.mappedTypeClass = COMPOSITE_DATA_CLASS;

            // check if a static from method exists
            try {
                fromMethod = AccessController.doPrivileged(new PrivilegedExceptionAction<Method>() {
                        public Method run() throws NoSuchMethodException {
                            return javaClass.getMethod("from", COMPOSITE_DATA_CLASS);
                        }
                    });
            } catch (PrivilegedActionException e) {
                // ignore NoSuchMethodException since we allow classes
                // that has no from method to be embeded in another class.
            }

            if (COMPOSITE_DATA_CLASS.isAssignableFrom(c)) {
                // c implements CompositeData - set openType to null
                // defer generating the CompositeType
                // until the object is constructed
                this.isCompositeData = true;
                this.openType = null;
            } else {
                this.isCompositeData = false;

                // Make a CompositeData containing all the getters
                final Method[] methods =
                    AccessController.doPrivileged(new PrivilegedAction<Method[]>() {
                            public Method[] run() {
                                return javaClass.getMethods();
                            }
                        });
                final List<String> names = new ArrayList<>();
                final List<OpenType<?>> types = new ArrayList<>();

                /* Select public methods that look like "T getX()" or "boolean
                 isX()", where T is not void and X is not the empty
                 string.  Exclude "Class getClass()" inherited from Object.  */
                for (int i = 0; i < methods.length; i++) {
                    final Method method = methods[i];
                    final String name = method.getName();
                    final Type type = method.getGenericReturnType();
                    final String rest;
                    if (name.startsWith("get")) {
                        rest = name.substring(3);
                    } else if (name.startsWith("is") &&
                               type instanceof Class &&
                               ((Class) type) == boolean.class) {
                        rest = name.substring(2);
                    } else {
                        // ignore non-getter methods
                        continue;
                    }

                    if (rest.isEmpty() ||
                        method.getParameterTypes().length > 0 ||
                        type == void.class ||
                        rest.equals("Class")) {

                        // ignore non-getter methods
                        continue;
                    }
                    names.add(decapitalize(rest));
                    types.add(toOpenType(type));
                }

                final String[] nameArray = names.toArray(new String[0]);
                openType = new CompositeType(c.getName(),
                        c.getName(),
                        nameArray, // field names
                        nameArray, // field descriptions
                        types.toArray(new OpenType<?>[0]));
            }
        }

        Type getJavaType() {
            return javaClass;
        }

        String getName() {
            return javaClass.getName();
        }

        public Object toOpenTypeData(Object data) throws OpenDataException {
            if (toMethod != null) {
                try {
                    return toMethod.invoke(null, data);
                } catch (IllegalAccessException e) {
                    // should never reach here
                    throw new AssertionError(e);
                } catch (InvocationTargetException e) {
                    final OpenDataException ode
                            = new OpenDataException("Failed to invoke "
                                    + toMethod.getName() + " to convert " + javaClass.getName()
                                    + " to CompositeData");
                    ode.initCause(e);
                    throw ode;
                }
            }

            if (data instanceof MemoryUsage) {
                return MemoryUsageCompositeData.toCompositeData((MemoryUsage) data);
            }

            if (data instanceof ThreadInfo) {
                return ThreadInfoCompositeData.toCompositeData((ThreadInfo) data);
            }

            if (data instanceof LockInfo) {
                if (data instanceof java.lang.management.MonitorInfo) {
                    return MonitorInfoCompositeData.toCompositeData((MonitorInfo) data);
                }
                return LockInfoCompositeData.toCompositeData((LockInfo) data);
            }

            if (data instanceof MemoryNotificationInfo) {
                return MemoryNotifInfoCompositeData.
                    toCompositeData((MemoryNotificationInfo) data);
            }

            if (isCompositeData) {
                // Classes that implement CompositeData
                //
                // construct a new CompositeDataSupport object
                // so that no other classes are sent over the wire
                CompositeData cd = (CompositeData) data;
                CompositeType ct = cd.getCompositeType();
                String[] itemNames = ct.keySet().toArray(new String[0]);
                Object[] itemValues = cd.getAll(itemNames);
                return new CompositeDataSupport(ct, itemNames, itemValues);
            }

            throw new OpenDataException(javaClass.getName() +
                " is not supported for platform MXBeans");
        }

        public Object toJavaTypeData(Object data)
            throws OpenDataException, InvalidObjectException {

            if (fromMethod == null) {
                throw new AssertionError("Does not support data conversion");
            }

            try {
                return fromMethod.invoke(null, data);
            } catch (IllegalAccessException e) {
                // should never reach here
                throw new AssertionError(e);
            } catch (InvocationTargetException e) {
                final OpenDataException ode =
                    new OpenDataException("Failed to invoke " +
                        fromMethod.getName() + " to convert CompositeData " +
                        " to " + javaClass.getName());
                ode.initCause(e);
                throw ode;
            }
        }
    }

    private static class InProgress<T> extends OpenType<T> {
        private static final String description =
                  "Marker to detect recursive type use -- internal use only!";

        InProgress() throws OpenDataException {
            super("java.lang.String", "java.lang.String", description);
        }

        public String toString() {
            return description;
        }

        public int hashCode() {
            return 0;
        }

        public boolean equals(Object o) {
            return false;
        }

        public boolean isValue(Object o) {
            return false;
        }
        private static final long serialVersionUID = -3413063475064374490L;
    }
    private static final OpenType<?> inProgress;
    static {
        OpenType<?> t;
        try {
            t = new InProgress<>();
        } catch (OpenDataException e) {
            // Should not reach here
            throw new AssertionError(e);
        }
        inProgress = t;
    }

    private static final OpenType<?>[] simpleTypes = {
        BIGDECIMAL, BIGINTEGER, BOOLEAN, BYTE, CHARACTER, DATE,
        DOUBLE, FLOAT, INTEGER, LONG, OBJECTNAME, SHORT, STRING,
        VOID,
    };
    static {
        try {
            for (int i = 0; i < simpleTypes.length; i++) {
                final OpenType<?> t = simpleTypes[i];
                Class<?> c;
                try {
                    c = Class.forName(t.getClassName(), false,
                                      MappedMXBeanType.class.getClassLoader());
                    MappedMXBeanType.newBasicType(c, t);
                } catch (ClassNotFoundException e) {
                    // the classes that these predefined types declare
                    // must exist!
                    throw new AssertionError(e);
                } catch (OpenDataException e) {
                    throw new AssertionError(e);
                }

                if (c.getName().startsWith("java.lang.")) {
                    try {
                        final Field typeField = c.getField("TYPE");
                        final Class<?> primitiveType = (Class<?>) typeField.get(null);
                        MappedMXBeanType.newBasicType(primitiveType, t);
                    } catch (NoSuchFieldException e) {
                        // OK: must not be a primitive wrapper
                    } catch (IllegalAccessException e) {
                        // Should not reach here
                       throw new AssertionError(e);
                    }
                }
            }
        } catch (OpenDataException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Utility method to take a string and convert it to normal Java variable
     * name capitalization.  This normally means converting the first
     * character from upper case to lower case, but in the (unusual) special
     * case when there is more than one character and both the first and
     * second characters are upper case, we leave it alone.
     * <p>
     * Thus "FooBah" becomes "fooBah" and "X" becomes "x", but "URL" stays
     * as "URL".
     *
     * @param  name The string to be decapitalized.
     * @return  The decapitalized version of the string.
     */
    private static String decapitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) &&
                        Character.isUpperCase(name.charAt(0))){
            return name;
        }
        char chars[] = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

}
