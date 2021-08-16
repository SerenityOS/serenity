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
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:-UseJVMCICompiler jdk.vm.ci.runtime.test.TestJavaType
 */

package jdk.vm.ci.runtime.test;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link JavaType}.
 */
public class TestJavaType extends TypeUniverse {

    public TestJavaType() {
    }

    @Test
    public void getJavaKindTest() {
        for (Class<?> c : classes) {
            JavaType type = metaAccess.lookupJavaType(c);
            JavaKind expected = JavaKind.fromJavaClass(c);
            JavaKind actual = type.getJavaKind();
            assertEquals(expected, actual);
        }
    }
}
