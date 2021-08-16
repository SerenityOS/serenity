/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.StandardLocation;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.ModuleSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Options;
import com.sun.tools.javac.util.Pair;

import static com.sun.tools.javac.main.Option.*;
import static com.sun.tools.javac.code.Kinds.Kind.*;
import static com.sun.tools.javac.code.Scope.LookupKind.NON_RECURSIVE;

/** This class provides operations to write native header files for classes.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class JNIWriter {
    protected static final Context.Key<JNIWriter> jniWriterKey = new Context.Key<>();

    /** Access to files. */
    private final JavaFileManager fileManager;

    Types      types;
    Symtab     syms;

    /** The log to use for verbose output.
     */
    private final Log log;

    /** Switch: verbose output.
     */
    private boolean verbose;

    /** Switch: check all nested classes of top level class
     */
    private boolean checkAll;

    /**
     * If true, class files will be written in module-specific subdirectories
     * of the NATIVE_HEADER_OUTPUT location.
     */
    public boolean multiModuleMode;

    private Context context;

    private static final boolean isWindows =
        System.getProperty("os.name").startsWith("Windows");

    /** Get the ClassWriter instance for this context. */
    public static JNIWriter instance(Context context) {
        JNIWriter instance = context.get(jniWriterKey);
        if (instance == null)
            instance = new JNIWriter(context);
        return instance;
    }

    /** Construct a class writer, given an options table.
     */
    private JNIWriter(Context context) {
        context.put(jniWriterKey, this);
        fileManager = context.get(JavaFileManager.class);
        log = Log.instance(context);

        Options options = Options.instance(context);
        verbose = options.isSet(VERBOSE);
        checkAll = options.isSet("javah:full");

        this.context = context; // for lazyInit()
    }

    private void lazyInit() {
        if (types == null)
            types = Types.instance(context);
        if (syms == null)
            syms = Symtab.instance(context);

    }

    static boolean isSynthetic(Symbol s) {
        return hasFlag(s, Flags.SYNTHETIC);
    }
    static boolean isStatic(Symbol s) {
        return hasFlag(s, Flags.STATIC);
    }
    static boolean isFinal(Symbol s) {
        return hasFlag(s, Flags.FINAL);
    }
    static boolean isNative(Symbol s) {
        return hasFlag(s, Flags.NATIVE);
    }
    private static boolean hasFlag(Symbol m, int flag) {
        return (m.flags() & flag) != 0;
    }

    public boolean needsHeader(ClassSymbol c) {
        lazyInit();
        if (c.isDirectlyOrIndirectlyLocal() || isSynthetic(c))
            return false;
        return (checkAll)
                ? needsHeader(c.outermostClass(), true)
                : needsHeader(c, false);
    }

    private boolean needsHeader(ClassSymbol c, boolean checkNestedClasses) {
        if (c.isDirectlyOrIndirectlyLocal() || isSynthetic(c))
            return false;

        for (Symbol sym : c.members_field.getSymbols(NON_RECURSIVE)) {
            if (sym.kind == MTH && isNative(sym))
                return true;
            for (Attribute.Compound a: sym.getDeclarationAttributes()) {
                if (a.type.tsym == syms.nativeHeaderType.tsym)
                    return true;
            }
        }
        if (checkNestedClasses) {
            for (Symbol sym : c.members_field.getSymbols(NON_RECURSIVE)) {
                if ((sym.kind == TYP) && needsHeader(((ClassSymbol) sym), true))
                    return true;
            }
        }
        return false;
    }

    /** Emit a class file for a given class.
     *  @param c      The class from which a class file is generated.
     */
    public FileObject write(ClassSymbol c) throws IOException {
        String className = c.flatName().toString();
        Location outLocn;
        if (multiModuleMode) {
            ModuleSymbol msym = c.owner.kind == MDL ? (ModuleSymbol) c.owner : c.packge().modle;
            outLocn = fileManager.getLocationForModule(StandardLocation.NATIVE_HEADER_OUTPUT, msym.name.toString());
        } else {
            outLocn = StandardLocation.NATIVE_HEADER_OUTPUT;
        }
        FileObject outFile
            = fileManager.getFileForOutput(outLocn,
                "", className.replaceAll("[.$]", "_") + ".h", null);
        PrintWriter out = new PrintWriter(outFile.openWriter());
        try {
            write(out, c);
            if (verbose)
                log.printVerbose("wrote.file", outFile.getName());
            out.close();
            out = null;
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

    public void write(PrintWriter out, ClassSymbol sym) throws IOException {
        lazyInit();
        try {
            String cname = encode(sym.fullname, EncoderType.CLASS);
            fileTop(out);
            includes(out);
            guardBegin(out, cname);
            cppGuardBegin(out);

            writeStatics(out, sym);
            writeMethods(out, sym, cname);

            cppGuardEnd(out);
            guardEnd(out);
        } catch (TypeSignature.SignatureException e) {
            throw new IOException(e);
        }
    }
    protected void writeStatics(PrintWriter out, ClassSymbol sym) throws IOException {
        List<ClassSymbol> clist = new ArrayList<>();
        for (ClassSymbol cd = sym; cd != null;
                cd = (ClassSymbol) cd.getSuperclass().tsym) {
            clist.add(cd);
        }
        /*
         * list needs to be super-class, base-class1, base-class2 and so on,
         * so we reverse class hierarchy
         */
        Collections.reverse(clist);
        for (ClassSymbol cd : clist) {
            for (Symbol i : cd.getEnclosedElements()) {
                // consider only final, static and fields with ConstantExpressions
                if (isFinal(i) && i.isStatic() && i.kind == VAR) {
                    VarSymbol v = (VarSymbol) i;
                    if (v.getConstantValue() != null) {
                        Pair<ClassSymbol, VarSymbol> p = new Pair<>(sym, v);
                        printStaticDefines(out, p);
                    }
                }
            }
        }
    }
    static void printStaticDefines(PrintWriter out, Pair<ClassSymbol, VarSymbol> p) {
        ClassSymbol cls = p.fst;
        VarSymbol f = p.snd;
        Object value = f.getConstantValue();
        String valueStr = null;
        switch (f.asType().getKind()) {
            case BOOLEAN:
                valueStr = (((Boolean) value) ? "1L" : "0L");
                break;
            case BYTE: case SHORT: case INT:
                valueStr = value.toString() + "L";
                break;
            case LONG:
                // Visual C++ supports the i64 suffix, not LL.
                valueStr = value.toString() + ((isWindows) ? "i64" : "LL");
                break;
            case CHAR:
                Character ch = (Character) value;
                valueStr = String.valueOf(((int) ch) & 0xffff) + "L";
                break;
            case FLOAT:
                // bug compatible
                float fv = ((Float) value).floatValue();
                valueStr = (Float.isInfinite(fv))
                        ? ((fv < 0) ? "-" : "") + "Inff"
                        : value.toString() + "f";
                break;
            case DOUBLE:
                // bug compatible
                double d = ((Double) value).doubleValue();
                valueStr = (Double.isInfinite(d))
                        ? ((d < 0) ? "-" : "") + "InfD"
                        : value.toString();
                break;
            default:
                valueStr = null;
        }
        if (valueStr != null) {
            out.print("#undef ");
            String cname = encode(cls.getQualifiedName(), EncoderType.CLASS);
            String fname = encode(f.getSimpleName(), EncoderType.FIELDSTUB);
            out.println(cname + "_" + fname);
            out.print("#define " + cname + "_");
            out.println(fname + " " + valueStr);
        }
    }
    protected void writeMethods(PrintWriter out, ClassSymbol sym, String cname)
            throws IOException, TypeSignature.SignatureException {
        List<Symbol> classmethods = sym.getEnclosedElements();
        for (Symbol md : classmethods) {
            if (isNative(md)) {
                TypeSignature newtypesig = new TypeSignature(types);
                CharSequence methodName = md.getSimpleName();
                boolean isOverloaded = false;
                for (Symbol md2 : classmethods) {
                    if ((md2 != md)
                            && (methodName.equals(md2.getSimpleName()))
                            && isNative(md2)) {
                        isOverloaded = true;
                    }
                }
                out.println("/*");
                out.println(" * Class:     " + cname);
                out.println(" * Method:    " + encode(methodName, EncoderType.FIELDSTUB));
                out.println(" * Signature: " + newtypesig.getSignature(md.type));
                out.println(" */");
                out.println("JNIEXPORT " + jniType(types.erasure(md.type.getReturnType()))
                        + " JNICALL " + encodeMethod(md, sym, isOverloaded));
                out.print("  (JNIEnv *, ");
                out.print((md.isStatic())
                        ? "jclass"
                        : "jobject");
                for (Type arg : types.erasure(md.type.getParameterTypes())) {
                    out.print(", ");
                    out.print(jniType(arg));
                }
                out.println(");");
                out.println();
            }
        }
    }
    @SuppressWarnings("fallthrough")
    protected final String jniType(Type t) {
        switch (t.getKind()) {
            case ARRAY: {
                Type ct = ((Type.ArrayType)t).getComponentType();
                switch (ct.getKind()) {
                    case BOOLEAN:  return "jbooleanArray";
                    case BYTE:     return "jbyteArray";
                    case CHAR:     return "jcharArray";
                    case SHORT:    return "jshortArray";
                    case INT:      return "jintArray";
                    case LONG:     return "jlongArray";
                    case FLOAT:    return "jfloatArray";
                    case DOUBLE:   return "jdoubleArray";
                    case ARRAY:
                    case DECLARED: return "jobjectArray";
                    default: throw new Error(ct.toString());
                }
            }

            case VOID:     return "void";
            case BOOLEAN:  return "jboolean";
            case BYTE:     return "jbyte";
            case CHAR:     return "jchar";
            case SHORT:    return "jshort";
            case INT:      return "jint";
            case LONG:     return "jlong";
            case FLOAT:    return "jfloat";
            case DOUBLE:   return "jdouble";
            case DECLARED: {
                if (t.tsym.type == syms.stringType) {
                    return "jstring";
                } else if (types.isAssignable(t, syms.throwableType)) {
                    return "jthrowable";
                } else if (types.isAssignable(t, syms.classType)) {
                    return "jclass";
                } else {
                    return "jobject";
                }
            }
        }

        Assert.check(false, "jni unknown type");
        return null; /* dead code. */
    }

    protected void  fileTop(PrintWriter out) {
        out.println("/* DO NOT EDIT THIS FILE - it is machine generated */");
    }

    protected void includes(PrintWriter out) {
        out.println("#include <jni.h>");
    }

    /*
     * Deal with the C pre-processor.
     */
    protected void cppGuardBegin(PrintWriter out) {
        out.println("#ifdef __cplusplus");
        out.println("extern \"C\" {");
        out.println("#endif");
    }

    protected void cppGuardEnd(PrintWriter out) {
        out.println("#ifdef __cplusplus");
        out.println("}");
        out.println("#endif");
    }

    protected void guardBegin(PrintWriter out, String cname) {
        out.println("/* Header for class " + cname + " */");
        out.println();
        out.println("#ifndef _Included_" + cname);
        out.println("#define _Included_" + cname);
    }

    protected void guardEnd(PrintWriter out) {
        out.println("#endif");
    }

    String encodeMethod(Symbol msym, ClassSymbol clazz,
            boolean isOverloaded) throws TypeSignature.SignatureException {
        StringBuilder result = new StringBuilder(100);
        result.append("Java_");
        /* JNI */
        result.append(encode(clazz.flatname.toString(), EncoderType.JNI));
        result.append('_');
        result.append(encode(msym.getSimpleName(), EncoderType.JNI));
        if (isOverloaded) {
            TypeSignature typeSig = new TypeSignature(types);
            StringBuilder sig = typeSig.getParameterSignature(msym.type, true);
            result.append("__").append(encode(sig, EncoderType.JNI));
        }
        return result.toString();
    }

    static enum EncoderType {
        CLASS,
        FIELDSTUB,
        FIELD,
        JNI,
        SIGNATURE
    }
    @SuppressWarnings("fallthrough")
    static String encode(CharSequence name, EncoderType mtype) {
        StringBuilder result = new StringBuilder(100);
        int length = name.length();

        for (int i = 0; i < length; i++) {
            char ch = name.charAt(i);
            if (isalnum(ch)) {
                result.append(ch);
                continue;
            }
            switch (mtype) {
                case CLASS:
                    switch (ch) {
                        case '.':
                        case '_':
                            result.append("_");
                            break;
                        case '$':
                            result.append("__");
                            break;
                        default:
                            result.append(encodeChar(ch));
                    }
                    break;
                case JNI:
                    switch (ch) {
                        case '/':
                        case '.':
                            result.append("_");
                            break;
                        case '_':
                            result.append("_1");
                            break;
                        case ';':
                            result.append("_2");
                            break;
                        case '[':
                            result.append("_3");
                            break;
                        default:
                            result.append(encodeChar(ch));
                    }
                    break;
                case SIGNATURE:
                    result.append(isprint(ch) ? ch : encodeChar(ch));
                    break;
                case FIELDSTUB:
                    result.append(ch == '_' ? ch : encodeChar(ch));
                    break;
                default:
                    result.append(encodeChar(ch));
            }
        }
        return result.toString();
    }

    static String encodeChar(char ch) {
        String s = Integer.toHexString(ch);
        int nzeros = 5 - s.length();
        char[] result = new char[6];
        result[0] = '_';
        for (int i = 1; i <= nzeros; i++) {
            result[i] = '0';
        }
        for (int i = nzeros + 1, j = 0; i < 6; i++, j++) {
            result[i] = s.charAt(j);
        }
        return new String(result);
    }

    /* Warning: Intentional ASCII operation. */
    private static boolean isalnum(char ch) {
        return ch <= 0x7f && /* quick test */
                ((ch >= 'A' && ch <= 'Z')  ||
                 (ch >= 'a' && ch <= 'z')  ||
                 (ch >= '0' && ch <= '9'));
    }

    /* Warning: Intentional ASCII operation. */
    private static boolean isprint(char ch) {
        return ch >= 32 && ch <= 126;
    }

    private static class TypeSignature {
        static class SignatureException extends Exception {
            private static final long serialVersionUID = 1L;
            SignatureException(String reason) {
                super(reason);
            }
        }

        JavacElements elems;
        Types    types;

        /* Signature Characters */
        private static final String SIG_VOID                   = "V";
        private static final String SIG_BOOLEAN                = "Z";
        private static final String SIG_BYTE                   = "B";
        private static final String SIG_CHAR                   = "C";
        private static final String SIG_SHORT                  = "S";
        private static final String SIG_INT                    = "I";
        private static final String SIG_LONG                   = "J";
        private static final String SIG_FLOAT                  = "F";
        private static final String SIG_DOUBLE                 = "D";
        private static final String SIG_ARRAY                  = "[";
        private static final String SIG_CLASS                  = "L";

        public TypeSignature(Types types) {
            this.types = types;
        }

        StringBuilder getParameterSignature(Type mType, boolean useFlatname)
                throws SignatureException {
            StringBuilder result = new StringBuilder();
            for (Type pType : mType.getParameterTypes()) {
                result.append(getJvmSignature(pType, useFlatname));
            }
            return result;
        }

        StringBuilder getReturnSignature(Type mType)
                throws SignatureException {
            return getJvmSignature(mType.getReturnType(), false);
        }

        StringBuilder getSignature(Type mType) throws SignatureException {
            StringBuilder sb = new StringBuilder();
            sb.append("(").append(getParameterSignature(mType, false)).append(")");
            sb.append(getReturnSignature(mType));
            return sb;
        }

        /*
         * Returns jvm internal signature.
         */
        static class JvmTypeVisitor extends JNIWriter.SimpleTypeVisitor<Type, StringBuilder> {
            private final boolean useFlatname;

            JvmTypeVisitor(boolean useFlatname) {
                super();
                this.useFlatname = useFlatname;
            }

            @Override
            public Type visitClassType(Type.ClassType t, StringBuilder s) {
                setDeclaredType(t, s);
                return null;
            }

            @Override
            public Type visitArrayType(Type.ArrayType t, StringBuilder s) {
                s.append("[");
                return t.getComponentType().accept(this, s);
            }

            @Override
            public Type visitType(Type t, StringBuilder s) {
                if (t.isPrimitiveOrVoid()) {
                    s.append(getJvmPrimitiveSignature(t));
                    return null;
                }
                return t.accept(this, s);
            }
            private void setDeclaredType(Type t, StringBuilder s) {
                    String classname = useFlatname ? t.tsym.flatName().toString()
                            : t.tsym.getQualifiedName().toString();
                    classname = classname.replace('.', '/');
                    s.append("L").append(classname).append(";");
            }
            private String getJvmPrimitiveSignature(Type t) {
                switch (t.getKind()) {
                    case VOID:      return SIG_VOID;
                    case BOOLEAN:   return SIG_BOOLEAN;
                    case BYTE:      return SIG_BYTE;
                    case CHAR:      return SIG_CHAR;
                    case SHORT:     return SIG_SHORT;
                    case INT:       return SIG_INT;
                    case LONG:      return SIG_LONG;
                    case FLOAT:     return SIG_FLOAT;
                    case DOUBLE:    return SIG_DOUBLE;
                    default:
                        Assert.error("unknown type: should not happen");
                }
                return null;
            }
        }

        StringBuilder getJvmSignature(Type type, boolean useFlatname) {
            Type t = types.erasure(type);
            StringBuilder sig = new StringBuilder();
            JvmTypeVisitor jv = new JvmTypeVisitor(useFlatname);
            jv.visitType(t, sig);
            return sig;
        }
    }

    static class SimpleTypeVisitor<R, P> implements Type.Visitor<R, P> {

        protected final R DEFAULT_VALUE;

        protected SimpleTypeVisitor() {
            DEFAULT_VALUE = null;
        }

        protected SimpleTypeVisitor(R defaultValue) {
            DEFAULT_VALUE = defaultValue;
        }

        protected R defaultAction(Type t, P p) {
            return DEFAULT_VALUE;
        }

        @Override
        public R visitClassType(Type.ClassType t, P p) {
            return defaultAction(t, p);
        }

        @Override
        public R visitWildcardType(Type.WildcardType t, P p) {
            return defaultAction(t, p);
        }

        @Override
        public R visitArrayType(Type.ArrayType t, P p) {
            return defaultAction(t, p);
        }

        @Override
        public R visitMethodType(Type.MethodType t, P p) {
            return defaultAction(t, p);
        }

        @Override
        public R visitPackageType(Type.PackageType t, P p) {
            return defaultAction(t, p);
        }

        @Override
        public R visitTypeVar(Type.TypeVar t, P p) {
            return defaultAction(t, p);
        }

        @Override
        public R visitCapturedType(Type.CapturedType t, P p) {
            return defaultAction(t, p);
        }

        @Override
        public R visitForAll(Type.ForAll t, P p) {
            return defaultAction(t, p);
        }

        @Override
        public R visitUndetVar(Type.UndetVar t, P p) {
            return defaultAction(t, p);
        }

        @Override
        public R visitErrorType(Type.ErrorType t, P p) {
            return defaultAction(t, p);
        }

        @Override
        public R visitType(Type t, P p) {
            return defaultAction(t, p);
        }

        @Override
        public R visitModuleType(Type.ModuleType t, P p) {
            return defaultAction(t, p);
        }
    }
}
