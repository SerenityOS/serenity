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

package jdk.jfr.internal;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jdk.jfr.AnnotationElement;
import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Enabled;
import jdk.jfr.Experimental;
import jdk.jfr.Label;
import jdk.jfr.Period;
import jdk.jfr.Relational;
import jdk.jfr.StackTrace;
import jdk.jfr.Threshold;
import jdk.jfr.TransitionFrom;
import jdk.jfr.TransitionTo;
import jdk.jfr.Unsigned;

public final class MetadataLoader {

    // Caching to reduce allocation pressure and heap usage
    private final AnnotationElement RELATIONAL = new AnnotationElement(Relational.class);
    private final AnnotationElement ENABLED = new AnnotationElement(Enabled.class, false);
    private final AnnotationElement THRESHOLD = new AnnotationElement(Threshold.class, "0 ns");
    private final AnnotationElement STACK_TRACE = new AnnotationElement(StackTrace.class, true);
    private final AnnotationElement TRANSITION_TO = new AnnotationElement(TransitionTo.class);
    private final AnnotationElement TRANSITION_FROM = new AnnotationElement(TransitionFrom.class);
    private final AnnotationElement EXPERIMENTAL = new AnnotationElement(Experimental.class);
    private final AnnotationElement UNSIGNED = new AnnotationElement(Unsigned.class);
    private final List<Object> SMALL_TEMP_LIST = new ArrayList<>();
    private final Type LABEL_TYPE = TypeLibrary.createAnnotationType(Label.class);
    private final Type DESCRIPTION_TYPE = TypeLibrary.createAnnotationType(Description.class);
    private final Type CATEGORY_TYPE = TypeLibrary.createAnnotationType(Category.class);
    private final Type PERIOD_TYPE = TypeLibrary.createAnnotationType(Period.class);

    // <Event>, <Type> and <Relation>
    private static final class TypeElement {
        private final List<FieldElement> fields;
        private final String name;
        private final String label;
        private final String description;
        private final String category;
        private final String period;
        private final boolean thread;
        private final boolean startTime;
        private final boolean stackTrace;
        private final boolean cutoff;
        private final boolean throttle;
        private final boolean isEvent;
        private final boolean isRelation;
        private final boolean experimental;
        private final long id;

        public TypeElement(DataInputStream dis) throws IOException {
            int fieldCount = dis.readInt();
            fields = new ArrayList<>(fieldCount);
            for (int i = 0; i < fieldCount; i++) {
                fields.add(new FieldElement(dis));
            }
            name = dis.readUTF();
            label = dis.readUTF();
            description = dis.readUTF();
            category = dis.readUTF();
            thread = dis.readBoolean();
            stackTrace = dis.readBoolean();
            startTime = dis.readBoolean();
            period = dis.readUTF();
            cutoff = dis.readBoolean();
            throttle = dis.readBoolean();
            experimental = dis.readBoolean();
            id = dis.readLong();
            isEvent = dis.readBoolean();
            isRelation = dis.readBoolean();
        }
    }

    // <Field>
    private static class FieldElement {
        private final String name;
        private final String label;
        private final String description;
        private final String typeName;
        private final String annotations;
        private final String transition;
        private final String relation;
        private final boolean constantPool;
        private final boolean array;
        private final boolean experimental;
        private final boolean unsigned;

        public FieldElement(DataInputStream dis) throws IOException {
            name = dis.readUTF();
            typeName = dis.readUTF();
            label = dis.readUTF();
            description = dis.readUTF();
            constantPool = dis.readBoolean();
            array = dis.readBoolean();
            unsigned = dis.readBoolean();
            annotations = dis.readUTF();
            transition = dis.readUTF();
            relation = dis.readUTF();
            experimental = dis.readBoolean();
        }
    }

    private final List<TypeElement> types;
    private final Map<String, List<AnnotationElement>> anotationElements = new HashMap<>(20);
    private final Map<String, AnnotationElement> categories = new HashMap<>();

    MetadataLoader(DataInputStream dis) throws IOException {
        SMALL_TEMP_LIST.add(this); // add any object to expand list
        int typeCount = dis.readInt();
        types = new ArrayList<>(typeCount);
        for (int i = 0; i < typeCount; i++) {
            types.add(new TypeElement(dis));
        }
    }

    private List<AnnotationElement> createAnnotationElements(String annotation) throws InternalError {
        String[] annotations = annotation.split(",");
        List<AnnotationElement> annotationElements = new ArrayList<>();
        for (String a : annotations) {
            a = a.trim();
            int leftParenthesis = a.indexOf("(");
            if (leftParenthesis == -1) {
                annotationElements.add(new AnnotationElement(createAnnotationClass(a)));
            } else {
                int rightParenthesis = a.lastIndexOf(")");
                if (rightParenthesis == -1) {
                    throw new InternalError("Expected closing parenthesis for 'XMLContentType'");
                }
                String value = a.substring(leftParenthesis + 1, rightParenthesis);
                String type = a.substring(0, leftParenthesis);
                annotationElements.add(new AnnotationElement(createAnnotationClass(type), value));
            }
        }
        return annotationElements;
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Annotation> createAnnotationClass(String type) {
        try {
            if (!type.startsWith("jdk.jfr.")) {
                throw new IllegalStateException("Incorrect type " + type + ". Annotation class must be located in jdk.jfr package.");
            }
            Class<?> c = Class.forName(type, true, null);
            return (Class<? extends Annotation>) c;
        } catch (ClassNotFoundException cne) {
            throw new IllegalStateException(cne);
        }
    }

    public static List<Type> createTypes() throws IOException {
        try (DataInputStream dis = new DataInputStream(
                SecuritySupport.getResourceAsStream("/jdk/jfr/internal/types/metadata.bin"))) {
            MetadataLoader ml = new MetadataLoader(dis);
            return ml.buildTypes();
        } catch (Exception e) {
            throw new InternalError(e);
        }
    }

    private List<Type> buildTypes() {
        Map<String, Type> typeMap = buildTypeMap();
        Map<String, AnnotationElement> relationMap = buildRelationMap(typeMap);
        addFields(typeMap, relationMap);
        return new ArrayList<>(typeMap.values());
    }

    private Map<String, AnnotationElement> buildRelationMap(Map<String, Type> typeMap) {
        Map<String, AnnotationElement> relationMap = new HashMap<>(20);
        for (TypeElement t : types) {
            if (t.isRelation) {
                Type relationType = typeMap.get(t.name);
                AnnotationElement ae = PrivateAccess.getInstance().newAnnotation(relationType, Collections.emptyList(), true);
                relationMap.put(t.name, ae);
            }
        }
        return relationMap;
    }

    private void addFields(Map<String, Type> lookup, Map<String, AnnotationElement> relationMap) {
        for (TypeElement te : types) {
            Type type = lookup.get(te.name);
            if (te.isEvent) {
                boolean periodic = !te.period.isEmpty();
                TypeLibrary.addImplicitFields(type, periodic, te.startTime && !periodic, te.thread, te.stackTrace && !periodic, te.cutoff);
            }
            for (FieldElement f : te.fields) {
                Type fieldType = Type.getKnownType(f.typeName);
                if (fieldType == null) {
                    fieldType = Objects.requireNonNull(lookup.get(f.typeName));
                }
                List<AnnotationElement> aes = new ArrayList<>();
                if (f.unsigned) {
                    aes.add(UNSIGNED);
                }
                if (!f.annotations.isEmpty()) {
                    var ae = anotationElements.get(f.annotations);
                    if (ae == null) {
                        ae = createAnnotationElements(f.annotations);
                        anotationElements.put(f.annotations, ae);
                    }
                    aes.addAll(ae);
                }
                if (!f.relation.isEmpty()) {
                    AnnotationElement t = relationMap.get(f.relation);
                    aes.add(Objects.requireNonNull(t));
                }
                if (!f.label.isEmpty()) {
                    aes.add(newAnnotation(LABEL_TYPE, f.label));
                }
                if (f.experimental) {
                    aes.add(EXPERIMENTAL);
                }
                if (!f.description.isEmpty()) {
                    aes.add(newAnnotation(DESCRIPTION_TYPE, f.description));
                }
                if ("from".equals(f.transition)) {
                    aes.add(TRANSITION_FROM);
                }
                if ("to".equals(f.transition)) {
                    aes.add(TRANSITION_TO);
                }
                type.add(PrivateAccess.getInstance().newValueDescriptor(f.name, fieldType, aes, f.array ? 1 : 0, f.constantPool, null));
            }
        }
    }

    private AnnotationElement newAnnotation(Type type, Object value) {
        SMALL_TEMP_LIST.set(0, value);
        return PrivateAccess.getInstance().newAnnotation(type, SMALL_TEMP_LIST, true);
    }

    private Map<String, Type> buildTypeMap() {
        Map<String, Type> typeMap = new HashMap<>(2 * types.size());
        Map<String, Type> knownTypeMap = new HashMap<>(20);
        for (Type kt : Type.getKnownTypes()) {
            typeMap.put(kt.getName(), kt);
            knownTypeMap.put(kt.getName(), kt);
        }
        for (TypeElement t : types) {
            List<AnnotationElement> aes = new ArrayList<>();
            if (!t.category.isEmpty()) {
                AnnotationElement cat = categories.get(t.category);
                if (cat == null) {
                    String[] segments = buildCategorySegments(t.category);
                    cat = newAnnotation(CATEGORY_TYPE, segments);
                    categories.put(t.category, cat);
                }
                aes.add(cat);
            }
            if (!t.label.isEmpty()) {
                aes.add(newAnnotation(LABEL_TYPE, t.label));
            }
            if (!t.description.isEmpty()) {
                aes.add(newAnnotation(DESCRIPTION_TYPE, t.description));
            }
            if (t.isEvent) {
                if (!t.period.isEmpty()) {
                    aes.add(newAnnotation(PERIOD_TYPE, t.period));
                } else {
                    if (t.startTime) {
                        aes.add(THRESHOLD);
                    }
                    if (t.stackTrace) {
                        aes.add(STACK_TRACE);
                    }
                }
                if (t.cutoff) {
                    aes.add(new AnnotationElement(Cutoff.class, Cutoff.INFINITY));
                }
                if (t.throttle) {
                    aes.add(new AnnotationElement(Throttle.class, Throttle.DEFAULT));
                }
            }
            if (t.experimental) {
                aes.add(EXPERIMENTAL);
            }
            Type type;
            if (t.isEvent) {
                aes.add(ENABLED);
                type = new PlatformEventType(t.name, t.id, false, true);
            } else {
                type = knownTypeMap.get(t.name);
                if (type == null) {
                    if (t.isRelation) {
                        type = new Type(t.name, Type.SUPER_TYPE_ANNOTATION, t.id);
                        aes.add(RELATIONAL);
                    } else {
                        type = new Type(t.name, null, t.id);
                    }
                }
            }
            type.setAnnotations(aes);
            typeMap.put(t.name, type);
        }
        return typeMap;
    }

    private String[] buildCategorySegments(String category) {
        String[] segments = category.split(",");
        for (int i = 0; i < segments.length; i++) {
            segments[i] = segments[i].trim();
        }
        return segments;
    }
}
