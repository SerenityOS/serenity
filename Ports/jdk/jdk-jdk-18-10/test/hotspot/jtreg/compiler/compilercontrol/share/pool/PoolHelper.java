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

package compiler.compilercontrol.share.pool;

import jdk.test.lib.util.Pair;

import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This is a helper class that provides tests with methods
 */
public class PoolHelper extends MethodHolder {
    private static final List<Pair<Executable, Callable<?>>> METHODS;

    /**
     * Filters only those methods who belong to Klass or its internal class
     * Internal and named as "method" or is a constructor
     */
    public static final Predicate<Executable> METHOD_FILTER = executable -> {
        String methodName = executable.getName();
        String className = executable.getDeclaringClass().getName();
        return className.matches(".*(Klass)(\\$Internal)?") &&
                (methodName.equals("method") ||
                        methodName.equals(className)); // if method is <init>
    };

    static {
        METHODS = new ArrayList<>();
        List<MethodHolder> holders = new ArrayList<>();
        holders.add(new compiler.compilercontrol.share.pool.sub.Klass());
        holders.add(new compiler.compilercontrol.share.pool.sub.KlassDup());
        holders.add(new compiler.compilercontrol.share.pool.subpack.Klass());
        holders.add(new compiler.compilercontrol.share.pool.subpack.KlassDup());
        holders.add(new compiler.compilercontrol.share.pool.sub.Klass.Internal());
        holders.add(new compiler.compilercontrol.share.pool.subpack.KlassDup.Internal());
        for (MethodHolder holder : holders) {
            METHODS.addAll(holder.getAllMethods());
        }
    }

    /**
     * Gets all methods from the pool using specified filter
     *
     * @param filter method filter
     * @return pairs of Executable and appropriate Callable
     */
    public List<Pair<Executable, Callable<?>>> getAllMethods(
            Predicate<Executable> filter) {
        return getAllMethods().stream()
                .filter(pair -> filter.test(pair.first))
                .collect(Collectors.toList());
    }

    /**
     * Gets all methods from the pool
     *
     * @return pairs of Executable and appropriate Callable
     */
    @Override
    public List<Pair<Executable, Callable<?>>> getAllMethods() {
        return METHODS;
    }
}
