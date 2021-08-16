/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8255398
 * @run testng TestDropReturn
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static java.lang.invoke.MethodType.methodType;
import static org.testng.Assert.assertEquals;

public class TestDropReturn {

    @Test(dataProvider = "dropReturnCases")
    public void testDropReturn(Class<?> cls, Object testValue) throws Throwable {
        MethodHandle mh = MethodHandles.identity(cls);
        assertEquals(mh.type(), methodType(cls, cls));
        Object x = mh.invoke(testValue);
        assertEquals(x, testValue);

        mh = MethodHandles.dropReturn(mh);
        assertEquals(mh.type(), methodType(void.class, cls));
        mh.invoke(testValue); // should at least work
    }

    @DataProvider
    public static Object[][] dropReturnCases() {
        return new Object[][]{
            { boolean.class, true         },
            { byte.class,    (byte) 10    },
            { char.class,    'x'          },
            { short.class,   (short) 10   },
            { int.class,     10           },
            { long.class,    10L          },
            { float.class,   10F          },
            { double.class,  10D          },
            { Object.class,  new Object() },
            { String.class,  "ABCD"       },
        };
    }
}
