/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package invokevirtual;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;


public class Checker extends shared.Checker {
    public Checker(Class staticTargetClass, Class dynamicTargetClass) {
        super(staticTargetClass, dynamicTargetClass);
    }

    public String check (Class callerClass) {
        Method m;
        try {
            // May cause java.lang.VerifyError
            m = getOverriddenMethod();
        } catch (Throwable e) {
            return e.getClass().getName();
        }

        // Check method accessibility (it's a static property, according to JLS #6.6: Access Control)
        if (m != null) {
            Method staticTargetMethod = getDeclaredMethod(staticTargetClass);

            if (checkAccess(staticTargetMethod, callerClass)) {
                // Can't invoke abstract method
                if ( Modifier.isAbstract(m.getModifiers())) {
                    return "java.lang.AbstractMethodError";
                }

                return String.format("%s.%s"
                        , m.getDeclaringClass().getSimpleName()
                        , methodName
                        );
            } else {
                // if method isn't accessible, IllegalAccessError is thrown
                return "java.lang.IllegalAccessError";
            }
        } else {
            // if method == null, NoSuchMethodError is thrown
            return "java.lang.NoSuchMethodError";
        }
    }

    public Method getOverriddenMethod() {
        return getOverriddenMethod(staticTargetClass, dynamicTargetClass);
    }

    public Method getOverriddenMethod(Class staticTarget, Class dynamicTarget) {
        // Assertion #1. C is a subclass of A
        if (!staticTarget.isAssignableFrom(dynamicTarget)) {
            return null;
        }

        Method staticTargetMethod = getDeclaredMethod(staticTarget);
        Method dynamicTargetMethod = getDeclaredMethod(dynamicTarget);

        if (staticTarget.equals(dynamicTarget)) {
            return staticTargetMethod;
        }

        // TODO: ? need to find out the right behavior
        if (staticTargetMethod == null) {
            return null;
        }

        // Dynamic target doesn't have desired method, so check its superclass
        if (dynamicTargetMethod == null) {
            return getOverriddenMethod(staticTarget, dynamicTarget.getSuperclass());
        } else {
            // Private method can't override anything
            if (Modifier.isPrivate(dynamicTargetMethod.getModifiers())) {
                return getOverriddenMethod(staticTarget, dynamicTarget.getSuperclass());
            }
        }

        // TODO: abstract methods

        //Assertion #3.a: A.m2 is PUB || PROT || (PP && PKG(A) == PKG(C))
        int staticTargetModifiers = staticTargetMethod.getModifiers();
        {
            boolean isPublic = Modifier.isPublic(staticTargetModifiers);
            boolean isProtected = Modifier.isProtected(staticTargetModifiers);
            boolean isPrivate = Modifier.isPrivate(staticTargetModifiers) ;
            String staticTargetPkg = getClassPackageName(staticTarget);
            String dynamicTargetPkg = getClassPackageName(dynamicTarget);

            if (isPublic || isProtected
                || (!isPublic && !isProtected && !isPrivate
                    && staticTargetPkg.equals(dynamicTargetPkg))) {
                return dynamicTargetMethod;
            }
        }
        // OR
        //Assertion #3.b: exists m3: C.m1 != B.m3, A.m2 != B.m3, B.m3 overrides A.m2, C.m1 overrides B.m3
        Class ancestor = dynamicTarget.getSuperclass();
        while (ancestor != staticTarget) {
            Method OverriddenM2 = getOverriddenMethod(staticTarget, ancestor);
            Method m3 = getDeclaredMethod(ancestor);
            Method m1 = getOverriddenMethod(ancestor, dynamicTarget);

            if (m1 != null && m3 != null) {
                if (m1.equals(dynamicTargetMethod) && m3.equals(OverriddenM2)) {
                    return dynamicTargetMethod;
                }
            } else {
                if (m1 == null && dynamicTargetMethod == null
                    && m3 == null && OverriddenM2 == null)
                {
                    return null;
                }
            }

            ancestor = ancestor.getSuperclass();
        }

        return getOverriddenMethod(staticTarget, dynamicTarget.getSuperclass());
    }
}
