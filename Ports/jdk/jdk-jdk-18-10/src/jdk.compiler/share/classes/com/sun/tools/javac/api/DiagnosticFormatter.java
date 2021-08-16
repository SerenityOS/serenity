/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.api;

import java.util.Locale;
import java.util.Set;
import javax.tools.Diagnostic;
import com.sun.tools.javac.api.DiagnosticFormatter.*;

/**
 * Provides simple functionalities for javac diagnostic formatting.
 * @param <D> type of diagnostic handled by this formatter
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */
public interface DiagnosticFormatter<D extends Diagnostic<?>> {

    /**
     * Whether the source code output for this diagnostic is to be displayed.
     *
     * @param diag diagnostic to be formatted
     * @return true if the source line this diagnostic refers to is to be displayed
     */
    boolean displaySource(D diag);

    /**
     * Format the contents of a diagnostics.
     *
     * @param diag the diagnostic to be formatted
     * @param l locale object to be used for i18n
     * @return a string representing the diagnostic
     */
    public String format(D diag, Locale l);

    /**
     * Controls the way in which a diagnostic message is displayed.
     *
     * @param diag diagnostic to be formatted
     * @param l locale object to be used for i18n
     * @return string representation of the diagnostic message
     */
    public String formatMessage(D diag,Locale l);

    /**
     * Controls the way in which a diagnostic kind is displayed.
     *
     * @param diag diagnostic to be formatted
     * @param l locale object to be used for i18n
     * @return string representation of the diagnostic prefix
     */
    public String formatKind(D diag, Locale l);

    /**
     * Controls the way in which a diagnostic source is displayed.
     *
     * @param diag diagnostic to be formatted
     * @param l locale object to be used for i18n
     * @param fullname whether the source fullname should be printed
     * @return string representation of the diagnostic source
     */
    public String formatSource(D diag, boolean fullname, Locale l);

    /**
     * Controls the way in which a diagnostic position is displayed.
     *
     * @param diag diagnostic to be formatted
     * @param pk enum constant representing the position kind
     * @param l locale object to be used for i18n
     * @return string representation of the diagnostic position
     */
    public String formatPosition(D diag, PositionKind pk, Locale l);
    //where
    /**
     * This enum defines a set of constants for all the kinds of position
     * that a diagnostic can be asked for. All positions are intended to be
     * relative to a given diagnostic source.
     */
    public enum PositionKind {
        /**
         * Start position
         */
        START,
        /**
         * End position
         */
        END,
        /**
         * Line number
         */
        LINE,
        /**
         * Column number
         */
        COLUMN,
        /**
         * Offset position
         */
        OFFSET
    }

    /**
     * Get a list of all the enabled verbosity options.
     * @return verbosity options
     */
    public Configuration getConfiguration();
    //where

    /**
     * This interface provides functionalities for tuning the output of a
     * diagnostic formatter in multiple ways.
     */
    interface Configuration {
        /**
         * Configure the set of diagnostic parts that should be displayed
         * by the formatter.
         * @param visibleParts the parts to be set
         */
        public void setVisible(Set<DiagnosticPart> visibleParts);

        /**
         * Retrieve the set of diagnostic parts that should be displayed
         * by the formatter.
         * @return verbosity options
         */
        public Set<DiagnosticPart> getVisible();

        //where
        /**
         * A given diagnostic message can be divided into sub-parts each of which
         * might/might not be displayed by the formatter, according to the
         * current configuration settings.
         */
        public enum DiagnosticPart {
            /**
             * Short description of the diagnostic - usually one line long.
             */
            SUMMARY,
            /**
             * Longer description that provides additional details w.r.t. the ones
             * in the diagnostic's description.
             */
            DETAILS,
            /**
             * Source line the diagnostic refers to (if applicable).
             */
            SOURCE,
            /**
             * Subdiagnostics attached to a given multiline diagnostic.
             */
            SUBDIAGNOSTICS,
            /**
             * JLS paragraph this diagnostic might refer to (if applicable).
             */
            JLS
        }

        /**
         * Set a limit for multiline diagnostics.
         * Note: Setting a limit has no effect if multiline diagnostics are either
         * fully enabled or disabled.
         *
         * @param limit the kind of limit to be set
         * @param value the limit value
         */
        public void setMultilineLimit(MultilineLimit limit, int value);

        /**
         * Get a multiline diagnostic limit.
         *
         * @param limit the kind of limit to be retrieved
         * @return limit value or -1 if no limit is set
         */
        public int getMultilineLimit(MultilineLimit limit);
        //where
        /**
         * A multiline limit control the verbosity of multiline diagnostics
         * either by setting a maximum depth of nested multidiagnostics,
         * or by limiting the amount of subdiagnostics attached to a given
         * diagnostic (or both).
         */
        public enum MultilineLimit {
            /**
             * Controls the maximum depth of nested multiline diagnostics.
             */
            DEPTH,
            /**
             * Controls the maximum amount of subdiagnostics that are part of a
             * given multiline diagnostic.
             */
            LENGTH
        }
    }
}
