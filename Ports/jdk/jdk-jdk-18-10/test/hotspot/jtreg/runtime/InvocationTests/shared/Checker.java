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

package shared;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public abstract class Checker {
    protected Class staticTargetClass;
    protected Class dynamicTargetClass;
    protected String methodName;

    public abstract String check (Class callSite);

    public Checker(Class staticTargetClass, Class dynamicTargetClass) {
        if (!staticTargetClass.isAssignableFrom(dynamicTargetClass)) {
            throw new RuntimeException("Dynamic target class should be a subclass of the static target class.");
        }

        // **********************************************
        // NB!!! All classes are assumed to be PUBLIC !!!
        // **********************************************
        Class klass = dynamicTargetClass;
        while (klass != Object.class) {
            if (!Modifier.isPublic(klass.getModifiers())) {
                throw new AssertionError("Class "+klass.getName()+" isn't public.");
            }

            klass = klass.getSuperclass();
        }

        this.methodName = Utils.TARGET_METHOD_NAME;
        this.staticTargetClass = staticTargetClass;
        this.dynamicTargetClass = dynamicTargetClass;
    }

    protected Method getMethodInHierarchy (Class klass) {
        return getMethodInHierarchy(klass, methodName);
    }

    protected Method getMethodInHierarchy (Class klass, String name) {
        while (klass != null) {
            Method method = getDeclaredMethod (klass, name);

            if ( method != null) {
// TODO: why doesn't this check work in VM?
//                int modifiers = method.getModifiers();
//
//                if (Modifier.isPrivate(modifiers)) {
//                    if (klass == initialClass) {
//                        return method;
//                    }
//                } else {
//                    return method;
//                }
                return method;
            }
            klass = klass.getSuperclass();
        }

        return null;
    }

    protected Method getMethod (Class klass) {
        return getMethod (klass, methodName);
    }

    protected Method getDeclaredMethod (Class klass) {
        return getDeclaredMethod (klass, methodName);
    }

    static protected Method getMethod (Class klass, String name) {
        return findMethod (klass.getMethods(), name);
    }

    static protected Method getDeclaredMethod (Class klass, String name) {
        return findMethod (klass.getDeclaredMethods(), name);
    }

    static protected Method findMethod (Method[] methods, String name) {
        for (Method method : methods) {
            if (name.equals(method.getName())) {
                return method;
            }
        }

        return null;
    }

    static public String getClassPackageName(Class klass) {
        String name = klass.getName();
        return getClassPackageName(name);
    }

    static public String getClassPackageName(String name) {
        int lastDotIndex = name.lastIndexOf('.');
        if (lastDotIndex > -1) {
            return name.substring(0, lastDotIndex);
        } else {
            return "";
        }
    }

    public static String abbreviateResult(String result) {
        // Abbreviate exception names
        result = result.replaceAll("java.lang.NullPointerException", "NPE");
        result = result.replaceAll("java.lang.IllegalAccessError", "IAE");
        result = result.replaceAll("java.lang.IllegalAccessException", "IAExc");
        result = result.replaceAll("java.lang.NoSuchMethodError", "NSME");
        result = result.replaceAll("java.lang.AbstractMethodError", "AME");
        result = result.replaceAll("java.lang.IncompatibleClassChangeError", "ICCE");
        result = result.replaceAll("java.lang.VerifyError", "VE");
        result = result.replaceAll("java.lang.ClassFormatError", "CFE");

        return result;
    }

    // Check access possibility from particular call site
    protected boolean checkAccess(Class klass, Class callerClass) {
        int modifiers = klass.getModifiers();

        return checkAccess(modifiers, klass, callerClass);
    }

    protected boolean checkAccess(Method m, Class callerClass) {
        int modifiers = m.getModifiers();
        Class declaringClass = m.getDeclaringClass();

        return checkAccess(modifiers, declaringClass, callerClass);
    }

    protected boolean checkAccess(int modifiers, Class klass, Class callerClass) {
        if ( Modifier.isPublic(modifiers) ) {
            return true;
        } else if ( Modifier.isProtected(modifiers) ) {
            if (klass.isAssignableFrom(callerClass)) {
                return true;
            } else if (getClassPackageName(klass).equals(getClassPackageName(callerClass))) {
                return true;
            }
        } else if ( Modifier.isPrivate(modifiers)) {
            if (klass == callerClass) {
                return true;
            }
        } else if (getClassPackageName(klass).equals(getClassPackageName(callerClass))) {
            return true;
        } else {
            // if method isn't accessible, IllegalAccessException is thrown
            return false;
        }

        return false;
    }
}
