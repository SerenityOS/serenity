/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.util;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.tools.JavaFileObject;

import com.sun.tools.javac.code.Lint.LintCategory;
import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.JCDiagnostic.Note;
import com.sun.tools.javac.util.JCDiagnostic.Warning;


/**
 * A handler to process mandatory warnings, setting up a deferred diagnostic
 * to be printed at the end of the compilation if some warnings get suppressed
 * because too many warnings have already been generated.
 *
 * Note that the SuppressWarnings annotation can be used to suppress warnings
 * about conditions that would otherwise merit a warning. Such processing
 * is done when the condition is detected, and in those cases, no call is
 * made on any API to generate a warning at all. In consequence, this handler only
 * Returns to handle those warnings that JLS says must be generated.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class MandatoryWarningHandler {

    /**
     * The kinds of different deferred diagnostics that might be generated
     * if a mandatory warning is suppressed because too many warnings have
     * already been output.
     *
     * The parameter is a fragment used to build an I18N message key for Log.
     */
    private enum DeferredDiagnosticKind {
        /**
         * This kind is used when a single specific file is found to have warnings
         * and no similar warnings have already been given.
         * It generates a message like:
         *      FILE has ISSUES
         */
        IN_FILE(".filename"),
        /**
         * This kind is used when a single specific file is found to have warnings
         * and when similar warnings have already been reported for the file.
         * It generates a message like:
         *      FILE has additional ISSUES
         */
        ADDITIONAL_IN_FILE(".filename.additional"),
        /**
         * This kind is used when multiple files have been found to have warnings,
         * and none of them have had any similar warnings.
         * It generates a message like:
         *      Some files have ISSUES
         */
        IN_FILES(".plural"),
        /**
         * This kind is used when multiple files have been found to have warnings,
         * and some of them have had already had specific similar warnings.
         * It generates a message like:
         *      Some files have additional ISSUES
         */
        ADDITIONAL_IN_FILES(".plural.additional");

        DeferredDiagnosticKind(String v) { value = v; }
        String getKey(String prefix) { return prefix + value; }

        private final String value;
    }


    /**
     * Create a handler for mandatory warnings.
     * @param log     The log on which to generate any diagnostics
     * @param verbose Specify whether or not detailed messages about
     *                individual instances should be given, or whether an aggregate
     *                message should be generated at the end of the compilation.
     *                Typically set via  -Xlint:option.
     * @param enforceMandatory
     *                True if mandatory warnings and notes are being enforced.
     * @param prefix  A common prefix for the set of message keys for
     *                the messages that may be generated.
     * @param lc      An associated lint category for the warnings, or null if none.
     */
    public MandatoryWarningHandler(Log log, Source source, boolean verbose,
                                   boolean enforceMandatory, String prefix,
                                   LintCategory lc) {
        this.log = log;
        this.source = source;
        this.verbose = verbose;
        this.prefix = prefix;
        this.enforceMandatory = enforceMandatory;
        this.lintCategory = lc;
    }

    /**
     * Report a mandatory warning.
     */
    public void report(DiagnosticPosition pos, Warning warnKey) {
        JavaFileObject currentSource = log.currentSourceFile();

        if (verbose) {
            if (sourcesWithReportedWarnings == null)
                sourcesWithReportedWarnings = new HashSet<>();

            if (log.nwarnings < log.MaxWarnings) {
                // generate message and remember the source file
                logMandatoryWarning(pos, warnKey);
                sourcesWithReportedWarnings.add(currentSource);
            } else if (deferredDiagnosticKind == null) {
                // set up deferred message
                if (sourcesWithReportedWarnings.contains(currentSource)) {
                    // more errors in a file that already has reported warnings
                    deferredDiagnosticKind = DeferredDiagnosticKind.ADDITIONAL_IN_FILE;
                } else {
                    // warnings in a new source file
                    deferredDiagnosticKind = DeferredDiagnosticKind.IN_FILE;
                }
                deferredDiagnosticSource = currentSource;
                deferredDiagnosticArg = currentSource;
            } else if ((deferredDiagnosticKind == DeferredDiagnosticKind.IN_FILE
                        || deferredDiagnosticKind == DeferredDiagnosticKind.ADDITIONAL_IN_FILE)
                       && !Objects.equals(deferredDiagnosticSource, currentSource)) {
                // additional errors in more than one source file
                deferredDiagnosticKind = DeferredDiagnosticKind.ADDITIONAL_IN_FILES;
                deferredDiagnosticArg = null;
            }
        } else {
            if (deferredDiagnosticKind == null) {
                // warnings in a single source
                deferredDiagnosticKind = DeferredDiagnosticKind.IN_FILE;
                deferredDiagnosticSource = currentSource;
                deferredDiagnosticArg = currentSource;
            }  else if (deferredDiagnosticKind == DeferredDiagnosticKind.IN_FILE &&
                        !Objects.equals(deferredDiagnosticSource, currentSource)) {
                // warnings in multiple source files
                deferredDiagnosticKind = DeferredDiagnosticKind.IN_FILES;
                deferredDiagnosticArg = null;
            }
        }
    }

    /**
     * Report any diagnostic that might have been deferred by previous calls of report().
     */
    public void reportDeferredDiagnostic() {
        if (deferredDiagnosticKind != null) {
            if (deferredDiagnosticArg == null) {
                if (source != null) {
                    logMandatoryNote(deferredDiagnosticSource, deferredDiagnosticKind.getKey(prefix), source);
                } else {
                    logMandatoryNote(deferredDiagnosticSource, deferredDiagnosticKind.getKey(prefix));
                }
            } else {
                if (source != null) {
                    logMandatoryNote(deferredDiagnosticSource, deferredDiagnosticKind.getKey(prefix), deferredDiagnosticArg, source);
                } else {
                    logMandatoryNote(deferredDiagnosticSource, deferredDiagnosticKind.getKey(prefix), deferredDiagnosticArg);
                }
            }

            if (!verbose)
                logMandatoryNote(deferredDiagnosticSource, prefix + ".recompile");
        }
    }

    /**
     * The log to which to report warnings.
     */
    private final Log log;
    private final Source source;

    /**
     * Whether or not to report individual warnings, or simply to report a
     * single aggregate warning at the end of the compilation.
     */
    private final boolean verbose;

    /**
     * The common prefix for all I18N message keys generated by this handler.
     */
    private final String prefix;

    /**
     * A set containing the names of the source files for which specific
     * warnings have been generated -- i.e. in verbose mode.  If a source name
     * appears in this list, then deferred diagnostics will be phrased to
     * include "additionally"...
     */
    private Set<JavaFileObject> sourcesWithReportedWarnings;

    /**
     * A variable indicating the latest best guess at what the final
     * deferred diagnostic will be. Initially as specific and helpful
     * as possible, as more warnings are reported, the scope of the
     * diagnostic will be broadened.
     */
    private DeferredDiagnosticKind deferredDiagnosticKind;

    /**
     * If deferredDiagnosticKind is IN_FILE or ADDITIONAL_IN_FILE, this variable
     * gives the value of log.currentSource() for the file in question.
     */
    private JavaFileObject deferredDiagnosticSource;

    /**
     * An optional argument to be used when constructing the
     * deferred diagnostic message, based on deferredDiagnosticKind.
     * This variable should normally be set/updated whenever
     * deferredDiagnosticKind is updated.
     */
    private Object deferredDiagnosticArg;

    /**
     * True if mandatory warnings and notes are being enforced.
     */
    private final boolean enforceMandatory;

    /**
     * A LintCategory to be included in point-of-use diagnostics to indicate
     * how messages might be suppressed (i.e. with @SuppressWarnings).
     */
    private final LintCategory lintCategory;

    /**
     * Reports a mandatory warning to the log.  If mandatory warnings
     * are not being enforced, treat this as an ordinary warning.
     */
    private void logMandatoryWarning(DiagnosticPosition pos, Warning warnKey) {
        // Note: the following log methods are safe if lintCategory is null.
        if (enforceMandatory)
            log.mandatoryWarning(lintCategory, pos, warnKey);
        else
            log.warning(lintCategory, pos, warnKey);
    }

    /**
     * Reports a mandatory note to the log.  If mandatory notes are
     * not being enforced, treat this as an ordinary note.
     */
    private void logMandatoryNote(JavaFileObject file, String msg, Object... args) {
        if (enforceMandatory)
            log.mandatoryNote(file, new Note("compiler", msg, args));
        else
            log.note(file, new Note("compiler", msg, args));
    }

    public void clear() {
        sourcesWithReportedWarnings = null;
        deferredDiagnosticKind = null;
        deferredDiagnosticSource = null;
        deferredDiagnosticArg = null;
    }
}
