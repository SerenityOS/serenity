/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package compiler.compilercontrol.share;

import compiler.compilercontrol.share.method.MethodDescriptor;
import compiler.compilercontrol.share.method.MethodGenerator;
import compiler.compilercontrol.share.pool.PoolHelper;
import jdk.test.lib.util.Pair;

import java.lang.reflect.Executable;
import java.util.List;
import java.util.concurrent.Callable;

public abstract class AbstractTestBase {
    protected static final MethodGenerator METHOD_GEN = new MethodGenerator();
    protected static final int ATTEMPTS = 25;
    protected static final List<Pair<Executable, Callable<?>>> METHODS
            = new PoolHelper().getAllMethods(PoolHelper.METHOD_FILTER);

    public abstract void test();

    /**
     * Generate random valid method descriptor
     *
     * @param exec method to make descriptor for
     * @return a valid {@link MethodDescriptor#isValid()} descriptor instance
     */
    public static MethodDescriptor getValidMethodDescriptor(Executable exec) {
        MethodDescriptor md = METHOD_GEN.generateRandomDescriptor(exec);
        for (int i = 0; !md.isValid() && i < ATTEMPTS; i++) {
            md = METHOD_GEN.generateRandomDescriptor(exec);
        }
        if (!md.isValid() || "any.method()".matches(md.getRegexp())) {
            /* if we haven't got a valid pattern or it matches any method
               leading to timeouts, then use plain standard descriptor */
            md = MethodGenerator.commandDescriptor(exec);
        }
        return md;
    }
}
