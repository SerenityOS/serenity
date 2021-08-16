/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
package java.util.stream;

/**
 * An operation that injects or clears flags but otherwise performs no operation on elements.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class FlagDeclaringOp<T> implements StatelessTestOp<T, T> {
    private final int flags;
    private final StreamShape shape;

    public FlagDeclaringOp(int flags) {
        this(flags, StreamShape.REFERENCE);
    }

    public FlagDeclaringOp(int flags, StreamShape shape) {
        this.flags = flags;
        this.shape = shape;
    }

    @Override
    public StreamShape outputShape() {
        return shape;
    }

    @Override
    public StreamShape inputShape() {
        return shape;
    }

    @Override
    public int opGetFlags() {
        return flags;
    }

    @Override
    public Sink<T> opWrapSink(int flags, boolean parallel, Sink sink) {
        return sink;
    }
}
