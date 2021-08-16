/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8159111 8159740
 * @summary test wrappers and dependencies
 * @modules jdk.jshell/jdk.jshell
 * @build KullaTesting
 * @run testng WrapperTest
 */

import java.util.Collection;
import java.util.List;
import org.testng.annotations.Test;
import jdk.jshell.ErroneousSnippet;
import jdk.jshell.Snippet;
import jdk.jshell.Snippet.Kind;
import jdk.jshell.SourceCodeAnalysis.SnippetWrapper;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static jdk.jshell.Snippet.Status.RECOVERABLE_DEFINED;
import static jdk.jshell.Snippet.Status.VALID;

@Test
public class WrapperTest extends KullaTesting {

    public void testMethod() {
        String src = "void glib() { System.out.println(\"hello\"); }";
        List<SnippetWrapper> swl = getState().sourceCodeAnalysis().wrappers(src);
        assertEquals(swl.size(), 1, "unexpected list length");
        assertWrapperHas(swl.get(0), src, Kind.METHOD, "void", "glib", "println");
        assertPosition(swl.get(0), src, 0, 4);
        assertPosition(swl.get(0), src, 5, 4);
        assertPosition(swl.get(0), src, 15, 6);

        Snippet g = methodKey(assertEval(src, added(VALID)));
        SnippetWrapper swg = getState().sourceCodeAnalysis().wrapper(g);
        assertWrapperHas(swg, src, Kind.METHOD, "void", "glib", "println");
        assertPosition(swg, src, 0, 4);
        assertPosition(swg, src, 5, 4);
        assertPosition(swg, src, 15, 6);
    }

    // test 8159740
    public void testMethodCorralled() {
        String src = "void glib() { f(); }";
        //            _123456789_123456789
        Snippet g = methodKey(assertEval(src, added(RECOVERABLE_DEFINED)));
        SnippetWrapper swg = getState().sourceCodeAnalysis().wrapper(g);
        assertWrapperHas(swg, src, Kind.METHOD, "SPIResolutionException",
                "void", "glib");
        assertPosition(swg, src, 0, 4);
        assertPosition(swg, src, 5, 4);
    }

    // test 8159740
    public void testClassCorralled0() {
        String src = "class AAA { float mmm(double d1234) { return (float) (f0 * d1234); } }";
        //            _123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789
        Snippet a = classKey(assertEval(src, added(RECOVERABLE_DEFINED)));
        SnippetWrapper swa = getState().sourceCodeAnalysis().wrapper(a);
        assertWrapperHas(swa, src, Kind.TYPE_DECL, "SPIResolutionException",
                "class", "AAA", "float", "mmm", "double", "d1234");
        assertPosition(swa, src, 0, 5);
        assertPosition(swa, src, 6, 3);
        assertPosition(swa, src, 12, 5);
        assertPosition(swa, src, 18, 3);
        assertPosition(swa, src, 22, 6);
        assertPosition(swa, src, 29, 5);
    }

    // test 8159740
    public void testClassCorralled() {
        String src = "class AAA { int xxx = x0 + 4; float mmm(float ffff) { return f0 * ffff; } }";
        //            _123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789
        Snippet a = classKey(assertEval(src, added(RECOVERABLE_DEFINED)));
        SnippetWrapper swa = getState().sourceCodeAnalysis().wrapper(a);
        assertWrapperHas(swa, src, Kind.TYPE_DECL, "SPIResolutionException",
                "class", "AAA", "int", "xxx", "float", "mmm", "ffff");
        assertPosition(swa, src, 0, 5);
        assertPosition(swa, src, 6, 3);
        assertPosition(swa, src, 12, 3);
        assertPosition(swa, src, 16, 3);
        assertPosition(swa, src, 30, 5);
        assertPosition(swa, src, 36, 3);
        assertPosition(swa, src, 40, 5);
        assertPosition(swa, src, 46, 4);
    }

    // test 8159740
    public void testClassWithConstructorCorralled() {
        String src = "public class AAA { AAA(String b) {} int xxx = x0 + 4; float mmm(float ffff) { return f0 * ffff; } }";
        //            _123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789
        Snippet a = classKey(assertEval(src, added(RECOVERABLE_DEFINED)));
        SnippetWrapper swa = getState().sourceCodeAnalysis().wrapper(a);
        assertWrapperHas(swa, src, Kind.TYPE_DECL, "SPIResolutionException",
                "class", "AAA", "String", "int", "xxx", "float", "mmm", "ffff");
        assertPosition(swa, src, 7, 5);
        assertPosition(swa, src, 13, 3);
        assertPosition(swa, src, 19, 3);
        assertPosition(swa, src, 23, 5);
        assertPosition(swa, src, 30, 1);
        assertPosition(swa, src, 36, 3);
        assertPosition(swa, src, 40, 3);
        assertPosition(swa, src, 54, 5);
        assertPosition(swa, src, 60, 3);
        assertPosition(swa, src, 64, 5);
        assertPosition(swa, src, 70, 4);
    }

    // test 8159740
    public void testInterfaceCorralled() {
        String src = "interface AAA { default float mmm(double d1234) { return (float) (f0 * d1234); } }";
        //            _123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789
        Snippet a = classKey(assertEval(src, added(RECOVERABLE_DEFINED)));
        SnippetWrapper swa = getState().sourceCodeAnalysis().wrapper(a);
        assertWrapperHas(swa, src, Kind.TYPE_DECL, "SPIResolutionException",
                "interface", "AAA", "float", "mmm", "double", "d1234");
        assertPosition(swa, src, 0, 9);
        assertPosition(swa, src, 10, 3);
        assertPosition(swa, src, 16, 7);
        assertPosition(swa, src, 24, 5);
        assertPosition(swa, src, 30, 3);
        assertPosition(swa, src, 34, 6);
        assertPosition(swa, src, 41, 5);
    }

    // test 8159740
    public void testEnumCorralled() {
        String src =
                "public enum Planet {\n" +
                "    MERCURY (3.303e+23, 2.4397e6),\n" +
                "    VENUS   (4.869e+24, 6.0518e6),\n" +
                "    EARTH   (5.976e+24, 6.37814e6),\n" +
                "    MARS    (6.421e+23, 3.3972e6),\n" +
                "    JUPITER (1.9e+27,   7.1492e7),\n" +
                "    SATURN  (5.688e+26, 6.0268e7),\n" +
                "    URANUS  (8.686e+25, 2.5559e7),\n" +
                "    NEPTUNE (1.024e+26, 2.4746e7);\n" +
                "\n" +
                "    private final double mass;   // in kilograms\n" +
                "    private final double radius; // in meters\n" +
                "    Planet(double mass, double radius) {\n" +
                "        this.mass = mass;\n" +
                "        this.radius = radius;\n" +
                "    }\n" +
                "    private double mass() { return mass; }\n" +
                "    private double radius() { return radius; }\n" +
                "\n" +
                "    double surfaceGravity() {\n" +
                "        return GRAVITATIONAL_CONSTANT * mass / (radius * radius);\n" +
                "    }\n" +
                "    double surfaceWeight(double otherMass) {\n" +
                "        return otherMass * surfaceGravity();\n" +
                "    }\n" +
                "}\n";
        Snippet a = classKey(assertEval(src, added(RECOVERABLE_DEFINED)));
        SnippetWrapper swa = getState().sourceCodeAnalysis().wrapper(a);
        assertWrapperHas(swa, src, Kind.TYPE_DECL, "SPIResolutionException",
                "enum", "Planet", "double", "mass", "EARTH", "NEPTUNE", "MERCURY",
                "radius", "surfaceGravity", "surfaceWeight");
    }

    public void testMethodBad() {
        String src = "void flob() { ?????; }";
        List<SnippetWrapper> swl = getState().sourceCodeAnalysis().wrappers(src);
        assertEquals(swl.size(), 1, "unexpected list length");
        assertWrapperHas(swl.get(0), src, Kind.METHOD, "void", "flob", "?????");
        assertPosition(swl.get(0), src, 9, 2);

        Snippet f = key(assertEvalFail(src));
        assertEquals(f.kind(), Kind.ERRONEOUS);
        assertEquals(((ErroneousSnippet)f).probableKind(), Kind.METHOD);
        SnippetWrapper sw = getState().sourceCodeAnalysis().wrapper(f);
        assertWrapperHas(sw, src, Kind.METHOD, "void", "flob", "?????");
        assertPosition(swl.get(0), src, 14, 5);
    }

    public void testVar() {
        String src = "int gx = 1234;";
        List<SnippetWrapper> swl = getState().sourceCodeAnalysis().wrappers(src);
        assertEquals(swl.size(), 1, "unexpected list length");
        assertWrapperHas(swl.get(0), src, Kind.VAR, "int", "gx", "1234");
        assertPosition(swl.get(0), src, 4, 2);

        Snippet g = varKey(assertEval(src, added(VALID)));
        SnippetWrapper swg = getState().sourceCodeAnalysis().wrapper(g);
        assertWrapperHas(swg, src, Kind.VAR, "int", "gx", "1234");
        assertPosition(swg, src, 0, 3);
    }

    public void testVarBad() {
        String src = "double dd = ?????;";
        List<SnippetWrapper> swl = getState().sourceCodeAnalysis().wrappers(src);
        assertEquals(swl.size(), 1, "unexpected list length");
        assertWrapperHas(swl.get(0), src, Kind.VAR, "double", "dd", "?????");
        assertPosition(swl.get(0), src, 9, 2);

        Snippet f = key(assertEvalFail(src));
        assertEquals(f.kind(), Kind.ERRONEOUS);
        assertEquals(((ErroneousSnippet)f).probableKind(), Kind.VAR);
        SnippetWrapper sw = getState().sourceCodeAnalysis().wrapper(f);
        assertWrapperHas(sw, src, Kind.VAR, "double", "dd", "?????");
        assertPosition(swl.get(0), src, 12, 5);
    }

    public void testImport() {
        String src = "import java.lang.*;";
        List<SnippetWrapper> swl = getState().sourceCodeAnalysis().wrappers(src);
        assertEquals(swl.size(), 1, "unexpected list length");
        assertWrapperHas(swl.get(0), src, Kind.IMPORT, "import", "java.lang");
        assertPosition(swl.get(0), src, 7, 4);

        Snippet g = key(assertEval(src, added(VALID)));
        SnippetWrapper swg = getState().sourceCodeAnalysis().wrapper(g);
        assertWrapperHas(swg, src, Kind.IMPORT, "import", "java.lang");
        assertPosition(swg, src, 0, 6);
    }

    public void testImportBad() {
        String src = "import java.?????;";
        List<SnippetWrapper> swl = getState().sourceCodeAnalysis().wrappers(src);
        assertEquals(swl.size(), 1, "unexpected list length");
        assertWrapperHas(swl.get(0), src, Kind.IMPORT, "import", "?????");
        assertPosition(swl.get(0), src, 7, 4);

        Snippet f = key(assertEvalFail(src));
        assertEquals(f.kind(), Kind.ERRONEOUS);
        assertEquals(((ErroneousSnippet)f).probableKind(), Kind.IMPORT);
        SnippetWrapper sw = getState().sourceCodeAnalysis().wrapper(f);
        assertWrapperHas(sw, src, Kind.IMPORT, "import", "?????");
        assertPosition(swl.get(0), src, 0, 6);
    }

    public void testErroneous() {
        String src = "@@@@@@@@@@";
        List<SnippetWrapper> swl = getState().sourceCodeAnalysis().wrappers(src);
        assertEquals(swl.size(), 1, "unexpected list length");
        assertWrapperHas(swl.get(0), src, Kind.ERRONEOUS, "@@@@@@@@@@");
        assertPosition(swl.get(0), src, 0, 10);

        Snippet f = key(assertEvalFail(src));
        assertEquals(f.kind(), Kind.ERRONEOUS);
        assertEquals(((ErroneousSnippet)f).probableKind(), Kind.ERRONEOUS);
        SnippetWrapper sw = getState().sourceCodeAnalysis().wrapper(f);
        assertWrapperHas(sw, src, Kind.ERRONEOUS, "@@@@@@@@@@");
        assertPosition(swl.get(0), src, 0, 10);
    }

    public void testEmpty() {
        String src = "";
        List<SnippetWrapper> swl = getState().sourceCodeAnalysis().wrappers(src);
        assertEquals(swl.size(), 0, "expected empty list");
    }

    public void testDependencies() {
        Snippet a = key(assertEval("int aaa = 6;", added(VALID)));
        Snippet b = key(assertEval("class B { B(int x) { aaa = x; } }", added(VALID)));
        Snippet c = key(assertEval("B ccc() { return new B(aaa); }", added(VALID)));
        Collection<Snippet> dep;
        dep = getState().sourceCodeAnalysis().dependents(c);
        assertEquals(dep.size(), 0);
        dep = getState().sourceCodeAnalysis().dependents(b);
        assertEquals(dep.size(), 1);
        assertTrue(dep.contains(c));
        dep = getState().sourceCodeAnalysis().dependents(a);
        assertEquals(dep.size(), 2);
        assertTrue(dep.contains(c));
        assertTrue(dep.contains(b));
    }

    private void assertWrapperHas(SnippetWrapper sw, String source, Kind kind, String... has) {
        assertEquals(sw.source(), source);
        assertEquals(sw.kind(), kind);
        String s = sw.wrapped();
        if (kind == Kind.IMPORT) {
            assertHas(s, "import");
        } else {
            String cn = sw.fullClassName();
            int idx = cn.lastIndexOf(".");
            assertHas(s, cn.substring(idx+1));
            assertHas(s, "class");
        }
        for (String hx : has) {
            assertHas(s, hx);
        }
    }

    private void assertHas(String s, String has) {
        assertTrue(s.contains(has), "Expected to find '" + has + "' in: '" + s + "'");
    }

    private void assertPosition(SnippetWrapper sw, String source, int start, int length) {
        //System.err.printf("\n#assertPosition:\n#  debug-source: %s\n#  SnippetWrapper --\n#    source: %s\n#    wrapped: %s\n",
        //        source, sw.source(), sw.wrapped());
        //System.err.printf("#  start: %d    length: %d\n", start, length);
        int wpg = sw.sourceToWrappedPosition(start);
        //System.err.printf("#  wrappedPos: %d\n", wpg);
        String wrappedPart = sw.wrapped().substring(wpg, wpg+length);
        String sourcePart = source.substring(start, start+length);
        //System.err.printf("#  wrapped @ wrappedPos: %s\n", wrappedPart);
        //System.err.printf("#  source @ start: %s\n", sourcePart);

        assertEquals(wrappedPart, sourcePart,
                "position " + wpg + " in " + sw.wrapped());
        assertEquals(sw.wrappedToSourcePosition(wpg), start);
    }
}
