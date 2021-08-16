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

package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that sets the default name for an element.
 * <p>
 * The name must be a valid identifier as specified in the Java language (for
 * example, {@code "com.example.Transaction"} for an event class or
 * {@code "message"} for an event field).
 * <p>
 * A stable and easy-to-use event name is of the form:
 * <p>
 * {@code [org|com|net].[organization|product].EventName}
 * <p>
 * Events without a {@code @Name} annotation get their name from the fully
 * qualified class name, which works well for experimentation but should be
 * avoided in production.
 * <ul>
 * <li>The name should be stable to avoid breaking setting files and code that
 * consumes or configures the event.</li>
 * <li>The name should not contain redundant or unnecessary information such as
 * {@code "jfr"}, {@code "internal"}, {@code "events"}, or {@code "Event"}.</lI>
 * <li>The name should be short, but not so short that it clashes with other
 * organizations or products.</li>
 * <li>The name should be easy to understand and remember for users that want to
 * configure the event. This is especially true if the event is part of a
 * framework or library that is meant to be used by others. It is usually enough
 * to put all the events for a library or product in the same namespace. For
 * example, all the events for OpenJDK are in the {@code "jdk"} namespace, with
 * no sub-namespaces for {@code "hotspot"}, {@code "gc"}, or {@code "compiler"}.
 * This avoids unnecessary cognitive load for users. Events can instead be
 * arranged into categories, by using the {@code @Category} annotation.
 * Categories can be renamed freely without causing disruption to
 * dependencies</li>
 * </ul>
 * @since 9
 */
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@MetadataDefinition
public @interface Name {
    /**
     * Returns the name.
     *
     * @return the name
     */
    String value();
}
