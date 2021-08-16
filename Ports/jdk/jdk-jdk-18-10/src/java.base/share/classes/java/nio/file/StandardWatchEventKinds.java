/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.file;

/**
 * Defines the <em>standard</em> event kinds.
 *
 * @since 1.7
 */

public final class StandardWatchEventKinds {
    private StandardWatchEventKinds() { }

    /**
     * A special event to indicate that events may have been lost or
     * discarded.
     *
     * <p> The {@link WatchEvent#context context} for this event is
     * implementation specific and may be {@code null}. The event {@link
     * WatchEvent#count count} may be greater than {@code 1}.
     *
     * @see WatchService
     */
    public static final WatchEvent.Kind<Object> OVERFLOW =
        new StdWatchEventKind<Object>("OVERFLOW", Object.class);

    /**
     * Directory entry created.
     *
     * <p> When a directory is registered for this event then the {@link WatchKey}
     * is queued when it is observed that an entry is created in the directory
     * or renamed into the directory. The event {@link WatchEvent#count count}
     * for this event is always {@code 1}.
     */
    public static final WatchEvent.Kind<Path> ENTRY_CREATE =
        new StdWatchEventKind<Path>("ENTRY_CREATE", Path.class);

    /**
     * Directory entry deleted.
     *
     * <p> When a directory is registered for this event then the {@link WatchKey}
     * is queued when it is observed that an entry is deleted or renamed out of
     * the directory. The event {@link WatchEvent#count count} for this event
     * is always {@code 1}.
     */
    public static final WatchEvent.Kind<Path> ENTRY_DELETE =
        new StdWatchEventKind<Path>("ENTRY_DELETE", Path.class);

    /**
     * Directory entry modified.
     *
     * <p> When a directory is registered for this event then the {@link WatchKey}
     * is queued when it is observed that an entry in the directory has been
     * modified. The event {@link WatchEvent#count count} for this event is
     * {@code 1} or greater.
     */
    public static final WatchEvent.Kind<Path> ENTRY_MODIFY =
        new StdWatchEventKind<Path>("ENTRY_MODIFY", Path.class);

    private static class StdWatchEventKind<T> implements WatchEvent.Kind<T> {
        private final String name;
        private final Class<T> type;
        StdWatchEventKind(String name, Class<T> type) {
            this.name = name;
            this.type = type;
        }
        @Override public String name() { return name; }
        @Override public Class<T> type() { return type; }
        @Override public String toString() { return name; }
    }
}
