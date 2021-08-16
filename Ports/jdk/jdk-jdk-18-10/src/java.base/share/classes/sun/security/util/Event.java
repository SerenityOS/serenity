/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.util;

/**
 * This class implements an event model with services for reporter and listener.
 * Reporter uses report() method to generate an event.
 * Listener uses setReportListener() to register for listening to an event,
 * and uses clearReportListener() to unregister a listening session.
 * Listener should implement the event handling of the Reporter interface.
 */
public final class Event {
    private Event() {}

    public enum ReporterCategory {
        CRLCHECK(),
        ZIPFILEATTRS();

        private Reporter reporter;
    }

    public interface Reporter {
        public void handle(String type, Object... args);
    }

    public static void setReportListener(ReporterCategory cat, Reporter re) {
        cat.reporter = re;
    }

    public static void clearReportListener(ReporterCategory cat) {
        cat.reporter = null;
    }

    public static void report(ReporterCategory cat, String type, Object... args) {
        Reporter currentReporter = cat.reporter;

        if (currentReporter != null) {
            currentReporter.handle(type, args);
        }
    }
}
