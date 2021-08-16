/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Launched by the test ServiceBinding to test modules in the boot layer.
 */

public class TestBootLayer {
    public static void main(String[] args) throws Exception {
        Pattern splitter = Pattern.compile(",");

        // the names of all modules in the boot layer
        Set<String> modules = ModuleLayer.boot().modules().stream()
                .map(Module::getName)
                .collect(Collectors.toSet());

        // throw exception if an expected module is not in the boot layer
        splitter.splitAsStream(args[0])
                .filter(Predicate.not(String::isEmpty))
                .filter(mn -> !modules.contains(mn))
                .findAny()
                .ifPresent(mn -> {
                    throw new RuntimeException(mn + " not in boot layer!!!");
                });

        // throw exception if an unexpected module is in the boot layer
        splitter.splitAsStream(args[1])
                .filter(Predicate.not(String::isEmpty))
                .filter(mn -> modules.contains(mn))
                .findAny()
                .ifPresent(mn -> {
                    throw new RuntimeException(mn + " in boot layer!!!!");
                });
    }
}