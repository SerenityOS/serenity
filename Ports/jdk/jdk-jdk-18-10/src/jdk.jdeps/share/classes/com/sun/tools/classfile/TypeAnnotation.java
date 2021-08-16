/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.List;

import com.sun.tools.classfile.TypeAnnotation.Position.TypePathEntry;

/**
 * See JSR 308 specification, Section 3.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class TypeAnnotation {
    TypeAnnotation(ClassReader cr) throws IOException, Annotation.InvalidAnnotation {
        constant_pool = cr.getConstantPool();
        position = read_position(cr);
        annotation = new Annotation(cr);
    }

    public TypeAnnotation(ConstantPool constant_pool,
            Annotation annotation, Position position) {
        this.constant_pool = constant_pool;
        this.position = position;
        this.annotation = annotation;
    }

    public int length() {
        int n = annotation.length();
        n += position_length(position);
        return n;
    }

    @Override
    public String toString() {
        try {
            return "@" + constant_pool.getUTF8Value(annotation.type_index).toString().substring(1) +
                    " pos: " + position.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
    }

    public final ConstantPool constant_pool;
    public final Position position;
    public final Annotation annotation;

    private static Position read_position(ClassReader cr) throws IOException, Annotation.InvalidAnnotation {
        // Copied from ClassReader
        int tag = cr.readUnsignedByte(); // TargetType tag is a byte
        if (!TargetType.isValidTargetTypeValue(tag))
            throw new Annotation.InvalidAnnotation("TypeAnnotation: Invalid type annotation target type value: " + String.format("0x%02X", tag));

        TargetType type = TargetType.fromTargetTypeValue(tag);

        Position position = new Position();
        position.type = type;

        switch (type) {
        // instanceof
        case INSTANCEOF:
        // new expression
        case NEW:
        // constructor/method reference receiver
        case CONSTRUCTOR_REFERENCE:
        case METHOD_REFERENCE:
            position.offset = cr.readUnsignedShort();
            break;
        // local variable
        case LOCAL_VARIABLE:
        // resource variable
        case RESOURCE_VARIABLE:
            int table_length = cr.readUnsignedShort();
            position.lvarOffset = new int[table_length];
            position.lvarLength = new int[table_length];
            position.lvarIndex = new int[table_length];
            for (int i = 0; i < table_length; ++i) {
                position.lvarOffset[i] = cr.readUnsignedShort();
                position.lvarLength[i] = cr.readUnsignedShort();
                position.lvarIndex[i] = cr.readUnsignedShort();
            }
            break;
        // exception parameter
        case EXCEPTION_PARAMETER:
            position.exception_index = cr.readUnsignedShort();
            break;
        // method receiver
        case METHOD_RECEIVER:
            // Do nothing
            break;
        // type parameter
        case CLASS_TYPE_PARAMETER:
        case METHOD_TYPE_PARAMETER:
            position.parameter_index = cr.readUnsignedByte();
            break;
        // type parameter bound
        case CLASS_TYPE_PARAMETER_BOUND:
        case METHOD_TYPE_PARAMETER_BOUND:
            position.parameter_index = cr.readUnsignedByte();
            position.bound_index = cr.readUnsignedByte();
            break;
        // class extends or implements clause
        case CLASS_EXTENDS:
            position.type_index = cr.readUnsignedShort();;
            break;
        // throws
        case THROWS:
            position.type_index = cr.readUnsignedShort();
            break;
        // method parameter
        case METHOD_FORMAL_PARAMETER:
            position.parameter_index = cr.readUnsignedByte();
            break;
        // type cast
        case CAST:
        // method/constructor/reference type argument
        case CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
        case METHOD_INVOCATION_TYPE_ARGUMENT:
        case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
        case METHOD_REFERENCE_TYPE_ARGUMENT:
            position.offset = cr.readUnsignedShort();
            position.type_index = cr.readUnsignedByte();
            break;
        // We don't need to worry about these
        case METHOD_RETURN:
        case FIELD:
            break;
        case UNKNOWN:
            throw new AssertionError("TypeAnnotation: UNKNOWN target type should never occur!");
        default:
            throw new AssertionError("TypeAnnotation: Unknown target type: " + type);
        }

        { // Write type path
            int len = cr.readUnsignedByte();
            List<Integer> loc = new ArrayList<>(len);
            for (int i = 0; i < len * TypePathEntry.bytesPerEntry; ++i)
                loc.add(cr.readUnsignedByte());
            position.location = Position.getTypePathFromBinary(loc);
        }
        return position;
    }

    private static int position_length(Position pos) {
        int n = 0;
        n += 1; // TargetType tag is a byte
        switch (pos.type) {
        // instanceof
        case INSTANCEOF:
        // new expression
        case NEW:
        // constructor/method reference receiver
        case CONSTRUCTOR_REFERENCE:
        case METHOD_REFERENCE:
            n += 2; // offset
            break;
        // local variable
        case LOCAL_VARIABLE:
        // resource variable
        case RESOURCE_VARIABLE:
            n += 2; // table_length;
            int table_length = pos.lvarOffset.length;
            n += 2 * table_length; // offset
            n += 2 * table_length; // length
            n += 2 * table_length; // index
            break;
        // exception parameter
        case EXCEPTION_PARAMETER:
            n += 2; // exception_index
            break;
        // method receiver
        case METHOD_RECEIVER:
            // Do nothing
            break;
        // type parameter
        case CLASS_TYPE_PARAMETER:
        case METHOD_TYPE_PARAMETER:
            n += 1; // parameter_index
            break;
        // type parameter bound
        case CLASS_TYPE_PARAMETER_BOUND:
        case METHOD_TYPE_PARAMETER_BOUND:
            n += 1; // parameter_index
            n += 1; // bound_index
            break;
        // class extends or implements clause
        case CLASS_EXTENDS:
            n += 2; // type_index
            break;
        // throws
        case THROWS:
            n += 2; // type_index
            break;
        // method parameter
        case METHOD_FORMAL_PARAMETER:
            n += 1; // parameter_index
            break;
        // type cast
        case CAST:
        // method/constructor/reference type argument
        case CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
        case METHOD_INVOCATION_TYPE_ARGUMENT:
        case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
        case METHOD_REFERENCE_TYPE_ARGUMENT:
            n += 2; // offset
            n += 1; // type index
            break;
        // We don't need to worry about these
        case METHOD_RETURN:
        case FIELD:
            break;
        case UNKNOWN:
            throw new AssertionError("TypeAnnotation: UNKNOWN target type should never occur!");
        default:
            throw new AssertionError("TypeAnnotation: Unknown target type: " + pos.type);
        }

        {
            n += 1; // length
            n += TypePathEntry.bytesPerEntry * pos.location.size(); // bytes for actual array
        }

        return n;
    }

    // Code duplicated from com.sun.tools.javac.code.TypeAnnotationPosition
    public static class Position {
        public enum TypePathEntryKind {
            ARRAY(0),
            INNER_TYPE(1),
            WILDCARD(2),
            TYPE_ARGUMENT(3);

            public final int tag;

            private TypePathEntryKind(int tag) {
                this.tag = tag;
            }
        }

        public static class TypePathEntry {
            /** The fixed number of bytes per TypePathEntry. */
            public static final int bytesPerEntry = 2;

            public final TypePathEntryKind tag;
            public final int arg;

            public static final TypePathEntry ARRAY = new TypePathEntry(TypePathEntryKind.ARRAY);
            public static final TypePathEntry INNER_TYPE = new TypePathEntry(TypePathEntryKind.INNER_TYPE);
            public static final TypePathEntry WILDCARD = new TypePathEntry(TypePathEntryKind.WILDCARD);

            private TypePathEntry(TypePathEntryKind tag) {
                if (!(tag == TypePathEntryKind.ARRAY ||
                        tag == TypePathEntryKind.INNER_TYPE ||
                        tag == TypePathEntryKind.WILDCARD)) {
                    throw new AssertionError("Invalid TypePathEntryKind: " + tag);
                }
                this.tag = tag;
                this.arg = 0;
            }

            public TypePathEntry(TypePathEntryKind tag, int arg) {
                if (tag != TypePathEntryKind.TYPE_ARGUMENT) {
                    throw new AssertionError("Invalid TypePathEntryKind: " + tag);
                }
                this.tag = tag;
                this.arg = arg;
            }

            public static TypePathEntry fromBinary(int tag, int arg) {
                if (arg != 0 && tag != TypePathEntryKind.TYPE_ARGUMENT.tag) {
                    throw new AssertionError("Invalid TypePathEntry tag/arg: " + tag + "/" + arg);
                }
                switch (tag) {
                case 0:
                    return ARRAY;
                case 1:
                    return INNER_TYPE;
                case 2:
                    return WILDCARD;
                case 3:
                    return new TypePathEntry(TypePathEntryKind.TYPE_ARGUMENT, arg);
                default:
                    throw new AssertionError("Invalid TypePathEntryKind tag: " + tag);
                }
            }

            @Override
            public String toString() {
                return tag.toString() +
                        (tag == TypePathEntryKind.TYPE_ARGUMENT ? ("(" + arg + ")") : "");
            }

            @Override
            public boolean equals(Object other) {
                if (! (other instanceof TypePathEntry)) {
                    return false;
                }
                TypePathEntry tpe = (TypePathEntry) other;
                return this.tag == tpe.tag && this.arg == tpe.arg;
            }

            @Override
            public int hashCode() {
                return this.tag.hashCode() * 17 + this.arg;
            }
        }

        public TargetType type = TargetType.UNKNOWN;

        // For generic/array types.
        // TODO: or should we use null? Noone will use this object.
        public List<TypePathEntry> location = new ArrayList<>(0);

        // Tree position.
        public int pos = -1;

        // For typecasts, type tests, new (and locals, as start_pc).
        public boolean isValidOffset = false;
        public int offset = -1;

        // For locals. arrays same length
        public int[] lvarOffset = null;
        public int[] lvarLength = null;
        public int[] lvarIndex = null;

        // For type parameter bound
        public int bound_index = Integer.MIN_VALUE;

        // For type parameter and method parameter
        public int parameter_index = Integer.MIN_VALUE;

        // For class extends, implements, and throws clauses
        public int type_index = Integer.MIN_VALUE;

        // For exception parameters, index into exception table
        public int exception_index = Integer.MIN_VALUE;

        public Position() {}

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            sb.append(type);

            switch (type) {
            // instanceof
            case INSTANCEOF:
            // new expression
            case NEW:
            // constructor/method reference receiver
            case CONSTRUCTOR_REFERENCE:
            case METHOD_REFERENCE:
                sb.append(", offset = ");
                sb.append(offset);
                break;
            // local variable
            case LOCAL_VARIABLE:
            // resource variable
            case RESOURCE_VARIABLE:
                if (lvarOffset == null) {
                    sb.append(", lvarOffset is null!");
                    break;
                }
                sb.append(", {");
                for (int i = 0; i < lvarOffset.length; ++i) {
                    if (i != 0) sb.append("; ");
                    sb.append("start_pc = ");
                    sb.append(lvarOffset[i]);
                    sb.append(", length = ");
                    sb.append(lvarLength[i]);
                    sb.append(", index = ");
                    sb.append(lvarIndex[i]);
                }
                sb.append("}");
                break;
            // method receiver
            case METHOD_RECEIVER:
                // Do nothing
                break;
            // type parameter
            case CLASS_TYPE_PARAMETER:
            case METHOD_TYPE_PARAMETER:
                sb.append(", param_index = ");
                sb.append(parameter_index);
                break;
            // type parameter bound
            case CLASS_TYPE_PARAMETER_BOUND:
            case METHOD_TYPE_PARAMETER_BOUND:
                sb.append(", param_index = ");
                sb.append(parameter_index);
                sb.append(", bound_index = ");
                sb.append(bound_index);
                break;
            // class extends or implements clause
            case CLASS_EXTENDS:
                sb.append(", type_index = ");
                sb.append(type_index);
                break;
            // throws
            case THROWS:
                sb.append(", type_index = ");
                sb.append(type_index);
                break;
            // exception parameter
            case EXCEPTION_PARAMETER:
                sb.append(", exception_index = ");
                sb.append(exception_index);
                break;
            // method parameter
            case METHOD_FORMAL_PARAMETER:
                sb.append(", param_index = ");
                sb.append(parameter_index);
                break;
            // type cast
            case CAST:
            // method/constructor/reference type argument
            case CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
            case METHOD_INVOCATION_TYPE_ARGUMENT:
            case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
            case METHOD_REFERENCE_TYPE_ARGUMENT:
                sb.append(", offset = ");
                sb.append(offset);
                sb.append(", type_index = ");
                sb.append(type_index);
                break;
            // We don't need to worry about these
            case METHOD_RETURN:
            case FIELD:
                break;
            case UNKNOWN:
                sb.append(", position UNKNOWN!");
                break;
            default:
                throw new AssertionError("Unknown target type: " + type);
            }

            // Append location data for generics/arrays.
            if (!location.isEmpty()) {
                sb.append(", location = (");
                sb.append(location);
                sb.append(")");
            }

            sb.append(", pos = ");
            sb.append(pos);

            sb.append(']');
            return sb.toString();
        }

        /**
         * Indicates whether the target tree of the annotation has been optimized
         * away from classfile or not.
         * @return true if the target has not been optimized away
         */
        public boolean emitToClassfile() {
            return !type.isLocal() || isValidOffset;
        }

        /**
         * Decode the binary representation for a type path and set
         * the {@code location} field.
         *
         * @param list The bytecode representation of the type path.
         */
        public static List<TypePathEntry> getTypePathFromBinary(List<Integer> list) {
            List<TypePathEntry> loc = new ArrayList<>(list.size() / TypePathEntry.bytesPerEntry);
            int idx = 0;
            while (idx < list.size()) {
                if (idx + 1 == list.size()) {
                    throw new AssertionError("Could not decode type path: " + list);
                }
                loc.add(TypePathEntry.fromBinary(list.get(idx), list.get(idx + 1)));
                idx += 2;
            }
            return loc;
        }

        public static List<Integer> getBinaryFromTypePath(List<TypePathEntry> locs) {
            List<Integer> loc = new ArrayList<>(locs.size() * TypePathEntry.bytesPerEntry);
            for (TypePathEntry tpe : locs) {
                loc.add(tpe.tag.tag);
                loc.add(tpe.arg);
            }
            return loc;
        }
    }

    // Code duplicated from com.sun.tools.javac.code.TargetType
    // The IsLocal flag could be removed here.
    public enum TargetType {
        /** For annotations on a class type parameter declaration. */
        CLASS_TYPE_PARAMETER(0x00),

        /** For annotations on a method type parameter declaration. */
        METHOD_TYPE_PARAMETER(0x01),

        /** For annotations on the type of an "extends" or "implements" clause. */
        CLASS_EXTENDS(0x10),

        /** For annotations on a bound of a type parameter of a class. */
        CLASS_TYPE_PARAMETER_BOUND(0x11),

        /** For annotations on a bound of a type parameter of a method. */
        METHOD_TYPE_PARAMETER_BOUND(0x12),

        /** For annotations on a field. */
        FIELD(0x13),

        /** For annotations on a method return type. */
        METHOD_RETURN(0x14),

        /** For annotations on the method receiver. */
        METHOD_RECEIVER(0x15),

        /** For annotations on a method parameter. */
        METHOD_FORMAL_PARAMETER(0x16),

        /** For annotations on a throws clause in a method declaration. */
        THROWS(0x17),

        /** For annotations on a local variable. */
        LOCAL_VARIABLE(0x40, true),

        /** For annotations on a resource variable. */
        RESOURCE_VARIABLE(0x41, true),

        /** For annotations on an exception parameter. */
        EXCEPTION_PARAMETER(0x42, true),

        /** For annotations on a type test. */
        INSTANCEOF(0x43, true),

        /** For annotations on an object creation expression. */
        NEW(0x44, true),

        /** For annotations on a constructor reference receiver. */
        CONSTRUCTOR_REFERENCE(0x45, true),

        /** For annotations on a method reference receiver. */
        METHOD_REFERENCE(0x46, true),

        /** For annotations on a typecast. */
        CAST(0x47, true),

        /** For annotations on a type argument of an object creation expression. */
        CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT(0x48, true),

        /** For annotations on a type argument of a method call. */
        METHOD_INVOCATION_TYPE_ARGUMENT(0x49, true),

        /** For annotations on a type argument of a constructor reference. */
        CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT(0x4A, true),

        /** For annotations on a type argument of a method reference. */
        METHOD_REFERENCE_TYPE_ARGUMENT(0x4B, true),

        /** For annotations with an unknown target. */
        UNKNOWN(0xFF);

        private static final int MAXIMUM_TARGET_TYPE_VALUE = 0x4B;

        private final int targetTypeValue;
        private final boolean isLocal;

        private TargetType(int targetTypeValue) {
            this(targetTypeValue, false);
        }

        private TargetType(int targetTypeValue, boolean isLocal) {
            if (targetTypeValue < 0
                    || targetTypeValue > 255)
                    throw new AssertionError("Attribute type value needs to be an unsigned byte: " + String.format("0x%02X", targetTypeValue));
            this.targetTypeValue = targetTypeValue;
            this.isLocal = isLocal;
        }

        /**
         * Returns whether or not this TargetType represents an annotation whose
         * target is exclusively a tree in a method body
         *
         * Note: wildcard bound targets could target a local tree and a class
         * member declaration signature tree
         */
        public boolean isLocal() {
            return isLocal;
        }

        public int targetTypeValue() {
            return this.targetTypeValue;
        }

        private static final TargetType[] targets;

        static {
            targets = new TargetType[MAXIMUM_TARGET_TYPE_VALUE + 1];
            TargetType[] alltargets = values();
            for (TargetType target : alltargets) {
                if (target.targetTypeValue != UNKNOWN.targetTypeValue)
                    targets[target.targetTypeValue] = target;
            }
            for (int i = 0; i <= MAXIMUM_TARGET_TYPE_VALUE; ++i) {
                if (targets[i] == null)
                    targets[i] = UNKNOWN;
            }
        }

        public static boolean isValidTargetTypeValue(int tag) {
            if (tag == UNKNOWN.targetTypeValue)
                return true;
            return (tag >= 0 && tag < targets.length);
        }

        public static TargetType fromTargetTypeValue(int tag) {
            if (tag == UNKNOWN.targetTypeValue)
                return UNKNOWN;

            if (tag < 0 || tag >= targets.length)
                throw new AssertionError("Unknown TargetType: " + tag);
            return targets[tag];
        }
    }
}
