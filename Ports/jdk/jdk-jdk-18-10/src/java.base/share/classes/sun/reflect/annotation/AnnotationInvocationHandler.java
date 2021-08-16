/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.reflect.annotation;

import java.io.ObjectInputStream;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.io.Serializable;
import java.util.*;
import java.util.stream.*;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * InvocationHandler for dynamic proxy implementation of Annotation.
 *
 * @author  Josh Bloch
 * @since   1.5
 */
class AnnotationInvocationHandler implements InvocationHandler, Serializable {
    @java.io.Serial
    private static final long serialVersionUID = 6182022883658399397L;
    private final Class<? extends Annotation> type;
    @SuppressWarnings("serial") // Not statically typed as Serializable
    private final Map<String, Object> memberValues;

    AnnotationInvocationHandler(Class<? extends Annotation> type, Map<String, Object> memberValues) {
        Class<?>[] superInterfaces = type.getInterfaces();
        if (!type.isAnnotation() ||
            superInterfaces.length != 1 ||
            superInterfaces[0] != java.lang.annotation.Annotation.class)
            throw new AnnotationFormatError("Attempt to create proxy for a non-annotation type: " +
                                            type.getName());
        this.type = type;
        this.memberValues = memberValues;
    }

    public Object invoke(Object proxy, Method method, Object[] args) {
        String member = method.getName();
        int parameterCount = method.getParameterCount();

        // Handle Object and Annotation methods
        if (parameterCount == 1 && member == "equals" &&
                method.getParameterTypes()[0] == Object.class) {
            return equalsImpl(proxy, args[0]);
        }
        if (parameterCount != 0) {
            throw new AssertionError("Too many parameters for an annotation method");
        }

        if (member == "toString") {
            return toStringImpl();
        } else if (member == "hashCode") {
            return hashCodeImpl();
        } else if (member == "annotationType") {
            return type;
        }

        // Handle annotation member accessors
        Object result = memberValues.get(member);

        if (result == null)
            throw new IncompleteAnnotationException(type, member);

        if (result instanceof ExceptionProxy)
            throw ((ExceptionProxy) result).generateException();

        if (result.getClass().isArray() && Array.getLength(result) != 0)
            result = cloneArray(result);

        return result;
    }

    /**
     * This method, which clones its array argument, would not be necessary
     * if Cloneable had a public clone method.
     */
    private Object cloneArray(Object array) {
        Class<?> type = array.getClass();

        if (type == byte[].class) {
            byte[] byteArray = (byte[])array;
            return byteArray.clone();
        }
        if (type == char[].class) {
            char[] charArray = (char[])array;
            return charArray.clone();
        }
        if (type == double[].class) {
            double[] doubleArray = (double[])array;
            return doubleArray.clone();
        }
        if (type == float[].class) {
            float[] floatArray = (float[])array;
            return floatArray.clone();
        }
        if (type == int[].class) {
            int[] intArray = (int[])array;
            return intArray.clone();
        }
        if (type == long[].class) {
            long[] longArray = (long[])array;
            return longArray.clone();
        }
        if (type == short[].class) {
            short[] shortArray = (short[])array;
            return shortArray.clone();
        }
        if (type == boolean[].class) {
            boolean[] booleanArray = (boolean[])array;
            return booleanArray.clone();
        }

        Object[] objectArray = (Object[])array;
        return objectArray.clone();
    }


    /**
     * Implementation of dynamicProxy.toString()
     */
    private String toStringImpl() {
        StringBuilder result = new StringBuilder(128);
        result.append('@');
        result.append(type.getName());
        result.append('(');
        boolean firstMember = true;
        Set<Map.Entry<String, Object>> entries = memberValues.entrySet();
        boolean loneValue = entries.size() == 1;
        for (Map.Entry<String, Object> e : entries) {
            if (firstMember)
                firstMember = false;
            else
                result.append(", ");

            String key = e.getKey();
            if (!loneValue || !"value".equals(key)) {
                result.append(key);
                result.append('=');
            }
            loneValue = false;
            result.append(memberValueToString(e.getValue()));
        }
        result.append(')');
        return result.toString();
    }

    /**
     * Translates a member value (in "dynamic proxy return form") into a string.
     */
    private static String memberValueToString(Object value) {
        Class<?> type = value.getClass();
        if (!type.isArray()) {
            // primitive value, string, class, enum const, or annotation
            if (type == Class.class)
                return toSourceString((Class<?>) value);
            else if (type == String.class)
                return  toSourceString((String) value);
            if (type == Character.class)
                return toSourceString((char) value);
            else if (type == Double.class)
                return  toSourceString((double) value);
            else if (type == Float.class)
                return  toSourceString((float) value);
            else if (type == Long.class)
                return  toSourceString((long) value);
            else if (type == Byte.class)
                return  toSourceString((byte) value);
            else
                return value.toString();
        } else {
            Stream<String> stringStream;
            if (type == byte[].class)
                stringStream = convert((byte[]) value);
            else if (type == char[].class)
                stringStream = convert((char[]) value);
            else if (type == double[].class)
                stringStream = DoubleStream.of((double[]) value)
                    .mapToObj(AnnotationInvocationHandler::toSourceString);
            else if (type == float[].class)
                stringStream = convert((float[]) value);
            else if (type == int[].class)
                stringStream = IntStream.of((int[]) value).mapToObj(String::valueOf);
            else if (type == long[].class) {
                stringStream = LongStream.of((long[]) value)
                    .mapToObj(AnnotationInvocationHandler::toSourceString);
            } else if (type == short[].class)
                stringStream = convert((short[]) value);
            else if (type == boolean[].class)
                stringStream = convert((boolean[]) value);
            else if (type == Class[].class)
                stringStream =
                    Arrays.stream((Class<?>[]) value).
                    map(AnnotationInvocationHandler::toSourceString);
            else if (type == String[].class)
                stringStream =
                    Arrays.stream((String[])value).
                    map(AnnotationInvocationHandler::toSourceString);
            else
                stringStream = Arrays.stream((Object[])value).map(Objects::toString);

            return stringStreamToString(stringStream);
        }
    }

    /**
     * Translates a Class value to a form suitable for use in the
     * string representation of an annotation.
     */
    private static String toSourceString(Class<?> clazz) {
        Class<?> finalComponent = clazz;
        StringBuilder arrayBrackets = new StringBuilder();

        while(finalComponent.isArray()) {
            finalComponent = finalComponent.getComponentType();
            arrayBrackets.append("[]");
        }

        return finalComponent.getName() + arrayBrackets.toString() + ".class";
    }

    private static String toSourceString(float f) {
        if (Float.isFinite(f))
            return Float.toString(f) + "f" ;
        else {
            if (Float.isInfinite(f)) {
                return (f < 0.0f) ? "-1.0f/0.0f": "1.0f/0.0f";
            } else
                return "0.0f/0.0f";
        }
    }

    private static String toSourceString(double d) {
        if (Double.isFinite(d))
            return Double.toString(d);
        else {
            if (Double.isInfinite(d)) {
                return (d < 0.0f) ? "-1.0/0.0": "1.0/0.0";
            } else
                return "0.0/0.0";
        }
    }

    private static String toSourceString(char c) {
        StringBuilder sb = new StringBuilder(4);
        sb.append('\'');
        sb.append(quote(c));
        return sb.append('\'') .toString();
    }

    /**
     * Escapes a character if it has an escape sequence or is
     * non-printable ASCII.  Leaves non-ASCII characters alone.
     */
    private static String quote(char ch) {
        switch (ch) {
        case '\b':  return "\\b";
        case '\f':  return "\\f";
        case '\n':  return "\\n";
        case '\r':  return "\\r";
        case '\t':  return "\\t";
        case '\'':  return "\\'";
        case '\"':  return "\\\"";
        case '\\':  return "\\\\";
        default:
            return (isPrintableAscii(ch))
                ? String.valueOf(ch)
                : String.format("\\u%04x", (int) ch);
        }
    }

    /**
     * Is a character printable ASCII?
     */
    private static boolean isPrintableAscii(char ch) {
        return ch >= ' ' && ch <= '~';
    }

    private static String toSourceString(byte b) {
        return String.format("(byte)0x%02x", b);
    }

    private static String toSourceString(long ell) {
        return String.valueOf(ell) + "L";
    }

    /**
     * Return a string suitable for use in the string representation
     * of an annotation.
     */
    private static String toSourceString(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            sb.append(quote(s.charAt(i)));
        }
        sb.append('"');
        return sb.toString();
    }

    private static Stream<String> convert(byte[] values) {
        List<String> list = new ArrayList<>(values.length);
        for (byte b : values)
            list.add(toSourceString(b));
        return list.stream();
    }

    private static Stream<String> convert(char[] values) {
        List<String> list = new ArrayList<>(values.length);
        for (char c : values)
            list.add(toSourceString(c));
        return list.stream();
    }

    private static Stream<String> convert(float[] values) {
        List<String> list = new ArrayList<>(values.length);
        for (float f : values) {
            list.add(toSourceString(f));
        }
        return list.stream();
    }

    private static Stream<String> convert(short[] values) {
        List<String> list = new ArrayList<>(values.length);
        for (short s : values)
            list.add(Short.toString(s));
        return list.stream();
    }

    private static Stream<String> convert(boolean[] values) {
        List<String> list = new ArrayList<>(values.length);
        for (boolean b : values)
            list.add(Boolean.toString(b));
        return list.stream();
    }

    private static String stringStreamToString(Stream<String> stream) {
        return stream.collect(Collectors.joining(", ", "{", "}"));
    }

    /**
     * Implementation of dynamicProxy.equals(Object o)
     */
    private Boolean equalsImpl(Object proxy, Object o) {
        if (o == proxy)
            return true;

        if (!type.isInstance(o))
            return false;
        for (Method memberMethod : getMemberMethods()) {
            if (memberMethod.isSynthetic())
                continue;
            String member = memberMethod.getName();
            Object ourValue = memberValues.get(member);
            Object hisValue = null;
            AnnotationInvocationHandler hisHandler = asOneOfUs(o);
            if (hisHandler != null) {
                hisValue = hisHandler.memberValues.get(member);
            } else {
                try {
                    hisValue = memberMethod.invoke(o);
                } catch (InvocationTargetException e) {
                    return false;
                } catch (IllegalAccessException e) {
                    throw new AssertionError(e);
                }
            }
            if (!memberValueEquals(ourValue, hisValue))
                return false;
        }
        return true;
    }

    /**
     * Returns an object's invocation handler if that object is a dynamic
     * proxy with a handler of type AnnotationInvocationHandler.
     * Returns null otherwise.
     */
    private AnnotationInvocationHandler asOneOfUs(Object o) {
        if (Proxy.isProxyClass(o.getClass())) {
            InvocationHandler handler = Proxy.getInvocationHandler(o);
            if (handler instanceof AnnotationInvocationHandler)
                return (AnnotationInvocationHandler) handler;
        }
        return null;
    }

    /**
     * Returns true iff the two member values in "dynamic proxy return form"
     * are equal using the appropriate equality function depending on the
     * member type.  The two values will be of the same type unless one of
     * the containing annotations is ill-formed.  If one of the containing
     * annotations is ill-formed, this method will return false unless the
     * two members are identical object references.
     */
    private static boolean memberValueEquals(Object v1, Object v2) {
        Class<?> type = v1.getClass();

        // Check for primitive, string, class, enum const, annotation,
        // or ExceptionProxy
        if (!type.isArray())
            return v1.equals(v2);

        // Check for array of string, class, enum const, annotation,
        // or ExceptionProxy
        if (v1 instanceof Object[] && v2 instanceof Object[])
            return Arrays.equals((Object[]) v1, (Object[]) v2);

        // Check for ill formed annotation(s)
        if (v2.getClass() != type)
            return false;

        // Deal with array of primitives
        if (type == byte[].class)
            return Arrays.equals((byte[]) v1, (byte[]) v2);
        if (type == char[].class)
            return Arrays.equals((char[]) v1, (char[]) v2);
        if (type == double[].class)
            return Arrays.equals((double[]) v1, (double[]) v2);
        if (type == float[].class)
            return Arrays.equals((float[]) v1, (float[]) v2);
        if (type == int[].class)
            return Arrays.equals((int[]) v1, (int[]) v2);
        if (type == long[].class)
            return Arrays.equals((long[]) v1, (long[]) v2);
        if (type == short[].class)
            return Arrays.equals((short[]) v1, (short[]) v2);
        assert type == boolean[].class;
        return Arrays.equals((boolean[]) v1, (boolean[]) v2);
    }

    /**
     * Returns the member methods for our annotation type.  These are
     * obtained lazily and cached, as they're expensive to obtain
     * and we only need them if our equals method is invoked (which should
     * be rare).
     */
    private Method[] getMemberMethods() {
        Method[] value = memberMethods;
        if (value == null) {
            value = computeMemberMethods();
            memberMethods = value;
        }
        return value;
    }

    @SuppressWarnings("removal")
    private Method[] computeMemberMethods() {
        return AccessController.doPrivileged(
            new PrivilegedAction<Method[]>() {
                public Method[] run() {
                    final Method[] methods = type.getDeclaredMethods();
                    validateAnnotationMethods(methods);
                    AccessibleObject.setAccessible(methods, true);
                    return methods;
                }});
    }

    private transient volatile Method[] memberMethods;

    /**
     * Validates that a method is structurally appropriate for an
     * annotation type. As of Java SE 8, annotation types cannot
     * contain static methods and the declared methods of an
     * annotation type must take zero arguments and there are
     * restrictions on the return type.
     */
    private void validateAnnotationMethods(Method[] memberMethods) {
        /*
         * Specification citations below are from JLS
         * 9.6.1. Annotation Type Elements
         */
        boolean valid = true;
        Method currentMethod = null;
        for(Method method : memberMethods) {
            currentMethod = method;
            int modifiers = method.getModifiers();
            // Skip over methods that may be a static initializer or
            // similar construct. A static initializer may be used for
            // purposes such as initializing a lambda stored in an
            // interface field.
            if (method.isSynthetic() &&
                (modifiers & (Modifier.STATIC | Modifier.PRIVATE)) != 0 &&
                method.getParameterCount() == 0) {
                continue;
            }

            /*
             * "By virtue of the AnnotationTypeElementDeclaration
             * production, a method declaration in an annotation type
             * declaration cannot have formal parameters, type
             * parameters, or a throws clause.
             *
             * "By virtue of the AnnotationTypeElementModifier
             * production, a method declaration in an annotation type
             * declaration cannot be default or static."
             */
            if (modifiers != (Modifier.PUBLIC | Modifier.ABSTRACT) ||
                method.isDefault() ||
                method.getParameterCount() != 0 ||
                method.getExceptionTypes().length != 0) {
                valid = false;
                break;
            }

            /*
             * "It is a compile-time error if the return type of a
             * method declared in an annotation type is not one of the
             * following: a primitive type, String, Class, any
             * parameterized invocation of Class, an enum type
             * (section 8.9), an annotation type, or an array type
             * (chapter 10) whose element type is one of the preceding
             * types."
             */
            Class<?> returnType = method.getReturnType();
            if (returnType.isArray()) {
                returnType = returnType.getComponentType();
                if (returnType.isArray()) { // Only single dimensional arrays
                    valid = false;
                    break;
                }
            }

            if (!((returnType.isPrimitive() && returnType != void.class) ||
                  returnType == java.lang.String.class ||
                  returnType == java.lang.Class.class ||
                  returnType.isEnum() ||
                  returnType.isAnnotation())) {
                valid = false;
                break;
            }

            /*
             * "It is a compile-time error if any method declared in an
             * annotation type has a signature that is
             * override-equivalent to that of any public or protected
             * method declared in class Object or in the interface
             * java.lang.annotation.Annotation."
             *
             * The methods in Object or Annotation meeting the other
             * criteria (no arguments, contrained return type, etc.)
             * above are:
             *
             * String toString()
             * int hashCode()
             * Class<? extends Annotation> annotationType()
             */
            String methodName = method.getName();
            if ((methodName.equals("toString") && returnType == java.lang.String.class) ||
                (methodName.equals("hashCode") && returnType == int.class) ||
                (methodName.equals("annotationType") && returnType == java.lang.Class.class)) {
                valid = false;
                break;
            }
        }
        if (valid)
            return;
        else
            throw new AnnotationFormatError("Malformed method on an annotation type: " +
                                            currentMethod.toString());
    }

    /**
     * Implementation of dynamicProxy.hashCode()
     */
    private int hashCodeImpl() {
        int result = 0;
        for (Map.Entry<String, Object> e : memberValues.entrySet()) {
            result += (127 * e.getKey().hashCode()) ^
                memberValueHashCode(e.getValue());
        }
        return result;
    }

    /**
     * Computes hashCode of a member value (in "dynamic proxy return form")
     */
    private static int memberValueHashCode(Object value) {
        Class<?> type = value.getClass();
        if (!type.isArray())    // primitive, string, class, enum const,
                                // or annotation
            return value.hashCode();

        if (type == byte[].class)
            return Arrays.hashCode((byte[]) value);
        if (type == char[].class)
            return Arrays.hashCode((char[]) value);
        if (type == double[].class)
            return Arrays.hashCode((double[]) value);
        if (type == float[].class)
            return Arrays.hashCode((float[]) value);
        if (type == int[].class)
            return Arrays.hashCode((int[]) value);
        if (type == long[].class)
            return Arrays.hashCode((long[]) value);
        if (type == short[].class)
            return Arrays.hashCode((short[]) value);
        if (type == boolean[].class)
            return Arrays.hashCode((boolean[]) value);
        return Arrays.hashCode((Object[]) value);
    }

    @java.io.Serial
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = s.readFields();

        @SuppressWarnings("unchecked")
        Class<? extends Annotation> t = (Class<? extends Annotation>)fields.get("type", null);
        @SuppressWarnings("unchecked")
        Map<String, Object> streamVals = (Map<String, Object>)fields.get("memberValues", null);

        // Check to make sure that types have not evolved incompatibly

        AnnotationType annotationType = null;
        try {
            annotationType = AnnotationType.getInstance(t);
        } catch(IllegalArgumentException e) {
            // Class is no longer an annotation type; time to punch out
            throw new java.io.InvalidObjectException("Non-annotation type in annotation serial stream");
        }

        Map<String, Class<?>> memberTypes = annotationType.memberTypes();
        // consistent with runtime Map type
        Map<String, Object> mv = new LinkedHashMap<>();

        // If there are annotation members without values, that
        // situation is handled by the invoke method.
        for (Map.Entry<String, Object> memberValue : streamVals.entrySet()) {
            String name = memberValue.getKey();
            Object value = null;
            Class<?> memberType = memberTypes.get(name);
            if (memberType != null) {  // i.e. member still exists
                value = memberValue.getValue();
                if (!(memberType.isInstance(value) ||
                      value instanceof ExceptionProxy)) {
                    value = new AnnotationTypeMismatchExceptionProxy(
                            value.getClass() + "[" + value + "]").setMember(
                                annotationType.members().get(name));
                }
            }
            mv.put(name, value);
        }

        UnsafeAccessor.setType(this, t);
        UnsafeAccessor.setMemberValues(this, mv);
    }

    private static class UnsafeAccessor {
        private static final jdk.internal.misc.Unsafe unsafe
                = jdk.internal.misc.Unsafe.getUnsafe();
        private static final long typeOffset = unsafe.objectFieldOffset
                (AnnotationInvocationHandler.class, "type");
        private static final long memberValuesOffset = unsafe.objectFieldOffset
                (AnnotationInvocationHandler.class, "memberValues");

        static void setType(AnnotationInvocationHandler o,
                            Class<? extends Annotation> type) {
            unsafe.putReference(o, typeOffset, type);
        }

        static void setMemberValues(AnnotationInvocationHandler o,
                                    Map<String, Object> memberValues) {
            unsafe.putReference(o, memberValuesOffset, memberValues);
        }
    }
}
