/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7192246 8006694 8129962
 * @summary Automatic test for checking correctness of default super/this resolution
 *  temporarily workaround combo tests are causing time out in several platforms
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper
 * @run main TestDefaultSuperCall
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask.Result;
import combo.ComboTestHelper;

public class TestDefaultSuperCall extends ComboInstance<TestDefaultSuperCall> {

    enum InterfaceKind implements ComboParameter {
        DEFAULT("interface A extends B { default void m() { } }"),
        ABSTRACT("interface A extends B { void m(); }"),
        NONE("interface A extends B { }");

        String interfaceStr;

        InterfaceKind(String interfaceStr) {
            this.interfaceStr = interfaceStr;
        }

        boolean methodDefined() {
            return this == DEFAULT;
        }

        @Override
        public String expand(String optParameter) {
            return interfaceStr;
        }
    }

    enum PruneKind implements ComboParameter {
        NO_PRUNE("interface C { }"),
        PRUNE("interface C extends A { }");

        boolean methodDefined(InterfaceKind ik) {
            return this == PRUNE &&
                    ik.methodDefined();
        }

        String interfaceStr;

        PruneKind(String interfaceStr) {
            this.interfaceStr = interfaceStr;
        }

        @Override
        public String expand(String optParameter) {
            return interfaceStr;
        }
    }

    enum QualifierKind implements ComboParameter {
        DIRECT_1("C"),
        DIRECT_2("A"),
        INDIRECT("B"),
        UNRELATED("E"),
        ENCLOSING_1("name0"),
        ENCLOSING_2("name1");

        String qualifierStr;

        QualifierKind(String qualifierStr) {
            this.qualifierStr = qualifierStr;
        }

        boolean isEnclosing() {
            return this == ENCLOSING_1 ||
                    this == ENCLOSING_2;
        }

        boolean allowSuperCall(InterfaceKind ik, PruneKind pk) {
            switch (this) {
                case DIRECT_1:
                    return pk.methodDefined(ik);
                case DIRECT_2:
                    return ik.methodDefined() && pk == PruneKind.NO_PRUNE;
                default:
                    return false;
            }
        }

        @Override
        public String expand(String optParameter) {
            return qualifierStr;
        }
    }

    enum ExprKind implements ComboParameter {
        THIS("this"),
        SUPER("super");

        String exprStr;

        ExprKind(String exprStr) {
            this.exprStr = exprStr;
        }

        @Override
        public String expand(String optParameter) {
            return exprStr;
        }
    }

    enum ElementKind implements ComboParameter {
        INTERFACE("interface name#CURR { #BODY }", true),
        INTERFACE_EXTENDS("interface name#CURR extends A, C { #BODY }", true),
        CLASS("class name#CURR { #BODY }", false),
        CLASS_EXTENDS("abstract class name#CURR implements A, C { #BODY }", false),
        STATIC_CLASS("static class name#CURR { #BODY }", true),
        STATIC_CLASS_EXTENDS("abstract static class name#CURR implements A, C { #BODY }", true),
        ANON_CLASS("new Object() { #BODY };", false),
        METHOD("void test() { #BODY }", false),
        STATIC_METHOD("static void test() { #BODY }", true),
        DEFAULT_METHOD("default void test() { #BODY }", false);

        String templateDecl;
        boolean isStatic;

        ElementKind(String templateDecl, boolean isStatic) {
            this.templateDecl = templateDecl;
            this.isStatic = isStatic;
        }

        boolean isClassDecl() {
            switch(this) {
                case METHOD:
                case STATIC_METHOD:
                case DEFAULT_METHOD:
                    return false;
                default:
                    return true;
            }
        }

        boolean isAllowedEnclosing(ElementKind ek, boolean isTop) {
            switch (this) {
                case CLASS:
                case CLASS_EXTENDS:
                    //class is implicitly static inside interface, so skip this combo
                    return ek.isClassDecl() &&
                            ek != INTERFACE && ek != INTERFACE_EXTENDS;
                case ANON_CLASS:
                    return !ek.isClassDecl();
                case METHOD:
                    return ek == CLASS || ek == CLASS_EXTENDS ||
                            ek == STATIC_CLASS || ek == STATIC_CLASS_EXTENDS ||
                            ek == ANON_CLASS;
                case INTERFACE:
                case INTERFACE_EXTENDS:
                case STATIC_CLASS:
                case STATIC_CLASS_EXTENDS:
                case STATIC_METHOD:
                    return (isTop && (ek == CLASS || ek == CLASS_EXTENDS)) ||
                            ek == STATIC_CLASS || ek == STATIC_CLASS_EXTENDS;
                case DEFAULT_METHOD:
                    return ek == INTERFACE || ek == INTERFACE_EXTENDS;
                default:
                    throw new AssertionError("Bad enclosing element kind" + this);
            }
        }

        boolean isAllowedTop() {
            switch (this) {
                case CLASS:
                case CLASS_EXTENDS:
                case INTERFACE:
                case INTERFACE_EXTENDS:
                    return true;
                default:
                    return false;
            }
        }

        boolean hasSuper() {
            return this == INTERFACE_EXTENDS ||
                    this == STATIC_CLASS_EXTENDS ||
                    this == CLASS_EXTENDS;
        }

        @Override
        public String expand(String optParameter) {
            int nextDepth = new Integer(optParameter) + 1;
            String replStr = (nextDepth <= 4) ?
                    String.format("#{ELEM[%d].%d}", nextDepth, nextDepth) :
                    "#{QUAL}.#{EXPR}.#{METH}();";
            return templateDecl
                    .replaceAll("#CURR", optParameter)
                    .replaceAll("#BODY", replStr);
        }
    }

    static class Shape {

        List<ElementKind> enclosingElements;
        List<String> enclosingNames;
        List<String> elementsWithMethod;

        Shape(ElementKind... elements) {
            enclosingElements = new ArrayList<>();
            enclosingNames = new ArrayList<>();
            elementsWithMethod = new ArrayList<>();
            int count = 0;
            String prevName = null;
            for (ElementKind ek : elements) {
                String name = "name"+count++;
                if (ek.isStatic) {
                    enclosingElements = new ArrayList<>();
                    enclosingNames = new ArrayList<>();
                }
                if (ek.isClassDecl()) {
                    enclosingElements.add(ek);
                    enclosingNames.add(name);
                } else {
                    elementsWithMethod.add(prevName);
                }
                prevName = name;
            }
        }
    }

    public static void main(String... args) throws Exception {
        new ComboTestHelper<TestDefaultSuperCall>()
                .withFilter(TestDefaultSuperCall::filterBadTopElement)
                .withFilter(TestDefaultSuperCall::filterBadIntermediateElement)
                .withFilter(TestDefaultSuperCall::filterBadTerminalElement)
                .withDimension("INTF1", (x, ik) -> x.ik = ik, InterfaceKind.values())
                .withDimension("INTF2", (x, pk) -> x.pk = pk, PruneKind.values())
                .withArrayDimension("ELEM", (x, elem, idx) -> x.elements[idx] = elem, 5, ElementKind.values())
                .withDimension("QUAL", (x, qk) -> x.qk = qk, QualifierKind.values())
                .withDimension("EXPR", (x, ek) -> x.ek = ek, ExprKind.values())
                .run(TestDefaultSuperCall::new);
    }

    InterfaceKind ik;
    PruneKind pk;
    ElementKind[] elements = new ElementKind[5];
    QualifierKind qk;
    ExprKind ek;

    boolean filterBadTopElement() {
        return elements[0].isAllowedTop();
    }

    boolean filterBadIntermediateElement() {
        for (int i = 1 ; i < 4 ; i++) {
            if (!elements[i].isAllowedEnclosing(elements[i - 1], i == 1)) {
                return false;
            }
        }
        return true;
    }

    boolean filterBadTerminalElement() {
        return elements[4].isAllowedEnclosing(elements[3], false) && !elements[4].isClassDecl();
    }

    String template = "interface E {}\n" +
                      "interface B { }\n" +
                      "#{INTF1}\n" +
                      "#{INTF2}\n" +
                      "#{ELEM[0].0}";

    @Override
    public void doWork() throws IOException {
        newCompilationTask()
                .withSourceFromTemplate(template, this::methodName)
                .analyze(this::check);
    }

    ComboParameter methodName(String parameterName) {
        switch (parameterName) {
            case "METH":
                String methodName = ek == ExprKind.THIS ? "test" : "m";
                return new ComboParameter.Constant<String>(methodName);
            default:
                return null;
        }
    }

    void check(Result<?> res) {
        Shape sh = new Shape(elements);

        boolean errorExpected = false;

        boolean badEnclosing = false;
        boolean badThis = false;
        boolean badSuper = false;

        if (qk == QualifierKind.ENCLOSING_1 &&
                sh.enclosingNames.size() < 1) {
            errorExpected |= true;
            badEnclosing = true;
        }

        if (qk == QualifierKind.ENCLOSING_2 &&
                sh.enclosingNames.size() < 2) {
            errorExpected |= true;
            badEnclosing = true;
        }

        if (ek == ExprKind.THIS) {
            boolean found = false;
            for (int i = 0; i < sh.enclosingElements.size(); i++) {
                if (sh.enclosingElements.get(i) == ElementKind.ANON_CLASS) continue;
                if (sh.enclosingNames.get(i).equals(qk.qualifierStr)) {
                    found = sh.elementsWithMethod.contains(sh.enclosingNames.get(i));
                    break;
                }
            }
            errorExpected |= !found;
            if (!found) {
                badThis = true;
            }
        }

        if (ek == ExprKind.SUPER) {

            int lastIdx = sh.enclosingElements.size() - 1;
            boolean found = lastIdx == -1 ? false :
                    sh.enclosingElements.get(lastIdx).hasSuper() &&
                    qk.allowSuperCall(ik, pk);

            errorExpected |= !found;
            if (!found) {
                badSuper = true;
            }
        }

        if (res.hasErrors() != errorExpected) {
            fail("Problem when compiling source:\n" +
                    res.compilationInfo() +
                    "\nenclosingElems: " + sh.enclosingElements +
                    "\nenclosingNames: " + sh.enclosingNames +
                    "\nelementsWithMethod: " + sh.elementsWithMethod +
                    "\nbad encl: " + badEnclosing +
                    "\nbad this: " + badThis +
                    "\nbad super: " + badSuper +
                    "\nqual kind: " + qk +
                    "\nfound error: " + res.hasErrors());
        }
    }
}
