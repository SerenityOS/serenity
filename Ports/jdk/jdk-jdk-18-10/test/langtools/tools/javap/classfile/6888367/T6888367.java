/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.net.*;
import java.util.*;

import com.sun.tools.classfile.*;
import com.sun.tools.classfile.Type.ArrayType;
import com.sun.tools.classfile.Type.ClassSigType;
import com.sun.tools.classfile.Type.ClassType;
import com.sun.tools.classfile.Type.MethodType;
import com.sun.tools.classfile.Type.SimpleType;
import com.sun.tools.classfile.Type.TypeParamType;
import com.sun.tools.classfile.Type.WildcardType;

/*
 * @test
 * @bug 6888367
 * @summary classfile library parses signature attributes incorrectly
 * @modules jdk.jdeps/com.sun.tools.classfile
 */

/*
 * This test is a pretty detailed test both of javac signature generation and classfile
 * signature parsing.  The first part of the test tests all the examples given in the
 * second part of the test. Each example comes with one or two annotations, @Desc, @Sig,
 * for the descriptor and signature of the annotated declaration.  Annotations are
 * provided whenever the annotated item is expected to have a corresponding value.
 * Each annotation has two argument values.  The first arg is the expected value of the
 * descriptor/signature as found in the class file.  This value is mostly for documentation
 * purposes in reading the test.  The second value is the rendering of the descriptor or
 * signature using a custom Type visitor that explicitly includes an indication of the
 * Type classes being used to represent the  descriptor/signature.  Thus we test
 * that the descriptor/signature is being parsed into the expected type tree structure.
 */
public class T6888367 {

    public static void main(String... args) throws Exception {
        new T6888367().run();
    }

    public void run() throws Exception {
        ClassFile cf = getClassFile("Test");

        testFields(cf);
        testMethods(cf);
        testInnerClasses(cf); // recursive

        if (errors > 0)
            throw new Exception(errors + " errors found");
    }

    void testFields(ClassFile cf) throws Exception {
        String cn = cf.getName();
        ConstantPool cp = cf.constant_pool;
        for (Field f: cf.fields) {
            test("field " + cn + "." + f.getName(cp), f.descriptor, f.attributes, cp);
        }
    }

    void testMethods(ClassFile cf) throws Exception {
        String cn = cf.getName();
        ConstantPool cp = cf.constant_pool;
        for (Method m: cf.methods) {
            test("method " + cn + "." + m.getName(cp), m.descriptor, m.attributes, cp);
        }
    }

    void testInnerClasses(ClassFile cf) throws Exception {
        ConstantPool cp = cf.constant_pool;
        InnerClasses_attribute ic =
                (InnerClasses_attribute) cf.attributes.get(Attribute.InnerClasses);
        for (InnerClasses_attribute.Info info: ic.classes) {
            String outerClassName = cp.getClassInfo(info.outer_class_info_index).getName();
            if (!outerClassName.equals(cf.getName())) {
                continue;
            }
            String innerClassName = cp.getClassInfo(info.inner_class_info_index).getName();
            ClassFile icf = getClassFile(innerClassName);
            test("class " + innerClassName, null, icf.attributes, icf.constant_pool);
            testInnerClasses(icf);
        }
    }

    void test(String name, Descriptor desc, Attributes attrs, ConstantPool cp)
            throws Exception {
        AnnotValues d = getDescValue(attrs, cp);
        AnnotValues s = getSigValue(attrs, cp);
        if (d == null && s == null) // not a test field or method if no @Desc or @Sig given
            return;

        System.err.println(name);

        if (desc != null) {
            System.err.println("    descriptor: " + desc.getValue(cp));
            checkEqual(d.raw, desc.getValue(cp));
            Type dt = new Signature(desc.index).getType(cp);
            checkEqual(d.type, tp.print(dt));
        }

        Signature_attribute sa = (Signature_attribute) attrs.get(Attribute.Signature);
        if (sa != null)
            System.err.println("     signature: " + sa.getSignature(cp));

        if (s != null || sa != null) {
            if (s != null && sa != null) {
                checkEqual(s.raw, sa.getSignature(cp));
                Type st = new Signature(sa.signature_index).getType(cp);
                checkEqual(s.type, tp.print(st));
            } else if (s != null)
                error("@Sig annotation found but not Signature attribute");
            else
                error("Signature attribute found but no @Sig annotation");
        }

        System.err.println();
    }


    ClassFile getClassFile(String name) throws IOException, ConstantPoolException {
        URL url = getClass().getResource(name + ".class");
        InputStream in = url.openStream();
        try {
            return ClassFile.read(in);
        } finally {
            in.close();
        }
    }

    AnnotValues getDescValue(Attributes attrs, ConstantPool cp) throws Exception {
        return getAnnotValues(Desc.class.getName(), attrs, cp);
    }

    AnnotValues getSigValue(Attributes attrs, ConstantPool cp) throws Exception {
        return getAnnotValues(Sig.class.getName(), attrs, cp);
    }

    static class AnnotValues {
        AnnotValues(String raw, String type) {
            this.raw = raw;
            this.type = type;
        }
        final String raw;
        final String type;
    }

    AnnotValues getAnnotValues(String annotName, Attributes attrs, ConstantPool cp)
            throws Exception {
        RuntimeInvisibleAnnotations_attribute annots =
                (RuntimeInvisibleAnnotations_attribute)attrs.get(Attribute.RuntimeInvisibleAnnotations);
        if (annots != null) {
            for (Annotation a: annots.annotations) {
                if (cp.getUTF8Value(a.type_index).equals("L" + annotName + ";")) {
                    Annotation.Primitive_element_value pv0 =
                            (Annotation.Primitive_element_value) a.element_value_pairs[0].value;
                    Annotation.Primitive_element_value pv1 =
                            (Annotation.Primitive_element_value) a.element_value_pairs[1].value;
                    return new AnnotValues(
                            cp.getUTF8Value(pv0.const_value_index),
                            cp.getUTF8Value(pv1.const_value_index));
                }
            }
        }
        return null;

    }

    void checkEqual(String expect, String found) {
        if (!(expect == null ? found == null : expect.equals(found))) {
            System.err.println("expected: " + expect);
            System.err.println("   found: " + found);
            error("unexpected values found");
        }
    }

    void error(String msg) {
        System.err.println("error: " + msg);
        errors++;
    }

    int errors;

    TypePrinter tp = new TypePrinter();

    class TypePrinter implements Type.Visitor<String,Void> {
        String print(Type t) {
            return t == null ? null : t.accept(this, null);
        }
        String print(String pre, List<? extends Type> ts, String post) {
            if (ts == null)
                return null;
            StringBuilder sb = new StringBuilder();
            sb.append(pre);
            String sep = "";
            for (Type t: ts) {
                sb.append(sep);
                sb.append(print(t));
                sep = ",";
            }
            sb.append(post);
            return sb.toString();
        }

        public String visitSimpleType(SimpleType type, Void p) {
            return "S{" + type.name + "}";
        }

        public String visitArrayType(ArrayType type, Void p) {
            return "A{" + print(type.elemType) + "}";
        }

        public String visitMethodType(MethodType type, Void p) {
            StringBuilder sb = new StringBuilder();
            sb.append("M{");
            if (type.typeParamTypes != null)
                sb.append(print("<", type.typeParamTypes, ">"));
            sb.append(print(type.returnType));
            sb.append(print("(", type.paramTypes, ")"));
            if (type.throwsTypes != null)
                sb.append(print("", type.throwsTypes, ""));
            sb.append("}");
            return sb.toString();
        }

        public String visitClassSigType(ClassSigType type, Void p) {
            StringBuilder sb = new StringBuilder();
            sb.append("CS{");
            if (type.typeParamTypes != null)
                sb.append(print("<", type.typeParamTypes, ">"));
            sb.append(print(type.superclassType));
            if (type.superinterfaceTypes != null)
                sb.append(print("i(", type.superinterfaceTypes, ")"));
            sb.append("}");
            return sb.toString();
        }

        public String visitClassType(ClassType type, Void p) {
            StringBuilder sb = new StringBuilder();
            sb.append("C{");
            if (type.outerType != null) {
                sb.append(print(type.outerType));
                sb.append(".");
            }
            sb.append(type.name);
            if (type.typeArgs != null)
                sb.append(print("<", type.typeArgs, ">"));
            sb.append("}");
            return sb.toString();
        }

        public String visitTypeParamType(TypeParamType type, Void p) {
            StringBuilder sb = new StringBuilder();
            sb.append("TA{");
            sb.append(type.name);
            if (type.classBound != null) {
                sb.append(":c");
                sb.append(print(type.classBound));
            }
            if (type.interfaceBounds != null)
                sb.append(print(":i", type.interfaceBounds, ""));
            sb.append("}");
            return sb.toString();
        }

        public String visitWildcardType(WildcardType type, Void p) {
            switch (type.kind) {
                case UNBOUNDED:
                    return "W{?}";
                case EXTENDS:
                    return "W{e," + print(type.boundType) + "}";
                case SUPER:
                    return "W{s," + print(type.boundType) + "}";
                default:
                    throw new AssertionError();
            }
        }

    };
}


@interface Desc {
    String d();
    String t();
}

@interface Sig {
    String s();
    String t();
}

class Clss { }
interface Intf { }
class GenClss<T> { }

class Test {
    // fields

    @Desc(d="Z", t="S{boolean}")
    boolean z;

    @Desc(d="B", t="S{byte}")
    byte b;

    @Desc(d="C", t="S{char}")
    char c;

    @Desc(d="D", t="S{double}")
    double d;

    @Desc(d="F", t="S{float}")
    float f;

    @Desc(d="I", t="S{int}")
    int i;

    @Desc(d="J", t="S{long}")
    long l;

    @Desc(d="S", t="S{short}")
    short s;

    @Desc(d="LClss;", t="C{Clss}")
    Clss clss;

    @Desc(d="LIntf;", t="C{Intf}")
    Intf intf;

    @Desc(d="[I", t="A{S{int}}")
    int[] ai;

    @Desc(d="[LClss;", t="A{C{Clss}}")
    Clss[] aClss;

    @Desc(d="LGenClss;", t="C{GenClss}")
    @Sig(s="LGenClss<LClss;>;", t="C{GenClss<C{Clss}>}")
    GenClss<Clss> genClass;

    // methods, return types

    @Desc(d="()V", t="M{S{void}()}")
    void mv0() { }

    @Desc(d="()I", t="M{S{int}()}")
    int mi0() { return 0; }

    @Desc(d="()LClss;", t="M{C{Clss}()}")
    Clss mclss0() { return null; }

    @Desc(d="()[I", t="M{A{S{int}}()}")
    int[] mai0() { return null; }

    @Desc(d="()[LClss;", t="M{A{C{Clss}}()}")
    Clss[] maClss0() { return null; }

    @Desc(d="()LGenClss;", t="M{C{GenClss}()}")
    @Sig(s="()LGenClss<LClss;>;", t="M{C{GenClss<C{Clss}>}()}")
    GenClss<Clss> mgenClss0() { return null; }

    @Desc(d="()LGenClss;", t="M{C{GenClss}()}")
    @Sig(s="()LGenClss<*>;", t="M{C{GenClss<W{?}>}()}")
    GenClss<?> mgenClssW0() { return null; }

    @Desc(d="()LGenClss;", t="M{C{GenClss}()}")
    @Sig(s="()LGenClss<+LClss;>;", t="M{C{GenClss<W{e,C{Clss}}>}()}")
    GenClss<? extends Clss> mgenClssWExtClss0() { return null; }

    @Desc(d="()LGenClss;", t="M{C{GenClss}()}")
    @Sig(s="()LGenClss<-LClss;>;", t="M{C{GenClss<W{s,C{Clss}}>}()}")
    GenClss<? super Clss> mgenClssWSupClss0() { return null; }

    @Desc(d="()Ljava/lang/Object;", t="M{C{java/lang/Object}()}")
    @Sig(s="<T:Ljava/lang/Object;>()TT;", t="M{<TA{T:cC{java/lang/Object}}>S{T}()}")
    <T> T mt0() { return null; }

    @Desc(d="()LGenClss;", t="M{C{GenClss}()}")
    @Sig(s="<T:Ljava/lang/Object;>()LGenClss<+TT;>;",
        t="M{<TA{T:cC{java/lang/Object}}>C{GenClss<W{e,S{T}}>}()}")
    <T> GenClss<? extends T> mgenClssWExtT0() { return null; }

    @Desc(d="()LGenClss;", t="M{C{GenClss}()}")
    @Sig(s="<T:Ljava/lang/Object;>()LGenClss<-TT;>;", t="M{<TA{T:cC{java/lang/Object}}>C{GenClss<W{s,S{T}}>}()}")
    <T> GenClss<? super T> mgenClssWSupT0() { return null; }

    // methods, arg types

    @Desc(d="(I)V", t="M{S{void}(S{int})}")
    void mi1(int arg) { }

    @Desc(d="(LClss;)V", t="M{S{void}(C{Clss})}")
    void mclss1(Clss arg) { }

    @Desc(d="([I)V", t="M{S{void}(A{S{int}})}")
    void mai1(int[] arg) { }

    @Desc(d="([LClss;)V", t="M{S{void}(A{C{Clss}})}")
    void maClss1(Clss[] arg) { }

    @Desc(d="(LGenClss;)V", t="M{S{void}(C{GenClss})}")
    @Sig(s="(LGenClss<LClss;>;)V", t="M{S{void}(C{GenClss<C{Clss}>})}")
    void mgenClss1(GenClss<Clss> arg) { }

    @Desc(d="(LGenClss;)V", t="M{S{void}(C{GenClss})}")
    @Sig(s="(LGenClss<*>;)V", t="M{S{void}(C{GenClss<W{?}>})}")
    void mgenClssW1(GenClss<?> arg) { }

    @Desc(d="(LGenClss;)V", t="M{S{void}(C{GenClss})}")
    @Sig(s="(LGenClss<+LClss;>;)V", t="M{S{void}(C{GenClss<W{e,C{Clss}}>})}")
    void mgenClssWExtClss1(GenClss<? extends Clss> arg) { }

    @Desc(d="(LGenClss;)V", t="M{S{void}(C{GenClss})}")
    @Sig(s="(LGenClss<-LClss;>;)V", t="M{S{void}(C{GenClss<W{s,C{Clss}}>})}")
    void mgenClssWSupClss1(GenClss<? super Clss> arg) { }

    @Desc(d="(Ljava/lang/Object;)V", t="M{S{void}(C{java/lang/Object})}")
    @Sig(s="<T:Ljava/lang/Object;>(TT;)V",
        t="M{<TA{T:cC{java/lang/Object}}>S{void}(S{T})}")
    <T> void mt1(T arg) { }

    @Desc(d="(LGenClss;)V", t="M{S{void}(C{GenClss})}")
    @Sig(s="<T:Ljava/lang/Object;>(LGenClss<+TT;>;)V",
        t="M{<TA{T:cC{java/lang/Object}}>S{void}(C{GenClss<W{e,S{T}}>})}")
    <T> void mgenClssWExtT1(GenClss<? extends T> arg) { }

    @Desc(d="(LGenClss;)V", t="M{S{void}(C{GenClss})}")
    @Sig(s="<T:Ljava/lang/Object;>(LGenClss<-TT;>;)V",
        t="M{<TA{T:cC{java/lang/Object}}>S{void}(C{GenClss<W{s,S{T}}>})}")
    <T> void mgenClssWSupT1(GenClss<? super T> arg) { }

    // methods, throws

    @Desc(d="()V", t="M{S{void}()}")
    void m_E() throws Exception { }

    @Desc(d="()V", t="M{S{void}()}")
    @Sig(s="<T:Ljava/lang/Throwable;>()V^TT;",
        t="M{<TA{T:cC{java/lang/Throwable}}>S{void}()S{T}}")
    <T extends Throwable> void m_T() throws T { }

    // inner classes

    static class X {
        // no sig
        class P { }

        @Sig(s="<TQ:Ljava/lang/Object;>LTest$X$P;",
            t="CS{<TA{TQ:cC{java/lang/Object}}>C{Test$X$P}}")
        class Q<TQ> extends P { }

        @Sig(s="<TR:Ljava/lang/Object;>LTest$X$Q<TTR;>;",
            t="CS{<TA{TR:cC{java/lang/Object}}>C{Test$X$Q<S{TR}>}}")
        class R<TR> extends Q<TR> { }
    }

    @Sig(s="<TY:Ljava/lang/Object;>Ljava/lang/Object;",
        t="CS{<TA{TY:cC{java/lang/Object}}>C{java/lang/Object}}")
    static class Y<TY> {
        // no sig
        class P { }

        @Sig(s="<TQ:Ljava/lang/Object;>LTest$Y<TTY;>.P;",
            t="CS{<TA{TQ:cC{java/lang/Object}}>C{C{Test$Y<S{TY}>}.P}}")
        class Q<TQ> extends P { }

        @Sig(s="<TR:Ljava/lang/Object;>LTest$Y<TTY;>.Q<TTR;>;",
            t="CS{<TA{TR:cC{java/lang/Object}}>C{C{Test$Y<S{TY}>}.Q<S{TR}>}}")
        class R<TR> extends Q<TR> {
            // no sig
            class R1 { }

            @Sig(s="<TR2:Ljava/lang/Object;>LTest$Y<TTY;>.R<TTR;>.R1;",
                t="CS{<TA{TR2:cC{java/lang/Object}}>C{C{C{Test$Y<S{TY}>}.R<S{TR}>}.R1}}")
            class R2<TR2> extends R1 { }
        }

        @Sig(s="LTest$Y<TTY;>.Q<TTY;>;", t="C{C{Test$Y<S{TY}>}.Q<S{TY}>}")
        class S extends Q<TY> {
            // no sig
            class S1 { }

            @Sig(s="<TS2:Ljava/lang/Object;>LTest$Y<TTY;>.S.S1;",
                t="CS{<TA{TS2:cC{java/lang/Object}}>C{C{C{Test$Y<S{TY}>}.S}.S1}}")
            class S2<TS2> extends S1 { }

            @Sig(s="LTest$Y<TTY;>.S.S2<TTY;>;",
                t="C{C{C{Test$Y<S{TY}>}.S}.S2<S{TY}>}")
            class S3 extends S2<TY> { }
        }
    }
}


