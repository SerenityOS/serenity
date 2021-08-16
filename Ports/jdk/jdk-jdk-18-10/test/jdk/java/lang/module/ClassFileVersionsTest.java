/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @modules java.base/jdk.internal.module
 * @run testng ClassFileVersionsTest
 * @summary Test parsing of module-info.class with different class file versions
 */

import java.lang.module.InvalidModuleDescriptorException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires.Modifier;
import java.nio.ByteBuffer;
import java.util.Set;

import static java.lang.module.ModuleDescriptor.Requires.Modifier.*;

import jdk.internal.module.ModuleInfoWriter;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ClassFileVersionsTest {

    // major, minor, modifiers for requires java.base
    @DataProvider(name = "supported")
    public Object[][] supported() {
        return new Object[][]{
                { 53,   0,  Set.of() },                      // JDK 9
                { 53,   0,  Set.of(STATIC) },
                { 53,   0,  Set.of(TRANSITIVE) },
                { 53,   0,  Set.of(STATIC, TRANSITIVE) },

                { 54,   0,  Set.of() },                      // JDK 10
                { 55,   0,  Set.of() },                      // JDK 11
                { 56,   0,  Set.of() },                      // JDK 12
                { 57,   0,  Set.of() },                      // JDK 13
                { 58,   0,  Set.of() },                      // JDK 14
                { 59,   0,  Set.of() },                      // JDK 15
                { 60,   0,  Set.of() },                      // JDK 16
                { 61,   0,  Set.of() },                      // JDK 17
                { 62,   0,  Set.of() },                      // JDK 18
        };
    }

    // major, minor, modifiers for requires java.base
    @DataProvider(name = "unsupported")
    public Object[][] unsupported() {
        return new Object[][]{
                { 50,   0,  Set.of()},                       // JDK 6
                { 51,   0,  Set.of()},                       // JDK 7
                { 52,   0,  Set.of()},                       // JDK 8

                { 54,   0,  Set.of(STATIC) },                // JDK 10
                { 54,   0,  Set.of(TRANSITIVE) },
                { 54,   0,  Set.of(STATIC, TRANSITIVE) },

                { 55,   0,  Set.of(STATIC) },                // JDK 11
                { 55,   0,  Set.of(TRANSITIVE) },
                { 55,   0,  Set.of(STATIC, TRANSITIVE) },

                { 56,   0,  Set.of(STATIC) },                // JDK 12
                { 56,   0,  Set.of(TRANSITIVE) },
                { 56,   0,  Set.of(STATIC, TRANSITIVE) },

                { 57,   0,  Set.of(STATIC) },                // JDK 13
                { 57,   0,  Set.of(TRANSITIVE) },
                { 57,   0,  Set.of(STATIC, TRANSITIVE) },

                { 58,   0,  Set.of(STATIC) },                // JDK 14
                { 58,   0,  Set.of(TRANSITIVE) },
                { 58,   0,  Set.of(STATIC, TRANSITIVE) },

                { 59,   0,  Set.of(STATIC) },                // JDK 15
                { 59,   0,  Set.of(TRANSITIVE) },
                { 59,   0,  Set.of(STATIC, TRANSITIVE) },

                { 60,   0,  Set.of(STATIC) },                // JDK 16
                { 60,   0,  Set.of(TRANSITIVE) },
                { 60,   0,  Set.of(STATIC, TRANSITIVE) },

                { 61,   0,  Set.of(STATIC) },                // JDK 17
                { 61,   0,  Set.of(TRANSITIVE) },
                { 61,   0,  Set.of(STATIC, TRANSITIVE) },

                { 62,   0,  Set.of(STATIC) },                // JDK 18
                { 62,   0,  Set.of(TRANSITIVE) },
                { 62,   0,  Set.of(STATIC, TRANSITIVE) },

                { 63,   0,  Set.of()},                       // JDK 19
        };
    }

    @Test(dataProvider = "supported")
    public void testSupported(int major, int minor, Set<Modifier> ms) {
        ModuleDescriptor descriptor = ModuleDescriptor.newModule("foo")
                .requires(ms, "java.base")
                .build();
        ByteBuffer bb = ModuleInfoWriter.toByteBuffer(descriptor);
        classFileVersion(bb, major, minor);
        descriptor = ModuleDescriptor.read(bb);
        assertEquals(descriptor.name(), "foo");
    }

    @Test(dataProvider = "unsupported",
          expectedExceptions = InvalidModuleDescriptorException.class)
    public void testUnsupported(int major, int minor, Set<Modifier> ms) {
        ModuleDescriptor descriptor = ModuleDescriptor.newModule("foo")
                .requires(ms, "java.base")
                .build();
        ByteBuffer bb = ModuleInfoWriter.toByteBuffer(descriptor);
        classFileVersion(bb, major, minor);

        // throws InvalidModuleDescriptorException
        ModuleDescriptor.read(bb);
    }

    private void classFileVersion(ByteBuffer bb, int major, int minor) {
        bb.putShort(4, (short) minor);
        bb.putShort(6, (short) major);
    }
}
