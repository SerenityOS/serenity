/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8247373
 * @modules java.base/jdk.internal.util
 * @run testng NewLength
 * @summary Test edge cases of ArraysSupport.newLength
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import jdk.internal.util.ArraysSupport;

public class NewLength {
    static final int IMAX = Integer.MAX_VALUE;
    static final int SOFT = ArraysSupport.SOFT_MAX_ARRAY_LENGTH;

    // Data that is expected to return a valid value.

    @DataProvider(name = "valid")
    public Object[][] validProvider() {
        return new Object[][] {
           // old     min     pref    expected
            { 0,      1,      0,      1      },
            { 0,      1,      2,      2      },
            { 0,      2,      1,      2      },
            { 0,      1,      SOFT-1, SOFT-1 },
            { 0,      1,      SOFT,   SOFT   },
            { 0,      1,      SOFT+1, SOFT   },
            { 0,      1,      IMAX,   SOFT   },
            { 0,      SOFT-1, IMAX,   SOFT   },
            { 0,      SOFT,   IMAX,   SOFT   },
            { 0,      SOFT+1, IMAX,   SOFT+1 },
            { SOFT-2, 1,      2,      SOFT   },
            { SOFT-1, 1,      2,      SOFT   },
            { SOFT,   1,      2,      SOFT+1 },
            { SOFT+1, 1,      2,      SOFT+2 },
            { IMAX-2, 1,      2,      IMAX-1 },
            { IMAX-1, 1,      2,      IMAX   },
            { SOFT-2, 1,      IMAX,   SOFT   },
            { SOFT-1, 1,      IMAX,   SOFT   },
            { SOFT,   1,      IMAX,   SOFT+1 },
            { SOFT+1, 1,      IMAX,   SOFT+2 },
            { IMAX-2, 1,      IMAX,   IMAX-1 },
            { IMAX-1, 1,      IMAX,   IMAX   }
        };
    }

    // Data that should provoke an OutOfMemoryError

    @DataProvider(name = "error")
    public Object[][] errorProvider() {
        return new Object[][] {
            // old    min   pref
            {    1,   IMAX, IMAX },
            { SOFT,   IMAX, 0    },
            { SOFT,   IMAX, IMAX },
            { IMAX-1,    2, 0    },
            { IMAX,      1, 0    },
            { IMAX,   IMAX, 0    },
            { IMAX,   IMAX, IMAX }
        };
    }

    @Test(dataProvider = "valid")
    public void valid(int old, int min, int pref, int expected) {
        assertEquals(ArraysSupport.newLength(old, min, pref), expected);
    }

    @Test(dataProvider = "error")
    public void error(int old, int min, int pref) {
        try {
            int r = ArraysSupport.newLength(old, min, pref);
            fail("expected OutOfMemoryError, got normal return value of " + r);
        } catch (OutOfMemoryError success) { }
    }
}
