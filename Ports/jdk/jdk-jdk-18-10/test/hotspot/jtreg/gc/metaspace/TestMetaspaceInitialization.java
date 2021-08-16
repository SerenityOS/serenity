/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

package gc.metaspace;

import java.util.ArrayList;

/* @test TestMetaspaceInitialization
 * @bug 8024945
 * @summary Tests to initialize metaspace with a very low MetaspaceSize
 * @modules java.base/jdk.internal.misc
 * @run main/othervm -XX:MetaspaceSize=0 gc.metaspace.TestMetaspaceInitialization
 */
public class TestMetaspaceInitialization {
    private class Internal {
        @SuppressWarnings("unused")
        public int x;
        public Internal(int x) {
            this.x = x;
        }
    }

    private void test() {
        ArrayList<Internal> l = new ArrayList<>();
        l.add(new Internal(17));
    }

    public static void main(String[] args) {
        new TestMetaspaceInitialization().test();
    }
}
