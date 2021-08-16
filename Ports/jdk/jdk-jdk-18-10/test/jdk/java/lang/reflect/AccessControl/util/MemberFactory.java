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
package util;

import java.lang.reflect.AccessibleObject;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Function;

import static util.MemberFactory.Kind.CONSTRUCTOR;
import static util.MemberFactory.Kind.FIELD;
import static util.MemberFactory.Kind.METHOD;

/**
 * Enumeration of:
 * <p>
 * {private, package, protected, public} x {instance, static} x {field, method}
 * <p>
 * and:
 * <p>
 * {private, package, protected, public} x {constructor},
 * <p>
 * with each element acting as a factory of AccessibleObject(s)
 * declared by given declaringClass(es).
 */
public enum MemberFactory implements Function<Class<?>, AccessibleObject> {
    // instance fields
    PRIVATE_INSTANCE_FIELD(FIELD, "privateInstance"),
    PACKAGE_INSTANCE_FIELD(FIELD, "packageInstance"),
    PROTECTED_INSTANCE_FIELD(FIELD, "protectedInstance"),
    PUBLIC_INSTANCE_FIELD(FIELD, "publicInstance"),
    // instance methods
    PRIVATE_INSTANCE_METHOD(METHOD, "privateInstance"),
    PACKAGE_INSTANCE_METHOD(METHOD, "packageInstance"),
    PROTECTED_INSTANCE_METHOD(METHOD, "protectedInstance"),
    PUBLIC_INSTANCE_METHOD(METHOD, "publicInstance"),
    // static fields
    PRIVATE_STATIC_FIELD(FIELD, "privateStatic"),
    PACKAGE_STATIC_FIELD(FIELD, "packageStatic"),
    PROTECTED_STATIC_FIELD(FIELD, "protectedStatic"),
    PUBLIC_STATIC_FIELD(FIELD, "publicStatic"),
    // static methods
    PRIVATE_STATIC_METHOD(METHOD, "privateStatic"),
    PACKAGE_STATIC_METHOD(METHOD, "packageStatic"),
    PROTECTED_STATIC_METHOD(METHOD, "protectedStatic"),
    PUBLIC_STATIC_METHOD(METHOD, "publicStatic"),
    // constructors
    PRIVATE_CONSTRUCTOR(CONSTRUCTOR, null, Void.class, Void.class, Void.class),
    PACKAGE_CONSTRUCTOR(CONSTRUCTOR, null, Void.class, Void.class),
    PROTECTED_CONSTRUCTOR(CONSTRUCTOR, null, Void.class),
    PUBLIC_CONSTRUCTOR(CONSTRUCTOR, null),;

    final Kind kind;
    final String name;
    final Class<?>[] parameterTypes;

    MemberFactory(Kind kind, String name, Class<?>... parameterTypes) {
        this.kind = kind;
        this.name = name;
        this.parameterTypes = parameterTypes;
    }

    @Override
    public AccessibleObject apply(Class<?> declaringClass) {
        return kind.apply(declaringClass, this);
    }

    public static EnumSet<MemberFactory> asSet(MemberFactory... members) {
        return members.length == 0 ? EnumSet.noneOf(MemberFactory.class)
                                   : EnumSet.copyOf(Arrays.asList(members));
    }

    /**
     * @param members the set of MemberFactory(s) to convert to set of
     *                MemberFactory.Group(s).
     * @return a set of groups that cover all elements of the members set if
     * such set of groups exists or null if it doesn't.
     */
    public static EnumSet<Group> membersToGroupsOrNull(EnumSet<MemberFactory> members) {
        EnumSet<MemberFactory> mSet = members.clone();
        EnumSet<Group> gSet = EnumSet.allOf(Group.class);
        Iterator<Group> gIter = gSet.iterator();
        while (gIter.hasNext()) {
            Group g = gIter.next();
            if (mSet.containsAll(g.members)) {
                mSet.removeAll(g.members);
            } else {
                gIter.remove();
            }
        }
        return mSet.isEmpty() ? gSet : null;
    }

    /**
     * @param groups the set of MemberFactory.Group(s) to convert to set of
     *               MemberFactory(s).
     * @return a set of members as a union of members of all groups.
     */
    public static EnumSet<MemberFactory> groupsToMembers(EnumSet<Group> groups) {
        EnumSet<MemberFactory> mSet = EnumSet.noneOf(MemberFactory.class);
        for (Group g : groups) {
            mSet.addAll(g.members);
        }
        return mSet;
    }

    enum Kind implements BiFunction<Class<?>, MemberFactory, AccessibleObject> {
        FIELD {
            @Override
            public AccessibleObject apply(Class<?> declaringClass, MemberFactory factory) {
                assert factory.kind == this;
                try {
                    return declaringClass.getDeclaredField(factory.name);
                } catch (NoSuchFieldException e) {
                    // a fault in test - fail fast
                    throw new RuntimeException(e.getMessage());
                }
            }
        },
        METHOD {
            @Override
            public AccessibleObject apply(Class<?> declaringClass, MemberFactory factory) {
                assert factory.kind == this;
                try {
                    return declaringClass.getDeclaredMethod(factory.name, factory.parameterTypes);
                } catch (NoSuchMethodException e) {
                    // a fault in test - fail fast
                    throw new RuntimeException(e.getMessage());
                }
            }
        },
        CONSTRUCTOR {
            @Override
            public AccessibleObject apply(Class<?> declaringClass, MemberFactory factory) {
                assert factory.kind == this;
                try {
                    return declaringClass.getDeclaredConstructor(factory.parameterTypes);
                } catch (NoSuchMethodException e) {
                    // a fault in test - fail fast
                    throw new RuntimeException(e.getMessage());
                }
            }
        }
    }

    /**
     * We define groups of MemberFactory(s) for members that commonly
     * exhibit same access restrictions in various cases in order to allow
     * specifying groups instead of individual members in the test cases,
     * making them less verbose.
     */
    public enum Group {
        // all members
        ALL(MemberFactory.values()),
        // all private members
        PRIVATE_MEMBERS(PRIVATE_INSTANCE_FIELD, PRIVATE_INSTANCE_METHOD,
                        PRIVATE_STATIC_FIELD, PRIVATE_STATIC_METHOD,
                        PRIVATE_CONSTRUCTOR),
        // all package members
        PACKAGE_MEMBERS(PACKAGE_INSTANCE_FIELD, PACKAGE_INSTANCE_METHOD,
                        PACKAGE_STATIC_FIELD, PACKAGE_STATIC_METHOD,
                        PACKAGE_CONSTRUCTOR),
        // all protected members
        PROTECTED_MEMBERS(PROTECTED_INSTANCE_FIELD, PROTECTED_INSTANCE_METHOD,
                          PROTECTED_STATIC_FIELD, PROTECTED_STATIC_METHOD,
                          PROTECTED_CONSTRUCTOR),
        // all public members
        PUBLIC_MEMBERS(PUBLIC_INSTANCE_FIELD, PUBLIC_INSTANCE_METHOD,
                       PUBLIC_STATIC_FIELD, PUBLIC_STATIC_METHOD,
                       PUBLIC_CONSTRUCTOR),
        // instance field and method pairs
        PRIVATE_INSTANCE_F_M(PRIVATE_INSTANCE_FIELD, PRIVATE_INSTANCE_METHOD),
        PACKAGE_INSTANCE_F_M(PACKAGE_INSTANCE_FIELD, PACKAGE_INSTANCE_METHOD),
        PROTECTED_INSTANCE_F_M(PROTECTED_INSTANCE_FIELD, PROTECTED_INSTANCE_METHOD),
        PUBLIC_INSTANCE_F_M(PUBLIC_INSTANCE_FIELD, PUBLIC_INSTANCE_METHOD),
        // static field and method pairs
        PRIVATE_STATIC_F_M(PRIVATE_STATIC_FIELD, PRIVATE_STATIC_METHOD),
        PACKAGE_STATIC_F_M(PACKAGE_STATIC_FIELD, PACKAGE_STATIC_METHOD),
        PROTECTED_STATIC_F_M(PROTECTED_STATIC_FIELD, PROTECTED_STATIC_METHOD),
        PUBLIC_STATIC_F_M(PUBLIC_STATIC_FIELD, PUBLIC_STATIC_METHOD),
        // constructor singles
        PRIVATE_C(PRIVATE_CONSTRUCTOR),
        PACKAGE_C(PACKAGE_CONSTRUCTOR),
        PROTECTED_C(PROTECTED_CONSTRUCTOR),
        PUBLIC_C(PUBLIC_CONSTRUCTOR);

        final EnumSet<MemberFactory> members;

        Group(MemberFactory... members) {
            this.members = EnumSet.copyOf(Arrays.asList(members));
        }

        public static EnumSet<Group> asSet(Group... groups) {
            return groups.length == 0 ? EnumSet.noneOf(Group.class)
                                      : EnumSet.copyOf(Arrays.asList(groups));
        }
    }
}
