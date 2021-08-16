/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tools.javac.code.Kinds.Kind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.DynamicMethodSymbol;
import com.sun.tools.javac.code.Symbol.MethodHandleSymbol;
import com.sun.tools.javac.code.Symbol.ModuleSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.jvm.ClassWriter.PoolOverflow;
import com.sun.tools.javac.jvm.ClassWriter.StringOverflow;
import com.sun.tools.javac.jvm.PoolConstant.LoadableConstant;
import com.sun.tools.javac.jvm.PoolConstant.LoadableConstant.BasicConstant;
import com.sun.tools.javac.jvm.PoolConstant.Dynamic;
import com.sun.tools.javac.jvm.PoolConstant.Dynamic.BsmKey;
import com.sun.tools.javac.jvm.PoolConstant.NameAndType;
import com.sun.tools.javac.util.ByteBuffer;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import static com.sun.tools.javac.code.Kinds.Kind.TYP;
import static com.sun.tools.javac.code.TypeTag.ARRAY;
import static com.sun.tools.javac.code.TypeTag.CLASS;
import static com.sun.tools.javac.jvm.ClassFile.CONSTANT_Class;
import static com.sun.tools.javac.jvm.ClassFile.CONSTANT_MethodType;
import static com.sun.tools.javac.jvm.ClassFile.externalize;

/**
 * Pool interface towards {@code ClassWriter}. Exposes methods to encode and write javac entities
 * into the constant pool.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class PoolWriter {

    /** Max number of constant pool entries. */
    public static final int MAX_ENTRIES = 0xFFFF;

    /** Max number of char in a string constant. */
    public static final int MAX_STRING_LENGTH = 0xFFFF;

    private static final int POOL_BUF_SIZE = 0x7fff;

    private final Types types;

    private final Names names;

    /** Pool helper **/
    final WriteablePoolHelper pool;

    /** Sole signature generator */
    final SharedSignatureGenerator signatureGen;

    /** The inner classes to be written, as an ordered set (enclosing first). */
    LinkedHashSet<ClassSymbol> innerClasses = new LinkedHashSet<>();

    /** The list of entries in the BootstrapMethods attribute. */
    Map<BsmKey, Integer> bootstrapMethods = new LinkedHashMap<>();

    public PoolWriter(Types types, Names names) {
        this.types = types;
        this.names = names;
        this.signatureGen = new SharedSignatureGenerator(types);
        this.pool = new WriteablePoolHelper();
    }

    /**
     * Puts a class symbol into the pool and return its index.
     */
    int putClass(ClassSymbol csym) {
        return putClass(csym.type);
    }

    /**
     * Puts a type into the pool and return its index. The type could be either a class, a type variable
     * or an array type.
     */
    int putClass(Type t) {
        return pool.writeIfNeeded(types.erasure(t));
    }

    /**
     * Puts a member reference into the constant pool. Valid members are either field or method symbols.
     */
    int putMember(Symbol s) {
        return pool.writeIfNeeded(s);
    }

    /**
     * Puts a dynamic reference into the constant pool and return its index.
     */
    int putDynamic(Dynamic d) {
        return pool.writeIfNeeded(d);
    }

    /**
     * Puts a field or method descriptor into the constant pool and return its index.
     */
    int putDescriptor(Type t) {
        return putName(typeSig(types.erasure(t)));
    }

    /**
     * Puts a field or method descriptor into the constant pool and return its index.
     */
    int putDescriptor(Symbol s) {
        return putDescriptor(descriptorType(s));
    }

    /**
     * Puts a signature (see {@code Signature} attribute in JVMS 4.4) into the constant pool and
     * return its index.
     */
    int putSignature(Symbol s) {
        if (s.kind == TYP) {
            return putName(classSig(s.type));
        } else {
            return putName(typeSig(s.type));
        }
    }

    /**
     * Puts a constant value into the pool and return its index. Supported values are int, float, long,
     * double and String.
     */
    int putConstant(Object o) {
        if (o instanceof Integer intVal) {
            return putConstant(LoadableConstant.Int(intVal));
        } else if (o instanceof Float floatVal) {
            return putConstant(LoadableConstant.Float(floatVal));
        } else if (o instanceof Long longVal) {
            return putConstant(LoadableConstant.Long(longVal));
        } else if (o instanceof Double doubleVal) {
            return putConstant(LoadableConstant.Double(doubleVal));
        } else if (o instanceof String strVal) {
            return putConstant(LoadableConstant.String(strVal));
        } else {
            throw new AssertionError("unexpected constant: " + o);
        }
    }

    /**
     * Puts a constant into the pool and return its index.
     */
    int putConstant(LoadableConstant c) {
        switch (c.poolTag()) {
            case CONSTANT_Class:
                return putClass((Type)c);
            case CONSTANT_MethodType:
                return pool.writeIfNeeded(types.erasure((Type)c));
            default:
                return pool.writeIfNeeded(c);
        }
    }

    int putName(Name name) {
        return pool.writeIfNeeded(name);
    }

    /**
     * Puts a name and type pair into the pool and returns its index.
     */
    int putNameAndType(Symbol s) {
        return pool.writeIfNeeded(new NameAndType(s.name, descriptorType(s)));
    }

    /**
     * Puts a package entry into the pool and returns its index.
     */
    int putPackage(PackageSymbol pkg) {
        return pool.writeIfNeeded(pkg);
    }

    /**
     * Puts a module entry into the pool and returns its index.
     */
    int putModule(ModuleSymbol mod) {
        return pool.writeIfNeeded(mod);
    }

    /**
     * Enter an inner class into the `innerClasses' set.
     */
    void enterInner(ClassSymbol c) {
        if (c.type.isCompound()) {
            throw new AssertionError("Unexpected intersection type: " + c.type);
        }
        c.complete();
        if (c.owner.enclClass() != null && !innerClasses.contains(c)) {
            enterInner(c.owner.enclClass());
            innerClasses.add(c);
        }
    }

    /**
     * Create a new Utf8 entry representing a descriptor for given (member) symbol.
     */
    private Type descriptorType(Symbol s) {
        return s.kind == Kind.MTH ? s.externalType(types) : s.erasure(types);
    }

    private int makeBootstrapEntry(Dynamic dynamic) {
        BsmKey bsmKey = dynamic.bsmKey(types);

        // Figure out the index for existing BSM; create a new BSM if no key
        Integer index = bootstrapMethods.get(bsmKey);
        if (index == null) {
            index = bootstrapMethods.size();
            bootstrapMethods.put(bsmKey, index);
        }

        return index;
    }

    /**
     * Write pool contents into given byte buffer.
     */
    void writePool(OutputStream out) throws IOException, PoolOverflow {
        if (pool.overflowString != null) {
            throw new StringOverflow(pool.overflowString);
        }
        int size = size();
        if (size > MAX_ENTRIES) {
            throw new PoolOverflow();
        }
        out.write(size >> 8);
        out.write(size);
        out.write(pool.poolbuf.elems, 0, pool.poolbuf.length);
    }

    /**
     * Signature Generation
     */
    class SharedSignatureGenerator extends Types.SignatureGenerator {

        /**
         * An output buffer for type signatures.
         */
        ByteBuffer sigbuf = new ByteBuffer();

        SharedSignatureGenerator(Types types) {
            super(types);
        }

        /**
         * Assemble signature of given type in string buffer.
         * Check for uninitialized types before calling the general case.
         */
        @Override
        public void assembleSig(Type type) {
            switch (type.getTag()) {
                case UNINITIALIZED_THIS:
                case UNINITIALIZED_OBJECT:
                    // we don't yet have a spec for uninitialized types in the
                    // local variable table
                    assembleSig(types.erasure(((UninitializedType)type).qtype));
                    break;
                default:
                    super.assembleSig(type);
            }
        }

        @Override
        protected void append(char ch) {
            sigbuf.appendByte(ch);
        }

        @Override
        protected void append(byte[] ba) {
            sigbuf.appendBytes(ba);
        }

        @Override
        protected void append(Name name) {
            sigbuf.appendName(name);
        }

        @Override
        protected void classReference(ClassSymbol c) {
            enterInner(c);
        }

        protected void reset() {
            sigbuf.reset();
        }

        protected Name toName() {
            return sigbuf.toName(names);
        }
    }

    class WriteablePoolHelper {

        /** Pool entries. */
        private final Map<Object, Integer> keysToPos = new HashMap<>(64);

        final ByteBuffer poolbuf = new ByteBuffer(POOL_BUF_SIZE);

        int currentIndex = 1;

        ArrayDeque<PoolConstant> todo = new ArrayDeque<>();

        String overflowString = null;

        private <P extends PoolConstant> int writeIfNeeded(P p) {
            Object key = p.poolKey(types);
            Integer index = keysToPos.get(key);
            if (index == null) {
                keysToPos.put(key, index = currentIndex++);
                boolean first = todo.isEmpty();
                todo.addLast(p);
                if (first) {
                    while (!todo.isEmpty()) {
                        writeConstant(todo.peekFirst());
                        todo.removeFirst();
                    }
                }
            }
            return index;
        }

        void writeConstant(PoolConstant c) {
            int tag = c.poolTag();
            switch (tag) {
                case ClassFile.CONSTANT_Class: {
                    Type ct = (Type)c;
                    Name name = ct.hasTag(ARRAY) ?
                            typeSig(ct) :
                            names.fromUtf(externalize(ct.tsym.flatName()));
                    poolbuf.appendByte(tag);
                    poolbuf.appendChar(putName(name));
                    if (ct.hasTag(CLASS)) {
                        enterInner((ClassSymbol)ct.tsym);
                    }
                    break;
                }
                case ClassFile.CONSTANT_Utf8: {
                    Name name = (Name)c;
                    poolbuf.appendByte(tag);
                    byte[] bs = name.toUtf();
                    poolbuf.appendChar(bs.length);
                    poolbuf.appendBytes(bs, 0, bs.length);
                    if (overflowString == null && bs.length > MAX_STRING_LENGTH) {
                        //report error only once
                        overflowString = new String(bs);
                    }
                    break;
                }
                case ClassFile.CONSTANT_InterfaceMethodref:
                case ClassFile.CONSTANT_Methodref:
                case ClassFile.CONSTANT_Fieldref: {
                    Symbol sym = (Symbol)c;
                    poolbuf.appendByte(tag);
                    poolbuf.appendChar(putClass((ClassSymbol)sym.owner));
                    poolbuf.appendChar(putNameAndType(sym));
                    break;
                }
                case ClassFile.CONSTANT_Package: {
                    PackageSymbol pkg = (PackageSymbol)c;
                    Name pkgName = names.fromUtf(externalize(pkg.flatName()));
                    poolbuf.appendByte(tag);
                    poolbuf.appendChar(putName(pkgName));
                    break;
                }
                case ClassFile.CONSTANT_Module: {
                    ModuleSymbol mod = (ModuleSymbol)c;
                    int modName = putName(mod.name);
                    poolbuf.appendByte(mod.poolTag());
                    poolbuf.appendChar(modName);
                    break;
                }
                case ClassFile.CONSTANT_Integer:
                    poolbuf.appendByte(tag);
                    poolbuf.appendInt((int)((BasicConstant)c).data);
                    break;
                case ClassFile.CONSTANT_Float:
                    poolbuf.appendByte(tag);
                    poolbuf.appendFloat((float)((BasicConstant)c).data);
                    break;
                case ClassFile.CONSTANT_Long:
                    currentIndex++;
                    poolbuf.appendByte(tag);
                    poolbuf.appendLong((long)((BasicConstant)c).data);
                    break;
                case ClassFile.CONSTANT_Double:
                    currentIndex++;
                    poolbuf.appendByte(tag);
                    poolbuf.appendDouble((double)((BasicConstant)c).data);
                    break;
                case ClassFile.CONSTANT_MethodHandle: {
                    MethodHandleSymbol h = (MethodHandleSymbol)c;
                    poolbuf.appendByte(tag);
                    poolbuf.appendByte(h.referenceKind());
                    poolbuf.appendChar(putMember(h.baseSymbol()));
                    break;
                }
                case ClassFile.CONSTANT_MethodType: {
                    Type.MethodType mt = (Type.MethodType)c;
                    poolbuf.appendByte(tag);
                    poolbuf.appendChar(putDescriptor(mt.baseType()));
                    break;
                }
                case ClassFile.CONSTANT_String: {
                    Name utf = names.fromString((String)((BasicConstant)c).data);
                    poolbuf.appendByte(tag);
                    poolbuf.appendChar(putName(utf));
                    break;
                }
                case ClassFile.CONSTANT_NameandType: {
                    NameAndType nt = (NameAndType)c;
                    poolbuf.appendByte(tag);
                    poolbuf.appendChar(putName(nt.name));
                    poolbuf.appendChar(putDescriptor(nt.type));
                    break;
                }
                case ClassFile.CONSTANT_InvokeDynamic: {
                    DynamicMethodSymbol d = (DynamicMethodSymbol)c;
                    poolbuf.appendByte(tag);
                    poolbuf.appendChar(makeBootstrapEntry(d));
                    poolbuf.appendChar(putNameAndType(d));
                    break;
                }
                case ClassFile.CONSTANT_Dynamic: {
                    Symbol.DynamicVarSymbol d = (Symbol.DynamicVarSymbol)c;
                    poolbuf.appendByte(tag);
                    poolbuf.appendChar(makeBootstrapEntry(d));
                    poolbuf.appendChar(putNameAndType(d));
                    break;
                }
                default:
                    throw new AssertionError("Unexpected constant tag: " + tag);
            }
        }

        void reset() {
            keysToPos.clear();
            currentIndex = 1;
            todo.clear();
            overflowString = null;
            poolbuf.reset();
        }
    }

    int size() {
        return pool.currentIndex;
    }

    /**
     * Return signature of given type
     */
    private Name typeSig(Type type) {
        signatureGen.reset();
        signatureGen.assembleSig(type);
        return signatureGen.toName();
    }

    private Name classSig(Type t) {
        signatureGen.reset();
        List<Type> typarams = t.getTypeArguments();
        if (typarams.nonEmpty()) {
            signatureGen.assembleParamsSig(typarams);
        }
        signatureGen.assembleSig(types.supertype(t));
        for (Type i : types.interfaces(t))
            signatureGen.assembleSig(i);
        return signatureGen.toName();
    }

    void reset() {
        innerClasses.clear();
        bootstrapMethods.clear();
        pool.reset();
    }
}
