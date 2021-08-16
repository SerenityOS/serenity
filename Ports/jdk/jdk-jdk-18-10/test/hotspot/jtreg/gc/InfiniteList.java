/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4098578
 * @summary Check if the VM properly throws OutOfMemoryError
 * @author Sheng Liang
 * @run main/othervm -Xmx128m gc.InfiniteList
 */

package gc;

public class InfiniteList {
    InfiniteList next;
    long data[] = new long[50000];
    public static void main(String[] args) throws Exception {
        InfiniteList p, q;
        p = new InfiniteList ();
        p.data[p.data.length -1 ] = 999;
        try {
            while (p != null) {
                q = new InfiniteList ();
                q.next = p;
                p = q;
            }
            throw new Exception ("OutOfMemoryError not thrown as expected.");
        } catch (OutOfMemoryError e) {
            return;
        }
    }
}
