/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Set;
import java.util.Locale;
import javax.tools.Diagnostic;

import com.sun.tools.javac.api.DiagnosticFormatter;
import com.sun.tools.javac.api.DiagnosticFormatter.Configuration;
import com.sun.tools.javac.api.DiagnosticFormatter.Configuration.DiagnosticPart;
import com.sun.tools.javac.api.DiagnosticFormatter.Configuration.MultilineLimit;
import com.sun.tools.javac.api.DiagnosticFormatter.PositionKind;

/**
 * A delegated diagnostic formatter delegates all formatting
 * actions to an underlying formatter (aka the delegated formatter).
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */
public class ForwardingDiagnosticFormatter<D extends Diagnostic<?>, F extends DiagnosticFormatter<D>>
        implements DiagnosticFormatter<D> {

    /**
     * The delegated formatter
     */
    protected F formatter;

    /*
     * configuration object used by this formatter
     */
    protected ForwardingConfiguration configuration;

    public ForwardingDiagnosticFormatter(F formatter) {
        this.formatter = formatter;
        this.configuration = new ForwardingConfiguration(formatter.getConfiguration());
    }

    /**
     * Returns the underlying delegated formatter
     * @return delegate formatter
     */
    public F getDelegatedFormatter() {
        return formatter;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public boolean displaySource(D diag) {
        return formatter.displaySource(diag);
    }

    public String format(D diag, Locale l) {
        return formatter.format(diag, l);
    }

    public String formatKind(D diag, Locale l) {
        return formatter.formatKind(diag, l);
    }

    public String formatMessage(D diag, Locale l) {
        return formatter.formatMessage(diag, l);
    }

    public String formatPosition(D diag, PositionKind pk, Locale l) {
        return formatter.formatPosition(diag, pk, l);
    }

    public String formatSource(D diag, boolean fullname, Locale l) {
        return formatter.formatSource(diag, fullname, l);
    }

    /**
     * A delegated formatter configuration delegates all configurations settings
     * to an underlying configuration object (aka the delegated configuration).
     */
    public static class ForwardingConfiguration implements DiagnosticFormatter.Configuration {

        /** The configuration object to which the forwarding configuration delegates some settings */
        protected Configuration configuration;

        public ForwardingConfiguration(Configuration configuration) {
            this.configuration = configuration;
        }

        /**
         * Returns the underlying delegated configuration.
         * @return delegated configuration
         */
        public Configuration getDelegatedConfiguration() {
            return configuration;
        }

        public int getMultilineLimit(MultilineLimit limit) {
            return configuration.getMultilineLimit(limit);
        }

        public Set<DiagnosticPart> getVisible() {
            return configuration.getVisible();
        }

        public void setMultilineLimit(MultilineLimit limit, int value) {
            configuration.setMultilineLimit(limit, value);
        }

        public void setVisible(Set<DiagnosticPart> diagParts) {
            configuration.setVisible(diagParts);
        }
    }
}
