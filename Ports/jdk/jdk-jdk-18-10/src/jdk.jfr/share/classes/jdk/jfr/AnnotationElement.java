/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

import jdk.jfr.internal.Type;
import jdk.jfr.internal.TypeLibrary;
import jdk.jfr.internal.Utils;

/**
 * Describes event metadata, such as labels, descriptions and units.
 * <p>
 * The following example shows how {@code AnnotationElement} can be used to dynamically define events.
 *
 * <pre>{@literal
 *   List<AnnotationElement> typeAnnotations = new ArrayList<>();
 *   typeAnnotations.add(new AnnotationElement(Name.class, "com.example.HelloWorld"));
 *   typeAnnotations.add(new AnnotationElement(Label.class, "Hello World"));
 *   typeAnnotations.add(new AnnotationElement(Description.class, "Helps programmer getting started"));
 *
 *   List<AnnotationElement> fieldAnnotations = new ArrayList<>();
 *   fieldAnnotations.add(new AnnotationElement(Label.class, "Message"));
 *
 *   List<ValueDescriptor> fields = new ArrayList<>();
 *   fields.add(new ValueDescriptor(String.class, "message", fieldAnnotations));
 *
 *   EventFactory f = EventFactory.create(typeAnnotations, fields);
 *   Event event = f.newEvent();
 *   event.commit();
 * }</pre>
 *
 * @since 9
 */
public final class AnnotationElement {
    private final Type type;
    private final List<Object> annotationValues;
    private final boolean inBootClassLoader;

    // package private
    AnnotationElement(Type type, List<Object> objects, boolean boot) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(objects);
        this.type = type;
        List<ValueDescriptor> fields = type.getFields();
        int fieldCount = fields.size();
        if (objects.size() != fieldCount) {
            StringJoiner descriptors = new StringJoiner(",", "[", "]");
            for (ValueDescriptor v : type.getFields()) {
                descriptors.add(v.getName());
            }
            StringJoiner values = new StringJoiner(",", "[", "]");
            for (Object object : objects) {
                descriptors.add(String.valueOf(object));
            }
            throw new IllegalArgumentException("Annotation " + descriptors + " for " + type.getName() + " doesn't match number of values " + values);
        }

        for (int index = 0; index < fieldCount; index++) {
            Object object = objects.get(index);
            if (object == null) {
                throw new IllegalArgumentException("Annotation value can't be null");
            }
            Class<?> valueType = object.getClass();
            if (fields.get(index).isArray()) {
                valueType = valueType.getComponentType();
            }
            checkType(Utils.unboxType(valueType));
        }
        this.annotationValues = List.copyOf(objects);
        this.inBootClassLoader = boot;
    }

    /**
     * Creates an annotation element to use for dynamically defined events.
     * <p>
     * Supported value types are {@code byte}, {@code int}, {@code short},
     * {@code long}, {@code double}, {@code float}, {@code boolean}, {@code char},
     * and {@code String}. Enums, arrays and classes, are not supported.
     * <p>
     * If {@code annotationType} has annotations (directly present, indirectly
     * present, or associated), then those annotation are recursively included.
     * However, both the {@code annotationType} and any annotation found recursively
     * must have the {@link MetadataDefinition} annotation.
     * <p>
     * To statically define events, see {@link Event} class.
     *
     * @param annotationType interface extending
     *        {@code java.lang.annotation.Annotation}, not {@code null}
     * @param values a {@code Map} with keys that match method names of the specified
     *        annotation interface
     * @throws IllegalArgumentException if value/key is {@code null}, an unsupported
     *         value type is used, or a value/key is used that doesn't match the
     *         signatures in the {@code annotationType}
     */
    public AnnotationElement(Class<? extends Annotation> annotationType, Map<String, Object> values) {
        Objects.requireNonNull(annotationType);
        Objects.requireNonNull(values);
        Utils.checkRegisterPermission();
        // copy values to avoid modification after validation
        HashMap<String, Object> map = new HashMap<>(values);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getKey() == null) {
                throw new NullPointerException("Name of annotation method can't be null");
            }
            if (entry.getValue() == null) {
                throw new NullPointerException("Return value for annotation method can't be null");
            }
        }

        if (AnnotationElement.class.isAssignableFrom(annotationType) && annotationType.isInterface()) {
            throw new IllegalArgumentException("Must be interface extending " + Annotation.class.getName());
        }
        if (!isKnownJFRAnnotation(annotationType) && annotationType.getAnnotation(MetadataDefinition.class) == null) {
            throw new IllegalArgumentException("Annotation class must be annotated with jdk.jfr.MetadataDefinition to be valid");
        }
        if (isKnownJFRAnnotation(annotationType)) {
            this.type = new Type(annotationType.getCanonicalName(), Type.SUPER_TYPE_ANNOTATION, Type.getTypeId(annotationType));
        } else {
            this.type = TypeLibrary.createAnnotationType(annotationType);
        }
        Method[] methods = annotationType.getDeclaredMethods();
        if (methods.length != map.size()) {
            throw new IllegalArgumentException("Number of declared methods must match size of value map");
        }
        List<Object> v = new ArrayList<>(methods.length);
        Set<String> nameSet = methods.length > 1 ? new HashSet<String>() : null;
        for (Method method : methods) {
            String fieldName = method.getName();
            Object object = map.get(fieldName);
            if (object == null) {
                throw new IllegalArgumentException("No method in annotation interface " + annotationType.getName() + " matching name " + fieldName);
            }
            Class<?> fieldType = object.getClass();

            if (fieldType == Class.class) {
                throw new IllegalArgumentException("Annotation value for " + fieldName + " can't be class");
            }
            if (object instanceof Enum) {
                throw new IllegalArgumentException("Annotation value for " + fieldName + " can't be enum");
            }
            if (!fieldType.equals(object.getClass())) {
                throw new IllegalArgumentException("Return type of annotation " + fieldType.getName() + " must match type of object" + object.getClass());
            }

            if (fieldType.isArray()) {
                Class<?> componentType = fieldType.getComponentType();
                checkType(componentType);
                if (componentType.equals(String.class)) {
                    String[] stringArray = (String[]) object;
                    for (int i = 0; i < stringArray.length; i++) {
                        if (stringArray[i] == null) {
                            throw new IllegalArgumentException("Annotation value for " + fieldName + " contains null");
                        }
                    }
                }
            } else {
                fieldType = Utils.unboxType(object.getClass());
                checkType(fieldType);
            }
            if (nameSet!= null) {
                if (nameSet.contains(fieldName)) {
                    throw new IllegalArgumentException("Value with name '" + fieldName + "' already exists");
                }
                nameSet.add(fieldName);
            }
            if (isKnownJFRAnnotation(annotationType)) {
                ValueDescriptor vd = new ValueDescriptor(fieldType, fieldName, Collections.emptyList(), true);
                type.add(vd);
            }
            v.add(object);
        }
        this.annotationValues = List.copyOf(v);
        this.inBootClassLoader = annotationType.getClassLoader() == null;
    }

    /**
     * Creates an annotation element to use for dynamically defined events.
     * <p>
     * Supported value types are {@code byte}, {@code int}, {@code short},
     * {@code long}, {@code double}, {@code float}, {@code boolean}, {@code char},
     * and {@code String}. Enums, arrays, and classes are not supported.
     * <p>
     * If {@code annotationType} has annotations (directly present, indirectly
     * present, or associated), then those annotations are recursively included.
     * However, both {@code annotationType} and any annotation found recursively
     * must have the {@link MetadataDefinition} annotation.
     * <p>
     * To statically define events, see {@link Event} class.
     *
     * @param annotationType interface extending
     *        {@code java.lang.annotation.Annotation,} not {@code null}
     * @param value the value that matches the {@code value} method of the specified
     *        {@code annotationType}
     * @throws IllegalArgumentException if value/key is {@code null}, an unsupported
     *         value type is used, or a value/key is used that doesn't match the
     *         signatures in the {@code annotationType}
     */
    public AnnotationElement(Class<? extends Annotation> annotationType, Object value) {
        this(annotationType, Collections.singletonMap("value", Objects.requireNonNull(value)));
    }

    /**
     * Creates an annotation element to use for dynamically defined events.
     * <p>
     * Supported value types are {@code byte}, {@code short}, {@code int},
     * {@code long}, {@code double}, {@code float}, {@code boolean}, {@code char},
     * and {@code String}. Enums, arrays, and classes are not supported.
     * <p>
     * If {@code annotationType} has annotations (directly present, indirectly
     * present or associated), then those annotation are recursively included.
     * However, both {@code annotationType} and any annotation found recursively
     * must have the {@link MetadataDefinition} annotation.
     * <p>
     * To statically define events, see {@link Event} class.
     *
     * @param annotationType interface extending java.lang.annotation.Annotation,
     *        not {@code null}
     */
    public AnnotationElement(Class<? extends Annotation> annotationType) {
        this(annotationType, Collections.emptyMap());
    }

    /**
     * Returns an immutable list of annotation values in an order that matches the
     * value descriptors for this {@code AnnotationElement}.
     *
     * @return list of values, not {@code null}
     */
    public List<Object> getValues() {
        return annotationValues;
    }

    /**
     * Returns an immutable list of descriptors that describes the annotation values
     * for this {@code AnnotationElement}.
     *
     * @return the list of value descriptors for this {@code Annotation}, not
     *         {@code null}
     */
    public List<ValueDescriptor> getValueDescriptors() {
        return Collections.unmodifiableList(type.getFields());
    }

    /**
     * Returns an immutable list of annotation elements for this
     * {@code AnnotationElement}.
     *
     * @return a list of meta annotation, not {@code null}
     */
    public List<AnnotationElement> getAnnotationElements() {
        return type.getAnnotationElements();
    }

    /**
     * Returns the fully qualified name of the annotation type that corresponds to
     * this {@code AnnotationElement} (for example, {@code "jdk.jfr.Label"}).
     *
     * @return type name, not {@code null}
     */
    public String getTypeName() {
        return type.getName();
    }

    /**
     * Returns a value for this {@code AnnotationElement}.
     *
     * @param name the name of the method in the annotation interface, not
     *        {@code null}.
     *
     * @return the annotation value, not {@code null}.
     *
     * @throws IllegalArgumentException if a method with the specified name does
     *         not exist in the annotation
     */
    public Object getValue(String name) {
        Objects.requireNonNull(name);
        int index = type.indexOf(name);
        if (index != -1) {
            return annotationValues.get(index);
        }
        StringJoiner valueNames = new StringJoiner(",", "[", "]");
        for (ValueDescriptor v : type.getFields()) {
            valueNames.add(v.getName());
        }
        throw new IllegalArgumentException("No value with name '" + name + "'. Valid names are " + valueNames);
    }

    /**
     * Returns {@code true} if an annotation value with the specified name exists in
     * this {@code AnnotationElement}.
     *
     * @param name name of the method in the annotation interface to find, not
     *        {@code null}
     *
     * @return {@code true} if method exists, {@code false} otherwise
     */
    public boolean hasValue(String name) {
        Objects.requireNonNull(name);
        return type.indexOf(name) != -1;
    }

    /**
     * Returns the first annotation for the specified type if an
     * {@code AnnotationElement} with the same name exists, else {@code null}.
     *
     * @param <A> the type of the annotation to query for and return if it exists
     * @param annotationType the {@code Class object} corresponding to the annotation type,
     *        not {@code null}
     * @return this element's annotation for the specified annotation type if
     *         it exists, else {@code null}
     */
    public final <A> A getAnnotation(Class<? extends Annotation> annotationType) {
        Objects.requireNonNull(annotationType);
        return type.getAnnotation(annotationType);
    }

    /**
     * Returns the type ID for this {@code AnnotationElement}.
     * <p>
     * The ID is a unique identifier for the type in the Java Virtual Machine (JVM). The ID might not
     * be the same between JVM instances.
     *
     * @return the type ID, not negative
     */
    public long getTypeId() {
        return type.getId();
    }

    // package private
    Type getType() {
        return type;
    }

    private static void checkType(Class<?> type) {
        if (type.isPrimitive()) {
            return;
        }
        if (type == String.class) {
            return;
        }
        throw new IllegalArgumentException("Only primitives types or java.lang.String are allowed");
    }

    // List of annotation classes that are allowed, even though
    // they don't have @MetadataDefinition.
    private static boolean isKnownJFRAnnotation(Class<? extends Annotation> annotationType) {
        if (annotationType == Registered.class) {
            return true;
        }
        if (annotationType == Threshold.class) {
            return true;
        }
        if (annotationType == StackTrace.class) {
            return true;
        }
        if (annotationType == Period.class) {
            return true;
        }
        if (annotationType == Enabled.class) {
            return true;
        }
        return false;
    }

    // package private
    boolean isInBoot() {
        return inBootClassLoader;
    }

}
