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
package jdk.jpackage.test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandArguments<T> {

    CommandArguments() {
        args = new ArrayList<>();
    }

    final public T addArgument(String v) {
        args.add(v);
        return (T) this;
    }

    final public T addArguments(List<String> v) {
        args.addAll(v);
        return (T) this;
    }

    final public T addArgument(Path v) {
        return addArgument(v.toString());
    }

    final public T addArguments(String... v) {
        return addArguments(Arrays.asList(v));
    }

    final public T addPathArguments(List<Path> v) {
        return addArguments(v.stream().map((p) -> p.toString()).collect(
                Collectors.toList()));
    }

    final public List<String> getAllArguments() {
        return List.copyOf(args);
    }

    protected void verifyMutable() {
        if (!isMutable()) {
            throw new UnsupportedOperationException(
                    "Attempt to modify immutable object");
        }
    }

    protected boolean isMutable() {
        return true;
    }

    protected List<String> args;
}
