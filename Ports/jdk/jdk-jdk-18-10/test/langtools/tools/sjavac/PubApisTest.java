/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8241312 8246774
 * @summary test for com.sun.tools.sjavac.comp.PubAPIs and com.sun.tools.sjavac.comp.PubapiVisitor
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.util
 *          jdk.compiler/com.sun.tools.sjavac.comp
 *          jdk.compiler/com.sun.tools.sjavac.pubapi
 */
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.util.Context;
import java.io.IOException;
import com.sun.tools.sjavac.comp.PubAPIs;
import com.sun.tools.sjavac.pubapi.PubApi;
import java.net.URI;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import java.util.List;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class PubApisTest {

    public static void main(String[] args) throws Throwable {
        javax.tools.JavaCompiler c = ToolProvider.getSystemJavaCompiler();
        JavacTask t = (JavacTask) c.getTask(null, null, null, null, null,
                List.of(new SimpleJavaFileObject(URI.create("TestClass.java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
                return String.join("\n",
                        "import java.util.*;",
                        "public final class TestClass {",
                        "    private String s1 = \"str 1\";",
                        "    public String s2 = \"str 2\";",
                        "    protected final String s3 = \"str 3\";",
                        "    static String s4 = \"str 4\";",
                        "    protected TestClass(int i) {}",
                        "    protected void m1() {}",
                        "    public static Map<Integer, List<String>> m2() {",
                        "        return null;",
                        "    }",
                        "    final void m3(Set<Map<Integer, Map<String, String>>> s) {}",
                        "    static class DummyInner1 implements Runnable {",
                        "        protected int field;",
                        "        public void run() {}",
                        "    }",
                        "    final class DummyInner2 { }",
                        "    public record Record3(int f1, String f2) {}",
                        "}");
            }
        }));
        PubAPIs apis = PubAPIs.instance(new Context());
        t.analyze().forEach(apis::visitPubapi);
        PubApi actualApi = (PubApi) apis.getPubapis(emptySet(), false).get(":");
        PubApi expectedApi = new PubApi();
        asList( "TYPE final public TestClass",
                "  VAR public java.lang.String s2",
                "  VAR final protected java.lang.String s3 = \"\\u0073\\u0074\\u0072\\u0020\\u0033\"",
                "  VAR static java.lang.String s4",
                "  METHOD protected void m1()",
                "  METHOD public static java.util.Map m2()",
                "  METHOD final void m3(java.util.Set)",
                "  METHOD protected void <init>(int)",
                "  TYPE static TestClass$DummyInner1",
                "    VAR protected int field",
                "    METHOD public void run()",
                "    METHOD void <init>()",
                "  TYPE final TestClass$DummyInner2",
                "    METHOD void <init>()",
                "  TYPE final public static TestClass$Record3",
                "    METHOD final public boolean equals(java.lang.Object)",
                "    METHOD final public int hashCode()",
                "    METHOD public int f1()",
                "    METHOD public java.lang.String f2()",
                "    METHOD final public java.lang.String toString()",
                "    METHOD public void <init>(int,java.lang.String)"
        ).forEach(expectedApi::appendItem);
        if (!expectedApi.equals(actualApi)) {
            List<String> diffs = expectedApi.diff(actualApi);
            System.out.println(diffs.size() + " differences found.");
            diffs.forEach(System.out::println);
            throw new AssertionError("Actual API differs from expected API.");
        }
    }
}
