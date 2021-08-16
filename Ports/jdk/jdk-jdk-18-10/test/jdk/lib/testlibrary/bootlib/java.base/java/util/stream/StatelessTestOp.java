/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * The base type of a stateless test operation
 */
interface StatelessTestOp<E_IN, E_OUT> extends IntermediateTestOp<E_IN, E_OUT> {

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static<T> AbstractPipeline chain(AbstractPipeline upstream,
                                            StatelessTestOp<?, T> op) {
        int flags = op.opGetFlags();
        switch (op.outputShape()) {
            case REFERENCE:
                return new ReferencePipeline.StatelessOp<Object, T>(upstream, op.inputShape(), flags) {
                    public Sink opWrapSink(int flags, Sink<T> sink) {
                        return op.opWrapSink(flags, isParallel(), sink);
                    }
                };
            case INT_VALUE:
                return new IntPipeline.StatelessOp<Object>(upstream, op.inputShape(), flags) {
                    public Sink opWrapSink(int flags, Sink sink) {
                        return op.opWrapSink(flags, isParallel(), sink);
                    }
                };
            case LONG_VALUE:
                return new LongPipeline.StatelessOp<Object>(upstream, op.inputShape(), flags) {
                    @Override
                    Sink opWrapSink(int flags, Sink sink) {
                        return op.opWrapSink(flags, isParallel(), sink);
                    }
                };
            case DOUBLE_VALUE:
                return new DoublePipeline.StatelessOp<Object>(upstream, op.inputShape(), flags) {
                    @Override
                    Sink opWrapSink(int flags, Sink sink) {
                        return op.opWrapSink(flags, isParallel(), sink);
                    }
                };
            default: throw new IllegalStateException(op.outputShape().toString());
        }
    }

    default StreamShape inputShape() { return StreamShape.REFERENCE; }

    default StreamShape outputShape() { return StreamShape.REFERENCE; }

    default int opGetFlags() { return 0; }

    Sink<E_IN> opWrapSink(int flags, boolean parallel, Sink<E_OUT> sink);
}

