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

package com.sun.tools.javac.code;

import java.util.Iterator;

import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.util.*;

/** A type annotation position.
*
*  <p><b>This is NOT part of any supported API.
*  If you write code that depends on this, you do so at your own risk.
*  This code and its internal interfaces are subject to change or
*  deletion without notice.</b>
*/
// Code duplicated in com.sun.tools.classfile.TypeAnnotation.Position
public class TypeAnnotationPosition {

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
            Assert.check(tag == TypePathEntryKind.ARRAY ||
                    tag == TypePathEntryKind.INNER_TYPE ||
                    tag == TypePathEntryKind.WILDCARD);
            this.tag = tag;
            this.arg = 0;
        }

        public TypePathEntry(TypePathEntryKind tag, int arg) {
            Assert.check(tag == TypePathEntryKind.TYPE_ARGUMENT);
            this.tag = tag;
            this.arg = arg;
        }

        public static TypePathEntry fromBinary(int tag, int arg) {
            Assert.check(arg == 0 || tag == TypePathEntryKind.TYPE_ARGUMENT.tag);
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
                Assert.error("Invalid TypePathEntryKind tag: " + tag);
                return null;
            }
        }

        @Override
        public String toString() {
            return tag.toString() +
                    (tag == TypePathEntryKind.TYPE_ARGUMENT ? ("(" + arg + ")") : "");
        }

        @Override
        public boolean equals(Object other) {
            return (other instanceof TypePathEntry entry)
                    && this.tag == entry.tag
                    && this.arg == entry.arg;
        }

        @Override
        public int hashCode() {
            return this.tag.hashCode() * 17 + this.arg;
        }
    }

    public static final List<TypePathEntry> emptyPath = List.nil();

    public final TargetType type;

    // For generic/array types.

    public List<TypePathEntry> location;

    // Tree position.
    public final int pos;

    // For type casts, type tests, new, locals (as start_pc),
    // and method and constructor reference type arguments.
    public boolean isValidOffset = false;
    public int offset = -1;

    // For locals. arrays same length
    public int[] lvarOffset = null;
    public int[] lvarLength = null;
    public int[] lvarIndex = null;

    // For type parameter bound
    public final int bound_index;

    // For type parameter and method parameter
    public int parameter_index;

    // For class extends, implements, and throws clauses
    public final int type_index;

    // For exception parameters, index into exception table.  In
    // com.sun.tools.javac.jvm.Gen.genCatch, we first use this to hold
    // the catch type's constant pool entry index.  Then in
    // com.sun.tools.javac.jvm.Code.fillExceptionParameterPositions we
    // use that value to determine the exception table index.
    // When read from class file, this holds
    private int exception_index = Integer.MIN_VALUE;

    // The exception start position.
    // Corresponding to the start_pc in the exception table.
    private int exceptionStartPos = Integer.MIN_VALUE;

    // If this type annotation is within a lambda expression,
    // store a pointer to the lambda expression tree in order
    // to allow a later translation to the right method.
    public final JCLambda onLambda;

    // NOTE: This constructor will eventually go away, and be replaced
    // by static builder methods.

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
            Assert.error("Unknown target type: " + type);
        }

        // Append location data for generics/arrays.
        if (!location.isEmpty()) {
            sb.append(", location = (");
            sb.append(location);
            sb.append(")");
        }

        sb.append(", pos = ");
        sb.append(pos);

        if (onLambda != null) {
            sb.append(", onLambda hash = ");
            sb.append(onLambda.hashCode());
        }

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


    public boolean matchesPos(int pos) {
        return this.pos == pos;
    }

    public void updatePosOffset(int to) {
        offset = to;
        lvarOffset = new int[]{to};
        isValidOffset = true;
    }

    public boolean hasExceptionIndex() {
        return exception_index >= 0;
    }

    public int getExceptionIndex() {
        Assert.check(exception_index >= 0, "exception_index is not set");
        return exception_index;
    }

    public void setExceptionIndex(final int exception_index) {
        Assert.check(!hasExceptionIndex(), "exception_index already set");
        Assert.check(exception_index >= 0, "Expected a valid index into exception table");
        this.exception_index = exception_index;
        this.isValidOffset = true;
    }

    public boolean hasCatchType() {
        return exception_index < 0 && exception_index != Integer.MIN_VALUE;
    }

    public int getCatchType() {
        Assert.check(hasCatchType(),
                     "exception_index does not contain valid catch info");
        return (-this.exception_index) - 1;
    }

    public int getStartPos() {
        Assert.check(exceptionStartPos >= 0,
                     "exceptionStartPos does not contain valid start position");
        return this.exceptionStartPos;
    }

    public void setCatchInfo(final int catchType, final int startPos) {
        Assert.check(!hasExceptionIndex(),
                     "exception_index is already set");
        Assert.check(catchType >= 0, "Expected a valid catch type");
        Assert.check(startPos >= 0, "Expected a valid start position");
        this.exception_index = -(catchType + 1);
        this.exceptionStartPos = startPos;
    }

    /**
     * Decode the binary representation for a type path and set
     * the {@code location} field.
     *
     * @param list The bytecode representation of the type path.
     */
    public static List<TypePathEntry> getTypePathFromBinary(java.util.List<Integer> list) {
        ListBuffer<TypePathEntry> loc = new ListBuffer<>();
        Iterator<Integer> iter = list.iterator();
        while (iter.hasNext()) {
            Integer fst = iter.next();
            Assert.check(iter.hasNext());
            Integer snd = iter.next();
            loc = loc.append(TypePathEntry.fromBinary(fst, snd));
        }
        return loc.toList();
    }

    public static List<Integer> getBinaryFromTypePath(java.util.List<TypePathEntry> locs) {
        ListBuffer<Integer> loc = new ListBuffer<>();
        for (TypePathEntry tpe : locs) {
            loc = loc.append(tpe.tag.tag);
            loc = loc.append(tpe.arg);
        }
        return loc.toList();
    }

    // These methods are the new preferred way to create
    // TypeAnnotationPositions

    // Never make this a public constructor without creating a builder.
    private TypeAnnotationPosition(final TargetType ttype,
                                   final int pos,
                                   final int parameter_index,
                                   final JCLambda onLambda,
                                   final int type_index,
                                   final int bound_index,
                                   final List<TypePathEntry> location) {
        Assert.checkNonNull(location);
        this.type = ttype;
        this.pos = pos;
        this.parameter_index = parameter_index;
        this.onLambda = onLambda;
        this.type_index = type_index;
        this.bound_index = bound_index;
        this.location = location;
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a method return.
     *
     * @param location The type path.
     * @param onLambda The lambda for this parameter.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition
        methodReturn(final List<TypePathEntry> location,
                     final JCLambda onLambda,
                     final int pos) {
        return new TypeAnnotationPosition(TargetType.METHOD_RETURN, pos,
                                          Integer.MIN_VALUE, onLambda,
                                          Integer.MIN_VALUE, Integer.MIN_VALUE,
                                          location);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a method return.
     *
     * @param location The type path.
     */
    public static TypeAnnotationPosition
        methodReturn(final List<TypePathEntry> location) {
        return methodReturn(location, null, -1);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a method return.
     *
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition methodReturn(final int pos) {
        return methodReturn(emptyPath, null, pos);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a method receiver parameter.
     *
     * @param location The type path.
     * @param onLambda The lambda for this parameter.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition
        methodReceiver(final List<TypePathEntry> location,
                     final JCLambda onLambda,
                     final int pos) {
        return new TypeAnnotationPosition(TargetType.METHOD_RECEIVER, pos,
                                          Integer.MIN_VALUE, onLambda,
                                          Integer.MIN_VALUE, Integer.MIN_VALUE,
                                          location);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a method receiver parameter.
     *
     * @param location The type path.
     */
    public static TypeAnnotationPosition
        methodReceiver(final List<TypePathEntry> location) {
        return methodReceiver(location, null, -1);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a method receiver parameter.
     *
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition methodReceiver(final int pos) {
        return methodReceiver(emptyPath, null, pos);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a method formal parameter.
     *
     * @param location The type path.
     * @param onLambda The lambda for this parameter.
     * @param parameter_index The index of the parameter.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition
        methodParameter(final List<TypePathEntry> location,
                        final JCLambda onLambda,
                        final int parameter_index,
                        final int pos) {
        return new TypeAnnotationPosition(TargetType.METHOD_FORMAL_PARAMETER,
                                          pos, parameter_index, onLambda,
                                          Integer.MIN_VALUE, Integer.MIN_VALUE,
                                          location);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a method formal parameter.
     *
     * @param onLambda The lambda for this parameter.
     * @param parameter_index The index of the parameter.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition
        methodParameter(final JCLambda onLambda,
                        final int parameter_index,
                        final int pos) {
        return methodParameter(emptyPath, onLambda,
                               parameter_index, pos);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a method formal parameter.
     *
     * @param parameter_index The index of the parameter.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition
        methodParameter(final int parameter_index,
                        final int pos) {
        return methodParameter(null, parameter_index, pos);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a method formal parameter.
     *
     * @param location The type path.
     * @param parameter_index The index of the parameter.
     */
    public static TypeAnnotationPosition
        methodParameter(final List<TypePathEntry> location,
                        final int parameter_index) {
        return methodParameter(location, null, parameter_index, -1);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a method reference.
     *
     * @param location The type path.
     * @param onLambda The lambda for this method reference.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition
        methodRef(final List<TypePathEntry> location,
                  final JCLambda onLambda,
                  final int pos) {
        return new TypeAnnotationPosition(TargetType.METHOD_REFERENCE, pos,
                                          Integer.MIN_VALUE, onLambda,
                                          Integer.MIN_VALUE, Integer.MIN_VALUE,
                                          location);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a method reference.
     *
     * @param location The type path.
     */
    public static TypeAnnotationPosition
        methodRef(final List<TypePathEntry> location) {
        return methodRef(location, null, -1);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a constructor reference.
     *
     * @param location The type path.
     * @param onLambda The lambda for this constructor reference.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition
        constructorRef(final List<TypePathEntry> location,
                       final JCLambda onLambda,
                       final int pos) {
        return new TypeAnnotationPosition(TargetType.CONSTRUCTOR_REFERENCE, pos,
                                          Integer.MIN_VALUE, onLambda,
                                          Integer.MIN_VALUE, Integer.MIN_VALUE,
                                          location);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a constructor reference.
     *
     * @param location The type path.
     */
    public static TypeAnnotationPosition
        constructorRef(final List<TypePathEntry> location) {
        return constructorRef(location, null, -1);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a field.
     *
     * @param location The type path.
     * @param onLambda The lambda for this variable.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition
        field(final List<TypePathEntry> location,
              final JCLambda onLambda,
              final int pos) {
        return new TypeAnnotationPosition(TargetType.FIELD, pos,
                                          Integer.MIN_VALUE, onLambda,
                                          Integer.MIN_VALUE, Integer.MIN_VALUE,
                                          location);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a field.
     *
     * @param location The type path.
     */
    public static TypeAnnotationPosition
        field(final List<TypePathEntry> location) {
        return field(location, null, -1);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a field.
     *
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition field(final int pos) {
        return field(emptyPath, null, pos);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a local variable.
     *
     * @param location The type path.
     * @param onLambda The lambda for this variable.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition
        localVariable(final List<TypePathEntry> location,
                      final JCLambda onLambda,
                      final int pos) {
        return new TypeAnnotationPosition(TargetType.LOCAL_VARIABLE, pos,
                                          Integer.MIN_VALUE, onLambda,
                                          Integer.MIN_VALUE, Integer.MIN_VALUE,
                                          location);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a local variable.
     *
     * @param onLambda The lambda for this variable.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition
        localVariable(final JCLambda onLambda,
                      final int pos) {
        return localVariable(emptyPath, onLambda, pos);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a local variable.
     *
     * @param location The type path.
     */
    public static TypeAnnotationPosition
        localVariable(final List<TypePathEntry> location) {
        return localVariable(location, null, -1);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for an exception parameter.
     *
     * @param location The type path.
     * @param onLambda The lambda for this parameter.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition
        exceptionParameter(final List<TypePathEntry> location,
                           final JCLambda onLambda,
                           final int pos) {
        return new TypeAnnotationPosition(TargetType.EXCEPTION_PARAMETER, pos,
                                          Integer.MIN_VALUE, onLambda,
                                          Integer.MIN_VALUE, Integer.MIN_VALUE,
                                          location);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for an exception parameter.
     *
     * @param onLambda The lambda for this parameter.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition
        exceptionParameter(final JCLambda onLambda,
                           final int pos) {
        return exceptionParameter(emptyPath, onLambda, pos);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for an exception parameter.
     *
     * @param location The type path.
     */
    public static TypeAnnotationPosition
        exceptionParameter(final List<TypePathEntry> location) {
        return exceptionParameter(location, null, -1);
    }


    /**
     * Create a {@code TypeAnnotationPosition} for a resource variable.
     *
     * @param location The type path.
     * @param onLambda The lambda for this variable.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition
        resourceVariable(final List<TypePathEntry> location,
                         final JCLambda onLambda,
                         final int pos) {
        return new TypeAnnotationPosition(TargetType.RESOURCE_VARIABLE, pos,
                                          Integer.MIN_VALUE, onLambda,
                                          Integer.MIN_VALUE, Integer.MIN_VALUE,
                                          location);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a resource variable.
     *
     * @param onLambda The lambda for this variable.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition
        resourceVariable(final JCLambda onLambda,
                         final int pos) {
        return resourceVariable(emptyPath, onLambda, pos);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a resource variable.
     *
     * @param location The type path.
     */
    public static TypeAnnotationPosition
        resourceVariable(final List<TypePathEntry> location) {
        return resourceVariable(location, null, -1);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a new.
     *
     * @param location The type path.
     * @param onLambda The lambda for this variable.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition
        newObj(final List<TypePathEntry> location,
               final JCLambda onLambda,
               final int pos) {
        return new TypeAnnotationPosition(TargetType.NEW, pos,
                                          Integer.MIN_VALUE, onLambda,
                                          Integer.MIN_VALUE, Integer.MIN_VALUE,
                                          location);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a new.
     *
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition newObj(final int pos) {
        return newObj(emptyPath, null, pos);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a new.
     *
     * @param location The type path.
     */
    public static TypeAnnotationPosition
        newObj(final List<TypePathEntry> location) {
        return newObj(location, null, -1);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a class extension.
     *
     * @param location The type path.
     * @param onLambda The lambda for this variable.
     * @param type_index The index of the interface.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition
        classExtends(final List<TypePathEntry> location,
                     final JCLambda onLambda,
                     final int type_index,
                     final int pos) {
        return new TypeAnnotationPosition(TargetType.CLASS_EXTENDS, pos,
                                          Integer.MIN_VALUE, onLambda,
                                          type_index, Integer.MIN_VALUE,
                                          location);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a class extension.
     *
     * @param location The type path.
     * @param onLambda The lambda for this variable.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition
        classExtends(final List<TypePathEntry> location,
                     final JCLambda onLambda,
                     final int pos) {
        return classExtends(location, onLambda, 65535, pos);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a class extension.
     *
     * @param location The type path.
     * @param type_index The index of the interface.
     */
    public static TypeAnnotationPosition
        classExtends(final List<TypePathEntry> location,
                     final int type_index) {
        return classExtends(location, null, type_index, -1);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a class extension.
     *
     * @param type_index The index of the interface.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition classExtends(final int type_index,
                                                      final int pos) {
        return classExtends(emptyPath, null, type_index, pos);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a class extension.
     *
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition classExtends(final int pos) {
        return classExtends(65535, pos);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for an instanceof.
     *
     * @param location The type path.
     * @param onLambda The lambda for this variable.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition
        instanceOf(final List<TypePathEntry> location,
                   final JCLambda onLambda,
                   final int pos) {
        return new TypeAnnotationPosition(TargetType.INSTANCEOF, pos,
                                          Integer.MIN_VALUE, onLambda,
                                          Integer.MIN_VALUE, Integer.MIN_VALUE,
                                          location);
    }
    /**
     * Create a {@code TypeAnnotationPosition} for an instanceof.
     *
     * @param location The type path.
     */
    public static TypeAnnotationPosition
        instanceOf(final List<TypePathEntry> location) {
        return instanceOf(location, null, -1);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a type cast.
     *
     * @param location The type path.
     * @param onLambda The lambda for this variable.
     * @param type_index The index into an intersection type.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition
        typeCast(final List<TypePathEntry> location,
                 final JCLambda onLambda,
                 final int type_index,
                 final int pos) {
        return new TypeAnnotationPosition(TargetType.CAST, pos,
                                          Integer.MIN_VALUE, onLambda,
                                          type_index, Integer.MIN_VALUE,
                                          location);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a type cast.
     *
     * @param location The type path.
     * @param type_index The index into an intersection type.
     */
    public static TypeAnnotationPosition
        typeCast(final List<TypePathEntry> location,
                 final int type_index) {
        return typeCast(location, null, type_index, -1);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a method
     * invocation type argument.
     *
     * @param location The type path.
     * @param onLambda The lambda for this variable.
     * @param type_index The index of the type argument.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition
        methodInvocationTypeArg(final List<TypePathEntry> location,
                                final JCLambda onLambda,
                                final int type_index,
                                final int pos) {
        return new TypeAnnotationPosition(TargetType.METHOD_INVOCATION_TYPE_ARGUMENT,
                                          pos, Integer.MIN_VALUE, onLambda,
                                          type_index, Integer.MIN_VALUE,
                                          location);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a method
     * invocation type argument.
     *
     * @param location The type path.
     * @param type_index The index of the type argument.
     */
    public static TypeAnnotationPosition
        methodInvocationTypeArg(final List<TypePathEntry> location,
                                final int type_index) {
        return methodInvocationTypeArg(location, null, type_index, -1);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a constructor
     * invocation type argument.
     *
     * @param location The type path.
     * @param onLambda The lambda for this variable.
     * @param type_index The index of the type argument.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition
        constructorInvocationTypeArg(final List<TypePathEntry> location,
                                     final JCLambda onLambda,
                                     final int type_index,
                                     final int pos) {
        return new TypeAnnotationPosition(TargetType.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT,
                                          pos, Integer.MIN_VALUE, onLambda,
                                          type_index, Integer.MIN_VALUE,
                                          location);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a constructor
     * invocation type argument.
     *
     * @param location The type path.
     * @param type_index The index of the type argument.
     */
    public static TypeAnnotationPosition
        constructorInvocationTypeArg(final List<TypePathEntry> location,
                                     final int type_index) {
        return constructorInvocationTypeArg(location, null, type_index, -1);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a type parameter.
     *
     * @param location The type path.
     * @param onLambda The lambda for this variable.
     * @param parameter_index The index of the type parameter.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition
        typeParameter(final List<TypePathEntry> location,
                      final JCLambda onLambda,
                      final int parameter_index,
                      final int pos) {
        return new TypeAnnotationPosition(TargetType.CLASS_TYPE_PARAMETER, pos,
                                          parameter_index, onLambda,
                                          Integer.MIN_VALUE, Integer.MIN_VALUE,
                                          location);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a type parameter.
     *
     * @param location The type path.
     * @param parameter_index The index of the type parameter.
     */
    public static TypeAnnotationPosition
        typeParameter(final List<TypePathEntry> location,
                      final int parameter_index) {
        return typeParameter(location, null, parameter_index, -1);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a method type parameter.
     *
     * @param location The type path.
     * @param onLambda The lambda for this variable.
     * @param parameter_index The index of the type parameter.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition
        methodTypeParameter(final List<TypePathEntry> location,
                            final JCLambda onLambda,
                            final int parameter_index,
                            final int pos) {
        return new TypeAnnotationPosition(TargetType.METHOD_TYPE_PARAMETER,
                                          pos, parameter_index, onLambda,
                                          Integer.MIN_VALUE, Integer.MIN_VALUE,
                                          location);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a method type parameter.
     *
     * @param location The type path.
     * @param parameter_index The index of the type parameter.
     */
    public static TypeAnnotationPosition
        methodTypeParameter(final List<TypePathEntry> location,
                            final int parameter_index) {
        return methodTypeParameter(location, null, parameter_index, -1);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a throws clause.
     *
     * @param location The type path.
     * @param onLambda The lambda for this variable.
     * @param type_index The index of the exception.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition
        methodThrows(final List<TypePathEntry> location,
                     final JCLambda onLambda,
                     final int type_index,
                     final int pos) {
        return new TypeAnnotationPosition(TargetType.THROWS, pos,
                                          Integer.MIN_VALUE, onLambda,
                                          type_index, Integer.MIN_VALUE,
                                          location);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a throws clause.
     *
     * @param location The type path.
     * @param type_index The index of the exception.
     */
    public static TypeAnnotationPosition
        methodThrows(final List<TypePathEntry> location,
                     final int type_index) {
        return methodThrows(location, null, type_index, -1);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a method reference
     * type argument.
     *
     * @param location The type path.
     * @param onLambda The lambda for this variable.
     * @param type_index The index of the type argument.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition
        methodRefTypeArg(final List<TypePathEntry> location,
                         final JCLambda onLambda,
                         final int type_index,
                         final int pos) {
        return new TypeAnnotationPosition(TargetType.METHOD_REFERENCE_TYPE_ARGUMENT,
                                          pos, Integer.MIN_VALUE, onLambda,
                                          type_index, Integer.MIN_VALUE,
                                          location);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a method reference
     * type argument.
     *
     * @param location The type path.
     * @param type_index The index of the type argument.
     */
    public static TypeAnnotationPosition
        methodRefTypeArg(final List<TypePathEntry> location,
                         final int type_index) {
        return methodRefTypeArg(location, null, type_index, -1);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a constructor reference
     * type argument.
     *
     * @param location The type path.
     * @param onLambda The lambda for this variable.
     * @param type_index The index of the type argument.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition
        constructorRefTypeArg(final List<TypePathEntry> location,
                              final JCLambda onLambda,
                              final int type_index,
                              final int pos) {
        return new TypeAnnotationPosition(TargetType.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT,
                                          pos, Integer.MIN_VALUE, onLambda,
                                          type_index, Integer.MIN_VALUE,
                                          location);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a constructor reference
     * type argument.
     *
     * @param location The type path.
     * @param type_index The index of the type argument.
     */
    public static TypeAnnotationPosition
        constructorRefTypeArg(final List<TypePathEntry> location,
                              final int type_index) {
        return constructorRefTypeArg(location, null, type_index, -1);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a type parameter bound.
     *
     * @param location The type path.
     * @param onLambda The lambda for this variable.
     * @param parameter_index The index of the type parameter.
     * @param bound_index The index of the type parameter bound.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition
        typeParameterBound(final List<TypePathEntry> location,
                           final JCLambda onLambda,
                           final int parameter_index,
                           final int bound_index,
                           final int pos) {
        return new TypeAnnotationPosition(TargetType.CLASS_TYPE_PARAMETER_BOUND,
                                          pos, parameter_index, onLambda,
                                          Integer.MIN_VALUE, bound_index,
                                          location);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a type parameter bound.
     *
     * @param location The type path.
     * @param parameter_index The index of the type parameter.
     * @param bound_index The index of the type parameter bound.
     */
    public static TypeAnnotationPosition
        typeParameterBound(final List<TypePathEntry> location,
                           final int parameter_index,
                           final int bound_index) {
        return typeParameterBound(location, null, parameter_index,
                                  bound_index, -1);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a method type
     * parameter bound.
     *
     * @param location The type path.
     * @param onLambda The lambda for this variable.
     * @param parameter_index The index of the type parameter.
     * @param bound_index The index of the type parameter bound.
     * @param pos The position from the associated tree node.
     */
    public static TypeAnnotationPosition
        methodTypeParameterBound(final List<TypePathEntry> location,
                                 final JCLambda onLambda,
                                 final int parameter_index,
                                 final int bound_index,
                                 final int pos) {
        return new TypeAnnotationPosition(TargetType.METHOD_TYPE_PARAMETER_BOUND,
                                          pos, parameter_index, onLambda,
                                          Integer.MIN_VALUE, bound_index,
                                          location);
    }

    /**
     * Create a {@code TypeAnnotationPosition} for a method type
     * parameter bound.
     *
     * @param location The type path.
     * @param parameter_index The index of the type parameter.
     * @param bound_index The index of the type parameter bound.
     */
    public static TypeAnnotationPosition
        methodTypeParameterBound(final List<TypePathEntry> location,
                                 final int parameter_index,
                                 final int bound_index) {
        return methodTypeParameterBound(location, null, parameter_index,
                                        bound_index, -1);
    }

    // Consider this deprecated on arrival.  We eventually want to get
    // rid of this value altogether.  Do not use it for anything new.
    public static final TypeAnnotationPosition unknown =
        new TypeAnnotationPosition(TargetType.UNKNOWN, -1,
                                   Integer.MIN_VALUE, null,
                                   Integer.MIN_VALUE, Integer.MIN_VALUE,
                                   emptyPath);
}
