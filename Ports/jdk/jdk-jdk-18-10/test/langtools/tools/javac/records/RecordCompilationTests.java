/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * RecordCompilationTests
 *
 * @test
 * @bug 8250629 8252307 8247352 8241151 8246774 8259025
 * @summary Negative compilation tests, and positive compilation (smoke) tests for records
 * @library /lib/combo /tools/lib /tools/javac/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.code
 *      jdk.compiler/com.sun.tools.javac.util
 *      jdk.jdeps/com.sun.tools.classfile
 * @build JavacTestingAbstractProcessor
 * @run testng/othervm -DuseAP=false RecordCompilationTests
 * @run testng/othervm -DuseAP=true RecordCompilationTests
 */

import java.io.File;

import java.lang.annotation.ElementType;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import com.sun.tools.javac.util.Assert;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeMirror;

import com.sun.tools.classfile.AccessFlags;
import com.sun.tools.classfile.Annotation;
import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.Attributes;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.ConstantPool;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Fieldref_info;
import com.sun.tools.classfile.ConstantPool.CPInfo;
import com.sun.tools.classfile.Field;
import com.sun.tools.classfile.Instruction;
import com.sun.tools.classfile.Method;
import com.sun.tools.classfile.Record_attribute;
import com.sun.tools.classfile.Record_attribute.ComponentInfo;
import com.sun.tools.classfile.RuntimeAnnotations_attribute;
import com.sun.tools.classfile.RuntimeTypeAnnotations_attribute;
import com.sun.tools.classfile.RuntimeVisibleAnnotations_attribute;
import com.sun.tools.classfile.RuntimeVisibleParameterAnnotations_attribute;
import com.sun.tools.classfile.RuntimeVisibleTypeAnnotations_attribute;
import com.sun.tools.classfile.TypeAnnotation;

import com.sun.tools.javac.api.ClientCodeWrapper.DiagnosticSourceUnwrapper;
import com.sun.tools.javac.code.Attribute.TypeCompound;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.util.JCDiagnostic;

import org.testng.annotations.Test;
import tools.javac.combo.CompilationTestCase;

import static java.lang.annotation.ElementType.*;
import static org.testng.Assert.assertEquals;

/** Records are the first feature which sports automatic injection of (declarative and type) annotations : from a
 *  given record component to one or more record members, if applicable.
 *  This implies that the record's implementation can be stressed with the presence of annotation processors. Which is
 *  something the implementator could easily skip. For this reason this test is executed twice, once without the
 *  presence of any annotation processor and one with a simple annotation processor (which does not annotation processing
 *  at all) just to force at least a round of annotation processing.
 *
 *  Tests needing special compilation options need to store current options, set its customs options by invoking method
 *  `setCompileOptions` and then reset the previous compilation options for other tests. To see an example of this check
 *  method: testAnnos()
 */

@Test
public class RecordCompilationTests extends CompilationTestCase {
    private static String[] OPTIONS_WITH_AP = {"-processor", SimplestAP.class.getName()};

    private static final List<String> BAD_COMPONENT_NAMES = List.of(
            "clone", "finalize", "getClass", "hashCode",
            "notify", "notifyAll", "toString", "wait");

    /* simplest annotation processor just to force a round of annotation processing for all tests
     */
    @SupportedAnnotationTypes("*")
    public static class SimplestAP extends AbstractProcessor {
        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            return true;
        }
    }

    boolean useAP;

    public RecordCompilationTests() {
        useAP = System.getProperty("useAP", "false").equals("true");
        setDefaultFilename("R.java");
        if (useAP) {
            setCompileOptions(OPTIONS_WITH_AP);
        }
        System.out.println(useAP ? "running all tests using an annotation processor" : "running all tests without annotation processor");
    }

    public void testMalformedDeclarations() {
        assertFail("compiler.err.premature.eof", "record R()");
        assertFail("compiler.err.expected", "record R();");
        assertFail("compiler.err.illegal.start.of.type", "record R(,) { }");
        assertFail("compiler.err.illegal.start.of.type", "record R((int x)) { }");
        assertFail("compiler.err.record.header.expected", "record R { }");
        assertFail("compiler.err.expected", "record R(foo) { }");
        assertFail("compiler.err.expected", "record R(int int) { }");
        assertFail("compiler.err.mod.not.allowed.here", "abstract record R(String foo) { }");
        //assertFail("compiler.err.illegal.combination.of.modifiers", "non-sealed record R(String foo) { }");
        assertFail("compiler.err.repeated.modifier", "public public record R(String foo) { }");
        assertFail("compiler.err.repeated.modifier", "private private record R(String foo) { }");
        assertFail("compiler.err.already.defined", "record R(int x, int x) {}");
        for (String s : List.of("var", "record"))
            assertFail("compiler.err.restricted.type.not.allowed.here", "record R(# x) { }", s);
        for (String s : List.of("public", "protected", "private", "static", "final", "transient", "volatile",
                "abstract", "synchronized", "native", "strictfp")) // missing: sealed and non-sealed
            assertFail("compiler.err.record.cant.declare.field.modifiers", "record R(# String foo) { }", s);
        assertFail("compiler.err.varargs.must.be.last", "record R(int... x, int... y) {}");
        assertFail("compiler.err.instance.initializer.not.allowed.in.records", "record R(int i) { {} }");
    }

    public void testGoodDeclarations() {
        assertOK("public record R() { }");
        assertOK("record R() { }");
        assertOK("record R() implements java.io.Serializable, Runnable { public void run() { } }");
        assertOK("record R(int x) { }");
        assertOK("record R(int x, int y) { }");
        assertOK("record R(int... xs) { }");
        assertOK("record R(String... ss) { }");
        assertOK("@Deprecated record R(int x, int y) { }");
        assertOK("record R(@Deprecated int x, int y) { }");
        assertOK("record R<T>(T x, T y) { }");
    }

    public void testGoodMemberDeclarations() {
        String template = "public record R(int x) {\n"
                + "    public R(int x) { this.x = x; }\n"
                + "    public int x() { return x; }\n"
                + "    public boolean equals(Object o) { return true; }\n"
                + "    public int hashCode() { return 0; }\n"
                + "    public String toString() { return null; }\n"
                + "}";
        assertOK(template);
    }

    public void testBadComponentNames() {
        for (String s : BAD_COMPONENT_NAMES)
            assertFail("compiler.err.illegal.record.component.name", "record R(int #) { } ", s);
    }

    public void testRestrictedIdentifiers() {
        for (String s : List.of("interface record { void m(); }",
                "@interface record { }",
                "class record { }",
                "record record(int x) { }",
                "enum record { A, B }",
                "class R<record> { }")) {
            assertFail(
                    "compiler.err.restricted.type.not.allowed",
                    diagWrapper -> {
                        JCDiagnostic diagnostic = ((DiagnosticSourceUnwrapper)diagWrapper).d;
                        Object[] args = diagnostic.getArgs();
                        Assert.check(args.length == 2);
                        Assert.check(args[1].toString().equals("JDK14"));
                    },
                    s);
        }
    }

    public void testValidMembers() {
        for (String s : List.of("record X(int j) { }",
                "interface I { }",
                "static { }",
                "enum E { A, B }",
                "class C { }"
        )) {
            assertOK("record R(int i) { # }", s);
        }
    }

    public void testCyclic() {
        // Cyclic records are OK, but cyclic inline records would not be
        assertOK("record R(R r) { }");
    }

    public void testBadExtends() {
        assertFail("compiler.err.expected", "record R(int x) extends Object { }");
        assertFail("compiler.err.expected", "record R(int x) {}\n"
                + "record R2(int x) extends R { }");
        assertFail("compiler.err.cant.inherit.from.final", "record R(int x) {}\n"
                + "class C extends R { }");
    }

    public void testNoExtendRecord() {
        assertFail("compiler.err.invalid.supertype.record",
                   """
                   class R extends Record {
                       public String toString() { return null; }
                       public int hashCode() { return 0; }
                       public boolean equals(Object o) { return false; }
                   }
                   """
        );
    }

    public void testFieldDeclarations() {
        // static fields are OK
        assertOK("public record R(int x) {\n" +
                "    static int I = 1;\n" +
                "    static final String S = \"Hello World!\";\n" +
                "    static private Object O = null;\n" +
                "    static protected Object O2 = null;\n" +
                "}");

        // instance fields are not
        assertFail("compiler.err.record.cannot.declare.instance.fields",
                "public record R(int x) {\n" +
                        "    private final int y = 0;" +
                        "}");

        // mutable instance fields definitely not
        assertFail("compiler.err.record.cannot.declare.instance.fields",
                "public record R(int x) {\n" +
                        "    private int y = 0;" +
                        "}");

        // redeclaring components also not
        assertFail("compiler.err.record.cannot.declare.instance.fields",
                "public record R(int x) {\n" +
                        "    private final int x;" +
                        "}");
    }

    public void testAccessorRedeclaration() {
        assertOK("public record R(int x) {\n" +
                "    public int x() { return x; };" +
                "}");

        assertOK("public record R(int... x) {\n" +
                "    public int[] x() { return x; };" +
                "}");

        assertOK("public record R(int x) {\n" +
                "    public final int x() { return 0; };" +
                "}");

        assertOK("public record R(int x) {\n" +
                "    public final int x() { return 0; };" +
                "}");

        assertFail("compiler.err.invalid.accessor.method.in.record",
                "public record R(int x) {\n" +
                        "    final int x() { return 0; };" +
                        "}");

        assertFail("compiler.err.invalid.accessor.method.in.record",
                "public record R(int x) {\n" +
                        "    int x() { return 0; };" +
                        "}");

        assertFail("compiler.err.invalid.accessor.method.in.record",
                "public record R(int x) {\n" +
                        "    private int x() { return 0; };" +
                        "}");

        assertFail("compiler.err.invalid.accessor.method.in.record",
                   "public record R(int x) {\n" +
                   "    public int x() throws Exception { return 0; };" +
                   "}");

        for (String s : List.of("List", "List<?>", "Object", "ArrayList<String>", "int"))
            assertFail("compiler.err.invalid.accessor.method.in.record",
                    "import java.util.*;\n" +
                            "public record R(List<String> x) {\n" +
                            "    public # x() { return null; };" +
                            "}", s);

        assertFail("compiler.err.invalid.accessor.method.in.record",
                "public record R(int x) {\n" +
                        "    public <T> int x() { return x; };" +
                        "}");

        assertFail("compiler.err.invalid.accessor.method.in.record",
                "public record R(int x) {\n" +
                        "    static private final j = 0;" +
                        "    static public int x() { return j; };" +
                        "}");
    }

    public void testConstructorRedeclaration() {
        for (String goodCtor : List.of(
                "public R(int x) { this(x, 0); }",
                "public R(int x, int y) { this.x = x; this.y = y; }",
                "public R { }"))
            assertOK("record R(int x, int y) { # }", goodCtor);

        assertOK("import java.util.*; record R(String x, String y) {  public R { Objects.requireNonNull(x); Objects.requireNonNull(y); } }");

        // The lambda expressions in the constructor should be compiled successfully.
        assertOK("""
                import static java.util.Objects.*;
                record R(String v) {
                    R {
                        requireNonNull(v, () -> "v must be provided");
                        requireNonNullElseGet(v, () -> "w");
                    }
                }""");

        // Not OK to redeclare canonical without DA
        assertFail("compiler.err.var.might.not.have.been.initialized", "record R(int x, int y) { # }",
                   "public R(int x, int y) { this.x = x; }");

        // Not OK to rearrange or change names
        for (String s : List.of("public R(int y, int x) { this.x = x; this.y = y; }",
                                "public R(int _x, int _y) { this.x = _x; this.y = _y; }"))
            assertFail("compiler.err.invalid.canonical.constructor.in.record", "record R(int x, int y) { # }", s);

        // ctor args must match types
        assertFail("compiler.err.invalid.canonical.constructor.in.record",
                "import java.util.*;\n" +
                        "record R(List<String> list) { # }",
                "R(List list) { this.list = list; }");

        // canonical ctor should not throw checked exceptions
        assertFail("compiler.err.invalid.canonical.constructor.in.record",
                   "record R() { # }",
                   "public R() throws Exception { }");

        // same for compact
        assertFail("compiler.err.invalid.canonical.constructor.in.record",
                "record R() { # }",
                "public R throws Exception { }");

        // not even unchecked exceptions
        assertFail("compiler.err.invalid.canonical.constructor.in.record",
                "record R() { # }",
                 "public R() throws IllegalArgumentException { }");

        // ditto
        assertFail("compiler.err.invalid.canonical.constructor.in.record",
                "record R() { # }",
                "public R throws IllegalArgumentException { }");

        // If types match, names must match
        assertFail("compiler.err.invalid.canonical.constructor.in.record",
                   "record R(int x, int y) { public R(int y, int x) { this.x = this.y = 0; }}");

        // first invocation should be one to the canonical
        assertFail("compiler.err.first.statement.must.be.call.to.another.constructor",
                "record R(int x, int y) { public R(int y, int x, int z) { this.x = this.y = 0; } }");

        assertFail("compiler.err.first.statement.must.be.call.to.another.constructor",
                "record R(int x, int y) { public R(int y, int x, int z) { super(); this.x = this.y = 0; } }");

        assertOK("record R(int x, int y) { " +
                 "    public R(int x, int y, int z) { this(x, y); } " +
                 "}");

        assertOK("record R(int x) { " +
                "    public R(int x, int y) { this(x, y, 0); } " +
                "    public R(int x, int y, int z) { this(x); } " +
                "}");

        assertFail("compiler.err.invalid.canonical.constructor.in.record",
                "record R<T>(T a) { # }",
                "public <T> R {}");

        assertFail("compiler.err.invalid.canonical.constructor.in.record",
                "record R(int i) { # }",
                "public <T> R(int i) { this.i = i; }");

        assertFail("compiler.err.invalid.canonical.constructor.in.record",
                "record R<T>(T a) { # }",
                "public <T> R(T a) { this.a = a; }");

        assertFail("compiler.err.invalid.canonical.constructor.in.record",
                "record R(int a) { # }",
                "public R(int a) { super(); this.a = a; }");
    }

    public void testAnnotationCriteria() {
        String imports = "import java.lang.annotation.*;\n";
        String template = "@Target({ # }) @interface A {}\n";
        EnumMap<ElementType, String> annotations = new EnumMap<>(ElementType.class);
        for (ElementType e : values())
            annotations.put(e, template.replace("#", "ElementType." + e.name()));
        EnumSet<ElementType> goodSet = EnumSet.of(RECORD_COMPONENT, FIELD, METHOD, PARAMETER, TYPE_USE);
        EnumSet<ElementType> badSet = EnumSet.of(CONSTRUCTOR, PACKAGE, TYPE, LOCAL_VARIABLE, ANNOTATION_TYPE, TYPE_PARAMETER, MODULE);

        assertEquals(goodSet.size() + badSet.size(), values().length);
        String A_GOOD = template.replace("#",
                                         goodSet.stream().map(ElementType::name).map(s -> "ElementType." + s).collect(Collectors.joining(",")));
        String A_BAD = template.replace("#",
                                        badSet.stream().map(ElementType::name).map(s -> "ElementType." + s).collect(Collectors.joining(",")));
        String A_ALL = template.replace("#",
                                        Stream.of(ElementType.values()).map(ElementType::name).map(s -> "ElementType." + s).collect(Collectors.joining(",")));
        String A_NONE = "@interface A {}";

        for (ElementType e : goodSet)
            assertOK(imports + annotations.get(e) + "record R(@A int x) { }");
        assertOK(imports + A_GOOD + "record R(@A int x) { }");
        assertOK(imports + A_ALL + "record R(@A int x) { }");
        assertOK(imports + A_NONE);

        for (ElementType e : badSet) {
            assertFail("compiler.err.annotation.type.not.applicable", imports + annotations.get(e) + "record R(@A int x) { }");
        }

        assertFail("compiler.err.annotation.type.not.applicable", imports + A_BAD + "record R(@A int x) { }");

        // TODO: OK to redeclare with or without same annos
    }

    public void testNestedRecords() {
        String template = "class R { \n" +
                          "    # record RR(int a) { }\n" +
                          "}";

        for (String s : List.of("", "static", "final",
                                "private", "public", "protected",
                                "private static", "public static", "private static final"))
            assertOK(template, s);

        for (String s : List.of("class C { }",
                                "static class C { }",
                                "enum X { A; }",
                                "interface I { }",
                                "record RR(int y) { }"))
            assertOK("record R(int x) { # }", s);
    }

    public void testDuplicatedMember() {
        String template
                = "    record R(int i) {\n" +
                  "        public int i() { return i; }\n" +
                  "        public int i() { return i; }\n" +
                  "    }";
        assertFail("compiler.err.already.defined", template);
    }

    public void testStaticLocals() {
        // static locals can't capture local variables, instance fields or type variables
        for (String s : List.of(
                "record RR(int x) { public int x() { return y; }};",
                "record RR(int x) { public int x() { return z; }};",
                "record RR(int x) { public int x() { return instance; }};",
                "record RR(T t) {};",
                "record RR(U u) {};",

                "interface I { default int x() { return y; }};",
                "interface I { default int x() { return z; }};",
                "interface I { default int x() { return instance; }};",
                "interface I { default int x(T t) { return 0; }};",
                "interface I { default int x(U u) { return 0; }};",

                "enum E { A; int x() { return y; }};",
                "enum E { A; int x() { return z; }};",
                "enum E { A; int x() { return instance; }};",
                "enum E { A; int x(T t) { return 0; }};",
                "enum E { A; int x(U u) { return 0; }};"
        )) {
            assertFail("compiler.err.non-static.cant.be.ref",
                """
                class R<T> {
                    int instance = 0;
                    <U> U m(int y) {
                        int z;
                        #S
                        return null;
                    }
                }
                """.replaceFirst("#S", s));
        }

        // a similar example but a bit more complex
        for (String s : List.of(
                "record R() { void test1() { class X { void test2() { System.err.println(localVar); } } } }",
                "record R() { void test1() { class X { void test2() { System.err.println(param); } } } }",
                "record R() {void test1() { class X { void test2() { System.err.println(instanceField); } } } }",
                "record R() { void test1() { class X { T t; } } }",
                "record R() { void test1() { class X { U u; } } }",

                "interface I { default void test1() { class X { void test2() { System.err.println(localVar); } } } }",
                "interface I() { default void test1() { class X { void test2() {System.err.println(param);} } } }",
                "interface I { default void test1() { class X { void test2() { System.err.println(instanceField); } } } }",
                "interface I { default void test1() { class X { T t; } } }",
                "interface I() { default void test1() { class X {U u;} } }",

                "enum E { A; void test1() { class X { void test2() { System.err.println(localVar); } } } }",
                "enum E { A; void test1() { class X { void test2() {System.err.println(param);} } } }",
                "enum E { A; void test1() { class X { void test2() { System.err.println(instanceField); } } } }",
                "enum E { A; void test1() { class X { T t; } } }",
                "enum E { A; void test1() { class X {U u;} } }"
        )) {
            assertFail("compiler.err.non-static.cant.be.ref",
                    """
                    class C<T> {
                        String instanceField = "instance";
                        static <U> U m(String param) {
                            String localVar = "local";
                            #S
                            return null;
                    }
                }
                """.replaceFirst("#S", s));
        }

        // can't self-shadow
        for (String s : List.of("record R() {}", "interface R {}", "enum R { A }")) {
            assertFail("compiler.err.already.defined", "class R { void m() { #S } }".replaceFirst("#S", s));
        }

        // can't be explicitly static
        for (String s : List.of("static record RR() { }", "static interface I {}", "static enum E { A }")) {
            assertFail("compiler.err.illegal.start.of.expr", "class R { void m() { #S } }".replaceFirst("#S", s));
        }

        // but static fields can be accessed
        for (String s : List.of(
                "record RR() { public int x() { return z; } };",
                "interface I { default int x() { return z; } }",
                "enum E { A; int x() { return z; } }"
        )) {
            assertOK("class R { static int z = 0; void m() { #S } }".replaceFirst("#S", s));
        }

        // local records can also be final
        assertOK("class R { void m() { final record RR(int x) { }; } }");
    }

    public void testStaticDefinitionsInInnerClasses() {
        // static defs in inner classes can't capture instance fields or type variables
        for (String s : List.of(
                """
                record R() {
                    void test() { System.err.println(field); }
                }
                """,
                """
                record R() {
                    void test(T t) {}
                }
                """,
                """
                record R() {
                    void test1() {
                        class X {
                            void test2() { System.err.println(field); }
                        }
                    }
                }
                """,
                """
                record R() {
                    void test1() {
                        class X { void test2(T t) {} }
                    }
                }
                """,

                """
                interface I {
                    default void test() { System.err.println(field); }
                }
                """,
                """
                interface I {
                    default void test(T t) {}
                }
                """,
                """
                interface I {
                    default void test1() {
                        class X {
                            void test2() { System.err.println(field); }
                        }
                    }
                }
                """,
                """
                interface I {
                    default void test1() {
                        class X { void test2(T t) {} }
                    }
                }
                """,

                """
                enum E {
                    A;
                    void test() { System.err.println(field); }
                }
                """,
                """
                enum E {
                    A;
                    void test(T t) {}
                }
                """,
                """
                enum E {
                    A;
                    void test1() {
                        class X {
                            void test2() { System.err.println(field); }
                        }
                    }
                }
                """,
                """
                enum E {
                    A;
                    void test1() {
                        class X { void test2(T t) {} }
                    }
                }
                """,

                """
                static class SC {
                    void test() { System.err.println(field); }
                }
                """,
                """
                static class SC {
                    void test(T t) {}
                }
                """,
                """
                static class SC {
                    void test1() {
                        class X {
                            void test2() { System.err.println(field); }
                        }
                    }
                }
                """,
                """
                static class SC {
                    void test1() {
                        class X { void test2(T t) {} }
                    }
                }
                """
        )) {
            assertFail("compiler.err.non-static.cant.be.ref",
                    """
                    class C<T> {
                        String field = "field";
                        class Inner {
                            #S
                        }
                    }
                    """.replaceFirst("#S", s));
        }

        // another, more complex, example
        // static defs in inner classes can't capture instance locals, fields or type variables
        for (String s : List.of(
                """
                record R() {
                    void test() { System.err.println(field); }
                }
                """,
                """
                record R() {
                    void test1() {
                        class X { void test2() { System.err.println(field); } }
                    }
                }
                """,
                """
                record R() {
                    void test() { System.err.println(param); }
                }
                """,
                """
                record R() {
                    void test1() {
                        class X { void test2() { System.err.println(param); } }
                    }
                }
                """,
                """
                record R() {
                    void test() { System.err.println(local); }
                }
                """,
                """
                record R() {
                    void test1() {
                        class X { void test2() { System.err.println(local); } }
                    }
                }
                """,
                """
                record R() {
                    void test(T t) {}
                }
                """,
                """
                record R() {
                    void test(U u) {}
                }
                """,
                """
                record R() {
                    void test1() {
                        class X { void test2(T t) {} }
                    }
                }
                """,
                """
                record R() {
                    void test1() {
                        class X { void test2(U u) {} }
                    }
                }
                """,

                """
                interface I {
                    default void test() { System.err.println(field); }
                }
                """,
                """
                interface I {
                    default void test1() {
                        class X {
                            void test2() { System.err.println(field); }
                        }
                    }
                }
                """,
                """
                interface I {
                    default void test() { System.err.println(param); }
                }
                """,
                """
                interface I {
                    default void test1() {
                        class X {
                            void test2() { System.err.println(param); }
                        }
                    }
                }
                """,
                """
                interface I {
                    default void test() { System.err.println(local); }
                }
                """,
                """
                interface I {
                    default void test1() {
                        class X {
                            void test2() { System.err.println(local); }
                        }
                    }
                }
                """,
                """
                interface I {
                    default void test(T t) {}
                }
                """,
                """
                interface I {
                    default void test(U u) {}
                }
                """,
                """
                interface I {
                    default void test1() {
                        class X { void test2(T t) {} }
                    }
                }
                """,
                """
                interface I {
                    default void test1() {
                        class X { void test2(U u) {} }
                    }
                }
                """,

                """
                enum E {
                    A;
                    void test() { System.err.println(field); }
                }
                """,
                """
                enum E {
                    A;
                    void test1() {
                        class X {
                            void test2() { System.err.println(field); }
                        }
                    }
                }
                """,
                """
                enum E {
                    A;
                    void test() { System.err.println(param); }
                }
                """,
                """
                enum E {
                    A;
                    void test1() {
                        class X {
                            void test2() { System.err.println(param); }
                        }
                    }
                }
                """,
                """
                enum E {
                    A;
                    void test() { System.err.println(local); }
                }
                """,
                """
                enum E {
                    A;
                    void test1() {
                        class X {
                            void test2() { System.err.println(local); }
                        }
                    }
                }
                """,
                """
                enum E {
                    A;
                    void test(T t) {}
                }
                """,
                """
                enum E {
                    A;
                    void test(U u) {}
                }
                """,
                """
                enum E {
                    A;
                    void test1() {
                        class X { void test2(T t) {} }
                    }
                }
                """,
                """
                enum E {
                    A;
                    void test1() {
                        class X { void test2(U u) {} }
                    }
                }
                """,

                """
                static class SC {
                    void test() { System.err.println(field); }
                }
                """,
                """
                static class SC {
                    void test1() {
                        class X {
                            void test2() { System.err.println(field); }
                        }
                    }
                }
                """,
                """
                static class SC {
                    void test() { System.err.println(param); }
                }
                """,
                """
                static class SC {
                    void test1() {
                        class X {
                            void test2() { System.err.println(param); }
                        }
                    }
                }
                """,
                """
                static class SC {
                    void test() { System.err.println(local); }
                }
                """,
                """
                static class SC {
                    void test1() {
                        class X {
                            void test2() { System.err.println(local); }
                        }
                    }
                }
                """,
                """
                static class SC {
                    void test(T t) {}
                }
                """,
                """
                static class SC {
                    void test(U u) {}
                }
                """,
                """
                static class SC {
                    void test1() {
                        class X { void test2(T t) {} }
                    }
                }
                """,
                """
                static class SC {
                    void test1() {
                        class X { void test2(U u) {} }
                    }
                }
                """
        )) {
            assertFail("compiler.err.non-static.cant.be.ref",
                    """
                    class C<T> {
                        String field = "field";
                        <U> U m(String param) {
                            String local = "local";
                            class Local {
                                class Inner { #S }
                            }
                            return null;
                        }
                    }
                    """.replaceFirst("#S", s));
        }

        // inner classes can contain static methods too
        assertOK(
                """
                class C {
                    class Inner {
                        // static method inside inner class
                        static void m() {}
                    }
                }
                """
        );

        assertOK(
                """
                class C {
                     void m() {
                         new Object() {
                            // static method inside inner class
                            static void m() {}
                         };
                     }
                }
                """
        );

        // but still non-static declarations can't be accessed from a static method inside a local class
        for (String s : List.of(
                "System.out.println(localVar)",
                "System.out.println(param)",
                "System.out.println(field)",
                "T t",
                "U u"
        )) {
            assertFail("compiler.err.non-static.cant.be.ref",
                    """
                    class C<T> {
                        int field = 0;
                        <U> void foo(int param) {
                            int localVar = 1;
                            class Local {
                                static void m() {
                                    #S;
                                }
                            }
                        }
                    }
                    """.replaceFirst("#S", s));
        }
    }

    public void testReturnInCanonical_Compact() {
        assertFail("compiler.err.invalid.canonical.constructor.in.record", "record R(int x) { # }",
                "public R { return; }");
        assertFail("compiler.err.invalid.canonical.constructor.in.record", "record R(int x) { # }",
                "public R { if (i < 0) { return; }}");
        assertOK("record R(int x) { public R(int x) { this.x = x; return; } }");
        assertOK("record R(int x) { public R { Runnable r = () -> { return; };} }");
    }

    public void testArgumentsAreNotFinalInCompact() {
        assertOK(
                """
                record R(int x) {
                    public R {
                        x++;
                    }
                }
                """);
    }

    public void testNoNativeMethods() {
        assertFail("compiler.err.mod.not.allowed.here", "record R(int x) { # }",
                "public native R {}");
        assertFail("compiler.err.mod.not.allowed.here", "record R(int x) { # }",
                "public native void m();");
    }

    public void testRecordsInsideInner() {
        assertOK(
                """
                class Outer {
                    class Inner {
                        record R(int a) {}
                    }
                }
                """
        );
        assertOK(
                """
                class Outer {
                    public void test() {
                        class Inner extends Outer {
                            record R(int i) {}
                        }
                    }
                }
                """);
        assertOK(
                """
                class Outer {
                    Runnable run = new Runnable() {
                        record TestRecord(int i) {}
                        public void run() {}
                    };
                }
                """);
        assertOK(
                """
                class Outer {
                    void m() {
                        record A() {
                            record B() { }
                        }
                    }
                }
                """);
    }

    public void testAnnoInsideLocalOrAnonymous() {
        assertFail("compiler.err.annotation.decl.not.allowed.here",
                """
                class Outer {
                    public void test() {
                        class Local {
                            @interface A {}
                        }
                    }
                }
                """);
        assertFail("compiler.err.annotation.decl.not.allowed.here",
                """
                class Outer {
                    public void test() {
                        interface I {
                            @interface A {}
                        }
                    }
                }
                """);
        assertFail("compiler.err.annotation.decl.not.allowed.here",
                """
                class Outer {
                    public void test() {
                        record R() {
                            @interface A {}
                        }
                    }
                }
                """);
        assertFail("compiler.err.annotation.decl.not.allowed.here",
                """
                class Outer {
                    public void test() {
                        enum E {
                            E1;
                            @interface A {}
                        }
                    }
                }
                """);

        assertFail("compiler.err.annotation.decl.not.allowed.here",
                """
                class Outer {
                    public void test() {
                        class Local1 {
                            class Local2 {
                                @interface A {}
                            }
                        }
                    }
                }
                """);
        assertFail("compiler.err.annotation.decl.not.allowed.here",
                """
                class Outer {
                    public void test() {
                        class Local {
                            interface I {
                                @interface A {}
                            }
                        }
                    }
                }
                """);
        assertFail("compiler.err.annotation.decl.not.allowed.here",
                """
                class Outer {
                    public void test() {
                        class Local {
                            record R() {
                                @interface A {}
                            }
                        }
                    }
                }
                """);
        assertFail("compiler.err.annotation.decl.not.allowed.here",
                """
                class Outer {
                    public void test() {
                        class Local {
                            enum E {
                                E1;
                                @interface A {}
                            }
                        }
                    }
                }
                """);

        assertFail("compiler.err.annotation.decl.not.allowed.here",
                """
                class Outer {
                    Runnable run = new Runnable() {
                        @interface A {}
                        public void run() {}
                    };
                }
                """);
    }

    public void testReceiverParameter() {
        assertFail("compiler.err.receiver.parameter.not.applicable.constructor.toplevel.class",
                """
                record R(int i) {
                    public R(R this, int i) {
                        this.i = i;
                    }
                }
                """);
        assertFail("compiler.err.non-static.cant.be.ref",
                """
                class Outer {
                    record R(int i) {
                        public R(Outer Outer.this, int i) {
                            this.i = i;
                        }
                    }
                }
                """);
        assertOK(
                """
                record R(int i) {
                    void m(R this) {}
                    public int i(R this) { return i; }
                }
                """);
    }

    public void testOnlyOneFieldRef() throws Exception {
        int numberOfFieldRefs = 0;
        File dir = assertOK(true, "record R(int recordComponent) {}");
        for (final File fileEntry : dir.listFiles()) {
            if (fileEntry.getName().equals("R.class")) {
                ClassFile classFile = ClassFile.read(fileEntry);
                for (CPInfo cpInfo : classFile.constant_pool.entries()) {
                    if (cpInfo instanceof ConstantPool.CONSTANT_Fieldref_info) {
                        numberOfFieldRefs++;
                        ConstantPool.CONSTANT_NameAndType_info nameAndType =
                                (ConstantPool.CONSTANT_NameAndType_info)classFile.constant_pool
                                        .get(((ConstantPool.CONSTANT_Fieldref_info)cpInfo).name_and_type_index);
                        Assert.check(nameAndType.getName().equals("recordComponent"));
                    }
                }
            }
        }
        Assert.check(numberOfFieldRefs == 1);
    }

    /*  check that fields are initialized in a canonical constructor in the same declaration order as the corresponding
     *  record component
     */
    public void testCheckInitializationOrderInCompactConstructor() throws Exception {
        int putField1 = -1;
        int putField2 = -1;
        File dir = assertOK(true, "record R(int i, String s) { R {} }");
        for (final File fileEntry : dir.listFiles()) {
            if (fileEntry.getName().equals("R.class")) {
                ClassFile classFile = ClassFile.read(fileEntry);
                for (Method method : classFile.methods) {
                    if (method.getName(classFile.constant_pool).equals("<init>")) {
                        Code_attribute code_attribute = (Code_attribute) method.attributes.get("Code");
                        for (Instruction instruction : code_attribute.getInstructions()) {
                            if (instruction.getMnemonic().equals("putfield")) {
                                if (putField1 != -1 && putField2 != -1) {
                                    throw new AssertionError("was expecting only two putfield instructions in this method");
                                }
                                if (putField1 == -1) {
                                    putField1 = instruction.getShort(1);
                                } else if (putField2 == -1) {
                                    putField2 = instruction.getShort(1);
                                }
                            }
                        }
                        // now we need to check that we are assigning to `i` first and to `s` afterwards
                        CONSTANT_Fieldref_info fieldref_info1 = (CONSTANT_Fieldref_info)classFile.constant_pool.get(putField1);
                        if (!fieldref_info1.getNameAndTypeInfo().getName().equals("i")) {
                            throw new AssertionError("was expecting variable name 'i'");
                        }

                        CONSTANT_Fieldref_info fieldref_info2 = (CONSTANT_Fieldref_info)classFile.constant_pool.get(putField2);
                        if (!fieldref_info2.getNameAndTypeInfo().getName().equals("s")) {
                            throw new AssertionError("was expecting variable name 's'");
                        }
                    }
                }
            }
        }
    }

    public void testAcceptRecordId() {
        String[] previousOptions = getCompileOptions();
        String[] testOptions = {/* no options */};
        setCompileOptions(testOptions);
        assertFail("compiler.err.illegal.start.of.type",
                "class R {\n" +
                "    record RR(int i) {\n" +
                "        return null;\n" +
                "    }\n" +
                "    class record {}\n" +
                "}");
        setCompileOptions(previousOptions);
    }

    public void testAnnos() throws Exception {
        String[] previousOptions = getCompileOptions();
        String srcTemplate =
                """
                    import java.lang.annotation.*;
                    @Target({#TARGET})
                    @Retention(RetentionPolicy.RUNTIME)
                    @interface Anno { }

                    record R(@Anno String s) {}
                """;

        // testing several combinations, adding even more combinations won't add too much value
        List<String> annoApplicableTargets = List.of(
                "ElementType.FIELD",
                "ElementType.METHOD",
                "ElementType.PARAMETER",
                "ElementType.RECORD_COMPONENT",
                "ElementType.TYPE_USE",
                "ElementType.TYPE_USE,ElementType.FIELD",
                "ElementType.TYPE_USE,ElementType.METHOD",
                "ElementType.TYPE_USE,ElementType.PARAMETER",
                "ElementType.TYPE_USE,ElementType.RECORD_COMPONENT",
                "ElementType.TYPE_USE,ElementType.FIELD,ElementType.METHOD",
                "ElementType.TYPE_USE,ElementType.FIELD,ElementType.PARAMETER",
                "ElementType.TYPE_USE,ElementType.FIELD,ElementType.RECORD_COMPONENT",
                "ElementType.FIELD,ElementType.TYPE_USE",
                "ElementType.FIELD,ElementType.CONSTRUCTOR",
                "ElementType.FIELD,ElementType.LOCAL_VARIABLE",
                "ElementType.FIELD,ElementType.ANNOTATION_TYPE",
                "ElementType.FIELD,ElementType.PACKAGE",
                "ElementType.FIELD,ElementType.TYPE_PARAMETER",
                "ElementType.FIELD,ElementType.MODULE",
                "ElementType.METHOD,ElementType.TYPE_USE",
                "ElementType.PARAMETER,ElementType.TYPE_USE",
                "ElementType.RECORD_COMPONENT,ElementType.TYPE_USE",
                "ElementType.FIELD,ElementType.METHOD,ElementType.TYPE_USE",
                "ElementType.FIELD,ElementType.PARAMETER,ElementType.TYPE_USE",
                "ElementType.FIELD,ElementType.RECORD_COMPONENT,ElementType.TYPE_USE"
        );

        String[] generalOptions = {
                "-processor", Processor.class.getName(),
                "-Atargets="
        };

        for (String target : annoApplicableTargets) {
            String code = srcTemplate.replaceFirst("#TARGET", target);
            String[] testOptions = generalOptions.clone();
            testOptions[testOptions.length - 1] = testOptions[testOptions.length - 1] + target;
            setCompileOptions(testOptions);

            File dir = assertOK(true, code);

            ClassFile classFile = ClassFile.read(findClassFileOrFail(dir, "R.class"));

            // field first
            Assert.check(classFile.fields.length == 1);
            Field field = classFile.fields[0];
            /* if FIELD is one of the targets then there must be a declaration annotation applied to the field, apart from
             * the type annotation
             */
            if (target.contains("ElementType.FIELD")) {
                checkAnno(classFile,
                        (RuntimeAnnotations_attribute)findAttributeOrFail(
                                field.attributes,
                                RuntimeVisibleAnnotations_attribute.class),
                        "Anno");
            } else {
                assertAttributeNotPresent(field.attributes, RuntimeVisibleAnnotations_attribute.class);
            }

            // lets check now for the type annotation
            if (target.contains("ElementType.TYPE_USE")) {
                checkTypeAnno(
                        classFile,
                        (RuntimeVisibleTypeAnnotations_attribute)findAttributeOrFail(field.attributes, RuntimeVisibleTypeAnnotations_attribute.class),
                        "FIELD",
                        "Anno");
            } else {
                assertAttributeNotPresent(field.attributes, RuntimeVisibleTypeAnnotations_attribute.class);
            }

            // checking for the annotation on the corresponding parameter of the canonical constructor
            Method init = findMethodOrFail(classFile, "<init>");
            /* if PARAMETER is one of the targets then there must be a declaration annotation applied to the parameter, apart from
             * the type annotation
             */
            if (target.contains("ElementType.PARAMETER")) {
                checkParameterAnno(classFile,
                        (RuntimeVisibleParameterAnnotations_attribute)findAttributeOrFail(
                                init.attributes,
                                RuntimeVisibleParameterAnnotations_attribute.class),
                        "Anno");
            } else {
                assertAttributeNotPresent(init.attributes, RuntimeVisibleAnnotations_attribute.class);
            }
            // let's check now for the type annotation
            if (target.contains("ElementType.TYPE_USE")) {
                checkTypeAnno(
                        classFile,
                        (RuntimeVisibleTypeAnnotations_attribute) findAttributeOrFail(init.attributes, RuntimeVisibleTypeAnnotations_attribute.class),
                        "METHOD_FORMAL_PARAMETER", "Anno");
            } else {
                assertAttributeNotPresent(init.attributes, RuntimeVisibleTypeAnnotations_attribute.class);
            }

            // checking for the annotation in the accessor
            Method accessor = findMethodOrFail(classFile, "s");
            /* if METHOD is one of the targets then there must be a declaration annotation applied to the accessor, apart from
             * the type annotation
             */
            if (target.contains("ElementType.METHOD")) {
                checkAnno(classFile,
                        (RuntimeAnnotations_attribute)findAttributeOrFail(
                                accessor.attributes,
                                RuntimeVisibleAnnotations_attribute.class),
                        "Anno");
            } else {
                assertAttributeNotPresent(accessor.attributes, RuntimeVisibleAnnotations_attribute.class);
            }
            // let's check now for the type annotation
            if (target.contains("ElementType.TYPE_USE")) {
                checkTypeAnno(
                        classFile,
                        (RuntimeVisibleTypeAnnotations_attribute)findAttributeOrFail(accessor.attributes, RuntimeVisibleTypeAnnotations_attribute.class),
                        "METHOD_RETURN", "Anno");
            } else {
                assertAttributeNotPresent(accessor.attributes, RuntimeVisibleTypeAnnotations_attribute.class);
            }

            // checking for the annotation in the Record attribute
            Record_attribute record = (Record_attribute)findAttributeOrFail(classFile.attributes, Record_attribute.class);
            Assert.check(record.component_count == 1);
            /* if RECORD_COMPONENT is one of the targets then there must be a declaration annotation applied to the
             * field, apart from the type annotation
             */
            if (target.contains("ElementType.RECORD_COMPONENT")) {
                checkAnno(classFile,
                        (RuntimeAnnotations_attribute)findAttributeOrFail(
                                record.component_info_arr[0].attributes,
                                RuntimeVisibleAnnotations_attribute.class),
                        "Anno");
            } else {
                assertAttributeNotPresent(record.component_info_arr[0].attributes, RuntimeVisibleAnnotations_attribute.class);
            }
            // lets check now for the type annotation
            if (target.contains("ElementType.TYPE_USE")) {
                checkTypeAnno(
                        classFile,
                        (RuntimeVisibleTypeAnnotations_attribute)findAttributeOrFail(
                                record.component_info_arr[0].attributes,
                                RuntimeVisibleTypeAnnotations_attribute.class),
                        "FIELD", "Anno");
            } else {
                assertAttributeNotPresent(record.component_info_arr[0].attributes, RuntimeVisibleTypeAnnotations_attribute.class);
            }
        }

        // let's reset the default compiler options for other tests
        setCompileOptions(previousOptions);
    }

    private void checkTypeAnno(ClassFile classFile,
                               RuntimeTypeAnnotations_attribute rtAnnos,
                               String positionType,
                               String annoName) throws Exception {
        // containing only one type annotation
        Assert.check(rtAnnos.annotations.length == 1);
        TypeAnnotation tAnno = (TypeAnnotation)rtAnnos.annotations[0];
        Assert.check(tAnno.position.type.toString().equals(positionType));
        String annotationName = classFile.constant_pool.getUTF8Value(tAnno.annotation.type_index).toString().substring(1);
        Assert.check(annotationName.startsWith(annoName));
    }

    private void checkAnno(ClassFile classFile,
                           RuntimeAnnotations_attribute rAnnos,
                           String annoName) throws Exception {
        // containing only one type annotation
        Assert.check(rAnnos.annotations.length == 1);
        Annotation anno = (Annotation)rAnnos.annotations[0];
        String annotationName = classFile.constant_pool.getUTF8Value(anno.type_index).toString().substring(1);
        Assert.check(annotationName.startsWith(annoName));
    }

    // special case for parameter annotations
    private void checkParameterAnno(ClassFile classFile,
                           RuntimeVisibleParameterAnnotations_attribute rAnnos,
                           String annoName) throws Exception {
        // containing only one type annotation
        Assert.check(rAnnos.parameter_annotations.length == 1);
        Assert.check(rAnnos.parameter_annotations[0].length == 1);
        Annotation anno = (Annotation)rAnnos.parameter_annotations[0][0];
        String annotationName = classFile.constant_pool.getUTF8Value(anno.type_index).toString().substring(1);
        Assert.check(annotationName.startsWith(annoName));
    }

    private File findClassFileOrFail(File dir, String name) {
        for (final File fileEntry : dir.listFiles()) {
            if (fileEntry.getName().equals("R.class")) {
                return fileEntry;
            }
        }
        throw new AssertionError("file not found");
    }

    private Method findMethodOrFail(ClassFile classFile, String name) throws Exception {
        for (Method method : classFile.methods) {
            if (method.getName(classFile.constant_pool).equals(name)) {
                return method;
            }
        }
        throw new AssertionError("method not found");
    }

    private Attribute findAttributeOrFail(Attributes attributes, Class<? extends Attribute> attrClass) {
        for (Attribute attribute : attributes) {
            if (attribute.getClass() == attrClass) {
                return attribute;
            }
        }
        throw new AssertionError("attribute not found");
    }

    private void assertAttributeNotPresent(Attributes attributes, Class<? extends Attribute> attrClass) {
        for (Attribute attribute : attributes) {
            if (attribute.getClass() == attrClass) {
                throw new AssertionError("attribute not expected");
            }
        }
    }

    @SupportedAnnotationTypes("*")
    public static final class Processor extends JavacTestingAbstractProcessor {

        String targets;
        int numberOfTypeAnnotations;

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            targets = processingEnv.getOptions().get("targets");
            for (TypeElement te : annotations) {
                if (te.toString().equals("Anno")) {
                    checkElements(te, roundEnv, targets);
                    if (targets.contains("TYPE_USE")) {
                        Element element = processingEnv.getElementUtils().getTypeElement("R");
                        numberOfTypeAnnotations = 0;
                        checkTypeAnnotations(element);
                        Assert.check(numberOfTypeAnnotations == 4);
                    }
                }
            }
            return true;
        }

        void checkElements(TypeElement te, RoundEnvironment renv, String targets) {
            Set<? extends Element> annoElements = renv.getElementsAnnotatedWith(te);
            Set<String> targetSet = new HashSet<>(Arrays.asList(targets.split(",")));
            // we will check for type annotation in another method
            targetSet.remove("ElementType.TYPE_USE");
            for (Element e : annoElements) {
                Symbol s = (Symbol) e;
                switch (s.getKind()) {
                    case FIELD:
                        Assert.check(targetSet.contains("ElementType.FIELD"));
                        targetSet.remove("ElementType.FIELD");
                        break;
                    case METHOD:
                        Assert.check(targetSet.contains("ElementType.METHOD"));
                        targetSet.remove("ElementType.METHOD");
                        break;
                    case PARAMETER:
                        Assert.check(targetSet.contains("ElementType.PARAMETER"));
                        targetSet.remove("ElementType.PARAMETER");
                        break;
                    case RECORD_COMPONENT:
                        Assert.check(targetSet.contains("ElementType.RECORD_COMPONENT"));
                        targetSet.remove("ElementType.RECORD_COMPONENT");
                        break;
                    default:
                        throw new AssertionError("unexpected element kind");
                }
            }
        }

        private void checkTypeAnnotations(Element rootElement) {
            new ElementScanner<Void, Void>() {
                @Override public Void visitVariable(VariableElement e, Void p) {
                    Symbol s = (Symbol) e;
                    if (s.getKind() == ElementKind.FIELD ||
                            s.getKind() == ElementKind.PARAMETER &&
                            s.name.toString().equals("s")) {
                        int currentTAs = numberOfTypeAnnotations;
                        verifyTypeAnnotations(e.asType().getAnnotationMirrors());
                        Assert.check(currentTAs + 1 == numberOfTypeAnnotations);
                    }
                    return null;
                }
                @Override
                public Void visitExecutable(ExecutableElement e, Void p) {
                    Symbol s = (Symbol) e;
                    if (s.getKind() == ElementKind.METHOD &&
                                    s.name.toString().equals("s")) {
                        int currentTAs = numberOfTypeAnnotations;
                        verifyTypeAnnotations(e.getReturnType().getAnnotationMirrors());
                        Assert.check(currentTAs + 1 == numberOfTypeAnnotations);
                    }
                    scan(e.getParameters(), p);
                    return null;
                }
                @Override public Void visitRecordComponent(RecordComponentElement e, Void p) {
                    int currentTAs = numberOfTypeAnnotations;
                    verifyTypeAnnotations(e.asType().getAnnotationMirrors());
                    Assert.check(currentTAs + 1 == numberOfTypeAnnotations);
                    return null;
                }
            }.scan(rootElement, null);
        }

        private void verifyTypeAnnotations(Iterable<? extends AnnotationMirror> annotations) {
            for (AnnotationMirror mirror : annotations) {
                Assert.check(mirror.toString().startsWith("@Anno"));
                if (mirror instanceof TypeCompound) {
                    numberOfTypeAnnotations++;
                }
            }
        }
    }

    public void testMethodsInheritedFromRecordArePublicAndFinal() throws Exception {
        int numberOfFieldRefs = 0;
        File dir = assertOK(true, "record R() {}");
        for (final File fileEntry : dir.listFiles()) {
            if (fileEntry.getName().equals("R.class")) {
                ClassFile classFile = ClassFile.read(fileEntry);
                for (Method method : classFile.methods)
                    switch (method.getName(classFile.constant_pool)) {
                        case "toString", "equals", "hashCode" ->
                            Assert.check(method.access_flags.is(AccessFlags.ACC_PUBLIC) && method.access_flags.is(AccessFlags.ACC_FINAL));
                        default -> { /* do nothing */ }
                    }
            }
        }
    }

    private static final List<String> ACCESSIBILITY = List.of(
            "public", "protected", "", "private");

    public void testCanonicalAccessibility() throws Exception {
        // accessibility of canonical can't be stronger than that of the record type
        for (String a1 : ACCESSIBILITY) {
            for (String a2 : ACCESSIBILITY) {
                if (protection(a2) > protection(a1)) {
                    assertFail("compiler.err.invalid.canonical.constructor.in.record", "class R {# record RR() { # RR {} } }", a1, a2);
                } else {
                    assertOK("class R {# record RR() { # RR {} } }", a1, a2);
                }
            }
        }

        // now lets check that when compiler the compiler generates the canonical, it has the same accessibility
        // as the record type
        for (String a : ACCESSIBILITY) {
            File dir = assertOK(true, "class R {# record RR() {} }", a);
            for (final File fileEntry : dir.listFiles()) {
                if (fileEntry.getName().equals("R$RR.class")) {
                    ClassFile classFile = ClassFile.read(fileEntry);
                    for (Method method : classFile.methods)
                        if (method.getName(classFile.constant_pool).equals("<init>")) {
                            Assert.check(method.access_flags.flags == accessFlag(a),
                                    "was expecting access flag " + accessFlag(a) + " but found " + method.access_flags.flags);
                        }
                }
            }
        }
    }

    private int protection(String access) {
        switch (access) {
            case "private": return 3;
            case "protected": return 1;
            case "public": return 0;
            case "": return 2;
            default:
                throw new AssertionError();
        }
    }

    private int accessFlag(String access) {
        switch (access) {
            case "private": return AccessFlags.ACC_PRIVATE;
            case "protected": return AccessFlags.ACC_PROTECTED;
            case "public": return AccessFlags.ACC_PUBLIC;
            case "": return 0;
            default:
                throw new AssertionError();
        }
    }

    public void testSameArity() {
        for (String source : List.of(
                """
                record R(int... args) {
                    public R(int... args) {
                        this.args = args;
                    }
                }
                """,
                """
                record R(int[] args) {
                    public R(int[] args) {
                        this.args = args;
                    }
                }
                """,
                """
                record R(@A int... ints) {}

                @java.lang.annotation.Target({
                        java.lang.annotation.ElementType.TYPE_USE,
                        java.lang.annotation.ElementType.RECORD_COMPONENT})
                @interface A {}
                """,
                """
                record R(@A int... ints) {
                    R(@A int... ints) {
                        this.ints = ints;
                    }
                }

                @java.lang.annotation.Target({
                        java.lang.annotation.ElementType.TYPE_USE,
                        java.lang.annotation.ElementType.RECORD_COMPONENT})
                @interface A {}
                """
        )) {
            assertOK(source);
        }

        for (String source : List.of(
                """
                record R(int... args) {
                    public R(int[] args) {
                        this.args = args;
                    }
                }
                """,
                """
                record R(int... args) {
                    public R(int[] args) {
                        this.args = args;
                    }
                }
                """,
                """
                record R(String... args) {
                    public R(String[] args) {
                        this.args = args;
                    }
                }
                """,
                """
                record R(String... args) {
                    public R(String[] args) {
                        this.args = args;
                    }
                }
                """
        )) {
            assertFail("compiler.err.invalid.canonical.constructor.in.record", source);
        }
    }

    public void testSafeVararsAnno() {
        assertFail("compiler.err.annotation.type.not.applicable",
                """
                @SafeVarargs
                record R<T>(T... t) {}
                """,
                """
                @SafeVarargs
                record R<T>(T... t) {
                    R(T... t) {
                        this.t = t;
                    }
                }
                """
        );

        assertOK(
                """
                record R<T>(T... t) {
                    @SafeVarargs
                    R(T... t) {
                        this.t = t;
                    }
                }
                """
        );

        appendCompileOptions("-Xlint:unchecked");
        assertOKWithWarning("compiler.warn.unchecked.varargs.non.reifiable.type",
                """
                record R<T>(T... t) {
                    R(T... t) {
                        this.t = t;
                    }
                }
                """
        );
        removeLastCompileOptions(1);

        assertOK(
                """
                @SuppressWarnings("unchecked")
                record R<T>(T... t) {
                    R(T... t) {
                        this.t = t;
                    }
                }
                """
        );

        assertOK(
                """
                record R<T>(T... t) {
                    @SuppressWarnings("unchecked")
                    R(T... t) {
                        this.t = t;
                    }
                }
                """
        );
    }

    public void testOverrideAtAccessor() {
        assertOK(
                """
                record R(int i) {
                    @Override
                    public int i() { return i; }
                }
                """,
                """
                record R(int i, int j) {
                    @Override
                    public int i() { return i; }
                    public int j() { return j; }
                }
                """,
                """
                interface I { int i(); }
                record R(int i) implements I {
                    @Override
                    public int i() { return i; }
                }
                """,
                """
                interface I { int i(); }
                record R(int i) implements I {
                    public int i() { return i; }
                }
                """,
                """
                interface I { default int i() { return 0; } }
                record R(int i) implements I {
                    @Override
                    public int i() { return i; }
                }
                """
        );
    }

    public void testNoAssigmentInsideCompactRecord() {
        assertFail("compiler.err.cant.assign.val.to.final.var",
                """
                record R(int i) {
                    R {
                        this.i = i;
                    }
                }
                """
        );
        assertFail("compiler.err.cant.assign.val.to.final.var",
                """
                record R(int i) {
                    R {
                        (this).i = i;
                    }
                }
                """
        );
    }

    public void testNoNPEStaticAnnotatedFields() {
        assertOK(
                """
                import java.lang.annotation.Native;
                record R() {
                    @Native public static final int i = 0;
                }
                """
        );
        assertOK(
                """
                import java.lang.annotation.Native;
                class Outer {
                    record R() {
                        @Native public static final int i = 0;
                    }
                }
                """
        );
        assertOK(
                """
                import java.lang.annotation.Native;
                class Outer {
                    void m() {
                        record R () {
                            @Native public static final int i = 0;
                        }
                    }
                }
                """
        );
    }

    public void testDoNotAllowCStyleArraySyntaxForRecComponents() {
        assertFail("compiler.err.record.component.and.old.array.syntax",
                """
                record R(int i[]) {}
                """
        );
        assertFail("compiler.err.record.component.and.old.array.syntax",
                """
                record R(String s[]) {}
                """
        );
        assertFail("compiler.err.record.component.and.old.array.syntax",
                """
                record R<T>(T t[]) {}
                """
        );
    }

    public void testNoWarningForSerializableRecords() {
        if (!useAP) {
            /* dont execute this test when the default annotation processor is on as it will fail due to
             * spurious warnings
             */
            appendCompileOptions("-Werror", "-Xlint:serial");
            assertOK(
                    """
                    import java.io.*;
                    record R() implements java.io.Serializable {}
                    """
            );
            removeLastCompileOptions(2);
        }
    }

    public void testAnnotationsOnVarargsRecComp() {
        assertOK(
                """
                import java.lang.annotation.*;

                @Target({ElementType.TYPE_USE})
                @interface Simple {}

                record R(@Simple int... val) {
                    static void test() {
                        R rec = new R(10, 20);
                    }
                }
                """
        );
        assertOK(
                """
                import java.lang.annotation.*;

                @Target({ElementType.TYPE_USE})
                @interface SimpleContainer{ Simple[] value(); }

                @Repeatable(SimpleContainer.class)
                @Target({ElementType.TYPE_USE})
                @interface Simple {}

                record R(@Simple int... val) {
                    static void test() {
                        R rec = new R(10, 20);
                    }
                }
                """
        );
    }

    public void testSaveVarargsAnno() {
        // the compiler would generate an erronous accessor
        assertFail("compiler.err.varargs.invalid.trustme.anno",
                """
                record R(@SafeVarargs String... s) {}
                """
        );
        // but this is OK
        assertOK(
                """
                record R(@SafeVarargs String... s) {
                    public String[] s() { return s; }
                }
                """
        );
    }
}
