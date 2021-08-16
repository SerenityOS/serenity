/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package p1;

import java.lang.module.ModuleFinder;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This tests if JAVA_HOME is linked only with the specified modules.
 */
public class Main {
    public static void main(String... args) {
        Set<String> modules = ModuleFinder.ofSystem().findAll().stream()
            .map(mref -> mref.descriptor().name())
            .filter(mn -> !mn.equals("java.base"))
            .collect(Collectors.toSet());

        Set<String> notLinked = Stream.of(args).filter(mn -> !modules.contains(mn))
                                      .collect(Collectors.toSet());
        if (!notLinked.isEmpty()) {
            throw new RuntimeException("Expected modules not linked in the image: "
                + notLinked);
        }
        Stream.of(args).forEach(modules::remove);

        if (!modules.isEmpty()) {
            throw new RuntimeException("Unexpected modules linked in the image: "
                + modules);
        }
    }
}
