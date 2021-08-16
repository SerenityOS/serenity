/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.invoke.util;

import java.lang.reflect.Modifier;
import static java.lang.reflect.Modifier.*;
import jdk.internal.reflect.Reflection;

/**
 * This class centralizes information about the JVM's linkage access control.
 * @author jrose
 */
public class VerifyAccess {

    private VerifyAccess() { }  // cannot instantiate

    private static final int UNCONDITIONAL_ALLOWED = java.lang.invoke.MethodHandles.Lookup.UNCONDITIONAL;
    private static final int ORIGINAL_ALLOWED = java.lang.invoke.MethodHandles.Lookup.ORIGINAL;
    private static final int MODULE_ALLOWED = java.lang.invoke.MethodHandles.Lookup.MODULE;
    private static final int PACKAGE_ONLY = 0;
    private static final int PACKAGE_ALLOWED = java.lang.invoke.MethodHandles.Lookup.PACKAGE;
    private static final int PROTECTED_OR_PACKAGE_ALLOWED = (PACKAGE_ALLOWED|PROTECTED);
    private static final int ALL_ACCESS_MODES = (PUBLIC|PRIVATE|PROTECTED|PACKAGE_ONLY);

    /**
     * Evaluate the JVM linkage rules for access to the given method
     * on behalf of a caller class which proposes to perform the access.
     * Return true if the caller class has privileges to invoke a method
     * or access a field with the given properties.
     * This requires an accessibility check of the referencing class,
     * plus an accessibility check of the member within the class,
     * which depends on the member's modifier flags.
     * <p>
     * The relevant properties include the defining class ({@code defc})
     * of the member, and its modifier flags ({@code mods}).
     * Also relevant is the class used to make the initial symbolic reference
     * to the member ({@code refc}).  If this latter class is not distinguished,
     * the defining class should be passed for both arguments ({@code defc == refc}).
     * <h3>JVM Specification, 5.4.4 "Access Control"</h3>
     * A field or method R is accessible to a class or interface D if
     * and only if any of the following is true:
     * <ul>
     * <li>R is public.</li>
     * <li>R is protected and is declared in a class C, and D is either
     *     a subclass of C or C itself. Furthermore, if R is not static,
     *     then the symbolic reference to R must contain a symbolic
     *     reference to a class T, such that T is either a subclass of D,
     *     a superclass of D, or D itself.
     *     <p>During verification, it was also required that, even if T is
     *     a superclass of D, the target reference of a protected instance
     *     field access or method invocation must be an instance of D or a
     *     subclass of D (4.10.1.8).</p></li>
     * <li>R is either protected or has default access (that is, neither
     *     public nor protected nor private), and is declared by a class
     *     in the same run-time package as D.</li>
     * <li>R is private and is declared in D by a class or interface
     *     belonging to the same nest as D.</li>
     * </ul>
     * If a referenced field or method is not accessible, access checking
     * throws an IllegalAccessError. If an exception is thrown while
     * attempting to determine the nest host of a class or interface,
     * access checking fails for the same reason.
     *
     * @param refc the class used in the symbolic reference to the proposed member
     * @param defc the class in which the proposed member is actually defined
     * @param mods modifier flags for the proposed member
     * @param lookupClass the class for which the access check is being made
     * @param prevLookupClass the class for which the access check is being made
     * @param allowedModes allowed modes
     * @return true iff the accessing class can access such a member
     */
    public static boolean isMemberAccessible(Class<?> refc,  // symbolic ref class
                                             Class<?> defc,  // actual def class
                                             int      mods,  // actual member mods
                                             Class<?> lookupClass,
                                             Class<?> prevLookupClass,
                                             int      allowedModes) {
        if (allowedModes == 0)  return false;
        assert((allowedModes & ~(ALL_ACCESS_MODES|PACKAGE_ALLOWED|MODULE_ALLOWED|UNCONDITIONAL_ALLOWED|ORIGINAL_ALLOWED)) == 0);
        // The symbolic reference class (refc) must always be fully verified.
        if (!isClassAccessible(refc, lookupClass, prevLookupClass, allowedModes)) {
            return false;
        }
        // Usually refc and defc are the same, but verify defc also in case they differ.
        if (defc == lookupClass  &&
            (allowedModes & PRIVATE) != 0)
            return true;        // easy check; all self-access is OK with a private lookup

        switch (mods & ALL_ACCESS_MODES) {
        case PUBLIC:
            assert (allowedModes & PUBLIC) != 0 || (allowedModes & UNCONDITIONAL_ALLOWED) != 0;
            return true;  // already checked above
        case PROTECTED:
            assert !defc.isInterface(); // protected members aren't allowed in interfaces
            if ((allowedModes & PROTECTED_OR_PACKAGE_ALLOWED) != 0 &&
                isSamePackage(defc, lookupClass))
                return true;
            if ((allowedModes & PROTECTED) == 0)
                return false;
            // Protected members are accessible by subclasses, which does not include interfaces.
            // Interfaces are types, not classes. They should not have access to
            // protected members in j.l.Object, even though it is their superclass.
            if ((mods & STATIC) != 0 &&
                !isRelatedClass(refc, lookupClass))
                return false;
            if ((allowedModes & PROTECTED) != 0 &&
                isSubClass(lookupClass, defc))
                return true;
            return false;
        case PACKAGE_ONLY:  // That is, zero.  Unmarked member is package-only access.
            assert !defc.isInterface(); // package-private members aren't allowed in interfaces
            return ((allowedModes & PACKAGE_ALLOWED) != 0 &&
                    isSamePackage(defc, lookupClass));
        case PRIVATE:
            // Rules for privates follows access rules for nestmates.
            boolean canAccess = ((allowedModes & PRIVATE) != 0 &&
                                 Reflection.areNestMates(defc, lookupClass));
            // for private methods the selected method equals the
            // resolved method - so refc == defc
            assert (canAccess && refc == defc) || !canAccess;
            return canAccess;
        default:
            throw new IllegalArgumentException("bad modifiers: "+Modifier.toString(mods));
        }
    }

    static boolean isRelatedClass(Class<?> refc, Class<?> lookupClass) {
        return (refc == lookupClass ||
                isSubClass(refc, lookupClass) ||
                isSubClass(lookupClass, refc));
    }

    static boolean isSubClass(Class<?> lookupClass, Class<?> defc) {
        return defc.isAssignableFrom(lookupClass) &&
               !lookupClass.isInterface(); // interfaces are types, not classes.
    }

    static int getClassModifiers(Class<?> c) {
        // This would return the mask stored by javac for the source-level modifiers.
        //   return c.getModifiers();
        // But what we need for JVM access checks are the actual bits from the class header.
        // ...But arrays and primitives are synthesized with their own odd flags:
        if (c.isArray() || c.isPrimitive())
            return c.getModifiers();
        return Reflection.getClassAccessFlags(c);
    }

    /**
     * Evaluate the JVM linkage rules for access to the given class on behalf of caller.
     * <h3>JVM Specification, 5.4.4 "Access Control"</h3>
     * A class or interface C is accessible to a class or interface D
     * if and only if any of the following conditions are true:<ul>
     * <li>C is public and in the same module as D.
     * <li>D is in a module that reads the module containing C, C is public and in a
     * package that is exported to the module that contains D.
     * <li>C and D are members of the same runtime package.
     * </ul>
     *
     * @param refc the symbolic reference class to which access is being checked (C)
     * @param lookupClass the class performing the lookup (D)
     * @param prevLookupClass the class from which the lookup was teleported or null
     * @param allowedModes allowed modes
     */
    public static boolean isClassAccessible(Class<?> refc,
                                            Class<?> lookupClass,
                                            Class<?> prevLookupClass,
                                            int allowedModes) {
        if (allowedModes == 0)  return false;
        assert((allowedModes & ~(ALL_ACCESS_MODES|PACKAGE_ALLOWED|MODULE_ALLOWED|UNCONDITIONAL_ALLOWED|ORIGINAL_ALLOWED)) == 0);

        if ((allowedModes & PACKAGE_ALLOWED) != 0 &&
            isSamePackage(lookupClass, refc))
            return true;

        int mods = getClassModifiers(refc);
        if (isPublic(mods)) {

            Module lookupModule = lookupClass.getModule();
            Module refModule = refc.getModule();

            // early VM startup case, java.base not defined
            if (lookupModule == null) {
                assert refModule == null;
                return true;
            }

            // allow access to public types in all unconditionally exported packages
            if ((allowedModes & UNCONDITIONAL_ALLOWED) != 0) {
                return refModule.isExported(refc.getPackageName());
            }

            if (lookupModule == refModule && prevLookupClass == null) {
                // allow access to all public types in lookupModule
                if ((allowedModes & MODULE_ALLOWED) != 0)
                    return true;

                assert (allowedModes & PUBLIC) != 0;
                return refModule.isExported(refc.getPackageName());
            }

            // cross-module access
            // 1. refc is in different module from lookupModule, or
            // 2. refc is in lookupModule and a different module from prevLookupModule
            Module prevLookupModule = prevLookupClass != null ? prevLookupClass.getModule()
                                                              : null;
            assert refModule != lookupModule || refModule != prevLookupModule;
            if (isModuleAccessible(refc, lookupModule, prevLookupModule))
                return true;

            // not exported but allow access during VM initialization
            // because java.base does not have its exports setup
            if (!jdk.internal.misc.VM.isModuleSystemInited())
                return true;

            // public class not accessible to lookupClass
            return false;
        }

        return false;
    }

    /*
     * Tests if a class or interface REFC is accessible to m1 and m2 where m2
     * may be null.
     *
     * A class or interface REFC in m is accessible to m1 and m2 if and only if
     * both m1 and m2 read m and m exports the package of REFC at least to
     * both m1 and m2.
     */
    public static boolean isModuleAccessible(Class<?> refc,  Module m1, Module m2) {
        Module refModule = refc.getModule();
        assert refModule != m1 || refModule != m2;
        int mods = getClassModifiers(refc);
        if (isPublic(mods)) {
            if (m1.canRead(refModule) && (m2 == null || m2.canRead(refModule))) {
                String pn = refc.getPackageName();

                // refc is exported package to at least both m1 and m2
                if (refModule.isExported(pn, m1) && (m2 == null || refModule.isExported(pn, m2)))
                    return true;
            }
        }
        return false;
    }

    /**
     * Decide if the given method type, attributed to a member or symbolic
     * reference of a given reference class, is really visible to that class.
     * @param type the supposed type of a member or symbolic reference of refc
     * @param refc the class attempting to make the reference
     */
    public static boolean isTypeVisible(Class<?> type, Class<?> refc) {
        if (type == refc) {
            return true;  // easy check
        }
        while (type.isArray())  type = type.getComponentType();
        if (type.isPrimitive() || type == Object.class) {
            return true;
        }
        ClassLoader typeLoader = type.getClassLoader();
        ClassLoader refcLoader = refc.getClassLoader();
        if (typeLoader == refcLoader) {
            return true;
        }
        if (refcLoader == null && typeLoader != null) {
            return false;
        }
        if (typeLoader == null && type.getName().startsWith("java.")) {
            // Note:  The API for actually loading classes, ClassLoader.defineClass,
            // guarantees that classes with names beginning "java." cannot be aliased,
            // because class loaders cannot load them directly.
            return true;
        }

        // Do it the hard way:  Look up the type name from the refc loader.
        //
        // Force the refc loader to report and commit to a particular binding for this type name (type.getName()).
        //
        // In principle, this query might force the loader to load some unrelated class,
        // which would cause this query to fail (and the original caller to give up).
        // This would be wasted effort, but it is expected to be very rare, occurring
        // only when an attacker is attempting to create a type alias.
        // In the normal case, one class loader will simply delegate to the other,
        // and the same type will be visible through both, with no extra loading.
        //
        // It is important to go through Class.forName instead of ClassLoader.loadClass
        // because Class.forName goes through the JVM system dictionary, which records
        // the class lookup once for all. This means that even if a not-well-behaved class loader
        // would "change its mind" about the meaning of the name, the Class.forName request
        // will use the result cached in the JVM system dictionary. Note that the JVM system dictionary
        // will record the first successful result. Unsuccessful results are not stored.
        //
        // We use doPrivileged in order to allow an unprivileged caller to ask an arbitrary
        // class loader about the binding of the proposed name (type.getName()).
        // The looked up type ("res") is compared for equality against the proposed
        // type ("type") and then is discarded.  Thus, the worst that can happen to
        // the "child" class loader is that it is bothered to load and report a class
        // that differs from "type"; this happens once due to JVM system dictionary
        // memoization.  And the caller never gets to look at the alternate type binding
        // ("res"), whether it exists or not.
        final String name = type.getName();
        @SuppressWarnings("removal")
        Class<?> res = java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<>() {
                    public Class<?> run() {
                        try {
                            return Class.forName(name, false, refcLoader);
                        } catch (ClassNotFoundException | LinkageError e) {
                            return null; // Assume the class is not found
                        }
                    }
            });
        return (type == res);
    }

    /**
     * Decide if the given method type, attributed to a member or symbolic
     * reference of a given reference class, is really visible to that class.
     * @param type the supposed type of a member or symbolic reference of refc
     * @param refc the class attempting to make the reference
     */
    public static boolean isTypeVisible(java.lang.invoke.MethodType type, Class<?> refc) {
        if (!isTypeVisible(type.returnType(), refc)) {
            return false;
        }
        for (int n = 0, max = type.parameterCount(); n < max; n++) {
            if (!isTypeVisible(type.parameterType(n), refc)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests if two classes are in the same module.
     * @param class1 a class
     * @param class2 another class
     * @return whether they are in the same module
     */
    public static boolean isSameModule(Class<?> class1, Class<?> class2) {
        return class1.getModule() == class2.getModule();
    }

    /**
     * Test if two classes have the same class loader and package qualifier.
     * @param class1 a class
     * @param class2 another class
     * @return whether they are in the same package
     */
    public static boolean isSamePackage(Class<?> class1, Class<?> class2) {
        if (class1 == class2)
            return true;
        if (class1.getClassLoader() != class2.getClassLoader())
            return false;
        return class1.getPackageName() == class2.getPackageName();
    }

    /**
     * Test if two classes are defined as part of the same package member (top-level class).
     * If this is true, they can share private access with each other.
     * @param class1 a class
     * @param class2 another class
     * @return whether they are identical or nested together
     */
    public static boolean isSamePackageMember(Class<?> class1, Class<?> class2) {
        if (class1 == class2)
            return true;
        if (!isSamePackage(class1, class2))
            return false;
        if (getOutermostEnclosingClass(class1) != getOutermostEnclosingClass(class2))
            return false;
        return true;
    }

    private static Class<?> getOutermostEnclosingClass(Class<?> c) {
        Class<?> pkgmem = c;
        for (Class<?> enc = c; (enc = enc.getEnclosingClass()) != null; )
            pkgmem = enc;
        return pkgmem;
    }

    private static boolean loadersAreRelated(ClassLoader loader1, ClassLoader loader2,
                                             boolean loader1MustBeParent) {
        if (loader1 == loader2 || loader1 == null
                || (loader2 == null && !loader1MustBeParent)) {
            return true;
        }
        for (ClassLoader scan2 = loader2;
                scan2 != null; scan2 = scan2.getParent()) {
            if (scan2 == loader1)  return true;
        }
        if (loader1MustBeParent)  return false;
        // see if loader2 is a parent of loader1:
        for (ClassLoader scan1 = loader1;
                scan1 != null; scan1 = scan1.getParent()) {
            if (scan1 == loader2)  return true;
        }
        return false;
    }

    /**
     * Is the class loader of parentClass identical to, or an ancestor of,
     * the class loader of childClass?
     * @param parentClass a class
     * @param childClass another class, which may be a descendent of the first class
     * @return whether parentClass precedes or equals childClass in class loader order
     */
    public static boolean classLoaderIsAncestor(Class<?> parentClass, Class<?> childClass) {
        return loadersAreRelated(parentClass.getClassLoader(), childClass.getClassLoader(), true);
    }
}
