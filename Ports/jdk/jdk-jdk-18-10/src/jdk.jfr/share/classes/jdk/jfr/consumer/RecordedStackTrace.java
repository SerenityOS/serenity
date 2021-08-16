/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.jfr.consumer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jdk.jfr.internal.consumer.ObjectContext;

/**
 * A recorded stack trace.
 *
 * @since 9
 */
public final class RecordedStackTrace extends RecordedObject {
    // package private
    RecordedStackTrace(ObjectContext objectContext, Object[] values) {
        super(objectContext, values);
    }

    /**
     * Returns the frames in the stack trace.
     *
     * @return a list of Java stack frames, not {@code null}
     */
    @SuppressWarnings("unchecked")
    public List<RecordedFrame> getFrames() {
        Object[] array = getTyped("frames", Object[].class, null);
        if (array == null) {
            return new ArrayList<>(0);
        }
        List<?> list = Arrays.asList(array);
        return (List<RecordedFrame>) list;
    }

    /**
     * Returns {@code true} if the stack trace is truncated due to its size,
     * {@code false} otherwise.
     *
     * @return {@code true} if the stack trace is truncated, {@code false}
     *         otherwise
     */
    public boolean isTruncated() {
        return getTyped("truncated", Boolean.class, true);
    }
}
