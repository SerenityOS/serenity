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

package invokeinterface;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class Checker extends shared.Checker {
    private Class interfaceClass;

    public Checker(Class interfaceClass, Class dynamicTargetClass) {
        super(interfaceClass, dynamicTargetClass);

        if (staticTargetClass.isInterface()) {
            this.interfaceClass = staticTargetClass;
        } else {
            throw new RuntimeException("Static target class should be an interface.");
        }
    }

    public String check (Class callerClass) {
        // Check access rights to interface for caller
        if (!checkAccess(interfaceClass, callerClass)) {
            return "java.lang.IllegalAccessError";
        }

        // NSME is thrown when interface doesn't declare the method
        if (getDeclaredMethod(interfaceClass) == null) {
            return "java.lang.NoSuchMethodError";
        }

        // 9.1.5 Access to Interface Member Names
        // "All interface members are implicitly public. They are
        // accessible outside the package where the interface is
        // declared if the interface is also declared public or
        // protected, in accordance with the rules of 6.6."

        // Search for method declaration in the hierarchy
        Class klass = dynamicTargetClass;

        while (klass != Object.class) {
            Method method = getDeclaredMethod(klass);

            if (method != null) {
                int modifiers = method.getModifiers();

                // Check whether obtained method is public and isn't abstract
                if ( Modifier.isPublic(modifiers)) {
                    if (Modifier.isAbstract(modifiers)) {
                        return "java.lang.AbstractMethodError";
                    } else {
                        return String.format("%s.%s",
                            method.getDeclaringClass().getSimpleName(),
                            methodName);
                    }
                } else {
                    // IAE is thrown when located method isn't PUBLIC
                    // or private.  Private methods are skipped when
                    // looking for an interface method.
                    if (!Modifier.isPrivate(modifiers)) {
                        return "java.lang.IllegalAccessError";
                    }
                }
            }

            klass = klass.getSuperclass();
        }

        // No method declaration is found
        return "java.lang.AbstractMethodError";
    }
}
