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

/**
 * @test
 * @requires vm.jvmci
 * @library ../../../../../
 * @modules jdk.internal.vm.ci/jdk.vm.ci.meta
 *          jdk.internal.vm.ci/jdk.vm.ci.runtime
 *          java.base/jdk.internal.misc
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:-UseJVMCICompiler jdk.vm.ci.runtime.test.TestJavaField
 */

package jdk.vm.ci.runtime.test;

import jdk.vm.ci.meta.JavaField;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaType;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link JavaField}.
 */
public class TestJavaField extends FieldUniverse {

    @Test
    public void getNameTest() {
        for (Map.Entry<Field, ResolvedJavaField> e : fields.entrySet()) {
            String expected = e.getKey().getName();
            String actual = e.getValue().getName();
            assertEquals(expected, actual);
        }
    }

    @Test
    public void getTypeTest() {
        for (Map.Entry<Field, ResolvedJavaField> e : fields.entrySet()) {
            // Must resolve types first as a resolved types != unresolved types
            ResolvedJavaField rf = e.getValue();
            JavaType expected = metaAccess.lookupJavaType(e.getKey().getType()).resolve(rf.getDeclaringClass());
            JavaType actual = rf.getType().resolve(rf.getDeclaringClass());
            assertEquals(expected, actual);
        }
    }

    @Test
    public void getJavaKindTest() {
        for (Map.Entry<Field, ResolvedJavaField> e : fields.entrySet()) {
            JavaKind expected = metaAccess.lookupJavaType(e.getKey().getType()).getJavaKind();
            JavaKind actual = e.getValue().getJavaKind();
            assertEquals(expected, actual);
        }
    }

    @Test
    public void getDeclaringClassTest() {
        for (Map.Entry<Field, ResolvedJavaField> e : fields.entrySet()) {
            Class<?> expected = e.getKey().getDeclaringClass();
            ResolvedJavaType actual = e.getValue().getDeclaringClass();
            assertTrue(actual.equals(metaAccess.lookupJavaType(expected)));
        }
    }
}
