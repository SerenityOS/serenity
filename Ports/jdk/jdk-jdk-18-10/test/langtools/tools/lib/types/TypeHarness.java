/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import java.net.URI;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.Check;
import com.sun.tools.javac.comp.Infer;
import com.sun.tools.javac.comp.InferenceContext;
import com.sun.tools.javac.comp.Modules;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.Abort;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

import static com.sun.tools.javac.util.List.*;

/**
 * Test harness whose goal is to simplify the task of writing type-system
 * regression test. It provides functionalities to build custom types as well
 * as to access the underlying javac's symbol table in order to retrieve
 * predefined types. Among the features supported by the harness are: type
 * substitution, type containment, subtyping, cast-conversion, assigment
 * conversion.
 *
 * This class is meant to be a common super class for all concrete type test
 * classes. A subclass can access the type-factory and the test methods so as
 * to write compact tests. An example is reported below:
 *
 * <pre>
 * Type X = fac.TypeVariable();
 * Type Y = fac.TypeVariable();
 * Type A_X_Y = fac.Class(0, X, Y);
 * Type A_Obj_Obj = fac.Class(0,
 *           predef.objectType,
 *           predef.objectType);
 * checkSameType(A_Obj_Obj, subst(A_X_Y,
 *           Mapping(X, predef.objectType),
 *           Mapping(Y, predef.objectType)));
 * </pre>
 *
 * The above code is used to create two class types, namely {@code A<X,Y>} and
 * {@code A<Object,Object>} where both {@code X} and {@code Y} are type-variables.
 * The code then verifies that {@code [X:=Object,Y:=Object]A<X,Y> == A<Object,Object>}.
 *
 * @author mcimadamore
 * @author vromero
 */
public class TypeHarness {

    protected Types types;
    protected Check chk;
    protected Symtab predef;
    protected Names names;
    protected ReusableJavaCompiler tool;
    protected Infer infer;
    protected Context context;

    protected Factory fac;

    protected TypeHarness() {
        context = new Context();
        JavacFileManager.preRegister(context);
        MyAttr.preRegister(context);
        tool = new ReusableJavaCompiler(context);
        types = Types.instance(context);
        infer = Infer.instance(context);
        chk = Check.instance(context);
        predef = Symtab.instance(context);
        names = Names.instance(context);
        fac = new Factory();
    }

    // <editor-fold defaultstate="collapsed" desc="type assertions">

    /** assert that 's' is a subtype of 't' */
    public void assertSubtype(Type s, Type t) {
        assertSubtype(s, t, true);
    }

    /** assert that 's' is/is not a subtype of 't' */
    public void assertSubtype(Type s, Type t, boolean expected) {
        if (types.isSubtype(s, t) != expected) {
            String msg = expected ?
                " is not a subtype of " :
                " is a subtype of ";
            error(s + msg + t);
        }
    }

    /** assert that 's' is the same type as 't' */
    public void assertSameType(Type s, Type t) {
        assertSameType(s, t, true);
    }

    /** assert that 's' is/is not the same type as 't' */
    public void assertSameType(Type s, Type t, boolean expected) {
        if (types.isSameType(s, t) != expected) {
            String msg = expected ?
                " is not the same type as " :
                " is the same type as ";
            error(s + msg + t);
        }
    }

    /** assert that 's' is castable to 't' */
    public void assertCastable(Type s, Type t) {
        assertCastable(s, t, true);
    }

    /** assert that 's' is/is not castable to 't' */
    public void assertCastable(Type s, Type t, boolean expected) {
        if (types.isCastable(s, t) != expected) {
            String msg = expected ?
                " is not castable to " :
                " is castable to ";
            error(s + msg + t);
        }
    }

    /** assert that 's' is convertible (method invocation conversion) to 't' */
    public void assertConvertible(Type s, Type t) {
        assertCastable(s, t, true);
    }

    /** assert that 's' is/is not convertible (method invocation conversion) to 't' */
    public void assertConvertible(Type s, Type t, boolean expected) {
        if (types.isConvertible(s, t) != expected) {
            String msg = expected ?
                " is not convertible to " :
                " is convertible to ";
            error(s + msg + t);
        }
    }

    /** assert that 's' is assignable to 't' */
    public void assertAssignable(Type s, Type t) {
        assertCastable(s, t, true);
    }

    /** assert that 's' is/is not assignable to 't' */
    public void assertAssignable(Type s, Type t, boolean expected) {
        if (types.isAssignable(s, t) != expected) {
            String msg = expected ?
                " is not assignable to " :
                " is assignable to ";
            error(s + msg + t);
        }
    }

    /** assert that generic type 't' is well-formed */
    public void assertValidGenericType(Type t) {
        assertValidGenericType(t, true);
    }

    /** assert that 's' is/is not assignable to 't' */
    public void assertValidGenericType(Type t, boolean expected) {
        if (chk.checkValidGenericType(t) != expected) {
            String msg = expected ?
                " is not a valid generic type" :
                " is a valid generic type";
            error(t + msg + "   " + t.tsym.type);
        }
    }
    // </editor-fold>

    /** Creates an inference context given a list of type variables and performs the given action on it.
     *  The intention is to provide a way to do unit testing on inference contexts.
     *  @param typeVars  a list of type variables to create the inference context from
     *  @param consumer  the action to be performed on the inference context
     */
    protected void withInferenceContext(List<Type> typeVars, Consumer<InferenceContext> consumer) {
        Assert.check(!typeVars.isEmpty(), "invalid parameter, empty type variables list");
        ListBuffer undetVarsBuffer = new ListBuffer();
        typeVars.stream().map((tv) -> new UndetVar((TypeVar)tv, null, types)).forEach((undetVar) -> {
            undetVarsBuffer.add(undetVar);
        });
        List<Type> undetVarsList = undetVarsBuffer.toList();
        InferenceContext inferenceContext = new InferenceContext(infer, nil(), undetVarsList);
        inferenceContext.rollback(undetVarsList);
        consumer.accept(inferenceContext);
    }

    private void error(String msg) {
        throw new AssertionError("Unexpected result: " + msg);
    }

    // <editor-fold defaultstate="collapsed" desc="type functions">

    /** compute the erasure of a type 't' */
    public Type erasure(Type t) {
        return types.erasure(t);
    }

    /** compute the capture of a type 't' */
    public Type capture(Type t) {
        return types.capture(t);
    }

    /** compute the boxed type associated with 't' */
    public Type box(Type t) {
        if (!t.isPrimitive()) {
            throw new AssertionError("Cannot box non-primitive type: " + t);
        }
        return types.boxedClass(t).type;
    }

    /** compute the unboxed type associated with 't' */
    public Type unbox(Type t) {
        Type u = types.unboxedType(t);
        if (t == null) {
            throw new AssertionError("Cannot unbox reference type: " + t);
        } else {
            return u;
        }
    }

    /** compute a type substitution on 't' given a list of type mappings */
    public Type subst(Type t, Mapping... maps) {
        ListBuffer<Type> from = new ListBuffer<>();
        ListBuffer<Type> to = new ListBuffer<>();
        for (Mapping tm : maps) {
            from.append(tm.from);
            to.append(tm.to);
        }
        return types.subst(t, from.toList(), to.toList());
    }

    /** create a fresh type mapping from a type to another */
    public Mapping Mapping(Type from, Type to) {
        return new Mapping(from, to);
    }

    public static class Mapping {
        Type from;
        Type to;
        private Mapping(Type from, Type to) {
            this.from = from;
            this.to = to;
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="type factory">

    /**
     * This class is used to create Java types in a simple way. All main
     * kinds of type are supported: primitive, reference, non-denotable. The
     * factory also supports creation of constant types (used by the compiler
     * to represent the type of a literal).
     */
    public class Factory {

        private int synthNameCount = 0;

        private Name syntheticName() {
            return names.fromString("A$" + synthNameCount++);
        }

        public ClassType Class(long flags, Type... typeArgs) {
            ClassSymbol csym = new ClassSymbol(flags, syntheticName(), predef.noSymbol);
            csym.type = new ClassType(Type.noType, List.from(typeArgs), csym);
            ((ClassType)csym.type).supertype_field = predef.objectType;
            return (ClassType)csym.type;
        }

        public ClassType Class(Type... typeArgs) {
            return Class(0, typeArgs);
        }

        public ClassType Interface(Type... typeArgs) {
            return Class(Flags.INTERFACE, typeArgs);
        }

        public ClassType Interface(long flags, Type... typeArgs) {
            return Class(Flags.INTERFACE | flags, typeArgs);
        }

        public Type Constant(byte b) {
            return predef.byteType.constType(b);
        }

        public Type Constant(short s) {
            return predef.shortType.constType(s);
        }

        public Type Constant(int i) {
            return predef.intType.constType(i);
        }

        public Type Constant(long l) {
            return predef.longType.constType(l);
        }

        public Type Constant(float f) {
            return predef.floatType.constType(f);
        }

        public Type Constant(double d) {
            return predef.doubleType.constType(d);
        }

        public Type Constant(char c) {
            return predef.charType.constType(c + 0);
        }

        public ArrayType Array(Type elemType) {
            return new ArrayType(elemType, predef.arrayClass);
        }

        public TypeVar TypeVariable() {
            return TypeVariable(predef.objectType);
        }

        public TypeVar TypeVariable(Type bound) {
            TypeSymbol tvsym = new TypeVariableSymbol(0, syntheticName(), null, predef.noSymbol);
            tvsym.type = new TypeVar(tvsym, bound, Type.noType);
            return (TypeVar)tvsym.type;
        }

        public WildcardType Wildcard(BoundKind bk, Type bound) {
            return new WildcardType(bound, bk, predef.boundClass);
        }

        public CapturedType CapturedVariable(Type upper, Type lower) {
            return new CapturedType(syntheticName(), predef.noSymbol, upper, lower, null);
        }

        public ClassType Intersection(Type classBound, Type... intfBounds) {
            ClassType ct = Class(Flags.COMPOUND);
            ct.supertype_field = classBound;
            ct.interfaces_field = List.from(intfBounds);
            return ct;
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="StrToTypeFactory">
    /**
     * StrToTypeFactory is a class provided to ease the creation of complex types from Strings.
     * The client code can specify a package, a list of imports and a list of type variables when
     * creating an instance of StrToTypeFactory. Later types including, or not, these type variables
     * can be created by the factory. All occurrences of the same type variable in a type defined
     * using a String are guaranteed to refer to the same type variable in the created type.
     *
     * An example is reported below:
     *
     * <pre>
     * List<String> imports = new ArrayList<>();
     * imports.add("java.util.*");
     * List<String> typeVars = new ArrayList<>();
     * typeVars.add("T");
     * strToTypeFactory = new StrToTypeFactory(null, imports, typeVars);
     *
     * Type freeType = strToTypeFactory.getType("List<? extends T>");
     * Type aType = strToTypeFactory.getType("List<? extends String>");
     *
     * // method withInferenceContext() belongs to TypeHarness
     * withInferenceContext(strToTypeFactory.getTypeVars(), inferenceContext -> {
     *     assertSameType(inferenceContext.asUndetVar(freeType), aType);
     *     UndetVar undetVarForT = (UndetVar)inferenceContext.undetVars().head;
     *     com.sun.tools.javac.util.List<Type> equalBounds = undetVarForT.getBounds(InferenceBound.EQ);
     *     Assert.check(!equalBounds.isEmpty() && equalBounds.length() == 1,
     *          "undetVar must have only one equality bound");
     * });
     * </pre>
     */
    public class StrToTypeFactory {
        int id = 0;
        String pkg;
        java.util.List<String> imports;
        public java.util.List<String> typeVarDecls;
        public List<Type> typeVariables;

        public StrToTypeFactory(String pkg, java.util.List<String> imports, java.util.List<String> typeVarDecls) {
            this.pkg = pkg;
            this.imports = imports;
            this.typeVarDecls = typeVarDecls == null ? new ArrayList<>() : typeVarDecls;
            this.typeVariables = from(this.typeVarDecls.stream()
                    .map(this::typeVarName)
                    .map(this::getType)
                    .collect(Collectors.toList())
            );
        }

        TypeVar getTypeVarFromStr(String name) {
            if (typeVarDecls.isEmpty()) {
                return null;
            }
            int index = typeVarDecls.indexOf(name);
            if (index != -1) {
                return (TypeVar)typeVariables.get(index);
            }
            return null;
        }

        List<Type> getTypeVars() {
            return typeVariables;
        }

        String typeVarName(String typeVarDecl) {
            String[] ss = typeVarDecl.split(" ");
            return ss[0];
        }

        public final Type getType(String type) {
            JavaSource source = new JavaSource(type);
            MyAttr.theType = null;
            MyAttr.typeParameters = List.nil();
            tool.clear();
            List<JavaFileObject> inputs = of(source);
            try {
                tool.compile(inputs);
            } catch (Throwable ex) {
                throw new Abort(ex);
            }
            if (typeVariables != null) {
                return types.subst(MyAttr.theType, MyAttr.typeParameters, typeVariables);
            }
            return MyAttr.theType;
        }

        class JavaSource extends SimpleJavaFileObject {

            String id;
            String type;
            String template = "#Package;\n" +
                    "#Imports\n" +
                    "class G#Id#TypeVars {\n" +
                    "   #FieldType var;" +
                    "}";

            JavaSource(String type) {
                super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
                this.id = String.valueOf(StrToTypeFactory.this.id++);
                this.type = type;
            }

            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                String impStmts = imports.size() > 0 ?
                        imports.stream().map(i -> "import " + i + ";").collect(Collectors.joining("\n")) : "";
                String tvars = !typeVarDecls.isEmpty() ?
                        typeVarDecls.stream().collect(Collectors.joining(",", "<", ">")) : "";
                return template
                        .replace("#Package", (pkg == null) ? "" : "package " + pkg + ";")
                        .replace("#Imports", impStmts)
                        .replace("#Id", id)
                        .replace("#TypeVars", tvars)
                        .replace("#FieldType", type);
            }
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="helper classes">
    static class MyAttr extends Attr {

        private static Type theType;
        private static List<Type> typeParameters = List.nil();

        static void preRegister(Context context) {
            context.put(attrKey, (com.sun.tools.javac.util.Context.Factory<Attr>) c -> new MyAttr(c));
        }

        MyAttr(Context context) {
            super(context);
        }

        @Override
        public void visitVarDef(JCVariableDecl tree) {
            super.visitVarDef(tree);
            theType = tree.type;
        }

        @Override
        public void attribClass(DiagnosticPosition pos, ClassSymbol c) {
            super.attribClass(pos, c);
            ClassType ct = (ClassType)c.type;
            typeParameters = ct.typarams_field;
        }
    }

    static class ReusableJavaCompiler extends JavaCompiler {
        ReusableJavaCompiler(Context context) {
            super(context);
        }

        @Override
        protected void checkReusable() {
            // do nothing
        }

        @Override
        public void close() {
            //do nothing
        }

        void clear() {
            newRound();
            Modules.instance(context).newRound();
        }
    }
    // </editor-fold>
}
