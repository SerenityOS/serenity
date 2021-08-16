/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8003639
 * @summary convert lambda testng tests to jtreg and add them
 * @run testng InInterface
 */

import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

interface LTII {

    interface ILsp1 {
        String m();
    }

    interface ILsp2 {
        String m(String x);
    }

    default ILsp1 t1() {
        return () -> { return "yo"; };
    }

    default ILsp2 t2() {
        return (x) -> { return "snur" + x; };
    }

}

@Test
public class InInterface implements LTII {

    public void testLambdaInDefaultMethod() {
        assertEquals(t1().m(), "yo");
        assertEquals(t2().m("p"), "snurp");
    }

}
