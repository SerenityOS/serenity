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

import jdk.jfr.internal.consumer.ObjectContext;

/**
 * A recorded thread.
 *
 * @since 9
 */
public final class RecordedThread extends RecordedObject {
    private final long uniqueId;

    // package private
    RecordedThread(ObjectContext objectContext, long id, Object[] values) {
        super(objectContext, values);
        this.uniqueId = id;
    }

    /**
     * Returns the thread name used by the operating system.
     *
     * @return the OS thread name, or {@code null} if doesn't exist
     */
    public String getOSName() {
        return getTyped("osName", String.class, null);
    }

    /**
     * Returns the thread ID used by the operating system.
     *
     * @return The Java thread ID, or {@code -1} if doesn't exist
     */
    public long getOSThreadId() {
        Long l = getTyped("osThreadId", Long.class, -1L);
        return l.longValue();
    }

    /**
     * Returns the Java thread group, if available.
     *
     * @return the thread group, or {@code null} if doesn't exist
     */
    public RecordedThreadGroup getThreadGroup() {
        return getTyped("group", RecordedThreadGroup.class, null);
    }

    /**
     * Returns the Java thread name, or {@code null} if doesn't exist.
     * <p>
     * Returns {@code java.lang.Thread.getName()} if the thread has a Java
     * representation. {@code null} otherwise.
     *
     * @return the Java thread name, or {@code null} if doesn't exist
     */
    public String getJavaName() {
        return getTyped("javaName", String.class, null);
    }

    /**
     * Returns the Java thread ID, or {@code -1} if it's not a Java thread.
     *
     * @return the Java thread ID, or {@code -1} if it's not a Java thread
     */
    public long getJavaThreadId() {
        Long l = getTyped("javaThreadId", Long.class, -1L);
        return l.longValue();
    }

    /**
     * Returns a unique ID for both native threads and Java threads that can't be
     * reused within the lifespan of the JVM.
     * <p>
     * See {@link #getJavaThreadId()} for the ID that is returned by
     * {@code java.lang.Thread.getId()}
     *
     * @return a unique ID for the thread
     */
    public long getId() {
        return uniqueId;
    }
}
