/*
 * Copyright (c) 2007, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javap;

import com.sun.tools.classfile.Annotation;
import com.sun.tools.classfile.TypeAnnotation;
import com.sun.tools.classfile.Annotation.Annotation_element_value;
import com.sun.tools.classfile.Annotation.Array_element_value;
import com.sun.tools.classfile.Annotation.Class_element_value;
import com.sun.tools.classfile.Annotation.Enum_element_value;
import com.sun.tools.classfile.Annotation.Primitive_element_value;
import com.sun.tools.classfile.ConstantPool;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.Descriptor;
import com.sun.tools.classfile.Descriptor.InvalidDescriptor;

/**
 *  A writer for writing annotations as text.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class AnnotationWriter extends BasicWriter {
    static AnnotationWriter instance(Context context) {
        AnnotationWriter instance = context.get(AnnotationWriter.class);
        if (instance == null)
            instance = new AnnotationWriter(context);
        return instance;
    }

    protected AnnotationWriter(Context context) {
        super(context);
        classWriter = ClassWriter.instance(context);
        constantWriter = ConstantWriter.instance(context);
    }

    public void write(Annotation annot) {
        write(annot, false);
        println();
        indent(+1);
        write(annot, true);
        indent(-1);
    }

    public void write(Annotation annot, boolean resolveIndices) {
        writeDescriptor(annot.type_index, resolveIndices);
        if (resolveIndices) {
            boolean showParens = annot.num_element_value_pairs > 0;
            if (showParens) {
                println("(");
                indent(+1);
            }
            for (int i = 0; i < annot.num_element_value_pairs; i++) {
                write(annot.element_value_pairs[i], true);
                println();
            }
            if (showParens) {
                indent(-1);
                print(")");
            }
        } else {
            print("(");
            for (int i = 0; i < annot.num_element_value_pairs; i++) {
                if (i > 0)
                    print(",");
                write(annot.element_value_pairs[i], false);
            }
            print(")");
        }
    }

    public void write(TypeAnnotation annot) {
        write(annot, true, false);
        println();
        indent(+1);
        write(annot.annotation, true);
        indent(-1);
    }

    public void write(TypeAnnotation annot, boolean showOffsets, boolean resolveIndices) {
        write(annot.annotation, resolveIndices);
        print(": ");
        write(annot.position, showOffsets);
    }

    public void write(TypeAnnotation.Position pos, boolean showOffsets) {
        print(pos.type);

        switch (pos.type) {
        // instanceof
        case INSTANCEOF:
        // new expression
        case NEW:
        // constructor/method reference receiver
        case CONSTRUCTOR_REFERENCE:
        case METHOD_REFERENCE:
            if (showOffsets) {
                print(", offset=");
                print(pos.offset);
            }
            break;
        // local variable
        case LOCAL_VARIABLE:
        // resource variable
        case RESOURCE_VARIABLE:
            if (pos.lvarOffset == null) {
                print(", lvarOffset is Null!");
                break;
            }
            print(", {");
            for (int i = 0; i < pos.lvarOffset.length; ++i) {
                if (i != 0) print("; ");
                if (showOffsets) {
                    print("start_pc=");
                    print(pos.lvarOffset[i]);
                }
                print(", length=");
                print(pos.lvarLength[i]);
                print(", index=");
                print(pos.lvarIndex[i]);
            }
            print("}");
            break;
        // exception parameter
        case EXCEPTION_PARAMETER:
            print(", exception_index=");
            print(pos.exception_index);
            break;
        // method receiver
        case METHOD_RECEIVER:
            // Do nothing
            break;
        // type parameter
        case CLASS_TYPE_PARAMETER:
        case METHOD_TYPE_PARAMETER:
            print(", param_index=");
            print(pos.parameter_index);
            break;
        // type parameter bound
        case CLASS_TYPE_PARAMETER_BOUND:
        case METHOD_TYPE_PARAMETER_BOUND:
            print(", param_index=");
            print(pos.parameter_index);
            print(", bound_index=");
            print(pos.bound_index);
            break;
        // class extends or implements clause
        case CLASS_EXTENDS:
            print(", type_index=");
            print(pos.type_index);
            break;
        // throws
        case THROWS:
            print(", type_index=");
            print(pos.type_index);
            break;
        // method parameter
        case METHOD_FORMAL_PARAMETER:
            print(", param_index=");
            print(pos.parameter_index);
            break;
        // type cast
        case CAST:
        // method/constructor/reference type argument
        case CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
        case METHOD_INVOCATION_TYPE_ARGUMENT:
        case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
        case METHOD_REFERENCE_TYPE_ARGUMENT:
            if (showOffsets) {
                print(", offset=");
                print(pos.offset);
            }
            print(", type_index=");
            print(pos.type_index);
            break;
        // We don't need to worry about these
        case METHOD_RETURN:
        case FIELD:
            break;
        case UNKNOWN:
            throw new AssertionError("AnnotationWriter: UNKNOWN target type should never occur!");
        default:
            throw new AssertionError("AnnotationWriter: Unknown target type for position: " + pos);
        }

        // Append location data for generics/arrays.
        if (!pos.location.isEmpty()) {
            print(", location=");
            print(pos.location);
        }
    }

    public void write(Annotation.element_value_pair pair, boolean resolveIndices) {
        writeIndex(pair.element_name_index, resolveIndices);
        print("=");
        write(pair.value, resolveIndices);
    }

    public void write(Annotation.element_value value) {
        write(value, false);
        println();
        indent(+1);
        write(value, true);
        indent(-1);
    }

    public void write(Annotation.element_value value, boolean resolveIndices) {
        ev_writer.write(value, resolveIndices);
    }

    private void writeDescriptor(int index, boolean resolveIndices) {
        if (resolveIndices) {
            try {
                ConstantPool constant_pool = classWriter.getClassFile().constant_pool;
                Descriptor d = new Descriptor(index);
                print(d.getFieldType(constant_pool));
                return;
            } catch (ConstantPoolException | InvalidDescriptor ignore) {
            }
        }

        print("#" + index);
    }

    private void writeIndex(int index, boolean resolveIndices) {
        if (resolveIndices) {
            print(constantWriter.stringValue(index));
        } else
            print("#" + index);
    }

    element_value_Writer ev_writer = new element_value_Writer();

    class element_value_Writer implements Annotation.element_value.Visitor<Void,Boolean> {
        public void write(Annotation.element_value value, boolean resolveIndices) {
            value.accept(this, resolveIndices);
        }

        @Override
        public Void visitPrimitive(Primitive_element_value ev, Boolean resolveIndices) {
            if (resolveIndices) {
                int index = ev.const_value_index;
                switch (ev.tag) {
                    case 'B':
                        print("(byte) ");
                        print(constantWriter.stringValue(index));
                        break;
                    case 'C':
                        print("'");
                        print(constantWriter.charValue(index));
                        print("'");
                        break;
                    case 'D':
                    case 'F':
                    case 'I':
                    case 'J':
                        print(constantWriter.stringValue(index));
                        break;
                    case 'S':
                        print("(short) ");
                        print(constantWriter.stringValue(index));
                        break;
                    case 'Z':
                        print(constantWriter.booleanValue(index));
                        break;
                    case 's':
                        print("\"");
                        print(constantWriter.stringValue(index));
                        print("\"");
                        break;
                    default:
                        print(((char) ev.tag) + "#" + ev.const_value_index);
                        break;
                }
            } else {
                print(((char) ev.tag) + "#" + ev.const_value_index);
            }
            return null;
        }

        @Override
        public Void visitEnum(Enum_element_value ev, Boolean resolveIndices) {
            if (resolveIndices) {
                writeIndex(ev.type_name_index, resolveIndices);
                print(".");
                writeIndex(ev.const_name_index, resolveIndices);
            } else {
                print(((char) ev.tag) + "#" + ev.type_name_index + ".#" + ev.const_name_index);
            }
            return null;
        }

        @Override
        public Void visitClass(Class_element_value ev, Boolean resolveIndices) {
            if (resolveIndices) {
                print("class ");
                writeIndex(ev.class_info_index, resolveIndices);
            } else {
                print(((char) ev.tag) + "#" + ev.class_info_index);
            }
            return null;
        }

        @Override
        public Void visitAnnotation(Annotation_element_value ev, Boolean resolveIndices) {
            print((char) ev.tag);
            AnnotationWriter.this.write(ev.annotation_value, resolveIndices);
            return null;
        }

        @Override
        public Void visitArray(Array_element_value ev, Boolean resolveIndices) {
            print("[");
            for (int i = 0; i < ev.num_values; i++) {
                if (i > 0)
                    print(",");
                write(ev.values[i], resolveIndices);
            }
            print("]");
            return null;
        }

    }

    private final ClassWriter classWriter;
    private final ConstantWriter constantWriter;
}
