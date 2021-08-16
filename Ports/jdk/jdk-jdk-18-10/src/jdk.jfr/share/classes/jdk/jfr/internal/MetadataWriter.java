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
import static jdk.jfr.internal.MetadataDescriptor.ATTRIBUTE_DEFAULT_VALUE;
import static jdk.jfr.internal.MetadataDescriptor.ATTRIBUTE_DIMENSION;
import static jdk.jfr.internal.MetadataDescriptor.ATTRIBUTE_GMT_OFFSET;
import static jdk.jfr.internal.MetadataDescriptor.ATTRIBUTE_ID;
import static jdk.jfr.internal.MetadataDescriptor.ATTRIBUTE_LOCALE;
import static jdk.jfr.internal.MetadataDescriptor.ATTRIBUTE_NAME;
import static jdk.jfr.internal.MetadataDescriptor.ATTRIBUTE_SIMPLE_TYPE;
import static jdk.jfr.internal.MetadataDescriptor.ATTRIBUTE_SUPER_TYPE;
import static jdk.jfr.internal.MetadataDescriptor.ATTRIBUTE_TYPE_ID;
import static jdk.jfr.internal.MetadataDescriptor.ELEMENT_ANNOTATION;
import static jdk.jfr.internal.MetadataDescriptor.ELEMENT_FIELD;
import static jdk.jfr.internal.MetadataDescriptor.ELEMENT_SETTING;
import static jdk.jfr.internal.MetadataDescriptor.ELEMENT_TYPE;

import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import jdk.jfr.AnnotationElement;
import jdk.jfr.SettingDescriptor;
import jdk.jfr.ValueDescriptor;
import jdk.jfr.internal.MetadataDescriptor.Attribute;
import jdk.jfr.internal.MetadataDescriptor.Element;
import jdk.jfr.internal.consumer.StringParser;

/**
 * Class responsible for converting a list of types into a format that can be
 * parsed by a client.
 *
 */
final class MetadataWriter {

    private final Element metadata = new Element("metadata");
    private final Element root = new Element("root");

    public MetadataWriter(MetadataDescriptor descriptor) {
        descriptor.getTypes().forEach(type -> makeTypeElement(metadata, type));

        root.add(metadata);
        Element region = new Element("region");
        region.addAttribute(ATTRIBUTE_LOCALE, descriptor.locale);
        region.addAttribute(ATTRIBUTE_GMT_OFFSET, descriptor.gmtOffset);
        root.add(region);
    }

    public void writeBinary(DataOutput output) throws IOException {
        Set<String> stringPool = new HashSet<>(1000);
        // Possible improvement, sort string by how often they occur.
        // and assign low number to the most frequently used.
        buildStringPool(root, stringPool);
        HashMap<String, Integer> lookup = new LinkedHashMap<>(stringPool.size());
        int index = 0;
        int poolSize = stringPool.size();
        writeInt(output, poolSize);
        for (String s : stringPool) {
            lookup.put(s, index);
            writeString(output, s);
            index++;
        }
        write(output, root, lookup);
    }

    private void writeString(DataOutput out, String s) throws IOException {
        if (s == null ) {
            out.writeByte(StringParser.Encoding.NULL.byteValue());
            return;
        }
        out.writeByte(StringParser.Encoding.CHAR_ARRAY.byteValue()); // encoding UTF-16
        int length = s.length();
        writeInt(out, length);
            for (int i = 0; i < length; i++) {
                writeInt(out, s.charAt(i));
            }
    }

    private void writeInt(DataOutput out, int v) throws IOException {

        long s = v & 0xffffffffL;
        if (s < 1 << 7) {
            out.write((byte) (s));
            return;
        }
        out.write((byte) (s | 0x80)); // first byte written
        s >>= 7;
        if (s < 1 << 7) {
            out.write((byte) (s));
            return;
        }
        out.write((byte) (s | 0x80)); // second byte written
        s >>= 7;
        if (s < 1 << 7) {
            out.write((byte) (s));
            return;
        }
        out.write((byte) (s | 0x80)); // third byte written
        s >>= 7;
        if (s < 1 << 7) {
            out.write((byte) (s));
            return;
        }
        s >>= 7;
        out.write((byte) (s));// fourth byte written
    }

    private void buildStringPool(Element element, Set<String> pool) {
        pool.add(element.name);
        for (Attribute a : element.attributes) {
            pool.add(a.name);
            pool.add(a.value);
        }
        for (Element child : element.elements) {
            buildStringPool(child, pool);
        }
    }

    private void write(DataOutput output,Element element, HashMap<String, Integer> lookup) throws IOException {
        writeInt(output, lookup.get(element.name));
        writeInt(output, element.attributes.size());
        for (Attribute a : element.attributes) {
            writeInt(output, lookup.get(a.name));
            writeInt(output, lookup.get(a.value));
        }
        writeInt(output, element.elements.size());
        for (Element child : element.elements) {
            write(output, child, lookup);
        }
    }

    private void makeTypeElement(Element root, Type type) {
        Element element = root.newChild(ELEMENT_TYPE);
        element.addAttribute(ATTRIBUTE_NAME, type.getName());
        String superType = type.getSuperType();
        if (superType != null) {
            element.addAttribute(ATTRIBUTE_SUPER_TYPE, superType);
        }
        if (type.isSimpleType()) {
            element.addAttribute(ATTRIBUTE_SIMPLE_TYPE, true);
        }
        element.addAttribute(ATTRIBUTE_ID, type.getId());
        if (type instanceof PlatformEventType pType) {
            for (SettingDescriptor v : pType.getSettings()) {
                makeSettingElement(element, v);
            }
        }
        for (ValueDescriptor v : type.getFields()) {
            makeFieldElement(element, v);
        }
        for (AnnotationElement a : type.getAnnotationElements()) {
            makeAnnotation(element, a);
        }
    }

    private void makeSettingElement(Element typeElement, SettingDescriptor s) {
        Element element = typeElement.newChild(ELEMENT_SETTING);
        element.addAttribute(ATTRIBUTE_NAME, s.getName());
        element.addAttribute(ATTRIBUTE_TYPE_ID, s.getTypeId());
        element.addAttribute(ATTRIBUTE_DEFAULT_VALUE, s.getDefaultValue());
        for (AnnotationElement a : s.getAnnotationElements()) {
            makeAnnotation(element, a);
        }
    }

    private void makeFieldElement(Element typeElement, ValueDescriptor v) {
        Element element = typeElement.newChild(ELEMENT_FIELD);
        element.addAttribute(ATTRIBUTE_NAME, v.getName());
        element.addAttribute(ATTRIBUTE_TYPE_ID, v.getTypeId());
        if (v.isArray()) {
            element.addAttribute(ATTRIBUTE_DIMENSION, 1);
        }
        if (PrivateAccess.getInstance().isConstantPool(v)) {
            element.addAttribute(ATTRIBUTE_CONSTANT_POOL, true);
        }
        for (AnnotationElement a : v.getAnnotationElements()) {
            makeAnnotation(element, a);
        }
    }

    private void makeAnnotation(Element entity, AnnotationElement annotation) {
        Element element = entity.newChild(ELEMENT_ANNOTATION);
        element.addAttribute(ATTRIBUTE_TYPE_ID, annotation.getTypeId());
        List<Object> values = annotation.getValues();
        int index = 0;
        for (ValueDescriptor v : annotation.getValueDescriptors()) {
            Object value = values.get(index++);
            if (v.isArray()) {
                element.addArrayAttribute(element, v.getName(), value);
            } else {
                element.addAttribute(v.getName(), value);
            }
        }
    }

}
