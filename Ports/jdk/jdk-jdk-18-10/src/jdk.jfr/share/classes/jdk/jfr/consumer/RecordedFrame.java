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

import java.lang.reflect.Modifier;

import jdk.jfr.internal.consumer.ObjectContext;

/**
 * A recorded frame in a stack trace.
 *
 * @since 9
 */
public final class RecordedFrame extends RecordedObject {
    // package private
    RecordedFrame(ObjectContext objectContext, Object[] values) {
        super(objectContext, values);
    }

    /**
     * Returns {@code true} if this is a Java frame, {@code false} otherwise.
     * <p>
     * A Java method that has a native modifier is considered a Java frame.
     *
     * @return {@code true} if this is a Java frame, {@code false} otherwise
     *
     * @see Modifier#isNative(int)
     */
    public boolean isJavaFrame() {
        // Only Java frames exist today, but this allows
        // API to be extended for native frame in the future.
        if (hasField("javaFrame")) {
            return getTyped("javaFrame", Boolean.class, Boolean.TRUE);
        }
        return true;
    }

    /**
     * Returns the bytecode index for the execution point that is represented by
     * this recorded frame.
     *
     * @return byte code index, or {@code -1} if doesn't exist
     */
    public int getBytecodeIndex() {
        return getTyped("bytecodeIndex", Integer.class, Integer.valueOf(-1));
    }

    /**
     * Returns the line number for the execution point that is represented by this
     * recorded frame, or {@code -1} if doesn't exist
     *
     * @return the line number, or {@code -1} if doesn't exist
     */
    public int getLineNumber() {
        return getTyped("lineNumber", Integer.class, Integer.valueOf(-1));
    }

    /**
     * Returns the frame type for the execution point that is represented by this
     * recorded frame (for example, {@code "Interpreted"}, {@code "JIT compiled"} or
     * {@code "Inlined"}).
     *
     * @return the frame type, or {@code null} if doesn't exist
     */
    public String getType() {
        return getTyped("type", String.class, null);
    }

    /**
     * Returns the method for the execution point that is represented by this
     * recorded frame.
     *
     * @return the method, not {@code null}
     */
    public RecordedMethod getMethod() {
        return getTyped("method", RecordedMethod.class, null);
    }
}
