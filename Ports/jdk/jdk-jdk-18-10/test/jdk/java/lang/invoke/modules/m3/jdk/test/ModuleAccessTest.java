/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;

import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import e1.CrackM5Access;

import static java.lang.invoke.MethodHandles.Lookup.*;
import static org.testng.Assert.*;

public class ModuleAccessTest {
    static ModuleLookup m3;
    static ModuleLookup m4;
    static ModuleLookup m5;
    static Map<String, ModuleLookup> moduleLookupMap = new HashMap<>();
    static Lookup privLookupIn;
    static Lookup privLookupIn2;
    static Lookup unnamedLookup;
    static Class<?> unnamed;
    static Class<?> unnamed1;

    @BeforeTest
    public void setup() throws Exception {
        m3 = new ModuleLookup("m3", 'C');
        m4 = new ModuleLookup("m4", 'D');
        m5 = new ModuleLookup("m5", 'E');
        moduleLookupMap.put(m3.name(), m3);
        moduleLookupMap.put(m4.name(), m4);
        moduleLookupMap.put(m5.name(), m5);

        privLookupIn = MethodHandles.privateLookupIn(m3.type2, m3.lookup);
        privLookupIn2 = MethodHandles.privateLookupIn(m4.type1, m3.lookup);

        unnamed = Class.forName("Unnamed");
        unnamed1 = Class.forName("Unnamed1");
        unnamedLookup = (Lookup)unnamed.getMethod("lookup").invoke(null);

        // m5 reads m3
        CrackM5Access.addReads(m3.module);
        CrackM5Access.addReads(unnamed.getModule());
    }

    @DataProvider(name = "samePackage")
    public Object[][] samePackage() throws Exception {
        return new Object[][] {
            { m3.lookup,     m3.type2 },
            { privLookupIn,  m3.type1 },
            { privLookupIn2, m4.type2 },
            { unnamedLookup, unnamed1 }
        };
    }

    /**
     * Test lookup.in(T) where T is in the same package of the lookup class.
     *
     * [A0] targetClass becomes the lookup class
     * [A1] no change in previous lookup class
     * [A2] PROTECTED, PRIVATE and ORIGINAL are dropped
     */
    @Test(dataProvider = "samePackage")
    public void testLookupInSamePackage(Lookup lookup, Class<?> targetClass) throws Exception {
        Class<?> lookupClass = lookup.lookupClass();
        Lookup lookup2 = lookup.in(targetClass);

        assertTrue(lookupClass.getPackage() == targetClass.getPackage());
        assertTrue(lookupClass.getModule() == targetClass.getModule());
        assertTrue(lookup2.lookupClass() == targetClass);   // [A0]
        assertTrue(lookup2.previousLookupClass() == lookup.previousLookupClass());  // [A1]
        assertTrue(lookup2.lookupModes() == (lookup.lookupModes() & ~(PROTECTED|PRIVATE|ORIGINAL)));  // [A2]
    }

    @DataProvider(name = "sameModule")
    public Object[][] sameModule() throws Exception {
        return new Object[][] {
            { m3.lookup,     m3.type3},
            { privLookupIn,  m3.type3},
            { privLookupIn2, m4.type3}
        };
    }

    /**
     * Test lookup.in(T) where T is in the same module but different package from the lookup class.
     *
     * [A0] targetClass becomes the lookup class
     * [A1] no change in previous lookup class
     * [A2] PROTECTED, PRIVATE, PACKAGE and ORIGINAL are dropped
     */
    @Test(dataProvider = "sameModule")
    public void testLookupInSameModule(Lookup lookup, Class<?> targetClass) throws Exception {
        Class<?> lookupClass = lookup.lookupClass();
        Lookup lookup2 = lookup.in(targetClass);

        assertTrue(lookupClass.getPackage() != targetClass.getPackage());
        assertTrue(lookupClass.getModule() == targetClass.getModule());
        assertTrue(lookup2.lookupClass() == targetClass);   // [A0]
        assertTrue(lookup2.previousLookupClass() == lookup.previousLookupClass());  // [A1]
        assertTrue(lookup2.lookupModes() == (lookup.lookupModes() & ~(PROTECTED|PRIVATE|PACKAGE|ORIGINAL))); // [A2]
    }

    @DataProvider(name = "anotherModule")
    public Object[][] anotherModule() throws Exception {
        return new Object[][] {
            { m3.lookup, m4.type1, m5, m5.accessibleTypesTo(m3.module, m4.module) },
            { m4.lookup, m5.type2, m3, m3.accessibleTypesTo(m4.module, m5.module) },
            { m3.lookup, m5.type1, m4, m4.accessibleTypesTo(m3.module, m5.module) },
            { m5.lookup, unnamed,  m3, m3.accessibleTypesTo(m5.module, unnamed.getModule()) },
        };
    }

    /**
     * Test lookup.in(T) where T is in a different module from the lookup class.
     *
     * [A0] targetClass becomes the lookup class
     * [A1] lookup class becomes previous lookup class
     * [A2] PROTECTED, PRIVATE, PACKAGE, MODULE and ORIGINAL are dropped
     * [A3] no access to module internal types in m0 and m1
     * [A4] if m1 reads m0, can access public types in m0; otherwise no access.
     * [A5] can access public types in m1 exported to m0
     * [A6] can access public types in m2 exported to m0 and m1
     */
    @Test(dataProvider = "anotherModule")
    public void testLookupInAnotherModule(Lookup lookup, Class<?> targetClass,
                                          ModuleLookup m2, Set<Class<?>> otherTypes) throws Exception {
        Class<?> lookupClass = lookup.lookupClass();
        Module m0 = lookupClass.getModule();
        Module m1 = targetClass.getModule();

        assertTrue(m0 != m1);
        assertTrue(m0.canRead(m1));
        assertTrue(m1.isExported(targetClass.getPackageName(), m0));

        Lookup lookup2 = lookup.in(targetClass);
        assertTrue(lookup2.lookupClass() == targetClass);   // [A0]
        assertTrue(lookup2.previousLookupClass() == lookup.lookupClass());  // [A1]
        assertTrue(lookup2.lookupModes() == (lookup.lookupModes() & ~(PROTECTED|PRIVATE|PACKAGE|MODULE|ORIGINAL)));  // [A2]

        // [A3] no access to module internal type in m0
        // [A4] if m1 reads m0,
        // [A4]   no access to public types exported from m0 unconditionally
        // [A4]   no access to public types exported from m0
        ModuleLookup ml0 = moduleLookupMap.get(m0.getName());
        if (m1.canRead(m0)) {
            for (Class<?> type : ml0.unconditionalExports()) {
                testAccess(lookup2, type);
            }
            for (Class<?> type : ml0.qualifiedExportsTo(m1)) {
                testAccess(lookup2, type);
            }
        } else {
            findConstructorExpectingIAE(lookup2, ml0.type1, void.class);
            findConstructorExpectingIAE(lookup2, ml0.type2, void.class);
            findConstructorExpectingIAE(lookup2, ml0.type3, void.class);
        }

        // [A5] can access public types exported from m1 unconditionally
        // [A5] can access public types exported from m1 to m0
        if (m1.isNamed()) {
            ModuleLookup ml1 = moduleLookupMap.get(m1.getName());
            assertTrue(ml1.unconditionalExports().size() + ml1.qualifiedExportsTo(m0).size() > 0);
            for (Class<?> type : ml1.unconditionalExports()) {
                testAccess(lookup2, type);
            }
            for (Class<?> type : ml1.qualifiedExportsTo(m0)) {
                testAccess(lookup2, type);
            }
        } else {
            // unnamed module
            testAccess(lookup2, unnamed1);
        }

        // [A5] can access public types exported from m2 unconditionally
        // [A5] can access public types exported from m2 to m0 and m1
        for (Class<?> type : otherTypes) {
            assertTrue(type.getModule() == m2.module);
            testAccess(lookup2, type);
        }

        // test inaccessible types
        for (Class<?> type : Set.of(m2.type1, m2.type2, m2.type3)) {
            if (!otherTypes.contains(type)) {
                // type is accessible to this lookup
                try {
                    lookup2.accessClass(type);
                    assertTrue(false);
                } catch (IllegalAccessException e) {}

                findConstructorExpectingIAE(lookup2, type, void.class);
            }
        }
    }

    public void testAccess(Lookup lookup, Class<?> type) throws Exception {
        // type is accessible to this lookup
        assertTrue(lookup.accessClass(type) == type);

        // can find constructor
        findConstructor(lookup, type, void.class);

        Module m0 = lookup.previousLookupClass().getModule();
        Module m1 = lookup.lookupClass().getModule();
        Module m2 = type.getModule();

        assertTrue(m0 != m1 && m0 != null);
        assertTrue((lookup.lookupModes() & MODULE) == 0);
        assertTrue(m0 != m2 || m1 != m2);

        MethodHandles.Lookup lookup2 = lookup.in(type);
        if (m2 == m1) {
            // the same module of the lookup class
            assertTrue(lookup2.lookupClass() == type);
            assertTrue(lookup2.previousLookupClass() == lookup.previousLookupClass());
        } else if (m2 == m0) {
            // hop back to the module of the previous lookup class
            assertTrue(lookup2.lookupClass() == type);
            assertTrue(lookup2.previousLookupClass() == lookup.lookupClass());
        } else {
            // hop to a third module
            assertTrue(lookup2.lookupClass() == type);
            assertTrue(lookup2.previousLookupClass() == lookup.lookupClass());
            assertTrue(lookup2.lookupModes() == 0);
        }
    }

    @DataProvider(name = "thirdModule")
    public Object[][] thirdModule() throws Exception {
        return new Object[][] {
            { m3.lookup, m4.type1, m5.type1},
            { m3.lookup, m4.type2, m5.type1},
            { unnamedLookup, m3.type1, m4.type1 },
        };
    }

    /**
     * Test lookup.in(c1).in(c2) where c1 is in second module and c2 is in a third module.
     *
     * [A0] c2 becomes the lookup class
     * [A1] c1 becomes previous lookup class
     * [A2] all access bits are dropped
     */
    @Test(dataProvider = "thirdModule")
    public void testLookupInThirdModule(Lookup lookup, Class<?> c1, Class<?> c2) throws Exception {
        Class<?> c0 = lookup.lookupClass();
        Module m0 = c0.getModule();
        Module m1 = c1.getModule();
        Module m2 = c2.getModule();

        assertTrue(m0 != m1 && m0 != m2 && m1 != m2);
        assertTrue(m0.canRead(m1) && m0.canRead(m2));
        assertTrue(m1.canRead(m2));
        assertTrue(m1.isExported(c1.getPackageName(), m0));
        assertTrue(m2.isExported(c2.getPackageName(), m0) && m2.isExported(c2.getPackageName(), m1));

        Lookup lookup1 = lookup.in(c1);
        assertTrue(lookup1.lookupClass() == c1);
        assertTrue(lookup1.previousLookupClass() == c0);
        assertTrue(lookup1.lookupModes() == (lookup.lookupModes() & ~(PROTECTED|PRIVATE|PACKAGE|MODULE|ORIGINAL)));

        Lookup lookup2 = lookup1.in(c2);
        assertTrue(lookup2.lookupClass() == c2);                    // [A0]
        assertTrue(lookup2.previousLookupClass() == c1);            // [A1]
        assertTrue(lookup2.lookupModes() == 0, lookup2.toString()); // [A2]
    }

    @DataProvider(name = "privLookupIn")
    public Object[][] privLookupIn() throws Exception {
        return new Object[][] {
            { m3.lookup,  m4.type1 },
            { m3.lookup,  m5.type1 },
            { m4.lookup,  m5.type2 },
            { m5.lookup,  m3.type3 },
            { m5.lookup,  unnamed  }
        };
    }

    /**
     * Test privateLookupIn(T, lookup) where T is in another module
     *
     * [A0] full capabilities except MODULE bit
     * [A1] target class becomes the lookup class
     * [A2] the lookup class becomes previous lookup class
     * [A3] IAE thrown if lookup has no MODULE access
     */
    @Test(dataProvider = "privLookupIn")
    public void testPrivateLookupIn(Lookup lookup, Class<?> targetClass) throws Exception {
        Module m0 = lookup.lookupClass().getModule();
        Module m1 = targetClass.getModule();

        // privateLookupIn from m0 to m1
        assertTrue(m0 != m1);
        assertTrue(m1.isOpen(targetClass.getPackageName(), m0));
        Lookup privLookup1 = MethodHandles.privateLookupIn(targetClass, lookup);
        assertTrue(privLookup1.lookupModes() == (PROTECTED|PRIVATE|PACKAGE|PUBLIC));  // [A0]
        assertTrue(privLookup1.lookupClass() == targetClass);                    // [A1]
        assertTrue(privLookup1.previousLookupClass() == lookup.lookupClass());   // [A2]

        // privLookup1 has no MODULE access; can't do privateLookupIn
        try {
            Lookup privLookup2 = MethodHandles.privateLookupIn(targetClass, privLookup1); // [A3]
            assertFalse(privLookup2 != null);
        } catch (IllegalAccessException e) {}
    }

    /**
     * Test member access from the Lookup returned from privateLookupIn
     */
    @Test
    public void testPrivateLookupAccess() throws Exception {
        Class<?> staticsClass = e1.Statics.class;
        Lookup privLookup1 = MethodHandles.privateLookupIn(staticsClass, m4.lookup);
        assertTrue((privLookup1.lookupModes() & MODULE) == 0);
        assertTrue(privLookup1.lookupClass() == staticsClass);
        assertTrue(privLookup1.previousLookupClass() == m4.lookup.lookupClass());

        // access private member and default package member in m5
        MethodType mtype = MethodType.methodType(void.class);
        MethodHandle mh1 = privLookup1.findStatic(staticsClass, "privateMethod", mtype);
        MethodHandle mh2 = privLookup1.findStatic(staticsClass, "packageMethod", mtype);

        // access public member in exported types from m5 to m4
        findConstructor(privLookup1, m5.type1, void.class);
        // no access to public member in non-exported types to m5
        findConstructorExpectingIAE(privLookup1, m5.type3, void.class);

        // no access to public types in m4 since m5 does not read m4
        assertFalse(m5.module.canRead(m4.module));
        findConstructorExpectingIAE(privLookup1, m4.type1, void.class);

        // teleport from a privateLookup to another class in the same package
        // lose private access
        Lookup privLookup2 = MethodHandles.privateLookupIn(m5.type1, m4.lookup);
        Lookup lookup = privLookup2.in(staticsClass);
        assertTrue((lookup.lookupModes() & PRIVATE) == 0);
        MethodHandle mh3 = lookup.findStatic(staticsClass, "packageMethod", mtype);
        try {
            lookup.findStatic(staticsClass, "privateMethod", mtype);
            assertTrue(false);
        } catch (IllegalAccessException e) {}
    }

    /**
     * Test member access from the Lookup returned from privateLookupIn and
     * the lookup mode after dropLookupMode
     */
    @Test
    public void testDropLookupMode() throws Exception {
        Lookup lookup = MethodHandles.privateLookupIn(m5.type1, m4.lookup);
        assertTrue((lookup.lookupModes() & MODULE) == 0);

        Lookup lookup1 = lookup.dropLookupMode(PRIVATE);
        assertTrue(lookup1.lookupModes() == (lookup.lookupModes() & ~(PROTECTED|PRIVATE)));
        Lookup lookup2 = lookup.dropLookupMode(PACKAGE);
        assertTrue(lookup2.lookupModes() == (lookup.lookupModes() & ~(PROTECTED|PRIVATE|PACKAGE)));
        Lookup lookup3 = lookup.dropLookupMode(MODULE);
        assertTrue(lookup3.lookupModes() == (lookup.lookupModes() & ~(PROTECTED|PRIVATE|PACKAGE)));
        Lookup lookup4 = lookup.dropLookupMode(PUBLIC);
        assertTrue(lookup4.lookupModes() == 0);

    }

    /**
     * Test no access to a public member on a non-public class
     */
    @Test
    public void testPrivateLookupOnNonPublicType() throws Exception {
        // privateLookup in a non-public type
        Class<?> nonPUblicType = Class.forName("e1.NonPublic");
        Lookup privLookup = MethodHandles.privateLookupIn(nonPUblicType, m4.lookup);
        MethodType mtype = MethodType.methodType(void.class);
        MethodHandle mh1 = privLookup.findStatic(nonPUblicType, "publicStatic", mtype);

        // drop MODULE access i.e. only PUBLIC access
        Lookup lookup = privLookup.dropLookupMode(MODULE);
        assertTrue(lookup.lookupModes() == PUBLIC);
        try {
            MethodHandle mh2 = lookup.findStatic(nonPUblicType, "publicStatic", mtype);
            assertFalse(mh2 != null);
        } catch (IllegalAccessException e) {}
    }

    @Test
    public void testPublicLookup() {
        Lookup publicLookup = MethodHandles.publicLookup();
        Lookup pub1 = publicLookup.in(m3.type1);
        Lookup pub2 = pub1.in(java.lang.String.class);
        Lookup pub3 = pub2.in(java.lang.management.ThreadMXBean.class);
        Lookup pub4 = pub3.dropLookupMode(UNCONDITIONAL);

        assertTrue(publicLookup.lookupClass() == Object.class);
        assertTrue(publicLookup.lookupModes() == UNCONDITIONAL);
        assertTrue(pub1.lookupClass() == m3.type1);
        assertTrue(pub1.lookupModes() == UNCONDITIONAL);
        assertTrue(pub2.lookupClass() == String.class);
        assertTrue(pub2.lookupModes() == UNCONDITIONAL);
        assertTrue(pub3.lookupClass() == java.lang.management.ThreadMXBean.class);
        assertTrue(pub3.lookupModes() == UNCONDITIONAL);
        assertTrue(pub4.lookupModes() == 0);

        // publicLookup has no MODULE access; can't do privateLookupIn
        try {
            Lookup pub5 = MethodHandles.privateLookupIn(m4.type1, pub1);
            assertFalse(pub5 != null);
        } catch (IllegalAccessException e) {}
    }

    static class ModuleLookup {
        private final Module module;
        private final Set<String> packages;
        private final Lookup lookup;
        private final Class<?> type1;
        private final Class<?> type2;
        private final Class<?> type3;

        ModuleLookup(String mn, char c) throws Exception {
            this.module = ModuleLayer.boot().findModule(mn).orElse(null);
            assertNotNull(this.module);
            this.packages = module.getDescriptor().packages();
            assertTrue(packages.size() <= 3);
            Lookup lookup = null;
            Class<?> type1 = null;
            Class<?> type2 = null;
            Class<?> type3 = null;
            for (String pn : packages) {
                char n = pn.charAt(pn.length() - 1);
                switch (n) {
                    case '1':
                        type1 = Class.forName(pn + "." + c + "1");
                        type2 = Class.forName(pn + "." + c + "2");
                        Method m = type1.getMethod("lookup");
                        lookup = (Lookup) m.invoke(null);
                        break;
                    case '2':
                        type3 = Class.forName(pn + "." + c + "3");
                        break;

                    default:
                }
            }
            this.lookup = lookup;
            this.type1 = type1;
            this.type2 = type2;
            this.type3 = type3;
        }

        String name() {
            return module.getName();
        }

        /*
         * Returns the set of types that are unconditionally exported.
         */
        Set<Class<?>> unconditionalExports() {
            return Stream.of(type1, type2, type3)
                         .filter(c -> module.isExported(c.getPackageName()))
                         .collect(Collectors.toSet());
        }

        /*
         * Returns the set of types that are qualifiedly exported to the specified
         * caller module
         */
        Set<Class<?>> qualifiedExportsTo(Module caller) {
            if (caller.canRead(this.module)) {
                return Stream.of(type1, type2, type3)
                             .filter(c -> !module.isExported(c.getPackageName())
                                          && module.isExported(c.getPackageName(), caller))
                             .collect(Collectors.toSet());
            } else {
                return Set.of();
            }
        }

        /*
         * Returns the set of types that are qualifiedly exported to the specified
         * caller module
         */
        Set<Class<?>> accessibleTypesTo(Module m0, Module m1) {
            if (m0.canRead(this.module) && m1.canRead(this.module)) {
                return Stream.of(type1, type2, type3)
                             .filter(c -> module.isExported(c.getPackageName(), m0)
                                          && module.isExported(c.getPackageName(), m1))
                             .collect(Collectors.toSet());
            } else {
                return Set.of();
            }
        }

        /*
         * Returns the set of types that are open to the specified caller
         * unconditionally or qualifiedly.
         */
        Set<Class<?>> opensTo(Module caller) {
            if (caller.canRead(this.module)) {
                return Stream.of(type1, type2, type3)
                             .filter(c -> module.isOpen(c.getPackageName(), caller))
                             .collect(Collectors.toSet());
            } else {
                return Set.of();
            }
        }

        public String toString() {
            return module.toString();
        }
    }

    /**
     * Invokes Lookup findConstructor with a method type constructed from the
     * given return and parameter types, expecting IllegalAccessException to be
     * thrown.
     */
    static void findConstructorExpectingIAE(Lookup lookup,
                                            Class<?> clazz,
                                            Class<?> rtype,
                                            Class<?>... ptypes) throws Exception {
        try {
            MethodHandle mh = findConstructor(lookup, clazz, rtype, ptypes);
            assertTrue(false);
        } catch (IllegalAccessException expected) { }
    }

    /**
     * Invokes Lookup findConstructor with a method type constructored from the
     * given return and parameter types.
     */
    static MethodHandle findConstructor(Lookup lookup,
                                        Class<?> clazz,
                                        Class<?> rtype,
                                        Class<?>... ptypes) throws Exception {
        MethodType mt = MethodType.methodType(rtype, ptypes);
        return lookup.findConstructor(clazz, mt);
    }
}
