/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import util.ClassSupplier;
import util.MemberFactory;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toCollection;
import static util.MemberFactory.*;
import static util.MemberFactory.Group.*;
import static util.ClassSupplier.*;

/**
 * @test
 * @summary An exhaustive test of reflective access controls
 * @bug 6378384
 * @build a.PublicSuper a.Package b.PublicSub b.Package
 *        util.MemberFactory util.ClassSupplier
 * @run main AccessControlTest
 */
public class AccessControlTest {

    public static void main(String[] args) throws Exception {
        boolean ok = true;

        ok &= new Test()
            .current(PACKAGE_CLASS_IN_PKG_A)
            .member (PACKAGE_CLASS_IN_PKG_A).target(PACKAGE_CLASS_IN_PKG_A)
            .allowed(ALL)
            .perform();

        ok &= new Test()
            .current(PACKAGE_CLASS_IN_PKG_A)
            .member (PUBLIC_SUPERCLASS_IN_PKG_A).target(PUBLIC_SUPERCLASS_IN_PKG_A)
            .allowed(PACKAGE_MEMBERS, PROTECTED_MEMBERS, PUBLIC_MEMBERS)
            .denied (PRIVATE_MEMBERS)
            .perform();

        ok &= new Test()
            .current(PACKAGE_CLASS_IN_PKG_A)
            .member (PUBLIC_SUPERCLASS_IN_PKG_A).target(PUBLIC_SUBCLASS_IN_PKG_B)
            .allowed(PACKAGE_MEMBERS, PROTECTED_MEMBERS, PUBLIC_MEMBERS)
            .denied (PRIVATE_MEMBERS)
            .perform();

        ok &= new Test()
            .current(PACKAGE_CLASS_IN_PKG_A)
            .member (PACKAGE_CLASS_IN_PKG_B).target(PACKAGE_CLASS_IN_PKG_B)
            .denied (ALL)
            .perform();

        ok &= new Test()
            .current(PACKAGE_CLASS_IN_PKG_A)
            .member (PUBLIC_SUBCLASS_IN_PKG_B).target(PUBLIC_SUBCLASS_IN_PKG_B)
            .allowed(PUBLIC_MEMBERS)
            .denied (PRIVATE_MEMBERS, PACKAGE_MEMBERS, PROTECTED_MEMBERS)
            .perform();

        ok &= new Test()
            .current(PUBLIC_SUPERCLASS_IN_PKG_A)
            .member (PACKAGE_CLASS_IN_PKG_A).target(PACKAGE_CLASS_IN_PKG_A)
            .allowed(PACKAGE_MEMBERS, PROTECTED_MEMBERS, PUBLIC_MEMBERS)
            .denied (PRIVATE_MEMBERS)
            .perform();

        ok &= new Test()
            .current(PUBLIC_SUPERCLASS_IN_PKG_A)
            .member (PUBLIC_SUPERCLASS_IN_PKG_A).target(PUBLIC_SUPERCLASS_IN_PKG_A)
            .allowed(ALL)
            .perform();

        ok &= new Test()
            .current(PUBLIC_SUPERCLASS_IN_PKG_A)
            .member (PUBLIC_SUPERCLASS_IN_PKG_A).target(PUBLIC_SUBCLASS_IN_PKG_B)
            .allowed(ALL)
            .perform();

        ok &= new Test()
            .current(PUBLIC_SUPERCLASS_IN_PKG_A)
            .member (PACKAGE_CLASS_IN_PKG_B).target(PACKAGE_CLASS_IN_PKG_B)
            .denied (ALL)
            .perform();

        ok &= new Test()
            .current(PUBLIC_SUPERCLASS_IN_PKG_A)
            .member (PUBLIC_SUBCLASS_IN_PKG_B).target(PUBLIC_SUBCLASS_IN_PKG_B)
            .allowed(PUBLIC_MEMBERS)
            .denied (PRIVATE_MEMBERS, PACKAGE_MEMBERS, PROTECTED_MEMBERS)
            .perform();

        ok &= new Test()
            .current(PACKAGE_CLASS_IN_PKG_B)
            .member (PACKAGE_CLASS_IN_PKG_A).target(PACKAGE_CLASS_IN_PKG_A)
            .denied (ALL)
            .perform();

        ok &= new Test()
            .current(PACKAGE_CLASS_IN_PKG_B)
            .member (PUBLIC_SUPERCLASS_IN_PKG_A).target(PUBLIC_SUPERCLASS_IN_PKG_A)
            .allowed(PUBLIC_MEMBERS)
            .denied (PRIVATE_MEMBERS, PACKAGE_MEMBERS, PROTECTED_MEMBERS)
            .perform();

        ok &= new Test()
            .current(PACKAGE_CLASS_IN_PKG_B)
            .member (PUBLIC_SUPERCLASS_IN_PKG_A).target(PUBLIC_SUBCLASS_IN_PKG_B)
            .allowed(PUBLIC_MEMBERS)
            .denied (PRIVATE_MEMBERS, PACKAGE_MEMBERS, PROTECTED_MEMBERS)
            .perform();

        ok &= new Test()
            .current(PACKAGE_CLASS_IN_PKG_B)
            .member (PACKAGE_CLASS_IN_PKG_B).target(PACKAGE_CLASS_IN_PKG_B)
            .allowed(ALL)
            .perform();

        ok &= new Test()
            .current(PACKAGE_CLASS_IN_PKG_B)
            .member (PUBLIC_SUBCLASS_IN_PKG_B).target(PUBLIC_SUBCLASS_IN_PKG_B)
            .allowed(PACKAGE_MEMBERS, PROTECTED_MEMBERS, PUBLIC_MEMBERS)
            .denied (PRIVATE_MEMBERS)
            .perform();

        ok &= new Test()
            .current(PUBLIC_SUBCLASS_IN_PKG_B)
            .member (PACKAGE_CLASS_IN_PKG_A).target(PACKAGE_CLASS_IN_PKG_A)
            .denied (ALL)
            .perform();

        ok &= new Test()
            .current(PUBLIC_SUBCLASS_IN_PKG_B)
            .member (PUBLIC_SUPERCLASS_IN_PKG_A).target(PUBLIC_SUPERCLASS_IN_PKG_A)
            .allowed(PUBLIC_MEMBERS, PROTECTED_STATIC_F_M)
            .denied (PRIVATE_MEMBERS, PACKAGE_MEMBERS, PROTECTED_INSTANCE_F_M,
                     PROTECTED_C)
            .perform();

        ok &= new Test()
            .current(PUBLIC_SUBCLASS_IN_PKG_B)
            .member (PUBLIC_SUPERCLASS_IN_PKG_A).target(PUBLIC_SUBCLASS_IN_PKG_B)
            .allowed(PUBLIC_MEMBERS, PROTECTED_INSTANCE_F_M, PROTECTED_STATIC_F_M)
            .denied (PRIVATE_MEMBERS, PACKAGE_MEMBERS, PROTECTED_C)
            .perform();

        ok &= new Test()
            .current(PUBLIC_SUBCLASS_IN_PKG_B)
            .member (PACKAGE_CLASS_IN_PKG_B).target(PACKAGE_CLASS_IN_PKG_B)
            .allowed(PACKAGE_MEMBERS, PROTECTED_MEMBERS, PUBLIC_MEMBERS)
            .denied (PRIVATE_MEMBERS)
            .perform();

        ok &= new Test()
            .current(PUBLIC_SUBCLASS_IN_PKG_B)
            .member (PUBLIC_SUBCLASS_IN_PKG_B).target(PUBLIC_SUBCLASS_IN_PKG_B)
            .allowed(ALL)
            .perform();

        if (ok) {
            System.out.println("\nAll cases passed.");
        } else {
            throw new RuntimeException("Some cases failed - see log.");
        }
    }

    // use this for generating an exhaustive set of test cases on stdout
    public static class Generate {
        public static void main(String[] args) {
            for (ClassSupplier current : ClassSupplier.values()) {
                for (ClassSupplier member : ClassSupplier.values()) {
                    for (ClassSupplier target : ClassSupplier.values()) {
                        if (member.get().isAssignableFrom(target.get())) {
                            new Test()
                                .current(current).member(member).target(target)
                                .allowed(ALL)
                                .perform(true);
                        }
                    }
                }
            }
        }
    }

    static class Test {

        ClassSupplier currentClassSupplier, memberClassSupplier, targetClassSupplier;
        EnumSet<MemberFactory> expectAllowedMembers = EnumSet.noneOf(MemberFactory.class);
        EnumSet<MemberFactory> expectDeniedMembers = EnumSet.noneOf(MemberFactory.class);

        Test current(ClassSupplier current) {
            currentClassSupplier = current;
            return this;
        }

        Test member(ClassSupplier member) {
            memberClassSupplier = member;
            return this;
        }

        Test target(ClassSupplier target) {
            targetClassSupplier = target;
            return this;
        }

        Test allowed(MemberFactory... allowed) {
            expectAllowedMembers = MemberFactory.asSet(allowed);
            return this;
        }

        Test allowed(MemberFactory.Group... allowedGroups) {
            expectAllowedMembers = MemberFactory.groupsToMembers(
                MemberFactory.Group.asSet(allowedGroups));
            return this;
        }

        Test denied(MemberFactory... denied) {
            expectDeniedMembers = MemberFactory.asSet(denied);
            return this;
        }

        Test denied(MemberFactory.Group... deniedGroups) {
            expectDeniedMembers = MemberFactory.groupsToMembers(
                MemberFactory.Group.asSet(deniedGroups));
            return this;
        }

        boolean perform() {
            return perform(false);
        }

        boolean perform(boolean generateCases) {

            // some validation 1st
            EnumSet<MemberFactory> intersection = EnumSet.copyOf(expectAllowedMembers);
            intersection.retainAll(expectDeniedMembers);
            if (!intersection.isEmpty()) {
                throw new IllegalArgumentException(
                    "Expected allowed and denied MemberFactories have non-empty intersection: " +
                    intersection);
            }

            EnumSet<MemberFactory> missing = EnumSet.allOf(MemberFactory.class);
            missing.removeAll(expectAllowedMembers);
            missing.removeAll(expectDeniedMembers);
            if (!missing.isEmpty()) {
                throw new IllegalArgumentException(
                    "Union of expected allowed and denied MemberFactories is missing elements: " +
                    missing);
            }

            // retrieve method that will perform reflective access
            Method checkAccessMethod;
            try {
                checkAccessMethod = currentClassSupplier.get().getDeclaredMethod(
                    "checkAccess", AccessibleObject.class, Object.class);
                // in case of inaccessible currentClass
                checkAccessMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }

            // construct a target object (for instance field/method)
            Object target;
            Constructor<?> targetConstructor =
                (Constructor<?>) PUBLIC_CONSTRUCTOR.apply(targetClassSupplier.get());
            // in case of inaccessible targetClass
            targetConstructor.setAccessible(true);
            try {
                target = targetConstructor.newInstance(
                    new Object[targetConstructor.getParameterCount()]);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }

            Class<?> memberClass = memberClassSupplier.get();

            Map<Boolean, EnumSet<MemberFactory>> actualMembers = Stream.concat(

                expectAllowedMembers.stream().map(member -> new Trial(member, true)),
                expectDeniedMembers.stream().map(member -> new Trial(member, false))

            ).map(trial -> {

                // obtain AccessibleObject to be used to perform reflective access
                AccessibleObject accessibleObject = trial.member.apply(memberClass);

                // only need target 'obj' for instance fields and methods
                Object obj =
                    (accessibleObject instanceof Field &&
                     !Modifier.isStatic(((Field) accessibleObject).getModifiers())
                     ||
                     accessibleObject instanceof Method &&
                     !Modifier.isStatic(((Method) accessibleObject).getModifiers())
                    )
                    ? target : null;

                // invoke checkAccess method and let it perform the reflective access
                try {
                    checkAccessMethod.invoke(null, accessibleObject, obj);
                    trial.actualAllowed = true;
                } catch (IllegalAccessException e) {
                    // should not happen as checkAccessMethod.isAccessible()
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    if (e.getTargetException() instanceof IllegalAccessException) {
                        trial.actualAllowed = false;
                    } else {
                        // any other Exception is a fault in test or infrastructure - fail fast
                        throw new RuntimeException(e.getTargetException());
                    }
                }

                if (!generateCases) {
                    System.out.printf(
                        "%-26s accessing %26s's %-25s %-43s - expected %s, actual %s: %s\n",
                        currentClassSupplier, memberClassSupplier, trial.member.name(),
                        (obj == null ? "" : "with instance of " + targetClassSupplier),
                        (trial.expectAllowed ? "allowed" : "denied "),
                        (trial.actualAllowed ? "allowed" : "denied "),
                        (trial.expectAllowed == trial.actualAllowed ? "OK" : "FAILURE")
                    );
                }

                return trial;

            }).collect(
                groupingBy(
                    Trial::isActualAllowed,
                    mapping(
                        Trial::getMember,
                        toCollection(() -> EnumSet.noneOf(MemberFactory.class))))
            );

            EnumSet<MemberFactory> actualAllowedMembers =
                Optional.ofNullable(actualMembers.get(true))
                        .orElse(EnumSet.noneOf(MemberFactory.class));
            EnumSet<MemberFactory> actualDeniedMembers =
                Optional.ofNullable(actualMembers.get(false))
                        .orElse(EnumSet.noneOf(MemberFactory.class));

            if (generateCases) {
                System.out.printf(
                    "        ok &= new Test()\n" +
                    "            .current(%s)\n" +
                    "            .member (%s).target(%s)\n",
                    currentClassSupplier,
                    memberClassSupplier, targetClassSupplier
                );

                if (!actualAllowedMembers.isEmpty()) {
                    EnumSet<? extends Enum> actualAllowed =
                        MemberFactory.membersToGroupsOrNull(actualAllowedMembers);
                    if (actualAllowed == null)
                        actualAllowed = actualAllowedMembers;
                    System.out.print(
                        chunkBy(3, actualAllowed.stream().map(Enum::name))
                            .map(chunk -> chunk.collect(joining(", ")))
                            .collect(joining(",\n" +
                                             "                     ",
                                             "            .allowed(",
                                             ")\n"))
                    );
                }

                if (!actualDeniedMembers.isEmpty()) {
                    EnumSet<? extends Enum> actualDenied =
                        MemberFactory.membersToGroupsOrNull(actualDeniedMembers);
                    if (actualDenied == null)
                        actualDenied = actualAllowedMembers;
                    System.out.print(
                        chunkBy(3, actualDenied.stream().map(Enum::name))
                            .map(chunk -> chunk.collect(joining(", ")))
                            .collect(joining(",\n" +
                                             "                     ",
                                             "            .denied (",
                                             ")\n"))
                    );
                }

                System.out.print(
                    "            .perform();\n"
                );
            }

            return expectAllowedMembers.equals(actualAllowedMembers) &&
                   expectDeniedMembers.equals(actualDeniedMembers);
        }
    }

    private static <T> Stream<Stream<T>> chunkBy(int chunkSize, Stream<T> stream) {
        Iterator<T> elements = stream.iterator();
        Stream.Builder<Stream<T>> b1 = Stream.builder();
        while (elements.hasNext()) {
            Stream.Builder<T> b2 = Stream.builder();
            for (int i = 0; i < chunkSize && elements.hasNext(); i++) {
                b2.accept(elements.next());
            }
            b1.accept(b2.build());
        }
        return b1.build();
    }

    private static class Trial {
        final MemberFactory member;
        final boolean expectAllowed;
        boolean actualAllowed;

        Trial(MemberFactory member, boolean expectAllowed) {
            this.member = member;
            this.expectAllowed = expectAllowed;
        }

        MemberFactory getMember() {
            return member;
        }

        boolean isActualAllowed() {
            return actualAllowed;
        }
    }
}
