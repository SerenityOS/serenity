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

import org.testng.Assert;

import java.util.EnumSet;

class TestFlagExpectedOp<T> extends FlagDeclaringOp<T> {

    static class Builder<T> {
        final int flags;
        StreamShape shape = StreamShape.REFERENCE;

        EnumSet<StreamOpFlag> known = EnumSet.noneOf(StreamOpFlag.class);
        EnumSet<StreamOpFlag> preserve = EnumSet.noneOf(StreamOpFlag.class);
        EnumSet<StreamOpFlag> notKnown = EnumSet.noneOf(StreamOpFlag.class);

        Builder(int flags) {
            this.flags = flags;
        }

        Builder<T> known(EnumSet<StreamOpFlag> known) {
            this.known = known;
            return this;
        }

        Builder<T> preserve(EnumSet<StreamOpFlag> preserve) {
            this.preserve = preserve;
            return this;
        }

        Builder<T> notKnown(EnumSet<StreamOpFlag> notKnown) {
            this.notKnown = notKnown;
            return this;
        }

        Builder<T> shape(StreamShape shape) {
            this.shape = shape;
            return this;
        }

        TestFlagExpectedOp<T> build() {
            return new TestFlagExpectedOp<>(flags, known, preserve, notKnown, shape);
        }
    }

    final EnumSet<StreamOpFlag> known;
    final EnumSet<StreamOpFlag> preserve;
    final EnumSet<StreamOpFlag> notKnown;
    final StreamShape shape;

    TestFlagExpectedOp(int flags,
                       EnumSet<StreamOpFlag> known,
                       EnumSet<StreamOpFlag> preserve,
                       EnumSet<StreamOpFlag> notKnown) {
        this(flags, known, preserve, notKnown, StreamShape.REFERENCE);
    }

    TestFlagExpectedOp(int flags,
                       EnumSet<StreamOpFlag> known,
                       EnumSet<StreamOpFlag> preserve,
                       EnumSet<StreamOpFlag> notKnown,
                       StreamShape shape) {
        super(flags);
        this.known = known;
        this.preserve = preserve;
        this.notKnown = notKnown;
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
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Sink<T> opWrapSink(int flags, boolean parallel, Sink upstream) {
        assertFlags(flags);
        return upstream;
    }

    private void assertFlags(int flags) {
        for (StreamOpFlag f : known) {
            Assert.assertTrue(f.isKnown(flags),
                              String.format("Flag %s is not known, but should be known.", f.toString()));
        }

        for (StreamOpFlag f : preserve) {
            Assert.assertTrue(f.isPreserved(flags),
                              String.format("Flag %s is not preserved, but should be preserved.", f.toString()));
        }

        for (StreamOpFlag f : notKnown) {
            Assert.assertFalse(f.isKnown(flags),
                               String.format("Flag %s is known, but should be not known.", f.toString()));
        }
    }
}
