/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

import static jdk.jfr.internal.MetadataDescriptor.ATTRIBUTE_CONSTANT_POOL;
import static jdk.jfr.internal.MetadataDescriptor.ATTRIBUTE_DIMENSION;
import static jdk.jfr.internal.MetadataDescriptor.ATTRIBUTE_ID;
import static jdk.jfr.internal.MetadataDescriptor.ATTRIBUTE_NAME;
import static jdk.jfr.internal.MetadataDescriptor.ATTRIBUTE_SIMPLE_TYPE;
import static jdk.jfr.internal.MetadataDescriptor.ATTRIBUTE_SUPER_TYPE;
import static jdk.jfr.internal.MetadataDescriptor.ATTRIBUTE_TYPE_ID;
import static jdk.jfr.internal.MetadataDescriptor.ELEMENT_ANNOTATION;
import static jdk.jfr.internal.MetadataDescriptor.ELEMENT_FIELD;
import static jdk.jfr.internal.MetadataDescriptor.ELEMENT_SETTING;
import static jdk.jfr.internal.MetadataDescriptor.ELEMENT_TYPE;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.jfr.AnnotationElement;
import jdk.jfr.SettingDescriptor;
import jdk.jfr.ValueDescriptor;
import jdk.jfr.internal.MetadataDescriptor.Element;
import jdk.jfr.internal.consumer.RecordingInput;
import jdk.jfr.internal.consumer.StringParser;

/**
 * Parses metadata.
 *
 */
final class MetadataReader {

    private final DataInput input;
    private final List<String> pool;
    private final MetadataDescriptor descriptor;
    private final Map<Long, Type> types = new HashMap<>();

    public MetadataReader(RecordingInput input) throws IOException {
        this.input = input;
        int size = input.readInt();
        this.pool = new ArrayList<>(size);
        StringParser p = new StringParser(null, false);
        for (int i = 0; i < size; i++) {
            this.pool.add((String) p.parse(input));
        }
        descriptor = new MetadataDescriptor();
        Element root = createElement();
        Element metadata = root.elements("metadata").get(0);
        declareTypes(metadata);
        defineTypes(metadata);
        annotateTypes(metadata);
        buildEvenTypes();
        Element time = root.elements("region").get(0);
        descriptor.gmtOffset = time.attribute(MetadataDescriptor.ATTRIBUTE_GMT_OFFSET, 1);
        descriptor.locale = time.attribute(MetadataDescriptor.ATTRIBUTE_LOCALE, "");
        descriptor.root = root;
        if (Logger.shouldLog(LogTag.JFR_SYSTEM_PARSER, LogLevel.TRACE)) {
             List<Type> ts = new ArrayList<>(types.values());
             Collections.sort(ts, (x,y) -> x.getName().compareTo(y.getName()));
             for (Type t : ts) {
                 t.log("Found", LogTag.JFR_SYSTEM_PARSER, LogLevel.TRACE);
             }
        }
    }

    private String readString() throws IOException {
        return pool.get(readInt());
    }

    private int readInt() throws IOException {
        return input.readInt();
    }

    private Element createElement() throws IOException {
        String name = readString();
        Element e = new Element(name);
        int attributeCount = readInt();
        for (int i = 0; i < attributeCount; i++) {
            e.addAttribute(readString(), readString());
        }
        int childrenCount = readInt();
        for (int i = 0; i < childrenCount; i++) {
            e.add(createElement());
        }
        return e;
    }

    private void annotateTypes(Element metadata) throws IOException {
        for (Element typeElement : metadata.elements(ELEMENT_TYPE)) {
            Type type = getType(ATTRIBUTE_ID, typeElement);
            ArrayList<AnnotationElement> aes = new ArrayList<>();
            for (Element annotationElement : typeElement.elements(ELEMENT_ANNOTATION)) {
                aes.add(makeAnnotation(annotationElement));
            }
            aes.trimToSize();
            type.setAnnotations(aes);

            int index = 0;
            if (type instanceof PlatformEventType pType) {
                List<SettingDescriptor> settings = pType.getAllSettings();
                for (Element settingElement : typeElement.elements(ELEMENT_SETTING)) {
                    ArrayList<AnnotationElement> annotations = new ArrayList<>();
                    for (Element annotationElement : settingElement.elements(ELEMENT_ANNOTATION)) {
                        annotations.add(makeAnnotation(annotationElement));
                    }
                    annotations.trimToSize();
                    PrivateAccess.getInstance().setAnnotations(settings.get(index), annotations);
                    index++;
                }
            }
            index = 0;
            List<ValueDescriptor> fields = type.getFields();
            for (Element fieldElement : typeElement.elements(ELEMENT_FIELD)) {
                ArrayList<AnnotationElement> annotations = new ArrayList<>();
                for (Element annotationElement : fieldElement.elements(ELEMENT_ANNOTATION)) {
                    annotations.add(makeAnnotation(annotationElement));
                }
                annotations.trimToSize();
                PrivateAccess.getInstance().setAnnotations(fields.get(index), annotations);
                index++;
            }
        }
    }

    private AnnotationElement makeAnnotation(Element annotationElement) throws IOException {
        Type annotationType = getType(ATTRIBUTE_TYPE_ID, annotationElement);
        List<Object> values = new ArrayList<>();
        for (ValueDescriptor v : annotationType.getFields()) {
            if (v.isArray()) {
                List<Object> list = new ArrayList<>();
                int index = 0;
                while (true) {
                    String text = annotationElement.attribute(v.getName() + "-" + index);
                    if (text == null) {
                        break;
                    }
                    list.add(objectify(v.getTypeName(), text));
                    index++;
                }
                Object object = Utils.makePrimitiveArray(v.getTypeName(), list);
                if (object == null) {
                    throw new IOException("Unsupported type " + list + " in array");
                }
                values.add(object);
            } else {
                String text = annotationElement.attribute(v.getName());
                values.add(objectify(v.getTypeName(), text));
            }
        }
        return PrivateAccess.getInstance().newAnnotation(annotationType, values, false);
    }

    private Object objectify(String typeName, String text) throws IOException {
        try {
            switch (typeName) {
            case "int":
                return Integer.valueOf(text);
            case "long":
                return Long.valueOf(text);
            case "double":
                return Double.valueOf(text);
            case "float":
                return Float.valueOf(text);
            case "short":
                return Short.valueOf(text);
            case "char":
                if (text.length() != 1) {
                    throw new IOException("Unexpected size of char");
                }
                return text.charAt(0);
            case "byte":
                return Byte.valueOf(text);
            case "boolean":
                return Boolean.valueOf(text);
            case "java.lang.String":
                return text;
            }
        } catch (IllegalArgumentException iae) {
            throw new IOException("Could not parse text representation of " + typeName);
        }
        throw new IOException("Unsupported type for annotation " + typeName);
    }

    private Type getType(String attribute, Element element) {
        long id = element.longValue(attribute);
        Type type = types.get(id);
        if (type == null) {
            String name = element.attribute("type");
            throw new IllegalStateException("Type '" + id + "' is not defined for " + name);
        }
        return type;
    }

    private void buildEvenTypes() {
        for (Type type : descriptor.types) {
            if (type instanceof PlatformEventType pType) {
                descriptor.eventTypes.add(PrivateAccess.getInstance().newEventType(pType));
            }
        }
    }

    private void defineTypes(Element metadata) {
        for (Element typeElement : metadata.elements(ELEMENT_TYPE)) {
            long id = typeElement.attribute(ATTRIBUTE_ID, -1);
            Type t = types.get(id);
            for (Element fieldElement : typeElement.elements(ELEMENT_SETTING)) {
                String name = fieldElement.attribute(ATTRIBUTE_NAME);
                String defaultValue = fieldElement.attribute(ATTRIBUTE_NAME);
                Type settingType = getType(ATTRIBUTE_TYPE_ID, fieldElement);
                PlatformEventType eventType = (PlatformEventType) t;
                eventType.add(PrivateAccess.getInstance().newSettingDescriptor(settingType, name, defaultValue, new ArrayList<>(2)));
            }
            for (Element fieldElement : typeElement.elements(ELEMENT_FIELD)) {
                String name = fieldElement.attribute(ATTRIBUTE_NAME);
                Type fieldType = getType(ATTRIBUTE_TYPE_ID, fieldElement);
                long dimension = fieldElement.attribute(ATTRIBUTE_DIMENSION, 0);
                boolean constantPool = fieldElement.attribute(ATTRIBUTE_CONSTANT_POOL) != null;
                // Add annotation later, because they may refer to undefined
                // types at this stage
                t.add(PrivateAccess.getInstance().newValueDescriptor(name, fieldType, new ArrayList<>(), (int) dimension, constantPool, null));
            }
            t.trimFields();
        }
    }

    private void declareTypes(Element metadata) {
        for (Element typeElement : metadata.elements(ELEMENT_TYPE)) {
            String typeName = typeElement.attribute(ATTRIBUTE_NAME);
            String superType = typeElement.attribute(ATTRIBUTE_SUPER_TYPE);
            boolean simpleType = typeElement.attribute(ATTRIBUTE_SIMPLE_TYPE) != null;
            long id = typeElement.attribute(ATTRIBUTE_ID, -1);
            Type t;
            if (Type.SUPER_TYPE_EVENT.equals(superType)) {
                t = new PlatformEventType(typeName, id, false, false);
            } else {
                t = new Type(typeName, superType, id, simpleType);
            }
            types.put(id, t);
            descriptor.types.add(t);
        }
    }

    public MetadataDescriptor getDescriptor() {
        return descriptor;
    }
}
