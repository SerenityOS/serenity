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

import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import jdk.jfr.EventType;
import jdk.jfr.internal.consumer.RecordingInput;

/**
 * Metadata about a chunk
 */
public final class MetadataDescriptor {

    static final class Attribute {
        final String name;
        final String value;

        private Attribute(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    static final class Element {
        final String name;
        final List<Element> elements = new ArrayList<>();
        final List<Attribute> attributes = new ArrayList<>();

        Element(String name) {
            this.name = name;
        }

        long longValue(String name) {
            String v = attribute(name);
            if (v != null)
                return Long.parseLong(v);
            else
                throw new IllegalArgumentException(name);
        }

        String attribute(String name) {
            for (Attribute a : attributes) {
                if (a.name.equals(name)) {
                    return a.value;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            try {
                prettyPrintXML(sb, "", this);
            } catch (IOException e) {
                // should not happen
            }
            return sb.toString();
        }

        long attribute(String name, long defaultValue) {
            String text = attribute(name);
            if (text == null) {
                return defaultValue;
            }
            return Long.parseLong(text);
        }

        String attribute(String name, String defaultValue) {
            String text = attribute(name);
            if (text == null) {
                return defaultValue;
            }
            return text;
        }

        List<Element> elements(String... names) {
            List<Element> filteredElements = new ArrayList<>();
            for (String name : names) {
                for (Element e : elements) {
                    if (e.name.equals(name)) {
                        filteredElements.add(e);
                    }
                }
            }
            return filteredElements;
        }

        void add(Element element) {
            elements.add(element);
        }

        void addAttribute(String name, Object value) {
            attributes.add(new Attribute(name, String.valueOf(value)));
        }

        Element newChild(String name) {
            Element e = new Element(name);
            elements.add(e);
            return e;
        }

        public void addArrayAttribute(Element element, String name, Object value) {
            String typeName = value.getClass().getComponentType().getName();
            switch (typeName) {
            case "int":
                int[] ints = (int[]) value;
                for (int i = 0; i < ints.length; i++) {
                    addAttribute(name  + "-" + i , ints[i]);
                }
                break;
            case "long":
                long[] longs = (long[]) value;
                for (int i = 0; i < longs.length; i++) {
                    addAttribute(name  + "-" + i , longs[i]);
                }
                break;
            case "float":
                float[] floats = (float[]) value;
                for (int i = 0; i < floats.length; i++) {
                    addAttribute(name  + "-" + i , floats[i]);
                }
                break;

            case "double":
                double[] doubles = (double[]) value;
                for (int i = 0; i < doubles.length; i++) {
                    addAttribute(name  + "-" + i , doubles[i]);
                }
                break;
            case "short":
                short[] shorts = (short[]) value;
                for (int i = 0; i < shorts.length; i++) {
                    addAttribute(name  + "-" + i , shorts[i]);
                }
                break;
            case "char":
                char[] chars = (char[]) value;
                for (int i = 0; i < chars.length; i++) {
                    addAttribute(name  + "-" + i , chars[i]);
                }
                break;
            case "byte":
                byte[] bytes = (byte[]) value;
                for (int i = 0; i < bytes.length; i++) {
                    addAttribute(name  + "-" + i , bytes[i]);
                }
                break;
            case "boolean":
                boolean[] booleans = (boolean[]) value;
                for (int i = 0; i < booleans.length; i++) {
                    addAttribute(name  + "-" + i , booleans[i]);
                }
                break;
            case "java.lang.String":
                String[] strings = (String[]) value;
                for (int i = 0; i < strings.length; i++) {
                    addAttribute(name  + "-" + i , strings[i]);
                }
                break;
            default:
                throw new InternalError("Array type of " + typeName + " is not supported");
            }
        }
    }

    static final String ATTRIBUTE_ID = "id";
    static final String ATTRIBUTE_SIMPLE_TYPE = "simpleType";
    static final String ATTRIBUTE_GMT_OFFSET = "gmtOffset";
    static final String ATTRIBUTE_LOCALE = "locale";
    static final String ELEMENT_TYPE = "class";
    static final String ELEMENT_SETTING = "setting";
    static final String ELEMENT_ANNOTATION = "annotation";
    static final String ELEMENT_FIELD = "field";
    static final String ATTRIBUTE_SUPER_TYPE = "superType";
    static final String ATTRIBUTE_TYPE_ID = "class";
    static final String ATTRIBUTE_DIMENSION = "dimension";
    static final String ATTRIBUTE_NAME = "name";
    static final String ATTRIBUTE_CONSTANT_POOL = "constantPool";
    static final String ATTRIBUTE_DEFAULT_VALUE = "defaultValue";

    final List<EventType> eventTypes = new ArrayList<>();
    final Collection<Type> types = new ArrayList<>();
    long gmtOffset;
    String locale;
    Element root;
    public long metadataId;

    // package private
    MetadataDescriptor() {
    }

    private static void prettyPrintXML(Appendable sb, String indent, Element e) throws IOException {
        sb.append(indent + "<" + e.name);
        for (Attribute a : e.attributes) {
            sb.append(" ").append(a.name).append("=\"").append(a.value).append("\"");
        }
        if (e.elements.size() == 0) {
            sb.append("/");
        }
        sb.append(">\n");
        for (Element child : e.elements) {
            prettyPrintXML(sb, indent + "  ", child);
        }
        if (e.elements.size() != 0) {
            sb.append(indent).append("</").append(e.name).append(">\n");
        }
    }

    public Collection<Type> getTypes() {
        return types;
    }

    public List<EventType> getEventTypes() {
        return eventTypes;
    }

    public int getGMTOffset() {
        return (int) gmtOffset;
    }

    public String getLocale() {
        return locale;
    }

    public static MetadataDescriptor read(RecordingInput input) throws IOException {
        MetadataReader r = new MetadataReader(input);
        return r.getDescriptor();
    }

    static void write(List<Type> types, DataOutput output) throws IOException {
        MetadataDescriptor m = new MetadataDescriptor();
        m.locale = Locale.getDefault().toString();
        m.gmtOffset = TimeZone.getDefault().getRawOffset();
        m.types.addAll(types);
        MetadataWriter w = new MetadataWriter(m);
        w.writeBinary(output);
    }

    @Override
    public String toString() {
        return root.toString();
    }
}
