/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @summary test access checking by java.lang.invoke.MethodHandles.Lookup
 * @compile AccessControlTest.java AccessControlTest_subpkg/Acquaintance_remote.java
 * @run testng/othervm test.java.lang.invoke.AccessControlTest
 */

package test.java.lang.invoke;

import java.lang.invoke.*;
import java.lang.reflect.*;
import java.lang.reflect.Modifier;
import java.util.*;
import org.testng.annotations.*;

import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodHandles.Lookup.*;
import static java.lang.invoke.MethodType.*;
import static org.testng.Assert.*;

import test.java.lang.invoke.AccessControlTest_subpkg.Acquaintance_remote;


/**
 * Test many combinations of Lookup access and cross-class lookupStatic.
 * @author jrose
 */
public class AccessControlTest {
    static final Class<?> THIS_CLASS = AccessControlTest.class;
    // How much output?
    static int verbosity = 0;
    static {
        String vstr = System.getProperty(THIS_CLASS.getSimpleName()+".verbosity");
        if (vstr == null)
            vstr = System.getProperty(THIS_CLASS.getName()+".verbosity");
        if (vstr != null)  verbosity = Integer.parseInt(vstr);
    }

    private class LookupCase implements Comparable<LookupCase> {
        final Lookup   lookup;
        final Class<?> lookupClass;
        final Class<?> prevLookupClass;
        final int      lookupModes;
        public LookupCase(Lookup lookup) {
            this.lookup = lookup;
            this.lookupClass = lookup.lookupClass();
            this.prevLookupClass = lookup.previousLookupClass();
            this.lookupModes = lookup.lookupModes();

            assert(lookupString().equals(lookup.toString()));
            numberOf(lookupClass().getClassLoader()); // assign CL#
        }
        public LookupCase(Class<?> lookupClass, Class<?> prevLookupClass, int lookupModes) {
            this.lookup = null;
            this.lookupClass = lookupClass;
            this.prevLookupClass = prevLookupClass;
            this.lookupModes = lookupModes;
            numberOf(lookupClass().getClassLoader()); // assign CL#
        }

        public final Class<?> lookupClass()     { return lookupClass; }
        public final Class<?> prevLookupClass() { return prevLookupClass; }
        public final int      lookupModes()     { return lookupModes; }

        public Lookup lookup() { lookup.getClass(); return lookup; }

        @Override
        public int compareTo(LookupCase that) {
            Class<?> c1 = this.lookupClass();
            Class<?> c2 = that.lookupClass();
            Class<?> p1 = this.prevLookupClass();
            Class<?> p2 = that.prevLookupClass();
            if (c1 != c2) {
                int cmp = c1.getName().compareTo(c2.getName());
                if (cmp != 0)  return cmp;
                cmp = numberOf(c1.getClassLoader()) - numberOf(c2.getClassLoader());
                assert(cmp != 0);
                return cmp;
            } else if (p1 != p2){
                if (p1 == null)
                    return 1;
                else if (p2 == null)
                    return -1;
                int cmp = p1.getName().compareTo(p2.getName());
                if (cmp != 0)  return cmp;
                cmp = numberOf(p1.getClassLoader()) - numberOf(p2.getClassLoader());
                assert(cmp != 0);
                return cmp;
            }
            return -(this.lookupModes() - that.lookupModes());
        }

        @Override
        public boolean equals(Object that) {
            return (that instanceof LookupCase && equals((LookupCase)that));
        }
        public boolean equals(LookupCase that) {
            return (this.lookupClass() == that.lookupClass() &&
                    this.prevLookupClass() == that.prevLookupClass() &&
                    this.lookupModes() == that.lookupModes());
        }

        @Override
        public int hashCode() {
            return lookupClass().hashCode() + (lookupModes() * 31);
        }

        /** Simulate all assertions in the spec. for Lookup.toString. */
        private String lookupString() {
            String name = lookupClass.getName();
            if (prevLookupClass != null)
                name += "/" + prevLookupClass.getName();
            String suffix = "";
            if (lookupModes == 0)
                suffix = "/noaccess";
            else if (lookupModes == PUBLIC)
                suffix = "/public";
             else if (lookupModes == UNCONDITIONAL)
                suffix = "/publicLookup";
            else if (lookupModes == (PUBLIC|MODULE))
                suffix = "/module";
            else if (lookupModes == (PUBLIC|PACKAGE)
                     || lookupModes == (PUBLIC|MODULE|PACKAGE))
                suffix = "/package";
            else if (lookupModes == (PUBLIC|PACKAGE|PRIVATE)
                    || lookupModes == (PUBLIC|MODULE|PACKAGE|PRIVATE))
                suffix = "/private";
            else if (lookupModes == (PUBLIC|PACKAGE|PRIVATE|PROTECTED)
                     || lookupModes == (PUBLIC|MODULE|PACKAGE|PRIVATE|PROTECTED)
                     || lookupModes == (PUBLIC|MODULE|PACKAGE|PRIVATE|PROTECTED|ORIGINAL))
                suffix = "";
            else
                suffix = "/#"+Integer.toHexString(lookupModes);
            return name+suffix;
        }

        /** Simulate all assertions from the spec. for Lookup.in:
         * <hr>
         * Creates a lookup on the specified new lookup class.
         * [A1] The resulting object will report the specified
         * class as its own {@link #lookupClass lookupClass}.
         * [A1-a] However, the resulting {@code Lookup} object is guaranteed
         * to have no more access capabilities than the original.
         * In particular, access capabilities can be lost as follows:<ul>
         * [A2] If the new lookup class is not the same as the old lookup class,
         * then {@link #ORIGINAL ORIGINAL} access is lost.
         * [A3] If the new lookup class is in a different module from the old one,
         * i.e. {@link #MODULE MODULE} access is lost.
         * [A4] If the new lookup class is in a different package
         * than the old one, protected and default (package) members will not be accessible,
         * i.e. {@link #PROTECTED PROTECTED} and {@link #PACKAGE PACKAGE} access are lost.
         * [A5] If the new lookup class is not within the same package member
         * as the old one, private members will not be accessible, and protected members
         * will not be accessible by virtue of inheritance,
         * i.e. {@link #PRIVATE PRIVATE} access is lost.
         * (Protected members may continue to be accessible because of package sharing.)
         * [A6] If the new lookup class is not
         * {@linkplain #accessClass(Class) accessible} to this lookup,
         * then no members, not even public members, will be accessible
         * i.e. all access modes are lost.
         * [A7] If the new lookup class, the old lookup class and the previous lookup class
         * are all in different modules i.e. teleporting to a third module,
         * all access modes are lost.
         * <p>
         * The new previous lookup class is chosen as follows:
         * [A8] If the new lookup object has {@link #UNCONDITIONAL UNCONDITIONAL} bit,
         * the new previous lookup class is {@code null}.
         * [A9] If the new lookup class is in the same module as the old lookup class,
         * the new previous lookup class is the old previous lookup class.
         * [A10] If the new lookup class is in a different module from the old lookup class,
         * the new previous lookup class is the the old lookup class.
         *
         * Other than the above cases, the new lookup will have the same
         * access capabilities as the original. [A11]
         * <hr>
         */
        public LookupCase in(Class<?> c2) {
            Class<?> c1 = lookupClass();
            Module m1 = c1.getModule();
            Module m2 = c2.getModule();
            Module m0 = prevLookupClass() != null ? prevLookupClass.getModule() : c1.getModule();
            int modes1 = lookupModes();
            int changed = 0;
            // for the purposes of access control then treat classes in different unnamed
            // modules as being in the same module.
            boolean sameModule = (m1 == m2) ||
                                 (!m1.isNamed() && !m2.isNamed());
            boolean samePackage = (c1.getClassLoader() == c2.getClassLoader() &&
                                   c1.getPackageName().equals(c2.getPackageName()));
            boolean sameTopLevel = (topLevelClass(c1) == topLevelClass(c2));
            boolean sameClass = (c1 == c2);
            assert(samePackage  || !sameTopLevel);
            assert(sameTopLevel || !sameClass);
            boolean accessible = sameClass;

            if ((modes1 & PACKAGE) != 0)  accessible |= samePackage;
            if ((modes1 & PUBLIC ) != 0)  {
                if (isModuleAccessible(c2))
                    accessible |= (c2.getModifiers() & PUBLIC) != 0;
                else
                    accessible = false;
            }
            if ((modes1 & UNCONDITIONAL) != 0) {
                if (m2.isExported(c2.getPackageName()))
                    accessible |= (c2.getModifiers() & PUBLIC) != 0;
                else
                    accessible = false;
            }
            if (!accessible) {
                // no access to c2; lose all access.
                changed |= (PUBLIC|MODULE|PACKAGE|PRIVATE|PROTECTED|UNCONDITIONAL);  // [A7]
            }
            if (!sameClass) {
                changed |= ORIGINAL;  // [A2]
            }
            if (m2 != m1 && m0 != m1) {
                // hop to a third module; lose all access
                changed |= (PUBLIC|MODULE|PACKAGE|PRIVATE|PROTECTED);  // [A8]
            }
            if (!sameModule) {
                changed |= MODULE;  // [A3]
            }
            if (!samePackage) {
                // Different package; loose PACKAGE and lower access.
                changed |= (PACKAGE|PRIVATE|PROTECTED);  // [A4]
            }
            if (!sameTopLevel) {
                // Different top-level class.  Lose PRIVATE and PROTECTED access.
                changed |= (PRIVATE|PROTECTED);  // [A5]
            }
            if (sameClass) {
                assert(changed == 0);       // [A11] (no deprivation if same class)
            }

            if (accessible)  assert((changed & PUBLIC) == 0);
            int modes2 = modes1 & ~changed;
            Class<?> plc = (m1 == m2) ? prevLookupClass() : c1; // [A9] [A10]
            if ((modes1 & UNCONDITIONAL) != 0) plc = null;      // [A8]
            LookupCase l2 = new LookupCase(c2, plc, modes2);
            assert(l2.lookupClass() == c2);         // [A1]
            assert((modes1 | modes2) == modes1);    // [A1-a] (no elevation of access)
            assert(l2.prevLookupClass() == null || (modes2 & MODULE) == 0);
            return l2;
        }

        LookupCase dropLookupMode(int modeToDrop) {
            int oldModes = lookupModes();
            int newModes = oldModes & ~(modeToDrop | PROTECTED | ORIGINAL);
            switch (modeToDrop) {
                case PUBLIC: newModes &= ~(MODULE|PACKAGE|PROTECTED|PRIVATE); break;
                case MODULE: newModes &= ~(PACKAGE|PRIVATE); break;
                case PACKAGE: newModes &= ~(PRIVATE); break;
                case PROTECTED:
                case PRIVATE:
                case ORIGINAL:
                case UNCONDITIONAL: break;
                default: throw new IllegalArgumentException(modeToDrop + " is not a valid mode to drop");
            }
            if (newModes == oldModes) return this;  // return self if no change
            LookupCase l2 = new LookupCase(lookupClass(), prevLookupClass(), newModes);
            assert((oldModes | newModes) == oldModes);    // [A2] (no elevation of access)
            assert(l2.prevLookupClass() == null || (newModes & MODULE) == 0);
            return l2;
        }

        boolean isModuleAccessible(Class<?> c) {
            Module m1 = lookupClass().getModule();
            Module m2 = c.getModule();
            Module m0 = prevLookupClass() != null ? prevLookupClass.getModule() : m1;
            String pn = c.getPackageName();
            boolean accessible = m1.canRead(m2) && m2.isExported(pn, m1);
            if (m1 != m0) {
                accessible = accessible && m0.canRead(m2) && m2.isExported(pn, m0);
            }
            return accessible;
        }

        @Override
        public String toString() {
            String s = lookupClass().getSimpleName();
            String lstr = lookupString();
            int sl = lstr.indexOf('/');
            if (sl >= 0)  s += lstr.substring(sl);
            ClassLoader cld = lookupClass().getClassLoader();
            if (cld != THIS_LOADER)  s += "/loader#"+numberOf(cld);
            return s;
        }

        /** Predict the success or failure of accessing this method. */
        public boolean willAccess(Method m) {
            Class<?> c1 = lookupClass();
            Class<?> c2 = m.getDeclaringClass();
            Module m1 = c1.getModule();
            Module m2 = c2.getModule();
            Module m0 = prevLookupClass != null ? prevLookupClass.getModule() : m1;
            // unconditional has access to all public types/members of types that is in a package
            // are unconditionally exported
            if ((lookupModes & UNCONDITIONAL) != 0) {
                return m2.isExported(c2.getPackageName())
                       && Modifier.isPublic(c2.getModifiers())
                       && Modifier.isPublic(m.getModifiers());
            }

            // c1 and c2 are in different module
            if (m1 != m2 || m0 != m2) {
                return (lookupModes & PUBLIC) != 0
                       && isModuleAccessible(c2)
                       && Modifier.isPublic(c2.getModifiers())
                       && Modifier.isPublic(m.getModifiers());
            }

            assert(m1 == m2 && prevLookupClass == null);

            if (!willAccessClass(c2, false))
                return false;

            LookupCase lc = this.in(c2);
            int modes1 = lc.lookupModes();
            int modes2 = fixMods(m.getModifiers());
            // allow private lookup on nestmates. Otherwise, privacy is strictly enforced
            if (c1 != c2 && ((modes2 & PRIVATE) == 0 || !c1.isNestmateOf(c2))) {
                modes1 &= ~PRIVATE;
            }
            // protected access is sometimes allowed
            if ((modes2 & PROTECTED) != 0) {
                int prev = modes2;
                modes2 |= PACKAGE;  // it acts like a package method also
                if ((lookupModes() & PROTECTED) != 0 &&
                    c2.isAssignableFrom(c1))
                    modes2 |= PUBLIC;  // from a subclass, it acts like a public method also
            }
            if (verbosity >= 2)
                System.out.format("%s willAccess %s modes1=0x%h modes2=0x%h => %s%n", lookupString(), lc.lookupString(), modes1, modes2, (modes2 & modes1) != 0);
            return (modes2 & modes1) != 0;
        }

        /** Predict the success or failure of accessing this class. */
        public boolean willAccessClass(Class<?> c2, boolean load) {
            Class<?> c1 = lookupClass();
            if (load && c2.getClassLoader() != null) {
                if (c1.getClassLoader() == null) {
                    // not visible
                    return false;
                }
            }

            Module m1 = c1.getModule();
            Module m2 = c2.getModule();
            Module m0 = prevLookupClass != null ? prevLookupClass.getModule() : m1;
            // unconditional has access to all public types that is in an unconditionally exported package
            if ((lookupModes & UNCONDITIONAL) != 0) {
                return m2.isExported(c2.getPackageName()) && Modifier.isPublic(c2.getModifiers());
            }
            // c1 and c2 are in different module
            if (m1 != m2 || m0 != m2) {
                return (lookupModes & PUBLIC) != 0
                    && isModuleAccessible(c2)
                    && Modifier.isPublic(c2.getModifiers());
            }

            assert(m1 == m2 && prevLookupClass == null);

            LookupCase lc = this.in(c2);
            int modes1 = lc.lookupModes();
            boolean r = false;
            if (modes1 == 0) {
                r = false;
            } else {
                if (Modifier.isPublic(c2.getModifiers())) {
                    if ((modes1 & MODULE) != 0)
                        r = true;
                    else if ((modes1 & PUBLIC) != 0)
                        r = m1.isExported(c2.getPackageName());
                } else {
                    if ((modes1 & PACKAGE) != 0 && c1.getPackage() == c2.getPackage())
                        r = true;
                }
            }
            if (verbosity >= 2) {
                System.out.println(this+" willAccessClass "+lc+" c1="+c1+" c2="+c2+" => "+r);
            }
            return r;
        }
    }

    private static Class<?> topLevelClass(Class<?> cls) {
        Class<?> c = cls;
        for (Class<?> ec; (ec = c.getEnclosingClass()) != null; )
            c = ec;
        assert(c.getEnclosingClass() == null);
        assert(c == cls || cls.getEnclosingClass() != null);
        return c;
    }

    private final TreeSet<LookupCase> CASES = new TreeSet<>();
    private final TreeMap<LookupCase,TreeSet<LookupCase>> CASE_EDGES = new TreeMap<>();
    private final ArrayList<ClassLoader> LOADERS = new ArrayList<>();
    private final ClassLoader THIS_LOADER = this.getClass().getClassLoader();
    { if (THIS_LOADER != null)  LOADERS.add(THIS_LOADER); }  // #1

    private LookupCase lookupCase(String name) {
        for (LookupCase lc : CASES) {
            if (lc.toString().equals(name))
                return lc;
        }
        throw new AssertionError(name);
    }

    private int numberOf(ClassLoader cl) {
        if (cl == null)  return 0;
        int i = LOADERS.indexOf(cl);
        if (i < 0) {
            i = LOADERS.size();
            LOADERS.add(cl);
        }
        return i+1;
    }

    private void addLookupEdge(LookupCase l1, Class<?> c2, LookupCase l2, int dropAccess) {
        TreeSet<LookupCase> edges = CASE_EDGES.get(l2);
        if (edges == null)  CASE_EDGES.put(l2, edges = new TreeSet<>());
        if (edges.add(l1)) {
            Class<?> c1 = l1.lookupClass();
            assert(l2.lookupClass() == c2); // [A1]
            int m1 = l1.lookupModes();
            int m2 = l2.lookupModes();
            assert((m1 | m2) == m1);        // [A2] (no elevation of access)
            LookupCase expect = dropAccess == 0 ? l1.in(c2) : l1.in(c2).dropLookupMode(dropAccess);
            if (!expect.equals(l2))
                System.out.println("*** expect "+l1+" => "+expect+" but got "+l2);
            assertEquals(l2, expect);
        }
    }

    private void makeCases(Lookup[] originalLookups) {
        // make initial set of lookup test cases
        CASES.clear(); LOADERS.clear(); CASE_EDGES.clear();
        ArrayList<Class<?>> classes = new ArrayList<>();
        for (Lookup l : originalLookups) {
            CASES.add(new LookupCase(l));
            classes.remove(l.lookupClass());  // no dups please
            classes.add(l.lookupClass());
        }
        System.out.println("loaders = "+LOADERS);
        int rounds = 0;
        for (int lastCount = -1; lastCount != CASES.size(); ) {
            lastCount = CASES.size();  // if CASES grow in the loop we go round again
            for (LookupCase lc1 : CASES.toArray(new LookupCase[0])) {
                for (int mode : ACCESS_CASES) {
                    LookupCase lc2 = new LookupCase(lc1.lookup().dropLookupMode(mode));
                    addLookupEdge(lc1, lc1.lookupClass(), lc2, mode);
                    CASES.add(lc2);
                }
                for (Class<?> c2 : classes) {
                    LookupCase lc2 = new LookupCase(lc1.lookup().in(c2));
                    addLookupEdge(lc1, c2, lc2, 0);
                    CASES.add(lc2);
                }
            }
            rounds++;
        }
        System.out.println("filled in "+CASES.size()+" cases from "+originalLookups.length+" original cases in "+rounds+" rounds");
        if (false) {
            System.out.println("CASES: {");
            for (LookupCase lc : CASES) {
                System.out.println(lc);
                Set<LookupCase> edges = CASE_EDGES.get(lc);
                if (edges != null)
                    for (LookupCase prev : edges) {
                        System.out.println("\t"+prev);
                    }
            }
            System.out.println("}");
        }
    }

    @Test public void test() {
        makeCases(lookups());
        if (verbosity > 0) {
            verbosity += 9;
            Method pro_in_self = targetMethod(THIS_CLASS, PROTECTED, methodType(void.class));
            testOneAccess(lookupCase("AccessControlTest/module"),  pro_in_self, "find");
            testOneAccess(lookupCase("Remote_subclass/module"),    pro_in_self, "find");
            testOneAccess(lookupCase("Remote_subclass"),           pro_in_self, "find");
            verbosity -= 9;
        }
        Set<Class<?>> targetClassesDone = new HashSet<>();
        for (LookupCase targetCase : CASES) {
            Class<?> targetClass = targetCase.lookupClass();
            if (!targetClassesDone.add(targetClass))  continue;  // already saw this one
            String targetPlace = placeName(targetClass);
            if (targetPlace == null)  continue;  // Object, String, not a target
            for (int targetAccess : ACCESS_CASES) {
                if (targetAccess == MODULE || targetAccess == UNCONDITIONAL)
                    continue;
                MethodType methodType = methodType(void.class);
                Method method = targetMethod(targetClass, targetAccess, methodType);
                // Try to access target method from various contexts.
                for (LookupCase sourceCase : CASES) {
                    testOneAccess(sourceCase, method, "findClass");
                    testOneAccess(sourceCase, method, "accessClass");
                    testOneAccess(sourceCase, method, "find");
                    testOneAccess(sourceCase, method, "unreflect");
                }
            }
        }
        System.out.println("tested "+testCount+" access scenarios; "+testCountFails+" accesses were denied");
    }

    private int testCount, testCountFails;

    private void testOneAccess(LookupCase sourceCase, Method method, String kind) {
        Class<?> targetClass = method.getDeclaringClass();
        String methodName = method.getName();
        MethodType methodType = methodType(method.getReturnType(), method.getParameterTypes());
        boolean isFindOrAccessClass = "findClass".equals(kind) || "accessClass".equals(kind);
        boolean willAccess = isFindOrAccessClass ?
                sourceCase.willAccessClass(targetClass, "findClass".equals(kind)) : sourceCase.willAccess(method);
        boolean didAccess = false;
        ReflectiveOperationException accessError = null;
        try {
            switch (kind) {
            case "accessClass":
                sourceCase.lookup().accessClass(targetClass);
                break;
            case "findClass":
                sourceCase.lookup().findClass(targetClass.getName());
                break;
            case "find":
                if ((method.getModifiers() & Modifier.STATIC) != 0)
                    sourceCase.lookup().findStatic(targetClass, methodName, methodType);
                else
                    sourceCase.lookup().findVirtual(targetClass, methodName, methodType);
                break;
            case "unreflect":
                sourceCase.lookup().unreflect(method);
                break;
            default:
                throw new AssertionError(kind);
            }
            didAccess = true;
        } catch (ReflectiveOperationException ex) {
            accessError = ex;
        }
        if (willAccess != didAccess) {
            System.out.println(sourceCase+" => "+targetClass.getSimpleName()+(isFindOrAccessClass?"":"."+methodName+methodType));
            System.out.println("fail "+(isFindOrAccessClass?kind:"on "+method)+" ex="+accessError);
            assertEquals(willAccess, didAccess);
        }
        testCount++;
        if (!didAccess)  testCountFails++;
    }

    static Method targetMethod(Class<?> targetClass, int targetAccess, MethodType methodType) {
        String methodName = accessName(targetAccess)+placeName(targetClass);
        if (verbosity >= 2)
            System.out.println(targetClass.getSimpleName()+"."+methodName+methodType);
        try {
            Method method = targetClass.getDeclaredMethod(methodName, methodType.parameterArray());
            assertEquals(method.getReturnType(), methodType.returnType());
            int haveMods = method.getModifiers();
            assert(Modifier.isStatic(haveMods));
            assert(targetAccess == fixMods(haveMods));
            return method;
        } catch (NoSuchMethodException ex) {
            throw new AssertionError(methodName, ex);
        }
    }

    static String placeName(Class<?> cls) {
        // return "self", "sibling", "nestmate", etc.
        if (cls == AccessControlTest.class)  return "self";
        String cln = cls.getSimpleName();
        int under = cln.lastIndexOf('_');
        if (under < 0)  return null;
        return cln.substring(under+1);
    }
    static String accessName(int acc) {
        switch (acc) {
        case PUBLIC:     return "pub_in_";
        case PROTECTED:  return "pro_in_";
        case PACKAGE:    return "pkg_in_";
        case PRIVATE:    return "pri_in_";
        }
        assert(false);
        return "?";
    }
    private static final int[] ACCESS_CASES = {
        PUBLIC, PACKAGE, PRIVATE, PROTECTED, MODULE, UNCONDITIONAL
    };
    /*
     * Adjust PUBLIC => PUBLIC|MODULE|UNCONDITIONAL
     * Adjust 0 => PACKAGE
     */
    /** Return one of the ACCESS_CASES. */
    static int fixMods(int mods) {
        mods &= (PUBLIC|PRIVATE|PROTECTED);
        switch (mods) {
        case PUBLIC: case PRIVATE: case PROTECTED: return mods;
        case 0:  return PACKAGE;
        }
        throw new AssertionError(mods);
    }

    static Lookup[] lookups() {
        ArrayList<Lookup> tem = new ArrayList<>();
        Collections.addAll(tem,
                           AccessControlTest.lookup_in_self(),
                           Inner_nestmate.lookup_in_nestmate(),
                           AccessControlTest_sibling.lookup_in_sibling());
        if (true) {
            Collections.addAll(tem,Acquaintance_remote.lookups());
        } else {
            try {
                Class<?> remc = Class.forName("test.java.lang.invoke.AccessControlTest_subpkg.Acquaintance_remote");
                Lookup[] remls = (Lookup[]) remc.getMethod("lookups").invoke(null);
                Collections.addAll(tem, remls);
            } catch (ReflectiveOperationException ex) {
                throw new LinkageError("reflection failed", ex);
            }
        }
        tem.add(publicLookup());
        tem.add(publicLookup().in(String.class));
        tem.add(publicLookup().in(List.class));
        return tem.toArray(new Lookup[0]);
    }

    static Lookup lookup_in_self() {
        return MethodHandles.lookup();
    }
    public static      void pub_in_self() { }
    protected static   void pro_in_self() { }
    static /*package*/ void pkg_in_self() { }
    private static     void pri_in_self() { }

    static class Inner_nestmate {
        static Lookup lookup_in_nestmate() {
            return MethodHandles.lookup();
        }
        public static      void pub_in_nestmate() { }
        protected static   void pro_in_nestmate() { }
        static /*package*/ void pkg_in_nestmate() { }
        private static     void pri_in_nestmate() { }
    }
}
class AccessControlTest_sibling {
    static Lookup lookup_in_sibling() {
        return MethodHandles.lookup();
    }
    public static      void pub_in_sibling() { }
    protected static   void pro_in_sibling() { }
    static /*package*/ void pkg_in_sibling() { }
    private static     void pri_in_sibling() { }
}

// This guy tests access from outside the package:
/*
package test.java.lang.invoke.AccessControlTest_subpkg;
public class Acquaintance_remote {
    public static Lookup[] lookups() { ...
    }
    ...
}
*/
