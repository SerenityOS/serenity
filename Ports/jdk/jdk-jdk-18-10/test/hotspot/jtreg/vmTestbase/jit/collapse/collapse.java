/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase jit/collapse.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.collapse.collapse
 */

package jit.collapse;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

public class collapse {
        public static final GoldChecker goldChecker = new GoldChecker( "collapse" );

        public static void main( String[] argv )
        {
                int i;
                int j=0, k=0;
                long lj=0L, lk=0L;
                float fj=0.0F, fk=0.0F;
                double dj=0.0D, dk=0.0D;
                int n = 4;
                boolean boolPass = true;
                for (i=1; i<n; i++) {
                        j = j + 6;
                        j = j + 6;
                        j = j + 6;
                        j = j + 6;
                }
                for (i=1; i<n; i++) {
                        k = k + 24;
                }
                boolPass &= j==k;
                collapse.goldChecker.println("int + test: "+j+" should equal "+k);
                for (i=1; i<n; i++) {
                        lj = lj + 6L;
                        lj = lj + 6L;
                        lj = lj + 6L;
                        lj = lj + 6L;
                }
                for (i=1; i<n; i++) {
                        lk = lk + 24L;
                }
                boolPass &= lj==lk;
                collapse.goldChecker.println("long + test: "+lj+" should equal "+lk);
                for (i=1; i<n; i++) {
                        dj = dj + 6;
                        dj = dj + 6;
                        dj = dj + 6;
                        dj = dj + 6;
                }
                for (i=1; i<n; i++) {
                        dk = dk + 24;
                }
                boolPass &= dj==dk;
                collapse.goldChecker.println("double + test: "+dj+" should equal "+dk);
                for (i=1; i<n; i++) {
                        fj = fj + 6;
                        fj = fj + 6;
                        fj = fj + 6;
                        fj = fj + 6;
                }
                for (i=1; i<n; i++) {
                        fk = fk + 24;
                }
                boolPass &= fj==fk;
                collapse.goldChecker.println("float + test: "+fj+" should equal "+fk);
                j=0; k=0;
                lj=0L; lk=0L;
                fj=0.0F; fk=0.0F;
                dj=0.0D; dk=0.0D;
                for (i=1; i<n; i++) {
                        j += 6;
                        j += 6;
                        j += 6;
                        j += 6;
                }
                for (i=1; i<n; i++) {
                        k += 24;
                }
                boolPass &= j==k;
                collapse.goldChecker.println("int += test: "+j+" should equal "+k);
                for (i=1; i<n; i++) {
                        lj += 6;
                        lj += 6;
                        lj += 6;
                        lj += 6;
                }
                for (i=1; i<n; i++) {
                        lk += 24;
                }
                boolPass &= lj==lk;
                collapse.goldChecker.println("long += test: "+lj+" should equal "+lk);
                for (i=1; i<n; i++) {
                        dj += 6;
                        dj += 6;
                        dj += 6;
                        dj += 6;
                }
                for (i=1; i<n; i++) {
                        dk += 24;
                }
                boolPass &= dj==dk;
                collapse.goldChecker.println("double += test: "+dj+" should equal "+dk);
                for (i=1; i<n; i++) {
                        fj += 6;
                        fj += 6;
                        fj += 6;
                        fj += 6;
                }
                for (i=1; i<n; i++) {
                        fk += 24;
                }
                boolPass &= fj==fk;
                collapse.goldChecker.println("float += test: "+fj+" should equal "+fk);

                if (!boolPass)
                    throw new TestFailure("Test failed.");
                collapse.goldChecker.check();
        }
}
