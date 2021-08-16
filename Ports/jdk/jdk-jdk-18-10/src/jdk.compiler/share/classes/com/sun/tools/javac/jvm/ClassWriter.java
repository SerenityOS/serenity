/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.jvm;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.function.ToIntFunction;

import javax.tools.JavaFileManager;
import javax.tools.FileObject;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Attribute.RetentionPolicy;
import com.sun.tools.javac.code.Directive.*;
import com.sun.tools.javac.code.Source.Feature;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.code.Types.SignatureGenerator.InvalidSignatureException;
import com.sun.tools.javac.comp.Check;
import com.sun.tools.javac.file.PathFileObject;
import com.sun.tools.javac.jvm.PoolConstant.LoadableConstant;
import com.sun.tools.javac.jvm.PoolConstant.Dynamic.BsmKey;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.resources.CompilerProperties.Fragments;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.List;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Kinds.Kind.*;
import static com.sun.tools.javac.code.Scope.LookupKind.NON_RECURSIVE;
import static com.sun.tools.javac.code.TypeTag.*;
import static com.sun.tools.javac.main.Option.*;

import static javax.tools.StandardLocation.CLASS_OUTPUT;

/** This class provides operations to map an internal symbol table graph
 *  rooted in a ClassSymbol into a classfile.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class ClassWriter extends ClassFile {
    protected static final Context.Key<ClassWriter> classWriterKey = new Context.Key<>();

    private final Options options;

    /** Switch: verbose output.
     */
    private boolean verbose;

    /** Switch: emit source file attribute.
     */
    private boolean emitSourceFile;

    /** Switch: generate CharacterRangeTable attribute.
     */
    private boolean genCrt;

    /** Switch: describe the generated stackmap.
     */
    private boolean debugstackmap;

    /** Preview language level.
     */
    private Preview preview;

    /**
     * Target class version.
     */
    private Target target;

    /**
     * Source language version.
     */
    private Source source;

    /** Type utilities. */
    private Types types;

    private Check check;

    /**
     * If true, class files will be written in module-specific subdirectories
     * of the CLASS_OUTPUT location.
     */
    public boolean multiModuleMode;

    private List<ToIntFunction<Symbol>> extraAttributeHooks = List.nil();

    /** The initial sizes of the data and constant pool buffers.
     *  Sizes are increased when buffers get full.
     */
    static final int DATA_BUF_SIZE = 0x0fff0;
    static final int CLASS_BUF_SIZE = 0x1fff0;

    /** An output buffer for member info.
     */
    public ByteBuffer databuf = new ByteBuffer(DATA_BUF_SIZE);

    /** An output buffer for the constant pool.
     */
    ByteBuffer poolbuf = new ByteBuffer(CLASS_BUF_SIZE);

    /** The constant pool writer.
     */
    final PoolWriter poolWriter;

    /** The log to use for verbose output.
     */
    private final Log log;

    /** The name table. */
    private final Names names;

    /** Access to files. */
    private final JavaFileManager fileManager;

    /** The tags and constants used in compressed stackmap. */
    static final int SAME_FRAME_SIZE = 64;
    static final int SAME_LOCALS_1_STACK_ITEM_EXTENDED = 247;
    static final int SAME_FRAME_EXTENDED = 251;
    static final int FULL_FRAME = 255;
    static final int MAX_LOCAL_LENGTH_DIFF = 4;

    /** Get the ClassWriter instance for this context. */
    public static ClassWriter instance(Context context) {
        ClassWriter instance = context.get(classWriterKey);
        if (instance == null)
            instance = new ClassWriter(context);
        return instance;
    }

    /** Construct a class writer, given an options table.
     */
    protected ClassWriter(Context context) {
        context.put(classWriterKey, this);

        log = Log.instance(context);
        names = Names.instance(context);
        options = Options.instance(context);
        preview = Preview.instance(context);
        target = Target.instance(context);
        source = Source.instance(context);
        types = Types.instance(context);
        check = Check.instance(context);
        fileManager = context.get(JavaFileManager.class);
        poolWriter = Gen.instance(context).poolWriter;

        verbose        = options.isSet(VERBOSE);
        genCrt         = options.isSet(XJCOV);
        debugstackmap = options.isSet("debug.stackmap");

        emitSourceFile = options.isUnset(G_CUSTOM) ||
                            options.isSet(G_CUSTOM, "source");

        String modifierFlags = options.get("debug.dumpmodifiers");
        if (modifierFlags != null) {
            dumpClassModifiers = modifierFlags.indexOf('c') != -1;
            dumpFieldModifiers = modifierFlags.indexOf('f') != -1;
            dumpInnerClassModifiers = modifierFlags.indexOf('i') != -1;
            dumpMethodModifiers = modifierFlags.indexOf('m') != -1;
        }
    }

    public void addExtraAttributes(ToIntFunction<Symbol> addExtraAttributes) {
        extraAttributeHooks = extraAttributeHooks.prepend(addExtraAttributes);
    }

/******************************************************************
 * Diagnostics: dump generated class names and modifiers
 ******************************************************************/

    /** Value of option 'dumpmodifiers' is a string
     *  indicating which modifiers should be dumped for debugging:
     *    'c' -- classes
     *    'f' -- fields
     *    'i' -- innerclass attributes
     *    'm' -- methods
     *  For example, to dump everything:
     *    javac -XDdumpmodifiers=cifm MyProg.java
     */
    private boolean dumpClassModifiers; // -XDdumpmodifiers=c
    private boolean dumpFieldModifiers; // -XDdumpmodifiers=f
    private boolean dumpInnerClassModifiers; // -XDdumpmodifiers=i
    private boolean dumpMethodModifiers; // -XDdumpmodifiers=m


    /** Return flags as a string, separated by " ".
     */
    public static String flagNames(long flags) {
        StringBuilder sbuf = new StringBuilder();
        int i = 0;
        long f = flags & StandardFlags;
        while (f != 0) {
            if ((f & 1) != 0) {
                sbuf.append(" ");
                sbuf.append(flagName[i]);
            }
            f = f >> 1;
            i++;
        }
        return sbuf.toString();
    }
    //where
        private static final String[] flagName = {
            "PUBLIC", "PRIVATE", "PROTECTED", "STATIC", "FINAL",
            "SUPER", "VOLATILE", "TRANSIENT", "NATIVE", "INTERFACE",
            "ABSTRACT", "STRICTFP"};

/******************************************************************
 * Output routines
 ******************************************************************/

    /** Write a character into given byte buffer;
     *  byte buffer will not be grown.
     */
    void putChar(ByteBuffer buf, int op, int x) {
        buf.elems[op  ] = (byte)((x >>  8) & 0xFF);
        buf.elems[op+1] = (byte)((x      ) & 0xFF);
    }

    /** Write an integer into given byte buffer;
     *  byte buffer will not be grown.
     */
    void putInt(ByteBuffer buf, int adr, int x) {
        buf.elems[adr  ] = (byte)((x >> 24) & 0xFF);
        buf.elems[adr+1] = (byte)((x >> 16) & 0xFF);
        buf.elems[adr+2] = (byte)((x >>  8) & 0xFF);
        buf.elems[adr+3] = (byte)((x      ) & 0xFF);
    }

/******************************************************************
 * Writing the Constant Pool
 ******************************************************************/

    /** Thrown when the constant pool is over full.
     */
    public static class PoolOverflow extends RuntimeException {
        private static final long serialVersionUID = 0;
        public PoolOverflow() {}
    }
    public static class StringOverflow extends RuntimeException {
        private static final long serialVersionUID = 0;
        public final String value;
        public StringOverflow(String s) {
            value = s;
        }
    }

/******************************************************************
 * Writing Attributes
 ******************************************************************/

    /** Write header for an attribute to data buffer and return
     *  position past attribute length index.
     */
    public int writeAttr(Name attrName) {
        int index = poolWriter.putName(attrName);
        databuf.appendChar(index);
        databuf.appendInt(0);
        return databuf.length;
    }

    /** Fill in attribute length.
     */
    public void endAttr(int index) {
        putInt(databuf, index - 4, databuf.length - index);
    }

    /** Leave space for attribute count and return index for
     *  number of attributes field.
     */
    int beginAttrs() {
        databuf.appendChar(0);
        return databuf.length;
    }

    /** Fill in number of attributes.
     */
    void endAttrs(int index, int count) {
        putChar(databuf, index - 2, count);
    }

    /** Write the EnclosingMethod attribute if needed.
     *  Returns the number of attributes written (0 or 1).
     */
    int writeEnclosingMethodAttribute(ClassSymbol c) {
        return writeEnclosingMethodAttribute(names.EnclosingMethod, c);
    }

    /** Write the EnclosingMethod attribute with a specified name.
     *  Returns the number of attributes written (0 or 1).
     */
    protected int writeEnclosingMethodAttribute(Name attributeName, ClassSymbol c) {
        if (c.owner.kind != MTH && // neither a local class
            c.name != names.empty) // nor anonymous
            return 0;

        int alenIdx = writeAttr(attributeName);
        ClassSymbol enclClass = c.owner.enclClass();
        MethodSymbol enclMethod =
            (c.owner.type == null // local to init block
             || c.owner.kind != MTH) // or member init
            ? null
            : ((MethodSymbol)c.owner).originalEnclosingMethod();
        databuf.appendChar(poolWriter.putClass(enclClass));
        databuf.appendChar(enclMethod == null ? 0 : poolWriter.putNameAndType(enclMethod));
        endAttr(alenIdx);
        return 1;
    }

    /** Write flag attributes; return number of attributes written.
     */
    int writeFlagAttrs(long flags) {
        int acount = 0;
        if ((flags & DEPRECATED) != 0) {
            int alenIdx = writeAttr(names.Deprecated);
            endAttr(alenIdx);
            acount++;
        }
        return acount;
    }

    /** Write member (field or method) attributes;
     *  return number of attributes written.
     */
    int writeMemberAttrs(Symbol sym, boolean isRecordComponent) {
        int acount = 0;
        if (!isRecordComponent) {
            acount = writeFlagAttrs(sym.flags());
        }
        long flags = sym.flags();
        if ((flags & (SYNTHETIC | BRIDGE)) != SYNTHETIC &&
            (flags & ANONCONSTR) == 0 &&
            (!types.isSameType(sym.type, sym.erasure(types)) ||
             poolWriter.signatureGen.hasTypeVar(sym.type.getThrownTypes()))) {
            // note that a local class with captured variables
            // will get a signature attribute
            int alenIdx = writeAttr(names.Signature);
            databuf.appendChar(poolWriter.putSignature(sym));
            endAttr(alenIdx);
            acount++;
        }
        acount += writeJavaAnnotations(sym.getRawAttributes());
        acount += writeTypeAnnotations(sym.getRawTypeAttributes(), false);
        return acount;
    }

    /**
     * Write method parameter names attribute.
     */
    int writeMethodParametersAttr(MethodSymbol m) {
        MethodType ty = m.externalType(types).asMethodType();
        final int allparams = ty.argtypes.size();
        if (m.params != null && allparams != 0) {
            final int attrIndex = writeAttr(names.MethodParameters);
            databuf.appendByte(allparams);
            // Write extra parameters first
            for (VarSymbol s : m.extraParams) {
                final int flags =
                    ((int) s.flags() & (FINAL | SYNTHETIC | MANDATED)) |
                    ((int) m.flags() & SYNTHETIC);
                databuf.appendChar(poolWriter.putName(s.name));
                databuf.appendChar(flags);
            }
            // Now write the real parameters
            for (VarSymbol s : m.params) {
                final int flags =
                    ((int) s.flags() & (FINAL | SYNTHETIC | MANDATED)) |
                    ((int) m.flags() & SYNTHETIC);
                databuf.appendChar(poolWriter.putName(s.name));
                databuf.appendChar(flags);
            }
            // Now write the captured locals
            for (VarSymbol s : m.capturedLocals) {
                final int flags =
                    ((int) s.flags() & (FINAL | SYNTHETIC | MANDATED)) |
                    ((int) m.flags() & SYNTHETIC);
                databuf.appendChar(poolWriter.putName(s.name));
                databuf.appendChar(flags);
            }
            endAttr(attrIndex);
            return 1;
        } else
            return 0;
    }

    private void writeParamAnnotations(List<VarSymbol> params,
                                       RetentionPolicy retention) {
        databuf.appendByte(params.length());
        for (VarSymbol s : params) {
            ListBuffer<Attribute.Compound> buf = new ListBuffer<>();
            for (Attribute.Compound a : s.getRawAttributes())
                if (types.getRetention(a) == retention)
                    buf.append(a);
            databuf.appendChar(buf.length());
            for (Attribute.Compound a : buf)
                writeCompoundAttribute(a);
        }

    }

    private void writeParamAnnotations(MethodSymbol m,
                                       RetentionPolicy retention) {
        databuf.appendByte(m.params.length());
        writeParamAnnotations(m.params, retention);
    }

    /** Write method parameter annotations;
     *  return number of attributes written.
     */
    int writeParameterAttrs(List<VarSymbol> vars) {
        boolean hasVisible = false;
        boolean hasInvisible = false;
        if (vars != null) {
            for (VarSymbol s : vars) {
                for (Attribute.Compound a : s.getRawAttributes()) {
                    switch (types.getRetention(a)) {
                    case SOURCE: break;
                    case CLASS: hasInvisible = true; break;
                    case RUNTIME: hasVisible = true; break;
                    default: // /* fail soft */ throw new AssertionError(vis);
                    }
                }
            }
        }

        int attrCount = 0;
        if (hasVisible) {
            int attrIndex = writeAttr(names.RuntimeVisibleParameterAnnotations);
            writeParamAnnotations(vars, RetentionPolicy.RUNTIME);
            endAttr(attrIndex);
            attrCount++;
        }
        if (hasInvisible) {
            int attrIndex = writeAttr(names.RuntimeInvisibleParameterAnnotations);
            writeParamAnnotations(vars, RetentionPolicy.CLASS);
            endAttr(attrIndex);
            attrCount++;
        }
        return attrCount;
    }

/**********************************************************************
 * Writing Java-language annotations (aka metadata, attributes)
 **********************************************************************/

    /** Write Java-language annotations; return number of JVM
     *  attributes written (zero or one).
     */
    int writeJavaAnnotations(List<Attribute.Compound> attrs) {
        if (attrs.isEmpty()) return 0;
        ListBuffer<Attribute.Compound> visibles = new ListBuffer<>();
        ListBuffer<Attribute.Compound> invisibles = new ListBuffer<>();
        for (Attribute.Compound a : attrs) {
            switch (types.getRetention(a)) {
            case SOURCE: break;
            case CLASS: invisibles.append(a); break;
            case RUNTIME: visibles.append(a); break;
            default: // /* fail soft */ throw new AssertionError(vis);
            }
        }

        int attrCount = 0;
        if (visibles.length() != 0) {
            int attrIndex = writeAttr(names.RuntimeVisibleAnnotations);
            databuf.appendChar(visibles.length());
            for (Attribute.Compound a : visibles)
                writeCompoundAttribute(a);
            endAttr(attrIndex);
            attrCount++;
        }
        if (invisibles.length() != 0) {
            int attrIndex = writeAttr(names.RuntimeInvisibleAnnotations);
            databuf.appendChar(invisibles.length());
            for (Attribute.Compound a : invisibles)
                writeCompoundAttribute(a);
            endAttr(attrIndex);
            attrCount++;
        }
        return attrCount;
    }

    int writeTypeAnnotations(List<Attribute.TypeCompound> typeAnnos, boolean inCode) {
        if (typeAnnos.isEmpty()) return 0;

        ListBuffer<Attribute.TypeCompound> visibles = new ListBuffer<>();
        ListBuffer<Attribute.TypeCompound> invisibles = new ListBuffer<>();

        for (Attribute.TypeCompound tc : typeAnnos) {
            if (tc.hasUnknownPosition()) {
                boolean fixed = tc.tryFixPosition();

                // Could we fix it?
                if (!fixed) {
                    // This happens for nested types like @A Outer. @B Inner.
                    // For method parameters we get the annotation twice! Once with
                    // a valid position, once unknown.
                    // TODO: find a cleaner solution.
                    PrintWriter pw = log.getWriter(Log.WriterKind.ERROR);
                    pw.println("ClassWriter: Position UNKNOWN in type annotation: " + tc);
                    continue;
                }
            }

            if (tc.position.type.isLocal() != inCode)
                continue;
            if (!tc.position.emitToClassfile())
                continue;
            switch (types.getRetention(tc)) {
            case SOURCE: break;
            case CLASS: invisibles.append(tc); break;
            case RUNTIME: visibles.append(tc); break;
            default: // /* fail soft */ throw new AssertionError(vis);
            }
        }

        int attrCount = 0;
        if (visibles.length() != 0) {
            int attrIndex = writeAttr(names.RuntimeVisibleTypeAnnotations);
            databuf.appendChar(visibles.length());
            for (Attribute.TypeCompound p : visibles)
                writeTypeAnnotation(p);
            endAttr(attrIndex);
            attrCount++;
        }

        if (invisibles.length() != 0) {
            int attrIndex = writeAttr(names.RuntimeInvisibleTypeAnnotations);
            databuf.appendChar(invisibles.length());
            for (Attribute.TypeCompound p : invisibles)
                writeTypeAnnotation(p);
            endAttr(attrIndex);
            attrCount++;
        }

        return attrCount;
    }

    /** A visitor to write an attribute including its leading
     *  single-character marker.
     */
    class AttributeWriter implements Attribute.Visitor {
        public void visitConstant(Attribute.Constant _value) {
            if (_value.type.getTag() == CLASS) {
                Assert.check(_value.value instanceof String);
                String s = (String)_value.value;
                databuf.appendByte('s');
                databuf.appendChar(poolWriter.putName(names.fromString(s)));
            } else {
                switch (_value.type.getTag()) {
                    case BYTE:
                        databuf.appendByte('B');
                        break;
                    case CHAR:
                        databuf.appendByte('C');
                        break;
                    case SHORT:
                        databuf.appendByte('S');
                        break;
                    case INT:
                        databuf.appendByte('I');
                        break;
                    case LONG:
                        databuf.appendByte('J');
                        break;
                    case FLOAT:
                        databuf.appendByte('F');
                        break;
                    case DOUBLE:
                        databuf.appendByte('D');
                        break;
                    case BOOLEAN:
                        databuf.appendByte('Z');
                        break;
                    default:
                        throw new AssertionError(_value.type);
                }
                databuf.appendChar(poolWriter.putConstant(_value.value));
            }
        }
        public void visitEnum(Attribute.Enum e) {
            databuf.appendByte('e');
            databuf.appendChar(poolWriter.putDescriptor(e.value.type));
            databuf.appendChar(poolWriter.putName(e.value.name));
        }
        public void visitClass(Attribute.Class clazz) {
            databuf.appendByte('c');
            databuf.appendChar(poolWriter.putDescriptor(clazz.classType));
        }
        public void visitCompound(Attribute.Compound compound) {
            databuf.appendByte('@');
            writeCompoundAttribute(compound);
        }
        public void visitError(Attribute.Error x) {
            throw new AssertionError(x);
        }
        public void visitArray(Attribute.Array array) {
            databuf.appendByte('[');
            databuf.appendChar(array.values.length);
            for (Attribute a : array.values) {
                a.accept(this);
            }
        }
    }
    AttributeWriter awriter = new AttributeWriter();

    /** Write a compound attribute excluding the '@' marker. */
    void writeCompoundAttribute(Attribute.Compound c) {
        databuf.appendChar(poolWriter.putDescriptor(c.type));
        databuf.appendChar(c.values.length());
        for (Pair<Symbol.MethodSymbol,Attribute> p : c.values) {
            databuf.appendChar(poolWriter.putName(p.fst.name));
            p.snd.accept(awriter);
        }
    }

    void writeTypeAnnotation(Attribute.TypeCompound c) {
        writePosition(c.position);
        writeCompoundAttribute(c);
    }

    void writePosition(TypeAnnotationPosition p) {
        databuf.appendByte(p.type.targetTypeValue()); // TargetType tag is a byte
        switch (p.type) {
        // instanceof
        case INSTANCEOF:
        // new expression
        case NEW:
        // constructor/method reference receiver
        case CONSTRUCTOR_REFERENCE:
        case METHOD_REFERENCE:
            databuf.appendChar(p.offset);
            break;
        // local variable
        case LOCAL_VARIABLE:
        // resource variable
        case RESOURCE_VARIABLE:
            databuf.appendChar(p.lvarOffset.length);  // for table length
            for (int i = 0; i < p.lvarOffset.length; ++i) {
                databuf.appendChar(p.lvarOffset[i]);
                databuf.appendChar(p.lvarLength[i]);
                databuf.appendChar(p.lvarIndex[i]);
            }
            break;
        // exception parameter
        case EXCEPTION_PARAMETER:
            databuf.appendChar(p.getExceptionIndex());
            break;
        // method receiver
        case METHOD_RECEIVER:
            // Do nothing
            break;
        // type parameter
        case CLASS_TYPE_PARAMETER:
        case METHOD_TYPE_PARAMETER:
            databuf.appendByte(p.parameter_index);
            break;
        // type parameter bound
        case CLASS_TYPE_PARAMETER_BOUND:
        case METHOD_TYPE_PARAMETER_BOUND:
            databuf.appendByte(p.parameter_index);
            databuf.appendByte(p.bound_index);
            break;
        // class extends or implements clause
        case CLASS_EXTENDS:
            databuf.appendChar(p.type_index);
            break;
        // throws
        case THROWS:
            databuf.appendChar(p.type_index);
            break;
        // method parameter
        case METHOD_FORMAL_PARAMETER:
            databuf.appendByte(p.parameter_index);
            break;
        // type cast
        case CAST:
        // method/constructor/reference type argument
        case CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
        case METHOD_INVOCATION_TYPE_ARGUMENT:
        case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
        case METHOD_REFERENCE_TYPE_ARGUMENT:
            databuf.appendChar(p.offset);
            databuf.appendByte(p.type_index);
            break;
        // We don't need to worry about these
        case METHOD_RETURN:
        case FIELD:
            break;
        case UNKNOWN:
            throw new AssertionError("jvm.ClassWriter: UNKNOWN target type should never occur!");
        default:
            throw new AssertionError("jvm.ClassWriter: Unknown target type for position: " + p);
        }

        { // Append location data for generics/arrays.
            databuf.appendByte(p.location.size());
            java.util.List<Integer> loc = TypeAnnotationPosition.getBinaryFromTypePath(p.location);
            for (int i : loc)
                databuf.appendByte((byte)i);
        }
    }

/**********************************************************************
 * Writing module attributes
 **********************************************************************/

    /** Write the Module attribute if needed.
     *  Returns the number of attributes written (0 or 1).
     */
    int writeModuleAttribute(ClassSymbol c) {
        ModuleSymbol m = (ModuleSymbol) c.owner;

        int alenIdx = writeAttr(names.Module);

        databuf.appendChar(poolWriter.putModule(m));
        databuf.appendChar(ModuleFlags.value(m.flags)); // module_flags
        databuf.appendChar(m.version != null ? poolWriter.putName(m.version) : 0);

        ListBuffer<RequiresDirective> requires = new ListBuffer<>();
        for (RequiresDirective r: m.requires) {
            if (!r.flags.contains(RequiresFlag.EXTRA))
                requires.add(r);
        }
        databuf.appendChar(requires.size());
        for (RequiresDirective r: requires) {
            databuf.appendChar(poolWriter.putModule(r.module));
            databuf.appendChar(RequiresFlag.value(r.flags));
            databuf.appendChar(r.module.version != null ? poolWriter.putName(r.module.version) : 0);
        }

        List<ExportsDirective> exports = m.exports;
        databuf.appendChar(exports.size());
        for (ExportsDirective e: exports) {
            databuf.appendChar(poolWriter.putPackage(e.packge));
            databuf.appendChar(ExportsFlag.value(e.flags));
            if (e.modules == null) {
                databuf.appendChar(0);
            } else {
                databuf.appendChar(e.modules.size());
                for (ModuleSymbol msym: e.modules) {
                    databuf.appendChar(poolWriter.putModule(msym));
                }
            }
        }

        List<OpensDirective> opens = m.opens;
        databuf.appendChar(opens.size());
        for (OpensDirective o: opens) {
            databuf.appendChar(poolWriter.putPackage(o.packge));
            databuf.appendChar(OpensFlag.value(o.flags));
            if (o.modules == null) {
                databuf.appendChar(0);
            } else {
                databuf.appendChar(o.modules.size());
                for (ModuleSymbol msym: o.modules) {
                    databuf.appendChar(poolWriter.putModule(msym));
                }
            }
        }

        List<UsesDirective> uses = m.uses;
        databuf.appendChar(uses.size());
        for (UsesDirective s: uses) {
            databuf.appendChar(poolWriter.putClass(s.service));
        }

        // temporary fix to merge repeated provides clause for same service;
        // eventually this should be disallowed when analyzing the module,
        // so that each service type only appears once.
        Map<ClassSymbol, Set<ClassSymbol>> mergedProvides = new LinkedHashMap<>();
        for (ProvidesDirective p : m.provides) {
            mergedProvides.computeIfAbsent(p.service, s -> new LinkedHashSet<>()).addAll(p.impls);
        }
        databuf.appendChar(mergedProvides.size());
        mergedProvides.forEach((srvc, impls) -> {
            databuf.appendChar(poolWriter.putClass(srvc));
            databuf.appendChar(impls.size());
            impls.forEach(impl -> databuf.appendChar(poolWriter.putClass(impl)));
        });

        endAttr(alenIdx);
        return 1;
    }

/**********************************************************************
 * Writing Objects
 **********************************************************************/

    /** Write "inner classes" attribute.
     */
    void writeInnerClasses() {
        int alenIdx = writeAttr(names.InnerClasses);
        databuf.appendChar(poolWriter.innerClasses.size());
        for (ClassSymbol inner : poolWriter.innerClasses) {
            inner.markAbstractIfNeeded(types);
            char flags = (char) adjustFlags(inner.flags_field);
            if ((flags & INTERFACE) != 0) flags |= ABSTRACT; // Interfaces are always ABSTRACT
            flags &= ~STRICTFP; //inner classes should not have the strictfp flag set.
            if (dumpInnerClassModifiers) {
                PrintWriter pw = log.getWriter(Log.WriterKind.ERROR);
                pw.println("INNERCLASS  " + inner.name);
                pw.println("---" + flagNames(flags));
            }
            databuf.appendChar(poolWriter.putClass(inner));
            databuf.appendChar(
                inner.owner.kind == TYP && !inner.name.isEmpty() ? poolWriter.putClass((ClassSymbol)inner.owner) : 0);
            databuf.appendChar(
                !inner.name.isEmpty() ? poolWriter.putName(inner.name) : 0);
            databuf.appendChar(flags);
        }
        endAttr(alenIdx);
    }

    int writeRecordAttribute(ClassSymbol csym) {
        int alenIdx = writeAttr(names.Record);
        Scope s = csym.members();
        databuf.appendChar(csym.getRecordComponents().size());
        for (VarSymbol v: csym.getRecordComponents()) {
            //databuf.appendChar(poolWriter.putMember(v.accessor.head.snd));
            databuf.appendChar(poolWriter.putName(v.name));
            databuf.appendChar(poolWriter.putDescriptor(v));
            int acountIdx = beginAttrs();
            int acount = 0;
            acount += writeMemberAttrs(v, true);
            endAttrs(acountIdx, acount);
        }
        endAttr(alenIdx);
        return 1;
    }

    /**
     * Write NestMembers attribute (if needed)
     */
    int writeNestMembersIfNeeded(ClassSymbol csym) {
        ListBuffer<ClassSymbol> nested = new ListBuffer<>();
        listNested(csym, nested);
        Set<ClassSymbol> nestedUnique = new LinkedHashSet<>(nested);
        if (csym.owner.kind == PCK && !nestedUnique.isEmpty()) {
            int alenIdx = writeAttr(names.NestMembers);
            databuf.appendChar(nestedUnique.size());
            for (ClassSymbol s : nestedUnique) {
                databuf.appendChar(poolWriter.putClass(s));
            }
            endAttr(alenIdx);
            return 1;
        }
        return 0;
    }

    /**
     * Write NestHost attribute (if needed)
     */
    int writeNestHostIfNeeded(ClassSymbol csym) {
        if (csym.owner.kind != PCK) {
            int alenIdx = writeAttr(names.NestHost);
            databuf.appendChar(poolWriter.putClass(csym.outermostClass()));
            endAttr(alenIdx);
            return 1;
        }
        return 0;
    }

    private void listNested(Symbol sym, ListBuffer<ClassSymbol> seen) {
        if (sym.kind != TYP) return;
        ClassSymbol csym = (ClassSymbol)sym;
        if (csym.owner.kind != PCK) {
            seen.add(csym);
        }
        if (csym.members() != null) {
            for (Symbol s : sym.members().getSymbols()) {
                listNested(s, seen);
            }
        }
        if (csym.trans_local != null) {
            for (Symbol s : csym.trans_local) {
                listNested(s, seen);
            }
        }
    }

    /** Write "PermittedSubclasses" attribute.
     */
    int writePermittedSubclassesIfNeeded(ClassSymbol csym) {
        if (csym.permitted.nonEmpty()) {
            int alenIdx = writeAttr(names.PermittedSubclasses);
            databuf.appendChar(csym.permitted.size());
            for (Symbol c : csym.permitted) {
                databuf.appendChar(poolWriter.putClass((ClassSymbol) c));
            }
            endAttr(alenIdx);
            return 1;
        }
        return 0;
    }

    /** Write "bootstrapMethods" attribute.
     */
    void writeBootstrapMethods() {
        int alenIdx = writeAttr(names.BootstrapMethods);
        databuf.appendChar(poolWriter.bootstrapMethods.size());
        for (BsmKey bsmKey : poolWriter.bootstrapMethods.keySet()) {
            //write BSM handle
            databuf.appendChar(poolWriter.putConstant(bsmKey.bsm));
            LoadableConstant[] uniqueArgs = bsmKey.staticArgs;
            //write static args length
            databuf.appendChar(uniqueArgs.length);
            //write static args array
            for (LoadableConstant arg : uniqueArgs) {
                databuf.appendChar(poolWriter.putConstant(arg));
            }
        }
        endAttr(alenIdx);
    }

    /** Write field symbol, entering all references into constant pool.
     */
    void writeField(VarSymbol v) {
        int flags = adjustFlags(v.flags());
        databuf.appendChar(flags);
        if (dumpFieldModifiers) {
            PrintWriter pw = log.getWriter(Log.WriterKind.ERROR);
            pw.println("FIELD  " + v.name);
            pw.println("---" + flagNames(v.flags()));
        }
        databuf.appendChar(poolWriter.putName(v.name));
        databuf.appendChar(poolWriter.putDescriptor(v));
        int acountIdx = beginAttrs();
        int acount = 0;
        if (v.getConstValue() != null) {
            int alenIdx = writeAttr(names.ConstantValue);
            databuf.appendChar(poolWriter.putConstant(v.getConstValue()));
            endAttr(alenIdx);
            acount++;
        }
        acount += writeMemberAttrs(v, false);
        acount += writeExtraAttributes(v);
        endAttrs(acountIdx, acount);
    }

    /** Write method symbol, entering all references into constant pool.
     */
    void writeMethod(MethodSymbol m) {
        int flags = adjustFlags(m.flags());
        databuf.appendChar(flags);
        if (dumpMethodModifiers) {
            PrintWriter pw = log.getWriter(Log.WriterKind.ERROR);
            pw.println("METHOD  " + m.name);
            pw.println("---" + flagNames(m.flags()));
        }
        databuf.appendChar(poolWriter.putName(m.name));
        databuf.appendChar(poolWriter.putDescriptor(m));
        int acountIdx = beginAttrs();
        int acount = 0;
        if (m.code != null) {
            int alenIdx = writeAttr(names.Code);
            writeCode(m.code);
            m.code = null; // to conserve space
            endAttr(alenIdx);
            acount++;
        }
        List<Type> thrown = m.erasure(types).getThrownTypes();
        if (thrown.nonEmpty()) {
            int alenIdx = writeAttr(names.Exceptions);
            databuf.appendChar(thrown.length());
            for (List<Type> l = thrown; l.nonEmpty(); l = l.tail)
                databuf.appendChar(poolWriter.putClass(l.head));
            endAttr(alenIdx);
            acount++;
        }
        if (m.defaultValue != null) {
            int alenIdx = writeAttr(names.AnnotationDefault);
            m.defaultValue.accept(awriter);
            endAttr(alenIdx);
            acount++;
        }
        if (target.hasMethodParameters() && (options.isSet(PARAMETERS) || m.isConstructor() && (m.flags_field & RECORD) != 0)) {
            if (!m.isLambdaMethod()) // Per JDK-8138729, do not emit parameters table for lambda bodies.
                acount += writeMethodParametersAttr(m);
        }
        acount += writeMemberAttrs(m, false);
        if (!m.isLambdaMethod())
            acount += writeParameterAttrs(m.params);
        acount += writeExtraAttributes(m);
        endAttrs(acountIdx, acount);
    }

    /** Write code attribute of method.
     */
    void writeCode(Code code) {
        databuf.appendChar(code.max_stack);
        databuf.appendChar(code.max_locals);
        databuf.appendInt(code.cp);
        databuf.appendBytes(code.code, 0, code.cp);
        databuf.appendChar(code.catchInfo.length());
        for (List<char[]> l = code.catchInfo.toList();
             l.nonEmpty();
             l = l.tail) {
            for (int i = 0; i < l.head.length; i++)
                databuf.appendChar(l.head[i]);
        }
        int acountIdx = beginAttrs();
        int acount = 0;

        if (code.lineInfo.nonEmpty()) {
            int alenIdx = writeAttr(names.LineNumberTable);
            databuf.appendChar(code.lineInfo.length());
            for (List<char[]> l = code.lineInfo.reverse();
                 l.nonEmpty();
                 l = l.tail)
                for (int i = 0; i < l.head.length; i++)
                    databuf.appendChar(l.head[i]);
            endAttr(alenIdx);
            acount++;
        }

        if (genCrt && (code.crt != null)) {
            CRTable crt = code.crt;
            int alenIdx = writeAttr(names.CharacterRangeTable);
            int crtIdx = beginAttrs();
            int crtEntries = crt.writeCRT(databuf, code.lineMap, log);
            endAttrs(crtIdx, crtEntries);
            endAttr(alenIdx);
            acount++;
        }

        // counter for number of generic local variables
        if (code.varDebugInfo && code.varBufferSize > 0) {
            int nGenericVars = 0;
            int alenIdx = writeAttr(names.LocalVariableTable);
            databuf.appendChar(code.getLVTSize());
            for (int i=0; i<code.varBufferSize; i++) {
                Code.LocalVar var = code.varBuffer[i];

                for (Code.LocalVar.Range r: var.aliveRanges) {
                    // write variable info
                    Assert.check(r.start_pc >= 0
                            && r.start_pc <= code.cp);
                    databuf.appendChar(r.start_pc);
                    Assert.check(r.length > 0
                            && (r.start_pc + r.length) <= code.cp);
                    databuf.appendChar(r.length);
                    VarSymbol sym = var.sym;
                    databuf.appendChar(poolWriter.putName(sym.name));
                    databuf.appendChar(poolWriter.putDescriptor(sym));
                    databuf.appendChar(var.reg);
                    if (needsLocalVariableTypeEntry(var.sym.type)) {
                        nGenericVars++;
                    }
                }
            }
            endAttr(alenIdx);
            acount++;

            if (nGenericVars > 0) {
                alenIdx = writeAttr(names.LocalVariableTypeTable);
                databuf.appendChar(nGenericVars);
                int count = 0;

                for (int i=0; i<code.varBufferSize; i++) {
                    Code.LocalVar var = code.varBuffer[i];
                    VarSymbol sym = var.sym;
                    if (!needsLocalVariableTypeEntry(sym.type))
                        continue;
                    for (Code.LocalVar.Range r : var.aliveRanges) {
                        // write variable info
                        databuf.appendChar(r.start_pc);
                        databuf.appendChar(r.length);
                        databuf.appendChar(poolWriter.putName(sym.name));
                        databuf.appendChar(poolWriter.putSignature(sym));
                        databuf.appendChar(var.reg);
                        count++;
                    }
                }
                Assert.check(count == nGenericVars);
                endAttr(alenIdx);
                acount++;
            }
        }

        if (code.stackMapBufferSize > 0) {
            if (debugstackmap) System.out.println("Stack map for " + code.meth);
            int alenIdx = writeAttr(code.stackMap.getAttributeName(names));
            writeStackMap(code);
            endAttr(alenIdx);
            acount++;
        }

        acount += writeTypeAnnotations(code.meth.getRawTypeAttributes(), true);

        endAttrs(acountIdx, acount);
    }
    //where
    private boolean needsLocalVariableTypeEntry(Type t) {
        //a local variable needs a type-entry if its type T is generic
        //(i.e. |T| != T) and if it's not an non-denotable type (non-denotable
        // types are not supported in signature attribute grammar!)
        return !types.isSameType(t, types.erasure(t)) &&
                check.checkDenotable(t);
    }

    void writeStackMap(Code code) {
        int nframes = code.stackMapBufferSize;
        if (debugstackmap) System.out.println(" nframes = " + nframes);
        databuf.appendChar(nframes);

        switch (code.stackMap) {
        case CLDC:
            for (int i=0; i<nframes; i++) {
                if (debugstackmap) System.out.print("  " + i + ":");
                Code.StackMapFrame frame = code.stackMapBuffer[i];

                // output PC
                if (debugstackmap) System.out.print(" pc=" + frame.pc);
                databuf.appendChar(frame.pc);

                // output locals
                int localCount = 0;
                for (int j=0; j<frame.locals.length;
                     j += Code.width(frame.locals[j])) {
                    localCount++;
                }
                if (debugstackmap) System.out.print(" nlocals=" +
                                                    localCount);
                databuf.appendChar(localCount);
                for (int j=0; j<frame.locals.length;
                     j += Code.width(frame.locals[j])) {
                    if (debugstackmap) System.out.print(" local[" + j + "]=");
                    writeStackMapType(frame.locals[j]);
                }

                // output stack
                int stackCount = 0;
                for (int j=0; j<frame.stack.length;
                     j += Code.width(frame.stack[j])) {
                    stackCount++;
                }
                if (debugstackmap) System.out.print(" nstack=" +
                                                    stackCount);
                databuf.appendChar(stackCount);
                for (int j=0; j<frame.stack.length;
                     j += Code.width(frame.stack[j])) {
                    if (debugstackmap) System.out.print(" stack[" + j + "]=");
                    writeStackMapType(frame.stack[j]);
                }
                if (debugstackmap) System.out.println();
            }
            break;
        case JSR202: {
            Assert.checkNull(code.stackMapBuffer);
            for (int i=0; i<nframes; i++) {
                if (debugstackmap) System.out.print("  " + i + ":");
                StackMapTableFrame frame = code.stackMapTableBuffer[i];
                frame.write(this);
                if (debugstackmap) System.out.println();
            }
            break;
        }
        default:
            throw new AssertionError("Unexpected stackmap format value");
        }
    }

        //where
        void writeStackMapType(Type t) {
            if (t == null) {
                if (debugstackmap) System.out.print("empty");
                databuf.appendByte(0);
            }
            else switch(t.getTag()) {
            case BYTE:
            case CHAR:
            case SHORT:
            case INT:
            case BOOLEAN:
                if (debugstackmap) System.out.print("int");
                databuf.appendByte(1);
                break;
            case FLOAT:
                if (debugstackmap) System.out.print("float");
                databuf.appendByte(2);
                break;
            case DOUBLE:
                if (debugstackmap) System.out.print("double");
                databuf.appendByte(3);
                break;
            case LONG:
                if (debugstackmap) System.out.print("long");
                databuf.appendByte(4);
                break;
            case BOT: // null
                if (debugstackmap) System.out.print("null");
                databuf.appendByte(5);
                break;
            case CLASS:
            case ARRAY:
            case TYPEVAR:
                if (debugstackmap) System.out.print("object(" + types.erasure(t).tsym + ")");
                databuf.appendByte(7);
                databuf.appendChar(poolWriter.putClass(types.erasure(t)));
                break;
            case UNINITIALIZED_THIS:
                if (debugstackmap) System.out.print("uninit_this");
                databuf.appendByte(6);
                break;
            case UNINITIALIZED_OBJECT:
                { UninitializedType uninitType = (UninitializedType)t;
                databuf.appendByte(8);
                if (debugstackmap) System.out.print("uninit_object@" + uninitType.offset);
                databuf.appendChar(uninitType.offset);
                }
                break;
            default:
                throw new AssertionError();
            }
        }

    /** An entry in the JSR202 StackMapTable */
    abstract static class StackMapTableFrame {
        abstract int getFrameType();

        void write(ClassWriter writer) {
            int frameType = getFrameType();
            writer.databuf.appendByte(frameType);
            if (writer.debugstackmap) System.out.print(" frame_type=" + frameType);
        }

        static class SameFrame extends StackMapTableFrame {
            final int offsetDelta;
            SameFrame(int offsetDelta) {
                this.offsetDelta = offsetDelta;
            }
            int getFrameType() {
                return (offsetDelta < SAME_FRAME_SIZE) ? offsetDelta : SAME_FRAME_EXTENDED;
            }
            @Override
            void write(ClassWriter writer) {
                super.write(writer);
                if (getFrameType() == SAME_FRAME_EXTENDED) {
                    writer.databuf.appendChar(offsetDelta);
                    if (writer.debugstackmap){
                        System.out.print(" offset_delta=" + offsetDelta);
                    }
                }
            }
        }

        static class SameLocals1StackItemFrame extends StackMapTableFrame {
            final int offsetDelta;
            final Type stack;
            SameLocals1StackItemFrame(int offsetDelta, Type stack) {
                this.offsetDelta = offsetDelta;
                this.stack = stack;
            }
            int getFrameType() {
                return (offsetDelta < SAME_FRAME_SIZE) ?
                       (SAME_FRAME_SIZE + offsetDelta) :
                       SAME_LOCALS_1_STACK_ITEM_EXTENDED;
            }
            @Override
            void write(ClassWriter writer) {
                super.write(writer);
                if (getFrameType() == SAME_LOCALS_1_STACK_ITEM_EXTENDED) {
                    writer.databuf.appendChar(offsetDelta);
                    if (writer.debugstackmap) {
                        System.out.print(" offset_delta=" + offsetDelta);
                    }
                }
                if (writer.debugstackmap) {
                    System.out.print(" stack[" + 0 + "]=");
                }
                writer.writeStackMapType(stack);
            }
        }

        static class ChopFrame extends StackMapTableFrame {
            final int frameType;
            final int offsetDelta;
            ChopFrame(int frameType, int offsetDelta) {
                this.frameType = frameType;
                this.offsetDelta = offsetDelta;
            }
            int getFrameType() { return frameType; }
            @Override
            void write(ClassWriter writer) {
                super.write(writer);
                writer.databuf.appendChar(offsetDelta);
                if (writer.debugstackmap) {
                    System.out.print(" offset_delta=" + offsetDelta);
                }
            }
        }

        static class AppendFrame extends StackMapTableFrame {
            final int frameType;
            final int offsetDelta;
            final Type[] locals;
            AppendFrame(int frameType, int offsetDelta, Type[] locals) {
                this.frameType = frameType;
                this.offsetDelta = offsetDelta;
                this.locals = locals;
            }
            int getFrameType() { return frameType; }
            @Override
            void write(ClassWriter writer) {
                super.write(writer);
                writer.databuf.appendChar(offsetDelta);
                if (writer.debugstackmap) {
                    System.out.print(" offset_delta=" + offsetDelta);
                }
                for (int i=0; i<locals.length; i++) {
                     if (writer.debugstackmap) System.out.print(" locals[" + i + "]=");
                     writer.writeStackMapType(locals[i]);
                }
            }
        }

        static class FullFrame extends StackMapTableFrame {
            final int offsetDelta;
            final Type[] locals;
            final Type[] stack;
            FullFrame(int offsetDelta, Type[] locals, Type[] stack) {
                this.offsetDelta = offsetDelta;
                this.locals = locals;
                this.stack = stack;
            }
            int getFrameType() { return FULL_FRAME; }
            @Override
            void write(ClassWriter writer) {
                super.write(writer);
                writer.databuf.appendChar(offsetDelta);
                writer.databuf.appendChar(locals.length);
                if (writer.debugstackmap) {
                    System.out.print(" offset_delta=" + offsetDelta);
                    System.out.print(" nlocals=" + locals.length);
                }
                for (int i=0; i<locals.length; i++) {
                    if (writer.debugstackmap) System.out.print(" locals[" + i + "]=");
                    writer.writeStackMapType(locals[i]);
                }

                writer.databuf.appendChar(stack.length);
                if (writer.debugstackmap) { System.out.print(" nstack=" + stack.length); }
                for (int i=0; i<stack.length; i++) {
                    if (writer.debugstackmap) System.out.print(" stack[" + i + "]=");
                    writer.writeStackMapType(stack[i]);
                }
            }
        }

       /** Compare this frame with the previous frame and produce
        *  an entry of compressed stack map frame. */
        static StackMapTableFrame getInstance(Code.StackMapFrame this_frame,
                                              int prev_pc,
                                              Type[] prev_locals,
                                              Types types) {
            Type[] locals = this_frame.locals;
            Type[] stack = this_frame.stack;
            int offset_delta = this_frame.pc - prev_pc - 1;
            if (stack.length == 1) {
                if (locals.length == prev_locals.length
                    && compare(prev_locals, locals, types) == 0) {
                    return new SameLocals1StackItemFrame(offset_delta, stack[0]);
                }
            } else if (stack.length == 0) {
                int diff_length = compare(prev_locals, locals, types);
                if (diff_length == 0) {
                    return new SameFrame(offset_delta);
                } else if (-MAX_LOCAL_LENGTH_DIFF < diff_length && diff_length < 0) {
                    // APPEND
                    Type[] local_diff = new Type[-diff_length];
                    for (int i=prev_locals.length, j=0; i<locals.length; i++,j++) {
                        local_diff[j] = locals[i];
                    }
                    return new AppendFrame(SAME_FRAME_EXTENDED - diff_length,
                                           offset_delta,
                                           local_diff);
                } else if (0 < diff_length && diff_length < MAX_LOCAL_LENGTH_DIFF) {
                    // CHOP
                    return new ChopFrame(SAME_FRAME_EXTENDED - diff_length,
                                         offset_delta);
                }
            }
            // FULL_FRAME
            return new FullFrame(offset_delta, locals, stack);
        }

        static boolean isInt(Type t) {
            return (t.getTag().isStrictSubRangeOf(INT)  || t.hasTag(BOOLEAN));
        }

        static boolean isSameType(Type t1, Type t2, Types types) {
            if (t1 == null) { return t2 == null; }
            if (t2 == null) { return false; }

            if (isInt(t1) && isInt(t2)) { return true; }

            if (t1.hasTag(UNINITIALIZED_THIS)) {
                return t2.hasTag(UNINITIALIZED_THIS);
            } else if (t1.hasTag(UNINITIALIZED_OBJECT)) {
                if (t2.hasTag(UNINITIALIZED_OBJECT)) {
                    return ((UninitializedType)t1).offset == ((UninitializedType)t2).offset;
                } else {
                    return false;
                }
            } else if (t2.hasTag(UNINITIALIZED_THIS) || t2.hasTag(UNINITIALIZED_OBJECT)) {
                return false;
            }

            return types.isSameType(t1, t2);
        }

        static int compare(Type[] arr1, Type[] arr2, Types types) {
            int diff_length = arr1.length - arr2.length;
            if (diff_length > MAX_LOCAL_LENGTH_DIFF || diff_length < -MAX_LOCAL_LENGTH_DIFF) {
                return Integer.MAX_VALUE;
            }
            int len = (diff_length > 0) ? arr2.length : arr1.length;
            for (int i=0; i<len; i++) {
                if (!isSameType(arr1[i], arr2[i], types)) {
                    return Integer.MAX_VALUE;
                }
            }
            return diff_length;
        }
    }

    void writeFields(Scope s) {
        // process them in reverse sibling order;
        // i.e., process them in declaration order.
        List<VarSymbol> vars = List.nil();
        for (Symbol sym : s.getSymbols(NON_RECURSIVE)) {
            if (sym.kind == VAR) vars = vars.prepend((VarSymbol)sym);
        }
        while (vars.nonEmpty()) {
            writeField(vars.head);
            vars = vars.tail;
        }
    }

    void writeMethods(Scope s) {
        List<MethodSymbol> methods = List.nil();
        for (Symbol sym : s.getSymbols(NON_RECURSIVE)) {
            if (sym.kind == MTH && (sym.flags() & HYPOTHETICAL) == 0)
                methods = methods.prepend((MethodSymbol)sym);
        }
        while (methods.nonEmpty()) {
            writeMethod(methods.head);
            methods = methods.tail;
        }
    }

    /** Emit a class file for a given class.
     *  @param c      The class from which a class file is generated.
     */
    public JavaFileObject writeClass(ClassSymbol c)
        throws IOException, PoolOverflow, StringOverflow
    {
        String name = (c.owner.kind == MDL ? c.name : c.flatname).toString();
        Location outLocn;
        if (multiModuleMode) {
            ModuleSymbol msym = c.owner.kind == MDL ? (ModuleSymbol) c.owner : c.packge().modle;
            outLocn = fileManager.getLocationForModule(CLASS_OUTPUT, msym.name.toString());
        } else {
            outLocn = CLASS_OUTPUT;
        }
        JavaFileObject outFile
            = fileManager.getJavaFileForOutput(outLocn,
                                               name,
                                               JavaFileObject.Kind.CLASS,
                                               c.sourcefile);
        OutputStream out = outFile.openOutputStream();
        try {
            writeClassFile(out, c);
            if (verbose)
                log.printVerbose("wrote.file", outFile.getName());
            out.close();
            out = null;
        } catch (InvalidSignatureException ex) {
            log.error(Errors.CannotGenerateClass(c, Fragments.IllegalSignature(c, ex.type())));
        } finally {
            if (out != null) {
                // if we are propagating an exception, delete the file
                out.close();
                outFile.delete();
                outFile = null;
            }
        }
        return outFile; // may be null if write failed
    }

    /** Write class `c' to outstream `out'.
     */
    public void writeClassFile(OutputStream out, ClassSymbol c)
        throws IOException, PoolOverflow, StringOverflow {
        Assert.check((c.flags() & COMPOUND) == 0);
        databuf.reset();
        poolbuf.reset();

        Type supertype = types.supertype(c.type);
        List<Type> interfaces = types.interfaces(c.type);
        List<Type> typarams = c.type.getTypeArguments();

        int flags;
        if (c.owner.kind == MDL) {
            flags = ACC_MODULE;
        } else {
            flags = adjustFlags(c.flags() & ~DEFAULT);
            if ((flags & PROTECTED) != 0) flags |= PUBLIC;
            flags = flags & ClassFlags & ~STRICTFP;
            if ((flags & INTERFACE) == 0) flags |= ACC_SUPER;
        }

        if (dumpClassModifiers) {
            PrintWriter pw = log.getWriter(Log.WriterKind.ERROR);
            pw.println();
            pw.println("CLASSFILE  " + c.getQualifiedName());
            pw.println("---" + flagNames(flags));
        }
        databuf.appendChar(flags);

        if (c.owner.kind == MDL) {
            PackageSymbol unnamed = ((ModuleSymbol) c.owner).unnamedPackage;
            databuf.appendChar(poolWriter.putClass(new ClassSymbol(0, names.module_info, unnamed)));
        } else {
            databuf.appendChar(poolWriter.putClass(c));
        }
        databuf.appendChar(supertype.hasTag(CLASS) ? poolWriter.putClass((ClassSymbol)supertype.tsym) : 0);
        databuf.appendChar(interfaces.length());
        for (List<Type> l = interfaces; l.nonEmpty(); l = l.tail)
            databuf.appendChar(poolWriter.putClass((ClassSymbol)l.head.tsym));
        int fieldsCount = 0;
        int methodsCount = 0;
        for (Symbol sym : c.members().getSymbols(NON_RECURSIVE)) {
            switch (sym.kind) {
            case VAR: fieldsCount++; break;
            case MTH: if ((sym.flags() & HYPOTHETICAL) == 0) methodsCount++;
                      break;
            case TYP: poolWriter.enterInner((ClassSymbol)sym); break;
            default : Assert.error();
            }
        }

        if (c.trans_local != null) {
            for (ClassSymbol local : c.trans_local) {
                poolWriter.enterInner(local);
            }
        }

        databuf.appendChar(fieldsCount);
        writeFields(c.members());
        databuf.appendChar(methodsCount);
        writeMethods(c.members());

        int acountIdx = beginAttrs();
        int acount = 0;

        boolean sigReq =
            typarams.length() != 0 || supertype.allparams().length() != 0;
        for (List<Type> l = interfaces; !sigReq && l.nonEmpty(); l = l.tail)
            sigReq = l.head.allparams().length() != 0;
        if (sigReq) {
            int alenIdx = writeAttr(names.Signature);
            databuf.appendChar(poolWriter.putSignature(c));
            endAttr(alenIdx);
            acount++;
        }

        if (c.sourcefile != null && emitSourceFile) {
            int alenIdx = writeAttr(names.SourceFile);
            // WHM 6/29/1999: Strip file path prefix.  We do it here at
            // the last possible moment because the sourcefile may be used
            // elsewhere in error diagnostics. Fixes 4241573.
            String simpleName = PathFileObject.getSimpleName(c.sourcefile);
            databuf.appendChar(poolWriter.putName(names.fromString(simpleName)));
            endAttr(alenIdx);
            acount++;
        }

        if (genCrt) {
            // Append SourceID attribute
            int alenIdx = writeAttr(names.SourceID);
            databuf.appendChar(poolWriter.putName(names.fromString(Long.toString(getLastModified(c.sourcefile)))));
            endAttr(alenIdx);
            acount++;
            // Append CompilationID attribute
            alenIdx = writeAttr(names.CompilationID);
            databuf.appendChar(poolWriter.putName(names.fromString(Long.toString(System.currentTimeMillis()))));
            endAttr(alenIdx);
            acount++;
        }

        acount += writeFlagAttrs(c.flags());
        acount += writeJavaAnnotations(c.getRawAttributes());
        acount += writeTypeAnnotations(c.getRawTypeAttributes(), false);
        acount += writeEnclosingMethodAttribute(c);
        if (c.owner.kind == MDL) {
            acount += writeModuleAttribute(c);
            acount += writeFlagAttrs(c.owner.flags() & ~DEPRECATED);
        }
        acount += writeExtraClassAttributes(c);
        acount += writeExtraAttributes(c);

        poolbuf.appendInt(JAVA_MAGIC);
        if (preview.isEnabled() && preview.usesPreview(c.sourcefile)) {
            poolbuf.appendChar(ClassFile.PREVIEW_MINOR_VERSION);
        } else {
            poolbuf.appendChar(target.minorVersion);
        }
        poolbuf.appendChar(target.majorVersion);

        if (c.owner.kind != MDL) {
            if (target.hasNestmateAccess()) {
                acount += writeNestMembersIfNeeded(c);
                acount += writeNestHostIfNeeded(c);
            }
        }

        if (c.isRecord()) {
            acount += writeRecordAttribute(c);
        }

        if (target.hasSealedClasses()) {
            acount += writePermittedSubclassesIfNeeded(c);
        }

        if (!poolWriter.bootstrapMethods.isEmpty()) {
            writeBootstrapMethods();
            acount++;
        }

        if (!poolWriter.innerClasses.isEmpty()) {
            writeInnerClasses();
            acount++;
        }

        endAttrs(acountIdx, acount);

        out.write(poolbuf.elems, 0, poolbuf.length);

        poolWriter.writePool(out);
        poolWriter.reset(); // to save space

        out.write(databuf.elems, 0, databuf.length);
    }

     /**Allows subclasses to write additional class attributes
      *
      * @return the number of attributes written
      */
    protected int writeExtraClassAttributes(ClassSymbol c) {
        return 0;
    }

    /**Allows friends to write additional attributes
     *
     * @return the number of attributes written
     */
    protected int writeExtraAttributes(Symbol sym) {
        int i = 0;
        for (ToIntFunction<Symbol> hook : extraAttributeHooks) {
            i += hook.applyAsInt(sym);
        }
        return i;
    }

    int adjustFlags(final long flags) {
        int result = (int)flags;

        // Elide strictfp bit in class files
        if (target.obsoleteAccStrict())
            result &= ~STRICTFP;

        if ((flags & BRIDGE) != 0)
            result |= ACC_BRIDGE;
        if ((flags & VARARGS) != 0)
            result |= ACC_VARARGS;
        if ((flags & DEFAULT) != 0)
            result &= ~ABSTRACT;
        return result;
    }

    long getLastModified(FileObject filename) {
        long mod = 0;
        try {
            mod = filename.getLastModified();
        } catch (SecurityException e) {
            throw new AssertionError("CRT: couldn't get source file modification date: " + e.getMessage());
        }
        return mod;
    }
}
