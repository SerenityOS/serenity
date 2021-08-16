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
 * @bug 8042251
 * @summary Testing InnerClasses_attribute of inner classes in local class.
 * @library /tools/lib /tools/javac/lib ../lib
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox InMemoryFileManager TestResult TestBase
 * @build InnerClassesTestBase
 * @run main InnerClassesInLocalClassTest
 */

import java.util.*;

public class InnerClassesInLocalClassTest extends InnerClassesTestBase {

    private final static Modifier[] LOCAL_CLASS_MODIFIERS =
            new Modifier[]{Modifier.EMPTY, Modifier.ABSTRACT, Modifier.FINAL};
    private final static String CLASS_TEMPLATE =
            "public %CLASS% OuterClass {\n" +
            "%SOURCE%\n" +
            "}";

    private final List<Data> innerClassesData;

    public InnerClassesInLocalClassTest() {
        innerClassesData = new ArrayList<>();
        for (Modifier outerModifier : LOCAL_CLASS_MODIFIERS) {
            StringBuilder sb = new StringBuilder();
            sb.append(outerModifier.getString()).append(' ');
            sb.append("class Local {");
            Map<String, Set<String>> class2Flags = new HashMap<>();
            for (int i = 0; i < LOCAL_CLASS_MODIFIERS.length; ++i) {
                Modifier innerModifier = LOCAL_CLASS_MODIFIERS[i];
                sb.append(innerModifier.getString()).append(' ')
                        .append("class").append(' ')
                        .append('A').append(i).append("{}\n");
                class2Flags.put("A" + i, getFlags(innerModifier));
            }
            sb.append("};");
            class2Flags.put("1Local", getFlags(outerModifier));
            innerClassesData.add(new Data(sb.toString(), class2Flags));
        }
    }

    public static void main(String[] args) throws TestFailedException {
        InnerClassesTestBase test = new InnerClassesInLocalClassTest();
        test.test("OuterClass$1Local", "1Local");
    }

    @Override
    public void setProperties() {
    }

    @Override
    public List<TestCase> generateTestCases() {
        List<TestCase> testCases = new ArrayList<>();
        testCases.addAll(localClassInClassMethod());
        testCases.addAll(localClassInInterfaceMethod());
        return testCases;
    }

    private List<TestCase> localClassInClassMethod() {
        List<TestCase> list = new ArrayList<>();
        String template = CLASS_TEMPLATE.replace("%CLASS%", "class");
        list.addAll(lambda(template));
        list.addAll(constructor(template));
        list.addAll(method(template,
                new Modifier[]{Modifier.EMPTY, Modifier.PRIVATE, Modifier.PROTECTED, Modifier.PUBLIC},
                new Modifier[]{Modifier.EMPTY, Modifier.FINAL, Modifier.STATIC}));
        list.addAll(staticAndInstanceInitializer(template));
        return list;
    }

    private List<TestCase> localClassInInterfaceMethod() {
        String template = CLASS_TEMPLATE.replace("%CLASS%", "interface");
        return method(template,
                new Modifier[]{Modifier.EMPTY, Modifier.PUBLIC},
                new Modifier[]{Modifier.DEFAULT, Modifier.STATIC});
    }

    private List<TestCase> generate(String template, String prefix, String suffix) {
        List<TestCase> list = new ArrayList<>();
        for (Data data : innerClassesData) {
            list.add(new TestCase(template.replace("%SOURCE%",
                    prefix + data.sources + suffix),
                    data.class2Flags));
        }
        return list;
    }

    private List<TestCase> lambda(String template) {
        return generate(template, "Runnable run = () -> {", "};");
    }

    private List<TestCase> constructor(String template) {
        List<TestCase> list = new ArrayList<>();
        for (Modifier modifier :
                new Modifier[]{Modifier.EMPTY, Modifier.PRIVATE, Modifier.PROTECTED, Modifier.PUBLIC}) {
            list.addAll(generate(template, modifier.getString() + " OuterClass() {", "}"));
        }
        return list;
    }

    private List<TestCase> method(String template, Modifier[] mods, Modifier[] otherMods) {
        List<TestCase> list = new ArrayList<>();
        for (Modifier modifier : mods) {
            for (Modifier otherMod : otherMods) {
                list.addAll(generate(template,
                        String.format("%s %s void method() {",
                                modifier.getString(),
                                otherMod.getString()),
                        "}"));
            }
        }
        return list;
    }

    private List<TestCase> staticAndInstanceInitializer(String template) {
        List<TestCase> list = new ArrayList<>();
        for (Modifier modifier : new Modifier[]{Modifier.EMPTY, Modifier.STATIC}) {
            list.addAll(generate(template, modifier.getString() + "{", "}"));
        }
        return list;
    }

    private Set<String> getFlags(Modifier modifier) {
        HashSet<String> set = new HashSet<>();
        if (modifier != Modifier.EMPTY) {
            set.add("ACC_" + modifier.getString().toUpperCase());
        }
        return set;
    }

    /**
     * Class represents part of sources which is inserted in other code.
     */
    private static class Data {
        public final String sources;
        public final Map<String, Set<String>> class2Flags;

        public Data(String sources, Map<String, Set<String>> class2Flags) {
            this.sources = sources;
            this.class2Flags = class2Flags;
        }
    }
}
