/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8042251 8062373
 * @summary Testing InnerClasses_attribute of inner classes in anonymous class.
 * @library /tools/lib /tools/javac/lib ../lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox InMemoryFileManager TestResult TestBase
 * @build InnerClassesTestBase
 * @run main InnerClassesInAnonymousClassTest
 */

import java.util.*;

public class InnerClassesInAnonymousClassTest extends InnerClassesTestBase {

    private ClassType currentClassType;

    public static void main(String[] args) throws TestFailedException {
        InnerClassesTestBase test = new InnerClassesInAnonymousClassTest();
        test.test("InnerClassesSrc$1", "Anonymous", "1");
    }

    @Override
    public void setProperties() {
        setOuterOtherModifiers(Modifier.EMPTY, Modifier.ABSTRACT, Modifier.STATIC);
        setInnerAccessModifiers(Modifier.EMPTY);
        setInnerOtherModifiers(Modifier.EMPTY, Modifier.ABSTRACT, Modifier.FINAL);
        setOuterClassType(ClassType.OTHER);
        setInnerClassType(ClassType.CLASS);
        setSuffix("};}");
    }

    @Override
    public List<TestCase> generateTestCases() {
        currentClassType = ClassType.CLASS;
        setPrefix("class Anonymous {} {new Anonymous() {");
        List<TestCase> sources = super.generateTestCases();

        currentClassType = ClassType.INTERFACE;
        setPrefix("interface Anonymous {} {new Anonymous() {");
        sources.addAll(super.generateTestCases());

        currentClassType = ClassType.ANNOTATION;
        setPrefix("@interface Anonymous {} {new Anonymous() {@Override public "
                + "Class<? extends java.lang.annotation.Annotation> "
                + "annotationType() {return null;}");
        sources.addAll(super.generateTestCases());
        return sources;
    }

    @Override
    public void getAdditionalFlags(Map<String, Set<String>> class2Flags, ClassType type, Modifier... flags) {
        super.getAdditionalFlags(class2Flags, type, flags);
        class2Flags.put("Anonymous", getFlags(currentClassType, Arrays.asList(flags)));
        class2Flags.put("1", new HashSet<>() {});
    }
}
