/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.RetentionPolicy;
import java.util.*;
import java.util.stream.Collectors;

public class TestCase {

    /**
     * The top-level classes of the test case.
     */
    public final Map<String, TestClassInfo> classes = new LinkedHashMap<>();

    /**
     * Constructs a test class info with {@code classType} as top-level class,
     * with {@code outerClassName} as name and {@code mods} as modifiers.
     *
     * @param classType a class type
     * @param outerClassName a name
     * @param mods an array of modifiers
     */
    public TestClassInfo addClassInfo(ClassType classType, String outerClassName, String...mods) {
        return addClassInfo(null, classType, outerClassName, mods);
    }

    /**
     * Constructs a test class info with {@code classType} as top-level class,
     * with {@code outerClassName} as name, {@code parent} class name
     * as parent class and {@code mods} as modifiers.
     *
     * @param classType a class type
     * @param outerClassName a name
     * @param mods an array of modifiers
     */
    public TestClassInfo addClassInfo(String parent, ClassType classType, String outerClassName, String...mods) {
        TestClassInfo clazz = new TestClassInfo(classType, outerClassName, parent, mods);
        if (classes.put(outerClassName, clazz) != null) {
            throw new IllegalArgumentException("Duplicate class name: " + outerClassName);
        }
        return clazz;
    }

    public String generateSource() {
        return classes.values().stream()
                .map(TestMemberInfo::generateSource)
                .collect(Collectors.joining("\n"));
    }

    /**
     * Returns {@code TestClassInfo} by class signature.
     * Example, {@code getTestClassInfo(&quot;Test$1Local&quot;)}
     * returns local inner class of class {@code Test}.
     *
     * @param classSignature a class signature
     * @return {@code TestClassInfo} by class signature
     */
    public TestClassInfo getTestClassInfo(String classSignature) {
        String[] cs = classSignature.split("\\$");
        if (cs.length > 0 && classes.containsKey(cs[0])) {
            // check signature corresponds to top level class
            if (cs.length == 1) {
                return classes.get(cs[0]);
            }
        } else {
            throw new IllegalArgumentException("Cannot find class : " + classSignature);
        }
        TestClassInfo current = classes.get(cs[0]);
        // find class info in the inner classes
        for (int i = 1; i < cs.length; ++i) {
            Map<String, TestClassInfo> innerClasses = current.innerClasses;
            Map<String, TestMethodInfo> methods = current.methods;
            current = innerClasses.get(cs[i]);
            // if current is null then class info does not exist or the class is local
            if (current == null) {
                if (!cs[i].isEmpty()) {
                    // the class is local, remove leading digit
                    String className = cs[i].substring(1);
                    Optional<TestClassInfo> opt = methods.values().stream()
                            .flatMap(c -> c.localClasses.values().stream())
                            .filter(c -> c.name.equals(className)).findAny();
                    if (opt.isPresent()) {
                        current = opt.get();
                        // continue analysis of local class
                        continue;
                    }
                }
                throw new IllegalArgumentException("Cannot find class : " + classSignature);
            }
        }
        return current;
    }

    /**
     * Class represents a program member.
     */
    public static abstract class TestMemberInfo {
        // next two fields are used for formatting
        protected final int indention;
        protected final ClassType containerType;
        public final List<String> mods;
        public final String name;
        public final Map<String, TestAnnotationInfo> annotations;

        TestMemberInfo(int indention, ClassType containerType, String name, String... mods) {
            this.indention = indention;
            this.containerType = containerType;
            this.mods = Arrays.asList(mods);
            this.name = name;
            this.annotations = new HashMap<>();
        }

        public abstract String generateSource();

        public boolean isAnnotated(RetentionPolicy policy) {
            return annotations.values().stream()
                    .filter(a -> a.policy == policy)
                    .findAny().isPresent();
        }

        public Set<String> getRuntimeVisibleAnnotations() {
            return getRuntimeAnnotations(RetentionPolicy.RUNTIME);
        }

        public Set<String> getRuntimeInvisibleAnnotations() {
            return getRuntimeAnnotations(RetentionPolicy.CLASS);
        }

        private Set<String> getRuntimeAnnotations(RetentionPolicy policy) {
            return annotations.values().stream()
                    .filter(e -> e.policy == policy)
                    .map(a -> a.annotationName)
                    .distinct()
                    .collect(Collectors.toSet());
        }

        /**
         * Generates source for annotations.
         *
         * @param prefix a leading text
         * @param suffix a trailing text
         * @param joining a text between annotations
         * @return source for annotations
         */
        protected String generateSourceForAnnotations(String prefix, String suffix, String joining) {
            StringBuilder sb = new StringBuilder();
            for (TestAnnotationInfo annotation : annotations.values()) {
                sb.append(prefix);
                if (annotation.isContainer) {
                    // the annotation is repeatable
                    // container consists of an array of annotations
                    TestAnnotationInfo.TestArrayElementValue containerElementValue =
                            (TestAnnotationInfo.TestArrayElementValue) annotation.elementValues.get(0).elementValue;
                    // concatenate sources of repeatable annotations
                    sb.append(containerElementValue.values.stream()
                            .map(TestAnnotationInfo.TestElementValue::toString)
                            .collect(Collectors.joining(joining)));
                } else {
                    sb.append(annotation);
                }
                sb.append(suffix);
            }
            String src = sb.toString();
            return src.trim().isEmpty() ? "" : src;

        }

        /**
         * Generates source for annotations.
         *
         * @return source for annotations
         */
        public String generateSourceForAnnotations() {
            return generateSourceForAnnotations(indention(), "\n", "\n" + indention());
        }

        /**
         * Adds annotation info to the member.
         *
         * @param anno an annotation info
         */
        public void addAnnotation(TestAnnotationInfo anno) {
            String containerName = anno.annotationName + "Container";
            TestAnnotationInfo annotation = annotations.get(anno.annotationName);
            TestAnnotationInfo containerAnnotation = annotations.get(containerName);

            if (annotation == null) {
                // if annotation is null then either it is first adding of the annotation to the member
                // or there is the container of the annotation.
                if (containerAnnotation == null) {
                    // first adding to the member
                    annotations.put(anno.annotationName, anno);
                } else {
                    // add annotation to container
                    TestAnnotationInfo.TestArrayElementValue containerElementValue =
                            ((TestAnnotationInfo.TestArrayElementValue) containerAnnotation.elementValues.get(0).elementValue);
                    containerElementValue.values.add(new TestAnnotationInfo.TestAnnotationElementValue(anno.annotationName, anno));
                }
            } else {
                // remove previously added annotation and add new container of repeatable annotation
                // which contains previously added and new annotation
                annotations.remove(anno.annotationName);
                containerAnnotation = new TestAnnotationInfo(
                        containerName,
                        anno.policy,
                        true,
                        new TestAnnotationInfo.Pair("value",
                                new TestAnnotationInfo.TestArrayElementValue(
                                        new TestAnnotationInfo.TestAnnotationElementValue(anno.annotationName, annotation),
                                        new TestAnnotationInfo.TestAnnotationElementValue(anno.annotationName, anno))));
                annotations.put(containerName, containerAnnotation);
            }
        }

        public String indention() {
            char[] a = new char[4 * indention];
            Arrays.fill(a, ' ');
            return new String(a);
        }

        public String getName() {
            return name;
        }
    }

    /**
     * The class represents a class.
     */
    public static class TestClassInfo extends TestMemberInfo {
        public final ClassType classType;
        public final String parent;
        public final Map<String, TestClassInfo> innerClasses;
        public final Map<String, TestMethodInfo> methods;
        public final Map<String, TestFieldInfo> fields;

        TestClassInfo(int indention, ClassType classType, String className, String... mods) {
            this(indention, classType, className, null, mods);
        }

        TestClassInfo(ClassType classType, String className, String parent, String... mods) {
            this(0, classType, className, parent, mods);
        }

        TestClassInfo(int indention, ClassType classType, String className, String parent, String... mods) {
            super(indention, null, className, mods);
            this.classType = classType;
            this.parent = parent;
            innerClasses = new LinkedHashMap<>();
            methods = new LinkedHashMap<>();
            fields = new LinkedHashMap<>();
        }

        /**
         * Generates source which represents the class.
         *
         * @return source which represents the class
         */
        @Override
        public String generateSource() {
            String sourceForAnnotations = generateSourceForAnnotations();
            String classModifiers = mods.stream().collect(Collectors.joining(" "));
            return sourceForAnnotations
                    + String.format("%s%s %s %s %s {%n",
                    indention(),
                    classModifiers,
                    classType.getDescription(),
                    name,
                    parent == null ? "" : "extends " + parent)
                    + classType.collectFields(fields.values())
                    + classType.collectMethods(methods.values())
                    + classType.collectInnerClasses(innerClasses.values())
                    + indention() + "}";
        }

        /**
         * Adds a new inner class to the class.
         *
         * @param classType a class type
         * @param className a class name
         * @param mods modifiers
         * @return a new added inner class to the class
         */
        public TestClassInfo addInnerClassInfo(ClassType classType, String className, String... mods) {
            TestClassInfo testClass = new TestClassInfo(indention + 1, classType, className, mods);
            if (innerClasses.put(className, testClass) != null) {
                throw new IllegalArgumentException("Duplicated class : " + className);
            }
            return testClass;
        }

        /**
         * Adds a new method to the class.
         *
         * @param methodName a method name
         * @param mods modifiers
         * @return a new inner class to the class
         */
        public TestMethodInfo addMethodInfo(String methodName, String... mods) {
            return addMethodInfo(methodName, false, mods);
        }

        /**
         * Adds a new method to the class.
         *
         * @param methodName a method name
         * @param isSynthetic if {@code true} the method is synthetic
         * @param mods modifiers
         * @return a new method added to the class
         */
        public TestMethodInfo addMethodInfo(String methodName, boolean isSynthetic, String... mods) {
            boolean isConstructor = methodName.contains("<init>");
            if (isConstructor) {
                methodName = methodName.replace("<init>", name);
            }
            TestMethodInfo testMethod = new TestMethodInfo(indention + 1, classType, methodName, isConstructor, isSynthetic, mods);
            if (methods.put(methodName, testMethod) != null) {
                throw new IllegalArgumentException("Duplicated method : " + methodName);
            }
            return testMethod;
        }

        /**
         * Adds a new field to the class.
         *
         * @param fieldName a method name
         * @param mods modifiers
         * @return a new field added to the class
         */
        public TestFieldInfo addFieldInfo(String fieldName, String... mods) {
            TestFieldInfo field = new TestFieldInfo(indention + 1, classType, fieldName, mods);
            if (fields.put(fieldName, field) != null) {
                throw new IllegalArgumentException("Duplicated field : " + fieldName);
            }
            return field;
        }

        public TestMethodInfo getTestMethodInfo(String methodName) {
            return methods.get(methodName);
        }

        public TestFieldInfo getTestFieldInfo(String fieldName) {
            return fields.get(fieldName);
        }
    }

    public static class TestMethodInfo extends TestMemberInfo {
        public final boolean isConstructor;
        public final boolean isSynthetic;
        public final Map<String, TestClassInfo> localClasses;
        public final List<TestParameterInfo> parameters;

        TestMethodInfo(int indention, ClassType containerType, String methodName,
                               boolean isConstructor, boolean isSynthetic, String... mods) {
            super(indention, containerType, methodName, mods);
            this.isSynthetic = isSynthetic;
            this.localClasses = new LinkedHashMap<>();
            this.parameters = new ArrayList<>();
            this.isConstructor = isConstructor;
        }

        public boolean isParameterAnnotated(RetentionPolicy policy) {
            return parameters.stream()
                    .filter(p -> p.isAnnotated(policy))
                    .findFirst().isPresent();
        }

        public TestParameterInfo addParameter(String type, String name) {
            TestParameterInfo testParameter = new TestParameterInfo(type, name);
            parameters.add(testParameter);
            return testParameter;
        }

        /**
         * Adds a local class to the method.
         *
         * @param className a class name
         * @param mods modifiers
         * @return a local class added to the method
         */
        public TestClassInfo addLocalClassInfo(String className, String... mods) {
            TestClassInfo testClass = new TestClassInfo(indention + 1, ClassType.CLASS, className, mods);
            if (localClasses.put(className, testClass) != null) {
                throw new IllegalArgumentException("Duplicated class : " + className);
            }
            return testClass;
        }

        @Override
        public String generateSource() {
            if (isSynthetic) {
                return "";
            }
            return generateSourceForAnnotations() +
                    containerType.methodToString(this);
        }

        @Override
        public String getName() {
            return name.replaceAll("\\(.*\\)", "");
        }
    }

    /**
     * The class represents a method parameter.
     */
    public static class TestParameterInfo extends TestMemberInfo {
        public final String type;

        TestParameterInfo(String type, String name) {
            super(0, null, name);
            this.type = type;
        }

        @Override
        public String generateSource() {
            return generateSourceForAnnotations() + type + " " + name;
        }

        public String generateSourceForAnnotations() {
            return generateSourceForAnnotations("", " ", " ");
        }
    }

    /**
     * The class represents a field.
     */
    public static class TestFieldInfo extends TestMemberInfo {

        TestFieldInfo(int indention, ClassType containerType, String fieldName, String... mods) {
            super(indention, containerType, fieldName, mods);
        }

        @Override
        public String generateSource() {
            return generateSourceForAnnotations() +
                    containerType.fieldToString(this);
        }
    }
}
