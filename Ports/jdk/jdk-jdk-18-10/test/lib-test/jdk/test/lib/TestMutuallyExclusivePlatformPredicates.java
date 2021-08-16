/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.Asserts;
import jdk.test.lib.Platform;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @test
 * @summary Verify that for each group of mutually exclusive predicates defined
 *          in jdk.test.lib.Platform one and only one predicate
 *          evaluates to true.
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run main TestMutuallyExclusivePlatformPredicates
 */
public class TestMutuallyExclusivePlatformPredicates {
    private static enum MethodGroup {
        ARCH("isAArch64", "isARM", "isPPC", "isS390x", "isX64", "isX86"),
        BITNESS("is32bit", "is64bit"),
        OS("isAix", "isLinux", "isOSX", "isWindows"),
        VM_TYPE("isClient", "isServer", "isMinimal", "isZero", "isEmbedded"),
        MODE("isInt", "isMixed", "isComp"),
        IGNORED("isEmulatedClient", "isDebugBuild", "isFastDebugBuild",
                "isSlowDebugBuild", "hasSA", "isRoot", "isTieredSupported",
                "areCustomLoadersSupportedForCDS", "isDefaultCDSArchiveSupported",
                "isSignedOSX");

        public final List<String> methodNames;

        private MethodGroup(String... methodNames) {
            this.methodNames = Collections.unmodifiableList(
                    Arrays.asList(methodNames));
        }
    }

    public static void main(String args[]) {
        EnumSet<MethodGroup> notIgnoredMethodGroups
                = EnumSet.complementOf(EnumSet.of(MethodGroup.IGNORED));

        notIgnoredMethodGroups.forEach(
                TestMutuallyExclusivePlatformPredicates::verifyPredicates);

        TestMutuallyExclusivePlatformPredicates.verifyCoverage();
    }

    /**
     * Verifies that one and only one predicate method defined in
     * {@link jdk.test.lib.Platform}, whose name included into
     * methodGroup will return {@code true}.
     * @param methodGroup The group of methods that should be tested.
     */
    private static void verifyPredicates(MethodGroup methodGroup) {
        System.out.println("Verifying method group: " + methodGroup.name());
        long truePredicatesCount = methodGroup.methodNames.stream()
                .filter(TestMutuallyExclusivePlatformPredicates
                        ::evaluatePredicate)
                .count();

        Asserts.assertEQ(truePredicatesCount, 1L, String.format(
                "Only one predicate from group %s should be evaluated to true "
                        + "(Actually %d predicates were evaluated to true).",
                methodGroup.name(), truePredicatesCount));
    }

    /**
     * Verifies that all predicates defined in
     * {@link jdk.test.lib.Platform} were either tested or
     * explicitly ignored.
     */
    private static void verifyCoverage() {
        Set<String> allMethods = new HashSet<>();
        for (MethodGroup group : MethodGroup.values()) {
            allMethods.addAll(group.methodNames);
        }

        for (Method m : Platform.class.getMethods()) {
            if (m.getParameterCount() == 0
                    && m.getReturnType() == boolean.class) {
                Asserts.assertTrue(allMethods.contains(m.getName()),
                        "All Platform's methods with signature '():Z' should "
                                + "be tested. Missing: " + m.getName());
            }
        }
    }

    /**
     * Evaluates predicate method with name {@code name} defined in
     * {@link jdk.test.lib.Platform}.
     *
     * @param name The name of a predicate to be evaluated.
     * @return evaluated predicate's value.
     * @throws java.lang.Error if predicate is not defined or could not be
     *                         evaluated.
     */
    private static boolean evaluatePredicate(String name) {
        try {
            System.out.printf("Trying to evaluate predicate with name %s%n",
                    name);
            boolean value
                    = (Boolean) Platform.class.getMethod(name).invoke(null);
            System.out.printf("Predicate evaluated to: %s%n", value);
            return value;
        } catch (NoSuchMethodException e) {
            throw new Error("Predicate with name " + name
                    + " is not defined in " + Platform.class.getName(), e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new Error("Unable to evaluate predicate " + name, e);
        }
    }
}
