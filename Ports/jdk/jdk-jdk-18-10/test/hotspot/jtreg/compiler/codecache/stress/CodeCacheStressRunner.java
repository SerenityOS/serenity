/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

package compiler.codecache.stress;

import jdk.test.lib.TimeLimitedRunner;
import jdk.test.lib.Utils;

public class CodeCacheStressRunner {
    private final Runnable action;

    public CodeCacheStressRunner(Runnable action) {
        this.action = action;
    }

    protected final void runTest() {
        Helper.startInfiniteLoopThread(action);
        try {
            // Adjust timeout and substract vm init and exit time
            new TimeLimitedRunner(60 * 1000, 2.0d, this::test).call();
        } catch (Exception e) {
            throw new Error("Exception occurred during test execution", e);
        }
    }

    private boolean test() {
        Helper.TestCase obj = Helper.TestCase.get();
        Helper.callMethod(obj.getCallable(), obj.expectedValue());
        return true;
    }

}
