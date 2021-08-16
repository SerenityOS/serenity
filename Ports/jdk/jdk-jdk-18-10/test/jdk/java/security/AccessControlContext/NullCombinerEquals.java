/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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

import java.security.*;

/*
 * @test
 * @bug 4250093
 * @summary AccessControlContext throws NullPointerException
 * if the Combiner is null.
 */
public class NullCombinerEquals {
    public static void main(String[] args) throws Exception {
        NullCombinerEquals nce = new NullCombinerEquals();

        try {
            nce.go();
        } catch (Exception e) {
            throw new Exception("Test Failed: " + e.toString());
        }
    }

    void go() throws Exception {
        AccessControlContext acc = AccessController.getContext();

        // test both combiners are NULL
        acc.equals(acc);

        // test one combiner is NULL
        AccessControlContext acc2 = new AccessControlContext(acc, new DC());
        acc.equals(acc2);

        // test other combiner is NULL
        acc2.equals(acc);

        // test neither combiner is NULL
        AccessControlContext acc3 = new AccessControlContext(acc, new DC());
        acc2.equals(acc3);
    }

    private static class DC implements DomainCombiner {
        public ProtectionDomain[] combine(ProtectionDomain[] a,
                                        ProtectionDomain[] b) {
            // this is irrelevant
            return a;
        }
    }
}
