/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

import com.sun.tools.classfile.AccessFlags;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.ConstantPool;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.DescriptorException;
import com.sun.tools.classfile.Instruction;
import com.sun.tools.classfile.Instruction.TypeKind;
import com.sun.tools.classfile.Method;

/*
 *  Write the contents of a Code attribute.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class CodeWriter extends BasicWriter {
    public static CodeWriter instance(Context context) {
        CodeWriter instance = context.get(CodeWriter.class);
        if (instance == null)
            instance = new CodeWriter(context);
        return instance;
    }

    protected CodeWriter(Context context) {
        super(context);
        context.put(CodeWriter.class, this);
        attrWriter = AttributeWriter.instance(context);
        classWriter = ClassWriter.instance(context);
        constantWriter = ConstantWriter.instance(context);
        sourceWriter = SourceWriter.instance(context);
        tryBlockWriter = TryBlockWriter.instance(context);
        stackMapWriter = StackMapWriter.instance(context);
        localVariableTableWriter = LocalVariableTableWriter.instance(context);
        localVariableTypeTableWriter = LocalVariableTypeTableWriter.instance(context);
        typeAnnotationWriter = TypeAnnotationWriter.instance(context);
        options = Options.instance(context);
    }

    void write(Code_attribute attr, ConstantPool constant_pool) {
        println("Code:");
        indent(+1);
        writeVerboseHeader(attr, constant_pool);
        writeInstrs(attr);
        writeExceptionTable(attr);
        attrWriter.write(attr, attr.attributes, constant_pool);
        indent(-1);
    }

    public void writeVerboseHeader(Code_attribute attr, ConstantPool constant_pool) {
        Method method = classWriter.getMethod();
        String argCount;
        try {
            int n = method.descriptor.getParameterCount(constant_pool);
            if (!method.access_flags.is(AccessFlags.ACC_STATIC))
                ++n;  // for 'this'
            argCount = Integer.toString(n);
        } catch (ConstantPoolException e) {
            argCount = report(e);
        } catch (DescriptorException e) {
            argCount = report(e);
        }

        println("stack=" + attr.max_stack +
                ", locals=" + attr.max_locals +
                ", args_size=" + argCount);

    }

    public void writeInstrs(Code_attribute attr) {
        List<InstructionDetailWriter> detailWriters = getDetailWriters(attr);

        for (Instruction instr: attr.getInstructions()) {
            try {
                for (InstructionDetailWriter w: detailWriters)
                    w.writeDetails(instr);
                writeInstr(instr);
            } catch (ArrayIndexOutOfBoundsException | IllegalStateException e) {
                println(report("error at or after byte " + instr.getPC()));
                break;
            }
        }

        for (InstructionDetailWriter w: detailWriters)
            w.flush();
    }

    public void writeInstr(Instruction instr) {
        print(String.format("%4d: %-13s ", instr.getPC(), instr.getMnemonic()));
        // compute the number of indentations for the body of multi-line instructions
        // This is 6 (the width of "%4d: "), divided by the width of each indentation level,
        // and rounded up to the next integer.
        int indentWidth = options.indentWidth;
        int indent = (6 + indentWidth - 1) / indentWidth;
        instr.accept(instructionPrinter, indent);
        println();
    }
    // where
    Instruction.KindVisitor<Void,Integer> instructionPrinter =
            new Instruction.KindVisitor<>() {

        public Void visitNoOperands(Instruction instr, Integer indent) {
            return null;
        }

        public Void visitArrayType(Instruction instr, TypeKind kind, Integer indent) {
            print(" " + kind.name);
            return null;
        }

        public Void visitBranch(Instruction instr, int offset, Integer indent) {
            print((instr.getPC() + offset));
            return null;
        }

        public Void visitConstantPoolRef(Instruction instr, int index, Integer indent) {
            print("#" + index);
            tab();
            print("// ");
            printConstant(index);
            return null;
        }

        public Void visitConstantPoolRefAndValue(Instruction instr, int index, int value, Integer indent) {
            print("#" + index + ",  " + value);
            tab();
            print("// ");
            printConstant(index);
            return null;
        }

        public Void visitLocal(Instruction instr, int index, Integer indent) {
            print(index);
            return null;
        }

        public Void visitLocalAndValue(Instruction instr, int index, int value, Integer indent) {
            print(index + ", " + value);
            return null;
        }

        public Void visitLookupSwitch(Instruction instr,
                int default_, int npairs, int[] matches, int[] offsets, Integer indent) {
            int pc = instr.getPC();
            print("{ // " + npairs);
            indent(indent);
            for (int i = 0; i < npairs; i++) {
                print(String.format("%n%12d: %d", matches[i], (pc + offsets[i])));
            }
            print("\n     default: " + (pc + default_) + "\n}");
            indent(-indent);
            return null;
        }

        public Void visitTableSwitch(Instruction instr,
                int default_, int low, int high, int[] offsets, Integer indent) {
            int pc = instr.getPC();
            print("{ // " + low + " to " + high);
            indent(indent);
            for (int i = 0; i < offsets.length; i++) {
                print(String.format("%n%12d: %d", (low + i), (pc + offsets[i])));
            }
            print("\n     default: " + (pc + default_) + "\n}");
            indent(-indent);
            return null;
        }

        public Void visitValue(Instruction instr, int value, Integer indent) {
            print(value);
            return null;
        }

        public Void visitUnknown(Instruction instr, Integer indent) {
            return null;
        }
    };


    public void writeExceptionTable(Code_attribute attr) {
        if (attr.exception_table_length > 0) {
            println("Exception table:");
            indent(+1);
            println(" from    to  target type");
            for (int i = 0; i < attr.exception_table.length; i++) {
                Code_attribute.Exception_data handler = attr.exception_table[i];
                print(String.format(" %5d %5d %5d",
                        handler.start_pc, handler.end_pc, handler.handler_pc));
                print("   ");
                int catch_type = handler.catch_type;
                if (catch_type == 0) {
                    println("any");
                } else {
                    print("Class ");
                    println(constantWriter.stringValue(catch_type));
                }
            }
            indent(-1);
        }

    }

    private void printConstant(int index) {
        constantWriter.write(index);
    }

    private List<InstructionDetailWriter> getDetailWriters(Code_attribute attr) {
        List<InstructionDetailWriter> detailWriters = new ArrayList<>();
        if (options.details.contains(InstructionDetailWriter.Kind.SOURCE)) {
            sourceWriter.reset(classWriter.getClassFile(), attr);
            if (sourceWriter.hasSource())
                detailWriters.add(sourceWriter);
            else
                println("(Source code not available)");
        }

        if (options.details.contains(InstructionDetailWriter.Kind.LOCAL_VARS)) {
            localVariableTableWriter.reset(attr);
            detailWriters.add(localVariableTableWriter);
        }

        if (options.details.contains(InstructionDetailWriter.Kind.LOCAL_VAR_TYPES)) {
            localVariableTypeTableWriter.reset(attr);
            detailWriters.add(localVariableTypeTableWriter);
        }

        if (options.details.contains(InstructionDetailWriter.Kind.STACKMAPS)) {
            stackMapWriter.reset(attr);
            stackMapWriter.writeInitialDetails();
            detailWriters.add(stackMapWriter);
        }

        if (options.details.contains(InstructionDetailWriter.Kind.TRY_BLOCKS)) {
            tryBlockWriter.reset(attr);
            detailWriters.add(tryBlockWriter);
        }

        if (options.details.contains(InstructionDetailWriter.Kind.TYPE_ANNOS)) {
            typeAnnotationWriter.reset(attr);
            detailWriters.add(typeAnnotationWriter);
        }

        return detailWriters;
    }

    private AttributeWriter attrWriter;
    private ClassWriter classWriter;
    private ConstantWriter constantWriter;
    private LocalVariableTableWriter localVariableTableWriter;
    private LocalVariableTypeTableWriter localVariableTypeTableWriter;
    private TypeAnnotationWriter typeAnnotationWriter;
    private SourceWriter sourceWriter;
    private StackMapWriter stackMapWriter;
    private TryBlockWriter tryBlockWriter;
    private Options options;
}
