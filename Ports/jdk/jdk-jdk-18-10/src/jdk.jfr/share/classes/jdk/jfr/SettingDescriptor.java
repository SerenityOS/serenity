/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.util.List;
import java.util.Objects;

import jdk.jfr.internal.AnnotationConstruct;
import jdk.jfr.internal.Type;

/**
 * Describes an event setting.
 *
 * @since 9
 */
public final class SettingDescriptor {

    private final AnnotationConstruct annotationConstruct;
    private final Type type;
    private final String name;
    private final String defaultValue;

    // package private, invoked by jdk.internal.
    SettingDescriptor(Type type, String name, String defaultValue, List<AnnotationElement> annotations) {
        Objects.requireNonNull(annotations);
        this.name = Objects.requireNonNull(name, "Name of value descriptor can't be null");
        this.type = Objects.requireNonNull(type);
        this.annotationConstruct = new AnnotationConstruct(annotations);
        this.defaultValue = Objects.requireNonNull(defaultValue);
    }

    // package private
    void setAnnotations(List<AnnotationElement> as) {
        annotationConstruct.setAnnotationElements(as);
    }

    /**
     * Returns the name of the setting (for example, {@code "threshold"}).
     *
     * @return the name, not {@code null}
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a human-readable name that describes the setting (for example,
     * {@code "Threshold"}).
     * <p>
     * If the setting lacks a label, the label for the type that is associated with this
     * setting is returned, or {@code null} if doesn't exist
     *
     * @return a human-readable name, or {@code null} if doesn't exist
     */
    public String getLabel() {
        String label = annotationConstruct.getLabel();
        if (label == null) {
            label = type.getLabel();
        }
        return label;
    }

    /**
     * Returns a sentence that describes the setting (for example
     * {@code "Record event with duration
     * above or equal to threshold"}).
     * <p>
     * If the setting lacks a description, the description for the type that is
     * associated with this setting is returned, or {@code null} if doesn't exist.
     *
     * @return the description, or {@code null} if doesn't exist
     */
    public String getDescription() {
        String description = annotationConstruct.getDescription();
        if (description == null) {
            description = type.getDescription();
        }
        return description;
    }

    /**
     * Returns a textual identifier that specifies how a value that is represented by
     * this {@code SettingDescriptor} object is interpreted or formatted.
     * <p>
     * For example, if the setting descriptor represents a percentage, then
     * {@code "jdk.jfr.Percentage"} hints to a client that a value of "0.5"
     * is formatted as "50%".
     * <p>
     * The JDK provides the following predefined content types:
     * <ul>
     * <li>jdk.jfr.Percentage</li>
     * <li>jdk.jfr.Timespan</li>
     * <li>jdk.jfr.Timestamp</li>
     * <li>jdk.jfr.Frequency</li>
     * <li>jdk.jfr.Flag</li>
     * <li>jdk.jfr.MemoryAddress</li>
     * <li>jdk.jfr.DataAmount</li>
     * <li>jdk.jfr.NetworkAddress</li>
     * </ul>
     * <p>
     * User-defined content types can be created by using {@link ContentType}.
     * <p>
     * If the setting lacks a content type, the content type for the type
     * that is associated with this setting is returned, or {@code null} if not
     * available.
     *
     * @return the content type, or {@code null} if doesn't exist
     *
     * @see ContentType
     */
    public String getContentType() {
        for (AnnotationElement anno : getAnnotationElements()) {
            for (AnnotationElement meta : anno.getAnnotationElements()) {
                if (meta.getTypeName().equals(ContentType.class.getName())) {
                    return anno.getTypeName();
                }
            }
        }
        for (AnnotationElement anno : type.getAnnotationElements()) {
            for (AnnotationElement meta : anno.getAnnotationElements()) {
                if (meta.getTypeName().equals(ContentType.class.getName())) {
                    return anno.getTypeName();
                }
            }
        }
        return null;
    }

    /**
     * Returns the fully qualified class name of the type that is associated with this
     * setting descriptor.
     *
     * @return the type name, not {@code null}
     *
     * @see SettingDescriptor#getTypeId()
     */
    public String getTypeName() {
        return type.getName();
    }

    /**
     * Returns a unique ID for the type in the Java Virtual Machine (JVM).
     * <p>
     * The ID might not be the same between JVM instances.
     *
     * @return the type ID, not negative
     */
    public long getTypeId() {
        return type.getId();
    }

    /**
     * Returns the first annotation for the specified type if an annotation
     * element with the same name is available, {@code null} otherwise.
     *
     * @param <A> the type of the annotation to query for and return if available
     * @param annotationType the Class object that corresponds to the annotation
     *        type, not {@code null}
     * @return this element's annotation for the specified annotation type if
     *         available, {@code null} otherwise
     */
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        Objects.requireNonNull(annotationType);
        return annotationConstruct.getAnnotation(annotationType);
    }

    /**
     * Returns an immutable list of annotation elements for this value
     * descriptor.
     *
     * @return a list of annotations, not {@code null}
     */
    public List<AnnotationElement> getAnnotationElements() {
        return annotationConstruct.getUnmodifiableAnnotationElements();
    }

    /**
     * Returns the default value for this setting descriptor.
     *
     * @return the default value, not {@code null}
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    // package private
    Type getType() {
        return type;
    }
}
