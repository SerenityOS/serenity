/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8054717
 * @summary Make sure extraction of non-private APIs work as expected.
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.sjavac
 *          jdk.compiler/com.sun.tools.sjavac.options
 *          jdk.compiler/com.sun.tools.sjavac.pubapi
 * @build Wrapper toolbox.ToolBox toolbox.JavacTask
 * @run main Wrapper ApiExtraction
 */

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.type.TypeKind;

import com.sun.tools.sjavac.PubApiExtractor;
import com.sun.tools.sjavac.options.Options;
import com.sun.tools.sjavac.pubapi.PrimitiveTypeDesc;
import com.sun.tools.sjavac.pubapi.PubApi;
import com.sun.tools.sjavac.pubapi.PubMethod;
import com.sun.tools.sjavac.pubapi.PubType;
import com.sun.tools.sjavac.pubapi.PubVar;
import com.sun.tools.sjavac.pubapi.ReferenceTypeDesc;

import toolbox.JavacTask;
import toolbox.ToolBox;

public class ApiExtraction {
    public static void main(String[] args) throws IOException {

        String testSrc = String.join("\n",
                "import java.util.*;",
                "public final class TestClass extends Thread {",

                // Fields with various combination of modifiers
                "    private String s1 = \"str 1\";",
                "    public String s2 = \"str 2\";",
                "    protected final String s3 = \"str 3\";",
                "    static String s4 = \"str 4\";",

                // Methods with various combinations of types and modifiers
                "    protected void m1() {}",
                "    public static Map<Integer, List<String>> m2() {",
                "        return null;",
                "    }",
                "    final void m3(Set<Map<Integer, Map<String, String>>> s) {}",

                // Some inner classes
                "    static class DummyInner1 implements Runnable {",
                "        protected int field;",
                "        public void run() {}",
                "    }",
                "    final class DummyInner2 { }",
                "}");

        // Create class file to extract API from
        new JavacTask(new ToolBox()).sources(testSrc).run();

        // Extract PubApi
        Options options = Options.parseArgs("-d", "bin", "--state-dir=bin", "-cp", ".");
        PubApiExtractor pubApiExtr = new PubApiExtractor(options);
        PubApi actualApi = pubApiExtr.getPubApi("TestClass");
        pubApiExtr.close();

        // Validate result
        PubApi expectedApi = getExpectedPubApi();
        if (!expectedApi.equals(actualApi)) {
            List<String> diffs = expectedApi.diff(actualApi);
            System.out.println(diffs.size() + " differences found.");
            for (String diff : diffs) {
                System.out.println(diff);
            }
            throw new AssertionError("Actual API differs from expected API.");
        }
    }

    private static PubApi getExpectedPubApi() {

        ReferenceTypeDesc string = new ReferenceTypeDesc("java.lang.String");

        // Fields
        // (s1 is private and therefore not included)
        PubVar s2 = new PubVar(setOf(PUBLIC), string, "s2", null);
        PubVar s4 = new PubVar(setOf(STATIC), string, "s4", null);
        PubVar s3 = new PubVar(setOf(PROTECTED, FINAL), string, "s3",
                                   "\"\\u0073\\u0074\\u0072\\u0020\\u0033\"");

        // Methods
        PubMethod init = new PubMethod(setOf(PUBLIC),
                                       emptyList(),
                                       new PrimitiveTypeDesc(TypeKind.VOID),
                                       "<init>",
                                       emptyList(),
                                       emptyList());

        PubMethod clinit = new PubMethod(setOf(STATIC),
                                         emptyList(),
                                         new PrimitiveTypeDesc(TypeKind.VOID),
                                         "<clinit>",
                                         emptyList(),
                                         emptyList());

        PubMethod m1 = new PubMethod(setOf(PROTECTED),
                                     emptyList(),
                                     new PrimitiveTypeDesc(TypeKind.VOID),
                                     "m1",
                                     emptyList(),
                                     emptyList());

        PubMethod m2 = new PubMethod(setOf(PUBLIC, STATIC),
                                     emptyList(),
                                     new ReferenceTypeDesc("java.util.Map"),
                                     "m2",
                                     emptyList(),
                                     emptyList());

        PubMethod m3 = new PubMethod(setOf(FINAL),
                                     emptyList(),
                                     new PrimitiveTypeDesc(TypeKind.VOID),
                                     "m3",
                                     asList(new ReferenceTypeDesc("java.util.Set")),
                                     emptyList());

        // Complete class
        PubType testClass = new PubType(setOf(PUBLIC, FINAL),
                                        "TestClass",
                                        new PubApi(asList(getDummyInner1(), getDummyInner2()),
                                                   asList(s2, s3, s4),
                                                   asList(init, clinit, m1, m2, m3)));

        // Wrap in "package level" PubApi
        return new PubApi(asList(testClass), emptyList(), emptyList());
    }

    private static PubType getDummyInner1() {
        PubMethod init = new PubMethod(setOf(),
                                       emptyList(),
                                       new PrimitiveTypeDesc(TypeKind.VOID),
                                       "<init>",
                                       emptyList(),
                                       emptyList());

        PubMethod run = new PubMethod(setOf(PUBLIC),
                                      emptyList(),
                                      new PrimitiveTypeDesc(TypeKind.VOID),
                                      "run",
                                      emptyList(),
                                      emptyList());

        PubVar field = new PubVar(setOf(PROTECTED),
                                  new PrimitiveTypeDesc(TypeKind.INT),
                                  "field",
                                  null);

        return new PubType(setOf(STATIC),
                           "TestClass$DummyInner1",
                           new PubApi(emptyList(),
                                      asList(field),
                                      asList(init, run)));
    }

    private static PubType getDummyInner2() {
        PubMethod init = new PubMethod(setOf(),
                                       emptyList(),
                                       new PrimitiveTypeDesc(TypeKind.VOID),
                                       "<init>",
                                       emptyList(),
                                       emptyList());

        return new PubType(setOf(FINAL),
                           "TestClass$DummyInner2",
                           new PubApi(emptyList(),
                                      emptyList(),
                                      asList(init)));
    }

    @SafeVarargs
    private static <T> Set<T> setOf(T... elements) {
        return new HashSet<>(asList(elements));
    }
}
