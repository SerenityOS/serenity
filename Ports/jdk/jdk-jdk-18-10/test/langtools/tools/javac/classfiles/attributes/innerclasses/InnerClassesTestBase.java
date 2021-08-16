/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.InnerClasses_attribute;
import com.sun.tools.classfile.InnerClasses_attribute.Info;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base class for tests of inner classes attribute.
 * The scenario of tests:
 *   1. set possible values of class modifiers.
 *   2. according to set class modifiers, a test generates sources
 * and golden data with {@code generateTestCases}.
 *   3. a test loops through all test cases and checks InnerClasses
 * attribute with {@code test}.
 *
 * Example, possible flags for outer class are {@code Modifier.PRIVATE and Modifier.PUBLIC},
 * possible flags for inner class are {@code Modifier.EMPTY}.
 * At the second step the test generates two test cases:
 *   1. public class A {
 *        public class B {
 *          class C {}
 *        }
 *      }
 *   2. public class A {
 *        private class B {
 *          class C {}
 *        }
 *      }
 */
public abstract class InnerClassesTestBase extends TestResult {

    private Modifier[] outerAccessModifiers = {Modifier.EMPTY, Modifier.PRIVATE, Modifier.PROTECTED, Modifier.PUBLIC};
    private Modifier[] outerOtherModifiers = {Modifier.EMPTY, Modifier.STATIC, Modifier.FINAL, Modifier.ABSTRACT};
    private Modifier[] innerAccessModifiers = outerAccessModifiers;
    private Modifier[] innerOtherModifiers = outerOtherModifiers;
    private boolean isForbiddenWithoutStaticInOuterMods = false;

    private ClassType outerClassType;
    private ClassType innerClassType;
    private boolean hasSyntheticClass;
    private String prefix = "";
    private String suffix = "";

    /**
     * Sets properties.
     *
     * Returns generated list of test cases. Method is called in {@code test()}.
     */
    public abstract void setProperties();

    /**
     * Runs the test.
     *
     * @param classToTest expected name of outer class
     * @param skipClasses classes that names should not be checked
     */
    public void test(String classToTest, String...skipClasses) throws TestFailedException {
        try {
            String testName = getClass().getName();
            List<TestCase> testCases = generateTestCases();
            for (int i = 0; i < testCases.size(); ++i) {
                TestCase test = testCases.get(i);
                String testCaseName = testName + i + ".java";
                addTestCase(testCaseName);
                writeToFileIfEnabled(Paths.get(testCaseName), test.getSource());
                test(classToTest, test, skipClasses);
            }
        } catch (Exception e) {
            addFailure(e);
        } finally {
            checkStatus();
        }
    }

    /**
     * If {@code flag} is {@code true} an outer class can not have static modifier.
     *
     * @param flag if {@code true} the outer class can not have static modifier
     */
    public void setForbiddenWithoutStaticInOuterMods(boolean flag) {
        isForbiddenWithoutStaticInOuterMods = flag;
    }

    /**
     * Sets the possible access flags of an outer class.
     *
     * @param mods the possible access flags of an outer class
     */
    public void setOuterAccessModifiers(Modifier...mods) {
        outerAccessModifiers = mods;
    }

    /**
     * Sets the possible flags of an outer class.
     *
     * @param mods the possible flags of an outer class
     */
    public void setOuterOtherModifiers(Modifier...mods) {
        outerOtherModifiers = mods;
    }

    /**
     * Sets the possible access flags of an inner class.
     *
     * @param mods the possible access flags of an inner class
     */
    public void setInnerAccessModifiers(Modifier...mods) {
        innerAccessModifiers = mods;
    }

    /**
     * Sets the possible flags of an inner class.
     *
     * @param mods the possible flags of an inner class
     */
    public void setInnerOtherModifiers(Modifier...mods) {
        innerOtherModifiers = mods;
    }

    /**
     * Sets the suffix for the generated source.
     *
     * @param suffix a suffix
     */
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    /**
     * Sets the prefix for the generated source.
     *
     * @param prefix a prefix
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * If {@code true} synthetic class is generated.
     *
     * @param hasSyntheticClass if {@code true} synthetic class is generated
     */
    public void setHasSyntheticClass(boolean hasSyntheticClass) {
        this.hasSyntheticClass = hasSyntheticClass;
    }

    /**
     * Sets the inner class type.
     *
     * @param innerClassType the inner class type
     */
    public void setInnerClassType(ClassType innerClassType) {
        this.innerClassType = innerClassType;
    }

    /**
     * Sets the outer class type.
     *
     * @param outerClassType the outer class type
     */
    public void setOuterClassType(ClassType outerClassType) {
        this.outerClassType = outerClassType;
    }

    private void test(String classToTest, TestCase test, String...skipClasses) {
        printf("Testing :\n%s\n", test.getSource());
        try {
            Map<String, Set<String>> class2Flags = test.getFlags();
            ClassFile cf = readClassFile(compile(getCompileOptions(), test.getSource())
                    .getClasses().get(classToTest));
            InnerClasses_attribute innerClasses = (InnerClasses_attribute)
                    cf.getAttribute(Attribute.InnerClasses);
            int count = 0;
            for (Attribute a : cf.attributes.attrs) {
                if (a instanceof InnerClasses_attribute) {
                    ++count;
                }
            }
            checkEquals(1, count, "Number of inner classes attribute");
            if (!checkNotNull(innerClasses, "InnerClasses attribute should not be null")) {
                return;
            }
            checkEquals(cf.constant_pool.
                    getUTF8Info(innerClasses.attribute_name_index).value, "InnerClasses",
                    "innerClasses.attribute_name_index");
            // Inner Classes attribute consists of length (2 bytes)
            // and 8 bytes for each inner class's entry.
            checkEquals(innerClasses.attribute_length,
                    2 + 8 * class2Flags.size(), "innerClasses.attribute_length");
            checkEquals(innerClasses.number_of_classes,
                    class2Flags.size(), "innerClasses.number_of_classes");
            Set<String> visitedClasses = new HashSet<>();
            for (Info e : innerClasses.classes) {
                String baseName = cf.constant_pool.getClassInfo(
                        e.inner_class_info_index).getBaseName();
                if (cf.major_version >= 51 && e.inner_name_index == 0) {
                    checkEquals(e.outer_class_info_index, 0,
                            "outer_class_info_index "
                                    + "in case of inner_name_index is zero : "
                                    + baseName);
                }
                String className = baseName.replaceFirst(".*\\$", "");
                checkTrue(class2Flags.containsKey(className),
                        className);
                checkTrue(visitedClasses.add(className),
                        "there are no duplicates in attribute : " + className);
                checkEquals(e.inner_class_access_flags.getInnerClassFlags(),
                        class2Flags.get(className),
                        "inner_class_access_flags " + className);
                if (!Arrays.asList(skipClasses).contains(className)) {
                        checkEquals(
                                cf.constant_pool.getClassInfo(e.inner_class_info_index).getBaseName(),
                                classToTest + "$" + className,
                                "inner_class_info_index of " + className);
                    if (e.outer_class_info_index > 0) {
                        checkEquals(
                                cf.constant_pool.getClassInfo(e.outer_class_info_index).getName(),
                                classToTest,
                                "outer_class_info_index of " + className);
                    }
                }
            }
        } catch (Exception e) {
            addFailure(e);
        }
    }

    /**
     * Methods generates list of test cases. Method generates all possible combinations
     * of acceptable flags for nested inner classes.
     *
     * @return generated list of test cases
     */
    protected List<TestCase> generateTestCases() {
        setProperties();
        List<TestCase> list = new ArrayList<>();

        List<List<Modifier>> outerMods = getAllCombinations(outerAccessModifiers, outerOtherModifiers);
        List<List<Modifier>> innerMods = getAllCombinations(innerAccessModifiers, innerOtherModifiers);

        for (List<Modifier> outerMod : outerMods) {
            if (isForbiddenWithoutStaticInOuterMods && !outerMod.contains(Modifier.STATIC)) {
                continue;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("public class InnerClassesSrc {")
                    .append(toString(outerMod)).append(' ')
                    .append(outerClassType).append(' ')
                    .append(prefix).append(' ').append('\n');
            int count = 0;
            Map<String, Set<String>> class2Flags = new HashMap<>();
            List<String> syntheticClasses = new ArrayList<>();
            for (List<Modifier> innerMod : innerMods) {
                ++count;
                String privateConstructor = "";
                if (hasSyntheticClass && !innerMod.contains(Modifier.ABSTRACT)) {
                    privateConstructor = "private A" + count + "() {}";
                    syntheticClasses.add("new A" + count + "();");
                }
                sb.append(toString(innerMod)).append(' ');
                sb.append(String.format("%s A%d {%s}\n", innerClassType, count, privateConstructor));
                Set<String> flags = getFlags(innerClassType, innerMod);
                class2Flags.put("A" + count, flags);
            }
            if (hasSyntheticClass) {
                // Source to generate synthetic classes
                sb.append(syntheticClasses.stream().collect(Collectors.joining(" ", "{", "}")));
                class2Flags.put("1", new HashSet<>(Arrays.asList("ACC_STATIC", "ACC_SYNTHETIC")));
            }
            sb.append(suffix).append("\n}");
            getAdditionalFlags(class2Flags, outerClassType, outerMod.toArray(new Modifier[outerMod.size()]));
            list.add(new TestCase(sb.toString(), class2Flags));
        }
        return list;
    }

    /**
     * Methods returns flags which must have type.
     *
     * @param type class, interface, enum or annotation
     * @param mods modifiers
     * @return set of access flags
     */
    protected Set<String> getFlags(ClassType type, List<Modifier> mods) {
        Set<String> flags = mods.stream()
                .map(Modifier::getString)
                .filter(str -> !str.isEmpty())
                .map(str -> "ACC_" + str.toUpperCase())
                .collect(Collectors.toSet());
        type.addSpecificFlags(flags);
        return flags;
    }

    protected List<String> getCompileOptions() {
        return Collections.emptyList();
    }

    private List<List<Modifier>> getAllCombinations(Modifier[] accessModifiers, Modifier[] otherModifiers) {
        List<List<Modifier>> list = new ArrayList<>();
        for (Modifier access : accessModifiers) {
            for (int i = 0; i < otherModifiers.length; ++i) {
                Modifier mod1 = otherModifiers[i];
                for (int j = i + 1; j < otherModifiers.length; ++j) {
                    Modifier mod2 = otherModifiers[j];
                    if (isForbidden(mod1, mod2)) {
                        continue;
                    }
                    list.add(Arrays.asList(access, mod1, mod2));
                }
                if (mod1 == Modifier.EMPTY) {
                    list.add(Collections.singletonList(access));
                }
            }
        }
        return list;
    }

    private boolean isForbidden(Modifier mod1, Modifier mod2) {
        return mod1 == Modifier.FINAL && mod2 == Modifier.ABSTRACT
                || mod1 == Modifier.ABSTRACT && mod2 == Modifier.FINAL;
    }

    private String toString(List<Modifier> mods) {
        return mods.stream()
                .map(Modifier::getString)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" "));
    }

    /**
     * Method is called in generateTestCases().
     * If you need to add additional access flags, you should override this method.
     *
     *
     * @param class2Flags map with flags
     * @param type class, interface, enum or @annotation
     * @param mods modifiers
     */
    public void getAdditionalFlags(Map<String, Set<String>> class2Flags, ClassType type, Modifier...mods) {
        class2Flags.values().forEach(type::addFlags);
    }

    public enum ClassType {
        CLASS("class") {
            @Override
            public void addSpecificFlags(Set<String> flags) {
            }
        },
        INTERFACE("interface") {
            @Override
            public void addFlags(Set<String> flags) {
                flags.add("ACC_STATIC");
                flags.add("ACC_PUBLIC");
            }

            @Override
            public void addSpecificFlags(Set<String> flags) {
                flags.add("ACC_INTERFACE");
                flags.add("ACC_ABSTRACT");
                flags.add("ACC_STATIC");
            }
        },
        ANNOTATION("@interface") {
            @Override
            public void addFlags(Set<String> flags) {
                flags.add("ACC_STATIC");
                flags.add("ACC_PUBLIC");
            }

            @Override
            public void addSpecificFlags(Set<String> flags) {
                flags.add("ACC_INTERFACE");
                flags.add("ACC_ABSTRACT");
                flags.add("ACC_STATIC");
                flags.add("ACC_ANNOTATION");
            }
        },
        ENUM("enum") {
            @Override
            public void addSpecificFlags(Set<String> flags) {
                flags.add("ACC_ENUM");
                flags.add("ACC_FINAL");
                flags.add("ACC_STATIC");
            }
        },
        OTHER("") {
            @Override
            public void addSpecificFlags(Set<String> flags) {
            }
        };

        private final String classType;

        ClassType(String clazz) {
            this.classType = clazz;
        }

        public abstract void addSpecificFlags(Set<String> flags);

        public String toString() {
            return classType;
        }

        public void addFlags(Set<String> set) {
        }
    }

    public enum Modifier {
        PUBLIC("public"), PRIVATE("private"),
        PROTECTED("protected"), DEFAULT("default"),
        FINAL("final"), ABSTRACT("abstract"),
        STATIC("static"), EMPTY("");

        private final String str;

        Modifier(String str) {
            this.str = str;
        }

        public String getString() {
            return str;
        }
    }
}
