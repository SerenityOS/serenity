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
 * A recorded Java type, such as a class or an interface.
 *
 * @since 9
 */
public final class RecordedClass extends RecordedObject {
    private final long uniqueId;

    // package private
    RecordedClass(ObjectContext objectContext, long id, Object[] values) {
        super(objectContext, values);
        this.uniqueId = id;
    }

    /**
     * Returns the modifiers of the class.
     *
     * @return the modifiers
     *
     * @see java.lang.reflect.Modifier
     */
    public int getModifiers() {
        return getTyped("modifiers", Integer.class, -1);
    }

    /**
     * Returns the class loader that defined the class.
     * <p>
     * If the bootstrap class loader is represented as {@code null} in the Java
     * Virtual Machine (JVM), then {@code null} is also the return value of this method.
     *
     * @return the class loader defining this class, can be {@code null}
     */
    public RecordedClassLoader getClassLoader() {
        return getTyped("classLoader", RecordedClassLoader.class, null);
    }

    /**
     * Returns the fully qualified name of the class (for example,
     * {@code "java.lang.String"}).
     *
     * @return the class name, not {@code null}
     */
    public String getName() {
        return getTyped("name", String.class, null).replace("/", ".");
    }

    /**
     * Returns a unique ID for the class.
     * <p>
     * The ID might not be the same between Java Virtual Machine (JVM) instances.
     *
     * @return a unique ID
     */
    public long getId() {
        return uniqueId;
    }
}
