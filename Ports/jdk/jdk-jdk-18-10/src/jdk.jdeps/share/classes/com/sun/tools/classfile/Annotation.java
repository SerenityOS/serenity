/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.classfile;

import java.io.IOException;

/**
 * See JVMS, section 4.8.16.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Annotation {
    static class InvalidAnnotation extends AttributeException {
        private static final long serialVersionUID = -4620480740735772708L;
        InvalidAnnotation(String msg) {
            super(msg);
        }
    }

    Annotation(ClassReader cr) throws IOException, InvalidAnnotation {
        type_index = cr.readUnsignedShort();
        num_element_value_pairs = cr.readUnsignedShort();
        element_value_pairs = new element_value_pair[num_element_value_pairs];
        for (int i = 0; i < element_value_pairs.length; i++)
            element_value_pairs[i] = new element_value_pair(cr);
    }

    public Annotation(ConstantPool constant_pool,
            int type_index,
            element_value_pair[] element_value_pairs) {
        this.type_index = type_index;
        num_element_value_pairs = element_value_pairs.length;
        this.element_value_pairs = element_value_pairs;
    }

    public int length() {
        int n = 2 /*type_index*/ + 2 /*num_element_value_pairs*/;
        for (element_value_pair pair: element_value_pairs)
            n += pair.length();
        return n;
    }

    public final int type_index;
    public final int num_element_value_pairs;
    public final element_value_pair element_value_pairs[];

    /**
     * See JVMS, section 4.8.16.1.
     */
    public static abstract class element_value {
        public static element_value read(ClassReader cr)
                throws IOException, InvalidAnnotation {
            int tag = cr.readUnsignedByte();
            switch (tag) {
            case 'B':
            case 'C':
            case 'D':
            case 'F':
            case 'I':
            case 'J':
            case 'S':
            case 'Z':
            case 's':
                return new Primitive_element_value(cr, tag);

            case 'e':
                return new Enum_element_value(cr, tag);

            case 'c':
                return new Class_element_value(cr, tag);

            case '@':
                return new Annotation_element_value(cr, tag);

            case '[':
                return new Array_element_value(cr, tag);

            default:
                throw new InvalidAnnotation("unrecognized tag: " + tag);
            }
        }

        protected element_value(int tag) {
            this.tag = tag;
        }

        public abstract int length();

        public abstract <R,P> R accept(Visitor<R,P> visitor, P p);

        public interface Visitor<R,P> {
            R visitPrimitive(Primitive_element_value ev, P p);
            R visitEnum(Enum_element_value ev, P p);
            R visitClass(Class_element_value ev, P p);
            R visitAnnotation(Annotation_element_value ev, P p);
            R visitArray(Array_element_value ev, P p);
        }

        public final int tag;
    }

    public static class Primitive_element_value extends element_value {
        Primitive_element_value(ClassReader cr, int tag) throws IOException {
            super(tag);
            const_value_index = cr.readUnsignedShort();
        }

        public Primitive_element_value(int const_value_index, int tag) {
            super(tag);
            this.const_value_index = const_value_index;
        }

        @Override
        public int length() {
            return 2;
        }

        public <R,P> R accept(Visitor<R,P> visitor, P p) {
            return visitor.visitPrimitive(this, p);
        }

        public final int const_value_index;

    }

    public static class Enum_element_value extends element_value {
        Enum_element_value(ClassReader cr, int tag) throws IOException {
            super(tag);
            type_name_index = cr.readUnsignedShort();
            const_name_index = cr.readUnsignedShort();
        }

        public Enum_element_value(int type_name_index, int const_name_index, int tag) {
            super(tag);
            this.type_name_index = type_name_index;
            this.const_name_index = const_name_index;
        }

        @Override
        public int length() {
            return 4;
        }

        public <R,P> R accept(Visitor<R,P> visitor, P p) {
            return visitor.visitEnum(this, p);
        }

        public final int type_name_index;
        public final int const_name_index;
    }

    public static class Class_element_value extends element_value {
        Class_element_value(ClassReader cr, int tag) throws IOException {
            super(tag);
            class_info_index = cr.readUnsignedShort();
        }

        public Class_element_value(int class_info_index, int tag) {
            super(tag);
            this.class_info_index = class_info_index;
        }

        @Override
        public int length() {
            return 2;
        }

        public <R,P> R accept(Visitor<R,P> visitor, P p) {
            return visitor.visitClass(this, p);
        }

        public final int class_info_index;
    }

    public static class Annotation_element_value extends element_value {
        Annotation_element_value(ClassReader cr, int tag)
                throws IOException, InvalidAnnotation {
            super(tag);
            annotation_value = new Annotation(cr);
        }

        public Annotation_element_value(Annotation annotation_value, int tag) {
            super(tag);
            this.annotation_value = annotation_value;
        }

        @Override
        public int length() {
            return annotation_value.length();
        }

        public <R,P> R accept(Visitor<R,P> visitor, P p) {
            return visitor.visitAnnotation(this, p);
        }

        public final Annotation annotation_value;
    }

    public static class Array_element_value extends element_value {
        Array_element_value(ClassReader cr, int tag)
                throws IOException, InvalidAnnotation {
            super(tag);
            num_values = cr.readUnsignedShort();
            values = new element_value[num_values];
            for (int i = 0; i < values.length; i++)
                values[i] = element_value.read(cr);
        }

        public Array_element_value(element_value[] values, int tag) {
            super(tag);
            this.num_values = values.length;
            this.values = values;
        }

        @Override
        public int length() {
            int n = 2;
            for (int i = 0; i < values.length; i++)
                n += values[i].length();
            return n;
        }

        public <R,P> R accept(Visitor<R,P> visitor, P p) {
            return visitor.visitArray(this, p);
        }

        public final int num_values;
        public final element_value[] values;
    }

    public static class element_value_pair {
        element_value_pair(ClassReader cr)
                throws IOException, InvalidAnnotation {
            element_name_index = cr.readUnsignedShort();
            value = element_value.read(cr);
        }

        public element_value_pair(int element_name_index, element_value value) {
            this.element_name_index = element_name_index;
            this.value = value;
        }

        public int length() {
            return 2 + value.length();
        }

        public final int element_name_index;
        public final element_value value;
    }
}
