/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @library src
 * @build m/* Basic
 * @run testng/othervm Basic
 * @summary Basic test for annotations on modules
 */

import java.lang.annotation.Annotation;

import p.annotation.Foo;
import p.annotation.Bar;
import p.annotation.Baz;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class Basic {

    final Module module = Foo.class.getModule();

    /**
     * {@code @Foo} does not have RUNTIME retention policy.
     */
    @Test
    public void testInvisibleAnnotation() {
        assertFalse(module.isAnnotationPresent(Foo.class));
        assertNull(module.getAnnotation(Foo.class));
    }

    /**
     * {@code @Bar} has RUNTIME retention policy and value "bar"
     */
    @Test
    public void testBarAnnotation() {
        assertTrue(module.isAnnotationPresent(Bar.class));
        Bar bar = module.getAnnotation(Bar.class);
        assertNotNull(bar);
        assertEquals(bar.value(), "bar");
    }

    /**
     * {@code @Baz} has RUNTIME retention policy has a repeating value
     */
    @Test
    public void testBazAnnotation() {
        assertTrue(module.isAnnotationPresent(Baz.class));
        Baz baz = module.getAnnotation(Baz.class);
        assertNotNull(baz);
        String[] expected = { "one", "two", "three" };
        assertEquals(baz.value(), expected);
    }

    /**
     * Test annotations with RUNTIME retention policy
     */
    @Test
    public void testRuntimeAnnotations() {
        Annotation[] a = module.getAnnotations();
        assertEquals(a, module.getDeclaredAnnotations());
        assertTrue(a.length == 2);
        Bar bar;
        Baz baz;
        if (a[0] instanceof Bar) {
            bar = (Bar)a[0];
            baz = (Baz)a[1];
        } else {
            bar = (Bar)a[1];
            baz = (Baz)a[0];
        }
        assertEquals(bar.value(), "bar");
        String[] expected = { "one", "two", "three"};
        assertEquals(baz.value(), expected);
    }
}
