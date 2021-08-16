/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package compiler.jvmci.common.testcases;

import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A test case for tests in compiler.jvmci.compilerToVM package.
 */
public class TestCase {
    private static final Class<?>[] CLASSES = {
            AbstractClass.class,
            AbstractClassExtender.class,
            AnotherSingleImplementer.class,
            AnotherSingleImplementerInterface.class,
            DoNotExtendClass.class,
            DoNotImplementInterface.class,
            MultipleAbstractImplementer.class,
            MultipleImplementer1.class,
            MultipleImplementer2.class,
            MultipleImplementersInterface.class,
            MultipleImplementersInterfaceExtender.class,
            MultiSubclassedClass.class,
            MultiSubclassedClassSubclass1.class,
            MultiSubclassedClassSubclass2.class,
            PackagePrivateClass.class,
            SimpleClass.class,
            SingleImplementer.class,
            SingleImplementerInterface.class,
            SingleSubclass.class,
            SingleSubclassedClass.class
    };

    public static Collection<Class<?>> getAllClasses() {
        return  Arrays.asList(CLASSES);
    }

    public static Collection<Executable> getAllExecutables() {
        Set<Executable> result = new HashSet<>();
        for (Class<?> aClass : CLASSES) {
            result.addAll(Arrays.asList(aClass.getMethods()));
            result.addAll(Arrays.asList(aClass.getConstructors()));
        }
        return result;
    }
}
