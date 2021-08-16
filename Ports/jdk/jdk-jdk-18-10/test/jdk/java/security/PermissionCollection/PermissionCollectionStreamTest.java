/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8081678
 * @summary Tests for stream returning methods
 * @library /lib/testlibrary/bootlib
 * @build java.base/java.util.stream.OpTestCase
 * @run testng/othervm PermissionCollectionStreamTest
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.FilePermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;
import java.util.stream.OpTestCase;
import java.util.stream.Stream;
import java.util.stream.TestData;

public class PermissionCollectionStreamTest extends OpTestCase {

    @DataProvider
    public static Object[][] permissions() {
        return new Object[][]{
                {
                        "FilePermission",
                        new Permission[]{
                                new FilePermission("/tmp/foobar", "read"),
                                new FilePermission("/tmp/foo", "write"),
                                new FilePermission("/tmp/foobar", "read,write"),
                        }
                },
        };
    }


    private PermissionCollection create(Permission[] pa) {
        PermissionCollection pc = pa[0].newPermissionCollection();
        for (Permission p : pa) {
            pc.add(p);
        }
        return pc;
    }

    @Test(dataProvider = "permissions")
    public void testElementsAsStream(String description, Permission[] pa) {
        PermissionCollection pc = create(pa);

        Supplier<Stream<Permission>> ss = pc::elementsAsStream;

        Collection<Permission> expected = Collections.list(pc.elements());
        withData(TestData.Factory.ofSupplier(description, ss))
                .stream(s -> s)
                .expectedResult(expected)
                .exercise();
    }
}
