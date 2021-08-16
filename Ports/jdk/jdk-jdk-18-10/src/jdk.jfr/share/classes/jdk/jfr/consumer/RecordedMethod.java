/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * A recorded method.
 *
 * @since 9
 */
public final class RecordedMethod extends RecordedObject {

    // package private
    RecordedMethod(ObjectContext objectContext, Object[] values) {
        super(objectContext, values);
    }

    /**
     * Returns the class this method belongs to, if it belong to a Java frame.
     * <p>
     * To ensure this is a Java frame, use the {@link RecordedFrame#isJavaFrame()}
     * method.
     *
     * @return the class, may be {@code null} if not a Java frame
     *
     * @see RecordedFrame#isJavaFrame()
     */
    public RecordedClass getType() {
        return getTyped("type", RecordedClass.class, null);
    }

    /**
     * Returns the name of this method, for example {@code "toString"}.
     * <p>
     * If this method doesn't belong to a Java frame the result is undefined.
     *
     * @return method name, or {@code null} if doesn't exist
     *
     * @see RecordedFrame#isJavaFrame()
     */
    public String getName() {
        return getTyped("name", String.class, null);
    }

    /**
     * Returns the method descriptor for this method (for example,
     * {@code "(Ljava/lang/String;)V"}).
     * <p>
     * If this method doesn't belong to a Java frame then the result is undefined.
     *
     * @return method descriptor
     *
     * @see RecordedFrame#isJavaFrame()
     * @jvms 4.3 Descriptors
     */
    public String getDescriptor() {
        return getTyped("descriptor", String.class, null);
    }

    /**
     * Returns the modifiers for this method.
     * <p>
     * If this method doesn't belong to a Java frame, then the result is undefined.
     *
     * @return the modifiers
     *
     * @see Modifier
     * @see RecordedFrame#isJavaFrame
     */
    public int getModifiers() {
        return getTyped("modifiers", Integer.class, Integer.valueOf(0));
    }

    /**
     * Returns whether this method is hidden (for example, wrapper code in a lambda
     * expressions).
     *
     * @return {@code true} if method is hidden, {@code false} otherwise
     */
    public boolean isHidden() {
        return getTyped("hidden", Boolean.class, Boolean.FALSE);
    }
}
