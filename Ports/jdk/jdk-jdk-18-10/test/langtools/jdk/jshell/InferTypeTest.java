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
 * @bug 8131027
 * @summary Test Type Inference
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 * @build KullaTesting TestingInputStream toolbox.ToolBox Compiler
 * @run testng InferTypeTest
 */

import org.testng.annotations.Test;

@Test
public class InferTypeTest extends KullaTesting {

    public void testTypeInference() {
        assertInferredType("1", "int");
        assertEval("import java.util.*;");
        assertInferredType("new ArrayList<String>()", "ArrayList<String>");
        assertInferredType("null", "Object");
        assertInferredType("1 + ", null); //incomplete
        assertInferredType("undef", null);  //unresolvable
        assertEval("List<String> l1;");
        assertEval("List<? extends String> l2;");
        assertEval("List<? super String> l3;");
        assertInferredType("l1", "List<String>");
        assertInferredType("l2", "List<? extends String>");
        assertInferredType("l3", "List<? super String>");
        assertInferredType("l1.get(0)", "String");
        assertInferredType("l2.get(0)", "String");
        assertInferredType("l3.get(0)", "Object");
        assertInferredType("\"\" + 1", "String");
        assertEval("int i = 0;");
        assertInferredType("i++", "int");
        assertInferredType("++i", "int");
        assertInferredType("i == 0 ? l1.get(0) : l2.get(0)", "String");
        assertInferredType("", null);
        assertInferredType("void test() { }", null);
        assertInferredType("class Test { }", null);
        assertInferredType("enum Test { A; }", null);
        assertInferredType("interface Test { }", null);
        assertInferredType("@interface Test { }", null);
        assertInferredType("Object o;", null);
    }

}
