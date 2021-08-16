/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8046171
 * @summary Test the new nestmate reflection API
 * @compile TestReflectionAPI.java
 *          PackagedNestHost.java
 *          PackagedNestHost2.java
 *          SampleNest.java
 *          Hosts.java
 *          InvalidNestHost.java
 *
 * @compile MemberNoHost.jcod
 *          MemberMissingHost.jcod
 *          MemberNotInstanceHost.jcod
 *          MemberNotOurHost.jcod
 *          MemberMalformedHost.jcod
 *          MalformedHost.jcod
 *          PackagedNestHost.jcod
 *          PackagedNestHost2Member.jcod
 *          PackagedNestHostMember.jcod
 *          HostOfMemberNoHost.jcod
 *          HostOfMemberMissingHost.jcod
 *          HostOfMemberNotInstanceHost.jcod
 *          HostOfMemberNotOurHost.jcod
 *          HostOfMemberMalformedHost.jcod
 *          HostWithSelfMember.jcod
 *          HostWithDuplicateMembers.jcod
 *
 * @run main/othervm TestReflectionAPI
 * @run main/othervm/java.security.policy=empty.policy TestReflectionAPI
 */

// We need a nest member class that is invalid for each of the possible reasons,
// plus we need some external classes to test other failure modes.
// For each nested class below there is a corresponding .jcod file which breaks one
// of the rules regarding nest membership. For the package related tests we have
// additional PackageNestHost*.java sources.
// For testing getNestMembers we need an external host class that has a nested class
// which we can form a jcod file from such that we get all the expected failure modes.
// Note that all the .java files must be compiled in the same step, while all
// .jcod files must be compiled in a later step.

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

public class TestReflectionAPI {

    // Valid nest member
    static class Member {}

    // Missing NestHost attribute
    static class MemberNoHost {}

    // Missing NestHost class
    static class MemberMissingHost {}

    // Invalid NestHost class (not instance class)
    static class MemberNotInstanceHost {
        Object[] oa; // create CP entry to use in jcod change
    }

    // Valid but different NestHost class
    static class MemberNotOurHost {}

    // Malformed NestHost class
    static class MemberMalformedHost {}

    public static void main(String[] args) throws Throwable {
        // run tests twice so that failure reasons are
        // seen to remain the same
        for (int i = 0; i < 2; i++) {
            test_getNestHost();
            test_isNestmateOf();
            test_getNestMembers();
        }
    }

    static void test_getNestHost() {
        Class<?> host = TestReflectionAPI.class;

        // sampling of "good" checks

        checkHost(host, host);
        checkHost(Member.class, host);
        Runnable r = new Runnable() { public void run() {}};
        checkHost(r.getClass(), host);

        // all the "bad" classes should report themselves as their
        // own nest host - no exceptions should be thrown
        Class<?>[] allClasses = host.getDeclaredClasses();
        for (Class<?> c : allClasses) {
            if (c == Member.class)
                continue;
            checkHost(c, c);
        }
        checkHost(P1.PackagedNestHost.Member.class,
                  P1.PackagedNestHost.Member.class);
        checkHost(P2.PackagedNestHost2.Member.class,
                  P2.PackagedNestHost2.Member.class);

        // test some 'special' classes
        checkHost(int.class, int.class);                   // primitive
        checkHost(Object[].class, Object[].class);         // array
        checkHost(Thread.State.class, Thread.class);       // enum
        checkHost(java.lang.annotation.Documented.class,   // annotation
                  java.lang.annotation.Documented.class);
    }

    static void test_isNestmateOf() {
        Class<?> host = TestReflectionAPI.class;
        checkNestmates(host, host, true);
        checkNestmates(Member.class, host, true);
        Runnable r = new Runnable() { public void run() {}};
        checkNestmates(r.getClass(), host, true);

        // all the "bad" classes should report themselves as their
        // own nest host - no exceptions should be thrown - so not
        // nestmates
        Class<?>[] allClasses = host.getDeclaredClasses();
        for (Class<?> c : allClasses) {
            if (c == Member.class)
                continue;
            checkNestmates(host, c, false);
        }

        // 'special' classes
        checkNestmates(int.class, int.class, true);             // primitive
        checkNestmates(int.class, long.class, false);           // primitive
        checkNestmates(Object[].class, Object[].class, true);   // array
        checkNestmates(Object[].class, int[].class, false);     // array
        checkNestmates(Thread.State.class, Thread.class, true); // enum
        checkNestmates(java.lang.annotation.Documented.class,   // annotation
                       java.lang.annotation.Documented.class, true);
    }

    static void test_getNestMembers() {
        // Sampling of "good" checks
        Class<?>[] good = { Object.class, Object[].class, int.class};
        checkSingletonNests(good);

        // More thorough correctness check
        checkNest(SampleNest.class, SampleNest.nestedTypes(), false);

        // Special cases - legal but not produced by javac
        checkNest(HostWithSelfMember.class,
                  new Class<?>[] { HostWithSelfMember.class,
                          HostWithSelfMember.Member.class },
                  true);
        checkNest(HostWithDuplicateMembers.class,
                  new Class<?>[] { HostWithDuplicateMembers.class,
                          HostWithDuplicateMembers.Member1.class,
                          HostWithDuplicateMembers.Member2.class },
                  true);

        // Hosts with only "bad" members
        Class<?>[] bad = {
            HostOfMemberNoHost.class,
            HostOfMemberMissingHost.class,
            HostOfMemberNotOurHost.class,
            HostOfMemberNotInstanceHost.class,
            HostOfMemberMalformedHost.class,
        };
        checkSingletonNests(bad);
    }

    static void checkHost(Class<?> target, Class<?> expected) {
        System.out.println("Checking nest host of " + target.getName());
        Class<?> host = target.getNestHost();
        if (host != expected)
            throw new Error("Class " + target.getName() +
                            " has nest host " + host.getName() +
                            " but expected " + expected.getName());
    }

    static void checkNestmates(Class<?> a, Class<?> b, boolean mates) {
        System.out.println("Checking if " + a.getName() +
                           " isNestmateOf " + b.getName());

        if (a.isNestmateOf(b) != mates)
            throw new Error("Class " + a.getName() + " is " +
                            (mates ? "not " : "") +
                            "a nestmate of " + b.getName() + " but should " +
                            (mates ? "" : "not ") + "be");
    }

    static Comparator<Class<?>> cmp = Comparator.comparing(Class::getName);

    static void checkNest(Class<?> host, Class<?>[] unsortedTypes, boolean expectDups) {
        Class<?>[] members = host.getNestMembers();
        Arrays.sort(members, cmp);
        Class<?>[] nestedTypes = unsortedTypes.clone();
        Arrays.sort(nestedTypes, cmp);
        printMembers(host, members);
        printDeclared(host, nestedTypes);
        if (!Arrays.equals(members, nestedTypes)) {
            if (!expectDups) {
                throw new Error("Class " + host.getName() + " has different members " +
                                "compared to declared classes");
            }
            else {
                // get rid of duplicates
                Class<?>[] memberSet =
                    Arrays.stream(members).sorted(cmp).distinct().toArray(Class<?>[]::new);
                if (!Arrays.equals(memberSet, nestedTypes)) {
                    throw new Error("Class " + host.getName() + " has different members " +
                                "compared to declared classes, even after duplicate removal");
                }
            }
        }
        // verify all the relationships that must hold for nest members
        for (Class<?> a : members) {
            checkHost(a, host);
            checkNestmates(a, host, true);
            Class<?>[] aMembers = a.getNestMembers();
            if (aMembers[0] != host) {
                throw new Error("Class " + a.getName() + " getNestMembers()[0] = " +
                                aMembers[0].getName() + " not " + host.getName());

            }
            Arrays.sort(aMembers, cmp);
            if (!Arrays.equals(members, aMembers)) {
                throw new Error("Class " + a.getName() + " has different members " +
                                "compared to host " + host.getName());
            }
            for (Class<?> b : members) {
                checkNestmates(a, b, true);
            }
        }
    }

    static void checkSingletonNests(Class<?>[] classes) {
        for (Class<?> host : classes) {
            Class<?>[] members = host.getNestMembers();
            if (members.length != 1) {
                printMembers(host, members);
                throw new Error("Class " + host.getName() + " lists " + members.length
                                + " members instead of 1 (itself)");
            }
            if (members[0] != host) {
                printMembers(host, members);
                throw new Error("Class " + host.getName() + " lists " +
                                members[0].getName() + " as member instead of itself");
            }
        }
    }

    static void printMembers(Class<?> host, Class<?>[] members) {
        System.out.println("Class " + host.getName() + " has members: ");
        for (Class<?> c : members) {
            System.out.println(" - " + c.getName());
        }
    }

    static void printDeclared(Class<?> host, Class<?>[] declared) {
        System.out.println("Class " + host.getName() + " has declared types: ");
        for (Class<?> c : declared) {
            System.out.println(" - " + c.getName());
        }
    }

}
