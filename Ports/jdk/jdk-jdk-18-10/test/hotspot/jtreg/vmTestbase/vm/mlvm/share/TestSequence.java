/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

package vm.mlvm.share;

import java.util.Collection;

public class TestSequence extends MlvmTest {

    private Collection<Class<?>> testClasses;
    private boolean abortOnFirstFailure;

    TestSequence(Collection<Class<?>> testClasses, boolean abortOnFirstFailure) {
        this.testClasses = testClasses;
        this.abortOnFirstFailure = abortOnFirstFailure;
    }

    @Override
    public boolean run() throws Exception {
        boolean areSomeTestsFailed = false;
        Throwable exception = null;
        for (Class<?> c : this.testClasses) {
            boolean hasTestPassed;
            try {
                hasTestPassed = MlvmTestExecutor.runMlvmTest(c);
            } catch (Throwable e) {
                hasTestPassed = false;
                if (exception == null) {
                    exception = e;
                }
            }
            if (!hasTestPassed) {
                if (this.abortOnFirstFailure) {
                    if (exception != null) {
                        Env.throwAsUncheckedException(exception);
                    }
                    return false;
                } else {
                    areSomeTestsFailed = true;
                }
            }
        }
        if (exception != null) {
            Env.throwAsUncheckedException(exception);
        }
        return !areSomeTestsFailed;
    }
}
