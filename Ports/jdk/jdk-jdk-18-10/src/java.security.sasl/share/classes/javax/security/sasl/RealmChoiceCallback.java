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

import javax.security.auth.callback.ChoiceCallback;

/**
 * This callback is used by {@code SaslClient} and {@code SaslServer}
 * to obtain a realm given a list of realm choices.
 *
 * @since 1.5
 *
 * @author Rosanna Lee
 * @author Rob Weltman
 */
public class RealmChoiceCallback extends ChoiceCallback {

    /**
     * Constructs a {@code RealmChoiceCallback} with a prompt, a list of
     * choices and a default choice.
     *
     * @param prompt the non-null prompt to use to request the realm.
     * @param choices the non-null list of realms to choose from.
     * @param defaultChoice the choice to be used as the default choice
     * when the list of choices is displayed. It is an index into
     * the {@code choices} array.
     * @param multiple true if multiple choices allowed; false otherwise
     * @throws IllegalArgumentException If {@code prompt} is null or the empty string,
     * if {@code choices} has a length of 0, if any element from
     * {@code choices} is null or empty, or if {@code defaultChoice}
     * does not fall within the array boundary of {@code choices}
     */
    public RealmChoiceCallback(String prompt, String[]choices,
        int defaultChoice, boolean multiple) {
        super(prompt, choices, defaultChoice, multiple);
    }

    private static final long serialVersionUID = -8588141348846281332L;
}
