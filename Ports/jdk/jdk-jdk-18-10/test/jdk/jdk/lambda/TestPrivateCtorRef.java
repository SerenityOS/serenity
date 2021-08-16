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

import java.util.function.Supplier;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * TestPrivateCtorRef
 */
@Test
public class TestPrivateCtorRef {
    // @@@ Really, this needs to be a combo test:
    //     target class = nested static, nested instance, auxilliary
    //     class protection = PPPP
    //     ctor protection = PPPP
    //     ctor = explicit, explicit
    //     ref = lambda, method ref
    static<T> T makeOne(Supplier<T> supp) {
        T t = supp.get();
        assertNotNull(t != null);
        return t;
    }

    public void testPrivateStatic() {
        makeOne(PS::new);
    }

    private static class PS {
    }

    public void testPrivateInstance() {
        makeOne(PI::new);
    }

    private class PI {
    }
}

