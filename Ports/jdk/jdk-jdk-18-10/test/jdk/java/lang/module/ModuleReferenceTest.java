/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @run testng ModuleReferenceTest
 * @summary Basic tests for java.lang.module.ModuleReference
 */

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.util.Set;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class ModuleReferenceTest {

    private ModuleReference newModuleReference(ModuleDescriptor descriptor, URI uri) {
        return new ModuleReference(descriptor, uri) {
            @Override
            public ModuleReader open() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public void testBasic() throws Exception {
        ModuleDescriptor descriptor
            = ModuleDescriptor.newModule("m")
                .exports("p")
                .exports("q")
                .packages(Set.of("p.internal"))
                .build();

        URI uri = URI.create("module:/m");

        ModuleReference mref = newModuleReference(descriptor, uri);

        assertTrue(mref.descriptor().equals(descriptor));
        assertTrue(mref.location().get().equals(uri));
    }

    @Test(expectedExceptions = { NullPointerException.class })
    public void testNullDescriptor() throws Exception {
        URI location = URI.create("module:/m");
        newModuleReference(null, location);
    }

    public void testNullLocation() {
        ModuleDescriptor descriptor
            = ModuleDescriptor.newModule("m")
                .exports("p")
                .build();
        ModuleReference mref = newModuleReference(descriptor, null);
        assertTrue(!mref.location().isPresent());
    }

}
