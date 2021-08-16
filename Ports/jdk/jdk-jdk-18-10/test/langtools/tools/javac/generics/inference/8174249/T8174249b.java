/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8174249
 * @summary Regression in generic method unchecked calls
 * @compile T8174249b.java
 */

import java.util.*;

class T8174249b {

    static void cs(Collection<String> cs) {}

    void test1(Collection c) {
        cs(rawCollection((Class)null));
        Collection<String> cs1 = rawCollection((Class)null);
    }

    void test2(Collection c) {
        cs(rawCollection2((Class)null));
        Collection<String> cs2 = rawCollection2((Class)null);
    }

    void test3(Collection c) {
        cs(rawCollection3((Class)null));
        Collection<String> cs3 = rawCollection2((Class)null);
    }

    Collection<Integer> rawCollection(Class<String> cs) { return null; }

    <Z> Collection<Integer> rawCollection2(Class<Z> cs) { return null; }

    <Z> Collection<Z> rawCollection3(Class<Z> cs) { return null; }
}
