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
 * @summary converted from VM Testbase jit/verifier/VerifyInitLocal.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @compile VerifyInitLocal1P.jasm
 * @compile VerifyInitLocal2N.jasm
 * @compile VerifyInitLocal3N.jasm
 * @run main/othervm jit.verifier.VerifyInitLocal.VerifyInitLocal
 */

package jit.verifier.VerifyInitLocal;

import nsk.share.TestFailure;

/**
 * @(#)VerifyInitLocal.java       1.1 01/03/15
 * @bug 4408261
 * @summary Make sure verifier allows initialization of local fields.
 */

public abstract class VerifyInitLocal {
    static final boolean debug = true;
    public static void main(String[] args) throws Exception {
        String[] classes = { "jit.verifier.VerifyInitLocal.VerifyInitLocal1P",
                             "jit.verifier.VerifyInitLocal.VerifyInitLocal2N",
                             "jit.verifier.VerifyInitLocal.VerifyInitLocal3N" };
        for (int i = 0; i < classes.length; i++) {
            boolean is_neg = classes[i].endsWith("N");
            try {
                if (debug)  System.out.println(classes[i]);
                Class.forName(classes[i]).newInstance();
                if (is_neg) {
                    throw new TestFailure("shouldn't successfully verify " +
                                            classes[i]);
                }
            } catch (VerifyError e) {
                if (!is_neg) {
                    System.out.println(e);
                    throw new TestFailure("should successfully verify " +
                                            classes[i]);
                }
                if (debug)  System.out.println(e);
            }
        }
    }

    // Superclass stuff for tests to inherit from:
    protected boolean touch_me_not;
    VerifyInitLocal() {
        if (!verify()) {
            throw new TestFailure("verify()==false in " + getClass());
        }
    }
    public abstract boolean verify();
}
