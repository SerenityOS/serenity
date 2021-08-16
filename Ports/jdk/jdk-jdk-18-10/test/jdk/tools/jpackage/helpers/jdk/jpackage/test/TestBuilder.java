/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jpackage.test;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jdk.jpackage.test.Annotations.AfterEach;
import jdk.jpackage.test.Annotations.BeforeEach;
import jdk.jpackage.test.Annotations.Parameter;
import jdk.jpackage.test.Annotations.ParameterGroup;
import jdk.jpackage.test.Annotations.Parameters;
import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.Functional.ThrowingConsumer;
import jdk.jpackage.test.Functional.ThrowingFunction;

final class TestBuilder implements AutoCloseable {

    @Override
    public void close() throws Exception {
        flushTestGroup();
    }

    TestBuilder(Consumer<TestInstance> testConsumer) {
        argProcessors = Map.of(
                CMDLINE_ARG_PREFIX + "after-run",
                arg -> getJavaMethodsFromArg(arg).map(
                        this::wrap).forEachOrdered(afterActions::add),

                CMDLINE_ARG_PREFIX + "before-run",
                arg -> getJavaMethodsFromArg(arg).map(
                        this::wrap).forEachOrdered(beforeActions::add),

                CMDLINE_ARG_PREFIX + "run",
                arg -> addTestGroup(getJavaMethodsFromArg(arg).map(
                        ThrowingFunction.toFunction(
                                TestBuilder::toMethodCalls)).flatMap(s -> s).collect(
                        Collectors.toList())),

                CMDLINE_ARG_PREFIX + "exclude",
                arg -> (excludedTests = Optional.ofNullable(
                        excludedTests).orElseGet(() -> new HashSet<String>())).add(arg),

                CMDLINE_ARG_PREFIX + "include",
                arg -> (includedTests = Optional.ofNullable(
                        includedTests).orElseGet(() -> new HashSet<String>())).add(arg),

                CMDLINE_ARG_PREFIX + "space-subst",
                arg -> spaceSubstitute = arg,

                CMDLINE_ARG_PREFIX + "group",
                arg -> flushTestGroup(),

                CMDLINE_ARG_PREFIX + "dry-run",
                arg -> dryRun = true
        );
        this.testConsumer = testConsumer;
        clear();
    }

    void processCmdLineArg(String arg) throws Throwable {
        int separatorIdx = arg.indexOf('=');
        final String argName;
        final String argValue;
        if (separatorIdx != -1) {
            argName = arg.substring(0, separatorIdx);
            argValue = arg.substring(separatorIdx + 1);
        } else {
            argName = arg;
            argValue = null;
        }
        try {
            ThrowingConsumer<String> argProcessor = argProcessors.get(argName);
            if (argProcessor == null) {
                throw new ParseException("Unrecognized");
            }
            argProcessor.accept(argValue);
        } catch (ParseException ex) {
            ex.setContext(arg);
            throw ex;
        }
    }

    private void addTestGroup(List<MethodCall> newTestGroup) {
        if (testGroup != null) {
            testGroup.addAll(newTestGroup);
        } else {
            testGroup = newTestGroup;
        }
    }

    private static Stream<MethodCall> filterTests(Stream<MethodCall> tests,
            Set<String> filters, UnaryOperator<Boolean> pred, String logMsg) {
        if (filters == null) {
            return tests;
        }

        // Log all matches before returning from the function
        return tests.filter(test -> {
            String testDescription = test.createDescription().testFullName();
            boolean match = filters.stream().anyMatch(testDescription::contains);
            if (match) {
                trace(String.format(logMsg + ": %s", testDescription));
            }
            return pred.apply(match);
        }).collect(Collectors.toList()).stream();
    }

    private Stream<MethodCall> filterTestGroup() {
        Objects.requireNonNull(testGroup);

        UnaryOperator<Set<String>> restoreSpaces = filters -> {
            if (spaceSubstitute == null || filters == null) {
                return filters;
            }
            return filters.stream().map(
                    filter -> filter.replace(spaceSubstitute, " ")).collect(
                            Collectors.toSet());
        };

        if (includedTests != null) {
            return filterTests(testGroup.stream(), restoreSpaces.apply(
                    includedTests), x -> x, "Include");
        }

        return filterTests(testGroup.stream(),
                restoreSpaces.apply(excludedTests), x -> !x, "Exclude");
    }

    private void flushTestGroup() {
        if (testGroup != null) {
            filterTestGroup().forEach(this::createTestInstance);
            clear();
        }
    }

    private void createTestInstance(MethodCall testBody) {
        final List<ThrowingConsumer> curBeforeActions;
        final List<ThrowingConsumer> curAfterActions;

        Method testMethod = testBody.getMethod();
        if (Stream.of(BeforeEach.class, AfterEach.class).anyMatch(
                testMethod::isAnnotationPresent)) {
            curBeforeActions = beforeActions;
            curAfterActions = afterActions;
        } else {
            curBeforeActions = new ArrayList<>(beforeActions);
            curAfterActions = new ArrayList<>(afterActions);

            selectFrameMethods(testMethod.getDeclaringClass(), BeforeEach.class).map(
                    this::wrap).forEachOrdered(curBeforeActions::add);
            selectFrameMethods(testMethod.getDeclaringClass(), AfterEach.class).map(
                    this::wrap).forEachOrdered(curAfterActions::add);
        }

        TestInstance test = new TestInstance(testBody, curBeforeActions,
                curAfterActions, dryRun);
        if (includedTests == null) {
            trace(String.format("Create: %s", test.fullName()));
        }
        testConsumer.accept(test);
    }

    private void clear() {
        beforeActions = new ArrayList<>();
        afterActions = new ArrayList<>();
        excludedTests = null;
        includedTests = null;
        spaceSubstitute = null;
        testGroup = null;
    }

    private static Class probeClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    private static Stream<Method> selectFrameMethods(Class type, Class annotationType) {
        return Stream.of(type.getMethods())
                .filter(m -> m.getParameterCount() == 0)
                .filter(m -> !m.isAnnotationPresent(Test.class))
                .filter(m -> m.isAnnotationPresent(annotationType))
                .sorted((a, b) -> a.getName().compareTo(b.getName()));
    }

    private static Stream<String> cmdLineArgValueToMethodNames(String v) {
        List<String> result = new ArrayList<>();
        String defaultClassName = null;
        for (String token : v.split(",")) {
            Class testSet = probeClass(token);
            if (testSet != null) {
                // Test set class specified. Pull in all public methods
                // from the class with @Test annotation removing name duplicates.
                // Overloads will be handled at the next phase of processing.
                defaultClassName = token;
                Stream.of(testSet.getMethods()).filter(
                        m -> m.isAnnotationPresent(Test.class)).map(
                                Method::getName).distinct().forEach(
                                name -> result.add(String.join(".", token, name)));

                continue;
            }

            final String qualifiedMethodName;
            final int lastDotIdx = token.lastIndexOf('.');
            if (lastDotIdx != -1) {
                qualifiedMethodName = token;
                defaultClassName = token.substring(0, lastDotIdx);
            } else if (defaultClassName == null) {
                throw new ParseException("Default class name not found in");
            } else {
                qualifiedMethodName = String.join(".", defaultClassName, token);
            }
            result.add(qualifiedMethodName);
        }
        return result.stream();
    }

    private static boolean filterMethod(String expectedMethodName, Method method) {
        if (!method.getName().equals(expectedMethodName)) {
            return false;
        }
        switch (method.getParameterCount()) {
            case 0:
                return !isParametrized(method);
            case 1:
                return isParametrized(method);
        }
        return false;
    }

    private static boolean isParametrized(Method method) {
        return method.isAnnotationPresent(ParameterGroup.class) || method.isAnnotationPresent(
                Parameter.class);
    }

    private static List<Method> getJavaMethodFromString(
            String qualifiedMethodName) {
        int lastDotIdx = qualifiedMethodName.lastIndexOf('.');
        if (lastDotIdx == -1) {
            throw new ParseException("Class name not found in");
        }
        String className = qualifiedMethodName.substring(0, lastDotIdx);
        String methodName = qualifiedMethodName.substring(lastDotIdx + 1);
        Class methodClass;
        try {
            methodClass = Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new ParseException(String.format("Class [%s] not found;",
                    className));
        }
        // Get the list of all public methods as need to deal with overloads.
        List<Method> methods = Stream.of(methodClass.getMethods()).filter(
                (m) -> filterMethod(methodName, m)).collect(Collectors.toList());
        if (methods.isEmpty()) {
            throw new ParseException(String.format(
                    "Method [%s] not found in [%s] class;",
                    methodName, className));
        }

        trace(String.format("%s -> %s", qualifiedMethodName, methods));
        return methods;
    }

    private static Stream<Method> getJavaMethodsFromArg(String argValue) {
        return cmdLineArgValueToMethodNames(argValue).map(
                ThrowingFunction.toFunction(
                        TestBuilder::getJavaMethodFromString)).flatMap(
                        List::stream).sequential();
    }

    private static Parameter[] getMethodParameters(Method method) {
        if (method.isAnnotationPresent(ParameterGroup.class)) {
            return ((ParameterGroup) method.getAnnotation(ParameterGroup.class)).value();
        }

        if (method.isAnnotationPresent(Parameter.class)) {
            return new Parameter[]{(Parameter) method.getAnnotation(
                Parameter.class)};
        }

        // Unexpected
        return null;
    }

    private static Stream<Object[]> toCtorArgs(Method method) throws
            IllegalAccessException, InvocationTargetException {
        Class type = method.getDeclaringClass();
        List<Method> paremetersProviders = Stream.of(type.getMethods())
                .filter(m -> m.getParameterCount() == 0)
                .filter(m -> (m.getModifiers() & Modifier.STATIC) != 0)
                .filter(m -> m.isAnnotationPresent(Parameters.class))
                .sorted()
                .collect(Collectors.toList());
        if (paremetersProviders.isEmpty()) {
            // Single instance using the default constructor.
            return Stream.ofNullable(MethodCall.DEFAULT_CTOR_ARGS);
        }

        // Pick the first method from the list.
        Method paremetersProvider = paremetersProviders.iterator().next();
        if (paremetersProviders.size() > 1) {
            trace(String.format(
                    "Found %d public static methods without arguments with %s annotation. Will use %s",
                    paremetersProviders.size(), Parameters.class,
                    paremetersProvider));
            paremetersProviders.stream().map(Method::toString).forEach(
                    TestBuilder::trace);
        }

        // Construct collection of arguments for test class instances.
        return ((Collection) paremetersProvider.invoke(null)).stream();
    }

    private static Stream<MethodCall> toMethodCalls(Method method) throws
            IllegalAccessException, InvocationTargetException {
        return toCtorArgs(method).map(v -> toMethodCalls(v, method)).flatMap(
                s -> s).peek(methodCall -> {
                    // Make sure required constructor is accessible if the one is needed.
                    // Need to probe all methods as some of them might be static
                    // and some class members.
                    // Only class members require ctors.
                    try {
                        methodCall.checkRequiredConstructor();
                    } catch (NoSuchMethodException ex) {
                        throw new ParseException(ex.getMessage() + ".");
                    }
                });
    }

    private static Stream<MethodCall> toMethodCalls(Object[] ctorArgs, Method method) {
        if (!isParametrized(method)) {
            return Stream.of(new MethodCall(ctorArgs, method));
        }
        Parameter[] annotations = getMethodParameters(method);
        if (annotations.length == 0) {
            return Stream.of(new MethodCall(ctorArgs, method));
        }
        return Stream.of(annotations).map((a) -> {
            Class paramType = method.getParameterTypes()[0];
            final Object annotationValue;
            if (!paramType.isArray()) {
                annotationValue = fromString(a.value()[0], paramType);
            } else {
                Class paramComponentType = paramType.getComponentType();
                annotationValue = Array.newInstance(paramComponentType, a.value().length);
                var idx = new AtomicInteger(-1);
                Stream.of(a.value()).map(v -> fromString(v, paramComponentType)).sequential().forEach(
                        v -> Array.set(annotationValue, idx.incrementAndGet(), v));
            }
            return new MethodCall(ctorArgs, method, annotationValue);
        });
    }

    private static Object fromString(String value, Class toType) {
        Function<String, Object> converter = conv.get(toType);
        if (converter == null) {
            throw new RuntimeException(String.format(
                    "Failed to find a conversion of [%s] string to %s type",
                    value, toType));
        }
        return converter.apply(value);
    }

    // Wraps Method.invike() into ThrowingRunnable.run()
    private ThrowingConsumer wrap(Method method) {
        return (test) -> {
            Class methodClass = method.getDeclaringClass();
            String methodName = String.join(".", methodClass.getName(),
                    method.getName());
            TKit.log(String.format("[ CALL     ] %s()", methodName));
            if (!dryRun) {
                if (methodClass.isInstance(test)) {
                    method.invoke(test);
                } else {
                    method.invoke(null);
                }
            }
        };
    }

    private static class ParseException extends IllegalArgumentException {

        ParseException(String msg) {
            super(msg);
        }

        void setContext(String badCmdLineArg) {
            this.badCmdLineArg = badCmdLineArg;
        }

        @Override
        public String getMessage() {
            String msg = super.getMessage();
            if (badCmdLineArg != null) {
                msg = String.format("%s parameter=[%s]", msg, badCmdLineArg);
            }
            return msg;
        }
        private String badCmdLineArg;
    }

    static void trace(String msg) {
        if (TKit.VERBOSE_TEST_SETUP) {
            TKit.log(msg);
        }
    }

    private final Map<String, ThrowingConsumer<String>> argProcessors;
    private Consumer<TestInstance> testConsumer;
    private List<MethodCall> testGroup;
    private List<ThrowingConsumer> beforeActions;
    private List<ThrowingConsumer> afterActions;
    private Set<String> excludedTests;
    private Set<String> includedTests;
    private String spaceSubstitute;
    private boolean dryRun;

    private final static Map<Class, Function<String, Object>> conv = Map.of(
            boolean.class, Boolean::valueOf,
            Boolean.class, Boolean::valueOf,
            int.class, Integer::valueOf,
            Integer.class, Integer::valueOf,
            long.class, Long::valueOf,
            Long.class, Long::valueOf,
            String.class, String::valueOf);

    final static String CMDLINE_ARG_PREFIX = "--jpt-";
}
