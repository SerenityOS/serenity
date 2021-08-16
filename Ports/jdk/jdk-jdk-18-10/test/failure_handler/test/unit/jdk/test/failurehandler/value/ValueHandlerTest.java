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

package jdk.test.failurehandler.value;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Properties;

public class ValueHandlerTest {
    @Test
    public void testApplyAnonymousPrivateFinalInt() throws Exception {
        Properties p = new Properties();
        p.put("int", "010");
        Object o = new Object() {
            @Value (name = "int")
            private final int i1 = -1;
        };
        Field f = o.getClass().getDeclaredField("i1");
        f.setAccessible(true);
        int value = f.getInt(o);
        Assert.assertEquals(value, -1);
        f.setAccessible(false);
        ValueHandler.apply(o, p, null);
        f.setAccessible(true);
        value = f.getInt(o);
        Assert.assertEquals(value, 8);
        f.setAccessible(false);
    }

    @Test
    public void testApplyPublicStaticWithDefault() throws Exception {
        Assert.assertEquals(StaticDefaultCase.s, null);
        Properties p = new Properties();
        StaticDefaultCase o = new StaticDefaultCase();
        ValueHandler.apply(o, p, "prefix");
        Assert.assertEquals(StaticDefaultCase.s, "default");
        p.put("s", "new2");
        ValueHandler.apply(o, p, "prefix");
        Assert.assertEquals(StaticDefaultCase.s, "new2");
        p.put("prefix.s", "new");
        ValueHandler.apply(o, p, "prefix");
        Assert.assertEquals(StaticDefaultCase.s, "new");
        ValueHandler.apply(o, p, null);
        Assert.assertEquals(StaticDefaultCase.s, "new2");
    }

    protected class InnerClass1 {
        @Value (name = "innerClass")
        String[] arr = null;
    }

    public class InnerClass2 extends InnerClass1 {
        @Value (name = "float")
        float f = 0.0f;

        @SubValues (prefix = "inner")
        InnerClass1 inner1 = new InnerClass1();

        @SubValues (prefix = "")
        InnerClass1 inner2 = new InnerClass1();
    }

    @Test
    public void testApplySub() throws Exception {
        InnerClass2 o = new InnerClass2();
        Assert.assertArrayEquals(o.arr, null);
        Assert.assertArrayEquals(o.inner1.arr, null);
        Assert.assertArrayEquals(o.inner2.arr, null);
        Assert.assertEquals(o.f, 0.0f, Float.MIN_VALUE);

        Properties p = new Properties();
        p.put("float", "1.f");
        p.put("innerClass", "a b");
        p.put("inner.innerClass", "a b c");
        ValueHandler.apply(o, p, "");
        Assert.assertArrayEquals(o.arr, new String[]{"a", "b"});
        Assert.assertArrayEquals(o.inner1.arr, new String[]{"a", "b", "c"});
        Assert.assertArrayEquals(o.inner2.arr, new String[]{"a", "b"});
        Assert.assertEquals(o.f, 1.0f, Float.MIN_VALUE);
    }
}

class StaticDefaultCase {
    @Value (name = "s")
    @DefaultValue (value = "default")
    public static String s;
}
