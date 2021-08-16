/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javax.security.sasl;

import javax.security.auth.callback.TextInputCallback;

/**
 * This callback is used by {@code SaslClient} and {@code SaslServer}
 * to retrieve realm information.
 *
 * @since 1.5
 *
 * @author Rosanna Lee
 * @author Rob Weltman
 */
public class RealmCallback extends TextInputCallback {

    /**
     * Constructs a {@code RealmCallback} with a prompt.
     *
     * @param prompt The non-null prompt to use to request the realm information.
     * @throws IllegalArgumentException If {@code prompt} is null or
     * the empty string.
     */
    public RealmCallback(String prompt) {
        super(prompt);
    }

    /**
     * Constructs a {@code RealmCallback} with a prompt and default
     * realm information.
     *
     * @param prompt The non-null prompt to use to request the realm information.
     * @param defaultRealmInfo The non-null default realm information to use.
     * @throws IllegalArgumentException If {@code prompt} is null or
     * the empty string,
     * or if {@code defaultRealm} is empty or null.
     */
    public RealmCallback(String prompt, String defaultRealmInfo) {
        super(prompt, defaultRealmInfo);
    }

    private static final long serialVersionUID = -4342673378785456908L;
}
