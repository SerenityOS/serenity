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

package com.sun.tools.doclint;

import java.util.ServiceLoader;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;

/**
 * The base class for the DocLint service used by javac.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */
public abstract class DocLint implements Plugin {
    public static final String XMSGS_OPTION = "-Xmsgs";
    public static final String XMSGS_CUSTOM_PREFIX = "-Xmsgs:";
    public static final String XCHECK_PACKAGE = "-XcheckPackage:";

    private static ServiceLoader.Provider<DocLint> docLintProvider;

    public abstract boolean isValidOption(String opt);

    public static synchronized DocLint newDocLint() {
        if (docLintProvider == null) {
            docLintProvider = ServiceLoader.load(DocLint.class, ClassLoader.getSystemClassLoader()).stream()
                    .filter(p_ -> p_.get().getName().equals("doclint"))
                    .findFirst()
                    .orElse(new ServiceLoader.Provider<>() {
                        @Override
                        public Class<? extends DocLint> type() {
                            return NoDocLint.class;
                        }

                        @Override
                        public DocLint get() {
                            return new NoDocLint();
                        }
                    });
        }
        return docLintProvider.get();
    }

    private static class NoDocLint extends DocLint {
        @Override
        public String getName() {
            return "doclint-not-available";
        }

        @Override
        public void init(JavacTask task, String... args) {
            throw new IllegalStateException("doclint not available");
        }

        @Override
        public boolean isValidOption(String s) {
            // passively accept all "plausible" options
            return s.equals(XMSGS_OPTION)
                    || s.startsWith(XMSGS_CUSTOM_PREFIX)
                    || s.startsWith(XCHECK_PACKAGE);
        }
    }
}
