/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8013852 8031744
 * @summary Annotations on types
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.processing
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @build JavacTestingAbstractProcessor DPrinter BasicAnnoTests
 * @compile/process -XDaccessInternalAPI -processor BasicAnnoTests -proc:only BasicAnnoTests.java
 */

import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Target;
import java.util.ArrayList;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Name;

import static com.sun.tools.javac.code.Attribute.Array;
import static com.sun.tools.javac.code.Attribute.Constant;
import static com.sun.tools.javac.code.Attribute.Compound;

/**
 * The test scans this file looking for test cases annotated with @Test.
 */
public class BasicAnnoTests extends JavacTestingAbstractProcessor {
    DPrinter dprinter;
    PrintWriter out;
    boolean verbose = true;

    @Override
    public void init(ProcessingEnvironment pEnv) {
        super.init(pEnv);
        dprinter = new DPrinter(((JavacProcessingEnvironment) pEnv).getContext());
        out = dprinter.out;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        TestElementScanner s = new TestElementScanner();
        for (Element e: roundEnv.getRootElements()) {
            s.scan(e, null);
        }
        return true;
    }

    void error(Element e, String msg) {
        messager.printMessage(Kind.ERROR, msg, e);
        errors++;
    }

    int errors;

    /**
     * Scan an element looking for declarations annotated with @Test.
     * Run a TestTypeScanner on the annotations that are found.
     */
    class TestElementScanner extends ElementScanner<Void,Void> {
        public Void scan(Element elem, Void ignore) {
            List<AnnotationMirror> tests = new ArrayList<>();
            AnnotationMirror test = getAnnotation(elem, Test.class.getName().replace('$', '.'));
            if (test != null) {
                tests.add(test);
            }
            tests.addAll(getAnnotations(elem, Tests.class.getName().replace('$', '.')));

            if (tests.size() > 0) {
                out.println("Test: " + elem + " " + test);
                TestTypeScanner s = new TestTypeScanner(elem, tests, types);
                s.test(elem.asType());
                out.println();
            }
            return super.scan(elem, ignore);
        }
    }

    /**
     * Scan the type of an element, looking for an annotation
     * to match the expected annotation specified in the @Test annotation.
     */
    class TestTypeScanner extends TypeScanner<Void, Void> {
        Element elem;
        NavigableMap<Integer, AnnotationMirror> toBeFound;
        int count = 0;
        Set<TypeMirror> seen = new HashSet<>();

        TestTypeScanner(Element elem, List<AnnotationMirror> tests, Types types) {
            super(types);
            this.elem = elem;

            NavigableMap<Integer, AnnotationMirror> testByPos = new TreeMap<>();
            for (AnnotationMirror test : tests) {
                for (int pos : getPosn(test)) {
                    testByPos.put(pos, test);
                }
            }
            this.toBeFound = testByPos;
        }

        public void test(TypeMirror t) {
            scan(t, null);
        }

        @Override
        Void scan(TypeMirror t, Void ignore) {
            if (t == null)
                return DEFAULT_VALUE;

            if (!seen.contains(t)) {
                try {
                    seen.add(t);
                    if (verbose)
                        out.println("scan " + count + ": " + t);
                    if (toBeFound.size() > 0) {
                        if (toBeFound.firstKey().equals(count)) {
                            AnnotationMirror test = toBeFound.pollFirstEntry().getValue();
                            String annoType = getAnnoType(test);
                            AnnotationMirror anno = getAnnotation(t, annoType);
                            if (anno == null) {
                                error(elem, "annotation not found on " + count + ": " + t);
                            } else {
                                String v = getValue(anno, "value").toString();
                                if (v.equals(getExpect(test))) {
                                    out.println("found " + anno + " as expected");
                                } else {
                                    error(elem, "Unexpected value: " + v + ", expected: " + getExpect(test));
                                }
                            }
                        } else if (count > toBeFound.firstKey()) {
                            rescue();
                        } else {
                            List<? extends AnnotationMirror> annos = t.getAnnotationMirrors();
                            if (annos.size() > 0) {
                                for (AnnotationMirror a : annos)
                                    error(elem, "annotation " + a + " found on " + count + ": " + t);
                            }
                        }
                    } else {
                        List<? extends AnnotationMirror> annos = t.getAnnotationMirrors();
                        if (annos.size() > 0) {
                            for (AnnotationMirror a : annos)
                                error(elem, "annotation " + a + " found on " + count + ": " + t);
                        }
                    }
                    count++;
                    return super.scan(t, ignore);

                } finally {
                    seen.remove(t);
                }
            }

            return DEFAULT_VALUE;

        }

        private void rescue() {
            while (toBeFound.size() > 0 && toBeFound.firstKey() >= count)
                toBeFound.pollFirstEntry();
        }
    }

    /** Get the position value from an element annotated with a @Test annotation mirror. */
    static int[] getPosn(Element elem) {
        return elem.getAnnotation(Test.class).posn();
    }

    /** Get the position value from a @Test annotation mirror. */
    static Integer[] getPosn(AnnotationMirror test) {
        AnnotationValue v = getValue(test, "posn");
        Object value = v.getValue();
        Integer i = 0;
        if (value instanceof Constant) {
            i = (Integer)((Constant)value).getValue();
            Integer[] res = new Integer[1];
            res[0] = i;
            return res;
        } else if (value instanceof List) {
            List<Constant> l = (List<Constant>)value;
            Integer[] res = new Integer[l.size()];
            for (int c = 0; c < l.size(); c++) {
                res[c] = (Integer)l.get(c).getValue();
            }
            return res;
        }
        return null;
    }

    /** Get the expect value from an @Test annotation mirror. */
    static String getExpect(AnnotationMirror test) {
        AnnotationValue v = getValue(test, "expect");
        return (String) v.getValue();
    }

    /** Get the annoType value from an @Test annotation mirror. */
    static String getAnnoType(AnnotationMirror test) {
        AnnotationValue v = getValue(test, "annoType");
        TypeMirror m = (TypeMirror) v.getValue();
        return m.toString();
    }

    /**
     * Get a specific annotation mirror from an annotated construct.
     */
    static AnnotationMirror getAnnotation(AnnotatedConstruct e, String name) {
        for (AnnotationMirror m: e.getAnnotationMirrors()) {
            TypeElement te = (TypeElement) m.getAnnotationType().asElement();
            if (te.getQualifiedName().contentEquals(name)) {
                return m;
            }
        }
        return null;
    }

    static List<AnnotationMirror> getAnnotations(Element e, String name) {
        Name valueName = ((Symbol)e).getSimpleName().table.names.value;
        List<AnnotationMirror> res = new ArrayList<>();

        for (AnnotationMirror m : e.getAnnotationMirrors()) {
            TypeElement te = (TypeElement) m.getAnnotationType().asElement();
            if (te.getQualifiedName().contentEquals(name)) {
                Compound theAnno = (Compound)m;
                Array valueArray = (Array)theAnno.member(valueName);
                for (Attribute a : valueArray.getValue()) {
                    AnnotationMirror theMirror = (AnnotationMirror) a;

                    res.add(theMirror);
                }
            }
        }
        return res;
    }

    /**
     * Get a specific value from an annotation mirror.
     */
    static AnnotationValue getValue(AnnotationMirror anno, String name) {
        Map<? extends ExecutableElement, ? extends AnnotationValue> map = anno.getElementValues();
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e: map.entrySet()) {
            if (e.getKey().getSimpleName().contentEquals(name)) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * The Language Model API does not provide a type scanner, so provide
     * one sufficient for our needs.
     */
    static class TypeScanner<R, P> extends SimpleTypeVisitor<R, P> {
        private Types types;

        public TypeScanner(Types types) {
            super();
            this.types = types;
        }

        @Override
        public R visitArray(ArrayType t, P p) {
            scan(t.getComponentType(), p);
            return super.visitArray(t, p);
        }

        @Override
        public R visitExecutable(ExecutableType t, P p) {
            //out.println("  type parameters: " + t.getTypeVariables());
            scan(t.getTypeVariables(), p);
            //out.println("  return: " + t.getReturnType());
            scan(t.getReturnType(), p);
            //out.println("  receiver: " + t.getReceiverTypes());
            scan(t.getReceiverType(), p);
            //out.println("  params: " + t.getParameterTypes());
            scan(t.getParameterTypes(), p);
            //out.println("  throws: " + t.getThrownTypes());
            scan(t.getThrownTypes(), p);
            return super.visitExecutable(t, p);
        }

        @Override
        public R visitDeclared(DeclaredType t, P p) {
            scan(t.getTypeArguments(), p);
            // don't scan enclosing
            scan(types.directSupertypes(t), p);
            return super.visitDeclared(t, p);
        }

        @Override
        public R visitIntersection(IntersectionType t, P p) {
            scan(t.getBounds(), p);
            return super.visitIntersection(t, p);
        }

        @Override
        public R visitTypeVariable(TypeVariable t, P p) {
            scan(t.getLowerBound(), p);
            scan(t.getUpperBound(), p);
            return super.visitTypeVariable(t, p);
        }

        @Override
        public R visitWildcard(WildcardType t, P p) {
            scan(t.getExtendsBound(), p);
            scan(t.getSuperBound(), p);
            return super.visitWildcard(t, p);
        }

        R scan(TypeMirror t) {
            return scan(t, null);
        }

        R scan(TypeMirror t, P p) {
            return (t == null) ? DEFAULT_VALUE : t.accept(this, p);
        }

        R scan(Iterable<? extends TypeMirror> iter, P p) {
            if (iter == null)
                return DEFAULT_VALUE;
            R result = DEFAULT_VALUE;
            for (TypeMirror t: iter)
                result = scan(t, p);
            return result;
        }
    }

    /** Annotation to identify test cases. */
    @Repeatable(Tests.class)
    @interface Test {
        /** Where to look for the annotation, expressed as a scan index. */
        int[] posn();
        /** The annotation to look for. */
        Class<? extends Annotation> annoType();
        /** The string representation of the annotation's value. */
        String expect();
    }

    @interface Tests {
        Test[] value();
    }

    /** Type annotation to use in test cases. */
    @Target(ElementType.TYPE_USE)
    public @interface TA {
        int value();
    }
    @Target(ElementType.TYPE_USE)
    public @interface TB {
        int value();
    }

    // Test cases

    // TODO: add more cases for arrays
    //       all annotated
    //       all but one annotated
    //             vary position of one not annotated
    //       only one annotated
    //             vary position of one annotated
    //       the three above with the corner case of the ambiguos decl + type anno added

    @Test(posn=0, annoType=TA.class, expect="1")
    public @TA(1) int f1;

    @Test(posn=0, annoType=TA.class, expect="11")
    @TA(11) public int f11;

    @Test(posn=1, annoType=TA.class, expect="111")
    @TA(111) public int [] f111;

    @Test(posn=1, annoType=TA.class, expect="1120")
    @Test(posn=0, annoType=TB.class, expect="1121")
    @TA(1120) public int @TB(1121) [] f112;

    @Test(posn=0, annoType=TB.class, expect="11211")
    @Test(posn=1, annoType=TA.class, expect="11200")
    public @TA(11200) int @TB(11211) [] f112b;

    @Test(posn=1, annoType=TB.class, expect="1131")
    @Test(posn=2, annoType=TA.class, expect="1130")
    @TA(1130) public int [] @TB(1131) [] f113;

    @Test(posn=5, annoType=TA.class, expect="12")
    public @TA(12) int [] [] [] [] [] f12;

    @Test(posn=6, annoType=TA.class, expect="13")
    public @TA(13) int [] [] [] [] [] [] f13;

    @Test(posn=7, annoType=TA.class, expect="14")
    @TA(14) public int [] [] [] [] [] [] [] f14;

    @Test(posn=6, annoType=TA.class, expect="150")
    @Test(posn=7, annoType=TB.class, expect="151")
    @TB(151) public int [] [] [] [] [] [] @TA(150) [] f15;

    @Test(posn=0, annoType=TB.class, expect="1511")
    @Test(posn=3, annoType=TA.class, expect="1512")
    @Test(posn=6, annoType=TA.class, expect="150")
    @Test(posn=7, annoType=TB.class, expect="151")
    @TB(151) public int @TB(1511) [] [] [] @TA(1512) [] [] [] @TA(150) [] f15b;

    @Test(posn=0, annoType=TB.class, expect="1521")
    @Test(posn=3, annoType=TA.class, expect="1522")
    @Test(posn=6, annoType=TA.class, expect="152")
    public int @TB(1521) [] [] [] @TA(1522) [] [] [] @TA(152) [] f15c;

    @Test(posn=5, annoType=TA.class, expect="160")
    @Test(posn=6, annoType=TB.class, expect="161")
    public int [] [] [] [] [] @TA(160) [] @TB(161) [] f16;

    @Test(posn=0, annoType=TA.class, expect="2")
    public int @TA(2) [] f2;

    @Test(posn=0, annoType=TB.class, expect="33")
    @Test(posn=1, annoType=TA.class, expect="3")
    public @TA(3) int @TB(33) [] f3;

    @Test(posn=3, annoType=TA.class, expect="4")
    public int m1(@TA(4) float a) throws Exception { return 0; }

    @Test(posn=1, annoType=TA.class, expect="5")
    public @TA(5) int m2(float a) throws Exception { return 0; }

    @Test(posn=4, annoType=TA.class, expect="6")
    public int m3(float a) throws @TA(6) Exception { return 0; }

    // Also tests that a decl anno on a typevar doesn't show up on the Type
    @Test(posn=8, annoType=TA.class, expect="8")
    public <@TA(7) M> M m4(@TA(8) float a) throws Exception { return null; }

    // Also tests that a decl anno on a typevar doesn't show up on the Type
    @Test(posn=4, annoType=TA.class, expect="10")
    public class Inner1<@TA(9) S> extends @TA(10) Object implements Cloneable {}

    // Also tests that a decl anno on a typevar doesn't show up on the Type
    @Test(posn=5, annoType=TA.class, expect="12")
    public class Inner2<@TA(11) S> extends Object implements @TA(12) Cloneable {}

    @Test(posn={3,6}, annoType=TA.class, expect="13")
    public <M extends @TA(13) Object> M m5(float a) { return null; }

    @Test(posn=3, annoType=TA.class, expect="14")
    public class Inner3<QQQ extends @TA(14) Map> {}

    @Test(posn=4, annoType=TA.class, expect="15")
    public class Inner4<T extends @TA(15) Object & Cloneable & Serializable> {}

    @Test(posn=5, annoType=TA.class, expect="16")
    public class Inner5<T extends Object & @TA(16) Cloneable & Serializable> {}

    @Test(posn=7, annoType=TA.class, expect="17")
    public class Inner6<T extends Object & Cloneable & @TA(17) Serializable> {}

    // Test annotated bounds

    @Test(posn=1, annoType=TA.class, expect="18")
    public Set<@TA(18) ? extends Object> f4;

    @Test(posn=2, annoType=TA.class, expect="19")
    public Set<? extends @TA(19) Object> f5;

    @Test(posn=3, annoType=TA.class, expect="20")
    public Set<? extends Set<@TA(20) ? extends Object>> f6;

    @Test(posn=4, annoType=TA.class, expect="21")
    public Set<? extends Set<? extends @TA(21) Object>> f7;

    @Test(posn=1, annoType=TA.class, expect="22")
    public Set<@TA(22) ?> f8;

    @Test(posn=1, annoType=TA.class, expect="23")
    public Set<@TA(23) ? super Object> f9;

    // Test type use annotations on uses of type variables
    @Test(posn=6, annoType = TA.class, expect = "25")
    @Test(posn=6, annoType = TB.class, expect = "26")
    <T> void m6(@TA(25) @TB(26) T t) { }

    class Inner7<T> {
        @Test(posn=0, annoType = TA.class, expect = "30")
        @Test(posn=0, annoType = TB.class, expect = "31")
        @TA(30) @TB(31) T f;
    }

    // Test type use annotations on uses of type variables
    @Test(posn=6, annoType = TB.class, expect = "41")
    <@TA(40) T> void m7(@TB(41) T t) { }

    class Inner8<@TA(50) T> {
        @Test(posn=0, annoType = TB.class, expect = "51")
        @TB(51) T f;
    }

    // Test type use annotations on uses of Class types
    @Test(posn=6, annoType = TA.class, expect = "60")
    @Test(posn=6, annoType = TB.class, expect = "61")
    <T> void m60(@TA(60) @TB(61) String t) { }

    class Inner70<T> {
        @Test(posn=0, annoType = TA.class, expect = "70")
        @Test(posn=0, annoType = TB.class, expect = "71")
        @TA(70) @TB(71) String f;
    }

    // Test type use annotations on uses of type variables
    @Test(posn=6, annoType = TB.class, expect = "81")
    <@TA(80) T> void m80(@TB(81) String t) { }

    class Inner90<@TA(90) T> {
        @Test(posn=0, annoType = TB.class, expect = "91")
        @TB(91) String f;
    }

    // Recursive bound
    @Test(posn=4, annoType = TB.class, expect = "100")
    class Inner100<T extends Inner100<@TB(100) T>> {
    }
}
