/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package gc.stress.gcold;

/*
 * @test TestGCOldWithZ
 * @key randomness
 * @library / /test/lib
 * @requires vm.gc.Z
 * @summary Stress the Z
 * @run main/othervm -Xmx384M -XX:+UseZGC gc.stress.gcold.TestGCOldWithZ 50 1 20 10 10000
 * @run main/othervm -Xmx256m -XX:+UseZGC gc.stress.gcold.TestGCOldWithZ 50 5 20 1 5000
 */
public class TestGCOldWithZ {
    public static void main(String[] args) {
        TestGCOld.main(args);
    }
}
